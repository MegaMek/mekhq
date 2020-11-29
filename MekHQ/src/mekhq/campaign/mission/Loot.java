/*
 * Loot.java
 *
 * Copyright (c) 2011 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.util.ArrayList;

import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.Part;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loot implements MekHqXmlSerializable {

    private String name;
    private Money cash;
    private ArrayList<Entity> units;
    private ArrayList<Part> parts;
    //Personnel?

    public Loot() {
        name = "None";
        cash = Money.zero();
        units = new ArrayList<>();
        parts = new ArrayList<>();
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

    public void setCash(Money c) {
        cash = c;
    }

    public Money getCash() {
        return cash;
    }

    public void addUnit(Entity e) {
        units.add(e);
    }

    public ArrayList<Entity> getUnits() {
        return units;
    }

    public void clearUnits() {
        units = new ArrayList<>();
    }

    public ArrayList<Part> getParts() {
        return parts;
    }

    public void addPart(Part p) {
        parts.add(p);
    }

    public void clearParts() {
        parts = new ArrayList<>();
    }

    public String getShortDescription() {
        String desc = getName() + " - ";
        if(cash.isPositive()) {
            desc += cash.toAmountAndSymbolString();
        }
        if(units.size() > 0) {
            String s = units.size() + " unit";
            if(units.size() > 1) {
                s += "s";
            }
            if(cash.isPositive()) {
                s = ", " + s;
            }
            desc += s;
        }
        if(parts.size() > 0) {
            String s = parts.size() + " part";
            if(parts.size() > 1) {
                s += "s";
            }
            if(cash.isPositive() || units.size() > 0) {
                s = ", " + s;
            }
            desc += s;
        }
        return desc;
    }

    public void get(Campaign campaign, Scenario s) {
        //TODO: put in some reports
        if(cash.isPositive()) {
            campaign.getFinances().credit(cash, Transaction.C_MISC,
                    "Reward for " + getName() + " during " + s.getName(), campaign.getLocalDate());
        }
        for(Entity e : units) {
            campaign.addNewUnit(e, false, 0);
        }
        for(Part p : parts) {
            campaign.getQuartermaster().addPart(p, 0);
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<loot>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<name>"
                +MekHqXmlUtil.escape(name)
                +"</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<cash>"
                +cash.toXmlString()
                +"</cash>");
        for (Entity e : units) {
            String lookupName = e.getChassis() + " " + e.getModel();
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<entityName>"
                    +lookupName.trim()
                    +"</entityName>");
        }
        for (Part p : parts) {
            p.writeToXml(pw1, indent+1);
        }

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</loot>");
    }

    public static Loot generateInstanceFromXML(Node wn, Campaign c, Version version) {
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
                    retVal.cash = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("entityName")) {
                    MechSummary summary = MechSummaryCache.getInstance().getMech(wn2.getTextContent());
                    if(null == summary) {
                        throw(new EntityLoadingException());
                    }
                    Entity e = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                    if(null == e) {
                        continue;
                    }
                    retVal.units.add(e);
                } else if (wn2.getNodeName().equalsIgnoreCase("part")) {
                    Part p = Part.generateInstanceFromXML(wn2, version);
                    p.setCampaign(c);
                    retVal.parts.add(p);
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
