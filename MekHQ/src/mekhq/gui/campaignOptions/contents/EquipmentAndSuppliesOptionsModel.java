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
import mekhq.campaign.campaignOptions.CampaignOption;
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
    boolean limitClanTech;
    String mekWarriorDefaultKit;
    String vehicleCrewDefaultKit;
    String aircraftDefaultKit;
    boolean addDefaultKitToProcurement;
    boolean npcFactionArmorKits;

    EquipmentAndSuppliesOptionsModel(@Nonnull CampaignOptions options) {
        acquisitionType = options.get(CampaignOption.ACQUISITIONS_TYPE);
        useFunctionalAppraisal = options.get(CampaignOption.USE_FUNCTIONAL_APPRAISAL);
        acquisitionPersonnelCategory = options.get(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY);
        clanAcquisitionPenalty = options.get(CampaignOption.CLAN_ACQUISITION_PENALTY);
        isAcquisitionPenalty = options.get(CampaignOption.IS_ACQUISITION_PENALTY);
        waitingPeriod = options.get(CampaignOption.WAITING_PERIOD);
        maxAcquisitions = options.get(CampaignOption.MAX_ACQUISITIONS);
        autoLogisticsMekHead = options.get(CampaignOption.AUTO_LOGISTICS_MEK_HEAD);
        autoLogisticsMekLocation = options.get(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION);
        autoLogisticsNonRepairableLocation = options.get(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION);
        autoLogisticsArmor = options.get(CampaignOption.AUTO_LOGISTICS_ARMOR);
        autoLogisticsAmmunition = options.get(CampaignOption.AUTO_LOGISTICS_AMMUNITION);
        autoLogisticsActuators = options.get(CampaignOption.AUTO_LOGISTICS_ACTUATORS);
        autoLogisticsJumpJets = options.get(CampaignOption.AUTO_LOGISTICS_JUMP_JETS);
        autoLogisticsHeadComponents = options.get(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS);
        autoLogisticsEngines = options.get(CampaignOption.AUTO_LOGISTICS_ENGINES);
        autoLogisticsGyros = options.get(CampaignOption.AUTO_LOGISTICS_GYROS);
        autoLogisticsHeatSink = options.get(CampaignOption.AUTO_LOGISTICS_HEAT_SINK);
        autoLogisticsWeapons = options.get(CampaignOption.AUTO_LOGISTICS_WEAPONS);
        autoLogisticsOther = options.get(CampaignOption.AUTO_LOGISTICS_OTHER);
        unitTransitTime = options.get(CampaignOption.UNIT_TRANSIT_TIME);
        noDeliveriesInTransit = options.get(CampaignOption.NO_DELIVERIES_IN_TRANSIT);
        usePlanetaryAcquisition = options.get(CampaignOption.USE_PLANETARY_ACQUISITION);
        maxJumpsPlanetaryAcquisition = options.get(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION);
        planetAcquisitionFactionLimit = options.get(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT);
        disallowPlanetAcquisitionClanCrossover = options.get(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER);
        noClanPartsFromIS = options.get(CampaignOption.NO_CLAN_PARTS_FROM_IS);
        penaltyClanPartsFromIS = options.get(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS);
        planetAcquisitionVerbose = options.get(CampaignOption.PLANET_ACQUISITION_VERBOSE);

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

        mekWarriorDefaultKit = options.get(CampaignOption.MEKWARRIOR_DEFAULT_KIT);
        vehicleCrewDefaultKit = options.get(CampaignOption.VEHICLE_CREW_DEFAULT_KIT);
        aircraftDefaultKit = options.get(CampaignOption.AIRCRAFT_DEFAULT_KIT);
        addDefaultKitToProcurement = options.get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT);
        npcFactionArmorKits = options.get(CampaignOption.NPC_FACTION_ARMOR_KITS);
        limitByYear = options.get(CampaignOption.LIMIT_BY_YEAR);
        disallowExtinctStuff = options.get(CampaignOption.DISALLOW_EXTINCT_STUFF);
        allowClanPurchases = options.get(CampaignOption.ALLOW_CLAN_PURCHASES);
        allowISPurchases = options.get(CampaignOption.ALLOW_IS_PURCHASES);
        allowCanonOnly = options.get(CampaignOption.ALLOW_CANON_ONLY);
        allowCanonRefitOnly = options.get(CampaignOption.ALLOW_CANON_REFIT_ONLY);
        limitClanTech = options.get(CampaignOption.LIMIT_CLAN_TECH);
        techLevel = options.get(CampaignOption.TECH_LEVEL);
        variableTechLevel = options.get(CampaignOption.VARIABLE_TECH_LEVEL);
        useAmmoByType = options.get(CampaignOption.USE_AMMO_BY_TYPE);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.ACQUISITIONS_TYPE, acquisitionType);
        options.set(CampaignOption.USE_FUNCTIONAL_APPRAISAL, useFunctionalAppraisal);
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, mekWarriorDefaultKit);
        options.set(CampaignOption.VEHICLE_CREW_DEFAULT_KIT, vehicleCrewDefaultKit);
        options.set(CampaignOption.AIRCRAFT_DEFAULT_KIT, aircraftDefaultKit);
        options.set(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT, addDefaultKitToProcurement);
        options.set(CampaignOption.NPC_FACTION_ARMOR_KITS, npcFactionArmorKits);
        options.set(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY, acquisitionPersonnelCategory);
        options.set(CampaignOption.CLAN_ACQUISITION_PENALTY, clanAcquisitionPenalty);
        options.set(CampaignOption.IS_ACQUISITION_PENALTY, isAcquisitionPenalty);
        options.set(CampaignOption.WAITING_PERIOD, waitingPeriod);
        options.set(CampaignOption.MAX_ACQUISITIONS, maxAcquisitions);
        options.set(CampaignOption.AUTO_LOGISTICS_MEK_HEAD, autoLogisticsMekHead);
        options.set(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION, autoLogisticsMekLocation);
        options.set(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION, autoLogisticsNonRepairableLocation);
        options.set(CampaignOption.AUTO_LOGISTICS_ARMOR, autoLogisticsArmor);
        options.set(CampaignOption.AUTO_LOGISTICS_AMMUNITION, autoLogisticsAmmunition);
        options.set(CampaignOption.AUTO_LOGISTICS_ACTUATORS, autoLogisticsActuators);
        options.set(CampaignOption.AUTO_LOGISTICS_JUMP_JETS, autoLogisticsJumpJets);
        options.set(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS, autoLogisticsHeadComponents);
        options.set(CampaignOption.AUTO_LOGISTICS_ENGINES, autoLogisticsEngines);
        options.set(CampaignOption.AUTO_LOGISTICS_GYROS, autoLogisticsGyros);
        options.set(CampaignOption.AUTO_LOGISTICS_HEAT_SINK, autoLogisticsHeatSink);
        options.set(CampaignOption.AUTO_LOGISTICS_WEAPONS, autoLogisticsWeapons);
        options.set(CampaignOption.AUTO_LOGISTICS_OTHER, autoLogisticsOther);
        options.set(CampaignOption.UNIT_TRANSIT_TIME, unitTransitTime);
        options.set(CampaignOption.NO_DELIVERIES_IN_TRANSIT, noDeliveriesInTransit);
        options.set(CampaignOption.USE_PLANETARY_ACQUISITION, usePlanetaryAcquisition);
        options.set(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION, maxJumpsPlanetaryAcquisition);
        options.set(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT, planetAcquisitionFactionLimit);
        options.set(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER, disallowPlanetAcquisitionClanCrossover);
        options.set(CampaignOption.NO_CLAN_PARTS_FROM_IS, noClanPartsFromIS);
        options.set(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS, penaltyClanPartsFromIS);
        options.set(CampaignOption.PLANET_ACQUISITION_VERBOSE, planetAcquisitionVerbose);

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

        options.set(CampaignOption.LIMIT_BY_YEAR, limitByYear);
        options.set(CampaignOption.DISALLOW_EXTINCT_STUFF, disallowExtinctStuff);
        options.set(CampaignOption.ALLOW_CLAN_PURCHASES, allowClanPurchases);
        options.set(CampaignOption.ALLOW_IS_PURCHASES, allowISPurchases);
        options.set(CampaignOption.ALLOW_CANON_ONLY, allowCanonOnly);
        options.set(CampaignOption.ALLOW_CANON_REFIT_ONLY, allowCanonRefitOnly);
        options.set(CampaignOption.TECH_LEVEL, techLevel);
        options.set(CampaignOption.VARIABLE_TECH_LEVEL, variableTechLevel);
        options.set(CampaignOption.USE_AMMO_BY_TYPE, useAmmoByType);
        options.set(CampaignOption.LIMIT_CLAN_TECH, limitClanTech);
    }
}
