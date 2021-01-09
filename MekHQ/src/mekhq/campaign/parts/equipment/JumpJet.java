/*
 * JumpJet.java
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
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.MissingPart;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpJet extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	public JumpJet() {
    	this(0, null, -1, false, null);
    }

    public JumpJet(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(tonnage, et, equipNum, 1.0, omniPodded, c);
    }

    public JumpJet clone() {
    	JumpJet clone = new JumpJet(getUnitTonnage(), getType(), getEquipmentNum(), omniPodded, campaign);
        clone.copyBaseData(this);
    	return clone;
    }

    @Override
    public double getTonnage() {
        double ton;
        if (type.hasFlag(MiscType.F_PROTOMECH_EQUIPMENT)) {
            if(getUnitTonnage() <=5) {
                ton = 0.05;
            } else if (getUnitTonnage() <= 9){
                ton = 0.1;
            } else {
                ton = 0.15;
            }
        } else {
        	if(getUnitTonnage() >= 90) {
        		ton = 2.0;
        	} else if(getUnitTonnage() >= 60) {
        		ton = 1.0;
        	} else {
        	    ton = 0.5;
        	}
        }
    	if(type.hasSubType(MiscType.S_IMPROVED)) {
    		ton *= 2;
    	}
    	return ton;
    }

    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     */
    @Override
    public Money getStickerPrice() {
        if (isOmniPodded()) {
            return Money.of(250 * getUnitTonnage());
        } else {
            return Money.of(200 * getUnitTonnage());
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
    	if(null != unit) {
			return super.getDetails(includeRepairDetails);
    	}
    	return getUnitTonnage() + " ton unit";
    }

	@Override
	public MissingJumpJet getMissingPart() {
		return new MissingJumpJet(getUnitTonnage(), type, equipmentNum, omniPodded, campaign);
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
			}
		}
	}

	@Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return isOmniPodded()? 30 : 60;
		}
		return 100;
	}

	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return 0;
		}
		return -3;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public boolean isOmniPoddable() {
	    return true;
	}
}
