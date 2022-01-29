/*
 * ChangePersonStoryTrigger.java
 *
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

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.storyarc.StoryTrigger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.UUID;

/**
 * A StoryTrigger that can change various characteristics of a Person
 */
public class ChangePersonStoryTrigger extends StoryTrigger implements Serializable, MekHqXmlSerializable {

    /**
     * The id of the Person in the campaign
     */
    private UUID personId;

    /**
     * A PersonnelStatus to change the person to
     */
    private PersonnelStatus status;

    /**
     * The number of hits the person should have. Will only change if higher than current hits
     */
    private int hits;

    @Override
    protected void execute() {
        Person p = getCampaign().getPerson(personId);
        if(null != p) {
            if(null != status) {
                p.changeStatus(getCampaign(), getCampaign().getLocalDate(), status);
            }
            // only change hits if it is higher than current hits
            if(hits > 0 & hits > p.getHits()) {
                p.setHits(hits);
            }
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "personId", personId);
        if(null != status) {
            MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "status", status.name());
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    status = PersonnelStatus.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    hits = Integer.parseInt(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
