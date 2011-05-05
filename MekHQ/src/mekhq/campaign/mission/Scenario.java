/*
 * Scenario.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.mission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;


/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Scenario implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2193761569359938090L;
	
	public static final int S_CURRENT = 0;
	public static final int S_VICTORY = 1;
	public static final int S_DEFEAT  = 2;
	public static final int S_DRAW    = 3;
	
	private String name;
	private String desc;
	private int status;
	private Date date;
	private ArrayList<Integer> units;
	
	public Scenario(String n) {
		this.name = n;
		this.desc = "";
		this.status = S_CURRENT;
		this.date = null;
		this.units = new ArrayList<Integer>();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String n) {
		this.name = n;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public void setDesc(String d) {
		this.desc = d;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int s) {
		this.status = s;
	}
	
	public void setDate(Date d) {
		this.date = d;
	}
	
	public Date getDate() {
		return date;
	}
	
	public ArrayList<Integer> getUnitIds() {
		return units;
	}
	
	public void addUnits(int uid) {
		units.add(uid);
	}
	
}