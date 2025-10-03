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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.service.mrms;

import static mekhq.campaign.personnel.skills.SkillUtilities.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillUtilities.EXP_ULTRA_GREEN;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MRMSOption {
    private static final MMLogger LOGGER = MMLogger.create(MRMSOption.class);

    // region Variable Declarations
    private PartRepairType type;
    private boolean active;
    private int skillMin;
    private int skillMax;
    private int targetNumberPreferred;
    private int targetNumberMax;
    private int dailyTimeMin;

    private static final int TARGET_NUMBER_PREFERRED = 4;
    private static final int TARGET_NUMBER_MAX = 6;
    private static final int DAILY_TIME_MIN = 0;
    // endregion Variable Declarations

    // region Constructors
    public MRMSOption(PartRepairType type) {
        this(type,
              false,
              EXP_ULTRA_GREEN,
              EXP_LEGENDARY,
              TARGET_NUMBER_PREFERRED,
              TARGET_NUMBER_MAX,
              DAILY_TIME_MIN);
    }

    public MRMSOption(PartRepairType type, boolean active, int skillMin, int skillMax, int targetNumberPreferred,
          int targetNumberMax, int dailyTimeMin) {
        this.type = type;
        this.active = active;
        this.skillMin = skillMin;
        this.skillMax = skillMax;
        this.targetNumberPreferred = targetNumberPreferred;
        this.targetNumberMax = targetNumberMax;
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

    public int getTargetNumberPreferred() {
        return targetNumberPreferred;
    }

    /**
     * @deprecated consider {@link #getTargetNumberPreferred()}
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getBthMin() {
        return this.getTargetNumberPreferred();
    }

    public void setTargetNumberPreferred(int targetNumberPreferred) {
        this.targetNumberPreferred = targetNumberPreferred;
    }

    /**
     * @deprecated consider {@link #setTargetNumberPreferred(int)}
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setBthMin(int bthMin) {
        this.setTargetNumberPreferred(bthMin);
    }

    public int getTargetNumberMax() {
        return targetNumberMax;
    }

    /**
     * @deprecated consider {@link #getTargetNumberMax()}
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getBthMax() {
        return this.getTargetNumberMax();
    }

    public void setTargetNumberMax(int targetNumberMax) {
        this.targetNumberMax = targetNumberMax;
    }

    /**
     * @deprecated consider {@link #setTargetNumberMax(int)}
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setBthMax(int bthMax) {
        this.setTargetNumberMax(bthMax);
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
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "targetNumberPreferred", getTargetNumberPreferred());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "targetNumberMax", getTargetNumberMax());
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
                if (version.isBetween("0.47.10", "0.47.16") && (mrmsOption.getType() == PartRepairType.HEAT_SINK)) {
                    mrmsOption.setType(PartRepairType.POD_SPACE);
                }

                if ((mrmsOption.getType() == PartRepairType.UNKNOWN_LOCATION) ||
                          !partRepairTypes.contains(mrmsOption.getType())) {
                    LOGGER.error("Attempted to load MRMSOption with illegal type id of " + mrmsOption.getType());
                } else {
                    mrmsOptions.add(mrmsOption);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to parse MRMSOption from XML", ex);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("targetNumberPreferred")) {
                    mrmsOption.setTargetNumberPreferred(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("targetNumberMax")) {
                    mrmsOption.setTargetNumberMax(Integer.parseInt(wn2.getTextContent().trim()));

                } else if (wn2.getNodeName().equalsIgnoreCase("dailyTimeMin")) {
                    mrmsOption.setDailyTimeMin(Integer.parseInt(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        return mrmsOption;
    }
    // endregion File I/O
}
