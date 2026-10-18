/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import java.io.PrintWriter;
import java.util.Map;

import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The bombs a player wants an aircraft to carry into every scenario. When a unit has one, its bombs are topped back up
 * to it from the warehouse after each scenario (issue #6200). Internal bays and external hardpoints are kept apart, as
 * MegaMek keeps them.
 *
 * @param internalBombs the bombs carried in internal bays
 * @param externalBombs the bombs carried on external hardpoints
 */
public record PreferredBombLoadout(BombLoadout internalBombs, BombLoadout externalBombs) {
    private static final MMLogger LOGGER = MMLogger.create(PreferredBombLoadout.class);

    private static final String XML_TAG = "preferredBombLoadout";
    private static final String INTERNAL_TAG = "internalBomb";
    private static final String EXTERNAL_TAG = "externalBomb";

    /**
     * Copies both loadouts, so later changes to the aircraft's bombs do not change the preference.
     */
    public PreferredBombLoadout {
        internalBombs = new BombLoadout(internalBombs);
        externalBombs = new BombLoadout(externalBombs);
    }

    /**
     * @return {@code true} if the loadout holds at least one bomb
     */
    public boolean hasBombs() {
        return (internalBombs.getTotalBombs() + externalBombs.getTotalBombs()) > 0;
    }

    /**
     * Writes the loadout as one tag per bomb type and position, such as {@code <externalBomb>HEBomb:2</externalBomb>}.
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, XML_TAG);
        writeBombs(pw, indent, INTERNAL_TAG, internalBombs);
        writeBombs(pw, indent, EXTERNAL_TAG, externalBombs);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, XML_TAG);
    }

    private static void writeBombs(final PrintWriter pw, int indent, String tag, BombLoadout bombs) {
        for (Map.Entry<BombTypeEnum, Integer> bombAndCount : bombs.entrySet()) {
            if (bombAndCount.getValue() > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, tag,
                      bombAndCount.getKey().getInternalName() + ':' + bombAndCount.getValue());
            }
        }
    }

    /**
     * @return {@code true} if the node is a saved preferred bomb loadout
     */
    public static boolean isXMLTag(Node node) {
        return XML_TAG.equalsIgnoreCase(node.getNodeName());
    }

    /**
     * Reads a loadout written by {@link #writeToXML}. A bomb type this version does not know is skipped and logged.
     */
    public static PreferredBombLoadout generateInstanceFromXML(Node node) {
        BombLoadout internalBombs = new BombLoadout();
        BombLoadout externalBombs = new BombLoadout();
        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (INTERNAL_TAG.equalsIgnoreCase(child.getNodeName())) {
                readBomb(child.getTextContent().trim(), internalBombs);
            } else if (EXTERNAL_TAG.equalsIgnoreCase(child.getNodeName())) {
                readBomb(child.getTextContent().trim(), externalBombs);
            }
        }
        return new PreferredBombLoadout(internalBombs, externalBombs);
    }

    private static void readBomb(String bombAndCount, BombLoadout bombs) {
        int separator = bombAndCount.lastIndexOf(':');
        if (separator < 1) {
            LOGGER.warn("[BombRearm] Skipped a saved preferred bomb with no count: {}", bombAndCount);
            return;
        }
        BombTypeEnum bombType = BombTypeEnum.fromInternalName(bombAndCount.substring(0, separator));
        if ((bombType == null) || (bombType == BombTypeEnum.NONE)) {
            LOGGER.warn("[BombRearm] Skipped a saved preferred bomb of an unknown type: {}", bombAndCount);
            return;
        }
        try {
            bombs.addBombs(bombType, Integer.parseInt(bombAndCount.substring(separator + 1)));
        } catch (NumberFormatException exception) {
            LOGGER.warn("[BombRearm] Skipped a saved preferred bomb with a bad count: {}", bombAndCount);
        }
    }
}
