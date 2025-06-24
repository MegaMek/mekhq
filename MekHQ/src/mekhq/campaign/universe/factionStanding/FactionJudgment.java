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

import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.MIN_CENSURE_SEVERITY;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_4;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FactionJudgment {
    private static final MMLogger LOGGER = MMLogger.create(FactionJudgment.class);

    static final int THRESHOLD_FOR_CENSURE = STANDING_LEVEL_4.getStandingLevel();

    private final Map<String, CensureEntry> factionCensures = new HashMap<>();

    public FactionJudgment() {
    }

    public void setFactionCensures(final Map<String, CensureEntry> factionCensures) {
        this.factionCensures.clear();
        this.factionCensures.putAll(factionCensures);
    }

    public void setCensureForFaction(final String factionCode, final FactionCensureLevel censureLevel,
          final LocalDate today) {
        CensureEntry censureEntry = new CensureEntry(censureLevel, today);
        factionCensures.put(factionCode, censureEntry);
    }

    /**
     * Processes all tracked faction censures to determine if any have expired as of the provided date, and
     * automatically degrades (reduces) the censure level for any faction whose censure has expired.
     * <p>
     * Iterates through all current censure entries, checking if each entry's expiration date has passed. If so, it
     * triggers a decrease in censure for the corresponding faction effective on the given day.
     * </p>
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

    public @Nullable FactionCensureLevel increaseCensureForFaction(final String factionCode, final LocalDate today) {
        CensureEntry censureEntry = factionCensures.get(factionCode);

        if (censureEntry == null) {
            setCensureForFaction(factionCode, FactionCensureLevel.WARNING, today);
            return FactionCensureLevel.WARNING;
        }

        if (!censureEntry.canEscalate(today)) {
            return null;
        }

        FactionCensureLevel currentCensureLevel = censureEntry.level();
        int currentSeverity = currentCensureLevel.getSeverity();
        currentSeverity++;
        FactionCensureLevel newCensureLevel = FactionCensureLevel.getCensureLevelFromSeverity(currentSeverity);

        setCensureForFaction(factionCode, newCensureLevel, today);
        return newCensureLevel;
    }

    public void writeFactionJudgmentToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionCensures");
        for (String factionCode : factionCensures.keySet()) {
            CensureEntry censureEntry = factionCensures.get(factionCode);

            if (censureEntry != null) {
                MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, factionCode);
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "level", censureEntry.level().toString());
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "issueDate", censureEntry.issueDate().toString());
                MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, factionCode);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionCensures");
    }

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
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return judgements;
    }

    static Map<String, CensureEntry> processCensureEntries(Node parentNode) {
        Map<String, CensureEntry> entries = new HashMap<>();
        NodeList childNodes = parentNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String factionCode = node.getNodeName(); // here: "DC", or whatever code
                CensureEntry entry = readFromFactionNode(node);
                if (entry != null) {
                    entries.put(factionCode, entry);
                }
            }
        }
        return entries;
    }

    private static CensureEntry readFromFactionNode(Node codeNode) {
        FactionCensureLevel level = FactionCensureLevel.NONE;
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
}
