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

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO : Add PoW, On Leave and AWOL implementations
 */
public enum PersonnelStatus {
    //region Enum Declarations
    ACTIVE("PersonnelStatus.ACTIVE.text", "PersonnelStatus.ACTIVE.toolTipText", "PersonnelStatus.ACTIVE.reportText", "PersonnelStatus.ACTIVE.logText"),
    MIA("PersonnelStatus.MIA.text", "PersonnelStatus.MIA.toolTipText", "PersonnelStatus.MIA.reportText", "PersonnelStatus.MIA.logText"),
    POW("PersonnelStatus.POW.text", "PersonnelStatus.POW.toolTipText", "PersonnelStatus.POW.reportText", "PersonnelStatus.POW.logText"),
    ON_LEAVE("PersonnelStatus.ON_LEAVE.text", "PersonnelStatus.ON_LEAVE.toolTipText", "PersonnelStatus.ON_LEAVE.reportText", "PersonnelStatus.ON_LEAVE.logText"),
    AWOL("PersonnelStatus.AWOL.text", "PersonnelStatus.AWOL.toolTipText", "PersonnelStatus.AWOL.reportText", "PersonnelStatus.AWOL.logText"),
    RETIRED("PersonnelStatus.RETIRED.text", "PersonnelStatus.RETIRED.toolTipText", "PersonnelStatus.RETIRED.reportText", "PersonnelStatus.RETIRED.logText"),
    DESERTED("PersonnelStatus.DESERTED.text", "PersonnelStatus.DESERTED.toolTipText", "PersonnelStatus.DESERTED.reportText", "PersonnelStatus.DESERTED.logText"),
    STUDENT("PersonnelStatus.STUDENT.text", "PersonnelStatus.STUDENT.toolTipText", "PersonnelStatus.STUDENT.reportText", "PersonnelStatus.STUDENT.logText"),
    MISSING("PersonnelStatus.MISSING.text", "PersonnelStatus.MISSING.toolTipText", "PersonnelStatus.MISSING.reportText", "PersonnelStatus.MISSING.logText"),
    KIA("PersonnelStatus.KIA.text", "PersonnelStatus.KIA.toolTipText", "PersonnelStatus.KIA.reportText", "PersonnelStatus.KIA.logText"),
    HOMICIDE("PersonnelStatus.HOMICIDE.text", "PersonnelStatus.HOMICIDE.toolTipText", "PersonnelStatus.HOMICIDE.reportText", "PersonnelStatus.HOMICIDE.logText"),
    WOUNDS("PersonnelStatus.WOUNDS.text", "PersonnelStatus.WOUNDS.toolTipText", "PersonnelStatus.WOUNDS.reportText", "PersonnelStatus.WOUNDS.logText"),
    DISEASE("PersonnelStatus.DISEASE.text", "PersonnelStatus.DISEASE.toolTipText", "PersonnelStatus.DISEASE.reportText", "PersonnelStatus.DISEASE.logText"),
    ACCIDENTAL("PersonnelStatus.ACCIDENTAL.text", "PersonnelStatus.ACCIDENTAL.toolTipText", "PersonnelStatus.ACCIDENTAL.reportText", "PersonnelStatus.ACCIDENTAL.logText"),
    NATURAL_CAUSES("PersonnelStatus.NATURAL_CAUSES.text", "PersonnelStatus.NATURAL_CAUSES.toolTipText", "PersonnelStatus.NATURAL_CAUSES.reportText", "PersonnelStatus.NATURAL_CAUSES.logText"),
    OLD_AGE("PersonnelStatus.OLD_AGE.text", "PersonnelStatus.OLD_AGE.toolTipText", "PersonnelStatus.OLD_AGE.reportText", "PersonnelStatus.OLD_AGE.logText"),
    MEDICAL_COMPLICATIONS("PersonnelStatus.MEDICAL_COMPLICATIONS.text", "PersonnelStatus.MEDICAL_COMPLICATIONS.toolTipText", "PersonnelStatus.MEDICAL_COMPLICATIONS.reportText", "PersonnelStatus.MEDICAL_COMPLICATIONS.logText"),
    PREGNANCY_COMPLICATIONS("PersonnelStatus.PREGNANCY_COMPLICATIONS.text", "PersonnelStatus.PREGNANCY_COMPLICATIONS.toolTipText", "PersonnelStatus.PREGNANCY_COMPLICATIONS.reportText", "PersonnelStatus.PREGNANCY_COMPLICATIONS.logText"),
    UNDETERMINED("PersonnelStatus.UNDETERMINED.text", "PersonnelStatus.UNDETERMINED.toolTipText", "PersonnelStatus.UNDETERMINED.reportText", "PersonnelStatus.UNDETERMINED.logText"),
    SUICIDE("PersonnelStatus.SUICIDE.text", "PersonnelStatus.SUICIDE.toolTipText", "PersonnelStatus.SUICIDE.reportText", "PersonnelStatus.SUICIDE.logText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String reportText;
    private final String logText;
    //endregion Variable Declarations

    //region Constructors
    PersonnelStatus(final String name, final String toolTipText, final String reportText,
                    final String logText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.reportText = resources.getString(reportText);
        this.logText = resources.getString(logText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getReportText() {
        return reportText;
    }

    public String getLogText() {
        return logText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isMIA() {
        return this == MIA;
    }

    public boolean isPoW() {
        return this == POW;
    }

    public boolean isOnLeave() {
        return this == ON_LEAVE;
    }

    public boolean isAWOL() {
        return this == AWOL;
    }

    public boolean isRetired() {
        return this == RETIRED;
    }

    public boolean isDeserted() {
        return this == DESERTED;
    }

    public boolean isStudent() {
        return this == STUDENT;
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    public boolean isKIA() {
        return this == KIA;
    }

    public boolean isHomicide() {
        return this == HOMICIDE;
    }

    public boolean isWounds() {
        return this == WOUNDS;
    }

    public boolean isDisease() {
        return this == DISEASE;
    }

    public boolean isAccidental() {
        return this == ACCIDENTAL;
    }

    public boolean isNaturalCauses() {
        return this == NATURAL_CAUSES;
    }

    public boolean isOldAge() {
        return this == OLD_AGE;
    }

    public boolean isMedicalComplications() {
        return this == MEDICAL_COMPLICATIONS;
    }

    public boolean isPregnancyComplications() {
        return this == PREGNANCY_COMPLICATIONS;
    }

    public boolean isUndetermined() {
        return this == UNDETERMINED;
    }

    public boolean isSuicide() {
        return this == SUICIDE;
    }

    /**
     * @return true if a person is currently absent from the core force, otherwise false
     */
    public boolean isAbsent() {
        return isMIA() || isPoW() || isOnLeave() || isAWOL() || isStudent() || isMissing();
    }

    /**
     * @return true if a person is dead, otherwise false
     */
    public boolean isDead() {
        return isKIA() || isHomicide() || isWounds() || isDisease() || isAccidental()
                || isNaturalCauses() || isOldAge() || isMedicalComplications()
                || isPregnancyComplications() || isUndetermined() || isSuicide();
    }

    /**
     * @return true if a person is dead or MIA, otherwise false
     */
    public boolean isDeadOrMIA() {
        return isDead() || isMIA();
    }
    //endregion Boolean Comparison Methods

    public static List<PersonnelStatus> getImplementedStatuses() {
        return Stream.of(values())
                .filter(personnelStatus -> !personnelStatus.isPoW() && !personnelStatus.isOnLeave()
                        && !personnelStatus.isAWOL())
                .collect(Collectors.toList());
    }

    //region File I/O
    /**
     * @param text containing the PersonnelStatus
     * @return the saved PersonnelStatus
     */
    public static PersonnelStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ACTIVE;
                case 1:
                    return RETIRED;
                case 2:
                    return KIA;
                case 3:
                    return MIA;
                case 4:
                    return STUDENT;
                case 5:
                    return MISSING;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into a PersonnelStatus. Returning ACTIVE.");
        return ACTIVE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
