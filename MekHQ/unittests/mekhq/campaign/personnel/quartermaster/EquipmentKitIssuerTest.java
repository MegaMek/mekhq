/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.quartermaster;

import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class EquipmentKitIssuerTest {
    private EquipmentType kit;
    private Campaign campaign;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        kit = EquipmentType.get(KIT_BASIC_TOOLKIT);
    }

    /** A person carrying the given single tool kit ({@code null} for none), with a live single-kit get/set/has mock. */
    private static Person personOwning(String kit) {
        Person person = mock(Person.class);
        wireKitState(person, kit);
        return person;
    }

    private static Person techWithRole(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);
        when(person.getSecondaryRole()).thenReturn(NONE);
        wireKitState(person, null);
        return person;
    }

    /** Backs the single tool-kit and intended-kit accessors with mutable holders so issue/remove/strip round-trip. */
    private static void wireKitState(Person person, String initialKit) {
        String[] held = { initialKit };
        String[] intended = { null };
        when(person.getRepairKitName()).thenAnswer(invocation -> held[0]);
        doAnswer(invocation -> {
            held[0] = invocation.getArgument(0);
            return null;
        }).when(person).setRepairKitName(any());
        when(person.getIntendedRepairKitName()).thenAnswer(invocation -> intended[0]);
        doAnswer(invocation -> {
            intended[0] = invocation.getArgument(0);
            return null;
        }).when(person).setIntendedRepairKitName(any());
        when(person.hasRepairKit(anyString())).thenAnswer(invocation -> invocation.getArgument(0).equals(held[0]));
    }

    /** A present, spare warehouse part of the given kit type. */
    private EquipmentPart kitPart() {
        EquipmentPart part = mock(EquipmentPart.class);
        when(part.isPresent()).thenReturn(true);
        when(part.isSpare()).thenReturn(true);
        when(part.getQuantity()).thenReturn(1);
        when(part.getType()).thenReturn(kit);
        return part;
    }

    /** A warehouse holding the given spare parts, whose findSparePart honours the predicate against them. */
    private static LocalWarehouse warehouseHolding(List<Part> spares) {
        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.getSpareParts()).thenReturn(spares);
        when(warehouse.findSparePart(any())).thenAnswer(invocation -> {
            Predicate<Part> predicate = invocation.getArgument(0);
            return spares.stream().filter(predicate).findFirst().orElse(null);
        });
        return warehouse;
    }

    private static CampaignOptions optionsWith(String mekTechDefault, boolean addToProcurement) {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, mekTechDefault);
        options.set(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT, addToProcurement);
        return options;
    }

    // region localStock
    @Test
    void localStockCountsPresentKitsButNotInTransitOnes() {
        EquipmentPart present = kitPart();
        EquipmentPart inTransit = kitPart();
        when(inTransit.isPresent()).thenReturn(false);
        LocalWarehouse warehouse = warehouseHolding(List.of(present, inTransit));

        Person person = mock(Person.class);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertEquals(1, EquipmentKitIssuer.localStock(person, kit));
    }

    @Test
    void localStockIsZeroWithoutAWarehouse() {
        Person person = mock(Person.class);
        when(person.getWarehouse()).thenReturn(null);
        assertEquals(0, EquipmentKitIssuer.localStock(person, kit));
    }
    // endregion localStock

    // region issueFromStock
    @Test
    void issueFromStockDrawsAPresentKitAndRecordsItOnTheTech() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart()));
        Person person = personOwning(null);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertTrue(EquipmentKitIssuer.issueFromStock(person, kit, campaign));
        verify(warehouse).removePart(any(), eq(1));
        assertEquals(kit.getInternalName(), person.getRepairKitName());
    }

    @Test
    void issueFromStockFailsWhenOnlyInTransitKitsAreHeld() {
        EquipmentPart inTransit = kitPart();
        when(inTransit.isPresent()).thenReturn(false);
        LocalWarehouse warehouse = warehouseHolding(List.of(inTransit));
        Person person = personOwning(null);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertFalse(EquipmentKitIssuer.issueFromStock(person, kit, campaign));
        verify(warehouse, never()).removePart(any(), anyInt());
        assertNull(person.getRepairKitName());
    }

    @Test
    void issueFromStockIsANoOpWhenTheTechAlreadyOwnsTheKit() {
        Person person = personOwning(kit.getInternalName());

        assertTrue(EquipmentKitIssuer.issueFromStock(person, kit, campaign));
        verify(person, never()).getWarehouse();
    }
    // endregion issueFromStock

    // region removeKit
    @Test
    void removeKitReturnsAnOwnedKitToLocalStores() {
        LocalWarehouse warehouse = warehouseHolding(new java.util.ArrayList<>());
        Person person = personOwning(kit.getInternalName());
        when(person.getWarehouse()).thenReturn(warehouse);

        assertTrue(EquipmentKitIssuer.removeKit(person, kit, campaign));
        assertNull(person.getRepairKitName());
        verify(warehouse).addPart(any(), eq(true));
    }

    @Test
    void removeKitIsANoOpWhenTheTechDoesNotOwnIt() {
        Person person = personOwning(null);
        assertFalse(EquipmentKitIssuer.removeKit(person, kit, campaign));
        verify(person, never()).getWarehouse();
    }
    // endregion removeKit

    // region equipDefaultToolKitOnRecruitment
    @Test
    void recruitmentIssuesTheProfessionDefaultStraightFromStock() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart()));
        Person tech = techWithRole(MEK_TECH);
        when(tech.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(optionsWith(KIT_BASIC_TOOLKIT, false));

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(tech, campaign, false);

        assertEquals(kit.getInternalName(), tech.getRepairKitName());
        verify(warehouse).removePart(any(), eq(1));
    }

    @Test
    void recruitmentSkipsAProfessionWhoseDefaultIsNone() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart()));
        Person tech = techWithRole(MEK_TECH);
        when(tech.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(optionsWith(EquipmentKitCatalog.NO_DEFAULT_KIT, false));

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(tech, campaign, false);

        assertNull(tech.getRepairKitName());
        verify(warehouse, never()).removePart(any(), anyInt());
    }

    @Test
    void recruitmentGmAddGrantsTheKitDirectlyWhenNotInStockAndProcurementIsOn() {
        LocalWarehouse warehouse = warehouseHolding(new java.util.ArrayList<>()); // empty stores
        Person tech = techWithRole(MEK_TECH);
        when(tech.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(optionsWith(KIT_BASIC_TOOLKIT, true));

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(tech, campaign, true);

        assertEquals(kit.getInternalName(), tech.getRepairKitName());
    }

    @Test
    void recruitmentQueuesAPendingKitWhenNotInStockAndProcurementIsOn() {
        LocalWarehouse warehouse = warehouseHolding(new java.util.ArrayList<>()); // empty stores
        Person tech = techWithRole(MEK_TECH);
        when(tech.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(optionsWith(KIT_BASIC_TOOLKIT, true));

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(tech, campaign, false);

        assertNull(tech.getRepairKitName(), "not yet owned — awaiting delivery");
        assertEquals(kit.getInternalName(), tech.getIntendedRepairKitName());
    }

    @Test
    void recruitmentDoesNothingWhenNotInStockAndProcurementIsOff() {
        LocalWarehouse warehouse = warehouseHolding(new java.util.ArrayList<>()); // empty stores
        Person tech = techWithRole(MEK_TECH);
        when(tech.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(optionsWith(KIT_BASIC_TOOLKIT, false));

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(tech, campaign, false);

        assertNull(tech.getRepairKitName());
        assertNull(tech.getIntendedRepairKitName());
    }
    // endregion equipDefaultToolKitOnRecruitment
}
