/*
 * Asset.java
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
 */

package mekhq.campaign.finances;

import java.io.PrintWriter;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An asset is a fake pre-existing asset that a user can enter in order to increase loan
 * collateral and get bigger loans - it is generally for merc campaigns that are just starting out
 * Assets can also generate income on a schedule
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Asset implements MekHqXmlSerializable {

    private String name;
    private long value;
    //lets only allow monthly and yearly and pay at the first of each
    private int schedule;
    private long income;
    
    public Asset() {
        name = "New Asset";
        value = 0;
        schedule = Finances.SCHEDULE_YEARLY;
        income = 0;
    }
    
    public Asset(String n, long v, long inc, int s) {
        name = n;
        schedule = s;
        value = v;
        income = inc;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String s) {
        name = s;
    }
    
    public long getValue() {
        return value;
    }
    
    public void setValue(Long l) {
        value = l;
    }
    
    public int getSchedule() {
        return schedule;
    }
    
    public void setSchedule(int s) {
        schedule = s;
    }
    
    public long getIncome() {
        return income;
    }
    
    public void setIncome(long l) {
        income = l;
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<asset>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<name>"
                +MekHqXmlUtil.escape(name)
                +"</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<value>"
                +value
                +"</value>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<schedule>"
                +schedule
                +"</schedule>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<income>"
                +income
                +"</income>");
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</asset>");
    }

    public static Asset generateInstanceFromXML(Node wn) {
        Asset retVal = new Asset();
        
        NodeList nl = wn.getChildNodes();
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("name")) {
                retVal.name = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("value")) {
                retVal.value = Long.parseLong(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("income")) {
                retVal.income = Long.parseLong(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("schedule")) {
                retVal.schedule = Integer.parseInt(wn2.getTextContent().trim());
            } 
        }
        return retVal;
    }
}