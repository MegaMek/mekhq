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

/**
 * Base class for the contract pay schemes: given a generated contract, each scheme decides how much the employer pays
 * as a monthly retainer, a per-battle combat bonus, and up-front transport compensation.
 *
 * <p>Two schemes exist. The default {@link ChaosContractDeterminationPay} derives pay from the contract's abstract
 * scale and support-point multipliers; {@link CamOpsContractDeterminationPay} grounds the monthly retainer in the
 * Campaign Operations force-value calculation instead. A campaign opts into the CamOps scheme with
 * {@link CampaignOption#USE_LEGACY_CONTRACT_PAY}; use {@link #forCampaign(Campaign)} to obtain the scheme it has
 * chosen.</p>
 *
 * <p>Both schemes share the transport-pay calculation, so it lives here rather than in either subclass.</p>
 *
 * @see ChaosContractDeterminationPay
 * @see CamOpsContractDeterminationPay
 */
public abstract class AbstractContractDeterminationPay {
    public final static int DEFAULT_TRANSPORT_COST_MULTIPLIER = 50; // Draconis Reach first printing pg 26
    public final static int HIRING_HALL_RETURN_MULTIPLIER = 2; // Draconis Reach first printing pg 26

    /**
     * The deniability premium a false flag operation pays on its retainer and combat bonus, over the going rate for the
     * same job. Deliberately modest: it is meant to read as "suspiciously generous" to an attentive player rather than
     * to announce itself, the only in-market tell that a contract is a false flag.
     */
    private static final double FALSE_FLAG_PAY_PREMIUM = 1.25;

    /**
     * Returns the pay scheme the campaign has opted into: the CamOps force-value scheme when
     * {@link CampaignOption#USE_LEGACY_CONTRACT_PAY} is set, otherwise the default Chaos scheme.
     */
    public static @NonNull AbstractContractDeterminationPay forCampaign(Campaign campaign) {
        if (campaign.getCampaignOptions().get(CampaignOption.USE_LEGACY_CONTRACT_PAY)) {
            return new CamOpsContractDeterminationPay();
        }
        return new ChaosContractDeterminationPay();
    }

    /**
     * Populates the contract's {@link ContractFinanceData} from this scheme's three pay components: the monthly
     * retainer, the per-battle combat bonus, and up-front transport compensation.
     */
    public void determineContractPay(Campaign campaign, LocalDate currentDate, AbstractContract contract,
          AbstractLocation currentLocation) {
        Money monthlyPay = getMonthlyPay(campaign, contract);
        Money combatPay = getCombatPay(campaign, contract);
        Money transportPay = getTransportPay(campaign, currentDate, contract, currentLocation);

        // A false flag operation quietly pays a deniability premium on the retainer and combat bonus - the "too
        // generous for this job" tell an attentive player can learn to notice. Transport is cost reimbursement, so it
        // is left alone. Applies to whichever pay scheme is in effect, since both route through here.
        if (contract.isFalseFlag()) {
            monthlyPay = monthlyPay.multipliedBy(FALSE_FLAG_PAY_PREMIUM);
            combatPay = combatPay.multipliedBy(FALSE_FLAG_PAY_PREMIUM);
        }

        ContractFinanceData contractFinanceData = new ContractFinanceData(transportPay, monthlyPay, combatPay);
        contract.setContractFinanceData(contractFinanceData);
    }

    /** The monthly retainer the employer pays. */
    public abstract @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract);

    /** The per-battle combat bonus the employer pays; some schemes fold this into the monthly retainer and return zero. */
    public abstract @NonNull Money getCombatPay(Campaign campaign, AbstractContract contract);

    /**
     * Up-front transport compensation for the journey from the player's current location to the contract's target,
     * derived from the contract's scale, transport terms, and jump count. Shared by every pay scheme.
     */
    public @NonNull Money getTransportPay(Campaign campaign, LocalDate currentDate, AbstractContract contract,
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

    /**
     * Whether Chaos support-point pay should be converted to C-bills (the "BSP to BV" conversion). Enabled by default;
     * when disabled, pay is expressed in raw support points.
     */
    protected static boolean shouldConvertSupportPoints(Campaign campaign) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION);
    }
}
