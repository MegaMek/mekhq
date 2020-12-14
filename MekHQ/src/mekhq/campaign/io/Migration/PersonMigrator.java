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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.Ranks;

public class PersonMigrator {

    //region Rank Migration
    public static final String[] oldRankNames = {
            "Star League", "Federated Sun", "Lyran Alliance", "Free Worlds League",
            "Capellan Confederation", "Draconis Combine", "Clan", "Custom"
    };

    public static final String[][] oldRankSystems = {
            {"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant JG","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
            {"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies"},
            {"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
            {"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
            {"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun"},
            {"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei"},
            {"None","Point","Point Commander","Star Commander","Star Captain","Star Colonel","Galaxy Commander","Khan","ilKhan"}
    };

    public static int getNewRank(Campaign campaign, int oldSystem, int oldRank) throws ArrayIndexOutOfBoundsException {
        Ranks ranks = Ranks.getRanksFromSystem(translateRankSystem(oldSystem, campaign.getFactionCode()));
        String rankName;

        // Try and acquire the rank name...
        rankName = oldRankSystems[oldSystem][oldRank];

        for (int rankNum = Ranks.RE_MIN; rankNum < Ranks.RC_NUM; rankNum++) {
            if (ranks.getRank(rankNum).getName(Ranks.RPROF_MW).equals(rankName)) {
                return rankNum;
            }
        }

        // If we didn't find anything to translate to, then we can kick them as Rank "None"
        return 0;
    }

    public static int translateRankSystem(int old, String faction) {
        switch (old) {
            case 1:
                return 1;
            case 2:
                return 4;
            case 3:
                return 5;
            case 4:
                return 6;
            case 5:
                return 8;
            case 6:
                return 9;
            case 7:
                switch (faction) {
                    case "WOB":
                        return 11;
                    case "FC":
                        return 2;
                    case "CS":
                        return 10;
                    case "CDS":
                    case "CGB":
                    case "CHH":
                    case "CJF":
                    case "CNC":
                    case "CSJ":
                    case "CSV":
                    case "CW":
                        return 9;
                    case "OA":
                        return 16;
                    case "MH":
                        return 15;
                    case "TC":
                        return 14;
                    case "MOC":
                        return 13;
                    case "FRR":
                        return 17;
                }
                return 12;
            case 0:
            default:
                return 0;
        }
    }

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
    //endregion Rank Migration

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
