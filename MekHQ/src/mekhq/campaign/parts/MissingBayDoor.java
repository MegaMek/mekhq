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

import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public class MissingBayDoor extends MissingPart {
    
    /**
     * 
     */
    private static final long serialVersionUID = 4652276524852879974L;
    
    public MissingBayDoor() {
        this(0, null);
    }

    public MissingBayDoor(int tonnage, Campaign c) {
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
        return 600;
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
        return part instanceof BayDoor;
    }

    @Override
    public Part getNewPart() {
        return new BayDoor(getUnitTonnage(), campaign);
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
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
