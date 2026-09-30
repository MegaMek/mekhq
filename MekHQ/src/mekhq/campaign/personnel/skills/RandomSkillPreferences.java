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
package mekhq.campaign.personnel.skills;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson
 */
public class RandomSkillPreferences {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptions.class);

    private int overallRecruitBonus;
    Map<PersonnelRole, Integer> recruitmentBonuses;
    private boolean randomizeSkill;
    private boolean useAttributes;
    private boolean randomizeAttributes;
    private boolean randomizeTraits;
    private boolean useClanBonuses;
    private int antiMekProb;
    private final int[] specialAbilityBonus;
    private int combatSmallArmsBonus;
    private int supportSmallArmsBonus;
    private final int[] commandSkillsModifier;
    private final int[] utilitySkillsModifier;
    private int roleplaySkillModifier;
    private int artilleryProb;
    private int artilleryBonus;
    private int secondSkillProb;
    private int secondSkillBonus;

    public RandomSkillPreferences() {
        overallRecruitBonus = 0;
        recruitmentBonuses = new HashMap<>();
        randomizeSkill = true;
        useAttributes = false;
        randomizeAttributes = false;
        randomizeTraits = false;
        useClanBonuses = true;
        antiMekProb = 10;
        combatSmallArmsBonus = -3;
        supportSmallArmsBonus = -10;
        specialAbilityBonus = new int[] { -10, -10, -2, 0, 1, 1, 1 };
        commandSkillsModifier = new int[] { -12, -11, -10, -9, -8, -7, -7 };
        utilitySkillsModifier = new int[] { -12, -11, -10, -9, -8, -7, -7 };
        roleplaySkillModifier = -12;
        artilleryProb = 10;
        artilleryBonus = -2;
        secondSkillProb = 0;
        secondSkillBonus = -4;
    }

    public int getOverallRecruitBonus() {
        return overallRecruitBonus;
    }

    public void setOverallRecruitBonus(int b) {
        overallRecruitBonus = b;
    }

    /**
     * Retrieves the current recruitment bonus values for different personnel roles.
     *
     * @return A map containing personnel roles as keys and their associated recruitment bonus values as integers
     */
    public Map<PersonnelRole, Integer> getRecruitmentBonuses() {
        return recruitmentBonuses;
    }

    /**
     * Adds or updates a recruitment bonus for a specific personnel role.
     *
     * @param role  The personnel role to set a bonus for
     * @param bonus The integer value representing the recruitment bonus
     */
    public void addRecruitmentBonus(final PersonnelRole role, final int bonus) {
        recruitmentBonuses.put(role, bonus);
    }

    /**
     * Retrieves the recruitment bonus value for a specific personnel role.
     *
     * <p>If no bonus has been defined for the requested role, this method returns 0.</p>
     *
     * @param role The personnel role to get the recruitment bonus for
     *
     * @return The integer bonus value for the specified role, or 0 if no bonus is defined
     */
    public int getRecruitmentBonus(final PersonnelRole role) {
        return recruitmentBonuses.getOrDefault(role, 0);
    }

    public int getSpecialAbilityBonus(int type) {
        return (type < specialAbilityBonus.length) ? specialAbilityBonus[type] : 0;
    }

    public void setSpecialAbilityBonus(int type, int bonus) {
        if (type < specialAbilityBonus.length) {
            specialAbilityBonus[type] = bonus;
        }
    }

    public boolean randomizeSkill() {
        return randomizeSkill;
    }

    public void setRandomizeSkill(boolean b) {
        this.randomizeSkill = b;
    }

    public boolean isUseAttributes() {
        return useAttributes;
    }

    public void setUseAttributes(boolean useAttributes) {
        this.useAttributes = useAttributes;
    }

    public boolean isRandomizeAttributes() {
        return randomizeAttributes;
    }

    public void setRandomizeAttributes(boolean isRandomizeAttributes) {
        this.randomizeAttributes = isRandomizeAttributes;
    }

    public boolean isRandomizeTraits() {
        return randomizeTraits;
    }

    public void setRandomizeTraits(boolean randomizeTraits) {
        this.randomizeTraits = randomizeTraits;
    }

    public int getAntiMekProb() {
        return antiMekProb;
    }

    public void setAntiMekProb(int b) {
        antiMekProb = b;
    }

    public int getCombatSmallArmsBonus() {
        return combatSmallArmsBonus;
    }

    public void setCombatSmallArmsBonus(int b) {
        combatSmallArmsBonus = b;
    }

    public int getSupportSmallArmsBonus() {
        return supportSmallArmsBonus;
    }

    public void setSupportSmallArmsBonus(int b) {
        supportSmallArmsBonus = b;
    }

    public int getCommandSkillsModifier(int lvl) {
        return commandSkillsModifier[lvl];
    }

    public void setCommandSkillsMod(int lvl, int bonus) {
        if (lvl < commandSkillsModifier.length) {
            commandSkillsModifier[lvl] = bonus;
        }
    }

    public int getUtilitySkillsModifier(int lvl) {
        return utilitySkillsModifier[lvl];
    }

    public void setUtilitySkillsMod(int lvl, int bonus) {
        if (lvl < utilitySkillsModifier.length) {
            utilitySkillsModifier[lvl] = bonus;
        }
    }

    public int getRoleplaySkillModifier() {
        return roleplaySkillModifier;
    }

    public void setRoleplaySkillModifier(int roleplaySkillModifier) {
        this.roleplaySkillModifier = roleplaySkillModifier;
    }

    public void setArtilleryProb(int b) {
        this.artilleryProb = b;
    }

    public int getArtilleryProb() {
        return artilleryProb;
    }

    public void setArtilleryBonus(int b) {
        this.artilleryBonus = b;
    }

    public int getArtilleryBonus() {
        return artilleryBonus;
    }

    public void setSecondSkillProb(int b) {
        this.secondSkillProb = b;
    }

    public int getSecondSkillProb() {
        return secondSkillProb;
    }

    public void setSecondSkillBonus(int b) {
        this.secondSkillBonus = b;
    }

    public int getSecondSkillBonus() {
        return secondSkillBonus;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "randomSkillPreferences");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overallRecruitBonus", overallRecruitBonus);
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "recruitmentBonuses");
        for (final Entry<PersonnelRole, Integer> entry : recruitmentBonuses.entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "recruitmentBonuses");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialAbilityBonus", specialAbilityBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandSkillsModifier", commandSkillsModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "utilitySkillsModifier", utilitySkillsModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "roleplaySkillModifier", roleplaySkillModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeSkill", randomizeSkill);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAttributes", useAttributes);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeAttributes", randomizeAttributes);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeTraits", randomizeTraits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useClanBonuses", useClanBonuses);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "antiMekProb", antiMekProb);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "combatSmallArmsBonus", combatSmallArmsBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportSmallArmsBonus", supportSmallArmsBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "artilleryProb", artilleryProb);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "artilleryBonus", artilleryBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "secondSkillProb", secondSkillProb);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "secondSkillBonus", secondSkillBonus);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "randomSkillPreferences");
    }

    public static RandomSkillPreferences generateRandomSkillPreferencesFromXml(Node wn, Version version) {
        LOGGER.debug("Loading Random Skill Preferences from XML...");

        wn.normalize();
        RandomSkillPreferences retVal = new RandomSkillPreferences();
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            LOGGER.debug("{}: {}", wn2.getNodeName(), wn2.getTextContent());

            try {
                if (wn2.getNodeName().equalsIgnoreCase("overallRecruitBonus")) {
                    retVal.overallRecruitBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("randomizeSkill")) {
                    retVal.randomizeSkill = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("useAttributes")) {
                    retVal.useAttributes = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("randomizeAttributes")) {
                    retVal.randomizeAttributes = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("randomizeTraits")) {
                    retVal.randomizeTraits = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("useClanBonuses")) {
                    retVal.useClanBonuses = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("antiMekProb")) {
                    retVal.antiMekProb = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("combatSmallArmsBonus")) {
                    retVal.combatSmallArmsBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("supportSmallArmsBonus")) {
                    retVal.supportSmallArmsBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("artilleryProb")) {
                    retVal.artilleryProb = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("artilleryBonus")) {
                    retVal.artilleryBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondSkillProb")) {
                    retVal.secondSkillProb = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondSkillBonus")) {
                    retVal.secondSkillBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("recruitmentBonuses")) {
                    processRecruitmentBonusNodes(wn2, retVal);
                } else if (wn2.getNodeName().equalsIgnoreCase("commandSkillsModifier")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.commandSkillsModifier[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("utilitySkillsModifier")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.utilitySkillsModifier[i] = MathUtility.parseInt(values[i], -1);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("roleplaySkillModifier")) {
                    retVal.roleplaySkillModifier = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("specialAbilityBonus")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.specialAbilityBonus[i] = Integer.parseInt(values[i]);
                    }
                }
            } catch (Exception ex) {
                LOGGER.debug(ex, "Unknown Exception - generateRandomSkillPreferencesFromXML");
            }
        }

        LOGGER.debug("Load Random Skill Preferences Complete!");

        return retVal;
    }

    /**
     * Processes XML nodes containing recruitment bonus information and populates the random skill preferences.
     *
     * <p>This method parses child nodes where each node name represents a {@link PersonnelRole} enum value and
     * the node's text content represents the bonus value as an integer. Each parsed role-bonus pair is added into the
     * specified {@link RandomSkillPreferences} object. If parsing fails for any node, an error is logged.</p>
     *
     * @param node             The parent XML node containing recruitment bonus child nodes
     * @param skillPreferences The {@link RandomSkillPreferences} object to populate with recruitment bonuses
     */
    private static void processRecruitmentBonusNodes(Node node, RandomSkillPreferences skillPreferences) {
        if (!node.hasChildNodes()) {
            return;
        }

        final NodeList nodes = node.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            final Node workingNode = nodes.item(j);
            if (workingNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = workingNode.getNodeName().trim();
            String nodeValue = workingNode.getTextContent().trim();

            try {
                skillPreferences.addRecruitmentBonus(PersonnelRole.valueOf(nodeName), Integer.parseInt(nodeValue));
            } catch (Exception ex) {
                LOGGER.error(ex, "Failed to process recruitment bonus node: {}, {}", nodeName, nodeValue);
            }
        }
    }
}
