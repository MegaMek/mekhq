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
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
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
        super(tonnage, et, equipNum, shots, singleShot, c);
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
    
    @Override
    public double getTonnage() {
        return 0;
    }
    
    //no salvaging of BA parts
    @Override
    public boolean isSalvaging() {
        return false;
    }
    
    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
                if(type.equals(mounted.getType())) {
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
                mounted.setShotsLeft(getFullShots() - (shotsNeeded/getNumTroopers()));
            }
        }
    }
    
    public void loadBin(boolean changeEntity) {
        int shots = Math.min(getAmountAvailable(), shotsNeeded);
        int shotsPerTrooper = shots / getNumTroopers();
        shots = shots * getNumTroopers();
        if(null != unit && changeEntity) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
                if(mounted.getType().equals(type)) {
                    //just a simple reload
                    mounted.setShotsLeft(mounted.getBaseShotsLeft() + shotsPerTrooper);
                } else {
                    //loading a new type of ammo                
                    unload(true);
                    mounted.changeAmmoType((AmmoType)type);
                    mounted.setShotsLeft(shotsPerTrooper);
                }
            }
        }
        changeAmountAvailable(-1 * shots, (AmmoType)type);
        shotsNeeded -= shots;
    }
    
    public void unload(boolean changeEntity) {
        int shots = 0;
        AmmoType curType = (AmmoType)type;
        if(null != unit && changeEntity) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);      
            if(null != mounted) {
                shots = mounted.getBaseShotsLeft() * getNumTroopers();
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
        //shouldnt be here
        return;
    }
    
    @Override
    public Part getNewPart() {
        return new AmmoStorage(1,type,((AmmoType)type).getShots() * getNumTroopers(),campaign);
    }
    
    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new AmmoStorage(1,type,((AmmoType)type).getShots()  * getNumTroopers(),campaign);
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
        toReturn += ((AmmoType)type).getShots() * getNumTroopers() + " shots<br/>";
        String[] inventories = campaign.getPartInventory(getNewPart());
        toReturn += inventories[1] + " in transit, " + inventories[2] + " on order<br>"; 
        toReturn += Utilities.getCurrencyString(getBuyCost()) + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public boolean needsMaintenance() {
        return false;
    }
}