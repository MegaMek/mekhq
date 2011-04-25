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
	public static final int RS_SL = 0;
	private static final String[][] rankSystems = {
		{"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"}
	};
	
	private ArrayList<String> ranks;

	public Ranks() {
		this(RS_SL);
	}
	
	public Ranks(int system) {
		useRankSystem(system);
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
	}
	
	public String getRank(int r) {
		if(r >= ranks.size()) {
			return "Unknown";
		}
		return ranks.get(r);
	}
	
	public int getRankOrder(String rank) {
		return ranks.indexOf(rank);
	}
	
	
}