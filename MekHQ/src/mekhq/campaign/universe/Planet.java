/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2011-2022 - The MegaMek Team. All Rights Reserved.
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

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.Utilities;
import mekhq.adapter.*;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.universe.Faction.Tag;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDate;
import java.util.*;

/**
 * This is the start of a planet object that will keep lots of information about
 * planets that can be displayed on the interstellar map.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
@XmlRootElement(name = "planet")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Planet {
    @XmlElement(name = "xcood")
    private Double x;
    @XmlElement(name = "ycood")
    private Double y;

    // Base data
    @SuppressWarnings("unused")
    private UUID uniqueIdentifier;
    private String id;
    private String name;
    private String shortName;
    private Integer sysPos;

    //Orbital information
    /** orbital radius (average distance to parent star), in AU */
    @XmlElement(name = "orbitalDist")
    private Double orbitRadius;

    // Stellar neighbourhood
    //for reading in because lists are easier
    @XmlElement(name = "satellite")
    private List<Satellite> satellites;
    @XmlElement(name = "smallMoons")
    private int smallMoons;
    @XmlElement(name = "ring")
    private boolean ring;

    // Global physical characteristics
    @XmlElement(name = "type")
    private String planetType;
    /** diameter in km */
    private double diameter;
    /** Density in g/m^3 */
    private Double density;
    private Double gravity;
    private Double dayLength;
    private Double yearLength;

    @XmlElement(name = "class")
    private String className;

    // Surface description
    @XmlElement(name = "water")
    private Integer percentWater;
    @XmlElement(name = "volcanism")
    private Integer volcanicActivity;
    @XmlElement(name = "tectonics")
    private Integer tectonicActivity;
    @XmlElement(name="landMass")
    private List<String> landMasses;

    // Atmospheric description
    /** Pressure classification */
    @XmlJavaTypeAdapter(PressureAdapter.class)
    private Integer pressure;
    @XmlJavaTypeAdapter(AtmosphereAdapter.class)
    private Atmosphere atmosphere;
    private String composition;
    private Integer temperature;

    // Ecosphere
    @XmlElement(name = "lifeForm")
    @XmlJavaTypeAdapter(LifeFormAdapter.class)
    private LifeForm life;

    // Human influence
    private Long population;
    @XmlJavaTypeAdapter(SocioIndustrialDataAdapter.class)
    private SocioIndustrialData socioIndustrial;
    @XmlJavaTypeAdapter(HPGRatingAdapter.class)
    private Integer hpg;
    @XmlElement(name = "faction")
    @XmlJavaTypeAdapter(StringListAdapter.class)
    private List<String> factions;

    //private List<String> garrisonUnits;

    //the system that this planet belongs to
    private PlanetarySystem parentSystem;

    // Fluff
    private String desc;
    private String icon;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     */
    @XmlTransient
    private TreeMap<LocalDate, PlanetaryEvent> events;

    /**
     * This is a cache of the current event data based
     * on the latest date given. {@link Planet#refreshEvents()}
     * should be called if event data has been modified
     * or the current date moved backwards.
     */
    @XmlTransient
    CurrentEvents currentEvents;

    //a hash to keep track of dynamic garrison changes
    //TreeMap<LocalDate, List<String>> garrisonHistory;

    /** @deprecated Use "event", which can have any number of changes to the planetary data */
    @Deprecated
    @XmlElement(name = "factionChange")
    private List<FactionChange> factionChanges;
    // For export and import only (lists are easier than maps) */
    @XmlElement(name = "event")
    private List<Planet.PlanetaryEvent> eventList;

    /** Marker for "please delete this planet" */
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    public Boolean delete;

    public Planet() {
    }

    public Planet(String id) {
        this.id = id;
    }

    /**
     * Overloaded constructor that parses out a single line of tsv data for a planet
     * with the help of a list of event years
     * @param tsvData tab-separated data line
     * @param years The list of years acquired from the tsv file
     * @throws Exception
     */
    public Planet(String tsvData, List<LocalDate> years) throws Exception {
        eventList = new ArrayList<>();
        events = new TreeMap<>();

        // map of faction names that are different in the SUCS data, but have a correspondence to our factions
        Map<String, String> factionReplacements = new HashMap<>();
        factionReplacements.put("LC", "LA");
        factionReplacements.put("U", "UND");
        factionReplacements.put("A", "ABN");
        factionReplacements.put("I", "IND");
        factionReplacements.put("", "UND"); // no data. defaulting to "undiscovered"

        try {
            // "Name" \t X-coordinate \t Y-coordinate \t "Ownership info".
            //      "Ownership info" breaks down to "FactionCode, irrelevantstuff"
            String[] infoElements = tsvData.split("\t");

            // sometimes, names are formatted like this:
            // Primary Name (Alternate Name)
            // Primary Name (Alternate Name YEAR+)

            String nameString = infoElements[0].replace("\"", ""); // get rid of surrounding quotation marks
            int plusIndex = nameString.indexOf('+');
            int nameChangeYear = 2000;

            // this indicates that there's an (Alternate Name YEAR+) here
            if (plusIndex > 0) {
                String yearString = nameString.substring(plusIndex - 4, plusIndex);
                nameChangeYear = Integer.parseInt(yearString);
            }

            LocalDate nameChangeYearDate = LocalDate.ofYearDay(nameChangeYear, 1);

            String altName;
            String primaryName = nameString;
            int parenIndex = nameString.indexOf('(');
            int closingParenIndex = nameString.indexOf(')');
            // this indicates that there's an (Alternate Name) sequence of some kind
            if (parenIndex > 0) {
                // we chop off the year if there is one
                if (plusIndex > 0) {
                    altName = nameString.substring(parenIndex + 1, plusIndex - 5);
                }
                // otherwise, we just chop off the closing paren
                else {
                    altName = nameString.substring(parenIndex + 1, closingParenIndex);
                }

                // there are a few situations where all this stuff with parens is for naught, which is
                // PlanetName (FactionCode) or if the PlanetName (AltName) is already in our planets "database"

                if ((null == Factions.getInstance().getFaction(altName)) && (null == Systems.getInstance().getSystemById(primaryName))) {
                    primaryName = nameString.substring(0, parenIndex - 1);

                    PlanetaryEvent nameChangeEvent = getOrCreateEvent(nameChangeYearDate);
                    nameChangeEvent.name = altName;

                    eventList.add(nameChangeEvent);
                }
            }

            // now we have a primary name
            this.name = primaryName;

            this.id = this.name;
            this.x = Double.parseDouble(infoElements[1]);
            this.y = Double.parseDouble(infoElements[2]);

            for (int x = 3; x < infoElements.length; x++) {
                String infoElement = infoElements[x].replace("\"", "");
                String newFaction;

                int commaIndex = infoElement.indexOf(',');
                if (commaIndex < 0) { // sometimes there are no commas
                    newFaction = infoElement;
                } else {
                    // anything after the first comma is fluff
                    // we also want to forego the opening quote
                    newFaction = infoElement.substring(0, commaIndex);
                }

                //dirty hack, replace faction name with one we can use
                if (factionReplacements.containsKey(newFaction)) {
                    newFaction = factionReplacements.get(newFaction);
                }

                // for brevity, only add the new event if the faction hasn't changed since the previous event
                // or if it's the first event

                // dirty hack here assumes that there's only one faction per event, which is true in the case
                // of this spreadsheet
                if ((x == 3) || !eventList.get(eventList.size() - 1).faction.get(0).equals(newFaction)) {
                    LocalDate eventDate = years.get(x - 3);

                    PlanetaryEvent pe = events.getOrDefault(eventDate, new PlanetaryEvent());
                    pe.faction = new ArrayList<>();
                    pe.faction.add(newFaction);

                    if (!eventList.contains(pe)) {
                        eventList.add(pe);
                    }
                }
            }
        } catch (Exception e) {
            Exception ne = new Exception("Error running Planet constructor with following line:\n" + tsvData);
            ne.addSuppressed(e);
            throw(ne);
        }
    }

    // Constant base data

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public Double getGravity() {
        return gravity;
    }

    public Double getDensity() {
        return density;
    }

    public double getDiameter() {
        return diameter;
    }

    public String getGravityText() {
        return null != gravity ? gravity + "g" : "unknown";
    }

    public Double getOrbitRadius() {
        return orbitRadius;
    }

    public void setParentSystem(PlanetarySystem system) {
        parentSystem = system;
    }


    public ArrayList<Satellite> getSatellites() {
        return null != satellites ? new ArrayList<>(satellites) : null;
    }

    public int getSmallMoons() {
        return smallMoons;
    }

    public String getSatelliteDescription() {
        String desc = "";
        if (null != satellites) {
            List<String> satNames = new ArrayList<>();
            for (Satellite satellite : satellites) {
                satNames.add(satellite.getDescription());
            }
            desc = Utilities.combineString(satNames, ", ");
        }
        if (smallMoons > 0) {
            String smallDesc = smallMoons + " small moons";
            if (desc.isBlank()) {
                desc = smallDesc;
            } else {
                desc = desc + ", " + smallDesc;
            }
        }
        if (hasRing()) {
            desc = desc + ", and a dust ring";
        }
        return desc;
    }

    public boolean hasRing() {
        return ring;
    }


    public List<String> getLandMasses() {
        return null != landMasses ? new ArrayList<>(landMasses) : null;
    }

    public String getLandMassDescription() {
        return null != landMasses ? Utilities.combineString(landMasses, ", ") : "";
    }

    public Integer getVolcanicActivity() {
        return volcanicActivity;
    }

    public Integer getTectonicActivity() {
        return tectonicActivity;
    }

    public Double getDayLength(LocalDate when) {
        //yes day length can change because Venus
        return getEventData(when, dayLength, e -> e.dayLength);
    }

    public Double getYearLength() {
        return yearLength;
    }

    public String getPlanetType() {
        return planetType;
    }

    public Integer getSystemPosition() {
        return sysPos;
    }

    /**
     * This function returns a system position for the planet that does not account for asteroid belts. Therefore
     * this result may be different than that actual sysPos variable.
     * @return String of system position after removing asteroid belts
     */
    public String getDisplayableSystemPosition() {
        //We won't give the actual system position here, because we don't want asteroid belts to count
        //for system position
        if ((null == getParentSystem()) || (null == sysPos)) {
            return "?";
        }
        int pos = 0;
        for (int i = 1; i <= sysPos; i++) {
            if (getParentSystem().getPlanet(i).getPlanetType().equals("Asteroid Belt")) {
                continue;
            }
            pos++;
        }
        return Integer.toString(pos);
    }

    public String getDescription() {
        return desc;
    }

    public String getIcon() {
        return icon;
    }

    // Constant stellar data (to be moved out later)

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public PlanetarySystem getParentSystem() {
        return parentSystem;
    }

    // Date-dependant data

    public synchronized PlanetaryEvent getOrCreateEvent(LocalDate when) {
        if (null == when) {
            return null;
        }
        if (null == events) {
            events = new TreeMap<>();
        }
        PlanetaryEvent event = events.get(when);
        if (null == event) {
            event = new PlanetaryEvent();
            event.date = when;
            events.put(when, event);
        }
        currentEvents = null;
        return event;
    }

    public PlanetaryEvent getEvent(LocalDate when) {
        if ((null == when) || (null == events)) {
            return null;
        }
        return events.get(when);
    }

    public List<PlanetaryEvent> getEvents() {
        if (null == events) {
            return null;
        }
        return new ArrayList<>(events.values());
    }

    public List<PlanetaryEvent> getCustomEvents() {
        List<PlanetaryEvent> customEvents = new ArrayList<>();
        if (events != null) {
            for (PlanetaryEvent event : events.values()) {
                if (event.custom) {
                    customEvents.add(event);
                }
            }
        }
        return Collections.unmodifiableList(customEvents);
    }

    protected <T> T getEventData(LocalDate when, T defaultValue, EventGetter<T> getter) {
        if ((null == when) || (null == events) || (null == getter)) {
            return defaultValue;
        }

        PlanetaryEvent event = getCurrentEvent(when);

        T result = getter.get(event);

        return ObjectUtility.nonNull(result, defaultValue);
    }

    private synchronized PlanetaryEvent getCurrentEvent(LocalDate now) {
        if (currentEvents == null) {
            currentEvents = new CurrentEvents();
        }

        return currentEvents.getCurrentEvent(now);
    }

    /**
     * This methed signals that the internal cache of event data
     * should be refreshed. This should be called when any
     * field on a planetary event is updated, or if any events
     * are added and/or removed.
     */
    public synchronized void refreshEvents() {
        currentEvents = null;
    }

    /**
     * This class tracks the current {@link PlanetaryEvent}.
     */
    class CurrentEvents {
        private LocalDate lastUpdated;
        private PlanetaryEvent planetaryEvent = new PlanetaryEvent();
        private Map.Entry<LocalDate, PlanetaryEvent> nextEvent;
        private Iterator<Map.Entry<LocalDate, PlanetaryEvent>> eventStream;

        private void initialize(LocalDate now) {
            lastUpdated = now;
            if (events != null) {
                eventStream = events.entrySet().iterator();
                if (eventStream.hasNext()) {
                    nextEvent = eventStream.next();
                }
            }
        }

        /**
         * Gets the current {@link PlanetaryEvent} for the time.
         * @param now The current time.
         * @return The up-to-date {@link PlanetaryEvent} as of {@code now}.
         */
        public PlanetaryEvent getCurrentEvent(LocalDate now) {
            if ((lastUpdated == null) || lastUpdated.isAfter(now)) {
                // initialize ourselves if we're fresh or if we
                // went back in time (which breaks how the event stream works)
                initialize(now);
            }

            // if we have no more events for this planet,
            // or if our current date is before the next date
            // return our cached event
            if ((nextEvent == null) || now.isBefore(nextEvent.getKey())) {
                return planetaryEvent;
            }

            // fast-forward to the next event
            do {
                planetaryEvent.copyDataFrom(nextEvent.getValue());
                if (eventStream.hasNext()) {
                    nextEvent = eventStream.next();
                } else {
                    nextEvent = null;
                }

            } while ((nextEvent != null) && !now.isBefore(nextEvent.getKey()));

            return planetaryEvent;
        }
    }

    /** @return events for this year. Never returns <i>null</i>. */
    public List<PlanetaryEvent> getEvents(int year) {
        if (null == events) {
            return Collections.emptyList();
        }
        List<PlanetaryEvent> result = new ArrayList<>();
        for (LocalDate date : events.navigableKeySet()) {
            if (date.getYear() > year) {
                break;
            }
            if (date.getYear() == year) {
                result.add(events.get(date));
            }
        }
        return result;
    }

    public String getName(LocalDate when) {
        return getEventData(when, name, e -> e.name);
    }

    public String getShortName(LocalDate when) {
        return getEventData(when, shortName, e -> e.shortName);
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();

        for (PlanetaryEvent p : events.values()) {
            names.add(p.name);
        }

        return names;
    }

    /** @return short name if set, else full name, else "unnamed" */
    public String getPrintableName(LocalDate when) {
        String result = getShortName(when);
        if (null == result) {
            result = getName(when);
        }
        return null != result ? result : "unnamed";
    }

    public SocioIndustrialData getSocioIndustrial(LocalDate when) {
        return getEventData(when, socioIndustrial, e -> e.socioIndustrial);
    }

    public String getSocioIndustrialText(LocalDate when) {
        SocioIndustrialData sid = getSocioIndustrial(when);
        return null != sid ? sid.toString() : "";
    }

    public Integer getHPG(LocalDate when) {
        return getEventData(when, hpg, e -> e.hpg);
    }

    public String getHPGClass(LocalDate when) {
        return StarUtil.getHPGClass(getHPG(when));
    }

    public Long getPopulation(LocalDate when) {
        return getEventData(when, population, e -> e.population);
    }


    public LifeForm getLifeForm(LocalDate when) {
        return getEventData(when, null != life ? life : LifeForm.NONE, e -> e.lifeForm);
    }

    public String getLifeFormName(LocalDate when) {
        return getLifeForm(when).name;
    }

    public Integer getPercentWater(LocalDate when) {
        return getEventData(when, percentWater, e -> e.percentWater);
    }

    public Integer getTemperature(LocalDate when) {
        return getEventData(when, temperature, e -> e.temperature);
    }

    public Integer getPressure(LocalDate when) {
        return getEventData(when, pressure, e -> e.pressure);
    }

    public String getPressureName(LocalDate when) {
        megamek.common.planetaryconditions.Atmosphere currentPressure = megamek.common.planetaryconditions.Atmosphere.getAtmosphere(getPressure(when));
        return null != currentPressure ? currentPressure.toString() : "unknown";
    }

    public Atmosphere getAtmosphere(LocalDate when) {
        return getEventData(when, null != atmosphere ? atmosphere : Atmosphere.NONE, e -> e.atmosphere);
    }

    public String getAtmosphereName(LocalDate when) {
        return getAtmosphere(when).name;
    }

    public String getComposition(LocalDate when) {
        return getEventData(when, composition, e -> e.composition);
    }

    public List<String> getFactions(LocalDate when) {
        List<String> retVal = getEventData(when, factions, e -> e.faction);
        if (retVal != null) {
            return retVal;
        }
        return Collections.emptyList();
    }

    private static Set<Faction> getFactionsFrom(Collection<String> codes) {
        if (null == codes) {
            return Collections.emptySet();
        }
        Set<Faction> factions = new HashSet<>(codes.size());
        for (String code : codes) {
            factions.add(Factions.getInstance().getFaction(code));
        }
        return factions;
    }

    /** @return set of factions at a given date */
    public Set<Faction> getFactionSet(LocalDate when) {
        List<String> currentFactions = getFactions(when);
        return getFactionsFrom(currentFactions);
    }

    public String getShortDesc(LocalDate when) {
        return getShortName(when) + " (" + getFactionDesc(when) + ")";
    }

    public String getFactionDesc(LocalDate when) {
        String toReturn = Faction.getFactionNames(getFactionSet(when), when.getYear());
        if (toReturn.isEmpty()) {
            toReturn = "Uncolonized";
        }
        return toReturn;
    }

    /** @return the distance to another planet in light years (0 if both are in the same system) */
    public double getDistanceTo(Planet anotherPlanet) {
        return Math.sqrt(Math.pow(x - anotherPlanet.x, 2) + Math.pow(y - anotherPlanet.y, 2));
    }

    /** @return the distance to a point in space in light years */
    public double getDistanceTo(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
    }

    // Astronavigation

    /** @return the average travel time from low orbit to the jump point at 1g, in Terran days */
    public double getTimeToJumpPoint(double acceleration) {
        //based on the formula in StratOps
        return Math.sqrt((getDistanceToJumpPoint() * 1000) / (StarUtil.G * acceleration)) / 43200;
    }

    /** @return the average distance to the system's jump point in km */
    public double getDistanceToJumpPoint() {
        if (null == parentSystem) {
            LogManager.getLogger().error("reference to planet with no parent system");
            return 0;
        }
        return Math.sqrt(Math.pow(getOrbitRadiusKm(), 2) + Math.pow(parentSystem.getStarDistanceToJumpPoint(), 2));
    }

    public double getOrbitRadiusKm() {
        if (null == orbitRadius) {
            //TODO: figure out a better way to handle missing orbit radius (really this should not be missing)
            return 0.5 * StarUtil.AU;
        }
        return  orbitRadius * StarUtil.AU;
    }


    /**
     * Returns whether the planet has not been discovered or is a dead planet. This code was adapted from
     * InterstellarPlanetMapPanel.isPlanetEmpty
     * @param when - the <code>LocalDate</code> object indicating what time we are asking about.
     * @return true if the planet is empty; false if the planet is not empty
     */
    public boolean isEmpty(LocalDate when) {
        Set<Faction> factions = getFactionSet(when);
        if ((null == factions) || factions.isEmpty()) {
            return true;
        }

        for (Faction faction : factions) {
            if (!faction.is(Tag.ABANDONED)) {
                return false;
            }
        }

        return true;
    }

    /**
     * A function to return any planetary related modifiers to a target roll for acquiring
     * parts. Feeds in the campaign options because this will include important information
     * about these mods as well as faction information.
     *
     * @param target - current TargetRoll for acquisitions
     * @param when - a LocalDate object for the campaign to retrieve information from the planet
     * @param options - the campaign options from which important values need to be determined
     * @return an updated TargetRoll with planet specific mods
     */
    public TargetRoll getAcquisitionMods(TargetRoll target, LocalDate when, CampaignOptions options, Faction faction, boolean clanPart) {
        //check faction limitations
        Set<Faction> planetFactions = getFactionSet(when);
        if (null != planetFactions) {
            boolean enemies = false;
            boolean neutrals = false;
            boolean allies = false;
            boolean ownFaction = false;
            boolean clanCrossover = true;
            boolean noClansPresent = true;
            for (Faction planetFaction : planetFactions) {
                if (faction.equals(planetFaction)) {
                    ownFaction = true;
                }
                if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(faction, planetFaction, when)) {
                    enemies = true;
                } else if (RandomFactionGenerator.getInstance().getFactionHints().isAlliedWith(faction, planetFaction, when)) {
                    allies = true;
                } else {
                    neutrals = true;
                }

                if (faction.isClan()) {
                    noClansPresent = false;
                }
                if (faction.isClan() == planetFaction.isClan()) {
                    clanCrossover = false;
                }
            }
            if (!ownFaction) {
                if (enemies && !neutrals && !allies
                        && !options.getPlanetAcquisitionFactionLimit().generateOnEnemyPlanets()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "No supplies from enemy planets");
                } else if (neutrals && !allies
                        && !options.getPlanetAcquisitionFactionLimit().generateOnNeutralPlanets()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "No supplies from neutral planets");
                } else if (allies && !options.getPlanetAcquisitionFactionLimit().generateOnAlliedPlanets()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "No supplies from allied planets");
                } else if (clanCrossover && options.isPlanetAcquisitionNoClanCrossover()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "The clans and inner sphere do not trade supplies");
                }
            }

            if (noClansPresent && clanPart) {
                if (options.isNoClanPartsFromIS()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "No clan parts from non-clan factions");
                }
                target.addModifier(options.getPenaltyClanPartsFromIS(), "clan parts from non-clan faction");
            }
        }

        SocioIndustrialData socioIndustrial = getSocioIndustrial(when);
        if (null == socioIndustrial) {
            //nothing has been coded for this planet, so we will assume C across the board
            socioIndustrial = new SocioIndustrialData();
            socioIndustrial.tech = EquipmentType.RATING_C;
            socioIndustrial.industry = EquipmentType.RATING_C;
            socioIndustrial.output = EquipmentType.RATING_C;
            socioIndustrial.rawMaterials = EquipmentType.RATING_C;
            socioIndustrial.agriculture = EquipmentType.RATING_C;
        }

        //don't allow acquisitions from caveman planets
        if ((socioIndustrial.tech == EquipmentType.RATING_X)
                || (socioIndustrial.industry == EquipmentType.RATING_X)
                || (socioIndustrial.output == EquipmentType.RATING_X)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,"Regressed: Pre-industrial world");
        }

        target.addModifier(options.getPlanetTechAcquisitionBonus(socioIndustrial.tech),
                "planet tech: " + ITechnology.getRatingName(socioIndustrial.tech));
        target.addModifier(options.getPlanetIndustryAcquisitionBonus(socioIndustrial.industry),
                "planet industry: " + ITechnology.getRatingName(socioIndustrial.industry));
        target.addModifier(options.getPlanetOutputAcquisitionBonus(socioIndustrial.output),
                "planet output: " + ITechnology.getRatingName(socioIndustrial.output));

        return target;

    }

    // JAXB marshalling support

    @SuppressWarnings({ "unused", "unchecked" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (null == id) {
            id = name;
        }

        // Fill up events
        events = new TreeMap<>();
        if (null != eventList) {
            for (PlanetaryEvent event : eventList) {
                if ((null != event) && (null != event.date)) {
                    events.put(event.date, event);
                }
            }
            eventList.clear();
        }
        eventList = null;
        // Merge faction change events into the event data
        if (null != factionChanges) {
            for (FactionChange change : factionChanges) {
                if ((null != change) && (null != change.date)) {
                    PlanetaryEvent event = getOrCreateEvent(change.date);
                    event.faction = change.faction;
                }
            }
            factionChanges.clear();
        }
        factionChanges = null;
    }

    @SuppressWarnings("unused")
    private boolean beforeMarshal(Marshaller marshaller) {
        // Fill up our event list from the internal data type
        eventList = new ArrayList<>(events.values());
        return true;
    }

    /**
     * Updates the current planet's coordinates and faction ownership from the given other planet.
     * Makes several assumptions about the way the other planet's ownership events are structured.
     * @param tsvPlanet The planet from which to update.
     * @param dryRun Whether to actually perform the updates.
     * @return Human readable form of what was/would have been updated.
     */
    public String updateFromTSVPlanet(final Planet tsvPlanet, boolean dryRun) {
        StringBuilder sb = new StringBuilder();

        if (!tsvPlanet.x.equals(this.x) || !tsvPlanet.y.equals(this.y)) {
            sb.append("Coordinate update from ").append(x).append(", ").append(y).append(" to ")
                    .append(tsvPlanet.x).append(", ").append(tsvPlanet.y).append("\r\n");

            if (!dryRun) {
                this.x = tsvPlanet.x;
                this.y = tsvPlanet.y;
            }
        }

        // loop using index
        // look ahead by one event (if possible) and check that getFaction(next event year) isn't already
        // the same as the faction from the current event : sometimes, our data is more exact than the incoming data
        List<PlanetaryEvent> tsvEvents = tsvPlanet.getEvents();
        for (int eventIndex = 0; eventIndex < tsvEvents.size(); eventIndex++) {
            PlanetaryEvent event = tsvEvents.get(eventIndex);
            // check other planet events (currently only updating faction change events)
            // if the other planet has an 'ownership change' event with a non-"U" faction
            // check that this planet does not have an existing non-"U" faction already owning it at the event date
            // and does not acquire such a faction between this and the next event
            // Then we will add the ownership change event
            if ((event.faction != null) && !event.faction.isEmpty()) {
                // the purpose of this code is to evaluate whether the current "other planet" event
                // is a faction change to an active, valid faction.
                Faction eventFaction = Factions.getInstance().getFaction(event.faction.get(0));
                boolean eventHasActualFaction = eventFaction != null && (!eventFaction.is(Tag.INACTIVE) && !eventFaction.is(Tag.ABANDONED));

                if (eventHasActualFaction) {
                    List<String> currentFactions = this.getFactions(event.date);

                    // if this planet has an "inactive and abandoned" current faction...
                    // we also want to catch the situation where the next faction change isn't to the same exact faction
                    if ((currentFactions.size() == 1)
                            && Factions.getInstance().getFaction(currentFactions.get(0)).is(Tag.INACTIVE)
                            && Factions.getInstance().getFaction(currentFactions.get(0)).is(Tag.ABANDONED)) {
                        // now we travel into the future, to the next "other" event, and if this planet has acquired a faction
                        // before the next "other" event, then we
                        int nextEventIndex = eventIndex + 1;
                        PlanetaryEvent nextEvent = nextEventIndex < tsvEvents.size() ? tsvEvents.get(nextEventIndex) : null;
                        LocalDate nextEventDate;

                        // if we're at the last event, then just check that the planet doesn't have a faction in the year 3600
                        if (nextEvent == null) {
                            nextEventDate = LocalDate.ofYearDay(3600, 1);
                        } else {
                            nextEventDate = nextEvent.date;
                        }

                        List<String> nextFactions = this.getFactions(nextEventDate);
                        boolean factionBeforeNextEvent = !(nextFactions.size() == 1 &&
                                Factions.getInstance().getFaction(nextFactions.get(0)).is(Tag.INACTIVE) &&
                                Factions.getInstance().getFaction(nextFactions.get(0)).is(Tag.ABANDONED));

                        if (!factionBeforeNextEvent) {
                            sb.append("Adding faction change in ").append(event.date.getYear())
                                    .append(" from ").append(currentFactions.get(0)).append(" to ")
                                    .append(event.faction).append("\r\n");

                            if (!dryRun) {
                                this.events.put(event.date, event);
                            }
                        }
                    }
                }
            }
        }

        if (sb.length() > 0) {
            sb.insert(0, "Updating planet " + this.getId() + "\r\n");
        }

        return sb.toString();
    }

    /**
     * Copy all but id from the other planet. Update event list. Events with the
     * same date as others already in the list get overwritten, others added.
     * To effectively delete an event, simply create a new one with <i>just</i> the date.
     */
    public void copyDataFrom(Planet other) {
        if (null != other) {
            // We don't change the ID
            name = ObjectUtility.nonNull(other.name, name);
            shortName = ObjectUtility.nonNull(other.shortName, shortName);
            x = ObjectUtility.nonNull(other.x, x);
            y = ObjectUtility.nonNull(other.y, y);
            desc = ObjectUtility.nonNull(other.desc, desc);
            factions = ObjectUtility.nonNull(other.factions, factions);
            gravity = ObjectUtility.nonNull(other.gravity, gravity);
            hpg = ObjectUtility.nonNull(other.hpg, hpg);
            landMasses = ObjectUtility.nonNull(other.landMasses, landMasses);
            life = ObjectUtility.nonNull(other.life, life);
            percentWater = ObjectUtility.nonNull(other.percentWater, percentWater);
            pressure = ObjectUtility.nonNull(other.pressure, pressure);
            atmosphere = ObjectUtility.nonNull(other.atmosphere, atmosphere);
            volcanicActivity = ObjectUtility.nonNull(other.volcanicActivity, volcanicActivity);
            tectonicActivity = ObjectUtility.nonNull(other.tectonicActivity, tectonicActivity);
            population = ObjectUtility.nonNull(other.population, population);
            dayLength = ObjectUtility.nonNull(other.dayLength, dayLength);
            smallMoons = ObjectUtility.nonNull(other.smallMoons, smallMoons);
            satellites = ObjectUtility.nonNull(other.satellites, satellites);
            sysPos = ObjectUtility.nonNull(other.sysPos, sysPos);
            temperature = ObjectUtility.nonNull(other.temperature, temperature);
            socioIndustrial = ObjectUtility.nonNull(other.socioIndustrial, socioIndustrial);
            icon = ObjectUtility.nonNull(other.icon, icon);
            // Merge (not replace!) events
            if (null != other.events) {
                for (PlanetaryEvent event : other.getEvents()) {
                    if ((null != event) && (null != event.date)) {
                        PlanetaryEvent myEvent = getOrCreateEvent(event.date);
                        myEvent.copyDataFrom(event);
                    }
                }
            }
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final Planet other = (Planet) object;
        return Objects.equals(id, other.id);
    }

    public static int convertRatingToCode(String rating) {
        if (rating.equalsIgnoreCase("A")) {
            return EquipmentType.RATING_A;
        } else if (rating.equalsIgnoreCase("B")) {
            return EquipmentType.RATING_B;
        } else if (rating.equalsIgnoreCase("C")) {
            return EquipmentType.RATING_C;
        } else if (rating.equalsIgnoreCase("D")) {
            return EquipmentType.RATING_D;
        } else if (rating.equalsIgnoreCase("E")) {
            return EquipmentType.RATING_E;
        } else if (rating.equalsIgnoreCase("F")) {
            return EquipmentType.RATING_F;
        } else {
            return EquipmentType.RATING_C;
        }
    }

    /** A class representing some event, possibly changing planetary information */
    @XmlRootElement(name="event")
    public static final class PlanetaryEvent {
        @XmlJavaTypeAdapter(DateAdapter.class)
        public LocalDate date;
        public String message;
        public String name;
        public String shortName;
        @XmlJavaTypeAdapter(StringListAdapter.class)
        public List<String> faction;
        @XmlTransient
        public Set<Faction> factions;
        @XmlJavaTypeAdapter(LifeFormAdapter.class)
        public LifeForm lifeForm;
        @XmlJavaTypeAdapter(ClimateAdapter.class)
        public Climate climate;
        @XmlElement(name = "water")
        public Integer percentWater;
        public Integer temperature;
        @XmlJavaTypeAdapter(SocioIndustrialDataAdapter.class)
        public SocioIndustrialData socioIndustrial;
        @XmlJavaTypeAdapter(HPGRatingAdapter.class)
        public Integer hpg;
        @XmlJavaTypeAdapter(PressureAdapter.class)
        private Integer pressure;
        @XmlJavaTypeAdapter(AtmosphereAdapter.class)
        private Atmosphere atmosphere;
        public String composition;
        public Long population;
        public Double dayLength;
        // Events marked as "custom" are saved to scenario files and loaded from there
        public transient boolean custom = false;

        public void copyDataFrom(PlanetaryEvent other) {
            climate = ObjectUtility.nonNull(other.climate, climate);
            faction = ObjectUtility.nonNull(other.faction, faction);
            factions = updateFactions(factions, faction, other.faction);
            hpg = ObjectUtility.nonNull(other.hpg, hpg);
            lifeForm = ObjectUtility.nonNull(other.lifeForm, lifeForm);
            message = ObjectUtility.nonNull(other.message, message);
            name = ObjectUtility.nonNull(other.name, name);
            percentWater = ObjectUtility.nonNull(other.percentWater, percentWater);
            shortName = ObjectUtility.nonNull(other.shortName, shortName);
            socioIndustrial = ObjectUtility.nonNull(other.socioIndustrial, socioIndustrial);
            temperature = ObjectUtility.nonNull(other.temperature, temperature);
            pressure = ObjectUtility.nonNull(other.pressure, pressure);
            atmosphere = ObjectUtility.nonNull(other.atmosphere, atmosphere);
            composition = ObjectUtility.nonNull(other.composition, composition);
            population = ObjectUtility.nonNull(other.population, population);
            dayLength = ObjectUtility.nonNull(other.dayLength, dayLength);
            custom = (other.custom || custom);
        }

        private Set<Faction> updateFactions(Set<Faction> current, List<String> codes, List<String> otherCodes) {
            // CAW: reference equality intended
            if (codes != otherCodes) {
                current = new HashSet<>(codes.size());
                for (String code : codes) {
                    current.add(Factions.getInstance().getFaction(code));
                }
            }
            return current;
        }

        public void replaceDataFrom(PlanetaryEvent other) {
            climate = other.climate;
            faction = other.faction;
            hpg = other.hpg;
            lifeForm = other.lifeForm;
            message = other.message;
            name = other.name;
            percentWater = other.percentWater;
            shortName = other.shortName;
            socioIndustrial = other.socioIndustrial;
            temperature = other.temperature;
            pressure = other.pressure;
            atmosphere = other.atmosphere;
            composition = other.composition;
            population = other.population;
            dayLength = other.dayLength;
            custom = (other.custom || custom);
        }

        /** @return <code>true</code> if the event doesn't contain any change */
        public boolean isEmpty() {
            return (null == climate) && (null == faction) && (null == hpg) && (null == lifeForm)
                && (null == message) && (null == name) && (null == shortName) && (null == socioIndustrial)
                && (null == temperature) && (null == pressure) && (null == atmosphere)
                && (null == composition) && (null == population) && (null == dayLength);
        }
    }

    public static final class FactionChange {
        @XmlJavaTypeAdapter(DateAdapter.class)
        public LocalDate date;
        @XmlJavaTypeAdapter(StringListAdapter.class)
        public List<String> faction;

        @Override
        public String toString() {
            return "{" + "date=" + date + "," + "faction=" + faction + "}";
        }
    }

    // @FunctionalInterface in Java 8, or just use Function<PlanetaryEvent, T>
    private interface EventGetter<T> {
        T get(PlanetaryEvent e);
    }

    /** BT planet types */
    public enum PlanetaryType {
        SMALL_ASTEROID, MEDIUM_ASTEROID, DWARF_TERRESTRIAL, TERRESTRIAL, GIANT_TERRESTRIAL, GAS_GIANT, ICE_GIANT
    }
}
