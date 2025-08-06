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
    AGING_EFFECTS("AGING_EFFECTS", "Personnel Modules/Aging Effects"),
    AWARDS_MODULE("AWARDS_MODULE", "Personnel Modules/Awards Module"),
    EDUCATION_MODULE("EDUCATION_MODULE", "Personnel Modules/Education Module"),
    RANDOM_DEATH("RANDOM_DEATH", "Personnel Modules/Random Death in MekHQ"),
    RANDOM_DEPENDENTS("RANDOM_DEPENDENTS", "Personnel Modules/Random Dependents"),
    RANDOM_PERSONALITIES("RANDOM_PERSONALITIES", "Personnel Modules/Random Personalities"),
    STARTING_ATTRIBUTE_SCORES("STARTING_ATTRIBUTE_SCORES", "Personnel Modules/Starting Attribute Scores"),
    TURNOVER_AND_RETENTION("TURNOVER_AND_RETENTION", "Personnel Modules/Turnover & Retention Module (feat. Fatigue)"),
    PRISONERS_OF_WAR("PRISONERS_OF_WAR", "MekHQ Systems/Prisoners of War & Abstracted Search and Rescue"),
    ACAR("ACAR", "StratCon/ACAR-Abstract Combat Auto-Resolve documentation"),
    ADMIN_SKILLS("ADMIN_SKILLS", "StratCon/Admin Skills"),
    COMBAT_TEAMS("COMBAT_TEAMS", "StratCon/Combat Teams, Roles, Training & Reinforcements"),
    MORALE("MORALE", "StratCon/MekHQ Morale"),
    RESUPPLY_AND_CONVOYS("RESUPPLY_AND_CONVOYS", "StratCon/Resupply & Convoys"),
    UNIT_MARKETS("UNIT_MARKETS", "StratCon/Unit Markets"),
    CHAOS_CAMPAIGNS("CHAOS_CAMPAIGNS", "MegaMek -MekHQ Chaos Campaign Guide"),
    COOP_CAMPAIGNS("COOP_CAMPAIGNS", "MekHQ Co-Op Campaign Guide v1"),
    NEW_PLAYER_GUIDE("NEW_PLAYER_GUIDE", "0_MHQ New Player Guide"),
    RECRUITMENT("RECRUITMENT", "Personnel Modules/Recruitment"),
    FACTION_STANDINGS("FACTION_STANDINGS", "MekHQ Systems/Faction Standings");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DocumentationEntry";

    private final String DIRECTORY = "docs/";
    private final String FILE_EXTENSION = ".pdf";

    private final String lookUpName;
    private final String fileAddress;

    /**
     * Constructs a {@code DocumentationEntry} with the specified lookup name.
     *
     * @param lookUpName  the resource key used to look up this entry's localized strings
     * @param fileAddress the document address
     *
     * @author Illiani
     * @since 0.50.07
     */
    DocumentationEntry(String lookUpName, String fileAddress) {
        this.lookUpName = lookUpName;
        this.fileAddress = fileAddress;
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
