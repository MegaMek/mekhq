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
package mekhq.campaign.universe.enums;

import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * @author Justin "Windchild" Bowen
 */
public enum MysteryBoxType {
    //region Enum Declarations
    THIRD_SUCCESSION_WAR("MysteryBoxType.THIRD_SUCCESSION_WAR.text", "MysteryBoxType.THIRD_SUCCESSION_WAR.toolTipText"),
    STAR_LEAGUE_ROYAL("MysteryBoxType.STAR_LEAGUE_ROYAL.text", "MysteryBoxType.STAR_LEAGUE_ROYAL.toolTipText"),
    STAR_LEAGUE_REGULAR("MysteryBoxType.STAR_LEAGUE_REGULAR.text", "MysteryBoxType.STAR_LEAGUE_REGULAR.toolTipText"),
    INNER_SPHERE_EXPERIMENTAL("MysteryBoxType.INNER_SPHERE_EXPERIMENTAL.text", "MysteryBoxType.INNER_SPHERE_EXPERIMENTAL.toolTipText"),
    CLAN_KESHIK("MysteryBoxType.CLAN_KESHIK.text", "MysteryBoxType.CLAN_KESHIK.toolTipText"),
    CLAN_FRONT_LINE("MysteryBoxType.CLAN_FRONT_LINE.text", "MysteryBoxType.CLAN_FRONT_LINE.toolTipText"),
    CLAN_SECOND_LINE("MysteryBoxType.CLAN_SECOND_LINE.text", "MysteryBoxType.CLAN_SECOND_LINE.toolTipText"),
    CLAN_EXPERIMENTAL("MysteryBoxType.CLAN_EXPERIMENTAL.text", "MysteryBoxType.CLAN_EXPERIMENTAL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    MysteryBoxType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isThirdSuccessionWar() {
        return this == THIRD_SUCCESSION_WAR;
    }

    public boolean isStarLeagueRoyal() {
        return this == STAR_LEAGUE_ROYAL;
    }

    public boolean isStarLeagueRegular() {
        return this == STAR_LEAGUE_REGULAR;
    }

    public boolean isInnerSphereExperimental() {
        return this == INNER_SPHERE_EXPERIMENTAL;
    }

    public boolean isClanKeshik() {
        return this == CLAN_KESHIK;
    }

    public boolean isClanFrontLine() {
        return this == CLAN_FRONT_LINE;
    }

    public boolean isClanSecondLine() {
        return this == CLAN_SECOND_LINE;
    }

    public boolean isClanExperimental() {
        return this == CLAN_EXPERIMENTAL;
    }
    //endregion Boolean Comparison Methods

/*
    public AbstractMysteryBox getMysteryBox() {
        switch (this) {
            case STAR_LEAGUE_ROYAL:
                return new StarLeagueRoyalMysteryBox();
            case STAR_LEAGUE_REGULAR:
                return new StarLeagueRegularMysteryBox();
            case INNER_SPHERE_EXPERIMENTAL:
                return new InnerSphereExperimentalMysteryBox();
            case CLAN_KESHIK:
                return new ClanKeshikMysteryBox();
            case CLAN_FRONT_LINE:
                return new ClanFrontLineMysteryBox();
            case CLAN_SECOND_LINE:
                return new ClanSecondLineMysteryBox();
            case CLAN_EXPERIMENTAL:
                return new ClanExperimentalMysteryBox();
            case THIRD_SUCCESSION_WAR:
            default:
                return new ThirdSuccessionWarMysteryBox();
        }
    }
*/

    @Override
    public String toString() {
        return name;
    }
}
