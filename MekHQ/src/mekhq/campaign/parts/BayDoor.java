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

import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public class BayDoor extends Part {
    
    /**
     * 
     */
    private static final long serialVersionUID = 685375245347077715L;

    public BayDoor() {
        this(0, null);
    }
    
    public BayDoor(int tonnage, Campaign c) {
        super(tonnage, false, c);
        name = "Bay Door";
    }
    
    @Override
    public String getName() {
        Part parent = campaign.getPart(parentPartId);
        if (null != parent) {
            return parent.getName() + " Door";
        }
        return super.getName();
    }
    
    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 600;
        }
        return 60;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // This is handled by the transport bay part to coordinate all the doors
    }

    @Override
    public void updateConditionFromPart() {
        // This is handled by the transport bay part to coordinate all the doors
    }
    
    @Override
    public void fix() {
        super.fix();
        Part bayPart = campaign.getPart(parentPartId);
        if (null != bayPart) {
            Bay bay = ((TransportBayPart) bayPart).getBay();
            if (null != bay) {
                bay.setCurrentDoors(Math.min(bay.getCurrentDoors() + 1, bay.getDoors()));
            }
        }
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
        return new MissingBayDoor(getUnitTonnage(), campaign);
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
        return hits > 0;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -1;
        }
        return -3;
    }

    @Override
    public long getStickerPrice() {
        return 1000;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof BayDoor;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
    }

    @Override
    public Part clone() {
        Part newPart = new BayDoor(getUnitTonnage(), campaign);
        copyBaseData(newPart);
        return newPart;
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return new TechAdvancement().setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);
    }

}
