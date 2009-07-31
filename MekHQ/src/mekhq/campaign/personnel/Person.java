/*
 * Person.java
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

package mekhq.campaign.personnel;

import java.io.Serializable;
import mekhq.campaign.Campaign;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.PersonnelWorkItem;

/**
 * This is an abstract class for verious types of personnel
 * The personnel types themselves will be various wrappers for 
 * 1) pilots (including tank crews)
 * 2) large aero crews (because they can double as teams)
 * 3) support teams
 * 4) infantry squads/platoons (including BA)
 * 5) Administrators/other staff?
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class Person implements Serializable {
    
    protected int id;
    //any existing work item for this person
    protected PersonnelWorkItem task;
    //days of rest
    protected int daysRest;
    protected boolean deployed;
    
    //default constructor
    public Person() {
        daysRest = 0;
    }
    
    public abstract String getDesc();
    
    public abstract String getDescHTML();

    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setTask(PersonnelWorkItem task) {
        this.task = task;
    }
    
    public PersonnelWorkItem getTask() {
        return task;
    }
    
    public SupportTeam getTeamAssigned() {
        if(null == task) {
            return null;
        }
        return task.getTeam();
    }
    
    public String getAssignedDoctorString() {
        if(null == getTeamAssigned()) {
            return "";
        }
        return " (assigned to " + getTeamAssigned().getName() + ")";
    }
    
    public abstract void runDiagnostic(Campaign campaign);
    
    public abstract void heal();
    
    public abstract boolean needsHealing();
    
    public boolean checkNaturalHealing() {
        if(needsHealing() && null == task) {
            daysRest++;
            if(daysRest >= 15) {
                heal();
                daysRest = 0;
                return true;
            }
        }
        return false;
    }
    
    public boolean isDeployed() {
        return deployed;
    }
    
    public void setDeployed(boolean b) {
        this.deployed = b;
        if(deployed) {
            task.setTeam(null);
        }
    }
}
