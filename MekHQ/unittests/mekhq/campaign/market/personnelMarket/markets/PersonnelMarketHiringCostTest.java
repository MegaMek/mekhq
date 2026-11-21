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
package mekhq.campaign.market.personnelMarket.markets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.records.HiringCostBreakdown;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests that every personnel market charges what Pay for Recruitment would, and that the MekHQ market's Golden Hello is
 * charged on top whatever Pay for Recruitment is set to.
 */
class PersonnelMarketHiringCostTest {
    private static final Money MONTHLY_SALARY = Money.of(1_000);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Person applicant;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        // Salary-based pricing: neither training cost option charges C-bills
        when(campaignOptions.get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_SPA_TRAINING_COSTS)).thenReturn(false);

        applicant = mock(Person.class);
        when(applicant.getSalary(any(CampaignOptions.class), anyBoolean(), any())).thenReturn(MONTHLY_SALARY);
    }

    private void setPayForRecruitment(boolean isPayForRecruitment) {
        when(campaignOptions.get(CampaignOption.PAY_FOR_RECRUITMENT)).thenReturn(isPayForRecruitment);
    }

    private PersonnelMarketMekHQ mekHQMarket(boolean wasOfferingGoldenHello) {
        PersonnelMarketMekHQ market = new PersonnelMarketMekHQ();
        market.setCampaign(campaign);
        market.setWasOfferingGoldenHello(wasOfferingGoldenHello);
        return market;
    }

    @Test
    void camOpsMarketChargesTwoMonthsSalaryWithPayForRecruitment() {
        setPayForRecruitment(true);
        PersonnelMarketCamOpsRevised market = new PersonnelMarketCamOpsRevised();
        market.setCampaign(campaign);

        HiringCostBreakdown breakdown = market.getHiringCostBreakdown(applicant);

        assertEquals(Money.of(2_000), breakdown.salaryCost());
        assertNull(breakdown.trainingCost());
        assertEquals(Money.zero(), breakdown.goldenHelloExtra());
        assertEquals(Money.of(2_000), market.getHiringCost(applicant));
    }

    @Test
    void camOpsMarketIsFreeWithoutPayForRecruitment() {
        setPayForRecruitment(false);
        PersonnelMarketCamOpsStrict market = new PersonnelMarketCamOpsStrict();
        market.setCampaign(campaign);

        assertEquals(Money.zero(), market.getHiringCost(applicant));
    }

    @Test
    void mekHQMarketChargesTwoMonthsSalaryWithPayForRecruitment() {
        setPayForRecruitment(true);

        assertEquals(Money.of(2_000), mekHQMarket(false).getHiringCost(applicant));
    }

    @Test
    void mekHQMarketIsFreeWithoutPayForRecruitmentOrGoldenHello() {
        setPayForRecruitment(false);

        assertEquals(Money.zero(), mekHQMarket(false).getHiringCost(applicant));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void goldenHelloIsChargedWhateverPayForRecruitmentIsSetTo(boolean isPayForRecruitment) {
        setPayForRecruitment(isPayForRecruitment);

        HiringCostBreakdown breakdown = mekHQMarket(true).getHiringCostBreakdown(applicant);

        assertEquals(Money.of(22_000), breakdown.goldenHelloExtra());
        assertEquals(isPayForRecruitment ? Money.of(2_000) : Money.zero(), breakdown.salaryCost());
        assertEquals(isPayForRecruitment ? Money.of(24_000) : Money.of(22_000), breakdown.total());
    }
}
