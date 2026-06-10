/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.location;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Personnel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents a specific academy campus as an {@link ILocation}.
 *
 * <p>Each {@code AcademyCampusLocation} is a lightweight node in the location tree,
 * serving as a child of a {@link mekhq.campaign.FixedLocation} at the same planetary system.
 * Personnel enrolled at the campus can be attached as children of this node.</p>
 */
public class AcademyCampusLocation implements IPlace {

    private static final MMLogger LOGGER = MMLogger.create(AcademyCampusLocation.class);

    private final LocationNode locationNode;
    private final Personnel personnel = new Personnel();
    private final String academySet;
    private final String academyName;

    public AcademyCampusLocation(String academySet, String academyName) {
        this.academySet = academySet;
        this.academyName = academyName;
        this.locationNode = new LocationNode(this);
        LocationNode.LocationManager.setLocation(personnel, this);
    }

    public String getAcademySet() {
        return academySet;
    }

    public String getAcademyName() {
        return academyName;
    }

    public @Nullable Academy getAcademy() {
        return AcademyFactory.getInstance().getAcademy(academySet, academyName);
    }

    @Override
    public LocationNode getLocationNode() {
        return locationNode;
    }

    @Override
    public Personnel getPersonnel() {
        return personnel;
    }

    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "academyCampus");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "academySet", academySet);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "academyName", academyName);
        for (LocationNode child : locationNode.getChildren()) {
            if (child.getLocatable() instanceof CurrentLocation currentLoc) {
                currentLoc.writeToXML(pw, indent);
            }
        }
        for (LocationNode child : personnel.getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof Person person) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", person.getId().toString());
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "academyCampus");
    }

    // Populated during XML load; drained by CampaignXmlParser to reconnect persons after load.
    private transient List<UUID> pendingPersonIds = new ArrayList<>();

    /** Returns and clears the person UUIDs read from XML, for use during post-load reconnection. */
    public List<UUID> drainPendingPersonIds() {
        List<UUID> ids = new ArrayList<>(pendingPersonIds);
        pendingPersonIds.clear();
        return ids;
    }

    public static @Nullable AcademyCampusLocation generateInstanceFromXML(Node wn) {
        AcademyCampusLocation campus = null;
        try {
            String academySet = null;
            String academyName = null;
            List<UUID> personIds = new ArrayList<>();
            NodeList nodeList = wn.getChildNodes();
            for (int x = 0; x < nodeList.getLength(); x++) {
                Node wn2 = nodeList.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn2.getNodeName().equalsIgnoreCase("academySet")) {
                    academySet = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("academyName")) {
                    academyName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    personIds.add(UUID.fromString(wn2.getTextContent().trim()));
                }
            }
            if (academySet == null || academyName == null) {
                return null;
            }
            campus = new AcademyCampusLocation(academySet, academyName);
            campus.pendingPersonIds.addAll(personIds);
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
        return campus;
    }
}
