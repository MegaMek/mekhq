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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link ChaosContractObjectiveDetermination#determineContractObjectiveType(int)} - which bracket the 2d6 roll
 * (plus a generation modifier, clamped to 1..13) lands in, and the follow-up roll that picks the opposing objective.
 *
 * <p>Each call rolls d6 more than once, so the stub feeds a sequence: the first value is the bracket roll and the rest
 * are consumed, in order, by the player/opposing sub-rolls. Player objectives that are fixed (raid, invasion) consume
 * no extra roll.</p>
 *
 * <p>Assertions compare the {@link ChaosObjectiveType} category rather than the concrete CamOps objective:
 * {@link ChaosObjectiveType#getCamOpsObjectiveType()} picks a <em>random</em> CamOps variant among the candidates that
 * map back to a category (e.g. RAID -> Recon Raid or Diversionary Raid), so the concrete variant is not deterministic
 * across two independent invocations.</p>
 */
class ChaosContractObjectiveDeterminationTest {

    /** The first roll is the bracket; any further rolls feed the sub-objective picks in call order. */
    private static ContractObjectiveData determine(final int modifier, final Integer firstRoll,
          final Integer... furtherRolls) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(firstRoll, furtherRolls);
            return ChaosContractObjectiveDetermination.determineContractObjectiveType(modifier);
        }
    }

    @Test
    void raidBracketFixesThePlayerObjectiveAndRollsTheOpposingOne() {
        // Bracket roll 7 -> raid; opposing roll 11 -> RAID.
        ContractObjectiveData data = determine(0, 7, 11);

        assertEquals(ChaosObjectiveType.RAID, data.playerObjectiveType().getChaosObjectiveType());
        assertEquals(ChaosObjectiveType.RAID, data.opposingObjectiveType().getChaosObjectiveType());
    }

    @Test
    void expeditionBracketRollsBothPlayerAndOpposingObjectives() {
        // Bracket roll 2 -> expedition; player roll 2 -> EXPEDITION; opposing roll 9 -> RAID.
        ContractObjectiveData data = determine(0, 2, 2, 9);

        assertEquals(ChaosObjectiveType.EXPEDITION, data.playerObjectiveType().getChaosObjectiveType());
        assertEquals(ChaosObjectiveType.RAID, data.opposingObjectiveType().getChaosObjectiveType());
    }

    @Test
    void garrisonBracketRollsBothObjectives() {
        // Bracket roll 5 -> garrison; player roll 12 -> GARRISON; opposing roll 12 -> INVASION.
        ContractObjectiveData data = determine(0, 5, 12, 12);

        assertEquals(ChaosObjectiveType.GARRISON, data.playerObjectiveType().getChaosObjectiveType());
        assertEquals(ChaosObjectiveType.INVASION, data.opposingObjectiveType().getChaosObjectiveType());
    }

    @Test
    void aPositiveModifierPushesTheRollIntoTheInvasionBracket() {
        // Bracket roll 6 + modifier 5 = 11 -> invasion; opposing roll 2 -> EXPEDITION.
        ContractObjectiveData data = determine(5, 6, 2);

        assertEquals(ChaosObjectiveType.INVASION, data.playerObjectiveType().getChaosObjectiveType());
        assertEquals(ChaosObjectiveType.EXPEDITION, data.opposingObjectiveType().getChaosObjectiveType());
    }

    @Test
    void aLargeNegativeModifierIsClampedIntoTheLowestBracket() {
        // Bracket roll 2 + modifier -5 = -3, clamped to 1 -> expedition (must not throw).
        ContractObjectiveData data = determine(-5, 2, 2, 9);

        assertEquals(ChaosObjectiveType.EXPEDITION, data.playerObjectiveType().getChaosObjectiveType());
        assertEquals(ChaosObjectiveType.RAID, data.opposingObjectiveType().getChaosObjectiveType());
    }
}
