/*
 * CheckPersonActiveStoryPoint.java
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
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * This StoryPoint checks the status of a Person in the campaign by their UUID
 * and returns the raw status enum value
 * (e.g. ACTIVE, KIA) as the result. Can be used, for example, to check whether
 * a person is active before assigning
 * them a dialog.
 */
public class CheckPersonStatusStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(CheckPersonStatusStoryPoint.class);

    private UUID personId;

    public CheckPersonStatusStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    protected String getResult() {
        Person p = getCampaign().getPerson(personId);
        if (null == p) {
            return "UNKNOWN";
        } else {
            return p.getStatus().name();
        }
    }

    @Override
    public List<String> getAllPossibleResults() {
        ArrayList<String> results = new ArrayList<>();
        for(PersonnelStatus status : PersonnelStatus.getImplementedStatuses()) {
            results.add(status.name());
        }
        results.add(DEFAULT_OUTCOME);
        return results;
    }

    @Override
    public void start() {
        super.start();
        complete();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "personId", personId);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
