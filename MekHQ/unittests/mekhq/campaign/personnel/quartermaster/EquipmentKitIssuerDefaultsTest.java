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

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MECHANIC;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DELUXE_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXV;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_PERSONAL_COMPUTER;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_WEAPON;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.count;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.kit;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.person;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.setRoster;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.warehouseWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.AbstractKitIssuer.KitIssueTotals;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KitProfession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for the two-slot behaviour of {@link EquipmentKitIssuer}: slot-specific removal and replacement,
 * recruitment and fulfilment across both slots, bulk default issue, default switching, and the one-off Basic Toolkit
 * handout.
 */
class EquipmentKitIssuerDefaultsTest {
    private Campaign campaign;
    private CampaignOptions options;
    private EquipmentType basic;
    private EquipmentType deluxe;
    private EquipmentType computer;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        options = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(options);
        basic = kit(KIT_BASIC_TOOLKIT);
        deluxe = kit(KIT_DELUXE_TOOLKIT);
        computer = kit(KIT_PERSONAL_COMPUTER);
    }

    private ForceShoppingList shoppingList() {
        return campaign.getPlayerForce().getShoppingList();
    }

    // region removeKit / issueFromStock by slot
    @Test
    void removeKitBySlotReturnsThatSlotsKitAndLeavesTheOtherSlot() {
        LocalWarehouse warehouse = warehouseWith();
        Person person = person(MEK_TECH, ADMINISTRATOR);
        KitTestSupport.wireEquipmentKits(person, KIT_BASIC_TOOLKIT, KIT_PERSONAL_COMPUTER);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertTrue(EquipmentKitIssuer.removeKit(person, KitSlot.SECONDARY, campaign));

        assertEquals(KIT_BASIC_TOOLKIT, person.getRepairKitName());
        assertNull(person.getSecondaryKitName());
        assertEquals(1, count(warehouse, computer));
    }

    @Test
    void removeKitBySlotIsANoOpForAnEmptySlot() {
        Person person = person(MEK_TECH, NONE);
        assertFalse(EquipmentKitIssuer.removeKit(person, KitSlot.SECONDARY, campaign));
    }

    @Test
    void issuingIntoAFilledSlotReturnsTheOldKitToStores() {
        LocalWarehouse warehouse = warehouseWith(deluxe);
        Person person = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(person, KIT_BASIC_TOOLKIT, null);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertTrue(EquipmentKitIssuer.issueFromStock(person, deluxe, KitSlot.PRIMARY, campaign));

        assertEquals(KIT_DELUXE_TOOLKIT, person.getRepairKitName());
        assertEquals(0, count(warehouse, deluxe));
        assertEquals(1, count(warehouse, basic), "the replaced Basic Toolkit goes back on the shelf");
    }
    // endregion removeKit / issueFromStock by slot

    // region recruitment
    @Test
    void aSharedDefaultForBothRolesIsOrderedOnlyOnce() {
        LocalWarehouse warehouse = warehouseWith(); // empty
        Person person = person(MEK_TECH, MECHANIC);
        when(person.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.MECHANIC_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT, true);

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(person, campaign, false);

        verify(shoppingList(), times(1)).addShoppingItem(any(), eq(1), any());
        assertEquals(KIT_BASIC_TOOLKIT, person.getIntendedKitName(KitSlot.PRIMARY));
        assertNull(person.getIntendedKitName(KitSlot.SECONDARY));
    }

    @Test
    void aSharedDefaultForBothRolesIsIssuedOnlyOnce() {
        LocalWarehouse warehouse = warehouseWith(basic, basic);
        Person person = person(MEK_TECH, MECHANIC);
        when(person.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.MECHANIC_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);

        EquipmentKitIssuer.equipDefaultToolKitOnRecruitment(person, campaign, false);

        assertEquals(KIT_BASIC_TOOLKIT, person.getRepairKitName());
        assertNull(person.getSecondaryKitName());
        assertEquals(1, count(warehouse, basic), "only one Basic Toolkit is drawn");
    }
    // endregion recruitment

    // region fulfillPendingToolKits
    @Test
    void fulfilmentFillsTheSecondarySlotAndClearsItsIntent() {
        LocalWarehouse warehouse = warehouseWith(computer);
        Person person = person(MEK_TECH, ADMINISTRATOR);
        KitTestSupport.wireEquipmentKits(person, KIT_BASIC_TOOLKIT, null);
        person.setIntendedKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);
        when(person.getWarehouse()).thenReturn(warehouse);
        setRoster(campaign, List.of(person));

        EquipmentKitIssuer.fulfillPendingToolKits(campaign);

        assertEquals(KIT_BASIC_TOOLKIT, person.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, person.getSecondaryKitName());
        assertNull(person.getIntendedKitName(KitSlot.SECONDARY));
    }

    @Test
    void fulfilmentKeepsWaitingWhileTheKitIsNotInStores() {
        Person person = person(MEK_TECH, ADMINISTRATOR);
        person.setIntendedKitName(KitSlot.SECONDARY, KIT_PERSONAL_COMPUTER);
        when(person.getWarehouse()).thenReturn(warehouseWith());
        setRoster(campaign, List.of(person));

        EquipmentKitIssuer.fulfillPendingToolKits(campaign);

        assertNull(person.getSecondaryKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, person.getIntendedKitName(KitSlot.SECONDARY));
    }
    // endregion fulfillPendingToolKits

    // region issueDefaultKits
    @Test
    void issueDefaultKitsReplacesBothSlotsFromStores() {
        LocalWarehouse warehouse = warehouseWith(basic, computer);
        Person person = person(MEK_TECH, ADMINISTRATOR);
        KitTestSupport.wireEquipmentKits(person, KIT_DELUXE_TOOLKIT, KIT_DESCARTES_MK_XXV);
        when(person.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.ADMIN_DEFAULT_TOOL_KIT, KIT_PERSONAL_COMPUTER);

        KitIssueTotals totals = new KitIssueTotals();
        EquipmentKitIssuer.issueDefaultKits(List.of(person), campaign, totals);

        assertEquals(KIT_BASIC_TOOLKIT, person.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, person.getSecondaryKitName());
        assertEquals(2, totals.issued);
        assertEquals(0, totals.ordered);
        assertEquals(1, count(warehouse, deluxe), "replaced kits go back to stores");
        assertEquals(1, count(warehouse, kit(KIT_DESCARTES_MK_XXV)));
    }

    @Test
    void issueDefaultKitsOrdersTheWholeShortfallInOneEntryPerKit() {
        Person first = person(MEK_TECH, ADMINISTRATOR);
        Person second = person(MEK_TECH, ADMINISTRATOR);
        when(first.getWarehouse()).thenReturn(warehouseWith());
        when(second.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.ADMIN_DEFAULT_TOOL_KIT, KIT_PERSONAL_COMPUTER);

        KitIssueTotals totals = new KitIssueTotals();
        EquipmentKitIssuer.issueDefaultKits(List.of(first, second), campaign, totals);

        verify(shoppingList(), times(2)).addShoppingItem(any(), eq(2), any());
        assertEquals(0, totals.issued);
        assertEquals(4, totals.ordered);
        assertEquals(KIT_BASIC_TOOLKIT, first.getIntendedKitName(KitSlot.PRIMARY));
        assertEquals(KIT_PERSONAL_COMPUTER, second.getIntendedKitName(KitSlot.SECONDARY));
    }

    @Test
    void issueDefaultKitsLeavesASlotWhoseRoleHasNoDefault() {
        Person person = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(person, KIT_WEAPON, KIT_DESCARTES_MK_XXV);
        when(person.getWarehouse()).thenReturn(warehouseWith());
        // no defaults configured at all

        KitIssueTotals totals = new KitIssueTotals();
        EquipmentKitIssuer.issueDefaultKits(List.of(person), campaign, totals);

        assertEquals(KIT_WEAPON, person.getRepairKitName());
        assertEquals(KIT_DESCARTES_MK_XXV, person.getSecondaryKitName());
        assertEquals(0, totals.issued + totals.ordered);
    }

    @Test
    void issueDefaultKitsDoesNotReorderAKitAlreadyAwaited() {
        Person person = person(MEK_TECH, NONE);
        person.setIntendedKitName(KitSlot.PRIMARY, KIT_BASIC_TOOLKIT);
        when(person.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);

        KitIssueTotals totals = new KitIssueTotals();
        EquipmentKitIssuer.issueDefaultKits(List.of(person), campaign, totals);

        verify(shoppingList(), never()).addShoppingItem(any(), anyInt(), any());
        assertEquals(0, totals.ordered);
    }
    // endregion issueDefaultKits

    // region switchDefaultKit
    @Test
    void switchDefaultKitOnlyTouchesTheProfessionsSlotAndHoldersOfTheOldKit() {
        Person holder = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(holder, KIT_BASIC_TOOLKIT, null);
        when(holder.getWarehouse()).thenReturn(warehouseWith(deluxe));

        Person otherKit = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(otherKit, KIT_WEAPON, null);
        when(otherKit.getWarehouse()).thenReturn(warehouseWith(deluxe));

        Person otherProfession = person(ADMINISTRATOR, NONE);
        KitTestSupport.wireEquipmentKits(otherProfession, KIT_BASIC_TOOLKIT, null);
        when(otherProfession.getWarehouse()).thenReturn(warehouseWith(deluxe));

        Person secondaryHolder = person(ADMINISTRATOR, MEK_TECH);
        KitTestSupport.wireEquipmentKits(secondaryHolder, KIT_BASIC_TOOLKIT, KIT_BASIC_TOOLKIT);
        when(secondaryHolder.getWarehouse()).thenReturn(warehouseWith()); // no stock: ordered

        setRoster(campaign, List.of(holder, otherKit, otherProfession, secondaryHolder));

        KitIssueTotals totals = new KitIssueTotals();
        EquipmentKitIssuer.switchDefaultKit(campaign, KitProfession.MEK_TECH, KIT_BASIC_TOOLKIT, deluxe, totals);

        assertEquals(KIT_DELUXE_TOOLKIT, holder.getRepairKitName());
        assertEquals(KIT_WEAPON, otherKit.getRepairKitName(), "not carrying the old default");
        assertEquals(KIT_BASIC_TOOLKIT, otherProfession.getRepairKitName(), "an Admin's slot is not a MekTech's");
        assertEquals(KIT_BASIC_TOOLKIT, secondaryHolder.getRepairKitName(), "the Admin (primary) slot is untouched");
        assertEquals(KIT_BASIC_TOOLKIT, secondaryHolder.getSecondaryKitName(), "kept until the new kit arrives");
        assertEquals(KIT_DELUXE_TOOLKIT, secondaryHolder.getIntendedKitName(KitSlot.SECONDARY));
        assertEquals(1, totals.issued);
        assertEquals(1, totals.ordered);
        verify(shoppingList(), times(1)).addShoppingItem(any(), eq(1), any());
    }
    // endregion switchDefaultKit

    // region equipAllTechsWithBasicToolKit
    @Test
    void basicToolkitHandoutUsesAFreeSlot() {
        Person tech = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(tech, KIT_DESCARTES_MK_XXV, null);
        when(tech.isTechExpanded()).thenReturn(true);
        setRoster(campaign, List.of(tech));

        EquipmentKitIssuer.equipAllTechsWithBasicToolKit(campaign);

        assertEquals(KIT_DESCARTES_MK_XXV, tech.getRepairKitName());
        assertEquals(KIT_BASIC_TOOLKIT, tech.getSecondaryKitName());
    }

    @Test
    void basicToolkitHandoutReturnsTheSecondaryKitWhenBothSlotsAreFull() {
        LocalWarehouse warehouse = warehouseWith();
        Person tech = person(MEK_TECH, ADMINISTRATOR);
        KitTestSupport.wireEquipmentKits(tech, KIT_DESCARTES_MK_XXV, KIT_PERSONAL_COMPUTER);
        when(tech.isTechExpanded()).thenReturn(true);
        when(tech.getWarehouse()).thenReturn(warehouse);
        setRoster(campaign, List.of(tech));

        EquipmentKitIssuer.equipAllTechsWithBasicToolKit(campaign);

        assertEquals(KIT_DESCARTES_MK_XXV, tech.getRepairKitName());
        assertEquals(KIT_BASIC_TOOLKIT, tech.getSecondaryKitName());
        assertEquals(1, count(warehouse, computer));
    }

    @Test
    void basicToolkitHandoutSkipsATechWithAToolKitInEitherSlot() {
        Person tech = person(MEK_TECH, NONE);
        KitTestSupport.wireEquipmentKits(tech, KIT_DESCARTES_MK_XXV, KIT_WEAPON);
        when(tech.isTechExpanded()).thenReturn(true);
        setRoster(campaign, List.of(tech));

        EquipmentKitIssuer.equipAllTechsWithBasicToolKit(campaign);

        assertEquals(KIT_DESCARTES_MK_XXV, tech.getRepairKitName());
        assertEquals(KIT_WEAPON, tech.getSecondaryKitName());
    }

    @Test
    void basicToolkitHandoutSkipsNonTechs() {
        Person admin = person(ADMINISTRATOR, NONE);
        when(admin.isTechExpanded()).thenReturn(false);
        setRoster(campaign, List.of(admin));

        EquipmentKitIssuer.equipAllTechsWithBasicToolKit(campaign);

        assertNull(admin.getRepairKitName());
    }
    // endregion equipAllTechsWithBasicToolKit
}
