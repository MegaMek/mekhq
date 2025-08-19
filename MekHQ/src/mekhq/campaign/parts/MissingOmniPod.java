/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Like {@link OmniPod} this is never added to a <code>Unit</code>.
 * <code>OmniPod</code> is used for empty
 * pods in the warehouse, and <code>MissingOmniPod</code> is used for acquisition.
 *
 * @author neoancient
 */
public class MissingOmniPod extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingOmniPod.class);

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

    /**
     * Exports class data to xml
     */
    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        pw.print(MHQXMLUtility.indentStr(indent) + "<partType tonnage='" + partType.getUnitTonnage()
                       + "' type='");
        if (partType instanceof AeroHeatSink) {
            pw.print("AeroHeatSink' hsType='" + ((AeroHeatSink) partType).getType());
        } else if (partType instanceof EquipmentPart) {
            pw.print(((EquipmentPart) partType).getType().getInternalName());
            if (partType instanceof MASC) {
                pw.print("' rating='" + ((MASC) partType).getEngineRating());
            }
        } else {
            logger.info("MissingOmniPod partType is not EquipmentType");
        }
        pw.println("'/>");
        writeToXMLEnd(pw, indent);
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
                        if (hsType != Aero.HEAT_SINGLE && hsType != Aero.HEAT_DOUBLE
                                  && hsType != AeroHeatSink.CLAN_HEAT_DOUBLE) {
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
                                int rating = Integer
                                                   .parseInt(wn2.getAttributes()
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
    public Part getNewPart() {
        return new OmniPod(getPartType(), campaign);
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
    public @Nullable String checkFixable() {
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
