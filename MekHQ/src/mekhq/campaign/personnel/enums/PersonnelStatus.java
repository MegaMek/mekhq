/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum class representing personnel status.
 */
public enum PersonnelStatus {
    // region Enum Declarations
    ACTIVE("PersonnelStatus.ACTIVE.text", "PersonnelStatus.ACTIVE.toolTipText",
        "PersonnelStatus.ACTIVE.reportText", "PersonnelStatus.ACTIVE.logText"),
    MIA("PersonnelStatus.MIA.text", "PersonnelStatus.MIA.toolTipText",
        "PersonnelStatus.MIA.reportText", "PersonnelStatus.MIA.logText"),
    POW("PersonnelStatus.POW.text", "PersonnelStatus.POW.toolTipText",
        "PersonnelStatus.POW.reportText", "PersonnelStatus.POW.logText"),
    ON_LEAVE("PersonnelStatus.ON_LEAVE.text", "PersonnelStatus.ON_LEAVE.toolTipText",
            "PersonnelStatus.ON_LEAVE.reportText", "PersonnelStatus.ON_LEAVE.logText"),
    ON_MATERNITY_LEAVE("PersonnelStatus.ON_MATERNITY_LEAVE.text", "PersonnelStatus.ON_MATERNITY_LEAVE.toolTipText",
            "PersonnelStatus.ON_MATERNITY_LEAVE.reportText", "PersonnelStatus.ON_MATERNITY_LEAVE.logText"),
    AWOL("PersonnelStatus.AWOL.text", "PersonnelStatus.AWOL.toolTipText",
        "PersonnelStatus.AWOL.reportText", "PersonnelStatus.AWOL.logText"),
    RETIRED("PersonnelStatus.RETIRED.text", "PersonnelStatus.RETIRED.toolTipText",
        "PersonnelStatus.RETIRED.reportText", "PersonnelStatus.RETIRED.logText"),
    RESIGNED("PersonnelStatus.RESIGNED.text", "PersonnelStatus.RESIGNED.toolTipText",
            "PersonnelStatus.RESIGNED.reportText", "PersonnelStatus.RESIGNED.logText"),
    SACKED("PersonnelStatus.SACKED.text", "PersonnelStatus.SACKED.toolTipText",
        "PersonnelStatus.SACKED.reportText", "PersonnelStatus.SACKED.logText"),
    LEFT("PersonnelStatus.LEFT.text", "PersonnelStatus.LEFT.toolTipText",
        "PersonnelStatus.LEFT.reportText", "PersonnelStatus.LEFT.logText"),
    DESERTED("PersonnelStatus.DESERTED.text", "PersonnelStatus.DESERTED.toolTipText",
            "PersonnelStatus.DESERTED.reportText", "PersonnelStatus.DESERTED.logText"),
    DEFECTED("PersonnelStatus.DEFECTED.text", "PersonnelStatus.DEFECTED.toolTipText",
            "PersonnelStatus.DEFECTED.reportText", "PersonnelStatus.DEFECTED.logText"),
    STUDENT("PersonnelStatus.STUDENT.text", "PersonnelStatus.STUDENT.toolTipText",
        "PersonnelStatus.STUDENT.reportText", "PersonnelStatus.STUDENT.logText"),
    MISSING("PersonnelStatus.MISSING.text", "PersonnelStatus.MISSING.toolTipText",
        "PersonnelStatus.MISSING.reportText", "PersonnelStatus.MISSING.logText"),
    KIA("PersonnelStatus.KIA.text", "PersonnelStatus.KIA.toolTipText",
        "PersonnelStatus.KIA.reportText", "PersonnelStatus.KIA.logText"),
    HOMICIDE("PersonnelStatus.HOMICIDE.text", "PersonnelStatus.HOMICIDE.toolTipText",
            "PersonnelStatus.HOMICIDE.reportText", "PersonnelStatus.HOMICIDE.logText"),
    WOUNDS("PersonnelStatus.WOUNDS.text", "PersonnelStatus.WOUNDS.toolTipText",
        "PersonnelStatus.WOUNDS.reportText", "PersonnelStatus.WOUNDS.logText"),
    DISEASE("PersonnelStatus.DISEASE.text", "PersonnelStatus.DISEASE.toolTipText",
        "PersonnelStatus.DISEASE.reportText", "PersonnelStatus.DISEASE.logText"),
    ACCIDENTAL("PersonnelStatus.ACCIDENTAL.text", "PersonnelStatus.ACCIDENTAL.toolTipText",
            "PersonnelStatus.ACCIDENTAL.reportText", "PersonnelStatus.ACCIDENTAL.logText"),
    NATURAL_CAUSES("PersonnelStatus.NATURAL_CAUSES.text", "PersonnelStatus.NATURAL_CAUSES.toolTipText",
            "PersonnelStatus.NATURAL_CAUSES.reportText", "PersonnelStatus.NATURAL_CAUSES.logText"),
    OLD_AGE("PersonnelStatus.OLD_AGE.text", "PersonnelStatus.OLD_AGE.toolTipText",
        "PersonnelStatus.OLD_AGE.reportText", "PersonnelStatus.OLD_AGE.logText"),
    MEDICAL_COMPLICATIONS("PersonnelStatus.MEDICAL_COMPLICATIONS.text", "PersonnelStatus.MEDICAL_COMPLICATIONS.toolTipText",
        "PersonnelStatus.MEDICAL_COMPLICATIONS.reportText", "PersonnelStatus.MEDICAL_COMPLICATIONS.logText"),
    PREGNANCY_COMPLICATIONS("PersonnelStatus.PREGNANCY_COMPLICATIONS.text", "PersonnelStatus.PREGNANCY_COMPLICATIONS.toolTipText",
        "PersonnelStatus.PREGNANCY_COMPLICATIONS.reportText", "PersonnelStatus.PREGNANCY_COMPLICATIONS.logText"),
    UNDETERMINED("PersonnelStatus.UNDETERMINED.text", "PersonnelStatus.UNDETERMINED.toolTipText",
            "PersonnelStatus.UNDETERMINED.reportText", "PersonnelStatus.UNDETERMINED.logText"),
    SUICIDE("PersonnelStatus.SUICIDE.text", "PersonnelStatus.SUICIDE.toolTipText",
        "PersonnelStatus.SUICIDE.reportText", "PersonnelStatus.SUICIDE.logText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String reportText;
    private final String logText;
    // endregion Variable Declarations

    // region Constructors
    /**
     * Initializes a new instance of the {@link PersonnelStatus} class with the given parameters.
     *
     * @param name        the name of the personnel status
     * @param toolTipText the tooltip text for the personnel status
     * @param reportText  the report text for the personnel status
     * @param logText     the log text for the personnel status
     */
    PersonnelStatus(final String name, final String toolTipText, final String reportText,
            final String logText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.reportText = resources.getString(reportText);
        this.logText = resources.getString(logText);
    }
    // endregion Constructors

    /**
     * Retrieves the tooltip text for a given component.
     *
     * @return The tooltip text of the component, or null if no tooltip text is set.
     */
    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    /**
     * Retrieves the report text.
     *
     * @return The report text as a string.
     */
    public String getReportText() {
        return reportText;
    }

    /**
     * Retrieves the log text.
     *
     * @return The log text.
     */
    public String getLogText() {
        return logText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    /**
     * Checks if the character has the {@code ACTIVE} personnel status.
     *
     * @return {@code true} if the character has the {@code ACTIVE} personnel status {@code false}
     * otherwise.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if the character has the {@code MIA} personnel status.
     *
     * @return {@code true} if the character has the {@code MIA} personnel status {@code false}
     * otherwise.
     */
    public boolean isMIA() {
        return this == MIA;
    }

    /**
     * Checks if the character has the {@code POW} personnel status.
     *
     * @return {@code true} if the character has the {@code POW} personnel status {@code false}
     * otherwise.
     */
    public boolean isPoW() {
        return this == POW;
    }

    /**
     * Checks if the character has the {@code ON_LEAVE} personnel status.
     *
     * @return {@code true} if the character has the {@code ON_LEAVE} personnel status {@code false}
     * otherwise.
     */
    public boolean isOnLeave() {
        return this == ON_LEAVE;
    }

    /**
     * Checks if the character has the {@code ON_MATERNITY_LEAVE} personnel status.
     *
     * @return {@code true} if the character has the {@code ON_MATERNITY_LEAVE} personnel status
     * {@code false} otherwise.
     */
    public boolean isOnMaternityLeave() {
        return this == ON_MATERNITY_LEAVE;
    }

    /**
     * Checks if the character has the {@code AWOL} personnel status.
     *
     * @return {@code true} if the character has the {@code AWOL} personnel status {@code false}
     * otherwise.
     */
    public boolean isAwol() {
        return this == AWOL;
    }

    /**
     * Checks if the character has the {@code RETIRED} personnel status.
     *
     * @return {@code true} if the character has the {@code RETIRED} personnel status {@code false}
     * otherwise.
     */
    public boolean isRetired() {
        return this == RETIRED;
    }

    /**
     * Checks if the character has the {@code RESIGNED} personnel status.
     *
     * @return {@code true} if the character has the {@code RESIGNED} personnel status {@code false}
     * otherwise.
     */
    public boolean isResigned() {
        return this == RESIGNED;
    }

    /**
     * Checks if the character has the {@code SACKED} personnel status.
     *
     * @return {@code true} if the character has the {@code SACKED} personnel status {@code false}
     * otherwise.
     */
    public boolean isSacked() {
        return this == SACKED;
    }

    /**
     * Checks if the character has the {@code LEFT} personnel status.
     *
     * @return {@code true} if the character has the {@code LEFT} personnel status {@code false}
     * otherwise.
     */
    public boolean isLeft() {
        return this == LEFT;
    }

    /**
     * Checks if the character has the {@code DESERTED} personnel status.
     *
     * @return {@code true} if the character has the {@code DESERTED} personnel status {@code false}
     * otherwise.
     */
    public boolean isDeserted() {
        return this == DESERTED;
    }

    /**
     * Checks if the character has the {@code DEFECTED} personnel status.
     *
     * @return {@code true} if the character has the {@code DEFECTED} personnel status {@code false}
     * otherwise.
     */
    public boolean isDefected() {
        return this == DEFECTED;
    }

    /**
     * Checks if the character has the {@code STUDENT} personnel status.
     *
     * @return {@code true} if the character has the {@code STUDENT} personnel status {@code false}
     * otherwise.
     */
    public boolean isStudent() {
        return this == STUDENT;
    }

    /**
     * Checks if the character has the {@code MISSING} personnel status.
     *
     * @return {@code true} if the character has the {@code MISSING} personnel status {@code false}
     * otherwise.
     */
    public boolean isMissing() {
        return this == MISSING;
    }

    /**
     * Checks if the character has the {@code KIA} personnel status.
     *
     * @return {@code true} if the character has the {@code KIA} personnel status {@code false}
     * otherwise.
     */
    public boolean isKIA() {
        return this == KIA;
    }

    /**
     * Checks if the character has the {@code HOMICIDE} personnel status.
     *
     * @return {@code true} if the character has the {@code HOMICIDE} personnel status {@code false}
     * otherwise.
     */
    public boolean isHomicide() {
        return this == HOMICIDE;
    }

    /**
     * Checks if the character has the {@code WOUNDS} personnel status.
     *
     * @return {@code true} if the character has the {@code WOUNDS} personnel status {@code false}
     * otherwise.
     */
    public boolean isWounds() {
        return this == WOUNDS;
    }

    /**
     * Checks if the character has the {@code DISEASE} personnel status.
     *
     * @return {@code true} if the character has the {@code DISEASE} personnel status {@code false}
     * otherwise.
     */
    public boolean isDisease() {
        return this == DISEASE;
    }

    /**
     * Checks if the character has the {@code ACCIDENTAL} personnel status.
     *
     * @return {@code true} if the character has the {@code ACCIDENTAL} personnel status {@code false}
     * otherwise.
     */
    public boolean isAccidental() {
        return this == ACCIDENTAL;
    }

    /**
     * Checks if the character has the {@code NATURAL_CAUSES} personnel status.
     *
     * @return {@code true} if the character has the {@code NATURAL_CAUSES} personnel status {@code false}
     * otherwise.
     */
    public boolean isNaturalCauses() {
        return this == NATURAL_CAUSES;
    }

    /**
     * Checks if the character has the {@code OLD_AGE} personnel status.
     *
     * @return {@code true} if the character has the {@code OLD_AGE} personnel status {@code false}
     * otherwise.
     */
    public boolean isOldAge() {
        return this == OLD_AGE;
    }

    /**
     * Checks if the character has the {@code MEDICAL_COMPLICATIONS} personnel status.
     *
     * @return {@code true} if the character has the {@code MEDICAL_COMPLICATIONS} personnel status
     * {@code false} otherwise.
     */
    public boolean isMedicalComplications() {
        return this == MEDICAL_COMPLICATIONS;
    }

    /**
     * Checks if the character has the {@code PREGNANCY_COMPLICATIONS} personnel status.
     *
     * @return {@code true} if the character has the {@code PREGNANCY_COMPLICATIONS} personnel status
     * {@code false} otherwise.
     */
    public boolean isPregnancyComplications() {
        return this == PREGNANCY_COMPLICATIONS;
    }

    /**
     * Checks if the character has the {@code UNDETERMINED} personnel status.
     *
     * @return {@code true} if the character has the {@code UNDETERMINED} personnel status {@code false}
     * otherwise.
     */
    public boolean isUndetermined() {
        return this == UNDETERMINED;
    }

    /**
     * Checks if the character has the {@code SUICIDE} personnel status.
     *
     * @return {@code true} if the character has the {@code SUICIDE} personnel status {@code false}
     * otherwise.
     */
    public boolean isSuicide() {
        return this == SUICIDE;
    }

    /**
     * @return {@code true} if a person is currently absent from the core force, otherwise
     *         {@code false}
     */
    public boolean isAbsent() {
        return isMIA() || isPoW() || isOnLeave() || isOnMaternityLeave() || isAwol() || isStudent() || isMissing();
    }

    /**
     * @return {@code true} if a person has left the unit, otherwise {@code false}
     */
    public boolean isDepartedUnit() {
        return isDead() || isRetired() || isResigned() || isSacked() || isDeserted() || isDefected() || isMissing()
                || isLeft();
    }

    /**
     * @return {@code true} if a person is dead, otherwise {@code false}
     */
    public boolean isDead() {
        return isKIA() || isHomicide() || isWounds() || isDisease() || isAccidental()
                || isNaturalCauses() || isOldAge() || isMedicalComplications()
                || isPregnancyComplications() || isUndetermined() || isSuicide();
    }

    /**
     * @return {@code true} if a person is dead or MIA, otherwise {@code false}
     */
    public boolean isDeadOrMIA() {
        return isDead() || isMIA();
    }
    // endregion Boolean Comparison Methods

    /**
     * @return The list of implemented personnel statuses.
     */
    public static List<PersonnelStatus> getImplementedStatuses() {
        return Stream.of(values())
                .collect(Collectors.toList());
    }

    // region File I/O
    /**
     * Parses a string representation of {@link PersonnelStatus} into a {@link PersonnelStatus} object.
     * If the string representation cannot be parsed, it returns the default {@code PersonnelStatus.ACTIVE}.
     *
     * @param text The {@link String} representation of {@link PersonnelStatus}
     * @return The parsed {@link PersonnelStatus} object or {@code PersonnelStatus.ACTIVE} if parsing fails
     */
    public static PersonnelStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {}

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
                case 6:
                    return POW;
                case 7:
                    return ON_LEAVE;
                case 8:
                    return AWOL;
                case 9:
                    return RESIGNED;
                case 10:
                    return DESERTED;
                case 11:
                    return DEFECTED;
                case 12:
                    return HOMICIDE;
                case 13:
                    return WOUNDS;
                case 14:
                    return DISEASE;
                case 15:
                    return ACCIDENTAL;
                case 16:
                    return NATURAL_CAUSES;
                case 17:
                    return OLD_AGE;
                case 18:
                    return MEDICAL_COMPLICATIONS;
                case 19:
                    return PREGNANCY_COMPLICATIONS;
                case 20:
                    return UNDETERMINED;
                case 21:
                    return SUICIDE;
                case 22:
                    return SACKED;
                case 23:
                    return ON_MATERNITY_LEAVE;
                default:
                    break;
            }
        } catch (Exception ignored) {}

        String message = String.format("Unable to parse %s into a PersonnelStatus. Returning ACTIVE.",
                text);

        MMLogger.create(PersonnelStatus.class).error(message);
        return ACTIVE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
