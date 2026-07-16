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

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import mekhq.campaign.JumpPath;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.mission.newContract.AbstractContractObjective;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.jspecify.annotations.NonNull;

/**
 * Calculates the payment terms of a generated contract &mdash; base pay, transit pay, per-objective pay, straight
 * support, and transport payment &mdash; following CamOps pg 41 (rev 5th printing). This is a static utility class and
 * is not instantiable.
 */
public class ContractDeterminationPay {
    /** CampOps pg 41 5th printing */
    private final static double PEACETIME_OPERATING_COSTS_PERCENTAGE = 0.75;

    private ContractDeterminationPay() {}

    /**
     * Generates the full payment breakdown for a contract, combining base pay, transit pay, and per-objective pay.
     *
     * @param contractManager         the contract manager, providing the jump path, employer modifiers, and objectives
     * @param formations              the campaign's formations
     * @param localHangar             the campaign's localHangar
     * @param campaignOptions         the campaign options governing pay calculations
     * @param currentDate             the current campaign date
     * @param temporaryAsTechPoolSize the temporary astech pool size, factored into operating costs
     * @param temporaryMedicPool      the temporary medic pool size, factored into operating costs
     * @param temporaryCrewMap        temporary crew counts by role, factored into operating costs
     * @param campaignFaction         the campaign faction (its Clan status affects operating costs)
     * @param reputationFactor        the force's reputation factor
     *
     * @return the assembled contract payment data
     */
    public static ContractPayData generateContractPay(AbstractContractManager contractManager,
          List<Formation> formations, LocalHangar localHangar, CampaignOptions campaignOptions, LocalDate currentDate,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap,
          Faction campaignFaction, double reputationFactor) {
        boolean isClanCampaign = campaignFaction.isClan();

        ContractBasePayData basePayData = calculateBasePay(isClanCampaign,
              formations,
              localHangar,
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

        TransitPayData transitPayData = calculateTransitPay(calculatedBasePay,
              reputationFactor,
              cachedJumpPath,
              isUseTwoWayPay,
              employmentMultiplier);

        List<AbstractContractObjective> objectives = contractManager.getContractAllObjectivesCopy();
        Map<UUID, ObjectivePayData> objectivePayDataMap = calculateObjectivePay(objectives,
              calculatedBasePay,
              employmentMultiplier,
              reputationFactor);

        Money totalObjectivePay = getTotalObjectivePay(objectivePayDataMap);

        return new ContractPayData(basePayData, transitPayData, objectivePayDataMap, totalObjectivePay);
    }

    private static Money getTotalObjectivePay(Map<UUID, ObjectivePayData> objectivePayDataMap) {
        Money totalObjectivePay = Money.zero();
        for (ObjectivePayData objectivePayData : objectivePayDataMap.values()) {
            Money calculatedObjectivePay = objectivePayData.calculatedObjectivePay();
            totalObjectivePay = totalObjectivePay.plus(calculatedObjectivePay);
        }

        return totalObjectivePay;
    }

    /**
     * Calculates a contract's base pay: scaled peacetime operating costs plus the assessed combat-unit value, times the
     * base-payment multiplier.
     *
     * @param isClanCampaign          whether the campaign is a Clan campaign
     * @param formations              the campaign's formations
     * @param localHangar             the campaign's localHangar
     * @param campaignOptions         the campaign options governing pay calculations
     * @param currentDate             the current campaign date
     * @param temporaryAsTechPoolSize the temporary astech pool size, factored into operating costs
     * @param temporaryMedicPool      the temporary medic pool size, factored into operating costs
     * @param temporaryCrewMap        temporary crew counts by role, factored into operating costs
     * @param campaignFaction         the campaign faction
     *
     * @return the base-pay breakdown
     */
    public static ContractBasePayData calculateBasePay(boolean isClanCampaign, List<Formation> formations,
          LocalHangar localHangar, CampaignOptions campaignOptions, LocalDate currentDate, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap, Faction campaignFaction) {
        // TODO replace with campaign option
        BasePaymentMultiplier basePaymentMultiplier = BasePaymentMultiplier.NORMAL;

        Money peacetimeOperatingCosts = calculatePeacetimeOperatingCostsIncludingMultiplier(isClanCampaign,
              formations,
              localHangar,
              campaignOptions,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap);

        Money totalCostOfCombatUnits = calculateTotalCostOfCombatUnits(campaignOptions,
              campaignFaction,
              formations,
              localHangar);

        Money calculatedBasePay = peacetimeOperatingCosts.plus(totalCostOfCombatUnits);
        calculatedBasePay = calculatedBasePay.multipliedBy(basePaymentMultiplier.getMultiplier());

        return new ContractBasePayData(peacetimeOperatingCosts, totalCostOfCombatUnits, calculatedBasePay);
    }

    private static Money calculatePeacetimeOperatingCostsIncludingMultiplier(boolean isClanCampaign,
          List<Formation> formations,
          LocalHangar localHangar, CampaignOptions campaignOptions, LocalDate currentDate, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap) {
        Money peacetimeOperatingCosts = Accountant.getPeacetimeOperatingCosts(formations,
              localHangar,
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
          List<Formation> formations, LocalHangar localHangar) {
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
                  localHangar,
                  useDiminishingContractPay,
                  excludeInfantry,
                  combatUnitContractPercent,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent);
        } else {
            forceValue = Accountant.getForceValue(formations,
                  localHangar,
                  campaignFaction,
                  campaignOptions,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);
        }

        return forceValue;
    }

    private static @NonNull TransitPayData calculateTransitPay(Money calculatedBasePay, double reputationFactor,
          JumpPath cachedJumpPath, boolean isUseTwoWayPay, double employmentMultiplier) {
        int transportPeriod = calculateTransportPeriod(cachedJumpPath, isUseTwoWayPay);

        Money calculatedTransitPay = Money.zero()
                                           .plus(calculatedBasePay)
                                           .multipliedBy(transportPeriod)
                                           .multipliedBy(employmentMultiplier)
                                           .multipliedBy(reputationFactor);

        return new TransitPayData(calculatedBasePay, transportPeriod, employmentMultiplier, reputationFactor,
              calculatedTransitPay);
    }

    /**
     * Calculates the transport period, in weeks, for a contract's jump path (CamOps pg 41, rev 5th printing). A journey
     * that begins and ends in the same system has a period of zero.
     *
     * @param cachedJumpPath the jump path from the current location to the contract's target
     * @param isUseTwoWayPay whether transport pay covers the return journey, doubling the period
     *
     * @return the transport period in weeks
     */
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

    private static @NonNull Map<UUID, ObjectivePayData> calculateObjectivePay(
          List<AbstractContractObjective> objectives, Money calculatedBasePay,
          double employmentMultiplier, double reputationFactor) {
        Map<UUID, ObjectivePayData> objectivePayMap = new HashMap<>();

        for (AbstractContractObjective objective : objectives) {
            int objectiveLength = objective.getLengthInMonths();
            double tempoMultiplier = objective.getObjectiveType().getOperationsTempoMultiplier();

            Money calculatedObjectivePay = Money.zero()
                                                 .plus(calculatedBasePay)
                                                 .multipliedBy(objectiveLength)
                                                 .multipliedBy(tempoMultiplier)
                                                 .multipliedBy(employmentMultiplier)
                                                 .multipliedBy(reputationFactor);

            ObjectivePayData objectivePayData = new ObjectivePayData(calculatedBasePay,
                  objectiveLength,
                  tempoMultiplier,
                  employmentMultiplier,
                  reputationFactor,
                  calculatedObjectivePay);

            objectivePayMap.put(objective.getId(), objectivePayData);
        }

        return objectivePayMap;
    }

    /**
     * Calculates the actual transport payment for moving a force along a jump path, accounting for command-circuit use
     * and the cost of the traveling units, parts, and personnel.
     *
     * @param isOverridingCommandCircuitRequirements whether command-circuit requirements are being overridden
     * @param isGM                                   whether the action is being taken by a GM
     * @param factionStandings                       the campaign's faction standings, consulted for command-circuit
     *                                               eligibility
     * @param employerCode                           the employer faction's short code
     * @param jumpPath                               the jump path to be traveled
     * @param currentDate                            the current campaign date
     * @param currentLocation                        the campaign's current location, providing transit time
     * @param travelingUnits                         the units being transported
     * @param travelingParts                         the spare parts being transported
     * @param travelingPersonnel                     the personnel being transported
     * @param jumpShipCrewExperienceLevel            the experience level of the jump-ship crew
     *
     * @return the total transport payment for the journey
     */
    public static Money determineTransportPayment(boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode, JumpPath jumpPath, LocalDate currentDate,
          ILocation currentLocation, Collection<Unit> travelingUnits, Collection<Part> travelingParts,
          Collection<Person> travelingPersonnel, int jumpShipCrewExperienceLevel) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);

        int duration = (int) ceil(jumpPath.getTotalTime(currentDate, currentLocation.getTransitTime(),
              isUseCommandCircuit));

        TransportCostCalculations costCalculation = new TransportCostCalculations(travelingUnits,
              travelingParts,
              travelingPersonnel,
              jumpShipCrewExperienceLevel);

        return costCalculation.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());
    }

    /**
     * Calculates the straight-support payment: the force's peacetime operating costs scaled by the negotiated
     * straight-support multiplier.
     *
     * @param isClanCampaign            whether the campaign is a Clan campaign
     * @param formations                the campaign's formations
     * @param localHangar               the campaign's localHangar
     * @param campaignOptions           the campaign options governing operating-cost calculations
     * @param currentDate               the current campaign date
     * @param temporaryAsTechPoolSize   the temporary astech pool size, factored into operating costs
     * @param temporaryMedicPool        the temporary medic pool size, factored into operating costs
     * @param temporaryCrewMap          temporary crew counts by role, factored into operating costs
     * @param straightSupportMultiplier the negotiated straight-support multiplier
     *
     * @return the straight-support payment
     */
    public static Money determineStraightSupport(boolean isClanCampaign, List<Formation> formations,
          LocalHangar localHangar, CampaignOptions campaignOptions, LocalDate currentDate, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap, double straightSupportMultiplier) {
        Money peacetimeOperatingCosts = Accountant.getPeacetimeOperatingCosts(formations,
              localHangar,
              campaignOptions,
              isClanCampaign,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap,
              true);

        return peacetimeOperatingCosts.multipliedBy(straightSupportMultiplier);
    }
}
