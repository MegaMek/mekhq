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

import static megamek.common.compute.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.roleplay.Concepts.Concept;
import mekhq.campaign.roleplay.Roleplay.PlotThreadChange;

/**
 * The solo-roleplay actions a player takes: asking the Fate Chart, rolling Concepts, and creating and revealing plot
 * threads. Each action applies its effects to the campaign's {@link Roleplay} and records itself in the Oracle log,
 * tagged with the threads and characters involved, so the screens only have to show the results.
 */
public class OracleActions {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final Roleplay roleplay;
    private final Supplier<LocalDate> today;
    private final IntSupplier maximumLogEntries;
    private final Supplier<RandomOracleGenerator> generator;

    /**
     * @param roleplay          the roleplay state to act on
     * @param today             supplies the current in-game date
     * @param maximumLogEntries supplies the Oracle log's size limit
     * @param generator         supplies the oracle tables
     */
    public OracleActions(final Roleplay roleplay, final Supplier<LocalDate> today, final IntSupplier maximumLogEntries,
          final Supplier<RandomOracleGenerator> generator) {
        this.roleplay = roleplay;
        this.today = today;
        this.maximumLogEntries = maximumLogEntries;
        this.generator = generator;
    }

    /**
     * @param campaign the campaign
     *
     * @return actions on the campaign's roleplay state, dated by the campaign and limited by its options
     */
    public static OracleActions forCampaign(final Campaign campaign) {
        return new OracleActions(campaign.getRoleplay(), campaign::getLocalDate,
              () -> campaign.getCampaignOptions().get(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES),
              RandomOracleGenerator::getInstance);
    }

    public Roleplay getRoleplay() {
        return roleplay;
    }

    // region Fate Chart

    /**
     * The result of asking the Fate Chart.
     *
     * @param answer    the question, odds, chaos, roll and answer
     * @param fate      the full Fate Chart result, including any random event
     * @param character the character a random event picked, or {@code null}
     * @param thread    the thread a random event moved, or {@code null}
     * @param log       the Oracle log record written for this question
     */
    public record AskOutcome(OracleAnswer answer, FateChartResult fate, @Nullable OracleCharacter character,
          @Nullable PlotThreadChange thread, JournalEntry log) {
        /**
         * @return {@code true} if a random event needed a character but the cast was empty
         */
        public boolean isMissingCharacter() {
            return fate.hasRandomEvent() && fate.randomEventFocus().involvesNPC() && character == null;
        }

        /**
         * @return {@code true} if a random event needed a thread but none could be moved
         */
        public boolean isMissingThread() {
            if (!fate.hasRandomEvent()) {
                return false;
            }
            RandomEventFocus focus = fate.randomEventFocus();
            return (focus == RandomEventFocus.MOVE_TOWARD_A_THREAD || focus == RandomEventFocus.MOVE_AWAY_FROM_A_THREAD)
                         && thread == null;
        }
    }

    /**
     * Asks the Fate Chart a question, rolling the dice.
     *
     * @param question the question, or an empty string
     * @param odds     the odds chosen
     *
     * @return the outcome
     */
    public AskOutcome ask(final String question, final FateChartOdds odds) {
        final int roll = randomInt(100) + 1;
        return ask(question, odds, roll, randomInt(100) + 1);
    }

    /**
     * Asks the Fate Chart a question with given rolls.
     *
     * @param question        the question, or an empty string
     * @param odds            the odds chosen
     * @param roll            the d100 roll
     * @param randomEventRoll the follow-up d100 roll, used only if {@code roll} triggers a random event
     *
     * @return the outcome
     */
    public AskOutcome ask(final String question, final FateChartOdds odds, final int roll, final int randomEventRoll) {
        final int chaos = roleplay.getChaosFactor();
        final FateChartResult fate = FateChart.consult(odds, chaos, roll, randomEventRoll);
        final String trimmedQuestion = (question == null) ? "" : question.strip();
        final OracleAnswer answer = new OracleAnswer(trimmedQuestion, odds, chaos, roll, fate.answer());

        OracleCharacter character = null;
        PlotThreadChange threadChange = null;
        final List<String> lines = new ArrayList<>();
        if (!trimmedQuestion.isEmpty()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.fateChart.question", trimmedQuestion));
        }
        lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.fateChart", odds.getLabel(), chaos, roll,
              fate.answer().getLabel()));

        if (fate.hasRandomEvent()) {
            final RandomEventFocus focus = fate.randomEventFocus();
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.randomEvent", focus.getLabel(),
                  fate.randomEventRoll()));
            switch (focus) {
                case NPC_ACTION, NPC_NEGATIVE, NPC_POSITIVE -> {
                    character = roleplay.pickRandomCharacter();
                    lines.add(character == null ? getTextAt(RESOURCE_BUNDLE, "OracleLog.noCharacter")
                                    : getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.character", character.getName()));
                }
                case MOVE_TOWARD_A_THREAD -> {
                    threadChange = roleplay.progressRandomThread(today.get());
                    lines.add(describeProgress(threadChange));
                }
                case MOVE_AWAY_FROM_A_THREAD -> {
                    threadChange = roleplay.loseRandomThreadProgress(generator.get());
                    lines.add(threadChange == null ? getTextAt(RESOURCE_BUNDLE, "OracleLog.noThreadToLose")
                                    : getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.threadLost",
                                          threadChange.thread().getName(), threadChange.stepNumber()));
                }
                default -> { }
            }
        }

        final JournalEntry log = log(fate.hasRandomEvent() ? JournalEntryType.RANDOM_EVENT
                                           : JournalEntryType.FATE_CHART, String.join("\n", lines));
        log.setAnswer(answer);
        log.tagCharacter(character);
        if (threadChange != null) {
            log.tagThread(threadChange.thread());
        }
        return new AskOutcome(answer, fate, character, threadChange, log);
    }

    private String describeProgress(final @Nullable PlotThreadChange change) {
        if (change == null) {
            return getTextAt(RESOURCE_BUNDLE, "OracleLog.noThreadToProgress");
        }
        final PlotThreadStep step = change.revealed();
        final String key = step.conclusion() ? "OracleLog.threadConclusion"
                                 : step.majorRevelation() ? "OracleLog.threadMajorRevelation"
                                         : "OracleLog.threadProgress";
        return getFormattedTextAt(RESOURCE_BUNDLE, key, change.thread().getName(), change.stepNumber()) + '\n'
                     + describeConcepts(step.concepts());
    }

    // endregion Fate Chart

    // region Concepts and threads

    /**
     * Rolls on up to three oracle tables and logs the result.
     *
     * @param tables the tables to roll on
     *
     * @return the concepts rolled; empty if no table was given
     */
    public List<Concept> rollConcepts(final List<OracleTable> tables) {
        final List<Concept> concepts = Concepts.roll(tables, generator.get());
        if (!concepts.isEmpty()) {
            log(JournalEntryType.CONCEPTS,
                  getTextAt(RESOURCE_BUNDLE, "OracleLog.concepts") + '\n' + describeConcepts(concepts));
        }
        return concepts;
    }

    /**
     * Creates a plot thread, secretly rolling its steps, and logs it.
     *
     * @param name   the thread's name
     * @param length the length of its track
     *
     * @return the new thread
     */
    public PlotThread createThread(final String name, final PlotThreadLength length) {
        final PlotThread thread = PlotThread.create(name.strip(), length, generator.get());
        roleplay.getPlotThreads().add(thread);
        log(JournalEntryType.THREAD, getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.threadCreated", thread.getName(),
              length.getLabel())).tagThread(thread);
        return thread;
    }

    /**
     * Reveals a thread's next step and logs it.
     *
     * @param thread the thread
     *
     * @return the revealed step, or {@code null} if the thread was already concluded
     */
    public @Nullable PlotThreadStep revealNextStep(final PlotThread thread) {
        final PlotThreadStep step = thread.revealNextStep(today.get());
        if (step != null) {
            final String key = step.conclusion() ? "OracleLog.threadRevealedConclusion"
                                     : step.majorRevelation() ? "OracleLog.threadRevealedMajorRevelation"
                                             : "OracleLog.threadRevealed";
            log(JournalEntryType.THREAD, getFormattedTextAt(RESOURCE_BUNDLE, key, thread.getName(), step.number())
                                               + '\n' + describeConcepts(step.concepts())).tagThread(thread);
        }
        return step;
    }

    /**
     * Deletes a thread and logs it. Journal entries tagged with it keep the tag.
     *
     * @param thread the thread
     */
    public void deleteThread(final PlotThread thread) {
        if (roleplay.getPlotThreads().remove(thread)) {
            log(JournalEntryType.THREAD, getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.threadDeleted",
                  thread.getName())).tagThread(thread);
        }
    }

    // endregion Concepts and threads

    /**
     * @param concepts concepts to describe
     *
     * @return one plain-text line per concept, as the Oracle log shows them
     */
    public static String describeConcepts(final List<Concept> concepts) {
        final List<String> lines = new ArrayList<>();
        for (Concept concept : concepts) {
            final String meaning = (concept.meaning() == null) ? getTextAt(RESOURCE_BUNDLE, "OracleLog.noConcept")
                                         : concept.meaning();
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.concept", concept.table().getLabel(), meaning));
        }
        return String.join("\n", lines);
    }

    private JournalEntry log(final JournalEntryType type, final String text) {
        return roleplay.logOracle(today.get(), type, text, maximumLogEntries.getAsInt());
    }
}
