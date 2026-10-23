/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import javax.swing.JOptionPane;

/**
 * The outcome of the employee turnover prompt shown when advancing the day.
 *
 * @see mekhq.campaign.Campaign#checkTurnoverPrompt()
 */
public enum TurnoverPromptOutcome {
    /** The turnover prompt was not shown to the user. */
    NOT_PROMPTED,
    /** The user chose to launch the employee turnover dialog. */
    EMPLOYEE_TURNOVER,
    /** The user chose to advance the day without resolving turnover. */
    ADVANCE_DAY_REGARDLESS,
    /** The user chose to cancel advancing the day. */
    CANCEL_ADVANCE_DAY;

    public boolean isNotPrompted() {
        return this == NOT_PROMPTED;
    }

    public boolean isEmployeeTurnover() {
        return this == EMPLOYEE_TURNOVER;
    }

    public boolean isAdvanceDayRegardless() {
        return this == ADVANCE_DAY_REGARDLESS;
    }

    public boolean isCancelAdvanceDay() {
        return this == CANCEL_ADVANCE_DAY;
    }

    /**
     * Converts the value returned by {@link JOptionPane#showOptionDialog} for the turnover prompt into an outcome.
     *
     * <p>The prompt's options are, in order, "Employee Turnover", "Advance Day Regardless", and "Cancel Advance
     * Day". Closing the dialog without picking an option ({@link JOptionPane#CLOSED_OPTION}) is treated as
     * {@link #ADVANCE_DAY_REGARDLESS}.</p>
     *
     * @param dialogResult the index of the option the user selected, or {@link JOptionPane#CLOSED_OPTION}
     *
     * @return the matching outcome
     *
     * @throws IllegalArgumentException if {@code dialogResult} does not match any prompt option
     */
    public static TurnoverPromptOutcome fromDialogResult(final int dialogResult) {
        return switch (dialogResult) {
            case 0 -> EMPLOYEE_TURNOVER;
            case 1, JOptionPane.CLOSED_OPTION -> ADVANCE_DAY_REGARDLESS;
            case 2 -> CANCEL_ADVANCE_DAY;
            default -> throw new IllegalArgumentException("Unexpected turnover prompt dialog result: " +
                                                                dialogResult);
        };
    }
}
