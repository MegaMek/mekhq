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

import static java.lang.Math.round;
import static megamek.common.options.OptionsConstants.*;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_ATTRACTIVE;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_POISON_RESISTANCE;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_PAINKILLER_ADDICTION;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_UNATTRACTIVE;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.ADVANCED;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.CLONE;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.CRUDE;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.ENHANCED;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.SIMPLE;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticComplexity.STANDARD;
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
import megamek.common.options.IOption;
import mekhq.MHQConstants;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

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
          CRUDE,
          2,
          AlternateInjuries.WOODEN_ARM,
          Money.of(75),
          PlanetarySystem.PlanetarySophistication.REGRESSED,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    HOOK_HAND("HOOK_HAND",
          CRUDE,
          2,
          AlternateInjuries.HOOK_HAND,
          Money.of(75),
          PlanetarySystem.PlanetarySophistication.REGRESSED,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    PEG_LEG("PEG_LEG",
          CRUDE,
          2,
          AlternateInjuries.PEG_LEG,
          Money.of(75),
          PlanetarySystem.PlanetarySophistication.REGRESSED,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    WOODEN_FOOT("WOODEN_FOOT",
          CRUDE,
          2,
          AlternateInjuries.WOODEN_FOOT,
          Money.of(75),
          PlanetarySystem.PlanetarySophistication.REGRESSED,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    SIMPLE_ARM("SIMPLE_ARM",
          SIMPLE,
          2,
          AlternateInjuries.SIMPLE_ARM,
          Money.of(750),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    SIMPLE_CLAW_HAND("SIMPLE_CLAW_HAND",
          SIMPLE,
          2,
          AlternateInjuries.SIMPLE_CLAW_HAND,
          Money.of(750),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    SIMPLE_LEG("SIMPLE_LEG",
          SIMPLE,
          2,
          AlternateInjuries.SIMPLE_LEG,
          Money.of(250),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    SIMPLE_FOOT("SIMPLE_FOOT",
          SIMPLE,
          2,
          AlternateInjuries.SIMPLE_FOOT,
          Money.of(250),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    PROSTHETIC_ARM("PROSTHETIC_ARM",
          STANDARD,
          5,
          AlternateInjuries.PROSTHETIC_ARM,
          Money.of(7500),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    PROSTHETIC_HAND("PROSTHETIC_HAND",
          STANDARD,
          5,
          AlternateInjuries.PROSTHETIC_HAND,
          Money.of(7500),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    PROSTHETIC_LEG("PROSTHETIC_LEG",
          STANDARD,
          5,
          AlternateInjuries.PROSTHETIC_LEG,
          Money.of(10000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    PROSTHETIC_FOOT("PROSTHETIC_FOOT",
          STANDARD,
          5,
          AlternateInjuries.PROSTHETIC_FOOT,
          Money.of(10000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    ADVANCED_PROSTHETIC_ARM("ADVANCED_PROSTHETIC_ARM",
          ADVANCED,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_ARM,
          Money.of(25000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    ADVANCED_PROSTHETIC_HAND("ADVANCED_PROSTHETIC_HAND",
          ADVANCED,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_HAND,
          Money.of(25000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    ADVANCED_PROSTHETIC_LEG("ADVANCED_PROSTHETIC_LEG",
          ADVANCED,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_LEG,
          Money.of(17500),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    ADVANCED_PROSTHETIC_FOOT("ADVANCED_PROSTHETIC_FOOT",
          ADVANCED,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_FOOT,
          Money.of(17500),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    MYOMER_ARM("MYOMER_ARM",
          ENHANCED,
          5,
          AlternateInjuries.MYOMER_ARM,
          Money.of(200000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true,
          false,
          List.of()),
    MYOMER_HAND("MYOMER_HAND",
          ENHANCED,
          5,
          AlternateInjuries.MYOMER_HAND,
          Money.of(100000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true,
          false,
          List.of()),
    MYOMER_LEG("MYOMER_LEG",
          ENHANCED,
          5,
          AlternateInjuries.MYOMER_LEG,
          Money.of(125000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true,
          false,
          List.of()),
    MYOMER_FOOT("MYOMER_FOOT",
          ENHANCED,
          5,
          AlternateInjuries.MYOMER_FOOT,
          Money.of(50000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true,
          false,
          List.of()),
    CLONED_ARM("CLONED_ARM",
          CLONE,
          5,
          AlternateInjuries.CLONED_ARM,
          Money.of(500000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false,
          false,
          List.of()),
    CLONED_HAND("CLONED_HAND",
          CLONE,
          5,
          AlternateInjuries.CLONED_HAND,
          Money.of(300000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false,
          false,
          List.of()),
    CLONED_LEG("CLONED_LEG",
          CLONE,
          5,
          AlternateInjuries.CLONED_LEG,
          Money.of(350000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false,
          false,
          List.of()),
    CLONED_FOOT("CLONED_FOOT",
          CLONE,
          5,
          AlternateInjuries.CLONED_FOOT,
          Money.of(50000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false,
          false,
          List.of()),
    EYE_IMPLANT("EYE_IMPLANT",
          SIMPLE,
          2,
          AlternateInjuries.EYE_IMPLANT,
          Money.of(350),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    BIONIC_EAR("BIONIC_EAR",
          STANDARD,
          5,
          AlternateInjuries.BIONIC_EAR,
          Money.of(100000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    BIONIC_EYE("BIONIC_EYE",
          ADVANCED,
          5,
          AlternateInjuries.BIONIC_EYE,
          Money.of(220000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    BIONIC_HEART("BIONIC_HEART",
          STANDARD,
          5,
          AlternateInjuries.BIONIC_HEART,
          Money.of(500000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false,
          false,
          List.of()),
    BIONIC_LUNGS("BIONIC_LUNGS",
          ADVANCED,
          5,
          AlternateInjuries.BIONIC_LUNGS,
          Money.of(800000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    BIONIC_ORGAN_OTHER("BIONIC_ORGAN_OTHER",
          ADVANCED,
          5,
          AlternateInjuries.BIONIC_ORGAN_OTHER,
          Money.of(750000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false,
          false,
          List.of()),
    COSMETIC_SURGERY("COSMETIC_SURGERY",
          SIMPLE,
          2,
          AlternateInjuries.COSMETIC_SURGERY,
          Money.of(2500),
          PlanetarySystem.PlanetarySophistication.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of()),
    ELECTIVE_MYOMER_ARM("ELECTIVE_MYOMER_ARM",
          ENHANCED,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_ARM,
          Money.of(300000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ELECTIVE_MYOMER_HAND("ELECTIVE_MYOMER_HAND",
          ENHANCED,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_HAND,
          Money.of(150000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ELECTIVE_MYOMER_LEG("ELECTIVE_MYOMER_LEG",
          ENHANCED,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_LEG,
          Money.of(375000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    ENHANCED_IMAGING("ENHANCED_IMAGING",
          ENHANCED,
          5,
          AlternateInjuries.ENHANCED_IMAGING_IMPLANT,
          Money.of(1500000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          true,
          false,
          false,
          List.of(),
          List.of(UNOFFICIAL_EI_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BONE_REINFORCEMENT("BONE_REINFORCEMENT",
          ADVANCED,
          5,
          AlternateInjuries.BONE_REINFORCEMENT,
          Money.of(10000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of(),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    LIVER_FILTRATION_IMPLANT("LIVER_FILTRATION_IMPLANT",
          ENHANCED,
          5,
          AlternateInjuries.LIVER_FILTRATION_IMPLANT,
          Money.of(10000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false,
          false,
          List.of("TC"),
          List.of(),
          List.of(ATOW_POISON_RESISTANCE, COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_1_FILTER("BIONIC_LUNGS_WITH_TYPE_1_FILTER",
          ENHANCED,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_1_FILTER,
          Money.of(805000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of("TC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_2_FILTER("BIONIC_LUNGS_WITH_TYPE_2_FILTER",
          ENHANCED,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_2_FILTER,
          Money.of(815000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of("TC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BIONIC_LUNGS_WITH_TYPE_3_FILTER("BIONIC_LUNGS_WITH_TYPE_3_FILTER",
          ENHANCED,
          5,
          AlternateInjuries.BIONIC_LUNGS_WITH_TYPE_3_FILTER,
          Money.of(845000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false,
          false,
          List.of("TC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_EM_IR("CYBERNETIC_EYE_EM_IR",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EYE_EM_IR,
          Money.of(650000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(MD_CYBER_IMP_VISUAL),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_TELESCOPE("CYBERNETIC_EYE_TELESCOPE",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EYE_TELESCOPE,
          Money.of(450000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(MD_CYBER_IMP_TELE),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_LASER("CYBERNETIC_EYE_LASER",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EYE_LASER,
          Money.of(600000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(MD_CYBER_IMP_LASER),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_MULTI("CYBERNETIC_EYE_MULTI",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EYE_MULTI,
          Money.of(1050000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          false,
          true,
          List.of(),
          List.of(MD_MM_IMPLANTS),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EYE_MULTI_ENHANCED("CYBERNETIC_EYE_MULTI_ENHANCED",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EYE_MULTI_ENHANCED,
          Money.of(17000000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          false,
          true,
          List.of(),
          List.of(MD_ENH_MM_IMPLANTS),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_COMMUNICATIONS("CYBERNETIC_EAR_COMMUNICATIONS",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EAR_COMMUNICATIONS,
          Money.of(8000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D,
          false,
          false,
          false,
          List.of(),
          List.of(MD_COMM_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS("CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS,
          Money.of(8000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          false,
          true,
          List.of(),
          List.of(MD_BOOST_COMM_IMPLANT),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_ENHANCED("CYBERNETIC_EAR_ENHANCED",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EAR_ENHANCED,
          Money.of(200000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(MD_CYBER_IMP_AUDIO),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_SIGNAL("CYBERNETIC_EAR_SIGNAL",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EAR_SIGNAL,
          Money.of(400000),
          PlanetarySystem.PlanetarySophistication.C,
          AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(MD_CYBER_IMP_AUDIO),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_EAR_MULTI("CYBERNETIC_EAR_MULTI",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_EAR_MULTI,
          Money.of(600000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          false,
          false,
          true,
          List.of(),
          List.of(MD_MM_IMPLANTS),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    CYBERNETIC_SPEECH_IMPLANT("CYBERNETIC_SPEECH_IMPLANT",
          ENHANCED,
          5,
          AlternateInjuries.CYBERNETIC_SPEECH_IMPLANT,
          Money.of(200000),
          PlanetarySystem.PlanetarySophistication.D,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    PHEROMONE_EFFUSER("PHEROMONE_EFFUSER",
          ENHANCED,
          5,
          AlternateInjuries.PHEROMONE_EFFUSER,
          Money.of(40000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          true,
          List.of(),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_BEAUTY_ENHANCEMENT("COSMETIC_BEAUTY_ENHANCEMENT",
          STANDARD,
          5,
          AlternateInjuries.COSMETIC_BEAUTY_ENHANCEMENT,
          Money.of(15000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false,
          false,
          List.of(),
          List.of(),
          List.of(ATOW_ATTRACTIVE, COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_HORROR_ENHANCEMENT("COSMETIC_HORROR_ENHANCEMENT",
          STANDARD,
          5,
          AlternateInjuries.COSMETIC_HORROR_ENHANCEMENT,
          Money.of(15000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          false,
          List.of(),
          List.of(),
          List.of(FLAW_UNATTRACTIVE, COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_TAIL_PROSTHETIC("COSMETIC_TAIL_PROSTHETIC",
          STANDARD,
          5,
          AlternateInjuries.COSMETIC_TAIL_PROSTHETIC,
          Money.of(60000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_ANIMAL_EAR_PROSTHETIC("COSMETIC_ANIMAL_EAR_PROSTHETIC",
          STANDARD,
          5,
          AlternateInjuries.COSMETIC_ANIMAL_EAR_PROSTHETIC,
          Money.of(60000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    COSMETIC_ANIMAL_LEG_PROSTHETIC("COSMETIC_ANIMAL_LEG_PROSTHETIC",
          STANDARD,
          5,
          AlternateInjuries.COSMETIC_ANIMAL_LEG_PROSTHETIC,
          Money.of(60000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    DERMAL_MYOMER_ARM_ARMOR("DERMAL_MYOMER_ARM_ARMOR",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR,
          Money.of(450000),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC", "MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE)),
    DERMAL_MYOMER_ARM_CAMO("DERMAL_MYOMER_LEG_CAMO",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_CAMO,
          Money.of(330000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          false,
          List.of("CC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, MISC_PAIN_RESISTANCE)),
    DERMAL_MYOMER_ARM_TRIPLE("DERMAL_MYOMER_LEG_TRIPLE",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_ARM_TRIPLE,
          Money.of(750000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC", "MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, ATOW_TOUGHNESS)),
    DERMAL_MYOMER_LEG_ARMOR("DERMAL_MYOMER_LEG_ARMOR",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_ARMOR,
          Money.of(562500),
          PlanetarySystem.PlanetarySophistication.B,
          AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC", "MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, MISC_PAIN_RESISTANCE)),
    DERMAL_MYOMER_LEG_CAMO("DERMAL_MYOMER_LEG_CAMO",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_CAMO,
          Money.of(412500),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          false,
          List.of("CC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE)),
    DERMAL_MYOMER_LEG_TRIPLE("DERMAL_MYOMER_LEG_TRIPLE",
          ENHANCED,
          5,
          AlternateInjuries.DERMAL_MYOMER_LEG_TRIPLE,
          Money.of(937500),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          false,
          List.of("CC", "MOC"),
          List.of(),
          List.of(COMPULSION_PAINKILLER_ADDICTION, FLAW_UNATTRACTIVE, ATOW_TOUGHNESS)),
    VDNI("VDNI",
          ENHANCED,
          5,
          AlternateInjuries.VEHICULAR_DNI,
          Money.of(1400000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          true,
          List.of(),
          List.of(MD_VDNI),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BUFFERED_VDNI("BUFFERED_VDNI",
          ENHANCED,
          5,
          AlternateInjuries.BUFFERED_VDNI,
          Money.of(2000000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E,
          false,
          false,
          true,
          List.of(),
          List.of(MD_BVDNI),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    BUFFERED_VDNI_TRIPLE_CORE("BUFFERED_VDNI_TRIPLE_CORE",
          ENHANCED,
          5,
          AlternateInjuries.BUFFERED_VDNI_TRIPLE_CORE,
          Money.of(5000000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          true,
          List.of(),
          List.of(MD_BVDNI, MD_TRIPLE_CORE_PROCESSOR),
          List.of(COMPULSION_PAINKILLER_ADDICTION)),
    PAIN_SHUNT("PAIN_SHUNT",
          ENHANCED,
          5,
          AlternateInjuries.PAIN_SHUNT,
          Money.of(50000),
          PlanetarySystem.PlanetarySophistication.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F,
          false,
          false,
          true,
          List.of(),
          List.of(MD_PAIN_SHUNT),
          List.of(COMPULSION_PAINKILLER_ADDICTION));

    private final String lookupName;
    private final ProstheticComplexity prostheticType;
    private final int surgeryLevel;
    private final InjuryType injuryType;
    private final Money baseCost;
    // We have to do a degree of translation here. ATOW tech rating runs A-F, but the normal TW tech rating runs F-A,
    // but planets don't use tech rating they use Planetary Sophistication. Planetary Sophistication can be converted
    // into TW tech rating. However, then we run into a UX issue where there is no way for the player to see at-a-glance
    // what planet they need to be on to gain access to a specific Prosthetic. So we translate ATOW tech rating
    // directly to Planetary Sophistication. In this way ATOW tech rating A is Regressed, B is Sophistication F, and
    // so on.
    private final PlanetarySystem.PlanetarySophistication requiredPlanetarySophistication;
    private final AvailabilityValue availabilityEarly;
    private final AvailabilityValue availabilityMid;
    private final AvailabilityValue availabilityLate;
    private final boolean isComStarOnly;
    private final boolean isClanOnly;
    private final boolean isWordOfBlakeOnly;
    private final List<String> otherAffiliation;
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
     * @param lookupName                      the resource key for localization and lookup
     * @param prostheticType                  the prosthetic tier (as per ATOW)
     * @param surgeryLevel                    the minimum medical skill or facility level required
     * @param injuryType                      the injury this prosthetic 'inflicts'
     * @param baseCost                        the base market price before modifiers (as per ATOW)
     * @param requiredPlanetarySophistication the required planetary sophistication for construction (as per ATOW,
     *                                        translated into planetary sophistication)
     * @param availabilityEarly               availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid                 availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate                availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly                      whether this item is exclusive to Clan factions (as per ATOW)
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, ProstheticComplexity prostheticType, int surgeryLevel, InjuryType injuryType,
          Money baseCost, PlanetarySystem.PlanetarySophistication requiredPlanetarySophistication,
          AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid, AvailabilityValue availabilityLate,
          boolean isClanOnly, boolean isComStarOnly, boolean isWordOfBlakeOnly, List<String> otherAffiliation) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.requiredPlanetarySophistication = requiredPlanetarySophistication;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.isWordOfBlakeOnly = isWordOfBlakeOnly;
        this.otherAffiliation = otherAffiliation;
        this.associatedPilotOptions = new ArrayList<>();
        this.associatedPersonnelOptions = new ArrayList<>();
    }

    /**
     * Constructs a new {@code ProstheticType} entry.
     *
     * @param lookupName                      the resource key for localization and lookup
     * @param prostheticType                  the prosthetic tier (as per ATOW)
     * @param surgeryLevel                    the minimum medical skill or facility level required
     * @param injuryType                      the injury this prosthetic 'inflicts'
     * @param baseCost                        the base market price before modifiers (as per ATOW)
     * @param requiredPlanetarySophistication the required technology rating for construction (as per ATOW)
     * @param availabilityEarly               availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid                 availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate                availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly                      whether this item is exclusive to Clan factions (as per ATOW)
     * @param associatedPilotOptions          Any Pilot Options that should be added to the character when they receive
     *                                        this prosthetic
     * @param associatedPersonnelOptions      Any Personnel Options that should be added to the character when they
     *                                        received this prosthetic
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, ProstheticComplexity prostheticType, int surgeryLevel, InjuryType injuryType,
          Money baseCost, PlanetarySystem.PlanetarySophistication requiredPlanetarySophistication,
          AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid, AvailabilityValue availabilityLate,
          boolean isClanOnly, boolean isComStarOnly, boolean isWordOfBlakeOnly, List<String> otherAffiliation,
          List<String> associatedPilotOptions,
          List<String> associatedPersonnelOptions) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.requiredPlanetarySophistication = requiredPlanetarySophistication;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.isWordOfBlakeOnly = isWordOfBlakeOnly;
        this.otherAffiliation = otherAffiliation;
        this.associatedPilotOptions = associatedPilotOptions;
        this.associatedPersonnelOptions = associatedPersonnelOptions;
    }

    /** @return the prosthetic classification. */
    public int getProstheticType() {
        return prostheticType.getType();
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

    public boolean isClanOnly() {
        return isClanOnly;
    }

    public boolean isComStarOnly() {
        return isComStarOnly;
    }

    public boolean isWordOfBlakeOnly() {
        return isWordOfBlakeOnly;
    }

    /**
     * Determines whether the specified faction may access this prosthetic type based on faction restrictions and
     * in-universe technological availability dates.
     *
     * <p>This method evaluates faction-locked prosthetics—such as Clan-exclusive, ComStar-exclusive, or Word of
     * Blake-exclusive technologies—against the current campaign faction and the provided in-game date. Once certain
     * historical milestones have passed, these technologies become generally available to all factions.</p>
     *
     * <ul>
     *     <li><b>Clan-only:</b> Before the Battle of Tukayyid, only Clan campaigns may access Clan-exclusive
     *     prosthetics. After Tukayyid, these items become broadly available.</li>
     *     <li><b>ComStar-only:</b> Before the ComStar Schism, access is limited to ComStar, Word of Blake, Star
     *     League, and Terran Hegemony factions. After the Schism, ComStar-exclusive prosthetics become available to
     *     all factions.</li>
     *     <li><b>Word of Blake-only:</b> Before the end of Operation Scour, only Word of Blake factions may access
     *     these prosthetics. Afterward, they become unrestricted.</li>
     * </ul>
     *
     * @param campaignFaction the faction whose access rules are being evaluated
     * @param today           the current in-game date used to determine timeline availability
     *
     * @return {@code true} if the faction is permitted to access this prosthetic type at the given date.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isAvailableToFaction(Faction campaignFaction, LocalDate today) {
        // After Tukayyid Clan tech becomes more widely available, we use that date as the threshold after which
        // non-Clan campaigns can access Clan-only prosthetics (cloned limbs, mostly)
        if (isClanOnly && !today.isAfter(MHQConstants.BATTLE_OF_TUKAYYID)) {
            return campaignFaction.isClan();
        }

        // The ComStar Schism was in response to Focht opening up ComStar's secrets to the wider Inner Sphere. We're
        // going to use that date as the threshold after which ComStar-only prosthetics become available to all
        // factions.
        if (isComStarOnly && !today.isAfter(MHQConstants.COMSTAR_SCHISM)) {
            boolean isComStarOrWoBCampaign = campaignFaction.isComStarOrWoB();
            String campaignFactionCode = campaignFaction.getShortName();
            // Anything restricted to ComStar is also available to the Word of Blake, Star League, and Terran Hegemony.
            boolean isStarLeagueCampaign = campaignFactionCode.equals("SL") || campaignFactionCode.equals("TH");

            return isComStarOrWoBCampaign || isStarLeagueCampaign;
        }

        // While knowledge of the Word of Blake's more advanced prosthetics would likely be suppressed, player
        // campaigns are universally special cases, so we give them access to this technology after Operation Scour
        // (the fall of Terra, specifically).
        if (isWordOfBlakeOnly && !today.isAfter(MHQConstants.OPERATION_SCOUR_ENDS)) {
            return campaignFaction.isWoB();
        }

        // There are no other faction-locked technologies, so just return true.
        return true;
    }

    /**
     * Checks if this prosthetic is available for purchase or use based on the current location and planetary tech
     * sophistication rating.
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
        PlanetarySystem.PlanetarySophistication minimumSophistication = PlanetarySystem.PlanetarySophistication.F;

        // In transit: availability limited to rating F or lower
        if (!currentLocation.isOnPlanet()) {
            return minimumSophistication.isBetterOrEqualThan(requiredPlanetarySophistication);
        }

        Planet planet = currentLocation.getPlanet();
        PlanetarySystem.PlanetarySophistication planetarySophistication = planet.getSocioIndustrial(today).tech;
        if (planetarySophistication == null || minimumSophistication.isBetterThan(planetarySophistication)) {
            planetarySophistication = minimumSophistication;
        }

        return planetarySophistication.isBetterOrEqualThan(requiredPlanetarySophistication);
    }

    /**
     * Calculates the adjusted cost of this prosthetic based on the purchasing faction and the current in-game date.
     * Pricing is influenced by both the prosthetic's availability rating for the given year and whether the requesting
     * faction has in-faction access to the item.
     *
     * <p>The availability rating for the year is resolved and converted into a price multiplier via
     * {@link #getAvailabilityMultiplier(int, boolean)}. If the multiplier is {@code 0.0}, the prosthetic is considered
     * unavailable in the specified era or to the specified faction, and this method returns {@code null} .</p>
     *
     * <p>Affiliation rules are evaluated using {@code campaignFactionCode}; factions outside the prosthetic's
     * permitted affiliation list incur different availability multipliers.</p>
     *
     * @param campaignFaction the faction attempting to acquire the prosthetic
     * @param today           the current in-game date used to determine availability by year
     *
     * @return the adjusted cost as a {@link Money} value, or {@code null} if the prosthetic is not available to the
     *       faction or in the specified year
     *
     * @author Illiani
     * @since 0.50.10
     */
    public @Nullable Money getCost(Faction campaignFaction, LocalDate today) {
        double availabilityMultiplier = getAvailabilityMultiplier(today.getYear(), isWrongAffiliation(campaignFaction));
        if (availabilityMultiplier == 0.0) {
            return null;
        }
        return baseCost.multipliedBy(availabilityMultiplier);
    }

    /**
     * Returns the price multiplier for this prosthetic based on its availability rating in the specified year and
     * whether the purchasing faction is the same faction that produces the prosthetic.
     *
     * <p>The availability rating for the given year is first resolved via {@link #getAvailability(int)}. Each rating
     * corresponds to two possible multipliers: one applied when the requesting faction is the prosthetic's native
     * producer, and a higher (or equal) one when the item is being acquired outside its originating faction.</p>
     *
     * <p>The {@code outsideFactionAccess} flag indicates whether the campaign faction lacks in-faction production
     * access. If {@code true}, the prosthetic is treated as rarer and more difficult to acquire, increasing its cost
     * multiplier.</p>
     *
     * @param gameYear             the current in-game year used to determine the prosthetic's availability rating
     * @param outsideFactionAccess {@code true} if the item is being purchased by a faction other than the one that
     *                             produces it; {@code false} if the purchasing faction has in-faction access
     *
     * @return a price multiplier reflecting rarity and availability for the given conditions
     *
     * @author Illiani
     * @since 0.50.10
     */
    public double getAvailabilityMultiplier(int gameYear, boolean outsideFactionAccess) {
        AvailabilityValue availability = getAvailability(gameYear);
        return switch (availability) {
            case A -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_B : AVAILABILITY_MULTIPLIER_A;
            case B -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_C : AVAILABILITY_MULTIPLIER_B;
            case C -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_D : AVAILABILITY_MULTIPLIER_C;
            case D -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_E : AVAILABILITY_MULTIPLIER_D;
            case E -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_F : AVAILABILITY_MULTIPLIER_E;
            case F -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_F_STAR : AVAILABILITY_MULTIPLIER_F;
            case F_STAR -> outsideFactionAccess ? AVAILABILITY_MULTIPLIER_X : AVAILABILITY_MULTIPLIER_F_STAR;
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
     * Returns the {@link ProstheticType} associated with the given {@link InjuryType}, or {@code null} if no matching
     * prosthetic type exists.
     *
     * <p>This method iterates over all defined {@link ProstheticType} values and compares their mapped injury types
     * against the provided {@code injuryType}. If a match is found, the corresponding prosthetic type is returned
     * immediately.</p>
     *
     * <p>Note that this method returns {@code null} when no association is defined, so callers should perform a null
     * check or annotate accordingly when using the result.</p>
     *
     * @param injuryType the injury type to look up; must not be {@code null}
     *
     * @return the matching prosthetic type, or {@code null} if none exists
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static @Nullable ProstheticType getProstheticTypeFromInjuryType(InjuryType injuryType) {
        for (ProstheticType prostheticType : ProstheticType.values()) {
            if (prostheticType.getInjuryType().equals(injuryType)) {
                return prostheticType;
            }
        }

        return null;
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
     * Builds a localized, formatted tooltip summarizing all relevant information about this prosthetic for UI display.
     * The tooltip aggregates cost, surgical requirements, attribute and skill modifiers, recovery time, implants, and
     * any associated personnel abilities.
     *
     * <p>This method respects faction-based availability when calculating cost (via
     * {@link #getCost(Faction, LocalDate)}), and applies Kinder Mode to reduce the displayed recovery time when
     * requested. Additional elements—such as attribute modifiers, derived skill effects, and associated implant
     * options—are included only when applicable.</p>
     *
     * <p><b>Ordering:</b> For consistency and readability, the display order of sections mirrors that used by
     * {@link InjuryEffect#getTooltip(List)}.</p>
     *
     * @param campaignFaction the faction requesting the tooltip, used to determine cost availability and any
     *                        affiliation-based restrictions
     * @param today           the current in-game date used for availability and cost calculations
     * @param isUseKinderMode {@code true} to reduce the listed recovery time by 50%; otherwise {@code false}
     *
     * @return a fully formatted HTML-compatible tooltip string suitable for display in Swing-based UI components
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getTooltip(Faction campaignFaction, LocalDate today, boolean isUseKinderMode) {
        StringJoiner tooltipPortion = new StringJoiner("<br>- ", "- ", "");

        // 1) Surgery level required
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.skill", surgeryLevel));

        // 2) ATOW Type
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
              "ProstheticType.tooltip.type",
              prostheticType.toString()));

        // 3) Base cost
        Money cost = getCost(campaignFaction, today);
        if (cost != null) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.cost",
                  cost.toAmountString()));
        }

        // 4) Required planetary tech rating
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.sophistication",
              requiredPlanetarySophistication.getName()));

        // 5) Estimated recovery time
        int recoveryTime = (int) round(injuryType.getBaseRecoveryTime() * (isUseKinderMode ? 0.5 : 1.0));
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.recovery", recoveryTime));

        // 6) Misc
        InjuryEffect effect = injuryType.getInjuryEffect();
        int toughness = effect.getToughnessModifier();
        if (toughness != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.toughness", toughness));
        }

        // 7) Skills
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

        // 8) Attribute modifiers
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
                      "ProstheticType.tooltip.attribute", modifier, attribute.getLabel()));
            }
        }

        // 9) Implants
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

        // 10) Abilities
        for (String lookupName : associatedPersonnelOptions) {
            IOption ability = options.getOption(lookupName);
            String label = ability == null ? lookupName : ability.getDisplayableName();
            String description = ability == null ? "-" : ability.getDescription();
            tooltipPortion.add("<b>" + label + ":</b> " + description);
        }

        return tooltipPortion.toString();
    }

    /**
     * Determines whether the given campaign faction is barred from accessing this prosthetic based on explicit faction
     * restrictions (Clan-only, ComStar-only, Word of Blake-only) or the additional affiliation list defined for this
     * item.
     *
     * <p>Faction-locked prosthetics follow strict rules:
     * <ul>
     *     <li><b>Clan-only:</b> Allowed only to Clan factions.</li>
     *     <li><b>ComStar-only:</b> Allowed to ComStar, Word of Blake, Star League ("SL"), and Terran Hegemony
     *     ("TH").</li>
     *     <li><b>Word of Blake-only:</b> Allowed only to Word of Blake factions.</li>
     * </ul>
     *
     * <p>If none of these explicit restrictions apply, the method falls back to evaluating the
     * {@code otherAffiliation} list. When this list is non-empty, only factions whose short names appear in it are
     * permitted.</p>
     *
     * <p>If {@code otherAffiliation} is empty and no explicit faction lock applies, the prosthetic is not
     * affiliation-restricted and this method returns {@code false}.</p>
     *
     * @param campaignFaction the faction attempting to access this prosthetic
     *
     * @return {@code true} if the faction is not permitted under the prosthetic's affiliation restrictions
     *
     * @author Illiani
     * @since 0.50.11
     */
    private boolean isWrongAffiliation(Faction campaignFaction) {
        String campaignFactionCode = campaignFaction.getShortName();

        if (isClanOnly) {
            return !campaignFaction.isClan();
        }

        if (isComStarOnly) {
            return !campaignFaction.isComStarOrWoB() &&
                         !campaignFactionCode.equals("SL") &&
                         !campaignFactionCode.equals("TH");
        }

        if (isWordOfBlakeOnly) {
            return !campaignFaction.isWoB();
        }

        return !otherAffiliation.isEmpty() && !otherAffiliation.contains(campaignFactionCode);
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

    /**
     * Fetches the {@link ProstheticType} that corresponds to a given {@link InjuryType} produced by a permanent
     * modification (implant/prosthetic).
     *
     * <p>This is a convenience lookup used when removing or analyzing injuries to determine which prosthetic granted
     * the effect. If the provided injury type is not a permanent modification, or if no matching prosthetic is defined,
     * this returns {@code null}.</p>
     *
     * @param injuryType the injury type to resolve; ignored if it is not a permanent modification
     *
     * @return the matching prosthetic type, or {@code null} if none applies
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static @Nullable ProstheticType getProstheticFromInjury(InjuryType injuryType) {
        if (!injuryType.getSubType().isPermanentModification()) {
            return null;
        }

        for (ProstheticType prosthetic : ProstheticType.values()) {
            if (prosthetic.getInjuryType() == injuryType) {
                return prosthetic;
            }
        }

        return null;
    }
}
