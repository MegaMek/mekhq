/*
 * CompleteMissionStoryTrigger.java
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

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.storypoint.MissionStoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;

/**
 * A trigger that completes a mission. It can optionally include information on the final victory
 * status of the mission.
 */
public class CompleteMissionStoryTrigger extends StoryTrigger {

    UUID missionStoryPointId;
    MissionStatus missionStatus;

    @Override
    protected void execute() {
        StoryPoint storyPoint = getStoryArc().getStoryPoint(missionStoryPointId);
        if (storyPoint instanceof MissionStoryPoint) {
            if (null != missionStatus) {
                ((MissionStoryPoint) storyPoint).getMission().setStatus(missionStatus);
            }
            storyPoint.complete();
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionStoryPointId", missionStoryPointId);
        if (null != missionStatus) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionStatus", missionStatus.name());
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("missionStoryPointId")) {
                    missionStoryPointId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionStatus")) {
                    missionStatus = MissionStatus.parseFromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                 LogManager.getLogger().error(e);
            }
        }
    }

}
