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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.force.FormationType.STANDARD;

import megamek.common.units.Entity;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.unit.Unit;

public class ChaosContractDeterminationScale {
    private final static double BATTLE_VALUE_PER_SCALE = 4_500.0; // Draconis Reach first printing pg 36
    private final static double BATTLEFIELD_SUPPORT_POINTS_PER_SCALE = 32.0; // Draconis Reach first printing pg 36
    private final static double BATTLE_VALUE_PER_BSP = 500.0; // Battle for Tukayyid, pg24

    static int generateScaleForDetachment(PlayerForce playerForce, LocalHangar hangar, boolean isCadreDuty,
          boolean convertSupportPointsToBattleValue) {
        int validBattleValue = 0;

        for (Unit unit : hangar.getUnits()) {
            int formationId = unit.getFormationId();
            Formation formation = playerForce.getFormation(formationId);
            if (formation != null) {
                CombatRole roleInMemory = formation.getCombatRoleInMemory();
                boolean hasCombatRole = roleInMemory.isCombatRole() || (isCadreDuty && roleInMemory.isCadre());
                if (formation.isFormationType(STANDARD) && hasCombatRole) {
                    Entity entity = unit.getEntity();
                    validBattleValue += entity != null ? entity.calculateBattleValue(true, true) : 0;
                }
            }
        }

        double battleValuePerScale = BATTLE_VALUE_PER_SCALE;
        if (convertSupportPointsToBattleValue) {
            // Fold the battlefield-support-point allotment into the per-scale Battle Value by converting it to BV.
            battleValuePerScale += BATTLEFIELD_SUPPORT_POINTS_PER_SCALE * BATTLE_VALUE_PER_BSP;
        }

        return (int) ceil(validBattleValue / battleValuePerScale);
    }
}
