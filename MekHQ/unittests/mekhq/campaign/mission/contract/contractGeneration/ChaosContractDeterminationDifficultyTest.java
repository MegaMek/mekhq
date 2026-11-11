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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import megamek.client.ratgenerator.FactionRecord;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.universe.RATGeneratorConnector;
import org.junit.jupiter.api.Test;

class ChaosContractDeterminationDifficultyTest {
    private static final double DELTA = 0.0001;
    /** The Total Warfare skill multiplier for a 3/4 (Veteran) crew. */
    private static final double VETERAN_MULTIPLIER = 1.32;
    /** Regular (4/5) crews are the neutral point of the skill table. */
    private static final ToDoubleFunction<Entity> REGULAR_CREWS = unit -> 1.0;

    private static Entity unit(final boolean isMek, final int genericBattleValue) {
        Entity entity = mock(Entity.class);
        when(entity.isMek()).thenReturn(isMek);
        when(entity.getGenericBattleValue()).thenReturn(genericBattleValue);
        return entity;
    }

    // --- modifySkillLevelBasedOnFaction ---

    @Test
    void socIsRaisedToElite() {
        assertEquals(SkillLevel.ELITE,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("SOC", SkillLevel.GREEN));
        assertEquals(SkillLevel.ELITE,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("SOC", SkillLevel.VETERAN));
    }

    @Test
    void socAlreadyAboveEliteKeepsItsSkill() {
        assertEquals(SkillLevel.HEROIC,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("SOC", SkillLevel.HEROIC));
        assertEquals(SkillLevel.LEGENDARY,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("SOC", SkillLevel.LEGENDARY));
    }

    @Test
    void clanSkillIsNotBumpedAgain() {
        // Generation already applies the Clan edge; the difficulty estimate must take the rolled skill as-is.
        assertEquals(SkillLevel.VETERAN,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("CJF", SkillLevel.VETERAN));
    }

    @Test
    void otherFactionsAreUnchanged() {
        assertEquals(SkillLevel.REGULAR,
              ChaosContractDeterminationDifficulty.modifySkillLevelBasedOnFaction("FS", SkillLevel.REGULAR));
    }

    // --- estimatePlayerPower ---

    @Test
    void noUnitsHaveNoPower() {
        assertEquals(0, ChaosContractDeterminationDifficulty.estimatePlayerPower(List.of(), REGULAR_CREWS), DELTA);
    }

    @Test
    void regularCrewsUseGenericBattleValueUnchanged() {
        List<Entity> units = List.of(unit(true, 1000), unit(true, 1200));

        assertEquals(1100, ChaosContractDeterminationDifficulty.estimatePlayerPower(units, REGULAR_CREWS), DELTA);
    }

    @Test
    void crewSkillScalesPlayerPower() {
        List<Entity> units = List.of(unit(true, 1000));

        assertEquals(1000 * VETERAN_MULTIPLIER,
              ChaosContractDeterminationDifficulty.estimatePlayerPower(units, unit -> VETERAN_MULTIPLIER), DELTA);
    }

    @Test
    void crewSkillIsAppliedPerUnit() {
        Entity veteranMek = unit(true, 1000);
        Entity regularMek = unit(true, 1000);
        ToDoubleFunction<Entity> multipliers = unit -> (unit == veteranMek) ? VETERAN_MULTIPLIER : 1.0;

        assertEquals((1000 * VETERAN_MULTIPLIER + 1000) / 2,
              ChaosContractDeterminationDifficulty.estimatePlayerPower(List.of(veteranMek, regularMek), multipliers),
              DELTA);
    }

    @Test
    void nonMeksAreLeftOutWhenTheForceFieldsMeks() {
        Entity infantry = unit(false, 50);
        List<Entity> units = List.of(unit(true, 1000), unit(true, 1200), infantry);

        // Infantry would otherwise drag the average from 1100 down to 750.
        assertEquals(1100, ChaosContractDeterminationDifficulty.estimatePlayerPower(units, REGULAR_CREWS), DELTA);
        verify(infantry, never()).getGenericBattleValue();
    }

    @Test
    void forceWithoutMeksIsAveragedAcrossEverything() {
        List<Entity> units = List.of(unit(false, 800), unit(false, 50));

        assertEquals(425, ChaosContractDeterminationDifficulty.estimatePlayerPower(units, REGULAR_CREWS), DELTA);
    }

    @Test
    void fullBattleValueIsNeverCalculated() {
        Entity mek = unit(true, 1000);

        ChaosContractDeterminationDifficulty.estimatePlayerPower(List.of(mek), REGULAR_CREWS);

        verify(mek, never()).calculateBattleValue();
    }

    // --- equipment rating reaching the RAT generator ---

    @Test
    void equipmentRatingIsTranslatedToTheFactionsRatingName() {
        FactionRecord innerSphere = mock(FactionRecord.class);
        ArrayList<String> ratings = new ArrayList<>(List.of("F", "D", "C", "B", "A"));
        when(innerSphere.getRatingLevels()).thenReturn(ratings);
        when(innerSphere.getRatingLevelSystem()).thenReturn(ratings);

        // The RAT generator matches ratings by name, so a bare number such as "2" would match nothing.
        assertEquals("C", RATGeneratorConnector.getFactionSpecificRating(innerSphere,
              DragoonRating.DRAGOON_C.getRating()));
        // A* sits above the top of a five-step ladder and is capped to it.
        assertEquals("A", RATGeneratorConnector.getFactionSpecificRating(innerSphere,
              DragoonRating.DRAGOON_ASTAR.getRating()));
    }
}
