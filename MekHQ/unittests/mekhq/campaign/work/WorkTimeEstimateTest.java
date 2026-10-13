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
package mekhq.campaign.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Locale;

import megamek.common.rolls.TargetRoll;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Checks the repair time estimates on the Repair tab: whose working day they count, when overtime is included, what
 * each work mode would do, the daily report for a job that carries over, and the tech list's time and finish date.
 */
class WorkTimeEstimateTest {
    private MockedStatic<MekHQ> mekHQ;
    private MHQOptions mhqOptions;
    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        // Built before MekHQ's options are stubbed: some option lists load their names when first used
        campaignOptions = new CampaignOptions();

        // Pin the display choice so the player's own saved preference cannot change the results
        mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getWorkTimeDisplay()).thenReturn(WorkTimeDisplay.AUTOMATIC);
        when(mhqOptions.getLocale()).thenReturn(Locale.getDefault());
        when(mhqOptions.getDisplayFormattedDate(any())).thenAnswer(call -> call.getArgument(0).toString());
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);

        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    private static Person techWithTimeMultiplier(double multiplier) {
        Person tech = mock(Person.class);
        when(tech.calculateTechTimeMultiplier(anyBoolean())).thenReturn(multiplier);
        return tech;
    }

    private static IPartWork job(int minutesLeft, Person tech, Unit unit) {
        IPartWork partWork = mock(IPartWork.class);
        when(partWork.getTimeLeft()).thenReturn(minutesLeft);
        when(partWork.getActualTime()).thenReturn(minutesLeft);
        when(partWork.getTech()).thenReturn(tech);
        when(partWork.getUnit()).thenReturn(unit);
        when(partWork.getMode()).thenReturn(WorkTime.NORMAL);
        return partWork;
    }

    @Test
    void workingDayIsEightHoursPlusFourHoursOfOvertime() {
        assertEquals(480, WorkTimeEstimate.getMinutesPerDay(campaign, null, false));
        assertEquals(720, WorkTimeEstimate.getMinutesPerDay(campaign, null, true));
    }

    @Test
    void administrationSkillChangesTheWorkingDay() {
        // An Elite administrator works 10% longer; day and overtime are rounded separately, as at the new day
        Person tech = techWithTimeMultiplier(1.1);
        assertEquals(528, WorkTimeEstimate.getMinutesPerDay(campaign, tech, false));
        assertEquals(792, WorkTimeEstimate.getMinutesPerDay(campaign, tech, true));
    }

    @Test
    void unstartedJobShowsTheTimeWithAndWithoutOvertime() {
        IPartWork partWork = job(1440, null, null);
        assertEquals("3 days (2 days with overtime)", WorkTimeEstimate.describeTimeLeft(campaign, partWork));
    }

    @Test
    void fullDisplayAddsTheHoursOfWork() {
        when(mhqOptions.getWorkTimeDisplay()).thenReturn(WorkTimeDisplay.FULL);
        IPartWork partWork = job(1440, null, null);
        assertEquals("24 h of work, 3 days (2 days with overtime)",
              WorkTimeEstimate.describeTimeLeft(campaign, partWork));
    }

    @Test
    void jobWithNoTimeShowsNoTime() {
        // Armor with none in stock has nothing to time; "0 min" would read as instant
        assertEquals("", WorkTimeEstimate.describeTimeLeft(campaign, job(0, null, null)));
    }

    @Test
    void shortUnstartedJobShowsOneTime() {
        IPartWork partWork = job(90, null, null);
        assertEquals("1 h 30 min", WorkTimeEstimate.describeTimeLeft(campaign, partWork));
    }

    @Test
    void scheduledJobUsesItsTechAndTheOvertimeSetting() {
        Person tech = techWithTimeMultiplier(1.0);
        IPartWork partWork = job(1440, tech, null);
        when(campaign.isOvertimeAllowed()).thenReturn(false);
        assertEquals("3 days (scheduled)", WorkTimeEstimate.describeTimeLeft(campaign, partWork));

        when(campaign.isOvertimeAllowed()).thenReturn(true);
        assertEquals("2 days (scheduled)", WorkTimeEstimate.describeTimeLeft(campaign, partWork));
    }

    @Test
    void refitNeverCountsOvertime() {
        Person tech = techWithTimeMultiplier(1.0);
        Refit refit = mock(Refit.class);
        when(refit.getTimeLeft()).thenReturn(1440);
        when(refit.getTech()).thenReturn(tech);
        when(campaign.isOvertimeAllowed()).thenReturn(true);
        assertEquals("3 days (scheduled)", WorkTimeEstimate.describeTimeLeft(campaign, refit));
    }

    @Test
    void shipJobCountsItsOwnCrewsWorkingDay() {
        // The ship's crew has no Administration skill: 0.9 gives a 432-minute day, 648 with overtime
        Unit ship = mock(Unit.class);
        Person engineer = techWithTimeMultiplier(0.9);
        when(ship.getEngineer()).thenReturn(engineer);
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, true);
        IPartWork partWork = job(1296, null, ship);
        assertEquals("3 days (2 days with overtime)", WorkTimeEstimate.describeTimeLeft(campaign, partWork));
    }

    @Test
    void modeMenuShowsTimeAndTargetNumber() {
        IPartWork partWork = job(120, null, null);
        assertEquals("Extra time (x2): 4 h, TN -1",
              WorkTimeEstimate.describeWorkMode(campaign, partWork, WorkTime.EXTRA_2));
        assertEquals("Normal: 2 h", WorkTimeEstimate.describeWorkMode(campaign, partWork, WorkTime.NORMAL));
    }

    @Test
    void rushShowsTheSkillPenaltyAndOnlyAddsToTheTargetUnderDestroyByMargin() {
        IPartWork partWork = job(120, null, null);
        campaignOptions.set(CampaignOption.DESTROY_BY_MARGIN, false);
        assertEquals("Rush Job (1/2): 1 h, tech counts as one level lower",
              WorkTimeEstimate.describeWorkMode(campaign, partWork, WorkTime.RUSH_2));

        campaignOptions.set(CampaignOption.DESTROY_BY_MARGIN, true);
        assertEquals("Rush Job (1/4): 30 min, TN +2, tech counts as 2 levels lower",
              WorkTimeEstimate.describeWorkMode(campaign, partWork, WorkTime.RUSH_4));
    }

    @Test
    void modeMenuScalesFromTheCurrentMode() {
        // A job already on extra time (x2) takes 240 minutes; back in normal mode it takes 120
        IPartWork partWork = job(240, null, null);
        when(partWork.getMode()).thenReturn(WorkTime.EXTRA_2);
        assertEquals("Normal: 2 h", WorkTimeEstimate.describeWorkMode(campaign, partWork, WorkTime.NORMAL));
    }

    @Test
    void carriedOverReportSaysWhenTheWorkFinishes() {
        Person tech = techWithTimeMultiplier(1.0);
        assertEquals("5 h left. Work will be finished tomorrow.",
              WorkTimeEstimate.describeWorkCarriedOver(campaign, job(300, tech, null), tech));
        assertEquals("10 h left. Work will be finished on 3025-01-03.",
              WorkTimeEstimate.describeWorkCarriedOver(campaign, job(600, tech, null), tech));
    }

    @Test
    void carriedOverRepairCountsOvertimeWhenAllowed() {
        // 600 minutes fit in one 720-minute day with overtime, so the repair ends tomorrow
        Person tech = techWithTimeMultiplier(1.0);
        when(campaign.isOvertimeAllowed()).thenReturn(true);
        assertEquals("10 h left. Work will be finished tomorrow.",
              WorkTimeEstimate.describeWorkCarriedOver(campaign, job(600, tech, null), tech));
    }

    @Test
    void carriedOverReportForATechWithNoWorkingDayGivesNoDate() {
        Person tech = techWithTimeMultiplier(0.0);
        assertEquals("5 h left.",
              WorkTimeEstimate.describeWorkCarriedOver(campaign, job(300, tech, null), tech));
    }

    @Test
    void techDayShowsFreeTimeAndOvertime() {
        Person tech = techWithTimeMultiplier(1.0);
        when(tech.getMinutesLeft()).thenReturn(360);
        when(tech.getOvertimeLeft()).thenReturn(240);
        assertEquals("Free today: 6 h of 8 h", WorkTimeEstimate.describeTechDay(campaign, tech, false));
        assertEquals("Free today: 6 h of 8 h (+ 4 h overtime)", WorkTimeEstimate.describeTechDay(campaign, tech,
              true));
    }

    @Test
    void techDayNamesTheAdministrationChange() {
        // A Veteran administrator works 5% longer; with no Administration skill the day is 15% shorter
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, true);
        Person veteran = techWithTimeMultiplier(1.05);
        when(veteran.getMinutesLeft()).thenReturn(408);
        assertEquals("Free today: 6 h 48 min of 8 h 24 min (Administration +5%)",
              WorkTimeEstimate.describeTechDay(campaign, veteran, false));
        Person untrained = techWithTimeMultiplier(0.85);
        when(untrained.getMinutesLeft()).thenReturn(408);
        assertEquals("Free today: 6 h 48 min of 6 h 48 min (Administration -15%)",
              WorkTimeEstimate.describeTechDay(campaign, untrained, false));
    }

    @Test
    void techDayLeavesOutAdministrationWhenItChangesNothing() {
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, true);
        Person regular = techWithTimeMultiplier(1.0);
        when(regular.getMinutesLeft()).thenReturn(480);
        assertEquals("Free today: 8 h of 8 h", WorkTimeEstimate.describeTechDay(campaign, regular, false));

        // With the option off, a Veteran administrator gets the plain 8-hour day and no note
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, false);
        Person veteran = mock(Person.class);
        when(veteran.calculateTechTimeMultiplier(true)).thenReturn(1.05);
        when(veteran.calculateTechTimeMultiplier(false)).thenReturn(1.0);
        when(veteran.getMinutesLeft()).thenReturn(480);
        assertEquals("Free today: 8 h of 8 h", WorkTimeEstimate.describeTechDay(campaign, veteran, false));
    }

    @Test
    void techJobShowsWorkAndTheRollNeeded() {
        TechTaskEstimate estimate = new TechTaskEstimate(6, "", false, 72, 1350, 2);
        assertEquals("This job: 22 h 30 min of work, needs 6+ (72%)", WorkTimeEstimate.describeTechJob(estimate));

        TechTaskEstimate automatic = new TechTaskEstimate(TargetRoll.AUTOMATIC_SUCCESS, "", false, 100, 15, 0);
        assertEquals("This job: 15 min of work, automatic success", WorkTimeEstimate.describeTechJob(automatic));
    }

    @Test
    void impossibleTechJobGivesTheReasonSafeForTheRow() {
        TechTaskEstimate impossible = new TechTaskEstimate(TargetRoll.IMPOSSIBLE, "Busy with <Union>", true, 0, 60,
              0);
        assertEquals("Cannot do this job: Busy with &lt;Union&gt;", WorkTimeEstimate.describeTechJob(impossible));
        assertEquals("", WorkTimeEstimate.describeTechFinish(campaign, impossible));
    }

    @Test
    void techEstimateCountsOvertimeOnEveryLaterDay() {
        // 1,200 minutes with the day already used up: 3 more 8-hour days, but only 2 more with overtime, because
        // work carried overnight uses overtime too
        Person tech = techWithTimeMultiplier(1.0);
        IPartWork partWork = job(1200, null, null);
        when(campaign.getTargetFor(partWork, tech)).thenReturn(new TargetRoll(6, "test"));

        when(campaign.isOvertimeAllowed()).thenReturn(false);
        assertEquals(3, TechTaskEstimate.estimate(campaign, partWork, tech).daysToFinish());
        when(campaign.isOvertimeAllowed()).thenReturn(true);
        assertEquals(2, TechTaskEstimate.estimate(campaign, partWork, tech).daysToFinish());
    }

    @Test
    void techFinishGivesTheDate() {
        assertEquals("Finishes today", WorkTimeEstimate.describeTechFinish(campaign,
              new TechTaskEstimate(6, "", false, 72, 60, 0)));
        assertEquals("Finishes tomorrow, 3025-01-02", WorkTimeEstimate.describeTechFinish(campaign,
              new TechTaskEstimate(6, "", false, 72, 600, 1)));
        TechTaskEstimate twoDays = new TechTaskEstimate(6, "", false, 72, 1350, 2);
        assertEquals("Finishes 3025-01-03, in 2 days", WorkTimeEstimate.describeTechFinish(campaign, twoDays));
        assertEquals("This job: 22 h 30 min of work, needs 6+ (72%). Finishes 3025-01-03, in 2 days",
              WorkTimeEstimate.describeTechJobAndFinish(campaign, twoDays));
    }
}
