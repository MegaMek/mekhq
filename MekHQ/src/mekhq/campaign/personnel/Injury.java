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
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;

// Injury class based on Jayof9s' <jayof9s@gmail.com> Advanced Medical documents
public class Injury {
    private String fluff;
    private int days;
    private int originalDays;
    private int hits;
    private BodyLocation location;
    private int type;
    private boolean permanent;
    private boolean workedOn;
    private boolean extended;
    protected UUID id;
    
    // Static defines for type of injury
    // Do not reorder these for backwards compatibility!
     public static final int INJ_CUT = 0;
     public static final int INJ_BRUISE = 1;
     public static final int INJ_LACERATION = 2;
     public static final int INJ_SPRAIN = 3;
     public static final int INJ_CONCUSSION = 4;
     public static final int INJ_BROKEN_RIB = 5;
     public static final int INJ_BRUISED_KIDNEY = 6;
     public static final int INJ_BROKEN_LIMB = 7;
     public static final int INJ_BROKEN_COLLAR_BONE = 8;
     public static final int INJ_INTERNAL_BLEEDING = 9;
     public static final int INJ_LOST_LIMB = 10;
     public static final int INJ_CEREBRAL_CONTUSION = 11;
     public static final int INJ_PUNCTURED_LUNG = 12;
     public static final int INJ_CTE = 13;
     public static final int INJ_BROKEN_BACK = 14;
     public static final int INJ_NUM = 15;
     
     // Base constructor, in reality should never be used
     public Injury() {
         this(0, "", BodyLocation.GENERIC, 0, 1, false, false);
    }
    
     // Normal constructor for a new injury that has not been treated by a doctor & does not have extended time
    public Injury(int time, String text, BodyLocation loc, int type, int num, boolean perm) {
        this(time, text, loc, type, num, perm, false);
    }

    // Constructor if this injury has been treated by a doctor, but without extended time
    public Injury(int time, String text, BodyLocation loc, int type, int num, boolean perm, boolean workedOn) {
        this(time, text, loc, type, num, perm, workedOn, false);
    }
    
    // Constructor for when this injury has extended time, full options includng worked on by a doctor
    public Injury(int time, String text, BodyLocation loc, int type, int num, boolean perm, boolean workedOn, boolean extended) {
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
    
    public String getUUIDAsString() {
        return id.toString();
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
        hits = num;
    }
    
    public boolean getPermanent() {
        return permanent;
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
    
    public boolean getWorkedOn() {
        return workedOn;
    }
    
    public void setWorkedOn(boolean wo) {
        workedOn = wo;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    // End Details Methods
    
    // Returns the full long name of this injury including location and type as applicable
    public String getName() {
        String buffer = Utilities.capitalize(location.readableName);
        
        switch (type) {
        case INJ_CUT:
            buffer = "Cut "+buffer;
            break;
         case INJ_BRUISE:
            buffer = "Bruised "+buffer;
            break;
         case INJ_LACERATION:
            buffer = "Lacerated "+buffer;
            break;
         case INJ_SPRAIN:
            buffer = "Sprained "+buffer;
            break;
         case INJ_CONCUSSION:
            buffer = "Concussion";
            break;
         case INJ_BROKEN_RIB:
            buffer = "Broken Rib";
            break;
         case INJ_BRUISED_KIDNEY:
            buffer = "Bruised Kidney";
            break;
         case INJ_BROKEN_LIMB:
            buffer = "Broken "+buffer;
            break;
         case INJ_BROKEN_COLLAR_BONE:
            buffer = "Broken Collarbone";
            break;
         case INJ_INTERNAL_BLEEDING:
            buffer = "Internal Bleeding";
            break;
         case INJ_LOST_LIMB:
            buffer = "Missing "+buffer;
            break;
         case INJ_CEREBRAL_CONTUSION:
            buffer = "Cerebral Contusion";
            break;
         case INJ_PUNCTURED_LUNG:
            buffer = "Punctured Lung";
            break;
         case INJ_CTE:
            buffer = "Chronic Traumatic Encephalopathy";
            break;
         case INJ_BROKEN_BACK:
            buffer = "Broken Back";
            break;
        }
        
        return buffer;
    }
    
    // Save to campaign file as XML
    // Also used by the personnel exporter
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<injury>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<fluff>"
                +MekHqXmlUtil.escape(fluff)
                +"</fluff>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<days>"
                +days
                +"</days>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<originalDays>"
                +originalDays
                +"</originalDays>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<hits>"
                +hits
                +"</hits>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<location>"
                +location
                +"</location>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<type>"
                +type
                +"</type>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<permanent>"
                +permanent
                +"</permanent>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<extended>"
                +extended
                +"</extended>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<workedOn>"
                +workedOn
                +"</workedOn>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<InjuryUUID>"
                +id.toString()
                +"</InjuryUUID>");
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</injury>");
    }
    
    // Load from campaign file XML
    // Also used by the personnel exporter
    public static Injury generateInstanceFromXML(Node wn) {
        Injury retVal = new Injury();
        
        try {    
            // Okay, now load fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("fluff")) {
                    retVal.fluff = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("days")) {
                    retVal.days = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalDays")) {
                    retVal.originalDays = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                    retVal.location = BodyLocation.of(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    retVal.type = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("permanent")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.permanent = true;
                    else
                        retVal.permanent = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("extended")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.extended = true;
                    else
                        retVal.extended = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("workedOn")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.workedOn = true;
                    else
                        retVal.workedOn = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("InjuryUUID")) {
                    retVal.id = UUID.fromString(wn2.getTextContent());
                }
            }
            if (retVal.id == null) { // We didn't have an ID, so let's generate one!
                retVal.id = UUID.randomUUID();
            }
            // Fix hand/foot locations
            if(retVal.fluff.endsWith(" hand")) {
                switch(retVal.location) {
                    case LEFT_ARM: retVal.location = BodyLocation.LEFT_HAND; break;
                    case RIGHT_ARM: retVal.location = BodyLocation.RIGHT_HAND; break;
                    default: // do nothing
                }
            }
            if(retVal.fluff.endsWith(" foot")) {
                switch(retVal.location) {
                    case LEFT_LEG: retVal.location = BodyLocation.LEFT_FOOT; break;
                    case RIGHT_LEG: retVal.location = BodyLocation.RIGHT_FOOT; break;
                    default: // do nothing
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.logError(ex);
        }
        
        return retVal;
    }
    
    // Return the location name for the injury by passing location to the static overload
    public String getLocationName() {
        return Utilities.capitalize(location.readableName);
    }
    
    // Return the name for this type of injury by passing the type to the static overload
    public String getTypeName() {
        return getTypeName(type);
    }
    
    // Return the name of a specific type of injury
    public static String getTypeName(int type) {
        String buffer = "";
        
        switch (type) {
        case INJ_CUT:
            buffer = "Cut";
            break;
         case INJ_BRUISE:
            buffer = "Bruised";
            break;
         case INJ_LACERATION:
            buffer = "Lacerated";
            break;
         case INJ_SPRAIN:
            buffer = "Sprained";
            break;
         case INJ_CONCUSSION:
            buffer = "Concussion";
            break;
         case INJ_BROKEN_RIB:
            buffer = "Broken Rib";
            break;
         case INJ_BRUISED_KIDNEY:
            buffer = "Bruised Kidney";
            break;
         case INJ_BROKEN_LIMB:
            buffer = "Broken";
            break;
         case INJ_BROKEN_COLLAR_BONE:
            buffer = "Broken Collarbone";
            break;
         case INJ_INTERNAL_BLEEDING:
            buffer = "Internal Bleeding";
            break;
         case INJ_LOST_LIMB:
            buffer = "Missing";
            break;
         case INJ_CEREBRAL_CONTUSION:
            buffer = "Cerebral Contusion";
            break;
         case INJ_PUNCTURED_LUNG:
            buffer = "Punctured Lung";
            break;
         case INJ_CTE:
            buffer = "Chronic Traumatic Encephalopathy";
            break;
         case INJ_BROKEN_BACK:
            buffer = "Broken Back";
            break;
        }
        
        return buffer;
    }
    
    // Generate appropriate fluff text for this injury based on type and location. Uses proper gender pronouns.
    public static String generateInjuryFluffText(int type, BodyLocation location, int genderType) {
        String name = location.readableName;
        
        switch (type) {
        case INJ_CUT:
            return "Some cuts on "+Person.getGenderPronoun(genderType, Person.PRONOUN_HISHER)+" "+name;
        case INJ_BRUISE:
            return "A bruise on "+Person.getGenderPronoun(genderType, Person.PRONOUN_HISHER)+" "+name;
        case INJ_LACERATION:
            return "A laceration on "+Person.getGenderPronoun(genderType, Person.PRONOUN_HISHER)+" "+name;
        case INJ_SPRAIN:
            return "A sprained "+name;
        case INJ_CONCUSSION:
            return "A concussion";
        case INJ_BROKEN_RIB:
            return "A broken rib";
        case INJ_BRUISED_KIDNEY:
            return "A bruised kidney";
        case INJ_BROKEN_LIMB:
            return "A broken "+name;
        case INJ_BROKEN_COLLAR_BONE:
            return "A broken collar bone";
        case INJ_INTERNAL_BLEEDING:
            return "Internal bleeding";
        case INJ_LOST_LIMB:
            return "Lost "+Person.getGenderPronoun(genderType, Person.PRONOUN_HISHER)+" "+name;
        case INJ_CEREBRAL_CONTUSION:
            return "A cerebral contusion";
        case INJ_PUNCTURED_LUNG:
            return "A punctured lung";
        case INJ_CTE:
            return "Chronic traumatic encephalopathy";
        case INJ_BROKEN_BACK:
            return "A broken back";
        default:
            System.err.println("ERROR: Default CASE reached in (Advanced Medical Section) Person.generateFluffText()");
            break;
        }
        return "";
    }
    
    // Called when creating a new injury to determine the type of injury it is
    public static int getInjuryTypeByLocation(BodyLocation loc, int roll, int hit_location) {
        switch (loc) {
        case LEFT_ARM: case LEFT_HAND:
        case RIGHT_ARM: case RIGHT_HAND:
        case LEFT_LEG: case LEFT_FOOT:
        case RIGHT_LEG: case RIGHT_FOOT:
            if (hit_location == 1) {
                if (roll == 2) {
                    return INJ_CUT;
                } else {
                    return INJ_BRUISE;
                }
            } else if (hit_location == 2) {
                return INJ_SPRAIN;
            } else if (hit_location == 3) {
                return INJ_BROKEN_LIMB;
            } else if (hit_location > 3) {
                return INJ_LOST_LIMB;
            }
            break;
        case HEAD:
            if (hit_location == 1) {
                return INJ_LACERATION;
            } else if (hit_location == 2 || hit_location == 3) {
                return INJ_CONCUSSION;
            } else if (hit_location == 4) {
                return INJ_CEREBRAL_CONTUSION;
            } else if (hit_location > 4) {
                return INJ_CTE;
            }
            break;
        case CHEST:
            if (hit_location == 1) {
                if (roll == 2) {
                    return INJ_CUT;
                } else {
                    return INJ_BRUISE;
                }
            } else if (hit_location == 2) {
                return INJ_BROKEN_RIB;
            } else if (hit_location == 3) {
                return INJ_BROKEN_COLLAR_BONE;
            } else if (hit_location == 4) {
                return INJ_PUNCTURED_LUNG;
            } else if (hit_location > 4) {
                return INJ_BROKEN_BACK;
            }
            break;
        case ABDOMEN:
            if (hit_location == 1) {
                if (roll == 2) {
                    return INJ_CUT;
                } else {
                    return INJ_BRUISE;
                }
            } else if (hit_location == 2) {
                return INJ_BRUISED_KIDNEY;
            } else if (hit_location > 2) {
                return INJ_INTERNAL_BLEEDING;
            }
            break;
        }
        return 0;
    }
    
    // Called when creating a new injury to generate a slightly randomized healing time
    public static int generateHealingTime(Campaign c, int type, int hits, Person p) {
        int rand = Compute.randomInt(100);
        int mod = 100;
        int time;
        if (rand < 5) {
            if (Compute.d6() < 4) {
                mod += rand;
            } else {
                mod -= rand;
            }
        }
        switch (type) {
        case INJ_CUT:
        case INJ_BRUISE:
        case INJ_LACERATION:
            time = Compute.d6();
            break;
        case INJ_SPRAIN:
            time = 12;
            break;
        case INJ_CONCUSSION:
            if (hits > 2) {
                time = (14*3);
            } else {
                time = 14;
            }
            break;
        case INJ_BROKEN_RIB:
            time = 20;
            break;
        case INJ_BRUISED_KIDNEY:
            time = 10;
            break;
        case INJ_BROKEN_LIMB:
            time = 30;
            break;
        case INJ_BROKEN_COLLAR_BONE:
            time = 22;
            break;
        case INJ_INTERNAL_BLEEDING:
            time = 20;
            if (hits == 4) {
                time = time * 2;
            } if (hits == 5) {
                time = time * 3;
            }
            break;
        case INJ_LOST_LIMB:
            time = 28;
            break;
        case INJ_CEREBRAL_CONTUSION:
            time = 90;
            break;
        case INJ_PUNCTURED_LUNG:
            time = 20;
            break;
        case INJ_CTE:
            time = 180;
            break;
        case INJ_BROKEN_BACK:
            time = 150;
            break;
        default:
            System.err.println("ERROR: Default CASE reached in (Advanced Medical Section) Person.generateHealingTime()");
            return Compute.d6()+5;
        }
        time = Math.round(time * mod / 100);
        time = Math.round(time * p.getAbilityTimeModifier() / 100);
        return time;
    }
}