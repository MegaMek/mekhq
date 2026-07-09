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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.mission.newContract.contractGeneration.NegotiationTermsTables.SALVAGE_RIGHTS_EXCHANGE_MARKER;

import mekhq.campaign.mission.enums.ContractCommandRights;

public record NegotiationsData(
      ContractCommandRights commandRights,
      double salvageRights,
      double supportRights,
      double transportRights) {
    public boolean isExchange() {
        return salvageRights == SALVAGE_RIGHTS_EXCHANGE_MARKER;
    }

    public boolean isBattleCompensation() {
        return supportRights < 0;
    }

    public NegotiationsData updateClause(ContractNegotiationClause clause, Object newValue) {
        try {
            switch (clause) {
                case COMMAND_RIGHTS -> {
                    return new NegotiationsData((ContractCommandRights) newValue,
                          salvageRights,
                          supportRights,
                          transportRights);
                }
                case SALVAGE_RIGHTS -> {
                    return new NegotiationsData(commandRights, (double) newValue, supportRights, transportRights);
                }
                case SUPPORT_RIGHTS -> {
                    return new NegotiationsData(commandRights, salvageRights, (double) newValue, transportRights);
                }
                case TRANSPORT_RIGHTS -> {
                    return new NegotiationsData(commandRights, salvageRights, supportRights, (double) newValue);
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid value type for clause " + clause, e);
        }

        return this;
    }
}
