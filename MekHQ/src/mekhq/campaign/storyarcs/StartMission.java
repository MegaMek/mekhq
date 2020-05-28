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

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * A StoryEvent class to start a new mission. This will pull from hash of possible missions in StoryArc. Because
 * Story missions need to be related to an actual integer id in the campaign, all story missions should be unique,
 * i.e. non-repeatable. Scenarios however can be repeatable.
 */
public class StartMission extends StoryEvent {

    UUID missionId;
    int campaignMissionId;

    /**
     * The StartMission event has no outcome variability so should choose a single next event,
     * typically an AddScenario
     **/
    UUID nextEventId;

    @Override
    public void startEvent(Campaign c) {
        Mission m = arc.getStoryMission(missionId);
        if(null != m) {
            campaignMissionId = c.addMission(m);
            //TODO: a pop-up dialog of the mission
        }
        super.startEvent(c);
        //no need for this event to stick around
        super.completeEvent(c);
    }

    @Override
    protected UUID getNextStoryEvent(Campaign c) {
        return nextEventId;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }
}
