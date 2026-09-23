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

import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.DROPSHIP;
import static megamek.common.units.UnitType.INFANTRY;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests for beleaguered forces: relieving them when no scenario breaks out (by any formation), winning or losing the
 * Relief Column fought to reach them, never paying the combat bonus (their contract keeps its Essential scenarios),
 * their visibility, and their rolled lifespan.
 *
 * <p>A scenario breaking out - a Relief Column being placed over them - builds a full scenario from a template, so it is
 * not covered here; its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded} and the daily
 * step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConBeleagueredForcesBehaviorTest {
    private static final String TYPE_ID = "UnitTestBeleagueredForces";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords FORCES_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest beleagueredForces;
    private StratConStrategicObjective objective;
    // the active contract of the campaign built by deploymentCampaign
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConBeleagueredForcesBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(3);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        beleagueredForces = new StratConPointOfInterest(TYPE_ID, FORCES_COORDS);
        assertTrue(track.addPointOfInterest(beleagueredForces), "test setup: the forces should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, beleagueredForces);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With Essential Scenarios Only on, no scenario can break out.
     */
    private Campaign deploymentCampaign(int primaryUnitType, ContractMoraleLevel moraleLevel,
          boolean essentialScenariosOnly) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(essentialScenariosOnly);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(moraleLevel);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              Money.of(25000)));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, FORCES_COORDS, FORMATION_ID, campaign);
    }

    private void assertRelieved() {
        assertNull(track.getPointOfInterest(beleagueredForces.getId()), "relieved forces leave the map");
        assertEquals(PointOfInterestStatus.RESOLVED, beleagueredForces.getStatus());
        assertTrue(objective.isObjectiveCompleted(track));
        assertFalse(objective.isObjectiveFailed(track));
    }

    private void assertOverrun() {
        assertNull(track.getPointOfInterest(beleagueredForces.getId()), "overrun forces leave the map");
        assertTrue(track.getStrategicObjectives().contains(objective), "the objective stays, failed");
        assertFalse(objective.isObjectiveCompleted(track));
        assertTrue(objective.isObjectiveFailed(track));
    }

    // Registration

    @Test
    void theBeleagueredForcesBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConBeleagueredForcesBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConBeleagueredForcesBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConBeleagueredForcesBehavior.class, beleagueredForces.getBehavior());
    }

    @Test
    void theObjectiveIsDescribedAsRelievingTheForces() {
        String description = beleagueredForces.getBehavior().getObjectiveDescription(beleagueredForces, track);

        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    // Relieving them

    @ParameterizedTest
    @ValueSource(ints = { MEK, INFANTRY, AEROSPACE_FIGHTER, DROPSHIP })
    void anyFormationRelievesThemWhenNoScenarioBreaksOut(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertRelieved();
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void relievingThemWithoutAFightPaysNoCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void aRoutedEnemyCannotStopTheRelief() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.ROUTED, false);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertRelieved();
    }

    @Test
    void aFormationJoiningTheReliefColumnDoesNotRollAgain() {
        beleagueredForces.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(beleagueredForces, track.getPointOfInterest(beleagueredForces.getId()));
        assertEquals(SCENARIO_ID, beleagueredForces.getLinkedScenarioId());
    }

    // The Relief Column fought to reach them

    @Test
    void winningTheReliefColumnRelievesThemButPaysNoCombatBonus() {
        beleagueredForces.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertRelieved();
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void losingTheReliefColumnLeavesThemOverrun() {
        beleagueredForces.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertOverrun();
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void aReliefColumnLeftUnplayedLeavesThemOverrun() {
        beleagueredForces.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertOverrun();
    }

    // On the map

    @Test
    void beleagueredForcesAreVisibleWithoutBeingScouted() {
        assertFalse(beleagueredForces.isRevealed());
        assertTrue(beleagueredForces.isVisibleToPlayer(track));
    }

    @Test
    void beleagueredForcesNotRelievedInTimeAreOverrun() {
        beleagueredForces.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertEquals(PointOfInterestStatus.EXPIRED, beleagueredForces.getStatus());
        assertOverrun();
    }

    @Test
    void beleagueredForcesWaitingOnTheirReliefColumnAreNotOverrun() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(FORCES_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        beleagueredForces.setLinkedScenarioId(SCENARIO_ID);
        beleagueredForces.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(beleagueredForces, track.getPointOfInterest(beleagueredForces.getId()));
        assertTrue(beleagueredForces.isActive());
    }

    @Test
    void beleagueredForcesHoldOutOneToThreeDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);
        boolean[] rolled = new boolean[4];

        for (int attempt = 0; attempt < 300; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, FORCES_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 3), "rolled a lifespan of " + lifespanDays);
            rolled[lifespanDays] = true;
        }

        for (int lifespanDays = 1; lifespanDays <= 3; lifespanDays++) {
            assertTrue(rolled[lifespanDays], "a lifespan of " + lifespanDays + " should come up in 300 rolls");
        }
    }
}
