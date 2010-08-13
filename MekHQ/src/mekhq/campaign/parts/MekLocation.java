/*
 * Location.java
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
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.LocationReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLocation extends Part {
	private static final long serialVersionUID = -122291037522319765L;
	protected int loc;
    protected int structureType;
    protected boolean tsm;

    public MekLocation() {
    	this(false, 0, 0, 0, false);
    	reCalc();
    }
    
	@Override
    public void reCalc() {
    	// Do nothing.
    }
    
    public int getLoc() {
        return loc;
    }

    public boolean isTsm() {
        return tsm;
    }

    public int getStructureType() {
        return structureType;
    }
    
    public MekLocation(boolean salvage, int loc, int tonnage, int structureType, boolean hasTSM) {
        super(salvage, tonnage);
        this.loc = loc;
        this.structureType = structureType;
        this.tsm = hasTSM;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
        switch(loc) {
            case(Mech.LOC_HEAD):
                this.name = "Mech Head";
                break;
            case(Mech.LOC_CT):
                this.name = "Mech Center Torso";
                break;
            case(Mech.LOC_LT):
                this.name = "Mech Left Torso";
                break;
            case(Mech.LOC_RT):
                this.name = "Mech Right Torso";
                break;
            case(Mech.LOC_LARM):
                this.name = "Mech Left Arm";
                break;
            case(Mech.LOC_RARM):
                this.name = "Mech Right Arm";
                break;
            case(Mech.LOC_LLEG):
                this.name = "Mech Left Leg";
                break;
            case(Mech.LOC_RLEG):
                this.name = "Mech Right Leg";
                break;
        }
        if(structureType != EquipmentType.T_STRUCTURE_STANDARD) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType) + ")";
        }
        if(tsm) {
            this.name += " (TSM)";
        }
        this.name += " (" + tonnage + ")";
        computeCost();
    }
    
    private void computeCost () {
        double totalStructureCost = EquipmentType.getStructureCost(getStructureType()) * getTonnage();
        int muscCost = isTsm() ? 16000 : 2000;
        double totalMuscleCost = muscCost * getTonnage();
        double cost = 0.1 * (totalStructureCost + totalMuscleCost);

        if (loc == Mech.LOC_HEAD) {
            // Add cockpit cost
            // TODO create a class for cockpit or memorize cockpit type
            cost += 200000;
        }

        this.cost = (long) Math.round(cost);
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof LocationReplacement 
                && ((LocationReplacement)task).getUnit().getEntity() instanceof Mech
                && ((LocationReplacement)task).getLoc() == loc
                && ((LocationReplacement)task).getUnit().getEntity().getWeight() == tonnage
                && ((LocationReplacement)task).getUnit().hasTSM() == tsm
                && ((LocationReplacement)task).getUnit().getEntity().getStructureType() == structureType;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekLocation
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getLoc() == ((MekLocation)part).getLoc()
                && getTonnage() == ((MekLocation)part).getTonnage()
                && isTsm() == ((MekLocation)part).isTsm()
                && getStructureType() == ((MekLocation) part).getStructureType();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_BODY_PART;
    }

    @Override
    public ArrayList<String> getPotentialSSWNames(int faction) {
        ArrayList<String> sswNames = new ArrayList<String>();

        // The tech base of the part doesn't matter (Clan and IS can use each other's Endo Steel parts)
        // However the tech base of the faction is important : Clans get Endo Steel parts before IS
        String techBase = (Faction.isClanFaction(faction) ? "(CL)" : "(IS)");

        String sswName = getName();

        sswNames.add(techBase + " " + sswName);
        sswNames.add(sswName);

        return sswNames;
    }

    @Override
    public String getSaveString () {
        return getName() + ";" + getTonnage() + ";" + getLoc() + ";" + getStructureType() + ";" + (isTsm()?"true":"false");
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<structureType>"
				+structureType
				+"</structureType>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tsm>"
				+tsm
				+"</tsm>");
		writeToXmlEnd(pw1, indent, id);
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
			} else if (wn2.getNodeName().equalsIgnoreCase("tsm")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					tsm = true;
				else
					tsm = false;
			} 
		}
	}
}
