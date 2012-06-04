/*
 * ResolveScenarioTracker.java
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

import gd.xml.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import megamek.client.Client;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.IArmorState;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Tank;
import megamek.common.XMLStreamParser;
import mekhq.MekHQ;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
 * This object will be the main workforce for the scenario
 * resolution wizard. It will keep track of information and be 
 * fed back and forth between the various wizards
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ResolveScenarioTracker {
	
	Hashtable<UUID, Entity> entities;
	Hashtable<UUID, Pilot> pilots;
	ArrayList<Entity> potentialSalvage;
	ArrayList<Unit> actualSalvage;
	ArrayList<Unit> leftoverSalvage;
	ArrayList<Unit> units;
	Hashtable<UUID, PersonStatus> peopleStatus;
	Hashtable<String, String> killCredits;
	Campaign campaign;
	Scenario scenario;
	JFileChooser unitList;
	JFileChooser salvageList;
	Client client;
	
	public ResolveScenarioTracker(Scenario s, Campaign c) {
		this.scenario = s;
		this.campaign = c;
		entities = new Hashtable<UUID, Entity>();
		potentialSalvage = new ArrayList<Entity>();
		actualSalvage = new ArrayList<Unit>();
		leftoverSalvage = new ArrayList<Unit>();
		pilots = new Hashtable<UUID, Pilot>();
		units = new ArrayList<Unit>();
		peopleStatus = new Hashtable<UUID, PersonStatus>();
		killCredits = new Hashtable<String, String>();
		for(UUID uid : scenario.getForces(campaign).getAllUnits()) {
			Unit u = campaign.getUnit(uid);
			if(null != u) {
				units.add(u);
			}
		}
		unitList = new JFileChooser(".");
		unitList.setDialogTitle("Load Units");
		
		unitList.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File dir) {
				if (dir.isDirectory()) {
					return true;
				}
				return dir.getName().endsWith(".mul");
			}

			@Override
			public String getDescription() {
				return "MUL file";
			}
		});
		
		salvageList = new JFileChooser(".");
		salvageList.setDialogTitle("Load Units");
		
		salvageList.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File dir) {
				if (dir.isDirectory()) {
					return true;
				}
				return dir.getName().endsWith(".mul");
			}

			@Override
			public String getDescription() {
				return "MUL file";
			}
		});
	}
	
	public void findUnitFile() {
		unitList.showOpenDialog(null);
	}
	
	public String getUnitFilePath() {
		File unitFile = unitList.getSelectedFile();
		if(null == unitFile) {
			return "No file selected";
		} else {
			return unitFile.getAbsolutePath();
		}
	}
	
	public void findSalvageFile() {
		salvageList.showOpenDialog(null);
	}
	
	public String getSalvageFilePath() {
		File salvageFile = salvageList.getSelectedFile();
		if(null == salvageFile) {
			return "No file selected";
		} else {
			return salvageFile.getAbsolutePath();
		}
	}
	
	public void setClient(Client c) {
		client = c;
	}
	
	public void processMulFiles(boolean controlsField) {
		File unitFile = unitList.getSelectedFile();
		File salvageFile = salvageList.getSelectedFile();
		if(null != unitFile) {
			try {
				loadUnitsAndPilots(unitFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(null != salvageFile && controlsField) {
			try {
				loadSalvage(salvageFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		checkStatusOfPersonnel();
	}
	
	public void processGame(boolean controlsField) {

		int pid = client.getLocalPlayer().getId();
		
		for (Enumeration<Entity> iter = client.game.getEntities(); iter.hasMoreElements();) {
			Entity e = iter.nextElement();
			if(e.getOwnerId() == pid) {
				if(e.canEscape() || controlsField) {
					if(!e.getExternalIdAsString().equals("-1")) {
						entities.put(UUID.fromString(e.getExternalIdAsString()), e);
					}
					if(null != e.getCrew()) {
						if(!e.getCrew().getExternalIdAsString().equals("-1")) {
							pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
						}
					}
				}			
			} else if(e.getOwner().isEnemyOf(client.getLocalPlayer())) {
				if(!e.canEscape() && controlsField) {
					killCredits.put(e.getDisplayName(), "None");
					if(e instanceof Infantry && !(e instanceof BattleArmor)) {
						continue;
					}
					potentialSalvage.add(e);
				}
			}
		}	
		//add retreated units
		for (Enumeration<Entity> iter = client.game.getRetreatedEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if(e.getOwnerId() == pid) {
            	if(!e.getExternalIdAsString().equals("-1")) {
					entities.put(UUID.fromString(e.getExternalIdAsString()), e);
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
					}
				}
            }
        }
		

        Enumeration<Entity> wrecks = client.game.getWreckedEntities();
        while (wrecks.hasMoreElements()) {
        	Entity e = wrecks.nextElement();
        	if(e.getOwnerId() == pid && controlsField && e.isSalvage()) {
        		if(!e.getExternalIdAsString().equals("-1")) {
					entities.put(UUID.fromString(e.getExternalIdAsString()), e);
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
					}
				}
        	} else if(e.getOwner().isEnemyOf(client.getLocalPlayer())) {
        		Entity killer = client.game.getEntity(e.getKillerId());
        		if(null != killer && killer.getOwnerId() == pid) {
        			//the killer is one of your units, congrats!
        			killCredits.put(e.getDisplayName(), killer.getExternalIdAsString());
        		} else {
        			killCredits.put(e.getDisplayName(), "None");
        		}
        		if(e.isSalvage()) {
        			if(e instanceof Infantry && !(e instanceof BattleArmor)) {
						continue;
					}
        			potentialSalvage.add(e);
        		}
        	}
        }       
        checkStatusOfPersonnel();
	}

	private void checkForEquipmentStatus(Entity en, boolean controlsField) {
		Unit u = null;
		if(!en.getExternalIdAsString().equals("-1")) {
			UUID id = UUID.fromString(en.getExternalIdAsString());
			if(null != id) {
				u = campaign.getUnit(id);
			}
		}
		ArrayList<String> brokenParts = new ArrayList<String>();
		for(int loc = 0; loc < en.locations(); loc++) {
			if(en.isLocationBlownOff(loc) && !controlsField) {
				//sorry dude, we cant find your arm
				en.setLocationBlownOff(loc, false);
				en.setArmor(IArmorState.ARMOR_DESTROYED, loc);
				en.setInternal(IArmorState.ARMOR_DESTROYED, loc);
			}
			for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
				final CriticalSlot cs = en.getCritical(loc, i);
				if(null == cs || !cs.isEverHittable()) {
					continue;
				}
				if(cs.isMissing() && !controlsField) {
					//equipment in this location got left with the 
					//limb
					cs.setRepairable(false);
					cs.setDestroyed(true);
					cs.setMissing(false);
					Mounted m = cs.getMount();
		            if(null != m) {
		            	m.setMissing(false);
		            	m.setDestroyed(true);
		            	m.setRepairable(false);
		            }
				} 
				if(cs.isDamaged()) {
					if(cs.getIndex() == Mech.ACTUATOR_SHOULDER 
							|| cs.getIndex() == Mech.ACTUATOR_HIP
							|| cs.getIndex() == Mech.SYSTEM_ENGINE) {
						continue;
					}
					//we have to do this little hack-y thing to account for actuators which are not
					//uniquely identified without location
					String strIndex = Integer.toString(cs.getIndex());
					//check to make sure this equipment wasnt already damaged
					if(null != u) {
						Part p = u.getPartForCriticalSlot(cs.getIndex(), loc);
						if(null != p && p.getHits() > 0) {
							continue;
						}
					}
					if(cs.getIndex() >= Mech.ACTUATOR_UPPER_ARM && cs.getIndex() <= Mech.ACTUATOR_FOOT) {
						strIndex += ":" + loc;
					}
					if(!brokenParts.contains(strIndex) && Compute.d6(2) < 10) {
						cs.setRepairable(false);
						cs.setDestroyed(true);
						cs.setMissing(false);
						Mounted m = cs.getMount();
			            if(null != m) {
			            	m.setMissing(false);
			            	m.setDestroyed(true);
			            	m.setRepairable(false);
			            }
			            brokenParts.add(strIndex);
			            //we dont care that we wont flag all the critical slots. Flagging one
			            //and the mounted should do the trick
					} 
					
				}
			}
		}
	}
	
	public void postProcessEntities(boolean controlsField) {
		for(UUID id : entities.keySet()) {
			Entity en = entities.get(id);
			if(null == en) {
				continue;
			}
			checkForEquipmentStatus(en, controlsField);
		}
		for(Entity en : potentialSalvage) {
			checkForEquipmentStatus(en, controlsField);
		}
	}
	
	private ArrayList<Person> shuffleCrew(ArrayList<Person> source) {
	    ArrayList<Person> sortedList = new ArrayList<Person>();
	    Random generator = new Random();

	    while (source.size() > 0)
	    {
	        int position = generator.nextInt(source.size());
	        sortedList.add(source.get(position));
	        source.remove(position);
	    }

	    return sortedList;
	}
	
	public void assignKills() {
		for(Unit u : units) {
			for(String killed : killCredits.keySet()) {
				if(killCredits.get(killed).equalsIgnoreCase("none")) {
					continue;
				}
				if(u.getId().toString().equals(killCredits.get(killed))) {
					for(Person p : u.getActiveCrew()) {
						PersonStatus status = peopleStatus.get(p.getId());
						status.addKill(new Kill(p.getId(), killed, u.getEntity().getShortNameRaw(), campaign.getCalendar().getTime()));
					}
				}
			}
			
		}
	}
	
	public void checkStatusOfPersonnel() {
		//for single-crewed units, we can check pilot directly, otherwise we need to check
		//the unit and associated entity
		
		//lets cycle through units and get their crew
		PersonStatus status;
		for(Unit u : units) {
			//shuffling the crew ensures that casualties are randomly assigned in multi-crew units
			ArrayList<Person> crew = shuffleCrew(u.getActiveCrew());
			Entity en = entities.get(u.getId());
			int casualties = 0;
			int casualtiesAssigned = 0;
			if(null != en && en instanceof Infantry && u.getEntity() instanceof Infantry) {
				en.applyDamage();
				casualties = crew.size() - ((Infantry)en).getShootingStrength();
			}
			for(Person p : crew) {
				status = new PersonStatus(p.getName(), u.getEntity().getDisplayName(), p.getHits());			
				if(u.usesSoloPilot()) {
					Pilot pilot = pilots.get(p.getId());
					if(null == pilot) {
						status.setMissing(true);
					} else {
						status.setHits(pilot.getHits());
					}
				} else {
					//we have a multi-crewed vee				
					if(null == en) {
						status.setMissing(true);				
					} else {
						if(en instanceof Tank) {
							boolean destroyed = false;
							for(int loc = 0; loc < en.locations(); loc++) {
								if(loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2 || loc == Tank.LOC_BODY) {
									continue;
								}
								if(en.getInternal(loc) <= 0) {
									destroyed = true;
									break;
								} 
							}
							if(destroyed || null == en.getCrew() || en.getCrew().isDead()) {
								if(Compute.d6(2) >= 7) {
									status.setHits(1);
								} else {
									status.setHits(6);
								}
							}
							else if(((Tank)en).isDriverHit() && u.isDriver(p)) {
								if(Compute.d6(2) >= 7) {
									status.setHits(1);
								} else {
									status.setHits(6);
								}
							}
							else if(((Tank)en).isCommanderHit() && u.isCommander(p)) {
								if(Compute.d6(2) >= 7) {
									status.setHits(1);
								} else {
									status.setHits(6);
								}
							}
						}
						else if(en instanceof Infantry) {
							if(casualtiesAssigned < casualties) {
								casualtiesAssigned++;
								if(Compute.d6(2) >= 7) {
									status.setHits(1);
								} else {
									status.setHits(6);
								}
							}
						}
					}
				}
				status.setXP(campaign.getCampaignOptions().getScenarioXP());
				peopleStatus.put(p.getId(), status);
			}
		}
	}
	
	private void loadUnitsAndPilots(File unitFile) throws IOException {
		
		if (unitFile != null) {
			// I need to get the parser myself, because I want to pull both
			// entities and pilots from it
			// Create an empty parser.
			XMLStreamParser parser = new XMLStreamParser();

			// Open up the file.
			InputStream listStream = new FileInputStream(unitFile);
			// Read a Vector from the file.
			try {

				parser.parse(listStream);
				listStream.close();
			} catch (ParseException excep) {
				excep.printStackTrace(System.err);
				// throw new IOException("Unable to read from: " +
				// unitFile.getName());
			}

			// Was there any error in parsing?
			if (parser.hasWarningMessage()) {
				MekHQ.logMessage(parser.getWarningMessage());
			}

			// Add the units from the file.
			for (Entity entity : parser.getEntities()) {
				entities.put(UUID.fromString(entity.getExternalIdAsString()), entity);
			}
			
			// add any ejected pilots
			for (Pilot pilot : parser.getPilots()) {
				pilots.put(UUID.fromString(pilot.getExternalIdAsString()), pilot);
			}
		}
	}
	
	private void loadSalvage(File salvageFile) throws IOException {
		if (salvageFile != null) {
			// I need to get the parser myself, because I want to pull both
			// entities and pilots from it
			// Create an empty parser.
			XMLStreamParser parser = new XMLStreamParser();

			// Open up the file.
			InputStream listStream = new FileInputStream(salvageFile);
			// Read a Vector from the file.
			try {
				parser.parse(listStream);
				listStream.close();
			} catch (ParseException excep) {
				excep.printStackTrace(System.err);
				// throw new IOException("Unable to read from: " +
				// unitFile.getName());
			}

			// Was there any error in parsing?
			if (parser.hasWarningMessage()) {
				MekHQ.logMessage(parser.getWarningMessage());
			}

			// Add the units from the file.		
			for (Entity entity : parser.getEntities()) {
				//dont allow the salvaging of conventional infantry
				if(entity instanceof Infantry && !(entity instanceof BattleArmor)) {
					continue;
				}
				//some of the players units and personnel may be in the salvage pile, so 
				//lets check for these first
				if(!entity.getExternalIdAsString().equals("-1") && foundMatch(entity, units)) {
					entities.put(UUID.fromString(entity.getExternalIdAsString()), entity);
					if(null != entity.getCrew()) {
						pilots.put(UUID.fromString(entity.getCrew().getExternalIdAsString()), entity.getCrew());
					}
				} else {		
					potentialSalvage.add(entity);
				}
			}
		}
	}
	
	private boolean foundMatch(Entity en, ArrayList<Unit> units) {
		for(Unit u : units) {
			if(u.getId().equals(UUID.fromString(en.getExternalIdAsString()))) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Entity> getPotentialSalvage() {
		return potentialSalvage;
	}
	
	public ArrayList<Unit> getActualSalvage() {
		return actualSalvage;
	}
	
	public void salvageUnit(int i) {
		actualSalvage.add(new Unit(potentialSalvage.get(i), campaign));	
	}

	public void dontSalvageUnit(int i) {
		leftoverSalvage.add(new Unit(potentialSalvage.get(i), campaign));	
	}
	
	public Campaign getCampaign() {
		return campaign;
	}
	
	public Mission getMission() {
		return campaign.getMission(scenario.getMissionId());
	}
	
	public Hashtable<String, String> getKillCredits() {
		return killCredits;
	}
	
	public ArrayList<Unit> getUnits() {
		return units;
	}
	
	public void resolveScenario(int resolution, String report) {

		//lets start by generating a stub file for our records
		scenario.generateStub(campaign);

		//ok lets do the whole enchilada and go ahead and update campaign
		
		//first figure out if we need any battle loss comp
		double blc = 0;
		Mission m = campaign.getMission(scenario.getMissionId());
		if(m instanceof Contract) {
			blc = ((Contract)m).getBattleLossComp()/100.0;
		}
			
		//now lets update personnel
		for(UUID pid : peopleStatus.keySet()) {
			Person person = campaign.getPerson(pid);
			PersonStatus status = peopleStatus.get(pid);
			if(null == person || null == status) {
				continue;
			}
			person.setXp(person.getXp() + status.xp);
			person.setHits(status.getHits());
			person.addLogEntry(campaign.getDate(), "Participated in " + scenario.getName() + " during mission " + m.getName());
			for(Kill k : status.getKills()) {
				campaign.addKill(k);
			}
			if(status.isMissing()) {
				campaign.changeStatus(person, Person.S_MIA);
			}
			if(status.isDead()) {
				campaign.changeStatus(person, Person.S_KIA);
			}
		}
		
		//now lets update all units
		for(Unit unit : units) {
			Entity en = entities.get(unit.getId());
			if(null == en) {
				//missing unit
				if(blc > 0) {
					long value = (long)(blc * unit.getSellValue());
					campaign.getFinances().credit(value, Transaction.C_BLC, "Battle loss compensation for " + unit.getName(), campaign.getCalendar().getTime());
					DecimalFormat formatter = new DecimalFormat();
					campaign.addReport(formatter.format(value) + " in battle loss compensation for " + unit.getName() + " has been credited to your account.");
				}
				campaign.removeUnit(unit.getId());
			} else {
				long currentValue = unit.getValueOfAllMissingParts();
				campaign.clearGameData(en);
				unit.setEntity(en);
				unit.runDiagnostic();
				unit.resetPilotAndEntity();
				if(!unit.isRepairable()) {
					unit.setSalvage(true);
				}
				//check for BLC
				long newValue = unit.getValueOfAllMissingParts();
				campaign.addReport(unit.getName() + " has been recovered.");
				if(blc > 0 && newValue > currentValue) {
					long finalValue = (long)(blc * (newValue - currentValue));
					campaign.getFinances().credit(finalValue, Transaction.C_BLC, "Battle loss compensation (parts) for " + unit.getName(), campaign.getCalendar().getTime());
					DecimalFormat formatter = new DecimalFormat();
					campaign.addReport(formatter.format(finalValue) + " in battle loss compensation for parts for " + unit.getName() + " has been credited to your account.");
				}
			}
		}
		
		//now lets take care of salvage
		for(Unit salvageUnit : actualSalvage) {
			campaign.clearGameData(salvageUnit.getEntity());
			campaign.addUnit(salvageUnit.getEntity(), false);
			//if this is a contract, add to th salvaged value
			if(getMission() instanceof Contract) {
				salvageUnit.initializeParts(false);
				salvageUnit.runDiagnostic();
				((Contract)getMission()).addSalvageByUnit(salvageUnit.getSellValue());
			}
		}
		if(getMission() instanceof Contract) {
			if(((Contract)getMission()).isSalvageExchange()) {
				//add exchange value of bank account
				long value = 0;
				for(Entity en : potentialSalvage) {
					Unit salvageUnit= new Unit(en, campaign);
					salvageUnit.initializeParts(false);
					salvageUnit.runDiagnostic();
					value += salvageUnit.getSellValue();
				}
				value = (long)(((double)value) * (((Contract)getMission()).getSalvagePct()/100.0));
				campaign.getFinances().credit(value, Transaction.C_SALVAGE, "salvage exchange for " + scenario.getName(),  campaign.getCalendar().getTime());
				DecimalFormat formatter = new DecimalFormat();
				campaign.addReport(formatter.format(value) + " C-Bills have been credited to your account for salvage exchange.");
			} else {
				for(Unit salvageUnit : leftoverSalvage) {
					salvageUnit.initializeParts(false);
					salvageUnit.runDiagnostic();
					((Contract)getMission()).addSalvageByEmployer(salvageUnit.getSellValue());
				}
			}
		}
		scenario.setStatus(resolution);
		scenario.setReport(report);
		scenario.clearAllForcesAndPersonnel(campaign);
		scenario.setDate(campaign.getCalendar().getTime());
		client = null;
	}
	
	public ArrayList<Person> getMissingPersonnel() {
		ArrayList<Person> mia = new ArrayList<Person>();
		for(UUID pid : peopleStatus.keySet()) {
			PersonStatus status = peopleStatus.get(pid);
			if(status.isMissing()) {
				Person p = campaign.getPerson(pid);
				if(null != p) {
					mia.add(p);
				}
			}
		}
		return mia;
	}
	
	public ArrayList<Person> getDeadPersonnel() {
		ArrayList<Person> kia = new ArrayList<Person>();
		for(UUID pid : peopleStatus.keySet()) {
			PersonStatus status = peopleStatus.get(pid);
			if(status.isDead()) {
				Person p = campaign.getPerson(pid);
				if(null != p) {
					kia.add(p);
				}
			}
		}
		return kia;
	}
	
	public ArrayList<Person> getRecoveredPersonnel() {
		ArrayList<Person> recovered = new ArrayList<Person>();
		for(UUID pid : peopleStatus.keySet()) {
			PersonStatus status = peopleStatus.get(pid);
			if(!status.isDead() && !status.isMissing()) {
				Person p = campaign.getPerson(pid);
				if(null != p) {
					recovered.add(p);
				}
			}
		}
		return recovered;
	}
	
	public Hashtable<UUID, PersonStatus> getPeopleStatus() {
		return peopleStatus;
	}
	
	public ArrayList<Unit> getMissingUnits() {
		ArrayList<Unit> missing = new ArrayList<Unit>();
		for(Unit u : units) {
			if(null == entities.get(u.getId())) {
				missing.add(u);
			}
		}
		return missing;
	}
	
	public ArrayList<Unit> getRecoveredUnits() {
		ArrayList<Unit> recovered = new ArrayList<Unit>();
		for(Unit u : units) {
			if(null != entities.get(u.getId())) {
				recovered.add(u);
			}
		}
		return recovered;
	}
	
	public void recoverUnit(Unit u) {
		entities.put(u.getId(), u.getEntity());
	}
	

	/**
	 * This object is used to track the status of a particular personnel. At the present, 
	 * we track the person's missing status, hits, and XP
	 * @author Jay Lawson
	 *
	 */
	public class PersonStatus {
		
		private String name;
		private String unitName;
		private int hits;
		private boolean missing;
		private int xp;
		private ArrayList<Kill> kills;
		
		public PersonStatus(String n, String u, int h) {
			name = n;
			unitName = u;
			hits = h;
			missing = false;
			xp = 0;
			kills = new ArrayList<Kill>();
		}
		
		public String getName() {
			return name;
		}
		
		public String getUnitName() {
			return unitName;
		}
		
		public int getHits() {
			return hits;
		}
		
		public void setHits(int h) {
			hits = h;
		}
		
		public boolean isDead() {
			return hits >= 6;
		}
		
		public boolean isMissing() {
			return missing && !isDead();
		}
		
		public void setMissing(boolean b) {
			missing = b;
		}
		
		public int getXP() {
			if(isDead()) {
				return 0;
			}
			return xp;
		}
		
		public void setXP(int x) {
			xp = x;
		}
		
		public void addKill(Kill k) {
			kills.add(k);
		}
		
		public ArrayList<Kill> getKills() {
			return kills;
		}
	}
}