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
package mekhq.campaign.mission.mission.contractGeneration;

import static java.lang.Math.ceil;
import static java.lang.Math.round;
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
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.jspecify.annotations.NonNull;

public class ContractObjectivePay {
    /** CampOps pg 41 5th printing */
    private final static double PEACETIME_OPERATING_COSTS_PERCENTAGE = 0.75;

    private final CampaignOptions campaignOptions;
    private final Faction campaignFaction;
    private final LocalDate today;
    private final Hangar hangar;
    private final Collection<Part> spareParts;
    private final Collection<Person> allPersonnel;
    private final int temporaryAsTechPoolSize;
    private final int temporaryMedicPool;
    private final Map<PersonnelRole, Integer> tempCrewMap;
    private final List<Formation> formations;
    private final BasePaymentMultiplier basePaymentMultiplier;
    private final ILocation currentLocation;
    private final JumpPath jumpPath;
    private final boolean isOverridingCommandCircuitRequirements;
    private final boolean isGM;
    private final FactionStandings factionStandings;
    private final String employerCode;
    private final double tempoMultiplier;
    private final double employmentMultiplier;
    private final double reputationFactor;

    // Base Pay
    private Money peacetimeOperatingCosts;
    private Money totalCostOfCombatUnits;
    private Money basePay;

    // Length
    private final int lengthOfObjective;
    private int transportPeriod;
    private int totalLength;

    // Transport
    private Money transportPayment;

    public ContractObjectivePay(CampaignOptions campaignOptions, Faction campaignFaction, LocalDate today,
          Hangar hangar, final Collection<Part> spareParts, final Collection<Person> allPersonnel,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap,
          List<Formation> formations, BasePaymentMultiplier basePaymentMultiplier, ILocation currentLocation,
          JumpPath jumpPath, boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode, int lengthOfObjective, double tempoMultiplier,
          double employmentMultiplier, double reputationFactor) {
        this.campaignOptions = campaignOptions;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.hangar = hangar;
        this.spareParts = spareParts;
        this.allPersonnel = allPersonnel;
        this.temporaryAsTechPoolSize = temporaryAsTechPoolSize;
        this.temporaryMedicPool = temporaryMedicPool;
        this.tempCrewMap = tempCrewMap;
        this.formations = formations;
        this.basePaymentMultiplier = basePaymentMultiplier;
        this.currentLocation = currentLocation;
        this.jumpPath = jumpPath;
        this.isOverridingCommandCircuitRequirements = isOverridingCommandCircuitRequirements;
        this.isGM = isGM;
        this.factionStandings = factionStandings;
        this.employerCode = employerCode;
        this.lengthOfObjective = lengthOfObjective;
        this.tempoMultiplier = tempoMultiplier;
        this.employmentMultiplier = employmentMultiplier;
        this.reputationFactor = reputationFactor;
    }

    private @NonNull Money getObjectivePay() {
        return Money.zero()
                     .plus(basePay)
                     .multipliedBy(totalLength)
                     .multipliedBy(tempoMultiplier)
                     .multipliedBy(employmentMultiplier)
                     .multipliedBy(reputationFactor);
    }

    private @NonNull Money travelPay() {
        return Money.zero().plus(basePay)
                     .multipliedBy(transportPeriod)
                     .multipliedBy(employmentMultiplier)
                     .multipliedBy(reputationFactor)
                     .plus(transportPayment);
    }

    public void calculateBasePay() {
        peacetimeOperatingCosts = calculatePeacetimeOperatingCosts(campaignFaction.isClan());
        totalCostOfCombatUnits = calculateTotalCostOfCombatUnits();

        basePay = peacetimeOperatingCosts.plus(totalCostOfCombatUnits);
    }

    private Money calculatePeacetimeOperatingCosts(boolean isClanCampaign) {
        Money peacetimeOperatingCosts = Accountant.getPeacetimeOperatingCosts(formations, hangar, campaignOptions,
              isClanCampaign, today, temporaryAsTechPoolSize, temporaryMedicPool, tempCrewMap, true);

        return peacetimeOperatingCosts.multipliedBy(PEACETIME_OPERATING_COSTS_PERCENTAGE);
    }

    private Money calculateTotalCostOfCombatUnits() {
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

    public void calculateLengthOfMission() {
        // The calculation in this method is taken from CamOps pg 41 rev 5th printing
        PlanetarySystem startSystem = jumpPath.getFirstSystem();
        PlanetarySystem endSystem = jumpPath.getLastSystem();

        boolean isInSameSystem = startSystem.equals(endSystem);
        if (isInSameSystem) {
            transportPeriod = 0;
            return;
        }

        int baseWeeks = 2;
        double additionalWeeks = 1.1 * jumpPath.getJumps();
        int multiplier = 2;

        transportPeriod = (int) round((baseWeeks + additionalWeeks) * multiplier);

        totalLength = transportPeriod + lengthOfObjective;
    }

    public void calculateObjectiveTransportPay(Collection<Unit> units) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);

        int duration = (int) ceil(jumpPath.getTotalTime(today, currentLocation.getTransitTime(),
              isUseCommandCircuit));

        TransportCostCalculations costCalculation = new TransportCostCalculations(units,
              spareParts,
              allPersonnel,
              EXP_REGULAR);
        transportPayment = costCalculation.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());
    }
}
