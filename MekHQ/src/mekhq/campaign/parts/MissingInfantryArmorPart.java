/*
 * MissingInfantryMotiveType.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.io.PrintWriter;

import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingInfantryArmorPart extends MissingPart {
    private static final long serialVersionUID = 330450091994252073L;

    private double damageDivisor;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;

    public MissingInfantryArmorPart() {
        this(0, null, 1.0, false, false, false, false, false, false);
    }

    public MissingInfantryArmorPart(int tonnage, Campaign c, double divisor, boolean enc, boolean dest, boolean camo, boolean ir, boolean ecm, boolean space) {
        super(tonnage, c);
        this.damageDivisor = divisor;
        this.encumbering = enc;
        this.dest = dest;
        this.sneak_camo = camo;
        this.sneak_ecm = ecm;
        this.sneak_ir = ir;
        this.spaceSuit = space;
        assignName();
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    private void assignName() {
        String heavyString = "";
        if (damageDivisor > 1) {
            heavyString = "Heavy ";
        }
        String baseName = "Armor Kit";
        if (isDest()) {
            baseName = "DEST Infiltration Suit";
        } else if (isSneakCamo() || isSneakECM() || isSneakIR()) {
            baseName = "Sneak Suit";
        } else if (isSpaceSuit()) {
            baseName = "Space Suit";
        }

        this.name = heavyString + baseName;
    }

    @Override
    public void updateConditionFromPart() {

    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new InfantryArmorPart(getUnitTonnage(), campaign, damageDivisor, encumbering, dest, sneak_camo, sneak_ecm, sneak_ir, spaceSuit);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof InfantryArmorPart
                && damageDivisor == ((InfantryArmorPart)part).getDamageDivisor()
                && dest == ((InfantryArmorPart)part).isDest()
                && encumbering == ((InfantryArmorPart)part).isEncumbering()
                && sneak_camo == ((InfantryArmorPart)part).isSneakCamo()
                && sneak_ecm == ((InfantryArmorPart)part).isSneakECM()
                && sneak_ir == ((InfantryArmorPart)part).isSneakIR()
                && spaceSuit == ((InfantryArmorPart)part).isSpaceSuit();
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<damageDivisor>"
                +damageDivisor
                +"</damageDivisor>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<dest>"
                +dest
                +"</dest>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<encumbering>"
                +encumbering
                +"</encumbering>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<sneak_camo>"
                +sneak_camo
                +"</sneak_camo>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<sneak_ecm>"
                +sneak_ecm
                +"</sneak_ecm>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<sneak_ir>"
                +sneak_ir
                +"</sneak_ir>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<spaceSuit>"
                +spaceSuit
                +"</spaceSuit>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("damageDivisor")) {
                damageDivisor = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("dest")) {
                dest = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("encumbering")) {
                encumbering = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("sneak_camo")) {
                sneak_camo = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("sneak_ecm")) {
                sneak_ecm = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("sneak_ir")) {
                sneak_ir = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("spaceSuit")) {
                spaceSuit = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }
    }

    public double getDamageDivisor() {
        return damageDivisor;
    }

    public boolean isDest() {
        return dest;
    }

    public boolean isEncumbering() {
        return encumbering;
    }

    public boolean isSneakCamo() {
        return sneak_camo;
    }

    public boolean isSneakECM() {
        return sneak_ecm;
    }

    public boolean isSneakIR() {
        return sneak_ir;
    }

    public boolean isSpaceSuit() {
        return spaceSuit;
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ARMOR;
    }
}
