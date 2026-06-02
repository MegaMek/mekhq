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
package mekhq.gui.campaignOptions.contents;

import java.util.Arrays;

import mekhq.campaign.personnel.skills.SkillType;

/**
 * A mutable, package-private data holder for one skill's editable values (target number, per-level XP costs, and the
 * six experience milestones). Fields are intentionally exposed directly and accessed by the other Skills tab classes
 * ({@code SkillsTab}, {@code SkillsTableModel}, {@code SkillAdvancedEditorDialog}); this is a plain DTO, not an
 * encapsulated domain object, so it stays trivially copyable for the table's Copy/Paste feature.
 */
class SkillConfiguration {
    int targetNumber;
    Integer[] costs;
    int greenLevel;
    int regularLevel;
    int veteranLevel;
    int eliteLevel;
    int heroicLevel;
    int legendaryLevel;

    SkillConfiguration(SkillType skillType) {
        targetNumber = skillType.getTarget();
        costs = Arrays.copyOf(skillType.getCosts(), skillType.getCosts().length);
        greenLevel = skillType.getGreenLevel();
        regularLevel = skillType.getRegularLevel();
        veteranLevel = skillType.getVeteranLevel();
        eliteLevel = skillType.getEliteLevel();
        heroicLevel = skillType.getHeroicLevel();
        legendaryLevel = skillType.getLegendaryLevel();
    }

    SkillConfiguration(SkillConfiguration other) {
        copyFrom(other);
    }

    /**
     * Copies every configurable value from {@code other} into this configuration, leaving {@code other} untouched.
     *
     * @param other the configuration to copy values from
     */
    void copyFrom(SkillConfiguration other) {
        targetNumber = other.targetNumber;
        costs = Arrays.copyOf(other.costs, other.costs.length);
        greenLevel = other.greenLevel;
        regularLevel = other.regularLevel;
        veteranLevel = other.veteranLevel;
        eliteLevel = other.eliteLevel;
        heroicLevel = other.heroicLevel;
        legendaryLevel = other.legendaryLevel;
    }

    void applyTo(SkillType skillType) {
        skillType.setTarget(targetNumber);
        Integer[] targetCosts = skillType.getCosts();
        for (int i = 0; i < Math.min(costs.length, targetCosts.length); i++) {
            targetCosts[i] = costs[i];
        }
        skillType.setGreenLevel(greenLevel);
        skillType.setRegularLevel(regularLevel);
        skillType.setVeteranLevel(veteranLevel);
        skillType.setEliteLevel(eliteLevel);
        skillType.setHeroicLevel(heroicLevel);
        skillType.setLegendaryLevel(legendaryLevel);
    }
}