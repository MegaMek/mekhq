/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import megamek.common.enums.Faction;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Provides an ITechnology interface for every MekSummary, optionally customized for a particular faction. This requires
 * loading each Entity and calculating the CompositeTechLevel. It usually runs once when the campaign is loaded after
 * the faction is set but also needs to run if date from another faction is needed. This is usually a result of changing
 * the faction or changing the option to use faction-specific tech, but the data can be calculated for multiple factions
 * and used, for example, for a tracked OpFor. The calculation is performed on a separate thread and only blocks if the
 * data is needed before the task completes. There is also a non-blocking call.
 *
 * @author Neoancient
 */
public class UnitTechProgression {
    private static final MMLogger LOGGER = MMLogger.create(UnitTechProgression.class);

    private static final UnitTechProgression instance = new UnitTechProgression();

    private final Map<Faction, FutureTask<Map<MekSummary, ITechnology>>> techMap = new HashMap<>();

    /**
     * Initializes the data for a particular faction
     */
    public static void loadFaction(Faction techFaction) {
        instance.getTask(techFaction);
    }

    /**
     * Find the FutureTask associated with a particular faction. If no data has been generated for the faction, start a
     * thread to do so.
     *
     * @param techFaction The faction for which to calculate progression data.
     *
     * @return The task responsible for calculating the data for the faction.
     */
    private FutureTask<Map<MekSummary, ITechnology>> getTask(Faction techFaction) {
        FutureTask<Map<MekSummary, ITechnology>> task = instance.techMap.get(techFaction);
        if (null == task) {
            task = new FutureTask<>(new BuildMapTask(techFaction));
            new Thread(task).start();
            instance.techMap.put(techFaction, task);
        }
        return task;
    }

    /**
     * Get a faction-specific ITechnology object that can be used to calculate tech levels for the given unit. If values
     * have not been generated for the techFaction, a new task will be started.
     *
     * @param unit        The <code>Unit</code> for which to calculate the tech progression.
     * @param techFaction The faction to use in calculating the progression.
     * @param block       If the task has not completed this method will wait until completion if block is true, or
     *                    return null if block is false. If the task has completed, it will return the value without
     *                    waiting.
     *
     * @return An ITechnology object for the unit and faction. If the task has not completed and block is false, or
     *       there was an exception processing the task, null is returned.
     */
    public static ITechnology getProgression(final Unit unit,
          final Faction techFaction, final boolean block) {
        MekSummary ms = MekSummaryCache.getInstance().getMek(unit.getEntity().getShortName());
        if (null != ms) {
            return getProgression(ms, techFaction, block);
        } else {
            return null;
        }
    }

    /**
     * Get a faction-specific ITechnology object that can be used to calculate tech levels for the given unit. If values
     * have not been generated for the techFaction, a new task will be started.
     *
     * @param ms          The <code>MekSummary</code> for which to calculate the tech progression.
     * @param techFaction The faction to use in calculating the progression.
     * @param block       If the task has not completed this method will wait until completion if block is true, or
     *                    return null if block is false. If the task has completed, it will return the value without
     *                    waiting.
     *
     * @return An ITechnology object for the unit and faction. If the task has not completed and block is false, or
     *       there was an exception processing the task, null is returned.
     */
    public static ITechnology getProgression(final MekSummary ms, final Faction techFaction,
          final boolean block) {
        FutureTask<Map<MekSummary, ITechnology>> task = instance.getTask(techFaction);
        if (!block && !task.isDone()) {
            return null;
        }
        try {
            Map<MekSummary, ITechnology> map = task.get();
            if (!map.containsKey(ms)) {
                map.put(ms, calcTechProgression(ms, techFaction));
            }
            return map.get(ms);
        } catch (InterruptedException e) {
            task.cancel(true);
        } catch (ExecutionException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    private static ITechnology calcTechProgression(MekSummary ms, Faction techFaction) {
        try {
            Entity en = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            if (null == en) {
                LOGGER.error("Entity was null: {}", ms.getName());
                return null;
            }
            return en.factionTechLevel(techFaction);
        } catch (EntityLoadingException ex) {
            LOGGER.error("Exception loading entity {}", ms.getName(), ex);
            return null;
        }
    }

    /**
     * Goes through all the entries in MekSummaryCache, loads them, and calculates the composite tech level of all the
     * equipment and construction options for a specific faction.
     */
    private record BuildMapTask(Faction techFaction) implements Callable<Map<MekSummary, ITechnology>> {

        // Load all the Entities in the MekSummaryCache and calculate the tech level for
        // the given faction.
        @Override
        public Map<MekSummary, ITechnology> call() {
            Map<MekSummary, ITechnology> map = new HashMap<>();
            for (MekSummary mekSummary : MekSummaryCache.getInstance().getAllMeks()) {
                map.put(mekSummary, calcTechProgression(mekSummary, techFaction));
            }
            return map;
        }
    }
}
