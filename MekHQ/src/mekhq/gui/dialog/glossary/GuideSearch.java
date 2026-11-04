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
 * Finds words on the pages of a guide, for the guide viewer's find box.
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
