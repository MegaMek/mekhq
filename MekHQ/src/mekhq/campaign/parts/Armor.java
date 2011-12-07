/*
 * Armor.java
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

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.Utilities;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Armor extends Part implements IAcquisitionWork {
	private static final long serialVersionUID = 5275226057484468868L;
	protected int type;
    protected int amount;
    protected int amountNeeded;
    private int location;
    private boolean rear;
    private boolean checkedToday;
    private boolean clan;
    
    public Armor() {
    	this(0, 0, 0, -1, false, false);
    }
    
    public Armor(int tonnage, int t, int points, int loc, boolean r, boolean clan) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage);
        this.type = t;
        this.amount = points;
        this.location = loc;
        this.rear = r;
        this.clan = clan;
        this.name = "Armor";
        if(type > -1) {
        	this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
    }
    
    public Armor clone() {
    	return new Armor(0, type, amount, -1, false, clan);
    }
    
    @Override
    public double getTonnage() {
    	double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type, isClanTechBase());
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        return amount / armorPerTon;
    }
    
    @Override
    public long getCurrentValue() {
    	return (long)(getTonnage() * EquipmentType.getArmorCost(type));
    }
    
    @Override
    public long getStickerPrice() {
    	//always in 5-ton increments
    	return (long)(5 * EquipmentType.getArmorCost(type));
    }
    
    public String getDesc() {
    	if(isSalvaging()) {
    		return super.getDesc();
    	}
		String bonus = getAllMods().getValueAsString();
		if (getAllMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
	
		String scheduled = "";
		if (getAssignedTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>Replace " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		if(getAmountAvailable() > 0) {
			toReturn += "" + getTimeLeft() + " minutes" + scheduled;
			toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
			toReturn += " " + bonus;
		}
		if (getMode() != Modes.MODE_NORMAL) {
			toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
		}
		toReturn += "</font></html>";
		return toReturn;
	}
    
    @Override
	public String getDetails() {
		if(null != unit) {
			String rearMount = "";
			if(rear) {
				rearMount = " (R)";
			}
			String availability = "";
			int amountAvailable = getAmountAvailable();
			if(!isSalvaging()) {
				if(amountAvailable == 0) {
					availability = "<br><font color='red'>No spare armor available</font>";
				} else if(amountAvailable < amountNeeded) {
					availability = "<br><font color='red'>Only " + amountAvailable + " points of armor available</font>";
				}
			}
			return unit.getEntity().getLocationName(location) + rearMount + ", " + amountNeeded + " points" + availability;
		}
		return amount + " points";
	}
    
    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
    
    public int getAmountNeeded() {
    	return amountNeeded;
    }
    
    public int getTotalAmount() {
    	return amount + amountNeeded;
    }
    
    public int getLocation() {
    	return location;
    }
    
    public boolean isRearMounted() {
    	return rear;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public void setAmountNeeded(int needed) {
    	this.amountNeeded = needed;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof Armor
                && getType() == ((Armor)part).getType() 
                && isClanTechBase() == ((Armor)part).isClanTechBase()
                && getRefitId() == part.getRefitId();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_ARMOR;
    }

    @Override
    public boolean isClanTechBase() {
    	return clan;
    }
    
    @Override
    public int getTech () {
        // Armor tech base is not used (Clan/IS can use each other's armor for now)
        // TODO Set Tech base correctly for armor
        // Clan FF and IS FF do not have the same armor points per ton
        return TechConstants.T_INTRO_BOXSET;
    }

    public double getArmorWeight(int points) {
        // from megamek.common.Entity.getArmorWeight()
        
        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.
        double armorPointMultiplier = EquipmentType.getArmorPointMultiplier(getType(), isClanTechBase());
        double armorPerTon = 16.0 * armorPointMultiplier;
        if (getType() == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        
        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<amount>"
				+amount
				+"</amount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<location>"
				+location
				+"</location>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<rear>"
				+rear
				+"</rear>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<amountNeeded>"
				+amountNeeded
				+"</amountNeeded>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<checkedToday>"
				+checkedToday
				+"</checkedToday>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<clan>"
				+clan
				+"</clan>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("amount")) {
				amount = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("location")) {
				location = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("amountNeeded")) {
				amountNeeded = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("rear")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					rear = true;
				} else {
					rear = false;
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("checkedToday")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					checkedToday = true;
				} else {
					checkedToday = false;
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					clan = true;
				} else {
					clan = false;
				}
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		switch(type) {
		case EquipmentType.T_ARMOR_FERRO_FIBROUS:
		case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_D;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_D;
			}
		case EquipmentType.T_ARMOR_LIGHT_FERRO:
		case EquipmentType.T_ARMOR_HEAVY_FERRO:
		case EquipmentType.T_ARMOR_STEALTH:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
		case EquipmentType.T_ARMOR_INDUSTRIAL:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_C;
			} else {
				return EquipmentType.RATING_B;
			}
		case EquipmentType.T_ARMOR_COMMERCIAL:	
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_B;
			} else {
				return EquipmentType.RATING_A;
			}
		case EquipmentType.T_ARMOR_REACTIVE:
		case EquipmentType.T_ARMOR_REFLECTIVE:
		case EquipmentType.T_ARMOR_HARDENED:
		case EquipmentType.T_ARMOR_PATCHWORK:
		case EquipmentType.T_ARMOR_FERRO_IMP:
		case EquipmentType.T_ARMOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
		default:
			return EquipmentType.RATING_C;	
		}
	}

	@Override
	public int getTechRating() {
		switch(type) {
		case EquipmentType.T_ARMOR_INDUSTRIAL:
			return EquipmentType.RATING_C;
		case EquipmentType.T_ARMOR_COMMERCIAL:	
			return EquipmentType.RATING_B;
		case EquipmentType.T_ARMOR_FERRO_FIBROUS:
		case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO:		
		case EquipmentType.T_ARMOR_LIGHT_FERRO:
		case EquipmentType.T_ARMOR_HEAVY_FERRO:
		case EquipmentType.T_ARMOR_STEALTH:
			return EquipmentType.RATING_E;	
		case EquipmentType.T_ARMOR_HARDENED:
			return EquipmentType.RATING_D;
		case EquipmentType.T_ARMOR_REACTIVE:
		case EquipmentType.T_ARMOR_REFLECTIVE:
			return EquipmentType.RATING_E;
		case EquipmentType.T_ARMOR_PATCHWORK:
		case EquipmentType.T_ARMOR_FERRO_IMP:
		case EquipmentType.T_ARMOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
			return EquipmentType.RATING_F;
		default:
			return EquipmentType.RATING_D;	
		}
	}

	@Override
	public void fix() {
		int amount = Math.min(getAmountAvailable(), amountNeeded);
		int curAmount = unit.getEntity().getArmor(location, rear);
		if(curAmount < 0) {
			curAmount = 0;
		}
		unit.getEntity().setArmor(amount + curAmount, location, rear);
		changeAmountAvailable(-1 * amount);
		updateConditionFromEntity();
	}
	
	@Override
	public String find() {
		changeAmountAvailable((int)Math.round(5 * getArmorPointsPerTon()));
		setCheckedToday(true);
		if(unit.campaign.getCampaignOptions().payForParts()) {
			unit.campaign.getFinances().debit(adjustCostsForCampaignOptions(getStickerPrice(), unit.campaign), Transaction.C_EQUIP, "Purchase of " + getName(), unit.campaign.calendar.getTime());
		}
		return "<font color='green'><b> part found.</b></font>";
	}
	
	@Override
	public String failToFind() {
		setCheckedToday(false);
		return "<font color='red'><b> part not found.</b></font>";
	}

	@Override
	public Part getMissingPart() {
		//no such thing
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
		if(salvage) {
			changeAmountAvailable(amountNeeded);
		}
		updateConditionFromEntity();
	}

	public int getBaseTimeFor(Entity entity) {
		if(entity instanceof Tank) {
			return 3;
		} 
		else if(entity instanceof Aero) {
			return 15;
		}
		return 5;
	}
	
	@Override
	public void updateConditionFromEntity() {
		if(isReservedForRefit()) {
			return;
		}
		int currentArmor = unit.getEntity().getArmor(location, rear);
		if(currentArmor < 0) {
			currentArmor = 0;
		}
		if(salvaging) {
			amountNeeded = currentArmor;
			amount = unit.getEntity().getOArmor(location, rear) - amountNeeded;
		} else {			
			amountNeeded = unit.getEntity().getOArmor(location, rear) - currentArmor;
			amount = currentArmor;
		}
		//time should be based on amount available if less than amount needed
		if(salvaging) {
			time = getBaseTimeFor(unit.getEntity()) * amountNeeded;
		} else {
			time = getBaseTimeFor(unit.getEntity()) * Math.min(amountNeeded, getAmountAvailable());
		}
		difficulty = -2;
	}
	
	@Override
	public boolean isSalvaging() {
		return salvaging && amountNeeded > 0;
	}
	
	@Override
	public String succeed() {
		boolean tmpSalvaging = salvaging;
		String toReturn = super.succeed();
		if(tmpSalvaging) {
			salvaging = true;
			updateConditionFromEntity();
		}
		return toReturn;
	}

	@Override
	public boolean needsFixing() {
		return amountNeeded > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			int armor = amount;
			if(salvaging) {
				armor = amountNeeded;
			}
			unit.getEntity().setArmor(armor, location, rear);
		}
	}
	
	@Override
	public String checkFixable() {
		if(isSalvaging()) {
			return null;
		}
		if(getAmountAvailable() == 0) {
			return "No spare armor available";
		}
		if (unit.isLocationDestroyed(location)) {
			return unit.getEntity().getLocationName(location) + " is destroyed.";
		}
		return null;
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
		toReturn += "<b>" + getName() + "</b> " + bonus + "<br/>";
		toReturn += Utilities.getCurrencyString(adjustCostsForCampaignOptions(getStickerPrice(), unit.campaign)) + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}
	
	@Override
	public String getAcquisitionName() {
		return getName();
	}
	
	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        int factionMod = 0;
        if (null != unit && unit.campaign.getCampaignOptions().useFactionModifiers()) {
        	factionMod = Availability.getFactionAndTechMod(this, unit.campaign);
        }   
        //availability mod
        int avail = getAvailability(unit.campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
        if(factionMod != 0) {
     	   target.addModifier(factionMod, "faction");
        }
        return target;
    }

	public double getArmorPointsPerTon() {
		if(null != unit) {
			// armor is checked for in 5-ton increments
			int armorType = unit.getEntity().getArmorType(location);
			double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(armorType, unit.getEntity().getTechLevel());
			if (armorType == EquipmentType.T_ARMOR_HARDENED) {
				armorPerTon = 8.0;
			}
			return armorPerTon;
		}
		return 0.0;
	}
	
	@Override 
	public Part getNewPart() {
		return null;
	}

	@Override
	public boolean hasCheckedToday() {
		return checkedToday;
	}

	@Override
	public void setCheckedToday(boolean b) {
		this.checkedToday = b;
	}
	
	public boolean isEnoughSpareArmorAvailable() {
		return getAmountAvailable() >= amountNeeded;
	}
	
	public int getAmountAvailable() {
		if(null != unit) {
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof Armor) {
					Armor a = (Armor)part;
					if(a.getType() == type && a.isClanTechBase() == clan && !a.isReservedForRefit()) {
						return a.getAmount();
					}
				}
			}
			return 0;
		}
		return 0;
	}
	
	public void changeAmountAvailable(int amount) {
		if(null != unit) {
			Armor a = null;
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof Armor && ((Armor)part).getType() == type 
						&& ((Armor)part).isClanTechBase() == clan 
						&& getRefitId() == part.getRefitId()) {
					a = (Armor)part;				
					a.setAmount(a.getAmount() + amount);
					break;
				}
			}
			if(null != a && a.getAmount() <= 0) {
				unit.campaign.removePart(a);
			} else if(null == a && amount > 0) {			
				unit.campaign.addPart(new Armor(getUnitTonnage(), type, amount, -1, false, isClanTechBase()));
			}
			unit.campaign.updateAllArmorForNewSpares();
		}
	}

	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		//if we are impossible to fix now, we should scrap this amount of armor
		//from spares and start over
		String scrap = "";
		if(skillMin > SkillType.EXP_ELITE) {
			scrap = " Armor supplies lost!";
			if(isSalvaging()) {
				remove(false);
			} else {
				skillMin = SkillType.EXP_GREEN;
				changeAmountAvailable(-1 * Math.min(amountNeeded, getAmountAvailable()));
			}
		}
		return " <font color='red'><b> failed." + scrap + "</b></font>";
	}
	
	@Override
	public boolean canScrap() {
		return true;
	}
	
	@Override
	public String scrap() {
		remove(false);
		skillMin = SkillType.EXP_GREEN;
		return EquipmentType.armorNames[type] + " armor scrapped.";
	}
}
