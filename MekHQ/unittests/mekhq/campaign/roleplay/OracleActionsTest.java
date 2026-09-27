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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.roleplay.OracleActions.AskOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OracleActionsTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 1);

    /** 50/50 at chaos 5 is 10 / 50 / 91: 22 is a plain yes that triggers a random event; 23 does not. */
    private static final int YES_WITH_EVENT = 22;
    private static final int YES_WITHOUT_EVENT = 23;
    /** Focus rolls: 30 is NPC Action, 53 Move Toward A Thread, 60 Move Away From A Thread. */
    private static final int NPC_ACTION = 30;
    private static final int MOVE_TOWARD = 53;
    private static final int MOVE_AWAY = 60;

    private Roleplay roleplay;
    private OracleActions actions;

    @BeforeEach
    void setUp() {
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        for (OracleTable table : OracleTable.values()) {
            WeightedIntMap<String> pool = new WeightedIntMap<>();
            pool.add(1, table.name().toLowerCase());
            pools.put(table, pool);
        }
        RandomOracleGenerator generator = RandomOracleGenerator.createForTesting(pools);
        roleplay = new Roleplay();
        actions = new OracleActions(roleplay, () -> TODAY, () -> 500, () -> generator);
    }

    @AfterEach
    void tearDown() {
        RandomOracleGenerator.resetForTesting();
    }

    @Test
    void askLogsTheAnswerWithItsDetails() {
        AskOutcome outcome = actions.ask("  Is the militia hostile? ", FateChartOdds.FIFTY_FIFTY, YES_WITHOUT_EVENT, 1);

        assertEquals(new OracleAnswer("Is the militia hostile?", FateChartOdds.FIFTY_FIFTY, 5, YES_WITHOUT_EVENT,
              FateChartAnswer.NORMAL_YES), outcome.answer());
        JournalEntry log = roleplay.getOracleLog().get(0);
        assertEquals(outcome.log(), log);
        assertEquals(JournalEntryType.FATE_CHART, log.getType());
        assertEquals(TODAY, log.getDate());
        assertEquals(outcome.answer(), log.getAnswer());
        assertTrue(log.getText().contains("Is the militia hostile?"), log.getText());
    }

    @Test
    void npcEventsPickFromTheCastAndTagTheLog() {
        OracleCharacter ana = roleplay.addCharacter("Ana");
        AskOutcome outcome = actions.ask("", FateChartOdds.FIFTY_FIFTY, YES_WITH_EVENT, NPC_ACTION);

        assertEquals(RandomEventFocus.NPC_ACTION, outcome.fate().randomEventFocus());
        assertEquals(ana, outcome.character());
        assertEquals(JournalEntryType.RANDOM_EVENT, outcome.log().getType());
        assertEquals(Set.of(ana.getId()), outcome.log().getCharacters());
        assertFalse(outcome.isMissingCharacter());
    }

    @Test
    void npcEventsWithAnEmptyCastSaySo() {
        AskOutcome outcome = actions.ask("", FateChartOdds.FIFTY_FIFTY, YES_WITH_EVENT, NPC_ACTION);
        assertNull(outcome.character());
        assertTrue(outcome.isMissingCharacter());
        assertTrue(outcome.log().getCharacters().isEmpty());
    }

    @Test
    void threadEventsMoveAThreadAndTagTheLog() {
        PlotThread thread = actions.createThread(" Who sent Natasha? ", PlotThreadLength.SHORT);
        assertEquals("Who sent Natasha?", thread.getName());

        AskOutcome forward = actions.ask("", FateChartOdds.FIFTY_FIFTY, YES_WITH_EVENT, MOVE_TOWARD);
        assertEquals(thread, forward.thread().thread());
        assertEquals(1, thread.getRevealedSteps());
        assertEquals(TODAY, thread.getRevealDate(1));
        assertEquals(Set.of(thread.getId()), forward.log().getThreads());

        AskOutcome back = actions.ask("", FateChartOdds.FIFTY_FIFTY, YES_WITH_EVENT, MOVE_AWAY);
        assertEquals(thread, back.thread().thread());
        assertNull(back.thread().revealed());
        assertEquals(0, thread.getRevealedSteps());
    }

    @Test
    void threadEventsWithNothingToMoveSaySo() {
        AskOutcome forward = actions.ask("", FateChartOdds.FIFTY_FIFTY, YES_WITH_EVENT, MOVE_TOWARD);
        assertNull(forward.thread());
        assertTrue(forward.isMissingThread());
    }

    @Test
    void threadActionsAreLoggedAndTagged() {
        PlotThread thread = actions.createThread("Hunt", PlotThreadLength.SHORT);
        PlotThreadStep step = actions.revealNextStep(thread);
        assertNotNull(step);
        actions.deleteThread(thread);

        assertTrue(roleplay.getPlotThreads().isEmpty());
        List<JournalEntry> log = roleplay.getOracleLog();
        assertEquals(3, log.size(), "created, revealed and deleted");
        for (JournalEntry entry : log) {
            assertEquals(JournalEntryType.THREAD, entry.getType());
            assertEquals(Set.of(thread.getId()), entry.getThreads());
        }
        assertTrue(log.get(1).getText().contains("adventure"), "the reveal lists the step's concepts");
    }

    @Test
    void conceptsAreLogged() {
        List<Concepts.Concept> concepts = actions.rollConcepts(List.of(OracleTable.THEMES_FEAR));
        assertEquals("themes_fear", concepts.get(0).meaning());
        assertEquals(JournalEntryType.CONCEPTS, roleplay.getOracleLog().get(0).getType());

        assertTrue(actions.rollConcepts(List.of()).isEmpty());
        assertEquals(1, roleplay.getOracleLog().size(), "an empty roll is not logged");
    }
}
