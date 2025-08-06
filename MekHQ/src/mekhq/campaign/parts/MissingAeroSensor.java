/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Aero;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingAeroSensor extends MissingPart {
    private boolean dropship;

    public MissingAeroSensor() {
        this(0, false, null);
    }

    public MissingAeroSensor(int tonnage, boolean dropship, Campaign campaign) {
        super(tonnage, campaign);
        this.name = "Aero Sensors";
        this.dropship = dropship;
    }

    @Override
    public int getBaseTime() {
        //Published errata for replacement times of small aero vs large craft
        if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
            return 1200;
        }
        return 260;
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new AeroSensor(getUnitTonnage(), dropship, campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof AeroSensor && dropship == ((AeroSensor) part).isForSpaceCraft()
                     && (dropship || getUnitTonnage() == part.getUnitTonnage());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dropship", dropship);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("dropship")) {
                dropship = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setSensorHits(3);
        }
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
    public TechAdvancement getTechAdvancement() {
        return AeroSensor.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }

    /**
     * This function is over-ridden from it's parent because Aero Sensors depend on tonnage for small craft, but not for
     * dropships/warships So we have to use regexes to change the acquistion name if the sensors are for spacecraft
     *
     * @return description string for the missing part
     */
    @Override
    public String getAcquisitionName() {
        if (dropship) {
            //The below regex splits by the () characters but keeps them in the description
            String[] sliced = super.getAcquisitionName().split("(?<=\\()|(?=\\))");
            String description = sliced[0] + sliced[2] + sliced[3];
            return description;
        } else {
            return super.getAcquisitionName();
        }
    }
}
