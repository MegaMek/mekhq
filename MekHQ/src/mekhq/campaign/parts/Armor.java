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
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.team.SupportTeam;
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
    
    public Armor() {
    	this(false, 0, 0, 0);
    }
    
    public Armor(boolean salvage, int tonnage, int t, int points) {
    	this(salvage, tonnage, t, points, -1, false);
    }
    
    public Armor(boolean salvage, int tonnage, int t, int points, int loc, boolean r) {
        // Amount is used for armor quantity, not tonnage
        super(false, tonnage);
        this.type = t;
        this.amount = points;
        this.location = loc;
        this.rear = r;
        this.name = "Armor";
        if(type > -1) {
        	this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
        computeCost();
    }
    
    protected void computeCost() {
    	double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type);
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        double armorWeight = amount / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;

    	this.cost = (int)(armorWeight * EquipmentType.getArmorCost(type));
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
		if (getTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>Replace " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		if(getAmountAvailable() > 0) {
			toReturn += "" + getTimeLeft() + " minutes" + scheduled;
			toReturn += ", " + SupportTeam.getRatingName(getSkillMin());
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
					availability = "<br>No spare armor available";
				} else if(amountAvailable < amountNeeded) {
					availability = "<br>Only " + amountAvailable + " points of armor available";
				}
			}
			return unit.getEntity().getLocationName(location) + rearMount + ", " + amountNeeded + " points" + availability;
		}
		return "";
	}
    
    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
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

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof Armor
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((Armor)part).getType();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_ARMOR;
    }

    @Override
    public boolean isClanTechBase() {
        // Armor tech base is not used (Clan/IS can use each other's armor for now)
        // TODO Set Tech base correctly for armor
        // Clan FF and IS FF do not have the same armor points per ton
        return false;
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

        boolean isClanArmor = false;
        if (isClanTechBase())
            isClanArmor= true;

        double armorPointMultiplier = EquipmentType.getArmorPointMultiplier(getType(), isClanArmor);
        double armorPerTon = 16.0 * armorPointMultiplier;
        if (getType() == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        
        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

    public int getCost (int amount) {
        return (int) Math.round(getArmorWeight(amount) * EquipmentType.getArmorCost(getType()));
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
		reduceAmountAvailable(amount);
		updateConditionFromEntity();
	}

	@Override
	public Part getMissingPart() {
		//no such thing
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
		//cycle through spare parts and add to existing armor if found
		if(salvage) {
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof Armor) {
					Armor a = (Armor)part;
					if(a.getType() == type) {
						a.setAmount(a.getAmount() + amountNeeded);
						unit.campaign.updateAllArmorForNewSpares();
						return;
					}
				}
			}
			//if we are still here then we did not find any armor, so lets create a new part and stick it in spares
			Armor newArmor = new Armor(true,tonnage,type,amountNeeded,-1,false);
			unit.campaign.addPart(newArmor);
		}
		updateConditionFromEntity();
	}

	private int getBaseTimeFor(Entity entity) {
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
		int currentArmor = unit.getEntity().getArmor(location, rear);
		if(currentArmor < 0) {
			currentArmor = 0;
		}
		if(salvaging) {
			amountNeeded = currentArmor;
		} else {			
			amountNeeded = unit.getEntity().getOArmor(location, rear) - currentArmor;
		}
		//time should be based on amount available if less than amount needed
		if(amountNeeded > 0) {
			if(salvaging) {
				time = getBaseTimeFor(unit.getEntity()) * amountNeeded;
			} else {
				time = getBaseTimeFor(unit.getEntity()) * Math.min(amountNeeded, getAmountAvailable());
			}
			difficulty = -2;
		} else {
			time = 0;
			difficulty = 0;
		}
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
		//this should never happen for armor
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
		toReturn += getCostString() + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
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

	@Override
	public Part getNewPart() {
		if(null != unit) {
			// armor is checked for in 5-ton increments
			int armorType = unit.getEntity().getArmorType(location);
			double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(armorType, unit.getEntity().getTechLevel());
			if (armorType == EquipmentType.T_ARMOR_HARDENED) {
				armorPerTon = 8.0;
			}
			int points = (int) Math.floor(armorPerTon * 5);
			return new Armor(false, (int) unit.getEntity().getWeight(), armorType, points);
		}
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
					if(a.getType() == type) {
						return a.getAmount();
					}
				}
			}
			return 0;
		}
		return 0;
	}
	
	public void reduceAmountAvailable(int amount) {
		if(null != unit) {
			Armor a = null;
			for(Part part : unit.campaign.getSpareParts()) {
				if(part instanceof Armor) {
					a = (Armor)part;
					if(a.getType() == type) {
						a.setAmount(a.getAmount() - amount);
						break;
					}
				}
			}
			if(null != a && a.getAmount() <= 0) {
				unit.campaign.removePart(a);
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
		if(skillMin > SupportTeam.EXP_ELITE) {
			scrap = " Armor supplies lost!";
			if(isSalvaging()) {
				remove(false);
			} else {
				skillMin = SupportTeam.EXP_GREEN;
				reduceAmountAvailable(Math.min(amountNeeded, getAmountAvailable()));
			}
		}
		return " <font color='red'><b> failed." + scrap + "</b></font>";
	}
}
