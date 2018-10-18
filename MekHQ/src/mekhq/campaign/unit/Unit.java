/*
 * Unit.java
 *
 * Copyright (C) 2016 MegaMek team
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

package mekhq.campaign.unit;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.BayType;
import megamek.common.CargoBay;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.HeavyVehicleBay;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.IPlayer;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.InsulatedCargoBay;
import megamek.common.Jumpship;
import megamek.common.LAMPilot;
import megamek.common.LandAirMech;
import megamek.common.LightVehicleBay;
import megamek.common.LiquidCargoBay;
import megamek.common.LivestockCargoBay;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.PillionSeatCargoBay;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.ProtomechBay;
import megamek.common.QuadMech;
import megamek.common.QuadVee;
import megamek.common.RefrigeratedCargoBay;
import megamek.common.SimpleTechLevel;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.SpaceStation;
import megamek.common.StandardSeatCargoBay;
import megamek.common.SupportTank;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonCrewAssignmentEvent;
import mekhq.campaign.event.PersonTechAssignmentEvent;
import mekhq.campaign.event.UnitArrivedEvent;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AeroLifeSupport;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.BayDoor;
import mekhq.campaign.parts.Cubicle;
import mekhq.campaign.parts.DropshipDockingCollar;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.FireControlSystem;
import mekhq.campaign.parts.InfantryArmorPart;
import mekhq.campaign.parts.InfantryMotiveType;
import mekhq.campaign.parts.LandingGear;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.MissingAeroHeatSink;
import mekhq.campaign.parts.MissingAeroLifeSupport;
import mekhq.campaign.parts.MissingAeroSensor;
import mekhq.campaign.parts.MissingAvionics;
import mekhq.campaign.parts.MissingBattleArmorSuit;
import mekhq.campaign.parts.MissingBayDoor;
import mekhq.campaign.parts.MissingCubicle;
import mekhq.campaign.parts.MissingDropshipDockingCollar;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingFireControlSystem;
import mekhq.campaign.parts.MissingLandingGear;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingMekCockpit;
import mekhq.campaign.parts.MissingMekGyro;
import mekhq.campaign.parts.MissingMekLifeSupport;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.MissingMekSensor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.MissingProtomekArmActuator;
import mekhq.campaign.parts.MissingProtomekJumpJet;
import mekhq.campaign.parts.MissingProtomekLegActuator;
import mekhq.campaign.parts.MissingProtomekLocation;
import mekhq.campaign.parts.MissingProtomekSensor;
import mekhq.campaign.parts.MissingQuadVeeGear;
import mekhq.campaign.parts.MissingRotor;
import mekhq.campaign.parts.MissingSpacecraftEngine;
import mekhq.campaign.parts.MissingThrusters;
import mekhq.campaign.parts.MissingTurret;
import mekhq.campaign.parts.MissingVeeSensor;
import mekhq.campaign.parts.MissingVeeStabiliser;
import mekhq.campaign.parts.MotiveSystem;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.ProtomekArmActuator;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.ProtomekJumpJet;
import mekhq.campaign.parts.ProtomekLegActuator;
import mekhq.campaign.parts.ProtomekLocation;
import mekhq.campaign.parts.ProtomekSensor;
import mekhq.campaign.parts.QuadVeeGear;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.Rotor;
import mekhq.campaign.parts.SpacecraftEngine;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.Thrusters;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.Turret;
import mekhq.campaign.parts.TurretLock;
import mekhq.campaign.parts.VeeSensor;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.BattleArmorAmmoBin;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.InfantryWeaponPart;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingBattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingHeatSink;
import mekhq.campaign.parts.equipment.MissingJumpJet;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;

/**
 * This is a wrapper class for entity, so that we can add some functionality to
 * it
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Unit implements MekHqXmlSerializable, ITechnology {
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
    private int oldId;
    private String fluffName = "";

    //assignments
    private int forceId;
    protected int scenarioId;

    private ArrayList<UUID> drivers;
    private ArrayList<UUID> gunners;
    private ArrayList<UUID> vesselCrew;
    //this is the id of the tech officer in a superheavy tripod
    private UUID techOfficer;
    private UUID navigator;
    //this is the id of the tech assigned for maintenance if any
    private UUID tech;

    //mothballing variables - if mothball time is not zero then mothballing/activating is in progress
    private int mothballTime;
    private boolean mothballed;

    private int daysSinceMaintenance;
    private int daysActivelyMaintained;
    private int astechDaysMaintained;

    //old ids for reverse compatability
    private ArrayList<Integer> oldDrivers;
    private ArrayList<Integer> oldGunners;
    private ArrayList<Integer> oldVesselCrew;
    private Integer oldNavigator;

    public Campaign campaign;

    private ArrayList<Part> parts;
    private String lastMaintenanceReport;
    private ArrayList<PodSpace> podSpace;

    private Refit refit;

    //a made-up person to handle repairs on Large Craft
    private Person engineer;

    //for backwards compatability with 0.1.8, but otherwise is no longer used
    @SuppressWarnings("unused")
    private int pilotId = -1;

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
            entity.setCamoCategory(null);
            entity.setCamoFileName(null);
        }
        this.site = SITE_BAY;
        this.salvaged = false;
        this.campaign = c;
        this.parts = new ArrayList<Part>();
        this.podSpace = new ArrayList<>();
        this.drivers = new ArrayList<UUID>();
        this.gunners = new ArrayList<UUID>();
        this.vesselCrew = new ArrayList<UUID>();
        this.navigator = null;
        this.tech = null;
        this.mothballTime = 0;
        this.mothballed = false;
        this.oldDrivers = new ArrayList<Integer>();
        this.oldGunners = new ArrayList<Integer>();
        this.oldVesselCrew = new ArrayList<Integer>();
        this.oldNavigator = -1;
        scenarioId = -1;
        this.refit = null;
        this.engineer = null;
        this.history = "";
        this.daysSinceMaintenance = 0;
        this.daysActivelyMaintained = 0;
        this.astechDaysMaintained = 0;
        this.lastMaintenanceReport = null;
        this.fluffName = "";
        reCalc();
    }

    public static String getDamageStateName(int i) {
        switch(i) {
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
        if (ignoreRefit) {
            return isPresent() && !isDeployed() && !isMothballing() && !isMothballed();
        }
        return isPresent() && !isDeployed() && !isRefitting() && !isMothballing() && !isMothballed();
    }

    public String getStatus() {
        if(isMothballing()) {
            if(isMothballed()) {
                return "Activating (" + getMothballTime() + "m)";
            } else {
                return "Mothballing (" + getMothballTime() + "m)";
            }
        }
        if(isMothballed()) {
            return "Mothballed";
        }
        if(isDeployed()) {
            return "Deployed";
        }
        if(!isPresent()) {
            return "In transit (" + getDaysToArrival() + " days)";
        }
        if(isRefitting()) {
            return "Refitting";
        }
        if(!isRepairable()) {
            return "Salvage";
        }
        else if(!isFunctional()) {
            return "Inoperable";
        }
        else {
            return getDamageStateName(getDamageState());
        }
    }


    public void reCalc() {
        // Do nothing.
    }

    public void setEntity(Entity en) {
        //if there is already an entity, then make sure this
        //one gets some of the same things set
        if(null != this.entity) {
            en.setId(this.entity.getId());
            en.duplicateMarker = this.entity.duplicateMarker;
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
            if(cockpitHits > 0) {
                return false;
            }
        }
        if (en instanceof Tank) {
            for (int i = 0; i < en.locations(); i++) {
                if(i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2) {
                    continue;
                }
                if (en.isLocationBad(i)) {
                    return false;
                }
            }
            if(en instanceof VTOL) {
                if(en.getWalkMP() <= 0) {
                    return false;
                }
            }
        }
        if(en instanceof Aero) {
            if(en.getWalkMP() <= 0 && !(en instanceof Jumpship)) {
                return false;
            }
            if(((Aero)en).getSI() <= 0) {
                return false;
            }
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
                if(i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2 || i == Tank.LOC_BODY) {
                    continue;
                }
                if (en.getInternal(i) <= 0) {
                    return false;
                }
            }
        }
        if(en instanceof Aero) {
            if(((Aero)en).getSI() <= 0) {
                return false;
            }
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
     * TODO: This is being called in the PersonnelTableModel after changes to the personnel
     * attached to a unit, but I am not sure it needs to be. I don't think any parts check
     * attached personnel. I think it could b removed, but I am going to leave it for the
     * moment because I have made so many other changes in this version.
     */
    public void runDiagnostic(boolean checkForDestruction) {

        //need to set up an array of part ids to avoid concurrent modification
        //problems because some updateCondition methods will remove the part and put
        //in a new one
        ArrayList<Part> tempParts = new ArrayList<Part>();
        for(Part p : parts) {
            tempParts.add(p);
        }
        for(Part part : tempParts) {
            part.updateConditionFromEntity(checkForDestruction);
        }
    }

    private boolean isPartAvailableForRepairs(IPartWork partWork, boolean onlyNotBeingWorkedOn) {
    	return (!onlyNotBeingWorkedOn || (onlyNotBeingWorkedOn && !partWork.isBeingWorkedOn()));
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
        ArrayList<IPartWork> brokenParts = new ArrayList<IPartWork>();
        for(Part part: parts) {
            if(part.needsFixing() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
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
        ArrayList<IPartWork> salvageParts = new ArrayList<IPartWork>();
        for(Part part: parts) {
            if(part.isSalvaging() && isPartAvailableForRepairs(part, onlyNotBeingWorkedOn)) {
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
        ArrayList<IAcquisitionWork> missingParts = new ArrayList<IAcquisitionWork>();
        if(isSalvage() || !isRepairable()) {
            return missingParts;
        }
        boolean armorFound = false;
        for(Part part: parts) {
            if(part instanceof MissingPart && part.needsFixing() && null == ((MissingPart)part).findReplacement(false)) {
                missingParts.add((MissingPart)part);
            }
            //we need to check for armor as well, but this one is funny because we dont want to
            //check per location really, since armor can be used anywhere. So stop after we reach
            //the first Armor needing replacement
            //TODO: we need to adjust for patchwork armor, which can have different armor types by location
            if(!armorFound && part instanceof Armor) {
                Armor a = (Armor)part;
                if(a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if(!armorFound && part instanceof ProtomekArmor) {
                ProtomekArmor a = (ProtomekArmor)part;
                if(a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if(!armorFound && part instanceof BaArmor) {
                BaArmor a = (BaArmor)part;
                if(a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if(part instanceof AmmoBin && !((AmmoBin)part).isEnoughSpareAmmoAvailable()) {
                missingParts.add((AmmoBin)part);
            }
        }

        return missingParts;
    }

    public long getValueOfAllMissingParts() {
        long value = 0;
        for(Part part : parts) {
            if(part instanceof MissingAmmoBin) {
                AmmoBin newBin = (AmmoBin) ((MissingAmmoBin)part).getNewEquipment();
                value += newBin.getValueNeeded();
            }
            if(part instanceof MissingPart) {
                Part newPart = (Part)((MissingPart)part).getNewEquipment();
                newPart.setBrandNew(!campaign.getCampaignOptions().useBLCSaleValue());
                value += newPart.getActualValue();
            }
            else if(part instanceof AmmoBin) {
                value += ((AmmoBin)part).getValueNeeded();
            }
            else if(part instanceof Armor) {
                value += ((Armor)part).getValueNeeded();
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
        if(isRefitting()) {
            return "unit is being refit";
        }
        if(entity instanceof Tank
                && getActiveCrew().size() < getFullCrewSize()) {
            return "This vehicle requires a crew of " + getFullCrewSize();
        }
        //Taharqa: I am not going to allow BattleArmor units with unmanned suits to deploy. It is
        //possible to hack this to work in MM, but it becomes a serious problem when the unit becomes
        //a total loss because the unmanned suits are also treated as destroyed. I tried hacking something
        //together in ResolveScenarioTracker and decided that it was not right. If someone wants to deploy
        //a non-full strength BA unit, they can salvage the suits that are unmanned and then they can deploy
        //it
        if(entity instanceof BattleArmor) {
            for(int i = BattleArmor.LOC_TROOPER_1; i <= ((BattleArmor)entity).getTroopers(); i++) {
                if(entity.getInternal(i) == 0) {
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
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) && (m.equals(ccs.getMount()) || m.equals(ccs
                                                                                                                      .getMount2()))) {
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
            if(neededHits > usedHits) {
                usedHits += damageSystem(type, equipmentNum, loc, neededHits-usedHits);
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
                if(nhits < hits) {
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
                if(nhits < hits) {
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
                if (TechConstants.getTechName(etype.getTechLevel(year)).equals(
                        "Inner Sphere"))
                    heatSinkIsClanTechBase = false;
                else if (TechConstants.getTechName(etype.getTechLevel(year))
                        .equals("Clan"))
                    heatSinkIsClanTechBase = true;
                break;
            }
        }

        String heatSinkTypeString = heatSinkIsClanTechBase ? "(CL) " : "(IS) ";
        if (heatSinkType == MiscType.F_LASER_HEAT_SINK)
            heatSinkTypeString += "Laser Heat Sink";
        else if (heatSinkType == MiscType.F_DOUBLE_HEAT_SINK)
            heatSinkTypeString += "Double Heat Sink";
        else if (heatSinkType == MiscType.F_HEAT_SINK)
            heatSinkTypeString += "Heat Sink";

        return heatSinkTypeString;
    }

    public long getSellValue() {
        long partsValue = 0;
        for(Part part : parts) {
            partsValue += part.getActualValue() * part.getQuantity();
        }
        //TODO: we need to adjust this for equipment that doesn't show up as parts
        //Spacecraft need: drive unit, computer, and bridge
        if(entity instanceof SmallCraft || entity instanceof Jumpship) {
            //bridge
            partsValue += 200000 + 10 * entity.getWeight();
            //computer
            partsValue += 200000;
            //drive unit
            partsValue += 500 * entity.getOriginalWalkMP() * entity.getWeight()/100;
            // KF Drive, Docking Collars, etc...
            if (entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                Jumpship js = (Jumpship) entity;
                double driveCost = 0;
                // coil
                driveCost += 60000000 + (75000000 * js.getDocks());
                // initiator
                driveCost += 25000000 + (5000000 * js.getDocks());
                // controller
                driveCost += 50000000;
                // tankage
                driveCost += 50000 * js.getKFIntegrity();
                // sail
                driveCost += 50000 * (30 + (js.getWeight() / 7500));
                // charging system
                driveCost += 500000 + (200000 * js.getDocks());
                // compact core
                if (js instanceof Warship) {
                    driveCost *= 5;
                }
                // lithium fusion?
                if (js.hasLF()) {
                    driveCost *= 3;
                }
                // Drive Support Systems
                if (js instanceof Warship) {
                    driveCost += 20000000 * (50 + js.getWeight() / 10000);
                } else {
                    driveCost += 10000000 * (js.getWeight() / 10000);
                }
                partsValue += driveCost;

                // Docking Collars
                partsValue += 100000 * js.getDocks();
                // HPG
                if (js.hasHPG()) {
                    partsValue += 1000000000;
                }

                // fuel tanks
                partsValue += 200 * js.getFuel() / js.getFuelPerTon();

                // armor
                partsValue += js.getArmorWeight(js.locations()) * EquipmentType.getArmorCost(js.getArmorType(0));

                // heat sinks
                int sinkCost = 2000 + 4000 * js.getHeatType();// == HEAT_DOUBLE ? 6000:
                                                           // 2000;
                partsValue += sinkCost * js.getHeatSinks();

                // grav deck
                partsValue += 5000000 * js.getGravDeck();
                partsValue += 10000000 * js.getGravDeckLarge();
                partsValue += 40000000 * js.getGravDeckHuge();

                // get bays
                int baydoors = 0;
                int bayCost = 0;
                for (Bay next : js.getTransportBays()) {
                    baydoors += next.getDoors();
                    if ((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                        bayCost += 20000 * next.getCapacity();
                    }
                    if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                        bayCost += 20000 * next.getCapacity();
                    }
                }

                partsValue += bayCost + baydoors * 1000;

                // life boats and escape pods
                partsValue += 5000 * (js.getLifeBoats() + js.getEscapePods());
            }
        }

        //protomeks: heat sinks are unhittable
        if(entity instanceof Protomech) {
            int sinks = 0;
            for (Mounted mount : entity.getWeaponList()) {
                if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                    WeaponType wtype = (WeaponType) mount.getType();
                    sinks += wtype.getHeat();
                }
            }
            partsValue += 2000 * sinks;
        }

        return (long)(partsValue * getUnitCostMultiplier());
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

    public int getDocks() {
        return getEntity().getDocks();
    }

    public int getLightVehicleCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof LightVehicleBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getHeavyVehicleCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof HeavyVehicleBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getBattleArmorCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof BattleArmorBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getInfantryCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof InfantryBay) {
                bays += b.getCapacity() / ((InfantryBay) b).getPlatoonType().getWeight();
            }
        }
        return bays;
    }

    public int getASFCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof ASFBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getSmallCraftCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof SmallCraftBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getMechCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof MechBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public int getProtomechCapacity() {
        int bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof ProtomechBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    public double getUnitCostMultiplier() {
        double multiplier = 1.0;
        if(!isRepairable()) {
            //if the unit is not repairable, set it as equal to its parts separately
            //this is not RAW, but not really a way to make that work and this makes more sense
            //although we might want to adjust it downward because of the labor cost of salvaging
            return 1.0;
        }
        double tonnage = 100;
        if(entity instanceof Mech && ((Mech)entity).isIndustrial()) {
            tonnage = 400;
        }
        else if(entity instanceof VTOL) {
            tonnage = 30;
        }
        else if(entity instanceof Tank) {
            if(entity.getMovementMode() == EntityMovementMode.WHEELED || entity.getMovementMode() == EntityMovementMode.NAVAL) {
                tonnage = 200;
            }
            else if(entity.getMovementMode() == EntityMovementMode.HOVER || entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
                tonnage = 50;
            }
            else if(entity.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                tonnage = 75;
            }
            else if(entity.getMovementMode() == EntityMovementMode.WIGE) {
                tonnage = 25;
            }
        }
        else if(entity instanceof Dropship) {
            if(((Aero)entity).isSpheroid()) {
                multiplier = 28;
            } else {
                multiplier = 36;
            }
        }
        else if(entity instanceof SmallCraft) {
            tonnage = 50;
        }
        else if(entity instanceof SpaceStation) {
            multiplier = 5;
        }
        else if(entity instanceof Warship) {
            multiplier = 2;
        }
        else if(entity instanceof Jumpship) {
            multiplier = 1.25;
        }
        else if(entity instanceof Aero) {
            tonnage = 200;
        }
        if(!(entity instanceof Infantry) && !(entity instanceof Dropship) && !(entity instanceof Jumpship)) {
            multiplier = 1 + (entity.getWeight() / tonnage);
        }
        if(entity.isOmni()) {
            multiplier *= 1.25;
        }
        return multiplier;
    }

    public long getBuyCost() {
        long cost = (long) Math.round(getEntity().getCost(false));
        if(entity instanceof Infantry) {
            cost = (long) Math.round(getEntity().getAlternateCost());
        }
        if(entity.isClan()) {
            cost *= campaign.getCampaignOptions().getClanPriceModifier();
        }
        return cost;
    }

    public int getDamageState() {
        return getDamageState(getEntity());
    }

    public void writeToXml(PrintWriter pw1, int indentLvl) {
        pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "<unit id=\"" + id.toString()
                + "\" type=\"" + this.getClass().getName() + "\">");

        pw1.println(MekHqXmlUtil.writeEntityToXmlString(entity, indentLvl+1, campaign.getEntities()));
        for(UUID did : drivers) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<driverId>"
                    + did.toString() + "</driverId>");
        }
        for(UUID gid : gunners) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<gunnerId>"
                    + gid.toString() + "</gunnerId>");
        }
        for(UUID vid : vesselCrew) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<vesselCrewId>"
                    + vid.toString() + "</vesselCrewId>");
        }
        if(null != navigator) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                    +"<navigatorId>"
                    +navigator.toString()
                    +"</navigatorId>");
        }
        if(null != techOfficer) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                    +"<techOfficerId>"
                    +techOfficer.toString()
                    +"</techOfficerId>");
        }
        if(null != tech) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                    +"<techId>"
                    +tech.toString()
                    +"</techId>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<salvaged>"
                + salvaged + "</salvaged>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<site>" + site
                + "</site>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<forceId>"
                +forceId
                +"</forceId>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<scenarioId>"
                +scenarioId
                +"</scenarioId>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<daysToArrival>"
                +daysToArrival
                +"</daysToArrival>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<daysSinceMaintenance>"
                +daysSinceMaintenance
                +"</daysSinceMaintenance>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<daysActivelyMaintained>"
                +daysActivelyMaintained
                +"</daysActivelyMaintained>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<astechDaysMaintained>"
                +astechDaysMaintained
                +"</astechDaysMaintained>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<mothballTime>"
                +mothballTime
                +"</mothballTime>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<mothballed>"
                +mothballed
                +"</mothballed>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<fluffName>"
                +MekHqXmlUtil.escape(fluffName)
                +"</fluffName>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<history>"
                +MekHqXmlUtil.escape(history)
                +"</history>");
        if(null != refit) {
            refit.writeToXml(pw1, indentLvl+1);
        }
        if(null != lastMaintenanceReport && campaign.getCampaignOptions().checkMaintenance()) {
            pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                    +"<lastMaintenanceReport><![CDATA["
                    +lastMaintenanceReport
                    +"]]></lastMaintenanceReport>");
        }
        
        if(null != mothballInfo) {
            mothballInfo.writeToXml(pw1, indentLvl);
        }
        
        pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "</unit>");
    }

    public static Unit generateInstanceFromXML(Node wn, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Version)"; //$NON-NLS-1$

        Unit retVal = new Unit();
        NamedNodeMap attrs = wn.getAttributes();
        Node idNode = attrs.getNamedItem("id");
        if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
            retVal.oldId = Integer.parseInt(idNode.getTextContent());
        } else {
            retVal.id = UUID.fromString(idNode.getTextContent());
        }

        // Okay, now load Part-specific fields!
        NodeList nl = wn.getChildNodes();

        try {
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("site")) {
                    retVal.site = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("pilotId")) {
                    retVal.pilotId = Integer.parseInt(wn2.getTextContent());
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
                } else if (wn2.getNodeName().equalsIgnoreCase("driverId")) {
                    if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldDrivers.add(Integer.parseInt(wn2.getTextContent()));
                    } else {
                        retVal.drivers.add(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("gunnerId")) {
                    if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldGunners.add(Integer.parseInt(wn2.getTextContent()));
                    } else {
                        retVal.gunners.add(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("vesselCrewId")) {
                    if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldVesselCrew.add(Integer.parseInt(wn2.getTextContent()));
                    } else {
                        retVal.vesselCrew.add(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("navigatorId")) {
                    if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldNavigator = Integer.parseInt(wn2.getTextContent());
                    } else {
                        if(!wn2.getTextContent().equals("null")) {
                            retVal.navigator = UUID.fromString(wn2.getTextContent());
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("techOfficerId")) {
                    if(!wn2.getTextContent().equals("null")) {
                        retVal.techOfficer = UUID.fromString(wn2.getTextContent());
                    }
                }
                else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    if(!wn2.getTextContent().equals("null")) {
                        retVal.tech = UUID.fromString(wn2.getTextContent());
                    }
                }
                else if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("salvaged")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.salvaged = true;
                    else
                        retVal.salvaged = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballed")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.mothballed = true;
                    else
                        retVal.mothballed = false;
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.entity = MekHqXmlUtil.getEntityFromXmlString(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("refit")) {
                    retVal.refit = Refit.generateInstanceFromXML(wn2, retVal, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("history")) {
                    retVal.history = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("fluffName")) {
                    retVal.fluffName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("lastMaintenanceReport")) {
                    retVal.lastMaintenanceReport = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballInfo")) {
                    retVal.mothballInfo = MothballInfo.generateInstanceFromXML(wn2, version);
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().error(Unit.class, METHOD_NAME, ex);
        }

        if (retVal.id == null) {
            MekHQ.getLogger().log(Unit.class, METHOD_NAME, LogLevel.WARNING,
                    "ID not pre-defined; generating unit's ID."); //$NON-NLS-1$
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
    public String getQuirksList() {
        String quirkString = "";
        boolean first = true;
        if(null != getEntity().getGame() && getEntity().getGame().getOptions().booleanOption("stratops_quirks")) {
            for (Enumeration<IOptionGroup> i = getEntity().getQuirks().getGroups(); i.hasMoreElements();) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                    IOption quirk = j.nextElement();
                    if (quirk.booleanValue()) {
                        if(first) {
                            first = false;
                        } else {
                            quirkString += "\n";
                        }
                        quirkString += quirk.getDisplayableNameWithValue();
                    }
                }
            }
        }
        if(quirkString.equals("")) {
            return null;
        }
        return quirkString;
    }

    public void acquireQuirk(String name, Object value) {
        for (Enumeration<IOption> i = getEntity().getQuirks().getOptions(); i.hasMoreElements();) {
            IOption ability = i.nextElement();
            if(ability.getName().equals(name)) {
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
    public int getMaintenanceCost() {
        return (int)Math.ceil(getWeeklyMaintenanceCost() * (campaign.getCampaignOptions().getMaintenanceCycleDays() / 7.0));
    }

    public int getWeeklyMaintenanceCost() {
        Entity en = getEntity();
        Boolean isOmni = en.isOmni();
        double mCost = 0;
        double value = 0;

        //we will assume sale value for now, but make this customizable
        if(campaign.getCampaignOptions().useEquipmentContractSaleValue()) {
            value += getSellValue();
        } else {
            value += getBuyCost();
        }

        if (campaign.getCampaignOptions().usePercentageMaint()) {
            if(en instanceof Mech) {
                mCost = value * 0.02;
            } else if(en instanceof Warship) {
                mCost = value * 0.07;
            } else if(en instanceof Jumpship) {
                mCost = value * 0.06;
            } else if(en instanceof Dropship) {
                mCost = value * 0.05;
            } else if(en instanceof ConvFighter) {
                mCost = value * 0.03;
            } else if(en instanceof Aero) {
                mCost = value * 0.04;
            } else if(en instanceof VTOL) {
                mCost = value * 0.02;
            } else if(en instanceof Tank) {
                mCost = value * 0.015;
            } else if(en instanceof BattleArmor) {
                mCost = value * 0.03;
            } else if(en instanceof Infantry) {
                mCost = value * 0.005;
            }
            // Mothballed Units cost only 10% to maintain
            if(isMothballed()) {
                mCost *= .1;
            }
        } else {
            if(en instanceof Mech) {
                if(isOmni) {
                    return 100;
                } else {
                    return 75;
                }
            } else if(en instanceof Warship) {
                return 5000;
            } else if(en instanceof Jumpship) {
                return 800;
            } else if(en instanceof Dropship) {
                return 500;
            } else if(en instanceof ConvFighter) {
                return 50;
            } else if(en instanceof Aero) {
                if(isOmni) {
                    return 125;
                } else  {
                    return 65;
                }
            } else if(en instanceof VTOL) {
                return 65;
            } else if(en instanceof Tank) {
                return 25;
            } else if(en instanceof BattleArmor) {
                return ((BattleArmor)en).getTroopers() * 50;
            } else if(en instanceof Infantry) {
                return ((Infantry)en).getSquadN()*10;
            }
        }
        return (int)Math.ceil(mCost/52);
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
        if(!(entity instanceof FighterSquadron) && (null != entity.getEngine())) {
            erating = entity.getEngine().getRating();
        }

        ArrayList<Part> partsToAdd = new ArrayList<Part>();

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
        Hashtable<Integer,Part> equipParts = new Hashtable<Integer,Part>();
        Hashtable<Integer,Part> ammoParts = new Hashtable<Integer,Part>();
        Hashtable<Integer,Part> heatSinks = new Hashtable<Integer,Part>();
        Hashtable<Integer,Part> jumpJets = new Hashtable<Integer,Part>();
        Hashtable<Integer,Part[]> baEquipParts = new Hashtable<Integer, Part[]>();
        Part motiveSystem = null;
        Part avionics = null;
        Part fcs = null;
        Part landingGear = null;
        Part turretLock = null;
        ArrayList<Part> aeroHeatSinks = new ArrayList<Part>();
        int podAeroHeatSinks = 0;
        Part motiveType = null;
        Part primaryW = null;
        Part secondaryW = null;
        Part infantryArmor = null;
        Part dropCollar = null;
        Part protoLeftArmActuator = null;
        Part protoRightArmActuator = null;
        Part protoLegsActuator = null;
        ArrayList<Part> protoJumpJets = new ArrayList<Part>();
        Part aeroThrustersLeft = null;
        Part aeroThrustersRight = null;
        Map<Integer, Part> bays = new HashMap<>();
        Map<Integer, List<Part>> bayPartsToAdd = new HashMap<>();

        for(Part part : parts) {
            if(part instanceof MekGyro || part instanceof MissingMekGyro) {
                gyro = part;
            } else if(part instanceof EnginePart || part instanceof MissingEnginePart) {
                //reverse compatability check, spaceships get different engines
                if(!(entity instanceof SmallCraft || entity instanceof Jumpship)) {
                    engine = part;
                }
            } else if(part instanceof SpacecraftEngine || part instanceof MissingSpacecraftEngine) {
                engine = part;
            } else if(part instanceof MekLifeSupport  || part instanceof MissingMekLifeSupport) {
                lifeSupport = part;
            } else if(part instanceof AeroLifeSupport  || part instanceof MissingAeroLifeSupport) {
                lifeSupport = part;
            } else if(part instanceof MekSensor || part instanceof MissingMekSensor) {
                sensor = part;
            } else if(part instanceof ProtomekSensor || part instanceof MissingProtomekSensor) {
                sensor = part;
            } else if(part instanceof MekCockpit || part instanceof MissingMekCockpit) {
                cockpit = part;
            }  else if(part instanceof VeeSensor || part instanceof MissingVeeSensor) {
                sensor = part;
            }  else if(part instanceof InfantryMotiveType) {
                motiveType = part;
            }  else if(part instanceof InfantryArmorPart) {
                infantryArmor = part;
            }  else if(part instanceof InfantryWeaponPart) {
                if(((InfantryWeaponPart)part).isPrimary()) {
                    primaryW = part;
                } else {
                    secondaryW = part;
                }
            }  else if(part instanceof StructuralIntegrity) {
                structuralIntegrity = part;
            } else if(part instanceof MekLocation) {
                locations[((MekLocation)part).getLoc()] = part;
            } else if(part instanceof MissingMekLocation) {
                locations[part.getLocation()] = part;
            } else if(part instanceof TankLocation) {
                locations[((TankLocation)part).getLoc()] = part;
            } else if(part instanceof Rotor) {
                locations[((Rotor)part).getLoc()] = part;
            } else if(part instanceof MissingRotor) {
                locations[VTOL.LOC_ROTOR] = part;
            } else if(part instanceof Turret) {
                locations[((Turret)part).getLoc()] = part;
            } else if(part instanceof MissingTurret) {
                locations[Tank.LOC_TURRET] = part;
            } else if(part instanceof ProtomekLocation) {
                locations[((ProtomekLocation)part).getLoc()] = part;
            } else if(part instanceof MissingProtomekLocation) {
                locations[((MissingProtomekLocation)part).getLoc()] = part;
            } else if(part instanceof BattleArmorSuit) {
                locations[((BattleArmorSuit)part).getTrooper()] = part;
            } else if(part instanceof MissingBattleArmorSuit) {
                locations[((MissingBattleArmorSuit)part).getTrooper()] = part;
            } else if(part instanceof Armor) {
                if(((Armor)part).isRearMounted()) {
                    armorRear[((Armor)part).getLocation()] = (Armor)part;
                } else {
                    armor[((Armor)part).getLocation()] = (Armor)part;
                }
            } else if(part instanceof ProtomekArmor) {
                armor[((ProtomekArmor)part).getLocation()] = (ProtomekArmor)part;
            } else if(part instanceof BaArmor) {
                armor[((BaArmor)part).getLocation()] = (BaArmor)part;
            } else if(part instanceof VeeStabiliser) {
                stabilisers[((VeeStabiliser)part).getLocation()] = part;
            } else if(part instanceof MissingVeeStabiliser) {
                stabilisers[((MissingVeeStabiliser)part).getLocation()] = part;
            } else if(part instanceof AmmoBin) {
                ammoParts.put(((AmmoBin)part).getEquipmentNum(), part);
            } else if(part instanceof MissingAmmoBin) {
                ammoParts.put(((MissingAmmoBin)part).getEquipmentNum(), part);
            } else if(part instanceof HeatSink) {
                heatSinks.put(((HeatSink)part).getEquipmentNum(), part);
            } else if(part instanceof MissingHeatSink) {
                heatSinks.put(((MissingHeatSink)part).getEquipmentNum(), part);
            } else if(part instanceof JumpJet) {
                jumpJets.put(((JumpJet)part).getEquipmentNum(), part);
            } else if(part instanceof MissingJumpJet) {
                jumpJets.put(((MissingJumpJet)part).getEquipmentNum(), part);
            }  else if(part instanceof BattleArmorEquipmentPart) {
                Part[] parts = baEquipParts.get(((BattleArmorEquipmentPart)part).getEquipmentNum());
                if(null == parts) {
                    parts = new Part[((BattleArmor)entity).getSquadSize()];
                }
                parts[((BattleArmorEquipmentPart)part).getTrooper()-BattleArmor.LOC_TROOPER_1] = part;
                baEquipParts.put(((BattleArmorEquipmentPart)part).getEquipmentNum(), parts);
            } else if(part instanceof MissingBattleArmorEquipmentPart) {
                Part[] parts = baEquipParts.get(((MissingBattleArmorEquipmentPart)part).getEquipmentNum());
                if(null == parts) {
                    parts = new Part[((BattleArmor)entity).getSquadSize()];
                }
                parts[((MissingBattleArmorEquipmentPart)part).getTrooper()-BattleArmor.LOC_TROOPER_1] = part;
                baEquipParts.put(((MissingBattleArmorEquipmentPart)part).getEquipmentNum(), parts);
            } else if(part instanceof EquipmentPart) {
                equipParts.put(((EquipmentPart)part).getEquipmentNum(), part);
            } else if(part instanceof MissingEquipmentPart) {
                equipParts.put(((MissingEquipmentPart)part).getEquipmentNum(), part);
            } else if(part instanceof MekActuator || part instanceof MissingMekActuator) {
                int type = -1;
                int loc = -1;
                if(part instanceof MekActuator) {
                    type = ((MekActuator)part).getType();
                    loc = ((MekActuator)part).getLocation();
                } else {
                    type = ((MissingMekActuator)part).getType();
                    loc = ((MissingMekActuator)part).getLocation();
                }
                if(type == Mech.ACTUATOR_UPPER_ARM) {
                    if(loc == Mech.LOC_RARM) {
                        rightUpperArm = part;
                    } else {
                        leftUpperArm = part;
                    }
                } else if(type == Mech.ACTUATOR_LOWER_ARM) {
                    if(loc == Mech.LOC_RARM) {
                        rightLowerArm = part;
                    } else {
                        leftLowerArm = part;
                    }
                } else if(type == Mech.ACTUATOR_HAND) {
                    if(loc == Mech.LOC_RARM) {
                        rightHand = part;
                    } else {
                        leftHand = part;
                    }
                } else if(type == Mech.ACTUATOR_UPPER_LEG) {
                    if(loc == Mech.LOC_LARM) {
                        leftUpperFrontLeg = part;
                    } else if(loc == Mech.LOC_RARM) {
                        rightUpperFrontLeg = part;
                    } else if(loc == Mech.LOC_RLEG) {
                        rightUpperLeg = part;
                    } else {
                        leftUpperLeg = part;
                    }
                } else if(type == Mech.ACTUATOR_LOWER_LEG) {
                    if(loc == Mech.LOC_LARM) {
                        leftLowerFrontLeg = part;
                    } else if(loc == Mech.LOC_RARM) {
                        rightLowerFrontLeg = part;
                    } else if(loc == Mech.LOC_RLEG) {
                        rightLowerLeg = part;
                    } else {
                        leftLowerLeg = part;
                    }
                } else if(type == Mech.ACTUATOR_FOOT) {
                    if(loc == Mech.LOC_LARM) {
                        leftFrontFoot = part;
                    } else if(loc == Mech.LOC_RARM) {
                        rightFrontFoot = part;
                    } else if(loc == Mech.LOC_RLEG) {
                        rightFoot = part;
                    } else {
                        leftFoot = part;
                    }
                }
            } else if(part instanceof QuadVeeGear || part instanceof MissingQuadVeeGear) {
                qvGear = part;
            } else if(part instanceof Avionics || part instanceof MissingAvionics) {
                avionics = part;
            } else if(part instanceof FireControlSystem || part instanceof MissingFireControlSystem) {
                fcs = part;
                //for reverse compatability, calculate costs
                if(part instanceof FireControlSystem) {
                    ((FireControlSystem)fcs).calculateCost();
                }
            } else if(part instanceof AeroSensor || part instanceof MissingAeroSensor) {
                sensor = part;
            } else if(part instanceof LandingGear || part instanceof MissingLandingGear) {
                landingGear = part;
            } else if(part instanceof AeroHeatSink || part instanceof MissingAeroHeatSink) {
                aeroHeatSinks.add(part);
                if (part.isOmniPodded()) {
                    podAeroHeatSinks++;
                }
            } else if(part instanceof MotiveSystem) {
                motiveSystem = part;
            } else if(part instanceof TurretLock) {
                turretLock = part;
            } else if(part instanceof DropshipDockingCollar || part instanceof MissingDropshipDockingCollar) {
                dropCollar = part;
            } else if(part instanceof ProtomekArmActuator || part instanceof MissingProtomekArmActuator) {
                int loc = -1;
                if(part instanceof ProtomekArmActuator) {
                    loc = ((ProtomekArmActuator)part).getLocation();
                } else {
                    loc = ((MissingProtomekArmActuator)part).getLocation();
                }
                if(loc == Protomech.LOC_LARM) {
                    protoLeftArmActuator = part;
                }
                else if(loc == Protomech.LOC_RARM) {
                    protoRightArmActuator = part;
                }
            } else if(part instanceof ProtomekLegActuator || part instanceof MissingProtomekLegActuator) {
                protoLegsActuator = part;
            } else if(part instanceof ProtomekJumpJet || part instanceof MissingProtomekJumpJet) {
                protoJumpJets.add(part);
            } else if ((part instanceof Thrusters)
                    && (entity.isLargeCraft() || entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT))) {
                if (((Thrusters) part).isLeftThrusters()) {
                    aeroThrustersLeft = part;
                } else {
                    aeroThrustersRight = part;
                }
            } else if ((part instanceof MissingThrusters)
                    && (entity.isLargeCraft() || entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT))) {
                if (((MissingThrusters) part).isLeftThrusters()) {
                    aeroThrustersLeft = part;
                } else {
                    aeroThrustersRight = part;
                }
            } else if (part instanceof TransportBayPart) {
                bays.put(((TransportBayPart) part).getBayNumber(), part);
            }

            part.updateConditionFromPart();
        }
        //now check to see what is null
        for(int i = 0; i<locations.length; i++) {
            if(entity.getOInternal(i) == IArmorState.ARMOR_NA) {
                //this is not a valid location, so we should skip it
                continue;
            }
            if(null == locations[i]) {
                if(entity instanceof Mech) {
                    MekLocation mekLocation = new MekLocation(i, (int) getEntity().getWeight(),
                            getEntity().getStructureType(), TechConstants.isClan(entity.getStructureTechLevel()),
                            hasTSM(), entity instanceof QuadMech, false, false, campaign);
                    addPart(mekLocation);
                    partsToAdd.add(mekLocation);
                } else if(entity instanceof Protomech && i != Protomech.LOC_NMISS) {
                    ProtomekLocation protomekLocation = new ProtomekLocation(i, (int) getEntity().getWeight(), getEntity().getStructureType(), ((Protomech)getEntity()).hasMyomerBooster(), entity instanceof QuadMech, campaign);
                    addPart(protomekLocation);
                    partsToAdd.add(protomekLocation);
                } else if(entity instanceof Tank && i != Tank.LOC_BODY) {
                    if(entity instanceof VTOL) {
                        if (i == VTOL.LOC_ROTOR) {
                            Rotor rotor = new Rotor((int)getEntity().getWeight(), campaign);
                            addPart(rotor);
                            partsToAdd.add(rotor);
                        } else if (i == VTOL.LOC_TURRET) {
                            if(((VTOL)entity).hasNoTurret()) {
                                continue;
                            }
                            Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
                            addPart(turret);
                            partsToAdd.add(turret);
                        } else if (i == VTOL.LOC_TURRET_2) {
                            if(((VTOL)entity).hasNoDualTurret()) {
                                continue;
                            }
                            Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
                            addPart(turret);
                            partsToAdd.add(turret);
                        } else {
                            TankLocation tankLocation = new TankLocation(i, (int) getEntity().getWeight(), campaign);
                            addPart(tankLocation);
                            partsToAdd.add(tankLocation);
                        }
                    } else if(i == Tank.LOC_TURRET) {
                         if(((Tank)entity).hasNoTurret()) {
                             continue;
                         }
                         Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
                         addPart(turret);
                         partsToAdd.add(turret);
                    } else if(i == Tank.LOC_TURRET_2) {
                         if(((Tank)entity).hasNoDualTurret()) {
                             continue;
                         }
                         Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
                         addPart(turret);
                         partsToAdd.add(turret);
                    } else {
                        TankLocation tankLocation = new TankLocation(i, (int) getEntity().getWeight(), campaign);
                        addPart(tankLocation);
                        partsToAdd.add(tankLocation);
                    }
                } else if(entity instanceof BattleArmor && i != 0 && i <= ((BattleArmor)entity).getSquadSize()) {
                    BattleArmorSuit baSuit = new BattleArmorSuit((BattleArmor)entity, i, campaign);
                    addPart(baSuit);
                    partsToAdd.add(baSuit);
                }
            }
            if(null == armor[i]) {
                if(entity instanceof Protomech) {
                    ProtomekArmor a = new ProtomekArmor((int) getEntity().getWeight(), getEntity().getOArmor(i, false), i, true, campaign);
                    addPart(a);
                    partsToAdd.add(a);
                }
                else if(entity instanceof BattleArmor) {
                    BaArmor a = new BaArmor((int) getEntity().getWeight(), getEntity().getOArmor(i, false), ((BattleArmor)entity).getArmorType(1), i, entity.isClan(), campaign);
                    addPart(a);
                    partsToAdd.add(a);
                }
                else {
                    Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, false), i, false, entity.isClanArmor(i), campaign);
                    addPart(a);
                    partsToAdd.add(a);
                }
            }
            if(null == armorRear[i] && entity.hasRearArmor(i)) {
                Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, true), i, true, entity.isClanArmor(i), campaign);
                addPart(a);
                partsToAdd.add(a);
            }
            if(entity instanceof Tank && null == stabilisers[i] && i != Tank.LOC_BODY) {
                VeeStabiliser s = new VeeStabiliser((int)getEntity().getWeight(),i, campaign);
                addPart(s);
                partsToAdd.add(s);
            }
        }
        for(Mounted m : entity.getEquipment()) {
            if(m.getLocation() == Entity.LOC_NONE) {
                //FIXME: is this ok? - are there any valid parts in LOC_NONE?
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
            if(!m.getType().isHittable()) {
                //there are some kind of non-hittable parts we might want to include for cost calculations
                if(!(m.getType() instanceof MiscType)) {
                    continue;
                }
                if(!(((MiscType)m.getType()).hasFlag(MiscType.F_BA_MANIPULATOR) ||
                                ((MiscType)m.getType()).hasFlag(MiscType.F_BA_MEA) ||
                                ((MiscType)m.getType()).hasFlag(MiscType.F_AP_MOUNT))) {
                       continue;
                }
            }
            if(m.getType() instanceof AmmoType) {
                int eqnum = entity.getEquipmentNum(m);
                Part apart = ammoParts.get(eqnum);
                int fullShots = ((AmmoType)m.getType()).getShots();
                boolean oneShot = false;
                if(m.getLocation() == Entity.LOC_NONE) {
                    fullShots = 1;
                    oneShot = true;
                }
                if(null == apart) {
                    if(entity instanceof BattleArmor) {
                        apart = new BattleArmorAmmoBin((int)entity.getWeight(), m.getType(), eqnum, ((BattleArmor)entity).getSquadSize() * (fullShots - m.getBaseShotsLeft()), oneShot, campaign);
                    } else if (entity.usesWeaponBays()) {
                        apart = new LargeCraftAmmoBin((int) entity.getWeight(), m.getType(), eqnum,
                                fullShots - m.getBaseShotsLeft(), m.getAmmoCapacity(), campaign);
                        ((LargeCraftAmmoBin) apart).setBay(entity.getBayByAmmo(m));
                    } else {
                        apart = new AmmoBin((int)entity.getWeight(), m.getType(), eqnum,
                                fullShots - m.getBaseShotsLeft(), oneShot, m.isOmniPodMounted(), campaign);
                    }
                    addPart(apart);
                    partsToAdd.add(apart);

                }
            } else if(m.getType() instanceof MiscType && (m.getType().hasFlag(MiscType.F_HEAT_SINK) || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
                if(m.getLocation() == Entity.LOC_NONE) {
                    //heat sinks located in LOC_NONE are base unhittable heat sinks
                    continue;
                }
                int eqnum = entity.getEquipmentNum(m);
                Part epart = heatSinks.get(eqnum);
                if(null == epart) {
                    epart = new HeatSink((int)entity.getWeight(), m.getType(), eqnum,
                            m.isOmniPodMounted(), campaign);
                    addPart(epart);
                    partsToAdd.add(epart);
                }
            } else if(m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                int eqnum = entity.getEquipmentNum(m);
                Part epart = jumpJets.get(eqnum);
                if(null == epart) {
                    epart = new JumpJet((int)entity.getWeight(), m.getType(), eqnum,
                            m.isOmniPodMounted(), campaign);
                    addPart(epart);
                    partsToAdd.add(epart);
                    if (entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                        protoJumpJets.add(epart);
                    }
                }
            } else {
                int eqnum = entity.getEquipmentNum(m);
                EquipmentType type = m.getType();
                if(entity instanceof BattleArmor) {
                    //for BattleArmor we have multiple parts per mount, one for each trooper
                    Part[] eparts = baEquipParts.get(eqnum);
                    for(int i = 0; i < ((BattleArmor)entity).getSquadSize(); i++) {
                        if(null == eparts || null == eparts[i]) {
                            Part epart = new BattleArmorEquipmentPart((int)entity.getWeight(), type, eqnum, i+BattleArmor.LOC_TROOPER_1, campaign);
                            addPart(epart);
                            partsToAdd.add(epart);
                        }
                    }
                } else {
                    Part epart = equipParts.get(eqnum);
                    if(null == epart) {
                        if(type instanceof InfantryAttack) {
                            continue;
                        }
                        if(entity instanceof Infantry && !(entity instanceof BattleArmor)
                                && m.getLocation() != Infantry.LOC_FIELD_GUNS) {
                            //don't add weapons here for infantry, unless field guns
                            continue;
                        }
                        if(type instanceof BayWeapon) {
                            //weapon bays aren't real parts
                            continue;
                        }
                        epart = new EquipmentPart((int)entity.getWeight(), type, eqnum,
                                m.isOmniPodMounted(), campaign);
                        if(type instanceof MiscType && type.hasFlag(MiscType.F_MASC)) {
                            epart = new MASC((int)entity.getWeight(), type, eqnum, campaign,
                                    erating, m.isOmniPodMounted());
                        }
                        addPart(epart);
                        partsToAdd.add(epart);
                    }
                }
            }
        }
        
        if((null == engine) && !(entity instanceof Infantry) && !(entity instanceof FighterSquadron)) {
            if(entity instanceof SmallCraft || entity instanceof Jumpship) {
                engine = new SpacecraftEngine((int) entity.getWeight(), 0, campaign, entity.isClan());
                addPart(engine);
                partsToAdd.add(engine);
                ((SpacecraftEngine)engine).calculateTonnage();
            } else if(null != entity.getEngine()) {
                engine = new EnginePart((int) entity.getWeight(), new Engine(entity.getEngine().getRating(), entity.getEngine().getEngineType(), entity.getEngine().getFlags()), campaign, entity.getMovementMode() == EntityMovementMode.HOVER && entity instanceof Tank);
                addPart(engine);
                partsToAdd.add(engine);
            }
        }
        
        for (Bay bay : entity.getTransportBays()) {
            bayPartsToAdd.put(bay.getBayNumber(), new ArrayList<>());
            BayType btype = BayType.getTypeForBay(bay);
            Part bayPart = bays.get(bay.getBayNumber());
            if (null == bayPart) {
                bayPart = new TransportBayPart((int) entity.getWeight(),
                        bay.getBayNumber(), bay.getCapacity(), campaign);
                addPart(bayPart);
                partsToAdd.add(bayPart);
                for (int i = 0; i < bay.getDoors(); i++) {
                    Part door = new BayDoor((int) entity.getWeight(), campaign);
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                }
                if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    for (int i = 0; i < bay.getCapacity(); i++) {
                        Part cubicle = new Cubicle((int) entity.getWeight(), btype, campaign);
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                    }
                }
            } else {
                List<Part> doors = bayPart.getChildPartIds().stream()
                        .map(id -> campaign.getPart(id))
                        .filter(p -> (null != p) && ((p instanceof BayDoor)
                                || (p instanceof MissingBayDoor)))
                        .collect(Collectors.toList());
                while (bay.getDoors() > doors.size()) {
                    Part door = new MissingBayDoor((int) entity.getWeight(), campaign);
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                    doors.add(door);
                }
                if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    List<Part> cubicles = bayPart.getChildPartIds().stream()
                            .map(id -> campaign.getPart(id))
                            .filter(p -> (null != p) && ((p instanceof Cubicle)
                                    || (p instanceof MissingCubicle)))
                            .collect(Collectors.toList());
                    while (bay.getCapacity() > cubicles.size()) {
                        Part cubicle = new MissingCubicle((int) entity.getWeight(), btype, campaign);
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                        cubicles.add(cubicle);
                    }
                }
            }
        }


        if(entity instanceof Mech) {
            if(null == gyro) {
                gyro =  new MekGyro((int) entity.getWeight(), entity.getGyroType(), entity.getOriginalWalkMP(), entity.isClan(), campaign);
                addPart(gyro);
                partsToAdd.add(gyro);
            }
            if(null == lifeSupport) {
                lifeSupport = new MekLifeSupport((int) entity.getWeight(), campaign);
                addPart(lifeSupport);
                partsToAdd.add(lifeSupport);
            }
            if(null == sensor) {
                sensor = new MekSensor((int) entity.getWeight(), campaign);
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if(null == cockpit) {
                cockpit = new MekCockpit((int) entity.getWeight(), ((Mech)entity).getCockpitType(), entity.isClan(), campaign);
                addPart(cockpit);
                partsToAdd.add(cockpit);
            }
            if(null == rightUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
                rightUpperArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM, campaign);
                addPart(rightUpperArm);
                partsToAdd.add(rightUpperArm);
            }
            if(null == leftUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
                leftUpperArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM, campaign);
                addPart(leftUpperArm);
                partsToAdd.add(leftUpperArm);
            }
            if(null == rightLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
                rightLowerArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM, campaign);
                addPart(rightLowerArm);
                partsToAdd.add(rightLowerArm);
            }
            if(null == leftLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
                leftLowerArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM, campaign);
                addPart(leftLowerArm);
                partsToAdd.add(leftLowerArm);
            }
            if(null == rightHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
                rightHand = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_RARM, campaign);
                addPart(rightHand);
                partsToAdd.add(rightHand);
            }
            if(null == leftHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
                leftHand = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_LARM, campaign);
                addPart(leftHand);
                partsToAdd.add(leftHand);
            }
            if(null == rightUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)) {
                rightUpperLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG, campaign);
                addPart(rightUpperLeg);
                partsToAdd.add(rightUpperLeg);
            }
            if(null == leftUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)) {
                leftUpperLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG, campaign);
                addPart(leftUpperLeg);
                partsToAdd.add(leftUpperLeg);
            }
            if(null == rightLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG)) {
                rightLowerLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG, campaign);
                addPart(rightLowerLeg);
                partsToAdd.add(rightLowerLeg);
            }
            if(null == leftLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)) {
                leftLowerLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG, campaign);
                addPart(leftLowerLeg);
                partsToAdd.add(leftLowerLeg);
            }
            if(null == rightFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RLEG)) {
                rightFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RLEG, campaign);
                addPart(rightFoot);
                partsToAdd.add(rightFoot);
            }
            if(null == leftFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)) {
                leftFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LLEG, campaign);
                addPart(leftFoot);
                partsToAdd.add(leftFoot);
            }
            if(null == rightUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM)) {
                rightUpperFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM, campaign);
                addPart(rightUpperFrontLeg);
                partsToAdd.add(rightUpperFrontLeg);
            }
            if(null == leftUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM)) {
                leftUpperFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM, campaign);
                addPart(leftUpperFrontLeg);
                partsToAdd.add(leftUpperFrontLeg);
            }
            if(null == rightLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM)) {
                rightLowerFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM, campaign);
                addPart(rightLowerFrontLeg);
                partsToAdd.add(rightLowerFrontLeg);
            }
            if(null == leftLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM)) {
                leftLowerFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM, campaign);
                addPart(leftLowerFrontLeg);
                partsToAdd.add(leftLowerFrontLeg);
            }
            if(null == rightFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RARM)) {
                rightFrontFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RARM, campaign);
                addPart(rightFrontFoot);
                partsToAdd.add(rightFrontFoot);
            }
            if(null == leftFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LARM)) {
                leftFrontFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LARM, campaign);
                addPart(leftFrontFoot);
                partsToAdd.add(leftFrontFoot);
            }
        }
        if (entity instanceof QuadVee && null == qvGear) {
            if (null == qvGear) {
                qvGear = new QuadVeeGear((int)entity.getWeight(), campaign);
                addPart(qvGear);
                partsToAdd.add(qvGear);
            }
        }
        if(entity instanceof Aero) {
            if(null == structuralIntegrity) {
                structuralIntegrity = new StructuralIntegrity((int)entity.getWeight(), campaign);
                addPart(structuralIntegrity);
                partsToAdd.add(structuralIntegrity);
            }
            if(null == avionics) {
                avionics = new Avionics((int)entity.getWeight(), campaign);
                addPart(avionics);
                partsToAdd.add(avionics);
            }
            if(null == fcs) {
                fcs = new FireControlSystem((int)entity.getWeight(), 0, campaign);
                addPart(fcs);
                partsToAdd.add(fcs);
                ((FireControlSystem)fcs).calculateCost();
            }
            if(null == sensor) {
                sensor = new AeroSensor((int) entity.getWeight(), entity instanceof Dropship || entity instanceof Jumpship, campaign);
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if(null == landingGear) {
                landingGear = new LandingGear((int) entity.getWeight(), campaign);
                addPart(landingGear);
                partsToAdd.add(landingGear);
            }
            if(null == lifeSupport) {
                lifeSupport = new AeroLifeSupport((int) entity.getWeight(), 0, !(entity instanceof SmallCraft || entity instanceof Jumpship), campaign);
                addPart(lifeSupport);
                partsToAdd.add(lifeSupport);
                ((AeroLifeSupport)lifeSupport).calculateCost();
            }
            if(null == dropCollar && entity instanceof Dropship) {
                dropCollar = new DropshipDockingCollar((int) entity.getWeight(), campaign,
                        ((Dropship) entity).getCollarType());
                addPart(dropCollar);
                partsToAdd.add(dropCollar);
            }
            int hsinks = ((Aero)entity).getOHeatSinks() - aeroHeatSinks.size();
            int podhsinks = ((Aero)entity).getPodHeatSinks() - podAeroHeatSinks;
            while(hsinks > 0) {
                AeroHeatSink aHeatSink = new AeroHeatSink((int)entity.getWeight(),
                        ((Aero)entity).getHeatType(), podhsinks > 0, campaign);
                addPart(aHeatSink);
                partsToAdd.add(aHeatSink);
                hsinks--;
                if (podhsinks > 0) {
                    podhsinks--;
                }
            }
            if (entity.isLargeCraft() || entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                if (aeroThrustersLeft == null) {
                    aeroThrustersLeft = new Thrusters(0, campaign, true);
                    addPart(aeroThrustersLeft);
                    partsToAdd.add(aeroThrustersLeft);
                }
                if (aeroThrustersRight == null) {
                    aeroThrustersRight = new Thrusters(0, campaign, false);
                    addPart(aeroThrustersRight);
                    partsToAdd.add(aeroThrustersRight);
                }
            }
         }
        if(entity instanceof Tank) {
            if(null == motiveSystem) {
                motiveSystem = new MotiveSystem((int)entity.getWeight(), campaign);
                addPart(motiveSystem);
                partsToAdd.add(motiveSystem);
            }
            if(null == sensor) {
                sensor = new VeeSensor((int) entity.getWeight(), campaign);
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if(!(entity instanceof VTOL) && !((Tank)entity).hasNoTurret() && null == turretLock) {
                turretLock = new TurretLock(campaign);
                addPart(turretLock);
                partsToAdd.add(turretLock);
            }
        }
        if(entity instanceof Protomech) {
            if(!entity.entityIsQuad()) {
                if(null == protoLeftArmActuator) {
                    protoLeftArmActuator = new ProtomekArmActuator((int)entity.getWeight(),Protomech.LOC_LARM, campaign);
                    addPart(protoLeftArmActuator);
                    partsToAdd.add(protoLeftArmActuator);
                }
                if(null == protoRightArmActuator) {
                    protoRightArmActuator = new ProtomekArmActuator((int)entity.getWeight(),Protomech.LOC_RARM, campaign);
                    addPart(protoRightArmActuator);
                    partsToAdd.add(protoRightArmActuator);
                }
            }
            if(null == protoLegsActuator) {
                protoLegsActuator = new ProtomekLegActuator((int)entity.getWeight(), campaign);
                addPart(protoLegsActuator);
                partsToAdd.add(protoLegsActuator);
            }
            if(null == sensor) {
                sensor = new ProtomekSensor((int) entity.getWeight(), campaign);
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            int jj = (entity).getOriginalJumpMP() - protoJumpJets.size();
            while(jj > 0) {
                ProtomekJumpJet protoJJ = new ProtomekJumpJet((int)entity.getWeight(), campaign);
                addPart(protoJJ);
                partsToAdd.add(protoJJ);
                jj--;
            }
        }
        if(entity instanceof Infantry && !(entity instanceof BattleArmor)) {
            if(null == motiveType && entity.getMovementMode() != EntityMovementMode.INF_LEG) {
                int number = ((Infantry)entity).getOInternal(Infantry.LOC_INFANTRY);
                if(((Infantry)entity).isMechanized()) {
                    number = ((Infantry)entity).getSquadN();
                }
                while(number > 0) {
                    motiveType = new InfantryMotiveType(0, campaign, entity.getMovementMode());
                    addPart(motiveType);
                    partsToAdd.add(motiveType);
                    number--;
                }
            }
            if(null == infantryArmor) {
                EquipmentType eq = ((Infantry)entity).getArmorKit();
                if (null != eq) {
                    infantryArmor = new EquipmentPart(0, eq, 0, false, campaign);
                } else {
                    infantryArmor = new InfantryArmorPart(0, campaign, ((Infantry)entity).getDamageDivisor(), ((Infantry)entity).isArmorEncumbering(), ((Infantry)entity).hasDEST(), ((Infantry)entity).hasSneakCamo(), ((Infantry)entity).hasSneakECM(), ((Infantry)entity).hasSneakIR(), ((Infantry)entity).hasSpaceSuit());
                }
                if(infantryArmor.getStickerPrice() > 0) {
                    int number = ((Infantry)entity).getOInternal(Infantry.LOC_INFANTRY);
                    while(number > 0) {
                        infantryArmor = new InfantryArmorPart(0, campaign, ((Infantry)entity).getDamageDivisor(), ((Infantry)entity).isArmorEncumbering(), ((Infantry)entity).hasDEST(), ((Infantry)entity).hasSneakCamo(), ((Infantry)entity).hasSneakECM(), ((Infantry)entity).hasSneakIR(), ((Infantry)entity).hasSpaceSuit());
                        addPart(infantryArmor);
                        partsToAdd.add(infantryArmor);
                        number--;
                    }
                }
            }
            InfantryWeapon primaryType = ((Infantry)entity).getPrimaryWeapon();
            InfantryWeapon secondaryType = ((Infantry)entity).getSecondaryWeapon();
            if(null == primaryW && null != primaryType) {
                int number = (((Infantry)entity).getSquadSize() - ((Infantry)entity).getSecondaryN()) * ((Infantry)entity).getSquadN();
                while(number > 0) {
                    primaryW = new InfantryWeaponPart((int)entity.getWeight(), primaryType, -1, campaign, true);
                    addPart(primaryW);
                    partsToAdd.add(primaryW);
                    number--;
                }

            }
            if(null == secondaryW && null != secondaryType) {
                int number = ((Infantry)entity).getSecondaryN() * ((Infantry)entity).getSquadN();
                while(number > 0) {
                    secondaryW = new InfantryWeaponPart((int)entity.getWeight(), secondaryType, -1, campaign, false);
                    addPart(secondaryW);
                    partsToAdd.add(secondaryW);
                    number--;
                }
            }
        }
        if (getEntity() instanceof LandAirMech) {
            if (null == avionics) {
                avionics = new Avionics((int)entity.getWeight(), campaign);
                addPart(avionics);
                partsToAdd.add(avionics);
            }
            if (null == landingGear) {
                landingGear = new LandingGear((int) entity.getWeight(), campaign);
                addPart(landingGear);
                partsToAdd.add(landingGear);
            }
        }

        if(addParts) {
            for(Part p : partsToAdd) {
                campaign.addPart(p, 0);
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
    
    /**
     * Checks for additional ammo bins and adds the appropriate part.
     * 
     * Large craft can combine all the ammo of a single type into a single bin. Switching the munition type
     * of one or more tons of ammo can require the addition of an ammo bin and can change the ammo bin
     * capacity.
     */
    public void adjustLargeCraftAmmo() {
        Map<Integer,Part> ammoParts = new HashMap<>();
        List<Part> toAdd = new ArrayList<>();
        for (Part p : parts) {
            if (p instanceof LargeCraftAmmoBin) {
                ammoParts.put(((LargeCraftAmmoBin) p).getEquipmentNum(), p);
            }
        }
        for (Mounted m : entity.getAmmo()) {
            Integer eqNum = entity.getEquipmentNum(m);
            Part part = ammoParts.get(eqNum);
            if (null == part) {
                part = new LargeCraftAmmoBin((int)entity.getWeight(), m.getType(), eqNum,
                        m.getOriginalShots() - m.getBaseShotsLeft(), m.getAmmoCapacity(), campaign);
                ((LargeCraftAmmoBin) part).setBay(entity.getBayByAmmo(m));
                toAdd.add(part);
            } else {
                part.updateConditionFromEntity(false);
                // Reset the name
                ((LargeCraftAmmoBin) part).changeMunition(m.getType());
            }
        }
        for (Part p : toAdd) {
            addPart(p);
            campaign.addPart(p, 0);
        }
    }
    
    /**
     * Adds a new zero-capacity ammo bin to the bay as part of an ammo swap. If the bay has a zeroed-out
     * bin, sets the ammo type on that one and returns it instead of creating a new one.
     * 
     * @param etype The type of ammo being changed to
     * @param bay   The weapon bay where the ammo is located
     * @return      The new ammo bin part
     */
    public LargeCraftAmmoBin addBayAmmoBin(EquipmentType etype, Mounted bay) {
        for (Part p : getParts()) {
            if (p instanceof LargeCraftAmmoBin) {
                final LargeCraftAmmoBin bin = (LargeCraftAmmoBin) p;
                if ((bin.getCapacity() == 0) && (bin.getBay() == bay)) {
                    bin.changeMunition(etype);
                    bin.updateConditionFromPart();
                    return bin;
                }
            }
        }
        try {
            Mounted m = entity.addEquipment(etype, bay.getLocation(), bay.isRearMounted(), 0);
            int anum = entity.getEquipmentNum(m);
            bay.addAmmoToBay(anum);
            LargeCraftAmmoBin bin = new LargeCraftAmmoBin((int) entity.getWeight(), etype, anum,
                    0, 0, campaign);
            addPart(bin);
            campaign.addPart(bin, 0);
            return bin;
        } catch (LocationFullException ex) {
            MekHQ.getLogger().log(Unit.class, "addBayAmmoBin(EquipmentType, Mounted)",
                    LogLevel.ERROR, "Location full exception attempting to add " + etype.getDesc()
                    + " to unit " + getName());
            MekHQ.getLogger().error(Unit.class, "addBayAmmoBin(EquipmentType, Mounted)", ex);
            return null;
        }
        
    }
    
    public ArrayList<Part> getParts() {
        return parts;
    }

    public void setParts(ArrayList<Part> newParts) {
        parts = newParts;
    }
    
    public ArrayList<PodSpace> getPodSpace() {
        return podSpace;
    }
    
    public void refreshPodSpace() {
        podSpace.forEach(ps -> ps.updateConditionFromEntity(false));
    }

    public ArrayList<AmmoBin> getWorkingAmmoBins() {
        ArrayList<AmmoBin> ammo = new ArrayList<AmmoBin>();
        for(Part part : parts) {
            if(part instanceof AmmoBin) {
                ammo.add((AmmoBin)part);
            }
        }
        return ammo;
    }

    public String getCamoCategory() {
        if (null == entity) {
            return "";
        }

        String category = campaign.getCamoCategory();
        if (isEntityCamo()) {
            category = entity.getCamoCategory();
        }

        if (Player.ROOT_CAMO.equals(category)) {
            category = "";
        }

        return category;
    }

    public String getCamoFileName() {
        if (null == campaign && null == entity) {
            return "";
        }

        String fileName = campaign.getCamoFileName();
        if (isEntityCamo()) {
            fileName = entity.getCamoFileName();
        }

        if (null == fileName) {
            fileName = "";
        }

        return fileName;
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
        //take first by rank
        //if rank is tied, take gunners over drivers
        //if two of the same type are tie rank, take the first one
        Person commander = null;
        for (UUID id : vesselCrew) {
            if (id == null) {
                continue;
            }
            Person p = campaign.getPerson(id);
            if ((null != p) && p.outRanks(commander)){
                commander = p;
            }
        }
        for (UUID pid : gunners) {
            if (pid == null) {
                continue;
            }
            Person p = campaign.getPerson(pid);
            if (p != null && entity != null && (entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) {
                continue;
            }
            if ((null != p) && p.outRanks(commander)) {
                commander = p;
            }
        }
        for (UUID pid : drivers) {
            if (pid == null) {
                continue;
            }
            Person p = campaign.getPerson(pid);
            if (p != null && entity != null && (entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) {
                continue;
            }
            if ((null != p) && p.outRanks(commander)) {
                commander = p;
            }
        }
        if (navigator != null) {
            Person p = campaign.getPerson(navigator);
            if(null != p && p.outRanks(commander)) {
                commander = p;
            }
        }
        return commander;
    }

    public void resetPilotAndEntity() {
        if (entity.getCrew().getSlotCount() > 1) {
            final String driveType = SkillType.getDrivingSkillFor(entity);
            final String gunType = SkillType.getGunnerySkillFor(entity);
            if (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos()) {
                //Command console; each crew is assigned as both driver and gunner
                int slot = 0;
                for(UUID pid : gunners) {
                    final Person p = campaign.getPerson(pid);
                    if (p.hasSkill(gunType) && p.hasSkill(driveType) && p.isActive()
                            && slot < entity.getCrew().getSlotCount()) {
                        assignToCrewSlot(p, slot, gunType, driveType);
                        slot++;
                    }
                }
                while (slot < entity.getCrew().getSlotCount()) {
                    entity.getCrew().setMissing(true, slot++);
                }
            } else {
                //tripod, quadvee, or dual cockpit; driver and gunner are assigned separately
                Optional<Person> person = drivers.stream().map(id -> campaign.getPerson(id))
                        .filter(p -> p.hasSkill(driveType) && p.isActive()).findFirst();
                if (person.isPresent()) {
                    assignToCrewSlot(person.get(), 0, gunType, driveType);
                } else {
                    entity.getCrew().setMissing(true, 0);
                }
                person = gunners.stream().map(id -> campaign.getPerson(id))
                        .filter(p -> p.hasSkill(driveType) && p.isActive()).findFirst();
                if (person.isPresent()) {
                    assignToCrewSlot(person.get(), 1, gunType, driveType);
                } else {
                    entity.getCrew().setMissing(true, 1);
                }
                int techPos = entity.getCrew().getCrewType().getTechPos();
                if (techPos >= 0) {
                    Person to = null;
                    if (null != techOfficer) {
                        to = campaign.getPerson(techOfficer);
                        if (!to.hasSkill(driveType) || !to.hasSkill(gunType) || !to.isActive()) {
                            to = null;
                        }
                    }
                    if (null != to) {
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
            if(null == commander) {
                entity.getCrew().setMissing(true, 0);
                return;
            }
            entity.getCrew().setName(commander.getFullTitle(), 0);
            entity.getCrew().setNickname(commander.getCallsign(), 0);
            entity.getCrew().setPortraitCategory(commander.getPortraitCategory(), 0);
            entity.getCrew().setPortraitFileName(commander.getPortraitFileName(), 0);
            entity.getCrew().setExternalIdAsString(commander.getId().toString(), 0);
            entity.getCrew().setToughness(commander.getToughness(), 0);
            
            if (entity instanceof Tank) {
                ((Tank)entity).setCommanderHit(commander.getHits() > 0);
            }
            entity.getCrew().setMissing(false, 0);
        }
        entity.getCrew().resetFatigue();
        entity.getCrew().setEjected(false);

        // Clear any stale game data that may somehow have gotten set incorrectly
        campaign.clearGameData(entity);
        //create a new set of options. For now we will just assign based on commander, but
        //we really should be more detailed about this.
        Person commander = getCommander();
        if (null != commander) {
            if (campaign.getCampaignOptions().useAbilities()) {
                PilotOptions options = new PilotOptions();
                for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
                     IOptionGroup group = i.nextElement();
                     for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                         IOption option = j.nextElement();
                         option.setValue(commander.getOptions().getOption(option.getName()).getValue());
                     }
                }
                entity.getCrew().setOptions(options);
            }
            if(usesSoloPilot()) {
                if(!commander.isActive()) {
                    entity.getCrew().setMissing(true, 0);;
                    return;
                }
                entity.getCrew().setHits(commander.getHits(), 0);
            }
            resetEngineer();
            //TODO: game option to use tactics as command and ind init bonus
            if(commander.hasSkill(SkillType.S_TACTICS)) {
                entity.getCrew().setCommandBonus(commander.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
            } else {
                entity.getCrew().setCommandBonus(0);
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

        for(UUID pid : drivers) {
            Person p = campaign.getPerson(pid);
            if(p.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }
            if(p.hasSkill(driveType)) {
                sumPiloting += p.getSkill(driveType).getFinalSkillValue();
                nDrivers++;
            } else if(entity instanceof Infantry) {
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
            if(campaign.getCampaignOptions().useAdvancedMedical()) {
                sumPiloting += p.getPilotingInjuryMod();
            }
        }
        for(UUID pid : gunners) {
            Person p = campaign.getPerson(pid);
            if(p.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }
            if(p.hasSkill(gunType)) {
                sumGunnery += p.getSkill(gunType).getFinalSkillValue();
                nGunners++;
            }
            if(p.hasSkill(SkillType.S_ARTILLERY)
                    && p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue() < artillery) {
                artillery = p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue();
            }
            if(campaign.getCampaignOptions().useAdvancedMedical()) {
                sumGunnery += p.getGunneryInjuryMod();
            }
        }
        
        for(UUID pid : vesselCrew) {
            Person p = campaign.getPerson(pid);
            if(null !=p && p.getHits() == 0) {
                nCrew++;
            }
        }
        if(null != navigator) {
            Person p = campaign.getPerson(navigator);
            if(null !=p && p.getHits() == 0) {
                nCrew++;
            }
        }
        //Using the tech officer field for the secondary commander; if nobody assigned to the command
        //console we will flag the entity as using the console commander, which has the effect of limiting
        //the tank to a single commander. As the console commander is not counted against crew requirements,
        //we do not increase nCrew if present.
        if (entity instanceof Tank && ((Tank)entity).hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
            Person p = null;
            if (null != techOfficer) {
                p = campaign.getPerson(techOfficer);
            }
            if (null == p || p.getHits() > 0) {
                ((Tank)entity).setUsingConsoleCommander(true);
            }
        }

        if(nDrivers > 0) {
            piloting = (int)Math.round(((double)sumPiloting)/nDrivers);
        }
        if(nGunners > 0) {
            gunnery = (int)Math.round(((double)sumGunnery)/nGunners);
        }
        if(entity instanceof Infantry) {
            if(entity instanceof BattleArmor) {
                int ntroopers = 0;
                //OK, we want to reorder the way we move through suits, so that we always put BA
                //in the suits with more armor. Otherwise, we may put a soldier in a suit with no
                //armor when a perfectly good suit is waiting further down the line.
                Map<String, Integer> bestSuits = new HashMap<String, Integer>();
                for(int i = BattleArmor.LOC_TROOPER_1; i <= ((BattleArmor)entity).getTroopers(); i++) {
                    bestSuits.put(Integer.toString(i), entity.getArmorForReal(i));
                    if(entity.getInternal(i)<0) {
                        bestSuits.put(Integer.toString(i), IArmorState.ARMOR_DESTROYED);
                    }
                    bestSuits = Utilities.sortMapByValue(bestSuits, true);
                }
                bestSuits.keySet();
                for(String key : bestSuits.keySet()) {
                    int i = Integer.parseInt(key);
                    if(!isBattleArmorSuitOperable(i)) {
                        //no suit here move along
                        continue;
                    }
                    if(ntroopers < nGunners) {
                        entity.setInternal(1, i);
                        ntroopers++;
                    } else {
                        entity.setInternal(0, i);
                    }
                }
                if(ntroopers < nGunners) {
                    //TODO: we have too many soldiers assigned to the available suits - do something!
                    //probably remove some crew and then re-run resetentityandpilot
                }
            }
            entity.setInternal(nGunners, Infantry.LOC_INFANTRY);
        }
        
        if(entity instanceof Tank) {
            if(nDrivers == 0 && nGunners == 0) {
                //nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            }
            if(nDrivers == 0) {
                ((Tank)entity).setDriverHit(true);
            } else {
                ((Tank)entity).setDriverHit(false);
            }
        } else if(entity instanceof Infantry) {
            if(nDrivers == 0 && nGunners == 0) {
                //nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            } 
        } else if(entity instanceof SmallCraft || entity instanceof Jumpship) {
            //assign crew hits based on what percent of the crew is present
            int currentSize = nDrivers + nGunners + nCrew;
            double percent = Math.max(0.0, 1.0 - (1.0 * currentSize)/getFullCrewSize());
            int hits = (int)Math.floor(percent * 6);
            if(percent > 0.0 && hits==0) {
                //at least one hit if less than full staffed
                hits = 1;
            }
            entity.getCrew().setHits(hits, 0);
        }

        //TODO: For the moment we need to max these out at 8 so people don't get errors
        //when they customize in MM but we should put an option in MM to ignore those limits
        //and set it to true when we start up through MHQ
        entity.getCrew().setPiloting(Math.min(Math.max(piloting, 0), 8), 0);
        entity.getCrew().setGunnery(Math.min(Math.max(gunnery, 0), 7), 0);
        entity.getCrew().setArtillery(Math.min(Math.max(artillery, 0), 8), 0);
        entity.getCrew().setSize(nCrew);
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

        if (campaign.getCampaignOptions().useAdvancedMedical()) {
            pilotingMech += pilot.getPilotingInjuryMod();
            gunneryMech += pilot.getGunneryInjuryMod();
            pilotingAero += pilot.getPilotingInjuryMod();
            gunneryAero += pilot.getGunneryInjuryMod();
            artillery += pilot.getGunneryInjuryMod();
        }
        LAMPilot crew = (LAMPilot)entity.getCrew();
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
        entity.getCrew().setPortraitCategory(p.getPortraitCategory(), slot);
        entity.getCrew().setPortraitFileName(p.getPortraitFileName(), slot);
        entity.getCrew().setHits(p.getHits(), slot);
        int gunnery = 7;
        int artillery = 7;
        int piloting = 8;
        if (p.hasSkill(gunType)) {
            gunnery = p.getSkill(gunType).getFinalSkillValue();
        }
        if (campaign.getCampaignOptions().useAdvancedMedical()) {
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
        //also set RPG gunnery skills in case present in game options
        entity.getCrew().setGunneryL(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setGunneryM(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setGunneryB(Math.min(Math.max(gunnery, 0), 7), slot);
        entity.getCrew().setArtillery(Math.min(Math.max(artillery, 0), 7), slot);
        entity.getCrew().setToughness(p.getToughness(), slot);
        entity.getCrew().setExternalIdAsString(p.getId().toString(), slot);
        entity.getCrew().setMissing(false, slot);
    }

    public void resetEngineer() {
        if(!isSelfCrewed()) {
            return;
        }
        int minutesLeft = TECH_WORK_DAY;
        int overtimeLeft = TECH_WORK_DAY / 2;
        int edgeLeft = 0;
        boolean breakpartreroll = true;
        boolean failrefitreroll = true;
        if(null != engineer) {
            minutesLeft = engineer.getMinutesLeft();
            overtimeLeft = engineer.getOvertimeLeft();
            edgeLeft = engineer.getEdge();
        } else {
            //then get the number based on the least amount available to crew members
            //in the case of Edge, everyone must have the same triggers set for Edge to work
            for(Person p : getActiveCrew()) {
                if(p.getMinutesLeft() < minutesLeft) {
                    minutesLeft = p.getMinutesLeft();
                }
                if(p.getOvertimeLeft() < overtimeLeft) {
                    overtimeLeft = p.getOvertimeLeft();
                }
                if(p.getEdge() < edgeLeft) {
                    edgeLeft = p.getEdge();
                }
            }
        }
        if(getEntity() instanceof Infantry) {
            if(!isUnmanned()) {
                engineer = new Person(getCommander().getName(), campaign);
                engineer.setEngineer(true);
                engineer.setMinutesLeft(minutesLeft);
                engineer.setOvertimeLeft(overtimeLeft);
                engineer.setId(getCommander().getId());
                engineer.setPrimaryRole(Person.T_MECHANIC);
                engineer.setRankNumeric(getCommander().getRankNumeric());
                //will only be reloading ammo, so doesn't really matter what skill level we give them - set to regular
                engineer.addSkill(SkillType.S_TECH_MECHANIC, SkillType.getType(SkillType.S_TECH_MECHANIC).getRegularLevel(), 0);
            } else {
                engineer = null;
            }
        } else {
            if (vesselCrew.size() > 0) {
                int nCrew = 0;
                int sumSkill = 0;
                int sumBonus = 0;
                int sumEdge = 0;
                int sumEdgeUsed = 0;
                String engineerName = "Nobody";
                int bestRank = Integer.MIN_VALUE;
                for(UUID pid : vesselCrew) {
                    Person p = campaign.getPerson(pid);
                    if(null == p) {
                        continue;
                    }
                    //If the engineer used edge points, remove some from vessel crewmembers until all is paid for
                    if (engineer != null) {
                        if (engineer.getEdgeUsed() > 0) {
                            //Don't subtract an Edge if the individual has none left
                            if (p.getEdge() > 0) {
                                p.setEdge(p.getEdge() - 1);
                                engineer.setEdgeUsed(engineer.getEdgeUsed() - 1);
                            }
                        }
                        //If the engineer gained XP, add it for each crewman
                        p.setXp(p.getXp() + engineer.getEngineerXp());
                        
                        //Update each crewman's successful task count too
                        p.setNTasks(p.getNTasks() + engineer.getNTasks());
                        if (p.getNTasks() >= campaign.getCampaignOptions().getNTasksXP()) {
                            p.setXp(p.getXp() + campaign.getCampaignOptions().getTaskXP());
                            p.setNTasks(0);
                        }
                        sumEdgeUsed = engineer.getEdgeUsed();
                    }
                    sumEdge += p.getEdge();                    
                    
                    if(p.hasSkill(SkillType.S_TECH_VESSEL)) {
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
                    if(p.getRankNumeric() > bestRank) {
                        engineerName = p.getFullName();
                        bestRank = p.getRankNumeric();
                    }
                }
                if(nCrew > 0) {
                    engineer = new Person(engineerName, campaign);
                    engineer.setEngineer(true);
                    engineer.setEngineerXp(0);
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_BREAK_PART, breakpartreroll);
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, failrefitreroll);
                    engineer.setMinutesLeft(minutesLeft);
                    engineer.setOvertimeLeft(overtimeLeft);
                    engineer.setId(getCommander().getId());
                    engineer.setPrimaryRole(Person.T_SPACE_CREW);
                    if(bestRank > -1) {
                        engineer.setRankNumeric(bestRank);
                    }
                    engineer.addSkill(SkillType.S_TECH_VESSEL, sumSkill/nCrew, sumBonus/nCrew);
                    engineer.setEdgeUsed(sumEdgeUsed);
                    engineer.setEdge((sumEdge - sumEdgeUsed)/nCrew);
                    engineer.setUnitId(this.getId());
                } else {
                    engineer = null;
                }
            } else { // Needed to fix bug where removed crew doesn't remove engineer
                engineer = null;
            }
        }
        if(null != engineer) {
            //change reference for any scheduled tasks
            for(Part p : getParts()) {
                if(p.isBeingWorkedOn()) {
                    p.setTeamId(engineer.getId());
                }
            }
        } else {
            //cancel any mothballing if this happens
            if(isMothballing()) {
                mothballTime = 0;
            }
            //cancel any scheduled tasks
            for(Part p : getParts()) {
                if(p.isBeingWorkedOn()) {
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

    public int getTotalCrewNeeds() {
        int nav = 0;
        if(entity instanceof SmallCraft || entity instanceof Jumpship) {
            if(entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                nav = 1;
            }
            return getAeroCrewNeeds() - getTotalDriverNeeds() - nav;
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
        if(entity instanceof SmallCraft || entity instanceof Jumpship) {
            if(entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                nav = 1;
            }
            return nCrew < (getAeroCrewNeeds() - getTotalDriverNeeds() - nav);
        }
        return false;
    }
    
    public boolean canTakeNavigator() {
        return entity instanceof Jumpship && !(entity instanceof SpaceStation) && navigator == null;
    }

    public boolean canTakeTechOfficer() {
        return techOfficer == null &&
                (entity.getCrew().getCrewType().getTechPos() >= 0
                //Use techOfficer field for secondary commander
                || (entity instanceof Tank && entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)));
    }

    public boolean canTakeTech() {
        return tech == null && requiresMaintenance() && !isSelfCrewed();
    }

    public boolean canTakeMoreGunners() {
        int nGunners = gunners.size();
        if(nGunners == 3) {
        }
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
        ensurePersonIsRegistered(p);
        drivers.add(p.getId());
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void addGunner(Person p) {
        addGunner(p, false);
    }

    public void addGunner(Person p, boolean useTransfers) {
        ensurePersonIsRegistered(p);
        gunners.add(p.getId());
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void addVesselCrew(Person p) {
        addVesselCrew(p, false);
    }

    public void addVesselCrew(Person p, boolean useTransfers) {
        ensurePersonIsRegistered(p);
        vesselCrew.add(p.getId());
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void setNavigator(Person p) {
        setNavigator(p, false);
    }
    
    public void setNavigator(Person p, boolean useTransfers) {
        ensurePersonIsRegistered(p);
        navigator = p.getId();
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }
    
    public boolean isTechOfficer(Person p) {
        return null != techOfficer && techOfficer.equals(p.getId());
    }

    public void setTechOfficer(Person p) {
        setTechOfficer(p, false);
    }

    public void setTechOfficer(Person p, boolean useTransfers) {
        ensurePersonIsRegistered(p);
        techOfficer = p.getId();
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void setTech(Person p) {
        ensurePersonIsRegistered(p);
        tech = p.getId();
        p.addTechUnitID(getId());
        p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        MekHQ.triggerEvent(new PersonTechAssignmentEvent(p, this));
    }

    public void setTech(UUID pid) {
        tech = pid;
    }

    public void removeTech() {
        if (tech != null) {
            MekHQ.triggerEvent(new PersonTechAssignmentEvent(campaign.getPerson(tech), this));
            tech = null;
        }
    }

    private void ensurePersonIsRegistered(Person p) {
        Objects.requireNonNull(p);
        if(null == campaign.getPerson(p.getId())) {
            campaign.addPersonWithoutId(p, false);
        }
    }
    
    public void addPilotOrSoldier(Person p) {
        addPilotOrSoldier(p, false);
    }

    public void addPilotOrSoldier(Person p, boolean useTransfers) {
        ensurePersonIsRegistered(p);
        drivers.add(p.getId());
        //Multi-crew cockpits should not set the pilot to the gunner position 
        if (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos()) {
            gunners.add(p.getId());
        }
        p.setUnitId(getId());
        resetPilotAndEntity();
        if (useTransfers) {
            p.addLogEntry(campaign.getDate(), "Reassigned to " + getName());
        } else {
            p.addLogEntry(campaign.getDate(), "Assigned to " + getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
    }

    public void remove(Person p, boolean log) {
        ensurePersonIsRegistered(p);
        if(p.getId().equals(tech)) {
            tech = null;
            p.removeTechUnitId(getId());
            MekHQ.triggerEvent(new PersonTechAssignmentEvent(p, this));
        } else {
            p.setUnitId(null);
            drivers.remove(p.getId());
            gunners.remove(p.getId());
            vesselCrew.remove(p.getId());
            if(p.getId().equals(navigator)) {
                navigator = null;
            }
            if (p.getId().equals(techOfficer)) {
                techOfficer = null;
            }
            if((null != engineer) && p.getId().equals(engineer.getId())) {
            	engineer = null;
            }
            resetPilotAndEntity();
            MekHQ.triggerEvent(new PersonCrewAssignmentEvent(p, this));
        }
        if(log) {
            p.addLogEntry(campaign.getDate(), "Removed from " + getName());
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

    public ArrayList<Person> getCrew() {
        ArrayList<Person> crew = new ArrayList<Person>();
        for(UUID id : drivers) {
            Person p = campaign.getPerson(id);
            if(null != p) {
                crew.add(p);
            }
        }
        if(!usesSoloPilot() && !usesSoldiers()) {
            for(UUID id : gunners) {
                Person p = campaign.getPerson(id);
                if(null != p) {
                    crew.add(p);
                }
            }
        }
        for(UUID id : vesselCrew) {
            Person p = campaign.getPerson(id);
            if(null != p) {
                crew.add(p);
            }
        }
        if(navigator != null) {
            Person p = campaign.getPerson(navigator);
            if(null != p) {
                crew.add(p);
            }
        }
        if (techOfficer != null) {
            Person p = campaign.getPerson(techOfficer);
            if (null != p) {
                crew.add(p);
            }
        }
        return crew;
    }

    public ArrayList<UUID> getDriverIDs() {
        return drivers;
    }
    
    public ArrayList<UUID> getGunnerIDs() {
        return gunners;
    }
    
    public ArrayList<UUID> getVesselCrewIDs() {
        return vesselCrew;
    }
    
    public UUID getTechOfficerID() {
        return techOfficer;
    }

    public UUID getNavigatorID() {
        return navigator;
    }
    
    public Person getTech() {
        if(null != engineer) {
            return engineer;
        }
        if(null != tech) {
            return campaign.getPerson(tech);
        }
        return null;
    }

    public boolean isMothballing() {
        return mothballTime > 0;
    }

    public int getMothballTime() {
        return mothballTime;
    }

    public void setMothballTime(int t) {
        mothballTime = t;
    }

    public boolean isMothballed() {
        return mothballed;
    }

    public void setMothballed(boolean b) {
        this.mothballed = b;
        // Tech gets removed either way bug [#488]
        if(null != tech) {
            remove(getTech(), true);
        }
        if(mothballed) {
            //remove any other personnel
            for(Person p : getCrew()) {
                remove(p, true);
            }
            resetPilotAndEntity();
        } else {
            //start maintenance cycle over again
            resetDaysSinceMaintenance();
            
            // if we previously mothballed this unit, attempt to restore its pre-mothball state
            if(mothballInfo != null) {
                mothballInfo.restorePreMothballInfo(this, campaign);
                mothballInfo = null;
            }
        }
    }

    public void startMothballing(UUID id) {
        if(!isMothballed()) {
            mothballInfo = new MothballInfo(this);
        }
        
        //set this person as tech
        if(!isSelfCrewed() && null != tech && !tech.equals(id)) {
            if(null != getTech()) {
                remove(getTech(), true);
            }
        }
        tech = id;
        //dont remove personnel yet, because self crewed units need their crews to mothball
        campaign.removeUnitFromForce(this);
        //clear any assigned tasks
        for(Part p : getParts()) {
            p.cancelAssignment();
        }
        //set mothballing time
        if(getEntity() instanceof Infantry) {
            mothballTime = TECH_WORK_DAY;
        }
        else if(getEntity() instanceof Dropship || getEntity() instanceof Jumpship) {
            mothballTime = TECH_WORK_DAY * (int)Math.ceil(getEntity().getWeight()/500.0);
        } else {
            if(isMothballed()) {
                mothballTime = TECH_WORK_DAY;
            } else {
                mothballTime = TECH_WORK_DAY * 2;
            }
        }
        campaign.mothball(this);
    }

    public ArrayList<Person> getActiveCrew() {
        ArrayList<Person> crew = new ArrayList<Person>();
        for(UUID id : drivers) {
            Person p = campaign.getPerson(id);
            if(null != p) {
                if(p.getHits() > 0 && (entity instanceof Tank || entity instanceof Infantry)) {
                    continue;
                }
                crew.add(p);
            }
        }
        if(!usesSoloPilot() && !usesSoldiers()) {
            for(UUID id : gunners) {
                Person p = campaign.getPerson(id);
                if(null != p) {
                    if(p.getHits() > 0 && (entity instanceof Tank || entity instanceof Infantry)) {
                        continue;
                    }
                    crew.add(p);
                }
            }
        }
        for(UUID id : vesselCrew) {
            Person p = campaign.getPerson(id);
            if(null != p) {
                crew.add(p);
            }
        }
        if (null != navigator) {
            Person p = campaign.getPerson(navigator);
            if(null != p) {
                crew.add(p);
            }
        }
        if (techOfficer != null) {
            Person p = campaign.getPerson(techOfficer);
            if (null != p) {
                crew.add(p);
            }
        }
        return crew;
    }

    public boolean isDriver(Person person) {
        for(UUID id : drivers) {
            if(person.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGunner(Person person) {
        for(UUID id : gunners) {
            if(person.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a person is considered the commander of this unit.
     * 
     * @param person A <code>Person</code> in the campaign. The person need not be assigned to the unit as
     *               crew, in which case the return value will be false.
     * @return       Whether the person is considered the unit commander. If <code>person</code> is null or
     *               the unit has no crew, this method will return false
     * 
     * @see {@link #getCommander()}
     */
    public boolean isCommander(Person person) {
        Person commander = getCommander();
        return (null != person) && (null != commander) && person.getId().equals(commander.getId());
    }

    public boolean isNavigator(Person person) {
        return person.getId().equals(navigator);
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
        if (getFluffName() != null && !getFluffName().equals("")) {
            return entity.getShortName() + " - " + getFluffName();
        }
        return entity.getShortName();
    }

    public String getHyperlinkedName() {
        return "<a href='UNIT:" + getId() + "'>" + entity.getShortName() + "</a>";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Unit other = (Unit) obj;
        return Objects.equals(id, other.id) && Objects.equals(getName(), other.getName());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, getName());
    }

    public Person getEngineer() {
        return engineer;
    }

    public UUID getTechId() {
        return tech;
    }

    public int getOldId() {
        return oldId;
    }

    public void fixIdReferences(Map<Integer, UUID> uHash, Map<Integer, UUID> peopleHash) {
        for(int oid : oldDrivers) {
            UUID nid = peopleHash.get(oid);
            if(null != nid) {
                drivers.add(peopleHash.get(oid));
            }
        }
        for(int oid : oldGunners) {
            UUID nid = peopleHash.get(oid);
            if(null != nid) {
                gunners.add(peopleHash.get(oid));
            }
        }
        for(int oid : oldVesselCrew) {
            UUID nid = peopleHash.get(oid);
            if(null != nid) {
                vesselCrew.add(peopleHash.get(oid));
            }
        }
        navigator = peopleHash.get(oldNavigator);
        if(null != refit) {
            refit.fixIdReferences(uHash, peopleHash);
        }
    }

    public Part getPartForEquipmentNum(int index, int loc) {
        for(Part p : parts) {
            if(p.isPartForEquipmentNum(index, loc)) {
                return p;
            }
        }
        return null;
    }

    public boolean isEntityCamo() {
        if ((null != entity) && (null != entity.getCamoCategory()
                && entity.getCamoCategory() != IPlayer.NO_CAMO
                && !entity.getCamoCategory().isEmpty())
                && (null != entity.getCamoFileName())
                && (!Player.NO_CAMO.equals(entity.getCamoFileName()))
                && !entity.getCamoFileName().isEmpty()) {
            return true;
        }

        return false;
    }

    public int getAvailability(int era) {
        //take the highest availability of all parts
        int availability = EquipmentType.RATING_A;
        for(Part p : parts) {
            int newAvailability = p.getAvailability();
            //Taharqa: its not clear whether a unit should really be considered extinct
            //when its parts are extinct as many probably outlive the production of parts
            //it would be better to just use the unit extinction date itself, but given
            //that there are no canon extinction/reintro dates for units, we will use this
            //instead
            if(p.isExtinct(campaign.getCalendar().get(Calendar.YEAR), campaign.getFaction().isClan())) {
                newAvailability = EquipmentType.RATING_X;
            }
            if(newAvailability > availability) {
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
        if(daysToArrival > 0) {
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
        if(getEntity() instanceof Mech) {
            switch(getEntity().getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                return 30;
            case EntityWeightClass.WEIGHT_LIGHT:
                return 45;
            case EntityWeightClass.WEIGHT_MEDIUM:
                return 60;
            case EntityWeightClass.WEIGHT_HEAVY:
                return 75;
            case EntityWeightClass.WEIGHT_ASSAULT:
            default:
                return  90;
            }
        }
        if(getEntity() instanceof Protomech) {
            return 20;
        }
        if(getEntity() instanceof BattleArmor) {
            return 10;
        }
        if(getEntity() instanceof ConvFighter) {
            return 45;
        }
        if(getEntity() instanceof SmallCraft && !(getEntity() instanceof Dropship)) {
            return 90;
        }
        if(getEntity() instanceof Aero
                && !(getEntity() instanceof Dropship)
                && !(getEntity() instanceof Jumpship)) {
            switch(getEntity().getWeightClass()) {
            case EntityWeightClass.WEIGHT_LIGHT:
                return 45;
            case EntityWeightClass.WEIGHT_MEDIUM:
                return 60;
            case EntityWeightClass.WEIGHT_HEAVY:
            default:
                return  75;
            }
        }
        if(getEntity() instanceof SupportTank) {
            switch(getEntity().getWeightClass()) {
            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                return 20;
            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                return 35;
            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
            default:
                return  100;
            }
        }
        if(getEntity() instanceof Tank) {
            switch(getEntity().getWeightClass()) {
            case EntityWeightClass.WEIGHT_LIGHT:
                return 30;
            case EntityWeightClass.WEIGHT_MEDIUM:
                return 50;
            case EntityWeightClass.WEIGHT_HEAVY:
                return 75;
            case EntityWeightClass.WEIGHT_ASSAULT:
                return 90;
            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
            default:
                return  120;
            }
        }
        //the rest get support from crews, so zero
        return 0;
    }

    public void incrementDaysSinceMaintenance(boolean maintained, int astechs) {
        daysSinceMaintenance++;
        astechDaysMaintained += astechs;
        if(maintained) {
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
        return (daysActivelyMaintained/(double)daysSinceMaintenance);
    }

    public boolean isFullyMaintained() {
        return daysActivelyMaintained == daysSinceMaintenance;
    }

    public int getAstechsMaintained() {
        return (int)Math.floor((1.0 * astechDaysMaintained) / daysSinceMaintenance);
    }

    public int getQuality() {
        int nParts = 0;
        int sumQuality = 0;
        for(Part p : getParts()) {
            //no rules about this but lets assume missing parts are quality A
            if(p instanceof MissingPart) {
                nParts++;
            }
            else if(p.needsMaintenance()) {
                nParts++;
                sumQuality += p.getQuality();
            }
        }
        if(nParts == 0) {
            return Part.QUALITY_D;
        }
        return (int)Math.round((1.0 * sumQuality)/nParts);
    }

    public void setQuality(int q) {
        for (Part p : getParts()) {
            if (!(p instanceof MissingPart)) {
                p.setQuality(q);
            }
        }
    }

    public String getQualityName() {
        return Part.getQualityName(getQuality(), campaign.getCampaignOptions().reverseQualityNames());
    }

    public boolean requiresMaintenance() {
        if(!isAvailable()) {
            return false;
        }
        if(getEntity() instanceof Infantry && !(getEntity() instanceof BattleArmor)) {
            return false;
        }
        return true;
    }

    public boolean isSelfCrewed() {
        return (getEntity() instanceof Dropship || getEntity() instanceof Jumpship
                || getEntity() instanceof Infantry && !(getEntity() instanceof BattleArmor));
    }

    public boolean isUnderRepair() {
        for(Part p : getParts()) {
            if(null != p.getTeamId()) {
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

    public static int getDamageState(Entity en) {
        return en.getDamageLevel(false);
    }

    public void resetParts() {
        parts = new ArrayList<Part>();
    }

    /**
     * @return the name
     */
    public String getFluffName() {
        return this.fluffName;
    }

    /**
     * @param name the name to set
     */
    public void setFluffName(String name) {
        this.fluffName = name;
    }

    /**
     * Checks to see if a particular BA suit on BA is currently operable
     * This requires the suit to not be destroyed and to have not missing equipment parts
     */
    public boolean isBattleArmorSuitOperable(int trooper) {
        if(null == getEntity() || !(getEntity() instanceof BattleArmor)) {
            return false;
        }
        if(getEntity().getInternal(trooper) < 0) {
            return false;
        }
        for(Part part : getParts()) {
            if(part instanceof MissingBattleArmorEquipmentPart &&
                    ((MissingBattleArmorEquipmentPart)part).getTrooper() == trooper) {
                return false;
            }
        }
        return true;
    }

    public boolean isIntroducedBy(int year) {
        return null != entity && entity.getYear() <= year;
    }

    public boolean isExtinctIn(int year) {
        //TODO: currently we do not track this in MM (and I don't think it really exists,
        //but I am adding the code elsewhere to take advantage of this method if we do code it.
        return false;
    }

    public String toString() {
        String entName = "None";
        if (getEntity() != null) {
            entName = getEntity().getDisplayName();
        }
        return "Unit for Entity: " + entName;
    }
    
    public String displayMonthlyCost() {
        DecimalFormat numFormatter = new DecimalFormat();
        
        String unitMonthlyCost = "<b>Spare Parts</b>: " + numFormatter.format(getSparePartsCost()) + " C-bills<br>"
                               + "<b>Ammunition</b>: " + numFormatter.format(getAmmoCost()) + " C-bills<br>"
                               + "<b>Fuel</b>: " + numFormatter.format(getFuelCost()) + " C-bills<br>";
        return unitMonthlyCost;
    }

    public long getSparePartsCost() {
        final String METHOD_NAME = "getSparePartsCost()"; //$NON-NLS-1$
        long partsCost = 0;

        entity = getEntity();
        if (isMothballed()) {
            return partsCost;
        }
        if ((entity instanceof Jumpship) || (entity instanceof SpaceStation)) {
            partsCost += ((long)entity.getWeight()) * .0001 * 15000;
        } else if (entity instanceof Aero) {
            partsCost += ((long)entity.getWeight()) * .001 * 15000;
        } else if (entity instanceof Tank) {
            partsCost += ((long)entity.getWeight()) * .001 * 8000;
        } else if ((entity instanceof Mech) || (entity instanceof BattleArmor)
                || ((Infantry) entity).isMechanized()) {
            partsCost += ((long)entity.getWeight()) * .001 * 10000;
        } else if (entity instanceof Infantry) {
            if (entity.getMovementMode() == EntityMovementMode.INF_LEG) {
                partsCost += 3 * .002 * 10000;
            } else if (entity.getMovementMode() == EntityMovementMode.INF_JUMP) {
                partsCost += 4 * .002 * 10000;
            } else if (entity.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                partsCost += 6 * .002 * 10000;
            } else {
                partsCost += ((long)entity.getWeight()) * .002 * 10000;
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        getName() + " is not a generic CI. Movement mode is "
                        + entity.getMovementModeAsString());
            }

        } else {
            // Only protomechs should fall here. Anything else needs to be logged
            if (!(entity instanceof Protomech)) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        getName() + " has no Spare Parts value for unit type " 
                        + Entity.getEntityTypeName(entity.getEntityType()));
            }
        }

        // Handle cost for quirks if used
        if (entity.hasQuirk("easy_maintain")) {
            partsCost = (long)(partsCost * .8);
        }
        if (entity.hasQuirk("difficult_maintain")) {
            partsCost = (long)(partsCost * 1.25);
        }
        if (entity.hasQuirk("non_standard")) {
            partsCost = (long)(partsCost * 2.0);
        }
        if (entity.hasQuirk("ubiquitous_is")) {
            partsCost = (long)(partsCost * .75);
        }
        // TODO Obsolete quirk

        // Now for extended parts cost modifiers
        if (campaign.getCampaignOptions().useExtendedPartsModifier()) {
            Engine en = entity.getEngine();
            int currentYear = campaign.getCalendar().get(Calendar.YEAR);
            int rating = getTechRating();
            if (((currentYear > 2859) && (currentYear < 3040))
                    && (!campaign.getFaction().isClan() && !campaign.getFaction().isComstar())) {
                if (rating > EquipmentType.RATING_D) {
                    partsCost = (long)(partsCost * 5.0);
                }
            }
            if (rating == EquipmentType.RATING_E) {
                partsCost = (long)(partsCost * 1.1);
            }
            if (rating == EquipmentType.RATING_F) {
                partsCost = (long)(partsCost * 1.25);
            }
            if ((entity instanceof Tank)
                    && (en.getEngineType() == Engine.NORMAL_ENGINE)) {
                partsCost = (long)(partsCost * 2.0);
            }
            if (!(entity instanceof Infantry)) {
                if ((en.getEngineType() == Engine.XL_ENGINE)
                        || (en.getEngineType() == Engine.XXL_ENGINE)) {
                    partsCost = (long)(partsCost * 2.5);
                }
                if (en.getEngineType() == Engine.LIGHT_ENGINE) {
                    partsCost = (long)(partsCost * 1.5);
                }
            }
            if (entity.isClan()) {
                if ((currentYear >3048) && (currentYear < 3071)) {
                    partsCost = (long)(partsCost * 5.0);
                } else if (currentYear > 3070) {
                    partsCost = (long)(partsCost * 4.0);
                }
            }
        }

        return partsCost;
    }

    public long getAmmoCost() {
        long ammoCost = 0;

        for (Part p : getParts()) {
            if (p instanceof EquipmentPart && ((EquipmentPart)p).getType() instanceof AmmoType) {
                ammoCost += p.getStickerPrice();
            }
        }
        ammoCost = (long)(ammoCost * .25);

        return ammoCost;
    }

    public long getFuelCost() {
        long fuelCost = 0;

        if ((entity instanceof Warship) || (entity instanceof SmallCraft) ) {
            fuelCost += ((long)getTonsBurnDay(entity));
        } else if (entity instanceof Jumpship) {
            fuelCost += ((long)getTonsBurnDay(entity));// * 3 * 15000;
        } else if (entity instanceof ConvFighter) {
            fuelCost += getFighterFuelCost(entity);
        } else if (entity instanceof megamek.common.Aero) {
            fuelCost += ((long)((Aero) entity).getFuelTonnage()) * 4 * 15000;
        } else if ((entity instanceof Tank) || (entity instanceof Mech)) {
            fuelCost += getVehicleFuelCost(entity);
        } else if (entity instanceof Infantry) {
            fuelCost += getInfantryFuelCost(entity);
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

    public double getFighterFuelCost(Entity e) {
        Engine en = e.getEngine();
        if (en.isFusion()) {
            return ((Aero) e).getFuelTonnage() * 4 * 15000;
        } else {
            return ((Aero) e).getFuelTonnage() * 4 * 1000;
        }
    }

    public double getVehicleFuelCost(Entity e) {
        Engine en = e.getEngine();
        if (e instanceof SupportTank) {
            if (en.getEngineType() == Engine.FUEL_CELL) {
                return (((SupportTank) e).getFuelTonnage() * 15000 * 4);
            } else if (en.getEngineType() == Engine.COMBUSTION_ENGINE) {
                return (((SupportTank) e).getFuelTonnage() * 1000 * 4);
            } else {
                return 0;
            }
        } else {
            if (en.getEngineType() == Engine.FUEL_CELL) {
                return (en.getWeightEngine(e) *.1 * 15000 * 4);
            } else if (en.getEngineType() == Engine.COMBUSTION_ENGINE) {
                return (en.getWeightEngine(e) *.1 * 1000 * 4);
            } else {
                return 0;
            }
        }
    }

    public double getInfantryFuelCost(Entity e) {
        if (e instanceof BattleArmor) {
            if (e.getJumpMP() > 0) {
                return (e.getWeight() * .02 * 1000 * 4);
            } else {
                return 0;
            }
        }
        if (e.getMovementMode() == EntityMovementMode.INF_LEG) {
            return 0;
        } else {
            return (e.getWeight() *.02 * 1000 * 4);
        }
    }
    
    /**
     * @return Tech progression data for this unit, using the campaign faction if the useFactionIntroDate
     *         option is enabled.
     */
    private ITechnology getTechProgression() {
        return getTechProgression(campaign.getTechFaction());
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
        if (campaign.useVariableTechLevel()) {
            return getSimpleLevel(campaign.getCalendar().get(Calendar.YEAR));
        } else {
            return getStaticTechLevel();
        }
    }
    
    public SimpleTechLevel getSimpleTechLevel(int year) {
        if (campaign.useVariableTechLevel()) {
            return getSimpleLevel(year);
        } else {
            return getStaticTechLevel();
        }
    }
    
    public SimpleTechLevel getSimpleTechLevel(int year, boolean clan, int faction) {
        if (campaign.useVariableTechLevel()) {
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
    
}
