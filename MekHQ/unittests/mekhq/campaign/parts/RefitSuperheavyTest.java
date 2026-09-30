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
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * On a superheavy Mek one critical slot holds what two standard slots would, and two one-slot items can share a slot.
 * Refitting between the two Omegas must still leave each with exactly the parts a factory-fresh Omega of the new model
 * has, in the same places, including the ammunition bins that share slots.
 */
class RefitSuperheavyTest {
    private final PartsScenario scenario = PartsScenario.create();

    private void assertRefitMatchesFactoryFresh(UnitFixture from, UnitFixture to) throws Exception {
        Unit omega = scenario.withUnit(from);
        assertTrue(omega.getEntity().isSuperHeavy());
        Refit refit = new Refit(omega, to.loadEntity(), true, false, false);
        // The two Omegas differ in internal structure, endo steel against standard
        assertEquals(Refit.CLASS_F, refit.getRefitClass());

        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();

        Unit freshOmega = scenario.withUnit(to);
        assertEquals(PartsCensus.ofUnit(freshOmega), PartsCensus.ofUnit(omega),
              "The refitted Omega has the parts of a factory-fresh one, in the same places");
    }

    @Test
    void anOmegaShp4xRefitsIntoAnShp5r() throws Exception {
        assertRefitMatchesFactoryFresh(UnitFixture.OMEGA_SHP_4X, UnitFixture.OMEGA_SHP_5R);
    }

    @Test
    void anOmegaShp5rRefitsIntoAnShp4x() throws Exception {
        assertRefitMatchesFactoryFresh(UnitFixture.OMEGA_SHP_5R, UnitFixture.OMEGA_SHP_4X);
    }
}
