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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Enumeration of categories for life paths in the advanced character builder.
 *
 * <p>Each category is associated with a lookup name for serialization and search purposes.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum LifePathCategory {
    CLAN("CLAN"),
    DARK_CASTE("DARK_CASTE"),
    FIELD_ANALYSIS("FIELD_ANALYSIS"),
    FIELD_ANTHROPOLOGIST("FIELD_ANTHROPOLOGIST"),
    FIELD_ARCHAEOLOGIST("FIELD_ARCHAEOLOGIST"),
    FIELD_BASIC_TRAINING("FIELD_BASIC_TRAINING"),
    FIELD_BASIC_TRAINING_NAVAL("FIELD_BASIC_TRAINING_NAVAL"),
    FIELD_CARTOGRAPHER("FIELD_CARTOGRAPHER"),
    FIELD_CAVALRY("FIELD_CAVALRY"),
    FIELD_CLAN_AEROSPACE_WARRIOR("FIELD_CLAN_AEROSPACE_WARRIOR"),
    FIELD_CLAN_BASIC_TRAINING("FIELD_CLAN_BASIC_TRAINING"),
    FIELD_CLAN_CAVALRY("FIELD_CLAN_CAVALRY"),
    FIELD_CLAN_ELEMENTAL("FIELD_CLAN_ELEMENTAL"),
    FIELD_CLAN_MEKWARRIOR("FIELD_CLAN_MEKWARRIOR"),
    FIELD_CLAN_PROTOMEK_WARRIOR("FIELD_CLAN_PROTOMEK_WARRIOR"),
    FIELD_COMMUNICATIONS("FIELD_COMMUNICATIONS"),
    FIELD_COVERT_OPERATIONS("FIELD_COVERT_OPERATIONS"),
    FIELD_DETECTIVE("FIELD_DETECTIVE"),
    FIELD_DOCTOR("FIELD_DOCTOR"),
    FIELD_ENGINEER("FIELD_ENGINEER"),
    FIELD_GENERAL_STUDIES("FIELD_GENERAL_STUDIES"),
    FIELD_HPG_TECHNICIAN("FIELD_HPG_TECHNICIAN"),
    FIELD_INFANTRY("FIELD_INFANTRY"),
    FIELD_INFANTRY_ANTI_MEK("FIELD_INFANTRY_ANTI_MEK"),
    FIELD_INTELLIGENCE("FIELD_INTELLIGENCE"),
    FIELD_JOURNALIST("FIELD_JOURNALIST"),
    FIELD_LAWYER("FIELD_LAWYER"),
    FIELD_MANAGER("FIELD_MANAGER"),
    FIELD_MARINE("FIELD_MARINE"),
    FIELD_MEDICAL_ASSISTANT("FIELD_MEDICAL_ASSISTANT"),
    FIELD_MEKWARRIOR("FIELD_MEKWARRIOR"),
    FIELD_MERCHANT("FIELD_MERCHANT"),
    FIELD_MERCHANT_MARINE("FIELD_MERCHANT_MARINE"),
    FIELD_MILITARY_SCIENTIST("FIELD_MILITARY_SCIENTIST"),
    FIELD_OFFICER("FIELD_OFFICER"),
    FIELD_PILOT_AEROSPACE_CIVILIAN("FIELD_PILOT_AEROSPACE_CIVILIAN"),
    FIELD_PILOT_AEROSPACE_COMBAT("FIELD_PILOT_AEROSPACE_COMBAT"),
    FIELD_PILOT_AIRCRAFT_CIVILIAN("FIELD_PILOT_AIRCRAFT_CIVILIAN"),
    FIELD_PILOT_AIRCRAFT_COMBAT("FIELD_PILOT_AIRCRAFT_COMBAT"),
    FIELD_PILOT_BATTLE_ARMOR("FIELD_PILOT_BATTLE_ARMOR"),
    FIELD_PILOT_DROPSHIP("FIELD_PILOT_DROPSHIP"),
    FIELD_PILOT_EXOSKELETON("FIELD_PILOT_EXOSKELETON"),
    FIELD_PILOT_INDUSTRIAL_MEK("FIELD_PILOT_INDUSTRIAL_MEK"),
    FIELD_PILOT_JUMPSHIP("FIELD_PILOT_JUMPSHIP"),
    FIELD_PILOT_WARSHIP("FIELD_PILOT_WARSHIP"),
    FIELD_PLANETARY_SURVEYOR("FIELD_PLANETARY_SURVEYOR"),
    FIELD_POLICE_OFFICER("FIELD_POLICE_OFFICER"),
    FIELD_POLICE_TACTICAL_OFFICER("FIELD_POLICE_TACTICAL_OFFICER"),
    FIELD_POLITICIAN("FIELD_POLITICIAN"),
    FIELD_SCIENTIST("FIELD_SCIENTIST"),
    FIELD_SCOUT("FIELD_SCOUT"),
    FIELD_SHIPS_CREW("FIELD_SHIPS_CREW"),
    FIELD_SPECIAL_FORCES("FIELD_SPECIAL_FORCES"),
    FIELD_TECHNICIAN_AEROSPACE("FIELD_TECHNICIAN_AEROSPACE"),
    FIELD_TECHNICIAN_CIVILIAN("FIELD_TECHNICIAN_CIVILIAN"),
    FIELD_TECHNICIAN_MEK("FIELD_TECHNICIAN_MEK"),
    FIELD_TECHNICIAN_MILITARY("FIELD_TECHNICIAN_MILITARY"),
    FIELD_TECHNICIAN_VEHICLE("FIELD_TECHNICIAN_VEHICLE"),
    GENERAL("GENERAL"),
    SCHOOL_CIVILIAN("SCHOOL_CIVILIAN"),
    SCHOOL_INTELLIGENCE("SCHOOL_INTELLIGENCE"),
    SCHOOL_MILITARY("SCHOOL_MILITARY"),
    SCHOOL_OFFICER_CANDIDATE("SCHOOL_OFFICER_CANDIDATE"),
    SCHOOL_POLICE("SCHOOL_POLICE");

    private static final MMLogger LOGGER = MMLogger.create(LifePathCategory.class);

    private final String lookupName;

    /**
     * Constructs a {@link LifePathCategory}.
     *
     * @param lookupName the string used for lookups, serialization, and parsing
     *
     * @author Illiani
     * @since 0.50.07
     */
    LifePathCategory(String lookupName) {
        this.lookupName = lookupName;
    }

    /**
     * Returns the lookup name associated with this life path category.
     *
     * @return the lookup name for this category
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * Gets the {@link LifePathCategory} associated with a lookup name (case-insensitive).
     *
     * @param lookup the name to match
     *
     * @return the matching {@link LifePathCategory}, or {@code null} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable LifePathCategory fromLookupName(String lookup) {
        if (lookup == null) {
            LOGGER.warn("Null lookup passed to LifePathCategory#fromLookupName");
            return null;
        }

        for (LifePathCategory category : values()) {
            if (category.lookupName.equalsIgnoreCase(lookup)) {
                return category;
            }
        }

        LOGGER.warn("Unknown lookup name: {}", lookup);
        return null;
    }
}
