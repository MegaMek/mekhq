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
package mekhq.gui.dialog.glossary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds words in guides: on the pages of one guide, for the viewer's find box, and across every guide, for its
 * library.
 *
 * <p>Searching ignores case. Page text has its white space collapsed when read, so a phrase broken across lines is
 * still found.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuideSearch {
    /** Queries shorter than this find nothing, as nearly every page has every single letter. */
    static final int MINIMUM_QUERY_LENGTH = 2;

    private GuideSearch() {
    }

    /**
     * One place the words appear.
     *
     * @param page   the page, from 1
     * @param start  where on the page's text the words start
     * @param length how long the words are
     */
    record PageHit(int page, int start, int length) {}

    /**
     * One guide, as the library searches it.
     *
     * @param key       the guide's lookup name
     * @param title     the guide's title
     * @param pageTexts the text of each page, or an empty list while the guide has not been read yet
     */
    record Guide(String key, String title, List<String> pageTexts) {}

    /**
     * What a library search found: guides whose titles hold the words, and pages that hold them.
     *
     * @param titleMatches the guides whose titles hold the words, in the order given
     * @param pageMatches  the pages that hold the words, at most one per page, guide by guide
     */
    record LibraryResult(List<Guide> titleMatches, List<LibraryHit> pageMatches) {}

    /**
     * A page of a guide that holds the words.
     *
     * @param guide the guide
     * @param hit   the first place on the page they appear
     */
    record LibraryHit(Guide guide, PageHit hit) {}

    /**
     * @param query the words as typed
     *
     * @return the words to look for, in lower case, or empty if there are too few letters to search for
     */
    static String normalise(String query) {
        String normalised = GuideReader.clean(query).toLowerCase(Locale.ROOT);
        return normalised.length() < MINIMUM_QUERY_LENGTH ? "" : normalised;
    }

    /**
     * Finds every place the words appear in a guide.
     *
     * @param pageTexts the text of each page
     * @param query     the words as typed
     *
     * @return every place, page by page
     */
    static List<PageHit> find(List<String> pageTexts, String query) {
        String needle = normalise(query);
        List<PageHit> hits = new ArrayList<>();
        if (needle.isEmpty()) {
            return hits;
        }
        for (int page = 0; page < pageTexts.size(); page++) {
            String text = lower(pageTexts.get(page));
            int at = text.indexOf(needle);
            while (at >= 0) {
                hits.add(new PageHit(page + 1, at, needle.length()));
                at = text.indexOf(needle, at + needle.length());
            }
        }
        return hits;
    }

    /**
     * Searches every guide's title and, if asked, every page of every guide that has been read.
     *
     * @param guides       the guides
     * @param query        the words as typed
     * @param searchInside {@code true} to search the guides' pages as well as their titles
     *
     * @return what was found
     */
    static LibraryResult searchLibrary(List<Guide> guides, String query, boolean searchInside) {
        String needle = normalise(query);
        List<Guide> titleMatches = new ArrayList<>();
        List<LibraryHit> pageMatches = new ArrayList<>();
        if (needle.isEmpty()) {
            return new LibraryResult(titleMatches, pageMatches);
        }
        for (Guide guide : guides) {
            if (lower(guide.title()).contains(needle)) {
                titleMatches.add(guide);
            }
            if (!searchInside) {
                continue;
            }
            for (int page = 0; page < guide.pageTexts().size(); page++) {
                int at = lower(guide.pageTexts().get(page)).indexOf(needle);
                if (at >= 0) {
                    pageMatches.add(new LibraryHit(guide, new PageHit(page + 1, at, needle.length())));
                }
            }
        }
        return new LibraryResult(titleMatches, pageMatches);
    }

    /**
     * Cuts the passage around a place on a page, for showing where the words were found.
     *
     * @param text   the page's text
     * @param hit    the place
     * @param before roughly how many characters to keep before it
     * @param after  roughly how many characters to keep after it
     *
     * @return the passage, as three parts: before the words, the words, and after them; each cut part starts or
     *       ends with an ellipsis
     */
    static String[] passage(String text, PageHit hit, int before, int after) {
        int start = Math.max(0, hit.start() - before);
        int end = Math.min(text.length(), hit.start() + hit.length() + after);
        // Cut at word boundaries where there is one nearby.
        if (start > 0) {
            int space = text.indexOf(' ', start);
            if (space >= 0 && space < hit.start()) {
                start = space + 1;
            }
        }
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space > hit.start() + hit.length()) {
                end = space;
            }
        }
        int wordsEnd = Math.min(text.length(), hit.start() + hit.length());
        return new String[] { (start > 0 ? "\u2026" : "") + text.substring(start, hit.start()),
                              text.substring(hit.start(), wordsEnd),
                              text.substring(wordsEnd, end) + (end < text.length() ? "\u2026" : "") };
    }

    /**
     * Lower-cases text without changing its length, so places found in it are places in the original. A character
     * whose lower case is longer is left as it is.
     */
    static String lower(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.length() == text.length()) {
            return lower;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            builder.append(Character.toLowerCase(text.charAt(i)));
        }
        return builder.toString();
    }
}
