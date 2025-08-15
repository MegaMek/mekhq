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

import java.util.Locale;
import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;

public enum Profession {
    // region Enum Declarations
    MEKWARRIOR("Profession.MEKWARRIOR.text", "Profession.MEKWARRIOR.toolTipText"),
    AEROSPACE("Profession.AEROSPACE.text", "Profession.AEROSPACE.toolTipText"),
    VEHICLE("Profession.VEHICLE.text", "Profession.VEHICLE.toolTipText"),
    NAVAL("Profession.NAVAL.text", "Profession.NAVAL.toolTipText"),
    INFANTRY("Profession.INFANTRY.text", "Profession.INFANTRY.toolTipText"),
    TECH("Profession.TECH.text", "Profession.TECH.toolTipText"),
    MEDICAL("Profession.MEDICAL.text", "Profession.MEDICAL.toolTipText"),
    ADMINISTRATOR("Profession.ADMINISTRATOR.text", "Profession.ADMINISTRATOR.toolTipText"),
    CIVILIAN("Profession.CIVILIAN.text", "Profession.CIVILIAN.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    Profession(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
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

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }

    public boolean isCivilian() {
        return this == CIVILIAN;
    }
    // endregion Boolean Comparison Methods

    /**
     * This takes this, the initial profession, converts it into a base profession, and then calls getProfessionFromBase
     * to determine the profession to use for the provided rank.
     *
     * @param rankSystem the rank system to determine the profession within
     * @param rank       the rank to determine the profession for
     *
     * @return the determined profession
     */
    public Profession getProfession(final RankSystem rankSystem, final Rank rank) {
        return getBaseProfession(rankSystem).getProfessionFromBase(rankSystem, rank);
    }

    /**
     * This takes this, the base profession, and uses it to determine the profession to use for the provided rank in the
     * provided rank system
     *
     * @param rankSystem the rank system to determine the profession within
     * @param rank       the rank to determine the profession for
     *
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
     * This is used to get the base profession for the rank column following any required redirects based on this, the
     * initial profession.
     *
     * @param rankSystem the rank system to get the base profession for
     *
     * @return the final base profession for this rank system based on this being the initial base profession
     */
    public Profession getBaseProfession(final RankSystem rankSystem) {
        Profession baseProfession = this;
        while (baseProfession.isEmptyProfession(rankSystem)) {
            baseProfession = baseProfession.getAlternateProfession(rankSystem);
        }
        return baseProfession;
    }

    /**
     * This is used to determine if a profession is empty, which means the first rank is an alternative system while
     * every other rank tier is empty.
     *
     * @param rankSystem the rank system to determine if this profession is empty in
     *
     * @return whether the profession is empty or not
     */
    public boolean isEmptyProfession(final RankSystem rankSystem) {
        // MekWarrior profession cannot be empty
        // TODO : I should be allowed to be empty, and have my default replaced by
        // another column,
        // TODO : albeit with the validator properly run before to ensure the rank
        // system is valid.
        // TODO : The default return for getAlternativeProfession would not need to
        // change in this case
        if (isMekWarrior()) {
            return false;
        }

        final Rank rank = rankSystem.getRanks().get(0);
        if (!rank.indicatesAlternativeSystem(this)) {
            // Return false if the first rank doesn't indicate an alternative rank system,
            // as the
            // rank system is not empty.
            return false;
        } else if (rankSystem.getRanks().size() == 1) {
            // Return true if that's the only rank to check
            return true;
        } else {
            // Return true if all ranks except the first are empty
            return rankSystem.getRanks()
                         .subList(1, rankSystem.getRanks().size())
                         .stream()
                         .allMatch(r -> r.isEmpty(this));
        }
    }

    /**
     * Determines the alternative profession to use based on the initial rank value
     *
     * @param rankSystem the rank system to use to determine the alternative profession
     *
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final RankSystem rankSystem) {
        return getAlternateProfession(rankSystem.getRanks().get(0));
    }

    /**
     * Determines the alternative profession to use based on the provided rank
     *
     * @param rank the rank to determine the alternative profession for
     *
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final Rank rank) {
        return getAlternateProfession(rank.getName(this));
    }

    /**
     * Determines the alternative profession to use based on the name of a rank
     *
     * @param name the name of the rank to use in determining the alternative profession
     *
     * @return the alternative profession determined
     */
    public Profession getAlternateProfession(final String name) {
        switch (name.toUpperCase(Locale.ENGLISH)) {
            case "--MW":
                return MEKWARRIOR;
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
                return ADMINISTRATOR;
            case "--CIVILIAN":
                return CIVILIAN;
            default:
                MMLogger.create(Profession.class).debug("Cannot get alternate profession for unknown alternative "
                                                              + name + " returning MEKWARRIOR.");
                return MEKWARRIOR;
        }
    }

    /**
     * @param role the personnel role to get the profession for
     *
     * @return the profession for the role
     */
    public static Profession getProfessionFromPersonnelRole(final PersonnelRole role) {
        return switch (role) {
            case AEROSPACE_PILOT, CONVENTIONAL_AIRCRAFT_PILOT -> AEROSPACE;
            case GROUND_VEHICLE_DRIVER, NAVAL_VEHICLE_DRIVER, VTOL_PILOT, VEHICLE_GUNNER, VEHICLE_CREW -> VEHICLE;
            case BATTLE_ARMOUR, SOLDIER -> INFANTRY;
            case VESSEL_PILOT, VESSEL_CREW, VESSEL_GUNNER, VESSEL_NAVIGATOR -> NAVAL;
            case MEK_TECH, MECHANIC, AERO_TEK, BA_TECH, ASTECH -> TECH;
            case DOCTOR, MEDIC -> MEDICAL;
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_HR, ADMINISTRATOR_TRANSPORT ->
                  ADMINISTRATOR;
            case MEKWARRIOR, LAM_PILOT, PROTOMEK_PILOT -> MEKWARRIOR;
            default -> CIVILIAN;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
