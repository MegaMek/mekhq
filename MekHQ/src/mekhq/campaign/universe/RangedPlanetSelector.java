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

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.joda.time.DateTime;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction.Tag;

public class RangedPlanetSelector extends AbstractPlanetSelector {

    private final int range;

    private double distanceScale = 0.45;
    private boolean isExtraRandom;

    public RangedPlanetSelector(int range) {
        this(range, /*isExtraRandom*/false);
    }

    public RangedPlanetSelector(int range, boolean isExtraRandom) {
        this.range = range;
        this.isExtraRandom = isExtraRandom;
    }

    public double getDistanceScale() {
        return distanceScale;
    }

    public void setDistanceScale(double distanceScale) {
        this.distanceScale = distanceScale;
    }

    public boolean isExtraRandom() {
        return isExtraRandom;
    }

    public void setExtraRandom(boolean b) {
        isExtraRandom = b;
    }

    @Override
    public Planet selectPlanet(Campaign campaign) {
        return selectPlanet(campaign, campaign.getFaction());
    }

    @Override
    public Planet selectPlanet(Campaign campaign, Faction faction) {
        DateTime now = Utilities.getDateTimeDay(campaign.getDate());

        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        TreeMap<Double, Planet> planets = new TreeMap<>();
        List<PlanetarySystem> systems = Systems.getInstance().getNearbySystems(campaign.getCurrentSystem(), range);

        double total = 0.0;
        for (PlanetarySystem system : systems) {
            double distance = system.getDistanceTo(currentSystem);
            if (faction.is(Tag.MERC) || system.getFactionSet(now).contains(faction)) {
                if (!isExtraRandom) {
                    Planet planet = system.getPrimaryPlanet();
                    Long pop = planet.getPopulation(now);
                    if (pop != null) {
                        total += 100.0 * Math.log10((long)pop) / (1 + distance * distanceScale);
                        planets.put(total, planet);
                    }
                } else {
                    for (Planet planet : system.getPlanets()) {
                        Long pop = planet.getPopulation(now);
                        if (pop != null) {
                            total += 100.0 * Math.log10((long)pop) / (1 + distance * distanceScale);
                            planets.put(total, planet);
                        }
                    }
                }
            }
        }

        if (planets.isEmpty()) {
            return currentSystem.getPrimaryPlanet();
        }

        double random = ThreadLocalRandom.current().nextDouble(planets.lastKey());
        return planets.ceilingEntry(random).getValue();
    }
}
