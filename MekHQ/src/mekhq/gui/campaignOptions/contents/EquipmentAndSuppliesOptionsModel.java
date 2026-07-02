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
package mekhq.gui.campaignOptions.contents;

import jakarta.annotation.Nonnull;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;

class EquipmentAndSuppliesOptionsModel {
    AcquisitionsType acquisitionType;
    boolean useFunctionalAppraisal;
    ProcurementPersonnelPick acquisitionPersonnelCategory;
    int clanAcquisitionPenalty;
    int isAcquisitionPenalty;
    int waitingPeriod;
    int maxAcquisitions;
    int autoLogisticsMekHead;
    int autoLogisticsMekLocation;
    int autoLogisticsNonRepairableLocation;
    int autoLogisticsArmor;
    int autoLogisticsAmmunition;
    int autoLogisticsActuators;
    int autoLogisticsJumpJets;
    int autoLogisticsHeadComponents;
    int autoLogisticsEngines;
    int autoLogisticsGyros;
    int autoLogisticsHeatSink;
    int autoLogisticsWeapons;
    int autoLogisticsOther;
    int unitTransitTime;
    boolean noDeliveriesInTransit;
    boolean usePlanetaryAcquisition;
    int maxJumpsPlanetaryAcquisition;
    PlanetaryAcquisitionFactionLimit planetAcquisitionFactionLimit;
    boolean disallowPlanetAcquisitionClanCrossover;
    boolean noClanPartsFromIS;
    int penaltyClanPartsFromIS;
    boolean planetAcquisitionVerbose;
    final int[] planetTechAcquisitionBonus = new int[PlanetarySophistication.values().length];
    final int[] planetIndustryAcquisitionBonus = new int[PlanetaryRating.values().length];
    final int[] planetOutputAcquisitionBonus = new int[PlanetaryRating.values().length];
    boolean limitByYear;
    boolean disallowExtinctStuff;
    boolean allowClanPurchases;
    boolean allowISPurchases;
    boolean allowCanonOnly;
    boolean allowCanonRefitOnly;
    int techLevel;
    boolean variableTechLevel;
    boolean useAmmoByType;

    EquipmentAndSuppliesOptionsModel(@Nonnull CampaignOptions options) {
        acquisitionType = options.getAcquisitionType();
        useFunctionalAppraisal = options.isUseFunctionalAppraisal();
        acquisitionPersonnelCategory = options.getAcquisitionPersonnelCategory();
        clanAcquisitionPenalty = options.getClanAcquisitionPenalty();
        isAcquisitionPenalty = options.getIsAcquisitionPenalty();
        waitingPeriod = options.getWaitingPeriod();
        maxAcquisitions = options.getMaxAcquisitions();
        autoLogisticsMekHead = options.getAutoLogisticsMekHead();
        autoLogisticsMekLocation = options.getAutoLogisticsMekLocation();
        autoLogisticsNonRepairableLocation = options.getAutoLogisticsNonRepairableLocation();
        autoLogisticsArmor = options.getAutoLogisticsArmor();
        autoLogisticsAmmunition = options.getAutoLogisticsAmmunition();
        autoLogisticsActuators = options.getAutoLogisticsActuators();
        autoLogisticsJumpJets = options.getAutoLogisticsJumpJets();
        autoLogisticsHeadComponents = options.getAutoLogisticsHeadComponents();
        autoLogisticsEngines = options.getAutoLogisticsEngines();
        autoLogisticsGyros = options.getAutoLogisticsGyros();
        autoLogisticsHeatSink = options.getAutoLogisticsHeatSink();
        autoLogisticsWeapons = options.getAutoLogisticsWeapons();
        autoLogisticsOther = options.getAutoLogisticsOther();
        unitTransitTime = options.getUnitTransitTime();
        noDeliveriesInTransit = options.isNoDeliveriesInTransit();
        usePlanetaryAcquisition = options.isUsePlanetaryAcquisition();
        maxJumpsPlanetaryAcquisition = options.getMaxJumpsPlanetaryAcquisition();
        planetAcquisitionFactionLimit = options.getPlanetAcquisitionFactionLimit();
        disallowPlanetAcquisitionClanCrossover = options.isPlanetAcquisitionNoClanCrossover();
        noClanPartsFromIS = options.isNoClanPartsFromIS();
        penaltyClanPartsFromIS = options.getPenaltyClanPartsFromIS();
        planetAcquisitionVerbose = options.isPlanetAcquisitionVerbose();

        int index = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            planetTechAcquisitionBonus[index] = options.getPlanetTechAcquisitionBonus(sophistication);
            index++;
        }
        index = 0;
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            planetIndustryAcquisitionBonus[index] = options.getPlanetIndustryAcquisitionBonus(rating);
            planetOutputAcquisitionBonus[index] = options.getPlanetOutputAcquisitionBonus(rating);
            index++;
        }

        limitByYear = options.isLimitByYear();
        disallowExtinctStuff = options.isDisallowExtinctStuff();
        allowClanPurchases = options.isAllowClanPurchases();
        allowISPurchases = options.isAllowISPurchases();
        allowCanonOnly = options.isAllowCanonOnly();
        allowCanonRefitOnly = options.isAllowCanonRefitOnly();
        techLevel = options.getTechLevel();
        variableTechLevel = options.isVariableTechLevel();
        useAmmoByType = options.isUseAmmoByType();
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.setAcquisitionType(acquisitionType);
        options.setUseFunctionalAppraisal(useFunctionalAppraisal);
        options.setAcquisitionPersonnelCategory(acquisitionPersonnelCategory);
        options.setClanAcquisitionPenalty(clanAcquisitionPenalty);
        options.setIsAcquisitionPenalty(isAcquisitionPenalty);
        options.setWaitingPeriod(waitingPeriod);
        options.setMaxAcquisitions(maxAcquisitions);
        options.setAutoLogisticsMekHead(autoLogisticsMekHead);
        options.setAutoLogisticsMekLocation(autoLogisticsMekLocation);
        options.setAutoLogisticsNonRepairableLocation(autoLogisticsNonRepairableLocation);
        options.setAutoLogisticsArmor(autoLogisticsArmor);
        options.setAutoLogisticsAmmunition(autoLogisticsAmmunition);
        options.setAutoLogisticsActuators(autoLogisticsActuators);
        options.setAutoLogisticsJumpJets(autoLogisticsJumpJets);
        options.setAutoLogisticsHeadComponents(autoLogisticsHeadComponents);
        options.setAutoLogisticsEngines(autoLogisticsEngines);
        options.setAutoLogisticsGyros(autoLogisticsGyros);
        options.setAutoLogisticsHeatSink(autoLogisticsHeatSink);
        options.setAutoLogisticsWeapons(autoLogisticsWeapons);
        options.setAutoLogisticsOther(autoLogisticsOther);
        options.setUnitTransitTime(unitTransitTime);
        options.setNoDeliveriesInTransit(noDeliveriesInTransit);
        options.setPlanetaryAcquisition(usePlanetaryAcquisition);
        options.setMaxJumpsPlanetaryAcquisition(maxJumpsPlanetaryAcquisition);
        options.setPlanetAcquisitionFactionLimit(planetAcquisitionFactionLimit);
        options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetAcquisitionClanCrossover);
        options.setDisallowClanPartsFromIS(noClanPartsFromIS);
        options.setPenaltyClanPartsFromIS(penaltyClanPartsFromIS);
        options.setPlanetAcquisitionVerboseReporting(planetAcquisitionVerbose);

        int index = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            options.setPlanetTechAcquisitionBonus(planetTechAcquisitionBonus[index], sophistication);
            index++;
        }
        index = 0;
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            options.setPlanetIndustryAcquisitionBonus(planetIndustryAcquisitionBonus[index], rating);
            options.setPlanetOutputAcquisitionBonus(planetOutputAcquisitionBonus[index], rating);
            index++;
        }

        options.setLimitByYear(limitByYear);
        options.setDisallowExtinctStuff(disallowExtinctStuff);
        options.setAllowClanPurchases(allowClanPurchases);
        options.setAllowISPurchases(allowISPurchases);
        options.setAllowCanonOnly(allowCanonOnly);
        options.setAllowCanonRefitOnly(allowCanonRefitOnly);
        options.setTechLevel(techLevel);
        options.setVariableTechLevel(variableTechLevel);
        options.setUseAmmoByType(useAmmoByType);
    }
}
