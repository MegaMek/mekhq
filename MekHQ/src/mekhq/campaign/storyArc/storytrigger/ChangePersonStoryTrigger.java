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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.storyArc.StoryTrigger;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A StoryTrigger that can change various characteristics of a Person
 */
public class ChangePersonStoryTrigger extends StoryTrigger {
    private static final MMLogger LOGGER = MMLogger.create(ChangePersonStoryTrigger.class);

    /**
     * The id of the Person in the campaign
     */
    private UUID personId;

    /**
     * A PersonnelStatus to change the person to
     */
    private PersonnelStatus status;

    /**
     * Boolean for whether Persons switched to inactive status also take any unit they are assigned to with them
     */
    boolean takeUnit = false;

    /**
     * Add the number of hits to the current person, which could kill them
     */
    private int addHits;

    /**
     * The reason for death if adding hits kills a person
     */
    private PersonnelStatus deathStatusHits;

    /**
     * heal the number of hits to the current person
     */
    private int healHits;

    /**
     * The rank the person should have
     */
    private int rank;
    /**
     * A bloodname to assign
     */
    private String bloodname;

    /**
     * A boolean indicator for whether the bloodname variable is a key to variable in the Story Arc
     */
    private boolean assignKeyBloodname = false;

    @Override
    protected void execute() {
        Person p = getCampaign().getPerson(personId);
        if (null != p) {
            Unit u = p.getUnit();
            if (null != status) {
                p.changeStatus(getCampaign(), getCampaign().getLocalDate(), status);
                if (takeUnit && !status.isActive() && (null != u)) {
                    getCampaign().removeUnit(u.getId());
                }
            }

            if (addHits > 0) {
                p.setHits(p.getHits() + addHits);
                if (p.getHits() >= 6) {
                    if (null == deathStatusHits) {
                        deathStatusHits = PersonnelStatus.KIA;
                    }
                    p.changeStatus(getCampaign(), getCampaign().getLocalDate(), deathStatusHits);
                    p.setHits(6);
                }
            }

            if (healHits > 0) {
                p.setHits(Math.max(0, p.getHits() - healHits));
            }

            if (rank > 0) {
                p.setRank(rank);
            }

            if (null != bloodname && !bloodname.isEmpty()) {
                if (assignKeyBloodname) {
                    String name = getStoryArc().getCustomStringVariable(bloodname);
                    if (null != name && !name.isEmpty()) {
                        p.setBloodname(name);
                    }
                } else {
                    p.setBloodname(bloodname);
                }
            }
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "personId", personId);
        if (null != status) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "status", status.name());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "takeUnit", takeUnit);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "addHits", addHits);
        if (null != deathStatusHits) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "deathStatusHits", deathStatusHits.name());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "healHits", healHits);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "rank", rank);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "bloodname", bloodname);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "assignKeyBloodname", assignKeyBloodname);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    status = PersonnelStatus.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("takeUnit")) {
                    takeUnit = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("addHits")) {
                    addHits = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("deathStatusHits")) {
                    deathStatusHits = PersonnelStatus.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("healHits")) {
                    healHits = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    rank = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("bloodname")) {
                    bloodname = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("assignKeyBloodname")) {
                    assignKeyBloodname = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }
}
