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
 */
package mekhq.campaign;

import mekhq.MekHQ;
import mekhq.campaign.event.PersonMedicalAssignmentEvent;
import mekhq.campaign.personnel.Person;

import java.util.List;

public class OptimizeInfirmaryAssignments {
    private final Campaign campaign;

    /**
     * Optimizes the assignment of doctors to patients within the campaign.
     *
     * <p>This method sorts the doctors by their experience level and patients by the severity of
     * their injuries. It then assigns doctors to patients until either all doctors or all patients
     * are exhausted. Each doctor is assigned a limited number of patients based on the campaign's
     * configuration.</p>
     *
     * <p>Priority is given to patients with the most severe injuries, with prisoners considered lower
     * priority than other personnel. The assignment also generates a medical assignment event for
     * each pairing.</p>
     */
    public OptimizeInfirmaryAssignments(Campaign campaign) {
        this.campaign = campaign;

        // Get campaign configuration details
        int patientsPerDoctor = campaign.getCampaignOptions().getMaximumPatients();
        int healingWaitingPeriod = campaign.getCampaignOptions().getHealingWaitingPeriod();

        // First, order the doctors based on experience level, highest to lowest
        List<Person> doctors = organizeDoctors(campaign);

        // Then, order the patients based on severity of injuries,
        // rating prisoners as a lower priority than all other personnel.
        List<Person> patients = organizePatients(campaign);

        // Assign doctors to patients
        assignDoctors(patientsPerDoctor, healingWaitingPeriod, patients, doctors);
    }

    /**
     * Assigns doctors to patients within the specified constraints.
     *
     * <p>This method first clears all existing doctor assignments for the provided patients and
     * then assigns doctors to patients based on the provided lists. The assignment ensures
     * that each doctor is responsible for no more than a predefined number of patients.
     * When doctors are exhausted, remaining patients are left unassigned.</p>
     *
     * @param patientsPerDoctor the maximum number of patients that each doctor can handle
     * @param healingWaitingPeriod the number of days for which the doctor is assigned to a patient
     * @param patients the list of patients to be assigned doctors
     * @param doctors the list of available doctors, ordered by priority (e.g., experience level)
     */
    private static void assignDoctors(int patientsPerDoctor, int healingWaitingPeriod,
                                      List<Person> patients, List<Person> doctors) {
        int patientCounter = patientsPerDoctor;
        for (Person patient : patients) {
            patient.setDoctorId(null, healingWaitingPeriod);

            if (doctors.isEmpty()) {
                // at this point, we're just unassigning the doctor assignments for any remaining personnel.
                continue;
            }

            Person doctor = doctors.get(0);

            // Make the assignment
            patient.setDoctorId(doctor.getId(), healingWaitingPeriod);
            MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, patient));

            // Check if the current doctor has reached their patient limit
            if (patientCounter-- == 0) {
                doctors.remove(0); // Move to the next doctor
                patientCounter = patientsPerDoctor;
            }
        }
    }

    /**
     * Organizes the list of doctors in descending order of experience level.
     *
     * <p>This method sorts the doctors within the campaign, prioritizing those with the highest
     * experience levels so that the most skilled doctors are assigned first.</p>
     *
     * @param campaign the {@link Campaign} instance containing the current list of doctors
     * @return a sorted list of doctors, ordered by decreasing experience level
     */
    private List<Person> organizeDoctors(Campaign campaign) {
        List<Person> doctors = campaign.getDoctors();
        doctors.sort((doctor1, doctor2) -> Integer.compare(
              getDoctorExperienceLevel(doctor2),
              getDoctorExperienceLevel(doctor1)
        ));
        return doctors;
    }

    /**
     * Organizes the list of patients in descending order of severity.
     *
     * <p>This method sorts the patients based on their medical need. Patients with more severe
     * injuries are given higher priority, while prisoners are treated as lower priority by
     * artificially increasing the severity value of non-prisoners.</p>
     *
     * @param campaign the {@link Campaign} instance containing the current list of patients
     * @return a sorted list of patients, ordered by decreasing severity
     */
    private List<Person> organizePatients(Campaign campaign) {
        List<Person> patients = campaign.getPatients();
        patients.sort((patient1, patient2) -> Integer.compare(
              getSeverity(patient2),
              getSeverity(patient1)
        ));
        return patients;
    }

    /**
     * Calculates the severity of a patient's injuries.
     *
     * <p>This method evaluates the severity of a patient's condition based on their injuries or
     * health status. Priority is given to patients who need fixing, with the severity based on the
     * number of injuries. Non-Prisoners are treated as higher priority, with their severity score
     * multiplied by a factor of 10.</p>
     *
     * @param patient the {@link Person} to calculate the severity for
     * @return the severity score of the patient’s medical condition
     */
    private int getSeverity(Person patient) {
        int severity = 0;

        if (patient.needsAMFixing()) {
            severity = patient.getInjuries().size(); // Severity based on number of injuries
        } else if (patient.needsFixing()) {
            severity = patient.getHits(); // Severity based on number of hits
        }

        if (patient.getPrisonerStatus().isFreeOrBondsman()) {
            severity *= 10; // Prioritize Non-Prisoners
        }

        return severity;
    }

    /**
     * Retrieves the experience level of a doctor.
     *
     * <p>This method calculates a doctor's experience level by delegating to their personal
     * experience values. It also checks if the doctor’s secondary role qualifies them as a doctor
     * within the current campaign.</p>
     *
     * @param doctor the {@link Person} acting as the doctor
     * @return the experience level of the doctor
     */
    private int getDoctorExperienceLevel(Person doctor) {
        return doctor.getExperienceLevel(campaign, doctor.getSecondaryRole().isDoctor());
    }
}
