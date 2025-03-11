/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.storyarc.storytrigger;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storypoint.CheckDateReachedStoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * This StoryTrigger will set the date in a
 * {@link CheckDateReachedStoryPoint CheckDateReachedStoryPoint} identified by
 * its id.
 * This can be used to assign dates to events where the date might not be known
 * in advance. The date can be assigned
 * either by an actual date or by the number of days into the future from the
 * point of this trigger.
 */
public class SetDateStoryTrigger extends StoryTrigger {
    private static final MMLogger logger = MMLogger.create(SetDateStoryTrigger.class);

    // region Variable Declarations
    /** The id of the CheckDateReachedStoryPoint that should be changed **/
    private UUID storyPointId;

    /**
     * the date to be changed to. This can be null if this trigger uses number of
     * days instead
     **/
    private LocalDate date;

    /** The number of days from the present when this event should happen **/
    private int futureDays;
    // endregion Variable Declarations

    // region Constructors
    public SetDateStoryTrigger() {
        super();
        // set the default to be one day into the future in case it is missing
        futureDays = 1;
    }
    // endregion Constructors

    @Override
    protected void execute() {
        StoryPoint storyPoint = getStoryArc().getStoryPoint(storyPointId);
        if (!(storyPoint instanceof CheckDateReachedStoryPoint)) {
            return;
        }
        if (null == date) {
            // if a specific date is not given, then calculate from futureDays
            date = getCampaign().getLocalDate().plusDays(futureDays);
            ((CheckDateReachedStoryPoint) storyPoint).setDate(date);
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "storyPointId", storyPointId.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "date", date);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "futureDays", futureDays);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("storyPointId")) {
                    storyPointId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    date = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("futureDays")) {
                    futureDays = Integer.parseInt(wn2.getTextContent().trim());
                }

            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
