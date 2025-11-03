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
package mekhq.campaign;

import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonMedicalAssignmentEvent;
import mekhq.campaign.personnel.Person;

/**
 * Handles the optimization of doctor-to-patient assignments.
 *
 * <p>The {@code OptimizeInfirmaryAssignments} class is responsible for efficiently assigning doctors to patients
 * based on the severity of patients' injuries and the experience level of doctors. The assignment process takes into
 * account the maximum number of patients a doctor can handle, as well as the prisoner status of the patient.</p>
 *
 * <p>Key features of this class include:</p>
 * <ul>
 *   <li>Sorting doctors by their experience level to ensure the most skilled doctors are utilized first.</li>
 *   <li>Sorting patients by the severity of their medical needs, prioritizing critically injured individuals
 *       and deprioritizing prisoners.</li>
 *   <li>Assigning doctors to patients based on the sorted lists, ensuring capacity limits are adhered to
 *       while maintaining an efficient assignment strategy.</li>
 *   <li>Generating medical assignment events for doctor-patient pairings, which can be tracked throughout
 *       the campaign for monitoring and reporting purposes.</li>
 * </ul>
 *
 * <p>This class is designed to be instantiated with a {@link Campaign} object, after which it automatically
 * organizes doctors, sorts patients by priority, and assigns them according to the specified constraints.</p>
 *
 * @see Campaign Represents the campaign containing doctors, patients, and configuration details.
 * @see Person Represents an individual in the campaign, such as a doctor or patient.
 */
public class OptimizeInfirmaryAssignments {
    private final Campaign campaign;
    private List<Person> doctors;
    private List<Person> patients;

    /**
     * Optimizes the assignment of doctors to patients within the campaign.
     *
     * <p>This method sorts the doctors by their experience level and patients by the severity of
     * their injuries. It then assigns doctors to patients until either all doctors or all patients are exhausted. Each
     * doctor is assigned a limited number of patients based on the campaign's configuration.</p>
     *
     * <p>Priority is given to patients with the most severe injuries, with prisoners considered lower
     * priority than other personnel. The assignment also generates a medical assignment event for each pairing.</p>
     */
    public OptimizeInfirmaryAssignments(Campaign campaign) {
        // Get campaign configuration details
        this.campaign = campaign;
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
        final int maximumPatients = campaignOptions.getMaximumPatients();
        final int healingWaitingPeriod = campaignOptions.getHealingWaitingPeriod();

        // First, order the doctors based on experience level, highest to lowest
        organizeDoctors();

        // Then, order the patients based on severity of injuries,
        // rating prisoners as a lower priority than all other personnel.
        organizePatients();

        // Assign doctors to patients
        boolean isOnContract = !campaign.getActiveMissions(false).isEmpty();
        boolean isPlanetside = campaign.getLocation().isOnPlanet();
        boolean isOnContractAndPlanetside = isPlanetside && isOnContract;
        assignDoctors(isDoctorsUseAdministration,
              maximumPatients,
              healingWaitingPeriod,
              patients,
              doctors,
              isOnContractAndPlanetside);
    }

    /**
     * Assigns doctors to patients within the specified constraints while considering their capacities and healing
     * period.
     *
     * <p>This method ensures that each doctor is assigned to a limited number of patients, determined by their
     * calculated medical capacity. It first unassigns any existing doctor assignments for the provided patients, then
     * assigns doctors to the patients one by one. If a doctor reaches their capacity, the next available doctor is
     * assigned. Any remaining unassigned patients are left without a doctor when all doctors are exhausted.</p>
     *
     * @param isDoctorsUseAdministration A flag indicating whether the administrative skills of the doctors should be
     *                                   factored into their medical capacity calculation.
     * @param maximumPatients            The maximum base number of patients that a doctor can potentially handle, which
     *                                   is further adjusted based on their capacity calculation.
     * @param healingWaitingPeriod       The number of days for which a doctor is assigned to a patient. This value is
     *                                   applied to the assignments to represent the duration of a healing period.
     * @param patients                   The list of patients to which doctors need to be assigned. Any patients that
     *                                   cannot be assigned due to insufficient doctor capacity remain unassigned.
     * @param doctors                    The list of available doctors, ordered by priority (e.g., experience level or
     *                                   suitability). Doctors higher on the list are assigned first.
     * @param isOnContractAndPlanetside  {@code true} if the campaign has an active mission and is planetside (i.e., not
     *                                   in transit).
     */
    private void assignDoctors(final boolean isDoctorsUseAdministration, final int maximumPatients,
          final int healingWaitingPeriod, final List<Person> patients, List<Person> doctors,
          final boolean isOnContractAndPlanetside) {
        boolean useMASHTheatres = campaign.getCampaignOptions().isUseMASHTheatres();
        int mashTheatreCapacity = useMASHTheatres ? campaign.getMashTheatreCapacity() : Integer.MAX_VALUE;

        int totalPatientCounter = 0;
        int patientCounter = 0;
        int doctorCapacity = 0;

        for (Person patient : patients) {
            patient.setDoctorId(null, healingWaitingPeriod);

            if (doctors.isEmpty()) {
                // At this point, we're just unassigning the doctor assignments for any remaining personnel.
                continue;
            }

            if (isOnContractAndPlanetside && useMASHTheatres && totalPatientCounter >= mashTheatreCapacity) {
                // Similar to the above, we're just unassigning doctors for any remaining patients.
                continue;
            }

            Person doctor = doctors.get(0);
            if (doctorCapacity == 0) {
                doctorCapacity = doctor.getDoctorMedicalCapacity(isDoctorsUseAdministration, maximumPatients);
            }

            if (doctorCapacity == 0) {
                continue;
            }

            // Make the assignment
            patient.setDoctorId(doctor.getId(), healingWaitingPeriod);
            totalPatientCounter++;
            MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, patient));

            // Check if the current doctor has reached their patient limit
            if (++patientCounter == doctorCapacity) {
                doctors.remove(0); // Move to the next doctor
                patientCounter = 0; // Reset patient counter
                doctorCapacity = 0; // Reset doctor capacity
            }
        }
    }

    /**
     * Organizes the list of doctors in descending order of experience level.
     *
     * <p>This method sorts the doctors within the campaign, prioritizing those with the highest
     * experience levels so that the most skilled doctors are assigned first.</p>
     */
    private void organizeDoctors() {
        doctors = campaign.getDoctors();
        doctors.sort((doctor1, doctor2) -> Integer.compare(getDoctorExperienceLevel(doctor2),
              getDoctorExperienceLevel(doctor1)));
    }

    /**
     * Organizes the list of patients in descending order of severity.
     *
     * <p>This method sorts the patients based on their medical need. Patients with more severe
     * injuries are given higher priority, while prisoners are treated as lower priority by artificially increasing the
     * severity value of non-prisoners.</p>
     */
    private void organizePatients() {
        patients = campaign.getPatients();
        patients.sort((patient1, patient2) -> Integer.compare(getSeverity(patient2), getSeverity(patient1)));
    }

    /**
     * Calculates the severity of a patient's injuries.
     *
     * <p>This method evaluates the severity of a patient's condition based on their injuries or
     * health status. Priority is given to patients who need fixing, with the severity based on the number of injuries.
     * Non-Prisoners are treated as higher priority, with their severity score multiplied by a factor of 10.</p>
     *
     * @param patient the {@link Person} to calculate the severity for
     *
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
     * experience values. It also checks if the doctor’s secondary role qualifies them as a doctor within the current
     * campaign.</p>
     *
     * @param doctor the {@link Person} acting as the doctor
     *
     * @return the experience level of the doctor
     */
    private int getDoctorExperienceLevel(Person doctor) {
        return doctor.getExperienceLevel(campaign, doctor.getSecondaryRole().isDoctor());
    }
}
