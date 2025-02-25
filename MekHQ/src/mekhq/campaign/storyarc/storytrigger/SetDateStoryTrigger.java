/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved
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
import mekhq.gui.StoryPointHyperLinkListener;
import mekhq.gui.panels.storytriggerpanels.FakeStoryTriggerPanel;
import mekhq.gui.panels.storytriggerpanels.SetDateStoryTriggerPanel;
import mekhq.gui.panels.storytriggerpanels.StoryTriggerPanel;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storypoint.CheckDateReachedStoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
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


    public UUID getStoryPointId() {
        return storyPointId;
    }

    public void setStoryPointId(UUID storyPointId) {
        this.storyPointId = storyPointId;
    }

    public int getFutureDays() {
        return futureDays;
    }

    public void setFutureDays(int futureDays) {
        this.futureDays = futureDays;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

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
    public String getDescription() {
        StoryPoint storyPoint = getStoryArc().getStoryPoint(storyPointId);
        StringBuilder sb = new StringBuilder();
        sb.append("Set date");
        if(storyPoint != null) {
            sb.append(" in ");
            sb.append(storyPoint.getHyperlinkedName());
            if(date == null) {
                sb.append(" ahead by ");
                sb.append(futureDays);
                sb.append(" days");
            } else {
                sb.append(" to ");
                sb.append(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
            }
        }
        return sb.toString();
    }

    @Override
    public StoryTriggerPanel getPanel(JFrame frame) {
        return new SetDateStoryTriggerPanel(frame, "StoryTriggerPanel", this);
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
