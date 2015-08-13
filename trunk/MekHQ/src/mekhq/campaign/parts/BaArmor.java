/*
 * BaArmor.java
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
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TreeSet;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class BaArmor extends Part implements IAcquisitionWork {
    private static final long serialVersionUID = 5275226057484468868L;

    protected int amount;
    protected int amountNeeded;
    protected int type;
    private int location;
    private boolean clan;
    
    public static boolean canBeClan(int type) {
        return type == EquipmentType.T_ARMOR_BA_STANDARD || type == EquipmentType.T_ARMOR_BA_STEALTH_BASIC 
                || type == EquipmentType.T_ARMOR_BA_STEALTH_IMP || type == EquipmentType.T_ARMOR_BA_STEALTH
                || type == EquipmentType.T_ARMOR_BA_FIRE_RESIST; 
    }
    
    public static boolean canBeIs(int type) {
        return type != EquipmentType.T_ARMOR_BA_FIRE_RESIST;
    }
    

    public static double getPointsPerTon(int t, boolean isClan) {
        return 1.0/EquipmentType.getBaArmorWeightPerPoint(t, isClan);
    }
    
    public BaArmor() {
        this(0, 0, 0, -1, false, null);
    }
    
    public BaArmor(int tonnage, int points, int type, int loc, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, c);
        this.amount = points;
        this.location = loc;
        this.type = type;
        this.clan = clan;
        this.name = "Armor";
        if(type > -1) {
        	this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
    }
    
    public BaArmor clone() {
        BaArmor clone = new BaArmor(0, amount, type, location, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }
    
    @Override
    public double getTonnage() {
        return EquipmentType.getBaArmorWeightPerPoint(type, clan) * getActualAmount();
    }
    
    public int getPointCost() {
        switch(type) {
        case EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED:
            return 12500;
        case EquipmentType.T_ARMOR_BA_MIMETIC:
        case EquipmentType.T_ARMOR_BA_STEALTH:
            return 15000;
        case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
            return 12000;
        case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
            return 20000;
        case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
            return 50000;
        case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
        case EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE:
        case EquipmentType.T_ARMOR_BA_STANDARD:
        default:
            return 10000;           
        }
    }
    
    private double getPointsPerTon() {
        return getPointsPerTon(type, clan);
    }
    
    public int getType() {
        return type;
    }
    
    @Override
    public long getCurrentValue() {
        return getActualAmount() * getPointCost();
    }
    
    public int getActualAmount() {
        if(null != unit) {
            int currentArmor = unit.getEntity().getArmorForReal(location, false);
            if(currentArmor < 0) {
                currentArmor = 0;
            }
            return currentArmor;
        }
        return amount;
    }
    
    
    public double getTonnageNeeded() {
        return amountNeeded / getPointsPerTon();
    }
    
    public long getValueNeeded() {
        return adjustCostsForCampaignOptions((long)(amountNeeded * getPointCost()));
    }
    
    @Override
    public long getStickerPrice() {
        //always in 5-ton increments
        return (long)(5 * getPointsPerTon() * getPointCost());
    }
    
    @Override
    public long getBuyCost() {
        return getStickerPrice();
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
        if (getAssignedTeamId() != null) {
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
        if (getMode() != Modes.MODE_NORMAL) {
            toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
        }
        toReturn += "</font></html>";
        return toReturn;
    }
  
    @Override
    public String getDetails() {
        if(null != unit) {
            String availability = "";
            int amountAvailable = getAmountAvailable();
            if(!isSalvaging()) {
                String[] inventories = campaign.getPartInventory(getNewPart());
                if(amountAvailable == 0) {
                    availability = "<br><font color='red'>No armor ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
                } else if(amountAvailable < amountNeeded) {
                    availability = "<br><font color='red'>Only " + amountAvailable + " available ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
                }
            }
            return unit.getEntity().getLocationName(location) + ", " + amountNeeded + " points" + availability;
        }
        return amount + " points";
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

    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public void setAmountNeeded(int needed) {
        this.amountNeeded = needed;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof BaArmor
                && isClanTechBase() == part.isClanTechBase()
                && ((BaArmor)part).getType() == this.getType()
                && getRefitId() == part.getRefitId();
    }
    
    @Override
    public boolean isSameStatus(Part part) {
        return !hasParentPart() && !part.hasParentPart() && this.getDaysToArrival() == part.getDaysToArrival();
    }

    @Override
    public int getTechLevel() {
    	//just use what is already in equipment types to figure it out
    	EquipmentType etype = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
    	if(null == etype) {
    		return TechConstants.T_TECH_UNKNOWN;
    	}
    	int techLevel = etype.getTechLevel(campaign.getCalendar().get(GregorianCalendar.YEAR));
    	if(techLevel == TechConstants.T_TECH_UNKNOWN && !etype.getTechLevels().isEmpty()) {
        	//If this is tech unknown we are probably using a part before its date of introduction
        	//in this case, try to give it the date of the earliest entry if it exists
        	SortedSet<Integer> keys = new TreeSet<Integer>(etype.getTechLevels().keySet());
        	techLevel = etype.getTechLevels().get(keys.first());
        }
        if ((techLevel != TechConstants.T_ALLOWED_ALL && techLevel < 0) || techLevel >= TechConstants.T_ALL)
            return TechConstants.T_TECH_UNKNOWN;
        else
            return techLevel;
    }

    public double getArmorWeight(int points) {
        return points * 50/1000.0;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<amount>"
                +amount
                +"</amount>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<location>"
                +location
                +"</location>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<amountNeeded>"
                +amountNeeded
                +"</amountNeeded>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<type>"
                +type
                +"</type>");
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
            } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                location = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("amountNeeded")) {
                amountNeeded = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                type = Integer.parseInt(wn2.getTextContent());
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
        case EquipmentType.T_ARMOR_BA_STEALTH:
            if(era == EquipmentType.ERA_CLAN) {
                return EquipmentType.RATING_E;
            } else {
                return EquipmentType.RATING_X;
            } 
        case EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE:
        case EquipmentType.T_ARMOR_BA_STANDARD:
        case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
            if(era == EquipmentType.ERA_CLAN) {
                return EquipmentType.RATING_E;
            } else {
                return EquipmentType.RATING_F;
            } 
        default:
            if(era == EquipmentType.ERA_CLAN) {
                return EquipmentType.RATING_F;
            } else {
                return EquipmentType.RATING_X;
            }           
        }
        
    }
    
    @Override
    public int getIntroDate() {
    	EquipmentType etype = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
    	if(null == etype) {
    		return TechConstants.T_TECH_UNKNOWN;
    	}
    	return etype.getIntroductionDate();
    }
    
    @Override
    public int getExtinctDate() {
    	EquipmentType etype = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
    	if(null == etype) {
    		return TechConstants.T_TECH_UNKNOWN;
    	}
    	return etype.getExtinctionDate();
    }
    
    @Override
    public int getReIntroDate() {
    	EquipmentType etype = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
    	if(null == etype) {
    		return TechConstants.T_TECH_UNKNOWN;
    	}
    	return etype.getReintruductionDate();
    }
    
    public int getTechRating() {
		EquipmentType etype = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
    	if(null == etype) {
    		return EquipmentType.RATING_E;
    	}
    	return etype.getTechRating();
	}

    @Override
    public void fix() {
        int amount = Math.min(getAmountAvailable(), amountNeeded);
        int curAmount = unit.getEntity().getArmorForReal(location, false);
        if(curAmount < 0) {
            curAmount = 0;
        }
        unit.getEntity().setArmor(amount + curAmount, location, false);
        changeAmountAvailable(-1 * amount);
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
        return new BaArmor(0, (int)Math.round(5 * getPointsPerTon()), type, -1, clan, campaign);
    }

    @Override
    public void remove(boolean salvage) {
    	unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, false);
        if(salvage) {
            changeAmountAvailable(amountNeeded);
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
        int currentArmor = unit.getEntity().getArmorForReal(location, false);
        if(currentArmor < 0) {
            currentArmor = 0;
        }
        if(salvaging || isMountedOnDestroyedLocation()) {
            amountNeeded = currentArmor;
            amount = unit.getEntity().getOArmor(location, false) - amountNeeded;
        } else {            
            amountNeeded = unit.getEntity().getOArmor(location, false) - currentArmor;
            amount = currentArmor;
        }
    }
    
    @Override 
	public int getBaseTime() {
		if(isSalvaging()) {
			return getBaseTimeFor(unit.getEntity()) * amountNeeded;
		}
		return getBaseTimeFor(unit.getEntity()) * Math.min(amountNeeded, getAmountAvailable());
	}
	
	@Override
	public int getDifficulty() {
		return -2;
	}
    
    @Override
    public boolean isSalvaging() {
        return (salvaging || isMountedOnDestroyedLocation()) && amountNeeded > 0;
    }
    
    @Override
    public String succeed() {
        boolean tmpSalvaging = salvaging;
        String toReturn = super.succeed();
        if(tmpSalvaging) {
            salvaging = true;
            updateConditionFromEntity(false);
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
            unit.getEntity().setArmor(armor, location, false);
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
        String bonus = getAllAcquisitionMods().getValueAsString();
        if(getAllAcquisitionMods().getValue() > -1) {
            bonus = "+" + bonus;
        }
        bonus = "(" + bonus + ")";
        String toReturn = "<html><font size='2'";
        
        toReturn += ">";
        toReturn += "<b>" + getName() + "</b> " + bonus + "<br/>";
        toReturn += ((int)Math.round(getPointsPerTon())) * 5 + " points (5 tons)<br/>";
        String[] inventories = campaign.getPartInventory(getNewPart());
        toReturn += inventories[1] + " in transit, " + inventories[2] + " on order<br>"; 
        toReturn += Utilities.getCurrencyString(adjustCostsForCampaignOptions(getStickerPrice())) + "<br/>";
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
        if(isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }  
        //availability mod
        int avail = getAvailability(campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
        return target;
    }
    
    public Part getNewPart() {
        return new BaArmor(0, (int)Math.round(5 * getPointsPerTon()), type, -1, clan, campaign);
    }
    
    public boolean isEnoughSpareArmorAvailable() {
        return getAmountAvailable() >= amountNeeded;
    }
    
    public int getAmountAvailable() {
        for(Part part : campaign.getSpareParts()) {
            if(part instanceof BaArmor) {
                BaArmor a = (BaArmor)part;
                if(a.isClanTechBase() == clan 
                        && a.getType() == type
                        && !a.isReservedForRefit() && a.isPresent()) {
                    return a.getAmount();
                }
            }
        }
        return 0;
    }
    
    public void changeAmountAvailable(int amount) {
        BaArmor a = null;
        for(Part part : campaign.getSpareParts()) {
            if(part instanceof BaArmor 
                    && ((BaArmor)part).isClanTechBase() == clan 
                    && ((BaArmor)part).getType() == type
                    && getRefitId() == part.getRefitId()
                    && part.isPresent()) {
                a = (BaArmor)part;                
                a.setAmount(a.getAmount() + amount);
                break;
            }
        }
        if(null != a && a.getAmount() <= 0) {
            campaign.removePart(a);
        } else if(null == a && amount > 0) {            
            campaign.addPart(new BaArmor(getUnitTonnage(), amount, type, -1, isClanTechBase(), campaign), 0);
        }
        campaign.updateAllArmorForNewSpares();
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
        return "Protomech armor scrapped.";
    }

    @Override
    public boolean isInSupply() {
        int currentArmor = Math.max(0, unit.getEntity().getArmorForReal(location, false));
        int fullArmor = unit.getEntity().getOArmor(location, false);
        int neededArmor = fullArmor - currentArmor;
        return neededArmor <= getAmountAvailable();
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
        d = Math.min(d, amount);
        unit.getEntity().setArmor(d, location);
        updateConditionFromEntity(false);
    }
    
    public void setLocation(int l) {
        location = l;
    }
    
    @Override
    public boolean isPriceAdustedForAmount() {
        return true;
    }

	@Override
	public String getLocationName() {
		return unit.getEntity().getLocationName(location);
	}
}
