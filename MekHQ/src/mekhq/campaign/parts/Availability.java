/*
 * Availability.java
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

import java.util.Calendar;

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.Faction;

/**
 * Helper functions for determining part availibility and tech base
 * and the associated modifiers. A lot of this code is borrowed from
 * the deprecated SSWLibHelper.java
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Availability {
	
	public static int getAvailabilityModifier(int availability) {
        int modifier = 999;

        switch (availability) {
            case (EquipmentType.RATING_A) :
                modifier = -4;
                break;
            case (EquipmentType.RATING_B) :
                modifier = -3;
                break;
            case (EquipmentType.RATING_C) :
                modifier = -2;
                break;
            case (EquipmentType.RATING_D) :
                modifier = -1;
                break;
            case (EquipmentType.RATING_E) :
                modifier = 0;
                break;
            case (EquipmentType.RATING_F) :
                modifier = 2;
                break;
            case (EquipmentType.RATING_X) :
                modifier = 5;
                break;
        }
        
        return modifier;
    }
    
	public static int getTechModifier(int tech) {
        int modifier = 999;

        switch (tech) {
            case (EquipmentType.RATING_A) :
                modifier = -4;
                break;
            case (EquipmentType.RATING_B) :
                modifier = -2;
                break;
            case (EquipmentType.RATING_C) :
                modifier = 0;
                break;
            case (EquipmentType.RATING_D) :
                modifier = 1;
                break;
            case (EquipmentType.RATING_E) :
                modifier = 2;
                break;
            case (EquipmentType.RATING_F) :
                modifier = 3;
                break;
        }
        
        return modifier;
    }
	
}
