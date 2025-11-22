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
import static java.lang.Math.round;
import static megamek.common.options.OptionsConstants.*;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_ATTRACTIVE;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_POISON_RESISTANCE;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_PAINKILLER_ADDICTION;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_UNATTRACTIVE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechRating;
import megamek.common.options.IOption;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * Enumeration representing the various types of prosthetics and artificial body replacements available in the Alternate
 * Advanced Medical system.
 *
 * <p>Each {@code ProstheticType} entry defines attributes such as its base cost, required surgery level, associated
 * {@link InjuryType}, and availability across different eras and factions. These values are used by the MekHQ medical
 * framework to determine purchase cost, eligibility, and in-game behavior.</p>
 *
 * <p>Prosthetic types range from crude wooden limbs to advanced cloned or myomer replacements. Each type also
 * encodes its associated technology rating, factional exclusivity, and temporal availability.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public enum ProstheticType {
    WOODEN_ARM("WOODEN_ARM",
          1,
          2,
          AlternateInjuries.WOODEN_ARM,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    HOOK_HAND("HOOK_HAND",
          1,
          2,
          AlternateInjuries.HOOK_HAND,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    PEG_LEG("PEG_LEG",
          1,
          2,
          AlternateInjuries.PEG_LEG,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    WOODEN_FOOT("WOODEN_FOOT",
          1,
          2,
          AlternateInjuries.WOODEN_FOOT,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    SIMPLE_ARM("SIMPLE_ARM",
          2,
          2,
          AlternateInjuries.SIMPLE_ARM,
          Money.of(750),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_CLAW_HAND("SIMPLE_CLAW_HAND",
          2,
          2,
          AlternateInjuries.SIMPLE_CLAW_HAND,
          Money.of(750),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_LEG("SIMPLE_LEG",
          2,
          2,
          AlternateInjuries.SIMPLE_LEG,
          Money.of(250),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_FOOT("SIMPLE_FOOT",
          2,
          2,
          AlternateInjuries.SIMPLE_FOOT,
          Money.of(250),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    PROSTHETIC_ARM("PROSTHETIC_ARM",
          3,
          5,
          AlternateInjuries.PROSTHETIC_ARM,
          Money.of(7500),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_HAND("PROSTHETIC_HAND",
          3,
          5,
          AlternateInjuries.PROSTHETIC_HAND,
          Money.of(7500),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_LEG("PROSTHETIC_LEG",
          3,
          5,
          AlternateInjuries.PROSTHETIC_LEG,
          Money.of(10000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_FOOT("PROSTHETIC_FOOT",
          3,
          5,
          AlternateInjuries.PROSTHETIC_FOOT,
          Money.of(10000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    ADVANCED_PROSTHETIC_ARM("ADVANCED_PROSTHETIC_ARM",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_ARM,
          Money.of(25000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_HAND("ADVANCED_PROSTHETIC_HAND",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_HAND,
          Money.of(25000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_LEG("ADVANCED_PROSTHETIC_LEG",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_LEG,
          Money.of(17500),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_FOOT("ADVANCED_PROSTHETIC_FOOT",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_FOOT,
          Money.of(17500),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    MYOMER_ARM("MYOMER_ARM",
          5,
          5,
          AlternateInjuries.MYOMER_ARM,
          Money.of(200000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_HAND("MYOMER_HAND",
          5,
          5,
          AlternateInjuries.MYOMER_HAND,
          Money.of(100000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_LEG("MYOMER_LEG",
          5,
          5,
          AlternateInjuries.MYOMER_LEG,
          Money.of(125000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_FOOT("MYOMER_FOOT",
          5,
          5,
          AlternateInjuries.MYOMER_FOOT,
          Money.of(50000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    CLONED_ARM("CLONED_ARM",
          6,
          5,
          AlternateInjuries.CLONED_ARM,
          Money.of(500000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_HAND("CLONED_HAND",
          6,
          5,
          AlternateInjuries.CLONED_HAND,
          Money.of(300000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_LEG("CLONED_LEG",
          6,
          5,
          AlternateInjuries.CLONED_LEG,
          Money.of(350000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_FOOT("CLONED_FOOT",
          6,
          5,
          AlternateInjuries.CLONED_FOOT,
          Money.of(50000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    EYE_IMPLANT("EYE_IMPLANT",
          2,
          2,
          AlternateInjuries.EYE_IMPLANT,
          Money.of(350),
          TechRating.E,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_EAR("BIONIC_EAR",
          3,
          5,
          AlternateInjuries.BIONIC_EAR,
          Money.of(100000),
          TechRating.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_EYE("BIONIC_EYE",
          4,
          5,
          AlternateInjuries.BIONIC_EYE,
          Money.of(220000),
          TechRating.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_HEART("BIONIC_HEART",
          3,
          5,
          AlternateInjuries.BIONIC_HEART,
          Money.of(500000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_LUNGS("BIONIC_LUNGS",
          4,
          5,
          AlternateInjuries.BIONIC_LUNGS,
          Money.of(800000),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    BIONIC_ORGAN_OTHER("BIONIC_ORGAN_OTHER",
          4,
          5,
          AlternateInjuries.BIONIC_ORGAN_OTHER,
          Money.of(750000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false),
    COSMETIC_SURGERY("COSMETIC_SURGERY",
          2,
          2,
          AlternateInjuries.COSMETIC_SURGERY,
          Money.of(2500),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    ELECTIVE_MYOMER_ARM("ELECTIVE_MYOMER_ARM",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_ARM,
          Money.of(300000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ELECTIVE_MYOMER_HAND("ELECTIVE_MYOMER_HAND",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_HAND,
          Money.of(150000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ELECTIVE_MYOMER_LEG("ELECTIVE_MYOMER_LEG",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_LEG,
          Money.of(375000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ENHANCED_IMAGING("ENHANCED_IMAGING",
          5,
          5,
          AlternateInjuries.ENHANCED_IMAGING_IMPLANT,
          Money.of(1500000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          true,
          false,
          List.of(UNOFFICIAL_EI_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BONE_REINFORCEMENT("BONE_REINFORCEMENT",
          4,
          5,
          AlternateInjuries.BONE_REINFORCEMENT,
          Money.of(10000),
          TechRating.D,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    LIVER_FILTRATION_IMPLANT("LIVER_FILTRATION_IMPLANT",
          5,
          5,
          AlternateInjuries.LIVER_FILTRATION_IMPLANT,
          Money.of(10000),
          TechRating.C,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          List.of(),
          List.of(ATOW_POISON_RESISTANCE, COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_1_FILTER("BIONIC_LUNGS_WITH_TYPE_1_FILTER",
          5,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_1_FILTER,
          Money.of(805000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_2_FILTER("BIONIC_LUNGS_WITH_TYPE_2_FILTER",
          5,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_2_FILTER,
          Money.of(815000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_3_FILTER("BIONIC_LUNGS_WITH_TYPE_3_FILTER",
          5,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_3_FILTER,
          Money.of(845000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_EM_IR("CYBERNETIC_EYE_EM_IR",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EYE_EM_IR,
          Money.of(650000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          List.of(MD_CYBER_IMP_VISUAL),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_TELESCOPE("CYBERNETIC_EYE_TELESCOPE",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EYE_TELESCOPE,
          Money.of(450000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          List.of(MD_CYBER_IMP_LASER),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_LASER("CYBERNETIC_EYE_LASER",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EYE_LASER,
          Money.of(600000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          List.of(MD_CYBER_IMP_LASER),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_MULTI("CYBERNETIC_EYE_MULTI",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EYE_MULTI,
          Money.of(1050000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          true,
          List.of(MD_CYBER_IMP_LASER),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_MULTI_ENHANCED("CYBERNETIC_EYE_MULTI_ENHANCED",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EYE_MULTI_ENHANCED,
          Money.of(17000000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          true,
          List.of(MD_CYBER_IMP_LASER, MD_CYBER_IMP_VISUAL),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_SIGNAL("CYBERNETIC_EAR_SIGNAL",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EAR_SIGNAL,
          Money.of(400000),
          TechRating.C,
          AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.C,
          false,
          false,
          List.of(MD_COMM_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_MULTI("CYBERNETIC_EAR_MULTI",
          5,
          5,
          AlternateInjuries.CYBERNETIC_EAR_MULTI,
          Money.of(600000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          true,
          List.of(MD_BOOST_COMM_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_SPEECH_IMPLANT("CYBERNETIC_SPEECH_IMPLANT",
          5,
          5,
          AlternateInjuries.CYBERNETIC_SPEECH_IMPLANT,
          Money.of(200000),
          TechRating.D,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.C,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    PHEROMONE_EFFUSER("PHEROMONE_EFFUSER",
          5,
          5,
          AlternateInjuries.PHEROMONE_EFFUSER,
          Money.of(40000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          true,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_BEAUTY_ENHANCEMENT("COSMETIC_BEAUTY_ENHANCEMENT",
          3,
          5,
          AlternateInjuries.COSMETIC_BEAUTY_ENHANCEMENT,
          Money.of(15000),
          TechRating.B,
          AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false,
          List.of(),
          List.of(ATOW_ATTRACTIVE, COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_HORROR_ENHANCEMENT("COSMETIC_HORROR_ENHANCEMENT",
          3,
          5,
          AlternateInjuries.COSMETIC_HORROR_ENHANCEMENT,
          Money.of(15000),
          TechRating.B,
          AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(FLAW_UNATTRACTIVE, COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_TAIL_PROSTHETIC("COSMETIC_TAIL_PROSTHETIC",
          3,
          5,
          AlternateInjuries.COSMETIC_TAIL_PROSTHETIC,
          Money.of(60000),
          TechRating.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_ANIMAL_EAR_PROSTHETIC("COSMETIC_ANIMAL_EAR_PROSTHETIC",
          3,
          5,
          AlternateInjuries.COSMETIC_ANIMAL_EAR_PROSTHETIC,
          Money.of(60000),
          TechRating.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_ANIMAL_LEG_PROSTHETIC("COSMETIC_ANIMAL_LEG_PROSTHETIC",
          3,
          5,
          AlternateInjuries.COSMETIC_ANIMAL_LEG_PROSTHETIC,
          Money.of(60000),
          TechRating.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    DERMAL_MYOMER_ARM_ARMOR("DERMAL_MYOMER_ARM_ARMOR",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR,
          Money.of(450000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE)),
    DERMAL_MYOMER_ARM_CAMO("DERMAL_MYOMER_LEG_CAMO",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_CAMO,
          Money.of(330000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, MISC_PAIN_RESISTANCE)),
    DERMAL_MYOMER_ARM_TRIPLE("DERMAL_MYOMER_LEG_TRIPLE",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_TRIPLE,
          Money.of(750000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, ATOW_TOUGHNESS)),
    DERMAL_MYOMER_LEG_ARMOR("DERMAL_MYOMER_LEG_ARMOR",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_ARMOR,
          Money.of(562500),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, MISC_PAIN_RESISTANCE)),
    DERMAL_MYOMER_LEG_CAMO("DERMAL_MYOMER_LEG_CAMO",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_CAMO,
          Money.of(412500),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE)),
    DERMAL_MYOMER_LEG_TRIPLE("DERMAL_MYOMER_LEG_TRIPLE",
          5,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_TRIPLE,
          Money.of(937500),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, ATOW_TOUGHNESS)),
    VDNI("VDNI",
          5,
          5,
          AlternateInjuries.VEHICULAR_DNI,
          Money.of(1400000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          true,
          List.of(MD_VDNI),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BUFFERED_VDNI("BUFFERED_VDNI",
          5,
          5,
          AlternateInjuries.BUFFERED_VDNI,
          Money.of(2000000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          true,
          List.of(MD_BVDNI),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BUFFERED_VDNI_TRIPLE_CORE("BUFFERED_VDNI_TRIPLE_CORE",
          5,
          5,
          AlternateInjuries.BUFFERED_VDNI_TRIPLE_CORE,
          Money.of(5000000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          true,
          List.of(MD_BVDNI, MD_TRIPLE_CORE_PROCESSOR),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    PAIN_SHUNT("PAIN_SHUNT",
          5,
          5,
          AlternateInjuries.PAIN_SHUNT,
          Money.of(50000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          true,
          List.of(MD_PAIN_SHUNT),
          List.of(COMPULSION_PAINKILLER_ADDICTION));

    private final String lookupName;
    private final int prostheticType;
    private final int surgeryLevel;
    private final InjuryType injuryType;
    private final Money baseCost;
    private final TechRating technologyRating;
    private final AvailabilityValue availabilityEarly;
    private final AvailabilityValue availabilityMid;
    private final AvailabilityValue availabilityLate;
    private final boolean isComStarOnly;
    private final boolean isClanOnly;
    private final List<String> associatedPilotOptions;
    private final List<String> associatedPersonnelOptions;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ProstheticType";

    // Era boundaries
    private static final int EARLY_ERA_CUTOFF = 2800;
    private static final int LATE_ERA_START = 3051;

    // Availability cost multipliers
    private static final double AVAILABILITY_MULTIPLIER_A = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_B = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_C = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_D = 1.25;
    private static final double AVAILABILITY_MULTIPLIER_E = 1.5;
    private static final double AVAILABILITY_MULTIPLIER_F = 10.0;
    private static final double AVAILABILITY_MULTIPLIER_F_STAR = 0.0;
    private static final double AVAILABILITY_MULTIPLIER_X = 0.0;


    /**
     * Constructs a new {@code ProstheticType} entry.
     *
     * @param lookupName        the resource key for localization and lookup
     * @param prostheticType    the prosthetic tier (as per ATOW)
     * @param surgeryLevel      the minimum medical skill or facility level required
     * @param injuryType        the injury this prosthetic 'inflicts'
     * @param baseCost          the base market price before modifiers (as per ATOW)
     * @param technologyRating  the required technology rating for construction (as per ATOW)
     * @param availabilityEarly availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid   availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate  availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly        whether this item is exclusive to Clan factions (as per ATOW)
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, int prostheticType, int surgeryLevel, InjuryType injuryType, Money baseCost,
          TechRating technologyRating, AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid,
          AvailabilityValue availabilityLate, boolean isClanOnly, boolean isComStarOnly) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.technologyRating = technologyRating;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.associatedPilotOptions = new ArrayList<>();
        this.associatedPersonnelOptions = new ArrayList<>();
    }

    /**
     * Constructs a new {@code ProstheticType} entry.
     *
     * @param lookupName                 the resource key for localization and lookup
     * @param prostheticType             the prosthetic tier (as per ATOW)
     * @param surgeryLevel               the minimum medical skill or facility level required
     * @param injuryType                 the injury this prosthetic 'inflicts'
     * @param baseCost                   the base market price before modifiers (as per ATOW)
     * @param technologyRating           the required technology rating for construction (as per ATOW)
     * @param availabilityEarly          availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid            availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate           availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly                 whether this item is exclusive to Clan factions (as per ATOW)
     * @param associatedPilotOptions     Any Pilot Options that should be added to the character when they receive this
     *                                   prosthetic
     * @param associatedPersonnelOptions Any Personnel Options that should be added to the character when they received
     *                                   this prosthetic
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, int prostheticType, int surgeryLevel, InjuryType injuryType, Money baseCost,
          TechRating technologyRating, AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid,
          AvailabilityValue availabilityLate, boolean isClanOnly, boolean isComStarOnly,
          List<String> associatedPilotOptions, List<String> associatedPersonnelOptions) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.technologyRating = technologyRating;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.associatedPilotOptions = associatedPilotOptions;
        this.associatedPersonnelOptions = associatedPersonnelOptions;
    }

    /** @return the prosthetic classification. */
    public int getProstheticType() {
        return prostheticType;
    }

    /** @return the minimum surgical skill required. */
    public int getSurgeryLevel() {
        return surgeryLevel;
    }

    /**
     * Retrieves all valid body locations this prosthetic can replace, as defined by its associated {@link InjuryType}.
     *
     * @return a set of {@link BodyLocation} values eligible for replacement.
     */
    public Set<BodyLocation> getEligibleLocations() {
        return injuryType.getAllowedLocations();
    }

    /** @return the {@link InjuryType} this prosthetic 'inflicts'. */
    public InjuryType getInjuryType() {
        return injuryType;
    }

    public List<String> getAssociatedPilotOptions() {
        return associatedPilotOptions;
    }

    public List<String> getAssociatedPersonnelOptions() {
        return associatedPersonnelOptions;
    }

    public boolean isElectiveImplant() {
        return this == ELECTIVE_MYOMER_ARM ||
                     this == ELECTIVE_MYOMER_HAND ||
                     this == ELECTIVE_MYOMER_LEG;
    }

    /**
     * Determines if the given faction can access this prosthetic type.
     *
     * @param campaignFaction the faction to check
     *
     * @return {@code true} if the faction can access this prosthetic type; otherwise {@code false}.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isAvailableToFaction(Faction campaignFaction) {
        if (!campaignFaction.isClan() && isClanOnly) {
            return false;
        }
        return campaignFaction.isComStarOrWoB() || !isComStarOnly;
    }

    /**
     * Checks if this prosthetic is available for purchase or use based on the current location and planetary tech
     * rating.
     *
     * @param currentLocation the campaign's current location
     * @param today           the in-game date
     *
     * @return {@code true} if available in the current location and era
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isAvailableInCurrentLocation(CurrentLocation currentLocation, LocalDate today) {
        int minimumTechRating = TechRating.E.getIndex();
        int prostheticTechLevel = technologyRating.getIndex();

        if (!currentLocation.isOnPlanet()) {
            // In transit: availability limited to rating E or lower
            return minimumTechRating >= prostheticTechLevel;
        }

        Planet planet = currentLocation.getPlanet();
        int planetTechRating = max(minimumTechRating, planet.getTechRating(today).getIndex());

        return planetTechRating >= prostheticTechLevel;
    }

    /**
     * Calculates the adjusted cost for this prosthetic based on the game year. The price may vary depending on its
     * availability in that era.
     *
     * @param gameYear the current in-game year
     *
     * @return the adjusted cost, or {@code null} if the item is not available
     *
     * @author Illiani
     * @since 0.50.10
     */
    public @Nullable Money getCost(int gameYear) {
        double availabilityMultiplier = getAvailabilityMultiplier(gameYear);
        if (availabilityMultiplier == 0.0) {
            return null;
        }
        return baseCost.multipliedBy(availabilityMultiplier);
    }

    /**
     * Returns the price multiplier for this prosthetic based on its availability rating in the specified year.
     *
     * @param gameYear the current in-game year
     *
     * @return a multiplier representing rarity and availability
     *
     * @author Illiani
     * @since 0.50.10
     */
    public double getAvailabilityMultiplier(int gameYear) {
        AvailabilityValue availability = getAvailability(gameYear);
        return switch (availability) {
            case A -> AVAILABILITY_MULTIPLIER_A;
            case B -> AVAILABILITY_MULTIPLIER_B;
            case C -> AVAILABILITY_MULTIPLIER_C;
            case D -> AVAILABILITY_MULTIPLIER_D;
            case E -> AVAILABILITY_MULTIPLIER_E;
            case F -> AVAILABILITY_MULTIPLIER_F;
            case F_STAR -> AVAILABILITY_MULTIPLIER_F_STAR;
            case X -> AVAILABILITY_MULTIPLIER_X;
        };
    }

    public boolean isBurnRemoveOnly() {
        return this == COSMETIC_SURGERY;
    }

    /**
     * Determines which {@link AvailabilityValue} applies for a given year.
     *
     * @param gameYear the current in-game year
     *
     * @return the effective {@link AvailabilityValue} for that era
     *
     * @author Illiani
     * @since 0.50.10
     */
    private AvailabilityValue getAvailability(int gameYear) {
        if (gameYear < EARLY_ERA_CUTOFF) {
            return availabilityEarly;
        } else if (gameYear >= LATE_ERA_START) {
            return availabilityLate;
        } else {
            return availabilityMid;
        }
    }

    /**
     * Returns the localized display name for this prosthetic type.
     *
     * @return the translated name string
     */
    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "ProstheticType." + lookupName + ".name");
    }

    /**
     * Builds a localized tooltip summarizing key information about this prosthetic, including cost, surgical
     * requirements, and attribute modifiers.
     *
     * <p><b>Note:</b> For consistency, the order shown in the tooltip is meant to mirror that of
     * {@link InjuryEffect#getTooltip(List)}.</p>
     *
     * @param gameYear the current in-game year for cost and availability calculation
     *
     * @return a formatted tooltip string suitable for UI display
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getTooltip(int gameYear, boolean isUseKinderMode) {
        StringJoiner tooltipPortion = new StringJoiner("<br>- ", "- ", "");

        // 1) Surgery level required
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.skill", surgeryLevel));

        // 2) Base cost
        Money cost = getCost(gameYear);
        if (cost != null) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.cost",
                  cost.toAmountString()));
        }

        // 3) Required planetary tech rating
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.techLevel",
              technologyRating.getName()));

        // 4) Estimated recovery time
        int recoveryTime = (int) round(injuryType.getBaseRecoveryTime() * (isUseKinderMode ? 0.5 : 1.0));
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.recovery", recoveryTime));

        // 5) Misc
        InjuryEffect effect = injuryType.getInjuryEffect();
        int toughness = effect.getToughnessModifier();
        if (toughness != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.toughness", toughness));
        }

        // 6) Skills
        int gunnery = effect.getGunneryModifier();
        if (gunnery != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.gunnery", gunnery));
        }

        int leadership = effect.getLeadershipModifier();
        if (leadership != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.leadership", leadership));
        }

        int negotiation = effect.getNegotiationModifier();
        if (negotiation != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.negotiation", negotiation));
        }

        int perception = effect.getPerceptionModifier();
        if (perception != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.perception", perception));
        }

        int survival = effect.getSurvivalModifier();
        if (survival != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.survival", survival));
        }

        int interrogation = effect.getInterrogationModifier();
        if (interrogation != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ProstheticType.tooltip.interrogation",
                  interrogation));
        }

        int acting = effect.getActingModifier();
        if (acting != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.acting", acting));
        }

        int acrobatics = effect.getAcrobaticsModifier();
        if (acrobatics != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.acrobatics", acrobatics));
        }

        // 7) Attribute modifiers
        Map<SkillAttribute, Integer> attributeTotals = new EnumMap<>(SkillAttribute.class);

        addToMap(attributeTotals, SkillAttribute.STRENGTH, effect.getStrengthModifier());
        addToMap(attributeTotals, SkillAttribute.BODY, effect.getBodyModifier());
        addToMap(attributeTotals, SkillAttribute.REFLEXES, effect.getReflexesModifier());
        addToMap(attributeTotals, SkillAttribute.DEXTERITY, effect.getDexterityModifier());
        addToMap(attributeTotals, SkillAttribute.INTELLIGENCE, effect.getIntelligenceModifier());
        addToMap(attributeTotals, SkillAttribute.WILLPOWER, effect.getWillpowerModifier());
        addToMap(attributeTotals, SkillAttribute.CHARISMA, effect.getCharismaModifier());

        for (SkillAttribute attribute : SkillAttribute.values()) {
            int modifier = attributeTotals.getOrDefault(attribute, 0);
            if (modifier != 0) {
                tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "ProstheticType.tooltip.attribute", modifier, attribute));
            }
        }

        // 8) Implants
        PersonnelOptions options = new PersonnelOptions();
        for (String lookupName : associatedPilotOptions) {
            IOption option = options.getOption(lookupName);

            String label = option == null ? lookupName : option.getDisplayableName();
            String description = option == null ? "-" : option.getDescription();

            // Special handlers
            switch (lookupName) {
                case UNOFFICIAL_EI_IMPLANT ->
                      description += ". " + getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.ei");
                case MD_VDNI -> description += ". " + getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.vdni");
                case MD_BVDNI -> description += ". " + getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.bvdni");
                default -> {}
            }

            tooltipPortion.add("<b>" + label + ":</b> " + description);
        }

        switch (this) { // Covers special cases
            case DERMAL_MYOMER_ARM_ARMOR, DERMAL_MYOMER_LEG_ARMOR ->
                  tooltipPortion.add(getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.dermal.armor"));
            case DERMAL_MYOMER_ARM_CAMO, DERMAL_MYOMER_LEG_CAMO ->
                  tooltipPortion.add(getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.dermal.camo"));
        }

        // 9) Abilities
        for (String lookupName : associatedPersonnelOptions) {
            IOption ability = options.getOption(lookupName);
            String label = ability == null ? lookupName : ability.getDisplayableName();
            String description = ability == null ? "-" : ability.getDescription();
            tooltipPortion.add("<b>" + label + ":</b> " + description);
        }

        return tooltipPortion.toString();
    }

    /**
     * Utility method for aggregating skill attribute modifiers.
     *
     * @param map   the aggregation map
     * @param key   the skill attribute being modified
     * @param value the modifier to add (ignored if zero)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addToMap(Map<SkillAttribute, Integer> map, SkillAttribute key, int value) {
        map.merge(key, value, Integer::sum);
    }
}
