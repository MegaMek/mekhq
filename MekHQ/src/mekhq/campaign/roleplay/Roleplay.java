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
package mekhq.campaign.roleplay;

import java.io.PrintWriter;

import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Holds the campaign's solo-roleplay state, such as the current chaos factor used by the {@link FateChart}.
 */
public class Roleplay {
    private static final MMLogger LOGGER = MMLogger.create(Roleplay.class);

    public static final int DEFAULT_CHAOS_FACTOR = 5;

    private int chaosFactor = DEFAULT_CHAOS_FACTOR;

    /**
     * @return the current chaos factor
     */
    public int getChaosFactor() {
        return chaosFactor;
    }

    /**
     * Sets the chaos factor, clamped to the range the {@link FateChart} accepts.
     *
     * @param chaosFactor the new chaos factor
     */
    public void setChaosFactor(final int chaosFactor) {
        this.chaosFactor = FateChart.clampChaosFactor(chaosFactor);
    }

    /**
     * Raises the chaos factor by one, up to {@link FateChart#MAXIMUM_CHAOS_FACTOR}.
     */
    public void increaseChaosFactor() {
        setChaosFactor(chaosFactor + 1);
    }

    /**
     * Lowers the chaos factor by one, down to {@link FateChart#MINIMUM_CHAOS_FACTOR}.
     */
    public void decreaseChaosFactor() {
        setChaosFactor(chaosFactor - 1);
    }

    /**
     * Writes this object to the campaign save as a {@code <roleplay>} element.
     *
     * @param writer the writer to output to
     * @param indent the current indentation level
     */
    public void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "roleplay");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "chaosFactor", chaosFactor);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "roleplay");
    }

    /**
     * Creates an instance from a {@code <roleplay>} element. Unknown or malformed children are logged and skipped.
     *
     * @param node the {@code <roleplay>} node
     *
     * @return the loaded instance
     */
    public static Roleplay generateInstanceFromXML(final Node node) {
        final Roleplay roleplay = new Roleplay();
        final NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                if (child.getNodeName().equalsIgnoreCase("chaosFactor")) {
                    roleplay.setChaosFactor(Integer.parseInt(child.getTextContent().trim()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse roleplay element {}", child.getNodeName(), e);
            }
        }

        return roleplay;
    }
}
