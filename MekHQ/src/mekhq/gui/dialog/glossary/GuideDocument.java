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

import java.util.List;

/**
 * What the document viewer knows about one guide before drawing it: the size of each page, the guide's own outline,
 * and the text of each page for searching.
 *
 * @param pageSizes the size of each page, in PDF points, as it is shown (turned pages already turned)
 * @param outline   the guide's bookmarks, in reading order; empty if it has none
 * @param pageTexts the text of each page, with runs of white space collapsed to single spaces
 *
 * @author Illiani
 * @since 0.51.01
 */
record GuideDocument(List<PageSize> pageSizes, List<OutlineItem> outline, List<String> pageTexts) {
    /**
     * The size of a page.
     *
     * @param width  the width, in PDF points
     * @param height the height, in PDF points
     */
    record PageSize(float width, float height) {}

    /**
     * One bookmark.
     *
     * @param title the bookmark's title
     * @param page  the page it leads to, from 1; 0 if it leads nowhere
     * @param depth 0 for a top-level bookmark, 1 for one beneath it
     */
    record OutlineItem(String title, int page, int depth) {}

    /** @return the number of pages */
    int pageCount() {
        return pageSizes.size();
    }

    /**
     * @param page a page, from 1
     *
     * @return the last bookmark at or before that page, or {@code null} if there is none
     */
    OutlineItem sectionAt(int page) {
        OutlineItem section = null;
        for (OutlineItem item : outline) {
            if (item.page() > 0 && item.page() <= page) {
                section = item;
            }
        }
        return section;
    }
}
