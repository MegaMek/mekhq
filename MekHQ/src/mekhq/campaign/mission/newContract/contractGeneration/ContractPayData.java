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

import java.util.Map;
import java.util.UUID;

import mekhq.campaign.finances.Money;

/**
 * The full breakdown of a generated contract's payment terms: the base pay, transit pay, per-objective payouts, and the
 * straight-support and transport payment components. Support and transport figures are added in a later stage of
 * generation via the {@code rebuild*} methods, since they depend on values not known when the base pay is first
 * computed.
 *
 * @param basePayData                the base-pay breakdown (peacetime operating costs, combat-unit value, base pay)
 * @param transitPayData             the transit-pay breakdown
 * @param objectivePayDataMap        per-objective payment data, keyed by objective id
 * @param totalObjectivePay          the summed payout across all objectives
 * @param straightSupportMultiplier  the multiplier used to derive the straight-support estimate
 * @param straightSupportEstimate    the estimated straight-support payment
 * @param transportPaymentMultiplier the multiplier used to derive the transport payment
 * @param transportPayment           the transport payment
 */
public record ContractPayData(
      ContractBasePayData basePayData,
      TransitPayData transitPayData,
      Map<UUID, ObjectivePayData> objectivePayDataMap,
      Money totalObjectivePay,
      double straightSupportMultiplier,
      Money straightSupportEstimate,
      double transportPaymentMultiplier,
      Money transportPayment
) {
    /**
     * Creates pay data with the straight-support and transport payment components defaulted to zero, to be populated
     * later via {@link #rebuildIncludingStraightSupport(double, Money)} and
     * {@link #rebuildIncludingTransportPay(double, Money)}.
     *
     * @param basePayData         the base-pay breakdown
     * @param transitPayData      the transit-pay breakdown
     * @param objectivePayDataMap per-objective payment data, keyed by objective id
     * @param totalObjectivePay   the summed payout across all objectives
     */
    public ContractPayData(ContractBasePayData basePayData, TransitPayData transitPayData,
          Map<UUID, ObjectivePayData> objectivePayDataMap, Money totalObjectivePay) {
        this(basePayData, transitPayData, objectivePayDataMap, totalObjectivePay, 0.0, Money.zero(), 0.0, Money.zero());
    }

    /**
     * Returns a copy with the straight-support fields replaced, leaving all other fields unchanged.
     *
     * @param straightSupportMultiplier the straight-support multiplier
     * @param straightSupportEstimate   the estimated straight-support payment
     *
     * @return a new {@link ContractPayData} with updated straight-support values
     */
    public ContractPayData rebuildIncludingStraightSupport(double straightSupportMultiplier,
          Money straightSupportEstimate) {
        return new ContractPayData(
              basePayData,
              transitPayData,
              objectivePayDataMap,
              totalObjectivePay,
              straightSupportMultiplier,
              straightSupportEstimate,
              transportPaymentMultiplier,
              transportPayment
        );
    }

    /**
     * Returns a copy with the transport payment fields replaced, leaving all other fields unchanged.
     *
     * @param transportPaymentMultiplier the transport payment multiplier
     * @param transportPayment           the transport payment
     *
     * @return a new {@link ContractPayData} with updated transport payment values
     */
    public ContractPayData rebuildIncludingTransportPay(double transportPaymentMultiplier, Money transportPayment) {
        return new ContractPayData(
              basePayData,
              transitPayData,
              objectivePayDataMap,
              totalObjectivePay,
              straightSupportMultiplier,
              straightSupportEstimate,
              transportPaymentMultiplier,
              transportPayment
        );
    }
}
