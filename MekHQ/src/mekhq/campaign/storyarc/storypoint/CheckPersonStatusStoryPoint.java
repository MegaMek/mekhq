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
 */
package mekhq.campaign.storyarc.storypoint;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.utilities.MHQXMLUtility;

/**
 * This StoryPoint checks the status of a Person in the campaign by their UUID
 * and returns the raw status enum value
 * (e.g. ACTIVE, KIA) as the result. Can be used, for example, to check whether
 * a person is active before assigning
 * them a dialog.
 */
public class CheckPersonStatusStoryPoint extends StoryPoint {
    private static final MMLogger logger = MMLogger.create(CheckPersonStatusStoryPoint.class);

    private UUID personId;

    public CheckPersonStatusStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    protected String getResult() {
        Person p = getCampaign().getPerson(personId);
        if (null == p) {
            return "UNKNOWN";
        } else {
            return p.getStatus().name();
        }
    }

    @Override
    public void start() {
        super.start();
        complete();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "personId", personId);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personId = UUID.fromString(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
