/*
 * SupportTeam.java
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

package mekhq.campaign.team;

import mekhq.campaign.*;
import java.io.Serializable;
import java.util.ArrayList;
import megamek.common.TargetRoll;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author Taharqa
 * This is the code for a team (medical, technical, etc.)
 */
public abstract class SupportTeam implements Serializable {

    public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;
    public static final int EXP_ELITE = 3;
    public static final int EXP_NUM = 4;
   
    protected String name;
    protected int rating; 
    protected int id;
    protected int fullSize;
    protected int currentSize;
    protected int hours;
    protected int minutesLeft;
    
    protected ArrayList<WorkItem> assignedTasks;
    
    protected Campaign campaign;
    
    //private Vector<WorkItem> tasksAssigned;
    
    public static String getRatingName(int rating) {
        switch(rating) {
           case SupportTeam.EXP_GREEN:
               return "Green";
           case SupportTeam.EXP_REGULAR:
               return "Regular";
           case SupportTeam.EXP_VETERAN:
               return "Veteran";
           case SupportTeam.EXP_ELITE:
               return "Elite";
       }
       return "Unknown";
    }
    
    public SupportTeam(Campaign c, String name, int rating) {
        this.campaign = c;
        this.name = name;
        this.rating = rating;
        this.hours = 8;
        this.assignedTasks = new ArrayList<WorkItem>();
        resetMinutesLeft();
    }
    
    public int getRating() {
        return rating;
    }
    
    public String getName() {
        return name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int i) {
        this.id = i;
    } 
    
    public int getFullStrength() {
        return fullSize;
    }
    
    public int getCurrentStrength() {
        return currentSize;
    }
    
    public int getHours() {
        return hours;
    }
    
    public void setHours(int i) {
        this.hours = i;
    }
    
    public int getMinutesLeft() {
        return minutesLeft;
    }
    
    public void setMinutesLeft(int m) {
        this.minutesLeft = m;
    }
    
    public void resetMinutesLeft() {
        this.minutesLeft = 60 * getHours();
    }
    
    public int getCasualties() {
        return getFullStrength() - getCurrentStrength();
    }
    
    public TargetRoll getTarget() {    
        TargetRoll target = new TargetRoll(getSkillBase(), getRatingName());
        return target;
    }

   public abstract int getSkillBase();   
   
   public String getRatingName() {
       return getRatingName(rating);
   }
   
   public String getDesc() {
       return getName() + " (" + getRatingName() + " " + getTypeDesc() + ") " + getTasksDesc();
   }
   
   public abstract String getTasksDesc();
   
   public abstract String getTypeDesc();
    
   public ArrayList<WorkItem> getTasksAssigned() {
        return assignedTasks;
   }
   
   public void addTask(WorkItem task) {
       assignedTasks.add(task);
   }
   
   public void removeTask(WorkItem task) {
       assignedTasks.remove(task);
   }
   
   public abstract boolean canDo(WorkItem task);
   
   public abstract int makeRoll(WorkItem task);
   
   public String doAssigned(WorkItem task) {
       String report = "  " + task.getDisplayName();
       //check whether the task is currently possible
       if(null != task.checkFixable()) {
           report = report + ", but the task is impossible because " + task.checkFixable();
           return report;
       }        
       int minutes = task.getTime();
       if(minutes > getMinutesLeft()) {
           report = report  + ", but ran out of time for the day, see you tommorrow!";
           return report;
       } else {
           setMinutesLeft(getMinutesLeft() - minutes);
       }
       TargetRoll target = getTarget();
       target.append(task.getAllMods()); 
       int roll = makeRoll(task);
       report = report + ", needs " + target.getValueAsString() + " and rolls " + roll + ":";
       if(roll >= target.getValue()) {
           report = report + " task completed.";
           task.fix();
           task.complete();
       } else {
           report = report + " task failed.";
           task.fail(getRating());
           //have we run out of options?
           if(task.getSkillMin() > EXP_ELITE) {
               if(task instanceof RepairItem) {
                   //turn it into a replacement item
                   campaign.mutateTask(task, ((RepairItem)task).replace());
                   report = report + " Item cannot be repaired, it must be replaced instead.";
               } else if(task instanceof ReplacementItem) {
                   //TODO: destroy component, but parts no implemented yet
                   report = report + " Component destroyed!";
                   //reset the skill min counter back to green
                   task.setSkillMin(EXP_GREEN);
               }
           }
       }
       return report;
    }
   
   /** 
    * I think my logic here is flawed. Assignments should be processed
    * immediately not as part of a queue. Otherwise, it becomes too difficult
    * to deal with failures and dependencies.
   public ArrayList<String> doAssignments() {
        
       ArrayList<String> reports = new ArrayList<String>();
      
       //need to clone the assigned tasks array list to avoid concommitant errors
       ArrayList<WorkItem> schedule = new ArrayList<WorkItem>();
       for(WorkItem task : getTasksAssigned()) {
           schedule.add(task);
       }
       
       reports.add(getName() + " Assignments:");
       //do all of the assignments in the assignment vector until 
       //you run out of time
       int minutesWorked = 0;
       for(WorkItem task : schedule) {
           String report = "  " + task.getDisplayName();
           //check whether the task is currently possible
           if(null != task.checkFixable()) {
               report = report + ", but the task is impossible because " + task.checkFixable();
               reports.add(report);
               task.unassignTeam();
               continue;
           }        
           minutesWorked += task.getTime();
           if(minutesWorked > (getHours() * 60)) {
               report = report  + ", but ran out of time for the day, see you tommorrow!";
               reports.add(report);
               break;
           }                 
           TargetRoll target = getTarget();
           target.append(task.getAllMods()); 
           int roll = makeRoll(task);
           report = report + ", needs " + target.getValueAsString() + " and rolls " + roll + ":";
           if(roll >= target.getValue()) {
               report = report + " task completed.";
               task.fix();
               task.complete();
           } else {
               report = report + " task failed.";
               task.fail();
               //have we run out of options?
               if(task.getSkillMin() > EXP_ELITE) {
                   if(task instanceof RepairItem) {
                       //turn it into a replacement item
                       campaign.mutateTask(task, ((RepairItem)task).replace());
                       report = report + " Item cannot be repaired, it must be replaced instead.";
                   } else if(task instanceof ReplacementItem) {
                       //TODO: destroy component, but parts no implemented yet
                       report = report + " Component destroyed!";
                       //reset the skill min counter back to green
                       task.setSkillMin(EXP_GREEN);
                   }
               }
           }
           reports.add(report);
       }
       return reports;
   }
    * */
}
