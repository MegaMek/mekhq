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
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.BipedMek;
import megamek.common.CriticalSlot;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekActuator extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingMekActuator.class);

    protected int type;
    protected int location;

    public MissingMekActuator() {
        this(0, 0, null);
    }

    public int getType() {
        return type;
    }

    public MissingMekActuator(int tonnage, int type, Campaign c) {
        this(tonnage, type, -1, c);
    }

    public MissingMekActuator(int tonnage, int type, int loc, Campaign c) {
        super(tonnage, c);
        this.type = type;
        Mek m = new BipedMek();
        this.name = m.getSystemName(type) + " Actuator";
        this.location = loc;
        this.unitTonnageMatters = true;
    }

    @Override
    public int getBaseTime() {
        return isOmniPodded() ? 30 : 90;
    }

    @Override
    public int getDifficulty() {
        return -3;
    }

    @Override
    public double getTonnage() {
        // TODO: how much do actuators weight?
        // apparently nothing
        return 0;
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "location", location);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    type = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                    location = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            ((MekActuator) actualReplacement).setLocation(location);
            remove(false);
            // assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof MekActuator) {
            MekActuator actuator = (MekActuator) part;
            return actuator.getType() == type && getUnitTonnage() == actuator.getUnitTonnage();
        }
        return false;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(location)) {
            return unit.getEntity().getLocationName(location) + " is breached.";
        }
        if (unit.isLocationDestroyed(location)) {
            return unit.getEntity().getLocationName(location) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return null != unit && unit.hasBadHipOrShoulder(location);
    }

    @Override
    public Part getNewPart() {
        return new MekActuator(getUnitTonnage(), type, -1, campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, type, location);
        }
    }

    @Override
    public boolean isOmniPoddable() {
        return type == Mek.ACTUATOR_LOWER_ARM || type == Mek.ACTUATOR_HAND;
    }

    @Override
    public boolean isOmniPodded() {
        return isOmniPoddable() && getUnit() != null && getUnit().getEntity().isOmni();
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(location) : null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return (getUnitTonnage() <= 100) ? MekActuator.TA_STANDARD : MekActuator.TA_SUPERHEAVY;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ACTUATOR;
    }
}
