/*
 * UnitStub.java
 *
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.io.Serializable;

import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UnitStub implements Serializable {
    private static final long serialVersionUID = 1448449600864209589L;

    private String desc;
    private AbstractIcon portrait;

    public UnitStub() {
        portrait = new Portrait();
        desc = "";
    }

    public UnitStub(Unit u) {
        desc = getUnitDescription(u);
        Person commander = u.getCommander();
        portrait = (commander == null) ? new Portrait() : commander.getPortrait();
    }

    @Override
    public String toString() {
        return desc;
    }

    public AbstractIcon getPortrait() {
        return portrait;
    }

    private String getUnitDescription(Unit u) {
        String name = "<font color='red'>No Crew</font>";
        String uname;
        Person pp = u.getCommander();
        if (null != pp) {
            name = pp.getFullTitle();
            name += " (" + u.getEntity().getCrew().getGunnery() + "/" + u.getEntity().getCrew().getPiloting() + ")";
            if (pp.needsFixing()) {
                name = "<font color='red'>" + name + "</font>";
            }
        }
        uname = "<i>" + u.getName() + "</i>";
        if (u.isDamaged()) {
            uname = "<font color='red'>" + uname + "</font>";
        }
        return "<html>" + name + ", " + uname + "</html>";
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "unitStub");
        if (!StringUtil.isNullOrEmpty(desc)) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "desc", desc);
        }

        if (!getPortrait().hasDefaultCategory()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "portraitCategory", getPortrait().getCategory());
        }

        if (!getPortrait().hasDefaultFilename()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "portraitFileName", getPortrait().getFilename());
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "unitStub");
    }

    public static UnitStub generateInstanceFromXML(Node wn) {
        UnitStub retVal = null;

        try {
            retVal = new UnitStub();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.desc = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) {
                    retVal.getPortrait().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitFileName")) {
                    retVal.getPortrait().setFilename(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(ex);
        }
        return retVal;
    }
}
