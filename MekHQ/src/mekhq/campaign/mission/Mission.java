/*
 * Mission.java
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


/**
 * Missions are primarily holder objects for a set of scenarios.
 * 
 * The really cool stuff will happen when we subclass this into Contract
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Mission implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5692134027829715149L;
	
	public static final int S_ACTIVE = 0;
	public static final int S_SUCESS = 1;
	public static final int S_FAILED = 2;
	public static final int S_BREACH = 3;
	
	private String name;
	private int status;
	private String desc;
	private ArrayList<Scenario> scenarios;
	private int id = -1;
	
	public Mission(String n) {
		this.name = n;
		this.desc = "";
		this.status = S_ACTIVE;
		scenarios = new ArrayList<Scenario>();
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
	
	public ArrayList<Scenario> getScenarios() {
		return scenarios;
	}
	
	public void addScenario(Scenario s) {
		scenarios.add(s);
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	public boolean isActive() {
		return status ==S_ACTIVE;
	}
	
}