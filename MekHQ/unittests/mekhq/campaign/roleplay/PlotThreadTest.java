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

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.roleplay.Concepts.Concept;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Node;

class PlotThreadTest {
    private RandomOracleGenerator generator;

    @BeforeEach
    void setUp() {
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        for (OracleTable table : OracleTable.values()) {
            WeightedIntMap<String> pool = new WeightedIntMap<>();
            pool.add(1, table.name().toLowerCase() + " & <co>");
            pools.put(table, pool);
        }
        generator = RandomOracleGenerator.createForTesting(pools);
    }

    @AfterEach
    void tearDown() {
        RandomOracleGenerator.resetForTesting();
    }

    @ParameterizedTest
    @EnumSource(PlotThreadLength.class)
    void createRollsEveryStepSecretly(final PlotThreadLength length) {
        PlotThread thread = PlotThread.create("Hunt", length, generator);

        assertEquals(length.getSteps(), thread.getSteps().size());
        assertEquals(0, thread.getRevealedSteps());
        assertTrue(thread.getRevealed().isEmpty());

        for (PlotThreadStep step : thread.getSteps()) {
            boolean flashpoint = step.number() % 5 == 0 && step.number() < length.getSteps();
            assertEquals(flashpoint, step.majorRevelation(), "step " + step.number());
            assertEquals(step.number() == length.getSteps(), step.conclusion(), "step " + step.number());

            List<Concept> concepts = step.concepts();
            assertEquals(4, concepts.size());
            assertTrue(concepts.get(0).table().name().startsWith("THEMES_"));
            List<OracleTable> adventure = concepts.subList(1, 4).stream().map(Concept::table).toList();
            assertTrue(adventure.stream().allMatch(table -> table.name().startsWith("ADVENTURE_")));
            assertEquals(3, new HashSet<>(adventure).size(), "adventure tables must differ");
            if (flashpoint) {
                assertEquals(PlotThread.MAJOR_REVELATION_TABLES, adventure);
            }
            if (step.conclusion()) {
                assertEquals(PlotThread.CONCLUSION_TABLES, adventure);
            }
            concepts.forEach(concept -> assertNotNull(concept.meaning()));
        }
    }

    @Test
    void flashpointsFallOnEveryFifthStepBeforeTheLast() {
        assertEquals(List.of(5), flashpoints(PlotThreadLength.SHORT));
        assertEquals(List.of(5, 10), flashpoints(PlotThreadLength.MEDIUM));
        assertEquals(List.of(5, 10, 15), flashpoints(PlotThreadLength.LONG));
    }

    private static List<Integer> flashpoints(final PlotThreadLength length) {
        return java.util.stream.IntStream.rangeClosed(1, length.getSteps())
                     .filter(number -> PlotThread.isFlashpoint(number, length)).boxed().toList();
    }

    @Test
    void revealsStepsInOrderUntilComplete() {
        PlotThread thread = PlotThread.create("Hunt", PlotThreadLength.SHORT, generator);
        for (int number = 1; number <= 10; number++) {
            assertFalse(thread.isComplete());
            assertEquals(number, thread.revealNextStep().number());
        }
        assertTrue(thread.isComplete());
        assertNull(thread.revealNextStep());
        assertEquals(10, thread.getRevealed().size());
    }

    @Test
    void losingProgressHidesAndRerollsTheLatestStep() {
        PlotThread thread = PlotThread.create("Hunt", PlotThreadLength.SHORT, generator);
        assertFalse(thread.canLoseProgress());
        assertEquals(0, thread.loseProgress(generator));

        thread.revealNextStep();
        thread.revealNextStep();
        PlotThreadStep before = thread.getSteps().get(1);

        // Swap in pools with different meanings so the re-roll is visible.
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        for (OracleTable table : OracleTable.values()) {
            WeightedIntMap<String> pool = new WeightedIntMap<>();
            pool.add(1, "rerolled");
            pools.put(table, pool);
        }
        RandomOracleGenerator rerollGenerator = RandomOracleGenerator.createForTesting(pools);

        assertEquals(2, thread.loseProgress(rerollGenerator));
        assertEquals(1, thread.getRevealedSteps());
        PlotThreadStep after = thread.getSteps().get(1);
        assertEquals(2, after.number());
        assertTrue(after.concepts().stream().allMatch(concept -> "rerolled".equals(concept.meaning())));
        assertFalse(before.equals(after));
        assertEquals(2, thread.revealNextStep().number());
    }

    @Test
    void concludedThreadsCannotLoseProgress() {
        PlotThread thread = PlotThread.create("Hunt", PlotThreadLength.SHORT, generator);
        while (!thread.isComplete()) {
            thread.revealNextStep();
        }
        assertFalse(thread.canLoseProgress());
        assertEquals(0, thread.loseProgress(generator));
        assertEquals(10, thread.getRevealedSteps());
    }

    @Test
    void randomEventsOnlyTouchEligibleThreads() {
        Roleplay roleplay = new Roleplay();
        assertNull(roleplay.progressRandomThread());
        assertNull(roleplay.loseRandomThreadProgress(generator));

        PlotThread concluded = PlotThread.create("Done", PlotThreadLength.SHORT, generator);
        while (!concluded.isComplete()) {
            concluded.revealNextStep();
        }
        PlotThread fresh = PlotThread.create("Fresh", PlotThreadLength.SHORT, generator);
        roleplay.getPlotThreads().add(concluded);
        roleplay.getPlotThreads().add(fresh);

        // Only the fresh thread is unresolved, and nothing has progress that can be lost yet.
        assertNull(roleplay.loseRandomThreadProgress(generator));
        Roleplay.PlotThreadChange progress = roleplay.progressRandomThread();
        assertEquals(fresh, progress.thread());
        assertEquals(1, progress.stepNumber());
        assertEquals(1, fresh.getRevealedSteps());

        Roleplay.PlotThreadChange loss = roleplay.loseRandomThreadProgress(generator);
        assertEquals(fresh, loss.thread());
        assertEquals(1, loss.stepNumber());
        assertNull(loss.revealed());
        assertEquals(0, fresh.getRevealedSteps());
        assertEquals(10, concluded.getRevealedSteps());
    }

    @Test
    void threadsSurviveSaveAndLoad() throws Exception {
        Roleplay original = new Roleplay();
        PlotThread thread = PlotThread.create("The <Long> Hunt & more", PlotThreadLength.MEDIUM, generator);
        thread.revealNextStep();
        thread.revealNextStep();
        original.getPlotThreads().add(thread);
        original.getPlotThreads().add(PlotThread.create("Second", PlotThreadLength.SHORT, generator));

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            original.writeToXML(writer, 0);
        }
        Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                          .parse(new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        Roleplay loaded = Roleplay.generateInstanceFromXML(node);

        assertEquals(2, loaded.getPlotThreads().size());
        PlotThread loadedThread = loaded.getPlotThreads().get(0);
        assertEquals("The <Long> Hunt & more", loadedThread.getName());
        assertEquals(PlotThreadLength.MEDIUM, loadedThread.getLength());
        assertEquals(2, loadedThread.getRevealedSteps());
        assertEquals(thread.getSteps(), loadedThread.getSteps());
        assertEquals("Second", loaded.getPlotThreads().get(1).getName());
    }
}
