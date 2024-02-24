/*
 * Avionics.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IAero;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Avionics extends Part {
    public Avionics() {
        this(0, null);
    }

    public Avionics(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Avionics";
    }

    @Override
    public Avionics clone() {
        Avionics clone = new Avionics(0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit
                && (unit.getEntity().getEntityType() & (Entity.ETYPE_AEROSPACEFIGHTER | Entity.ETYPE_LAND_AIR_MECH)) != 0) {
            hits = ((IAero) unit.getEntity()).getAvionicsHits();
            if (checkForDestruction
                    && hits > priorHits
                    && (hits < 3 && !campaign.getCampaignOptions().isUseAeroSystemHits())
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            } else if (hits >= 3) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time;
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 240;
            } else {
                time = 120;
            }
            if (isSalvaging()) {
                if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                    time *= 10;
                } else {
                    time *= 5;
                }
            }
            if (hits == 1) {
                time *= 1;
            }
            if (hits == 2) {
                time *= 2;
            }
            return time;
        }
        if (isSalvaging()) {
            time = 4800;
        } else {
            time = 480;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair time and difficulty
            if (isSalvaging()) {
                return 1;
            }
            if (hits == 1) {
                return 0;
            }
            if (hits == 2) {
                return 1;
            }
        }
        if (isSalvaging()) {
            return 1;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null == unit) {
            return;
        }
        if (unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setAvionicsHits(hits);
        } else if (unit.getEntity() instanceof LandAirMech) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, hits);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setAvionicsHits(0);
            } else if (unit.getEntity() instanceof LandAirMech) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS);
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setAvionicsHits(3);
            } else if (unit.getEntity() instanceof LandAirMech) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, 3);
            }
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
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
        return new MissingAvionics(getUnitTonnage(), campaign);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        // TODO: table in TechManual makes no sense - where are control systems for ASFs?
        return Money.zero();
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public int getTechRating() {
        // go with conventional fighter avionics
        return EquipmentType.RATING_B;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof Avionics;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // nothing to load
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
