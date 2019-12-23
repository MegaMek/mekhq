/*
 * Copyright (C) 2019 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.joda.time.DateTime;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Faction.Tag;

public class RangedFactionSelector extends AbstractFactionSelector {

    private final int range;

    private boolean allowClan = false;
    private double distanceScale = 0.6;

    public RangedFactionSelector(int range) {
        this.range = range;
    }

    public boolean isAllowClan() {
        return allowClan;
    }

    public void setAllowClan(boolean allowClan) {
        this.allowClan = allowClan;
    }

    public double getDistanceScale() {
        return distanceScale;
    }

    public void setDistanceScale(double distanceScale) {
        this.distanceScale = distanceScale;
    }

    @Override
    public Faction selectFaction(Campaign campaign) {
        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        DateTime now = Utilities.getDateTimeDay(campaign.getDate());

        Map<Faction, Double> weights = new HashMap<>();
        Systems.getInstance().visitNearbySystems(currentSystem, range, planetarySystem -> {
            Planet planet = planetarySystem.getPrimaryPlanet();
            Long pop = planet.getPopulation(now);
            if (pop == null || (long)pop <= 0) {
                return;
            }

            double distance = planetarySystem.getDistanceTo(currentSystem);

            // Weight the faction by the planet's population divided by its
            // distance from our current system. The scaling factor is used
            // to affect the 'spread'.
            double delta = Math.log10((long)pop) / (1 + distance * distanceScale);
            for (Faction faction : planetarySystem.getFactionSet(now)) {
                if (faction.is(Tag.ABANDONED) || faction.is(Tag.HIDDEN) || faction.is(Tag.INACTIVE)
                    || faction.is(Tag.MERC)) {
                    continue;
                }

                if (faction.is(Tag.CLAN) && !allowClan) {
                    continue;
                }

                // each faction on planet is given even weighting
                // TODO: apply a weight based on the faction tag (e.g. MAJOR vs MINOR)
                weights.compute(faction, (f, total) -> total != null ? total + delta : delta);
            }
        });

        if (weights.isEmpty()) {
            return Faction.getFaction("MERC");
        }

        Set<Faction> enemies = getEnemies(campaign);

        //
        // Convert the tallied per-faction weights into a TreeMap
        // to perform roulette randomization
        //
        TreeMap<Double, Faction> factions = new TreeMap<>();
        double total = 0.0;
        for (Map.Entry<Faction, Double> entry : weights.entrySet()) {
            Faction faction = entry.getKey();
            double value = entry.getValue();

            // Only take factions which are not actively fighting us
            if (!enemies.contains(faction)) {
                double factionWeight = getFactionWeight(faction);
                total += value * factionWeight;
                factions.put(total, faction);
            }
        }

        // There is a chance they're a merc!
        Faction mercenaries = Faction.getFaction("MERC");
        if (factions.isEmpty()) {
            return mercenaries;
        }

        factions.put(total + total * getFactionWeight(mercenaries), mercenaries);

        double random = ThreadLocalRandom.current().nextDouble(factions.lastKey());
        return factions.ceilingEntry(random).getValue();
    }

    private Set<Faction> getEnemies(Campaign campaign) {
        Set<Faction> enemies = new HashSet<>();
        for (Contract contract : campaign.getActiveContracts()) {
            if (contract instanceof AtBContract) {
                enemies.add(Faction.getFaction(((AtBContract)contract).getEnemyCode()));
            }
        }
        return enemies;
    }

    private double getFactionWeight(Faction faction) {
        if (faction.isComstar() || faction.getShortName().equals("WOB")) {
            return 0.05;
        } else if (faction.is(Tag.MERC) || faction.is(Tag.SUPER) || faction.is(Tag.MAJOR)) {
            return 1.0;
        } else if (faction.is(Tag.MINOR)) {
            return 0.5;
        } else if (faction.is(Tag.SMALL)) {
            return 0.2;
        } else if (faction.is(Tag.REBEL) || faction.is(Tag.CHAOS)
                    || faction.is(Tag.TRADER) || faction.is(Tag.PIRATE)) {
            return 0.05;
        } else if (faction.is(Tag.CLAN)) {
            return 0.01;
        } else {
            // Don't include any of the other tags
            return 0.0;
        }
    }
}
