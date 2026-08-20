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

import static java.lang.Math.round;

import java.time.LocalDate;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.PlanetarySystem;
import org.jspecify.annotations.NonNull;

public class ChaosContractPayDetermination {
    public final static int DEFAULT_MONTHLY_PAY_MULTIPLIER = 500; // Draconis Reach first printing pg 26
    public final static int DEFAULT_COMBAT_PAY_MULTIPLIER = 500; // Draconis Reach first printing pg 26
    public final static int DEFAULT_TRANSPORT_COST_MULTIPLIER = 50; // Draconis Reach first printing pg 26
    public final static int HIRING_HALL_RETURN_MULTIPLIER = 2; // Draconis Reach first printing pg 26

    public static void determineContractPayForChaosContract(Campaign campaign, LocalDate currentDate,
          AbstractContract contract,
          AbstractLocation currentLocation) {
        Money monthlyPay = getMonthlyPay(campaign, contract);
        Money combatPay = getCombatPay(campaign, contract);
        Money transportPay = getTransportPay(campaign, currentDate, contract, currentLocation);

        ContractFinanceData contractFinanceData = new ContractFinanceData(transportPay, monthlyPay, combatPay);
        contract.setContractFinanceData(contractFinanceData);
    }

    public static @NonNull Money getTransportPay(Campaign campaign, LocalDate currentDate, AbstractContract contract,
          AbstractLocation currentLocation) {
        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();

        JumpPath cachedJumpPath = ContractUtilities.getJumpPath(campaign, contract, currentLocation);
        int jumpCount = cachedJumpPath == null ? 0 : cachedJumpPath.getJumps();

        if (jumpCount == 0) {
            return Money.zero();
        }

        int transportCostInSupportPoints = DEFAULT_TRANSPORT_COST_MULTIPLIER * contract.getScale();
        transportCostInSupportPoints *= jumpCount;

        // Two-way pay for hiring halls: when the player sets out from a hiring hall, the employer covers the return
        // journey too, doubling transport pay. Only applied when the campaign opts into it.
        boolean isAtHiringHall = currentSystem.isHiringHall(currentDate);
        boolean useTwoWayPayForHiringHalls = campaign.getCampaignOptions().get(CampaignOption.IS_USE_TWO_WAY_PAY);
        if (isAtHiringHall && useTwoWayPayForHiringHalls) {
            transportCostInSupportPoints *= HIRING_HALL_RETURN_MULTIPLIER;
        }

        transportCostInSupportPoints = (int) round(transportCostInSupportPoints * contract.getTransportMultiplier());
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(transportCostInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }

    public static @NonNull Money getCombatPay(Campaign campaign, AbstractContract contract) {
        int combatPayInSupportPoints = DEFAULT_COMBAT_PAY_MULTIPLIER * contract.getScale();
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(combatPayInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }

    public static @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
        int monthlyPayInSupportPoints = DEFAULT_MONTHLY_PAY_MULTIPLIER * contract.getScale();
        monthlyPayInSupportPoints = (int) round(monthlyPayInSupportPoints * contract.getBasePayMultiplier());
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(monthlyPayInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }

    /**
     * Whether Chaos support-point pay should be converted to C-bills (the "BSP to BV" conversion). Enabled by default;
     * when disabled, pay is expressed in raw support points.
     */
    private static boolean shouldConvertSupportPoints(Campaign campaign) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION);
    }
}
