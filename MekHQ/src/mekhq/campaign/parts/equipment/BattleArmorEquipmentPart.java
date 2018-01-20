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
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * BA equipment is never critted so we are going to disable salvaging as well. It would
 * be nice at some point to allow for this but we would need some way in MM of tracking
 * how many actual weapons on the squad are operational (nWeapon?)
 * When an individual suit is removed we also remove all the equipment and keep it with
 * the suit. See BattleArmorSuit for details.
 *
 * Taharqa: as of 8/7/2015, I am working on making a change to this to allow for salvaging out parts
 * that are modularly mounted. The way I am planning on handling this is to set up a check in Unit
 * for whether a BattleSuit is operable or not and if not then soldiers would not be allowed to mount
 * it. It will be defined as inoperable if it is missing modular equipment. I will also likely have
 * to make changes to the BattleArmorSuit object to accomodate this as well.
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
    	if(null != unit) {
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
            //need to record this as missing for trooper on entity
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && isModular()) {
				mounted.setMissingForTrooper(trooper, true);
			}
            //sorry dude, but you can't pilot a messed up BA suit
            if(unit.getEntity().getInternal(trooper) > 0) {
                unit.getEntity().setInternal(0, trooper);
                if(unit.getCrew().size() > 0) {
                    Person trooperToRemove = unit.getCrew().get(unit.getCrew().size()-1);
                    if(null != trooperToRemove) {
                    	unit.remove(trooperToRemove, true);
                    }
                }
            }
        }
    	if(!salvage) {
    		campaign.removePart(this);
    	}
        setUnit(null);
        equipmentNum = -1;
        trooper = -1;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
    	if(null != unit && isModular()) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(mounted.isMissingForTrooper(trooper)) {
					remove(false);
					return;
				}
			}
		}
    }

    @Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return 30;
		}
		return super.getBaseTime();
	}

	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return -2;
		}
		return super.getBaseTime();
	}

    @Override
    public boolean needsFixing() {
        //cant be critted so shouldnt need to be fixed
        return false;
    }

    @Override
    public boolean isSalvaging() {
    	if(isModular()) {
    		return super.isSalvaging();
    	}
        //guess what - you cant salvage this
        return false;
    }

    @Override
    public void updateConditionFromPart() {
        if(isModular()) {
        	Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setMissingForTrooper(trooper, false);
			}
        }
    }

    @Override
    public String getDetails() {
        if(null != unit) {
            return unit.getEntity().getLocationName(trooper);
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
	public int getLocation() {
		return trooper;
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
    	for (Mounted m : unit.getEntity().getMisc()){
    		if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_BA_MEA) &&
    				type instanceof MiscType && type.hasFlag(MiscType.F_BA_MANIPULATOR)
    				&& this.getBaMountLocation()== m.getBaMountLoc()){
    			return true;
    		}
    		/*if (type instanceof InfantryWeapon &&
    				m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_AP_MOUNT)
    				&& this.getBaMountLocation()== m.getBaMountLoc()){
    			return true;
    		}*/
    	}
    	return false;
    }

    public boolean needsMaintenance() {
        return false;
    }

    public boolean canNeverScrap() {
    	return isModular();
	}
}
