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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

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
	}
	
	@Override 
	public int getBaseTime() {
		return 60;
	}
	
	@Override
	public int getDifficulty() {
		return -1;
	}
	
	public MotiveSystem clone() {
		MotiveSystem clone = new MotiveSystem(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
	}
	
	@Override
	public int getBaseAvailability(int era) {
		return RATING_B;
	}

	@Override
	public long getStickerPrice() {
		// TODO Auto-generated method stub
		return 0;
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
	public MissingPart getMissingPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		// you can't do this so nothing here
		
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		//motive systems don't have to check for destruction since they 
		//cannot be removed
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
	public String checkScrappable() {
		return "Motive type cannot be scrapped";
	}
	
	@Override
	public boolean canNeverScrap() {
		return true;
	}

	@Override
	public boolean isSalvaging() {
		return false;
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
	public TechAdvancement getTechAdvancement() {
	    return TankLocation.TECH_ADVANCEMENT;
	}
	
}