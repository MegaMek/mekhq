/*
 * Campaign.java
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

package mekhq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import megamek.common.Entity;

import megamek.common.Mech;
import mekhq.work.ArmorReplacement;
import mekhq.work.MekGyroReplacement;
import mekhq.work.WorkItem;

/**
 *
 * @author Taharqa
 * The main campaign class, keeps track of teams and units
 */
public class Campaign {

    //we have three things to track: (1) teams, (2) units, (3) repair tasks
    //we will use the same basic system (borrowed from MegaMek) for tracking all three
    
    private ArrayList<SupportTeam> teams = new ArrayList<SupportTeam>();
    private Hashtable<Integer, SupportTeam> teamIds = new Hashtable<Integer, SupportTeam>();
    private ArrayList<Entity> entities = new ArrayList<Entity>();
    private Hashtable<Integer, Entity> entityIds = new Hashtable<Integer, Entity>();
    private ArrayList<WorkItem> tasks = new ArrayList<WorkItem>();
    private Hashtable<Integer, WorkItem> taskIds = new Hashtable<Integer, WorkItem>();
    
    private int lastTeamId;
    private int lastEntityId;
    private int lastTaskId;
    
    private ArrayList<String> currentReport = new ArrayList<String>();
    
    public Campaign() {
        
    }
    
    public void addTeam(SupportTeam t) {
        int id = lastTeamId + 1;
        t.setId(id);
        teams.add(t);
        teamIds.put(new Integer(id), t);
        lastTeamId = id;
    }
    
    public ArrayList<SupportTeam> getTeams() {
        return teams;
    }
    
    public SupportTeam getTeam(int id) {
        return teamIds.get(new Integer(id));
    }
    
     
    public void addEntity(Entity en) {
        //TODO: check for duplicate display names
        int id = lastEntityId + 1;
        en.setId(id);
        entities.add(en);
        entityIds.put(new Integer(id), en);
        lastEntityId = id;
        //TODO: collect all the work items outstanding on this unit
        //and add them to the workitem vector
        runDiagnostic(en);
    }
    
    public ArrayList<Entity> getEntities() {
        return entities;
    }
    
    public Entity getEntity(int id) {
        return entityIds.get(new Integer(id));
    }
    
    public void addWork(WorkItem task) {
        //TODO: check for duplicate display names
        int id = lastTaskId + 1;
        task.setId(id);
        tasks.add(task);
        taskIds.put(new Integer(id), task);
        lastTaskId = id;
    }
    
    public ArrayList<WorkItem> getTasks() {
        return tasks;
    }
    
    public WorkItem getTask(int id) {
        return taskIds.get(new Integer(id));
    }
    
    public ArrayList<String> getCurrentReport() {
        return currentReport;
    }
    
    public ArrayList<WorkItem> getTasksForEntity(int entId) {
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(task.getEntityId() == entId) {
                newTasks.add(task);
            }
        }
        return newTasks;
    }
    
    public String getEntityTaskDesc(int entId) {
        ArrayList<WorkItem> entTasks = getTasksForEntity(entId);
        int minutes = 0;
        int total = 0;
        int assigned = 0;
        for(WorkItem task : entTasks) {
            total++;
            minutes += task.getTime();
            if(!task.isUnassigned()) {
                assigned++;
            } 
        }
        if(total == 0) {
            return "";
        }
        return " (" + assigned + "/" + total + "; " + minutes + " minutes)";
    }
    
     public ArrayList<WorkItem> getTasksForTeam(int teamId) {
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(task.getTeamId() == teamId) {
                newTasks.add(task);
            }
        }
        return newTasks;
    }
     
     public void assignTask(int teamId, int taskId) {
         taskIds.get(new Integer(taskId)).assignTeam(teamId);
     }
    
    //definitely need to refactor this but I can put it here now
    /**
     * Run a diagnostic on the given entity and add its work items to the 
     * list
     */
    public void runDiagnostic(Entity entity) {
        
        //check armor replacement
        for(int i = 0; i < entity.locations(); i++) {
            //TODO: get rear locations as well
            int diff = entity.getOArmor(i) - entity.getArmor(i);
            if(diff > 0) {
                addWork(new ArmorReplacement(entity, i, diff, false));
            }
        }
        
        if(entity instanceof Mech) {
            Mech mech = (Mech)entity;
        }
        
    }
    
    public void processDay() {
        currentReport = new ArrayList<String>();
        //cycle through teams and tell them to get to work
        for(SupportTeam team : getTeams()) {
            currentReport.addAll(team.doAssignments());
        }
        
        //ok now cycle through all tasks and only keep the ones that 
        //have not been completed
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(!task.isCompleted()) {
                newTasks.add(task);
            }
        }
        this.tasks = newTasks;
    }
    
    public void clearEntities() {
        this.entities = new ArrayList<Entity>();
        this.entityIds = new Hashtable<Integer, Entity>();
        this.lastEntityId = 0;
        //also clear tasks, because you can't have tasks without entities
        this.tasks = new ArrayList<WorkItem>();
        this.taskIds = new Hashtable<Integer, WorkItem>();
        this.lastTaskId = 0;
        
    }
    
}
