/*
 * GameOverStoryTrigger.java
 *
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved
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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryTrigger;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * A StoryTrigger to end the game by killing the current campaign and putting the player back in the startup screen.
 * This would typically be used for game failure (i.e. main character is killed).
 */
public class GameOverStoryTrigger extends StoryTrigger {

    @Override
    protected void execute() {
        MekHQ.unregisterHandler(getCampaign().getStoryArc());
        getCampaign().getApp().restart();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        // nothing to load
    }
}
