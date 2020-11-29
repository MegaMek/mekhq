/*
 * FireControlSystem.java
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
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class FireControlSystem extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -717866644605314883L;

    private Money cost;

    public FireControlSystem() {
        this(0, Money.zero(), null);
    }

    public FireControlSystem(int tonnage, Money cost, Campaign c) {
        super(tonnage, c);
        this.cost = cost;
        this.name = "Fire Control System";
    }

    public FireControlSystem clone() {
        FireControlSystem clone = new FireControlSystem(0, cost, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit && unit.getEntity() instanceof Aero) {
            hits = ((Aero)unit.getEntity()).getFCSHits();
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
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship))  {
                time = 120;
                if (unit.getEntity().hasNavalC3()) {
                    time *= 2;
                }
            } else {
                time = 60;
            }
            if (isSalvaging()) {
                time *= 10;
            } else if (hits == 1) {
                time *= 1;
            } else if (hits == 2) {
                time *= 2;
            }
            return time;
        }
        if(isSalvaging()) {
            time = 4320;
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
                return 0;
            }
            if (hits == 1) {
                return 1;
            }
            if (hits == 2) {
                return 2;
            }
        }
        if(isSalvaging()) {
            return 0;
        }
        return 1;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setFCSHits(hits);
        }

    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setFCSHits(0);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setFCSHits(3);
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
        return new MissingFireControlSystem(getUnitTonnage(), cost, campaign);
    }

    @Override
    public String checkFixable() {
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                // FCS/CIC computers are designed for and built into the ship. Can't salvage and use somewhere else
                return "You cannot salvage a spacecraft FCS. You must scrap it instead.";
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        calculateCost();
        return cost;
    }

    public void calculateCost() {
        if(null != unit) {
            if(unit.getEntity() instanceof SmallCraft) {
                cost = Money.of(100000 + 10000 * ((SmallCraft)unit.getEntity()).getArcswGuns());
            }
            else if(unit.getEntity() instanceof Jumpship) {
                cost = Money.of(100000 + 10000 * ((Jumpship)unit.getEntity()).getArcswGuns());
            }
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        calculateCost();
        return part instanceof FireControlSystem && getStickerPrice().equals(part.getStickerPrice());
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<cost>"
                +cost.toXmlString()
                +"</cost>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                cost = Money.fromXmlString(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }
}
