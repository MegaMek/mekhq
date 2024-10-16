/*
 * Loot.java
 *
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
import java.util.HashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Loot {
    private static final MMLogger logger = MMLogger.create(Loot.class);

    private String name;
    private Money cash;
    private ArrayList<Entity> units;
    private ArrayList<Part> parts;
    // Personnel?

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
        if (!cash.isZero()) {
            desc += cash.toAmountAndSymbolString();
        }

        if (!units.isEmpty()) {
            String s = units.size() + " unit";
            if (units.size() > 1) {
                s += "s";
            }

            if (!cash.isZero()) {
                s = ", " + s;
            }
            desc += s;
        }

        if (!parts.isEmpty()) {
            String s = parts.size() + " part";
            if (parts.size() > 1) {
                s += "s";
            }

            if (!cash.isZero() || !units.isEmpty()) {
                s = ", " + s;
            }
            desc += s;
        }

        return desc;

    }

    /**
     * Looting method that adds loot to the campaign, including cash, parts, and
     * units.
     *
     * @param campaign the campaign to add the loot to
     * @param scenario the scenario during which the loot was acquired
     */
    public void getLoot(Campaign campaign, Scenario scenario) {
        if (cash.isPositive()) {
            logger.debug("Looting cash: {}", cash);

            campaign.getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(), cash,
                    "Reward for " + getName() + " during " + scenario.getName());
        }

        for (Part p : parts) {
            logger.debug("Looting part: {}", p.getName());

            campaign.getQuartermaster().addPart(p, 0);

            logger.debug("Looting parts complete");
        }

        // This only needs to be done once, so we do it outside the 'loot units' loop
        // for efficiency
        HashMap<String, Integer> qualityAndModifier = getQualityAndModifier(
                campaign.getMission(scenario.getMissionId()));

        for (Entity e : units) {
            logger.debug("Looting unit: {}", e.getDisplayName());

            if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
                qualityAndModifier.put("quality",
                    Unit.getRandomUnitQuality(qualityAndModifier.get("modifier")).toNumeric());
            }

            campaign.addNewUnit(e, false, 0,
                PartQuality.fromNumeric(qualityAndModifier.get("quality")));

            logger.debug("Looting units complete");
        }
    }

    /**
     * Returns fixed quality values, and modifiers (for dynamic quality) used to
     * generate a new unit
     * with quality based on the equipment quality of the contract OpFor.
     * If the contract isn't an instance of AtBContract we use fixed values.
     *
     * @param contract the mission contract
     * @return a HashMap containing quality and modifier as key-value pairs:
     * @throws IllegalStateException if the contract is an instance of AtBContract
     *                               and the enemy quality is not recognized
     */
    private static HashMap<String, Integer> getQualityAndModifier(Mission contract) {
        HashMap<String, Integer> qualityAndModifier = new HashMap<>();

        if (contract instanceof AtBContract) {
            switch (((AtBContract) contract).getEnemyQuality()) {
                case IUnitRating.DRAGOON_F:
                    qualityAndModifier.put("quality", PartQuality.QUALITY_A.toNumeric());
                    qualityAndModifier.put("modifier", -2);
                    break;
                case IUnitRating.DRAGOON_D:
                    qualityAndModifier.put("quality", PartQuality.QUALITY_B.toNumeric());
                    qualityAndModifier.put("modifier", -1);
                    break;
                case IUnitRating.DRAGOON_C:
                case IUnitRating.DRAGOON_B:
                    qualityAndModifier.put("quality", PartQuality.QUALITY_C.toNumeric());
                    qualityAndModifier.put("modifier", 0);
                    break;
                case IUnitRating.DRAGOON_A:
                    qualityAndModifier.put("quality", PartQuality.QUALITY_D.toNumeric());
                    qualityAndModifier.put("modifier", 1);
                    break;
                case IUnitRating.DRAGOON_ASTAR:
                    qualityAndModifier.put("quality", PartQuality.QUALITY_F.toNumeric());
                    qualityAndModifier.put("modifier", 2);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected value in mekhq/campaign/mission/Loot.java/getQualityAndModifier: "
                                    + ((AtBContract) contract).getEnemyQuality());
            }
        } else {
            qualityAndModifier.put("quality", PartQuality.QUALITY_D.toNumeric());
            qualityAndModifier.put("modifier", 0);
        }

        return qualityAndModifier;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "loot");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cash", getCash());
        for (Entity e : units) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "entityName", e.getShortNameRaw());
        }

        for (Part p : parts) {
            p.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "loot");
    }

    public static Loot generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Loot retVal = null;

        try {
            retVal = new Loot();

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("cash")) {
                    retVal.cash = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("entityName")) {
                    MekSummary summary = MekSummaryCache.getInstance().getMek(wn2.getTextContent());
                    if (null == summary) {
                        throw (new EntityLoadingException());
                    }
                    Entity e = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                    if (null == e) {
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
            logger.error("", ex);
        }

        return retVal;
    }
}
