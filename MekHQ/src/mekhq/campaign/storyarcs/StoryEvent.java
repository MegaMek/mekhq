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
import mekhq.campaign.Campaign;

import java.util.UUID;

/**
 * The StoryEvent abstract class is the basic building block of a StoryArc. StoryEvents can do
 * different things when they are started. When they are completed they may start other events as
 * determined by the specific class and user input. StoryEvents are started in one of the following ways:
 *  - By being selected as the next event by a prior StoryEvent
 *  - By meeting the trigger conditions that are checked in various places in Campaign such as a specific date
 **/
public abstract class StoryEvent implements MekHqXmlSerializable {

    StoryArc arc;

    boolean active;

    public StoryEvent() {
        active = false;
    }

    /**
     * Determine whether the event should be triggered by some feature of the campaign
     * @param c A MekHQ Campaign
     * @return boolean for whether the event is triggered
     */
    private boolean isTriggered(Campaign c) {
        return false;
    }

    public void checkTriggered(Campaign c) {
        if(isTriggered(c)) {
            startEvent(c);
        }
    }

    /**
     * Do whatever needs to be done to start this event. Specific event types may need to override this and then
     * call the super at the end.
     * @param c
     */
    public void startEvent(Campaign c) {
        active = true;
    }

    /**
     * Complete the event. Specific event types may need to override this.
     * @param c A MekHQ Campaign
     */
    public void completeEvent(Campaign c) {
        active = false;
        proceedToNextStoryEvent(c);
    }

    /**
     * Gets the next story event and if it is not null, starts it
     */
    protected void proceedToNextStoryEvent(Campaign c) {
        //get the next story event
        UUID nextStoryEventId = getNextStoryEvent(c);
        StoryEvent nextStoryEvent = arc.getStoryEvent(nextStoryEventId);
        if(null != nextStoryEvent) {
            nextStoryEvent.startEvent(c);
        }
    }

    /** determine the next story event in the story arc based on the event **/
    protected abstract UUID getNextStoryEvent(Campaign c);

}
