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

import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_FACTION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.NO_FLEXIBLE_PICKS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.TOO_MANY_FLEXIBLE_PICKS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.codeUtilities.StringUtility;

public class LifePathValidator {
    private final int flexiblePicks;
    private final int maximumGroupSize;
    private final List<ATOWLifeStage> lifeStages;
    private final Map<Integer, List<String>> requirementsFactions;
    private final String source;
    private final String name;
    private final List<LifePathCategory> categories;

    private final Set<InvalidLifePathReason> invalidReasons = new HashSet<>();

    public Set<InvalidLifePathReason> getInvalidReasons() {
        return invalidReasons;
    }

    public LifePathValidator(int flexiblePicks, int maximumGroupSize, List<ATOWLifeStage> lifeStages,
          Map<Integer, List<String>> requirementsFactions, String source, String name,
          List<LifePathCategory> categories) {
        this.flexiblePicks = flexiblePicks;
        this.maximumGroupSize = maximumGroupSize;
        this.lifeStages = lifeStages;
        this.requirementsFactions = requirementsFactions;
        this.source = source;
        this.name = name;
        this.categories = categories;

        // Tests
        checkFlexiblePicks();
        checkAffiliationFactionRequirement();
        checkSource();
        checkName();
        checkLifeStages();
        checkCategories();
    }

    public static int getMaximumGroupSize(int flexibleAbilitiesCount, int flexibleAttributesCount,
          int flexibleSkillsCount, int flexibleMetaSkillsCount, int flexibleTraitsCount) {
        return Math.max(flexibleAbilitiesCount,
              Math.max(flexibleAttributesCount,
                    Math.max(flexibleSkillsCount,
                          Math.max(flexibleMetaSkillsCount, flexibleTraitsCount)
                    )
              )
        );
    }

    private void checkFlexiblePicks() {
        // Find the largest group size among all flexible XP award groups


        // If groups are present, but no picks are selected, then the Life Path is invalid
        if (flexiblePicks <= 0) {
            if (maximumGroupSize > 0) {
                invalidReasons.add(NO_FLEXIBLE_PICKS);
            }
        } else {
            // If there are flexible picks, but the number of picks exceeds the maximum group size, then the Life
            // Path is invalid
            if (flexiblePicks > maximumGroupSize) {
                invalidReasons.add(TOO_MANY_FLEXIBLE_PICKS);
            }
        }
    }

    private void checkAffiliationFactionRequirement() {
        // If the Life Path has an affiliation stage, but no factions are selected, then the Life Path is invalid
        if (lifeStages.contains(ATOWLifeStage.AFFILIATION) || lifeStages.contains(ATOWLifeStage.SUB_AFFILIATION)) {
            for (List<String> group : requirementsFactions.values()) {
                if (group.isEmpty()) {
                    invalidReasons.add(MISSING_FACTION);
                    return;
                }
            }
        }
    }

    private void checkSource() {
        if (StringUtility.isNullOrBlank(source)) {
            invalidReasons.add(InvalidLifePathReason.MISSING_SOURCE);
        }
    }

    private void checkName() {
        if (StringUtility.isNullOrBlank(name)) {
            invalidReasons.add(InvalidLifePathReason.MISSING_NAME);
        }
    }

    private void checkLifeStages() {
        if (lifeStages.isEmpty()) {
            invalidReasons.add(InvalidLifePathReason.MISSING_LIFE_STAGE);
        }
    }

    private void checkCategories() {
        if (categories.isEmpty()) {
            invalidReasons.add(InvalidLifePathReason.MISSING_CATEGORIES);
        }
    }
}
