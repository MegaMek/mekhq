/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum PrisonerStatus {
    //region Enum Declarations
    /**
     * This is used for personnel who are not (currently) prisoners
     */
    FREE("PrisonerStatus.FREE.text"),
    /**
     * This is used to track standard personnel who are prisoners and not willing to defect
     */
    PRISONER("PrisonerStatus.PRISONER.text"),
    /**
     * This is used to track standard personnel who are prisoners and are willing to defect
     */
    PRISONER_DEFECTOR("PrisonerStatus.PRISONER_DEFECTOR.text"),
    /**
     * This is used to track clan personnel who become Bondsmen when captured
     */
    BONDSMAN("PrisonerStatus.BONDSMAN.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String typeName;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PrisonerStatus(String typeName) {
        this.typeName = resources.getString(typeName);
    }
    //endregion Constructors

    public String getTypeName() {
        return typeName;
    }

    //region Boolean Comparisons
    public boolean isFree() {
        return this == FREE;
    }

    public boolean isPrisoner() {
        return (this == PRISONER) || (this == PRISONER_DEFECTOR);
    }

    public boolean isWillingToDefect() {
        return this == PRISONER_DEFECTOR;
    }

    public boolean isBondsman() {
        return this == BONDSMAN;
    }
    //endregion Boolean Comparisons

    @Override
    public String toString() {
        return getTypeName();
    }

    /**
     * @param text The saved value to parse, either the older magic number save format or the
     *             PrisonerStatus.name() value
     * @return the Prisoner Status in question
     */
    public static PrisonerStatus parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Magic Number Save Format
        try {
            switch (Integer.parseInt(text)) {
                case 2:
                    return BONDSMAN;
                case 1:
                    return PRISONER;
                case 0:
                default:
                    return FREE;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error(PrisonerStatus.class, "parseFromString",
                    "Unable to parse " + text + " into a PrisonerStatus. Returning FREE.");

        return FREE;
    }
}
