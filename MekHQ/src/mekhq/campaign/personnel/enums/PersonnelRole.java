/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static mekhq.campaign.personnel.skills.InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS;
import static mekhq.campaign.personnel.skills.SkillType.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * The PersonnelRole enum represents various roles a person can have. Each role is associated with a name, an optional
 * clan name, and a mnemonic key event. These roles can be used to classify personnel.
 */
public enum PersonnelRole {
    // region Enum Declarations
    /**
     * Individual roles with corresponding name texts and mnemonics.
     */
    // I used an average of the modifiers from the MekWarrior, Hot Shot, and Grizzled Veteran ATOW Archetypes
    MEKWARRIOR(PersonnelRoleSubType.COMBAT, KeyEvent.VK_M, 4, 5, 6, 6, 4, 4, 4),

    // I used an average of the modifiers from the MekWarrior, and Aerospace Pilot ATOW Archetypes
    LAM_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_UNDEFINED, 3, 4, 6, 6, 4, 4, 5),

    // ATOW: Tanker Archetype
    GROUND_VEHICLE_DRIVER(PersonnelRoleSubType.COMBAT, KeyEvent.VK_V, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Tanker Archetype
    NAVAL_VEHICLE_DRIVER(PersonnelRoleSubType.COMBAT, KeyEvent.VK_N, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Companion Chopper Pilot Archetype
    VTOL_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_UNDEFINED, 4, 4, 5, 5, 4, 4, 4),

    // ATOW: Tanker Archetype
    VEHICLE_GUNNER(PersonnelRoleSubType.COMBAT, KeyEvent.VK_G, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    VEHICLE_CREW(PersonnelRoleSubType.COMBAT, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Aerospace Pilot Archetype
    AEROSPACE_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_A, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Aerospace Pilot Archetype
    CONVENTIONAL_AIRCRAFT_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_C, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Aerospace Pilot Archetype (most ProtoMek pilots are Aerospace Sibkbo washouts, so this made the most sense)
    PROTOMEK_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_P, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Elemental Archetype
    BATTLE_ARMOUR(PersonnelRoleSubType.COMBAT, true, KeyEvent.VK_B, 7, 6, 4, 5, 3, 4, 3),

    // ATOW: Renegade Warrior Archetype
    SOLDIER(PersonnelRoleSubType.COMBAT, KeyEvent.VK_S, 5, 5, 4, 5, 4, 6, 3),

    // ATOW: Tanker Archetype
    VESSEL_PILOT(PersonnelRoleSubType.COMBAT, KeyEvent.VK_I, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Tanker Archetype
    VESSEL_GUNNER(PersonnelRoleSubType.COMBAT, KeyEvent.VK_U, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    VESSEL_CREW(PersonnelRoleSubType.COMBAT, KeyEvent.VK_W, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype
    VESSEL_NAVIGATOR(PersonnelRoleSubType.COMBAT, KeyEvent.VK_Y, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    MEK_TECH(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_T, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    MECHANIC(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_E, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    AERO_TEK(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_O, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    BA_TECH(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    ASTECH(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Communications Specialist Archetype (this might seem like an odd choice, but the Attributes for this Archetype
    // work really well for this profession). However, we have switched Dexterity and Reflexes.
    DOCTOR(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_D, 3, 4, 5, 4, 6, 4, 4),

    // ATOW: Communications Specialist Archetype (this might seem like an odd choice, but the Attributes for this Archetype
    // work really well for this profession
    MEDIC(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_UNDEFINED, 3, 4, 4, 5, 6, 4, 4),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_COMMAND(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_UNDEFINED, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_LOGISTICS(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_L, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_TRANSPORT(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_R, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_HR(PersonnelRoleSubType.SUPPORT, KeyEvent.VK_H, 3, 3, 3, 4, 6, 3, 5),

    // If we're generating a character without a Profession, we're just going to leave them with middle of the road
    // Attribute scores (5 in everything)
    NONE(PersonnelRoleSubType.CIVILIAN, false, KeyEvent.VK_UNDEFINED, 5, 5, 5, 5, 5, 5, 5),

    // No archetype, but ATOW pg 35 states that the Attribute scores for an average person are 4
    DEPENDENT(KeyEvent.VK_UNDEFINED),
    ADULT_ENTERTAINER(KeyEvent.VK_UNDEFINED),
    ANTIQUARIAN(KeyEvent.VK_UNDEFINED),
    SPORTS_STAR(KeyEvent.VK_UNDEFINED),
    ASTROGRAPHER(KeyEvent.VK_UNDEFINED),
    BARBER(KeyEvent.VK_UNDEFINED),
    BARTENDER(KeyEvent.VK_UNDEFINED),
    WAR_CORRESPONDENT(KeyEvent.VK_UNDEFINED),
    BRAWLER(KeyEvent.VK_UNDEFINED),
    BROKER(KeyEvent.VK_UNDEFINED),
    CHEF(KeyEvent.VK_UNDEFINED),
    CIVILIAN_AERO_MECHANIC(KeyEvent.VK_UNDEFINED),
    CIVILIAN_DROPSHIP_PILOT(KeyEvent.VK_UNDEFINED),
    POLICE_OFFICER(KeyEvent.VK_UNDEFINED),
    CIVILIAN_VTOL_PILOT(KeyEvent.VK_UNDEFINED),
    CIVIL_CLERK(KeyEvent.VK_UNDEFINED),
    CLOWN(KeyEvent.VK_UNDEFINED),
    CON_ARTIST(KeyEvent.VK_UNDEFINED),
    MILITARY_CORONER(KeyEvent.VK_UNDEFINED),
    COURIER(KeyEvent.VK_UNDEFINED),
    CRIMINAL_MECHANIC(KeyEvent.VK_UNDEFINED),
    CULTURAL_CENSOR(KeyEvent.VK_UNDEFINED),
    CULTURAL_LIAISON(KeyEvent.VK_UNDEFINED),
    CUSTOMS_INSPECTOR(KeyEvent.VK_UNDEFINED),
    DATA_SMUGGLER(KeyEvent.VK_UNDEFINED),
    DATA_ANALYST(KeyEvent.VK_UNDEFINED),
    SPACEPORT_WORKER(KeyEvent.VK_UNDEFINED),
    DRUG_DEALER(KeyEvent.VK_UNDEFINED),
    FACTORY_WORKER(KeyEvent.VK_UNDEFINED),
    LIVESTOCK_FARMER(KeyEvent.VK_UNDEFINED),
    AGRI_FARMER(KeyEvent.VK_UNDEFINED),
    FIREFIGHTER(KeyEvent.VK_UNDEFINED),
    FISHER(KeyEvent.VK_UNDEFINED),
    COUNTERFEITER(KeyEvent.VK_UNDEFINED),
    GAMBLER(KeyEvent.VK_UNDEFINED),
    CIVILIAN_DOCTOR(KeyEvent.VK_UNDEFINED),
    HACKER(KeyEvent.VK_UNDEFINED),
    HERALD(KeyEvent.VK_UNDEFINED),
    HISTORIAN(KeyEvent.VK_UNDEFINED),
    HOLO_CARTOGRAPHER(KeyEvent.VK_UNDEFINED),
    HOLO_GAMER(KeyEvent.VK_UNDEFINED),
    HOLO_JOURNALIST(KeyEvent.VK_UNDEFINED),
    HOLO_STAR(KeyEvent.VK_UNDEFINED),
    INDUSTRIAL_MEK_PILOT(KeyEvent.VK_UNDEFINED),
    INFORMATION_BROKER(KeyEvent.VK_UNDEFINED),
    MILITARY_LIAISON(KeyEvent.VK_UNDEFINED),
    JANITOR(KeyEvent.VK_UNDEFINED),
    JUMPSHIP_CHEF(KeyEvent.VK_UNDEFINED),
    EXOSKELETON_LABORER(KeyEvent.VK_UNDEFINED),
    LAWYER(KeyEvent.VK_UNDEFINED),
    PROPHET(KeyEvent.VK_UNDEFINED),
    RELIC_HUNTER(KeyEvent.VK_UNDEFINED),
    MEDIATOR(KeyEvent.VK_UNDEFINED),
    MEDICAL_RESEARCHER(KeyEvent.VK_UNDEFINED),
    MEK_RANGE_INSTRUCTOR(KeyEvent.VK_UNDEFINED),
    MERCHANT(KeyEvent.VK_UNDEFINED),
    MILITARY_ACCOUNTANT(KeyEvent.VK_UNDEFINED),
    MILITARY_ANALYST(KeyEvent.VK_UNDEFINED),
    SPY(KeyEvent.VK_UNDEFINED),
    MILITARY_THEORIST(KeyEvent.VK_UNDEFINED),
    MINER(KeyEvent.VK_UNDEFINED),
    MOUNTAIN_CLIMBER(KeyEvent.VK_UNDEFINED),
    FACTORY_FOREMAN(KeyEvent.VK_UNDEFINED),
    MUNITIONS_FACTORY_WORKER(KeyEvent.VK_UNDEFINED),
    MUSICIAN(KeyEvent.VK_UNDEFINED),
    ORBITAL_DEFENSE_GUNNER(KeyEvent.VK_UNDEFINED),
    ORBITAL_SHUTTLE_PILOT(KeyEvent.VK_UNDEFINED),
    PARAMEDIC(KeyEvent.VK_UNDEFINED),
    PAINTER(KeyEvent.VK_UNDEFINED),
    PATHFINDER(KeyEvent.VK_UNDEFINED),
    PERFORMER(KeyEvent.VK_UNDEFINED),
    PERSONAL_VALET(KeyEvent.VK_UNDEFINED),
    ESCAPED_PRISONER(KeyEvent.VK_UNDEFINED),
    PROPAGANDIST(KeyEvent.VK_UNDEFINED),
    PSYCHOLOGIST(KeyEvent.VK_UNDEFINED),
    FIRING_RANGE_SAFETY_OFFICER(KeyEvent.VK_UNDEFINED),
    RECRUITMENT_SCREENING_OFFICER(KeyEvent.VK_UNDEFINED),
    RELIGIOUS_LEADER(KeyEvent.VK_UNDEFINED),
    REPAIR_BAY_SUPERVISOR(KeyEvent.VK_UNDEFINED),
    REVOLUTIONIST(KeyEvent.VK_UNDEFINED),
    RITUALIST(KeyEvent.VK_UNDEFINED),
    SALVAGE_RAT(KeyEvent.VK_UNDEFINED),
    SCRIBE(KeyEvent.VK_UNDEFINED),
    SCULPTURER(KeyEvent.VK_UNDEFINED),
    SENSOR_TECHNICIAN(KeyEvent.VK_UNDEFINED),
    CIVILIAN_PILOT(KeyEvent.VK_UNDEFINED),
    STREET_SURGEON(KeyEvent.VK_UNDEFINED),
    SWIMMING_INSTRUCTOR(KeyEvent.VK_UNDEFINED),
    TACTICAL_ANALYST(KeyEvent.VK_UNDEFINED),
    TAILOR(KeyEvent.VK_UNDEFINED),
    TEACHER(KeyEvent.VK_UNDEFINED),
    TECH_COMMUNICATIONS(KeyEvent.VK_UNDEFINED),
    TECH_ZERO_G(KeyEvent.VK_UNDEFINED),
    TECH_HYDROPONICS(KeyEvent.VK_UNDEFINED),
    TECH_FUSION_PLANT(KeyEvent.VK_UNDEFINED),
    TECH_SECURITY(KeyEvent.VK_UNDEFINED),
    TECH_WASTE_MANAGEMENT(KeyEvent.VK_UNDEFINED),
    TECH_WATER_RECLAMATION(KeyEvent.VK_UNDEFINED),
    THIEF(KeyEvent.VK_UNDEFINED),
    BURGLAR(KeyEvent.VK_UNDEFINED),
    TRAINING_SIM_OPERATOR(KeyEvent.VK_UNDEFINED),
    TRANSPORT_DRIVER(KeyEvent.VK_UNDEFINED),
    ARTIST(KeyEvent.VK_UNDEFINED),
    WAREHOUSE_WORKER(KeyEvent.VK_UNDEFINED),
    WARFARE_PLANNER(KeyEvent.VK_UNDEFINED),
    WEATHERCASTER(KeyEvent.VK_UNDEFINED),
    XENOANIMAL_TRAINER(KeyEvent.VK_UNDEFINED),
    XENO_BIOLOGIST(KeyEvent.VK_UNDEFINED),
    GENETICIST(KeyEvent.VK_UNDEFINED),
    MASSEUSE(KeyEvent.VK_UNDEFINED),
    BODYGUARD(KeyEvent.VK_UNDEFINED),
    ARTISAN_MICROBREWER(KeyEvent.VK_UNDEFINED),
    INTERSTELLAR_TOURISM_GUIDE(KeyEvent.VK_UNDEFINED),
    CORPORATE_CONCIERGE(KeyEvent.VK_UNDEFINED),
    VIRTUAL_REALITY_THERAPIST(KeyEvent.VK_UNDEFINED),
    EXOTIC_PET_CARETAKER(KeyEvent.VK_UNDEFINED),
    CULTURAL_SENSITIVITY_ADVISOR(KeyEvent.VK_UNDEFINED),
    PLANETARY_IMMIGRATION_ASSESSOR(KeyEvent.VK_UNDEFINED),
    PERSONAL_ASSISTANT(KeyEvent.VK_UNDEFINED),
    PENNILESS_NOBLE(KeyEvent.VK_UNDEFINED),
    AIDE_DE_CAMP(KeyEvent.VK_UNDEFINED),
    NEUROHELMET_INTERFACE_CALIBRATOR(KeyEvent.VK_UNDEFINED),
    CIVILIAN_AEROSPACE_INSTRUCTOR(KeyEvent.VK_UNDEFINED),
    CIVILIAN_JUMPSHIP_NAVIGATOR(KeyEvent.VK_UNDEFINED),
    POLITICAL_AGITATOR(KeyEvent.VK_UNDEFINED),
    NOBLE_STEWARD(KeyEvent.VK_UNDEFINED),
    BATTLE_ROM_EDITOR(KeyEvent.VK_UNDEFINED),
    LUXURY_COMPANION(KeyEvent.VK_UNDEFINED),
    PLANETARY_SURVEYOR(KeyEvent.VK_UNDEFINED),
    DISGRACED_NOBLE(KeyEvent.VK_UNDEFINED),
    SPACEPORT_BUREAUCRAT(KeyEvent.VK_UNDEFINED),
    VR_ENTERTAINER(KeyEvent.VK_UNDEFINED),
    PERSONAL_ARCHIVIST(KeyEvent.VK_UNDEFINED),
    INDUSTRIAL_INSPECTOR(KeyEvent.VK_UNDEFINED),
    SPACEPORT_COURIER(KeyEvent.VK_UNDEFINED),
    MEKBAY_SCHEDULER(KeyEvent.VK_UNDEFINED),
    MILITARY_CONTRACTOR(KeyEvent.VK_UNDEFINED),
    MILITARY_HOLO_FILMER(KeyEvent.VK_UNDEFINED),
    WEAPONS_TESTER(KeyEvent.VK_UNDEFINED),
    PARAMILITARY_TRAINER(KeyEvent.VK_UNDEFINED),
    MILITIA_LEADER(KeyEvent.VK_UNDEFINED),
    FIELD_HOSPITAL_ADMINISTRATOR(KeyEvent.VK_UNDEFINED),
    CIVILIAN_REQUISITION_OFFICER(KeyEvent.VK_UNDEFINED),
    TRAINING_SIM_DESIGNER(KeyEvent.VK_UNDEFINED),
    COMMS_OPERATOR(KeyEvent.VK_UNDEFINED),
    DECOMMISSIONING_SPECIALIST(KeyEvent.VK_UNDEFINED),
    WAR_CRIME_INVESTIGATOR(KeyEvent.VK_UNDEFINED),
    SECURITY_ADVISOR(KeyEvent.VK_UNDEFINED),
    MILITARY_RECRUITER(KeyEvent.VK_UNDEFINED),
    MILITARY_PONY_EXPRESS_COURIER(KeyEvent.VK_UNDEFINED),
    MILITARY_PAINTER(KeyEvent.VK_UNDEFINED),
    MORALE_OFFICER(KeyEvent.VK_UNDEFINED),
    COMBAT_CHAPLAIN(KeyEvent.VK_UNDEFINED),
    LOGISTICS_COORDINATOR(KeyEvent.VK_UNDEFINED),
    FOOD_TRUCK_OPERATOR(KeyEvent.VK_UNDEFINED),
    MESS_HALL_MANAGER(KeyEvent.VK_UNDEFINED),
    CIVILIAN_LIAISON(KeyEvent.VK_UNDEFINED),
    FIELD_LAUNDRY_OPERATOR(KeyEvent.VK_UNDEFINED),
    MUNITIONS_CLERK(KeyEvent.VK_UNDEFINED),
    SECURITY_DESK_OPERATOR(KeyEvent.VK_UNDEFINED),
    ARMS_DEALER(KeyEvent.VK_UNDEFINED),
    DATA_LAUNDERER(KeyEvent.VK_UNDEFINED),
    UNLICENSED_CHEMIST(KeyEvent.VK_UNDEFINED),
    SMUGGLER(KeyEvent.VK_UNDEFINED),
    PROTECTION_RACKETEER(KeyEvent.VK_UNDEFINED),
    THUG(KeyEvent.VK_UNDEFINED),
    GANG_LEADER(KeyEvent.VK_UNDEFINED),
    PRISON_FIXER(KeyEvent.VK_UNDEFINED),
    TORTURER(KeyEvent.VK_UNDEFINED),
    INTELLIGENCE_ANALYST(KeyEvent.VK_UNDEFINED),
    CODEBREAKER(KeyEvent.VK_UNDEFINED),
    COUNTERINTELLIGENCE_LIAISON(KeyEvent.VK_UNDEFINED),
    DATA_INTERCEPT_OPERATOR(KeyEvent.VK_UNDEFINED),
    SURVEILLANCE_EXPERT(KeyEvent.VK_UNDEFINED),
    TECH_ENCRYPTION(KeyEvent.VK_UNDEFINED),
    DEEP_COVER_OPERATIVE(KeyEvent.VK_UNDEFINED),
    INTERROGATOR(KeyEvent.VK_UNDEFINED),
    DATA_HARVESTER(KeyEvent.VK_UNDEFINED),
    SIGNAL_JAMMING_SPECIALIST(KeyEvent.VK_UNDEFINED),
    CORPORATE_ESPIONAGE_AGENT(KeyEvent.VK_UNDEFINED),
    LOYALTY_MONITOR(KeyEvent.VK_UNDEFINED),
    MEDIA_MANIPULATOR(KeyEvent.VK_UNDEFINED),
    CIVILIAN_DEBRIEFER(KeyEvent.VK_UNDEFINED),
    SPACEPORT_ENGINEER(KeyEvent.VK_UNDEFINED),
    FRONTIER_DOCTOR(KeyEvent.VK_UNDEFINED),
    DOOMSDAY_PREACHER(KeyEvent.VK_UNDEFINED),
    TAX_AUDITOR(KeyEvent.VK_UNDEFINED),
    MARKET_MANIPULATOR(KeyEvent.VK_UNDEFINED),
    SUBVERSIVE_POET(KeyEvent.VK_UNDEFINED),
    LEGAL_ARCHIVIST(KeyEvent.VK_UNDEFINED),
    CONFLICT_RESOLUTION_TRAINER(KeyEvent.VK_UNDEFINED),
    DUELIST(KeyEvent.VK_UNDEFINED),
    SCANDAL_FIXER(KeyEvent.VK_UNDEFINED),
    RELATIONSHIP_MATCHMAKER(KeyEvent.VK_UNDEFINED),
    PRISON_GUARD(KeyEvent.VK_UNDEFINED),
    GENETIC_THERAPY_SPECIALIST(KeyEvent.VK_UNDEFINED),
    IMPLANT_SURGEON(KeyEvent.VK_UNDEFINED),
    DISEASE_CONTROL_ADMINISTRATOR(KeyEvent.VK_UNDEFINED),
    TRAUMA_COUNSELOR(KeyEvent.VK_UNDEFINED),
    ORGAN_HARVESTER(KeyEvent.VK_UNDEFINED),
    PHYSICAL_REHABILITATION_THERAPIST(KeyEvent.VK_UNDEFINED),
    SURGICAL_SIMULATOR_INSTRUCTOR(KeyEvent.VK_UNDEFINED),
    COMBAT_PROSTHETICS_FITTER(KeyEvent.VK_UNDEFINED),
    PLANETARY_ADAPTATION_PHYSIOLOGIST(KeyEvent.VK_UNDEFINED),
    ZERO_G_PHYSICAL_THERAPIST(KeyEvent.VK_UNDEFINED),
    ORBITAL_DEBRIS_TRACKER(KeyEvent.VK_UNDEFINED),
    PUBLIC_TRANSPORT_OVERSEER(KeyEvent.VK_UNDEFINED),
    MILITARY_PROMOTER(KeyEvent.VK_UNDEFINED),
    AEROSPACE_SCAVENGER(KeyEvent.VK_UNDEFINED),
    MYTHOLOGIST(KeyEvent.VK_UNDEFINED),
    GRAFFITI_ARTIST(KeyEvent.VK_UNDEFINED),
    PSYOPS_BROADCASTER(KeyEvent.VK_UNDEFINED),
    WEDDING_PLANNER(KeyEvent.VK_UNDEFINED),
    FREIGHT_LIFT_OPERATOR(KeyEvent.VK_UNDEFINED),
    REEDUCATION_SPECIALIST(KeyEvent.VK_UNDEFINED),
    GUILD_LIAISON(KeyEvent.VK_UNDEFINED),
    ILLEGAL_PET_SMUGGLER(KeyEvent.VK_UNDEFINED),
    HOLO_DJ(KeyEvent.VK_UNDEFINED),
    CLAIMS_ARBITRATOR(KeyEvent.VK_UNDEFINED),
    LIVESTREAM_ENTERTAINER(KeyEvent.VK_UNDEFINED),
    MILITARY_TATTOO_ARTIST(KeyEvent.VK_UNDEFINED),
    RATION_DISTRIBUTOR(KeyEvent.VK_UNDEFINED),
    MINEFIELD_PLANNER(KeyEvent.VK_UNDEFINED),
    CARGO_SEAL_INSPECTOR(KeyEvent.VK_UNDEFINED),
    INTERIOR_DECORATOR(KeyEvent.VK_UNDEFINED),
    RIOT_RESPONSE_PLANNER(KeyEvent.VK_UNDEFINED),
    SYSTEMS_CONSULTANT(KeyEvent.VK_UNDEFINED),
    TECH_AIR_FILTRATION(KeyEvent.VK_UNDEFINED),
    EARLY_DETECTION_SYSTEMS_OPERATOR(KeyEvent.VK_UNDEFINED),
    CIVIC_CONTROLLER(KeyEvent.VK_UNDEFINED),
    PUBLIC_EXECUTION_BROADCASTER(KeyEvent.VK_UNDEFINED),
    IDENTITY_FABRICATOR(KeyEvent.VK_UNDEFINED),
    NOBLE_HEIR_IN_HIDING(KeyEvent.VK_UNDEFINED),
    PERSONAL_SOMMELIER(KeyEvent.VK_UNDEFINED),
    PHILOSOPHER(KeyEvent.VK_UNDEFINED),
    MILITARY_ACADEMY_DROPOUT(KeyEvent.VK_UNDEFINED),
    ASTECH_TRAINER(KeyEvent.VK_UNDEFINED),
    NOBLE_PAGE(KeyEvent.VK_UNDEFINED),
    FALSE_PROPHET(KeyEvent.VK_UNDEFINED),
    CULTIST(KeyEvent.VK_UNDEFINED),
    LIBRARIAN(KeyEvent.VK_UNDEFINED),
    BANQUET_PLANNER(KeyEvent.VK_UNDEFINED),
    COMMUNITY_LEADER(KeyEvent.VK_UNDEFINED),
    LOREKEEPER(KeyEvent.VK_UNDEFINED),
    ELECTION_FIXER(KeyEvent.VK_UNDEFINED),
    SURVEILLANCE_SWEEPER(KeyEvent.VK_UNDEFINED),
    LOYALTY_AUDITOR(KeyEvent.VK_UNDEFINED),
    DATA_LEAK_TRACKER(KeyEvent.VK_UNDEFINED),
    PROFESSIONAL_COSPLAYER(KeyEvent.VK_UNDEFINED),
    PLANETARY_MIGRATION_COORDINATOR(KeyEvent.VK_UNDEFINED),
    RADIATION_RISK_MONITOR(KeyEvent.VK_UNDEFINED),
    DROPSHIP_ENTERTAINMENT_OFFICER(KeyEvent.VK_UNDEFINED),
    JUMPSHIP_BOTANIST(KeyEvent.VK_UNDEFINED),
    LOCAL_WARLORD(KeyEvent.VK_UNDEFINED),
    MISCELLANEOUS_JOB(KeyEvent.VK_UNDEFINED),
    NOBLE(KeyEvent.VK_UNDEFINED),
    COMMON_CRIMINAL(KeyEvent.VK_UNDEFINED);
    // endregion Enum Declarations

    // region Variable Declarations
    private static final MMLogger logger = MMLogger.create(PersonnelRole.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelRole";

    private final PersonnelRoleSubType subType;
    private final boolean hasClanName;
    private final int mnemonic; // Unused: J, K, Q, X, Z
    private final int strength;
    private final int body;
    private final int dexterity;
    private final int reflexes;
    private final int intelligence;
    private final int willpower;
    private final int charisma;
    // endregion Variable Declarations

    // region Constructors
    PersonnelRole(final int mnemonic) {
        this(PersonnelRoleSubType.CIVILIAN, false, mnemonic, 4, 4, 4, 4, 4, 4, 4);
    }

    PersonnelRole(final PersonnelRoleSubType subType, final int mnemonic, final int strength, final int body,
          final int dexterity, final int reflexes, final int intelligence, final int willpower, final int charisma) {
        this(subType, false, mnemonic, strength, body, dexterity, reflexes, intelligence, willpower, charisma);
    }

    PersonnelRole(final PersonnelRoleSubType subType, final boolean hasClanName, final int mnemonic, final int strength,
          final int body, final int dexterity, final int reflexes, final int intelligence, final int willpower,
          final int charisma) {
        this.subType = subType;
        this.hasClanName = hasClanName;
        this.mnemonic = mnemonic;
        this.strength = strength;
        this.body = body;
        this.dexterity = dexterity;
        this.reflexes = reflexes;
        this.intelligence = intelligence;
        this.willpower = willpower;
        this.charisma = charisma;
    }
    // endregion Constructors

    // region Getters

    /**
     * Retrieves the label for this instance, optionally using a clan-specific label if applicable.
     *
     * <p>This method generates a label based on a specific resource bundle key. If the specified
     * option to use a clan label is enabled and a clan name is available, it retrieves the clan-specific label.
     * Otherwise, it retrieves the standard label.</p>
     *
     * @param isClan A flag indicating whether to use the clan-specific label. If {@code true} and the instance has a
     *               clan name, the clan-specific label will be used.
     *
     * @return The formatted label string, either clan-specific or standard, based on the provided flag and
     *       availability.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public String getLabel(final boolean isClan) {
        final boolean useClan = isClan && hasClanName;
        return getFormattedTextAt(RESOURCE_BUNDLE, name() + ".label" + (useClan ? ".clan" : ""));
    }

    /**
     * @deprecated use {@link #getTooltip(boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String getDescription() {
        return getDescription(false);
    }

    /**
     * Retrieves the plain text description for this personnel role from the resource bundle.
     *
     * @return the description string associated with the personnel role.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getDescription(final boolean isClan) {
        final boolean useClan = isClan && hasClanName;
        return getTextAt(RESOURCE_BUNDLE, name() + ".description" + (useClan ? ".clan" : ""));
    }

    /**
     * @deprecated use {@link #getTooltip(boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String getTooltip() {
        return getTooltip(false);
    }

    /**
     * Builds an HTML tooltip string providing a description of this personnel role and a list of related skills with
     * their linked attributes, if available.
     *
     * <p>If the list of skills for this profession is not empty, the tooltip will include each skill followed by its
     * relevant {@link SkillAttribute} types. Otherwise, a default formatted description is returned from the resource
     * bundle.</p>
     *
     * @return an HTML-formatted tooltip string detailing the profession and corresponding skills.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getTooltip(final boolean isClan) {
        StringBuilder tooltip = new StringBuilder(getDescription(isClan)).append("<br>");

        List<String> skills = new ArrayList<>();
        if (this == VEHICLE_CREW) {
            // Vehicle Crew is a bit of a special case as any of these skills makes a character eligible for
            // experience level improvements.
            List<String> relevantSkills = List.of(SkillType.S_TECH_MEK,
                  SkillType.S_TECH_AERO,
                  SkillType.S_TECH_MECHANIC,
                  SkillType.S_TECH_BA,
                  SkillType.S_SURGERY,
                  SkillType.S_MEDTECH,
                  SkillType.S_ASTECH,
                  SkillType.S_COMMUNICATIONS,
                  SkillType.S_ART_COOKING,
                  SkillType.S_SENSOR_OPERATIONS);
            skills.addAll(relevantSkills);
        } else if (this == SOLDIER) {
            skills.addAll(INFANTRY_GUNNERY_SKILLS);
        } else {
            skills.addAll(getSkillsForProfession());
        }

        for (String skill : skills) {
            tooltip.append("<br>- ").append(skill);

            SkillType skillType = SkillType.getType(skill);

            if (skillType != null) {
                List<SkillAttribute> linkedAttributes = new ArrayList<>(skillType.getAttributes());
                linkedAttributes.remove(SkillAttribute.NONE);

                for (SkillAttribute attribute : linkedAttributes) {
                    if (linkedAttributes.indexOf(attribute) == 0) {
                        tooltip.append(" (");
                    }

                    tooltip.append(attribute.getLabel());

                    if (linkedAttributes.indexOf(attribute) < linkedAttributes.size() - 1) {
                        tooltip.append(", ");
                    } else if (linkedAttributes.indexOf(attribute) == linkedAttributes.size() - 1) {
                        tooltip.append(')');
                    }
                }
            }
        }

        return tooltip.toString();
    }

    public int getMnemonic() {
        return mnemonic;
    }

    /**
     * Retrieves the corresponding modifier value for the given {@link SkillAttribute}.
     *
     * <p>This method determines the modifier by matching the input {@link SkillAttribute}
     * to its associated property within the class. The mapping is as follows:</p>
     * <ul>
     *     <li>{@link SkillAttribute#NONE}: Returns {@code 0} as no modification is applicable.</li>
     *     <li>{@link SkillAttribute#STRENGTH}: Returns the value of the {@code strength} modifier.</li>
     *     <li>{@link SkillAttribute#BODY}: Returns the value of the {@code body} modifier.</li>
     *     <li>{@link SkillAttribute#REFLEXES}: Returns the value of the {@code reflexes} modifier.</li>
     *     <li>{@link SkillAttribute#DEXTERITY}: Returns the value of the {@code dexterity} modifier.</li>
     *     <li>{@link SkillAttribute#INTELLIGENCE}: Returns the value of the {@code intelligence} modifier.</li>
     *     <li>{@link SkillAttribute#WILLPOWER}: Returns the value of the {@code willpower} modifier.</li>
     *     <li>{@link SkillAttribute#CHARISMA}: Returns the value of the {@code charisma} modifier.</li>
     * </ul>
     *
     * @param attribute The {@link SkillAttribute} for which the modifier value is requested. Must not be {@code null}.
     *
     * @return The integer value of the modifier corresponding to the given {@link SkillAttribute}.
     */
    public int getAttributeModifier(final SkillAttribute attribute) {
        if (attribute == null) {
            return 0;
        }

        return switch (attribute) {
            case NONE -> 0;
            case STRENGTH -> strength;
            case BODY -> body;
            case REFLEXES -> reflexes;
            case DEXTERITY -> dexterity;
            case INTELLIGENCE -> intelligence;
            case WILLPOWER -> willpower;
            case CHARISMA -> charisma;
        };
    }

    /**
     * @return a list of skill names representing the profession-appropriate skills
     *
     * @see #getSkillsForProfession(boolean, boolean, boolean, boolean)
     */
    public List<String> getSkillsForProfession() {
        return getSkillsForProfession(false, false, false, false, false);
    }

    /**
     * Retrieves the list of skill names relevant to this profession, tailored according to provided campaign or
     * generation options.
     *
     * <p>The set of returned skills may vary depending on input flags that define whether certain support or
     * specialty skills (such as Negotiation, Administration, or Artillery) should be included for appropriate
     * roles.</p>
     *
     * <p>This method is typically used during personnel creation or skill assignment to ensure each role receives a
     * fitting skill set based on campaign rules and user preferences.</p>
     *
     * @param isAdminsHaveNegotiation    if {@code true}, includes Negotiation skill for administrators
     * @param isDoctorsUseAdministration if {@code true}, includes Administration skill for medical roles
     * @param isTechsUseAdministration   if {@code true}, includes Administration skill for technical roles
     * @param isUseArtillery             if {@code true}, includes Artillery skills where applicable
     *
     * @return a list of skill names representing the profession-appropriate skills
     */
    public List<String> getSkillsForProfession(boolean isAdminsHaveNegotiation, boolean isDoctorsUseAdministration,
          boolean isTechsUseAdministration, boolean isUseArtillery) {
        return getSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration, isTechsUseAdministration,
              isUseArtillery, false);
    }


    /**
     * Retrieves the list of skill names relevant to this profession, tailored according to provided campaign or
     * generation options.
     *
     * <p>The set of returned skills may vary depending on input flags that define whether certain support or
     * specialty skills (such as Negotiation, Administration, or Artillery) should be included for appropriate
     * roles.</p>
     *
     * <p>This method is typically used during personnel creation or skill assignment to ensure each role receives a
     * fitting skill set based on campaign rules and user preferences.</p>
     *
     * @param isAdminsHaveNegotiation    if {@code true}, includes Negotiation skill for administrators
     * @param isDoctorsUseAdministration if {@code true}, includes Administration skill for medical roles
     * @param isTechsUseAdministration   if {@code true}, includes Administration skill for technical roles
     * @param isUseArtillery             if {@code true}, includes Artillery skills where applicable
     * @param includeExpandedSkills      if {@code true}, includes expanded skills for conventional infantry and vehicle
     *                                   crewmember roles
     *
     * @return a list of skill names representing the profession-appropriate skills
     */
    public List<String> getSkillsForProfession(boolean isAdminsHaveNegotiation, boolean isDoctorsUseAdministration,
          boolean isTechsUseAdministration, boolean isUseArtillery, boolean includeExpandedSkills) {
        return switch (this) {
            case MEKWARRIOR -> {
                if (isUseArtillery) {
                    yield List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_ARTILLERY);
                } else {
                    yield List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
                }
            }
            case LAM_PILOT ->
                  List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case GROUND_VEHICLE_DRIVER -> List.of(SkillType.S_PILOT_GVEE);
            case NAVAL_VEHICLE_DRIVER -> List.of(SkillType.S_PILOT_NVEE);
            case VTOL_PILOT -> List.of(SkillType.S_PILOT_VTOL);
            case VEHICLE_GUNNER -> {
                if (isUseArtillery) {
                    yield List.of(SkillType.S_GUN_VEE, SkillType.S_ARTILLERY);
                } else {
                    yield List.of(SkillType.S_GUN_VEE);
                }
            }
            case MECHANIC -> List.of(SkillType.S_TECH_MECHANIC);
            case VEHICLE_CREW -> {
                if (includeExpandedSkills) {
                    yield List.of(S_TECH_MEK,
                          S_TECH_AERO,
                          S_TECH_MECHANIC,
                          S_TECH_BA,
                          S_SURGERY,
                          S_MEDTECH,
                          S_ASTECH,
                          S_COMMUNICATIONS,
                          S_SENSOR_OPERATIONS,
                          S_ART_COOKING);
                } else {
                    yield List.of(SkillType.S_TECH_MECHANIC, SkillType.S_GUN_VEE);
                }
            }
            case AEROSPACE_PILOT -> List.of(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case CONVENTIONAL_AIRCRAFT_PILOT -> List.of(SkillType.S_GUN_JET, SkillType.S_PILOT_JET);
            case PROTOMEK_PILOT -> List.of(SkillType.S_GUN_PROTO);
            case BATTLE_ARMOUR -> List.of(SkillType.S_GUN_BA, SkillType.S_ANTI_MEK);
            case SOLDIER -> {
                if (includeExpandedSkills) {
                    yield INFANTRY_GUNNERY_SKILLS;
                } else {
                    yield List.of(SkillType.S_SMALL_ARMS);
                }
            }
            case VESSEL_PILOT -> List.of(SkillType.S_PILOT_SPACE);
            case VESSEL_GUNNER -> List.of(SkillType.S_GUN_SPACE);
            case VESSEL_CREW -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillType.S_TECH_VESSEL, SkillType.S_ADMIN);
                } else {
                    yield List.of(SkillType.S_TECH_VESSEL);
                }
            }
            case VESSEL_NAVIGATOR -> List.of(SkillType.S_NAVIGATION);
            case MEK_TECH -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillType.S_TECH_MEK, SkillType.S_ADMIN);
                } else {
                    yield List.of(SkillType.S_TECH_MEK);
                }
            }
            case AERO_TEK -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillType.S_TECH_AERO, SkillType.S_ADMIN);
                } else {
                    yield List.of(SkillType.S_TECH_AERO);
                }
            }
            case BA_TECH -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillType.S_TECH_BA, SkillType.S_ADMIN);
                } else {
                    yield List.of(SkillType.S_TECH_BA);
                }
            }
            case ASTECH -> List.of(SkillType.S_ASTECH);
            case DOCTOR -> {
                if (isDoctorsUseAdministration) {
                    yield List.of(SkillType.S_SURGERY, SkillType.S_ADMIN);
                } else {
                    yield List.of(SkillType.S_SURGERY);
                }
            }
            case MEDIC -> List.of(SkillType.S_MEDTECH);
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR -> {
                if (isAdminsHaveNegotiation) {
                    yield List.of(SkillType.S_ADMIN, SkillType.S_NEGOTIATION);
                } else {
                    yield List.of(SkillType.S_ADMIN);
                }
            }
            case DEPENDENT, NONE -> List.of();
            case MISCELLANEOUS_JOB -> List.of(SkillType.S_CAREER_ANY);
            case ADULT_ENTERTAINER -> List.of(SkillType.S_ART_OTHER, SkillType.S_ACTING);
            case ANTIQUARIAN -> List.of(SkillType.S_INTEREST_ANTIQUES, SkillType.S_INTEREST_HISTORY);
            case SPORTS_STAR -> List.of(SkillType.S_CAREER_ANY, SkillType.S_INTEREST_SPORTS);
            case ASTROGRAPHER -> List.of(SkillType.S_INTEREST_CARTOGRAPHY, SkillType.S_NAVIGATION);
            case BARBER -> List.of(SkillType.S_ART_OTHER, SkillType.S_INTEREST_FASHION);
            case BARTENDER -> List.of(SkillType.S_STREETWISE, SkillType.S_INTEREST_POP_CULTURE);
            case WAR_CORRESPONDENT -> List.of(SkillType.S_ART_WRITING, SkillType.S_INTEREST_MILITARY);
            case BRAWLER -> List.of(SkillType.S_MARTIAL_ARTS, SkillType.S_STREETWISE);
            case BROKER -> List.of(SkillType.S_STREETWISE, SkillType.S_NEGOTIATION);
            case CHEF -> List.of(SkillType.S_ART_COOKING, SkillType.S_LEADER);
            case CIVILIAN_AERO_MECHANIC -> List.of(SkillType.S_TECH_AERO, SkillType.S_TECH_MECHANIC);
            case CIVILIAN_DROPSHIP_PILOT -> List.of(SkillType.S_PILOT_SPACE, SkillType.S_PROTOCOLS);
            case POLICE_OFFICER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_INVESTIGATION);
            case CIVILIAN_VTOL_PILOT -> List.of(SkillType.S_PILOT_VTOL, SkillType.S_TECH_MECHANIC);
            case CIVIL_CLERK -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case CLOWN -> List.of(SkillType.S_ACROBATICS, SkillType.S_ACTING);
            case CON_ARTIST -> List.of(SkillType.S_DISGUISE, SkillType.S_ACTING);
            case MILITARY_CORONER -> List.of(SkillType.S_SURGERY, SkillType.S_SCIENCE_PHARMACOLOGY);
            case COURIER -> List.of(SkillType.S_RUNNING, SkillType.S_STREETWISE);
            case CRIMINAL_MECHANIC -> List.of(SkillType.S_STREETWISE, SkillType.S_TECH_MECHANIC);
            case CULTURAL_CENSOR -> List.of(SkillType.S_INTEREST_POLITICS, SkillType.S_INTEREST_LITERATURE);
            case CULTURAL_LIAISON -> List.of(SkillType.S_PROTOCOLS, SkillType.S_LANGUAGES);
            case CUSTOMS_INSPECTOR -> List.of(SkillType.S_INVESTIGATION, SkillType.S_PROTOCOLS);
            case DATA_SMUGGLER -> List.of(SkillType.S_COMPUTERS, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC);
            case DATA_ANALYST -> List.of(SkillType.S_COMPUTERS, SkillType.S_SCIENCE_MATHEMATICS);
            case SPACEPORT_WORKER -> List.of(SkillType.S_ASTECH, SkillType.S_PILOT_GVEE);
            case DRUG_DEALER -> List.of(SkillType.S_STREETWISE, SkillType.S_SCIENCE_PHARMACOLOGY);
            case FACTORY_WORKER -> List.of(SkillType.S_ASTECH, SkillType.S_TECH_MECHANIC);
            case LIVESTOCK_FARMER -> List.of(SkillType.S_ANIMAL_HANDLING, SkillType.S_SCIENCE_XENOBIOLOGY);
            case AGRI_FARMER -> List.of(SkillType.S_ASTECH, SkillType.S_SCIENCE_BIOLOGY);
            case FIREFIGHTER -> List.of(SkillType.S_PILOT_GVEE, SkillType.S_ANTI_MEK);
            case FISHER -> List.of(SkillType.S_INTEREST_FISHING, SkillType.S_PILOT_NVEE);
            case GAMBLER -> List.of(SkillType.S_APPRAISAL, SkillType.S_INTEREST_GAMBLING);
            case CIVILIAN_DOCTOR -> List.of(SkillType.S_SURGERY, SkillType.S_ADMIN);
            case HACKER -> List.of(SkillType.S_CRYPTOGRAPHY, SkillType.S_COMPUTERS);
            case HERALD -> List.of(SkillType.S_ACTING, SkillType.S_PROTOCOLS);
            case HISTORIAN -> List.of(SkillType.S_INTEREST_HISTORY, SkillType.S_ART_WRITING);
            case HOLO_CARTOGRAPHER -> List.of(SkillType.S_INTEREST_CARTOGRAPHY, SkillType.S_SCIENCE_GEOLOGY);
            case HOLO_GAMER -> List.of(SkillType.S_INTEREST_HOLO_GAMES, SkillType.S_COMPUTERS);
            case HOLO_JOURNALIST -> List.of(SkillType.S_INVESTIGATION, SkillType.S_ART_WRITING);
            case HOLO_STAR -> List.of(SkillType.S_ACTING, SkillType.S_INTEREST_HOLO_CINEMA);
            case INDUSTRIAL_MEK_PILOT -> List.of(SkillType.S_PILOT_MEK, SkillType.S_TECH_MEK);
            case INFORMATION_BROKER -> List.of(SkillType.S_STREETWISE, SkillType.S_INVESTIGATION);
            case MILITARY_LIAISON -> List.of(SkillType.S_INVESTIGATION, SkillType.S_COMMUNICATIONS);
            case JANITOR -> List.of(SkillType.S_ASTECH, SkillType.S_CAREER_ANY);
            case JUMPSHIP_CHEF -> List.of(SkillType.S_ART_COOKING, SkillType.S_ZERO_G_OPERATIONS);
            case EXOSKELETON_LABORER -> List.of(SkillType.S_ASTECH, SkillType.S_TECH_BA);
            case LAWYER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_INTEREST_LAW);
            case PROPHET -> List.of(SkillType.S_INTEREST_ASTROLOGY, SkillType.S_INTEREST_THEOLOGY);
            case RELIC_HUNTER -> List.of(SkillType.S_INTEREST_ARCHEOLOGY, SkillType.S_SURVIVAL);
            case MEDIATOR -> List.of(SkillType.S_NEGOTIATION, SkillType.S_INTEREST_POLITICS);
            case MEDICAL_RESEARCHER -> List.of(SkillType.S_SCIENCE_PHARMACOLOGY, SkillType.S_SCIENCE_BIOLOGY);
            case MEK_RANGE_INSTRUCTOR -> List.of(SkillType.S_GUN_MEK, SkillType.S_LEADER);
            case MERCHANT -> List.of(SkillType.S_NEGOTIATION, SkillType.S_APPRAISAL);
            case MILITARY_ACCOUNTANT -> List.of(SkillType.S_INTEREST_ECONOMICS, SkillType.S_ADMIN);
            case MILITARY_ANALYST -> List.of(SkillType.S_STRATEGY, SkillType.S_SCIENCE_MATHEMATICS);
            case SPY -> List.of(SkillType.S_STEALTH, SkillType.S_DISGUISE);
            case MILITARY_THEORIST -> List.of(SkillType.S_TACTICS, SkillType.S_INTEREST_MILITARY);
            case MINER -> List.of(SkillType.S_DEMOLITIONS, SkillType.S_TECH_MECHANIC);
            case MOUNTAIN_CLIMBER -> List.of(SkillType.S_ANTI_MEK, SkillType.S_SURVIVAL);
            case FACTORY_FOREMAN -> List.of(SkillType.S_ASTECH, SkillType.S_ADMIN);
            case MUNITIONS_FACTORY_WORKER -> List.of(SkillType.S_DEMOLITIONS, SkillType.S_ASTECH);
            case MUSICIAN -> List.of(SkillType.S_ART_INSTRUMENT, SkillType.S_INTEREST_MUSIC);
            case ORBITAL_DEFENSE_GUNNER -> List.of(SkillType.S_GUN_VEE, SkillType.S_TECH_MECHANIC);
            case ORBITAL_SHUTTLE_PILOT -> List.of(SkillType.S_PILOT_AERO, SkillType.S_PROTOCOLS);
            case PARAMEDIC -> List.of(SkillType.S_MEDTECH, SkillType.S_PILOT_GVEE);
            case PAINTER -> List.of(SkillType.S_ART_PAINTING, SkillType.S_INTEREST_MYTHOLOGY);
            case PATHFINDER -> List.of(SkillType.S_TRACKING, SkillType.S_SURVIVAL);
            case PERFORMER -> List.of(SkillType.S_ART_SINGING, SkillType.S_ART_DANCING);
            case PERSONAL_VALET -> List.of(SkillType.S_PROTOCOLS, SkillType.S_PILOT_GVEE);
            case ESCAPED_PRISONER -> List.of(SkillType.S_ESCAPE_ARTIST, SkillType.S_STEALTH);
            case PROPAGANDIST -> List.of(SkillType.S_ART_WRITING, SkillType.S_INTEREST_POLITICS);
            case PSYCHOLOGIST -> List.of(SkillType.S_SCIENCE_PSYCHOLOGY, SkillType.S_NEGOTIATION);
            case FIRING_RANGE_SAFETY_OFFICER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_LEADER);
            case RECRUITMENT_SCREENING_OFFICER -> List.of(SkillType.S_INTERROGATION, SkillType.S_SCIENCE_PSYCHOLOGY);
            case RELIGIOUS_LEADER -> List.of(SkillType.S_INTEREST_THEOLOGY, SkillType.S_LEADER);
            case REPAIR_BAY_SUPERVISOR -> List.of(SkillType.S_TECH_MECHANIC, SkillType.S_LEADER);
            case REVOLUTIONIST -> List.of(SkillType.S_INTEREST_POLITICS, SkillType.S_LEADER);
            case RITUALIST -> List.of(SkillType.S_INTEREST_THEOLOGY, SkillType.S_ART_DANCING);
            case SALVAGE_RAT -> List.of(SkillType.S_TECH_MECHANIC, SkillType.S_TECH_MEK);
            case SCRIBE -> List.of(SkillType.S_ADMIN, SkillType.S_ART_WRITING);
            case SCULPTURER -> List.of(SkillType.S_ART_SCULPTURE, SkillType.S_APPRAISAL);
            case SENSOR_TECHNICIAN -> List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_COMPUTERS);
            case CIVILIAN_PILOT -> List.of(SkillType.S_PILOT_JET, SkillType.S_PROTOCOLS);
            case STREET_SURGEON -> List.of(SkillType.S_SURGERY, SkillType.S_STREETWISE);
            case SWIMMING_INSTRUCTOR -> List.of(SkillType.S_SWIMMING, SkillType.S_TRAINING);
            case TACTICAL_ANALYST -> List.of(SkillType.S_TACTICS, SkillType.S_COMPUTERS);
            case TAILOR -> List.of(SkillType.S_ART_OTHER, SkillType.S_INTEREST_FASHION);
            case TEACHER -> List.of(SkillType.S_LEADER, SkillType.S_TRAINING);
            case TECH_COMMUNICATIONS -> List.of(SkillType.S_COMMUNICATIONS, SkillType.S_TECH_MECHANIC);
            case TECH_ZERO_G -> List.of(SkillType.S_ZERO_G_OPERATIONS, SkillType.S_TECH_VESSEL);
            case TECH_HYDROPONICS -> List.of(SkillType.S_ASTECH, SkillType.S_SCIENCE_BIOLOGY);
            case TECH_FUSION_PLANT -> List.of(SkillType.S_ASTECH, SkillType.S_SCIENCE_PHYSICS);
            case TECH_SECURITY ->
                  List.of(SkillType.S_SECURITY_SYSTEMS_ELECTRONIC, SkillType.S_SECURITY_SYSTEMS_MECHANICAL);
            case TECH_WASTE_MANAGEMENT -> List.of(SkillType.S_ASTECH, SkillType.S_CAREER_ANY);
            case TECH_WATER_RECLAMATION -> List.of(SkillType.S_ASTECH, SkillType.S_SCIENCE_CHEMISTRY);
            case THIEF -> List.of(SkillType.S_SLEIGHT_OF_HAND, SkillType.S_STREETWISE);
            case BURGLAR -> List.of(SkillType.S_STEALTH, SkillType.S_ACROBATICS);
            case TRAINING_SIM_OPERATOR -> List.of(SkillType.S_COMPUTERS, SkillType.S_TRAINING);
            case TRANSPORT_DRIVER -> List.of(SkillType.S_PILOT_GVEE, SkillType.S_TECH_MECHANIC);
            case ARTIST -> List.of(SkillType.S_ART_DRAWING, SkillType.S_COMPUTERS);
            case COUNTERFEITER -> List.of(SkillType.S_APPRAISAL, SkillType.S_STREETWISE);
            case WAREHOUSE_WORKER -> List.of(SkillType.S_ASTECH, SkillType.S_ADMIN);
            case WARFARE_PLANNER -> List.of(SkillType.S_STRATEGY, SkillType.S_INTEREST_MILITARY);
            case WEATHERCASTER -> List.of(SkillType.S_SCIENCE_PHYSICS, SkillType.S_ACTING);
            case XENOANIMAL_TRAINER -> List.of(SkillType.S_INTEREST_EXOTIC_ANIMALS, SkillType.S_ANIMAL_HANDLING);
            case XENO_BIOLOGIST -> List.of(SkillType.S_SCIENCE_XENOBIOLOGY, SkillType.S_SCIENCE_BIOLOGY);
            case GENETICIST -> List.of(SkillType.S_SCIENCE_BIOLOGY, SkillType.S_SCIENCE_GENETICS);
            case MASSEUSE -> List.of(SkillType.S_ART_OTHER, SkillType.S_MEDTECH);
            case BODYGUARD -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_PERCEPTION);
            case ARTISAN_MICROBREWER -> List.of(SkillType.S_SCIENCE_CHEMISTRY, SkillType.S_ART_COOKING);
            case INTERSTELLAR_TOURISM_GUIDE -> List.of(SkillType.S_PROTOCOLS, SkillType.S_LANGUAGES);
            case CORPORATE_CONCIERGE -> List.of(SkillType.S_PROTOCOLS, SkillType.S_STREETWISE);
            case VIRTUAL_REALITY_THERAPIST -> List.of(SkillType.S_COMPUTERS, SkillType.S_SCIENCE_PSYCHOLOGY);
            case EXOTIC_PET_CARETAKER -> List.of(SkillType.S_ANIMAL_HANDLING, SkillType.S_SCIENCE_XENOBIOLOGY);
            case CULTURAL_SENSITIVITY_ADVISOR -> List.of(SkillType.S_PROTOCOLS, SkillType.S_SCIENCE_PSYCHOLOGY);
            case PLANETARY_IMMIGRATION_ASSESSOR -> List.of(SkillType.S_ADMIN, SkillType.S_LANGUAGES);
            case PERSONAL_ASSISTANT -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case PENNILESS_NOBLE -> List.of(SkillType.S_INTEREST_POLITICS, SkillType.S_LEADER);
            case AIDE_DE_CAMP -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case NEUROHELMET_INTERFACE_CALIBRATOR -> List.of(SkillType.S_COMPUTERS, SkillType.S_TECH_MEK);
            case CIVILIAN_AEROSPACE_INSTRUCTOR -> List.of(SkillType.S_TRAINING, SkillType.S_PILOT_AERO);
            case CIVILIAN_JUMPSHIP_NAVIGATOR -> List.of(SkillType.S_NAVIGATION, SkillType.S_SCIENCE_MATHEMATICS);
            case POLITICAL_AGITATOR -> List.of(SkillType.S_INTEREST_POLITICS, SkillType.S_ACTING);
            case NOBLE_STEWARD -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case BATTLE_ROM_EDITOR -> List.of(SkillType.S_INTEREST_HOLO_CINEMA, SkillType.S_INTEREST_POLITICS);
            case LUXURY_COMPANION -> List.of(SkillType.S_ACTING, SkillType.S_PROTOCOLS);
            case PLANETARY_SURVEYOR -> List.of(SkillType.S_SCIENCE_GEOLOGY, SkillType.S_NAVIGATION);
            case DISGRACED_NOBLE -> List.of(SkillType.S_NEGOTIATION, SkillType.S_STREETWISE);
            case SPACEPORT_BUREAUCRAT -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case VR_ENTERTAINER -> List.of(SkillType.S_ACTING, SkillType.S_COMPUTERS);
            case PERSONAL_ARCHIVIST -> List.of(SkillType.S_ART_WRITING, SkillType.S_INTEREST_HISTORY);
            case INDUSTRIAL_INSPECTOR -> List.of(SkillType.S_INVESTIGATION, SkillType.S_TECH_MECHANIC);
            case SPACEPORT_COURIER -> List.of(SkillType.S_RUNNING, SkillType.S_PILOT_GVEE);
            case MEKBAY_SCHEDULER -> List.of(SkillType.S_ADMIN, SkillType.S_ASTECH);
            case MILITARY_CONTRACTOR -> List.of(SkillType.S_NEGOTIATION, SkillType.S_TECH_MECHANIC);
            case MILITARY_HOLO_FILMER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_INTEREST_HOLO_CINEMA);
            case WEAPONS_TESTER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_TECH_MECHANIC);
            case PARAMILITARY_TRAINER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_TRAINING);
            case MILITIA_LEADER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_LEADER);
            case FIELD_HOSPITAL_ADMINISTRATOR -> List.of(SkillType.S_ADMIN, SkillType.S_MEDTECH);
            case CIVILIAN_REQUISITION_OFFICER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_ADMIN);
            case TRAINING_SIM_DESIGNER -> List.of(SkillType.S_COMPUTERS, SkillType.S_TACTICS);
            case COMMS_OPERATOR -> List.of(SkillType.S_COMMUNICATIONS, SkillType.S_ADMIN);
            case DECOMMISSIONING_SPECIALIST -> List.of(SkillType.S_TECH_MEK, SkillType.S_APPRAISAL);
            case WAR_CRIME_INVESTIGATOR -> List.of(SkillType.S_INVESTIGATION, SkillType.S_INTEREST_HISTORY);
            case SECURITY_ADVISOR -> List.of(SkillType.S_TACTICS, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC);
            case MILITARY_PONY_EXPRESS_COURIER -> List.of(SkillType.S_PILOT_SPACE, SkillType.S_STEALTH);
            case MILITARY_RECRUITER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_LEADER);
            case MILITARY_PAINTER -> List.of(SkillType.S_ART_PAINTING, SkillType.S_INTEREST_MILITARY);
            case MORALE_OFFICER -> List.of(SkillType.S_ACTING, SkillType.S_LEADER);
            case COMBAT_CHAPLAIN -> List.of(SkillType.S_INTEREST_THEOLOGY, SkillType.S_MEDTECH);
            case LOGISTICS_COORDINATOR -> List.of(SkillType.S_ADMIN, SkillType.S_PILOT_GVEE);
            case FOOD_TRUCK_OPERATOR -> List.of(SkillType.S_ART_COOKING, SkillType.S_PILOT_GVEE);
            case MESS_HALL_MANAGER -> List.of(SkillType.S_ART_COOKING, SkillType.S_ADMIN);
            case CIVILIAN_LIAISON -> List.of(SkillType.S_ACTING, SkillType.S_NEGOTIATION);
            case FIELD_LAUNDRY_OPERATOR -> List.of(SkillType.S_ASTECH, SkillType.S_CAREER_ANY);
            case MUNITIONS_CLERK -> List.of(SkillType.S_ADMIN, SkillType.S_DEMOLITIONS);
            case SECURITY_DESK_OPERATOR ->
                  List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC);
            case ARMS_DEALER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_NEGOTIATION);
            case DATA_LAUNDERER -> List.of(SkillType.S_CRYPTOGRAPHY, SkillType.S_COMPUTERS);
            case UNLICENSED_CHEMIST -> List.of(SkillType.S_SCIENCE_CHEMISTRY, SkillType.S_SCIENCE_PHARMACOLOGY);
            case SMUGGLER -> List.of(SkillType.S_STEALTH, SkillType.S_PILOT_GVEE);
            case PROTECTION_RACKETEER -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_NEGOTIATION);
            case THUG -> List.of(SkillType.S_MELEE_WEAPONS, SkillType.S_STREETWISE);
            case GANG_LEADER -> List.of(SkillType.S_LEADER, SkillType.S_STREETWISE);
            case PRISON_FIXER -> List.of(SkillType.S_APPRAISAL, SkillType.S_NEGOTIATION);
            case TORTURER -> List.of(SkillType.S_INTERROGATION, SkillType.S_MEDTECH);
            case INTELLIGENCE_ANALYST -> List.of(SkillType.S_INVESTIGATION, SkillType.S_SCIENCE_MATHEMATICS);
            case CODEBREAKER -> List.of(SkillType.S_CRYPTOGRAPHY, SkillType.S_COMPUTERS);
            case COUNTERINTELLIGENCE_LIAISON -> List.of(SkillType.S_INVESTIGATION, SkillType.S_INTEREST_POLITICS);
            case DATA_INTERCEPT_OPERATOR -> List.of(SkillType.S_COMMUNICATIONS, SkillType.S_SENSOR_OPERATIONS);
            case SURVEILLANCE_EXPERT -> List.of(SkillType.S_STEALTH, SkillType.S_SENSOR_OPERATIONS);
            case TECH_ENCRYPTION -> List.of(SkillType.S_COMPUTERS, SkillType.S_CRYPTOGRAPHY);
            case DEEP_COVER_OPERATIVE -> List.of(SkillType.S_DISGUISE, SkillType.S_LANGUAGES);
            case INTERROGATOR -> List.of(SkillType.S_INTERROGATION, SkillType.S_SCIENCE_PSYCHOLOGY);
            case DATA_HARVESTER -> List.of(SkillType.S_COMPUTERS, SkillType.S_APPRAISAL);
            case SIGNAL_JAMMING_SPECIALIST -> List.of(SkillType.S_COMMUNICATIONS, SkillType.S_TECH_VESSEL);
            case CORPORATE_ESPIONAGE_AGENT -> List.of(SkillType.S_STEALTH, SkillType.S_INVESTIGATION);
            case LOYALTY_MONITOR -> List.of(SkillType.S_INVESTIGATION, SkillType.S_SCIENCE_PSYCHOLOGY);
            case MEDIA_MANIPULATOR -> List.of(SkillType.S_ART_WRITING, SkillType.S_ACTING);
            case CIVILIAN_DEBRIEFER -> List.of(SkillType.S_INTERROGATION, SkillType.S_LEADER);
            case SPACEPORT_ENGINEER -> List.of(SkillType.S_TECH_VESSEL, SkillType.S_TECH_MECHANIC);
            case FRONTIER_DOCTOR -> List.of(SkillType.S_SURGERY, SkillType.S_SURVIVAL);
            case DOOMSDAY_PREACHER -> List.of(SkillType.S_ACTING, SkillType.S_INTEREST_ASTROLOGY);
            case TAX_AUDITOR -> List.of(SkillType.S_INTEREST_ECONOMICS, SkillType.S_INVESTIGATION);
            case MARKET_MANIPULATOR -> List.of(SkillType.S_INTEREST_ECONOMICS, SkillType.S_COMPUTERS);
            case SUBVERSIVE_POET -> List.of(SkillType.S_ART_POETRY, SkillType.S_INTEREST_POLITICS);
            case LEGAL_ARCHIVIST -> List.of(SkillType.S_ADMIN, SkillType.S_INTEREST_LAW);
            case CONFLICT_RESOLUTION_TRAINER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_TRAINING);
            case DUELIST -> List.of(SkillType.S_MELEE_WEAPONS, SkillType.S_LEADER);
            case SCANDAL_FIXER -> List.of(SkillType.S_INVESTIGATION, SkillType.S_PROTOCOLS);
            case RELATIONSHIP_MATCHMAKER -> List.of(SkillType.S_PROTOCOLS, SkillType.S_NEGOTIATION);
            case PRISON_GUARD -> List.of(SkillType.S_MELEE_WEAPONS, SkillType.S_PERCEPTION);
            case GENETIC_THERAPY_SPECIALIST -> List.of(SkillType.S_SCIENCE_GENETICS, SkillType.S_MEDTECH);
            case IMPLANT_SURGEON -> List.of(SkillType.S_SURGERY, SkillType.S_ASTECH);
            case DISEASE_CONTROL_ADMINISTRATOR -> List.of(SkillType.S_SCIENCE_BIOLOGY, SkillType.S_ADMIN);
            case TRAUMA_COUNSELOR -> List.of(SkillType.S_SCIENCE_PSYCHOLOGY, SkillType.S_ADMIN);
            case ORGAN_HARVESTER -> List.of(SkillType.S_SURGERY, SkillType.S_STREETWISE);
            case PHYSICAL_REHABILITATION_THERAPIST -> List.of(SkillType.S_MEDTECH, SkillType.S_TRAINING);
            case SURGICAL_SIMULATOR_INSTRUCTOR -> List.of(SkillType.S_TRAINING, SkillType.S_SURGERY);
            case COMBAT_PROSTHETICS_FITTER -> List.of(SkillType.S_SURGERY, SkillType.S_ASTECH);
            case PLANETARY_ADAPTATION_PHYSIOLOGIST ->
                  List.of(SkillType.S_SCIENCE_BIOLOGY, SkillType.S_SCIENCE_PSYCHOLOGY);
            case ZERO_G_PHYSICAL_THERAPIST -> List.of(SkillType.S_MEDTECH, SkillType.S_ZERO_G_OPERATIONS);
            case ORBITAL_DEBRIS_TRACKER -> List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_COMPUTERS);
            case PUBLIC_TRANSPORT_OVERSEER -> List.of(SkillType.S_ADMIN, SkillType.S_PILOT_GVEE);
            case MILITARY_PROMOTER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_INTEREST_MILITARY);
            case AEROSPACE_SCAVENGER -> List.of(SkillType.S_TECH_AERO, SkillType.S_PILOT_AERO);
            case MYTHOLOGIST -> List.of(SkillType.S_INTEREST_MYTHOLOGY, SkillType.S_INTEREST_HISTORY);
            case GRAFFITI_ARTIST -> List.of(SkillType.S_ART_PAINTING, SkillType.S_STEALTH);
            case PSYOPS_BROADCASTER -> List.of(SkillType.S_COMMUNICATIONS, SkillType.S_INTEREST_POLITICS);
            case WEDDING_PLANNER -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case FREIGHT_LIFT_OPERATOR -> List.of(SkillType.S_PILOT_GVEE, SkillType.S_ASTECH);
            case REEDUCATION_SPECIALIST -> List.of(SkillType.S_TRAINING, SkillType.S_SCIENCE_PSYCHOLOGY);
            case GUILD_LIAISON -> List.of(SkillType.S_NEGOTIATION, SkillType.S_PROTOCOLS);
            case ILLEGAL_PET_SMUGGLER -> List.of(SkillType.S_ANIMAL_HANDLING, SkillType.S_STREETWISE);
            case HOLO_DJ -> List.of(SkillType.S_INTEREST_MUSIC, SkillType.S_COMPUTERS);
            case CLAIMS_ARBITRATOR -> List.of(SkillType.S_APPRAISAL, SkillType.S_INTEREST_LAW);
            case LIVESTREAM_ENTERTAINER -> List.of(SkillType.S_ACTING, SkillType.S_COMPUTERS);
            case MILITARY_TATTOO_ARTIST -> List.of(SkillType.S_ART_DRAWING, SkillType.S_INTEREST_MILITARY);
            case RATION_DISTRIBUTOR -> List.of(SkillType.S_ADMIN, SkillType.S_NEGOTIATION);
            case MINEFIELD_PLANNER -> List.of(SkillType.S_TACTICS, SkillType.S_DEMOLITIONS);
            case CARGO_SEAL_INSPECTOR -> List.of(SkillType.S_INVESTIGATION, SkillType.S_TECH_MECHANIC);
            case INTERIOR_DECORATOR -> List.of(SkillType.S_ART_DRAWING, SkillType.S_ART_PAINTING);
            case RIOT_RESPONSE_PLANNER -> List.of(SkillType.S_TACTICS, SkillType.S_SMALL_ARMS);
            case SYSTEMS_CONSULTANT -> List.of(SkillType.S_COMPUTERS, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC);
            case TECH_AIR_FILTRATION -> List.of(SkillType.S_TECH_MECHANIC, SkillType.S_SCIENCE_CHEMISTRY);
            case EARLY_DETECTION_SYSTEMS_OPERATOR -> List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_INVESTIGATION);
            case CIVIC_CONTROLLER -> List.of(SkillType.S_ADMIN, SkillType.S_MELEE_WEAPONS);
            case PUBLIC_EXECUTION_BROADCASTER -> List.of(SkillType.S_ACTING, SkillType.S_INTEREST_POLITICS);
            case IDENTITY_FABRICATOR -> List.of(SkillType.S_FORGERY, SkillType.S_PROTOCOLS);
            case NOBLE_HEIR_IN_HIDING -> List.of(SkillType.S_DISGUISE, SkillType.S_INTEREST_POLITICS);
            case PERSONAL_SOMMELIER -> List.of(SkillType.S_APPRAISAL, SkillType.S_ART_COOKING);
            case PHILOSOPHER -> List.of(SkillType.S_INTEREST_PHILOSOPHY, SkillType.S_ART_WRITING);
            case MILITARY_ACADEMY_DROPOUT -> List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
            case ASTECH_TRAINER -> List.of(SkillType.S_TECH_MEK, SkillType.S_TRAINING);
            case NOBLE_PAGE -> List.of(SkillType.S_PROTOCOLS, SkillType.S_ADMIN);
            case FALSE_PROPHET -> List.of(SkillType.S_ACTING, SkillType.S_INTEREST_THEOLOGY);
            case CULTIST -> List.of(SkillType.S_INTEREST_THEOLOGY, SkillType.S_MELEE_WEAPONS);
            case LIBRARIAN -> List.of(SkillType.S_INTEREST_LITERATURE, SkillType.S_ADMIN);
            case BANQUET_PLANNER -> List.of(SkillType.S_ADMIN, SkillType.S_ART_COOKING);
            case COMMUNITY_LEADER -> List.of(SkillType.S_NEGOTIATION, SkillType.S_LEADER);
            case LOREKEEPER -> List.of(SkillType.S_INTEREST_HISTORY, SkillType.S_ART_WRITING);
            case ELECTION_FIXER -> List.of(SkillType.S_FORGERY, SkillType.S_INTEREST_POLITICS);
            case SURVEILLANCE_SWEEPER ->
                  List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC);
            case LOYALTY_AUDITOR -> List.of(SkillType.S_INTERROGATION, SkillType.S_ADMIN);
            case DATA_LEAK_TRACKER -> List.of(SkillType.S_COMPUTERS, SkillType.S_INVESTIGATION);
            case PROFESSIONAL_COSPLAYER -> List.of(SkillType.S_ACTING, SkillType.S_ART_OTHER);
            case PLANETARY_MIGRATION_COORDINATOR -> List.of(SkillType.S_ADMIN, SkillType.S_PROTOCOLS);
            case RADIATION_RISK_MONITOR -> List.of(SkillType.S_SENSOR_OPERATIONS, SkillType.S_SCIENCE_PHYSICS);
            case DROPSHIP_ENTERTAINMENT_OFFICER -> List.of(SkillType.S_ART_INSTRUMENT, SkillType.S_TECH_VESSEL);
            case JUMPSHIP_BOTANIST -> List.of(SkillType.S_SCIENCE_BIOLOGY, SkillType.S_TECH_VESSEL);
            case LOCAL_WARLORD -> List.of(SkillType.S_SMALL_ARMS, SkillType.S_LEADER);
            case NOBLE -> List.of(SkillType.S_PROTOCOLS, SkillType.S_INTEREST_POLITICS);
            case COMMON_CRIMINAL -> List.of(SkillType.S_STREETWISE, SkillType.S_INTEREST_GAMBLING);
        };
    }
    // endregion Getters

    // region Boolean Comparison Methods

    /**
     * @return {@code true} if the personnel has the Mek Warrior role, {@code false} otherwise.
     */
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    /**
     * @return {@code true} if the personnel has the LAM Pilot role, {@code false} otherwise.
     */
    public boolean isLAMPilot() {
        return this == LAM_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Ground Vehicle Driver role, {@code false} otherwise.
     */
    public boolean isGroundVehicleDriver() {
        return this == GROUND_VEHICLE_DRIVER;
    }

    /**
     * @return {@code true} if the personnel has the Naval Vehicle Driver role, {@code false} otherwise.
     */
    public boolean isNavalVehicleDriver() {
        return this == NAVAL_VEHICLE_DRIVER;
    }

    /**
     * @return {@code true} if the personnel has the VTOL Pilot role, {@code false} otherwise.
     */
    public boolean isVTOLPilot() {
        return this == VTOL_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Vehicle Gunner role, {@code false} otherwise.
     */
    public boolean isVehicleGunner() {
        return this == VEHICLE_GUNNER;
    }

    /**
     * @return {@code true} if the personnel has the Vehicle Crew role, {@code false} otherwise.
     */
    public boolean isVehicleCrew() {
        return this == VEHICLE_CREW;
    }

    /**
     * Returns {@code true} if this profession is suitable for vehicle crew positions.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isVehicleCrewExtended() {
        return this == VEHICLE_CREW ||
                     this == MEK_TECH ||
                     this == AERO_TEK ||
                     this == MECHANIC ||
                     this == BA_TECH ||
                     this == ASTECH ||
                     this == DOCTOR ||
                     this == MEDIC ||
                     this == COMMS_OPERATOR ||
                     this == TECH_COMMUNICATIONS ||
                     this == SENSOR_TECHNICIAN ||
                     this == CHEF;
    }


    /**
     * @return {@code true} if the personnel has the Aerospace Pilot role, {@code false} otherwise.
     */
    public boolean isAerospacePilot() {
        return this == AEROSPACE_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Conventional Aircraft Pilot role, {@code false} otherwise.
     */
    public boolean isConventionalAircraftPilot() {
        return this == CONVENTIONAL_AIRCRAFT_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the ProtoMek Pilot role, {@code false} otherwise.
     */
    public boolean isProtoMekPilot() {
        return this == PROTOMEK_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Battle Armor Pilot role, {@code false} otherwise.
     */
    public boolean isBattleArmour() {
        return this == BATTLE_ARMOUR;
    }

    /**
     * @return {@code true} if the personnel has the Soldier role, {@code false} otherwise.
     */
    public boolean isSoldier() {
        return this == SOLDIER;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Pilot role, {@code false} otherwise.
     */
    public boolean isVesselPilot() {
        return this == VESSEL_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Gunner role, {@code false} otherwise.
     */
    public boolean isVesselGunner() {
        return this == VESSEL_GUNNER;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Crew role, {@code false} otherwise.
     */
    public boolean isVesselCrew() {
        return this == VESSEL_CREW;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Navigator role, {@code false} otherwise.
     */
    public boolean isVesselNavigator() {
        return this == VESSEL_NAVIGATOR;
    }

    /**
     * @return {@code true} if the personnel has the MekTech role, {@code false} otherwise.
     */
    public boolean isMekTech() {
        return this == MEK_TECH;
    }

    /**
     * @return {@code true} if the personnel has the Mechanic role, {@code false} otherwise.
     */
    public boolean isMechanic() {
        return this == MECHANIC;
    }

    /**
     * @return {@code true} if the personnel has the AeroTek role, {@code false} otherwise.
     */
    public boolean isAeroTek() {
        return this == AERO_TEK;
    }

    /**
     * @return {@code true} if the personnel has the Battle Armor Tech role, {@code false} otherwise.
     */
    public boolean isBATech() {
        return this == BA_TECH;
    }

    /**
     * @return {@code true} if the personnel has the Astech role, {@code false} otherwise.
     */
    public boolean isAstech() {
        return this == ASTECH;
    }

    /**
     * @return {@code true} if the personnel has the Doctor role, {@code false} otherwise.
     */
    public boolean isDoctor() {
        return this == DOCTOR;
    }

    /**
     * @return {@code true} if the personnel has the Medic role, {@code false} otherwise.
     */
    public boolean isMedic() {
        return this == MEDIC;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Command role, {@code false} otherwise.
     */
    public boolean isAdministratorCommand() {
        return this == ADMINISTRATOR_COMMAND;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Logistics role, {@code false} otherwise.
     */
    public boolean isAdministratorLogistics() {
        return this == ADMINISTRATOR_LOGISTICS;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Transport role, {@code false} otherwise.
     */
    public boolean isAdministratorTransport() {
        return this == ADMINISTRATOR_TRANSPORT;
    }

    /**
     * @return {@code true} if the personnel has the Admin/HR role, {@code false} otherwise.
     */
    public boolean isAdministratorHR() {
        return this == ADMINISTRATOR_HR;
    }

    /**
     * @return {@code true} if the personnel has the Dependent role, {@code false} otherwise.
     */
    public boolean isDependent() {
        return this == DEPENDENT;
    }

    /**
     * @return {@code true} if the personnel has the None role, {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * @return {@code true} if the character has a combat role, {@code true} otherwise.
     */
    public boolean isCombat() {
        return subType == PersonnelRoleSubType.COMBAT;
    }

    /**
     * Checks if this object's subtype matches the specified {@link PersonnelRoleSubType}.
     *
     * @param subType the subtype to compare against
     *
     * @return {@code true} if this object's subtype is equal to the specified subtype; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.06
     */
    public boolean isSubType(PersonnelRoleSubType subType) {
        return this.subType == subType;
    }

    /**
     * @return {@code true} if the character is a MekWarrior or a LAM Pilot, {@code false} otherwise.
     */
    public boolean isMekWarriorGrouping() {
        return isMekWarrior() || isLAMPilot();
    }

    /**
     * @return {@code true} if the character is an Aerospace Pilot or a LAM Pilot, {@code false} otherwise.
     */
    public boolean isAerospaceGrouping() {
        return isLAMPilot() || isAerospacePilot();
    }

    /**
     * @return {@code true} if the character is assigned to the Ground Vehicle Driver, Vehicle Gunner, or the Vehicle
     *       Crew role, {@code false} otherwise.
     */
    public boolean isGroundVehicleCrew() {
        return isGroundVehicleDriver() || isVehicleGunner() || isVehicleCrewExtended();
    }

    /**
     * @return {@code true} if the character is assigned to the Naval Vehicle Driver, Vehicle Gunner, or the Vehicle
     *       Crew role, {@code false} otherwise.
     */
    public boolean isNavalVehicleCrew() {
        return isNavalVehicleDriver() || isVehicleGunner() || isVehicleCrewExtended();
    }

    /**
     * @return {@code true} if the character is assigned to the VTOL Pilot, Vehicle Gunner, or the Vehicle Crew role,
     *       {@code false} otherwise.
     */
    public boolean isVTOLCrew() {
        return isVTOLPilot() || isVehicleGunner() || isVehicleCrewExtended();
    }

    /**
     * @return {@code true} if the character is assigned to the Ground Vehicle Crew, Naval Vehicle Crew, or the VTOL
     *       Pilot role, {@code false} otherwise.
     */
    public boolean isVehicleCrewMember() {
        return isGroundVehicleCrew() || isNavalVehicleDriver() || isVTOLPilot();
    }

    /**
     * @return {@code true} if the character is assigned to the Soldier, or the Battle Armor role, {@code false}
     *       otherwise.
     */
    public boolean isSoldierOrBattleArmour() {
        return isSoldier() || isBattleArmour();
    }

    /**
     * @return {@code true} if the character is assigned to the Vessel Pilot, Vessel Gunner, Vessel Crew, or the Vessel
     *       Navigator role, {@code false} otherwise.
     */
    public boolean isVesselCrewMember() {
        return isVesselPilot() || isVesselGunner() || isVesselCrew() || isVesselNavigator();
    }

    /**
     * @return {@code true} if the character is assigned to a support role, excluding civilian roles, {@code false}
     *       otherwise.
     */
    public boolean isSupport() {
        return isSupport(true);
    }

    /**
     * @param excludeCivilian whether to exclude civilian roles
     *
     * @return {@code true} if the character is assigned to a support role, {@code false} otherwise.
     */
    public boolean isSupport(final boolean excludeCivilian) {
        return isSubType(PersonnelRoleSubType.SUPPORT) || (!excludeCivilian && isCivilian());
    }

    /**
     * Checks whether a character is assigned to a technician role. If checking secondary roles, {@code isTechSecondary}
     * should be used.
     *
     * @return {@code true} if the character is assigned to a technician role, {@code false} otherwise.
     */
    public boolean isTech() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech() || isVesselCrew();
    }

    /**
     * Checks whether a character is assigned to a technician role. If checking primary roles, {@code isTech} should be
     * used.
     *
     * @return {@code true} if the character is assigned to a technician role, {@code false} otherwise.
     */
    public boolean isTechSecondary() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech();
    }

    /**
     * @return {@code true} if the character is assigned to a medical role, {@code false} otherwise.
     */
    public boolean isMedicalStaff() {
        return isDoctor() || isMedic();
    }


    /**
     * Determines if the current entity is an assistant by checking if it is either an Astech or a Medic.
     *
     * @return {@code true} if the entity is an assistant (either an Astech or a Medic), {@code false} otherwise.
     */
    public boolean isAssistant() {
        return isAstech() || isMedic();
    }

    /**
     * @return {@code true} if the character is assigned to an Administrative role, {@code false} otherwise.
     */
    public boolean isAdministrator() {
        return isAdministratorCommand() ||
                     isAdministratorLogistics() ||
                     isAdministratorTransport() ||
                     isAdministratorHR();
    }

    /**
     * @return {@code true} if the character's assigned role has a subtype of {@link PersonnelRoleSubType#CIVILIAN},
     *       {@code false} otherwise. This method no longer considers roles such as {@code DEPENDENT} or {@code NONE} as
     *       civilian roles, as in previous implementations.
     */
    public boolean isCivilian() {
        return isSubType(PersonnelRoleSubType.CIVILIAN);
    }
    // endregion Boolean Comparison Methods

    // region Static Methods

    /**
     * @return a list of roles that can be included in the personnel market
     */
    public static List<PersonnelRole> getMarketableRoles() {
        List<PersonnelRole> marketableRoles = getCombatRoles();
        marketableRoles.addAll(getSupportRoles());

        return marketableRoles;
    }

    /**
     * @return a list of personnel roles classified as combat roles.
     */
    public static List<PersonnelRole> getCombatRoles() {
        List<PersonnelRole> combatRoles = new ArrayList<>();
        for (PersonnelRole personnelRole : PersonnelRole.values()) {
            if (personnelRole.isCombat()) {
                combatRoles.add(personnelRole);
            }
        }
        return combatRoles;
    }

    /**
     * @return a list of personnel roles classified as support roles.
     */
    public static List<PersonnelRole> getSupportRoles() {
        List<PersonnelRole> supportRoles = new ArrayList<>();
        for (PersonnelRole personnelRole : PersonnelRole.values()) {
            if (personnelRole.isSubType(PersonnelRoleSubType.SUPPORT)) {
                supportRoles.add(personnelRole);
            }
        }
        return supportRoles;
    }

    /**
     * Returns a list of {@link PersonnelRole} instances that are of the subtype {@code CIVILIAN}.
     *
     * @return a {@code List<PersonnelRole>} containing all civilian personnel roles.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static List<PersonnelRole> getCivilianRoles() {
        List<PersonnelRole> civilianRoles = new ArrayList<>();
        for (PersonnelRole personnelRole : PersonnelRole.values()) {
            if (personnelRole.isSubType(PersonnelRoleSubType.CIVILIAN)) {
                civilianRoles.add(personnelRole);
            }
        }
        return civilianRoles;
    }

    /**
     * Returns a list of {@link PersonnelRole} instances that are of the subtype {@code CIVILIAN}, excluding the
     * {@code NONE} role.
     *
     * @return a {@code List<PersonnelRole>} containing all civilian personnel roles except {@code NONE}
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static List<PersonnelRole> getCivilianRolesExceptNone() {
        List<PersonnelRole> civilianRoles = getCivilianRoles();
        civilianRoles.remove(NONE);
        return civilianRoles;
    }

    /**
     * @return a list of roles that are potential primary roles. Currently, this is all bar NONE
     */
    public static List<PersonnelRole> getPrimaryRoles() {
        return Stream.of(values()).filter(role -> !role.isNone()).collect(Collectors.toList());
    }

    /**
     * @return a list of roles that are considered to be vessel (as in spacecraft) crew members
     */
    public static List<PersonnelRole> getVesselRoles() {
        return Stream.of(values()).filter(PersonnelRole::isVesselCrewMember).collect(Collectors.toList());
    }

    /**
     * @return a list of roles that are considered to be techs
     */
    public static List<PersonnelRole> getTechRoles() {
        return Stream.of(values()).filter(PersonnelRole::isTech).collect(Collectors.toList());
    }

    /**
     * @return a list of all roles that are considered to be administrators
     */
    public static List<PersonnelRole> getAdministratorRoles() {
        return Stream.of(values()).filter(PersonnelRole::isAdministrator).collect(Collectors.toList());
    }

    /**
     * @return the number of civilian roles
     */
    public static int getCivilianCount() {
        return Math.toIntExact(Stream.of(values()).filter(PersonnelRole::isCivilian).count());
    }
    // endregion Static Methods

    /**
     * Converts a given string into a {@code PersonnelRole}.
     *
     * <p>This method attempts to parse the input string into a {@code PersonnelRole} using a series of steps:</p>
     *
     * <ol>
     *   <li>If the input is {@code null} or blank, the method logs an error and returns {@code NONE}.</li>
     *   <li>Tries to parse the input as an enum name by converting it to uppercase and replacing spaces with underscores.</li>
     *   <li>Attempts to match the input string with the labels of available {@code PersonnelRole} values, both standard and clan-specific.</li>
     *   <li>Includes compatibility handling for versions earlier than 50.1 with specific string mappings.</li>
     *   <li>Finally, tries to parse the input as an ordinal value of the enum.</li>
     *   <li>If all attempts fail, the method logs an error and returns {@code NONE}.</li>
     * </ol>
     *
     * @param text The input string to be converted into a {@code PersonnelRole}.
     *
     * @return The corresponding {@code PersonnelRole} if successfully parsed, or {@code NONE} if parsing fails.
     *
     * @author Illiani
     * @since 0.50.5
     */
    public static PersonnelRole fromString(String text) {
        if (text == null || text.isBlank()) {
            logger.error("Unable to parse text into a PersonnelRole. Returning NONE");
            return NONE;
        }

        // Parse from name
        try {
            return PersonnelRole.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        // Parse from label
        try {
            for (PersonnelRole personnelRole : PersonnelRole.values()) {
                if (personnelRole.getLabel(false).equalsIgnoreCase(text)) {
                    return personnelRole;
                }

                if (personnelRole.getLabel(true).equalsIgnoreCase(text)) {
                    return personnelRole;
                }
            }
        } catch (Exception ignored) {
        }

        // Parse from ordinal
        try {
            return PersonnelRole.values()[MathUtility.parseInt(text, NONE.ordinal())];
        } catch (Exception ignored) {
        }

        logger.error("Unable to parse {} into a PersonnelRole. Returning NONE", text);
        return NONE;
    }
    // endregion File I/O

    /**
     * This method is not recommended to be used in MekHQ, but is provided for non-specified utilization
     *
     * @return the base name of this role, without applying any overrides
     */
    @Override
    public String toString() {
        return getLabel(false);
    }

    /**
     * Returns an array of all {@link PersonnelRole} values sorted alphabetically by their display label.
     *
     * <p>
     * The sorting is performed based on the label returned by {@code getLabel(clanCampaign)} for each role, ensuring
     * that the roles are ordered according to the user-facing names, which may differ depending on whether a clan
     * campaign is in effect.
     * </p>
     *
     * @param clanCampaign {@code true} to use labels appropriate for a clan campaign; {@code false} to use standard
     *                     labels
     *
     * @return a {@code PersonnelRole[]} containing all enum values sorted alphabetically by label
     *
     * @since 0.50.06
     */
    public static PersonnelRole[] getValuesSortedAlphabetically(boolean clanCampaign) {
        return Arrays.stream(PersonnelRole.values())
                     .sorted(Comparator.comparing(role -> role.getLabel(clanCampaign)))
                     .toArray(PersonnelRole[]::new);
    }
}
