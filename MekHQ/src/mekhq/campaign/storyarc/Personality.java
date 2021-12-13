/*
 * Personality.java
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
package mekhq.campaign.storyarc;

import megamek.common.icons.Portrait;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * The personality class holds information about a personality that may interact with the players during the
 * story arc. This personality may be drawn from the campaign's own personnel, but does not necessarily need to
 * do so. So it could be an employer, a liaison, a rival, etc.
 *
 * The Personality class mainly contains a Portrait and a title that is used when displaying story related dialogs
 * associated with the Personality
 */
public class Personality implements MekHqXmlSerializable {

    //region Variable Declarations
    /** The UUID id of this personality */
    private UUID id;

    private Portrait portrait;

    private String title;

    /**
     * optionally a personality can be connected to a Person in the campaign. The personCampaignId identifies
     * this person
     */
    private UUID personCampaignId;

    protected static final String NL = System.lineSeparator();
    //endregion Variable Declarations

    //region Constructors
    public Personality() {

    }
    //endregion Constructors

    //region Getter/Setters
    public void setTitle(String t) { this.title = t; }

    public String getTitle() { return title; }

    public void setPortrait(Portrait p) { this.portrait = p; }

    public Portrait getPortrait() { return portrait; }

    public void setId(UUID id) { this.id = id; }

    protected UUID getId() { return id; }

    public Image getImage() {
        return portrait.getBaseImage();
    }
    //endregion Getter/Setters

    public void updatePersonalityFromCampaign(Campaign c) {
        if(null == personCampaignId) {
            return;
        }
        Person p = c.getPerson(personCampaignId);
        if(null == p) {
            return;
        }
        setPortrait(p.getPortrait());
        setTitle(p.getFullTitle());
    }

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        String level = MekHqXmlUtil.indentStr(indent),
                level1 = MekHqXmlUtil.indentStr(indent + 1);

        StringBuilder builder = new StringBuilder(256);
        builder.append(level)
                .append("<personality id=\"")
                .append(id)
                .append("\">")
                .append(NL)
                .append(level1)
                .append("<title>")
                .append(title)
                .append("</title>")
                .append(NL);
        if(null != personCampaignId) {
            builder.append(level1)
                    .append("<personCampaignId>")
                    .append(personCampaignId)
                    .append("</personCampaignId>")
                    .append(NL);
        }

        pw1.print(builder.toString());
        portrait.writeToXML(pw1, indent + 1);
    }

    public static Personality generateInstanceFromXML(Node wn, Campaign c) {
        Personality retVal = new Personality();

        try {
            retVal.id = UUID.fromString(wn.getAttributes().getNamedItem("id").getTextContent().trim());

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    retVal.title = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase(Portrait.XML_TAG)) {
                    retVal.portrait = Portrait.parseFromXML(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("personCampaignId")) {
                    retVal.personCampaignId = UUID.fromString(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

    //endregion File I/O

}
