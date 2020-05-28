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
import java.io.PrintWriter;
import java.util.UUID;

/**
 * Extends the StoryEvent class and implements a simple narrative description that will be made visible to the player
 * immediately.
 */
public class StoryNarrative extends StoryEvent {

    String title;
    String narrative;

    /** narratives are linear so should link directly to another event **/
    UUID nextEventId;

    public StoryNarrative() {

    }

    public StoryNarrative(String t, String n) {
        this.title = t;
        this.narrative = n;
    }

    @Override
    public void startEvent(Campaign c) {
        super.startEvent(c);
        //TODO: create dialog and display
        completeEvent(c);
    }

    @Override
    protected UUID getNextStoryEvent(Campaign c) {
        return nextEventId;
    }


    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

    }
}
