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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.enums.Gender;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckRoll;
import mekhq.campaign.personnel.skills.AttributeCheck;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.roleplay.RoleplayChecks.Opponent;
import mekhq.campaign.roleplay.RoleplayChecks.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

class RoleplayChecksTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 1);
    private static final CheckTrait WILLPOWER = CheckTrait.attributes(SkillAttribute.WILLPOWER, null);

    private Roleplay roleplay;
    private final Deque<Integer> rolls = new ArrayDeque<>();
    private final List<String> reports = new ArrayList<>();
    private boolean edgeAllowed = true;
    private RoleplayChecks checks;

    @BeforeEach
    void setUp() {
        roleplay = new Roleplay();
        OracleActions actions = new OracleActions(roleplay, () -> TODAY, () -> 500, () -> null);
        checks = new RoleplayChecks(actions, () -> new Settings(false, false, TODAY, edgeAllowed), reports::add,
              rollType -> {
                  int total = rolls.pop();
                  return new ActionCheckRoll(total, List.of(total / 2, total - total / 2));
              });
    }

    /** Someone whose Willpower check needs 7 or more. */
    private static Person person(final String name, final int edge) {
        Person person = mock(Person.class);
        when(person.getFullName()).thenReturn(name);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getCurrentEdge()).thenReturn(edge);
        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.getHyperlinkedFullTitle()).thenReturn(name);
        when(person.checkAttribute(SkillAttribute.WILLPOWER)).thenAnswer(invocation -> new AttributeCheck(person,
              new TargetRoll(7, "base"), SkillAttribute.WILLPOWER, SkillAttribute.NO_ATTRIBUTE));
        return person;
    }

    private Opponent rated(final String name, final NpcRating rating, final CheckDifficulty difficulty) {
        return Opponent.rated(roleplay.addCharacter(name), rating, difficulty, 0);
    }

    @Test
    void theHigherMarginWins() {
        rolls.addAll(List.of(10, 9));
        CheckRecord record = checks.opposed(rated("Rook", NpcRating.VETERAN, CheckDifficulty.NORMAL),
              rated("Dace", NpcRating.REGULAR, CheckDifficulty.NORMAL), "  Haggle ", roleplay.getCharacters(), null);

        assertTrue(record.opposed());
        assertEquals("Haggle", record.reason());
        assertEquals(3, record.sides().get(0).margin());
        assertEquals(1, record.sides().get(1).margin());
        assertTrue(record.sides().get(0).won());
        assertFalse(record.sides().get(1).won());
        assertEquals(2, record.winningDifference());
        assertEquals("7+", record.sides().get(0).target());
    }

    @Test
    void theDefenderWinsATie() {
        rolls.addAll(List.of(9, 9));
        CheckRecord record = checks.opposed(rated("Rook", NpcRating.REGULAR, CheckDifficulty.NORMAL),
              rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        assertEquals(0, record.winningDifference());
        assertFalse(record.sides().get(0).won());
        assertTrue(record.sides().get(1).won());
        assertTrue(roleplay.getOracleLog().get(0).getText().contains("Guard wins the tie"),
              roleplay.getOracleLog().get(0).getText());
    }

    @Test
    void theLoserReRollsOnceWithEdge() {
        Person natasha = person("Natasha", 2);
        rolls.addAll(List.of(6, 9, 11));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0,
              true), rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        verify(natasha, times(1)).spendEdge();
        assertTrue(record.sides().get(0).usedEdge());
        assertEquals(11, record.sides().get(0).roll());
        assertTrue(record.sides().get(0).won());
        assertTrue(rolls.isEmpty());
    }

    @Test
    void theDefenderCanReRollALossToo() {
        Person natasha = person("Natasha", 1);
        rolls.addAll(List.of(12, 7, 8));
        CheckRecord record = checks.opposed(rated("Rook", NpcRating.REGULAR, CheckDifficulty.NORMAL),
              Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0, true), "",
              roleplay.getCharacters(), null);

        verify(natasha).spendEdge();
        assertTrue(record.sides().get(1).usedEdge());
        assertTrue(record.sides().get(0).won(), "the re-roll of 8 still loses to a margin of 4");
    }

    @Test
    void noEdgeWhenTheCampaignDoesNotUseIt() {
        edgeAllowed = false;
        Person natasha = person("Natasha", 2);
        rolls.addAll(List.of(6, 9));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0,
              true), rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        verify(natasha, never()).spendEdge();
        assertFalse(record.sides().get(0).usedEdge());
    }

    @Test
    void anOpposedCheckIsLoggedTaggedAndReported() {
        Person natasha = person("Natasha", 0);
        OracleCharacter linked = roleplay.addLinkedCharacter(natasha.getId(), "Natasha");
        OracleCharacter guard = roleplay.addCharacter("Guard");
        PlotThread thread = mock(PlotThread.class);
        when(thread.getId()).thenReturn(UUID.randomUUID());
        rolls.addAll(List.of(9, 5));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.HARD, -1,
              false), Opponent.rated(guard, NpcRating.VETERAN, CheckDifficulty.NORMAL, 0), "Stare him down",
              roleplay.getCharacters(), thread);

        JournalEntry log = roleplay.getOracleLog().get(0);
        assertEquals(JournalEntryType.CHECK, log.getType());
        assertEquals(record, log.getCheck());
        assertEquals(Set.of(linked.getId(), guard.getId()), log.getCharacters());
        assertEquals(Set.of(thread.getId()), log.getThreads());
        assertEquals(NpcRating.VETERAN, guard.getRating(), "the rating is remembered");
        assertEquals("8+", record.sides().get(0).target(), "7, Hard +2, other -1");
        assertTrue(record.sides().get(0).action().startsWith("Willpower (Hard +2"), record.sides().get(0).action());
        assertTrue(log.getText().contains("Stare him down"), log.getText());
        assertTrue(log.getText().contains("Natasha wins by 3"), log.getText());
        assertEquals(1, reports.size());
    }

    @Test
    void aGroupCheckRollsForEveryone() {
        Person natasha = person("Natasha", 0);
        Person rook = person("Rook", 0);
        roleplay.addLinkedCharacter(rook.getId(), "Rook");
        CheckRecord record = checks.check(List.of(natasha, rook), WILLPOWER, CheckDifficulty.EASY, 0, false,
              "Hold the line", roleplay.getCharacters(), null);

        assertFalse(record.opposed());
        assertEquals(2, record.sides().size());
        assertEquals("6+", record.sides().get(0).target());
        assertEquals(2, reports.size());
        JournalEntry log = roleplay.getOracleLog().get(0);
        assertEquals(1, log.getCharacters().size(), "only Rook is in the cast");
        assertTrue(log.getText().contains("passed"), log.getText());
    }

    @Test
    void checksAndRatingsSurviveSaving() throws Exception {
        OracleCharacter guard = roleplay.addCharacter("Guard & Co");
        rolls.addAll(List.of(9, 5));
        CheckRecord record = checks.opposed(rated("Rook", NpcRating.ELITE, CheckDifficulty.NORMAL),
              Opponent.rated(guard, NpcRating.GREEN, CheckDifficulty.VERY_HARD, 0), "<Sneak>",
              roleplay.getCharacters(), null);

        StringWriter text = new StringWriter();
        try (PrintWriter writer = new PrintWriter(text)) {
            roleplay.writeToXML(writer, 0);
        }
        Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                          .parse(new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        Roleplay loaded = Roleplay.generateInstanceFromXML(node);

        assertEquals(record, loaded.getOracleLog().get(0).getCheck());
        assertEquals(NpcRating.GREEN, loaded.getCharacter(guard.getId()).getRating());
        assertNull(loaded.getJournal().isEmpty() ? null : loaded.getJournal().get(0).getCheck());
    }

    @Test
    void typedTextIsEscapedInTheDailyReport() {
        rolls.addAll(List.of(9, 5));
        checks.opposed(rated("<b>Rook</b>", NpcRating.REGULAR, CheckDifficulty.NORMAL),
              rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "Sneak <past> & hide",
              roleplay.getCharacters(), null);

        String report = reports.get(0);
        assertFalse(report.contains("<b>Rook</b>"), report);
        assertTrue(report.contains("&lt;b&gt;Rook&lt;/b&gt;"), report);
        assertTrue(report.contains("Sneak &lt;past&gt; &amp; hide"), report);
        assertTrue(roleplay.getOracleLog().get(0).getText().contains("Sneak <past> & hide"),
              "the journal keeps the text as typed");
    }

    @Test
    void theGroupReportEscapesTheReason() {
        Person natasha = person("Natasha", 0);
        checks.check(List.of(natasha), WILLPOWER, CheckDifficulty.NORMAL, 0, false, "<i>Hold</i>",
              roleplay.getCharacters(), null);

        assertTrue(reports.get(0).contains("&lt;i&gt;Hold&lt;/i&gt;"), reports.get(0));
    }

    @Test
    void edgeIsNeverSpentOnAHopelessCheck() {
        Person natasha = person("Natasha", 2);
        when(natasha.checkAttribute(SkillAttribute.WILLPOWER)).thenAnswer(invocation -> new AttributeCheck(natasha,
              new TargetRoll(TargetRoll.IMPOSSIBLE, "no chance"), SkillAttribute.WILLPOWER,
              SkillAttribute.NO_ATTRIBUTE));
        rolls.addAll(List.of(12, 2));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0,
              true), rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        verify(natasha, never()).spendEdge();
        assertEquals(-10, record.sides().get(0).margin());
        assertEquals("–", record.sides().get(0).target());
        assertFalse(record.sides().get(0).won());
    }

    @Test
    void aWinnerNeverReRolls() {
        Person natasha = person("Natasha", 2);
        rolls.addAll(List.of(12, 2));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0,
              true), rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        verify(natasha, never()).spendEdge();
        assertTrue(record.sides().get(0).won());
        assertFalse(record.sides().get(0).usedEdge());
    }

    @Test
    void someoneWithNoEdgeLeftCannotReRoll() {
        Person natasha = person("Natasha", 0);
        rolls.addAll(List.of(2, 12));
        CheckRecord record = checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0,
              true), rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        verify(natasha, never()).spendEdge();
        assertFalse(record.sides().get(0).usedEdge());
        assertTrue(rolls.isEmpty());
    }

    @Test
    void aRatingWithoutACastMemberIsNotRemembered() {
        rolls.addAll(List.of(9, 5));
        CheckRecord record = checks.opposed(Opponent.rated(null, NpcRating.ELITE, CheckDifficulty.NORMAL, 0),
              rated("Guard", NpcRating.REGULAR, CheckDifficulty.NORMAL), "", roleplay.getCharacters(), null);

        assertEquals("", record.sides().get(0).name());
        assertEquals("6+", record.sides().get(0).target());
        assertEquals(Set.of(roleplay.getCharacters().get(0).getId()), roleplay.getOracleLog().get(0).getCharacters());
    }

    @Test
    void aPersonInTheCastIsTaggedOnceEvenOnBothSides() {
        Person natasha = person("Natasha", 0);
        OracleCharacter linked = roleplay.addLinkedCharacter(natasha.getId(), "Natasha");
        rolls.addAll(List.of(9, 5));
        checks.opposed(Opponent.person(natasha, null, WILLPOWER, CheckDifficulty.NORMAL, 0, false),
              Opponent.person(natasha, linked, WILLPOWER, CheckDifficulty.NORMAL, 0, false), "",
              roleplay.getCharacters(), null);

        assertEquals(Set.of(linked.getId()), roleplay.getOracleLog().get(0).getCharacters());
    }

    @Test
    void edgeTracksTheCampaignAndThePerson() {
        assertTrue(checks.isEdgeAllowed());
        assertTrue(checks.canUseEdge(person("Natasha", 1)));
        assertFalse(checks.canUseEdge(person("Rook", 0)));
        assertFalse(checks.canUseEdge(null));
        edgeAllowed = false;
        assertFalse(checks.canUseEdge(person("Natasha", 1)));
    }

    @Test
    void targetsIncludeTheModifier() {
        Person natasha = person("Natasha", 0);
        assertEquals("9+", checks.target(natasha, WILLPOWER, 2).describe());
        assertEquals("6+", checks.target(natasha, WILLPOWER, -1).describe());
        assertTrue(RoleplayChecks.isTrained(natasha, WILLPOWER), "attributes are always trained");
    }
}
