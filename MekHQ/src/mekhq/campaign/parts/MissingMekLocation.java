/*
 * MissingMekLocation.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.TechAdvancement;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekLocation extends MissingPart {
    private static final long serialVersionUID = -122291037522319765L;
    protected int loc;
    protected int structureType;
    protected boolean clan; // Needed for Endo-steel
    protected boolean tsm;
    protected double percent;
    protected boolean forQuad;

    public MissingMekLocation() {
        this(0, 0, 0, false, false, false, null);
    }

    public boolean isTsm() {
        return tsm;
    }

    public int getStructureType() {
        return structureType;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public MissingMekLocation(int loc, int tonnage, int structureType, boolean clan, boolean hasTSM, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.clan = clan;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
        switch (loc) {
            case Mech.LOC_HEAD:
                this.name = "Mech Head";
                break;
            case Mech.LOC_CT:
                this.name = "Mech Center Torso";
                break;
            case Mech.LOC_LT:
                this.name = "Mech Left Torso";
                break;
            case Mech.LOC_RT:
                this.name = "Mech Right Torso";
                break;
            case(Mech.LOC_LARM):
                this.name = "Mech Left Arm";
                if (forQuad) {
                    this.name = "Mech Front Left Leg";
                }
                break;
            case Mech.LOC_RARM:
                this.name = "Mech Right Arm";
                if (forQuad) {
                    this.name = "Mech Front Left Leg";
                }
                break;
            case Mech.LOC_LLEG:
                this.name = "Mech Left Leg";
                if (forQuad) {
                    this.name = "Mech Rear Left Leg";
                }
                break;
            case Mech.LOC_RLEG:
                this.name = "Mech Right Leg";
                if (forQuad) {
                    this.name = "Mech Rear Right Leg";
                }
                break;
        }

        if (EquipmentType.T_STRUCTURE_ENDO_STEEL == structureType) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType, clan) + ")";
        } else if (structureType != EquipmentType.T_STRUCTURE_STANDARD) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType) + ")";
        }

        if (tsm) {
            this.name += " (TSM)";
        }
    }

    @Override
    public int getBaseTime() {
        return 240;
    }

    @Override
    public int getDifficulty() {
        return 3;
    }

    @Override
    public double getTonnage() {
        //TODO: how much should this weigh?
        return 0;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<loc>"
                +loc
                +"</loc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<structureType>"
                +structureType
                +"</structureType>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "clan", clan);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<tsm>"
                +tsm
                +"</tsm>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<percent>"
                +percent
                +"</percent>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<forQuad>"
                +forQuad
                +"</forQuad>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                    loc = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("structureType")) {
                    structureType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                    clan = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
                    percent = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("tsm")) {
                    tsm = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
                    forQuad = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }

    private boolean isArm() {
        return loc == Mech.LOC_RARM || loc == Mech.LOC_LARM;
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if ((loc == Mech.LOC_CT) && !refit) {
            //you can't replace a center torso
            return false;
        } else if (part instanceof MekLocation) {
            MekLocation mekLoc = (MekLocation) part;
            return (mekLoc.getLoc() == loc)
                    && (mekLoc.getUnitTonnage() == getUnitTonnage())
                    && (mekLoc.isTsm() == tsm)
                    && (mekLoc.clan == clan)
                    && (mekLoc.getStructureType() == structureType)
                    && (!isArm() || (mekLoc.forQuad() == forQuad));
        } else {
            return false;
        }
    }

    @Override
    public String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.getEntity() instanceof Mech) {
            // cant replace appendages when corresponding torso is gone
            if (loc == Mech.LOC_LARM
                    && unit.getEntity().isLocationBad(Mech.LOC_LT)) {
                return "must replace left torso first";
            } else if (loc == Mech.LOC_RARM
                    && unit.getEntity().isLocationBad(Mech.LOC_RT)) {
                return "must replace right torso first";
            }
        }

        //there must be no usable equipment currently in the location
        //you can only salvage a location that has nothing left on it
        Set<Integer> equipmentSeen = new HashSet<>();
        StringJoiner partsToSalvageOrScrap = new StringJoiner(", ");
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                // Skip Hip and Shoulder actuators
                if ((slot.getIndex() == Mech.ACTUATOR_HIP)
                        || (slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                    continue;
                }
                if (unit.getEntity() instanceof LandAirMech) {
                    // Skip Landing Gear if already gone
                    if (slot.getIndex() == LandAirMech.LAM_LANDING_GEAR) {
                        if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                            continue;
                        } else {
                            partsToSalvageOrScrap.add(String.format("Landing Gear (%s)", unit.getEntity().getLocationName(loc)));
                        }
                    // Skip Avionics if already gone
                    } else if (slot.getIndex() == LandAirMech.LAM_AVIONICS) {
                        if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                            continue;
                        } else {
                            partsToSalvageOrScrap.add(String.format("Avionics (%s)", unit.getEntity().getLocationName(loc)));
                        }
                    }
                }
            } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if ((slot.getMount() != null) && !slot.getMount().isDestroyed()) {
                    EquipmentType equipmentType = slot.getMount().getType();
                    if (equipmentType.hasFlag(MiscType.F_NULLSIG)) {
                        partsToSalvageOrScrap.add("Null-Signature System");
                    } else if (equipmentType.hasFlag(MiscType.F_VOIDSIG)) {
                        partsToSalvageOrScrap.add("Void-Signature System");
                    } else if (equipmentType.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                        partsToSalvageOrScrap.add("Chameleon Shield");
                    }
                }
            }

            if (slot.isRepairable()) {
                String partName = "Repairable Part";

                // Try to get a more specific name
                int equipmentNum = unit.getEntity().getEquipmentNum(slot.getMount());
                if (equipmentNum >= 0) {
                    if (!equipmentSeen.add(equipmentNum)) {
                        // We have already marked this part as needing to be salvaged/scrapped
                        continue;
                    }

                    Part repairablePart = unit.findPart(p -> (p instanceof EquipmentPart) && (((EquipmentPart) p).getEquipmentNum() == equipmentNum));
                    if (repairablePart != null) {
                        partName = repairablePart.getName();
                    }
                }

                partsToSalvageOrScrap.add(String.format("%s (%s)", partName, unit.getEntity().getLocationName(loc)));
            }
        }

        if (partsToSalvageOrScrap.length() == 0) {
            return null;
        }

        return "The following parts must be salvaged or scrapped first: " + partsToSalvageOrScrap;
    }

    @Override
    public Part getNewPart() {
       /* int cockpitType = -1;
        if (null != unit) {
            cockpitType = ((Mech) unit.getEntity()).getCockpitType();
        }*/
        boolean lifeSupport = (loc == Mech.LOC_HEAD);
        boolean sensors = (loc == Mech.LOC_HEAD);
        return new MekLocation(loc, getUnitTonnage(), structureType, clan,
                tsm, forQuad, sensors, lifeSupport, campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
        }
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            //TODO: if this is a mech head, check to see if it had components
            if ((loc == Mech.LOC_HEAD) && (actualReplacement instanceof MekLocation)) {
                updateHeadComponents((MekLocation) actualReplacement);
                ((MekLocation) actualReplacement).setSensors(false);
                ((MekLocation) actualReplacement).setLifeSupport(false);
            }
            //fix shoulders and hips
            if (loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
                if (forQuad) {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
                } else {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc);
                }
            } else if (loc == Mech.LOC_RLEG || loc == Mech.LOC_LLEG) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
            }
            remove(false);
            actualReplacement.updateConditionFromPart();
        }
    }

    private void updateHeadComponents(MekLocation part) {
        MissingMekSensor missingSensor = null;
        MissingMekLifeSupport missingLifeSupport = null;
        for(Part p : unit.getParts()) {
            if (null == missingSensor && p instanceof MissingMekSensor) {
                missingSensor = (MissingMekSensor) p;
            }
            if (null == missingLifeSupport && p instanceof MissingMekLifeSupport) {
                missingLifeSupport = (MissingMekLifeSupport) p;
            }
            if ((null != missingSensor) && (null != missingLifeSupport)) {
                break;
            }
        }
        Part newPart;
        if(part.hasSensors() && null != missingSensor) {
            newPart = missingSensor.getNewPart();
            unit.addPart(newPart);
            campaign.getQuartermaster().addPart(newPart, 0);
            missingSensor.remove(false);
            newPart.updateConditionFromPart();
        }
        /*if (part.hasCockpit() && null != missingCockpit) {
            newPart = missingCockpit.getNewPart();
            unit.addPart(newPart);
            campaign.getQuartermaster().addPart(newPart);
            missingCockpit.remove(false);
            newPart.updateConditionFromPart();
        }*/
        if (part.hasLifeSupport() && null != missingLifeSupport) {
            newPart = missingLifeSupport.getNewPart();
            unit.addPart(newPart);
            campaign.getQuartermaster().addPart(newPart, 0);
            missingLifeSupport.remove(false);
            newPart.updateConditionFromPart();
        }
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(loc) : null;
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EquipmentType.getStructureTechAdvancement(structureType, clan);
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.MEK_LOCATION;
    }
}
