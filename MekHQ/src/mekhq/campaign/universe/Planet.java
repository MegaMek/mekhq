/*
 * Planet.java
 *
 * Copyright (C) 2011-2016 MegaMek team
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;
import mekhq.Utilities;
import mekhq.adapter.BooleanValueAdapter;
import mekhq.adapter.ClimateAdapter;
import mekhq.adapter.DateAdapter;
import mekhq.adapter.HPGRatingAdapter;
import mekhq.adapter.LifeFormAdapter;
import mekhq.adapter.SocioIndustrialDataAdapter;
import mekhq.adapter.SpectralClassAdapter;
import mekhq.adapter.StringListAdapter;


/**
 * This is the start of a planet object that will keep lots of information about
 * planets that can be displayed on the interstellar map.
 *
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
@XmlRootElement(name="planet")
@XmlAccessorType(XmlAccessType.FIELD)
public class Planet implements Serializable {
    private static final long serialVersionUID = -8699502165157515100L;

    // Star classification data and methods
    
    public static final int SPECTRAL_O = 0;
    public static final int SPECTRAL_B = 1;
    public static final int SPECTRAL_A = 2;
    public static final int SPECTRAL_F = 3;
    public static final int SPECTRAL_G = 4;
    public static final int SPECTRAL_K = 5;
    public static final int SPECTRAL_M = 6;
    public static final int SPECTRAL_L = 7;
    public static final int SPECTRAL_T = 8;
    public static final int SPECTRAL_Y = 9;
    // Spectral class "D" (white dwarfs) are determined by their luminosity "VII" - the number is here for sorting
    public static final int SPECTRAL_D = 99;
    // "Q" - not a proper star (neutron stars QN, pulsars QP, black holes QB, ...)
    public static final int SPECTRAL_Q = 100;
    // TODO: Wolf-Rayet stars ("W"), carbon stars ("C"), S-type stars ("S"), 
    
    public static final String LUM_0           = "0"; //$NON-NLS-1$
    public static final String LUM_IA          = "Ia"; //$NON-NLS-1$
    public static final String LUM_IAB         = "Iab"; //$NON-NLS-1$
    public static final String LUM_IB          = "Ib"; //$NON-NLS-1$
    // Generic class, consisting of Ia, Iab and Ib
    public static final String LUM_I           = "I"; //$NON-NLS-1$
    public static final String LUM_II_EVOLVED  = "I/II"; //$NON-NLS-1$
    public static final String LUM_II          = "II"; //$NON-NLS-1$
    public static final String LUM_III_EVOLVED = "II/III"; //$NON-NLS-1$
    public static final String LUM_III         = "III"; //$NON-NLS-1$
    public static final String LUM_IV_EVOLVED  = "III/IV"; //$NON-NLS-1$
    public static final String LUM_IV          = "IV"; //$NON-NLS-1$
    public static final String LUM_V_EVOLVED   = "IV/V"; //$NON-NLS-1$
    public static final String LUM_V           = "V"; //$NON-NLS-1$
    // typically used as a prefix "sd", not as a suffix
    public static final String LUM_VI          = "VI";  //$NON-NLS-1$
    // typically used as a prefix "esd", not as a suffix
    public static final String LUM_VI_PLUS     = "VI+"; //$NON-NLS-1$
    // always used as class designation "D", never as a suffix
    public static final String LUM_VII         = "VII"; //$NON-NLS-1$
    
    @XmlElement(name = "xcood")
    private Double x;
    @XmlElement(name = "ycood")
    private Double y;

    // Base data
    private String id;
    private String name;
    private String shortName;
    private Integer sysPos;

    //Star data (to be factored out)
    private String spectralType;
    @XmlJavaTypeAdapter(SpectralClassAdapter.class)
    private Integer spectralClass;
    private Double subtype;
    private String luminosity;

    // Orbital information
    /** Semimajor axis (average distance to parent star), in AU */
    @XmlElement(name = "orbitRadius")
    private Double orbitSemimajorAxis;
    private Double orbitEccentricity;
    /** Degrees to the system's invariable plane */
    private Double orbitInclination;
    
    // Stellar neighbourhood
    @XmlElement(name="satellites")
    private Integer numSatellites;
    @XmlElement(name="satellite")
    private List<String> satellites;
    
    // Global physical characteristics
    /** Mass in Earth masses */
    private Double mass;
    /** Radius in Earth radii */
    private Double radius;
    /** Density in kg/m^3 */
    private Double density;
    private Double gravity;
    private Double dayLength;
    private Double tilt;
    @XmlElement(name = "class")
    private String className;
    
    // Surface description
    private Integer percentWater;
    @XmlElement(name = "volcamism")
    private Integer volcanicActivity;
    @XmlElement(name = "tectonics")
    private Integer tectonicActivity;
    @XmlElement(name="landMass")
    private List<String> landMasses;
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    private Boolean nadirCharge;
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    private Boolean zenithCharge;

    // Atmospheric description
    /** Pressure classification */
    private Integer pressure;
    /** Pressure in standard pressure (101325 Pa) */
    private Double pressureAtm;
    /** Atmospheric description */
    private String atmosphere;
    /** Atmospheric mass compared to Earth's 28.9645 kg/mol */
    private Double atmMass;
    private Double albedo;
    @XmlElement(name="greenhouse")
    private Double greenhouseEffect;
    /** Average surface temperature at equator in °C */
    private Integer temperature;
    @XmlJavaTypeAdapter(ClimateAdapter.class)
    private Climate climate;
    
    // Ecosphere
    @XmlJavaTypeAdapter(LifeFormAdapter.class)
    private LifeForm lifeForm;
    private Integer habitability;
    
    // Human influence
    /** Order of magnitude of the population - 1 */
    @XmlElement(name = "pop")
    private Integer populationRating;
    private String government;
    private Integer controlRating;
    @XmlJavaTypeAdapter(SocioIndustrialDataAdapter.class)
    private SocioIndustrialData socioIndustrial;
    @XmlJavaTypeAdapter(HPGRatingAdapter.class)
    private Integer hpg;
    @XmlElement(name = "faction")
    @XmlJavaTypeAdapter(StringListAdapter.class)
    private List<String> factions;
    
    //private List<String> garrisonUnits;

    // Fluff
    private String desc;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     */
    @XmlTransient
    TreeMap<DateTime, PlanetaryEvent> events;
    
    //a hash to keep track of dynamic garrison changes
    //TreeMap<DateTime, List<String>> garrisonHistory;

    /** @deprecated Use "event", which can have any number of changes to the planetary data */
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
    
    public Double getMass() {
        return mass;
    }
    
    public Double getDensity() {
        return density;
    }
    
    public Double getRadius() {
        return radius;
    }
    
    public String getGravityText() {
        return null != gravity ? gravity.toString() + "g" : "unknown"; //$NON-NLS-1$
    }

    public Double getOrbitSemimajorAxis() {
        return orbitSemimajorAxis;
    }
    
    /** @return orbital semimajor axis in km; in the middle of the star's life zone if not set */
    public double getOrbitSemimajorAxisKm() {
        return null != orbitSemimajorAxis ? orbitSemimajorAxis * StarUtil.AU : getStarAverageLifeZone();
    }

    public List<String> getSatellites() {
        return null != satellites ? new ArrayList<String>(satellites) : null;
    }

    public String getSatelliteDescription() {
        if(null == satellites || satellites.isEmpty()) {
            return "0"; //$NON-NLS-1$
        }
        return satellites.size() + " (" + Utilities.combineString(satellites, ", ") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public List<String> getLandMasses() {
        return null != landMasses ? new ArrayList<String>(landMasses) : null;
    }

    public String getLandMassDescription() {
        return null != landMasses ? Utilities.combineString(landMasses, ", ") : ""; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    public Integer getVolcanicActivity() {
        return volcanicActivity;
    }

    public Integer getTectonicActivity() {
        return tectonicActivity;
    }

    public Double getDayLength() {
        return dayLength;
    }
    
    public Integer getSystemPosition() {
        return sysPos;
    }
    
    public String getSystemPositionText() {
        return null != sysPos ? sysPos.toString() : "?"; //$NON-NLS-1$
    }

    public Double getOrbitEccentricity() {
        return orbitEccentricity;
    }

    public Double getOrbitInclination() {
        return orbitInclination;
    }

    public Double getTilt() {
        return tilt;
    }
    
    public String getDescription() {
        return desc;
    }
    
    // Constant stellar data (to be moved out later)
    
    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public String getSpectralType() {
        return spectralType;
    }
    
    /** @return normalized spectral type, for display */
    public String getSpectralTypeNormalized() {
        return null != spectralType ? StarUtil.getSpectralType(spectralClass, subtype, luminosity) : "?"; //$NON-NLS-1$
    }
    
    public String getSpectralTypeText() {
        if(null == spectralType || spectralType.isEmpty()) {
            return "unknown";
        }
        if(spectralType.startsWith("Q")) {
            switch(spectralType) {
                case "QB": return "black hole"; //$NON-NLS-1$
                case "QN": return "neutron star"; //$NON-NLS-1$
                case "QP": return "pulsar"; //$NON-NLS-1$
                default: return "unknown";
            }
        }
        return spectralType;
    }

    public Integer getSpectralClass() {
        return spectralClass;
    }

    public void setSpectralClass(Integer spectralClass) {
        this.spectralClass = spectralClass;
    }

    public Double getSubtype() {
        return subtype;
    }

    public void setSubtype(double subtype) {
        this.subtype = subtype;
    }

    // Date-dependant data
    
    @SuppressWarnings("unchecked")
    private PlanetaryEvent getOrCreateEvent(DateTime when) {
        if(null == when) {
            return null;
        }
        if(null == events) {
            events = new TreeMap<DateTime, PlanetaryEvent>(DateTimeComparator.getDateOnlyInstance());
        }
        PlanetaryEvent event = events.get(when);
        if(null == event) {
            event = new PlanetaryEvent();
            event.date = when;
            events.put(when, event);
        }
        return event;
    }
    
    public PlanetaryEvent getEvent(DateTime when) {
        if((null == when) || (null == events)) {
            return null;
        }
        return events.get(when);
    }
    
    public List<PlanetaryEvent> getEvents() {
        if( null == events ) {
            return null;
        }
        return new ArrayList<PlanetaryEvent>(events.values());
    }
    
    protected <T> T getEventData(DateTime when, T defaultValue, EventGetter<T> getter) {
        if( null == when || null == events || null == getter ) {
            return defaultValue;
        }
        T result = defaultValue;
        for( DateTime date : events.navigableKeySet() ) {
            if( date.isAfter(when) ) {
                break;
            }
            result = Utilities.nonNull(getter.get(events.get(date)), result);
        }
        return result;
    }
    
    /** @return events for this year. Never returns <i>null</i>. */
    public List<PlanetaryEvent> getEvents(int year) {
        if( null == events ) {
            return Collections.<PlanetaryEvent>emptyList();
        }
        List<PlanetaryEvent> result = new ArrayList<PlanetaryEvent>();
        for( DateTime date : events.navigableKeySet() ) {
            if( date.getYear() > year ) {
                break;
            }
            if( date.getYear() == year ) {
                result.add(events.get(date));
            }
        }
        return result;
    }
    
    public String getName(DateTime when) {
        return getEventData(when, name, new EventGetter<String>() {
            @Override public String get(PlanetaryEvent e) { return e.name; }
        });
    }

    public String getShortName(DateTime when) {
        return getEventData(when, shortName, new EventGetter<String>() {
            @Override public String get(PlanetaryEvent e) { return e.shortName; }
        });
    }

    /** @return short name if set, else full name, else "unnamed" */
    public String getPrintableName(DateTime when) {
        String result = getShortName(when);
        if( null == result ) {
            result = getName(when);
        }
        return null != result ? result : "unnamed"; //$NON-NLS-1$
    }
    
    public SocioIndustrialData getSocioIndustrial(DateTime when) {
        return getEventData(when, socioIndustrial, new EventGetter<SocioIndustrialData>() {
            @Override public SocioIndustrialData get(PlanetaryEvent e) { return e.socioIndustrial; }
        });
    }

    public String getSocioIndustrialText(DateTime when) {
        SocioIndustrialData sid = getSocioIndustrial(when);
        return null != sid ? sid.toString() : ""; //$NON-NLS-1$
    }

    public Integer getHPG(DateTime when) {
        return getEventData(when, hpg, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.hpg; }
        });
    }

    public String getHPGClass(DateTime when) {
        Integer currentHPG = getHPG(when);
        return null != currentHPG ? EquipmentType.getRatingName(currentHPG) : ""; //$NON-NLS-1$
    }

    public Integer getPopulationRating(DateTime when) {
        return getEventData(when, populationRating, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.populationRating; }
        });
    }
    
    public String getPopulationRatingString(DateTime when) {
        Integer pops = getPopulationRating(when);
        return (null != pops) ? StarUtil.getPopulationRatingString(pops.intValue()) : "unknown";
    }
    
    public String getGovernment(DateTime when) {
        return getEventData(when, government, new EventGetter<String>() {
            @Override public String get(PlanetaryEvent e) { return e.government; }
        });
    }

    public Integer getControlRating(DateTime when) {
        return getEventData(when, controlRating, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.controlRating; }
        });
    }
    
    public String getControlRatingString(DateTime when) {
        Integer cr = getControlRating(when);
        return (null != cr) ? StarUtil.getControlRatingString(cr.intValue()) : "actual situation unclear";
    }
    
    public LifeForm getLifeForm(DateTime when) {
        return getEventData(when, null != lifeForm ? lifeForm : LifeForm.NONE, new EventGetter<LifeForm>() {
            @Override public LifeForm get(PlanetaryEvent e) { return e.lifeForm; }
        });
    }

    public String getLifeFormName(DateTime when) {
        return getLifeForm(when).name;
    }

    public Climate getClimate(DateTime when) {
        return getEventData(when, climate, new EventGetter<Climate>() {
            @Override public Climate get(PlanetaryEvent e) { return e.climate; }
        });
    }

    public String getClimateName(DateTime when) {
        Climate c = getClimate(when);
        return null != c ? c.climateName : null;
    }

    public Integer getPercentWater(DateTime when) {
        return getEventData(when, percentWater, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.percentWater; }
        });
    }

    public Integer getTemperature(DateTime when) {
        return getEventData(when, temperature, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.temperature; }
        });
    }
    
    public Integer getPressure(DateTime when) {
        return getEventData(when, pressure, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.pressure; }
        });
    }
    
    public String getPressureName(DateTime when) {
        Integer currentPressure = getPressure(when);
        return null != currentPressure ? PlanetaryConditions.getAtmosphereDisplayableName(currentPressure) : "unknown";
    }

    public Double getPressureAtm(DateTime when) {
        return getEventData(when, pressureAtm, new EventGetter<Double>() {
            @Override public Double get(PlanetaryEvent e) { return e.pressureAtm; }
        });
    }

    public Double getAtmMass(DateTime when) {
        return getEventData(when, atmMass, new EventGetter<Double>() {
            @Override public Double get(PlanetaryEvent e) { return e.atmMass; }
        });
    }

    public String getAtmosphere(DateTime when) {
        return getEventData(when, atmosphere, new EventGetter<String>() {
            @Override public String get(PlanetaryEvent e) { return e.atmosphere; }
        });
    }

    public Double getAlbedo(DateTime when) {
        return getEventData(when, albedo, new EventGetter<Double>() {
            @Override public Double get(PlanetaryEvent e) { return e.albedo; }
        });
    }

    public Double getGreenhouseEffect(DateTime when) {
        return getEventData(when, greenhouseEffect, new EventGetter<Double>() {
            @Override public Double get(PlanetaryEvent e) { return e.greenhouseEffect; }
        });
    }

    public Integer getHabitability(DateTime when) {
        return getEventData(when, habitability, new EventGetter<Integer>() {
            @Override public Integer get(PlanetaryEvent e) { return e.habitability; }
        });
    }

    public List<String> getFactions(DateTime when) {
        return getEventData(when, factions, new EventGetter<List<String>>() {
            @Override public List<String> get(PlanetaryEvent e) { return e.faction; }
        });
    }

    private static Set<Faction> getFactionsFrom(Collection<String> codes) {
        Set<Faction> factions = new HashSet<Faction>(codes.size());
        for(String code : codes) {
            factions.add(Faction.getFaction(code));
        }
        return factions;
    }

    /** @return set of factions at a given date */
    public Set<Faction> getFactionSet(DateTime when) {
        List<String> currentFactions = getFactions(when);
        return null != currentFactions ? getFactionsFrom(currentFactions) : null;
    }

    public String getShortDesc(DateTime when) {
        return getShortName(when) + " (" + getFactionDesc(when) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public String getFactionDesc(DateTime when) {
        int era = Era.getEra(when.getYear() + 1900);
        Set<Faction> factions = getFactionSet(when);
        if( null == factions ) {
            return "-"; //$NON-NLS-1$
        }
        List<String> factionNames = new ArrayList<String>(factions.size());
        for( Faction f : factions ) {
            factionNames.add(f.getFullName(era));
        }
        Collections.sort(factionNames);
        return Utilities.combineString(factionNames, "/"); //$NON-NLS-1$
    }

    // Stellar event data, to be moved
    
    public Boolean isNadirCharge(DateTime when) {
        return getEventData(when, nadirCharge, new EventGetter<Boolean>() {
            @Override public Boolean get(PlanetaryEvent e) { return e.nadirCharge; }
        });
    }

    public boolean isZenithCharge(DateTime when) {
        return getEventData(when, zenithCharge, new EventGetter<Boolean>() {
            @Override public Boolean get(PlanetaryEvent e) { return e.zenithCharge; }
        });
    }

    public String getRechargeStationsText(DateTime when) {
        Boolean nadir = isNadirCharge(when);
        Boolean zenith = isZenithCharge(when);
        if(null != nadir && null != zenith && nadir.booleanValue() && zenith.booleanValue()) {
            return "Zenith, Nadir";
        } else if(null != zenith && zenith.booleanValue()) {
            return "Zenith";
        } else if(null != nadir && nadir.booleanValue()) {
            return "Nadir";
        } else {
            return "None";
        }
    }
    
    /** Recharge time in hours (assuming the usage of the fastest charing method available) */
    public double getRechargeTime(DateTime when) {
        if(isZenithCharge(when) || isNadirCharge(when)) {
            return Math.min(176.0, 141 + 10*spectralClass + subtype);
        } else {
            return getSolarRechargeTime();
        }
    }
    
    /** Recharge time in hours using solar radiation alone (at jump point and 100% efficiency) */
    public double getSolarRechargeTime() {
        if( null == spectralClass || null == subtype ) {
            return 183;
        }
        return StarUtil.getSolarRechargeTime(spectralClass, subtype);
    }

    public String getRechargeTimeText(DateTime when) {
        double time = getRechargeTime(when);
        if(Double.isInfinite(time)) {
            return "recharging impossible"; //$NON-NLS-1$
        } else {
            return String.format("%.0f hours", time); //$NON-NLS-1$
        }
    }
    
    // Astronavigation
    
    /** @return the average travel time from low orbit to the jump point at 1g, in Terran days */
    public double getTimeToJumpPoint(double acceleration) {
        //based on the formula in StratOps
        return Math.sqrt((getDistanceToJumpPoint() * 1000) / (StarUtil.G * acceleration)) / 43200;
    }

    /** @return the average distance to the system's jump point in km */
    public double getDistanceToJumpPoint() {
        return Math.sqrt(Math.pow(getOrbitSemimajorAxisKm(), 2) + Math.pow(getStarDistanceToJumpPoint(), 2));
    }

    private double getStarDistanceToJumpPoint() {
        if( null == spectralClass || null == subtype ) {
            return StarUtil.getDistanceToJumpPoint(42);
        }
        return StarUtil.getDistanceToJumpPoint(spectralClass, subtype);
    }
    
    /** @return the rough middle of the habitable zone around this star, in km */
    private double getStarAverageLifeZone() {
        // TODO Calculate from luminosity and the like. For now, using the table in IO Beta.
        if( null == spectralClass || null == subtype ) {
            return (StarUtil.getMinLifeZone(42) + StarUtil.getMaxLifeZone(42)) / 2;
        }
        return (StarUtil.getMinLifeZone(spectralClass, subtype) + StarUtil.getMaxLifeZone(spectralClass, subtype)) / 2;
    }
    
    /** @return the distance to another planet in light years (0 if both are in the same system) */
    public double getDistanceTo(Planet anotherPlanet) {
        return Math.sqrt(Math.pow(x - anotherPlanet.x, 2) + Math.pow(y - anotherPlanet.y, 2));
    }

    // JAXB marshalling support
    
    @SuppressWarnings({ "unused", "unchecked" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if( null == id ) {
            id = name;
        }
        
        // Spectral classification: use spectralType if available, else the separate values
        if( null != spectralType ) {
            setSpectralType(spectralType);
        } else {
            spectralType = StarUtil.getSpectralType(spectralClass, subtype, luminosity);
        }
        nadirCharge = Utilities.nonNull(nadirCharge, Boolean.FALSE);
        zenithCharge = Utilities.nonNull(zenithCharge, Boolean.FALSE);
        // Generate a bunch of data if we still don't have it
        if( null == spectralType ) {
            setSpectralType(StarUtil.generateSpectralType(
                new Random(id.hashCode() + 133773), true, (null != spectralClass) ? spectralClass.intValue() : -1));
        }        
        
        // Fill up events
        events = new TreeMap<DateTime, PlanetaryEvent>(DateTimeComparator.getDateOnlyInstance());
        if( null != eventList ) {
            for( PlanetaryEvent event : eventList ) {
                if( null != event && null != event.date ) {
                    events.put(event.date, event);
                }
            }
            eventList.clear();
        }
        eventList = null;
        // Merge faction change events into the event data
        if( null != factionChanges ) {
            for( FactionChange change : factionChanges ) {
                if( null != change && null != change.date ) {
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
        eventList = new ArrayList<PlanetaryEvent>(events.values());
        return true;
    }
    
    /** Includes a parser for spectral type strings */
    protected void setSpectralType(String type) {
        SpectralDefinition scDef = StarUtil.parseSpectralType(type);
        
        if( null == scDef ) {
            return;
        }
        
        spectralType = scDef.spectralType;
        spectralClass = scDef.spectralClass;
        subtype = scDef.subtype;
        luminosity = scDef.luminosity;
    }
    
    /**
     * Copy all but id from the other planet. Update event list. Events with the
     * same date as others already in the list get overwritten, others added.
     * To effectively delete an event, simply create a new one with <i>just</i> the date.
     */
    public void copyDataFrom(Planet other) {
        if( null != other ) {
            // We don't change the ID
            name = Utilities.nonNull(other.name, name);
            shortName = Utilities.nonNull(other.shortName, shortName);
            x = Utilities.nonNull(other.x, x);
            y = Utilities.nonNull(other.y, y);
            spectralType = Utilities.nonNull(other.spectralType, spectralType);
            spectralClass =Utilities.nonNull(other.spectralClass, spectralClass);
            subtype = Utilities.nonNull(other.subtype, subtype);
            luminosity = Utilities.nonNull(other.luminosity, luminosity);
            climate = Utilities.nonNull(other.climate, climate);
            desc = Utilities.nonNull(other.desc, desc);
            factions = Utilities.nonNull(other.factions, factions);
            gravity = Utilities.nonNull(other.gravity, gravity);
            hpg = Utilities.nonNull(other.hpg, hpg);
            landMasses = Utilities.nonNull(other.landMasses, landMasses);
            lifeForm = Utilities.nonNull(other.lifeForm, lifeForm);
            orbitSemimajorAxis = Utilities.nonNull(other.orbitSemimajorAxis, orbitSemimajorAxis);
            orbitEccentricity = Utilities.nonNull(other.orbitEccentricity, orbitEccentricity);
            orbitInclination = Utilities.nonNull(other.orbitInclination, orbitInclination);
            percentWater = Utilities.nonNull(other.percentWater, percentWater);
            pressure = Utilities.nonNull(other.pressure, pressure);
            pressureAtm = Utilities.nonNull(other.pressureAtm, pressureAtm);
            pressureAtm = Utilities.nonNull(other.pressureAtm, pressureAtm);
            atmMass = Utilities.nonNull(other.atmMass, atmMass);
            atmosphere = Utilities.nonNull(other.atmosphere, atmosphere);
            albedo = Utilities.nonNull(other.albedo, albedo);
            greenhouseEffect = Utilities.nonNull(other.greenhouseEffect, greenhouseEffect);
            volcanicActivity = Utilities.nonNull(other.volcanicActivity, volcanicActivity);
            tectonicActivity = Utilities.nonNull(other.tectonicActivity, tectonicActivity);
            populationRating = Utilities.nonNull(other.populationRating, populationRating);
            government = Utilities.nonNull(other.government, government);
            controlRating = Utilities.nonNull(other.controlRating, controlRating);
            habitability = Utilities.nonNull(other.habitability, habitability);
            dayLength = Utilities.nonNull(other.dayLength, dayLength);
            satellites = Utilities.nonNull(other.satellites, satellites);
            sysPos = Utilities.nonNull(other.sysPos, sysPos);
            temperature = Utilities.nonNull(other.temperature, temperature);
            socioIndustrial = Utilities.nonNull(other.socioIndustrial, socioIndustrial);
            // Merge (not replace!) events
            if( null != other.events ) {
                for( PlanetaryEvent event : other.getEvents() ) {
                    if( null != event && null != event.date ) {
                        PlanetaryEvent myEvent = getOrCreateEvent(event.date);
                        myEvent.climate = Utilities.nonNull(event.climate, myEvent.climate);
                        myEvent.faction = Utilities.nonNull(event.faction, myEvent.faction);
                        myEvent.hpg = Utilities.nonNull(event.hpg, myEvent.hpg);
                        myEvent.lifeForm = Utilities.nonNull(event.lifeForm, myEvent.lifeForm);
                        myEvent.message = Utilities.nonNull(event.message, myEvent.message);
                        myEvent.name = Utilities.nonNull(event.name, myEvent.name);
                        myEvent.percentWater = Utilities.nonNull(event.percentWater, myEvent.percentWater);
                        myEvent.shortName = Utilities.nonNull(event.shortName, myEvent.shortName);
                        myEvent.socioIndustrial = Utilities.nonNull(event.socioIndustrial, myEvent.socioIndustrial);
                        myEvent.temperature = Utilities.nonNull(event.temperature, myEvent.temperature);
                        myEvent.pressure = Utilities.nonNull(event.pressure, myEvent.pressure);
                        myEvent.pressureAtm = Utilities.nonNull(event.pressureAtm, myEvent.pressureAtm);
                        myEvent.atmMass = Utilities.nonNull(event.atmMass, myEvent.atmMass);
                        myEvent.atmosphere = Utilities.nonNull(event.atmosphere, myEvent.atmosphere);
                        myEvent.albedo = Utilities.nonNull(event.albedo, myEvent.albedo);
                        myEvent.greenhouseEffect = Utilities.nonNull(event.greenhouseEffect, myEvent.greenhouseEffect);
                        myEvent.habitability = Utilities.nonNull(event.habitability, myEvent.habitability);
                        myEvent.populationRating = Utilities.nonNull(event.populationRating, myEvent.populationRating);
                        myEvent.government = Utilities.nonNull(event.government, myEvent.government);
                        myEvent.controlRating = Utilities.nonNull(event.controlRating, myEvent.controlRating);
                        myEvent.nadirCharge = Utilities.nonNull(event.nadirCharge, myEvent.nadirCharge);
                        myEvent.zenithCharge = Utilities.nonNull(event.zenithCharge, myEvent.zenithCharge);
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
        if(this == object) {
            return true;
        }
        if((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final Planet other = (Planet) object;
        return Objects.equals(id, other.id);
    }

    public static int convertRatingToCode(String rating) {
        if(rating.equalsIgnoreCase("A")) { //$NON-NLS-1$
            return EquipmentType.RATING_A;
        }
        else if(rating.equalsIgnoreCase("B")) { //$NON-NLS-1$
            return EquipmentType.RATING_B;
        }
        else if(rating.equalsIgnoreCase("C")) { //$NON-NLS-1$
            return EquipmentType.RATING_C;
        }
        else if(rating.equalsIgnoreCase("D")) { //$NON-NLS-1$
            return EquipmentType.RATING_D;
        }
        else if(rating.equalsIgnoreCase("E")) { //$NON-NLS-1$
            return EquipmentType.RATING_E;
        }
        else if(rating.equalsIgnoreCase("F")) { //$NON-NLS-1$
            return EquipmentType.RATING_F;
        }
        return EquipmentType.RATING_C;
    }

    public static final class SocioIndustrialData {
        public static final SocioIndustrialData NONE = new SocioIndustrialData();
        static {
            NONE.tech = EquipmentType.RATING_X;
            NONE.industry = EquipmentType.RATING_X;
            NONE.rawMaterials = EquipmentType.RATING_X;
            NONE.output = EquipmentType.RATING_X;
            NONE.agriculture = EquipmentType.RATING_X;
        }
        
        public int tech;
        public int industry;
        public int rawMaterials;
        public int output;
        public int agriculture;
        
        @Override
        public String toString() {
             return EquipmentType.getRatingName(tech)
                + "-" + EquipmentType.getRatingName(industry) //$NON-NLS-1$
                + "-" + EquipmentType.getRatingName(rawMaterials) //$NON-NLS-1$
                + "-" + EquipmentType.getRatingName(output) //$NON-NLS-1$
                + "-" + EquipmentType.getRatingName(agriculture); //$NON-NLS-1$
             }
    }

    /** A class representing some event, possibly changing planetary information */
    public static final class PlanetaryEvent {
        @XmlJavaTypeAdapter(DateAdapter.class)
        public DateTime date;
        public String message;
        public String name;
        public String shortName;
        @XmlJavaTypeAdapter(StringListAdapter.class)
        public List<String> faction;
        @XmlJavaTypeAdapter(LifeFormAdapter.class)
        public LifeForm lifeForm;
        @XmlJavaTypeAdapter(ClimateAdapter.class)
        public Climate climate;
        public Integer percentWater;
        public Integer temperature;
        @XmlJavaTypeAdapter(SocioIndustrialDataAdapter.class)
        public SocioIndustrialData socioIndustrial;
        @XmlJavaTypeAdapter(HPGRatingAdapter.class)
        public Integer hpg;
        public Integer pressure;
        public Double pressureAtm;
        public Double atmMass;
        public String atmosphere;
        public Double albedo;
        public Double greenhouseEffect;
        public Integer habitability;
        @XmlElement(name = "pop")
        public Integer populationRating;
        public String government;
        public Integer controlRating;
        // Stellar support, to be moved later
        public Boolean nadirCharge;
        public Boolean zenithCharge;
    }
    
    public static final class FactionChange {
        @XmlJavaTypeAdapter(DateAdapter.class)
        public DateTime date;
        @XmlJavaTypeAdapter(StringListAdapter.class)
        public List<String> faction;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{"); //$NON-NLS-1$
               sb.append("date=").append(date).append(","); //$NON-NLS-1$ //$NON-NLS-2$
               sb.append("faction=").append(faction).append("}"); //$NON-NLS-1$ //$NON-NLS-2$
               return sb.toString();
        }
    }

    // @FunctionalInterface in Java 8, or just use Function<PlanetaryEvent, T>
    private static interface EventGetter<T> {
        T get(PlanetaryEvent e);
    }
    
    /** BT planet types */
    public static enum PlanetaryType {
        SMALL_ASTEROID, MEDIUM_ASTEROID, DWARF_TERRESTRIAL, TERRESTRIAL, GIANT_TERRESTRIAL, GAS_GIANT, ICE_GIANT;
    }
    
    /** Data class to hold parsed spectral definitions */
    public static final class SpectralDefinition {
        public String spectralType;
        public int spectralClass;
        public double subtype;
        public String luminosity;
        
        public SpectralDefinition(String spectralType, int spectralClass, double subtype, String luminosity) {
            this.spectralType = Objects.requireNonNull(spectralType);
            this.spectralClass = spectralClass;
            this.subtype = subtype;
            this.luminosity = Objects.requireNonNull(luminosity);
        }
    }
}