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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.RandomNameGenerator;
import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.CargoBay;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.FighterSquadron;
import megamek.common.Game;
import megamek.common.GunEmplacement;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.VTOL;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.util.BuildingBlock;
import megamek.common.util.DirectoryItems;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingMASC;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IMedicalWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;
import mekhq.gui.PortraitFileFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Taharqa The main campaign class, keeps track of teams and units
 */
public class Campaign implements Serializable {
	private static final long serialVersionUID = -6312434701389973056L;

	// we have three things to track: (1) teams, (2) units, (3) repair tasks
	// we will use the same basic system (borrowed from MegaMek) for tracking
	// all three
	// OK now we have more, parts, personnel, forces, missions, and scenarios.
	// TODO: do we really need to track this in an array and hashtable?
	// It seems like we could track in a hashtable and then iterate through the
	// keys of the hash
	// to create an arraylist on demand
	private ArrayList<SupportTeam> teams = new ArrayList<SupportTeam>();
	private Hashtable<Integer, SupportTeam> teamIds = new Hashtable<Integer, SupportTeam>();
	private ArrayList<Unit> units = new ArrayList<Unit>();
	private Hashtable<UUID, Unit> unitIds = new Hashtable<UUID, Unit>();
	private ArrayList<Person> personnel = new ArrayList<Person>();
	private Hashtable<UUID, Person> personnelIds = new Hashtable<UUID, Person>();
	private ArrayList<Part> parts = new ArrayList<Part>();
	private Hashtable<Integer, Part> partIds = new Hashtable<Integer, Part>();
	private Hashtable<Integer, Force> forceIds = new Hashtable<Integer, Force>();
	private ArrayList<Mission> missions = new ArrayList<Mission>();
	private Hashtable<Integer, Mission> missionIds = new Hashtable<Integer, Mission>();
	private Hashtable<Integer, Scenario> scenarioIds = new Hashtable<Integer, Scenario>();
	private ArrayList<Kill> kills = new ArrayList<Kill>();

	private Hashtable<String, Integer> duplicateNameHash = new Hashtable<String, Integer>();

	private int astechPool;
	private int astechPoolMinutes;
	private int astechPoolOvertime;
	private int medicPool;

	private int lastTeamId;
	private int lastPartId;
	private int lastForceId;
	private int lastMissionId;
	private int lastScenarioId;

	// I need to put a basic game object in campaign so that I can
	// assign it to the entities, otherwise some entity methods may get NPE
	// if they try to call up game options
	private Game game;
	private Player player;

	private GameOptions gameOptions;

	private static String name;

	private RandomNameGenerator rng;

	// hierarchically structured Force object to define TO&E
	private Force forces;

	// calendar stuff
	public GregorianCalendar calendar;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat shortDateFormat;

	private String factionCode;
	private Ranks ranks;

	private ArrayList<String> currentReport;

	private boolean overtime;
	private boolean gmMode;

	private String camoCategory = Player.NO_CAMO;
	private String camoFileName = null;
	private int colorIndex = 0;

	private Finances finances;

	private CurrentLocation location;

	private PartsStore partsStore;

	private ArrayList<String> customs;

	private CampaignOptions campaignOptions = new CampaignOptions();
	private RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();

	private ShoppingList shoppingList;
	
	public Campaign() {
		game = new Game();
		player = new Player(0, "self");
		game.addPlayer(0, player);
		currentReport = new ArrayList<String>();
		calendar = new GregorianCalendar(3067, Calendar.JANUARY, 1);
		dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
		shortDateFormat = new SimpleDateFormat("yyyyMMdd");
		name = "My Campaign";
		rng = new RandomNameGenerator();
		rng.populateNames();
		overtime = false;
		gmMode = false;
		factionCode = "MERC";
		ranks = new Ranks();
		forces = new Force(name);
		forceIds.put(new Integer(lastForceId), forces);
		lastForceId++;
		finances = new Finances();
		location = new CurrentLocation(Planets.getInstance().getPlanets()
				.get("Outreach"), 0);
		SkillType.initializeTypes();
		astechPool = 0;
		medicPool = 0;
		resetAstechMinutes();
		partsStore = new PartsStore(this);
		gameOptions = new GameOptions();
		gameOptions.initialize();
		game.setOptions(gameOptions);
		customs = new ArrayList<String>();
		shoppingList = new ShoppingList();
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

	public String getCurrentPlanetName() {
		return location.getCurrentPlanet().getShortName();
	}

	public Planet getCurrentPlanet() {
		return location.getCurrentPlanet();
	}

	public long getFunds() {
		return finances.getBalance();
	}

	public Force getForces() {
		return forces;
	}

	public ShoppingList getShoppingList() {
		return shoppingList;
	}

	/**
	 * Add force to an existing superforce. This method will also assign the
	 * force an id and place it in the forceId hash
	 * 
	 * @param force
	 *            - the Force to add
	 * @param superForce
	 *            - the superforce to add the new force to
	 */
	public void addForce(Force force, Force superForce) {
		int id = lastForceId + 1;
		force.setId(id);
		superForce.addSubForce(force, true);
		force.setScenarioId(superForce.getScenarioId());
		forceIds.put(new Integer(id), force);
		lastForceId = id;

	}

	public void moveForce(Force force, Force superForce) {
		Force parentForce = force.getParentForce();
		if (null != parentForce) {
			parentForce.removeSubForce(force.getId());
		}
		superForce.addSubForce(force, true);
		force.setScenarioId(superForce.getScenarioId());
		for (Object o : force.getAllChildren(this)) {
			if (o instanceof Unit) {
				((Unit) o).setScenarioId(superForce.getScenarioId());
			} else if (o instanceof Force) {
				((Force) o).setScenarioId(superForce.getScenarioId());
			}
		}
	}

	/**
	 * This is used by the XML loader. The id should already be set for this
	 * force so dont increment
	 * 
	 * @param force
	 */
	public void addForceToHash(Force force) {
		forceIds.put(force.getId(), force);
	}

	/**
	 * This is used by the XML loader. The id should already be set for this
	 * scenario so dont increment
	 * 
	 * @param scenario
	 */
	public void addScenarioToHash(Scenario scenario) {
		scenarioIds.put(scenario.getId(), scenario);
	}

	/**
	 * Add unit to an existing force. This method will also assign that force's
	 * id to the unit.
	 * 
	 * @param u
	 * @param id
	 */
	public void addUnitToForce(Unit u, int id) {
		Force prevForce = forceIds.get(u.getForceId());
		if (null != prevForce) {
			prevForce.removeUnit(u.getId());
		}
		Force force = forceIds.get(id);
		if (null != force) {
			u.setForceId(id);
			force.addUnit(u.getId());
			u.setScenarioId(force.getScenarioId());
		}
	}

	/**
	 * Add a support team to the campaign
	 * 
	 * @param t
	 *            The support team to be added
	 */
	public void addTeam(SupportTeam t) {
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
	 * Add a mission to the campaign
	 * 
	 * @param m
	 *            The mission to be added
	 */
	public int addMission(Mission m) {
		int id = lastMissionId + 1;
		m.setId(id);
		missions.add(m);
		missionIds.put(new Integer(id), m);
		lastMissionId = id;
		return id;
	}

	private void addMissionWithoutId(Mission m) {
		missions.add(m);
		missionIds.put(new Integer(m.getId()), m);

		if (m.getId() > lastMissionId)
			lastMissionId = m.getId();
	}

	/**
	 * @return an <code>ArrayList</code> of missions in the campaign
	 */
	public ArrayList<Mission> getMissions() {
		return missions;
	}

	/**
	 * Add scenario to an existing mission. This method will also assign the
	 * scenario an id and place it in the scenarioId hash
	 * 
	 * @param s
	 *            - the Scenario to add
	 * @param m
	 *            - the mission to add the new scenario to
	 */
	public void addScenario(Scenario s, Mission m) {
		int id = lastScenarioId + 1;
		s.setId(id);
		m.addScenario(s);
		scenarioIds.put(new Integer(id), s);
		lastScenarioId = id;

	}

	/**
	 * 
	 * @return missions arraylist sorted with complete missions at the bottom
	 */
	public ArrayList<Mission> getSortedMissions() {
		ArrayList<Mission> msns = new ArrayList<Mission>();
		for (Mission m : getMissions()) {
			msns.add(m);
		}
		Collections.sort(msns, new Comparator<Mission>() {
			public int compare(final Mission m1, final Mission m2) {
				return ((Comparable<Boolean>) m2.isActive()).compareTo(m1
						.isActive());
			}
		});
		return msns;
	}

	/**
	 * @param id
	 *            the <code>int</code> id of the team
	 * @return a <code>SupportTeam</code> object
	 */
	public Mission getMission(int id) {
		return missionIds.get(new Integer(id));
	}

	public Scenario getScenario(int id) {
		return scenarioIds.get(new Integer(id));
	}

	public CurrentLocation getLocation() {
		return location;
	}

	private void addUnit(Unit u) {
		MekHQ.logMessage("Adding unit: (" + u.getId() + "):" + u, 5);
		units.add(u);
		unitIds.put(u.getId(), u);
		checkDuplicateNamesDuringAdd(u.getEntity());

		// Assign an entity ID to our new unit
		if (Entity.NONE == u.getEntity().getId()) {
			u.getEntity().setId(game.getNextEntityId());
		}
		game.addEntity(u.getEntity().getId(), u.getEntity());
	}

	/**
	 * Add a unit to the campaign. This is only for new units
	 * 
	 * @param en
	 *            An <code>Entity</code> object that the new unit will be
	 *            wrapped around
	 */
	public void addUnit(Entity en, boolean allowNewPilots, int days) {
		// reset the game object
		en.setOwner(player);
		en.setGame(game);

		UUID id = UUID.randomUUID();
		// check for the very rare chance of getting same id
		while (null != unitIds.get(id)) {
			id = UUID.randomUUID();
		}
		en.setExternalIdAsString(id.toString());
		Unit unit = new Unit(en, this);
		unit.setId(id);
		units.add(unit);
		unitIds.put(id, unit);
		removeUnitFromForce(unit); // Added to avoid the 'default force bug'
									// when calculating cargo

		unit.initializeParts(true);
		unit.runDiagnostic();
		if (!unit.isRepairable()) {
			unit.setSalvage(true);
		}
		unit.setDaysToArrival(days);

		if (allowNewPilots) {
			Utilities.generateRandomCrewWithCombinedSkill(unit, this);
		}
		unit.resetPilotAndEntity();

		// Assign an entity ID to our new unit
		if (Entity.NONE == en.getId()) {
			en.setId(game.getNextEntityId());
		}
		game.addEntity(en.getId(), en);

		checkDuplicateNamesDuringAdd(en);
		addReport(unit.getName() + " has been added to the unit roster.");
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

	public Unit getUnit(UUID id) {
		if (null == id) {
			return null;
		}
		return unitIds.get(id);
	}

	public boolean makePrisoner(Person p) {
		return recruitPerson(p, true, false);
	}

	public boolean makeBondsman(Person p) {
		return recruitPerson(p, false, true);
	}

	public boolean recruitPerson(Person p) {
		return recruitPerson(p, false, false);
	}

	public boolean recruitPerson(Person p, boolean prisoner, boolean bondsman) {
		if (prisoner && bondsman) {
			addReport("<font color='red'><b>Cannot have someone who is both a prisoner and a bondsman, there is an error in the code.</b></font>");
			return false;
		}
		if (p == null) {
			return false;
		}
		// Only pay if option set and this isn't a prisoner or bondsman
		if (getCampaignOptions().payForRecruitment() && !(prisoner || bondsman)) {
			if (!getFinances().debit(2 * p.getSalary(), Transaction.C_SALARY,
					"recruitment of " + p.getName(), getCalendar().getTime())) {
				addReport("<font color='red'><b>Insufficient funds to recruit "
						+ p.getName() + "</b></font>");
				return false;
			}
		}
		UUID id = UUID.randomUUID();
		while (null != personnelIds.get(id)) {
			id = UUID.randomUUID();
		}
		p.setId(id);
		personnel.add(p);
		personnelIds.put(id, p);
		addReport(p.getName() + " has been added to the personnel roster.");
		if (p.getPrimaryRole() == Person.T_ASTECH) {
			astechPoolMinutes += 480;
			astechPoolOvertime += 240;
		}
		if (p.getSecondaryRole() == Person.T_ASTECH) {
			astechPoolMinutes += 240;
			astechPoolOvertime += 120;
		}
		String rankEntry = "";
		if (p.getRank() > 0) {
			rankEntry = " as a " + getRanks().getRank(p.getRank());
		}
		if (prisoner) {
			p.setPrisoner();
			p.addLogEntry(getDate(), "Made Prisoner " + getName() + rankEntry);
		} else if (bondsman) {
			p.setBondsman();
			p.addLogEntry(getDate(), "Made Bondsman " + getName() + rankEntry);
		} else {
			p.setFreeMan();
			p.addLogEntry(getDate(), "Joined " + getName() + rankEntry);
		}
		return true;
	}

	private void addPersonWithoutId(Person p) {
		personnel.add(p);
		personnelIds.put(p.getId(), p);
	}

	public void addPersonWithoutId(Person p, boolean log) {
		while (null != personnelIds.get(p.getId())) {
			p.setId(UUID.randomUUID());
		}
		p.setRankSystem(ranks);
		addPersonWithoutId(p);
		if (log) {
			addReport(p.getName() + " has been added to the personnel roster.");
		}
		if (p.getPrimaryRole() == Person.T_ASTECH) {
			astechPoolMinutes += 480;
			astechPoolOvertime += 240;
		}
		if (p.getSecondaryRole() == Person.T_ASTECH) {
			astechPoolMinutes += 240;
			astechPoolOvertime += 120;
		}
		String rankEntry = "";
		if (p.getRank() > 0) {
			rankEntry = " as a " + getRanks().getRank(p.getRank());
		}
		p.addLogEntry(getDate(), "Joined " + getName() + rankEntry);
	}

	public Date getDate() {
		return calendar.getTime();
	}

	public ArrayList<Person> getPersonnel() {
		return personnel;
	}

	public ArrayList<Person> getPatients() {
		ArrayList<Person> patients = new ArrayList<Person>();
		for (Person p : getPersonnel()) {
			if (p.needsFixing()
					|| (getCampaignOptions().useAdvancedMedical()
							&& p.hasInjuries(true) && p.isActive())) {
				patients.add(p);
			}
		}
		return patients;
	}

	public ArrayList<Unit> getServiceableUnits() {
		ArrayList<Unit> service = new ArrayList<Unit>();
		for (Unit u : getUnits()) {
			if (!u.isAvailable()) {
				continue;
			}
			if (u.isSalvage() || !u.isRepairable()) {
				if (u.getSalvageableParts().size() > 0) {
					service.add(u);
				}
			} else {
				if (u.getPartsNeedingFixing().size() > 0) {
					service.add(u);
				}
			}
		}
		return service;
	}

	public Person getPerson(UUID id) {
		if (null == id) {
			return null;
		}
		return personnelIds.get(id);
	}

	public void addPart(Part p, int transitDays) {
		p.setDaysToArrival(transitDays);
		p.setBrandNew(false);
		// dont add missing parts if they dont have units or units with not id
		if (p instanceof MissingPart
				&& (null == p.getUnit() || null == p.getUnitId())) {
			return;
		}
		Part spare = checkForExistingSparePart(p);
		if (null == p.getUnit() && null != spare) {
			if (p instanceof Armor) {
				if (spare instanceof Armor) {
					((Armor) spare).setAmount(((Armor) spare).getAmount()
							+ ((Armor) p).getAmount());
					updateAllArmorForNewSpares();
					return;
				}
			}
			if (p instanceof ProtomekArmor) {
				if (spare instanceof ProtomekArmor) {
					((ProtomekArmor) spare).setAmount(((ProtomekArmor) spare)
							.getAmount() + ((ProtomekArmor) p).getAmount());
					updateAllArmorForNewSpares();
					return;
				}
			}
			if (p instanceof BaArmor) {
				if (spare instanceof BaArmor) {
					((BaArmor) spare).setAmount(((BaArmor) spare).getAmount()
							+ ((BaArmor) p).getAmount());
					updateAllArmorForNewSpares();
					return;
				}
			} else if (p instanceof AmmoStorage) {
				if (spare instanceof AmmoStorage) {
					((AmmoStorage) spare).changeShots(((AmmoStorage) p)
							.getShots());
					return;
				}
			} else {
				spare.incrementQuantity();
				return;
			}
		}
		int id = lastPartId + 1;
		p.setId(id);
		parts.add(p);
		partIds.put(new Integer(id), p);
		lastPartId = id;
		if (p instanceof Armor || p instanceof ProtomekArmor
				|| p instanceof BaArmor) {
			updateAllArmorForNewSpares();
		}
	}

	/**
	 * This is similar to addPart, but we just check to see if this part can be
	 * added to an existing part, without actually adding it to the campaign
	 * (because its already there). Should be called up when a part goes from 1
	 * daysToArrival to zero.
	 * 
	 * @param p
	 */
	public void arrivePart(Part p) {
		if (null != p.getUnit()) {
			return;
		}
		p.setDaysToArrival(0);
		addReport(p.getArrivalReport());
		int quantity = p.getQuantity();
		Part spare = checkForExistingSparePart(p);
		if (null != spare) {
			if (p instanceof Armor) {
				if (spare instanceof Armor) {
					while (quantity > 0) {
						((Armor) spare).setAmount(((Armor) spare).getAmount()
								+ ((Armor) p).getAmount());
						quantity--;
					}
					updateAllArmorForNewSpares();
					removePart(p);
				}
			}
			if (p instanceof ProtomekArmor) {
				if (spare instanceof ProtomekArmor) {
					while (quantity > 0) {
						((ProtomekArmor) spare)
								.setAmount(((ProtomekArmor) spare).getAmount()
										+ ((ProtomekArmor) p).getAmount());
						quantity--;
					}
					updateAllArmorForNewSpares();
					return;
				}
			}
			if (p instanceof BaArmor) {
				if (spare instanceof BaArmor) {
					while (quantity > 0) {
						((BaArmor) spare).setAmount(((BaArmor) spare)
								.getAmount() + ((BaArmor) p).getAmount());
						quantity--;
					}
					updateAllArmorForNewSpares();
					return;
				}
			} else if (p instanceof AmmoStorage) {
				if (spare instanceof AmmoStorage) {
					while (quantity > 0) {
						((AmmoStorage) spare).changeShots(((AmmoStorage) p)
								.getShots());
						quantity--;
					}
					removePart(p);
				}
			} else {
				while (quantity > 0) {
					spare.incrementQuantity();
					quantity--;
				}
				removePart(p);
			}
		} else if (p instanceof Armor || p instanceof ProtomekArmor
				|| p instanceof BaArmor) {
			updateAllArmorForNewSpares();
		}
	}

	/**
	 * call this whenever armor spare parts are changed so that armor knows
	 * whether it gets partial repairs or not
	 */
	public void updateAllArmorForNewSpares() {
		for (Part part : getParts()) {
			if (part instanceof Armor || part instanceof ProtomekArmor
					|| part instanceof BaArmor) {
				if (null != part.getUnit() && part.needsFixing()) {
					part.updateConditionFromEntity();
				}
			}
		}
	}

	public void addPartWithoutId(Part p) {
		if (p instanceof MissingPart && null == p.getUnitId()) {
			// we shouldn't have spare missing parts. I think their existence is
			// a relic.
			return;
		}
		// go ahead and check for existing parts because some version weren't
		// properly collecting parts
		if (!(p instanceof MissingPart)) {
			Part spare = checkForExistingSparePart(p);
			if (null == p.getUnitId() && null != spare) {
				if (p instanceof Armor) {
					if (spare instanceof Armor) {
						((Armor) spare).setAmount(((Armor) spare).getAmount()
								+ ((Armor) p).getAmount());
						updateAllArmorForNewSpares();
						return;
					}
				}
				if (p instanceof ProtomekArmor) {
					if (spare instanceof ProtomekArmor) {
						((ProtomekArmor) spare)
								.setAmount(((ProtomekArmor) spare).getAmount()
										+ ((ProtomekArmor) p).getAmount());
						updateAllArmorForNewSpares();
						return;
					}
				}
				if (p instanceof BaArmor) {
					if (spare instanceof BaArmor) {
						((BaArmor) spare).setAmount(((BaArmor) spare)
								.getAmount() + ((BaArmor) p).getAmount());
						updateAllArmorForNewSpares();
						return;
					}
				} else if (p instanceof AmmoStorage) {
					if (spare instanceof AmmoStorage) {
						((AmmoStorage) spare).changeShots(((AmmoStorage) p)
								.getShots());
						return;
					}
				} else {
					spare.incrementQuantity();
					return;
				}
			}
		}
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
		// lets do the report backwards
		for (String s : currentReport) {
			toReturn += s + "<br/>";
		}
		return toReturn;
	}

	public ArrayList<Person> getTechs() {
		ArrayList<Person> techs = new ArrayList<Person>();
		for (Person p : personnel) {
			if (p.isTech() && p.isActive()) {
				techs.add(p);
			}
		}
		return techs;
	}

	public boolean isWorkingOnRefit(Person p) {
		for (Unit u : units) {
			if (u.isRefitting()) {
				if (null != u.getRefit().getAssignedTeamId()
						&& u.getRefit().getAssignedTeamId().equals(p.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	public ArrayList<Person> getDoctors() {
		ArrayList<Person> docs = new ArrayList<Person>();
		for (Person p : personnel) {
			if (p.isDoctor() && p.isActive()) {
				docs.add(p);
			}
		}
		return docs;
	}

	public int getPatientsFor(Person doctor) {
		int patients = 0;
		for (Person person : getPersonnel()) {
			if (null != person.getAssignedTeamId()
					&& person.getAssignedTeamId().equals(doctor.getId())) {
				patients++;
			}
		}
		return patients;
	}

	/**
	 * return an html report on this unit. This will go in MekInfo
	 * 
	 * @param unitId
	 * @return
	 */
	/*
	public String getUnitDesc(UUID unitId) {
		Unit unit = getUnit(unitId);
		String toReturn = "<html><font size='2'";
		if (unit.isDeployed()) {
			toReturn += " color='white'";
		}
		toReturn += ">";
		toReturn += unit.getDescHTML();
		int totalMin = 0;
		int total = 0;
		// int cost = unit.getRepairCost();

		if (total > 0) {
			toReturn += "Total tasks: " + total + " (" + totalMin
					+ " minutes)<br/>";
		}
		/*
		 * if (cost > 0) { NumberFormat numberFormat =
		 * DecimalFormat.getIntegerInstance(); String text =
		 * numberFormat.format(cost) + " " + (cost != 0 ? "CBills" : "CBill");
		 * toReturn += "Repair cost : " + text + "<br/>"; }
		 */
/*
		toReturn += "</font>";
		toReturn += "</html>";
		return toReturn;
	}
*/

	public String healPerson(Person medWork, Person doctor) {
		String report = "";
		if (getCampaignOptions().useAdvancedMedical()) {
			return advancedMedicalHealPerson(medWork, doctor);
		}
		report += doctor.getName() + " attempts to heal "
				+ medWork.getPatientName();
		TargetRoll target = getTargetFor(medWork, doctor);
		int roll = Compute.d6(2);
		report = report + ",  needs " + target.getValueAsString()
				+ " and rolls " + roll + ":";
		int xpGained = 0;
		if (roll >= target.getValue()) {
			report = report + medWork.succeed();
			Unit u = getUnit(medWork.getUnitId());
			if (null != u) {
				u.resetPilotAndEntity();
			}
			if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
				xpGained += getCampaignOptions().getSuccessXP();
			}
			if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
				doctor.setNTasks(doctor.getNTasks() + 1);
			}
			if (doctor.getNTasks() >= getCampaignOptions().getNTasksXP()) {
				xpGained += getCampaignOptions().getTaskXP();
				doctor.setNTasks(0);
			}
		} else {
			report = report + medWork.fail(0);
			if (roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
				xpGained += getCampaignOptions().getMistakeXP();
			}
		}
		if (xpGained > 0) {
			doctor.setXp(doctor.getXp() + xpGained);
			report += " (" + xpGained + "XP gained) ";
		}
		medWork.setDaysToWaitForHealing(getCampaignOptions()
				.getHealingWaitingPeriod());
		return report;
	}

	public String advancedMedicalHealPerson(Person medWork, Person doctor) {
		Skill skill = doctor.getSkill(SkillType.S_DOCTOR);
		int level = skill.getLevel();
		int roll = Compute.randomInt(100);
		int fumble;
		int critSuccess;
		int xpGained = 0;
		String report = "";
		String eol = System.getProperty("line.separator");

		switch (level) {
		case 0:
			fumble = 50;
			critSuccess = 98;
			break;
		case 1:
			fumble = 40;
			critSuccess = 97;
			break;
		case 2:
			fumble = 30;
			critSuccess = 94;
			break;
		case 3:
			fumble = 20;
			critSuccess = 89;
			break;
		case 4:
			fumble = 12;
			critSuccess = 84;
			break;
		case 5:
			fumble = 6;
			critSuccess = 79;
			break;
		case 6:
			fumble = 5;
			critSuccess = 74;
			break;
		case 7:
			fumble = 4;
			critSuccess = 69;
			break;
		case 8:
			fumble = 3;
			critSuccess = 64;
			break;
		case 9:
			fumble = 2;
			critSuccess = 59;
			break;
		case 10:
			fumble = 1;
			critSuccess = 49;
			break;
		default: // defalt is same as 0
			fumble = 50;
			critSuccess = 98;
			break;
		}

		for (Injury injury : medWork.getInjuries()) {
			if (!injury.getWorkedOn()) {
				if (roll < fumble) {
					injury.setTime((int) Math.max(
							Math.ceil(injury.getTime() * 1.2),
							injury.getTime() + 5));
					report = report + doctor.getName()
							+ " made a mistake in the treatment of "
							+ medWork.getName() + " and caused "
							+ medWork.getGenderPronoun(Person.PRONOUN_HISHER)
							+ " " + injury.getName() + " to worsen.";
					if (Compute.randomInt(100) < (fumble / 4)) {
						// TODO: Add in special handling of the critical
						// injuries like broken back (make perm),
						// broken ribs (punctured lung/death chance) internal
						// bleeding (death chance)
					}
					if (roll < Math.max(1, fumble / 10)) {
						xpGained += getCampaignOptions().getMistakeXP();
					}
				} else if (roll > critSuccess) {
					injury.setTime((int) Math.floor(injury.getTime() * 90 / 100));
					report = report + doctor.getName()
							+ " performed some amazing work in treating "
							+ medWork.getName() + "'s " + injury.getName()
							+ " (10% less time to heal)";
					if (roll > Math.min(98,
							99 - Math.round(99 - critSuccess) / 10)) {
						xpGained += getCampaignOptions().getSuccessXP();
					}
				} else {
					if (doctor.getNTasks() >= getCampaignOptions()
							.getNTasksXP()) {
						xpGained += getCampaignOptions().getTaskXP();
						doctor.setNTasks(0);
					}
					doctor.setNTasks(doctor.getNTasks() + 1);
					report = report + doctor.getName()
							+ " successfully treated " + medWork.getName();
				}
				injury.setWorkedOn(true);
				Unit u = getUnit(medWork.getUnitId());
				if (null != u) {
					u.resetPilotAndEntity();
				}
			} else {
				report = report + medWork.getName()
						+ " spent time resting to heal "
						+ medWork.getGenderPronoun(Person.PRONOUN_HISHER) + " "
						+ injury.getName() + "!";
			}
			if (xpGained > 0) {
				doctor.setXp(doctor.getXp() + xpGained);
				report += " (" + xpGained + "XP gained)";
			}
			report += eol;
		}
		medWork.AMheal();
		return report;
	}

	public TargetRoll getTargetFor(IMedicalWork medWork, Person doctor) {
		Skill skill = doctor.getSkill(SkillType.S_DOCTOR);
		if (null == skill) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getName()
					+ " isn't a doctor, he just plays one on TV.");
		}
		if (medWork.getAssignedTeamId() != null
				&& !medWork.getAssignedTeamId().equals(doctor.getId())) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					medWork.getPatientName()
							+ " is already being tended by another doctor");
		}
		if (!medWork.needsFixing()
				&& !(getCampaignOptions().useAdvancedMedical() && medWork
						.needsAMFixing())) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					medWork.getPatientName() + " does not require healing.");
		}
		if (getPatientsFor(doctor) > 25) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, doctor.getName()
					+ " already has 25 patients.");
		}
		TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
				SkillType.getExperienceLevelName(skill.getExperienceLevel()));
		if (target.getValue() == TargetRoll.IMPOSSIBLE) {
			return target;
		}
		// understaffed mods
		int helpMod = getShorthandedMod(getMedicsPerDoctor(), true);
		if (helpMod > 0) {
			target.addModifier(helpMod, "shorthanded");
		}
		target.append(medWork.getAllMods());
		return target;
	}

	public Person getLogisticsPerson() {
		int bestSkill = -1;
		Person admin = null;
		String skill = getCampaignOptions().getAcquisitionSkill();
		if (skill.equals(CampaignOptions.S_AUTO)) {
			return admin;
		} else if (skill.equals(CampaignOptions.S_TECH)) {
			for (Person p : personnel) {
				if (getCampaignOptions().isAcquisitionSupportStaffOnly()
						&& !p.isSupport()) {
					continue;
				}
				if (p.isActive() && null != p.getBestTechSkill()
						&& p.getBestTechSkill().getLevel() > bestSkill) {
					admin = p;
					bestSkill = p.getBestTechSkill().getLevel();
				}
			}
		} else {
			for (Person p : personnel) {
				if (getCampaignOptions().isAcquisitionSupportStaffOnly()
						&& !p.isSupport()) {
					continue;
				}
				if (p.isActive() && p.hasSkill(skill)
						&& p.getSkill(skill).getLevel() > bestSkill) {
					admin = p;
					bestSkill = p.getSkill(skill).getLevel();
				}
			}
		}
		return admin;
	}

	public boolean acquireEquipment(IAcquisitionWork acquisition, Person person) {
		boolean found = false;
		String report = "";
		TargetRoll target = getTargetForAcquisition(acquisition, person, false);
		if (target.getValue() == TargetRoll.IMPOSSIBLE) {
			addReport(target.getDesc());
			return false;
		}
		if (null != person) {
			report += person.getName() + " ";
		}
		report += "attempts to find " + acquisition.getAcquisitionName();
		int roll = Compute.d6(2);
		report += "  needs " + target.getValueAsString();
		report += " and rolls " + roll + ":";
		int mos = roll - target.getValue();
		if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
			mos = roll - 2;
		}
		int xpGained = 0;
		if (roll >= target.getValue()) {
			int transitDays = calculatePartTransitTime(mos);
			report = report + acquisition.find(transitDays);
			found = true;
			if (person != null) {
				if (roll == 12
						&& target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
					xpGained += getCampaignOptions().getSuccessXP();
				}
				if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
					person.setNTasks(person.getNTasks() + 1);
				}
				if (person.getNTasks() >= getCampaignOptions().getNTasksXP()) {
					xpGained += getCampaignOptions().getTaskXP();
					person.setNTasks(0);
				}
			}
		} else {
			report = report + acquisition.failToFind();
			if (person != null && roll == 2
					&& target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
				xpGained += getCampaignOptions().getMistakeXP();
			}
		}
		if (xpGained > 0) {
			person.setXp(person.getXp() + xpGained);
			report += " (" + xpGained + "XP gained) ";
		}
		addReport(report);
		return found;
	}

	public void mothball(Unit u) {
	    Person tech = u.getTech();
	    if(null == tech) {
	        //uh-oh
	        //TODO: report someting
	        addReport("No tech assigned to the mothballing of " + u.getName());
	        return;
	    }
	    //don't allow overtime minutes for mothballing because its cheating
	    //since you dont roll
	    int minutes = Math.min(tech.getMinutesLeft(), u.getMothballTime());
	    //check astech time
	    if(!u.isSelfCrewed() && astechPoolMinutes < minutes * 6) {
	        //uh-oh
	        addReport("Not enough astechs to work on mothballing of " + u.getName());
	        return;
	    }
	    u.setMothballTime(u.getMothballTime() - minutes);
	    String action = " mothballing ";
	    if(u.isMothballed()) {
	        action = " activating ";
	    }
	    String report = tech.getFullTitle() + " spent " + minutes + " minutes" + action + u.getName();
	    if(!u.isMothballing()) {
	        if(u.isMothballed()) {
	            u.setMothballed(false);
	            report += ". Activation complete.";
	        } else {
	            u.setMothballed(true);
	            report += ". Mothballing complete.";
	        }
	    } else {
	        report += ". " + u.getMothballTime() + " minutes remaining.";
	    }
	    tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
	    if(!u.isSelfCrewed()) {
	        astechPoolMinutes -= 6 * minutes;
	    }
	    addReport(report);	    
	}
	
	public void refit(Refit r) {
		Person tech = getPerson(r.getAssignedTeamId());
		if (null == tech) {
			addReport("No tech is assigned to refit "
					+ r.getOriginalEntity().getShortName()
					+ ". Refit cancelled.");
			r.cancel();
			return;
		}
		TargetRoll target = getTargetFor(r, tech);
		// check that all parts have arrived
		if (!r.acquireParts()) {
			return;
		}
		String report = tech.getName() + " works on " + r.getPartName();
		int minutes = r.getTimeLeft();
		if (minutes > tech.getMinutesLeft()) {
			r.addTimeSpent(tech.getMinutesLeft());
			tech.setMinutesLeft(0);
			report = report + ", " + r.getTimeLeft() + " minutes left.";
		} else {
			tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
			r.addTimeSpent(minutes);
			if (r.hasFailedCheck()) {
				report = report + ", " + r.succeed();
			} else {
				int roll;
				String wrongType = "";
				if (tech.isRightTechTypeFor(r)) {
					roll = Compute.d6(2);
				} else {
					roll = Utilities.roll3d6();
					wrongType = " <b>Warning: wrong tech type for this refit.</b>";
				}
				report = report + ",  needs " + target.getValueAsString()
						+ " and rolls " + roll + ": ";
				if (roll >= target.getValue()) {
					report += r.succeed();
				} else {
					report += r.fail(SkillType.EXP_GREEN);
					// try to refit again in case the tech has any time left
					refit(r);
				}
				report += wrongType;
			}
		}
		addReport(report);
	}

	public void fixPart(IPartWork partWork, Person tech) {
		partWork.setTeamId(tech.getId());
		TargetRoll target = getTargetFor(partWork, tech);
		partWork.setTeamId(null);
		String report = "";
		String action = " fix ";
		// TODO: this should really be a method on the part
		if (partWork instanceof AmmoBin) {
			action = " reload ";
		}
		if (partWork.isSalvaging()) {
			action = " salvage ";
		}
		if (partWork instanceof MissingPart) {
			action = " replace ";
		}
		if (partWork instanceof MekLocation) {
			if (((MekLocation) partWork).isBlownOff()) {
				action = " re-attach ";
			} else if (((MekLocation) partWork).isBreached()) {
				action = " seal ";
			}
		}
		if ((partWork instanceof Armor) && !partWork.isSalvaging()) {
			if (!((Armor) partWork).isInSupply()) {
				report += "<b>Not enough armor remaining.  Task suspended.</b>";
				addReport(report);
				return;
			}
		}
		if ((partWork instanceof ProtomekArmor) && !partWork.isSalvaging()) {
			if (!((ProtomekArmor) partWork).isInSupply()) {
				report += "<b>Not enough Protomech armor remaining.  Task suspended.</b>";
				addReport(report);
				return;
			}
		}
		if ((partWork instanceof BaArmor) && !partWork.isSalvaging()) {
			if (!((BaArmor) partWork).isInSupply()) {
				report += "<b>Not enough BA armor remaining.  Task suspended.</b>";
				addReport(report);
				return;
			}
		}
		report += tech.getName() + " attempts to" + action
				+ partWork.getPartName();
		int minutes = partWork.getTimeLeft();
		int minutesUsed = minutes;
		boolean usedOvertime = false;
		if (minutes > tech.getMinutesLeft()) {
			minutes -= tech.getMinutesLeft();
			// check for overtime first
			if (isOvertimeAllowed() && minutes <= tech.getOvertimeLeft()) {
				// we are working overtime
				usedOvertime = true;
				tech.setMinutesLeft(0);
				tech.setOvertimeLeft(tech.getOvertimeLeft() - minutes);
			} else {
				// we need to finish the task tomorrow
				minutesUsed = tech.getMinutesLeft();
				if (isOvertimeAllowed()) {
					minutesUsed += tech.getOvertimeLeft();
					partWork.setWorkedOvertime(true);
					usedOvertime = false;
				}
				partWork.addTimeSpent(minutesUsed);
				tech.setMinutesLeft(0);
				tech.setOvertimeLeft(0);
				int helpMod = getShorthandedMod(
						getAvailableAstechs(minutesUsed, usedOvertime), false);
				if (null != partWork.getUnit()
						&& (partWork.getUnit().getEntity() instanceof Dropship || partWork
								.getUnit().getEntity() instanceof Jumpship)) {
					helpMod = 0;
				}
				if (partWork.getShorthandedMod() < helpMod) {
					partWork.setShorthandedMod(helpMod);
				}
				partWork.setTeamId(tech.getId());
				report += " - <b>Not enough time, the remainder of the task will be finished tomorrow.</b>";
				addReport(report);
				return;
			}
		} else {
			tech.setMinutesLeft(tech.getMinutesLeft() - minutes);
		}
		int astechMinutesUsed = minutesUsed
				* getAvailableAstechs(minutesUsed, usedOvertime);
		if (astechPoolMinutes < astechMinutesUsed) {
			astechMinutesUsed -= astechPoolMinutes;
			astechPoolMinutes = 0;
			astechPoolOvertime -= astechMinutesUsed;
		} else {
			astechPoolMinutes -= astechMinutesUsed;
		}
		// check for the type
		int roll;
		String wrongType = "";
		if (tech.isRightTechTypeFor(partWork)) {
			roll = Compute.d6(2);
		} else {
			roll = Utilities.roll3d6();
			wrongType = " <b>Warning: wrong tech type for this repair.</b>";
		}
		report = report + ",  needs " + target.getValueAsString()
				+ " and rolls " + roll + ":";
		int xpGained = 0;
		if (roll >= target.getValue()) {
			report = report + partWork.succeed();
			if (roll == 12 && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
				xpGained += getCampaignOptions().getSuccessXP();
			}
			if (target.getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
				tech.setNTasks(tech.getNTasks() + 1);
			}
			if (tech.getNTasks() >= getCampaignOptions().getNTasksXP()) {
				xpGained += getCampaignOptions().getTaskXP();
				tech.setNTasks(0);
			}
		} else {
			int modePenalty = Modes.getModeExperienceReduction(partWork
					.getMode());
			int effectiveSkillLvl = tech.getSkillForWorkingOn(partWork)
					.getExperienceLevel() - modePenalty;
			if (getCampaignOptions().isDestroyByMargin()) {
				if (getCampaignOptions().getDestroyMargin() > (target
						.getValue() - roll)) {
					// not destroyed - set the effective level as low as
					// possible
					effectiveSkillLvl = SkillType.EXP_ULTRA_GREEN;
				} else {
					// destroyed - set the effective level to elite
					effectiveSkillLvl = SkillType.EXP_ELITE;
				}
			}
			report = report + partWork.fail(effectiveSkillLvl);

			if (roll == 2 && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
				xpGained += getCampaignOptions().getMistakeXP();
			}
		}
		if (xpGained > 0) {
			tech.setXp(tech.getXp() + xpGained);
			report += " (" + xpGained + "XP gained) ";
		}
		report += wrongType;
		partWork.resetTimeSpent();
		partWork.resetOvertime();
		partWork.setTeamId(null);
		partWork.resetRepairStatus();
		addReport(report);
	}

	public void newDay() {
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		addReport("<p/><b>" + getDateAsString() + "</b>");

		location.newDay(this);

		for (Person p : getPersonnel()) {
			if (!p.isActive()) {
				continue;
			}
			p.resetMinutesLeft();
			if (p.needsFixing()
					|| (getCampaignOptions().useAdvancedMedical() && p
							.needsAMFixing())) {
				p.decrementDaysToWaitForHealing();
				Person doctor = getPerson(p.getDoctorId());
				if (null != doctor && doctor.isDoctor()) {
					if (p.getDaysToWaitForHealing() <= 0) {
						addReport(healPerson(p, doctor));
					}
				} else if (p.checkNaturalHealing(15)) {
					addReport(p.getDesc() + " heals naturally!");
					Unit u = getUnit(p.getUnitId());
					if (null != u) {
						u.resetPilotAndEntity();
					}
				} else if (getCampaignOptions().useAdvancedMedical()
						&& p.needsAMFixing() && doctor == null) {
					for (Injury injury : p.getInjuries()) {
						// We didn't get treated by a doctor... oops!
						if (!injury.getWorkedOn()) {
							if (!injury.getExtended()) {
								injury.setExtended(true);
								injury.setTime(Math.round(injury.getTime()
										* (1 + ((Compute.randomInt(15) + 35) / 100))));
								// We need to set the original time to the
								// extended time for purposes of seeing if it
								// becomes permanent
								injury.setOriginalTime(injury.getTime());
							}
							// The longer you wait to get this checked out, the
							// more likely it is to become permanent.
							/*
							 * if (Compute.randomInt(100) <
							 * (injury.getOriginalTime() - injury.getTime())) {
							 * 
							 * }
							 */
						}
						addReport(p.getName() + " spent time resting to heal "
								+ p.getGenderPronoun(Person.PRONOUN_HISHER)
								+ " " + injury.getName() + "!");
					}
					p.AMheal();
					Unit u = getUnit(p.getUnitId());
					if (null != u) {
						u.resetPilotAndEntity();
					}
				}
			}
			if (getCampaignOptions().getIdleXP() > 0
					&& calendar.get(Calendar.DAY_OF_MONTH) == 1 && p.isActive()) {
				p.setIdleMonths(p.getIdleMonths() + 1);
				if (p.getIdleMonths() >= getCampaignOptions().getMonthsIdleXP()) {
					if (Compute.d6(2) >= getCampaignOptions().getTargetIdleXP()) {
						p.setXp(p.getXp() + getCampaignOptions().getIdleXP());
						addReport(p.getFullTitle() + " has gained "
								+ getCampaignOptions().getIdleXP() + " XP");
					}
					p.setIdleMonths(0);
				}
			}

		}
		resetAstechMinutes();

		shoppingList.newDay(this);

		//need to loop through units twice, the first time to do all maintenance and the second
		//time to do whatever else. Otherwise, maintenance minutes might get sucked up by other
		//stuff
		for (Unit u : getUnits()) {
			if (null != u.getEngineer()) {
				u.getEngineer().resetMinutesLeft();
			}
			
			// do maintenance checks
            doMaintenance(u);
			
		}
		
		for (Unit u : getUnits()) {
			if (u.isRefitting()) {
				refit(u.getRefit());
			}
			if(u.isMothballing()) {
			    mothball(u);
			}
			if (!u.isPresent()) {
                u.checkArrival();
			}		
		}

		// need to check for assigned tasks in two steps to avoid
		// concurrent mod problems
		ArrayList<Integer> assignedPartIds = new ArrayList<Integer>();
		ArrayList<Integer> arrivedPartIds = new ArrayList<Integer>();
		for (Part part : getParts()) {
			if (part.getAssignedTeamId() != null) {
				assignedPartIds.add(part.getId());
			}
			if (part.checkArrival()) {
				arrivedPartIds.add(part.getId());
			}
		}
		for (int pid : arrivedPartIds) {
			Part part = getPart(pid);
			if (null != part) {
				arrivePart(part);
			}
		}
		for (int pid : assignedPartIds) {
			Part part = getPart(pid);
			if (null != part) {
				Person tech = getPerson(part.getAssignedTeamId());
				if (null != part.getUnit()
						&& (part.getUnit().getEntity() instanceof SmallCraft || part
								.getUnit().getEntity() instanceof Jumpship)) {
					tech = part.getUnit().getEngineer();
				}
				if (null != tech) {
					fixPart(part, tech);
				}
				// check to see if this part can now be combined with other
				// spare parts
				if (part.isSpare()) {
					Part spare = checkForExistingSparePart(part);
					if (null != spare) {
						spare.incrementQuantity();
						removePart(part);
					}
				}
			}
		}

		DecimalFormat formatter = new DecimalFormat();
		// check for a new year
		if (calendar.get(Calendar.MONTH) == 0
				&& calendar.get(Calendar.DAY_OF_MONTH) == 1) {
			// clear the ledger
			finances.newFiscalYear(calendar.getTime());
		}
		/*
		 * Now that we have maintenance checks in for real, we are going to pay maintenance
		 * on individual units when they come up for their maintenance checks and apply a +1 penalty
		 * if the cash is not there
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
			// maintenance costs
			if (campaignOptions.payForMaintain()) {
				if (finances.debit(getMaintenanceCosts(),
						Transaction.C_MAINTAIN, "Weekly Maintenance",
						calendar.getTime())) {
					addReport("Your account has been debited for "
							+ formatter.format(getMaintenanceCosts())
							+ " C-bills in maintenance costs");
				} else {
					addReport("<font color='red'><b>You cannot afford to pay maintenance costs!</b></font> Units will make their next maintenance check at a disadvantage.");
				}
			}
		}*/
		if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
			// check for contract payments
			for (Contract contract : getActiveContracts()) {
				finances.credit(contract.getMonthlyPayOut(),
						Transaction.C_CONTRACT, "Monthly payment for "
								+ contract.getName(), calendar.getTime());
				addReport("Your account has been credited for "
						+ formatter.format(contract.getMonthlyPayOut())
						+ " C-bills for the monthly payout from contract "
						+ contract.getName());
			}
			// Payday!
			if (campaignOptions.payForSalaries()) {
				if (finances.debit(getPayRoll(), Transaction.C_SALARY,
						"Monthly salaries", calendar.getTime())) {
					addReport("Payday! Your account has been debited for "
							+ formatter.format(getPayRoll())
							+ " C-bills in personnel salaries");
				} else {
					addReport("<font color='red'><b>You cannot afford to pay payroll costs!</b></font> Lucky for you that personnel morale is not yet implemented.");
				}
			}
			if (campaignOptions.payForOverhead()) {
				if (finances.debit(getOverheadExpenses(),
						Transaction.C_OVERHEAD, "Monthly overhead",
						calendar.getTime())) {
					addReport("Your account has been debited for "
							+ formatter.format(getOverheadExpenses())
							+ " C-bills in overhead expenses");
				} else {
					addReport("<font color='red'><b>You cannot afford to pay overhead costs!</b></font> Lucky for you that this does not appear to have any effect.");
				}
			}
		}
		// check for anything else in finances
		finances.newDay(this);
	}

	private ArrayList<Contract> getActiveContracts() {
		ArrayList<Contract> active = new ArrayList<Contract>();
		for (Mission m : getMissions()) {
			if (!(m instanceof Contract)) {
				continue;
			}
			Contract c = (Contract) m;
			if (c.isActive()
					&& !getCalendar().getTime().after(c.getEndingDate())
					&& !getCalendar().getTime().before(c.getStartDate())) {
				active.add(c);
			}
		}
		return active;
	}

	public Person getFlaggedCommander() {
		for (Person p : getPersonnel()) {
			if (p.isCommander()) {
				return p;
			}
		}
		return null;
	}

	public long getPayRoll() {
		long salaries = 0;
		for (Person p : personnel) {
			if (p.isActive() && !p.isDependent()
					&& !(p.isPrisoner() || p.isBondsman())) {
				salaries += p.getSalary();
			}
		}
		// add in astechs from the astech pool
		// we will assume vee mechanic * able-bodied * enlisted
		// 640 * 0.5 * 0.6 = 192
		salaries += (192 * astechPool);
		salaries += (320 * medicPool);
		return salaries;
	}

	public long getSupportPayRoll() {
		long salaries = 0;
		for (Person p : personnel) {
			if (p.isActive() && p.isSupport()) {
				salaries += p.getSalary();
			}
		}
		return salaries;
	}

	public long getMaintenanceCosts() {
		long costs = 0;
		for (Unit u : units) {
			if (u.requiresMaintenance() && null != u.getTech()) {
				costs += u.getMaintenanceCost();
			}
		}
		return costs;
	}

	public long getOverheadExpenses() {
		return (long) (getPayRoll() * 0.05);
	}

	public void clearAllUnits() {
		this.units = new ArrayList<Unit>();
		this.unitIds = new Hashtable<UUID, Unit>();
		// TODO: clear parts associated with unit

	}

	public void removeUnit(UUID id) {
		Unit unit = getUnit(id);

		// remove all parts for this unit as well
		for (Part p : unit.getParts()) {
			removePart(p);
		}

		// remove any personnel from this unit
		for (Person p : unit.getCrew()) {
			unit.remove(p, true);
		}

		Person tech = unit.getTech();
		if (null != tech) {
			unit.remove(tech, true);
		}

		// remove unit from any forces
		removeUnitFromForce(unit);

		// finally remove the unit
		units.remove(unit);
		unitIds.remove(unit.getId());
		checkDuplicateNamesDuringDelete(unit.getEntity());
		addReport(unit.getName() + " has been removed from the unit roster.");

	}

	public void removePerson(UUID id) {
		Person person = getPerson(id);

		Unit u = getUnit(person.getUnitId());
		if (null != u) {
			u.remove(person, true);
		}
		removeAllPatientsFor(person);

		addReport(person.getDesc()
				+ " has been removed from the personnel roster.");
		personnel.remove(person);
		personnelIds.remove(id);
		if (person.getPrimaryRole() == Person.T_ASTECH) {
			astechPoolMinutes = Math.max(0, astechPoolMinutes - 480);
			astechPoolOvertime = Math.max(0, astechPoolOvertime - 240);
		}
		if (person.getSecondaryRole() == Person.T_ASTECH) {
			astechPoolMinutes = Math.max(0, astechPoolMinutes - 240);
			astechPoolOvertime = Math.max(0, astechPoolOvertime - 120);
		}
	}

	public void removeAllPatientsFor(Person doctor) {
		for (Person p : personnel) {
			if (null != p.getAssignedTeamId()
					&& p.getAssignedTeamId().equals(doctor.getId())) {
				p.setDoctorId(null, getCampaignOptions()
						.getNaturalHealingWaitingPeriod());
			}
		}
	}

	public void removeScenario(int id) {
		Scenario scenario = getScenario(id);
		scenario.clearAllForcesAndPersonnel(this);
		Mission mission = getMission(scenario.getMissionId());
		if (null != mission) {
			mission.removeScenario(scenario.getId());
		}
		scenarioIds.remove(new Integer(id));
	}

	public void removeMission(int id) {
		Mission mission = getMission(id);
		int idx = 0;
		// Loop through scenarios here! We need to remove them as well.
		if (null != mission) {
			for (Scenario scenario : mission.getScenarios()) {
				scenario.clearAllForcesAndPersonnel(this);
				scenarioIds.remove(scenario.getId());
			}
			mission.clearScenarios();
		}
		idx = 0;
		boolean mfound = false;
		for (Mission m : getMissions()) {
			if (m.getId() == id) {
				mfound = true;
				break;
			}
			idx++;
		}
		if (mfound) {
			missions.remove(idx);
		}
		missionIds.remove(new Integer(id));
	}

	public void removePart(Part part) {
		parts.remove(part);
		partIds.remove(new Integer(part.getId()));
	}

	public void removeKill(Kill k) {
		kills.remove(k);
	}

	public void removeForce(Force force) {
		int fid = force.getId();
		forceIds.remove(new Integer(fid));
		// clear forceIds of all personnel with this force
		for (UUID uid : force.getUnits()) {
			Unit u = getUnit(uid);
			if (null == u) {
				continue;
			}
			if (u.getForceId() == fid) {
				u.setForceId(-1);
				if (force.isDeployed()) {
					u.setScenarioId(-1);
				}
			}
		}
		// also remove this force's id from any scenarios
		if (force.isDeployed()) {
			Scenario s = getScenario(force.getScenarioId());
			s.removeForce(fid);
		}
		if (null != force.getParentForce()) {
			force.getParentForce().removeSubForce(fid);
		}
		ArrayList<Force> subs = new ArrayList<Force>();
		for (Force sub : force.getSubForces()) {
			subs.add(sub);
		}
		for (Force sub : subs) {
			removeForce(sub);
		}
	}

	public void removeUnitFromForce(Unit u) {
		Force force = getForce(u.getForceId());
		if (null != force) {
			force.removeUnit(u.getId());
			u.setForceId(Force.FORCE_NONE);
			u.setScenarioId(-1);
			if (u.getEntity().hasC3i()
					&& u.getEntity().calculateFreeC3Nodes() < 5) {
				Vector<Unit> removedUnits = new Vector<Unit>();
				removedUnits.add(u);
				removeUnitsFromNetwork(removedUnits);
				refreshNetworks();
			}
			if (u.getEntity().hasC3M()) {
				removeUnitsFromC3Master(u);
			}
			u.getEntity().setC3MasterIsUUIDAsString(null);
			u.getEntity().setC3Master(null, true);
		}
	}

	public Force getForceFor(Unit u) {
		return getForce(u.getForceId());
	}

	public Force getForceFor(Person p) {
		Unit u = getUnit(p.getUnitId());
		if (null == u) {
			return null;
		} else {
			return getForceFor(u);
		}
	}

	public String getDateAsString() {
		return dateFormat.format(calendar.getTime());
	}

	public String getShortDateAsString() {
		return shortDateFormat.format(calendar.getTime());
	}

	public void restore() {
		for (Part part : getParts()) {
			if (part instanceof EquipmentPart) {
				((EquipmentPart) part).restore();
			}
			if (part instanceof MissingEquipmentPart) {
				((MissingEquipmentPart) part).restore();
			}
		}

		for (Unit unit : getUnits()) {
			if (null != unit.getEntity()) {
				unit.getEntity().setOwner(player);
				unit.getEntity().setGame(game);
				unit.getEntity().restore();
			}
		}

		shoppingList.restore();
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

	public Faction getFaction() {
		return Faction.getFaction(factionCode);
	}

	public String getFactionName() {
		return getFaction().getFullName(getEra());
	}

	public void setFactionCode(String i) {
		this.factionCode = i;
	}

	public String getFactionCode() {
		return factionCode;
	}

	public void addReport(String r) {
		int maxLine = 150;
		while (currentReport.size() > maxLine) {
			currentReport.remove(currentReport.size() - 1);
		}
		currentReport.add(0, r);
	}

	public void addReports(ArrayList<String> reports) {
		for (String r : reports) {
			addReport(r);
		}
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

	public ArrayList<Part> getSpareParts() {
		ArrayList<Part> spares = new ArrayList<Part>();
		for (Part part : getParts()) {
			if (part.isSpare()) {
				spares.add(part);
			}
		}
		return spares;
	}

	public void addFunds(long quantity) {
		addFunds(quantity, "Rich Uncle", Transaction.C_MISC);
	}

	public void addFunds(long quantity, String description, int category) {
		if (description == null || description.isEmpty()) {
			description = "Rich Uncle";
		}
		if (category == -1) {
			category = Transaction.C_MISC;
		}
		finances.credit(quantity, category, description, calendar.getTime());
		NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
		String quantityString = numberFormat.format(quantity);
		addReport("Funds added : " + quantityString + " (" + description + ")");
	}

	public boolean hasEnoughFunds(long cost) {
		return getFunds() >= cost;
	}

	public boolean buyUnit(Entity en, int days) {
		long cost = new Unit(en, this).getBuyCost();
		if (campaignOptions.payForUnits()) {
			if (finances.debit(cost, Transaction.C_UNIT,
					"Purchased " + en.getShortName(), calendar.getTime())) {
				addUnit(en, false, days);
				return true;
			} else {
				return false;
			}
		} else {
			addUnit(en, false, days);
			return true;
		}
	}

	public void sellUnit(UUID id) {
		Unit unit = getUnit(id);
		long sellValue = unit.getSellValue();
		finances.credit(sellValue, Transaction.C_UNIT_SALE,
				"Sale of " + unit.getName(), calendar.getTime());
		removeUnit(id);
	}

	public void sellPart(Part part, int quantity) {
		if (part instanceof AmmoStorage) {
			sellAmmo((AmmoStorage) part, quantity);
			return;
		}
		long cost = part.getActualValue() * quantity;
		String plural = "";
		if (quantity > 1) {
			plural = "s";
		}
		finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + quantity
				+ " " + part.getName() + plural, calendar.getTime());
		while (quantity > 0 && part.getQuantity() > 0) {
			part.decrementQuantity();
			quantity--;
		}
	}

	public void sellAmmo(AmmoStorage ammo, int shots) {
		shots = Math.min(shots, ammo.getShots());
		boolean sellingAllAmmo = shots == ammo.getShots();
		long cost = (long) (ammo.getActualValue() * ((double) shots / ammo
				.getShots()));
		finances.credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + shots
				+ " " + ammo.getName(), calendar.getTime());
		if (sellingAllAmmo) {
			ammo.decrementQuantity();
		} else {
			ammo.changeShots(-1 * shots);
		}
	}

	public boolean buyPart(Part part, int transitDays) {
		return buyPart(part, 1, transitDays);
	}

	public boolean buyPart(Part part, double multiplier, int transitDays) {
		if (getCampaignOptions().payForParts()) {
			if (finances.debit((long) (multiplier * part.getActualValue()),
					Transaction.C_EQUIP, "Purchase of " + part.getName(),
					calendar.getTime())) {
				addPart(part, transitDays);
				return true;
			} else {
				return false;
			}
		} else {
			addPart(part, transitDays);
			return true;
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

		ResourceBundle resourceMap = ResourceBundle
				.getBundle("mekhq.resources.MekHQ");
		// Start the XML root.
		pw1.println("<campaign version=\""
				+ resourceMap.getString("Application.version") + "\">");

		// Basic Campaign Info
		pw1.println("\t<info>");

		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "name", name);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "faction", factionCode);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "ranks", ranks.getRankSystem());
		if (ranks.getRankSystem() == Ranks.RS_CUSTOM) {
			MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "officerCut",
					ranks.getOfficerCut());
			MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "rankNames",
					ranks.getRankNameList());
		}
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "nameGen",
				rng.getChosenFaction());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "percentFemale",
				rng.getPercentFemale());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "overtime", overtime);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "gmMode", gmMode);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPool", astechPool);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPoolMinutes",
				astechPoolMinutes);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "astechPoolOvertime",
				astechPoolOvertime);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "medicPool", medicPool);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoCategory", camoCategory);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "camoFileName", camoFileName);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "colorIndex", colorIndex);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastTeamId", lastTeamId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastPartId", lastPartId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastForceId", lastForceId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "lastMissionId", lastMissionId);
		MekHqXmlUtil
				.writeSimpleXmlTag(pw1, 2, "lastScenarioId", lastScenarioId);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, 2, "calendar",
				df.format(calendar.getTime()));
		{
			pw1.println("\t\t<nameGen>");
			pw1.print("\t\t\t<faction>");
			pw1.print(MekHqXmlUtil.escape(rng.getChosenFaction()));
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
		writeArrayAndHashToXmlforUUID(pw1, 1, "units", units, unitIds); // Units
		writeArrayAndHashToXmlforUUID(pw1, 1, "personnel", personnel,
				personnelIds); // Personnel
		writeArrayAndHashToXml(pw1, 1, "missions", missions, missionIds); // Missions
		// the forces structure is hierarchical, but that should be handled
		// internally
		// from with writeToXML function for Force
		pw1.println("\t<forces>");
		forces.writeToXml(pw1, 2);
		pw1.println("\t</forces>");
		finances.writeToXml(pw1, 1);
		location.writeToXml(pw1, 1);
		shoppingList.writeToXml(pw1, 1);
		pw1.println("\t<kills>");
		for (Kill k : kills) {
			k.writeToXml(pw1, 2);
		}
		pw1.println("\t</kills>");
		pw1.println("\t<skillTypes>");
		for (String name : SkillType.skillList) {
			SkillType type = SkillType.getType(name);
			if (null != type) {
				type.writeToXml(pw1, 2);
			}
		}
		SkillType.writeAbilityCostsToXML(pw1, 2);
		pw1.println("\t</skillTypes>");
		rskillPrefs.writeToXml(pw1, 1);
		// parts is the biggest so it goes last
		writeArrayAndHashToXml(pw1, 1, "parts", parts, partIds); // Parts

		writeGameOptions(pw1);

		writeCustoms(pw1);
		// Okay, we're done.
		// Close everything out and be done with it.
		pw1.println("</campaign>");
	}

	public void writeGameOptions(PrintWriter pw1) {
		pw1.println("\t<gameOptions>");
		for (IBasicOption option : getGameOptionsVector()) {
			pw1.println("\t\t<gameoption>"); //$NON-NLS-1$
			MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "name", option.getName());
			MekHqXmlUtil.writeSimpleXmlTag(pw1, 3, "value", option.getValue()
					.toString());
			pw1.println("\t\t</gameoption>"); //$NON-NLS-1$
		}
		pw1.println("\t</gameOptions>");
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
		for (Integer x : hashtab.keySet()) {
			((MekHqXmlSerializable) (hashtab.get(x))).writeToXml(pw1,
					indent + 1);
		}

		pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
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
	private <arrType> void writeArrayAndHashToXmlforUUID(PrintWriter pw1,
			int indent, String tag, ArrayList<arrType> array,
			Hashtable<UUID, arrType> hashtab) {
		// Hooray for implicitly-type-detected genericized functions!
		// However, I still ended up making an interface to handle this.
		// That way, I can cast it and call "writeToXml" to make it cleaner.
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

		// Enumeration<Integer> = hashtab.keys
		for (UUID x : hashtab.keySet()) {
			((MekHqXmlSerializable) (hashtab.get(x))).writeToXml(pw1,
					indent + 1);
		}

		pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
	}

	private void writeCustoms(PrintWriter pw1) {
		for (String name : customs) {
			MechSummary ms = MechSummaryCache.getInstance().getMech(name);
			if (ms == null)
				continue;

			MechFileParser mechFileParser = null;
			try {
				mechFileParser = new MechFileParser(ms.getSourceFile());
			} catch (EntityLoadingException ex) {
				Logger.getLogger(Campaign.class.getName()).log(Level.SEVERE,
						"MechFileParse exception : " + name, ex);
			}
			if (mechFileParser == null) {
				continue;
			}
			Entity en = mechFileParser.getEntity();
			pw1.println("\t<custom>");
			pw1.println("\t\t<name>" + name + "</name>");
			if (en instanceof Mech) {
				pw1.print("\t\t<mtf>");
				pw1.print(((Mech) en).getMtf());
				pw1.print("\t\t</mtf>\n");
			} else {
				pw1.print("\t\t<blk><![CDATA[");

				BuildingBlock blk = BLKFile.getBlock(en);
				for (String s : blk.getAllDataAsString()) {
					if (s.isEmpty()) {
						continue;
					}
					pw1.print(s + "\n");
				}
				pw1.print("]]>\n\t\t</blk>\n");
			}
			pw1.println("\t</custom>");
		}
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
	public static Campaign createCampaignFromXMLFileInputStream(
			FileInputStream fis) throws DOMException, ParseException,
			NullEntityException {
		MekHQ.logMessage("Starting load of campaign file from XML...");
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
			MekHQ.logError(ex);
		}

		Element campaignEle = xmlDoc.getDocumentElement();
		NodeList nl = campaignEle.getChildNodes();

		// Get rid of empty text nodes and adjacent text nodes...
		// Stupid weird parsing of XML. At least this cleans it up.
		campaignEle.normalize();

		Version version = new Version(campaignEle.getAttribute("version"));

		// we need to iterate through three times, the first time to collect
		// any custom units that might not be written yet
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

				if (xn.equalsIgnoreCase("info")) { // This is needed so that the campaign name gets set in retVal
					processInfoNode(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("custom")) {
					processCustom(retVal, wn);
				}
			} else {
				// If it's a text node or attribute or whatever at this level,
				// it's probably white-space.
				// We can safely ignore it even if it isn't, for now.
				continue;
			}
		}
		MechSummaryCache.getInstance().loadMechData();

		// the second time to check for any null entities
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

				if (xn.equalsIgnoreCase("units")) {
					String missingList = checkUnits(wn);
					if (null != missingList) {
						throw new NullEntityException(missingList);
					}
				}
			} else {
				// If it's a text node or attribute or whatever at this level,
				// it's probably white-space.
				// We can safely ignore it even if it isn't, for now.
				continue;
			}
		}

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
				} else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
					retVal.rskillPrefs = RandomSkillPreferences
							.generateRandomSkillPreferencesFromXml(wn);
				} else if (xn.equalsIgnoreCase("info")) {
					processInfoNode(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("parts")) {
					processPartNodes(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("personnel")) {
					processPersonnelNodes(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("teams")) {
					processTeamNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("units")) {
					processUnitNodes(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("missions")) {
					processMissionNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("forces")) {
					processForces(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("finances")) {
					processFinances(retVal, wn);
				} else if (xn.equalsIgnoreCase("location")) {
					retVal.location = CurrentLocation.generateInstanceFromXML(
							wn, retVal);
				} else if (xn.equalsIgnoreCase("skillTypes")) {
					processSkillTypeNodes(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("gameOptions")) {
					processGameOptionNodes(retVal, wn);
				} else if (xn.equalsIgnoreCase("kills")) {
					processKillNodes(retVal, wn, version);
				} else if (xn.equalsIgnoreCase("shoppingList")) {
					retVal.shoppingList = ShoppingList.generateInstanceFromXML(
							wn, retVal, version);
				}

			} else {
				// If it's a text node or attribute or whatever at this level,
				// it's probably white-space.
				// We can safely ignore it even if it isn't, for now.
				continue;
			}
		}

		// Okay, after we've gone through all the nodes and constructed the
		// Campaign object...
		// We need to do a post-process pass to restore a number of references.

		// if the version is earlier than 0.1.14, then we need to replace all
		// the old integer
		// ids of units and personnel with their UUIDs where they are
		// referenced.
		if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
				&& version.getSnapshot() < 14) {
			fixIdReferences(retVal);
		}
		// if the version is earlier than 0.1.16, then we need to run another
		// fix to update
		// the externalIds to match the Unit IDs.
		if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
				&& version.getSnapshot() < 16) {
			fixIdReferencesB(retVal);
		}

		// adjust tech levels for version before 0.1.21
		if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
				&& version.getSnapshot() < 21) {
			retVal.campaignOptions.setTechLevel(retVal.campaignOptions
					.getTechLevel() + 1);
		}

		// First, iterate through Support Teams;
		// they have a reference to the Campaign object.
		for (int x = 0; x < retVal.teams.size(); x++) {
			SupportTeam st = retVal.teams.get(x);

			// Okay, last trigger a reCalc.
			// This should fix some holes in the data.
			st.reCalc();
		}

		// loop through forces to set force id
		for (int fid : retVal.forceIds.keySet()) {
			Force f = retVal.forceIds.get(fid);
			Scenario s = retVal.getScenario(f.getScenarioId());
			if (null != s
					&& (null == f.getParentForce() || !f.getParentForce()
							.isDeployed())) {
				s.addForces(fid);
			}
			// some units may need force id set for backwards compatability
			// some may also need scenario id set
			for (UUID uid : f.getUnits()) {
				Unit u = retVal.getUnit(uid);
				if (null != u) {
					u.setForceId(f.getId());
					if (f.isDeployed()) {
						u.setScenarioId(f.getScenarioId());
					}
				}
			}
		}

		// Process parts...
		ArrayList<Part> spareParts = new ArrayList<Part>();
		ArrayList<Part> removeParts = new ArrayList<Part>();
		for (int x = 0; x < retVal.parts.size(); x++) {
			Part prt = retVal.parts.get(x);
			Unit u = retVal.getUnit(prt.getUnitId());
			prt.setUnit(u);
			if (null != u) {
				// get rid of any equipmentparts without locations or mounteds
				if (prt instanceof EquipmentPart) {
					Mounted m = u.getEntity().getEquipment(
							((EquipmentPart) prt).getEquipmentNum());
					if (null == m || m.getLocation() == Entity.LOC_NONE) {
						removeParts.add(prt);
					}
				}
				// if actuators on units have no location (on version 1.23 and
				// earlier) then remove them and let initializeParts (called
				// later) create new ones
				else if (prt instanceof MekActuator
						&& ((MekActuator) prt).getLocation() == Entity.LOC_NONE) {
					removeParts.add(prt);
				} else if (prt instanceof MissingMekActuator
						&& ((MissingMekActuator) prt).getLocation() == Entity.LOC_NONE) {
					removeParts.add(prt);
				} else {
					u.addPart(prt);
					if (prt instanceof AmmoBin) {
						((AmmoBin) prt).restoreMunitionType();
					}
				}

			} else if (version.getMajorVersion() == 0
					&& version.getMinorVersion() < 2
					&& version.getSnapshot() < 16) {
				prt.setSalvaging(false);
				// this seems weird but we need to get difficulty and time
				// updated for non-salvage
				prt.updateConditionFromEntity();
				boolean found = false;
				for (Part spare : spareParts) {
					if (spare.isSamePartTypeAndStatus(prt)) {
						spare.incrementQuantity();
						removeParts.add(prt);
						found = true;
						break;
					}
				}
				if (!found) {
					spareParts.add(prt);
				}
			}
			if (prt instanceof MissingPart) {
				// run this to make sure that slots for missing parts are set as
				// unrepairable
				// because they will not be in missing locations
				prt.updateConditionFromPart();
			}
			// old versions didnt distinguish tank engines
			if (prt instanceof EnginePart && prt.getName().contains("Vehicle")) {
				boolean isHover = null != u
						&& u.getEntity().getMovementMode() == EntityMovementMode.HOVER
						&& u.getEntity() instanceof Tank;
				((EnginePart) prt).fixTankFlag(isHover);
			}
			// clan flag might not have been properly set in early versions
			if (prt instanceof EnginePart && prt.getName().contains("(Clan")
					&& prt.getTechBase() != Part.T_CLAN) {
				((EnginePart) prt).fixClanFlag();
			}
			if (prt instanceof MissingEnginePart && null != u
					&& u.getEntity() instanceof Tank) {
				boolean isHover = null != u
						&& u.getEntity().getMovementMode() == EntityMovementMode.HOVER
						&& u.getEntity() instanceof Tank;
				((MissingEnginePart) prt).fixTankFlag(isHover);
			}
			if (prt instanceof MissingEnginePart
					&& prt.getName().contains("(Clan")
					&& prt.getTechBase() != Part.T_CLAN) {
				((MissingEnginePart) prt).fixClanFlag();
			}

		}
		for (Part prt : removeParts) {
			retVal.removePart(prt);
		}

		// All personnel need the rank reference fixed
		for (int x = 0; x < retVal.personnel.size(); x++) {
			Person psn = retVal.personnel.get(x);

			psn.setRankSystem(retVal.ranks);

			// skill types might need resetting
			psn.resetSkillTypes();

			// reverse compatability check for assigning support personnel
			// characteristics from their support team
			if (psn.getOldSupportTeamId() >= 0) {
				SupportTeam t = retVal.teamIds.get(psn.getOldSupportTeamId());
				psn.setName(t.getName());
				int lvl = 0;
				switch (t.getRating()) {
				case 0:
					lvl = 1;
					break;
				case 1:
					lvl = 3;
					break;
				case 2:
					lvl = 4;
					break;
				case 3:
					lvl = 5;
					break;
				}
				if (t instanceof TechTeam) {
					switch (((TechTeam) t).getType()) {
					case TechTeam.T_MECH:
						psn.setPrimaryRole(Person.T_MECH_TECH);
						psn.addSkill(SkillType.S_TECH_MECH, lvl, 0);
						break;
					case TechTeam.T_MECHANIC:
						psn.setPrimaryRole(Person.T_MECHANIC);
						psn.addSkill(SkillType.S_TECH_MECHANIC, lvl, 0);
						break;
					case TechTeam.T_AERO:
						psn.setPrimaryRole(Person.T_AERO_TECH);
						psn.addSkill(SkillType.S_TECH_AERO, lvl, 0);
						break;
					case TechTeam.T_BA:
						psn.setPrimaryRole(Person.T_BA_TECH);
						psn.addSkill(SkillType.S_TECH_BA, lvl, 0);
						break;
					}
				} else {
					psn.setPrimaryRole(Person.T_DOCTOR);
					psn.addSkill(SkillType.S_DOCTOR, lvl, 0);
				}
			}
		}

		// Okay, Units, need their pilot references fixed.
		for (int x = 0; x < retVal.units.size(); x++) {
			Unit unit = retVal.units.get(x);

			// Also, the unit should have its campaign set.
			unit.campaign = retVal;

			// reset the pilot and entity, to reflect newly assigned personnel
			unit.resetPilotAndEntity();

			if (null != unit.getRefit()) {
				unit.getRefit().reCalc();
				if (null == unit.getRefit().getNewArmorSupplies()
						&& unit.getRefit().getNewArmorSuppliesId() > 0) {
					unit.getRefit().setNewArmorSupplies(
							(Armor) retVal.getPart(unit.getRefit()
									.getNewArmorSuppliesId()));
				}
				if (!unit.getRefit().isCustomJob()
						&& !unit.getRefit().kitFound()) {
					retVal.shoppingList.addShoppingItemWithoutChecking(unit
							.getRefit());
				}
			}

			// lets make sure the force id set actually corresponds to a force
			// TODO: we have some reports of force id relics - need to fix
			if (unit.getForceId() > 0
					&& null == retVal.getForce(unit.getForceId())) {
				unit.setForceId(-1);
			}

			// Its annoying to have to do this, but this helps to ensure
			// that equipment numbers correspond to the right parts - its
			// possible that these might have changed if changes were made to
			// the
			// ordering of equipment in the underlying data file for the unit
			Utilities.unscrambleEquipmentNumbers(unit);

			// some units might need to be assigned to scenarios
			Scenario s = retVal.getScenario(unit.getScenarioId());
			if (null != s) {
				// most units will be properly assigned through their
				// force, so check to make sure they aren't already here
				if (!s.isAssigned(unit, retVal)) {
					s.addUnit(unit.getId());
				}
			}

			retVal.refreshNetworks();

		}

		// ok, once we are sure that campaign has been set for all units, we can
		// now go
		// through and initializeParts and run diagnostics
		for (int x = 0; x < retVal.units.size(); x++) {
			Unit unit = retVal.units.get(x);
			// just in case parts are missing (i.e. because they weren't tracked
			// in previous versions)
			unit.initializeParts(true);
			unit.runDiagnostic();
			if (!unit.isRepairable()) {
				if (unit.getSalvageableParts().isEmpty()) {
					// we shouldnt get here but some units seem to stick around
					// for some reason
					retVal.removeUnit(unit.getId());
				} else {
					unit.setSalvage(true);
				}
			}
		}

		MekHQ.logMessage("Load of campaign file complete!");

		return retVal;
	}

	private static void fixIdReferences(Campaign retVal) {
		// set up translation hashes
		Hashtable<Integer, UUID> uHash = new Hashtable<Integer, UUID>();
		Hashtable<Integer, UUID> pHash = new Hashtable<Integer, UUID>();
		for (Unit u : retVal.units) {
			uHash.put(u.getOldId(), u.getId());
		}
		for (Person p : retVal.personnel) {
			pHash.put(p.getOldId(), p.getId());
		}
		// ok now go through and fix
		for (Unit u : retVal.units) {
			u.fixIdReferences(uHash, pHash);
		}
		for (Person p : retVal.personnel) {
			p.fixIdReferences(uHash, pHash);
		}
		retVal.forces.fixIdReferences(uHash);
		for (Part p : retVal.parts) {
			p.fixIdReferences(uHash, pHash);
		}
		ArrayList<Kill> ghostKills = new ArrayList<Kill>();
		for (Kill k : retVal.kills) {
			k.fixIdReferences(pHash);
			if (null == k.getPilotId()) {
				ghostKills.add(k);
			}
		}
		// check for kills with missing person references
		for (Kill k : ghostKills) {
			if (null == k.getPilotId()) {
				retVal.removeKill(k);
			}
		}
	}

	private static void fixIdReferencesB(Campaign retVal) {
		for (Unit u : retVal.units) {
			Entity en = u.getEntity();
			UUID id = u.getId();
			en.setExternalIdAsString(id.toString());

			// If they have C3 or C3i we need to set their ID
			if (en.hasC3() || en.hasC3i()) {
				en.setC3UUID();
				en.setC3NetIdSelf();
			}
		}
	}

	private static void processFinances(Campaign retVal, Node wn) {
		MekHQ.logMessage("Loading Finances from XML...", 4);
		retVal.finances = Finances.generateInstanceFromXML(wn);
		MekHQ.logMessage("Load of Finances complete!");
	}

	private static void processForces(Campaign retVal, Node wn, Version version) {
		MekHQ.logMessage("Loading Force Organization from XML...", 4);

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
				MekHQ.logMessage("Unknown node type not loaded in Forces nodes: "
						+ wn2.getNodeName());

				continue;
			}

			if (!foundForceAlready) {
				Force f = Force.generateInstanceFromXML(wn2, retVal, version);
				if (null != f) {
					retVal.forces = f;
					foundForceAlready = true;
				}
			} else {
				MekHQ.logMessage("More than one type-level force found", 5);
			}
		}

		MekHQ.logMessage("Load of Force Organization complete!");
	}

	private static void processPersonnelNodes(Campaign retVal, Node wn,
			Version version) {
		MekHQ.logMessage("Loading Personnel Nodes from XML...", 4);

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
				MekHQ.logMessage("Unknown node type not loaded in Personnel nodes: "
						+ wn2.getNodeName());

				continue;
			}

			Person p = Person.generateInstanceFromXML(wn2, version);

			if (p != null) {
				retVal.addPersonWithoutId(p);
			}
		}

		MekHQ.logMessage("Load Personnel Nodes Complete!", 4);
	}

	private static void processSkillTypeNodes(Campaign retVal, Node wn,
			Version version) {
		MekHQ.logMessage("Loading Skill Type Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (wn2.getNodeName().startsWith("ability-")) {
				SkillType.readAbilityCostFromXML(wn2);
				continue;
			} else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Skill Type nodes: "
						+ wn2.getNodeName());

				continue;
			}

			SkillType.generateInstanceFromXML(wn2, version);
		}

		MekHQ.logMessage("Load Skill Type Nodes Complete!", 4);
	}

	private static void processKillNodes(Campaign retVal, Node wn,
			Version version) {
		MekHQ.logMessage("Loading Kill Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			else if (!wn2.getNodeName().equalsIgnoreCase("kill")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Kill nodes: "
						+ wn2.getNodeName());

				continue;
			}

			retVal.kills.add(Kill.generateInstanceFromXML(wn2, version));
		}

		MekHQ.logMessage("Load Kill Nodes Complete!", 4);
	}

	private static void processGameOptionNodes(Campaign retVal, Node wn) {
		MekHQ.logMessage("Loading GameOption Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			else if (!wn2.getNodeName().equalsIgnoreCase("gameoption")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Game Option nodes: "
						+ wn2.getNodeName());

				continue;
			}
			NodeList nl = wn2.getChildNodes();

			String name = null;
			Object value = null;
			for (int y = 0; y < nl.getLength(); y++) {
				Node wn3 = nl.item(y);
				if (wn3.getNodeName().equalsIgnoreCase("name")) {
					name = wn3.getTextContent();
				} else if (wn3.getNodeName().equalsIgnoreCase("value")) {
					value = wn3.getTextContent();
				}
			}
			if ((null != name) && (null != value)) {
				IOption option = retVal.gameOptions.getOption(name);
				if (null != option) {
					if (!option.getValue().toString().equals(value.toString())) {
						try {
							switch (option.getType()) {
							case IOption.STRING:
								option.setValue((String) value);
								break;

							case IOption.BOOLEAN:
								option.setValue(new Boolean(value.toString()));
								break;

							case IOption.INTEGER:
								option.setValue(new Integer(value.toString()));
								break;

							case IOption.FLOAT:
								option.setValue(new Float(value.toString()));
								break;
							}
						} catch (IllegalArgumentException iaEx) {
							MekHQ.logMessage("Error trying to load option '" + name + "' with a value of '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				} else {
					MekHQ.logMessage("Invalid option '" + name + "' when trying to load options file."); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		MekHQ.logMessage("Load Game Option Nodes Complete!", 4);
	}

	private static void processCustom(Campaign retVal, Node wn) {
		String sCustomsDir = "data" + File.separator + "mechfiles"
				+ File.separator + "customs";
		String sCustomsDirCampaign = sCustomsDir + File.separator + retVal.getName();
		File customsDir = new File(sCustomsDir);
		if (!customsDir.exists()) {
			customsDir.mkdir();
		}
		File customsDirCampaign = new File(sCustomsDirCampaign);
		if (!customsDirCampaign.exists()) {
			customsDirCampaign.mkdir();
		}

		NodeList wList = wn.getChildNodes();

		String name = null;
		String mtf = null;
		String blk = null;

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (wn2.getNodeName().equalsIgnoreCase("name")) {
				name = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("mtf")) {
				mtf = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("blk")) {
				blk = wn2.getTextContent();
			}
		}
		retVal.addCustom(name);
		if (null != name && null != mtf) {
			try {
				// if this file already exists then don't overwrite it or we
				// will end up with a bunch of copies
				String fileName = sCustomsDir + File.separator + name + ".mtf";
				String fileNameCampaign = sCustomsDirCampaign + File.separator
						+ name + ".mtf";
				if ((new File(fileName)).exists()
						|| (new File(fileNameCampaign)).exists()) {
					return;
				}
				MekHQ.logMessage("Loading Custom unit from XML...", 4);
				FileOutputStream out = new FileOutputStream(fileNameCampaign);
				PrintStream p = new PrintStream(out);
				p.println(mtf);
				p.close();
				out.close();
				MekHQ.logMessage("Loaded Custom Unit!", 4);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (null != name && null != blk) {
			try {
				// if this file already exists then don't overwrite it or we
				// will end up with a bunch of copies
				String fileName = sCustomsDir + File.separator + name + ".blk";
				String fileNameCampaign = sCustomsDirCampaign + File.separator
						+ name + ".blk";
				if ((new File(fileName)).exists()
						|| (new File(fileNameCampaign)).exists()) {
					return;
				}
				MekHQ.logMessage("Loading Custom unit from XML...", 4);
				FileOutputStream out = new FileOutputStream(fileNameCampaign);
				PrintStream p = new PrintStream(out);
				p.println(blk);
				p.close();
				out.close();
				MekHQ.logMessage("Loaded Custom Unit!", 4);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static void processMissionNodes(Campaign retVal, Node wn) {
		MekHQ.logMessage("Loading Mission Nodes from XML...", 4);

		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (!wn2.getNodeName().equalsIgnoreCase("mission")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Mission nodes: "
						+ wn2.getNodeName());

				continue;
			}

			Mission m = Mission.generateInstanceFromXML(wn2);

			if (m != null) {
				// add scenarios to the scenarioId hash
				for (Scenario s : m.getScenarios()) {
					retVal.addScenarioToHash(s);
				}
				retVal.addMissionWithoutId(m);
			}
		}

		MekHQ.logMessage("Load Mission Nodes Complete!", 4);
	}

	private static void processTeamNodes(Campaign retVal, Node wn) {
		MekHQ.logMessage("Loading Team Nodes from XML...", 4);

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
				MekHQ.logMessage("Unknown node type not loaded in Team nodes: "
						+ wn2.getNodeName());

				continue;
			}

			SupportTeam t = SupportTeam.generateInstanceFromXML(wn2);

			if (t != null) {
				retVal.addTeamWithoutId(t);
			}
		}

		MekHQ.logMessage("Load Team Nodes Complete!", 4);
	}

	private static String checkUnits(Node wn) {
		MekHQ.logMessage("Checking for missing entities...", 4);

		ArrayList<String> unitList = new ArrayList<String>();
		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
				continue;
			}

			NodeList nl = wn2.getChildNodes();

			for (int y = 0; y < nl.getLength(); y++) {
				Node wn3 = nl.item(y);
				try {
					if (wn3.getNodeName().equalsIgnoreCase("entity")) {
						if (null == MekHqXmlUtil.getEntityFromXmlString(wn3)) {
							String name = MekHqXmlUtil
									.getEntityNameFromXmlString(wn3);
							if (!unitList.contains(name)) {
								unitList.add(name);
							}
						}
					}
				} catch (Exception ex) {

				}
			}
		}
		MekHQ.logMessage("Finished checking for missing entities!", 4);

		if (unitList.isEmpty()) {
			return null;
		} else {
			String unitListString = "";
			for (String s : unitList) {
				unitListString += "\n" + s;
			}
			return unitListString;
		}
	}

	private static void processUnitNodes(Campaign retVal, Node wn,
			Version version) {
		MekHQ.logMessage("Loading Unit Nodes from XML...", 4);

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
				MekHQ.logMessage("Unknown node type not loaded in Unit nodes: "
						+ wn2.getNodeName());

				continue;
			}

			Unit u = Unit.generateInstanceFromXML(wn2, version);

			if (u != null) {
				retVal.addUnit(u);
			}
		}

		MekHQ.logMessage("Load Unit Nodes Complete!", 4);
	}

	private static void processPartNodes(Campaign retVal, Node wn,
			Version version) {
		MekHQ.logMessage("Loading Part Nodes from XML...", 4);

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
				MekHQ.logMessage("Unknown node type not loaded in Part nodes: "
						+ wn2.getNodeName());

				continue;
			}

			Part p = Part.generateInstanceFromXML(wn2, version);

			// deal with the Weapon as Heat Sink problem from earlier versions
			if (p instanceof HeatSink && !p.getName().contains("Heat Sink")) {
				continue;
			}

			// deal with equipmentparts that are now subtyped
			int pid = p.getId();
			if (p instanceof EquipmentPart
					&& ((EquipmentPart) p).getType() instanceof MiscType
					&& ((EquipmentPart) p).getType().hasFlag(MiscType.F_MASC)
					&& !(p instanceof MASC)) {
				p = new MASC(p.getUnitTonnage(), ((EquipmentPart) p).getType(),
						((EquipmentPart) p).getEquipmentNum(), retVal, 0);
				p.setId(pid);
			}
			if (p instanceof MissingEquipmentPart
					&& ((MissingEquipmentPart) p).getType().hasFlag(
							MiscType.F_MASC) && !(p instanceof MASC)) {
				p = new MissingMASC(p.getUnitTonnage(),
						((MissingEquipmentPart) p).getType(),
						((MissingEquipmentPart) p).getEquipmentNum(), retVal,
						((MissingEquipmentPart) p).getTonnage(), 0);
				p.setId(pid);
			}
			// deal with true values for sensor and life support on non-Mech
			// heads
			if (p instanceof MekLocation
					&& ((MekLocation) p).getLoc() != Mech.LOC_HEAD) {
				((MekLocation) p).setSensors(false);
				((MekLocation) p).setLifeSupport(false);
			}

			if (version.getMinorVersion() < 3 && !p.needsFixing()
					&& !p.isSalvaging()) {
				// repaired parts were not getting experience properly reset
				p.setSkillMin(SkillType.EXP_GREEN);
			}

			if (p != null) {
				p.setCampaign(retVal);
				retVal.addPartWithoutId(p);
			}
		}

		MekHQ.logMessage("Load Part Nodes Complete!", 4);
	}

	/**
	 * Pulled out purely for encapsulation. Makes the code neater and easier to
	 * read.
	 * 
	 * @param retVal
	 *            The Campaign object that is being populated.
	 * @param wni
	 *            The XML node we're working from.
	 * @throws ParseException
	 * @throws DOMException
	 */
	private static void processInfoNode(Campaign retVal, Node wni,
			Version version) throws DOMException, ParseException {
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
							retVal.getRNG().setChosenFaction(
									wn2.getTextContent().trim());
						} else if (wn2.getNodeName().equalsIgnoreCase(
								"percentFemale")) {
							retVal.getRNG().setPerentFemale(
									Integer.parseInt(wn2.getTextContent()
											.trim()));
						}
					}
				} else if (xn.equalsIgnoreCase("currentReport")) {
					// First, get all the child nodes;
					NodeList nl2 = wn.getChildNodes();

					// Then, make sure the report is empty. *just* in case.
					// ...That is, creating a new campaign throws in a date line
					// for us...
					// So make sure it's cleared out.
					retVal.currentReport.clear();

					for (int x2 = 0; x2 < nl2.getLength(); x2++) {
						Node wn2 = nl2.item(x2);

						if (wn2.getParentNode() != wn)
							continue;

						if (wn2.getNodeName().equalsIgnoreCase("reportLine"))
							retVal.currentReport.add(wn2.getTextContent());
					}
				} else if (xn.equalsIgnoreCase("faction")) {
					if (version.getMajorVersion() == 0
							&& version.getMinorVersion() < 2
							&& version.getSnapshot() < 14) {
						retVal.factionCode = Faction.getFactionCode(Integer
								.parseInt(wn.getTextContent()));
					} else {
						retVal.factionCode = wn.getTextContent();
					}
				} else if (xn.equalsIgnoreCase("ranks")) {
					int rankSystem = Integer.parseInt(wn.getTextContent()
							.trim());
					if (rankSystem != Ranks.RS_CUSTOM) {
						retVal.ranks = new Ranks(rankSystem);
					}
				} else if (xn.equalsIgnoreCase("officerCut")) {
					retVal.ranks.setOfficerCut(Integer.parseInt(wn
							.getTextContent().trim()));
				} else if (xn.equalsIgnoreCase("rankNames")) {
					retVal.ranks.setRanksFromList(wn.getTextContent().trim());
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
				} else if (xn.equalsIgnoreCase("lastTeamId")) {
					retVal.lastTeamId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastMissionId")) {
					retVal.lastMissionId = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("lastScenarioId")) {
					retVal.lastScenarioId = Integer.parseInt(wn
							.getTextContent().trim());
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
				} else if (xn.equalsIgnoreCase("astechPool")) {
					retVal.astechPool = Integer.parseInt(wn.getTextContent()
							.trim());
				} else if (xn.equalsIgnoreCase("astechPoolMinutes")) {
					retVal.astechPoolMinutes = Integer.parseInt(wn
							.getTextContent().trim());
				} else if (xn.equalsIgnoreCase("astechPoolOvertime")) {
					retVal.astechPoolOvertime = Integer.parseInt(wn
							.getTextContent().trim());
				} else if (xn.equalsIgnoreCase("medicPool")) {
					retVal.medicPool = Integer.parseInt(wn.getTextContent()
							.trim());
				}
			}
		}
	}

	public ArrayList<Planet> getPlanets() {
		ArrayList<Planet> plnts = new ArrayList<Planet>();
		for (String key : Planets.getInstance().getPlanets().keySet()) {
			plnts.add(Planets.getInstance().getPlanets().get(key));
		}
		return plnts;
	}

	public Vector<String> getPlanetNames() {
		Vector<String> plntNames = new Vector<String>();
		for (String key : Planets.getInstance().getPlanets().keySet()) {
			plntNames.add(key);
		}
		return plntNames;
	}

	public Planet getPlanet(String name) {
		return Planets.getInstance().getPlanets().get(name);
	}

	/**
	 * Generate a new pilotPerson of the given type using whatever randomization
	 * options have been given in the CampaignOptions
	 * 
	 * @param type
	 * @return
	 */
	public Person newPerson(int type) {
		boolean isFemale = getRNG().isFemale();
		Person person = new Person();
		if (isFemale) {
			person.setGender(Person.G_FEMALE);
		}
		person.setName(getRNG().generate(isFemale));
		int expLvl = Utilities.generateExpLevel(rskillPrefs
				.getOverallRecruitBonus() + rskillPrefs.getRecruitBonus(type));
		GregorianCalendar birthdate = (GregorianCalendar) getCalendar().clone();
		birthdate.set(
				Calendar.YEAR,
				birthdate.get(Calendar.YEAR)
						- Utilities.getAgeByExpLevel(expLvl));
		// choose a random day and month
		int randomDay = Compute.randomInt(365) + 1;
		if (birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
			randomDay = Compute.randomInt(366) + 1;
		}
		birthdate.set(Calendar.DAY_OF_YEAR, randomDay);
		person.setBirthday(birthdate);
		person.setPrimaryRole(type);
		if (getCampaignOptions().useDylansRandomXp()) {
			person.setXp(Utilities.generateRandomExp());
		}
		person.setDaysToWaitForHealing(getCampaignOptions()
				.getNaturalHealingWaitingPeriod());
		int bonus = 0;
		// set default skills
		switch (type) {
		case (Person.T_MECHWARRIOR):
			if (getFaction().isClan()) {
				bonus = 1;
			}
			person.addSkill(SkillType.S_PILOT_MECH, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_MECH, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_GVEE_DRIVER):
			person.addSkill(SkillType.S_PILOT_GVEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_VEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_NVEE_DRIVER):
			person.addSkill(SkillType.S_PILOT_NVEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_VEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_VTOL_PILOT):
			person.addSkill(SkillType.S_PILOT_VTOL, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_VEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_VEE_GUNNER):
			person.addSkill(SkillType.S_GUN_VEE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_CONV_PILOT):
			person.addSkill(SkillType.S_PILOT_JET, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_JET, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_AERO_PILOT):
			if (getFaction().isClan()) {
				bonus = 1;
			}
			person.addSkill(SkillType.S_PILOT_AERO, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_GUN_AERO, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_PROTO_PILOT):
			person.addSkill(SkillType.S_GUN_PROTO, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_BA):
			if (getFaction().isClan()) {
				bonus = 1;
			}
			person.addSkill(SkillType.S_GUN_BA, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_ANTI_MECH, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			person.addSkill(SkillType.S_SMALL_ARMS, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_INFANTRY):
			if (Utilities.rollProbability(rskillPrefs.getAntiMekProb())) {
				person.addSkill(SkillType.S_ANTI_MECH, expLvl,
						rskillPrefs.randomizeSkill(), bonus);
			}
			person.addSkill(SkillType.S_SMALL_ARMS, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_SPACE_PILOT):
			person.addSkill(SkillType.S_PILOT_SPACE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_SPACE_CREW):
			person.addSkill(SkillType.S_TECH_VESSEL, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_SPACE_GUNNER):
			person.addSkill(SkillType.S_GUN_SPACE, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_NAVIGATOR):
			person.addSkill(SkillType.S_NAV, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_MECH_TECH):
			person.addSkill(SkillType.S_TECH_MECH, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_MECHANIC):
			person.addSkill(SkillType.S_TECH_MECHANIC, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_AERO_TECH):
			person.addSkill(SkillType.S_TECH_AERO, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_BA_TECH):
			person.addSkill(SkillType.S_TECH_BA, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_ASTECH):
			person.addSkill(SkillType.S_ASTECH, 0, 0);
			break;
		case (Person.T_DOCTOR):
			person.addSkill(SkillType.S_DOCTOR, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		case (Person.T_MEDIC):
			person.addSkill(SkillType.S_MEDTECH, 0, 0);
			break;
		case (Person.T_ADMIN_COM):
		case (Person.T_ADMIN_LOG):
		case (Person.T_ADMIN_TRA):
		case (Person.T_ADMIN_HR):
			person.addSkill(SkillType.S_ADMIN, expLvl,
					rskillPrefs.randomizeSkill(), bonus);
			break;
		}
		// roll small arms skill
		if (!person.hasSkill(SkillType.S_SMALL_ARMS)) {
			int sarmsLvl = -12;
			if (person.isSupport()) {
				sarmsLvl = Utilities.generateExpLevel(rskillPrefs
						.getSupportSmallArmsBonus());
			} else {
				sarmsLvl = Utilities.generateExpLevel(rskillPrefs
						.getCombatSmallArmsBonus());
			}
			if (sarmsLvl > SkillType.EXP_ULTRA_GREEN) {
				person.addSkill(SkillType.S_SMALL_ARMS, sarmsLvl,
						rskillPrefs.randomizeSkill(), bonus);
			}

		}
		// roll tactics skill
		if (getCampaignOptions().useTactics() && !person.isSupport()) {
			int tacLvl = Utilities.generateExpLevel(rskillPrefs
					.getTacticsMod(expLvl));
			if (tacLvl > SkillType.EXP_ULTRA_GREEN) {
				person.addSkill(SkillType.S_TACTICS, tacLvl,
						rskillPrefs.randomizeSkill(), bonus);
			}
		}
		// roll artillery skill
		if (getCampaignOptions().useArtillery()
				&& (type == Person.T_MECHWARRIOR || type == Person.T_VEE_GUNNER || type == Person.T_INFANTRY)
				&& Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
			int artyLvl = Utilities.generateExpLevel(rskillPrefs
					.getArtilleryBonus());
			if (artyLvl > SkillType.EXP_ULTRA_GREEN) {
				person.addSkill(SkillType.S_ARTILLERY, artyLvl,
						rskillPrefs.randomizeSkill(), bonus);
			}
		}
		// roll random secondary skill
		if (Utilities.rollProbability(rskillPrefs.getSecondSkillProb())) {
			ArrayList<String> possibleSkills = new ArrayList<String>();
			for (String stype : SkillType.skillList) {
				if (!person.hasSkill(stype)) {
					possibleSkills.add(stype);
				}
			}
			String selSkill = possibleSkills.get(Compute
					.randomInt(possibleSkills.size()));
			int secondLvl = Utilities.generateExpLevel(rskillPrefs
					.getSecondSkillBonus());
			person.addSkill(selSkill, secondLvl, rskillPrefs.randomizeSkill(),
					bonus);
		}
		// TODO: roll special abilities
		if (getCampaignOptions().useAbilities()) {
			int nabil = Utilities.rollSpecialAbilities(rskillPrefs
					.getSpecialAbilBonus(expLvl));
			ArrayList<String> abilityList = person.getAvailableOptions();
			while (nabil > 0 && abilityList.size() > 0) {
				// create a weighted list
				ArrayList<String> weightedList = new ArrayList<String>();
				for (String s : abilityList) {
					int cost = SkillType.getAbilityCost(s);
					if (cost < 1) {
						cost = 100;
					}
					int weight = Math.max(1, 100 / cost);
					while (weight > 0) {
						weightedList.add(s);
						weight--;
					}

				}
				String name = weightedList.get(Compute.randomInt(weightedList
						.size()));
				if (name.equals("specialist")) {
					String special = Crew.SPECIAL_NONE;
					switch (Compute.randomInt(2)) {
					case 0:
						special = Crew.SPECIAL_LASER;
						break;
					case 1:
						special = Crew.SPECIAL_BALLISTIC;
						break;
					case 2:
						special = Crew.SPECIAL_MISSILE;
						break;
					}
					person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
							special);
				} else if (name.equals("weapon_specialist")) {
					person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
							Utilities.chooseWeaponSpecialization(type,
									getFaction().isClan(), getCampaignOptions()
											.getTechLevel(),
									getCalendar().get(GregorianCalendar.YEAR)));
				} else {
					person.acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
							true);
				}
				abilityList.remove(name);
				if (name.equals("specialist")) {
					abilityList.remove("weapon_specialist");
					abilityList.remove("gunnery_laser");
					abilityList.remove("gunnery_missile");
					abilityList.remove("gunnery_ballistic");

				}
				if (name.equals("weapon_specialist")) {
					abilityList.remove("specialist");
					abilityList.remove("gunnery_laser");
					abilityList.remove("gunnery_missile");
					abilityList.remove("gunnery_ballistic");
				}
				if (name.contains("gunnery_")) {
					abilityList.remove("weapon_specialist");
					abilityList.remove("specialist");
					abilityList.remove("gunnery_laser");
					abilityList.remove("gunnery_missile");
					abilityList.remove("gunnery_ballistic");
				}
				nabil--;
			}
		}
		person.setRankSystem(ranks);
		if (getCampaignOptions().usePortraitForType(type)) {
			assignRandomPortraitFor(person);
		}
		return person;
	}

	public Ranks getRanks() {
		return ranks;
	}

	public void setRankSystem(int system) {
		getRanks().setRankSystem(system);
		for (Person p : getPersonnel()) {
			p.setRank(0);
		}
	}

	public ArrayList<Force> getAllForces() {
		ArrayList<Force> allForces = new ArrayList<Force>();
		for (int x : forceIds.keySet()) {
			allForces.add(forceIds.get(x));
		}
		return allForces;
	}

	public Finances getFinances() {
		return finances;
	}

	public ArrayList<Part> getPartsNeedingServiceFor(UUID uid) {
		if (null == uid) {
			return new ArrayList<Part>();
		}
		Unit u = getUnit(uid);
		if (u != null) {
			if (u.isSalvage() || !u.isRepairable()) {
				return u.getSalvageableParts();
			} else {
				return u.getPartsNeedingFixing();
			}
		}
		return new ArrayList<Part>();
	}

	public ArrayList<IAcquisitionWork> getAcquisitionsForUnit(UUID uid) {
		if (null == uid) {
			return new ArrayList<IAcquisitionWork>();
		}
		Unit u = getUnit(uid);
		if (u != null) {
			return u.getPartsNeeded();
		}
		return new ArrayList<IAcquisitionWork>();
	}

	/**
	 * Use an A* algorithm to find the best path between two planets For right
	 * now, we are just going to minimze the number of jumps but we could extend
	 * this to take advantage of recharge information or other variables as well
	 * Based on http://www.policyalmanac.org/games/aStarTutorial.htm
	 * 
	 * @param startKey
	 * @param endKey
	 * @return
	 */
	public JumpPath calculateJumpPath(String startKey, String endKey) {

		if (startKey.equals(endKey)) {
			JumpPath jpath = new JumpPath();
			jpath.addPlanet(getPlanet(startKey));
			return jpath;
		}

		String current = startKey;
		ArrayList<String> closed = new ArrayList<String>();
		ArrayList<String> open = new ArrayList<String>();
		boolean found = false;
		int jumps = 0;

		Planet end = Planets.getInstance().getPlanets().get(endKey);

		// we are going to through and set up some hashes that will make our
		// work easier
		// hash of parent key
		Hashtable<String, String> parent = new Hashtable<String, String>();
		// hash of H for each planet which will not change
		Hashtable<String, Double> scoreH = new Hashtable<String, Double>();
		// hash of G for each planet which might change
		Hashtable<String, Integer> scoreG = new Hashtable<String, Integer>();

		for (String key : Planets.getInstance().getPlanets().keySet()) {
			scoreH.put(
					key,
					end.getDistanceTo(Planets.getInstance().getPlanets()
							.get(key)));
		}
		scoreG.put(current, 0);
		closed.add(current);

		while (!found && jumps < 10000) {
			jumps++;
			int currentG = scoreG.get(current) + 1;
			ArrayList<String> neighborKeys = getAllReachablePlanetsFrom(Planets
					.getInstance().getPlanets().get(current));
			for (String neighborKey : neighborKeys) {
				if (closed.contains(neighborKey)) {
					continue;
				} else if (open.contains(neighborKey)) {
					// is the current G better than the existing G
					if (currentG < scoreG.get(neighborKey)) {
						// then change G and parent
						scoreG.put(neighborKey, currentG);
						parent.put(neighborKey, current);
					}
				} else {
					// put the current G for this one in memory
					scoreG.put(neighborKey, currentG);
					// put the parent in memory
					parent.put(neighborKey, current);
					open.add(neighborKey);
				}
			}
			String bestMatch = null;
			double bestF = Integer.MAX_VALUE;
			for (String possible : open) {
				// calculate F
				double currentF = scoreG.get(possible) + scoreH.get(possible);
				if (currentF < bestF) {
					bestMatch = possible;
					bestF = currentF;
				}
			}
			current = bestMatch;
			closed.add(current);
			open.remove(current);
			if (current.equals(endKey)) {
				found = true;
			}
		}
		// now we just need to back up from the last current by parents until we
		// hit null
		ArrayList<Planet> path = new ArrayList<Planet>();
		String nextKey = current;
		while (null != nextKey) {
			path.add(Planets.getInstance().getPlanets().get(nextKey));
			// MekHQApp.logMessage(nextKey);
			nextKey = parent.get(nextKey);

		}
		// now reverse the direaction
		JumpPath finalPath = new JumpPath();
		for (int i = (path.size() - 1); i >= 0; i--) {
			finalPath.addPlanet(path.get(i));
		}
		return finalPath;
	}

	public ArrayList<String> getAllReachablePlanetsFrom(Planet planet) {
		ArrayList<String> neighbors = new ArrayList<String>();
		for (String key : Planets.getInstance().getPlanets().keySet()) {
			Planet p = Planets.getInstance().getPlanets().get(key);
			if (planet.getDistanceTo(p) <= 30.0) {
				neighbors.add(key);
			}
		}
		return neighbors;
	}

	/**
	 * Right now this is going to be a total hack because the rules from FM Merc
	 * would be a nightmare to calculate and I want to get something up and
	 * running so we can do contracts. There are two components to figure - the
	 * costs of leasing dropships for excess units and the cost of leasing
	 * jumpships based on the number of dropships. Right now, we are just going
	 * to calculate average costs per unit and then make some guesses about
	 * total dropship collar needs.
	 * 
	 * Hopefully, StellarOps will clarify all of this.
	 */
	public long calculateCostPerJump(boolean excludeOwnTransports) {
		// first we need to get the total number of units by type
		int nMech = 0;
		int nVee = 0;
		int nAero = 0;
		int nBA = 0;
		int nMechInf = 0;
		int nMotorInf = 0;
		int nFootInf = 0;

		double cargoSpace = 0.0;

		int nDropship = 0;
		int nCollars = 0;

		for (Unit u : getUnits()) {
			Entity en = u.getEntity();
			if (en instanceof Dropship && excludeOwnTransports) {
				nDropship++;
				// decrement total needs by what this dropship can offer
				for (Bay bay : en.getTransportBays()) {
					if (bay instanceof MechBay) {
						nMech -= bay.getCapacity();
					} else if (bay instanceof LightVehicleBay) {
						nVee -= bay.getCapacity();
					} else if (bay instanceof HeavyVehicleBay) {
						nVee -= bay.getCapacity();
					} else if (bay instanceof ASFBay
							|| bay instanceof SmallCraftBay) {
						nAero -= bay.getCapacity();
					} else if (bay instanceof BattleArmorBay) {
						nBA -= bay.getCapacity() * 4;
					} else if (bay instanceof InfantryBay) {
						nMechInf -= bay.getCapacity() * 28;
					} else if (bay instanceof CargoBay) {
						cargoSpace += bay.getCapacity();
					}
				}
			} else if (en instanceof Jumpship && excludeOwnTransports) {
				nCollars += ((Jumpship) en).getDocks();
			} else if (en instanceof Mech) {
				nMech++;
			} else if (en instanceof Tank) {
				nVee++;
			} else if (en instanceof Aero && !(en instanceof Dropship)
					&& !(en instanceof Jumpship)) {
				nAero++;
			} else if (en instanceof BattleArmor) {
				nBA += 4;
			} else if (en instanceof Infantry) {
				if (en.getMovementMode() == EntityMovementMode.INF_LEG
						|| en.getMovementMode() == EntityMovementMode.INF_LEG) {
					nFootInf += ((Infantry) en).getSquadN()
							* ((Infantry) en).getSquadSize();
				} else if (en.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
					nMotorInf += ((Infantry) en).getSquadN()
							* ((Infantry) en).getSquadSize();
				} else {
					nMechInf += ((Infantry) en).getSquadN()
							* ((Infantry) en).getSquadSize();
				}
			}
			// if we havent got you yet then you fly free (yay!)
		}

		if (nMech < 0) {
			nMech = 0;
		}
		if (nVee < 0) {
			nVee = 0;
		}
		if (nAero < 0) {
			nAero = 0;
		}
		if (nBA < 0) {
			nBA = 0;
		}
		// now lets resort the infantry a bit
		if (nMechInf < 0) {
			nMotorInf += nMechInf;
			nMechInf = 0;
		}
		if (nMotorInf < 0) {
			nFootInf += nMotorInf;
		}
		if (nFootInf < 0) {
			nFootInf = 0;
		}

		// Ok, now the costs per unit - this is the dropship fee. I am loosely
		// basing this on Field Manual Mercs, although I think the costs are
		// f'ed up
		long dropshipCost = 0;
		dropshipCost += nMech * 10000;
		dropshipCost += nAero * 15000;
		dropshipCost += nVee * 3000;
		dropshipCost += nBA * 250;
		dropshipCost += nMechInf * 100;
		dropshipCost += nMotorInf * 50;
		dropshipCost += nFootInf * 10;

		// ok, now how many dropship collars do we need for these units? base
		// this on
		// some of the canonical designs
		int collarsNeeded = 0;
		// for mechs assume a union or smaller
		collarsNeeded += (int) Math.ceil(nMech / 12.0);
		// for aeros, they may ride for free on the union, if not assume a
		// leopard cv
		collarsNeeded += (int) Math
				.ceil(Math.max(0, nAero - collarsNeeded * 2) / 6.0);
		// for vees, assume a Triumph
		collarsNeeded += (int) Math.ceil(nVee / 53.0);
		// for now I am going to let infantry and BA tag along because of cargo
		// space rules

		// add the existing dropships
		collarsNeeded += nDropship;

		// now factor in owned jumpships
		collarsNeeded = Math.max(0, collarsNeeded - nCollars);

		return dropshipCost + collarsNeeded * 50000;
	}

	public void personUpdated(Person p) {
		Unit u = getUnit(p.getUnitId());
		if (null != u) {
			u.resetPilotAndEntity();

		}
	}

	public TargetRoll getTargetFor(IPartWork partWork, Person tech) {
		Skill skill = tech.getSkillForWorkingOn(partWork);
		int modePenalty = Modes.getModeExperienceReduction(partWork.getMode());
		if (null != partWork.getUnit() && !partWork.getUnit().isAvailable(partWork instanceof Refit)) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"This unit is not currently available!");
		}
		if (partWork.getAssignedTeamId() != null
				&& !partWork.getAssignedTeamId().equals(tech.getId())) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"Already being worked on by another team");
		}
		if (null == skill) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"Assigned tech does not have the right skills");
		}
		if (!getCampaignOptions().isDestroyByMargin()
				&& partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"Task is beyond this tech's skill level");
		}
		if (partWork.getSkillMin() > SkillType.EXP_ELITE) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is impossible.");
		}
		if (!partWork.needsFixing() && !partWork.isSalvaging()) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, "Task is not needed.");
		}
		if (partWork instanceof MissingPart
				&& null == ((MissingPart) partWork).findReplacement(false)) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, "Part not available.");
		}
		if (!(partWork instanceof Refit) && tech.getMinutesLeft() <= 0
				&& (!isOvertimeAllowed() || tech.getOvertimeLeft() <= 0)) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE, "No time left.");
		}
		if (null != partWork.getUnit()) {
			String notFixable = partWork.checkFixable();
			if (null != notFixable) {
				return new TargetRoll(TargetRoll.IMPOSSIBLE, notFixable);
			}
		}
		// if this is an infantry refit, then automatic success
		if (partWork instanceof Refit && null != partWork.getUnit()
				&& partWork.getUnit().getEntity() instanceof Infantry
				&& !(partWork.getUnit().getEntity() instanceof BattleArmor)) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
					"infantry refit");
		}

		// this is ugly, if the mode penalty drops you to green, you drop two
		// levels instead of two
		int value = skill.getFinalSkillValue() + modePenalty;
		if (modePenalty > 0
				&& SkillType.EXP_GREEN == (skill.getExperienceLevel() - modePenalty)) {
			value++;
		}
		TargetRoll target = new TargetRoll(value,
				SkillType.getExperienceLevelName(skill.getExperienceLevel()
						- modePenalty));
		if (target.getValue() == TargetRoll.IMPOSSIBLE) {
			return target;
		}

		target.append(partWork.getAllMods());

		if (getCampaignOptions().useEraMods()) {
			target.addModifier(getFaction().getEraMod(getEra()), "era");
		}

		boolean isOvertime = false;
		if (isOvertimeAllowed()
				&& (tech.isTaskOvertime(partWork) || partWork
						.hasWorkedOvertime())) {
			target.addModifier(3, "overtime");
			isOvertime = true;
		}

		int minutes = partWork.getTimeLeft();
		if (minutes > tech.getMinutesLeft()) {
			if (isOvertimeAllowed()) {
				if (minutes > (tech.getMinutesLeft() + tech.getOvertimeLeft())) {
					minutes = tech.getMinutesLeft() + tech.getOvertimeLeft();
				}
			} else {
				minutes = tech.getMinutesLeft();
			}
		}
		int helpMod = 0;
		if(null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
		    int hits = 0;
		    if(null != partWork.getUnit().getEntity().getCrew()) {
		        hits = partWork.getUnit().getEntity().getCrew().getHits();
		    } else {
		        hits = 6;
		    }
		    helpMod = getShorthandedModForCrews(hits);
		} else {
		    int helpers = getAvailableAstechs(minutes, isOvertime);
		    helpMod = getShorthandedMod(helpers, false);
		    // we may have just gone overtime with our helpers
	        if (!isOvertime && astechPoolMinutes < (minutes * helpers)) {
	            target.addModifier(3, "overtime astechs");
	        }
		}
		if (null != partWork.getUnit()
				&& (partWork.getUnit().getEntity() instanceof Dropship || partWork
						.getUnit().getEntity() instanceof Jumpship)) {
			helpMod = 0;
		}
		if (partWork.getShorthandedMod() > helpMod) {
			helpMod = partWork.getShorthandedMod();
		}
		if (helpMod > 0) {
			target.addModifier(helpMod, "shorthanded");
		}
		return target;
	}

	public TargetRoll getTargetForMaintenance(IPartWork partWork, Person tech) {
		int value = 10;
		String skillLevel = "Unmaintained";
		if (null != tech) {
			Skill skill = tech.getSkillForWorkingOn(partWork);
			if (null != skill) {
				value = skill.getFinalSkillValue();
				skillLevel = SkillType.getExperienceLevelName(skill
						.getExperienceLevel());
			}
		}

		TargetRoll target = new TargetRoll(value, skillLevel);
		if (target.getValue() == TargetRoll.IMPOSSIBLE) {
			return target;
		}

		target.append(partWork.getAllModsForMaintenance());

		if (getCampaignOptions().useEraMods()) {
			target.addModifier(getFaction().getEraMod(getEra()), "era");
		}

		if (null != partWork.getUnit()) {
			// we have no official rules for what happens when a tech is only
			// assigned
			// for part of the maintenance cycle, so we will create our own
			// penalties
			if (partWork.getUnit().getMaintainedPct() < .5) {
				target.addModifier(2, "partial maintenance");
			} else if (partWork.getUnit().getMaintainedPct() < 1) {
				target.addModifier(1, "partial maintenance");
			}

			// the astech issue is crazy, because you can actually be better off
			// not maintaining
			// than going it short-handed, but that is just the way it is.
			// Still, there is also some fuzziness about what happens if you are
			// short astechs
			// for part of the cycle. We will keep keep track of the total
			// "astech days" used over
			// the cycle and take the average per day rounding down as our team
			// size
			int helpMod = 0;
	        if(null != partWork.getUnit() && partWork.getUnit().isSelfCrewed()) {
	            int hits = 0;
	            if(null != partWork.getUnit().getEntity().getCrew()) {
	                hits = partWork.getUnit().getEntity().getCrew().getHits();
	            } else {
	                hits = 6;
	            }
	            helpMod = getShorthandedModForCrews(hits);
	        } else {
	            int helpers = partWork.getUnit().getAstechsMaintained();
	            helpMod = getShorthandedMod(helpers, false);
	        }
			if (helpMod > 0) {
				target.addModifier(helpMod, "shorthanded");
			}
		}

		return target;
	}

	public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
			Person person) {
		return getTargetForAcquisition(acquisition, person, true);
	}

	public TargetRoll getTargetForAcquisition(IAcquisitionWork acquisition,
			Person person, boolean checkDaysToWait) {
		if (getCampaignOptions().getAcquisitionSkill().equals(
				CampaignOptions.S_AUTO)) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS,
					"Automatic Success");
		}
		if (null == person) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"No one on your force is capable of acquiring parts");
		}
		Skill skill = person.getSkillForWorkingOn(acquisition,
				getCampaignOptions().getAcquisitionSkill());
		if (null != getShoppingList().getShoppingItem(
				acquisition.getNewEquipment())
				&& checkDaysToWait) {
			return new TargetRoll(
					TargetRoll.AUTOMATIC_FAIL,
					"You must wait until the new cycle to check for this part. Further attempts will be added to the shopping list.");
		}
		if (acquisition.getTechBase() == Part.T_CLAN
				&& !getCampaignOptions().allowClanPurchases()) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"You cannot acquire clan parts");
		}
		if (acquisition.getTechBase() == Part.T_IS
				&& !getCampaignOptions().allowISPurchases()) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"You cannot acquire inner sphere parts");
		}
		if (getCampaignOptions().getTechLevel() < Utilities
				.getSimpleTechLevel(acquisition.getTechLevel())) {
			return new TargetRoll(TargetRoll.IMPOSSIBLE,
					"You cannot acquire parts of this tech level");
		}
		TargetRoll target = new TargetRoll(skill.getFinalSkillValue(),
				SkillType.getExperienceLevelName(skill.getExperienceLevel()));// person.getTarget(Modes.MODE_NORMAL);
		target.append(acquisition.getAllAcquisitionMods());
		return target;
	}

	public void resetAstechMinutes() {
		astechPoolMinutes = 480 * getNumberPrimaryAstechs() + 240
				* getNumberSecondaryAstechs();
		astechPoolOvertime = 240 * getNumberPrimaryAstechs() + 120
				* getNumberSecondaryAstechs();
	}

	public int getAstechPoolMinutes() {
		return astechPoolMinutes;
	}

	public int getAstechPoolOvertime() {
		return astechPoolOvertime;
	}

	public int getAstechPool() {
		return astechPool;
	}

	public int getMedicPool() {
		return medicPool;
	}

	public void increaseAstechPool(int i) {
		astechPool += i;
		astechPoolMinutes += (480 * i);
		astechPoolOvertime += (240 * i);
	}

	public void decreaseAstechPool(int i) {
		astechPool = Math.max(0, astechPool - i);
		// always assume that we fire the ones who have not yet worked
		astechPoolMinutes = Math.max(0, astechPoolMinutes - 480 * i);
		astechPoolOvertime = Math.max(0, astechPoolOvertime - 240 * i);
	}

	public int getNumberAstechs() {
		return getNumberPrimaryAstechs() + getNumberSecondaryAstechs();
	}

	public int getNumberPrimaryAstechs() {
		int astechs = astechPool;
		for (Person p : personnel) {
			if ((p.getPrimaryRole() == Person.T_ASTECH) && p.isActive()
					&& !p.isDeployed(this)) {
				astechs++;
			}
		}
		return astechs;
	}

	public int getNumberSecondaryAstechs() {
		int astechs = 0;
		for (Person p : personnel) {
			if ((p.getSecondaryRole() == Person.T_ASTECH) && p.isActive()
					&& !p.isDeployed(this)) {
				astechs++;
			}
		}
		return astechs;
	}

	public int getAvailableAstechs(int minutes, boolean alreadyOvertime) {
		int availableHelp = (int) Math.floor(((double) astechPoolMinutes)
				/ minutes);
		if (isOvertimeAllowed() && availableHelp < 6) {
			// if we are less than fully staffed, then determine whether
			// we should dip into overtime or just continue as short-staffed
			int shortMod = getShorthandedMod(availableHelp, false);
			int remainingMinutes = astechPoolMinutes - availableHelp * minutes;
			int extraHelp = (remainingMinutes + astechPoolOvertime) / minutes;
			int helpNeeded = 6 - availableHelp;
			if (alreadyOvertime && shortMod > 0) {
				// then add whatever we can
				availableHelp += extraHelp;
			} else if (shortMod > 3) {
				// only dip in if we can bring ourselves up to full
				if (extraHelp >= helpNeeded) {
					availableHelp = 6;
				}
			}
		}
		if (availableHelp > 6) {
			availableHelp = 6;
		}
		if (availableHelp > getNumberAstechs()) {
			return getNumberAstechs();
		}
		return availableHelp;
	}

	public int getShorthandedMod(int availableHelp, boolean medicalStaff) {
		if (medicalStaff) {
			availableHelp += 2;
		}
		int helpMod = 0;
		if (availableHelp == 0) {
			helpMod = 4;
		} else if (availableHelp == 1) {
			helpMod = 3;
		} else if (availableHelp < 4) {
			helpMod = 2;
		} else if (availableHelp < 6) {
			helpMod = 1;
		}
		return helpMod;
	}
	
	public int getShorthandedModForCrews(int hits) {
        int helpMod = 0;
        if (hits >= 5) {
            helpMod = 4;
        } else if (hits == 4) {
            helpMod = 3;
        } else if (hits == 3) {
            helpMod = 2;
        } else if (hits > 0) {
            helpMod = 1;
        }
        return helpMod;
    }

	public int getMedicsPerDoctor() {
		int ndocs = getDoctors().size();
		int nmedics = getNumberMedics();
		if (ndocs == 0) {
			return 0;
		}
		// TODO: figure out what to do with fractions
		return Math.min(nmedics / ndocs, 4);
	}

	public int getNumberMedics() {
		int medics = medicPool;
		for (Person p : personnel) {
			if ((p.getPrimaryRole() == Person.T_MEDIC || p.getSecondaryRole() == Person.T_MEDIC)
					&& p.isActive() && !p.isDeployed(this)) {
				medics++;
			}
		}
		return medics;
	}

	public void increaseMedicPool(int i) {
		medicPool += i;
	}

	public void decreaseMedicPool(int i) {
		medicPool = Math.max(0, medicPool - i);
	}

	public void changePrisonerStatus(Person p, int status) {
		switch (status) {
		case Person.PRISONER_NOT:
			p.setFreeMan();
			p.addLogEntry(getDate(), "Freed");
			break;
		case Person.PRISONER_YES:
			if (p.getRank() > 0) {
				changeRank(p, 0, true); // They don't get to have a rank. Their
										// rank is Prisoner or Bondsman.
			}
			p.setPrisoner();
			p.addLogEntry(getDate(), "Made Prisoner");
			break;
		case Person.PRISONER_BONDSMAN:
			if (p.getRank() > 0) {
				changeRank(p, 0, true); // They don't get to have a rank. Their
										// rank is Prisoner or Bondsman.
			}
			p.setBondsman();
			p.addLogEntry(getDate(), "Made Bondsman");
			break;
		default:
			break;
		}
		if (p.isBondsman() || p.isPrisoner()) {
			Unit u = getUnit(p.getUnitId());
			if (u != null) {
				u.remove(p, true);
			}
		}
	}

	public void changeStatus(Person person, int status) {
		Unit u = getUnit(person.getUnitId());
		if (status == Person.S_KIA) {
			person.addLogEntry(getDate(), "Killed in action");
			// set the deathday
			person.setDeathday((GregorianCalendar) calendar.clone());
		} else if (person.getStatus() == Person.S_KIA) {
			// remove deathdates for resurrection
			person.setDeathday(null);
		}
		if (status == Person.S_MIA) {
			person.addLogEntry(getDate(), "Missing in action");
		}
		if (status == Person.S_RETIRED) {
			person.addLogEntry(getDate(), "Retired from active duty");
		}
		if (status == Person.S_ACTIVE && person.getStatus() == Person.S_MIA) {
			person.addLogEntry(getDate(), "Recovered from MIA status");
		}
		person.setStatus(status);
		if (status != Person.S_ACTIVE) {
			person.setDoctorId(null, getCampaignOptions()
					.getNaturalHealingWaitingPeriod());
			if (null != u) {
				u.remove(person, true);
			}
		}
	}

	public void changeRank(Person person, int rank, boolean report) {
		if (report) {
			if (rank > person.getRank()) {
				person.addLogEntry(getDate(), "Promoted to "
						+ getRanks().getRank(rank));
			} else if (rank < person.getRank()) {
				person.addLogEntry(getDate(), "Demoted to "
						+ getRanks().getRank(rank));
			}
		}
		person.setRank(rank);
		personUpdated(person);
	}

	public GameOptions getGameOptions() {
		return gameOptions;
	}

	public Vector<IBasicOption> getGameOptionsVector() {
		Vector<IBasicOption> options = new Vector<IBasicOption>();
		for (Enumeration<IOptionGroup> i = gameOptions.getGroups(); i
				.hasMoreElements();) {
			IOptionGroup group = i.nextElement();
			for (Enumeration<IOption> j = group.getOptions(); j
					.hasMoreElements();) {
				IOption option = j.nextElement();
				options.add((IBasicOption) option);
			}
		}
		return options;
	}

	public void setGameOptions(Vector<IBasicOption> options) {
		for (IBasicOption option : options) {
			gameOptions.getOption(option.getName()).setValue(option.getValue());
		}
	}

	public void addKill(Kill k) {
		kills.add(k);
		if (getCampaignOptions().getKillsForXP() > 0
				&& getCampaignOptions().getKillXPAward() > 0) {
			if ((getKillsFor(k.getPilotId()).size() % getCampaignOptions()
					.getKillsForXP()) == 0) {
				Person p = getPerson(k.getPilotId());
				if (null != p) {
					p.setXp(p.getXp() + getCampaignOptions().getKillXPAward());
				}
			}
		}
	}

	public ArrayList<Kill> getKillsFor(UUID pid) {
		ArrayList<Kill> personalKills = new ArrayList<Kill>();
		for (Kill k : kills) {
			if (k.getPilotId().equals(pid)) {
				personalKills.add(k);
			}
		}
		Collections.sort(personalKills, new Comparator<Kill>() {
			public int compare(final Kill u1, final Kill u2) {
				return u1.getDate().compareTo(u2.getDate());
			}
		});
		return personalKills;
	}

	public PartsStore getPartsStore() {
		return partsStore;
	}

	public void addCustom(String name) {
		customs.add(name);
	}

	public boolean isCustom(Unit u) {
		return customs.contains(u.getEntity().getChassis() + " "
				+ u.getEntity().getModel());
	}

	/**
	 * borrowed from megamek.client
	 */
	private void checkDuplicateNamesDuringAdd(Entity entity) {
		if (duplicateNameHash.get(entity.getShortName()) == null) {
			duplicateNameHash.put(entity.getShortName(), new Integer(1));
		} else {
			int count = duplicateNameHash.get(entity.getShortName()).intValue();
			count++;
			duplicateNameHash.put(entity.getShortName(), new Integer(count));
			entity.duplicateMarker = count;
			entity.generateShortName();
			entity.generateDisplayName();

		}
	}

	/**
	 * If we remove a unit, we may need to update the duplicate identifier.
	 * TODO: This function is super slow :(
	 * 
	 * @param entity
	 */
	private void checkDuplicateNamesDuringDelete(Entity entity) {
		Object o = duplicateNameHash.get(entity.getShortNameRaw());
		if (o != null) {
			int count = ((Integer) o).intValue();
			if (count > 1) {
				for (Unit u : getUnits()) {
					Entity e = u.getEntity();
					if (e.getShortNameRaw().equals(entity.getShortNameRaw())
							&& (e.duplicateMarker > entity.duplicateMarker)) {
						e.duplicateMarker--;
						e.generateShortName();
						e.generateDisplayName();
					}
				}
				duplicateNameHash.put(entity.getShortNameRaw(), new Integer(
						count - 1));
			} else {
				duplicateNameHash.remove(entity.getShortNameRaw());
			}
		}
	}

	public void hirePersonnelFor(UUID uid) {
		Unit unit = getUnit(uid);
		if (null == unit) {
			return;
		}
		while (unit.canTakeMoreDrivers()) {
			Person p = null;
			if (unit.getEntity() instanceof Mech) {
				p = newPerson(Person.T_MECHWARRIOR);
			} else if (unit.getEntity() instanceof SmallCraft
					|| unit.getEntity() instanceof Jumpship) {
				p = newPerson(Person.T_SPACE_PILOT);
			} else if (unit.getEntity() instanceof ConvFighter) {
				p = newPerson(Person.T_CONV_PILOT);
			} else if (unit.getEntity() instanceof Aero) {
				p = newPerson(Person.T_AERO_PILOT);
			} else if (unit.getEntity() instanceof VTOL) {
				p = newPerson(Person.T_VTOL_PILOT);
			} else if (unit.getEntity() instanceof Tank) {
				p = newPerson(Person.T_GVEE_DRIVER);
			} else if (unit.getEntity() instanceof Protomech) {
				p = newPerson(Person.T_PROTO_PILOT);
			} else if (unit.getEntity() instanceof BattleArmor) {
				p = newPerson(Person.T_BA);
			} else if (unit.getEntity() instanceof Infantry) {
				p = newPerson(Person.T_INFANTRY);
			}
			if (null == p) {
				break;
			}
			if (!recruitPerson(p)) {
				return;
			}
			if (unit.usesSoloPilot() || unit.usesSoldiers()) {
				unit.addPilotOrSoldier(p);
			} else {
				unit.addDriver(p);
			}
		}
		while (unit.canTakeMoreGunners()) {
			Person p = null;
			if (unit.getEntity() instanceof Tank) {
				p = newPerson(Person.T_VEE_GUNNER);
			} else if (unit.getEntity() instanceof SmallCraft
					|| unit.getEntity() instanceof Jumpship) {
				p = newPerson(Person.T_SPACE_GUNNER);
			}
			if (!recruitPerson(p)) {
				return;
			}
			unit.addGunner(p);
		}
		while (unit.canTakeMoreVesselCrew()) {
			Person p = newPerson(Person.T_SPACE_CREW);
			if (!recruitPerson(p)) {
				return;
			}
			unit.addVesselCrew(p);
		}
		if (unit.canTakeNavigator()) {
			Person p = newPerson(Person.T_NAVIGATOR);
			if (!recruitPerson(p)) {
				return;
			}
			unit.setNavigator(p);
		}
		unit.resetPilotAndEntity();
		unit.runDiagnostic();
	}

	public String getDragoonRating() {
		IDragoonsRating rating = DragoonsRatingFactory.getDragoonsRating(this);
		rating.reInitialize();
		return rating.getDragoonRating();
	}

	public RandomSkillPreferences getRandomSkillPreferences() {
		return rskillPrefs;
	}

	public void setStartingPlanet() {
		Planet startingPlanet = Planets.getInstance().getPlanets()
				.get(getFaction().getStartingPlanet(getEra()));
		if (startingPlanet == null) {
			location = new CurrentLocation(Planets.getInstance().getPlanets()
					.get("Terra"), 0);
		} else {
			location = new CurrentLocation(startingPlanet, 0);
		}
	}

	public void addLogEntry(Person p, LogEntry entry) {
		p.addLogEntry(entry);
	}

	public void assignRandomPortraitFor(Person p) {
		// first create a list of existing portait strings, so we can check for
		// duplicates
		ArrayList<String> existingPortraits = new ArrayList<String>();
		for (Person existingPerson : this.getPersonnel()) {
			existingPortraits.add(existingPerson.getPortraitCategory() + ":"
					+ existingPerson.getPortraitFileName());
		}
		// TODO: it would be nice to pull the portraits directory from MekHQ
		// itself
		DirectoryItems portraits;
		try {
			portraits = new DirectoryItems(
					new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
					PortraitFileFactory.getInstance());
		} catch (Exception e) {
			return;
		}
		ArrayList<String> possiblePortraits = new ArrayList<String>();
		Iterator<String> categories = portraits.getCategoryNames();
		while (categories.hasNext()) {
			String category = categories.next();
			if ((category.endsWith("Male/") && p.getGender() == Person.G_MALE)
					|| (category.endsWith("Female/") && p.getGender() == Person.G_FEMALE)) {
				Iterator<String> names = portraits.getItemNames(category);
				while (names.hasNext()) {
					String name = names.next();
					String location = category + ":" + name;
					if (existingPortraits.contains(location)) {
						continue;
					}
					possiblePortraits.add(location);
				}
			}
		}
		if (!possiblePortraits.isEmpty()) {
			String chosenPortrait = possiblePortraits.get(Compute
					.randomInt(possiblePortraits.size()));
			String[] temp = chosenPortrait.split(":");
			if (temp.length != 2) {
				return;
			}
			p.setPortraitCategory(temp[0]);
			p.setPortraitFileName(temp[1]);
		}
	}

	public void clearGameData(Entity entity) {
		for (Mounted m : entity.getEquipment()) {
			m.setUsedThisRound(false);
			m.setJammed(false);
		}
		entity.setPassedThrough(new Vector<Coords>());
		entity.resetFiringArcs();
		entity.resetBays();
		entity.setEvading(false);
		entity.setFacing(0);
		entity.setPosition(null);
		entity.setDeployRound(0);
		// TODO: still a lot of stuff to do here, but oh well
		entity.setOwner(player);
		entity.setGame(game);
	}

	public Part checkForExistingSparePart(Part part) {
		for (Part spare : getSpareParts()) {
			if (spare.getId() == part.getId()) {
				continue;
			}
			if (part.isSamePartTypeAndStatus(spare)) {
				return spare;
			}
		}
		return null;
	}

	public void refreshNetworks() {

		for (Unit unit : getUnits()) {
			// we are going to rebuild the c3 and c3i networks based on
			// the c3UUIDs
			// TODO: can we do this more efficiently?
			// this code is cribbed from megamek.server#receiveEntityAdd
			Entity entity = unit.getEntity();
			if (null != entity && (entity.hasC3() || entity.hasC3i())) {
				boolean C3iSet = false;

				for (Enumeration<Entity> entities = game.getEntities(); entities
						.hasMoreElements();) {
					Entity e = entities.nextElement();

					// C3 Checks
					if (entity.hasC3()) {
						if ((entity.getC3MasterIsUUIDAsString() != null)
								&& entity.getC3MasterIsUUIDAsString().equals(
										e.getC3UUIDAsString())) {
							entity.setC3Master(e, false);
							break;
						}
					}

					// C3i Checks// C3i Checks
					if (entity.hasC3i() && (C3iSet == false)) {
						entity.setC3NetIdSelf();
						int pos = 0;
						while (pos < Entity.MAX_C3i_NODES) {
							// We've found a network, join it.
							if ((entity.getC3iNextUUIDAsString(pos) != null)
									&& (e.getC3UUIDAsString() != null)
									&& entity.getC3iNextUUIDAsString(pos)
											.equals(e.getC3UUIDAsString())) {
								entity.setC3NetId(e);
								C3iSet = true;
								break;
							}

							pos++;
						}
					}
				}
			}
		}
	}

	public void disbandNetworkOf(Unit u) {
		// collect all of the other units on this network to rebuild the uuids
		Vector<Unit> networkedUnits = new Vector<Unit>();
		for (Unit unit : getUnits()) {
			if (null != unit.getEntity().getC3NetId()
					&& unit.getEntity().getC3NetId()
							.equals(u.getEntity().getC3NetId())) {
				networkedUnits.add(unit);
			}
		}
		for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
			for (Unit nUnit : networkedUnits) {
				nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
			}
		}
		refreshNetworks();
	}

	public void removeUnitsFromNetwork(Vector<Unit> removedUnits) {
		// collect all of the other units on this network to rebuild the uuids
		Vector<String> uuids = new Vector<String>();
		Vector<Unit> networkedUnits = new Vector<Unit>();
		String network = removedUnits.get(0).getEntity().getC3NetId();
		for (Unit unit : getUnits()) {
			if (removedUnits.contains(unit)) {
				continue;
			}
			if (null != unit.getEntity().getC3NetId()
					&& unit.getEntity().getC3NetId().equals(network)) {
				networkedUnits.add(unit);
				uuids.add(unit.getEntity().getC3UUIDAsString());
			}
		}
		for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
			for (Unit u : removedUnits) {
				u.getEntity().setC3iNextUUIDAsString(pos, null);
			}
			for (Unit nUnit : networkedUnits) {
				if (pos < uuids.size()) {
					nUnit.getEntity().setC3iNextUUIDAsString(pos,
							uuids.get(pos));
				} else {
					nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
				}
			}
		}
		refreshNetworks();
	}

	public void addUnitsToNetwork(Vector<Unit> addedUnits, String netid) {
		// collect all of the other units on this network to rebuild the uuids
		Vector<String> uuids = new Vector<String>();
		Vector<Unit> networkedUnits = new Vector<Unit>();
		for (Unit u : addedUnits) {
			uuids.add(u.getEntity().getC3UUIDAsString());
			networkedUnits.add(u);
		}
		for (Unit unit : getUnits()) {
			if (addedUnits.contains(unit)) {
				continue;
			}
			if (null != unit.getEntity().getC3NetId()
					&& unit.getEntity().getC3NetId().equals(netid)) {
				networkedUnits.add(unit);
				uuids.add(unit.getEntity().getC3UUIDAsString());
			}
		}
		for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
			for (Unit nUnit : networkedUnits) {
				if (pos < uuids.size()) {
					nUnit.getEntity().setC3iNextUUIDAsString(pos,
							uuids.get(pos));
				} else {
					nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
				}
			}
		}
		refreshNetworks();
	}

	public Vector<String[]> getAvailableC3iNetworks() {
		Vector<String[]> networks = new Vector<String[]>();
		Vector<String> networkNames = new Vector<String>();

		for (Entity en : getEntities()) {
			if (en.hasC3i() && en.calculateFreeC3Nodes() < 5
					&& en.calculateFreeC3Nodes() > 0) {
				String[] network = new String[2];
				network[0] = en.getC3NetId();
				network[1] = "" + en.calculateFreeC3Nodes();
				if (!networkNames.contains(network[0])) {
					networks.add(network);
					networkNames.add(network[0]);
				}
			}
		}
		return networks;
	}

	public Vector<String[]> getAvailableC3MastersForSlaves() {
		Vector<String[]> networks = new Vector<String[]>();
		Vector<String> networkNames = new Vector<String>();

		for (Entity en : getEntities()) {
			// count of free c3 nodes for single company-level masters
			// will not be right so skip
			if (en.hasC3M() && !en.hasC3MM() && en.C3MasterIs(en)) {
				continue;
			}
			if (en.calculateFreeC3Nodes() > 0) {
				String[] network = new String[3];
				network[0] = en.getC3UUIDAsString();
				network[1] = "" + en.calculateFreeC3Nodes();
				network[2] = "" + en.getShortName();
				if (!networkNames.contains(network[0])) {
					networks.add(network);
					networkNames.add(network[0]);
				}
			}
		}

		return networks;
	}

	public Vector<String[]> getAvailableC3MastersForMasters() {
		Vector<String[]> networks = new Vector<String[]>();
		Vector<String> networkNames = new Vector<String>();

		for (Entity en : getEntities()) {
			if (en.calculateFreeC3MNodes() > 0) {
				String[] network = new String[3];
				network[0] = en.getC3UUIDAsString();
				network[1] = "" + en.calculateFreeC3MNodes();
				network[2] = "" + en.getShortName();
				if (!networkNames.contains(network[0])) {
					networks.add(network);
					networkNames.add(network[0]);
				}
			}
		}

		return networks;
	}

	public void removeUnitsFromC3Master(Unit master) {
		for (Unit unit : getUnits()) {
			if (null != unit.getEntity().getC3MasterIsUUIDAsString()
					&& unit.getEntity().getC3MasterIsUUIDAsString()
							.equals(master.getEntity().getC3UUIDAsString())) {
				unit.getEntity().setC3MasterIsUUIDAsString(null);
				unit.getEntity().setC3Master(null, true);
			}
		}
		refreshNetworks();
	}

	/**
	 * This function reloads the game entities into the game at the end of
	 * scenario resolution, so that entities are properly updated and destroyed
	 * ones removed
	 */
	public void reloadGameEntities() {
		game.reset();
		for (Unit u : units) {
			Entity en = u.getEntity();
			if (null != en) {
				game.addEntity(en.getId(), en);
			}
		}
	}

	public void completeMission(int id, int status) {
		Mission mission = getMission(id);
		if (null == mission) {
			return;
		}
		mission.setStatus(status);
		if (mission instanceof Contract) {
			Contract contract = (Contract) mission;
			// check for money in escrow
			// According to FMM(r) pg 179, both failure and breach lead to no
			// further payment even though this seems stupid
			if (contract.getStatus() == Mission.S_SUCCESS
					&& contract.getMonthsLeft(getDate()) > 0) {
				DecimalFormat formatter = new DecimalFormat();
				long remainingMoney = contract.getMonthlyPayOut()
						* contract.getMonthsLeft(getDate());
				finances.credit(remainingMoney, Transaction.C_CONTRACT,
						"Remaining payment for " + contract.getName(),
						calendar.getTime());
				addReport("Your account has been credited for "
						+ formatter.format(remainingMoney)
						+ " C-bills for the remaining payout from contract "
						+ contract.getName());
			}
		}

	}

	public int calculatePartTransitTime(int mos) {
		int nDice = getCampaignOptions().getNDiceTransitTime();
		int time = getCampaignOptions().getConstantTransitTime();
		if (nDice > 0) {
			time += Compute.d6(nDice);
		}
		// now step forward through the calendar
		GregorianCalendar arrivalDate = (GregorianCalendar) calendar.clone();
		switch (getCampaignOptions().getUnitTransitTime()) {
		case CampaignOptions.TRANSIT_UNIT_MONTH:
			arrivalDate.add(Calendar.MONTH, time);
			break;
		case CampaignOptions.TRANSIT_UNIT_WEEK:
			arrivalDate.add(Calendar.WEEK_OF_YEAR, time);
			break;
		case CampaignOptions.TRANSIT_UNIT_DAY:
		default:
			arrivalDate.add(Calendar.DAY_OF_MONTH, time);
		}

		// now adjust for MoS and minimums
		int mosBonus = getCampaignOptions().getAcquireMosBonus() * mos;
		switch (getCampaignOptions().getAcquireMosUnit()) {
		case CampaignOptions.TRANSIT_UNIT_MONTH:
			arrivalDate.add(Calendar.MONTH, -1 * mosBonus);
			break;
		case CampaignOptions.TRANSIT_UNIT_WEEK:
			arrivalDate.add(Calendar.WEEK_OF_YEAR, -1 * mosBonus);
			break;
		case CampaignOptions.TRANSIT_UNIT_DAY:
		default:
			arrivalDate.add(Calendar.DAY_OF_MONTH, -1 * mosBonus);
		}
		// now establish minimum date and if this is before
		GregorianCalendar minimumDate = (GregorianCalendar) calendar.clone();
		switch (getCampaignOptions().getAcquireMinimumTimeUnit()) {
		case CampaignOptions.TRANSIT_UNIT_MONTH:
			minimumDate.add(Calendar.MONTH, getCampaignOptions()
					.getAcquireMinimumTime());
			break;
		case CampaignOptions.TRANSIT_UNIT_WEEK:
			minimumDate.add(Calendar.WEEK_OF_YEAR, getCampaignOptions()
					.getAcquireMinimumTime());
			break;
		case CampaignOptions.TRANSIT_UNIT_DAY:
		default:
			minimumDate.add(Calendar.DAY_OF_MONTH, getCampaignOptions()
					.getAcquireMinimumTime());
		}

		if (arrivalDate.before(minimumDate)) {
			return Utilities.getDaysBetween(calendar.getTime(),
					minimumDate.getTime());
		} else {
			return Utilities.getDaysBetween(calendar.getTime(),
					arrivalDate.getTime());
		}

	}

	/**
	 * This returns a String array of length three, with the following values 0
	 * - the number of parts of this type, or armor points, or ammo shots
	 * currently in stock 1 - the number of parts of this type, or armor points,
	 * or ammo shots currently in transit 2 - the number of parts of this type,
	 * or armor points, or ammo shots currently on order
	 * 
	 * @param part
	 * @return
	 */
	public String[] getPartInventory(Part part) {
		String[] inventories = new String[3];

		int nSupply = 0;
		int nTransit = 0;
		for (Part p : getParts()) {
			if (!p.isSpare()) {
				continue;
			}
			if (part.isSamePartType(p)) {
				if (p.isPresent()) {
					if (p instanceof Armor) {
						nSupply += ((Armor) p).getAmount();
					} else if (p instanceof ProtomekArmor) {
						nSupply += ((ProtomekArmor) p).getAmount();
					} else if (p instanceof BaArmor) {
						nSupply += ((BaArmor) p).getAmount();
					} else if (p instanceof AmmoStorage) {
						nSupply += ((AmmoStorage) p).getShots();
					} else {
						nSupply += p.getQuantity();
					}
				} else {
					if (p instanceof Armor) {
						nTransit += ((Armor) p).getAmount();
					} else if (p instanceof ProtomekArmor) {
						nTransit += ((ProtomekArmor) p).getAmount();
					} else if (p instanceof BaArmor) {
						nTransit += ((BaArmor) p).getAmount();
					} else if (p instanceof AmmoStorage) {
						nTransit += ((AmmoStorage) p).getShots();
					} else {
						nTransit += p.getQuantity();
					}
				}
			}
		}

		int nOrdered = 0;
		IAcquisitionWork onOrder = getShoppingList().getShoppingItem(part);
		if (null != onOrder) {
			if (onOrder instanceof Armor) {
				nOrdered += ((Armor) onOrder).getAmount();
			} else if (onOrder instanceof ProtomekArmor) {
				nOrdered += ((ProtomekArmor) onOrder).getAmount();
			} else if (onOrder instanceof BaArmor) {
				nOrdered += ((BaArmor) onOrder).getAmount();
			} else if (onOrder instanceof AmmoStorage) {
				nOrdered += ((AmmoStorage) onOrder).getShots();
			} else {
				nOrdered += onOrder.getQuantity();
			}
		}

		String strSupply = Integer.toString(nSupply);
		String strTransit = Integer.toString(nTransit);
		String strOrdered = Integer.toString(nOrdered);

		if (part instanceof Armor || part instanceof ProtomekArmor
				|| part instanceof BaArmor) {
			strSupply += " points";
			strTransit += " points";
			strOrdered += " points";
		}
		if (part instanceof AmmoStorage) {
			strSupply += " shots";
			strTransit += " shots";
			strOrdered += " shots";
		}
		inventories[0] = strSupply;
		inventories[1] = strTransit;
		inventories[2] = strOrdered;
		return inventories;
	}

	public long getTotalEquipmentValue() {
		long value = 0;
		for (Unit u : getUnits()) {
			value += u.getSellValue();
		}
		for (Part p : getSpareParts()) {
			value += p.getActualValue();
		}
		return value;
	}

	/**
	 * Calculate the total value of units in the TO&E. This serves as the basis
	 * for contract payments in the StellarOps Beta.
	 * 
	 * @return
	 */
	public long getForceValue() {
		long value = 0;
		for (UUID uuid : forces.getAllUnits()) {
			Unit u = getUnit(uuid);
			if (null == u) {
				continue;
			}
			// lets exclude dropships and jumpships
			if (u.getEntity() instanceof Dropship
					|| u.getEntity() instanceof Jumpship) {
				continue;
			}
			// we will assume sale value for now, but make this customizable
			if (getCampaignOptions().useEquipmentContractSaleValue()) {
				value += u.getSellValue();
			} else {
				value += u.getBuyCost();
			}
		}
		return value;
	}

	public long getContractBase() {
		if (getCampaignOptions().useEquipmentContractBase()) {
			return (long) ((getCampaignOptions().getEquipmentContractPercent() / 100.0) * getForceValue());
		} else {
			return getPayRoll();
		}
	}

	public void addLoan(Loan loan) {
		addReport("You have taken out loan " + loan.getDescription()
				+ ". Your account has been credited "
				+ DecimalFormat.getInstance().format(loan.getPrincipal())
				+ " for the principal amount.");
		finances.addLoan(loan);
		finances.credit(loan.getPrincipal(), Transaction.C_LOAN_PRINCIPAL,
				"loan principal for " + loan.getDescription(),
				calendar.getTime());
	}

	public void payOffLoan(Loan loan) {
		if (finances.debit(loan.getRemainingValue(),
				Transaction.C_LOAN_PAYMENT,
				"loan payoff for " + loan.getDescription(), calendar.getTime())) {
			addReport("You have paid off the remaining loan balance of "
					+ DecimalFormat.getInstance().format(
							loan.getRemainingValue()) + "on "
					+ loan.getDescription());
			finances.removeLoan(loan);
		} else {
			addReport("<font color='red'>You do not have enough funds to pay off "
					+ loan.getDescription() + "</font>");
		}

	}

	public String getFinancialReport() {
		StringBuffer sb = new StringBuffer();
		long cash = finances.getBalance();
		long loans = finances.getLoanBalance();
		long mech = 0;
		long vee = 0;
		long ba = 0;
		long infantry = 0;
		long smallCraft = 0;
		long largeCraft = 0;
		long proto = 0;
		long spareParts = 0;
		for (Unit u : units) {
			long value = u.getSellValue();
			if (u.getEntity() instanceof Mech) {
				mech += value;
			} else if (u.getEntity() instanceof Tank) {
				vee += value;
			} else if (u.getEntity() instanceof BattleArmor) {
				ba += value;
			} else if (u.getEntity() instanceof Infantry) {
				infantry += value;
			} else if (u.getEntity() instanceof Dropship
					|| u.getEntity() instanceof Jumpship) {
				largeCraft += value;
			} else if (u.getEntity() instanceof Aero) {
				smallCraft += value;
			} else if (u.getEntity() instanceof Protomech) {
				proto += value;
			}
		}
		for (Part p : getSpareParts()) {
			spareParts += p.getActualValue();
		}

		long monthlyIncome = 0;
		long monthlyExpenses = 0;
		long maintenance = 0;
		long salaries = 0;
		long overhead = 0;
		long contracts = 0;
		if (campaignOptions.payForMaintain()) {
			maintenance = getMaintenanceCosts() * 4;
		}
		if (campaignOptions.payForSalaries()) {
			salaries = getPayRoll();
		}
		if (campaignOptions.payForOverhead()) {
			overhead = getOverheadExpenses();
		}
		for (Contract contract : getActiveContracts()) {
			contracts += contract.getMonthlyPayOut();
		}
		monthlyIncome += contracts;
		monthlyExpenses = maintenance + salaries + overhead;

		long assets = cash + mech + vee + ba + infantry + largeCraft
				+ smallCraft + proto;
		long liabilities = loans;
		long netWorth = assets - liabilities;
		int longest = Math.max(DecimalFormat.getInstance().format(liabilities)
				.length(), DecimalFormat.getInstance().format(assets).length());
		longest = Math.max(DecimalFormat.getInstance().format(netWorth)
				.length(), longest);
		String formatted = "%1$" + longest + "s";
		sb.append("Net Worth................ ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(netWorth))).append("\n\n");
		sb.append("    Assets............... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(assets))).append("\n");
		sb.append("       Cash.............. ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(cash))).append("\n");
		if (mech > 0) {
			sb.append("       Mechs............. ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(mech))).append("\n");
		}
		if (vee > 0) {
			sb.append("       Vehicles.......... ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(vee))).append("\n");
		}
		if (ba > 0) {
			sb.append("       BattleArmor....... ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(ba))).append("\n");
		}
		if (infantry > 0) {
			sb.append("       Infantry.......... ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(infantry))).append("\n");
		}
		if (proto > 0) {
			sb.append("       Protomechs........ ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(proto))).append("\n");
		}
		if (smallCraft > 0) {
			sb.append("       Small Craft....... ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(smallCraft))).append("\n");
		}
		if (largeCraft > 0) {
			sb.append("       Large Craft....... ")
					.append(String.format(formatted, DecimalFormat
							.getInstance().format(largeCraft))).append("\n");
		}
		sb.append("       Spare Parts....... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(spareParts))).append("\n\n");
		// sb.append("       Other Assets........ ").append("").append("\n\n");
		sb.append("    Liabilities.......... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(liabilities))).append("\n");
		sb.append("       Loans............. ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(loans))).append("\n\n\n");

		sb.append("Monthly Profit........... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(monthlyIncome - monthlyExpenses)))
				.append("\n\n");
		sb.append("Monthly Income........... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(monthlyIncome))).append("\n");
		sb.append("    Contract Payments.... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(contracts))).append("\n\n");
		sb.append("Monthly Expenses......... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(monthlyExpenses))).append("\n");
		sb.append("    Salaries............. ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(salaries))).append("\n");
		sb.append("    Maintenance.......... ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(maintenance))).append("\n");
		sb.append("    Overhead............. ")
				.append(String.format(formatted, DecimalFormat.getInstance()
						.format(overhead))).append("\n");

		return new String(sb);
	}

	public void setHealingTimeOptions(int newHeal, int newNaturalHeal) {
		// we need to check the current values and then if necessary change the
		// times for all
		// personnel, giving them credit for their current waiting time
		int currentHeal = getCampaignOptions().getHealingWaitingPeriod();
		int currentNaturalHeal = getCampaignOptions()
				.getNaturalHealingWaitingPeriod();

		getCampaignOptions().setHealingWaitingPeriod(newHeal);
		getCampaignOptions().setNaturalHealingWaitingPeriod(newNaturalHeal);

		int healDiff = newHeal - currentHeal;
		int naturalDiff = newNaturalHeal - currentNaturalHeal;

		if (healDiff != 0 || naturalDiff != 0) {
			for (Person p : getPersonnel()) {
				if (p.getDoctorId() != null) {
					p.setDaysToWaitForHealing(Math.max(
							p.getDaysToWaitForHealing() + healDiff, 1));
				} else {
					p.setDaysToWaitForHealing(Math.max(
							p.getDaysToWaitForHealing() + naturalDiff, 1));
				}
			}
		}
	}
	
	public int getTotalMechBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getMechCapacity();
		}
		return bays;
	}
	
	public int getTotalASFBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getASFCapacity();
		}
		return bays;
	}
	
	public int getTotalSmallCraftBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getSmallCraftCapacity();
		}
		return bays;
	}
	
	public int getTotalBattleArmorBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getBattleArmorCapacity();
		}
		return bays;
	}
	
	public int getTotalInfantryBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getInfantryCapacity();
		}
		return bays;
	}
	
	public int getTotalHeavyVehicleBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getHeavyVehicleCapacity();
		}
		return bays;
	}
	
	public int getTotalLightVehicleBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getLightVehicleCapacity();
		}
		return bays;
	}
	
	public int getTotalProtomechBays() {
		int bays = 0;
		for (Unit u : getUnits()) {
			bays += u.getProtomechCapacity();
		}
		return bays;
	}
	
	public int getTotalDockingCollars() {
		int collars = 0;
		for (Unit u : getUnits()) {
			if (u.getEntity() instanceof Jumpship) {
				collars += u.getDocks();
			}
		}
		return collars;
	}

	public double getTotalInsulatedCargoCapacity() {
		double capacity = 0;
		for (UUID id : getForces().getAllUnits()) {
			Unit u = getUnit(id);
			capacity += u.getInsulatedCargoCapacity();
		}
		return capacity;
	}

	public double getTotalRefrigeratedCargoCapacity() {
		double capacity = 0;
		for (UUID id : getForces().getAllUnits()) {
			Unit u = getUnit(id);
			capacity += u.getRefrigeratedCargoCapacity();
		}
		return capacity;
	}

	public double getTotalLivestockCargoCapacity() {
		double capacity = 0;
		for (UUID id : getForces().getAllUnits()) {
			Unit u = getUnit(id);
			capacity += u.getLivestockCargoCapacity();
		}
		return capacity;
	}

	public double getTotalLiquidCargoCapacity() {
		double capacity = 0;
		for (UUID id : getForces().getAllUnits()) {
			Unit u = getUnit(id);
			capacity += u.getLiquidCargoCapacity();
		}
		return capacity;
	}

	public double getTotalCargoCapacity() {
		double capacity = 0;
		for (UUID id : getForces().getAllUnits()) {
			Unit u = getUnit(id);
			capacity += u.getCargoCapacity();
		}
		return capacity;
	}
	
	// Liquid not included
	public double getTotalCombinedCargoCapacity() {
		return getTotalCargoCapacity() + getTotalLivestockCargoCapacity()
				+ getTotalInsulatedCargoCapacity() + getTotalRefrigeratedCargoCapacity();
	}
	
	public int getNumberOfUnitsByType(long type) {
		return getNumberOfUnitsByType(type, false, false);
	}
	
	public int getNumberOfUnitsByType(long type, boolean inTransit) {
		return getNumberOfUnitsByType(type, inTransit, false);
	}
	
	public int getNumberOfUnitsByType(long type, boolean inTransit, boolean lv) {
		int num = 0;
		for (Unit unit : getUnits()) {
			if (!inTransit && !unit.isPresent()) {
				continue;
			}
			Entity en = unit.getEntity();
			if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
				continue;
			}
			if (type == Entity.ETYPE_MECH && en instanceof Mech) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft
					&& !(en instanceof Dropship)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_AERO && en instanceof Aero
					&& !(en instanceof SmallCraft || en instanceof ConvFighter)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_TANK && en instanceof Tank) {
				if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
					num++;
				}
				continue;
			}
			if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
				num++;
				continue;
			}
		}
		
		return num;
	}

	public double getCargoTonnage(boolean inTransit) {
		double cargoTonnage = 0;
		int mechs = getNumberOfUnitsByType(Entity.ETYPE_MECH);
		int ds = getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
		int sc = getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
		int cf = getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
		int asf = getNumberOfUnitsByType(Entity.ETYPE_AERO);
		int inf = getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
		int ba = getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
		int lv = getNumberOfUnitsByType(Entity.ETYPE_TANK, true);
		int hv = getNumberOfUnitsByType(Entity.ETYPE_TANK, false);
		int protos = getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);
		
		for (Part part : getSpareParts()) {
			if (!inTransit && !part.isPresent()) {
				continue;
			}
			cargoTonnage += (part.getQuantity() * part.getTonnage());
		}
		
		// place units in bays, with remainder going to cargo.
		for (Unit unit : getUnits()) {
			if (!inTransit && !unit.isPresent()) {
				continue;
			}
			Entity en = unit.getEntity();
			if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
				continue;
			}
			if (mechs > 0 && en instanceof Mech) {
				mechs--;
				continue;
			}
			if (ds > 0 && en instanceof Dropship) {
				ds--;
				continue;
			}
			if (sc > 0 && en instanceof SmallCraft && !(en instanceof Dropship)) {
				sc--;
				continue;
			}
			if (cf > 0 && en instanceof ConvFighter) {
				cf--;
				continue;
			}
			if (asf > 0 && en instanceof Aero
					&& !(en instanceof SmallCraft || en instanceof ConvFighter)) {
				asf--;
				continue;
			}
			if (inf > 0 && en instanceof Infantry && !(en instanceof BattleArmor)) {
				inf--;
				continue;
			}
			if (ba > 0 && en instanceof BattleArmor) {
				ba--;
				continue;
			}
			if (lv > 0 && en instanceof Tank && !(en instanceof GunEmplacement) && en.getWeight() <= 50) {
				lv--;
				continue;
			}
			if (hv > 0 && en instanceof Tank && !(en instanceof GunEmplacement) && en.getWeight() > 50) {
				hv--;
				continue;
			}
			if (protos > 0 && en instanceof Protomech) {
				protos--;
				continue;
			}
			cargoTonnage += unit.getEntity().getWeight();
		}
		return cargoTonnage;
	}
	
	public int getOccupiedBays(long type) {
		return getOccupiedBays(type, false);
	}
	
	public int getOccupiedBays(long type, boolean lv) {
		int num = 0;
		for (Unit unit : getUnits()) {
			Entity en = unit.getEntity();
			if (en instanceof GunEmplacement || en instanceof Jumpship) {
				continue;
			}
			if (type == Entity.ETYPE_MECH && en instanceof Mech) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft && !(en instanceof Dropship)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_AERO && en instanceof Aero
					&& !(en instanceof SmallCraft || en instanceof ConvFighter)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
				num++;
				continue;
			}
			if (type == Entity.ETYPE_TANK && en instanceof Tank) {
				if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
					num++;
				}
				continue;
			}
			if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
				num++;
				continue;
			}
		}
		
		if (type == Entity.ETYPE_MECH) {
			if (getTotalMechBays() > num) {
				return num;
			}
			return getTotalMechBays();
		}
		
		if (type == Entity.ETYPE_AERO) {
			if (getTotalASFBays() > num) {
				return num;
			}
			return getTotalASFBays();
		}
		
		if (type == Entity.ETYPE_INFANTRY) {
			if (getTotalInfantryBays() > num) {
				return num;
			}
			return getTotalInfantryBays();
		}
		
		if (type == Entity.ETYPE_BATTLEARMOR) {
			if (getTotalBattleArmorBays() > num) {
				return num;
			}
			return getTotalBattleArmorBays();
		}
		
		if (type == Entity.ETYPE_TANK) {
			if (lv) {
				if (getTotalLightVehicleBays() > num) {
					return num;
				}
				return getTotalLightVehicleBays();
			}
			if (getTotalHeavyVehicleBays() > num) {
				return num;
			}
			return getTotalHeavyVehicleBays();
		}
		
		if (type == Entity.ETYPE_SMALL_CRAFT) {
			if (getTotalSmallCraftBays() > num) {
				return num;
			}
			return getTotalSmallCraftBays();
		}
		
		if (type == Entity.ETYPE_PROTOMECH) {
			if (getTotalProtomechBays() > num) {
				return num;
			}
			return getTotalProtomechBays();
		}
		
		if (type == Entity.ETYPE_DROPSHIP) {
			if (getTotalDockingCollars() > num) {
				return num;
			}
			return getTotalDockingCollars();
		}
		
		return -1; // default, this is an error condition
	}
	
	public String getTransportDetails() {
		int noMech = Math.max(getNumberOfUnitsByType(Entity.ETYPE_MECH) - getOccupiedBays(Entity.ETYPE_MECH), 0);
		int noDS = Math.max(getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP) - getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
		int noSC = Math.max(getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT) - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
		int noCF = Math.max(getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER) - getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
		int noASF = Math.max(getNumberOfUnitsByType(Entity.ETYPE_AERO) - getOccupiedBays(Entity.ETYPE_AERO), 0);
		int nolv = Math.max(getNumberOfUnitsByType(Entity.ETYPE_TANK, true) - getOccupiedBays(Entity.ETYPE_TANK, true), 0);
		int nohv = Math.max(getNumberOfUnitsByType(Entity.ETYPE_TANK) - getOccupiedBays(Entity.ETYPE_TANK), 0);
		int noinf = Math.max(getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
		int noBA = Math.max(getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
		int noProto = Math.max(getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH) - getOccupiedBays(Entity.ETYPE_PROTOMECH), 0);
		int freehv = Math.max(getTotalHeavyVehicleBays() - getOccupiedBays(Entity.ETYPE_TANK), 0);
		int freeinf = Math.max(getTotalInfantryBays() - getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
		int freeba = Math.max(getTotalBattleArmorBays() - getOccupiedBays(Entity.ETYPE_TANK), 0);
		int freeSC = Math.max(getTotalSmallCraftBays() - getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
		
		String asfAppend = "";
		int newNoASF = Math.max(noASF - freeSC, 0);
		int placedASF = Math.max(noASF - newNoASF, 0);
		if (noASF > 0 && freeSC > 0) {
			asfAppend = " ["+placedASF+" ASF will be placed in Small Craft bays]";
		}
		
		if (nolv > 0 && freehv > 0) {
			
		}
		
		if (noBA > 0 && freeinf > 0) {
			
		}
		
		if (noinf > 0 && freeba > 0) {
			
		}
		
		StringBuffer sb = new StringBuffer("Transports\n\n");
		
		// Lets do Mechs first.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Mech Bays (Occupied):",
				getTotalMechBays(), getOccupiedBays(Entity.ETYPE_MECH), "Mechs Not Transported:", noMech));
		
		// Lets do ASF next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d%s\n", "ASF Bays (Occupied):",
				getTotalASFBays(), getOccupiedBays(Entity.ETYPE_AERO), "ASF Not Transported:", noASF, asfAppend));
		
		// Lets do Light Vehicles next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Light Vehicle Bays (Occupied):",
				getTotalLightVehicleBays(), getOccupiedBays(Entity.ETYPE_TANK), "Light Vehicles Not Transported:", nolv));
		
		// Lets do Heavy Vehicles next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Heavy Vehicle Bays (Occupied):",
				getTotalHeavyVehicleBays(), getOccupiedBays(Entity.ETYPE_TANK), "Heavy Vehicles Not Transported:", nohv));
		
		if (nolv > 0 && freehv > 0) {
			
		}
		
		// Lets do Infantry next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Infantry Bays (Occupied):",
				getTotalInfantryBays(), getOccupiedBays(Entity.ETYPE_INFANTRY), "Infantry Not Transported:", noinf));
		
		if (noBA > 0 && freeinf > 0) {
			
		}
		
		// Lets do Battle Armor next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Battle Armor Bays (Occupied):",
				getTotalBattleArmorBays(), getOccupiedBays(Entity.ETYPE_BATTLEARMOR), "Battle Armor Not Transported:", noBA));
		
		if (noinf > 0 && freeba > 0) {
			
		}
		
		// Lets do Small Craft next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Small Craft Bays (Occupied):",
				getTotalSmallCraftBays(), getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), "Small Craft Not Transported:", noSC));
		
		if (noASF > 0 && freeSC > 0) {
			// Lets do ASF in Free Small Craft Bays next.
			sb.append(String.format("%-35s   %4d (%4d)      %-35s     %4d\n", "   ASF in Small Craft Bays (Occupied):",
					getTotalSmallCraftBays(), getOccupiedBays(Entity.ETYPE_SMALL_CRAFT)+placedASF, "ASF Not Transported:", newNoASF));
		}
		
		// Lets do Protomechs next.
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Protomech Bays (Occupied):",
				getTotalProtomechBays(), getOccupiedBays(Entity.ETYPE_PROTOMECH), "Protomechs Not Transported:", noSC));
		
		sb.append("\n\n");
		
		sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Docking Collars (Occupied):",
				getTotalDockingCollars(), getOccupiedBays(Entity.ETYPE_DROPSHIP), "Dropships Not Transported:", noDS));
		
		return new String(sb);
	}

	public String getCombatPersonnelDetails() {
		int[] countPersonByType = new int[Person.T_SPACE_GUNNER + 1];
		int countTotal = 0;
		int countInjured = 0;
		int countMIA = 0;
		int countKIA = 0;
		int countRetired = 0;
		long salary = 0;

		for (Person p : getPersonnel()) {
			// Add them to the total count
			if (p.getPrimaryRole() <= Person.T_SPACE_GUNNER && !p.isPrisoner()
					&& !p.isBondsman() && p.isActive()) {
				countPersonByType[p.getPrimaryRole()]++;
				countTotal++;
				if (getCampaignOptions().useAdvancedMedical()
						&& p.getInjuries().size() > 0) {
					countInjured++;
				} else if (p.getHits() > 0) {
					countInjured++;
				}
				salary += p.getSalary();
			} else if (p.getPrimaryRole() <= Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_RETIRED) {
				countRetired++;
			} else if (p.getPrimaryRole() <= Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_MIA) {
				countMIA++;
			} else if (p.getPrimaryRole() <= Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_KIA) {
				countKIA++;
			}
		}

		StringBuffer sb = new StringBuffer("Combat Personnel\n\n");

		String buffer = "";

		buffer = String.format("%-30s        %4s\n", "Total Combat Personnel",
				countTotal);
		sb.append(buffer);

		for (int i = Person.T_NONE + 1; i <= Person.T_SPACE_GUNNER; i++) {
			buffer = String.format("    %-30s    %4s\n", Person.getRoleDesc(i),
					countPersonByType[i]);
			sb.append(buffer);
		}

		buffer = String.format("%-30s        %4s\n",
				"Injured Combat Personnel", countInjured);
		sb.append("\n" + buffer);
		buffer = String.format("%-30s        %4s\n", "MIA Combat Personnel",
				countMIA);
		sb.append(buffer);
		buffer = String.format("%-30s        %4s\n", "KIA Combat Personnel",
				countKIA);
		sb.append(buffer);
		buffer = String.format("%-30s        %4s\n",
				"Retired Combat Personnel", countRetired);
		sb.append(buffer);

		sb.append("\nMonthly Salary For Combat Personnel: " + salary);

		return new String(sb);
	}

	public String getSupportPersonnelDetails() {
		int[] countPersonByType = new int[Person.T_NUM];
		int countTotal = 0;
		int countInjured = 0;
		int countMIA = 0;
		int countKIA = 0;
		int countRetired = 0;
		long salary = 0;
		int prisoners = 0;
		int bondsmen = 0;

		for (Person p : getPersonnel()) {
			// Add them to the total count
			if (p.getPrimaryRole() > Person.T_SPACE_GUNNER && !p.isPrisoner()
					&& !p.isBondsman() && p.isActive()) {
				countPersonByType[p.getPrimaryRole()]++;
				countTotal++;
				if (p.getInjuries().size() > 0 || p.getHits() > 0) {
					countInjured++;
				}
				salary += p.getSalary();
			} else if (p.isPrisoner() && p.isActive()) {
				prisoners++;
				if (p.getInjuries().size() > 0 || p.getHits() > 0) {
					countInjured++;
				}
			} else if (p.isBondsman() && p.isActive()) {
				bondsmen++;
				if (p.getInjuries().size() > 0 || p.getHits() > 0) {
					countInjured++;
				}
			} else if (p.getPrimaryRole() > Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_RETIRED) {
				countRetired++;
			} else if (p.getPrimaryRole() > Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_MIA) {
				countMIA++;
			} else if (p.getPrimaryRole() > Person.T_SPACE_GUNNER
					&& p.getStatus() == Person.S_KIA) {
				countKIA++;
			}
		}

		StringBuffer sb = new StringBuffer("Support Personnel\n\n");

		String buffer = "";

		buffer = String.format("%-30s        %4s\n", "Total Support Personnel",
				countTotal);
		sb.append(buffer);

		for (int i = Person.T_NAVIGATOR; i < Person.T_NUM; i++) {
			buffer = String.format("    %-30s    %4s\n", Person.getRoleDesc(i),
					countPersonByType[i]);
			sb.append(buffer);
		}

		buffer = String.format("%-30s        %4s\n",
				"Injured Support Personnel", countInjured);
		sb.append("\n" + buffer);
		buffer = String.format("%-30s        %4s\n", "MIA Support Personnel",
				countMIA);
		sb.append(buffer);
		buffer = String.format("%-30s        %4s\n", "KIA Support Personnel",
				countKIA);
		sb.append(buffer);
		buffer = String.format("%-30s        %4s\n",
				"Retired Support Personnel", countRetired);
		sb.append(buffer);

		sb.append("\nMonthly Salary For Support Personnel: " + salary);

		sb.append(String.format("\nYou have " + prisoners + " prisoner%s",
				prisoners == 1 ? "" : "s"));
		sb.append(String.format("\nYou have " + bondsmen + " %s",
				bondsmen == 1 ? "bondsman" : "bondsmen"));

		return new String(sb);
	}

	public void doMaintenance(Unit u) {
		if (!u.requiresMaintenance()) {
			return;
		}
		// lets start by checking times
		Person tech = u.getTech();
		int minutesUsed = u.getMaintenanceTime();
		int astechsUsed = getAvailableAstechs(minutesUsed, false);
		boolean maintained = null != tech
				&& tech.getMinutesLeft() > minutesUsed;
		boolean paidMaintenance = true;
		if (maintained) {
			// use the time
			tech.setMinutesLeft(tech.getMinutesLeft() - minutesUsed);
			astechPoolMinutes -= astechsUsed * minutesUsed;
		}
		u.incrementDaysSinceMaintenance(maintained, astechsUsed);
		if (getCampaignOptions().checkMaintenance()
				&& u.getDaysSinceMaintenance() >= getCampaignOptions()
						.getMaintenanceCycleDays()) {
			// its time for a maintenance check
			int qualityOrig = u.getQuality();
			String techName = "Nobody";
			if (null != tech) {
				techName = tech.getName();
				//maybe use the money
	            if (campaignOptions.payForMaintain()) {
	                if (finances.debit(u.getMaintenanceCost(),
	                        Transaction.C_MAINTAIN, "Maintenance for " + u.getName(),
	                        calendar.getTime())) {
	                } else {
	                    paidMaintenance = false;
	                }
	            }
			}
			// dont do actual damage until we clear the for loop to avoid
			// concurrent mod problems
			// put it into a hash - 4 points of damage will mean destruction
			HashMap<Integer, Integer> partsToDamage = new HashMap<Integer, Integer>();
			for (Part p : u.getParts()) {
				String partReport = u.getName() + ": " + techName
						+ " maintaining " + p.getName() + " (Quality "
						+ p.getQualityName() + ")";
				if (!p.needsMaintenance()) {
					continue;
				}
				TargetRoll target = getTargetForMaintenance(p, tech);
				if(!paidMaintenance) {
				    //I should probably make this modifier user inputtable
				    target.addModifier(1, "did not pay maintenance");
				}
				partReport += ", TN " + target.getValue() + "["
						+ target.getDesc() + "]";
				int roll = Compute.d6(2);
				int margin = roll - target.getValue();
				partReport += " rolled a " + roll + ", margin of " + margin;
				switch (p.getQuality()) {
				case Part.QUALITY_F:
					if (margin < -2) {
						p.decreaseQuality();
						if (margin < -6) {
							partsToDamage.put(p.getId(), 1);
						}
					}
					if (margin >= 6) {
						// TODO: award XP point (make this optional)
					}
					break;
				case Part.QUALITY_E:
					if (margin < -2) {
						p.decreaseQuality();
						if (margin < -5) {
							partsToDamage.put(p.getId(), 1);
						}
					}
					if (margin >= 6) {
						p.improveQuality();
					}
					break;
				case Part.QUALITY_D:
					if (margin < -3) {
						p.decreaseQuality();
						if (margin < -4) {
							partsToDamage.put(p.getId(), 1);
						}
					}
					if (margin >= 5) {
						p.improveQuality();
					}
					break;
				case Part.QUALITY_C:
					if (margin < -6) {
						partsToDamage.put(p.getId(), 2);
					} else if (margin < -3) {
						partsToDamage.put(p.getId(), 1);
					}
					if (margin < -4) {
						p.decreaseQuality();
					}
					if (margin >= 5) {
						p.improveQuality();
					}
					break;
				case Part.QUALITY_B:
					if (margin < -6) {
						partsToDamage.put(p.getId(), 2);
					} else if (margin < -2) {
						partsToDamage.put(p.getId(), 1);
					}
					if (margin < -5) {
						p.decreaseQuality();
					}
					if (margin >= 4) {
						p.improveQuality();
					}
					break;
				case Part.QUALITY_A:
					if (margin < -6) {
						partsToDamage.put(p.getId(), 4);
					} else if (margin < -4) {
						partsToDamage.put(p.getId(), 3);
					} else if (margin == -4) {
						partsToDamage.put(p.getId(), 2);
					} else if (margin < -1) {
						partsToDamage.put(p.getId(), 1);
					}
					if (margin >= 4) {
						p.improveQuality();
					}
					break;
				}
				partReport += ": new quality is " + p.getQualityName();
				if (null != partsToDamage.get(p.getId())) {
					if (partsToDamage.get(p.getId()) > 3) {
						partReport += ", part destroyed";
					} else {
						partReport += ", part damaged";
					}
				}
				if (getCampaignOptions().logMaintenance()) {
					MekHQ.logMessage(partReport);
				}
			}
			String damageList = "";
			String destroyList = "";
			for (int key : partsToDamage.keySet()) {
				Part p = getPart(key);
				if (null != p) {
					int damage = partsToDamage.get(key);
					if (damage > 3) {
						destroyList += p.getName() + ", ";
						p.remove(false);
					} else {
						p.doMaintenanceDamage(damage);
						damageList += p.getName() + ", ";
					}
				}
			}
			damageList = damageList.replaceAll(", $", "");
			destroyList = destroyList.replaceAll(", $", "");

			u.resetDaysSinceMaintenance();
			int quality = u.getQuality();
			String qualityString = "";
			if (quality > qualityOrig) {
				qualityString = "<font color='green'>Overall quality improves from "
						+ Part.getQualityName(qualityOrig)
						+ " to "
						+ Part.getQualityName(quality) + "</font>";
			} else if (quality < qualityOrig) {
				qualityString = "<font color='red'>Overall quality declines from "
						+ Part.getQualityName(qualityOrig)
						+ " to "
						+ Part.getQualityName(quality) + "</font>";
			} else {
				qualityString = "Overall quality remains "
						+ Part.getQualityName(quality);
			}
			String damageString = "";
			if (!damageList.isEmpty()) {
				damageString += "Damage was suffered to " + damageList + ". ";
			}
			if (!destroyList.isEmpty()) {
				damageString += "The following parts were destroyed: "
						+ destroyList + ".";
			}
			if (!damageString.isEmpty()) {
				damageString = "<b><font color='red'>" + damageString
						+ "</b></font>";
			}
			String paidString = "";
			if(!paidMaintenance) {
			    paidString = "<font color='red'>Could not afford maintenance costs, so check is at a penalty.</font>";
			}
			addReport(techName + " performs maintenance on " + u.getName()
					+ ". " + paidString + qualityString + ". " + damageString);
		}
	}
}
