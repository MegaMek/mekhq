/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.service.mrms;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MRMSOption {
    private static final MMLogger logger = MMLogger.create(MRMSOption.class);

    // region Variable Declarations
    private PartRepairType type;
    private boolean active;
    private int skillMin;
    private int skillMax;
    private int bthMin;
    private int bthMax;
    private int dailyTimeMin;

    private static final int DEFAULT_BTH = 4;
    private static final int MIN_DAILY_TIME = 0;
    // endregion Variable Declarations

    // region Constructors
    public MRMSOption(PartRepairType type) {
        this(type, false, SkillType.EXP_ULTRA_GREEN, SkillType.EXP_ELITE, DEFAULT_BTH, DEFAULT_BTH, MIN_DAILY_TIME);
    }

    public MRMSOption(PartRepairType type, boolean active, int skillMin, int skillMax, int bthMin, int bthMax, int dailyTimeMin) {
        this.type = type;
        this.active = active;
        this.skillMin = skillMin;
        this.skillMax = skillMax;
        this.bthMin = bthMin;
        this.bthMax = bthMax;
        this.dailyTimeMin = dailyTimeMin;
    }
    // endregion Constructors

    // region Getters/Setters
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

    public int getDailyTimeMin() {
        return dailyTimeMin;
    }

    public void setDailyTimeMin(int minDailyTime) {
        this.dailyTimeMin = minDailyTime;
    }
    // endregion Getters/Setters

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mrmsOption");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", getType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "active", isActive() ? 1 : 0);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skillMin", getSkillMin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skillMax", getSkillMax());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "btnMin", getBthMin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "btnMax", getBthMax());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dailyTimeMin", getDailyTimeMin());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mrmsOption");
    }

    public static List<MRMSOption> parseListFromXML(Node wn, Version version) {
        List<MRMSOption> mrmsOptions = new ArrayList<>();
        NodeList nl = wn.getChildNodes();
        List<PartRepairType> partRepairTypes = PartRepairType.getMRMSValidTypes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node wn2 = nl.item(i);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                MRMSOption mrmsOption = parseFromXML(wn2);

                // This fixes a migration issue from 0.47.10 to 0.47.11
                if (version.isBetween("0.47.10", "0.47.16")
                        && (mrmsOption.getType() == PartRepairType.HEAT_SINK)) {
                    mrmsOption.setType(PartRepairType.POD_SPACE);
                }

                if ((mrmsOption.getType() == PartRepairType.UNKNOWN_LOCATION)
                        || !partRepairTypes.contains(mrmsOption.getType())) {
                    logger.error("Attempted to load MRMSOption with illegal type id of " + mrmsOption.getType());
                } else {
                    mrmsOptions.add(mrmsOption);
                }
            } catch (Exception ex) {
                logger.error("Failed to parse MRMSOption from XML", ex);
            }
        }

        return mrmsOptions;
    }

    private static MRMSOption parseFromXML(Node wn) {
        MRMSOption mrmsOption = new MRMSOption(PartRepairType.UNKNOWN_LOCATION);

        NodeList nl = wn.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn2 = nl.item(i);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    mrmsOption.setType(PartRepairType.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("active")) {
                    mrmsOption.setActive(Integer.parseInt(wn2.getTextContent().trim()) == 1);
                } else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
                    mrmsOption.setSkillMin(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("skillMax")) {
                    mrmsOption.setSkillMax(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("btnMin")) {
                    mrmsOption.setBthMin(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("btnMax")) {
                    mrmsOption.setBthMax(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("dailyTimeMin")) {
                    mrmsOption.setDailyTimeMin(Integer.parseInt(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        return mrmsOption;
    }
    // endregion File I/O
}
