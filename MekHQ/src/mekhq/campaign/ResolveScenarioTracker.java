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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import megamek.client.Client;
import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.EjectedCrew;
import megamek.common.Entity;
import megamek.common.IArmorState;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechWarrior;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.event.GameVictoryEvent;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * This object will be the main workhorse for the scenario
 * resolution wizard. It will keep track of information and be
 * fed back and forth between the various wizards
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ResolveScenarioTracker {

	//Hashtable<UUID, Entity> entities;
    Hashtable<UUID, UnitStatus> unitsStatus;
    Hashtable<UUID, UnitStatus> salvageStatus;
    Hashtable<UUID, Crew> pilots;
	Hashtable<UUID, Crew> mia;
	ArrayList<Person> newPilots;
	ArrayList<TestUnit> potentialSalvage;
	ArrayList<TestUnit> alliedUnits;
	ArrayList<TestUnit> actualSalvage;
	ArrayList<TestUnit> leftoverSalvage;
	ArrayList<Unit> units;
	ArrayList<Loot> potentialLoot;
	ArrayList<Loot> actualLoot;
    Hashtable<UUID, PersonStatus> peopleStatus;
    Hashtable<UUID, PersonStatus> prisonerStatus;
	Hashtable<String, String> killCredits;
	Hashtable<UUID, EjectedCrew> ejections;

	/* AtB */
	int contractBreaches = 0;
	int bonusRolls = 0;

	Campaign campaign;
	Scenario scenario;
	JFileChooser unitList;
	Client client;
	Boolean control;
    private GameVictoryEvent victoryEvent;

	public ResolveScenarioTracker(Scenario s, Campaign c, boolean ctrl) {
		this.scenario = s;
		this.campaign = c;
		this.control = ctrl;
		unitsStatus = new Hashtable<UUID, UnitStatus>();
		salvageStatus = new Hashtable<UUID, UnitStatus>();
		potentialSalvage = new ArrayList<TestUnit>();
		alliedUnits = new ArrayList<TestUnit>(); // TODO: Make some use of this?
		actualSalvage = new ArrayList<TestUnit>();
		leftoverSalvage = new ArrayList<TestUnit>();
		pilots = new Hashtable<UUID, Crew>();
		mia = new Hashtable<UUID, Crew>();
		newPilots = new ArrayList<Person>();
		units = new ArrayList<Unit>();
		potentialLoot = scenario.getLoot();
		actualLoot = new ArrayList<Loot>();
		peopleStatus = new Hashtable<UUID, PersonStatus>();
		prisonerStatus = new Hashtable<UUID, PersonStatus>();
		killCredits = new Hashtable<String, String>();
		ejections = new Hashtable<UUID, EjectedCrew>();
		for(UUID uid : scenario.getForces(campaign).getAllUnits()) {
			Unit u = campaign.getUnit(uid);
			if(null != u && null == u.checkDeployment()) {
				units.add(u);
				//assume its missing until we can confirm otherwise
				unitsStatus.put(uid, new UnitStatus(u));
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

	public void setClient(Client c) {
		client = c;
	}

	public void processMulFiles() {
		File unitFile = unitList.getSelectedFile();
		//File salvageFile = salvageList.getSelectedFile();
		if(null != unitFile) {
			try {
				loadUnitsAndPilots(unitFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*if(null != salvageFile) {
			try {
				loadSalvage(salvageFile, controlsField);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		checkStatusOfPersonnel();
	}

	private TestUnit generateNewTestUnit(Entity e) {
		// Do some hoops here so that the new mech gets it's old individual paint job!
        String cat = e.getCamoCategory();
        String fn = e.getCamoFileName();
        TestUnit nu = new TestUnit(e, campaign, true);
        nu.getEntity().setCamoCategory(cat);
        nu.getEntity().setCamoFileName(fn);
        UUID id = UUID.randomUUID();
        nu.getEntity().setExternalIdAsString(id.toString());
        nu.setId(id);
        return nu;
	}

	public void processGame() {

		int pid = client.getLocalPlayer().getId();
		int team = client.getLocalPlayer().getTeam();

		for (Enumeration<Entity> iter = victoryEvent.getEntities(); iter.hasMoreElements();) {
			Entity e = iter.nextElement();
			checkForLostLimbs(e, control);
			if(e.getOwnerId() == pid || e.getOwner().getTeam() == team) {
				if(!e.getExternalIdAsString().equals("-1")) {
					UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
					if(null != status) {
						boolean lost = (!e.canEscape() && !control) || e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED;
						status.assignFoundEntity(e, lost);						
					} else {
						TestUnit nu = generateNewTestUnit(e);
						UnitStatus us = new UnitStatus(nu);
						unitsStatus.put(nu.getId(), us);
						alliedUnits.add(nu);
					}
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
						if(e instanceof EjectedCrew) {
							ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew)e);
						}
					}
				}
			} else if(e.getOwner().isEnemyOf(client.getLocalPlayer())) {
				if(control) {
					// Kill credit automatically assigned only if they can't escape
					if (!e.canEscape()) {
					    Entity killer = victoryEvent.getEntity(e.getKillerId());
		                if(null != killer && killer.getOwnerId() == pid) {
		                    //the killer is one of your units, congrats!
		                    killCredits.put(e.getDisplayName(), killer.getExternalIdAsString());
		                } else {
		                    killCredits.put(e.getDisplayName(), "None");
		                }
					}
					if(e instanceof EjectedCrew) {
						continue;
					}
					TestUnit nu = generateNewTestUnit(e);
                    UnitStatus us = new UnitStatus(nu);
                    salvageStatus.put(nu.getId(), us);
                    potentialSalvage.add(nu);
                    ArrayList<Person> crewMembers = Utilities.generateRandomCrewWithCombinedSkill(nu, campaign, false, true);
                    if (null != crewMembers) {
                        newPilots.addAll(crewMembers);
                    }
				}
			}
		}
		// Utterly destroyed entities
		for (Enumeration<Entity> iter = victoryEvent.getDevastatedEntities(); iter.hasMoreElements();) {
		    Entity e = iter.nextElement();
		    if(e.getOwnerId() == pid || e.getOwner().getTeam() == team) {
		        if(!e.getExternalIdAsString().equals("-1")) {
		            UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
		            if(null != status) {
		                status.assignFoundEntity(e, true);
		            } else {
		                TestUnit nu = generateNewTestUnit(e);
		                UnitStatus us = new UnitStatus(nu);
		                unitsStatus.put(nu.getId(), us);
		                alliedUnits.add(nu);
		            }
		        }
		    } else {
                Entity killer = victoryEvent.getEntity(e.getKillerId());
                if(null != killer && killer.getOwnerId() == pid) {
                    //the killer is one of your units, congrats!
                    killCredits.put(e.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    killCredits.put(e.getDisplayName(), "None");
                }
                //why are we doing this, aren't they utterly destroyed?
                //Taharqa: I am commenting this out
                /*TestUnit nu = generateNewTestUnit(e);
                UnitStatus us = new UnitStatus(nu);
                salvageStatus.put(nu.getId(), us);
                potentialSalvage.add(nu);*/
		    }
		}
		//add retreated units
		for (Enumeration<Entity> iter = victoryEvent.getRetreatedEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
			checkForLostLimbs(e, control);
            if(e.getOwnerId() == pid || e.getOwner().getTeam() == team) {
            	if(!e.getExternalIdAsString().equals("-1")) {
            	    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                    if(null != status) {
                        status.assignFoundEntity(e, false);
					} else {
					    TestUnit nu = generateNewTestUnit(e);
	                    UnitStatus us = new UnitStatus(nu);
	                    unitsStatus.put(nu.getId(), us);
	                    alliedUnits.add(nu);
					}
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
						if(e instanceof EjectedCrew) {
							ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew)e);
						}
					}
				}
            }
        }


        Enumeration<Entity> wrecks = victoryEvent.getGraveyardEntities();
        while (wrecks.hasMoreElements()) {
        	Entity e = wrecks.nextElement();
			checkForLostLimbs(e, control);
        	if(e.getOwnerId() == pid || e.getOwner().getTeam() == team) {
        		if(!e.getExternalIdAsString().equals("-1")) {
        		    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                    if(null != status) {
                        status.assignFoundEntity(e, !control);
                        if(e instanceof EjectedCrew) {
							ejections.put(UUID.fromString(e.getExternalIdAsString()), (EjectedCrew)e);
						}
					} else {
					    TestUnit nu = generateNewTestUnit(e);
	                    UnitStatus us = new UnitStatus(nu);
	                    unitsStatus.put(nu.getId(), us);
	                    alliedUnits.add(nu);
					}
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						if(e instanceof EjectedCrew) {
							ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew)e);
						}
						if(control) {
							pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
						} else {
							mia.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
						}
					}
				}
        	} else if(e.getOwner().isEnemyOf(client.getLocalPlayer())) {
        		Entity killer = victoryEvent.getEntity(e.getKillerId());
        		if(null != killer && killer.getOwnerId() == pid) {
        			//the killer is one of your units, congrats!
        			killCredits.put(e.getDisplayName(), killer.getExternalIdAsString());
        		} else {
        			killCredits.put(e.getDisplayName(), "None");
        		}
        		if(control && !(e instanceof EjectedCrew)) {
        			TestUnit nu = generateNewTestUnit(e);
                    UnitStatus us = new UnitStatus(nu);
                    salvageStatus.put(nu.getId(), us);
                    potentialSalvage.add(nu);
                    ArrayList<Person> crewMembers = Utilities.generateRandomCrewWithCombinedSkill(nu, campaign, false, true);
                    if (null != crewMembers) {
                        newPilots.addAll(crewMembers);
                    }
        		}
        	}
        }
        checkStatusOfPersonnel();
	}

	/**
	 * This checks whether an entity has any blown off limbs. If the battlefield
	 * was not controlled it marks the limb as destroyed. if the battlefield was
	 * controlled it clears the missing status from any equipment.
	 *
	 * This method should be run the first time an entity is loaded into the tracker,
	 * either from the game or from a MUL file.
	 * @param en
	 * @param controlsField
	 */
	private void checkForLostLimbs(Entity en, boolean controlsField) {
		for(int loc = 0; loc < en.locations(); loc++) {
			if(en.isLocationBlownOff(loc) && !controlsField) {
				//sorry dude, we cant find your arm
				en.setLocationBlownOff(loc, false);
				en.setArmor(IArmorState.ARMOR_DESTROYED, loc);
				en.setInternal(IArmorState.ARMOR_DESTROYED, loc);
			}
			//check for mounted and critical slot missingness as well
			for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
				final CriticalSlot cs = en.getCritical(loc, i);
				if(null == cs || !cs.isEverHittable()) {
					continue;
				}
				Mounted m = cs.getMount();
				if(cs.isMissing()) {
					if(controlsField) {
						cs.setMissing(false);
						if(null != m) {
			            	m.setMissing(false);
						}
					} else {
						if(null != m) {
							m.setMissing(true);
						}
					}
				}
			}
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
				if(killCredits.get(killed).equalsIgnoreCase("None")) {
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
		PersonStatus status;
		java.util.HashSet<Integer> pickedUpPilots = new java.util.HashSet<Integer>();

		for(Unit u : units) {
			for (int mwid : u.getEntity().getPickedUpMechWarriors()) {
				MechWarrior mw = (MechWarrior)victoryEvent.getEntity(mwid);
				pickedUpPilots.add(mw.getOriginalRideId());
			}
		}

		//lets cycle through units and get their crew
        for(Unit u : units) {
            //shuffling the crew ensures that casualties are randomly assigned in multi-crew units
            ArrayList<Person> crew = shuffleCrew(u.getActiveCrew());
            Entity en = null;
            UnitStatus ustatus = unitsStatus.get(u.getId());
            if(null != ustatus) {
                en = ustatus.getEntity();
            }
            //check for an ejected entity and if we find one then assign it instead to switch vees
            //over to infantry checks for casualties
            Entity ejected = ejections.get(u.getCommander().getId());
            if(null != ejected) {
            	en = ejected;
            }
            //determine total casualties for infantry and large craft
            int casualties = 0;
            int casualtiesAssigned = 0;
            if(null != en && en instanceof Infantry) {
                en.applyDamage();
                int strength = ((Infantry)en).getShootingStrength();
                casualties = crew.size() - strength;
                if (ustatus.isTotalLoss()) {
                    casualties = crew.size();
                }
            }
            if(null != en && (en instanceof SmallCraft || en instanceof Jumpship)) {
            	//need to check for existing hits because you can fly aeros with less than full
            	//crew
            	int existingHits = 0;
            	int currentHits = 0;
            	if(null != u.getEntity().getCrew()) {
            		existingHits = u.getEntity().getCrew().getHits();
            	}
            	if(null != en && null != en.getCrew()) {
            		currentHits = en.getCrew().getHits();
            	}
            	int newHits = Math.max(0,currentHits - existingHits);
            	casualties = (int)Math.ceil(Compute.getFullCrewSize(en) * (newHits/6.0));
            }
            //try to find the crew in our pilot and mia vectors
            Crew pilot = pilots.get(u.getCommander().getId());
            boolean missingCrew = false;
            if(null == pilot) {
            	pilot = mia.get(u.getCommander().getId());
            	missingCrew = true;
            }
            for(Person p : crew) {
                status = new PersonStatus(p.getFullName(), u.getEntity().getDisplayName(), p.getHits(), p.getId());
                status.setMissing(missingCrew);
                //if the pilot was not found in either the pilot or mia vector
                //then the unit was devastated and no one ejected, so they should be dead, really dead
                if(null == pilot) {                    
                	status.setHits(6);
                	continue;
                }
                //cant do the following by u.usesSoloPilot because entity may be different if ejected
                if(en instanceof Mech 
                		|| en instanceof Protomech 
                		|| (en instanceof Aero && !(en instanceof SmallCraft || en instanceof Jumpship))) {
                	status.setHits(pilot.getHits());
                    if (pickedUpPilots.contains(u.getEntity().getId())) {
                        status.setPickedUp(true);
                    }
                } else {
                	//we have a multi-crewed vee/Aero/Infantry
                    boolean wounded = false;
                    //tanks need to be handled specially because of the special crits and because
                    //tank destruction should "kill" the crew
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
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                        else if(((Tank)en).isDriverHit() && u.isDriver(p)) {
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                        else if(((Tank)en).isCommanderHit() && u.isCommander(p)) {
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                    }
                    else {
                        if(casualtiesAssigned < casualties) {
                            casualtiesAssigned++;
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                    }
                    if(wounded) {
                        int hits = campaign.getCampaignOptions().getMinimumHitsForVees();
                        if (campaign.getCampaignOptions().useAdvancedMedical() || campaign.getCampaignOptions().useRandomHitsForVees()) {
                            int range = 6 - hits;
                            hits = hits + Compute.randomInt(range);
                        }
                        status.setHits(hits);
                    }
                }
                /**
                 * If the entity cannot be found, or it was deployed at least once during the scenario
                 * Then the pilot gets XP
                 * FIXME: I dont think this will work for manual resolution
                 */
                if (en == null || !en.wasNeverDeployed()) {
                    status.setXP(campaign.getCampaignOptions().getScenarioXP());
                }
                peopleStatus.put(p.getId(), status);
            }
        }

        // And now we have potential prisoners that are crewing a unit...
        for(Unit u : potentialSalvage) {
            if (null == u) {
                continue; // Shouldn't happen... but well... ya know
            }
            //shuffling the crew ensures that casualties are randomly assigned in multi-crew units
            ArrayList<Person> crew = shuffleCrew(getActiveCrewFromPrisoners(u));
            Entity en = null;
            UnitStatus ustatus = salvageStatus.get(u.getId());
            if(null != ustatus) {
                en = ustatus.getEntity();
            }
            int casualties = 0;
            int casualtiesAssigned = 0;
            if(null != en && en instanceof Infantry && u.getEntity() instanceof Infantry) {
                en.applyDamage();
                int strength = ((Infantry)en).getShootingStrength();
                casualties = crew.size() - strength;
                if (ustatus.isTotalLoss()) {
                    casualties = crew.size();
                }
            }
            if(null != en && en instanceof Aero && !u.usesSoloPilot()) {
            	//need to check for existing hits because you can fly aeros with less than full
            	//crew
            	int existingHits = 0;
            	int currentHits = 0;
            	if(null != u.getEntity().getCrew()) {
            		existingHits = u.getEntity().getCrew().getHits();
            	}
            	if(null != en && null != en.getCrew()) {
            		currentHits = en.getCrew().getHits();
            	}
            	int newHits = Math.max(0,currentHits - existingHits);
            	casualties = (int)Math.ceil(Compute.getFullCrewSize(en) * (newHits/6.0));
            }
            for(Person p : crew) {
                status = new PersonStatus(p.getFullName(), u.getEntity().getDisplayName(), p.getHits(), p.getId());
                if(null != ustatus && ustatus.isTotalLoss()) {
                	status.setMissing(true);
                }
                if(u.usesSoloPilot()) {
                    Crew pilot = null;
                    if (null != u.getEntity()) {
                        pilot = u.getEntity().getCrew();
                    }
                    if(null == pilot) {
                        Crew missingPilot = mia.get(p.getId());
                        if (missingPilot != null) {
                            status.setHits(missingPilot.getHits());
                        }
                        status.setMissing(true);
                    } else {
                        status.setHits(pilot.getHits());
                    }
                    if (pickedUpPilots.contains(u.getEntity().getId())
                            || (null != pilot && pilot.isUnconscious())
                            || u.getEntity().isStalled()
                            || u.getEntity().isStuck()
                            || u.getEntity().isShutDown()) {
                        if (!status.isMissing() && !status.isDead()) {
                            status.setPickedUp(true);
                            status.setCaptured(true);
                        }
                    }
                } else {
                    //we have a multi-crewed vee
                    boolean wounded = false;
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
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                        else if(((Tank)en).isDriverHit() && u.isDriver(p)) {
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                        else if(((Tank)en).isCommanderHit() && u.isCommander(p)) {
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                    }
                    else if(en instanceof Infantry || en instanceof Aero) {
                        if(casualtiesAssigned < casualties) {
                            casualtiesAssigned++;
                            if(Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        }
                    }
                    if(wounded) {
                        int hits = campaign.getCampaignOptions().getMinimumHitsForVees();
                        if (campaign.getCampaignOptions().useAdvancedMedical() || campaign.getCampaignOptions().useRandomHitsForVees()) {
                            int range = 6 - hits;
                            hits = hits + Compute.randomInt(range);
                        }
                        status.setHits(hits);
                    }
                    if (pickedUpPilots.contains(u.getEntity().getId())
                            || (null != u.getEntity().getCrew()
                            && u.getEntity().getCrew().isUnconscious())
                            || u.getEntity().isStalled()
                            || u.getEntity().isStuck()
                            || u.getEntity().isShutDown()) {
                        if (!status.isMissing() && !status.isDead()) {
                            status.setPickedUp(true);
                            status.setCaptured(true);
                        }
                    }
                }
                /**
                 * If the entity cannot be found, or it was deployed at least once during the scenario
                 * Then the pilot gets XP
                 */
                if (en == null || !en.wasNeverDeployed()) {
                    status.setXP(campaign.getCampaignOptions().getScenarioXP());
                }

                // Fix up the UUID as needed
                UUID id = null;
                if (p.getId() != null) {
                    id = p.getId();
                }
                if (id == null) {
                    id = UUID.randomUUID();
                }
                while (campaign.getPerson(id) != null && !campaign.getPerson(id).equals(p)) {
                    id = UUID.randomUUID();
                }
                p.setId(id);

                prisonerStatus.put(id, status);
            }
        }

        // And now we have potential prisoners that didn't have a unit...
        for (Person p : newPilots) {
            // Can we have NULL pilots in this stupid list?
            if (p == null) {
                continue;
            }
            // Fix up the UUID as needed
            UUID id = null;
            if (p.getId() != null) {
                id = p.getId();
            }
            if (id == null) {
                id = UUID.randomUUID();
            }
            while (campaign.getPerson(id) != null && !campaign.getPerson(id).equals(p)) {
                id = UUID.randomUUID();
            }
            p.setId(id);

            // Create a status for them
            status = new PersonStatus(p.getFullName(), "None", p.getHits(), p.getId());
            status.setCaptured(true);
            prisonerStatus.put(id, status);
        }
	}

	private ArrayList<Person> getActiveCrewFromPrisoners(Unit u) {
	    ArrayList<Person> crew = new ArrayList<Person>();
	    for (Iterator<Person> i = newPilots.iterator(); i.hasNext(); ) {
	        Person p = i.next();
	        if (null != p && null != p.getUnitId() && p.getUnitId().equals(u.getId())) {
	            crew.add(p);
	            i.remove();
	        }
	    }
	    return crew;
	}

	private void loadUnitsAndPilots(File unitFile) throws IOException {

		if (unitFile != null) {
			// I need to get the parser myself, because I want to pull both
			// entities and pilots from it
			// Create an empty parser.
			MULParser parser = new MULParser();

			// Open up the file.
			InputStream listStream = new FileInputStream(unitFile);
			// Read a Vector from the file.
			try {
				parser.parse(listStream);
				listStream.close();
			} catch (Exception excep) {
				excep.printStackTrace(System.err);
				// throw new IOException("Unable to read from: " +
				// unitFile.getName());
			}

			// Was there any error in parsing?
			if (parser.hasWarningMessage()) {
				MekHQ.logMessage(parser.getWarningMessage());
			}
			
			killCredits = parser.getKills();
			
			for (Entity e : parser.getSurvivors()) {
				checkForLostLimbs(e, control);
				if(!e.getExternalIdAsString().equals("-1")) {
					UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
					if(null != status) {
						boolean lost = (!e.canEscape() && !control) || e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED;
						status.assignFoundEntity(e, lost);						
					}
				}
				if(null != e.getCrew()) {
					if(!e.getCrew().getExternalIdAsString().equals("-1")) {
						pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
					}
					if(e instanceof EjectedCrew) {
						ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew)e);
					}
				}
			}
			
			// Utterly destroyed entities
			for (Entity e : parser.getDevastated()) {
				if(!e.getExternalIdAsString().equals("-1")) {
					UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
					if(null != status) {
						status.assignFoundEntity(e, true);
					}
			    } else {
	                //why are we doing this, aren't they utterly destroyed?
	                //Taharqa: I am commenting this out
	                /*TestUnit nu = generateNewTestUnit(e);
	                UnitStatus us = new UnitStatus(nu);
	                salvageStatus.put(nu.getId(), us);
	                potentialSalvage.add(nu);*/
			    }
			}
			
	        for(Entity e : parser.getSalvage()) {
				checkForLostLimbs(e, control);
				if(!e.getExternalIdAsString().equals("-1") && e.isSalvage()) {
					UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
					if(null != status) {
						status.assignFoundEntity(e, !control);	
					} 
					if(null != e.getCrew()) {
						if(!e.getCrew().getExternalIdAsString().equals("-1")) {
							if(e instanceof EjectedCrew) {
								ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew)e);
							}
							if(control) {
								pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
							} else {
								mia.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
							}
						}
					}
	        	} else {
	        		if(control && !(e instanceof EjectedCrew)) {
	        			TestUnit nu = generateNewTestUnit(e);
	                    UnitStatus us = new UnitStatus(nu);
	                    salvageStatus.put(nu.getId(), us);
	                    potentialSalvage.add(nu);
	                    ArrayList<Person> crewMembers = Utilities.generateRandomCrewWithCombinedSkill(nu, campaign, false, true);
	                    if (null != crewMembers) {
	                        newPilots.addAll(crewMembers);
	                    }
	        		}
	        	}
	        }
	        checkStatusOfPersonnel();			
		}
	}

	public ArrayList<TestUnit> getAlliedUnits() {
		return alliedUnits;
	}

	public ArrayList<TestUnit> getPotentialSalvage() {
		return potentialSalvage;
	}

	public ArrayList<TestUnit> getActualSalvage() {
		return actualSalvage;
	}

	public void salvageUnit(int i) {
	    TestUnit salvageUnit = potentialSalvage.get(i);
		actualSalvage.add(salvageUnit);
	}

	public void dontSalvageUnit(int i) {
		leftoverSalvage.add(potentialSalvage.get(i));
	}

	public void setContractBreaches(int i) {
		contractBreaches = i;
	}

	public void setBonusRolls(int i) {
		bonusRolls = i;
	}

	public Campaign getCampaign() {
		return campaign;
	}

	public Scenario getScenario() {
		return scenario;
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
            if(status.getHits() > person.getHits()) {
                person.setHits(status.getHits());
            }
            person.addLogEntry(campaign.getDate(), "Participated in " + scenario.getName() + " during mission " + m.getName());
            for(Kill k : status.getKills()) {
                campaign.addKill(k);
            }
            if(status.isMissing()) {
                campaign.changeStatus(person, Person.S_MIA);
            }
            if(status.isDead()) {
                campaign.changeStatus(person, Person.S_KIA);
                if (campaign.getCampaignOptions().getUseAtB() &&
                        m instanceof AtBContract) {
                    campaign.getRetirementDefectionTracker().removeFromCampaign(person,
                            true, campaign.getCampaignOptions().getUseShareSystem()?person.getNumShares(campaign.getCampaignOptions().getSharesForAll()):0,
                                    campaign, (AtBContract)m);
                }
            }
            if (campaign.getCampaignOptions().useAdvancedMedical()) {
                person.diagnose(status.getHits());
            }
            if (status.toRemove()) {
                campaign.removePerson(pid, false);
            }
        }
        // update prisoners
        for(UUID pid : prisonerStatus.keySet()) {
            Person person = campaign.getPerson(pid);
            campaign.removePerson(pid, false);
            if (person == null) {
                for (Person p : newPilots) {
                    if (p != null && p.getId() == pid) {
                        person = p;
                        break;
                    }
                }
            }
            PersonStatus status = prisonerStatus.get(pid);
            if(null == person || null == status) {
                continue;
            }
            if (status.isPrisoner() || status.isBondsman()) {
                getCampaign().recruitPerson(person, status.isPrisoner(), status.isBondsman());
                if (getCampaign().getCampaignOptions().getUseAtB() &&
                        getCampaign().getCampaignOptions().getUseAtBCapture() &&
                        m instanceof AtBContract &&
                        status.isPrisoner()) {
                    getCampaign().getFinances().credit(50000, Transaction.C_MISC,
                            "Bonus for prisoner capture", getCampaign().getDate());
                    if (Compute.d6(2) >= 10 + ((AtBContract)m).getEnemySkill() - getCampaign().getUnitRatingMod()) {
                        getCampaign().addReport("You have convinced "
                                + person.getHyperlinkedName() + " to defect.");
                    }
                }
            } else {
                continue;
            }
            person.setXp(person.getXp() + status.xp);
            if(status.getHits() > person.getHits()) {
                person.setHits(status.getHits());
            }
            person.addLogEntry(campaign.getDate(), "Participated in " + scenario.getName() + " during mission " + m.getName());
            for(Kill k : status.getKills()) {
                campaign.addKill(k);
            }
            if(status.isMissing()) {
                campaign.changeStatus(person, Person.S_MIA);
            }
            if(status.isDead()) {
                campaign.changeStatus(person, Person.S_KIA);
                if (campaign.getCampaignOptions().getUseAtB() &&
                        m instanceof AtBContract) {
                    campaign.getRetirementDefectionTracker().removeFromCampaign(person,
                            true, campaign.getCampaignOptions().getUseShareSystem()?person.getNumShares(campaign.getCampaignOptions().getSharesForAll()):0,
                                    campaign, (AtBContract)m);
                }
            }
            if (campaign.getCampaignOptions().useAdvancedMedical()) {
                person.diagnose(status.getHits());
            }
            if (status.isBondsman()) {
                person.setBondsman();
            }
            if (status.isPrisoner()) {
                person.setPrisoner();
            }
            if (!status.isBondsman() && !status.isPrisoner() && status.isCaptured()) {
                person.setFreeMan();
            }
            if (status.toRemove()) {
                campaign.removePerson(pid, false);
            }
        }

		//now lets update all units
		for(Unit unit : units) {
		    UnitStatus ustatus = unitsStatus.get(unit.getId());
		    if(null == ustatus) {
		        //shouldnt happen
		        continue;
		    }
		    Entity en = ustatus.getEntity();
		    long unitValue = unit.getBuyCost();
		    if(campaign.getCampaignOptions().useBLCSaleValue()) {
		        unitValue = unit.getSellValue();
		    } 
			if(ustatus.isTotalLoss()) {
				//missing unit
				if(blc > 0) {
					long value = (long)(blc * unitValue);
					campaign.getFinances().credit(value, Transaction.C_BLC, "Battle loss compensation for " + unit.getName(), campaign.getCalendar().getTime());
					DecimalFormat formatter = new DecimalFormat();
					campaign.addReport(formatter.format(value) + " in battle loss compensation for " + unit.getName() + " has been credited to your account.");
				}
				campaign.removeUnit(unit.getId());
			} else {
			    en.setDeployed(false);
				long currentValue = unit.getValueOfAllMissingParts();
				campaign.clearGameData(en);
				// FIXME: Need to implement a "fuel" part just like the "armor" part
				if (en instanceof Aero) {
					((Aero)en).setFuelTonnage(((Aero)ustatus.getBaseEntity()).getFuelTonnage());
				}
				unit.setEntity(en);
				unit.runDiagnostic(true);
				unit.resetPilotAndEntity();
				if(!unit.isRepairable()) {
					unit.setSalvage(true);
				}
				campaign.addReport(unit.getHyperlinkedName() + " has been recovered.");
				//check for BLC
				long newValue = unit.getValueOfAllMissingParts();
				long blcValue = newValue - currentValue;
				String blcString = "attle loss compensation (parts) for " + unit.getName();
				if(!unit.isRepairable()) {
					//if the unit is not repairable, you should get BLC for it but we should subtract 
					//the value of salvageable parts
					blcValue = unitValue - unit.getSellValue();			
					blcString = "attle loss compensation for " + unit.getName();
				}
				if(blc > 0 && blcValue > 0) {
					long finalValue = (long)(blc * blcValue);
					campaign.getFinances().credit(finalValue, Transaction.C_BLC, "B" + blcString, campaign.getCalendar().getTime());
					DecimalFormat formatter = new DecimalFormat();
					campaign.addReport(formatter.format(finalValue) + " in b" + blcString + " has been credited to your account.");
				}
			}
		}

		//now lets take care of salvage
		for(TestUnit salvageUnit : actualSalvage) {
			UnitStatus salstatus = new UnitStatus(salvageUnit);
			// FIXME: Need to implement a "fuel" part just like the "armor" part
			if (salvageUnit.getEntity() instanceof Aero) {
				((Aero)salvageUnit.getEntity()).setFuelTonnage(((Aero)salstatus.getBaseEntity()).getFuelTonnage());
			}
			campaign.clearGameData(salvageUnit.getEntity());
			campaign.addTestUnit(salvageUnit);
			//if this is a contract, add to the salvaged value
			if(getMission() instanceof Contract) {
				((Contract)getMission()).addSalvageByUnit(salvageUnit.getSellValue());
			}
		}
		if(getMission() instanceof Contract) {
			long value = 0;
			for(Unit salvageUnit : leftoverSalvage) {
				value += salvageUnit.getSellValue();
			}
			if(((Contract)getMission()).isSalvageExchange()) {
				value = (long)(((double)value) * (((Contract)getMission()).getSalvagePct()/100.0));
				campaign.getFinances().credit(value, Transaction.C_SALVAGE, "salvage exchange for " + scenario.getName(),  campaign.getCalendar().getTime());
				DecimalFormat formatter = new DecimalFormat();
				campaign.addReport(formatter.format(value) + " C-Bills have been credited to your account for salvage exchange.");
			} else {
				((Contract)getMission()).addSalvageByEmployer(value);
			}
		}

		if (campaign.getCampaignOptions().getUseAtB() && getMission() instanceof AtBContract) {
			int unitRatingMod = campaign.getUnitRatingMod();
			for (Unit unit : units) {
				unit.setSite(((AtBContract)getMission()).getRepairLocation(unitRatingMod));
			}
			for (Unit unit : actualSalvage) {
				unit.setSite(((AtBContract)getMission()).getRepairLocation(unitRatingMod));
			}
		}

		for(Loot loot : actualLoot) {
		    loot.get(campaign, scenario);
		}

		scenario.setStatus(resolution);
		scenario.setReport(report);
		scenario.clearAllForcesAndPersonnel(campaign);
		//lets reset the network ids from the c3UUIDs
		campaign.reloadGameEntities();
		campaign.refreshNetworks();
		scenario.setDate(campaign.getCalendar().getTime());
		if (campaign.getCampaignOptions().getUseAtB() && scenario instanceof AtBScenario) {
			((AtBScenario)scenario).doPostResolution(campaign, contractBreaches, bonusRolls);
		}
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

    public Hashtable<UUID, PersonStatus> getPrisonerStatus() {
        return prisonerStatus;
    }

    public Hashtable<UUID, UnitStatus> getUnitsStatus() {
        return unitsStatus;
    }

    public Hashtable<UUID, UnitStatus> getSalvageStatus() {
        return salvageStatus;
    }

	public ArrayList<Loot> getPotentialLoot() {
	    return potentialLoot;
	}

	public void addLoot(Loot loot) {
	    actualLoot.add(loot);
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
		private boolean captured;
		private boolean prisoner;
		private boolean bondsman;
		private boolean remove;
		private boolean pickedUp;
		private UUID personId;

		public PersonStatus(String n, String u, int h, UUID id) {
			name = n;
			unitName = u;
			hits = h;
			missing = false;
			xp = 0;
			kills = new ArrayList<Kill>();
			captured = false;
			prisoner = false;
			bondsman = false;
			remove = false;
			pickedUp = false;
			personId = id;
		}

        public UUID getId() {
            return personId;
        }

        public void setRemove(UUID set) {
            personId = set;
        }

        public boolean toRemove() {
            return remove;
        }

        public void setRemove(boolean set) {
            remove = set;
        }

		public boolean isCaptured() {
			return captured;
		}

		public void setCaptured(boolean set) {
			captured = set;
		}

		public boolean isPrisoner() {
			return prisoner;
		}

		public void setPrisoner(boolean set) {
			prisoner = set;
		}

		public boolean isBondsman() {
			return bondsman;
		}

		public void setBondsman(boolean set) {
			bondsman = set;
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

		public boolean wasPickedUp() {
			return pickedUp;
		}

		public void setPickedUp(boolean set) {
			pickedUp = set;
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

	/**
     * This object is used to track the status of a particular unit.
     * @author Jay Lawson
     *
     */
    public class UnitStatus {

        private String name;
        private String chassis;
        private String model;
        private boolean totalLoss;
        private Entity entity;
        private Entity baseEntity;
        Unit unit;

        public UnitStatus(Unit unit) {
            this.unit = unit;
            this.name = unit.getName();
            chassis = unit.getEntity().getChassis();
            model = unit.getEntity().getModel();
            //assume its a total loss until we find something that says otherwise
            totalLoss = true;
            //create a brand new entity until we find one
            MechSummary summary = MechSummaryCache.getInstance().getMech(getLookupName());
            if(null == summary) {

            } else {
                try {
                    entity = unit.getEntity() == null ? new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity() : unit.getEntity();
                    baseEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                } catch (EntityLoadingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public String getName() {
            return name;
        }

        public String getLookupName() {
            String s = chassis + " " + model;
            s = s.trim();
            return s;
        }

        public Entity getEntity() {
            return entity;
        }

        public void assignFoundEntity(Entity e, boolean loss) {
            totalLoss = loss;
            entity = e;
        }

        public Entity getBaseEntity() {
			return baseEntity;
		}

		public void setBaseEntity(Entity baseEntity) {
			this.baseEntity = baseEntity;
		}

		public boolean isTotalLoss() {
            return totalLoss;
        }

        public void setTotalLoss(boolean b) {
            totalLoss = b;
        }

        public String getDesc() {
            // Commenting out since I can't remember why I added it... and it's weird to create a new unit when we have a unit!
            // It's also causing bugs with individual camos - ralgith
            // Unit unit = new Unit(entity, this.unit.campaign);
            String color = "black";
            if (!unit.isRepairable()) {
                color = "rgb(190, 150, 55)";
            } else if (!unit.isFunctional()) {
                color = "rgb(205, 92, 92)";
            } else {
                switch(unit.getDamageState()) {
                    case Entity.DMG_LIGHT:
                        color = "green";
                        break;
                    case Entity.DMG_MODERATE:
                        color = "yellow";
                        break;
                    case Entity.DMG_HEAVY:
                        color = "orange";
                        break;
                    case Entity.DMG_CRIPPLED:
                        color = "red";
                        break;
                }
            }
            String s = "<html><b>" + getName() + "</b><br><font color='" + color + "'>"+ unit.getStatus() + "</font></html>";
            return s;

        }
    }

    public void setEvent(GameVictoryEvent gve) {
        victoryEvent = gve;
    }

    public void clearNewPersonnel() {
        for(UUID pid : prisonerStatus.keySet()) {
            campaign.removePerson(pid, false);
        }
    }
}
