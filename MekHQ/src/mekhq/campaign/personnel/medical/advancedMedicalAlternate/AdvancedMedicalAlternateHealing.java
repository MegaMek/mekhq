/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.units.Crew.DEATH;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_FIT;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.enums.PersonnelStatus.MEDICAL_COMPLICATIONS;
import static mekhq.campaign.personnel.medical.BodyLocation.GENERIC;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.HealingMarginOfSuccessEffects.getEffectFromHealingAttempt;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PatientLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;


/**
 * Handles daily healing for the "Advanced Medical Alternate" ruleset.
 *
 * <p>This class implements the logic for processing natural and assisted healing checks for {@link Injury} instances
 * on a {@link Person} under the alternate advanced medical rules. It applies SPA-based modifiers, prosthetic penalties,
 * and optional fatigue damage, and interprets the margin of success using {@link HealingMarginOfSuccessEffects}.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class AdvancedMedicalAlternateHealing {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedMedicalAlternateHealing";
    private static final int PROSTHETIC_PENALTY = 4; // Interstellar Operations page 70

    // ATOW pg193 6th printing
    private static final int PERMANENT_INJURY_THRESHOLD = -3;

    /**
     * Processes the start-of-day healing for a given patient.
     *
     * <p>This method calculates any SPA-based modifiers, determines whether the healing attempt is assisted or
     * unassisted based on the presence of a doctor, and then performs the appropriate healing checks. It also handles
     * optional fatigue changes and the use of medical Edge.</p>
     *
     * @param campaign the {@link Campaign} context
     * @param patient  the person undergoing healing
     * @param doctor   the doctor providing treatment, or {@code null} if the patient is healing naturally
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processNewDay(Campaign campaign, Person patient,
          @Nullable Person doctor) {
        // Modifiers
        List<TargetRollModifier> modifiers = getSPAModifiers(patient);
        Set<BodyLocation> prostheticPenalties = getProstheticPenalties(patient);


        // Healing
        if (doctor == null) {
            modifiers.add(new TargetRollModifier(-3, "Unassisted Healing"));
        }

        performHealingCheck(campaign,
              patient,
              Objects.requireNonNullElse(doctor, patient),
              modifiers,
              prostheticPenalties);
    }

    /**
     * Determines which body locations incur a prosthetic penalty during healing.
     *
     * <p>This method scans all permanent injuries and records the primary body locations that are associated with
     * prosthetic subtypes. These locations will later receive a flat penalty to healing attempts.</p>
     *
     * @param patient the person whose permanent injuries are being examined
     *
     * @return a set of body locations that suffer the prosthetic penalty
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static Set<BodyLocation> getProstheticPenalties(Person patient) {
        Set<BodyLocation> prostheticPenalties = new HashSet<>();
        for (Injury injury : patient.getPermanentInjuries()) {
            InjurySubType injurySubType = injury.getSubType();
            if (injurySubType.isPermanentModification()) {
                BodyLocation location = injury.getLocation();
                BodyLocation primaryLocation = location.getPrimaryLocation();

                if (primaryLocation != null) {
                    prostheticPenalties.add(primaryLocation);
                }
            }
        }
        return prostheticPenalties;
    }

    /**
     * Builds the list of Target Roll modifiers granted by the patient's SPAs (Special Pilot Abilities).
     *
     * <p>Currently this considers the {@code ATOW_FIT} and {@code ATOW_TOUGHNESS} options and applies a -1 modifier
     * for each if present.</p>
     *
     * @param patient the person whose SPAs are being checked
     *
     * @return a list of {@link TargetRollModifier} instances representing SPA-based healing modifiers
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static List<TargetRollModifier> getSPAModifiers(Person patient) {
        List<TargetRollModifier> modifiers = new ArrayList<>();

        PersonnelOptions options = patient.getOptions();
        if (options.booleanOption(ATOW_FIT)) {
            modifiers.add(new TargetRollModifier(-1, "Fit SPA"));
        }

        if (options.booleanOption(ATOW_TOUGHNESS)) {
            modifiers.add(new TargetRollModifier(-1, "Toughness SPA"));
        }
        return modifiers;
    }

    private static void addPathologicInsightModifier(List<TargetRollModifier> modifiers, Injury injury,
          boolean hasPathologicInsight) {
        if (hasPathologicInsight && injury.isDisease()) {
            modifiers.add(new TargetRollModifier(-2, "Pathologic Insight"));
        }
    }


    /**
     * Calculates the combined penalty applied to a healing roll for a specific injury.
     *
     * <p>The base penalty is the total injury severity for the patient. If the injury is located on a body part
     * represented by a prosthetic and that location is present in {@code prostheticPenalties}, the prosthetic penalty
     * is added.</p>
     *
     * @param injuryPenalty          the total injury severity for the patient
     * @param prostheticPenalties    the set of body locations that incur prosthetic penalties
     * @param location               the body location of the injury being healed
     * @param hasTraumaSurgeon       {@code true} if the patient has the trauma surgeon SPA (reduces injury penalty)
     * @param hasProthesisTechnician {@code true} if the patient has the prothesis technician SPA (reduces prosthetic
     *                               penalty)
     *
     * @return the sum of the base injury penalty and any applicable prosthetic penalty
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getMiscPenalty(int injuryPenalty, Set<BodyLocation> prostheticPenalties, BodyLocation location,
          boolean hasTraumaSurgeon, boolean hasProthesisTechnician) {
        int miscPenalty = injuryPenalty;
        int traumaSurgeonModifier = hasTraumaSurgeon ? -1 : 0;
        miscPenalty = max(0, miscPenalty + traumaSurgeonModifier);

        boolean hasProstheticPenalty = false;
        BodyLocation primaryLocation = location.getPrimaryLocation();
        if (prostheticPenalties.contains(primaryLocation)) {
            miscPenalty += PROSTHETIC_PENALTY;
            hasProstheticPenalty = true;
        }

        if (hasProstheticPenalty) {
            int technicianModifier = hasProthesisTechnician ? -1 : 0;
            miscPenalty = max(0, miscPenalty + technicianModifier);
        }

        return miscPenalty;
    }

    /**
     * Performs the daily assisted healing checks for a patient treated by a doctor.
     *
     * <p>This method iterates over all non-permanent injuries, decrements their remaining healing time, and once the
     * timer reaches zero or below, resolves a surgery skill check by the doctor. The result is then interpreted into
     * concrete healing effects, logging, and task rewards. Medical Edge may be used to reroll potentially permanent
     * results.</p>
     *
     * <p>A defensive copy of the injury list is used because successful healing may remove injuries from the
     * underlying collection.</p>
     *
     * @param campaign            the {@link Campaign} context
     * @param patient             the person being treated
     * @param doctor              the doctor performing the assisted healing
     * @param modifiers           the list of modifiers applied to the surgery check
     * @param prostheticPenalties the set of body locations that should incur a prosthetic penalty
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void performHealingCheck(Campaign campaign, Person patient, Person doctor,
          List<TargetRollModifier> modifiers, Set<BodyLocation> prostheticPenalties) {
        HealingSPAOptions healingSPAOptions = HealingSPAOptions.from(doctor, patient);
        LocalDate today = campaign.getLocalDate();

        // We need a defensive copy of the list as we're going to be removing injuries from it when successfully healing
        for (Injury injury : new ArrayList<>(patient.getInjuries())) {
            attemptHealing(campaign, patient, doctor, modifiers, prostheticPenalties, injury, healingSPAOptions, today);
        }
    }

    /**
     * Attempts to heal a specified injury for a given patient under the care of a doctor within a campaign context. The
     * method takes into account various modifiers, penalties, and situational factors to determine the progress of
     * healing or possible worsening of the injury.
     *
     * <p><b>Note:</b> for unassisted healing the patient is also going to be the doctor.</p>
     *
     * @param campaign            The campaign context in which the healing attempt is taking place.
     * @param patient             The person receiving treatment for the injury.
     * @param doctor              The person providing medical care for the injury.
     * @param modifiers           A list of target roll modifiers that influence the healing outcome.
     * @param prostheticPenalties A set of body locations where prosthetic-related penalties may apply.
     * @param injury              The injury being treated, which includes severity, location, and status.
     * @param healingSPAOptions   Various special options and attributes influencing the healing process, such as
     *                            holistic care or medical proficiency features.
     * @param today               The current date in the campaign, used for determining the healing progress for
     *                            unassisted healing and for reporting healing outcomes.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void attemptHealing(Campaign campaign, Person patient, Person doctor,
          List<TargetRollModifier> modifiers, Set<BodyLocation> prostheticPenalties, Injury injury,
          HealingSPAOptions healingSPAOptions, LocalDate today) {
        addPathologicInsightModifier(modifiers, injury, healingSPAOptions.hasPathologicInsight());

        // If we're performing unassisted healing, recovery time is substantially decreased sevenfold, as per ATOW
        // pg193 (6th printing). We represent this by only decreasing healing time on Mondays.
        if (Objects.equals(doctor, patient)) {
            if (today.getDayOfWeek() != DayOfWeek.MONDAY) {
                return;
            }
        }

        if (!injury.isPermanent()) {
            // This needs to be refetched each cycle as the number of concurrent injuries might have changed
            int injuryPenalty = max(0, patient.getTotalInjurySeverity() - patient.getAdjustedToughness());

            int healingDelta = healingSPAOptions.hasHolisticCareSPA() && randomInt(20) == 0 ? -2 : -1;
            injury.changeTime(healingDelta);

            int miscPenalty = getMiscPenalty(injuryPenalty, prostheticPenalties, injury.getLocation(),
                  healingSPAOptions.hasTraumaSurgeon(), healingSPAOptions.hasProthesisTechnician());
            miscPenalty += healingSPAOptions.hasHypochondriac() ? 1 : 0;

            boolean useEdge = campaign.getCampaignOptions().isUseEdge();
            useEdge = useEdge && healingSPAOptions.hasMedicalEdge();
            int marginOfSuccess = getMarginOfSuccessForHealing(
                  doctor, campaign, modifiers, miscPenalty, useEdge);

            if (injury.getTime() <= 0) {
                HealingMarginOfSuccessEffects outcome = processHealingEffects(campaign,
                      patient,
                      injury,
                      marginOfSuccess);
                processTaskAwardsAndPersonnelLogUpdates(today, patient, doctor, injury, outcome);
            } else if (marginOfSuccess < 0) { // The injury took longer to heal
                injury.changeTime(1); // Undo the prior reduction
            }
        }
    }

    /**
     * Performs the assisted healing roll for a doctor and returns its margin of success.
     *
     * <p>The roll is a skill check using the doctor's {@code Surgery} skill, modified by the provided target roll
     * modifiers and miscellaneous penalties. If the initial result causes the injury to become permanent (margin of
     * success &le; {@link #PERMANENT_INJURY_THRESHOLD}) and {@code useEdge} is {@code true}, a second roll is made and
     * its result replaces the original.</p>
     *
     * @param doctor      the person performing the surgery check
     * @param campaign    the {@link Campaign} context
     * @param modifiers   the list of modifiers applied to the surgery roll
     * @param miscPenalty the combined penalty for this healing attempt
     * @param useEdge     {@code true} if medical Edge may be used to reroll a potentially permanent injury;
     *                    {@code false} otherwise
     *
     * @return the final margin of success after any Edge reroll
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getMarginOfSuccessForHealing(Person doctor, Campaign campaign,
          List<TargetRollModifier> modifiers, int miscPenalty, boolean useEdge) {
        SkillCheck skillCheck = doctor.checkSkill(S_SURGERY, campaign)
                                      .withMiscModifier(miscPenalty)
                                      .withExternalModifiers(modifiers);
        ActionCheckResult actionCheckResult = skillCheck.resolve(false, getTextAt(RESOURCE_BUNDLE,
              "AdvancedMedicalAlternateHealing.assistedHealing.normal"));

        // Edge
        if (actionCheckResult.marginOfSuccess() <= PERMANENT_INJURY_THRESHOLD &&
                  useEdge &&
                  doctor.getCurrentEdge() > 0) { // Permanent injury
            // manually update edge because if we pass useEdge == true, the doctor will get one free roll
            doctor.spendEdge();
            actionCheckResult = skillCheck.resolve(false, getTextAt(RESOURCE_BUNDLE,
                  "AdvancedMedicalAlternateHealing.assistedHealing.edge"));
        }

        return actionCheckResult.marginOfSuccess();
    }

    /**
     * Updates task counters and logs healing outcomes based on the margin of success.
     *
     * <p>On success ({@code marginOfSuccess >= 0}), this method increments the appropriate task counter (for the
     * doctor or the patient) and logs a successful treatment entry. On partial failure, it logs that treatment was
     * unsuccessful and the injury is taking longer to heal. On severe failure, it logs that the injury has become
     * permanent.</p>
     *
     * @param today           the current in-game date
     * @param patient         the person receiving treatment
     * @param doctor          the doctor performing the treatment, or {@code null} if this was an unassisted healing
     *                        attempt
     * @param injury          the injury being resolved
     * @param marginOfSuccess the final margin of success for the healing attempt
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void processTaskAwardsAndPersonnelLogUpdates(LocalDate today, Person patient,
          @Nullable Person doctor, Injury injury, HealingMarginOfSuccessEffects marginOfSuccess) {
        if (marginOfSuccess.isHealed()) {
            if (doctor != null && !Objects.equals(doctor, patient)) {
                doctor.changeNTasks(1);
                PatientLogger.successfullyTreatedAltAdvancedMedical(doctor, patient, today, injury.getName());
            } else {
                patient.changeNTasks(1);
                PatientLogger.successfullyTreatedOwnInjuryAltAdvancedMedical(patient, today, injury.getName());
            }
        }

        if (marginOfSuccess.isPermanent()) {
            MedicalLogger.permanentInjuryAltAdvancedMedical(patient, today, injury.getName());
        }

        if (marginOfSuccess.isHasComplication()) {
            MedicalLogger.medicalComplicationAltAdvancedMedical(patient, today, injury.getName());
        }
    }

    /**
     * Applies the concrete effects of a healing attempt to the patient and injury based on the margin of success.
     *
     * <p>The margin of success is translated into a {@link HealingMarginOfSuccessEffects} instance, which defines
     * fatigue damage, recovery, permanence, and/or additional healing delay. If fatigue is enabled, the patient's
     * fatigue is changed. If the effect indicates recovery, the injury is removed. If the effect indicates permanence,
     * the injury is marked permanent. Otherwise, the configured delay adjusts the injury's remaining healing time.</p>
     *
     * @param campaign        the {@link Campaign} context
     * @param patient         the person undergoing healing
     * @param injury          the injury being updated
     * @param marginOfSuccess the final margin of success for the healing attempt
     *
     * @return the {@link HealingMarginOfSuccessEffects} instance representing the healing outcome
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static HealingMarginOfSuccessEffects processHealingEffects(Campaign campaign, Person patient, Injury injury,
          int marginOfSuccess) {
        boolean isUseKinderHealing = campaign.getCampaignOptions().isUseAlternativeAdvancedMedicalKinderHealing();
        HealingMarginOfSuccessEffects healingEffect = getEffectFromHealingAttempt(marginOfSuccess, isUseKinderHealing);

        // Some healing effects are mutually exclusive. These conditionals are constructed so we bypass illogical
        // values.
        LocalDate today = campaign.getLocalDate();
        if (healingEffect.isHealed()) {
            patient.removeInjury(injury, today);

            if (patient.getInjuries().isEmpty()) {
                if (!(null == patient.getDoctorId()) && patient.getPrisonerStatus().isFreeOrBondsman()) {
                    MedicalLogger.dismissedFromInfirmary(patient, campaign);
                }
                // AAM doesn't use 'days to wait for healing' so we just set it to '1.' If the player toggles AAM off,
                // they will get a free day's worth of healing the next day, but that's not a huge issue.
                patient.setDoctorId(null, 1); // Clear old doctor assignment, if any
            }
        } else {
            if (healingEffect.isDelayed()) {
                injury.changeTime(healingEffect.getHealingDelay(injury.getOriginalTime()));
            } else if (healingEffect.isPermanent()) {
                injury.setPermanent(true);
            }
        }

        // The following effects are universal and can apply even if the original injury is healed
        if (campaign.getCampaignOptions().isUseFatigue()) {
            int fatigueRate = campaign.getCampaignOptions().getFatigueRate();
            patient.changeFatigue(healingEffect.getFatigueDamage() * fatigueRate);
        }

        if (healingEffect.isHasComplication()) {
            patient.addInjury(createMedicalComplicationInjury(campaign, patient));
            if (patient.getTotalInjurySeverity() >= DEATH) {
                patient.changeStatus(campaign, today, MEDICAL_COMPLICATIONS);
            }
        }

        return healingEffect;
    }

    private static Injury createMedicalComplicationInjury(Campaign campaign, Person person) {
        return AlternateInjuries.MEDICAL_COMPLICATION.newInjury(campaign, person, GENERIC, 1);
    }
}
