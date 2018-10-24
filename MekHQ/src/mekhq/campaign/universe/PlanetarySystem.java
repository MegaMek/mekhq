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
import java.util.Date;
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
import mekhq.Utilities;
import mekhq.adapter.BooleanValueAdapter;
import mekhq.adapter.SpectralClassAdapter;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.SocioIndustrialData;


/**
 * This is a PlanetarySystem object which will contain information
 * about the system as well as an ArrayList of the Planet objects
 * that make up the system
 *
 * @author Taharqa
 */

@XmlRootElement(name="system")
@XmlAccessorType(XmlAccessType.FIELD)
public class PlanetarySystem implements Serializable {
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
    @SuppressWarnings("unused")
    private UUID uniqueIdentifier;
    private String id;
    private String name;
    private String shortName;

    //Star data (to be factored out)
    private String spectralType;
    @XmlJavaTypeAdapter(SpectralClassAdapter.class)
    private Integer spectralClass;
    private Double subtype;
    private String luminosity;
    
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    private Boolean nadirCharge;
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    private Boolean zenithCharge;
    
    //tree map of planets sorted by system position
    @XmlTransient
    private TreeMap<Integer, Planet> planets;
    
    //for reading in because lists are easier
    @XmlElement(name = "planet")
    private List<Planet> planetList;

    //the location of the primary planet for this system
    private int primarySlot;
    
    /** Marker for "please delete this system" */
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    public Boolean delete;
    
    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     * Package-private so that Planets can access it
     */
    @XmlTransient
    TreeMap<DateTime, PlanetaryEvent> events;
    
    // For export and import only (lists are easier than maps) */
    @XmlElement(name = "event")
    private List<Planet.PlanetaryEvent> eventList;
    
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
    
    public String getName(DateTime when) {
        if(null != getPrimaryPlanet()) {
            return getPrimaryPlanet().getName(when);
        }
        return "Unknown";
        
    }

    public String getShortName(DateTime when) {
        if(null != getPrimaryPlanet()) {
            return getPrimaryPlanet().getName(when);
        }
        return "Unknown";
    }
    
    public List<String> getFactions(DateTime when) {
        ArrayList<String> factions = new ArrayList<String>();
        for(Planet planet : planets.values()) {
            List<String> f = planet.getFactions(when);
            if(null != f) {
                factions.addAll(f);
            }
        }
        return factions;
    }
    
    public Set<Faction> getFactionSet(DateTime when) {
        Set<Faction> factions = new HashSet<Faction>();
        for(Planet planet : planets.values()) {
            Set<Faction> f = planet.getFactionSet(when);
            if(null != f) {
                factions.addAll(f);
            }
        }
        return factions;
    }

    /** highest socio-industrial ratings among all planets in system for the map **/
    public SocioIndustrialData getSocioIndustrial(DateTime when) {
        int tech = EquipmentType.RATING_X;
        int industry = EquipmentType.RATING_X;
        int rawMaterials = EquipmentType.RATING_X;
        int output = EquipmentType.RATING_X;
        int agriculture = EquipmentType.RATING_X;
        
        for(Planet planet : planets.values()) {
            SocioIndustrialData sic = planet.getSocioIndustrial(when);
            if(null != sic) {
                if(sic.tech < tech) {
                    tech = sic.tech;
                }
                if(sic.industry < industry) {
                    industry = sic.industry;
                }
                if(sic.rawMaterials < rawMaterials) {
                    rawMaterials = sic.rawMaterials;
                }
                if(sic.output < output) {
                    output = sic.output;
                }
                if(sic.agriculture < agriculture) {
                    agriculture = sic.agriculture;
                }
            }
        }
        return new SocioIndustrialData(tech, industry, rawMaterials, output, agriculture);
    }
    
    /** @return the highest HPG rating among planets **/
    public Integer getHPG(DateTime when) {
        int rating = EquipmentType.RATING_X;
        for(Planet planet : planets.values()) {
            if(null != planet.getHPG(when) && planet.getHPG(when) < rating) {
                rating = planet.getHPG(when);
            }
        }
        return rating;
    }
    
    /** @return short name if set, else full name, else "unnamed" */
    public String getPrintableName(DateTime when) {
        String result = getPrimaryPlanet().getPrintableName(when);
        if(null == result) {
            return "Unnamed";
        }
        //remove numbers or roman numerals from name
        String new_result = "";
        for(String str : result.split("\\s+")) {
            if(str.matches("\\d+") || str.matches("(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV)")) {
                continue;
            }
            new_result = new_result + " " + str;
        }
        return new_result; //$NON-NLS-1$
    }
    
    /** @return the distance to a point in space in light years */
    public double getDistanceTo(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
    }
    
    /** @return the distance to another system in light years (0 if both are in the same system) */
    public double getDistanceTo(PlanetarySystem anotherSystem) {
        return Math.sqrt(Math.pow(x - anotherSystem.x, 2) + Math.pow(y - anotherSystem.y, 2));
    }
    
    public Boolean isNadirCharge(DateTime when) {
        //TODO: add event stuff to system so I can get this information
        /*
        return getEventData(when, nadirCharge, new EventGetter<Boolean>() {
            @Override public Boolean get(PlanetaryEvent e) { return e.nadirCharge; }
        });
        */
        return nadirCharge;
    }

    public boolean isZenithCharge(DateTime when) {
        //TODO: add event stuff to system so I can get this information
        /*
        return getEventData(when, zenithCharge, new EventGetter<Boolean>() {
            @Override public Boolean get(PlanetaryEvent e) { return e.zenithCharge; }
        });
        */
        return zenithCharge;
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
    
    public double getStarDistanceToJumpPoint() {
        if( null == spectralClass || null == subtype ) {
            return StarUtil.getDistanceToJumpPoint(42);
        }
        return StarUtil.getDistanceToJumpPoint(spectralClass, subtype);
    }
    
    /** @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary position*/
    public double getTimeToJumpPoint(double acceleration) {
        return getTimeToJumpPoint(acceleration, primarySlot);
    }
    
    /** @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary position*/
    public double getTimeToJumpPoint(double acceleration, int sysPos) {
        return planets.get(sysPos).getTimeToJumpPoint(acceleration);
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
    
    public Planet getPrimaryPlanet() {
        //TODO: safety checks please, a lot depends on this
        return planets.get(primarySlot);
    }
    
    public int getPrimaryPlanetPosition() {
        return primarySlot;
    }
    
    public Planet getPlanet(int pos) {
        return planets.get(pos);
    }
    
    public Set<Integer> getPlanetPositions() {
        return planets.keySet();
    }
    
    public Collection<Planet> getPlanets() {
        return planets.values();
    }
    
    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        }
        if((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final PlanetarySystem other = (PlanetarySystem) object;
        return Objects.equals(id, other.id);
    }
    
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
    
    // JAXB marshalling support
    @SuppressWarnings({ "unused", "unchecked" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if( null == id ) {
            id = name;
        }
        
        // Spectral classification: use spectralType if available, else the separate values
        if( null != spectralType ) {
            setSpectralType(spectralType);
        }
        nadirCharge = Utilities.nonNull(nadirCharge, Boolean.FALSE);
        zenithCharge = Utilities.nonNull(zenithCharge, Boolean.FALSE);
        
        //fill up planets
        planets = new TreeMap<Integer, Planet>();
        if(null != planetList) {
            for(Planet p : planetList) {
                p.setParentSystem(this);
                if(!planets.containsKey(p.getSystemPosition())) {
                    planets.put(p.getSystemPosition(), p);
                }
            }
            planetList.clear();
        }
        planetList = null;
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
    }
    
    @SuppressWarnings("unused")
    private boolean beforeMarshal(Marshaller marshaller) {
        // Fill up our event list from the internal data type
        eventList = new ArrayList<PlanetaryEvent>(events.values());
        //same for planet list
        planetList = new ArrayList<Planet>(planets.values());
        return true;
    }
    
    public void copyDataFrom(PlanetarySystem other) {
        if( null != other ) {
            // We don't change the ID
            name = Utilities.nonNull(other.name, name);
            shortName = Utilities.nonNull(other.shortName, shortName);
            x = Utilities.nonNull(other.x, x);
            y = Utilities.nonNull(other.y, y);
            //TODO: some other changes should be possible
            // Merge (not replace!) events
            if(null != other.events) {
                for(PlanetaryEvent event : other.getEvents()) {
                    if( null != event && null != event.date ) {
                        PlanetaryEvent myEvent = getOrCreateEvent(event.date);
                        myEvent.copyDataFrom(event);
                    }
                }
            }
            //check for planet level changes
            if(null != other.planets) {
                for(Planet p : other.planets.values()) {
                    int pos = p.getSystemPosition();
                    if(planets.containsKey(pos)) {
                        planets.get(pos).copyDataFrom(p);
                    } else {
                        planets.put(pos, p);
                    }
                }
            }
        }
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