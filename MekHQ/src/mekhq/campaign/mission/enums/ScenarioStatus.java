/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum ScenarioStatus {
    //region Enum Declarations
    CURRENT("ScenarioStatus.CURRENT.text", "ScenarioStatus.CURRENT.toolTipText"),
    DECISIVE_VICTORY("ScenarioStatus.DECISIVE_VICTORY.text", "ScenarioStatus.DECISIVE_VICTORY.toolTipText"),
    VICTORY("ScenarioStatus.VICTORY.text", "ScenarioStatus.VICTORY.toolTipText"),
    MARGINAL_VICTORY("ScenarioStatus.MARGINAL_VICTORY.text", "ScenarioStatus.MARGINAL_VICTORY.toolTipText"),
    PYRRHIC_VICTORY("ScenarioStatus.PYRRHIC_VICTORY.text", "ScenarioStatus.PYRRHIC_VICTORY.toolTipText"),
    DRAW("ScenarioStatus.DRAW.text", "ScenarioStatus.DRAW.toolTipText"),
    MARGINAL_DEFEAT("ScenarioStatus.MARGINAL_DEFEAT.text", "ScenarioStatus.MARGINAL_DEFEAT.toolTipText"),
    DEFEAT("ScenarioStatus.DEFEAT.text", "ScenarioStatus.DEFEAT.toolTipText"),
    DECISIVE_DEFEAT("ScenarioStatus.DECISIVE_DEFEAT.text", "ScenarioStatus.DECISIVE_DEFEAT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    ScenarioStatus(final String name, final String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
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

    public boolean isOverallVictory() {
        return isDecisiveVictory() || isVictory() || isMarginalVictory() || isPyrrhicVictory();
    }

    public boolean isOverallDefeat() {
        return isDecisiveDefeat() || isDefeat() || isMarginalDefeat();
    }
    //endregion Boolean Comparisons

    //region File I/O
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

        MekHQ.getLogger().error("Failed to parse text " + text + " into a ScenarioStatus, returning CURRENT.");

        return CURRENT;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
