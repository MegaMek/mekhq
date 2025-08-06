/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import megamek.common.Compute;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;

public class RandomPortraitGenerator {
    private static final MMLogger logger = MMLogger.create(RandomPortraitGenerator.class);

    private RandomPortraitGenerator() {

    }

    /**
     * This generates a unique Portrait based on the supplied {@link Person}
     *
     * @param personnel a list of all personnel, from which existing portraits are determined
     * @param person    the {@link Person} to generate a unique portrait for
     *
     * @return the generated portrait
     */
    public static Portrait generate(Collection<Person> personnel, Person person, Boolean allowDuplicatePortraits) {
        // first create a list of existing portrait strings, so we can check for
        // duplicates - unless they are allowed in campaign options
        Set<String> existingPortraits = new HashSet<>();
        if (!allowDuplicatePortraits) {
            for (Person existingPerson : personnel) {
                existingPortraits.add(existingPerson.getPortrait().getCategory() +
                                            ':' +
                                            existingPerson.getPortrait().getFilename());
            }
        }

        // Will search for portraits in the /gender/primaryrole folder first,
        // and if none are found then /gender/rolegroup, then /gender/combat or
        // /gender/support, then in /gender.
        File genderFile = new File(person.getGender().isFemale() ? "Female" : "Male");

        PersonnelRole primaryRole = person.getPrimaryRole();
        String primaryRoleLabel = primaryRole.getLabel(person.isClanPersonnel());
        if (primaryRole.isSubType(PersonnelRoleSubType.CIVILIAN)) {
            primaryRoleLabel = "Civilian";
        } else {
            primaryRoleLabel = primaryRole.getLabel(person.isClanPersonnel());
        }
        File searchFile = new File(genderFile, primaryRoleLabel);

        List<String> possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);

        if (possiblePortraits.isEmpty()) {
            String searchCat_RoleGroup = "";
            if (person.getPrimaryRole().isAdministrator()) {
                searchCat_RoleGroup = "Admin";
            } else if (person.getPrimaryRole().isVesselCrew()) {
                searchCat_RoleGroup = "Vessel Crew";
            } else if (person.getPrimaryRole().isVehicleCrewMember()) {
                searchCat_RoleGroup = "Vehicle Crew";
            } else if (person.getPrimaryRole().isTech()) {
                searchCat_RoleGroup = "Tech";
            } else if (person.getPrimaryRole().isMedicalStaff()) {
                searchCat_RoleGroup = "Medical";
            }

            if (!searchCat_RoleGroup.isBlank()) {
                searchFile = new File(genderFile, searchCat_RoleGroup);
                possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);
            }
        }

        if (possiblePortraits.isEmpty()) {
            searchFile = new File(genderFile, person.getPrimaryRole().isCombat() ? "Combat" : "Support");
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
                logger.error("Failed to generate portrait for " + person.getFullTitle() +
                                   ". " +
                                   chosenPortrait +
                                   " does not split into an array of length 2.");
            }
        } else {
            logger.warn("Failed to generate portrait for " + person.getFullTitle() + ". No possible portraits found.");
        }

        return new Portrait();
    }

    /**
     * This is a helper method that determines what possible unassigned portraits can be generated based on the supplied
     * subdirectory
     *
     * @param existingPortraits the list of existing portraits that have already been assigned
     * @param subdirectory      the subdirectory to search
     *
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
