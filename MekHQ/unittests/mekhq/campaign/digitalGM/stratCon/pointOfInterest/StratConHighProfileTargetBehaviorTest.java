package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
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
 * Tests for the high profile target: striking it when no scenario breaks out (by any formation, for 3d6 Escalation
 * but no combat bonus - its contract keeps its Essential scenarios), its ambush being fought as a Decoy Engagement and
 * spending the target whatever the result, its visibility, and its rolled lifespan. High profile targets are not
 * strategic objectives: a Diversionary Raid's objective is its Escalation.
 *
 * <p>The ambush itself builds a full scenario from a template and shows the ambush dialog, so it is not covered here;
 * its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded} and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConHighProfileTargetBehaviorTest {
    private static final String TYPE_ID = "UnitTestHighProfileTarget";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords TARGET_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest target;
    // the StratCon state of the contract in the campaign built by deploymentCampaign
    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setDisplayableName("High Profile Target");
        definition.setBehaviorId(StratConHighProfileTargetBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        target = new StratConPointOfInterest(TYPE_ID, TARGET_COORDS);
        assertTrue(track.addPointOfInterest(target), "test setup: the target should be placed");
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active Diversionary Raid - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With Essential Scenarios Only on, no scenario can break out, so there is
     * never an ambush.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        return deploymentCampaign(primaryUnitType, true);
    }

    private Campaign deploymentCampaign(int primaryUnitType, boolean isContractsUseSpecialMechanics) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(true);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(isContractsUseSpecialMechanics);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.DIVERSIONARY_RAID);
        when(contract.getScale()).thenReturn(1);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
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
        return StratConPointOfInterestRules.processFormationDeployment(track, TARGET_COORDS, FORMATION_ID, campaign);
    }

    /** @return the single general report the campaign received, checked to come from a real resource key */
    private static String generalReport(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        String report = reportCaptor.getValue();
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
        return report;
    }

    private void assertSpent() {
        assertNull(track.getPointOfInterest(target.getId()), "a spent target leaves the map");
        assertFalse(target.hasLinkedScenario());
    }

    // Registration and scenario

    @Test
    void theHighProfileTargetBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConHighProfileTargetBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConHighProfileTargetBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConHighProfileTargetBehavior.class, target.getBehavior());
    }

    @Test
    void theAmbushIsFoughtAsADecoyEngagement() {
        assertEquals("Decoy Engagement.json", new StratConHighProfileTargetBehavior().getScenarioTemplateName());
        assertNull(new StratConVulnerableInfrastructureBehavior().getScenarioTemplateName(),
              "other ambush types still draw a template suited to the ambushed unit");
    }

    // Hitting it

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationHitsTheTargetWhenThereIsNoAmbush(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));

        assertNull(track.getPointOfInterest(target.getId()), "a hit target leaves the map");
        assertEquals(PointOfInterestStatus.RESOLVED, target.getStatus());
        assertTrue(generalReport(campaign).contains("enemy's"), "the report's apostrophe survives formatting");
    }

    @Test
    void hittingTheTargetPaysNoCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    // Escalation

    @Test
    void strikingTheTargetRaisesEscalationByThreeDice() {
        Campaign campaign = deploymentCampaign(MEK);

        deploy(campaign);

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
    }

    @Test
    void anAmbushedTargetRaisesNoEscalationOfItsOwn() {
        // Winning the ambush escalates the fighting as any won scenario does, when the scenario resolves; the target
        // itself adds nothing.
        target.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertEquals(0, campaignState.getEscalation());
    }

    @Test
    void withoutSpecialMechanicsStrikingTheTargetRaisesNoEscalation() {
        Campaign campaign = deploymentCampaign(MEK, false);

        deploy(campaign);

        assertEquals(0, campaignState.getEscalation());
    }

    // An ambushed target is spent whatever the result

    @Test
    void winningTheAmbushStillSpendsTheTarget() {
        target.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertSpent();
        generalReport(campaign);
    }

    @Test
    void losingTheAmbushSpendsTheTarget() {
        target.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, deploymentCampaign(MEK));

        assertSpent();
    }

    @Test
    void anAmbushLeftUnplayedSpendsTheTarget() {
        target.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSpent();
    }

    // On the map

    @Test
    void aHighProfileTargetIsVisibleWithoutBeingScouted() {
        assertFalse(target.isRevealed());
        assertTrue(target.isVisibleToPlayer(track));
    }

    @Test
    void aHighProfileTargetNotHitInTimeIsGone() {
        target.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(target.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, target.getStatus());
    }

    @Test
    void aHighProfileTargetWaitingOnItsAmbushDoesNotExpire() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(TARGET_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        target.setLinkedScenarioId(SCENARIO_ID);
        target.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(target, track.getPointOfInterest(target.getId()));
        assertTrue(target.isActive());
    }

    @Test
    void aHighProfileTargetLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);

        for (int attempt = 0; attempt < 100; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, TARGET_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
        }
    }
}
