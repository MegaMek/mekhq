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
    //region Variable Declarations
    private static final long serialVersionUID = 1448449600864209589L;

    private String desc;
    private AbstractIcon portrait;
    //endregion Variable Declarations

    //region Constructors
    public UnitStub() {
        portrait = new Portrait();
        desc = "";
    }

    public UnitStub(Unit u) {
        desc = getUnitDescription(u);
        Person commander = u.getCommander();
        portrait = (commander == null) ? new Portrait() : commander.getPortrait();
    }
    //endregion Constructors

    //region Getters/Setters
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
    //endregion Getters/Setters

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
        if (!StringUtil.isNullOrEmpty(getDesc())) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "desc", getDesc());
        }
        getPortrait().writeToXML(pw1, indent);
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
                    retVal.setDesc(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(Portrait.XML_TAG)) {
                    retVal.setPortrait(Portrait.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitFileName")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setFilename(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }
        return retVal;
    }

    @Override
    public String toString() {
        return desc;
    }
}
