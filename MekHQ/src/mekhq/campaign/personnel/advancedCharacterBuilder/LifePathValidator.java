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
import java.util.Set;

public class LifePathValidator {
    private final LifePath lifePath;
    private final Set<InvalidLifePathReason> invalidReasons = new HashSet<>();

    public Set<InvalidLifePathReason> getInvalidReasons() {
        return invalidReasons;
    }

    public LifePathValidator(LifePath lifePath) {
        this.lifePath = lifePath;

        // Tests
        checkFlexiblePicks();
        checkAffiliationFactionRequirement();
    }

    private void checkFlexiblePicks() {
        int flexiblePicks = lifePath.flexibleXPPickCount();

        // Find the largest group size among all flexible XP award groups
        int maxGroupSize = Math.max(
              Math.max(lifePath.flexibleXPAbilities().size(), lifePath.flexibleXPAttributes().size()),
              Math.max(lifePath.flexibleXPSkills().size(), lifePath.flexibleXPTraits().size())
        );

        // If groups are present, but no picks are selected, then the Life Path is invalid
        if (flexiblePicks <= 0) {
            if (maxGroupSize > 0) {
                invalidReasons.add(NO_FLEXIBLE_PICKS);
            }
        } else {
            // If there are flexible picks, but the number of picks exceeds the maximum group size, then the Life
            // Path is invalid
            if (flexiblePicks > maxGroupSize) {
                invalidReasons.add(TOO_MANY_FLEXIBLE_PICKS);
            }
        }
    }

    private void checkAffiliationFactionRequirement() {
        if (lifePath.lifeStages().contains(ATOWLifeStage.AFFILIATION)) {
            boolean hasEmptyGroup = true;
            for (List<String> group : lifePath.requirementsFactions().values()) {
                if (!group.isEmpty()) {
                    hasEmptyGroup = false;
                    break;
                }
            }

            if (!hasEmptyGroup) {
                return;
            }

            // If the Life Path has an affiliation stage, but no factions are selected, then the Life Path is invalid
            invalidReasons.add(MISSING_FACTION);
        }
    }
}
