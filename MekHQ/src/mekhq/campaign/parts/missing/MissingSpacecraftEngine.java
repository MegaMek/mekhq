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
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;

import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.SpacecraftEngine;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingSpacecraftEngine extends MissingPart {
    double engineTonnage;
    boolean clan;

    public MissingSpacecraftEngine() {
        this(0, 0, null, false);
    }

    public MissingSpacecraftEngine(int tonnage, double eTonnage, Campaign c, boolean clan) {
        super(tonnage, c);
        this.engineTonnage = eTonnage;
        this.clan = clan;
        this.name = "Spacecraft Engine";
    }

    @Override
    public int getBaseTime() {
        //Per errata, small craft now use fighter engine times but still have the
        //large craft engine part
        if (null != unit && (unit.getEntity() instanceof SmallCraft && !(unit.getEntity() instanceof Dropship))) {
            return 360;
        }
        return 43200;
    }

    @Override
    public int getDifficulty() {
        return 1;
    }

    @Override
    public double getTonnage() {
        return engineTonnage;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof SpacecraftEngine
                     && getName().equals(part.getName())
                     && getTonnage() == part.getTonnage();
    }

    @Override
    public int getTechLevel() {
        if (clan) {
            return TechConstants.T_CLAN_TW;
        } else {
            return TechConstants.T_IS_TW_NON_BOX;
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineTonnage", engineTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("engineTonnage")) {
                engineTonnage = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public Part getNewPart() {
        return new SpacecraftEngine(getUnitTonnage(), engineTonnage, campaign, clan);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(3);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return SpacecraftEngine.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ENGINE;
    }
}
