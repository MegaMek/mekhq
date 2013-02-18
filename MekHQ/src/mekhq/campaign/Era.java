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

package mekhq.campaign;

import megamek.common.EquipmentType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Era {

    public static final int E_AOW   = 0;
    public static final int E_RW    = 1;
    public static final int E_SL    = 2;
    public static final int E_1SW   = 3;
    public static final int E_2SW   = 4;
    public static final int E_3SW   = 5;
    public static final int E_4SW   = 6;
    public static final int E_CLAN  = 7;
    public static final int E_JIHAD = 8;
    public static final int E_NUM   = 9;
    
    public static int getEra(int year) {
        if(year < 2570) {
            return E_AOW;
        } 
        else if(year < 2598) {
            return E_RW;
        }
        else if(year < 2785) {
            return E_SL;
        } 
        else if(year < 2828) {
            return E_1SW;
        }
        else if(year < 2864) {
            return E_2SW;
        }
        else if(year < 3028) {
            return E_3SW;
        }
        else if(year < 3050) {
            return E_4SW;
        }
        else if(year < 3067) {
            return E_CLAN;
        }
        else {
            return E_JIHAD;
        }
    }
    
    public static String getEraName(int era) {
        switch(era) {
            case E_AOW:
                return "Age of War";
            case E_RW:
                return "Reunification War";
            case E_SL:
                return "Star League";
            case E_1SW:
                return "First Succession War";
            case E_2SW:
                return "Second Succession War";
            case E_3SW:
                return "Third Succession War";
            case E_4SW:
                return "Fourth Succession War";
            case E_CLAN:
                return "Clan Invasion";
            case E_JIHAD:
                return "Jihad";
            default:
                return "Unknown";
        }
    }
    
    public static String getEraNameFromYear(int year) {
        return getEraName(getEra(year));
    }
 
    /**
     * Convert the eras used in Strategic Ops to the availability-based eras
     * used in the TechManual
     * @param era
     * @return
     */
    public static int convertEra(int era) {
    	switch(era) {
    	case E_AOW:
        case E_RW:
        case E_SL:
            return EquipmentType.ERA_SL;
        case E_1SW:
        case E_2SW:
        case E_3SW:
        case E_4SW:
        	return EquipmentType.ERA_SW;
        case E_CLAN:
        case E_JIHAD:
            return EquipmentType.ERA_CLAN;
        default:
            return -1;
    	}
    }
    
}
