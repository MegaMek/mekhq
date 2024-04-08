/*
 * AddPersonStoryTrigger.java
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

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryTrigger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * A StoryTrigger that adds a Person to the Campaign.
 */
public class AddPersonStoryTrigger extends StoryTrigger {

    private Person person;


    @Override
    protected void execute() {
        if (null != person) {
            getCampaign().recruitPerson(person, true);
        }

    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        if (null != person) {
            person.writeToXML(pw1, indent, getCampaign());
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("person")) {
                    person = Person.generateInstanceFromXML(wn2, c, v);
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
