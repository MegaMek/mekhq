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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that every resource key built from an enum constant exists, so a constant added without its text fails here
 * instead of showing the player a {@code !key!}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityResourceKeysTest {
    private static final ResourceBundle OPERATIONS = ResourceBundle.getBundle(
          "mekhq.resources.StratConFacilityOperations");
    private static final ResourceBundle STRATCON = ResourceBundle.getBundle("mekhq.resources.AtBStratCon");

    private static void assertHasKey(ResourceBundle bundle, String key) {
        assertTrue(bundle.containsKey(key), "Missing resource key " + key);
    }

    @ParameterizedTest
    @EnumSource(FacilityOperation.class)
    void everyOrderHasANameAndATooltip(FacilityOperation operation) {
        assertHasKey(OPERATIONS, "operation." + operation.name());
        assertHasKey(OPERATIONS, "operation.tooltip." + operation.name());
    }

    @ParameterizedTest
    @EnumSource(value = FacilityOperation.class, names = { "RECON", "BUILD", "SIEGE" })
    void everyTimedOrderCanBeAbandonedOrOverrun(FacilityOperation operation) {
        assertHasKey(OPERATIONS, "report.abandoned." + operation.name());
        assertHasKey(OPERATIONS, "report.targetTaken." + operation.name());
    }

    @ParameterizedTest
    @EnumSource(FacilityTrait.class)
    void everyTraitHasANameAndADescription(FacilityTrait trait) {
        assertHasKey(OPERATIONS, "trait." + trait.name());
        assertHasKey(OPERATIONS, "trait." + trait.name() + ".description");
    }

    @ParameterizedTest
    @EnumSource(FacilityCaptureChoice.class)
    void everyCaptureChoiceHasAButtonAndAReport(FacilityCaptureChoice choice) {
        assertHasKey(OPERATIONS, "capture.button." + choice.name());
        assertHasKey(OPERATIONS, "report.capture." + choice.name());
    }

    @ParameterizedTest
    @EnumSource(FacilityTier.class)
    void everyTierHasAName(FacilityTier tier) {
        assertHasKey(STRATCON, "stratConTab.facilityTier." + tier.name());
    }

    @ParameterizedTest
    @EnumSource(FacilityCondition.class)
    void everyConditionHasAName(FacilityCondition condition) {
        assertHasKey(STRATCON, "stratConTab.facilityCondition." + condition.name());
    }

    @ParameterizedTest
    @EnumSource(FacilityIntel.class)
    void everyIntelLevelHasAName(FacilityIntel intel) {
        assertHasKey(STRATCON, "stratConTab.facilityIntel." + intel.name());
    }
}
