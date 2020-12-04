/*
 * KFBoom.java
 *
 * Copyright (c) 2019 MegaMek Team
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
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author MKerensky
 */
public class KfBoom extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -3211076278442082220L;

    static final TechAdvancement TA_KFBOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2458, 2470, 2500).setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH).setTechRating(RATING_C)
            .setAvailability(RATING_D, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_PROTOTYPE_KF_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2458, 2470, 2500).setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH).setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private int boomType = Dropship.BOOM_STANDARD;

    public KfBoom() {
        this(0, null, Dropship.BOOM_STANDARD);
    }

    public KfBoom(int tonnage, Campaign c, int boomType) {
        super(tonnage, c);
        this.boomType = boomType;
        this.name = "Dropship K-F Boom";
        if (boomType == Dropship.BOOM_PROTOTYPE) {
            name += " (Prototype)";
        }
    }

    public KfBoom clone() {
        KfBoom clone = new KfBoom(getUnitTonnage(), campaign, boomType);
        clone.copyBaseData(this);
        return clone;
    }

    public int getBoomType() {
        return boomType;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if(null != unit && unit.getEntity() instanceof Dropship) {
             if(((Dropship)unit.getEntity()).isKFBoomDamaged()) {
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
        if(isSalvaging()) {
            return 3600;
        }
        return 360;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            return 0;
        }
        return -1;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageKFBoom(hits > 0);
        }
    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship)unit.getEntity()).setDamageKFBoom(false);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship)unit.getEntity()).setDamageKFBoom(true);
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
        return new MissingKFBoom(getUnitTonnage(), campaign, boomType);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return (hits > 0);
    }

    @Override
    public Money getStickerPrice() {
        if (boomType == Dropship.BOOM_STANDARD) {
            return Money.of(10000);
        } else if (boomType == Dropship.BOOM_PROTOTYPE) {
            return Money.of(1010000) ;
        } else {
            return Money.zero();
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof KfBoom)
                && (boomType == ((KfBoom)part).boomType);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "boomType", boomType);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("boomType")) {
                boomType = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
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
        if (boomType != Dropship.BOOM_STANDARD) {
            return TA_PROTOTYPE_KF_BOOM;
        } else {
            return TA_KFBOOM;
        }
    }
}
