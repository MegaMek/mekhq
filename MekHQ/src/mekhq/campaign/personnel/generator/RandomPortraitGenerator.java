/*
 * Copyright (C) 2020-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.personnel.generator;

import megamek.common.Compute;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.personnel.Person;

import java.io.File;
import java.util.*;

public class RandomPortraitGenerator {
    private static final MMLogger logger = MMLogger.create(RandomPortraitGenerator.class);

    private RandomPortraitGenerator() {

    }

    /**
     * This generates a unique Portrait based on the supplied {@link Person}
     *
     * @param personnel a list of all personnel, from which existing portraits are
     *                  determined
     * @param p         the {@link Person} to generate a unique portrait for
     * @return the generated portrait
     */
    public static Portrait generate(Collection<Person> personnel, Person p,
            Boolean allowDuplicatePortraits) {
        // first create a list of existing portrait strings, so we can check for
        // duplicates - unless they are allowed in campaign options
        Set<String> existingPortraits = new HashSet<>();
        if (!allowDuplicatePortraits) {
            for (Person existingPerson : personnel) {
                existingPortraits.add(existingPerson.getPortrait().getCategory() + ':'
                    + existingPerson.getPortrait().getFilename());
            }
        }

        List<String> possiblePortraits;

        // Will search for portraits in the /gender/primaryrole folder first,
        // and if none are found then /gender/rolegroup, then /gender/combat or
        // /gender/support, then in /gender.
        File genderFile = new File(p.getGender().isFemale() ? "Female" : "Male");
        File searchFile = new File(genderFile, p.getPrimaryRole().getName(p.isClanPersonnel()));

        possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);

        if (possiblePortraits.isEmpty()) {
            String searchCat_RoleGroup = "";
            if (p.getPrimaryRole().isAdministrator()) {
                searchCat_RoleGroup = "Admin";
            } else if (p.getPrimaryRole().isVesselCrew()) {
                searchCat_RoleGroup = "Vessel Crew";
            } else if (p.getPrimaryRole().isVehicleCrewMember()) {
                searchCat_RoleGroup = "Vehicle Crew";
            } else if (p.getPrimaryRole().isTech()) {
                searchCat_RoleGroup = "Tech";
            } else if (p.getPrimaryRole().isMedicalStaff()) {
                searchCat_RoleGroup = "Medical";
            }

            if (!searchCat_RoleGroup.isBlank()) {
                searchFile = new File(genderFile, searchCat_RoleGroup);
                possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);
            }
        }

        if (possiblePortraits.isEmpty()) {
            searchFile = new File(genderFile, p.getPrimaryRole().isCombat() ? "Combat" : "Support");
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
                logger.error("Failed to generate portrait for " + p.getFullTitle() + ". "
                        + chosenPortrait + " does not split into an array of length 2.");
            }
        } else {
            logger.warn("Failed to generate portrait for " + p.getFullTitle()
                    + ". No possible portraits found.");
        }

        return new Portrait();
    }

    /**
     * This is a helper method that determines what possible unassigned portraits
     * can be generated
     * based on the supplied subdirectory
     *
     * @param existingPortraits the list of existing portraits that have already
     *                          been assigned
     * @param subdirectory      the subdirectory to search
     * @return a list of all possible unassigned random portraits
     */
    private static List<String> getPossibleRandomPortraits(final Set<String> existingPortraits,
            final File subdirectory) {
        if (MHQStaticDirectoryManager.getPortraits() == null) {
            return new ArrayList<>();
        }

        final List<String> possiblePortraits = new ArrayList<>();
        for (final String category : MHQStaticDirectoryManager.getPortraits().getNonEmptyCategoryPaths()) {
            if (new File(category).compareTo(subdirectory) != 0) {
                continue;
            }

            final Iterator<String> names = MHQStaticDirectoryManager.getPortraits().getItemNames(category);
            while (names.hasNext()) {
                final String location = category + ':' + names.next();
                if (existingPortraits.contains(location)) {
                    continue;
                }
                possiblePortraits.add(location);
            }
        }
        return possiblePortraits;
    }
}
