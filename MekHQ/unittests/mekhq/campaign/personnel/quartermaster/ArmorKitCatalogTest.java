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
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArmorKitCatalogTest {
    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    private static final String COMBAT_SUIT = "MechWarrior Combat Suit";
    private static final String MW_BASIC = "MekWarrior Kit (Basic)";
    private static final String MW_ADVANCED = "MekWarrior Kit (Advanced)";
    private static final String MW_CLAN = "MekWarrior Kit (Clan)";
    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    private static final String COOLING_SUIT = "MechWarrior Cooling Suit";
    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    private static final String COOLING_VEST = "MechWarrior Cooling Vest (Only)";
    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    private static final String AERO_KIT = "Aerospace Fighter Pilot Kit";
    private static final String SMOCK = "Tanker's Smock";
    private static final String FLAK = "Flak, Standard";

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
        return ArmorKitCatalog.availableKits(category).stream()
                     .map(EquipmentType::getInternalName)
                     .collect(Collectors.toSet());
    }

    // region kit-name regression guards
    // These names are compared as strings against MegaMek's equipment tables. If a kit is renamed in MegaMek, the
    // lookup returns null and the kit silently drops out of its group — these tests turn that into a loud failure.
    @Test
    void everyReferencedKitNameStillResolvesToARealKit() {
        for (String name : ArmorKitCatalog.allReferencedKitNames()) {
            assertNotNull(EquipmentType.get(name),
                  "Kit name '" +
                        name +
                        "' no longer resolves — it was renamed or removed in MegaMek's equipment tables");
        }
    }

    @Test
    void theSpecificKitsTheLogicDependsOnResolve() {
        // The three graded MekWarrior choices, the NPC fallbacks, and the default all matter to branching logic.
        for (String name : List.of(COMBAT_SUIT, MW_BASIC, MW_ADVANCED, MW_CLAN, COOLING_SUIT, COOLING_VEST,
              AERO_KIT, SMOCK, FLAK, "Snowsuit", "Heat Suit", "Environment Suit, Light",
              ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            assertNotNull(EquipmentType.get(name), "Kit name '" + name + "' no longer resolves");
        }
    }
    // endregion kit-name regression guards

    // region categoryOf
    @Test
    void categoryOfSortsKitsByName() {
        assertEquals(Category.MEKWARRIOR, ArmorKitCatalog.categoryOf(MW_BASIC));
        assertEquals(Category.MEKWARRIOR, ArmorKitCatalog.categoryOf(COMBAT_SUIT));
        assertEquals(Category.AIRCRAFT, ArmorKitCatalog.categoryOf(AERO_KIT));
        assertEquals(Category.INFANTRY, ArmorKitCatalog.categoryOf(SMOCK));
    }
    // endregion categoryOf

    // region availableKits
    @Test
    void mekWarriorGroupOffersOnlyTheThreeGradedKits() {
        Set<String> names = availableNames(Category.MEKWARRIOR);
        assertTrue(names.contains(MW_BASIC));
        assertTrue(names.contains(MW_ADVANCED));
        assertTrue(names.contains(MW_CLAN));
        assertFalse(names.contains(COMBAT_SUIT), "combat suit is not a player choice");
        assertFalse(names.contains(COOLING_SUIT), "cooling gear is not a player choice");
        assertFalse(names.contains(COOLING_VEST));
    }

    @Test
    void vehicleGroupIsTheFiveVehicleKits() {
        Set<String> names = availableNames(Category.INFANTRY);
        assertTrue(names.contains(SMOCK));
        assertTrue(names.contains(FLAK));
        assertTrue(names.contains("Snowsuit"));
        assertFalse(names.contains(MW_BASIC));
        assertFalse(names.contains(AERO_KIT));
    }

    @Test
    void soldierGroupIsEveryInfantryKitButNotMekWarriorOrAeroOrCoveralls() {
        Set<String> names = availableNames(Category.SOLDIER);
        assertTrue(names.contains(SMOCK));
        assertTrue(names.contains(FLAK));
        assertFalse(names.contains(MW_BASIC));
        assertFalse(names.contains(AERO_KIT));
        assertFalse(names.contains(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME));
        assertTrue(names.size() > availableNames(Category.INFANTRY).size(), "soldiers draw from more than vehicles");
    }
    // endregion availableKits

    // region categoryFor
    @Test
    void categoryForMapsRoleToGroup() {
        assertEquals(Category.MEKWARRIOR, ArmorKitCatalog.categoryFor(personWithRole(MEKWARRIOR)));
        assertEquals(Category.AIRCRAFT, ArmorKitCatalog.categoryFor(personWithRole(AEROSPACE_PILOT)));
        assertEquals(Category.SOLDIER, ArmorKitCatalog.categoryFor(personWithRole(SOLDIER)));
        assertEquals(Category.INFANTRY, ArmorKitCatalog.categoryFor(personWithRole(VEHICLE_CREW_GROUND)));
    }
    // endregion categoryFor

    // region canBeIssuedKit
    @Test
    void combatCrewsMayBeIssuedKits() {
        assertTrue(ArmorKitCatalog.canBeIssuedKit(personWithRole(MEKWARRIOR)));
        assertTrue(ArmorKitCatalog.canBeIssuedKit(personWithRole(AEROSPACE_PILOT)));
        assertTrue(ArmorKitCatalog.canBeIssuedKit(personWithRole(SOLDIER)));
        assertTrue(ArmorKitCatalog.canBeIssuedKit(personWithRole(VEHICLE_CREW_GROUND)));
    }

    @Test
    void supportAndNullPersonnelMayNot() {
        assertFalse(ArmorKitCatalog.canBeIssuedKit(personWithRole(DOCTOR)));
        assertFalse(ArmorKitCatalog.canBeIssuedKit(null));
    }
    // endregion canBeIssuedKit

    // region canWearIssuedKit
    @Test
    void mekVehicleAndAeroCrewsWearIssuedKitsButInfantryDoNot() {
        assertTrue(ArmorKitCatalog.canWearIssuedKit(new BipedMek()));
        assertTrue(ArmorKitCatalog.canWearIssuedKit(new Tank()));
        assertTrue(ArmorKitCatalog.canWearIssuedKit(new ConvFighter()));
        assertFalse(ArmorKitCatalog.canWearIssuedKit(new ConvInfantry()));
        assertFalse(ArmorKitCatalog.canWearIssuedKit(null));
    }
    // endregion canWearIssuedKit

    // region npcKitFor
    @Test
    void npcClanMekWarriorTakesTheClanKit() {
        assertEquals(MW_CLAN, ArmorKitCatalog.npcKitFor(new BipedMek(), true, 3067));
    }

    @Test
    void npcInnerSphereMekWarriorTakesTheCoolingSuit() {
        assertEquals(COOLING_SUIT, ArmorKitCatalog.npcKitFor(new BipedMek(), false, 3067));
    }

    @Test
    void npcInnerSphereMekWarriorFallsBackToTheVestWhileTheSuitIsExtinct() {
        assertEquals(COOLING_VEST, ArmorKitCatalog.npcKitFor(new BipedMek(), false, 2900));
    }

    @Test
    void npcTankersTakeTheSmockAndPilotsTheAeroKit() {
        assertEquals(SMOCK, ArmorKitCatalog.npcKitFor(new Tank(), false, 3067));
        assertEquals(AERO_KIT, ArmorKitCatalog.npcKitFor(new ConvFighter(), false, 3067));
    }

    @Test
    void npcInfantryGetNoCrewKit() {
        assertNull(ArmorKitCatalog.npcKitFor(new ConvInfantry(), false, 3067));
    }
    // endregion npcKitFor
}
