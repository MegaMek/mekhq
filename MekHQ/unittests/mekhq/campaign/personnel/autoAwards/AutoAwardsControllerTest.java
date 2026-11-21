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
package mekhq.campaign.personnel.autoAwards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AwardBonus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AutoAwardsControllerTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 1);

    private static final String AWARD_SET = "AutoAwardsControllerTestSet";
    private static final String FIRST_PROMOTION = "First Promotion";
    private static final String SECOND_PROMOTION = "Second Promotion";
    private static final String SERVICE_AWARD = "Service Award";

    @BeforeAll
    static void loadAwards() {
        String awardsXml = """
              <?xml version="1.0"?>
              <awards>
                  <award><name>First Promotion</name><item>Rank</item><range>Promotion</range></award>
                  <award><name>Second Promotion</name><item>Rank</item><range>Promotion</range></award>
                  <award><name>Service Award</name><item>Time</item><range>1</range></award>
              </awards>
              """.strip();
        AwardsFactory.getInstance()
              .loadAwardsFromStream(new ByteArrayInputStream(awardsXml.getBytes(StandardCharsets.UTF_8)), AWARD_SET);
    }

    private static Campaign campaignWithoutAwardBonuses() {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.AWARD_BONUS_STYLE)).thenReturn(AwardBonus.NONE);
        when(campaignOptions.get(CampaignOption.USE_REPLACE_EDGE_AWARDS)).thenReturn(false);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(START);
        return campaign;
    }

    private static Award award(String name) {
        return AwardsFactory.getInstance().generateNew(AWARD_SET, name);
    }

    private static Person personWith(Campaign campaign, String... awardNames) {
        Person person = new Person("Given", "Surname", null, "MERC");
        for (String awardName : awardNames) {
            AutoAwardsController.issueAward(campaign, person, award(awardName));
        }
        return person;
    }

    @Test
    void issueAwardReplacesAnEarlierPromotionAward() {
        Campaign campaign = campaignWithoutAwardBonuses();
        Person person = personWith(campaign, FIRST_PROMOTION, SERVICE_AWARD);

        AutoAwardsController.issueAward(campaign, person, award(SECOND_PROMOTION));

        assertTrue(person.getAwardController().hasAward(AWARD_SET, SECOND_PROMOTION));
        assertFalse(person.getAwardController().hasAward(AWARD_SET, FIRST_PROMOTION),
              "a new promotion award must replace the old one");
        assertTrue(person.getAwardController().hasAward(AWARD_SET, SERVICE_AWARD),
              "only promotion awards are replaced");
        assertEquals(2, person.getAwardController().getAwards().size());
    }

    @Test
    void issueAwardKeepsThePromotionAwardWhenTheNewAwardIsNotAPromotion() {
        Campaign campaign = campaignWithoutAwardBonuses();
        Person person = personWith(campaign, FIRST_PROMOTION);

        AutoAwardsController.issueAward(campaign, person, award(SERVICE_AWARD));

        assertTrue(person.getAwardController().hasAward(AWARD_SET, FIRST_PROMOTION));
        assertTrue(person.getAwardController().hasAward(AWARD_SET, SERVICE_AWARD));
    }

    private static AbstractContract contractEnding(LocalDate endDate) {
        AbstractContract contract = new ShellContract();
        contract.setScheduleData(new ContractScheduleData(START, endDate, 0));
        return contract;
    }

    @Test
    void lastContractEndingDateIsTheLatestEndDate() {
        List<AbstractContract> completedContracts = List.of(contractEnding(START.plusMonths(3)),
              contractEnding(START.plusMonths(9)), contractEnding(START.plusMonths(6)));

        assertEquals(START.plusMonths(9), AutoAwardsController.getLastContractEndingDate(completedContracts));
    }

    @Test
    void lastContractEndingDateSkipsContractsWithoutAnEndDate() {
        List<AbstractContract> completedContracts = List.of(contractEnding(START.plusMonths(3)),
              contractEnding(null));

        assertEquals(START.plusMonths(3), AutoAwardsController.getLastContractEndingDate(completedContracts),
              "an open-ended contract must not break the comparison");
    }

    @Test
    void lastContractEndingDateIsNullWhenNoContractHasAnEndDate() {
        assertNull(AutoAwardsController.getLastContractEndingDate(List.of(contractEnding(null))));
    }

    @ParameterizedTest
    @CsvSource({ "Individual, true, false, true", "Individual, false, true, false", "Lance, false, true, true",
                 "Company, true, false, false", "'', true, false, true" })
    void killAwardFollowsItsOwnTrackingOption(String size, boolean isIndividualTracked, boolean isFormationTracked,
          boolean expected) {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS, isIndividualTracked);
        campaignOptions.set(CampaignOption.ENABLE_FORMATION_KILL_AWARDS, isFormationTracked);

        Award award = new Award("Kill Award", AWARD_SET, "", "", List.of(), List.of(), List.of(), 0, 0, false, 5,
              "Kill", size, "Mission", 0);

        assertEquals(expected, AutoAwardsController.isKillAwardTracked(campaignOptions, award));
    }

    @Test
    void graduationAwardsWaitForAutoAwardsToBeEnabled() {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.ENABLE_AUTO_AWARDS, false);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        new AutoAwardsController().PostGraduationController(campaign, List.of(UUID.randomUUID()), new HashMap<>());

        verify(campaign, only()).getCampaignOptions();
    }
}
