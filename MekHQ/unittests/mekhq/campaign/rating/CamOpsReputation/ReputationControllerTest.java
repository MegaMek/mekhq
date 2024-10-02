package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ReputationControllerTest {
    private ReputationController reputation;
    private Campaign campaign;
    private MockedStatic<AverageExperienceRating> averageExperienceRating;
    private MockedStatic<CommandRating> commandRating;
    private MockedStatic<CombatRecordRating> combatRecordRating;
    private MockedStatic<TransportationRating> transportationRating;
    private MockedStatic<SupportRating> supportRating;
    private MockedStatic<FinancialRating> financialRating;
    private MockedStatic<CrimeRating> crimeRating;
    private MockedStatic<OtherModifiers> otherModifiersRating;

    @BeforeEach
    void setUp() {
        reputation = new ReputationController();
        campaign = mock(Campaign.class);
        when(campaign.getFlaggedCommander()).thenReturn(null);
        when(campaign.getFinances()).thenReturn(null);
        when(campaign.getDateOfLastCrime()).thenReturn(null);
        averageExperienceRating = mockStatic(AverageExperienceRating.class);
        commandRating = mockStatic(CommandRating.class);
        combatRecordRating = mockStatic(CombatRecordRating.class);
        transportationRating = mockStatic(TransportationRating.class);
        supportRating = mockStatic(SupportRating.class);
        financialRating = mockStatic(FinancialRating.class);
        crimeRating = mockStatic(CrimeRating.class);
        otherModifiersRating = mockStatic(OtherModifiers.class);
    }

    @AfterEach
    void tearDown() {
        averageExperienceRating.close();
        commandRating.close();
        combatRecordRating.close();
        transportationRating.close();
        supportRating.close();
        financialRating.close();
        crimeRating.close();
        otherModifiersRating.close();
    }

    @Test
    void testGetReputationModifierShouldBeFour() {
        averageExperienceRating.when(() ->
            AverageExperienceRating.getSkillLevel(campaign, true))
            .thenReturn(SkillLevel.VETERAN);
        averageExperienceRating.when(() ->
            AverageExperienceRating.getAverageExperienceModifier(SkillLevel.VETERAN))
            .thenReturn(20);
        averageExperienceRating.when(() ->
            AverageExperienceRating.getAtBModifier(campaign))
            .thenReturn(3);
        commandRating.when(() ->
            CommandRating.calculateCommanderRating(campaign, null))
            .thenReturn(Collections.singletonMap("total", 3));
        combatRecordRating.when(() ->
            CombatRecordRating.calculateCombatRecordRating(campaign))
            .thenReturn(Collections.singletonMap("total", 3));

        List<Map<String, Integer>> transportationData = new ArrayList<>();
        transportationData.add(Collections.singletonMap("total", 3));
        transportationData.add(Collections.singletonMap("total", 3));
        transportationData.add(Collections.singletonMap("total", 3));
        transportationRating.when(() ->
            TransportationRating.calculateTransportationRating(campaign))
            .thenReturn(transportationData);

        Map<String, Map<String, ?>> supportData = new HashMap<>();
        supportData.put("total", Collections.singletonMap("total", 3));
        supportRating.when(() ->
            SupportRating.calculateSupportRating(campaign, transportationData.get(1)))
            .thenReturn(supportData);

        financialRating.when(() ->
            FinancialRating.calculateFinancialRating(campaign.getFinances()))
            .thenReturn(Collections.singletonMap("total", 3));

        crimeRating.when(() ->
            CrimeRating.calculateCrimeRating(campaign))
            .thenReturn(Collections.singletonMap("total", 3));

        otherModifiersRating.when(() ->
            OtherModifiers.calculateOtherModifiers(campaign))
            .thenReturn(Collections.singletonMap("total", 3));

        reputation.initializeReputation(campaign);
        assertEquals(41, reputation.getReputationRating());
        assertEquals(4, reputation.getReputationModifier());
    }

    @Test
    void testGetReputationModifierShouldBeZero() {
        averageExperienceRating.when(() ->
                AverageExperienceRating.getSkillLevel(campaign, true))
            .thenReturn(SkillLevel.ULTRA_GREEN);
        averageExperienceRating.when(() ->
                AverageExperienceRating.getAverageExperienceModifier(SkillLevel.ULTRA_GREEN))
            .thenReturn(5);
        averageExperienceRating.when(() ->
                AverageExperienceRating.getAtBModifier(campaign))
            .thenReturn(0);
        commandRating.when(() ->
                CommandRating.calculateCommanderRating(campaign, null))
            .thenReturn(Collections.singletonMap("total", 0));
        combatRecordRating.when(() ->
                CombatRecordRating.calculateCombatRecordRating(campaign))
            .thenReturn(Collections.singletonMap("total", 0));

        List<Map<String, Integer>> transportationData = new ArrayList<>();
        transportationData.add(Collections.singletonMap("total", 0));
        transportationData.add(Collections.singletonMap("total", 0));
        transportationData.add(Collections.singletonMap("total", 0));
        transportationRating.when(() ->
                TransportationRating.calculateTransportationRating(campaign))
            .thenReturn(transportationData);

        Map<String, Map<String, ?>> supportData = new HashMap<>();
        supportData.put("total", Collections.singletonMap("total", 0));
        supportRating.when(() ->
                SupportRating.calculateSupportRating(campaign, transportationData.get(1)))
            .thenReturn(supportData);

        financialRating.when(() ->
                FinancialRating.calculateFinancialRating(campaign.getFinances()))
            .thenReturn(Collections.singletonMap("total", 0));

        crimeRating.when(() ->
                CrimeRating.calculateCrimeRating(campaign))
            .thenReturn(Collections.singletonMap("total", 0));

        otherModifiersRating.when(() ->
                OtherModifiers.calculateOtherModifiers(campaign))
            .thenReturn(Collections.singletonMap("total", 0));

        reputation.initializeReputation(campaign);
        assertEquals(5, reputation.getReputationRating());
        assertEquals(0, reputation.getReputationModifier());
    }
}