/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedical;

import static mekhq.campaign.personnel.enums.ModifierValue.GUNNERY;
import static mekhq.campaign.personnel.enums.ModifierValue.PILOTING;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.finances.Money;
import mekhq.campaign.log.MedicalLogEntry;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Modifier;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.enums.ModifierValue;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries;

/** Advanced Medical sub-system injury types */
public final class InjuryTypes {
    private static final MMLogger LOGGER = MMLogger.create(InjuryType.class);

    // Predefined types
    public static final InjuryType PUNCTURE = new Puncture();
    public static final InjuryType FRACTURE = new Fracture();
    public static final InjuryType LACERATION = new Laceration();
    public static final InjuryType TORN_MUSCLE = new TornMuscle();
    public static final InjuryType CONCUSSION = new Concussion();
    public static final InjuryType BROKEN_RIB = new BrokenRib();
    public static final InjuryType BRUISED_KIDNEY = new BruisedKidney();
    public static final InjuryType BROKEN_LIMB = new BrokenLimb();
    public static final InjuryType BROKEN_COLLAR_BONE = new BrokenCollarBone();
    public static final InjuryType INTERNAL_BLEEDING = new InternalBleeding();
    public static final InjuryType LOST_LIMB = new LostLimb();
    public static final InjuryType REPLACEMENT_LIMB_RECOVERY = new ReplacementLimbRecovery();
    public static final InjuryType POSTPARTUM_RECOVERY = new PostpartumRecovery();
    public static final InjuryType CEREBRAL_CONTUSION = new CerebralContusion();
    public static final InjuryType PUNCTURED_LUNG = new PuncturedLung();
    public static final InjuryType CTE = new Cte();
    public static final InjuryType BROKEN_BACK = new BrokenBack();
    // New injury types go here (or extend the class)
    public static final InjuryType SEVERED_SPINE = new SeveredSpine();
    public static final InjuryType TRANSIT_DISORIENTATION_SYNDROME = new TransitDisorientationSyndrome();
    public static final InjuryType DISCONTINUATION_SYNDROME = new DiscontinuationSyndrome();
    public static final InjuryType CRIPPLING_FLASHBACKS = new CripplingFlashbacks();
    public static final InjuryType CHILDLIKE_REGRESSION = new ChildlikeRegression();
    public static final InjuryType CATATONIA = new Catatonia();

    // Replacement Limbs
    public static int REPLACEMENT_LIMB_MINIMUM_SKILL_REQUIRED_TYPES_3_4_5 = 5;
    public static Money REPLACEMENT_LIMB_COST_ARM_TYPE_5 = Money.of(200000);
    public static Money REPLACEMENT_LIMB_COST_HAND_TYPE_5 = Money.of(100000);
    public static Money REPLACEMENT_LIMB_COST_LEG_TYPE_5 = Money.of(125000);
    public static Money REPLACEMENT_LIMB_COST_FOOT_TYPE_5 = Money.of(50000);

    private static boolean registered = false;

    /**
     * Register all injury types defined here. Don't use them until you called this once!
     */
    public static synchronized void registerAll() {
        if (!registered) {
            // `am:cut`, `am:bruise`, and `am:sprain` are the old codes for those injury types.
            // We maintain them to avoid breaking compatibility
            InjuryType.register(0, "am:cut", PUNCTURE);
            InjuryType.register(1, "am:bruise", FRACTURE);
            InjuryType.register(2, "am:laceration", LACERATION);
            InjuryType.register(3, "am:sprain", TORN_MUSCLE);
            InjuryType.register(4, "am:concussion", CONCUSSION);
            InjuryType.register(5, "am:broken_rib", BROKEN_RIB);
            InjuryType.register(6, "am:bruised_kidney", BRUISED_KIDNEY);
            InjuryType.register(7, "am:broken_limb", BROKEN_LIMB);
            InjuryType.register(8, "am:broken_collar_bone", BROKEN_COLLAR_BONE);
            InjuryType.register(9, "am:internal_bleeding", INTERNAL_BLEEDING);
            InjuryType.register(10, "am:lost_limb", LOST_LIMB);
            InjuryType.register(11, "am:cerebral_contusion", CEREBRAL_CONTUSION);
            InjuryType.register(12, "am:punctured_lung", PUNCTURED_LUNG);
            InjuryType.register(13, "am:cte", CTE);
            InjuryType.register(14, "am:broken_back", BROKEN_BACK);

            InjuryType.register(15, "alt:SEVERED_HEAD", SEVERED_HEAD);
            InjuryType.register(16, "alt:BURN_FACE", BURN_FACE);
            InjuryType.register(17, "alt:HEARING_LOSS", HEARING_LOSS);
            InjuryType.register(18, "alt:BLINDNESS", BLINDNESS);
            InjuryType.register(19, "alt:FRACTURED_JAW", FRACTURED_JAW);
            InjuryType.register(20, "alt:FRACTURED_SKULL", FRACTURED_SKULL);
            InjuryType.register(21, "alt:BURNED_CHEST", BURNED_CHEST);
            InjuryType.register(22, "alt:FRACTURED_RIB", FRACTURED_RIB);
            InjuryType.register(23, "alt:SMOKE_INHALATION", SMOKE_INHALATION);
            InjuryType.register(24, "alt:PUNCTURED_LUNG", AlternateInjuries.PUNCTURED_LUNG);
            InjuryType.register(25, "alt:HEART_TRAUMA", HEART_TRAUMA);
            InjuryType.register(26, "alt:BURN_ABDOMINAL", BURN_ABDOMINAL);
            InjuryType.register(27, "alt:BRUISED_ORGAN", BRUISED_ORGAN);
            InjuryType.register(28, "alt:ORGAN_TRAUMA", ORGAN_TRAUMA);
            InjuryType.register(29, "alt:FRACTURED_GROIN", FRACTURED_GROIN);
            InjuryType.register(30, "alt:DISEMBOWELED", DISEMBOWELED);
            InjuryType.register(31, "alt:SEVERED_ARM", SEVERED_ARM);
            InjuryType.register(32, "alt:BURN_UPPER_ARM", BURN_UPPER_ARM);
            InjuryType.register(33, "alt:FRACTURED_UPPER_ARM", FRACTURED_UPPER_ARM);
            InjuryType.register(34, "alt:FRACTURED_ELBOW", FRACTURED_ELBOW);
            InjuryType.register(35, "alt:FRACTURED_SHOULDER", FRACTURED_SHOULDER);
            InjuryType.register(36, "alt:COMPOUND_FRACTURED_SHOULDER", COMPOUND_FRACTURED_SHOULDER);
            InjuryType.register(37, "alt:SEVERED_HAND", SEVERED_HAND);
            InjuryType.register(38, "alt:BURN_HAND", BURN_HAND);
            InjuryType.register(39, "alt:FRACTURED_HAND", FRACTURED_HAND);
            InjuryType.register(40, "alt:FRACTURED_WRIST", FRACTURED_WRIST);
            InjuryType.register(41, "alt:FRACTURED_FOREARM", FRACTURED_FOREARM);
            InjuryType.register(42, "alt:COMPOUND_FRACTURED_FOREARM", COMPOUND_FRACTURED_FOREARM);
            InjuryType.register(43, "alt:SEVERED_LEG", SEVERED_LEG);
            InjuryType.register(44, "alt:BURN_THIGH", BURN_THIGH);
            InjuryType.register(45, "alt:BRUISED_FEMUR", BRUISED_FEMUR);
            InjuryType.register(46, "alt:FRACTURED_FEMUR", FRACTURED_FEMUR);
            InjuryType.register(47, "alt:COMPOUND_FRACTURED_FEMUR", COMPOUND_FRACTURED_FEMUR);
            InjuryType.register(48, "alt:FRACTURED_HIP", FRACTURED_HIP);
            InjuryType.register(49, "alt:SEVERED_FOOT", SEVERED_FOOT);
            InjuryType.register(50, "alt:BURN_CALF", BURN_CALF);
            InjuryType.register(51, "alt:FRACTURED_FOOT", FRACTURED_FOOT);
            InjuryType.register(52, "alt:FRACTURED_ANKLE", FRACTURED_ANKLE);
            InjuryType.register(53, "alt:FRACTURED_KNEE", FRACTURED_KNEE);
            InjuryType.register(54, "alt:COMPOUND_FRACTURED_SHIN", COMPOUND_FRACTURED_SHIN);
            InjuryType.register(55, "alt:BLOOD_LOSS", BLOOD_LOSS);
            InjuryType.register(56, "alt:GROWTHS_DISCOMFORT", GROWTHS_DISCOMFORT);
            InjuryType.register(57, "alt:GROWTHS_SLIGHT", GROWTHS_SLIGHT);
            InjuryType.register(58, "alt:GROWTHS_MODERATE", GROWTHS_MODERATE);
            InjuryType.register(59, "alt:GROWTHS_SEVERE", GROWTHS_SEVERE);
            InjuryType.register(60, "alt:GROWTHS_DEADLY", GROWTHS_DEADLY);
            InjuryType.register(61, "alt:INFECTION_DISCOMFORT", INFECTION_DISCOMFORT);
            InjuryType.register(62, "alt:INFECTION_SLIGHT", INFECTION_SLIGHT);
            InjuryType.register(63, "alt:INFECTION_MODERATE", INFECTION_MODERATE);
            InjuryType.register(64, "alt:INFECTION_SEVERE", INFECTION_SEVERE);
            InjuryType.register(65, "alt:INFECTION_DEADLY", INFECTION_DEADLY);
            InjuryType.register(66, "alt:HEARING_DISCOMFORT", HEARING_DISCOMFORT);
            InjuryType.register(67, "alt:HEARING_SLIGHT", HEARING_SLIGHT);
            InjuryType.register(68, "alt:HEARING_MODERATE", HEARING_MODERATE);
            InjuryType.register(69, "alt:HEARING_SEVERE", HEARING_SEVERE);
            InjuryType.register(70, "alt:HEARING_DEADLY", HEARING_DEADLY);
            InjuryType.register(71, "alt:WEAKNESS_DISCOMFORT", WEAKNESS_DISCOMFORT);
            InjuryType.register(72, "alt:WEAKNESS_SLIGHT", WEAKNESS_SLIGHT);
            InjuryType.register(73, "alt:WEAKNESS_MODERATE", WEAKNESS_MODERATE);
            InjuryType.register(74, "alt:WEAKNESS_SEVERE", WEAKNESS_SEVERE);
            InjuryType.register(75, "alt:WEAKNESS_DEADLY", WEAKNESS_DEADLY);
            InjuryType.register(76, "alt:SORES_DISCOMFORT", SORES_DISCOMFORT);
            InjuryType.register(77, "alt:SORES_SLIGHT", SORES_SLIGHT);
            InjuryType.register(78, "alt:SORES_MODERATE", SORES_MODERATE);
            InjuryType.register(79, "alt:SORES_SEVERE", SORES_SEVERE);
            InjuryType.register(80, "alt:SORES_DEADLY", SORES_DEADLY);
            InjuryType.register(81, "alt:FLU_DISCOMFORT", FLU_DISCOMFORT);
            InjuryType.register(82, "alt:FLU_SLIGHT", FLU_SLIGHT);
            InjuryType.register(83, "alt:FLU_MODERATE", FLU_MODERATE);
            InjuryType.register(84, "alt:FLU_SEVERE", FLU_SEVERE);
            InjuryType.register(85, "alt:FLU_DEADLY", FLU_DEADLY);
            InjuryType.register(86, "alt:SIGHT_DISCOMFORT", SIGHT_DISCOMFORT);
            InjuryType.register(87, "alt:SIGHT_SLIGHT", SIGHT_SLIGHT);
            InjuryType.register(88, "alt:SIGHT_MODERATE", SIGHT_MODERATE);
            InjuryType.register(89, "alt:SIGHT_SEVERE", SIGHT_SEVERE);
            InjuryType.register(90, "alt:SIGHT_DEADLY", SIGHT_DEADLY);
            InjuryType.register(91, "alt:TREMORS_DISCOMFORT", TREMORS_DISCOMFORT);
            InjuryType.register(92, "alt:TREMORS_SLIGHT", TREMORS_SLIGHT);
            InjuryType.register(93, "alt:TREMORS_MODERATE", TREMORS_MODERATE);
            InjuryType.register(94, "alt:TREMORS_SEVERE", TREMORS_SEVERE);
            InjuryType.register(95, "alt:TREMORS_DEADLY", TREMORS_DEADLY);
            InjuryType.register(96, "alt:BREATHING_DISCOMFORT", BREATHING_DISCOMFORT);
            InjuryType.register(97, "alt:BREATHING_SLIGHT", BREATHING_SLIGHT);
            InjuryType.register(98, "alt:BREATHING_MODERATE", BREATHING_MODERATE);
            InjuryType.register(99, "alt:BREATHING_SEVERE", BREATHING_SEVERE);
            InjuryType.register(100, "alt:BREATHING_DEADLY", BREATHING_DEADLY);
            InjuryType.register(101, "alt:HEMOPHILIA_DISCOMFORT", HEMOPHILIA_DISCOMFORT);
            InjuryType.register(102, "alt:HEMOPHILIA_SLIGHT", HEMOPHILIA_SLIGHT);
            InjuryType.register(103, "alt:HEMOPHILIA_MODERATE", HEMOPHILIA_MODERATE);
            InjuryType.register(104, "alt:HEMOPHILIA_SEVERE", HEMOPHILIA_SEVERE);
            InjuryType.register(105, "alt:HEMOPHILIA_DEADLY", HEMOPHILIA_DEADLY);
            InjuryType.register(106, "alt:VENEREAL_DISCOMFORT", VENEREAL_DISCOMFORT);
            InjuryType.register(107, "alt:VENEREAL_SLIGHT", VENEREAL_SLIGHT);
            InjuryType.register(108, "alt:VENEREAL_MODERATE", VENEREAL_MODERATE);
            InjuryType.register(109, "alt:VENEREAL_SEVERE", VENEREAL_SEVERE);
            InjuryType.register(110, "alt:VENEREAL_DEADLY", VENEREAL_DEADLY);
            InjuryType.register(111, "alt:WOODEN_ARM", WOODEN_ARM);
            InjuryType.register(112, "alt:HOOK_HAND", HOOK_HAND);
            InjuryType.register(113, "alt:PEG_LEG", PEG_LEG);
            InjuryType.register(114, "alt:WOODEN_FOOT", WOODEN_FOOT);
            InjuryType.register(115, "alt:SIMPLE_ARM", SIMPLE_ARM);
            InjuryType.register(116, "alt:SIMPLE_CLAW_HAND", SIMPLE_CLAW_HAND);
            InjuryType.register(117, "alt:SIMPLE_LEG", SIMPLE_LEG);
            InjuryType.register(118, "alt:SIMPLE_FOOT", SIMPLE_FOOT);
            InjuryType.register(119, "alt:PROSTHETIC_ARM", PROSTHETIC_ARM);
            InjuryType.register(120, "alt:PROSTHETIC_HAND", PROSTHETIC_HAND);
            InjuryType.register(121, "alt:PROSTHETIC_LEG", PROSTHETIC_LEG);
            InjuryType.register(122, "alt:PROSTHETIC_FOOT", PROSTHETIC_FOOT);
            InjuryType.register(123, "alt:ADVANCED_PROSTHETIC_ARM", ADVANCED_PROSTHETIC_ARM);
            InjuryType.register(124, "alt:ADVANCED_PROSTHETIC_HAND", ADVANCED_PROSTHETIC_HAND);
            InjuryType.register(125, "alt:ADVANCED_PROSTHETIC_LEG", ADVANCED_PROSTHETIC_LEG);
            InjuryType.register(126, "alt:ADVANCED_PROSTHETIC_FOOT", ADVANCED_PROSTHETIC_FOOT);
            InjuryType.register(127, "alt:MYOMER_ARM", MYOMER_ARM);
            InjuryType.register(128, "alt:MYOMER_HAND", MYOMER_HAND);
            InjuryType.register(129, "alt:MYOMER_LEG", MYOMER_LEG);
            InjuryType.register(130, "alt:MYOMER_FOOT", MYOMER_FOOT);
            InjuryType.register(131, "alt:CLONED_ARM", CLONED_ARM);
            InjuryType.register(132, "alt:CLONED_HAND", CLONED_HAND);
            InjuryType.register(133, "alt:CLONED_LEG", CLONED_LEG);
            InjuryType.register(134, "alt:CLONED_FOOT", CLONED_FOOT);
            InjuryType.register(135, "alt:EYE_IMPLANT", EYE_IMPLANT);
            InjuryType.register(136, "alt:BIONIC_EAR", BIONIC_EAR);
            InjuryType.register(137, "alt:BIONIC_EYE", BIONIC_EYE);
            InjuryType.register(138, "alt:BIONIC_HEART", BIONIC_HEART);
            InjuryType.register(139, "alt:BIONIC_LUNGS", BIONIC_LUNGS);
            InjuryType.register(140, "alt:BIONIC_ORGAN_OTHER", BIONIC_ORGAN_OTHER);
            InjuryType.register(141, "alt:COSMETIC_SURGERY", COSMETIC_SURGERY);
            InjuryType.register(142, "alt:CLONED_LIMB_RECOVERY", CLONED_LIMB_RECOVERY);
            InjuryType.register(143, "alt:REPLACEMENT_LIMB_RECOVERY", AlternateInjuries.REPLACEMENT_LIMB_RECOVERY);
            InjuryType.register(144, "alt:COSMETIC_SURGERY_RECOVERY", COSMETIC_SURGERY_RECOVERY);
            InjuryType.register(145, "alt:REPLACEMENT_ORGAN_RECOVERY", REPLACEMENT_ORGAN_RECOVERY);
            InjuryType.register(146, "alt:FAILED_SURGERY_RECOVERY", FAILED_SURGERY_RECOVERY);
            InjuryType.register(147, "alt:ELECTIVE_MYOMER_ARM", ELECTIVE_MYOMER_ARM);
            InjuryType.register(148, "alt:ELECTIVE_MYOMER_HAND", ELECTIVE_MYOMER_HAND);
            InjuryType.register(149, "alt:ELECTIVE_MYOMER_LEG", ELECTIVE_MYOMER_LEG);
            InjuryType.register(150, "alt:ENHANCED_IMAGING", ENHANCED_IMAGING_IMPLANT);
            InjuryType.register(151, "alt:ELECTIVE_IMPLANT_RECOVERY", ELECTIVE_IMPLANT_RECOVERY);
            InjuryType.register(152, "alt:EI_IMPLANT_RECOVERY", EI_IMPLANT_RECOVERY);
            InjuryType.register(153, "alt:BONE_REINFORCEMENT", BONE_REINFORCEMENT);
            InjuryType.register(154, "alt:LIVER_FILTRATION_IMPLANT", LIVER_FILTRATION_IMPLANT);
            InjuryType.register(155, "alt:BIONIC_LUNGS_WITH_TYPE_1_FILTER", BIONIC_LUNGS_WITH_TYPE_1_FILTER);
            InjuryType.register(156, "alt:BIONIC_LUNGS_WITH_TYPE_2_FILTER", BIONIC_LUNGS_WITH_TYPE_2_FILTER);
            InjuryType.register(157, "alt:BIONIC_LUNGS_WITH_TYPE_3_FILTER", BIONIC_LUNGS_WITH_TYPE_3_FILTER);
            InjuryType.register(158, "alt:CYBERNETIC_EYE_EM_IR", CYBERNETIC_EYE_EM_IR);
            InjuryType.register(159, "alt:CYBERNETIC_EYE_TELESCOPE", CYBERNETIC_EYE_TELESCOPE);
            InjuryType.register(160, "alt:CYBERNETIC_EYE_LASER", CYBERNETIC_EYE_LASER);
            InjuryType.register(161, "alt:CYBERNETIC_EYE_MULTI", CYBERNETIC_EYE_MULTI);
            InjuryType.register(162, "alt:CYBERNETIC_EYE_MULTI_ENHANCED", CYBERNETIC_EYE_MULTI_ENHANCED);
            InjuryType.register(163, "alt:CYBERNETIC_EAR_SIGNAL", CYBERNETIC_EAR_SIGNAL);
            InjuryType.register(164, "alt:CYBERNETIC_EAR_MULTI", CYBERNETIC_EAR_MULTI);
            InjuryType.register(165, "alt:CYBERNETIC_SPEECH_IMPLANT", CYBERNETIC_SPEECH_IMPLANT);
            InjuryType.register(166, "alt:PHEROMONE_EFFUSER", PHEROMONE_EFFUSER);
            InjuryType.register(167, "alt:COSMETIC_BEAUTY_ENHANCEMENT", COSMETIC_BEAUTY_ENHANCEMENT);
            InjuryType.register(168, "alt:COSMETIC_HORROR_ENHANCEMENT", COSMETIC_HORROR_ENHANCEMENT);
            InjuryType.register(169, "alt:COSMETIC_TAIL_PROSTHETIC", COSMETIC_TAIL_PROSTHETIC);
            InjuryType.register(170, "alt:COSMETIC_ANIMAL_EAR_PROSTHETIC", COSMETIC_ANIMAL_EAR_PROSTHETIC);
            InjuryType.register(171, "alt:COSMETIC_ANIMAL_LEG_PROSTHETIC", COSMETIC_ANIMAL_LEG_PROSTHETIC);

            InjuryType.register("am:severed_spine", SEVERED_SPINE);
            InjuryType.register("am:replacement_limb_recovery", REPLACEMENT_LIMB_RECOVERY);
            InjuryType.register("am:Postpartum_Recovery", POSTPARTUM_RECOVERY);
            InjuryType.register("am:Transit_Disorientation_Syndrome", TRANSIT_DISORIENTATION_SYNDROME);
            InjuryType.register("am:DiscontinuationSyndrome", DISCONTINUATION_SYNDROME);
            InjuryType.register("am:Crippling_Flashbacks", CRIPPLING_FLASHBACKS);
            InjuryType.register("am:Childlike_Regression", CHILDLIKE_REGRESSION);
            InjuryType.register("am:Catatonia", CATATONIA);
            registered = true;
        }
    }

    private static class AMInjuryType extends InjuryType {
        protected int modifyInjuryTime(final Campaign campaign, final Person person, final int time) {
            // Randomize healing time
            int mod = 100;
            int rand = Compute.randomInt(100);
            if (rand < 5) {
                mod += (Compute.d6() < 4) ? rand : -rand;
            }
            return (int) Math.round((time * mod * person.getAbilityTimeModifier(campaign)) / 10000.0);
        }

        @Override
        public Injury newInjury(Campaign campaign, Person person, BodyLocation bodyLocation, int severity) {
            Injury result = super.newInjury(campaign, person, bodyLocation, severity);
            final int time = modifyInjuryTime(campaign, person, result.getOriginalTime());
            result.setOriginalTime(time);
            result.setTime(time);
            return result;
        }
    }

    public static final class SeveredSpine extends AMInjuryType {
        public SeveredSpine() {
            recoveryTime = 180;
            allowedLocations = EnumSet.of(BodyLocation.CHEST, BodyLocation.ABDOMEN);
            permanent = true;
            fluffText = "A severed spine";
            simpleName = "severed spine";
            level = InjuryLevel.CHRONIC;
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "A severed spine in " + ((loc == BodyLocation.CHEST) ? "upper" : "lower") + " body";
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                  Integer.MAX_VALUE,
                  null,
                  InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class BrokenBack extends AMInjuryType {
        public BrokenBack() {
            recoveryTime = 150;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken back";
            simpleName = "broken back";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(new GameEffect(
                  "20% chance of severing the spine, permanently paralyzing the character",
                  rnd -> {
                      if (rnd.applyAsInt(100) < 20) {
                          Injury severedSpine = SEVERED_SPINE.newInjury(c, p, BodyLocation.CHEST, 1);
                          p.addInjury(severedSpine);

                          MedicalLogEntry entry = MedicalLogger.severedSpine(p, c.getLocalDate());
                          LOGGER.info(entry.toString());
                      }
                  }));
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(new Modifier(ModifierValue.GUNNERY, 3, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(ModifierValue.PILOTING, 3, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class Cte extends AMInjuryType {
        public Cte() {
            recoveryTime = 180;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            permanent = true;
            fluffText = "Chronic traumatic encephalopathy";
            simpleName = "CTE";
            level = InjuryLevel.DEADLY;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            int deathChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
            if (hits > 4) {
                return Collections.singletonList(new GameEffect("certain death", rnd -> {
                    p.changeStatus(c, c.getLocalDate(), PersonnelStatus.WOUNDS);
                    MedicalLogEntry entry = MedicalLogger.diedDueToBrainTrauma(p, c.getLocalDate());
                    LOGGER.info(entry.toString());
                }));
            } else {
                // We have a chance!
                return Arrays.asList(newResetRecoveryTimeAction(i),
                      new GameEffect(deathChance + "% chance of death", rnd -> {
                          if (rnd.applyAsInt(6) + hits >= 5) {
                              p.changeStatus(c, c.getLocalDate(), PersonnelStatus.WOUNDS);
                              MedicalLogEntry entry = MedicalLogger.diedDueToBrainTrauma(p, c.getLocalDate());
                              LOGGER.info(entry.toString());
                          }
                      }));
            }
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                  Integer.MAX_VALUE,
                  null,
                  InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class PuncturedLung extends AMInjuryType {
        public PuncturedLung() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A punctured lung";
            simpleName = "punctured lung";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(newResetRecoveryTimeAction(i));
        }
    }

    public static final class CerebralContusion extends AMInjuryType {
        public CerebralContusion() {
            recoveryTime = 90;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            fluffText = "A cerebral contusion";
            simpleName = "cerebral contusion";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            String secondEffectFluff = "development of a chronic traumatic encephalopathy";
            if (hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            return Arrays.asList(newResetRecoveryTimeAction(i), new GameEffect(secondEffectFluff, rnd -> {
                if (rnd.applyAsInt(6) + hits >= 5) {
                    Injury cte = CTE.newInjury(c, p, BodyLocation.HEAD, 1);
                    p.addInjury(cte);
                    p.removeInjury(i);
                    MedicalLogEntry entry = MedicalLogger.developedEncephalopathy(p, c.getLocalDate());
                    LOGGER.info(entry.toString());
                }
            }));
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Collections.singletonList(new Modifier(ModifierValue.PILOTING, 2, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class LostLimb extends AMInjuryType {
        public LostLimb() {
            recoveryTime = 28;
            permanent = true;
            simpleName = "lost";
            level = InjuryLevel.CHRONIC;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb();
        }

        @Override
        public boolean impliesMissingLocation() {
            return true;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Missing " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "Lost " + GenderDescriptors.HIS_HER_THEIR.getDescriptor(gender) + ' ' + loc.locationName();
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            return switch (loc) {
                case LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND ->
                      Collections.singletonList(new Modifier(ModifierValue.GUNNERY,
                            3,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                case LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT ->
                      Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                            3,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                default -> Collections.emptyList();
            };
        }
    }

    public static final class ReplacementLimbRecovery extends AMInjuryType {
        public ReplacementLimbRecovery() {
            recoveryTime = 42;
            permanent = false;
            simpleName = "Replacement Limb Recovery";
            level = InjuryLevel.CHRONIC;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb();
        }

        @Override
        public boolean impliesMissingLocation() {
            return true;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return String.format("Replacement %s Recovery", loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "Replaced " + GenderDescriptors.HIS_HER_THEIR.getDescriptor(gender) + ' ' + loc.locationName();
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            return switch (loc) {
                case LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND ->
                      Collections.singletonList(new Modifier(ModifierValue.GUNNERY,
                            6,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                case LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT ->
                      Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                            6,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                default -> Collections.emptyList();
            };
        }
    }

    public static final class InternalBleeding extends AMInjuryType {
        public InternalBleeding() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.ABDOMEN, BodyLocation.INTERNAL);
            maxSeverity = 3;
            simpleName = "internal bleeding";
        }

        @Override
        public int getRecoveryTime(int severity) {
            return 20 * severity;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return Utilities.capitalize(getFluffText(loc, severity, Gender.MALE));
        }

        @Override
        public InjuryLevel getLevel(Injury i) {
            return (i.getHits() > 2) ? InjuryLevel.DEADLY : InjuryLevel.MAJOR;
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return switch (severity) {
                case 2 -> "Severe internal bleeding";
                case 3 -> "Critical internal bleeding";
                default -> "Internal bleeding";
            };
        }

        @Override
        public String getSimpleName(int severity) {
            return switch (severity) {
                case 2 -> "internal bleeding (severe)";
                case 3 -> "internal bleeding (critical)";
                default -> "internal bleeding";
            };
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            String secondEffectFluff = (i.getHits() < 3) ? "internal bleeding worsening" : "death";
            if (hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            if (hits >= 5 && i.getHits() >= 3) {
                // Don't even bother doing anything else; we're dead
                return Collections.singletonList(new GameEffect("certain death", rnd -> {
                    p.changeStatus(c, c.getLocalDate(), PersonnelStatus.WOUNDS);
                    MedicalLogEntry entry = MedicalLogger.diedOfInternalBleeding(p, c.getLocalDate());
                    LOGGER.info(entry.toString());
                }));
            } else {
                // We have a chance!
                return Arrays.asList(newResetRecoveryTimeAction(i), new GameEffect(secondEffectFluff, rnd -> {
                    if (rnd.applyAsInt(6) + hits >= 5) {
                        if (i.getHits() < 3) {
                            i.setHits(i.getHits() + 1);
                            MedicalLogEntry entry = MedicalLogger.internalBleedingWorsened(p, c.getLocalDate());
                            LOGGER.info(entry.toString());
                        } else {
                            p.changeStatus(c, c.getLocalDate(), PersonnelStatus.WOUNDS);
                            MedicalLogEntry entry = MedicalLogger.diedOfInternalBleeding(p, c.getLocalDate());
                            LOGGER.info(entry.toString());
                        }
                    }
                }));
            }
        }
    }

    public static final class BrokenCollarBone extends AMInjuryType {
        public BrokenCollarBone() {
            recoveryTime = 22;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken collar bone";
            simpleName = "broken collar bone";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(newResetRecoveryTimeAction(i));
        }
    }

    public static final class BrokenLimb extends AMInjuryType {
        public BrokenLimb() {
            recoveryTime = 30;
            simpleName = "broken";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb();
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Broken " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "A broken " + loc.locationName();
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(newResetRecoveryTimeAction(i));
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            return switch (loc) {
                case LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND ->
                      Collections.singletonList(new Modifier(ModifierValue.GUNNERY,
                            inj.isPermanent() ? 1 : 2,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                case LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT ->
                      Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                            inj.isPermanent() ? 1 : 2,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                default -> Collections.emptyList();
            };
        }
    }

    public static final class BruisedKidney extends AMInjuryType {
        public BruisedKidney() {
            recoveryTime = 10;
            allowedLocations = EnumSet.of(BodyLocation.ABDOMEN);
            fluffText = "A bruised kidney";
            simpleName = "bruised kidney";
            level = InjuryLevel.MINOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(new GameEffect("10% chance of internal bleeding", rnd -> {
                if (rnd.applyAsInt(100) < 10) {
                    Injury bleeding = INTERNAL_BLEEDING.newInjury(c, p, BodyLocation.ABDOMEN, 1);
                    p.addInjury(bleeding);
                    MedicalLogEntry entry = MedicalLogger.brokenRibPuncture(p, c.getLocalDate());
                    LOGGER.info(entry.toString());
                }
            }));
        }
    }

    public static final class PostpartumRecovery extends AMInjuryType {
        public PostpartumRecovery() {
            recoveryTime = 10;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Postpartum recovery";
            simpleName = "postpartum recovery";
            level = InjuryLevel.MINOR;
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return List.of(new Modifier(GUNNERY, 1, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(PILOTING, 1, null, InjuryType.MOD_TAG_INJURY));
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(new GameEffect("10% chance of internal bleeding", rnd -> {
                if (rnd.applyAsInt(100) < 10) {
                    Injury bleeding = INTERNAL_BLEEDING.newInjury(c, p, BodyLocation.ABDOMEN, 1);
                    p.addInjury(bleeding);
                    MedicalLogEntry entry = MedicalLogger.brokenRibPuncture(p, c.getLocalDate());
                    LOGGER.info(entry.toString());
                }
            }));
        }
    }

    public static final class BrokenRib extends AMInjuryType {
        public BrokenRib() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken rib";
            simpleName = "broken rib";
            level = InjuryLevel.MAJOR;
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Collections.singletonList(new GameEffect("1% chance of death; 9% chance of puncturing a lung",
                  rnd -> {
                      int rib = rnd.applyAsInt(100);
                      if (rib < 1) {
                          p.changeStatus(c, c.getLocalDate(), PersonnelStatus.WOUNDS);
                          MedicalLogEntry entry = MedicalLogger.brokenRibPunctureDead(p, c.getLocalDate());
                          LOGGER.info(entry.toString());
                      } else if (rib < 10) {
                          Injury puncturedLung = PUNCTURED_LUNG.newInjury(c, p, BodyLocation.CHEST, 1);
                          p.addInjury(puncturedLung);
                          MedicalLogEntry entry = MedicalLogger.brokenRibPuncture(p, c.getLocalDate());
                          LOGGER.info(entry.toString());
                      }
                  }));
        }
    }

    public static final class Concussion extends AMInjuryType {
        public Concussion() {
            recoveryTime = 14;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            maxSeverity = 2;
            fluffText = "A concussion";
        }

        @Override
        public int getRecoveryTime(int severity) {
            return severity >= 2 ? 42 : 14;
        }

        @Override
        public InjuryLevel getLevel(Injury i) {
            return (i.getHits() > 1) ? InjuryLevel.MAJOR : InjuryLevel.MINOR;
        }

        @Override
        public String getSimpleName(int severity) {
            return ((severity == 1) ? "concussion" : "concussion (severe)");
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            String secondEffectFluff = (i.getHits() == 1) ?
                                             "concussion worsening" :
                                             "development of a cerebral contusion";
            if (hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            return Arrays.asList(newResetRecoveryTimeAction(i), new GameEffect(secondEffectFluff, rnd -> {
                if (rnd.applyAsInt(6) + hits >= 5) {
                    if (i.getHits() == 1) {
                        i.setHits(2);
                        MedicalLogEntry entry = MedicalLogger.concussionWorsened(p, c.getLocalDate());
                        LOGGER.info(entry.toString());
                    } else {
                        Injury cerebralContusion = CEREBRAL_CONTUSION.newInjury(c, p, BodyLocation.HEAD, 1);
                        p.addInjury(cerebralContusion);
                        p.removeInjury(i);
                        MedicalLogEntry entry = MedicalLogger.developedCerebralContusion(p, c.getLocalDate());
                        LOGGER.info(entry.toString());
                    }
                }
            }));
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Collections.singletonList(new Modifier(ModifierValue.PILOTING, 1, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class TornMuscle extends AMInjuryType {
        public TornMuscle() {
            recoveryTime = 12;
            simpleName = "torn muscle";
            level = InjuryLevel.MINOR;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb();
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Torn " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "A torn muscle in " +
                         GenderDescriptors.HIS_HER_THEIR.getDescriptor(gender) +
                         ' ' +
                         loc.locationName();
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            return switch (loc) {
                case LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND ->
                      Collections.singletonList(new Modifier(ModifierValue.GUNNERY,
                            1,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                case LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT ->
                      Collections.singletonList(new Modifier(ModifierValue.PILOTING,
                            1,
                            null,
                            InjuryType.MOD_TAG_INJURY));
                default -> Collections.emptyList();
            };
        }
    }

    public static final class Laceration extends AMInjuryType {
        public Laceration() {
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            simpleName = "laceration";
            level = InjuryLevel.MINOR;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Lacerated " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "A laceration on " + GenderDescriptors.HIS_HER_THEIR.getDescriptor(gender) + " head";
        }

        @Override
        protected int modifyInjuryTime(final Campaign campaign, final Person person, final int time) {
            return super.modifyInjuryTime(campaign, person, time + Compute.d6());
        }
    }

    public static final class Fracture extends AMInjuryType {
        public Fracture() {
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            simpleName = "fracture";
            level = InjuryLevel.MINOR;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb() || super.isValidInLocation(loc);
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Fractured " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "A fractured " + loc.locationName();
        }

        @Override
        protected int modifyInjuryTime(final Campaign campaign, final Person person, final int time) {
            return super.modifyInjuryTime(campaign, person, time + Compute.d6());
        }
    }

    public static final class Puncture extends AMInjuryType {
        public Puncture() {
            allowedLocations = EnumSet.of(BodyLocation.CHEST, BodyLocation.ABDOMEN);
            simpleName = "puncture";
            level = InjuryLevel.MINOR;
        }

        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb() || super.isValidInLocation(loc);
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Punctured " + Utilities.capitalize(loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return "Puncture wound to the " + loc.locationName();
        }

        @Override
        protected int modifyInjuryTime(final Campaign campaign, final Person person, final int time) {
            return super.modifyInjuryTime(campaign, person, time + Compute.d6());
        }
    }

    public static final class TransitDisorientationSyndrome extends AMInjuryType {
        public TransitDisorientationSyndrome() {
            recoveryTime = 2;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Transit Disorientation Syndrome";
            simpleName = "Transit Disorientation Syndrome";
            maxSeverity = 2;
            level = InjuryLevel.MINOR;
        }

        @Override
        public int getRecoveryTime(int severity) {
            return severity >= 2 ? 3 : 2;
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            if (severity == 2) {
                return "Severe " + fluffText;
            }
            return fluffText;
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return List.of(new Modifier(GUNNERY, 1, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(PILOTING, 1, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class CripplingFlashbacks extends AMInjuryType {
        public CripplingFlashbacks() {
            recoveryTime = 7;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Crippling Flashbacks";
            simpleName = "Crippling Flashbacks";
            maxSeverity = 1;
            level = InjuryLevel.MINOR;
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return List.of(new Modifier(GUNNERY, 1, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(PILOTING, 1, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class DiscontinuationSyndrome extends AMInjuryType {
        public DiscontinuationSyndrome() {
            recoveryTime = 7;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Discontinuation Syndrome";
            simpleName = "Discontinuation Syndrome";
            maxSeverity = 1;
        }
    }

    public static final class ChildlikeRegression extends AMInjuryType {
        public ChildlikeRegression() {
            recoveryTime = 7;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Childlike Regression";
            simpleName = "Childlike Regression";
            maxSeverity = 1;
            level = InjuryLevel.MINOR;
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return List.of(new Modifier(GUNNERY, 4, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(PILOTING, 4, null, InjuryType.MOD_TAG_INJURY));
        }
    }

    public static final class Catatonia extends AMInjuryType {
        public Catatonia() {
            recoveryTime = 7;
            allowedLocations = EnumSet.of(BodyLocation.INTERNAL);
            fluffText = "Catatonia";
            simpleName = "Catatonia";
            maxSeverity = 1;
            level = InjuryLevel.MINOR;
        }

        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return List.of(new Modifier(GUNNERY, 20, null, InjuryType.MOD_TAG_INJURY),
                  new Modifier(PILOTING, 20, null, InjuryType.MOD_TAG_INJURY));
        }
    }
}
