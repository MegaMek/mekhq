/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import java.io.PrintWriter;
import java.util.UUID;
import java.util.Vector;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this is a hierarchical object that represents formations from the TO&amp;E using strings rather than unit objects. This
 * makes it static and thus usable to keep track of formations involved in completed scenarios
 *
 * <p>Known as {@code FormationStub} prior to 0.50.12</p>
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since 0.50.12
 */
public class FormationStub {
    private static final MMLogger LOGGER = MMLogger.create(FormationStub.class);

    // region Variable Declarations
    private String name;
    private StandardFormationIcon formationIcon;
    private final Vector<FormationStub> subFormations;
    private final Vector<UnitStub> units;
    // endregion Variable Declarations

    // region Constructors
    public FormationStub() {
        this(null, null);
    }

    public FormationStub(final @Nullable Formation formation, final @Nullable Campaign campaign) {
        name = (formation == null) ? "" : formation.getFullName();
        setFormationIcon((formation == null) ? new LayeredFormationIcon() : formation.getFormationIcon().clone());

        subFormations = new Vector<>();
        if (formation != null) {
            for (Formation sub : formation.getSubFormations()) {
                FormationStub stub = new FormationStub(sub, campaign);
                subFormations.add(stub);
            }
        }

        units = new Vector<>();
        if ((formation != null) && (campaign != null)) {
            for (UUID uid : formation.getUnits()) {
                Unit u = campaign.getUnit(uid);
                if (null != u) {
                    units.add(new UnitStub(u));
                }
            }
        }
    }
    // endregion Constructors

    // region Getters/Setters
    public StandardFormationIcon getFormationIcon() {
        return formationIcon;
    }

    public void setFormationIcon(final StandardFormationIcon formationIcon) {
        this.formationIcon = formationIcon;
    }
    // endregion Getters/Setters

    public Vector<Object> getAllChildren() {
        Vector<Object> children = new Vector<>();
        children.addAll(subFormations);
        children.addAll(units);

        return children;
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "formationStub");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        getFormationIcon().writeToXML(pw, indent);

        if (!units.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "units");
            for (UnitStub unitStub : units) {
                unitStub.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "units");
        }

        if (!subFormations.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "subFormations");
            for (FormationStub sub : subFormations) {
                sub.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "subFormations");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "formationStub");
    }

    public static FormationStub generateInstanceFromXML(final Node wn, final Version version) {
        final FormationStub retVal = new FormationStub();

        try {
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase(StandardFormationIcon.XML_TAG)) {
                    retVal.setFormationIcon(StandardFormationIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(LayeredFormationIcon.XML_TAG)) {
                    retVal.setFormationIcon(LayeredFormationIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (!wn3.getNodeName().equalsIgnoreCase("unitStub")) {
                            LOGGER
                                  .error("Unknown node type not loaded in FormationStub nodes: {}", wn3.getNodeName());
                            continue;
                        }

                        retVal.units.add(UnitStub.generateInstanceFromXML(wn3));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("subFormations") || wn2.getNodeName().equalsIgnoreCase(
                      "subForces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (!wn3.getNodeName().equalsIgnoreCase("formationStub") &&
                                         !wn3.getNodeName().equalsIgnoreCase("forceStub")) {
                            LOGGER
                                  .error("Unknown node type not loaded in FormationStub nodes: {}", wn3.getNodeName());
                            continue;
                        }

                        retVal.subFormations.add(generateInstanceFromXML(wn3, version));
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return retVal;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
