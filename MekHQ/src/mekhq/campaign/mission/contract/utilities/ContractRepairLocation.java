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
package mekhq.campaign.mission.contract.utilities;

import java.util.List;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.unit.Unit;

public class ContractRepairLocation {
    /**
     * Determines the repair location for the contract based on the contract type.
     *
     * <p>The returned repair location corresponds to the type of operation:</p>
     *
     * <ul>
     *   <li>Guerrilla warfare contracts: {@link Unit#SITE_IMPROVISED}</li>
     *   <li>Raid-type contracts: {@link Unit#SITE_FIELD_WORKSHOP}</li>
     *   <li>All other contracts: {@link Unit#SITE_FACILITY_BASIC}</li>
     * </ul>
     *
     * @return the repair location constant based on the contract type
     */
    public static int getRepairLocation(ContractObjectiveType contractType) {
        int repairLocation = Unit.SITE_FACILITY_BASIC;

        if (contractType.isGuerrillaType()) {
            repairLocation = Unit.SITE_IMPROVISED;
        } else if (contractType.isRaidType()) {
            repairLocation = Unit.SITE_FIELD_WORKSHOP;
        }

        return repairLocation;
    }

    /**
     * Determines the best available repair location from a list of active contracts.
     *
     * <p>This method evaluates all active contracts and returns the highest quality repair facility available.
     * Repair locations are ranked numerically, with higher values representing better facilities. If no active
     * contracts exist, a basic facility is assumed to be available.</p>
     *
     * @param activeContracts the list of active contracts to evaluate for repair facilities
     *
     * @return the numeric value of the best available repair location; returns {@link Unit#SITE_FACILITY_BASIC} if no
     *       contracts are active
     */
    public static int getBestRepairLocation(List<AbstractContract> activeContracts) {
        if (activeContracts.isEmpty()) {
            return Unit.SITE_FACILITY_BASIC;
        }

        int bestSite = Unit.SITE_IMPROVISED;
        for (AbstractContract contract : activeContracts) {
            int repairLocation = getRepairLocation(contract.getObjectiveType());
            if (repairLocation > bestSite) {
                bestSite = repairLocation;
            }
        }

        return bestSite;
    }
}
