/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import megamek.Version;
import mekhq.MHQConstants;

/**
 * The {@code DocumentationEntry} enum represents individual entries in the documentation glossary.
 *
 * <p>Each enum constant is associated with a lookup name used to fetch localized strings.</p>
 *
 * <p>This class provides methods to retrieve localized titles, sort entries, and look up entries by name.</p>
 *
 * <p>Each document is a Markdown file under {@code docs/mekhq/}, shown in the Glossary Console's reader. The files
 * are written in GitHub-flavored Markdown, so they read the same in the repository.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum DocumentationEntry {
    FORMATION_ICONS("FORMATION_ICONS", "glossaryDocs/Force Icons", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RANDOM_PORTRAITS("RANDOM_PORTRAITS", "glossaryDocs/Portrait Generation", "0.51.01.12", "0.51.01.12",
          "0.51.01.12"),
    RANKS("RANKS", "glossaryDocs/Ranks", "0.51.01.12", "0.51.01.12", "0.51.01.12");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DocumentationEntry";

    private final String DIRECTORY = "docs/mekhq/";
    private final String FILE_EXTENSION = ".md";

    private final String lookUpName;
    private final String fileAddress;
    private final Version versionAdded;
    private final Version versionLastUpdated;
    private final Version versionLastVerified;

    /**
     * Constructs a {@code DocumentationEntry} with the specified lookup name, document and version history.
     *
     * <p>Each version is the release that first shipped the change, including its revision where it has one, such
     * as {@code 0.51.01.4}. A change not yet released takes the version of the release it will ship in.</p>
     *
     * @param lookUpName          the resource key used to look up this entry's localized strings
     * @param fileAddress         the document's path under {@code docs/mekhq/}, without its {@code .md} extension
     * @param versionAdded        the release that first listed this document in the glossary
     * @param versionLastUpdated  the release that last shipped a significant revision of the document
     * @param versionLastVerified the release in which the document was last checked against the game and found
     *                            accurate
     *
     * @author Illiani
     * @since 0.50.07
     */
    DocumentationEntry(String lookUpName, String fileAddress, String versionAdded, String versionLastUpdated,
          String versionLastVerified) {
        this.lookUpName = lookUpName;
        this.fileAddress = fileAddress;
        this.versionAdded = new Version(versionAdded);
        this.versionLastUpdated = new Version(versionLastUpdated);
        this.versionLastVerified = new Version(versionLastVerified);
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
     * Returns the lookup name for this documentation entry.
     *
     * @return the lookup name for this entry
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getLookUpName() {
        return lookUpName;
    }

    /**
     * @return the release that first listed this document in the glossary
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionAdded() {
        return versionAdded;
    }

    /**
     * Returns the release that last shipped a significant revision of this document. This is never older than
     * {@link #getVersionAdded()}, and is the version that decides whether the document is shown as new or changed.
     *
     * @return the release that last significantly updated this document
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionLastUpdated() {
        return versionLastUpdated;
    }

    /**
     * @return the release in which this document was last checked and found accurate; never older than
     *       {@link #getVersionLastUpdated()}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionLastVerified() {
        return versionLastVerified;
    }

    /**
     * Returns the title of this object, possibly appended with an icon/text indicating if it was added or updated since
     * the last development or milestone version.
     *
     * <p>If {@link #versionLastUpdated} is equal to or newer than the current version,
     * an "added since last development" icon/text is appended.</p>
     *
     * <p>Otherwise, if {@link #versionLastUpdated} is newer than the last milestone version,
     * an "added since last milestone" icon/text is appended.</p>
     *
     * @return the title string, with an update/version icon appended if applicable
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getTitleWithVersionUpdateIcon() {
        Version currentVersion = MHQConstants.VERSION;
        Version lastMilestone = MHQConstants.LAST_MILESTONE;
        String title = getTitle();

        // We use an inverted 'isLowerThan' check as that will include instances where 'versionLastUpdated' is the
        // same as 'currentVersion'
        if (!versionLastUpdated.isLowerThan(currentVersion)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_DEVELOPMENT;
        } else if (versionLastUpdated.isHigherThan(lastMilestone)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_MILESTONE;
        }

        return title;
    }

    /**
     * Returns the full file address for this object by concatenating the directory path, the stored file address, and
     * the file extension constants.
     *
     * @return the complete file address as a String
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getFileAddress() {
        return DIRECTORY + fileAddress + FILE_EXTENSION;
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
        return Arrays.stream(DocumentationEntry.values())
                     .sorted(Comparator.comparing(DocumentationEntry::getTitle))
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
