/*
 * CheckStringVariableStoryPoint.java
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
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.ChangeStringVariableStoryTrigger;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * This StoryPoint checks the value of a stored string variable from the
 * {@link mekhq.campaign.storyarc.StoryArc StoryArc}. It returns the result of
 * that value.
 */
public class CheckStringVariableStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(CheckStringVariableStoryPoint.class);

    /**
     * the key of the desired variable
     */
    private String key;

    public CheckStringVariableStoryPoint() {
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
    public List<String> getAllPossibleResults() {
        // this one is complicated, but we can find all the triggers where this has been changed and collect
        // all possibilities
        List<String> results = new ArrayList<>();
        for(StoryPoint sp : getStoryArc().getStoryPoints()) {
            for(StoryTrigger trigger : sp.getStoryTriggers()) {
                if(trigger instanceof ChangeStringVariableStoryTrigger) {
                    if(!((ChangeStringVariableStoryTrigger)trigger).getKey().equals(key)) {
                        continue;
                    }
                    String value = ((ChangeStringVariableStoryTrigger)trigger).getValue();
                    if(!results.contains(value)) {
                        results.add(value);
                    }
                }
            }
        }
        // finally, check the current value of this variable and add it if not already there
        String value = getStoryArc().getCustomStringVariable(key);
        if(!results.contains(value)) {
            results.add(value);
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
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "key", key);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("key")) {
                    key = wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
