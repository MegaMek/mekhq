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
package mekhq.campaign.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * What a check tests: one skill, or one or two attributes.
 *
 * @param skillName the skill's name, or {@code null} for an attribute check
 * @param first     the first attribute, or {@code null} for a skill check
 * @param second    the second attribute, or {@code null} for a check on one attribute
 */
public record CheckTrait(@Nullable String skillName, @Nullable SkillAttribute first,
      @Nullable SkillAttribute second) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /**
     * @param skillName the skill's name
     *
     * @return a skill check
     */
    public static CheckTrait skill(final String skillName) {
        return new CheckTrait(skillName, null, null);
    }

    /**
     * @param first  the first attribute
     * @param second the second attribute, or {@code null} or {@link SkillAttribute#NO_ATTRIBUTE} for one attribute
     *
     * @return an attribute check
     */
    public static CheckTrait attributes(final SkillAttribute first, final @Nullable SkillAttribute second) {
        return new CheckTrait(null, first, (second == null || second.isNoAttribute()) ? null : second);
    }

    public boolean isSkill() {
        return skillName != null;
    }

    /**
     * @return the name players see, such as "Negotiation" or "Body-Willpower"
     */
    public String getLabel() {
        if (skillName != null) {
            return skillName.replace(" (RP Only)", "");
        }
        if (first == null) {
            return getTextAt(RESOURCE_BUNDLE, "OracleLog.check.nothing");
        }
        return (second == null) ? first.getLabel() : first.getLabel() + "-" + second.getLabel();
    }
}
