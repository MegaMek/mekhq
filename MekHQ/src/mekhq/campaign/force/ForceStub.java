/*
 * ForceStub.java
 *
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
import mekhq.io.migration.ForceIconMigrator;
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
                } else if (wn2.getNodeName().equalsIgnoreCase("iconCategory")) { // Legacy - 0.49.6 removal
                    retVal.getForceIcon().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("iconHashMap")) { // Legacy - 0.49.6 removal
                    final LayeredForceIcon layeredForceIcon = new LayeredForceIcon();
                    ForceIconMigrator.migrateLegacyIconMapNodes(layeredForceIcon, wn2);
                    retVal.setForceIcon(layeredForceIcon);
                } else if (wn2.getNodeName().equalsIgnoreCase("iconFileName")) { // Legacy - 0.49.6 removal
                    retVal.getForceIcon().setFilename(wn2.getTextContent().trim());
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

        if (version.isLowerThan("0.49.6")) {
            retVal.setForceIcon(ForceIconMigrator.migrateForceIconToKailans(retVal.getForceIcon()));
        } else if (version.isLowerThan("0.49.7")) {
            retVal.setForceIcon(ForceIconMigrator.migrateForceIcon0496To0497(retVal.getForceIcon()));
        }

        return retVal;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
