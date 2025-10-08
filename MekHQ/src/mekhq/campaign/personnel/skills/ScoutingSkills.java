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
package mekhq.campaign.personnel.skills;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;

public class ScoutingSkills {
    /**
     * Unmodifiable list of all skill type strings considered to be scouting skills.
     */
    public static final List<String> SCOUTING_SKILLS = List.of(SkillType.S_COMMUNICATIONS,
          SkillType.S_PERCEPTION, SkillType.S_SENSOR_OPERATIONS, SkillType.S_STEALTH, SkillType.S_TRACKING);

    public static @Nullable String getBestScoutingSkill(Person person) {
        String bestSkill = null;
        int highestLevel = -1;

        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();
        for (String skillName : SCOUTING_SKILLS) {
            if (person.hasSkill(skillName)) {
                int skillLevel = person.getSkill(skillName).getTotalSkillLevel(options, attributes);

                if (skillLevel > highestLevel) {
                    highestLevel = skillLevel;
                    bestSkill = skillName;
                }
            }
        }

        return bestSkill;
    }
}
