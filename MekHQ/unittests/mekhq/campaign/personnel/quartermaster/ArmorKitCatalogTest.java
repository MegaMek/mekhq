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

import static mekhq.campaign.personnel.enums.PersonnelRole.AEROSPACE_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvFighter;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Tank;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArmorKitCatalogTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Person personWithRole(PersonnelRole primary) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(primary);
        when(person.getSecondaryRole()).thenReturn(NONE);
        return person;
    }

    private static Set<String> availableNames(Category category) {
        return availableKits(category).stream()
                     .map(EquipmentType::getInternalName)
                     .collect(Collectors.toSet());
    }

    // region kit-name regression guards
    // These names are compared as strings against MegaMek's equipment tables. If a kit is renamed in MegaMek, the
    // lookup returns null and the kit silently drops out of its group — these tests turn that into a loud failure.
    @Test
    void everyReferencedKitNameStillResolvesToARealKit() {
        for (String name : allReferencedKitNames()) {
            assertNotNull(EquipmentType.get(name),
                  "Kit name '" +
                        name +
                        "' no longer resolves — it was renamed or removed in MegaMek's equipment tables");
        }
    }

    @Test
    void theSpecificKitsTheLogicDependsOnResolve() {
        // The graded MekWarrior choices, the vehicle kits, the aero kit and the default all matter to branching logic.
        for (String name : List.of(KIT_MEKWARRIOR_BASIC,
              KIT_MEKWARRIOR_ADVANCED,
              KIT_MEKWARRIOR_CLAN,
              KIT_AEROSPACE_PILOT,
              KIT_SNOWSUIT,
              KIT_FLAK_STANDARD,
              KIT_SNOWSUIT,
              KIT_HEAT_SUIT,
              KIT_ENVIRONMENT_SUIT_LIGHT,
              DEFAULT_ARMOR_KIT_NAME)) {
            assertNotNull(EquipmentType.get(name), "Kit name '" + name + "' no longer resolves");
        }
    }
    // endregion kit-name regression guards

    // region categoryOf
    @Test
    void categoryOfSortsKitsByName() {
        assertEquals(Category.MEKWARRIOR, categoryOf(KIT_MEKWARRIOR_BASIC));
        assertEquals(Category.AIRCRAFT, categoryOf(KIT_AEROSPACE_PILOT));
        assertEquals(Category.INFANTRY, categoryOf(KIT_SNOWSUIT));
    }
    // endregion categoryOf

    // region availableKits
    @Test
    void mekWarriorGroupOffersOnlyTheThreeGradedKits() {
        assertEquals(Set.of(KIT_MEKWARRIOR_BASIC, KIT_MEKWARRIOR_ADVANCED, KIT_MEKWARRIOR_CLAN),
              availableNames(Category.MEKWARRIOR));
    }

    @Test
    void vehicleGroupIsTheFiveVehicleKits() {
        Set<String> names = availableNames(Category.INFANTRY);
        assertTrue(names.contains(KIT_SNOWSUIT));
        assertTrue(names.contains(KIT_FLAK_STANDARD));
        assertTrue(names.contains("Snowsuit"));
        assertFalse(names.contains(KIT_MEKWARRIOR_BASIC));
        assertFalse(names.contains(KIT_AEROSPACE_PILOT));
    }

    @Test
    void soldierGroupIsEveryInfantryKitButNotMekWarriorOrAeroOrCoveralls() {
        Set<String> names = availableNames(Category.SOLDIER);
        assertTrue(names.contains(KIT_SNOWSUIT));
        assertTrue(names.contains(KIT_FLAK_STANDARD));
        assertFalse(names.contains(KIT_MEKWARRIOR_BASIC));
        assertFalse(names.contains(KIT_AEROSPACE_PILOT));
        assertFalse(names.contains(DEFAULT_ARMOR_KIT_NAME));
        assertTrue(names.size() > availableNames(Category.INFANTRY).size(), "soldiers draw from more than vehicles");
    }
    // endregion availableKits

    // region categoryFor
    @Test
    void categoryForMapsRoleToGroup() {
        assertEquals(Category.MEKWARRIOR, categoryFor(personWithRole(MEKWARRIOR)));
        assertEquals(Category.AIRCRAFT, categoryFor(personWithRole(AEROSPACE_PILOT)));
        assertEquals(Category.SOLDIER, categoryFor(personWithRole(SOLDIER)));
        assertEquals(Category.INFANTRY, categoryFor(personWithRole(VEHICLE_CREW_GROUND)));
    }
    // endregion categoryFor

    // region canBeIssuedKit
    @Test
    void combatCrewsMayBeIssuedKits() {
        assertTrue(canBeIssuedKit(personWithRole(MEKWARRIOR)));
        assertTrue(canBeIssuedKit(personWithRole(AEROSPACE_PILOT)));
        assertTrue(canBeIssuedKit(personWithRole(SOLDIER)));
        assertTrue(canBeIssuedKit(personWithRole(VEHICLE_CREW_GROUND)));
    }

    @Test
    void supportAndNullPersonnelMayNot() {
        assertFalse(canBeIssuedKit(personWithRole(DOCTOR)));
        assertFalse(canBeIssuedKit(null));
    }
    // endregion canBeIssuedKit

    // region canWearIssuedKit
    @Test
    void mekVehicleAndAeroCrewsWearIssuedKitsButInfantryDoNot() {
        assertTrue(canWearIssuedKit(new BipedMek()));
        assertTrue(canWearIssuedKit(new Tank()));
        assertTrue(canWearIssuedKit(new ConvFighter()));
        assertFalse(canWearIssuedKit(new ConvInfantry()));
        assertFalse(canWearIssuedKit(null));
    }
    // endregion canWearIssuedKit

    // region npcKitFor
    @Test
    void npcMekWarriorsTakeTheAdvancedKitOnceItIsAvailable() {
        // By 3067 the Advanced kit is in production, so both Clan and Inner Sphere MekWarriors take it.
        assertEquals(KIT_MEKWARRIOR_ADVANCED, npcKitFor(new BipedMek(), true, 3067));
        assertEquals(KIT_MEKWARRIOR_ADVANCED, npcKitFor(new BipedMek(), false, 3067));
    }

    @Test
    void npcMekWarriorsFallBackToTheBasicKitBeforeTheAdvancedKitExists() {
        // In 2500 the Advanced kit is not yet available, so the Basic kit is issued instead.
        assertEquals(KIT_MEKWARRIOR_BASIC, npcKitFor(new BipedMek(), false, 2500));
    }

    @Test
    void npcTankersTakeTheKIT_SNOWSUITAndPilotsTheAeroKit() {
        assertEquals(KIT_SNOWSUIT, npcKitFor(new Tank(), false, 3067));
        assertEquals(KIT_AEROSPACE_PILOT, npcKitFor(new ConvFighter(), false, 3067));
    }

    @Test
    void npcInfantryGetNoCrewKit() {
        assertNull(npcKitFor(new ConvInfantry(), false, 3067));
    }
    // endregion npcKitFor
}
