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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static java.lang.Math.max;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_FIT;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_MEDICAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.HealingMarginOfSuccessEffects.getEffectFromHealingAttempt;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.BODY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PatientLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.AttributeCheckUtility;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;


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
    private static final int PROSTHETIC_PENALTY = 4; // Interstellar Operations page 70

    /**
     * Processes the start-of-day healing for a given patient.
     *
     * <p>This method calculates any SPA-based modifiers, determines whether the healing attempt is assisted or
     * unassisted based on the presence of a doctor, and then performs the appropriate healing checks. It also handles
     * optional fatigue changes and the use of medical Edge.</p>
     *
     * @param today        the current in-game date
     * @param isUseFatigue {@code true} if fatigue effects from healing should be applied; {@code false} otherwise
     * @param patient      the person undergoing healing
     * @param doctor       the doctor providing treatment, or {@code null} if the patient is healing naturally
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processNewDay(LocalDate today, boolean isUseFatigue, Person patient, @Nullable Person doctor) {
        // Modifiers
        List<TargetRollModifier> modifiers = getSPAModifiers(patient);
        Set<BodyLocation> prostheticPenalties = getProstheticPenalties(patient);

        // Healing
        if (doctor == null) {
            boolean patientUsesEdge = patient.getOptions().booleanOption(EDGE_MEDICAL);
            performUnassistedHealingCheck(today, isUseFatigue, patient, modifiers, prostheticPenalties,
                  patientUsesEdge);
        } else {
            boolean doctorUsesEdge = doctor.getOptions().booleanOption(EDGE_MEDICAL);
            performAssistedHealingCheck(today, isUseFatigue, patient, doctor, modifiers, prostheticPenalties,
                  doctorUsesEdge);
        }
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
            if (injury.getSubType().isProsthetic()) {
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

    /**
     * Performs the daily natural (unassisted) healing checks for a patient.
     *
     * <p>This method iterates over all non-permanent injuries, decrements their remaining healing time, and performs
     * an attribute-based healing check when appropriate. Depending on the margin of success, injuries may fully heal,
     * be delayed, or become permanent. If configured, medical Edge can be used to reroll potentially permanent
     * injuries.</p>
     *
     * <p>A defensive copy of the injury list is used because successful healing may remove injuries from the
     * underlying collection.</p>
     *
     * @param today               the current in-game date
     * @param isUseFatigue        {@code true} if fatigue effects from healing should be applied; {@code false}
     *                            otherwise
     * @param patient             the person attempting to heal naturally
     * @param modifiers           the list of SPA-based and other modifiers applied to the natural healing roll
     * @param prostheticPenalties the set of body locations that should incur a prosthetic penalty
     * @param useEdge             {@code true} if the patient is allowed to use medical Edge for rerolls; {@code false}
     *                            otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void performUnassistedHealingCheck(LocalDate today, boolean isUseFatigue, Person patient,
          List<TargetRollModifier> modifiers, Set<BodyLocation> prostheticPenalties, boolean useEdge) {
        // We need a defensive copy of the list as we're going to be removing injuries from it when successfully healing
        for (Injury injury : new ArrayList<>(patient.getInjuries())) {
            if (!injury.isPermanent()) {
                // This needs to be refetched each cycle as the number of concurrent injuries might have changed
                int injuryPenalty = max(0, patient.getTotalInjurySeverity() - patient.getAdjustedToughness());

                injury.changeTime(-1);
                int miscPenalty = getMiscPenalty(injuryPenalty, prostheticPenalties, injury.getLocation());
                int marginOfSuccess = getMarginOfSuccessForUnassistedHealing(patient, modifiers, miscPenalty, useEdge);

                if (injury.getTime() <= 0) { // Time to try and fully heal the injury
                    processHealingEffects(isUseFatigue, patient, injury, marginOfSuccess);
                    processTaskAwardsAndPersonnelLogUpdates(today, patient, null, injury, marginOfSuccess);
                } else if (marginOfSuccess <= -6) { // The injury became permanent
                    injury.setPermanent(true);
                    MedicalLogger.permanentInjuryAltAdvancedMedical(patient, today, injury.getName());
                } else if (marginOfSuccess < 0) { // The injury worsened
                    injury.changeTime(1); // Undo the prior reduction
                }
            }
        }
    }


    /**
     * Calculates the combined penalty applied to a healing roll for a specific injury.
     *
     * <p>The base penalty is the total injury severity for the patient. If the injury is located on a body part
     * represented by a prosthetic and that location is present in {@code prostheticPenalties}, the prosthetic penalty
     * is added.</p>
     *
     * @param injuryPenalty       the total injury severity for the patient
     * @param prostheticPenalties the set of body locations that incur prosthetic penalties
     * @param location            the body location of the injury being healed
     *
     * @return the sum of the base injury penalty and any applicable prosthetic penalty
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getMiscPenalty(int injuryPenalty, Set<BodyLocation> prostheticPenalties, BodyLocation location) {
        int miscPenalty = injuryPenalty;

        BodyLocation primaryLocation = location.getPrimaryLocation();
        if (prostheticPenalties.contains(primaryLocation)) {
            miscPenalty += PROSTHETIC_PENALTY;
        }

        return miscPenalty;
    }

    /**
     * Performs the natural healing roll for an unassisted healing attempt and returns its margin of success.
     *
     * <p>The roll is an attribute check based on the patient's {@link SkillAttribute#BODY} attribute, modified by the
     * provided target roll modifiers and miscellaneous penalties. If the initial result causes the injury to become
     * permanent (margin of success &le; -6) and {@code useEdge} is {@code true}, a second roll is made and its result
     * replaces the original.</p>
     *
     * @param patient     the person attempting to heal naturally
     * @param modifiers   the list of modifiers applied to the healing roll
     * @param miscPenalty the combined penalty for this healing attempt
     * @param useEdge     {@code true} if medical Edge may be used to reroll a potentially permanent injury;
     *                    {@code false} otherwise
     *
     * @return the final margin of success after any Edge reroll
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getMarginOfSuccessForUnassistedHealing(Person patient, List<TargetRollModifier> modifiers,
          int miscPenalty, boolean useEdge) {
        AttributeCheckUtility naturalHealing = new AttributeCheckUtility(patient, BODY, NONE, modifiers, miscPenalty,
              false, true);
        int marginOfSuccess = naturalHealing.getMarginOfSuccess();

        // Edge
        if (marginOfSuccess <= -6 && useEdge) { // Attempt to reroll a permanent injury
            AttributeCheckUtility edgeReroll = new AttributeCheckUtility(patient, BODY, NONE, modifiers, miscPenalty,
                  false, true);
            marginOfSuccess = edgeReroll.getMarginOfSuccess(); // Edge always replaces the original
        }
        return marginOfSuccess;
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
     * @param today               the current in-game date
     * @param isUseFatigue        {@code true} if fatigue effects from healing should be applied; {@code false}
     *                            otherwise
     * @param patient             the person being treated
     * @param doctor              the doctor performing the assisted healing
     * @param modifiers           the list of modifiers applied to the surgery check
     * @param prostheticPenalties the set of body locations that should incur a prosthetic penalty
     * @param useEdge             {@code true} if the doctor is allowed to use medical Edge for rerolls; {@code false}
     *                            otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void performAssistedHealingCheck(LocalDate today, boolean isUseFatigue, Person patient, Person doctor,
          List<TargetRollModifier> modifiers, Set<BodyLocation> prostheticPenalties, boolean useEdge) {
        // We need a defensive copy of the list as we're going to be removing injuries from it when successfully healing
        for (Injury injury : new ArrayList<>(patient.getInjuries())) {
            if (!injury.isPermanent()) {
                // This needs to be refetched each cycle as the number of concurrent injuries might have changed
                int injuryPenalty = max(0, patient.getTotalInjurySeverity() - patient.getAdjustedToughness());

                injury.changeTime(-1);

                if (injury.getTime() <= 0) {
                    int miscPenalty = getMiscPenalty(injuryPenalty, prostheticPenalties, injury.getLocation());
                    int marginOfSuccess = getMarginOfSuccessForAssistedHealing(doctor, modifiers, miscPenalty, useEdge);

                    processHealingEffects(isUseFatigue, patient, injury, marginOfSuccess);
                    processTaskAwardsAndPersonnelLogUpdates(today, patient, doctor, injury, marginOfSuccess);
                }
            }
        }
    }

    /**
     * Performs the assisted healing roll for a doctor and returns its margin of success.
     *
     * <p>The roll is a skill check using the doctor's {@code Surgery} skill, modified by the provided target roll
     * modifiers and miscellaneous penalties. If the initial result causes the injury to become permanent (margin of
     * success &le; -6) and {@code useEdge} is {@code true}, a second roll is made and its result replaces the
     * original.</p>
     *
     * @param doctor      the person performing the surgery check
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
    private static int getMarginOfSuccessForAssistedHealing(Person doctor, List<TargetRollModifier> modifiers,
          int miscPenalty, boolean useEdge) {
        SkillCheckUtility surgery = new SkillCheckUtility(doctor, S_SURGERY, modifiers, miscPenalty, false, true);
        int marginOfSuccess = surgery.getMarginOfSuccess();

        // Edge
        if (marginOfSuccess <= -6 && useEdge) { // Permanent injury
            SkillCheckUtility edgeReroll = new SkillCheckUtility(doctor, S_SURGERY, modifiers, miscPenalty, false,
                  true);
            marginOfSuccess = edgeReroll.getMarginOfSuccess(); // Edge always replaces the original
        }
        return marginOfSuccess;
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
          @Nullable Person doctor, Injury injury, int marginOfSuccess) {
        if (marginOfSuccess >= 0) { // 0+ is a success, the injury will have been removed
            if (doctor != null) {
                doctor.changeNTasks(1);
                PatientLogger.successfullyTreatedAltAdvancedMedical(doctor,
                      patient,
                      today,
                      injury.getName());
            } else {
                patient.changeNTasks(1); // Patient gets credit for their own medical prowess
                PatientLogger.successfullyTreatedOwnInjuryAltAdvancedMedical(patient,
                      today,
                      injury.getName());
            }
        } else if (marginOfSuccess > -6) { // Injury is taking longer to heal
            MedicalLogger.unsuccessfullyTreatedAltAdvancedMedical(patient, today, injury.getName());
        } else { // Injury has become permanent
            MedicalLogger.permanentInjuryAltAdvancedMedical(patient, today, injury.getName());
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
     * @param isUseFatigue    {@code true} if fatigue effects from healing should be applied; {@code false} otherwise
     * @param patient         the person undergoing healing
     * @param injury          the injury being updated
     * @param marginOfSuccess the final margin of success for the healing attempt
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void processHealingEffects(boolean isUseFatigue, Person patient, Injury injury,
          int marginOfSuccess) {
        HealingMarginOfSuccessEffects healingEffect = getEffectFromHealingAttempt(marginOfSuccess);
        if (isUseFatigue) {
            patient.changeFatigue(healingEffect.getFatigueDamage());
        }

        if (healingEffect.isHealed()) {
            patient.removeInjury(injury);

            if (patient.getInjuries().isEmpty()) {
                // AAM doesn't use 'days to wait for healing' so we just set it to '1.' If the player toggles AAM off,
                // they will get a free day's worth of healing the next day, but that's not a huge issue.
                patient.setDoctorId(null, 1); // Clear old doctor assignment, if any
            }

            return;
        }

        if (healingEffect.isPermanent()) {
            injury.setPermanent(true);
            return;
        }

        injury.changeTime(healingEffect.getHealingDelay(injury.getOriginalTime()));
    }
}
