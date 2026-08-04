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
package mekhq.campaign.reputation.chaosReputation;

import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.skillLevelFromExperienceLevel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.CampaignGUI;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the deterministic logic in {@link ChaosReputation}: the reputation cap, debt penalty, per-role
 * experience lookup, force average skill level, and the contract-completion reputation change.
 */
class ChaosReputationTest {
    private static final LocalDate DATE = LocalDate.of(3151, 1, 1);

    // region applyReputationCap
    @Test
    void applyReputationCap_capOfZeroLeavesReputationUnchanged() {
        assertEquals(42, ChaosReputation.applyReputationCap(0, 42));
    }

    @Test
    void applyReputationCap_reputationAboveCapIsClamped() {
        assertEquals(5, ChaosReputation.applyReputationCap(5, 8));
    }

    @Test
    void applyReputationCap_reputationBelowCapIsUnchanged() {
        assertEquals(3, ChaosReputation.applyReputationCap(5, 3));
    }
    // endregion applyReputationCap

    // region getDebtModifier
    private static Loan loanAged(long ageInMonths) {
        Loan loan = mock(Loan.class);
        when(loan.getAgeInMonths(any(LocalDate.class))).thenReturn(ageInMonths);
        return loan;
    }

    @Test
    void getDebtModifier_noLoansIsZero() {
        assertEquals(0, ChaosReputation.getDebtModifier(Collections.emptyList(), DATE, false));
    }

    @Test
    void getDebtModifier_singleFreshLoanIsMinusOne() {
        // floor(0 / 6) + 1 = 1, negated -> -1
        assertEquals(-1, ChaosReputation.getDebtModifier(List.of(loanAged(0)), DATE, false));
    }

    @Test
    void getDebtModifier_singleOldLoanScalesWithAge() {
        // floor(12 / 6) + 1 = 3, negated -> -3
        assertEquals(-3, ChaosReputation.getDebtModifier(List.of(loanAged(12)), DATE, false));
    }

    @Test
    void getDebtModifier_withoutStackingUsesOldestLoanOnly() {
        assertEquals(-3, ChaosReputation.getDebtModifier(List.of(loanAged(0), loanAged(12)), DATE, false));
    }

    @Test
    void getDebtModifier_withStackingSumsEveryLoan() {
        // -1 (fresh) + -3 (12 months) = -4
        assertEquals(-4, ChaosReputation.getDebtModifier(List.of(loanAged(0), loanAged(12)), DATE, true));
    }
    // endregion getDebtModifier

    // region getExperienceLevel
    @Test
    void getExperienceLevel_civilianRoleReturnsNone() {
        Campaign campaign = mock(Campaign.class);
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);

        assertEquals(EXP_NONE, ChaosReputation.getExperienceLevel(campaign, person, true));
    }

    @Test
    void getExperienceLevel_combatRoleDelegatesToPerson() {
        Campaign campaign = mock(Campaign.class);
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getExperienceLevel(campaign, false, true)).thenReturn(EXP_REGULAR);

        assertEquals(EXP_REGULAR, ChaosReputation.getExperienceLevel(campaign, person, true));
    }
    // endregion getExperienceLevel

    // region getAverageSkillLevel

    /** Builds an active character whose primary combat role has the given experience level and no secondary role. */
    private static Person combatPerson(boolean employed, int primaryExperienceLevel) {
        Person person = mock(Person.class);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isEmployed()).thenReturn(employed);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getExperienceLevel(any(), eq(false), eq(true))).thenReturn(primaryExperienceLevel);
        return person;
    }

    @Test
    void getAverageSkillLevel_averagesContributingRoles() {
        Campaign campaign = mock(Campaign.class);
        // Two REGULAR (2) and two ELITE (4) -> mean 3.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_ELITE),
              combatPerson(true, EXP_ELITE));

        assertEquals(skillLevelFromExperienceLevel(3), ChaosReputation.getAverageSkillLevel(campaign, personnel));
    }

    @Test
    void getAverageSkillLevel_ignoresUnemployedPersonnel() {
        Campaign campaign = mock(Campaign.class);
        // The unemployed ELITE character is excluded, leaving only the REGULAR average.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR), combatPerson(false, EXP_ELITE));

        assertEquals(skillLevelFromExperienceLevel(EXP_REGULAR),
              ChaosReputation.getAverageSkillLevel(campaign, personnel));
    }

    @Test
    void getAverageSkillLevel_emptyRosterUsesNone() {
        Campaign campaign = mock(Campaign.class);

        assertEquals(skillLevelFromExperienceLevel(EXP_NONE),
              ChaosReputation.getAverageSkillLevel(campaign, Collections.emptyList()));
    }
    // endregion getAverageSkillLevel

    // region processContractCompletion
    private static Campaign campaignLevelCampaign(int storedReputation, PlayerForce playerForce) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        Finances finances = mock(Finances.class);

        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getLocalDate()).thenReturn(DATE);
        when(campaign.getGUI()).thenReturn(mock(CampaignGUI.class));

        when(playerForce.getChaosCampaignReputation()).thenReturn(storedReputation);
        when(playerForce.getFinances()).thenReturn(finances);
        when(finances.getLoans()).thenReturn(Collections.emptyList());

        when(options.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(true);
        when(options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);
        when(options.get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION)).thenReturn(false);
        when(options.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)).thenReturn(false);
        when(options.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER)).thenReturn(0);
        when(options.get(CampaignOption.CHAOS_REPUTATION_CAP)).thenReturn(0);

        return campaign;
    }

    @Test
    void processContractCompletion_campaignLevelSuccessRaisesReputation() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = campaignLevelCampaign(5, playerForce);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.SUCCESS, Collections.emptyList());

        verify(playerForce).changeChaosCampaignReputation(1);
    }

    @Test
    void processContractCompletion_campaignLevelBreachLowersReputation() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = campaignLevelCampaign(10, playerForce);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.BREACH, Collections.emptyList());

        // Break delta = -max(round(10 * 0.5), 3) = -5.
        verify(playerForce).changeChaosCampaignReputation(-5);
    }

    @Test
    void processContractCompletion_failedContractDoesNothing() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = campaignLevelCampaign(10, playerForce);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.FAILED, Collections.emptyList());

        verify(playerForce, never()).changeChaosCampaignReputation(anyInt());
        verify(campaign, never()).addReport(any(DailyReportType.class), any());
    }

    @Test
    void processContractCompletion_chaosReputationDisabledDoesNothing() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(false);
        when(options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(false);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.SUCCESS, Collections.emptyList());

        verify(playerForce, never()).changeChaosCampaignReputation(anyInt());
        verify(campaign, never()).addReport(any(DailyReportType.class), any());
    }
    // endregion processContractCompletion
}
