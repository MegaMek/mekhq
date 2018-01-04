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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

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
import megamek.common.ITechnology;
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
import mekhq.campaign.universe.Faction.Tag;


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
    private UUID uniqueIdentifier;
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
    @XmlElement(name = "volcanism")
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
    private String icon;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     * Package-private so that Planets can access it
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
    public Planet(String tsvData, List<DateTime> years) throws Exception {
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
            if(plusIndex > 0) {
                String yearString = nameString.substring(plusIndex - 4, plusIndex);
                nameChangeYear = Integer.parseInt(yearString);
            }
            
            // this is a dirty hack: in order to avoid colliding with faction changes, we
            // set name changes to be a second into the new year
            DateTime nameChangeYearDate = new DateTime(nameChangeYear, 1, 1, 0, 0, 1, 0);
            
            String altName;
            String primaryName = nameString;
            PlanetaryEvent nameChangeEvent = null;
            int parenIndex = nameString.indexOf('(');
            int closingParenIndex = nameString.indexOf(')');
            // this indicates that there's an (Alternate Name) sequence of some kind
            if(parenIndex > 0) {
                // we chop off the year if there is one
                if(plusIndex > 0) {
                    altName = nameString.substring(parenIndex + 1, plusIndex - 5);
                }
                // otherwise, we just chop off the closing paren
                else {
                    altName = nameString.substring(parenIndex + 1, closingParenIndex);
                }
                
                // there are a few situations where all this stuff with parens is for naught, which is
                // PlanetName (FactionCode) or if the PlanetName (AltName) is already in our planets "database"
                
                if(null == Faction.getFaction(altName) && null == Planets.getInstance().getPlanetById(primaryName)) {
                    primaryName = nameString.substring(0, parenIndex - 1);
                    
                    nameChangeEvent = new PlanetaryEvent();
                    nameChangeEvent.date = nameChangeYearDate;
                    nameChangeEvent.name = altName;
                }
            }
            
            // now we have a primary name and possibly a name change planetary event
            this.name = primaryName;
            
            if(null != nameChangeEvent) {
                this.events.put(nameChangeYearDate, nameChangeEvent);
                this.eventList.add(nameChangeEvent);
            }
            
            this.id = this.name;
            this.x = Double.parseDouble(infoElements[1]);
            this.y = Double.parseDouble(infoElements[2]);
            
            for(int x = 3; x < infoElements.length; x++) {
                String infoElement = infoElements[x].replace("\"", "");
                String newFaction;
                
                if(infoElement.trim().length() == 0) {
                    newFaction = "";
                }
                
                int commaIndex = infoElement.indexOf(',');
                if(commaIndex < 0) { // sometimes there are no commas
                    newFaction = infoElement;
                } else {
                    // anything after the first comma is fluff
                    // we also want to forego the opening quote
                    newFaction = infoElement.substring(0, commaIndex);
                }
                
                //dirty hack, replace faction name with one we can use
                if(factionReplacements.containsKey(newFaction)) {
                    newFaction = factionReplacements.get(newFaction);
                }
                
                // for brevity, only add the new event if the faction hasn't changed since the previous event
                // or if it's the first event
                
                // dirty hack here assumes that there's only one faction per event, which is true in the case
                // of this spreadsheet
                if(x == 3 || !eventList.get(eventList.size() - 1).faction.get(0).equals(newFaction)) {
                    PlanetaryEvent pe = new PlanetaryEvent();
                    DateTime eventDate = years.get(x - 3);
                    pe.faction = new ArrayList<String>();
                    pe.faction.add(newFaction);
                    pe.date = eventDate;
                    
                    this.eventList.add(pe);                
                    this.events.put(eventDate, pe);
                }
            }
        } catch(Exception e) {
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
    public PlanetaryEvent getOrCreateEvent(DateTime when) {
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
    
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        
        for(PlanetaryEvent p : events.values()) {
            names.add(p.name);
        }
        
        return names;
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
        return StarUtil.getHPGClass(getHPG(when));
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
            factions.add(Faction.getFaction(code.toUpperCase()));
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
        return Faction.getFactionNames(getFactionSet(when), when.getYear());
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

    /** @return the distance to a point in space in light years */
    public double getDistanceTo(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
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
     * Updates the current planet's coordinates and faction ownership from the given other planet.
     * Makes several assumptions about the way the other planet's ownership events are structured.
     * @param tsvPlanet The planet from which to update.
     * @param dryRun Whether to actually perform the updates.
     * @return Human readable form of what was/would have been updated. 
     */
    public String updateFromTSVPlanet(Planet tsvPlanet, boolean dryRun) {
        StringBuilder sb = new StringBuilder();
        
        if(!tsvPlanet.x.equals(this.x) || !tsvPlanet.y.equals(this.y)) {
            sb.append("Coordinate update from " + x + ", " + y + " to " + tsvPlanet.x + ", " + tsvPlanet.y + "\r\n");
            
            if(!dryRun) {
                this.x = tsvPlanet.x;
                this.y = tsvPlanet.y;
            }
        }
        
        // loop using index
        // look ahead by one event (if possible) and check that getFaction(next event year) isn't already
        // the same as the faction from the current event : sometimes, our data is more exact than the incoming data 
        for(int eventIndex = 0; eventIndex < tsvPlanet.getEvents().size(); eventIndex++) {
            PlanetaryEvent event = tsvPlanet.getEvents().get(eventIndex);
            // check other planet events (currently only updating faction change events)
            // if the other planet has an 'ownership change' event with a non-"U" faction
            // check that this planet does not have an existing non-"U" faction already owning it at the event date
            // and does not acquire such a faction between this and the next event
            // Then we will add an the ownership change event
            
            if(event.faction != null && event.faction.size() > 0) { 
                // the purpose of this code is to evaluate whether the current "other planet" event is 
                // a faction change to an active, valid faction.
                Faction eventFaction = Faction.getFaction(event.faction.get(0));
                boolean eventHasActualFaction = eventFaction != null ? !eventFaction.is(Tag.INACTIVE) && !eventFaction.is(Tag.ABANDONED) : false;
                
                if(eventHasActualFaction) {
                    List<String> currentFactions = this.getFactions(event.date);
                                         
                    // if this planet has an "inactive and abandoned" current faction... 
                    // we also want to catch the situation where the next faction change isn't to the same exact faction 
                    if(currentFactions.size() == 1 && 
                            Faction.getFaction(currentFactions.get(0)).is(Tag.INACTIVE) &&
                            Faction.getFaction(currentFactions.get(0)).is(Tag.ABANDONED)) {
                        
                        // now we travel into the future, to the next "other" event, and if this planet has acquired a faction
                        // before the next "other" event, then we
                        int nextEventIndex = eventIndex + 1;                        
                        PlanetaryEvent nextEvent = nextEventIndex < tsvPlanet.getEvents().size() ? tsvPlanet.getEvents().get(nextEventIndex) : null;
                        DateTime nextEventDate; 
                        
                        // if we're at the last event, then just check that the planet doesn't have a faction in the year 3600
                        if(nextEvent == null) {
                            nextEventDate = new DateTime(3600, 1, 1, 0, 0, 1, 0);
                        } else {
                            nextEventDate = nextEvent.date;
                        }
                        
                        List<String> nextFactions = this.getFactions(nextEventDate);
                        boolean factionBeforeNextEvent = !(nextFactions.size() == 1 &&
                                Faction.getFaction(nextFactions.get(0)).is(Tag.INACTIVE) &&
                                Faction.getFaction(nextFactions.get(0)).is(Tag.ABANDONED));
                        
                        if(!factionBeforeNextEvent) {
                            sb.append("Adding faction change in " + event.date.getYear() + " from " + currentFactions.get(0) + " to " + event.faction + "\r\n");
                            
                            if(!dryRun) {
                                this.events.put(event.date, event);
                            }
                        }
                    }
                }
            }
        }
        
        if(sb.length() > 0) {
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
            icon = Utilities.nonNull(other.icon, icon);
            // Merge (not replace!) events
            if( null != other.events ) {
                for( PlanetaryEvent event : other.getEvents() ) {
                    if( null != event && null != event.date ) {
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
             return ITechnology.getRatingName(tech)
                + "-" + ITechnology.getRatingName(industry) //$NON-NLS-1$
                + "-" + ITechnology.getRatingName(rawMaterials) //$NON-NLS-1$
                + "-" + ITechnology.getRatingName(output) //$NON-NLS-1$
                + "-" + ITechnology.getRatingName(agriculture); //$NON-NLS-1$
         }
        
        /** @return the USILR rating as a HTML description */
        public String getHTMLDescription() {
            // TODO: Internationalization
            // TODO: Some way to encode "advanced" ultra-tech worlds (rating "AA" for technological sophistication)
            // TODO: Some way to encode "regressed" worlds
            // Note that rating "E" isn't used in official USILR codes, but we add them for completeness
            StringBuilder sb = new StringBuilder("<html><body style='width: 50px; font: 10px sans-serif'>");
            switch(tech) {
                case -1:
                    sb.append("Advanced: Ultra high-tech world<br>");
                    break;
                case EquipmentType.RATING_A:
                    sb.append("A: High-tech world<br>");
                    break;
                case EquipmentType.RATING_B:
                    sb.append("B: Advanced world<br>");
                    break;
                case EquipmentType.RATING_C:
                    sb.append("C: Moderately advanced world<br>");
                    break;
                case EquipmentType.RATING_D:
                    sb.append("D: Lower-tech world; about 21st- to 22nd-century level<br>");
                    break;
                case EquipmentType.RATING_E:
                    sb.append("E: Lower-tech world; about 20th century level<br>");
                    break;
                case EquipmentType.RATING_F:
                    sb.append("F: Primitive world<br>");
                    break;
                case EquipmentType.RATING_X:
                    sb.append("Regressed: Pre-industrial world<br>");
                    break;
                default:
                    sb.append("X: Technological sophistication unknown<br>");
                    break;
            }
            switch(industry) {
                case EquipmentType.RATING_A:
                    sb.append("A: Heavily industrialized<br>");
                    break;
                case EquipmentType.RATING_B:
                    sb.append("B: Moderately industrialized<br>");
                    break;
                case EquipmentType.RATING_C:
                    sb.append("C: Basic heavy industry; about 22nd century level<br>");
                    break;
                case EquipmentType.RATING_D:
                    sb.append("D: Low industrialization; about 20th century level<br>");
                    break;
                case EquipmentType.RATING_E:
                    sb.append("E: Very low industrialization; about 19th century level<br>");
                    break;
                case EquipmentType.RATING_F:
                    sb.append("F: No industrialization<br>");
                    break;
                default:
                    sb.append("X: Industrialization level unknown<br>");
                    break;
            }
            switch(rawMaterials) {
                case EquipmentType.RATING_A:
                    sb.append("A: Fully self-sufficient raw material production<br>");
                    break;
                case EquipmentType.RATING_B:
                    sb.append("B: Mostly self-sufficient raw material production<br>");
                    break;
                case EquipmentType.RATING_C:
                    sb.append("C: Limited raw material production<br>");
                    break;
                case EquipmentType.RATING_D:
                    sb.append("D: Production dependent on imports of raw materials<br>");
                    break;
                case EquipmentType.RATING_E:
                    sb.append("E: Production highly dependent on imports of raw materials<br>");
                    break;
                case EquipmentType.RATING_F:
                    sb.append("F: No economically viable local raw material production<br>");
                    break;
                default:
                    sb.append("X: Raw material dependence unknown<br>");
                    break;
            }
            switch(output) {
                case EquipmentType.RATING_A:
                    sb.append("A: High industrial output<br>");
                    break;
                case EquipmentType.RATING_B:
                    sb.append("B: Good industrial output<br>");
                    break;
                case EquipmentType.RATING_C:
                    sb.append("C: Limited industrial output<br>"); // Bad for Ferengi
                    break;
                case EquipmentType.RATING_D:
                    sb.append("D: Negligable industrial output<br>");
                    break;
                case EquipmentType.RATING_E:
                    sb.append("E: Negligable industrial output<br>");
                    break;
                case EquipmentType.RATING_F:
                    sb.append("F: No industrial output<br>"); // Good for Ferengi
                    break;
                default:
                    sb.append("X: Industrial output unknown<br>");
                    break;
            }
            switch(agriculture) {
                case EquipmentType.RATING_A:
                    sb.append("A: Breadbasket<br>");
                    break;
                case EquipmentType.RATING_B:
                    sb.append("B: Agriculturally abundant world<br>");
                    break;
                case EquipmentType.RATING_C:
                    sb.append("C: Modest agriculture<br>");
                    break;
                case EquipmentType.RATING_D:
                    sb.append("D: Poor agriculture<br>");
                    break;
                case EquipmentType.RATING_E:
                    sb.append("E: Very poor agriculture<br>");
                    break;
                case EquipmentType.RATING_F:
                    sb.append("F: Barren world<br>");
                    break;
                default:
                    sb.append("X: Agricultural level unknown<br>");
                    break;
            }

            return sb.append("</body></html>").toString();
        }
    }

    /** A class representing some event, possibly changing planetary information */
    @XmlRootElement(name="event")
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
        // Events marked as "custom" are saved to scenario files and loaded from there
        public transient boolean custom = false;
        
        public void copyDataFrom(PlanetaryEvent other) {
            climate = Utilities.nonNull(other.climate, climate);
            faction = Utilities.nonNull(other.faction, faction);
            hpg = Utilities.nonNull(other.hpg, hpg);
            lifeForm = Utilities.nonNull(other.lifeForm, lifeForm);
            message = Utilities.nonNull(other.message, message);
            name = Utilities.nonNull(other.name, name);
            percentWater = Utilities.nonNull(other.percentWater, percentWater);
            shortName = Utilities.nonNull(other.shortName, shortName);
            socioIndustrial = Utilities.nonNull(other.socioIndustrial, socioIndustrial);
            temperature = Utilities.nonNull(other.temperature, temperature);
            pressure = Utilities.nonNull(other.pressure, pressure);
            pressureAtm = Utilities.nonNull(other.pressureAtm, pressureAtm);
            atmMass = Utilities.nonNull(other.atmMass, atmMass);
            atmosphere = Utilities.nonNull(other.atmosphere, atmosphere);
            albedo = Utilities.nonNull(other.albedo, albedo);
            greenhouseEffect = Utilities.nonNull(other.greenhouseEffect, greenhouseEffect);
            habitability = Utilities.nonNull(other.habitability, habitability);
            populationRating = Utilities.nonNull(other.populationRating, populationRating);
            government = Utilities.nonNull(other.government, government);
            controlRating = Utilities.nonNull(other.controlRating, controlRating);
            nadirCharge = Utilities.nonNull(other.nadirCharge, nadirCharge);
            zenithCharge = Utilities.nonNull(other.zenithCharge, zenithCharge);
            custom = (other.custom || custom);
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
            pressureAtm = other.pressureAtm;
            atmMass = other.atmMass;
            atmosphere = other.atmosphere;
            albedo = other.albedo;
            greenhouseEffect = other.greenhouseEffect;
            habitability = other.habitability;
            populationRating = other.populationRating;
            government = other.government;
            controlRating = other.controlRating;
            nadirCharge = other.nadirCharge;
            zenithCharge = other.zenithCharge;
            custom = (other.custom || custom);
        }
        
        /** @return <code>true</code> if the event doesn't contain any change */
        public boolean isEmpty() {
            return (null == climate) && (null == faction) && (null == hpg) && (null == lifeForm)
                && (null == message) && (null == name) && (null == shortName) && (null == socioIndustrial)
                && (null == temperature) && (null == pressure) && (null == pressureAtm)
                && (null == atmMass) && (null == atmosphere) && (null == albedo) && (null == greenhouseEffect)
                && (null == habitability) && (null == populationRating) && (null == government)
                && (null == controlRating) && (null == nadirCharge) && (null == zenithCharge);
        }
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