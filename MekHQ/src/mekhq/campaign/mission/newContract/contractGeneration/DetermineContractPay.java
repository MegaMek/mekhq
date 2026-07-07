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

import static java.lang.Math.ceil;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.jspecify.annotations.NonNull;

public class DetermineContractPay {
    /** CampOps pg 41 5th printing */
    private final static double PEACETIME_OPERATING_COSTS_PERCENTAGE = 0.75;

    public DetermineContractPay() {}

    public static ContractPayData generateContractPay(AbstractContractManager contractManager, boolean isClanCampaign,
          List<Formation> formations, Hangar hangar, CampaignOptions campaignOptions, LocalDate currentDate,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap,
          Faction campaignFaction, double reputationFactor, Collection<Unit> travelingUnits,
          boolean isOverridingCommandCircuitRequirements, boolean isGM, FactionStandings factionStandings,
          ILocation currentLocation, Collection<Part> travelingParts, Collection<Person> travelingPersonnel) {
        ContractBasePayData basePayData = calculateBasePay(isClanCampaign,
              formations,
              hangar,
              campaignOptions,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap,
              campaignFaction);

        Money calculatedBasePay = basePayData.calculatedBasePay();
        JumpPath cachedJumpPath = contractManager.getCachedJumpPathDirect();
        boolean isUseTwoWayPay = campaignOptions.isUseTwoWayPay();
        EmployerModifierData employerModifierData = contractManager.getEmployerModifierData();
        double employmentMultiplier = employerModifierData.getEmploymentMultiplier();
        String employerId = contractManager.getEmployerFactionCode();

        TravelPayData travelPay = calculateTravelPay(calculatedBasePay,
              reputationFactor,
              cachedJumpPath,
              isUseTwoWayPay,
              employmentMultiplier,
              travelingUnits,
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerId,
              currentDate,
              currentLocation,
              travelingParts,
              travelingPersonnel);
    }

    public static ContractBasePayData calculateBasePay(boolean isClanCampaign, List<Formation> formations,
          Hangar hangar, CampaignOptions campaignOptions, LocalDate currentDate, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap, Faction campaignFaction) {
        // TODO replace with campaign option
        BasePaymentMultiplier basePaymentMultiplier = BasePaymentMultiplier.NORMAL;

        Money peacetimeOperatingCosts = calculatePeacetimeOperatingCosts(isClanCampaign,
              formations,
              hangar,
              campaignOptions,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap);

        Money totalCostOfCombatUnits = calculateTotalCostOfCombatUnits(campaignOptions,
              campaignFaction,
              formations,
              hangar);

        Money calculatedBasePay = peacetimeOperatingCosts.plus(totalCostOfCombatUnits);
        calculatedBasePay = calculatedBasePay.multipliedBy(basePaymentMultiplier.getMultiplier());

        return new ContractBasePayData(peacetimeOperatingCosts, totalCostOfCombatUnits, calculatedBasePay);
    }

    private static Money calculatePeacetimeOperatingCosts(boolean isClanCampaign, List<Formation> formations,
          Hangar hangar, CampaignOptions campaignOptions, LocalDate currentDate, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap) {
        Money peacetimeOperatingCosts = Accountant.getPeacetimeOperatingCosts(formations,
              hangar,
              campaignOptions,
              isClanCampaign,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap,
              true);

        return peacetimeOperatingCosts.multipliedBy(PEACETIME_OPERATING_COSTS_PERCENTAGE);
    }

    private static Money calculateTotalCostOfCombatUnits(CampaignOptions campaignOptions, Faction campaignFaction,
          List<Formation> formations, Hangar hangar) {
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

    private @NonNull Money getObjectivePay() {
        return Money.zero()
                     .plus(basePay)
                     .multipliedBy(totalLength)
                     .multipliedBy(tempoMultiplier)
                     .multipliedBy(employmentMultiplier)
                     .multipliedBy(reputationFactor);
    }

    private static @NonNull TravelPayData calculateTravelPay(Money calculatedBasePay, double reputationFactor,
          JumpPath cachedJumpPath, boolean isUseTwoWayPay, double employmentMultiplier, Collection<Unit> travelingUnits,
          boolean isOverridingCommandCircuitRequirements, boolean isGM, FactionStandings factionStandings,
          String employerId, LocalDate currentDate, ILocation currentLocation, Collection<Part> travelingParts,
          Collection<Person> travelingPersonnel) {
        int transportPeriod = calculateTransportPeriod(cachedJumpPath, isUseTwoWayPay);

        Money transportPayment = calculateObjectiveTransportPay(travelingUnits,
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerId,
              cachedJumpPath,
              currentDate,
              currentLocation,
              travelingParts,
              travelingPersonnel);

        Money calculatedTravelPay = Money.zero()
                                          .plus(calculatedBasePay)
                                          .multipliedBy(transportPeriod)
                                          .multipliedBy(employmentMultiplier)
                                          .multipliedBy(reputationFactor)
                                          .plus(transportPayment);

        return new TravelPayData(transportPeriod, transportPayment, employmentMultiplier, reputationFactor,
              calculatedTravelPay);
    }

    public static int calculateTransportPeriod(JumpPath cachedJumpPath, boolean isUseTwoWayPay) {
        // The calculation in this method is taken from CamOps pg 41 rev 5th printing
        PlanetarySystem startSystem = cachedJumpPath.getFirstSystem();
        PlanetarySystem endSystem = cachedJumpPath.getLastSystem();

        boolean isInSameSystem = Objects.equals(startSystem, endSystem);
        if (isInSameSystem) {
            return 0;
        }

        int baseWeeks = 2;
        double additionalWeeks = 1.1 * cachedJumpPath.getJumps();
        int multiplier = isUseTwoWayPay ? 2 : 1;

        return (int) round((baseWeeks + additionalWeeks) * multiplier);
    }

    public static Money calculateObjectiveTransportPay(Collection<Unit> travelingUnits,
          boolean isOverridingCommandCircuitRequirements, boolean isGM, FactionStandings factionStandings,
          String employerId, JumpPath cachedJumpPath, LocalDate currentDate, ILocation currentLocation,
          Collection<Part> travelingParts, Collection<Person> travelingPersonnel) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerId);

        int duration = (int) ceil(cachedJumpPath.getTotalTime(currentDate, currentLocation.getTransitTime(),
              isUseCommandCircuit));

        TransportCostCalculations costCalculation = new TransportCostCalculations(travelingUnits,
              travelingParts,
              travelingPersonnel,
              EXP_REGULAR);
        return costCalculation.calculateJumpCostForEntireJourney(duration, cachedJumpPath.getJumps());
    }
}
