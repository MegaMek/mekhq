/*
 * PersonStatusStoryPoint.java
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
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This StoryPoint is started and completed whenever the status of a Person with a certain UUID is in a certain state.
 * Most typically this would be used to end the game if the main character is killed.
 * <p>This class differs from {@link CheckPersonStatusStoryPoint CheckPersonStatusStoryPoint} in that it is activated by
 * the listener in StoryArc rather than being called explicitly in a chain of StoryPoints.</p>
 */
public class PersonStatusStoryPoint extends StoryPoint {

    /**
     * ID of the person being checked
     */
    private UUID personId;

    /**
     * A list of PersonnelStatus enums that will trigger the story point
     */
    private List<PersonnelStatus> statusConditions;

    public PersonStatusStoryPoint() {
        super();
        statusConditions = new ArrayList<>();
    }

    @Override
    public String getTitle() {
        return "Person status check";
    }

    public UUID getPersonId() {
        return personId;
    }

    public List<PersonnelStatus> getStatusConditions() {
        return statusConditions;
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
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "personId", personId);
        for (PersonnelStatus status : statusConditions) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "statusCondition", status.name());
        }
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
                } else if (wn2.getNodeName().equalsIgnoreCase("statusCondition")) {
                    PersonnelStatus status = PersonnelStatus.parseFromString(wn2.getTextContent().trim());
                    if(null != status) {
                        statusConditions.add(status);
                    }
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
