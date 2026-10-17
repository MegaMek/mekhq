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
package mekhq.campaign.personnel.turnoverAndRetention;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FatigueTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 5);
    private static final int LEAVE_THRESHOLD = 13;

    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getActiveContracts()).thenReturn(List.of(mock(AbstractContract.class)));
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(0);
    }

    /**
     * Creates a mocked, active, free person whose raw and adjusted fatigue are both {@code fatigue}.
     */
    private static Person mockPerson(int fatigue, int permanentFatigue, boolean isClan, SkillLevel skillLevel) {
        Person person = mock(Person.class);
        when(person.getAdjustedFatigue()).thenReturn(fatigue);
        when(person.getFatigueDirect()).thenReturn(fatigue);
        when(person.getPermanentFatigue()).thenReturn(permanentFatigue);
        when(person.isClanPersonnel()).thenReturn(isClan);
        when(person.getSkillLevel(any(Campaign.class), anyBoolean(), anyBoolean())).thenReturn(skillLevel);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return person;
    }

    private static Person mockPerson(int fatigue) {
        return mockPerson(fatigue, 0, false, SkillLevel.REGULAR);
    }

    @Nested
    class EffectiveFatigue {
        @ParameterizedTest(name = "fatigue {0}, permanent {1}, Clan {2}, {3} -> {4}")
        @CsvSource({
              "10, 0, false, REGULAR, 10",
              "10, 3, false, REGULAR, 13",
              "10, 0, true, REGULAR, 8",
              "10, 0, false, VETERAN, 9",
              "10, 0, false, ELITE, 8",
              "10, 0, false, HEROIC, 8",
              "10, 0, false, LEGENDARY, 8",
              "10, 2, true, VETERAN, 9",
              "0, 0, true, ELITE, -4"
        })
        void appliesAllModifiers(int fatigue, int permanentFatigue, boolean isClan, SkillLevel skillLevel,
              int expected) {
            Person person = mockPerson(fatigue, permanentFatigue, isClan, skillLevel);

            assertEquals(expected, Fatigue.getEffectiveFatigue(person, campaign));
        }
    }

    @Nested
    class TurnoverModifier {
        @ParameterizedTest(name = "effective fatigue {0} -> +{1}")
        @CsvSource({ "-4, 0", "0, 0", "8, 0", "9, 1", "12, 1", "13, 2", "16, 2", "17, 3", "30, 3" })
        void matchesFatigueBands(int effectiveFatigue, int expected) {
            assertEquals(expected, Fatigue.getTurnoverModifier(effectiveFatigue));
        }
    }

    @Nested
    class FatigueActions {
        @ParameterizedTest(name = "effective fatigue {0}")
        @CsvSource({ "5", "8", "9", "11", "12", "16", "17", "30" })
        void reportsAndStartsRecoveryInEveryBand(int fatigue) {
            Person person = mockPerson(fatigue);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(true);
        }

        @Test
        void doesNothingBelowTired() {
            Person person = mockPerson(4);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign, never()).addReport(any(), anyString());
            verify(person, never()).setIsRecoveringFromFatigue(anyBoolean());
        }

        @Test
        void doesNothingWhenFatigueDisabled() {
            when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(20);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign, never()).addReport(any(), anyString());
            verify(person, never()).changeStatus(any(), any(), any());
        }
    }

    @Nested
    class AutomaticLeave {
        @BeforeEach
        void enableLeaveThreshold() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
        }

        @Test
        void sendsOnLeaveAtThreshold() {
            Person person = mockPerson(LEAVE_THRESHOLD);

            Fatigue.processFatigueActions(campaign, person);

            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
        }

        @Test
        void usesEffectiveFatigue() {
            // 14 raw fatigue, -2 for Elite, is below the threshold
            Person person = mockPerson(LEAVE_THRESHOLD + 1, 0, false, SkillLevel.ELITE);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentBelowThreshold() {
            Person person = mockPerson(LEAVE_THRESHOLD - 1);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenThresholdDisabled() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(0);
            Person person = mockPerson(30);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenBusy() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.isBusy()).thenReturn(true);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenInjured() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getNonPermanentInjurySeverity()).thenReturn(1);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenPrisoner() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenCampFollower() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getStatus()).thenReturn(PersonnelStatus.CAMP_FOLLOWER);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }
    }

    @Nested
    class WeeklyRecovery {
        @Test
        void recoversOnePointOnContract() {
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-1);
        }

        @Test
        void recoversExtraPointOnLeave() {
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void recoversExtraPointWithoutContracts() {
            when(campaign.getActiveContracts()).thenReturn(List.of());
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void leaveAndNoContractsDoNotStack() {
            when(campaign.getActiveContracts()).thenReturn(List.of());
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void recoversExtraPointWithFieldKitchens() {
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person).changeFatigue(-2);
        }

        @Test
        void allBonusesTogether() {
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person).changeFatigue(-3);
        }

        @Test
        void noRecoveryWithoutFatigue() {
            Person person = mockPerson(0);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person, never()).changeFatigue(anyInt());
        }

        @Test
        void recoveringPersonOverThresholdIsSentOnLeave() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(LEAVE_THRESHOLD + 2);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            // Already recovering, so no repeat of the fatigue state report
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void recoveredPersonReturnsFromLeave() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(0);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(campaign, times(1)).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(false);
            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }
    }
}
