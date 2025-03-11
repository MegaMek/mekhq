/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
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
package mekhq.campaign.force;

import java.io.PrintWriter;
import java.util.UUID;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;

/**
 * this is a hierarchical object that represents forces from the TO&amp;E using
 * strings rather than unit objects. This makes it static and thus usable to
 * keep track of forces involved in completed scenarios
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ForceStub {
    private static final MMLogger logger = MMLogger.create(ForceStub.class);

    // region Variable Declarations
    private String name;
    private StandardForceIcon forceIcon;
    private Vector<ForceStub> subForces;
    private Vector<UnitStub> units;
    // endregion Variable Declarations

    // region Constructors
    public ForceStub() {
        this(null, null);
    }

    public ForceStub(final @Nullable Force force, final @Nullable Campaign campaign) {
        name = (force == null) ? "" : force.getFullName();
        setForceIcon((force == null) ? new LayeredForceIcon() : force.getForceIcon().clone());

        subForces = new Vector<>();
        if (force != null) {
            for (Force sub : force.getSubForces()) {
                ForceStub stub = new ForceStub(sub, campaign);
                subForces.add(stub);
            }
        }

        units = new Vector<>();
        if ((force != null) && (campaign != null)) {
            for (UUID uid : force.getUnits()) {
                Unit u = campaign.getUnit(uid);
                if (null != u) {
                    units.add(new UnitStub(u));
                }
            }
        }
    }
    // endregion Constructors

    // region Getters/Setters
    public StandardForceIcon getForceIcon() {
        return forceIcon;
    }

    public void setForceIcon(final StandardForceIcon forceIcon) {
        this.forceIcon = forceIcon;
    }
    // endregion Getters/Setters

    public Vector<Object> getAllChildren() {
        Vector<Object> children = new Vector<>();
        children.addAll(subForces);
        children.addAll(units);

        return children;
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "forceStub");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        getForceIcon().writeToXML(pw, indent);

        if (!units.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "units");
            for (UnitStub ustub : units) {
                ustub.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "units");
        }

        if (!subForces.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "subforces");
            for (ForceStub sub : subForces) {
                sub.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "subforces");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "forceStub");
    }

    public static ForceStub generateInstanceFromXML(final Node wn, final Version version) {
        final ForceStub retVal = new ForceStub();

        try {
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase(StandardForceIcon.XML_TAG)) {
                    retVal.setForceIcon(StandardForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(LayeredForceIcon.XML_TAG)) {
                    retVal.setForceIcon(LayeredForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (!wn3.getNodeName().equalsIgnoreCase("unitStub")) {
                            logger
                                    .error("Unknown node type not loaded in ForceStub nodes: " + wn3.getNodeName());
                            continue;
                        }

                        retVal.units.add(UnitStub.generateInstanceFromXML(wn3));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("subforces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (!wn3.getNodeName().equalsIgnoreCase("forceStub")) {
                            logger
                                    .error("Unknown node type not loaded in ForceStub nodes: " + wn3.getNodeName());
                            continue;
                        }

                        retVal.subForces.add(generateInstanceFromXML(wn3, version));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
