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
package mekhq.campaign.events;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

/**
 * Triggered once the player has answered a Faction Standing ultimatum and its consequences have been applied. An
 * ultimatum the GM ignored triggers nothing, since it had no consequences.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class UltimatumResolvedEvent extends CampaignEvent {
    private final String ultimatumName;
    private final Faction chosenFaction;
    private final boolean wentRogue;
    private final List<Person> departed;

    /**
     * @param campaign      the campaign that received the ultimatum
     * @param ultimatumName the ultimatum's unique name
     * @param chosenFaction the faction the campaign now belongs to: the chosen side's, or Mercenary or Pirate
     * @param wentRogue     {@code true} if the player went Mercenary or Pirate instead of choosing a side
     * @param departed      everyone who left the campaign or died because of the decision
     */
    public UltimatumResolvedEvent(Campaign campaign, String ultimatumName, Faction chosenFaction, boolean wentRogue,
          List<Person> departed) {
        super(campaign);
        this.ultimatumName = ultimatumName;
        this.chosenFaction = chosenFaction;
        this.wentRogue = wentRogue;
        this.departed = List.copyOf(departed);
    }

    public String getUltimatumName() {
        return ultimatumName;
    }

    public Faction getChosenFaction() {
        return chosenFaction;
    }

    public boolean isWentRogue() {
        return wentRogue;
    }

    /**
     * @return everyone who left the campaign or died because of the decision
     */
    public List<Person> getDeparted() {
        return departed;
    }
}
