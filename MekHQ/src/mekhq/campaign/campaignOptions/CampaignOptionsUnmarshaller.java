/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.campaignOptions;

import static java.lang.Boolean.parseBoolean;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.ALL;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.SUPPORT;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CampaignOptionsUnmarshaller {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsUnmarshaller.class);

    public static CampaignOptions generateCampaignOptionsFromXml(Node parentNod, Version version) {
        LOGGER.info("Loading Campaign Options from Version {} XML...", version);

        parentNod.normalize();
        CampaignOptions campaignOptions = new CampaignOptions();
        NodeList childNodes = parentNod.getChildNodes();

        boolean sawRandomTalent = false;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = childNode.getNodeName();
            String nodeContents = childNode.getTextContent().trim();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}\n\t{}", nodeName, nodeContents);
            }

            if (CampaignOption.USE_RANDOM_TALENT.xmlTag().equals(nodeName)) {
                sawRandomTalent = true;
            }

            try {
                parseNodeName(version, nodeName, campaignOptions, nodeContents, childNode);
            } catch (Exception ex) {
                LOGGER.error(ex, "Exception parsing campaign option node: {}", nodeName);
            }
        }

        // Talent (Reasoning) was coupled to random personalities before it gained its own toggle. Saves written before
        // that split carry no useRandomTalent tag, so inherit the personality setting to preserve their behavior.
        if (!sawRandomTalent) {
            campaignOptions.set(CampaignOption.USE_RANDOM_TALENT,
                  campaignOptions.get(CampaignOption.USE_RANDOM_PERSONALITIES));
        }

        LOGGER.debug("Load Campaign Options Complete!");
        return campaignOptions;
    }

    /**
     * Applies one campaign-options XML node. Almost every tag is handled generically by
     * {@link CampaignOptionCodecs#readTag}, keyed by {@link CampaignOption#xmlTag()} (plus legacy read aliases). Only a
     * few obsolete tags need bespoke migration logic that maps to no single option value; those are handled here.
     */
    private static void parseNodeName(Version version, String nodeName, CampaignOptions campaignOptions,
          String nodeContents, Node childNode) {
        if (CampaignOptionCodecs.readTag(nodeName, childNode, nodeContents, version, campaignOptions)) {
            return;
        }

        switch (nodeName) {
            // Legacy acquisition skill: a free-text label mapped onto the modern AcquisitionsType.
            case "acquisitionSkill" -> {
                AcquisitionsType newType = switch (nodeContents) {
                    case "Administration" -> AcquisitionsType.ADMINISTRATION;
                    case "Negotiation" -> AcquisitionsType.NEGOTIATION;
                    case "Automatic Success" -> AcquisitionsType.AUTOMATIC;
                    default -> AcquisitionsType.ANY_TECH;
                };
                campaignOptions.set(CampaignOption.ACQUISITIONS_TYPE, newType);
            }
            // Legacy boolean replaced by the ProcurementPersonnelPick category.
            case "acquisitionSupportStaffOnly" ->
                  campaignOptions.set(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY, parseBoolean(nodeContents) ? SUPPORT : ALL);
            // < 50.10 compatibility: booleans replaced by the StratConPlayType enum.
            case "useStratCon" -> {
                if (parseBoolean(nodeContents)) {
                    // Can still be overwritten by 'useMaplessStratCon' if that clause is hit before this one.
                    if (campaignOptions.get(CampaignOption.STRAT_CON_PLAY_TYPE) == StratConPlayType.DISABLED) {
                        campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.NORMAL);
                    }
                }
            }
            case "useMaplessStratCon" -> {
                if (parseBoolean(nodeContents)) {
                    campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.MAPLESS);
                }
            }
            // Legacy boolean replaced by the alternative advanced medical healing-time multiplier.
            case "useKinderAlternativeAdvancedMedical" -> {
                if (parseBoolean(nodeContents)) {
                    campaignOptions.set(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER, 0.5);
                }
            }
            default -> LOGGER.warn("Potentially unexpected entry in campaign options: {}", nodeName);
        }
    }
}
