/*
 * MissingMekCockpit.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.parts;

import megamek.common.CriticalSlot;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekCockpit extends MissingPart {
    private int type;
    protected boolean isClan;

    public MissingMekCockpit() {
        this(0, Mek.COCKPIT_STANDARD, false, null);
    }

    public MissingMekCockpit(int tonnage, int t, boolean isClan,Campaign c) {
        super(tonnage, c);
        this.type = t;
        this.isClan = isClan;
        this.name = Mek.getCockpitDisplayString(type);
    }

    @Override
    public int getBaseTime() {
        return 300;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        switch (type) {
            case Mek.COCKPIT_SMALL:
                return 2.0;
            case Mek.COCKPIT_TORSO_MOUNTED:
            case Mek.COCKPIT_DUAL:
            case Mek.COCKPIT_SUPERHEAVY:
            case Mek.COCKPIT_SUPERHEAVY_INDUSTRIAL:
            case Mek.COCKPIT_TRIPOD:
            case Mek.COCKPIT_TRIPOD_INDUSTRIAL:
            case Mek.COCKPIT_INTERFACE:
            case Mek.COCKPIT_QUADVEE:
                return 4.0;
            case Mek.COCKPIT_PRIMITIVE:
            case Mek.COCKPIT_PRIMITIVE_INDUSTRIAL:
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD:
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL:
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
                return 5.0;
            case Mek.COCKPIT_COMMAND_CONSOLE:
                return 6.0;
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                return 7.0;
            case Mek.COCKPIT_STANDARD:
            case Mek.COCKPIT_INDUSTRIAL:
            case Mek.COCKPIT_VRRP:
            default:
                return 3.0;
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("small")) {
                    type = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof MekCockpit && ((MekCockpit) part).getType() == type;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean isClan() {
        return isClan;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i) > 0) {
                if (unit.isLocationBreached(i)) {
                    return unit.getEntity().getLocationName(i) + " is breached.";
                } else if (unit.isLocationDestroyed(i)) {
                    return unit.getEntity().getLocationName(i) + " is destroyed.";
                }
            }
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new MekCockpit(getUnitTonnage(), type, isClan, campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT);
        }
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        if (type == Mek.COCKPIT_TORSO_MOUNTED) {
            return Mek.LOC_CT;
        } else {
            return Mek.LOC_HEAD;
        }
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return Mek.getCockpitTechAdvancement(type);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
