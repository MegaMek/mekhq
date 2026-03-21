package mekhq.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for AIService JSON parsing and extraction logic.
 */
public class AIServiceTest {

    private AIService aiService;

    @BeforeEach
    public void setUp() {
        aiService = new AIService();
    }

    @Test
    public void testExtractJsonWithMarkdown() {
        String input = "Here is the JSON:\n```json\n{\"campaignName\": \"Test\"}\n```\nHope you like it.";
        String extracted = aiService.extractJson(input);
        assertEquals("{\"campaignName\": \"Test\"}", extracted.trim());
    }

    @Test
    public void testExtractJsonWithoutMarkdown() {
        String input = "{\"campaignName\": \"Test\"}";
        String extracted = aiService.extractJson(input);
        assertEquals("{\"campaignName\": \"Test\"}", extracted.trim());
    }

    @Test
    public void testParseCampaignProposalWithComments() throws Exception {
        String json = "{\n" +
                "  \"campaignName\": \"The Iron First\", // This is a comment\n" +
                "  \"mercenaryUnitName\": \"Iron Brigade\",\n" +
                "  \"startYear\": 3025,\n" +
                "  \"startingFactionCode\": \"FS\",\n" +
                "  \"startingPlanetName\": \"New Avalon\",\n" +
                "  \"backgroundStory\": \"A long story...\",\n" +
                "  \"startingFunds\": 20000000\n" +
                "}";
        
        CampaignProposal proposal = aiService.getObjectMapper().readValue(json, CampaignProposal.class);
        assertNotNull(proposal);
        assertEquals("The Iron First", proposal.campaignName);
        assertEquals(3025, proposal.startYear);
        assertEquals(20000000, proposal.startingFunds);
    }

    @Test
    public void testParseMissionProposal() throws Exception {
        String json = "{\n" +
                "  \"title\": \"Raid on Hesperus II\",\n" +
                "  \"briefing\": \"Strike the factories.\",\n" +
                "  \"missionType\": \"OBJECTIVE_RAID\",\n" +
                "  \"employerCode\": \"FS\",\n" +
                "  \"enemyCode\": \"LC\",\n" +
                "  \"planetName\": \"Hesperus II\",\n" +
                "  \"difficulty\": 5,\n" +
                "  \"lengthWeeks\": 12\n" +
                "}";
        
        MissionProposal proposal = aiService.getObjectMapper().readValue(json, MissionProposal.class);
        assertNotNull(proposal);
        assertEquals("Raid on Hesperus II", proposal.title);
        assertEquals("OBJECTIVE_RAID", proposal.missionType);
        assertEquals(5, proposal.difficulty);
    }
}
