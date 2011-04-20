/*
 * MekActuator.java
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

import megamek.common.BipedMech;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.MekActuatorReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuator extends Part {
	private static final long serialVersionUID = 719878556021696393L;
	protected int type;

	public MekActuator() {
		this(false, 0, 0);
		reCalc();
	}
	
	@Override
	public void reCalc() {
		// Do nothing.
	}
	
    public int getType() {
        return type;
    }
    
    public MekActuator(boolean salvage, int tonnage, int type) {
        super(salvage, tonnage);
        this.type = type;
        Mech m = new BipedMech();
        this.name = m.getSystemName(type) + " Actuator (" + tonnage + " tons)" ;
        computeCost();
    }

    private void computeCost () {
        long unitCost = 0;
        switch (getType()) {
            case (Mech.ACTUATOR_UPPER_ARM) : {
                unitCost = 100;
                break;
            }
            case (Mech.ACTUATOR_LOWER_ARM) : {
                unitCost = 50;
                break;
            }
            case (Mech.ACTUATOR_HAND) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_UPPER_LEG) : {
                unitCost = 150;
                break;
            }
            case (Mech.ACTUATOR_LOWER_LEG) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_FOOT) : {
                unitCost = 120;
                break;
            }
            case (Mech.ACTUATOR_HIP) : {
                // not used
                unitCost = 0;
                break;
            }
            case (Mech.ACTUATOR_SHOULDER) : {
                // not used
                unitCost = 0;
                break;
            }
        }
        this.cost = getTonnage() * unitCost;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof MekActuatorReplacement 
                && tonnage == ((MekActuatorReplacement)task).getUnit().getEntity().getWeight()
                && type == ((MekActuatorReplacement)task).getType();
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekActuator
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((MekActuator)part).getType()
                && getTonnage() == ((MekActuator)part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_ACTUATOR;
    }

    @Override
    public String getSaveString () {
        return getName() + ";" + getTonnage() + ";" + getType();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}
}
