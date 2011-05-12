/*
 * AmmoBin.java
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
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Era;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.Utilities;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.EquipmentRepair;
import mekhq.campaign.work.EquipmentReplacement;
import mekhq.campaign.work.EquipmentSalvage;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AmmoBin extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	protected long munition;
	protected int shotsNeeded;
    
    public AmmoBin() {
    	this(false, 0, null, -1, 0);
    }
    
    public AmmoBin(boolean salvage, int tonnage, EquipmentType et, int equipNum, int shots) {
        super(salvage, tonnage, et, equipNum);
        this.shotsNeeded = shots;
        if(null != type && type instanceof AmmoType) {
        	this.munition = ((AmmoType)type).getMunitionType();
        }
        if(null != name) {
        	this.name += " Bin";
        }
    }

    public int getShotsNeeded() {
    	return shotsNeeded;
    }
    
    public void changeMunition(long m) {
    	this.munition = m;
    	for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(),(AmmoType)type)) {
    		if (atype.getMunitionType() == munition) {
    			type = atype;
    			break;
    		}
    	}
    	updateConditionFromEntity();
    }
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shotsNeeded>"
				+shotsNeeded
				+"</shotsNeeded>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("munition")) {
				munition = Long.parseLong(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("shotsNeeded")) {
				shotsNeeded = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {		
		return type.getAvailability(Era.convertEra(era));
	}

	@Override
	public int getTechRating() {
		return type.getTechRating();
	}

	@Override
	public void fix() {
		int shots = Math.min(getAmountAvailable(), shotsNeeded);
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.changeAmmoType((AmmoType)type);
				mounted.setShotsLeft(mounted.getShotsLeft() + shots);
			}
		}
		shotsNeeded -= shots;
	}
	
	public void unload() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			int shots = 0;
			if(null != mounted) {
				shots = mounted.getShotsLeft();
				mounted.setShotsLeft(0);
			}
			shotsNeeded = ((AmmoType)type).getShots();
			if(shots > 0) {
				//now lets cycle through and add ammo to ammo storage
				for(Part part : unit.campaign.getSpareParts()) {
					if(part instanceof AmmoStorage) {
						AmmoStorage a = (AmmoStorage)part;
						if(a.getType() == type) {
							a.addShots(shots);
							return;
						}
					}
				}
				//if we are still here then we did not find any armor, so lets create a new part and stick it in spares
				AmmoStorage newAmmo = new AmmoStorage(true,tonnage,type,-1,shots);
				unit.campaign.addPart(newAmmo);
			}	
		}
	}
	
	@Override
	public void remove(boolean salvage) {
		//cycle through spare parts and add to existing ammo if found
		if(salvage) {
			unload();
		}
		super.remove(salvage);
	}

	@Override
	public Part getMissingPart() {
		return new MissingAmmoBin(isSalvage(), getTonnage(), type, equipmentNum);
	}
	
	@Override
	public TargetRoll getAllMods() {
		if(getAmountAvailable() == 0) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, "No ammo of this type is available");
		}
		return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "ammo loading");
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(!mounted.isRepairable() || mounted.isDestroyed()) {
					remove(false);
					return;
				}
				if(type.equals(mounted.getType())) {
					shotsNeeded = ((AmmoType)type).getShots() - mounted.getShotsLeft();	
					time = 15;
				} else {
					//we have a change of munitions
					shotsNeeded = ((AmmoType)type).getShots();
					time = 30;
				}
			}
		}
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setHit(false);
		        mounted.setDestroyed(false);
		        mounted.setRepairable(true);
		        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
				mounted.setShotsLeft(((AmmoType)type).getShots() - shotsNeeded);
			}
		}
	}

	@Override
	public boolean needsFixing() {
		return shotsNeeded > 0;
	}
	
	public String getDesc() {
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>Reload " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		int amountAvailable = getAmountAvailable();
		if(!salvaging) {
			if(amountAvailable == 0) {
				toReturn += "No ammo available";
			} else {
				toReturn += "" + getTimeLeft() + " minutes" + scheduled;
			}
		}
		toReturn += "</font></html>";
		return toReturn;
	}
	
    @Override
    public String getDetails() {
    	return ((AmmoType)type).getDesc() + ", " + shotsNeeded + " shots needed";
    }
	
	@Override
    public String checkFixable() {
        return null;
    }
	
	public void reduceAmountAvailable(int amount) {
		if(null != unit) {
			AmmoStorage a = null;
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof AmmoStorage) {
					a = (AmmoStorage)part;
					if(a.getType() == type) {
						a.reduceShots(amount);
						break;
					}
				}
			}
			if(null != a && a.getShots() <= 0) {
				unit.campaign.removePart(a);
			}
		}
	}
	
	public int getAmountAvailable() {
		if(null != unit) {
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof AmmoStorage) {
					AmmoStorage a = (AmmoStorage)part;
					if(a.getType() == type) {
						return a.getShots();
					}
				}
			}
			return 0;
		}
		return 0;
	}
}
