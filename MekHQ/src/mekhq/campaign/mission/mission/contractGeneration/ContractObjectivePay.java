package mekhq.campaign.mission.mission.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractObjectivePay {
    /** CampOps pg 41 5th printing */
    private final static double PEACETIME_OPERATING_COSTS_PERCENTAGE = 0.75;

    // Base Pay
    private Money peacetimeOperatingCosts;
    private Money totalCostOfCombatUnits;
    private Money basePay;

    // Transport
    private Money transportPayment;

    public ContractObjectivePay(CampaignOptions campaignOptions, Faction campaignFaction, LocalDate today,
          Hangar hangar, final Collection<Part> spareParts, final Collection<Person> allPersonnel,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap,
          List<Formation> formations, BasePaymentMultiplier basePaymentMultiplier, ILocation currentLocation,
          JumpPath jumpPath, boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode) {
        calculateBasePay(campaignOptions,
              campaignFaction,
              today,
              hangar,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              formations,
              basePaymentMultiplier);

        calculateObjectiveTransportPay(today,
              hangar,
              spareParts,
              allPersonnel,
              currentLocation,
              jumpPath,
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);
    }

    public void calculateBasePay(CampaignOptions campaignOptions, Faction campaignFaction, LocalDate today,
          Hangar hangar, int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap,
          List<Formation> formations, BasePaymentMultiplier basePaymentMultiplier) {
        peacetimeOperatingCosts = calculatePeacetimeOperatingCosts(formations,
              hangar,
              campaignOptions,
              campaignFaction.isClan(),
              today,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap);

        totalCostOfCombatUnits = calculateTotalCostOfCombatUnits(campaignOptions,
              campaignFaction,
              hangar,
              formations, basePaymentMultiplier);

        basePay = peacetimeOperatingCosts.plus(totalCostOfCombatUnits);
    }

    private Money calculatePeacetimeOperatingCosts(Collection<Formation> formations, Hangar hangar,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap) {
        Money peacetimeOperatingCosts = Accountant.getPeacetimeOperatingCosts(formations, hangar, campaignOptions,
              isClanCampaign, today, temporaryAsTechPoolSize, temporaryMedicPool, tempCrewMap, true);

        return peacetimeOperatingCosts.multipliedBy(PEACETIME_OPERATING_COSTS_PERCENTAGE);
    }

    private Money calculateTotalCostOfCombatUnits(CampaignOptions campaignOptions, Faction campaignFaction,
          Hangar hangar, List<Formation> formations, BasePaymentMultiplier basePaymentMultiplier) {
        final boolean excludeInfantry = campaignOptions.isInfantryDontCount();
        final double combatUnitContractPercent = campaignOptions.getEquipmentContractPercent();
        final double dropShipContractPercent = campaignOptions.getDropShipContractPercent();
        final double warShipContractPercent = campaignOptions.getWarShipContractPercent();
        final double jumpShipContractPercent = campaignOptions.getJumpShipContractPercent();
        final boolean useEquipmentSellValue = campaignOptions.isEquipmentContractSaleValue();
        final boolean useDiminishingContractPay = campaignOptions.isUseDiminishingContractPay();

        Money forceValue;
        if (campaignOptions.isUseAlternatePaymentMode()) {
            forceValue = AlternatePaymentModelValues.getForceValue(campaignFaction,
                  formations,
                  hangar,
                  useDiminishingContractPay,
                  excludeInfantry,
                  combatUnitContractPercent,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent);
        } else {
            forceValue = Accountant.getForceValue(formations,
                  hangar,
                  campaignFaction,
                  campaignOptions,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);
        }

        double basePaymentMultiplierValue = basePaymentMultiplier.getMultiplier();
        return forceValue.multipliedBy(basePaymentMultiplierValue);
    }

    public void calculateObjectiveTransportPay(LocalDate today, Hangar hangar,
          final Collection<Part> spareParts, final Collection<Person> allPersonnel, ILocation currentLocation,
          JumpPath jumpPath, boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);

        int duration = (int) ceil(jumpPath.getTotalTime(today, currentLocation.getTransitTime(),
              isUseCommandCircuit));

        TransportCostCalculations costCalculation = new TransportCostCalculations(hangar,
              spareParts,
              allPersonnel,
              EXP_REGULAR);
        transportPayment = costCalculation.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());
    }
}
