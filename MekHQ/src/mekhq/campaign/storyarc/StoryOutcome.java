/*
 * StoryOutcome.java
 *
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved
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
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * This class governs what happens when a story event is completed. Every StoryEvent will have a StoryOutcome
 * associated with it.
 */
public class StoryOutcome implements MekHqXmlSerializable {

    /** result this outcome is tied too **/
    String result;

    /** id of the next event to start. Can be null **/
    private UUID nextEventId;

    /** A list of StoryTriggers to replace the defaults on this outcome */
    List<StoryTrigger> storyTriggers;

    protected static final String NL = System.lineSeparator();

    StoryOutcome()  {
        storyTriggers = new ArrayList<>();
    }

    public String getResult() { return result; }

    public UUID getNextEventId() { return nextEventId; }

    public List<StoryTrigger> getStoryTriggers() { return storyTriggers; }

    /**
     * Set the StoryArc on all StoryTriggers here
     * @param a
     */
    public void setStoryArc(StoryArc a) {
        for(StoryTrigger storyTrigger : storyTriggers) {
            storyTrigger.setStoryArc(a);
        }
    }

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        String level = MekHqXmlUtil.indentStr(indent),
                level1 = MekHqXmlUtil.indentStr(indent + 1);

        StringBuilder builder = new StringBuilder(256);
        builder.append(level)
                .append("<storyOutcome result=\"")
                .append(result)
                .append("\">")
                .append(NL)
                .append(level1)
                .append("<nextEventId>")
                .append(nextEventId)
                .append("</nextEventId>")
                .append(NL);
        pw1.print(builder.toString());
        if(!storyTriggers.isEmpty()) {
            for (StoryTrigger trigger : storyTriggers) {
                trigger.writeToXml(pw1, indent + 1);
            }
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</storyOutcome>");
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
                } else if(wn2.getNodeName().equalsIgnoreCase("storyTrigger")) {
                    StoryTrigger trigger = StoryTrigger.generateInstanceFromXML(wn2, c);
                    retVal.storyTriggers.add(trigger);
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

}
