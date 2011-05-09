/*
 * IWork.java
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
package mekhq.campaign.work;

import megamek.common.TargetRoll;
import mekhq.campaign.team.SupportTeam;


/**
 * 
 * @author Jay
 */
public abstract interface IWork {
	
	public static final int MODE_NORMAL = 0;
	public static final int MODE_EXTRA_ONE = 1;
	public static final int MODE_EXTRA_TWO = 2;
	public static final int MODE_RUSH_ONE = 3;
	public static final int MODE_RUSH_TWO = 4;
	public static final int MODE_RUSH_THREE = 5;
	public static final int MODE_N = 6;
	
	public abstract boolean needsFixing();
	
	public abstract int getDifficulty();
	
	public abstract TargetRoll getAllMods();
	
	public abstract String succeed();
	
	public abstract String fail(int rating);
	
	public abstract int getTeamId();
	
	public abstract boolean canFix(SupportTeam team);
	
	public abstract int getMode();
	
}