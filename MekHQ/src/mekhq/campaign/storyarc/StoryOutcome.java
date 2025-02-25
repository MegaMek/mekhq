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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;

/**
 * This class controls what happens when a story point is completed and a
 * certain result is achieved. Basically, it
 * tracks an alternate `nextStoryPointId` and a list of {@link StoryTrigger
 * StoryTrigger} objects that will replace
 * the default
 */
public class StoryOutcome {
    private static final MMLogger logger = MMLogger.create(StoryOutcome.class);

    /** result this outcome is tied too **/
    String result;

    /** id of the next story point to start. Can be null **/
    private UUID nextStoryPointId;

    /** A list of StoryTriggers to replace the defaults on this outcome */
    List<StoryTrigger> storyTriggers;

    public StoryOutcome()  {
        storyTriggers = new ArrayList<>();
    }

    public String getResult() {
        return result;
    }

    public void setResult(String r) {
        this.result = r;
    }

    public UUID getNextStoryPointId() {
        return nextStoryPointId;
    }

    public void setNextStoryPointId(UUID id) {
        this.nextStoryPointId = id;
    }

    public List<StoryTrigger> getStoryTriggers() {
        return storyTriggers;
    }

    public void setStoryTriggers(List<StoryTrigger> triggers) {
        this.storyTriggers = triggers;
    }

    /**
     * Set the StoryArc on all StoryTriggers here
     *
     * @param a a {@link StoryArc StoryArc}
     */
    public void setStoryArc(StoryArc a) {
        for (StoryTrigger storyTrigger : storyTriggers) {
            storyTrigger.setStoryArc(a);
        }
    }

    // region File I/O
    public void writeToXml(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "storyOutcome", "result", result);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "nextStoryPointId", nextStoryPointId);
        if (!storyTriggers.isEmpty()) {
            for (StoryTrigger trigger : storyTriggers) {
                trigger.writeToXml(pw1, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "storyOutcome");
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
                } else if (wn2.getNodeName().equalsIgnoreCase("storyTrigger")) {
                    StoryTrigger trigger = StoryTrigger.generateInstanceFromXML(wn2, c, v);
                    retVal.storyTriggers.add(trigger);
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }

        return retVal;
    }

}
