/*
 * Copyright (C) 2018-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

package mekhq.campaign.log;

import java.time.LocalDate;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;

/**
 * This class is responsible for instantiating the desired log entries from xml
 * nodes.
 *
 * @author Miguel Azevedo
 */
public class LogEntryFactory {
    private static final MMLogger logger = MMLogger.create(LogEntryFactory.class);

    private static LogEntryFactory logEntryFactory = null;

    public LogEntryFactory() {

    }

    public static LogEntryFactory getInstance() {
        if (logEntryFactory == null) {
            logEntryFactory = new LogEntryFactory();
        }
        return logEntryFactory;
    }

    /**
     * Creates a new log entry based on its type. Used for xml unmarshalling
     *
     * @param date date of the log
     * @param desc description of the log
     * @param type type of the log
     * @return the new log entry
     */
    public LogEntry generateNew(LocalDate date, String desc, LogEntryType type) {
        switch (type) {
            case MEDICAL:
                return new MedicalLogEntry(date, desc);
            case AWARD:
                return new AwardLogEntry(date, desc);
            case SERVICE:
                return new ServiceLogEntry(date, desc);
            case PERSONAL:
                return new PersonalLogEntry(date, desc);
            case HISTORICAL:
                return new HistoricalLogEntry(date, desc);
            case CUSTOM:
            default:
                return new CustomLogEntry(date, desc);
        }
    }

    /**
     * Generates a log entry from a node
     *
     * @param wn xml node
     * @return log entry
     */
    public @Nullable LogEntry generateInstanceFromXML(Node wn) {
        LocalDate date = null;
        String desc = null;
        LogEntryType type = null;

        try {
            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node node = nl.item(x);
                switch (node.getNodeName()) {
                    case "desc":
                        desc = MHQXMLUtility.unEscape(node.getTextContent());
                        break;
                    case "type":
                        type = LogEntryType.valueOf(node.getTextContent());
                        break;
                    case "date":
                        date = MHQXMLUtility.parseDate(node.getTextContent().trim());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }

        return generateNew(date, desc, type);
    }
}
