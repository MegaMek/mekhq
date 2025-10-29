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

import megamek.Version;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;

/**
 * The {@code GlossaryEntry} enum represents individual glossary entries used within MekHQ.
 *
 * <p>Each entry is associated with a lookup name, a localized title, and a localized definition.</p>
 *
 * <p>Methods are provided to retrieve localized strings and to access entries based on their lookup names or sorted
 * order.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum GlossaryEntry {
    ADMIN_STRAIN("HR_STRAIN", new Version("0.50.06")),
    ADVANCED_SCOUTING("ADVANCED_SCOUTING", new Version("0.50.10")),
    AGING("AGING", new Version("0.50.06")),
    AREA_OF_OPERATIONS("AREA_OF_OPERATIONS", new Version("0.50.06")),
    ATB("ATB", new Version("0.50.06")),
    ATOW_TRAITS("ATOW_TRAITS", new Version("0.50.06")),
    ATTRIBUTE_SCORES("ATTRIBUTE_SCORES", new Version("0.50.06")),
    AUTOINFIRMARY("AUTOINFIRMARY", new Version("0.50.06")),
    BLOODMARK("BLOODMARK", new Version("0.50.07")),
    CAPITAL_SYSTEMS("CAPITAL_SYSTEMS", new Version("0.50.06")),
    CHANGING_FILTERS_HANGAR("CHANGING_FILTERS_HANGAR", new Version("0.50.06")),
    CHANGING_FILTERS_PERSONNEL("CHANGING_FILTERS_PERSONNEL", new Version("0.50.06")),
    CHANGING_FILTERS_WAREHOUSE("CHANGING_FILTERS_WAREHOUSE", new Version("0.50.06")),
    COMBAT_ROLES("COMBAT_ROLES", new Version("0.50.10")),
    COMBAT_TEAMS("COMBAT_TEAMS", new Version("0.50.06")),
    COMPLETE_MISSION_GUIDANCE("COMPLETE_MISSION_GUIDANCE", new Version("0.50.06")),
    CONNECTIONS("CONNECTIONS", new Version("0.50.07")),
    CONTRACT_VICTORY_POINTS("CONTRACT_VICTORY_POINTS", new Version("0.50.06")),
    CREW_REQUIREMENTS("CREW_REQUIREMENTS", new Version("0.50.06")),
    CRISIS_SCENARIO("CRISIS_SCENARIO", new Version("0.50.06")),
    DAMAGED_PARTS("DAMAGED_PARTS", new Version("0.50.06")),
    DELIVERY_TIMES("DELIVERY_TIMES", new Version("0.50.06")),
    DEPRECATED("DEPRECATED", new Version("0.50.06")),
    DIGITAL_GM("DIGITAL_GM", new Version("0.50.06")),
    EDGE("EDGE", new Version("0.50.06")),
    EDUCATION("EDUCATION", new Version("0.50.06")),
    EMPTY_SYSTEMS("EMPTY_SYSTEMS", new Version("0.50.06")),
    EXPERIENCE_COSTS("EXPERIENCE_COSTS", new Version("0.50.06")),
    EXPERIENCE_RATING("EXPERIENCE_RATING", new Version("0.50.06")),
    EXTRA_INCOME("EXTRA_INCOME", new Version("0.50.10")),
    FATIGUE("FATIGUE", new Version("0.50.06")),
    FIELD_CONTROL("FIELD_CONTROL", new Version("0.50.06")),
    FIELD_KITCHENS("FIELD_KITCHENS", new Version("0.50.06")),
    FORCE_REPUTATION("FORCE_REPUTATION", new Version("0.50.06")),
    FORCE_TYPE_COMBAT("FORCE_TYPE_COMBAT", new Version("0.50.06")),
    FORCE_TYPE_CONVOY("FORCE_TYPE_CONVOY", new Version("0.50.06")),
    FORCE_TYPE_SECURITY("FORCE_TYPE_SECURITY", new Version("0.50.06")),
    FORCE_TYPE_SUPPORT("FORCE_TYPE_SUPPORT", new Version("0.50.06")),
    GROUP_BY_UNIT("GROUP_BY_UNIT", new Version("0.50.06")),
    HOSPITAL_BEDS("HOSPITAL_BEDS", new Version("0.50.10")),
    HOW_TO_DEPLOY("HOW_TO_DEPLOY", new Version("0.50.06")),
    HOW_TO_REINFORCE("HOW_TO_REINFORCE", new Version("0.50.06")),
    INFANTRY_GUNNERY_SKILLS("INFANTRY_GUNNERY_SKILLS", new Version("0.50.06")),
    JUMP_COST_CALCULATIONS("JUMP_COST_CALCULATIONS", new Version("0.50.10")),
    LEADERSHIP_UNITS("LEADERSHIP_UNITS", new Version("0.50.06")),
    LINKED_ATTRIBUTES("LINKED_ATTRIBUTES", new Version("0.50.06")),
    LOYALTY("LOYALTY", new Version("0.50.06")),
    MAINTENANCE("MAINTENANCE", new Version("0.50.06")),
    MANAGEMENT_SKILL("MANAGEMENT_SKILL", new Version("0.50.06")),
    MASS_REPAIR_MASS_SALVAGE("MASS_REPAIR_MASS_SALVAGE", new Version("0.50.06")),
    MISSING_IN_ACTION("MISSING_IN_ACTION", new Version("0.50.06")),
    MISSING_LIMBS("MISSING_LIMBS", new Version("0.50.06")),
    MISSIONS_AND_CONTRACTS("MISSIONS_AND_CONTRACTS", new Version("0.50.06")),
    MORALE("MORALE", new Version("0.50.06")),
    NAVIGATION_INFIRMARY("NAVIGATION_INFIRMARY", new Version("0.50.06")),
    NAVIGATION_INTERSTELLAR_MAP("NAVIGATION_INTERSTELLAR_MAP", new Version("0.50.06")),
    NEW_PLAYER_GUIDE("NEW_PLAYER_GUIDE", new Version("0.50.06")),
    PARTS_AVAILABILITY("PARTS_AVAILABILITY", new Version("0.50.06")),
    PARTS_IN_USE("PARTS_IN_USE", new Version("0.50.06")),
    PRISONERS_OF_WAR("PRISONERS_OF_WAR", new Version("0.50.06")),
    PRISONER_CAPACITY("PRISONER_CAPACITY", new Version("0.50.06")),
    PROFESSIONS("PROFESSIONS", new Version("0.50.06")),
    RANDOM_PERSONALITIES("RANDOM_PERSONALITIES", new Version("0.50.06")),
    RELEASE_TYPES("RELEASE_TYPES", new Version("0.50.06")),
    RELEASE_TYPE_DEVELOPMENT("RELEASE_TYPE_DEVELOPMENT", new Version("0.50.06")),
    RELEASE_TYPE_MILESTONE("RELEASE_TYPE_MILESTONE", new Version("0.50.06")),
    RELEASE_TYPE_NIGHTLY("RELEASE_TYPE_NIGHTLY", new Version("0.50.06")),
    RELOADING_FIELD_GUNS("RELOADING_FIELD_GUNS", new Version("0.50.06")),
    REMAIN_DEPLOYED("REMAIN_DEPLOYED", new Version("0.50.06")),
    REPAIRING_DAMAGED_HIP_SHOULDER("REPAIRING_DAMAGED_HIP_SHOULDER", new Version("0.50.06")),
    REPAIR_SITE("REPAIR_SITE", new Version("0.50.06")),
    RESUPPLY("RESUPPLY", new Version("0.50.06")),
    SCENARIO_VICTORY_POINTS("SCENARIO_VICTORY_POINTS", new Version("0.50.06")),
    SEED_FORCES("SEED_FORCES", new Version("0.50.06")),
    SKILL_TYPES("SKILL_TYPES", new Version("0.50.06")),
    STRATCON("STRATCON", new Version("0.50.06")),
    STRATCON_FACILITIES("STRATCON_FACILITIES", new Version("0.50.06")),
    STRATEGIC_OBJECTIVES("STRATEGIC_OBJECTIVES", new Version("0.50.06")),
    STRIPPING_AND_REPAIRING("STRIPPING_AND_REPAIRING", new Version("0.50.06")),
    SUPPORT_POINTS("SUPPORT_POINTS", new Version("0.50.06")),
    TECH_TIME("TECH_TIME", new Version("0.50.06")),
    TEMP_PERSONNEL("TEMP_PERSONNEL", new Version("0.50.06")),
    TOE("TOE", new Version("0.50.06")),
    TURNING_POINT("TURNING_POINT", new Version("0.50.06")),
    TURNOVER("TURNOVER", new Version("0.50.06")),
    UNABLE_TO_START_SCENARIO("UNABLE_TO_START_SCENARIO", new Version("0.50.06")),
    VEHICLE_CREWS("VEHICLE_CREWS", new Version("0.50.06")),
    VOCATIONAL_XP("VOCATIONAL_XP", new Version("0.50.06")),
    WINTER_HOLIDAY("WINTER_HOLIDAY", new Version("0.50.06"));

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GlossaryEntry";

    private final String lookUpName;
    private final Version versionAddedOrLastUpdated;

    /**
     * Constructs a {@code GlossaryEntry} with the specified lookup name.
     *
     * @param lookUpName                the resource key used to look up this entry's localized strings
     * @param versionAddedOrLastUpdated the last {@link Version} this entry was added or last updated in the glossary
     *
     * @author Illiani
     * @since 0.50.07
     */
    GlossaryEntry(String lookUpName, Version versionAddedOrLastUpdated) {
        this.lookUpName = lookUpName;
        this.versionAddedOrLastUpdated = versionAddedOrLastUpdated;
    }

    /**
     * Returns the lookup name for this glossary entry.
     *
     * @return the lookup name for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookUpName() {
        return lookUpName;
    }

    /**
     * Returns the localized title for this glossary entry.
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
     * Returns the title of this object, possibly appended with an icon/text indicating if it was added or updated since
     * the last development or milestone version.
     *
     * <p>If {@link #versionAddedOrLastUpdated} is equal to or newer than the current version,
     * an "added since last development" icon/text is appended.</p>
     *
     * <p>Otherwise, if {@link #versionAddedOrLastUpdated} is newer than the last milestone version,
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

        // We use an inverted 'isLowerThan' check as that will include instances where 'versionAddedOrLastUpdated' is
        // the same as 'currentVersion'
        if (!versionAddedOrLastUpdated.isLowerThan(currentVersion)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_DEVELOPMENT;
        } else if (versionAddedOrLastUpdated.isHigherThan(lastMilestone)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_MILESTONE;
        }

        return title;
    }

    /**
     * Returns the localized definition for this glossary entry.
     *
     * @return the definition string for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getDefinition() {
        return getTextAt(RESOURCE_BUNDLE, lookUpName + ".definition");
    }

    /**
     * Returns a list of all lookup names, sorted in ascending order of their corresponding localized titles.
     *
     * @return a list of lookup names sorted by title
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static List<String> getLookUpNamesSortedByTitle() {
        return java.util.Arrays.stream(GlossaryEntry.values())
                     .sorted(java.util.Comparator.comparing(GlossaryEntry::getTitle))
                     .map(entry -> entry.lookUpName)
                     .toList();
    }

    /**
     * Returns the {@code GlossaryEntry} associated with the given lookup name, or {@code null} if not found.
     *
     * @param lookUpName the lookup name to search for
     *
     * @return the corresponding {@code GlossaryEntry} if found; {@code null} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable GlossaryEntry getGlossaryEntryFromLookUpName(String lookUpName) {
        for (GlossaryEntry entry : GlossaryEntry.values()) {
            if (entry.lookUpName.equals(lookUpName)) {
                return entry;
            }
        }
        return null;
    }
}
