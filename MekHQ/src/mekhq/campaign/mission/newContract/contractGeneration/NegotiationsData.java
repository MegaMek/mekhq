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

/**
 * The negotiated terms of a contract: command rights plus the salvage, support, and transport percentages. Certain
 * sentinel values carry special meaning &mdash; see {@link #isExchange()} and {@link #isBattleCompensation()}.
 *
 * @param commandRights   the negotiated command-rights arrangement
 * @param salvageRights   the salvage share (or the exchange marker; see {@link #isExchange()})
 * @param supportRights   the support share (a negative value denotes battle-loss compensation)
 * @param transportRights the transport share
 */
public record NegotiationsData(
      ContractCommandRights commandRights,
      double salvageRights,
      double supportRights,
      double transportRights) {
    /**
     * @return {@code true} if salvage rights are set to the "salvage exchange" marker rather than a percentage
     */
    public boolean isExchange() {
        return salvageRights == SALVAGE_RIGHTS_EXCHANGE_MARKER;
    }

    /**
     * @return {@code true} if support rights are negative, indicating battle-loss compensation terms
     */
    public boolean isBattleCompensation() {
        return supportRights < 0;
    }

    /**
     * Returns a copy with a single clause replaced. The command-rights clause expects a {@link ContractCommandRights}
     * value; every other clause expects a {@code double}.
     *
     * @param clause   the clause to replace
     * @param newValue the replacement value, whose type must match the clause
     *
     * @return a new {@link NegotiationsData} with the clause updated
     *
     * @throws IllegalArgumentException if {@code newValue} is not the type expected for {@code clause}
     */
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
