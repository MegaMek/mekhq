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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import megamek.common.annotations.Nullable;

/**
 * Writes what happens in the campaign into the Oracle log, so the journal records the story MekHQ plays out as well as
 * the player's own: contracts, battles, arrivals, and people joining, dying or leaving. People events on the same day
 * are gathered into one entry, such as "Killed: Ana, Ben", so a hard battle does not bury the journal.
 *
 * <p>This class holds the rules; {@link CampaignChronicleListener} feeds it MekHQ's events.</p>
 */
public class CampaignChronicle {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /** The kinds of people event gathered into one entry per day. */
    public enum PersonChange {
        JOINED, BORN, KILLED, MISSING, CAPTURED, DEPARTED
    }

    private record Group(LocalDate date, JournalEntry entry, List<String> names) {}

    private final Roleplay roleplay;
    private final Supplier<LocalDate> today;
    private final IntSupplier maximumLogEntries;
    private final BooleanSupplier enabled;
    private final Map<PersonChange, Group> groups = new EnumMap<>(PersonChange.class);
    /** What has been recorded today, so an event MekHQ announces twice is written once. */
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
                            : getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.contractStartedAt", name, employer, system);
        return record("contract-new:" + id, text);
    }

    /** @return the entry for a finished contract */
    public @Nullable JournalEntry contractEnded(final UUID id, final String name, final String outcome) {
        return record("contract-end:" + id, getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.contractEnded", name,
              outcome));
    }

    /** @return the entry for a resolved battle */
    public @Nullable JournalEntry battleResolved(final int id, final String name, final String outcome) {
        return record("battle:" + id, getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.battle", name, outcome));
    }

    /** @return the entry for arriving in a system */
    public @Nullable JournalEntry arrived(final String system) {
        return record("arrived:" + system, getFormattedTextAt(RESOURCE_BUNDLE, "Chronicle.arrived", system));
    }

    /**
     * Records a person joining, being born, dying, going missing, being captured or leaving. The day's people with the
     * same change share one entry, and anyone in the cast is tagged on it.
     *
     * @param change   what happened
     * @param personId the person's id
     * @param name     the person's name
     * @param detail   what exactly happened, such as "Killed in Action", or {@code null}
     *
     * @return the entry written or added to, or {@code null} if the chronicle is off or this was already recorded
     */
    public @Nullable JournalEntry personChanged(final PersonChange change, final UUID personId, final String name,
          final @Nullable String detail) {
        if (!enabled.getAsBoolean() || isRepeat("person:" + change + ":" + personId)) {
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
        for (OracleCharacter character : roleplay.getCharacters()) {
            if (personId.equals(character.getPersonId())) {
                group.entry().tagCharacter(character);
            }
        }
        return group.entry();
    }
}
