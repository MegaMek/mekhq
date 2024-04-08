/*
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
import megamek.common.Compute;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * This StoryPoint rolls some dice and returns the result. It can be used when some randomization is required.
 */
public class RollDiceStoryPoint extends StoryPoint {

    private int ndice;
    private int sides;
    private int value;

    public RollDiceStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return "roll dice";
    }

    @Override
    public void start() {
        super.start();
        value = Compute.individualDice(ndice, sides).get(0);
        complete();
    }

    @Override
    protected String getResult() {
        return Integer.toString(value);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "ndice", ndice);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "sides", sides);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("ndice")) {
                    ndice = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("sides")) {
                    sides = Integer.parseInt(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
