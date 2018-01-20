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

package mekhq.campaign.parts.equipment;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class HeatSink extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	public HeatSink() {
    	this(0, null, -1, false, null);
    }

    public HeatSink(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, omniPodded, c);
    }

    public HeatSink clone() {
    	HeatSink clone = new HeatSink(getUnitTonnage(), getType(), getEquipmentNum(), omniPodded, campaign);
        clone.copyBaseData(this);
    	return clone;
    }

    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     */
    @Override
    public long getStickerPrice() {
    	if(type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || type.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
    		return isOmniPodded()? 7500 : 6000;
    	} else {
    		return isOmniPodded()? 2500 : 2000;
    	}
    }

	@Override
	public MissingPart getMissingPart() {
		return new MissingHeatSink(getUnitTonnage(), type, equipmentNum, omniPodded, campaign);
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit) {
			int priorHits = hits;
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(mounted.isMissing()) {
					remove(false);
					return;
				}
				hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, equipmentNum, mounted.getLocation());
			}
			if(checkForDestruction
					&& hits > priorHits
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
				return;
			}
		}
	}

	@Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return isOmniPodded()? 30 : 90;
		}
		return 120;
	}

	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return isOmniPodded()? -4 : -2;
		}
		return -1;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public int getRepairPartType() {
    	return Part.REPAIR_PART_TYPE.HEATSINK;
    }

	@Override
	public boolean isOmniPoddable() {
	    return true;
	}
}
