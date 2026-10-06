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

import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.ConvInfantry;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class ArmorKitIssuerTest {
    private EquipmentType kit;
    private Campaign campaign;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        kit = EquipmentType.get("MekWarrior Kit (Basic)");
    }

    private static Person personWithRole(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);
        when(person.getSecondaryRole()).thenReturn(NONE);
        return person;
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

    // region gatherPersonnel
    @Test
    void gatherPersonnelKeepsKitEligiblePeopleAndUnitCrewWithoutDuplicates() {
        Person mekWarrior = personWithRole(MEKWARRIOR);
        Person medic = personWithRole(DOCTOR);
        Person soldier = personWithRole(SOLDIER);

        Unit platoon = mock(Unit.class);
        when(platoon.getCrew()).thenReturn(List.of(soldier, medic));

        Set<Person> gathered = ArmorKitIssuer.gatherPersonnel(List.of(mekWarrior, medic), List.of(platoon));

        assertTrue(gathered.contains(mekWarrior));
        assertTrue(gathered.contains(soldier));
        assertFalse(gathered.contains(medic), "a medic cannot be issued a kit");
        assertEquals(2, gathered.size());
    }

    @Test
    void gatherPersonnelToleratesNullSelections() {
        assertTrue(ArmorKitIssuer.gatherPersonnel(null, null).isEmpty());
    }
    // endregion gatherPersonnel

    // region localStock
    @Test
    void localStockCountsPresentKitsButNotInTransitOnes() {
        EquipmentPart present = kitPart();
        EquipmentPart inTransit = kitPart();
        when(inTransit.isPresent()).thenReturn(false);
        LocalWarehouse warehouse = warehouseHolding(List.of(present, inTransit));

        Person person = mock(Person.class);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertEquals(1, ArmorKitIssuer.localStock(person, kit));
    }

    @Test
    void localStockIsZeroWithoutAWarehouse() {
        Person person = mock(Person.class);
        when(person.getWarehouse()).thenReturn(null);
        assertEquals(0, ArmorKitIssuer.localStock(person, kit));
    }
    // endregion localStock

    // region issueFromStock
    @Test
    void issueFromStockDrawsAPresentKitAndWearsIt() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart()));
        Person person = mock(Person.class);
        when(person.getArmorKitName()).thenReturn(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertTrue(ArmorKitIssuer.issueFromStock(person, kit, campaign));
        verify(warehouse).removePart(any(), eq(1));
        verify(person).setArmorKitName(kit.getInternalName());
    }

    @Test
    void issueFromStockFailsWhenOnlyInTransitKitsAreHeld() {
        EquipmentPart inTransit = kitPart();
        when(inTransit.isPresent()).thenReturn(false);
        LocalWarehouse warehouse = warehouseHolding(List.of(inTransit));
        Person person = mock(Person.class);
        when(person.getArmorKitName()).thenReturn(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
        when(person.getWarehouse()).thenReturn(warehouse);

        assertFalse(ArmorKitIssuer.issueFromStock(person, kit, campaign));
        verify(warehouse, never()).removePart(any(), anyInt());
        verify(person, never()).setArmorKitName(any());
    }

    @Test
    void issueFromStockIsANoOpWhenAlreadyWearingTheKit() {
        Person person = mock(Person.class);
        when(person.getArmorKitName()).thenReturn(kit.getInternalName());

        assertTrue(ArmorKitIssuer.issueFromStock(person, kit, campaign));
        verify(person, never()).getWarehouse();
    }
    // endregion issueFromStock

    // region issuePlatoonKit
    private Unit infantryPlatoon(int troopers, LocalWarehouse warehouse) {
        Person[] crew = new Person[troopers];
        for (int i = 0; i < troopers; i++) {
            crew[i] = mock(Person.class);
        }
        Unit unit = mock(Unit.class);
        when(unit.getCrew()).thenReturn(List.of(crew));
        when(unit.getWarehouse()).thenReturn(warehouse);
        when(unit.getEntity()).thenReturn(mock(ConvInfantry.class));
        when(unit.getArmorKitName()).thenReturn(null);
        when(unit.getDesignedInfantryKitName()).thenReturn(null);
        return unit;
    }

    @Test
    void issuePlatoonKitWaitsForArrivalWhenStoresCannotOutfitTheWholePlatoon() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart())); // 1 in stock
        Unit platoon = infantryPlatoon(3, warehouse); // needs 3

        ArmorKitIssuer.issuePlatoonKit(platoon, kit, campaign);

        verify(platoon).setIntendedArmorKitName(kit.getInternalName());
        verify(platoon, never()).setArmorKitName(any());
        verify(platoon, never()).resetPilotAndEntity();
    }

    @Test
    void issuePlatoonKitEquipsImmediatelyWhenStoresCanOutfitTheWholePlatoon() {
        LocalWarehouse warehouse = warehouseHolding(List.of(kitPart(), kitPart(), kitPart())); // 3 in stock
        Unit platoon = infantryPlatoon(3, warehouse); // needs 3

        ArmorKitIssuer.issuePlatoonKit(platoon, kit, campaign);

        verify(platoon).setArmorKitName(kit.getInternalName());
        verify(platoon).setIntendedArmorKitName(null);
        verify(platoon).resetPilotAndEntity();
        verify(warehouse, times(3)).removePart(any(), eq(1));
    }
    // endregion issuePlatoonKit
}
