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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
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
    MEKWARRIOR(true, KeyEvent.VK_M, 4, 5, 6, 6, 4, 4, 4),

    // I used an average of the modifiers from the MekWarrior, and Aerospace Pilot ATOW Archetypes
    LAM_PILOT(true, KeyEvent.VK_UNDEFINED, 3, 4, 6, 6, 4, 4, 5),

    // ATOW: Tanker Archetype
    GROUND_VEHICLE_DRIVER(true, KeyEvent.VK_V, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Tanker Archetype
    NAVAL_VEHICLE_DRIVER(true, KeyEvent.VK_N, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Companion Chopper Pilot Archetype
    VTOL_PILOT(true, KeyEvent.VK_UNDEFINED, 4, 4, 5, 5, 4, 4, 4),

    // ATOW: Tanker Archetype
    VEHICLE_GUNNER(true, KeyEvent.VK_G, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    VEHICLE_CREW(true, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Aerospace Pilot Archetype
    AEROSPACE_PILOT(true, KeyEvent.VK_A, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Aerospace Pilot Archetype
    CONVENTIONAL_AIRCRAFT_PILOT(true, KeyEvent.VK_C, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Aerospace Pilot Archetype (most ProtoMek pilots are Aerospace Sibkbo washouts, so this made the most sense)
    PROTOMEK_PILOT(true, KeyEvent.VK_P, 2, 3, 5, 5, 4, 4, 5),

    // ATOW: Elemental Archetype
    BATTLE_ARMOUR(true, true, KeyEvent.VK_B, 7, 6, 4, 5, 3, 4, 3),

    // ATOW: Renegade Warrior Archetype
    SOLDIER(true, KeyEvent.VK_S, 5, 5, 4, 5, 4, 6, 3),

    // ATOW: Tanker Archetype
    VESSEL_PILOT(true, KeyEvent.VK_I, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Tanker Archetype
    VESSEL_GUNNER(true, KeyEvent.VK_U, 4, 5, 5, 6, 4, 4, 4),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    VESSEL_CREW(true, KeyEvent.VK_W, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype
    VESSEL_NAVIGATOR(true, KeyEvent.VK_Y, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    MEK_TECH(false, KeyEvent.VK_T, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    MECHANIC(false, KeyEvent.VK_E, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    AERO_TEK(false, KeyEvent.VK_O, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    BA_TECH(false, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Battlefield Tech Archetype (but with the reduced Dexterity removed, as that's a Linked Attribute for the
    // Technician skill)
    ASTECH(false, KeyEvent.VK_UNDEFINED, 5, 4, 5, 3, 5, 4, 3),

    // ATOW: Communications Specialist Archetype (this might seem like an odd choice, but the Attributes for this Archetype
    // work really well for this profession). However, we have switched Dexterity and Reflexes.
    DOCTOR(false, KeyEvent.VK_D, 3, 4, 5, 4, 6, 4, 4),

    // ATOW: Communications Specialist Archetype (this might seem like an odd choice, but the Attributes for this Archetype
    // work really well for this profession
    MEDIC(false, KeyEvent.VK_UNDEFINED, 3, 4, 4, 5, 6, 4, 4),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_COMMAND(false, KeyEvent.VK_UNDEFINED, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_LOGISTICS(false, KeyEvent.VK_L, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_TRANSPORT(false, KeyEvent.VK_R, 3, 3, 3, 4, 6, 3, 5),

    // ATOW: Faceman Archetype
    ADMINISTRATOR_HR(false, KeyEvent.VK_H, 3, 3, 3, 4, 6, 3, 5),

    // No archetype, but ATOW pg 35 states that the Attribute scores for an average person are 4
    DEPENDENT(false, KeyEvent.VK_UNDEFINED, 4, 4, 4, 4, 4, 4, 4),

    // If we're generating a character without a Profession, we're just going to leave them with middle of the road
    // Attribute scores (5 in everything)
    NONE(KeyEvent.VK_UNDEFINED);
    // endregion Enum Declarations

    // region Variable Declarations
    private static final MMLogger logger = MMLogger.create(PersonnelRole.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + PersonnelRole.class.getSimpleName();

    private final boolean isCombat;
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
        this(false, false, mnemonic, 5, 5, 5, 5, 5, 5, 5);
    }

    PersonnelRole(final boolean isCombat, final int mnemonic, final int strength, final int body, final int dexterity,
          final int reflexes, final int intelligence, final int willpower, final int charisma) {
        this(isCombat, false, mnemonic, strength, body, dexterity, reflexes, intelligence, willpower, charisma);
    }

    PersonnelRole(final boolean isCombat, final boolean hasClanName, final int mnemonic, final int strength,
          final int body, final int dexterity, final int reflexes, final int intelligence, final int willpower,
          final int charisma) {
        this.isCombat = isCombat;
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
     * @deprecated use {@link #getLabel(boolean)} instead
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public String getName(final boolean isClan) {
        return getLabel(isClan);
    }

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
        return isCombat;
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
     * @since 0.50.04
     * @deprecated use {@code isConventionalAircraftPilot()}. Remediated in 0.50.06, remove in 0.50.07
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean isConventionalAirGrouping() {
        return isConventionalAircraftPilot();
    }

    /**
     * @return {@code true} if the character is assigned to the Ground Vehicle Driver, Vehicle Gunner, or the Vehicle
     *       Crew role, {@code false} otherwise.
     */
    public boolean isGroundVehicleCrew() {
        return isGroundVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    /**
     * @return {@code true} if the character is assigned to the Naval Vehicle Driver, Vehicle Gunner, or the Vehicle
     *       Crew role, {@code false} otherwise.
     */
    public boolean isNavalVehicleCrew() {
        return isNavalVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    /**
     * @return {@code true} if the character is assigned to the VTOL Pilot, Vehicle Gunner, or the Vehicle Crew role,
     *       {@code false} otherwise.
     */
    public boolean isVTOLCrew() {
        return isVTOLPilot() || isVehicleGunner() || isVehicleCrew();
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
        return isSupport(false);
    }

    /**
     * @param excludeCivilian whether to exclude civilian roles
     *
     * @return {@code true} if the character is assigned to a support role, {@code false} otherwise.
     */
    public boolean isSupport(final boolean excludeCivilian) {
        return !isCombat() && (!excludeCivilian || !isCivilian());
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
     * @return Unused
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
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
     * @return {@code true} if the character's assigned role is Dependent or None, {@code false} otherwise.
     */
    public boolean isCivilian() {
        return isDependent() || isNone();
    }
    // endregion Boolean Comparison Methods

    // region Static Methods

    /**
     * @deprecated Use {@link #getMarketableRoles()}.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static List<PersonnelRole> getMilitaryRoles() {
        return getMarketableRoles();
    }

    /**
     * @return a list of roles that can be included in the personnel market
     */
    public static List<PersonnelRole> getMarketableRoles() {
        return Stream.of(values()).filter(personnelRole -> !personnelRole.isCivilian()).collect(Collectors.toList());
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
     * @return a list of personnel roles classified as support (non-combat) roles.
     */
    public static List<PersonnelRole> getSupportRoles() {
        List<PersonnelRole> supportRoles = new ArrayList<>();
        for (PersonnelRole personnelRole : PersonnelRole.values()) {
            if (!personnelRole.isCombat()) {
                supportRoles.add(personnelRole);
            }
        }
        return supportRoles;
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

    // region File I/O

    /**
     * @deprecated use {@link #fromString(String)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static PersonnelRole parseFromString(final String personnelRole) {
        return fromString(personnelRole);
    }

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

        // <50.1 compatibility
        switch (text.toUpperCase().replace(" ", "_")) {
            case "MECHWARRIOR" -> {
                return MEKWARRIOR;
            }
            case "PROTOMECH_PILOT" -> {
                return PROTOMEK_PILOT;
            }
            case "MECH_TECH" -> {
                return MEK_TECH;
            }
            case "AERO_TECH" -> {
                return AERO_TEK;
            }
            default -> {
            }
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
}
