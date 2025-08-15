/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.storyarc;

import java.awt.Image;
import java.io.PrintWriter;
import java.util.UUID;

import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The personality class holds information about a personality that may interact with the players during the story arc.
 * This personality may be drawn from the campaign's own personnel, but does not necessarily need to do so. So it could
 * be an employer, a liaison, a rival, etc.
 * <p>
 * The Personality class mainly contains a Portrait and a title that is used when displaying story related dialogs
 * associated with the Personality
 * </p>
 */
public class Personality {
    private static final MMLogger logger = MMLogger.create(Personality.class);

    // region Variable Declarations
    /** A name for this personality **/
    private String name;

    /** The UUID id of this personality */
    private UUID id;

    private StoryPortrait portrait;

    private String title;

    /**
     * optionally a personality can be connected to a Person in the campaign. The personCampaignId identifies this
     * person
     */
    private UUID personCampaignId;
    // endregion Variable Declarations

    // region Constructors
    public Personality() {
        portrait = new StoryPortrait();
    }
    // endregion Constructors

    // region Getter/Setters
    public void setTitle(String t) {
        this.title = t;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public void setPortrait(StoryPortrait p) {
        this.portrait = p;
    }

    public Portrait getPortrait() {
        return portrait;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    protected UUID getId() {
        return id;
    }

    public Image getImage() {
        return portrait.getBaseImage();
    }
    // endregion Getter/Setters

    public void updatePersonalityFromCampaign(Campaign c) {
        if (null == personCampaignId) {
            return;
        }
        Person p = c.getPerson(personCampaignId);
        if (null == p) {
            return;
        }
        portrait.setCategory(p.getPortrait().getCategory());
        portrait.setFilename(p.getPortrait().getFilename());
        setTitle(p.getFullTitle());
    }

    // region File I/O
    public void writeToXml(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "personality", "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "title", title);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "personCampaignId", personCampaignId);
        portrait.writeToXML(pw1, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "personality");
    }

    public static Personality generateInstanceFromXML(Node wn, Campaign c) {
        Personality retVal = new Personality();

        try {
            retVal.name = wn.getAttributes().getNamedItem("name").getTextContent().trim();

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    retVal.title = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase(StoryPortrait.XML_TAG)) {
                    retVal.portrait = StoryPortrait.parseFromXML(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("personCampaignId")) {
                    retVal.personCampaignId = UUID.fromString(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }

        return retVal;
    }

    // endregion File I/O

}
