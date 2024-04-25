/*
 * RemoveUnitStoryTrigger.java
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
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * A StoryTrigger to remove a set of Units from the Campaign.
 */
public class RemoveUnitStoryTrigger extends StoryTrigger {

    /** ArrayList of UUID unit ids to remove **/
    ArrayList<UUID> unitIds = new ArrayList<UUID>();

    /** boolean to remove all units **/
    boolean removeAll = false;

    @Override
    protected void execute() {
        if (removeAll) {
            unitIds = new ArrayList<UUID>();
            for(Unit u : getCampaign().getUnits()) {
                unitIds.add(u.getId());
            }
        }

        for(UUID unitId : unitIds) {
            getCampaign().removeUnit(unitId);
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        if(!unitIds.isEmpty()) {
            for (UUID unitId : unitIds) {
                MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "unitId", unitId);
            }
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "removeAll", removeAll);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
                    UUID id = UUID.fromString(wn2.getTextContent().trim());
                    if (null != id) {
                        unitIds.add(id);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("removeAll")) {
                    removeAll = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
