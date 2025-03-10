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
 */
package mekhq.campaign.personnel.enums;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * Represents the various statuses that a {@link Person} can have within MekHQ.
 *
 * <p>This class is implemented as an {@code enum}, providing a predefined set of constants
 * representing specific personnel statuses (e.g., ACTIVE, MIA, KIA, SUICIDE, etc.). Each status
 * is associated with a {@link NotificationSeverity} that indicates the severity level or category
 * of the condition, such as {@code POSITIVE}, {@code WARNING}, or {@code NEGATIVE}. This helps
 * color-code messages or reports for better visual communication.</p>
 *
 * <p>Additional features include methods to retrieve localized labels, tooltips, report texts,
 * and log texts for each status. These utilize resource bundles for localization, enabling
 * multi-language support. There are also utility methods for checking whether a status belongs
 * to certain predefined categories like "absent," "dead," or "departed from the unit."</p>
 */
public enum PersonnelStatus {
    // region Enum Declarations
    ACTIVE(NotificationSeverity.WARNING),
    MIA(NotificationSeverity.WARNING),
    POW(NotificationSeverity.WARNING),
    ON_LEAVE(NotificationSeverity.WARNING),
    ON_MATERNITY_LEAVE(NotificationSeverity.WARNING),
    AWOL(NotificationSeverity.WARNING),
    RETIRED(NotificationSeverity.NEGATIVE),
    RESIGNED(NotificationSeverity.NEGATIVE),
    SACKED(NotificationSeverity.WARNING),
    LEFT(NotificationSeverity.NEGATIVE),
    DESERTED(NotificationSeverity.NEGATIVE),
    DEFECTED(NotificationSeverity.NEGATIVE),
    STUDENT(NotificationSeverity.POSITIVE),
    MISSING(NotificationSeverity.NEGATIVE),
    KIA(NotificationSeverity.NEGATIVE),
    HOMICIDE(NotificationSeverity.NEGATIVE),
    WOUNDS(NotificationSeverity.NEGATIVE),
    DISEASE(NotificationSeverity.NEGATIVE),
    ACCIDENTAL(NotificationSeverity.NEGATIVE),
    NATURAL_CAUSES(NotificationSeverity.NEGATIVE),
    OLD_AGE(NotificationSeverity.NEGATIVE),
    MEDICAL_COMPLICATIONS(NotificationSeverity.NEGATIVE),
    PREGNANCY_COMPLICATIONS(NotificationSeverity.NEGATIVE),
    UNDETERMINED(NotificationSeverity.NEGATIVE),
    SUICIDE(NotificationSeverity.NEGATIVE),
    ENEMY_BONDSMAN(NotificationSeverity.NEGATIVE),
    BONDSREF(NotificationSeverity.NEGATIVE);

    /**
     * Represents the severity levels of a status.
     */
    public enum NotificationSeverity {
        /**
         * Indicates a critical or negative status.
         *
         * <p>Defaults to {@code RED}</p>
         */
        NEGATIVE,

        /**
         * Indicates a cautionary status that requires attention.
         *
         * <p>Defaults to {@code YELLOW}</p>
         */
        WARNING,

        /**
         * Indicates no severity or a neutral status.
         *
         * <p>Defaults to {@code Theme Default Text Color}</p>
         */
        NEUTRAL,

        /**
         * Indicates a positive status.
         *
         * <p>Defaults to {@code GREEN}</p>
         */
        POSITIVE
    }
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    // region Variable Declarations
    private final NotificationSeverity severity;
    // endregion Variable Declarations

    // region Constructors
    /**
     * Initializes a new instance of the {@link PersonnelStatus} class with the specified severity level.
     *
     * @param severity the severity level of the personnel status, represented by {@link NotificationSeverity}
     */
    PersonnelStatus(final NotificationSeverity severity) {
        this.severity = severity;
    }
    // endregion Constructors
    /**
     * Retrieves the severity level of this status.
     *
     * @return the severity level, represented by {@link NotificationSeverity}
     */
    public NotificationSeverity getSeverity() {
        return severity;
    }

    /**
     * Retrieves the label text associated with this status.
     *
     * <p>The label text is retrieved from the resource bundle using the key
     * formed by appending <code>".label"</code> to the name of this status.</p>
     *
     * @return the localized label text
     */
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the tooltip text associated with this status.
     *
     * <p>The tooltip text is retrieved from the resource bundle using the key
     * formed by appending <code>".tooltip"</code> to the name of this status.</p>
     *
     * @return the localized tooltip text
     */
    public String getToolTipText() {
        final String RESOURCE_KEY = name() + ".tooltip";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the report text associated with this status.
     *
     * <p>The report text is retrieved from the resource bundle using the key
     * formed by appending <code>".report"</code> to the name of this status.</p>
     *
     * @return the localized report text
     */
    public String getReportText() {
        final String RESOURCE_KEY = name() + ".report";

        String OPENING_SPAN_TEXT = switch (severity) {
            case NEGATIVE -> spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());
            case WARNING -> spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());
            case NEUTRAL -> "";
            case POSITIVE -> spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());
        };

        String CLOSING_SPAN_TEXT = OPENING_SPAN_TEXT.isBlank() ? "" : CLOSING_SPAN_TAG;

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, OPENING_SPAN_TEXT, CLOSING_SPAN_TEXT);
    }

    /**
     * Retrieves the log text associated with this status.
     *
     * <p>The log text is retrieved from the resource bundle using the key
     * formed by appending <code>".log"</code> to the name of this status.</p>
     *
     * @return the localized log text
     */
    public String getLogText() {
        final String RESOURCE_KEY = name() + ".log";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
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
     * Checks if the character has the {@code ENEMY_BONDSMAN} personnel status.
     *
     * @return {@code true} if the character has the {@code ENEMY_BONDS} personnel status {@code false}
     * otherwise.
     */
    public boolean isEnemyBondsman() {
        return this == ENEMY_BONDSMAN;
    }

    /**
     * Checks if the character has the {@code BONDSREF} personnel status.
     *
     * @return {@code true} if the character has the {@code BONDSREF} personnel status {@code false}
     * otherwise.
     */
    public boolean isBondsref() {
        return this == BONDSREF;
    }

    /**
     * @return {@code true} if a person is currently absent from the core force, otherwise
     *         {@code false}
     */
    public boolean isAbsent() {
        return isMIA() || isPoW() || isEnemyBondsman() || isOnLeave() || isOnMaternityLeave()
            || isAwol() || isStudent() || isMissing();
    }

    /**
     * @return {@code true} if a person has left the unit, otherwise {@code false}
     */
    public boolean isDepartedUnit() {
        return isDead() || isRetired() || isResigned() || isSacked() || isDeserted() || isDefected()
            || isMissing() || isLeft() || isEnemyBondsman();
    }

    /**
     * @return {@code true} if a person is dead, otherwise {@code false}
     */
    public boolean isDead() {
        return isKIA() || isHomicide() || isWounds() || isDisease() || isAccidental()
                || isNaturalCauses() || isOldAge() || isMedicalComplications()
                || isPregnancyComplications() || isUndetermined() || isSuicide() || isBondsref();
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
     * Converts a given string to its corresponding {@code PersonnelStatus} enumeration value. The
     * method first attempts to parse the string as the name of an {@code PersonnelStatus} enum value.
     * If that fails, it attempts to parse the string as an integer representing the ordinal of an
     * {@code PersonnelStatus} enum value. If neither succeeds, it logs an error and defaults to
     * returning {@code ACTIVE}.
     *
     * @param text the input string to parse, which represents either the name or the ordinal
     *             of an {@code PersonnelStatus} enum value.
     * @return the corresponding {@code PersonnelStatus} enum instance for the given input string,
     *         or {@code ACTIVE} if no valid match is found.
     */
    public static PersonnelStatus fromString(String text) {
        try {
            return PersonnelStatus.valueOf(text);
        } catch (Exception ignored) {}

        try {
            return PersonnelStatus.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        MMLogger logger = MMLogger.create(PersonnelStatus.class);
        logger.error("Unknown PersonnelStatus ordinal: {} - returning {}.", text, ACTIVE);

        return ACTIVE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getLabel();
    }
}
