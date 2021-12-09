/*
 * Copyright (C) 2018 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TechAdvancement;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;

/**
 * Like {@link OmniPod} this is never added to a <code>Unit</code>. <code>OmniPod</code> is used for empty
 * pods in the warehouse, and <code>MissingOmniPod</code> is used for acquisition.
 *
 * @author neoancient
 */
public class MissingOmniPod extends MissingPart {
    private static final long serialVersionUID = -1231514024730868438L;

    // Pods are specific to the type of equipment they contain.
    private Part partType;

    public MissingOmniPod() {
        this(new EquipmentPart(), null);
    }

    /**
     * @param partType The type of part that can be installed in this pod
     * @param c        The campaign
     */
    public MissingOmniPod(Part partType, Campaign c) {
        super(0, false, c);
        this.partType = partType;
    }

    @Override
    public void setCampaign(Campaign c) {
        super.setCampaign(c);
        partType.setCampaign(c);
    }

    /**
     * @return The type of part that can be installed in this pod
     */
    public Part getPartType() {
        return partType;
    }

    /**
     * Exports class data to xml
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.print(MekHqXmlUtil.indentStr(indent + 1) + "<partType tonnage='" + partType.getUnitTonnage()
            + "' type='");
        if (partType instanceof AeroHeatSink) {
            pw1.print("AeroHeatSink' hsType='" + ((AeroHeatSink) partType).getType());
        } else if (partType instanceof EquipmentPart) {
            pw1.print(((EquipmentPart) partType).getType().getInternalName());
            if (partType instanceof MASC) {
                pw1.print("' rating='" + ((MASC) partType).getEngineRating());
            }
        } else {
            MekHQ.getLogger().info("MissingOmniPod partType is not EquipmentType");
        }
        pw1.println("'/>");
        writeToXmlEnd(pw1, indent);
    }


    /**
     * Loads class fields from XML
     */
    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("partType")) {
                if (null == wn2.getAttributes().getNamedItem("type")) {
                    MekHQ.getLogger().error("OmniPod lacks part type attribute.");
                } else if (null == wn2.getAttributes().getNamedItem("tonnage")) {
                    MekHQ.getLogger().error("OmniPod lacks partType tonnage attribute.");
                } else {
                    String type = wn2.getAttributes().getNamedItem("type").getTextContent();
                    int tonnage = Integer.parseInt(wn2.getAttributes().getNamedItem("tonnage").getTextContent());
                    if (type.equals("AeroHeatSink")) {
                        int hsType = -1;
                        if (null != wn2.getAttributes().getNamedItem("hsType")) {
                            hsType = Integer.parseInt(wn2.getAttributes().getNamedItem("hsType").getTextContent());
                        }
                        if (hsType != Aero.HEAT_SINGLE && hsType != Aero.HEAT_DOUBLE && hsType != AeroHeatSink.CLAN_HEAT_DOUBLE) {
                            MekHQ.getLogger().error("Aero heatsink OmniPod does not have a legal value for heat sink type; using SINGLE");
                            hsType = Aero.HEAT_SINGLE;
                        }
                        partType = new AeroHeatSink(0, hsType, false, campaign);
                    } else {
                        EquipmentType et = EquipmentType.get(type);
                        if (null == et) {
                            MekHQ.getLogger().error("Unknown part type " + type + " for OmniPod");
                            //Throw a generic value in there to prevent NPE but still indicate a problem
                            et = EquipmentType.get(EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_STANDARD));
                        }
                        if (et instanceof MiscType
                                && (et.hasFlag(MiscType.F_HEAT_SINK)
                                        || et.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                                        || et.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE))) {
                            partType = new HeatSink(0, et, -1, false, campaign);
                        } else if (et instanceof MiscType && et.hasFlag(MiscType.F_JUMP_JET)) {
                            partType = new JumpJet(tonnage, et, -1, false, campaign);
                        } else if (et instanceof MiscType
                                && et.hasFlag(MiscType.F_MASC)
                                && (et.getSubType() & MiscType.S_SUPERCHARGER) == 0) {
                            if (null != wn2.getAttributes().getNamedItem("rating")) {
                                int rating = Integer.parseInt(wn2.getAttributes().getNamedItem("rating").getTextContent());
                                partType = new MASC(tonnage, et, -1, campaign, rating, false);
                            } else {
                                MekHQ.getLogger().error("OmniPod for MASC lacks engine rating");
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
    public Part getNewPart() {
        return new OmniPod(getPartType(), campaign);
    }

    //Using tech rating for Omni construction option from IOps.
    @Override
    public int getTechRating() {
        return EquipmentType.RATING_E;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return Entity.getOmniAdvancement();
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        // not relevant
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof OmniPod) && part.isSamePartType(partType);
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public String getLocationName() {
        return null;
    }

}
