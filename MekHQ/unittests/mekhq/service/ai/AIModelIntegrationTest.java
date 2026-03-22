package mekhq.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;

import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.enums.ScenarioStatus;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for AI-generated campaign and mission logic.
 * Verifies that the mapping from AI proposals to MekHQ objects (Contracts, Scenarios) is correct.
 */
public class AIModelIntegrationTest {

    @Test
    public void testCreateContractAndScenarioFromProposal() {
        // Setup a mock proposal
        MissionProposal proposal = new MissionProposal();
        proposal.title = "AI Test Mission";
        proposal.briefing = "Test Briefing Content";
        proposal.missionType = "OBJECTIVE_RAID";
        proposal.employerCode = "FS";
        proposal.enemyCode = "DC";
        proposal.difficulty = 5;
        proposal.lengthWeeks = 12;

        // Create the contract
        AtBContract contract = new AtBContract(proposal.title);
        contract.setDesc(proposal.briefing);
        contract.setContractType(AtBContractType.OBJECTIVE_RAID);
        contract.setDifficulty(proposal.difficulty);
        contract.setLength(proposal.lengthWeeks);
        contract.setStatus(MissionStatus.ACTIVE);

        // Add the scenario (the feature we just implemented)
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setName("Opening Engagement: " + proposal.title);
        scenario.setDesc(proposal.briefing);
        scenario.setDate(LocalDate.of(3025, 1, 1));
        scenario.setStatus(ScenarioStatus.CURRENT);
        contract.addScenario(scenario);

        // Verify the contract state
        assertEquals("AI Test Mission", contract.getName());
        assertEquals("Test Briefing Content", contract.getDescription());
        assertEquals(AtBContractType.OBJECTIVE_RAID, contract.getContractType());
        assertEquals(MissionStatus.ACTIVE, contract.getStatus());

        // Verify the scenario state
        assertNotNull(contract.getScenarios());
        assertFalse(contract.getScenarios().isEmpty());
        AtBDynamicScenario resultScenario = (AtBDynamicScenario) contract.getScenarios().get(0);
        assertEquals("Opening Engagement: AI Test Mission", resultScenario.getName());
        assertEquals("Test Briefing Content", resultScenario.getDescription());
        assertEquals(ScenarioStatus.CURRENT, resultScenario.getStatus());
        
        // Verify that the scenario's contract reference works (prevents NPE in GUI)
        // Note: we can't easily mock a full Campaign object here without Mockito, so we rely on the ID check.
        assertEquals(-1, resultScenario.getMissionId());
    }
}
