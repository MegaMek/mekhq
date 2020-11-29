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

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

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
		VeeSensor clone = new VeeSensor(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
		return clone;
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
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setSensorHits(0);
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingVeeSensor(getUnitTonnage(), campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setSensorHits(4);
			Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
			if(!salvage) {
				campaign.getWarehouse().removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.getWarehouse().removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.getQuartermaster().addPart(missing, 0);
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit && unit.getEntity() instanceof Tank) {
			int priorHits = hits;
			hits = ((Tank)unit.getEntity()).getSensorHits();
			if(checkForDestruction
					&& hits > priorHits
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
			}
		}
	}

	@Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return 260;
		}
		return 75;
	}

	@Override
	public int getDifficulty() {
		return 0;
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
	public Money getStickerPrice() {
		// TODO Auto-generated method stub
		return Money.zero();
	}

	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_MECHANIC);
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

    @Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
