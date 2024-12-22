/*
 * MissingAeroSensor.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;

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

    /**  This function is over-ridden from it's parent because Aero Sensors depend on tonnage for small craft, but not for dropships/warships
    * So we have to use regexes to change the acquistion name if the sensors are for spacecraft
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
