/*
 * HeatSink.java
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

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class HeatSink extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	public HeatSink() {
    	this(0, null, -1, null);
    }
    
    public HeatSink(int tonnage, EquipmentType et, int equipNum, Campaign c) {
        super(tonnage, et, equipNum, c);
    }
    
    public HeatSink clone() {
    	return new HeatSink(getUnitTonnage(), getType(), getEquipmentNum(), campaign);
    }
    
    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     */
    @Override
    public long getStickerPrice() {		
    	if(type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || type.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
    		return 6000;
    	} else {
    		return 2000;	
    	}
    }

	@Override
	public Part getMissingPart() {
		return new MissingHeatSink(getUnitTonnage(), type, equipmentNum, campaign);
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(!mounted.isRepairable()) {
					remove(false);
					return;
				} else if(mounted.isDestroyed()) {
					//TODO: calculate actual hits
					hits = 1;
				} else {
					hits = 0;
				}
			}
			if(hits == 0) {
				time = 0;
				difficulty = 0;
			} else if(hits > 0) {
				this.time = 120;
				this.difficulty = -1;
			}
			if(isSalvaging()) {
				this.time = 90;
				this.difficulty = -2;
			}
		}
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
}
