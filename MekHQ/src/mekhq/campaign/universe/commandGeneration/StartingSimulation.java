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
package mekhq.campaign.universe.commandGeneration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ratgenerator.Ruleset;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;

/**
 * Gives a new command a past (Run Starting Simulation).
 *
 * <p>The people generated arrive with no history. This walks back the number of years asked for and runs the
 * campaign's random marriage and procreation rules week by week over them, the way the retired AtB Company
 * Generator did, so some arrive married and some with children. The campaign's own date does not move, and
 * nothing else in the campaign is touched. Any injury a birth left behind is healed at the end, since the
 * command is meant to start ready for work.</p>
 *
 * <p>The rules used are the campaign's: a campaign whose random marriage or procreation method is off gets no
 * marriages or births from this, and the log says so.</p>
 */
public final class StartingSimulation {

    private static final MMLogger LOGGER = MMLogger.create(StartingSimulation.class);
    private static final String LOG_TAG = "[CompanyGen][Simulation]";
    /** Progress is reported this often, in weeks: once a quarter. */
    private static final int WEEKS_PER_PROGRESS_STEP = 13;

    /**
     * What the simulation did.
     *
     * @param weeksSimulated how many weeks were run
     * @param peopleHealed   how many people were healed of what the simulation left them with
     */
    public record Result(int weeksSimulated, int peopleHealed) {

        public static Result none() {
            return new Result(0, 0);
        }
    }

    private StartingSimulation() {
    }

    /**
     * Runs the simulation over the people generated.
     *
     * @param campaign the campaign whose rules and date are used
     * @param options  the generation options; nothing happens unless Run Starting Simulation is on
     * @param people   the people generated for the command; those born or married in during the run are not
     *                 themselves simulated
     * @param listener told of progress week by week; {@code null} for none
     *
     * @return what was done
     */
    public static Result run(Campaign campaign, CommandGenerationOptions options, List<Person> people,
          @Nullable Ruleset.ProgressListener listener) {
        if (!options.isRunStartingSimulation()) {
            LOGGER.info("{} off; the command starts with no history", LOG_TAG);
            return Result.none();
        }
        int years = options.getSimulationDuration();
        boolean marriages = options.isSimulateRandomMarriages();
        boolean births = options.isSimulateRandomProcreation();
        if ((years <= 0) || (!marriages && !births)) {
            LOGGER.info("{} nothing to simulate: {} year(s), marriages {}, births {}", LOG_TAG, years,
                  marriages ? "on" : "off", births ? "on" : "off");
            return Result.none();
        }

        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        AbstractMarriage marriage = humanResources.getMarriage();
        AbstractProcreation procreation = humanResources.getProcreation();
        if (marriages && (marriage instanceof DisabledRandomMarriage)) {
            LOGGER.info("{} the campaign's random marriage method is off, so no marriages will happen", LOG_TAG);
        }
        if (births && (procreation instanceof DisabledRandomProcreation)) {
            LOGGER.info("{} the campaign's random procreation method is off, so no births will happen", LOG_TAG);
        }

        List<Person> simulated = new ArrayList<>(people);
        int marriedBefore = countMarried(simulated);
        int personnelBefore = humanResources.getPersonnel().size();
        LocalDate today = campaign.getLocalDate();
        LocalDate date = today.minusYears(years);
        long totalWeeks = Math.max(1, ChronoUnit.WEEKS.between(date, today));
        LOGGER.info("{} running {} year(s) of history over {} people, from {} to {}", LOG_TAG, years,
              simulated.size(), date, today);

        int weeks = 0;
        while (date.isBefore(today)) {
            date = date.plusWeeks(1);
            if (date.isAfter(today)) {
                date = today;
            }
            for (Person person : simulated) {
                if (marriages) {
                    marriage.processNewWeek(campaign, date, person);
                }
                if (births) {
                    procreation.processNewWeek(campaign, date, person);
                }
            }
            weeks++;
            // A quarter at a time: the dialog logs every status change, and a decade is over five hundred weeks.
            boolean isQuarter = (weeks % WEEKS_PER_PROGRESS_STEP) == 0;
            boolean isLast = !date.isBefore(today);
            if ((listener != null) && (isQuarter || isLast)) {
                listener.updateProgress(Math.min(1.0, weeks / (double) totalWeeks),
                      "Simulating the command's history... " + date);
            }
        }

        int healed = healWhatTheSimulationLeft(campaign, simulated, today);
        int marriedAfter = countMarried(simulated);
        int joined = humanResources.getPersonnel().size() - personnelBefore;
        LOGGER.info("{} done: {} week(s) simulated over {} people; {} now married (was {}); {} new people joined"
                    + " as spouses or children; {} people healed afterwards",
              LOG_TAG, weeks, simulated.size(), marriedAfter, marriedBefore, joined, healed);
        return new Result(weeks, healed);
    }

    private static int countMarried(List<Person> people) {
        int married = 0;
        for (Person person : people) {
            if (person.getGenealogy().hasSpouse()) {
                married++;
            }
        }
        return married;
    }

    /**
     * A birth can leave the mother injured. The command starts ready for work, so those are healed.
     *
     * @return how many people were healed
     */
    private static int healWhatTheSimulationLeft(Campaign campaign, List<Person> people, LocalDate today) {
        boolean isAdvancedMedical = campaign.getCampaignOptions().get(CampaignOption.USE_ADVANCED_MEDICAL);
        int healed = 0;
        for (Person person : people) {
            if (!person.needsFixing()) {
                continue;
            }
            if (isAdvancedMedical) {
                person.clearInjuriesExcludingProsthetics(today);
            } else {
                while (person.needsFixing()) {
                    person.heal();
                }
            }
            healed++;
        }
        return healed;
    }
}
