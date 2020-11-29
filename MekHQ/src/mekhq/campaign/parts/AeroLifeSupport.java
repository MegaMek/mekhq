/*
 * AeroLifeSupport.java
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
public class AeroLifeSupport extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -717866644605314883L;

    private Money cost;
    private boolean fighter;

    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public AeroLifeSupport() {
        this(0, Money.zero(), false, null);
    }

    public AeroLifeSupport(int tonnage, Money cost, boolean f, Campaign c) {
        super(tonnage, c);
        this.cost = cost;
        this.name = "Fighter Life Support";
        this.fighter = f;
        if(!fighter) {
            this.name = "Spacecraft Life Support";
        }
    }

    public AeroLifeSupport clone() {
        AeroLifeSupport clone = new AeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit && unit.getEntity() instanceof Aero) {
             if(((Aero)unit.getEntity()).hasLifeSupport()) {
                 hits = 0;
             } else {
                 hits = 1;
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
        int time = 0;
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            //Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                if (isSalvaging()) {
                    time = 1200;
                } else {
                    time = 120;
                }
            } else {
                if (isSalvaging()) {
                    time = 180;
                } else {
                    time = 60;
                }
            }
            return time;
        }
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 6720;
            } else {
                time = 180;
            }
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                return 0;
            } else {
                return -1;
            }
        }
        return 1;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Aero) {
            if(hits > 0) {
                ((Aero)unit.getEntity()).setLifeSupport(false);
            } else {
                ((Aero)unit.getEntity()).setLifeSupport(true);
            }
        }

    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setLifeSupport(true);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setLifeSupport(false);
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
        return new MissingAeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
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
        return cost;
    }

    public void calculateCost() {
        if(fighter) {
            cost = Money.of(50000);
        }
        if(null != unit) {
            cost = Money.of(5000.0 * (((Aero)unit.getEntity()).getNCrew() + ((Aero)unit.getEntity()).getNPassenger()));
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    public boolean isForFighter() {
        return fighter;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AeroLifeSupport && fighter == ((AeroLifeSupport)part).isForFighter()
                && (getStickerPrice().equals(part.getStickerPrice()));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<fighter>"
                +fighter
                +"</fighter>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<cost>"
                + cost.toXmlString()
                +"</cost>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("fighter")) {
                fighter = wn2.getTextContent().trim().equalsIgnoreCase("true");
            } else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                cost = Money.fromXmlString(wn2.getTextContent().trim());
            }
        }
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
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }
}
