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

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.NO_ACCOLADE;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.MIN_CENSURE_SEVERITY;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_5;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tracks and manages judgments for factions within a campaign.
 *
 * <p>This class maintains the current judgment levels and issue date for each faction, provides methods for
 * escalating or degrading censures, and handles (de)serialization from and to XML for persistence. Censures represent
 * punitive actions that can increase (escalate) or decrease (degrade) over time based on in-game events.</p>
 *
 * <p>Typical usage involves adding or updating faction censure records, processing degradations on a new campaign
 * day, escalating censure due to events, or instantiating from XML-serialized data.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionJudgment {
    private static final MMLogger LOGGER = MMLogger.create(FactionJudgment.class);

    static final int THRESHOLD_FOR_CENSURE = 0;
    static final int THRESHOLD_FOR_ACCOLADE = STANDING_LEVEL_5.getStandingLevel();

    private final Map<String, CensureEntry> factionCensures = new HashMap<>();
    private final Map<String, AccoladeEntry> factionAccolades = new HashMap<>();

    /**
     * Constructs a new, empty {@code FactionJudgment} instance.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgment() {
    }

    /**
     * Replaces the map of current faction censures with the provided map.
     *
     * @param factionCensures the map of faction codes to {@link CensureEntry} objects
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionCensures(final Map<String, CensureEntry> factionCensures) {
        this.factionCensures.clear();
        this.factionCensures.putAll(factionCensures);
    }

    /**
     * Checks whether the specified faction currently has an active censure.
     *
     * @param factionCode the unique code identifying the faction to check
     *
     * @return {@code true} if the faction has an active censure; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean factionHasCensure(final String factionCode) {
        return factionCensures.containsKey(factionCode);
    }

    /**
     * Retrieves the censure level associated with the specified faction code.
     *
     * <p>If no censure entry exists for the given faction code, this method returns the default censure level {@link
     * FactionCensureLevel#CENSURE_LEVEL_0}.</p>
     *
     * @param factionCode the code representing the faction whose censure level is being queried
     *
     * @return the {@link FactionCensureLevel} for the specified faction, or {@code CENSURE_LEVEL_0} if none exists
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureLevel getCensureLevelForFaction(final String factionCode) {
        CensureEntry censureEntry = factionCensures.get(factionCode);
        if (censureEntry == null) {
            return CENSURE_LEVEL_0;
        }
        return censureEntry.level();
    }

    /**
     * Sets the censure level for a specific faction as of the provided date.
     *
     * @param factionCode  the faction's code
     * @param censureLevel the {@link FactionCensureLevel} to assign
     * @param today        the issue date of the new censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setCensureForFaction(final String factionCode, final FactionCensureLevel censureLevel,
          final LocalDate today) {
        CensureEntry censureEntry = new CensureEntry(censureLevel, today);
        factionCensures.put(factionCode, censureEntry);
    }

    /**
     * Processes all tracked faction censures to determine if any have expired as of the provided date, and
     * automatically degrades (reduces) the censure level for any faction whose censure has expired.
     *
     * <p>Iterates through all current censure entries, checking if each entry's expiration date has passed. If so, it
     * triggers a decrease in censure for the corresponding faction effective on the given day.</p>
     *
     * @param today the date to use when checking for censure expiration and applying any degradation
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processCensureDegradation(final LocalDate today) {
        for (Map.Entry<String, CensureEntry> entry : factionCensures.entrySet()) {
            String factionCode = entry.getKey();
            CensureEntry censureEntry = entry.getValue();

            if (censureEntry.hasExpired(today)) {
                decreaseCensureForFaction(factionCode, today);
            }
        }
    }

    /**
     * Lowers the censure level for the specified faction, if possible.
     *
     * <p>If the faction has a censure above the minimum severity, its level is reduced; otherwise, no action is
     * performed.</p>
     *
     * @param factionCode the faction's code
     * @param today       the date to use as issue date for the new, reduced censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void decreaseCensureForFaction(final String factionCode, final LocalDate today) {
        CensureEntry censureEntry = factionCensures.get(factionCode);

        if (censureEntry == null) {
            return;
        }

        FactionCensureLevel currentCensureLevel = censureEntry.level();
        int currentSeverity = currentCensureLevel.getSeverity();

        if (currentSeverity > MIN_CENSURE_SEVERITY) {
            currentSeverity--;
            FactionCensureLevel newCensureLevel = FactionCensureLevel.getCensureLevelFromSeverity(currentSeverity);

            setCensureForFaction(factionCode, newCensureLevel, today);
        }
    }

    /**
     * Raises the censure level for the specified faction if escalation is permitted.
     *
     * <p>If the faction does not have a current censure entry, sets the level to
     * {@link FactionCensureLevel#CENSURE_LEVEL_1}. If an escalation is possible (not blocked by time or rules),
     * increases the severity by one.</p>
     *
     * @param faction        the faction performing the censure
     * @param today          the current date for issue date/escalation check
     * @param activeMissions a list of active missions, used to determine whether a censure level increase is possible
     *
     * @return the new censure level if increased, {@code null} if no escalation occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionCensureLevel increaseCensureForFaction(final Faction faction, final LocalDate today,
          final List<Mission> activeMissions, final boolean campaignInTransit) {
        String factionCode = faction.getShortName();
        CensureEntry censureEntry = factionCensures.get(factionCode);

        // Determine the new censure level and check escalation
        FactionCensureLevel newCensureLevel;

        if (censureEntry == null) {
            newCensureLevel = FactionCensureLevel.CENSURE_LEVEL_1;
        } else {
            if (!censureEntry.canEscalate(today)) {
                return null;
            }
            int nextSeverity = censureEntry.level().getSeverity() + 1;
            newCensureLevel = FactionCensureLevel.getCensureLevelFromSeverity(nextSeverity);
        }

        FactionCensureAction censureAction = newCensureLevel.getFactionAppropriateAction(faction);
        boolean validOnContract = censureAction.isValidOnContract() || activeMissions.isEmpty();
        boolean validInTransit = censureAction.isValidInTransit() || !campaignInTransit;

        if (validOnContract && validInTransit) {
            setCensureForFaction(factionCode, newCensureLevel, today);
            return newCensureLevel;
        }

        return null;
    }

    /**
     * Replaces the current map of faction accolades with the provided map.
     *
     * @param factionAccolades a map where the key is the faction code and the value is the corresponding
     *                         {@link AccoladeEntry} for that faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionAccolades(final Map<String, AccoladeEntry> factionAccolades) {
        this.factionAccolades.clear();
        this.factionAccolades.putAll(factionAccolades);
    }

    /**
     * Sets the accolade level for a specific faction.
     *
     * <p>Creates a new {@link AccoladeEntry} with the given accolade level and date, replacing any existing entry
     * for the faction.</p>
     *
     * @param factionCode   the code identifying the faction
     * @param accoladeLevel the accolade level to assign to the faction
     * @param today         the date of the accolade assignment
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setAccoladeForFaction(final String factionCode, final FactionAccoladeLevel accoladeLevel,
          final LocalDate today) {
        AccoladeEntry accoladeEntry = new AccoladeEntry(accoladeLevel, today);
        factionAccolades.put(factionCode, accoladeEntry);
    }

    /**
     * Retrieves the accolade level assigned to a given faction, if any.
     *
     * <p>This method looks up the saved {@code AccoladeEntry} for the specified faction code and returns the
     * associated {@link FactionAccoladeLevel}. If the faction has no accolades recorded, this method returns
     * {@code FactionAccoladeLevel#NO_ACCOLADE}.</p>
     *
     * @param factionCode the unique string code identifying the faction whose accolade is queried
     *
     * @return the {@link FactionAccoladeLevel} for the provided faction, or {@code null} if none is present
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionAccoladeLevel getAccoladeForFaction(final String factionCode) {
        AccoladeEntry accoladeEntry = factionAccolades.get(factionCode);
        if (accoladeEntry == null) {
            return NO_ACCOLADE;
        }
        return accoladeEntry.level();
    }

    /**
     * Increases the accolade level for the specified faction if it is eligible for improvement.
     *
     * <ul>
     *     <li>If the faction does not have an existing accolade entry, a new entry with {@code FIELD_COMMENDATION}
     *     is added.</li>
     *     <li>If the accolade cannot be improved (due to date or standing), returns {@code null}.</li>
     *     <li>Otherwise, increments the accolade level and updates the entry.</li>
     * </ul>
     *
     * @param faction                    the faction issuing the accolade
     * @param today                      the current date for consideration in improvement logic
     * @param currentStandingWithFaction the current standing level with the faction, which may affect eligibility
     *
     * @return the new {@link FactionAccoladeLevel} if increased, or {@code null} if no increase was made
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionAccoladeLevel increaseAccoladeForFaction(final Faction faction, final LocalDate today,
          FactionStandingLevel currentStandingWithFaction) {
        String factionCode = faction.getShortName();
        AccoladeEntry accoladeEntry = factionAccolades.get(factionCode);

        if (accoladeEntry == null) {
            setAccoladeForFaction(factionCode, FactionAccoladeLevel.TAKING_NOTICE_0, today);
            LOGGER.debug("Faction {} has no accolade entry, assigning TAKING_NOTICE_0", factionCode);
            return FactionAccoladeLevel.TAKING_NOTICE_0;
        }

        if (!accoladeEntry.canImprove(today, currentStandingWithFaction)) {
            LOGGER.debug("Faction {} cannot improve accolade, skipping", factionCode);
            return null;
        }

        FactionAccoladeLevel currentAccoladeLevel = accoladeEntry.level();
        int currentRecognition = currentAccoladeLevel.getRecognition();
        currentRecognition++;
        FactionAccoladeLevel updatedAccoladeLevel = FactionAccoladeLevel.getAccoladeRecognitionFromRecognition(
              currentRecognition);

        if (faction.isMercenaryOrganization() && !updatedAccoladeLevel.isMercenarySuitable()) {
            LOGGER.debug("Faction {} cannot improve accolade due to mercenary suitability", factionCode);
            return null;
        }

        if (factionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE) && !updatedAccoladeLevel.isPirateSuitable()) {
            LOGGER.debug("Faction {} cannot improve accolade due to pirate suitability", factionCode);
            return null;
        }

        // This accolade requires an active contract
        if (updatedAccoladeLevel.is(FactionAccoladeLevel.TRIUMPH_OR_REMEMBRANCE)) {
            LOGGER.debug("Faction {} cannot improve accolade due to lack of active contract", factionCode);
            return null;
        }

        LOGGER.debug("Increasing accolade level for faction {} to {}", factionCode, updatedAccoladeLevel);
        setAccoladeForFaction(factionCode, updatedAccoladeLevel, today);

        return updatedAccoladeLevel;
    }

    /**
     * Writes the current faction censure data as XML to the given writer.
     *
     * @param writer the {@link PrintWriter} to write to
     * @param indent the indentation level for pretty-printing the XML
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void writeFactionJudgmentToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionCensures");
        for (String factionCode : factionCensures.keySet()) {
            CensureEntry censureEntry = factionCensures.get(factionCode);

            if (censureEntry != null) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, factionCode);
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "level", censureEntry.level().name());
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "issueDate", censureEntry.issueDate().toString());
                MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, factionCode);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionCensures");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionAccolades");
        for (String factionCode : factionAccolades.keySet()) {
            AccoladeEntry accoladeEntry = factionAccolades.get(factionCode);

            if (accoladeEntry != null) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, factionCode);
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "level", accoladeEntry.level().name());
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "issueDate", accoladeEntry.issueDate().toString());
                MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, factionCode);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionAccolades");
    }

    /**
     * Constructs a new {@link FactionJudgment} instance by reading serialized censure data from an XML node.
     *
     * @param parentNode the XML parent node containing the censure entries
     *
     * @return a new {@link FactionJudgment} instance populated from the XML data
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionJudgment generateInstanceFromXML(final Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();

        FactionJudgment judgements = new FactionJudgment();
        try {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                String nodeName = childNode.getNodeName();

                if (nodeName.equalsIgnoreCase("factionCensures")) {
                    judgements.setFactionCensures(processCensureEntries(childNode));
                }
                if (nodeName.equalsIgnoreCase("factionAccolades")) {
                    judgements.setFactionAccolades(processAccoladeEntries(childNode));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return judgements;
    }

    /**
     * Reads all faction censure entries from the given XML node and returns them as a map.
     *
     * @param parentNode the XML node containing child nodes for each faction's censure entry
     *
     * @return a map of faction code to {@link CensureEntry} read from the XML
     *
     * @author Illiani
     * @since 0.50.07
     */
    static Map<String, CensureEntry> processCensureEntries(Node parentNode) {
        Map<String, CensureEntry> entries = new HashMap<>();
        NodeList childNodes = parentNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String factionCode = node.getNodeName();
                CensureEntry entry = readCensureEntryFromFactionNode(node);
                entries.put(factionCode, entry);
            }
        }
        return entries;
    }

    /**
     * Creates a {@link CensureEntry} by reading censure data from the given XML node.
     *
     * @param codeNode the XML node containing "level" and "issueDate" elements
     *
     * @return a {@link CensureEntry} populated from the node's content
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static CensureEntry readCensureEntryFromFactionNode(Node codeNode) {
        FactionCensureLevel level = CENSURE_LEVEL_0;
        LocalDate issueDate = LocalDate.MIN;

        NodeList props = codeNode.getChildNodes();
        for (int i = 0; i < props.getLength(); i++) {
            Node propNode = props.item(i);
            if (propNode.getNodeType() == Node.ELEMENT_NODE) {
                String name = propNode.getNodeName();
                String value = propNode.getTextContent().trim();

                if (name.equalsIgnoreCase("level")) {
                    level = FactionCensureLevel.getCensureLevelFromCensureString(value);
                } else if (name.equalsIgnoreCase("issueDate")) {
                    issueDate = LocalDate.parse(value);
                }
            }
        }

        return new CensureEntry(level, issueDate);
    }

    /**
     * Reads all faction accolade entries from the given XML node and returns them as a map.
     *
     * @param parentNode the XML node containing child nodes for each faction's accolade entry
     *
     * @return a map of faction code to {@link AccoladeEntry} read from the XML
     *
     * @author Illiani
     * @since 0.50.07
     */
    static Map<String, AccoladeEntry> processAccoladeEntries(Node parentNode) {
        Map<String, AccoladeEntry> entries = new HashMap<>();
        NodeList childNodes = parentNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String factionCode = node.getNodeName();
                AccoladeEntry entry = readAccoladeEntryFromFactionNode(node);
                entries.put(factionCode, entry);
            }
        }
        return entries;
    }

    /**
     * Creates a {@link AccoladeEntry} by reading accolade data from the given XML node.
     *
     * @param codeNode the XML node containing "level" and "issueDate" elements
     *
     * @return a {@link AccoladeEntry} populated from the node's content
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static AccoladeEntry readAccoladeEntryFromFactionNode(Node codeNode) {
        FactionAccoladeLevel level = NO_ACCOLADE;
        LocalDate issueDate = LocalDate.MIN;

        NodeList props = codeNode.getChildNodes();
        for (int i = 0; i < props.getLength(); i++) {
            Node propNode = props.item(i);
            if (propNode.getNodeType() == Node.ELEMENT_NODE) {
                String name = propNode.getNodeName();
                String value = propNode.getTextContent().trim();

                if (name.equalsIgnoreCase("level")) {
                    level = FactionAccoladeLevel.getAccoladeRecognitionFromString(value);
                } else if (name.equalsIgnoreCase("issueDate")) {
                    issueDate = LocalDate.parse(value);
                }
            }
        }

        return new AccoladeEntry(level, issueDate);
    }
}
