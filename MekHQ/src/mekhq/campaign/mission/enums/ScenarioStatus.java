/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum ScenarioStatus {
    // region Enum Declarations
    CURRENT("ScenarioStatus.CURRENT.text", "ScenarioStatus.CURRENT.toolTipText"),
    DECISIVE_VICTORY("ScenarioStatus.DECISIVE_VICTORY.text", "ScenarioStatus.DECISIVE_VICTORY.toolTipText"),
    VICTORY("ScenarioStatus.VICTORY.text", "ScenarioStatus.VICTORY.toolTipText"),
    MARGINAL_VICTORY("ScenarioStatus.MARGINAL_VICTORY.text", "ScenarioStatus.MARGINAL_VICTORY.toolTipText"),
    PYRRHIC_VICTORY("ScenarioStatus.PYRRHIC_VICTORY.text", "ScenarioStatus.PYRRHIC_VICTORY.toolTipText"),
    DRAW("ScenarioStatus.DRAW.text", "ScenarioStatus.DRAW.toolTipText"),
    MARGINAL_DEFEAT("ScenarioStatus.MARGINAL_DEFEAT.text", "ScenarioStatus.MARGINAL_DEFEAT.toolTipText"),
    DEFEAT("ScenarioStatus.DEFEAT.text", "ScenarioStatus.DEFEAT.toolTipText"),
    DECISIVE_DEFEAT("ScenarioStatus.DECISIVE_DEFEAT.text", "ScenarioStatus.DECISIVE_DEFEAT.toolTipText"),
    REFUSED_ENGAGEMENT("ScenarioStatus.REFUSED_ENGAGEMENT.text", "ScenarioStatus.REFUSED_ENGAGEMENT.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    ScenarioStatus(final String name, final String toolTipText) {
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
    public boolean isCurrent() {
        return this == CURRENT;
    }

    public boolean isDecisiveVictory() {
        return this == DECISIVE_VICTORY;
    }

    public boolean isVictory() {
        return this == VICTORY;
    }

    public boolean isMarginalVictory() {
        return this == MARGINAL_VICTORY;
    }

    public boolean isPyrrhicVictory() {
        return this == PYRRHIC_VICTORY;
    }

    public boolean isDraw() {
        return this == DRAW;
    }

    public boolean isMarginalDefeat() {
        return this == MARGINAL_DEFEAT;
    }

    public boolean isDefeat() {
        return this == DEFEAT;
    }

    public boolean isDecisiveDefeat() {
        return this == DECISIVE_DEFEAT;
    }

    public boolean isRefusedEngagement() {
        return this == REFUSED_ENGAGEMENT;
    }

    public boolean isOverallVictory() {
        return isDecisiveVictory() || isVictory() || isMarginalVictory() || isPyrrhicVictory();
    }

    public boolean isOverallDefeat() {
        return isDecisiveDefeat() || isDefeat() || isMarginalDefeat() || isRefusedEngagement();
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static ScenarioStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return CURRENT;
                case 1:
                    return VICTORY;
                case 2:
                    return MARGINAL_VICTORY;
                case 3:
                    return DEFEAT;
                case 4:
                    return MARGINAL_DEFEAT;
                case 5:
                    return DRAW;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(ScenarioStatus.class)
              .error("Unable to parse " + text + " into a ScenarioStatus. Returning CURRENT.");
        return CURRENT;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
