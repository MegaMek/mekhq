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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import megamek.common.units.Entity;
import megamek.common.units.VTOL;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.InfantryWeaponPart;
import mekhq.campaign.parts.equipment.MissingInfantryWeaponPart;
import mekhq.campaign.parts.missing.MissingRotor;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Vehicle and infantry refits keep what they do not change and buy what the unit itself uses: a VTOL keeps its damaged
 * rotor instead of buying a new one, and a platoon rearmed with new weapons gets infantry weapon parts, filled from the
 * warehouse where it can, with no second set added on a reload (issue #10207).
 */
class RefitVehicleInfantryTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static void complete(Refit refit) throws Exception {
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    /**
     * @return how many of each part the unit has, by part class and name
     */
    private static Map<String, Integer> partsByClass(Unit unit) {
        Map<String, Integer> parts = new TreeMap<>();
        for (Part part : unit.getParts()) {
            parts.merge(part.getClass().getSimpleName() + " " + part.getName(), 1, Integer::sum);
        }
        return parts;
    }

    private static long countOf(Refit refit, Class<? extends Part> partType) {
        long count = 0;
        for (Part part : refit.getShoppingList()) {
            if (partType.isInstance(part)) {
                count++;
            }
        }
        return count;
    }

    @Test
    void aVtolKeepsItsDamagedRotor() {
        Unit warrior = scenario.withUnit(UnitFixture.WARRIOR_H_7_ATTACK_HELICOPTER);
        Entity entity = warrior.getEntity();
        entity.setInternal(entity.getInternal(VTOL.LOC_ROTOR) - 1, VTOL.LOC_ROTOR);
        for (Part part : warrior.getParts()) {
            part.updateConditionFromEntity(false);
        }
        assertTrue(PartsScenario.unitParts(warrior, Rotor.class).getFirst().needsFixing());

        Refit refit = new Refit(warrior, UnitFixture.WARRIOR_H_7_ATTACK_HELICOPTER.loadEntity(), true, false, false);

        assertEquals(0, countOf(refit, MissingRotor.class), "A damaged rotor is still the same rotor");
        assertEquals(Refit.NO_CHANGE, refit.getRefitClass());
        assertEquals(Money.zero(), refit.getCost());
    }

    @Test
    void aRearmedPlatoonGetsInfantryWeaponPartsAndNoSecondSet() throws Exception {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);
        Refit refit = new Refit(platoon, UnitFixture.FOOT_PLATOON_LASER.loadEntity(), true, false, false);

        complete(refit);

        Unit freshLaserPlatoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        assertEquals(partsByClass(freshLaserPlatoon), partsByClass(platoon),
              "The rearmed platoon carries the infantry weapon parts a laser platoon has, not generic equipment");

        // Loading the campaign builds the unit's parts again from those it already has
        platoon.initializeParts(true);

        assertEquals(partsByClass(freshLaserPlatoon), partsByClass(platoon),
              "Loading the campaign adds no second set of weapons");
    }

    @Test
    void spareInfantryWeaponsAreUsedBeforeBuyingNew() {
        Unit laserPlatoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        InfantryWeaponPart laserRifle = PartsScenario.unitParts(laserPlatoon, InfantryWeaponPart.class).getFirst();
        int laserRifles = PartsScenario.unitParts(laserPlatoon, InfantryWeaponPart.class).size();
        Part spareLaserRifles = laserRifle.clone();
        scenario.withSpare(spareLaserRifles, laserRifles);
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);

        Refit refit = new Refit(platoon, UnitFixture.FOOT_PLATOON_LASER.loadEntity(), true, false, false);

        assertTrue(refit.getShoppingList().isEmpty(), "The laser rifles in the warehouse fill the refit");
        assertEquals(Money.zero(), refit.getCost());
    }

    @Test
    void aMissingSecondaryWeaponStaysSecondaryThroughASave() throws Exception {
        // The MG platoon's machine guns are its secondary weapons
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);
        InfantryWeaponPart secondaryWeapon = null;
        for (InfantryWeaponPart weapon : PartsScenario.unitParts(platoon, InfantryWeaponPart.class)) {
            if (!weapon.isPrimary()) {
                secondaryWeapon = weapon;
            }
        }
        assertTrue(secondaryWeapon != null, "The MG platoon has secondary weapons");

        Part loaded = PartXmlRoundTripTest.saveAndLoad(secondaryWeapon.getMissingPart(), scenario.getCampaign());

        MissingInfantryWeaponPart missingWeapon = assertInstanceOf(MissingInfantryWeaponPart.class, loaded);
        assertFalse(missingWeapon.isPrimary());
        assertFalse(missingWeapon.getNewPart().isPrimary(), "The replacement is bought as a secondary weapon");
    }
}
