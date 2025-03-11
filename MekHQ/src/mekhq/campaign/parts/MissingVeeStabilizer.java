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

import megamek.common.Tank;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingVeeStabilizer extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingVeeStabilizer.class);

    private int loc;

    public MissingVeeStabilizer() {
        this(0, 0, null);
    }

    public MissingVeeStabilizer(int tonnage, int loc, Campaign c) {
        super(0, c);
        this.name = "Vehicle Stabiliser";
        this.loc = loc;
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new VeeStabilizer(getUnitTonnage(), loc, campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof VeeStabilizer;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                    loc = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        VeeStabilizer replacement = (VeeStabilizer) findReplacement(false);
        if (null != replacement) {
            VeeStabilizer actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            actualReplacement.setLocation(loc);
            remove(false);
            // assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Tank) {
            ((Tank) unit.getEntity()).setStabiliserHit(loc);
        }
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(loc) : null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TankLocation.TECH_ADVANCEMENT;
    }
}
