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

import common.DataFactory;
import common.EquipmentFactory;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import megamek.common.loaders.EntityLoadingException;
import mekhq.campaign.team.SupportTeam;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Entity;

import megamek.common.Game;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.GenericSparePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.personnel.SupportPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.Customization;
import mekhq.campaign.work.FullRepairWarchest;
import mekhq.campaign.work.Refit;
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
    public GregorianCalendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat shortDateFormat;
    
    private int faction; 
    
    private ArrayList<String> currentReport;
    
    private boolean overtime;
    private boolean gmMode;
    private boolean storeTime;
    
    private String camoCategory = Player.NO_CAMO;
    private String camoFileName = null;
    private int colorIndex = 0;

    private static components.Mech sswMech;
    private static EquipmentFactory sswEquipmentFactory;

    private int funds;

    private CampaignOptions campaignOptions = new CampaignOptions();
    
    public Campaign() {
    
        game = new Game();
        currentReport = new ArrayList<String>();
        calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
        dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");  
        shortDateFormat = new SimpleDateFormat("MMddyyyy");
        addReport("<b>" + getDateAsString() + "</b>");
        name = "My Campaign";
        overtime = false;
        gmMode = false;
        faction = Faction.F_MERC;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String s) {
        this.name = s;
    }
    
    public String getEraName() {
       return Era.getEraNameFromYear(calendar.get(Calendar.YEAR)); 
    }
    
    public int getEraMod() {
        return Era.getEraMod(Era.getEra(calendar.get(Calendar.YEAR)), getFaction());
    }
    
    public String getTitle() {
        return getName() + " (" + getFactionName() + ")" + " - " + getDateAsString() + " (" + getEraName() + ")";
    }

    public GregorianCalendar getCalendar() {
        return calendar;
    }

    public static EquipmentFactory getSswEquipmentFactory() {
        if (sswEquipmentFactory==null)
            sswEquipmentFactory = getNewSswEquipmentFactory();
        return sswEquipmentFactory;
    }

    public static components.Mech getSswMech() {
        if (sswMech==null)
            sswMech = getNewSswMech();
        return sswMech;
    }

    public int getFunds() {
        return funds;
    }

    public void setFunds(int funds) {
        this.funds = funds;
    }

    public static EquipmentFactory getNewSswEquipmentFactory() {
        try {
            DataFactory sswDataFactory = new DataFactory(getSswMech());
            EquipmentFactory sswEquipmentFactory = sswDataFactory.GetEquipment();
            return sswEquipmentFactory;
        } catch (Exception ex) {
            Logger.getLogger(Campaign.class.getName()).log(Level.SEVERE, "SSW Lib initialization problem", ex);
            System.exit(1);
        }
        return null;
    }

    public static components.Mech getNewSswMech() {
        return new components.Mech();
    }
    
    /**
     * Add a support team to the campaign
     * @param t 
     *      The support team to be added
     */
    public void addTeam(SupportTeam t) {
        t.setCampaign(this);
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
    public void addUnit(Entity en, boolean allowNewPilots) {
        //TODO: check for duplicate display names
        //first check to see if the externalId of this entity matches any units we already have
        int type = PilotPerson.T_MECH;
        if(en instanceof Tank) {
            type = PilotPerson.T_VEE;
        }
        else if(en instanceof Protomech) {
            type = PilotPerson.T_PROTO;
        }
        else if(en instanceof Aero) {
            type = PilotPerson.T_AERO;
        }
        Unit priorUnit = unitIds.get(new Integer(en.getExternalId()));
        if(null != priorUnit) {
            //this is an existing unit so we need to update it
            en.setId(priorUnit.getId());
            priorUnit.setEntity(en);
            priorUnit.setDeployed(false);
            if(null == en.getCrew() 
                    || en.getCrew().isDead()
                    || en.getCrew().isEjected()) {
                priorUnit.removePilot();
            } else {
                addPilot(en.getCrew(), type, allowNewPilots);
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
            priorUnit.runDiagnostic();
            //this last one is tricky because I want to keep information about skill level required from the old
            //tasks, otherwise reloading a unit will allow user to reset the skill required to green (i.e. cheat)
            for(WorkItem task : getTasksForUnit(priorUnit.getId())) {
                for(WorkItem oldTask : oldTasks) {
                    if(task.sameAs(oldTask)) {
                        task.setSkillMin(oldTask.getSkillMin());
                    }
                }
            }
            addReport(priorUnit.getEntity().getDisplayName() + " has been recovered.");
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
            if(null != en.getCrew()                  
                    && !en.getCrew().isDead()
                    && !en.getCrew().isEjected()) {
                PilotPerson pp = addPilot(en.getCrew(), type, allowNewPilots);
                if(pp != null) {
                    unit.setPilot(pp);
                }
            }
            //collect all the work items outstanding on this unit and add them to the workitem vector
            unit.runDiagnostic();
            addReport(unit.getEntity().getDisplayName() + " has been added to the unit roster.");
        }
    }
    
    /**
     * Add a pilot to the campaign
     * @param en
     *      An <code>Entity</code> object that the new unit will be wrapped around
     */
    public PilotPerson addPilot(Pilot pilot, int type, boolean allowNewPilots) {    
        //check to see if the externalId of this pilot matches any personnel we already have
        Person priorPilot = personnelIds.get(new Integer(pilot.getExternalId()));
        if(null != priorPilot && priorPilot instanceof PilotPerson) {
            if(pilot.isEjected()) {
                ((PilotPerson)priorPilot).getAssignedUnit().removePilot();
            }
            pilot.setEjected(false);
            ((PilotPerson)priorPilot).setPilot(pilot);
            priorPilot.setDeployed(false);        
            //remove any existing tasks for pilot so we can diagnose new ones
            if(null != priorPilot.getTask()) {
                WorkItem task = priorPilot.getTask();
                tasks.remove(task);
                taskIds.remove(new Integer(task.getId()));
            }
            priorPilot.runDiagnostic(this);
            addReport(priorPilot.getDesc() + " has been recovered");
            return (PilotPerson)priorPilot;
        }
        else if (allowNewPilots) {
            PilotPerson pp = new PilotPerson(pilot, type);
            addPerson(pp);
            return pp;
        }
        return null;
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
        if(p instanceof PilotPerson) {
            ((PilotPerson)p).getPilot().setExternalId(id);
        }
        personnel.add(p);
        personnelIds.put(new Integer(id), p);
        lastPersonId = id;
        //check for any work items on this person
        p.runDiagnostic(this);
        addReport(p.getDesc() + " has been added to the personnel roster.");
    }
    
    public ArrayList<Person> getPersonnel() {
        return personnel;
    }
    
    public Person getPerson(int id) {
        return personnelIds.get(new Integer(id));
    }
    
    public void addPart(Part p) {

        if (p instanceof GenericSparePart) {
            for (Part part : getParts()) {
                if (part instanceof GenericSparePart
                        && p.isSamePartTypeAndStatus(part)) {
                    ((GenericSparePart) part).setAmount(((GenericSparePart) part).getAmount() + ((GenericSparePart) p).getAmount());
                    assignParts();
                    return;
                }
            }
        }

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
        if(null == unit) {
            return newTasks;
        }
        int repairSystem = getCampaignOptions().getRepairSystem();
        for(WorkItem task : getTasks()) {
            if(task instanceof UnitWorkItem && ((UnitWorkItem)task).getUnitId() == unitId) {
                if (task instanceof SalvageItem) {
                    if (unit.isSalvage() || unit.isCustomized())
                        newTasks.add(task);
                } else if (task instanceof Refit || task instanceof Customization) {
                    if (unit.isCustomized())
                        newTasks.add(task);
                } else if (task instanceof RepairItem || task instanceof ReplacementItem) {
                    if ((repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS
                            || repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS)
                            && !unit.isSalvage() && !unit.isCustomized())
                        newTasks.add(task);
                } else if (task instanceof ReloadItem) {
                    if ((repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS
                            || repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS)
                            && !unit.isSalvage() && !unit.isCustomized()) {
                        newTasks.add(task);
                    } else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM
                            && !unit.isSalvage() && !unit.isCustomized()) {
                        newTasks.add(task);
                    }
                } else if (task instanceof FullRepairWarchest) {
                    if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM
                            && !unit.isSalvage() && !unit.isCustomized())
                        newTasks.add(task);
                }
            }
        }
        return newTasks;
    }

    public ArrayList<WorkItem> getSalvageTasksForUnit(int unitId) {
        Unit unit = getUnit(unitId);
        ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
        if(null == unit) {
            return newTasks;
        }
        for(WorkItem task : getTasks()) {
            if(task instanceof UnitWorkItem
                    && ((UnitWorkItem)task).getUnitId() == unitId
                    && task instanceof SalvageItem) {
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
        int cost = unit.getRepairCost();
        for(WorkItem task : unitTasks) {
            if(task.isNeeded()) {
                total++;
                totalMin += task.getTime();
            }
        }

        if(total > 0) {
            toReturn += "Total tasks: " + total + " (" + totalMin + " minutes)<br>";
        }
        if (cost > 0) {
            NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
            String text = numberFormat.format(cost) + " " + (cost!=0?"CBills":"CBill");
            toReturn += "Repair cost : " + text + "<br>";
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
         addReport(team.doAssigned(task));
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
             if (isStoreTime()) {
                 int minutesLeft = team.getMinutesLeft();
                 int overtimeMinutesLeft = team.getOvertimeLeft();
                 team.resetMinutesLeft();
                 team.setMinutesLeft(team.getMinutesLeft()+minutesLeft);
                 team.setOvertimeLeft(team.getMinutesLeft()+overtimeMinutesLeft);
             } else {
                 team.resetMinutesLeft();
             }
         }
         ArrayList<WorkItem> assigned = new ArrayList<WorkItem>();
         for(WorkItem task : getTasks()) {
             if(task instanceof ReplacementItem) {
                 ((ReplacementItem)task).setPartCheck(false);
             }
             if(task.isAssigned()) {
                 assigned.add(task);
             }
         }
         for(WorkItem task : assigned) {
            processTask(task, task.getTeam());
         }
         for(Person p : getPersonnel()) {
            if(p.checkNaturalHealing()) {
                addReport(p.getDesc() + " heals naturally!");
            }
         }
         calendar.add(Calendar.DAY_OF_MONTH, 1);
         addReport("<p><b>" + getDateAsString() + "</b>");
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
        for(WorkItem task : getAllTasksForUnit(unit.getId())) {
            removeTask(task);
        }
    }

    public void removeAllUnitWorkItems () {
        ArrayList<WorkItem> tasksToRemove = new ArrayList<WorkItem>();
        for(WorkItem task : getTasks()) {
            if (task instanceof UnitWorkItem)
                tasksToRemove.add(task);
        }
        for (WorkItem task : tasksToRemove) {
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
        addReport(unit.getEntity().getDisplayName() + " has been removed from the unit roster.");
    }
    
    public void removePerson(int id) {
        Person person = getPerson(id);
        
        if(person instanceof PilotPerson && ((PilotPerson)person).isAssigned()) {
            ((PilotPerson)person).getAssignedUnit().removePilot();
        } else if(person instanceof SupportPerson && null != ((SupportPerson)person).getTeam()) {
            removeTeam(((SupportPerson)person).getTeam().getId());
        }
        
        addReport(person.getDesc() + " has been removed from the personnel roster.");
        personnel.remove(person);
        personnelIds.remove(new Integer(id));
    }
    
    public void removeTeam(int id) {
        SupportTeam team = getTeam(id);
        
        for(WorkItem task : getTasks()) {
            if(task.isAssigned() && task.getTeam().getId() == id) {
                task.setTeam(null);
            }
        }
        
        teams.remove(team);
        teamIds.remove(new Integer(id));
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
    
    public String getShortDateAsString() {
        return shortDateFormat.format(calendar.getTime());
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
        for(Unit unit: getUnits()) {
            if(null != unit.getEntity()) {
                unit.getEntity().restore();
            }
        }
    }
    
    public boolean isOvertimeAllowed() {
        return overtime;
    }
    
    public void setOvertime(boolean b) {
        this.overtime = b;
    } 
    
    public boolean isGM() {
        return gmMode;
    }
    
    public void setGMMode(boolean b) {
        this.gmMode = b;
    }

    public void setStoreTime(boolean storeTime) {
        this.storeTime = storeTime;
    }

    public boolean isStoreTime() {
        return storeTime;
    }
    
    public int getFaction() {
        return faction;
    }
    
    public void setFaction(int i) {
        this.faction = i;
    }
    
    public String getFactionName() {
        return Faction.getFactionName(faction);
    }
    
    public void addReport(String r) {
        int maxLine = 150;
        while (currentReport.size()>maxLine) {
            currentReport.remove(0);
        }
        currentReport.add(r);
    }
    
    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        camoFileName = name;
    }

    public String getCamoFileName() {
        return camoFileName;
    }
    
    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        colorIndex = index;
    }

    /**
     * Creates an {@link ArrayList} containing a {@link PartInventory} for each part owned ({@link parts})
     * 
     */
    // TODO : Add some kind of caching method to speed things up when lots of parts
    public ArrayList<PartInventory> getPartsInventory () {
        ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();

        Iterator<Part> itParts = getParts().iterator();
        while (itParts.hasNext()) {
            Part part = itParts.next();
            if (!partsInventory.contains(new PartInventory(part, 0))) {
                partsInventory.add(new PartInventory(part, 1));
            } else {
                partsInventory.get(partsInventory.indexOf(new PartInventory(part, 0))).addOnePart();
            }
        }

        return partsInventory;
    }

    /**
     * Creates an {@link ArrayList} containing a {@link PartInventory} for each part returned by {@link getPartsInventory()} which is of the type {@link partType}
     *
     * @param partType The type of the part as defined in {@link Part}
     */
    public ArrayList<PartInventory> getPartsInventory (int partType) {
        ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();
        Iterator<PartInventory> itParts = getPartsInventory().iterator();
        while (itParts.hasNext()) {
            PartInventory partInventory = itParts.next();
            Part part = partInventory.getPart();
            if (part.getPartType()==partType)
                partsInventory.add(partInventory);
        }
        return partsInventory;
    }

    public void addFunds (int quantity) {
        if(!getCampaignOptions().useFinances()) {
            return;
        }
        setFunds(getFunds()+quantity);
        NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
        String quantityString = numberFormat.format(quantity);
        addReport("Funds added : " + quantityString);
    }
    
    public boolean hasEnoughFunds(int cost) {
        return getFunds()>=cost || !getCampaignOptions().useFinances();
    }

    public boolean buyUnit(Entity en, boolean allowNewPilots) {
        int cost = new Unit(en, this).getBuyCost();
        
        if (hasEnoughFunds(cost)) {
            addUnit(en, allowNewPilots);
            addFunds(-cost);
            return true;
        } else
            return false;
    }

    public void sellUnit (int id) {
        Unit unit = getUnit(id);
        int sellValue = unit.getSellValue();
        
        addFunds(sellValue);
        removeUnit(id);
    }

    public void sellPart (Part part) {
        int cost = part.getCost();
        addFunds(cost / 2);
        removePart(part);
    }

    public void buyPart (Part part) {
        int cost = part.getCost();
        addFunds(-cost);
        addPart(part);
    }

    public void refreshAllUnitDiagnostics() {
        removeAllUnitWorkItems();
        for (Unit unit : getUnits()) {
            unit.runDiagnostic();
        }
    }

    public static Entity getBrandNewUndamagedEntity (String entityShortName) {
        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(entityShortName);
        if (mechSummary==null)
            return null;

        MechFileParser mechFileParser = null;
        try {
            mechFileParser = new MechFileParser(mechSummary.getSourceFile());
        } catch (EntityLoadingException ex) {
            Logger.getLogger(Campaign.class.getName()).log(Level.SEVERE, "MechFileParse exception : " + entityShortName, ex);
        }
        if (mechFileParser==null)
            return null;

        return mechFileParser.getEntity();
    }
    
    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }
}
