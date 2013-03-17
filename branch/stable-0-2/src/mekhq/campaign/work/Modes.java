package mekhq.campaign.work;

/*
 * Modes.java
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

public class Modes {
	
	public static final int MODE_NORMAL = 0;
	public static final int MODE_EXTRA_DOUBLE = 1;
	public static final int MODE_EXTRA_TRIPLE = 2;
	public static final int MODE_EXTRA_QUAD = 3;
	public static final int MODE_RUSH_ONE = 4;
	public static final int MODE_RUSH_TWO = 5;
	public static final int MODE_RUSH_THREE = 6;
	public static final int MODE_N = 7;
	
	public static int getModeMod(int mode) {
		switch (mode) {
		case MODE_EXTRA_DOUBLE:
			return -1;
		case MODE_EXTRA_TRIPLE:
			return -2;
		case MODE_EXTRA_QUAD:
			return -3;
		default:
			return 0;
		}
	}
	
	public static int getModeExperienceReduction(int mode) {
		switch (mode) {
		case MODE_RUSH_ONE:
			return 1;
		case MODE_RUSH_TWO:
			return 2;
		case MODE_RUSH_THREE:
			return 3;
		default:
			return 0;
		}
	}

	public static String getModeName(int mode) {
		switch (mode) {
		case MODE_EXTRA_DOUBLE:
			return "Extra time (x2)";
		case MODE_EXTRA_TRIPLE:
			return "Extra time (x3)";
		case MODE_EXTRA_QUAD:
			return "Extra time (x4)";
		case MODE_RUSH_ONE:
			return "Rush Job (1/2)";
		case MODE_RUSH_TWO:
			return "Rush Job (1/4)";
		case MODE_RUSH_THREE:
			return "Rush Job (1/8)";
		default:
			return "Normal";
		}
	}
}