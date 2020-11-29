/*
 * VeeStabiliser.java
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

import megamek.common.Compute;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeStabiliser extends Part {
    private static final long serialVersionUID = 6708245721569856817L;

    private int loc;

    public VeeStabiliser() {
        this(0, 0, null);
    }

    public VeeStabiliser(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.name = "Vehicle Stabiliser";
    }

    public VeeStabiliser clone() {
        VeeStabiliser clone = new VeeStabiliser(getUnitTonnage(), 0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof VeeStabiliser;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<loc>"
                +loc
                +"</loc>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                loc = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public int getBaseAvailability(int era) {
        return EquipmentType.RATING_B;
    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit && unit.getEntity() instanceof Tank) {
            ((Tank)unit.getEntity()).clearStabiliserHit(loc);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingVeeStabiliser(getUnitTonnage(), loc, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit && unit.getEntity() instanceof Tank) {
            ((Tank)unit.getEntity()).setStabiliserHit(loc);
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
        if(null != unit && unit.getEntity() instanceof Tank) {
            int priorHits = hits;
            if(((Tank)unit.getEntity()).isStabiliserHit(loc)) {
                hits = 1;
            } else {
                hits = 0;
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
        return 60;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Tank) {
            if(hits > 0 && !((Tank)unit.getEntity()).isStabiliserHit(loc)) {
                ((Tank)unit.getEntity()).setStabiliserHit(loc);
            }
            else if(hits == 0 && ((Tank)unit.getEntity()).isStabiliserHit(loc)) {
                ((Tank)unit.getEntity()).clearStabiliserHit(loc);
            }
        }
    }

    @Override
    public String checkFixable() {
        if(!isSalvaging() && (null != unit) && unit.isLocationBreached(loc)) {
            return unit.getEntity().getLocationName(loc) + " is breached.";
        }
        return null;
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        // TODO Auto-generated method stub
        return Money.zero();
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if(null != unit) {
            return unit.getEntity().getLocationName(loc);
        }
        return "";
    }

    public int getLocation() {
        return loc;
    }

    public void setLocation(int l) {
        this.loc = l;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECHANIC);
    }

    @Override
    public String getLocationName() {
        return unit.getEntity().getLocationName(loc);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TankLocation.TECH_ADVANCEMENT;
    }

}
