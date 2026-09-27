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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import org.junit.jupiter.api.Test;

/** The small value types behind checks: targets, traits, difficulty levels, ratings and how a check is written up. */
class CheckValuesTest {
    // region Targets

    @Test
    void targetsReadAsPlayersExpect() {
        assertEquals("7+", CheckTarget.of(7).describe());
        assertEquals("7-", new CheckTarget(7, true, false, RollType.NORMAL).describe());
        assertEquals("–", new CheckTarget(7, false, true, RollType.NORMAL).describe());
    }

    @Test
    void onlyReachableTargetsCanSucceed() {
        assertTrue(CheckTarget.of(12).canSucceed());
        assertFalse(CheckTarget.of(13).canSucceed());
        assertTrue(new CheckTarget(2, true, false, RollType.NORMAL).canSucceed());
        assertFalse(new CheckTarget(1, true, false, RollType.NORMAL).canSucceed());
        assertFalse(new CheckTarget(7, false, true, RollType.NORMAL).canSucceed());
    }

    @Test
    void meetingTheTargetSucceedsByZero() {
        assertEquals(0, CheckTarget.of(8).margin(8));
        assertTrue(CheckTarget.of(8).succeeds(8));
        assertFalse(CheckTarget.of(8).succeeds(7));
        assertTrue(new CheckTarget(8, true, false, RollType.NORMAL).succeeds(8));
        assertFalse(new CheckTarget(8, true, false, RollType.NORMAL).succeeds(9));
    }

    @Test
    void extremeTargetsDoNotOverflow() {
        assertEquals(10, CheckTarget.of(Integer.MIN_VALUE).margin(12));
        assertEquals(-10, CheckTarget.of(Integer.MAX_VALUE).margin(2));
    }

    // endregion Targets

    // region Traits, difficulty and ratings

    @Test
    void traitsNameWhatIsRolled() {
        assertEquals("Negotiation", CheckTrait.skill("Negotiation").getLabel());
        assertEquals("Acting", CheckTrait.skill("Acting (RP Only)").getLabel());
        assertTrue(CheckTrait.skill("Negotiation").isSkill());

        CheckTrait single = CheckTrait.attributes(SkillAttribute.WILLPOWER, SkillAttribute.NO_ATTRIBUTE);
        assertFalse(single.isSkill());
        assertNull(single.second());
        assertEquals(SkillAttribute.WILLPOWER.getLabel(), single.getLabel());
        assertEquals(SkillAttribute.BODY.getLabel() + "-" + SkillAttribute.STRENGTH.getLabel(),
              CheckTrait.attributes(SkillAttribute.BODY, SkillAttribute.STRENGTH).getLabel());
        assertEquals(single, CheckTrait.attributes(SkillAttribute.WILLPOWER, null));
    }

    @Test
    void difficultyLevelsMatchTheAgreedModifiers() {
        assertEquals(List.of(-3, -1, 0, 2, 4),
              Arrays.stream(CheckDifficulty.values()).map(CheckDifficulty::getModifier).toList());
        assertEquals("−3", CheckDifficulty.VERY_EASY.getSignedModifier());
        assertEquals("±0", CheckDifficulty.NORMAL.getSignedModifier());
        assertEquals("+4", CheckDifficulty.VERY_HARD.getSignedModifier());
        for (CheckDifficulty difficulty : CheckDifficulty.values()) {
            assertFalse(difficulty.getLabel().startsWith("!"), difficulty.name());
        }
    }

    @Test
    void ratingsRunFromUltraGreenToLegendary() {
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4),
              Arrays.stream(NpcRating.values()).map(NpcRating::getTargetNumber).toList());
        assertEquals(NpcRating.VETERAN.getLabel() + " · 7+", NpcRating.VETERAN.toString());
        for (NpcRating rating : NpcRating.values()) {
            assertFalse(rating.getLabel().startsWith("!"), rating.name());
        }
        assertEquals("9+", RoleplayChecks.target(NpcRating.REGULAR, 1).describe());
    }

    // endregion Traits, difficulty and ratings

    // region Write-up

    private static CheckRecord.Side side(final String name, final int roll, final String target, final int margin,
          final boolean edge, final boolean won) {
        return new CheckRecord.Side(name, UUID.randomUUID(), "Negotiation (Hard +2)", target, roll, List.of(3, 4),
              margin, edge, won);
    }

    @Test
    void aSingleCheckIsOneLine() {
        String text = OracleActions.describeCheck(new CheckRecord(false, "",
              List.of(side("Rook", 9, "7+", 2, false, true))));
        assertEquals("Rook, Negotiation (Hard +2): rolled 9 against 7+, passed by 2.", text);
    }

    @Test
    void aFailureSaysByHowMuchAndMentionsEdge() {
        String text = OracleActions.describeCheck(new CheckRecord(false, "Haggle",
              List.of(side("Rook", 5, "7+", -2, true, false))));
        assertEquals("“Haggle”\nRook, Negotiation (Hard +2): rolled 5 against 7+, failed by 2. Edge spent on a "
                           + "re-roll.", text);
    }

    @Test
    void aGroupCheckCountsThePasses() {
        String text = OracleActions.describeCheck(new CheckRecord(false, "", List.of(
              side("Rook", 9, "7+", 2, false, true), side("Dace", 4, "7+", -3, false, false))));
        assertTrue(text.startsWith("Group check: 1 of 2 passed.\n"), text);
    }

    @Test
    void anOpposedCheckNamesTheWinner() {
        CheckRecord won = new CheckRecord(true, "", List.of(side("Rook", 9, "7+", 2, false, true),
              side("Dace", 7, "8+", -1, false, false)));
        assertTrue(OracleActions.describeCheck(won).startsWith("Opposed check: Rook wins by 3.\n"));
        assertEquals(3, won.winningDifference());
        assertEquals(1, won.countWon());

        CheckRecord tie = new CheckRecord(true, "", List.of(side("Rook", 8, "7+", 1, false, false),
              side("Dace", 9, "8+", 1, false, true)));
        assertTrue(OracleActions.describeCheck(tie).startsWith("Opposed check: Dace wins the tie.\n"));
    }

    @Test
    void actionsSpellOutTheSituation() {
        assertEquals("Stealth (Normal)", RoleplayChecks.describeAction("Stealth", CheckDifficulty.NORMAL, 0));
        assertEquals("Stealth (Hard +2)", RoleplayChecks.describeAction("Stealth", CheckDifficulty.HARD, 0));
        assertEquals("Stealth (Easy −1, other +3)",
              RoleplayChecks.describeAction("Stealth", CheckDifficulty.EASY, 3));
    }

    @Test
    void onlyOpposedChecksHaveAWinningDifference() {
        CheckRecord group = new CheckRecord(false, "", List.of(side("Rook", 9, "7+", 2, false, true),
              side("Dace", 4, "7+", -3, false, false)));
        assertEquals(0, group.winningDifference());
    }

    // endregion Write-up
}
