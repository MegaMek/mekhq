/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;

import java.util.Locale;
import java.util.ResourceBundle;

public enum Profession {
    //region Enum Declarations
    MECHWARRIOR("Profession.MECHWARRIOR.text", "Profession.MECHWARRIOR.toolTipText"),
    AEROSPACE("Profession.AEROSPACE.text", "Profession.AEROSPACE.toolTipText"),
    VEHICLE("Profession.VEHICLE.text", "Profession.VEHICLE.toolTipText"),
    NAVAL("Profession.NAVAL.text", "Profession.NAVAL.toolTipText"),
    INFANTRY("Profession.INFANTRY.text", "Profession.INFANTRY.toolTipText"),
    TECH("Profession.TECH.text", "Profession.TECH.toolTipText"),
    MEDICAL("Profession.MEDICAL.text", "Profession.MEDICAL.toolTipText"),
    ADMIN("Profession.ADMIN.text", "Profession.ADMIN.toolTipText"),
    CIVILIAN("Profession.CIVILIAN.text", "Profession.CIVILIAN.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    Profession(final String name, final String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isMechWarrior() {
        return this == MECHWARRIOR;
    }

    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    public boolean isVehicle() {
        return this == VEHICLE;
    }

    public boolean isNaval() {
        return this == NAVAL;
    }

    public boolean isInfantry() {
        return this == INFANTRY;
    }

    public boolean isTech() {
        return this == TECH;
    }

    public boolean isMedical() {
        return this == MEDICAL;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isCivilian() {
        return this == CIVILIAN;
    }
    //endregion Boolean Comparisons

    /**
     * This takes this, the initial profession, converts it into a base profession, and then calls
     * getProfessionFromBase to determine the profession to use for the provided rank.
     *
     * @param rankSystem the rank system to determine the profession within
     * @param rank the rank to determine the profession for
     * @return the determined profession
     */
    public Profession getProfession(final RankSystem rankSystem, final Rank rank) {
        return getBaseProfession(rankSystem).getProfessionFromBase(rankSystem, rank);
    }

    /**
     * This takes this, the base profession, and uses it to determine the determine the profession
     * to use for the provided rank in the provided rank system
     *
     * @param rankSystem the rank system to determine the profession within
     * @param rank the rank to determine the profession for
     * @return the determined profession
     */
    public Profession getProfessionFromBase(final RankSystem rankSystem, final Rank rank) {
        Profession profession = this;

        // This runs if the rank is empty or indicates an alternative system
        for (int i = 0; i < values().length; i++) {
            if (rank.isEmpty(profession)) {
                profession = profession.getAlternateProfession(rankSystem);
            } else if (rank.indicatesAlternativeSystem(profession)) {
                profession = profession.getAlternateProfession(rank);
            } else {
                break;
            }
        }
        return profession;
    }

    /**
     * This is used to get the base profession for the rank column following any required redirects
     * based on this, the initial profession.
     *
     * @param rankSystem the rank system to get the base profession for
     * @return the final base profession for this rank system based on this being the initial base
     * profession
     */
    public Profession getBaseProfession(final RankSystem rankSystem) {
        Profession baseProfession = this;
        while (baseProfession.isEmptyProfession(rankSystem)) {
            baseProfession = baseProfession.getAlternateProfession(rankSystem);
        }
        return baseProfession;
    }

    /**
     * This is used to determine if a profession is empty, which means the first rank is an
     * alternative system while every other rank tier is empty.
     *
     * @param rankSystem the rank system to determine if this profession is empty in
     * @return whether the profession is empty or not
     */
    public boolean isEmptyProfession(final RankSystem rankSystem) {
        // MechWarrior profession cannot be empty
        // TODO : I should be allowed to be empty, and have my default replaced by another column,
        // TODO : albeit with the validator properly run before to ensure the rank system is valid.
        // TODO : The default return for getAlternativeProfession would not need to change in this case
        if (isMechWarrior()) {
            return false;
        }

        // Check the first rank to ensure it indicates an alternative rank system, as otherwise the
        // rank system is not empty.
        final Rank rank = rankSystem.getRanks().get(0);
        if (!rank.indicatesAlternativeSystem(this)) {
            return false;
        }

        // Return true if all ranks except the first are empty
        return rankSystem.getRanks().subList(1, rankSystem.getRanks().size()).stream().allMatch(r -> r.isEmpty(this));
    }

    /**
     * Determines the alternative profession to use based on the initial rank value
     * @param rankSystem the rank system to use to determine the alternative profession
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final RankSystem rankSystem) {
        return getAlternateProfession(rankSystem.getRanks().get(0));
    }

    /**
     * Determines the alternative profession to use based on the provided rank
     * @param rank the rank to determine the alternative profession for
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final Rank rank) {
        return getAlternateProfession(rank.getName(this));
    }

    /**
     * Determines the alternative profession to use based on the name of a rank
     * @param name the name of the rank to use in determining the alternative profession
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final String name) {
        switch (name.toUpperCase(Locale.ENGLISH)) {
            case "--ASF":
                return AEROSPACE;
            case "--VEE":
                return VEHICLE;
            case "--NAVAL":
                return NAVAL;
            case "--INF":
                return INFANTRY;
            case "--TECH":
                return TECH;
            case "--MEDICAL":
                return MEDICAL;
            case "--ADMIN":
                return ADMIN;
            case "--CIVILIAN":
                return CIVILIAN;
            case "--MW":
            default:
                return MECHWARRIOR;
        }
    }

    /**
     * @param role the personnel role to get the profession for
     * @return the profession for the role
     */
    public static Profession getProfessionFromPersonnelRole(final PersonnelRole role) {
        switch (role) {
            case AEROSPACE_PILOT:
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return AEROSPACE;
            case GROUND_VEHICLE_DRIVER:
            case NAVAL_VEHICLE_DRIVER:
            case VTOL_PILOT:
            case VEHICLE_GUNNER:
            case VEHICLE_CREW:
                return VEHICLE;
            case BATTLE_ARMOUR:
            case SOLDIER:
                return INFANTRY;
            case VESSEL_PILOT:
            case VESSEL_CREW:
            case VESSEL_GUNNER:
            case VESSEL_NAVIGATOR:
                return NAVAL;
            case MECH_TECH:
            case MECHANIC:
            case AERO_TECH:
            case BA_TECH:
            case ASTECH:
                return TECH;
            case DOCTOR:
            case MEDIC:
                return MEDICAL;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_HR:
            case ADMINISTRATOR_TRANSPORT:
                return ADMIN;
            case DEPENDENT:
            case NONE:
                return CIVILIAN;
            case MECHWARRIOR:
            case LAM_PILOT:
            case PROTOMECH_PILOT:
            default:
                return MECHWARRIOR;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
