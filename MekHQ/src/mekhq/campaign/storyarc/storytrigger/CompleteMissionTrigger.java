/*
 * CompleteMissionTrigger.java
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
package mekhq.campaign.storyarc.storytrigger;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.StoryEvent;
import mekhq.campaign.storyarc.storyevent.MissionStoryEvent;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;

/**
 * A trigger that completes a mission. It can optionally include information on the final victory
 * status of the mission
 */
public class CompleteMissionTrigger extends StoryTrigger implements Serializable, MekHqXmlSerializable {

    UUID missionEventId;
    MissionStatus missionStatus;

    @Override
    protected void execute() {
        StoryEvent storyEvent = getStoryArc().getStoryEvent(missionEventId);
        if(storyEvent instanceof MissionStoryEvent) {
            if(null != missionStatus) {
                ((MissionStoryEvent) storyEvent).getMission().setStatus(missionStatus);
            }
            storyEvent.completeEvent();
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<storyEventId>"
                +missionEventId
                +"</storyEventId>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<missionStatus>"
                +missionStatus.name()
                +"</missionStatus>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("missionEventId")) {
                    missionEventId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionStatus")) {
                    missionStatus = MissionStatus.parseFromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }

}
