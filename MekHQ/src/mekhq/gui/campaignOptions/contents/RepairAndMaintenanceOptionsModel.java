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

    RepairAndMaintenanceOptionsModel(@Nonnull CampaignOptions options) {
        techsUseAdministration = options.isTechsUseAdministration();
        useUsefulAsTechs = options.isUseUsefulAsTechs();
        useEraMods = options.isUseEraMods();
        assignedTechFirst = options.isAssignedTechFirst();
        resetToFirstTech = options.isResetToFirstTech();
        useQuirks = options.isUseQuirks();
        useAeroSystemHits = options.isUseAeroSystemHits();
        destroyByMargin = options.isDestroyByMargin();
        destroyMargin = options.getDestroyMargin();
        destroyPartTarget = options.getDestroyPartTarget();
        checkMaintenance = options.isCheckMaintenance();
        maintenanceCycleDays = options.getMaintenanceCycleDays();
        maintenanceBonus = options.getMaintenanceBonus();
        defaultMaintenanceTime = options.getDefaultMaintenanceTime();
        useQualityMaintenance = options.isUseQualityMaintenance();
        reverseQualityNames = options.isReverseQualityNames();
        useRandomUnitQualities = options.isUseRandomUnitQualities();
        usePlanetaryModifiers = options.isUsePlanetaryModifiers();
        useUnofficialMaintenance = options.isUseUnofficialMaintenance();
        logMaintenance = options.isLogMaintenance();
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.setTechsUseAdministration(techsUseAdministration);
        options.setIsUseUsefulAsTechs(useUsefulAsTechs);
        options.setEraMods(useEraMods);
        options.setAssignedTechFirst(assignedTechFirst);
        options.setResetToFirstTech(resetToFirstTech);
        options.setQuirks(useQuirks);
        options.setUseAeroSystemHits(useAeroSystemHits);
        options.setDestroyByMargin(destroyByMargin);
        options.setDestroyMargin(destroyMargin);
        options.setDestroyPartTarget(destroyPartTarget);
        options.setCheckMaintenance(checkMaintenance);
        options.setMaintenanceCycleDays(maintenanceCycleDays);
        options.setMaintenanceBonus(maintenanceBonus);
        options.setDefaultMaintenanceTime(defaultMaintenanceTime);
        options.setUseQualityMaintenance(useQualityMaintenance);
        options.setReverseQualityNames(reverseQualityNames);
        options.setUseRandomUnitQualities(useRandomUnitQualities);
        options.setUsePlanetaryModifiers(usePlanetaryModifiers);
        options.setUseUnofficialMaintenance(useUnofficialMaintenance);
        options.setLogMaintenance(logMaintenance);
    }
}
