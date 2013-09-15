/*
 * Loot.java
 * 
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.Part;


/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loot implements MekHqXmlSerializable {
   
    private String name;
    private long cash;
    private ArrayList<Entity> units;
    private ArrayList<Part> parts;
    //Personnel?
    
    public Loot() {
        name = "None";
        cash = 0;
        units = new ArrayList<Entity>();
        parts = new ArrayList<Part>();
    }
    
    @Override
    public Object clone() {
        Loot newLoot = new Loot();
        newLoot.name = name;
        newLoot.cash = cash;
        newLoot.units = units;
        newLoot.parts = parts;
        return newLoot;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String s) {
        name = s;
    }
    
    public void setCash(long c) {
        cash = c;
    }
    
    public long getCash() {
        return cash;
    }
    
    public void addUnit(Entity e) {
        units.add(e);
    }
    
    public ArrayList<Entity> getUnits() {
        return units;
    }
    
    public void clearUnits() {
        units = new ArrayList<Entity>();
    }
    
    public ArrayList<Part> getParts() {
        return parts;
    }
    
    public void addPart(Part p) {
        parts.add(p);
    }
    
    public void clearParts() {
        parts = new ArrayList<Part>();
    }
    
    public String getShortDescription() {
        String desc = getName() + " - ";
        if(cash > 0) {
            desc += DecimalFormat.getIntegerInstance().format(cash) + " C-bills";
        }
        if(units.size() > 0) {
            String s = units.size() + " unit";
            if(units.size() > 1) {
                s += "s";
            }
            if(cash > 0) {
                s = ", " + s;
            }
            desc += s;
        }
        if(parts.size() > 0) {
            String s = parts.size() + " part";
            if(parts.size() > 1) {
                s += "s";
            }
            if(cash > 0 || units.size() > 0) {
                s = ", " + s;
            }
            desc += s;
        }
        return desc;
    }
    
    public void get(Campaign campaign, Scenario s) {
        //TODO: put in some reports
        if(cash > 0) {
            campaign.getFinances().credit(cash, Transaction.C_MISC, "Reward for " + getName() + " during " + s.getName(), campaign.getDate());
        }
        for(Entity e : units) {
            campaign.addUnit(e, false, 0);
        }
        for(Part p : parts) {
            campaign.addPart(p, 0);
        }
    }
    
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<loot>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<name>"
                +MekHqXmlUtil.escape(name)
                +"</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<cash>"
                +cash
                +"</cash>");
        if(units.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<units>");
            for(Entity e : units) {
                String lookupName = e.getChassis() + " " + e.getModel();
                lookupName.replaceAll("\\s+$", "");
                pw1.println(MekHqXmlUtil.indentStr(indent+2)
                        +"<entityName>"
                        +lookupName
                        +"</entityName>");
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</units>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</loot>");
    }
    
    public static Loot generateInstanceFromXML(Node wn) {
        Loot retVal = null;
       
        try {
            retVal = new Loot();
            
            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("cash")) {
                    retVal.cash = Long.parseLong(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y=0; y<nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        
                        if (!wn3.getNodeName().equalsIgnoreCase("entityName")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in techUnitIds nodes: "+wn3.getNodeName());
                            continue;
                        }               
                        MechSummary summary = MechSummaryCache.getInstance().getMech(wn3.getTextContent());
                        if(null == summary) {
                            throw(new EntityLoadingException());
                        }
                        Entity e = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                        if(null == e) {
                             continue;
                        }
                        retVal.units.add(e);
                    }
                } 
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
        }
        
        return retVal;
    }
    
}