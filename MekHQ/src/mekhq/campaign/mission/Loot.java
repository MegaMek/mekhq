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

import megamek.Version;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker.UnitStatus;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

import static mekhq.campaign.mission.resupplyAndCaches.GenerateResupplyContents.RESUPPLY_MINIMUM_PART_WEIGHT;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_AMMO_TONNAGE;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_ARMOR_TONNAGE;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

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
        StringBuilder description = new StringBuilder("<html>");

        description.append("<b>").append(getName()).append("</b>");
        if (!cash.isZero()) {
            description.append("<br>- ").append(cash.toAmountAndSymbolString());
        }

        for (Entity entity : units) {
            description.append("<br>- ").append(entity.getDisplayName());
        }

        for (Part part : parts) {
            description.append("<br>- ").append(part.getName());
            if (part.isClan()) {
                description.append(" (Clan)");
            }
            description.append(" [").append(part.getQualityName()).append(']');
        }

        description.append("</html>");

        return description.toString();

    }

    /**
     * Handles the looting process after a scenario is completed by adding loot to the campaign.
     * The loot can include cash rewards, salvageable parts, and units, along with handling
     * special scenarios like resupply interception.
     *
     * <p>This method evaluates the loot based on the scenario type and unit statuses,
     * ensuring that captured, lost, or excess loot is appropriately managed.</p>
     *
     * @param campaign              the campaign to which the looted resources (e.g., cash, parts, units) are added
     * @param scenario              the specific scenario during which the loot was acquired
     * @param unitsStatuses         a mapping of unit IDs to their respective status after the scenario
     *                              (e.g., whether units are lost, captured, or operational)
     */
    public void getLoot(Campaign campaign, Scenario scenario,
                        Hashtable<UUID, UnitStatus> unitsStatuses) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Loot",
            MekHQ.getMHQOptions().getLocale());

        boolean isResupply = scenario.getStratConScenarioType() == ScenarioType.SPECIAL_RESUPPLY;
        double cargo = 0;

        // If we're looting as the result of a StratCon Emergency Convoy Defence scenario,
        // we need to determine how much of the convoy survived
        if (isResupply) {
            List<UUID> allUnitIds = new ArrayList<>(
                scenario.getForces(campaign).getAllUnits(false));

            for (UUID unitId : allUnitIds) {
                Unit unit = campaign.getUnit(unitId);

                if (unit != null) {
                    if (unitsStatuses.containsKey(unitId)) {
                        if (unitsStatuses.get(unitId).isTotalLoss()) {
                            logger.debug("Unit {} is total loss", unit.getName());
                            continue;
                        }

                        if (unitsStatuses.get(unitId).isLikelyCaptured()) {
                            logger.debug("Unit {} is likely captured", unit.getName());
                            continue;
                        }
                    }

                    cargo += unit.getCargoCapacity();
                }
            }
            logger.debug("cargo capacity: {}", cargo);
        }

        if (cash.isPositive()) {
            logger.debug("Looting cash: {}", cash);

            campaign.getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(), cash,
                    "Reward for " + getName() + " during " + scenario.getName());

            campaign.addReport(String.format(resources.getString("looted.cash"),
                cash.toAmountAndSymbolString(),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
        }

        List<String> abandonedParts = new ArrayList<>();
        List<String> lootedParts = new ArrayList<>();
        Collections.shuffle(parts);

        logger.info("Looting parts: {}", parts.toString());

        for (Part part : parts) {
            double partWeight = part.getTonnage();
            partWeight = partWeight == 0 ? RESUPPLY_MINIMUM_PART_WEIGHT : partWeight;

            if (part instanceof AmmoBin) {
                partWeight = RESUPPLY_AMMO_TONNAGE;
            } else if (part instanceof Armor) {
                partWeight = RESUPPLY_ARMOR_TONNAGE;
            }

            if (isResupply) {
                if (cargo - partWeight < 0) {
                    abandonedParts.add("<br>- " + part.getName() + " (" + partWeight + " tons)");
                    continue;
                } else {
                    cargo -= partWeight;
                }
            }

            logger.debug("Looting part: {}", part.getName());

            lootedParts.add("<br>- " + part.getName() + " (" + partWeight + " tons)");
            campaign.getQuartermaster().addPart(part, 0);

            logger.debug("Looting parts complete");
        }

        if (!lootedParts.isEmpty()) {
            String lootedPartsReport = lootedParts.toString()
                .replace("[", "")
                .replace("]", "");
            campaign.addReport(String.format(resources.getString("looted.successful.parts"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG, lootedPartsReport));
        }

        if (!abandonedParts.isEmpty()) {
            String abandonedPartsReport = abandonedParts.toString()
                .replace("[", "")
                .replace("]", "");
            campaign.addReport(String.format(resources.getString("looted.failed.parts"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG, abandonedPartsReport));
        }

        // This only needs to be done once, so we do it outside the 'loot units' loop
        // for efficiency
        HashMap<String, Integer> qualityAndModifier = getQualityAndModifier(
                campaign.getMission(scenario.getMissionId()));

        for (Entity entity : units) {
            logger.debug("Looting unit: {}", entity.getDisplayName());

            if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
                qualityAndModifier.put("quality",
                    Unit.getRandomUnitQuality(qualityAndModifier.get("modifier")).toNumeric());
            }

            campaign.addNewUnit(entity, false, 0,
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
