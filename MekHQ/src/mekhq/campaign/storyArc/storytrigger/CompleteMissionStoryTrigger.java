/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.storyArc.storytrigger;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyArc.StoryPoint;
import mekhq.campaign.storyArc.StoryTrigger;
import mekhq.campaign.storyArc.storypoint.MissionStoryPoint;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A trigger that completes a mission. It can optionally include information on the final victory status of the
 * mission.
 */
public class CompleteMissionStoryTrigger extends StoryTrigger {
    private static final MMLogger LOGGER = MMLogger.create(CompleteMissionStoryTrigger.class);

    UUID missionStoryPointId;
    MissionStatus missionStatus;

    @Override
    protected void execute() {
        StoryPoint storyPoint = getStoryArc().getStoryPoint(missionStoryPointId);
        if (storyPoint instanceof MissionStoryPoint) {
            if (null != missionStatus) {
                ((MissionStoryPoint) storyPoint).getMission().setStatus(missionStatus);
            }
            storyPoint.complete();
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionStoryPointId", missionStoryPointId);
        if (null != missionStatus) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionStatus", missionStatus.name());
        }
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("missionStoryPointId")) {
                    missionStoryPointId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionStatus")) {
                    missionStatus = MissionStatus.parseFromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }

}
