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

import jakarta.annotation.Nonnull;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;

/**
 * Plain data holder for the Attributes &amp; Traits page. Its fields live partly on {@link CampaignOptions} and partly
 * on {@link RandomSkillPreferences}: the constructor copies every value out of those sources and {@link #applyTo} writes
 * every value back. These <i>A Time of War</i> character-generation and skill rules previously lived on the Systems
 * page; they were relocated to the Advancement section (see issue #9560).
 */
class AttributesAndTraitsOptionsModel {
    boolean useAttributes;
    boolean randomizeAttributes;
    boolean displayAllAttributes;
    boolean useAgeEffects;
    boolean randomizeTraits;
    boolean useSmallArmsOnly;

    AttributesAndTraitsOptionsModel(@Nonnull CampaignOptions options, @Nonnull RandomSkillPreferences skillPreferences) {
        useAttributes = skillPreferences.isUseAttributes();
        randomizeAttributes = skillPreferences.isRandomizeAttributes();
        displayAllAttributes = options.isDisplayAllAttributes();
        useAgeEffects = options.isUseAgeEffects();
        randomizeTraits = skillPreferences.isRandomizeTraits();
        useSmallArmsOnly = options.isUseSmallArmsOnly();
    }

    void applyTo(@Nonnull CampaignOptions options, @Nonnull RandomSkillPreferences skillPreferences) {
        skillPreferences.setUseAttributes(useAttributes);
        skillPreferences.setRandomizeAttributes(randomizeAttributes);
        options.setDisplayAllAttributes(displayAllAttributes);
        options.setUseAgeEffects(useAgeEffects);
        skillPreferences.setRandomizeTraits(randomizeTraits);
        options.setUseSmallArmsOnly(useSmallArmsOnly);
    }
}
