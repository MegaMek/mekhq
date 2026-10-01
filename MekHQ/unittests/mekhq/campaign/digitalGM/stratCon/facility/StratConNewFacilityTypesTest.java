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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the Sensor Post, Jamming Station, Field Fortifications and Militia Barracks definitions: that each loads with
 * a profile for both sides, and that each does what its description says.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConNewFacilityTypesTest {
    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConFacility facility(FacilityType facilityType, ForceAlignment owner) {
        StratConFacility facility = StratConContractFacilityProfile.createFacility(facilityType, owner);
        assertNotNull(facility, facilityType + " has no definition for " + owner);
        return facility;
    }

    @ParameterizedTest
    @EnumSource(value = FacilityType.class, names = { "SensorPost", "JammingStation", "FieldFortifications",
                                                      "MilitiaBarracks" })
    void eachNewTypeHasADescribedProfileForBothSides(FacilityType facilityType) {
        StratConFacilityDefinition definition = StratConFacilityFactory.getDefinitionForType(facilityType);
        assertNotNull(definition);
        assertTrue(definition.hasProfileFor(ForceAlignment.Allied));
        assertTrue(definition.hasProfileFor(ForceAlignment.Opposing));
        assertNotNull(definition.getAlliedProfile().getDescription());
        assertNotNull(definition.getHostileProfile().getDescription());
    }

    @Test
    void aSensorPostOnYourSideExtendsScanRange() {
        assertEquals(1, facility(FacilityType.SensorPost, ForceAlignment.Allied).getScanRangeIncrease());
    }

    @Test
    void anEnemySensorPostMakesFightsMoreLikelyAndAmbushesAtThePost() {
        StratConFacility sensorPost = facility(FacilityType.SensorPost, ForceAlignment.Opposing);

        assertTrue(sensorPost.getScenarioOddsModifier() > 0);
        assertTrue(sensorPost.getLocalModifiers().contains("EnemyAmbush.json"));
    }

    @Test
    void anEnemyJammingStationCutsYourScanRangeOnTheTrack() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
        track.addFacility(new StratConCoords(1, 1), facility(FacilityType.DataCenter, ForceAlignment.Allied));
        track.addFacility(new StratConCoords(5, 5), facility(FacilityType.JammingStation, ForceAlignment.Opposing));

        // The Data Center's +1 and the jammer's -1 cancel out.
        assertEquals(0, track.getScanRangeIncrease());
    }

    @Test
    void aJammingStationOnYourSideMakesFightsLessLikely() {
        assertTrue(facility(FacilityType.JammingStation, ForceAlignment.Allied).getScenarioOddsModifier() < 0);
    }

    @Test
    void fieldFortificationsOnlyMatterWhereTheyStand() {
        for (ForceAlignment owner : new ForceAlignment[] { ForceAlignment.Allied, ForceAlignment.Opposing }) {
            StratConFacility fortifications = facility(FacilityType.FieldFortifications, owner);
            assertFalse(fortifications.getLocalModifiers().isEmpty(), owner.name());
            assertTrue(fortifications.getSharedModifiers().isEmpty(), owner.name());
        }
    }

    @Test
    void enemyMilitiaAreGreenButStirUpTheSector() {
        StratConFacility barracks = facility(FacilityType.MilitiaBarracks, ForceAlignment.Opposing);

        assertTrue(barracks.getLocalModifiers().contains("Rookies.json"));
        assertTrue(barracks.getScenarioOddsModifier() > 0);
    }

    @Test
    void militiaOnYourSideCalmTheSector() {
        assertTrue(facility(FacilityType.MilitiaBarracks, ForceAlignment.Allied).getScenarioOddsModifier() < 0);
    }
}
