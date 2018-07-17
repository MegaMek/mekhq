/**
 * 
 */
package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

import mekhq.Utilities;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public class FactionBorders {
    
    private final Faction faction;
    private Set<Planet> planets;
    private RegionBorder border;
    
    public FactionBorders(Faction faction, Campaign c) {
        this.faction = faction;
        calculateRegion(Utilities.getDateTimeDay(c.getCalendar()));
    }
    
    /**
     * Finds all planets currently owned (completely or partially) by the faction and finds
     * its border.
     * 
     * @param when The date for testing faction ownership.
     */
    public void calculateRegion(DateTime when) {
        calculateRegion(when, Planets.getInstance().getPlanets().values());
    }
    
    /**
     * Finds all planets within the supplied collection currently owned (completely or partially)
     * by the faction and finds its border. This can be used to narrow the area to one section of
     * the galaxy.
     * 
     * @param when    The date for testing faction ownership.
     * @param planets The set of planets to include in the region.
     */
    public void calculateRegion(DateTime when, Collection<Planet> planets) {
        planets = planets.stream()
                .filter(p -> p.getFactionSet(when).contains(faction))
                .collect(Collectors.toSet());
        border = new RegionBorder(planets);
    }
    
    /**
     * @return The faction used to calculate the borders
     */
    public Faction getFaction() {
        return faction;
    }
    
    /**
     * @return The planets controlled by the indicated faction at the indicated time.
     */
    public Set<Planet> getPlanets() {
        return Collections.unmodifiableSet(planets);
    }
    
    /**
     * @return A polygon that surrounds the region controlled by this faction.
     */
    public RegionBorder getBorder() {
        return border;
    }
    
    /**
     * Finds planets of another faction that on a border with this faction.
     * 
     * @param other       The other faction's region.
     * @param borderSize  The size of the border.
     * @return            All planets from the other faction that are within borderSize light years
     *                    of one of this faction's planets.
     */
    List<Planet> getBorderPlanets(FactionBorders other, double borderSize) {
        List<RegionBorder.Point> intersection = border.intersection(other.getBorder(), borderSize);
        if (intersection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Planet> theirPlanets = other.getPlanets().stream()
                .filter(p -> RegionBorder.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted((p1, p2) -> Double.compare(p1.getX(), p2.getX()))
                .collect(Collectors.toList());
        List<Planet> ourPlanets = getPlanets().stream()
                .filter(p -> RegionBorder.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted((p1, p2) -> Double.compare(p1.getX(), p2.getX()))
                .collect(Collectors.toList());
        
        List<Planet> retVal = new ArrayList<>();
        int start = 0;
        for (Planet p : theirPlanets) {
            // As we work through the list of potential enemy planets we move the start index
            // of the friendly planets so we don't have to iterate over the ones out of range.
            while ((start < ourPlanets.size()) && (p.getX() > ourPlanets.get(start).getX() + borderSize)) {
                start++;
            }
            if (start >= ourPlanets.size()) {
                break;
            }
            for (int i = start; i < ourPlanets.size(); i++) {
                final Planet p2 = ourPlanets.get(i);
                if (p.getX() < p2.getX() - borderSize) {
                    break;
                }
                // We're going to do a cheap bounding rectangle check first to determine whether
                // the more computationally expensive distance calculation is even necessary
                if ((p2.getY() > p.getY() - borderSize)
                        && (p2.getY() < p.getY() + borderSize)
                        && (p.getDistanceTo(p2) <= borderSize)) {
                    retVal.add(p);
                }
            }
        }
        
        return retVal;
    }

}
