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
package mekhq.campaign.mission.mission;

import static megamek.common.compute.Compute.d6;

import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractEmployerDetermination {
    private final CampaignTypeForContractDetermination campaignType;
    private final HiringHallLevel hiringHallLevel;
    private final int forceReputationModifier;
    private final ConnectionsLevel connectionsLevel;

    public ContractEmployerDetermination(CampaignTypeForContractDetermination campaignType,
          HiringHallLevel hiringHallLevel, int forceReputationModifier, ConnectionsLevel connectionsLevel) {
        this.campaignType = campaignType;
        this.hiringHallLevel = hiringHallLevel;
        this.forceReputationModifier = forceReputationModifier;
        this.connectionsLevel = connectionsLevel;
    }

    public GlobalEmployerTableValue getContractEmployer() {

        // CamOps pg 39 rev 5th printing states that a player can pick any employer at or below their roll. This
        // creates a UX issue for MekHQ. To avoid spamming the player, we instead use the exact employer matching the
        // roll
        GlobalEmployerTableValue globalEmployerType = getGlobalEmployer();
        IndependentEmployerTableValue independentEmployerType = getIndependentEmployer();
    }

    private IndependentEmployerTableValue getIndependentEmployer() {
        int roll = getEmployerRoll();
        return IndependentEmployerTableValue.getEmployerForRoll(roll);
    }

    private GlobalEmployerTableValue getGlobalEmployer() {
        int roll = getEmployerRoll();
        return GlobalEmployerTableValue.getEmployerForRoll(roll);
    }

    private int getEmployerRoll() {
        int roll = d6(2);
        int hiringHallModifier = hiringHallLevel.getEmployerModifier();
        int connectionsModifier = connectionsLevel.getEquipLevel();

        return roll + hiringHallModifier + connectionsModifier + forceReputationModifier;
    }
}
