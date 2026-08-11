package mekhq.campaign.mission.newContract.contractGeneration.targetFinder;

import static java.lang.Math.round;

import java.time.LocalDate;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.mission.newContract.ContractUtilities;
import mekhq.campaign.mission.newContract.contractData.ContractFinanceData;
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
        Money monthlyPay = getMonthlyPay(contract);
        Money combatPay = getCombatPay(contract);
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

        boolean isAtHiringHall = currentSystem.isHiringHall(currentDate);
        if (isAtHiringHall) {
            transportCostInSupportPoints *= HIRING_HALL_RETURN_MULTIPLIER;
        }

        transportCostInSupportPoints = (int) round(transportCostInSupportPoints * contract.getTransportMultiplier());
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(transportCostInSupportPoints);
    }

    public static @NonNull Money getCombatPay(AbstractContract contract) {
        int combatPayInSupportPoints = DEFAULT_COMBAT_PAY_MULTIPLIER * contract.getScale();
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(combatPayInSupportPoints);
    }

    public static @NonNull Money getMonthlyPay(AbstractContract contract) {
        int monthlyPayInSupportPoints = DEFAULT_MONTHLY_PAY_MULTIPLIER * contract.getScale();
        monthlyPayInSupportPoints = (int) round(monthlyPayInSupportPoints * contract.getBasePayMultiplier());
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(monthlyPayInSupportPoints);
    }
}
