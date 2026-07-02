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
