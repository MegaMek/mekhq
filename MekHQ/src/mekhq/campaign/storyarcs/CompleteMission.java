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

import java.io.PrintWriter;
import java.util.UUID;

public class CompleteMission extends StoryEvent {

    UUID missionId;

    public CompleteMission(StoryArc a) {
        super(a);
    }

    @Override
    public void startEvent() {
        super.startEvent();
        Mission m = arc.getCampaignMission(missionId, arc.getCampaign());
        if(null != m) {
            //TODO: review some criteria to determine status, but for now assume everyone wins
            m.setStatus(Mission.S_SUCCESS);
            //TODO: a pop-up dialog of a description for missions end
        }
        //no need for this event to stick around
        super.completeEvent();
    }

    @Override
    protected UUID getNextStoryEvent() {
        //TODO: need some setup to decide next StoryEvent based on concluding status of this mission
        return null;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }
}
