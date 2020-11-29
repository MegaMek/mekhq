/*
 * LandingGear.java
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

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LandingGear extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -717866644605314883L;

    public LandingGear() {
        this(0, null);
    }

    public LandingGear(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Landing Gear";
    }

    public LandingGear clone() {
        LandingGear clone = new LandingGear(0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit) {
            if (unit.getEntity() instanceof Aero) {
                if(((Aero)unit.getEntity()).isGearHit()) {
                    hits = 1;
                } else {
                    hits = 0;
                }
            } else if (unit.getEntity() instanceof LandAirMech) {
                hits = unit.getHitCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR);
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
        int time;
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            //Test of proposed errata for repair times
            if (unit != null && unit.getEntity() instanceof Dropship) {
                time = 120;
            } else {
                time = 60;
            }
            if(isSalvaging()) {
                time *= 10;
            }
            return time;
        }
        if(isSalvaging()) {
            time = 1200;
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            return 3;
        }
        return 2;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Aero) {
                ((Aero)unit.getEntity()).setGearHit(needsFixing());
        } else if (null != unit && unit.getEntity() instanceof LandAirMech) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, hits);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setGearHit(false);
        } else if (null != unit && unit.getEntity() instanceof LandAirMech) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero)unit.getEntity()).setGearHit(true);
            } else if (unit.getEntity() instanceof LandAirMech) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, 3);
            }
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
    public MissingPart getMissingPart() {
        return new MissingLandingGear(getUnitTonnage(), campaign);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(10.0 * getUnitTonnage());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public int getTechRating() {
        //go with conventional fighter avionics
        return EquipmentType.RATING_B;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof LandingGear;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        //nothing to load
    }

    @Override
    public boolean isRightTechType(String skillType) {
        if (unit != null && unit.getEntity() instanceof LandAirMech) {
            return skillType.equals(SkillType.S_TECH_MECH);
        }
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
    }

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }
}
