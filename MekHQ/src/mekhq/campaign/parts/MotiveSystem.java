/*
 * MotiveSystem.java
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

import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MotiveSystem extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5637743997294510810L;

	int damage;
	int penalty;
	
	public MotiveSystem() {
		this(0, null);
	}
	
	public MotiveSystem(int ton, Campaign c) {
		super(ton, c);
		this.name = "Motive System";
		this.damage = 0;
		this.penalty = 0;
		this.time = 60;
		this.difficulty = -1;
	}
	
	public MotiveSystem clone() {
		MotiveSystem clone = new MotiveSystem(getUnitTonnage(), campaign);
        clone.copyRepairData(this);
        return clone;
	}
	
	@Override
	public int getAvailability(int era) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getStickerPrice() {
		// TODO Auto-generated method stub
		return 0;
	}
	
    @Override
	public int getTechLevel() {
		return TechConstants.T_INTRO_BOXSET;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof MotiveSystem;
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("damage")) {
				damage = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("penalty")) {
				penalty = Integer.parseInt(wn2.getTextContent());
			} 
		}
		
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<damage>"
				+damage
				+"</damage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<penalty>"
				+penalty
				+"</penalty>");
		writeToXmlEnd(pw1, indent);
		
	}

	@Override
	public String checkFixable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fix() {
		super.fix();
		damage = 0;
		penalty = 0;
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).resetMovementDamage();
		}
	}

	@Override
	public Part getMissingPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		// you can't do this so nothing here
		
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			Tank t = (Tank)unit.getEntity();
			damage = t.getMotiveDamage();
			penalty = t.getMotivePenalty();
		}
	}

	@Override
	public void updateConditionFromPart() {
		// TODO Auto-generated method stub
		//you can't get here so, dont worry about it
	}

	@Override
	public boolean needsFixing() {
		return damage > 0 || penalty > 0;
	}
	
	@Override
    public String getDetails() {
        return "-" + damage + " MP/-" + penalty + " Piloting";
    }
	
	@Override
	public boolean canScrap() {
		return false;
	}

	@Override
	public boolean isSalvaging() {
		return false;
	}
	
}