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

import mekhq.campaign.Campaign;
import mekhq.campaign.Force;
import mekhq.campaign.personnel.Person;


/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Scenario implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2193761569359938090L;
	
	public static final int S_CURRENT  = 0;
	public static final int S_VICTORY  = 1;
	public static final int S_MVICTORY = 2;
	public static final int S_DEFEAT   = 3;
	public static final int S_MDEFEAT  = 4;
	public static final int S_DRAW     = 5;
	public static final int S_NUM      = 6;
	
	private String name;
	private String desc;
	private int status;
	private Date date;
	private ArrayList<Integer> subForceIds;
	private ArrayList<Integer> personnelIds;
	private int id = -1;
	
	public Scenario(String n) {
		this.name = n;
		this.desc = "";
		this.status = S_CURRENT;
		this.date = null;
		this.subForceIds = new ArrayList<Integer>();
		this.personnelIds = new ArrayList<Integer>();
	}
	
	public static String getStatusName(int s) {
		switch(s) {
		case S_CURRENT:
			return "Pending";
		case S_VICTORY:
			return "Victory";
		case S_MVICTORY:
			return "Marginal Victory";
		case S_DEFEAT:
			return "Defeat";
		case S_MDEFEAT:
			return "Marginal Defeat";
		case S_DRAW:
			return "Draw";
		default:
			return "?";
		}
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
	
	public String getStatusName() {
		return getStatusName(getStatus());
	}
	
	public void setDate(Date d) {
		this.date = d;
	}
	
	public Date getDate() {
		return date;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	public Force getForces(Campaign campaign) {
		Force force = new Force("Assigned Forces");
		for(int subid : subForceIds) {
			Force sub = campaign.getForce(subid);
			if(null != sub) {
				force.addSubForce(sub, false);
			}
		}
		for(int pid : personnelIds) {
			force.addPerson(pid);
		}
		return force;
	}
	
	public void addForces(int fid) {
		subForceIds.add(fid);
	}
	
	public void addPersonnel(int pid) {
		personnelIds.add(pid);
	}
	
	public void removePersonnel(int pid) {
		int idx = -1;
		for(int i = 0; i < personnelIds.size(); i++) {
			if(pid == personnelIds.get(i)) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			personnelIds.remove(idx);
		}
	}
	
	public void removeForce(int fid) {
		int idx = -1;
		for(int i = 0; i < subForceIds.size(); i++) {
			if(fid == subForceIds.get(i)) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			subForceIds.remove(idx);
		}
	}
	
	public boolean isCurrent() {
		return status == S_CURRENT;
	}
	
	public void clearAllForcesAndPersonnel(Campaign campaign) {
		for(int fid : subForceIds) {
			Force f = campaign.getForce(fid);
			if(null != f) {
				f.setScenarioId(-1);
			}	
		}
		for(int pid : personnelIds) {
			Person p = campaign.getPerson(pid);
			if(null != p) {
				p.setScenarioId(-1);
			}	
		}
		subForceIds = new ArrayList<Integer>();
		personnelIds = new ArrayList<Integer>();
	}
	
}