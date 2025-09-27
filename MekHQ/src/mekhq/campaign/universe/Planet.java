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
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechRating;
import megamek.common.rolls.TargetRoll;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.enums.PlanetaryType;

/**
 * This is the start of a planet object that will keep lots of information about planets that can be displayed on the
 * interstellar map.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = Planet.PlanetPostLoader.class)
public class Planet {
    private static final MMLogger logger = MMLogger.create(Planet.class);

    // Base data
    private String id;
    @JsonProperty("name")
    private SourceableValue<String> name;
    private String shortName;
    @JsonProperty("sysPos")
    private SourceableValue<Integer> sysPos;

    // Orbital information
    /** orbital radius (average distance to parent star), in AU */
    @JsonProperty("orbitalDist")
    private Double orbitRadius;

    // Stellar neighbourhood
    // for reading in because lists are easier
    @JsonProperty("satellite")
    private List<Satellite> satellites;
    @JsonProperty("smallMoons")
    private SourceableValue<Integer> smallMoons;
    @JsonProperty("ring")
    private SourceableValue<Boolean> ring;

    // Global physical characteristics
    @JsonProperty("type")
    private SourceableValue<PlanetaryType> planetType;
    /** diameter in km */
    @JsonProperty("diameter")
    private SourceableValue<Double> diameter;
    /** Density in g/m^3 */
    @JsonProperty("density")
    private SourceableValue<Double> density;
    @JsonProperty("gravity")
    private SourceableValue<Double> gravity;
    @JsonProperty("dayLength")
    private SourceableValue<Double> dayLength;
    @JsonProperty("yearLength")
    private SourceableValue<Double> yearLength;

    // Surface description
    @JsonProperty("water")
    private SourceableValue<Integer> percentWater;

    @JsonProperty("landmass")
    private List<LandMass> landMasses;

    // Atmospheric description
    /** Pressure classification - we use the MegaMek enum here directly */
    @JsonProperty("pressure")
    private SourceableValue<megamek.common.planetaryConditions.Atmosphere> pressure;
    @JsonProperty("atmosphere")
    private SourceableValue<Atmosphere> atmosphere;
    @JsonProperty("composition")
    private SourceableValue<String> composition;
    @JsonProperty("temperature")
    private SourceableValue<Integer> temperature;

    // Eco sphere
    @JsonProperty("lifeForm")
    private SourceableValue<LifeForm> lifeForm;

    // private List<String> garrisonUnits;

    // the system that this planet belongs to
    private PlanetarySystem parentSystem;

    // Fluff
    @JsonProperty("desc")
    private String desc;
    @JsonProperty("icon")
    private String icon;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     */
    private TreeMap<LocalDate, PlanetaryEvent> events;

    /**
     * This is a cache of the current event data based on the latest date given.
     */
    CurrentEvents currentEvents;

    // For export and import only (lists are easier than maps) */
    @JsonProperty("event")
    private List<Planet.PlanetaryEvent> eventList;

    public Planet() {
    }

    public Planet(String id) {
        this.id = id;
    }

    // Constant base data

    public String getId() {
        return id;
    }

    public Double getGravity() {
        return (null == getSourcedGravity()) ? null : getSourcedGravity().getValue();
    }

    public SourceableValue<Double> getSourcedGravity() {
        return gravity;
    }

    /**
     * @deprecated no indicated uses
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public SourceableValue<Double> getSourcedDensity() {
        return density;
    }

    public double getDiameter() {
        return (null == getSourcedDiameter() ? 0.0 : getSourcedDiameter().getValue());
    }

    public SourceableValue<Double> getSourcedDiameter() {
        return diameter;
    }

    public Double getOrbitRadius() {
        return orbitRadius;
    }

    public void setParentSystem(PlanetarySystem system) {
        parentSystem = system;
    }

    public List<Satellite> getSatellites() {
        return null != satellites ? new ArrayList<>(satellites) : null;
    }

    public Integer getSmallMoons() {
        return (null == getSourcedSmallMoons()) ? 0 : getSourcedSmallMoons().getValue();
    }

    public SourceableValue<Integer> getSourcedSmallMoons() {
        return smallMoons;
    }

    public Boolean hasRing() {
        return null != getSourcedRing() && getSourcedRing().getValue();
    }

    public SourceableValue<Boolean> getSourcedRing() {
        return ring;
    }

    public List<LandMass> getLandMasses() {
        return null != landMasses ? new ArrayList<>(landMasses) : null;
    }

    public SourceableValue<Double> getSourcedDayLength(LocalDate when) {
        // yes day length can change because Venus
        return getEventData(when, dayLength, e -> e.dayLength);
    }

    public SourceableValue<Double> getSourcedYearLength() {
        return yearLength;
    }

    public PlanetaryType getPlanetType() {
        if (null == getSourcedPlanetType()) {
            logger.error("No planetary type for {}. Using Terrestrial", id);
            return PlanetaryType.TERRESTRIAL;
        }
        return getSourcedPlanetType().getValue();
    }

    public SourceableValue<PlanetaryType> getSourcedPlanetType() {
        return planetType;
    }

    public Integer getSystemPosition() {
        return (null == sysPos) ? null : getSourcedSystemPosition().getValue();
    }

    public SourceableValue<Integer> getSourcedSystemPosition() {
        return sysPos;
    }

    /**
     * This function returns a system position for the planet that does not account for asteroid belts. Therefore, this
     * result may be different from that actual sysPos variable.
     *
     * @return String of system position after removing asteroid belts
     */
    public String getDisplayableSystemPosition() {
        // We won't give the actual system position here, because we don't want asteroid
        // belts to count for system position
        if ((null == getParentSystem()) || (null == getSystemPosition())) {
            return "?";
        }
        int pos = 0;
        for (int i = 1; i <= getSystemPosition(); i++) {
            if (getParentSystem().getPlanet(i).getPlanetType() == PlanetaryType.ASTEROID_BELT) {
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
        return null == getParentSystem() ? 0.0 : getParentSystem().getX();
    }

    public Double getY() {
        return null == getParentSystem() ? 0.0 : getParentSystem().getY();
    }

    public PlanetarySystem getParentSystem() {
        return parentSystem;
    }

    // Date-dependant data

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
     * This class tracks the current {@link PlanetaryEvent}.
     */
    class CurrentEvents {
        private LocalDate lastUpdated;
        private PlanetaryEvent planetaryEvent;
        private Map.Entry<LocalDate, PlanetaryEvent> nextEvent;
        private Iterator<Map.Entry<LocalDate, PlanetaryEvent>> eventStream;

        private void initialize(LocalDate now) {
            planetaryEvent = new PlanetaryEvent();
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
         *
         * @param now The current time.
         *
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

    public String getName(LocalDate when) {
        return getSourcedName(when).getValue();
    }

    public SourceableValue<String> getSourcedName(LocalDate when) {
        return getEventData(when, name, e -> e.name);
    }

    public String getShortName(LocalDate when) {
        return getEventData(when, shortName, e -> e.shortName);
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
        return (null == getSourcedSocioIndustrial(when)) ? null : getSourcedSocioIndustrial(when).getValue();
    }

    public SourceableValue<SocioIndustrialData> getSourcedSocioIndustrial(LocalDate when) {
        return getEventData(when, null, e -> e.socioIndustrial);
    }

    public HPGRating getHPG(LocalDate when) {
        return (null == getSourcedHPG(when)) ? HPGRating.X : getSourcedHPG(when).getValue();
    }

    public SourceableValue<HPGRating> getSourcedHPG(LocalDate when) {
        return getEventData(when, null, e -> e.hpg);
    }

    public Long getPopulation(LocalDate when) {
        return (null == getSourcedPopulation(when)) ? null : getSourcedPopulation(when).getValue();
    }

    public SourceableValue<Long> getSourcedPopulation(LocalDate when) {
        return getEventData(when, null, e -> e.population);
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public LifeForm getLifeForm(LocalDate when) {
        return (null == getSourcedLifeForm(when)) ? LifeForm.NONE : getSourcedLifeForm(when).getValue();
    }

    public SourceableValue<LifeForm> getSourcedLifeForm(LocalDate when) {
        return getEventData(when, lifeForm, e -> e.lifeForm);
    }

    public SourceableValue<Integer> getSourcedPercentWater(LocalDate when) {
        return getEventData(when, percentWater, e -> e.percentWater);
    }

    public Integer getTemperature(LocalDate when) {
        return (null == getSourcedTemperature(when)) ? null : getSourcedTemperature(when).getValue();
    }

    public SourceableValue<Integer> getSourcedTemperature(LocalDate when) {
        return getEventData(when, temperature, e -> e.temperature);
    }

    public megamek.common.planetaryConditions.Atmosphere getPressure(LocalDate when) {
        return (null == getSourcedPressure(when)) ? null : getSourcedPressure(when).getValue();
    }

    public SourceableValue<megamek.common.planetaryConditions.Atmosphere> getSourcedPressure(LocalDate when) {
        return getEventData(when, pressure, e -> e.pressure);
    }

    public Atmosphere getAtmosphere(LocalDate when) {
        return (null == getSourcedAtmosphere(when)) ? Atmosphere.NONE : getSourcedAtmosphere(when).getValue();
    }

    public SourceableValue<Atmosphere> getSourcedAtmosphere(LocalDate when) {
        return getEventData(when, atmosphere, e -> e.atmosphere);
    }

    public SourceableValue<String> getSourcedComposition(LocalDate when) {
        return getEventData(when, composition, e -> e.composition);
    }

    public List<String> getFactions(LocalDate when) {
        return (null == getSourcedFactions(when)) ? Collections.emptyList() : getSourcedFactions(when).getValue();
    }

    public SourceableValue<List<String>> getSourcedFactions(LocalDate when) {
        return getEventData(when, null, e -> e.faction);
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

    public String getFactionDesc(LocalDate when) {
        String toReturn = Faction.getFactionNames(getFactionSet(when), when.getYear());
        if (toReturn.isEmpty()) {
            toReturn = "Uncolonized";
        }
        return toReturn;
    }

    /**
     * Retrieves the level of the Hiring Hall on the planet on the specified date. The level is dynamically determined
     * on various planetary characteristics, including Technological Sophistication, HPG level, and planetary
     * governments.
     *
     * @param when Date to check for the level of the hiring hall
     *
     * @return The hiring hall level on the given date
     */
    public HiringHallLevel getHiringHallLevel(LocalDate when) {
        HiringHallLevel staticHall = (null == getSourcedHiringHallLevel(when)) ?
                                           HiringHallLevel.NONE :
                                           getSourcedHiringHallLevel(when).getValue();
        if (!staticHall.isNone()) {
            return staticHall;
        }

        // this is stupid - the hiring hall should still be none, but just add other
        // mods to the contract roll for these things
        if (getPopulation(when) == null || getPopulation(when) == 0) {
            return HiringHallLevel.NONE;
        }
        for (Faction faction : getFactionSet(when)) {
            if (faction.isPirate() || faction.isChaos()) {
                return HiringHallLevel.QUESTIONABLE;
            }
            if (faction.isClan() || faction.isInactive()) {
                return HiringHallLevel.NONE;
            }
        }
        int score = calculateHiringHallScore(when);
        return resolveHiringHallLevel(score);
    }

    public SourceableValue<HiringHallLevel> getSourcedHiringHallLevel(LocalDate when) {
        return getEventData(when, null, e -> e.hiringHall);
    }

    private int calculateHiringHallScore(LocalDate when) {
        int score = 0;
        score += getHiringHallHpgBonus(when);
        score += getHiringHallTechBonus(when);
        return score;
    }

    private HiringHallLevel resolveHiringHallLevel(int score) {
        if (score > 9) {
            return HiringHallLevel.GREAT;
        } else if (score > 6) {
            return HiringHallLevel.STANDARD;
        } else if (score > 4) {
            return HiringHallLevel.MINOR;
        }
        return HiringHallLevel.NONE;
    }

    private int getHiringHallHpgBonus(LocalDate when) {
        if (null == getHPG(when)) {
            return 0;
        }
        return switch (getHPG(when)) {
            case A -> 5;
            case B -> 3;
            case C, D -> 1;
            default -> 0;
        };
    }

    private int getHiringHallTechBonus(LocalDate when) {
        if (null == getSocioIndustrial(when)) {
            return 0;
        }
        return switch (getSocioIndustrial(when).tech) {
            case ADVANCED -> 5; // Ultra-Advanced; not accounted for in the EquipmentType.RATING constants
            case A, B -> 3;
            case C, D -> 1;
            default -> 0;
        };
    }

    /**
     * Returns the equipment technology rating of the planet based on its sophistication.
     */
    public @Nullable TechRating getTechRating(LocalDate when) {
        return getSocioIndustrial(when).getEquipmentTechRating();
    }

    // Astronavigation

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in Terran days
     */
    public double getTimeToJumpPoint(double acceleration) {
        // based on the formula in StratOps
        return Math.sqrt((getDistanceToJumpPoint() * 1000) / (StarUtil.G * acceleration)) / 43200;
    }

    /** @return the average distance to the system's jump point in km */
    public double getDistanceToJumpPoint() {
        if (null == parentSystem) {
            logger.error("reference to planet with no parent system");
            return 0;
        }
        return Math.sqrt(Math.pow(getOrbitRadiusKm(), 2) + Math.pow(parentSystem.getStarDistanceToJumpPoint(), 2));
    }

    public double getOrbitRadiusKm() {
        if (null == orbitRadius) {
            // TODO: figure out a better way to handle missing orbit radius (really this
            // should not be missing)
            return 0.5 * StarUtil.AU;
        }
        return orbitRadius * StarUtil.AU;
    }

    /**
     * Returns whether the planet has not been discovered or is a dead planet. This code was adapted from
     * InterstellarPlanetMapPanel.isPlanetEmpty
     *
     * @param when - the <code>LocalDate</code> object indicating what time we are asking about.
     *
     * @return true if the planet is empty; false if the planet is not empty
     */
    public boolean isEmpty(LocalDate when) {
        Set<Faction> factions = getFactionSet(when);
        if ((null == factions) || factions.isEmpty()) {
            return true;
        }

        for (Faction faction : factions) {
            if (!faction.is(FactionTag.ABANDONED)) {
                return false;
            }
        }

        return true;
    }

    /**
     * A function to return any planetary related modifiers to a target roll for acquiring parts. Feeds in the campaign
     * options because this will include important information about these mods as well as faction information.
     *
     * @param target  - current TargetRoll for acquisitions
     * @param when    - a LocalDate object for the campaign to retrieve information from the planet
     * @param options - the campaign options from which important values need to be determined
     *
     * @return an updated TargetRoll with planet specific mods
     */
    public TargetRoll getAcquisitionMods(TargetRoll target, LocalDate when, CampaignOptions options, Faction faction,
          boolean clanPart) {
        // check faction limitations
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
                } else if (RandomFactionGenerator.getInstance()
                                 .getFactionHints()
                                 .isAlliedWith(faction, planetFaction, when)) {
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
                if (enemies &&
                          !neutrals &&
                          !allies &&
                          !options.getPlanetAcquisitionFactionLimit().generateOnEnemyPlanets()) {
                    return new TargetRoll(TargetRoll.IMPOSSIBLE, "No supplies from enemy planets");
                } else if (neutrals &&
                                 !allies &&
                                 !options.getPlanetAcquisitionFactionLimit().generateOnNeutralPlanets()) {
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
            // nothing has been coded for this planet, so we will use the default values (CampaignOps)
            socioIndustrial = new SocioIndustrialData();
        }

        // don't allow acquisitions from caveman planets
        if ((socioIndustrial.tech == PlanetarySophistication.REGRESSED) ||
                  (socioIndustrial.industry == PlanetaryRating.F) ||
                  (socioIndustrial.output == PlanetaryRating.F)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Regressed: Pre-industrial world");
        }

        target.addModifier(options.getPlanetTechAcquisitionBonus(socioIndustrial.tech),
              "planet tech: " + socioIndustrial.tech.getName());
        target.addModifier(options.getPlanetIndustryAcquisitionBonus(socioIndustrial.industry),
              "planet industry: " + socioIndustrial.industry.getName());
        target.addModifier(options.getPlanetOutputAcquisitionBonus(socioIndustrial.output),
              "planet output: " + socioIndustrial.output.getName());

        return target;

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

    /** A class representing some event, possibly changing planetary information */
    public static final class PlanetaryEvent {

        @JsonProperty("date")
        public LocalDate date;
        @JsonProperty("message")
        public String message;
        @JsonProperty("name")
        public SourceableValue<String> name;
        @JsonProperty("shortName")
        public String shortName;

        @JsonProperty("faction")
        public SourceableValue<List<String>> faction;
        public Set<Faction> factions;
        @JsonProperty("lifeForm")
        public SourceableValue<LifeForm> lifeForm;
        @JsonProperty("water")
        public SourceableValue<Integer> percentWater;
        @JsonProperty("temperature")
        public SourceableValue<Integer> temperature;
        public SourceableValue<SocioIndustrialData> socioIndustrial;
        @JsonProperty("hpg")
        public SourceableValue<HPGRating> hpg;
        @JsonProperty("pressure")
        private SourceableValue<megamek.common.planetaryConditions.Atmosphere> pressure;
        @JsonProperty("hiringHall")
        private SourceableValue<HiringHallLevel> hiringHall;
        @JsonProperty("atmosphere")
        private SourceableValue<Atmosphere> atmosphere;
        @JsonProperty("composition")
        public SourceableValue<String> composition;
        @JsonProperty("population")
        public SourceableValue<Long> population;
        @JsonProperty("dayLength")
        public SourceableValue<Double> dayLength;
        // Events marked as "custom" are saved to scenario files and loaded from there
        @JsonProperty("custom")
        public boolean custom = false;

        public void copyDataFrom(PlanetaryEvent other) {
            faction = ObjectUtility.nonNull(other.faction, faction);
            if (null != other.faction) {
                factions = updateFactions(factions, faction.getValue(), other.faction.getValue());
            }
            hpg = ObjectUtility.nonNull(other.hpg, hpg);
            lifeForm = ObjectUtility.nonNull(other.lifeForm, lifeForm);
            message = ObjectUtility.nonNull(other.message, message);
            name = ObjectUtility.nonNull(other.name, name);
            percentWater = ObjectUtility.nonNull(other.percentWater, percentWater);
            shortName = ObjectUtility.nonNull(other.shortName, shortName);
            socioIndustrial = ObjectUtility.nonNull(other.socioIndustrial, socioIndustrial);
            hiringHall = ObjectUtility.nonNull(other.hiringHall, hiringHall);
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

        /** @return <code>true</code> if the event doesn't contain any change */
        public boolean isEmpty() {
            return (null == faction) &&
                         (null == hpg) &&
                         (null == lifeForm) &&
                         (null == message) &&
                         (null == name) &&
                         (null == shortName) &&
                         (null == socioIndustrial) &&
                         (null == temperature) &&
                         (null == pressure) &&
                         (null == atmosphere) &&
                         (null == composition) &&
                         (null == population) &&
                         (null == dayLength) &&
                         (null == hiringHall);
        }
    }

    // @FunctionalInterface in Java 8, or just use Function<PlanetaryEvent, T>
    protected interface EventGetter<T> {
        T get(PlanetaryEvent e);
    }

    /**
     * This class is used to do some additional work after a planet file is loaded with Jackson
     **/
    public static class PlanetPostLoader extends StdConverter<Planet, Planet> {

        @Override
        public Planet convert(Planet planet) {
            if (null == planet.id) {
                planet.id = planet.name.getValue();
            }

            // Fill up events
            planet.events = new TreeMap<>();
            if (null != planet.eventList) {
                for (PlanetaryEvent event : planet.eventList) {
                    if ((null != event) && (null != event.date)) {
                        planet.events.put(event.date, event);
                    }
                }
                planet.eventList.clear();
            }
            planet.eventList = null;

            return planet;
        }
    }

}
