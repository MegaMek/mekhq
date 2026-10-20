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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * The jobs each tech is in the middle of: repairs or salvage still under way, refits, and units being mothballed or
 * activated. A tech in the middle of a job is busy, even with minutes free today, because the job will keep claiming
 * their time until it is done. For example, a tech with 450 of 480 minutes going to maintenance takes eight days to
 * finish a four-hour repair.
 *
 * <p>The jobs are gathered once, when the snapshot is taken, so a table can colour every row from it without searching
 * the campaign for each one.</p>
 */
public final class TechJobs {
    private static final MMLogger LOGGER = MMLogger.create(TechJobs.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private final Map<UUID, List<String>> jobsByTech;

    private TechJobs(Map<UUID, List<String>> jobsByTech) {
        this.jobsByTech = jobsByTech;
    }

    /**
     * @return a snapshot with no jobs, for places that do not track them
     */
    public static TechJobs none() {
        return new TechJobs(Map.of());
    }

    /**
     * Takes a snapshot of every job a tech is in the middle of.
     *
     * @param campaign the campaign
     *
     * @return the jobs, by tech
     */
    public static TechJobs find(Campaign campaign) {
        Map<UUID, List<String>> jobsByTech = new HashMap<>();
        for (Unit unit : campaign.getUnits()) {
            for (Part part : unit.getParts()) {
                addPartJob(campaign, jobsByTech, part);
            }
            Refit refit = unit.getRefit();
            if ((refit != null) && (refit.getTech() != null)) {
                addJob(jobsByTech, refit.getTech(), getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.refit",
                      unit.getName(), describeTimeLeft(campaign, refit.getTech(), refit.getTimeLeft())));
            }
            if (unit.isMothballing() && (unit.getTech() != null)) {
                String key = unit.isMothballed() ? "TechJobs.activate" : "TechJobs.mothball";
                addJob(jobsByTech, unit.getTech(), getFormattedTextAt(RESOURCE_BUNDLE, key, unit.getName(),
                      describeTimeLeft(campaign, unit.getTech(), unit.getMothballTime())));
            }
        }
        List<LocalWarehouse> warehouses = new ArrayList<>();
        warehouses.add(campaign.getPlayerForce().getWarehouse());
        for (PlayerBase playerBase : campaign.getCampaignLocationManager().getPlayerBases()) {
            warehouses.add(playerBase.getBaseWarehouse());
        }
        for (LocalWarehouse warehouse : warehouses) {
            for (Part part : new ArrayList<>(warehouse.getParts())) {
                // Parts on a unit were counted with the unit
                if (part.getUnit() == null) {
                    addPartJob(campaign, jobsByTech, part);
                }
            }
        }
        LOGGER.debug("[TechJobs] {} techs are in the middle of a job", jobsByTech.size());
        return new TechJobs(jobsByTech);
    }

    /**
     * @param tech a tech
     *
     * @return the jobs the tech is in the middle of, each described for the player; empty if none
     */
    public List<String> getJobs(Person tech) {
        return jobsByTech.getOrDefault(tech.getId(), List.of());
    }

    private static void addPartJob(Campaign campaign, Map<UUID, List<String>> jobsByTech, Part part) {
        Person tech = part.getTech();
        if (tech == null) {
            return;
        }
        String timeLeft = describeTimeLeft(campaign, tech, part.getTimeLeft());
        Unit unit = part.getUnit();
        String job;
        if (unit == null) {
            job = getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.benchRepair", part.getName(), timeLeft);
        } else if (part.isSalvaging()) {
            job = getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.salvage", part.getName(), unit.getName(), timeLeft);
        } else {
            job = getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.repair", part.getName(), unit.getName(), timeLeft);
        }
        addJob(jobsByTech, tech, job);
    }

    private static void addJob(Map<UUID, List<String>> jobsByTech, Person tech, String job) {
        jobsByTech.computeIfAbsent(tech.getId(), key -> new ArrayList<>()).add(job);
    }

    /**
     * Says how long the rest of a job will take, at the minutes the tech has left each day after maintenance. A tech
     * spending most of the day on maintenance takes far longer than the job's hours suggest.
     */
    private static String describeTimeLeft(Campaign campaign, Person tech, int minutesLeft) {
        if (minutesLeft <= 0) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.finishing");
        }
        int minutesPerDay = WorkTimeEstimate.getMinutesPerDay(campaign, tech, false);
        int minutesFreeForJobs = Math.max(1, minutesPerDay - tech.getMaintenanceTimeUsing());
        String duration = WorkTimeFormatter.formatDuration(minutesLeft, minutesFreeForJobs, campaign.getLocalDate(),
              MekHQ.getMHQOptions().getWorkTimeDisplay());
        return getFormattedTextAt(RESOURCE_BUNDLE, "TechJobs.timeLeft", duration);
    }
}
