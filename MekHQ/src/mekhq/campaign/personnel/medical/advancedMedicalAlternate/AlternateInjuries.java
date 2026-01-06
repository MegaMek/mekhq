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

import static mekhq.campaign.personnel.enums.InjuryLevel.CHRONIC;
import static mekhq.campaign.personnel.enums.InjuryLevel.DEADLY;
import static mekhq.campaign.personnel.enums.InjuryLevel.MAJOR;
import static mekhq.campaign.personnel.enums.InjuryLevel.MINOR;
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate.MAXIMUM_INJURY_DURATION_MULTIPLIER;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjuryEffect.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.FLAW;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.IMPLANT_GENERIC;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.IMPLANT_VDNI;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.NORMAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.PROSTHETIC_MYOMER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import megamek.common.enums.Gender;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.medical.BodyLocation;

public class AlternateInjuries {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AlternateInjuries";

    private static final int BURN_HEALING_DAYS = 4; // Internet says 1-3 weeks
    private static final int DEAFNESS_HEALING_DAYS = 4; // Internet says around a week
    private static final int BLINDNESS_HEALING_DAYS = 4; // Internet says this varies a lot, we went with a week
    private static final int FRACTURE_HEALING_DAYS = 21; // Internet says 6-8 weeks
    private static final int COMPOUND_FRACTURE_HEALING_DAYS = 21; // Internet says 6-12 weeks
    private static final int SMOKE_INHALATION_HEALING_DAYS = 1; // Internet says 2-3 days
    private static final int PUNCTURED_LUNG_HEALING_DAYS = 21; // Internet says 6-8 weeks
    private static final int HEART_TRAUMA_HEALING_DAYS = 14; // Internet says 4-12 weeks
    private static final int ORGAN_BRUISE_HEALING_DAYS = 14; // Internet says 4-6 weeks
    private static final int ORGAN_TRAUMA_HEALING_DAYS = 21; // Internet says 6-8 weeks
    private static final int DISEMBOWELED_HEALING_DAYS = 21; // Internet says 6-8 weeks
    private static final int BONE_BRUISE_HEALING_DAYS = 21; // Internet says 6 weeks
    private static final int BLOOD_LOSS_HEALING_DAYS = 14; // Internet says 4-6 weeks
    private static final int SEVER_HEALING_DAYS = 180; // We need to have something here for Advanced Medical
    private static final int CLONED_LIMB_HEALING_DAYS = 21; // ATOW pg 316
    private static final int REPLACEMENT_LIMB_HEALING_DAYS = 42; // ATOW pg 316
    private static final int COSMETIC_SURGERY_RECOVERY_HEALING_DAYS = 7; // Internet says 2-3 weeks
    private static final int ELECTIVE_IMPLANT_RECOVERY_HEALING_DAYS = 90; // ATOW pg 317
    private static final int ENHANCED_IMAGING_IMPLANT_RECOVERY_HEALING_DAYS = 365; // ATOW pg 317
    private static final int PAIN_SHUNT_RECOVERY_HEALING_DAYS = 365; // ATOW:Companion pg 182
    private static final int DISCONTINUATION_SYNDROME_HEALING_DAYS = 7; // We check for this weekly
    private static final int WEEKLY_CHECK_ILLNESS_HEALING_DAYS = 7;
    private static final int POSTPARTUM_RECOVERY_HEALING_DAYS = 21; // Internet says 6 weeks
    private static final int TRANSIT_DISORIENTATION_SYNDROME_HEALING_DAYS = 1;
    private static final int CHILDUS_FEVER_RECOVERY_TIME = 365;
    private static final int OLD_WOUND_HEALING_DAYS = 7;

    private static final InjuryLevel SEVER_INJURY_LEVEL = CHRONIC;
    private static final InjuryLevel FRACTURE_INJURY_LEVEL = MAJOR;
    private static final InjuryLevel BURN_INJURY_LEVEL = MINOR;

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
    public static final InjuryType DISCONTINUATION_SYNDROME = new DiscontinuationSyndrome();
    public static final InjuryType POSTPARTUM_RECOVERY = new PostpartumRecovery();
    public static final InjuryType TRANSIT_DISORIENTATION_SYNDROME = new TransitDisorientationSyndrome();
    public static final InjuryType CRIPPLING_FLASHBACKS = new CripplingFlashbacks();
    public static final InjuryType CHILDLIKE_REGRESSION = new ChildlikeRegression();
    public static final InjuryType CATATONIA = new ChronicDisassociation();
    public static final InjuryType TERRIBLE_BRUISES = new TerribleBruises();
    public static final InjuryType OLD_WOUND = new OldWound();
    // Diseases
    public static final InjuryType GROWTHS_DISCOMFORT = new GrowthsDiscomfort();
    public static final InjuryType GROWTHS_SLIGHT = new GrowthsSlight();
    public static final InjuryType GROWTHS_MODERATE = new GrowthsModerate();
    public static final InjuryType GROWTHS_SEVERE = new GrowthsSevere();
    public static final InjuryType GROWTHS_DEADLY = new GrowthsDeadly();
    public static final InjuryType INFECTION_DISCOMFORT = new InfectionDiscomfort();
    public static final InjuryType INFECTION_SLIGHT = new InfectionSlight();
    public static final InjuryType INFECTION_MODERATE = new InfectionModerate();
    public static final InjuryType INFECTION_SEVERE = new InfectionSevere();
    public static final InjuryType INFECTION_DEADLY = new InfectionDeadly();
    public static final InjuryType HEARING_DISCOMFORT = new HearingDiscomfort();
    public static final InjuryType HEARING_SLIGHT = new HearingSlight();
    public static final InjuryType HEARING_MODERATE = new HearingModerate();
    public static final InjuryType HEARING_SEVERE = new HearingSevere();
    public static final InjuryType HEARING_DEADLY = new HearingDeadly();
    public static final InjuryType WEAKNESS_DISCOMFORT = new WeaknessDiscomfort();
    public static final InjuryType WEAKNESS_SLIGHT = new WeaknessSlight();
    public static final InjuryType WEAKNESS_MODERATE = new WeaknessModerate();
    public static final InjuryType WEAKNESS_SEVERE = new WeaknessSevere();
    public static final InjuryType WEAKNESS_DEADLY = new WeaknessDeadly();
    public static final InjuryType SORES_DISCOMFORT = new SoresDiscomfort();
    public static final InjuryType SORES_SLIGHT = new SoresSlight();
    public static final InjuryType SORES_MODERATE = new SoresModerate();
    public static final InjuryType SORES_SEVERE = new SoresSevere();
    public static final InjuryType SORES_DEADLY = new SoresDeadly();
    public static final InjuryType FLU_DISCOMFORT = new FluDiscomfort();
    public static final InjuryType FLU_SLIGHT = new FluSlight();
    public static final InjuryType FLU_MODERATE = new FluModerate();
    public static final InjuryType FLU_SEVERE = new FluSevere();
    public static final InjuryType FLU_DEADLY = new FluDeadly();
    public static final InjuryType SIGHT_DISCOMFORT = new SightDiscomfort();
    public static final InjuryType SIGHT_SLIGHT = new SightSlight();
    public static final InjuryType SIGHT_MODERATE = new SightModerate();
    public static final InjuryType SIGHT_SEVERE = new SightSevere();
    public static final InjuryType SIGHT_DEADLY = new SightDeadly();
    public static final InjuryType TREMORS_DISCOMFORT = new TremorsDiscomfort();
    public static final InjuryType TREMORS_SLIGHT = new TremorsSlight();
    public static final InjuryType TREMORS_MODERATE = new TremorsModerate();
    public static final InjuryType TREMORS_SEVERE = new TremorsSevere();
    public static final InjuryType TREMORS_DEADLY = new TremorsDeadly();
    public static final InjuryType BREATHING_DISCOMFORT = new BreathingDiscomfort();
    public static final InjuryType BREATHING_SLIGHT = new BreathingSlight();
    public static final InjuryType BREATHING_MODERATE = new BreathingModerate();
    public static final InjuryType BREATHING_SEVERE = new BreathingSevere();
    public static final InjuryType BREATHING_DEADLY = new BreathingDeadly();
    public static final InjuryType HEMOPHILIA_DISCOMFORT = new HemophiliaDiscomfort();
    public static final InjuryType HEMOPHILIA_SLIGHT = new HemophiliaSlight();
    public static final InjuryType HEMOPHILIA_MODERATE = new HemophiliaModerate();
    public static final InjuryType HEMOPHILIA_SEVERE = new HemophiliaSevere();
    public static final InjuryType HEMOPHILIA_DEADLY = new HemophiliaDeadly();
    public static final InjuryType VENEREAL_DISCOMFORT = new VenerealDiscomfort();
    public static final InjuryType VENEREAL_SLIGHT = new VenerealSlight();
    public static final InjuryType VENEREAL_MODERATE = new VenerealModerate();
    public static final InjuryType VENEREAL_SEVERE = new VenerealSevere();
    public static final InjuryType VENEREAL_DEADLY = new VenerealDeadly();

    public static final InjuryType ALARION_HANTA_VIRUS = new AlarionHantaVirus();
    public static final InjuryType ALBIERO_CONSUMPTION = new AlbieroConsumption();
    public static final InjuryType ALGEDI_BLOOD_BURN = new AlgediBloodBurn();
    public static final InjuryType ANCHA_VIRUS = new AnchaVirus();
    public static final InjuryType BETHOLD_SYNDROME = new BetholdSyndrome();
    public static final InjuryType BLACK_MARSH_FEVER = new BlackMarshFever();
    public static final InjuryType BRISBANE_VIRUS = new BrisbaneVirus();
    public static final InjuryType CHELOSIAN_VIRUS = new ChelosianVirus();
    public static final InjuryType CHILDUS_FEVER = new ChildusFever();
    public static final InjuryType CHUNGALOMENINGITIS_AMARIS = new ChungalomeningitisAmaris();
    public static final InjuryType CHUNGALOMENINGITIS_TRADITIONAL = new ChungalomeningitisTraditional();
    public static final InjuryType CROMARTY_SUPERFLU = new CromartySuperflu();
    public static final InjuryType CURSE_OF_EDEN = new CurseOfEden();
    public static final InjuryType CURSE_OF_GALEDON = new CurseOfGaledon();
    public static final InjuryType CUSSET_CRUD = new CussetCrud();
    public static final InjuryType DANGMARS_FEVER = new DangmarsFever();
    public static final InjuryType DARRS_DISEASE = new DarrsDisease();
    public static final InjuryType DELPHI_CURSE = new DelphiCurse();
    public static final InjuryType DEVILITCH = new Devilitch();
    public static final InjuryType DOWNING_POLTURS_DISEASE = new DowningPoltursDisease();
    public static final InjuryType EDISON_WHITE_FLU = new EdisonWhiteFlu();
    public static final InjuryType ELTANIN_BRAIN_FEVER = new EltaninBrainFever();
    public static final InjuryType FENRIS_PLAGUE = new FenrisPlague();
    public static final InjuryType GALAX_PATHOGEN = new GalaxPathogen();
    public static final InjuryType GARMS_SYNDROME = new GarmsSyndrome();
    public static final InjuryType GENOAN_SPINAL_MENINGITIS = new GenoanSpinalMeningitis();
    public static final InjuryType HYBORIAN_BLOOD_PLAGUE = new HyborianBloodPlague();
    public static final InjuryType KAER_PATHOGEN = new KaerPathogen();
    public static final InjuryType BIRTH_DEFECT = new BirthDefect();
    public static final InjuryType KILEN_WATTS_SYNDROME = new KilenWattsSyndrome();
    public static final InjuryType KNIGHTS_GRASSE_SYNDROME = new KnightsGrasseSyndrome();
    public static final InjuryType LAENS_REGRET = new LaensRegret();
    public static final InjuryType LANDMARK_SUPERVIRUS = new LandmarkSupervirus();
    public static final InjuryType MIAPLACIDUS_PLAGUE = new MiaplacidusPlague();
    public static final InjuryType NEISSERIA_MALTHUSIA = new NeisseriaMalthusia();
    public static final InjuryType NEO_SMALLPOX = new NeoSmallpox();
    public static final InjuryType NOTILC_SWEATS = new NotilicSweats();
    public static final InjuryType NYKVARN_VIRUS = new NykvarnVirus();
    public static final InjuryType OCKHAMS_BLOOD_DISEASE = new OckhamsBloodDisease();
    public static final InjuryType PINGREE_FEVER = new PingreeFever();
    public static final InjuryType REDBURN_VIRUS = new RedburnVirus();
    public static final InjuryType ROCKLAND_FEVER = new RocklandFever();
    public static final InjuryType SCOURGE_PLAGUE = new ScourgePlague();
    public static final InjuryType SKOKIE_SHIVERS = new SkokieShivers();
    public static final InjuryType TOXOPLASMA_GONDII_HARDCOREA = new ToxoplasmaGondiiHardcorea();
    public static final InjuryType UNOLE_FLU = new UnoleFlu();
    public static final InjuryType WINSONS_REGRET = new WinsonsRegret();
    public static final InjuryType YIMPISEE_FEVER = new YimpiseeFever();
    // Prosthetics
    public static final InjuryType WOODEN_ARM = new WoodenArm();
    public static final InjuryType HOOK_HAND = new HookHand();
    public static final InjuryType PEG_LEG = new PegLeg();
    public static final InjuryType WOODEN_FOOT = new WoodenFoot();
    public static final InjuryType SIMPLE_ARM = new SimpleArm();
    public static final InjuryType SIMPLE_CLAW_HAND = new SimpleClawHand();
    public static final InjuryType SIMPLE_LEG = new SimpleLeg();
    public static final InjuryType SIMPLE_FOOT = new SimpleFoot();
    public static final InjuryType PROSTHETIC_ARM = new ProstheticArm();
    public static final InjuryType PROSTHETIC_HAND = new ProstheticHand();
    public static final InjuryType PROSTHETIC_LEG = new ProstheticLeg();
    public static final InjuryType PROSTHETIC_FOOT = new ProstheticFoot();
    public static final InjuryType ADVANCED_PROSTHETIC_ARM = new AdvancedProstheticArm();
    public static final InjuryType ADVANCED_PROSTHETIC_HAND = new AdvancedProstheticHand();
    public static final InjuryType ADVANCED_PROSTHETIC_LEG = new AdvancedProstheticLeg();
    public static final InjuryType ADVANCED_PROSTHETIC_FOOT = new AdvancedProstheticFoot();
    public static final InjuryType MYOMER_ARM = new MyomerArm();
    public static final InjuryType MYOMER_HAND = new MyomerHand();
    public static final InjuryType MYOMER_LEG = new MyomerLeg();
    public static final InjuryType MYOMER_FOOT = new MyomerFoot();
    public static final InjuryType CLONED_ARM = new ClonedArm();
    public static final InjuryType CLONED_HAND = new ClonedHand();
    public static final InjuryType CLONED_LEG = new ClonedLeg();
    public static final InjuryType CLONED_FOOT = new ClonedFoot();
    public static final InjuryType EYE_IMPLANT = new EyeImplant();
    public static final InjuryType BIONIC_EAR = new BionicEar();
    public static final InjuryType BIONIC_EYE = new BionicEye();
    public static final InjuryType BIONIC_HEART = new BionicHeart();
    public static final InjuryType BIONIC_LUNGS = new BionicLungs();
    public static final InjuryType BIONIC_ORGAN_OTHER = new BionicOrganOther();
    public static final InjuryType COSMETIC_SURGERY = new CosmeticSurgery();
    public static final InjuryType CLONED_LIMB_RECOVERY = new ClonedLimbRecovery();
    public static final InjuryType REPLACEMENT_LIMB_RECOVERY = new ReplacementLimbRecovery();
    public static final InjuryType REPLACEMENT_ORGAN_RECOVERY = new ReplacementOrganRecovery();
    public static final InjuryType COSMETIC_SURGERY_RECOVERY = new CosmeticSurgeryRecovery();
    public static final InjuryType FAILED_SURGERY_RECOVERY = new FailedSurgeryRecovery();
    public static final InjuryType ELECTIVE_MYOMER_ARM = new ElectiveMyomerArm();
    public static final InjuryType ELECTIVE_MYOMER_HAND = new ElectiveMyomerHand();
    public static final InjuryType ELECTIVE_MYOMER_LEG = new ElectiveMyomerLeg();
    public static final InjuryType ENHANCED_IMAGING_IMPLANT = new EnhancedImagingImplant();
    public static final InjuryType ELECTIVE_IMPLANT_RECOVERY = new ElectiveImplantRecovery();
    public static final InjuryType EI_IMPLANT_RECOVERY = new EIImplantRecovery();
    public static final InjuryType PAIN_SHUNT_RECOVERY = new PainShuntRecovery();
    public static final InjuryType BONE_REINFORCEMENT = new BoneReinforcement();
    public static final InjuryType LIVER_FILTRATION_IMPLANT = new OrganFiltrationImplant();
    public static final InjuryType BIONIC_LUNGS_WITH_TYPE_1_FILTER = new BionicLungsWithType1Filter();
    public static final InjuryType BIONIC_LUNGS_WITH_TYPE_2_FILTER = new BionicLungsWithType2Filter();
    public static final InjuryType BIONIC_LUNGS_WITH_TYPE_3_FILTER = new BionicLungsWithType3Filter();
    public static final InjuryType CYBERNETIC_EYE_EM_IR = new CyberneticEyeEMIR();
    public static final InjuryType CYBERNETIC_EYE_TELESCOPE = new CyberneticEyeTelescope();
    public static final InjuryType CYBERNETIC_EYE_LASER = new CyberneticEyeLaser();
    public static final InjuryType CYBERNETIC_EYE_MULTI = new CyberneticEyeMulti();
    public static final InjuryType CYBERNETIC_EYE_MULTI_ENHANCED = new CyberneticEyeMultiEnhanced();
    public static final InjuryType CYBERNETIC_EAR_COMMUNICATIONS = new CyberneticEarCommunications();
    public static final InjuryType CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS = new CyberneticEarBoostedCommunications();
    public static final InjuryType CYBERNETIC_EAR_ENHANCED = new CyberneticEarEnhanced();
    public static final InjuryType CYBERNETIC_EAR_SIGNAL = new CyberneticEarSignal();
    public static final InjuryType CYBERNETIC_EAR_MULTI = new CyberneticEarMulti();
    public static final InjuryType CYBERNETIC_SPEECH_IMPLANT = new CyberneticSpeechImplant();
    public static final InjuryType PHEROMONE_EFFUSER = new PheromoneEffuser();
    public static final InjuryType COSMETIC_BEAUTY_ENHANCEMENT = new CosmeticBeautyEnhancement();
    public static final InjuryType COSMETIC_HORROR_ENHANCEMENT = new CosmeticHorrorEnhancement();
    public static final InjuryType COSMETIC_TAIL_PROSTHETIC = new CosmeticTailProsthetic();
    public static final InjuryType COSMETIC_ANIMAL_EAR_PROSTHETIC = new CosmeticAnimalEarProsthetic();
    public static final InjuryType COSMETIC_ANIMAL_LEG_PROSTHETIC = new CosmeticLegProsthetic();
    public static final InjuryType DERMAL_MYOMER_ARM_ARMOR = new DermalMyomerArmorArm();
    public static final InjuryType DERMAL_MYOMER_ARM_CAMO = new DermalMyomerCamoArm();
    public static final InjuryType DERMAL_MYOMER_ARM_TRIPLE = new DermalMyomerTripleArm();
    public static final InjuryType DERMAL_MYOMER_LEG_ARMOR = new DermalMyomerArmorLeg();
    public static final InjuryType DERMAL_MYOMER_LEG_CAMO = new DermalMyomerCamoLeg();
    public static final InjuryType DERMAL_MYOMER_LEG_TRIPLE = new DermalMyomerTripleLeg();
    public static final InjuryType PROTOTYPE_VDNI = new PrototypeVDNI();
    public static final InjuryType VEHICULAR_DNI = new VehicularDNI();
    public static final InjuryType BUFFERED_VDNI = new BufferedVDNI();
    public static final InjuryType BUFFERED_VDNI_TRIPLE_CORE = new BufferedVDNITripleCore();
    public static final InjuryType PAIN_SHUNT = new PainShunt();
    public static final InjuryType IMPLANT_REMOVAL_RECOVERY = new ImplantRemovalRecovery();
    public static final InjuryType SECONDARY_POWER_SUPPLY = new SecondaryPowerSupply();

    // Base injury type classes with common behavior
    private abstract static class BaseInjury extends InjuryType {
        protected BaseInjury(int recoveryTime, boolean permanent, InjuryLevel level, InjuryEffect effect,
              Set<BodyLocation> locations) {
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
            super(BURN_HEALING_DAYS, false, BURN_INJURY_LEVEL, NONE, locations);
            this.simpleName = getTextAt(RESOURCE_BUNDLE, nameKey);
            this.fluffText = getTextAt(RESOURCE_BUNDLE, nameKey);
            this.injurySubType = InjurySubType.BURN;
        }
    }

    private abstract static class FormattedBurn extends BaseInjury {
        protected FormattedBurn(Set<BodyLocation> locations) {
            super(BURN_HEALING_DAYS, false, BURN_INJURY_LEVEL, NONE, locations);
            this.injurySubType = InjurySubType.BURN;
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
            String key = injuryEffect == COMPOUND_FRACTURE
                               ? "AlternateInjuries.COMPOUND_FRACTURE.simpleName"
                               : "AlternateInjuries.FRACTURE.simpleName";
            return getFormattedTextAt(RESOURCE_BUNDLE, key, loc.locationName());
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, Gender gender) {
            return getName(loc, severity);
        }
    }

    private abstract static class FormattedSever extends BaseInjury {
        protected FormattedSever(Set<BodyLocation> locations) {
            super(SEVER_HEALING_DAYS, true, SEVER_INJURY_LEVEL, SEVERED, locations);
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
                  Set.of(FACE));
        }
    }

    public static final class HearingLoss extends BaseInjury {
        public HearingLoss() {
            super(DEAFNESS_HEALING_DAYS, false, MINOR,
                  DEAFENED, Set.of(EARS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_LOSS.simpleName");
        }
    }

    public static final class Blindness extends BaseInjury {
        public Blindness() {
            super(BLINDNESS_HEALING_DAYS, false, MINOR,
                  BLINDED, Set.of(EYES));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BLINDNESS.simpleName");
        }
    }

    public static final class FracturedJaw extends FormattedFracture {
        public FracturedJaw() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_JAW, Set.of(JAW));
        }
    }

    public static final class FracturedSkull extends FormattedFracture {
        public FracturedSkull() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_SKULL, Set.of(SKULL));
        }
    }

    public static final class SeveredHead extends FormattedSever {
        public SeveredHead() {
            super(Set.of(HEAD));
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    // Torso injuries
    public static final class BurnedChest extends FormattedBurn {
        public BurnedChest() {
            super(Set.of(CHEST));
        }
    }

    public static final class FracturedRib extends FormattedFracture {
        public FracturedRib() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_RIB, Set.of(RIBS));
        }
    }

    public static final class SmokeInhalation extends BaseInjury {
        public SmokeInhalation() {
            super(SMOKE_INHALATION_HEALING_DAYS, false, MINOR,
                  NONE, Set.of(LUNGS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SMOKE_INHALATION.simpleName");
        }
    }

    public static final class PuncturedLung extends BaseInjury {
        public PuncturedLung() {
            super(PUNCTURED_LUNG_HEALING_DAYS, false, DEADLY,
                  PUNCTURED, Set.of(LUNGS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PUNCTURED_LUNG.simpleName");
        }
    }

    public static final class HeartTrauma extends BaseInjury {
        public HeartTrauma() {
            super(HEART_TRAUMA_HEALING_DAYS, false, DEADLY,
                  INTERNAL_BLEEDING, Set.of(HEART));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEART_TRAUMA.simpleName");
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign campaign, Person person, Injury injury, int hits) {
            return Collections.singletonList(new GameEffect("Blood Loss", operator -> {
                Injury bleeding = BLOOD_LOSS.newInjury(campaign, person, GENERIC, 1);
                person.addInjury(bleeding);
                MedicalLogger.internalBleedingWorsened(person, campaign.getLocalDate());
            }));
        }
    }

    public static final class AbdominalBurn extends SimpleBurn {
        public AbdominalBurn() {
            super("AlternateInjuries.BURN_ABDOMINAL.simpleName", Set.of(ABDOMEN));
        }
    }

    public static final class BruisedOrgan extends BaseInjury {
        public BruisedOrgan() {
            super(ORGAN_BRUISE_HEALING_DAYS, false, MINOR,
                  NONE, Set.of(ORGANS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BRUISED_ORGAN.simpleName");
        }
    }

    public static final class OrganTrauma extends BaseInjury {
        public OrganTrauma() {
            super(ORGAN_TRAUMA_HEALING_DAYS, false, DEADLY,
                  INTERNAL_BLEEDING, Set.of(ORGANS));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ORGAN_TRAUMA.simpleName");
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign campaign, Person person, Injury injury, int hits) {
            return Collections.singletonList(new GameEffect("Blood Loss", operator -> {
                Injury bleeding = BLOOD_LOSS.newInjury(campaign, person, GENERIC, 1);
                person.addInjury(bleeding);
                MedicalLogger.internalBleedingWorsened(person, campaign.getLocalDate());
            }));
        }
    }

    public static final class FracturedGroin extends BaseInjury {
        public FracturedGroin() {
            super(FRACTURE_HEALING_DAYS, false, FRACTURE_INJURY_LEVEL,
                  FRACTURE_LIMB, Set.of(GROIN));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FRACTURED_GROIN.simpleName");
        }
    }

    public static final class Disemboweled extends BaseInjury {
        public Disemboweled() {
            super(DISEMBOWELED_HEALING_DAYS, false, DEADLY,
                  INTERNAL_BLEEDING, Set.of(ABDOMEN));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DISEMBOWELED.simpleName");
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign campaign, Person person, Injury injury, int hits) {
            return Collections.singletonList(new GameEffect("Blood Loss", operator -> {
                Injury bleeding = BLOOD_LOSS.newInjury(campaign, person, GENERIC, 1);
                person.addInjury(bleeding);
                MedicalLogger.internalBleedingWorsened(person, campaign.getLocalDate());
            }));
        }
    }

    // Arm injuries
    public static final class SeveredArm extends FormattedSever {
        public SeveredArm() {
            super(Set.of(LEFT_ARM, RIGHT_ARM));
        }
    }

    public static final class BurnedUpperArm extends FormattedBurn {
        public BurnedUpperArm() {
            super(Set.of(UPPER_LEFT_ARM, UPPER_RIGHT_ARM));
        }
    }

    public static final class FracturedUpperArm extends FormattedFracture {
        public FracturedUpperArm() {
            super(FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(UPPER_LEFT_ARM, UPPER_RIGHT_ARM));
        }
    }

    public static final class FracturedElbow extends FormattedFracture {
        public FracturedElbow() {
            super(FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_ELBOW, RIGHT_ELBOW));
        }
    }

    public static final class FracturedShoulder extends FormattedFracture {
        public FracturedShoulder() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_SHOULDER, RIGHT_SHOULDER));
        }
    }

    public static final class CompoundFracturedShoulder extends FormattedFracture {
        public CompoundFracturedShoulder() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_SHOULDER, RIGHT_SHOULDER));
        }
    }

    // Hand injuries
    public static final class SeveredHand extends FormattedSever {
        public SeveredHand() {
            super(Set.of(LEFT_HAND, RIGHT_HAND));
        }
    }

    public static final class HandBurn extends FormattedBurn {
        public HandBurn() {
            super(Set.of(LEFT_HAND, RIGHT_HAND));
        }
    }

    public static final class FracturedHand extends FormattedFracture {
        public FracturedHand() {
            super(FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_HAND, RIGHT_HAND));
        }
    }

    public static final class FracturedWrist extends FormattedFracture {
        public FracturedWrist() {
            super(FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_WRIST, RIGHT_WRIST));
        }
    }

    public static final class FracturedForearm extends FormattedFracture {
        public FracturedForearm() {
            super(FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_FOREARM, RIGHT_FOREARM));
        }
    }

    public static final class CompoundFracturedForearm extends FormattedFracture {
        public CompoundFracturedForearm() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_FOREARM, RIGHT_FOREARM));
        }
    }

    // Leg injuries
    public static final class SeveredLeg extends FormattedSever {
        public SeveredLeg() {
            super(Set.of(LEFT_LEG, RIGHT_LEG));
        }
    }

    public static final class ThighBurn extends FormattedBurn {
        public ThighBurn() {
            super(Set.of(LEFT_THIGH, RIGHT_THIGH));
        }
    }

    public static final class BruisedFemur extends BaseInjury {
        public BruisedFemur() {
            super(BONE_BRUISE_HEALING_DAYS, false, MINOR,
                  NONE, Set.of(LEFT_FEMUR, RIGHT_FEMUR));
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
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_FEMUR, RIGHT_FEMUR));
        }
    }

    public static final class CompoundFracturedFemur extends FormattedFracture {
        public CompoundFracturedFemur() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_FEMUR, RIGHT_FEMUR));
        }
    }

    public static final class FracturedHip extends FormattedFracture {
        public FracturedHip() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_HIP, RIGHT_HIP));
        }
    }

    // Foot injuries
    public static final class SeveredFoot extends FormattedSever {
        public SeveredFoot() {
            super(Set.of(LEFT_FOOT, RIGHT_FOOT));
        }
    }

    public static final class CalfBurn extends FormattedBurn {
        public CalfBurn() {
            super(Set.of(LEFT_CALF, RIGHT_CALF));
        }
    }

    public static final class FracturedFoot extends FormattedFracture {
        public FracturedFoot() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_FOOT, RIGHT_FOOT));
        }
    }

    public static final class FracturedAnkle extends FormattedFracture {
        public FracturedAnkle() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_ANKLE, RIGHT_ANKLE));
        }
    }

    public static final class FracturedKnee extends FormattedFracture {
        public FracturedKnee() {
            super(FRACTURE_HEALING_DAYS, FRACTURE_LIMB,
                  Set.of(LEFT_KNEE, RIGHT_KNEE));
        }
    }

    public static final class CompoundFracturedShin extends FormattedFracture {
        public CompoundFracturedShin() {
            super(COMPOUND_FRACTURE_HEALING_DAYS, COMPOUND_FRACTURE,
                  Set.of(LEFT_SHIN, RIGHT_SHIN));
        }
    }

    public static final class BloodLoss extends BaseInjury {
        public BloodLoss() {
            super(BLOOD_LOSS_HEALING_DAYS, false, MAJOR,
                  NONE, Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BLOOD_LOSS.simpleName");
            this.fluffText = simpleName;
        }
    }

    // Diseases
    private abstract static class Disease extends BaseInjury {
        protected Disease() {
            super(0, // This is just a placeholder value, we assign it elsewhere
                  false,
                  MINOR,
                  NONE,
                  Set.of(INTERNAL));
            this.maxSeverity = 1;
            this.injurySubType = InjurySubType.DISEASE_GENERIC;
        }
    }

    private abstract static class Bioweapon extends BaseInjury {
        protected Bioweapon() {
            super(0, // This is just a placeholder value, we assign it elsewhere
                  false,
                  MINOR,
                  NONE,
                  Set.of(INTERNAL));
            this.maxSeverity = 1;
            this.injurySubType = InjurySubType.DISEASE_CANON_BIOWEAPON;
        }
    }

    public static final class GrowthsDiscomfort extends Disease {
        GrowthsDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GROWTHS_DISCOMFORT.simpleName");
        }
    }

    public static final class GrowthsSlight extends Disease {
        GrowthsSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GROWTHS_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_GROWTHS_SLIGHT;
        }
    }

    public static final class GrowthsModerate extends Disease {
        GrowthsModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GROWTHS_MODERATE.simpleName");
            this.injuryEffect = DISEASE_GROWTHS_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class GrowthsSevere extends Disease {
        GrowthsSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GROWTHS_SEVERE.simpleName");
            this.injuryEffect = DISEASE_GROWTHS_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class GrowthsDeadly extends Disease {
        GrowthsDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GROWTHS_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class InfectionDiscomfort extends Disease {
        InfectionDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.INFECTION_DISCOMFORT.simpleName");
        }
    }

    public static final class InfectionSlight extends Disease {
        InfectionSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.INFECTION_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_INFECTION_SLIGHT;
        }
    }

    public static final class InfectionModerate extends Disease {
        InfectionModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.INFECTION_MODERATE.simpleName");
            this.injuryEffect = DISEASE_INFECTION_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class InfectionSevere extends Disease {
        InfectionSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.INFECTION_SEVERE.simpleName");
            this.injuryEffect = DISEASE_INFECTION_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class InfectionDeadly extends Disease {
        InfectionDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.INFECTION_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class HearingDiscomfort extends Disease {
        HearingDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_DISCOMFORT.simpleName");
        }
    }

    public static final class HearingSlight extends Disease {
        HearingSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_HEARING_SLIGHT;
        }
    }

    public static final class HearingModerate extends Disease {
        HearingModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_MODERATE.simpleName");
            this.injuryEffect = DISEASE_HEARING_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class HearingSevere extends Disease {
        HearingSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_SEVERE.simpleName");
            this.injuryEffect = DISEASE_HEARING_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class HearingDeadly extends Disease {
        HearingDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEARING_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class WeaknessDiscomfort extends Disease {
        WeaknessDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WEAKNESS_DISCOMFORT.simpleName");
        }
    }

    public static final class WeaknessSlight extends Disease {
        WeaknessSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WEAKNESS_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_WEAKNESS_SLIGHT;
        }
    }

    public static final class WeaknessModerate extends Disease {
        WeaknessModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WEAKNESS_MODERATE.simpleName");
            this.injuryEffect = DISEASE_WEAKNESS_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class WeaknessSevere extends Disease {
        WeaknessSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WEAKNESS_SEVERE.simpleName");
            this.injuryEffect = DISEASE_WEAKNESS_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class WeaknessDeadly extends Disease {
        WeaknessDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WEAKNESS_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class SoresDiscomfort extends Disease {
        SoresDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SORES_DISCOMFORT.simpleName");
        }
    }

    public static final class SoresSlight extends Disease {
        SoresSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SORES_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_SORES_SLIGHT;
        }
    }

    public static final class SoresModerate extends Disease {
        SoresModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SORES_MODERATE.simpleName");
            this.injuryEffect = DISEASE_SORES_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class SoresSevere extends Disease {
        SoresSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SORES_SEVERE.simpleName");
            this.injuryEffect = DISEASE_SORES_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class SoresDeadly extends Disease {
        SoresDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SORES_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class FluDiscomfort extends Disease {
        FluDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FLU_DISCOMFORT.simpleName");
        }
    }

    public static final class FluSlight extends Disease {
        FluSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FLU_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_FLU_SLIGHT;
        }
    }

    public static final class FluModerate extends Disease {
        FluModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FLU_MODERATE.simpleName");
            this.injuryEffect = DISEASE_FLU_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class FluSevere extends Disease {
        FluSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FLU_SEVERE.simpleName");
            this.injuryEffect = DISEASE_FLU_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class FluDeadly extends Disease {
        FluDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FLU_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class SightDiscomfort extends Disease {
        SightDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SIGHT_DISCOMFORT.simpleName");
        }
    }

    public static final class SightSlight extends Disease {
        SightSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SIGHT_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_SIGHT_SLIGHT;
        }
    }

    public static final class SightModerate extends Disease {
        SightModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SIGHT_MODERATE.simpleName");
            this.injuryEffect = DISEASE_SIGHT_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class SightSevere extends Disease {
        SightSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SIGHT_SEVERE.simpleName");
            this.injuryEffect = DISEASE_SIGHT_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class SightDeadly extends Disease {
        SightDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SIGHT_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class TremorsDiscomfort extends Disease {
        TremorsDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TREMORS_DISCOMFORT.simpleName");
        }
    }

    public static final class TremorsSlight extends Disease {
        TremorsSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TREMORS_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_TREMORS_SLIGHT;
        }
    }

    public static final class TremorsModerate extends Disease {
        TremorsModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TREMORS_MODERATE.simpleName");
            this.injuryEffect = DISEASE_TREMORS_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class TremorsSevere extends Disease {
        TremorsSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TREMORS_SEVERE.simpleName");
            this.injuryEffect = DISEASE_TREMORS_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class TremorsDeadly extends Disease {
        TremorsDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TREMORS_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class BreathingDiscomfort extends Disease {
        BreathingDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BREATHING_DISCOMFORT.simpleName");
        }
    }

    public static final class BreathingSlight extends Disease {
        BreathingSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BREATHING_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_BREATHING_SLIGHT;
        }
    }

    public static final class BreathingModerate extends Disease {
        BreathingModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BREATHING_MODERATE.simpleName");
            this.injuryEffect = DISEASE_BREATHING_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class BreathingSevere extends Disease {
        BreathingSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BREATHING_SEVERE.simpleName");
            this.injuryEffect = DISEASE_BREATHING_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class BreathingDeadly extends Disease {
        BreathingDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BREATHING_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class HemophiliaDiscomfort extends Disease {
        HemophiliaDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEMOPHILIA_DISCOMFORT.simpleName");
        }
    }

    public static final class HemophiliaSlight extends Disease {
        HemophiliaSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEMOPHILIA_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_HEMOPHILIA_SLIGHT;
        }
    }

    public static final class HemophiliaModerate extends Disease {
        HemophiliaModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEMOPHILIA_MODERATE.simpleName");
            this.injuryEffect = DISEASE_HEMOPHILIA_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class HemophiliaSevere extends Disease {
        HemophiliaSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEMOPHILIA_SEVERE.simpleName");
            this.injuryEffect = DISEASE_HEMOPHILIA_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class HemophiliaDeadly extends Disease {
        HemophiliaDeadly() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HEMOPHILIA_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
            this.level = DEADLY;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class VenerealDiscomfort extends Disease {
        VenerealDiscomfort() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.VENEREAL_DISCOMFORT.simpleName");
        }
    }

    public static final class VenerealSlight extends Disease {
        VenerealSlight() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.VENEREAL_SLIGHT.simpleName");
            this.injuryEffect = DISEASE_VENEREAL_SLIGHT;
        }
    }

    public static final class VenerealModerate extends Disease {
        VenerealModerate() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.VENEREAL_MODERATE.simpleName");
            this.injuryEffect = DISEASE_VENEREAL_MODERATE;
            this.level = MAJOR;
        }
    }

    public static final class VenerealSevere extends Disease {
        VenerealSevere() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.VENEREAL_SEVERE.simpleName");
            this.injuryEffect = DISEASE_VENEREAL_SEVERE;
            this.level = MAJOR;
        }
    }

    public static final class VenerealDeadly extends Disease {
        VenerealDeadly() {
            super();
            this.level = DEADLY;
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.VENEREAL_DEADLY.simpleName");
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class AlarionHantaVirus extends Bioweapon {
        AlarionHantaVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ALARION_HANTA_VIRUS.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class AlbieroConsumption extends Disease {
        AlbieroConsumption() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ALBIERO_CONSUMPTION.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class AlgediBloodBurn extends Disease {
        AlgediBloodBurn() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ALGEDI_BLOOD_BURN.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class AnchaVirus extends Disease {
        AnchaVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ANCHA_VIRUS.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class BetholdSyndrome extends Disease {
        BetholdSyndrome() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BETHOLD_SYNDROME.simpleName");
            this.level = CHRONIC;
            this.permanent = true;
        }
    }

    public static final class BlackMarshFever extends Disease {
        BlackMarshFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BLACK_MARSH_FEVER.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class BrisbaneVirus extends Disease {
        BrisbaneVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BRISBANE_VIRUS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_WEAKNESS_SEVERE;
        }
    }

    public static final class ChelosianVirus extends Disease {
        ChelosianVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CHELOSIAN_VIRUS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class ChildusFever extends Disease {
        ChildusFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CHILDUS_FEVER.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_WEAKNESS_SEVERE;
            this.recoveryTime = CHILDUS_FEVER_RECOVERY_TIME;
        }
    }

    public static final class ChungalomeningitisAmaris extends Bioweapon {
        ChungalomeningitisAmaris() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CHUNGALOMENINGITIS_AMARIS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_TREMORS_SEVERE;
        }
    }

    public static final class ChungalomeningitisTraditional extends Disease {
        ChungalomeningitisTraditional() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CHUNGALOMENINGITIS_TRADITIONAL.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_TREMORS_MODERATE;
        }
    }

    public static final class CromartySuperflu extends Disease {
        CromartySuperflu() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CROMARTY_SUPERFLU.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class CurseOfEden extends Disease {
        CurseOfEden() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CURSE_OF_EDEN.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class CurseOfGaledon extends Disease {
        CurseOfGaledon() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CURSE_OF_GALEDON.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class CussetCrud extends Disease {
        CussetCrud() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CUSSET_CRUD.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class DangmarsFever extends Disease {
        DangmarsFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DANGMARS_FEVER.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class DarrsDisease extends Disease {
        DarrsDisease() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DARRS_DISEASE.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class DelphiCurse extends Disease {
        DelphiCurse() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DELPHI_CURSE.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_WEAKNESS_MODERATE;
            this.permanent = true;
        }
    }

    public static final class Devilitch extends Disease {
        Devilitch() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DEVILITCH.simpleName");
            this.level = CHRONIC;
            this.injuryEffect = DISEASE_SORES_SLIGHT;
        }
    }

    public static final class DowningPoltursDisease extends Disease {
        DowningPoltursDisease() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DOWNING_POLTURS_DISEASE.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class EdisonWhiteFlu extends Disease {
        EdisonWhiteFlu() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.EDISON_WHITE_FLU.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_SEVERE;
        }
    }

    public static final class EltaninBrainFever extends Disease {
        EltaninBrainFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELTANIN_BRAIN_FEVER.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_SIGHT_SEVERE;
        }
    }

    public static final class FenrisPlague extends Disease {
        FenrisPlague() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FENRIS_PLAGUE.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_FLU_MODERATE;
        }
    }

    public static final class GalaxPathogen extends Bioweapon {
        GalaxPathogen() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GALAX_PATHOGEN.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class GarmsSyndrome extends Disease {
        GarmsSyndrome() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GARMS_SYNDROME.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_TREMORS_MODERATE;
        }
    }

    public static final class GenoanSpinalMeningitis extends Disease {
        GenoanSpinalMeningitis() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.GENOAN_SPINAL_MENINGITIS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_TREMORS_SEVERE;
        }
    }

    public static final class HyborianBloodPlague extends Disease {
        HyborianBloodPlague() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HYBORIAN_BLOOD_PLAGUE.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_INFECTION_SEVERE;
        }
    }

    public static final class KaerPathogen extends Disease {
        KaerPathogen() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.KAER_PATHOGEN.simpleName");
            this.level = CHRONIC;
            this.permanent = true;
        }
    }

    public static final class BirthDefect extends BaseInjury {
        BirthDefect() {
            super(5, true, CHRONIC, InjuryEffect.BIRTH_DEFECT, Set.of(INTERNAL));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIRTH_DEFECT.simpleName");
        }
    }

    public static final class KilenWattsSyndrome extends Disease {
        KilenWattsSyndrome() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.KILEN_WATTS_SYNDROME.simpleName");
            this.level = CHRONIC;
            this.permanent = true;
        }
    }

    public static final class KnightsGrasseSyndrome extends Disease {
        KnightsGrasseSyndrome() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.KNIGHTS_GRASSE_SYNDROME.simpleName");
            this.level = CHRONIC;
            this.permanent = true;
        }
    }

    public static final class LaensRegret extends Disease {
        LaensRegret() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.LAENS_REGRET.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_SORES_SLIGHT;
        }
    }

    public static final class LandmarkSupervirus extends Bioweapon {
        LandmarkSupervirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.LANDMARK_SUPERVIRUS.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class MiaplacidusPlague extends Disease {
        MiaplacidusPlague() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MIAPLACIDUS_PLAGUE.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_FLU_SLIGHT;
        }
    }

    public static final class NeisseriaMalthusia extends Disease {
        NeisseriaMalthusia() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.NEISSERIA_MALTHUSIA.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_VENEREAL_MODERATE;
        }
    }

    public static final class NeoSmallpox extends Disease {
        NeoSmallpox() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.NEO_SMALLPOX.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class NotilicSweats extends Disease {
        NotilicSweats() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.NOTILC_SWEATS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_TREMORS_SEVERE;
        }
    }

    public static final class NykvarnVirus extends Disease {
        NykvarnVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.NYKVARN_VIRUS.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class OckhamsBloodDisease extends Disease {
        OckhamsBloodDisease() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.OCKHAMS_BLOOD_DISEASE.simpleName");
            this.level = CHRONIC;
            this.injuryEffect = DISEASE_WEAKNESS_SLIGHT;
            this.permanent = true;
        }
    }

    public static final class PingreeFever extends Disease {
        PingreeFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PINGREE_FEVER.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class RedburnVirus extends Bioweapon {
        RedburnVirus() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.REDBURN_VIRUS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_BREATHING_SEVERE;
        }
    }

    public static final class RocklandFever extends Disease {
        RocklandFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ROCKLAND_FEVER.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_SORES_MODERATE;
        }
    }

    public static final class ScourgePlague extends Bioweapon {
        ScourgePlague() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SCOURGE_PLAGUE.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    public static final class SkokieShivers extends Disease {
        SkokieShivers() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.SKOKIE_SHIVERS.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_TREMORS_SEVERE;
        }
    }

    public static final class ToxoplasmaGondiiHardcorea extends Disease {
        ToxoplasmaGondiiHardcorea() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.TOXOPLASMA_GONDII_HARDCOREA.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_FLU_MODERATE;
        }
    }

    public static final class UnoleFlu extends Disease {
        UnoleFlu() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.UNOLE_FLU.simpleName");
            this.level = MINOR;
            this.injuryEffect = DISEASE_FLU_SLIGHT;
        }
    }

    public static final class WinsonsRegret extends Disease {
        WinsonsRegret() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WINSONS_REGRET.simpleName");
            this.level = MAJOR;
            this.injuryEffect = DISEASE_INFECTION_SEVERE;
        }
    }

    public static final class YimpiseeFever extends Disease {
        YimpiseeFever() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.YIMPISEE_FEVER.simpleName");
            this.level = DEADLY;
            this.injuryEffect = DISEASE_DEADLY;
            this.permanent = true;
        }

        @Override
        public boolean impliesDead(BodyLocation loc) {
            return true;
        }
    }

    // Prosthetics
    private abstract static class Prosthetic extends BaseInjury {
        protected Prosthetic() {
            super(SEVER_HEALING_DAYS, // As a permanent 'injury' healing time is largely irrelevant
                  true,
                  CHRONIC,
                  NONE,
                  Set.of(INTERNAL)); // A placeholder effect, we replace it later
            this.maxSeverity = 0; // Prosthetics don't count towards the character's "hits"
            this.injurySubType = InjurySubType.PROSTHETIC_GENERIC;
        }
    }

    public static final class WoodenArm extends Prosthetic {
        WoodenArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WOODEN_LIMB.simpleName");
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_1_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WOODEN_LIMB.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class HookHand extends Prosthetic {
        HookHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HOOK_HAND.simpleName");
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_1_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.HOOK_HAND.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class PegLeg extends Prosthetic {
        PegLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PEG_LEG.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_1_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PEG_LEG.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class WoodenFoot extends Prosthetic {
        WoodenFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WOODEN_LIMB.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_1_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.WOODEN_LIMB.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class SimpleArm extends Prosthetic {
        SimpleArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_2_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class SimpleClawHand extends Prosthetic {
        SimpleClawHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_2_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class SimpleLeg extends Prosthetic {
        SimpleLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_2_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class SimpleFoot extends Prosthetic {
        SimpleFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_FOOT, RIGHT_FOOT);
            this.injuryEffect = TYPE_2_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PLASTIC_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ProstheticArm extends Prosthetic {
        ProstheticArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_3_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ProstheticHand extends Prosthetic {
        ProstheticHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_3_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ProstheticLeg extends Prosthetic {
        ProstheticLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_3_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ProstheticFoot extends Prosthetic {
        ProstheticFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_FOOT, RIGHT_FOOT);
            this.injuryEffect = TYPE_3_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COMPLEX_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class AdvancedProstheticArm extends Prosthetic {
        AdvancedProstheticArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_4_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class AdvancedProstheticHand extends Prosthetic {
        AdvancedProstheticHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_4_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class AdvancedProstheticLeg extends Prosthetic {
        AdvancedProstheticLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_4_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class AdvancedProstheticFoot extends Prosthetic {
        AdvancedProstheticFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_FOOT, RIGHT_FOOT);
            this.injuryEffect = TYPE_4_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ADVANCED_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class MyomerArm extends Prosthetic {
        MyomerArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_5_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class MyomerHand extends Prosthetic {
        MyomerHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_5_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class MyomerLeg extends Prosthetic {
        MyomerLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_5_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class MyomerFoot extends Prosthetic {
        MyomerFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_FOOT, RIGHT_FOOT);
            this.injuryEffect = TYPE_5_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ClonedArm extends Prosthetic {
        ClonedArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName");
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TYPE_6_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ClonedHand extends Prosthetic {
        ClonedHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName");
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = TYPE_6_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ClonedLeg extends Prosthetic {
        ClonedLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TYPE_6_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ClonedFoot extends Prosthetic {
        ClonedFoot() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName");
            this.allowedLocations = Set.of(LEFT_FOOT, RIGHT_FOOT);
            this.injuryEffect = TYPE_6_LIMB_REPLACEMENT;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class EyeImplant extends Prosthetic {
        EyeImplant() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.EYE_IMPLANT.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = TYPE_2_SENSORY_REPLACEMENT;
        }
    }

    public static final class BionicEar extends Prosthetic {
        BionicEar() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIONIC_EAR.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = TYPE_3_SENSORY_REPLACEMENT;
        }
    }

    public static final class BionicEye extends Prosthetic {
        BionicEye() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIONIC_EYE.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = TYPE_4_SENSORY_REPLACEMENT;
        }
    }

    public static final class BionicHeart extends Prosthetic {
        BionicHeart() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIONIC_HEART.simpleName");
            this.allowedLocations = Set.of(HEART);
        }
    }

    public static final class BionicLungs extends Prosthetic {
        BionicLungs() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIONIC_LUNGS.simpleName");
            this.allowedLocations = Set.of(LUNGS);
        }
    }

    public static final class BionicOrganOther extends Prosthetic {
        BionicOrganOther() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BIONIC_ORGAN_OTHER.simpleName");
            this.allowedLocations = Set.of(ORGANS);
        }
    }

    public static final class CosmeticSurgery extends Prosthetic {
        CosmeticSurgery() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COSMETIC_SURGERY.simpleName");
            this.allowedLocations = Set.of(FACE, ABDOMEN, CHEST, LEFT_ARM, RIGHT_ARM, LEFT_HAND,
                  RIGHT_HAND, LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = NONE;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COSMETIC_SURGERY.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ClonedLimbRecovery extends BaseInjury {
        ClonedLimbRecovery() {
            super(CLONED_LIMB_HEALING_DAYS, false, MINOR, SEVERED, Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.CLONED_LIMB_RECOVERY.simpleName");
        }
    }

    public static final class ReplacementLimbRecovery extends BaseInjury {
        ReplacementLimbRecovery() {
            super(REPLACEMENT_LIMB_HEALING_DAYS, false, MINOR, SEVERED, Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.REPLACEMENT_LIMB_RECOVERY.simpleName");
        }
    }

    public static final class ReplacementOrganRecovery extends BaseInjury {
        ReplacementOrganRecovery() {
            super(REPLACEMENT_LIMB_HEALING_DAYS,
                  false,
                  MINOR,
                  INTERNAL_BLEEDING,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.REPLACEMENT_ORGAN_RECOVERY.simpleName");
        }
    }

    public static final class CosmeticSurgeryRecovery extends BaseInjury {
        CosmeticSurgeryRecovery() {
            super(COSMETIC_SURGERY_RECOVERY_HEALING_DAYS,
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COSMETIC_SURGERY_RECOVERY.simpleName");
        }
    }

    public static final class FailedSurgeryRecovery extends BaseInjury {
        FailedSurgeryRecovery() {
            super(COSMETIC_SURGERY_RECOVERY_HEALING_DAYS, // Not a mistake
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.FAILED_SURGERY_RECOVERY.simpleName");
        }
    }

    public static final class ElectiveMyomerArm extends Prosthetic {
        ElectiveMyomerArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = MYOMER_IMPLANT_ARM;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerArmorArm extends Prosthetic {
        DermalMyomerArmorArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_ARMOR.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = MYOMER_IMPLANT_ARM;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_ARMOR.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerCamoArm extends Prosthetic {
        DermalMyomerCamoArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_CAMO.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = MYOMER_IMPLANT_ARM;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_CAMO.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerTripleArm extends Prosthetic {
        DermalMyomerTripleArm() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_TRIPLE.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_ARM, RIGHT_ARM);
            this.injuryEffect = TRIPLE_STRENGTH_MYOMER_IMPLANT_ARM;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_TRIPLE.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ElectiveMyomerHand extends Prosthetic {
        ElectiveMyomerHand() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_HAND, RIGHT_HAND);
            this.injuryEffect = MYOMER_IMPLANT_HAND;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class ElectiveMyomerLeg extends Prosthetic {
        ElectiveMyomerLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = MYOMER_IMPLANT_LEG;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_MYOMER.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerArmorLeg extends Prosthetic {
        DermalMyomerArmorLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_ARMOR.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = MYOMER_IMPLANT_LEG;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_ARMOR.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerCamoLeg extends Prosthetic {
        DermalMyomerCamoLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_CAMO.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = MYOMER_IMPLANT_LEG;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_CAMO.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class DermalMyomerTripleLeg extends Prosthetic {
        DermalMyomerTripleLeg() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_TRIPLE.simpleName");
            this.injurySubType = PROSTHETIC_MYOMER;
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = TRIPLE_STRENGTH_MYOMER_IMPLANT_LEG;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DERMAL_MYOMER_TRIPLE.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class BoneReinforcement extends Prosthetic {
        BoneReinforcement() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.BONE_REINFORCEMENT.simpleName");
            this.allowedLocations = Set.of(BONES);
            this.injuryEffect = InjuryEffect.BONE_REINFORCEMENT;
        }
    }

    public static final class OrganFiltrationImplant extends Prosthetic {
        OrganFiltrationImplant() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.LIVER_FILTRATION_IMPLANT.simpleName");
            this.allowedLocations = Set.of(ORGANS);
            this.injuryEffect = InjuryEffect.LIVER_FILTRATION_IMPLANT;
        }
    }

    public static final class BionicLungsWithType1Filter extends Prosthetic {
        BionicLungsWithType1Filter() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_1_FILTER.simpleName");
            this.allowedLocations = Set.of(LUNGS);
            this.injuryEffect = TYPE_1_SURVIVAL_IMPLANT;
        }
    }

    public static final class BionicLungsWithType2Filter extends Prosthetic {
        BionicLungsWithType2Filter() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_2_FILTER.simpleName");
            this.allowedLocations = Set.of(LUNGS);
            this.injuryEffect = TYPE_2_SURVIVAL_IMPLANT;
        }
    }

    public static final class BionicLungsWithType3Filter extends Prosthetic {
        BionicLungsWithType3Filter() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_3_FILTER.simpleName");
            this.allowedLocations = Set.of(LUNGS);
            this.injuryEffect = TYPE_3_SURVIVAL_IMPLANT;
        }
    }

    public static final class CyberneticEyeEMIR extends Prosthetic {
        CyberneticEyeEMIR() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EYE_EM_IR.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = NONE;
        }
    }

    public static final class CyberneticEyeTelescope extends Prosthetic {
        CyberneticEyeTelescope() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EYE_TELESCOPE.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = InjuryEffect.EYESIGHT_ENHANCED;
        }
    }

    public static final class CyberneticEyeLaser extends Prosthetic {
        CyberneticEyeLaser() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EYE_LASER.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = InjuryEffect.EYESIGHT_LASER;
        }
    }

    public static final class CyberneticEyeMulti extends Prosthetic {
        CyberneticEyeMulti() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EYE_MULTI.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = InjuryEffect.EYESIGHT_MULTI;
        }
    }

    public static final class CyberneticEyeMultiEnhanced extends Prosthetic {
        CyberneticEyeMultiEnhanced() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EYE_MULTI_ENHANCED.simpleName");
            this.allowedLocations = Set.of(EYES);
            this.injuryEffect = InjuryEffect.EYESIGHT_MULTI;
        }
    }

    public static final class CyberneticEarCommunications extends Prosthetic {
        CyberneticEarCommunications() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EAR_COMMUNICATIONS.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = NONE;
        }
    }

    public static final class CyberneticEarBoostedCommunications extends Prosthetic {
        CyberneticEarBoostedCommunications() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = NONE;
        }
    }

    public static final class CyberneticEarEnhanced extends Prosthetic {
        CyberneticEarEnhanced() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EAR_ENHANCED.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = InjuryEffect.HEARING_ENHANCED;
        }
    }

    public static final class CyberneticEarSignal extends Prosthetic {
        CyberneticEarSignal() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EAR_SIGNAL.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = NONE;
        }
    }

    public static final class CyberneticEarMulti extends Prosthetic {
        CyberneticEarMulti() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_EAR_MULTI.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = InjuryEffect.HEARING_ENHANCED;
        }
    }

    public static final class CyberneticSpeechImplant extends Prosthetic {
        CyberneticSpeechImplant() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CYBERNETIC_SPEECH_IMPLANT.simpleName");
            this.allowedLocations = Set.of(MOUTH);
            this.injuryEffect = InjuryEffect.CYBERNETIC_SPEECH_IMPLANT;
        }
    }

    public static final class PheromoneEffuser extends Prosthetic {
        PheromoneEffuser() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.PHEROMONE_EFFUSER.simpleName");
            this.allowedLocations = Set.of(INTERNAL);
            this.injuryEffect = InjuryEffect.PHEROMONE_EFFUSER;
        }
    }

    public static final class SecondaryPowerSupply extends Prosthetic {
        SecondaryPowerSupply() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.SECONDARY_POWER_SUPPLY.simpleName");
            this.allowedLocations = Set.of(INTERNAL);
            this.injuryEffect = InjuryEffect.NONE;
        }
    }

    public static final class CosmeticBeautyEnhancement extends Prosthetic {
        CosmeticBeautyEnhancement() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.COSMETIC_BEAUTY_ENHANCEMENT.simpleName");
            this.allowedLocations = Set.of(FACE);
            this.injuryEffect = InjuryEffect.COSMETIC_BEAUTY_ENHANCEMENT;
        }
    }

    public static final class CosmeticHorrorEnhancement extends Prosthetic {
        CosmeticHorrorEnhancement() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.COSMETIC_HORROR_ENHANCEMENT.simpleName");
            this.allowedLocations = Set.of(FACE);
            this.injuryEffect = InjuryEffect.COSMETIC_HORROR_ENHANCEMENT;
        }
    }

    public static final class CosmeticTailProsthetic extends Prosthetic {
        CosmeticTailProsthetic() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.COSMETIC_TAIL_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(RUMP);
            this.injuryEffect = COSMETIC_ANIMAL_LIMB_PROSTHETIC;
        }
    }

    public static final class CosmeticAnimalEarProsthetic extends Prosthetic {
        CosmeticAnimalEarProsthetic() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.COSMETIC_ANIMAL_EAR_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(EARS);
            this.injuryEffect = NONE;
        }
    }

    public static final class PrototypeVDNI extends Prosthetic {
        PrototypeVDNI() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.PROTOTYPE_VDNI.simpleName");
            this.injurySubType = IMPLANT_VDNI;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = NONE;
        }
    }

    public static final class VehicularDNI extends Prosthetic {
        VehicularDNI() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.VEHICULAR_DNI.simpleName");
            this.injurySubType = IMPLANT_VDNI;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = NONE;
        }
    }

    public static final class BufferedVDNI extends Prosthetic {
        BufferedVDNI() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.BUFFERED_VDNI.simpleName");
            this.injurySubType = IMPLANT_VDNI;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = NONE;
        }
    }

    public static final class BufferedVDNITripleCore extends Prosthetic {
        BufferedVDNITripleCore() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.BUFFERED_VDNI_TRIPLE_CORE.simpleName");
            this.injurySubType = IMPLANT_VDNI;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = TRIPLE_CORE_PROCESSOR;
        }
    }

    public static final class PainShunt extends Prosthetic {
        PainShunt() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.PAIN_SHUNT.simpleName");
            this.injurySubType = IMPLANT_GENERIC;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = InjuryEffect.PAIN_SHUNT;
        }
    }

    public static final class ImplantRemovalRecovery extends BaseInjury {
        ImplantRemovalRecovery() {
            super(SEVER_HEALING_DAYS, false, DEADLY, InjuryEffect.BRAIN_TRAUMA, Set.of(BRAIN));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.IMPLANT_REMOVAL_RECOVERY.simpleName");
            this.injurySubType = NORMAL;
        }
    }

    public static final class CosmeticLegProsthetic extends Prosthetic {
        CosmeticLegProsthetic() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.COSMETIC_ANIMAL_LEG_PROSTHETIC.simpleName");
            this.allowedLocations = Set.of(LEFT_LEG, RIGHT_LEG);
            this.injuryEffect = COSMETIC_ANIMAL_LIMB_PROSTHETIC;
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AlternateInjuries.COSMETIC_ANIMAL_LEG_PROSTHETIC.simpleName",
                  Utilities.capitalize(loc.locationName()));
        }
    }

    public static final class EnhancedImagingImplant extends Prosthetic {
        EnhancedImagingImplant() {
            super();
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ENHANCED_IMAGING.simpleName");
            this.injurySubType = IMPLANT_GENERIC;
            this.allowedLocations = Set.of(BRAIN);
            this.injuryEffect = NONE;
        }
    }

    public static final class ElectiveImplantRecovery extends BaseInjury {
        ElectiveImplantRecovery() {
            super(ELECTIVE_IMPLANT_RECOVERY_HEALING_DAYS, // Not a mistake
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.ELECTIVE_IMPLANT_RECOVERY.simpleName");
        }
    }

    public static final class EIImplantRecovery extends BaseInjury {
        EIImplantRecovery() {
            super(ENHANCED_IMAGING_IMPLANT_RECOVERY_HEALING_DAYS, // Not a mistake
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.EI_IMPLANT_RECOVERY.simpleName");
        }
    }

    public static final class PainShuntRecovery extends BaseInjury {
        PainShuntRecovery() {
            super(PAIN_SHUNT_RECOVERY_HEALING_DAYS, // Not a mistake
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.PAIN_SHUNT_RECOVERY.simpleName");
        }
    }

    public static final class DiscontinuationSyndrome extends BaseInjury {
        DiscontinuationSyndrome() {
            super(DISCONTINUATION_SYNDROME_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.DISCONTINUATION_SYNDROME.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class PostpartumRecovery extends BaseInjury {
        PostpartumRecovery() {
            super(POSTPARTUM_RECOVERY_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.POSTPARTUM_RECOVERY.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class TransitDisorientationSyndrome extends BaseInjury {
        TransitDisorientationSyndrome() {
            super(TRANSIT_DISORIENTATION_SYNDROME_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.TRANSIT_DISORIENTATION_SYNDROME.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class CripplingFlashbacks extends BaseInjury {
        CripplingFlashbacks() {
            super(WEEKLY_CHECK_ILLNESS_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CRIPPLING_FLASHBACKS.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class ChildlikeRegression extends BaseInjury {
        ChildlikeRegression() {
            super(WEEKLY_CHECK_ILLNESS_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CHILDLIKE_REGRESSION.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class ChronicDisassociation extends BaseInjury {
        ChronicDisassociation() {
            super(WEEKLY_CHECK_ILLNESS_HEALING_DAYS,
                  false,
                  CHRONIC,
                  InjuryEffect.STRESS,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.CATATONIA.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class TerribleBruises extends BaseInjury {
        TerribleBruises() {
            super(WEEKLY_CHECK_ILLNESS_HEALING_DAYS,
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE,
                  "AlternateInjuries.TERRIBLE_BRUISES.simpleName");
            this.injurySubType = FLAW;
        }
    }

    public static final class OldWound extends BaseInjury {
        OldWound() {
            super(OLD_WOUND_HEALING_DAYS,
                  false,
                  MINOR,
                  NONE,
                  Set.of(GENERIC));
            this.simpleName = getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.OLD_WOUND.simpleName");
        }
    }
}
