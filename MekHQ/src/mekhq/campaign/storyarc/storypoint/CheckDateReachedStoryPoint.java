/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.storyarc.storypoint;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * StoryPoint to check whether a certain date has been reached. The
 * {@link mekhq.campaign.storyarc.StoryArc StoryArc} will
 * check for StoryPoints of this class whenever a new day is reached and if the
 * date matches, this StoryPoint will start
 * and complete.
 * <p>
 * if the exact date cannot be known ahead of time, date can be left null
 * initially and a
 * {@link mekhq.campaign.storyarc.storytrigger.SetDateStoryTrigger
 * SetDateStoryTrigger} can be used by another story
 * point to assign a date some specified number of days in the future.
 */
public class CheckDateReachedStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(CheckDateReachedStoryPoint.class);

    /**
     * The date to be checked. If null, this StoryPoint will be ignored when
     * checking a new day. Date can be
     * set later with
     * {@link mekhq.campaign.storyarc.storytrigger.SetDateStoryTrigger
     * SetDateStoryTrigger}.
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
                logger.error(e);
            }
        }
    }
}
