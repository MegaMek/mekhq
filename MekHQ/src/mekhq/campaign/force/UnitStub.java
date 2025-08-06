/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.codeUtilities.StringUtility;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UnitStub {
    private static final MMLogger logger = MMLogger.create(UnitStub.class);

    // region Variable Declarations
    private String desc;
    private AbstractIcon portrait;
    // endregion Variable Declarations

    // region Constructors
    public UnitStub() {
        portrait = new Portrait();
        desc = "";
    }

    public UnitStub(Unit u) {
        desc = getUnitDescription(u);
        Person commander = u.getCommander();
        portrait = (commander == null) ? new Portrait() : commander.getPortrait();
    }
    // endregion Constructors

    // region Getters/Setters
    public AbstractIcon getPortrait() {
        return portrait;
    }

    public void setPortrait(final AbstractIcon portrait) {
        this.portrait = portrait;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }
    // endregion Getters/Setters

    private String getUnitDescription(Unit u) {
        String name = "<font color='" + ReportingUtilities.getNegativeColor() + "'>No Crew</font>";
        Person pp = u.getCommander();
        if (null != pp) {
            name = pp.getFullTitle();
            name += " (" + u.getEntity().getCrew().getGunnery() + "/" + u.getEntity().getCrew().getPiloting() + ")";
            if (pp.needsFixing()) {
                name = "<font color='" + ReportingUtilities.getNegativeColor() + "'>" + name + "</font>";
            }
        }
        String uname = "<i>" + u.getName() + "</i>";
        if (u.isDamaged()) {
            uname = "<font color='" + ReportingUtilities.getNegativeColor() + "'>" + uname + "</font>";
        }
        return "<html>" + name + ", " + uname + "</html>";
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "unitStub");
        if (!StringUtility.isNullOrBlank(getDesc())) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "desc", getDesc());
        }
        getPortrait().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unitStub");
    }

    public static UnitStub generateInstanceFromXML(Node wn) {
        UnitStub retVal = null;

        try {
            retVal = new UnitStub();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDesc(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(Portrait.XML_TAG)) {
                    retVal.setPortrait(Portrait.parseFromXML(wn2));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
        return retVal;
    }

    @Override
    public String toString() {
        return desc;
    }
}
