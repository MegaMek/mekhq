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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;

/**
 * Writes what happens in the campaign into the Oracle log, so the journal records the story MekHQ plays out as well as
 * the player's own: contracts, ultimatums, battles the cast fought in, and cast members joining, dying or leaving.
 * Everything else is left out, and travel has its own {@link TravelLog}. People events on the same day are
 * gathered into one entry, such as "Killed: Ana, Ben", so a hard battle does not bury the journal.
 *
 * <p>This class holds the rules; {@link CampaignChronicleListener} feeds it MekHQ's events.</p>
 */
public class CampaignChronicle {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /** The kinds of people event gathered into one entry per day. */
    public enum PersonChange {
        JOINED, BORN, KILLED, MISSING, CAPTURED, DEPARTED
    }

    /** The answers a Faction Standing ultimatum can be given. */
    public enum UltimatumChoice {
        SIDE, MERCENARY, PIRATE
    }

    private record Group(LocalDate date, JournalEntry entry, List<String> names) {}

    private final Roleplay roleplay;
    private final Supplier<LocalDate> today;
    private final IntSupplier maximumLogEntries;
    private final BooleanSupplier enabled;
    private final Map<PersonChange, Group> groups = new EnumMap<>(PersonChange.class);
    /**
     * What has been recorded today, so an event MekHQ announces twice is written once. This and the day's groups are
     * kept in memory only, so after a same-day save and reload the next change starts a fresh entry. That's harmless.
     */
    private final Set<String> recorded = new HashSet<>();
    private LocalDate recordedOn;

    /**
     * @param roleplay          the roleplay state to write to
     * @param today             supplies the current in-game date
     * @param maximumLogEntries supplies the Oracle log's size limit
     * @param enabled           supplies whether the campaign keeps a chronicle
     */
    public CampaignChronicle(final Roleplay roleplay, final Supplier<LocalDate> today,
          final IntSupplier maximumLogEntries, final BooleanSupplier enabled) {
        this.roleplay = roleplay;
        this.today = today;
        this.maximumLogEntries = maximumLogEntries;
        this.enabled = enabled;
    }

    /**
     * Records a campaign event once. The same key on the same day is ignored, as MekHQ can announce one change more
     * than once.
     *
     * @param key  identifies the event, such as "contract-new:" and the contract's id
     * @param text the entry's text
     *
     * @return the entry written, or {@code null} if the chronicle is off or the event was already recorded today
     */
    public @Nullable JournalEntry record(final String key, final String text) {
        if (!enabled.getAsBoolean() || isRepeat(key)) {
            return null;
        }
        return roleplay.logOracle(today.get(), JournalEntryType.CHRONICLE, text, maximumLogEntries.getAsInt());
    }

    private boolean isRepeat(final String key) {
        if (!today.get().equals(recordedOn)) {
            recorded.clear();
            recordedOn = today.get();
        }
        return !recorded.add(key);
    }

    /** @return the entry for a new contract */
    public @Nullable JournalEntry contractStarted(final UUID id, final String name, final String employer,
          final @Nullable String system) {
        String text = (system == null || system.isBlank())
                            ? getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.contractStarted", name, employer)
                            : getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.contractStartedAt", name, employer,
                                  system);
        return record("contract-new:" + id, text);
    }

    /** @return the entry for a finished contract */
    public @Nullable JournalEntry contractEnded(final UUID id, final String name, final String outcome) {
        return record("contract-end:" + id, getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.contractEnded", name,
              outcome));
    }

    /**
     * Records a resolved battle, if anyone in the current cast fought in it. The cast members who did are tagged.
     *
     * @param id             the scenario's id
     * @param name           the scenario's name
     * @param outcome        how it ended, such as "Victory"
     * @param participantIds the ids of the campaign's people who took part
     *
     * @return the entry written, or {@code null} if the chronicle is off, no cast member fought, or this was already
     *       recorded today
     */
    public @Nullable JournalEntry battleResolved(final int id, final String name, final String outcome,
          final Collection<UUID> participantIds) {
        List<OracleCharacter> castMembers = castMembersFor(participantIds);
        if (castMembers.isEmpty()) {
            return null;
        }
        JournalEntry entry = record("battle:" + id, getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.battle", name,
              outcome));
        if (entry != null) {
            castMembers.forEach(entry::tagCharacter);
        }
        return entry;
    }

    /**
     * Records the player's answer to a Faction Standing ultimatum and how many people refused to follow it. Cast
     * members among them are tagged.
     *
     * @param ultimatumName     the ultimatum's unique name
     * @param choice            what the player chose
     * @param sideName          the name of the faction sided with, or {@code null} when going rogue
     * @param departedPersonIds the ids of everyone who left or died because of the decision
     *
     * @return the entry written, or {@code null} if the chronicle is off or this was already recorded today
     */
    public @Nullable JournalEntry ultimatumResolved(final String ultimatumName, final UltimatumChoice choice,
          final @Nullable String sideName, final Collection<UUID> departedPersonIds) {
        String text = getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.ultimatum." + choice.name(), sideName) + ' '
                            + getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.ultimatum.departed",
                                  departedPersonIds.size());
        JournalEntry entry = record("ultimatum:" + ultimatumName, text);
        if (entry != null) {
            castMembersFor(departedPersonIds).forEach(entry::tagCharacter);
        }
        return entry;
    }

    /**
     * Records a person in the cast joining, being born, dying, going missing, being captured or leaving. See
     * {@link #personChanged(PersonChange, UUID, String, String, Collection)}.
     *
     * @return the entry written or added to, or {@code null} if nothing was recorded
     */
    public @Nullable JournalEntry personChanged(final PersonChange change, final UUID personId, final String name,
          final @Nullable String detail) {
        return personChanged(change, personId, name, detail, List.of());
    }

    /**
     * Records a person joining, being born, dying, going missing, being captured or leaving, if it concerns the
     * current cast: the person, or one of their relatives, must be linked to someone in the cast. Everyone else's
     * comings and goings would bury the story. The day's people with the same change share one entry, and the cast
     * members involved are tagged on it.
     *
     * @param change    what happened
     * @param personId  the person's id
     * @param name      the person's name
     * @param detail    what exactly happened, such as "Killed in Action", or {@code null}
     * @param relatives the ids of people whose being in the cast also makes this worth recording, such as a newborn's
     *                  parents
     *
     * @return the entry written or added to, or {@code null} if the chronicle is off, nobody involved is in the cast,
     *       or this was already recorded
     */
    public @Nullable JournalEntry personChanged(final PersonChange change, final UUID personId, final String name,
          final @Nullable String detail, final Collection<UUID> relatives) {
        Set<UUID> involved = new HashSet<>(relatives);
        involved.add(personId);
        List<OracleCharacter> castMembers = castMembersFor(involved);
        if (!enabled.getAsBoolean() || castMembers.isEmpty() || isRepeat("person:" + change + ":" + personId)) {
            return null;
        }
        LocalDate date = today.get();
        String shown = (detail == null || detail.isBlank()) ? name : name + " (" + detail + ")";
        Group group = groups.get(change);
        // Keep adding to today's entry while it is still in the log; otherwise start a new one.
        if (group == null || !group.date().equals(date) || !roleplay.getOracleLog().contains(group.entry())) {
            JournalEntry entry = roleplay.logOracle(date, JournalEntryType.CHRONICLE, "",
                  maximumLogEntries.getAsInt());
            group = new Group(date, entry, new ArrayList<>());
            groups.put(change, group);
        }
        group.names().add(shown);
        group.entry().setText(getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.person." + change.name(),
              String.join(", ", group.names())));
        castMembers.forEach(group.entry()::tagCharacter);
        return group.entry();
    }

    /**
     * @param personIds the ids of people in the campaign
     *
     * @return the active cast members linked to any of them, in cast order
     */
    private List<OracleCharacter> castMembersFor(final Collection<UUID> personIds) {
        return roleplay.getActiveCharacters().stream()
                     .filter(character -> character.isLinked() && personIds.contains(character.getPersonId()))
                     .toList();
    }
}
