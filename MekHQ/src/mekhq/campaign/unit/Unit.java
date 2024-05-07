/*
 * Unit.java
 *
 * Copyright (C) 2016-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import megamek.Version;
import megamek.client.ui.swing.tileset.EntityImage;
import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.icons.Camouflage;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonCrewAssignmentEvent;
import mekhq.campaign.event.PersonTechAssignmentEvent;
import mekhq.campaign.event.UnitArrivedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.enums.CrewAssignmentState;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.io.migration.CamouflageMigrator;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.UIManager;

/**
 * This is a wrapper class for entity, so that we can add some functionality to it
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Unit implements ITechnology {
    public static final int SITE_FIELD = 0;
    public static final int SITE_MOBILE_BASE = 1;
    public static final int SITE_BAY = 2;
    public static final int SITE_FACILITY = 3;
    public static final int SITE_FACTORY = 4;
    public static final int SITE_N = 5;

    // To be used for transport and cargo reports
    public static final int ETYPE_MOTHBALLED = -9876;

    public static final int TECH_WORK_DAY = 480;

    protected Entity entity;
    private int site;
    private boolean salvaged;
    private UUID id;
    private String fluffName;
    // This is the large craft assigned to transport this unit
    private TransportShipAssignment transportShipAssignment;
    // If this unit is a transport, list all other units assigned to it
    private Set<Unit> transportedUnits = new HashSet<>();
    private double aeroCapacity = 0.0;
    private double baCapacity = 0.0;
    private int dockCapacity = 0;
    private double hVeeCapacity = 0.0;
    private double infCapacity = 0.0;
    private double lVeeCapacity = 0.0;
    private double mechCapacity = 0.0;
    private double protoCapacity = 0.0;
    private double shVeeCapacity = 0.0;
    private double scCapacity = 0.0;

    //assignments
    private int forceId;
    protected int scenarioId;

    private List<Person> drivers;
    private List<Person> gunners;
    private List<Person> vesselCrew;
    // Contains unique Id of each Infantry/BA Entity assigned to this unit as marines
    // Used to calculate marine points (which are based on equipment) as well as Personnel IDs
    // TODO: private Set<Person> marines;
    //this is the id of the tech officer in a superheavy tripod
    private Person techOfficer;
    private Person navigator;
    //this is the id of the tech assigned for maintenance if any
    private Person tech;

    //mothballing variables - if mothball time is not zero then mothballing/activating is in progress
    private int mothballTime;
    private boolean mothballed;

    private int daysSinceMaintenance;
    private int daysActivelyMaintained;
    private int astechDaysMaintained;
    private int maintenanceMultiplier;

    private Campaign campaign;

    private ArrayList<Part> parts;
    private String lastMaintenanceReport;
    private ArrayList<PodSpace> podSpace;

    private Refit refit;

    //a made-up person to handle repairs on Large Craft
    private Person engineer;

    private String history;

    //for delivery
    protected int daysToArrival;

    private MothballInfo mothballInfo;

    public Unit() {
        this(null, null);
    }

    public Unit(Entity en, Campaign c) {
        this.entity = en;
        if (entity != null) {
            entity.setCamouflage(new Camouflage());
        }
        this.site = SITE_BAY;
        this.campaign = c;
        this.parts = new ArrayList<>();
        this.podSpace = new ArrayList<>();
        this.drivers = new ArrayList<>();
        this.gunners = new ArrayList<>();
        this.vesselCrew = new ArrayList<>();
        forceId = Force.FORCE_NONE;
        scenarioId = Scenario.S_DEFAULT_ID;
        this.history = "";
        this.lastMaintenanceReport = "";
        this.fluffName = "";
        this.maintenanceMultiplier = 1;
        reCalc();
    }

    public static String getDamageStateName(int i) {
        switch (i) {
            case Entity.DMG_NONE:
                return "Undamaged";
            case Entity.DMG_LIGHT:
                return "Light Damage";
            case Entity.DMG_MODERATE:
                return "Moderate Damage";
            case Entity.DMG_HEAVY:
                return "Heavy Damage";
            case Entity.DMG_CRIPPLED:
                return "Crippled";
            default:
                return "Unknown";
        }
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign c) {
        campaign = c;
    }

    /**
     * A convenience function to tell whether the unit can be acted upon
     * e.g. assigned pilots, techs, repaired, etc.
     * @return
     */
    public boolean isAvailable() {
        return isAvailable(false);
    }

    /**
     * A convenience function to tell whether the unit can be acted upon
     * e.g. assigned pilots, techs, repaired, etc.
     * @return
     */
    public boolean isAvailable(boolean ignoreRefit) {
        return isPresent() && !isDeployed() && (ignoreRefit || !isRefitting()) && !isMothballing() && !isMothballed();
    }

    public String getStatus() {
        if (isMothballing()) {
            if (isMothballed()) {
                return "Activating (" + getMothballTime() + "m)";
            } else {
                return "Mothballing (" + getMothballTime() + "m)";
            }
        } else if (isMothballed()) {
            return "Mothballed";
        } else if (isDeployed()) {
            return "Deployed";
        } else if (!isPresent()) {
            return "In transit (" + getDaysToArrival() + " days)";
        } else if (isRefitting()) {
            return "Refitting";
        } else {
            return getCondition();
        }
    }

    public String getCondition() {
        if (!isRepairable()) {
            return "Salvage";
        } else if (!isFunctional()) {
            return "Inoperable";
        } else {
            return getDamageStateName(getDamageState());
        }
    }

    public CrewAssignmentState getCrewState() {
        final boolean uncrewed = getCrew().isEmpty();
        if (getTech() != null) {
            if (uncrewed) {
                return CrewAssignmentState.UNCREWED;
            } else if (canTakeMoreDrivers() || canTakeMoreVesselCrew() || canTakeTechOfficer()
                    || canTakeMoreGunners() || canTakeNavigator()) {
                return CrewAssignmentState.PARTIALLY_CREWED;
            } else {
                return CrewAssignmentState.FULLY_CREWED;
            }
        } else {
            return uncrewed ? CrewAssignmentState.UNSUPPORTED : CrewAssignmentState.UNMAINTAINED;
        }
    }

    public void reCalc() {
        // Do Nothing
    }

    public void initializeBaySpace() {
        // Initialize the bay capacity
        this.aeroCapacity = getASFCapacity();
        this.baCapacity = getBattleArmorCapacity();
        this.dockCapacity = getDocks();
        this.hVeeCapacity = getHeavyVehicleCapacity();
        this.infCapacity = getInfantryCapacity();
        this.lVeeCapacity = getLightVehicleCapacity();
        this.mechCapacity = getMechCapacity();
        this.protoCapacity = getProtomechCapacity();
        this.shVeeCapacity = getSuperHeavyVehicleCapacity();
        this.scCapacity = getSmallCraftCapacity();
    }

    public void setEntity(Entity en) {
        // if there is already an entity, then make sure this
        // one gets some of the same things set
        if (null != this.entity) {
            en.setId(this.entity.getId());
            en.setDuplicateMarker(this.entity.getDuplicateMarker());
            en.generateShortName();
            en.generateDisplayName();
        }
        this.entity = en;
    }

    public Entity getEntity() {
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID i) {
        this.id = i;
    }

    // A set of methods for working with transport ship assignment for this unit

    /**
     * Gets a value indicating whether or not this unit is assigned
     * to a transport ship.
     */
    public boolean hasTransportShipAssignment() {
        return (transportShipAssignment != null);
    }

    /**
     * Gets the transport ship assignment for this unit,
     * or null if this unit is not being transported.
     */
    public @Nullable TransportShipAssignment getTransportShipAssignment() {
        return transportShipAssignment;
    }

    /**
     * Sets the transport ship assignment for this unit.
     * @param assignment The transport ship assignment, or null if this unit
     *                   is not being transported.
     */
    public void setTransportShipAssignment(@Nullable TransportShipAssignment assignment) {
        transportShipAssignment = assignment;
    }

    /**
     * Gets a value indicating whether or not this unit is
     * transporting units.
     */
    public boolean hasTransportedUnits() {
        return !transportedUnits.isEmpty();
    }

    /**
     * @return the set of units being transported by this unit.
     */
    public Set<Unit> getTransportedUnits() {
        return Collections.unmodifiableSet(transportedUnits);
    }

    /**
     * Adds a unit to our set of transported units.
     * @param unit The unit being transported by this instance.
     */
    public void addTransportedUnit(Unit unit) {
        transportedUnits.add(Objects.requireNonNull(unit));
    }

    /**
     * Adds a unit to a specific bay on our unit.
     * @param unit The unit being transported by this instance.
     * @param bayNumber The bay which will contain the unit.
     */
    public void addTransportedUnit(Unit unit, int bayNumber) {
        Objects.requireNonNull(unit);

        unit.setTransportShipAssignment(new TransportShipAssignment(this, bayNumber));
        addTransportedUnit(unit);
    }

    /**
     * Removes a unit from our set of transported units.
     * @param unit The unit to remove from our set of transported units.
     * @return True if the unit was removed from our bays, otherwise false.
     */
    public boolean removeTransportedUnit(Unit unit) {
        return transportedUnits.remove(unit);
    }

    /**
     * Clears the set of units being transported by this unit.
     */
    public void clearTransportedUnits() {
        transportedUnits.clear();
    }

    /**
     * Gets a value indicating whether or not we are transporting any smaller aero units
     */
    public boolean isCarryingSmallerAero() {
        return transportedUnits.stream().anyMatch(u -> u.getEntity().isAero()
                && !u.getEntity().isLargeCraft()
                && (u.getEntity().getUnitType() != UnitType.SMALL_CRAFT));
    }

    /**
     * Gets a value indicating whether or not we are transporting any ground units.
     */
    public boolean isCarryingGround() {
        return transportedUnits.stream().anyMatch(u -> !u.getEntity().isAero());
    }

    public int getSite() {
        return site;
    }

    public void setSite(int i) {
        this.site = i;
    }

    public boolean isSalvage() {
        return salvaged;
    }

    public void setSalvage(boolean b) {
        this.salvaged = b;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String s) {
        this.history = s;
    }

    public static boolean isFunctional(Entity en) {
        if (en instanceof Mech) {
            // center torso bad?? head bad?
            if (en.isLocationBad(Mech.LOC_CT)
                    || en.isLocationBad(Mech.LOC_HEAD)) {
                return false;
            }
            // engine destruction?
            //cockpit hits
            int engineHits = 0;
            int cockpitHits = 0;
            for (int i = 0; i < en.locations(); i++) {
                engineHits += en.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, i);
                cockpitHits += en.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_COCKPIT, i);
            }
            if (engineHits > 2) {
                return false;
            }
            if (cockpitHits > 0) {
                return false;
            }
        }
        if (en instanceof Tank) {
            for (int i = 0; i < en.locations(); i++) {
                if (i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2) {
                    continue;
                }
                if (en.isLocationBad(i)) {
                    return false;
                }
            }
            if (en instanceof VTOL) {
                if (en.getWalkMP() <= 0) {
                    return false;
                }
            }
        }
        if (en instanceof Aero) {
            // aerospace units are considered non-functional if their walk MP is 0
            // unless they are grounded spheroid dropships or jumpships
            boolean hasNoWalkMP = en.getWalkMP() <= 0;
            boolean isJumpship = en instanceof Jumpship;
            boolean isGroundedSpheroid = (en instanceof Dropship) && ((Dropship) en).isSpheroid() && en.getAltitude() == 0;
            if (hasNoWalkMP && !isJumpship && !isGroundedSpheroid) {
                return false;
            }
            return ((Aero) en).getSI() > 0;
        }
        return true;
    }

    public boolean isFunctional() {
        return isFunctional(entity);
    }

    public static boolean isRepairable(Entity en) {
        if (en instanceof Mech) {
            // you can repair anything so long as one point of CT is left
            if (en.getInternal(Mech.LOC_CT) <= 0) {
                return false;
            }
        }
        if (en instanceof Tank) {
            // can't repair a tank with a destroyed location
            for (int i = 0; i < en.locations(); i++) {
                if (i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2 || i == Tank.LOC_BODY) {
                    continue;
                }
                if (en.getInternal(i) <= 0) {
                    return false;
                }
            }
        }
        if (en instanceof Aero) {
            return ((Aero) en).getSI() > 0;
        }
        return true;
    }

    public boolean isRepairable() {
        return isRepairable(entity);
    }

    /**
     * Determines if this unit can be serviced.
     *
     * @return <code>true</code> if the unit has parts that are salvageable or in
     *         need of repair.
     */
    public boolean isServiceable() {
        if (isSalvage() || !isRepairable()) {
            return hasSalvageableParts();
        } else {
            return hasPartsNeedingFixing();
        }
    }

    /**
     * Is the given location on the entity destroyed?
     *
     * @param loc
     *            - an <code>int</code> for the location
     * @return <code>true</code> if the location is destroyed
     */
    public boolean isLocationDestroyed(int loc) {
        if (loc > entity.locations() || loc < 0) {
            return false;
        }
        /*boolean blownOff = entity.isLocationBlownOff(loc);
        entity.setLocationBlownOff(loc, false);
        boolean isDestroyed = entity.isLocationBad(loc);
        entity.setLocationBlownOff(loc, blownOff);
        return isDestroyed;
        */
        return entity.isLocationTrulyDestroyed(loc);
    }

    public boolean isLocationBreached(int loc) {
        return entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED;
    }

    public boolean hasBadHipOrShoulder(int loc) {
        return entity instanceof Mech
                && (entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0
                        || entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc) > 0);
    }

    /**
     * Run a diagnostic on this unit
     */
    public void runDiagnostic(boolean checkForDestruction) {
        // need to set up an array of part ids to avoid concurrent modification
        // problems because some updateCondition methods will remove the part and put
        // in a new one
        List<Part> tempParts = new ArrayList<>();
        tempParts.addAll(parts);

        for (Part part : tempParts) {
            part.updateConditionFromEntity(checkForDestruction);
        }
    }

    private boolean isPartAvailableForRepairs(IPartWork partWork, boolean onlyNotBeingWorkedOn) {
        return (!onlyNotBeingWorkedOn || (onlyNotBeingWorkedOn && !partWork.isBeingWorkedOn()));
    }

    /**
     * Gets a list of every part on a unit which need service (either repair or salvage),
     * including parts currently being worked on.
     */
    public List<IPartWork> getPartsNeedingService() {
        return getPartsNeedingService(false);
    }

    /**
     * Gets a list of parts on a unit which need service (either repair or salvage),
     * optionally excluding parts already being worked on.
     * @param onlyNotBeingWorkedOn When true, excludes parts currently being repaired or salvaged.
     */
    public List<IPartWork> getPartsNeedingService(boolean onlyNotBeingWorkedOn) {
        if (isSalvage() || !isRepairable()) {
            return getSalvageableParts(onlyNotBeingWorkedOn);
        } else {
            return getPartsNeedingFixing(onlyNotBeingWorkedOn);
        }
    }

    public ArrayList<IPartWork> getPartsNeedingFixing() {
        return getPartsNeedingFixing(false);
    }

    /**
     * Determines if this unit has parts in need of repair.
     *
     * @return <code>true</code> if the unit has parts that are in need of repair.
     */
    public boolean hasPartsNeedingFixing() {
        boolean onlyNotBeingWorkedOn = false;
        for (Part part : parts) {
            if (part.needsFixing() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
                return true;
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.needsFixing() && isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<IPartWork> getPartsNeedingFixing(boolean onlyNotBeingWorkedOn) {
        ArrayList<IPartWork> brokenParts = new ArrayList<>();
        for (Part part: parts) {
            if (part.needsFixing() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
                brokenParts.add(part);
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.needsFixing() && isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn)) {
                brokenParts.add(pod);
            }
        }
        return brokenParts;
    }

    public ArrayList<IPartWork> getSalvageableParts() {
        return getSalvageableParts(false);
    }

    /**
     * Determines if this unit has parts that are salvageable.
     *
     * @return <code>true</code> if the unit has parts that are salvageable.
     */
    public boolean hasSalvageableParts() {
        boolean onlyNotBeingWorkedOn = false;
        for (Part part : parts) {
            if (part.isSalvaging() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
                return true;
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.hasSalvageableParts() && isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<IPartWork> getSalvageableParts(boolean onlyNotBeingWorkedOn) {
        ArrayList<IPartWork> salvageParts = new ArrayList<>();
        for (Part part: parts) {
            if (part.isSalvaging() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
                salvageParts.add(part);
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.hasSalvageableParts() && isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn)) {
                salvageParts.add(pod);
            }
        }
        return salvageParts;
    }

    public ArrayList<IAcquisitionWork> getPartsNeeded() {
        ArrayList<IAcquisitionWork> missingParts = new ArrayList<>();
        if (isSalvage() || !isRepairable()) {
            return missingParts;
        }
        boolean armorFound = false;
        for (Part part : parts) {
            if (part instanceof MissingPart && part.needsFixing() && null == ((MissingPart) part).findReplacement(false)) {
                missingParts.add((MissingPart) part);
            }
            //we need to check for armor as well, but this one is funny because we dont want to
            //check per location really, since armor can be used anywhere. So stop after we reach
            //the first Armor needing replacement
            //TODO: we need to adjust for patchwork armor, which can have different armor types by location
            if (!armorFound && part instanceof Armor) {
                Armor a = (Armor) part;
                if (a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if (!armorFound && part instanceof ProtomekArmor) {
                ProtomekArmor a = (ProtomekArmor) part;
                if (a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if (!armorFound && part instanceof BaArmor) {
                BaArmor a = (BaArmor) part;
                if (a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if (part instanceof AmmoBin && !((AmmoBin) part).isEnoughSpareAmmoAvailable()) {
                missingParts.add((AmmoBin) part);
            }
        }

        return missingParts;
    }

    public Money getValueOfAllMissingParts() {
        Money value = Money.zero();
        for (Part part : parts) {
            if (part instanceof MissingAmmoBin) {
                AmmoBin newBin = (AmmoBin) ((MissingAmmoBin) part).getNewEquipment();
                value = value.plus(newBin.getValueNeeded());
            }
            if (part instanceof MissingPart) {
                Part newPart = (Part) ((MissingPart) part).getNewEquipment();
                newPart.setBrandNew(!getCampaign().getCampaignOptions().isBLCSaleValue());
                value = value.plus(newPart.getActualValue());
            }
            else if (part instanceof AmmoBin) {
                value = value.plus(((AmmoBin) part).getValueNeeded());
            }
            else if (part instanceof Armor) {
                value = value.plus(((Armor) part).getValueNeeded());
            }
        }
        return value;
    }

    public void removePart(Part part) {
        parts.remove(part);
    }

    /**
     * @param m
     *            - A Mounted class to find crits for
     * @return the number of crits existing for this Mounted
     */
    public int getCrits(Mounted m) {
        // TODO: I should probably just add this method to Entity in MM
        // For the above, Mounted would probably be even better than Entity
        int hits = 0;
        for (int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = entity.getCritical(loc, i);
                // ignore empty & system slots
                if ((slot == null)
                        || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                Mounted m1 = slot.getMount();
                Mounted m2 = slot.getMount2();
                if (slot.getIndex() == -1) {
                    if ((m.equals(m1) || m.equals(m2))
                            && (slot.isHit() || slot.isDestroyed())) {
                        hits++;
                    }
                } else {
                    if (entity.getEquipmentNum(m) == slot.getIndex()
                            && (slot.isHit() || slot.isDestroyed())) {
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    public boolean hasPilot() {
        return null != entity.getCrew();
    }

    public String getPilotDesc() {
        if (hasPilot()) {
            return entity.getCrew().getName() + " "
                    + entity.getCrew().getGunnery() + "/"
                    + entity.getCrew().getPiloting();
        }
        return "NO PILOT";
    }

    /**
     * produce a string in HTML that can be embedded in larger reports
     */
    public String getDescHTML() {
        String toReturn = "<b>" + getName() + "</b><br/>";
        toReturn += getPilotDesc() + "<br/>";
        if (isDeployed()) {
            toReturn += "DEPLOYED!<br/>";
        } else {
            toReturn += "Site: " + getCurrentSiteName() + "<br/>";
        }
        return toReturn;
    }

    public TargetRoll getSiteMod() {
        switch (site) {
            case SITE_FIELD:
                return new TargetRoll(2, "in the field");
            case SITE_MOBILE_BASE:
                return new TargetRoll(1, "field workshop");
            case SITE_BAY:
                return new TargetRoll(0, "transport bay");
            case SITE_FACILITY:
                return new TargetRoll(-2, "maintenance facility");
            case SITE_FACTORY:
                return new TargetRoll(-4, "factory");
            default:
                return new TargetRoll(0, "unknown location");
        }
    }

    public static String getSiteName(int loc) {
        switch (loc) {
            case SITE_FIELD:
                return "In the Field";
            case SITE_MOBILE_BASE:
                return "Field Workshop";
            case SITE_BAY:
                return "Transport Bay";
            case SITE_FACILITY:
                return "Maintenance Facility";
            case SITE_FACTORY:
                return "Factory";
            default:
                return "Unknown";
        }
    }

    public String getCurrentSiteName() {
        return getSiteName(site);
    }

    public boolean isDeployed() {
        return scenarioId != -1;
    }

    public void undeploy() {
        scenarioId = -1;
    }

    // TODO: Add support for advanced medical
    public String checkDeployment() {
        if (!isFunctional()) {
            return "unit is not functional";
        }
        if (isUnmanned()) {
            return "unit has no pilot";
        }
        if (isRefitting()) {
            return "unit is being refit";
        }
        if (entity instanceof Tank
                && getActiveCrew().size() < getFullCrewSize()) {
            return "This vehicle requires a crew of " + getFullCrewSize();
        }
        //Taharqa: I am not going to allow BattleArmor units with unmanned suits to deploy. It is
        //possible to hack this to work in MM, but it becomes a serious problem when the unit becomes
        //a total loss because the unmanned suits are also treated as destroyed. I tried hacking something
        //together in ResolveScenarioTracker and decided that it was not right. If someone wants to deploy
        //a non-full strength BA unit, they can salvage the suits that are unmanned and then they can deploy
        //it
        if (entity instanceof BattleArmor) {
            for (int i = BattleArmor.LOC_TROOPER_1; i <= ((BattleArmor) entity).getTroopers(); i++) {
                if (entity.getInternal(i) == 0) {
                    return "This BattleArmor unit has empty suits. Fill them with pilots or salvage them.";
                }
            }
        }
        return null;
    }

    /**
     * Have to make one here because the one in MegaMek only returns true if
     * operable
     *
     * @return
     */
    public boolean hasTSM() {
        for (Mounted mEquip : entity.getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (null != mtype && mtype.hasFlag(MiscType.F_TSM)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns true if there is at least one missing critical slot for
     * this system in the given location
     */
    public boolean isSystemMissing(int system, int loc) {
        for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = entity.getCritical(loc, i);
            if ((ccs != null) && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                && (ccs.getIndex() == system) && ccs.isMissing()) {
                return true;
            }
        }
        return false;
    }

     /**
     * Number of slots doomed, missing or destroyed in all locations
     * @param type
     * @param index
     * @return
     */
    public int getHitCriticals(int type, int index) {
        int hits = 0;
        for (int loc = 0; loc < entity.locations(); loc++) {
            hits += getHitCriticals(type, index, loc);
        }
        return hits;
    }

    /**
     * Number of slots doomed, missing or destroyed in a location
     */
    public int getHitCriticals(int type, int index, int loc) {
        int hits = 0;
        Mounted m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = entity.getEquipment(index);
        }

        int numberOfCriticals = entity.getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = entity.getCritical(loc, i);

            //  Check to see if this crit mounts the supplied item
            //  For systems, we can compare the index, but for equipment we
            //  need to get the Mounted that is mounted in that index and
            //  compare types.  Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (ccs.isDestroyed()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        hits++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT)
                        && (null != m) && (m.equals(ccs.getMount()) || m.equals(ccs.getMount2()))) {
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    public void damageSystem(int type, int equipmentNum, int hits) {
        //make sure we take note of existing hits to start and as we cycle through locations
        int existingHits = getHitCriticals(type, equipmentNum);
        int neededHits = Math.max(0, hits - existingHits);
        int usedHits = 0;
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            if (neededHits > usedHits) {
                usedHits += damageSystem(type, equipmentNum, loc, neededHits - usedHits);
            }
        }
    }

    public int damageSystem(int type, int equipmentNum, int loc, int hits) {
        int nhits = 0;
        for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted mounted = getEntity().getEquipment(equipmentNum);
            Mounted m1 = cs.getMount();
            Mounted m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                if (nhits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    cs.setRepairable(true);
                    cs.setMissing(false);
                    nhits++;
                }
            }
        }
        return nhits;
    }

    public void destroySystem(int type, int equipmentNum) {
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            destroySystem(type, equipmentNum, loc);
        }
    }

    public void destroySystem(int type, int equipmentNum, int loc) {
        for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted mounted = getEntity().getEquipment(equipmentNum);
            Mounted m1 = cs.getMount();
            Mounted m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                cs.setHit(true);
                cs.setDestroyed(true);
                cs.setRepairable(false);
                cs.setMissing(false);
            }
        }
    }

    public void destroySystem(int type, int equipmentNum, int loc, int hits) {
        int nhits = 0;
        for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted mounted = getEntity().getEquipment(equipmentNum);
            Mounted m1 = cs.getMount();
            Mounted m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                if (nhits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    cs.setRepairable(false);
                    cs.setMissing(false);
                    nhits++;
                } else {
                    cs.setHit(false);
                    cs.setDestroyed(false);
                    cs.setRepairable(true);
                    cs.setMissing(false);
                }
            }
        }
    }

    public void repairSystem(int type, int equipmentNum) {
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            repairSystem(type, equipmentNum, loc);
        }
    }

    public void repairSystem(int type, int equipmentNum, int loc) {
        for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted mounted = getEntity().getEquipment(equipmentNum);
            Mounted m1 = cs.getMount();
            Mounted m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                cs.setHit(false);
                cs.setMissing(false);
                cs.setDestroyed(false);
                cs.setBreached(false);
                cs.setRepairable(true);
            }
        }
    }

    public boolean isDamaged() {
        return getDamageState() != Entity.DMG_NONE;
    }

    public String getHeatSinkTypeString(int year) {
        BigInteger heatSinkType = MiscType.F_HEAT_SINK;
        boolean heatSinkIsClanTechBase = false;

        for (Mounted mounted : getEntity().getEquipment()) {
            // Also goes through heat sinks inside the engine
            EquipmentType etype = mounted.getType();
            boolean isHeatSink = false;

            if (etype instanceof MiscType) {
                if (etype.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
                    heatSinkType = MiscType.F_LASER_HEAT_SINK;
                    isHeatSink = true;
                } else if (etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                    heatSinkType = MiscType.F_DOUBLE_HEAT_SINK;
                    isHeatSink = true;
                } else if (etype.hasFlag(MiscType.F_HEAT_SINK)) {
                    heatSinkType = MiscType.F_HEAT_SINK;
                    isHeatSink = true;
                }
            }

            if (isHeatSink) {
                if (TechConstants.getTechName(etype.getTechLevel(year)).equals("Inner Sphere")) {
                    heatSinkIsClanTechBase = false;
                } else if (TechConstants.getTechName(etype.getTechLevel(year)).equals("Clan")) {
                    heatSinkIsClanTechBase = true;
                }
                break;
            }
        }

        String heatSinkTypeString = heatSinkIsClanTechBase ? "(CL) " : "(IS) ";
        if (heatSinkType.equals(MiscType.F_LASER_HEAT_SINK)) {
            heatSinkTypeString += "Laser Heat Sink";
        } else if (heatSinkType.equals(MiscType.F_DOUBLE_HEAT_SINK)) {
            heatSinkTypeString += "Double Heat Sink";
        } else if (heatSinkType.equals(MiscType.F_HEAT_SINK)) {
            heatSinkTypeString += "Heat Sink";
        }

        return heatSinkTypeString;
    }

    public Money getSellValue() {
        Money partsValue = Money.zero();

        partsValue = partsValue
                .plus(parts.stream()
                    .map(x -> x.getActualValue().multipliedBy(x.getQuantity()))
                    .collect(Collectors.toList()));

        //We need to adjust this for equipment that doesn't show up as parts
        //Docking collars, Grav decks, KF Drive - Now parts
        //Drive unit - see SpacecraftEngine
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            if (entity instanceof SmallCraft) {
                //JS/SS/WS Bridge, Computer - see CombatInformationCenter
                //bridge
                partsValue = partsValue.plus(200000.0 + 10.0 * entity.getWeight());
                //computer
                partsValue = partsValue.plus(200000.0);
            }
            // Jump sail and KF drive support systems
            if ((entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                Jumpship js = (Jumpship) entity;
                Money driveCost = Money.zero();
                // sail
                driveCost = driveCost.plus(50000.0 * (30.0 + (js.getWeight() / 7500.0)));
                // lithium fusion and compact core?
                if (js.getDriveCoreType() == Jumpship.DRIVE_CORE_COMPACT && js.hasLF()) {
                    driveCost = driveCost.multipliedBy(15);
                } else if (js.getDriveCoreType() == Jumpship.DRIVE_CORE_COMPACT) {
                    // just a compact core?
                    driveCost = driveCost.multipliedBy(5);
                } else if (js.hasLF()) {
                    // lithium fusion?
                    driveCost = driveCost.multipliedBy(3);
                }
                // Drive Support Systems
                if (js instanceof Warship) {
                    driveCost = driveCost.plus(20000000.0 * (50.0 + js.getWeight() / 10000.0));
                } else {
                    driveCost = driveCost.plus(10000000.0 * (js.getWeight() / 10000.0));
                }
                partsValue = partsValue.plus(driveCost);

                // HPG
                if (js.hasHPG()) {
                    partsValue = partsValue.plus(1000000000.0);
                }

                // fuel tanks
                partsValue = partsValue.plus(200.0 * js.getFuel() / js.getFuelPerTon());

                // armor
                partsValue = partsValue.plus(js.getArmorWeight(js.locations()) * ArmorType.forEntity(js).getCost());

                // heat sinks
                Money sinkCost = Money.of(2000.0 + 4000.0 * js.getHeatType());// == HEAT_DOUBLE ? 6000 : 2000
                partsValue = partsValue.plus(sinkCost.multipliedBy(js.getOHeatSinks()));

                // get bays
                int baydoors = 0;
                Money bayCost = Money.zero();
                for (Bay next : js.getTransportBays()) {
                    baydoors += next.getDoors();
                    if ((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                        bayCost = bayCost.plus(20000.0 * next.getCapacity());
                    }
                    if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                        bayCost = bayCost.plus(20000.0 * next.getCapacity());
                    }
                }

                partsValue = partsValue.plus(bayCost.plus(baydoors * 1000.0));

                // life boats and escape pods
                partsValue = partsValue.plus((js.getLifeBoats() + js.getEscapePods()) * 5000.0);
            }
        }

        //protomeks: heat sinks can't be hit
        if (entity instanceof Protomech) {
            int sinks = 0;
            for (Mounted mount : entity.getWeaponList()) {
                if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                    WeaponType wType = (WeaponType) mount.getType();
                    sinks += wType.getHeat();
                }
            }
            partsValue = partsValue.plus(2000.0 * sinks);
        }

        // Scale the final value by the entity's price multiplier
        partsValue = partsValue.multipliedBy(entity.getPriceMultiplier());

        return partsValue;
    }

    public double getCargoCapacity() {
        double capacity = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof CargoBay) {
                capacity += bay.getCapacity();
            }
            if (bay instanceof PillionSeatCargoBay) {
                capacity += bay.getCapacity();
            }
            if (bay instanceof StandardSeatCargoBay) {
                capacity += bay.getCapacity();
            }
        }
        return capacity;
    }

    public double getRefrigeratedCargoCapacity() {
        double capacity = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof RefrigeratedCargoBay) {
                capacity += bay.getCapacity();
            }
        }
        return capacity;
    }

    public double getLiquidCargoCapacity() {
        double capacity = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof LiquidCargoBay) {
                capacity += bay.getCapacity();
            }
        }
        return capacity;
    }

    public double getLivestockCargoCapacity() {
        double capacity = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof LivestockCargoBay) {
                capacity += bay.getCapacity();
            }
        }
        return capacity;
    }

    public double getInsulatedCargoCapacity() {
        double capacity = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof InsulatedCargoBay) {
                capacity += bay.getCapacity();
            }
        }
        return capacity;
    }

    /**
     * Convenience method to call the right capacity getter based on unit type and weight
     * @param unitType integer obtained from a unit's entity that denotes its type (mech, tank, etc)
     * @param unitWeight double Weight in tons of the unit's entity. Important for tanks and infantry
     */
    public double getCorrectBayCapacity(int unitType, double unitWeight) {
        switch (unitType) {
            case UnitType.MEK:
                return getCurrentMechCapacity();
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
                // Return a small craft slot if no ASF slots exist
                if (getCurrentASFCapacity() > 0) {
                    return getCurrentASFCapacity();
                } else {
                    return getCurrentSmallCraftCapacity();
                }
            case UnitType.DROPSHIP:
                return getCurrentDocks();
            case UnitType.SMALL_CRAFT:
                return getCurrentSmallCraftCapacity();
            case UnitType.BATTLE_ARMOR:
                return getCurrentBattleArmorCapacity();
            case UnitType.INFANTRY:
                return getCurrentInfantryCapacity();
            case UnitType.TANK:
            case UnitType.NAVAL:
            case UnitType.VTOL:
                // Return the smallest available bay that can hold the unit
                if (unitWeight <= 50) {
                    if (getCurrentLightVehicleCapacity() > 0) {
                        return getCurrentLightVehicleCapacity();
                    } else if (getCurrentHeavyVehicleCapacity() > 0) {
                        return getCurrentHeavyVehicleCapacity();
                    } else {
                        return getCurrentSuperHeavyVehicleCapacity();
                    }
                } else if (unitWeight <= 100) {
                    if (getCurrentHeavyVehicleCapacity() > 0) {
                        return getCurrentHeavyVehicleCapacity();
                    } else {
                        return getCurrentSuperHeavyVehicleCapacity();
                    }
                } else {
                    return getCurrentSuperHeavyVehicleCapacity();
                }
            default:
                LogManager.getLogger().error("No transport bay defined for specified unit type.");
                return 0;
        }
    }

    /**
     * Convenience method to call the right capacity update based on unit type
     * When updating capacity, this method is concerned primarily with ensuring that space isn't released
     * beyond the unit's maximum. Checks are made to keep from going below 0 before we ever get here.
     * @param unitType integer obtained from a unit's entity that denotes its type (mech, tank, etc)
     * @param unitWeight double Weight in tons of the unit's entity. Important for infantry
     * @param addUnit boolean value that determines whether to add or subtract 1 from bay capacity
     * @param bayNumber integer representing the bay number that has been assigned to a cargo entity
     */
    public void updateBayCapacity(int unitType, double unitWeight, boolean addUnit, int bayNumber) {
        // Default. Consume 1 bay of the appropriate type
        int amount = -1;
        if (addUnit) {
            // Return 1 bay/cubicle to the transport's pool
            amount = 1;
        }
        switch (unitType) {
            // Be sure that when releasing bay space, the transport does not go over its normal maximum
            case UnitType.MEK:
                setMechCapacity(Math.min((getCurrentMechCapacity() + amount), getMechCapacity()));
                break;
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
                // Use the assigned bay number to determine if we need to update ASF or Small Craft capacity
                Bay aeroBay = getEntity().getBayById(bayNumber);
                if (aeroBay != null) {
                    if (BayType.getTypeForBay(aeroBay).equals(BayType.FIGHTER)) {
                        setASFCapacity(Math.min((getCurrentASFCapacity() + amount), getASFCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(aeroBay).equals(BayType.SMALL_CRAFT)) {
                        setSmallCraftCapacity(Math.min((getCurrentSmallCraftCapacity() + amount), getSmallCraftCapacity()));
                        break;
                    } else {
                        //This shouldn't happen
                        LogManager.getLogger().error("Fighter got assigned to a non-ASF, non-SC bay.");
                        break;
                    }
                }
                // This shouldn't happen either
                LogManager.getLogger().error("Fighter's bay number assignment produced a null bay");
                break;
            case UnitType.DROPSHIP:
                setDocks(Math.min((getCurrentDocks() + amount), getDocks()));
                break;
            case UnitType.SMALL_CRAFT:
                setSmallCraftCapacity(Math.min((getCurrentSmallCraftCapacity() + amount), getSmallCraftCapacity()));
                break;
            case UnitType.INFANTRY:
                // Infantry bay capacities are in tons, so consumption depends on platoon type
                setInfantryCapacity(Math.min((getCurrentInfantryCapacity() + (amount * unitWeight)), getInfantryCapacity()));
                break;
            case UnitType.BATTLE_ARMOR:
                setBattleArmorCapacity(Math.min((getCurrentBattleArmorCapacity() + amount),getBattleArmorCapacity()));
                break;
            case UnitType.TANK:
            case UnitType.NAVAL:
            case UnitType.VTOL:
                // Use the assigned bay number to determine if we need to update ASF or Small Craft capacity
                Bay tankBay = getEntity().getBayById(bayNumber);
                if (tankBay != null) {
                    if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_LIGHT)) {
                        setLightVehicleCapacity(Math.min((getCurrentLightVehicleCapacity() + amount), getLightVehicleCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_HEAVY)) {
                        setHeavyVehicleCapacity(Math.min((getCurrentHeavyVehicleCapacity() + amount), getHeavyVehicleCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_SH)) {
                        setSuperHeavyVehicleCapacity(Math.min((getCurrentSuperHeavyVehicleCapacity() + amount), getSuperHeavyVehicleCapacity()));
                        break;
                    } else {
                        //This shouldn't happen
                        LogManager.getLogger().error("Vehicle got assigned to a non-light/heavy/super heavy vehicle bay.");
                        break;
                    }
                }
                // This shouldn't happen either
                LogManager.getLogger().error("Vehicle's bay number assignment produced a null bay");
                break;
        }
    }

    public int getDocks() {
        return getEntity().getDocks();
    }

    // Get only collars to which a DropShip has been assigned
    public int getCurrentDocks() {
        return dockCapacity;
    }

    // Used to assign a Dropship to a collar on a specific Jumpship in the TO&E
    public void setDocks(int docks) {
        dockCapacity = docks;
    }

    public double getLightVehicleCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof LightVehicleBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a light tank has been assigned
    public double getCurrentLightVehicleCapacity() {
        return lVeeCapacity;
    }

    // Used to assign a tank to a bay on a specific transport ship in the TO&E
    public void setLightVehicleCapacity(double bays) {
        lVeeCapacity = bays;
    }

    public double getHeavyVehicleCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof HeavyVehicleBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a heavy tank has been assigned
    public double getCurrentHeavyVehicleCapacity() {
        return hVeeCapacity;
    }

    // Used to assign a tank to a bay on a specific transport ship in the TO&E
    public void setHeavyVehicleCapacity(double bays) {
        hVeeCapacity = bays;
    }

    public double getSuperHeavyVehicleCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof SuperHeavyVehicleBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a super heavy tank has been assigned
    public double getCurrentSuperHeavyVehicleCapacity() {
        return shVeeCapacity;
    }

    // Used to assign a tank to a bay on a specific transport ship in the TO&E
    public void setSuperHeavyVehicleCapacity(double bays) {
        shVeeCapacity = bays;
    }

    public double getBattleArmorCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof BattleArmorBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }
    // Get only bays to which a ba squad has been assigned
    public double getCurrentBattleArmorCapacity() {
        return baCapacity;
    }

    // Used to assign a ba squad to a bay on a specific transport ship in the TO&E
    public void setBattleArmorCapacity(double bays) {
        baCapacity = bays;
    }

    public double getInfantryCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof InfantryBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Return the unused tonnage of any conventional infantry bays
    public double getCurrentInfantryCapacity() {
        return infCapacity;
    }

    // Used to assign an infantry unit to a bay on a specific transport ship in the TO&E
    // Tonnage consumed depends on the platoon/squad weight
    public void setInfantryCapacity(double tonnage) {
        infCapacity = tonnage;
    }

    public double getASFCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof ASFBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a fighter has been assigned
    public double getCurrentASFCapacity() {
        return aeroCapacity;
    }

    // Used to assign a fighter to a bay on a specific transport ship in the TO&E
    public void setASFCapacity(double bays) {
        aeroCapacity = bays;
    }

    public double getSmallCraftCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof SmallCraftBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a small craft has been assigned
    public double getCurrentSmallCraftCapacity() {
        return scCapacity;
    }

    // Used to assign a small craft to a bay on a specific transport ship in the TO&E
    public void setSmallCraftCapacity(double bays) {
        scCapacity = bays;
    }

    public double getMechCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof MechBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a mech has been assigned
    public double getCurrentMechCapacity() {
        return mechCapacity;
    }

    // Used to assign a mech or LAM to a bay on a specific transport ship in the TO&E
    public void setMechCapacity(double bays) {
        mechCapacity = bays;
    }

    public double getProtomechCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof ProtomechBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    // Get only bays to which a protomech has been assigned
    public double getCurrentProtomechCapacity() {
        return protoCapacity;
    }

    // Used to assign a Protomech to a bay on a specific transport ship in the TO&E
    public void setProtoCapacity(double bays) {
        protoCapacity = bays;
    }

    /**
     * Bay loading utility used when assigning units to bay-equipped transport units
     * For each passed-in unit, this will find the first available, transport bay and set
     * both the target bay and the UUID of the transport ship. Once in the MM lobby, this data
     * will be used to actually load the unit into a bay on the transport.
     * @param units  Vector of units that we wish to load into this transport
     */
    public void loadTransportShip(Vector<Unit> units) {
        for (Unit u : units) {
            int unitType = u.getEntity().getUnitType();
            double unitWeight;
            if (u.getEntity().getUnitType() == UnitType.INFANTRY) {
                unitWeight = calcInfantryBayWeight(u.getEntity());
            } else {
                unitWeight = u.getEntity().getWeight();
            }
            int bayNumber = Utilities.selectBestBayFor(u.getEntity(), getEntity());
            addTransportedUnit(u, bayNumber);
            updateBayCapacity(unitType, unitWeight, false, bayNumber);
        }
    }

    /**
     * Calculates transport bay space required by an infantry platoon,
     * which is not the same as the flat weight of that platoon
     * @param unit The Entity that we need the weight for
     */
    public double calcInfantryBayWeight(Entity unit) {
        PlatoonType type = PlatoonType.getPlatoonType(unit);
        if ((unit instanceof Infantry) && (type == PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) unit).getSquadCount();
        } else {
            return type.getWeight();
        }
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport units
     * and/or moving them to a new transport
     * @param u The unit that we wish to unload from this transport
     */
    public void unloadFromTransportShip(Unit u) {
        Objects.requireNonNull(u);

        // Remove this unit from our collection of transported units.
        removeTransportedUnit(u);

        // And if the unit is being transported by us,
        // then update its transport ship assignment (provided the
        // assignment is actually to us!).
        if (u.hasTransportShipAssignment()
                && u.getTransportShipAssignment().getTransportShip().equals(this)) {
            double unitWeight;
            if (u.getEntity().getUnitType() == UnitType.INFANTRY) {
                unitWeight = calcInfantryBayWeight(u.getEntity());
            } else {
                unitWeight = u.getEntity().getWeight();
            }

            updateBayCapacity(u.getEntity().getUnitType(), unitWeight,
                    true, u.getTransportShipAssignment().getBayNumber());

            u.setTransportShipAssignment(null);
        }
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     */
    public void unloadTransportShip() {
        clearTransportedUnits();
        initializeBaySpace();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTransportShipAssignment()
                    && Objects.equals(this, u.getTransportShipAssignment().getTransportShip())) {
                u.setTransportShipAssignment(null);
            }
        });
    }

    public double getUnitCostMultiplier() {
        double multiplier = 1.0;
        if (!isRepairable()) {
            //if the unit is not repairable, set it as equal to its parts separately
            //this is not RAW, but not really a way to make that work and this makes more sense
            //although we might want to adjust it downward because of the labor cost of salvaging
            return 1.0;
        }
        double tonnage = 100;
        if (entity instanceof Mech && ((Mech) entity).isIndustrial()) {
            tonnage = 400;
        } else if (entity instanceof VTOL) {
            tonnage = 30;
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode() == EntityMovementMode.WHEELED || entity.getMovementMode() == EntityMovementMode.NAVAL) {
                tonnage = 200;
            }
            else if (entity.getMovementMode() == EntityMovementMode.HOVER || entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
                tonnage = 50;
            }
            else if (entity.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                tonnage = 75;
            }
            else if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                tonnage = 25;
            }
        } else if (entity instanceof Dropship) {
            if (((Aero) entity).isSpheroid()) {
                multiplier = 28;
            } else {
                multiplier = 36;
            }
        } else if (entity instanceof SmallCraft) {
            tonnage = 50;
        } else if (entity instanceof SpaceStation) {
            multiplier = 5;
        } else if (entity instanceof Warship) {
            multiplier = 2;
        } else if (entity instanceof Jumpship) {
            multiplier = 1.25;
        } else if (entity instanceof Aero) {
            tonnage = 200;
        }

        if (!(entity instanceof Infantry) && !(entity instanceof Dropship) && !(entity instanceof Jumpship)) {
            multiplier = 1 + (entity.getWeight() / tonnage);
        }

        if (entity.isOmni()) {
            multiplier *= 1.25;
        }
        return multiplier;
    }

    public Money getBuyCost() {
        Money cost = Money.of((getEntity() instanceof Infantry) ? getEntity().getAlternateCost()
                : getEntity().getCost(false));

        if (getEntity().isMixedTech()) {
            cost = cost.multipliedBy(getCampaign().getCampaignOptions().getMixedTechUnitPriceMultiplier());
        } else if (getEntity().isClan()) {
            cost = cost.multipliedBy(getCampaign().getCampaignOptions().getClanUnitPriceMultiplier());
        } else { // Inner Sphere Entity
            cost = cost.multipliedBy(getCampaign().getCampaignOptions().getInnerSphereUnitPriceMultiplier());
        }

        return cost;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "unit", "id", id, "type", getClass());
        pw.println(MHQXMLUtility.writeEntityToXmlString(entity, indent, getCampaign().getEntities()));
        for (Person driver : drivers) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "driverId", driver.getId());
        }

        for (Person gunner : gunners) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gunnerId", gunner.getId());
        }

        for (Person crew : vesselCrew) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vesselCrewId", crew.getId());
        }

        if (navigator != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "navigatorId", navigator.getId());
        }

        if (techOfficer != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techOfficerId", techOfficer.getId());
        }

        if (tech != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techId", tech.getId());
        }

        // If this entity is assigned to a transport, write that
        if (hasTransportShipAssignment()) {
            pw.println(MHQXMLUtility.indentStr(indent) + "<transportShip id=\""
                    + getTransportShipAssignment().getTransportShip().getId()
                    + "\" baynumber=\"" + getTransportShipAssignment().getBayNumber() + "\"/>");
        }

        for (Unit unit : getTransportedUnits()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportedUnitId", unit.getId());
        }

        // Used transport bay space
        if ((getEntity() != null) && !getEntity().getTransportBays().isEmpty()) {
            if (aeroCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "asfCapacity", aeroCapacity);
            }

            if (baCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "baCapacity", baCapacity);
            }

            if (dockCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dockCapacity", dockCapacity);
            }

            if (hVeeCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hVeeCapacity", hVeeCapacity);
            }

            if (infCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "infCapacity", infCapacity);
            }

            if (lVeeCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lVeeCapacity", lVeeCapacity);
            }

            if (mechCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mechCapacity", mechCapacity);
            }

            if (protoCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "protoCapacity", protoCapacity);
            }

            if (scCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scCapacity", scCapacity);
            }

            if (shVeeCapacity > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shVeeCapacity", shVeeCapacity);
            }
        }
        // Salvage status
        if (salvaged) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvaged", true);
        }

        if (site != SITE_BAY) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "site", site);
        }

        if (forceId != Force.FORCE_NONE) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceId", forceId);
        }

        if (scenarioId != Scenario.S_DEFAULT_ID) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioId", scenarioId);
        }

        if (daysToArrival > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToArrival", daysToArrival);
        }

        if (daysSinceMaintenance > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysSinceMaintenance", daysSinceMaintenance);
        }

        if (daysActivelyMaintained > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysActivelyMaintained", daysActivelyMaintained);
        }

        if (astechDaysMaintained > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "astechDaysMaintained", astechDaysMaintained);
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maintenanceMultiplier", maintenanceMultiplier);

        if (mothballTime > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mothballTime", mothballTime);
        }

        if (mothballed) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mothballed", true);
        }

        if (!fluffName.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fluffName", fluffName);
        }

        if (!history.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "history", history);
        }

        if (refit != null) {
            refit.writeToXML(pw, indent);
        }

        if ((lastMaintenanceReport != null) && !lastMaintenanceReport.isEmpty()
                && getCampaign().getCampaignOptions().isCheckMaintenance()) {
            pw.println(MHQXMLUtility.indentStr(indent)
                    + "<lastMaintenanceReport><![CDATA[" + lastMaintenanceReport + "]]></lastMaintenanceReport>");

        }

        if (mothballInfo != null) {
            mothballInfo.writeToXML(pw, indent);
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unit");
    }

    public static Unit generateInstanceFromXML(final Node wn, final Version version,
                                               final Campaign campaign) {
        Unit retVal = new Unit();
        NamedNodeMap attrs = wn.getAttributes();
        Node idNode = attrs.getNamedItem("id");

        retVal.id = UUID.fromString(idNode.getTextContent());

        //Temp storage for used bay capacities
        boolean needsBayInitialization = true;

        // Okay, now load Part-specific fields!
        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("site")) {
                    retVal.site = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToArrival")) {
                    retVal.daysToArrival = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysActivelyMaintained")) {
                    retVal.daysActivelyMaintained = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysSinceMaintenance")) {
                    retVal.daysSinceMaintenance = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballTime")) {
                    retVal.mothballTime = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("astechDaysMaintained")) {
                    retVal.astechDaysMaintained = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("maintenanceMultiplier")) {
                    retVal.maintenanceMultiplier = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("driverId")) {
                    retVal.drivers.add(new UnitPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("gunnerId")) {
                    retVal.gunners.add(new UnitPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("vesselCrewId")) {
                    retVal.vesselCrew.add(new UnitPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("navigatorId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.navigator = new UnitPersonRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("techOfficerId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.techOfficer = new UnitPersonRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.tech = new UnitPersonRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("transportShip")) {
                    NamedNodeMap attributes = wn2.getAttributes();
                    UUID id = UUID.fromString(attributes.getNamedItem("id").getTextContent());
                    int bay = Integer.parseInt(attributes.getNamedItem("baynumber").getTextContent());
                    retVal.setTransportShipAssignment(new TransportShipAssignment(new UnitRef(id), bay));
                } else if (wn2.getNodeName().equalsIgnoreCase("transportedUnitId")) {
                    retVal.addTransportedUnit(new UnitRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("asfCapacity")) {
                    retVal.setASFCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("baCapacity")) {
                    retVal.setBattleArmorCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("dockCapacity")) {
                    retVal.setDocks(Integer.parseInt(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("hVeeCapacity")) {
                    retVal.setHeavyVehicleCapacity(Double.parseDouble(wn2.getTextContent()));
                   needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("infCapacity")) {
                    retVal.setInfantryCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("lVeeCapacity")) {
                    retVal.setLightVehicleCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("mechCapacity")) {
                    retVal.setMechCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("protoCapacity")) {
                    retVal.setProtoCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("scCapacity")) {
                    retVal.setSmallCraftCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("shVeeCapacity")) {
                    retVal.setSuperHeavyVehicleCapacity(Double.parseDouble(wn2.getTextContent()));
                    needsBayInitialization = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("salvaged")) {
                    retVal.salvaged = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballed")) {
                    retVal.mothballed = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.entity = MHQXMLUtility.parseSingleEntityMul((Element) wn2, campaign);
                } else if (wn2.getNodeName().equalsIgnoreCase("refit")) {
                    retVal.refit = Refit.generateInstanceFromXML(wn2, version, campaign, retVal);
                } else if (wn2.getNodeName().equalsIgnoreCase("history")) {
                    retVal.history = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("fluffName")) {
                    retVal.fluffName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("lastMaintenanceReport")) {
                    retVal.lastMaintenanceReport = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballInfo")) {
                    retVal.mothballInfo = MothballInfo.generateInstanceFromXML(wn2, version);
                }
                // Set up bay space values after we've loaded everything from the unit record
                // Used for older campaign
                if (retVal.entity != null && retVal.getEntity().isLargeCraft() && needsBayInitialization) {
                    retVal.initializeBaySpace();
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Could not parse unit " + idNode.getTextContent().trim(), ex);
            return null;
        }

        if (version.isLowerThan("0.49.3")) {
            CamouflageMigrator.migrateCamouflage(version, retVal.getCamouflage());
        }

        if (retVal.id == null) {
            LogManager.getLogger().warn("ID not pre-defined; generating unit's ID.");
            retVal.id = UUID.randomUUID();
        }

        // Protection for old broken campaign files
        // Also for entities that do not have an external ID to match the UUID
        if (retVal.entity.getExternalIdAsString().equals("-1")
                || !(retVal.entity.getExternalIdAsString().equals(retVal.id.toString()))) {
            retVal.entity.setExternalIdAsString(retVal.id.toString());
        }

        return retVal;
    }

    /**
     * This function returns an html-coded list that says what
     * quirks are enabled for this unit
     * @return
     */
    public @Nullable String getQuirksList() {
        String quirkString = "";
        boolean first = true;
        if (null != getEntity().getGame() && getEntity().getGame().getOptions().booleanOption("stratops_quirks")) {
            for (Enumeration<IOptionGroup> i = getEntity().getQuirks().getGroups(); i.hasMoreElements();) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                    IOption quirk = j.nextElement();
                    if (quirk.booleanValue()) {
                        if (first) {
                            first = false;
                        } else {
                            quirkString += "<br>";
                        }
                        quirkString += quirk.getDisplayableNameWithValue();
                    }
                }
            }
        }
        return quirkString.isBlank() ? null : "<html>" + quirkString + "</html>";
    }

    public void acquireQuirk(String name, Object value) {
        for (Enumeration<IOption> i = getEntity().getQuirks().getOptions(); i.hasMoreElements();) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(value);
            }
        }
    }

    /**
     * The weekly maintenance cycle combined with a user defined maintenance cycle length
     * is confusing and difficult to manage so lets just make maintenance costs relative
     * to the length of the maintenance cycle that the user defined
     * @return
     */
    public Money getMaintenanceCost() {
        return getWeeklyMaintenanceCost()
                .multipliedBy(getCampaign().getCampaignOptions().getMaintenanceCycleDays())
                .dividedBy(7.0);
    }

    public Money getWeeklyMaintenanceCost() {
        Entity en = getEntity();
        Money mCost = Money.zero();
        Money value;

        // we will assume sale value for now, but make this customizable
        if (getCampaign().getCampaignOptions().isEquipmentContractSaleValue()) {
            value = getSellValue();
        } else {
            value = getBuyCost();
        }

        if (getCampaign().getCampaignOptions().isUsePercentageMaint()) {
            if (en instanceof Mech) {
                mCost = value.multipliedBy(0.02);
            } else if (en instanceof Warship) {
                mCost = value.multipliedBy(0.07);
            } else if (en instanceof Jumpship) {
                mCost = value.multipliedBy(0.06);
            } else if (en instanceof Dropship) {
                mCost = value.multipliedBy(0.05);
            } else if (en instanceof ConvFighter) {
                mCost = value.multipliedBy(0.03);
            } else if (en instanceof Aero) {
                mCost = value.multipliedBy(0.04);
            } else if (en instanceof VTOL) {
                mCost = value.multipliedBy(0.02);
            } else if (en instanceof Tank) {
                mCost = value.multipliedBy(0.015);
            } else if (en instanceof BattleArmor) {
                mCost = value.multipliedBy(0.03);
            } else if (en instanceof Infantry) {
                mCost = value.multipliedBy(0.005);
            }
            // Mothballed Units cost only 10% to maintain
            if (isMothballed()) {
                mCost = mCost.multipliedBy(0.1);
            }
        } else {
            if (en instanceof Mech) {
                if (en.isOmni()) {
                    return Money.of(100.0);
                } else {
                    return Money.of(75.0);
                }
            } else if (en instanceof Warship) {
                return Money.of(5000.0);
            } else if (en instanceof Jumpship) {
                return Money.of(800.0);
            } else if (en instanceof Dropship) {
                return Money.of(500.0);
            } else if (en instanceof ConvFighter) {
                return Money.of(50.0);
            } else if (en instanceof Aero) {
                if (en.isOmni()) {
                    return Money.of(125.0);
                } else  {
                    return Money.of(65.0);
                }
            } else if (en instanceof VTOL) {
                return Money.of(65.0);
            } else if (en instanceof Tank) {
                return Money.of(25.0);
            } else if (en instanceof BattleArmor) {
                return Money.of(((BattleArmor) en).getTroopers() * 50.0);
            } else if (en instanceof Infantry) {
                return Money.of(((Infantry) en).getSquadCount()*10.0);
            }
        }
        return mCost.dividedBy(52.0);
    }

    public void addPart(Part part) {
        part.setUnit(this);
        parts.add(part);
    }

    /**
     * This will check a unit for certain parts and if they are missing, it will create a new
     * version and update its condition. checking for existing parts makes this a more complicated
     * method but it also ensures that you can call this at any time and you won't overwrite existing
     * parts
     */
    public void initializeParts(boolean addParts) {
        int erating = 0;
        int builtInHeatSinks = 0;
        if (!(entity instanceof FighterSquadron) && (null != entity.getEngine())) {
            erating = entity.getEngine().getRating();
            if (entity.getEngine().isFusion()) {
                //10 weight-free heatsinks for fusion engines.
                //Used for fighters to prevent adding extra parts
                builtInHeatSinks = 10;
            }
        }

        ArrayList<Part> partsToAdd = new ArrayList<>();
        ArrayList<Part> partsToRemove = new ArrayList<>();

        Part gyro = null;
        Part engine = null;
        Part lifeSupport = null;
        Part sensor = null;
        Part cockpit = null;
        Part rightHand = null;
        Part rightLowerArm = null;
        Part rightUpperArm = null;
        Part leftHand = null;
        Part leftLowerArm = null;
        Part leftUpperArm = null;
        Part rightFoot = null;
        Part rightLowerLeg = null;
        Part rightUpperLeg = null;
        Part leftFoot = null;
        Part leftLowerLeg = null;
        Part leftUpperLeg = null;
        Part centerFoot = null;
        Part centerLowerLeg = null;
        Part centerUpperLeg = null;
        Part rightFrontFoot = null;
        Part rightLowerFrontLeg = null;
        Part rightUpperFrontLeg = null;
        Part leftFrontFoot = null;
        Part leftLowerFrontLeg = null;
        Part leftUpperFrontLeg = null;
        Part qvGear = null;
        Part structuralIntegrity = null;
        Part[] locations = new Part[entity.locations()];
        Part[] armor = new Part[entity.locations()];
        Part[] armorRear = new Part[entity.locations()];
        Part[] stabilisers = new Part[entity.locations()];
        Hashtable<Integer,Part> equipParts = new Hashtable<>();
        Hashtable<Integer,Part> ammoParts = new Hashtable<>();
        Hashtable<Integer,Part> heatSinks = new Hashtable<>();
        Hashtable<Integer,Part> jumpJets = new Hashtable<>();
        Hashtable<Integer,Part[]> baEquipParts = new Hashtable<>();
        Part motiveSystem = null;
        Part avionics = null;
        Part fcs = null;
        Part cic = null;
        Part chargingSystem = null;
        Part driveCoil = null;
        Part driveController = null;
        Part fieldInitiator = null;
        Part heliumTank = null;
        Part lfBattery = null;
        Part landingGear = null;
        Part turretLock = null;
        ArrayList<Part> aeroHeatSinks = new ArrayList<>();
        int podAeroHeatSinks = 0;
        Part motiveType = null;
        Part primaryW = null;
        Part secondaryW = null;
        Part infantryArmor = null;
        Part dropCollar = null;
        Part kfBoom = null;
        Part protoLeftArmActuator = null;
        Part protoRightArmActuator = null;
        Part protoLegsActuator = null;
        ArrayList<Part> protoJumpJets = new ArrayList<>();
        Part aeroThrustersLeft = null;
        Part aeroThrustersRight = null;
        Part coolingSystem = null;
        Map<Integer, Part> bays = new HashMap<>();
        Map<Integer, List<Part>> bayPartsToAdd = new HashMap<>();
        Map<Integer, Part> jumpCollars = new HashMap<>();
        Map<Integer, Part> gravDecks = new HashMap<>();

        for (Part part : parts) {
            if (part instanceof MekGyro || part instanceof MissingMekGyro) {
                gyro = part;
            } else if (part instanceof EnginePart || part instanceof MissingEnginePart) {
                // reverse compatibility check, spaceships get different engines
                if (!(entity instanceof SmallCraft || entity instanceof Jumpship)) {
                    engine = part;
                }
            } else if (part instanceof SpacecraftEngine
                    || part instanceof MissingSpacecraftEngine
                    || part instanceof SVEnginePart
                    || part instanceof MissingSVEngine) {
                engine = part;
            } else if (part instanceof MekLifeSupport  || part instanceof MissingMekLifeSupport) {
                lifeSupport = part;
            } else if (part instanceof AeroLifeSupport  || part instanceof MissingAeroLifeSupport) {
                lifeSupport = part;
            } else if (part instanceof MekSensor || part instanceof MissingMekSensor) {
                sensor = part;
            } else if (part instanceof ProtomekSensor || part instanceof MissingProtomekSensor) {
                sensor = part;
            } else if (part instanceof MekCockpit || part instanceof MissingMekCockpit) {
                cockpit = part;
            }  else if (part instanceof VeeSensor || part instanceof MissingVeeSensor) {
                sensor = part;
            }  else if (part instanceof InfantryMotiveType) {
                motiveType = part;
            }  else if (part instanceof InfantryArmorPart) {
                infantryArmor = part;
            }  else if (part instanceof InfantryWeaponPart) {
                if (((InfantryWeaponPart) part).isPrimary()) {
                    primaryW = part;
                } else {
                    secondaryW = part;
                }
            }  else if (part instanceof StructuralIntegrity) {
                structuralIntegrity = part;
            } else if (part instanceof MekLocation) {
                if (((MekLocation) part).getLoc() < locations.length) {
                    locations[((MekLocation) part).getLoc()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof TankLocation) {
                if (((TankLocation) part).getLoc() < locations.length) {
                    locations[((TankLocation) part).getLoc()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof MissingRotor) {
                locations[VTOL.LOC_ROTOR] = part;
            } else if (part instanceof MissingTurret && Tank.LOC_TURRET < locations.length) {
                locations[Tank.LOC_TURRET] = part;
            } else if (part instanceof ProtomekLocation) {
                if (((ProtomekLocation) part).getLoc() < locations.length) {
                    locations[((ProtomekLocation) part).getLoc()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof MissingMekLocation
                    || part instanceof MissingProtomekLocation) {
                if (part.getLocation() < locations.length) {
                    locations[part.getLocation()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof BattleArmorSuit) {
                if ((entity instanceof BattleArmor)
                        && ((BattleArmorSuit) part).getTrooper() < locations.length) {
                    locations[((BattleArmorSuit) part).getTrooper()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof MissingBattleArmorSuit) {
                if ((entity instanceof BattleArmor)
                        && ((MissingBattleArmorSuit) part).getTrooper() < locations.length) {
                    locations[((MissingBattleArmorSuit) part).getTrooper()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof Armor) {
                if (part.getLocation() < armor.length) {
                    if (((Armor) part).isRearMounted()) {
                        armorRear[part.getLocation()] = part;
                    } else {
                        armor[part.getLocation()] = part;
                    }
                } else {
                    partsToRemove.add(part);
                }
            } else if ((part instanceof VeeStabiliser || part instanceof MissingVeeStabiliser)) {
                if (part.getLocation() < stabilisers.length) {
                    stabilisers[part.getLocation()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof AmmoBin) {
                ammoParts.put(((AmmoBin) part).getEquipmentNum(), part);
            } else if (part instanceof MissingAmmoBin) {
                ammoParts.put(((MissingAmmoBin) part).getEquipmentNum(), part);
            } else if (part instanceof HeatSink) {
                heatSinks.put(((HeatSink) part).getEquipmentNum(), part);
            } else if (part instanceof MissingHeatSink) {
                heatSinks.put(((MissingHeatSink) part).getEquipmentNum(), part);
            } else if (part instanceof JumpJet) {
                jumpJets.put(((JumpJet) part).getEquipmentNum(), part);
            } else if (part instanceof MissingJumpJet) {
                jumpJets.put(((MissingJumpJet) part).getEquipmentNum(), part);
            }  else if (part instanceof BattleArmorEquipmentPart) {
                if (!(entity instanceof BattleArmor)) {
                    partsToRemove.add(part);
                } else {
                    Part[] parts = baEquipParts.get(((BattleArmorEquipmentPart) part).getEquipmentNum());
                    if (null == parts) {
                        parts = new Part[((BattleArmor) entity).getSquadSize()];
                    }
                    parts[((BattleArmorEquipmentPart) part).getTrooper() - BattleArmor.LOC_TROOPER_1] = part;
                    baEquipParts.put(((BattleArmorEquipmentPart) part).getEquipmentNum(), parts);
                }
            } else if (part instanceof MissingBattleArmorEquipmentPart) {
                if (!(entity instanceof BattleArmor)) {
                    partsToRemove.add(part);
                } else {
                    Part[] parts = baEquipParts.get(((MissingBattleArmorEquipmentPart) part).getEquipmentNum());
                    if (null == parts) {
                        parts = new Part[((BattleArmor) entity).getSquadSize()];
                    }
                    parts[((MissingBattleArmorEquipmentPart) part).getTrooper() - BattleArmor.LOC_TROOPER_1] = part;
                    baEquipParts.put(((MissingBattleArmorEquipmentPart) part).getEquipmentNum(), parts);
                }
            } else if (part instanceof EquipmentPart) {
                equipParts.put(((EquipmentPart) part).getEquipmentNum(), part);
            } else if (part instanceof MissingEquipmentPart) {
                equipParts.put(((MissingEquipmentPart) part).getEquipmentNum(), part);
            } else if (part instanceof MekActuator || part instanceof MissingMekActuator) {
                int type = -1;
                int loc = -1;
                if (part instanceof MekActuator) {
                    type = ((MekActuator) part).getType();
                    loc = ((MekActuator) part).getLocation();
                } else {
                    type = ((MissingMekActuator) part).getType();
                    loc = ((MissingMekActuator) part).getLocation();
                }
                if (type == Mech.ACTUATOR_UPPER_ARM) {
                    if (loc == Mech.LOC_RARM) {
                        rightUpperArm = part;
                    } else {
                        leftUpperArm = part;
                    }
                } else if (type == Mech.ACTUATOR_LOWER_ARM) {
                    if (loc == Mech.LOC_RARM) {
                        rightLowerArm = part;
                    } else {
                        leftLowerArm = part;
                    }
                } else if (type == Mech.ACTUATOR_HAND) {
                    if (loc == Mech.LOC_RARM) {
                        rightHand = part;
                    } else {
                        leftHand = part;
                    }
                } else if (type == Mech.ACTUATOR_UPPER_LEG) {
                    if (loc == Mech.LOC_LARM) {
                        leftUpperFrontLeg = part;
                    } else if (loc == Mech.LOC_RARM) {
                        rightUpperFrontLeg = part;
                    } else if (loc == Mech.LOC_RLEG) {
                        rightUpperLeg = part;
                    } else if (loc == Mech.LOC_LLEG) {
                        leftUpperLeg = part;
                    } else if (loc == Mech.LOC_CLEG) {
                        centerUpperLeg = part;
                    } else {
                        LogManager.getLogger().error("Unknown location of " + loc + " for a Upper Leg Actuator.");
                    }
                } else if (type == Mech.ACTUATOR_LOWER_LEG) {
                    if (loc == Mech.LOC_LARM) {
                        leftLowerFrontLeg = part;
                    } else if (loc == Mech.LOC_RARM) {
                        rightLowerFrontLeg = part;
                    } else if (loc == Mech.LOC_RLEG) {
                        rightLowerLeg = part;
                    } else if (loc == Mech.LOC_LLEG) {
                        leftLowerLeg = part;
                    } else if (loc == Mech.LOC_CLEG) {
                        centerLowerLeg = part;
                    } else {
                        LogManager.getLogger().error("Unknown location of " + loc + " for a Lower Leg Actuator.");
                    }
                } else if (type == Mech.ACTUATOR_FOOT) {
                    if (loc == Mech.LOC_LARM) {
                        leftFrontFoot = part;
                    } else if (loc == Mech.LOC_RARM) {
                        rightFrontFoot = part;
                    } else if (loc == Mech.LOC_RLEG) {
                        rightFoot = part;
                    } else if (loc == Mech.LOC_LLEG) {
                        leftFoot = part;
                    } else if (loc == Mech.LOC_CLEG) {
                        centerFoot = part;
                    } else {
                        LogManager.getLogger().error("Unknown location of " + loc + " for a Foot Actuator.");
                    }
                }
            } else if (part instanceof QuadVeeGear || part instanceof MissingQuadVeeGear) {
                qvGear = part;
            } else if (part instanceof Avionics || part instanceof MissingAvionics) {
                avionics = part;
            // Don't initialize FCS for JS/WS/SS, use CIC for these instead
            } else if (part instanceof FireControlSystem || part instanceof MissingFireControlSystem) {
                fcs = part;
                // for reverse compatibility, calculate costs
                if (part instanceof FireControlSystem) {
                    ((FireControlSystem) fcs).calculateCost();
                }
                // If a JS/WS/SS already has an FCS, remove it
                if (entity instanceof Jumpship) {
                    partsToRemove.add(part);
                }
            // Don't initialize CIC for any ASF/SC/DS, use FCS for these instead
            } else if ((part instanceof CombatInformationCenter || part instanceof MissingCIC)
                    && (entity instanceof Jumpship)) {
                cic = part;
                // for reverse compatibility, calculate costs
                if (part instanceof CombatInformationCenter) {
                    ((CombatInformationCenter) cic).calculateCost();
                }
            // Only JumpShips and WarShips have these
            } else if ((part instanceof LFBattery || part instanceof MissingLFBattery)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation)
                            && ((Jumpship) entity).hasLF())) {
                lfBattery = part;
            } else if ((part instanceof KFHeliumTank || part instanceof MissingKFHeliumTank)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                heliumTank = part;
            } else if ((part instanceof KFChargingSystem || part instanceof MissingKFChargingSystem)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                chargingSystem = part;
            } else if ((part instanceof KFFieldInitiator || part instanceof MissingKFFieldInitiator)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                fieldInitiator = part;
            } else if ((part instanceof KFDriveController || part instanceof MissingKFDriveController)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                driveController = part;
            } else if ((part instanceof KFDriveCoil || part instanceof MissingKFDriveCoil)
                    && ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                driveCoil = part;
            // For Small Craft and larger, add this as a container for all their heatsinks instead of adding hundreds
            // of individual heatsink parts.
            } else if (part instanceof SpacecraftCoolingSystem
                    && (entity instanceof SmallCraft || entity instanceof Jumpship)) {
                coolingSystem = part;
            } else if (part instanceof AeroSensor || part instanceof MissingAeroSensor) {
                sensor = part;
            } else if (part instanceof LandingGear || part instanceof MissingLandingGear) {
                landingGear = part;
                // If a JS/WS/SS already has Landing Gear, remove it
                if (entity instanceof Jumpship) {
                    partsToRemove.add(part);
                }
            } else if (part instanceof AeroHeatSink || part instanceof MissingAeroHeatSink) {
                // If a SC/DS/JS/WS/SS already has heatsinks, remove them. We're using the spacecraft cooling system instead
                if (entity instanceof SmallCraft || entity instanceof Jumpship) {
                    partsToRemove.add(part);
                } else if (entity.getEngine().isFusion() && builtInHeatSinks > 0) {
                    // Don't add parts for the 10 heatsinks included with a fusion engine
                    partsToRemove.add(part);
                    builtInHeatSinks--;
                } else {
                    aeroHeatSinks.add(part);
                    if (part.isOmniPodded()) {
                        podAeroHeatSinks++;
                    }
                }
            } else if (part instanceof MotiveSystem) {
                motiveSystem = part;
            } else if (part instanceof TurretLock) {
                turretLock = part;
            } else if (part instanceof DropshipDockingCollar || part instanceof MissingDropshipDockingCollar) {
                dropCollar = part;
            } else if ((part instanceof KfBoom) || (part instanceof MissingKFBoom)) {
                kfBoom = part;
            } else if ((part instanceof ProtomekArmActuator) || (part instanceof MissingProtomekArmActuator)) {
                int loc = part.getLocation();
                if (loc == Protomech.LOC_LARM) {
                    protoLeftArmActuator = part;
                } else if (loc == Protomech.LOC_RARM) {
                    protoRightArmActuator = part;
                }
            } else if (part instanceof ProtomekLegActuator || part instanceof MissingProtomekLegActuator) {
                protoLegsActuator = part;
            } else if (part instanceof ProtomekJumpJet || part instanceof MissingProtomekJumpJet) {
                protoJumpJets.add(part);
            } else if ((part instanceof Thrusters)
                    && (entity instanceof SmallCraft || entity instanceof Jumpship)) {
                if (((Thrusters) part).isLeftThrusters()) {
                    aeroThrustersLeft = part;
                } else {
                    aeroThrustersRight = part;
                }
            } else if ((part instanceof MissingThrusters)
                    && (entity instanceof SmallCraft || entity instanceof Jumpship)) {
                if (((MissingThrusters) part).isLeftThrusters()) {
                    aeroThrustersLeft = part;
                } else {
                    aeroThrustersRight = part;
                }
            } else if (part instanceof TransportBayPart) {
                bays.put(((TransportBayPart) part).getBayNumber(), part);
            } else if (part instanceof JumpshipDockingCollar) {
                jumpCollars.put(((JumpshipDockingCollar) part).getCollarNumber(), part);
            } else if (part instanceof GravDeck) {
                gravDecks.put(((GravDeck) part).getDeckNumber(), part);
            }

            part.updateConditionFromPart();
        }
        // Remove invalid Aero parts due to changes after 0.45.4
        for (Part part : partsToRemove) {
            removePart(part);
        }
        // now check to see what is null
        for (int i = 0; i < locations.length; i++) {
            if (entity.getOInternal(i) == IArmorState.ARMOR_NA) {
                // this is not a valid location, so we should skip it
                continue;
            }
            if (null == locations[i]) {
                if (entity instanceof Mech) {
                    MekLocation mekLocation = new MekLocation(i, (int) getEntity().getWeight(),
                            getEntity().getStructureType(), TechConstants.isClan(entity.getStructureTechLevel()),
                            hasTSM(), entity instanceof QuadMech, false, false, getCampaign());
                    addPart(mekLocation);
                    partsToAdd.add(mekLocation);
                } else if (entity instanceof Protomech && i != Protomech.LOC_NMISS) {
                    ProtomekLocation protomekLocation = new ProtomekLocation(i, (int) getEntity().getWeight(), getEntity().getStructureType(), ((Protomech) getEntity()).hasMyomerBooster(), false, getCampaign());
                    addPart(protomekLocation);
                    partsToAdd.add(protomekLocation);
                } else if (entity instanceof Tank && i != Tank.LOC_BODY) {
                    if (entity instanceof VTOL) {
                        if (i == VTOL.LOC_ROTOR) {
                            Rotor rotor = new Rotor((int) getEntity().getWeight(), getCampaign());
                            addPart(rotor);
                            partsToAdd.add(rotor);
                        } else if (i == VTOL.LOC_TURRET) {
                            if (((VTOL) entity).hasNoTurret()) {
                                continue;
                            }
                            Turret turret = new Turret(i, (int) getEntity().getWeight(), getCampaign());
                            addPart(turret);
                            partsToAdd.add(turret);
                        } else if (i == VTOL.LOC_TURRET_2) {
                            if (((VTOL) entity).hasNoDualTurret()) {
                                continue;
                            }
                            Turret turret = new Turret(i, (int) getEntity().getWeight(), getCampaign());
                            addPart(turret);
                            partsToAdd.add(turret);
                        } else {
                            TankLocation tankLocation = new TankLocation(i, (int) getEntity().getWeight(), getCampaign());
                            addPart(tankLocation);
                            partsToAdd.add(tankLocation);
                        }
                    } else if (i == Tank.LOC_TURRET) {
                         if (((Tank) entity).hasNoTurret()) {
                             continue;
                         }
                         Turret turret = new Turret(i, (int) getEntity().getWeight(), getCampaign());
                         addPart(turret);
                         partsToAdd.add(turret);
                    } else if (i == Tank.LOC_TURRET_2) {
                         if (((Tank) entity).hasNoDualTurret()) {
                             continue;
                         }
                         Turret turret = new Turret(i, (int) getEntity().getWeight(), getCampaign());
                         addPart(turret);
                         partsToAdd.add(turret);
                    } else {
                        TankLocation tankLocation = new TankLocation(i, (int) getEntity().getWeight(), getCampaign());
                        addPart(tankLocation);
                        partsToAdd.add(tankLocation);
                    }
                } else if ((entity instanceof BattleArmor) && (i != 0) && (i <= ((BattleArmor) entity).getSquadSize())) {
                    BattleArmorSuit baSuit = new BattleArmorSuit((BattleArmor) entity, i, getCampaign());
                    addPart(baSuit);
                    partsToAdd.add(baSuit);
                }
            }
            if (null == armor[i]) {
                if (entity instanceof Protomech) {
                    ProtomekArmor a = new ProtomekArmor((int) getEntity().getWeight(), getEntity().getArmorType(i),
                            getEntity().getOArmor(i, false), i, true, getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else if (entity instanceof BattleArmor) {
                    BaArmor a = new BaArmor((int) getEntity().getWeight(), getEntity().getOArmor(i, false), entity.getArmorType(1), i, entity.isClan(), getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else if (entity.isSupportVehicle() && (entity.getArmorType(i) == EquipmentType.T_ARMOR_STANDARD)) {
                    Armor a = new SVArmor(entity.getBARRating(i), entity.getArmorTechRating(),
                            getEntity().getOArmor(i, false), i, getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else {
                    Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, false), i, false, entity.isClanArmor(i), getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                }
            }
            if ((null == armorRear[i]) && entity.hasRearArmor(i)) {
                Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, true), i, true, entity.isClanArmor(i), getCampaign());
                addPart(a);
                partsToAdd.add(a);
            }
            if (entity instanceof Tank && null == stabilisers[i] && i != Tank.LOC_BODY) {
                VeeStabiliser s = new VeeStabiliser((int) getEntity().getWeight(), i, getCampaign());
                addPart(s);
                partsToAdd.add(s);
            }
        }
        for (Mounted m : entity.getEquipment()) {
            if ((m.getLocation() == Entity.LOC_NONE) && !(m.getType() instanceof AmmoType)) {
                continue;
            }
            // We want to ignore weapon groups so that we don't get phantom weapons
            if (m.isWeaponGroup()) {
                continue;
            }
            // Anti-Mek attacks aren't actual parts
            if (m.getType() instanceof InfantryAttack) {
                continue;
            }
            if (!m.getType().isHittable()) {
                //there are some kind of non-hittable parts we might want to include for cost calculations
                if (!(m.getType() instanceof MiscType)) {
                    continue;
                }
                if (!(m.getType().hasFlag(MiscType.F_BA_MANIPULATOR) ||
                        m.getType().hasFlag(MiscType.F_BA_MEA) ||
                        m.getType().hasFlag(MiscType.F_AP_MOUNT) ||
                        m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP))) {
                    continue;
                }
            }
            if (m.getType() instanceof AmmoType) {
                int eqnum = entity.getEquipmentNum(m);
                Part apart = ammoParts.get(eqnum);
                boolean oneShot = m.isOneShotAmmo();
                int fullShots = oneShot ? 1 : ((AmmoType) m.getType()).getShots();
                if (null == apart) {
                    if (entity instanceof BattleArmor) {
                        apart = new BattleArmorAmmoBin((int) entity.getWeight(), (AmmoType) m.getType(), eqnum,
                                ((BattleArmor) entity).getSquadSize() * (fullShots - m.getBaseShotsLeft()),
                                oneShot, getCampaign());
                    } else if (entity.usesWeaponBays()) {
                        apart = new LargeCraftAmmoBin((int) entity.getWeight(), (AmmoType) m.getType(), eqnum,
                                fullShots - m.getBaseShotsLeft(), m.getSize(), getCampaign());
                        ((LargeCraftAmmoBin) apart).setBay(entity.getBayByAmmo((AmmoMounted) m));
                    } else if (entity.isSupportVehicle()
                            && (((AmmoType) m.getType()).getAmmoType() == AmmoType.T_INFANTRY)) {
                        Mounted<?> weapon = m.getLinkedBy();
                        while (weapon.getType() instanceof AmmoType) {
                            weapon = weapon.getLinkedBy();
                        }
                        int size = m.getOriginalShots() / ((InfantryWeapon) weapon.getType()).getShots();
                        apart = new InfantryAmmoBin((int) entity.getWeight(), (AmmoType) m.getType(), eqnum,
                                m.getOriginalShots() - m.getBaseShotsLeft(),
                                (InfantryWeapon) weapon.getType(), size, weapon.isOmniPodMounted(), getCampaign());
                    } else {
                        apart = new AmmoBin((int) entity.getWeight(), (AmmoType) m.getType(), eqnum,
                                fullShots - m.getBaseShotsLeft(), oneShot, m.isOmniPodMounted(), getCampaign());
                    }
                    addPart(apart);
                    partsToAdd.add(apart);

                }
            } else if (m.getType() instanceof MiscType
                    && (m.getType().hasFlag(MiscType.F_HEAT_SINK) || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
                if (m.getLocation() == Entity.LOC_NONE) {
                    //heat sinks located in LOC_NONE are base unhittable heat sinks
                    continue;
                }
                int eqnum = entity.getEquipmentNum(m);
                Part epart = heatSinks.get(eqnum);
                if (null == epart) {
                    epart = new HeatSink((int) entity.getWeight(), m.getType(), eqnum,
                            m.isOmniPodMounted(), getCampaign());
                    addPart(epart);
                    partsToAdd.add(epart);
                }
            } else if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                int eqnum = entity.getEquipmentNum(m);
                Part epart = jumpJets.get(eqnum);
                if (null == epart) {
                    epart = new JumpJet((int) entity.getWeight(), m.getType(), eqnum,
                            m.isOmniPodMounted(), getCampaign());
                    addPart(epart);
                    partsToAdd.add(epart);
                    if (entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                        protoJumpJets.add(epart);
                    }
                }
            } else {
                int eqnum = entity.getEquipmentNum(m);
                EquipmentType type = m.getType();
                if (entity instanceof BattleArmor) {
                    //for BattleArmor we have multiple parts per mount, one for each trooper
                    Part[] eparts = baEquipParts.get(eqnum);
                    for (int i = 0; i < ((BattleArmor) entity).getSquadSize(); i++) {
                        if ((null == eparts) || (null == eparts[i])) {
                            Part epart = new BattleArmorEquipmentPart((int) entity.getWeight(), type, eqnum,
                                    m.getSize(), i+BattleArmor.LOC_TROOPER_1, getCampaign());
                            addPart(epart);
                            partsToAdd.add(epart);
                        }
                    }
                } else {
                    Part epart = equipParts.get(eqnum);
                    if (null == epart) {
                        if (type instanceof InfantryAttack) {
                            continue;
                        }
                        if ((entity instanceof Infantry) && (m.getLocation() != Infantry.LOC_FIELD_GUNS)) {
                            //don't add weapons here for infantry, unless field guns
                            continue;
                        }
                        if (type instanceof BayWeapon) {
                            //weapon bays aren't real parts
                            continue;
                        }
                        epart = new EquipmentPart((int) entity.getWeight(), type, eqnum, m.getSize(),
                                m.isOmniPodMounted(), getCampaign());
                        if ((type instanceof MiscType) && type.hasFlag(MiscType.F_MASC)) {
                            epart = new MASC((int) entity.getWeight(), type, eqnum, getCampaign(),
                                    erating, m.isOmniPodMounted());
                        }
                        addPart(epart);
                        partsToAdd.add(epart);
                    }
                }
            }
        }

        if ((null == engine) && !(entity instanceof Infantry) && !(entity instanceof FighterSquadron)) {
            if (entity instanceof SmallCraft || entity instanceof Jumpship) {
                engine = new SpacecraftEngine((int) entity.getWeight(), 0, getCampaign(), entity.isClan());
                addPart(engine);
                partsToAdd.add(engine);
                ((SpacecraftEngine) engine).calculateTonnage();
            } else if (entity.isSupportVehicle()) {
                // Check for trailer with no engine
                if (entity.hasEngine() && entity.getEngine().getEngineType() != Engine.NONE) {
                    // Surface vehicles (including vehicles) have to choose the fuel type for a combustion engine.
                    // Fixed wing with an ICE will have the fuel type set to NONE, which is technically
                    // not correct but does the job of distinguishing the turbine from other ICE types.
                    FuelType fuel = FuelType.NONE;
                    if (entity instanceof Tank) {
                        fuel = ((Tank) entity).getICEFuelType();
                    }
                    engine = new SVEnginePart((int) entity.getWeight(), entity.getEngine().getWeightEngine(entity),
                            entity.getEngine().getEngineType(), entity.getEngineTechRating(),
                            fuel, getCampaign());
                    addPart(engine);
                    partsToAdd.add(engine);
                }
            } else if (null != entity.getEngine()) {
                engine = new EnginePart((int) entity.getWeight(),new Engine(entity.getEngine().getRating(), entity.getEngine().getEngineType(), entity.getEngine().getFlags()), getCampaign(), entity.getMovementMode() == EntityMovementMode.HOVER && entity instanceof Tank);
                addPart(engine);
                partsToAdd.add(engine);
            }
        }

        // Transport Bays
        for (Bay bay : entity.getTransportBays()) {
            bayPartsToAdd.put(bay.getBayNumber(), new ArrayList<>());
            BayType btype = BayType.getTypeForBay(bay);
            Part bayPart = bays.get(bay.getBayNumber());
            if (null == bayPart) {
                bayPart = new TransportBayPart((int) entity.getWeight(),
                        bay.getBayNumber(), bay.getCapacity(), getCampaign());
                addPart(bayPart);
                partsToAdd.add(bayPart);
                for (int i = 0; i < bay.getDoors(); i++) {
                    Part door = new BayDoor((int) entity.getWeight(), getCampaign());
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                }
                if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    for (int i = 0; i < bay.getCapacity(); i++) {
                        Part cubicle = new Cubicle((int) entity.getWeight(), btype, getCampaign());
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                    }
                }
            } else {
                List<Part> doors = bayPart.getChildParts().stream()
                        .filter(p -> ((p instanceof BayDoor) || (p instanceof MissingBayDoor)))
                        .collect(Collectors.toList());
                while (bay.getDoors() > doors.size()) {
                    Part door = new MissingBayDoor((int) entity.getWeight(), getCampaign());
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                    doors.add(door);
                }
                if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    List<Part> cubicles = bayPart.getChildParts().stream()
                            .filter(p -> ((p instanceof Cubicle) || (p instanceof MissingCubicle)))
                            .collect(Collectors.toList());
                    while (bay.getCapacity() > cubicles.size()) {
                        Part cubicle = new MissingCubicle((int) entity.getWeight(), btype, getCampaign());
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                        cubicles.add(cubicle);
                    }
                }
            }
        }


        if (entity instanceof Mech) {
            if (null == gyro) {
                gyro =  new MekGyro((int) entity.getWeight(), entity.getGyroType(), entity.getOriginalWalkMP(), entity.isClan(), getCampaign());
                addPart(gyro);
                partsToAdd.add(gyro);
            }
            if (null == lifeSupport) {
                lifeSupport = new MekLifeSupport((int) entity.getWeight(), getCampaign());
                addPart(lifeSupport);
                partsToAdd.add(lifeSupport);
            }
            if (null == sensor) {
                sensor = new MekSensor((int) entity.getWeight(), getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if (null == cockpit) {
                cockpit = new MekCockpit((int) entity.getWeight(), ((Mech) entity).getCockpitType(), entity.isClan(), getCampaign());
                addPart(cockpit);
                partsToAdd.add(cockpit);
            }
            if (null == rightUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
                rightUpperArm = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM, getCampaign());
                addPart(rightUpperArm);
                partsToAdd.add(rightUpperArm);
            }
            if (null == leftUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
                leftUpperArm = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM, getCampaign());
                addPart(leftUpperArm);
                partsToAdd.add(leftUpperArm);
            }
            if (null == rightLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
                rightLowerArm = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM, getCampaign());
                addPart(rightLowerArm);
                partsToAdd.add(rightLowerArm);
            }

            if (null == leftLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
                leftLowerArm = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM, getCampaign());
                addPart(leftLowerArm);
                partsToAdd.add(leftLowerArm);
            }

            if (null == rightHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
                rightHand = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_RARM, getCampaign());
                addPart(rightHand);
                partsToAdd.add(rightHand);
            }

            if (null == leftHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
                leftHand = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_LARM, getCampaign());
                addPart(leftHand);
                partsToAdd.add(leftHand);
            }

            if (null == rightUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)) {
                rightUpperLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG, getCampaign());
                addPart(rightUpperLeg);
                partsToAdd.add(rightUpperLeg);
            }

            if (null == leftUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)) {
                leftUpperLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG, getCampaign());
                addPart(leftUpperLeg);
                partsToAdd.add(leftUpperLeg);
            }

            if ((centerUpperLeg == null) && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_CLEG)) {
                centerUpperLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_CLEG, getCampaign());
                addPart(centerUpperLeg);
                partsToAdd.add(centerUpperLeg);
            }

            if (null == rightLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG)) {
                rightLowerLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG, getCampaign());
                addPart(rightLowerLeg);
                partsToAdd.add(rightLowerLeg);
            }

            if (null == leftLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)) {
                leftLowerLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG, getCampaign());
                addPart(leftLowerLeg);
                partsToAdd.add(leftLowerLeg);
            }

            if ((centerLowerLeg == null) && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_CLEG)) {
                centerLowerLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_CLEG, getCampaign());
                addPart(centerLowerLeg);
                partsToAdd.add(centerLowerLeg);
            }

            if (null == rightFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RLEG)) {
                rightFoot = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RLEG, getCampaign());
                addPart(rightFoot);
                partsToAdd.add(rightFoot);
            }

            if (null == leftFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)) {
                leftFoot = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LLEG, getCampaign());
                addPart(leftFoot);
                partsToAdd.add(leftFoot);
            }

            if ((centerFoot == null) && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_CLEG)) {
                centerFoot = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_CLEG, getCampaign());
                addPart(centerFoot);
                partsToAdd.add(centerFoot);
            }

            if (null == rightUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM)) {
                rightUpperFrontLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM, getCampaign());
                addPart(rightUpperFrontLeg);
                partsToAdd.add(rightUpperFrontLeg);
            }
            if (null == leftUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM)) {
                leftUpperFrontLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM, getCampaign());
                addPart(leftUpperFrontLeg);
                partsToAdd.add(leftUpperFrontLeg);
            }
            if (null == rightLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM)) {
                rightLowerFrontLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM, getCampaign());
                addPart(rightLowerFrontLeg);
                partsToAdd.add(rightLowerFrontLeg);
            }
            if (null == leftLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM)) {
                leftLowerFrontLeg = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM, getCampaign());
                addPart(leftLowerFrontLeg);
                partsToAdd.add(leftLowerFrontLeg);
            }
            if (null == rightFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RARM)) {
                rightFrontFoot = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RARM, getCampaign());
                addPart(rightFrontFoot);
                partsToAdd.add(rightFrontFoot);
            }
            if (null == leftFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LARM)) {
                leftFrontFoot = new MekActuator((int) entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LARM, getCampaign());
                addPart(leftFrontFoot);
                partsToAdd.add(leftFrontFoot);
            }
        }
        if (entity instanceof QuadVee && null == qvGear) {
            qvGear = new QuadVeeGear((int) entity.getWeight(), getCampaign());
            addPart(qvGear);
            partsToAdd.add(qvGear);
        }
        if (entity instanceof Aero) {
            if (null == structuralIntegrity) {
                structuralIntegrity = new StructuralIntegrity((int) entity.getWeight(), getCampaign());
                addPart(structuralIntegrity);
                partsToAdd.add(structuralIntegrity);
            }
            if (null == avionics) {
                avionics = new Avionics((int) entity.getWeight(), getCampaign());
                addPart(avionics);
                partsToAdd.add(avionics);
            }
            if (null == cic && entity instanceof Jumpship) {
                cic = new CombatInformationCenter((int) entity.getWeight(), Money.zero(), getCampaign());
                addPart(cic);
                partsToAdd.add(cic);
                ((CombatInformationCenter) cic).calculateCost();
            }
            if (null == driveCoil && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                driveCoil = new KFDriveCoil((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(driveCoil);
                partsToAdd.add(driveCoil);
            }
            if (null == driveController && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                driveController = new KFDriveController((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(driveController);
                partsToAdd.add(driveController);
            }
            if (null == fieldInitiator && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                fieldInitiator = new KFFieldInitiator((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(fieldInitiator);
                partsToAdd.add(fieldInitiator);
            }
            if (null == chargingSystem && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                chargingSystem = new KFChargingSystem((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(chargingSystem);
                partsToAdd.add(chargingSystem);
            }
            if (null == heliumTank && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                heliumTank = new KFHeliumTank((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(heliumTank);
                partsToAdd.add(heliumTank);
            }
            if (null == lfBattery
                    && (entity instanceof Jumpship) && !(entity instanceof SpaceStation) && ((Jumpship) entity).hasLF()) {
                lfBattery = new LFBattery((int) entity.getWeight(), ((Jumpship) entity).getDriveCoreType(), entity.getDocks(), getCampaign());
                addPart(lfBattery);
                partsToAdd.add(lfBattery);
            }
            if (null == fcs && !(entity instanceof Jumpship)) {
                fcs = new FireControlSystem((int) entity.getWeight(), Money.zero(), getCampaign());
                addPart(fcs);
                partsToAdd.add(fcs);
                ((FireControlSystem) fcs).calculateCost();
            }
            if (null == coolingSystem && (entity instanceof SmallCraft || entity instanceof Jumpship)) {
                int sinkType = ((Aero) entity).getHeatType();
                if (sinkType == Aero.HEAT_DOUBLE && entity.isClan()) {
                    sinkType = AeroHeatSink.CLAN_HEAT_DOUBLE;
                }
                coolingSystem = new SpacecraftCoolingSystem((int) entity.getWeight(), ((Aero) entity).getOHeatSinks(), sinkType, getCampaign());
                addPart(coolingSystem);
                partsToAdd.add(coolingSystem);
            }
            if (null == sensor) {
                sensor = new AeroSensor((int) entity.getWeight(), entity instanceof Dropship || entity instanceof Jumpship, getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if (null == landingGear && !(entity instanceof Jumpship)) {
                landingGear = new LandingGear((int) entity.getWeight(), getCampaign());
                addPart(landingGear);
                partsToAdd.add(landingGear);
            }
            if (null == lifeSupport) {
                lifeSupport = new AeroLifeSupport((int) entity.getWeight(), Money.zero(), !(entity instanceof SmallCraft || entity instanceof Jumpship), getCampaign());
                addPart(lifeSupport);
                partsToAdd.add(lifeSupport);
                ((AeroLifeSupport) lifeSupport).calculateCost();
            }
            if (null == dropCollar && entity instanceof Dropship) {
                dropCollar = new DropshipDockingCollar((int) entity.getWeight(), getCampaign(),
                        ((Dropship) entity).getCollarType());
                addPart(dropCollar);
                partsToAdd.add(dropCollar);
            }
            if (null == kfBoom && entity instanceof Dropship) {
                kfBoom = new KfBoom((int) entity.getWeight(), getCampaign(),
                        ((Dropship) entity).getBoomType());
                addPart(kfBoom);
                partsToAdd.add(kfBoom);
            }
            if (jumpCollars.isEmpty() && entity instanceof Jumpship) {
                for (DockingCollar collar : entity.getDockingCollars()) {
                    Part collarPart = new JumpshipDockingCollar (0, collar.getCollarNumber(), getCampaign(), ((Jumpship) entity).getDockingCollarType());
                    jumpCollars.put(collar.getCollarNumber(), collarPart);
                    addPart(collarPart);
                    partsToAdd.add(collarPart);
                }
            }
            if (gravDecks.isEmpty() && entity instanceof Jumpship) {
                for (int deck : ((Jumpship) entity).getGravDecks()) {
                    int deckNumber = ((Jumpship) entity).getGravDecks().indexOf(deck);
                    // Default to standard size
                    int deckType = GravDeck.GRAV_DECK_TYPE_STANDARD;
                    if (deck > Jumpship.GRAV_DECK_STANDARD_MAX && deck <= Jumpship.GRAV_DECK_LARGE_MAX) {
                        deckType = GravDeck.GRAV_DECK_TYPE_LARGE;
                    } else if (deck > Jumpship.GRAV_DECK_LARGE_MAX) {
                        deckType = GravDeck.GRAV_DECK_TYPE_HUGE;
                    }
                    Part gravDeckPart = new GravDeck (0, deckNumber, getCampaign(), deckType);
                    gravDecks.put(deckNumber, gravDeckPart);
                    addPart(gravDeckPart);
                    partsToAdd.add(gravDeckPart);
                }
            }

            // Only add heatsink parts to fighters. Larger craft get a cooling system instead.
            if (!(entity instanceof SmallCraft) && !(entity instanceof Jumpship)) {
                int hsinks = ((Aero) entity).getOHeatSinks()
                        - aeroHeatSinks.size()
                        // Ignore the 10 free heatsinks we took out for fusion powered fighters
                        - ((entity.getEngine() != null && entity.getEngine().isFusion()) ? 10 : 0);
                int podhsinks = ((Aero) entity).getPodHeatSinks() - podAeroHeatSinks;
                int sinkType = ((Aero) entity).getHeatType();
                if (sinkType == Aero.HEAT_DOUBLE && entity.isClan()) {
                    sinkType = AeroHeatSink.CLAN_HEAT_DOUBLE;
                }

                // add busted heat sinks even if they're "engine free" so they can be repaired
                if (hsinks == 0) {
                    for (int x = 0; x < ((Aero) entity).getHeatSinkHits(); x++) {
                        MissingAeroHeatSink aHeatSink = new MissingAeroHeatSink((int) entity.getWeight(),
                                sinkType, false, getCampaign());
                        addPart(aHeatSink);
                        partsToAdd.add(aHeatSink);
                    }
                } else {
                    while (hsinks > 0) {
                        AeroHeatSink aHeatSink = new AeroHeatSink((int) entity.getWeight(),
                                sinkType, podhsinks > 0, getCampaign());
                        addPart(aHeatSink);
                        partsToAdd.add(aHeatSink);
                        hsinks--;
                        if (podhsinks > 0) {
                            podhsinks--;
                        }
                    }
                }
            }
            if (entity instanceof SmallCraft || entity instanceof Jumpship) {
                if (aeroThrustersLeft == null) {
                    aeroThrustersLeft = new Thrusters(0, getCampaign(), true);
                    addPart(aeroThrustersLeft);
                    partsToAdd.add(aeroThrustersLeft);
                }
                if (aeroThrustersRight == null) {
                    aeroThrustersRight = new Thrusters(0, getCampaign(), false);
                    addPart(aeroThrustersRight);
                    partsToAdd.add(aeroThrustersRight);
                }
            }
         }
        if (entity instanceof Tank) {
            if (null == motiveSystem) {
                motiveSystem = new MotiveSystem((int) entity.getWeight(), getCampaign());
                addPart(motiveSystem);
                partsToAdd.add(motiveSystem);
            }
            if (null == sensor) {
                sensor = new VeeSensor((int) entity.getWeight(), getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if (!(entity instanceof VTOL) && !((Tank) entity).hasNoTurret() && null == turretLock) {
                turretLock = new TurretLock(getCampaign());
                addPart(turretLock);
                partsToAdd.add(turretLock);
            }
        }
        if (entity instanceof Protomech) {
            if (!entity.entityIsQuad()) {
                if (null == protoLeftArmActuator) {
                    protoLeftArmActuator = new ProtomekArmActuator((int) entity.getWeight(),Protomech.LOC_LARM, getCampaign());
                    addPart(protoLeftArmActuator);
                    partsToAdd.add(protoLeftArmActuator);
                }
                if (null == protoRightArmActuator) {
                    protoRightArmActuator = new ProtomekArmActuator((int) entity.getWeight(),Protomech.LOC_RARM, getCampaign());
                    addPart(protoRightArmActuator);
                    partsToAdd.add(protoRightArmActuator);
                }
            }
            if (null == protoLegsActuator) {
                protoLegsActuator = new ProtomekLegActuator((int) entity.getWeight(), getCampaign());
                addPart(protoLegsActuator);
                partsToAdd.add(protoLegsActuator);
            }
            if (null == sensor) {
                sensor = new ProtomekSensor((int) entity.getWeight(), getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            int jj = (entity).getOriginalJumpMP() - protoJumpJets.size();
            while (jj > 0) {
                ProtomekJumpJet protoJJ = new ProtomekJumpJet((int) entity.getWeight(), getCampaign());
                addPart(protoJJ);
                partsToAdd.add(protoJJ);
                jj--;
            }
        }

        if (isConventionalInfantry()) {
            if ((null == motiveType) && (entity.getMovementMode() != EntityMovementMode.INF_LEG)) {
                int number = entity.getOInternal(Infantry.LOC_INFANTRY);
                if (((Infantry) entity).isMechanized()) {
                    number = ((Infantry) entity).getSquadCount();
                }
                while (number > 0) {
                    motiveType = new InfantryMotiveType(0, getCampaign(), entity.getMovementMode());
                    addPart(motiveType);
                    partsToAdd.add(motiveType);
                    number--;
                }
            }
            if (null == infantryArmor) {
                EquipmentType eq = ((Infantry) entity).getArmorKit();
                if (null != eq) {
                    infantryArmor = new EquipmentPart(0, eq, 0, 1.0, false, getCampaign());
                } else {
                    infantryArmor = new InfantryArmorPart(0, getCampaign(),
                            ((Infantry) entity).getArmorDamageDivisor(), ((Infantry) entity).isArmorEncumbering(), ((Infantry) entity).hasDEST(), ((Infantry) entity).hasSneakCamo(), ((Infantry) entity).hasSneakECM(), ((Infantry) entity).hasSneakIR(), ((Infantry) entity).hasSpaceSuit());
                }
                if (infantryArmor.getStickerPrice().isPositive()) {
                    int number = entity.getOInternal(Infantry.LOC_INFANTRY);
                    while (number > 0) {
                        infantryArmor = new InfantryArmorPart(0, getCampaign(),
                                ((Infantry) entity).getArmorDamageDivisor(), ((Infantry) entity).isArmorEncumbering(), ((Infantry) entity).hasDEST(), ((Infantry) entity).hasSneakCamo(), ((Infantry) entity).hasSneakECM(),
                                ((Infantry) entity).hasSneakIR(), ((Infantry) entity).hasSpaceSuit());
                        addPart(infantryArmor);
                        partsToAdd.add(infantryArmor);
                        number--;
                    }
                }
            }
            InfantryWeapon primaryType = ((Infantry) entity).getPrimaryWeapon();
            InfantryWeapon secondaryType = ((Infantry) entity).getSecondaryWeapon();
            if ((null == primaryW) && (null != primaryType)) {
                int number = (((Infantry) entity).getSquadSize() - ((Infantry) entity).getSecondaryWeaponsPerSquad()) * ((Infantry) entity).getSquadCount();
                while (number > 0) {
                    primaryW = new InfantryWeaponPart((int) entity.getWeight(), primaryType, -1, getCampaign(), true);
                    addPart(primaryW);
                    partsToAdd.add(primaryW);
                    number--;
                }

            }
            if (null == secondaryW && null != secondaryType) {
                int number = ((Infantry) entity).getSecondaryWeaponsPerSquad() * ((Infantry) entity).getSquadCount();
                while (number > 0) {
                    secondaryW = new InfantryWeaponPart((int) entity.getWeight(), secondaryType, -1, getCampaign(), false);
                    addPart(secondaryW);
                    partsToAdd.add(secondaryW);
                    number--;
                }
            }
        }
        if (getEntity() instanceof LandAirMech) {
            if (null == avionics) {
                avionics = new Avionics((int) entity.getWeight(), getCampaign());
                addPart(avionics);
                partsToAdd.add(avionics);
            }
            if (null == landingGear) {
                landingGear = new LandingGear((int) entity.getWeight(), getCampaign());
                addPart(landingGear);
                partsToAdd.add(landingGear);
            }
        }

        if (addParts) {
            for (Part p : partsToAdd) {
                getCampaign().getQuartermaster().addPart(p, 0);
            }
        }
        // We can't add the child parts to the transport bay part until they have been added to the
        // campaign and have an id.
        for (int bayNum : bayPartsToAdd.keySet()) {
            Optional<Part> bayPart = getParts().stream()
                    .filter(p -> (p instanceof TransportBayPart) && ((TransportBayPart) p).getBayNumber() == bayNum)
                    .findAny();
            if (bayPart.isPresent()) {
                bayPartsToAdd.get(bayNum).forEach(p -> bayPart.get().addChildPart(p));
            }
        }
        if (getEntity().isOmni()) {
            podSpace.clear();
            for (int loc = 0; loc < getEntity().locations(); loc++) {
                podSpace.add(new PodSpace(loc, this));
            }
            podSpace.forEach(ps -> ps.updateConditionFromEntity(false));
        }
    }

    public List<Part> getParts() {
        return parts;
    }

    /**
     * Find a part on a unit.
     * @param predicate A predicate to apply to each part on the unit.
     * @return The first part which matched the predicate, otherwise null.
     */
    public @Nullable Part findPart(Predicate<Part> predicate) {
        for (Part part : parts) {
            if (predicate.test(part)) {
                return part;
            }
        }

        return null;
    }

    public void setParts(ArrayList<Part> newParts) {
        parts = newParts;
    }

    public List<PodSpace> getPodSpace() {
        return podSpace;
    }

    public void refreshPodSpace() {
        podSpace.forEach(ps -> ps.updateConditionFromEntity(false));
    }

    public List<AmmoBin> getWorkingAmmoBins() {
        List<AmmoBin> ammo = new ArrayList<>();
        for (Part part : parts) {
            if (part instanceof AmmoBin) {
                ammo.add((AmmoBin) part);
            }
        }
        return ammo;
    }

    public Camouflage getCamouflage() {
        return (entity == null) ? new Camouflage() : entity.getCamouflage();
    }

    public Camouflage getUtilizedCamouflage(final Campaign campaign) {
        if (getCamouflage().hasDefaultCategory()) {
            final Force force = campaign.getForce(getForceId());
            return (force != null) ? force.getCamouflageOrElse(campaign.getCamouflage())
                    : campaign.getCamouflage();
        } else {
            return getCamouflage();
        }
    }

    public @Nullable Image getImage(final Component component) {
        return getImage(component, getUtilizedCamouflage(getCampaign()), true);
    }

    public @Nullable Image getImage(final Component component, final Camouflage camouflage,
                                    final boolean showDamage) {
        if (MHQStaticDirectoryManager.getMechTileset() == null) {
            return null;
        }
        final Image base = MHQStaticDirectoryManager.getMechTileset().imageFor(getEntity());
        return new EntityImage(base, camouflage, component, getEntity()).loadPreviewImage(showDamage);
    }

    public Color determineForegroundColor(String type) {
        if (isDeployed()) {
            return MekHQ.getMHQOptions().getDeployedForeground();
        } else if (!isPresent()) {
            return MekHQ.getMHQOptions().getInTransitForeground();
        } else if (isRefitting()) {
            return MekHQ.getMHQOptions().getRefittingForeground();
        } else if (isMothballing()) {
            return MekHQ.getMHQOptions().getMothballingForeground();
        } else if (isMothballed()) {
            return MekHQ.getMHQOptions().getMothballedForeground();
        } else if (getCampaign().getCampaignOptions().isCheckMaintenance() && isUnmaintained()) {
            return MekHQ.getMHQOptions().getUnmaintainedForeground();
        } else if (!isRepairable()) {
            return MekHQ.getMHQOptions().getNotRepairableForeground();
        } else if (!isFunctional()) {
            return MekHQ.getMHQOptions().getNonFunctionalForeground();
        } else if (hasPartsNeedingFixing()) {
            return MekHQ.getMHQOptions().getNeedsPartsFixedForeground();
        } else if (getActiveCrew().size() < getFullCrewSize()) {
            return MekHQ.getMHQOptions().getUncrewedForeground();
        } else {
            return UIManager.getColor(type + ".Foreground");
        }
    }

    public Color determineBackgroundColor(String type) {
        if (isDeployed()) {
            return MekHQ.getMHQOptions().getDeployedBackground();
        } else if (!isPresent()) {
            return MekHQ.getMHQOptions().getInTransitBackground();
        } else if (isRefitting()) {
            return MekHQ.getMHQOptions().getRefittingBackground();
        } else if (isMothballing()) {
            return MekHQ.getMHQOptions().getMothballingBackground();
        } else if (isMothballed()) {
            return MekHQ.getMHQOptions().getMothballedBackground();
        } else if (getCampaign().getCampaignOptions().isCheckMaintenance() && isUnmaintained()) {
            return MekHQ.getMHQOptions().getUnmaintainedBackground();
        } else if (!isRepairable()) {
            return MekHQ.getMHQOptions().getNotRepairableBackground();
        } else if (!isFunctional()) {
            return MekHQ.getMHQOptions().getNonFunctionalBackground();
        } else if (hasPartsNeedingFixing()) {
            return MekHQ.getMHQOptions().getNeedsPartsFixedBackground();
        } else if (getActiveCrew().size() < getFullCrewSize()) {
            return MekHQ.getMHQOptions().getUncrewedBackground();
        } else {
            return UIManager.getColor(type + ".Background");
        }
    }

    /**
     * Determines which crew member is considered the unit commander. For solo-piloted units there is
     * only one option, but units with multiple crew (vehicles, aerospace vessels, infantry) use the following
     * criteria:
     * 1. The highest rank.
     * 2. If there is more than one with the highest rank, select according to the following order, from
     *    highest to lowest priority:
     *    a. vessel crew
     *    b. gunners
     *    c. pilots/drivers
     *    d. hyperspace navigator.
     * 3. If there is still a tie, take the first one in the crew list.
     *
     * @return The unit commander, or null if the unit has no crew.
     */
    public @Nullable Person getCommander() {
        // take first by rank
        // if rank is tied, take gunners over drivers
        // if two of the same type are tie rank, take the first one
        if (entity == null) {
            return null;
        }

        Person commander = null;

        for (Person p : vesselCrew) {
            if (p.outRanks(commander)) {
                commander = p;
            }
        }
        for (Person p : gunners) {
            if (p.outRanks(commander)) {
                commander = p;
            }
        }
        for (Person p : drivers) {
            if (p.outRanks(commander)) {
                commander = p;
            }
        }
        if (navigator != null) {
            if (navigator.outRanks(commander)) {
                commander = navigator;
            }
        }
        return commander;
    }

    public boolean hasCommander() {
        return getCommander() != null;
    }

    public void resetPilotAndEntity() {
        entity.getCrew().resetGameState();
        if (entity.getCrew().getSlotCount() > 1) {
            final String driveType = SkillType.getDrivingSkillFor(entity);
            final String gunType = SkillType.getGunnerySkillFor(entity);
            if (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos()) {
                //Command console; each crew is assigned as both driver and gunner
                int slot = 0;
                for (Person p : gunners) {
                    if (p.hasSkill(gunType) && p.hasSkill(driveType) && p.getStatus().isActive()
                            && slot < entity.getCrew().getSlotCount()) {
                        assignToCrewSlot(p, slot, gunType, driveType);
                        slot++;
                    }
                }
                while (slot < entity.getCrew().getSlotCount()) {
                    entity.getCrew().setMissing(true, slot++);
                }
            } else {
                // tripod, quadvee, or dual cockpit; driver and gunner are assigned separately
                Optional<Person> person = drivers.stream().filter(p -> p.hasSkill(driveType) && p.getStatus().isActive())
                        .findFirst();
                if (person.isPresent()) {
                    assignToCrewSlot(person.get(), 0, gunType, driveType);
                } else {
                    entity.getCrew().setMissing(true, 0);
                }
                person = gunners.stream().filter(p -> p.hasSkill(driveType) && p.getStatus().isActive())
                        .findFirst();
                if (person.isPresent()) {
                    assignToCrewSlot(person.get(), 1, gunType, driveType);
                } else {
                    entity.getCrew().setMissing(true, 1);
                }
                int techPos = entity.getCrew().getCrewType().getTechPos();
                if (techPos >= 0) {
                    Person to = techOfficer;
                    if (to != null) {
                        if (!to.hasSkill(driveType) || !to.hasSkill(gunType) || !to.getStatus().isActive()) {
                            to = null;
                        }
                    }
                    if (to != null) {
                        assignToCrewSlot(to, techPos, gunType, driveType);
                    } else {
                        entity.getCrew().setMissing(true, techPos);
                    }
                }
            }
        } else {
            if ((entity.getEntityType() & Entity.ETYPE_LAND_AIR_MECH) == 0) {
                calcCompositeCrew();
            } else {
                refreshLAMPilot();
            }
            if (entity.getCrew().isMissing(0)) {
                return;
            }
            Person commander = getCommander();
            if (null == commander) {
                entity.getCrew().setMissing(true, 0);
                return;
            }
            entity.getCrew().setName(commander.getFullTitle(), 0);
            entity.getCrew().setNickname(commander.getCallsign(), 0);
            entity.getCrew().setGender(commander.getGender(), 0);
            entity.getCrew().setClanPilot(commander.isClanPersonnel(), 0);
            entity.getCrew().setPortrait(commander.getPortrait().clone(), 0);
            entity.getCrew().setExternalIdAsString(commander.getId().toString(), 0);
            entity.getCrew().setToughness(commander.getToughness(), 0);

            if (entity instanceof Tank) {
                ((Tank) entity).setCommanderHit(commander.getHits() > 0);
            }
            entity.getCrew().setMissing(false, 0);
        }

        // Clear any stale game data that may somehow have gotten set incorrectly
        getCampaign().clearGameData(entity);
        // Set up SPAs, Implants, Edge, etc
        if (getCampaign().getCampaignOptions().isUseAbilities()) {
            PilotOptions options = new PilotOptions(); // MegaMek-style as it is sent to MegaMek
            // This double enumeration is annoying to work with for crew-served units.
            // Get the option names while we enumerate so they can be used later
            List<String> optionNames = new ArrayList<>();
            Set<String> cyberOptionNames = new HashSet<>();
            for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
                 IOptionGroup group = i.nextElement();
                 for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                     IOption option = j.nextElement();
                     if (group.getKey().equals(PersonnelOptions.MD_ADVANTAGES)) {
                         cyberOptionNames.add(option.getName());
                     } else {
                         optionNames.add(option.getName());
                     }
                 }
            }

            // For crew-served units, let's look at the abilities of the group. If more than half the crew
            // (gunners and pilots only, for spacecraft) have an ability, grant the benefit to the unit
            // TODO : Mobile structures, large naval support vehicles
            if (entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                    || entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                    || entity.hasETypeFlag(Entity.ETYPE_TANK)
                    || entity.hasETypeFlag(Entity.ETYPE_INFANTRY)
                    || entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH)) {
                // Find the unit commander
                Person commander = getCommander();
                // If there is no crew, there's nothing left to do here.
                if (null == commander) {
                    return;
                }
                // Combine drivers and gunners into a single list

                List<Person> crew = new ArrayList<>(drivers);

                // Infantry and BA troops count as both drivers and gunners
                // only count them once.
                if (!entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                    crew.addAll(gunners);
                }
                double crewSize = crew.size();

                // This does the following:
                // 1. For each crew member, get all of their PersonnelOptions by name
                // 2. Flatten the crew member options into one stream
                // 3. Group these options by their name
                // 4. For each group, group by the object value and get the counts for each value
                // 5. Take each group which has more than crewSize/2 values, and find the maximum value
                Map<String, Optional<Object>> bestOptions = crew.stream().flatMap(p -> optionNames.stream().map(n -> p.getOptions().getOption(n)))
                    .collect(Collectors.groupingBy(
                        IOption::getName,
                        Collectors.collectingAndThen(
                            Collectors.groupingBy(IOption::getValue, Collectors.counting()),
                            m -> m.entrySet().stream().filter(e -> (cyberOptionNames.contains(e.getKey()) ? e.getValue() >= crewSize : e.getValue() > crewSize / 2))
                                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                        )
                    ));

                // Go through all the options and start with the commander's value,
                // then add any values which more than half our crew had
                for (String optionName : optionNames) {
                    IOption option = commander.getOptions().getOption(optionName);
                    if (null != option) {
                        options.getOption(optionName).setValue(option.getValue());
                    }

                    if (bestOptions.containsKey(optionName)) {
                        Optional<Object> crewOption = bestOptions.get(optionName);
                        crewOption.ifPresent(o -> options.getOption(optionName).setValue(o));
                    }
                }

                // Yuck. Most cybernetic implants require all members of a unit's crew to have the implant rather than half.
                // A few just require 1/4 the crew, there's at least one commander only, some just add an effect for every
                // trooper who has the implant...you get the idea.
                // TODO : Revisit this once all implants are fully implemented.
                for (String implantName : cyberOptionNames) {
                    IOption option = commander.getOptions().getOption(implantName);
                    if (null != option) {
                        options.getOption(implantName).setValue(option.getValue());
                    }

                    if (bestOptions.containsKey(implantName)) {
                        Optional<Object> crewOption = bestOptions.get(implantName);
                        crewOption.ifPresent(o -> options.getOption(implantName).setValue(o));
                    }
                }

                // Assign the options to our unit
                entity.getCrew().setOptions(options);

                // Assign edge points to spacecraft and vehicle crews and infantry units
                // This overwrites the Edge value assigned above.
                if (getCampaign().getCampaignOptions().isUseEdge()) {
                    double sumEdge = 0;
                    int edge;
                    for (Person p : drivers) {
                        sumEdge += p.getEdge();
                    }
                    //Again, don't count infantrymen twice
                    if (!entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                        for (Person p : gunners) {
                            sumEdge += p.getEdge();
                        }
                    }
                    // Average the edge values of pilots and gunners. The Spacecraft Engineer (vessel crewmembers)
                    // handle edge solely through MHQ as noncombat personnel, so aren't considered here
                    edge = (int) Math.round(sumEdge / crewSize);
                    IOption edgeOption = entity.getCrew().getOptions().getOption(OptionsConstants.EDGE);
                    edgeOption.setValue((Integer) edge);
                }

                // Reset the composite technician used by spacecraft and infantry
                // Important if you just changed technician edge options for members of either unit type
                resetEngineer();
                // Tactics command bonus. This should actually reflect the unit's commander,
                // unlike most everything else in this block.
                // TODO : game option to use tactics as command and ind init bonus
                if (commander.hasSkill(SkillType.S_TACTICS)) {
                    entity.getCrew().setCommandBonus(commander.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
                } else {
                    entity.getCrew().setCommandBonus(0);
                }

                // TODO : Set up crew hits. This might only apply to spacecraft, and should reflect
                // the unit's current crew size vs its required crew size. There's also the question
                // of what to do with extra crew quarters and crewmember assignments beyond the minimum.

            } else {
                // For other unit types, just use the unit commander's abilities.
                Person commander = getCommander();
                PilotOptions cdrOptions = new PilotOptions(); // MegaMek-style as it is sent to MegaMek
                if (null != commander) {
                    for (String optionName : optionNames) {
                        IOption option = commander.getOptions().getOption(optionName);
                        if (null != option) {
                            cdrOptions.getOption(optionName).setValue(option.getValue());
                        }
                    }
                    for (String implantName : cyberOptionNames) {
                        IOption option = commander.getOptions().getOption(implantName);
                        if (null != option) {
                            cdrOptions.getOption(implantName).setValue(option.getValue());
                        }
                    }
                    entity.getCrew().setOptions(cdrOptions);

                    if (usesSoloPilot()) {
                        if (!commander.getStatus().isActive()) {
                            entity.getCrew().setMissing(true, 0);
                            return;
                        }
                        entity.getCrew().setHits(commander.getHits(), 0);
                    }
                }

                //There was a resetEngineer() here. We shouldn't need it as spacecraft and infantry are handled
                //by the preceding block

                //TODO: game option to use tactics as command and ind init bonus
                if (null != commander && commander.hasSkill(SkillType.S_TACTICS)) {
                    entity.getCrew().setCommandBonus(commander.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
                } else {
                    entity.getCrew().setCommandBonus(0);
                }
            }
        }
    }

    /**
     * For vehicles, infantry, and naval vessels, compute the piloting and gunnery skills based
     * on the crew as a whole.
     */
    private void calcCompositeCrew() {
        if (drivers.isEmpty() && gunners.isEmpty()) {
            entity.getCrew().setMissing(true, 0);
            entity.getCrew().setSize(0);
            return;
        }

        int piloting = 13;
        int gunnery = 13;
        int artillery = 13;
        String driveType = SkillType.getDrivingSkillFor(entity);
        String gunType = SkillType.getGunnerySkillFor(entity);
        int sumPiloting = 0;
        int nDrivers = 0;
        int sumGunnery = 0;
        int nGunners = 0;
        int nCrew = 0;

        for (Person p : drivers) {
            if (p.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }
            if (p.hasSkill(driveType)) {
                sumPiloting += p.getSkill(driveType).getFinalSkillValue();
                nDrivers++;
            } else if (entity instanceof Infantry) {
                //For infantry we need to assign an 8 if they have no antimech skill
                sumPiloting += 8;
                nDrivers++;
            }

            if (entity instanceof Tank
                    && Compute.getFullCrewSize(entity) == 1
                    && p.hasSkill(gunType)) {
                sumGunnery += p.getSkill(gunType).getFinalSkillValue();
                nGunners++;
            }
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                sumPiloting += p.getPilotingInjuryMod();
            }
        }
        for (Person p : gunners) {
            if (p.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }
            if (p.hasSkill(gunType)) {
                sumGunnery += p.getSkill(gunType).getFinalSkillValue();
                nGunners++;
            }
            if (p.hasSkill(SkillType.S_ARTILLERY)
                    && p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue() < artillery) {
                artillery = p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue();
            }
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                sumGunnery += p.getGunneryInjuryMod();
            }
        }

        for (Person p : vesselCrew) {
            if (p.getHits() == 0) {
                nCrew++;
            }
        }
        if ((getNavigator() != null) && (getNavigator().getHits() == 0)) {
            nCrew++;
        }
        //Using the tech officer field for the secondary commander; if nobody assigned to the command
        //console we will flag the entity as using the console commander, which has the effect of limiting
        //the tank to a single commander. As the console commander is not counted against crew requirements,
        //we do not increase nCrew if present.
        if ((entity instanceof Tank) && entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
            if ((techOfficer == null) || (techOfficer.getHits() > 0)) {
                ((Tank) entity).setUsingConsoleCommander(true);
            }
        }

        if (nDrivers > 0) {
            piloting = (int) Math.round(((double) sumPiloting) / nDrivers);
        }
        if (nGunners > 0) {
            gunnery = (int) Math.round(((double) sumGunnery) / nGunners);
        }
        if (entity instanceof Infantry) {
            if (entity instanceof BattleArmor) {
                int ntroopers = 0;
                //OK, we want to reorder the way we move through suits, so that we always put BA
                //in the suits with more armor. Otherwise, we may put a soldier in a suit with no
                //armor when a perfectly good suit is waiting further down the line.
                Map<String, Integer> bestSuits = new HashMap<>();
                for (int i = BattleArmor.LOC_TROOPER_1; i <= ((BattleArmor) entity).getTroopers(); i++) {
                    bestSuits.put(Integer.toString(i), entity.getArmorForReal(i));
                    if (entity.getInternal(i) < 0) {
                        bestSuits.put(Integer.toString(i), IArmorState.ARMOR_DESTROYED);
                    }
                    bestSuits = Utilities.sortMapByValue(bestSuits, true);
                }
                for (String key : bestSuits.keySet()) {
                    int i = Integer.parseInt(key);
                    if (!isBattleArmorSuitOperable(i)) {
                        //no suit here move along
                        continue;
                    }
                    if (ntroopers < nGunners) {
                        entity.setInternal(1, i);
                        ntroopers++;
                    } else {
                        entity.setInternal(0, i);
                    }
                }
                if (ntroopers < nGunners) {
                    //TODO: we have too many soldiers assigned to the available suits - do something!
                    //probably remove some crew and then re-run resetentityandpilot
                }
            }
            entity.setInternal(nGunners, Infantry.LOC_INFANTRY);
        }

        if (entity instanceof Tank) {
            if (nDrivers == 0 && nGunners == 0) {
                //nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            }
            if (nDrivers == 0) {
                ((Tank) entity).setDriverHit(true);
            } else {
                ((Tank) entity).setDriverHit(false);
            }
        } else if (entity instanceof Infantry) {
            if (nDrivers == 0 && nGunners == 0) {
                //nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            }
        }
        //TODO: For the moment we need to max these out at 8 so people don't get errors
        //when they customize in MM but we should put an option in MM to ignore those limits
        //and set it to true when we start up through MHQ
        entity.getCrew().setPiloting(Math.min(Math.max(piloting, 0), 8), 0);
        entity.getCrew().setGunnery(Math.min(Math.max(gunnery, 0), 7), 0);
        entity.getCrew().setArtillery(Math.min(Math.max(artillery, 0), 8), 0);
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            //Use tacops crew hits calculations and current size versus maximum size
            entity.getCrew().setCurrentSize(nCrew + nGunners + nDrivers);
            entity.getCrew().setSize(Compute.getFullCrewSize(entity));
            entity.getCrew().setHits(entity.getCrew().calculateHits(),0);
        } else if (entity instanceof Infantry || usesSoloPilot()) {
            //Set the crew size based on gunners, since all personnel are both gunners and drivers
            entity.getCrew().setSize(nGunners);
        } else {
            //Crew size should be the total of the 3 types of crewmembers
            entity.getCrew().setSize(nCrew + nGunners + nDrivers);
        }
        entity.getCrew().setMissing(false, 0);
    }

    /**
     * LAMs require a pilot that is cross-trained for mechs and fighters
     */
    private void refreshLAMPilot() {
        Person pilot = getCommander();
        if (null == pilot) {
            entity.getCrew().setMissing(true, 0);
            entity.getCrew().setSize(0);
            return;
        }

        int pilotingMech = 13;
        int gunneryMech = 13;
        int pilotingAero = 13;
        int gunneryAero = 13;
        int artillery = 13;

        if (pilot.hasSkill(SkillType.S_PILOT_MECH)) {
            pilotingMech = pilot.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue();
        }
        if (pilot.hasSkill(SkillType.S_GUN_MECH)) {
            gunneryMech = pilot.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue();
        }
        if (pilot.hasSkill(SkillType.S_PILOT_AERO)) {
            pilotingAero = pilot.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue();
        }
        if (pilot.hasSkill(SkillType.S_GUN_AERO)) {
            gunneryAero = pilot.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue();
        }
        if (pilot.hasSkill(SkillType.S_ARTILLERY)) {
            artillery = pilot.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue();
        }

        if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
            pilotingMech += pilot.getPilotingInjuryMod();
            gunneryMech += pilot.getGunneryInjuryMod();
            pilotingAero += pilot.getPilotingInjuryMod();
            gunneryAero += pilot.getGunneryInjuryMod();
            artillery += pilot.getGunneryInjuryMod();
        }
        LAMPilot crew = (LAMPilot) entity.getCrew();
        crew.setPiloting(Math.min(Math.max(pilotingMech, 0), 8));
        crew.setGunnery(Math.min(Math.max(gunneryMech, 0), 7));
        crew.setPilotingAero(Math.min(Math.max(pilotingAero, 0), 8));
        crew.setGunneryAero(Math.min(Math.max(gunneryAero, 0), 7));
        entity.getCrew().setArtillery(Math.min(Math.max(artillery, 0), 8), 0);
        entity.getCrew().setSize(1);
        entity.getCrew().setMissing(false, 0);
    }

    /**
     * Sets the values of a slot in the entity crew for the indicated person.
     *
     * @param p
     * @param slot
     * @param gunType
     * @param driveType
     */
    private void assignToCrewSlot(Person p, int slot, String gunType, String driveType) {
        entity.getCrew().setName(p.getFullTitle(), slot);
        entity.getCrew().setNickname(p.getCallsign(), slot);
        entity.getCrew().setGender(p.getGender(), slot);
        entity.getCrew().setClanPilot(p.isClanPersonnel(), slot);
        entity.getCrew().setPortrait(p.getPortrait().clone(), slot);
        entity.getCrew().setHits(p.getHits(), slot);
        int gunnery = 7;
        int artillery = 7;
        int piloting = 8;
        if (p.hasSkill(gunType)) {
            gunnery = p.getSkill(gunType).getFinalSkillValue();
        }
        if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
            gunnery += p.getGunneryInjuryMod();
        }
        if (p.hasSkill(driveType)) {
            piloting = p.getSkill(driveType).getFinalSkillValue();
        }
        if (p.hasSkill(SkillType.S_ARTILLERY)
                && p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue() < artillery) {
            artillery = p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue();
        }
        entity.getCrew().setPiloting(Math.min(Math.max(piloting, 0), 8), slot);
        entity.getCrew().setGunnery(Math.min(Math.max(gunnery, 0), 7), slot);
        // also set RPG gunnery skills in case present in game options
        entity.getCrew().setGunneryL(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setGunneryM(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setGunneryB(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setArtillery(Math.min(Math.max(artillery, 0), 7), slot);
        entity.getCrew().setToughness(p.getToughness(), slot);
        entity.getCrew().setExternalIdAsString(p.getId().toString(), slot);
        entity.getCrew().setMissing(false, slot);
    }

    public void resetEngineer() {
        if (!isSelfCrewed()) {
            return;
        }
        int minutesLeft = TECH_WORK_DAY;
        int overtimeLeft = TECH_WORK_DAY / 2;
        boolean breakpartreroll = true;
        boolean failrefitreroll = true;
        if (null != engineer) {
            minutesLeft = engineer.getMinutesLeft();
            overtimeLeft = engineer.getOvertimeLeft();
        } else {
            // then get the number based on the least amount available to crew members
            // in the case of Edge, everyone must have the same triggers set for Edge to work
            for (Person p : getActiveCrew()) {
                if (p.getMinutesLeft() < minutesLeft) {
                    minutesLeft = p.getMinutesLeft();
                }

                if (p.getOvertimeLeft() < overtimeLeft) {
                    overtimeLeft = p.getOvertimeLeft();
                }
            }
        }

        if (getEntity() instanceof Infantry) {
            if (!isUnmanned()) {
                engineer = new Person(getCommander().getGivenName(), getCommander().getSurname(), getCampaign());
                engineer.setEngineer(true);
                engineer.setClanPersonnel(getCommander().isClanPersonnel());
                engineer.setMinutesLeft(minutesLeft);
                engineer.setOvertimeLeft(overtimeLeft);
                engineer.setId(getCommander().getId());
                engineer.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
                engineer.setRank(getCommander().getRankNumeric());
                // will only be reloading ammo, so doesn't really matter what skill level we give them - set to regular
                engineer.addSkill(SkillType.S_TECH_MECHANIC, SkillType.getType(SkillType.S_TECH_MECHANIC).getRegularLevel(), 0);
            } else {
                engineer = null;
            }
        } else {
            if (!vesselCrew.isEmpty()) {
                int nCrew = 0;
                int sumSkill = 0;
                int sumBonus = 0;
                int sumEdge = 0;
                int sumEdgeUsed = 0;
                String engineerGivenName = "Nobody";
                String engineerSurname = "Nobody";
                int bestRank = Integer.MIN_VALUE;
                for (Person p : vesselCrew) {
                    if (engineer != null) {
                        // If the engineer used edge points, remove some from vessel crewmembers until all is paid for
                        if (engineer.getEdgeUsed() > 0) {
                            // Don't subtract an Edge if the individual has none left
                            if (p.getCurrentEdge() > 0) {
                                p.changeCurrentEdge(-1);
                                engineer.setEdgeUsed(engineer.getEdgeUsed() - 1);
                            }
                        }
                        // If the engineer gained XP, add it for each crewman
                        p.awardXP(getCampaign(), engineer.getXP());

                        // Update each crewman's successful task count too
                        p.setNTasks(p.getNTasks() + engineer.getNTasks());
                        if (p.getNTasks() >= getCampaign().getCampaignOptions().getNTasksXP()) {
                            p.awardXP(getCampaign(), getCampaign().getCampaignOptions().getTaskXP());
                            p.setNTasks(0);
                        }
                        sumEdgeUsed = engineer.getEdgeUsed();
                    }
                    sumEdge += p.getEdge();

                    if (p.hasSkill(SkillType.S_TECH_VESSEL)) {
                        sumSkill += p.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                        sumBonus += p.getSkill(SkillType.S_TECH_VESSEL).getBonus();
                        nCrew++;
                    }
                    if (!(p.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART))) {
                        breakpartreroll = false;
                    }
                    if (!(p.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT))) {
                        failrefitreroll = false;
                    }
                    if (p.getRankNumeric() > bestRank) {
                        engineerGivenName = p.getGivenName();
                        engineerSurname = p.getSurname();
                        bestRank = p.getRankNumeric();
                    }
                }
                if (nCrew > 0) {
                    engineer = new Person(engineerGivenName, engineerSurname, getCampaign());
                    engineer.setEngineer(true);
                    engineer.setClanPersonnel(getCommander().isClanPersonnel());
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_BREAK_PART, breakpartreroll);
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, failrefitreroll);
                    engineer.setMinutesLeft(minutesLeft);
                    engineer.setOvertimeLeft(overtimeLeft);
                    engineer.setId(getCommander().getId());
                    engineer.setPrimaryRoleDirect(PersonnelRole.VESSEL_CREW);
                    if (bestRank > -1) {
                        engineer.setRank(bestRank);
                    }
                    engineer.addSkill(SkillType.S_TECH_VESSEL, sumSkill / nCrew, sumBonus / nCrew);
                    engineer.setEdgeUsed(sumEdgeUsed);
                    engineer.setCurrentEdge((sumEdge - sumEdgeUsed) / nCrew);
                    engineer.setUnit(this);
                } else {
                    engineer = null;
                }
            } else {
                // Needed to fix bug where removed crew doesn't remove engineer
                engineer = null;
            }
        }
        if (null != engineer) {
            // change reference for any scheduled tasks
            for (Part p : getParts()) {
                if (p.isBeingWorkedOn()) {
                    p.setTech(engineer);
                }
            }
        } else {
            // cancel any mothballing if this happens
            if (isMothballing()) {
                mothballTime = 0;
            }
            // cancel any scheduled tasks
            for (Part p : getParts()) {
                if (p.isBeingWorkedOn()) {
                    p.cancelAssignment();
                }
            }
        }
    }

    public int getAeroCrewNeeds() {
        return Compute.getAeroCrewNeeds(entity);
    }

    public int getFullCrewSize() {
        return Compute.getFullCrewSize(entity);
    }

    public int getTotalDriverNeeds() {
        return Compute.getTotalDriverNeeds(entity);
    }

    /**
     * Compute the number of generic space/vehicle crew (e.g. not driver, gunner, or navigator)
     * @return The number of generic crew required
     */
    public int getTotalCrewNeeds() {
        int nav = 0;
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            if (entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                nav = 1;
            }
            return getAeroCrewNeeds() - getTotalDriverNeeds() - nav;
        } else if (entity.isSupportVehicle()) {
            return getFullCrewSize() - getTotalDriverNeeds() - getTotalGunnerNeeds();
        }
        return 0;
    }

    public boolean canTakeMoreDrivers() {
        int nDrivers = drivers.size();
        return nDrivers < getTotalDriverNeeds();
    }

    public boolean canTakeMoreVesselCrew() {
        int nCrew = vesselCrew.size();
        int nav = 0;
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            if (entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                nav = 1;
            }
            return nCrew < (getAeroCrewNeeds() - getTotalDriverNeeds() - nav);
        } else if (entity.isSupportVehicle()) {
            return nCrew < (getFullCrewSize() - getTotalDriverNeeds() - getTotalGunnerNeeds());
        }
        return false;
    }

    public boolean canTakeNavigator() {
        return entity instanceof Jumpship && !(entity instanceof SpaceStation) && (navigator == null);
    }

    public boolean canTakeTechOfficer() {
        return (techOfficer == null) &&
                (entity.getCrew().getCrewType().getTechPos() >= 0
                // Use techOfficer field for secondary commander
                || (entity instanceof Tank && entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)));
    }

    public boolean canTakeTech() {
        return isUnmaintained() && !isSelfCrewed();
    }

    // TODO : Switch similar tables in person to use this one instead
    public String determineUnitTechSkillType() {
        if ((entity instanceof Mech) || (entity instanceof Protomech)) {
            return SkillType.S_TECH_MECH;
        } else if (entity instanceof BattleArmor) {
            return SkillType.S_TECH_BA;
        } else if (entity instanceof Tank) {
            return SkillType.S_TECH_MECHANIC;
        } else if ((entity instanceof Dropship) || (entity instanceof Jumpship)) {
            return SkillType.S_TECH_VESSEL;
        } else if ((entity instanceof Aero)) {
            return SkillType.S_TECH_AERO;
        } else {
            return "";
        }
    }

    public boolean canTakeMoreGunners() {
        int nGunners = gunners.size();
        return nGunners < getTotalGunnerNeeds();
    }

    public int getTotalGunnerNeeds() {
        return Compute.getTotalGunnerNeeds(entity);
    }

    public boolean usesSoloPilot() {
        //return Compute.getFullCrewSize(entity) == 1;
        //Taharqa: I dont think we should do it based on computed size, but whether the unit logically
        //is the type of unit that has only one pilot. This is partly because there may be some vees
        //that only have one pilot and this is also a problem for BA units with only one active suit
        return ((entity instanceof Mech)
                || (entity instanceof Protomech)
                || (entity instanceof Aero && !(entity instanceof SmallCraft) && !(entity instanceof Jumpship)))
                && (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos());
    }

    public boolean usesSoldiers() {
        return entity instanceof Infantry;
    }

    public void addDriver(Person p) {
        addDriver(p, false);
    }

    public void addDriver(Person p, boolean useTransfers) {
        Objects.requireNonNull(p);

        ensurePersonIsRegistered(p);
        drivers.add(p);
        p.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(p, getCampaign().getLocalDate(), getName());
        } else {
            ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void addGunner(Person p) {
        addGunner(p, false);
    }

    public void addGunner(Person p, boolean useTransfers) {
        Objects.requireNonNull(p);

        ensurePersonIsRegistered(p);
        gunners.add(p);
        p.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(p, getCampaign().getLocalDate(), getName());
        } else {
            ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void addVesselCrew(Person p) {
        addVesselCrew(p, false);
    }

    public void addVesselCrew(Person p, boolean useTransfers) {
        Objects.requireNonNull(p);

        ensurePersonIsRegistered(p);
        vesselCrew.add(p);
        p.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(p, getCampaign().getLocalDate(), getName());
        } else {
            ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void setNavigator(Person p) {
        setNavigator(p, false);
    }

    public void setNavigator(Person p, boolean useTransfers) {
        Objects.requireNonNull(p);

        ensurePersonIsRegistered(p);
        navigator = p;
        p.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(p, getCampaign().getLocalDate(), getName());
        } else {
            ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public boolean isTechOfficer(@Nullable Person p) {
        return (techOfficer != null) && techOfficer.equals(p);
    }

    public void setTechOfficer(Person p) {
        setTechOfficer(p, false);
    }

    public void setTechOfficer(Person p, boolean useTransfers) {
        Objects.requireNonNull(p);

        ensurePersonIsRegistered(p);
        techOfficer = p;
        p.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(p, getCampaign().getLocalDate(), getName());
        } else {
            ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void setTech(Person p) {
        Objects.requireNonNull(p);

        if (null != tech) {
            LogManager.getLogger().warn(String.format("New tech assigned %s without removing previous tech %s", p.getFullName(), tech));
        }
        ensurePersonIsRegistered(p);
        tech = p;
        p.addTechUnit(this);
        ServiceLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
        MekHQ.triggerEvent(new PersonTechAssignmentEvent(p, this));
    }

    public void removeTech() {
        if (tech != null) {
            Person originalTech = tech;
            tech.removeTechUnit(this);
            tech = null;
            MekHQ.triggerEvent(new PersonTechAssignmentEvent(originalTech, null));
        }
    }

    private void ensurePersonIsRegistered(final Person person) {
        Objects.requireNonNull(person);
        if (getCampaign().getPerson(person.getId()) == null) {
            getCampaign().recruitPerson(person, person.getPrisonerStatus(), true,  false);
            LogManager.getLogger().warn(String.format("The person %s added this unit %s, was not in the campaign.", person.getFullName(), getName()));
        }
    }

    public void addPilotOrSoldier(final Person person) {
        addPilotOrSoldier(person, false);
    }

    public void addPilotOrSoldier(final Person person, final boolean useTransfers) {
        addPilotOrSoldier(person, null, useTransfers);
    }

    public void addPilotOrSoldier(final Person person, final @Nullable Unit oldUnit,
                                  final boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        drivers.add(person);
        // Multi-crew cockpits should not set the pilot to the gunner position
        if (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos()) {
            gunners.add(person);
        }
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            ServiceLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
            ServiceLogger.reassignedTOEForce(getCampaign(), person, getCampaign().getLocalDate(),
                    getCampaign().getForceFor(oldUnit), getCampaign().getForceFor(this));
        } else {
            ServiceLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
            ServiceLogger.addedToTOEForce(getCampaign(), person, getCampaign().getLocalDate(),
                    getCampaign().getForceFor(this));
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(person, this));
    }

    /**
     * @param person the person to remove. If this is null we return immediately without parsing.
     * @param log whether to log the removal
     */
    public void remove(final @Nullable Person person, final boolean log) {
        if (person == null) {
            return;
        }

        ensurePersonIsRegistered(person);
        if (person.equals(tech)) {
            removeTech();
        } else {
            person.setUnit(null);
            drivers.remove(person);
            gunners.remove(person);
            vesselCrew.remove(person);
            if (person.equals(navigator)) {
                navigator = null;
            }

            if (person.equals(techOfficer)) {
                techOfficer = null;
            }

            if (person.equals(engineer)) {
                engineer = null;
            }
            resetPilotAndEntity();
            MekHQ.triggerEvent(new PersonCrewAssignmentEvent(person, this));
        }

        if (log) {
            ServiceLogger.removedFrom(person, getCampaign().getLocalDate(), getName());
            ServiceLogger.removedFromTOEForce(getCampaign(), person, getCampaign().getLocalDate(),
                    getCampaign().getForceFor(this));
        }
    }

    public boolean isUnmanned() {
        return (null == getCommander());
    }

    public int getForceId() {
        return forceId;
    }

    public void setForceId(int id) {
        this.forceId = id;
    }

    public int getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(int i) {
        this.scenarioId = i;
    }

    public List<Person> getCrew() {
        final List<Person> crew = new ArrayList<>(drivers);
        if (!usesSoloPilot() && !usesSoldiers()) {
            crew.addAll(gunners);
        }
        crew.addAll(vesselCrew);
        if (navigator != null) {
            crew.add(navigator);
        }

        if (techOfficer != null) {
            crew.add(techOfficer);
        }
        return crew;
    }

    public void clearCrew() {
        if (isDeployed()) {
            return;
        }

        for (final Person person : getCrew()) {
            remove(person, true);
        }

        removeTech();

        if (getEngineer() != null) {
            remove(getEngineer(), true);
        }
    }


    public List<Person> getDrivers() {
        return Collections.unmodifiableList(drivers);
    }

    public List<Person> getGunners() {
        return Collections.unmodifiableList(gunners);
    }

    public List<Person> getVesselCrew() {
        return Collections.unmodifiableList(vesselCrew);
    }

    public @Nullable Person getTechOfficer() {
        return techOfficer;
    }

    public @Nullable Person getNavigator() {
        return navigator;
    }

    public @Nullable Person getTech() {
        return (getEngineer() != null) ? getEngineer() : tech;
    }

    //region Mothballing/Activation
    /**
     * Gets a value indicating whether or not the unit is being mothballed
     * or activated.
     * @return True if the unit is undergoing mothballing or activation,
     *         otherwise false.
     */
    public boolean isMothballing() {
        return mothballTime > 0;
    }

    /**
     * Gets the time (in minutes) remaining to mothball or activate the unit.
     * @return The time (in minutes) remaining to mothball or activate the unit.
     */
    public int getMothballTime() {
        return mothballTime;
    }

    /**
     * Sets the time (in minutes) remaining to mothball or activate the unit.
     * @param t The time (in minutes) remaining to mothball or activate the unit.
     */
    public void setMothballTime(int t) {
        mothballTime = Math.max(t, 0);
    }

    /**
     * Gets a value indicating whether or not this unit is mothballed.
     * @return True if the unit is mothballed, otherwise false.
     */
    public boolean isMothballed() {
        return mothballed;
    }

    /**
     * Sets a value indicating whether or not this unit is mothballed.
     *
     * If the unit is being mothballed, all of its personnel will be removed.
     * If the unit is being activated, all of its personnel will be restored (if applicable)
     * and its maintenance cycle will be reset.
     * @param b True if the unit is now mothballed, or false if the unit is now activated.
     */
    public void setMothballed(boolean b) {
        this.mothballed = b;
        // Tech gets removed either way bug [#488]
        if (null != tech) {
            remove(tech, true);
        }
        if (mothballed) {
            //remove any other personnel
            for (Person p : getCrew()) {
                remove(p, true);
            }
            resetPilotAndEntity();
        } else {
            //start maintenance cycle over again
            resetDaysSinceMaintenance();
        }
    }

    /**
     * Begins mothballing a unit.
     * @param mothballTech The tech performing the mothball.
     */
    public void startMothballing(Person mothballTech) {
        startMothballing(mothballTech, false);
    }

    /**
     * Begins mothballing a unit, optionally as a GM action.
     * @param mothballTech The tech performing the mothball.
     * @param isGM A value indicating if the mothball action should
     *             be performed immediately by the GM.
     */
    public void startMothballing(@Nullable Person mothballTech, boolean isGM) {
        if (!isMothballed() && MekHQ.getMHQOptions().getSaveMothballState()) {
            mothballInfo = new MothballInfo(this);
        }

        //set this person as tech
        if (!isSelfCrewed() && (tech != null) && !tech.equals(mothballTech)) {
            remove(tech, true);
        }
        tech = mothballTech;

        //don't remove personnel yet, because self crewed units need their crews to mothball
        getCampaign().removeUnitFromForce(this);

        //clear any assigned tasks
        for (Part p : getParts()) {
            p.cancelAssignment();
        }

        if (!isGM) {
            setMothballTime(getMothballOrActivationTime());
            getCampaign().mothball(this);
        } else {
            completeMothball();
            getCampaign().addReport(getHyperlinkedName() + " has been mothballed (GM)");
        }
    }

    /**
     * Completes the mothballing of a unit.
     */
    public void completeMothball() {
        if (tech != null) {
            remove(tech, false);
        }

        setMothballTime(0);
        setMothballed(true);
    }

    /**
     * Begins activating a unit which has been mothballed.
     * @param activationTech The tech performing the activation.
     */
    public void startActivating(Person activationTech) {
        startActivating(activationTech, false);
    }

    /**
     * Begins activating a unit which has been mothballed,
     * optionally as a GM action.
     * @param activationTech The tech performing the activation.
     * @param isGM A value indicating if the activation action should
     *             be performed immediately by the GM.
     */
    public void startActivating(@Nullable Person activationTech, boolean isGM) {
        if (!isMothballed()) {
            return;
        }

        // set this person as tech
        if (!isSelfCrewed() && (tech != null) && !tech.equals(activationTech)) {
            remove(tech, true);
        }
        tech = activationTech;

        if (!isGM) {
            setMothballTime(getMothballOrActivationTime());
            getCampaign().activate(this);
        } else {
            completeActivation();
            getCampaign().addReport(getHyperlinkedName() + " has been activated (GM)");
        }
    }

    /**
     * Completes the activation of a unit.
     */
    public void completeActivation() {
        if (getTech() != null) {
            remove(getTech(), false);
        }

        setMothballTime(0);
        setMothballed(false);

        // if we previously mothballed this unit, attempt to restore its pre-mothball state
        if (mothballInfo != null) {
            mothballInfo.restorePreMothballInfo(this, getCampaign());
            mothballInfo = null;
        }
    }

    /**
     * Gets the time required to mothball or activate this unit.
     * @return The time in minutes required to mothball or activate this unit.
     */
    private int getMothballOrActivationTime() {
        //set mothballing time
        if (getEntity() instanceof Infantry) {
            return TECH_WORK_DAY;
        } else if ((getEntity() instanceof Dropship) || (getEntity() instanceof Jumpship)) {
            return TECH_WORK_DAY * (int) Math.ceil(getEntity().getWeight() / 500.0);
        } else if (isMothballed()) {
            return TECH_WORK_DAY;
        } else {
            return TECH_WORK_DAY * 2;
        }
    }

    /**
     * Cancels a pending mothball or activation work order.
     */
    public void cancelMothballOrActivation() {
        if (!isMothballing()) {
            return;
        }

        setMothballTime(0);

        // ...if we were mothballing, restore our crew
        if (!isMothballed()) {
            mothballInfo.restorePreMothballInfo(this, getCampaign());
            mothballInfo = null;
        }

        // reset our mothball status
        setMothballed(isMothballed());
    }
    //endregion Mothballing/Activation

    public List<Person> getActiveCrew() {
        List<Person> crew = new ArrayList<>();
        for (Person p : drivers) {
            if ((p.getHits() > 0) && ((entity instanceof Tank) || (entity instanceof Infantry))) {
                continue;
            }
            crew.add(p);
        }

        if (!usesSoloPilot() && !usesSoldiers()) {
            for (Person p : gunners) {
                if ((p.getHits() > 0) && ((entity instanceof Tank) || (entity instanceof Infantry))) {
                    continue;
                }
                crew.add(p);
            }
        }
        crew.addAll(vesselCrew);
        if (navigator != null) {
            crew.add(navigator);
        }
        if (techOfficer != null) {
            crew.add(techOfficer);
        }
        return crew;
    }

    /**
     * Prototype TSM makes a unit harder to repair and maintain.
     *
     * @return Whether the unit has prototype TSM
     */
    public boolean hasPrototypeTSM() {
        for (Mounted m : getEntity().getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TSM) && m.getType().hasFlag(MiscType.F_PROTOTYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a personnel count for each marine platoon/squad assigned to this unit
     * @return The number of marines aboard
     */
    public int getMarineCount() {
        return 0;
        // TODO: implement Marines
        // int count = 0;
        // for (Person marine : marines) {
        //    count += marine.getGunners().size();
        // }
        // return count;
    }

    public boolean isDriver(@Nullable Person person) {
        return drivers.contains(person);
    }

    public boolean isGunner(@Nullable Person person) {
        return gunners.contains(person);
    }

    /**
     * Checks whether a person is considered the commander of this unit.
     *
     * @param person A <code>Person</code> in the campaign. The person need not be assigned to the unit as
     *               crew, in which case the return value will be false.
     * @return       Whether the person is considered the unit commander. If <code>person</code> is null or
     *               the unit has no crew, this method will return false
     *
     * @see #getCommander()
     */
    public boolean isCommander(@Nullable Person person) {
        Person commander = getCommander();
        return (commander != null) && commander.equals(person);
    }

    public boolean isNavigator(@Nullable Person person) {
        return (navigator != null) && navigator.equals(person);
    }

    public void setRefit(Refit r) {
        refit = r;
    }

    public Refit getRefit() {
        return refit;
    }

    public boolean isRefitting() {
        return null != refit;
    }

    public String getName() {
        return getFluffName().isBlank() ? getEntity().getShortName()
                : getEntity().getShortName() + " - " + getFluffName();
    }

    public String getHyperlinkedName() {
        return "<a href='UNIT:" + getId() + "'>" + entity.getShortName() + "</a>";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Unit other = (Unit) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Person getEngineer() {
        return engineer;
    }

    public @Nullable Part getPartForEquipmentNum(int index, int loc) {
        for (Part p : parts) {
            if (p.isPartForEquipmentNum(index, loc)) {
                return p;
            }
        }
        return null;
    }

    public int getAvailability(int era) {
        //take the highest availability of all parts
        int availability = EquipmentType.RATING_A;
        for (Part p : parts) {
            int newAvailability = p.getAvailability();
            //Taharqa: its not clear whether a unit should really be considered extinct
            //when its parts are extinct as many probably outlive the production of parts
            //it would be better to just use the unit extinction date itself, but given
            //that there are no canon extinction/reintro dates for units, we will use this
            //instead
            if (p.isExtinct(getCampaign().getGameYear(), getCampaign().getFaction().isClan())) {
                newAvailability = EquipmentType.RATING_X;
            }
            if (newAvailability > availability) {
                availability = newAvailability;
            }
        }
        return availability;
    }

    public void setDaysToArrival(int days) {
        daysToArrival = days;
    }

    public int getDaysToArrival() {
        return daysToArrival;
    }

    public boolean checkArrival() {
        if (daysToArrival > 0) {
            daysToArrival--;
            if (daysToArrival == 0) {
                MekHQ.triggerEvent(new UnitArrivedEvent(this));
                return true;
            }
        }
        return false;
    }

    public boolean isPresent() {
        return daysToArrival == 0;
    }

    public int getMaintenanceTime() {
        int retVal = 0;

        if (getEntity() instanceof Mech) {
            switch (getEntity().getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                    retVal = 30;
                    break;
                case EntityWeightClass.WEIGHT_LIGHT:
                    retVal = 45;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    retVal = 60;
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    retVal = 75;
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                default:
                    retVal = 90;
                    break;
            }
        } else if (getEntity() instanceof Protomech) {
            retVal = 20;
        } else if (getEntity() instanceof BattleArmor) {
            retVal = 10;
        } else if (getEntity() instanceof ConvFighter) {
            retVal = 45;
        } else if (getEntity() instanceof SmallCraft && !(getEntity() instanceof Dropship)) {
            retVal = 90;
        } else if (getEntity() instanceof Aero
                && !(getEntity() instanceof Dropship)
                && !(getEntity() instanceof Jumpship)) {
            switch (getEntity().getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    retVal = 45;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    retVal = 60;
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                default:
                    retVal = 75;
                    break;
            }
        } else if (getEntity() instanceof SupportTank) {
            switch (getEntity().getWeightClass()) {
                case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                    retVal = 20;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                    retVal = 35;
                    break;
                case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                default:
                    retVal = 100;
                    break;
            }
        } else if (getEntity() instanceof SupportVTOL) {
            switch (getEntity().getWeightClass()) {
                case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                    retVal = 20;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                    retVal = 35;
                    break;
                case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                default:
                    retVal = 100;
                    break;
            }
        } else if (getEntity() instanceof Tank) {
            switch (getEntity().getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    retVal = 30;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    retVal = 50;
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    retVal = 75;
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                    retVal = 90;
                    break;
                case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                default:
                    retVal = 120;
                    break;
            }
        }

        // default value for retVal is zero, so anything that didn't fall into one of the
        // above classifications is self-maintaining, meaning zero.
        return retVal * getMaintenanceMultiplier();
    }

    public void incrementDaysSinceMaintenance(boolean maintained, int astechs) {
        daysSinceMaintenance++;
        astechDaysMaintained += astechs;
        if (maintained) {
            daysActivelyMaintained++;
        }
    }

    public void resetDaysSinceMaintenance() {
        daysSinceMaintenance = 0;
        daysActivelyMaintained = 0;
        astechDaysMaintained = 0;
    }

    public int getDaysSinceMaintenance() {
        return daysSinceMaintenance;
    }

    //there are no official rules about partial maintenance
    //lets say less than half is +2
    //more than half is +1 penalty
    //also we will take the average rounded down of the number of astechs to figure out
    //shorthanded penalty
    public double getMaintainedPct() {
        return (daysActivelyMaintained / (double) daysSinceMaintenance);
    }

    public boolean isFullyMaintained() {
        return daysActivelyMaintained == daysSinceMaintenance;
    }

    public int getAstechsMaintained() {
        return (int) Math.floor((1.0 * astechDaysMaintained) / daysSinceMaintenance);
    }

    public int getMaintenanceMultiplier() {
        return maintenanceMultiplier;
    }

    public void setMaintenanceMultiplier(int value) {
        maintenanceMultiplier = value;
    }

    public int getQuality() {
        int nParts = 0;
        int sumQuality = 0;
        for (Part p : getParts()) {
            //no rules about this but lets assume missing parts are quality A
            if (p instanceof MissingPart) {
                nParts++;
            } else if (p.needsMaintenance()) {
                nParts++;
                sumQuality += p.getQuality();
            }
        }
        if (nParts == 0) {
            return Part.QUALITY_D;
        }
        return (int) Math.round((1.0 * sumQuality) / nParts);
    }

    public void setQuality(int q) {
        for (Part p : getParts()) {
            if (!(p instanceof MissingPart)) {
                p.setQuality(q);
            }
        }
    }

    public String getQualityName() {
        return Part.getQualityName(getQuality(), getCampaign().getCampaignOptions().isReverseQualityNames());
    }

    public boolean requiresMaintenance() {
        if (!isAvailable()) {
            return false;
        }
        return !(getEntity() instanceof Infantry) || getEntity() instanceof BattleArmor;
    }

    public boolean isUnmaintained() {
        return requiresMaintenance() && (getTech() == null);
    }

    public boolean isSelfCrewed() {
        return (getEntity() instanceof Dropship) || (getEntity() instanceof Jumpship)
                || isConventionalInfantry();
    }

    public boolean isUnderRepair() {
        for (Part p : getParts()) {
            if (null != p.getTech()) {
                return true;
            }
        }
        return false;
    }

    public String getLastMaintenanceReport() {
        return lastMaintenanceReport;
    }

    public void setLastMaintenanceReport(String r) {
        lastMaintenanceReport = r;
    }

    public int getDamageState() {
        return getDamageState(getEntity());
    }

    public static int getDamageState(Entity en) {
        return en.getDamageLevel(false);
    }

    /**
     * Removes all of the parts from a unit.
     *
     * NOTE: this puts the unit in an inconsistent state, and
     *       the unit should not be used until its parts have
     *       been re-assigned.
     *
     */
    public void removeParts() {
        for (Part part : parts) {
            part.setUnit(null);

            if (campaign != null) {
                campaign.getWarehouse().removePart(part);
            }
        }

        parts.clear();
    }

    /**
     * @return the name
     */
    public String getFluffName() {
        return fluffName;
    }

    /**
     * @param fluffName the name to set
     */
    public void setFluffName(String fluffName) {
        this.fluffName = fluffName;
    }

    /**
     * Checks to see if a particular BA suit on BA is currently operable
     * This requires the suit to not be destroyed and to have not missing equipment parts
     */
    public boolean isBattleArmorSuitOperable(int trooper) {
        if (!(getEntity() instanceof BattleArmor)) {
            return false;
        }
        if (getEntity().getInternal(trooper) < 0) {
            return false;
        }
        for (Part part : getParts()) {
            if (part instanceof MissingBattleArmorEquipmentPart &&
                    ((MissingBattleArmorEquipmentPart) part).getTrooper() == trooper) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if the unit is conventional infantry, otherwise false
     */
    public boolean isConventionalInfantry() {
        return (getEntity() != null) && getEntity().isConventionalInfantry();
    }

    public boolean isIntroducedBy(int year) {
        return null != entity && entity.getYear() <= year;
    }

    public boolean isExtinctIn(int year) {
        // TODO: currently we do not track this in MM (and I don't think it really exists,
        // but I am adding the code elsewhere to take advantage of this method if we do code it.
        return false;
    }

    @Override
    public String toString() {
        String entName = "None";
        if (getEntity() != null) {
            entName = getEntity().getDisplayName();
        }
        return "Unit for Entity: " + entName;
    }

    public String displayMonthlyCost() {
        return "<b>Spare Parts</b>: " + getSparePartsCost().toAmountAndSymbolString() + "<br>"
                + "<b>Ammunition</b>: " + getAmmoCost().toAmountAndSymbolString() + "<br>"
                + "<b>Fuel</b>: " + getFuelCost().toAmountAndSymbolString() + "<br>";
    }

    public Money getSparePartsCost() {
        if (isMothballed()) {
            return Money.zero();
        }

        Money partsCost = Money.zero();

        if (entity instanceof Jumpship) { // SpaceStation derives from JumpShip
            partsCost = partsCost.plus(entity.getWeight() * 0.0001 * 15000);
        } else if (entity instanceof Aero) {
            partsCost = partsCost.plus(entity.getWeight() * 0.001 * 15000);
        } else if (entity instanceof Tank) {
            partsCost = partsCost.plus(entity.getWeight() * 0.001 * 8000);
        } else if ((entity instanceof Mech) || (entity instanceof BattleArmor)) {
            partsCost = partsCost.plus(entity.getWeight() * 0.001 * 10000);
        } else if (entity instanceof Infantry) {
            if (((Infantry) entity).isMechanized()) {
                partsCost = partsCost.plus(entity.getWeight() * 0.001 * 10000);
            } else if (entity.getMovementMode().isLegInfantry()) {
                partsCost = partsCost.plus(3 * 0.002 * 10000);
            } else if (entity.getMovementMode().isJumpInfantry()) {
                partsCost = partsCost.plus(4 * 0.002 * 10000);
            } else if (entity.getMovementMode().isMotorizedInfantry()) {
                partsCost = partsCost.plus(6 * 0.002 * 10000);
            } else {
                partsCost = partsCost.plus(entity.getWeight() * 0.002 * 10000);
                LogManager.getLogger().error(getName() + " is not a generic CI. Movement mode is " + entity.getMovementModeAsString());
            }
        } else {
            // Only ProtoMechs should fall here. Anything else needs to be logged
            if (!(entity instanceof Protomech)) {
                LogManager.getLogger().error(getName() + " has no Spare Parts value for unit type " + Entity.getEntityTypeName(entity.getEntityType()));
            }
        }

        // Handle cost for quirks if used
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)) {
            partsCost = partsCost.multipliedBy(0.8);
        } else if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
            partsCost = partsCost.multipliedBy(1.25);
        } else if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_NON_STANDARD)) {
            partsCost = partsCost.multipliedBy(2.0);
        } else if (entity.hasQuirk(OptionsConstants.QUIRK_POS_UBIQUITOUS_IS)) {
            partsCost = partsCost.multipliedBy(0.75);
        }
        // TODO Obsolete quirk

        // Now for extended parts cost modifiers
        if (getCampaign().getCampaignOptions().isUseExtendedPartsModifier()) {
            Engine engine = entity.getEngine();
            int currentYear = getCampaign().getGameYear();
            int rating = getTechRating();
            if (((currentYear > 2859) && (currentYear < 3040))
                    && (!getCampaign().getFaction().isClan() && !getCampaign().getFaction().isComStar())) {
                if (rating > EquipmentType.RATING_D) {
                    partsCost = partsCost.multipliedBy(5.0);
                }
            }

            if (rating == EquipmentType.RATING_E) {
                partsCost = partsCost.multipliedBy(1.1);
            } else if (rating == EquipmentType.RATING_F) {
                partsCost = partsCost.multipliedBy(1.25);
            }

            if ((entity instanceof Tank) && (engine.getEngineType() == Engine.NORMAL_ENGINE)) {
                partsCost = partsCost.multipliedBy(2.0);
            }

            if (!(entity instanceof Infantry)) {
                if ((engine.getEngineType() == Engine.XL_ENGINE)
                        || (engine.getEngineType() == Engine.XXL_ENGINE)) {
                    partsCost = partsCost.multipliedBy(2.5);
                } else if (engine.getEngineType() == Engine.LIGHT_ENGINE) {
                    partsCost = partsCost.multipliedBy(1.5);
                }
            }

            if (entity.isClan()) {
                if ((currentYear > 3048) && (currentYear < 3071)) {
                    partsCost = partsCost.multipliedBy(5.0);
                } else if (currentYear > 3070) {
                    partsCost = partsCost.multipliedBy(4.0);
                }
            }
        }

        return partsCost;
    }

    public Money getAmmoCost() {
        Money ammoCost = Money.zero();

        for (Part p : getParts()) {
            if (p instanceof EquipmentPart && ((EquipmentPart) p).getType() instanceof AmmoType) {
                ammoCost = ammoCost.plus(p.getStickerPrice());
            }
        }

        return ammoCost.multipliedBy(0.25);
    }

    public Money getFuelCost() {
        Money fuelCost = Money.zero();

        if ((entity instanceof Warship) || (entity instanceof SmallCraft) ) {
            fuelCost = fuelCost.plus(getTonsBurnDay(entity));
        } else if (entity instanceof Jumpship) {
            fuelCost = fuelCost.plus(getTonsBurnDay(entity));// * 3 * 15000;
        } else if (entity instanceof ConvFighter) {
            fuelCost = fuelCost.plus(getFighterFuelCost(entity));
        } else if (entity instanceof megamek.common.Aero) {
            fuelCost = fuelCost.plus(((Aero) entity).getFuelTonnage() * 4.0 * 15000.0);
        } else if ((entity instanceof Tank) || (entity instanceof Mech)) {
            fuelCost = fuelCost.plus(getVehicleFuelCost(entity));
        } else if (entity instanceof Infantry) {
            fuelCost = fuelCost.plus(getInfantryFuelCost(entity));
        }

        return fuelCost;
    }

    public double getTonsBurnDay(Entity e) {
        double tonsburnday = 0;
        if (e instanceof Dropship) {
            if (((SmallCraft) e).getDesignType() != Dropship.MILITARY) {
                if (e.getWeight() < 1000) {
                    tonsburnday = 1.84;
                } else if (e.getWeight() < 4000) {
                    tonsburnday = 2.82;
                } else if (e.getWeight() < 9000) {
                    tonsburnday = 3.37;
                } else if (e.getWeight() < 20000) {
                    tonsburnday = 4.22;
                } else if (e.getWeight() < 30000) {
                    tonsburnday = 6.52;
                } else if (e.getWeight() < 40000) {
                    tonsburnday = 7.71;
                } else if (e.getWeight() < 50000) {
                    tonsburnday = 7.74;
                } else if (e.getWeight() < 70000) {
                    tonsburnday = 8.37;
                } else {
                    tonsburnday = 8.83;
                }
            } else {
                tonsburnday = 1.84;
            }
            return  (tonsburnday * 15 * 15000);
        } else if ((e instanceof SmallCraft)) {
            return (1.84 * 15 * 15000);
        } else if (e instanceof megamek.common.Jumpship) {
            if (e.getWeight() < 50000) {
                tonsburnday = 2.82;
            } else if (e.getWeight() < 100000) {
                tonsburnday = 9.77;
            } else if (e.getWeight() < 200000) {
                tonsburnday = 19.75;
            } else {
                tonsburnday = 39.52;
            }
            if (e instanceof megamek.common.Warship) {
                return (tonsburnday * 15 * 15000);
            }
            return (tonsburnday * 3 * 15000);
        }
        return tonsburnday;
    }

    public Money getFighterFuelCost(Entity e) {
        Engine en = e.getEngine();
        if (en.isFusion()) {
            return Money.of(((Aero) e).getFuelTonnage() * 4.0 * 15000.0);
        } else {
            return Money.of(((Aero) e).getFuelTonnage() * 4.0 * 1000.0);
        }
    }

    public Money getVehicleFuelCost(Entity e) {
        Engine en = e.getEngine();
        if (e instanceof SupportTank) {
            if (en.getEngineType() == Engine.FUEL_CELL) {
                return Money.of(((SupportTank) e).getFuelTonnage() * 15000.0 * 4.0);
            } else if (en.getEngineType() == Engine.COMBUSTION_ENGINE) {
                return Money.of(((SupportTank) e).getFuelTonnage() * 1000.0 * 4.0);
            } else {
                return Money.zero();
            }
        } else {
            if (en.getEngineType() == Engine.FUEL_CELL) {
                return Money.of(en.getWeightEngine(e) * 0.1 * 15000.0 * 4.0);
            } else if (en.getEngineType() == Engine.COMBUSTION_ENGINE) {
                return Money.of(en.getWeightEngine(e) * 0.1 * 1000.0 * 4.0);
            } else {
                return Money.zero();
            }
        }
    }

    public Money getInfantryFuelCost(Entity e) {
        if (e instanceof BattleArmor) {
            if (e.getJumpMP() > 0) {
                return Money.of(e.getWeight() * 0.02 * 1000.0 * 4.0);
            } else {
                return Money.zero();
            }
        }
        if (e.getMovementMode() == EntityMovementMode.INF_LEG) {
            return Money.zero();
        } else {
            return Money.of(e.getWeight() * 0.02 * 1000.0 * 4.0);
        }
    }

    /**
     * @return Tech progression data for this unit, using the campaign faction if the useFactionIntroDate
     *         option is enabled.
     */
    private ITechnology getTechProgression() {
        return getTechProgression(getCampaign().getTechFaction());
    }

    private ITechnology getTechProgression(int techFaction) {
        // If useFactionIntroDate is false, use the base data that was calculated for the Entity when is was loaded.
        if (techFaction < 0) {
            return getEntity();
        }
        // First check whether it has already been calculated for this faction, but don't wait if
        // it hasn't.
        ITechnology techProgression = UnitTechProgression.getProgression(this, techFaction, false);
        if (null != techProgression) {
            return techProgression;
        } else {
            return getEntity().factionTechLevel(techFaction);
        }
    }

    @Override
    public boolean isClan() {
        return getTechProgression().isClan();
    }

    @Override
    public boolean isMixedTech() {
        return getTechProgression().isMixedTech();
    }

    @Override
    public int getTechBase() {
        return getTechProgression().getTechBase();
    }

    @Override
    public int getIntroductionDate() {
        return getTechProgression().getIntroductionDate();
    }

    @Override
    public int getPrototypeDate() {
        return getTechProgression().getPrototypeDate();
    }

    @Override
    public int getProductionDate() {
        return getTechProgression().getProductionDate();
    }

    @Override
    public int getCommonDate() {
        return getTechProgression().getCommonDate();
    }

    @Override
    public int getExtinctionDate() {
        return getTechProgression().getExtinctionDate();
    }

    @Override
    public int getReintroductionDate() {
        return getTechProgression().getReintroductionDate();
    }

    @Override
    public int getTechRating() {
        return getTechProgression().getTechRating();
    }

    @Override
    public int getBaseAvailability(int era) {
        return getTechProgression().getBaseAvailability(era);
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return getTechProgression(faction).getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return getTechProgression(faction).getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return getTechProgression(faction).getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return getTechProgression(faction).getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return getTechProgression(faction).getReintroductionDate(clan, faction);
    }

    public SimpleTechLevel getSimpleTechLevel() {
        if (getCampaign().useVariableTechLevel()) {
            return getSimpleLevel(getCampaign().getGameYear());
        } else {
            return getStaticTechLevel();
        }
    }

    public SimpleTechLevel getSimpleTechLevel(int year) {
        if (getCampaign().useVariableTechLevel()) {
            return getSimpleLevel(year);
        } else {
            return getStaticTechLevel();
        }
    }

    public SimpleTechLevel getSimpleTechLevel(int year, boolean clan, int faction) {
        if (getCampaign().useVariableTechLevel()) {
            return getSimpleLevel(year, clan, faction);
        } else {
            return getStaticTechLevel();
        }
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return getEntity().getStaticTechLevel();
    }

    @Override
    public int calcYearAvailability(int year, boolean clan, int faction) {
        return getTechProgression(faction).calcYearAvailability(year, clan);
    }

    /**
     * Represents an unresolved reference to a Unit from a Unit.
     */
    public static class UnitRef extends Unit {
        public UnitRef(UUID id) {
            setId(id);
        }
    }

    /**
     * Represents an unresolved reference to a Person from a Unit.
     */
    public static class UnitPersonRef extends Person {
        public UnitPersonRef(UUID id) {
            super(id);
        }
    }

    public void fixReferences(Campaign campaign) {
        if (tech instanceof UnitPersonRef) {
            UUID id = tech.getId();
            tech = campaign.getPerson(id);
            if (tech == null) {
                LogManager.getLogger().error(
                    String.format("Unit %s ('%s') references missing tech %s",
                        getId(), getName(), id));
            }
        }
        for (int ii = drivers.size() - 1; ii >= 0; --ii) {
            Person driver = drivers.get(ii);
            if (driver instanceof UnitPersonRef) {
                drivers.set(ii, campaign.getPerson(driver.getId()));
                if (drivers.get(ii) == null) {
                    LogManager.getLogger().error(
                        String.format("Unit %s ('%s') references missing driver %s",
                            getId(), getName(), driver.getId()));
                    drivers.remove(ii);
                }
            }
        }
        for (int ii = gunners.size() - 1; ii >= 0; --ii) {
            Person gunner = gunners.get(ii);
            if (gunner instanceof UnitPersonRef) {
                gunners.set(ii, campaign.getPerson(gunner.getId()));
                if (gunners.get(ii) == null) {
                    LogManager.getLogger().error(
                        String.format("Unit %s ('%s') references missing gunner %s",
                            getId(), getName(), gunner.getId()));
                    gunners.remove(ii);
                }
            }
        }
        for (int ii = vesselCrew.size() - 1; ii >= 0; --ii) {
            Person crew = vesselCrew.get(ii);
            if (crew instanceof UnitPersonRef) {
                vesselCrew.set(ii, campaign.getPerson(crew.getId()));
                if (vesselCrew.get(ii) == null) {
                    LogManager.getLogger().error(
                        String.format("Unit %s ('%s') references missing vessel crew %s",
                            getId(), getName(), crew.getId()));
                    vesselCrew.remove(ii);
                }
            }
        }

        if (engineer instanceof UnitPersonRef) {
            UUID id = engineer.getId();
            engineer = campaign.getPerson(id);
            if (engineer == null) {
                LogManager.getLogger().error(
                    String.format("Unit %s ('%s') references missing engineer %s",
                        getId(), getName(), id));
            }
        }

        if (navigator instanceof UnitPersonRef) {
            UUID id = navigator.getId();
            navigator = campaign.getPerson(id);
            if (navigator == null) {
                LogManager.getLogger().error(
                    String.format("Unit %s ('%s') references missing navigator %s",
                        getId(), getName(), id));
            }
        }

        if (getTechOfficer() instanceof UnitPersonRef) {
            final UUID id = getTechOfficer().getId();
            techOfficer = campaign.getPerson(id);
            if (getTechOfficer() == null) {
                LogManager.getLogger().error(
                        String.format("Unit %s ('%s') references missing tech officer %s",
                                getId(), getName(), id));
            }
        }

        if (mothballInfo != null) {
            mothballInfo.fixReferences(campaign);
        }

        if ((transportShipAssignment != null)
                && (transportShipAssignment.getTransportShip() instanceof UnitRef)) {
            Unit transportShip = campaign.getHangar().getUnit(transportShipAssignment.getTransportShip().getId());
            if (transportShip != null) {
                transportShipAssignment =
                        new TransportShipAssignment(transportShip, transportShipAssignment.getBayNumber());
            } else {
                LogManager.getLogger().error(
                    String.format("Unit %s ('%s') references missing transport ship %s",
                        getId(), getName(), transportShipAssignment.getTransportShip().getId()));

                transportShipAssignment = null;
            }
        }

        if (!transportedUnits.isEmpty()) {
            Set<Unit> newTransportedUnits = new HashSet<>();
            for (Unit transportedUnit : transportedUnits) {
                if (transportedUnit instanceof UnitRef) {
                    Unit realUnit = campaign.getHangar().getUnit(transportedUnit.getId());
                    if (realUnit != null) {
                        newTransportedUnits.add(realUnit);
                    } else {
                        LogManager.getLogger().error(
                            String.format("Unit %s ('%s') references missing transported unit %s",
                                getId(), getName(), transportedUnit.getId()));
                    }
                } else {
                    newTransportedUnits.add(transportedUnit);
                }
            }
            transportedUnits = newTransportedUnits;
        }
    }
}
