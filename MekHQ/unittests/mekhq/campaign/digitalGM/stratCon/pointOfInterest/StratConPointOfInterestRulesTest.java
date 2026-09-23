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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Tests for how points of interest take part in play: being revealed by scouting (and their reveal hook firing), their
 * sector-wide scan range and scenario odds effects, the daily step and expiry, and their say in deployments.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestRulesTest {
    private static final String HIDDEN_TYPE_ID = "UnitTestRulesHidden";
    private static final String SENSOR_TYPE_ID = "UnitTestRulesSensor";
    private static final String DANGER_TYPE_ID = "UnitTestRulesDanger";
    private static final String COUNTING_BEHAVIOR_ID = "unitTestRulesCountingBehavior";
    private static final String REMOVING_BEHAVIOR_ID = "unitTestRulesRemovingBehavior";
    private static final String DAILY_TYPE_ID = "UnitTestRulesDaily";
    private static final String DAILY_BEHAVIOR_ID = "unitTestRulesDailyBehavior";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);

    // What the daily test behavior records, and how it answers
    private final List<String> newDayIds = new ArrayList<>();
    private final List<String> expiredIds = new ArrayList<>();
    private final List<String> deployedIds = new ArrayList<>();
    private PointOfInterestStatus statusToSetOnExpiry;
    private PointOfInterestDeploymentOutcome deploymentOutcome = PointOfInterestDeploymentOutcome.NO_EFFECT;
    // whether the daily test behavior places a scenario on its hex when a formation deploys there
    private boolean isPlacingScenarioOnDeployment;

    private final List<String> revealedIds = new ArrayList<>();
    private StratConTrackState track;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition hidden = definition(HIDDEN_TYPE_ID);
        hidden.setHiddenUntilScouted(true);
        hidden.setBehaviorId(COUNTING_BEHAVIOR_ID);
        StratConPointOfInterestDefinitions.registerDefinition(hidden);

        StratConPointOfInterestDefinition sensor = definition(SENSOR_TYPE_ID);
        sensor.setScanRangeIncrease(2);
        StratConPointOfInterestDefinitions.registerDefinition(sensor);

        StratConPointOfInterestDefinition danger = definition(DANGER_TYPE_ID);
        danger.setScenarioOddsModifier(10);
        StratConPointOfInterestDefinitions.registerDefinition(danger);

        StratConPointOfInterestBehaviors.registerBehavior(COUNTING_BEHAVIOR_ID, new IStratConPointOfInterestBehavior() {
            @Override
            public void onRevealed(StratConPointOfInterest pointOfInterest, StratConTrackState track,
                  Campaign campaign) {
                revealedIds.add(pointOfInterest.getId());
            }
        });

        StratConPointOfInterestDefinition daily = definition(DAILY_TYPE_ID);
        daily.setBehaviorId(DAILY_BEHAVIOR_ID);
        StratConPointOfInterestDefinitions.registerDefinition(daily);

        StratConPointOfInterestBehaviors.registerBehavior(DAILY_BEHAVIOR_ID, new IStratConPointOfInterestBehavior() {
            @Override
            public void onNewDay(StratConPointOfInterest pointOfInterest, StratConTrackState track,
                  Campaign campaign) {
                newDayIds.add(pointOfInterest.getId());
            }

            @Override
            public void onExpired(StratConPointOfInterest pointOfInterest, StratConTrackState track,
                  Campaign campaign) {
                expiredIds.add(pointOfInterest.getId());
                if (statusToSetOnExpiry != null) {
                    pointOfInterest.setStatus(statusToSetOnExpiry);
                }
            }

            @Override
            public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
                  StratConTrackState track, int formationId, Campaign campaign) {
                deployedIds.add(pointOfInterest.getId());
                if (isPlacingScenarioOnDeployment) {
                    StratConScenario scenario = new StratConScenario();
                    scenario.setCoords(pointOfInterest.getCoords());
                    track.getScenarios().put(pointOfInterest.getCoords(), scenario);
                }
                return deploymentOutcome;
            }
        });

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(HIDDEN_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(SENSOR_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(DANGER_TYPE_ID);
        StratConPointOfInterestBehaviors.unregisterBehavior(COUNTING_BEHAVIOR_ID);
        StratConPointOfInterestBehaviors.unregisterBehavior(REMOVING_BEHAVIOR_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(DAILY_TYPE_ID);
        StratConPointOfInterestBehaviors.unregisterBehavior(DAILY_BEHAVIOR_ID);
    }

    private static StratConPointOfInterestDefinition definition(String typeId) {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(typeId);
        return definition;
    }

    private StratConPointOfInterest place(String typeId, int x, int y) {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(typeId, new StratConCoords(x, y));
        assertTrue(track.addPointOfInterest(pointOfInterest), "test setup: the point of interest should be placed");
        return pointOfInterest;
    }

    // Revealing

    @Test
    void revealingAHexRevealsItsPointsOfInterestAndFiresTheHookOnce() {
        StratConPointOfInterest pointOfInterest = place(HIDDEN_TYPE_ID, 1, 1);
        Campaign campaign = mock(Campaign.class);

        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), campaign);
        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), campaign);

        assertTrue(pointOfInterest.isRevealed());
        assertEquals(List.of(pointOfInterest.getId()), revealedIds, "the hook fires once, however often it is scouted");
    }

    @Test
    void revealingAHexLeavesOtherHexesAlone() {
        StratConPointOfInterest elsewhere = place(HIDDEN_TYPE_ID, 3, 3);

        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), mock(Campaign.class));

        assertFalse(elsewhere.isRevealed());
        assertTrue(revealedIds.isEmpty());
    }

    @Test
    void aRevealHookMayRemoveItsOwnPointOfInterest() {
        StratConPointOfInterestDefinitions.getDefinition(HIDDEN_TYPE_ID).setBehaviorId(REMOVING_BEHAVIOR_ID);
        StratConPointOfInterestBehaviors.registerBehavior(REMOVING_BEHAVIOR_ID, new IStratConPointOfInterestBehavior() {
            @Override
            public void onRevealed(StratConPointOfInterest pointOfInterest, StratConTrackState track,
                  Campaign campaign) {
                track.removePointOfInterest(pointOfInterest.getId());
            }
        });
        place(HIDDEN_TYPE_ID, 1, 1);
        place(HIDDEN_TYPE_ID, 1, 1);

        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), mock(Campaign.class));

        assertTrue(track.getPointsOfInterest().isEmpty(), "both hooks ran even though the first changed the hex");
    }

    @Test
    void hidingAllPointsOfInterestLetsThemBeFoundAgain() {
        StratConPointOfInterest pointOfInterest = place(HIDDEN_TYPE_ID, 1, 1);
        Campaign campaign = mock(Campaign.class);
        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), campaign);

        StratConPointOfInterestRules.hideAllPointsOfInterest(track);
        assertFalse(pointOfInterest.isRevealed());

        StratConPointOfInterestRules.revealPointsOfInterest(track, new StratConCoords(1, 1), campaign);
        assertEquals(2, revealedIds.size(), "found afresh, so the hook fires again");
    }

    @Test
    void deployingOntoAHexRevealsItsPointsOfInterest() {
        StratConPointOfInterest pointOfInterest = place(HIDDEN_TYPE_ID, 2, 2);
        int formationId = 1;
        Campaign campaign = deploymentCampaign(formationId);

        StratConRulesManager.processForceDeployment(new StratConCoords(2, 2), formationId, campaign, track, false);

        assertTrue(pointOfInterest.isRevealed());
        assertEquals(List.of(pointOfInterest.getId()), revealedIds);
    }

    /**
     * A campaign just complete enough for {@link StratConRulesManager#processForceDeployment}: a formation with no
     * units (so no scouts and no fatigue to apply) and no combat team (so no patrol bonus).
     */
    private static Campaign deploymentCampaign(int formationId) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
        when(options.get(CampaignOption.FATIGUE_RATE)).thenReturn(0);
        when(options.get(CampaignOption.USE_ADVANCED_SCOUTING)).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 15));

        Formation formation = mock(Formation.class);
        when(formation.getAllUnits(false)).thenReturn(new Vector<UUID>());
        when(campaign.getPlayerForce().getFormation(formationId)).thenReturn(formation);
        when(campaign.getPlayerForce().getHangar()).thenReturn(mock(LocalHangar.class));
        when(campaign.getPlayerForce().getCombatTeamsAsMap(campaign)).thenReturn(new Hashtable<Integer, CombatTeam>());
        return campaign;
    }

    // Sector-wide effects

    @Test
    void scanRangeCountsOnlyActivePointsOfInterestHeldByThePlayerSide() {
        StratConPointOfInterest sensor = place(SENSOR_TYPE_ID, 1, 1);
        assertEquals(0, track.getScanRangeIncrease(), "a neutral sensor scouts for no one");

        sensor.setOwner(ForceAlignment.Opposing);
        assertEquals(0, track.getScanRangeIncrease(), "a hostile sensor scouts for the enemy");

        sensor.setOwner(ForceAlignment.Allied);
        assertEquals(2, track.getScanRangeIncrease());

        sensor.setStatus(PointOfInterestStatus.EXPIRED);
        assertEquals(0, track.getScanRangeIncrease(), "an expired sensor no longer helps");
    }

    @Test
    void scanRangeAddsPointsOfInterestToFacilities() {
        StratConFacility facility = new StratConFacility();
        facility.setIncreaseScanRange(true);
        track.addFacility(new StratConCoords(0, 0), facility);
        StratConPointOfInterest sensor = place(SENSOR_TYPE_ID, 1, 1);
        sensor.setOwner(ForceAlignment.Player);

        assertEquals(3, track.getScanRangeIncrease(), "one from the facility, two from the point of interest");
    }

    @Test
    void scenarioOddsCountActivePointsOfInterestWhoeverHoldsThem() {
        StratConPointOfInterest danger = place(DANGER_TYPE_ID, 1, 1);
        assertEquals(10, track.getScenarioOddsAdjustment());

        danger.setOwner(ForceAlignment.Allied);
        assertEquals(10, track.getScenarioOddsAdjustment(), "the ground is dangerous whoever holds it");

        danger.setStatus(PointOfInterestStatus.RESOLVED);
        assertEquals(0, track.getScenarioOddsAdjustment(), "a resolved point of interest no longer counts");
    }

    @Test
    void behaviorsCanOverrideTheSectorWideEffects() {
        StratConPointOfInterestDefinitions.getDefinition(DANGER_TYPE_ID).setBehaviorId(REMOVING_BEHAVIOR_ID);
        StratConPointOfInterestBehaviors.registerBehavior(REMOVING_BEHAVIOR_ID, new IStratConPointOfInterestBehavior() {
            @Override
            public int getScenarioOddsModifier(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
                return -25;
            }
        });
        place(DANGER_TYPE_ID, 1, 1);

        assertEquals(-25, track.getScenarioOddsAdjustment());
    }

    @Test
    void undefinedPointsOfInterestHaveNoSectorWideEffect() {
        StratConPointOfInterest undefined = place("UnitTestRulesUndefined", 1, 1);
        undefined.setOwner(ForceAlignment.Allied);

        assertEquals(0, track.getScanRangeIncrease());
        assertEquals(0, track.getScenarioOddsAdjustment());
    }

    // Daily step and expiry

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    @Test
    void newDayRunsTheDailyHookForActivePointsOfInterestOnly() {
        StratConPointOfInterest active = place(DAILY_TYPE_ID, 1, 1);
        StratConPointOfInterest resolved = place(DAILY_TYPE_ID, 2, 2);
        resolved.setStatus(PointOfInterestStatus.RESOLVED);
        StratConPointOfInterest notYetDue = place(DAILY_TYPE_ID, 3, 3);
        notYetDue.setExpiryDate(TODAY.plusDays(1));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertEquals(List.of(active.getId(), notYetDue.getId()), newDayIds);
        assertTrue(expiredIds.isEmpty());
    }

    @Test
    void aPointOfInterestExpiresOnItsExpiryDateInsteadOfGettingItsDailyHook() {
        StratConPointOfInterest expiring = place(DAILY_TYPE_ID, 1, 1);
        expiring.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertEquals(List.of(expiring.getId()), expiredIds);
        assertTrue(newDayIds.isEmpty());
        assertEquals(PointOfInterestStatus.EXPIRED, expiring.getStatus());
        assertSame(expiring, track.getPointOfInterest(expiring.getId()), "kept on the map, marked as expired");
    }

    @Test
    void anExpiredPointOfInterestIsRemovedWhenItsDefinitionSaysSo() {
        StratConPointOfInterestDefinitions.getDefinition(DAILY_TYPE_ID).setRemoveOnExpiry(true);
        StratConPointOfInterest expiring = place(DAILY_TYPE_ID, 1, 1);
        expiring.setExpiryDate(TODAY.minusDays(3));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(expiring.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, expiring.getStatus());
    }

    @Test
    void anExpiryHookMayResolveThePointOfInterestInstead() {
        StratConPointOfInterestDefinitions.getDefinition(DAILY_TYPE_ID).setRemoveOnExpiry(true);
        statusToSetOnExpiry = PointOfInterestStatus.RESOLVED;
        StratConPointOfInterest expiring = place(DAILY_TYPE_ID, 1, 1);
        expiring.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertEquals(PointOfInterestStatus.RESOLVED, expiring.getStatus());
        assertSame(expiring, track.getPointOfInterest(expiring.getId()), "a resolved point of interest is not removed");
    }

    @Test
    void expiryIsReportedOnlyWhenThePlayerCouldSeeIt() {
        Campaign campaign = dailyCampaign();
        StratConPointOfInterestDefinitions.getDefinition(DAILY_TYPE_ID).setHiddenUntilScouted(true);
        StratConPointOfInterest hidden = place(DAILY_TYPE_ID, 1, 1);
        hidden.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);
        verify(campaign, never()).addReport(eq(GENERAL), anyString());

        StratConPointOfInterest seen = place(DAILY_TYPE_ID, 2, 2);
        seen.setRevealed(true);
        seen.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    // Deployment

    @Test
    void deploymentOutcomesCombineWithSuppressionWinning() {
        PointOfInterestDeploymentOutcome none = PointOfInterestDeploymentOutcome.NO_EFFECT;
        PointOfInterestDeploymentOutcome force = PointOfInterestDeploymentOutcome.FORCE_SCENARIO;
        PointOfInterestDeploymentOutcome suppress = PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;

        assertEquals(force, none.combineWith(force));
        assertEquals(force, force.combineWith(none));
        assertEquals(suppress, force.combineWith(suppress));
        assertEquals(suppress, suppress.combineWith(force));
        assertEquals(none, none.combineWith(null));
    }

    @Test
    void deploymentAsksEveryActivePointOfInterestOnTheHex() {
        deploymentOutcome = PointOfInterestDeploymentOutcome.FORCE_SCENARIO;
        StratConPointOfInterest first = place(DAILY_TYPE_ID, 1, 1);
        StratConPointOfInterest second = place(DAILY_TYPE_ID, 1, 1);
        StratConPointOfInterest spent = place(DAILY_TYPE_ID, 1, 1);
        spent.setStatus(PointOfInterestStatus.EXPIRED);
        place(DAILY_TYPE_ID, 2, 2);

        PointOfInterestDeploymentOutcome outcome = StratConPointOfInterestRules.processFormationDeployment(track,
              new StratConCoords(1, 1),
              1,
              mock(Campaign.class));

        assertEquals(PointOfInterestDeploymentOutcome.FORCE_SCENARIO, outcome);
        assertEquals(List.of(first.getId(), second.getId()), deployedIds,
              "only the active points of interest on the deployed hex are asked");
    }

    @Test
    void deploymentOntoAHexThatAlreadyHoldsAScenarioAsksNoPointOfInterest() {
        deploymentOutcome = PointOfInterestDeploymentOutcome.FORCE_SCENARIO;
        StratConCoords coords = new StratConCoords(1, 1);
        place(DAILY_TYPE_ID, 1, 1);
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(coords);
        track.getScenarios().put(coords, scenario);

        PointOfInterestDeploymentOutcome outcome = StratConPointOfInterestRules.processFormationDeployment(track,
              coords,
              1,
              mock(Campaign.class));

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, outcome,
              "the formation joins the scenario already there");
        assertTrue(deployedIds.isEmpty(), "no point of interest may place a second scenario on the hex");
    }

    @Test
    void onceAPointOfInterestPlacesAScenarioNoOtherOnTheHexIsAsked() {
        deploymentOutcome = PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        isPlacingScenarioOnDeployment = true;
        StratConPointOfInterest first = place(DAILY_TYPE_ID, 1, 1);
        place(DAILY_TYPE_ID, 1, 1);

        StratConPointOfInterestRules.processFormationDeployment(track, new StratConCoords(1, 1), 1,
              mock(Campaign.class));

        assertEquals(List.of(first.getId()), deployedIds, "a second scenario would push the first off the map");
    }

    @Test
    void onlyAPointOfInterestStillInPlayCountsAsActiveOnItsHex() {
        StratConCoords coords = new StratConCoords(1, 1);
        StratConPointOfInterest spent = place(DAILY_TYPE_ID, 1, 1);
        spent.setStatus(PointOfInterestStatus.RESOLVED);

        assertFalse(StratConPointOfInterestRules.hasActivePointOfInterest(track, coords));

        place(DAILY_TYPE_ID, 1, 1);
        assertTrue(StratConPointOfInterestRules.hasActivePointOfInterest(track, coords));
        assertFalse(StratConPointOfInterestRules.hasActivePointOfInterest(track, new StratConCoords(2, 2)));
    }

    @Test
    void deploymentOntoAHexWithoutPointsOfInterestHasNoEffect() {
        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT,
              StratConPointOfInterestRules.processFormationDeployment(track,
                    new StratConCoords(1, 1),
                    1,
                    mock(Campaign.class)));
    }
}
