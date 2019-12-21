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
    private double distanceScale = 0.45;
    private double mercLikelihood = 0.5;

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

    /**
     * @return the mercLikelihood
     */
    public double getMercLikelihood() {
        return mercLikelihood;
    }

    public void setMercLikelihood(double fraction) {
        mercLikelihood = fraction;
    }

    @Override
    public Faction selectFaction(Campaign campaign) {
        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        DateTime now = Utilities.getDateTimeDay(campaign.getDate());

        Map<Faction, Double> weights = new HashMap<>();
        Systems.getInstance().visitNearbySystems(currentSystem, range, planetarySystem -> {
            Planet planet = planetarySystem.getPrimaryPlanet();
            Long pop = planet.getPopulation(now);
            if (pop == null) {
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

        Set<Faction> enemies = getEnemies(campaign);

        //
        // Convert the tallied per-faction weights into a TreeMap
        // to perform roulette randomization
        //
        TreeMap<Double, Faction> factions = new TreeMap<>();
        double total = 0.0, min = weights.values().stream().reduce(0.0, Double::sum) * 0.01;
        for (Map.Entry<Faction, Double> faction : weights.entrySet()) {
            // Only take factions which will have a probability > 1%
            // ...and are not actively fighting us
            if (faction.getValue() > min && !enemies.contains(faction.getKey())) {
                total += faction.getValue();
                factions.put(total, faction.getKey());
            }
        }

        // There is a chance they're a merc!
        factions.put(total + total * mercLikelihood, Faction.getFaction("MERC"));

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
}
