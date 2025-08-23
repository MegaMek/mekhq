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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public record LifePathComponentStorage(
      // Excluded from cost calculations
      int gameYear, List<String> factions,
      List<UUID> lifePaths,
      Map<LifePathCategory, Integer> categories,

      // Included in cost calculations
      Map<SkillAttribute, Integer> attributes,
      Map<LifePathEntryDataTraitLookup, Integer> traits,
      Map<String, Integer> skills,
      Map<String, Integer> abilities) {
    private static final MMLogger LOGGER = MMLogger.create(LifePathComponentStorage.class);

    @JsonIgnore
    public Map<SkillType, Integer> getSkillTypes() {
        Map<SkillType, Integer> skillTypes = new HashMap<>();

        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            SkillType type = SkillType.getType(entry.getKey());
            if (type == null) {
                LOGGER.warn("Unknown skill type: {}", entry.getKey());
                continue;
            }

            skillTypes.put(type, entry.getValue());
        }

        return skillTypes;
    }
}
