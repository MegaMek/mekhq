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

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingBattleArmorEquipmentPart extends MissingEquipmentPart {
    
    /**
     * 
     */
    private static final long serialVersionUID = -5675111120455420391L;
    
    private int trooper;
    
    public MissingBattleArmorEquipmentPart() {
        this(0, null, -1, -1, null, 0.0);
    }
    
    public MissingBattleArmorEquipmentPart(int tonnage, EquipmentType et, int equipNum, int trooper, Campaign c, double etonnage) {
        super(tonnage, et, equipNum, c, etonnage);
        this.trooper = trooper;
        this.time = 30;
        this.difficulty = -2;
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
    
    public int getBaMountLocation() {
    	if(null != unit) {
    		Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				return mounted.getBaMountLoc();
			}
    	}
    	return -1;
    }
    
    
    
    private boolean isModular() {
    	if(null == unit) {
    		return false;
    	}
    	for (Mounted m : unit.getEntity().getEquipment()){
    		if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_BA_MEA) &&
    				type instanceof MiscType && type.hasFlag(MiscType.F_BA_MANIPULATOR)
    				&& this.getBaMountLocation()== m.getBaMountLoc()){
    			return true;
    		}
    		//this is not quite right, they must be linked somehow
    		/*if (type instanceof InfantryWeapon &&
    				m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_AP_MOUNT)
    				&& this.getBaMountLocation()== m.getBaMountLoc()){
    			return true;
    		}*/
    	}
    	return false;
    }
    
    @Override
    public boolean needsFixing() {
        //can only be replaced the normal way if modular and suit exists
    	if(null != unit && unit.getEntity().getInternal(trooper)>=0 && isModular()) {
    		return true;
    	}
        return false;
    }

    public int getTrooper() {
        return trooper;
    }
    
    public void setTrooper(int t) {
        trooper = t;
    }
    
    @Override 
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            ((EquipmentPart)actualReplacement).setEquipmentNum(equipmentNum);
            ((BattleArmorEquipmentPart)actualReplacement).setTrooper(trooper);
            remove(false);
            //assign the replacement part to the unit           
            actualReplacement.updateConditionFromPart();
        }
    }
    
    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if(part instanceof BattleArmorEquipmentPart) {
            BattleArmorEquipmentPart eqpart = (BattleArmorEquipmentPart)part;
            EquipmentType et = eqpart.getType();
            return type.equals(et) && getTonnage() == part.getTonnage();
        }
        return false;
    }
    
    @Override
    public Part getNewPart() {
        BattleArmorEquipmentPart epart = new BattleArmorEquipmentPart(getUnitTonnage(), type, -1, -1, campaign);
        epart.setEquipTonnage(equipTonnage);
        return epart;
    }

    @Override
    public void updateConditionFromPart() {
        //you cant crit BA equipment, so do nothing
    }
    
    @Override
	public int getLocation() {
		return trooper;
	}
    
    @Override
    public String getDetails() {
    	if(null == unit) {      
    		return super.getDetails();

        }
    	String toReturn = unit.getEntity().getLocationName(trooper) + "<br>";
		return toReturn + super.getDetails();
    }
    
    
}