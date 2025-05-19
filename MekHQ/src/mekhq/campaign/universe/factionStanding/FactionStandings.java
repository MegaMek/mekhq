/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores and manages the standing values between factions in the Faction Standings system.
 *
 * <p>A {@link FactionStandings} object tracks the current fame values for all relevant factions using faction codes
 * as keys and numeric fame as values. Values may be positive (good reputation), negative (bad reputation), or zero
 * (neutral). This class provides functionality to initialize standings according to relationships, adjust and degrade
 * fame values, serialize data to XML, and reconstruct state from XML.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandings {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandings.class);

    private static final double DEFAULT_FAME = 0.0;
    private static final double DEFAULT_FAME_DEGRADATION = 0.25;

    private Map<String, Double> factionStandings;

    /**
     * Constructs an empty standings map. No initial relationships or fame values are set.
     *
     * <p><b>Usage:</b> this does not populate the 'standing' map with any values. That has to be handled
     * separately.</p>
     *
     * <p>If we're starting a new campaign, we should follow up object construction with a call to
     * {@link #initializeStartingFameValues(Faction, LocalDate)}</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandings() {
        this.factionStandings = new HashMap<>();
    }

    /**
     * Initializes the 'standings' map based on the campaign faction and the specific date.
     *
     * <p>Factions are assigned fame values based on whether they are direct allies, direct enemies, or have
     * secondary relationships (e.g., friends of allies/enemies of enemies).</p>
     *
     * <p>The initialization is performed in two passes:</p>
     * <ul>
     *     <li><b>Pass 1:</b> Sets high fame for direct allies and low fame for direct enemies, collecting these
     *     groups.</li>
     *     <li><b>Pass 2:</b> Assigns intermediate fame to factions indirectly related to the campaign faction (e.g.,
     *     friends of enemies), avoiding those already processed.</li>
     * </ul>
     *
     * <p>Fame values assigned are based on the {@link FactionStandingLevel} configuration.</p>
     *
     * <p><b>Usage:</b> generally we should only be using this when a campaign is first created or when we're wholly
     * resetting standings mid-campaign. For all other use cases, we should be manually populating the 'standings' map
     * to ensure we're not wiping progress.</p>
     *
     * @param campaignFaction the main faction whose perspective is used for all relationships
     * @param today           the current campaign date used for determining relationships
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void initializeStartingFameValues(Faction campaignFaction, LocalDate today) {
        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        double fameSameFaction = FactionStandingLevel.STANDING_LEVEL_1.getMinimumFame();
        double fameAlly = fameSameFaction / 2;
        double fameEnemy = FactionStandingLevel.STANDING_LEVEL_3.getMaximumFame();
        double fameEnemyOfAEnemy = fameEnemy / 2;

        // Pass 1: Determine primary allies/enemies and set immediate fame
        Set<Faction> allies = new HashSet<>();
        Set<Faction> enemies = new HashSet<>();

        for (Faction otherFaction : allFactions) {
            if (otherFaction.equals(campaignFaction)) {
                setFameForFaction(otherFaction.getShortName(), fameSameFaction);
                continue;
            }

            if (factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                setFameForFaction(otherFaction.getShortName(), fameAlly);
                allies.add(otherFaction);
            } else if (factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                setFameForFaction(otherFaction.getShortName(), fameEnemy);
                enemies.add(otherFaction);
            }
        }

        // Pass 2: Friends of enemies
        for (Faction otherFaction : allFactions) {
            if (otherFaction.equals(campaignFaction) ||
                      allies.contains(otherFaction) ||
                      enemies.contains(otherFaction)) {
                continue;
            }

            // Check if ally of enemy
            boolean isAllyOfEnemy = enemies.stream()
                                          .anyMatch(enemy -> factionHints.isAlliedWith(enemy, otherFaction, today));

            // set fame
            if (isAllyOfEnemy) {
                setFameForFaction(otherFaction.getShortName(), fameEnemyOfAEnemy);
            }
        }
    }

    /**
     * Replaces the current map of faction standings with the provided map.
     *
     * <p>Existing contents are discarded. After this call, only the entries in the given map remain.</p>
     *
     * @param factionStandings the new map of faction codes to fame values
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionStandings(Map<String, Double> factionStandings) {
        this.factionStandings = factionStandings;
    }

    /**
     * Retrieves all current faction standings.
     *
     * @return a {@link Map} containing all faction codes mapped to their current fame values.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<String, Double> getAllFactionStandings() {
        return factionStandings;
    }

    /**
     * Retrieves the current fame value for the specified faction.
     *
     * @param factionCode a unique code identifying the faction
     *
     * @return the fame value for the faction, or 0 if none is present
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getFameForFaction(final String factionCode) {
        return factionStandings.getOrDefault(factionCode, DEFAULT_FAME);
    }

    /**
     * Sets the fame value for the specified faction, directly assigning (or overwriting) the value.
     *
     * <p>If the faction code does not already exist, a new entry is created.</p>
     *
     * @param factionCode a unique code identifying the faction
     * @param fame        the fame (standing) value to assign
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFameForFaction(final String factionCode, final double fame) {
        changeFameForFaction(factionCode, fame);
    }

    /**
     * Adjusts the fame value for a specific faction by a given amount.
     *
     * <p>The current fame value for the faction is retrieved and incremented by {@code delta}. If the faction code
     * does not already exist, it is created with the value {@code delta}.</p>
     *
     * @param factionCode a unique code identifying the faction
     * @param delta       the amount to add to the faction's current standing
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void changeFameForFaction(final String factionCode, final double delta) {
        double originalFame = getFameForFaction(factionCode);
        double newFame = originalFame + delta;

        factionStandings.put(factionCode, newFame);
    }

    /**
     * Clears all faction standings, removing all records.
     *
     * <p>After this call, the 'standings' map is empty and no reputations are stored.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void wipeAllFactionStandings() {
        factionStandings.clear();
    }

    /**
     * Removes the standing entry for a single faction only.
     *
     * @param factionCode the code of the faction to remove
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void resetFactionStanding(final String factionCode) {
        factionStandings.remove(factionCode);
    }


    /**
     * Gradually shifts all non-zero faction standings toward zero by a fixed increment per call.
     *
     * <p>Fame values are adjusted by {@code -0.25} if positive and by {@code +0.25} if negative. If the new value
     * crosses zero, it is clamped to exactly zero, preventing fame "overshooting."</p>
     *
     * <p>This method simulates the natural decay of fame over time.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processFameDegradation() {
        for (String factionCode : new HashSet<>(factionStandings.keySet())) {
            double currentFame = factionStandings.get(factionCode);

            if (currentFame != DEFAULT_FAME) {
                double delta = currentFame > DEFAULT_FAME ? -DEFAULT_FAME_DEGRADATION : DEFAULT_FAME_DEGRADATION;
                changeFameForFaction(factionCode, delta);
                double newFame = getFameForFaction(factionCode);

                if ((currentFame > DEFAULT_FAME && newFame < DEFAULT_FAME) ||
                          (currentFame < DEFAULT_FAME && newFame > DEFAULT_FAME)) {
                    setFameForFaction(factionCode, DEFAULT_FAME);
                }
            }
        }
    }

    /**
     * Writes all faction standings as XML out to the specified {@link PrintWriter}.
     *
     * <p>The output includes each faction code and its current fame value as a separate tag, indented for
     * readability within a parent <standings> element.</p>
     *
     * <p>This is primarily used for saving campaign data.</p>
     *
     * @param writer the writer to output XML to
     * @param indent the indentation level for formatting
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void writeFactionStandingsToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "standings");
        for (String factionCode : factionStandings.keySet()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, factionCode, factionStandings.get(factionCode).toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "standings");
    }

    /**
     * Creates a new {@code FactionStandings} instance by parsing standing values from an XML node.
     *
     * <p>This method reads child elements of the provided XML node, looking for a "standings" element.</p>
     *
     * <p></p>For each faction code found as a subelement, it extracts the faction's fame value from the element's text
     * content. Parsed standing values are collected into a map, and then applied to a new {@link FactionStandings}
     * instance.</p>
     *
     * <p>If any parsing errors occur for individual entries, an error is logged and the process continues.</p>
     *
     * <p></p>If the entire node cannot be parsed, an error is logged and an (empty) {@link FactionStandings} is still
     * returned.</p>
     *
     * @param parentNode the XML node containing faction standings data, typically as a parent "standings" element
     *
     * @return a new {@link FactionStandings} instance populated with parsed standing values from the XML, or empty if
     *       nothing could be read
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionStandings generateInstanceFromXML(final Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();

        FactionStandings standings = new FactionStandings();

        Map<String, Double> factionStandings = new HashMap<>();
        try {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                String nodeName = childNode.getNodeName();

                if (nodeName.equalsIgnoreCase("standings")) {
                    NodeList factionEntries = childNode.getChildNodes();

                    for (int factionEntry = 0; factionEntry < factionEntries.getLength(); factionEntry++) {
                        Node node = factionEntries.item(factionEntry);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            try {
                                factionStandings.put(node.getNodeName(),
                                      MathUtility.parseDouble(node.getTextContent(), DEFAULT_FAME));
                            } catch (Exception ex) {
                                LOGGER.error("Could not parse {}: ", node.getNodeName(), ex);
                            }
                        }
                    }
                }
            }

            standings.setFactionStandings(factionStandings);
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return standings;
    }
}
