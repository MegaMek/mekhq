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

import megamek.Version;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * This class governs what happens when a story point is completed. It will override the default outcomes for that
 * story point
 */
public class StoryOutcome implements MekHqXmlSerializable {

    /** result this outcome is tied too **/
    String result;

    /** id of the next story point to start. Can be null **/
    private UUID nextStoryPointId;

    /** A list of StoryTriggers to replace the defaults on this outcome */
    List<StoryTrigger> storyTriggers;

    StoryOutcome()  {
        storyTriggers = new ArrayList<>();
    }

    public String getResult() { return result; }

    public UUID getNextStoryPointId() { return nextStoryPointId; }

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
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "storyOutcome", "result", result);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "nextStoryPointId", nextStoryPointId);
        if(!storyTriggers.isEmpty()) {
            for (StoryTrigger trigger : storyTriggers) {
                trigger.writeToXml(pw1, indent);
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "storyOutcome");
    }

    public static StoryOutcome generateInstanceFromXML(Node wn, Campaign c, Version v) {
        StoryOutcome retVal = null;

        try {
            retVal = new StoryOutcome();

            retVal.result = wn.getAttributes().getNamedItem("result").getTextContent().trim();

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("nextStoryPointId")) {
                    retVal.nextStoryPointId = UUID.fromString(wn2.getTextContent().trim());
                } else if(wn2.getNodeName().equalsIgnoreCase("storyTrigger")) {
                    StoryTrigger trigger = StoryTrigger.generateInstanceFromXML(wn2, c, v);
                    retVal.storyTriggers.add(trigger);
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error(ex);
        }

        return retVal;
    }

}
