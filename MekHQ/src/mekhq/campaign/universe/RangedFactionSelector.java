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
    private double distanceScale = 0.35;
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

        List<PlanetarySystem> systems = Systems.getInstance().getNearbySystems(currentSystem, range);

        DateTime now = Utilities.getDateTimeDay(campaign.getDate());

        Map<Faction, Double> weights = new HashMap<>();
        for (PlanetarySystem planetarySystem : systems) {
            Planet planet = planetarySystem.getPrimaryPlanet();
            Long pop = planet.getPopulation(now);
            if (pop == null) {
                continue;
            }
            double distance = planetarySystem.getDistanceTo(currentSystem);
            double delta = 100.0 * Math.log10((long)pop) / (1 + distance * distanceScale);
            for (Faction faction : planetarySystem.getFactionSet(now)) {
                if (faction.is(Tag.ABANDONED) || faction.is(Tag.HIDDEN) || faction.is(Tag.INACTIVE)
                    || faction.is(Tag.MERC)) {
                    continue;
                }

                if (faction.is(Tag.CLAN) && !allowClan) {
                    continue;
                }

                // each faction on planet is given even weighting...perhaps not ideal
                weights.compute(faction, (f, total) -> total != null ? total + delta : delta);
            }
        }

        Set<Faction> enemies = getEnemies(campaign);

        TreeMap<Double, Faction> factions = new TreeMap<>();
        double total = 0.0;
        for (Map.Entry<Faction, Double> faction : weights.entrySet()) {
            // always add to the total, even if we don't add the faction.
            // this ensures low probability factions don't jump to the top.
            if (faction.getValue() > 0.01 && !enemies.contains(faction.getKey())) {
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
