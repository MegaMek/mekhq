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

package mekhq;

import gd.xml.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import megamek.common.Entity;
import megamek.common.Pilot;
import megamek.common.XMLStreamParser;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;

/**
 * This object will be the main workforce for the scenario
 * resolution wizard. It will keep track of information and be 
 * fed back and forth between the various wizards
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ResolveScenarioTracker {
	
	ArrayList<Entity> entities;
	ArrayList<Entity> salvage;
	ArrayList<Pilot> pilots;
	ArrayList<Unit> units;
	ArrayList<PilotPerson> people;
	ArrayList<Unit> missingUnits;
	ArrayList<PilotPerson> missingPilots;
	ArrayList<Pilot> deadPilots;
	Campaign campaign;
	Scenario scenario;
	JFileChooser unitList;
	JFileChooser salvageList;
	
	public ResolveScenarioTracker(Scenario s, Campaign c) {
		this.scenario = s;
		this.campaign = c;
		entities = new ArrayList<Entity>();
		salvage = new ArrayList<Entity>();
		pilots = new ArrayList<Pilot>();
		units = new ArrayList<Unit>();
		people = new ArrayList<PilotPerson>();
		missingUnits = new ArrayList<Unit>();
		missingPilots = new ArrayList<PilotPerson>();
		for(int pid : scenario.getForces(campaign).getAllPersonnel()) {
			Person p = campaign.getPerson(pid);
			if(p instanceof PilotPerson) {
				PilotPerson pp = (PilotPerson)p;
				people.add(pp);
				if(null != pp.getAssignedUnit()) {
					units.add(pp.getAssignedUnit());
				}
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
	
	public void processMulFiles() {
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
		if(null != salvageFile) {
			try {
				loadSalvage(salvageFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		identifyMissingUnits();
		identifyMissingPilots();
		checkForCasualties();
	}
	
	
	public void identifyMissingUnits() {
		missingUnits = new ArrayList<Unit>();
		for(Unit u : units) {
			if(!foundMatch(u.getEntity(), entities) && !foundMatch(u.getEntity(), salvage)) {
				missingUnits.add(u);
			}
		}
	}
	
	public void identifyMissingPilots() {
		missingPilots = new ArrayList<PilotPerson>();
		for(PilotPerson person : people) {
			if(!foundMatch(person.getPilot(), pilots)) {
				missingPilots.add(person);
			}
		}
	}
	
	public void checkForCasualties() {
		deadPilots = new ArrayList<Pilot>();
		for(Pilot p : pilots) {
			if(p.isDead()) {
				deadPilots.add(p);
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
				MekHQApp.logMessage(parser.getWarningMessage());
			}

			// Add the units from the file.
			for (Entity entity : parser.getEntities()) {
				entities.add(entity);
			}
			
			// add any ejected pilots
			for (Pilot pilot : parser.getPilots()) {
				pilots.add(pilot);
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
				MekHQApp.logMessage(parser.getWarningMessage());
			}

			// Add the units from the file.
			//there may be duplicates of the saved unit mul in salvage, so check
			//this
			for (Entity entity : parser.getEntities()) {
				if(!foundMatch(entity, entities)) {
					salvage.add(entity);
				}
			}
		}
	}
	
	private boolean foundMatch(Entity en, ArrayList<Entity> ents) {
		for(Entity otherEntity : ents) {
			if(otherEntity.getExternalId() == en.getExternalId()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean foundMatch(Pilot p, ArrayList<Pilot> pils) {
		for(Pilot otherPilots : pils) {
			if(otherPilots.getExternalId() == p.getExternalId()) {
				return true;
			}
		}
		return false;
	}
	
	
	public ArrayList<Unit> getMissingUnits() {
		return missingUnits;
	}
	
	public void recoverMissingEntity(int i) {
		if(i < 0 || i > missingUnits.size()) {
			return;
		}
		Unit u = missingUnits.get(i);
		entities.add(u.getEntity());
	}
	
	public ArrayList<PilotPerson> getMissingPilots() {
		return missingPilots;
	}
	
	public void recoverMissingPilot(int i) {
		if(i < 0 || i > missingPilots.size()) {
			return;
		}
		PilotPerson pp = missingPilots.get(i);
		pilots.add(pp.getPilot());
	}
	
	public ArrayList<Pilot> getDeadPilots() {
		return deadPilots;
	}
	
	public void removeCasaulty(int i) {
		if(i < 0 || i > deadPilots.size()) {
			return;
		}
		Pilot casualty = deadPilots.get(i);
		casualty.setHits(5);
		casualty.setDead(false);
	}
	
	public ArrayList<Entity> getSalvage() {
		return salvage;
	}
	
	public void setSalvage(ArrayList<Entity> s) {
		this.salvage = s;
	}
	
	public Entity getSalvagedUnit(int i) {
		return salvage.get(i);
	}
	
	public ArrayList<Entity> getRecoveredUnits() {
		return entities;
	}
	
	public ArrayList<Pilot> getRecoveredPilots() {
		return pilots;
	}
	
	
	public void resolveScenario(int resolution, String report) {

		//ok lets do the whole enchilada and go ahead and update campaign
		
		//first lets update the entities on all units
		for(Entity en : entities) {
			updateUnitWith(en);
		}
		//now lets update pilots
		for(Pilot p : pilots) {
			updatePilotWith(p);
		}
		//now lets take care of dead pilots
		for(Pilot dead : deadPilots) {
			killPilot(dead);
		}
		//now lets take take care of missing pilots
		for(PilotPerson miss : missingPilots) {
			setMIA(miss);
		}
		//now lets take care of missing units
		for(Unit missUnit : missingUnits) {
			campaign.removeUnit(missUnit.getId());
		}
		//now lets take care of salvage
		for(Entity salvageEn : salvage) {
			campaign.addUnit(salvageEn, false);
		}
		scenario.setStatus(resolution);
		scenario.setReport(report);
		scenario.clearAllForcesAndPersonnel(campaign);
	}
	
	private void updateUnitWith(Entity en) {
		for(Unit u : campaign.getUnits()) {
			if(u.getEntity().getExternalId() == en.getExternalId()) {
				u.setEntity(en);
				u.runDiagnostic();
				campaign.addReport(u.getEntity().getDisplayName() + " has been recovered.");
				return;
			}
		}
	}
	
	private void updatePilotWith(Pilot pilot) {
		for(Person person : campaign.getPersonnel()) {
			if(!(person instanceof PilotPerson)) {
				continue;
			}
			PilotPerson pp = (PilotPerson)person;
			if(pp.getPilot().getExternalId() == pilot.getExternalId()) {
				pp.setPilot(pilot);
				pp.undeploy(campaign);
				if(null != pp.getAssignedUnit()) {
					//TODO: this is such a roundabout way of doing this - lets
					//make it personnel centric
					pp.getAssignedUnit().setPilot(pp);
					campaign.addReport(pp.getFullTitle() + " has been recovered.");
				}
				return;
			}
		}
	}
	
	private void killPilot(Pilot pilot) {
		for(Person person : campaign.getPersonnel()) {
			if(!(person instanceof PilotPerson)) {
				continue;
			}
			PilotPerson pp = (PilotPerson)person;
			if(pp.getPilot().getExternalId() == pilot.getExternalId()) {
				pp.setStatus(Person.S_KIA);
				campaign.removePersonFromForce(pp);
				campaign.addReport(pp.getFullTitle() + " has been killed in action.");
				return;
			}
		}
	}
	
	private void setMIA(PilotPerson pilot) {
		Person person = campaign.getPerson(pilot.getId());
		if(null != person) {
			person.setStatus(Person.S_MIA);
			person.undeploy(campaign);
			campaign.removePersonFromForce(person);
			campaign.addReport(person.getFullTitle() + " is missing in action.");
		}
	}
}