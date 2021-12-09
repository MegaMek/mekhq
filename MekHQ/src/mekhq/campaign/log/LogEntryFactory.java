/*
 * Copyright (C) 2018 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.log;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;

/**
 * This class is responsible for instantiating the desired log entries from xml nodes.
 * @author Miguel Azevedo
 */
public class LogEntryFactory {
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
     * Creates a new log entry based on its type. Used for xml unmarshelling
     * @param date date of the log
     * @param desc description of the log
     * @param type type of the log
     * @return the new log entry
     */
    public LogEntry generateNew(LocalDate date, String desc, LogEntryType type){
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
     * @param wn xml node
     * @return log entry
     */
    public LogEntry generateInstanceFromXML(Node wn) {
        LocalDate date = null;
        String desc = null;
        LogEntryType type = null;

        try {
            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node   node = nl.item(x);
                String nname = node.getNodeName();
                switch (nname) {
                    case "desc":
                        desc = MekHqXmlUtil.unEscape(node.getTextContent());
                        break;
                    case "type":
                        String typeString = MekHqXmlUtil.unEscape(node.getTextContent());
                        type = LogEntryType.valueOf(ensureBackwardCompatibility(typeString));
                        break;
                    case "date":
                        date = MekHqXmlUtil.parseDate(node.getTextContent().trim());
                        break;
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
            return null;
        }

        String newDescription = LogEntryController.updateOldDescription(desc);
        if (!newDescription.isEmpty()) {
            desc = newDescription;
        }

        if (type == null) {
            type = LogEntryController.determineTypeFromLogDescription(desc);
        }

        return generateNew(date, desc, type);
    }

    /**
     * Replaces the old "med" tag for medical log entries with the new MedicalLogEntry
     * @param logType the old log entry type
     * @return the new log entry type
     */
    private static String ensureBackwardCompatibility(String logType) {
        if (logType.equalsIgnoreCase("med")) {
            return LogEntryType.MEDICAL.toString();
        } else {
            return logType;
        }
    }
}
