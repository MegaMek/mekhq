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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import mekhq.campaign.MekHqXmlUtil;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAmmoBin extends MissingEquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	protected boolean oneShot;
	
    public MissingAmmoBin() {
    	this(0, null, -1, false);
    }
    
    public MissingAmmoBin(int tonnage, EquipmentType et, int equipNum, boolean singleShot) {
        super(tonnage, et, equipNum);
        this.oneShot = singleShot;
        this.difficulty = -2;
        if(null != name) {
        	this.name += " Bin";
        }
    }
	
	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if(part instanceof AmmoBin) {
			EquipmentPart eqpart = (EquipmentPart)part;
			EquipmentType et = eqpart.getType();
			return type.equals(et) && ((AmmoBin)part).isOneShot() == oneShot;
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
		return new AmmoBin(getUnitTonnage(), type, -1, getFullShots(), oneShot);
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+typeName
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<checkedToday>"
				+checkedToday
				+"</checkedToday>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<oneShot>"
				+oneShot
				+"</oneShot>");
		writeToXmlEnd(pw1, indent, id);
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
			} else if (wn2.getNodeName().equalsIgnoreCase("checkedToday")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					checkedToday = true;
				} else {
					checkedToday = false;
				}
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
}
