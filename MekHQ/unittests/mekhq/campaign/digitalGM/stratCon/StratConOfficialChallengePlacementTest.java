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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.ScenarioType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests for Official Challenge placement: an Official Challenge is moved off any hex holding a deployed formation, a
 * scenario, or a facility, is dropped when no free hex exists, and a siege sortie never rolls one.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConOfficialChallengePlacementTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        return track;
    }

    @Test
    void emptyHexIsFree() {
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track(), new StratConCoords(2, 2)));
    }

    @Test
    void hexWithDeployedFormationIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(2, 2);
        track.assignForce(7, coords, LocalDate.of(3025, 1, 1), false);

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track, new StratConCoords(1, 1)));
    }

    @Test
    void hexWithFacilityIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(3, 1);
        track.addFacility(coords, new StratConFacility());

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
    }

    @Test
    void hexWithScenarioIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(0, 4);
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(coords);
        track.addScenario(scenario);

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
    }

    @Test
    void missingTrackOrCoordsIsNeverOccupied() {
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(null, new StratConCoords(0, 0)));
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track(), null));
    }

    private static ScenarioTemplate template(ScenarioType scenarioType) {
        ScenarioTemplate template = mock(ScenarioTemplate.class);
        when(template.getStratConScenarioType()).thenReturn(scenarioType);
        return template;
    }

    @Test
    void challengeOnFreeHexStaysPut() {
        StratConCoords coords = new StratConCoords(2, 2);

        assertEquals(coords,
              StratConRulesManager.getOfficialChallengeCoords(mock(CampaignOptions.class), track(), coords));
    }

    @Test
    void challengeOnDeployedFormationIsMovedToFreeHex() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(2);
        track.setHeight(1);
        StratConCoords formationCoords = new StratConCoords(0, 0);
        track.assignForce(7, formationCoords, LocalDate.of(3025, 1, 1), false);

        StratConCoords placedCoords = StratConRulesManager.getOfficialChallengeCoords(mock(CampaignOptions.class),
              track,
              formationCoords);

        assertEquals(new StratConCoords(1, 0), placedCoords);
    }

    @Test
    void challengeWithNoFreeHexHasNoPlacement() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(1);
        track.setHeight(1);
        StratConCoords formationCoords = new StratConCoords(0, 0);
        track.assignForce(7, formationCoords, LocalDate.of(3025, 1, 1), false);

        assertNull(StratConRulesManager.getOfficialChallengeCoords(mock(CampaignOptions.class),
              track,
              formationCoords));
    }

    @Test
    void generateScenarioSkipsChallengeWhenNoFreeHex() {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        StratConTrackState track = new StratConTrackState();
        track.setWidth(1);
        track.setHeight(1);
        StratConCoords formationCoords = new StratConCoords(0, 0);
        track.assignForce(7, formationCoords, LocalDate.of(3025, 1, 1), false);

        try (MockedStatic<AtBDynamicScenarioFactory> scenarioFactory = mockStatic(AtBDynamicScenarioFactory.class)) {
            StratConScenario scenario = StratConRulesManager.generateScenario(campaign,
                  mock(AbstractContract.class),
                  track,
                  7,
                  formationCoords,
                  template(ScenarioType.OFFICIAL_CHALLENGE),
                  null);

            assertNull(scenario);
            scenarioFactory.verify(() -> AtBDynamicScenarioFactory.initializeScenarioFromTemplate(any(), any(),
                  any()), never());
        }
    }

    @Test
    void siegeSortieRerollsOfficialChallenge() {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        AbstractContract contract = mock(AbstractContract.class);
        StratConTrackState track = track();
        StratConCoords formationCoords = new StratConCoords(1, 1);
        ScenarioTemplate challenge = template(ScenarioType.OFFICIAL_CHALLENGE);
        ScenarioTemplate ordinary = template(ScenarioType.NONE);

        try (MockedStatic<StratConScenarioFactory> scenarioFactory = mockStatic(StratConScenarioFactory.class);
              MockedStatic<StratConRulesManager> rulesManager = mockStatic(StratConRulesManager.class,
                    CALLS_REAL_METHODS)) {
            rulesManager.when(() -> StratConRulesManager.getFleetRestrictedMapLocations(any())).thenReturn(null);
            scenarioFactory.when(() -> StratConScenarioFactory.getRandomScenario(anyInt(), anyBoolean(), anyBoolean(),
                  any())).thenReturn(challenge, ordinary);
            rulesManager.when(() -> StratConRulesManager.generateScenario(any(), any(), any(), any(), any(),
                  any(ScenarioTemplate.class), any())).thenReturn(null);

            StratConRulesManager.startSiegeScenario(campaign, contract, track, formationCoords, 7,
                  new StratConCoords(2, 2), true);

            rulesManager.verify(() -> StratConRulesManager.generateScenario(any(), any(), any(), any(),
                  eq(formationCoords), eq(ordinary), any()));
            rulesManager.verify(() -> StratConRulesManager.generateScenario(any(), any(), any(), any(), any(),
                  eq(challenge), any()), never());
        }
    }

    @Test
    void siegeSortieWithOnlyChallengesGeneratesNothing() {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        StratConTrackState track = track();
        ScenarioTemplate challenge = template(ScenarioType.OFFICIAL_CHALLENGE);

        try (MockedStatic<StratConScenarioFactory> scenarioFactory = mockStatic(StratConScenarioFactory.class);
              MockedStatic<StratConRulesManager> rulesManager = mockStatic(StratConRulesManager.class,
                    CALLS_REAL_METHODS)) {
            rulesManager.when(() -> StratConRulesManager.getFleetRestrictedMapLocations(any())).thenReturn(null);
            scenarioFactory.when(() -> StratConScenarioFactory.getRandomScenario(anyInt(), anyBoolean(), anyBoolean(),
                  any())).thenReturn(challenge);

            assertNull(StratConRulesManager.startSiegeScenario(campaign, mock(AbstractContract.class), track,
                  new StratConCoords(1, 1), 7, new StratConCoords(2, 2), true));
            rulesManager.verify(() -> StratConRulesManager.generateScenario(any(), any(), any(), any(), any(),
                  any(ScenarioTemplate.class), any()), never());
        }
    }
}
