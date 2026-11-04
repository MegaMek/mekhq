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
package mekhq.campaign.personnel.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.training.TrainingPlan.StageResult;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.AbilityPurchase;
import mekhq.campaign.personnel.training.TrainingService.CommitReport;
import mekhq.utilities.spaUtilities.SpaUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests the Training Console's pricing of special abilities: the C-bill training fee for the next ability, the
 * ability cap, flaws, and the rule that nothing is staged or committed unless the campaign can pay every fee. These
 * rules were tested on the Spend XP menu before the Training Console replaced it.
 */
class TrainingAbilityCostsTest {
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);
    private static final String ABILITY_NAME = "test_ability";
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Finances finances;
    private Person person;
    private SpecialAbility ability;
    private MockedStatic<SpecialAbility> specialAbilities;
    private MockedStatic<SpaUtilities> spaUtilities;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_ABILITIES, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        finances = mock(Finances.class);
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);

        person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getOptions()).thenReturn(mock(PersonnelOptions.class));
        when(person.getSkills()).thenReturn(new Skills());
        when(person.getXP()).thenReturn(1_000);
        when(person.getReasoningXpCostMultiplier(anyBoolean())).thenReturn(1.0);
        when(person.getFullTitle()).thenReturn("MekWarrior Test");

        ability = abilityCosting(2);
        specialAbilities = mockStatic(SpecialAbility.class);
        specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(ability);
        specialAbilities.when(() -> SpecialAbility.getDisplayName(ABILITY_NAME)).thenReturn("Test Ability");
        spaUtilities = mockStatic(SpaUtilities.class, CALLS_REAL_METHODS);
        spaUtilities.when(() -> SpaUtilities.getSpaCategory(any())).thenReturn(AbilityCategory.UTILITY_ABILITY);
        holdsSpas(0);
    }

    @AfterEach
    void tearDown() {
        spaUtilities.close();
        specialAbilities.close();
    }

    private static SpecialAbility abilityCosting(int cost) {
        SpecialAbility mocked = mock(SpecialAbility.class);
        when(mocked.getName()).thenReturn(ABILITY_NAME);
        when(mocked.getCost()).thenReturn(cost);
        when(mocked.getPrereqSkills()).thenReturn(new Vector<>());
        when(mocked.getPrereqAbilities()).thenReturn(new Vector<>());
        when(mocked.getInvalidAbilities()).thenReturn(new Vector<>());
        when(mocked.getChoiceValues()).thenReturn(new Vector<>());
        when(mocked.getRemovedAbilities()).thenReturn(new Vector<>());
        when(mocked.isClanRequirementMet(anyBoolean())).thenReturn(true);
        return mocked;
    }

    private void holdsSpas(int nonFlawSpas) {
        spaUtilities.when(() -> SpaUtilities.countNonFlawSpas(person)).thenReturn(nonFlawSpas);
    }

    private void abilityIsFlaw() {
        SpecialAbility flaw = abilityCosting(-2);
        specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(flaw);
        spaUtilities.when(() -> SpaUtilities.getSpaCategory(any())).thenReturn(AbilityCategory.CHARACTER_FLAW);
    }

    private TrainingPlan plan(Money funds) {
        return new TrainingPlan(new TrainingContext(campaign), funds);
    }

    private Quote quote() {
        return plan(Money.of(1_000_000_000)).quote(new AbilityPurchase(person, ABILITY_NAME, null));
    }

    private static Money supportPoints(int supportPoints) {
        return Money.of(supportPoints * SUPPORT_POINTS_TO_C_BILLS);
    }

    @Test
    void freeAndUncappedWhenBothOptionsAreOff() {
        holdsSpas(12);

        Quote quote = quote();
        assertTrue(quote.isAllowed());
        assertEquals(Money.zero(), quote.fee());
        assertNull(quote.feeCategory());
    }

    @ParameterizedTest
    @CsvSource({ "0, 60", "1, 180", "2, 360", "3, 600", "4, 900", "9, 900" })
    void pricesTheNextAbility(int heldSpas, int expectedSupportPoints) {
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        holdsSpas(heldSpas);

        Quote quote = quote();
        assertTrue(quote.isAllowed());
        assertEquals(supportPoints(expectedSupportPoints), quote.fee());
        assertEquals(TrainingFeeCategory.ABILITIES, quote.feeCategory());
    }

    @Test
    void refusedAtTheCap() {
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
        holdsSpas(SpaUtilities.SPA_CAP);

        assertEquals(TrainingRefusal.SPA_CAP, quote().refusal());
    }

    @Test
    void refusedAboveTheCap() {
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
        holdsSpas(SpaUtilities.SPA_CAP + 3);

        assertEquals(TrainingRefusal.SPA_CAP, quote().refusal());
    }

    @Test
    void allowedJustBelowTheCap() {
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
        holdsSpas(SpaUtilities.SPA_CAP - 1);

        assertTrue(quote().isAllowed());
    }

    @Test
    void stagedAbilitiesCountTowardsTheCap() {
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
        holdsSpas(SpaUtilities.SPA_CAP - 1);
        String second = "second_ability";
        SpecialAbility other = abilityCosting(2);
        when(other.getName()).thenReturn(second);
        specialAbilities.when(() -> SpecialAbility.getAbility(second)).thenReturn(other);
        TrainingPlan plan = plan(Money.of(1_000_000_000));

        assertEquals(1, plan.stage("first", List.of(new AbilityPurchase(person, ABILITY_NAME, null)))
                              .stagedCount());
        assertEquals(TrainingRefusal.SPA_CAP, plan.quote(new AbilityPurchase(person, second, null)).refusal());
    }

    @Test
    void flawsAreNeverChargedOrCapped() {
        abilityIsFlaw();
        campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        holdsSpas(SpaUtilities.SPA_CAP + 10);

        Quote quote = quote();
        assertTrue(quote.isAllowed());
        assertEquals(Money.zero(), quote.fee());
        // Taking a flaw gives XP rather than costing it
        assertTrue(quote.xp() < 0);
    }

    @Test
    void unknownAbilitiesAreRefused() {
        specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(null);

        assertEquals(TrainingRefusal.DISABLED, quote().refusal());
    }

    @Test
    void nothingIsStagedUnlessEveryFeeCanBePaid() {
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        TrainingPlan plan = plan(supportPoints(59));

        StageResult result = plan.stage("too dear", List.of(new AbilityPurchase(person, ABILITY_NAME, null)));
        assertNull(result.step());
        assertEquals(supportPoints(1), result.shortfall());
        assertTrue(plan.isEmpty());
        assertFalse(plan.projectionOf(person).hasAbility(ABILITY_NAME));
    }

    @Test
    void commitRefusesWhenFundsHaveFallenBelowTheFees() {
        campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
        TrainingPlan plan = plan(supportPoints(60));
        assertEquals(1, plan.stage("affordable", List.of(new AbilityPurchase(person, ABILITY_NAME, null)))
                              .stagedCount());
        when(finances.getBalance()).thenReturn(supportPoints(10));

        CommitReport report = TrainingService.commit(campaign, plan);
        assertTrue(report.isRefused());
        verify(finances, never()).debit(any(), any(), any(), anyString());
    }
}
