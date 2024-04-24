/*
 * WaitStoryPoint.java
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
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;

/**
 * A StoryPoint that waits until a certain number of days pass before completing.
 */
public class WaitStoryPoint extends StoryPoint {

    String title;
    int days;
    private LocalDate date;

    public WaitStoryPoint() {
        super();
        title = "";
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    protected String getResult() {
        //this one has no variation
        return "";
    }

    @Override
    public String getObjective() {
        return title;
    }

    @Override
    public void start() {
        super.start();
        // convert number of days to an actual date that we can check
        date = getCampaign().getLocalDate().plusDays(days);
        // refresh for objectives
        getCampaign().getApp().getCampaigngui().refreshAllTabs();
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "title", title);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "date", date);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "days", days);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    title = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    date = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("days")) {
                    days = Integer.parseInt(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
