/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;

public enum PersonnelStatus {
    //region Enum Declarations
    ACTIVE("PersonnelStatus.ACTIVE.text"),
    RETIRED("PersonnelStatus.RETIRED.text"),
    MIA("PersonnelStatus.MIA.text"),
    KIA("PersonnelStatus.KIA.text"),
    NATURAL_CAUSES("PersonnelStatus.NATURAL_CAUSES.text"),
    WOUNDS("PersonnelStatus.WOUNDS.text"),
    DISEASE("PersonnelStatus.DISEASE.text"),
    OLD_AGE("PersonnelStatus.OLD_AGE.text"),
    PREGNANCY_COMPLICATIONS("PersonnelStatus.PREGNANCY_COMPLICATIONS.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String statusName;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PersonnelStatus(String status) {
        this.statusName = resources.getString(status);
    }
    //endregion Constructors

    @Override
    public String toString() {
        return statusName;
    }

    //region Boolean Information Methods
    /**
     * @return true if a person is active, otherwise false
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    /**
     * @return true if a person is retired, otherwise false
     */
    public boolean isRetired() {
        return this == RETIRED;
    }

    /**
     * @return true if a person is MIA, otherwise false
     */
    public boolean isMIA() {
        return this == MIA;
    }

    /**
     * @return true if a person is KIA, otherwise false
     */
    public boolean isKIA() {
        return this == KIA;
    }

    /**
     * @return true if a person has died of NATURAL_CAUSES, otherwise false
     */
    public boolean hasDiedOfNaturalCauses() {
        return this == NATURAL_CAUSES;
    }

    /**
     * @return true if a person has died from WOUNDS, otherwise false
     */
    public boolean hasDiedOfWounds() {
        return this == WOUNDS;
    }

    /**
     * @return true if a person has died from DISEASE, otherwise false
     */
    public boolean hasDiedOfDisease() {
        return this == DISEASE;
    }

    /**
     * @return true if a person has died from OLD_AGE, otherwise false
     */
    public boolean hasDiedOfOldAge() {
        return this == OLD_AGE;
    }

    /**
     * @return true if a person has died from PREGNANCY_COMPLICATIONS, otherwise false
     */
    public boolean hasDiedOfPregnancyComplications() {
        return this == PREGNANCY_COMPLICATIONS;
    }

    /**
     * @return true if a person is dead, otherwise false
     */
    public boolean isDead() {
        return isKIA() || hasDiedOfNaturalCauses() || hasDiedOfWounds() || hasDiedOfDisease()
                || hasDiedOfOldAge() || hasDiedOfPregnancyComplications();
    }

    /**
     * @return true if a person is dead or MIA, otherwise false
     */
    public boolean isDeadOrMIA() {
        return isDead() || isMIA();
    }
    //endregion Boolean Information Methods

    /**
     * @param text containing the PersonnelStatus
     * @return the saved PersonnelStatus
     */
    public static PersonnelStatus parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return RETIRED;
                case 2:
                    return KIA;
                case 3:
                    return MIA;
                default:
                    return ACTIVE;
            }
        } catch (Exception ignored) {

        }

        return ACTIVE;
    }
}
