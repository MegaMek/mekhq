/*
 * StoryOutcome.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.storyarc;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.campaign.Campaign;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * This class governs what happens when a story event is completed. Every StoryEvent will have a StoryOutcome
 * associated with it.
 */
public class StoryOutcome implements MekHqXmlSerializable {

    /** result this outcome is tied too **/
    String result;

    /** id of the next event to start. Can be null **/
    private UUID nextEventId;

    StoryOutcome()  {
        //nothing to assign in the constructor
    }

    public String getResult() { return result; }

    private void setNextEventId(UUID id) { this.nextEventId = id; }

    public UUID getNextEventId() { return nextEventId; }

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }

    public static StoryOutcome generateInstanceFromXML(Node wn, Campaign c) {
        StoryOutcome retVal = null;
        NamedNodeMap attrs = wn.getAttributes();

        try {
            retVal = new StoryOutcome();

            retVal.result = wn.getAttributes().getNamedItem("result").getTextContent().trim();

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("nextEventId")) {
                    retVal.nextEventId = UUID.fromString(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

}
