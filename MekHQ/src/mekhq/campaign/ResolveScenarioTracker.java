/*
 * ResolveScenarioTracker.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import megamek.client.Client;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameVictoryEvent;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.event.PersonBattleFinishedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.AdjustLargeCraftAmmoAction;
import mekhq.gui.FileDialogs;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This object will be the main workhorse for the scenario
 * resolution wizard. It will keep track of information and be
 * fed back and forth between the various wizards
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ResolveScenarioTracker {
    Map<UUID, Entity> entities;
    Map<UUID, List<Entity>> bayLoadedEntities;
    Map<Integer, UUID> idMap;
    Hashtable<UUID, UnitStatus> unitsStatus;
    Hashtable<UUID, UnitStatus> salvageStatus;
    Hashtable<UUID, Crew> pilots;
    Hashtable<UUID, Crew> mia;
    List<TestUnit> potentialSalvage;
    List<TestUnit> alliedUnits;
    List<TestUnit> actualSalvage;
    List<TestUnit> ransomedSalvage;
    List<TestUnit> leftoverSalvage;
    List<TestUnit> devastatedEnemyUnits;
    List<Unit> units;
    List<Loot> potentialLoot;
    List<Loot> actualLoot;
    Hashtable<UUID, PersonStatus> peopleStatus;
    Hashtable<UUID, OppositionPersonnelStatus> oppositionPersonnel;
    Hashtable<String, String> killCredits;
    Hashtable<UUID, EjectedCrew> ejections;
    Hashtable<UUID, EjectedCrew> enemyEjections;

    /* AtB */
    int contractBreaches = 0;
    int bonusRolls = 0;

    Campaign campaign;
    Scenario scenario;
    Optional<File> unitList = Optional.empty();
    Client client;
    Boolean control;
    private GameVictoryEvent victoryEvent;

    public ResolveScenarioTracker(Scenario s, Campaign c, boolean ctrl) {
        this.scenario = s;
        this.campaign = c;
        this.control = ctrl;
        unitsStatus = new Hashtable<>();
        salvageStatus = new Hashtable<>();
        potentialSalvage = new ArrayList<>();
        alliedUnits = new ArrayList<>();
        actualSalvage = new ArrayList<>();
        ransomedSalvage = new ArrayList<>();
        leftoverSalvage = new ArrayList<>();
        devastatedEnemyUnits = new ArrayList<>();
        pilots = new Hashtable<>();
        mia = new Hashtable<>();
        units = new ArrayList<>();
        potentialLoot = scenario.getLoot();
        actualLoot = new ArrayList<>();
        peopleStatus = new Hashtable<>();
        oppositionPersonnel = new Hashtable<>();
        killCredits = new Hashtable<>();
        ejections = new Hashtable<>();
        enemyEjections = new Hashtable<>();
        entities = new HashMap<>();
        bayLoadedEntities = new HashMap<>();
        idMap = new HashMap<>();
        for (UUID uid : scenario.getForces(campaign).getAllUnits(true)) {
            Unit u = campaign.getUnit(uid);
            if (null != u && null == u.checkDeployment()) {
                units.add(u);
                //assume its missing until we can confirm otherwise
                unitsStatus.put(uid, new UnitStatus(u));
            }
        }
        // add potential traitor units
        for (Unit u : scenario.getTraitorUnits(campaign)) {
            units.add(u);
            // assume its missing until we can confirm otherwise
            unitsStatus.put(u.getId(), new UnitStatus(u));
        }
    }

    public void findUnitFile() {
        unitList = FileDialogs.openUnits(null);
    }

    public String getUnitFilePath() {
        return unitList.map(File::getAbsolutePath).orElse("No file selected");
    }

    public void setClient(Client c) {
        client = c;
    }

    public void processMulFiles() {
        if (unitList.isPresent()) {
            try {
                loadUnitsAndPilots(unitList.get());
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        } else {
            initUnitsAndPilotsWithoutBattle();
        }
        checkStatusOfPersonnel();
    }

    private TestUnit generateNewTestUnit(Entity e) {
        TestUnit nu = new TestUnit(e, campaign, true);
        nu.getEntity().setCamouflage(e.getCamouflage().clone());
        /* AtB uses id to track status of allied units */
        if (e.getExternalIdAsString().equals("-1")) {
            UUID id = UUID.randomUUID();
            nu.getEntity().setExternalIdAsString(id.toString());
            nu.setId(id);
        } else {
            nu.setId(UUID.fromString(e.getExternalIdAsString()));
        }

        for (Part part : nu.getParts()) {
            part.setBrandNew(false);
        }
        return nu;
    }

    public void processGame() {
        int pid = client.getLocalPlayer().getId();
        int team = client.getLocalPlayer().getTeam();

        for (Enumeration<Entity> iter = victoryEvent.getEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if (!e.getSubEntities().isEmpty()) {
                // Sub-entities have their own entry in the VictoryEvent data
                continue;
            }

            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            //Convenience data
            idMap.put(e.getId(), UUID.fromString(e.getExternalIdAsString()));

            checkForLostLimbs(e, control);
            if ((e.getOwnerId() == pid) || (e.getOwner().getTeam() == team) || scenario.isTraitor(e, campaign)) {
                if (!"-1".equals(e.getExternalIdAsString())) {
                    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                    if (null == status && scenario instanceof AtBScenario) {
                        status = processAlliedUnit(e);
                    }

                    if (null != status) {
                        boolean lost = (!e.canEscape() && !control) || e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED;
                        status.assignFoundEntity(e, lost);
                    }
                }
                if (null != e.getCrew()) {
                    if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                        if (!e.getCrew().isEjected() || (e instanceof EjectedCrew)) {
                            pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                        }
                        if (e instanceof EjectedCrew) {
                            ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                        }
                    }
                }
            } else if (e.getOwner().isEnemyOf(client.getLocalPlayer())) {
                if (control) {
                    if (e instanceof EjectedCrew) {
                        enemyEjections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                        continue;
                    }

                    if ((e instanceof BattleArmor) && e.isDestroyed()) {
                        // BA can only be salvaged with a 10+ roll
                        if (Utilities.dice(2, 6) < 10) {
                            continue;
                        }
                    }

                    TestUnit nu = generateNewTestUnit(e);
                    UnitStatus us = new UnitStatus(nu);
                    us.setTotalLoss(false);
                    salvageStatus.put(nu.getId(), us);
                    potentialSalvage.add(nu);
                }
            }
            // Kill credit automatically assigned only if they can't escape
            if (!e.canEscape()) {
                appendKillCredit(e);
            }
        }

        //If any units ended the game with others loaded in its bays, map those out
        for (Enumeration<Entity> iter = victoryEvent.getEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if (!e.getBayLoadedUnitIds().isEmpty()) {
                List<Entity> cargo = new ArrayList<>();
                for (int id : e.getBayLoadedUnitIds()) {
                    UUID extId = idMap.get(id);
                    if (extId != null) {
                        cargo.add(entities.get(extId));
                    }
                }
                bayLoadedEntities.put(UUID.fromString(e.getExternalIdAsString()), cargo);
            }
        }

        // Utterly destroyed entities
        for (Enumeration<Entity> iter = victoryEvent.getDevastatedEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if (!e.getSubEntities().isEmpty()) {
                // Sub-entities have their own entry in the VictoryEvent data
                continue;
            }

            entities.put(UUID.fromString(e.getExternalIdAsString()), e);

            if ((e.getOwnerId() == pid) || (e.getOwner().getTeam() == team) || scenario.isTraitor(e, campaign)) {
                if (!"-1".equals(e.getExternalIdAsString())) {
                    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));

                    if (null == status && scenario instanceof AtBScenario) {
                        status = processAlliedUnit(e);
                    }

                    if (null != status) {
                        status.assignFoundEntity(e, true);
                    }
                }
            }

            appendKillCredit(e);
        }

        //add retreated units
        for (Enumeration<Entity> iter = victoryEvent.getRetreatedEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if (!e.getSubEntities().isEmpty()) {
                // Sub-entities have their own entry in the VictoryEvent data
                continue;
            }

            entities.put(UUID.fromString(e.getExternalIdAsString()), e);

            checkForLostLimbs(e, control);
            if ((e.getOwnerId() == pid) || (e.getOwner().getTeam() == team) || scenario.isTraitor(e, campaign)) {
                if (!"-1".equals(e.getExternalIdAsString())) {
                    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                    if (null == status && scenario instanceof AtBScenario) {
                        status = processAlliedUnit(e);
                    }

                    if (null != status) {
                        status.assignFoundEntity(e, false);
                    }
                }
                if (null != e.getCrew()) {
                    if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                        pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                        if (e instanceof EjectedCrew) {
                            ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                        }
                    }
                }
            }
        }

        Enumeration<Entity> wrecks = victoryEvent.getGraveyardEntities();
        while (wrecks.hasMoreElements()) {
            Entity e = wrecks.nextElement();
            if (!e.getSubEntities().isEmpty()) {
                // Sub-entities have their own entry in the VictoryEvent data
                continue;
            }

            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            idMap.put(e.getId(), UUID.fromString(e.getExternalIdAsString()));

            checkForLostLimbs(e, control);
            if ((e.getOwnerId() == pid) || (e.getOwner().getTeam() == team) || scenario.isTraitor(e, campaign)) {
                if (!"-1".equals(e.getExternalIdAsString())) {
                    UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));

                    if (null == status && scenario instanceof AtBScenario) {
                        status = processAlliedUnit(e);
                    }

                    if (null != status) {
                        status.assignFoundEntity(e, !control);
                        if (e instanceof EjectedCrew) {
                            ejections.put(UUID.fromString(e.getExternalIdAsString()), (EjectedCrew) e);
                        }
                    }
                }
                if (null != e.getCrew()) {
                    if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                        if (e instanceof EjectedCrew) {
                            ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                        }
                        if (!e.getCrew().isEjected() || e instanceof EjectedCrew) {
                            if (control) {
                                pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                            } else {
                                mia.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                            }
                        }
                    }
                }
            } else if (e.getOwner().isEnemyOf(client.getLocalPlayer())) {
                if (e instanceof EjectedCrew) {
                    enemyEjections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    continue;
                }
                if (control) {
                    TestUnit nu = generateNewTestUnit(e);
                    UnitStatus us = new UnitStatus(nu);
                    us.setTotalLoss(false);
                    salvageStatus.put(nu.getId(), us);
                    potentialSalvage.add(nu);
                }
            }

            appendKillCredit(e);
        }
        //If a unit in a bay was destroyed, add it. We still need to deal with the crew
        for (Enumeration<Entity> iter = victoryEvent.getGraveyardEntities(); iter.hasMoreElements();) {
            Entity e = iter.nextElement();
            if (e.getTransportId() != Entity.NONE) {
                UUID trnId = idMap.get(e.getTransportId());
                List<Entity> cargo;
                if (bayLoadedEntities.containsKey(trnId)) {
                    cargo = bayLoadedEntities.get(trnId);
                } else {
                    cargo = new ArrayList<>();
                }
                cargo.add(e);
                bayLoadedEntities.put(trnId, cargo);
            }
        }
        checkStatusOfPersonnel();
    }

    /**
     * Worker function that, where appropriate, appends kill credit to the local killCredits tracker
     */
    private void appendKillCredit(Entity e) {
        // no need to add kill credits for player or allied units
        if (!e.getOwner().isEnemyOf(client.getLocalPlayer())) {
            return;
        }

        Entity killer = victoryEvent.getEntity(e.getKillerId());

        if ((null != killer) && !"-1".equals(killer.getExternalIdAsString())) {
            killCredits.put(e.getDisplayName(), killer.getExternalIdAsString());
        } else {
            killCredits.put(e.getDisplayName(), "None");
        }
    }

    private UnitStatus processAlliedUnit(Entity e) {
        TestUnit nu = generateNewTestUnit(e);
        UnitStatus us = new UnitStatus(nu);
        unitsStatus.put(nu.getId(), us);
        alliedUnits.add(nu);

        return us;
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
        for (int loc = 0; loc < en.locations(); loc++) {
            if (en.isLocationBlownOff(loc) && !controlsField) {
                // Sorry dude, we can't find your arm
                en.setLocationBlownOff(loc, false);
                en.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                en.setInternal(IArmorState.ARMOR_DESTROYED, loc);
            }
            // Check for mounted and critical slot missingness as well
            for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
                final CriticalSlot cs = en.getCritical(loc, i);
                if (null == cs || !cs.isEverHittable()) {
                    continue;
                }
                Mounted m = cs.getMount();
                if (cs.isMissing()) {
                    if (controlsField) {
                        cs.setMissing(false);
                        if (null != m) {
                            m.setMissing(false);
                        }
                    } else {
                        if (null != m) {
                            m.setMissing(true);
                        }
                    }
                }
            }
        }
    }

    private List<Person> shuffleCrew(List<Person> source) {
        List<Person> sortedList = new ArrayList<>();
        Random generator = new Random();

        while (!source.isEmpty()) {
            int position = generator.nextInt(source.size());
            sortedList.add(source.get(position));
            source.remove(position);
        }

        return sortedList;
    }

    public void assignKills() {
        for (Unit u : units) {
            for (String killed : killCredits.keySet()) {
                if (killCredits.get(killed).equalsIgnoreCase("None")) {
                    continue;
                }
                if (u.getId().toString().equals(killCredits.get(killed))) {
                    for (Person p : u.getActiveCrew()) {
                        PersonStatus status = peopleStatus.get(p.getId());
                        if (null == status) {
                            //this shouldn't happen so report
                            LogManager.getLogger().error(
                                    "A null person status was found for person id " + p.getId().toString()
                                    + " when trying to assign kills");
                            continue;
                        }
                        status.addKill(new Kill(p.getId(), killed, u.getEntity().getShortNameRaw(), campaign.getLocalDate(),
                                getMissionId(), getScenarioId()));
                    }
                }
            }

        }
    }

    public void checkStatusOfPersonnel() {
        // lets cycle through units and get their crew
        for (Unit u : units) {
            // shuffling the crew ensures that casualties are randomly assigned in multi-crew units
            List<Person> crew = shuffleCrew(u.getActiveCrew());
            Entity en = null;
            UnitStatus unitStatus = unitsStatus.get(u.getId());
            if (null != unitStatus) {
                en = unitStatus.getEntity();
            }
            if (null == en) {
                continue;
            }
            //Handle spacecraft a bit differently
            if ((en instanceof SmallCraft) || (en instanceof Jumpship)) {
                processLargeCraft(u, en, crew, unitStatus);
            } else {
                if (en.getTransportId() != Entity.NONE) {
                    // Check to see if the unit is in a large craft bay, if so, its crew will be processed with the ship,
                    // so ignore it here.
                    UUID trnId = idMap.get(en.getTransportId());
                    if (trnId != null) {
                        Entity transport = unitsStatus.get(trnId).getEntity();
                        if ((transport != null) && transport.isLargeCraft()) {
                            continue;
                        }
                    }
                }
                // check for an ejected entity and if we find one then assign it instead to switch vees
                // over to infantry checks for casualties
                Entity ejected = ejections.get(UUID.fromString(en.getCrew().getExternalIdAsString()));
                // determine total casualties for infantry and large craft
                int casualties = 0;
                int casualtiesAssigned = 0;
                Infantry infantry = null;
                if (en instanceof Infantry) {
                    infantry = (Infantry) en;
                } else if (ejected != null) {
                    infantry = (Infantry) ejected;
                }
                if (infantry != null) {
                    infantry.applyDamage();
                    // If reading from a MUL, the shooting strength is set to Integer.MAX_VALUE if there is no damage.
                    int strength = Math.min(infantry.getShootingStrength(), crew.size());
                    casualties = crew.size() - strength;
                    if (unitStatus.isTotalLoss()) {
                        casualties = crew.size();
                    }
                    // If a tank has already taken hits to the commander or driver, do not assign them again.
                    if (en instanceof Tank) {
                        if (((Tank) en).isDriverHit()) {
                            casualtiesAssigned++;
                        }
                        if (((Tank) en).isCommanderHit()) {
                            casualtiesAssigned++;
                        }
                    }
                }
                //try to find the crew in our pilot and mia vectors
                Crew pilot = pilots.get(u.getCommander().getId());
                boolean missingCrew = false;
                //For multi-crew cockpits, the crew id is the first slot, which is not necessarily the commander
                if (null == pilot) {
                    for (Person p : u.getCrew()) {
                        if (pilots.containsKey(p.getId())) {
                            pilot = pilots.get(p.getId());
                            break;
                        }
                    }
                }
                if (null == pilot) {
                    pilot = mia.get(UUID.fromString(en.getCrew().getExternalIdAsString()));
                    missingCrew = true;
                }
                for (Person p : crew) {
                    PersonStatus status = new PersonStatus(p.getFullName(), u.getEntity().getDisplayName(),
                            p.getHits(), p.getId());
                    status.setMissing(missingCrew);
                    // if the pilot was not found in either the pilot or mia vector
                    // then the unit was devastated and no one ejected, so they should be dead, really dead
                    if (null == pilot) {
                        status.setHits(6);
                        status.setDead(true);
                    }
                    // multi-crewed cockpit; set each crew member separately
                    else if (pilot.getSlotCount() > 1) {
                        for (int slot = 0; slot < pilot.getSlotCount(); slot++) {
                            if (p.getId().toString().equals(pilot.getExternalIdAsString(slot))) {
                                status.setHits(pilot.getHits(slot));
                                break;
                            }
                        }
                    // else if: can't do the following by u.usesSoloPilot because entity may be different if ejected
                    } else if (en instanceof Mech || en instanceof Protomech || en.isFighter()) {
                        status.setHits(pilot.getHits());
                    } else {
                        // we have a multi-crewed Vehicle/Aero/Infantry
                        boolean wounded = false;
                        // tanks need to be handled specially because of the special crits and because
                        // tank destruction should "kill" the crew
                        if (en instanceof Tank) {
                            boolean destroyed = false;
                            for (int loc = 0; loc < en.locations(); loc++) {
                                if (loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2 || loc == Tank.LOC_BODY) {
                                    continue;
                                }
                                if (en.getInternal(loc) <= 0) {
                                    destroyed = true;
                                    break;
                                }
                            }
                            if (destroyed || null == en.getCrew() || en.getCrew().isDead()) {
                                if (Compute.d6(2) >= 7) {
                                    wounded = true;
                                } else {
                                    status.setHits(6);
                                    status.setDead(true);
                                }
                            } else if (((Tank) en).isDriverHit() && u.isDriver(p)) {
                                if (Compute.d6(2) >= 7) {
                                    wounded = true;
                                } else {
                                    status.setHits(6);
                                    status.setDead(true);
                                }
                            } else if (((Tank) en).isCommanderHit() && (u.isCommander(p)
                                    || u.isTechOfficer(p))) {
                                //If there is a command console, the commander hit flag is set on the second such critical,
                                //which means both commanders have been hit.
                                if (Compute.d6(2) >= 7) {
                                    wounded = true;
                                } else {
                                    status.setHits(6);
                                    status.setDead(true);
                                }
                            } else if (((Tank) en).isUsingConsoleCommander() && u.isCommander(p)) {
                                //This flag is set after the first commander hit critical.
                                if (Compute.d6(2) >= 7) {
                                    wounded = true;
                                } else {
                                    status.setHits(6);
                                    status.setDead(true);
                                }
                            }
                        }
                        if (casualtiesAssigned < casualties) {
                            casualtiesAssigned++;
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }
                        }
                        if (wounded) {
                            int hits = campaign.getCampaignOptions().getMinimumHitsForVehicles();
                            if (campaign.getCampaignOptions().isUseAdvancedMedical() || campaign.getCampaignOptions().isUseRandomHitsForVehicles()) {
                                int range = 6 - hits;
                                hits = hits + Compute.randomInt(range);
                            }
                            status.setHits(hits);
                        }
                    }
                    status.setXP(campaign.getCampaignOptions().getScenarioXP());
                    status.setDeployed(!en.wasNeverDeployed());
                    peopleStatus.put(p.getId(), status);
                }
            }
        }

        // And now we have potential prisoners that are crewing a unit...
        if (!campaign.getCampaignOptions().getPrisonerCaptureStyle().isNone()) {
            processPrisonerCapture(potentialSalvage);
            processPrisonerCapture(devastatedEnemyUnits);
        }
    }

    /**
     * Helper function that handles crew and passengers ejected from a large spacecraft,
     * which may be scattered about on numerous other entities
     * @param ship The large craft unit we're currently processing
     * @param en The entity associated with the unit Ship
     * @param personnel The list of persons assigned to the ship as crew and marines
     * @param unitStatus The post-battle status of en
     */
    private void processLargeCraft(Unit ship, Entity en, List<Person> personnel,
                                   UnitStatus unitStatus) {
        // The entity must be an Aero for us to get here
        Aero aero = (Aero) en;
        // Find out if this large craft ejected or was in the process of ejecting,
        // and if so what entities are carrying the personnel
        int rescuedCrew = 0;
        int rescuedPassengers = 0;
        if (en.getCrew().isEjected() || aero.isEjecting()) {
            for (String id : aero.getEscapeCraft()) {
                Entity e = entities.get(UUID.fromString(id));
                // Invalid entity?
                if (e == null) {
                    LogManager.getLogger().error("Null entity reference in:" + aero.getDisplayName() + "getEscapeCraft()");
                    continue;
                }
                // If the escape craft was destroyed in combat, skip it
                if (e.isDestroyed() || e.isDoomed()) {
                    continue;
                }
                // Now let's see how many passengers and crew we picked up
                if (e instanceof SmallCraft) {
                    SmallCraft craft = (SmallCraft) e;
                    if (craft.getPassengers().get(en.getExternalIdAsString()) != null) {
                        rescuedPassengers += craft.getPassengers().get(en.getExternalIdAsString());
                    }

                    if (craft.getNOtherCrew().get(en.getExternalIdAsString()) != null) {
                        rescuedCrew += craft.getNOtherCrew().get(en.getExternalIdAsString());
                    }
                } else if (e instanceof EjectedCrew) {
                    EjectedCrew crew = (EjectedCrew) e;
                    if (crew.getPassengers().get(en.getExternalIdAsString()) != null) {
                        rescuedPassengers += crew.getPassengers().get(en.getExternalIdAsString());
                    }

                    if (crew.getNOtherCrew().get(en.getExternalIdAsString()) != null) {
                        rescuedCrew += crew.getNOtherCrew().get(en.getExternalIdAsString());
                    }
                }
            }
        }
        // Check crewed aeros for existing hits since they could be flying without full crews
        int casualties;
        int casualtiesAssigned = 0;
        int existingHits = 0;
        int currentHits = 0;
        if (null != ship.getEntity().getCrew()) {
            existingHits = ship.getEntity().getCrew().getHits();
        }

        if (null != en.getCrew()) {
            currentHits = en.getCrew().getHits();
        }

        if (en.isDestroyed()) {
            currentHits = 6;
        }
        int newHits = Math.max(0, currentHits - existingHits);
        casualties = (int) Math.ceil(Compute.getFullCrewSize(en) * (newHits / 6.0));
        // Now reduce the casualties if some "hits" were caused by ejection
        casualties = Math.max(0, casualties - rescuedCrew);

        // And assign the casualties and experience amongst the crew and marines
        for (Person p : personnel) {
            PersonStatus status = new PersonStatus(p.getFullName(),
                    ship.getEntity().getDisplayName(), p.getHits(), p.getId());
            boolean wounded = false;
            if (casualtiesAssigned < casualties) {
                casualtiesAssigned++;
                if (Compute.d6(2) >= 7) {
                    wounded = true;
                } else {
                    status.setHits(6);
                    status.setDead(true);
                }
            }

            if (wounded) {
                int hits = campaign.getCampaignOptions().getMinimumHitsForVehicles();
                if (campaign.getCampaignOptions().isUseAdvancedMedical() || campaign.getCampaignOptions().isUseRandomHitsForVehicles()) {
                    int range = 6 - hits;
                    hits = hits + Compute.randomInt(range);
                }
                status.setHits(hits);
            }
            status.setXP(campaign.getCampaignOptions().getScenarioXP());
            status.setDeployed(!en.wasNeverDeployed());
            peopleStatus.put(p.getId(), status);
        }

        // Now, did the passengers take any hits?
        // We'll assume that if units in transport bays were hit, their crews and techs might also have been
        Set<PersonStatus> allPassengersStatus = new HashSet<>(); //Use this to keep track of ejected passengers for the next step
        List<Entity> cargo = bayLoadedEntities.get(UUID.fromString(en.getExternalIdAsString()));
        if (cargo != null) {
            for (Entity e : cargo) {
                // Match the still-loaded cargo entity with its unit so we can get the crew
                Unit u = campaign.getUnit(UUID.fromString(e.getExternalIdAsString()));
                if (u != null) {
                    List<Person> cargoCrew = u.getActiveCrew();
                    cargoCrew.add(u.getTech());
                    cargoCrew = shuffleCrew(cargoCrew);
                    for (Person p : cargoCrew) {
                        PersonStatus status = new PersonStatus(p.getFullName(),
                                u.getEntity().getDisplayName(), p.getHits(), p.getId());
                        boolean wounded = false;
                        // The lore says bay crews have pressurized sleeping alcoves in the corners of each bay
                        // Let's assume people are injured on an 8+ if the unit is destroyed, same as a critical hit chance
                        if (e.isDestroyed() && Compute.d6(2) >= 8) {
                            // As with crewmembers, on a 7+ they're only wounded
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }

                            if (wounded) {
                                int hits = campaign.getCampaignOptions().getMinimumHitsForVehicles();
                                if (campaign.getCampaignOptions().isUseAdvancedMedical() || campaign.getCampaignOptions().isUseRandomHitsForVehicles()) {
                                    int range = 6 - hits;
                                    hits = hits + Compute.randomInt(range);
                                }
                                status.setHits(hits);
                            }
                        }
                        // Go ahead and add everyone to this master list, even if they're killed/wounded above.
                        // It's normal to have some casualties in the lifeboats...
                        allPassengersStatus.add(status);
                    }
                }
            }
        }
        // Now let's account for any passengers aboard escape craft
        // If the host ship ended the game mid-ejection but remains undestroyed, add its remaining passengers
        if (aero.isEjecting() || aero.getCrew().isEjected()) {
            if (aero.isEjecting() && !aero.isDestroyed()) {
                rescuedPassengers = allPassengersStatus.size();
            }
            // Convert the set to a list so we can pick a random value by index...
            List<PersonStatus> allPassengersStatusList = new ArrayList<>(allPassengersStatus);

            // Let's go through and handle the list
            while (rescuedPassengers > 0) {
                if (allPassengersStatus.isEmpty()) {
                    // Could happen on ships with passenger quarters where numbers exceed those associated
                    // with transported units, or ships that are just empty
                    break;
                }
                PersonStatus s = allPassengersStatusList.remove(Compute.randomInt(allPassengersStatusList.size()));
                UUID pid = s.getId();
                peopleStatus.put(pid, s);
                rescuedPassengers --;
            }
            // Everyone who didn't make it to an escape craft dies
            for (PersonStatus s : allPassengersStatusList) {
                s.setHits(6);
                s.setDead(true);
                peopleStatus.put(s.getId(), s);
            }
        } else {
            // No ejection is involved, just add everyone to our master peopleStatus table
            for (PersonStatus s : allPassengersStatus) {
                peopleStatus.put(s.getId(), s);
            }
        }
    }

    /**
     * Helper function that contains the logic for processing prisoner capture.
     * Copy and pasted from checkStatusOfPersonnel, so the internal logic is kind of opaque.
     * @param unitsToProcess The list of TestUnit entities to process. Note that
     *                  in order to be processed, a unit must be in the salvageStatus hashtable.
     */
    private void processPrisonerCapture(List<TestUnit> unitsToProcess) {
        Mission currentMission = campaign.getMission(scenario.getMissionId());
        String enemyCode;
        if (currentMission instanceof AtBContract) {
            enemyCode = ((AtBContract) currentMission).getEnemyCode();
        } else {
            enemyCode = "IND";
        }

        for (Unit u : unitsToProcess) {
            if (null == u) {
                continue; // Shouldn't happen... but well... ya know
            }
            Entity en = null;
            UnitStatus ustatus = salvageStatus.get(u.getId());
            if (null != ustatus) {
                en = ustatus.getEntity();
            }
            if (null == en) {
                continue;
            }
            // check for an ejected entity and if we find one then assign it instead to switch vees
            // over to infantry checks for casualties
            Entity ejected = null;
            if (!en.getCrew().getExternalIdAsString().equals("-1")) {
                ejected = enemyEjections.get(UUID.fromString(en.getCrew().getExternalIdAsString()));
            }

            if (null != ejected) {
                en = ejected;
            }
            // check if this ejection was picked up by a player's unit
            boolean pickedUp = en instanceof MechWarrior
                    && !((MechWarrior) en).getPickedUpByExternalIdAsString().equals("-1")
                    && null != unitsStatus.get(UUID.fromString(((MechWarrior) en).getPickedUpByExternalIdAsString()));
            // If this option is turned on and the player controls the battlefield,
            // assume that all ejected warriors have been picked up
            if (campaign.getGameOptions().booleanOption(OptionsConstants.ADVGRNDMOV_EJECTED_PILOTS_FLEE)) {
                pickedUp = true;
            }
            // if the crew ejected from this unit, then skip it because we should find them elsewhere
            // if they are alive
            if (!(en instanceof EjectedCrew)
                    && null != en.getCrew()
                    && (en.getCrew().isEjected() && !campaign.getGameOptions().booleanOption(OptionsConstants.ADVGRNDMOV_EJECTED_PILOTS_FLEE))) {
                continue;
            }
            // shuffling the crew ensures that casualties are randomly assigned in multi-crew units
            List<Person> crew = Utilities.genRandomCrewWithCombinedSkill(campaign, u, enemyCode).values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            crew = shuffleCrew(crew);

            // For vees we may need to know the commander or driver, which aren't assigned for TestUnit.
            Person commander = null;
            Person driver = null;
            Person console = null;
            if (en instanceof Tank) {
                // Prefer gunner over driver, as in Unit::getCommander
                for (Person p : crew) {
                    if (p.getPrimaryRole().isVehicleGunner()) {
                        commander = p;
                    } else if (p.getPrimaryRole().isGroundVehicleDriver()
                            || p.getPrimaryRole().isNavalVehicleDriver()
                            || p.getPrimaryRole().isVTOLPilot()) {
                        driver = p;
                    }
                }

                if (en.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
                    for (Person p : crew) {
                        if (!p.equals(commander) && !p.equals(driver)) {
                            console = p;
                            break;
                        }
                    }
                }
            }

            if ((commander == null) && !crew.isEmpty()) {
                commander = crew.get(0);
            }

            int casualties = 0;
            int casualtiesAssigned = 0;
            if (en instanceof Infantry) {
                en.applyDamage();
                int strength = ((Infantry) en).getShootingStrength();
                casualties = crew.size() - strength;
            }

            if ((en instanceof Aero) && !u.usesSoloPilot()) {
                // need to check for existing hits because you can fly aeros with less than full crew
                int existingHits = 0;
                int currentHits = 0;
                if (null != u.getEntity().getCrew()) {
                    existingHits = u.getEntity().getCrew().getHits();
                }

                if (null != en.getCrew()) {
                    currentHits = en.getCrew().getHits();
                }
                int newHits = Math.max(0, currentHits - existingHits);
                casualties = (int) Math.ceil(Compute.getFullCrewSize(en) * (newHits / 6.0));
            }

            for (Person p : crew) {
                OppositionPersonnelStatus status = new OppositionPersonnelStatus(p.getFullName(), u.getEntity().getDisplayName(), p);
                if (en instanceof Mech
                        || en instanceof Protomech
                        || en.isFighter()
                        || en instanceof MechWarrior) {
                    Crew pilot = en.getCrew();
                    if (null == pilot) {
                        continue;
                    }
                    int slot = 0;
                    //For multi-person cockpits the person id has been set to match the crew slot
                    for (int pos = 0; pos < pilot.getSlotCount(); pos++) {
                        if (p.getId().toString().equals(pilot.getExternalIdAsString(pos))) {
                            slot = pos;
                            break;
                        }
                    }
                    status.setHits(pilot.getHits(slot));
                } else {
                    //we have a multi-crewed vee
                    boolean wounded = false;
                    if (en instanceof Tank) {
                        boolean destroyed = false;
                        for (int loc = 0; loc < en.locations(); loc++) {
                            if (loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2 || loc == Tank.LOC_BODY) {
                                continue;
                            }
                            if (en.getInternal(loc) <= 0) {
                                destroyed = true;
                                break;
                            }
                        }

                        if (destroyed || (null == en.getCrew()) || en.getCrew().isDead()) {
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                            }
                        } else if (((Tank) en).isDriverHit() && p.equals(driver)) {
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }
                        } else if (((Tank) en).isCommanderHit() && (p.equals(commander) || p.equals(console))) {
                            //If there is a command console, the commander hit flag does not
                            //get set until after the second such critical, which means that
                            //both commanders have been hit.
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }
                        } else if (((Tank) en).isUsingConsoleCommander() && p.equals(commander)) {
                            //If this flag is set we are using a command console and have already
                            //taken one commander hit critical, which takes out the primary commander.
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }
                        }
                    } else if (en instanceof Infantry || en instanceof Aero) {
                        if (casualtiesAssigned < casualties) {
                            casualtiesAssigned++;
                            if (Compute.d6(2) >= 7) {
                                wounded = true;
                            } else {
                                status.setHits(6);
                                status.setDead(true);
                            }
                        }
                    }
                    if (wounded) {
                        int hits = campaign.getCampaignOptions().getMinimumHitsForVehicles();
                        if (campaign.getCampaignOptions().isUseAdvancedMedical() || campaign.getCampaignOptions().isUseRandomHitsForVehicles()) {
                            int range = 6 - hits;
                            hits = hits + Compute.randomInt(range);
                        }
                        status.setHits(hits);
                    }
                }
                status.setCaptured(Utilities.isLikelyCapture(en) || pickedUp);
                status.setXP(campaign.getCampaignOptions().getScenarioXP());
                oppositionPersonnel.put(p.getId(), status);
            }
        }
    }

    private void loadUnitsAndPilots(final @Nullable File unitFile) {
        if (unitFile == null) {
            return;
        }

        // I need to get the parser myself, because I want to pull both
        // entities and pilots from it
        // Create an empty parser.
        final MULParser parser;
        try {
            parser = new MULParser(unitFile, campaign.getGameOptions());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return;
        }

        killCredits = parser.getKills();

        //Map everyone's ID to External Id
        for (Entity e : parser.getEntities()) {
            idMap.put(e.getId(), UUID.fromString(e.getExternalIdAsString()));
        }
        for (Entity e : parser.getSalvage()) {
            idMap.put(e.getId(), UUID.fromString(e.getExternalIdAsString()));
        }
        for (Entity e : parser.getRetreated()) {
            idMap.put(e.getId(), UUID.fromString(e.getExternalIdAsString()));
        }

        //If any units ended the game with others loaded in its bays, map those out
        for (Entity e : parser.getEntities()) {
            if (!e.getBayLoadedUnitIds().isEmpty()) {
                List<Entity> cargo = new ArrayList<>();
                for (int id : e.getBayLoadedUnitIds()) {
                    UUID extId = idMap.get(id);
                    if (extId != null) {
                        cargo.add(entities.get(extId));
                    }
                }
                bayLoadedEntities.put(UUID.fromString(e.getExternalIdAsString()), cargo);
            }
        }

        for (Entity e : parser.getSurvivors()) {
            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            checkForLostLimbs(e, control);
            if (!"-1".equals(e.getExternalIdAsString())) {
                UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                if (null == status && scenario instanceof AtBScenario && !(e instanceof EjectedCrew)) {
                    status = processAlliedUnit(e);
                }

                if (null != status) {
                    boolean lost = (!e.canEscape() && !control) || e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED;
                    status.assignFoundEntity(e, lost);
                }
            }

            if (null != e.getCrew()) {
                if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                    if (!e.getCrew().isEjected() || e instanceof EjectedCrew) {
                        pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                    }
                    if (e instanceof EjectedCrew) {
                        ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    }

                }
            }
        }

        for (Entity e : parser.getAllies()) {
            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            checkForLostLimbs(e, control);
            if (!"-1".equals(e.getExternalIdAsString())) {
                UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                if (null == status && scenario instanceof AtBScenario && !(e instanceof EjectedCrew)) {
                    status = processAlliedUnit(e);
                }

                if (null != status) {
                    boolean lost = (!e.canEscape() && !control) || e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED;
                    status.assignFoundEntity(e, lost);
                }
            }
            if (null != e.getCrew()) {
                if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                    if (!e.getCrew().isEjected() || e instanceof EjectedCrew) {
                        pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                    }
                    if (e instanceof EjectedCrew) {
                        ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    }
                }
            }
        }

        // Utterly destroyed entities
        for (Entity e : parser.getDevastated()) {
            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            UnitStatus status = null;
            if (!"-1".equals(e.getExternalIdAsString())) {
                status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
            }
            if (null != status) {
                status.assignFoundEntity(e, true);
            } else {
                // completely destroyed units (such as from an ammo explosion) need to be
                // kept track of, as mechwarriors may eject from them, etc.
                TestUnit nu = generateNewTestUnit(e);
                UnitStatus us = new UnitStatus(nu);
                salvageStatus.put(nu.getId(), us);
                devastatedEnemyUnits.add(nu);
            }
        }

        for (Entity e : parser.getSalvage()) {
            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            checkForLostLimbs(e, control);
            UnitStatus status = null;
            if (!"-1".equals(e.getExternalIdAsString()) && e.isSalvage()) {
                // Check to see if this is a friendly deployed unit with a unit ID in the campaign
                status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
            }
            //If a unit in a bay was destroyed, add it. We still need to deal with the crew
            if (e.getTransportId() != Entity.NONE) {
                UUID trnId = idMap.get(e.getTransportId());
                List<Entity> cargo;
                if (bayLoadedEntities.containsKey(trnId)) {
                    cargo = bayLoadedEntities.get(trnId);
                } else {
                    cargo = new ArrayList<>();
                }
                e.setDestroyed(true);
                cargo.add(e);
                bayLoadedEntities.put(trnId, cargo);
            }
            if (null != status) {
                status.assignFoundEntity(e, !control);
                if (null != e.getCrew()) {
                    if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                        if (e instanceof EjectedCrew) {
                            ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                        }
                        if (!e.getCrew().isEjected() || e instanceof EjectedCrew) {
                            if (control) {
                                pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                            } else {
                                mia.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                            }
                        }
                    }
                }
            } else {
                // Enemy crew/pilot entity is actually in the salvage list
                if ((e instanceof EjectedCrew) && (null != e.getCrew()) &&
                        !"-1".equals(e.getCrew().getExternalIdAsString())) {
                    // check for possible traitors
                    if (scenario.isTraitor(UUID.fromString(e.getCrew().getExternalIdAsString()))) {
                        pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                        ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    } else {
                        enemyEjections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    }
                    continue;
                }
                if (control) {
                    TestUnit nu = generateNewTestUnit(e);
                    UnitStatus us = new UnitStatus(nu);
                    us.setTotalLoss(false);
                    salvageStatus.put(nu.getId(), us);
                    potentialSalvage.add(nu);
                }
            }
        }

        for (Entity e : parser.getRetreated()) {
            if (!"-1".equals(e.getExternalIdAsString())) {
                UnitStatus status = unitsStatus.get(UUID.fromString(e.getExternalIdAsString()));
                if (null == status && scenario instanceof AtBScenario) {
                    status = processAlliedUnit(e);
                }

                if (null != status) {
                    status.assignFoundEntity(e, false);
                }
                if (!e.getBayLoadedUnitIds().isEmpty()) {
                    List<Entity> cargo = new ArrayList<>();
                    for (int id : e.getBayLoadedUnitIds()) {
                        UUID extId = idMap.get(id);
                        if (extId != null) {
                            cargo.add(entities.get(extId));
                        }
                    }
                    bayLoadedEntities.put(UUID.fromString(e.getExternalIdAsString()), cargo);
                }
            }
            if (null != e.getCrew()) {
                if (!"-1".equals(e.getCrew().getExternalIdAsString())) {
                    pilots.put(UUID.fromString(e.getCrew().getExternalIdAsString()), e.getCrew());
                    if (e instanceof EjectedCrew) {
                        ejections.put(UUID.fromString(e.getCrew().getExternalIdAsString()), (EjectedCrew) e);
                    }
                }
            }

            entities.put(UUID.fromString(e.getExternalIdAsString()), e);
        }
    }

    /**
     * When resolving the battle manually without a resolution file (such as MekHQ + tabletop),
     * set initial status of units and crew as pre-battle.
     */
    private void initUnitsAndPilotsWithoutBattle() {
        for (Unit u : units) {
            UnitStatus status = unitsStatus.get(u.getId());
            status.assignFoundEntity(u.getEntity(), false);
            u.getEntity().setDeployed(true);
            Crew crew = u.getEntity().getCrew();
            if ((null != crew) && !crew.getExternalIdAsString().equals("-1")) {
                pilots.put(UUID.fromString(crew.getExternalIdAsString()), crew);
            }

            entities.put(UUID.fromString(u.getEntity().getExternalIdAsString()), u.getEntity());
        }

        // if it's an AtB scenario, load up all the bot units into the entities collection
        // for objective processing
        if (scenario instanceof AtBScenario) {
            AtBScenario atbScenario = (AtBScenario) scenario;
            for (Entity e : atbScenario.getAlliesPlayer()) {
                entities.put(UUID.fromString(e.getExternalIdAsString()), e);
            }

            for (int x = 0; x < atbScenario.getNumBots(); x++) {
                BotForce botForce = atbScenario.getBotForce(x);

                for (Entity e : botForce.getFullEntityList(campaign)) {
                    entities.put(UUID.fromString(e.getExternalIdAsString()), e);
                }
            }
        }
    }

    public List<TestUnit> getAlliedUnits() {
        return alliedUnits;
    }

    public List<TestUnit> getPotentialSalvage() {
        return potentialSalvage;
    }

    public List<TestUnit> getActualSalvage() {
        return actualSalvage;
    }

    public List<TestUnit> getSoldSalvage() {
        return ransomedSalvage;
    }

    public List<TestUnit> getLeftoverSalvage() {
        return leftoverSalvage;
    }

    public void salvageUnit(int i) {
        if (i < getPotentialSalvage().size()) {
            TestUnit salvageUnit = getPotentialSalvage().get(i);
            getActualSalvage().add(salvageUnit);
        }
    }

    public void sellUnit(int i) {
        if (i < getPotentialSalvage().size()) {
            TestUnit ransomUnit = getPotentialSalvage().get(i);
            getSoldSalvage().add(ransomUnit);
        }
    }

    public void doNotSalvageUnit(int i) {
        if (i < getPotentialSalvage().size()) {
            getLeftoverSalvage().add(getPotentialSalvage().get(i));
        }
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

    public int getMissionId() {
        return campaign.getMission(scenario.getMissionId()).getId();
    }

    public int getScenarioId() {
        return scenario.getId();
    }

    public Hashtable<String, String> getKillCredits() {
        return killCredits;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void resolveScenario(ScenarioStatus resolution, String report) {
        //lets start by generating a stub file for our records
        scenario.generateStub(campaign);

        // and create trackers for ransomed prisoners and units
        Money prisonerRansoms = Money.zero();
        Money unitRansoms = Money.zero();

        //ok lets do the whole enchilada and go ahead and update campaign
        //first figure out if we need any battle loss comp
        double blc = 0;
        final Mission mission = getMission();

        final boolean isContract = mission instanceof Contract;
        final boolean isAtBContract = mission instanceof AtBContract;
        if (isContract) {
            blc = ((Contract) mission).getBattleLossComp() / 100.0;
        }

        //now lets update personnel
        for (UUID pid : peopleStatus.keySet()) {
            Person person = campaign.getPerson(pid);
            PersonStatus status = peopleStatus.get(pid);
            if (null == person || null == status) {
                continue;
            }

            MekHQ.triggerEvent(new PersonBattleFinishedEvent(person, status));
            if (status.getHits() > person.getHits()) {
                person.setHits(status.getHits());
            }

            if (status.wasDeployed()) {
                person.awardXP(campaign, status.getXP());
                ServiceLogger.participatedInScenarioDuringMission(person, campaign.getLocalDate(),
                        scenario.getName(), mission.getName());
            }
            for (Kill k : status.getKills()) {
                getCampaign().addKill(k);
            }
            if (status.isMissing()) {
                person.changeStatus(getCampaign(), getCampaign().getLocalDate(), PersonnelStatus.MIA);
            } else if (status.isDead()) {
                person.changeStatus(getCampaign(), getCampaign().getLocalDate(), PersonnelStatus.KIA);
                if (getCampaign().getCampaignOptions().isUseAtB() && isAtBContract) {
                    getCampaign().getRetirementDefectionTracker().removeFromCampaign(person,
                            true, getCampaign(), (AtBContract) mission);
                }
            }

            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                person.diagnose(getCampaign(), status.getHits());
            }

            if (status.toRemove()) {
                getCampaign().removePerson(person, false);
            }
        }

        //region Prisoners
        for (UUID pid : oppositionPersonnel.keySet()) {
            OppositionPersonnelStatus status = oppositionPersonnel.get(pid);
            Person person = status.getPerson();
            if (person == null) {
                continue;
            }
            MekHQ.triggerEvent(new PersonBattleFinishedEvent(person, status));
            if (status.isDead()) {
                continue;
            } else if (status.isRansomed()) {
                prisonerRansoms = prisonerRansoms.plus(person.getRansomValue(getCampaign()));
                continue;
            } else if (status.isCaptured()) {
                PrisonerStatus prisonerStatus = getCampaign().getCampaignOptions().getDefaultPrisonerStatus();

                // Then, we need to determine if they are a defector
                if (prisonerStatus.isCurrentPrisoner() && getCampaign().getCampaignOptions().isUseAtBPrisonerDefection()
                        && isAtBContract) {
                    // Are they actually a defector?
                    if (Compute.d6(2) >= (8 + ((AtBContract) mission).getEnemySkill().ordinal() - getCampaign().getUnitRatingAsInteger())) {
                        prisonerStatus = PrisonerStatus.PRISONER_DEFECTOR;
                    }
                }

                getCampaign().recruitPerson(person, prisonerStatus);
                if (prisonerStatus.isPrisonerDefector()) {
                    getCampaign().addReport(String.format("You have convinced %s to defect.",
                            person.getHyperlinkedName()));
                }
            } else {
                continue;
            }
            person.awardXP(campaign, status.getXP());
            if (status.getHits() > person.getHits()) {
                person.setHits(status.getHits());
            }

            ServiceLogger.participatedInScenarioDuringMission(person, campaign.getLocalDate(), scenario.getName(), mission.getName());

            for (Kill k : status.getKills()) {
                campaign.addKill(k);
            }

            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                person.diagnose(getCampaign(), status.getHits());
            }
        }

        if (prisonerRansoms.isGreaterThan(Money.zero())) {
            getCampaign().getFinances().credit(TransactionType.RANSOM, getCampaign().getLocalDate(),
                    prisonerRansoms, "Prisoner ransoms for " + getScenario().getName());
            getCampaign().addReport(prisonerRansoms.toAmountAndSymbolString()
                    + " has been credited to your account for prisoner ransoms following "
                    + getScenario().getName() + ".");
        }
        //endregion Prisoners

        // now lets update all units
        for (Unit unit : getUnits()) {
            UnitStatus ustatus = unitsStatus.get(unit.getId());
            if (null == ustatus) {
                // shouldn't happen
                continue;
            }
            Entity en = ustatus.getEntity();
            Money unitValue = unit.getBuyCost();
            if (campaign.getCampaignOptions().isBLCSaleValue()) {
                unitValue = unit.getSellValue();
            }

            if (ustatus.isTotalLoss()) {
                // missing unit
                if (blc > 0) {
                    Money value = unitValue.multipliedBy(blc);
                    campaign.getFinances().credit(TransactionType.BATTLE_LOSS_COMPENSATION,
                            getCampaign().getLocalDate(), value,
                            "Battle loss compensation for " + unit.getName());
                    campaign.addReport(value.toAmountAndSymbolString() + " in battle loss compensation for "
                            + unit.getName() + " has been credited to your account.");
                }
                campaign.removeUnit(unit.getId());
            } else {
                Money currentValue = unit.getValueOfAllMissingParts();
                campaign.clearGameData(en);
                // FIXME: Need to implement a "fuel" part just like the "armor" part
                if (en.isAero()) {
                    ((IAero) en).setFuelTonnage(((IAero) ustatus.getBaseEntity()).getFuelTonnage());
                }
                unit.setEntity(en);
                if (en.usesWeaponBays()) {
                    new AdjustLargeCraftAmmoAction().execute(campaign, unit);
                }
                unit.runDiagnostic(true);
                unit.resetPilotAndEntity();
                if (!unit.isRepairable()) {
                    unit.setSalvage(true);
                }
                campaign.addReport(unit.getHyperlinkedName() + " has been recovered.");
                // check for BLC
                Money newValue = unit.getValueOfAllMissingParts();
                Money blcValue = newValue.minus(currentValue);
                Money repairBLC = Money.zero();
                String blcString = "battle loss compensation (parts) for " + unit.getName();
                if (!unit.isRepairable()) {
                    // if the unit is not repairable, you should get BLC for it but we should subtract
                    // the value of salvageable parts
                    blcValue = unitValue.minus(unit.getSellValue());
                    blcString = "battle loss compensation for " + unit.getName();
                }
                if (campaign.getCampaignOptions().isPayForRepairs()) {
                    for (Part p : unit.getParts()) {
                        if (p.needsFixing() && !(p instanceof Armor)) {
                            repairBLC = repairBLC.plus(p.getActualValue().multipliedBy(0.2));
                        }
                    }
                }
                blcValue = blcValue.plus(repairBLC);
                if ((blc > 0) && blcValue.isPositive()) {
                    Money finalValue = blcValue.multipliedBy(blc);
                    getCampaign().getFinances().credit(TransactionType.BATTLE_LOSS_COMPENSATION,
                            getCampaign().getLocalDate(), finalValue,
                            blcString.substring(0, 1).toUpperCase() + blcString.substring(1));
                    campaign.addReport( finalValue.toAmountAndSymbolString() + " in " + blcString + " has been credited to your account.");
                }
            }
        }

        // now lets take care of salvage
        for (TestUnit salvageUnit : getActualSalvage()) {
            UnitStatus salstatus = new UnitStatus(salvageUnit);
            // FIXME: Need to implement a "fuel" part just like the "armor" part
            if (salvageUnit.getEntity() instanceof Aero) {
                ((Aero) salvageUnit.getEntity()).setFuelTonnage(((Aero) salstatus.getBaseEntity()).getFuelTonnage());
            }
            campaign.clearGameData(salvageUnit.getEntity());
            campaign.addTestUnit(salvageUnit);
            // if this is a contract, add to the salvaged value
            if (isContract) {
                ((Contract) mission).addSalvageByUnit(salvageUnit.getSellValue());
            }
        }

        // And any ransomed salvaged units
        if (!getSoldSalvage().isEmpty()) {
            for (Unit ransomedUnit : getSoldSalvage()) {
                unitRansoms = unitRansoms.plus(ransomedUnit.getSellValue());
            }

            if (unitRansoms.isGreaterThan(Money.zero())) {
                getCampaign().getFinances().credit(TransactionType.SALVAGE, getCampaign().getLocalDate(),
                        unitRansoms, "Unit sales for " + getScenario().getName());
                getCampaign().addReport(unitRansoms.toAmountAndSymbolString()
                        + " has been credited to your account from unit salvage sold following "
                        + getScenario().getName() + ".");
                if (isContract) {
                    ((Contract) mission).addSalvageByUnit(unitRansoms);
                }
            }
        }

        if (isContract) {
            Money value = Money.zero();
            for (Unit salvageUnit : getLeftoverSalvage()) {
                value = value.plus(salvageUnit.getSellValue());
            }
            if (((Contract) mission).isSalvageExchange()) {
                value = value.multipliedBy(((Contract) mission).getSalvagePct()).dividedBy(100);
                campaign.getFinances().credit(TransactionType.SALVAGE_EXCHANGE, getCampaign().getLocalDate(),
                        value, "Salvage exchange for " + scenario.getName());
                campaign.addReport(value.toAmountAndSymbolString() + " have been credited to your account for salvage exchange.");
            } else {
                ((Contract) mission).addSalvageByEmployer(value);
            }
        }

        if (campaign.getCampaignOptions().isUseAtB() && isAtBContract) {
            final int unitRatingMod = campaign.getUnitRatingMod();
            for (Unit unit : getUnits()) {
                unit.setSite(((AtBContract) mission).getRepairLocation(unitRatingMod));
            }
            for (Unit unit : getActualSalvage()) {
                unit.setSite(((AtBContract) mission).getRepairLocation(unitRatingMod));
            }
        }

        for (Loot loot : actualLoot) {
            loot.get(campaign, scenario);
        }

        scenario.setStatus(resolution);
        scenario.setReport(report);
        scenario.clearAllForcesAndPersonnel(campaign);
        // lets reset the network ids from the c3UUIDs
        campaign.reloadGameEntities();
        campaign.refreshNetworks();
        scenario.setDate(campaign.getLocalDate());
        client = null;
    }

    public ArrayList<Person> getMissingPersonnel() {
        ArrayList<Person> mia = new ArrayList<>();
        for (UUID pid : peopleStatus.keySet()) {
            PersonStatus status = peopleStatus.get(pid);
            if (status.isMissing()) {
                Person p = campaign.getPerson(pid);
                if (null != p) {
                    mia.add(p);
                }
            }
        }
        return mia;
    }

    public ArrayList<Person> getDeadPersonnel() {
        ArrayList<Person> kia = new ArrayList<>();
        for (UUID pid : peopleStatus.keySet()) {
            PersonStatus status = peopleStatus.get(pid);
            if (status.isDead()) {
                Person p = campaign.getPerson(pid);
                if (null != p) {
                    kia.add(p);
                }
            }
        }
        return kia;
    }

    public ArrayList<Person> getRecoveredPersonnel() {
        ArrayList<Person> recovered = new ArrayList<>();
        for (UUID pid : peopleStatus.keySet()) {
            PersonStatus status = peopleStatus.get(pid);
            if (!status.isDead() && !status.isMissing()) {
                Person p = campaign.getPerson(pid);
                if (null != p) {
                    recovered.add(p);
                }
            }
        }
        return recovered;
    }

    public Hashtable<UUID, PersonStatus> getPeopleStatus() {
        return peopleStatus;
    }

    public Hashtable<UUID, OppositionPersonnelStatus> getOppositionPersonnel() {
        return oppositionPersonnel;
    }

    public Hashtable<UUID, UnitStatus> getUnitsStatus() {
        return unitsStatus;
    }

    public Hashtable<UUID, UnitStatus> getSalvageStatus() {
        return salvageStatus;
    }

    public Map<UUID, Entity> getAllInvolvedUnits() {
        return entities;
    }

    public List<Loot> getPotentialLoot() {
        return potentialLoot;
    }

    public void addLoot(Loot loot) {
        actualLoot.add(loot);
    }

    public ArrayList<PersonStatus> getSortedPeople() {
        //put all the PersonStatuses in an ArrayList and sort by the unit name
        ArrayList<PersonStatus> toReturn = new ArrayList<>();
        for (UUID id : getPeopleStatus().keySet()) {
            PersonStatus status = peopleStatus.get(id);
            if (null != status) {
                toReturn.add(status);
            }
        }
        //now sort
        Collections.sort(toReturn);
        return toReturn;
    }

    public ArrayList<OppositionPersonnelStatus> getSortedPrisoners() {
        //put all the PersonStatuses in an ArrayList and sort by the unit name
        ArrayList<OppositionPersonnelStatus> toReturn = new ArrayList<>();
        for (UUID id : getOppositionPersonnel().keySet()) {
            OppositionPersonnelStatus status = oppositionPersonnel.get(id);
            if (null != status) {
                toReturn.add(status);
            }
        }
        //now sort
        Collections.sort(toReturn);
        return toReturn;
    }

    public boolean usesSalvageExchange() {
        return (getMission() instanceof Contract) && ((Contract) getMission()).isSalvageExchange();
    }

    /**
     * This object is used to track the status of a particular personnel. At the present,
     * we track the person's missing status, hits, and XP
     * @author Jay Lawson
     *
     */
    public static class PersonStatus implements Comparable<PersonStatus> {
        private String name;
        private String unitName;
        private int hits;
        private boolean missing;
        private int xp;
        private ArrayList<Kill> kills;
        private boolean remove;
        private boolean pickedUp;
        private UUID personId;
        private boolean deployed;
        private boolean dead;

        public PersonStatus(String n, String u, int h, UUID id) {
            name = n;
            unitName = u;
            hits = h;
            missing = false;
            xp = 0;
            kills = new ArrayList<>();
            remove = false;
            pickedUp = false;
            personId = id;
            deployed = true;
            dead = false;
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
            if (hits >= 6) {
                setDead(true);
            } else {
                setDead(false);
            }
        }

        public boolean isDead() {
            return dead || (hits >= 6);
        }

        public void setDead(boolean dead) {
            this.dead = dead;
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
            if (isDead()) {
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

        public void setDeployed(boolean b) {
            deployed = b;
        }

        public boolean wasDeployed() {
            return deployed;
        }

        @Override
        public String toString() {
            return unitName;
        }

        @Override
        public int compareTo(PersonStatus ostatus) {
            return unitName.compareTo(ostatus.getUnitName());
       }

    }

    /**
     * This object is used to track the status of a opposition personnel. We need to actually put the whole
     * person object here because we are not already tracking it on the campaign
     * @author Jay Lawson
     *
     */
    public static class OppositionPersonnelStatus extends PersonStatus {
        //for prisoners we have to track a whole person
        private Person person;
        private boolean captured;
        private boolean ransomed = false;

        public OppositionPersonnelStatus(String n, String u, Person p) {
            super(n, u, 0, p.getId());
            person = p;
        }

        public Person getPerson() {
            return person;
        }

        public boolean isCaptured() {
            return captured;
        }

        public void setCaptured(boolean set) {
            captured = set;
        }

        public boolean isRansomed() {
            return ransomed;
        }

        public void setRansomed(boolean ransomed) {
            this.ransomed = ransomed;
        }
    }

    /**
     * This object is used to track the status of a particular unit.
     * @author Jay Lawson
     *
     */
    public static class UnitStatus {
        private String name;
        private String chassis;
        private String model;
        private boolean totalLoss;
        private Entity entity;
        private Entity baseEntity;
        private Unit unit;

        public UnitStatus(Unit unit) {
            this.unit = unit;
            this.name = unit.getName();
            chassis = unit.getEntity().getFullChassis();
            model = unit.getEntity().getModel();
            //assume its a total loss until we find something that says otherwise
            totalLoss = true;
            //create a brand new entity until we find one
            MechSummary summary = MechSummaryCache.getInstance().getMech(getLookupName());
            if (null != summary) {
                try {
                    entity = unit.getEntity() == null
                            ? new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity()
                            : unit.getEntity();
                    baseEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                } catch (EntityLoadingException e) {
                    LogManager.getLogger().error("", e);
                }
            }
        }

        @Override
        public String toString() {
            return "Unit status for: " + getName() + ", loss: " + isTotalLoss();
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

        public Unit getUnit() {
            return unit;
        }

        public boolean isTotalLoss() {
            return totalLoss;
        }

        public void setTotalLoss(boolean b) {
            totalLoss = b;
        }

        public String getDesc() {
            return getDesc(false);
        }

        public String getDesc(boolean printSellValue) {
            if (null == entity) {
                return "Whoops, No Entity";
            }
            String color = "black";
            String status = Unit.getDamageStateName(entity.getDamageLevel(false));
            if (!Unit.isRepairable(entity)) {
                color = "rgb(190, 150, 55)";
                status = "Salvage";
            } else if (!Unit.isFunctional(entity)) {
                color = "rgb(205, 92, 92)";
                status = "Inoperable";
            } else {
                switch (entity.getDamageLevel(false)) {
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

            if (printSellValue) {
                return "<html><b>" + getName() + "</b><br> (" + unit.getSellValue().toAmountAndSymbolString() + ") <font color='" + color + "'>"+ status + "</font></html>";
            } else {
                return "<html><b>" + getName() + "</b><br><font color='" + color + "'>"+ status + "</font></html>";
            }
        }

        public boolean isLikelyCaptured() {
            if (null == entity) {
                return false;
            }
            return Utilities.isLikelyCapture(entity);
        }
    }

    public void setEvent(GameVictoryEvent gve) {
        victoryEvent = gve;
    }

    public boolean playerHasBattlefieldControl() {
        return control;
    }
}
