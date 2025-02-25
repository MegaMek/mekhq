/*
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
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
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
import mekhq.campaign.mission.Mission;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * A story point that checks whether a given mission has more active scenarios.
 * This will typically be used
 * in cases where players are given all scenarios at once and GM does not know
 * which they will complete last.
 */
public class CheckMoreScenariosStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(CheckMoreScenariosStoryPoint.class);

    /** id of the mission to check **/
    private UUID missionStoryPointId;

    public CheckMoreScenariosStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return "Checking more scenarios";
    }

    @Override
    protected String getResult() {
        StoryPoint missionStoryPoint = getStoryArc().getStoryPoint(missionStoryPointId);
        if (missionStoryPoint instanceof MissionStoryPoint) {
            Mission m = ((MissionStoryPoint) missionStoryPoint).getMission();
            if ((null != m) && (m.getCurrentScenarios().isEmpty())) {
                return "false";
            }
        }
        return "true";
    }

    @Override
    public List<String> getAllPossibleResults() {
        ArrayList<String> results = new ArrayList<>();
        results.add("true");
        results.add("false");
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
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "missionStoryPointId", missionStoryPointId);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("missionStoryPointId")) {
                    missionStoryPointId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
