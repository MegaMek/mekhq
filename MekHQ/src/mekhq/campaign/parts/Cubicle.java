/*
 * Copyright (c) 2017 - The MegaMek Team. All rights reserved.
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

import megamek.common.BayType;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * A transport bay cubicle for a Mech, protomech, vehicle, fighter, or small craft.
 *
 * @author Neoancient
 *
 */
public class Cubicle extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -5341170772636675399L;

    private BayType bayType;

    public Cubicle() {
        this(0, null, null);
    }

    public Cubicle(int tonnage, BayType bayType, Campaign c) {
        super(tonnage, false, c);
        this.bayType = bayType;
        if (null != bayType) {
            name = bayType.getDisplayName() + " Cubicle";
        }
    }

    public BayType getBayType() {
        return bayType;
    }

    @Override
    public String getName() {
        Part parent = campaign.getPart(parentPartId);
        if (null != parent) {
            return parent.getName() + " Cubicle";
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        // replacement time 1 week
        return 3360;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void updateConditionFromPart() {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void remove(boolean salvage) {
        Part bayPart = campaign.getPart(parentPartId);
        if (null != bayPart) {
            Part spare = campaign.checkForExistingSparePart(this);
            if (!salvage) {
                campaign.removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
            bayPart.removeChildPart(id);
            bayPart.addChildPart(missing);
            bayPart.updateConditionFromPart();
        }
        setUnit(null);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingCubicle(getUnitTonnage(), bayType, campaign);
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        // Per replacement repair tables in SO, cubicles are replaced rather than repaired.
        return false;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public long getStickerPrice() {
        return bayType.getCost();
    }

    @Override
    public double getTonnage() {
        return bayType.getWeight();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof Cubicle)
                && (((Cubicle) part).getBayType() == bayType);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "bayType", bayType.toString());
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayType")) {
                bayType = BayType.parse(wn2.getTextContent());
                if (null == bayType) {
                    MekHQ.getLogger().log(Cubicle.class, "loadFieldsFromXmlNode(Node)",
                            LogLevel.ERROR, "Could not parse bay type " + wn2.getTextContent());
                    bayType = BayType.MECH;
                }
                name = bayType.getDisplayName() + " Cubicle";
            }
        }
    }

    @Override
    public Part clone() {
        Part part = new Cubicle(getUnitTonnage(), bayType, campaign);
        copyBaseData(part);
        return part;
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public ITechnology getTechAdvancement() {
        return bayType;
    }

}
