/*
 * PersonnelFinder.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel;

import java.util.Collection;

/**
 * Finds personnel matching given criteria.
 */
public class PersonnelFinder {

    /**
     * Finds the active person in a particular role with the highest level in a
     * given, with an optional secondary skill to break ties.
     *
     * @param personnel The collection of Persons to search.
     * @param role One of the Person.T_* constants
     * @param primary The skill to use for comparison.
     * @param secondary If not null and there is more than one person tied for the most
     *                 the highest, preference will be given to the one with a higher
     *                 level in the secondary skill.
     * @return The admin in the designated role with the most experience.
     */
    public Person findBestInRole(Collection<Person> personnel, int role, String primary, String secondary) {
        int highest = 0;
        Person retVal = null;
        for (Person p : personnel) {
            if ((p.getPrimaryRole() == role || p.getSecondaryRole() == role) && p.getSkill(primary) != null) {
                if (p.getSkill(primary).getLevel() > highest) {
                    retVal = p;
                    highest = p.getSkill(primary).getLevel();
                } else if (secondary != null && p.getSkill(primary).getLevel() == highest &&
                /*
                 * If the skill level of the current person is the same as the previous highest,
                 * select the current instead under the following conditions:
                 */
                        (retVal == null || // None has been selected yet (current has level 0)
                                retVal.getSkill(secondary) == null || // Previous selection does not have secondary
                                                                      // skill
                                (p.getSkill(secondary) != null // Current has secondary skill and it is higher than the
                                                               // previous.
                                        && p.getSkill(secondary).getLevel() > retVal.getSkill(secondary).getLevel()))) {
                    retVal = p;
                }
            }
        }
        return retVal;
    }

    public Person findBestInRole(Collection<Person> personnel, int role, String skill) {
        return findBestInRole(personnel, role, skill, null);
    }
}
