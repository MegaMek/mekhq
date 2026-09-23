package mekhq.campaign.digitalGM.stratCon;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for Escalation: which contracts track it, its maximum, what raises it and by how much, its part in the morale
 * check, and the Diversionary Raid's Escalation objective.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConEscalationTest {
    private static final int SCALE = 2;

    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        campaignState = new StratConCampaignState();
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        campaignState.addTrack(track);
    }

    private static Campaign campaign(boolean isContractsUseSpecialMechanics) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(isContractsUseSpecialMechanics);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    private AbstractContract contract(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getScale()).thenReturn(SCALE);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        return contract;
    }

    private StratConStrategicObjective escalationObjective() {
        for (StratConStrategicObjective objective : campaignState.getTrack(0).getStrategicObjectives()) {
            if (objective.getObjectiveType() == StrategicObjectiveType.Escalation) {
                return objective;
            }
        }
        return null;
    }

    // Which contracts track Escalation

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class,
          names = { "DIVERSIONARY_RAID", "EXTRACTION_RAID", "OBJECTIVE_RAID", "OBSERVATION_RAID", "RECON_RAID",
                    "GUERRILLA_WARFARE", "ASSASSINATION" })
    void raidsAndGuerrillaOperationsTrackEscalation(ContractObjectiveType objectiveType) {
        assertTrue(StratConEscalation.isEscalationContract(contract(objectiveType)));
        assertTrue(StratConEscalation.isEscalationUsed(campaign(true), contract(objectiveType)));
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, names = { "GARRISON_DUTY", "ESPIONAGE", "UNDEFINED" })
    void otherContractsDoNotTrackEscalation(ContractObjectiveType objectiveType) {
        assertFalse(StratConEscalation.isEscalationContract(contract(objectiveType)));
    }

    @Test
    void escalationBelongsToTheSpecialMechanicsOption() {
        assertFalse(StratConEscalation.isEscalationUsed(campaign(false),
              contract(ContractObjectiveType.DIVERSIONARY_RAID)));
    }

    @Test
    void aContractWithoutAStratConStateOrObjectiveTypeDoesNotTrackEscalation() {
        AbstractContract withoutState = contract(ContractObjectiveType.DIVERSIONARY_RAID);
        when(withoutState.getStratConCampaignState()).thenReturn(null);

        assertFalse(StratConEscalation.isEscalationUsed(campaign(true), withoutState));
        assertFalse(StratConEscalation.isEscalationUsed(campaign(true), null));
        assertFalse(StratConEscalation.isEscalationContract(contract(null)));
    }

    // Its range

    @Test
    void escalationRunsToOneHundredPerPointOfScale() {
        assertEquals(100 * SCALE, StratConEscalation.getMaximumEscalation(contract(ContractObjectiveType.RECON_RAID)));
        assertEquals(50 * SCALE,
              StratConEscalation.getDiversionaryRaidTarget(contract(ContractObjectiveType.DIVERSIONARY_RAID)));
    }

    @Test
    void escalationIsCappedAtItsMaximum() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.increaseEscalation(campaign(true), contract, 1000);

        assertEquals(100 * SCALE, campaignState.getEscalation());
    }

    @Test
    void escalationOnlyEverRises() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(10);

        StratConEscalation.increaseEscalation(campaign(true), contract, -5);
        StratConEscalation.increaseEscalation(campaign(true), contract, 0);

        assertEquals(10, campaignState.getEscalation());
    }

    @Test
    void escalationDoesNotRiseOnContractsThatDoNotTrackIt() {
        StratConEscalation.increaseEscalation(campaign(false), contract(ContractObjectiveType.RECON_RAID), 5);
        StratConEscalation.increaseEscalation(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY), 5);

        assertEquals(0, campaignState.getEscalation());
    }

    // What raises it

    @Test
    void deployingToAnEmptyHexRaisesEscalationByOne() {
        StratConEscalation.onEmptyHexDeployment(campaign(true), contract(ContractObjectiveType.RECON_RAID));

        assertEquals(1, campaignState.getEscalation());
    }

    @Test
    void resolvedScenariosRollTheirDice() {
        assertEquals(0, StratConEscalation.getScenarioEscalationDice(false, false), "a lost scenario adds nothing");
        assertEquals(1, StratConEscalation.getScenarioEscalationDice(true, false), "a won scenario adds 1d6");
        assertEquals(2, StratConEscalation.getScenarioEscalationDice(false, true), "a lost facility fight adds 2d6");
        assertEquals(3, StratConEscalation.getScenarioEscalationDice(true, true),
              "a won facility fight adds 3d6, in place of the 1d6 for winning");
    }

    @Test
    void aWonFacilityFightRaisesEscalationByThreeToEighteen() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.onScenarioCompleted(campaign(true), contract, true, true);

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
    }

    @Test
    void aLostScenarioAwayFromAFacilityRaisesNoEscalation() {
        StratConEscalation.onScenarioCompleted(campaign(true), contract(ContractObjectiveType.RECON_RAID), false,
              false);

        assertEquals(0, campaignState.getEscalation());
    }

    @Test
    void strikingAHighProfileTargetRaisesEscalationByThreeToEighteen() {
        StratConEscalation.onHighProfileTargetStruck(campaign(true), contract(ContractObjectiveType.DIVERSIONARY_RAID));

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
    }

    // The morale check

    @Test
    void aRollNoHigherThanEscalationRaisesMorale() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 40);

        verify(contract).changeMorale(ContractMoraleLevel.ADVANCING);
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void aRollAboveEscalationLeavesMoraleAlone() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 41);

        verify(contract, never()).changeMorale(any(ContractMoraleLevel.class));
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void noEscalationNeverRaisesMorale() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.applyMoraleRoll(contract, 1);

        verify(contract, never()).changeMorale(any(ContractMoraleLevel.class));
    }

    @Test
    void moraleRisesOneLevelAndNoFurtherThanOverwhelming() {
        assertEquals(ContractMoraleLevel.CRITICAL, StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.ROUTED));
        assertEquals(ContractMoraleLevel.ADVANCING,
              StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.STALEMATE));
        assertEquals(ContractMoraleLevel.OVERWHELMING,
              StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.OVERWHELMING));
    }

    @Test
    void contractsThatDoNotTrackEscalationRollNothing() {
        assertNull(StratConEscalation.rollForMorale(campaign(false), contract(ContractObjectiveType.RECON_RAID)));
    }

    // The Diversionary Raid objective

    @Test
    void aDiversionaryRaidMustReachFiftyEscalationPerPointOfScale() {
        StratConEscalation.addDiversionaryRaidObjective(contract(ContractObjectiveType.DIVERSIONARY_RAID),
              campaignState);

        StratConStrategicObjective objective = escalationObjective();
        assertEquals(50 * SCALE, objective.getDesiredObjectiveCount());
        assertEquals(50 * SCALE, StratConEscalation.getEscalationTarget(campaignState));
        assertFalse(objective.isObjectiveCompleted(campaignState.getTrack(0)));
        assertFalse(objective.isObjectiveFailed(campaignState.getTrack(0)), "short of the target is not a failure");
    }

    @Test
    void theObjectiveFollowsEscalationUntilItIsMet() {
        AbstractContract contract = contract(ContractObjectiveType.DIVERSIONARY_RAID);
        Campaign campaign = campaign(true);
        StratConEscalation.addDiversionaryRaidObjective(contract, campaignState);
        StratConStrategicObjective objective = escalationObjective();
        StratConTrackState track = campaignState.getTrack(0);

        StratConEscalation.increaseEscalation(campaign, contract, 30);
        assertEquals(30, objective.getCurrentObjectiveCount());
        assertFalse(objective.isObjectiveCompleted(track));

        StratConEscalation.increaseEscalation(campaign, contract, 150);
        assertEquals(50 * SCALE, objective.getCurrentObjectiveCount(), "capped at the target");
        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void aContractWithoutAnEscalationObjectiveHasNoTarget() {
        assertNull(StratConEscalation.getEscalationTarget(campaignState));
    }
}
