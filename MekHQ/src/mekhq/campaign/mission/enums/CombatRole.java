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
package mekhq.campaign.mission.enums;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum CombatRole {
    // region Enum Declarations
    MANEUVER("CombatRole.MANEUVER.text", "CombatRole.MANEUVER.toolTipText"),
    FRONTLINE("CombatRole.FRONTLINE.text", "CombatRole.FRONTLINE.toolTipText"),
    PATROL("CombatRole.PATROL.text", "CombatRole.PATROL.toolTipText"),
    TRAINING("CombatRole.TRAINING.text", "CombatRole.TRAINING.toolTipText"),
    AUXILIARY("CombatRole.AUXILIARY.text", "CombatRole.AUXILIARY.toolTipText"),
    RESERVE("CombatRole.RESERVE.text", "CombatRole.RESERVE.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    CombatRole(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
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
    public boolean isManeuver() {
        return this == MANEUVER;
    }

    public boolean isFrontline() {
        return this == FRONTLINE;
    }

    public boolean isPatrol() {
        return this == PATROL;
    }

    public boolean isTraining() {
        return this == TRAINING;
    }

    public boolean isAuxiliary() {
        return this == AUXILIARY;
    }

    public boolean isReserve() {
        return this == RESERVE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a {@link String} into a {@link CombatRole} enum value.
     * <p>
     * This method first attempts to interpret the input string as an integer and then maps it
     * to the corresponding {@link CombatRole} based on its ordinal index. If that fails, it
     * attempts to match the string to the name of a {@link CombatRole} using {@code Enum.valueOf(String)}.
     * If both parsing approaches fail, it logs an error and returns the default value {@code IN_RESERVE}.
     * </p>
     *
     * @param text the string to be parsed into a {@link CombatRole}.
     *             The string can represent either:
     *             <ul>
     *               <li>An integer corresponding to the ordinal index of a {@link CombatRole}.</li>
     *               <li>The name of a {@link CombatRole}.</li>
     *             </ul>
     * @return the corresponding {@link CombatRole} if the input is valid;
     *         otherwise, returns {@code IN_RESERVE}.
     */
    public static CombatRole parseFromString(final String text) {
        try {
            int value = Integer.parseInt(text);
            return values()[value];
        } catch (Exception ignored) {}

        try {
            return valueOf(text);
        } catch (Exception ignored) {}

        // <50.02 compatibility handler
        return switch (text) {
            case "FIGHTING" -> MANEUVER;
            case "SCOUTING", "RECON" -> PATROL;
            case "DEFENCE", "GARRISON" -> FRONTLINE;
            case "IN_RESERVE", "UNASSIGNED" -> RESERVE;
            default -> {
                MMLogger.create(CombatRole.class)
                    .warn(String.format("Unable to parse %s into an CombatRole. Returning RESERVE.",
                        text));

                yield RESERVE;
            }

        };

        // To be uncommented once the above compatibility handler is removed.
//        MMLogger.create(CombatRole.class)
//            .error("Unable to parse " + text + " into an CombatRole. Returning RESERVE.");
//
//        return RESERVE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
