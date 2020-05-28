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
 * Adds a scenario to the identified mission. Note that it will also create an id on the given scenario in campaign and
 * that scenario will trigger completeEvent upon completion.
 */
public class AddScenario extends StoryEvent {

    UUID missionId;
    UUID scenarioId;

    /** for now we will force a linear narrative **/
    UUID nextEventId;

    @Override
    public void startEvent(Campaign c) {
        super.startEvent(c);
        Mission m = arc.getCampaignMission(missionId, c);
        Scenario s = arc.getStoryScenario(scenarioId);
        if (null != m & null != s) {
            m.addScenario(s);
            //need some way to add the scenario UUID to Scenario so Scenario can check for completion
        }
        //this event should stick around until the scenario is completed
    }

    @Override
    protected UUID getNextStoryEvent(Campaign c) {
        //TODO: not yet properly implemented because we need logic here
        return nextEventId;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }
}
