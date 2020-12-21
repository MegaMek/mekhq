/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.service;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.SkillType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MassRepairOption {
    //region Variable Declarations
    private PartRepairType type;
    private boolean active;
    private int skillMin;
    private int skillMax;
    private int bthMin;
    private int bthMax;

    private static final int DEFAULT_BTH = 4;
    //endregion Variable Declarations

    //region Constructors
    public MassRepairOption(PartRepairType type) {
        this (type, false, SkillType.EXP_ULTRA_GREEN, SkillType.EXP_ELITE, DEFAULT_BTH, DEFAULT_BTH);
    }

    public MassRepairOption(PartRepairType type, boolean active, int skillMin, int skillMax, int bthMin, int bthMax) {
        this.type = type;
        this.active = active;
        this.skillMin = skillMin;
        this.skillMax = skillMax;
        this.bthMin = bthMin;
        this.bthMax = bthMax;
    }
    //endregion Constructors

    //region Getters/Setters
    public PartRepairType getType() {
        return type;
    }

    public void setType(PartRepairType type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSkillMin() {
        return skillMin;
    }

    public void setSkillMin(int skillMin) {
        this.skillMin = skillMin;
    }

    public int getSkillMax() {
        return skillMax;
    }

    public void setSkillMax(int skillMax) {
        this.skillMax = skillMax;
    }

    public int getBthMin() {
        return bthMin;
    }

    public void setBthMin(int bthMin) {
        this.bthMin = bthMin;
    }

    public int getBthMax() {
        return bthMax;
    }

    public void setBthMax(int bthMax) {
        this.bthMax = bthMax;
    }
    //endregion Getters/Setters

    //region File IO
    public void writeToXML(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "massRepairOption");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "type", getType().name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "active", isActive() ? 1 : 0);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "skillMin", getSkillMin());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "skillMax", getSkillMax());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "btnMin", getBthMin());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "btnMax", getBthMax());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "massRepairOption");
    }

    public static List<MassRepairOption> parseListFromXML(Node wn2, Version version) {
        List<MassRepairOption> massRepairOptions = new ArrayList<>();
        NodeList mroList = wn2.getChildNodes();
        List<PartRepairType> partRepairTypes = PartRepairType.getMRMSValidTypes();

        for (int mroIdx = 0; mroIdx < mroList.getLength(); mroIdx++) {
            Node mroNode = mroList.item(mroIdx);

            if (mroNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                MassRepairOption mro = parseFromXML(mroNode);

                // This fixes a migration issue from 0.47.10 to 0.47.11
                if (version.isBetween("0.47.10", "0.47.15") && (mro.getType() == PartRepairType.HEAT_SINK)) {
                    mro.setType(PartRepairType.POD_SPACE);
                }

                if ((mro.getType() == PartRepairType.UNKNOWN_LOCATION) || !partRepairTypes.contains(mro.getType())) {
                    MekHQ.getLogger().error("Attempted to load MassRepairOption with illegal type id of " + mro.getType());
                } else {
                    massRepairOptions.add(mro);
                }
            } catch (Exception e) {
                MekHQ.getLogger().error("Failed to parse MassRepairOption from XML", e);
            }
        }

        return massRepairOptions;
    }

    private static MassRepairOption parseFromXML(Node mroNode) {
        MassRepairOption mro = new MassRepairOption(PartRepairType.UNKNOWN_LOCATION);

        NodeList mroItemList = mroNode.getChildNodes();
        for (int mroItemIdx = 0; mroItemIdx < mroItemList.getLength(); mroItemIdx++) {
            Node mroItemNode = mroItemList.item(mroItemIdx);

            if (mroItemNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (mroItemNode.getNodeName().equalsIgnoreCase("type")) {
                mro.setType(PartRepairType.parseFromString(mroItemNode.getTextContent().trim()));
            } else if (mroItemNode.getNodeName().equalsIgnoreCase("active")) {
                mro.setActive(Integer.parseInt(mroItemNode.getTextContent().trim()) == 1);
            } else if (mroItemNode.getNodeName().equalsIgnoreCase("skillMin")) {
                mro.setSkillMin(Integer.parseInt(mroItemNode.getTextContent().trim()));
            } else if (mroItemNode.getNodeName().equalsIgnoreCase("skillMax")) {
                mro.setSkillMax(Integer.parseInt(mroItemNode.getTextContent().trim()));
            } else if (mroItemNode.getNodeName().equalsIgnoreCase("btnMin")) {
                mro.setBthMin(Integer.parseInt(mroItemNode.getTextContent().trim()));
            } else if (mroItemNode.getNodeName().equalsIgnoreCase("btnMax")) {
                mro.setBthMax(Integer.parseInt(mroItemNode.getTextContent().trim()));
            }

            MekHQ.getLogger().debug(String.format("massRepairOption %s.%s\n\t%s",
                    mro.getType(), mroItemNode.getNodeName(), mroItemNode.getTextContent()));
        }

        return mro;
    }
    //endregion File IO
}
