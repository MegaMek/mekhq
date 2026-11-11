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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches glossary entries by their titles and, optionally, by the text of their definitions, so players can find an
 * entry from words they remember reading in it rather than only from its label.
 *
 * <p>A query is split into terms at spaces; a phrase in double quotes is kept as one term. Every term must appear
 * somewhere in the entry. A term also matches its simple plural and "-ing" forms, so "contracts" finds "contract".
 * Matches in the title count for far more than matches in the text, and words found together as typed count for more
 * than the same words scattered through an entry.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossarySearch {
    private static final int MINIMUM_TERM_LENGTH = 2;
    private static final int MAXIMUM_COUNTED_OCCURRENCES = 12;
    private static final Pattern QUERY_TERM = Pattern.compile("\"([^\"]+)\"|(\\S+)");

    private GlossarySearch() {
    }

    /** Where an entry's best match was found. */
    public enum Where {
        /** Every term is in the title. */
        TITLE,
        /** At least one term is only in the entry's text. */
        TEXT
    }

    /**
     * One searchable entry.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class Document {
        private final String key;
        private final String title;
        private final String text;
        private final String titleLower;
        private final String textLower;

        /**
         * @param key   the entry's lookup name
         * @param title the entry's title
         * @param text  the entry's plain text, as {@link GlossaryText#plainText(String)} gives it
         */
        public Document(String key, String title, String text) {
            this.key = key;
            this.title = title;
            this.text = text;
            this.titleLower = title.toLowerCase(Locale.ROOT);
            this.textLower = text.toLowerCase(Locale.ROOT);
        }

        public String key() {
            return key;
        }

        public String title() {
            return title;
        }

        public String text() {
            return text;
        }
    }

    /**
     * One entry that matched a query.
     *
     * @param document the entry
     * @param score    how well it matched; higher is better
     * @param where    where it matched
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Hit(Document document, int score, Where where) {}

    /**
     * Splits a query into lower-case terms. A phrase in double quotes is one term; terms shorter than two characters
     * are dropped.
     *
     * @param query the query as typed
     *
     * @return the terms, in the order typed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> parseQuery(String query) {
        List<String> terms = new ArrayList<>();
        Matcher matcher = QUERY_TERM.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String term = (matcher.group(1) != null ? matcher.group(1) : matcher.group(2)).strip();
            if (term.length() >= MINIMUM_TERM_LENGTH) {
                terms.add(term);
            }
        }
        return terms;
    }

    /**
     * Searches the entries, best match first; equal matches are in title order.
     *
     * @param documents    the entries
     * @param query        the query as typed
     * @param searchInText {@code true} to search the entries' text as well as their titles
     *
     * @return the matching entries, or none if the query has no usable terms
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Hit> search(Collection<Document> documents, String query, boolean searchInText) {
        List<String> terms = parseQuery(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        List<Hit> hits = new ArrayList<>();
        for (Document document : documents) {
            score(document, terms, searchInText).ifPresent(hits::add);
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed()
                        .thenComparing(hit -> hit.document().title(), String.CASE_INSENSITIVE_ORDER));
        return hits;
    }

    /**
     * Scores one entry against a query's terms.
     *
     * @param document     the entry
     * @param terms        the query's terms, from {@link #parseQuery(String)}
     * @param searchInText {@code true} to search the entry's text as well as its title
     *
     * @return the match, or empty if some term is not in the entry
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Optional<Hit> score(Document document, List<String> terms, boolean searchInText) {
        int score = 0;
        int termsInTitle = 0;
        for (String term : terms) {
            int best = 0;
            boolean inTitle = false;
            boolean inText = false;
            for (String variant : variants(term)) {
                int titleScore = titleScore(document.titleLower, variant);
                if (titleScore > 0) {
                    inTitle = true;
                    best = Math.max(best, titleScore);
                }
                if (searchInText) {
                    int occurrences = countOccurrences(document.textLower, variant);
                    if (occurrences > 0) {
                        inText = true;
                        best = Math.max(best, 20 + Math.min(occurrences, MAXIMUM_COUNTED_OCCURRENCES) * 5);
                    }
                }
            }
            if (!inTitle && !inText) {
                return Optional.empty();
            }
            if (inTitle) {
                termsInTitle++;
            }
            score += best;
        }

        // Words found together as typed count for more than the same words scattered through an entry.
        if (terms.size() > 1) {
            String phrase = String.join(" ", terms);
            if (document.titleLower.contains(phrase)) {
                score += 300;
            } else if (searchInText && document.textLower.contains(phrase)) {
                score += 120;
            }
        }
        Where where = (termsInTitle == terms.size()) ? Where.TITLE : Where.TEXT;
        return Optional.of(new Hit(document, score, where));
    }

    private static int titleScore(String titleLower, String variant) {
        if (titleLower.equals(variant)) {
            return 1000;
        } else if (titleLower.startsWith(variant)) {
            return 400;
        } else if (startsAWord(titleLower, variant)) {
            return 260;
        } else if (titleLower.contains(variant)) {
            return 150;
        }
        return 0;
    }

    private static boolean startsAWord(String text, String variant) {
        int index = text.indexOf(variant);
        while (index != -1) {
            if (index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1))) {
                return true;
            }
            index = text.indexOf(variant, index + 1);
        }
        return false;
    }

    private static int countOccurrences(String text, String variant) {
        int count = 0;
        int index = text.indexOf(variant);
        while (index != -1 && count < MAXIMUM_COUNTED_OCCURRENCES) {
            count++;
            index = text.indexOf(variant, index + variant.length());
        }
        return count;
    }

    /**
     * Returns a term and the simpler forms it also matches: without a plural "s" or "es", or an "-ing".
     *
     * @param term a lower-case term
     *
     * @return the term first, then its simpler forms
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> variants(String term) {
        List<String> variants = new ArrayList<>();
        variants.add(term);
        if (term.length() > 4 && term.endsWith("es")) {
            variants.add(term.substring(0, term.length() - 2));
        }
        if (term.length() > 3 && term.endsWith("s")) {
            variants.add(term.substring(0, term.length() - 1));
        }
        if (term.length() > 5 && term.endsWith("ing")) {
            variants.add(term.substring(0, term.length() - 3));
        }
        return variants;
    }

    /**
     * A pattern matching every term and its simpler forms, longest first, ignoring case; for highlighting matches.
     *
     * @param terms the query's terms
     *
     * @return the pattern, or empty if there are no terms
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Optional<Pattern> highlightPattern(List<String> terms) {
        List<String> all = terms.stream()
                                 .flatMap(term -> variants(term).stream())
                                 .distinct()
                                 .sorted(Comparator.comparingInt(String::length).reversed())
                                 .map(Pattern::quote)
                                 .toList();
        if (all.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pattern.compile(String.join("|", all), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    /**
     * Cuts an excerpt of an entry's text around the first match, for showing why the entry was found. Where the terms
     * appear together as typed, the excerpt shows that place.
     *
     * @param document the entry
     * @param terms    the query's terms
     * @param before   roughly how many characters to keep before the match
     * @param after    roughly how many characters to keep after the match's start
     *
     * @return the excerpt, with an ellipsis where it was cut, or empty if no term is in the text
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String snippet(Document document, List<String> terms, int before, int after) {
        // Lower-casing can change a string's length; the excerpt is then cut from the lower-case text.
        String text = document.text.length() == document.textLower.length() ? document.text : document.textLower;
        int index = terms.size() > 1 ? document.textLower.indexOf(String.join(" ", terms)) : -1;
        if (index == -1) {
            for (String term : terms) {
                for (String variant : variants(term)) {
                    int found = document.textLower.indexOf(variant);
                    if (found != -1 && (index == -1 || found < index)) {
                        index = found;
                    }
                }
            }
        }
        if (index == -1) {
            return "";
        }

        int start = Math.max(0, index - before);
        int end = Math.min(text.length(), index + after);
        if (start > 0) {
            int space = text.indexOf(' ', start);
            start = (space == -1 || space >= index) ? start : space + 1;
        }
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            end = (space > index) ? space : end;
        }
        return (start > 0 ? "\u2026" : "") + text.substring(start, end) + (end < text.length() ? "\u2026" : "");
    }
}
