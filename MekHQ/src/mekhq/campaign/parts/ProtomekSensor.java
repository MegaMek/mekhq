/*
 * ProtomekActuator.java
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

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions.MassRepairOption;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ProtomekSensor extends Part {
    private static final long serialVersionUID = 719878556021696393L;

    public ProtomekSensor() {
        this(0, null);
    }

    public ProtomekSensor clone() {
        ProtomekSensor clone = new ProtomekSensor(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }


    public ProtomekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Sensors";
    }

    @Override
    public double getTonnage() {
        //TODO: how much do sensors weight?
        //apparently nothing
        return 0;
    }

    @Override
    public long getStickerPrice() {
        return getUnitTonnage() * 2000;
    }

    @Override
    public boolean isSamePartType (Part part) {
        return part instanceof ProtomekSensor
                && getUnitTonnage() == ((ProtomekSensor)part).getUnitTonnage();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public int getAvailability(int era) {
        if(era == EquipmentType.ERA_CLAN) {
            return EquipmentType.RATING_C;
        } else {
            return EquipmentType.RATING_X;
        }
    }

    @Override
    public int getTechRating() {
        return EquipmentType.RATING_C;
    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD);
        }
    }

    @Override
    public int getTechBase() {
        return T_CLAN;
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtomekSensor(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            int h = Math.max(1, hits);
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD, h);
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
            campaign.addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
        	int priorHits = hits;
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD);
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
			return 120;
		}
        if(hits <= 1) {
            return 100;
        }
        else if(hits == 2) {
            return 150;
        }
        else {
        	return 200;
        }
	}

	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return 0;
		}
		if(hits <= 1) {
            return 0;
        }
        else if(hits == 2) {
            return 1;
        }
        else {
        	return 3;
        }
	}

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public String getDetails() {
        if(null != unit) {
            return unit.getEntity().getLocationName(Protomech.LOC_HEAD);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            if(hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD, hits);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD);
            }
        }
    }

    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
        if(isSalvaging()) {
            return null;
        }
        if(unit.isLocationBreached(Protomech.LOC_HEAD)) {
            return unit.getEntity().getLocationName(Protomech.LOC_HEAD) + " is breached.";
        }
        if(isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(Protomech.LOC_HEAD) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(Protomech.LOC_HEAD);
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;//index == type && loc == location;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECH);
    }

    @Override
    public boolean isOmniPoddable() {
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub

    }

    @Override
	public String getLocationName() {
		return unit.getEntity().getLocationName(getLocation());
	}

	@Override
	public int getLocation() {
		return Protomech.LOC_HEAD;
	}

	@Override
	public int getIntroDate() {
		return 3055;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}
    
    @Override
	public int getMassRepairOptionType() {
    	return MassRepairOption.OPTION_TYPE.ELECTRONICS;
    }
}
