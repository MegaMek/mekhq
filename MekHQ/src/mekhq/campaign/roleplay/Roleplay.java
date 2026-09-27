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

import static megamek.common.compute.Compute.randomInt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Holds the campaign's solo-roleplay state: the current chaos factor used by the {@link FateChart}, the ordered
 * list of characters the Oracle can pick from when a random event involves an NPC, and the campaign's
 * {@link PlotThread}s.
 */
public class Roleplay {
    private static final MMLogger LOGGER = MMLogger.create(Roleplay.class);

    public static final int DEFAULT_CHAOS_FACTOR = 5;

    private int chaosFactor = DEFAULT_CHAOS_FACTOR;
    private final List<String> characters = new ArrayList<>();
    private final List<PlotThread> plotThreads = new ArrayList<>();

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
     * @return the live, ordered list of Oracle characters; changes to it are saved with the campaign
     */
    public List<String> getCharacters() {
        return characters;
    }

    /**
     * Adds a character to the end of the Oracle list. Blank names, and names already on the list, are ignored.
     *
     * @param name the character's name
     *
     * @return {@code true} if the character was added
     */
    public boolean addCharacter(final @Nullable String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        final String trimmed = name.trim();
        if (characters.contains(trimmed)) {
            return false;
        }
        return characters.add(trimmed);
    }

    /**
     * Picks a random character from the Oracle list.
     *
     * @return a random character, or {@code null} if the list is empty
     */
    public @Nullable String pickRandomCharacter() {
        if (characters.isEmpty()) {
            return null;
        }
        return characters.get(randomInt(characters.size()));
    }

    /**
     * @return the live, ordered list of plot threads; changes to it are saved with the campaign
     */
    public List<PlotThread> getPlotThreads() {
        return plotThreads;
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
        if (!characters.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "characters");
            for (String character : characters) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "character", character);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "characters");
        }
        if (!plotThreads.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "plotThreads");
            for (PlotThread thread : plotThreads) {
                thread.writeToXML(writer, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "plotThreads");
        }
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
                } else if (child.getNodeName().equalsIgnoreCase("characters")) {
                    final NodeList characterNodes = child.getChildNodes();
                    for (int j = 0; j < characterNodes.getLength(); j++) {
                        final Node characterNode = characterNodes.item(j);
                        if (characterNode.getNodeName().equalsIgnoreCase("character")) {
                            roleplay.addCharacter(characterNode.getTextContent());
                        }
                    }
                } else if (child.getNodeName().equalsIgnoreCase("plotThreads")) {
                    final NodeList threadNodes = child.getChildNodes();
                    for (int j = 0; j < threadNodes.getLength(); j++) {
                        final Node threadNode = threadNodes.item(j);
                        if (threadNode.getNodeName().equalsIgnoreCase("plotThread")) {
                            final PlotThread thread = PlotThread.generateInstanceFromXML(threadNode);
                            if (thread != null) {
                                roleplay.plotThreads.add(thread);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse roleplay element {}", child.getNodeName(), e);
            }
        }

        return roleplay;
    }
}
