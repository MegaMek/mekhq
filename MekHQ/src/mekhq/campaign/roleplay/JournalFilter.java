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

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import megamek.common.annotations.Nullable;

/**
 * Criteria for narrowing the journal. Every criterion is optional: a {@code null} (or blank search text) matches
 * everything.
 *
 * @param text      text the entry must contain, ignoring case and formatting
 * @param from      the earliest date to include
 * @param to        the latest date to include
 * @param type      the only type of entry to include
 * @param thread    the id of a plot thread the entry must be tagged with
 * @param character the name of an Oracle character the entry must be tagged with
 */
public record JournalFilter(@Nullable String text, @Nullable LocalDate from, @Nullable LocalDate to,
      @Nullable JournalEntryType type, @Nullable UUID thread, @Nullable String character) {
    /** A filter that matches every entry. */
    public static final JournalFilter ALL = new JournalFilter(null, null, null, null, null, null);

    /**
     * @param entry a journal entry
     *
     * @return {@code true} if the entry meets every criterion
     */
    public boolean matches(final JournalEntry entry) {
        if (from != null && entry.getDate().isBefore(from)) {
            return false;
        }
        if (to != null && entry.getDate().isAfter(to)) {
            return false;
        }
        if (type != null && entry.getType() != type) {
            return false;
        }
        if (thread != null && !entry.getThreads().contains(thread)) {
            return false;
        }
        if (character != null && !entry.getCharacters().contains(character)) {
            return false;
        }
        if (text != null && !text.isBlank()) {
            String needle = text.strip().toLowerCase(Locale.ROOT);
            return entry.getPlainText().toLowerCase(Locale.ROOT).contains(needle);
        }
        return true;
    }

    /**
     * @return {@code true} if this filter narrows anything
     */
    public boolean isActive() {
        return (text != null && !text.isBlank()) || from != null || to != null || type != null || thread != null
                     || character != null;
    }

    /**
     * @param newText the search text
     *
     * @return a copy of this filter with different search text
     */
    public JournalFilter withText(final @Nullable String newText) {
        return new JournalFilter(newText, from, to, type, thread, character);
    }
}
