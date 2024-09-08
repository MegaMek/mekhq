/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

/**
 * @author Justin "Windchild" Bowen
 */
public enum BattleMekFactionGenerationMethod {
    //region Enum Declarations
    ORIGIN_FACTION("BattleMechFactionGenerationMethod.ORIGIN_FACTION.text", "BattleMechFactionGenerationMethod.ORIGIN_FACTION.toolTipText"),
    CAMPAIGN_FACTION("BattleMechFactionGenerationMethod.CAMPAIGN_FACTION.text", "BattleMechFactionGenerationMethod.CAMPAIGN_FACTION.toolTipText"),
    SPECIFIED_FACTION("BattleMechFactionGenerationMethod.SPECIFIED_FACTION.text", "BattleMechFactionGenerationMethod.SPECIFIED_FACTION.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    BattleMekFactionGenerationMethod(final String name, final String toolTipText) {
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
    public boolean isOriginFaction() {
        return this == ORIGIN_FACTION;
    }

    public boolean isCampaignFaction() {
        return this == CAMPAIGN_FACTION;
    }

    public boolean isSpecifiedFaction() {
        return this == SPECIFIED_FACTION;
    }
    //endregion Boolean Comparison Methods

    public Faction generateFaction(final Person person, final Campaign campaign,
                                   final Faction specifiedFaction) {
        switch (this) {
            case CAMPAIGN_FACTION:
                return campaign.getFaction();
            case SPECIFIED_FACTION:
                return specifiedFaction;
            case ORIGIN_FACTION:
            default:
                return person.getOriginFaction();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
