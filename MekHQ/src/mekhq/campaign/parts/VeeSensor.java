/*
 * VeeSensor.java
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
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeSensor extends Part {
	private static final long serialVersionUID = 4101969895094531892L;

	public VeeSensor() {
		this(0, null);
	}
	
	public VeeSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Vehicle Sensors";
    }

	public VeeSensor clone() {
		return new VeeSensor(getUnitTonnage(), campaign);
	}
	
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof VeeSensor;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing.
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

    @Override
	public int getTechLevel() {
		return TechConstants.T_INTRO_BOXSET;
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setSensorHits(0);
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingVeeSensor(getUnitTonnage(), campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setSensorHits(4);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing);
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			hits = ((Tank)unit.getEntity()).getSensorHits();
		}
		if(hits > 0) {
			time = 75;
			difficulty = 0;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 260;
			difficulty = 0;
		}
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setSensorHits(hits);
		}
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getStickerPrice() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_MECHANIC);
	}
}
