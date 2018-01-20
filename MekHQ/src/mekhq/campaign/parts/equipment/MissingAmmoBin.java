/*
 * MissingAmmoBin.java
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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAmmoBin extends MissingEquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	protected boolean oneShot;

    public MissingAmmoBin() {
    	this(0, null, -1, false, false, null);
    }

    public MissingAmmoBin(int tonnage, EquipmentType et, int equipNum, boolean singleShot,
            boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, c, 1, omniPodded);
        this.oneShot = singleShot;
        if(null != name) {
        	this.name += " Bin";
        }
    }

    /* Per TM, ammo for fighters is stored in the fuselage. This makes a difference for omnifighter
     * pod space, so we're going to stick them in LOC_NONE where the heat sinks are */
    @Override
    public String getLocationName() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return "Fuselage";
        }
        return super.getLocationName();
    }

    @Override
    public int getLocation() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return Aero.LOC_NONE;
        }
        return super.getLocation();
    }

    @Override
    public boolean isInLocation(String loc) {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return loc.equals("FSLG");
        }
        return super.isInLocation(loc);
    }

    @Override
	public int getDifficulty() {
		return -2;
	}

	@Override
	public void fix() {
		Part replacement = findReplacement(false);
		if(null != replacement) {
		    Part actualReplacement;

            //Check to see if munition types are different
		    if (getType() == ((AmmoBin)replacement).getType()) {
		        actualReplacement = replacement.clone();
		    } else {
		        actualReplacement = new AmmoBin(getUnitTonnage(), getType(), getEquipmentNum(),
		                getFullShots(), isOneShot(), isOmniPodded(), campaign);
		    }

			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement, 0);
			replacement.decrementQuantity();
			((EquipmentPart)actualReplacement).setEquipmentNum(equipmentNum);
			remove(false);
			//assign the replacement part to the unit
			actualReplacement.updateConditionFromPart();
		}
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if ((part instanceof AmmoBin)
		        && !(part instanceof LargeCraftAmmoBin)) {
			EquipmentPart eqpart = (EquipmentPart)part;
			EquipmentType et = eqpart.getType();
			return type.equals(et) && ((AmmoBin)part).getFullShots() == getFullShots();
		}
		return false;
	}

	public boolean isOneShot() {
		return oneShot;
	}

	private int getFullShots() {
    	int fullShots = ((AmmoType)type).getShots();
		if(oneShot) {
			fullShots = 1;
		}
		return fullShots;
    }

	@Override
	public Part getNewPart() {
		return new AmmoBin(getUnitTonnage(), type, -1, getFullShots(), oneShot, omniPodded, campaign);
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+MekHqXmlUtil.escape(typeName)
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysToWait>"
				+daysToWait
				+"</daysToWait>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<oneShot>"
				+oneShot
				+"</oneShot>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		super.loadFieldsFromXmlNode(wn);
		NodeList nl = wn.getChildNodes();

		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                daysToWait = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("oneShot")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					oneShot = true;
				} else {
					oneShot = false;
				}
			}
		}
		restore();
	}

    @Override
    public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.AMMO;
    }
}
