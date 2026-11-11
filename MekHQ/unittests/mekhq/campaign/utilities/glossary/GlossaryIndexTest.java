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
package mekhq.campaign.utilities.glossary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.Version;
import mekhq.campaign.utilities.glossary.GlossaryIndex.Item;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlossaryIndex}, read from the real glossary: every entry is listed once in reading order, and the
 * links between entries run both ways.
 */
class GlossaryIndexTest {
    private static final GlossaryIndex INDEX = GlossaryIndex.build();

    @Test
    void everyEntryIsListedOnce() {
        assertEquals(GlossaryEntry.values().length, INDEX.items().size());
        assertEquals(GlossaryEntry.values().length, INDEX.items().stream().map(Item::entry).distinct().count());
    }

    @Test
    void entriesAreInTitleOrderIgnoringCase() {
        List<String> titles = INDEX.items().stream().map(Item::title).toList();
        List<String> sorted = new ArrayList<>(titles);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);

        assertEquals(sorted, titles);
    }

    @Test
    void everyEntryHasSearchableText() {
        for (Item item : INDEX.items()) {
            assertFalse(item.document().text().isBlank(), item.entry() + " has no text");
            assertFalse(item.document().text().contains("<p>"), item.entry() + " still has tags in its text");
        }
    }

    @Test
    void anEntryIsReferencedByEveryEntryThatLinksToIt() {
        for (Item item : INDEX.items()) {
            for (GlossaryEntry linked : item.seeAlso()) {
                assertTrue(INDEX.referencedBy(linked).contains(item.entry()),
                      item.entry() + " links to " + linked + " but is not listed as referring to it");
            }
        }
    }

    @Test
    void anEntryNeverListsItselfAsSeeAlso() {
        for (Item item : INDEX.items()) {
            assertFalse(item.seeAlso().contains(item.entry()), item.entry() + " lists itself");
        }
    }

    @Test
    void entriesAreFoundByLookUpName() {
        for (GlossaryEntry entry : GlossaryEntry.values()) {
            assertEquals(entry, INDEX.entry(entry.getLookUpName()));
            assertNotNull(INDEX.item(entry));
        }
    }

    @Test
    void changedEntriesAreGroupedByVersionNewestFirst() {
        List<GlossaryEntry> entries = List.of(GlossaryEntry.values()).subList(0, 6);
        Version newest = entries.stream().map(GlossaryEntry::getVersionAddedOrLastUpdated).max(Version::compareTo)
                               .orElseThrow();
        GlossaryIndex index = GlossaryIndex.build(entries,
              version -> version.equals(newest) ? GlossaryChange.THIS_BUILD : GlossaryChange.SINCE_LAST_CORE);

        Map<Version, List<Item>> grouped = index.changedByVersion();

        assertEquals(newest, grouped.keySet().iterator().next());
        assertEquals(6, grouped.values().stream().mapToInt(List::size).sum());
    }
}
