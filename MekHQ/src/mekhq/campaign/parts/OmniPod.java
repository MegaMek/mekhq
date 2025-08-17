/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An empty omnipod, which can be purchased or created when equipment is removed from a pod. When fixed, the omnipod is
 * removed from the warehouse and one replacement part is podded.
 *
 * @author Neoancient
 */
public class OmniPod extends Part {
    private static final MMLogger logger = MMLogger.create(OmniPod.class);

    // Pods are specific to the type of equipment they contain.
    private Part partType;

    public OmniPod() {
        this(new EquipmentPart(), null);
    }

    public OmniPod(Part partType, Campaign c) {
        super(0, false, c);
        this.partType = partType;
        if ((null != partType) && partType.isOmniPodded()) {
            partType.setOmniPodded(false);
        }
        name = "OmniPod";
    }

    /**
     * @return The tech base of the part the omnipod is meant to contain.
     */
    @Override
    public TechBase getTechBase() {
        if (null != partType) {
            return partType.getTechBase();
        } else {
            return TechBase.ALL;
        }
    }

    @Override
    public void setCampaign(Campaign c) {
        super.setCampaign(c);
        partType.setCampaign(c);
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        String details = partType.getDetails(includeRepairDetails);
        if (!details.isEmpty()) {
            return partType.getName() + " (" + details + ")";
        } else {
            return partType.getName();
        }
    }

    @Override
    public int getBaseTime() {
        return partType.getMissingPart().getBaseTime();
    }

    @Override
    public void updateConditionFromPart() {
        // do nothing
    }

    // This can only be found in the warehouse
    @Override
    public int getLocation() {
        return -1;
    }

    @Override
    public @Nullable String checkFixable() {
        if (partType.getMissingPart().isReplacementAvailable()) {
            return null;
        }
        return "No equipment available to install";
    }

    // Podding equipment is a Class D (Maintenance) refit, which carries a +2
    // modifier.
    @Override
    public int getDifficulty() {
        return partType.getDifficulty() + 2;
    }

    @Override
    public String getRepairDesc() {
        if (partType.getMissingPart().isReplacementAvailable()) {
            return super.getRepairDesc();
        } else {
            return "Part not available";
        }
    }

    // Weight is negligible
    @Override
    public double getTonnage() {
        return 0;
    }

    // Using tech rating for Omni construction option from IOps.
    @Override
    public TechRating getTechRating() {
        return TechRating.E;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return Entity.getOmniAdvancement();
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("partType")) {
                if (null == wn2.getAttributes().getNamedItem("type")) {
                    logger.error("OmniPod lacks part type attribute.");
                } else if (null == wn2.getAttributes().getNamedItem("tonnage")) {
                    logger.error("OmniPod lacks partType tonnage attribute.");
                } else {
                    String type = wn2.getAttributes().getNamedItem("type").getTextContent();
                    int tonnage = Integer.parseInt(wn2.getAttributes().getNamedItem("tonnage").getTextContent());
                    if (type.equals("AeroHeatSink")) {
                        int hsType = -1;
                        if (null != wn2.getAttributes().getNamedItem("hsType")) {
                            hsType = Integer.parseInt(wn2.getAttributes().getNamedItem("hsType").getTextContent());
                        }
                        if ((hsType != Aero.HEAT_SINGLE) && (hsType != Aero.HEAT_DOUBLE)) {
                            logger.error(
                                  "Aero heatsink OmniPod does not have a legal value for heat sink type; using SINGLE");
                            hsType = Aero.HEAT_SINGLE;
                        }
                        partType = new AeroHeatSink(0, hsType, false, campaign);
                    } else {
                        EquipmentType et = EquipmentType.get(type);
                        if (null == et) {
                            logger.error("Unknown part type " + type + " for OmniPod");
                            // Throw a generic value in there to prevent NPE but still indicate a problem
                            et = EquipmentType.get(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_STANDARD));
                        }
                        if (et instanceof MiscType &&
                                  (et.hasFlag(MiscType.F_HEAT_SINK) ||
                                         et.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) ||
                                         et.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE))) {
                            partType = new HeatSink(0, et, -1, false, campaign);
                        } else if (et instanceof MiscType && et.hasFlag(MiscType.F_JUMP_JET)) {
                            partType = new JumpJet(tonnage, et, -1, false, campaign);
                        } else if (et instanceof MiscType &&
                                         et.hasFlag(MiscType.F_MASC) &&
                                         (et.getSubType() & MiscType.S_SUPERCHARGER) == 0) {
                            if (null != wn2.getAttributes().getNamedItem("rating")) {
                                int rating = Integer.parseInt(wn2.getAttributes()
                                                                    .getNamedItem("rating")
                                                                    .getTextContent());
                                partType = new MASC(tonnage, et, -1, campaign, rating, false);
                            } else {
                                logger.error("OmniPod for MASC lacks engine rating");
                            }
                        } else {
                            partType = new EquipmentPart(tonnage, et, -1, 1.0, false, campaign);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // do nothing
    }

    @Override
    public void remove(boolean salvage) {
        // do nothing
    }

    @Override
    public MissingPart getMissingPart() {
        // This is used only for acquisition.
        return new MissingOmniPod(partType, campaign);
    }

    @Override
    public boolean needsFixing() {
        return true;
    }

    @Override
    public void fix() {
        Part newPart = partType.clone();
        Part oldPart = campaign.getWarehouse().checkForExistingSparePart(newPart.clone());
        if (null != oldPart) {
            newPart.setOmniPodded(true);
            campaign.getQuartermaster().addPart(newPart, 0);
            oldPart.decrementQuantity();
        }
    }

    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        if (skillMin > SkillType.EXP_LEGENDARY) {
            return " <font color='" + ReportingUtilities.getNegativeColor()
                         + "'><b> failed and part destroyed.</b></font>";
        } else {
            // OmniPod is only added back to the warehouse if repair fails without
            // destroying part.
            campaign.getQuartermaster().addPart(this, 0);
            return " <font color='" + ReportingUtilities.getNegativeColor() + "'><b> failed.</b></font>";
        }
    }

    @Override
    public String getStatus() {
        String toReturn = "Empty";
        if (isReservedForRefit()) {
            toReturn = "Reserved for Refit";
        }
        if (isReservedForReplacement()) {
            toReturn = "Reserved for Repair";
        }
        if (isBeingWorkedOn()) {
            toReturn = "Being worked on";
        }
        if (!isPresent()) {
            // toReturn = "" + getDaysToArrival() + " days to arrival";
            String dayName = "day";
            if (getDaysToArrival() > 1) {
                dayName += "s";
            }
            toReturn = "In transit (" + getDaysToArrival() + " " + dayName + ")";
        }
        return toReturn;
    }

    @Override
    public Money getStickerPrice() {
        return partType.getStickerPrice().dividedBy(5.0);
    }

    @Override
    public int getTechLevel() {
        if (partType.isClanTechBase()) {
            return TechConstants.T_CLAN_TW;
        } else {
            return TechConstants.T_IS_TW_ALL;
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof OmniPod) && partType.isSamePartType(((OmniPod) part).partType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        pw.print(MHQXMLUtility.indentStr(indent) + "<partType tonnage='" + partType.getUnitTonnage() + "' type='");
        if (partType instanceof AeroHeatSink) {
            pw.print("AeroHeatSink' hsType='" + ((AeroHeatSink) partType).getType());
        } else if (partType instanceof EquipmentPart) {
            pw.print(((EquipmentPart) partType).getType().getInternalName());
            if (partType instanceof MASC) {
                pw.print("' rating='" + ((MASC) partType).getEngineRating());
            }
        } else {
            logger.info("OmniPod partType is not EquipmentType");
        }
        pw.println("'/>");
        writeToXMLEnd(pw, indent);
    }

    @Override
    public Part clone() {
        Part p = new OmniPod(partType, campaign);
        p.copyBaseData(this);
        return p;
    }
}
