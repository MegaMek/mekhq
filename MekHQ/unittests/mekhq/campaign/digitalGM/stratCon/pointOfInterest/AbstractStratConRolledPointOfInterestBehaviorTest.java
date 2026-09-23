/*
 * Copyright (C) -2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests how a rolled point of interest places the scenario that breaks out over it: set up on its hex with the
 * deploying formation as its seed, readied, finalized without that formation, given anything its type adds, and linked
 * to the point of interest.
 *
 * @author Illiani
 * @since 0.51.01
 */
class AbstractStratConRolledPointOfInterestBehaviorTest {
    private static final String TEMPLATE_NAME = "Unit Test Template.json";
    private static final int BACKING_SCENARIO_ID = 7;
    private static final int FORMATION_ID = 3;

    private final List<String> events = new ArrayList<>();
    private Campaign campaign;
    private AbstractContract contract;
    private StratConTrackState track;
    private StratConPointOfInterest pointOfInterest;
    private StratConScenario scenario;
    private AtBDynamicScenario backingScenario;
    private ScenarioTemplate template;

    /** A rolled point of interest that records when its scenario is readied and finalized. */
    private final class TestBehavior extends AbstractStratConRolledPointOfInterestBehavior {
        private final boolean isAmbush;

        private TestBehavior(boolean isAmbush) {
            super("unitTestRolledBehavior", TEMPLATE_NAME);
            this.isAmbush = isAmbush;
        }

        @Override
        protected String getScenarioReportKeySuffix() {
            return "unitTest.report";
        }

        @Override
        protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
              AbstractContract contract, Campaign campaign) {
        }

        @Override
        protected boolean isScenarioAnAmbush() {
            return isAmbush;
        }

        @Override
        protected void prepareScenario(StratConScenario scenario, StratConTrackState track) {
            events.add("prepare");
        }

        @Override
        protected void onScenarioFinalized(StratConScenario scenario, AbstractContract contract, Campaign campaign) {
            events.add("finalized");
        }
    }

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        contract = mock(AbstractContract.class);
        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        pointOfInterest = new StratConPointOfInterest("UnitTestRolledType", new StratConCoords(2, 2));

        scenario = mock(StratConScenario.class);
        backingScenario = mock(AtBDynamicScenario.class);
        when(scenario.getBackingScenario()).thenReturn(backingScenario);
        when(scenario.getBackingScenarioID()).thenReturn(BACKING_SCENARIO_ID);
        // Set up with the deploying formation as its seed, as generateScenario does.
        when(scenario.getPrimaryForceIDs()).thenReturn(new ArrayList<>(List.of(FORMATION_ID)));
        template = new ScenarioTemplate();
    }

    private StratConScenario placeWithMockedRules(TestBehavior behavior, boolean isSetUpSuccessful) {
        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class);
              MockedStatic<StratConScenarioFactory> factory = mockStatic(StratConScenarioFactory.class)) {
            factory.when(() -> StratConScenarioFactory.getSpecificScenario(TEMPLATE_NAME)).thenReturn(template);
            rules.when(() -> StratConRulesManager.setupScenario(eq(pointOfInterest.getCoords()),
                  eq(FORMATION_ID),
                  eq(campaign),
                  eq(contract),
                  eq(track),
                  eq(template),
                  eq(true),
                  isNull())).thenReturn(isSetUpSuccessful ? scenario : null);
            rules.when(() -> StratConRulesManager.finalizeBackingScenario(campaign, contract, track, false, scenario))
                  .thenAnswer(invocation -> {
                      events.add("finalize");
                      return null;
                  });

            StratConScenario placed = behavior.placeScenario(pointOfInterest, track, FORMATION_ID, contract,
                  campaign);

            if (!isSetUpSuccessful) {
                rules.verify(() -> StratConRulesManager.finalizeBackingScenario(any(),
                      any(),
                      any(),
                      anyBoolean(),
                      any()), never());
            }
            return placed;
        }
    }

    @Test
    void theScenarioIsReadiedThenFinalizedThenLinked() {
        StratConScenario placed = placeWithMockedRules(new TestBehavior(false), true);

        assertSame(scenario, placed);
        assertEquals(List.of("prepare", "finalize", "finalized"),
              events,
              "readied before finalizing, so finalizing sees it; added to once finalized");
        assertEquals(BACKING_SCENARIO_ID, pointOfInterest.getLinkedScenarioId());
    }

    @Test
    void theDeployingFormationSeedsTheScenarioThenIsHandedBack() {
        // Seeding it sizes the opposition against the formation (the setupScenario stub only matches the formation's
        // ID); it is then taken out of the primary forces, to be assigned like any scenario found on the hex.
        StratConScenario placed = placeWithMockedRules(new TestBehavior(false), true);

        assertSame(scenario, placed, "set up with the deploying formation");
        assertFalse(scenario.getPrimaryForceIDs().contains(FORMATION_ID));
    }

    @Test
    void aScenarioThatIsNotAnAmbushKeepsWhatFinalizingDecided() {
        placeWithMockedRules(new TestBehavior(false), true);

        verify(backingScenario, never()).setIsCrisis(true);
        verify(scenario, never()).setTurningPoint(false);
    }

    @Test
    void anAmbushIsAlwaysACrisisAndNeverATurningPoint() {
        placeWithMockedRules(new TestBehavior(true), true);

        verify(backingScenario).setIsCrisis(true);
        verify(scenario).setTurningPoint(false);
    }

    @Test
    void aScenarioThatCannotBeSetUpIsNeitherFinalizedNorLinked() {
        StratConScenario placed = placeWithMockedRules(new TestBehavior(false), false);

        assertNull(placed);
        assertNull(pointOfInterest.getLinkedScenarioId());
        assertEquals(List.of(), events);
    }
}
