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
package mekhq.campaign.utilities.glossary;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import jakarta.annotation.Nullable;
import megamek.Version;
import mekhq.MHQConstants;

/**
 * The {@code GlossaryEntry} enum represents individual glossary entries used within MekHQ.
 *
 * <p>Each entry is associated with a lookup name, a localized title, and a localized definition.</p>
 *
 * <p>Methods are provided to retrieve localized strings and to access entries based on their lookup names or sorted
 * order.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum GlossaryEntry {
    ADMIN_STRAIN("HR_STRAIN", "0.50.05", "0.51.01.12", "0.51.01.12"),
    ADVANCED_MEDICAL("ADVANCED_MEDICAL", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ADVANCED_SCOUTING("ADVANCED_SCOUTING", "0.50.07", "0.51.01.1", "0.51.01.11"),
    AGGREGATE_COMBAT_VIEW("AGGREGATE_COMBAT_VIEW", "0.51.00", "0.51.00", "0.51.01.11"),
    AGGREGATE_TECH_VIEW("AGGREGATE_TECH_VIEW", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    AGING("AGING", "0.50.06", "0.51.01.12", "0.51.01.12"),
    ALT_ADVANCED_MEDICAL("ALT_ADVANCED_MEDICAL", "0.50.10", "0.51.01.12", "0.51.01.12"),
    ALTERNATE_UNIT_COST("ALTERNATE_UNIT_COST", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    APPLICANT_ORIGINS("APPLICANT_ORIGINS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    APPLICANT_PROFESSIONS("APPLICANT_PROFESSIONS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    AREA_OF_OPERATIONS("AREA_OF_OPERATIONS", "0.50.05", "0.50.05", "0.51.01.11"),
    ATB("ATB", "0.50.05", "0.50.10", "0.51.01.11"),
    ATTRIBUTE_SCORES("ATTRIBUTE_SCORES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    AUTOINFIRMARY("AUTOINFIRMARY", "0.50.06", "0.50.06", "0.51.01.11"),
    BATCHALL("BATCHALL", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    BLOODHOUSE("BLOODHOUSE", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    BLOODMARK("BLOODMARK", "0.50.07", "0.51.01.1", "0.51.01.11"),
    BLOODNAME("BLOODNAME", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    BLOODRIGHT("BLOODRIGHT", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    CAMP_FOLLOWERS("CAMP_FOLLOWERS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    CANONICAL_DISEASES("CANONICAL_DISEASES", "0.50.12", "0.50.12", "0.51.01.11"),
    CAPITAL_SYSTEMS("CAPITAL_SYSTEMS", "0.50.06", "0.50.06", "0.51.01.11"),
    CHALLENGE_SKULLS("CHALLENGE_SKULLS", "0.50.11", "0.50.11", "0.51.01.11"),
    CHANGING_FILTERS_HANGAR("CHANGING_FILTERS_HANGAR", "0.50.06", "0.50.06", "0.51.01.11"),
    CHANGING_FILTERS_PERSONNEL("CHANGING_FILTERS_PERSONNEL", "0.50.06", "0.50.06", "0.51.01.11"),
    CHANGING_FILTERS_WAREHOUSE("CHANGING_FILTERS_WAREHOUSE", "0.50.06", "0.50.06", "0.51.01.11"),
    CHAOS_FACTOR("CHAOS_FACTOR", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    CHASSIS_FAMILIARITY("CHASSIS_FAMILIARITY", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMBAT_ROLES("COMBAT_ROLES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    COMBAT_TEAMS("COMBAT_TEAMS", "0.50.05", "0.51.01.12", "0.51.01.12"),
    COMING_OF_AGE_AWARDS("COMING_OF_AGE_AWARDS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    COMMAND_GENERATOR("COMMAND_GENERATOR", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMMAND_GENERATOR_AUGMENTATION("COMMAND_GENERATOR_AUGMENTATION", "0.51.01.1", "0.51.01.4", "0.51.01.11"),
    COMMAND_GENERATOR_FINANCES("COMMAND_GENERATOR_FINANCES", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMMAND_GENERATOR_FORCE("COMMAND_GENERATOR_FORCE", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMMAND_GENERATOR_OFFICERS("COMMAND_GENERATOR_OFFICERS", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMMAND_GENERATOR_SIMULATION("COMMAND_GENERATOR_SIMULATION", "0.51.01.1", "0.51.01.4", "0.51.01.11"),
    COMMAND_GENERATOR_SUPPORT("COMMAND_GENERATOR_SUPPORT", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    COMMAND_RIGHTS("COMMAND_RIGHTS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    CONNECTIONS("CONNECTIONS", "0.50.07", "0.51.01.1", "0.51.01.11"),
    CONTRACTS_AND_MISSIONS("CONTRACTS_AND_MISSIONS", "0.50.06", "0.51.01.1", "0.51.01.11"),
    CONTRACT_VICTORY_POINTS("CONTRACT_VICTORY_POINTS", "0.50.05", "0.51.01.4", "0.51.01.11"),
    CRISIS_SCENARIO("CRISIS_SCENARIO", "0.50.05", "0.50.05", "0.51.01.11"),
    DAMAGED_PARTS("DAMAGED_PARTS", "0.50.06", "0.50.06", "0.51.01.11"),
    DELIVERY_TIMES("DELIVERY_TIMES", "0.50.06", "0.50.06", "0.51.01.12"),
    DEPLOYMENT_ORDERS("DEPLOYMENT_ORDERS", "0.51.01.1", "0.51.01.10", "0.51.01.11"),
    DEPRECATED("DEPRECATED", "0.50.05", "0.50.05", "0.51.01.11"),
    DIGITAL_GM("DIGITAL_GM", "0.50.05", "0.50.10", "0.51.01.11"),
    DISEASES("DISEASES", "0.50.10", "0.50.10", "0.51.01.11"),
    EDGE("EDGE", "0.50.06", "0.50.06", "0.51.01.11"),
    EDUCATION("EDUCATION", "0.50.06", "0.50.06", "0.51.01.12"),
    EMPLOYER_SUPPORT("EMPLOYER_SUPPORT", "0.50.06", "0.51.01.1", "0.51.01.11"),
    EMPTY_SYSTEMS("EMPTY_SYSTEMS", "0.50.06", "0.50.06", "0.51.01.11"),
    ESCALATION("ESCALATION", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ESSENTIAL_SCENARIOS("ESSENTIAL_SCENARIOS", "0.51.01.1", "0.51.01.10", "0.51.01.11"),
    EXPERIENCE_COSTS("EXPERIENCE_COSTS", "0.50.06", "0.50.06", "0.51.01.11"),
    EXPERIENCE_RATING("EXPERIENCE_RATING", "0.50.06", "0.50.06", "0.51.01.12"),
    EXTRA_INCOME("EXTRA_INCOME", "0.50.10", "0.50.10", "0.51.01.11"),
    FACTION_ACCOLADES("FACTION_ACCOLADES", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    FACTION_CENSURES("FACTION_CENSURES", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    FACTION_REGARD("FACTION_REGARD", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    FACTION_STANDING("FACTION_STANDING", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    FACTION_STANDING_EFFECTS("FACTION_STANDING_EFFECTS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    FAME("FAME", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    FATIGUE("FATIGUE", "0.50.05", "0.51.01.1", "0.51.01.11"),
    FIELD_CONTROL("FIELD_CONTROL", "0.50.06", "0.50.06", "0.51.01.12"),
    FIELD_KITCHENS("FIELD_KITCHENS", "0.50.06", "0.51.01.1", "0.51.01.11"),
    FORCE_GENERATION("FORCE_GENERATION", "0.50.11", "0.50.11", "0.51.01.11"),
    FORCE_REPUTATION("FORCE_REPUTATION", "0.50.05", "0.51.01.1", "0.51.01.11"),
    FORCE_SCALE("FORCE_SCALE", "0.51.01.1", "0.51.01.10", "0.51.01.11"),
    FORCE_TYPE_COMBAT("FORCE_TYPE_COMBAT", "0.50.06", "0.50.06", "0.51.01.11"),
    FORCE_TYPE_CONVOY("FORCE_TYPE_CONVOY", "0.50.06", "0.50.06", "0.51.01.11"),
    FORCE_TYPE_SALVAGE("FORCE_TYPE_SALVAGE", "0.50.10", "0.50.10", "0.51.01.11"),
    FORCE_TYPE_SECURITY("FORCE_TYPE_SECURITY", "0.50.06", "0.51.01.12", "0.51.01.12"),
    FORCE_TYPE_SUPPORT("FORCE_TYPE_SUPPORT", "0.50.06", "0.50.10", "0.51.01.11"),
    GARRISON_RAIDS("GARRISON_RAIDS", "0.51.01.9", "0.51.01.9", "0.51.01.11"),
    GENERIC_BATTLE_VALUE("GENERIC_BATTLE_VALUE", "0.50.11", "0.50.11", "0.51.01.11"),
    GENETIC_LEGACY("GENETIC_LEGACY", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    GOING_ROGUE("GOING_ROGUE", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    GOLDEN_HELLO("GOLDEN_HELLO", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    GROUP_BY_UNIT("GROUP_BY_UNIT", "0.50.06", "0.50.06", "0.51.01.11"),
    HIRING_COSTS("HIRING_COSTS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    HIRING_HALLS("HIRING_HALLS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    HOSPITAL_BEDS("HOSPITAL_BEDS", "0.50.06", "0.51.01.12", "0.51.01.12"),
    HOT_SPOTS_UPKEEP("HOT_SPOTS_UPKEEP", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    HOW_TO_DEPLOY("HOW_TO_DEPLOY", "0.50.06", "0.50.06", "0.51.01.11"),
    HOW_TO_REINFORCE("HOW_TO_REINFORCE", "0.50.06", "0.51.01.12", "0.51.01.12"),
    INFANTRY_GUNNERY_SKILLS("INFANTRY_GUNNERY_SKILLS", "0.50.07", "0.50.07", "0.51.01.11"),
    ISORLA("ISORLA", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    JUMP_COST_CALCULATIONS("JUMP_COST_CALCULATIONS", "0.50.10", "0.50.10", "0.51.01.11"),
    LEADERSHIP_UNITS("LEADERSHIP_UNITS", "0.50.06", "0.51.01.12", "0.51.01.12"),
    LINKED_ATTRIBUTES("LINKED_ATTRIBUTES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    LOYALTY("LOYALTY", "0.50.05", "0.51.01.12", "0.51.01.12"),
    MAINTENANCE("MAINTENANCE", "0.50.06", "0.51.01.1", "0.51.01.11"),
    MANAGEMENT_SKILL("MANAGEMENT_SKILL", "0.50.05", "0.51.01.12", "0.51.01.12"),
    MASS_REPAIR_MASS_SALVAGE("MASS_REPAIR_MASS_SALVAGE", "0.50.06", "0.51.01.12", "0.51.01.12"),
    MISSING_IN_ACTION("MISSING_IN_ACTION", "0.50.06", "0.51.01.12", "0.51.01.12"),
    MISSING_LIMBS("MISSING_LIMBS", "0.50.06", "0.51.01.12", "0.51.01.12"),
    MISSION_CONCLUSION_HELP("MISSION_CONCLUSION_HELP", "0.50.06", "0.51.01.12", "0.51.01.12"),
    MISSION_TYPES("MISSION_TYPES", "0.51.00", "0.51.01.1", "0.51.01.11"),
    MISSION_TYPE_ASSASSINATION("MISSION_TYPE_ASSASSINATION", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_CADRE_DUTY("MISSION_TYPE_CADRE_DUTY", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_DIVERSIONARY_RAID("MISSION_TYPE_DIVERSIONARY_RAID", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_ESPIONAGE("MISSION_TYPE_ESPIONAGE", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_EXTRACTION_RAID("MISSION_TYPE_EXTRACTION_RAID", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_GARRISON_DUTY("MISSION_TYPE_GARRISON_DUTY", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_GUERRILLA_WARFARE("MISSION_TYPE_GUERRILLA_WARFARE", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_MOLE_HUNTING("MISSION_TYPE_MOLE_HUNTING", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_OBJECTIVE_RAID("MISSION_TYPE_OBJECTIVE_RAID", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_OBSERVATION_RAID("MISSION_TYPE_OBSERVATION_RAID", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_PIRATE_HUNTING("MISSION_TYPE_PIRATE_HUNTING", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_PIRATE_RAID("MISSION_TYPE_PIRATE_RAID", "0.51.01.1", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_PLANETARY_ASSAULT("MISSION_TYPE_PLANETARY_ASSAULT", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_RECON_RAID("MISSION_TYPE_RECON_RAID", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_RELIEF_DUTY("MISSION_TYPE_RELIEF_DUTY", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_RETAINER("MISSION_TYPE_RETAINER", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_RIOT_DUTY("MISSION_TYPE_RIOT_DUTY", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_SABOTAGE("MISSION_TYPE_SABOTAGE", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_SECURITY_DUTY("MISSION_TYPE_SECURITY_DUTY", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MISSION_TYPE_TERRORISM("MISSION_TYPE_TERRORISM", "0.51.00", "0.51.01.9", "0.51.01.11"),
    MORALE("MORALE", "0.50.06", "0.51.01.9", "0.51.01.11"),
    MORALE_EFFECTS("MORALE_EFFECTS", "0.51.01.9", "0.51.01.9", "0.51.01.11"),
    NAVIGATION_INFIRMARY("NAVIGATION_INFIRMARY", "0.50.06", "0.50.06", "0.51.01.11"),
    NAVIGATION_INTERSTELLAR_MAP("NAVIGATION_INTERSTELLAR_MAP", "0.50.06", "0.50.06", "0.51.01.11"),
    NEW_PLAYER_GUIDE("NEW_PLAYER_GUIDE", "0.50.06", "0.50.07", "0.51.01.12"),
    NO_SEED_FORCES("NO_SEED_FORCES", "0.51.01.1", "0.51.01.1", "0.51.01.12"),
    ORACLE("ORACLE", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ORACLE_CAST("ORACLE_CAST", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ORACLE_CHECKS("ORACLE_CHECKS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ORACLE_CONCEPTS("ORACLE_CONCEPTS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    ORACLE_RANDOM_EVENTS("ORACLE_RANDOM_EVENTS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    PARTS_AVAILABILITY("PARTS_AVAILABILITY", "0.50.06", "0.51.01.1", "0.51.01.11"),
    PARTS_IN_USE("PARTS_IN_USE", "0.50.06", "0.50.06", "0.51.01.11"),
    PERSONALITY_VALUE("PERSONALITY_VALUE", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    PERSONNEL_MARKET("PERSONNEL_MARKET", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    PHENOTYPE("PHENOTYPE", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    PLOT_THREADS("PLOT_THREADS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    PRISONERS_OF_WAR("PRISONERS_OF_WAR", "0.50.06", "0.51.01.12", "0.51.01.12"),
    PRISONER_CAPACITY("PRISONER_CAPACITY", "0.50.05", "0.51.01.12", "0.51.01.12"),
    PRISONER_CAPTURE("PRISONER_CAPTURE", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    PRISONER_EVENTS("PRISONER_EVENTS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    PRISONER_MISSION_END("PRISONER_MISSION_END", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    PROFESSIONS("PROFESSIONS", "0.50.06", "0.51.01.12", "0.51.01.12"),
    PROSTHETICS("PROSTHETICS", "0.50.10", "0.51.01.12", "0.51.01.12"),
    QUICK_TRAIN("QUICK_TRAIN", "0.50.11", "0.51.01.7", "0.51.01.12"),
    RANDOM_CIVILIANS("RANDOM_CIVILIANS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RANDOM_DEATH("RANDOM_DEATH", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RANDOM_PERSONALITIES("RANDOM_PERSONALITIES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    REINFORCEMENT_ARRIVAL("REINFORCEMENT_ARRIVAL", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    REINFORCEMENT_CHECKS("REINFORCEMENT_CHECKS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RECRUITMENT_RESTRICTIONS("RECRUITMENT_RESTRICTIONS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RECRUITMENT_STYLES("RECRUITMENT_STYLES", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RECRUIT_ROLLS("RECRUIT_ROLLS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    RELEASE_TYPES("RELEASE_TYPES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    RELEASE_TYPE_DEVELOPMENT("RELEASE_TYPE_DEVELOPMENT", "0.50.06", "0.51.01.12", "0.51.01.12"),
    RELEASE_TYPE_MILESTONE("RELEASE_TYPE_MILESTONE", "0.50.06", "0.51.01.12", "0.51.01.12"),
    RELEASE_TYPE_NIGHTLY("RELEASE_TYPE_NIGHTLY", "0.50.06", "0.50.06", "0.51.01.11"),
    RELOADING_FIELD_GUNS("RELOADING_FIELD_GUNS", "0.50.06", "0.50.06", "0.51.01.11"),
    REMAIN_DEPLOYED("REMAIN_DEPLOYED", "0.50.06", "0.51.01.1", "0.51.01.11"),
    REPAIRING_DAMAGED_HIP_SHOULDER("REPAIRING_DAMAGED_HIP_SHOULDER", "0.50.06", "0.50.06", "0.51.01.11"),
    REPAIR_SITE("REPAIR_SITE", "0.50.06", "0.51.01.8", "0.51.01.11"),
    RESUPPLY("RESUPPLY", "0.50.05", "0.51.01.12", "0.51.01.12"),
    SCENARIO_VICTORY_POINTS("SCENARIO_VICTORY_POINTS", "0.50.05", "0.50.05", "0.51.01.11"),
    SCENES("SCENES", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    SEARCH_AND_RESCUE("SEARCH_AND_RESCUE", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    SEED_FORCES("SEED_FORCES", "0.50.06", "0.51.01.1", "0.51.01.11"),
    SHOW_OF_INTEREST("SHOW_OF_INTEREST", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    SKILL_TYPES("SKILL_TYPES", "0.50.06", "0.51.01.12", "0.51.01.12"),
    STRATCON("STRATCON", "0.50.05", "0.50.10", "0.51.01.11"),
    STRATCON_COUNTERATTACKS("STRATCON_COUNTERATTACKS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_ENEMY_ENGINEERS("STRATCON_ENEMY_ENGINEERS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_ENEMY_FACILITY_ACTIVITY("STRATCON_ENEMY_FACILITY_ACTIVITY", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITIES("STRATCON_FACILITIES", "0.50.05", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_CAPTURE("STRATCON_FACILITY_CAPTURE", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_INTEL("STRATCON_FACILITY_INTEL", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_ORDERS("STRATCON_FACILITY_ORDERS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_PROFILES("STRATCON_FACILITY_PROFILES", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    STRATCON_FACILITY_SYNERGIES("STRATCON_FACILITY_SYNERGIES", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_TIERS("STRATCON_FACILITY_TIERS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_FACILITY_TRAITS("STRATCON_FACILITY_TRAITS", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_MAPLESS("STRATCON_MAPLESS", "0.50.10", "0.50.10", "0.51.01.11"),
    STRATCON_SCENARIOS("STRATCON_SCENARIOS", "0.51.00", "0.51.01.12", "0.51.01.12"),
    STRATCON_SIEGES("STRATCON_SIEGES", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_SINGLE_DROP("STRATCON_SINGLE_DROP", "0.50.10", "0.50.10", "0.51.01.12"),
    STRATCON_SUPPLY_INTERDICTION("STRATCON_SUPPLY_INTERDICTION", "0.51.01.1", "0.51.01.12", "0.51.01.12"),
    STRATCON_SUPPLY_LINES("STRATCON_SUPPLY_LINES", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    STRATEGIC_OBJECTIVES("STRATEGIC_OBJECTIVES", "0.50.06", "0.51.01.4", "0.51.01.11"),
    STRIPPING_AND_REPAIRING("STRIPPING_AND_REPAIRING", "0.50.06", "0.50.06", "0.51.01.11"),
    SUPPORT_TEAMS("SUPPORT_TEAMS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    TALENT("TALENT", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    TECH_SPECIALIZATION("TECH_SPECIALIZATION", "0.51.01.7", "0.51.01.7", "0.51.01.11"),
    TECH_TIME("TECH_TIME", "0.50.06", "0.51.01.12", "0.51.01.12"),
    TEMP_CREWS("TEMP_CREWS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    TEMP_PERSONNEL("TEMP_PERSONNEL", "0.50.06", "0.50.06", "0.51.01.12"),
    TOE("TOE", "0.50.06", "0.50.06", "0.51.01.11"),
    TRAINING_COMBAT_TEAMS("TRAINING_COMBAT_TEAMS", "0.50.10", "0.51.01.12", "0.51.01.12"),
    TRAITS("TRAITS", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    TRAINING_COSTS("TRAINING_COSTS", "0.51.01.1", "0.51.01.4", "0.51.01.11"),
    TRIAL_OF_POSSESSION("TRIAL_OF_POSSESSION", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    TURNING_POINTS("TURNING_POINTS", "0.50.05", "0.51.01.1", "0.51.01.11"),
    TURNOVER("TURNOVER", "0.50.05", "0.51.01.12", "0.51.01.12"),
    TURNOVER_MODIFIERS("TURNOVER_MODIFIERS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    TURNOVER_PAYOUTS("TURNOVER_PAYOUTS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    ULTIMATUMS("ULTIMATUMS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    UNABLE_TO_START_SCENARIO("UNABLE_TO_START_SCENARIO", "0.50.06", "0.50.06", "0.51.01.11"),
    UNLUCKY("UNLUCKY", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    VEHICLE_CREWS("VEHICLE_CREWS", "0.50.10", "0.51.01.1", "0.51.01.11"),
    VETERANCY_AWARDS("VETERANCY_AWARDS", "0.51.01.12", "0.51.01.12", "0.51.01.12"),
    VOCATIONAL_XP("VOCATIONAL_XP", "0.50.06", "0.50.06", "0.51.01.11"),
    WARS_OF_REAVING("WARS_OF_REAVING", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    WEALTH("WEALTH", "0.51.01.1", "0.51.01.1", "0.51.01.11"),
    WINTER_HOLIDAY("WINTER_HOLIDAY", "0.50.05", "0.50.05", "0.51.01.11");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GlossaryEntry";

    private final String lookUpName;
    private final Version versionAdded;
    private final Version versionLastUpdated;
    private final Version versionLastVerified;

    /**
     * Constructs a {@code GlossaryEntry} with the specified lookup name and version history.
     *
     * <p>Each version is the release that first shipped the change, including its revision where it has one, such
     * as {@code 0.51.01.4}. A change not yet released takes the version of the release it will ship in.</p>
     *
     * @param lookUpName          the resource key used to look up this entry's localized strings
     * @param versionAdded        the release that first shipped this entry
     * @param versionLastUpdated  the release that last shipped a significant change to this entry's text: new or
     *                            corrected information, not spelling, formatting or a renamed term
     * @param versionLastVerified the release in which this entry was last checked against the game and found accurate
     *
     * @author Illiani
     * @since 0.50.07
     */
    GlossaryEntry(String lookUpName, String versionAdded, String versionLastUpdated, String versionLastVerified) {
        this.lookUpName = lookUpName;
        this.versionAdded = new Version(versionAdded);
        this.versionLastUpdated = new Version(versionLastUpdated);
        this.versionLastVerified = new Version(versionLastVerified);
    }

    /**
     * Returns the lookup name for this glossary entry.
     *
     * @return the lookup name for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookUpName() {
        return lookUpName;
    }

    /**
     * Returns the localized title for this glossary entry.
     *
     * @return the title string for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getTitle() {
        return getTextAt(RESOURCE_BUNDLE, lookUpName + ".title");
    }

    /**
     * @return the release that first shipped this entry
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionAdded() {
        return versionAdded;
    }

    /**
     * Returns the release that last shipped a significant change to this entry. A newly added entry counts as
     * updated in the release that added it, so this is never older than {@link #getVersionAdded()}.
     *
     * <p>This is the version that decides whether the entry is shown as new or changed.</p>
     *
     * @return the release that last significantly updated this entry
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionLastUpdated() {
        return versionLastUpdated;
    }

    /**
     * @return the release in which this entry was last checked and found accurate; never older than
     *       {@link #getVersionLastUpdated()}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Version getVersionLastVerified() {
        return versionLastVerified;
    }

    /**
     * Returns the title of this object, possibly appended with an icon/text indicating if it was added or updated since
     * the last development or milestone version.
     *
     * <p>If {@link #versionLastUpdated} is equal to or newer than the current version,
     * an "added since last development" icon/text is appended.</p>
     *
     * <p>Otherwise, if {@link #versionLastUpdated} is newer than the last milestone version,
     * an "added since last milestone" icon/text is appended.</p>
     *
     * @return the title string, with an update/version icon appended if applicable
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getTitleWithVersionUpdateIcon() {
        Version currentVersion = MHQConstants.VERSION;
        Version lastMilestone = MHQConstants.LAST_MILESTONE;
        String title = getTitle();

        // We use an inverted 'isLowerThan' check as that will include instances where 'versionLastUpdated' is the
        // same as 'currentVersion'
        if (!versionLastUpdated.isLowerThan(currentVersion)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_DEVELOPMENT;
        } else if (versionLastUpdated.isHigherThan(lastMilestone)) {
            title += ' ' + MHQConstants.ADDED_SINCE_LAST_MILESTONE;
        }

        return title;
    }

    /**
     * Returns the localized definition for this glossary entry.
     *
     * @return the definition string for this entry
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getDefinition() {
        return getTextAt(RESOURCE_BUNDLE, lookUpName + ".definition");
    }

    /**
     * Returns a list of all lookup names, sorted in ascending order of their corresponding localized titles.
     *
     * @return a list of lookup names sorted by title
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static List<String> getLookUpNamesSortedByTitle() {
        return java.util.Arrays.stream(GlossaryEntry.values())
                     .sorted(java.util.Comparator.comparing(GlossaryEntry::getTitle))
                     .map(entry -> entry.lookUpName)
                     .toList();
    }

    /**
     * Returns the {@code GlossaryEntry} associated with the given lookup name, or {@code null} if not found.
     *
     * @param lookUpName the lookup name to search for
     *
     * @return the corresponding {@code GlossaryEntry} if found; {@code null} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable GlossaryEntry getGlossaryEntryFromLookUpName(String lookUpName) {
        for (GlossaryEntry entry : GlossaryEntry.values()) {
            if (entry.lookUpName.equals(lookUpName)) {
                return entry;
            }
        }
        return null;
    }
}
