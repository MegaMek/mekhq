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
package mekhq.campaign.mission.newContract;

import java.time.LocalDate;

import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AbstractMissionTransition;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

public class ClanHomeworldsExclusion {
    /**
     * Radius, in light years, of the Clan Homeworlds exclusion zone centered on Strana Mechty.
     */
    private static final double HOMEWORLDS_EXCLUSION_RADIUS = 450;

    private static final String STRANA_MECHTY_SYSTEM_ID = "Strana Mechty";

    /**
     * Outside of Operation Bulldog ({@link MHQConstants#OPERATION_BULLDOG_START} to
     * {@link MHQConstants#OPERATION_BULLDOG_END}), no non-Clan faction has the reach to strike within
     * {@value #HOMEWORLDS_EXCLUSION_RADIUS} light years of Strana Mechty: that one historical invasion is the only time
     * Inner Sphere/mercenary forces ever operated that deep in the Clan Homeworlds.
     *
     * @param contract the contract whose target system to check
     * @param campaign the active campaign, used to compute the arrival date (current date plus travel time)
     *
     * @return {@code true} if the contract's attacking faction is non-Clan, its target is within the exclusion radius,
     *       and the attacker would arrive there outside the Operation Bulldog window
     */
    public static boolean violatesHomeworldsExclusion(AbstractMissionTransition contract, Campaign campaign) {
        Faction attacker = contract.isPlayerAttacker() ? contract.getEmployerFaction() : contract.getEnemy();
        if (attacker.isClan()) {
            return false;
        }

        PlanetarySystem stranaMechty = Systems.getInstance().getSystemById(STRANA_MECHTY_SYSTEM_ID);
        if ((stranaMechty == null) ||
                  (contract.getSystem().getDistanceTo(stranaMechty) > HOMEWORLDS_EXCLUSION_RADIUS)) {
            return false;
        }

        LocalDate arrivalDate = campaign.getLocalDate().plusDays(contract.getTravelDays(campaign));
        return !(arrivalDate.isAfter(MHQConstants.OPERATION_BULLDOG_START) &&
                       arrivalDate.isBefore(MHQConstants.OPERATION_BULLDOG_END));
    }
}
