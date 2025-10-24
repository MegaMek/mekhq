/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static megamek.common.equipment.MiscType.F_CARGO;
import static megamek.common.units.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM_SUPPORT;
import static megamek.common.units.EntityWeightClass.WEIGHT_SMALL_SUPPORT;
import static megamek.common.units.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static mekhq.campaign.enums.CampaignTransportType.SHIP_TRANSPORT;
import static mekhq.campaign.enums.CampaignTransportType.TACTICAL_TRANSPORT;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_A;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_B;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_C;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_D;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_E;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_F;
import static mekhq.campaign.unit.enums.TransporterType.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.UIManager;

import megamek.Version;
import megamek.client.ui.tileset.EntityImage;
import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.*;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.FuelType;
import megamek.common.icons.Camouflage;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.interfaces.ITechnology;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.units.CrewType;
import megamek.common.util.C3Util;
import megamek.common.weapons.attacks.InfantryAttack;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.persons.PersonCrewAssignmentEvent;
import mekhq.campaign.events.persons.PersonTechAssignmentEvent;
import mekhq.campaign.events.units.UnitArrivedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.AssignmentLogger;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.parts.kfs.KFBoom;
import mekhq.campaign.parts.kfs.KFChargingSystem;
import mekhq.campaign.parts.kfs.KFDriveCoil;
import mekhq.campaign.parts.kfs.KFDriveController;
import mekhq.campaign.parts.kfs.KFFieldInitiator;
import mekhq.campaign.parts.kfs.KFHeliumTank;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekCockpit;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.parts.missing.*;
import mekhq.campaign.parts.protomeks.ProtoMekArmActuator;
import mekhq.campaign.parts.protomeks.ProtoMekArmor;
import mekhq.campaign.parts.protomeks.ProtoMekJumpJet;
import mekhq.campaign.parts.protomeks.ProtoMekLegActuator;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekSensor;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.InfantryGunnerySkills;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.enums.CrewAssignmentState;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is a wrapper class for entity, so that we can add some functionality to it
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Unit implements ITechnology {
    private static final MMLogger LOGGER = MMLogger.create(Unit.class);

    public static final int SITE_IMPROVISED = 0;
    public static final int SITE_FIELD_WORKSHOP = 1;
    public static final int SITE_FACILITY_BASIC = 2;
    public static final int SITE_FACILITY_MAINTENANCE = 3;
    public static final int SITE_FACTORY_CONDITIONS = 4;
    public static final int SITE_UNKNOWN = 5;

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
    // This is the transport assigned for scenario deployments
    private ITransportAssignment tacticalTransportAssignment;
    // This is the unit that will tow this unit
    private ITransportAssignment towTransportAssignment;
    // Contains what kind of transport it is, what units it's carrying, and the
    // remaining capacity
    Set<AbstractTransportedUnitsSummary> transportedUnitsSummaries = new HashSet<>();

    // assignments
    private int forceId;
    protected int scenarioId;

    private final List<Person> drivers;
    private final Set<Person> gunners;
    private final List<Person> vesselCrew;
    // Contains unique Id of each Infantry/BA Entity assigned to this unit as
    // marines
    // Used to calculate marine points (which are based on equipment) as well as
    // Personnel IDs
    // TODO: private Set<Person> marines;
    // this is the id of the tech officer in a super heavy tripod
    private Person techOfficer;
    private Person navigator;
    // this is the id of the tech assigned for maintenance if any
    private Person tech;

    // mothballing variables - if mothball time is not zero then
    // mothballing/activating is in progress
    private int mothballTime;
    private boolean mothballed;

    private double daysSinceMaintenance;
    private double daysActivelyMaintained;
    private double asTechDaysMaintained;
    private int maintenanceMultiplier;

    private Campaign campaign;

    private ArrayList<Part> parts;
    private String lastMaintenanceReport;
    private final ArrayList<PodSpace> podSpace;

    private Refit refit;

    // a made-up person to handle repairs on Large Craft
    private Person engineer;

    private String history;

    // for delivery
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
        this.site = SITE_FACILITY_BASIC;
        this.campaign = c;
        this.parts = new ArrayList<>();
        this.podSpace = new ArrayList<>();
        this.drivers = new ArrayList<>();
        this.gunners = new HashSet<>();
        this.vesselCrew = new ArrayList<>();
        forceId = Force.FORCE_NONE;
        scenarioId = Scenario.S_DEFAULT_ID;
        this.history = "";
        this.lastMaintenanceReport = "";
        this.fluffName = "";
        this.maintenanceMultiplier = 4;
        initializeAllTransportSpace();
        reCalc();
    }

    public static String getDamageStateName(int i) {
        return switch (i) {
            case Entity.DMG_NONE -> "Undamaged";
            case Entity.DMG_LIGHT -> "Light Damage";
            case Entity.DMG_MODERATE -> "Moderate Damage";
            case Entity.DMG_HEAVY -> "Heavy Damage";
            case Entity.DMG_CRIPPLED -> "Crippled";
            default -> "Unknown";
        };
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign c) {
        campaign = c;
    }

    /**
     * A convenience function to tell whether the unit can be acted upon e.g. assigned pilots, techs, repaired, etc.
     *
     */
    public boolean isAvailable() {
        return isAvailable(false);
    }

    /**
     * A convenience function to tell whether the unit can be acted upon e.g. assigned pilots, techs, repaired, etc.
     *
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
            int arrivalTime = getDaysToArrival();

            // This means that, while the item is mothballed, it actually hasn't arrived yet, so we treat it as if
            // had a status of 'in transit'
            if (arrivalTime > 0) {
                return "In transit (" + getDaysToArrival() + " days)";
            } else {
                return "Mothballed";
            }
        } else if (isDeployed()) {
            return "Deployed";
        } else if (!isPresent()) {
            return "In transit (" + getDaysToArrival() + " days)";
        } else if (isRefitting()) {
            double timeLeft = refit.getTimeLeft();
            Person refitTech = refit.getTech();

            double minutesInWorkDay = TECH_WORK_DAY;
            if (refitTech != null) {
                boolean isTechsUseAdmin = getCampaign().getCampaignOptions().isTechsUseAdministration();
                minutesInWorkDay = refitTech.getDailyAvailableTechTime(isTechsUseAdmin);
            }

            int daysLeft = (int) Math.ceil(timeLeft / minutesInWorkDay);
            String dayString = daysLeft == 1 ? "day" : "days";

            return "Refitting" + " (" + daysLeft + ' ' + dayString + ')';
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
            } else if (canTakeMoreDrivers() ||
                             canTakeMoreVesselCrew() ||
                             canTakeTechOfficer() ||
                             canTakeMoreGunners() ||
                             canTakeNavigator()) {
                return CrewAssignmentState.PARTIALLY_CREWED;
            } else {
                return CrewAssignmentState.FULLY_CREWED;
            }
        } else {
            return uncrewed ? CrewAssignmentState.UNSUPPORTED : CrewAssignmentState.UNMAINTAINED;
        }
    }

    /**
     * Like UnitType.getTypeDisplayableName but prepends "Omni" to omni units
     *
     * @return String displayable name with possible "Omni"
     */
    public String getTypeDisplayableNameWithOmni() {
        Entity ourEntity = getEntity();
        int type = ourEntity.getUnitType();
        if (!ourEntity.isOmni()) {
            return UnitType.getTypeDisplayableName(type);
        }
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("Omni");
        if (!(type == UnitType.TANK || type == UnitType.MEK)) {
            toReturn.append(' ');
        }
        toReturn.append(UnitType.getTypeDisplayableName(type));
        return toReturn.toString();
    }

    public void reCalc() {
        // Do Nothing
    }

    /**
     * Initializes the transport capacity. If the campaign transport capacity type doesn't exist yet, try to create it.
     * If it does already exist, let's recalculate the transport capacity instead for all transporters that this Unit
     * has
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     */
    public void initializeShipTransportSpace() {
        // Initialize the bay capacity
        initializeTransportSpace(SHIP_TRANSPORT);
    }

    /**
     * Initializes the transport capacity. If the campaign transport capacity type doesn't exist yet, try to create it.
     * If it does already exist, let's recalculate the transport capacity instead for all transporters that this Unit
     * has
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see CampaignTransportType#TACTICAL_TRANSPORT
     */
    public void initializeTacticalTransportSpace() {
        initializeTransportSpace(CampaignTransportType.TACTICAL_TRANSPORT);
    }

    /**
     * For each CampaignTransportType, initialize the transport capacity. If the campaign transport capacity type
     * doesn't exist yet, try to create it. If it does already exist, let's recalculate the transport capacity instead
     * for all transporters that this Unit has
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see CampaignTransportType
     */
    public void initializeAllTransportSpace() {
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            initializeTransportSpace(campaignTransportType);
        }
    }

    public void clearAllTransportSpace() {
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            clearTransportSpace(campaignTransportType);
        }
    }

    private void clearTransportSpace(CampaignTransportType campaignTransportType) {
        AbstractTransportedUnitsSummary summary = getTransportedUnitsSummary(campaignTransportType);
        if (summary != null) {
            summary.clearTransportCapacityMap();
        }
    }

    private ShipTransportedUnitsSummary getShipTransportedUnitsSummary() {
        return (ShipTransportedUnitsSummary) getTransportedUnitsSummary(SHIP_TRANSPORT);
    }

    private TacticalTransportedUnitsSummary getTacticalTransportedUnitsSummary() {
        return (TacticalTransportedUnitsSummary) getTransportedUnitsSummary(CampaignTransportType.TACTICAL_TRANSPORT);
    }

    /**
     * Initializes the transport capacity. If the campaign transport capacity type doesn't exist yet, try to create it.
     * If it does already exist, let's recalculate the transport capacity instead for all transporters that this Unit
     * has
     *
     * @param campaignTransportType transport type we want to prepare
     */
    public void initializeTransportSpace(CampaignTransportType campaignTransportType) {
        // Initialize the capacity
        if (hasTransportedUnitsType(campaignTransportType)) {
            getTransportedUnitsSummary(campaignTransportType).recalculateTransportCapacity(getEntity().getTransports());
        } else {
            try {
                Constructor<? extends AbstractTransportedUnitsSummary> constructor = campaignTransportType.getTransportedUnitsSummaryType()
                                                                                           .getConstructor(Unit.class);
                addTransportedUnitType(constructor.newInstance(this));
            } catch (NoSuchMethodException e) {
                LOGGER.error("Could not find constructor to initialize transport space for {} Error: {} Cause: {}",
                      campaignTransportType.name(),
                      e.toString(),
                      e.getCause());
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Could not find constructor to initialize transport space for {} Error: {} Cause: {}",
                      campaignTransportType.name(),
                      e,
                      e.getCause());
            }
        }
    }

    /**
     * check to make sure the transported unit summary type exists
     *
     * @param campaignTransportType the transported unit type we're checking
     *
     * @return true if it exists, false if it doesn't
     */
    private boolean hasTransportedUnitsType(CampaignTransportType campaignTransportType) {
        for (AbstractTransportedUnitsSummary transportedUnitsSummary : transportedUnitsSummaries) {
            if (transportedUnitsSummary.getClass() == campaignTransportType.getTransportedUnitsSummaryType()) {
                return true;
            }
        }
        return false;
    }

    /**
     * For the provided campaign transport type, what's this unit's transported units summary
     *
     * @param campaignTransportType what kind of transport type are we checking
     *
     * @return transported units summary of that type, or null
     */
    public @Nullable AbstractTransportedUnitsSummary getTransportedUnitsSummary(
          CampaignTransportType campaignTransportType) {
        for (AbstractTransportedUnitsSummary transportedUnitSummary : transportedUnitsSummaries) {
            if (transportedUnitSummary.getClass() == campaignTransportType.getTransportedUnitsSummaryType()) {
                return transportedUnitSummary;
            }
        }
        return null;
    }

    private void addTransportedUnitType(AbstractTransportedUnitsSummary transportedUnitType) {
        transportedUnitsSummaries.add(transportedUnitType);
    }

    public void setEntity(Entity en) {
        // if there is already an entity, then make sure this
        // one gets some of the same things set
        if (null != this.entity) {
            en.setId(this.entity.getId());
            en.setDuplicateMarker(this.entity.getDuplicateMarker());
            en.generateShortName();
            en.generateDisplayName();
            if (en.getGame() != null) {
                C3Util.copyC3Networks(this.entity, en);
            }
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

    // Generic Transport Methods

    /**
     * For the given transport type, is this unit transporting any other units?
     *
     * @param campaignTransportType Transport Type (Enum) we're checking
     *
     * @return true if it has transported units
     *
     * @see CampaignTransportType
     */
    public boolean hasTransportedUnits(CampaignTransportType campaignTransportType) {
        if (hasTransportedUnitsType(campaignTransportType)) {
            return getTransportedUnitsSummary(campaignTransportType).hasTransportedUnits();
        }
        return false;
    }

    /**
     * For the given transport type, return the set of units it's transporting, or an empty set
     *
     * @param campaignTransportType Transport Type (Enum) we're checking
     *
     * @return Set of Units this transport is carrying, or an empty set
     */
    public Set<Unit> getTransportedUnits(CampaignTransportType campaignTransportType) {
        if (hasTransportedUnits(campaignTransportType)) {
            return getTransportedUnitsSummary(campaignTransportType).getTransportedUnits();
        }
        return new HashSet<>();
    }

    /**
     * For the given campaign transport type, add a unit to our transported units summary
     *
     * @param campaignTransportType Transport Type (Enum) we're checking
     * @param unit                  transported unit we're adding
     */
    void addTransportedUnit(CampaignTransportType campaignTransportType, Unit unit) {
        getTransportedUnitsSummary(campaignTransportType).addTransportedUnit(unit);
    }

    /**
     * For the given campaign transport type, remove a unit to our transported units summary
     *
     * @param campaignTransportType Transport Type (Enum) we're checking
     * @param unit                  transported unit we're adding
     */
    boolean removeTransportedUnit(CampaignTransportType campaignTransportType, Unit unit) {
        return getTransportedUnitsSummary(campaignTransportType).removeTransportedUnit(unit);
    }

    /**
     * Clears the set of units being transported by this unit.
     */
    public void clearTransportedUnits(CampaignTransportType campaignTransportType) {
        getTransportedUnitsSummary(campaignTransportType).clearTransportedUnits();
    }

    /**
     * Does this unit have a transport assignment for this campaign transport type?
     *
     * @param campaignTransportType the transport type (enum) we're interested in
     *
     * @return true if there is a transport assignment of that type, false if not
     */
    public boolean hasTransportAssignment(CampaignTransportType campaignTransportType) {
        if (campaignTransportType.isShipTransport()) {
            return hasTransportShipAssignment();
        } else if (campaignTransportType.isTacticalTransport()) {
            return hasTacticalTransportAssignment();
        } else if (campaignTransportType.isTowTransport()) {
            return getTransportAssignment(campaignTransportType) != null;
        }
        return false;
    }

    /**
     * Returns the transport assignment for the given transport type, or null if none is provided
     *
     * @param campaignTransportType the transport type (enum) we're interested in
     *
     * @return corresponding transport assignment, or null if there isn't one
     */
    public @Nullable ITransportAssignment getTransportAssignment(CampaignTransportType campaignTransportType) {
        if (campaignTransportType.isShipTransport()) {
            return transportShipAssignment;
        } else if (campaignTransportType.isTacticalTransport()) {
            return tacticalTransportAssignment;
        } else if (campaignTransportType.isTowTransport()) {
            return towTransportAssignment;
        }
        return null;
    }

    /**
     * Set this unit's transport assignment to the provided assignment, if possible
     *
     * @param campaignTransportType type (enum) of transport type
     * @param assignment            the assignment we're setting for this unit
     *
     * @see CampaignTransportType
     */
    public void setTransportAssignment(CampaignTransportType campaignTransportType,
          @Nullable ITransportAssignment assignment) {
        if (campaignTransportType.isShipTransport()) {
            if (assignment.getClass().isAssignableFrom(campaignTransportType.getTransportAssignmentType())) {
                setTransportShipAssignment((TransportShipAssignment) assignment);
            }
        } else if (campaignTransportType.isTacticalTransport()) {
            setTacticalTransportAssignment(assignment);
        } else if (campaignTransportType.isTowTransport()) {
            towTransportAssignment = assignment;
        }
    }

    /**
     * Unloads a unit from transport of the provided campaign transport type
     *
     * @param campaignTransportType type (enum) of transport type we want to unload from
     *
     * @return transport the unit was assigned to
     */
    public Unit unloadFromTransport(CampaignTransportType campaignTransportType) {
        if (!hasTransportAssignment(campaignTransportType)) {
            return null;
        }
        Unit oldTransport = getTransportAssignment(campaignTransportType).getTransport();
        oldTransport.getTransportedUnitsSummary(campaignTransportType).unloadTransport(this);
        return oldTransport;
    }

    // End Generic Transport Methods

    // A set of methods for working with transport ship assignment for this unit

    /**
     * Gets a value indicating whether this unit is assigned to a transport ship.
     */
    public boolean hasTransportShipAssignment() {
        return (transportShipAssignment != null);
    }

    /**
     * Gets the transport ship assignment for this unit, or null if this unit is not being transported.
     */
    public @Nullable TransportShipAssignment getTransportShipAssignment() {
        return transportShipAssignment;
    }

    /**
     * Sets the transport ship assignment for this unit.
     *
     * @param assignment The transport ship assignment, or null if this unit is not being transported.
     */
    public void setTransportShipAssignment(@Nullable TransportShipAssignment assignment) {
        transportShipAssignment = assignment;
    }

    /**
     * Gets a value indicating whether this unit is transporting units.
     */
    public boolean hasShipTransportedUnits() {
        return hasTransportedUnits(SHIP_TRANSPORT);
    }

    /**
     * @return the set of units being transported by this unit.
     */
    public Set<Unit> getShipTransportedUnits() {
        return getTransportedUnits(SHIP_TRANSPORT);
    }

    /**
     * Adds a unit to our set of transported units.
     *
     * @param unit The unit being transported by this instance.
     */
    public void addShipTransportedUnit(Unit unit) {
        addTransportedUnit(SHIP_TRANSPORT, unit);
    }

    /**
     * Removes a unit from our set of transported units.
     *
     * @param unit The unit to remove from our set of transported units.
     *
     * @return True if the unit was removed from our bays, otherwise false.
     */
    public boolean removeShipTransportedUnit(Unit unit) {
        return getShipTransportedUnitsSummary().removeTransportedUnit(unit);
    }

    /**
     * Clears the set of units being transported by this unit.
     */
    public void clearShipTransportedUnits() {
        getShipTransportedUnitsSummary().clearTransportedUnits();
    }

    /**
     * Gets a value indicating whether we are transporting any smaller aero units
     */
    public boolean isCarryingSmallerAero() {
        return getShipTransportedUnitsSummary().getTransportedUnits()
                     .stream()
                     .anyMatch(u -> u.getEntity().isAero() &&
                                          !u.getEntity().isLargeCraft() &&
                                          (u.getEntity().getUnitType() != UnitType.SMALL_CRAFT));
    }

    /**
     * Gets a value indicating whether we are transporting any ground units.
     */
    public boolean isCarryingGround() {
        return getShipTransportedUnitsSummary().getTransportedUnits().stream().anyMatch(u -> !u.getEntity().isAero());
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
        if (en instanceof Mek) {
            // center torso bad?? head bad?
            if (en.isLocationBad(Mek.LOC_CENTER_TORSO) || en.isLocationBad(Mek.LOC_HEAD)) {
                return false;
            }
            // engine destruction?
            // cockpit hits
            int engineHits = 0;
            int cockpitHits = 0;
            for (int i = 0; i < en.locations(); i++) {
                engineHits += en.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                cockpitHits += en.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
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
            // unless they are grounded spheroid drop-ships or jump ships
            boolean hasNoWalkMP = en.getWalkMP() <= 0;
            boolean isJumpship = en instanceof Jumpship;
            boolean isGroundedSpheroid = (en instanceof Dropship) && en.isSpheroid() && en.getAltitude() == 0;
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
        if (en instanceof Mek) {
            // you can repair anything so long as one point of CT is left
            if (en.getInternal(Mek.LOC_CENTER_TORSO) <= 0) {
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
     *       need of repair.
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
     * @param loc - an <code>int</code> for the location
     *
     * @return <code>true</code> if the location is destroyed
     */
    public boolean isLocationDestroyed(int loc) {
        if (loc > entity.locations() || loc < 0) {
            return false;
        }
        /*
         * boolean blownOff = entity.isLocationBlownOff(loc);
         * entity.setLocationBlownOff(loc, false);
         * boolean isDestroyed = entity.isLocationBad(loc);
         * entity.setLocationBlownOff(loc, blownOff);
         * return isDestroyed;
         */
        return entity.isLocationTrulyDestroyed(loc);
    }

    public boolean isLocationBreached(int loc) {
        return entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED;
    }

    public boolean hasBadHipOrShoulder(int loc) {
        return entity instanceof Mek &&
                     (entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0 ||
                            entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER, loc) > 0);
    }

    /**
     * Run a diagnostic on this unit
     */
    public void runDiagnostic(boolean checkForDestruction) {
        // need to set up an array of part ids to avoid concurrent modification
        // problems because some updateCondition methods will remove the part and put
        // in a new one
        List<Part> tempParts = new ArrayList<>(parts);

        for (Part part : tempParts) {
            part.updateConditionFromEntity(checkForDestruction);
        }
    }

    private boolean isPartAvailableForRepairs(IPartWork partWork, boolean onlyNotBeingWorkedOn) {
        return !onlyNotBeingWorkedOn || !partWork.isBeingWorkedOn();
    }

    /**
     * Gets a list of every part on a unit which need service (either repair or salvage), including parts currently
     * being worked on.
     */
    public List<IPartWork> getPartsNeedingService() {
        return getPartsNeedingService(false);
    }

    /**
     * Gets a list of parts on a unit which need service (either repair or salvage), optionally excluding parts already
     * being worked on.
     *
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
            if (part.needsFixing()) {
                isPartAvailableForRepairs(part, onlyNotBeingWorkedOn);
                return true;
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.needsFixing()) {
                isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn);
                return true;
            }
        }
        return false;
    }

    public ArrayList<IPartWork> getPartsNeedingFixing(boolean onlyNotBeingWorkedOn) {
        ArrayList<IPartWork> brokenParts = new ArrayList<>();
        for (Part part : parts) {
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
            if (part.isSalvaging()) {
                isPartAvailableForRepairs(part, onlyNotBeingWorkedOn);
                return true;
            }
        }
        for (PodSpace pod : podSpace) {
            if (pod.hasSalvageableParts()) {
                isPartAvailableForRepairs(pod, onlyNotBeingWorkedOn);
                return true;
            }
        }
        return false;
    }

    public ArrayList<IPartWork> getSalvageableParts(boolean onlyNotBeingWorkedOn) {
        ArrayList<IPartWork> salvageParts = new ArrayList<>();
        for (Part part : parts) {
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
            if (part instanceof MissingPart &&
                      part.needsFixing() &&
                      null == ((MissingPart) part).findReplacement(false)) {
                missingParts.add((MissingPart) part);
            }
            // we need to check for armor as well, but this one is funny because we dont
            // want to
            // check per location really, since armor can be used anywhere. So stop after we
            // reach
            // the first Armor needing replacement
            // TODO: we need to adjust for patchwork armor, which can have different armor
            // types by location
            if (!armorFound && part instanceof Armor a) {
                if (a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if (!armorFound && part instanceof ProtoMekArmor a) {
                if (a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
                    missingParts.add(a);
                    armorFound = true;
                }
            }
            if (!armorFound && part instanceof BAArmor a) {
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

    /**
     * A method that returns the value of all missing, but not damaged parts.
     *
     * @return The value of all missing parts.
     */
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
            } else if (part instanceof AmmoBin) {
                value = value.plus(((AmmoBin) part).getValueNeeded());
            } else if (part instanceof Armor) {
                value = value.plus(((Armor) part).getValueNeeded());
            }
        }
        return value;
    }

    /**
     * A method that returns the value of all damaged, but not missing parts.
     *
     * @return The value of all damaged parts.
     */
    public Money getValueOfAllDamagedParts() {
        Money value = Money.zero();

        for (Part part : getParts()) {
            if (part.needsFixing() && !(part instanceof Armor)) {
                value = value.plus(part.getActualValue());
            }
        }
        return value;
    }

    public void removePart(Part part) {
        parts.remove(part);
    }

    public boolean hasPilot() {
        return null != entity.getCrew();
    }

    public String getPilotDesc() {
        if (hasPilot()) {
            return entity.getCrew().getName() +
                         ' ' +
                         entity.getCrew().getGunnery() +
                         '/' +
                         entity.getCrew().getPiloting();
        }
        return "NO PILOT";
    }

    public TargetRoll getSiteMod() {
        return switch (site) {
            case SITE_IMPROVISED -> new TargetRoll(2, "Improvised");
            case SITE_FIELD_WORKSHOP -> new TargetRoll(1, "Field Workshop");
            case SITE_FACILITY_BASIC -> new TargetRoll(0, "Facility - Basic");
            case SITE_FACILITY_MAINTENANCE -> new TargetRoll(-2, "Facility - Maintenance");
            case SITE_FACTORY_CONDITIONS -> new TargetRoll(-4, "Factory Conditions");
            default -> new TargetRoll(0, "Unknown Location");
        };
    }

    public static String getSiteName(int loc) {
        return switch (loc) {
            case SITE_IMPROVISED -> "Improvised";
            case SITE_FIELD_WORKSHOP -> "Field Workshop";
            case SITE_FACILITY_BASIC -> "Facility - Basic";
            case SITE_FACILITY_MAINTENANCE -> "Facility - Maintenance";
            case SITE_FACTORY_CONDITIONS -> "Factory Conditions";
            default -> "Unknown";
        };
    }

    public static String getSiteToolTipText(int loc) {
        return switch (loc) {
            case SITE_IMPROVISED ->
                  "Battle-worn structures and improvised tools; survival depends on ingenuity and determination. Barely enough to keep units operational.";
            case SITE_FIELD_WORKSHOP ->
                  "Mobile units with essential gear; repairs are quick but rudimentary. Vital for frontline operations.";
            case SITE_FACILITY_BASIC ->
                  "Reliable shelter with all necessary tools for routine maintenance. Adequate but not exceptional.";
            case SITE_FACILITY_MAINTENANCE ->
                  "Well-equipped base with specialized machinery. Enables thorough repairs and maintenance, vital for prolonged campaigns.";
            case SITE_FACTORY_CONDITIONS ->
                  "State-of-the-art facility with advanced equipment. Peak efficiency for production and maintenance, ensuring top performance.";
            default -> "";
        };
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

    /**
     * Validates the deployment readiness of the unit.
     *
     * <p>
     * This method checks multiple conditions to determine whether the unit is deployable. If the unit is not
     * deployable, a descriptive error message is returned indicating the reason for its ineligibility. If the unit
     * passes all validations, {@code null} is returned, implying it is ready for deployment.
     * <p>
     * <p>
     * Deployment checks performed:
     * <ul>
     * <li>If the unit is not functional, it cannot be deployed.</li>
     * <li>If the unit is unmanned and is not an unmanned trailer, it cannot be
     * deployed.</li>
     * <li>If the unit is in the process of being refitted, it cannot be
     * deployed.</li>
     * <li>If the unit is a tank and does not have the required crew size, it cannot
     * be deployed.</li>
     * <li>If the unit is a BattleArmor unit with empty suits, it cannot be deployed
     * until these
     * are filled or salvaged.</li>
     * </ul>
     *
     * @return A descriptive {@code String} error message if the unit cannot be deployed, or {@code null} if the unit is
     *       deployable.
     */
    // TODO: Add support for advanced medical
    public @Nullable String checkDeployment() {
        if (!isFunctional()) {
            return "unit is not functional";
        }
        if (isUnmanned() && !(isUnmannedTrailer())) {
            return "unit has no pilot";
        }
        if (isRefitting()) {
            return "unit is being refit";
        }
        if (entity instanceof Tank && getActiveCrew().size() < getFullCrewSize()) {
            return "This vehicle requires a crew of " + getFullCrewSize();
        }
        // Taharqa: I am not going to allow BattleArmor units with unmanned suits to
        // deploy. It is
        // possible to hack this to work in MM, but it becomes a serious problem when
        // the unit becomes
        // a total loss because the unmanned suits are also treated as destroyed. I
        // tried hacking something
        // together in ResolveScenarioTracker and decided that it was not right. If
        // someone wants to deploy
        // a non-full strength BA unit, they can salvage the suits that are unmanned and
        // then they can deploy
        // it
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
     * @return Have to make one here because the one in MegaMek only returns true if operable
     */
    public boolean hasTSM() {
        for (Mounted<?> mEquip : entity.getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (null != miscType && miscType.hasFlag(MiscType.F_TSM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there is at least one missing critical slot for this system in the given location
     */
    public boolean isSystemMissing(int system, int loc) {
        for (int i = 0; i < entity.getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot ccs = entity.getCritical(loc, i);
            if ((ccs != null) &&
                      (ccs.getType() == CriticalSlot.TYPE_SYSTEM) &&
                      (ccs.getIndex() == system) &&
                      ccs.isMissing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Number of slots doomed, missing or destroyed in all locations
     *
     */
    public int getHitCriticalSlots(int type, int index) {
        int hits = 0;
        for (int loc = 0; loc < entity.locations(); loc++) {
            hits += getHitCriticalSlots(type, index, loc);
        }
        return hits;
    }

    /**
     * Number of slots doomed, missing or destroyed in a location
     */
    public int getHitCriticalSlots(int type, int index, int loc) {
        int hits = 0;
        Mounted<?> m = null;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = entity.getEquipment(index);
        }

        int numberOfCriticalSlots = entity.getNumberOfCriticalSlots(loc);
        for (int i = 0; i < numberOfCriticalSlots; i++) {
            CriticalSlot ccs = entity.getCritical(loc, i);

            // Check to see if this crit mounts the supplied item
            // For systems, we can compare the index, but for equipment we
            // need to get the Mounted that is mounted in that index and
            // compare types. Superheavies may have two Mounted in each crit
            if ((ccs != null) && (ccs.getType() == type)) {
                if (ccs.isDestroyed()) {
                    if ((type == CriticalSlot.TYPE_SYSTEM) && (ccs.getIndex() == index)) {
                        hits++;
                    } else if ((type == CriticalSlot.TYPE_EQUIPMENT) &&
                                     (null != m) &&
                                     (m.equals(ccs.getMount()) || m.equals(ccs.getMount2()))) {
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    public void damageSystem(int type, int equipmentNum, int hits) {
        // make sure we take note of existing hits to start and as we cycle through
        // locations
        int existingHits = getHitCriticalSlots(type, equipmentNum);
        int neededHits = max(0, hits - existingHits);
        int usedHits = 0;
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            if (neededHits > usedHits) {
                usedHits += damageSystem(type, equipmentNum, loc, neededHits - usedHits);
            }
        }
    }

    public int damageSystem(int type, int equipmentNum, int loc, int hits) {
        int numHits = 0;
        for (int i = 0; i < getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted<?> mounted = getEntity().getEquipment(equipmentNum);
            Mounted<?> m1 = cs.getMount();
            Mounted<?> m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                if (numHits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    cs.setRepairable(true);
                    cs.setMissing(false);
                    numHits++;
                }
            }
        }
        return numHits;
    }

    public void destroySystem(int type, int equipmentNum) {
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            destroySystem(type, equipmentNum, loc);
        }
    }

    public void destroySystem(int type, int equipmentNum, int loc) {
        for (int i = 0; i < getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted<?> mounted = getEntity().getEquipment(equipmentNum);
            Mounted<?> m1 = cs.getMount();
            Mounted<?> m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                cs.setHit(true);
                cs.setDestroyed(true);
                cs.setRepairable(false);
                cs.setMissing(false);
            }
        }
    }

    public void destroySystem(int type, int equipmentNum, int loc, int hits) {
        int numHits = 0;
        for (int i = 0; i < getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted<?> mounted = getEntity().getEquipment(equipmentNum);
            Mounted<?> m1 = cs.getMount();
            Mounted<?> m2 = cs.getMount2();
            if (cs.getIndex() == equipmentNum || (mounted != null && (mounted.equals(m1) || mounted.equals(m2)))) {
                if (numHits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    cs.setRepairable(false);
                    cs.setMissing(false);
                    numHits++;
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
        for (int i = 0; i < getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot cs = getEntity().getCritical(loc, i);
            // ignore empty & system slots
            if ((cs == null) || (cs.getType() != type)) {
                continue;
            }
            Mounted<?> mounted = getEntity().getEquipment(equipmentNum);
            Mounted<?> m1 = cs.getMount();
            Mounted<?> m2 = cs.getMount2();
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

    public Money getSellValue() {
        Money partsValue = Money.zero();

        partsValue = partsValue.plus(parts.stream()
                                           .map(x -> x.getActualValue().multipliedBy(x.getQuantity()))
                                           .collect(Collectors.toList()));

        // we use an alternative method of getting sell value for infantry
        if (entity instanceof Infantry) {
            Money unitCost = Money.of(entity.getAlternateCost());
            double[] usedPartPriceMultipliers = campaign.getCampaignOptions().getUsedPartPriceMultipliers();

            return switch (this.getQuality()) {
                case QUALITY_A -> unitCost.multipliedBy(usedPartPriceMultipliers[0]);
                case QUALITY_B -> unitCost.multipliedBy(usedPartPriceMultipliers[1]);
                case QUALITY_C -> unitCost.multipliedBy(usedPartPriceMultipliers[2]);
                case QUALITY_D -> unitCost.multipliedBy(usedPartPriceMultipliers[3]);
                case QUALITY_E -> unitCost.multipliedBy(usedPartPriceMultipliers[4]);
                case QUALITY_F -> unitCost.multipliedBy(usedPartPriceMultipliers[5]);
            };
        }

        // We need to adjust this for equipment that doesn't show up as parts
        // Docking collars, Grav decks, KF Drive - Now parts
        // Drive unit - see SpacecraftEngine
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            if (entity instanceof SmallCraft) {
                // JS/SS/WS Bridge, Computer - see CombatInformationCenter
                // bridge
                partsValue = partsValue.plus(200000.0 + 10.0 * entity.getWeight());
                // computer
                partsValue = partsValue.plus(200000.0);
            }
            // Jump Sail and KF drive support systems
            if ((entity instanceof Jumpship js) && !(entity instanceof SpaceStation)) {
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
                int bayDoors = 0;
                Money bayCost = Money.zero();
                for (Bay next : js.getTransportBays()) {
                    bayDoors += next.getDoors();
                    if ((next instanceof MekBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                        bayCost = bayCost.plus(20000.0 * next.getCapacity());
                    }
                    if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                        bayCost = bayCost.plus(20000.0 * next.getCapacity());
                    }
                }

                partsValue = partsValue.plus(bayCost.plus(bayDoors * 1000.0));

                // lifeboats and escape pods
                partsValue = partsValue.plus((js.getLifeBoats() + js.getEscapePods()) * 5000.0);
            }
        }

        // ProtoMeks: heat sinks can't be hit
        if (entity instanceof ProtoMek) {
            int sinks = 0;
            for (Mounted<?> mount : entity.getWeaponList()) {
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

    /**
     * Calculates the total cargo capacity of the entity, considering the usable capacities of transport bays and
     * mounted equipment designated for cargo. The calculation is performed only if the entity is fully crewed.
     *
     * <p>
     * The total cargo capacity is derived from the following:
     * </p>
     * <ul>
     * <li>The usable capacities of transport bays ({@link CargoBay},
     * {@link RefrigeratedCargoBay},
     * or {@link InsulatedCargoBay}), adjusted for existing damage.</li>
     * <li>The tonnage of mounted equipment tagged with the {@code F_CARGO} flag,
     * provided
     * the equipment is operable and located in non-destroyed sections of the
     * entity.</li>
     * </ul>
     *
     * <p>
     * <strong>Special Conditions:</strong>
     * </p>
     * <ul>
     * <li>The method returns {@code 0.0} if the entity is not fully crewed.</li>
     * <li>Bays or mounted equipment damaged beyond usability are excluded from the
     * total.</li>
     * <li>Only equipment in valid (non-destroyed) sections of the entity are
     * considered.</li>
     * </ul>
     *
     * @return The total cargo capacity of the entity if fully crewed; otherwise, {@code 0.0}.
     */
    public double getCargoCapacity() {
        if (!isFullyCrewed()) {
            return 0.0;
        }

        double capacity = 0.0;
        double cargoBayCapacity = -getTotalWeightOfUnitsAssignedToBeTransported(TACTICAL_TRANSPORT, CARGO_BAY);

        // Add capacities from transport bays
        for (Bay bay : entity.getTransportBays()) {
            double bayCapacity = bay.getCapacity();
            double bayDamage = bay.getBayDamage();

            double actualCapacity = max(0, bayCapacity - bayDamage);

            if (bay instanceof CargoBay) {
                cargoBayCapacity += actualCapacity;
                continue;
            }

            if (bay instanceof RefrigeratedCargoBay) {
                capacity += actualCapacity;
                continue;
            }

            if (bay instanceof InsulatedCargoBay) {
                capacity += actualCapacity;
            }
        }

        capacity += max(0, cargoBayCapacity);

        // Add capacities from mounted equipment
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_CARGO)) {
                // isOperable doesn't check if the mounted location still exists, so we check
                // for
                // that first.
                if (!mounted.getEntity().isLocationBad(mounted.getLocation()) && (mounted.isOperable())) {
                    capacity += mounted.getTonnage();
                }
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
     * Calculates the total weight of all units assigned to be transported in this unit for a specific transport type
     * and transporter type.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Finds all units currently assigned to be transported by this unit with the specified transport type.</li>
     *     <li>For each transported unit, checks if it is assigned to a transporter of the specified type.</li>
     *     <li>Adds the full weight of each matching transported unit to the total.</li>
     * </ul>
     *
     * @param transportType   The transport type to match when retrieving transported units.
     * @param transporterType The transporter type to filter assignments (only units assigned to this type are
     *                        considered).
     *
     * @return The sum weight of all units assigned to this unit via the given transport and transporter type.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public double getTotalWeightOfUnitsAssignedToBeTransported(CampaignTransportType transportType,
          TransporterType transporterType) {
        AbstractTransportedUnitsSummary transportSummary = getTransportedUnitsSummary(transportType);
        double cargoCapacityUsage = 0;

        if (transportSummary != null) {
            for (Unit transportedUnit : transportSummary.getTransportedUnits()) {
                ITransportAssignment assignment = transportedUnit.getTransportAssignment(transportType);
                if (assignment != null) {
                    if (assignment.getTransporterType() == transporterType) {
                        cargoCapacityUsage += transportedUnit.getEntity().getWeight();
                    }
                }
            }
        }

        return cargoCapacityUsage;
    }

    /**
     * Convenience method to call the right capacity getter based on unit type and weight
     *
     * @param unitType   integer obtained from a unit's entity that denotes its type (mek, tank, etc.)
     * @param unitWeight double Weight in tons of the unit's entity. Important for tanks and infantry
     */
    public double getCorrectBayCapacity(int unitType, double unitWeight) {
        switch (unitType) {
            case UnitType.MEK:
                return getCurrentMekCapacity();
            case UnitType.AEROSPACE_FIGHTER:
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
                LOGGER.error("No transport bay defined for specified unit type.");
                return 0;
        }
    }

    /**
     * Convenience method to call the right capacity update based on unit type When updating capacity, this method is
     * concerned primarily with ensuring that space isn't released beyond the unit's maximum. Checks are made to keep
     * from going below 0 before we ever get here.
     *
     * @param unitType   integer obtained from a unit's entity that denotes its type (mek, tank, etc.)
     * @param unitWeight double Weight in tons of the unit's entity. Important for infantry
     * @param addUnit    boolean value that determines whether to add or subtract 1 from bay capacity
     * @param bayNumber  integer representing the bay number that has been assigned to a cargo entity
     */
    public void updateBayCapacity(int unitType, double unitWeight, boolean addUnit, int bayNumber) {
        // Default. Consume 1 bay of the appropriate type
        int amount = -1;
        if (addUnit) {
            // Return 1 bay/cubicle to the transport's pool
            amount = 1;
        }
        switch (unitType) {
            // Be sure that when releasing bay space, the transport does not go over its
            // normal maximum
            case UnitType.MEK:
                setMekCapacity(Math.min((getCurrentMekCapacity() + amount), getMekCapacity()));
                break;
            case UnitType.AEROSPACE_FIGHTER:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
                // Use the assigned bay number to determine if we need to update ASF or Small
                // Craft capacity
                Bay aeroBay = getEntity().getBayById(bayNumber);
                if (aeroBay != null) {
                    if (BayType.getTypeForBay(aeroBay).equals(BayType.FIGHTER)) {
                        setASFCapacity(Math.min((getCurrentASFCapacity() + amount), getASFCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(aeroBay).equals(BayType.SMALL_CRAFT)) {
                        setSmallCraftCapacity(Math.min((getCurrentSmallCraftCapacity() + amount),
                              getSmallCraftCapacity()));
                        break;
                    } else {
                        // This shouldn't happen
                        LOGGER.error("Fighter got assigned to a non-ASF, non-SC bay.");
                        break;
                    }
                }
                // This shouldn't happen either
                LOGGER.error("Fighter's bay number assignment produced a null bay");
                break;
            case UnitType.DROPSHIP:
                setDocks(Math.min((getCurrentDocks() + amount), getDocks()));
                break;
            case UnitType.SMALL_CRAFT:
                setSmallCraftCapacity(Math.min((getCurrentSmallCraftCapacity() + amount), getSmallCraftCapacity()));
                break;
            case UnitType.INFANTRY:
                // Infantry bay capacities are in tons, so consumption depends on platoon type
                setInfantryCapacity(Math.min((getCurrentInfantryCapacity() + (amount * unitWeight)),
                      getInfantryCapacity()));
                break;
            case UnitType.BATTLE_ARMOR:
                setBattleArmorCapacity(Math.min((getCurrentBattleArmorCapacity() + amount), getBattleArmorCapacity()));
                break;
            case UnitType.TANK:
            case UnitType.NAVAL:
            case UnitType.VTOL:
                // Use the assigned bay number to determine if we need to update ASF or Small
                // Craft capacity
                Bay tankBay = getEntity().getBayById(bayNumber);
                if (tankBay != null) {
                    if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_LIGHT)) {
                        setLightVehicleCapacity(Math.min((getCurrentLightVehicleCapacity() + amount),
                              getLightVehicleCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_HEAVY)) {
                        setHeavyVehicleCapacity(Math.min((getCurrentHeavyVehicleCapacity() + amount),
                              getHeavyVehicleCapacity()));
                        break;
                    } else if (BayType.getTypeForBay(tankBay).equals(BayType.VEHICLE_SH)) {
                        setSuperHeavyVehicleCapacity(Math.min((getCurrentSuperHeavyVehicleCapacity() + amount),
                              getSuperHeavyVehicleCapacity()));
                        break;
                    } else {
                        // This shouldn't happen
                        LOGGER.error("Vehicle got assigned to a non-light/heavy/super heavy vehicle bay.");
                        break;
                    }
                }
                // This shouldn't happen either
                LOGGER.error("Vehicle's bay number assignment produced a null bay");
                break;
        }
    }

    public int getDocks() {
        return getEntity().getDocks();
    }

    /**
     * Get only collars to which a DropShip has been assigned Capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public int getCurrentDocks() {
        return (int) Math.floor(getShipTransportedUnitsSummary().getCurrentTransportCapacity(DOCKING_COLLAR));
    }

    /**
     * Used to assign a Drop-ship to a collar on a specific Jumpship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for a ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this, use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setDocks(int docks) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(DOCKING_COLLAR, docks);
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

    /**
     * Get only bays to which a light tank has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks a ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentLightVehicleCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(LIGHT_VEHICLE_BAY);
    }

    /**
     * Used to assign a tank to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for a ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setLightVehicleCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(LIGHT_VEHICLE_BAY, bays);
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

    /**
     * Get only bays to which a heavy tank has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks a ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentHeavyVehicleCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(HEAVY_VEHICLE_BAY);
    }

    /**
     * Used to assign a tank to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for a ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setHeavyVehicleCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(HEAVY_VEHICLE_BAY, bays);
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

    /**
     * Get only bays to which a super heavy tank has been assigned
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks a ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentSuperHeavyVehicleCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(SUPER_HEAVY_VEHICLE_BAY);
    }

    /**
     * Used to assign a tank to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for a ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setSuperHeavyVehicleCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(SUPER_HEAVY_VEHICLE_BAY, bays);
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

    /**
     * Get only bays to which a ba squad has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks a ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentBattleArmorCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(BATTLE_ARMOR_BAY);
    }

    /**
     * Used to assign a ba squad to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for a ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setBattleArmorCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(BATTLE_ARMOR_BAY, bays);
    }

    public double getInfantryCapacity() {
        double bays = 0;
        for (Bay bay : getEntity().getTransportBays()) {
            if (bay instanceof InfantryBay) {
                bays += bay.getCapacity() / ((InfantryBay) bay).getPlatoonType().getWeight();
            }
        }
        return bays;
    }

    /**
     * Return the unused tonnage of any conventional infantry bays
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks a ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentInfantryCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(INFANTRY_BAY);
    }

    /**
     * Used to assign an infantry unit to a bay on a specific transport ship in the TOE Tonnage consumed depends on the
     * platoon/squad weight
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setInfantryCapacity(double tonnage) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(INFANTRY_BAY, tonnage);
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

    /**
     * Get only bays to which a fighter has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentASFCapacity() {
        return getCurrentShipTransportCapacity(ASF_BAY);
    }

    /**
     * Used to assign a fighter to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setASFCapacity(double bays) {
        setCurrentShipTransportCapacity(ASF_BAY, bays);
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

    /**
     * Get only bays to which a small craft has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentSmallCraftCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(SMALL_CRAFT_BAY);
    }

    /**
     * Used to assign a small craft to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setSmallCraftCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(SMALL_CRAFT_BAY, bays);
    }

    public double getMekCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof MekBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    /**
     * Get only bays to which a mek has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentMekCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(MEK_BAY);
    }

    /**
     * Used to assign a mek or LAM to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setMekCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(MEK_BAY, bays);
    }

    public double getProtoMekCapacity() {
        double bays = 0;
        for (Bay b : getEntity().getTransportBays()) {
            if (b instanceof ProtoMekBay) {
                bays += b.getCapacity();
            }
        }
        return bays;
    }

    /**
     * Get only bays to which a protomek has been assigned
     *
     * @return capacity
     *
     * @see Unit#getCurrentTransportCapacity(CampaignTransportType, TransporterType)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only checks ship transport type, use
     *       getCurrentTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) to replicate this
     */
    @Deprecated(since = "0.50.04")
    public double getCurrentProtoMekCapacity() {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(PROTO_MEK_BAY);
    }

    /**
     * Used to assign a ProtoMek to a bay on a specific transport ship in the TOE
     *
     * @see Unit#initializeTransportSpace(CampaignTransportType)
     * @see Unit#setCurrentShipTransportCapacity(TransporterType, double)
     * @see CampaignTransportType#SHIP_TRANSPORT
     * @deprecated this only sets for ship transport type. Transport Capacities should not be manually updated with
     *       this, it should happen inside any loading flows. If you really need to replicate this use
     *       setCurrentShipTransportCapacity(CampaignTransportType.SHIP_TRANSPORT, appropriate bay class) - but you
     *       probably don't want to do that
     */
    @Deprecated(since = "0.50.04")
    public void setProtoCapacity(double bays) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(PROTO_MEK_BAY, bays);
    }

    /**
     * Bay loading utility used when assigning units to bay-equipped transport units For each passed-in unit, this will
     * find the first available, transport bay and set both the target bay and the UUID of the transport ship. Once in
     * the MM lobby, this data will be used to actually load the unit into a bay on the transport.
     *
     * @param transporterType type (Enum) of Transporter to transport the units in
     * @param units           Vector of units that we wish to load into this transport
     */
    public Set<Unit> loadShipTransport(TransporterType transporterType, Set<Unit> units) {
        Vector<Unit> unitsVector = new Vector<>(units);

        return getShipTransportedUnitsSummary().loadTransportShip(unitsVector, transporterType);
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport units and/or moving them to a new
     * transport
     *
     * @param unit The unit that we wish to unload from this transport
     */
    public void unloadFromTransportShip(Unit unit) {
        getShipTransportedUnitsSummary().unloadFromTransportShip(unit);
    }

    // Transport Assignments

    /**
     * Returns the current capacity
     *
     * @param transporterType class of Transporter
     *
     * @return capacity
     */
    public double getCurrentShipTransportCapacity(TransporterType transporterType) {
        return getShipTransportedUnitsSummary().getCurrentTransportCapacity(transporterType);
    }

    /**
     * Gets a value indicating whether this unit is assigned to transport.
     *
     * @return true if this unit has a tacticalTransportAssignment that isn't null
     */
    public boolean hasTacticalTransportAssignment() {
        return (tacticalTransportAssignment != null);
    }

    /**
     * Gets the tactical transport assignment for this unit, or null if this unit is not being transported.
     *
     * @return transport assignment
     */
    public @Nullable ITransportAssignment getTacticalTransportAssignment() {
        return tacticalTransportAssignment;
    }

    /**
     * Sets the transport assignment for this unit.
     *
     * @param assignment The transport ship assignment, or null if this unit is not being transported.
     */
    public void setTacticalTransportAssignment(@Nullable ITransportAssignment assignment) {
        tacticalTransportAssignment = assignment;
    }

    /**
     * Returns the current capacity
     *
     * @param campaignTransportType type (enum) being checked
     * @param transporterType       class of Transporter
     *
     * @return remaining capacity
     *
     * @see CampaignTransportType
     */
    public double getCurrentTransportCapacity(CampaignTransportType campaignTransportType,
          TransporterType transporterType) {
        return getTransportedUnitsSummary(campaignTransportType).getCurrentTransportCapacity(transporterType);
    }

    /**
     * Set the transport capacity for the specified transporter type to a specific capacity
     *
     * @param transporterType type (Enum) of transporter we want to set the capacity
     * @param capacity        how much this transporter should be able to transport
     */
    public void setCurrentShipTransportCapacity(TransporterType transporterType, double capacity) {
        getShipTransportedUnitsSummary().setCurrentTransportCapacity(transporterType, capacity);
    }

    /**
     * For the provided campaign transport type (enum), return the transporters this unit
     *
     * @param campaignTransportType type (enum) of campaign transport
     *
     * @return set of Transporter types (class)
     */
    public Set<TransporterType> getTransportCapabilities(CampaignTransportType campaignTransportType) {
        return getTransportedUnitsSummary(campaignTransportType).getTransportCapabilities();
    }

    /**
     * Does this unit have any assigned tactical transported units?
     *
     * @return true if the unit is assigned tactical transports
     */
    public boolean hasTacticalTransportedUnits() {
        if (hasTransportedUnitsType(CampaignTransportType.TACTICAL_TRANSPORT)) {
            return getTacticalTransportedUnitsSummary().hasTransportedUnits();
        }
        return false;
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport units and/or moving them to a new
     * transport
     *
     * @param transportedUnit The unit that we wish to unload from this transport
     */
    public void unloadTacticalTransport(Unit transportedUnit) {
        getTacticalTransportedUnitsSummary().unloadFromTransport(transportedUnit);
    }

    /**
     * Transporter loading utility used when assigning units to transport units For each passed-in unit, this will
     * assign the unit to the specified bay, or the type of Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param transporterType type (Enum) of bay or Transporter
     * @param units           units being loaded
     *
     * @return the old transports of the units, or an empty set if none
     */
    public Set<Unit> loadTacticalTransport(TransporterType transporterType, Set<Unit> units) {
        return getTacticalTransportedUnitsSummary().loadTransport(units, null, transporterType);
    }

    /**
     * Trailer hitching utility used when assigning a trailer to a tractor. It's a bit different from normal loading so
     * it gets its own method. This should be called on the towing unit (or towing entity) - the unit that is
     * specifically pulling the transportedUnit. Do not pass in the tractor that is pulling the entire "train" unless
     * you want the transportedUnit specifically attached to the tractor.
     *
     * @param transportedUnit     trailer Unit that should be towed
     * @param transportedLocation specific hitch the trailer should be attached to
     * @param transporterType     type of transporter towing the trailer, should probably be a TANK_TRAILER_HITCH
     *
     * @return original towing unit (Unit) that was pulling the transportedUnit
     *
     * @see TransporterType#TANK_TRAILER_HITCH
     */
    public @Nullable Unit towTrailer(Unit transportedUnit, @Nullable Transporter transportedLocation,
          TransporterType transporterType) {
        return ((TowTransportedUnitsSummary) getTransportedUnitsSummary(CampaignTransportType.TOW_TRANSPORT)).towTrailer(
              transportedUnit,
              transportedLocation,
              transporterType);
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit This removes all units assigned to the
     * transport from it
     */
    public void unloadTransport(CampaignTransportType campaignTransportType) {
        getTransportedUnitsSummary(campaignTransportType).clearTransportedUnits(campaign);
    }
    // End Transport Assignments

    public Money getBuyCost() {
        Money cost = Money.of((getEntity() instanceof Infantry) ?
                                    getEntity().getAlternateCost() :
                                    getEntity().getCost(false));

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

        // If this entity is assigned to a transport ship, write that
        if (hasTransportShipAssignment()) {
            pw.println(MHQXMLUtility.indentStr(indent) +
                             "<transportShip id=\"" +
                             getTransportShipAssignment().getTransportShip().getId() +
                             "\" baynumber=\"" +
                             getTransportShipAssignment().getBayNumber() +
                             "\"/>");
        }

        for (Unit unit : getShipTransportedUnits()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportedUnitId", unit.getId());
        }
        // START new transports
        // If this entity is assigned to a transport, write that
        if (hasTacticalTransportAssignment()) {
            String transportedLocation = "";
            if (getTacticalTransportAssignment().hasTransportedLocation()) {
                transportedLocation += " transportedLocation=\"" +
                                             getTacticalTransportAssignment().getTransportedLocation() +
                                             "\"";
            } else if (getTacticalTransportAssignment().hasTransporterType()) {
                transportedLocation += " transporterType=\"" +
                                             getTacticalTransportAssignment().getTransporterType() +
                                             "\"";
            }
            pw.println(MHQXMLUtility.indentStr(indent) +
                             "<transportAssignment id=\"" +
                             getTacticalTransportAssignment().getTransport().getId() +
                             "\"" +
                             transportedLocation +
                             "/>");
        }

        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            // Some transports were set up before this and use a different saving & loading
            // pattern. Let's ignore them.
            if (campaignTransportType.equals(SHIP_TRANSPORT)) {
                continue;
            }

            if (hasTransportAssignment(campaignTransportType)) {
                String transportedLocation = "";
                if (getTransportAssignment(campaignTransportType).hasTransportedLocation()) {
                    transportedLocation += " transportedLocation=\"" +
                                                 getTransportAssignment(campaignTransportType).getTransportedLocation() +
                                                 "\"";
                } else if (getTransportAssignment(campaignTransportType).hasTransporterType()) {
                    transportedLocation += " transporterType=\"" +
                                                 getTransportAssignment(campaignTransportType).getTransporterType() +
                                                 "\"";
                }
                pw.println(MHQXMLUtility.indentStr(indent) +
                                 "<transportAssignment id=\"" +
                                 getTransportAssignment(campaignTransportType).getTransport().getId() +
                                 "\"" +
                                 transportedLocation +
                                 " campaignTransportType=\"" +
                                 campaignTransportType +
                                 "\"/>");
            }

            for (Unit unit : getTransportedUnits(campaignTransportType)) {
                pw.println(MHQXMLUtility.indentStr(indent) +
                                 "<transportedUnit id=\"" +
                                 unit.getId() +
                                 "\" " +
                                 "campaignTransportType=\"" +
                                 campaignTransportType +
                                 "\"/>");

            }
        }

        // END new transports
        // Salvage status
        if (salvaged) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvaged", true);
        }

        if (site != SITE_FACILITY_BASIC) {
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

        if (asTechDaysMaintained > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "asTechDaysMaintained", asTechDaysMaintained);
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

        if ((lastMaintenanceReport != null) &&
                  !lastMaintenanceReport.isEmpty() &&
                  getCampaign().getCampaignOptions().isCheckMaintenance()) {
            pw.println(MHQXMLUtility.indentStr(indent) +
                             "<lastMaintenanceReport><![CDATA[" +
                             lastMaintenanceReport +
                             "]]></lastMaintenanceReport>");

        }

        if (mothballInfo != null) {
            mothballInfo.writeToXML(pw, indent);
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unit");
    }

    public static Unit generateInstanceFromXML(final Node wn, final Version version, final Campaign campaign) {
        Unit retVal = new Unit();
        NamedNodeMap attrs = wn.getAttributes();
        Node idNode = attrs.getNamedItem("id");

        retVal.id = UUID.fromString(idNode.getTextContent());

        // Temp storage for used bay capacities
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
                    retVal.daysActivelyMaintained = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysSinceMaintenance")) {
                    retVal.daysSinceMaintenance = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mothballTime")) {
                    retVal.mothballTime = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("asTechDaysMaintained")) {
                    retVal.asTechDaysMaintained = Double.parseDouble(wn2.getTextContent());
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
                    retVal.addShipTransportedUnit(new UnitRef(UUID.fromString(wn2.getTextContent())));
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
                } else if (wn2.getNodeName().equalsIgnoreCase("mekCapacity")) {
                    retVal.setMekCapacity(Double.parseDouble(wn2.getTextContent()));
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
                } else if (wn2.getNodeName().equalsIgnoreCase("transportAssignment")) {
                    NamedNodeMap attributes = wn2.getAttributes();
                    CampaignTransportType campaignTransportType;
                    if (attributes.getNamedItem("campaignTransportType") != null) {
                        campaignTransportType = CampaignTransportType.valueOf(attributes.getNamedItem(
                              "campaignTransportType").getTextContent());
                    } else {
                        // Tactical transports were added before the campaignTransportType attribute
                        // was. Assume it's a tactical transport.
                        campaignTransportType = CampaignTransportType.TACTICAL_TRANSPORT;
                    }
                    UUID id = UUID.fromString(attributes.getNamedItem("id").getTextContent());

                    if (attributes.getNamedItem("transportedLocation") != null) {
                        int transportedLocationHash = Integer.parseInt(attributes.getNamedItem("transportedLocation")
                                                                             .getTextContent());
                        retVal.setTransportAssignment(campaignTransportType,
                              new TransportAssignment(new UnitRef(id), transportedLocationHash));
                    } else if (attributes.getNamedItem("transporterType") != null) {
                        try {
                            TransporterType transporterType = TransporterType.valueOf((attributes.getNamedItem(
                                  "transporterType").getTextContent()));
                            retVal.setTransportAssignment(campaignTransportType,
                                  new TransportAssignment(new UnitRef(id), transporterType));
                        } catch (IllegalArgumentException e) {
                            LOGGER.error(e, "Could not find transporter type.");
                            retVal.setTransportAssignment(campaignTransportType,
                                  new TransportAssignment(new UnitRef(id)));
                        }
                    } else {
                        retVal.setTransportAssignment(campaignTransportType, new TransportAssignment(new UnitRef(id)));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("transportedUnit")) {
                    NamedNodeMap attributes = wn2.getAttributes();
                    CampaignTransportType campaignTransportType = CampaignTransportType.valueOf(attributes.getNamedItem(
                          "campaignTransportType").getTextContent());
                    retVal.addTransportedUnit(campaignTransportType,
                          new UnitRef(UUID.fromString(attributes.getNamedItem("id").getTextContent())));
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
                    retVal.initializeShipTransportSpace();
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse unit {}", idNode.getTextContent().trim(), ex);
            return null;
        }

        if (retVal.id == null) {
            LOGGER.warn("ID not pre-defined; generating unit's ID.");
            retVal.id = UUID.randomUUID();
        }

        // Protection for old broken campaign files
        // Also for entities that do not have an external ID to match the UUID
        if (retVal.entity.getExternalIdAsString().equals("-1") ||
                  !(retVal.entity.getExternalIdAsString().equals(retVal.id.toString()))) {
            retVal.entity.setExternalIdAsString(retVal.id.toString());
        }

        return retVal;
    }

    /**
     * @return a html-coded list that says what quirks are enabled for this unit
     */
    public @Nullable String getQuirksList() {
        StringBuilder quirkString = new StringBuilder();
        boolean first = true;
        if (null != getEntity().getGame() && getEntity().getGame().getOptions().booleanOption("stratops_quirks")) {
            for (Enumeration<IOptionGroup> i = getEntity().getQuirks().getGroups(); i.hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                    IOption quirk = j.nextElement();
                    if (quirk.booleanValue()) {
                        if (first) {
                            first = false;
                        } else {
                            quirkString.append("<br>");
                        }
                        quirkString.append(quirk.getDisplayableNameWithValue());
                    }
                }
            }
        }
        return quirkString.toString().isBlank() ? null : "<html>" + quirkString + "</html>";
    }

    /**
     * The weekly maintenance cycle combined with a user defined maintenance cycle length is confusing and difficult to
     * manage so lets just make maintenance costs relative to the length of the maintenance cycle that the user defined
     *
     */
    public Money getMaintenanceCost() {
        return getWeeklyMaintenanceCost().multipliedBy(getCampaign().getCampaignOptions().getMaintenanceCycleDays())
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

        if (getCampaign().getCampaignOptions().isUsePercentageMaintenance()) {
            if (en instanceof Mek) {
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
            if (en instanceof Mek) {
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
                } else {
                    return Money.of(65.0);
                }
            } else if (en instanceof VTOL) {
                return Money.of(65.0);
            } else if (en instanceof Tank) {
                return Money.of(25.0);
            } else if (en instanceof BattleArmor) {
                return Money.of(((BattleArmor) en).getTroopers() * 50.0);
            } else if (en instanceof Infantry) {
                return Money.of(((Infantry) en).getSquadCount() * 10.0);
            }
        }
        return mCost.dividedBy(52.0);
    }

    public void addPart(Part part) {
        part.setUnit(this);
        parts.add(part);
    }

    /**
     * This will check a unit for certain parts and if they are missing, it will create a new version and update its
     * condition. checking for existing parts makes this a more complicated method, but it also ensures that you can
     * call this at any time, and you won't overwrite existing parts
     */
    public void initializeParts(boolean addParts) {
        int engineRating = 0;
        int builtInHeatSinks = 0;
        if (!(entity instanceof FighterSquadron) && (null != entity.getEngine())) {
            engineRating = entity.getEngine().getRating();
            if (entity.getEngine().isFusion()) {
                // 10 weight-free heats inks for fusion engines.
                // Used for fighters to prevent adding extra parts
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
        Hashtable<Integer, Part> equipParts = new Hashtable<>();
        Hashtable<Integer, Part> ammoParts = new Hashtable<>();
        Hashtable<Integer, Part> heatSinks = new Hashtable<>();
        Hashtable<Integer, Part> jumpJets = new Hashtable<>();
        Hashtable<Integer, Part[]> baEquipParts = new Hashtable<>();
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
            } else if (part instanceof SpacecraftEngine ||
                             part instanceof MissingSpacecraftEngine ||
                             part instanceof SVEnginePart ||
                             part instanceof MissingSVEngine) {
                engine = part;
            } else if (part instanceof MekLifeSupport || part instanceof MissingMekLifeSupport) {
                lifeSupport = part;
            } else if (part instanceof AeroLifeSupport || part instanceof MissingAeroLifeSupport) {
                lifeSupport = part;
            } else if (part instanceof MekSensor || part instanceof MissingMekSensor) {
                sensor = part;
            } else if (part instanceof ProtoMekSensor || part instanceof MissingProtoMekSensor) {
                sensor = part;
            } else if (part instanceof MekCockpit || part instanceof MissingMekCockpit) {
                cockpit = part;
            } else if (part instanceof VeeSensor || part instanceof MissingVeeSensor) {
                sensor = part;
            } else if (part instanceof InfantryMotiveType) {
                motiveType = part;
            } else if (part instanceof InfantryArmorPart) {
                infantryArmor = part;
            } else if (part instanceof InfantryWeaponPart) {
                if (((InfantryWeaponPart) part).isPrimary()) {
                    primaryW = part;
                } else {
                    secondaryW = part;
                }
            } else if (part instanceof StructuralIntegrity) {
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
            } else if (part instanceof ProtoMekLocation) {
                if (((ProtoMekLocation) part).getLoc() < locations.length) {
                    locations[((ProtoMekLocation) part).getLoc()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof MissingMekLocation || part instanceof MissingProtoMekLocation) {
                if (part.getLocation() < locations.length) {
                    locations[part.getLocation()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof BattleArmorSuit) {
                if ((entity instanceof BattleArmor) && ((BattleArmorSuit) part).getTrooper() < locations.length) {
                    locations[((BattleArmorSuit) part).getTrooper()] = part;
                } else {
                    partsToRemove.add(part);
                }
            } else if (part instanceof MissingBattleArmorSuit) {
                if ((entity instanceof BattleArmor) &&
                          ((MissingBattleArmorSuit) part).getTrooper() < locations.length) {
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
            } else if ((part instanceof VeeStabilizer || part instanceof MissingVeeStabilizer)) {
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
            } else if (part instanceof BattleArmorEquipmentPart) {
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
                int type;
                if (part instanceof MekActuator) {
                    type = ((MekActuator) part).getType();
                } else {
                    type = ((MissingMekActuator) part).getType();
                }
                int loc = part.getLocation();
                if (type == Mek.ACTUATOR_UPPER_ARM) {
                    if (loc == Mek.LOC_RIGHT_ARM) {
                        rightUpperArm = part;
                    } else {
                        leftUpperArm = part;
                    }
                } else if (type == Mek.ACTUATOR_LOWER_ARM) {
                    if (loc == Mek.LOC_RIGHT_ARM) {
                        rightLowerArm = part;
                    } else {
                        leftLowerArm = part;
                    }
                } else if (type == Mek.ACTUATOR_HAND) {
                    if (loc == Mek.LOC_RIGHT_ARM) {
                        rightHand = part;
                    } else {
                        leftHand = part;
                    }
                } else if (type == Mek.ACTUATOR_UPPER_LEG) {
                    if (loc == Mek.LOC_LEFT_ARM) {
                        leftUpperFrontLeg = part;
                    } else if (loc == Mek.LOC_RIGHT_ARM) {
                        rightUpperFrontLeg = part;
                    } else if (loc == Mek.LOC_RIGHT_LEG) {
                        rightUpperLeg = part;
                    } else if (loc == Mek.LOC_LEFT_LEG) {
                        leftUpperLeg = part;
                    } else if (loc == Mek.LOC_CENTER_LEG) {
                        centerUpperLeg = part;
                    } else {
                        LOGGER.error("Unknown location of {} for a Upper Leg Actuator.", loc);
                    }
                } else if (type == Mek.ACTUATOR_LOWER_LEG) {
                    if (loc == Mek.LOC_LEFT_ARM) {
                        leftLowerFrontLeg = part;
                    } else if (loc == Mek.LOC_RIGHT_ARM) {
                        rightLowerFrontLeg = part;
                    } else if (loc == Mek.LOC_RIGHT_LEG) {
                        rightLowerLeg = part;
                    } else if (loc == Mek.LOC_LEFT_LEG) {
                        leftLowerLeg = part;
                    } else if (loc == Mek.LOC_CENTER_LEG) {
                        centerLowerLeg = part;
                    } else {
                        LOGGER.error("Unknown location of {} for a Lower Leg Actuator.", loc);
                    }
                } else if (type == Mek.ACTUATOR_FOOT) {
                    if (loc == Mek.LOC_LEFT_ARM) {
                        leftFrontFoot = part;
                    } else if (loc == Mek.LOC_RIGHT_ARM) {
                        rightFrontFoot = part;
                    } else if (loc == Mek.LOC_RIGHT_LEG) {
                        rightFoot = part;
                    } else if (loc == Mek.LOC_LEFT_LEG) {
                        leftFoot = part;
                    } else if (loc == Mek.LOC_CENTER_LEG) {
                        centerFoot = part;
                    } else {
                        LOGGER.error("Unknown location of {} for a Foot Actuator.", loc);
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
            } else if ((part instanceof CombatInformationCenter || part instanceof MissingCIC) &&
                             (entity instanceof Jumpship)) {
                cic = part;
                // for reverse compatibility, calculate costs
                if (part instanceof CombatInformationCenter) {
                    ((CombatInformationCenter) cic).calculateCost();
                }
                // Only JumpShips and WarShips have these
            } else if ((part instanceof LFBattery || part instanceof MissingLFBattery) &&
                             ((entity instanceof Jumpship) &&
                                    !(entity instanceof SpaceStation) &&
                                    ((Jumpship) entity).hasLF())) {
                lfBattery = part;
            } else if ((part instanceof KFHeliumTank || part instanceof MissingKFHeliumTank) &&
                             ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                heliumTank = part;
            } else if ((part instanceof KFChargingSystem || part instanceof MissingKFChargingSystem) &&
                             ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                chargingSystem = part;
            } else if ((part instanceof KFFieldInitiator || part instanceof MissingKFFieldInitiator) &&
                             ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                fieldInitiator = part;
            } else if ((part instanceof KFDriveController || part instanceof MissingKFDriveController) &&
                             ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                driveController = part;
            } else if ((part instanceof KFDriveCoil || part instanceof MissingKFDriveCoil) &&
                             ((entity instanceof Jumpship) && !(entity instanceof SpaceStation))) {
                driveCoil = part;
                // For Small Craft and larger, add this as a container for all their heats inks
                // instead of adding hundreds
                // of individual heatsink parts.
            } else if (part instanceof SpacecraftCoolingSystem &&
                             (entity instanceof SmallCraft || entity instanceof Jumpship)) {
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
                // If an SC/DS/JS/WS/SS already has heat-sinks, remove them. We're using the
                // spacecraft cooling system instead
                if (entity instanceof SmallCraft || entity instanceof Jumpship) {
                    partsToRemove.add(part);
                } else if (entity.getEngine().isFusion() && builtInHeatSinks > 0) {
                    // Don't add parts for the 10 heat-sinks included with a fusion engine
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
            } else if ((part instanceof KFBoom) || (part instanceof MissingKFBoom)) {
                kfBoom = part;
            } else if ((part instanceof ProtoMekArmActuator) || (part instanceof MissingProtoMekArmActuator)) {
                int loc = part.getLocation();
                if (loc == ProtoMek.LOC_LEFT_ARM) {
                    protoLeftArmActuator = part;
                } else if (loc == ProtoMek.LOC_RIGHT_ARM) {
                    protoRightArmActuator = part;
                }
            } else if (part instanceof ProtoMekLegActuator || part instanceof MissingProtoMekLegActuator) {
                protoLegsActuator = part;
            } else if (part instanceof ProtoMekJumpJet || part instanceof MissingProtoMekJumpJet) {
                protoJumpJets.add(part);
            } else if ((part instanceof Thrusters) && (entity instanceof SmallCraft || entity instanceof Jumpship)) {
                if (((Thrusters) part).isLeftThrusters()) {
                    aeroThrustersLeft = part;
                } else {
                    aeroThrustersRight = part;
                }
            } else if ((part instanceof MissingThrusters) &&
                             (entity instanceof SmallCraft || entity instanceof Jumpship)) {
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
                if (entity instanceof Mek) {
                    MekLocation mekLocation = new MekLocation(i,
                          (int) getEntity().getWeight(),
                          getEntity().getStructureType(),
                          TechConstants.isClan(entity.getStructureTechLevel()),
                          hasTSM(),
                          entity instanceof QuadMek,
                          false,
                          false,
                          getCampaign());
                    addPart(mekLocation);
                    partsToAdd.add(mekLocation);
                } else if (entity instanceof ProtoMek && i != ProtoMek.LOC_NEAR_MISS) {
                    ProtoMekLocation protoMekLocation = new ProtoMekLocation(i,
                          (int) getEntity().getWeight(),
                          getEntity().getStructureType(),
                          ((ProtoMek) getEntity()).hasMyomerBooster(),
                          false,
                          getCampaign());
                    addPart(protoMekLocation);
                    partsToAdd.add(protoMekLocation);
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
                            TankLocation tankLocation = new TankLocation(i,
                                  (int) getEntity().getWeight(),
                                  getCampaign());
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
                } else if ((entity instanceof BattleArmor) &&
                                 (i != 0) &&
                                 (i <= ((BattleArmor) entity).getSquadSize())) {
                    BattleArmorSuit baSuit = new BattleArmorSuit((BattleArmor) entity, i, getCampaign());
                    addPart(baSuit);
                    partsToAdd.add(baSuit);
                }
            }
            if (null == armor[i]) {
                if (entity instanceof ProtoMek) {
                    ProtoMekArmor a = new ProtoMekArmor((int) getEntity().getWeight(),
                          getEntity().getArmorType(i),
                          getEntity().getOArmor(i, false),
                          i,
                          true,
                          getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else if (entity instanceof BattleArmor) {
                    BAArmor a = new BAArmor((int) getEntity().getWeight(),
                          getEntity().getOArmor(i, false),
                          entity.getArmorType(1),
                          i,
                          entity.isClan(),
                          getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else if (entity.isSupportVehicle() && (entity.getArmorType(i) == EquipmentType.T_ARMOR_STANDARD)) {
                    Armor a = new SVArmor(entity.getBARRating(i),
                          entity.getArmorTechRating(),
                          getEntity().getOArmor(i, false),
                          i,
                          getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                } else {
                    Armor a = new Armor((int) getEntity().getWeight(),
                          getEntity().getArmorType(i),
                          getEntity().getOArmor(i, false),
                          i,
                          false,
                          entity.isClanArmor(i),
                          getCampaign());
                    addPart(a);
                    partsToAdd.add(a);
                }
            }
            if ((null == armorRear[i]) && entity.hasRearArmor(i)) {
                Armor a = new Armor((int) getEntity().getWeight(),
                      getEntity().getArmorType(i),
                      getEntity().getOArmor(i, true),
                      i,
                      true,
                      entity.isClanArmor(i),
                      getCampaign());
                addPart(a);
                partsToAdd.add(a);
            }
            if (entity instanceof Tank && null == stabilisers[i] && i != Tank.LOC_BODY) {
                VeeStabilizer s = new VeeStabilizer((int) getEntity().getWeight(), i, getCampaign());
                addPart(s);
                partsToAdd.add(s);
            }
        }
        for (Mounted<?> m : entity.getEquipment()) {
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
                // there are some kind of non-hittable parts we might want to include for cost
                // calculations
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
                int equipmentNumber = entity.getEquipmentNum(m);
                Part apart = ammoParts.get(equipmentNumber);
                boolean oneShot = m.isOneShotAmmo();
                int fullShots = oneShot ? 1 : ((AmmoType) m.getType()).getShots();
                if (null == apart) {
                    if (entity instanceof BattleArmor) {
                        apart = new BattleArmorAmmoBin((int) entity.getWeight(),
                              (AmmoType) m.getType(),
                              equipmentNumber,
                              ((BattleArmor) entity).getSquadSize() * (fullShots - m.getBaseShotsLeft()),
                              oneShot,
                              getCampaign());
                    } else if (entity.usesWeaponBays()) {
                        apart = new LargeCraftAmmoBin((int) entity.getWeight(),
                              (AmmoType) m.getType(),
                              equipmentNumber,
                              fullShots - m.getBaseShotsLeft(),
                              m.getSize(),
                              getCampaign());
                        ((LargeCraftAmmoBin) apart).setBay(entity.getBayByAmmo((AmmoMounted) m));
                    } else if (entity.isSupportVehicle() &&
                                     (((AmmoType) m.getType()).getAmmoType() == AmmoType.AmmoTypeEnum.INFANTRY)) {
                        Mounted<?> weapon = m.getLinkedBy();
                        while (weapon.getType() instanceof AmmoType) {
                            weapon = weapon.getLinkedBy();
                        }
                        int size = m.getOriginalShots() / ((InfantryWeapon) weapon.getType()).getShots();
                        apart = new InfantryAmmoBin((int) entity.getWeight(),
                              (AmmoType) m.getType(),
                              equipmentNumber,
                              m.getOriginalShots() - m.getBaseShotsLeft(),
                              (InfantryWeapon) weapon.getType(),
                              size,
                              weapon.isOmniPodMounted(),
                              getCampaign());
                    } else {
                        apart = new AmmoBin((int) entity.getWeight(),
                              (AmmoType) m.getType(),
                              equipmentNumber,
                              fullShots - m.getBaseShotsLeft(),
                              oneShot,
                              m.isOmniPodMounted(),
                              getCampaign());
                    }
                    addPart(apart);
                    partsToAdd.add(apart);

                }
            } else if (m.getType() instanceof MiscType &&
                             (m.getType().hasFlag(MiscType.F_HEAT_SINK) ||
                                    m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
                if (m.getLocation() == Entity.LOC_NONE) {
                    // heat sinks located in LOC_NONE are base unhittable heat sinks
                    continue;
                }
                int equipmentNumber = entity.getEquipmentNum(m);
                Part epart = heatSinks.get(equipmentNumber);
                if (null == epart) {
                    epart = new HeatSink((int) entity.getWeight(),
                          m.getType(),
                          equipmentNumber,
                          m.isOmniPodMounted(),
                          getCampaign());
                    addPart(epart);
                    partsToAdd.add(epart);
                }
            } else if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                int equipmentNum = entity.getEquipmentNum(m);
                Part epart = jumpJets.get(equipmentNum);
                if (null == epart) {
                    epart = new JumpJet((int) entity.getWeight(),
                          m.getType(),
                          equipmentNum,
                          m.isOmniPodMounted(),
                          getCampaign());
                    addPart(epart);
                    partsToAdd.add(epart);
                    if (entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                        protoJumpJets.add(epart);
                    }
                }
            } else {
                int equipmentNum = entity.getEquipmentNum(m);
                EquipmentType type = m.getType();
                if (entity instanceof BattleArmor) {
                    // for BattleArmor we have multiple parts per mount, one for each trooper
                    Part[] equipmentParts = baEquipParts.get(equipmentNum);
                    for (int i = 0; i < ((BattleArmor) entity).getSquadSize(); i++) {
                        if ((null == equipmentParts) || (null == equipmentParts[i])) {
                            Part epart = new BattleArmorEquipmentPart((int) entity.getWeight(),
                                  type,
                                  equipmentNum,
                                  m.getSize(),
                                  i + BattleArmor.LOC_TROOPER_1,
                                  getCampaign());
                            addPart(epart);
                            partsToAdd.add(epart);
                        }
                    }
                } else {
                    Part epart = equipParts.get(equipmentNum);
                    if (null == epart) {
                        if (type instanceof InfantryAttack) {
                            continue;
                        }
                        if ((entity instanceof Infantry) && (m.getLocation() != Infantry.LOC_FIELD_GUNS)) {
                            // don't add weapons here for infantry, unless field guns
                            continue;
                        }
                        if (type instanceof BayWeapon) {
                            // weapon bays aren't real parts
                            continue;
                        }
                        epart = new EquipmentPart((int) entity.getWeight(),
                              type,
                              equipmentNum,
                              m.getSize(),
                              m.isOmniPodMounted(),
                              getCampaign());
                        if ((type instanceof MiscType) && type.hasFlag(MiscType.F_MASC)) {
                            epart = new MASC((int) entity.getWeight(),
                                  type,
                                  equipmentNum,
                                  getCampaign(),
                                  engineRating,
                                  m.isOmniPodMounted());
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
                    // Surface vehicles (including vehicles) have to choose the fuel type for a
                    // combustion engine.
                    // Fixed wing with an ICE will have the fuel type set to NONE, which is
                    // technically
                    // not correct but does the job of distinguishing the turbine from other ICE
                    // types.
                    FuelType fuel = FuelType.NONE;
                    if (entity instanceof Tank) {
                        fuel = ((Tank) entity).getICEFuelType();
                    }
                    engine = new SVEnginePart((int) entity.getWeight(),
                          entity.getEngine().getWeightEngine(entity),
                          entity.getEngine().getEngineType(),
                          entity.getEngineTechRating(),
                          fuel,
                          getCampaign());
                    addPart(engine);
                    partsToAdd.add(engine);
                }
            } else if (null != entity.getEngine()) {
                engine = new EnginePart((int) entity.getWeight(),
                      new Engine(entity.getEngine().getRating(),
                            entity.getEngine().getEngineType(),
                            entity.getEngine().getFlags()),
                      getCampaign(),
                      entity.getMovementMode() == EntityMovementMode.HOVER && entity instanceof Tank);
                addPart(engine);
                partsToAdd.add(engine);
            }
        }

        // Transport Bays
        for (Bay bay : entity.getTransportBays()) {
            bayPartsToAdd.put(bay.getBayNumber(), new ArrayList<>());
            BayType bayType = BayType.getTypeForBay(bay);
            Part bayPart = bays.get(bay.getBayNumber());
            if (null == bayPart) {
                bayPart = new TransportBayPart((int) entity.getWeight(),
                      bay.getBayNumber(),
                      bay.getCapacity(),
                      getCampaign());
                addPart(bayPart);
                partsToAdd.add(bayPart);
                for (int i = 0; i < bay.getDoors(); i++) {
                    Part door = new BayDoor((int) entity.getWeight(), getCampaign());
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                }
                if (bayType.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    for (int i = 0; i < bay.getCapacity(); i++) {
                        Part cubicle = new Cubicle((int) entity.getWeight(), bayType, getCampaign());
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                    }
                }
            } else {
                List<Part> doors = bayPart.getChildParts()
                                         .stream()
                                         .filter(p -> ((p instanceof BayDoor) || (p instanceof MissingBayDoor)))
                                         .collect(Collectors.toList());
                while (bay.getDoors() > doors.size()) {
                    Part door = new MissingBayDoor((int) entity.getWeight(), getCampaign());
                    bayPartsToAdd.get(bay.getBayNumber()).add(door);
                    addPart(door);
                    partsToAdd.add(door);
                    doors.add(door);
                }
                if (bayType.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                    List<Part> cubicles = bayPart.getChildParts()
                                                .stream()
                                                .filter(p -> ((p instanceof Cubicle) || (p instanceof MissingCubicle)))
                                                .collect(Collectors.toList());
                    while (bay.getCapacity() > cubicles.size()) {
                        Part cubicle = new MissingCubicle((int) entity.getWeight(), bayType, getCampaign());
                        bayPartsToAdd.get(bay.getBayNumber()).add(cubicle);
                        addPart(cubicle);
                        partsToAdd.add(cubicle);
                        cubicles.add(cubicle);
                    }
                }
            }
        }

        if (entity instanceof Mek) {
            if (null == gyro) {
                gyro = new MekGyro((int) entity.getWeight(),
                      entity.getGyroType(),
                      entity.getOriginalWalkMP(),
                      entity.isClan(),
                      getCampaign());
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
                cockpit = new MekCockpit((int) entity.getWeight(),
                      ((Mek) entity).getCockpitType(),
                      entity.isClan(),
                      getCampaign());
                addPart(cockpit);
                partsToAdd.add(cockpit);
            }
            if (null == rightUpperArm && entity.hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RIGHT_ARM)) {
                rightUpperArm = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_ARM,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightUpperArm);
                partsToAdd.add(rightUpperArm);
            }
            if (null == leftUpperArm && entity.hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)) {
                leftUpperArm = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_ARM,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
                addPart(leftUpperArm);
                partsToAdd.add(leftUpperArm);
            }
            if (null == rightLowerArm && entity.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RIGHT_ARM)) {
                rightLowerArm = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_ARM,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightLowerArm);
                partsToAdd.add(rightLowerArm);
            }

            if (null == leftLowerArm && entity.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)) {
                leftLowerArm = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_ARM,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
                addPart(leftLowerArm);
                partsToAdd.add(leftLowerArm);
            }

            if (null == rightHand && entity.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)) {
                rightHand = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_HAND,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightHand);
                partsToAdd.add(rightHand);
            }

            if (null == leftHand && entity.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)) {
                leftHand = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_HAND,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
                addPart(leftHand);
                partsToAdd.add(leftHand);
            }

            if (null == rightUpperLeg && entity.hasSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_RIGHT_LEG)) {
                rightUpperLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_LEG,
                      Mek.LOC_RIGHT_LEG,
                      getCampaign());
                addPart(rightUpperLeg);
                partsToAdd.add(rightUpperLeg);
            }

            if (null == leftUpperLeg && entity.hasSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_LEFT_LEG)) {
                leftUpperLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_LEG,
                      Mek.LOC_LEFT_LEG,
                      getCampaign());
                addPart(leftUpperLeg);
                partsToAdd.add(leftUpperLeg);
            }

            if ((centerUpperLeg == null) && entity.hasSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_CENTER_LEG)) {
                centerUpperLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_LEG,
                      Mek.LOC_CENTER_LEG,
                      getCampaign());
                addPart(centerUpperLeg);
                partsToAdd.add(centerUpperLeg);
            }

            if (null == rightLowerLeg && entity.hasSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_RIGHT_LEG)) {
                rightLowerLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_LEG,
                      Mek.LOC_RIGHT_LEG,
                      getCampaign());
                addPart(rightLowerLeg);
                partsToAdd.add(rightLowerLeg);
            }

            if (null == leftLowerLeg && entity.hasSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_LEFT_LEG)) {
                leftLowerLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_LEG,
                      Mek.LOC_LEFT_LEG,
                      getCampaign());
                addPart(leftLowerLeg);
                partsToAdd.add(leftLowerLeg);
            }

            if ((centerLowerLeg == null) && entity.hasSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_CENTER_LEG)) {
                centerLowerLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_LEG,
                      Mek.LOC_CENTER_LEG,
                      getCampaign());
                addPart(centerLowerLeg);
                partsToAdd.add(centerLowerLeg);
            }

            if (null == rightFoot && entity.hasSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) {
                rightFoot = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_FOOT,
                      Mek.LOC_RIGHT_LEG,
                      getCampaign());
                addPart(rightFoot);
                partsToAdd.add(rightFoot);
            }

            if (null == leftFoot && entity.hasSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG)) {
                leftFoot = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_FOOT,
                      Mek.LOC_LEFT_LEG,
                      getCampaign());
                addPart(leftFoot);
                partsToAdd.add(leftFoot);
            }

            if ((centerFoot == null) && entity.hasSystem(Mek.ACTUATOR_FOOT, Mek.LOC_CENTER_LEG)) {
                centerFoot = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_FOOT,
                      Mek.LOC_CENTER_LEG,
                      getCampaign());
                addPart(centerFoot);
                partsToAdd.add(centerFoot);
            }

            if (null == rightUpperFrontLeg && entity.hasSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_RIGHT_ARM)) {
                rightUpperFrontLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_LEG,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightUpperFrontLeg);
                partsToAdd.add(rightUpperFrontLeg);
            }
            if (null == leftUpperFrontLeg && entity.hasSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_LEFT_ARM)) {
                leftUpperFrontLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_UPPER_LEG,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
                addPart(leftUpperFrontLeg);
                partsToAdd.add(leftUpperFrontLeg);
            }
            if (null == rightLowerFrontLeg && entity.hasSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_RIGHT_ARM)) {
                rightLowerFrontLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_LEG,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightLowerFrontLeg);
                partsToAdd.add(rightLowerFrontLeg);
            }
            if (null == leftLowerFrontLeg && entity.hasSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_LEFT_ARM)) {
                leftLowerFrontLeg = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_LOWER_LEG,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
                addPart(leftLowerFrontLeg);
                partsToAdd.add(leftLowerFrontLeg);
            }
            if (null == rightFrontFoot && entity.hasSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_ARM)) {
                rightFrontFoot = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_FOOT,
                      Mek.LOC_RIGHT_ARM,
                      getCampaign());
                addPart(rightFrontFoot);
                partsToAdd.add(rightFrontFoot);
            }
            if (null == leftFrontFoot && entity.hasSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_ARM)) {
                leftFrontFoot = new MekActuator((int) entity.getWeight(),
                      Mek.ACTUATOR_FOOT,
                      Mek.LOC_LEFT_ARM,
                      getCampaign());
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
                driveCoil = new KFDriveCoil((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
                addPart(driveCoil);
                partsToAdd.add(driveCoil);
            }
            if (null == driveController && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                driveController = new KFDriveController((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
                addPart(driveController);
                partsToAdd.add(driveController);
            }
            if (null == fieldInitiator && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                fieldInitiator = new KFFieldInitiator((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
                addPart(fieldInitiator);
                partsToAdd.add(fieldInitiator);
            }
            if (null == chargingSystem && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                chargingSystem = new KFChargingSystem((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
                addPart(chargingSystem);
                partsToAdd.add(chargingSystem);
            }
            if (null == heliumTank && (entity instanceof Jumpship) && !(entity instanceof SpaceStation)) {
                heliumTank = new KFHeliumTank((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
                addPart(heliumTank);
                partsToAdd.add(heliumTank);
            }
            if (null == lfBattery &&
                      (entity instanceof Jumpship) &&
                      !(entity instanceof SpaceStation) &&
                      ((Jumpship) entity).hasLF()) {
                lfBattery = new LFBattery((int) entity.getWeight(),
                      ((Jumpship) entity).getDriveCoreType(),
                      entity.getDocks(),
                      getCampaign());
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
                coolingSystem = new SpacecraftCoolingSystem((int) entity.getWeight(),
                      ((Aero) entity).getOHeatSinks(),
                      sinkType,
                      getCampaign());
                addPart(coolingSystem);
                partsToAdd.add(coolingSystem);
            }
            if (null == sensor) {
                sensor = new AeroSensor((int) entity.getWeight(),
                      entity instanceof Dropship || entity instanceof Jumpship,
                      getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            if (null == landingGear && !(entity instanceof Jumpship)) {
                landingGear = new LandingGear((int) entity.getWeight(), getCampaign());
                addPart(landingGear);
                partsToAdd.add(landingGear);
            }
            if (null == lifeSupport) {
                lifeSupport = new AeroLifeSupport((int) entity.getWeight(),
                      Money.zero(),
                      !(entity instanceof SmallCraft || entity instanceof Jumpship),
                      getCampaign());
                addPart(lifeSupport);
                partsToAdd.add(lifeSupport);
                ((AeroLifeSupport) lifeSupport).calculateCost();
            }
            if (null == dropCollar && entity instanceof Dropship) {
                dropCollar = new DropshipDockingCollar((int) entity.getWeight(),
                      getCampaign(),
                      ((Dropship) entity).getCollarType());
                addPart(dropCollar);
                partsToAdd.add(dropCollar);
            }
            if (null == kfBoom && entity instanceof Dropship) {
                kfBoom = new KFBoom((int) entity.getWeight(), getCampaign(), ((Dropship) entity).getBoomType());
                addPart(kfBoom);
                partsToAdd.add(kfBoom);
            }
            if (jumpCollars.isEmpty() && entity instanceof Jumpship) {
                for (DockingCollar collar : entity.getDockingCollars()) {
                    Part collarPart = new JumpshipDockingCollar(0,
                          collar.getCollarNumber(),
                          getCampaign(),
                          ((Jumpship) entity).getDockingCollarType());
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
                    Part gravDeckPart = new GravDeck(0, deckNumber, getCampaign(), deckType);
                    gravDecks.put(deckNumber, gravDeckPart);
                    addPart(gravDeckPart);
                    partsToAdd.add(gravDeckPart);
                }
            }

            // Only add heatsink parts to fighters. Larger craft get a cooling system
            // instead.
            if (!(entity instanceof SmallCraft) && !(entity instanceof Jumpship)) {
                int numAeroHeatSinks = ((Aero) entity).getOHeatSinks() - aeroHeatSinks.size()
                                             // Ignore the 10 free heats inks we took out for fusion powered fighters
                                             - ((entity.getEngine() != null && entity.getEngine().isFusion()) ? 10 : 0);
                int numPodHeatSinks = ((Aero) entity).getPodHeatSinks() - podAeroHeatSinks;
                int sinkType = ((Aero) entity).getHeatType();
                if (sinkType == Aero.HEAT_DOUBLE && entity.isClan()) {
                    sinkType = AeroHeatSink.CLAN_HEAT_DOUBLE;
                }

                // add busted heat sinks even if they're "engine free" so they can be repaired
                if (numAeroHeatSinks == 0) {
                    for (int x = 0; x < ((Aero) entity).getHeatSinkHits(); x++) {
                        MissingAeroHeatSink aHeatSink = new MissingAeroHeatSink((int) entity.getWeight(),
                              sinkType,
                              false,
                              getCampaign());
                        addPart(aHeatSink);
                        partsToAdd.add(aHeatSink);
                    }
                } else {
                    while (numAeroHeatSinks > 0) {
                        AeroHeatSink aHeatSink = new AeroHeatSink((int) entity.getWeight(),
                              sinkType,
                              numPodHeatSinks > 0,
                              getCampaign());
                        addPart(aHeatSink);
                        partsToAdd.add(aHeatSink);
                        numAeroHeatSinks--;
                        if (numPodHeatSinks > 0) {
                            numPodHeatSinks--;
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
        if (entity instanceof ProtoMek) {
            if (!entity.entityIsQuad()) {
                if (null == protoLeftArmActuator) {
                    protoLeftArmActuator = new ProtoMekArmActuator((int) entity.getWeight(),
                          ProtoMek.LOC_LEFT_ARM,
                          getCampaign());
                    addPart(protoLeftArmActuator);
                    partsToAdd.add(protoLeftArmActuator);
                }
                if (null == protoRightArmActuator) {
                    protoRightArmActuator = new ProtoMekArmActuator((int) entity.getWeight(),
                          ProtoMek.LOC_RIGHT_ARM,
                          getCampaign());
                    addPart(protoRightArmActuator);
                    partsToAdd.add(protoRightArmActuator);
                }
            }
            if (null == protoLegsActuator) {
                protoLegsActuator = new ProtoMekLegActuator((int) entity.getWeight(), getCampaign());
                addPart(protoLegsActuator);
                partsToAdd.add(protoLegsActuator);
            }
            if (null == sensor) {
                sensor = new ProtoMekSensor((int) entity.getWeight(), getCampaign());
                addPart(sensor);
                partsToAdd.add(sensor);
            }
            int jj = (entity).getOriginalJumpMP() - protoJumpJets.size();
            while (jj > 0) {
                ProtoMekJumpJet protoJJ = new ProtoMekJumpJet((int) entity.getWeight(), getCampaign());
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
                    infantryArmor = new InfantryArmorPart(0,
                          getCampaign(),
                          ((Infantry) entity).getCustomArmorDamageDivisor(),
                          ((Infantry) entity).isArmorEncumbering(),
                          ((Infantry) entity).hasDEST(),
                          ((Infantry) entity).hasSneakCamo(),
                          ((Infantry) entity).hasSneakECM(),
                          ((Infantry) entity).hasSneakIR(),
                          ((Infantry) entity).hasSpaceSuit());
                }
                if (infantryArmor.getStickerPrice().isPositive()) {
                    int number = entity.getOInternal(Infantry.LOC_INFANTRY);
                    while (number > 0) {
                        infantryArmor = new InfantryArmorPart(0,
                              getCampaign(),
                              ((Infantry) entity).getCustomArmorDamageDivisor(),
                              ((Infantry) entity).isArmorEncumbering(),
                              ((Infantry) entity).hasDEST(),
                              ((Infantry) entity).hasSneakCamo(),
                              ((Infantry) entity).hasSneakECM(),
                              ((Infantry) entity).hasSneakIR(),
                              ((Infantry) entity).hasSpaceSuit());
                        addPart(infantryArmor);
                        partsToAdd.add(infantryArmor);
                        number--;
                    }
                }
            }
            InfantryWeapon primaryType = ((Infantry) entity).getPrimaryWeapon();
            InfantryWeapon secondaryType = ((Infantry) entity).getSecondaryWeapon();
            if ((null == primaryW) && (null != primaryType)) {
                int number = (((Infantry) entity).getSquadSize() - ((Infantry) entity).getSecondaryWeaponsPerSquad()) *
                                   ((Infantry) entity).getSquadCount();
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
                    secondaryW = new InfantryWeaponPart((int) entity.getWeight(),
                          secondaryType,
                          -1,
                          getCampaign(),
                          false);
                    addPart(secondaryW);
                    partsToAdd.add(secondaryW);
                    number--;
                }
            }
        }
        if (getEntity() instanceof LandAirMek) {
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
                getCampaign().getQuartermaster().addPart(p, 0, false);
            }
        }
        // We can't add the child parts to the transport bay part until they have been
        // added to the
        // campaign and have an id.
        for (int bayNum : bayPartsToAdd.keySet()) {
            Optional<Part> bayPart = getParts().stream()
                                           .filter(p -> (p instanceof TransportBayPart) &&
                                                              ((TransportBayPart) p).getBayNumber() == bayNum)
                                           .findAny();
            bayPart.ifPresent(part -> bayPartsToAdd.get(bayNum).forEach(part::addChildPart));
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
     *
     * @param predicate A predicate to apply to each part on the unit.
     *
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
            return (force != null) ? force.getCamouflageOrElse(campaign.getCamouflage()) : campaign.getCamouflage();
        } else {
            return getCamouflage();
        }
    }

    public @Nullable Image getImage(final Component component) {
        return getImage(component, getUtilizedCamouflage(getCampaign()), true);
    }

    public @Nullable Image getImage(final Component component, final Camouflage camouflage, final boolean showDamage) {
        if (MHQStaticDirectoryManager.getMekTileset() == null) {
            return null;
        }
        final Image base = MHQStaticDirectoryManager.getMekTileset().imageFor(getEntity());
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
     * Returns the commander of the entity.
     * <p>
     * The commander is determined based on the merged list of all crew members, prioritizing certain roles over others.
     * The commander is initialized as the first person in the merged list and then updated by iterating over all crew
     * members and comparing their rank with the current commander. If a crew member outranks the current commander or
     * has the same rank, they are considered as the new commander.
     *
     * @return the commander of the entity, or null if the entity is null or if there are no crew members
     */
    public @Nullable Person getCommander() {
        // quick safety check
        if (entity == null) {
            return null;
        }

        // Merge all crew into a single list,
        // lists retain the order in which elements are added to them,
        // so this allows us to prioritize certain roles over others
        List<Person> allCrew = new ArrayList<>();
        allCrew.addAll(vesselCrew);
        allCrew.addAll(gunners);
        allCrew.addAll(drivers);

        if (navigator != null) {
            allCrew.add(navigator);
        }

        if (allCrew.isEmpty()) {
            return null;
        }

        // Initialize the commander as the first person
        Person commander = allCrew.get(0);

        // Iterate over all crew
        for (Person person : allCrew) {
            // Compare person with the current commander
            if (person.outRanks(commander) || (person.getRankNumeric() == commander.getRankNumeric())) {
                commander = person;
            }
        }

        // Return the final commander
        return commander;
    }

    public boolean hasCommander() {
        return getCommander() != null;
    }

    public void resetPilotAndEntity() {
        final CampaignOptions campaignOptions = getCampaign().getCampaignOptions();

        // Reset transient data
        getCampaign().clearGameData(entity);
        entity.setCommander(false);
        entity.getCrew().resetGameState();
        entity.getCrew().setCommandBonus(0);

        // Update crew data
        updateCrew();

        // commander can be null at this point, but that's ok because both of the following calls include null
        // handling built into their methods.
        Person commander = getCommander();

        if (campaignOptions.isUseInitiativeBonus()) {
            setCommandBonus(commander);
        }

        if (campaignOptions.isUseAbilities() || campaignOptions.isUseEdge() || campaignOptions.isUseImplants()) {
            processUnitSPAs(commander);
        }
    }

    private void updateCrew() {
        if (entity.getCrew().getSlotCount() > 1) {
            final String driveType = SkillType.getDrivingSkillFor(entity);
            final String gunType = SkillType.getGunnerySkillFor(entity);
            if (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos()) {
                // Command console; each crew is assigned as both driver and gunner
                int slot = 0;
                for (Person p : gunners) {
                    if (p.hasSkill(gunType) &&
                              p.hasSkill(driveType) &&
                              p.getStatus().isActive() &&
                              slot < entity.getCrew().getSlotCount()) {
                        assignToCrewSlot(p, slot, gunType, driveType);
                        slot++;
                    }
                }
                while (slot < entity.getCrew().getSlotCount()) {
                    entity.getCrew().setMissing(true, slot++);
                }
            } else {
                // tripod, quadvee, or dual cockpit; driver and gunner are assigned separately
                Optional<Person> person = drivers.stream()
                                                .filter(p -> p.hasSkill(driveType) && p.getStatus().isActive())
                                                .findFirst();
                if (person.isPresent()) {
                    assignToCrewSlot(person.get(), 0, gunType, driveType);
                } else {
                    entity.getCrew().setMissing(true, 0);
                }
                person = gunners.stream().filter(p -> p.hasSkill(driveType) && p.getStatus().isActive()).findFirst();
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
            if ((entity.getEntityType() & Entity.ETYPE_LAND_AIR_MEK) == 0) {
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
    }

    private void setCommandBonus(@Nullable Person commander) {
        // Tactics command bonus. This should actually reflect the unit's commander
        if (null != commander && commander.hasSkill(SkillType.S_TACTICS)) {
            entity.getCrew()
                  .setCommandBonus(commander.getSkill(SkillType.S_TACTICS)
                                         .getTotalSkillLevel(commander.getOptions(),
                                               commander.getATOWAttributes(),
                                               0));
        }
    }

    private void processUnitSPAs(@Nullable Person commander) {
        if (null == commander) {
            // This is a legacy decision. Previously we early exited, but that caused a heap of problems. So instead
            // we now create a temporary fake 'commander' with no PilotOptions. This allows all the code here to
            // process, without us needing to worry about nulls or rewriting this rather large and complex piece of
            // code. - Illiani 5th October 2025
            commander = new Person(getCampaign());
        }

        PilotOptions options = new PilotOptions(); // MegaMek-style as it is sent to MegaMek
        // This double enumeration is annoying to work with for crew-served units.
        // Get the option names while we enumerate so they can be used later
        List<String> optionNames = new ArrayList<>();
        Set<String> cyberOptionNames = new HashSet<>();
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                if (getCampaign().getCampaignOptions().isUseImplants() &&
                          group.getKey().equals(PersonnelOptions.MD_ADVANTAGES)) {
                    cyberOptionNames.add(option.getName());
                } else if (getCampaign().getCampaignOptions().isUseEdge() &&
                                 group.getKey().equals(PersonnelOptions.EDGE_ADVANTAGES)) {
                    optionNames.add(option.getName());
                } else if (getCampaign().getCampaignOptions().isUseAbilities() &&
                                 !group.getKey().equals(PersonnelOptions.EDGE_ADVANTAGES)) {
                    optionNames.add(option.getName());
                }
            }
        }

        boolean commanderOnly = campaign.getCampaignOptions().isUseCommanderAbilitiesOnly();

        // For crew-served units, let's look at the abilities of the group. If more than half the crew (gunners
        // and pilots only, for spacecraft) have an ability, grant the benefit to the unit
        // TODO : Mobile structures, large naval support vehicles
        if (!commanderOnly &&
                  (entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT) ||
                         entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                         entity.hasETypeFlag(Entity.ETYPE_TANK) ||
                         entity.hasETypeFlag(Entity.ETYPE_INFANTRY) ||
                         entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK))) {
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
            // 4. For each group, group by the object value and get the counts for each
            // value
            // 5. Take each group which has more than crewSize/2 values, and find the
            // maximum value
            Map<String, Optional<Object>> bestOptions = crew.stream()
                                                              .flatMap(p -> optionNames.stream()
                                                                                  .map(n -> p.getOptions()
                                                                                                  .getOption(n)))
                                                              .collect(Collectors.groupingBy(IOption::getName,
                                                                    Collectors.collectingAndThen(Collectors.groupingBy(
                                                                                IOption::getValue,
                                                                                Collectors.counting()),
                                                                          m -> m.entrySet()
                                                                                     .stream()
                                                                                     .filter(e -> (cyberOptionNames.contains(
                                                                                           e.getKey()) ?
                                                                                                         e.getValue() >=
                                                                                                               crewSize :
                                                                                                         e.getValue() >
                                                                                                               crewSize /
                                                                                                                     2))
                                                                                     .max(Entry.comparingByValue())
                                                                                     .map(Entry::getKey))));

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

            // Yuck. Most cybernetic implants require all members of a unit's crew to have
            // the implant rather than half.
            // A few just require 1/4 the crew, there's at least one commander only, some
            // just add an effect for every
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
                for (Person p : drivers) {
                    sumEdge += p.getCurrentEdge();
                }
                // Again, don't count infantrymen twice
                if (!entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                    for (Person p : gunners) {
                        sumEdge += p.getCurrentEdge();
                    }
                }
                // Average the edge values of pilots and gunners. The Spacecraft Engineer
                // (vessel crewmembers)
                // handle edge solely through MHQ as noncombat personnel, so aren't considered
                // here
                int edge = (int) Math.round(sumEdge / crewSize);
                IOption edgeOption = entity.getCrew().getOptions().getOption(OptionsConstants.EDGE);
                edgeOption.setValue((Integer) edge);
            }

            // Reset the composite technician used by spacecraft and infantry
            // Important if you just changed technician edge options for members of either
            // unit type
            resetEngineer();

            // TODO : Set up crew hits. This might only apply to spacecraft, and should
            // reflect
            // the unit's current crew size vs its required crew size. There's also the
            // question
            // of what to do with extra crew quarters and crewmember assignments beyond the
            // minimum.
        } else {
            // For other unit types, just use the unit commander's abilities.
            PilotOptions cdrOptions = new PilotOptions(); // MegaMek-style as it is sent to MegaMek
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
    }

    /**
     * For vehicles, infantry, and naval vessels, compute the piloting and gunnery skills based on the crew as a whole.
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

        boolean entityIsConventionalInfantry = entity.hasETypeFlag(Entity.ETYPE_INFANTRY) &&
                                                     !entity.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        for (Person person : drivers) {
            PersonnelOptions options = person.getOptions();
            Attributes attributes = person.getATOWAttributes();
            if (person.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }
            if (person.hasSkill(driveType)) {
                sumPiloting += person.getSkill(driveType).getFinalSkillValue(options, attributes);
                nDrivers++;
            } else if (entity instanceof Infantry) {
                // For infantry, we need to assign an 8 if they have no anti-mek skill
                sumPiloting += 8;
                nDrivers++;
            }

            if (entity instanceof Tank && Compute.getFullCrewSize(entity) == 1 && person.hasSkill(gunType)) {
                sumGunnery += person.getSkill(gunType).getFinalSkillValue(options, attributes);
                nGunners++;
            }
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                sumPiloting += person.getInjuryModifiers(true);
            }
        }
        for (Person person : gunners) {
            PersonnelOptions options = person.getOptions();
            Attributes attributes = person.getATOWAttributes();
            if (person.getHits() > 0 && !usesSoloPilot()) {
                continue;
            }

            String tempGunType = gunType;
            if (entityIsConventionalInfantry) {
                tempGunType = InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                      campaign.getCampaignOptions().isUseSmallArmsOnly());
                if (tempGunType == null) {
                    tempGunType = SkillType.S_SMALL_ARMS;
                }
            }

            if (person.hasSkill(tempGunType)) {
                sumGunnery += person.getSkill(tempGunType).getFinalSkillValue(options, attributes);
                nGunners++;
            }
            if (person.hasSkill(SkillType.S_ARTILLERY) &&
                      person.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue(options, attributes) < artillery) {
                artillery = person.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue(options, attributes);
            }
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                sumGunnery += person.getInjuryModifiers(false);
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
        // Using the tech officer field for the secondary commander; if nobody assigned
        // to the command
        // console we will flag the entity as using the console commander, which has the
        // effect of limiting
        // the tank to a single commander. As the console commander is not counted
        // against crew requirements,
        // we do not increase nCrew if present.
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
                int numTroopers = 0;
                // OK, we want to reorder the way we move through suits, so that we always put
                // BA
                // in the suits with more armor. Otherwise, we may put a soldier in a suit with
                // no
                // armor when a perfectly good suit is waiting further down the line.
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
                        // no suit here move along
                        continue;
                    }
                    if (numTroopers < nGunners) {
                        entity.setInternal(1, i);
                        numTroopers++;
                    } else {
                        entity.setInternal(0, i);
                    }
                }
            }
            entity.setInternal(nGunners, Infantry.LOC_INFANTRY);
        }

        if (entity instanceof Tank) {
            if (nDrivers == 0 && nGunners == 0) {
                // nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            }
            ((Tank) entity).setDriverHit(nDrivers == 0);
        } else if (entity instanceof Infantry) {
            if (nDrivers == 0 && nGunners == 0) {
                // nobody is healthy
                entity.getCrew().setSize(0);
                entity.getCrew().setMissing(true, 0);
                return;
            }
        }
        // TODO: For the moment we need to max these out at 8 so people don't get errors
        // when they customize in MM but we should put an option in MM to ignore those
        // limits
        // and set it to true when we start up through MHQ
        entity.getCrew().setPiloting(Math.min(max(piloting, 0), 8), 0);
        entity.getCrew().setGunnery(Math.min(max(gunnery, 0), 8), 0);
        entity.getCrew().setArtillery(Math.min(max(artillery, 0), 8), 0);
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            // Use tac ops crew hits calculations and current size versus maximum size
            entity.getCrew().setCurrentSize(nCrew + nGunners + nDrivers);
            entity.getCrew().setSize(Compute.getFullCrewSize(entity));
            entity.getCrew().setHits(entity.getCrew().calculateHits(), 0);
        } else if (entity instanceof Infantry || usesSoloPilot()) {
            // Set the crew size based on gunners, since all personnel are both gunners and
            // drivers
            entity.getCrew().setSize(nGunners);
        } else {
            // Crew size should be the total of the 3 types of crewmembers
            entity.getCrew().setSize(nCrew + nGunners + nDrivers);
        }
        entity.getCrew().setMissing(false, 0);
    }

    /**
     * LAMs require a pilot that is cross-trained for meks and fighters
     */
    private void refreshLAMPilot() {
        Person pilot = getCommander();
        if (null == pilot) {
            entity.getCrew().setMissing(true, 0);
            entity.getCrew().setSize(0);
            return;
        }

        int pilotingMek = 13;
        int gunneryMek = 13;
        int pilotingAero = 13;
        int gunneryAero = 13;
        int artillery = 13;

        PersonnelOptions options = pilot.getOptions();
        Attributes attributes = pilot.getATOWAttributes();
        if (pilot.hasSkill(SkillType.S_PILOT_MEK)) {
            pilotingMek = pilot.getSkill(SkillType.S_PILOT_MEK).getFinalSkillValue(options, attributes);
        }
        if (pilot.hasSkill(SkillType.S_GUN_MEK)) {
            gunneryMek = pilot.getSkill(SkillType.S_GUN_MEK).getFinalSkillValue(options, attributes);
        }
        if (pilot.hasSkill(SkillType.S_PILOT_AERO)) {
            pilotingAero = pilot.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue(options, attributes);
        }
        if (pilot.hasSkill(SkillType.S_GUN_AERO)) {
            gunneryAero = pilot.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue(options, attributes);
        }
        if (pilot.hasSkill(SkillType.S_ARTILLERY)) {
            artillery = pilot.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue(options, attributes);
        }

        if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
            pilotingMek += pilot.getInjuryModifiers(true);
            gunneryMek += pilot.getInjuryModifiers(false);
            pilotingAero += pilot.getInjuryModifiers(true);
            gunneryAero += pilot.getInjuryModifiers(false);
            artillery += pilot.getInjuryModifiers(false);
        }
        LAMPilot crew = (LAMPilot) entity.getCrew();
        crew.setPiloting(Math.min(max(pilotingMek, 0), 8), crew.getCrewType().getPilotPos());
        crew.setGunnery(Math.min(max(gunneryMek, 0), 8), crew.getCrewType().getGunnerPos());
        crew.setPilotingAero(Math.min(max(pilotingAero, 0), 8));
        crew.setGunneryAero(Math.min(max(gunneryAero, 0), 8));
        entity.getCrew().setArtillery(Math.min(max(artillery, 0), 8), 0);
        entity.getCrew().setSize(1);
        entity.getCrew().setMissing(false, 0);
    }

    /**
     * Sets the values of a slot in the entity crew for the indicated person.
     *
     */
    private void assignToCrewSlot(Person person, int slot, String gunType, String driveType) {
        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();
        entity.getCrew().setName(person.getFullTitle(), slot);
        entity.getCrew().setNickname(person.getCallsign(), slot);
        entity.getCrew().setGender(person.getGender(), slot);
        entity.getCrew().setClanPilot(person.isClanPersonnel(), slot);
        entity.getCrew().setPortrait(person.getPortrait().clone(), slot);
        entity.getCrew().setHits(person.getHits(), slot);
        int gunnery = 7;
        int artillery = 7;
        int piloting = 8;
        if (person.hasSkill(gunType)) {
            gunnery = person.getSkill(gunType).getFinalSkillValue(options, attributes);
        }
        if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
            gunnery += person.getInjuryModifiers(false);
        }
        if (person.hasSkill(driveType)) {
            piloting = person.getSkill(driveType).getFinalSkillValue(options, attributes);
        }
        if (person.hasSkill(SkillType.S_ARTILLERY) &&
                  person.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue(options, attributes) < artillery) {
            artillery = person.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue(options, attributes);
        }
        entity.getCrew().setPiloting(Math.min(max(piloting, 0), 8), slot);
        entity.getCrew().setGunnery(Math.min(max(gunnery, 0), 8), slot);
        // also set RPG gunnery skills in case present in game options
        entity.getCrew().setGunneryL(Math.min(max(gunnery, 0), 8), slot);
        entity.getCrew().setGunneryM(Math.min(max(gunnery, 0), 8), slot);
        entity.getCrew().setGunneryB(Math.min(max(gunnery, 0), 8), slot);
        entity.getCrew().setArtillery(Math.min(max(artillery, 0), 8), slot);
        entity.getCrew().setToughness(person.getToughness(), slot);

        entity.getCrew().setExternalIdAsString(person.getId().toString(), slot);
        entity.getCrew().setMissing(false, slot);
    }

    public void resetEngineer() {
        if (!isSelfCrewed()) {
            return;
        }
        int minutesLeft = TECH_WORK_DAY;
        int overtimeLeft = TECH_WORK_DAY / 2;
        boolean breakPartReRoll = true;
        boolean failRefitReRoll = true;
        if (null != engineer) {
            minutesLeft = engineer.getMinutesLeft();
            overtimeLeft = engineer.getOvertimeLeft();
        } else {
            // then get the number based on the least amount available to crew members
            // in the case of Edge, everyone must have the same triggers set for Edge to
            // work
            for (Person person : getActiveCrew()) {
                if (person.getMinutesLeft() < minutesLeft) {
                    minutesLeft = person.getMinutesLeft();
                }

                if (person.getOvertimeLeft() < overtimeLeft) {
                    overtimeLeft = person.getOvertimeLeft();
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
                // will only be reloading ammo, so doesn't really matter what skill level we
                // give them - set to regular
                engineer.addSkill(SkillType.S_TECH_MECHANIC,
                      SkillType.getType(SkillType.S_TECH_MECHANIC).getRegularLevel(),
                      0);
                engineer.addSkill(SkillType.S_ADMIN,
                      SkillType.getType(SkillType.S_ADMIN).getRegularLevel(),
                      0);
            } else {
                engineer = null;
            }
        } else {
            if (!vesselCrew.isEmpty()) {
                int nCrew = 0;
                int sumTechSkill = 0;
                int sumTechBonus = 0;
                int sumAdminSkill = -1; // Unskilled
                int sumAdminBonus = 0;
                int sumEdge = 0;
                int sumEdgeUsed = 0;
                String engineerGivenName = "Nobody";
                String engineerSurname = "Nobody";
                int bestRank = Integer.MIN_VALUE;
                for (Person person : vesselCrew) {
                    if (engineer != null) {
                        // If the engineer used edge points, remove some from vessel crewmembers until
                        // all is paid for
                        if (engineer.getEdgeUsed() > 0) {
                            // Don't subtract an Edge if the individual has none left
                            if (person.getCurrentEdge() > 0) {
                                person.changeCurrentEdge(-1);
                                engineer.setEdgeUsed(engineer.getEdgeUsed() - 1);
                            }
                        }
                        // If the engineer gained XP, add it for each crewman
                        person.awardXP(getCampaign(), engineer.getXP());

                        // Update each crewman's successful task count too
                        person.setNTasks(person.getNTasks() + engineer.getNTasks());
                        if (person.getNTasks() >= getCampaign().getCampaignOptions().getNTasksXP()) {
                            person.awardXP(getCampaign(), getCampaign().getCampaignOptions().getTaskXP());
                            person.setNTasks(0);
                        }
                        sumEdgeUsed = engineer.getEdgeUsed();
                    }
                    sumEdge += person.getAdjustedEdge();

                    if (person.hasSkill(SkillType.S_TECH_VESSEL)) {
                        sumTechSkill += person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                        sumTechBonus += person.getSkill(SkillType.S_TECH_VESSEL).getBonus();
                        nCrew++;
                    }

                    if (person.hasSkill(SkillType.S_ADMIN)) {
                        sumAdminSkill += person.getSkill(SkillType.S_ADMIN).getLevel();
                        sumAdminBonus += person.getSkill(SkillType.S_ADMIN).getBonus();
                    }
                    if (!(person.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART))) {
                        breakPartReRoll = false;
                    }
                    if (!(person.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT))) {
                        failRefitReRoll = false;
                    }
                    if (person.getRankNumeric() > bestRank) {
                        engineerGivenName = person.getGivenName();
                        engineerSurname = person.getSurname();
                        bestRank = person.getRankNumeric();
                    }
                }
                if (nCrew > 0) {
                    engineer = new Person(engineerGivenName, engineerSurname, getCampaign());
                    engineer.setEngineer(true);
                    engineer.setClanPersonnel(getCommander().isClanPersonnel());
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_BREAK_PART, breakPartReRoll);
                    engineer.setEdgeTrigger(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, failRefitReRoll);
                    engineer.setMinutesLeft(minutesLeft);
                    engineer.setOvertimeLeft(overtimeLeft);
                    engineer.setId(getCommander().getId());
                    engineer.setPrimaryRoleDirect(PersonnelRole.VESSEL_CREW);
                    if (bestRank > -1) {
                        engineer.setRank(bestRank);
                    }
                    engineer.addSkill(SkillType.S_TECH_VESSEL, sumTechSkill / nCrew, sumTechBonus / nCrew);
                    if (sumAdminSkill > -1) {
                        engineer.addSkill(SkillType.S_ADMIN, sumAdminSkill / nCrew, sumAdminBonus / nCrew);
                    }
                    engineer.setEdgeUsed(sumEdgeUsed);
                    engineer.setCurrentEdge(max(0, (sumEdge - sumEdgeUsed) / nCrew));
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
                    p.cancelAssignment(true);
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
     *
     * @return The number of generic crew required
     */
    public int getTotalCrewNeeds() {
        int nav = 0;
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            if (entity instanceof Jumpship && !(entity instanceof SpaceStation)) {
                nav = 1;
            }
            return getFullCrewSize() - getTotalDriverNeeds() - getTotalGunnerNeeds() - nav;
        }

        return getFullCrewSize() - getTotalDriverNeeds() - getTotalGunnerNeeds();
    }

    public boolean canTakeMoreDrivers() {
        int nDrivers = drivers.size();
        return nDrivers < getTotalDriverNeeds();
    }

    public boolean canTakeMoreVesselCrew() {
        int nCrew = vesselCrew.size();
        return nCrew < getTotalCrewNeeds();
    }

    public boolean canTakeNavigator() {
        return entity instanceof Jumpship && !(entity instanceof SpaceStation) && (navigator == null);
    }

    public boolean canTakeTechOfficer() {
        return (techOfficer == null) && (entity.getCrew().getCrewType().getTechPos() >= 0
                                               // Use techOfficer field for secondary commander
                                               ||
                                               (entity instanceof Tank &&
                                                      entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)));
    }

    public boolean canTakeTech() {
        return isUnmaintained() && !isSelfCrewed();
    }

    // TODO : Switch similar tables in person to use this one instead
    public String determineUnitTechSkillType() {
        if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
            return SkillType.S_TECH_MEK;
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
        // return Compute.getFullCrewSize(entity) == 1;
        // Taharqa: I dont think we should do it based on computed size, but whether the
        // unit logically
        // is the type of unit that has only one pilot. This is partly because there may
        // be some vees
        // that only have one pilot and this is also a problem for BA units with only
        // one active suit
        return ((entity instanceof Mek) ||
                      (entity instanceof ProtoMek) ||
                      (entity instanceof Aero &&
                             !(entity instanceof SmallCraft) &&
                             !(entity instanceof Jumpship) &&
                             !(entity instanceof ConvFighter))) &&
                     (entity.getCrew().getCrewType().getPilotPos() == entity.getCrew().getCrewType().getGunnerPos());
    }

    public boolean usesSoldiers() {
        return entity instanceof Infantry;
    }

    public void addDriver(Person p) {
        addDriver(p, false);
    }

    public void addDriver(Person person, boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        drivers.add(person);
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    public void addGunner(Person p) {
        addGunner(p, false);
    }

    public void addGunner(Person person, boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        gunners.add(person);
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    public void addVesselCrew(Person p) {
        addVesselCrew(p, false);
    }

    public void addVesselCrew(Person person, boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        vesselCrew.add(person);
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    public void setNavigator(Person p) {
        setNavigator(p, false);
    }

    public void setNavigator(Person person, boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        navigator = person;
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    public boolean isTechOfficer(@Nullable Person p) {
        return (techOfficer != null) && techOfficer.equals(p);
    }

    public void setTechOfficer(Person p) {
        setTechOfficer(p, false);
    }

    public void setTechOfficer(Person person, boolean useTransfers) {
        Objects.requireNonNull(person);

        ensurePersonIsRegistered(person);
        techOfficer = person;
        person.setUnit(this);
        resetPilotAndEntity();
        if (useTransfers) {
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    public void setTech(Person p) {
        Objects.requireNonNull(p);

        if (null != tech) {
            LOGGER.warn("New tech assigned {} without removing previous tech {}", p.getFullName(), tech);
        }
        ensurePersonIsRegistered(p);
        tech = p;
        p.addTechUnit(this);
        AssignmentLogger.assignedTo(p, getCampaign().getLocalDate(), getName());
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
            getCampaign().recruitPerson(person, person.getPrisonerStatus(), true, false, true);
            LOGGER.debug("The person {} added this unit {}, was not in the campaign.", person.getFullName(), getName());
        }
    }

    public void addPilotOrSoldier(final Person person) {
        addPilotOrSoldier(person, false);
    }

    public void addPilotOrSoldier(final Person person, final boolean useTransfers) {
        addPilotOrSoldier(person, null, useTransfers);
    }

    public void addPilotOrSoldier(final Person person, final @Nullable Unit oldUnit, final boolean useTransfers) {
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
            AssignmentLogger.reassignedTo(person, getCampaign().getLocalDate(), getName());
            AssignmentLogger.reassignedTOEForce(getCampaign(),
                  person,
                  getCampaign().getLocalDate(),
                  getCampaign().getForceFor(oldUnit),
                  getCampaign().getForceFor(this));
        } else {
            AssignmentLogger.assignedTo(person, getCampaign().getLocalDate(), getName());
            AssignmentLogger.addedToTOEForce(getCampaign(),
                  person,
                  getCampaign().getLocalDate(),
                  getCampaign().getForceFor(this));
        }
        MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
    }

    /**
     * @param person the person to remove. If this is null we return immediately without parsing.
     * @param log    whether to log the removal
     */
    public void remove(final @Nullable Person person, final boolean log) {
        if (person == null) {
            return;
        }

        ensurePersonIsRegistered(person);
        if (person.equals(tech)) {
            removeTech();
        }

        boolean wasCrew = false;

        if (this.equals(person.getUnit())) {
            wasCrew = true;
            person.setUnit(null);
        }
        wasCrew |= drivers.remove(person);
        wasCrew |= gunners.remove(person);
        wasCrew |= vesselCrew.remove(person);
        if (person.equals(navigator)) {
            wasCrew = true;
            navigator = null;
        }

        if (person.equals(techOfficer)) {
            wasCrew = true;
            techOfficer = null;
        }

        if (person.equals(engineer)) {
            wasCrew = true;
            engineer = null;
        }

        if (wasCrew) {
            resetPilotAndEntity();
            MekHQ.triggerEvent(new PersonCrewAssignmentEvent(campaign, person, this));
        }

        if (log) {
            AssignmentLogger.removedFrom(person, getCampaign().getLocalDate(), getName());
            AssignmentLogger.removedFromTOEForce(getCampaign(),
                  person,
                  getCampaign().getLocalDate(),
                  getCampaign().getForceFor(this));
        }
    }

    public boolean isUnmanned() {
        return (null == getCommander());
    }

    /**
     * A trailer with no engine or weapons doesn't end any crew.
     *
     * @return true if this Unit is an unmanned trailer, false if it isn't a trailer or has a crew
     */
    public boolean isUnmannedTrailer() {
        if (getEntity() instanceof Tank tank) {
            return tank.isTrailer() && (tank.defaultCrewType().equals(CrewType.NONE));
        }

        return false;
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

    public Set<Person> getGunners() {
        return Collections.unmodifiableSet(gunners);
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

    // region Mothballing/Activation

    /**
     * Gets a value indicating whether the unit is being mothballed or activated.
     *
     * @return True if the unit is undergoing mothballing or activation, otherwise false.
     */
    public boolean isMothballing() {
        return mothballTime > 0;
    }

    /**
     * Gets the time (in minutes) remaining to mothball or activate the unit.
     *
     * @return The time (in minutes) remaining to mothball or activate the unit.
     */
    public int getMothballTime() {
        return mothballTime;
    }

    /**
     * Sets the time (in minutes) remaining to mothball or activate the unit.
     *
     * @param t The time (in minutes) remaining to mothball or activate the unit.
     */
    public void setMothballTime(int t) {
        mothballTime = max(t, 0);
    }

    /**
     * Gets a value indicating whether this unit is mothballed.
     *
     * @return True if the unit is mothballed, otherwise false.
     */
    public boolean isMothballed() {
        return mothballed;
    }

    /**
     * Sets a value indicating whether this unit is mothballed.
     * <p>
     * If the unit is being mothballed, all of its personnel will be removed. If the unit is being activated, all of its
     * personnel will be restored (if applicable) and its maintenance cycle will be reset.
     *
     * @param b True if the unit is now mothballed, or false if the unit is now activated.
     */
    public void setMothballed(boolean b) {
        this.mothballed = b;
        // Tech gets removed either way bug [#488]
        if (null != tech) {
            remove(tech, true);
        }
        if (mothballed) {
            // remove any other personnel
            for (Person p : getCrew()) {
                remove(p, true);
            }
            resetPilotAndEntity();
        } else {
            // start maintenance cycle over again
            resetDaysSinceMaintenance();
        }
    }

    /**
     * Begins mothballing a unit.
     *
     * @param mothballTech The tech performing the mothball.
     */
    public void startMothballing(Person mothballTech) {
        startMothballing(mothballTech, false);
    }

    /**
     * Begins mothballing a unit, optionally as a GM action.
     *
     * @param mothballTech The tech performing the mothball.
     * @param isGM         A value indicating if the mothball action should be performed immediately by the GM.
     */
    public void startMothballing(@Nullable Person mothballTech, boolean isGM) {
        if (!isMothballed() && MekHQ.getMHQOptions().getSaveMothballState()) {
            mothballInfo = new MothballInfo(this);
        }

        // set this person as tech
        if (!isSelfCrewed() && (tech != null) && !tech.equals(mothballTech)) {
            remove(tech, true);
        }
        tech = mothballTech;

        // don't remove personnel yet, because self crewed units need their crews to
        // mothball
        getCampaign().removeUnitFromForce(this);

        // clear any assigned tasks
        for (Part p : getParts()) {
            p.cancelAssignment(true);
        }

        if (!isGM) {
            setMothballTime(getMothballOrActivationTime());
            getCampaign().mothball(this);
        } else {
            completeMothball();
            getCampaign().addReport(getHyperlinkedName() + " has been mothballed");
        }
    }

    /**
     * Returns the engineer responsible for the mothballing or activation of this unit.
     *
     * @return Person the previous engineer that worked on this vessel, or an empty object.
     */
    public Optional<Person> engineerResponsible() {
        // if it is NOT self crewed, or it is NOT mothballed, just get the tech
        if (!isMothballed() || !isSelfCrewed()) {
            return Optional.ofNullable(getTech());
        } else {
            // if it is self crewed AND is mothballed and has a mothball info, get the tech
            if (isSelfCrewed() && isMothballed() && (this.mothballInfo != null)) {
                UUID previousTechId = this.mothballInfo.getTechId();
                var previousTech = campaign.getPerson(previousTechId);
                var previousTechExists = previousTech != null;
                var previousTechIsActive = previousTechExists && previousTech.getStatus().isActive();
                if (previousTechIsActive) {
                    return Optional.of(previousTech);
                }
            }
        }

        return Optional.empty();
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

        // We don't want to clear transport assignments, but we do want to remove the
        // transport from the list of potential transports, if it's transport.
        if (campaign != null) {
            if (!getTransportCapabilities(SHIP_TRANSPORT).isEmpty()) {
                getCampaign().removeCampaignTransporter(SHIP_TRANSPORT, this);
            }

            if (!getTransportCapabilities(CampaignTransportType.TACTICAL_TRANSPORT).isEmpty()) {
                getCampaign().removeCampaignTransporter(CampaignTransportType.TACTICAL_TRANSPORT, this);
            }
        }
    }

    /**
     * Begins activating a unit which has been mothballed.
     *
     * @param activationTech The tech performing the activation.
     */
    public void startActivating(Person activationTech) {
        startActivating(activationTech, false);
    }

    /**
     * Begins activating a unit which has been mothballed, optionally as a GM action.
     *
     * @param activationTech The tech performing the activation.
     * @param isGM           A value indicating if the activation action should be performed immediately by the GM.
     */
    public void startActivating(@Nullable Person activationTech, boolean isGM) {
        if (!isMothballed()) {
            return;
        }

        // set this person as tech
        if (!isSelfCrewed() && (tech != null) && !tech.equals(activationTech)) {
            remove(tech, true);
            tech = activationTech;
        } else if (!isSelfCrewed() && (null == tech)) {
            tech = activationTech;
        }

        if (isSelfCrewed() && !isConventionalInfantry()) {
            if (engineerResponsible().isPresent() && engineerResponsible().get().getStatus().isActive()) {
                var assignedEngineer = engineerResponsible().get();
                addVesselCrew(assignedEngineer);
            } else if (activationTech != null && activationTech.isTechLargeVessel()) {
                addVesselCrew(activationTech);
            } else if (!isGM) {
                // In this case there is nothing to be done, we cant activate this unit.
                return;
            }

            resetEngineer();
        } else {
            tech = activationTech;
        }

        if (!isGM) {
            setMothballTime(getMothballOrActivationTime());
            getCampaign().activate(this);
        } else {
            completeActivation();
            getCampaign().addReport(getHyperlinkedName() + " has been activated");
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

        // if we previously mothballed this unit, attempt to restore its pre-mothball
        // state
        if (mothballInfo != null) {
            mothballInfo.restorePreMothballInfo(this, getCampaign());
            mothballInfo = null;
        }

        // If this unit is a transport, let's add it to the campaign's
        // transporter map.
        if (campaign != null) {
            if (!getTransportCapabilities(SHIP_TRANSPORT).isEmpty()) {
                getCampaign().addCampaignTransport(SHIP_TRANSPORT, this);
            }

            if (!getTransportCapabilities(CampaignTransportType.TACTICAL_TRANSPORT).isEmpty()) {
                getCampaign().addCampaignTransport(CampaignTransportType.TACTICAL_TRANSPORT, this);
            }

            if (!getTransportCapabilities(CampaignTransportType.TOW_TRANSPORT).isEmpty()) {
                getCampaign().addCampaignTransport(CampaignTransportType.TOW_TRANSPORT, this);
            }
        }
    }

    /**
     * Gets the time required to mothball or activate this unit.
     *
     * @return The time in minutes required to mothball or activate this unit.
     */
    private int getMothballOrActivationTime() {
        // set mothballing time
        if (getEntity() instanceof Infantry) {
            return TECH_WORK_DAY;
        } else if ((getEntity() instanceof Dropship) || (getEntity() instanceof Jumpship)) {
            return TECH_WORK_DAY * (int) ceil(getEntity().getWeight() / 500.0);
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
    // endregion Mothballing/Activation

    /**
     * Returns all soldiers or battle armor assigned to the unit. As members of this category appear in both the drivers
     * and gunners list, we only check drivers
     */
    public List<Person> getAllInfantry() {
        return drivers.stream().filter(person -> entity instanceof Infantry).collect(Collectors.toList());
    }

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
     * @return true if the unit is fully crewed, false otherwise.
     */
    public boolean isFullyCrewed() {
        return getActiveCrew().size() == getFullCrewSize();
    }

    /**
     * Prototype TSM makes a unit harder to repair and maintain.
     *
     * @return Whether the unit has prototype TSM
     */
    public boolean hasPrototypeTSM() {
        for (Mounted<?> m : getEntity().getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TSM) && m.getType().hasFlag(MiscType.F_PROTOTYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a personnel count for each marine platoon/squad assigned to this unit
     *
     * @return The number of marines aboard
     */
    public int getMarineCount() {
        return 0;
        // TODO: implement Marines
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
     * @param person A <code>Person</code> in the campaign. The person need not be assigned to the unit as crew, in
     *               which case the return value will be false.
     *
     * @return Whether the person is considered the unit commander. If
     *       <code>person</code> is null or
     *       the unit has no crew, this method will return false
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

    public @Nullable Refit getRefit() {
        return refit;
    }

    public boolean isRefitting() {
        return null != refit;
    }

    public String getName() {
        return getFluffName().isBlank() ?
                     getEntity().getShortName() :
                     getEntity().getShortName() + " - " + getFluffName();
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

    public AvailabilityValue getAvailability(int era) {
        // take the highest availability of all parts
        AvailabilityValue availability = AvailabilityValue.A;
        for (Part p : parts) {
            AvailabilityValue newAvailability = p.getAvailability();
            // Taharqa: it's not clear whether a unit should really be considered extinct when its parts are extinct
            // as many probably outlive the production of parts it would be better to just use the unit extinction
            // date itself, but given that there are no canon extinction/re-intro dates for units, we will use this
            // instead
            if (p.isExtinct(getCampaign().getGameYear(), getCampaign().getFaction().isClan())) {
                newAvailability = AvailabilityValue.X;
            }
            if (newAvailability.isBetterThan(availability)) {
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

    /**
     * Calculates the maintenance time required for the associated entity, factoring in its type, weight class, and the
     * maintenance multiplier.
     *
     * <p>The method determines the time in minutes needed for maintenance based on the specific class of the entity
     * (such as Mek, ProtoMek, BattleArmor, Tank, SmallCraft, etc.) and applies a multiplier for any necessary
     * adjustments. Entity types not specifically handled by this method are considered self-maintaining and return 0.
     * </p>
     *
     * @return the total maintenance time in minutes required per day, or 0 if no maintenance is needed
     */
    public int getMaintenanceTime() {
        if (entity instanceof Mek) {
            return switch (entity.getWeightClass()) {
                case WEIGHT_ULTRA_LIGHT -> 30 * maintenanceMultiplier;
                case WEIGHT_LIGHT -> 45 * maintenanceMultiplier;
                case WEIGHT_MEDIUM -> 60 * maintenanceMultiplier;
                case WEIGHT_HEAVY -> 75 * maintenanceMultiplier;
                default -> 90 * maintenanceMultiplier;
            };
        }

        if (entity instanceof ProtoMek) {
            return 20 * maintenanceMultiplier;
        }

        if (entity instanceof BattleArmor battleArmor) {
            return 10 * battleArmor.getSquadSize() * maintenanceMultiplier;
        }

        if (entity instanceof Infantry) {
            return 0;
        }

        // This should remain commented out until we have implemented infantry-as-techs
        //        if (entity instanceof Infantry infantry) {
        //            if (infantry.getMovementMode().isJumpInfantry()) {
        //                return 30 * maintenanceMultiplier;
        //            }
        //
        //            if (infantry.getMovementMode().isTrackedWheeledOrHover()) {
        //                return 30 * maintenanceMultiplier;
        //            }
        //
        //            if (infantry.getMovementMode().isMotorizedInfantry()) {
        //                return 20 * maintenanceMultiplier;
        //            }
        //
        //            if (infantry.getMovementMode().isVTOL()) {
        //                return 40 * maintenanceMultiplier;
        //            }
        //
        //            if (infantry.getMovementMode().isSubmarine()) {
        //                return 20 * maintenanceMultiplier;
        //            }
        //
        //            return 10 * maintenanceMultiplier;
        //        }

        if (entity instanceof SupportTank || entity instanceof SupportVTOL) {
            return switch (entity.getWeightClass()) {
                case WEIGHT_SMALL_SUPPORT -> 20 * maintenanceMultiplier;
                case WEIGHT_MEDIUM_SUPPORT -> 35 * maintenanceMultiplier;
                default -> 100 * maintenanceMultiplier;
            };
        }

        if (entity instanceof Tank) {
            return switch (entity.getWeightClass()) {
                case WEIGHT_LIGHT -> 30 * maintenanceMultiplier;
                case WEIGHT_MEDIUM -> 50 * maintenanceMultiplier;
                case WEIGHT_HEAVY -> 75 * maintenanceMultiplier;
                case EntityWeightClass.WEIGHT_ASSAULT -> 90 * maintenanceMultiplier;
                default -> 120 * maintenanceMultiplier;
            };
        }

        if (entity instanceof ConvFighter) {
            return 45 * maintenanceMultiplier;
        }

        if (entity instanceof AeroSpaceFighter) {
            return switch (entity.getWeightClass()) {
                case WEIGHT_LIGHT -> 45 * maintenanceMultiplier;
                case WEIGHT_MEDIUM -> 60 * maintenanceMultiplier;
                default -> 75 * maintenanceMultiplier;
            };
        }

        if (entity instanceof Dropship) {
            return 180 * maintenanceMultiplier;
        }

        if (entity instanceof SmallCraft) {
            return 90 * maintenanceMultiplier;
        }

        if (entity instanceof Warship || entity instanceof SpaceStation) {
            return 480 * maintenanceMultiplier;
        }

        // At time of writing current maintenance errata states that maintenance time is only deducted on the day in
        // which maintenance takes place. However, these two unit times have maintenance time requirements that
        // exceed the maximum of 480 minutes a team has in a single day. Making these maintenance times impossible.
        // While we wait for rules confirmation we have instead set the time deduction to be 480 minutes, or a full day.
        //        if (entity instanceof SpaceStation) {
        //            return 1140 * maintenanceMultiplier;
        //        }
        //
        //        if (entity instanceof Warship) {
        //            return 1440 * maintenanceMultiplier;
        //        }

        if (entity instanceof Jumpship) {
            return 360 * maintenanceMultiplier;
        }

        // Anything that didn't fall into one of the above classifications is self-maintaining, meaning zero.
        return 0;
    }

    public void incrementDaysSinceMaintenance(Campaign campaign, boolean maintained, int asTechs) {
        List<Mission> activeMissions = campaign.getActiveMissions(false);
        double timeIncrease = 0.25;

        for (Mission mission : activeMissions) {
            if (mission instanceof AtBContract) {
                if (((AtBContract) mission).getContractType().isGarrisonDuty()) {
                    continue;
                }
            }

            timeIncrease = 1;
            break;
        }

        daysSinceMaintenance += timeIncrease;
        asTechDaysMaintained += asTechs * timeIncrease;
        if (maintained) {
            daysActivelyMaintained += timeIncrease;
        }
    }

    public void resetDaysSinceMaintenance() {
        daysSinceMaintenance = 0;
        daysActivelyMaintained = 0;
        asTechDaysMaintained = 0;
    }

    public double getDaysSinceMaintenance() {
        return daysSinceMaintenance;
    }

    /**
     * there are no official rules about partial maintenance lets say less than half is +2 more than half is +1 penalty
     * also we will take the average rounded down of the number of AsTechs to figure out shorthanded penalty
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public double getMaintainedPct() {
        return (daysActivelyMaintained / daysSinceMaintenance);
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getAsTechsMaintained() {
        return (int) Math.floor(asTechDaysMaintained / daysSinceMaintenance);
    }

    public int getMaintenanceMultiplier() {
        return maintenanceMultiplier;
    }

    public void setMaintenanceMultiplier(int value) {
        maintenanceMultiplier = value;
    }

    public PartQuality getQuality() {
        int nParts = 0;
        int sumQuality = 0;
        for (Part p : getParts()) {
            // no rules about this but let's assume missing parts are quality A
            if (p instanceof MissingPart) {
                nParts++;
            } else if (p.needsMaintenance()) {
                nParts++;
                sumQuality += p.getQuality().toNumeric();
            }
        }
        if (nParts == 0) {
            return QUALITY_D;
        }
        return PartQuality.fromNumeric((int) Math.round((1.0 * sumQuality) / nParts));
    }

    public void setQuality(PartQuality q) {
        for (Part p : getParts()) {
            if (!(p instanceof MissingPart)) {
                p.setQuality(q);
            }
        }
    }

    public String getQualityName() {
        return getQuality().toName(getCampaign().getCampaignOptions().isReverseQualityNames());
    }

    public boolean requiresMaintenance() {
        if (!isAvailable()) {
            return false;
        }
        return !(getEntity() instanceof Infantry) || getEntity() instanceof BattleArmor;
    }

    /**
     * Not always opposite to isUnmaintained() - both are false for units that do not require maintenance.
     *
     * @return true if unit requires maintenance and has a tech assigned, false otherwise.
     *
     * @see #isUnmaintained()
     */
    public boolean isMaintained() {
        return requiresMaintenance() && (getTech() != null);
    }

    /**
     * Not always opposite to isMaintained() - both are false for units that do not require maintenance.
     *
     * @return true if unit requires maintenance and does not have a tech assigned, false otherwise.
     *
     * @see #isMaintained()
     */
    public boolean isUnmaintained() {
        return requiresMaintenance() && (getTech() == null);
    }

    public boolean isSelfCrewed() {
        return (getEntity() instanceof Dropship) || (getEntity() instanceof Jumpship) || isConventionalInfantry();
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
     * Removes all the parts from a unit.
     * <p>
     * NOTE: this puts the unit in an inconsistent state, and the unit should not be used until its parts have been
     * re-assigned.
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
     * Checks to see if a particular BA suit on BA is currently operable This requires the suit to not be destroyed and
     * to have not missing equipment parts
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

    /**
     * Checks if the associated entity is classified as battle armor.
     *
     * <p>
     * This method determines whether the entity linked to this object is considered battle armor. It first verifies
     * that the entity is not null, and then checks if the entity meets the criteria for battle armor.
     * </p>
     *
     * @return {@code true} if the entity is classified as battle armor and is not null, otherwise {@code false}.
     */
    public boolean isBattleArmor() {
        return (getEntity() != null) && getEntity().isBattleArmor();
    }

    public boolean isIntroducedBy(int year) {
        return null != entity && entity.getYear() <= year;
    }

    public boolean isExtinctIn(int year) {
        // TODO: currently we do not track this in MM (and I don't think it really exists, but I am adding the code
        //  elsewhere to take advantage of this method if we do code it.
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
        return "<b>Spare Parts</b>: " +
                     getSparePartsCost().toAmountAndSymbolString() +
                     "<br>" +
                     "<b>Ammunition</b>: " +
                     getAmmoCost().toAmountAndSymbolString() +
                     "<br>" +
                     "<b>Fuel</b>: ~" +
                     getFuelCost(0).toAmountAndSymbolString() +
                     "<br>";
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
        } else if ((entity instanceof Mek) || (entity instanceof BattleArmor)) {
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
                LOGGER.error("{} is not a generic CI. Movement mode is {}",
                      getName(),
                      entity.getMovementModeAsString());
            }
        } else {
            // Only ProtoMeks should fall here. Anything else needs to be logged
            if (!(entity instanceof ProtoMek)) {
                LOGGER.error("{} has no Spare Parts value for unit type {}",
                      getName(),
                      Entity.getEntityTypeName(entity.getEntityType()));
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
            TechRating rating = getTechRating();
            if (((currentYear > 2859) && (currentYear < 3040)) &&
                      (!getCampaign().getFaction().isClan() && !getCampaign().getFaction().isComStar())) {
                if (rating.isBetterThan(TechRating.D)) {
                    partsCost = partsCost.multipliedBy(5.0);
                }
            }

            if (rating.equals(TechRating.E)) {
                partsCost = partsCost.multipliedBy(1.1);
            } else if (rating.equals(TechRating.F)) {
                partsCost = partsCost.multipliedBy(1.25);
            }

            if ((entity instanceof Tank) && (engine.getEngineType() == Engine.NORMAL_ENGINE)) {
                partsCost = partsCost.multipliedBy(2.0);
            }

            if (!(entity instanceof Infantry)) {
                if ((engine.getEngineType() == Engine.XL_ENGINE) || (engine.getEngineType() == Engine.XXL_ENGINE)) {
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

    /**
     * Calculates the monthly fuel cost for this unit, applying any available hydrogen production credits.
     *
     * <p>Different entity types have different fuel requirements:</p>
     * <ul>
     *   <li>Large Craft and Small Craft: Calculated based on tons burned per day</li>
     *   <li>Conventional Fighters: Use fighter-specific fuel cost calculation</li>
     *   <li>Aerospace Fighters: Based on fuel tonnage multiplied by a factor of 4</li>
     *   <li>Vehicles and Meks: Use vehicle-specific fuel cost calculation</li>
     *   <li>Infantry: Use infantry-specific fuel cost calculation</li>
     * </ul>
     *
     * <p>Hydrogen produced by fusion engines is credited against the unit's hydrogen usage, reducing the overall
     * fuel cost.</p>
     *
     * @param hydrogenProduction The amount of hydrogen produced by fusion engines, which offsets hydrogen usage costs
     *
     * @return The calculated fuel cost as a Money object, always non-negative
     */
    public Money getFuelCost(int hydrogenProduction) {
        final int FUEL_COST_PER_HYDROGEN = 15000;

        LOGGER.debug("getFuelCost: {} hydrogen production", hydrogenProduction);

        Money fuelCost = Money.zero();
        double hydrogenUsage = 0;

        // Check if the engine is null. This can occur if the entity does not have an engine installed.
        Engine engine = entity.getEngine();
        if (engine == null) {
            return Money.zero();
        }

        // Calculate base fuel costs by entity type
        if (entity.isLargeCraft() || entity.isSmallCraft()) {
            hydrogenUsage = getCraftMonthlyHydrogenUsage(entity);
        } else if (entity.isConventionalFighter()) {
            if (engine.isFusion()) {
                hydrogenUsage = ((Aero) entity).getFuelTonnage() * 4.0;
            } else {
                fuelCost = fuelCost.plus(getFighterFuelCost(entity));
            }
        } else if (entity.isAerospaceFighter()) {
            try {
                hydrogenUsage = ((AeroSpaceFighter) entity).getFuelTonnage() * 4.0;
            } catch (ClassCastException e) {
                LOGGER.error("{} was thought to be an AeroSpace Fighter, but actually it isn't." +
                                   " This should be looked into.", getName());
            }
        } else if (entity.isVehicle() || entity.isMek()) {
            fuelCost = fuelCost.plus(getVehicleFuelCost(entity));
        } else if (entity.isInfantry()) {
            fuelCost = fuelCost.plus(getInfantryFuelCost(entity));
        }

        // Apply hydrogen production credit if there is any hydrogen usage
        if (hydrogenUsage > 0) {
            LOGGER.debug("getFuelCost: {} hydrogen usage", hydrogenUsage);
            // Ensure hydrogen usage doesn't go negative after applying production credit
            double actualHydrogenUsage = Math.max(0, hydrogenUsage - hydrogenProduction);
            LOGGER.debug("getFuelCost: {} hydrogen usage after production credit", actualHydrogenUsage);
            fuelCost = fuelCost.plus(Money.of(actualHydrogenUsage * FUEL_COST_PER_HYDROGEN));
            LOGGER.debug("getFuelCost: {} fuel cost", fuelCost);
        }

        return fuelCost;
    }

    /**
     * Calculates the average monthly hydrogen fuel usage for a given spacefaring entity.
     *
     * <p>The calculation is based on the entity's class and mass, with values derived from CamOps. Different formulas
     * are applied depending on whether the entity is a DropShip (with subtypes and weight considerations), Small Craft,
     * Jumpship, or Warship.</p>
     *
     * @param entity the {@link Entity} representing the spacecraft whose monthly hydrogen usage is to be calculated
     *
     * @return the total tons of hydrogen fuel consumed in an average month for the provided entity
     */
    public double getCraftMonthlyHydrogenUsage(Entity entity) {
        final int NON_JUMPSHIP_BURN_PER_DAY = 15;
        final int JUMPSHIP_BURN_PER_DAY = 3;

        double tonsBurnDay = 0;
        if (entity instanceof Dropship) {
            if (((SmallCraft) entity).getDesignType() != Dropship.MILITARY) {
                if (entity.getWeight() < 1000) {
                    tonsBurnDay = 1.84;
                } else if (entity.getWeight() < 4000) {
                    tonsBurnDay = 2.82;
                } else if (entity.getWeight() < 9000) {
                    tonsBurnDay = 3.37;
                } else if (entity.getWeight() < 20000) {
                    tonsBurnDay = 4.22;
                } else if (entity.getWeight() < 30000) {
                    tonsBurnDay = 6.52;
                } else if (entity.getWeight() < 40000) {
                    tonsBurnDay = 7.71;
                } else if (entity.getWeight() < 50000) {
                    tonsBurnDay = 7.74;
                } else if (entity.getWeight() < 70000) {
                    tonsBurnDay = 8.37;
                } else {
                    tonsBurnDay = 8.83;
                }
            } else {
                tonsBurnDay = 1.84;
            }
            return tonsBurnDay * NON_JUMPSHIP_BURN_PER_DAY;
        } else if ((entity instanceof SmallCraft)) {
            return 1.84 * NON_JUMPSHIP_BURN_PER_DAY;
        } else if (entity instanceof Jumpship) {
            if (entity.getWeight() < 50000) {
                tonsBurnDay = 2.82;
            } else if (entity.getWeight() < 100000) {
                tonsBurnDay = 9.77;
            } else if (entity.getWeight() < 200000) {
                tonsBurnDay = 19.75;
            } else {
                tonsBurnDay = 39.52;
            }
            if (entity instanceof Warship) {
                return tonsBurnDay * NON_JUMPSHIP_BURN_PER_DAY;
            }
            return tonsBurnDay * JUMPSHIP_BURN_PER_DAY;
        }
        return tonsBurnDay;
    }

    /**
     * Use {@link #getCraftMonthlyHydrogenUsage(Entity)} instead, so that hydrogen discounts can be accounted for
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public Money getTonsBurnMonthCost(Entity entity) {
        double tonsBurnDay = 0;
        if (entity instanceof Dropship) {
            if (((SmallCraft) entity).getDesignType() != Dropship.MILITARY) {
                if (entity.getWeight() < 1000) {
                    tonsBurnDay = 1.84;
                } else if (entity.getWeight() < 4000) {
                    tonsBurnDay = 2.82;
                } else if (entity.getWeight() < 9000) {
                    tonsBurnDay = 3.37;
                } else if (entity.getWeight() < 20000) {
                    tonsBurnDay = 4.22;
                } else if (entity.getWeight() < 30000) {
                    tonsBurnDay = 6.52;
                } else if (entity.getWeight() < 40000) {
                    tonsBurnDay = 7.71;
                } else if (entity.getWeight() < 50000) {
                    tonsBurnDay = 7.74;
                } else if (entity.getWeight() < 70000) {
                    tonsBurnDay = 8.37;
                } else {
                    tonsBurnDay = 8.83;
                }
            } else {
                tonsBurnDay = 1.84;
            }
            return Money.of(tonsBurnDay * 15 * 15000);
        } else if ((entity instanceof SmallCraft)) {
            return Money.of(1.84 * 15 * 15000);
        } else if (entity instanceof Jumpship) {
            if (entity.getWeight() < 50000) {
                tonsBurnDay = 2.82;
            } else if (entity.getWeight() < 100000) {
                tonsBurnDay = 9.77;
            } else if (entity.getWeight() < 200000) {
                tonsBurnDay = 19.75;
            } else {
                tonsBurnDay = 39.52;
            }
            if (entity instanceof Warship) {
                return Money.of(tonsBurnDay * 15 * 15000);
            }
            return Money.of(tonsBurnDay * 3 * 15000);
        }
        return Money.of(tonsBurnDay);
    }

    public Money getFighterFuelCost(Entity entity) {
        return Money.of(((Aero) entity).getFuelTonnage() * 4.0 * 1000.0);
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
     * @return Tech progression data for this unit, using the campaign faction if the useFactionIntroDate option is
     *       enabled.
     */
    private ITechnology getTechProgression() {
        return getTechProgression(getCampaign().getTechFaction());
    }

    private ITechnology getTechProgression(Faction techFaction) {
        // If useFactionIntroDate is false, use the base data that was calculated for the Entity when it was loaded.
        if (techFaction.equals(Faction.NONE)) {
            return getEntity();
        }
        // First check whether it has already been calculated for this faction, but don't wait if it hasn't.
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
    public TechBase getTechBase() {
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
    public TechRating getTechRating() {
        return getTechProgression().getTechRating();
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return getTechProgression().getBaseAvailability(era);
    }

    @Override
    public int getIntroductionDate(boolean clan, Faction faction) {
        return getTechProgression(faction).getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, Faction faction) {
        return getTechProgression(faction).getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, Faction faction) {
        return getTechProgression(faction).getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, Faction faction) {
        return getTechProgression(faction).getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, Faction faction) {
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

    public SimpleTechLevel getSimpleTechLevel(int year, boolean clan, Faction faction) {
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
    public AvailabilityValue calcYearAvailability(int year, boolean clan, Faction faction) {
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
                LOGGER.error("Unit {} ('{}') references missing tech {}", getId(), getName(), id);
            }
        }
        for (int ii = drivers.size() - 1; ii >= 0; --ii) {
            Person driver = drivers.get(ii);
            if (driver instanceof UnitPersonRef) {
                drivers.set(ii, campaign.getPerson(driver.getId()));
                if (drivers.get(ii) == null) {
                    LOGGER.error("Unit {} ('{}') references missing driver {}", getId(), getName(), driver.getId());
                    drivers.remove(ii);
                }
            }
        }
        for (Person gunner : new HashSet<>(gunners)) {
            if (gunner instanceof UnitPersonRef) {
                Person updatedGunner = campaign.getPerson(gunner.getId());
                if (updatedGunner != null) {
                    if (!gunners.remove(gunner)) { // Remove gunner person ref & log if it fails
                        LOGGER.warn("Unit {} ('{}') could not remove person ref {}",
                              getId(),
                              getName(),
                              gunner.getId());
                    }
                    if (!gunners.add(updatedGunner)) { // Add gunner person & log if it fails
                        LOGGER.warn("Unit {} ('{}') could not add person {}",
                              getId(),
                              getName(),
                              updatedGunner.getId());
                    }
                } else {
                    LOGGER.error("Unit {} ('{}') references missing gunner {}", getId(), getName(), gunner.getId());
                    gunners.remove(gunner);
                }
            }
        }

        for (int ii = vesselCrew.size() - 1; ii >= 0; --ii) {
            Person crew = vesselCrew.get(ii);
            if (crew instanceof UnitPersonRef) {
                vesselCrew.set(ii, campaign.getPerson(crew.getId()));
                if (vesselCrew.get(ii) == null) {
                    LOGGER.error("Unit {} ('{}') references missing vessel crew {}", getId(), getName(), crew.getId());
                    vesselCrew.remove(ii);
                }
            }
        }

        if (engineer instanceof UnitPersonRef) {
            UUID id = engineer.getId();
            engineer = campaign.getPerson(id);
            if (engineer == null) {
                LOGGER.error("Unit {} ('{}') references missing engineer {}", getId(), getName(), id);
            }
        }

        if (navigator instanceof UnitPersonRef) {
            UUID id = navigator.getId();
            navigator = campaign.getPerson(id);
            if (navigator == null) {
                LOGGER.error("Unit {} ('{}') references missing navigator {}", getId(), getName(), id);
            }
        }

        if (getTechOfficer() instanceof UnitPersonRef) {
            final UUID id = getTechOfficer().getId();
            techOfficer = campaign.getPerson(id);
            if (getTechOfficer() == null) {
                LOGGER.error("Unit {} ('{}') references missing tech officer {}", getId(), getName(), id);
            }
        }

        if (hasTransportShipAssignment()) {
            getTransportShipAssignment().fixReferences(campaign, this);
        }

        if (hasTacticalTransportAssignment()) {
            getTacticalTransportAssignment().fixReferences(campaign, this);
        }

        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            if (hasTransportedUnits(campaignTransportType)) {
                getTransportedUnitsSummary(campaignTransportType).fixReferences(campaign, this);
                initializeTransportSpace(campaignTransportType);
            }
        }
    }

    /**
     * Generates a random unit quality based on a 2d6 roll and a modifier.
     *
     * @param modifier the modifier to be applied to the 2d6 roll
     *
     * @return an integer representing the generated unit quality
     *
     * @throws IllegalStateException if an unexpected value is encountered during the switch statement
     */
    public static PartQuality getRandomUnitQuality(int modifier) {
        int roll = MathUtility.clamp((Compute.d6(2) + modifier), 2, 12);

        return switch (roll) {
            case 2, 3 -> QUALITY_A;
            case 4, 5 -> QUALITY_B;
            case 6, 7 -> QUALITY_C;
            case 8, 9 -> QUALITY_D;
            case 10, 11 -> QUALITY_E;
            case 12 -> QUALITY_F;
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/unit/Unit.java/getRandomUnitQuality: " + roll);
        };
    }
}
