/*
 * BattleArmorAmmoBin.java
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


import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class BattleArmorAmmoBin extends AmmoBin implements IAcquisitionWork {

    /**
     * Battle Armor ammo bins need to look for shots for all the remaining troopers in the 
     * squad. 
     * TODO: Think about how to handle the case of understrength squads. Right now they 
     * pay for more ammo than they need, but this is easier than trying to track ammo per suit
     * and adjust for different ammo types when suits are added and removed from squads.
     */
    private static final long serialVersionUID = 2421186617583650648L;
    
    public BattleArmorAmmoBin() {
        this(0, null, -1, 0, false, null);
    }
    
    public BattleArmorAmmoBin(int tonnage, EquipmentType et, int equipNum, int shots, boolean singleShot, Campaign c) {
        super(tonnage, et, equipNum, shots, singleShot, false, c);
    }
    
    public int getNumTroopers() {
        if(null != unit && unit.getEntity() instanceof BattleArmor) {
            //we are going to base this on the full squad size, even though this makes understrength
            //squads overpay for their ammo - that way suits can be moved around without having to adjust
            //ammo - Tech: "oh you finally got here. Check in the back corner, we stockpiled some ammo for 
            //you."
            return ((BattleArmor)unit.getEntity()).getSquadSize();
        }
        return 0;
    }
    
    //no salvaging of BA parts
    @Override
    public boolean isSalvaging() {
        return false;
    }
    
    /*@Override
    public int getFullShots() {
    	return super.getFullShots() * getNumTroopers();
    }*/
    
    @Override
    protected int getCurrentShots() {
    	int shots = getFullShots() * getNumTroopers() - shotsNeeded;
    	//replace with actual entity values if entity not null because the previous number will not
    	//be correct for ammo swaps
    	if(null != unit && null != unit.getEntity()) {
    		Mounted m = unit.getEntity().getEquipment(equipmentNum);
    		if(null != m) {
    			shots = m.getBaseShotsLeft() * getNumTroopers();
    		}
    	}
    	return shots;
    }
    
    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
            	long currentMuniType = 0;			
				if(mounted.getType() instanceof AmmoType) {
					currentMuniType = ((AmmoType)mounted.getType()).getMunitionType();
				}
				if(currentMuniType == getMunitionType()) {
                    shotsNeeded = (getFullShots() - mounted.getBaseShotsLeft()) * getNumTroopers();
                } else {
                    //we have a change of munitions
                    shotsNeeded = getFullShots() * getNumTroopers();
                }
            }
        }
    }
    
    @Override 
	public int getBaseTime() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(!type.equals(mounted.getType())) {
					return 30;
				}
			}
		}
		return 15;
	}
	
	@Override
	public int getDifficulty() {
		return 0;
	}
    
    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
                mounted.setHit(false);
                mounted.setDestroyed(false);
                mounted.setRepairable(true);
                unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
                mounted.setShotsLeft(getFullShots() - shotsNeeded/getNumTroopers());
            }
        }
    }
    
    @Override
    public void loadBin() {
        int shots = Math.min(getAmountAvailable(), shotsNeeded);
        int shotsPerTrooper = shots / getNumTroopers();
        shots = shotsPerTrooper * getNumTroopers();
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
            	if(mounted.getType().equals(type) 						
						&& ((AmmoType)mounted.getType()).getMunitionType() == getMunitionType()) {
                    //just a simple reload
                    mounted.setShotsLeft(mounted.getBaseShotsLeft() + shotsPerTrooper);
                } else {
                    //loading a new type of ammo                
                    unload();
                    mounted.changeAmmoType((AmmoType)type);
                    mounted.setShotsLeft(shotsPerTrooper);
                }
            }
        }
        changeAmountAvailable(-1 * shots, (AmmoType)type);
        shotsNeeded -= shots;
    }
    
    @Override
    public void unload() {
        int shots = 0;
        AmmoType curType = (AmmoType)type;
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);      
            if(null != mounted) {
                shots = mounted.getBaseShotsLeft() * getNumTroopers();
                mounted.setShotsLeft(0);
                curType = (AmmoType)mounted.getType();
            }
        }
        shotsNeeded = getFullShots() * getNumTroopers();
        if(shots > 0) {
            changeAmountAvailable(shots, curType);
        }   
    }
    
    @Override
    public String checkFixable() {
    	int amountAvailable = getAmountAvailable();
    	if(amountAvailable > 0 && amountAvailable < getNumTroopers()) {
    		return "Cannot do a partial reload of Battle Armor ammo less than the number of troopers";
    	}
    	return super.checkFixable();
    	
    }

    @Override
    public void remove(boolean salvage) {
        //shouldn't be here
    }
    
    @Override
    public Part getNewPart() {
    	int shots = (int) Math.floor(1000/((AmmoType)type).getKgPerShot());
		if(shots <= 0) {
			//FIXME: no idea what to do here, these really should be fixed on the MM side
			//because presumably this is happening because KgperShot is -1 or 0
			shots = 20;
		}
        return new AmmoStorage(1,type,shots,campaign);
    }
    
    @Override
    public IAcquisitionWork getAcquisitionWork() {
    	int shots = (int) Math.floor(1000/((AmmoType)type).getKgPerShot());
		if(shots <= 0) {
			//FIXME: no idea what to do here, these really should be fixed on the MM side
			//because presumably this is happening because KgperShot is -1 or 0
			shots = 20;
		}
        return new AmmoStorage(1,type,shots,campaign);
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font size='2'";
        
        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        toReturn += getAcquisitionExtraDesc() + "<br/>";
        PartInventory inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories.getTransitOrderedDetails() + "<br/>"; 
        toReturn += getBuyCost().toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }
	
    @Override
    public String getAcquisitionDisplayName() {
    	return type.getDesc();
    }    

    private int calculateShots() {
        int shots = (int) Math.floor(1000/((AmmoType)type).getKgPerShot());
		if(shots <= 0) {
			//FIXME: no idea what to do here, these really should be fixed on the MM side
			//because presumably this is happening because KgperShot is -1 or 0
			shots = 20;
		}
		
		return shots;
    }
    
	@Override
	public String getAcquisitionExtraDesc() {
		return calculateShots() + " shots";
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

    public boolean needsMaintenance() {
        return false;
    }
    
    public boolean canNeverScrap() {
    	return true;
	}
    
    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
        	typeName = type.getName();
        } else {
            type = EquipmentType.get(typeName);
        }
        
        
        //FIXME, this is a crappy hack, but we want something along these lines
        //to make sure that BA ammo gets removed from all parts - It might be better to run
        //a check on the XML loading after restore - we also will need to to the same for proto
        //ammo but we can only do this if we have all the correct ammo rack sizes for the 
        //generics (e.g. LRM1, LRM2, LRM3, etc)
        /*if(typeName.contains("BA-")) {
        	String newTypeName = "IS" + typeName.split("BA-")[1];
        	EquipmentType newType = EquipmentType.get(newTypeName);
        	if(null != newType) {
        		typeName = newTypeName;
        		type = newType;
        	}
        }*/
        

        if (type == null) {
            System.err
            .println("Mounted.restore: could not restore equipment type \""
                    + typeName + "\"");
            return;
        }
        try {
        	equipTonnage = type.getTonnage(null);
        } catch(NullPointerException ex) {
            MekHQ.getLogger().error(BattleArmorAmmoBin.class, "restore", ex);
        }
    }

    @Override
    public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.AMMO;
    }
}
