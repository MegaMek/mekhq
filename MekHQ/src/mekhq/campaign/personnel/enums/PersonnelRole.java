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
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;

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
            List<String> relevantSkills = List.of(SkillTypeNew.S_TECH_MEK.name(),
                  SkillTypeNew.S_TECH_AERO.name(),
                  SkillTypeNew.S_TECH_MECHANIC.name(),
                  SkillTypeNew.S_TECH_BA.name(),
                  SkillTypeNew.S_SURGERY.name(),
                  SkillTypeNew.S_MEDTECH.name(),
                  SkillTypeNew.S_ASTECH.name(),
                  SkillTypeNew.S_COMMUNICATIONS.name(),
                  SkillTypeNew.S_ART_COOKING.name(),
                  SkillTypeNew.S_SENSOR_OPERATIONS.name());
            skills.addAll(relevantSkills);
        } else {
            skills.addAll(getSkillsForProfession());
        }

        for (String skill : skills) {
            tooltip.append("<br>- ").append(skill);

            SkillTypeNew skillType = SkillTypeNew.getType(skill);

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
        return getSkillsForProfession(false, false, false, false);
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
        return switch (this) {
            case MEKWARRIOR -> {
                if (isUseArtillery) {
                    yield List.of(SkillTypeNew.S_GUN_MEK.name(),
                          SkillTypeNew.S_PILOT_MEK.name(),
                          SkillTypeNew.S_ARTILLERY.name());
                } else {
                    yield List.of(SkillTypeNew.S_GUN_MEK.name(), SkillTypeNew.S_PILOT_MEK.name());
                }
            }
            case LAM_PILOT -> List.of(SkillTypeNew.S_GUN_MEK.name(),
                  SkillTypeNew.S_PILOT_MEK.name(),
                  SkillTypeNew.S_GUN_AERO.name(),
                  SkillTypeNew.S_PILOT_AERO.name());
            case GROUND_VEHICLE_DRIVER -> List.of(SkillTypeNew.S_PILOT_GVEE.name());
            case NAVAL_VEHICLE_DRIVER -> List.of(SkillTypeNew.S_PILOT_NVEE.name());
            case VTOL_PILOT -> List.of(SkillTypeNew.S_PILOT_VTOL.name());
            case VEHICLE_GUNNER -> {
                if (isUseArtillery) {
                    yield List.of(SkillTypeNew.S_GUN_VEE.name(), SkillTypeNew.S_ARTILLERY.name());
                } else {
                    yield List.of(SkillTypeNew.S_GUN_VEE.name());
                }
            }
            case VEHICLE_CREW, MECHANIC -> List.of(SkillTypeNew.S_TECH_MECHANIC.name());
            case AEROSPACE_PILOT -> List.of(SkillTypeNew.S_GUN_AERO.name(), SkillTypeNew.S_PILOT_AERO.name());
            case CONVENTIONAL_AIRCRAFT_PILOT -> List.of(SkillTypeNew.S_GUN_JET.name(), SkillTypeNew.S_PILOT_JET.name());
            case PROTOMEK_PILOT -> List.of(SkillTypeNew.S_GUN_PROTO.name());
            case BATTLE_ARMOUR -> List.of(SkillTypeNew.S_GUN_BA.name(), SkillTypeNew.S_ANTI_MEK.name());
            case SOLDIER -> List.of(SkillTypeNew.S_SMALL_ARMS.name());
            case VESSEL_PILOT -> List.of(SkillTypeNew.S_PILOT_SPACE.name());
            case VESSEL_GUNNER -> List.of(SkillTypeNew.S_GUN_SPACE.name());
            case VESSEL_CREW -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillTypeNew.S_TECH_VESSEL.name(), SkillTypeNew.S_ADMIN.name());
                } else {
                    yield List.of(SkillTypeNew.S_TECH_VESSEL.name());
                }
            }
            case VESSEL_NAVIGATOR -> List.of(SkillTypeNew.S_NAVIGATION.name());
            case MEK_TECH -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillTypeNew.S_TECH_MEK.name(), SkillTypeNew.S_ADMIN.name());
                } else {
                    yield List.of(SkillTypeNew.S_TECH_MEK.name());
                }
            }
            case AERO_TEK -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillTypeNew.S_TECH_AERO.name(), SkillTypeNew.S_ADMIN.name());
                } else {
                    yield List.of(SkillTypeNew.S_TECH_AERO.name());
                }
            }
            case BA_TECH -> {
                if (isTechsUseAdministration) {
                    yield List.of(SkillTypeNew.S_TECH_BA.name(), SkillTypeNew.S_ADMIN.name());
                } else {
                    yield List.of(SkillTypeNew.S_TECH_BA.name());
                }
            }
            case ASTECH -> List.of(SkillTypeNew.S_ASTECH.name());
            case DOCTOR -> {
                if (isDoctorsUseAdministration) {
                    yield List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_ADMIN.name());
                } else {
                    yield List.of(SkillTypeNew.S_SURGERY.name());
                }
            }
            case MEDIC -> List.of(SkillTypeNew.S_MEDTECH.name());
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR -> {
                if (isAdminsHaveNegotiation) {
                    yield List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_NEGOTIATION.name());
                } else {
                    yield List.of(SkillTypeNew.S_ADMIN.name());
                }
            }
            case DEPENDENT, NONE -> List.of();
            case MISCELLANEOUS_JOB -> List.of(SkillTypeNew.S_CAREER_ANY.name());
            case ADULT_ENTERTAINER -> List.of(SkillTypeNew.S_ART_OTHER.name(), SkillTypeNew.S_ACTING.name());
            case ANTIQUARIAN ->
                  List.of(SkillTypeNew.S_INTEREST_ANTIQUES.name(), SkillTypeNew.S_INTEREST_HISTORY.name());
            case SPORTS_STAR -> List.of(SkillTypeNew.S_CAREER_ANY.name(), SkillTypeNew.S_INTEREST_SPORTS.name());
            case ASTROGRAPHER -> List.of(SkillTypeNew.S_INTEREST_CARTOGRAPHY.name(), SkillTypeNew.S_NAVIGATION.name());
            case BARBER -> List.of(SkillTypeNew.S_ART_OTHER.name(), SkillTypeNew.S_INTEREST_FASHION.name());
            case BARTENDER -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_INTEREST_POP_CULTURE.name());
            case WAR_CORRESPONDENT ->
                  List.of(SkillTypeNew.S_ART_WRITING.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case BRAWLER -> List.of(SkillTypeNew.S_MARTIAL_ARTS.name(), SkillTypeNew.S_STREETWISE.name());
            case BROKER -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_NEGOTIATION.name());
            case CHEF -> List.of(SkillTypeNew.S_ART_COOKING.name(), SkillTypeNew.S_LEADER.name());
            case CIVILIAN_AERO_MECHANIC ->
                  List.of(SkillTypeNew.S_TECH_AERO.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case CIVILIAN_DROPSHIP_PILOT -> List.of(SkillTypeNew.S_PILOT_SPACE.name(), SkillTypeNew.S_PROTOCOLS.name());
            case POLICE_OFFICER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_INVESTIGATION.name());
            case CIVILIAN_VTOL_PILOT -> List.of(SkillTypeNew.S_PILOT_VTOL.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case CIVIL_CLERK -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case CLOWN -> List.of(SkillTypeNew.S_ACROBATICS.name(), SkillTypeNew.S_ACTING.name());
            case CON_ARTIST -> List.of(SkillTypeNew.S_DISGUISE.name(), SkillTypeNew.S_ACTING.name());
            case MILITARY_CORONER -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_SCIENCE_PHARMACOLOGY.name());
            case COURIER -> List.of(SkillTypeNew.S_RUNNING.name(), SkillTypeNew.S_STREETWISE.name());
            case CRIMINAL_MECHANIC -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case CULTURAL_CENSOR ->
                  List.of(SkillTypeNew.S_INTEREST_POLITICS.name(), SkillTypeNew.S_INTEREST_LITERATURE.name());
            case CULTURAL_LIAISON -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_LANGUAGES.name());
            case CUSTOMS_INSPECTOR -> List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_PROTOCOLS.name());
            case DATA_SMUGGLER ->
                  List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name());
            case DATA_ANALYST -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_SCIENCE_MATHEMATICS.name());
            case SPACEPORT_WORKER -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case DRUG_DEALER -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_SCIENCE_PHARMACOLOGY.name());
            case FACTORY_WORKER -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case LIVESTOCK_FARMER ->
                  List.of(SkillTypeNew.S_ANIMAL_HANDLING.name(), SkillTypeNew.S_SCIENCE_XENOBIOLOGY.name());
            case AGRI_FARMER -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_SCIENCE_BIOLOGY.name());
            case FIREFIGHTER -> List.of(SkillTypeNew.S_PILOT_GVEE.name(), SkillTypeNew.S_ANTI_MEK.name());
            case FISHER -> List.of(SkillTypeNew.S_INTEREST_FISHING.name(), SkillTypeNew.S_PILOT_NVEE.name());
            case GAMBLER -> List.of(SkillTypeNew.S_APPRAISAL.name(), SkillTypeNew.S_INTEREST_GAMBLING.name());
            case CIVILIAN_DOCTOR -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_ADMIN.name());
            case HACKER -> List.of(SkillTypeNew.S_CRYPTOGRAPHY.name(), SkillTypeNew.S_COMPUTERS.name());
            case HERALD -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_PROTOCOLS.name());
            case HISTORIAN -> List.of(SkillTypeNew.S_INTEREST_HISTORY.name(), SkillTypeNew.S_ART_WRITING.name());
            case HOLO_CARTOGRAPHER ->
                  List.of(SkillTypeNew.S_INTEREST_CARTOGRAPHY.name(), SkillTypeNew.S_SCIENCE_GEOLOGY.name());
            case HOLO_GAMER -> List.of(SkillTypeNew.S_INTEREST_HOLO_GAMES.name(), SkillTypeNew.S_COMPUTERS.name());
            case HOLO_JOURNALIST -> List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_ART_WRITING.name());
            case HOLO_STAR -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_INTEREST_HOLO_CINEMA.name());
            case INDUSTRIAL_MEK_PILOT -> List.of(SkillTypeNew.S_PILOT_MEK.name(), SkillTypeNew.S_TECH_MEK.name());
            case INFORMATION_BROKER -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_INVESTIGATION.name());
            case MILITARY_LIAISON -> List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_COMMUNICATIONS.name());
            case JANITOR -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_CAREER_ANY.name());
            case JUMPSHIP_CHEF -> List.of(SkillTypeNew.S_ART_COOKING.name(), SkillTypeNew.S_ZERO_G_OPERATIONS.name());
            case EXOSKELETON_LABORER -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_TECH_BA.name());
            case LAWYER -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_INTEREST_LAW.name());
            case PROPHET -> List.of(SkillTypeNew.S_INTEREST_ASTROLOGY.name(), SkillTypeNew.S_INTEREST_THEOLOGY.name());
            case RELIC_HUNTER -> List.of(SkillTypeNew.S_INTEREST_ARCHEOLOGY.name(), SkillTypeNew.S_SURVIVAL.name());
            case MEDIATOR -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case MEDICAL_RESEARCHER ->
                  List.of(SkillTypeNew.S_SCIENCE_PHARMACOLOGY.name(), SkillTypeNew.S_SCIENCE_BIOLOGY.name());
            case MEK_RANGE_INSTRUCTOR -> List.of(SkillTypeNew.S_GUN_MEK.name(), SkillTypeNew.S_LEADER.name());
            case MERCHANT -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_APPRAISAL.name());
            case MILITARY_ACCOUNTANT -> List.of(SkillTypeNew.S_INTEREST_ECONOMICS.name(), SkillTypeNew.S_ADMIN.name());
            case MILITARY_ANALYST -> List.of(SkillTypeNew.S_STRATEGY.name(), SkillTypeNew.S_SCIENCE_MATHEMATICS.name());
            case SPY -> List.of(SkillTypeNew.S_STEALTH.name(), SkillTypeNew.S_DISGUISE.name());
            case MILITARY_THEORIST -> List.of(SkillTypeNew.S_TACTICS.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case MINER -> List.of(SkillTypeNew.S_DEMOLITIONS.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case MOUNTAIN_CLIMBER -> List.of(SkillTypeNew.S_ANTI_MEK.name(), SkillTypeNew.S_SURVIVAL.name());
            case FACTORY_FOREMAN -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_ADMIN.name());
            case MUNITIONS_FACTORY_WORKER -> List.of(SkillTypeNew.S_DEMOLITIONS.name(), SkillTypeNew.S_ASTECH.name());
            case MUSICIAN -> List.of(SkillTypeNew.S_ART_INSTRUMENT.name(), SkillTypeNew.S_INTEREST_MUSIC.name());
            case ORBITAL_DEFENSE_GUNNER -> List.of(SkillTypeNew.S_GUN_VEE.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case ORBITAL_SHUTTLE_PILOT -> List.of(SkillTypeNew.S_PILOT_AERO.name(), SkillTypeNew.S_PROTOCOLS.name());
            case PARAMEDIC -> List.of(SkillTypeNew.S_MEDTECH.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case PAINTER -> List.of(SkillTypeNew.S_ART_PAINTING.name(), SkillTypeNew.S_INTEREST_MYTHOLOGY.name());
            case PATHFINDER -> List.of(SkillTypeNew.S_TRACKING.name(), SkillTypeNew.S_SURVIVAL.name());
            case PERFORMER -> List.of(SkillTypeNew.S_ART_SINGING.name(), SkillTypeNew.S_ART_DANCING.name());
            case PERSONAL_VALET -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case ESCAPED_PRISONER -> List.of(SkillTypeNew.S_ESCAPE_ARTIST.name(), SkillTypeNew.S_STEALTH.name());
            case PROPAGANDIST -> List.of(SkillTypeNew.S_ART_WRITING.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case PSYCHOLOGIST -> List.of(SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name(), SkillTypeNew.S_NEGOTIATION.name());
            case FIRING_RANGE_SAFETY_OFFICER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_LEADER.name());
            case RECRUITMENT_SCREENING_OFFICER ->
                  List.of(SkillTypeNew.S_INTERROGATION.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case RELIGIOUS_LEADER -> List.of(SkillTypeNew.S_INTEREST_THEOLOGY.name(), SkillTypeNew.S_LEADER.name());
            case REPAIR_BAY_SUPERVISOR -> List.of(SkillTypeNew.S_TECH_MECHANIC.name(), SkillTypeNew.S_LEADER.name());
            case REVOLUTIONIST -> List.of(SkillTypeNew.S_INTEREST_POLITICS.name(), SkillTypeNew.S_LEADER.name());
            case RITUALIST -> List.of(SkillTypeNew.S_INTEREST_THEOLOGY.name(), SkillTypeNew.S_ART_DANCING.name());
            case SALVAGE_RAT -> List.of(SkillTypeNew.S_TECH_MECHANIC.name(), SkillTypeNew.S_TECH_MEK.name());
            case SCRIBE -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_ART_WRITING.name());
            case SCULPTURER -> List.of(SkillTypeNew.S_ART_SCULPTURE.name(), SkillTypeNew.S_APPRAISAL.name());
            case SENSOR_TECHNICIAN -> List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_COMPUTERS.name());
            case CIVILIAN_PILOT -> List.of(SkillTypeNew.S_PILOT_JET.name(), SkillTypeNew.S_PROTOCOLS.name());
            case STREET_SURGEON -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_STREETWISE.name());
            case SWIMMING_INSTRUCTOR -> List.of(SkillTypeNew.S_SWIMMING.name(), SkillTypeNew.S_TRAINING.name());
            case TACTICAL_ANALYST -> List.of(SkillTypeNew.S_TACTICS.name(), SkillTypeNew.S_COMPUTERS.name());
            case TAILOR -> List.of(SkillTypeNew.S_ART_OTHER.name(), SkillTypeNew.S_INTEREST_FASHION.name());
            case TEACHER -> List.of(SkillTypeNew.S_LEADER.name(), SkillTypeNew.S_TRAINING.name());
            case TECH_COMMUNICATIONS ->
                  List.of(SkillTypeNew.S_COMMUNICATIONS.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case TECH_ZERO_G -> List.of(SkillTypeNew.S_ZERO_G_OPERATIONS.name(), SkillTypeNew.S_TECH_VESSEL.name());
            case TECH_HYDROPONICS -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_SCIENCE_BIOLOGY.name());
            case TECH_FUSION_PLANT -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_SCIENCE_PHYSICS.name());
            case TECH_SECURITY -> List.of(SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name(),
                  SkillTypeNew.S_SECURITY_SYSTEMS_MECHANICAL.name());
            case TECH_WASTE_MANAGEMENT -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_CAREER_ANY.name());
            case TECH_WATER_RECLAMATION ->
                  List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_SCIENCE_CHEMISTRY.name());
            case THIEF -> List.of(SkillTypeNew.S_SLEIGHT_OF_HAND.name(), SkillTypeNew.S_STREETWISE.name());
            case BURGLAR -> List.of(SkillTypeNew.S_STEALTH.name(), SkillTypeNew.S_ACROBATICS.name());
            case TRAINING_SIM_OPERATOR -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_TRAINING.name());
            case TRANSPORT_DRIVER -> List.of(SkillTypeNew.S_PILOT_GVEE.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case ARTIST -> List.of(SkillTypeNew.S_ART_DRAWING.name(), SkillTypeNew.S_COMPUTERS.name());
            case COUNTERFEITER -> List.of(SkillTypeNew.S_APPRAISAL.name(), SkillTypeNew.S_STREETWISE.name());
            case WAREHOUSE_WORKER -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_ADMIN.name());
            case WARFARE_PLANNER -> List.of(SkillTypeNew.S_STRATEGY.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case WEATHERCASTER -> List.of(SkillTypeNew.S_SCIENCE_PHYSICS.name(), SkillTypeNew.S_ACTING.name());
            case XENOANIMAL_TRAINER ->
                  List.of(SkillTypeNew.S_INTEREST_EXOTIC_ANIMALS.name(), SkillTypeNew.S_ANIMAL_HANDLING.name());
            case XENO_BIOLOGIST ->
                  List.of(SkillTypeNew.S_SCIENCE_XENOBIOLOGY.name(), SkillTypeNew.S_SCIENCE_BIOLOGY.name());
            case GENETICIST -> List.of(SkillTypeNew.S_SCIENCE_BIOLOGY.name(), SkillTypeNew.S_SCIENCE_GENETICS.name());
            case MASSEUSE -> List.of(SkillTypeNew.S_ART_OTHER.name(), SkillTypeNew.S_MEDTECH.name());
            case BODYGUARD -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_PERCEPTION.name());
            case ARTISAN_MICROBREWER ->
                  List.of(SkillTypeNew.S_SCIENCE_CHEMISTRY.name(), SkillTypeNew.S_ART_COOKING.name());
            case INTERSTELLAR_TOURISM_GUIDE ->
                  List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_LANGUAGES.name());
            case CORPORATE_CONCIERGE -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_STREETWISE.name());
            case VIRTUAL_REALITY_THERAPIST ->
                  List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case EXOTIC_PET_CARETAKER ->
                  List.of(SkillTypeNew.S_ANIMAL_HANDLING.name(), SkillTypeNew.S_SCIENCE_XENOBIOLOGY.name());
            case CULTURAL_SENSITIVITY_ADVISOR ->
                  List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case PLANETARY_IMMIGRATION_ASSESSOR ->
                  List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_LANGUAGES.name());
            case PERSONAL_ASSISTANT -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case PENNILESS_NOBLE -> List.of(SkillTypeNew.S_INTEREST_POLITICS.name(), SkillTypeNew.S_LEADER.name());
            case AIDE_DE_CAMP -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case NEUROHELMET_INTERFACE_CALIBRATOR ->
                  List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_TECH_MEK.name());
            case CIVILIAN_AEROSPACE_INSTRUCTOR ->
                  List.of(SkillTypeNew.S_TRAINING.name(), SkillTypeNew.S_PILOT_AERO.name());
            case CIVILIAN_JUMPSHIP_NAVIGATOR ->
                  List.of(SkillTypeNew.S_NAVIGATION.name(), SkillTypeNew.S_SCIENCE_MATHEMATICS.name());
            case POLITICAL_AGITATOR -> List.of(SkillTypeNew.S_INTEREST_POLITICS.name(), SkillTypeNew.S_ACTING.name());
            case NOBLE_STEWARD -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case BATTLE_ROM_EDITOR ->
                  List.of(SkillTypeNew.S_INTEREST_HOLO_CINEMA.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case LUXURY_COMPANION -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_PROTOCOLS.name());
            case PLANETARY_SURVEYOR -> List.of(SkillTypeNew.S_SCIENCE_GEOLOGY.name(), SkillTypeNew.S_NAVIGATION.name());
            case DISGRACED_NOBLE -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_STREETWISE.name());
            case SPACEPORT_BUREAUCRAT -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case VR_ENTERTAINER -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_COMPUTERS.name());
            case PERSONAL_ARCHIVIST ->
                  List.of(SkillTypeNew.S_ART_WRITING.name(), SkillTypeNew.S_INTEREST_HISTORY.name());
            case INDUSTRIAL_INSPECTOR ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case SPACEPORT_COURIER -> List.of(SkillTypeNew.S_RUNNING.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case MEKBAY_SCHEDULER -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_ASTECH.name());
            case MILITARY_CONTRACTOR -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case MILITARY_HOLO_FILMER ->
                  List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_INTEREST_HOLO_CINEMA.name());
            case WEAPONS_TESTER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case PARAMILITARY_TRAINER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_TRAINING.name());
            case MILITIA_LEADER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_LEADER.name());
            case FIELD_HOSPITAL_ADMINISTRATOR -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_MEDTECH.name());
            case CIVILIAN_REQUISITION_OFFICER ->
                  List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_ADMIN.name());
            case TRAINING_SIM_DESIGNER -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_TACTICS.name());
            case COMMS_OPERATOR -> List.of(SkillTypeNew.S_COMMUNICATIONS.name(), SkillTypeNew.S_ADMIN.name());
            case DECOMMISSIONING_SPECIALIST -> List.of(SkillTypeNew.S_TECH_MEK.name(), SkillTypeNew.S_APPRAISAL.name());
            case WAR_CRIME_INVESTIGATOR ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_INTEREST_HISTORY.name());
            case SECURITY_ADVISOR ->
                  List.of(SkillTypeNew.S_TACTICS.name(), SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name());
            case MILITARY_PONY_EXPRESS_COURIER ->
                  List.of(SkillTypeNew.S_PILOT_SPACE.name(), SkillTypeNew.S_STEALTH.name());
            case MILITARY_RECRUITER -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_LEADER.name());
            case MILITARY_PAINTER ->
                  List.of(SkillTypeNew.S_ART_PAINTING.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case MORALE_OFFICER -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_LEADER.name());
            case COMBAT_CHAPLAIN -> List.of(SkillTypeNew.S_INTEREST_THEOLOGY.name(), SkillTypeNew.S_MEDTECH.name());
            case LOGISTICS_COORDINATOR -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case FOOD_TRUCK_OPERATOR -> List.of(SkillTypeNew.S_ART_COOKING.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case MESS_HALL_MANAGER -> List.of(SkillTypeNew.S_ART_COOKING.name(), SkillTypeNew.S_ADMIN.name());
            case CIVILIAN_LIAISON -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_NEGOTIATION.name());
            case FIELD_LAUNDRY_OPERATOR -> List.of(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_CAREER_ANY.name());
            case MUNITIONS_CLERK -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_DEMOLITIONS.name());
            case SECURITY_DESK_OPERATOR ->
                  List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name());
            case ARMS_DEALER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_NEGOTIATION.name());
            case DATA_LAUNDERER -> List.of(SkillTypeNew.S_CRYPTOGRAPHY.name(), SkillTypeNew.S_COMPUTERS.name());
            case UNLICENSED_CHEMIST ->
                  List.of(SkillTypeNew.S_SCIENCE_CHEMISTRY.name(), SkillTypeNew.S_SCIENCE_PHARMACOLOGY.name());
            case SMUGGLER -> List.of(SkillTypeNew.S_STEALTH.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case PROTECTION_RACKETEER -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_NEGOTIATION.name());
            case THUG -> List.of(SkillTypeNew.S_MELEE_WEAPONS.name(), SkillTypeNew.S_STREETWISE.name());
            case GANG_LEADER -> List.of(SkillTypeNew.S_LEADER.name(), SkillTypeNew.S_STREETWISE.name());
            case PRISON_FIXER -> List.of(SkillTypeNew.S_APPRAISAL.name(), SkillTypeNew.S_NEGOTIATION.name());
            case TORTURER -> List.of(SkillTypeNew.S_INTERROGATION.name(), SkillTypeNew.S_MEDTECH.name());
            case INTELLIGENCE_ANALYST ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_SCIENCE_MATHEMATICS.name());
            case CODEBREAKER -> List.of(SkillTypeNew.S_CRYPTOGRAPHY.name(), SkillTypeNew.S_COMPUTERS.name());
            case COUNTERINTELLIGENCE_LIAISON ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case DATA_INTERCEPT_OPERATOR ->
                  List.of(SkillTypeNew.S_COMMUNICATIONS.name(), SkillTypeNew.S_SENSOR_OPERATIONS.name());
            case SURVEILLANCE_EXPERT -> List.of(SkillTypeNew.S_STEALTH.name(), SkillTypeNew.S_SENSOR_OPERATIONS.name());
            case TECH_ENCRYPTION -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_CRYPTOGRAPHY.name());
            case DEEP_COVER_OPERATIVE -> List.of(SkillTypeNew.S_DISGUISE.name(), SkillTypeNew.S_LANGUAGES.name());
            case INTERROGATOR -> List.of(SkillTypeNew.S_INTERROGATION.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case DATA_HARVESTER -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_APPRAISAL.name());
            case SIGNAL_JAMMING_SPECIALIST ->
                  List.of(SkillTypeNew.S_COMMUNICATIONS.name(), SkillTypeNew.S_TECH_VESSEL.name());
            case CORPORATE_ESPIONAGE_AGENT ->
                  List.of(SkillTypeNew.S_STEALTH.name(), SkillTypeNew.S_INVESTIGATION.name());
            case LOYALTY_MONITOR ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case MEDIA_MANIPULATOR -> List.of(SkillTypeNew.S_ART_WRITING.name(), SkillTypeNew.S_ACTING.name());
            case CIVILIAN_DEBRIEFER -> List.of(SkillTypeNew.S_INTERROGATION.name(), SkillTypeNew.S_LEADER.name());
            case SPACEPORT_ENGINEER -> List.of(SkillTypeNew.S_TECH_VESSEL.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case FRONTIER_DOCTOR -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_SURVIVAL.name());
            case DOOMSDAY_PREACHER -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_INTEREST_ASTROLOGY.name());
            case TAX_AUDITOR -> List.of(SkillTypeNew.S_INTEREST_ECONOMICS.name(), SkillTypeNew.S_INVESTIGATION.name());
            case MARKET_MANIPULATOR ->
                  List.of(SkillTypeNew.S_INTEREST_ECONOMICS.name(), SkillTypeNew.S_COMPUTERS.name());
            case SUBVERSIVE_POET -> List.of(SkillTypeNew.S_ART_POETRY.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case LEGAL_ARCHIVIST -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_INTEREST_LAW.name());
            case CONFLICT_RESOLUTION_TRAINER ->
                  List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_TRAINING.name());
            case DUELIST -> List.of(SkillTypeNew.S_MELEE_WEAPONS.name(), SkillTypeNew.S_LEADER.name());
            case SCANDAL_FIXER -> List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_PROTOCOLS.name());
            case RELATIONSHIP_MATCHMAKER -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_NEGOTIATION.name());
            case PRISON_GUARD -> List.of(SkillTypeNew.S_MELEE_WEAPONS.name(), SkillTypeNew.S_PERCEPTION.name());
            case GENETIC_THERAPY_SPECIALIST ->
                  List.of(SkillTypeNew.S_SCIENCE_GENETICS.name(), SkillTypeNew.S_MEDTECH.name());
            case IMPLANT_SURGEON -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_ASTECH.name());
            case DISEASE_CONTROL_ADMINISTRATOR ->
                  List.of(SkillTypeNew.S_SCIENCE_BIOLOGY.name(), SkillTypeNew.S_ADMIN.name());
            case TRAUMA_COUNSELOR -> List.of(SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name(), SkillTypeNew.S_ADMIN.name());
            case ORGAN_HARVESTER -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_STREETWISE.name());
            case PHYSICAL_REHABILITATION_THERAPIST ->
                  List.of(SkillTypeNew.S_MEDTECH.name(), SkillTypeNew.S_TRAINING.name());
            case SURGICAL_SIMULATOR_INSTRUCTOR ->
                  List.of(SkillTypeNew.S_TRAINING.name(), SkillTypeNew.S_SURGERY.name());
            case COMBAT_PROSTHETICS_FITTER -> List.of(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_ASTECH.name());
            case PLANETARY_ADAPTATION_PHYSIOLOGIST ->
                  List.of(SkillTypeNew.S_SCIENCE_BIOLOGY.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case ZERO_G_PHYSICAL_THERAPIST ->
                  List.of(SkillTypeNew.S_MEDTECH.name(), SkillTypeNew.S_ZERO_G_OPERATIONS.name());
            case ORBITAL_DEBRIS_TRACKER ->
                  List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_COMPUTERS.name());
            case PUBLIC_TRANSPORT_OVERSEER -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PILOT_GVEE.name());
            case MILITARY_PROMOTER ->
                  List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case AEROSPACE_SCAVENGER -> List.of(SkillTypeNew.S_TECH_AERO.name(), SkillTypeNew.S_PILOT_AERO.name());
            case MYTHOLOGIST ->
                  List.of(SkillTypeNew.S_INTEREST_MYTHOLOGY.name(), SkillTypeNew.S_INTEREST_HISTORY.name());
            case GRAFFITI_ARTIST -> List.of(SkillTypeNew.S_ART_PAINTING.name(), SkillTypeNew.S_STEALTH.name());
            case PSYOPS_BROADCASTER ->
                  List.of(SkillTypeNew.S_COMMUNICATIONS.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case WEDDING_PLANNER -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case FREIGHT_LIFT_OPERATOR -> List.of(SkillTypeNew.S_PILOT_GVEE.name(), SkillTypeNew.S_ASTECH.name());
            case REEDUCATION_SPECIALIST ->
                  List.of(SkillTypeNew.S_TRAINING.name(), SkillTypeNew.S_SCIENCE_PSYCHOLOGY.name());
            case GUILD_LIAISON -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_PROTOCOLS.name());
            case ILLEGAL_PET_SMUGGLER ->
                  List.of(SkillTypeNew.S_ANIMAL_HANDLING.name(), SkillTypeNew.S_STREETWISE.name());
            case HOLO_DJ -> List.of(SkillTypeNew.S_INTEREST_MUSIC.name(), SkillTypeNew.S_COMPUTERS.name());
            case CLAIMS_ARBITRATOR -> List.of(SkillTypeNew.S_APPRAISAL.name(), SkillTypeNew.S_INTEREST_LAW.name());
            case LIVESTREAM_ENTERTAINER -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_COMPUTERS.name());
            case MILITARY_TATTOO_ARTIST ->
                  List.of(SkillTypeNew.S_ART_DRAWING.name(), SkillTypeNew.S_INTEREST_MILITARY.name());
            case RATION_DISTRIBUTOR -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_NEGOTIATION.name());
            case MINEFIELD_PLANNER -> List.of(SkillTypeNew.S_TACTICS.name(), SkillTypeNew.S_DEMOLITIONS.name());
            case CARGO_SEAL_INSPECTOR ->
                  List.of(SkillTypeNew.S_INVESTIGATION.name(), SkillTypeNew.S_TECH_MECHANIC.name());
            case INTERIOR_DECORATOR -> List.of(SkillTypeNew.S_ART_DRAWING.name(), SkillTypeNew.S_ART_PAINTING.name());
            case RIOT_RESPONSE_PLANNER -> List.of(SkillTypeNew.S_TACTICS.name(), SkillTypeNew.S_SMALL_ARMS.name());
            case SYSTEMS_CONSULTANT ->
                  List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name());
            case TECH_AIR_FILTRATION ->
                  List.of(SkillTypeNew.S_TECH_MECHANIC.name(), SkillTypeNew.S_SCIENCE_CHEMISTRY.name());
            case EARLY_DETECTION_SYSTEMS_OPERATOR ->
                  List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_INVESTIGATION.name());
            case CIVIC_CONTROLLER -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_MELEE_WEAPONS.name());
            case PUBLIC_EXECUTION_BROADCASTER ->
                  List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case IDENTITY_FABRICATOR -> List.of(SkillTypeNew.S_FORGERY.name(), SkillTypeNew.S_PROTOCOLS.name());
            case NOBLE_HEIR_IN_HIDING ->
                  List.of(SkillTypeNew.S_DISGUISE.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case PERSONAL_SOMMELIER -> List.of(SkillTypeNew.S_APPRAISAL.name(), SkillTypeNew.S_ART_COOKING.name());
            case PHILOSOPHER -> List.of(SkillTypeNew.S_INTEREST_PHILOSOPHY.name(), SkillTypeNew.S_ART_WRITING.name());
            case MILITARY_ACADEMY_DROPOUT -> List.of(SkillTypeNew.S_GUN_MEK.name(), SkillTypeNew.S_PILOT_MEK.name());
            case ASTECH_TRAINER -> List.of(SkillTypeNew.S_TECH_MEK.name(), SkillTypeNew.S_TRAINING.name());
            case NOBLE_PAGE -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_ADMIN.name());
            case FALSE_PROPHET -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_INTEREST_THEOLOGY.name());
            case CULTIST -> List.of(SkillTypeNew.S_INTEREST_THEOLOGY.name(), SkillTypeNew.S_MELEE_WEAPONS.name());
            case LIBRARIAN -> List.of(SkillTypeNew.S_INTEREST_LITERATURE.name(), SkillTypeNew.S_ADMIN.name());
            case BANQUET_PLANNER -> List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_ART_COOKING.name());
            case COMMUNITY_LEADER -> List.of(SkillTypeNew.S_NEGOTIATION.name(), SkillTypeNew.S_LEADER.name());
            case LOREKEEPER -> List.of(SkillTypeNew.S_INTEREST_HISTORY.name(), SkillTypeNew.S_ART_WRITING.name());
            case ELECTION_FIXER -> List.of(SkillTypeNew.S_FORGERY.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case SURVEILLANCE_SWEEPER ->
                  List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_SECURITY_SYSTEMS_ELECTRONIC.name());
            case LOYALTY_AUDITOR -> List.of(SkillTypeNew.S_INTERROGATION.name(), SkillTypeNew.S_ADMIN.name());
            case DATA_LEAK_TRACKER -> List.of(SkillTypeNew.S_COMPUTERS.name(), SkillTypeNew.S_INVESTIGATION.name());
            case PROFESSIONAL_COSPLAYER -> List.of(SkillTypeNew.S_ACTING.name(), SkillTypeNew.S_ART_OTHER.name());
            case PLANETARY_MIGRATION_COORDINATOR ->
                  List.of(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_PROTOCOLS.name());
            case RADIATION_RISK_MONITOR ->
                  List.of(SkillTypeNew.S_SENSOR_OPERATIONS.name(), SkillTypeNew.S_SCIENCE_PHYSICS.name());
            case DROPSHIP_ENTERTAINMENT_OFFICER ->
                  List.of(SkillTypeNew.S_ART_INSTRUMENT.name(), SkillTypeNew.S_TECH_VESSEL.name());
            case JUMPSHIP_BOTANIST -> List.of(SkillTypeNew.S_SCIENCE_BIOLOGY.name(), SkillTypeNew.S_TECH_VESSEL.name());
            case LOCAL_WARLORD -> List.of(SkillTypeNew.S_SMALL_ARMS.name(), SkillTypeNew.S_LEADER.name());
            case NOBLE -> List.of(SkillTypeNew.S_PROTOCOLS.name(), SkillTypeNew.S_INTEREST_POLITICS.name());
            case COMMON_CRIMINAL -> List.of(SkillTypeNew.S_STREETWISE.name(), SkillTypeNew.S_INTEREST_GAMBLING.name());
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
