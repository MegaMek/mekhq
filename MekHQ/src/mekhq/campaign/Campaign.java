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

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import megamek.client.RandomNameGenerator;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQApp;
import mekhq.campaign.team.SupportTeam;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Entity;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.GenericSparePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.personnel.SupportPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.Customization;
import mekhq.campaign.work.EquipmentRepair;
import mekhq.campaign.work.EquipmentReplacement;
import mekhq.campaign.work.EquipmentSalvage;
import mekhq.campaign.work.FullRepairWarchest;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.Refit;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.UnitWorkItem;
import mekhq.campaign.work.WorkItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.SalvageItem;

/**
 * 
 * @author Taharqa The main campaign class, keeps track of teams and units
 */
public class Campaign implements Serializable {
	private static final long serialVersionUID = -6312434701389973056L;
	// we have three things to track: (1) teams, (2) units, (3) repair tasks
	// we will use the same basic system (borrowed from MegaMek) for tracking
	// all three
	// OK now we have more, parts and personnel.
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
	private Hashtable<Integer, Force> forceIds = new Hashtable<Integer, Force>();


	private int lastTeamId;
	private int lastUnitId;
	private int lastTaskId;
	private int lastPersonId;
	private int lastPartId;
	private int lastForceId;

	// I need to put a basic game object in campaign so that I can
	// asssign it to the entities, otherwise some entity methods may get NPE
	// if they try to call up game options
	private Game game;

	private String name;

	private RandomNameGenerator rng;
	
	//hierarchically structured Force object to define TO&E
	private Force forces;
	
	// calendar stuff
	public GregorianCalendar calendar;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat shortDateFormat;

	private int faction;
	private Ranks ranks;
	private SkillCosts skillCosts;

	private ArrayList<String> currentReport;

	private boolean overtime;
	private boolean gmMode;

	private String camoCategory = Player.NO_CAMO;
	private String camoFileName = null;
	private int colorIndex = 0;

	private Finances finances;

	private CampaignOptions campaignOptions = new CampaignOptions();

	public Campaign() {
		game = new Game();
		currentReport = new ArrayList<String>();
		calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
		dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
		shortDateFormat = new SimpleDateFormat("MMddyyyy");
		addReport("<b>" + getDateAsString() + "</b>");
		name = "My Campaign";
		rng = new RandomNameGenerator();
		rng.populateNames();
		overtime = false;
		gmMode = false;
		faction = Faction.F_MERC;
		ranks = new Ranks();
		skillCosts = new SkillCosts();
		forces = new Force(name);
		forceIds.put(new Integer(lastForceId), forces);
		lastForceId++;
		finances = new Finances();
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
		return Era.getEraMod(getEra(),
				getFaction());
	}
	
	public int getEra() {
		return Era.getEra(calendar.get(Calendar.YEAR));
	}

	public String getTitle() {
		return getName() + " (" + getFactionName() + ")" + " - "
				+ getDateAsString() + " (" + getEraName() + ")";
	}

	public GregorianCalendar getCalendar() {
		return calendar;
	}

	public RandomNameGenerator getRNG() {
		return rng;
	}
	
	public void setRNG(RandomNameGenerator g) {
		this.rng = g;
	}
	
	public SkillCosts getSkillCosts() {
		return skillCosts;
	}
	
	public long getFunds() {
		return finances.getBalance();
	}
	
	public Force getForces() {
		return forces;
	}

	/**
	 * Add force to an existing superforce. This method will also
	 * assign the force an id and place it in the forceId hash
	 * @param force - the Force to add
	 * @param superForce - the superforce to add the new force to
	 */
	public void addForce(Force force, Force superForce) {
		int id = lastForceId + 1;
		force.setId(id);
		superForce.addSubForce(force);
		forceIds.put(new Integer(id), force);
		lastForceId = id;
		
	}
	
	/**
	 * This is used by the XML loader. The id should already be
	 * set for this force so dont increment
	 * @param force
	 */
	public void addForceToHash(Force force) {
		forceIds.put(force.getId(), force);
	}
	
	/**
	 * Add person to an existing force. This method will also
	 * assign that force's id to the person.
	 * @param p
	 * @param id
	 */
	public void addPersonToForce(Person p, int id) {
		Force force = forceIds.get(id);
		if(null != force) {
			p.setForceId(id);
			force.addPerson(p.getId());
		}
	}
	
	/**
	 * Add a support team to the campaign
	 * 
	 * @param t
	 *            The support team to be added
	 */
	public void addTeam(SupportTeam t) {
		t.setCampaign(this);
		int id = lastTeamId + 1;
		t.setId(id);
		teams.add(t);
		teamIds.put(new Integer(id), t);
		lastTeamId = id;
	}

	private void addTeamWithoutId(SupportTeam t) {
		teams.add(t);
		teamIds.put(new Integer(t.getId()), t);
		
		if (t.getId() > lastTeamId)
			lastTeamId = t.getId();
	}
	
	/**
	 * @return an <code>ArrayList</code> of SupportTeams in the campaign
	 */
	public ArrayList<SupportTeam> getTeams() {
		return teams;
	}

	/**
	 * @param id
	 *            the <code>int</code> id of the team
	 * @return a <code>SupportTeam</code> object
	 */
	public SupportTeam getTeam(int id) {
		return teamIds.get(new Integer(id));
	}


	private void addUnit(Unit u) {
		MekHQApp.logMessage("Adding unit: ("+u.getId()+"):"+u, 5);
		units.add(u);
		unitIds.put(new Integer(u.getId()), u);
		
		if (u.getId() > lastUnitId)
			lastUnitId = u.getId();
	}

	/**
	 * Add a unit to the campaign
	 * 
	 * @param en
	 *            An <code>Entity</code> object that the new unit will be
	 *            wrapped around
	 */
	public void addUnit(Entity en, boolean allowNewPilots) {
		// TODO: check for duplicate display names

		//reset the game object
		en.setGame(game);
		
		//figure out type for pilot addition
		int type = PilotPerson.T_MECHWARRIOR;
		if (en instanceof Tank) {
			type = PilotPerson.T_VEE_CREW;
		} else if (en instanceof Protomech) {
			type = PilotPerson.T_PROTO_PILOT;
		} else if (en instanceof Aero) {
			type = PilotPerson.T_AERO_PILOT;
		}

		//first check to see if the externalId of this entity matches any units
		// we already have
		Unit priorUnit = unitIds.get(new Integer(en.getExternalId()));
		
		if (null != priorUnit) {
			// this is an existing unit so we need to update it
			en.setId(priorUnit.getId());
			priorUnit.setEntity(en);
			priorUnit.setDeployed(false);
			
			if (null == en.getCrew() || en.getCrew().isDead()
					|| en.getCrew().isEjected()) {
				priorUnit.removePilot();
			} else {
				addPilot(en.getCrew(), type, allowNewPilots);
			}
			
			// remove old tasks
			ArrayList<WorkItem> oldTasks = new ArrayList<WorkItem>();
			
			for (WorkItem task : getAllTasksForUnit(priorUnit.getId())) {
				if (task instanceof RepairItem
						|| task instanceof ReplacementItem) {
					oldTasks.add(task);
				}
				
				tasks.remove(task);
				taskIds.remove(new Integer(task.getId()));
			}
			
			priorUnit.runDiagnostic();
			
			// this last one is tricky because I want to keep information about
			// skill level required from the old
			// tasks, otherwise reloading a unit will allow user to reset the
			// skill required to green (i.e. cheat)
			for (WorkItem task : getTasksForUnit(priorUnit.getId())) {
				for (WorkItem oldTask : oldTasks) {
					if (task.sameAs(oldTask)) {
						task.setSkillMin(oldTask.getSkillMin());
					}
				}
			}
			
			addReport(priorUnit.getEntity().getDisplayName()
					+ " has been recovered.");
		} else {
			// This is a new unit so add it
			int id = lastUnitId + 1;
			en.setId(id);
			en.setExternalId(id);
			Unit unit = new Unit(en, this);
			units.add(unit);
			unitIds.put(new Integer(id), unit);
			lastUnitId = id;
			
			if (null != en.getCrew() && !en.getCrew().isDead()
					&& !en.getCrew().isEjected()) {
				PilotPerson pp = addPilot(en.getCrew(), type, allowNewPilots);
				if (pp != null) {
					unit.setPilot(pp);
				}
			}
			
			// collect all the work items outstanding on this unit and add them
			// to the workitem vector
			unit.runDiagnostic();
			addReport(unit.getEntity().getDisplayName()
					+ " has been added to the unit roster.");
		}
	}

	/**
	 * Add a pilot to the campaign
	 * 
	 * @param en
	 *            An <code>Entity</code> object that the new unit will be
	 *            wrapped around
	 */
	public PilotPerson addPilot(Pilot pilot, int type, boolean allowNewPilots) {
		// check to see if the externalId of this pilot matches any personnel we
		// already have
		Person priorPilot = personnelIds
				.get(new Integer(pilot.getExternalId()));
		if (null != priorPilot && priorPilot instanceof PilotPerson) {
			if (pilot.isEjected()) {
				((PilotPerson) priorPilot).getAssignedUnit().removePilot();
			}
			pilot.setEjected(false);
			((PilotPerson) priorPilot).setPilot(pilot);
			priorPilot.setDeployed(false);
			// remove any existing tasks for pilot so we can diagnose new ones
			if (null != priorPilot.getTask()) {
				WorkItem task = priorPilot.getTask();
				tasks.remove(task);
				taskIds.remove(new Integer(task.getId()));
			}
			priorPilot.runDiagnostic(this);
			addReport(priorPilot.getDesc() + " has been recovered");
			return (PilotPerson) priorPilot;
		} else if (allowNewPilots) {
			PilotPerson pp = new PilotPerson(pilot, type, ranks);
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
		for (Unit unit : getUnits()) {
			entities.add(unit.getEntity());
		}
		return entities;
	}

	public Unit getUnit(int id) {
		return unitIds.get(new Integer(id));
	}

	public void addWork(WorkItem task) {
		// TODO: check for duplicate display names
		int id = lastTaskId + 1;
		task.setId(id);
		tasks.add(task);
		taskIds.put(new Integer(id), task);
		lastTaskId = id;
		assignPart(task);
	}
	
	private void addWorkWithoutId(WorkItem task) {
		tasks.add(task);
		taskIds.put(task.getId(), task);
		
		if (task.getId() > lastTaskId)
			lastTaskId = task.getId();
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
		if (p instanceof PilotPerson) {
			((PilotPerson) p).getPilot().setExternalId(id);
		}
		personnel.add(p);
		personnelIds.put(new Integer(id), p);
		lastPersonId = id;
		// check for any work items on this person
		p.runDiagnostic(this);
		addReport(p.getDesc() + " has been added to the personnel roster.");
		if(p instanceof SupportPerson) {
			addTeam(((SupportPerson)p).getTeam());
		}
	}
	
	private void addPersonWithoutId(Person p) {
		personnel.add(p);
		personnelIds.put(p.getId(), p);
		
		if (p.getId() > lastPersonId)
			lastPersonId = p.getId();
		
		//TODO: Should this have runDiagnostic on the person here?...
	}

	public ArrayList<Person> getPersonnel() {
		return personnel;
	}
	
	public ArrayList<Person> getPatients() {
		ArrayList<Person> patients = new ArrayList<Person>();
		for(Person p : getPersonnel()) {
			if(null != p.getTask()) {
				patients.add(p);
			}
		}
		return patients;
	}
	
	public ArrayList<Unit> getServiceableUnits() {
		ArrayList<Unit> service = new ArrayList<Unit>();
		for(Unit u : getUnits()) {
			if(countTasksFor(u.getId()) > 0) {
				service.add(u);
			}
		}
		return service;
	}

	public Person getPerson(int id) {
		return personnelIds.get(new Integer(id));
	}

	public void addPart(Part p) {

		if (p instanceof GenericSparePart) {
			for (Part part : getParts()) {
				if (part instanceof GenericSparePart
						&& p.isSamePartTypeAndStatus(part)) {
					((GenericSparePart) part)
							.setAmount(((GenericSparePart) part).getAmount()
									+ ((GenericSparePart) p).getAmount());
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
	
	private void addPartWithoutId(Part p) {
		parts.add(p);
		partIds.put(p.getId(), p);
		
		if (p.getId() > lastPartId)
			lastPartId = p.getId();
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
	
	public Force getForce(int id) {
		return forceIds.get(new Integer(id));
	}

	public ArrayList<String> getCurrentReport() {
		return currentReport;
	}

	public String getCurrentReportHTML() {
		String toReturn = "";
		//lets do the report backwards
		for (String s : currentReport) {
			toReturn += s + "<br/>";
		}
		return toReturn;
	}

	public ArrayList<SupportTeam> getDoctors() {
		ArrayList<SupportTeam> docs = new ArrayList<SupportTeam>();
		for (SupportTeam team : getTeams()) {
			if (team instanceof MedicalTeam) {
				docs.add(team);
			}
		}
		return docs;
	}

	public ArrayList<SupportTeam> getTechTeams() {
		ArrayList<SupportTeam> techs = new ArrayList<SupportTeam>();
		for (SupportTeam team : getTeams()) {
			if (team instanceof TechTeam) {
				techs.add(team);
			}
		}
		return techs;
	}

	public ArrayList<WorkItem> getTasksForUnit(int unitId) {
		Unit unit = getUnit(unitId);
		ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
		if (null == unit) {
			return newTasks;
		}
		int repairSystem = getCampaignOptions().getRepairSystem();
		for (WorkItem task : getTasks()) {
			if (task instanceof UnitWorkItem
					&& ((UnitWorkItem) task).getUnitId() == unitId) {
				if (task instanceof SalvageItem) {
					if (unit.isSalvage() || unit.isCustomized())
						newTasks.add(task);
				} else if (task instanceof Refit
						|| task instanceof Customization) {
					if (unit.isCustomized())
						newTasks.add(task);
				} else if (task instanceof RepairItem
						|| task instanceof ReplacementItem) {
					if ((repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS || repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS)
							&& !unit.isSalvage() && !unit.isCustomized())
						newTasks.add(task);
				} else if (task instanceof ReloadItem) {
					if ((repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS || repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS)
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
		if (null == unit) {
			return newTasks;
		}
		for (WorkItem task : getTasks()) {
			if (task instanceof UnitWorkItem
					&& ((UnitWorkItem) task).getUnitId() == unitId
					&& task instanceof SalvageItem) {
				newTasks.add(task);
			}
		}
		return newTasks;
	}

	public ArrayList<WorkItem> getAllTasksForUnit(int unitId) {
		ArrayList<WorkItem> newTasks = new ArrayList<WorkItem>();
		for (WorkItem task : getTasks()) {
			if (task instanceof UnitWorkItem
					&& ((UnitWorkItem) task).getUnitId() == unitId) {
				newTasks.add(task);
			}
		}
		return newTasks;
	}

	public int countTasksFor(int unitId) {
		int total = 0;
		for (WorkItem task : getTasksForUnit(unitId)) {
			if (task.isNeeded()) {
				total++;
			}
		}
		return total;
	}
	
	public int getTotalTasksFor(int unitId) {
		ArrayList<WorkItem> unitTasks = getTasksForUnit(unitId);
		int n = 0;
		for (WorkItem task : unitTasks) {
			if(task.isNeeded()) {
				n++;
			}
		}
		return n;
	}
	
	public int getTotalPartsFor(int unitId) {
		ArrayList<WorkItem> unitTasks = getTasksForUnit(unitId);
		int n = 0;
		for (WorkItem task : unitTasks) {
			if(task instanceof ReplacementItem && !((ReplacementItem)task).hasPart()) {
				n++;
			}
		}
		return n;
	}

	/**
	 * return an html report on this unit. This will go in MekInfo
	 * 
	 * @param unitId
	 * @return
	 */
	public String getUnitDesc(int unitId) {
		Unit unit = getUnit(unitId);
		String toReturn = "<html><font size='2'";
		if (unit.isDeployed()) {
			toReturn += " color='white'";
		}
		toReturn += ">";
		toReturn += unit.getDescHTML();
		ArrayList<WorkItem> unitTasks = getTasksForUnit(unitId);
		int totalMin = 0;
		int total = 0;
		int cost = unit.getRepairCost();
		for (WorkItem task : unitTasks) {
			if (task.isNeeded()) {
				total++;
				totalMin += task.getTimeLeft();
			}
		}

		if (total > 0) {
			toReturn += "Total tasks: " + total + " (" + totalMin
					+ " minutes)<br/>";
		}
		if (cost > 0) {
			NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
			String text = numberFormat.format(cost) + " "
					+ (cost != 0 ? "CBills" : "CBill");
			toReturn += "Repair cost : " + text + "<br/>";
		}

		toReturn += "</font>";
		toReturn += "</html>";
		return toReturn;
	}

	/**
	 * Transform one task into another
	 * 
	 * @param oldTask
	 *            The <code>WorkItem</code> to be transformed
	 * @param newTask
	 *            The new <code>WorkItem</code> to be transformed into
	 */
	public void mutateTask(WorkItem oldTask, WorkItem newTask) {
		newTask.setId(oldTask.getId());
		taskIds.put(oldTask.getId(), newTask);
		if (newTask instanceof ReplacementItem) {
			assignPart(newTask);
		}
		int index = -1;
		for (WorkItem task : getTasks()) {
			index++;
			if (oldTask.getId() == task.getId()) {
				break;
			}
		}
		getTasks().remove(index);
		getTasks().add(index, newTask);
	}

	public void getPartFor(WorkItem task, SupportTeam team) {
		addReport(team.acquirePartFor(task));
	}
	
	public boolean processTask(WorkItem task, SupportTeam team) {
		addReport(team.doAssigned(task));
		if (task.isCompleted()) {
			removeTask(task);
			return true;
		}
		return false;
	}

	/**
	 * loop through all replacement items and assign the best available part if
	 * possible The same part may be assigned to multiple tasks, so rerun this
	 * method after each task is processed
	 */
	public void assignParts() {
		for (WorkItem task : getTasks()) {
			assignPart(task);
		}
	}

	public void assignPart(WorkItem task) {
		if (task instanceof ReplacementItem) {
			ReplacementItem replacement = (ReplacementItem) task;
			replacement.setPart(null);
			for (Part part : getParts()) {
				if (part.canBeUsedBy(replacement)) {
					replacement.setPart(part);
					break;
				}
			}
		}
	}

	public void newDay() {
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		addReport("<p/><b>" + getDateAsString() + "</b>");

		for (SupportTeam team : getTeams()) {
			team.resetMinutesLeft();
		}
		ArrayList<WorkItem> assigned = new ArrayList<WorkItem>();
		for (WorkItem task : getTasks()) {
			if (task instanceof ReplacementItem) {
				((ReplacementItem) task).setPartCheck(false);
			}
			if (task.isAssigned()) {
				assigned.add(task);
			}
		}
		for (WorkItem task : assigned) {
			processTask(task, task.getTeam());
		}
		for (Person p : getPersonnel()) {
			if (p.checkNaturalHealing()) {
				addReport(p.getDesc() + " heals naturally!");
			}
		}
		DecimalFormat formatter = new DecimalFormat();
		//check for a new year
		if(calendar.get(Calendar.MONTH) == 0 && calendar.get(Calendar.DAY_OF_MONTH) == 1) {
			//clear the ledger
			finances.newFiscalYear(calendar.getTime());
		}
		if(calendar.get(Calendar.DAY_OF_WEEK) == 0) {
			//maintenance costs
			if(campaignOptions.payForMaintain()) {
				finances.debit(getMaintenanceCosts(), Transaction.C_MAINTAIN, "Weekly Maintenance", calendar.getTime());
				addReport("Your account has been debited for " + formatter.format(getMaintenanceCosts()) + " C-bills in maintenance costs");
			}
		}
		if(calendar.get(Calendar.DAY_OF_MONTH) == 1) {
			//Payday!
			if(campaignOptions.payForSalaries()) {
				finances.debit(getPayRoll(), Transaction.C_SALARY, "Monthly salaries", calendar.getTime());
				addReport("Payday! Your account has been debited for " + formatter.format(getPayRoll()) + " C-bills in personnel salaries");
			}
			if(campaignOptions.payForOverhead()) {
				finances.debit(getOverheadExpenses(), Transaction.C_OVERHEAD, "Monthly overhead", calendar.getTime());
				addReport("Your account has been debited for " + formatter.format(getOverheadExpenses()) + " C-bills in overhead expenses");
			}
		}
	}
	
	public long getPayRoll() {
		long salaries = 0;
		for(Person p : personnel) {
			if(p.isActive()) {
				salaries += p.getSalary();
			}
		}
		return salaries;
	}
	
	public long getMaintenanceCosts() {
		long costs = 0;
		for(Unit u : units) {
			if(!u.isSalvage()) {
				costs += u.getMaintenanceCost();
			}
		}
		return costs;
	}
	
	public long getOverheadExpenses() {
		return (long)(getPayRoll() * 0.05);
	}

	public void clearAllUnits() {
		this.units = new ArrayList<Unit>();
		this.unitIds = new Hashtable<Integer, Unit>();
		this.lastUnitId = 0;
		// also clear tasks, because you can't have tasks without entities
		this.tasks = new ArrayList<WorkItem>();
		this.taskIds = new Hashtable<Integer, WorkItem>();
		this.lastTaskId = 0;

	}

	public void removeAllTasksFor(Unit unit) {
		ArrayList<WorkItem> tasksToRemove = new ArrayList<WorkItem>();
		for (WorkItem task : getAllTasksForUnit(unit.getId())) {
			tasksToRemove.add(task);
		}
		for (WorkItem task : tasksToRemove) {
			removeTask(task);
		}
	}

	public void removeAllUnitWorkItems() {
		ArrayList<WorkItem> tasksToRemove = new ArrayList<WorkItem>();
		for (WorkItem task : getTasks()) {
			if (task instanceof UnitWorkItem)
				tasksToRemove.add(task);
		}
		for (WorkItem task : tasksToRemove) {
			removeTask(task);
		}
	}

	public void removeUnit(int id) {
		Unit unit = getUnit(id);
		// remove any tasks associated with this unit

		removeAllTasksFor(unit);

		// remove the pilot from this unit
		unit.removePilot();

		// finally remove the unit
		units.remove(unit);
		unitIds.remove(new Integer(unit.getId()));
		addReport(unit.getEntity().getDisplayName()
				+ " has been removed from the unit roster.");
	}

	public void removePerson(int id) {
		Person person = getPerson(id);

		if (person instanceof PilotPerson
				&& ((PilotPerson) person).isAssigned()) {
			((PilotPerson) person).getAssignedUnit().removePilot();
		} else if (person instanceof SupportPerson
				&& null != ((SupportPerson) person).getTeam()) {
			removeTeam(((SupportPerson) person).getTeam().getId());
		}

		addReport(person.getDesc()
				+ " has been removed from the personnel roster.");
		personnel.remove(person);
		personnelIds.remove(new Integer(id));
	}

	public void removeTeam(int id) {
		SupportTeam team = getTeam(id);

		for (WorkItem task : getTasks()) {
			if (task.isAssigned() && task.getTeam().getId() == id) {
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
	
	public void removeForce(Force force) {
		int fid = force.getId();
		forceIds.remove(new Integer(fid));
		//clear forceIds of all personnel with this force
		for(Person p : personnel) {
			if(p.getForceId() == fid) {
				p.setForceId(0);
			}
		}
		if(null != force.getParentForce()) {
			force.getParentForce().removeSubForce(fid);
		}
	}
	
	public void removePersonFromForce(Person p) {
		Force force = getForce(p.getForceId());
		if(null != force) {
			force.removePerson(p.getId());
			p.setForceId(0);
		}
	}
	 
	public Force getForceFor(Person p) {
		return getForce(p.getForceId());
	}
	
	/**
	 * return a string (HTML formatted) of tasks for this unit
	 * 
	 * @param unit
	 * @return
	 */
	public String getTaskListFor(Unit unit) {
		String toReturn = "";
		for (WorkItem task : getTasksForUnit(unit.getId())) {
			toReturn += task.getDesc() + "<br/>";
		}
		if(toReturn.equals("")) {
			return null;
		}
		return "<html>" + toReturn + "</html>";
	}

	/**
	 * return a string (HTML formatted) of tasks for this doctor
	 * 
	 * @param unit
	 * @return
	 */
	public String getToolTipFor(MedicalTeam doctor) {
		String toReturn = "<html><b>Tasks:</b><br/>";
		for (WorkItem task : getTasks()) {
			if (task.isAssigned() && task.getTeam().getId() == doctor.getId()) {
				toReturn += task.getDesc() + "<br/>";
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
		for (Person p : getPersonnel()) {
			if (!(p instanceof PilotPerson)) {
				continue;
			}
			PilotPerson pp = (PilotPerson) p;
			if (pp.canPilot(unit.getEntity())) {
				pilots.add(pp);
			}
		}
		return pilots;
	}
	
	public ArrayList<Unit> getEligibleUnitsFor(Person person) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		if(!(person instanceof PilotPerson)) {
			return units;
		}
		PilotPerson pp = (PilotPerson)person;
		for (Unit u : this.getUnits()) {
			if (pp.canPilot(u.getEntity())) {
				units.add(u);
			}
		}
		return units;
	}

	public void changePilot(Unit unit, PilotPerson pilot) {
		if (null != pilot.getAssignedUnit()) {
			pilot.getAssignedUnit().removePilot();
		}
		unit.setPilot(pilot);
	}

	public ReloadItem getReloadWorkFor(Mounted m, Unit unit) {
		for (WorkItem task : getTasks()) {
			if (task instanceof ReloadItem) {
				ReloadItem reload = (ReloadItem) task;
				if (unit.getEntity().getEquipmentNum(m) == unit.getEntity()
						.getEquipmentNum(reload.getMounted())) {
					return reload;
				}
			}
		}
		return null;
	}

	public void restore() {
		for (Part part : getParts()) {
			if (part instanceof EquipmentPart) {
				((EquipmentPart) part).restore();
			}
		}

		for (Unit unit : getUnits()) {
			if (null != unit.getEntity()) {
				unit.getEntity().setGame(game);
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
		while (currentReport.size() > maxLine) {
			currentReport.remove(currentReport.size()-1);
		}
		currentReport.add(0,r);
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
	 * Creates an {@link ArrayList} containing a {@link PartInventory} for each
	 * part owned ({@link parts})
	 * 
	 */
	// TODO : Add some kind of caching method to speed things up when lots of
	// parts
	public ArrayList<PartInventory> getPartsInventory() {
		ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();

		Iterator<Part> itParts = getParts().iterator();
		while (itParts.hasNext()) {
			Part part = itParts.next();
			if (!partsInventory.contains(new PartInventory(part, 0))) {
				partsInventory.add(new PartInventory(part, 1));
			} else {
				partsInventory.get(
						partsInventory.indexOf(new PartInventory(part, 0)))
						.addOnePart();
			}
		}

		return partsInventory;
	}

	/**
	 * Creates an {@link ArrayList} containing a {@link PartInventory} for each
	 * part returned by {@link getPartsInventory()} which is of the type
	 * {@link partType}
	 * 
	 * @param partType
	 *            The type of the part as defined in {@link Part}
	 */
	public ArrayList<PartInventory> getPartsInventory(int partType) {
		ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();
		Iterator<PartInventory> itParts = getPartsInventory().iterator();
		while (itParts.hasNext()) {
			PartInventory partInventory = itParts.next();
			Part part = partInventory.getPart();
			if (part.getPartType() == partType)
				partsInventory.add(partInventory);
		}
		return partsInventory;
	}

	public void addFunds(long quantity) {
		finances.credit(quantity, Transaction.C_MISC, "Rich Uncle", calendar.getTime());
		NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
		String quantityString = numberFormat.format(quantity);
		addReport("Funds added : " + quantityString);
	}

	public boolean hasEnoughFunds(long cost) {
		return getFunds() >= cost;
	}
	
	public boolean buyUnit(Entity en, boolean allowNewPilots) {
		int cost = new Unit(en, this).getBuyCost();

		if (hasEnoughFunds(cost) || !campaignOptions.payForUnits()) {
			addUnit(en, allowNewPilots);
			if(campaignOptions.payForUnits()) {
				finances.debit(cost, Transaction.C_UNIT, "Purchased " + en.getDisplayName(), calendar.getTime());
			}
			return true;
		} else
			return false;
	}

	public void sellUnit(int id) {
		Unit unit = getUnit(id);
		int sellValue = unit.getSellValue();
		finances.credit(sellValue, Transaction.C_UNIT_SALE, "Sale of " + unit.getEntity().getDisplayName(), calendar.getTime());
		removeUnit(id);
	}

	public void sellPart(Part part) {
		long cost = part.getCost();
		finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + part.getName(), calendar.getTime());
		removePart(part);
	}

	public void buyPart(Part part) {
		long cost = part.getCost();
		finances.debit(cost, Transaction.C_EQUIP, "Sale of " + part.getName(), calendar.getTime());
		addPart(part);
	}

	public void refreshAllUnitDiagnostics() {
		removeAllUnitWorkItems();
		for (Unit unit : getUnits()) {
			unit.runDiagnostic();
		}
	}

	public static Entity getBrandNewUndamagedEntity(String entityShortName) {
		MechSummary mechSummary = MechSummaryCache.getInstance().getMech(
				entityShortName);
		if (mechSummary == null)
			return null;

		MechFileParser mechFileParser = null;
		try {
			mechFileParser = new MechFileParser(mechSummary.getSourceFile());
		} catch (EntityLoadingException ex) {
			Logger.getLogger(Campaign.class.getName()).log(Level.SEVERE,
					"MechFileParse exception : " + entityShortName, ex);
		}
		if (mechFileParser == null)
			return null;

		return mechFileParser.getEntity();
	}

	public CampaignOptions getCampaignOptions() {
		return campaignOptions;
	}

	public void writeToXml(PrintWriter pw1) {
		
		// File header
		pw1.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		// Start the XML root.
		pw1.println("<campaign>");

		// Basic Campaign Info
		pw1.println("\t<info>");

		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "name", name);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "faction", faction);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "ranks", ranks.getRankSystem());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "nameGen", rng.getChosenFaction());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "percentFemale", rng.getPercentFemale());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "overtime", overtime);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "gmMode", gmMode);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoCategory", camoCategory);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoFileName", camoFileName);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "colorIndex", colorIndex);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastTeamId", lastTeamId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastUnitId", lastUnitId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastTaskId", lastTaskId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastPersonId", lastPersonId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastPartId", lastPartId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastForceId", lastForceId);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "calendar",
				df.format(calendar.getTime()));
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "dateFormat",
				dateFormat.toLocalizedPattern());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "shortDateFormat",
				shortDateFormat.toLocalizedPattern());
		{
			pw1.println("\t\t<nameGen>");
			pw1.print("\t\t\t<faction>");
			pw1.print(rng.getChosenFaction());
			pw1.println("</faction>");
			pw1.print("\t\t\t<percentFemale>");
			pw1.print(rng.getPercentFemale());
			pw1.println("</percentFemale>");
			pw1.println("\t\t</nameGen>");
		}
		
		{
			pw1.println("\t\t<currentReport>");

			for (int x = 0; x < currentReport.size(); x++) {
				pw1.print("\t\t\t<reportLine><![CDATA[");
				pw1.print(currentReport.get(x));
				pw1.println("]]></reportLine>");
			}

			pw1.println("\t\t</currentReport>");
		}

		pw1.println("\t</info>");

		// Campaign Options
		// private CampaignOptions campaignOptions = new CampaignOptions();
		if (getCampaignOptions() != null)
			getCampaignOptions().writeToXml(pw1, 1);

		// Lists of objects:
		writeArrayAndHashToXml(pw1, 1, "teams", teams, teamIds); // Teams
		writeArrayAndHashToXml(pw1, 1, "units", units, unitIds); // Units
		writeArrayAndHashToXml(pw1, 1, "tasks", tasks, taskIds); // Tasks
		writeArrayAndHashToXml(pw1, 1, "personnel", personnel, personnelIds); // Personnel
		writeArrayAndHashToXml(pw1, 1, "parts", parts, partIds); // Parts
		
		//the forces structure is hierarchical, but that should be handled internally
		//from with writeToXML function for Force
		pw1.println("\t<forces>");
		forces.writeToXml(pw1, 2);
		pw1.println("\t</forces>");

		finances.writeToXml(pw1,1);
		
		// Okay, we're done.
		// Close everything out and be done with it.
		pw1.println("</campaign>");
	}

	/**
	 * A helper function to encapsulate writing the array/hash pairs out to XML.
	 * Each of the types requires a different XML structure, but is in an
	 * identical holding structure. Thus, genericized function and interface to
	 * cleanly wrap it up. God, I love 3rd-generation programming languages.
	 * 
	 * @param <arrType>
	 *            The object type in the list. Must implement
	 *            MekHqXmlSerializable.
	 * @param pw1
	 *            The PrintWriter to output XML to.
	 * @param indent
	 *            The indentation level to use for writing XML (purely for
	 *            neatness).
	 * @param tag
	 *            The name of the tag to use to encapsulate it.
	 * @param array
	 *            The list of objects to write out.
	 * @param hashtab
	 *            The lookup hashtable for the associated array.
	 */
	private <arrType> void writeArrayAndHashToXml(PrintWriter pw1, int indent,
			String tag, ArrayList<arrType> array,
			Hashtable<Integer, arrType> hashtab) {
		// Hooray for implicitly-type-detected genericized functions!
		// However, I still ended up making an interface to handle this.
		// That way, I can cast it and call "writeToXml" to make it cleaner.
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

		// Enumeration<Integer> = hashtab.keys
		for (int x : hashtab.keySet()) {
			((MekHqXmlSerializable) (hashtab.get(x))).writeToXml(pw1,
					indent + 1, x);
		}

		pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
	}
	
	 

	/**
	 * Designed to create a campaign object from a file containing an XML
	 * structure. Instead of actually manually parsing it all, lets pull it into
	 * a DOM and parse that.
	 * 
	 * @param fis
	 *            The file holding the XML, in FileInputStream form.
	 * @return The created Campaign object, or null if there was a problem.
	 * @throws ParseException
	 * @throws DOMException
	 */
	public static Campaign createCampaignFromXMLFileInputStream(FileInputStream fis)
			throws DOMException, ParseException {
		MekHQApp.logMessage("Starting load of campaign file from XML...");
		// Initialize variables.
		Campaign retVal = new Campaign();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;

		try {
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// Parse using builder to get DOM representation of the XML file
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
			MekHQApp.logError(ex);
		}

		Element campaignEle = xmlDoc.getDocumentElement();
		NodeList nl = campaignEle.getChildNodes();

		// Get rid of empty text nodes and adjacent text nodes...
		// Stupid weird parsing of XML.  At least this cleans it up.
		campaignEle.normalize(); 

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);

			if (wn.getParentNode() != campaignEle)
				continue;

			int xc = wn.getNodeType();

			if (xc == Node.ELEMENT_NODE) {
				// This is what we really care about.
				// All the meat of our document is in this node type, at this
				// level.
				// Okay, so what element is it?
				String xn = wn.getNodeName();

				if (xn.equalsIgnoreCase("campaignOptions")) {
					retVal.campaignOptions = CampaignOptions
							.generateCampaignOptionsFromXml(wn);
				} else if (xn.equalsIgnoreCase("info")) {
					processInfoNode(retVal, wn);
				} else if (xn.equalsIgnoreCase("parts")) {
					processPartNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("personnel")) {
					processPersonnelNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("tasks")) {
					processTaskNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("teams")) {
					processTeamNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("units")) {
					processUnitNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("forces")) {
					processForces(retVal, wn);
				} else if (xn.equalsIgnoreCase("finances")) {
					processFinances(retVal, wn);
				}
				
			} else {
				// If it's a text node or attribute or whatever at this level,
				// it's probably white-space.
				// We can safely ignore it even if it isn't, for now.
				continue;
			}
		}
		
		// Okay, after we've gone through all the nodes and constructed the Campaign object...
		// We need to do a post-process pass to restore a number of references.

		// First, iterate through Support Teams;
		// they have a reference to the Campaign object.
		for (int x=0; x<retVal.teams.size(); x++) {
			SupportTeam st = retVal.teams.get(x);
			st.setCampaign(retVal);
			
			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			st.reCalc();
		}
		
		// Okay, Units, need their pilot references fixed.
		for (int x=0; x<retVal.units.size(); x++) {
			Unit unit = retVal.units.get(x);
			
			if (unit.getPilotId() >= 0)
				unit.setPilot((PilotPerson) retVal.personnelIds.get(unit.getPilotId()));
			
			// Also, the unit should have its campaign set.
			unit.campaign = retVal;
			
			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			unit.reCalc();
		}
		
		// Process parts...
		for (int x=0; x<retVal.parts.size(); x++) {
			Part prt = retVal.parts.get(x);
			
			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			prt.reCalc();
		}
		
		// Some personnel need their task references fixed.
		// All personnel need the rank reference fixed
		for (int x=0; x<retVal.personnel.size(); x++) {
			Person psn = retVal.personnel.get(x);
			
			psn.setRankSystem(retVal.ranks);
			
			if (psn.getTaskId() >= 0)
				psn.setTask((PersonnelWorkItem) retVal.taskIds.get(psn.getTaskId()));
			
			if (psn instanceof SupportPerson) {
				SupportPerson psn2 = (SupportPerson)psn;

				if (psn2.getTeamId() >= 0) {
					psn2.setTeam(retVal.teamIds.get(psn2.getTeamId()));
				}
			}
			
			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			psn.reCalc();
		}
		
		// Okay, work items.
		// ReplacementItem needs to have references to Part fixed.
		for (int x=0; x<retVal.tasks.size(); x++) {
			WorkItem wrk = retVal.tasks.get(x);
			int supportTeamId = wrk.getSupportTeamId();
			
			if (supportTeamId >= 0)
				wrk.setSupportTeam(retVal.teamIds.get(supportTeamId));
			
			if (wrk instanceof PersonnelWorkItem) {
				int personId = ((PersonnelWorkItem)wrk).getPersonId();
				
				if (personId >= 0)
					((PersonnelWorkItem)wrk).setPerson(retVal.personnelIds.get(personId));
			} else if (wrk instanceof UnitWorkItem) {
				int unitId = ((UnitWorkItem)wrk).getUnitStoredId();
				
				if (unitId >= 0) {
					((UnitWorkItem)wrk).setUnit(retVal.unitIds.get(unitId));
				}

				// If it's a replacement part, then it references a Part.
				// The part was stored as an ID.
				// Grab the part reference based on the ID.
				if (wrk instanceof ReplacementItem) {
					int partId = ((ReplacementItem) wrk).getPartId();
					
					if (partId >= 0)
						((ReplacementItem) wrk).setPart(retVal.partIds.get(partId));
				}
				
				// If it's one of several types, then the Mounted was stored as an Equipment ID.
				// We know, since it's a UnitWorkItem, that the unit was just defined above...
				// So we go to the unit...  Grab the entity...  And then pull the Mounted off of that.
				if (wrk instanceof EquipmentRepair) {
					int equipId = ((EquipmentRepair) wrk).getEquipmentNum();
					
					if (equipId >= 0)
						((EquipmentRepair) wrk).setMounted(((UnitWorkItem)wrk).getUnit().getEntity().getEquipment(equipId));
				}

				if (wrk instanceof EquipmentReplacement) {
					int equipId = ((EquipmentReplacement) wrk).getEquipmentNum();
					
					if (equipId >= 0)
						((EquipmentReplacement) wrk).setMounted(((UnitWorkItem)wrk).getUnit().getEntity().getEquipment(equipId));
				}

				if (wrk instanceof ReloadItem) {
					int equipId = ((ReloadItem) wrk).getEquipmentNum();
					
					if (equipId >= 0) {
						UnitWorkItem uwi = (UnitWorkItem)wrk;
						Unit un = uwi.getUnit();
						Entity en = un.getEntity();
						Mounted eq = en.getEquipment(equipId);
						((ReloadItem) wrk).setMounted(eq);
					}
				}

				if (wrk instanceof EquipmentSalvage) {
					int equipId = ((EquipmentSalvage) wrk).getEquipmentNum();
					
					if (equipId >= 0)
						((EquipmentSalvage) wrk).setMounted(((UnitWorkItem)wrk).getUnit().getEntity().getEquipment(equipId));
				}
			}
			
			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			wrk.reCalc();
		}

		MekHQApp.logMessage("Load of campaign file complete!");

		return retVal;
	}

	private static void processFinances(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Finances from XML...", 4);
		retVal.finances = Finances.generateInstanceFromXML(wn);
		MekHQApp.logMessage("Load of Finances complete!");
	}

	
	private static void processForces(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Force Organization from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		boolean foundForceAlready = false;
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("force")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Forces nodes: "+wn2.getNodeName());

				continue;
			}
			
			if(!foundForceAlready)  {
				Force f = Force.generateInstanceFromXML(wn2, retVal);
				if(null != f) {
					retVal.forces = f;
					foundForceAlready = true;
				}
			} else {
				MekHQApp.logMessage("More than one type-level force found", 5);
			}
		}
		
		MekHQApp.logMessage("Load of Force Organization complete!");
	}
	
	private static void processPersonnelNodes(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Personnel Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("person")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Personnel nodes: "+wn2.getNodeName());

				continue;
			}

			Person p = Person.generateInstanceFromXML(wn2);
			
			if (p != null) {
				retVal.addPersonWithoutId(p);
			}
		}

		MekHQApp.logMessage("Load Personnel Nodes Complete!", 4);
	}

	private static void processTaskNodes(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Task Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("workItem")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Task nodes: "+wn2.getNodeName());

				continue;
			}

			WorkItem w = WorkItem.generateInstanceFromXML(wn2);
			
			if (w != null) {
				retVal.addWorkWithoutId(w);
			}
		}

		MekHQApp.logMessage("Load Task Nodes Complete!", 4);
	}

	private static void processTeamNodes(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Team Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("supportTeam")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Team nodes: "+wn2.getNodeName());

				continue;
			}

			SupportTeam t = SupportTeam.generateInstanceFromXML(wn2);
			
			if (t != null) {
				retVal.addTeamWithoutId(t);
			}
		}

		MekHQApp.logMessage("Load Team Nodes Complete!", 4);
	}

	private static void processUnitNodes(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Unit Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Unit nodes: "+wn2.getNodeName());

				continue;
			}

			Unit u = Unit.generateInstanceFromXML(wn2);
			
			if (u != null) {
				retVal.addUnit(u);
			}
		}

		MekHQApp.logMessage("Load Unit Nodes Complete!", 4);
	}

	private static void processPartNodes(Campaign retVal, Node wn) {
		MekHQApp.logMessage("Loading Part Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("part")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQApp.logMessage("Unknown node type not loaded in Part nodes: "+wn2.getNodeName());

				continue;
			}

			Part p = Part.generateInstanceFromXML(wn2);
			
			if (p != null)
				retVal.addPartWithoutId(p);
		}

		MekHQApp.logMessage("Load Part Nodes Complete!", 4);
	}

	/**
	 * Pulled out purely for encapsulation. Makes the code neater and easier to
	 * read.
	 * 
	 * @param retVal
	 *            The Campaign object that is being populated.
	 * @param wn
	 *            The XML node we're working from.
	 * @throws ParseException
	 * @throws DOMException
	 */
	private static void processInfoNode(Campaign retVal, Node wni)
			throws DOMException, ParseException {
		NodeList nl = wni.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
			int xc = wn.getNodeType();

			// If it's not an element, again, we're ignoring it.
			if (xc == Node.ELEMENT_NODE) {
				String xn = wn.getNodeName();

				// Yeah, long if/then clauses suck.
				// I really couldn't think of a significantly better way to
				// handle it.
				// They're all primitives anyway...
				if (xn.equalsIgnoreCase("calendar")) {
					SimpleDateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd hh:mm:ss");
					retVal.calendar = (GregorianCalendar) GregorianCalendar
							.getInstance();
					retVal.calendar.setTime(df
							.parse(wn.getTextContent().trim()));
				} else if (xn.equalsIgnoreCase("camoCategory")) {
					String val = wn.getTextContent().trim();

					if (val.equals("null"))
						retVal.camoCategory = null;
					else
						retVal.camoCategory = val;
				} else if (xn.equalsIgnoreCase("camoFileName")) {
					String val = wn.getTextContent().trim();

					if (val.equals("null"))
						retVal.camoFileName = null;
					else
						retVal.camoFileName = val;
				} else if (xn.equalsIgnoreCase("colorIndex")) {
					retVal.colorIndex = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("nameGen")) {					
					// First, get all the child nodes;
					NodeList nl2 = wn.getChildNodes();	
					for (int x2 = 0; x2 < nl2.getLength(); x2++) {
						Node wn2 = nl2.item(x2);
						if (wn2.getParentNode() != wn)
							continue;
						if (wn2.getNodeName().equalsIgnoreCase("faction")) {
							retVal.getRNG().setChosenFaction(wn2.getTextContent().trim());
						} else if (wn2.getNodeName().equalsIgnoreCase("percentFemale")) {
							retVal.getRNG().setPerentFemale(Integer.parseInt(wn2.getTextContent().trim()));
						}
					}
				} else if (xn.equalsIgnoreCase("currentReport")) {
					// First, get all the child nodes;
					NodeList nl2 = wn.getChildNodes();
					
					// Then, make sure the report is empty.  *just* in case.
					// ...That is, creating a new campaign throws in a date line for us...
					// So make sure it's cleared out.
					retVal.currentReport.clear(); 

					for (int x2 = 0; x2 < nl2.getLength(); x2++) {
						Node wn2 = nl2.item(x2);

						if (wn2.getParentNode() != wn)
							continue;

						if (wn2.getNodeName().equalsIgnoreCase("reportLine"))
							retVal.currentReport.add(wn2.getTextContent());
					}
				} else if (xn.equalsIgnoreCase("dateFormat")) {
					retVal.dateFormat = new SimpleDateFormat(wn
							.getTextContent().trim());
				} else if (xn.equalsIgnoreCase("faction")) {
					retVal.faction = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("ranks")) {
					retVal.ranks = new Ranks(Integer.parseInt(wn.getTextContent().trim()));
				} else if (xn.equalsIgnoreCase("gmMode")) {
					if (wn.getTextContent().trim().equals("true"))
						retVal.gmMode = true;
					else
						retVal.gmMode = false;
				} else if (xn.equalsIgnoreCase("lastPartId")) {
					retVal.lastPartId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastForceId")) {
					retVal.lastForceId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastPersonId")) {
					retVal.lastPersonId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastTaskId")) {
					retVal.lastTaskId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastTeamId")) {
					retVal.lastTeamId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastUnitId")) {
					retVal.lastUnitId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("name")) {
					String val = wn.getTextContent().trim();

					if (val.equals("null"))
						retVal.name = null;
					else
						retVal.name = val;
				} else if (xn.equalsIgnoreCase("overtime")) {
					if (wn.getTextContent().trim().equals("true"))
						retVal.overtime = true;
					else
						retVal.overtime = false;
				} else if (xn.equalsIgnoreCase("shortDateFormat")) {
					retVal.shortDateFormat = new SimpleDateFormat(wn
							.getTextContent().trim());
				}
			}
		}
	}

	public ArrayList<WorkItem> getAcquisitionsForUnit(int unitId) {
		ArrayList<WorkItem> acquire = new ArrayList<WorkItem>();
		for(WorkItem task : getTasksForUnit(unitId)) {
			if(task instanceof ReplacementItem && !((ReplacementItem)task).hasPart()) {
				acquire.add(task);
			}
		}
		return acquire;
	}
	
	/**
	 * Generate a new pilotPerson of the given type
	 * using whatever randomization options have been
	 * given in the CampaignOptions
	 * @param type
	 * @return
	 */
	public PilotPerson newPilotPerson(int type) {
		boolean isFemale = getRNG().isFemale();
		Pilot pilot = new Pilot(getRNG().generate(isFemale),4,5);
		PilotPerson person = new PilotPerson(pilot, type, ranks);
		if(isFemale) {
			person.setGender(Person.G_FEMALE);
		}
		//now lets get a random birthdate, such that the person
		//is age 13+4d6 by default
		//TODO: let user specify age distribution
		GregorianCalendar birthdate = (GregorianCalendar)getCalendar().clone();
		//lets set age to be 14 + 4d6 by default		
		birthdate.set(Calendar.YEAR, birthdate.get(Calendar.YEAR) - (13 + Compute.d6(4)));
		//choose a random day and month
		int randomDay = Compute.randomInt(365)+1;
		if(birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
			randomDay = Compute.randomInt(366)+1;
		}
		birthdate.set(Calendar.DAY_OF_YEAR, randomDay);
		person.setBirthday(birthdate);
		return person;
	}
	
	public SupportPerson newTechPerson(int type) {
		boolean isFemale = getRNG().isFemale();
		TechTeam team = new TechTeam(getRNG().generate(isFemale), SupportTeam.EXP_REGULAR, type);
		SupportPerson person = new SupportPerson(team, ranks);
		if(isFemale) {
			person.setGender(Person.G_FEMALE);
		}
		//now lets get a random birthdate, such that the person
		//is age 13+4d6 by default
		//TODO: let user specify age distribution
		GregorianCalendar birthdate = (GregorianCalendar)getCalendar().clone();
		//lets set age to be 14 + 4d6 by default		
		birthdate.set(Calendar.YEAR, birthdate.get(Calendar.YEAR) - (13 + Compute.d6(4)));
		//choose a random day and month
		int randomDay = Compute.randomInt(365)+1;
		if(birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
			randomDay = Compute.randomInt(366)+1;
		}
		birthdate.set(Calendar.DAY_OF_YEAR, randomDay);
		person.setBirthday(birthdate);
		return person;
	}
	
	public SupportPerson newDoctorPerson() {
		boolean isFemale = getRNG().isFemale();
		MedicalTeam team = new MedicalTeam(getRNG().generate(isFemale), SupportTeam.EXP_REGULAR);
		SupportPerson person = new SupportPerson(team, ranks);
		if(isFemale) {
			person.setGender(Person.G_FEMALE);
		}
		//now lets get a random birthdate, such that the person
		//is age 13+4d6 by default
		//TODO: let user specify age distribution
		GregorianCalendar birthdate = (GregorianCalendar)getCalendar().clone();	
		birthdate.set(Calendar.YEAR, birthdate.get(Calendar.YEAR) - (13 + Compute.d6(4)));
		//choose a random day and month
		int randomDay = Compute.randomInt(365)+1;
		if(birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
			randomDay = Compute.randomInt(366)+1;
		}
		birthdate.set(Calendar.DAY_OF_YEAR, randomDay);
		person.setBirthday(birthdate);
		return person;
	}
	
	public Ranks getRanks() {
		return ranks;
	}
	
	public void setRankSystem(int system) {
		getRanks().setRankSystem(system);
		for(Person p : getPersonnel()) {
			p.setRank(0);
		}
	}
	
	public ArrayList<Force> getAllForces() {
		ArrayList<Force> allForces = new ArrayList<Force>();
		for(int x : forceIds.keySet()) {
			allForces.add(forceIds.get(x));
		}
		return allForces;
	}
	
	public Finances getFinances() {
		return finances;
	}
	
}
