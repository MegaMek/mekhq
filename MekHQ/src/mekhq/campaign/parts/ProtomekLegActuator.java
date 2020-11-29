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

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Protomech;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ProtomekLegActuator extends Part {
    private static final long serialVersionUID = 719878556021696393L;

    public ProtomekLegActuator() {
        this(0, null);
    }

    public ProtomekLegActuator clone() {
        ProtomekLegActuator clone = new ProtomekLegActuator(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }


    public ProtomekLegActuator(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Leg Actuator";
    }

    @Override
    public double getTonnage() {
        //TODO: how much do actuators weight?
        //apparently nothing
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(getUnitTonnage() * 540);
    }

    @Override
    public boolean isSamePartType (Part part) {
        return part instanceof ProtomekLegActuator
                && getUnitTonnage() == ((ProtomekLegActuator)part).getUnitTonnage();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_LEGCRIT, Protomech.LOC_LEG);
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
        return new MissingProtomekLegActuator(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            int h = Math.max(2, hits);
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_LEGCRIT, Protomech.LOC_LEG, h);
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
        if(null != unit) {
        	int priorHits = hits;
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_LEGCRIT, Protomech.LOC_LEG);
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
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if(null != unit) {
            return unit.getEntity().getLocationName(Protomech.LOC_LEG);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            if(hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_LEGCRIT, Protomech.LOC_LEG, hits);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_LEGCRIT, Protomech.LOC_LEG);
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
        if(unit.isLocationBreached(Protomech.LOC_LEG)) {
            return unit.getEntity().getLocationName(Protomech.LOC_LEG) + " is breached.";
        }
        if(isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(Protomech.LOC_LEG) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(Protomech.LOC_LEG);
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
   		return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
   	}

	@Override
	public int getLocation() {
		return Protomech.LOC_LEG;
	}

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtomekLocation.TECH_ADVANCEMENT;
    }

    @Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ACTUATOR;
    }
}
