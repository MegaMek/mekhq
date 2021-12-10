/*
 * StoryArc.java
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
package mekhq.campaign.storyarc.storyevent;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryEvent;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;

/**
 * Extends the StoryEvent class and implements a simple narrative description that will be made visible to the player
 * immediately.
 */
public class StoryNarrative extends StoryEvent implements Serializable, MekHqXmlSerializable {

    String title;
    String narrative;

    /** narratives are linear so should link directly to another event **/
    UUID nextEventId;

    public StoryNarrative() {
        this(null, null);
    }

    public StoryNarrative(String t, String n) {
        this.title = t;
        this.narrative = n;
    }

    @Override
    public void startEvent() {
        super.startEvent();
        //TODO: create dialog and display
        completeEvent();
    }

    @Override
    protected UUID getNextStoryEvent() {
        return nextEventId;
    }


    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        // Okay, now load mission-specific fields!
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    title =wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("narrative")) {
                    narrative =wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("nextEventId")) {
                    nextEventId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }
}
