/*
 * ChangeStringVariableStoryTrigger.java
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
import mekhq.gui.panels.storytriggerpanels.ChangeStringVariableStoryTriggerPanel;
import mekhq.gui.panels.storytriggerpanels.FakeStoryTriggerPanel;
import mekhq.gui.panels.storytriggerpanels.StoryTriggerPanel;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryTrigger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.io.PrintWriter;
import java.text.ParseException;
import java.io.PrintWriter;
import java.text.ParseException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.utilities.MHQXMLUtility;

/**
 * A StoryTrigger to change the value of a stored string variable in StoryArc.
 */
public class ChangeStringVariableStoryTrigger extends StoryTrigger {
    private static final MMLogger logger = MMLogger.create(ChangeStringVariableStoryTrigger.class);

    String key;
    String value;

    @Override
    protected void execute() {
        getStoryArc().addCustomStringVariable(key, value);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getDescription() {
        return "Change value of " + key + " to " + value;
    }

    @Override
    public StoryTriggerPanel getPanel(JFrame frame) {
        return new ChangeStringVariableStoryTriggerPanel(frame, "StoryTriggerPanel", this);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "key", key);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "value", value);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("key")) {
                    key = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("value")) {
                    value = wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
