/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.camOpsReputation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
        when(campaign.getCommander()).thenReturn(null);
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
                                           AverageExperienceRating.getSkillLevel(
                                                 campaign,
                                                 true))
              .thenReturn(SkillLevel.VETERAN);
        averageExperienceRating.when(() ->
                                           AverageExperienceRating.getAverageExperienceModifier(
                                                 SkillLevel.VETERAN))
              .thenReturn(20);
        commandRating.when(() ->
                                 CommandRating.calculateCommanderRating(campaign, null))
              .thenReturn(Collections.singletonMap("total", 3));
        combatRecordRating.when(() ->
                                      CombatRecordRating.calculateCombatRecordRating(
                                            campaign))
              .thenReturn(Collections.singletonMap("total", 3));

        List<Map<String, Integer>> transportationData = new ArrayList<>();
        transportationData.add(Collections.singletonMap("total", 3));
        transportationData.add(Collections.singletonMap("total", 3));
        transportationData.add(Collections.singletonMap("total", 3));
        transportationRating.when(() ->
                                        TransportationRating.calculateTransportationRating(
                                              campaign))
              .thenReturn(transportationData);

        Map<String, Map<String, ?>> supportData = new HashMap<>();
        supportData.put("total", Collections.singletonMap("total", 3));
        supportRating.when(() ->
                                 SupportRating.calculateSupportRating(campaign,
                                       transportationData.get(1)))
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
                                           AverageExperienceRating.getSkillLevel(
                                                 campaign,
                                                 true))
              .thenReturn(SkillLevel.ULTRA_GREEN);
        averageExperienceRating.when(() ->
                                           AverageExperienceRating.getAverageExperienceModifier(
                                                 SkillLevel.ULTRA_GREEN))
              .thenReturn(5);
        commandRating.when(() ->
                                 CommandRating.calculateCommanderRating(campaign, null))
              .thenReturn(Collections.singletonMap("total", 0));
        combatRecordRating.when(() ->
                                      CombatRecordRating.calculateCombatRecordRating(
                                            campaign))
              .thenReturn(Collections.singletonMap("total", 0));

        List<Map<String, Integer>> transportationData = new ArrayList<>();
        transportationData.add(Collections.singletonMap("total", 0));
        transportationData.add(Collections.singletonMap("total", 0));
        transportationData.add(Collections.singletonMap("total", 0));
        transportationRating.when(() ->
                                        TransportationRating.calculateTransportationRating(
                                              campaign))
              .thenReturn(transportationData);

        Map<String, Map<String, ?>> supportData = new HashMap<>();
        supportData.put("total", Collections.singletonMap("total", 0));
        supportRating.when(() ->
                                 SupportRating.calculateSupportRating(campaign,
                                       transportationData.get(1)))
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
