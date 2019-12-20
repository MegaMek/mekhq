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

    private double distanceScale = 0.35;
    private boolean isExtraRandom;

    public RangedPlanetSelector(int range) {
        this.range = range;
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
