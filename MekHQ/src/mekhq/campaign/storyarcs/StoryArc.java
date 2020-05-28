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
package mekhq.campaign.storyarcs;

import mekhq.MekHqXmlSerializable;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.Campaign;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Story Arc class manages a given story arc campaign
 */
public class StoryArc implements MekHqXmlSerializable {

    private String title;
    private String description;

    /** Can this story arc be added to existing campaign or does it need to start fresh? **/
    private boolean startNew;

    /** A UUID for the initial event in this track  - can be null **/
    private UUID startingEventId;

    /** A hash of all possible StoryEvents in this StoryArc, referenced by UUID **/
    private Map<UUID, StoryEvent> storyEvents = new LinkedHashMap<>();

    /** A hash of all possible Missions in this StoryArc, referenced by UUID **/
    private Map<UUID, Mission> storyMissions = new LinkedHashMap<>();

    /** A hash of all possible Scenarios in this StoryArc, referenced by UUID **/
    private Map<UUID, Scenario> storyScenarios = new LinkedHashMap<>();

    /**
     * We need to track a hash that relates active Story Missions to their actual integer id
     * in the Campaign in order to be able to add scenarios to the proper mission
     */
    private Map<UUID, Integer> campaignMissionIds = new LinkedHashMap<>();


    public StoryArc() {
        startNew = false;
    }

    public StoryEvent getStoryEvent(UUID id) {
        if (id == null) {
            return null;
        }
        return storyEvents.get(id);
    }

    public Mission getStoryMission(UUID id) {
        if (id == null) {
            return null;
        }
        return storyMissions.get(id);
    }

    public Scenario getStoryScenario(UUID id) {
        if (id == null) {
            return null;
        }
        return storyScenarios.get(id);
    }

    public void addMissionId(UUID missionId, int campaignId) {
        campaignMissionIds.put(missionId, campaignId);
    }

    public Mission getCampaignMission(UUID missionId, Campaign c) {
        int campaignMissionId = campaignMissionIds.get(missionId);
        return c.getMission(campaignMissionId);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }
}
