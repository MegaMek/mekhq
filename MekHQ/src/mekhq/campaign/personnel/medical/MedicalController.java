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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil.resolveDailyHealing;
import static mekhq.campaign.personnel.skills.SkillType.S_DOCTOR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.TargetRollModifier;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.PersonMedicalAssignmentEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.unit.Unit;

/**
 * The {@code MedicalController} class manages the healing process and medical-related tasks
 * within the campaign. It handles natural healing, doctor-assisted healing, and advanced
 * medical options for campaign participants.
 *
 * @author Illiani
 * @since MekHQ 0.50.06
 */
public class MedicalController {
    private static final MMLogger logger = MMLogger.create(MedicalController.class);

    final String RESOURCE_BUNDLE = "mekhq.resources.MedicalController";

    final private Campaign campaign;
    final private boolean isDoctorsUseAdministration;
    final private int maximumPatients;
    final private int healingWaitingPeriod;
    final private int naturalHealingWaitingPeriod;
    final private boolean isUseSupportEdge;
    final private boolean isUseMedicalController;


    /**
     * Constructs a new {@link MedicalController} with the given campaign, setting fields based on {@link CampaignOptions}
     *
     * @param campaign the {@link Campaign} this controller is associated with
     */
    public MedicalController(Campaign campaign) {
        this.campaign = campaign;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
        maximumPatients = campaignOptions.getMaximumPatients();
        healingWaitingPeriod = campaignOptions.getHealingWaitingPeriod();
        naturalHealingWaitingPeriod = campaignOptions.getNaturalHealingWaitingPeriod();
        isUseSupportEdge = campaignOptions.isUseSupportEdge();
        isUseMedicalController = campaignOptions.isUseAdvancedMedical();
    }


    /**
     * Processes the medical events for the given patient. This includes doctor-assisted healing,
     * natural healing, and advanced medical rules (if enabled).
     *
     * <p>For non-advanced medical rules, the method validates and assigns doctors and handles
     * natural healing if the doctor cannot assist. For advanced medical rules, it delegates
     * the processing to the advanced medical subsystem.</p>
     *
     * @param patient the {@link Person} being healed
     */
    public void processMedicalEvents(Person patient) {
        Person doctor = campaign.getPerson(patient.getDoctorId());

        if (doctor != null) {
            doctor = isValidDoctor(patient, doctor) ? doctor : null;
        }

        // Handle non-Advanced Medical healing
        if (patient.needsFixing()) {
            patient.decrementDaysToWaitForHealing();

            if (doctor != null && patient.getDaysToWaitForHealing() <= 0) {
                healPerson(patient, doctor);
            } else if (checkNaturalHealing(patient)) {
                // TODO change logging level from info to debug in 50.08
                logger.info(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.natural",
                      patient.getHyperlinkedFullTitle()));
                Unit unit = patient.getUnit();
                if (unit != null) {
                    unit.resetPilotAndEntity();
                }
            }
        }

        // Handle Advanced Medical
        if (isUseMedicalController) {
            resolveDailyHealing(campaign, patient);
            Unit unit = patient.getUnit();
            if (unit != null) {
                unit.resetPilotAndEntity();
            }
        }
    }


    /**
     * Checks if the patient can heal naturally (without a doctor) and processes the healing if possible.
     *
     * @param patient the {@link Person} to check and heal naturally
     * @return {@code true} if the patient successfully heals naturally; otherwise, {@code false}
     */
    public boolean checkNaturalHealing(Person patient) {
        if (patient.needsFixing() && (patient.getDaysToWaitForHealing() <= 0) && (patient.getDoctorId() == null)) {
            patient.heal();
            patient.setDaysToWaitForHealing(naturalHealingWaitingPeriod);
            return true;
        }
        return false;
    }


    /**
     * Heals the given patient with assistance from the specified doctor if they meet all necessary conditions.
     *
     * <p>The method calculates modifiers using campaign rules, performs a skill check for the doctor, and updates
     * the patient's healing status based on the result.</p>
     *
     * @param patient the {@link Person} receiving medical treatment
     * @param doctor  the {@link Person} performing the treatment
     */
    private void healPerson(Person patient, Person doctor) {
        // TODO change logging level from info to debug in 50.08
        logger.info(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.intro",
              doctor.getHyperlinkedFullTitle(), patient.getHyperlinkedFullTitle()));

        SkillCheckUtility skillCheckUtility = new SkillCheckUtility(doctor,
              S_DOCTOR,
              getAdditionalHealingModifiers(patient),
              0,
              isUseSupportEdge,
              false);

        // TODO change logging level from info to debug in 50.08
        logger.info(skillCheckUtility.getResultsText());

        if (skillCheckUtility.isSuccess()) {
            patient.heal();
            Unit unit = patient.getUnit();
            if (unit != null) {
                unit.resetPilotAndEntity();
            }
        }

        patient.setDaysToWaitForHealing(healingWaitingPeriod);
    }


    /**
     * Retrieves additional healing modifiers for the given patient. These modifiers are based on campaign-specific
     * rules, such as staff shortages.
     *
     * <p>The method utilizes campaign shorthand rules to calculate specific target roll modifiers and appends any
     * patient-specific healing adjustments.</p>
     *
     * @param patient the {@link Person} receiving medical care
     * @return a list of {@link TargetRollModifier}s that apply during the healing process
     */
    public List<TargetRollModifier> getAdditionalHealingModifiers(Person patient) {
        List<TargetRollModifier> modifiers = new ArrayList<>();

        // understaffed mods
        int helpModifier = campaign.getShorthandedMod(campaign.getMedicsPerDoctor(), true);

        if (helpModifier > 0) {
            modifiers.add(new TargetRollModifier(helpModifier, getFormattedTextAt(RESOURCE_BUNDLE,
                  "MedicalController.modifier.shorthanded")));
        }

        modifiers.add(patient.getHealingMods(campaign));

        return modifiers;
    }


    /**
     * Checks whether the given doctor is valid for treating the given patient.
     *
     * <p>A doctor is considered valid if:</p>
     *
     * <ul>
     *     <li>They have the necessary skills (are flagged as a doctor).</li>
     *     <li>They have not exceeded their medical capacity (based on the campaign rules).</li>
     * </ul>
     *
     * <p>If invalid, the doctor is unassigned from the patient.</p>
     *
     * @param patient the {@link Person} being checked
     * @param doctor  the {@link Person} being validated as a doctor
     * @return {@code true} if the doctor is valid; otherwise, {@code false}
     */
    private boolean isValidDoctor(Person patient, Person doctor) {
        int medicalCapacity = doctor.getDoctorMedicalCapacity(isDoctorsUseAdministration, maximumPatients);
        if (!doctor.isDoctor()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.notADoctor",
                  doctor.getHyperlinkedFullTitle(), patient.getHyperlinkedFullTitle()));
            unassignDoctor(patient, doctor);
            return false;
        }

        if (campaign.getPatientsFor(doctor) > medicalCapacity) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.overCapacity",
                  doctor.getHyperlinkedFullTitle(), patient.getHyperlinkedFullTitle()));
            unassignDoctor(patient, doctor);

            return false;
        }

        return true;
    }


    /**
     * Unassigns the doctor from the patient and triggers a medical assignment event.
     *
     * @param patient the {@link Person} whose doctor is being unassigned
     * @param doctor  the {@link Person} being unassigned from the patient
     */
    private void unassignDoctor(Person patient, Person doctor) {
        patient.setDoctorId(null, naturalHealingWaitingPeriod);
        MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, patient));
    }
}
