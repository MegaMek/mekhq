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
package mekhq.campaign.personnel;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.Gender.RANDOMIZE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.LEFT;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.FREE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.familyTree.Genealogy;

/**
 * The {@link RandomDependents} class manages the random addition and removal of dependent personnel based on the
 * current campaign state and options.
 *
 * <p>This class processes dependents, determining if some should be removed or new dependents added,
 * based on predefined campaign rules.</p>
 */
public class RandomDependents {
    private final Campaign campaign;
    private final boolean isUseRandomDependentAddition;
    private final boolean isUseRandomDependentRemoval;
    private final LocalDate currentDay;

    private final List<Person> activeDependents = new ArrayList<>();
    private final int activeNonDependents;
    private final int dependentCapacity;

    final static double DEPENDENT_CAPACITY_MULTIPLIER = 0.05;

    private final String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    /**
     * Constructs a new {@code RandomDependents} instance.
     *
     * <p>Initializes the dependent management process by calculating the current dependent capacity,
     * randomly removing dependents, and potentially adding new ones.</p>
     *
     * @param campaign The {@link Campaign} instance to which dependent operations are applied.
     */
    public RandomDependents(Campaign campaign) {
        this.campaign = campaign;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        this.isUseRandomDependentAddition = campaignOptions.isUseRandomDependentAddition();
        this.isUseRandomDependentRemoval = campaignOptions.isUseRandomDependentRemoval();
        currentDay = campaign.getLocalDate();

        // Prepare the data
        activeNonDependents = prepareData();
        Collections.shuffle(activeDependents);

        dependentCapacity = calculateDependentCapacity();
    }

    /**
     * Calculates the dependent capacity based on the number of active non-dependents and a predefined capacity
     * multiplier. The capacity is always at least 1.
     *
     * @return The calculated dependent capacity as an integer, ensuring a value of at least 1.
     */
    int calculateDependentCapacity() {
        return max(1, (int) round(activeNonDependents * DEPENDENT_CAPACITY_MULTIPLIER));
    }

    /**
     * Processes the monthly removal and addition of dependents based on campaign options and capacity.
     *
     * <p>This method manages both the removal and addition of dependents for the campaign within
     * the given constraints. The process consists of two phases:</p>
     * <ul>
     *   <li>Random addition of new dependents, if enabled in campaign options.</li>
     *   <li>Random removal of dependents, if enabled in campaign options.</li>
     * </ul>
     * The count of dependents is adjusted after the removal phase, and the method ensures the
     * total number of dependents does not exceed the allowed capacity.
     */
    public void processMonthlyRemovalAndAddition() {
        int dependentCount = activeDependents.size();

        // then roll for random addition
        if (isUseRandomDependentAddition) {
            dependentsAddNew(dependentCount);
        }

        // roll for random removal
        if (isUseRandomDependentRemoval) {
            dependentsRollForRemoval();
        }
    }

    /**
     * Calculates the number of active personnel in the campaign who are not dependents.
     *
     * <p>This method iterates through the active personnel in the associated {@link Campaign}
     * and applies the following filters to determine if a person qualifies as a non-dependent active personnel:</p>
     * <ul>
     *     <li>The person must not be categorized as a dependent.</li>
     *     <li>The person must not be a prisoner (unless they are a bondsman).</li>
     *     <li>The person must not be classified as a child based on the current date.</li>
     * </ul>
     *
     * <p>Personnel who fail these checks are either ignored or added to a corresponding list
     * ({@code activeDependents}) if they are identified as dependents.</p>
     *
     * @return The total number of active personnel who are non-dependents and meet the given criteria.
     */
    int prepareData() {
        int activeNonDependents = 0;

        for (Person person : campaign.getActivePersonnel(false)) {
            if (!person.isEmployed()) {
                activeDependents.add(person);
                continue;
            }

            if (person.isChild(currentDay)) {
                continue;
            }

            activeNonDependents++;
        }

        return activeNonDependents;
    }

    /**
     * Randomly removes dependents from the provided list based on campaign rules and options.
     *
     * <p>If random removal is enabled in the campaign options, dependents are evaluated for
     * eligibility for removal using {@link #isRemovalEligible(Person, LocalDate)}. Eligible dependents are removed
     * based on a rolling mechanism, and their status is updated within the campaign.</p>
     */
    void dependentsRollForRemoval() {
        List<Person> dependentsToRemove = new ArrayList<>();

        if (isUseRandomDependentRemoval) {
            for (Person dependent : activeDependents) {
                if (!isRemovalEligible(dependent, currentDay)) {
                    continue;
                }

                int roll = randomInt(20);

                if (activeDependents.size() > dependentCapacity) {
                    int secondRoll = randomInt(20);
                    roll = min(roll, secondRoll);
                }

                if (roll == 0) {
                    dependentsToRemove.add(dependent);

                    Genealogy genealogy = dependent.getGenealogy();
                    for (Person child : genealogy.getChildren()) {
                        if (child.isChild(currentDay)) {
                            dependentsToRemove.add(child);
                        }
                    }

                    Person spouse = genealogy.getSpouse();
                    if (spouse != null && (spouse.isDependent() || !spouse.isEmployed())) {
                        dependentsToRemove.add(spouse);
                    }
                }
            }

            if (!dependentsToRemove.isEmpty()) {
                int pluralizer = dependentsToRemove.size();

                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "dependentLeavesForce.report",
                      dependentsToRemove.size(), pluralizer));

                for (Person dependent : dependentsToRemove) {
                    dependent.changeStatus(campaign, currentDay, LEFT);
                    activeDependents.remove(dependent);
                }
            }
        }
    }

    /**
     * Determines whether a given dependent is eligible for removal based on their current status.
     *
     * <p>A dependent is eligible for removal if they have no non-adult children, no spouse, and
     * are not classified as a child themselves.</p>
     *
     * @param dependent   The {@link Person} object being evaluated.
     * @param currentDate The current date used for determining eligibility.
     *
     * @return {@code true} if the dependent is eligible for removal; {@code false} otherwise.
     */
    boolean isRemovalEligible(Person dependent, LocalDate currentDate) {
        boolean hasNonAdultChildren = dependent.getGenealogy().hasNonAdultChildren(currentDate);
        boolean hasSpouse = dependent.getGenealogy().hasSpouse();
        boolean isChild = dependent.isChild(currentDate);

        return !hasNonAdultChildren && !hasSpouse && !isChild;
    }

    /**
     * Randomly adds new dependents to the campaign based on the available dependent capacity and campaign options.
     *
     * <p>If the campaign options enable random dependent addition and the current dependent count
     * is below the allowed capacity, this method attempts to add new dependents. New dependents are created, recruited
     * into the campaign, and reported to the campaign logs.</p>
     *
     * @param dependentCount The current number of dependents.
     */
    void dependentsAddNew(int dependentCount) {
        if (isUseRandomDependentAddition && (dependentCount < dependentCapacity)) {
            int availableCapacity = dependentCapacity - dependentCount;
            int rollCount = (int) max(1, availableCapacity * 0.2);

            for (int i = 0; i < rollCount; i++) {
                int roll = Compute.randomInt(100);

                if (dependentCount < (dependentCapacity / 2)) {
                    int secondRoll = randomInt(20);
                    roll = min(roll, secondRoll);
                }

                if (roll == 0) {
                    final Person dependent = campaign.newDependent(RANDOMIZE);

                    campaign.recruitPerson(dependent, FREE, true, false, false);

                    campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "dependentJoinsForce.report",
                          dependent.getFullName(),
                          dependent.getPrimaryRole().getLabel(dependent.isClanPersonnel())));

                    dependentCount++;
                }
            }
        }
    }
}
