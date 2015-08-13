/*
 * Turret.java
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

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Tank;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingTurret extends MissingPart {
	private static final long serialVersionUID = 719267861685599789L;

	double weight;
	
	public MissingTurret() {
		this(0,0, null);
	}
	
	public MissingTurret(int tonnage, double weight, Campaign c) {
        super(tonnage, c);
        this.weight = weight;
        this.name = "Turret";
    }
	
	@Override 
	public int getBaseTime() {
		return 160;
	}
	
	@Override
	public int getDifficulty() {
		return -1;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<weight>"
				+weight
				+"</weight>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("weight")) {
				weight = Double.parseDouble(wn2.getTextContent());
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}


	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof Turret 
			&& (((TankLocation)part).getLoc() == Tank.LOC_TURRET || ((TankLocation)part).getLoc() == Tank.LOC_TURRET_2);
	}
	
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		//TODO: how to get second turret location?
		return new Turret(Tank.LOC_TURRET, getUnitTonnage(), weight, campaign);
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, Tank.LOC_TURRET);
		}
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public int getIntroDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}
	
}
