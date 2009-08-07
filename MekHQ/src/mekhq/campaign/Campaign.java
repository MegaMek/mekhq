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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import megamek.common.Entity;

import megamek.common.Game;
import megamek.common.Mounted;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.personnel.SupportPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.UnitWorkItem;
import mekhq.campaign.work.WorkItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.SalvageItem;

/**
 *
 * @author Taharqa
 * The main campaign class, keeps track of teams and units
 */
public class Campaign implements Serializable {

    //we have three things to track: (1) teams, (2) units, (3) repair tasks
    //we will use the same basic system (borrowed from MegaMek) for tracking all three
    //OK now we have more, parts and personnel.
    private ArrayList<SupportTeam> teams = new ArrayList<SupportTeam>();
    private Hashtable<Integer, SupportTeam> teamIds = new Hashtable<Integer, SupportTeam>();
    private ArrayList<Unit> units = new ArrayList<Unit>();
    private Hashtable<Integer, Unit> unitIds = new Hashtable<Integer, Unit>();
    private ArrayList<WorkItem> tasks = new ArrayList<WorkItem>();
    private Hashtable<Integer, WorkItem> taskIds = new Hashtable<Integer, WorkItem>();
    private ArrayList<Person> personnel = new ArrayList<Person>();
    private Hashtable<Integer, Person> personnelIds = new Hashtable<Integer, Person>();
    private ArrayList<Part> parts = new ArrayList<Part>();
    private Hashtable<Integer, Part> partIds = new Hashtable<Integer, Part>();
    
    private int lastTeamId;
    private int lastUnitId;
    private int lastTaskId;
    private int lastPersonId;
    private int lastPartId;
    
    //I need to put a basic game object in campaign so that I can
    //asssign it to the entities, otherwise some entity methods may get NPE
    //if they try to call up game options
    private Game game;
    
    private String name;
    
    //calendar stuff
    public Calendar calendar;
    private SimpleDateFormat dateFormat;
    
    private ArrayList<String> currentReport;
    
    public Campaign() {
    
        game = new Game();
        currentReport = new ArrayList<String>();
        calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
        dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
        currentReport.add("<b>" + getDateAsString() + "</b>");
        name = "My Campaign";
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String s) {
        this.name = s;
    }
    
    /**
     * Add a support team to the campaign
     * @param t 
     *      The support team to be added
     */
    public void addTeam(SupportTeam t) {
        int id = lastTeamId + 1;
        t.setId(id);
        teams.add(t);
        teamIds.put(new Integer(id), t);
        lastTeamId = id;
        addPerson(new SupportPerson(t));
    }
    
    /**
     * @return an <code>ArrayList</code> of SupportTeams in the campaign
     */
    public ArrayList<SupportTeam> getTeams() {
        return teams;
    }
    
    /**
     * @param id
     *      the <code>int</code> id of the team
     * @return a <code>SupportTeam</code> object
     */
    public SupportTeam getTeam(int id) {
        return teamIds.get(new Integer(id));
    }
    
    /**
     * Add a unit to the campaign
     * @param en
     *      An <code>Entity</code> object that the new unit will be wrapped around
     */
    public void addUnit(Entity en) {
        //TODO: check for duplicate display names
        //first check to see if the externalId of this entity matches any units we already have
        Unit priorUnit = unitIds.get(new Integer(en.getExternalId()));
        if(null != priorUnit) {
            //this is an existing unit so we need to update it
            en.setId(priorUnit.getId());
            priorUnit.setEntity(en);
            priorUnit.setDeployed(false);
            PilotPerson priorPilot = priorUnit.getPilot();
            if(null == en.getCrew()) {
                priorUnit.removePilot();
            }
            else if(null != priorPilot) { 
                priorPilot.setPilot(en.getCrew());
                priorPilot.setDeployed(false);
                //remove any existing tasks for pilot so we can diagnose new ones
                if(null != priorPilot.getTask()) {
                    WorkItem task = priorPilot.getTask();
                    tasks.remove(task);
                    taskIds.remove(new Integer(task.getId()));
                }
                priorPilot.runDiagnostic(this);
            }
            //remove old tasks
            ArrayList<WorkItem> oldTasks = new ArrayList<WorkItem>();
            for(WorkItem task : getAllTasksForUnit(priorUnit.getId())) {
                if(task instanceof RepairItem || task instanceof ReplacementItem) {
                    oldTasks.add(task);
                }
                tasks.remove(task);
                taskIds.remove(new Integer(task.getId()));
            }
            priorUnit.runDiagnostic(this);
            //this last one is tricky because I want to keep information about skill level required from the old
            //tasks, otherwise reloading a unit will allow user to reset the skill required to green (i.e. cheat)
            for(WorkItem task : getTasksForUnit(priorUnit.getId())) {
                for(WorkItem oldTask : oldTasks) {
                    if(task.sameAs(oldTask)) {
                        task.setSkillMin(oldTask.getSkillMin());
                    }
                }
            }
            
        } else {
            //this is a new unit so add it
            int id = lastUnitId + 1;
            en.setId(id);
            en.setExternalId(id);
            en.setGame(game);
            Unit unit = new Unit(en, this);
            units.add(unit);
            unitIds.put(new Integer(id), unit);
            lastUnitId = id;
            //check for pilot
            if(null != en.getCrew()) {
                PilotPerson pilot = new PilotPerson(en.getCrew(), PilotPerson.getType(en), unit);
                unit.setPilot(pilot);
                addPerson(pilot);
            }
            //collect all the work items outstanding on this unit and add them to the workitem vector
            unit.runDiagnostic(this);
        }
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
        assignPart(task);
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
    
    public void addPart(Part p) {
        int id = lastPartId + 1;
        p.setId(id);
        parts.add(p);
        partIds.put(new Integer(id), p);
        lastPartId = id;
        assignParts();
    }
    
    /**
     * @return an <code>ArrayList</code> of SupportTeams in the campaign
     */
    public ArrayList<Part> getParts() {
        return parts;
    }
    
    public Part getPart(int id) {
        return partIds.get(new Integer(id));
    }
    
    
    public ArrayList<String> getCurrentReport() {
        return currentReport;
    }
    
    public String getCurrentReportHTML() {
        String toReturn = "";
        for(String s: currentReport) {
            toReturn += s + "<br>";
        }
        return toReturn;
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
        Unit unit = getUnit(unitId);
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(task instanceof UnitWorkItem 
                    && ((UnitWorkItem)task).getUnitId() == unitId
                    && ((task instanceof SalvageItem && unit.isSalvage())
                        || (!(task instanceof SalvageItem) && !unit.isSalvage()))) {
                    newTasks.add(task);
            }
        }
        return newTasks;
    }
    
    public ArrayList<WorkItem> getAllTasksForUnit(int unitId) {
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if(task instanceof UnitWorkItem 
                    && ((UnitWorkItem)task).getUnitId() == unitId) {
                    newTasks.add(task);
            }
        }
        return newTasks;
    }
    
    public int countTasksFor(int unitId) {
        int total = 0;
        for(WorkItem task : getTasksForUnit(unitId)) {
            if(task.isNeeded()) {
                total++;
            }
        }
        return total;
    }
    
    /**
     * return an html report on this unit. This will go in MekInfo
     * @param unitId
     * @return
     */
    public String getUnitDesc(int unitId) {
        Unit unit = getUnit(unitId);
        String toReturn = "<html><font size='2'";
        if(unit.isDeployed()) {
            toReturn += " color='white'";
        }
        toReturn += ">";
        toReturn += unit.getDescHTML();
        ArrayList<WorkItem> unitTasks = getTasksForUnit(unitId);
        int totalMin = 0;
        int total = 0;
        for(WorkItem task : unitTasks) {
            if(task.isNeeded()) {
                total++;
                totalMin += task.getTime();
            }
        }
        if(total > 0) {
            toReturn += "Total tasks: " + total + " (" + totalMin + " minutes)<br>";
        }
        toReturn += "</font>";
        toReturn += "</html>";
        return toReturn;
    }
     
    /**
     * Transform one task into another
     * @param oldTask
     *      The <code>WorkItem</code> to be transformed
     * @param newTask
     *      The new <code>WorkItem</code> to be transformed into
     */
     public void mutateTask(WorkItem oldTask, WorkItem newTask) {
         newTask.setId(oldTask.getId());
         taskIds.put(oldTask.getId(), newTask);
         if(newTask instanceof ReplacementItem) {
             assignPart(newTask);
         }
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
    
     public boolean processTask(WorkItem task, SupportTeam team) {
         currentReport.add(team.doAssigned(task));
         if(task.isCompleted()) {
             removeTask(task);
             return true;
         }
         return false;
     }
    
     /**
      * loop through all replacement items and assign the best available part if possible
      * The same part may be assigned to multiple tasks, so rerun this method after each task
      * is processed
      */
     public void assignParts() {
         for(WorkItem task : getTasks()) {
             assignPart(task);
         }
     }
     
     public void assignPart(WorkItem task) {
         if(task instanceof ReplacementItem) {
             ReplacementItem replacement = (ReplacementItem)task;
             replacement.setPart(null);
             for(Part part : getParts()) {
                 if(part.canBeUsedBy(replacement)) {
                     replacement.setPart(part);
                     break;
                 }
             }
         }
     }
     
    public void newDay() {
         for(SupportTeam team : getTeams()) {
            team.resetMinutesLeft();
         }
         //check for any assigned tasks
         ArrayList<WorkItem> assigned = new ArrayList<WorkItem>();
         for(WorkItem task : getTasks()) {
             if(task.isAssigned()) {
                 assigned.add(task);
             }
         }
         for(WorkItem task : assigned) {
            processTask(task, task.getTeam());
         }
         for(Person p : getPersonnel()) {
            if(p.checkNaturalHealing()) {
                currentReport.add(p.getDesc() + " heals naturally!");
            }
         }
         calendar.add(Calendar.DAY_OF_MONTH, 1);
         currentReport.add("<p><b>" + getDateAsString() + "</b>");
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
    
    public void removeAllTasksFor(Unit unit) {
        for(WorkItem task : getTasksForUnit(unit.getId())) {
            removeTask(task);
        }
    }
    
    public void removeUnit(int id) {
        Unit unit = getUnit(id);
        //remove any tasks associated with this unit

        removeAllTasksFor(unit);
          
        //remove the pilot from this unit
        unit.removePilot();
        
        //finally remove the unit
        units.remove(unit);
        unitIds.remove(new Integer(unit.getId()));      
    }
    
    public void removePart(Part part) {
        parts.remove(part);
        partIds.remove(new Integer(part.getId()));
        assignParts();
    }
    
    public void removeTask(WorkItem task) {
        tasks.remove(task);
        taskIds.remove(new Integer(task.getId()));
    }
    
    /**
     * return a string (HTML formatted) of tasks for this unit
     * @param unit
     * @return
     */
    public String getToolTipFor(Unit unit) {
        
        String toReturn = "<html>Double-click for unit view<br>Right-click for further actions<br><b>Tasks:</b><br>";
        for(WorkItem task : getTasksForUnit(unit.getId())) {
            toReturn += task.getDesc() + "<br>";
        }
        toReturn += "</html>";
        return toReturn;
    }
    
    /**
     * return a string (HTML formatted) of tasks for this doctor
     * @param unit
     * @return
     */
    public String getToolTipFor(MedicalTeam doctor) {
        String toReturn = "<html><b>Tasks:</b><br>";
        for(WorkItem task : getTasks()) {
            if(task.isAssigned() && task.getTeam().getId() == doctor.getId()) {
                toReturn += task.getDesc() + "<br>";
            }
        }
        toReturn += "</html>";
        return toReturn;
    }
    
    public String getDateAsString() {
        return dateFormat.format(calendar.getTime());
    }
    
    public ArrayList<PilotPerson> getEligiblePilotsFor(Unit unit) {
        ArrayList<PilotPerson> pilots = new ArrayList<PilotPerson>();
        for(Person p : getPersonnel()) {
            if(!(p instanceof PilotPerson)) {
                continue;
            }
            PilotPerson pp = (PilotPerson)p;
            if(pp.canPilot(unit.getEntity())) {
                pilots.add(pp);
            }
        }
        return pilots;
    }
    
    public void changePilot(Unit unit, PilotPerson pilot) {
        if(null != pilot.getAssignedUnit()) {
            pilot.getAssignedUnit().removePilot();
        }
        unit.setPilot(pilot);
    }
    
    public ReloadItem getReloadWorkFor(Mounted m, Unit unit) {
        for(WorkItem task : getTasks()) {
            if(task instanceof ReloadItem) {
                ReloadItem reload = (ReloadItem)task;
                if(unit.getEntity().getEquipmentNum(m) == unit.getEntity().getEquipmentNum(reload.getMounted())) {
                    return reload;
                }
            }
        }
        return null;
    }
    
    public void restore() {
        for(Part part : getParts()) {
            if(part instanceof EquipmentPart) {
                ((EquipmentPart)part).restore();
            }
        }
    }
    
}
