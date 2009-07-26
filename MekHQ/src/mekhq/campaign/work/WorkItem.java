/*
 * WorkItem.java
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

import java.io.Serializable;
import megamek.common.TargetRoll;
import mekhq.campaign.team.SupportTeam;

/**
 * An abstract class representing some item that needs work
 * will be extended for repairs, replacement, reloading, etc.
 * @author Taharqa
 */
public abstract class WorkItem implements Serializable {

    protected String name;
    //the id of this work item
    protected int id;
    //the skill modifier for difficulty
    protected int difficulty;
    //the amount of time for the repair
    protected int time;
    //the minimum skill level in order to attempt
    protected int skillMin;
    //has this task been successfully completed?
    protected boolean completed;
    
    public WorkItem() {
        this.name = "Unknown";
        this.skillMin = SupportTeam.EXP_GREEN;
        this.completed = false;
    }
    
    public String getName() {
        return name;
    }
 
    public void setName(String s) {
        this.name = s;
    }
    
    public abstract String getDisplayName();
    
    public int getId() {
        return id;
    }
    
    public void setId(int i) {
        this.id = i;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public int getTime() {
        return time;
    }
    
    public void setTime(int i) {
        this.time = i;
    }
    
    public int getSkillMin() {
        return skillMin;
    }
    
    public void setSkillMin(int i) {
        this.skillMin = i;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void complete() {
        this.completed = true;
    }
    
    public String getDesc() {
        return  getName() + " " + getStats();  
    }
    
    public String getStats() {
        return "[" + getTime() + "m/" +   SupportTeam.getRatingName(getSkillMin()) + "/" + getAllMods().getValueAsString() + "]";
    }
    
    
    public TargetRoll getAllMods() {
        TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
        
        
        return mods;
    }
    
    public boolean isNeeded() {
        return true;
    }
    
    /**
     * check whether this work item is currently fixable
     * some conditions will make a work item impossible to fix
     * @return a <code>String</code> indicating the reason for non-fixability, null if fixable
     */
    public String checkFixable() {
        return null;
    }
    
    /**
     * Resolve this work item (i.e. repair it, replace it, reload it, etc)
     */
    public abstract void fix();
    
    /**
     * fail this work item
     * @param rating - an <code>int</code> of the skill rating of the currently assigned team
     */
    public void fail(int rating) {
        //increment the minimum skill level required
        setSkillMin(rating + 1);
    }
    
}
