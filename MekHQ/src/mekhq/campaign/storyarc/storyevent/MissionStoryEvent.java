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
import mekhq.campaign.mission.Mission;
import mekhq.campaign.storyarc.StoryEvent;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * A StoryEvent class to start a new mission. This will pull from hash of possible missions in StoryArc. Because
 * Story missions need to be related to an actual integer id in the campaign, all story missions should be unique,
 * i.e. non-repeatable. Scenarios however can be repeatable.
 */
public class MissionStoryEvent extends StoryEvent implements Serializable, MekHqXmlSerializable {

    /* A mission object to track the mission */
    private Mission mission;

    /**
     * A list of ScenarioStoryEvent ids to add to this mission when it is created. not all Scenarios
     * need to be added at the start as ScenarioStoryEvents may also trigger further ScenarioStoryEvents
     */
    private List<UUID> scenarioIds;

    public MissionStoryEvent() {
        super();
        scenarioIds = new ArrayList<UUID>();
    }

    @Override
    public void startEvent() {
        super.startEvent();
        if(null != mission) {
            getStoryArc().setCurrentMissionId(getStoryArc().getCampaign().addMission(mission));
        }
        StoryEvent scenarioEvent;
        for(UUID scenarioId : scenarioIds) {
            scenarioEvent = getStoryArc().getStoryEvent(scenarioId);
            if(null != scenarioEvent) {
                scenarioEvent.startEvent();
            }
        }
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
                if (wn2.getNodeName().equalsIgnoreCase("mission")) {
                    mission = Mission.generateInstanceFromXML(wn2, c, null);
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    scenarioIds.add(UUID.fromString(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }
}
