/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscType;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekLocation extends MissingPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingMekLocation.class);

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

    public MissingMekLocation(int loc, int tonnage, int structureType, boolean clan, boolean hasTSM, boolean quad,
          Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.clan = clan;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
        this.unitTonnageMatters = true;
        // TODO: need to account for internal structure and myomer types
        // crap, no static report for location names?
        this.name = "Mek Location";
        switch (loc) {
            case Mek.LOC_HEAD:
                this.name = "Mek Head";
                break;
            case Mek.LOC_CENTER_TORSO:
                this.name = "Mek Center Torso";
                break;
            case Mek.LOC_LEFT_TORSO:
                this.name = "Mek Left Torso";
                break;
            case Mek.LOC_RIGHT_TORSO:
                this.name = "Mek Right Torso";
                break;
            case Mek.LOC_LEFT_ARM:
                this.name = forQuad ? "Mek Front Left Leg" : "Mek Left Arm";
                break;
            case Mek.LOC_RIGHT_ARM:
                this.name = forQuad ? "Mek Front Right Leg" : "Mek Right Arm";
                break;
            case Mek.LOC_LEFT_LEG:
                this.name = forQuad ? "Mek Rear Left Leg" : "Mek Left Leg";
                break;
            case Mek.LOC_RIGHT_LEG:
                this.name = forQuad ? "Mek Rear Right Leg" : "Mek Right Leg";
                break;
            case Mek.LOC_CENTER_LEG:
                this.name = "Mek Center Leg";
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
        // TODO : how much should this weigh?
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "structureType", structureType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tsm", tsm);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percent", percent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forQuad", forQuad);
        writeToXMLEnd(pw, indent);
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
            } catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }
    }

    private boolean isArm() {
        return loc == Mek.LOC_RIGHT_ARM || loc == Mek.LOC_LEFT_ARM;
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if ((loc == Mek.LOC_CENTER_TORSO) && !refit) {
            // you can't replace a center torso
            return false;
        } else if (part instanceof MekLocation mekLoc) {
            return (mekLoc.getLoc() == loc)
                         && (mekLoc.getUnitTonnage() == getUnitTonnage())
                         && (mekLoc.isTsm() == tsm)
                         && (mekLoc.isClan() == clan)
                         && (mekLoc.getStructureType() == structureType)
                         && (!isArm() || (mekLoc.forQuad() == forQuad));
        } else {
            return false;
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.getEntity() instanceof Mek) {
            // Can't replace appendages when corresponding torso is gone
            if (loc == Mek.LOC_LEFT_ARM
                      && unit.getEntity().isLocationBad(Mek.LOC_LEFT_TORSO)) {
                return "must replace left torso first";
            } else if (loc == Mek.LOC_RIGHT_ARM
                             && unit.getEntity().isLocationBad(Mek.LOC_RIGHT_TORSO)) {
                return "must replace right torso first";
            }
        }

        // There must be no usable equipment currently in the location
        // You can only salvage a location that has nothing left on it
        Set<Integer> equipmentSeen = new HashSet<>();
        StringJoiner partsToSalvageOrScrap = new StringJoiner(", ");
        for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            // certain other specific crits need to be left out (must be a better way
            // to do this!)
            if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                // Skip Hip and Shoulder actuators
                if ((slot.getIndex() == Mek.ACTUATOR_HIP)
                          || (slot.getIndex() == Mek.ACTUATOR_SHOULDER)) {
                    continue;
                }
                if (unit.getEntity() instanceof LandAirMek) {
                    // Skip Landing Gear if already gone
                    if (slot.getIndex() == LandAirMek.LAM_LANDING_GEAR) {
                        if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                            continue;
                        } else {
                            partsToSalvageOrScrap
                                  .add(String.format("Landing Gear (%s)", unit.getEntity().getLocationName(loc)));
                        }
                        // Skip Avionics if already gone
                    } else if (slot.getIndex() == LandAirMek.LAM_AVIONICS) {
                        if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                            continue;
                        } else {
                            partsToSalvageOrScrap
                                  .add(String.format("Avionics (%s)", unit.getEntity().getLocationName(loc)));
                        }
                    }
                }
            } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if ((slot.getMount() != null) &&
                          (!slot.getMount().isDestroyed()) &&
                          (slot.getMount().getType() instanceof EquipmentType)) {
                    EquipmentType equipmentType = slot.getMount().getType();
                    if (equipmentType.hasFlag(MiscType.F_NULL_SIG)) {
                        partsToSalvageOrScrap.add("Null-Signature System");
                    } else if (equipmentType.hasFlag(MiscType.F_VOID_SIG)) {
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

                    Part repairablePart = unit.findPart(p -> (p instanceof EquipmentPart)
                                                                   &&
                                                                   (((EquipmentPart) p).getEquipmentNum() ==
                                                                          equipmentNum));
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
        /*
         * int cockpitType = -1;
         * if (null != unit) {
         * cockpitType = ((Mek) unit.getEntity()).getCockpitType();
         * }
         */
        boolean lifeSupport = (loc == Mek.LOC_HEAD);
        boolean sensors = (loc == Mek.LOC_HEAD);
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
            campaign.getQuartermaster().addPart(actualReplacement, 0, false);
            replacement.changeQuantity(-1);
            // TODO : if this is a mek head, check to see if it had components
            if ((loc == Mek.LOC_HEAD) && (actualReplacement instanceof MekLocation)) {
                updateHeadComponents((MekLocation) actualReplacement);
                ((MekLocation) actualReplacement).setSensors(false);
                ((MekLocation) actualReplacement).setLifeSupport(false);
            }
            // fix shoulders and hips
            if (loc == Mek.LOC_RIGHT_ARM || loc == Mek.LOC_LEFT_ARM) {
                if (forQuad) {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc);
                } else {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER, loc);
                }
            } else if ((loc == Mek.LOC_RIGHT_LEG) || (loc == Mek.LOC_LEFT_LEG) || (loc == Mek.LOC_CENTER_LEG)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc);
            }
            remove(false);
            actualReplacement.updateConditionFromPart();
        }
    }

    private void updateHeadComponents(MekLocation part) {
        MissingMekSensor missingSensor = null;
        MissingMekLifeSupport missingLifeSupport = null;
        for (Part p : unit.getParts()) {
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
        if (part.hasSensors() && null != missingSensor) {
            newPart = missingSensor.getNewPart();
            unit.addPart(newPart);
            campaign.getQuartermaster().addPart(newPart, 0, false);
            missingSensor.remove(false);
            newPart.updateConditionFromPart();
        }
        /*
         * if (part.hasCockpit() && null != missingCockpit) {
         * newPart = missingCockpit.getNewPart();
         * unit.addPart(newPart);
         * campaign.getQuartermaster().addPart(newPart);
         * missingCockpit.remove(false);
         * newPart.updateConditionFromPart();
         * }
         */
        if (part.hasLifeSupport() && null != missingLifeSupport) {
            newPart = missingLifeSupport.getNewPart();
            unit.addPart(newPart);
            campaign.getQuartermaster().addPart(newPart, 0, false);
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
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.MEK_LOCATION;
    }
}
