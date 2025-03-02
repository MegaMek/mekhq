/*
 * Copyright (C) 2020-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.storyarc.storypoint;

import java.io.PrintWriter;
import java.text.ParseException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.dialog.StoryNarrativeDialog;
import mekhq.utilities.MHQXMLUtility;

/**
 * This story point creates a {@link StoryNarrativeDialog StoryNarrativeDialog}
 * with a simple narrative description.
 */
public class NarrativeStoryPoint extends DialogStoryPoint {
    private static final MMLogger logger = MMLogger.create(NarrativeStoryPoint.class);

    String title;
    String narrative;

    public NarrativeStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getNarrative() {
        return narrative;
    }

    @Override
    public void start() {
        super.start();
        final StoryNarrativeDialog narrativeDialog = new StoryNarrativeDialog(null, this);
        narrativeDialog.setVisible(true);
        complete();
    }

    @Override
    public String getResult() {
        // this one has no variation
        return "";
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "title", title);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "narrative", narrative);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        super.loadFieldsFromXmlNode(wn, c, version);
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    title = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("narrative")) {
                    narrative = wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
