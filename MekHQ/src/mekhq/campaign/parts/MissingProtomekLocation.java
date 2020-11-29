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

import megamek.common.CriticalSlot;
import megamek.common.IArmorState;
import megamek.common.Protomech;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingProtomekLocation extends MissingPart {
    private static final long serialVersionUID = -122291037522319765L;
    protected int loc;
    protected int structureType;
    protected boolean booster;
    protected double percent;
    protected boolean forQuad;

    public MissingProtomekLocation() {
        this(0, 0, 0, false, false, null);
    }


    public MissingProtomekLocation(int loc, int tonnage, int structureType, boolean hasBooster, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.booster = hasBooster;
        this.percent = 1.0;
        this.forQuad = quad;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        switch(loc) {
        case(Protomech.LOC_HEAD):
            this.name = "Protomech Head";
            break;
        case(Protomech.LOC_TORSO):
            this.name = "Protomech Torso";
            break;
        case(Protomech.LOC_LARM):
            this.name = "Protomech Left Arm";
            break;
        case(Protomech.LOC_RARM):
            this.name = "Protomech Right Arm";
            break;
        case(Protomech.LOC_LEG):
            this.name = "Protomech Legs";
            if(forQuad) {
                this.name = "Protomech Legs (Quad)";
            }
            break;
        case(Protomech.LOC_MAINGUN):
            this.name = "Protomech Main Gun";
            break;
        default:
            this.name = "Protomech Location";
            break;
        }
        if(booster) {
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
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<booster>"
                +booster
                +"</booster>");
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

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                loc = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("structureType")) {
                structureType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
                percent = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("booster")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    booster = true;
                else
                    booster = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    forQuad = true;
                else
                    forQuad = false;
            }
        }
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if(loc == Protomech.LOC_TORSO && !refit) {
            //you can't replace a center torso
            return false;
        }
        if(part instanceof ProtomekLocation) {
            ProtomekLocation mekLoc = (ProtomekLocation)part;
            return mekLoc.getLoc() == loc
                && mekLoc.getUnitTonnage() == getUnitTonnage()
                && mekLoc.hasBooster() == booster
                && (!isLeg() || mekLoc.forQuad() == forQuad);
                //&& mekLoc.getStructureType() == structureType;
        }
        return false;
    }

    private boolean isLeg() {
        return loc == Protomech.LOC_LEG;
    }

    @Override
    public String checkFixable() {
    	if(null == unit) {
			 return null;
		 }
        //there must be no usable equipment currently in the location
        //you can only salvage a location that has nothing left on it
        for (Part part : unit.getParts()) {
            if ((part.getLocation() == getLocation())
                    && !(part instanceof MissingPart)
                    && (!(part instanceof ProtomekArmor) || ((ProtomekArmor) part).getAmount() > 0)) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first. They can then be re-installed.";
            }
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new ProtomekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
    }

    private int getAppropriateSystemIndex() {
    	switch(loc) {
    	case(Protomech.LOC_LEG):
    		return Protomech.SYSTEM_LEGCRIT;
    	case(Protomech.LOC_LARM):
    	case(Protomech.LOC_RARM):
    		return Protomech.SYSTEM_ARMCRIT;
    	case(Protomech.LOC_HEAD):
    		return Protomech.SYSTEM_HEADCRIT;
    	case(Protomech.LOC_TORSO):
    		return Protomech.SYSTEM_TORSOCRIT;
    	default:
    		return -1;
    	}
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            //need to assign all possible crits to the appropriate system
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, getAppropriateSystemIndex(), loc);
        }
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
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
        return ProtomekLocation.TECH_ADVANCEMENT;
    }

    @Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.GENERAL_LOCATION;
    }

	@Override
	public int getRepairPartType() {
    	return Part.REPAIR_PART_TYPE.MEK_LOCATION;
    }
}
