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

import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate.MAXIMUM_INJURY_DURATION_MULTIPLIER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.Set;

import megamek.common.enums.Gender;
import mekhq.Utilities;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.medical.BodyLocation;

public class AlternateInjuries {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AlternateInjuries";

    private static final int BURN_HEALING_DAYS = 14; // Internet says 1-3 weeks
    private static final int DEAFNESS_HEALING_DAYS = 7; // Internet says around a week
    private static final int BLINDNESS_HEALING_DAYS = 14; // Internet says this varies a lot, we went with 2 weeks
    private static final int FRACTURE_HEALING_DAYS = 49; // Internet says 6-8 weeks
    private static final int COMPOUND_FRACTURE_HEALING_DAYS = 56; // Internet says 6-12 weeks
    private static final int SMOKE_INHALATION_HEALING_DAYS = 3; // Internet says 2-3 days
    private static final int PUNCTURED_LUNG_HEALING_DAYS = 49; // Internet says 6-8 days
    private static final int HEART_TRAUMA_HEALING_DAYS = 56; // Internet says 4-12 weeks
    private static final int ORGAN_BRUISE_HEALING_DAYS = 35; // Internet says 4-6 weeks
    private static final int ORGAN_TRAUMA_HEALING_DAYS = 49; // Internet says 6-8 weeks
    private static final int DISEMBOWELED_HEALING_DAYS = 49; // Internet says 6-8 weeks
    private static final int BONE_BRUISE_HEALING_DAYS = 42; // Internet says 6 weeks
    private static final int BLOOD_LOSS_HEALING_DAYS = 35; // Internet says 4-6 weeks
    private static final int SEVER_HEALING_DAYS = 180; // We need to have something here for Advanced Medical

    private static final InjuryLevel SEVERE_INJURY_LEVEL = InjuryLevel.CHRONIC;
    private static final InjuryLevel FRACTURE_INJURY_LEVEL = InjuryLevel.MAJOR;
    private static final InjuryLevel BURN_INJURY_LEVEL = InjuryLevel.MINOR;

    // Head
    public static final InjuryType SEVERED_HEAD = new SeveredHead();
    public static final InjuryType BURN_FACE = new BurnedFace();
    public static final InjuryType HEARING_LOSS = new HearingLoss();
    public static final InjuryType BLINDNESS = new Blindness();
    public static final InjuryType FRACTURED_JAW = new FracturedJaw();
    public static final InjuryType FRACTURED_SKULL = new FracturedSkull();
    // Upper Torso
    public static final InjuryType BURNED_CHEST = new BurnedChest();
    public static final InjuryType FRACTURED_RIB = new FracturedRib();
    public static final InjuryType SMOKE_INHALATION = new SmokeInhalation();
    public static final InjuryType PUNCTURED_LUNG = new PuncturedLung();
    public static final InjuryType HEART_TRAUMA = new HeartTrauma();
    // Lower Torso
    public static final InjuryType BURN_ABDOMINAL = new AbdominalBurn();
    public static final InjuryType BRUISED_ORGAN = new BruisedOrgan();
    public static final InjuryType ORGAN_TRAUMA = new OrganTrauma();
    public static final InjuryType FRACTURED_GROIN = new FracturedGroin();
    public static final InjuryType DISEMBOWELED = new Disemboweled();
    // Upper Arm
    public static final InjuryType SEVERED_ARM = new SeveredArm();
    public static final InjuryType BURN_UPPER_ARM = new BurnedUpperArm();
    public static final InjuryType FRACTURED_UPPER_ARM = new FracturedUpperArm();
    public static final InjuryType FRACTURED_ELBOW = new FracturedElbow();
    public static final InjuryType FRACTURED_SHOULDER = new FracturedShoulder();
    public static final InjuryType COMPOUND_FRACTURED_SHOULDER = new CompoundFracturedShoulder();
    // Lower Arm
    public static final InjuryType SEVERED_HAND = new SeveredHand();
    public static final InjuryType BURN_HAND = new HandBurn();
    public static final InjuryType FRACTURED_HAND = new FracturedHand();
    public static final InjuryType FRACTURED_WRIST = new FracturedWrist();
    public static final InjuryType FRACTURED_FOREARM = new FracturedForearm();
    public static final InjuryType COMPOUND_FRACTURED_FOREARM = new CompoundFracturedForearm();
    // Upper Leg
    public static final InjuryType SEVERED_LEG = new SeveredLeg();
    public static final InjuryType BURN_THIGH = new ThighBurn();
    public static final InjuryType BRUISED_FEMUR = new BruisedFemur();
    public static final InjuryType FRACTURED_FEMUR = new FracturedFemur();
    public static final InjuryType COMPOUND_FRACTURED_FEMUR = new CompoundFracturedFemur();
    public static final InjuryType FRACTURED_HIP = new FracturedHip();
    // Lower Leg
    public static final InjuryType SEVERED_FOOT = new SeveredFoot();
    public static final InjuryType BURN_CALF = new CalfBurn();
    public static final InjuryType FRACTURED_FOOT = new FracturedFoot();
    public static final InjuryType FRACTURED_ANKLE = new FracturedAnkle();
    public static final InjuryType FRACTURED_KNEE = new FracturedKnee();
    public static final InjuryType COMPOUND_FRACTURED_SHIN = new CompoundFracturedShin();
    // Any
    public static final InjuryType BLOOD_LOSS = new BloodLoss();

    // Base injury type classes with common behavior
    private abstract static class BaseInjury extends InjuryType {
        protected BaseInjury(int recoveryTime, boolean permanent, InjuryLevel level,
              InjuryEffect effect, Set<BodyLocation> locations) {
            this.recoveryTime = recoveryTime;
            this.permanent = permanent;
            this.maxSeverity = MAXIMUM_INJURY_DURATION_MULTIPLIER;
            this.level = level;
            this.injuryEffect = effect;
            this.allowedLocations = locations;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return simpleName;
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getName(loc, severity);
        }

        @Override
        public int getRecoveryTime(int severity) {
            return recoveryTime * severity;
        }
    }

    private abstract static class SimpleBurn extends BaseInjury {
        protected SimpleBurn(String nameKey, Set<BodyLocation> locations) {
            super(BURN_HEALING_DAYS, false, BURN_INJURY_LEVEL, InjuryEffect.NONE, locations);
            this.simpleName = getTextAt(RESOURCE_BUNDLE, nameKey);
            this.fluffText = getTextAt(RESOURCE_BUNDLE, nameKey);
        }
    }

    private abstract static class FormattedBurn extends BaseInjury {
        protected FormattedBurn(Set<BodyLocation> locations) {
            super(BURN_HEALING_DAYS, false, BURN_INJURY_LEVEL, InjuryEffect.NONE, locations);
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BURN.simpleName",
                  loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BURN.simpleName",
                  loc.locationName());
        }
    }

    private abstract static class FormattedFracture extends BaseInjury {
        protected FormattedFracture(int healingDays, InjuryEffect effect, Set<BodyLocation> locations) {
            super(healingDays, false, FRACTURE_INJURY_LEVEL, effect, locations);

        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            String key = injuryEffect == InjuryEffect.COMPOUND_FRACTURE
                               ? "AlternateInjuries.COMPOUND_FRACTURE.simpleName"
                               : "AlternateInjuries.FRACTURE.simpleName";
            return getFormattedTextAt(RESOURCE_BUNDLE, key, loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getName(loc, severity);
        }
    }

    private abstract static class FormattedSevere extends BaseInjury {
        protected FormattedSevere(Set<BodyLocation> locations) {
            super(SEVER_HEALING_DAYS, true, SEVERE_INJURY_LEVEL, InjuryEffect.SEVERED, locations);
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            String key = "AlternateInjuries.SEVERED.simpleName";
            return getFormattedTextAt(RESOURCE_BUNDLE, key, loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SEVERED.simpleName",
                  loc.locationName());
        }

        @Override
        public boolean impliesMissingLocation() {
            return true;
        }
    }

    // Head injuries
    public static final class BurnedFace extends SimpleBurn {
        public BurnedFace() {
            super("AlternateInjuries.FACIAL_BURN.simpleName",
                  Set.of(BodyLocation.HEAD));
        }
    }

    public static final class HearingLoss extends BaseInjury {
        public HearingLoss() {
            super(DEAFNESS_HEALING_DAYS, false, InjuryLevel.MINOR,
                  InjuryEffect.DEAFENED, Set.of(BodyLocation.EARS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_LOSS.simpleName");
        }
    }

    public static final class Blindness extends BaseInjury {
        public Blindness() {
            super(BLINDNESS_HEALING_DAYS, false, InjuryLevel.MINOR,
                  InjuryEffect.BLINDED, Set.of(BodyLocation.EYES));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BLINDNESS.simpleName");
        }
    }

    public static final class FracturedJaw extends FormattedFracture {
        public FracturedJaw() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_JAW, Set.of(BodyLocation.JAW));
        }
    }

    public static final class FracturedSkull extends FormattedFracture {
        public FracturedSkull() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_SKULL, Set.of(BodyLocation.SKULL));
        }
    }

    public static final class SeveredHead extends FormattedSevere {
        public SeveredHead() {
            super(Set.of(BodyLocation.SKULL));
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    // Torso injuries
    public static final class BurnedChest extends FormattedBurn {
        public BurnedChest() {
            super(Set.of(BodyLocation.CHEST));
        }
    }

    public static final class FracturedRib extends FormattedFracture {
        public FracturedRib() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_RIB, Set.of(BodyLocation.RIBS));
        }
    }

    public static final class SmokeInhalation extends BaseInjury {
        public SmokeInhalation() {
            super(SMOKE_INHALATION_HEALING_DAYS, false, InjuryLevel.MINOR,
                  InjuryEffect.NONE, Set.of(BodyLocation.LUNGS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SMOKE_INHALATION.simpleName");
        }
    }

    public static final class PuncturedLung extends BaseInjury {
        public PuncturedLung() {
            super(PUNCTURED_LUNG_HEALING_DAYS, false, InjuryLevel.DEADLY,
                  InjuryEffect.PUNCTURED, Set.of(BodyLocation.LUNGS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PUNCTURED_LUNG.simpleName");
        }
    }

    public static final class HeartTrauma extends BaseInjury {
        public HeartTrauma() {
            super(HEART_TRAUMA_HEALING_DAYS, false, InjuryLevel.DEADLY,
                  InjuryEffect.INTERNAL_BLEEDING, Set.of(BodyLocation.HEART));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEART_TRAUMA.simpleName");
        }
    }

    public static final class AbdominalBurn extends SimpleBurn {
        public AbdominalBurn() {
            super("AlternateInjuries.BURN_ABDOMINAL.simpleName", Set.of(BodyLocation.ABDOMEN));
        }
    }

    public static final class BruisedOrgan extends BaseInjury {
        public BruisedOrgan() {
            super(ORGAN_BRUISE_HEALING_DAYS, false, InjuryLevel.MINOR,
                  InjuryEffect.NONE, Set.of(BodyLocation.CHEST));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BRUISED_ORGAN.simpleName");
        }
    }

    public static final class OrganTrauma extends BaseInjury {
        public OrganTrauma() {
            super(ORGAN_TRAUMA_HEALING_DAYS, false, InjuryLevel.DEADLY,
                  InjuryEffect.INTERNAL_BLEEDING, Set.of(BodyLocation.CHEST));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ORGAN_TRAUMA.simpleName");
        }
    }

    public static final class FracturedGroin extends BaseInjury {
        public FracturedGroin() {
            super(FRACTURE_HEALING_DAYS, false, FRACTURE_INJURY_LEVEL,
                  InjuryEffect.FRACTURE_LIMB, Set.of(BodyLocation.GROIN));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FRACTURED_GROIN.simpleName");
        }
    }

    public static final class Disemboweled extends BaseInjury {
        public Disemboweled() {
            super(DISEMBOWELED_HEALING_DAYS, false, InjuryLevel.DEADLY,
                  InjuryEffect.INTERNAL_BLEEDING, Set.of(BodyLocation.ABDOMEN));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DISEMBOWELED.simpleName");
        }
    }

    // Arm injuries
    public static final class SeveredArm extends FormattedSevere {
        public SeveredArm() {
            super(Set.of(BodyLocation.LEFT_ARM, BodyLocation.RIGHT_ARM));
        }
    }

    public static final class BurnedUpperArm extends FormattedBurn {
        public BurnedUpperArm() {
            super(Set.of(BodyLocation.UPPER_LEFT_ARM, BodyLocation.UPPER_RIGHT_ARM));
        }
    }

    public static final class FracturedUpperArm extends FormattedFracture {
        public FracturedUpperArm() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.UPPER_LEFT_ARM, BodyLocation.UPPER_RIGHT_ARM));
        }
    }

    public static final class FracturedElbow extends FormattedFracture {
        public FracturedElbow() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_ELBOW, BodyLocation.RIGHT_ELBOW));
        }
    }

    public static final class FracturedShoulder extends FormattedFracture {
        public FracturedShoulder() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_SHOULDER, BodyLocation.RIGHT_SHOULDER));
        }
    }

    public static final class CompoundFracturedShoulder extends FormattedFracture {
        public CompoundFracturedShoulder() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_SHOULDER, BodyLocation.RIGHT_SHOULDER));
        }
    }

    // Hand injuries
    public static final class SeveredHand extends FormattedSevere {
        public SeveredHand() {
            super(Set.of(BodyLocation.LEFT_HAND, BodyLocation.RIGHT_HAND));
        }
    }

    public static final class HandBurn extends FormattedBurn {
        public HandBurn() {
            super(Set.of(BodyLocation.LEFT_HAND, BodyLocation.RIGHT_HAND));
        }
    }

    public static final class FracturedHand extends FormattedFracture {
        public FracturedHand() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_HAND, BodyLocation.RIGHT_HAND));
        }
    }

    public static final class FracturedWrist extends FormattedFracture {
        public FracturedWrist() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_WRIST, BodyLocation.RIGHT_WRIST));
        }
    }

    public static final class FracturedForearm extends FormattedFracture {
        public FracturedForearm() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_FOREARM, BodyLocation.RIGHT_FOREARM));
        }
    }

    public static final class CompoundFracturedForearm extends FormattedFracture {
        public CompoundFracturedForearm() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_FOREARM, BodyLocation.RIGHT_FOREARM));
        }
    }

    // Leg injuries
    public static final class SeveredLeg extends FormattedSevere {
        public SeveredLeg() {
            super(Set.of(BodyLocation.LEFT_LEG, BodyLocation.RIGHT_LEG));
        }
    }

    public static final class ThighBurn extends FormattedBurn {
        public ThighBurn() {
            super(Set.of(BodyLocation.LEFT_THIGH, BodyLocation.RIGHT_THIGH));
        }
    }

    public static final class BruisedFemur extends BaseInjury {
        public BruisedFemur() {
            super(BONE_BRUISE_HEALING_DAYS, false, InjuryLevel.MINOR,
                  InjuryEffect.NONE, Set.of(BodyLocation.LEFT_FEMUR, BodyLocation.RIGHT_FEMUR));
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BRUISE.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BRUISE.simpleName",
                  loc.locationName());
        }
    }

    public static final class FracturedFemur extends FormattedFracture {
        public FracturedFemur() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_FEMUR, BodyLocation.RIGHT_FEMUR));
        }
    }

    public static final class CompoundFracturedFemur extends FormattedFracture {
        public CompoundFracturedFemur() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_FEMUR, BodyLocation.RIGHT_FEMUR));
        }
    }

    public static final class FracturedHip extends FormattedFracture {
        public FracturedHip() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_HIP, BodyLocation.RIGHT_HIP));
        }
    }

    // Foot injuries
    public static final class SeveredFoot extends FormattedSevere {
        public SeveredFoot() {
            super(Set.of(BodyLocation.LEFT_FOOT, BodyLocation.RIGHT_FOOT));
        }
    }

    public static final class CalfBurn extends FormattedBurn {
        public CalfBurn() {
            super(Set.of(BodyLocation.LEFT_CALF, BodyLocation.RIGHT_CALF));
        }
    }

    public static final class FracturedFoot extends FormattedFracture {
        public FracturedFoot() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_FOOT, BodyLocation.RIGHT_FOOT));
        }
    }

    public static final class FracturedAnkle extends FormattedFracture {
        public FracturedAnkle() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_ANKLE, BodyLocation.RIGHT_ANKLE));
        }
    }

    public static final class FracturedKnee extends FormattedFracture {
        public FracturedKnee() {
            super(FRACTURE_HEALING_DAYS, InjuryEffect.FRACTURE_LIMB,
                  Set.of(BodyLocation.LEFT_KNEE, BodyLocation.RIGHT_KNEE));
        }
    }

    public static final class CompoundFracturedShin extends FormattedFracture {
        public CompoundFracturedShin() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, InjuryEffect.COMPOUND_FRACTURE,
                  Set.of(BodyLocation.LEFT_SHIN, BodyLocation.RIGHT_SHIN));
        }
    }

    public static final class BloodLoss extends BaseInjury {
        public BloodLoss() {
            super(BLOOD_LOSS_HEALING_DAYS, false, InjuryLevel.MAJOR,
                  InjuryEffect.NONE, Set.of(BodyLocation.INTERNAL));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BLOOD_LOSS.simpleName");
            ;
            this.fluffText = simpleName;
        }
    }
}
