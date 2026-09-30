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

import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.protomeks.ProtoMekJumpJet;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Part data for Meks and ProtoMeks: a ProtoMek keeps one jump jet part per jump jet through a reload, and a gyro weighs
 * and costs what the rules say for superheavy, gyro-less and primitive-engine Meks (issue #10206).
 */
class MekPartDataTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static int jumpJetParts(Unit unit) {
        return PartsScenario.unitParts(unit, JumpJet.class).size()
              + PartsScenario.unitParts(unit, ProtoMekJumpJet.class).size();
    }

    private MekGyro gyroOf(UnitFixture fixture) {
        Unit unit = scenario.withUnit(fixture);
        return PartsScenario.unitParts(unit, MekGyro.class).getFirst();
    }

    @Test
    void aProtoMekKeepsOneJumpJetPartPerJumpJetThroughAReload() {
        Unit minotaur = scenario.withUnit(UnitFixture.MINOTAUR_PROTOMEK);
        int jumpMP = minotaur.getEntity().getOriginalJumpMP();
        assertEquals(jumpMP, jumpJetParts(minotaur), "A new ProtoMek has one jump jet part per point of jump MP");

        // Loading a campaign builds the unit's parts again from those it already has
        minotaur.initializeParts(true);

        assertEquals(jumpMP, jumpJetParts(minotaur), "Loading the campaign adds no second set of jump jets");
    }

    @Test
    void duplicateJumpJetsFromAnEarlierLoadAreRemoved() {
        Unit minotaur = scenario.withUnit(UnitFixture.MINOTAUR_PROTOMEK);
        int jumpMP = minotaur.getEntity().getOriginalJumpMP();
        for (int jumpJet = 0; jumpJet < jumpMP; jumpJet++) {
            ProtoMekJumpJet duplicate = new ProtoMekJumpJet((int) minotaur.getEntity().getWeight(),
                  scenario.getCampaign());
            minotaur.addPart(duplicate);
            scenario.getCampaign().getQuartermaster().addPart(duplicate, 0, false);
        }

        minotaur.initializeParts(true);

        assertEquals(jumpMP, jumpJetParts(minotaur));
        assertTrue(PartsScenario.unitParts(minotaur, ProtoMekJumpJet.class).isEmpty(),
              "The jump jets tied to the ProtoMek's equipment stay; the duplicates go");
        for (Part part : scenario.getWarehouse().getParts()) {
            assertTrue(!(part instanceof ProtoMekJumpJet), "No duplicate is left in the warehouse");
        }
    }

    @Test
    void aSuperheavyGyroWeighsAndCostsTwiceTheBase() {
        // Omega SHP-5R: engine rating 300, so a base of 3 tons, doubled for a superheavy gyro
        MekGyro gyro = gyroOf(UnitFixture.OMEGA_SHP_5R);

        assertEquals(6.0, gyro.getTonnage());
        assertEquals(Money.of(500000.0 * 6), gyro.getStickerPrice(), "Superheavy gyros cost 500,000 per ton");
    }

    @Test
    void aMekWithoutAGyroHasAWeightlessFreeGyroPart() {
        // The Skinwalker's interface cockpit is what lets it go without a gyro; such Meks are rare
        MekGyro gyro = gyroOf(UnitFixture.SKINWALKER_A);

        assertEquals(0.0, gyro.getTonnage());
        assertEquals(Money.zero(), gyro.getStickerPrice());
    }

    @Test
    void aPrimitiveEngineNeedsTheHeavierGyroItsRatingCalls() {
        // Lumberjack LM1A: a primitive engine rated 220, where walking MP times tonnage is only 180
        MekGyro gyro = gyroOf(UnitFixture.LUMBERJACK_LM1A);

        assertEquals(3.0, gyro.getTonnage(), "Gyro weight follows the engine rating: 220 / 100, rounded up");
    }
}
