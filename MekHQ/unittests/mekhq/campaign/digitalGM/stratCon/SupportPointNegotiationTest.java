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
package mekhq.campaign.digitalGM.stratCon;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link SupportPointNegotiation}.
 *
 * <p>The class exposes only two public entry points, {@link SupportPointNegotiation#negotiateAdditionalSupportPoints}
 * (weekly negotiation across all active contracts) and
 * {@link SupportPointNegotiation#negotiateInitialSupportPoints} (a single contract at contract start). Every private
 * helper is exercised through those two methods, one behaviour per test, by observing the interactions with the mocked
 * {@link Campaign}, {@link StratConCampaignState}, and the negotiating personnel.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SupportPointNegotiationTest {
    private static final LocalDate TODAY = LocalDate.of(3067, 1, 1);
    private static final String CONTRACT_NAME = "Contract";
    private static final String EMPLOYER_FACTION_CODE = "LA";

    private Campaign campaign;
    private PlayerForce playerForce;
    private ForceHumanResources humanResources;
    private CampaignOptions campaignOptions;
    private FactionStandings factionStandings;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        playerForce = mock(PlayerForce.class);
        humanResources = mock(ForceHumanResources.class);
        campaignOptions = mock(CampaignOptions.class);
        factionStandings = mock(FactionStandings.class);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(playerForce.getFactionStandings()).thenReturn(factionStandings);
        when(playerForce.isClanForce()).thenReturn(false);

        when(campaignOptions.get(CampaignOption.USE_AGE_EFFECTS)).thenReturn(false);
        when(campaignOptions.isUseFactionStandingSupportPointsSafe()).thenReturn(false);

        // Default to an empty (but mutable) admin pool; individual tests override as needed.
        when(humanResources.getAdmins()).thenReturn(new ArrayList<>());
    }

    // region negotiateAdditionalSupportPoints (weekly)

    @Nested
    public class NegotiateAdditionalSupportPoints {
        @Test
        void noActiveContractsReturnsEarly() {
            when(campaign.getActiveContracts()).thenReturn(new ArrayList<>());

            SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

            // Returning before touching personnel is the observable proof of the early exit.
            verify(humanResources, never()).getAdmins();
        }

        @Test
        void noAdministratorsAddsOneReportPerContract() {
            AbstractContract firstContract = mockContract(TODAY, null, 5, 2);
            AbstractContract secondContract = mockContract(TODAY.plusDays(1), null, 5, 2);
            when(campaign.getActiveContracts()).thenReturn(mutableList(firstContract, secondContract));

            SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

            verify(campaign, times(2)).addReport(eq(GENERAL), anyString());
        }

        @Test
        void nonAdministratorsAreFilteredOutAndTreatedAsNoPersonnel() {
            Person nonAdministrator = mock(Person.class);
            when(nonAdministrator.isAdministrator()).thenReturn(false);
            when(humanResources.getAdmins()).thenReturn(mutableList(nonAdministrator));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 5, 2);
            when(campaign.getActiveContracts()).thenReturn(mutableList(contract));

            SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

            verify(campaign).addReport(eq(GENERAL), anyString());
            verify(state, never()).changeSupportPoints(anyInt());
        }

        @Test
        void processesOldestContractFirstAndSharesTheAdminPool() {
            // A single admin can only service one contract; the oldest contract must win it.
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, true)));

            StratConCampaignState olderState = mockState(0);
            StratConCampaignState newerState = mockState(0);
            AbstractContract olderContract = mockContract(TODAY, olderState, 5, 2);
            AbstractContract newerContract = mockContract(TODAY.plusDays(10), newerState, 5, 2);

            // Deliberately supplied newest-first to prove the sort reorders them.
            when(campaign.getActiveContracts()).thenReturn(mutableList(newerContract, olderContract));

            SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

            verify(olderState).changeSupportPoints(1);
            verify(newerState, never()).changeSupportPoints(anyInt());
            // The newer contract, left with no admins, falls through to the no-personnel report.
            verify(campaign).addReport(eq(GENERAL), anyString());
        }

        @Test
        void successfulRollsAddOnePointPerSuccess() {
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, true),
                  mockAdmin(4, true),
                  mockAdmin(3, true)));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 99, 5);
            when(campaign.getActiveContracts()).thenReturn(mutableList(contract));

            SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

            verify(state).changeSupportPoints(3);
            // Weekly negotiations report against the contract scale as the maximum.
            verify(campaign).addReport(eq(GENERAL),
                  eq("supportPoints.biweekly"),
                  eq(CONTRACT_NAME),
                  anyString(),
                  eq(3),
                  eq(CLOSING_SPAN_TAG),
                  eq(5));
        }
    }

    // endregion negotiateAdditionalSupportPoints (weekly)

    // region negotiateInitialSupportPoints (contract start)

    @Nested
    public class NegotiateInitialSupportPoints {
        @Test
        void noAdministratorsAddsNoAdministratorsReportAndSkipsProcessing() {
            AbstractContract contract = mockContract(TODAY, mockState(0), 5, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(campaign).addReport(eq(GENERAL), anyString());
            // We never reach the processing stage, so the campaign state is never inspected.
            verify(contract, never()).getStratConCampaignState();
        }

        @Test
        void nullCampaignStateReturnsWithoutReportingOrChangingPoints() {
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, true)));
            AbstractContract contract = mockContract(TODAY, null, 5, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(contract).getStratConCampaignState();
            verify(campaign, never()).addReport(eq(GENERAL), anyString());
            verify(campaign, never()).addReport(eq(GENERAL), anyString(), any(), any(), any(), any(), any());
        }

        @Test
        void alreadyAtMaximumReportsMaximumAndAddsNoPoints() {
            Person admin = mockAdmin(5, true);
            when(humanResources.getAdmins()).thenReturn(mutableList(admin));

            StratConCampaignState state = mockState(3);
            AbstractContract contract = mockContract(TODAY, state, 3, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(campaign).addReport(eq(GENERAL), anyString());
            verify(state, never()).changeSupportPoints(anyInt());
            // No admin should be spent when the maximum has already been reached.
            verify(admin, never()).checkSkill(eq(S_ADMIN), any(Campaign.class));
        }

        @Test
        void successfulRollsAddPointsAndReportInitialNegotiation() {
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, true), mockAdmin(4, true)));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 10, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(state).changeSupportPoints(2);
            verify(campaign).addReport(eq(GENERAL),
                  eq("supportPoints.initial"),
                  eq(CONTRACT_NAME),
                  anyString(),
                  eq(2),
                  eq(CLOSING_SPAN_TAG),
                  eq(10));
        }

        @Test
        void failedRollsAddNoPointsButStillReport() {
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, false), mockAdmin(4, false)));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 10, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(state, never()).changeSupportPoints(anyInt());
            verify(campaign).addReport(eq(GENERAL),
                  eq("supportPoints.initial"),
                  eq(CONTRACT_NAME),
                  anyString(),
                  eq(0),
                  eq(CLOSING_SPAN_TAG),
                  eq(10));
        }

        @Test
        void stopsConsumingAdminsOnceMaximumIsReachedUsingHighestSkillFirst() {
            // Only one point is needed (current 2, max 3), so exactly one - the most skilled - admin is spent.
            Person lowSkill = mockAdmin(1, true);
            Person highSkill = mockAdmin(9, true);
            Person middleSkill = mockAdmin(5, true);
            when(humanResources.getAdmins()).thenReturn(mutableList(lowSkill, highSkill, middleSkill));

            StratConCampaignState state = mockState(2);
            AbstractContract contract = mockContract(TODAY, state, 3, 2);

            SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

            verify(state).changeSupportPoints(1);
            verify(highSkill).checkSkill(eq(S_ADMIN), any(Campaign.class));
            verify(middleSkill, never()).checkSkill(eq(S_ADMIN), any(Campaign.class));
            verify(lowSkill, never()).checkSkill(eq(S_ADMIN), any(Campaign.class));
        }
    }

    // endregion negotiateInitialSupportPoints (contract start)

    // region faction standing modifiers

    @Nested
    public class FactionStandingModifiers {
        @Test
        void initialNegotiationAddsContractStartBonusScaledByContractScale() {
            when(campaignOptions.isUseFactionStandingSupportPointsSafe()).thenReturn(true);
            when(humanResources.getAdmins()).thenReturn(mutableList(mockAdmin(5, true)));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 100, 3);

            try (MockedStatic<FactionStandingUtilities> factionStandingUtilities = mockStatic(
                  FactionStandingUtilities.class)) {
                factionStandingUtilities.when(() -> FactionStandingUtilities.getSupportPointModifierContractStart(
                      anyDouble())).thenReturn(2);

                SupportPointNegotiation.negotiateInitialSupportPoints(campaign, contract);

                // One admin success (1) plus the contract-start bonus (2 * scale 3 = 6) -> 7 points.
                verify(state).changeSupportPoints(7);
            }
        }

        @Test
        void weeklyNegotiationAppliesPeriodicModifierAsANegativeMiscModifier() {
            when(campaignOptions.isUseFactionStandingSupportPointsSafe()).thenReturn(true);

            SkillCheck skillCheck = mock(SkillCheck.class);
            Person admin = mockAdmin(5, true, skillCheck);
            when(humanResources.getAdmins()).thenReturn(mutableList(admin));

            StratConCampaignState state = mockState(0);
            AbstractContract contract = mockContract(TODAY, state, 99, 5);
            when(campaign.getActiveContracts()).thenReturn(mutableList(contract));

            try (MockedStatic<FactionStandingUtilities> factionStandingUtilities = mockStatic(
                  FactionStandingUtilities.class)) {
                factionStandingUtilities.when(() -> FactionStandingUtilities.getSupportPointModifierPeriodic(
                      anyDouble())).thenReturn(1);

                SupportPointNegotiation.negotiateAdditionalSupportPoints(campaign);

                // A periodic modifier of 1 is applied to the check as a negative misc modifier.
                verify(skillCheck).withMiscModifier(-1);
                verify(state).changeSupportPoints(1);
            }
        }
    }

    // endregion faction standing modifiers

    // region helpers

    private static List<AbstractContract> mutableList(AbstractContract... contracts) {
        return new ArrayList<>(List.of(contracts));
    }

    private static List<Person> mutableList(Person... people) {
        return new ArrayList<>(List.of(people));
    }

    private StratConCampaignState mockState(int currentSupportPoints) {
        StratConCampaignState state = mock(StratConCampaignState.class);
        when(state.getSupportPoints()).thenReturn(currentSupportPoints);
        return state;
    }

    private AbstractContract mockContract(LocalDate startDate, StratConCampaignState state, int maximumSupportPoints,
          int scale) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStartDate()).thenReturn(startDate);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getMaximumSupportPoints()).thenReturn(maximumSupportPoints);
        when(contract.getScale()).thenReturn(scale);
        when(contract.getHyperlinkedName()).thenReturn(CONTRACT_NAME);
        when(contract.getEmployerFactionCode()).thenReturn(EMPLOYER_FACTION_CODE);
        return contract;
    }

    private Person mockAdmin(int skillValue, boolean rollSucceeds) {
        return mockAdmin(skillValue, rollSucceeds, mock(SkillCheck.class));
    }

    private Person mockAdmin(int skillValue, boolean rollSucceeds, SkillCheck skillCheck) {
        Person admin = mock(Person.class);
        when(admin.isAdministrator()).thenReturn(true);

        Skill skill = mock(Skill.class);
        when(skill.getTotalSkillLevel(any())).thenReturn(skillValue);
        when(admin.getSkill(S_ADMIN)).thenReturn(skill);
        when(admin.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(mock(SkillModifierData.class));

        ActionCheckResult result = mock(ActionCheckResult.class);
        when(result.isSuccess()).thenReturn(rollSucceeds);

        when(skillCheck.resolve(anyBoolean(), any())).thenReturn(result);
        when(skillCheck.withMiscModifier(anyInt())).thenReturn(skillCheck);
        when(admin.checkSkill(eq(S_ADMIN), any(Campaign.class))).thenReturn(skillCheck);

        return admin;
    }

    // endregion helpers
}
