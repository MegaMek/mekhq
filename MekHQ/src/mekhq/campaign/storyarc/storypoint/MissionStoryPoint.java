/*
 * MissionStoryPoint.java
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
package mekhq.campaign.storyarc.storypoint;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * A StoryPoint class to start a new mission. This will pull from hash of possible missions in StoryArc. Because
 * Story missions need to be related to an actual integer id in the campaign, all story missions should be unique,
 * i.e. non-repeatable. Scenarios however can be repeatable.
 */
public class MissionStoryPoint extends StoryPoint implements Serializable, MekHqXmlSerializable {

    /* A mission object to track the mission */
    private Mission mission;

    /**
     * A list of ScenarioStoryPoint ids to add to this mission when it is created. not all Scenarios
     * need to be added at the start as ScenarioStoryPoints may also trigger further ScenarioStoryPoints
     */
    private List<UUID> scenarioStoryPointIds;

    public MissionStoryPoint() {
        super();
        scenarioStoryPointIds = new ArrayList<UUID>();
    }

    @Override
    public String getTitle() {
        if(null != mission) {
            return mission.getName();
        }
        return "";
    }

    @Override
    public void start() {
        super.start();
        if(null != mission) {
            getStoryArc().getCampaign().addMission(mission);
        }
        StoryPoint scenarioStoryPoint;
        for(UUID scenarioStoryPointId : scenarioStoryPointIds) {
            scenarioStoryPoint = getStoryArc().getStoryPoint(scenarioStoryPointId);
            if(null != scenarioStoryPoint) {
                scenarioStoryPoint.start();
            }
        }
    }

    @Override
    public void complete() {
        //Its possible mission status has already changed, but if not then set to failed for now for testing
        //TODO: create some logic for determining success of mission in cases where user does not specify
        if(null != mission && mission.getStatus().isActive()) {
            mission.setStatus(MissionStatus.FAILED);
        }
        super.complete();
    }

    public Mission getMission() { return mission; }

    @Override
    public String getResult() {
        if(null == mission || mission.getStatus().isActive()) {
            return "";
        }
        return mission.getStatus().name();
    }

    @Override
    public String getObjective() {
        return "Complete " + mission.getName() + " mission";
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        for(UUID scenarioStoryPointId : scenarioStoryPointIds) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<scenarioStoryPointId>"
                    +scenarioStoryPointId
                    +"</scenarioStoryPointId>");
        }
        if(null != mission) {
            //if the mission has a valid id, then just save this because the mission is saved
            //and loaded elsewhere so we need to link it
            if(mission.getId() > 0) {
                pw1.println(MekHqXmlUtil.indentStr(indent+1)
                        +"<missionId>"
                        +mission.getId()
                        +"</missionId>");
            } else {
                mission.writeToXml(pw1, indent+1);
            }
        }
        writeToXmlEnd(pw1, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        // Okay, now load mission-specific fields!
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
                    int missionId = Integer.parseInt(wn2.getTextContent().trim());
                    if(null != c) {
                        mission = c.getMission(missionId);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("mission")) {
                    mission = Mission.generateInstanceFromXML(wn2, c, null);
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioStoryPointId")) {
                    scenarioStoryPointIds.add(UUID.fromString(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
