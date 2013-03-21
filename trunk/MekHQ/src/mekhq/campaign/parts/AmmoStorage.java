/*
 * AmmoStorage.java
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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.work.IAcquisitionWork;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This will be a special type of part that will only exist as spares
 * It will determine the amount of ammo of a particular type that 
 * is available
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AmmoStorage extends EquipmentPart implements IAcquisitionWork {
	private static final long serialVersionUID = 2892728320891712304L;

	protected long munition;
	protected int shots;
	
    public AmmoStorage() {
    	this(0, null, 0, null);
    }
    
    public AmmoStorage(int tonnage, EquipmentType et, int shots, Campaign c) {
        super(tonnage, et, -1, c);
        this.shots = shots;
        if(null != type && type instanceof AmmoType) {
        	this.munition = ((AmmoType)type).getMunitionType();
        }

    }
    
    public AmmoStorage clone() {
    	AmmoStorage storage = new AmmoStorage(0, getType(), shots, campaign);
        storage.copyBaseData(this);
    	storage.munition = this.munition;
    	return storage;
    }
    
    @Override
    public double getTonnage() {
    	return ((double)shots / ((AmmoType)type).getShots());
    }
    
    @Override
    public long getStickerPrice() {
    	//costs are a total nightmare
        //some costs depend on entity, but we can't do it that way
        //because spare parts don't have entities. If parts start on an entity
        //thats fine, but this will become problematic when we set up a parts
        //store. For now I am just going to pass in a null entity and attempt
    	//to catch any resulting NPEs
    	Entity en = null;
    	boolean isArmored = false;
    	if (unit != null) {
            en = unit.getEntity();
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
            	isArmored = mounted.isArmored();
            }
    	}

        int itemCost = 0;      
        try {
        	itemCost = (int) type.getCost(en, isArmored, -1);
        } catch(NullPointerException ex) {
        	System.out.println("Found a null entity while calculating cost for " + name);
        }
    	return itemCost;
    }
    
    @Override
    public long getCurrentValue() {
    	return (long)(getStickerPrice() * ((double)shots / ((AmmoType)type).getShots()));
    }

    public int getShots() {
    	return shots;
    }
    
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AmmoStorage
                && ((AmmoType)getType()).getMunitionType() == ((AmmoType)((AmmoStorage)part).getType()).getMunitionType()
                && ((AmmoType)getType()).equals( (Object)((EquipmentPart)part).getType());
    }
    
    public void changeShots(int s) {
    	shots = Math.max(0, shots + s);
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
                +MekHqXmlUtil.escape(typeName)
                +"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shots>"
				+shots
				+"</shots>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("munition")) {
				munition = Long.parseLong(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("shots")) {
				shots = Integer.parseInt(wn2.getTextContent());
			}
		}
		restore();
	}

	@Override
	public int getAvailability(int era) {		
		return type.getAvailability(Era.convertEra(era));
	}

	@Override
	public int getTechRating() {
		return type.getTechRating();
	}
	
	//@Override
	//public int getTechBase() {
		//return T_BOTH;
	//}

	@Override
	public void fix() {
		//nothing to fix
		return;
	}

	@Override
	public Part getMissingPart() {
		//nothing to do here
		return null;
	}
	
	@Override
	public TargetRoll getAllMods() {
		//nothing to do here
		return null;
	}

	@Override
	public void updateConditionFromEntity() {
		//nothing to do here
		return;
	}
	
	@Override
	public void updateConditionFromPart() {
		//nothing to do here
		return;
	}

	@Override
	public boolean needsFixing() {
		return false;
	}
	
	public String getDesc() {
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getAssignedTeamId() != null) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>Reload " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += "</font></html>";
		return toReturn;
	}
	
    @Override
    public String getDetails() {
    	return shots + " shots";
    }
	
	@Override
    public String checkFixable() {
        return null;
    }
	
	@Override
    public String find(int transitDays) {
	    Part newPart = getNewPart();
        newPart.setDaysToArrival(transitDays);
        campaign.buyPart(newPart);
        return "<font color='green'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
    }
    
    @Override
    public String failToFind() {
        resetDaysToWait();
        return "<font color='red'><b> part not found</b>.</font>";
    }
    
    public void changeAmountAvailable(int amount, AmmoType curType) {
        AmmoStorage a = null;
        long curMunition = curType.getMunitionType();
        for(Part part : campaign.getSpareParts()) {
            if(!part.isPresent()) {
                continue;
            }
            if(part instanceof AmmoStorage 
                    && ((AmmoType)((AmmoStorage)part).getType()).equals((Object)curType)
                    && curMunition == ((AmmoType)((AmmoStorage)part).getType()).getMunitionType()) {
                a = (AmmoStorage)part;
                a.changeShots(amount);
                break;
            }
        }
        if(null != a && a.getShots() <= 0) {
            campaign.removePart(a);
        } else if(null == a && amount > 0) {
            campaign.addPart(new AmmoStorage(1,curType,amount,campaign));
        }
    }
    
    @Override
    public String getAcquisitionDesc() {
        String bonus = getAllAcquisitionMods().getValueAsString();
        if(getAllAcquisitionMods().getValue() > -1) {
            bonus = "+" + bonus;
        }
        bonus = "(" + bonus + ")";
        String toReturn = "<html><font size='2'";
        
        toReturn += ">";
        toReturn += "<b>" + type.getDesc() + "</b> " + bonus + "<br/>";
        toReturn += ((AmmoType)type).getShots() + " shots (1 ton)<br/>";
        String[] inventories = campaign.getPartInventory(getNewPart());
        toReturn += inventories[1] + " in transit, " + inventories[2] + " on order<br>"; 
        toReturn += Utilities.getCurrencyString(getStickerPrice()) + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }
    
    @Override
    public String getAcquisitionName() {
        return type.getDesc();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        int factionMod = 0;
        if (campaign.getCampaignOptions().useFactionModifiers()) {
            factionMod = campaign.getFaction().getTechMod(this, campaign);
        }   
        //availability mod
        int avail = getAvailability(campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
        if(factionMod != 0) {
           target.addModifier(factionMod, "faction");
        }
        return target;
    }

    @Override
    public Part getNewPart() {
        return new AmmoStorage(1,type,((AmmoType)type).getShots(),campaign);
    }
    
    @Override
    public String getQuantityName(int quan) {
        int totalShots = quan * getShots();
        String report = "" + totalShots + " shots of " + getName();
        if(totalShots == 1) {
            report = "" + totalShots + " shot of " + getName();
        }
        return report;
    }
    
    @Override
    public String getArrivalReport() {
        double totalShots = quantity * getShots();
        String report = getQuantityName(quantity);
        if(totalShots == 1) {
            report += " has arrived";
        } else {
            report += " have arrived";
        }
        return report;
    }
}

