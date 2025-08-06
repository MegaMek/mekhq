/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

public class UntreatedPersonnelNagLogic {
    /**
     * Determines whether the campaign has any personnel with untreated injuries.
     *
     * <p>This method evaluates the active personnel in the campaign to identify individuals who:</p>
     * <ul>
     *     <li>Require medical treatment ({@link Person#needsFixing()}).</li>
     *     <li>Have not been assigned to a doctor (their {@code getDoctorId()} is {@code null}).</li>
     * </ul>
     *
     * <p>If any personnel meet these criteria, the method returns {@code true}.</p>
     *
     * @param activePersonnel A {@link List} of active personnel in the campaign.
     *
     * @return {@code true} if there are untreated injuries among the personnel, {@code false} otherwise.
     */
    public static boolean campaignHasUntreatedInjuries(List<Person> activePersonnel, int doctorCapacity) {
        // if we're automatically optimizing medical assignments, we only want to advance day if there are more
        // patients than doctor capacity
        if (MekHQ.getMHQOptions().getNewDayOptimizeMedicalAssignments()) {
            return checkDoctorCapacity(activePersonnel, doctorCapacity);
        }

        // Otherwise, we only need to find the first unassigned patient.
        for (Person person : activePersonnel) {
            if (person.needsFixing() && person.getDoctorId() == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the current doctor capacity is sufficient to handle the patients needing attention.
     *
     * <p>This method iterates through a list of active personnel to count the number of patients
     * (individuals who need fixing) and available doctor capacity. The available doctor capacity is calculated by
     * multiplying the number of doctors by their individual capacity. The method returns whether the number of patients
     * exceeds the calculated doctor capacity.</p>
     *
     * @param activePersonnel a list of {@link Person} objects representing the active personnel, including both
     *                        patients and doctors.
     * @param doctorCapacity  the number of patients a single doctor can handle.
     *
     * @return {@code true} if the number of patients exceeds the total doctor capacity, {@code false} otherwise.
     */
    private static boolean checkDoctorCapacity(List<Person> activePersonnel, int doctorCapacity) {
        int patients = 0;
        int doctors = 0;

        for (Person person : activePersonnel) {
            if (person.needsFixing()) {
                patients++;
            }

            if (person.isDoctor()) {
                doctors += doctorCapacity;
            }
        }

        return patients > doctors;
    }

    /**
     * Calculates the total medical capacity of all doctors in the provided list of active personnel.
     *
     * <p>This method iterates through the list of active personnel and calculates each individual's
     * medical capacity based on their skills and experience, while optionally factoring in their administrative
     * capabilities. The total capacity is the sum of all individual doctor capacities.</p>
     *
     * @param activePersonnel            The list of active personnel to evaluate. Only those with the ability to act as
     *                                   doctors are considered in the calculation.
     * @param isDoctorsUseAdministration A flag determining whether to include each doctor's administrative skills as
     *                                   part of the capacity calculation.
     * @param baseBedCount               The base number of beds or patients a doctor can handle, which serves as the
     *                                   starting capacity for each doctor before adjustments.
     *
     * @return The total medical capacity of the doctors in the provided list, as an integer representing the total
     *       number of beds or patients they can collectively manage.
     */
    public static int calculateTotalDoctorCapacity(final List<Person> activePersonnel,
          final boolean isDoctorsUseAdministration, final int baseBedCount) {
        int totalCapacity = 0;

        for (Person person : activePersonnel) {
            totalCapacity += person.getDoctorMedicalCapacity(isDoctorsUseAdministration, baseBedCount);
        }

        return totalCapacity;
    }
}
