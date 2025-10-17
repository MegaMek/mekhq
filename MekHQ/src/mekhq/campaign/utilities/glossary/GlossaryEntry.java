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

import megamek.common.annotations.Nullable;

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
    ADMIN_STRAIN("ADMIN_STRAIN"),
    AGING("AGING"),
    AREA_OF_OPERATIONS("AREA_OF_OPERATIONS"),
    ATB("ATB"),
    ATOW_TRAITS("ATOW_TRAITS"),
    ATTRIBUTE_SCORES("ATTRIBUTE_SCORES"),
    AUTOINFIRMARY("AUTOINFIRMARY"),
    BLOODMARK("BLOODMARK"),
    CAPITAL_SYSTEMS("CAPITAL_SYSTEMS"),
    CHANGING_FILTERS_HANGAR("CHANGING_FILTERS_HANGAR"),
    CHANGING_FILTERS_PERSONNEL("CHANGING_FILTERS_PERSONNEL"),
    CHANGING_FILTERS_WAREHOUSE("CHANGING_FILTERS_WAREHOUSE"),
    COMBAT_ROLES("COMBAT_ROLES"),
    COMBAT_TEAMS("COMBAT_TEAMS"),
    COMPLETE_MISSION_GUIDANCE("COMPLETE_MISSION_GUIDANCE"),
    CONNECTIONS("CONNECTIONS"),
    CONTRACT_VICTORY_POINTS("CONTRACT_VICTORY_POINTS"),
    CREW_REQUIREMENTS("CREW_REQUIREMENTS"),
    CRISIS_SCENARIO("CRISIS_SCENARIO"),
    DAMAGED_PARTS("DAMAGED_PARTS"),
    DELIVERY_TIMES("DELIVERY_TIMES"),
    DEPRECATED("DEPRECATED"),
    DIGITAL_GM("DIGITAL_GM"),
    EDGE("EDGE"),
    EDUCATION("EDUCATION"),
    EMPTY_SYSTEMS("EMPTY_SYSTEMS"),
    EXPERIENCE_COSTS("EXPERIENCE_COSTS"),
    EXPERIENCE_RATING("EXPERIENCE_RATING"),
    FATIGUE("FATIGUE"),
    FIELD_CONTROL("FIELD_CONTROL"),
    FIELD_KITCHENS("FIELD_KITCHENS"),
    FORCE_REPUTATION("FORCE_REPUTATION"),
    FORCE_TYPE_COMBAT("FORCE_TYPE_COMBAT"),
    FORCE_TYPE_CONVOY("FORCE_TYPE_CONVOY"),
    FORCE_TYPE_SECURITY("FORCE_TYPE_SECURITY"),
    FORCE_TYPE_SUPPORT("FORCE_TYPE_SUPPORT"),
    GROUP_BY_UNIT("GROUP_BY_UNIT"),
    HOSPITAL_BEDS("HOSPITAL_BEDS"),
    HOW_TO_DEPLOY("HOW_TO_DEPLOY"),
    HOW_TO_REINFORCE("HOW_TO_REINFORCE"),
    INFANTRY_GUNNERY_SKILLS("INFANTRY_GUNNERY_SKILLS"),
    ADVANCED_SCOUTING("ADVANCED_SCOUTING"),
    LEADERSHIP_UNITS("LEADERSHIP_UNITS"),
    LINKED_ATTRIBUTES("LINKED_ATTRIBUTES"),
    LOYALTY("LOYALTY"),
    MAINTENANCE("MAINTENANCE"),
    MANAGEMENT_SKILL("MANAGEMENT_SKILL"),
    MASS_REPAIR_MASS_SALVAGE("MASS_REPAIR_MASS_SALVAGE"),
    MISSING_IN_ACTION("MISSING_IN_ACTION"),
    MISSING_LIMBS("MISSING_LIMBS"),
    MISSIONS_AND_CONTRACTS("MISSIONS_AND_CONTRACTS"),
    MORALE("MORALE"),
    NAVIGATION_INFIRMARY("NAVIGATION_INFIRMARY"),
    NAVIGATION_INTERSTELLAR_MAP("NAVIGATION_INTERSTELLAR_MAP"),
    NEW_PLAYER_GUIDE("NEW_PLAYER_GUIDE"),
    PARTS_AVAILABILITY("PARTS_AVAILABILITY"),
    PARTS_IN_USE("PARTS_IN_USE"),
    PRISONERS_OF_WAR("PRISONERS_OF_WAR"),
    PRISONER_CAPACITY("PRISONER_CAPACITY"),
    PROFESSIONS("PROFESSIONS"),
    RANDOM_PERSONALITIES("RANDOM_PERSONALITIES"),
    RELEASE_TYPES("RELEASE_TYPES"),
    RELEASE_TYPE_DEVELOPMENT("RELEASE_TYPE_DEVELOPMENT"),
    RELEASE_TYPE_MILESTONE("RELEASE_TYPE_MILESTONE"),
    RELEASE_TYPE_NIGHTLY("RELEASE_TYPE_NIGHTLY"),
    RELOADING_FIELD_GUNS("RELOADING_FIELD_GUNS"),
    REMAIN_DEPLOYED("REMAIN_DEPLOYED"),
    REPAIRING_DAMAGED_HIP_SHOULDER("REPAIRING_DAMAGED_HIP_SHOULDER"),
    REPAIR_SITE("REPAIR_SITE"),
    RESUPPLY("RESUPPLY"),
    SCENARIO_VICTORY_POINTS("SCENARIO_VICTORY_POINTS"),
    SEED_FORCES("SEED_FORCES"),
    SKILL_TYPES("SKILL_TYPES"),
    STRATCON("STRATCON"),
    STRATCON_FACILITIES("STRATCON_FACILITIES"),
    STRATEGIC_OBJECTIVES("STRATEGIC_OBJECTIVES"),
    STRIPPING_AND_REPAIRING("STRIPPING_AND_REPAIRING"),
    SUPPORT_POINTS("SUPPORT_POINTS"),
    TECH_TIME("TECH_TIME"),
    TEMP_PERSONNEL("TEMP_PERSONNEL"),
    TOE("TOE"),
    TURNING_POINT("TURNING_POINT"),
    TURNOVER("TURNOVER"),
    UNABLE_TO_START_SCENARIO("UNABLE_TO_START_SCENARIO"),
    VOCATIONAL_XP("VOCATIONAL_XP"),
    JUMP_COST_CALCULATIONS("JUMP_COST_CALCULATIONS"),
    WINTER_HOLIDAY("WINTER_HOLIDAY");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GlossaryEntry";

    final String lookUpName;

    /**
     * Constructs a {@code GlossaryEntry} with the specified lookup name.
     *
     * @param lookUpName the resource key used to look up this entry's localized strings
     *
     * @author Illiani
     * @since 0.50.07
     */
    GlossaryEntry(String lookUpName) {
        this.lookUpName = lookUpName;
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
