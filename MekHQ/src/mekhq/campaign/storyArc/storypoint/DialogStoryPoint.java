/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.storyArc.storypoint;

import java.awt.Image;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.UUID;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyArc.Personality;
import mekhq.campaign.storyArc.StoryPoint;
import mekhq.campaign.storyArc.StorySplash;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class DialogStoryPoint extends StoryPoint {
    private static final MMLogger LOGGER = MMLogger.create(DialogStoryPoint.class);

    /** A StorySplash image to display in a dialog. It can return a null image */
    private StorySplash storySplash;

    /**
     * The id of a personality who is associated with this StoryPoint. May be null.
     */
    private UUID personalityId;

    public DialogStoryPoint() {
        super();
        storySplash = new StorySplash();
    }

    public Image getImage() {
        if (storySplash.isDefault()) {
            return null;
        }
        return storySplash.getImage();
    }

    /**
     * Get the {@link Personality Personality} associated with this StoryPoint.
     *
     * @return A {@link Personality Personality} or null if no Personality is associated with the StoryPoint.
     */
    public Personality getPersonality() {
        if (null == personalityId) {
            return null;
        }
        return getStoryArc().getPersonality(personalityId);
    }

    @Override
    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        super.writeToXmlBegin(pw1, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw1, ++indent, "personalityId", personalityId);
        storySplash.writeToXML(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("personalityId")) {
                    personalityId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(StorySplash.XML_TAG)) {
                    storySplash = StorySplash.parseFromXML(wn2);
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }

}
