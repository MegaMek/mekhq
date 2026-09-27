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
import java.util.function.IntUnaryOperator;
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
     * Re-rolls the prompts of a thread's most recently revealed step and logs the new ones.
     *
     * @param thread the thread
     *
     * @return the re-rolled step, or {@code null} if no step has been revealed
     */
    public @Nullable PlotThreadStep rerollLatestStep(final PlotThread thread) {
        final PlotThreadStep step = thread.rerollLatestStep(generator.get());
        if (step != null) {
            log(JournalEntryType.THREAD, getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.threadRerolled",
                  thread.getName(), step.number()) + '\n' + describeConcepts(step.concepts())).tagThread(thread);
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

    // region Checks

    /**
     * Logs a check made from the Checks page.
     *
     * @param check      the check's result
     * @param characters the cast members who took part, to tag the entry with
     * @param thread     the thread the check belongs to, or {@code null}
     *
     * @return the Oracle log record written
     */
    public JournalEntry logCheck(final CheckRecord check, final List<OracleCharacter> characters,
          final @Nullable PlotThread thread) {
        final JournalEntry log = log(JournalEntryType.CHECK, describeCheck(check));
        log.setCheck(check);
        for (OracleCharacter character : characters) {
            log.tagCharacter(character);
        }
        log.tagThread(thread);
        return log;
    }

    /**
     * @param check a check's result
     *
     * @return the check as plain text, one line per side after a summary line
     */
    public static String describeCheck(final CheckRecord check) {
        final List<String> lines = new ArrayList<>();
        if (!check.reason().isBlank()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.reason", check.reason()));
        }
        final List<CheckRecord.Side> sides = check.sides();
        if (check.opposed() && sides.size() == 2) {
            final CheckRecord.Side winner = sides.get(0).won() ? sides.get(0) : sides.get(1);
            final int difference = check.winningDifference();
            lines.add(difference == 0 ? getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.opposedTie",
                  winner.name()) : getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.opposed", winner.name(),
                  difference));
        } else if (sides.size() > 1) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.group", check.countWon(), sides.size()));
        }
        for (CheckRecord.Side side : sides) {
            final String key = (side.margin() >= 0) ? "OracleLog.check.passed" : "OracleLog.check.failed";
            String line = getFormattedTextAt(RESOURCE_BUNDLE, key, side.name(), side.action(), side.roll(),
                  side.target(), Math.abs(side.margin()));
            if (side.usedEdge()) {
                line += ' ' + getTextAt(RESOURCE_BUNDLE, "OracleLog.check.edge");
            }
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    // endregion Checks

    // region Dice and NPCs

    /**
     * Rolls a dice expression and logs it.
     *
     * @param expression the expression, such as "2d6+1"
     * @param die        rolls one die of the given number of sides
     *
     * @return the roll
     *
     * @throws IllegalArgumentException if the expression is not valid
     */
    public DiceExpression.Roll rollDice(final String expression, final IntUnaryOperator die) {
        final DiceExpression.Roll roll = DiceExpression.parse(expression).roll(die);
        log(JournalEntryType.DICE, getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.dice", roll.expression(),
              roll.total(), roll.describeWorking()));
        return roll;
    }

    /**
     * Rolls a dice expression with real dice and logs it.
     *
     * @param expression the expression, such as "2d6+1"
     *
     * @return the roll
     *
     * @throws IllegalArgumentException if the expression is not valid
     */
    public DiceExpression.Roll rollDice(final String expression) {
        return rollDice(expression, sides -> randomInt(sides) + 1);
    }

    /** The Characters tables a generated NPC's profile is drawn from, in the order they are shown. */
    public static final List<OracleTable> NPC_PROFILE_TABLES = List.of(OracleTable.CHARACTERS_APPEARANCE,
          OracleTable.CHARACTERS_PERSONALITY, OracleTable.CHARACTERS_DEMEANOR, OracleTable.CHARACTERS_MOTIVE);

    /**
     * Makes a new cast member with a profile rolled from the Characters tables, and logs it.
     *
     * @param name the new character's name
     *
     * @return the new character, or {@code null} if the name is blank or already in the cast
     */
    public @Nullable OracleCharacter generateNpc(final String name) {
        final OracleCharacter character = roleplay.addCharacter(name);
        if (character == null) {
            return null;
        }
        final List<String> lines = new ArrayList<>();
        for (OracleTable table : NPC_PROFILE_TABLES) {
            final String meaning = generator.get().generate(table);
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.concept", table.getTableLabel(),
                  (meaning == null) ? getTextAt(RESOURCE_BUNDLE, "OracleLog.noConcept") : meaning).strip());
        }
        final String description = String.join("\n", lines);
        character.setNotes(description);
        log(JournalEntryType.CONCEPTS, getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.npc", character.getName())
                                             + '\n' + description).tagCharacter(character);
        return character;
    }

    // endregion Dice and NPCs

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
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.concept", concept.table().getTableLabel(), meaning));
        }
        return String.join("\n", lines);
    }

    private JournalEntry log(final JournalEntryType type, final String text) {
        return roleplay.logOracle(today.get(), type, text, maximumLogEntries.getAsInt());
    }
}
