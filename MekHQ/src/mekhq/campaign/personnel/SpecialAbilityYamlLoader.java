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
package mekhq.campaign.personnel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Reads the default special abilities from their YAML data file ({@code data/universe/defaultspa.yml}).
 *
 * <p>Abilities stored in campaign saves and campaign presets are still written and read as XML by
 * {@link SpecialAbility}; this loader only covers the default ability definitions.</p>
 *
 * @author Illiani
 * @since 0.51.02
 */
final class SpecialAbilityYamlLoader {
    private static final MMLogger LOGGER = MMLogger.create(SpecialAbilityYamlLoader.class);

    private SpecialAbilityYamlLoader() {
    }

    /**
     * The whole ability file.
     *
     * @param abilities every ability defined in the file, or {@code null} if the section is absent
     */
    private record AbilityFileData(@Nullable List<AbilityData> abilities) {
    }

    /**
     * One ability entry. Everything but the lookup name is optional.
     *
     * @param lookupName       the ability's internal name, matching its {@code PersonnelOptions} option
     * @param displayName      the name shown to players, or {@code null} to use the option's name
     * @param desc             the description shown to players, or {@code null} to use the option's description
     * @param xpCost           the XP cost, or {@code null} for 1
     * @param originOnly       whether the ability can only be gained during character creation, or {@code null} for
     *                         {@code false}
     * @param weight           the weight used when picking abilities at random, or {@code null} for 1
     * @param prereqAbilities  abilities a character must already have, or {@code null} for none
     * @param invalidAbilities abilities that prevent a character from taking this one, or {@code null} for none
     * @param removeAbilities  abilities removed when this one is gained, or {@code null} for none
     * @param choiceValues     the values a choice ability can be set to, or {@code null} for none
     * @param skillPrereqs     skill requirements, each mapping skill names to the minimum experience level name; a
     *                         character must meet any one skill within every requirement. {@code null} for none
     * @param miscPrereqs      miscellaneous requirements keyed by type, such as {@code clanperson}, or {@code null} for
     *                         none
     */
    record AbilityData(String lookupName, @Nullable String displayName, @Nullable String desc,
          @Nullable Integer xpCost, @Nullable Boolean originOnly, @Nullable Integer weight,
          @Nullable List<String> prereqAbilities, @Nullable List<String> invalidAbilities,
          @Nullable List<String> removeAbilities, @Nullable List<String> choiceValues,
          @Nullable List<Map<String, String>> skillPrereqs, @Nullable Map<String, String> miscPrereqs) {
    }

    /**
     * Reads every ability entry from the given file.
     *
     * @param file the ability YAML file
     *
     * @return the ability entries, in file order
     *
     * @throws IOException if the file cannot be read or parsed
     */
    static List<AbilityData> load(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AbilityFileData fileData = mapper.readValue(file, AbilityFileData.class);
        return ((fileData == null) || (fileData.abilities() == null)) ? List.of() : fileData.abilities();
    }

    /**
     * Converts an entry's skill requirements. Skills that do not exist are skipped, as are requirements left with no
     * skills.
     *
     * @param lookupName   the ability the requirements belong to, for logging
     * @param skillPrereqs the requirements as read from the file, or {@code null} for none
     *
     * @return the converted requirements
     */
    static Vector<SkillPrerequisite> toSkillPrerequisites(String lookupName,
          @Nullable List<Map<String, String>> skillPrereqs) {
        Vector<SkillPrerequisite> prerequisites = new Vector<>();
        if (skillPrereqs == null) {
            return prerequisites;
        }

        for (Map<String, String> skills : skillPrereqs) {
            if (skills == null) {
                continue;
            }

            SkillPrerequisite prerequisite = new SkillPrerequisite();
            for (Entry<String, String> skill : skills.entrySet()) {
                if (SkillType.getType(skill.getKey()) == null) {
                    LOGGER.warn("Skipping unknown skill {} required by special ability {}", skill.getKey(), lookupName);
                    continue;
                }
                prerequisite.addPrereq(skill.getKey(), parseSkillLevel(lookupName, skill.getValue()));
            }

            if (!prerequisite.isEmpty()) {
                prerequisites.add(prerequisite);
            }
        }
        return prerequisites;
    }

    /**
     * Converts an experience level name, such as {@code Veteran}, to its {@link SkillType} level.
     *
     * @param lookupName the ability the level belongs to, for logging
     * @param levelName  the level name, or {@code null} for any level
     *
     * @return the matching level, or {@link SkillType#EXP_ULTRA_GREEN} (any level) if the name is missing or unknown
     */
    private static int parseSkillLevel(String lookupName, @Nullable String levelName) {
        if ((levelName == null) || levelName.isBlank()) {
            return SkillType.EXP_ULTRA_GREEN;
        }

        for (int level = SkillType.EXP_ULTRA_GREEN; level <= SkillType.EXP_LEGENDARY; level++) {
            if (SkillType.getExperienceLevelName(level).equalsIgnoreCase(levelName.trim())) {
                return level;
            }
        }

        LOGGER.warn("Unknown skill level {} required by special ability {}; treating it as any level", levelName,
              lookupName);
        return SkillType.EXP_ULTRA_GREEN;
    }
}
