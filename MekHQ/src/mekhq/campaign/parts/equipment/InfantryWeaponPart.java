/*
 * InfantryWeapon.java
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

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.parts.MissingPart;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InfantryWeaponPart extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	private boolean primary;
	
	public InfantryWeaponPart() {
    	this(0, null, -1, null, false);
    }
    
    public InfantryWeaponPart(int tonnage, EquipmentType et, int equipNum, Campaign c, boolean p) {
        super(tonnage, et, equipNum, c);
        primary = p;
    }
    
    @Override
    public InfantryWeaponPart clone() {
    	InfantryWeaponPart clone = new InfantryWeaponPart(getUnitTonnage(), getType(), getEquipmentNum(), campaign, primary);
        clone.copyBaseData(this);
    	return clone;
    }

	@Override
	public MissingPart getMissingPart() {
		//shouldn't get here, but ok
		return new MissingEquipmentPart(getUnitTonnage(), type, equipmentNum, campaign, getTonnage());
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
				+MekHqXmlUtil.escape(type.getInternalName())
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipTonnage>"
				+equipTonnage
				+"</equipTonnage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<primary>"
				+primary
				+"</primary>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			}
			else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
				equipTonnage = Double.parseDouble(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("primary")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					primary = true;
				} else {
					primary = false;
				}
			}
		}
		restore();
	}
	
	public boolean isPrimary() {
		return primary;
	}
}