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
     * This migrates awards from the Default Set of pre-0.47.6 to the newer standard
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
}
