/*
 * AeroSensor.java
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
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AeroSensor extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -717866644605314883L;

    final static TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private boolean largeCraft;

    public AeroSensor() {
        this(0, false, null);
    }

    public AeroSensor(int tonnage, boolean lc, Campaign c) {
        super(tonnage, c);
        this.name = "Aerospace Sensors";
        this.largeCraft = lc;
    }

    public AeroSensor clone() {
        AeroSensor clone = new AeroSensor(getUnitTonnage(), largeCraft, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit && unit.getEntity() instanceof Aero) {
            hits = ((Aero)unit.getEntity()).getSensorHits();
            if(checkForDestruction
                    && hits > priorHits
                    && (hits < 3 && !campaign.getCampaignOptions().useAeroSystemHits())
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            } else if (hits >= 3) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time = 0;
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            //Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 120;
            } else {
                time = 75;
            }
            if (hits == 1) {
                time *= 1;
            }
            if (hits == 2) {
                time *= 2;
            }
            if (isSalvaging()) {
                if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                    time = 1200;
                } else {
                    time = 260;
                }
            }
            return time;
        }
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 1200;
            } else {
                time = 260;
            }
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            //Test of proposed errata for repair time and difficulty
            if(isSalvaging()) {
                return -2;
            }
            if (hits == 1) {
                return -1;
            }
            if (hits == 2) {
                return 0;
            }
        }
        if(isSalvaging()) {
            return -2;
        }
        return -1;
    }


    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setSensorHits(hits);
        }

    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setSensorHits(0);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setSensorHits(3);
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
        return new MissingAeroSensor(getUnitTonnage(), largeCraft, campaign);
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
        if(largeCraft) {
            return Money.of(80000);
        }
        return Money.of(2000 * getUnitTonnage());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AeroSensor && largeCraft == ((AeroSensor)part).isForSpaceCraft()
                && (largeCraft || getUnitTonnage() == part.getUnitTonnage());
    }

    public boolean isForSpaceCraft() {
        return largeCraft;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<dropship>"
                +largeCraft
                +"</dropship>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("dropship")) {
                largeCraft = wn2.getTextContent().trim().equalsIgnoreCase("true");
            }
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        String dropper = "";
        if(largeCraft) {
            dropper = " (spacecraft)";
        }

        String details = super.getDetails(includeRepairDetails);
        if (!details.isEmpty()) {
            details += ", ";
        }

        details += getUnitTonnage() + " tons" + dropper;

        return details;
    }

    @Override
    public boolean isRightTechType(String skillType) {
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
        return TECH_ADVANCEMENT;
    }
    @Override
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
