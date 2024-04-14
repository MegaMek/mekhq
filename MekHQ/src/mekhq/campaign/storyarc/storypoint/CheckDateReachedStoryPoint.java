/*
 * CheckDateReachedStoryPoint.java
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
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;

/**
 * StoryPoint to check whether a certain date has been reached. The {@link mekhq.campaign.storyarc.StoryArc StoryArc} will
 * check for StoryPoints of this class whenever a new day is reached and if the date matches, this StoryPoint will start
 * and complete.
 * <p>if the exact date cannot be known ahead of time, date can be left null initially and a
 * {@link mekhq.campaign.storyarc.storytrigger.SetDateStoryTrigger SetDateStoryTrigger} can be used by another story
 * point to assign a date some specified number of days in the future.
 */
public class CheckDateReachedStoryPoint extends StoryPoint {

    /**
     * The date to be checked. If null, this StoryPoint will be ignored when checking a new day. Date can be
     * set later with {@link mekhq.campaign.storyarc.storytrigger.SetDateStoryTrigger SetDateStoryTrigger}.
     */
    private LocalDate date;

    public CheckDateReachedStoryPoint() {
        super();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String getTitle() {
        return "Date reached";
    }

    @Override
    protected String getResult() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        complete();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "date", date);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    date = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
