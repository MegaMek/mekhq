/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;

class RepairAndMaintenanceOptionsModel {
    boolean techsUseAdministration;
    boolean useUsefulAsTechs;
    boolean useEraMods;
    boolean assignedTechFirst;
    boolean resetToFirstTech;
    boolean useQuirks;
    boolean useAeroSystemHits;
    boolean destroyByMargin;
    int destroyMargin;
    int destroyPartTarget;
    boolean checkMaintenance;
    int maintenanceCycleDays;
    int maintenanceBonus;
    int defaultMaintenanceTime;
    boolean useQualityMaintenance;
    boolean reverseQualityNames;
    boolean useRandomUnitQualities;
    boolean usePlanetaryModifiers;
    boolean useUnofficialMaintenance;
    boolean logMaintenance;
    boolean useFabrication;
    boolean useBalancedFabrication;
    boolean maintenanceFabrication;
    boolean useAmmoFabrication;

    RepairAndMaintenanceOptionsModel(@Nonnull CampaignOptions options) {
        techsUseAdministration = options.get(CampaignOption.TECHS_USE_ADMINISTRATION);
        useUsefulAsTechs = options.get(CampaignOption.USE_USEFUL_AS_TECHS);
        useEraMods = options.get(CampaignOption.USE_ERA_MODS);
        assignedTechFirst = options.get(CampaignOption.ASSIGNED_TECH_FIRST);
        resetToFirstTech = options.get(CampaignOption.RESET_TO_FIRST_TECH);
        useQuirks = options.get(CampaignOption.USE_QUIRKS);
        useAeroSystemHits = options.get(CampaignOption.USE_AERO_SYSTEM_HITS);
        destroyByMargin = options.get(CampaignOption.DESTROY_BY_MARGIN);
        destroyMargin = options.get(CampaignOption.DESTROY_MARGIN);
        destroyPartTarget = options.get(CampaignOption.DESTROY_PART_TARGET);
        checkMaintenance = options.get(CampaignOption.CHECK_MAINTENANCE);
        maintenanceCycleDays = options.get(CampaignOption.MAINTENANCE_CYCLE_DAYS);
        maintenanceBonus = options.get(CampaignOption.MAINTENANCE_BONUS);
        defaultMaintenanceTime = options.get(CampaignOption.DEFAULT_MAINTENANCE_TIME);
        useQualityMaintenance = options.get(CampaignOption.USE_QUALITY_MAINTENANCE);
        reverseQualityNames = options.get(CampaignOption.REVERSE_QUALITY_NAMES);
        useRandomUnitQualities = options.get(CampaignOption.USE_RANDOM_UNIT_QUALITIES);
        usePlanetaryModifiers = options.get(CampaignOption.USE_PLANETARY_MODIFIERS);
        useUnofficialMaintenance = options.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE);
        logMaintenance = options.get(CampaignOption.LOG_MAINTENANCE);
        useFabrication = options.get(CampaignOption.USE_FABRICATION);
        useBalancedFabrication = options.get(CampaignOption.USE_BALANCED_FABRICATION);
        maintenanceFabrication = options.get(CampaignOption.FABRICATE_D_IN_MAINTENANCE_FACILITY);
        useAmmoFabrication = options.get(CampaignOption.USE_AMMO_FABRICATION);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.TECHS_USE_ADMINISTRATION, techsUseAdministration);
        options.set(CampaignOption.USE_USEFUL_AS_TECHS, useUsefulAsTechs);
        options.set(CampaignOption.USE_ERA_MODS, useEraMods);
        options.set(CampaignOption.ASSIGNED_TECH_FIRST, assignedTechFirst);
        options.set(CampaignOption.RESET_TO_FIRST_TECH, resetToFirstTech);
        options.set(CampaignOption.USE_QUIRKS, useQuirks);
        options.set(CampaignOption.USE_AERO_SYSTEM_HITS, useAeroSystemHits);
        options.set(CampaignOption.DESTROY_BY_MARGIN, destroyByMargin);
        options.set(CampaignOption.DESTROY_MARGIN, destroyMargin);
        options.set(CampaignOption.DESTROY_PART_TARGET, destroyPartTarget);
        options.set(CampaignOption.CHECK_MAINTENANCE, checkMaintenance);
        options.set(CampaignOption.MAINTENANCE_CYCLE_DAYS, maintenanceCycleDays);
        options.set(CampaignOption.MAINTENANCE_BONUS, maintenanceBonus);
        options.set(CampaignOption.DEFAULT_MAINTENANCE_TIME, defaultMaintenanceTime);
        options.set(CampaignOption.USE_QUALITY_MAINTENANCE, useQualityMaintenance);
        options.set(CampaignOption.REVERSE_QUALITY_NAMES, reverseQualityNames);
        options.set(CampaignOption.USE_RANDOM_UNIT_QUALITIES, useRandomUnitQualities);
        options.set(CampaignOption.USE_PLANETARY_MODIFIERS, usePlanetaryModifiers);
        options.set(CampaignOption.USE_UNOFFICIAL_MAINTENANCE, useUnofficialMaintenance);
        options.set(CampaignOption.LOG_MAINTENANCE, logMaintenance);
        options.set(CampaignOption.USE_FABRICATION, useFabrication);
        options.set(CampaignOption.USE_BALANCED_FABRICATION, useBalancedFabrication);
        options.set(CampaignOption.FABRICATE_D_IN_MAINTENANCE_FACILITY, maintenanceFabrication);
        options.set(CampaignOption.USE_AMMO_FABRICATION, useAmmoFabrication);
    }
}
