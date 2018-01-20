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
import java.text.DecimalFormat;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ITechnology;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.WorkTime;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Armor extends Part implements IAcquisitionWork {
	private static final long serialVersionUID = 5275226057484468868L;
	protected int type;
    protected int amount;
    protected int amountNeeded;
    protected int location;
    private boolean rear;
    protected boolean clan;

    public Armor() {
    	this(0, 0, 0, -1, false, false, null);
    }

    public Armor(int tonnage, int t, int points, int loc, boolean r, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, c);
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
    	Armor clone = new Armor(0, type, amount, -1, false, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
    	return amount / getArmorPointsPerTon();
    }

    @Override
    public long getCurrentValue() {
    	return (long)(getTonnage() * EquipmentType.getArmorCost(type));
    }

    public double getTonnageNeeded() {
    	double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type, isClanTechBase());
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        return amountNeeded / armorPerTon;
    }

    public long getValueNeeded() {
    	return adjustCostsForCampaignOptions((long)(getTonnageNeeded() * EquipmentType.getArmorCost(type)));
    }

    @Override
    public long getStickerPrice() {
    	//always in 5-ton increments
    	return (long)(5 * EquipmentType.getArmorCost(type));
    }

    @Override
    public long getBuyCost() {
        return getStickerPrice();
    }

    public String getDesc() {
    	if(isSalvaging()) {
    		return super.getDesc();
    	}
		String bonus = getAllMods(null).getValueAsString();
		if (getAllMods(null).getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";

		String scheduled = "";
		if (getTeamId() != null) {
			scheduled = " (scheduled) ";
		}

		toReturn += ">";
		toReturn += "<b>Replace " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		if(getAmountAvailable() > 0) {
			toReturn += "" + getTimeLeft() + " minutes" + scheduled;
			if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
			    toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
			}
			toReturn += " " + bonus;
		}
		if (getMode() != WorkTime.NORMAL) {
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
			if(!isSalvaging()) {
				String availability = "";
				int amountAvailable = getAmountAvailable();
				String[] inventories = campaign.getPartInventory(getNewPart());

	            String orderTransitString = getOrderTransitStringForDetails(inventories);

				if(amountAvailable == 0) {
					availability = "<br><font color='red'>No armor " + orderTransitString + "</font>";
				} else if(amountAvailable < amountNeeded) {
					availability = "<br><font color='red'>Only " + amountAvailable + " available " + orderTransitString + "</font>";
				} else {
	                availability = "<br><font color='green'>" + amountAvailable + " available " + orderTransitString + "</font>";
	            }

				return unit.getEntity().getLocationName(location) + rearMount + ", " + amountNeeded + " points" + availability;
			}
			return unit.getEntity().getLocationName(location) + rearMount + ", " + amount + " points";
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

    public String getLocationName() {
    	return unit.getEntity().getLocationName(location);
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

    public boolean isSameType(Armor armor) {
    	if(getType() == EquipmentType.T_ARMOR_STANDARD
    			&& armor.getType() == EquipmentType.T_ARMOR_STANDARD) {
    		//standard armor is compatible between clan and IS
    		return true;
    	}
    	return getType() == armor.getType()  && isClanTechBase() == armor.isClanTechBase();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof Armor
                && isSameType((Armor)part)
                && getRefitId() == part.getRefitId();
    }

    @Override
    public boolean isSameStatus(Part part) {
    	return this.getDaysToArrival() == part.getDaysToArrival();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EquipmentType.getArmorTechAdvancement(type, clan);
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
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
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
				+"<clan>"
				+clan
				+"</clan>");
		writeToXmlEnd(pw1, indent);
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
	public void fix() {
		int amountFound = Math.min(getAmountAvailable(), amountNeeded);
		int fixAmount = Math.min(amount + amountFound, unit.getEntity().getOArmor(location, rear));
		unit.getEntity().setArmor(fixAmount, location, rear);
		changeAmountAvailable(-1 * amountFound);
		updateConditionFromEntity(false);
		skillMin = SkillType.EXP_GREEN;
		shorthandedMod = 0;
	}

	@Override
	public String find(int transitDays) {
	    Part newPart = getNewPart();
	    newPart.setBrandNew(true);
        newPart.setDaysToArrival(transitDays);
        if(campaign.buyPart(newPart, transitDays)) {
            return "<font color='green'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='red'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
        }
	}

	@Override
    public Object getNewEquipment() {
        return getNewPart();
    }

	@Override
	public String failToFind() {
	    resetDaysToWait();
		return "<font color='red'><b> part not found</b>.</font>";
	}

	@Override
	public MissingPart getMissingPart() {
		//no such thing
		return null;
	}

	@Override
    public IAcquisitionWork getAcquisitionWork() {
        return new Armor(0, type, (int)Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
    }

	@Override
	public void remove(boolean salvage) {
		unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
		if(salvage) {
			changeAmountAvailable(amount);
		}
		updateConditionFromEntity(false);
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
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(isReservedForRefit()) {
			return;
		}
		if(null == unit) {
			return;
		}
		amount = unit.getEntity().getArmorForReal(location, rear);
		if(amount < 0) {
			amount = 0;
		}
		amountNeeded = unit.getEntity().getOArmor(location, rear) - amount;
	}

	@Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return getBaseTimeFor(unit.getEntity()) * amount;
		}
		return getBaseTimeFor(unit.getEntity()) * Math.min(amountNeeded, getAmountAvailable());
	}

	@Override
	public int getDifficulty() {
		return -2;
	}

	@Override
	public boolean isSalvaging() {
		return super.isSalvaging() && amount > 0;
	}

	@Override
	public boolean needsFixing() {
		return amountNeeded > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			int armor = Math.min(amount, unit.getEntity().getOArmor(location, rear));
			if(armor == 0) {
				armor = IArmorState.ARMOR_DESTROYED;
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
		if (isMountedOnDestroyedLocation()) {
			return unit.getEntity().getLocationName(location) + " is destroyed.";
		}
		return null;
	}

	@Override
	public boolean isMountedOnDestroyedLocation() {
		return null != unit && unit.isLocationDestroyed(location);
	}

	@Override
	public boolean onBadHipOrShoulder() {
		return null != unit && unit.hasBadHipOrShoulder(location);
	}

	@Override
	public String getAcquisitionDesc() {
		String toReturn = "<html><font size='2'";

		toReturn += ">";
		toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
		toReturn += getAcquisitionExtraDesc() + "<br/>";
		String[] inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories[1] + " in transit, " + inventories[2] + " on order<br>";
		toReturn += Utilities.getCurrencyString(adjustCostsForCampaignOptions(getStickerPrice())) + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}

    @Override
    public String getAcquisitionDisplayName() {
    	return getName();
    }

	@Override
	public String getAcquisitionExtraDesc() {
		return ((int)Math.round(getArmorPointsPerTon())) * 5 + " points (5 tons)";
	}

	@Override
	public String getAcquisitionName() {
		return getName();
	}

	@Override
    public String getAcquisitionBonus() {
		String bonus = getAllAcquisitionMods().getValueAsString();
		if(getAllAcquisitionMods().getValue() > -1) {
			bonus = "+" + bonus;
		}

		return "(" + bonus + ")";
    }

	@Override
	public Part getAcquisitionPart() {
		return getNewPart();
	}

	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if(isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        //availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
        return target;
    }

	public double getArmorPointsPerTon() {
		//if(null != unit) {
			// armor is checked for in 5-ton increments
			//int armorType = unit.getEntity().getArmorType(location);
	    double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type, clan);
	    if (type == EquipmentType.T_ARMOR_HARDENED) {
	        armorPerTon = 8.0;
	    }
	    return armorPerTon;
		//}
		//return 0.0;
	}

	public Part getNewPart() {
		return new Armor(0, type, (int)Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
	}

	public boolean isEnoughSpareArmorAvailable() {
		return getAmountAvailable() >= amountNeeded;
	}

	public int getAmountAvailable() {
		for(Part part : campaign.getSpareParts()) {
			if(part instanceof Armor) {
				Armor a = (Armor)part;
				if(isSameType(a) && !a.isReservedForRefit() && a.isPresent()) {
					return a.getAmount();
				}
			}
		}
		return 0;
	}

	public void changeAmountAvailable(int amount) {
		Armor a = null;
		for(Part part : campaign.getSpareParts()) {
			if(part instanceof Armor && isSameType((Armor)part)
					&& getRefitId() == part.getRefitId()
					&& part.isPresent()) {
				a = (Armor)part;
				a.setAmount(a.getAmount() + amount);
				break;
			}
		}
		if(null != a && a.getAmount() <= 0) {
			campaign.removePart(a);
		} else if(null == a && amount > 0) {
			campaign.addPart(new Armor(getUnitTonnage(), type, amount, -1, false, isClanTechBase(), campaign), 0);
		}
	}

	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		shorthandedMod = 0;
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
	public String scrap() {
		remove(false);
		skillMin = SkillType.EXP_GREEN;
		return EquipmentType.armorNames[type] + " armor scrapped.";
	}

    @Override
    public boolean isInSupply() {
        //int currentArmor = Math.max(0, unit.getEntity().getArmorForReal(location, rear));
        //int fullArmor = unit.getEntity().getOArmor(location, rear);
        //int neededArmor = fullArmor - currentArmor;
        return amountNeeded <= getAmountAvailable();
    }

    @Override
    public String getQuantityName(int quan) {
        double totalTon = quan * getTonnage();
        String report = "" + DecimalFormat.getInstance().format(totalTon) + " tons of " + getName();
        if(totalTon == 1.0) {
            report = "" + DecimalFormat.getInstance().format(totalTon) + " ton of " + getName();
        }
        return report;
    }

    @Override
    public String getArrivalReport() {
        double totalTon = quantity * getTonnage();
        String report = getQuantityName(quantity);
        if(totalTon == 1.0) {
            report += " has arrived";
        } else {
            report += " have arrived";
        }
        return report;
    }

    public void doMaintenanceDamage(int d) {
        int current = unit.getEntity().getArmor(location, rear);
        if(d >= current) {
            unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
        } else {
            unit.getEntity().setArmor(current - d, location, rear);

       }
       updateConditionFromEntity(false);
    }

    @Override
    public boolean isPriceAdustedForAmount() {
        return true;
    }

	public void changeType(int ty, boolean cl) {
		this.type = ty;
		this.clan = cl;
		this.name = "Armor";
        if(type > -1) {
        	this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
	}

	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ARMOR;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
