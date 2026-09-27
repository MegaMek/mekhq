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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Holds the campaign's solo-roleplay state: the current chaos factor used by the {@link FateChart}, the cast of
 * {@link OracleCharacter}s the Oracle can pick from when a random event involves an NPC, and the campaign's
 * {@link PlotThread}s, and the journal: the player's own notes plus an automatic log of Oracle results.
 */
public class Roleplay {
    private static final MMLogger LOGGER = MMLogger.create(Roleplay.class);

    public static final int DEFAULT_CHAOS_FACTOR = 5;

    private int chaosFactor = DEFAULT_CHAOS_FACTOR;
    private final List<OracleCharacter> characters = new ArrayList<>();
    private final List<PlotThread> plotThreads = new ArrayList<>();
    private final List<JournalEntry> journal = new ArrayList<>();
    private final List<JournalEntry> oracleLog = new ArrayList<>();

    /**
     * @return the current chaos factor
     */
    public int getChaosFactor() {
        return chaosFactor;
    }

    /**
     * Sets the chaos factor, clamped to the range the {@link FateChart} accepts.
     *
     * @param chaosFactor the new chaos factor
     */
    public void setChaosFactor(final int chaosFactor) {
        this.chaosFactor = FateChart.clampChaosFactor(chaosFactor);
    }

    /**
     * Raises the chaos factor by one, up to {@link FateChart#MAXIMUM_CHAOS_FACTOR}.
     */
    public void increaseChaosFactor() {
        setChaosFactor(chaosFactor + 1);
    }

    /**
     * Lowers the chaos factor by one, down to {@link FateChart#MINIMUM_CHAOS_FACTOR}.
     */
    public void decreaseChaosFactor() {
        setChaosFactor(chaosFactor - 1);
    }

    /**
     * @return every Oracle character in cast order, including removed (inactive) ones
     */
    public List<OracleCharacter> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    /**
     * @return the characters currently in the cast, in cast order; the Oracle picks from these
     */
    public List<OracleCharacter> getActiveCharacters() {
        return characters.stream().filter(OracleCharacter::isActive).toList();
    }

    /**
     * @param id a character id
     *
     * @return the character with that id, active or not, or {@code null} if there is none
     */
    public @Nullable OracleCharacter getCharacter(final @Nullable UUID id) {
        return characters.stream().filter(character -> character.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * @param id a character id
     *
     * @return the character's name, or {@code null} if there is no such character
     */
    public @Nullable String getCharacterName(final @Nullable UUID id) {
        final OracleCharacter character = getCharacter(id);
        return (character == null) ? null : character.getName();
    }

    /**
     * Adds a typed-in character to the end of the cast. A blank name, or the name of someone already in the cast, is
     * ignored; the name of a removed character restores that character instead.
     *
     * @param name the character's name
     *
     * @return the added or restored character, or {@code null} if nothing changed
     */
    public @Nullable OracleCharacter addCharacter(final @Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        final String trimmed = name.trim();
        final OracleCharacter existing = findByName(trimmed);
        if (existing != null) {
            if (existing.isActive()) {
                return null;
            }
            restoreCharacter(existing);
            return existing;
        }

        final OracleCharacter character = new OracleCharacter(trimmed, null);
        characters.add(character);
        return character;
    }

    /**
     * Adds someone from the campaign's personnel to the end of the cast, linked to them. If they are already linked,
     * their character is restored (if removed) and renamed to match.
     *
     * @param personId the person's id
     * @param name     the person's current name
     *
     * @return the linked character
     */
    public OracleCharacter addLinkedCharacter(final UUID personId, final String name) {
        for (OracleCharacter character : characters) {
            if (personId.equals(character.getPersonId())) {
                character.setName(name);
                if (!character.isActive()) {
                    restoreCharacter(character);
                }
                return character;
            }
        }

        final OracleCharacter character = new OracleCharacter(name, personId);
        characters.add(character);
        return character;
    }

    private @Nullable OracleCharacter findByName(final String name) {
        return characters.stream().filter(character -> character.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Renames a character. Journal tags follow automatically, since they refer to the character by id.
     *
     * @param character the character to rename
     * @param newName   the new name
     *
     * @return {@code true} if renamed; {@code false} if the name is blank or another cast member already has it
     */
    public boolean renameCharacter(final OracleCharacter character, final @Nullable String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }

        final String trimmed = newName.trim();
        final OracleCharacter other = findByName(trimmed);
        if (other != null && other != character && other.isActive()) {
            return false;
        }
        character.setName(trimmed);
        return true;
    }

    /**
     * Removes a character from the cast. Their journal history is kept and they can be restored.
     *
     * @param character the character to remove
     */
    public void removeCharacter(final OracleCharacter character) {
        character.setActive(false);
    }

    /**
     * Returns a removed character to the end of the cast.
     *
     * @param character the character to restore
     */
    public void restoreCharacter(final OracleCharacter character) {
        characters.remove(character);
        character.setActive(true);
        final int firstInactive = characters.indexOf(characters.stream().filter(c -> !c.isActive()).findFirst()
                                                           .orElse(null));
        characters.add(firstInactive < 0 ? characters.size() : firstInactive, character);
    }

    /**
     * Moves an active character to a new place in the cast.
     *
     * @param character   the character to move
     * @param activeIndex its new position among the active characters
     */
    public void moveCharacter(final OracleCharacter character, final int activeIndex) {
        final List<OracleCharacter> active = new ArrayList<>(getActiveCharacters());
        if (!active.remove(character)) {
            return;
        }
        active.add(Math.clamp(activeIndex, 0, active.size()), character);
        final List<OracleCharacter> inactive = characters.stream().filter(c -> !c.isActive()).toList();
        characters.clear();
        characters.addAll(active);
        characters.addAll(inactive);
    }

    /**
     * Updates linked characters' names to match their person's current name.
     *
     * @param personNames looks up a person's current name by id; returns {@code null} if the person is gone
     */
    public void refreshLinkedNames(final Function<UUID, String> personNames) {
        for (OracleCharacter character : characters) {
            if (character.isLinked()) {
                final String name = personNames.apply(character.getPersonId());
                if (name != null && !name.isBlank()) {
                    character.setName(name);
                }
            }
        }
    }

    /**
     * Picks a random character from the cast. Every active character has the same chance.
     *
     * @return a random active character, or {@code null} if the cast is empty
     */
    public @Nullable OracleCharacter pickRandomCharacter() {
        return pickRandom(getActiveCharacters());
    }

    /**
     * @return the live, ordered list of plot threads; changes to it are saved with the campaign
     */
    public List<PlotThread> getPlotThreads() {
        return plotThreads;
    }

    /**
     * @return the live list of the player's journal notes, oldest first; changes to it are saved with the campaign
     */
    public List<JournalEntry> getJournal() {
        return journal;
    }

    /**
     * @return the live list of automatic Oracle log records, oldest first; changes to it are saved with the campaign
     */
    public List<JournalEntry> getOracleLog() {
        return oracleLog;
    }

    /**
     * Records an Oracle result in the Oracle log, then removes the oldest records until at most
     * {@code maximumEntries} remain.
     *
     * @param date           the in-game date of the result
     * @param type           what kind of result this is
     * @param text           a plain-text description of the result
     * @param maximumEntries the most records to keep; values below 1 are treated as 1
     *
     * @return the new record, so the caller can tag it with the threads and characters involved
     */
    public JournalEntry logOracle(final LocalDate date, final JournalEntryType type, final String text,
          final int maximumEntries) {
        final JournalEntry entry = new JournalEntry(date, type, text);
        oracleLog.add(entry);
        trimOracleLog(maximumEntries);
        return entry;
    }

    private void trimOracleLog(final int maximumEntries) {
        final int excess = oracleLog.size() - Math.max(1, maximumEntries);
        if (excess > 0) {
            oracleLog.subList(0, excess).clear();
        }
    }

    /**
     * Builds the combined timeline: the player's notes and the Oracle log, merged in date order. On the same date,
     * Oracle records come before notes, since a note usually reflects on what the Oracle said.
     *
     * @param filter which entries to include
     *
     * @return the matching entries, oldest first
     */
    public List<JournalEntry> getTimeline(final JournalFilter filter) {
        final List<JournalEntry> timeline = new ArrayList<>();
        oracleLog.stream().filter(filter::matches).forEach(timeline::add);
        journal.stream().filter(filter::matches).forEach(timeline::add);
        // A stable sort keeps each list's own order within a date.
        timeline.sort(Comparator.comparing(JournalEntry::getDate));
        return timeline;
    }

    /**
     * @param id a plot thread id
     *
     * @return the thread with that id, or {@code null} if there is none (for example, it was deleted)
     */
    public @Nullable PlotThread getPlotThread(final @Nullable UUID id) {
        return plotThreads.stream().filter(thread -> thread.getId().equals(id)).findFirst().orElse(null);
    }

    private List<JournalEntry> allEntries() {
        final List<JournalEntry> entries = new ArrayList<>(journal);
        entries.addAll(oracleLog);
        return entries;
    }

    /**
     * @param id a plot thread id
     *
     * @return the thread's name, or {@code null} if the thread no longer exists
     */
    public @Nullable String getPlotThreadName(final @Nullable UUID id) {
        final PlotThread thread = getPlotThread(id);
        return (thread == null) ? null : Objects.requireNonNull(thread.getName());
    }

    /**
     * Reveals the next step of a random unconcluded plot thread.
     *
     * @param date the in-game date of the reveal
     *
     * @return the change made, or {@code null} if every thread is concluded (or there are none)
     */
    public @Nullable PlotThreadChange progressRandomThread(final @Nullable LocalDate date) {
        final PlotThread thread = pickRandom(plotThreads.stream().filter(candidate -> !candidate.isComplete())
                                                   .toList());
        if (thread == null) {
            return null;
        }
        final PlotThreadStep step = thread.revealNextStep(date);
        return new PlotThreadChange(thread, step.number(), step);
    }

    /**
     * Loses the latest revealed step of a random unconcluded plot thread that has progress to lose. The step is
     * hidden again and re-rolled.
     *
     * @param generator the generator supplying the oracle tables for the re-roll
     *
     * @return the change made, or {@code null} if no thread has progress that can be lost
     */
    public @Nullable PlotThreadChange loseRandomThreadProgress(final RandomOracleGenerator generator) {
        final PlotThread thread = pickRandom(plotThreads.stream().filter(PlotThread::canLoseProgress).toList());
        if (thread == null) {
            return null;
        }
        return new PlotThreadChange(thread, thread.loseProgress(generator), null);
    }

    private static <T> @Nullable T pickRandom(final List<T> candidates) {
        return candidates.isEmpty() ? null : candidates.get(randomInt(candidates.size()));
    }

    /**
     * A change a random event made to a plot thread.
     *
     * @param thread     the thread changed
     * @param stepNumber the step revealed or lost
     * @param revealed   the step revealed, or {@code null} if progress was lost
     */
    public record PlotThreadChange(PlotThread thread, int stepNumber, @Nullable PlotThreadStep revealed) {}

    /**
     * Writes this object to the campaign save as a {@code <roleplay>} element.
     *
     * @param writer the writer to output to
     * @param indent the current indentation level
     */
    public void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "roleplay");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "chaosFactor", chaosFactor);
        if (!characters.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "characters");
            for (OracleCharacter character : characters) {
                character.writeToXML(writer, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "characters");
        }
        if (!plotThreads.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "plotThreads");
            for (PlotThread thread : plotThreads) {
                thread.writeToXML(writer, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "plotThreads");
        }
        JournalEntry.writeListToXML(writer, indent, "journal", journal);
        JournalEntry.writeListToXML(writer, indent, "oracleLog", oracleLog);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "roleplay");
    }

    /**
     * Creates an instance from a {@code <roleplay>} element. Unknown or malformed children are logged and skipped.
     *
     * @param node the {@code <roleplay>} node
     *
     * @return the loaded instance
     */
    public static Roleplay generateInstanceFromXML(final Node node) {
        final Roleplay roleplay = new Roleplay();
        final NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                if (child.getNodeName().equalsIgnoreCase("chaosFactor")) {
                    roleplay.setChaosFactor(Integer.parseInt(child.getTextContent().trim()));
                } else if (child.getNodeName().equalsIgnoreCase("characters")) {
                    final NodeList characterNodes = child.getChildNodes();
                    for (int j = 0; j < characterNodes.getLength(); j++) {
                        final Node characterNode = characterNodes.item(j);
                        if (characterNode.getNodeName().equalsIgnoreCase("character")) {
                            final OracleCharacter character = OracleCharacter.parse(characterNode);
                            if (!character.getName().isBlank()) {
                                roleplay.characters.add(character);
                            }
                        }
                    }
                } else if (child.getNodeName().equalsIgnoreCase("journal")) {
                    roleplay.journal.addAll(JournalEntry.parseList(child, JournalEntryType.NOTE));
                } else if (child.getNodeName().equalsIgnoreCase("oracleLog")) {
                    roleplay.oracleLog.addAll(JournalEntry.parseList(child, JournalEntryType.FATE_CHART));
                } else if (child.getNodeName().equalsIgnoreCase("plotThreads")) {
                    final NodeList threadNodes = child.getChildNodes();
                    for (int j = 0; j < threadNodes.getLength(); j++) {
                        final Node threadNode = threadNodes.item(j);
                        if (threadNode.getNodeName().equalsIgnoreCase("plotThread")) {
                            final PlotThread thread = PlotThread.generateInstanceFromXML(threadNode);
                            if (thread != null) {
                                roleplay.plotThreads.add(thread);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse roleplay element {}", child.getNodeName(), e);
            }
        }

        roleplay.resolveLegacyCharacterTags();
        return roleplay;
    }

    /**
     * Journal entries saved before characters had ids tag characters by name. Point those tags at the character with
     * that name, adding a removed character for any name no longer in the cast so the tag is kept.
     */
    private void resolveLegacyCharacterTags() {
        for (JournalEntry entry : allEntries()) {
            for (String name : entry.legacyCharacterNames) {
                OracleCharacter character = findByName(name);
                if (character == null) {
                    character = new OracleCharacter(name, null);
                    character.setActive(false);
                    characters.add(character);
                }
                entry.getCharacters().add(character.getId());
            }
            entry.legacyCharacterNames.clear();
        }
    }
}
