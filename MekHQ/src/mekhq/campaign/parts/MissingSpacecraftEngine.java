/*
 * MissingSpacecraftEngine.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingSpacecraftEngine extends MissingPart {
    private static final long serialVersionUID = -6961398614705924172L;
    double engineTonnage;
    boolean clan;

    public MissingSpacecraftEngine() {
        this(0, 0, null, false);
    }

    public MissingSpacecraftEngine(int tonnage, double etonnage, Campaign c, boolean clan) {
        super(tonnage, c);
        this.engineTonnage = etonnage;
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
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        // The engine is a MM object...
        // And doesn't support XML serialization...
        // But it's defined by 3 ints. So we'll save those here.
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<engineTonnage>"
                +engineTonnage
                +"</engineTonnage>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<clan>"
                +clan
                +"</clan>");
        writeToXmlEnd(pw1, indent);
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
    public String checkFixable() {
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
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ENGINE;
    }
}
