/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.campaign.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import megamek.common.CompositeTechLevel;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;

/**
 * Provides an ITechnology interface for every MechSummary, optionally customized for a particular
 * faction. This requires loading each Entity and calculating the CompositeTechLevel. It usually
 * runs once when the campaign is loaded after the faction is set but also needs to run if date from
 * another faction is needed. This is usually a result of changing the faction or changing the option
 * to use faction-specific tech, but the data can be calculated for multiple factions and used, for example,
 * for a tracked OpFor. The calculation is performed on a separate thread and only blocks if the data is
 * needed before the task completes. There is also a non-blocking call.
 * 
 * @author Neoancient
 *
 */
public class UnitTechProgression {
    
    private final static UnitTechProgression instance = new UnitTechProgression();
    
    private Map<Integer, FutureTask<Map<MechSummary,ITechnology>>> techMap = new HashMap<>();
    
    /**
     * Initializes the data for a particular faction
     */
    public static void loadFaction(int techFaction) {
        instance.getTask(techFaction);
    }
    
    /**
     * Find the FutureTask associated with a particular faction. If no data has been generated for the
     * faction, start a thread to do so.
     * 
     * @param techFaction The faction for which to calculate progression data.
     * @return            The task responsible for calculating the data for the faction.
     */
    private FutureTask<Map<MechSummary,ITechnology>> getTask(int techFaction) {
        FutureTask<Map<MechSummary,ITechnology>> task = instance.techMap.get(techFaction);
        if (null == task) {
            task = new FutureTask<>(new BuildMapTask(techFaction));
            new Thread(task).start();
            instance.techMap.put(techFaction, task);
        }
        return task;
    }
    
    /**
     * Get a faction-specific ITechnology object that can be used to calculate tech levels for the given unit.
     * If values have not been generated for the techFaction, a new task will be started. 
     * 
     * @param unit          The <code>Unit</code> for which to calculate the tech progression.
     * @param techFaction   The faction to use in calculating the progression.
     * @param block         If the task has not completed this method will wait until completion if block is true,
     *                      or return null if block is false. If the task has completed, it will return the value
     *                      without waiting.
     * @return              An ITechnology object for the unit and faction. If the task has not completed and
     *                      block is false, or there was an exception processing the task, null is returned.
     */
    public static ITechnology getProgression(final Unit unit,
            final int techFaction, final boolean block) {
        MechSummary ms = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortName());
        if (null != ms) {
            return getProgression(ms, techFaction, block);
        } else {
            return null;
        }
    }
    
    /**
     * Get a faction-specific ITechnology object that can be used to calculate tech levels for the given unit.
     * If values have not been generated for the techFaction, a new task will be started. 
     * 
     * @param MechSummary   The <code>MechSummary</code> for which to calculate the tech progression.
     * @param techFaction   The faction to use in calculating the progression.
     * @param block         If the task has not completed this method will wait until completion if block is true,
     *                      or return null if block is false. If the task has completed, it will return the value
     *                      without waiting.
     * @return              An ITechnology object for the unit and faction. If the task has not completed and
     *                      block is false, or there was an exception processing the task, null is returned.
     */
    public static ITechnology getProgression(final MechSummary ms,
            final int techFaction, final boolean block) {
        FutureTask<Map<MechSummary,ITechnology>> task = instance.getTask(techFaction);
        if (!block && !task.isDone()) {
            return null;
        }
        try {
            Map<MechSummary,ITechnology> map = task.get();
            if (!map.containsKey(ms)) {
                map.put(ms, calcTechProgression(ms, techFaction));
            }
            return map.get(ms);
        } catch (InterruptedException e) {
            task.cancel(true);
        } catch (ExecutionException e) {
            MekHQ.getLogger().log(UnitTechProgression.class,
                    "getProgression(MechSummary,int,boolean)", e);
        }
        return null;
    }
    
    private static ITechnology calcTechProgression(MechSummary ms, int techFaction) {
        final String METHOD_NAME = "calcTechProgression(MechSummary, int)"; // $NON-NLS-1$
        try {
            Entity en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            if (null == en) {
                MekHQ.getLogger().log(BuildMapTask.class, METHOD_NAME, LogLevel.ERROR,
                        "Entity was null: " + ms.getName());
            }
            return en.factionTechLevel(techFaction);
        } catch (EntityLoadingException ex) {
            MekHQ.getLogger().log(BuildMapTask.class, METHOD_NAME, LogLevel.ERROR,
                    "Exception loading entity " + ms.getName());
            MekHQ.getLogger().log(BuildMapTask.class, METHOD_NAME, ex);
            return null;
        }
    }
    
    /**
     * Goes through all the entries in MechSummaryCache, loads them, and calculates the composite
     * tech level of all the equipment and construction options for a specific faction.
     */
    private class BuildMapTask implements Callable<Map<MechSummary,ITechnology>> {
        private int techFaction;
        
        BuildMapTask(int techFaction) {
            this.techFaction = techFaction;
        }

        // Load all the Entities in the MechSummaryCache and calculate the tech level for the given faction.
        @Override
        public Map<MechSummary, ITechnology> call() throws Exception {
            Map<MechSummary,ITechnology> map = new HashMap<MechSummary,ITechnology>();
            for (MechSummary ms : MechSummaryCache.getInstance().getAllMechs()) {
                map.put(ms, calcTechProgression(ms, techFaction));
            }
            return map;
        }
    };
}
