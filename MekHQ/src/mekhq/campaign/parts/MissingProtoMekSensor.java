/*
 * MissingProtomekActuator.java
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

import megamek.common.CriticalSlot;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingProtoMekSensor extends MissingPart {
    public MissingProtoMekSensor() {
        this(0, null);
    }

    public MissingProtoMekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Sensors";
    }

    @Override
    public int getBaseTime() {
        return 120;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        // TODO : how much do actuators weight?
        // apparently nothing
        return 0;
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
    public void updateConditionFromPart() {
        if (null != unit) {
              unit.destroySystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEADCRIT, ProtoMek.LOC_HEAD, 1);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(ProtoMek.LOC_HEAD)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_HEAD) + " is breached.";
        }
        if (unit.isLocationDestroyed(ProtoMek.LOC_HEAD)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_HEAD) + " is destroyed.";
        }
        return null;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            remove(false);
            //assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtoMekSensor
                && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtoMekSensor(getUnitTonnage(), campaign);
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
    }

    @Override
    public int getLocation() {
        return ProtoMek.LOC_HEAD;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekLocation.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
