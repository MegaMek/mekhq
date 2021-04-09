/*
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
package mekhq.campaign.personnel.generator;

import megamek.common.Compute;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RandomPortraitGenerator {
    private RandomPortraitGenerator() {

    }

    /**
     * This generates a unique Portrait based on the supplied {@link Person}
     *
     * @param personnel a list of all personnel, from which existing portraits are determined
     * @param p the {@link Person} to generate a unique portrait for
     * @return the generated portrait
     */
    public static AbstractIcon generate(Collection<Person> personnel, Person p) {
        // first create a list of existing portrait strings, so we can check for
        // duplicates
        Set<String> existingPortraits = new HashSet<>();
        for (Person existingPerson : personnel) {
            existingPortraits.add(existingPerson.getPortraitCategory() + ":"
                    + existingPerson.getPortraitFileName());
        }

        List<String> possiblePortraits;

        // Will search for portraits in the /gender/primaryrole folder first,
        // and if none are found then /gender/rolegroup, then /gender/combat or
        // /gender/support, then in /gender.
        File genderFile = new File(p.getGender().isFemale() ? "Female" : "Male");
        File searchFile = new File(genderFile, Person.getRoleDesc(p.getPrimaryRole(), p.isClanner()));

        possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);

        if (possiblePortraits.isEmpty()) {
            String searchCat_RoleGroup = "";
            if (p.isAdminPrimary()) {
                searchCat_RoleGroup = "Admin";
            } else if (p.getPrimaryRole() == Person.T_SPACE_CREW
                    || p.getPrimaryRole() == Person.T_SPACE_GUNNER
                    || p.getPrimaryRole() == Person.T_SPACE_PILOT
                    || p.getPrimaryRole() == Person.T_NAVIGATOR) {
                searchCat_RoleGroup = "Vessel Crew";
            } else if (p.isTechPrimary()) {
                searchCat_RoleGroup = "Tech";
            } else if (p.getPrimaryRole() == Person.T_MEDIC
                    || p.getPrimaryRole() == Person.T_DOCTOR) {
                searchCat_RoleGroup = "Medical";
            }

            if (!searchCat_RoleGroup.isBlank()) {
                searchFile = new File(genderFile, searchCat_RoleGroup);
                possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);
            }
        }

        if (possiblePortraits.isEmpty()) {
            searchFile = new File(genderFile, p.hasPrimaryCombatRole() ? "Combat" : "Support");
            possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);
        }

        if (possiblePortraits.isEmpty()) {
            possiblePortraits = getPossibleRandomPortraits(existingPortraits, genderFile);
        }

        if (!possiblePortraits.isEmpty()) {
            String chosenPortrait = possiblePortraits.get(Compute.randomInt(possiblePortraits.size()));
            String[] temp = chosenPortrait.split(":");
            if (temp.length == 2) {
                return new Portrait(temp[0], temp[1]);
            } else {
                MekHQ.getLogger().error("Failed to generate portrait for " + p.getFullTitle() + ". "
                        + chosenPortrait + " does not split into an array of length 2.");
            }
        } else {
            MekHQ.getLogger().warning("Failed to generate portrait for " + p.getFullTitle()
                    + ". No possible portraits found.");
        }

        return new Portrait();
    }

    /**
     * This is a helper method that determines what possible unassigned portraits can be generated
     * based on the supplied subdirectory
     *
     * @param existingPortraits the list of existing portraits that have already been assigned
     * @param subDir the subdirectory to search
     * @return a list of all possible unassigned random portraits
     */
    private static List<String> getPossibleRandomPortraits(Set<String> existingPortraits, File subDir) {
        List<String> possiblePortraits = new ArrayList<>();
        Iterator<String> categories = MHQStaticDirectoryManager.getPortraits().getCategoryNames();
        while (categories.hasNext()) {
            String category = categories.next();
            if (new File(category).compareTo(subDir) == 0) {
                Iterator<String> names = MHQStaticDirectoryManager.getPortraits().getItemNames(category);
                while (names.hasNext()) {
                    String name = names.next();
                    String location = category + ":" + name;
                    if (existingPortraits.contains(location)) {
                        continue;
                    }
                    possiblePortraits.add(location);
                }
            }
        }
        return possiblePortraits;
    }
}
