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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import megamek.Version;

/**
 * Everything the glossary console needs to know about the entries, read once: each entry's title, plain text, how
 * recently it changed, the entries and documents it links to, and the entries that link to it.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossaryIndex {
    /**
     * One glossary entry, as the console shows it.
     *
     * @param entry     the entry
     * @param document  the entry's searchable title and text
     * @param change    how recently the entry was added or updated
     * @param seeAlso   the entries its text links to, in the order first linked
     * @param documents the documents its text links to, in the order first linked
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Item(GlossaryEntry entry, GlossarySearch.Document document, GlossaryChange change,
          List<GlossaryEntry> seeAlso, List<DocumentationEntry> documents) {
        /** @return the entry's title */
        public String title() {
            return document.title();
        }

        /** @return the version the entry was added or last updated in */
        public Version version() {
            return entry.getVersionAddedOrLastUpdated();
        }
    }

    /** Titles in reading order, ignoring case, so "HR Strain" files among the other H entries. */
    public static final Comparator<Item> BY_TITLE = Comparator.comparing(Item::title, String.CASE_INSENSITIVE_ORDER);

    /** The most recently added or updated first; entries from the same version in title order. */
    public static final Comparator<Item> NEWEST_FIRST = Comparator.comparing(Item::version, Comparator.reverseOrder())
                                                              .thenComparing(BY_TITLE);

    private final Map<GlossaryEntry, Item> items = new EnumMap<>(GlossaryEntry.class);
    private final Map<String, GlossaryEntry> byLookUpName = new HashMap<>();
    private final Map<GlossaryEntry, List<GlossaryEntry>> referencedBy = new EnumMap<>(GlossaryEntry.class);
    private final List<Item> byTitle;

    private GlossaryIndex(List<GlossaryEntry> entries, Function<Version, GlossaryChange> classifier) {
        for (GlossaryEntry entry : entries) {
            byLookUpName.put(entry.getLookUpName(), entry);
        }
        for (GlossaryEntry entry : entries) {
            String definition = entry.getDefinition();
            List<GlossaryEntry> seeAlso = GlossaryText.glossaryLinks(definition).stream()
                                                .map(byLookUpName::get)
                                                .filter(Objects::nonNull)
                                                .filter(linked -> linked != entry)
                                                .toList();
            List<DocumentationEntry> documents = GlossaryText.documentationLinks(definition).stream()
                                                       .map(DocumentationEntry::getDocumentationEntryFromLookUpName)
                                                       .filter(Objects::nonNull)
                                                       .toList();
            GlossarySearch.Document document = new GlossarySearch.Document(entry.getLookUpName(), entry.getTitle(),
                  GlossaryText.plainText(definition));
            items.put(entry, new Item(entry, document, classifier.apply(entry.getVersionAddedOrLastUpdated()),
                  seeAlso, documents));
            for (GlossaryEntry linked : seeAlso) {
                referencedBy.computeIfAbsent(linked, key -> new ArrayList<>()).add(entry);
            }
        }
        referencedBy.values().forEach(list -> list.sort(Comparator.comparing(GlossaryEntry::getTitle,
              String.CASE_INSENSITIVE_ORDER)));
        byTitle = items.values().stream().sorted(BY_TITLE).toList();
    }

    /**
     * Reads every glossary entry, judging how recently each changed against the running build.
     *
     * @return the index
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static GlossaryIndex build() {
        return new GlossaryIndex(List.of(GlossaryEntry.values()), GlossaryChange::of);
    }

    /**
     * Reads the given entries, judging how recently each changed with the given classifier; for tests.
     *
     * @param entries    the entries
     * @param classifier how recently an entry tagged with a version changed
     *
     * @return the index
     */
    static GlossaryIndex build(List<GlossaryEntry> entries, Function<Version, GlossaryChange> classifier) {
        return new GlossaryIndex(entries, classifier);
    }

    /** @return every entry, in title order */
    public List<Item> items() {
        return byTitle;
    }

    /**
     * @param entry an entry
     *
     * @return the entry's item, or {@code null} if the index does not hold it
     */
    public @Nullable Item item(GlossaryEntry entry) {
        return items.get(entry);
    }

    /**
     * @param lookUpName an entry's lookup name
     *
     * @return the entry, or {@code null} if there is none by that name
     */
    public @Nullable GlossaryEntry entry(String lookUpName) {
        return byLookUpName.get(lookUpName);
    }

    /**
     * @param entry an entry
     *
     * @return the entries whose text links to it, in title order
     */
    public List<GlossaryEntry> referencedBy(GlossaryEntry entry) {
        return referencedBy.getOrDefault(entry, List.of());
    }

    /** @return the searchable title and text of every entry, in title order */
    public List<GlossarySearch.Document> documents() {
        return byTitle.stream().map(Item::document).toList();
    }

    /**
     * Groups the entries changed since the last core release by the version they changed in, newest first.
     *
     * @return the changed entries by version, each version's entries in title order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Map<Version, List<Item>> changedByVersion() {
        Map<Version, List<Item>> grouped = new LinkedHashMap<>();
        byTitle.stream()
              .filter(item -> item.change().isChanged())
              .sorted(NEWEST_FIRST)
              .forEach(item -> grouped.computeIfAbsent(item.version(), version -> new ArrayList<>()).add(item));
        return grouped;
    }
}
