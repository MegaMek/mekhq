/*
 * MissingKFBoom.java
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author MKerensky
 */
public class MissingKFBoom extends MissingPart {

    /**
     * 
     */
    private static final long serialVersionUID = 8782809786871433915L;

    private int boomType;

    public MissingKFBoom() {
        this(0, null, Dropship.BOOM_STANDARD);
    }

    public MissingKFBoom(int tonnage, Campaign c, int boomType) {
        super(tonnage, c);
        this.boomType = boomType;
        this.name = "Dropship K-F Boom";
        if (boomType == Dropship.BOOM_PROTOTYPE) {
            name += " (Prototype)";
        }
    }

    @Override 
    public int getBaseTime() {
        return 3600;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship)unit.getEntity()).setDamageKFBoom(true);
        }
    }

    @Override
    public Part getNewPart() {
        return new KfBoom(getUnitTonnage(), campaign, boomType);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public double getTonnage() {
        return 0;
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
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof KfBoom)
                && (refit || (((KfBoom) part).getBoomType() == boomType));
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
            return KfBoom.TA_PROTOTYPE_KF_BOOM;
        } else {
            return KfBoom.TA_KFBOOM;
        }
    }
}