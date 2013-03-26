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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.parts.Part;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loot implements MekHqXmlSerializable {
   
    private long cash;
    private ArrayList<Entity> units;
    private ArrayList<Part> parts;
    //Personnel?
    
    public Loot() {
        cash = 0;
        units = new ArrayList<Entity>();
        parts = new ArrayList<Part>();
    }
    
    public void setCash(long c) {
        cash = c;
    }
    
    public void addUnit(Entity e) {
        units.add(e);
    }
    
    public void addPart(Part p) {
        parts.add(p);
    }
    
    public void getLoot(Campaign campaign) {
        campaign.getFinances().credit(cash, Transaction.C_MISC, "loot", campaign.getDate());
        for(Entity e : units) {
            campaign.addUnit(e, false, 0);
        }
        for(Part p : parts) {
            campaign.addPart(p, 0);
        }
    }
    
    public void writeToXml(PrintWriter pw1, int indent) {
     /*   SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<scenario id=\""
                +id
                +"\" type=\""
                +this.getClass().getName()
                +"\">");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<name>"
                +MekHqXmlUtil.escape(name)
                +"</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<desc>"
                +MekHqXmlUtil.escape(desc)
                +"</desc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<report>"
                +MekHqXmlUtil.escape(report)
                +"</report>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<status>"
                +status
                +"</status>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<id>"
                +id
                +"</id>");
        if(null != stub) {
            stub.writeToXml(pw1, indent+1);
        }
        if(null != date) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<date>"
                    +df.format(date)
                    +"</date>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</scenario>");
        */
    }
    
 /*   public static Scenario generateInstanceFromXML(Node wn) {
        Scenario retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        
        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = (Scenario) Class.forName(className).newInstance();
            
            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.status = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDesc(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("report")) {
                    retVal.setReport(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceStub")) {
                    retVal.stub = ForceStub.generateInstanceFromXML(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.date = df.parse(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }
        
        return retVal;
    }*/
    
}