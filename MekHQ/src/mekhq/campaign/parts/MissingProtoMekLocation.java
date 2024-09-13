/*
 * MissingMekLocation.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.IArmorState;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingProtoMekLocation extends MissingPart {
    protected int loc;
    protected int structureType;
    protected boolean booster;
    protected double percent;
    protected boolean forQuad;

    public MissingProtoMekLocation() {
        this(0, 0, 0, false, false, null);
    }


    public MissingProtoMekLocation(int loc, int tonnage, int structureType, boolean hasBooster, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.booster = hasBooster;
        this.percent = 1.0;
        this.forQuad = quad;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        switch (loc) {
            case ProtoMek.LOC_HEAD:
                this.name = "ProtoMek Head";
                break;
                case ProtoMek.LOC_TORSO:
                this.name = "ProtoMek Torso";
                break;
            case ProtoMek.LOC_LARM:
                this.name = "ProtoMek Left Arm";
                break;
            case ProtoMek.LOC_RARM:
                this.name = "ProtoMek Right Arm";
                break;
            case ProtoMek.LOC_LEG:
                this.name = "ProtoMek Legs";
                if (forQuad) {
                    this.name = "ProtoMek Legs (Quad)";
                }
                break;
            case ProtoMek.LOC_MAINGUN:
                this.name = "ProtoMek Main Gun";
                break;
            default:
                this.name = "ProtoMek Location";
                break;
        }
        if (booster) {
            this.name += " (Myomer Booster)";
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

    public int getLoc() {
        return loc;
    }

    public boolean hasBooster() {
        return booster;
    }

    public int getStructureType() {
        return structureType;
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
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "booster", booster);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percent", percent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forQuad", forQuad);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                loc = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("structureType")) {
                structureType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
                percent = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("booster")) {
                booster = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
                forQuad = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (loc == ProtoMek.LOC_TORSO && !refit) {
            //you can't replace a center torso
            return false;
        }
        if (part instanceof ProtoMekLocation) {
            ProtoMekLocation mekLoc = (ProtoMekLocation) part;
            return mekLoc.getLoc() == loc
                && mekLoc.getUnitTonnage() == getUnitTonnage()
                && mekLoc.hasBooster() == booster
                && (!isLeg() || mekLoc.forQuad() == forQuad);
                //&& mekLoc.getStructureType() == structureType;
        }
        return false;
    }

    private boolean isLeg() {
        return loc == ProtoMek.LOC_LEG;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
             return null;
         }
        //there must be no usable equipment currently in the location
        //you can only salvage a location that has nothing left on it
        for (Part part : unit.getParts()) {
            if ((part.getLocation() == getLocation())
                    && !(part instanceof MissingPart)
                    && (!(part instanceof ProtoMekArmor) || ((ProtoMekArmor) part).getAmount() > 0)) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first. They can then be re-installed.";
            }
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new ProtoMekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
    }

    private int getAppropriateSystemIndex() {
        switch (loc) {
            case ProtoMek.LOC_LEG:
                return ProtoMek.SYSTEM_LEGCRIT;
            case ProtoMek.LOC_LARM:
            case ProtoMek.LOC_RARM:
                return ProtoMek.SYSTEM_ARMCRIT;
            case ProtoMek.LOC_HEAD:
                return ProtoMek.SYSTEM_HEADCRIT;
            case ProtoMek.LOC_TORSO:
                return ProtoMek.SYSTEM_TORSOCRIT;
            default:
                return -1;
        }
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            //need to assign all possible crits to the appropriate system
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, getAppropriateSystemIndex(), loc);
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
            remove(false);
            actualReplacement.updateConditionFromPart();
        }
    }


    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekLocation.TECH_ADVANCEMENT;
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
