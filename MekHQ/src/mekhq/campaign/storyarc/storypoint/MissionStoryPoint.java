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

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * A StoryPoint class to start a new mission. A MissionStoryPoint will not complete until triggered by a
 * {@link mekhq.campaign.storyarc.storytrigger.CompleteMissionStoryTrigger CompleteMissionStoryTrigger}. That trigger
 * may include the final status of the mission (i.e. Success, Failure). If it does not, the MissionStoryPoint will
 * determine success based on the percentage of successful scenarios. The default will be 50%, but this can be adjusted
 * with the <code>percentWin</code> variable.
 * <p>A MissionStoryPoint can also include a list of starting UUIDs that identify
 * {@link ScenarioStoryPoint ScenarioStoryPoint} objects, but is not required to do so as scenarios can also be assigned
 * to the mission later through a{@link ScenarioStoryPoint ScenarioStoryPoint}. If multiple scenarios are included,
 * keep in mind that the player can decide which order they are completed. If the author wants to specify order, it is
 * better to start with one scenario and when that scenario is completed it can point to another
 * {@link ScenarioStoryPoint ScenarioStoryPoint}, and so on until the final scenario is reached at which point a
 * {@link mekhq.campaign.storyarc.storytrigger.CompleteMissionStoryTrigger CompleteMissionStoryTrigger} is triggered.</p>
 */
public class MissionStoryPoint extends StoryPoint {

    /* A mission object to track the mission */
    private Mission mission;

    /**
     * A double that tracks what percent of scenarios must be successful for successful mission. This
     * may not be relevant if mission will be closed in other ways.
     **/
    private double percentWin;

    /**
     * A list of ScenarioStoryPoint ids to add to this mission when it is created. not all Scenarios
     * need to be added at the start as ScenarioStoryPoints may also trigger further ScenarioStoryPoints
     */
    private List<UUID> scenarioStoryPointIds;

    public MissionStoryPoint() {
        super();
        scenarioStoryPointIds = new ArrayList<UUID>();
        // set 50% as the default win rate
        percentWin = 0.5;
    }

    @Override
    public String getTitle() {
        if (null != mission) {
            return mission.getName();
        }
        return "";
    }

    @Override
    public void start() {
        super.start();
        if (null != mission) {
            getStoryArc().getCampaign().addMission(mission);
        }
        StoryPoint scenarioStoryPoint;
        for (UUID scenarioStoryPointId : scenarioStoryPointIds) {
            scenarioStoryPoint = getStoryArc().getStoryPoint(scenarioStoryPointId);
            if (null != scenarioStoryPoint) {
                scenarioStoryPoint.start();
            }
        }
    }

    @Override
    public void complete() {
        if (null != mission && mission.getStatus().isActive()) {
            //if mission status is still active then we need to figure out the correct status based on percent
            //of successful scenarios
            double wins = mission.getCompletedScenarios().stream().filter(s -> s.getStatus().isOverallVictory()).count();
            if ((!mission.getCompletedScenarios().isEmpty()) &&
                    ((wins/mission.getCompletedScenarios().size()) >= percentWin)) {
                mission.setStatus(MissionStatus.SUCCESS);
            } else {
                mission.setStatus(MissionStatus.FAILED);
            }
        }
        super.complete();
    }

    public Mission getMission() { return mission; }

    @Override
    public String getResult() {
        if (null == mission || mission.getStatus().isActive()) {
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
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "percentWin", percentWin);
        for (UUID scenarioStoryPointId : scenarioStoryPointIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "scenarioStoryPointId", scenarioStoryPointId);
        }
        if (null != mission) {
            //if the mission has a valid id, then just save this because the mission is saved
            //and loaded elsewhere, so we need to link it
            if (mission.getId() > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionId", mission.getId());
            } else {
                mission.writeToXML(pw1, indent);
            }
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
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
                    mission = Mission.generateInstanceFromXML(wn2, c, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioStoryPointId")) {
                    scenarioStoryPointIds.add(UUID.fromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("percentWin")) {
                    percentWin = Double.parseDouble(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
