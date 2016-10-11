/*
 * java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * 
 * @author: Dylan Myers <ralgith@gmail.com>
 */
package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.ExtraData;
import mekhq.campaign.mod.am.InjuryTypes;

// Injury class based on Jayof9s' <jayof9s@gmail.com> Advanced Medical documents
@XmlRootElement(name="injury")
@XmlAccessorType(XmlAccessType.FIELD)
public class Injury {
    public static final int VERSION = 1;
    
    // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(Injury.class, BodyLocation.class, InjuryType.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            //marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch(JAXBException e) {
            MekHQ.logError(e);
        }
    } 
    
    /**
     *  Load from campaign file XML
     *  <p>
     *  Also used by the personnel exporter
     */
    public static Injury generateInstanceFromXML(Node wn) {
        try {    
            return unmarshaller.unmarshal(wn, Injury.class).getValue();
        } catch (Exception ex) {
            MekHQ.logError(ex);
        }
        return null;
    }

    private String fluff;
    private int days;
    private int originalDays;
    /** 0 = past injury, for scars, 1 = default, max depends on type */
    private int hits;
    private BodyLocation location;
    private InjuryType type;
    private boolean permanent;
    /** Flag to indicate someone capable successfully treated this injury. */
    private boolean workedOn;
    private boolean extended;
    @XmlElement(name="InjuryUUID")
    private UUID id;
    /** Generic extra data, for use with plugins and mods */
    private ExtraData extraData = new ExtraData();
    @XmlAttribute(name="v")
    private int version;
    
    // Base constructor, in reality should never be used
    public Injury() {
        this(0, "", BodyLocation.GENERIC, InjuryType.BAD_HEALTH, 1, false, false);
    }
    
    // Normal constructor for a new injury that has not been treated by a doctor & does not have extended time
    public Injury(int time, String text, BodyLocation loc, InjuryType type, int num, boolean perm) {
        this(time, text, loc, type, num, perm, false);
    }

    // Constructor if this injury has been treated by a doctor, but without extended time
    public Injury(int time, String text, BodyLocation loc, InjuryType type, int num, boolean perm, boolean workedOn) {
        this(time, text, loc, type, num, perm, workedOn, false);
    }
    
    // Constructor for when this injury has extended time, full options includng worked on by a doctor
    public Injury(int time, String text, BodyLocation loc, InjuryType type, int num, boolean perm, boolean workedOn, boolean extended) {
        setTime(time);
        setOriginalTime(time);
        setFluff(text);
        setLocation(loc);
        setType(type);
        setHits(num);
        setPermanent(perm);
        setWorkedOn(workedOn);
        setExtended(extended);
        id = UUID.randomUUID();
    }
    
    // UUID Control Methods
    public UUID getUUID() {
        return id;
    }
    
    public void setUUID(UUID uuid) {
        id = uuid;
    }
    // End UUID Control Methods
    
    // Time Control Methods
    public int getTime() {
        return days;
    }
    
    public void setTime(int time) {
        days = time;
    }
    
    public int getOriginalTime() {
        return originalDays;
    }
    
    public void setOriginalTime(int time) {
        originalDays = time;
    }
    // End Time Control Methods
    
    // Details Methods (Fluff, Location on Body, how many hits did it take, etc...)
    public String getFluff() {
        return fluff;
    }
    
    public void setFluff(String text) {
        fluff = text;
    }
    
    public BodyLocation getLocation() {
        return location;
    }
    
    public void setLocation(BodyLocation loc) {
        location = loc;
    }
    
    public int getHits() {
        return hits;
    }
    
    public void setHits(int num) {
        final int minSeverity = isPermanent() ? 1 : 0;
        final int maxSeverity = type.getMaxSeverity();
        if(num < minSeverity) {
            num = minSeverity;
        } else if(num > maxSeverity) {
            num = maxSeverity;
        }
        hits = num;
    }
    
    public boolean isPermanent() {
        return permanent || type.isPermanent();
    }
    
    public void setPermanent(boolean perm) {
        permanent = perm;
    }
    
    public boolean getExtended() {
        return extended;
    }
    
    public void setExtended(boolean ext) {
        extended = ext;
    }
    
    public boolean isWorkedOn() {
        return workedOn;
    }
    
    public void setWorkedOn(boolean wo) {
        workedOn = wo;
    }
    
    public InjuryType getType() {
        return type;
    }
    
    public void setType(InjuryType type) {
        this.type = Objects.requireNonNull(type);
    }
    
    public Collection<Modifier> getModifiers() {
        return type.getModifiers(this);
    }
    
    public ExtraData getExtraData() {
        return extraData;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
    // End Details Methods
    
    // Returns the full long name of this injury including location and type as applicable
    public String getName() {
        return type.getName(location, hits);
    }
    
    // Return the location name for the injury by passing location to the static overload
    public String getLocationName() {
        return Utilities.capitalize(location.readableName);
    }
    
    public String getTypeKey() {
        return type.getKey();
    }
    
    public int getTypeId() {
        return type.getId();
    }
    
    // Save to campaign file as XML
    // Also used by the personnel exporter
    public void writeToXml(PrintWriter pw1, int indent) {
        try {
            marshaller.marshal(this, pw1);
        } catch(JAXBException ex) {
            MekHQ.logError(ex);
        }
    }
    
    @SuppressWarnings({ "unused", "incomplete-switch" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        // Fix old-style "hits" into "severity".
        if(version < 1) {
            if(type == InjuryTypes.CONCUSSION) {
                hits -= 1;
            } else if(type == InjuryTypes.INTERNAL_BLEEDING) {
                hits -= 2;
            } else {
                hits = 1;
            }
        }
        
        // Fix hand/foot locations
        if(fluff.endsWith(" hand")) {
            switch(location) {
                case LEFT_ARM: location = BodyLocation.LEFT_HAND; break;
                case RIGHT_ARM: location = BodyLocation.RIGHT_HAND; break;
                default: // do nothing
            }
        }
        if(fluff.endsWith(" foot")) {
            switch(location) {
                case LEFT_LEG: location = BodyLocation.LEFT_FOOT; break;
                case RIGHT_LEG: location = BodyLocation.RIGHT_FOOT; break;
                default: // do nothing
            }
        }
        
        if (null == id) { // We didn't have an ID, so let's generate one!
            id = UUID.randomUUID();
        }
        
        if(null == extraData) {
            extraData = new ExtraData();
        }
        
        version = Injury.VERSION;
    }
}