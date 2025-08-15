/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

/**
 * @author Neoancient
 */
public class BayDoor extends Part {
    public BayDoor() {
        this(0, null);
    }

    public BayDoor(int tonnage, Campaign c) {
        super(tonnage, false, c);
        name = "Bay Door";
    }

    @Override
    public String getName() {
        if (null != parentPart) {
            return parentPart.getName() + " Door";
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
        if (parentPart instanceof TransportBayPart) {
            Bay bay = ((TransportBayPart) parentPart).getBay();
            if (null != bay) {
                bay.setCurrentDoors(Math.min(bay.getCurrentDoors() + 1, bay.getDoors()));
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        // Grab a reference to our parent part so that we don't accidentally NRE
        // when we remove the parent part reference.
        Part parentPart = getParentPart();
        if (null != parentPart) {
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
            parentPart.removeChildPart(this);
            parentPart.addChildPart(missing);
            parentPart.updateConditionFromPart();
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
    public @Nullable String checkFixable() {
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
    public Money getStickerPrice() {
        return Money.of(1000);
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
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
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
        return new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                     .setTechRating(TechRating.A)
                     .setAvailability(AvailabilityValue.A,
                           AvailabilityValue.A,
                           AvailabilityValue.A,
                           AvailabilityValue.A)
                     .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
