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

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.roleplay.Concepts.Concept;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A plot thread with a progress track of 10, 15 or 20 steps. Every step's concept is rolled secretly when the thread
 * is created, and the player reveals the steps one at a time as the plot progresses.
 *
 * <p>Every fifth step before the last is a flashpoint, which is a Major Revelation. A normal step's concept is a
 * random theme plus three different random adventure tables; a Major Revelation's is a random theme plus the
 * revelation, secret and twist tables. The final step is the thread's conclusion, rolled on a random theme plus the
 * stakes, cost and aftermath tables.</p>
 */
public class PlotThread {
    private static final MMLogger LOGGER = MMLogger.create(PlotThread.class);

    /** Every this many steps is a flashpoint. */
    public static final int FLASHPOINT_INTERVAL = 5;

    /** Written in place of a reveal date that is not known, so dates stay in step order. */
    private static final String UNKNOWN_DATE = "unknown";

    /** How many adventure tables a normal step rolls on. */
    public static final int ADVENTURE_ROLLS = 3;

    /** The adventure tables a Major Revelation rolls on. */
    static final List<OracleTable> MAJOR_REVELATION_TABLES = List.of(OracleTable.ADVENTURE_REVELATION,
          OracleTable.ADVENTURE_SECRET, OracleTable.ADVENTURE_TWIST);

    /** The adventure tables the conclusion rolls on: what is at stake, what it costs, and what follows. */
    static final List<OracleTable> CONCLUSION_TABLES = List.of(OracleTable.ADVENTURE_STAKES,
          OracleTable.ADVENTURE_COST, OracleTable.ADVENTURE_AFTERMATH);

    private UUID id = UUID.randomUUID();
    private String name;
    private PlotThreadLength length;
    private final List<PlotThreadStep> steps = new ArrayList<>();
    private int revealedSteps = 0;
    /** The in-game date each revealed step was revealed, in step order; {@code null} where it is not known. */
    private final List<LocalDate> revealDates = new ArrayList<>();

    private PlotThread(final String name, final PlotThreadLength length) {
        this.name = name;
        this.length = length;
    }

    /**
     * Creates a new thread, secretly rolling every step's concept.
     *
     * @param name      the thread's name
     * @param length    the length of the progress track
     * @param generator the generator supplying the oracle tables
     *
     * @return the new thread, with no steps revealed
     */
    public static PlotThread create(final String name, final PlotThreadLength length,
          final RandomOracleGenerator generator) {
        final PlotThread thread = new PlotThread(name, length);
        for (int number = 1; number <= length.getSteps(); number++) {
            thread.steps.add(rollStep(number, length, generator));
        }
        return thread;
    }

    /**
     * Rolls a fresh concept for one step of the track.
     *
     * @param number    the step's number, starting at 1
     * @param length    the thread's length
     * @param generator the generator supplying the oracle tables
     *
     * @return the rolled step
     */
    static PlotThreadStep rollStep(final int number, final PlotThreadLength length,
          final RandomOracleGenerator generator) {
        final boolean majorRevelation = isFlashpoint(number, length);
        final boolean conclusion = number == length.getSteps();
        final List<OracleTable> tables = new ArrayList<>();
        tables.add(randomTable(getThemeTables()));
        if (conclusion) {
            tables.addAll(CONCLUSION_TABLES);
        } else if (majorRevelation) {
            tables.addAll(MAJOR_REVELATION_TABLES);
        } else {
            tables.addAll(randomAdventureTables());
        }

        final List<Concept> concepts = new ArrayList<>();
        for (OracleTable table : tables) {
            concepts.add(new Concept(table, generator.generate(table)));
        }
        return new PlotThreadStep(number, majorRevelation, conclusion, concepts);
    }

    /**
     * @param number the step's number, starting at 1
     * @param length the thread's length
     *
     * @return {@code true} if the step is a flashpoint: every {@value #FLASHPOINT_INTERVAL}th step except the last
     */
    public static boolean isFlashpoint(final int number, final PlotThreadLength length) {
        return number % FLASHPOINT_INTERVAL == 0 && number < length.getSteps();
    }

    static List<OracleTable> getThemeTables() {
        return Arrays.stream(OracleTable.values()).filter(table -> table.name().startsWith("THEMES_")).toList();
    }

    static List<OracleTable> getAdventureTables() {
        return Arrays.stream(OracleTable.values()).filter(table -> table.name().startsWith("ADVENTURE_")).toList();
    }

    private static OracleTable randomTable(final List<OracleTable> tables) {
        return tables.get(randomInt(tables.size()));
    }

    private static List<OracleTable> randomAdventureTables() {
        final List<OracleTable> tables = new ArrayList<>(getAdventureTables());
        Collections.shuffle(tables);
        return tables.subList(0, ADVENTURE_ROLLS);
    }

    /**
     * @return the thread's permanent id, used to tag journal entries so the tags survive a rename
     */
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public PlotThreadLength getLength() {
        return length;
    }

    /**
     * @return every step on the track, including those not yet revealed
     */
    public List<PlotThreadStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * @return how many steps have been revealed
     */
    public int getRevealedSteps() {
        return revealedSteps;
    }

    /**
     * @return the steps revealed so far, in order
     */
    public List<PlotThreadStep> getRevealed() {
        return getSteps().subList(0, revealedSteps);
    }

    /**
     * @return {@code true} if every step, including the conclusion, has been revealed
     */
    public boolean isComplete() {
        return revealedSteps >= steps.size();
    }

    /**
     * Reveals the next step on the track.
     *
     * @param date the in-game date of the reveal, or {@code null} if it is not known
     *
     * @return the newly revealed step, or {@code null} if the thread is already complete
     */
    public @Nullable PlotThreadStep revealNextStep(final @Nullable LocalDate date) {
        if (isComplete()) {
            return null;
        }
        revealDates.add(date);
        return steps.get(revealedSteps++);
    }

    /**
     * @param stepNumber a step's number, starting at 1
     *
     * @return the in-game date the step was revealed, or {@code null} if it is unrevealed or the date is not known
     */
    public @Nullable LocalDate getRevealDate(final int stepNumber) {
        return (stepNumber >= 1 && stepNumber <= revealDates.size()) ? revealDates.get(stepNumber - 1) : null;
    }

    /**
     * @return the next step to be revealed, or {@code null} if the thread is complete
     */
    public @Nullable PlotThreadStep getNextStep() {
        return isComplete() ? null : steps.get(revealedSteps);
    }

    /**
     * @return {@code true} if at least one step is revealed and the thread is not yet concluded, so it has progress
     *       that can be lost
     */
    public boolean canLoseProgress() {
        return revealedSteps > 0 && !isComplete();
    }

    /**
     * Loses the most recently revealed step: it is hidden again and its concept is rolled afresh, so the player gets
     * a new surprise when they next progress the thread.
     *
     * @param generator the generator supplying the oracle tables
     *
     * @return the number of the step that was lost, or {@code 0} if the thread had no progress that could be lost
     */
    public int loseProgress(final RandomOracleGenerator generator) {
        if (!canLoseProgress()) {
            return 0;
        }

        final int index = --revealedSteps;
        revealDates.remove(index);
        steps.set(index, rollStep(index + 1, length, generator));
        return index + 1;
    }

    /**
     * Re-rolls the concepts of the most recently revealed step, keeping it revealed with its reveal date. The step
     * keeps its kind, so a Major Revelation stays a Major Revelation and the conclusion stays the conclusion.
     *
     * @param generator the generator supplying the oracle tables
     *
     * @return the re-rolled step, or {@code null} if no step has been revealed
     */
    public @Nullable PlotThreadStep rerollLatestStep(final RandomOracleGenerator generator) {
        if (revealedSteps == 0) {
            return null;
        }
        final int index = revealedSteps - 1;
        final PlotThreadStep step = rollStep(index + 1, length, generator);
        steps.set(index, step);
        return step;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Writes this thread as a {@code <plotThread>} element.
     *
     * @param writer the writer to output to
     * @param indent the current indentation level
     */
    public void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "plotThread");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "length", length.name());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "revealedSteps", revealedSteps);
        for (LocalDate date : revealDates) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "revealedOn", date == null ? UNKNOWN_DATE : date.toString());
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "steps");
        for (PlotThreadStep step : steps) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "step");
            for (Concept concept : step.concepts()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "concept");
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "table", concept.table().name());
                if (concept.meaning() != null) {
                    MHQXMLUtility.writeSimpleXMLTag(writer, indent, "meaning", concept.meaning());
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "concept");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "step");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "steps");
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "plotThread");
    }

    /**
     * Loads a thread from a {@code <plotThread>} element.
     *
     * @param node the {@code <plotThread>} node
     *
     * @return the thread, or {@code null} if it could not be loaded
     */
    public static @Nullable PlotThread generateInstanceFromXML(final Node node) {
        try {
            UUID id = null;
            String name = "";
            PlotThreadLength length = PlotThreadLength.SHORT;
            int revealedSteps = 0;
            Node stepsNode = null;
            final List<LocalDate> revealDates = new ArrayList<>();

            final NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                switch (child.getNodeName()) {
                    case "id" -> id = UUID.fromString(child.getTextContent().trim());
                    case "name" -> name = child.getTextContent();
                    case "length" -> length = PlotThreadLength.valueOf(child.getTextContent().trim());
                    case "revealedSteps" -> revealedSteps = Integer.parseInt(child.getTextContent().trim());
                    case "steps" -> stepsNode = child;
                    case "revealedOn" -> revealDates.add(parseRevealDate(child.getTextContent().trim()));
                    default -> { }
                }
            }

            final PlotThread thread = new PlotThread(name, length);
            if (id != null) {
                thread.id = id;
            }
            if (stepsNode != null) {
                final NodeList stepNodes = stepsNode.getChildNodes();
                for (int i = 0; i < stepNodes.getLength(); i++) {
                    final Node stepNode = stepNodes.item(i);
                    if (stepNode.getNodeName().equals("step")) {
                        final int number = thread.steps.size() + 1;
                        thread.steps.add(new PlotThreadStep(number, isFlashpoint(number, length),
                              number == length.getSteps(), parseConcepts(stepNode)));
                    }
                }
            }
            thread.revealedSteps = Math.clamp(revealedSteps, 0, thread.steps.size());
            // Saves from before reveal dates were kept have none; pad or trim so there is one per revealed step.
            for (int i = 0; i < thread.revealedSteps; i++) {
                thread.revealDates.add(i < revealDates.size() ? revealDates.get(i) : null);
            }
            return thread;
        } catch (Exception e) {
            LOGGER.error("Failed to load plot thread", e);
            return null;
        }
    }

    private static @Nullable LocalDate parseRevealDate(final String text) {
        if (UNKNOWN_DATE.equals(text)) {
            return null;
        }
        try {
            return MHQXMLUtility.parseDate(text);
        } catch (Exception e) {
            LOGGER.warn("Unreadable plot thread reveal date {}", text);
            return null;
        }
    }

    /** A table that no longer exists is skipped, rather than losing the whole thread. */
    private static @Nullable OracleTable parseTable(final String text) {
        try {
            return OracleTable.valueOf(text);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown oracle table {} in a plot thread", text);
            return null;
        }
    }

    private static List<Concept> parseConcepts(final Node stepNode) {
        final List<Concept> concepts = new ArrayList<>();
        final NodeList conceptNodes = stepNode.getChildNodes();
        for (int i = 0; i < conceptNodes.getLength(); i++) {
            final Node conceptNode = conceptNodes.item(i);
            if (!conceptNode.getNodeName().equals("concept")) {
                continue;
            }

            OracleTable table = null;
            String meaning = null;
            final NodeList fields = conceptNode.getChildNodes();
            for (int j = 0; j < fields.getLength(); j++) {
                final Node field = fields.item(j);
                if (field.getNodeName().equals("table")) {
                    table = parseTable(field.getTextContent().trim());
                } else if (field.getNodeName().equals("meaning")) {
                    meaning = field.getTextContent();
                }
            }
            if (table != null) {
                concepts.add(new Concept(table, meaning));
            }
        }
        return concepts;
    }
}
