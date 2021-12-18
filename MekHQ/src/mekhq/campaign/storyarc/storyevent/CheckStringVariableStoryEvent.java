/*
 * CheckStringVariableStoryEvent.java
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
package mekhq.campaign.storyarc.storyevent;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryEvent;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;

public class CheckStringVariableStoryEvent extends StoryEvent implements Serializable, MekHqXmlSerializable {

    String key;

    public CheckStringVariableStoryEvent() {
        super();
        key = "";
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    protected String getResult() {
        return getStoryArc().getCustomStringVariable(key);
    }

    @Override
    public void startEvent() {
        super.startEvent();
        completeEvent();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<key>"
                +key
                +"</key>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("key")) {
                    key =wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
