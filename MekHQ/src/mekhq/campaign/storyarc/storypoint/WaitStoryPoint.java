/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.storyarc.storypoint;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A StoryPoint that waits until a certain number of days pass before completing.
 */
public class WaitStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(WaitStoryPoint.class);

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
        // this one has no variation
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
                logger.error(e);
            }
        }
    }
}
