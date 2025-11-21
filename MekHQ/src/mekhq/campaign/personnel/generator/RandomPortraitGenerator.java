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

import megamek.common.compute.Compute;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;

public class RandomPortraitGenerator {
    private static final MMLogger LOGGER = MMLogger.create(RandomPortraitGenerator.class);

    private RandomPortraitGenerator() {

    }

    /**
     * Generates a unique {@link Portrait} for the specified {@link Person}, using a priority-based directory search and
     * optional duplicate filtering.
     *
     * <p>The portrait generation process searches for viable portraits within a gendered directory structure and
     * progressively relaxes the search criteria until a suitable match is found. Portrait paths are constructed from
     * the directory category and the portrait filename (formatted as {@code "category:filename"}).</p>
     *
     * <p>The method enforces uniqueness unless duplicate portraits are explicitly allowed. When duplicates are not
     * permitted, portrait identifiers of all existing personnel are collected and excluded from the pool of possible
     * results.</p>
     *
     * <p>Portrait search order:</p>
     * <ol>
     *     <li>{@code /<gender>/<primary role>} (e.g., {@code /Male/MechWarrior}, {@code /Female/Civilian})</li>
     *     <li>{@code /<gender>/<role group>} (e.g., {@code /Male/Tech}, {@code /Female/Officer})</li>
     *     <li>{@code /<gender>/Combat} or {@code /<gender>/Support}</li>
     *     <li>{@code /<gender>} (fallback: any portrait for the correct gender)</li>
     * </ol>
     *
     * <p>The first directory in this sequence containing available portraits is used. If a random portrait is
     * successfully selected, a corresponding {@link Portrait} object is returned. If no valid portraits can be
     * located, a default empty {@link Portrait} is returned and a warning is logged.</p>
     *
     * @param personnel               all personnel in the campaign; used to determine already-assigned portraits when
     *                                duplicates are not allowed
     * @param person                  the {@link Person} for whom a portrait should be generated
     * @param allowDuplicatePortraits if {@code true}, portraits already used by others may be reused; otherwise they
     *                                are excluded from the pool
     * @param genderedPortraitsOnly   if {@code true}, restricts portrait search to gender-only directories, skipping
     *                                role-based portrait categories
     *
     * @return the generated {@link Portrait}, or a default fallback portrait if none could be assigned
     */
    public static Portrait generate(Collection<Person> personnel, Person person, boolean allowDuplicatePortraits,
          boolean genderedPortraitsOnly) {
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

        List<String> possiblePortraits = new ArrayList<>();
        File searchFile;
        if (!genderedPortraitsOnly) {
            PersonnelRole primaryRole = person.getPrimaryRole();
            String primaryRoleLabel;
            if (primaryRole.isSubType(PersonnelRoleSubType.CIVILIAN)) {
                primaryRoleLabel = "Civilian";
            } else {
                primaryRoleLabel = primaryRole.getLabel(person.isClanPersonnel());
            }
            searchFile = new File(genderFile, primaryRoleLabel);

            possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);

            if (possiblePortraits.isEmpty()) {
                String searchCat_RoleGroup = getCatRoleGroup(person);

                // This is a fallback that doesn't match the current portrait directories. It's likely left here for
                // legacy reasons - Illiani, Nov 21st 2025
                if (!searchCat_RoleGroup.isBlank()) {
                    searchFile = new File(genderFile, searchCat_RoleGroup);
                    possiblePortraits = getPossibleRandomPortraits(existingPortraits, searchFile);
                }
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
                LOGGER.error("Failed to generate portrait for {}. {} does not split into an array of length 2.",
                      person.getFullTitle(),
                      chosenPortrait);
            }
        } else {
            LOGGER.warn("Failed to generate portrait for {}. No possible portraits found.", person.getFullTitle());
        }

        return new Portrait();
    }

    private static String getCatRoleGroup(Person person) {
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
        return searchCat_RoleGroup;
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
    private static List<String> getPossibleRandomPortraits(
          final Set<String> existingPortraits,
          final File subdirectory) {

        if (MHQStaticDirectoryManager.getPortraits() == null) {
            return new ArrayList<>();
        }

        final List<String> possiblePortraits = new ArrayList<>();

        final String basePath = subdirectory.getAbsolutePath();

        for (final String category : MHQStaticDirectoryManager.getPortraits().getNonEmptyCategoryPaths()) {
            final File categoryFile = new File(category);
            final String categoryPath = categoryFile.getAbsolutePath();

            // Accept the root directory OR any directory under it
            final boolean isSameDir = categoryPath.equals(basePath);
            final boolean isSubDir = categoryPath.startsWith(basePath + File.separator);

            if (!isSameDir && !isSubDir) {
                continue;
            }

            final Iterator<String> names =
                  MHQStaticDirectoryManager.getPortraits().getItemNames(category);

            while (names.hasNext()) {
                final String location = category + ':' + names.next();

                if (!existingPortraits.contains(location)) {
                    possiblePortraits.add(location);
                }
            }
        }
        return possiblePortraits;
    }
}
