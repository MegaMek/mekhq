/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
    ORIGIN_FACTION("BattleMekFactionGenerationMethod.ORIGIN_FACTION.text",
          "BattleMekFactionGenerationMethod.ORIGIN_FACTION.toolTipText"),
    CAMPAIGN_FACTION("BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.text",
          "BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.toolTipText"),
    SPECIFIED_FACTION("BattleMekFactionGenerationMethod.SPECIFIED_FACTION.text",
          "BattleMekFactionGenerationMethod.SPECIFIED_FACTION.toolTipText");
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
