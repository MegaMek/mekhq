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

    public RangedPlanetSelector(int range) {
        this.range = range;
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
                Planet planet = system.getPrimaryPlanet();
                Long pop = planet.getPopulation(now);
                if (pop != null) {
                    total += (double)(long)pop / Math.pow(10.0, distance / 10.0);
                    planets.put(total, planet);
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
