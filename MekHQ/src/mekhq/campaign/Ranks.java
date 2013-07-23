/*
 * Ranks.java
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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This object will keep track of rank information. It will keep information
 * on a set of pre-fab rank structures and will hold info on the one chosen by the user.
 * It will also allow for the input of a user-created rank structure from a comma-delimited
 * set of names
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Ranks implements Serializable {
	
	private static final long serialVersionUID = -2054016720766034618L;

	//pre-fab rank systems
	public static final int RS_CUSTOM = -1;
	public static final int RS_SL =  0;
	public static final int RS_FS =  1;
	public static final int RS_LA =  2;
	public static final int RS_FWL = 3;
	public static final int RS_CC =  4;
	public static final int RS_DC =  5;
	public static final int RS_CL =  6;
	public static final int RS_NUM = 7;
	private static final String[][] rankSystems = {
		{"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant-Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies"},
		{"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
		{"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun"},
		{"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei"},
		{"None","Point","Point Commander","Star Commander","Star Captain","Star Colonel","Galaxy Commander","Khan","ilKhan"}
	};
	private static final int[] officerCut = {7,8,11,9,5,9,3};
	public static final int RANK_BONDSMAN = -1;
	public static final int RANK_PRISONER = -2;
	
	private int rankSystem;
	private ArrayList<String> ranks;
	//an int indicating where the officer ranks start
	private int officer;

	public static String[] getRankSystem(int system) {
		return rankSystems[system];
	}
	
	public static String getRankSystemName(int system) {
		switch(system) {
		case RS_CUSTOM:
			return "Custom";
		case RS_SL:
			return "Star League";
		case RS_FS:
			return "Federated Suns";
		case RS_LA:
			return "Lyran Alliance";
		case RS_FWL:
			return "Free Worlds League";
		case RS_CC:
			return "Capellan Confederation";
		case RS_DC:
			return "Draconis Combine";
		case RS_CL:
			return "Clan";
		default:
			return "?";
		}
	}
	
	public Ranks() {
		this(RS_SL);
	}
	
	public Ranks(int system) {
		rankSystem = system;
		useRankSystem(rankSystem);
		officer = officerCut[rankSystem];
	}
	
	public ArrayList<String> getAllRanks() {
		return ranks;
	}
	
	public void useRankSystem(int system) {
		ranks = new ArrayList<String>();
		if(system >= rankSystems.length) {
			system = RS_SL;
		}
		for (int i = 0; i < rankSystems[system].length; i++) {
			ranks.add(rankSystems[system][i]);
		}
		if(system < officerCut.length) {
			officer = officerCut[system];
		}
	}
	
	public void setCustomRanks(ArrayList<String> customRanks, int offCut) {
		ranks = customRanks;
		rankSystem = RS_CUSTOM;
		officer = offCut;
	}
	
	public String getRank(int r) {
		if(r >= ranks.size()) {
			return "Unknown";
		}
		if (r == RANK_BONDSMAN) { // Bondsman
			return "Bondsman";
		}
		if (r == RANK_PRISONER) { // Prisoners
			return "Prisoner";
		}
		return ranks.get(r);
	}
	
	public int getRankOrder(String rank) {
		return ranks.indexOf(rank);
	}
	
	public int getRankSystem() {
		return rankSystem;
	}
	
	public void setRankSystem(int system) {
		if(system < RS_NUM) {
			rankSystem = system;
			useRankSystem(rankSystem);
		}	
	}
	
	public boolean isOfficer(int rank) {
		return rank >= officer;
	}
	
	public int getOfficerCut() {
		return officer;
	}
	
	public void setOfficerCut(int i) {
		officer = i;
	}
	
	public String getRankNameList() {
		String rankNames = "";
		int i = 0;
		for(String name : getAllRanks()) {
			rankNames += name;
			i++;
			if(i < getAllRanks().size()) {
				rankNames += ",";
			}
		}
		return rankNames;
	}
	
	public void setRanksFromList(String names) {
		ranks = new ArrayList<String>();
		String[] rankNames = names.split(",");
		for(String rankName : rankNames) {
			ranks.add(rankName);
		}
		rankSystem = RS_CUSTOM;
	}
	
	
}