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

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import megamek.common.Entity;
import megamek.common.Pilot;
import megamek.common.XMLStreamParser;
import mekhq.MekHQApp;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
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
	ArrayList<Entity> potentialSalvage;
	ArrayList<Unit> actualSalvage;
	ArrayList<Unit> leftoverSalvage;
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
		potentialSalvage = new ArrayList<Entity>();
		actualSalvage = new ArrayList<Unit>();
		leftoverSalvage = new ArrayList<Unit>();
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
		checkSalvageForPilotsAndUnits() ;
		checkForCasualties();
		identifyMissingUnits();
		identifyMissingPilots();
	}
	
	/**
	 * Sometimes pilots and units from the players own forces will end up in salvage so lets check for
	 * them and remove them from salvage if we find them there
	 */
	public void checkSalvageForPilotsAndUnits() {
		for(Unit u : units) {
			Entity match = getMatch(u.getEntity(), potentialSalvage);
			if(null != match) {
				entities.add(match);
				if(null != match.getCrew()) {
					pilots.add(match.getCrew());
				}
				potentialSalvage.remove(match);
			}
		}
	}
	
	
	public void identifyMissingUnits() {
		missingUnits = new ArrayList<Unit>();
		for(Unit u : units) {
			if(!foundMatch(u.getEntity(), entities) && !foundMatch(u.getEntity(), potentialSalvage)) {
				missingUnits.add(u);
			}
		}
	}
	
	public void identifyMissingPilots() {
		missingPilots = new ArrayList<PilotPerson>();
		for(PilotPerson person : people) {
			if(!foundMatch(person.getPilot(), pilots) && !foundMatch(person.getPilot(), deadPilots)) {
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
					potentialSalvage.add(entity);
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
	
	private Entity getMatch(Entity en, ArrayList<Entity> ents) {
		for(Entity otherEntity : ents) {
			if(otherEntity.getExternalId() == en.getExternalId()) {
				return otherEntity;
			}
		}
		return null;
	}
	
	public boolean foundMatch(Pilot p, ArrayList<Pilot> pils) {
		for(Pilot otherPilots : pils) {
			if(otherPilots.getExternalId() == p.getExternalId()) {
				return true;
			}
		}
		return false;
	}
	
	private Pilot getMatch(Pilot p, ArrayList<Pilot> pils) {
		for(Pilot otherPilot : pils) {
			if(otherPilot.getExternalId() == p.getExternalId()) {
				return otherPilot;
			}
		}
		return null;
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
	
	public void makeActive(Pilot pilot) {
		if(!foundMatch(pilot, pilots)) {
			pilots.add(pilot);
		}	
		int idx = -1;
		for(int i = 0; i < deadPilots.size(); i++) {
			if(deadPilots.get(i).getExternalId() == pilot.getExternalId()) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			deadPilots.remove(idx);
		}
	}
	
	public void makeCasualty(Pilot pilot) {
		if(!foundMatch(pilot, deadPilots)) {
			deadPilots.add(pilot);
		}	
	}
	
	public void makeMissing(Pilot pilot) {
		int idx = -1;
		for(int i = 0; i < pilots.size(); i++) {
			if(pilots.get(i).getExternalId() == pilot.getExternalId()) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			pilots.remove(idx);
		}
		idx = -1;
		for(int i = 0; i < deadPilots.size(); i++) {
			if(deadPilots.get(i).getExternalId() == pilot.getExternalId()) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			deadPilots.remove(idx);
		}	
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
	
	public ArrayList<PilotPerson> getPeople() {
		return people;
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
	
	public ArrayList<Entity> getRecoveredUnits() {
		return entities;
	}
	
	public ArrayList<Pilot> getRecoveredPilots() {
		return pilots;
	}
	
	public Campaign getCampaign() {
		return campaign;
	}
	
	public Mission getMission() {
		return campaign.getMission(scenario.getMissionId());
	}
	
	
	public void resolveScenario(int resolution, String report) {

		//ok lets do the whole enchilada and go ahead and update campaign
		
		//first figure out if we need any battle loss comp
		double blc = 0;
		Mission m = campaign.getMission(scenario.getMissionId());
		if(m instanceof Contract) {
			blc = ((Contract)m).getBattleLossComp()/100.0;
		}
		
		//first lets update the entities on all units
		for(Entity en : entities) {
			updateUnitWith(en, blc);
		}
		//now lets update pilots
		for(Pilot p : pilots) {
			updatePilotWith(p);
		}
		//now lets take take care of missing pilots
		for(PilotPerson miss : missingPilots) {
			setMIA(miss);
		}
		//now lets take care of dead pilots
		for(Pilot dead : deadPilots) {
			killPilot(dead);
		}
		//now lets take care of missing units
		for(Unit missUnit : missingUnits) {
			if(blc > 0) {
				long value = (long)(blc * missUnit.getSellValue());
				campaign.getFinances().credit(value, Transaction.C_BLC, "Battle loss compensation for " + missUnit.getEntity().getDisplayName(), campaign.getCalendar().getTime());
				DecimalFormat formatter = new DecimalFormat();
				campaign.addReport(formatter.format(value) + " in battle loss compensation for " + missUnit.getEntity().getDisplayName() + " has been credited to your account.");
			}
			campaign.removeUnit(missUnit.getId());
		}
		//now lets take care of salvage
		for(Unit salvageUnit : actualSalvage) {
			campaign.addUnit(salvageUnit.getEntity(), false);
			//if this is a contract, add to th salvaged value
			if(getMission() instanceof Contract) {
				((Contract)getMission()).addSalvageByUnit(salvageUnit.getSellValue());
			}
		}
		if(getMission() instanceof Contract) {
			if(((Contract)getMission()).isSalvageExchange()) {
				//add exchange value of bank account
				long value = 0;
				for(Entity en : potentialSalvage) {
					Unit salvageUnit= new Unit(en, campaign);
					salvageUnit.runDiagnostic();
					value += salvageUnit.getSellValue();
				}
				value = (long)(((double)value) * (((Contract)getMission()).getSalvagePct()/100.0));
				campaign.getFinances().credit(value, Transaction.C_SALVAGE, "salvage exchange for " + scenario.getName(),  campaign.getCalendar().getTime());
				DecimalFormat formatter = new DecimalFormat();
				campaign.addReport(formatter.format(value) + " C-Bills have been credited to your account for salvage exchange.");
			} else {
				for(Unit salvageUnit : leftoverSalvage) {
					((Contract)getMission()).addSalvageByEmployer(salvageUnit.getSellValue());
				}
			}
		}
		scenario.setStatus(resolution);
		scenario.setReport(report);
		scenario.clearAllForcesAndPersonnel(campaign);
	}
	
	private void updateUnitWith(Entity en, double blc) {
		for(Unit u : campaign.getUnits()) {
			if(u.getEntity().getExternalId() == en.getExternalId()) {
				//check the current value of missing parts before updating
				long currentValue = u.getValueOfAllMissingParts();
				u.setEntity(en);
				u.runDiagnostic();
				//check for BLC
				long newValue = u.getValueOfAllMissingParts();
				campaign.addReport(u.getEntity().getDisplayName() + " has been recovered.");
				if(blc > 0 && newValue > currentValue) {
					long finalValue = (long)(blc * (newValue - currentValue));
					campaign.getFinances().credit(finalValue, Transaction.C_BLC, "Battle loss compensation (parts) for " + en.getDisplayName(), campaign.getCalendar().getTime());
					DecimalFormat formatter = new DecimalFormat();
					campaign.addReport(formatter.format(finalValue) + " in battle loss compensation for parts for " + en.getDisplayName() + " has been credited to your account.");
				}
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
				if(pilot.isDead()) {
					pilot.setDead(false);
					pilot.setHits(0);
				}
				pp.setPilot(pilot);
				pp.undeploy(campaign);
				//assign XP
				pp.setXp(pp.getXp() + campaign.getSkillCosts().getScenarioXP());
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
				pp.getPilot().setDead(true);
				pp.getPilot().setHits(6);
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