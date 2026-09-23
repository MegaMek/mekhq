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

import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the strategic position, which shares the contested rules of the data cache and beleaguered forces (see
 * {@link StratConDataCacheBehaviorTest} for those in depth): these cover what differs - its Pivotal Engagement, its
 * text, its visibility, and paying no combat bonus - and that each of its outcomes plays out.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConStrategicPositionBehaviorTest {
    private static final String TYPE_ID = "UnitTestStrategicPosition";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords POSITION_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest position;
    private StratConStrategicObjective objective;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConStrategicPositionBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        position = new StratConPointOfInterest(TYPE_ID, POSITION_COORDS);
        assertTrue(track.addPointOfInterest(position), "test setup: the position should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, position);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With Essential Scenarios Only on, no scenario can break out.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(true);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              Money.of(25000)));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, POSITION_COORDS, FORMATION_ID,
              campaign);
    }

    /** Checks the single general report the campaign received comes from a real resource key. */
    private static void assertGeneralReportIsValid(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        assertTrue(isResourceKeyValid(reportCaptor.getValue()), "missing resource key: " + reportCaptor.getValue());
    }

    // What differs

    @Test
    void theStrategicPositionBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConStrategicPositionBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConStrategicPositionBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConStrategicPositionBehavior.class, position.getBehavior());
    }

    @Test
    void aContestedPositionIsFoughtAsAPivotalEngagement() {
        assertEquals("Pivotal Engagement.json", new StratConStrategicPositionBehavior().getScenarioTemplateName());
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = position.getBehavior().getObjectiveDescription(position, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    @Test
    void aStrategicPositionIsVisibleWithoutBeingScouted() {
        assertTrue(position.isVisibleToPlayer(track));
    }

    // Each outcome, with its own text

    @Test
    void aFormationCapturesThePositionWhenNoScenarioBreaksOut() {
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));

        assertNull(track.getPointOfInterest(position.getId()));
        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances, never()).credit(any(), any(), any(), anyString());
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void winningThePivotalEngagementCapturesThePositionButPaysNoCombatBonus() {
        position.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void losingThePivotalEngagementLosesThePosition() {
        position.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertNull(track.getPointOfInterest(position.getId()));
        assertTrue(objective.isObjectiveFailed(track));
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void aPositionNotTakenInTimeIsLost() {
        position.setExpiryDate(TODAY);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertEquals(PointOfInterestStatus.EXPIRED, position.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }
}
