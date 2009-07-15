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

package mekhq.campaign;

import java.io.Serializable;
import java.util.ArrayList;
import megamek.common.Compute;
import megamek.common.TargetRoll;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author Taharqa
 * This is the code for a team (medical, technical, etc.)
 */
public class SupportTeam implements Serializable {

    public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;
    public static final int EXP_ELITE = 3;
    public static final int EXP_NUM = 4;
    
    public static final int T_MECH = 0;
    public static final int T_MECHANIC = 1;
    public static final int T_AERO = 2;
    public static final int T_BA = 3;
    public static final int T_DOCTOR = 4;
    public static final int T_NUM = 5;
    
    private int type;
    
    protected String name;
    protected int rating; 
    protected int id;
    protected int fullSize;
    protected int currentSize;
    protected int hours;
    
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
    
    public static String getTypeDesc(int type) {
        switch(type) {
            case T_MECH:
                return "Mech Tech";
            case T_MECHANIC:
                return "Mechanic";
            case T_AERO:
                return "Aero Tech";
            case T_BA:
                return "BA Tech";
            case T_DOCTOR:
                return "Doctor";
        }
        return "?? Technician";
    }
    
    public SupportTeam(Campaign c, String name, int rating, int type) {
        this.campaign = c;
        this.name = name;
        this.rating = rating;
        this.type = type;
        if(isDoctor()) {
            this.fullSize = 5;
            this.currentSize = 5;
        } else {
            this.fullSize = 7;
            this.currentSize = 7;
        }
        this.hours = 8;
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
    
    public int getCasualties() {
        return getFullStrength() - getCurrentStrength();
    }
    
    public TargetRoll getTarget() {    
        TargetRoll target = new TargetRoll(getSkillBase(), getRatingName());
        return target;
    }
    
    public boolean isDoctor() {
        return type == T_DOCTOR;
    }

   public int getSkillBase() {
        int base = 11;
         switch(rating) {
           case SupportTeam.EXP_GREEN:
               base = 9;
               break;
           case SupportTeam.EXP_REGULAR:
               base = 7;
               break;
           case SupportTeam.EXP_VETERAN:
               base = 6;
               break;
           case SupportTeam.EXP_ELITE:
               base = 5;
               break;
       }
       if(isDoctor()) {
           base++;
       }
       return base;
    }
   
   public String getTypeDesc() {
        return getTypeDesc(type);
    }
   
   public String getRatingName() {
       return getRatingName(rating);
   }
   
   public String getDesc() {
       return getName() + " (" + getRatingName() + " " + getTypeDesc() + ") " + getTasksDesc();
   }
   
   public String getTasksDesc() {
       int total = 0;
       int minutes = 0;
       for(WorkItem task : getTasksAssigned()) {
           total++;
           minutes += task.getTime();
       }
       return "" + total + " task(s), " + minutes + "/" + getHours()*60 + " minutes";
   }
   
   private ArrayList<WorkItem> getTasksAssigned() {
        return campaign.getTasksForTeam(getId());
   }
   
   public boolean canDo(WorkItem task) {
       return task.getSkillMin() <= getRating();
   } 
   
   public ArrayList<String> doAssignments() {
       ArrayList<String> reports = new ArrayList<String>();
       
       reports.add(getName() + " Assignments:");
       //do all of the assignments in the assignment vector until 
       //you run out of time
       int minutesWorked = 0;
       for(WorkItem task : getTasksAssigned()) {
           String report = "  " + task.getName();
           //check whether the task is currently possible
           if(null != task.checkFixable()) {
               report = report + ", but the task is impossible because " + task.checkFixable();
               reports.add(report);
               continue;
           }        
           minutesWorked += task.getTime();
           if(minutesWorked > 480) {
               report = report  + ", but ran out of time for the day, see you tommorrow!";
               reports.add(report);
               break;
           }                 
           TargetRoll target = getTarget();
           target.append(task.getAllMods()); 
           int roll = Compute.d6(2);
           report = report + ", needs " + target.getValueAsString() + " and rolls " + roll + ":";
           if(roll >= target.getValue()) {
               report = report + " task completed.";
               //need to do some rolls here
               task.fix();
               //set this task for removal
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
                   }
               }
           }
           reports.add(report);
       }
       return reports;
   }
    
}
