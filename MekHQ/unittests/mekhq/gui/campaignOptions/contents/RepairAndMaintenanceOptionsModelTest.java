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

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for {@link RepairAndMaintenanceOptionsModel}. Every field this model exposes is a plain boolean or
 * int copied straight to and from {@link CampaignOptions}, so the test assigns a distinct value to each field, writes
 * the model out with {@link RepairAndMaintenanceOptionsModel#applyTo(CampaignOptions)}, reads it back into a fresh
 * model, and asserts nothing was lost &mdash; which catches any field that {@code applyTo} or the constructor forgets.
 */
class RepairAndMaintenanceOptionsModelTest {
    @Test
    void applyToPreservesEveryField() {
        RepairAndMaintenanceOptionsModel model = new RepairAndMaintenanceOptionsModel(new CampaignOptions());

        model.techsUseAdministration = true;
        model.useUsefulAsTechs = false;
        model.useEraMods = true;
        model.assignedTechFirst = false;
        model.resetToFirstTech = true;
        model.useQuirks = false;
        model.useAeroSystemHits = true;
        model.destroyByMargin = false;
        model.destroyMargin = 7;
        model.destroyPartTarget = 11;
        model.checkMaintenance = true;
        model.maintenanceCycleDays = 13;
        model.maintenanceBonus = 17;
        model.defaultMaintenanceTime = 19;
        model.useQualityMaintenance = false;
        model.reverseQualityNames = true;
        model.useRandomUnitQualities = false;
        model.usePlanetaryModifiers = true;
        model.useUnofficialMaintenance = false;
        model.logMaintenance = true;

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(destination);
        RepairAndMaintenanceOptionsModel roundTripped = new RepairAndMaintenanceOptionsModel(destination);

        assertEquals(model.techsUseAdministration, roundTripped.techsUseAdministration);
        assertEquals(model.useUsefulAsTechs, roundTripped.useUsefulAsTechs);
        assertEquals(model.useEraMods, roundTripped.useEraMods);
        assertEquals(model.assignedTechFirst, roundTripped.assignedTechFirst);
        assertEquals(model.resetToFirstTech, roundTripped.resetToFirstTech);
        assertEquals(model.useQuirks, roundTripped.useQuirks);
        assertEquals(model.useAeroSystemHits, roundTripped.useAeroSystemHits);
        assertEquals(model.destroyByMargin, roundTripped.destroyByMargin);
        assertEquals(model.destroyMargin, roundTripped.destroyMargin);
        assertEquals(model.destroyPartTarget, roundTripped.destroyPartTarget);
        assertEquals(model.checkMaintenance, roundTripped.checkMaintenance);
        assertEquals(model.maintenanceCycleDays, roundTripped.maintenanceCycleDays);
        assertEquals(model.maintenanceBonus, roundTripped.maintenanceBonus);
        assertEquals(model.defaultMaintenanceTime, roundTripped.defaultMaintenanceTime);
        assertEquals(model.useQualityMaintenance, roundTripped.useQualityMaintenance);
        assertEquals(model.reverseQualityNames, roundTripped.reverseQualityNames);
        assertEquals(model.useRandomUnitQualities, roundTripped.useRandomUnitQualities);
        assertEquals(model.usePlanetaryModifiers, roundTripped.usePlanetaryModifiers);
        assertEquals(model.useUnofficialMaintenance, roundTripped.useUnofficialMaintenance);
        assertEquals(model.logMaintenance, roundTripped.logMaintenance);
    }
}
