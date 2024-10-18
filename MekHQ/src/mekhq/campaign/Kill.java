/*
 * Kill.java
 *
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
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
    private int missionId = 0;
    private int scenarioId = 0;
    private int forceId = 0;
    private long unitType = 0;

    private static final MMLogger logger = MMLogger.create(Kill.class);

    public Kill() {
    }

    public Kill(UUID id, String kill, String killer, LocalDate d, int missionId, int scenarioId, int forceId,
            long unitType) {
        pilotId = id;
        this.killed = kill;
        this.killer = killer;
        date = d;
        this.missionId = missionId;
        this.scenarioId = scenarioId;
        this.forceId = forceId;
        this.unitType = unitType;
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

    public void setDate(LocalDate d) {
        date = d;
    }

    public String getWhatKilled() {
        return killed;
    }

    public void setWhatKilled(String s) {
        killed = s;
    }

    public String getKilledByWhat() {
        return killer;
    }

    public void setKilledByWhat(String s) {
        killer = s;
    }

    public int getMissionId() {
        return missionId;
    }

    public void setMissionId(int id) {
        missionId = id;
    }

    public int getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(int id) {
        scenarioId = id;
    }

    public int getForceId() {
        return forceId;
    }

    public void setForceId(final int id) {
        forceId = id;
    }

    /**
     * Returns a unique identifier for the award based on the combination of various factors.
     * <p>
     * This is used by autoAwards to identify duplicate kills across multi-crewed units.
     *
     * @return The string representation of the award identifier combining {@code killed},
     * {@code missionId}, {@code scenarioId}, {@code forceId}, and {@code unitType}
     */
    public String getAwardIdentifier() {
        return killed + missionId + scenarioId + forceId + unitType;
    }

    /**
     * @return the long corresponding to the Entity type killed,
     *         or -1 if the kill does not have a unit type logged
     */

    public long getUnitType() {
        return unitType;
    }

    public void setUnitType(final long type) {
        unitType = type;
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
                } else if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
                    retVal.missionId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("unitType")) {
                    retVal.unitType = Long.parseLong(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            logger.error("", ex);
        }
        return retVal;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "kill");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pilotId", pilotId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killed", killed);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killer", killer);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", date);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionId", missionId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioId", scenarioId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceId", forceId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitType", unitType);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "kill");
    }

    @Override
    public Kill clone() {
        return new Kill(getPilotId(), getWhatKilled(), getKilledByWhat(), getDate(), getMissionId(), getScenarioId(),
                getForceId(), getUnitType());
    }
}
