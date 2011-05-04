/*
 * IFaction.java
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

import java.awt.Color;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Faction {

	//5/4/2011 - I am changing factions so that they line 
	//up with all the possible factions identifiable on the 
	//Interstellar Map
    public static final int F_MERC      = 0;
    public static final int F_CAPCON    = 1;
    public static final int F_DRAC      = 2;
    public static final int F_FEDSUN    = 3;
    public static final int F_FWL       = 4;
    public static final int F_LYRAN     = 5;
    public static final int F_FEDCOM    = 6;
    public static final int F_ROS       = 7;
    public static final int F_COMSTAR   = 8;
    public static final int F_WOB       = 9;
    public static final int F_FRR       = 10;
    public static final int F_SIC       = 11;
    public static final int F_CANOPUS   = 12;
    public static final int F_OA        = 13;
    public static final int F_TC        = 14;
    public static final int F_MH        = 15;
    public static final int F_CHAOS     = 16;
    public static final int F_ARDC      = 17;
    public static final int F_TERRAN    = 18;
    public static final int F_RWR       = 19;
    public static final int F_PERIPHERY = 20;
    public static final int F_C_WOLF    = 21;
    public static final int F_C_JF      = 22;
    public static final int F_C_GB      = 23;
    public static final int F_C_SJ      = 24;
    public static final int F_C_NC      = 25;
    public static final int F_C_DS      = 26;
    public static final int F_C_SV      = 27;
    public static final int F_C_HH      = 28;
    public static final int F_C_OTHER   = 29;
    public static final int F_NUM       = 30;
    
    public static String getFactionName(int faction) {
        switch(faction) {
            case F_MERC:
                return "Mercenary";
            case F_CAPCON:
                return "Cappellan Confederation";
            case F_DRAC:
                return "Draconis Combine";
            case F_FEDSUN:
                return "Federated Suns";
            case F_FWL:
                return "Free Worlds League";
            case F_LYRAN:
                return "Lyran Commonwealth/Alliance";
            case F_FEDCOM:
                return "Federated Commonwealth"; 
            case F_ROS:
                    return "Republic of the Sphere";
            case F_COMSTAR:
                return "Comstar";
            case F_WOB:
                return "Word of Blake";
            case F_FRR:
                return "Free Rassalhague Republic";
            case F_SIC:
                return "St. Ives Compact";
            case F_CANOPUS:
                return "Magistracy of Canopus";
            case F_OA:
                return "Outworlds Alliance";
            case F_TC:
                return "Taurian Concordat";
            case F_MH:
                return "Marian Hegemony";
            case F_PERIPHERY:
                return "Periphery (Other)";
            case F_ARDC:
                return "Arc-Royal Defense Cordon";          
            case F_CHAOS:
                return "Chaos March";
            case F_TERRAN:
                return "Terran Hegemony";
            case F_RWR:
                return "Rim Worlds Republic";
            case F_C_WOLF:
                return "Clan Wolf";
            case F_C_JF:
                return "Clan Jade Falcon";
            case F_C_GB:
            	return "Clan Ghost Bear";
            case F_C_SJ:
            	return "Clan Smoke Jaguar";
            case F_C_NC:
                return "Clan Nova Cat";
            case F_C_DS:
                return "Clan Diamond Shark";
            case F_C_SV:
                return "Clan Steel Viper";
            case F_C_HH:
                return "Clan Hell's Horses";
            case F_C_OTHER:
                return "Clan (Other)";
            default:
                return "Unknown";
        }
    }

    public static boolean isPeripheryFaction (int faction) {
        return (faction==F_CANOPUS 
        		|| faction==F_OA 
        		|| faction==F_PERIPHERY 
        		|| faction==F_RWR 
        		|| faction==F_TC
        		|| faction==F_MH);
    }
    public static boolean isClanFaction (int faction) {
       switch(faction) {
       case F_C_WOLF:
       case F_C_JF:
       case F_C_GB:
       case F_C_SJ:
       case F_C_NC:
       case F_C_DS:
       case F_C_SV:
       case F_C_HH:
       case F_C_OTHER:
    	   return true;
       default:
    	   return false;
       }
    }
    
    public static String getFactionCodeForNameGenerator(int faction) {
        switch(faction) {
        
        case F_CAPCON:
            return "CC";
        case F_DRAC:
            return "DC";
        case F_FEDSUN:
            return "FS";
        case F_FWL:
            return "FWL";
        case F_LYRAN:
            return "LA";
        case F_C_WOLF:
        case F_C_JF:
        case F_C_GB:
        case F_C_SJ:
        case F_C_NC:
        case F_C_DS:
        case F_C_SV:
        case F_C_HH:
        case F_C_OTHER:
            return "Clan";
        case F_FRR:
            return "FRR";    
        case F_MERC:
        case F_COMSTAR:
        case F_WOB:
        case F_CANOPUS:
        case F_OA:
        case F_TC:
        case F_PERIPHERY:
        case F_CHAOS:
        case F_TERRAN:
        case F_RWR:
        default:
            return "General";
        }
    }
    
    public static Color getFactionColor(int faction) {
        switch(faction) {
        
        case F_CAPCON:
            return Color.GREEN;
        case F_DRAC:
            return Color.RED;
        case F_FEDSUN:
        case F_FEDCOM:
            return Color.YELLOW;
        case F_FWL:
            return Color.PINK;
        case F_LYRAN:
            return Color.BLUE;
        case F_C_WOLF:
        case F_C_JF:
        case F_C_GB:
        case F_C_SJ:
        case F_C_NC:
        case F_C_DS:
        case F_C_SV:
        case F_C_HH:
        case F_C_OTHER:
            return Color.CYAN;
        case F_FRR:    
        case F_MERC:
        case F_COMSTAR:
        case F_WOB:
        case F_CANOPUS:
        case F_OA:
        case F_TC:
        case F_PERIPHERY:
        case F_CHAOS:
        case F_TERRAN:
        case F_RWR:
        default:
            return Color.WHITE;
        }
    }

    
}
