/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

/**
 * The {@code DocumentationEntry} enum represents individual entries in the documentation glossary.
 *
 * <p>Each enum constant is associated with a lookup name used to fetch localized strings.</p>
 *
 * <p>This class provides methods to retrieve localized titles, sort entries, and look up entries by name.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum DocumentationEntry {
    /**
     * Placeholder documentation entry.
     */
    EXAMPLE("EXAMPLE");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DocumentationEntry";

    private final String lookUpName;

    /**
     * Constructs a {@code DocumentationEntry} with the specified lookup name.
     *
     * @param lookUpName the resource key used to look up this entry's localized strings
     *
     * @author Illiani
     * @since 0.50.07
     */
    DocumentationEntry(String lookUpName) {
        this.lookUpName = lookUpName;
    }

    /**
     * Returns the localized title for this documentation entry.
     *
     * @return the title string for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getTitle() {
        return getTextAt(RESOURCE_BUNDLE, lookUpName + ".title");
    }

    /**
     * Returns a list of all lookup names, sorted by their corresponding localized titles in ascending order.
     *
     * @return a list of lookup names sorted by title
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static List<String> getLookUpNamesSortedByTitle() {
        return java.util.Arrays.stream(DocumentationEntry.values())
                     .sorted(java.util.Comparator.comparing(DocumentationEntry::getTitle))
                     .map(entry -> entry.lookUpName)
                     .toList();
    }

    /**
     * Returns the {@code DocumentationEntry} associated with the given lookup name, or {@code null} if not found.
     *
     * @param lookUpName the lookup name to search for
     *
     * @return the corresponding {@code DocumentationEntry} if found; {@code null} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static DocumentationEntry getDocumentationEntryFromLookUpName(String lookUpName) {
        for (DocumentationEntry entry : DocumentationEntry.values()) {
            if (entry.lookUpName.equals(lookUpName)) {
                return entry;
            }
        }
        return null;
    }
}
