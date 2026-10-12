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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.EnumMap;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.utilities.spaUtilities.SpaUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests {@link RecruitmentCosts}: recruits normally cost two months' salary, but when recruitment is paid for and skill
 * or SPA training costs C-bills they cost what their training would have, with each skill category and SPAs scaled by
 * their own multiplier.
 */
class RecruitmentCostsTest {
    private static final Money SALARY = Money.of(10_000);
    private static final Money PROFESSION_TRAINING = Money.of(2_000_000);
    private static final Money UTILITY_TRAINING = Money.of(1_000_000);
    private static final Money ROLEPLAY_TRAINING = Money.of(800_000);
    private static final Money OTHER_TRAINING = Money.of(1_000_000);
    /** Every skill but Roleplay, which recruitment leaves out by default */
    private static final Money SKILL_TRAINING = Money.of(4_000_000);
    private static final Money SPA_TRAINING = Money.of(2_400_000);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Person person;
    private MockedStatic<SkillTrainingCosts> skillTrainingCosts;
    private MockedStatic<SpaUtilities> spaUtilities;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_RECRUITMENT, true);
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        person = mock(Person.class);
        when(person.getSalary(any(CampaignOptions.class), anyBoolean(), any())).thenReturn(SALARY);

        skillTrainingCosts = mockStatic(SkillTrainingCosts.class);
        skillTrainingCosts.when(() -> SkillTrainingCosts.getAccumulatedSkillTrainingCosts(any(), any()))
              .thenReturn(skillTrainingByCategory(PROFESSION_TRAINING, UTILITY_TRAINING, ROLEPLAY_TRAINING,
                    OTHER_TRAINING));
        spaUtilities = mockStatic(SpaUtilities.class);
        spaUtilities.when(() -> SpaUtilities.getAccumulatedSpaTrainingCost(any())).thenReturn(SPA_TRAINING);
    }

    @AfterEach
    void tearDown() {
        spaUtilities.close();
        skillTrainingCosts.close();
    }

    private static Map<SkillTrainingCategory, Money> skillTrainingByCategory(Money profession, Money utility,
          Money roleplay, Money other) {
        Map<SkillTrainingCategory, Money> costs = new EnumMap<>(SkillTrainingCategory.class);
        costs.put(SkillTrainingCategory.PROFESSION, profession);
        costs.put(SkillTrainingCategory.UTILITY, utility);
        costs.put(SkillTrainingCategory.ROLEPLAY, roleplay);
        costs.put(SkillTrainingCategory.OTHER, other);
        return costs;
    }

    @Test
    void multipliersDefaultToOneAndAHalfWithRoleplayLeftOut() {
        CampaignOptions defaultOptions = new CampaignOptions();

        assertEquals(1.5, (double) defaultOptions.get(CampaignOption.RECRUITMENT_TRAINING_COST_MULTIPLIER));
        assertEquals(1.5, (double) defaultOptions.get(CampaignOption.RECRUITMENT_PROFESSION_SKILL_COST_MULTIPLIER));
        assertEquals(1.5, (double) defaultOptions.get(CampaignOption.RECRUITMENT_UTILITY_SKILL_COST_MULTIPLIER));
        assertEquals(0.0, (double) defaultOptions.get(CampaignOption.RECRUITMENT_ROLEPLAY_SKILL_COST_MULTIPLIER));
    }

    @ParameterizedTest
    @CsvSource({ "false, false, false, false", "false, true, true, false", "true, false, false, false",
                 "true, true, false, true", "true, false, true, true", "true, true, true, true" })
    void trainingBasedCostNeedsRecruitmentAndATrainingCostOption(boolean payForRecruitment, boolean skillCosts,
          boolean spaCosts, boolean expected) {
        campaignOptions.set(CampaignOption.PAY_FOR_RECRUITMENT, payForRecruitment);
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, skillCosts);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, spaCosts);

        assertEquals(expected, RecruitmentCosts.isUsingTrainingBasedCost(campaignOptions));
    }

    @Test
    void recruitsNormallyCostTwoMonthsSalary() {
        assertEquals(Money.of(20_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void skillTrainingAloneCountsOnlySkills() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);

        assertEquals(SKILL_TRAINING.multipliedBy(1.5), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void spaTrainingAloneCountsOnlySpas() {
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);

        assertEquals(SPA_TRAINING.multipliedBy(1.5), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void bothTrainingOptionsCountSkillsAndSpas() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);

        // (4,000,000 + 2,400,000) x 1.5
        assertEquals(Money.of(9_600_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    /** Profession and Utility skills (3,000,000 x 1.5) are unaffected; other skills and SPAs scale. */
    @ParameterizedTest
    @CsvSource({ "0.0, 4500000", "1.0, 7900000", "2.5, 13000000" })
    void otherSkillsAndSpasMultiplierScalesOnlyOtherSkillsAndSpas(double multiplier, long expected) {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        campaignOptions.set(CampaignOption.RECRUITMENT_TRAINING_COST_MULTIPLIER, multiplier);

        assertEquals(Money.of(expected), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void professionMultiplierScalesOnlyProfessionSkills() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.RECRUITMENT_PROFESSION_SKILL_COST_MULTIPLIER, 3.0);

        // 2,000,000 x 3 + (1,000,000 + 1,000,000) x 1.5
        assertEquals(Money.of(9_000_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void utilityMultiplierScalesOnlyUtilitySkills() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.RECRUITMENT_UTILITY_SKILL_COST_MULTIPLIER, 3.0);

        // 1,000,000 x 3 + (2,000,000 + 1,000,000) x 1.5
        assertEquals(Money.of(7_500_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void roleplayMultiplierAddsRoleplaySkills() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.RECRUITMENT_ROLEPLAY_SKILL_COST_MULTIPLIER, 2.0);

        // 4,000,000 x 1.5 + 800,000 x 2
        assertEquals(Money.of(7_600_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void skillMultipliersDoNotAffectSpas() {
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        campaignOptions.set(CampaignOption.RECRUITMENT_PROFESSION_SKILL_COST_MULTIPLIER, 5.0);
        campaignOptions.set(CampaignOption.RECRUITMENT_UTILITY_SKILL_COST_MULTIPLIER, 5.0);
        campaignOptions.set(CampaignOption.RECRUITMENT_ROLEPLAY_SKILL_COST_MULTIPLIER, 5.0);

        assertEquals(SPA_TRAINING.multipliedBy(1.5), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    /** Salary plays no part in the training-based cost. */
    @Test
    void salaryIsIgnoredUnderTrainingBasedCost() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);

        when(person.getSalary(campaign.getCampaignOptions(),
                    campaign.getPlayerForce().isClanForce(),
                    campaign.getLocalDate()))
                   .thenReturn(Money.of(999_999_999));

        assertEquals(SKILL_TRAINING.multipliedBy(1.5), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void untrainedRecruitsAreFree() {
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        skillTrainingCosts.when(() -> SkillTrainingCosts.getAccumulatedSkillTrainingCosts(any(), any()))
              .thenReturn(skillTrainingByCategory(Money.zero(), Money.zero(), Money.zero(), Money.zero()));
        spaUtilities.when(() -> SpaUtilities.getAccumulatedSpaTrainingCost(any())).thenReturn(Money.zero());

        assertEquals(Money.zero(), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }

    @Test
    void trainingOptionsWithoutPaidRecruitmentKeepTheSalaryCost() {
        campaignOptions.set(CampaignOption.PAY_FOR_RECRUITMENT, false);
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);

        assertEquals(Money.of(20_000), RecruitmentCosts.getRecruitmentCost(campaign, person));
    }
}
