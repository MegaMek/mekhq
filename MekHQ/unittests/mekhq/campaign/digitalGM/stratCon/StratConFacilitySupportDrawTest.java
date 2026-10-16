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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.facility.IStratConFacilityEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests how a fight draws support from the facilities in its sector: which facilities can be drawn, that a fight at a
 * facility still draws from the others, that your employer's facilities answer only half the time, and the record of
 * who lent what.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilitySupportDrawTest {
    // the one scenario modifier in the test fixtures
    private static final String MODIFIER = "EnemyAirSupport_test.json";
    private static final int ALWAYS = 1;
    private static final int NEVER = 0;
    private static final StratConCoords FIGHT_COORDS = new StratConCoords(1, 1);
    private static final StratConCoords LENDER_COORDS = new StratConCoords(4, 4);
    private static final StratConCoords OTHER_COORDS = new StratConCoords(6, 2);

    private StratConTrackState track;
    private StratConScenario scenario;

    @BeforeAll
    static void loadData() {
        StratConTestData.install();
        AtBScenarioModifier.initializeScenarioModifiers(true);
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);

        scenario = new StratConScenario();
        scenario.setBackingScenario(new AtBDynamicScenario());
        scenario.setCoords(FIGHT_COORDS);
    }

    private StratConFacility place(StratConCoords coords, ForceAlignment owner, FacilityType facilityType,
          IStratConFacilityEffect... effects) {
        StratConFacility facility = StratConTestData.facility(owner, facilityType, effects);
        track.addFacility(coords, facility);
        return facility;
    }

    private StratConFacility placeLender(StratConCoords coords, ForceAlignment owner, FacilityType facilityType) {
        return place(coords, owner, facilityType, new SharedModifiersEffect(List.of(MODIFIER)));
    }

    private void drawSupport(int alliedDieSize, int enemyDieSize) {
        StratConRulesManager.applyFacilityModifiers(scenario,
              track,
              FIGHT_COORDS,
              alliedDieSize,
              enemyDieSize,
              false,
              false);
    }

    private static MockedStatic<Compute> employerCoinComesUp(int result) {
        MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS);
        compute.when(() -> Compute.randomInt(StratConRulesManager.EMPLOYER_LEND_DIE_SIZE)).thenReturn(result);
        return compute;
    }

    @Test
    void aFightWithNoFacilitiesStillRecordsThatItDrewNone() {
        drawSupport(ALWAYS, ALWAYS);

        assertTrue(scenario.isFacilitySupportRecorded());
        assertTrue(scenario.getAlliedFacilitySupport().isEmpty());
        assertTrue(scenario.getHostileFacilitySupport().isEmpty());
    }

    @Test
    void aFacilityWithNothingToLendIsNeverDrawn() {
        StratConFacility sensorPost = place(OTHER_COORDS, ForceAlignment.Player, FacilityType.SensorPost,
              new ScanRangeEffect(1));
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Player, FacilityType.AirBase);

        drawSupport(ALWAYS, NEVER);

        assertEquals(List.of(airBase.getDisplayableName()), scenario.getAlliedFacilitySupport());
        assertEquals(1, scenario.getBackingScenario().getScenarioModifiers().size());
        assertFalse(airBase.isAvailable());
        assertTrue(sensorPost.isAvailable());
    }

    @Test
    void aFightAtYourFacilityStillDrawsOnYourOtherFacilities() {
        // The facility the fight is on lends nothing to itself: it brings its garrison instead, and has used its turn.
        StratConFacility mekBase = place(FIGHT_COORDS, ForceAlignment.Player, FacilityType.MekBase,
              new LocalModifiersEffect(List.of(MODIFIER)),
              new SharedModifiersEffect(List.of(MODIFIER)));
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Player, FacilityType.AirBase);

        drawSupport(ALWAYS, NEVER);

        assertEquals(List.of(airBase.getDisplayableName()), scenario.getAlliedFacilitySupport());
        assertFalse(mekBase.isAvailable());
        assertFalse(airBase.isAvailable());
    }

    @Test
    void aSynergyPartnerLendsToAFightAtItsPartnerAndIsRecorded() {
        place(FIGHT_COORDS, ForceAlignment.Player, FacilityType.AirBase, new LocalModifiersEffect(List.of(MODIFIER)));
        StratConFacility earlyWarning = placeLender(LENDER_COORDS,
              ForceAlignment.Player,
              FacilityType.EarlyWarningSystem);

        drawSupport(NEVER, NEVER);

        assertEquals(List.of(earlyWarning.getDisplayableName()), scenario.getAlliedFacilitySupport());
    }

    @Test
    void enemySupportIsRecordedByHex() {
        placeLender(LENDER_COORDS, ForceAlignment.Opposing, FacilityType.AirBase);

        drawSupport(NEVER, ALWAYS);

        assertTrue(scenario.getAlliedFacilitySupport().isEmpty());
        assertEquals(List.of(LENDER_COORDS), scenario.getHostileFacilitySupport());
    }

    @Test
    void anEmployerFacilityThatDoesNotAnswerLendsNothingAndStaysReady() {
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Allied, FacilityType.AirBase);

        try (MockedStatic<Compute> ignored = employerCoinComesUp(1)) {
            drawSupport(ALWAYS, NEVER);
        }

        assertTrue(scenario.getAlliedFacilitySupport().isEmpty());
        assertTrue(scenario.getBackingScenario().getScenarioModifiers().isEmpty());
        assertTrue(airBase.isAvailable());
    }

    @Test
    void anEmployerFacilityThatAnswersLends() {
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Allied, FacilityType.AirBase);

        try (MockedStatic<Compute> ignored = employerCoinComesUp(0)) {
            drawSupport(ALWAYS, NEVER);
        }

        assertEquals(List.of(airBase.getDisplayableName()), scenario.getAlliedFacilitySupport());
        assertFalse(airBase.isAvailable());
    }

    @Test
    void aFacilityYouHoldAlwaysAnswers() {
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Player, FacilityType.AirBase);

        // The coin that turns an employer's facility away has no say over yours.
        try (MockedStatic<Compute> ignored = employerCoinComesUp(1)) {
            drawSupport(ALWAYS, NEVER);
        }

        assertEquals(List.of(airBase.getDisplayableName()), scenario.getAlliedFacilitySupport());
    }

    @Test
    void aFacilityThatHasLentThisWeekIsNotDrawnAgain() {
        StratConFacility airBase = placeLender(LENDER_COORDS, ForceAlignment.Player, FacilityType.AirBase);
        airBase.setIsAvailable(false);

        drawSupport(ALWAYS, NEVER);

        assertTrue(scenario.getAlliedFacilitySupport().isEmpty());
    }
}
