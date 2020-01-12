/*
 * Copyright (C) 2019 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;

/**
 * Tracks skills for a {@link Person}.
 */
public class Skills {
    private final Map<String, Skill> skills = new HashMap<>();

    /**
     * Gets the number of skills.
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
     * @param name The name of the skill.
     * @return True if and only if the skill is active.
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    /**
     * Gets a {@link Skill} by name.
     * @param name The name of the skill.
     * @return The {@link Skill}, if one exists, otherwise null.
     */
    @Nullable
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * Adds a skill.
     * @param name The name of the skill.
     * @param skill The {@link Skill} to track.
     */
    public void addSkill(String name, Skill skill) {
        skills.put(name, skill);
    }

    /**
     * Removes a skill.
     * @param name The name of the skill to remove.
     * @return True if the skill was removed, otherwise false.
     */
    public boolean removeSkill(String name) {
        return skills.remove(name) != null;
    }

    /**
     * Gets a collection of skill names.
     * @return A collection of skill names.
     */
	public Collection<String> getSkillNames() {
		return skills.keySet();
    }

    /**
     * Gets a collection of {@link Skill} objects.
     * @return A collection of {@link Skill} objects.
     */
    public Collection<Skill> getSkills() {
        return skills.values();
    }
}
