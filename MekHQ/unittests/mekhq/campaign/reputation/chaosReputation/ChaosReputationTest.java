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
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static mekhq.campaign.personnel.skills.SkillType.skillLevelFromExperienceLevel;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.gui.CampaignGUI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the deterministic logic in {@link ChaosReputation}: the reputation cap, debt penalty, per-role
 * experience lookup, force average skill level, and the contract-completion reputation change.
 */
class ChaosReputationTest {
    private static final LocalDate DATE = LocalDate.of(3151, 1, 1);

    /** A {@link PersonnelOptions} mock whose {@code booleanOption} is {@code false} except for the named SPAs. */
    private static PersonnelOptions options(String... enabledSpas) {
        PersonnelOptions options = mock(PersonnelOptions.class);
        when(options.booleanOption(anyString())).thenReturn(false);
        for (String spa : enabledSpas) {
            when(options.booleanOption(spa)).thenReturn(true);
        }
        return options;
    }

    /** A character mock with the given SPAs enabled and everything else off. */
    private static Person personWith(String... enabledSpas) {
        PersonnelOptions options = options(enabledSpas);
        Person person = mock(Person.class);
        when(person.getOptions()).thenReturn(options);
        return person;
    }

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
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);

        assertEquals(EXP_NONE,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }

    @Test
    void getExperienceLevel_combatRoleDelegatesToPerson() {
        Person person = personWith();
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getExperienceLevel(any(), anyBoolean(), any(), eq(false), eq(true))).thenReturn(EXP_REGULAR);

        assertEquals(EXP_REGULAR,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }

    /** A combat-role character with a fixed primary experience level and the given SPAs. */
    private static Person combatRolePerson(int primaryExperienceLevel, String... spas) {
        Person person = personWith(spas);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getExperienceLevel(any(), anyBoolean(), any(), eq(false), eq(true))).thenReturn(
              primaryExperienceLevel);
        return person;
    }

    @Test
    void getExperienceLevel_aboveTheirWeightRaisesByOne() {
        Person person = combatRolePerson(EXP_REGULAR, PersonnelOptions.LOOKS_GOOD_ON_PAPER);
        assertEquals(EXP_REGULAR + 1,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }

    @Test
    void getExperienceLevel_looksGoodOnPaperLowersByOne() {
        Person person = combatRolePerson(EXP_REGULAR, PersonnelOptions.UNASSUMING);
        assertEquals(EXP_REGULAR - 1,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }

    @Test
    void getExperienceLevel_aboveTheirWeightClampsAtLegendary() {
        Person person = combatRolePerson(EXP_LEGENDARY, PersonnelOptions.LOOKS_GOOD_ON_PAPER);
        assertEquals(EXP_LEGENDARY,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }

    @Test
    void getExperienceLevel_looksGoodOnPaperNeverDropsToNone() {
        // Ultra-green minus one must clamp to ultra-green, not EXP_NONE (which would drop the role from the tally).
        Person person = combatRolePerson(EXP_ULTRA_GREEN, PersonnelOptions.UNASSUMING);
        assertEquals(EXP_ULTRA_GREEN,
              ChaosReputation.getExperienceLevel(mock(CampaignOptions.class), false, DATE, person, true));
    }
    // endregion getExperienceLevel

    // region getAverageSkillLevel

    /** Builds an active character whose primary combat role has the given experience level and no secondary role. */
    private static Person combatPerson(boolean employed, int primaryExperienceLevel, String... spas) {
        Person person = personWith(spas);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isEmployed()).thenReturn(employed);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getExperienceLevel(any(), anyBoolean(), any(), eq(false), eq(true))).thenReturn(
              primaryExperienceLevel);
        return person;
    }

    @Test
    void getAverageSkillLevel_averagesContributingRoles() {
        // Two REGULAR (2) and two ELITE (4) -> mean 3.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_ELITE),
              combatPerson(true, EXP_ELITE));

        assertEquals(skillLevelFromExperienceLevel(3),
              ChaosReputation.getAverageSkillLevel(mock(CampaignOptions.class), false, DATE, personnel));
    }

    @Test
    void getAverageSkillLevel_ignoresUnemployedPersonnel() {
        // The unemployed ELITE character is excluded, leaving only the REGULAR average.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR), combatPerson(false, EXP_ELITE));

        assertEquals(skillLevelFromExperienceLevel(EXP_REGULAR),
              ChaosReputation.getAverageSkillLevel(mock(CampaignOptions.class), false, DATE, personnel));
    }

    @Test
    void getAverageSkillLevel_emptyRosterUsesNone() {
        assertEquals(skillLevelFromExperienceLevel(EXP_NONE),
              ChaosReputation.getAverageSkillLevel(mock(CampaignOptions.class), false, DATE, Collections.emptyList()));
    }

    @Test
    void getAverageSkillLevel_bigPersonalityCountsTwice() {
        // REGULAR(2) x1 + ELITE(4) x2 -> total 2 + 8 = 10, count 1 + 2 = 3, mean round(10/3) = 3.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_ELITE, PersonnelOptions.BIG_PERSONALITY));

        assertEquals(skillLevelFromExperienceLevel(3),
              ChaosReputation.getAverageSkillLevel(mock(CampaignOptions.class), false, DATE, personnel));
    }

    @Test
    void getAverageSkillLevel_unrecordedPresenceIsExcluded() {
        // The ELITE character is ignored, leaving only the REGULAR average.
        List<Person> personnel = List.of(combatPerson(true, EXP_REGULAR),
              combatPerson(true, EXP_ELITE, PersonnelOptions.REDACTED));

        assertEquals(skillLevelFromExperienceLevel(EXP_REGULAR),
              ChaosReputation.getAverageSkillLevel(mock(CampaignOptions.class), false, DATE, personnel));
    }
    // endregion getAverageSkillLevel

    // region getPersonnelReputationWeight
    @Test
    void getPersonnelReputationWeight_defaultIsOne() {
        assertEquals(1, ChaosReputation.getPersonnelReputationWeight(personWith()));
    }

    @Test
    void getPersonnelReputationWeight_bigPersonalityCountsTwice() {
        assertEquals(2, ChaosReputation.getPersonnelReputationWeight(personWith(PersonnelOptions.BIG_PERSONALITY)));
    }

    @Test
    void getPersonnelReputationWeight_unrecordedPresenceCountsZero() {
        assertEquals(0, ChaosReputation.getPersonnelReputationWeight(personWith(PersonnelOptions.REDACTED)));
    }

    @Test
    void getPersonnelReputationWeight_unrecordedPresenceTakesPrecedence() {
        Person person = personWith(PersonnelOptions.BIG_PERSONALITY, PersonnelOptions.REDACTED);
        assertEquals(0, ChaosReputation.getPersonnelReputationWeight(person));
    }
    // endregion getPersonnelReputationWeight

    // region applyCommanderDebtModifier
    @Test
    void applyCommanderDebtModifier_nullCommanderIsUnchanged() {
        assertEquals(-3, ChaosReputation.applyCommanderDebtModifier(-3, null));
    }

    @Test
    void applyCommanderDebtModifier_commanderWithoutRelevantSpaIsUnchanged() {
        assertEquals(-3, ChaosReputation.applyCommanderDebtModifier(-3, personWith()));
    }

    @Test
    void applyCommanderDebtModifier_silverTongueHalvesThePenalty() {
        assertEquals(-2, ChaosReputation.applyCommanderDebtModifier(-4, personWith(PersonnelOptions.SILVER_TONGUE)));
    }

    @Test
    void applyCommanderDebtModifier_loanSharkVictimDoublesThePenalty() {
        assertEquals(-8,
              ChaosReputation.applyCommanderDebtModifier(-4, personWith(PersonnelOptions.LOAN_SHARK_VICTIM)));
    }
    // endregion applyCommanderDebtModifier

    // region campaign-level effective reputation

    /** Campaign-level options with the given manual modifier and cap; debt penalties do not stack. */
    private static CampaignOptions campaignLevelOptions(int manualModifier, int cap) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);
        when(options.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)).thenReturn(false);
        when(options.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER)).thenReturn(manualModifier);
        when(options.get(CampaignOption.CHAOS_REPUTATION_CAP)).thenReturn(cap);
        return options;
    }

    /** A force storing the given (pure) base reputation, with the given outstanding loans and commander. */
    private static PlayerForce campaignLevelForce(int storedBase, List<Loan> loans, Person commander) {
        PlayerForce playerForce = mock(PlayerForce.class);
        Finances finances = mock(Finances.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);

        when(playerForce.getChaosCampaignReputation()).thenReturn(storedBase);
        when(playerForce.isClanForce()).thenReturn(false);
        when(playerForce.getFinances()).thenReturn(finances);
        when(finances.getLoans()).thenReturn(loans);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(commander);
        return playerForce;
    }

    @Test
    void getEffectiveCampaignLevelReputation_appliesLiveModifiersToStoredBase() {
        // Base 5, one fresh loan (debt -1), a Combat Sense commander (+1), manual +2 -> 5 - 1 + 1 + 2 = 7.
        PlayerForce playerForce = campaignLevelForce(5,
              List.of(loanAged(0)),
              personWith(OptionsConstants.ATOW_COMBAT_SENSE));
        CampaignOptions options = campaignLevelOptions(2, 0);

        assertEquals(7, ChaosReputation.getEffectiveCampaignLevelReputation(playerForce, options, DATE));
    }

    @Test
    void getEffectiveCampaignLevelReputation_neverMutatesStoredBase() {
        PlayerForce playerForce = campaignLevelForce(5, List.of(loanAged(0)), null);
        CampaignOptions options = campaignLevelOptions(0, 0);

        ChaosReputation.getEffectiveCampaignLevelReputation(playerForce, options, DATE);

        verify(playerForce, never()).setChaosCampaignReputation(anyInt());
    }
    // endregion campaign-level effective reputation

    // region processChaosCampaignReputationChanges
    @Test
    void processChaosCampaignReputationChanges_campaignLevelDoesNotDriftTheStoredBase() {
        // With one outstanding loan (debt -1), the buggy implementation stored base + (-1) back into the base field
        // every day, so the value drifted by -1 per call. The stored base must never be rewritten in campaign-level
        // mode - the debt modifier is applied live at display time instead.
        PlayerForce playerForce = campaignLevelForce(5, List.of(loanAged(0)), null);
        CampaignOptions options = campaignLevelOptions(0, 0);

        for (int day = 0; day < 5; day++) {
            ChaosReputation.processChaosCampaignReputationChanges(options, playerForce, DATE);
        }

        verify(playerForce, never()).setChaosCampaignReputation(anyInt());
    }
    // endregion processChaosCampaignReputationChanges

    // region processContractCompletion
    private static Campaign campaignLevelCampaign(int storedReputation, PlayerForce playerForce) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        Finances finances = mock(Finances.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);

        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getLocalDate()).thenReturn(DATE);
        when(campaign.getGUI()).thenReturn(mock(CampaignGUI.class));

        when(playerForce.getChaosCampaignReputation()).thenReturn(storedReputation);
        when(playerForce.getFinances()).thenReturn(finances);
        when(finances.getLoans()).thenReturn(Collections.emptyList());
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(null);

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

    /** An active, employed character with no SPAs, usable both in the passed personnel and in the recalculation. */
    private static Person contractPerson() {
        Person person = personWith();
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isEmployed()).thenReturn(true);
        when(person.getAdjustedReputation(anyBoolean(), anyBoolean(), any())).thenReturn(3);
        return person;
    }

    private static Campaign personnelLevelCampaign(List<Person> personnel, boolean noPartialReputation) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);
        Finances finances = mock(Finances.class);

        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getLocalDate()).thenReturn(DATE);
        when(campaign.getGUI()).thenReturn(mock(CampaignGUI.class));

        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(null);
        when(playerForce.allPersonnel()).thenReturn(new ArrayList<>(personnel));
        when(playerForce.isClanForce()).thenReturn(false);
        when(playerForce.getFinances()).thenReturn(finances);
        when(finances.getLoans()).thenReturn(Collections.emptyList());

        when(options.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(true);
        when(options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(false);
        when(options.get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION)).thenReturn(noPartialReputation);
        when(options.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)).thenReturn(false);
        when(options.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER)).thenReturn(0);
        when(options.get(CampaignOption.CHAOS_REPUTATION_CAP)).thenReturn(0);
        when(options.get(CampaignOption.USE_AGE_EFFECTS)).thenReturn(false);
        when(options.get(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION)).thenReturn(false);
        when(options.get(CampaignOption.USE_RANDOM_PERSONALITIES)).thenReturn(false);

        return campaign;
    }

    @Test
    void processContractCompletion_personnelLevelSuccessRewardsEachCharacter() {
        Person first = contractPerson();
        Person second = contractPerson();
        List<Person> personnel = List.of(first, second);
        Campaign campaign = personnelLevelCampaign(personnel, false);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.SUCCESS, personnel);

        verify(first).changeReputation(1);
        verify(second).changeReputation(1);
    }

    @Test
    void processContractCompletion_partialSuccessRewardsWhenNotSuppressed() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = campaignLevelCampaign(5, playerForce); // CHAOS_NO_PARTIAL_SUCCESS_REPUTATION = false

        ChaosReputation.processContractCompletion(campaign, MissionStatus.PARTIAL, Collections.emptyList());

        verify(playerForce).changeChaosCampaignReputation(1);
    }

    @Test
    void processContractCompletion_partialSuccessIgnoredWhenSuppressed() {
        PlayerForce playerForce = mock(PlayerForce.class);
        Campaign campaign = campaignLevelCampaign(5, playerForce);
        when(campaign.getCampaignOptions().get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION)).thenReturn(true);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.PARTIAL, Collections.emptyList());

        verify(playerForce, never()).changeChaosCampaignReputation(anyInt());
    }
    // endregion processContractCompletion

    // region tabulateReputationFromContracts
    private static AbstractContract mission(MissionStatus status, LocalDate endingDate) {
        AbstractContract mission = mock(AbstractContract.class);
        when(mission.getStatus()).thenReturn(status);
        when(mission.getEndingDate()).thenReturn(endingDate);
        return mission;
    }

    private static Campaign tabulationCampaign(boolean noPartialReputation, List<AbstractContract> missions) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION)).thenReturn(noPartialReputation);
        when(campaign.getCompletedContracts()).thenReturn(missions);
        return campaign;
    }

    @Test
    void tabulate_noMissionsReturnsStartingScore() {
        Campaign campaign = tabulationCampaign(false, List.of());
        assertEquals(ChaosReputation.STARTING_REPUTATION_SCORE,
              ChaosReputation.tabulateReputationFromContracts(campaign, null, null));
    }

    @Test
    void tabulate_successAddsOne() {
        Campaign campaign = tabulationCampaign(false, List.of(mission(MissionStatus.SUCCESS, DATE)));
        assertEquals(2, ChaosReputation.tabulateReputationFromContracts(campaign, null, null));
    }

    @Test
    void tabulate_partialSuccessAddsOneUnlessSuppressed() {
        Campaign rewarding = tabulationCampaign(false, List.of(mission(MissionStatus.PARTIAL, DATE)));
        assertEquals(2, ChaosReputation.tabulateReputationFromContracts(rewarding, null, null));

        Campaign suppressed = tabulationCampaign(true, List.of(mission(MissionStatus.PARTIAL, DATE)));
        assertEquals(1, ChaosReputation.tabulateReputationFromContracts(suppressed, null, null));
    }

    @Test
    void tabulate_failedContractDoesNotChangeReputation() {
        Campaign campaign = tabulationCampaign(false, List.of(mission(MissionStatus.FAILED, DATE)));
        assertEquals(1, ChaosReputation.tabulateReputationFromContracts(campaign, null, null));
    }

    @Test
    void tabulate_breachHalvesWithMinimumLossOfThree() {
        // From the starting score of 1: loss = max(round(1 * 0.5), 3) = 3 -> 1 - 3 = -2.
        Campaign campaign = tabulationCampaign(false, List.of(mission(MissionStatus.BREACH, DATE)));
        assertEquals(-2, ChaosReputation.tabulateReputationFromContracts(campaign, null, null));
    }

    @Test
    void tabulate_ignoresContractsEndingBeforeTheWindow() {
        AbstractContract early = mission(MissionStatus.SUCCESS, LocalDate.of(3150, 1, 1));
        AbstractContract late = mission(MissionStatus.SUCCESS, LocalDate.of(3151, 6, 1));
        Campaign campaign = tabulationCampaign(false, List.of(early, late));

        // Window starts after the early contract, so only the late success counts: 1 + 1 = 2.
        assertEquals(2, ChaosReputation.tabulateReputationFromContracts(campaign, LocalDate.of(3151, 1, 1), null));
    }

    @Test
    void tabulate_countsContractsWithNoEndingDateRegardlessOfWindow() {
        Campaign campaign = tabulationCampaign(false, List.of(mission(MissionStatus.SUCCESS, null)));

        assertEquals(2, ChaosReputation.tabulateReputationFromContracts(campaign,
              LocalDate.of(9999, 1, 1),
              LocalDate.of(9999, 12, 31)));
    }

    @Test
    void tabulate_processesContractsChronologicallyByEndingDate() {
        // Supplied breach-first, but the breach ends last. Chronologically: eight successes (1 -> 9), then the breach
        // loses max(round(9 * 0.5), 3) = 5 -> 4. In list order it would instead be 6, so the result proves the sort.
        List<AbstractContract> missions = new ArrayList<>();
        missions.add(mission(MissionStatus.BREACH, LocalDate.of(3151, 12, 1)));
        for (int month = 1; month <= 8; month++) {
            missions.add(mission(MissionStatus.SUCCESS, LocalDate.of(3151, month, 1)));
        }
        Campaign campaign = tabulationCampaign(false, missions);

        assertEquals(4, ChaosReputation.tabulateReputationFromContracts(campaign, null, null));
    }
    // endregion tabulateReputationFromContracts

    // region getOtherModifiers
    @Test
    void getOtherModifiers_nullCommanderIsZero() {
        assertEquals(0, ChaosReputation.getOtherModifiers(null));
    }

    @Test
    void getOtherModifiers_commanderWithoutRelevantSpaIsZero() {
        assertEquals(0, ChaosReputation.getOtherModifiers(personWith()));
    }

    @Test
    void getOtherModifiers_impressiveLeaderAddsOne() {
        assertEquals(1, ChaosReputation.getOtherModifiers(personWith(PersonnelOptions.IMPRESSIVE_LEADER)));
    }

    @Test
    void getOtherModifiers_disappointingLeaderSubtractsOne() {
        assertEquals(-1, ChaosReputation.getOtherModifiers(personWith(PersonnelOptions.DISAPPOINTING_LEADER)));
    }

    @Test
    void getOtherModifiers_combatSenseAddsOne() {
        assertEquals(1, ChaosReputation.getOtherModifiers(personWith(OptionsConstants.ATOW_COMBAT_SENSE)));
    }

    @Test
    void getOtherModifiers_combatParalysisSubtractsOne() {
        assertEquals(-1, ChaosReputation.getOtherModifiers(personWith(OptionsConstants.ATOW_COMBAT_PARALYSIS)));
    }

    @Test
    void getOtherModifiers_opposingModifiersCancelOut() {
        Person commander = personWith(OptionsConstants.ATOW_COMBAT_SENSE, PersonnelOptions.DISAPPOINTING_LEADER);
        assertEquals(0, ChaosReputation.getOtherModifiers(commander));
    }
    // endregion getOtherModifiers

    // region updatePersonnelCriminalRecord

    /** An employed/unemployed character mock, usable in criminal-record and contract-break helpers. */
    private static Person employablePerson(boolean employed) {
        Person person = personWith();
        when(person.isEmployed()).thenReturn(employed);
        return person;
    }

    @Test
    void updatePersonnelCriminalRecord_adjustsEmployedPersonnel() {
        Person employed = employablePerson(true);

        ChaosReputation.updatePersonnelCriminalRecord(List.of(employed), -2);

        verify(employed).changeCriminalRecord(-2);
    }

    @Test
    void updatePersonnelCriminalRecord_skipsUnemployedPersonnel() {
        Person unemployed = employablePerson(false);

        ChaosReputation.updatePersonnelCriminalRecord(List.of(unemployed), -2);

        verify(unemployed, never()).changeCriminalRecord(anyInt());
    }
    // endregion updatePersonnelCriminalRecord

    // region processContractCompletion (personnel-level breach)
    @Test
    void processContractCompletion_personnelLevelBreachPenalizesEachCharacter() {
        // getReputationDirect() defaults to 0, so the break delta is -max(round(0 * 0.5), 3) = -3. The change must be
        // negative: breaking a contract lowers reputation.
        Person first = contractPerson();
        Person second = contractPerson();
        List<Person> personnel = List.of(first, second);
        Campaign campaign = personnelLevelCampaign(personnel, false);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.BREACH, personnel);

        verify(first).changeReputation(-3);
        verify(second).changeReputation(-3);
    }

    @Test
    void processContractCompletion_personnelLevelBreachHalvesHighReputation() {
        // getReputationDirect() = 10, so the break delta is -max(round(10 * 0.5), 3) = -5.
        Person person = contractPerson();
        when(person.getReputationDirect()).thenReturn(10);
        List<Person> personnel = List.of(person);
        Campaign campaign = personnelLevelCampaign(personnel, false);

        ChaosReputation.processContractCompletion(campaign, MissionStatus.BREACH, personnel);

        verify(person).changeReputation(-5);
    }
    // endregion processContractCompletion (personnel-level breach)

    // region applyStartingReputation

    /** A character whose primary-role skill level is fixed, for starting-reputation tests. */
    private static Person startingReputationPerson(boolean civilian, int skillLevel) {
        Person person = mock(Person.class);
        when(person.isCivilian()).thenReturn(civilian);
        when(person.getExperienceLevel(any(), anyBoolean(), any(), eq(false), eq(true))).thenReturn(skillLevel);
        return person;
    }

    @Test
    void applyStartingReputation_civilianIsUnchanged() {
        Person civilian = startingReputationPerson(true, EXP_REGULAR);

        ChaosReputation.applyStartingReputation(mock(CampaignOptions.class), false, DATE, civilian);

        assertEquals(0, civilian.getReputationDirect());
    }

    @Test
    void applyStartingReputation_regularGetsStartingScore() {
        Person regular = startingReputationPerson(false, EXP_REGULAR);

        ChaosReputation.applyStartingReputation(mock(CampaignOptions.class), false, DATE, regular);

        verify(regular).setReputationDirect(ChaosReputation.STARTING_REPUTATION_SCORE);
    }

    @Test
    void applyStartingReputation_veteranGetsBonus() {
        Person veteran = startingReputationPerson(false, EXP_VETERAN);

        ChaosReputation.applyStartingReputation(mock(CampaignOptions.class), false, DATE, veteran);

        verify(veteran).setReputationDirect(ChaosReputation.STARTING_REPUTATION_SCORE + 1);
    }

    @Test
    void applyStartingReputation_eliteGetsLargerBonus() {
        Person elite = startingReputationPerson(false, EXP_ELITE);

        ChaosReputation.applyStartingReputation(mock(CampaignOptions.class), false, DATE, elite);

        verify(elite).setReputationDirect(ChaosReputation.STARTING_REPUTATION_SCORE + 2);
    }
    // endregion applyStartingReputation

    // region applyStartingCriminalRecord
    private static Person originFactionPerson(String factionCode, boolean child, boolean civilian) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(factionCode);
        Person person = mock(Person.class);
        when(person.getOriginFaction()).thenReturn(faction);
        when(person.isChild(any(), eq(false))).thenReturn(child);
        when(person.isCivilian()).thenReturn(civilian);
        return person;
    }

    @Test
    void applyStartingCriminalRecord_nonPirateIsUnchanged() {
        Person person = originFactionPerson("FS", false, false);

        ChaosReputation.applyStartingCriminalRecord(DATE, person);

        verify(person, never()).setCriminalRecord(anyInt());
    }

    @Test
    void applyStartingCriminalRecord_pirateChildIsUnchanged() {
        Person person = originFactionPerson(PIRATE_FACTION_CODE, true, false);

        ChaosReputation.applyStartingCriminalRecord(DATE, person);

        verify(person, never()).setCriminalRecord(anyInt());
    }

    @Test
    void applyStartingCriminalRecord_pirateCivilianIsUnchanged() {
        Person person = originFactionPerson(PIRATE_FACTION_CODE, false, true);

        ChaosReputation.applyStartingCriminalRecord(DATE, person);

        verify(person, never()).setCriminalRecord(anyInt());
    }

    @Test
    void applyStartingCriminalRecord_pirateAdultGetsNegativeD6() {
        Person person = originFactionPerson(PIRATE_FACTION_CODE, false, false);

        ChaosReputation.applyStartingCriminalRecord(DATE, person);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(person).setCriminalRecord(captor.capture());
        int record = captor.getValue();
        assertTrue(record >= -6 && record <= -1, "criminal record should be a negated d6 result, was " + record);
    }
    // endregion applyStartingCriminalRecord
}
