/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.enums.HiringHallLevel;

/**
 * This is a PlanetarySystem object that will contain information about the system as well as an ArrayList of the Planet
 * objects that make up the system
 *
 * @author Taharqa
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = PlanetarySystem.PlanetarySystemPostLoader.class)
public class PlanetarySystem {
    private static final double COMMAND_CIRCUIT_RECHARGE_TIME_HOURS = 10;

    // --- Sophistication Rating Enum ---
    public enum PlanetarySophistication {
        ADVANCED(0, "Advanced"),
        A(1, "A"),
        B(2, "B"),
        C(3, "C"),
        D(4, "D"),
        F(5, "F"),
        REGRESSED(6, "Regressed");

        private final int index;
        private final String name;
        private static final Map<Integer, PlanetarySophistication> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, PlanetarySophistication> NAME_LOOKUP = new HashMap<>();

        static {
            for (PlanetarySophistication tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }

        PlanetarySophistication(int idx, String name) {
            this.index = idx;
            this.name = name;
        }

        public int getIndex() {return index;}

        public String getName() {return name;}

        public static PlanetarySophistication fromIndex(int idx) {
            PlanetarySophistication tr = INDEX_LOOKUP.get(idx);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetarySophistication index: " + idx);}
            return tr;
        }

        public static PlanetarySophistication fromName(String name) {
            PlanetarySophistication tr = NAME_LOOKUP.get(name);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetarySophistication name: " + name);}
            return tr;
        }
    }

    // --- Planetary Rating Enum ---
    public enum PlanetaryRating {
        A(0, "A"),
        B(1, "B"),
        C(2, "C"),
        D(3, "D"),
        F(4, "F");

        private final int index;
        private final String name;
        private static final Map<Integer, PlanetaryRating> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, PlanetaryRating> NAME_LOOKUP = new HashMap<>();

        static {
            for (PlanetaryRating tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }

        PlanetaryRating(int idx, String name) {
            this.index = idx;
            this.name = name;
        }

        public int getIndex() {return index;}

        public String getName() {return name;}

        public static PlanetaryRating fromIndex(int idx) {
            PlanetaryRating tr = INDEX_LOOKUP.get(idx);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetaryRating index: " + idx);}
            return tr;
        }

        public static PlanetaryRating fromName(String name) {
            PlanetaryRating tr = NAME_LOOKUP.get(name);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetaryRating name: " + name);}
            return tr;
        }
    }


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
    private SourceableValue<Integer> primarySlot;

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
        if (getPrimaryPlanetPosition() < 1 && null != id) {
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
        PlanetarySophistication tech = PlanetarySophistication.REGRESSED;
        PlanetaryRating industry = PlanetaryRating.F;
        PlanetaryRating rawMaterials = PlanetaryRating.F;
        PlanetaryRating output = PlanetaryRating.F;
        PlanetaryRating agriculture = PlanetaryRating.F;

        for (Planet planet : planets.values()) {
            SocioIndustrialData sic = planet.getSocioIndustrial(when);
            if (null != sic) {
                if (sic.tech.getIndex() < tech.getIndex()) {
                    tech = sic.tech;
                }
                if (sic.industry.getIndex() < industry.getIndex()) {
                    industry = sic.industry;
                }
                if (sic.rawMaterials.getIndex() < rawMaterials.getIndex()) {
                    rawMaterials = sic.rawMaterials;
                }
                if (sic.output.getIndex() < output.getIndex()) {
                    output = sic.output;
                }
                if (sic.agriculture.getIndex() < agriculture.getIndex()) {
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
     * @return the distance to another system in light years (0 if both are in the same system)
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
     * Use {@link #getRechargeTime(LocalDate, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public double getRechargeTime(LocalDate when) {
        return getRechargeTime(when, false);
    }

    /**
     * Calculates the recharge time for a jump ship in hours, based on a given date and whether command circuits are
     * used.
     *
     * <p>When recharging at a zenith or nadir jump point, the method returns the minimum of either the command
     * circuit recharge time (if command circuits are enabled) or 176 hours, and the standard solar recharge time. For
     * all other locations, only the solar recharge time is considered, unless using command circuits, in which case
     * their recharge time is also taken into account.</p>
     *
     * @param when                 the date for which the recharge time should be determined
     * @param isUseCommandCircuits {@code true} if command circuits are being used, possibly reducing recharge time at
     *                             specific locations
     *
     * @return the calculated recharge time in hours
     */
    public double getRechargeTime(LocalDate when, boolean isUseCommandCircuits) {
        if (isZenithCharge(when) || isNadirCharge(when)) {
            // The 176 value comes from pg. 87-88 and 138 of StratOps
            return Math.min(isUseCommandCircuits ? COMMAND_CIRCUIT_RECHARGE_TIME_HOURS : 176.0, getSolarRechargeTime());
        } else {
            return Math.min(isUseCommandCircuits ? COMMAND_CIRCUIT_RECHARGE_TIME_HOURS : Double.MAX_VALUE,
                  getSolarRechargeTime());
        }
    }

    public double getSolarRechargeTime() {
        return getStar().getSolarRechargeTime();
    }

    /**
     * Use {@link #getRechargeTimeText(LocalDate, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String getRechargeTimeText(LocalDate when) {
        return getRechargeTimeText(when, false);
    }

    /**
     * Returns a human-readable description of the recharge time for a jump ship based on the specified date and whether
     * command circuits are used.
     *
     * <p>If the recharge is not possible (i.e., the computed recharge time is infinite), returns a message
     * indicating impossibility; otherwise, returns the recharge time formatted as hours.</p>
     *
     * @param when                the date to evaluate recharge conditions
     * @param isUseCommandCircuit {@code true} if command circuits are in use, which may affect recharge time
     *
     * @return a string describing the recharge time or indicating if recharging is impossible
     */
    public String getRechargeTimeText(LocalDate when, boolean isUseCommandCircuit) {
        double time = getRechargeTime(when, isUseCommandCircuit);
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
     * @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary
     *       position
     */
    public double getTimeToJumpPoint(double acceleration) {
        return getTimeToJumpPoint(acceleration, getPrimaryPlanetPosition());
    }

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary
     *       position
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

    public SourceableValue<Integer> getSourcedPrimarySlot() {return primarySlot;}

    /**
     * @return the planet object identified by the primary slot. If no primary slot is given, then this function will
     *       return the first planet
     */
    public Planet getPrimaryPlanet() {
        return planets.get(getPrimaryPlanetPosition());
    }

    public int getPrimaryPlanetPosition() {
        if (null == getSourcedPrimarySlot()) {
            // if no primary slot (i.e., an uninhabited system) then return the first planet
            return 1;
        }
        return getSourcedPrimarySlot().getValue();
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
     *
     * @return A list of filtered academies based on the campaign.
     */
    public List<Academy> getFilteredAcademies(Campaign campaign) {
        final LocalDate currentDate = campaign.getLocalDate();
        AcademyFactory academyFactory = AcademyFactory.getInstance();

        List<String> excludedSets = List.of("Local Academies", "Unit Education");

        return academyFactory.getAllSetNames().stream()
                     .filter(setName -> !excludedSets.contains(setName) // Excluding certain setNames
                                              && (!setName.equalsIgnoreCase("Prestigious Academies")
                                                        ||
                                                        campaign.getCampaignOptions()
                                                              .isEnablePrestigiousAcademies())) // Additional
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
     *
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
     * Retrieves a string representation of the prestigious academies available in the system.
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
        public PlanetarySystem convert(PlanetarySystem planetarySystem) {
            if (null == planetarySystem.id) {
                planetarySystem.id = planetarySystem.name;
            }

            // fill up planets
            planetarySystem.planets = new TreeMap<>();
            if (null != planetarySystem.planetList) {
                for (Planet planet : planetarySystem.planetList) {
                    planet.setParentSystem(planetarySystem);
                    if (!planetarySystem.planets.containsKey(planet.getSystemPosition())) {
                        planetarySystem.planets.put(planet.getSystemPosition(), planet);
                    }
                }
                planetarySystem.planetList.clear();
            }
            planetarySystem.planetList = null;
            // Fill up events
            planetarySystem.events = new TreeMap<>();
            if (null != planetarySystem.eventList) {
                for (PlanetarySystemEvent systemEvent : planetarySystem.eventList) {
                    if ((null != systemEvent) && (null != systemEvent.date)) {
                        planetarySystem.events.put(systemEvent.date, systemEvent);
                    }
                }
                planetarySystem.eventList.clear();
            }
            planetarySystem.eventList = null;

            return planetarySystem;
        }
    }
}
