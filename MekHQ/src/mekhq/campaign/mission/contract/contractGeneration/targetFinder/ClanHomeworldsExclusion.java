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
package mekhq.campaign.mission.contract.contractGeneration.targetFinder;

import java.time.LocalDate;

import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.ContractUtilities;
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
     * Outside of Operation Bulldog ({@link MHQConstants#IS_INVASION_OF_HUNTRESS_START} to
     * {@link MHQConstants#IS_INVASION_OF_HUNTRESS_END}), no non-Clan faction has the reach to strike within
     * {@value #HOMEWORLDS_EXCLUSION_RADIUS} light years of Strana Mechty: that one historical invasion is the only time
     * Inner Sphere/mercenary forces ever operated that deep in the Clan Homeworlds.
     *
     * @param contract        the contract whose target system to check
     * @param campaign        the active campaign, used to compute the arrival date (current date plus travel time)
     * @param currentLocation the detachment's current location, used to compute the arrival date (current date plus
     *                        travel time)
     *
     * @return {@code true} if exactly one side is a non-Clan attacker, the target is within the exclusion radius, and
     *       the force would arrive there outside the Task Force Serpent window - or inside that window but against an
     *       enemy other than the Smoke Jaguars it was mounted against
     */
    public static boolean violatesHomeworldsExclusion(AbstractContract contract, Campaign campaign,
          CurrentLocation currentLocation) {
        Faction allyFaction = contract.getEmployerFaction();
        boolean isAllyInnerSphere = !allyFaction.isClan();
        boolean alliedIsAttacker = contract.getObjectiveType().getChaosObjectiveType().isAttacker();
        boolean isAlliedInnerSphereAttacker = isAllyInnerSphere && alliedIsAttacker;

        Faction enemyFaction = contract.getEnemyFaction();
        boolean isEnemyInnerSphere = !enemyFaction.isClan();
        boolean enemyIsAttacker = contract.getOpposingObjectiveType().getChaosObjectiveType().isAttacker();
        boolean isEnemyInnerSphereAttacker = isEnemyInnerSphere && enemyIsAttacker;

        boolean triggerExclusion = isAlliedInnerSphereAttacker != isEnemyInnerSphereAttacker;
        if (!triggerExclusion) {
            return false;
        }

        PlanetarySystem stranaMechty = Systems.getInstance().getSystemById(STRANA_MECHTY_SYSTEM_ID);
        PlanetarySystem targetSystem = contract.getTargetSystem();
        if (stranaMechty == null ||
                  targetSystem == null ||
                  (targetSystem.getDistanceTo(stranaMechty) > HOMEWORLDS_EXCLUSION_RADIUS)) {
            return false;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        int travelDays = ContractUtilities.getTravelDays(campaign,
              contract,
              currentLocation,
              playerForce.isOverridingCommandCircuitRequirements(),
              playerForce.getFactionStandings());

        LocalDate arrivalDate = campaign.getLocalDate().plusDays(travelDays);
        boolean arrivesDuringTaskForceSerpent = !arrivalDate.isBefore(MHQConstants.IS_INVASION_OF_HUNTRESS_START) &&
                                                      !arrivalDate.isAfter(MHQConstants.IS_INVASION_OF_HUNTRESS_END);

        String clanSmokeJaguarCode = "CSJ";
        boolean containsSmokeJaguar = allyFaction.getShortName().equals(clanSmokeJaguarCode) ||
                                            enemyFaction.getShortName().equals(clanSmokeJaguarCode);

        // Task Force Serpent is the sole exception, and only against the Smoke Jaguars it was mounted to destroy.
        return !arrivesDuringTaskForceSerpent || !containsSmokeJaguar;
    }
}
