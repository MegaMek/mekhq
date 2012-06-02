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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AmmoBin extends EquipmentPart implements IAcquisitionWork {
	private static final long serialVersionUID = 2892728320891712304L;

	protected long munition;
	protected int shotsNeeded;
	protected boolean checkedToday;
	protected boolean oneShot;
	
    public AmmoBin() {
    	this(0, null, -1, 0, false, null);
    }
    
    public AmmoBin(int tonnage, EquipmentType et, int equipNum, int shots, boolean singleShot, Campaign c) {
        super(tonnage, et, equipNum, c);
        this.shotsNeeded = shots;
        this.oneShot = singleShot;
        this.checkedToday = false;
        if(null != type && type instanceof AmmoType) {
        	this.munition = ((AmmoType)type).getMunitionType();
        }
        if(null != name) {
        	this.name += " Bin";
        }
    }
    
    public AmmoBin clone() {
    	AmmoBin clone = new AmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), shotsNeeded, oneShot, campaign);
        clone.copyBaseData(this);
        clone.shotsNeeded = this.shotsNeeded;
        clone.munition = this.munition;
        return clone;
    }
    
    @Override
    public double getTonnage() {
    	return 1.0;
    }
    
    public int getFullShots() {
    	int fullShots = ((AmmoType)type).getShots();
		if(oneShot) {
			fullShots = 1;
		}
		return fullShots;
    }
    
    @Override
    public long getCurrentValue() {
    	//multiply full value of ammo ton by the percent of shots remaining  	
    	return (long)(getStickerPrice() * (1.0 - (double)shotsNeeded / getFullShots()));
    }
    
    public long getValueNeeded() {
    	return adjustCostsForCampaignOptions((long)(getStickerPrice() * ((double)shotsNeeded / getFullShots())));
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
        	itemCost = (int) type.getCost(en, isArmored);
        	if (itemCost == EquipmentType.COST_VARIABLE) {
        		itemCost = type.resolveVariableCost(en, isArmored);
        	}
        } catch(NullPointerException ex) {
        	System.out.println("Found a null entity while calculating cost for " + name);
        }
        return itemCost;
    }

    public int getShotsNeeded() {
    	return shotsNeeded;
    }
    
    public void changeMunition(long m) {
    	this.munition = m;
    	for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(),(AmmoType)type, CampaignOptions.TECH_EXPERIMENTAL)) {
    		if (atype.getMunitionType() == munition) {
    			type = atype;
    			break;
    		}
    	}
    	updateConditionFromEntity();
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
				+type.getInternalName()
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shotsNeeded>"
				+shotsNeeded
				+"</shotsNeeded>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<checkedToday>"
				+checkedToday
				+"</checkedToday>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<oneShot>"
				+oneShot
				+"</oneShot>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		super.loadFieldsFromXmlNode(wn);
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("munition")) {
				munition = Long.parseLong(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("shotsNeeded")) {
				shotsNeeded = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("checkedToday")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					checkedToday = true;
				} else {
					checkedToday = false;
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("oneShot")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					oneShot = true;
				} else {
					oneShot = false;
				}
			} 
		}
		restore();
	}

	public void restoreMunitionType() {
		for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(),(AmmoType)type, CampaignOptions.TECH_EXPERIMENTAL)) {
    		if (atype.getMunitionType() == munition && atype.getInternalName().equals(type.getInternalName())) {
    			type = atype;
    			break;
    		}
    	}
	}
	
	public long getMunitionType() {
		return munition;
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
	public int getTechBase() {
		return T_BOTH;
	}

	@Override
	public String getStatus() {
		String toReturn = "Fully Loaded";
		if(shotsNeeded >= getFullShots()) {
			toReturn = "Empty";
		} else if (shotsNeeded > 0) {
			toReturn = "Partially Loaded";
		}
		if(isReservedForRefit()) {
			toReturn += " (Reserved for Refit)";
		}
		return toReturn;
	}
	
	@Override
	public void fix() {
		loadBin(true);
	}
	
	public void loadBin(boolean changeEntity) {
		int shots = Math.min(getAmountAvailable(), shotsNeeded);
		if(null != unit && changeEntity) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(mounted.getType().equals(type)) {
					//just a simple reload
					mounted.setShotsLeft(mounted.getShotsLeft() + shots);
				} else {
					//loading a new type of ammo				
					unload(true);
					mounted.changeAmmoType((AmmoType)type);
					mounted.setShotsLeft(shots);
				}
			}
		}
		changeAmountAvailable(-1 * shots, (AmmoType)type);
		shotsNeeded -= shots;
	}
	
	public void setShotsNeeded(int shots) {
		this.shotsNeeded = shots;
	}
	
	@Override
	public String find() {
		changeAmountAvailable(getFullShots(), (AmmoType)type);
		setCheckedToday(true);
		if(campaign.getCampaignOptions().payForParts()) {
			campaign.getFinances().debit(adjustCostsForCampaignOptions(getStickerPrice()), Transaction.C_EQUIP, "Purchase of " + getAcquisitionName(), campaign.calendar.getTime());
		}
		return "<font color='green'> part found.</font>";
	}
	
	@Override
	public String failToFind() {
		setCheckedToday(false);
		return "<font color='red'> part not found.</font>";
	}
	
	public void unload(boolean changeEntity) {
		int shots = getFullShots() - shotsNeeded;
		AmmoType curType = (AmmoType)type;
		if(null != unit && changeEntity) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);		
			if(null != mounted) {
				shots = mounted.getShotsLeft();
				mounted.setShotsLeft(0);
				curType = (AmmoType)mounted.getType();
			}		
		}
		shotsNeeded = getFullShots();
		if(shots > 0) {
			changeAmountAvailable(shots, curType);
		}	
	}
	
	@Override
	public void remove(boolean salvage) {
		if(salvage) {
			unload(true);
		}
		super.remove(salvage);
	}

	@Override
	public Part getMissingPart() {
		return new MissingAmmoBin(getUnitTonnage(), type, equipmentNum, oneShot, campaign);
	}
	
	public boolean isOneShot() {
		return oneShot;
	}
	
	@Override
	public TargetRoll getAllMods() {
		if(isSalvaging()) {
			return super.getAllMods();
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
					shotsNeeded = getFullShots() - mounted.getShotsLeft();	
					time = 15;
					difficulty = 0;
				} else {
					//we have a change of munitions
					shotsNeeded = getFullShots();
					time = 30;
					difficulty = 0;
				}
				if(isSalvaging()) {
					time = 120;
					difficulty = -2;
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
				mounted.setShotsLeft(getFullShots() - shotsNeeded);
			}
		}
	}
	
	@Override
    public boolean isSamePartTypeAndStatus (Part part) {
		if(isReservedForRefit()) {
    		return false;
    	}
    	return  part instanceof AmmoBin
                        && getType().equals( ((AmmoBin)part).getType() )
                        && ((AmmoBin)part).isOneShot() == oneShot;
    }

	@Override
	public boolean needsFixing() {
		return shotsNeeded > 0 && null != unit;
	}
	
	public String getDesc() {
		if(isSalvaging()) {
			return super.getDesc();
		}
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
    	if(isSalvaging()) {
    		return super.getDetails();
    	}
    	if(null != unit) {
    		String availability = "";
    		int shotsAvailable = getAmountAvailable();		
    		if(shotsAvailable == 0) {
    			availability = ", <font color='red'>No ammo available</font>";
    		} else if(shotsAvailable < shotsNeeded) {
    			availability = ", <font color='red'>Only " + shotsAvailable + " shots available</font>";
    		}
			return ((AmmoType)type).getDesc() + ", " + shotsNeeded + " shots needed" + availability;
    	} else {
    		return "";
    	}
    }
	
	@Override
    public String checkFixable() {
		if(!isSalvaging() && getAmountAvailable() == 0) {
			return "No ammo of this type is available";
		}
        return null;
    }
	
	public void changeAmountAvailable(int amount, AmmoType curType) {
		AmmoStorage a = null;
		for(Part part : campaign.getSpareParts()) {
			if(part instanceof AmmoStorage && ((AmmoStorage)part).getType().equals(curType)) {
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
	
	public int getAmountAvailable() {
		for(Part part : campaign.getSpareParts()) {
			if(part instanceof AmmoStorage) {
				AmmoStorage a = (AmmoStorage)part;
				if(a.getType() == type) {
					return a.getShots();
				}
			}
		}
		return 0;
	}
	
	public boolean isEnoughSpareAmmoAvailable() {
		return getAmountAvailable() >= shotsNeeded;
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
	public boolean hasCheckedToday() {
		return checkedToday;
	}

	@Override
	public void setCheckedToday(boolean b) {
		this.checkedToday = b;
	}
}
