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

import java.io.PrintWriter;
import java.text.ParseException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;

/**
 * A StoryTrigger that adds a Unit to the Campaign.
 */
public class AddUnitStoryTrigger extends StoryTrigger {
    private static final MMLogger logger = MMLogger.create(AddUnitStoryTrigger.class);

    String entityName;

    @Override
    protected void execute() {
        MekSummary ms = MekSummaryCache.getInstance().getMek(entityName);
        if (ms == null) {
            logger.error("Cannot find entry for {}", entityName);
            return;
        }

        MekFileParser mekFileParser;
        try {
            mekFileParser = new MekFileParser(ms.getSourceFile(), ms.getEntryName());
        } catch (Exception ex) {
            logger.error("Unable to load unit: {}", ms.getEntryName(), ex);
            return;
        }

        Entity en = mekFileParser.getEntity();

        PartQuality quality = PartQuality.QUALITY_D;

        if (getCampaign().getCampaignOptions().isUseRandomUnitQualities()) {
            quality = Unit.getRandomUnitQuality(0);
        }

        getCampaign().addNewUnit(en, false, 0, quality);
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
                logger.error(e);
            }
        }
    }
}
