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
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.ReportingUtilities;

/**
 * Represents the various statuses that a {@link Person} can have within MekHQ.
 *
 * <p>This class is implemented as an {@code enum}, providing a predefined set of constants
 * representing specific personnel statuses (e.g., ACTIVE, MIA, KIA, SUICIDE, etc.). Each status is associated with a
 * {@link NotificationSeverity} that indicates the severity level or category of the condition, such as
 * {@code POSITIVE}, {@code WARNING}, or {@code NEGATIVE}. This helps color-code messages or reports for better visual
 * communication.</p>
 *
 * <p>Additional features include methods to retrieve localized labels, tooltips, report texts,
 * and log texts for each status. These utilize resource bundles for localization, enabling multi-language support.
 * There are also utility methods for checking whether a status belongs to certain predefined categories like "absent,"
 * "dead," or "departed from the unit."</p>
 */
public enum PersonnelStatus {
    // region Enum Declarations
    ACTIVE(NotificationSeverity.WARNING, true, false),
    MIA(NotificationSeverity.WARNING, false, false),
    POW(NotificationSeverity.WARNING, false, false),
    ON_LEAVE(NotificationSeverity.WARNING, false, false),
    ON_MATERNITY_LEAVE(NotificationSeverity.WARNING, false, false),
    AWOL(NotificationSeverity.WARNING, false, false),
    RETIRED(NotificationSeverity.NEGATIVE, false, false),
    RESIGNED(NotificationSeverity.NEGATIVE, false, false),
    SACKED(NotificationSeverity.WARNING, false, false),
    LEFT(NotificationSeverity.WARNING, true, false),
    DESERTED(NotificationSeverity.NEGATIVE, false, false),
    DEFECTED(NotificationSeverity.NEGATIVE, false, false),
    STUDENT(NotificationSeverity.POSITIVE, false, false),
    MISSING(NotificationSeverity.NEGATIVE, true, false),
    KIA(NotificationSeverity.NEGATIVE, true, true),
    HOMICIDE(NotificationSeverity.NEGATIVE, true, true),
    WOUNDS(NotificationSeverity.NEGATIVE, true, true),
    DISEASE(NotificationSeverity.NEGATIVE, true, true),
    ACCIDENTAL(NotificationSeverity.NEGATIVE, true, true),
    NATURAL_CAUSES(NotificationSeverity.NEGATIVE, true, true),
    OLD_AGE(NotificationSeverity.NEGATIVE, true, true),
    MEDICAL_COMPLICATIONS(NotificationSeverity.NEGATIVE, true, true),
    PREGNANCY_COMPLICATIONS(NotificationSeverity.NEGATIVE, true, true),
    UNDETERMINED(NotificationSeverity.NEGATIVE, true, true),
    SUICIDE(NotificationSeverity.NEGATIVE, true, true),
    ENEMY_BONDSMAN(NotificationSeverity.NEGATIVE, false, false),
    BONDSREF(NotificationSeverity.NEGATIVE, true, true),
    SEPPUKU(NotificationSeverity.NEGATIVE, true, true),
    BACKGROUND_CHARACTER(NotificationSeverity.WARNING, false, false),
    IMPRISONED(NotificationSeverity.NEGATIVE, false, false),
    DISHONORABLY_DISCHARGED(NotificationSeverity.NEGATIVE, false, false),
    CAMP_FOLLOWER(NotificationSeverity.WARNING, true, false);

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
    private final boolean isPrisonerSuitableStatus;
    private final boolean isCauseOfDeath;
    // endregion Variable Declarations

    // region Constructors

    /**
     * Initializes a new instance of the {@link PersonnelStatus} class with the specified severity level and suitability
     * status for prisoners.
     *
     * @param severity                 the severity level of the personnel status, represented by
     *                                 {@link NotificationSeverity}. This defines the importance or criticality of the
     *                                 personnel status.
     * @param isPrisonerSuitableStatus a boolean flag indicating whether the personnel status is suitable for
     *                                 prisoners.
     */
    PersonnelStatus(final NotificationSeverity severity, final boolean isPrisonerSuitableStatus,
          final boolean isCauseOfDeath) {
        this.severity = severity;
        this.isPrisonerSuitableStatus = isPrisonerSuitableStatus;
        this.isCauseOfDeath = isCauseOfDeath;
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
     * Checks whether the personnel status is suitable for prisoners.
     *
     * @return {@code true} if the personnel status is deemed suitable for prisoners; {@code false} otherwise.
     *
     * @since 0.50.05
     */
    public boolean isPrisonerSuitableStatus() {
        return isPrisonerSuitableStatus;
    }

    /**
     * Checks whether the personnel status is a cause of death.
     *
     * @return {@code true} if the personnel status is a cause of death; {@code false} otherwise.
     *
     * @since 0.50.05
     */
    public boolean isCauseOfDeath() {
        return isCauseOfDeath;
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
            case NEGATIVE -> spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());
            case WARNING -> spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
            case NEUTRAL -> "";
            case POSITIVE -> spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());
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
     * Checks if the character has either the {@link #ACTIVE} or {@link #CAMP_FOLLOWER} personnel status.
     *
     * @return {@code true} if the character has the {@link #ACTIVE} personnel status {@code false} otherwise.
     */
    public boolean isActiveFlexible() {
        return this == ACTIVE || this == CAMP_FOLLOWER;
    }

    /**
     * Checks if the character has the {@link #ACTIVE} personnel status.
     *
     * <p><b>Usage:</b> In most cases we likely want to use {@link #isActiveFlexible()} as this will also return
     * {@code true} for 'camp follower' characters. Those characters are also 'active', just not active employees of the
     * player's campaign.</p>
     *
     * @return {@code true} if the character has the {@link #ACTIVE} personnel status {@code false} otherwise.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if the character has the {@link #MIA} personnel status.
     *
     * @return {@code true} if the character has the {@link #MIA} personnel status {@code false} otherwise.
     */
    public boolean isMIA() {
        return this == MIA;
    }

    /**
     * Checks if the character has the {@link #POW} personnel status.
     *
     * @return {@code true} if the character has the {@link #POW} personnel status {@code false} otherwise.
     */
    public boolean isPoW() {
        return this == POW;
    }

    /**
     * Checks if the character has the {@link #ON_LEAVE} personnel status.
     *
     * @return {@code true} if the character has the {@link #ON_LEAVE} personnel status {@code false} otherwise.
     */
    public boolean isOnLeave() {
        return this == ON_LEAVE;
    }

    /**
     * Checks if the character has the {@link #ON_MATERNITY_LEAVE} personnel status.
     *
     * @return {@code true} if the character has the {@link #ON_MATERNITY_LEAVE} personnel status {@code false}
     *       otherwise.
     */
    public boolean isOnMaternityLeave() {
        return this == ON_MATERNITY_LEAVE;
    }

    /**
     * Checks if the character has the {@link #AWOL} personnel status.
     *
     * @return {@code true} if the character has the {@link #AWOL} personnel status {@code false} otherwise.
     */
    public boolean isAwol() {
        return this == AWOL;
    }

    /**
     * Checks if the character has the {@link #RETIRED} personnel status.
     *
     * @return {@code true} if the character has the {@link #RETIRED} personnel status {@code false} otherwise.
     */
    public boolean isRetired() {
        return this == RETIRED;
    }

    /**
     * Checks if the character has the {@link #RESIGNED} personnel status.
     *
     * @return {@code true} if the character has the {@link #RESIGNED} personnel status {@code false} otherwise.
     */
    public boolean isResigned() {
        return this == RESIGNED;
    }

    /**
     * Checks if the character has the {@link #SACKED} personnel status.
     *
     * @return {@code true} if the character has the {@link #SACKED} personnel status {@code false} otherwise.
     */
    public boolean isSacked() {
        return this == SACKED;
    }

    /**
     * Checks if the character has the {@link #LEFT} personnel status.
     *
     * @return {@code true} if the character has the {@link #LEFT} personnel status {@code false} otherwise.
     */
    public boolean isLeft() {
        return this == LEFT;
    }

    /**
     * Checks if the character has the {@link #DESERTED} personnel status.
     *
     * @return {@code true} if the character has the {@link #DESERTED} personnel status {@code false} otherwise.
     */
    public boolean isDeserted() {
        return this == DESERTED;
    }

    /**
     * Checks if the character has the {@link #DEFECTED} personnel status.
     *
     * @return {@code true} if the character has the {@link #DEFECTED} personnel status {@code false} otherwise.
     */
    public boolean isDefected() {
        return this == DEFECTED;
    }

    /**
     * Checks if the character has the {@link #STUDENT} personnel status.
     *
     * @return {@code true} if the character has the {@link #STUDENT} personnel status {@code false} otherwise.
     */
    public boolean isStudent() {
        return this == STUDENT;
    }

    /**
     * Checks if the character has the {@link #MISSING} personnel status.
     *
     * @return {@code true} if the character has the {@link #MISSING} personnel status {@code false} otherwise.
     */
    public boolean isMissing() {
        return this == MISSING;
    }

    /**
     * Checks if the character has the {@link #KIA} personnel status.
     *
     * @return {@code true} if the character has the {@link #KIA} personnel status {@code false} otherwise.
     */
    public boolean isKIA() {
        return this == KIA;
    }

    /**
     * Checks if the character has the {@link #HOMICIDE} personnel status.
     *
     * @return {@code true} if the character has the {@link #HOMICIDE} personnel status {@code false} otherwise.
     */
    public boolean isHomicide() {
        return this == HOMICIDE;
    }

    /**
     * Checks if the character has the {@link #WOUNDS} personnel status.
     *
     * @return {@code true} if the character has the {@link #WOUNDS} personnel status {@code false} otherwise.
     */
    public boolean isWounds() {
        return this == WOUNDS;
    }

    /**
     * Checks if the character has the {@link #DISEASE} personnel status.
     *
     * @return {@code true} if the character has the {@link #DISEASE} personnel status {@code false} otherwise.
     */
    public boolean isDisease() {
        return this == DISEASE;
    }

    /**
     * Checks if the character has the {@link #ACCIDENTAL} personnel status.
     *
     * @return {@code true} if the character has the {@link #ACCIDENTAL} personnel status {@code false} otherwise.
     */
    public boolean isAccidental() {
        return this == ACCIDENTAL;
    }

    /**
     * Checks if the character has the {@link #NATURAL_CAUSES} personnel status.
     *
     * @return {@code true} if the character has the {@link #NATURAL_CAUSES} personnel status {@code false} otherwise.
     */
    public boolean isNaturalCauses() {
        return this == NATURAL_CAUSES;
    }

    /**
     * Checks if the character has the {@link #OLD_AGE} personnel status.
     *
     * @return {@code true} if the character has the {@link #OLD_AGE} personnel status {@code false} otherwise.
     */
    public boolean isOldAge() {
        return this == OLD_AGE;
    }

    /**
     * Checks if the character has the {@link #MEDICAL_COMPLICATIONS} personnel status.
     *
     * @return {@code true} if the character has the {@link #MEDICAL_COMPLICATIONS} personnel status {@code false}
     *       otherwise.
     */
    public boolean isMedicalComplications() {
        return this == MEDICAL_COMPLICATIONS;
    }

    /**
     * Checks if the character has the {@link #PREGNANCY_COMPLICATIONS} personnel status.
     *
     * @return {@code true} if the character has the {@link #PREGNANCY_COMPLICATIONS} personnel status {@code false}
     *       otherwise.
     */
    public boolean isPregnancyComplications() {
        return this == PREGNANCY_COMPLICATIONS;
    }

    /**
     * Checks if the character has the {@link #UNDETERMINED} personnel status.
     *
     * @return {@code true} if the character has the {@link #UNDETERMINED} personnel status {@code false} otherwise.
     */
    public boolean isUndetermined() {
        return this == UNDETERMINED;
    }

    /**
     * Checks if the character has the {@link #SUICIDE} personnel status.
     *
     * @return {@code true} if the character has the {@link #SUICIDE} personnel status {@code false} otherwise.
     */
    public boolean isSuicide() {
        return this == SUICIDE;
    }

    /**
     * Checks if the character has the {@link #ENEMY_BONDSMAN} personnel status.
     *
     * @return {@code true} if the character has the {@link #ENEMY_BONDSMAN} personnel status {@code false} otherwise.
     */
    public boolean isEnemyBondsman() {
        return this == ENEMY_BONDSMAN;
    }

    /**
     * Checks if the character has the {@link #DISHONORABLY_DISCHARGED} personnel status.
     *
     * @return {@code true} if the character has the {@link #DISHONORABLY_DISCHARGED} personnel status {@code false}
     *       otherwise.
     */
    public boolean isDishonorablyDischarged() {
        return this == DISHONORABLY_DISCHARGED;
    }

    /**
     * Checks if the character has the {@link #CAMP_FOLLOWER} personnel status.
     *
     * @return {@code true} if the character has the {@link #CAMP_FOLLOWER} personnel status {@code false} otherwise.
     */
    public boolean isCampFollower() {
        return this == CAMP_FOLLOWER;
    }

    /**
     * Checks if the character has the {@link #BONDSREF} personnel status.
     *
     * @return {@code true} if the character has the {@link #BONDSREF} personnel status {@code false} otherwise.
     */
    public boolean isBondsref() {
        return this == BONDSREF;
    }

    /**
     * Checks if the character has the {@link #SEPPUKU} personnel status.
     *
     * @return {@code true} if the character has the {@link #SEPPUKU} personnel status {@code false} otherwise.
     */
    public boolean isSeppuku() {
        return this == SEPPUKU;
    }

    /**
     * Checks if the character has the {@link #BACKGROUND_CHARACTER} personnel status.
     *
     * @return {@code true} if the character has the {@link #BACKGROUND_CHARACTER} personnel status {@code false}
     *       otherwise.
     */
    public boolean isBackground() {
        return this == BACKGROUND_CHARACTER;
    }

    /**
     * Checks if the character has the {@link #IMPRISONED} personnel status.
     *
     * @return {@code true} if the character has the {@link #IMPRISONED} personnel status {@code false} otherwise.
     */
    public boolean isImprisoned() {
        return this == IMPRISONED;
    }

    /**
     * @return {@code true} if a person is currently absent from the core force, otherwise {@code false}
     */
    public boolean isAbsent() {
        return isMIA() ||
                     isPoW() ||
                     isOnLeave() ||
                     isOnMaternityLeave() ||
                     isAwol() ||
                     isStudent();
    }

    /**
     * Determines whether a person is eligible to receive a salary.
     *
     * @return {@code true} if the person is eligible to receive a salary; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.06
     */
    public boolean isSalaryEligible() {
        return isActive() || isPoW() || isOnLeave() || isOnMaternityLeave() || isStudent();
    }

    /**
     * @return {@code true} if a person has left the unit, otherwise {@code false}
     */
    public boolean isDepartedUnit() {
        return isDead() ||
                     isRetired() ||
                     isResigned() ||
                     isSacked() ||
                     isDeserted() ||
                     isDefected() ||
                     isMissing() ||
                     isLeft() ||
                     isImprisoned() ||
                     isEnemyBondsman() ||
                     isDishonorablyDischarged() ||
                     // We count background characters as departed, even though they technically never joined
                     isBackground();
    }

    /**
     * @return {@code true} if a person is dead, otherwise {@code false}
     */
    public boolean isDead() {
        return isKIA() ||
                     isHomicide() ||
                     isWounds() ||
                     isDisease() ||
                     isAccidental() ||
                     isNaturalCauses() ||
                     isOldAge() ||
                     isMedicalComplications() ||
                     isPregnancyComplications() ||
                     isUndetermined() ||
                     isSuicide() ||
                     isBondsref() ||
                     isSeppuku();
    }

    /**
     * @return {@code true} if a person is dead or MIA, otherwise {@code false}
     */
    public boolean isDeadOrMIA() {
        return isDead() || isMIA();
    }
    // endregion Boolean Comparison Methods

    /**
     * Retrieves a list of implemented personnel statuses based on the specified criteria.
     *
     * @param isFree              a boolean flag that determines the filtering behavior:
     *                            <ul>
     *                              <li>If {@code true}, all relevant {@link PersonnelStatus} values are returned.</li>
     *                              <li>If {@code false}, only {@link PersonnelStatus} values suitable for prisoners
     *                                  are included in the result.</li>
     *                            </ul>
     * @param includeCauseOfDeath a boolean flag that determines whether statuses marked as causes of death should be
     *                            included:
     *                            <ul>
     *                              <li>If {@code true}, cause of death statuses are included.</li>
     *                              <li>If {@code false}, cause of death statuses are excluded.</li>
     *                            </ul>
     *
     * @return a {@link List} of {@link PersonnelStatus} objects that match the specified criteria.
     *
     * @since 0.50.05
     */
    public static List<PersonnelStatus> getImplementedStatuses(boolean isFree, boolean includeCauseOfDeath) {
        List<PersonnelStatus> result = new ArrayList<>();
        for (PersonnelStatus value : values()) {
            if (value.isCauseOfDeath && !includeCauseOfDeath) {
                continue;
            }

            if (isFree || value.isPrisonerSuitableStatus()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Retrieves a list of personnel statuses that are marked as causes of death, based on the specified filtering
     * criteria.
     *
     * @param isFree a boolean flag that determines the filtering behavior:
     *               <ul>
     *                 <li>If {@code true}, all statuses marked as causes of death are included.</li>
     *                 <li>If {@code false}, only statuses marked as causes of death and suitable for prisoners
     *                     are included.</li>
     *               </ul>
     *
     * @return a {@link List} of {@link PersonnelStatus} objects that are causes of death and meet the specified
     *       criteria.
     *
     * @since 0.50.05
     */
    public static List<PersonnelStatus> getCauseOfDeathStatuses(boolean isFree) {
        List<PersonnelStatus> result = new ArrayList<>();
        for (PersonnelStatus value : values()) {
            if (!value.isCauseOfDeath) {
                continue;
            }

            if (isFree || value.isPrisonerSuitableStatus()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Validates and updates the {@link PersonnelStatus} of a {@link Person} based on their prisoner status.
     *
     * <p>This method is specifically designed to ensure data integrity in campaigns where invalid personnel statuses
     * may exist for prisoners (introduced in version <b>v0.50.05</b>). It guarantees that prisoners have status values
     * appropriate to their situation within the game context. </p>
     *
     * @param campaign   the {@link Campaign} object representing the current campaign. It is used for updating the
     *                   person's status with reference to the campaign context (e.g., current date).
     * @param person     the {@link Person} whose status is to be validated and potentially updated. This will be
     *                   modified only if they are marked as a prisoner and their status is deemed invalid.
     * @param isPrisoner a boolean flag indicating if the person is a prisoner:
     *                   <ul>
     *                     <li>{@code true}: The person is a prisoner, and their status will be validated
     *                     and corrected if needed.</li>
     *                     <li>{@code false}: No validation or updates are applied to the person's status.</li>
     *                   </ul>
     *
     * @since 0.50.05
     */
    public static void statusValidator(Campaign campaign, Person person, boolean isPrisoner) {
        if (!isPrisoner) {
            return;
        }

        PersonnelStatus status = person.getStatus();
        if (!status.isPrisonerSuitableStatus()) {
            person.changeStatus(campaign, campaign.getLocalDate(), ACTIVE);
        }
    }

    // region File I/O

    /**
     * Converts a given string to its corresponding {@code PersonnelStatus} enumeration value. The method first attempts
     * to parse the string as the name of an {@code PersonnelStatus} enum value. If that fails, it attempts to parse the
     * string as an integer representing the ordinal of an {@code PersonnelStatus} enum value. If neither succeeds, it
     * logs an error and defaults to returning {@code ACTIVE}.
     *
     * @param text the input string to parse, which represents either the name or the ordinal of an
     *             {@code PersonnelStatus} enum value.
     *
     * @return the corresponding {@code PersonnelStatus} enum instance for the given input string, or {@code ACTIVE} if
     *       no valid match is found.
     */
    public static PersonnelStatus fromString(String text) {
        try {
            return PersonnelStatus.valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            return PersonnelStatus.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {
        }

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
