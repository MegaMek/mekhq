/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.io.Migration;

public class PersonMigrator {
    /**
     * This migrates awards from the Default Set of pre-0.47.6 to the newer standard following 0.47.14
     * @param text the previous award name
     * @return the new award name
     */
    public static String awardDefaultSetMigrator(String text) {
        switch (text) {
            case "Armed Forces":
                return "Combat Cross";
            case "Combat Commendation":
                return "Combat Unit Commendation";
            case "Combat Unit Commendation":
                return "Meritorious Service";
            case "Distinguished Service":
                return "House Superior Service";
            case "Fedcom Civil War Campaign":
                return "FedCom Civil War Campaign";
            case "House Meritorious Service":
                return "Legion of Merit";
            case "House Superior Service":
                return "House Unit Citation";
            case "Meritourious Service":
                return "Combat Commendation";
            case "Meritourious Unit Commendation":
                return "Meritorious Unit Commendation";
            case "Bronze Star":
            case "Clan Invasion Campaign":
            case "Combat Achievement":
            case "Combat Action":
            case "Combat Cross":
            case "Expeditionary":
            case "Fourth Succession War Campaign":
            case "Galactic Service":
            case "Galactic Service Deployment":
            case "Galactic War on Pirating":
            case "Good Conduct":
            case "House Defense":
            case "House Distinguished Service":
            case "House Unit Citation":
            case "Humanitarian Service":
            case "Legion of Merit":
            case "Periphery Expeditionary":
            case "Prisoner of War":
            case "Purple Heart":
            case "Silver Star":
            case "Task Force Serpent Campaign":
            case "Third Succession War Campaign":
            case "War of 3039 Campaign":
                return text;
            default:
                return null;
        }
    }

    /**
     * This migrates the rank system name to the data file as per changes made in 0.47.15
     * @param system the system id of the loaded rank system
     * @return the name of the system
     */
    public static String migrateRankSystemName(int system) {
        switch (system) {
            case 0:
                return "Second Star League";
            case 1:
                return "Federated Suns";
            case 2:
                return "Federated Commonwealth";
            case 3:
                return "Lyran Commonwealth";
            case 4:
                return "Lyran Alliance";
            case 5:
                return "Free Worlds League";
            case 6:
                return "Capellan Confederation";
            case 7:
                return "Capellan Confederation Warrior House";
            case 8:
                return "Draconis Combine";
            case 9:
                return "Clan";
            case 10:
                return "ComStar";
            case 11:
                return "Word of Blake";
            case 12:
                return "Custom";
            case 13:
                return "Magistracy of Canopus";
            case 14:
                return "Taurian Concordat";
            case 15:
                return "Marian Hegemony";
            case 16:
                return "Outworlds Alliance";
            case 17:
                return "Free Rasalhague Republic";
            case 18:
                return "Aurigan Coalition";
            case 19:
                return "First Star League";
            default:
                return "?";
        }
    }
}
