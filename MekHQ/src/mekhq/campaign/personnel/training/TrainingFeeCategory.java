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
package mekhq.campaign.personnel.training;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import mekhq.campaign.personnel.skills.SkillTrainingCategory;

/**
 * What a training fee pays for, so the Training Console can show the player where their C-bills go. Skill fees follow
 * {@link SkillTrainingCategory}, each scaled by its own campaign option; ability fees are their own category.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum TrainingFeeCategory {
    PROFESSION,
    UTILITY,
    ROLEPLAY,
    OTHER,
    ABILITIES;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";

    /**
     * @param category a skill training category
     *
     * @return the matching fee category
     */
    public static TrainingFeeCategory of(SkillTrainingCategory category) {
        return switch (category) {
            case PROFESSION -> PROFESSION;
            case UTILITY -> UTILITY;
            case ROLEPLAY -> ROLEPLAY;
        };
    }

    /** @return the category's name for the player */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, "TrainingFeeCategory." + name());
    }
}
