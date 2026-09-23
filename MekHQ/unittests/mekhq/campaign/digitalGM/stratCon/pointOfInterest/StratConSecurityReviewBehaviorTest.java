package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
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
        definition.setBehaviorId(StratConSecurityReviewBehavior.BEHAVIOR_ID);
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

    /** Checks the single general report the campaign received comes from a real resource key. */
    private static void assertGeneralReportIsValid(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        assertTrue(isResourceKeyValid(reportCaptor.getValue()), "missing resource key: " + reportCaptor.getValue());
    }

    // What differs

    @Test
    void theSecurityReviewBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConSecurityReviewBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConSecurityReviewBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConSecurityReviewBehavior.class, review.getBehavior());
    }

    @Test
    void aContestedReviewIsFoughtAsAnEngagement() {
        assertEquals("Engagement.json", new StratConSecurityReviewBehavior().getScenarioTemplateName());
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
