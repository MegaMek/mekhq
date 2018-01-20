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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.BayType;
import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public class MissingCubicle extends MissingPart {

    /**
     *
     */
    private static final long serialVersionUID = -5418633125937755683L;

    private BayType bayType;

    public MissingCubicle() {
        this(0, null, null);
    }

    public MissingCubicle(int tonnage, BayType bayType, Campaign c) {
        super(tonnage, false, c);
        this.bayType = bayType;
        if (null != bayType) {
            name = bayType.getDisplayName() + " Cubicle";
        }
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
        return 3360; // one week
    }

    @Override
    public void updateConditionFromPart() {
        // TODO Auto-generated method stub

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
    public int getDifficulty() {
        return -1;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof Cubicle)
                && (((Cubicle) part).getBayType() == bayType);
    }

    @Override
    public Part getNewPart() {
        return new Cubicle(getUnitTonnage(), bayType, campaign);
    }

    @Override
    public double getTonnage() {
        return bayType.getWeight();
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayType")) {
                bayType = BayType.parse(wn2.getTextContent());
                if (null == bayType) {
                    MekHQ.getLogger().log(MissingCubicle.class, "loadFieldsFromXmlNode(Node)",
                            LogLevel.ERROR, "Could not parse bay type " + wn2.getTextContent());
                    bayType = BayType.MECH;
                }
                name = bayType.getDisplayName() + " Cubicle";
            }
        }
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
