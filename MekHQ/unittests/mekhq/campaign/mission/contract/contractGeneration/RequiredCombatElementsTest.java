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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.mission.contract.contractGeneration.RequiredCombatElements.calculateRequiredCombatElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.utilities.ContractUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link RequiredCombatElements#calculateRequiredCombatElements}, which turns the force a campaign can actually
 * field into the number of elements an employer demands.
 *
 * <p>Two paths: the bypass, a flat one-third reduction used where the contract type sets the commitment; and the
 * variance path, which scales the force by a rolled factor. Both floor at one element - a contract requiring nobody
 * cannot be fought - and the variance path is additionally capped at the force actually available, so a high roll
 * cannot demand units the player does not have.</p>
 */
class RequiredCombatElementsTest {
    /** Runs the calculation with the campaign's fieldable force stubbed to {@code effectiveForces}. */
    private static int calculate(int effectiveForces, boolean bypassVariance, double varianceFactor) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        try (MockedStatic<ContractUtilities> contractUtilities = mockStatic(ContractUtilities.class)) {
            contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign))
                  .thenReturn(effectiveForces);
            return calculateRequiredCombatElements(campaign, bypassVariance, varianceFactor);
        }
    }

    // region bypass path

    @ParameterizedTest
    @CsvSource({
          "36, 24",   // 36 - floor(36/3) = 24
          "12, 8",
          "10, 7",    // floor(10/3) = 3
          "4, 3",     // floor(4/3) = 1
          "3, 2"
    })
    void bypassingVarianceRemovesAThirdOfTheForce(int effectiveForces, int expected) {
        assertEquals(expected, calculate(effectiveForces, true, 1.0),
              "the bypass path applies a flat one-third reduction, rounded down");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void bypassingVarianceStillDemandsAtLeastOneElement(int effectiveForces) {
        assertEquals(1, calculate(effectiveForces, true, 1.0),
              "a contract requiring no combat elements at all cannot be fought");
    }

    // endregion bypass path

    // region variance path

    @ParameterizedTest
    @CsvSource({
          "36, 1.0, 36",    // the whole force
          "36, 0.5, 18",
          "36, 0.75, 27",
          "36, 0.33, 11",   // 11.88 floors to 11
          "10, 0.66, 6"     // 6.6 floors to 6
    })
    void varianceScalesTheForceAndRoundsDown(int effectiveForces, double varianceFactor, int expected) {
        assertEquals(expected, calculate(effectiveForces, false, varianceFactor));
    }

    @Test
    void aVarianceFactorAboveOneCannotDemandMoreThanTheForceAvailable() {
        assertEquals(36, calculate(36, false, 1.5),
              "the requirement is capped at the force the player can actually field");
    }

    @ParameterizedTest
    @CsvSource({ "36, 0.0", "1, 0.1" })
    void varianceStillDemandsAtLeastOneElement(int effectiveForces, double varianceFactor) {
        assertEquals(1, calculate(effectiveForces, false, varianceFactor),
              "a contract requiring no combat elements at all cannot be fought");
    }

    /**
     * The available-force cap is applied after the one-element floor, so a campaign that can field nothing is asked for
     * nothing. The bypass path clamps the other way and yields one - the two paths only disagree at zero, which a
     * campaign with any units at all never reaches.
     */
    @Test
    void aCampaignThatCanFieldNothingIsAskedForNothing() {
        assertEquals(0, calculate(0, false, 1.0));
        assertEquals(1, calculate(0, true, 1.0), "the bypass path floors at one instead");
    }

    // endregion variance path
}
