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

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Faction {

    public static final int F_MERC      = 0;
    public static final int F_CAPCON    = 1;
    public static final int F_DRAC      = 2;
    public static final int F_FEDSUN    = 3;
    public static final int F_FWL       = 4;
    public static final int F_LYRAN     = 5;
    public static final int F_COMSTAR   = 6;
    public static final int F_WOB       = 7;
    public static final int F_CLAN      = 8;
    public static final int F_FRR       = 9;
    public static final int F_CANOPUS   = 10;
    public static final int F_OA        = 11;
    public static final int F_TC        = 12;
    public static final int F_PERIPHERY = 13;
    public static final int F_CHAOS     = 14;
    public static final int F_TERRAN    = 15;
    public static final int F_RWR       = 16;
    public static final int F_NUM       = 17;
    
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
            case F_COMSTAR:
                return "Comstar";
            case F_WOB:
                return "Word of Blake";
            case F_CLAN:
                return "Clan";
            case F_FRR:
                return "Free Rassalhague Republic";
            case F_CANOPUS:
                return "Magistracy of Canopus";
            case F_OA:
                return "Outworlds Alliance";
            case F_TC:
                return "Taurian Concordat";
            case F_PERIPHERY:
                return "Periphery (Other)";
            case F_CHAOS:
                return "Chaos March";
            case F_TERRAN:
                return "Terran Hegemony";
            case F_RWR:
                return "Rim Worlds Republic";
            default:
                return "Unknown";
        }
    }

    public static boolean isPeripheryFaction (int faction) {
        return (faction==F_CANOPUS || faction==F_OA || faction==F_PERIPHERY || faction==F_RWR || faction==F_TC);
    }
    public static boolean isClanFaction (int faction) {
        return (faction==F_CLAN);
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
        case F_CLAN:
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
    
}
