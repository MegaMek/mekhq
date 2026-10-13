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

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Estimates how long a repair, refit or reload will take, as the player sees it on the Repair tab and in reports.
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
     * Describes the time left on a job, for example {@code 6 days (4 days with overtime)}.
     *
     * @param campaign the campaign
     * @param partWork the job
     *
     * @return the time left, as the player's chosen display shows it
     */
    public static String describeTimeLeft(Campaign campaign, IPartWork partWork) {
        return describeTimeLeft(campaign, partWork, partWork.getTimeLeft());
    }

    /**
     * Describes a given amount of work on a job, for example the time a different work mode would take.
     *
     * <ul>
     *   <li>A job someone is already on counts down with their working day, plus overtime when the campaign allows
     *   it, and is marked as scheduled.</li>
     *   <li>A job nobody is on shows the time with and without overtime, when the two differ.</li>
     * </ul>
     *
     * @param campaign    the campaign
     * @param partWork    the job
     * @param minutesLeft minutes of work to describe
     *
     * @return the time left, as the player's chosen display shows it
     */
    public static String describeTimeLeft(Campaign campaign, IPartWork partWork, int minutesLeft) {
        WorkTimeDisplay display = MekHQ.getMHQOptions().getWorkTimeDisplay();
        // Refits never use overtime (Campaign.refit spends only the tech's regular minutes)
        boolean canUseOvertime = !(partWork instanceof Refit);
        Person scheduledTech = partWork.getTech();
        if (scheduledTech != null) {
            boolean isWithOvertime = canUseOvertime && campaign.isOvertimeAllowed();
            String countdown = WorkTimeFormatter.format(minutesLeft,
                  getMinutesPerDay(campaign, scheduledTech, isWithOvertime), campaign.getLocalDate(), display);
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.scheduled", countdown);
        }
        Person crewWorker = getShipCrewWorker(partWork.getUnit());
        String countdown = WorkTimeFormatter.format(minutesLeft, getMinutesPerDay(campaign, crewWorker, false),
              campaign.getLocalDate(), display);
        if (!canUseOvertime) {
            return countdown;
        }
        String countdownWithOvertime = WorkTimeFormatter.format(minutesLeft,
              getMinutesPerDay(campaign, crewWorker, true), campaign.getLocalDate(), display);
        if (countdown.equals(countdownWithOvertime)) {
            return countdown;
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.withOvertime", countdown,
              countdownWithOvertime);
    }

    /**
     * Describes a job that ran out of time today and carries on tomorrow, for the daily report, for example
     * {@code 2 days 3 h left. Work will be finished in 3 days.}
     *
     * @param campaign the campaign
     * @param partWork the job, after today's work was added to it
     * @param tech     the tech who carries the job on
     *
     * @return the report text
     */
    public static String describeWorkCarriedOver(Campaign campaign, IPartWork partWork, Person tech) {
        boolean isWithOvertime = !(partWork instanceof Refit) && campaign.isOvertimeAllowed();
        int minutesPerDay = getMinutesPerDay(campaign, tech, isWithOvertime);
        String timeLeft = WorkTimeFormatter.format(partWork.getTimeLeft(), minutesPerDay, campaign.getLocalDate(),
              MekHQ.getMHQOptions().getWorkTimeDisplay());
        if (minutesPerDay <= 0) {
            // A tech with no working day cannot give a finishing date
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.noWorkingDay", timeLeft);
        }
        int daysLeft = (int) Math.ceil((double) partWork.getTimeLeft() / minutesPerDay);
        if (daysLeft <= 1) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.tomorrow", timeLeft);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.carriedOver.days", timeLeft, daysLeft);
    }

    /**
     * Describes what choosing a work mode would do to a job, for the work mode menu: the time it would take, its
     * target number modifier, and how many skill levels a rush costs the tech. For example {@code Extra time (x2): 4
     * days, TN -1}.
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
        StringBuilder text = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeEstimate.mode",
              workMode.name, describeTimeLeft(campaign, partWork, minutesLeft)));
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
     * @return the engineer who works on a self-crewed unit, or {@code null} when anyone suitable could be assigned
     */
    private static @Nullable Person getShipCrewWorker(@Nullable Unit unit) {
        return (unit == null) ? null : unit.getEngineer();
    }
}
