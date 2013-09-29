/*
 * BattleArmorEquipmentPart.java
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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;

/**
 * We need a special sub-class for BA equipment because there
 * should actually be multiple parts referencing the same equipment number in
 * the MegaMek entity. So we disable that equipment when any of them report being 
 * removed, but we dont destroy parts based on the condition of the entity. Since
 * equipment on BAs is never critted in combat, we should never have to update the condition
 * from the entity. Also, we need to change this to a regular equipment part when it is 
 * removed and replace it with a regular missingequipmentpart
 * 
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class BattleArmorEquipmentPart extends EquipmentPart {
    
    /**
     * 
     */
    private static final long serialVersionUID = -5675111120455420391L;
    
    private int trooper;
    
    public BattleArmorEquipmentPart() {
        this(0, null, -1, -1, null);
    }
    
    public BattleArmorEquipmentPart(int tonnage, EquipmentType et, int equipNum, int trooper, Campaign c) {
        super(tonnage, et, equipNum, c);
        this.trooper = trooper;
    }
    
    public EquipmentPart clone() {
        BattleArmorEquipmentPart clone = new BattleArmorEquipmentPart(getUnitTonnage(), type, equipmentNum, trooper, campaign);
        clone.copyBaseData(this);
        if(hasVariableTonnage(type)) {
            clone.setEquipTonnage(equipTonnage);
        }
        return clone;
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);       
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<equipmentNum>"
                +equipmentNum
                +"</equipmentNum>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<typeName>"
                +MekHqXmlUtil.escape(type.getInternalName())
                +"</typeName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<equipTonnage>"
                +equipTonnage
                +"</equipTonnage>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<trooper>"
                +trooper
                +"</trooper>");
        writeToXmlEnd(pw1, indent);
    }
    
    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
                equipmentNum = Integer.parseInt(wn2.getTextContent());
            }
            else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                typeName = wn2.getTextContent();
            }
            else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
                equipTonnage = Double.parseDouble(wn2.getTextContent());
            }
            else if (wn2.getNodeName().equalsIgnoreCase("trooper")) {
                trooper = Integer.parseInt(wn2.getTextContent());
            }
        }
        restore();
    }

    @Override
    public void remove(boolean salvage) {
        //BA equipment can only go with the suit or nowhere so dont allow separate salvage
        if(null != unit) {
            campaign.removePart(this);
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
        }
        setSalvaging(false);
        setUnit(null);
        equipmentNum = -1;
    }

    @Override
    public void updateConditionFromEntity() {
        //because BA equipment cannot be critted
    }
    
    @Override
    public boolean needsFixing() {
        //cant be critted so shouldnt need to be fixed
        return false;
    }
    
    @Override
    public boolean isSalvaging() {
        //guess what - you cant salvage this
        return false;
    }
    
    @Override
    public void updateConditionFromPart() {
        //BA equipment can never get critted
    }
    
    @Override
    public String getDetails() {
        if(null != unit) {           
            return unit.getEntity().getLocationName(trooper) + ", " + hits + " hit(s)";
        }
        return super.getDetails();
    }
    
    public int getTrooper() {
        return trooper;
    }
    
    public void setTrooper(int t) {
        trooper = t;
    }
    
    @Override
    public MissingPart getMissingPart() {
        return new MissingBattleArmorEquipmentPart(getUnitTonnage(), type, equipmentNum, trooper, campaign, equipTonnage);
    }    
    
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof BattleArmorEquipmentPart
                && getType().equals(((BattleArmorEquipmentPart)part).getType())
                && getTonnage() == part.getTonnage();
    }
}