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
package mekhq.campaign;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.UUID;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Roster of {@link Person}s, keyed by UUID.
 *
 * <p>Extends {@link LinkedHashMap} so all standard map operations work on existing call sites.
 * Adds {@link #writeToXML} and {@link #loadFromXML} to own the canonical save/load loop for
 * the {@code <personnel>} XML block.</p>
 */
public class Personnel extends LinkedHashMap<UUID, Person> {
    private static final MMLogger logger = MMLogger.create(Personnel.class);

    public void writeToXML(PrintWriter writer, int indent, Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "personnel");
        for (Person person : values()) {
            person.writeToXML(writer, indent, campaign);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "personnel");
    }

    /**
     * Parses a {@code <personnel>} XML node and imports each {@code <person>} child into {@code campaign}
     * via {@link Campaign#importPerson(Person)}.
     *
     * <p>Compatibility post-processing (edge conversion, academy validation) is left to the caller.</p>
     */
    public static void loadFromXML(Node wn, Campaign campaign, Version version) {
        NodeList wList = wn.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                logger.error("Unknown node type not loaded in Personnel nodes: {}", wn2.getNodeName());
                continue;
            }
            Person p = Person.generateInstanceFromXML(wn2, campaign, version);
            if (p != null) {
                campaign.importPerson(p);
            }
        }
    }
}
