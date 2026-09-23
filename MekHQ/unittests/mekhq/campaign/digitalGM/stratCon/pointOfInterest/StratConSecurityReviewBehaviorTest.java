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
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.SECURITY_REVIEW;
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
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the security review, which shares the contested rules of the data cache (see
 * {@link StratConDataCacheBehaviorTest} for those in depth): these cover what differs - any formation can carry one
 * out, a fight is an Engagement, it pays no combat bonus, and its text and visibility - and that each of its outcomes
 * plays out.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConSecurityReviewBehaviorTest {
    private static final String TYPE_ID = "UnitTestSecurityReview";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords REVIEW_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest review;
    private StratConStrategicObjective objective;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(SECURITY_REVIEW.getBehaviorId());
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        review = new StratConPointOfInterest(TYPE_ID, REVIEW_COORDS);
        assertTrue(track.addPointOfInterest(review), "test setup: the review should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, review);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With odds no roll can meet, no scenario can break out.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        // Odds no roll can meet, so no scenario can break out.
        track.setScenarioOdds(-100);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.setContractsUseSpecialMechanics(true);
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.SECURITY_DUTY);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              Money.of(25000)));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    /** Checks the single general report the campaign received comes from a real resource key. */
    private static void assertGeneralReportIsValid(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        assertTrue(isResourceKeyValid(reportCaptor.getValue()), "missing resource key: " + reportCaptor.getValue());
    }

    // What differs

    @Test
    void theSecurityReviewBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConContestedPointOfInterestBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(SECURITY_REVIEW.getBehaviorId()));
        assertInstanceOf(StratConContestedPointOfInterestBehavior.class, review.getBehavior());
    }

    @Test
    void aContestedReviewIsFoughtAsAnEngagement() {
        assertEquals("Engagement.json", SECURITY_REVIEW.createBehavior().getScenarioTemplateName());
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = review.getBehavior().getObjectiveDescription(review, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    @Test
    void aSecurityReviewIsVisibleWithoutBeingScouted() {
        assertTrue(review.isVisibleToPlayer(track));
    }

    // Each outcome, with its own text

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationPassesTheReviewWhenNoScenarioBreaksOutButEarnsNoBonus(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              StratConPointOfInterestRules.processFormationDeployment(track, REVIEW_COORDS, FORMATION_ID, campaign));

        assertNull(track.getPointOfInterest(review.getId()));
        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances, never()).credit(any(), any(), any(), anyString());
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void winningTheEngagementPassesTheReview() {
        review.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void losingTheEngagementFailsTheReview() {
        review.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertTrue(objective.isObjectiveFailed(track));
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void aReviewNotCarriedOutInTimeLapses() {
        review.setExpiryDate(TODAY);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertEquals(PointOfInterestStatus.EXPIRED, review.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }
}
