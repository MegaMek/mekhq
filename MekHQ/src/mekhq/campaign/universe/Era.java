/*
 * Era.java
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

package mekhq.campaign.universe;

import megamek.common.EquipmentType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com> 
 * September 2017 - Update Eras to
 * match MUL, and added reference to 4th era code
 */
public class Era {

    public static final int E_AOW   = 0;
    public static final int E_SL    = 1;
    public static final int E_ESW    = 2;
    public static final int E_LSW_LOSTECH   = 3;
    public static final int E_LSW_RENAISSANCE   = 4;
    public static final int E_CLAN_INVASION   = 5;
    public static final int E_CIVIL_WAR   = 6;
    public static final int E_JIHAD  = 7;
    public static final int E_EARLY_REPUBLIC = 8;
    public static final int E_LATE_REPUBLIC = 9;
    public static final int E_DARK_AGES = 10;
    public static final int E_NUM   = 11;
    
    public static int getEra(int year) {
        if(year < 2570) {
            return E_AOW;
        } 
        else if(year < 2780) {
            return E_SL;
        }
        else if(year < 2900) {
            return E_ESW;
        } 
        else if(year < 3019) {
            return E_LSW_LOSTECH;
        }
        else if(year < 3049) {
            return E_LSW_RENAISSANCE;
        }
        else if(year < 3061) {
            return E_CLAN_INVASION;
        }
        else if(year < 3067) {
            return E_CIVIL_WAR;
        }
        else if(year < 3085) {
            return E_JIHAD;
        }
        else if(year <3100) {
            return E_EARLY_REPUBLIC;
        }
        else if(year <3130) {
            return E_LATE_REPUBLIC;
        }
        else {
            return E_DARK_AGES;
        }
    }
    
    public static String getEraName(int era) {
        switch(era) {
            case E_AOW:
                return "Age of War";
            
            case E_SL:
                return "Star League";
            
            case E_ESW:
                return "Early Sucession War";
            
            case E_LSW_LOSTECH:
                return "Late Succession War - LosTech";
            
            case E_LSW_RENAISSANCE:
                return "Late Succession War - Renaissance";
            
            case E_CLAN_INVASION:
                return "Clan Invasion";
            
            case E_CIVIL_WAR:
                return "Civil War";
            
            case E_JIHAD:
                return "Jihad";
            
            case E_EARLY_REPUBLIC:
                return "Early Republic";
            
            case E_LATE_REPUBLIC:
                return "Late Republic";
            
            case E_DARK_AGES:
                return "Dark Ages";
            
            default:
                return "Unknown";
        }
    }
    
    public static String getEraNameFromYear(int year) {
        return getEraName(getEra(year));
    }
 
    /**
     * Convert the eras used in Strategic Ops to the availability-based eras
     * used in the TechManual. 
     * Updated for 4th Era Code.
     * @param era
     * @return
     */
    
    public static int convertEra(int era) {
    	switch(era) {
        case E_AOW:
        case E_SL:
            return EquipmentType.ERA_SL;
        case E_ESW:
        case E_LSW_LOSTECH:
        case E_LSW_RENAISSANCE:
            return EquipmentType.ERA_SW;
        case E_CLAN_INVASION:
        case E_CIVIL_WAR:
        case E_JIHAD:
        case E_EARLY_REPUBLIC:
        case E_LATE_REPUBLIC:
            return EquipmentType.ERA_CLAN;
        case E_DARK_AGES:
            return EquipmentType.ERA_DA;
        default:
            return -1;
    	}
    }
    
}
