/*
 * IPartWork.java
 * 
 * Copyright (C) 2016 MegaMek team
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
package mekhq.campaign.work;

import java.util.UUID;

import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public interface IPartWork extends IWork {
    
    String getPartName();
    
    int getSkillMin();

    int getBaseTime();
    int getActualTime();
    int getTimeSpent();
    int getTimeLeft();
    void addTimeSpent(int time);
    void resetTimeSpent();
    void resetOvertime();
    boolean isRightTechType(String skillType);
    default boolean canChangeWorkMode() {
        return false;
    }
    
    TargetRoll getAllModsForMaintenance();
    
    void setTeamId(UUID id);
    boolean hasWorkedOvertime();
    void setWorkedOvertime(boolean b);
    int getShorthandedMod();
    void setShorthandedMod(int i);
    
    void updateConditionFromEntity(boolean checkForDestruction);
    void updateConditionFromPart();
    void fix();
    void remove(boolean salvage);
    MissingPart getMissingPart();
    
    String getDesc();
    String getDetails();
    int getLocation();
    
    Unit getUnit();
    
    boolean isSalvaging();
    
    String checkFixable();
    
    void reservePart();
    void cancelReservation();
    boolean isBeingWorkedOn();
    
    int getMassRepairOptionType();
    int getRepairPartType();
    
    public static int findCorrectMassRepairType(IPartWork part) {
        if (part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType) {
            return Part.REPAIR_PART_TYPE.WEAPON;
        } else {            
            return part.getMassRepairOptionType();
        }
    }
    
    public static int findCorrectRepairType(IPartWork part) {
        if ((part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType) ||
                (part instanceof MissingEquipmentPart && ((MissingEquipmentPart)part).getType() instanceof WeaponType)) {
            return Part.REPAIR_PART_TYPE.WEAPON;
        } else {
            if (part instanceof EquipmentPart && ((EquipmentPart)part).getType().hasFlag(MiscType.F_CLUB)) {
                return Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON;
            }
            
            return part.getRepairPartType();
        }
    }
}