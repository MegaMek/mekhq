/*
 * InfantryMotiveType.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.io.PrintWriter;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * This part represents custom armor kit settings rather than one of the formal armor kits
 * from TacOps.
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */

public class InfantryArmorPart extends Part {

    /**
     *
     */
    private static final long serialVersionUID = 8298691936947743373L;

    private double damageDivisor;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;

    public InfantryArmorPart() {
        this(0, null, 1.0, false, false, false, false, false, false);
    }

    public InfantryArmorPart(int tonnage, Campaign c, double divisor, boolean enc, boolean dest, boolean camo, boolean ir, boolean ecm, boolean space) {
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

    private void assignName() {
        String heavyString = "";
        if(damageDivisor > 1) {
            heavyString = "Heavy ";
        }
        String baseName = "Armor Kit";
        if(isDest()) {
            baseName = "DEST Infiltration Suit";
        } else if(isSneakCamo() || isSneakECM() || isSneakIR()) {
            baseName = "Sneak Suit";
        } else if(isSpaceSuit()) {
            baseName = "Space Suit";
        }

        this.name = heavyString + baseName;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        String details = "";
        if(isEncumbering()) {
            details += "encumbering";
        }
        if(isSneakCamo()) {
            if(!details.equals("")) {
                details += ", ";
            }
            details += "camo";
        }
        if(isSneakECM()) {
            if(!details.equals("")) {
                details += ", ";
            }
            details += "ECM";
        }
        if(isSneakIR()) {
            if(!details.equals("")) {
                details += ", ";
            }
            details += "IR";
        }
        return details;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        //do nothing
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        //do nothing
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if(!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if(null != spare) {
                int number = quantity;
                while(number > 0) {
                    spare.incrementQuantity();
                    number--;
                }
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
        }
        setUnit(null);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingInfantryArmorPart(getUnitTonnage(), campaign, damageDivisor, encumbering, dest, sneak_camo, sneak_ecm, sneak_ir, spaceSuit);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public Money getStickerPrice() {
        double price = 0;
        if(damageDivisor > 1) {
            if(isEncumbering()) {
                price += 1600;
            } else {
                price += 4300;
            }
        }
        int nSneak = 0;
        if(isSneakCamo()) {
            nSneak++;
        }
        if(isSneakECM()) {
            nSneak++;
        }
        if(isSneakIR()) {
            nSneak++;
        }

        if(isDest()) {
            price += 50000;
        }
        else if(nSneak == 1) {
            price += 7000;
        }
        else if(nSneak == 2) {
            price += 21000;
        }
        else if(nSneak == 3) {
            price += 28000;
        }

        if(isSpaceSuit()) {
            price += 5000;
        }

        return Money.of(price);
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof InfantryArmorPart
                && damageDivisor == ((InfantryArmorPart)part).getDamageDivisor()
                && dest == ((InfantryArmorPart)part).isDest()
                && encumbering == ((InfantryArmorPart)part).isEncumbering()
                && sneak_camo == ((InfantryArmorPart)part).isSneakCamo()
                && sneak_ecm == ((InfantryArmorPart)part).isSneakECM()
                && sneak_ir == ((InfantryArmorPart)part).isSneakIR()
                && spaceSuit == ((InfantryArmorPart)part).isSpaceSuit();
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

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("damageDivisor")) {
                damageDivisor =Double.parseDouble(wn2.getTextContent());
            }
            else if (wn2.getNodeName().equalsIgnoreCase("dest")) {
                dest = wn2.getTextContent().equalsIgnoreCase("true");
            }
            else if (wn2.getNodeName().equalsIgnoreCase("encumbering")) {
                encumbering = wn2.getTextContent().equalsIgnoreCase("true");
            }
            else if (wn2.getNodeName().equalsIgnoreCase("sneak_camo")) {
                sneak_camo = wn2.getTextContent().equalsIgnoreCase("true");
            }
            else if (wn2.getNodeName().equalsIgnoreCase("sneak_ecm")) {
                sneak_ecm = wn2.getTextContent().equalsIgnoreCase("true");
            }
            else if (wn2.getNodeName().equalsIgnoreCase("sneak_ir")) {
                sneak_ir = wn2.getTextContent().equalsIgnoreCase("true");
            }
            else if (wn2.getNodeName().equalsIgnoreCase("spaceSuit")) {
                spaceSuit = wn2.getTextContent().equalsIgnoreCase("true");
            }
        }
    }

    @Override
    public Part clone() {
        return new InfantryArmorPart(getUnitTonnage(), campaign, damageDivisor, encumbering, dest, sneak_camo, sneak_ecm, sneak_ir, spaceSuit);
    }

    @Override
    public boolean needsMaintenance() {
        return false;
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
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ARMOR;
    }
}

