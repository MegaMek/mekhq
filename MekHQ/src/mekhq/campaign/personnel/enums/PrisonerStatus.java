/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import java.util.ResourceBundle;

public enum PrisonerStatus {
    //region Enum Declarations
    /**
     * This is used for personnel who are not (currently) prisoners
     */
    FREE("PrisonerStatus.FREE.text", "PrisonerStatus.FREE.titleExtension"),
    /**
     * This is used to track standard personnel who are prisoners and not willing to defect
     */
    PRISONER("PrisonerStatus.PRISONER.text", "PrisonerStatus.PRISONER.titleExtension"),
    /**
     * This is used to track standard personnel who are prisoners and are willing to defect
     */
    PRISONER_DEFECTOR("PrisonerStatus.PRISONER_DEFECTOR.text", "PrisonerStatus.PRISONER_DEFECTOR.titleExtension"),
    /**
     * This is used to track clan personnel who become Bondsmen when captured
     */
    BONDSMAN("PrisonerStatus.BONDSMAN.text", "PrisonerStatus.BONDSMAN.titleExtension");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String titleExtension;
    //endregion Variable Declarations

    //region Constructors
    PrisonerStatus(final String name, final String titleExtension) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.titleExtension = resources.getString(titleExtension);
    }
    //endregion Constructors

    //region Getters
    public String getTitleExtension() {
        return titleExtension;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isFree() {
        return this == FREE;
    }
    public boolean isFreeOrBondsman() {
        return isFree() || isBondsman();
    }

    public boolean isPrisoner() {
        return this == PRISONER;
    }

    public boolean isPrisonerDefector() {
        return this == PRISONER_DEFECTOR;
    }

    public boolean isBondsman() {
        return this == BONDSMAN;
    }

    public boolean isCurrentPrisoner() {
        return isPrisoner() || isPrisonerDefector();
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * @param text The saved value to parse, either the older magic number save format or the
     *             PrisonerStatus.name() value
     * @return the Prisoner Status in question
     */
    public static PrisonerStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Magic Number Save Format
        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return FREE;
                case 1:
                    return PRISONER;
                case 2:
                    return BONDSMAN;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into a PrisonerStatus. Returning FREE.");
        return FREE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
