/*
 * AdvanceTimeStoryTrigger.java
 *
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved
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
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * A StoryTrigger that adds a Unit to the Campaign.
 */
public class AddUnitStoryTrigger extends StoryTrigger {

    String entityName;

    @Override
    protected void execute() {
        MechSummary ms = MechSummaryCache.getInstance().getMech(entityName);
        if (ms == null) {
            LogManager.getLogger().error("Cannot find entry for " + entityName);
            return;
        }

        MechFileParser mechFileParser;
        try {
            mechFileParser = new MechFileParser(ms.getSourceFile(), ms.getEntryName());
        } catch (Exception ex) {
            LogManager.getLogger().error("Unable to load unit: " + ms.getEntryName(), ex);
            return;
        }
        Entity en = mechFileParser.getEntity();
        getCampaign().addNewUnit(en, false, 0);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "entityName", entityName);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("entityName")) {
                    entityName = wn2.getTextContent().trim();
                }

            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
