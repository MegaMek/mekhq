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
package mekhq.gui.adapter;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.Collections;
import javax.swing.JTable;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.utilities.spaUtilities.SpaUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests the charging and capping done when a player buys a skill level or SPA from the personnel menu: the C-bill
 * training costs, the SPA cap, and what happens when the force can't pay.
 */
class PersonnelTableMouseAdapterTrainingCostTest {
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);
    private static final String ABILITY_NAME = "test_ability";
    private static final String SKILL_NAME = "Test Skill";

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Finances finances;
    private Person person;
    private PersonnelTableMouseAdapter adapter;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        finances = mock(Finances.class);
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);

        person = mock(Person.class);
        when(person.getFullTitle()).thenReturn("MechWarrior Test");
        when(person.getHyperlinkedName()).thenReturn("Test");

        CampaignGUI gui = mock(CampaignGUI.class);
        when(gui.getCampaign()).thenReturn(campaign);
        adapter = new PersonnelTableMouseAdapter(gui, mock(JTable.class), mock(PersonnelTableModel.class));
    }

    private void debitSucceeds(boolean isSuccessful) {
        when(finances.debit(any(TransactionType.class), any(LocalDate.class), any(Money.class), anyString()))
              .thenReturn(isSuccessful);
    }

    @Nested
    class SpaTraining {
        private MockedStatic<SpecialAbility> specialAbilities;
        private MockedStatic<SpaUtilities> spaUtilities;

        @BeforeEach
        void setUpAbility() {
            SpecialAbility ability = mock(SpecialAbility.class);
            when(ability.getCost()).thenReturn(2);
            specialAbilities = mockStatic(SpecialAbility.class);
            specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(ability);
            specialAbilities.when(() -> SpecialAbility.getDisplayName(ABILITY_NAME)).thenReturn("Test Ability");

            // Stubbing a CALLS_REAL_METHODS static runs the real method once, so give it something to count
            when(person.getOptions(PersonnelOptions.LVL3_ADVANTAGES))
                  .thenAnswer(invocation -> Collections.emptyEnumeration());
            spaUtilities = mockStatic(SpaUtilities.class, CALLS_REAL_METHODS);
            holdsSpas(0);
        }

        @AfterEach
        void tearDown() {
            spaUtilities.close();
            specialAbilities.close();
        }

        private void holdsSpas(int nonFlawSpas) {
            spaUtilities.when(() -> SpaUtilities.countNonFlawSpas(person)).thenReturn(nonFlawSpas);
        }

        private void abilityIsFlaw() {
            SpecialAbility flaw = mock(SpecialAbility.class);
            when(flaw.getCost()).thenReturn(-2);
            specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(flaw);
        }

        @Test
        void freeAndUncappedWhenBothOptionsAreOff() {
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, false);
            holdsSpas(12);

            assertTrue(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @ParameterizedTest
        @CsvSource({ "0, 60", "1, 180", "2, 360", "3, 600", "4, 900", "9, 900" })
        void chargesThePriceOfTheNextSpa(int heldSpas, int expectedSupportPoints) {
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
            holdsSpas(heldSpas);
            debitSucceeds(true);

            assertTrue(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(finances).debit(eq(TransactionType.EDUCATION), eq(TODAY),
                  eq(Money.of(expectedSupportPoints * 10_000)), anyString());
        }

        @Test
        void refusedWhenTheForceCannotPay() {
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
            debitSucceeds(false);

            assertFalse(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(campaign).addReport(eq(PERSONNEL), anyString());
        }

        @Test
        void refusedAtTheCap() {
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
            holdsSpas(SpaUtilities.SPA_CAP);

            assertFalse(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(campaign).addReport(eq(PERSONNEL), anyString());
        }

        @Test
        void refusedAboveTheCap() {
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            holdsSpas(SpaUtilities.SPA_CAP + 3);

            assertFalse(adapter.payForSpaTraining(person, ABILITY_NAME));
        }

        @Test
        void allowedJustBelowTheCap() {
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
            holdsSpas(SpaUtilities.SPA_CAP - 1);

            assertTrue(adapter.payForSpaTraining(person, ABILITY_NAME));
        }

        /** A capped purchase must not take the money first. */
        @Test
        void capIsCheckedBeforeCharging() {
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
            holdsSpas(SpaUtilities.SPA_CAP);
            debitSucceeds(true);

            assertFalse(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @Test
        void flawsAreNeverChargedOrCapped() {
            abilityIsFlaw();
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
            holdsSpas(SpaUtilities.SPA_CAP + 10);

            assertTrue(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void unknownAbilitiesAreLetThrough() {
            specialAbilities.when(() -> SpecialAbility.getAbility(ABILITY_NAME)).thenReturn(null);
            campaignOptions.set(CampaignOption.CAP_TOTAL_SPAS, true);
            campaignOptions.set(CampaignOption.USE_SPA_TRAINING_COSTS, true);
            holdsSpas(SpaUtilities.SPA_CAP);

            assertTrue(adapter.payForSpaTraining(person, ABILITY_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
        }
    }

    @Nested
    class SkillTraining {
        private MockedStatic<SkillTrainingCosts> skillTrainingCosts;

        @BeforeEach
        void setUpCosts() {
            skillTrainingCosts = mockStatic(SkillTrainingCosts.class);
        }

        @AfterEach
        void tearDown() {
            skillTrainingCosts.close();
        }

        private void improvementCosts(Money cost) {
            skillTrainingCosts.when(() -> SkillTrainingCosts.getSkillImprovementCost(campaign, person, SKILL_NAME))
                  .thenReturn(cost);
        }

        @Test
        void freeImprovementsAreNotCharged() {
            improvementCosts(Money.zero());

            assertTrue(adapter.payForSkillTraining(person, SKILL_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @Test
        void pricedImprovementsAreChargedAsEducation() {
            improvementCosts(Money.of(3_000_000));
            debitSucceeds(true);

            assertTrue(adapter.payForSkillTraining(person, SKILL_NAME));
            verify(finances).debit(eq(TransactionType.EDUCATION), eq(TODAY), eq(Money.of(3_000_000)), anyString());
        }

        @Test
        void refusedWhenTheForceCannotPay() {
            improvementCosts(Money.of(3_000_000));
            debitSucceeds(false);

            assertFalse(adapter.payForSkillTraining(person, SKILL_NAME));
            verify(campaign).addReport(eq(PERSONNEL), anyString());
        }
    }
}
