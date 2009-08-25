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
import megamek.common.Compute;
import megamek.common.TargetRoll;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.UnitWorkItem;
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
    protected int overtimeLeft;
    
//    protected ArrayList<WorkItem> assignedTasks;
    
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
            case SupportTeam.EXP_NUM:
                return "Impossible";
       }
       return "Unknown";
    }
    
    public SupportTeam(String name, int rating) {
        this.name = name;
        this.rating = rating;
        this.hours = 8;
    //    this.assignedTasks = new ArrayList<WorkItem>();
        resetMinutesLeft();
    }
    
    public void setCampaign(Campaign c) {
        this.campaign = c;
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
    
    public void setCurrentStrength(int i) {
        this.currentSize = i;
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
    
    public int getOvertimeLeft() {
        return overtimeLeft;
    }
    
    public void setOvertimeLeft(int m) {
        this.overtimeLeft = m;
    }
    
    public void resetMinutesLeft() {
        this.minutesLeft = 60 * getHours();
        this.overtimeLeft = 60 * 4;
    }
    
    public int getCasualties() {
        return getFullStrength() - getCurrentStrength();
    }
    
    public TargetRoll getTarget(int mode) {    
        int effRating = getRating();
        switch(mode) {
            case WorkItem.MODE_RUSH_THREE:
                effRating--;
            case WorkItem.MODE_RUSH_TWO:
                effRating--;
            case WorkItem.MODE_RUSH_ONE:
                effRating--;
                break;
        }
        if(effRating < EXP_GREEN) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "the current team cannot perform this level of rush job");
        }
        TargetRoll target = new TargetRoll(getSkillBase(effRating), getRatingName(effRating));
        if(getCasualtyMods() > 0) {
            target.addModifier(getCasualtyMods(), "understaffed");
        }
        /*
         * TODO: need an option for era mods
        if(this instanceof TechTeam) {
            target.addModifier(campaign.getEraMod(), "era mod");
        }
         * */
        return target;
    }
    
   public abstract int getSkillBase(int effectiveRating);   
   
   public int getCasualtyMods() {
       int casualties = getCasualties();
       if(casualties > 0 && casualties < 3) {
           return 1;
       } 
       else if(casualties > 2 && casualties < 5) {
           return 2;
       }
       else if(casualties == 5) {
           return 3;
       }
       else if(casualties == 6) {
           return 4;
       }
       return 0;
   }
   
   public String getRatingName() {
       return getRatingName(rating);
   }
   
   public String getDesc() {
       return getName() + " (" + getRatingName() + " " + getTypeDesc() + ") ";
   }
   
   public abstract String getDescHTML();
   
  // public abstract String getTasksDesc();
   
   public abstract String getTypeDesc();
   
   public abstract boolean canDo(WorkItem task);
   
   public abstract int makeRoll(WorkItem task);
   
   public TargetRoll getTargetFor(WorkItem task) {
       if(null == task) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "no task?");
       }
       if(task instanceof UnitWorkItem && ((UnitWorkItem)task).getUnit().isDeployed()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "This unit is currently deployed!");
       }
       if(task instanceof PersonnelWorkItem && ((PersonnelWorkItem)task).getPerson().isDeployed()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "This person is currently deployed!");
       }
       if(null != task.checkFixable()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, task.checkFixable());
       } 
       if(task.getSkillMin() > getRating()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is beyond this team's skill level");
       }
       if(this instanceof MedicalTeam && task instanceof PersonnelWorkItem) {
           PersonnelWorkItem pw = (PersonnelWorkItem)task;
           MedicalTeam doc = (MedicalTeam)this;
           if((pw.getPatients() + doc.getPatients()) > 25) {
               return new TargetRoll(TargetRoll.IMPOSSIBLE, "The doctor already has 25 patients");
           }
       }
       if(!task.isNeeded()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
       }
       if(!canDo(task)) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "Support team cannot do this kind of task.");
       }
       if(task instanceof ReplacementItem && !((ReplacementItem)task).hasPart() && ((ReplacementItem)task).hasCheckedForPart()) {
           return new TargetRoll(TargetRoll.IMPOSSIBLE, "part not available and already checked for this cycle");
       }
       TargetRoll target = getTarget(task.getMode());
       if(target.getValue() == TargetRoll.IMPOSSIBLE) {
           return target;
       }
       //check time
       if(task.getTime() > getMinutesLeft()) {
           if(campaign.isOvertimeAllowed()) {
               if((task.getTime() - getMinutesLeft()) > getOvertimeLeft()) {
                   return new TargetRoll(TargetRoll.IMPOSSIBLE, "Not enough time");
               }
               target.addModifier(3, "overtime");
           } else {
               return new TargetRoll(TargetRoll.IMPOSSIBLE, "Not enough time");
           }
       }
       target.append(task.getAllMods());
       return target;
   }
   
   public String doAssigned(WorkItem task) {
       String report = "";
       if(task instanceof ReplacementItem && !((ReplacementItem)task).hasPart()) {
           //first we need to source the part
           ReplacementItem replace = (ReplacementItem)task;
           Part part = replace.partNeeded();    
           report += getName() + " must first obtain " + part.getDesc();
           TargetRoll target = getTarget(WorkItem.MODE_NORMAL);
           replace.setPartCheck(true);
           //TODO: availability mods
           int roll = Compute.d6(2);
           report += "  needs " + target.getValueAsString() + " and rolls " + roll + ":";
           if(roll >= target.getValue()) {
              report += " <font color='green'><b>part found.</b></font><br>";
              replace.setPart(part);
              campaign.addPart(part);
           } else {
              report += " <font color='red'><b>part not available.</b></font>";
              return report;
           }
       }
       report += getName() + " attempts to " + task.getDisplayName();    
       TargetRoll target = getTargetFor(task);
       int minutes = task.getTime();
       if(minutes > getMinutesLeft()) {
           //we ar working overtime
           minutes -= getMinutesLeft();
           setMinutesLeft(0);
           setOvertimeLeft(getOvertimeLeft() - minutes);
       } else {
           setMinutesLeft(getMinutesLeft() - minutes);
       }
       int roll = makeRoll(task);
       report = report + "  needs " + target.getValueAsString() + " and rolls " + roll + ":";
       if(roll >= target.getValue()) {
           report = report + task.succeed();
       } else {
           report = report + task.fail(getRating());
       }
       return report;
    }
}
