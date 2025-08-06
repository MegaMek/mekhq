/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;

/**
 * Tracks skills for a {@link Person}.
 */
public class Skills {
    public static final SkillLevel[] SKILL_LEVELS = SkillLevel.values();
    private final Map<String, Skill> skills = new HashMap<>();

    /**
     * Gets the number of skills.
     *
     * @return The number of skills.
     */
    public int size() {
        return skills.size();
    }

    /**
     * Removes all of the skills.
     */
    public void clear() {
        skills.clear();
    }

    /**
     * Gets a value indicating if a certain skill is possessed.
     *
     * @param name The name of the skill.
     *
     * @return True if and only if the skill is active.
     */
    public boolean hasSkill(final @Nullable String name) {
        return skills.containsKey(name);
    }

    /**
     * Gets a {@link Skill} by name.
     *
     * @param name The name of the skill.
     *
     * @return The {@link Skill}, if one exists, otherwise null.
     */
    @Nullable
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * Adds a skill.
     *
     * @param name  The name of the skill.
     * @param skill The {@link Skill} to track.
     */
    public void addSkill(String name, Skill skill) {
        skills.put(name, skill);
    }

    /**
     * Removes a skill.
     *
     * @param name The name of the skill to remove.
     *
     * @return True if the skill was removed, otherwise false.
     */
    public boolean removeSkill(String name) {
        return skills.remove(name) != null;
    }

    /**
     * Gets a collection of skill names.
     *
     * @return A collection of skill names.
     */
    public Collection<String> getSkillNames() {
        return skills.keySet();
    }

    /**
     * Gets a collection of {@link Skill} objects.
     *
     * @return A collection of {@link Skill} objects.
     */
    public Collection<Skill> getSkills() {
        return skills.values();
    }
}
