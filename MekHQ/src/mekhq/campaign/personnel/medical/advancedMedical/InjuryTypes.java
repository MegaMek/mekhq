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

            InjuryType.register(15, "alt:SEVERED_HEAD", AlternateInjuries.SEVERED_HEAD);
            InjuryType.register(16, "alt:BURN_FACE", AlternateInjuries.BURN_FACE);
            InjuryType.register(17, "alt:HEARING_LOSS", AlternateInjuries.HEARING_LOSS);
            InjuryType.register(18, "alt:BLINDNESS", AlternateInjuries.BLINDNESS);
            InjuryType.register(19, "alt:FRACTURED_JAW", AlternateInjuries.FRACTURED_JAW);
            InjuryType.register(20, "alt:FRACTURED_SKULL", AlternateInjuries.FRACTURED_SKULL);
            InjuryType.register(21, "alt:BURNED_CHEST", AlternateInjuries.BURNED_CHEST);
            InjuryType.register(22, "alt:FRACTURED_RIB", AlternateInjuries.FRACTURED_RIB);
            InjuryType.register(23, "alt:SMOKE_INHALATION", AlternateInjuries.SMOKE_INHALATION);
            InjuryType.register(24, "alt:PUNCTURED_LUNG", AlternateInjuries.PUNCTURED_LUNG);
            InjuryType.register(25, "alt:HEART_TRAUMA", AlternateInjuries.HEART_TRAUMA);
            InjuryType.register(26, "alt:BURN_ABDOMINAL", AlternateInjuries.BURN_ABDOMINAL);
            InjuryType.register(27, "alt:BRUISED_ORGAN", AlternateInjuries.BRUISED_ORGAN);
            InjuryType.register(28, "alt:ORGAN_TRAUMA", AlternateInjuries.ORGAN_TRAUMA);
            InjuryType.register(29, "alt:FRACTURED_GROIN", AlternateInjuries.FRACTURED_GROIN);
            InjuryType.register(30, "alt:DISEMBOWELED", AlternateInjuries.DISEMBOWELED);
            InjuryType.register(31, "alt:SEVERED_ARM", AlternateInjuries.SEVERED_ARM);
            InjuryType.register(32, "alt:BURN_UPPER_ARM", AlternateInjuries.BURN_UPPER_ARM);
            InjuryType.register(33, "alt:FRACTURED_UPPER_ARM", AlternateInjuries.FRACTURED_UPPER_ARM);
            InjuryType.register(34, "alt:FRACTURED_ELBOW", AlternateInjuries.FRACTURED_ELBOW);
            InjuryType.register(35, "alt:FRACTURED_SHOULDER", AlternateInjuries.FRACTURED_SHOULDER);
            InjuryType.register(36, "alt:COMPOUND_FRACTURED_SHOULDER", AlternateInjuries.COMPOUND_FRACTURED_SHOULDER);
            InjuryType.register(37, "alt:SEVERED_HAND", AlternateInjuries.SEVERED_HAND);
            InjuryType.register(38, "alt:BURN_HAND", AlternateInjuries.BURN_HAND);
            InjuryType.register(39, "alt:FRACTURED_HAND", AlternateInjuries.FRACTURED_HAND);
            InjuryType.register(40, "alt:FRACTURED_WRIST", AlternateInjuries.FRACTURED_WRIST);
            InjuryType.register(41, "alt:FRACTURED_FOREARM", AlternateInjuries.FRACTURED_FOREARM);
            InjuryType.register(42, "alt:COMPOUND_FRACTURED_FOREARM", AlternateInjuries.COMPOUND_FRACTURED_FOREARM);
            InjuryType.register(43, "alt:SEVERED_LEG", AlternateInjuries.SEVERED_LEG);
            InjuryType.register(44, "alt:BURN_THIGH", AlternateInjuries.BURN_THIGH);
            InjuryType.register(45, "alt:BRUISED_FEMUR", AlternateInjuries.BRUISED_FEMUR);
            InjuryType.register(46, "alt:FRACTURED_FEMUR", AlternateInjuries.FRACTURED_FEMUR);
            InjuryType.register(47, "alt:COMPOUND_FRACTURED_FEMUR", AlternateInjuries.COMPOUND_FRACTURED_FEMUR);
            InjuryType.register(48, "alt:FRACTURED_HIP", AlternateInjuries.FRACTURED_HIP);
            InjuryType.register(49, "alt:SEVERED_FOOT", AlternateInjuries.SEVERED_FOOT);
            InjuryType.register(50, "alt:BURN_CALF", AlternateInjuries.BURN_CALF);
            InjuryType.register(51, "alt:FRACTURED_FOOT", AlternateInjuries.FRACTURED_FOOT);
            InjuryType.register(52, "alt:FRACTURED_ANKLE", AlternateInjuries.FRACTURED_ANKLE);
            InjuryType.register(53, "alt:FRACTURED_KNEE", AlternateInjuries.FRACTURED_KNEE);
            InjuryType.register(54, "alt:COMPOUND_FRACTURED_SHIN", AlternateInjuries.COMPOUND_FRACTURED_SHIN);
            InjuryType.register(55, "alt:BLOOD_LOSS", AlternateInjuries.BLOOD_LOSS);
            InjuryType.register(56, "alt:GROWTHS_DISCOMFORT", AlternateInjuries.GROWTHS_DISCOMFORT);
            InjuryType.register(57, "alt:GROWTHS_SLIGHT", AlternateInjuries.GROWTHS_SLIGHT);
            InjuryType.register(58, "alt:GROWTHS_MODERATE", AlternateInjuries.GROWTHS_MODERATE);
            InjuryType.register(59, "alt:GROWTHS_SEVERE", AlternateInjuries.GROWTHS_SEVERE);
            InjuryType.register(60, "alt:GROWTHS_DEADLY", AlternateInjuries.GROWTHS_DEADLY);
            InjuryType.register(61, "alt:INFECTION_DISCOMFORT", AlternateInjuries.INFECTION_DISCOMFORT);
            InjuryType.register(62, "alt:INFECTION_SLIGHT", AlternateInjuries.INFECTION_SLIGHT);
            InjuryType.register(63, "alt:INFECTION_MODERATE", AlternateInjuries.INFECTION_MODERATE);
            InjuryType.register(64, "alt:INFECTION_SEVERE", AlternateInjuries.INFECTION_SEVERE);
            InjuryType.register(65, "alt:INFECTION_DEADLY", AlternateInjuries.INFECTION_DEADLY);
            InjuryType.register(66, "alt:HEARING_DISCOMFORT", AlternateInjuries.HEARING_DISCOMFORT);
            InjuryType.register(67, "alt:HEARING_SLIGHT", AlternateInjuries.HEARING_SLIGHT);
            InjuryType.register(68, "alt:HEARING_MODERATE", AlternateInjuries.HEARING_MODERATE);
            InjuryType.register(69, "alt:HEARING_SEVERE", AlternateInjuries.HEARING_SEVERE);
            InjuryType.register(70, "alt:HEARING_DEADLY", AlternateInjuries.HEARING_DEADLY);
            InjuryType.register(71, "alt:WEAKNESS_DISCOMFORT", AlternateInjuries.WEAKNESS_DISCOMFORT);
            InjuryType.register(72, "alt:WEAKNESS_SLIGHT", AlternateInjuries.WEAKNESS_SLIGHT);
            InjuryType.register(73, "alt:WEAKNESS_MODERATE", AlternateInjuries.WEAKNESS_MODERATE);
            InjuryType.register(74, "alt:WEAKNESS_SEVERE", AlternateInjuries.WEAKNESS_SEVERE);
            InjuryType.register(75, "alt:WEAKNESS_DEADLY", AlternateInjuries.WEAKNESS_DEADLY);
            InjuryType.register(76, "alt:SORES_DISCOMFORT", AlternateInjuries.SORES_DISCOMFORT);
            InjuryType.register(77, "alt:SORES_SLIGHT", AlternateInjuries.SORES_SLIGHT);
            InjuryType.register(78, "alt:SORES_MODERATE", AlternateInjuries.SORES_MODERATE);
            InjuryType.register(79, "alt:SORES_SEVERE", AlternateInjuries.SORES_SEVERE);
            InjuryType.register(80, "alt:SORES_DEADLY", AlternateInjuries.SORES_DEADLY);
            InjuryType.register(81, "alt:FLU_DISCOMFORT", AlternateInjuries.FLU_DISCOMFORT);
            InjuryType.register(82, "alt:FLU_SLIGHT", AlternateInjuries.FLU_SLIGHT);
            InjuryType.register(83, "alt:FLU_MODERATE", AlternateInjuries.FLU_MODERATE);
            InjuryType.register(84, "alt:FLU_SEVERE", AlternateInjuries.FLU_SEVERE);
            InjuryType.register(85, "alt:FLU_DEADLY", AlternateInjuries.FLU_DEADLY);
            InjuryType.register(86, "alt:SIGHT_DISCOMFORT", AlternateInjuries.SIGHT_DISCOMFORT);
            InjuryType.register(87, "alt:SIGHT_SLIGHT", AlternateInjuries.SIGHT_SLIGHT);
            InjuryType.register(88, "alt:SIGHT_MODERATE", AlternateInjuries.SIGHT_MODERATE);
            InjuryType.register(89, "alt:SIGHT_SEVERE", AlternateInjuries.SIGHT_SEVERE);
            InjuryType.register(90, "alt:SIGHT_DEADLY", AlternateInjuries.SIGHT_DEADLY);
            InjuryType.register(91, "alt:TREMORS_DISCOMFORT", AlternateInjuries.TREMORS_DISCOMFORT);
            InjuryType.register(92, "alt:TREMORS_SLIGHT", AlternateInjuries.TREMORS_SLIGHT);
            InjuryType.register(93, "alt:TREMORS_MODERATE", AlternateInjuries.TREMORS_MODERATE);
            InjuryType.register(94, "alt:TREMORS_SEVERE", AlternateInjuries.TREMORS_SEVERE);
            InjuryType.register(95, "alt:TREMORS_DEADLY", AlternateInjuries.TREMORS_DEADLY);
            InjuryType.register(96, "alt:BREATHING_DISCOMFORT", AlternateInjuries.BREATHING_DISCOMFORT);
            InjuryType.register(97, "alt:BREATHING_SLIGHT", AlternateInjuries.BREATHING_SLIGHT);
            InjuryType.register(98, "alt:BREATHING_MODERATE", AlternateInjuries.BREATHING_MODERATE);
            InjuryType.register(99, "alt:BREATHING_SEVERE", AlternateInjuries.BREATHING_SEVERE);
            InjuryType.register(100, "alt:BREATHING_DEADLY", AlternateInjuries.BREATHING_DEADLY);
            InjuryType.register(101, "alt:HEMOPHILIA_DISCOMFORT", AlternateInjuries.HEMOPHILIA_DISCOMFORT);
            InjuryType.register(102, "alt:HEMOPHILIA_SLIGHT", AlternateInjuries.HEMOPHILIA_SLIGHT);
            InjuryType.register(103, "alt:HEMOPHILIA_MODERATE", AlternateInjuries.HEMOPHILIA_MODERATE);
            InjuryType.register(104, "alt:HEMOPHILIA_SEVERE", AlternateInjuries.HEMOPHILIA_SEVERE);
            InjuryType.register(105, "alt:HEMOPHILIA_DEADLY", AlternateInjuries.HEMOPHILIA_DEADLY);
            InjuryType.register(106, "alt:VENEREAL_DISCOMFORT", AlternateInjuries.VENEREAL_DISCOMFORT);
            InjuryType.register(107, "alt:VENEREAL_SLIGHT", AlternateInjuries.VENEREAL_SLIGHT);
            InjuryType.register(108, "alt:VENEREAL_MODERATE", AlternateInjuries.VENEREAL_MODERATE);
            InjuryType.register(109, "alt:VENEREAL_SEVERE", AlternateInjuries.VENEREAL_SEVERE);
            InjuryType.register(110, "alt:VENEREAL_DEADLY", AlternateInjuries.VENEREAL_DEADLY);

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
