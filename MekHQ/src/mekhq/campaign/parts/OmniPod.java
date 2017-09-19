/*
 * Copyright (C) 2017 MegaMek team
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.personnel.SkillType;

/**
 * An empty omnipod, which can be purchased or created when equipment is removed from a pod.
 * When fixed, the omnipod is removed from the warehouse and one replacement part is podded.
 * 
 * @author Neoancient
 *
 */
public class OmniPod extends Part {

    private static final long serialVersionUID = -8236359530423260992L;
    
    // Pods are specific to the type of equipment they contain.
    private Part partType;
    
    public OmniPod() {
        this(new EquipmentPart(), null);
    }

    public OmniPod(Part partType, Campaign c) {
        super(0, false, c);
        this.partType = partType;
        if (null != partType) {
            partType.setOmniPodded(false);
        }
        name = "OmniPod";
    }
    
    @Override
    public void setCampaign(Campaign c) {
        super.setCampaign(c);
        partType.setCampaign(c);
    }

    @Override
    public String getDetails() {
        return partType.getName();
    }
    
    @Override
    public int getBaseTime() {
        return partType.getMissingPart().getBaseTime();
    }

    @Override
    public void updateConditionFromPart() {
        // do nothing
    }

    //This can only be found in the warehouse
    @Override
    public int getLocation() {
        return -1;
    }

    @Override
    public String checkFixable() {
        if (partType.getMissingPart().isReplacementAvailable()) {
            return null;
        }
        return "No equipment available to install";
    }

    //Podding equipment is a Class D (Maintenance) refit, which carries a +2 modifier.
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
    
    //Weight is negligible
    @Override
    public double getTonnage() {
        return 0;
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
    protected void loadFieldsFromXmlNode(Node wn) {
        final String METHOD_NAME = "generateInstanceFromXML(Node)"; //$NON-NLS-1$

        NodeList nl = wn.getChildNodes();

        for (int x=0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("partType")) {
                if (null == wn2.getAttributes().getNamedItem("type")) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            "OmniPod lacks part type attribute."); //$NON-NLS-1$
                } else if (null == wn2.getAttributes().getNamedItem("tonnage")) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            "OmniPod lacks partType tonnage attribute."); //$NON-NLS-1$
                } else {
                    String type = wn2.getAttributes().getNamedItem("type").getTextContent();
                    int tonnage = Integer.parseInt(wn2.getAttributes().getNamedItem("tonnage").getTextContent());
                    if (type.equals("AeroHeatSink")) {
                        int hsType = -1;
                        if (null != wn2.getAttributes().getNamedItem("hsType")) {
                            hsType = Integer.parseInt(wn2.getAttributes().getNamedItem("hsType").getTextContent());
                        }
                        if (hsType != Aero.HEAT_SINGLE && hsType != Aero.HEAT_DOUBLE) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "Aero heatsink OmniPod does not have a legal value for heat sink type; using SINGLE"); //$NON-NLS-1$
                            hsType = Aero.HEAT_SINGLE;
                        }
                        partType = new AeroHeatSink(0, hsType, false, campaign);
                    } else {
                        EquipmentType et = EquipmentType.get(type);
                        if (null == et) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "Unknown part type " + type + " for OmniPod"); //$NON-NLS-1$
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
                                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                        "OmniPod for MASC lacks engine rating"); //$NON-NLS-1$
                            }
                        } else {
                            partType = new EquipmentPart(tonnage, et, -1, false, campaign);
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
        //do nothing
    }

    @Override
    public void remove(boolean salvage) {
        //do nothing
    }

    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return true;
    }
    
    @Override
    public void fix() {
        Part newPart = partType.clone();
        Part oldPart = campaign.checkForExistingSparePart(newPart.clone());
        if(null != oldPart) {
            newPart.setOmniPodded(true);
            campaign.addPart(newPart, 0);
            oldPart.decrementQuantity();
        }
    }
    
    
    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        if(skillMin > SkillType.EXP_ELITE) {
            return " <font color='red'><b> failed and part destroyed.</b></font>";
        } else {
            //OmniPod is only added back to warehouse if repair fails without destroying part. 
            campaign.addPart(this, 0);
            return " <font color='red'><b> failed.</b></font>";
        }
    }
    
    @Override
    public String getStatus() {
        String toReturn = "Empty";
        if(isReservedForRefit()) {
            toReturn = "Reserved for Refit";
        }
        if(isReservedForReplacement()) {
            toReturn = "Reserved for Repair";
        }
        if(isBeingWorkedOn()) {
            toReturn = "Being worked on";
        }
        if(!isPresent()) {
            //toReturn = "" + getDaysToArrival() + " days to arrival";
            String dayName = "day";
            if(getDaysToArrival() > 1) {
                dayName += "s";
            }
            toReturn = "In transit (" + getDaysToArrival() + " " + dayName + ")";
        }
        return toReturn;
    }

    @Override
    public long getStickerPrice() {
        return (long)Math.ceil(partType.getStickerPrice() / 5.0);
    }

    @Override
    public int getTechLevel() {
        if (partType.isClanTechBase()) {
            return TechConstants.T_CLAN_TW;
        }
        return TechConstants.T_IS_TW_ALL;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof OmniPod
                && (partType.isSamePartType(((OmniPod)part).partType));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        final String METHOD_NAME = "writeToXml(PrintWriter,int)"; //$NON-NLS-1$
        
        writeToXmlBegin(pw1, indent);
        pw1.print(MekHqXmlUtil.indentStr(indent + 1) + "<partType tonnage='" + partType.getUnitTonnage()
            + "' type='");
        if (partType instanceof AeroHeatSink) {
            pw1.print("AeroHeatSink' hsType='" + ((AeroHeatSink)partType).getType());
        } else if (partType instanceof EquipmentPart) {
            pw1.print(((EquipmentPart)partType).getType().getInternalName());
            if (partType instanceof MASC) {
                pw1.print("' rating='" + ((MASC)partType).getEngineRating());
            }
        } else {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "OmniPod partType is not EquipmentType"); //$NON-NLS-1$
        }
        pw1.println("'/>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public Part clone() {
        Part p = new OmniPod(partType, campaign);
        p.copyBaseData(this);
        return p;
    }

}
