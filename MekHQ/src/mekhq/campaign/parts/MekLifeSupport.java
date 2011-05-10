/*
 * MekLifeSupport.java
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

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.work.MekLifeSupportRepair;
import mekhq.campaign.work.MekLifeSupportReplacement;
import mekhq.campaign.work.MekLifeSupportSalvage;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLifeSupport extends Part {
	private static final long serialVersionUID = -1989526319692474127L;

	public MekLifeSupport() {
		this(false, 0);
	}
	
	public MekLifeSupport(boolean salvage, int tonnage) {
        super(salvage, tonnage);
        this.name = "Mech Life Support System";
        this.cost = 50000;
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof MekLifeSupportReplacement;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekLifeSupport
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus());
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_LIFE_SUPPORT;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing - no fields to load.
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
	public void fix() {
		hits = 0;
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
		}
	}

	@Override
	public Part getMissingPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			//TODO create replacement part and add it to entity
		}
		unit.removePart(this);
		unit = null;	
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0) {
					if (entity.isSystemRepairable(Mech.SYSTEM_LIFE_SUPPORT, i)) {					
						hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i);	
						break;
					} else {
						remove(false);
						return;
					}
				}
			}
			if(hits == 0) {
				time = 0;
				difficulty = 0;
			} 
			else if(hits == 1) {
				time = 60;
				difficulty = -1;
			}
			else if(hits > 1) {
				time = 120;
				difficulty = 1;
			}
		}
		
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(hits == 0) {
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
			} else {
				for(int i = 0; i < hits; i++) {
					unit.hitSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
				}
			}
		}
	}
}
