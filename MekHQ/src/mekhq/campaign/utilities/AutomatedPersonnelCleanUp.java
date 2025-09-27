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
package mekhq.campaign.utilities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;

/**
 * Provides automated evaluation and selection of personnel records that are eligible for cleanup. This helps reduce the
 * effect of campaign files growing over time due to a large number of (no longer relevant) personnel records.
 *
 * <p>This class examines a list of {@link Person} objects on a specified date and determines, based on configuration
 * flags and each person's status and genealogy, which personnel records can be safely cleaned up.</p>
 *
 * <p>The configuration flags allow for exemptions, such as retaining information for retirees or deceased personnel
 * who are preserved for historical reasons.</p>
 */
public class AutomatedPersonnelCleanUp {
    private final LocalDate today;
    private final boolean isUseRemovalExemptRetirees;
    private final boolean isUseRemovalExemptCemetery;
    private final Collection<Person> personnelToProcess;
    private final List<Person> personnelToCleanUp = new ArrayList<>();

    /**
     * Constructs an automated personnel cleanup process.
     *
     * @param today                      the current date used as a reference for evaluating time-based conditions
     * @param personnelToProcess         the list of personnel to be examined for potential cleanup
     * @param isUseRemovalExemptRetirees whether retired personnel should be exempt from removal
     * @param isUseRemovalExemptCemetery whether deceased personnel should be exempt from removal
     */
    public AutomatedPersonnelCleanUp(LocalDate today, Collection<Person> personnelToProcess,
          boolean isUseRemovalExemptRetirees, boolean isUseRemovalExemptCemetery) {
        this.today = today;
        this.personnelToProcess = personnelToProcess;
        this.isUseRemovalExemptRetirees = isUseRemovalExemptRetirees;
        this.isUseRemovalExemptCemetery = isUseRemovalExemptCemetery;

        processPersonnel();
    }

    /**
     * Returns the list of personnel records identified as eligible for cleanup based on the evaluation logic.
     *
     * @return the list of {@link Person} objects to be cleaned up
     */
    public List<Person> getPersonnelToCleanUp() {
        return personnelToCleanUp;
    }

    /**
     * Processes all personnel records provided, applying evaluation logic to determine which individuals are eligible
     * for removal.
     *
     * <p>This method is invoked during object construction.</p>
     */
    private void processPersonnel() {
        for (Person person : personnelToProcess) {
            PersonnelStatus status = person.getStatus();

            if (status.isDepartedUnit()) {
                if (shouldRemovePerson(person)) {
                    personnelToCleanUp.add(person);
                }
            }
        }
    }

    /**
     * Determines whether a specific {@link Person} should be removed from the application's personnel records.
     *
     * <p>This decision is based on several rules:</p>
     * <ul>
     *   <li>If retirees or deceased are exempt (as per configuration), they will <b>not</b> be removed.</li>
     *   <li>If the person has any active genealogy, they will <b>not</b> be removed.</li>
     *   <li>If their retirement or death date was more than one month ago, they <b>will</b> be removed.</li>
     * </ul>
     *
     * @param person the individual to be evaluated
     *
     * @return {@code true} if the personnel record should be removed; {@code false} otherwise
     */
    private boolean shouldRemovePerson(Person person) {
        // We do these checks first, as they're less expensive than parsing the entire genealogy
        PersonnelStatus status = person.getStatus();

        if (status.isRetired() && isUseRemovalExemptRetirees) {
            return false;
        }

        if (status.isDead() && isUseRemovalExemptCemetery) {
            return false;
        }

        // Do not remove if the character has an active genealogy
        Genealogy genealogy = person.getGenealogy();

        if (genealogy.isActive()) {
            return false;
        }

        // Did the departure occur more than a month ago?
        LocalDate aMonthAgo = today.minusMonths(1);
        LocalDate retirementDate = person.getRetirement();
        if (retirementDate != null && retirementDate.isBefore(aMonthAgo)) {
            return true;
        }

        LocalDate deathDate = person.getDateOfDeath();
        return deathDate != null && deathDate.isBefore(aMonthAgo);
    }
}
