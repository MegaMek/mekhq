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

package mekhq.campaign;

import mekhq.campaign.team.SupportTeam;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import megamek.common.Entity;

import megamek.common.Game;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.personnel.SupportPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.UnitWorkItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author Taharqa
 * The main campaign class, keeps track of teams and units
 */
public class Campaign implements Serializable {

    //we have three things to track: (1) teams, (2) units, (3) repair tasks
    //we will use the same basic system (borrowed from MegaMek) for tracking all three
    
    private ArrayList<SupportTeam> teams = new ArrayList<SupportTeam>();
    private Hashtable<Integer, SupportTeam> teamIds = new Hashtable<Integer, SupportTeam>();
    private ArrayList<Unit> units = new ArrayList<Unit>();
    private Hashtable<Integer, Unit> unitIds = new Hashtable<Integer, Unit>();
    private ArrayList<WorkItem> tasks = new ArrayList<WorkItem>();
    private Hashtable<Integer, WorkItem> taskIds = new Hashtable<Integer, WorkItem>();
    private ArrayList<Person> personnel = new ArrayList<Person>();
    private Hashtable<Integer, Person> personnelIds = new Hashtable<Integer, Person>();
    
    private int lastTeamId;
    private int lastUnitId;
    private int lastTaskId;
    private int lastPersonId;
    
    private ArrayList<String> currentReport = new ArrayList<String>();
    
    //I need to put a basic game object in campaign so that I can
    //asssign it to the entities, otherwise some entity methods may get NPE
    //if they try to call up game options
    Game game;
    
    //calendar stuff
    public Calendar calendar;
    
    public Campaign() {
    
        game = new Game();
        calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
    
    }
    
    public void addTeam(SupportTeam t) {
        int id = lastTeamId + 1;
        t.setId(id);
        teams.add(t);
        teamIds.put(new Integer(id), t);
        lastTeamId = id;
        addPerson(new SupportPerson(t));
    }
    
    public ArrayList<SupportTeam> getTeams() {
        return teams;
    }
    
    public SupportTeam getTeam(int id) {
        return teamIds.get(new Integer(id));
    }
    
    public void addUnit(Entity en) {
        //TODO: check for duplicate display names
        int id = lastUnitId + 1;
        en.setId(id);
        en.setGame(game);
        Unit unit = new Unit(en);
        units.add(unit);
        unitIds.put(new Integer(id), unit);
        lastUnitId = id;
        //check for pilot
        if(null != en.getCrew()) {
            addPerson(new PilotPerson(en.getCrew(), PilotPerson.getType(en), unit));
        }
        //collect all the work items outstanding on this unit and add them to the workitem vector
        unit.runDiagnostic(this);
    }
    
    public ArrayList<Unit> getUnits() {
        return units;
    }
    
    public ArrayList<Entity> getEntities() {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for(Unit unit : getUnits()) {
            entities.add(unit.getEntity());
        }
        return entities;
    }
    
    public Unit getUnit(int id) {
        return unitIds.get(new Integer(id));
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
    
    public void addPerson(Person p) {
        int id = lastPersonId + 1;
        p.setId(id);
        personnel.add(p);
        personnelIds.put(new Integer(id), p);
        lastPersonId = id;
        //check for any work items on this person
        p.runDiagnostic(this);
    }
    
    public ArrayList<Person> getPersonnel() {
        return personnel;
    }
    
    public Person getPerson(int id) {
        return personnelIds.get(new Integer(id));
    }
    
    public ArrayList<String> getCurrentReport() {
        return currentReport;
    }
    
    public ArrayList<SupportTeam> getDoctors() {
        ArrayList<SupportTeam> docs = new ArrayList<SupportTeam>();
        for(SupportTeam team : getTeams()) {
            if(team instanceof MedicalTeam) {
                  docs.add(team);
            }
        }
        return docs;
    }
    
    public ArrayList<SupportTeam> getTechTeams() {
        ArrayList<SupportTeam> techs = new ArrayList<SupportTeam>();
        for(SupportTeam team : getTeams()) {
            if(team instanceof TechTeam) {
                  techs.add(team);
            }
        }
        return techs;
    }
    
    public ArrayList<WorkItem> getTasksForUnit(int unitId) {
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(task instanceof UnitWorkItem 
                    && ((UnitWorkItem)task).getUnitId() == unitId) {
                newTasks.add(task);
            }
        }
        return newTasks;
    }
    
    /**
     * return an html report on this unit. This will go in MekInfo
     * @param unitId
     * @return
     */
    public String getUnitDesc(int unitId) {
        String toReturn = "<html>" + getUnit(unitId).getDescHTML();
        ArrayList<WorkItem> unitTasks = getTasksForUnit(unitId);
        int totalMin = 0;
        int assignMin = 0;
        int total = 0;
        int assigned = 0;
        for(WorkItem task : unitTasks) {
            if(task.isNeeded()) {
                total++;
                totalMin += task.getTime();
                if(!task.isUnassigned()) {
                    assigned++;
                    assignMin += task.getTime();
                } 
            }
        }
        if(total > 0) {
            toReturn += "Total tasks: " + total + " (" + totalMin + " minutes)<br>";
            toReturn += "Assigned tasks: " + assigned  + " (" + assignMin + " minutes)<br>";
        }
        toReturn += "</html>";
        return toReturn;
    }
     
     public void assignTask(int teamId, int taskId) {
         WorkItem task = taskIds.get(new Integer(taskId));
         task.assignTeam(getTeam(teamId));
         teamIds.get(new Integer(teamId)).addTask(task);
     }
     
     /**
      * repair tasks can mutate into replacement tasks so we need to allow for this
      * via a method
      */
     public void mutateTask(WorkItem oldTask, WorkItem newTask) {
         newTask.setId(oldTask.getId());
         taskIds.put(oldTask.getId(), newTask);
         int index = -1;
         for(WorkItem task : getTasks()) {
             index++;
            if(oldTask.getId() == task.getId()) {
                break;
            }
         }
         getTasks().remove(index);
         getTasks().add(index, newTask);
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
        
        //advance the calendar
        calendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    
    public void clearAllUnits() {
        this.units = new ArrayList<Unit>();
        this.unitIds = new Hashtable<Integer, Unit>();
        this.lastUnitId = 0;
        //also clear tasks, because you can't have tasks without entities
        this.tasks = new ArrayList<WorkItem>();
        this.taskIds = new Hashtable<Integer, WorkItem>();
        this.lastTaskId = 0;
        
    }
    
    public void removeUnit(int id) {
        //remove any tasks associated with this unit

        for(WorkItem task : getTasksForUnit(id)) {
            tasks.remove(task);
            taskIds.remove(new Integer(task.getId()));
        }
        
        //also remove any pilot assigned to this unit
        Person pilot = null;
        for(Person p : getPersonnel()) {
            if(p instanceof PilotPerson && ((PilotPerson)p).getAssignedUnit().getId() == id) {
                pilot = p;
                break;
            }
        }
        if(null != pilot) {
            personnel.remove(pilot);
            personnelIds.remove(pilot.getId());
        }
        
        Unit unit = getUnit(id);
        //finally remove the unit
        units.remove(unit);
        unitIds.remove(new Integer(unit.getId()));      
    }
    
}
