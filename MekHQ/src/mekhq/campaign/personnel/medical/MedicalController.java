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

import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.TargetRollModifier;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonMedicalAssignmentEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateHealing;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.unit.Unit;

/**
 * The {@code MedicalController} class manages the healing process and medical-related tasks within the campaign. It
 * handles natural healing, doctor-assisted healing, and advanced medical options for campaign participants.
 *
 * @author Illiani
 * @since MekHQ 0.50.06
 */
public class MedicalController {
    private static final MMLogger LOGGER = MMLogger.create(MedicalController.class);

    final String RESOURCE_BUNDLE = "mekhq.resources.MedicalController";

    final private Campaign campaign;
    final private boolean isDoctorsUseAdministration;
    final private int maximumPatients;
    final private int healingWaitingPeriod;
    final private int naturalHealingWaitingPeriod;
    final private boolean isUseSupportEdge;
    final private boolean isUseAdvancedMedical;
    final private boolean isUseAltAdvancedMedical;


    /**
     * Constructs a new {@link MedicalController} with the given campaign, setting fields based on
     * {@link CampaignOptions}
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
        isUseAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        isUseAltAdvancedMedical = campaignOptions.isUseAlternativeAdvancedMedical();
    }

    /**
     * Use {@link #processMedicalEvents(Person, boolean, boolean, LocalDate)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void processMedicalEvents(Person patient) {
        processMedicalEvents(patient, false, false, LocalDate.of(3151, 1, 1));
    }

    /**
     * Processes daily medical events for a given patient, handling both standard and advanced medical healing.
     *
     * <p>The method orchestrates healing for a {@link Person} by applying doctor-assisted healing, natural healing
     * rolls, and advanced medical rules, depending on campaign settings and the patient's needs.</p>
     * <ul>
     *   <li>If the patient requires healing and advanced medical rules are <b>not</b> enabled,
     *       it attempts to validate and assign a doctor for assisted healing. If a doctor is not available or able
     *       to help, it attempts natural healing instead. Successful natural healing is logged, and related
     *       unit states are reset.</li>
     *   <li>If advanced medical rules are enabled, the method defers the healing process to the advanced
     *       medical subsystem and resets unit-related state as needed.</li>
     * </ul>
     *
     * @param patient           the {@link Person} undergoing healing
     * @param isUseAgingEffects {@code true} if aging effects should be included when applying healing
     * @param isClanCampaign    {@code true} if the campaign uses clan-based rules
     * @param today             the current {@link LocalDate} for time-dependent calculations
     */
    public void processMedicalEvents(Person patient, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        // Should the character be dead already?
        if (patient.getTotalInjurySeverity() >= 5) {
            patient.changeStatus(campaign, today, PersonnelStatus.WOUNDS);
            return; // Early exit as there is no point continuing to process the character
        }

        Person doctor = campaign.getPerson(patient.getDoctorId());

        if (doctor != null) {
            doctor = isValidDoctor(patient, doctor) ? doctor : null;
        }

        if (patient.needsFixing()) {
            // This will trigger for both AM-enabled and AM-disabled campaigns
            doctor = verifyTheatreAvailability(patient, doctor);
            patient.decrementDaysToWaitForHealing();

            // Handle Advanced Medical
            if (isUseAdvancedMedical) {
                if (isUseAltAdvancedMedical) {
                    AdvancedMedicalAlternateHealing.processNewDay(campaign.getLocalDate(),
                          campaign.getCampaignOptions().isUseFatigue(), campaign.getCampaignOptions().getFatigueRate(),
                          patient, doctor);
                } else {
                    InjuryUtil.resolveDailyHealing(campaign, patient);
                }
            } else {
                if (doctor != null && patient.getDaysToWaitForHealing() <= 0) {
                    healPerson(patient, doctor, isUseAgingEffects, isClanCampaign, today);
                } else if (checkNaturalHealing(patient)) {
                    LOGGER.debug(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.natural",
                          patient.getHyperlinkedFullTitle()));
                }
            }

            Unit unit = patient.getUnit();
            if (unit != null) {
                unit.resetPilotAndEntity();
            }
        }
    }

    private Person verifyTheatreAvailability(Person patient, Person doctor) {
        if (campaign.getCampaignOptions().isUseMASHTheatres()) {
            if (!campaign.getMashTheatresWithinCapacity()) {
                doctor = null;
                patient.setDoctorId(null, campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());
                campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                      "MedicalController.report.overTheatreCapacity",
                      spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                      patient.getHyperlinkedFullTitle()));
            }
        }
        return doctor;
    }

    /**
     * Checks if the patient can heal naturally (without a doctor) and processes the healing if possible.
     *
     * @param patient the {@link Person} to check and heal naturally
     *
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
     * Use {@link #healPerson(Person, Person, boolean, boolean, LocalDate)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    private void healPerson(Person patient, Person doctor) {
        healPerson(patient, doctor, false, false, LocalDate.of(3151, 1, 1));
    }

    /**
     * Applies medical treatment to the specified patient, using the given doctor as the medical provider.
     *
     * <p>This method performs a skill check for the doctor using current campaign rules and relevant situational
     * modifiers. If the skill check succeeds, the patient is healed and any associated unit state is reset. Regardless
     * of the outcome, the patient's healing waiting period is reset.</p>
     *
     * @param patient           the {@link Person} receiving treatment
     * @param doctor            the {@link Person} performing the medical treatment
     * @param isUseAgingEffects {@code true} if aging effects should influence the healing process
     * @param isClanCampaign    {@code true} if campaign-specific (clan) rules apply to healing
     * @param today             the current date, used for time-dependent effects
     */
    private void healPerson(Person patient, Person doctor, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        LOGGER.debug(getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.intro",
              doctor.getHyperlinkedFullTitle(), patient.getHyperlinkedFullTitle()));

        SkillCheckUtility skillCheckUtility = new SkillCheckUtility(
              getTextAt(RESOURCE_BUNDLE, "MedicalController.report.skillCheck"),
              doctor,
              S_SURGERY,
              getAdditionalHealingModifiers(patient),
              0,
              isUseSupportEdge,
              false,
              isUseAgingEffects,
              isClanCampaign,
              today);

        LOGGER.debug(skillCheckUtility.getResultsText());

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
     *
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
     *
     * @return {@code true} if the doctor is valid; otherwise, {@code false}
     */
    private boolean isValidDoctor(Person patient, Person doctor) {
        int medicalCapacity = doctor.getDoctorMedicalCapacity(isDoctorsUseAdministration, maximumPatients);
        if (!doctor.isDoctor()) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.notADoctor",
                  doctor.getHyperlinkedFullTitle(), patient.getHyperlinkedFullTitle()));
            unassignDoctor(patient, doctor);
            return false;
        }

        if (campaign.getPatientsFor(doctor) > medicalCapacity) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE, "MedicalController.report.overCapacity",
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
