/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.randomProcreation;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

import java.time.LocalDate;

public abstract class AbstractProcreation {
    //region Variable Declarations
    private final RandomProcreationMethod method;
    private final boolean enableRelationshiplessProcreation;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractProcreation(final RandomProcreationMethod method,
                                  final boolean enableRelationshiplessProcreation) {
        this.method = method;
        this.enableRelationshiplessProcreation = enableRelationshiplessProcreation;
    }
    //endregion Constructors

    //region Getters
    public RandomProcreationMethod getMethod() {
        return method;
    }

    public boolean isEnableRelationshiplessProcreation() {
        return enableRelationshiplessProcreation;
    }
    //endregion Getters

    /**
     * Process new day procreation for an individual
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewDay(final Campaign campaign, final LocalDate today, final Person person) {
        // Instantly return for male personnel
        if (person.getGender().isMale()) {
            return;
        }

        // Check if they are already pregnant
        if (person.isPregnant()) {
            // They give birth if the due date is the current day
            if (today.isEqual(person.getDueDate())) {
                person.birth(campaign, today);
            }
            return;
        }

        // Make the required checks for
        if (procreates(today, person)) {
            person.addPregnancy(campaign, today);
        }
    }

    /**
     * Determines if a non-pregnant female person procreates on a given day
     *
     * @param today the current day
     * @param person the person in question
     * @return true if they do, otherwise false
     */
    protected boolean procreates(final LocalDate today, final Person person) {
        if (!person.canProcreate(today)) {
            return false;
        } else if (person.getGenealogy().hasSpouse()) {
            final Person spouse = person.getGenealogy().getSpouse();
            if (spouse.getGender().isMale() && !spouse.getStatus().isDeadOrMIA()
                    && !spouse.isDeployed() && !spouse.isChild(today)) {
                return partneredProcreation(person);
            } else {
                return false;
            }
        } else if (isEnableRelationshiplessProcreation()) {
            return partnerlessProcreation(person);
        } else {
            return false;
        }
    }

    /**
     * Determines if a person with an eligible partner procreates
     * @param person the person to determine for
     * @return true if they do, otherwise false
     */
    protected abstract boolean partneredProcreation(final Person person);

    /**
     * Determines if a person without a partner procreates
     * @param person the person to determine for
     * @return true if they do, otherwise false
     */
    protected abstract boolean partnerlessProcreation(final Person person);
}
