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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Aerospace fighters have no assault class, so an Assault request built a Squadron with no Flights and one fighter
 * (MegaMek/megamek#9646). A preset saved with that request is rolled at a Random weight instead.
 */
class CommandGeneratorWeightClassTest {

    private static ForceDescriptorSnapshot snapshot(Integer unitType, Integer weightClass) {
        ForceDescriptorSnapshot snapshot = new ForceDescriptorSnapshot();
        snapshot.setUnitType(unitType);
        snapshot.setWeightClass(weightClass);
        return snapshot;
    }

    @Test
    void assaultAerospaceFightersRollAtRandomWeight() {
        assertNull(CommandGenerator.requestedWeightClass(
              snapshot(UnitType.AEROSPACE_FIGHTER, EntityWeightClass.WEIGHT_ASSAULT)));
    }

    @ParameterizedTest
    @ValueSource(ints = { EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_MEDIUM,
                          EntityWeightClass.WEIGHT_HEAVY })
    void otherAerospaceWeightsAreKept(int weightClass) {
        assertEquals(weightClass,
              CommandGenerator.requestedWeightClass(snapshot(UnitType.AEROSPACE_FIGHTER, weightClass)));
    }

    @Test
    void assaultMeksAreKept() {
        assertEquals(EntityWeightClass.WEIGHT_ASSAULT,
              CommandGenerator.requestedWeightClass(snapshot(UnitType.MEK, EntityWeightClass.WEIGHT_ASSAULT)));
    }

    @Test
    void assaultCombinedArmsIsKept() {
        assertEquals(EntityWeightClass.WEIGHT_ASSAULT,
              CommandGenerator.requestedWeightClass(snapshot(null, EntityWeightClass.WEIGHT_ASSAULT)));
    }

    @Test
    void randomStaysRandom() {
        assertNull(CommandGenerator.requestedWeightClass(snapshot(UnitType.AEROSPACE_FIGHTER, null)));
    }
}
