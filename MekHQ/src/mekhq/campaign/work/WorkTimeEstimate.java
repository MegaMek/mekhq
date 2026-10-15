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

import static mekhq.campaign.personnel.Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
import static mekhq.campaign.personnel.Person.PRIMARY_ROLE_SUPPORT_TIME;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Describes how long repairs, refits and reloads take, as the player sees it on the Repair tab, in the tech list and in
 * reports.
 *
 * <p>The estimate counts the working day of whoever does the job: the tech already on it, or for a DropShip,
 * JumpShip, WarShip or space station its own crew. An 8-hour day is assumed for a job nobody has started. Failed rolls
 * cannot be foreseen, so no estimate includes them.</p>
 */
public final class WorkTimeEstimate {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private WorkTimeEstimate() {}

    /**
     * Minutes a tech puts into a job each day: the normal working day, adjusted for their Administration skill when
     * the campaign uses that option, plus overtime if asked for.
     *
     * @param campaign       the campaign
     * @param tech           the tech, or {@code null} for an unstaffed job, which gets a plain 8-hour day
     * @param isWithOvertime {@code true} to add the tech's overtime
     *
     * @return minutes of work per day
     */
    public static int getMinutesPerDay(Campaign campaign, @Nullable Person tech, boolean isWithOvertime) {
        double multiplier = 1.0;
        if (tech != null) {
            multiplier = tech.calculateTechTimeMultiplier(
                  campaign.getCampaignOptions().get(CampaignOption.TECHS_USE_ADMINISTRATION));
        }
        // Rounded separately, as Person.resetMinutesLeft does
        int workingDay = (int) Math.round(PRIMARY_ROLE_SUPPORT_TIME * multiplier);
        int overtime = (int) Math.round(PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * multiplier);
        return isWithOvertime ? (workingDay + overtime) : workingDay;
    }

    /**
     * Describes the time left on a job, for example {@code 3 days (2 days with overtime)}.
     *
     * @param campaign the campaign
     * @param partWork the job
     *
     * @return the time left, as the player's chosen display shows it, or an empty string when the job needs no time
     */
    public static String describeTimeLeft(Campaign campaign, IPartWork partWork) {
        return describeTimeLeft(campaign, partWork, partWork.getTimeLeft(), true);
    }

    /**
     * Describes a given amount of work on a job, for example the time a different work mode would take.
     *
     * <ul>
     *   <li>A job someone is already on runs on their working day, plus overtime when the campaign allows it, and is
     *   marked as scheduled.</li>
     *   <li>A job nobody is on shows how long it runs with and without overtime, when the two differ.</li>
     *   <li>The Full display adds the hours of work in front of a job that runs past one day.</li>
     * </ul>
     *
     * @param campaign    the campaign
     * @param partWork    the job
     * @param minutesLeft minutes of work to describe
     *
     * @return the time left, as the player's chosen display shows it, or an empty string when there is no work, such
     *       as armor with none in stock
     */
    public static String describeTimeLeft(Campaign campaign, IPartWork partWork, int minutesLeft) {
        return describeTimeLeft(campaign, partWork, minutesLeft, false);
    }

    /**
     * @param isShowingProgress {@code true} to say how much of a started job is done, which only makes sense for the
     *                          job's real time left, not for the time another work mode would take
     */
    private static String describeTimeLeft(Campaign campaign, IPartWork partWork, int minutesLeft,
          boolean isShowingProgress) {
        if (minutesLeft <= 0) {
            return "";
        }
        WorkTimeDisplay display = MekHQ.getMHQOptions().getWorkTimeDisplay();
        LocalDate today = campaign.getLocalDate();
        // Refits never use overtime (Campaign.refit spends only the tech's regular minutes)
        boolean canUseOvertime = !(partWork instanceof Refit);
        Person scheduledTech = partWork.getTech();
        Person worker = (scheduledTech != null) ? scheduledTech : getShipCrewWorker(partWork.getUnit());
        int minutesPerDay = getMinutesPerDay(campaign, worker, false);

        String duration;
        int scheduledMinutesPerDay = 0;
        if (scheduledTech != null) {
            scheduledMinutesPerDay = getMinutesPerDay(campaign, scheduledTech,
                  canUseOvertime && campaign.isOvertimeAllowed());
            duration = WorkTimeFormatter.formatDuration(minutesLeft, scheduledMinutesPerDay, today, display);
        } else {
            duration = WorkTimeFormatter.formatDuration(minutesLeft, minutesPerDay, today, display);
            String durationWithOvertime = WorkTimeFormatter.formatDuration(minutesLeft,
                  getMinutesPerDay(campaign, worker, true), today, display);
            if (canUseOvertime && !duration.equals(durationWithOvertime)) {
                duration = getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.withOvertime", duration,
                      durationWithOvertime);
            }
        }

        boolean isShowingWorkToo = (display == WorkTimeDisplay.FULL) && (minutesLeft > minutesPerDay);
        if (isShowingWorkToo) {
            duration = getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.workAndDuration",
                  WorkTimeFormatter.formatWork(minutesLeft, display), duration);
        }
        if (scheduledTech == null) {
            return duration;
        }
        boolean hasProgress = isShowingProgress && (partWork.getTimeSpent() > 0);
        if (!hasProgress) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.scheduled", duration);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.scheduledWithProgress", duration,
              describeProgress(partWork, scheduledMinutesPerDay, display));
    }

    /**
     * @return how much of a started job is done: in days for a job longer than one working day, for example
     *       {@code 12 of 60 days}, otherwise in hours and minutes, for example {@code 2 h of 4 h}
     */
    private static String describeProgress(IPartWork partWork, int minutesPerDay, WorkTimeDisplay display) {
        int totalMinutes = partWork.getActualTime();
        int minutesSpent = partWork.getTimeSpent();
        boolean isCountedInDays = (display != WorkTimeDisplay.MINUTES) && (minutesPerDay > 0)
                                        && (totalMinutes > minutesPerDay);
        if (isCountedInDays) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.progressDays",
                  minutesSpent / minutesPerDay, WorkTimeFormatter.countWorkingDays(totalMinutes, minutesPerDay));
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.progressWork",
              WorkTimeFormatter.formatWork(minutesSpent, display), WorkTimeFormatter.formatWork(totalMinutes, display));
    }

    /**
     * @param unit the unit
     *
     * @return minutes of work left on every job the Repair tab lists for the unit
     */
    public static int getRepairMinutesLeft(Unit unit) {
        int minutes = 0;
        for (IPartWork partWork : unit.getPartsNeedingService()) {
            minutes += Math.max(0, partWork.getTimeLeft());
        }
        return minutes;
    }

    /**
     * Describes how long a unit's repairs take, for the unit list.
     *
     * <ul>
     *   <li>A DropShip, JumpShip, WarShip or space station is repaired only by its own crew, so its time is the
     *   calendar days that crew needs, for example {@code 6 days}.</li>
     *   <li>Any other unit shows its total work, for example {@code 22 h 30 min}, since several techs can share
     *   it.</li>
     * </ul>
     *
     * @param campaign the campaign
     * @param unit     the unit
     *
     * @return the repair time, which sorts by minutes of work left; {@code -} when there is no work
     */
    public static UnitRepairTime describeUnitRepairTime(Campaign campaign, Unit unit) {
        int minutes = getRepairMinutesLeft(unit);
        if (minutes == 0) {
            return new UnitRepairTime(0, "-");
        }
        WorkTimeDisplay display = MekHQ.getMHQOptions().getWorkTimeDisplay();
        Person shipCrew = getShipCrewWorker(unit);
        if (shipCrew == null) {
            return new UnitRepairTime(minutes, WorkTimeFormatter.formatWork(minutes, display));
        }
        String crewDays = WorkTimeFormatter.formatDuration(minutes, getMinutesPerDay(campaign, shipCrew, false),
              campaign.getLocalDate(), display);
        return new UnitRepairTime(minutes, crewDays);
    }

    /**
     * @param unit the unit
     *
     * @return what the unit list's repair time means for this unit
     */
    public static String describeUnitRepairTimeToolTip(Unit unit) {
        String key = (getShipCrewWorker(unit) == null) ? "WorkTimeEstimate.unitRepairTime.work.toolTip"
                           : "WorkTimeEstimate.unitRepairTime.crew.toolTip";
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * Describes a job that ran out of time today and carries on tomorrow, for the daily report, for example
     * {@code 12 h left. Work will be finished on 3151-01-04.}
     *
     * @param campaign the campaign
     * @param partWork the job, after today's work was added to it
     * @param tech     the tech who carries the job on
     *
     * @return the report text
     */
    public static String describeWorkCarriedOver(Campaign campaign, IPartWork partWork, Person tech) {
        MHQOptions options = MekHQ.getMHQOptions();
        String workLeft = WorkTimeFormatter.formatWork(partWork.getTimeLeft(), options.getWorkTimeDisplay());
        boolean isWithOvertime = !(partWork instanceof Refit) && campaign.isOvertimeAllowed();
        int minutesPerDay = getMinutesPerDay(campaign, tech, isWithOvertime);
        if (minutesPerDay <= 0) {
            // A tech with no working day cannot give a finishing date
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.noWorkingDay", workLeft);
        }
        int daysLeft = WorkTimeFormatter.countWorkingDays(partWork.getTimeLeft(), minutesPerDay);
        if (daysLeft <= 1) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.tomorrow", workLeft);
        }
        String finishDate = options.getDisplayFormattedDate(campaign.getLocalDate().plusDays(daysLeft));
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.date", workLeft, finishDate);
    }

    /**
     * Describes what choosing a work mode would do to a job, for the work mode menu: the time it would take, its
     * target number modifier, and how many skill levels a rush costs the tech. For example
     * {@code Extra time (x2): 4 h, TN -1}.
     *
     * @param campaign the campaign
     * @param partWork the job
     * @param workMode the work mode being offered
     *
     * @return the menu text for that work mode
     */
    public static String describeWorkMode(Campaign campaign, IPartWork partWork, WorkTime workMode) {
        // Every job's time scales with its work mode's multiplier, so the time in another mode follows from today's
        double modeTimeScale = workMode.timeMultiplier / partWork.getMode().timeMultiplier;
        int minutesInMode = (int) Math.ceil(partWork.getActualTime() * modeTimeScale);
        int minutesLeft = Math.max(0, minutesInMode - partWork.getTimeSpent());
        String timeLeft = describeTimeLeft(campaign, partWork, minutesLeft);
        String modeText = timeLeft.isEmpty() ? workMode.name
                                : getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.mode", workMode.name,
                                      timeLeft);
        StringBuilder text = new StringBuilder(modeText);
        int targetModifier = workMode.getMod(campaign.getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN));
        if (targetModifier != 0) {
            String signedModifier = ((targetModifier > 0) ? "+" : "") + targetModifier;
            text.append(getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.modeTargetNumber", signedModifier));
        }
        if (workMode.expReduction > 0) {
            text.append(getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.modeSkillReduction",
                  workMode.expReduction));
        }
        return text.toString();
    }

    /**
     * Describes a tech's time today for the tech list, for example {@code Free today: 6 h of 8 h (+ 4 h overtime)}.
     * When the campaign's Techs Use Administration option changes the tech's working day, the change is named, for
     * example {@code Free today: 6 h 48 min of 8 h 24 min (Administration +5%)}.
     *
     * @param campaign        the campaign
     * @param tech            the tech
     * @param overtimeAllowed {@code true} to show the overtime the tech has left
     *
     * @return the tech's time today
     */
    public static String describeTechDay(Campaign campaign, Person tech, boolean overtimeAllowed) {
        WorkTimeDisplay display = MekHQ.getMHQOptions().getWorkTimeDisplay();
        String freeToday = WorkTimeFormatter.formatWork(tech.getMinutesLeft(), display);
        String workingDay = WorkTimeFormatter.formatWork(getMinutesPerDay(campaign, tech, false), display);
        String techDay = overtimeAllowed ?
                               getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techDayWithOvertime",
                                     freeToday, workingDay,
                                     WorkTimeFormatter.formatWork(tech.getOvertimeLeft(), display)) :
                               getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techDay", freeToday,
                                     workingDay);
        String administrationChange = describeAdministrationChange(campaign, tech);
        return administrationChange.isEmpty() ? techDay : (techDay + ' ' + administrationChange);
    }

    /**
     * @return how much the tech's Administration skill lengthens or shortens their working day, for example
     *       {@code (Administration +5%)}, or an empty string when the option is off or the day is unchanged
     */
    private static String describeAdministrationChange(Campaign campaign, Person tech) {
        boolean isTechsUseAdministration = campaign.getCampaignOptions()
                                                 .get(CampaignOption.TECHS_USE_ADMINISTRATION);
        if (!isTechsUseAdministration) {
            return "";
        }
        long percentChange = Math.round((tech.calculateTechTimeMultiplier(true) - 1.0) * 100);
        if (percentChange == 0) {
            return "";
        }
        String signedPercent = ((percentChange > 0) ? "+" : "") + percentChange;
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techDayAdministration", signedPercent);
    }

    /**
     * Describes what a job asks of a tech, for example {@code This job: 22 h 30 min of work, needs 6+ (72%)}, or why
     * the tech cannot do it.
     *
     * @param estimate the tech's estimate for the job
     *
     * @return the job line for the tech list and the refit tech picker
     */
    public static String describeTechJob(TechTaskEstimate estimate) {
        if (estimate.isImpossible()) {
            // The reason can name a unit, so it is escaped before it goes into the row's HTML
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techJob.impossible",
                  StringEscapeUtils.escapeHtml4(estimate.targetDetails()));
        }
        String work = WorkTimeFormatter.formatWork(estimate.minutesNeeded(),
              MekHQ.getMHQOptions().getWorkTimeDisplay());
        if (estimate.targetNumber() == TargetRoll.AUTOMATIC_SUCCESS) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techJob.automatic", work);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techJob", work,
              String.valueOf(estimate.targetNumber()), String.valueOf(estimate.successPercent()));
    }

    /**
     * Describes when a tech would finish a job, for example {@code Finishes 3151-01-04, in 2 days}.
     *
     * @param campaign the campaign
     * @param estimate the tech's estimate for the job
     *
     * @return the finish line, or an empty string when the tech cannot do the job
     */
    public static String describeTechFinish(Campaign campaign, TechTaskEstimate estimate) {
        if (estimate.isImpossible()) {
            return "";
        }
        int daysToFinish = estimate.daysToFinish();
        if (daysToFinish == 0) {
            return getTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techFinish.today");
        }
        String finishDate = MekHQ.getMHQOptions()
                                  .getDisplayFormattedDate(campaign.getLocalDate().plusDays(daysToFinish));
        if (daysToFinish == 1) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techFinish.tomorrow", finishDate);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techFinish.days", finishDate, daysToFinish);
    }

    /**
     * Describes a job and when a tech would finish it on one line, for the refit tech picker, for example
     * {@code This job: 22 h 30 min of work, needs 6+ (72%). Finishes 3151-01-04, in 2 days}.
     *
     * @param campaign the campaign
     * @param estimate the tech's estimate for the job
     *
     * @return the job and its finish, or why the tech cannot do it
     */
    public static String describeTechJobAndFinish(Campaign campaign, TechTaskEstimate estimate) {
        String finish = describeTechFinish(campaign, estimate);
        if (finish.isEmpty()) {
            return describeTechJob(estimate);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.techJobAndFinish", describeTechJob(estimate),
              finish);
    }

    /**
     * @return the engineer who works on a self-crewed unit, or {@code null} when anyone suitable could be assigned
     */
    private static @Nullable Person getShipCrewWorker(@Nullable Unit unit) {
        return (unit == null) ? null : unit.getEngineer();
    }
}
