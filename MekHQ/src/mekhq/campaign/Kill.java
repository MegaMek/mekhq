/*
 * Kill.java
 *
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.campaign;

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A kill record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Kill {
    private UUID pilotId;
    private LocalDate date;
    private String killed;
    private String killer;

    public Kill() {
    }

    public Kill(UUID id, String kill, String killer, LocalDate d) {
        pilotId = id;
        this.killed = kill;
        this.killer = killer;
        date = d;
    }

    public UUID getPilotId() {
        return pilotId;
    }

    public void setPilotId(UUID id) {
        pilotId = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getWhatKilled() {
        return killed;
    }

    public String getKilledByWhat() {
        return killer;
    }

    public void setDate(LocalDate d) {
        date = d;
    }

    public void setWhatKilled(String s) {
        killed = s;
    }

    public void setKilledByWhat(String s) {
        killer = s;
    }

    public static Kill generateInstanceFromXML(Node wn, Version version) {
        Kill retVal = null;
        try {
            retVal = new Kill();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("killed")) {
                    retVal.killed = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("pilotId")) {
                    retVal.pilotId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("killer")) {
                    retVal.killer = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.date = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            LogManager.getLogger().error("", ex);
        }
        return retVal;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenIndentedLine(pw1, indent++, "kill");
        MHQXMLUtility.writeSimpleXmlTag(pw1, indent, "pilotId", pilotId);
        MHQXMLUtility.writeSimpleXmlTag(pw1, indent, "killed", killed);
        MHQXMLUtility.writeSimpleXmlTag(pw1, indent, "killer", killer);
        MHQXMLUtility.writeSimpleXmlTag(pw1, indent, "date", MHQXMLUtility.saveFormattedDate(date));
        MHQXMLUtility.writeSimpleXMLCloseIndentedLine(pw1, --indent, "kill");
    }

    @Override
    public Kill clone() {
        return new Kill(getPilotId(), getWhatKilled(), getKilledByWhat(), getDate());
    }
}
