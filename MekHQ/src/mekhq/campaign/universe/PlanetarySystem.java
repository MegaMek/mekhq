/*
 * PlanetarySystem.java
 *
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2011-2025 - The MegaMek team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.enums.HiringHallLevel;

/**
 * This is a PlanetarySystem object that will contain information
 * about the system as well as an ArrayList of the Planet objects
 * that make up the system
 *
 * @author Taharqa
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonDeserialize(converter= PlanetarySystem.PlanetarySystemPostLoader.class)
public class PlanetarySystem {
    @JsonProperty("xcood")
    private Double x;
    @JsonProperty("ycood")
    private Double y;

    // Base data
    @JsonProperty("id")
    private String id;
    private String name;

    // Star data (to be factored out)
    @JsonProperty("spectralType")
    private SourceableValue<StarType> star;

    // tree map of planets sorted by system position
    private TreeMap<Integer, Planet> planets;

    // for reading in because lists are easier
    @JsonProperty("planet")
    private List<Planet> planetList;

    // the location of the primary planet for this system
    @JsonProperty("primarySlot")
    private int primarySlot;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     * Package-private so that Planets can access it
     */
    TreeMap<LocalDate, PlanetarySystemEvent> events;

    // For export and import only (lists are easier than maps) */
    @JsonProperty("event")
    private List<PlanetarySystemEvent> eventList;

    public PlanetarySystem() {

    }

    public PlanetarySystem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public String getName(LocalDate when) {
        // if no primary slot, then just return the id
        if (primarySlot < 1 && null != id) {
            return id;
        }

        if (null != getPrimaryPlanet()) {
            return getPrimaryPlanet().getName(when);
        }

        return "Unknown";
    }

    public List<String> getFactions(LocalDate when) {
        List<String> factions = new ArrayList<>();
        for (Planet planet : planets.values()) {
            List<String> f = planet.getFactions(when);
            if (null != f) {
                factions.addAll(f);
            }
        }
        return factions;
    }

    public Set<Faction> getFactionSet(LocalDate when) {
        Set<Faction> factions = new HashSet<>();
        for (Planet planet : planets.values()) {
            Set<Faction> f = planet.getFactionSet(when);
            if (null != f) {
                factions.addAll(f);
            }
        }
        // ignore cases where abandoned (ABN) is given in addition to real factions
        if (factions.size() > 1) {
            factions.remove(Factions.getInstance().getFaction("ABN"));
        }
        return factions;
    }

    public long getPopulation(LocalDate when) {
        long pop = 0L;
        for (Planet planet : planets.values()) {
            if (null != planet.getPopulation(when)) {
                pop += planet.getPopulation(when);
            }
        }
        return pop;
    }

    /** highest socio-industrial ratings among all planets in-system for the map **/
    public SocioIndustrialData getSocioIndustrial(LocalDate when) {
        int tech = EquipmentType.RATING_X;
        int industry = EquipmentType.RATING_X;
        int rawMaterials = EquipmentType.RATING_X;
        int output = EquipmentType.RATING_X;
        int agriculture = EquipmentType.RATING_X;

        for (Planet planet : planets.values()) {
            SocioIndustrialData sic = planet.getSocioIndustrial(when);
            if (null != sic) {
                if (sic.tech < tech) {
                    tech = sic.tech;
                }
                if (sic.industry < industry) {
                    industry = sic.industry;
                }
                if (sic.rawMaterials < rawMaterials) {
                    rawMaterials = sic.rawMaterials;
                }
                if (sic.output < output) {
                    output = sic.output;
                }
                if (sic.agriculture < agriculture) {
                    agriculture = sic.agriculture;
                }
            }
        }
        return new SocioIndustrialData(tech, industry, rawMaterials, output, agriculture);
    }

    /** @return the highest HPG rating among planets **/
    public HPGRating getHPG(LocalDate when) {
        HPGRating rating = HPGRating.X;
        for (Planet planet : planets.values()) {
            if ((null != planet.getHPG(when)) && (planet.getHPG(when).compareTo(rating) > 0)) {
                rating = planet.getHPG(when);
            }
        }
        return rating;
    }

    /** @return the highest Hiring Hall rating among planets **/
    public HiringHallLevel getHiringHallLevel(LocalDate when) {
        HiringHallLevel level = HiringHallLevel.NONE;
        for (Planet planet : planets.values()) {
            if ((null != planet.getHiringHallLevel(when)) && (planet.getHiringHallLevel(when).compareTo(level) > 0)) {
                level = planet.getHiringHallLevel(when);
            }
        }
        return level;
    }

    /** @return true if a hiring hall is present in the system **/
    public boolean isHiringHall(LocalDate when) {
        return !getHiringHallLevel(when).isNone();
    }

    /**
     * @return short name if set, else full name, else "unnamed"
     */
    public String getPrintableName(LocalDate when) {
        final String system = getName(when);
        return (system == null) ? "Unknown System" : system;
    }

    /**
     * @return the distance to a point in space in light years
     */
    public double getDistanceTo(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
    }

    /**
     * @return the distance to another system in light years (0 if both are in the
     *         same system)
     */
    public double getDistanceTo(PlanetarySystem anotherSystem) {
        return Math.sqrt(Math.pow(x - anotherSystem.x, 2) + Math.pow(y - anotherSystem.y, 2));
    }

    public Boolean isNadirCharge(LocalDate when) {
        return (null == getSourcedNadirCharge(when) ? false : getSourcedNadirCharge(when).getValue());
    }

    public SourceableValue<Boolean> getSourcedNadirCharge(LocalDate when) {
        return getEventData(when, null, e -> e.nadirCharge);
    }

    public boolean isZenithCharge(LocalDate when) {
        return (null == getSourcedZenithCharge(when) ? false : getSourcedZenithCharge(when).getValue());
    }

    public SourceableValue<Boolean> getSourcedZenithCharge(LocalDate when) {
        return getEventData(when, null, e -> e.zenithCharge);
    }

    public int getNumberRechargeStations(LocalDate when) {
        return (isNadirCharge(when) ? 1 : 0) + (isZenithCharge(when) ? 1 : 0);
    }

    public String getRechargeStationsText(LocalDate when) {
        boolean nadir = isNadirCharge(when);
        boolean zenith = isZenithCharge(when);
        if (nadir && zenith) {
            return "Zenith, Nadir";
        } else if (zenith) {
            return "Zenith";
        } else if (nadir) {
            return "Nadir";
        } else {
            return "None";
        }
    }

    /**
     * Recharge time in hours (assuming the usage of the fastest charging method
     * available)
     */
    public double getRechargeTime(LocalDate when) {
        if (isZenithCharge(when) || isNadirCharge(when)) {
            // The 176 value comes from pg. 87-88 and 138 of StratOps
            return Math.min(176.0, getSolarRechargeTime());
        } else {
            return getSolarRechargeTime();
        }
    }

    /**
     * Recharge time in hours using solar radiation alone (at jump point and 100%
     * efficiency)
     */
    public double getSolarRechargeTime() {
        return getStar().getSolarRechargeTime();
    }

    public String getRechargeTimeText(LocalDate when) {
        double time = getRechargeTime(when);
        if (Double.isInfinite(time)) {
            return "recharging impossible";
        } else {
            return String.format("%.0f hours", time);
        }
    }

    public double getStarDistanceToJumpPoint() {
        if (null == star) {
            // 40 is close to the midpoint value across all star types
            return StarUtil.getDistanceToJumpPoint(40);
        }
        return getStar().getDistanceToJumpPoint();
    }

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in
     *         Terran days for a given planetary position
     */
    public double getTimeToJumpPoint(double acceleration) {
        return getTimeToJumpPoint(acceleration, getPrimaryPlanetPosition());
    }

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in
     *         Terran days for a given planetary position
     */
    public double getTimeToJumpPoint(double acceleration, int sysPos) {
        return planets.get(sysPos).getTimeToJumpPoint(acceleration);
    }

    public StarType getStar() {
        return getSourcedStar().getValue();
    }

    public SourceableValue<StarType> getSourcedStar() {
        return star;
    }

    /**
     * @return the planet object identified by the primary slot.
     *         If no primary slot is given, then this function will return the first
     *         planet
     */
    public Planet getPrimaryPlanet() {
        return planets.get(getPrimaryPlanetPosition());
    }

    public int getPrimaryPlanetPosition() {
        // if no primary slot (i.e., an uninhabited system) then return the first planet
        return Math.max(primarySlot, 1);
    }

    public Planet getPlanet(int pos) {
        return planets.get(pos);
    }

    public Planet getPlanetById(String id) {
        for (Planet p : planets.values()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public Set<Integer> getPlanetPositions() {
        return planets.keySet();
    }

    public Collection<Planet> getPlanets() {
        return planets.values();
    }

    public String getIcon() {
        return getStar().getIcon();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final PlanetarySystem other = (PlanetarySystem) object;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public PlanetarySystemEvent getEvent(LocalDate when) {
        if ((null == when) || (null == events)) {
            return null;
        }
        return events.get(when);
    }

    protected <T> T getEventData(LocalDate when, T defaultValue, EventGetter<T> getter) {
        if ((null == when) || (null == events) || (null == getter)) {
            return defaultValue;
        }
        T result = defaultValue;
        for (LocalDate date : events.navigableKeySet()) {
            if (date.isAfter(when)) {
                break;
            }
            result = ObjectUtility.nonNull(getter.get(events.get(date)), result);
        }
        return result;
    }

    public List<PlanetarySystemEvent> getEvents() {
        if (null == events) {
            return null;
        }
        return new ArrayList<>(events.values());
    }

    private interface EventGetter<T> {
        T get(PlanetarySystemEvent e);
    }

    /** A class representing some event, possibly changing planetary information */
    public static final class PlanetarySystemEvent {

        @JsonProperty("date")
        public LocalDate date;
        @JsonProperty("nadirCharge")
        public SourceableValue<Boolean> nadirCharge;
        @JsonProperty("zenithCharge")
        public SourceableValue<Boolean> zenithCharge;
        // Events marked as "custom" are saved to scenario files and loaded from there
        public transient boolean custom = false;

        /**
         * @return <code>true</code> if the event doesn't contain any change
         */
        public boolean isEmpty() {
            return (null == nadirCharge) && (null == zenithCharge);
        }
    }

    /**
     * Retrieves a list of filtered academies based on the given campaign.
     *
     * @param campaign The campaign for filtering the academies.
     * @return A list of filtered academies based on the campaign.
     */
    public List<Academy> getFilteredAcademies(Campaign campaign) {
        final LocalDate currentDate = campaign.getLocalDate();
        AcademyFactory academyFactory = AcademyFactory.getInstance();

        List<String> excludedSets = List.of("Local Academies", "Unit Education");

        return academyFactory.getAllSetNames().stream()
                .filter(setName -> !excludedSets.contains(setName) // Excluding certain setNames
                        && (!setName.equalsIgnoreCase("Prestigious Academies")
                                || campaign.getCampaignOptions().isEnablePrestigiousAcademies())) // Additional
                                                                                                  // condition for
                                                                                                  // "Prestigious
                                                                                                  // Academies"
                .flatMap(setName -> getFilteredAcademiesForSet(currentDate, setName).stream())
                .toList();
    }

    /**
     * Retrieves a list of filtered academies for a given set and current date.
     *
     * @param currentDate The current date to filter the academies.
     * @param setName     The set name to filter the academies.
     * @return A list of filtered academies for the given set and current date.
     */
    private List<Academy> getFilteredAcademiesForSet(LocalDate currentDate, String setName) {
        return AcademyFactory.getInstance().getAllAcademiesForSet(setName).stream()
                .filter(academy -> academy.getLocationSystems().contains(this.getId())
                        && !academy.isLocal()
                        && !academy.isHomeSchool()
                        && !academy.getName().contains("(Officer)")
                        && currentDate.getYear() >= academy.getConstructionYear()
                        && currentDate.getYear() < academy.getClosureYear()
                        && currentDate.getYear() < academy.getDestructionYear())
                .sorted()
                .toList();
    }

    /**
     * Retrieves a string representation of the prestigious academies available in
     * the system.
     *
     * @return A string representation of the prestigious academies in the system.
     */
    public String getAcademiesForSystem(List<Academy> filteredAcademies) {
        StringBuilder academyString = new StringBuilder();

        for (Academy academy : filteredAcademies) { // there are not enough entries to justify a Stream
            academyString.append("<b>").append(academy.getName()).append("</b><br>")
                .append(academy.getDescription()).append("<br><br>");
        }

        return academyString.toString();
    }

    /** This class allows for some additional code on a planetary system after it is loaded by Jackson **/
    public static class PlanetarySystemPostLoader extends StdConverter<PlanetarySystem, PlanetarySystem> {

        @Override
        public PlanetarySystem convert(PlanetarySystem ps) {
            if (null == ps.id) {
                ps.id = ps.name;
            }

            // fill up planets
            ps.planets = new TreeMap<>();
            if (null != ps.planetList) {
                for (Planet p : ps.planetList) {
                    p.setParentSystem(ps);
                    if (!ps.planets.containsKey(p.getSystemPosition())) {
                        ps.planets.put(p.getSystemPosition(), p);
                    }
                }
                ps.planetList.clear();
            }
            ps.planetList = null;
            // Fill up events
            ps.events = new TreeMap<>();
            if (null != ps.eventList) {
                for (PlanetarySystemEvent event : ps.eventList) {
                    if ((null != event) && (null != event.date)) {
                        ps.events.put(event.date, event);
                    }
                }
                ps.eventList.clear();
            }
            ps.eventList = null;

            return ps;
        }
    }
}
