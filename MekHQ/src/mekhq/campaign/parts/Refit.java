/*
 * Refit.java
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
package mekhq.campaign.parts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.InfantryAttack;
import megamek.logging.MMLogger;
import megameklab.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitRefitEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.RefitClass;
import mekhq.campaign.parts.enums.RefitStepType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.cleanup.EquipmentUnscrambler;
import mekhq.campaign.unit.cleanup.EquipmentUnscramblerResult;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;

/**
 * This object tracks the refit of a given unit into a new unit. It has fields for the current
 * entity and the new entity and it uses these to calculate various characteristics of the refit.
 *
 * It can then also be used to track the actual refit process, by attaching it to a Unit.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Refit extends Part implements IAcquisitionWork {
    private static final MMLogger logger = MMLogger.create(Refit.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
            MekHQ.getMHQOptions().getLocale());

    // These should live elsewhere eventually
    public static final int WORKHOUR = 60;
    public static final int WORKDAY = 480;
    public static final int WORKWEEK = 3360; // 7-day workweek
    public static final int WORKMONTH = 14400; // 30 day month

    private Unit oldUnit;
    private Entity newEntity;

    private RefitClass refitClass;
    private int unmodifiedTime;
    private int time;
    private int timeSpent;
    private Money cost;
    private boolean failedCheck;
    private boolean customJob;
    private boolean isRefurbishing;
    private boolean isSavingFile;
    private boolean kitFound;
    private boolean replacingLocations;
    private boolean isOmniRefit;
    private StringJoiner errorStrings;

    private List<Part> oldUnitParts;
    private List<Part> newUnitParts;
    private List<Part> neededList;
    private List<Part> returnsList;
    private List<Part> oldIntegratedHeatSinks;
    private Set<Part> largeCraftBinsToChange;

    private List<RefitStep> stepsList;

    private int armorNeeded;
    private Armor newArmorSupplies;
    private boolean sameArmorType;

    private Person assignedTech;

    /**
     * Blank refit constructor. Probably should not be used.
     */
    public Refit() {
        oldUnitParts = new ArrayList<Part>();
        newUnitParts = new ArrayList<Part>();
        neededList = new ArrayList<Part>();
        returnsList = new ArrayList<Part>();
        stepsList = new ArrayList<RefitStep>();
        oldIntegratedHeatSinks = new ArrayList<>();
        largeCraftBinsToChange = new HashSet<>();
        errorStrings = new StringJoiner("\n");
        cost = Money.zero();
    }

    /**
     * Standard Refit Constructor
     * @param oldUnit - The unit that is to be refit
     * @param newEntity - The target design for the unit to be refit into
     * @param custom - Is this custom work vs using a factory refit kit
     * @param refurbish - Are we refurbishing this unit rather than refitting it
     * @param saveFile - Does the new unit need to be saved to disk
     */
    public Refit(Unit oldUnit, Entity newEntity, boolean custom, boolean refurbish, boolean saveFile) {
        this();
        isRefurbishing = refurbish;
        customJob = custom;
        isSavingFile = saveFile;
        this.oldUnit = oldUnit;
        this.newEntity = newEntity;
        newEntity.setOwner(oldUnit.getEntity().getOwner());
        newEntity.setGame(oldUnit.getEntity().getGame());
        if (newEntity.getClass() == SmallCraft.class) { // SmallCraft but not subclasses
            // Entity.setGame() will add a Single Hex ECM part to SmallCraft that otherwise
            // has no ECM. This is required for MegaMek, but causes SmallCraft to be overweight when
            // refitting in MekHQ. Work-around is to remove the ECM part early during a refit.
            // Ref: https://github.com/MegaMek/mekhq/issues/1970
            newEntity.removeMisc(BattleArmor.SINGLE_HEX_ECM);
        }
        failedCheck = false;
        timeSpent = 0;
        kitFound = false;
        replacingLocations = false;
        campaign = oldUnit.getCampaign();

        analyze();
        figureRefitClass();
        figureRefitTime();

        optimizeShoppingLists();
        if (customJob) {
            suggestNewName();
        }
    }

    @Override
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * @return Is the old unit using the same armor type as the new unit
     */
    public boolean isSameArmorType() {
        return sameArmorType;
    }

       /**
     * @return the printable name of our refit class
     */
    public String getRefitClassName() {
        return refitClass.toName();
    }

    /**
     * @return the integer representing our refit class
     */
    public RefitClass getRefitClass() {
        return refitClass;
    }

    /**
     * @return The cost of carrying out this refit
     */
    public Money getCost() {
        return cost;
    }

    /**
     * @return A list of parts required to make this refit happen
     */
    public List<Part> getNeededList() {
        return neededList;
    }

    /**
     * @return A list of parts that will be reclaimed by doing this refit
     */
    public List<Part> getReturnsList() {
        return returnsList;
    }

    /**
     * @return A list of steps required to carry out this refit
     */
    public List<RefitStep> getStepsList() {
        return stepsList;
    }

    /**
     * @return the time this refit will take
     */
    public int getTime() {
        return time;
    }

    /**
     * @return the time not modified by the refit class. Mainly for UI.
     */
    public int getUnmodifiedTime() {
        return unmodifiedTime;
    }

    /**
     * @return has the analysis determined that this is an omni refit?
     */
    public boolean isOmniRefit() {
        return isOmniRefit;
    }

    /**
     * Do all the grunt work to determine what parts are being added, removed, moved, and what other
     * manipulations are happening in this refit. We locate the items and then pass off to RefitStep
     * set up the fine details of the exchange and determine things like time and class.
     */
    public void analyze() {
        Unit newUnit = new Unit(newEntity, getCampaign());
        newUnit.initializeParts(false);

        // Lists of parts to go through. The goal is to remove everything from both lists.
        List<Part> oldParts = new ArrayList<Part>(oldUnit.getParts());
        List<Part> newParts = new ArrayList<Part>(newUnit.getParts());
        Iterator<Part> oldIterator;
        Iterator<Part> newIterator;

        boolean[] brokenLocations = new boolean[oldUnit.getEntity().locations()];

        // region Armor

        oldIterator = oldParts.iterator();
        while (oldIterator.hasNext()) {
            Part oldPart = oldIterator.next();
            if (oldPart instanceof Armor) {
                Armor oldArmor = (Armor) oldPart;
        
                if (isInvalidAeroArmor(oldUnit, oldArmor)) {
                    // Ignore this one
                    oldIterator.remove();
                    continue;
                }

                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
                    if (newPart instanceof Armor) {
                        Armor newArmor = (Armor) newPart;
                        
                        if ((oldArmor.getLocation() == newArmor.getLocation())
                                && (oldArmor.isRearMounted() == newArmor.isRearMounted())) {
                            matchFound = true;
                            stepsList.add(new RefitStep(oldUnit, oldArmor, newArmor));
                            break;
                        }
                    }
                }
                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    // There's probably a turret being removed or something
                    stepsList.add(new RefitStep(oldUnit, oldArmor, null));
                    oldIterator.remove();
                }
            }
        }

        // Sanity check that we found all the armor on both units
        newIterator = newParts.iterator();
        while (newIterator.hasNext()) {
            Part newPart = newIterator.next();
            if (newPart instanceof Armor) {

                if (isInvalidAeroArmor(newUnit, (Armor) newPart)) {
                    // Ignore this one
                    newIterator.remove();
                    continue;
                }

                // There's probably a turret being added or something
                stepsList.add(new RefitStep(oldUnit, null, newPart));
                newIterator.remove();
            }
        }


        // region Locations

        oldIterator = oldParts.iterator();
        while (oldIterator.hasNext()) {
            Part oldPart = oldIterator.next();
            if ((oldPart instanceof MekLocation) || (oldPart instanceof MissingMekLocation)) {
        
                boolean matchFound = false;
                RefitStep refitStep = null;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
                    if (newPart instanceof MekLocation) { // New unit better not have missing locations
                        if (oldPart.getLocation() == newPart.getLocation()) {
                            matchFound = true;
                            refitStep = new RefitStep(oldUnit, oldPart, newPart);
                            stepsList.add(refitStep);
                            
                            break;
                        }
                    }
                }
                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    // This should create an error state in the UI
                    refitStep = new RefitStep(oldUnit, oldPart, null);
                    stepsList.add(refitStep);
                }

                if ((null != refitStep) && (refitStep.getType() != RefitStepType.CHANGE_STRUCTURE_TYPE)
                        && ((oldPart instanceof MissingMekLocation)
                            || ((MekLocation) oldPart).isBlownOff()
                            || ((MekLocation) oldPart).isBreached()
                            || ((MekLocation) oldPart).onBadHipOrShoulder())) {
                    // If we're not getting rid of the location due to a structure swap and the
                    // location is an invalid location for new parts, mark the location
                    brokenLocations[oldPart.getLocation()] = true;
                }

                    

            } else if ((oldPart instanceof Rotor) || (oldPart instanceof MissingRotor)) {

                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
                    if (newPart instanceof Rotor) { 
                        
                        // MissingRotors don't have locations so we'll have to take the match on faith
                        matchFound = true;
                        stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                        break;
                    
                    }
                }
                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    // This should create an error state in the UI
                    stepsList.add(new RefitStep(oldUnit, oldPart, null));
                    newIterator.remove();
                }
            } else if ((oldPart instanceof Turret) || (oldPart instanceof MissingTurret)) {

                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
                    if (newPart instanceof Turret) { 

                        // MissingTurrets don't have locations so we'll have to take the match on faith
                        matchFound = true;
                        stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                        break;
                    }
                }
                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    // This should create an error state in the UI
                    stepsList.add(new RefitStep(oldUnit, oldPart, null));
                    newIterator.remove();
                }
            } else if (oldPart instanceof TankLocation) {

                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
                    if (newPart instanceof TankLocation) {
                        if (oldPart.getLocation() == newPart.getLocation()) {
                            matchFound = true;
                            stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                            break;
                        }
                    }
                }
                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    // This should create an error state in the UI
                    stepsList.add(new RefitStep(oldUnit, oldPart, null));
                    newIterator.remove();
                }
            }
            
        }

        // Sanity check that we found all the locations on both units
        newIterator = newParts.iterator();
        while (newIterator.hasNext()) {
            Part newPart = newIterator.next();
            if (newPart instanceof MekLocation) {
                stepsList.add(new RefitStep(oldUnit, null, newPart)); // Error state
                newIterator.remove();
            } else if (newPart instanceof Rotor) {
                stepsList.add(new RefitStep(oldUnit, null, newPart)); // Error state
                newIterator.remove();
            } else if (newPart instanceof Turret) {
                stepsList.add(new RefitStep(oldUnit, null, newPart));
                newIterator.remove();
            } else if (newPart instanceof TankLocation) {
                stepsList.add(new RefitStep(oldUnit, null, newPart)); // Error state
                newIterator.remove();
            }
        }

        // region CASE

        for(int loc = 0; loc < oldUnit.getEntity().locations(); loc++) {
            CASE oldCASE = CASE.getCaseFor(loc, oldUnit, campaign);
            CASE newCASE = CASE.getCaseFor(loc, newUnit, campaign);
            
            if ((null != oldCASE) && (null != newCASE)) {
                if (oldCASE.isSamePartType(newCASE)) {
                    stepsList.add(new RefitStep(oldUnit, oldCASE, newCASE));
                } else {
                    stepsList.add(new RefitStep(oldUnit, oldCASE, null));
                    stepsList.add(new RefitStep(oldUnit, null, newCASE));
                }
            } else if ((null != oldCASE) || (null != newCASE)) {
                stepsList.add(new RefitStep(oldUnit, oldCASE, newCASE));
            }
        }


        // region Actuators

        oldIterator = oldParts.iterator();
        while (oldIterator.hasNext()) {
            Part oldPart = oldIterator.next();

            if ((oldPart instanceof MekActuator) || (oldPart instanceof MissingMekActuator)) {
                int oldLoc = oldPart.getLocation();
                int oldType = (oldPart instanceof MekActuator) ? 
                        ((MekActuator) oldPart).getType() : ((MissingMekActuator) oldPart).getType();
                
                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();

                    if ((newPart instanceof MekActuator) 
                            && (oldLoc == newPart.getLocation()) && (oldType == (((MekActuator) newPart).getType()))) {
                        
                        stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                        matchFound = true;
                        break;
                    }
                }

                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    oldIterator.remove();
                    stepsList.add(new RefitStep(oldUnit, oldPart, null));
                }
            }
        }

        newIterator = newParts.iterator();
        while (newIterator.hasNext()) {
            Part newPart = newIterator.next();

            if (newPart instanceof MekActuator) {
                newIterator.remove();
                stepsList.add(new RefitStep(oldUnit, null, newPart));
            }
        }


        // region Core Equipment

        // Engine

        Part oldEngine = findOnly(EnginePart.class, MissingEnginePart.class, oldParts, oldUnit);
        if (null != oldEngine) {
            Part newEngine = findOnly(EnginePart.class, null, newParts, newUnit);

            RefitStep engineStep = new RefitStep(oldUnit, oldEngine, newEngine);
            if ((engineStep.getRefitClass() == RefitClass.CLASS_E) && (!customJob)) {
                engineStep.setRefitClass(RefitClass.CLASS_D);
            }
            stepsList.add(engineStep);
        }

        // Gyro

        Part oldGyro = findOnly(MekGyro.class, MissingMekGyro.class, oldParts, oldUnit);
        if (null != oldGyro) {
            Part newGyro = findOnly(MekGyro.class, null, newParts, newUnit);
            stepsList.add(new RefitStep(oldUnit, oldGyro, newGyro));
        }

        // Cockpit

        Part oldCockpit = findOnly(MekCockpit.class, MissingMekCockpit.class, oldParts, oldUnit);
        if (null != oldCockpit) {
            Part newCockpit = findOnly(MekCockpit.class, null, newParts, newUnit);
            stepsList.add(new RefitStep(oldUnit, oldCockpit, newCockpit));
        }

        // Sensors

        Part oldSensors = findOnly(MekSensor.class, MissingMekSensor.class, oldParts, oldUnit);
        if (null != oldSensors) {
            Part newSensors = findOnly(MekSensor.class, null, newParts, newUnit);
            stepsList.add(new RefitStep(oldUnit, oldSensors, newSensors));
        }

        // Life Support

        Part oldLS = findOnly(MekLifeSupport.class, MissingMekLifeSupport.class, oldParts, oldUnit);
        if (null != oldLS) {
            Part newLS = findOnly(MekLifeSupport.class, null, newParts, newUnit);
            stepsList.add(new RefitStep(oldUnit, oldLS, newLS));
        }

        // Aero / SC Life Support

        Part oldALS = findOnly(AeroLifeSupport.class, MissingAeroLifeSupport.class, oldParts, oldUnit);
        if (null != oldALS) {
            Part newALS = findOnly(AeroLifeSupport.class, null, newParts, newUnit);
            Part matchPart;
            if (oldALS instanceof MissingPart) {
                matchPart = ((MissingPart) oldALS).getNewPart();
            } else {
                matchPart = oldALS;
            }
            // !crewSizeChanged - only change life support if actual crew size changes, not if misc
            // bay personnel change. Will save a lot of time on some refits...
            if (!crewSizeChanged() || matchPart.isSamePartType(newALS)) {
                stepsList.add(new RefitStep(oldUnit, oldALS, newALS));
            } else {
                stepsList.add(new RefitStep(oldUnit, oldALS, null));
                stepsList.add(new RefitStep(oldUnit, null, newALS));
            }

        }


        // Untracked Heat Sinks

        Part oldUHS = oldUnit.getUntrackedHeatSinks();
        Part newUHS = newUnit.getUntrackedHeatSinks();
        if ((null != oldUHS) || (null != newUHS)) { 
            stepsList.add(new RefitStep(oldUnit, oldUHS, newUHS, true));
        } else {
            // If we don't have UHS, we probably have a Spacecraft Cooling System
            Part oldSCCS = findOnly(SpacecraftCoolingSystem.class, null, oldParts, oldUnit);
            Part newSCCS = findOnly(SpacecraftCoolingSystem.class, null, newParts, newUnit);
            stepsList.add(new RefitStep(oldUnit, oldSCCS, newSCCS));
        }


        // region Ammo Bins :<

        oldIterator = oldParts.iterator();
        while (oldIterator.hasNext()) {
            Part oldPart = oldIterator.next();

            if ((oldPart instanceof AmmoBin) || (oldPart instanceof MissingAmmoBin)) {

                if (((oldPart instanceof AmmoBin && ((AmmoBin) oldPart).isOneShot()))
                        || (((oldPart instanceof MissingAmmoBin) && ((MissingAmmoBin) oldPart).isOneShot()))) {

                    // One-shot ammo bins are even more meta than regular ammo bins, we're not going
                    // to consider them as elements of a refit
                    oldIterator.remove();
                    continue;
                }

                int oldLoc = oldPart.getLocation();
                AmmoType oldType = (oldPart instanceof AmmoBin) ? 
                        ((AmmoBin) oldPart).getType() : ((MissingAmmoBin) oldPart).getType();
                
                boolean matchFound = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();

                    if ((newPart instanceof AmmoBin) 
                            && (oldLoc == newPart.getLocation()) 
                            && (oldType.equalsAmmoTypeOnly(((AmmoBin) newPart).getType()))
                            && (((AmmoBin) oldPart).getFullShots() == ((AmmoBin) newPart).getFullShots())) {
                        
                        matchFound = true;
                        stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                        break;
                    }
                }

                if (matchFound) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    boolean movedMatchFound = false;
                    newIterator = newParts.iterator();
                    while (newIterator.hasNext()) {
                        Part newPart = newIterator.next();
    
                        if ((newPart instanceof AmmoBin) 
                                && (oldType.equalsAmmoTypeOnly(((AmmoBin) newPart).getType()))
                                && (((AmmoBin) oldPart).getFullShots() == ((AmmoBin) newPart).getFullShots())) {

                            stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                            movedMatchFound = true;
                            break;
                        }
                    }
                    if (movedMatchFound) {
                        oldIterator.remove();
                        newIterator.remove();
                    } else {
                        oldIterator.remove();
                        stepsList.add(new RefitStep(oldUnit, oldPart, null));
                    }
                }
            }
        }

        newIterator = newParts.iterator();
        while (newIterator.hasNext()) {
            Part newPart = newIterator.next();

            if (newPart instanceof AmmoBin) {
                newIterator.remove();
                if (!((AmmoBin) newPart).isOneShot()) {
                    stepsList.add(new RefitStep(oldUnit, null, newPart));
                }
            }
        }


        // region TransportBays

        // These should hall have location none so we don't need to handle moves?

        // Have to avoid concrrent modification errors so we're going to have to use more lists

        List<TransportBayPart> oldTransportBays = oldParts.stream()
            .filter(part -> (part instanceof TransportBayPart))
            .map(part -> ((TransportBayPart) part))
            .collect(Collectors.toList());

        oldParts.removeIf(part -> (part instanceof TransportBayPart));

        List<TransportBayPart> newTransportBays = newParts.stream()
            .filter(part -> (part instanceof TransportBayPart))
            .map(part -> ((TransportBayPart) part))
            .collect(Collectors.toList());

        newParts.removeIf(part -> (part instanceof TransportBayPart));

        Iterator<TransportBayPart> oldTBIter = oldTransportBays.iterator();
        while (oldTBIter.hasNext()) {
            TransportBayPart oldTransportBay = oldTBIter.next();
            Bay oldBay = oldTransportBay.getBay();

            boolean matchFound = false;
            Iterator<TransportBayPart> newTBIter = newTransportBays.iterator();
            while (newTBIter.hasNext()) {
                TransportBayPart newTransportBay = newTBIter.next();
                Bay newBay = newTransportBay.getBay();

                if (oldBay.getType().equals(newBay.getType())
                        && (oldBay.getCapacity() == newBay.getCapacity())) {
                    stepsList.add(new RefitStep(oldUnit, oldTransportBay, newTransportBay));

                    List<Part> oldChildren = getChildPartsOfTypes(oldTransportBay, oldParts,
                            Cubicle.class, MissingCubicle.class);
                    List<Part> newChildren = getChildPartsOfTypes(newTransportBay, newParts,
                            Cubicle.class, MissingCubicle.class);

                    Iterator<Part> oldChildIter = oldChildren.iterator();
                    Iterator<Part> NewChildIter = newChildren.iterator();

                    while (oldChildIter.hasNext() && NewChildIter.hasNext()) {
                        Part oldCube = oldChildIter.next();
                        Part newCube = NewChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, oldCube, newCube));
                    }
                    while (oldChildIter.hasNext()) {
                        Part oldCubicle = oldChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, oldCubicle, null));
                    }
                    while (NewChildIter.hasNext()) {
                        Part newCubicle = NewChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, null, newCubicle));
                    }

                    oldChildren = getChildPartsOfTypes(oldTransportBay, oldParts,
                            BayDoor.class, MissingBayDoor.class);
                    newChildren = getChildPartsOfTypes(newTransportBay, newParts,
                            BayDoor.class, MissingBayDoor.class);

                    oldChildIter = oldChildren.iterator();
                    NewChildIter = newChildren.iterator();

                    while (oldChildIter.hasNext() && NewChildIter.hasNext()) {
                        Part oldCube = oldChildIter.next();
                        Part newCube = NewChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, oldCube, newCube));
                    }
                    while (oldChildIter.hasNext()) {
                        Part oldCubicle = oldChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, oldCubicle, null));
                    }
                    while (NewChildIter.hasNext()) {
                        Part newCubicle = NewChildIter.next();
                        stepsList.add(new RefitStep(oldUnit, null, newCubicle));
                    }



                    matchFound = true;
                    break;
                }
            }

            if (matchFound) {
                oldTBIter.remove();
                newTBIter.remove();
            } else {
                oldTBIter.remove();
                stepsList.add(new RefitStep(oldUnit, oldTransportBay, null));

                List<Part> oldChildren = getChildPartsOfTypes(oldTransportBay, oldParts,
                        Cubicle.class, MissingCubicle.class);
                for (Part oldChild : oldChildren) {
                    stepsList.add(new RefitStep(oldUnit, oldChild, null));
                }
                oldChildren = getChildPartsOfTypes(oldTransportBay, oldParts,
                        BayDoor.class, MissingBayDoor.class);
                for (Part oldChild : oldChildren) {
                    stepsList.add(new RefitStep(oldUnit, oldChild, null));
                }
            }
            
        }

        Iterator<TransportBayPart> newTBIter = newTransportBays.iterator();
        while (newTBIter.hasNext()) {
            TransportBayPart newTransportBay = newTBIter.next();

            newTBIter.remove();
            stepsList.add(new RefitStep(oldUnit, null, newTransportBay));

            List<Part> newChildren = getChildPartsOfTypes(newTransportBay, newParts, Cubicle.class, MissingCubicle.class);
            for (Part newChild : newChildren) {
                stepsList.add(new RefitStep(oldUnit, null, newChild));
            }
            newChildren = getChildPartsOfTypes(newTransportBay, newParts, BayDoor.class, MissingBayDoor.class);
            for (Part newChild : newChildren) {
                stepsList.add(new RefitStep(oldUnit, null, newChild));
            }
        }


        // region Everything Else

        oldIterator = oldParts.iterator();
        while (oldIterator.hasNext()) {
            Part oldPart = oldIterator.next();
            
            Part matchPart;
            if (oldPart instanceof MissingPart) {
                matchPart = ((MissingPart) oldPart).getNewPart();
            } else {
                matchPart = oldPart;
            }

            int matchLoc = matchPart.getLocation();
                        
            boolean matchFoundSameLoc = false;
            newIterator = newParts.iterator();
            while (newIterator.hasNext()) {
                Part newPart = newIterator.next();

                if (matchPart.isSamePartType(newPart) && (matchLoc == newPart.getLocation())) {
                    
                    stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                    matchFoundSameLoc = true;
                    break;
                }
            }

            if (matchFoundSameLoc) {
                oldIterator.remove();
                newIterator.remove();
            } else {
                // We haven't found same location, so check for moves
                boolean matchFountDiffLoc = false;
                newIterator = newParts.iterator();
                while (newIterator.hasNext()) {
                    Part newPart = newIterator.next();
    
                    if (matchPart.isSamePartType(newPart)) {
                        
                        stepsList.add(new RefitStep(oldUnit, oldPart, newPart));
                        matchFountDiffLoc = true;
                        break;
                    }
                }
                if (matchFountDiffLoc) {
                    oldIterator.remove();
                    newIterator.remove();
                } else {
                    oldIterator.remove();
                    stepsList.add(new RefitStep(oldUnit, oldPart, null));
                }
            }
        }

        newIterator = newParts.iterator();
        while (newIterator.hasNext()) {
            Part newPart = newIterator.next();
            
            // Dump final selection of new parts into the mix
            newIterator.remove();
            stepsList.add(new RefitStep(oldUnit, null, newPart));
            
        }

        // region Post Processing

        
        // Can't install new equipment in broken locaitons that aren't themselves being replaced.

        for (RefitStep step : stepsList) {
            if (step.getType().isAdditive() && step.getNewLoc() != -1 && brokenLocations[step.getNewLoc()]) {
                step.setType(RefitStepType.ERROR);
                step.setRefitClass(RefitClass.PLEASE_REPAIR);
                step.setNotes(resources.getString("RefitError.BrokenLocation.text"));
            }
        }


        // Let's see if this is possible as an omni refit.

        if(oldUnit.getEntity().isOmni() && newUnit.getEntity().isOmni()) {

            boolean anyFixedChanges = false;
            for (RefitStep step : stepsList) {
                if (!step.isOmniCompatable()) {
                    anyFixedChanges = true;
                    break;
                }
            }
            if (anyFixedChanges) {
                stepsList.add(RefitStep.specialOmniFixedRefit());
            } else {
                for (RefitStep step : stepsList) {
                    step.omniFixup();
                }
                isOmniRefit = true;
            }
        }

        

    }


    // region Analysis Subfunctions

    /**
     * @param unit - the unit to check against
     * @param armor - the armor piece to check
     * @return Is this armor on a location of an Aero unit that shouldn't have armor?
     */
    private boolean isInvalidAeroArmor(Unit unit, Armor armor) {
        if (unit.getEntity() instanceof Warship) {
            if ((armor.getLocation() == Warship.LOC_LBS) || (armor.getLocation() == Warship.LOC_RBS)
                    || (armor.getLocation() == Warship.LOC_HULL)) {
                return true;
            }
        } else if (unit.getEntity() instanceof Jumpship) {
            if (armor.getLocation() == Jumpship.LOC_HULL) {
                return true;
            }
        } else if (unit.getEntity() instanceof SmallCraft) { // Covers Dropships
            if (armor.getLocation() == SmallCraft.LOC_HULL) {
                return true;
            }
        } else if (unit.getEntity() instanceof Aero) {
            if ((armor.getLocation() == Aero.LOC_WINGS) || (armor.getLocation() == Aero.LOC_FUSELAGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the only Part of a given kind in a unit's Part list, remove it (so use a copied list)
     * and return it. If there's none or more than one, complain loudly.
     * @param partType - the .class of the Part type to look for
     * @param missingPartType - the .class of the MissingPart type to look for, if relevant. can null
     * @param searchList - the list to search in. will be mutated!
     * @param unit - unit for error reporting
     * @return the found Part or null
     * @throws IllegalStateException
     */
    @SuppressWarnings("rawtypes")
    private static Part findOnly(Class partType, Class missingPartType, List<Part> searchList, Unit unit) 
            throws IllegalStateException {

        boolean found = false;
        Part toReturn = null;

        Iterator<Part> searchIter = searchList.iterator();
        
        while (searchIter.hasNext()) {
            Part part = searchIter.next();

            if (partType.isInstance(part)) {
                if (found) {
                    String errorString = unit + " has more than one " + partType.getName();
                    if (null != missingPartType) {
                        errorString += " or " + missingPartType.getName();
                    }
                    logger.error(errorString);
                } else {
                    toReturn = part;
                    searchIter.remove();
                    found = true;
                }
            } else if ((null != missingPartType) && (missingPartType.isInstance(part))) {
                if (found) {
                    logger.error(unit + " has more than one " + partType.getName() + " or " + missingPartType.getName());
                } else {
                    toReturn = part;
                    searchIter.remove();
                    found = true;
                }
            }
        }

        return toReturn;
    }

    /**
     * Finds the transport cubicles that are parented to the given transport bay, removes them from
     * the given list (beware) and returns them in a new list.
     * @param parentPart - parent part
     * @param parts - part list to be searched - will be mutated
     * @return list of Cubicles attached to parent part
     */
    @SuppressWarnings("rawtypes")
    private List<Part> getChildPartsOfTypes(Part parentPart, List<Part> parts, Class partClass, Class missingPartClass) {
        List<Part> toReturn = new ArrayList<Part>();

        Iterator<Part> partsIterator = parts.iterator();
        while (partsIterator.hasNext()) {
            Part part = partsIterator.next();

            if (partClass.isInstance(part) || missingPartClass.isInstance(part)) {
                
                if (part.getParentPart() == parentPart) {
                    toReturn.add(part);
                    partsIterator.remove();
                    continue;
                }
            }
        }
        return toReturn;
    }



    /**
     * When this is finished, it wil collapse needed and returned parts into single items of varying
     * quantity, and account for the reuse of parts in add/remove steps if needed.
     */
    public void optimizeShoppingLists() {
        for (RefitStep step : stepsList) {
            if (null != step.getNeededPart()) {
                neededList.add(step.getNeededPart());
            }
            if (null != step.getReturnsPart()) {
                returnsList.add(step.getReturnsPart());
            }
        }
        // Treat armors separately because they use amount instead of quantity -.-
        Map<Integer,Armor> armorNeeded = new HashMap<Integer,Armor>();
        Map<Integer,Armor> armorReturns = new HashMap<Integer,Armor>();

        for (Part part : takeAllOfType(Armor.class, returnsList)) {
            Armor incomingArmor = (Armor) part;
            if (armorReturns.containsKey(incomingArmor.getType())) {
                Armor existingArmor = armorReturns.get(incomingArmor.getType());
                existingArmor.setAmount(existingArmor.getAmount() + incomingArmor.getAmount());
            } else {
                armorReturns.put(incomingArmor.getType(), incomingArmor);
            }
        }

        for (Part part : takeAllOfType(Armor.class, neededList)) {
            Armor incomingArmor = (Armor) part;
            int incomingAmount = incomingArmor.getAmount();
            // Do we have returned armor to use
            if (armorReturns.containsKey(incomingArmor.getType())) {
                Armor returnsArmor = armorReturns.get(incomingArmor.getType());
                int returnsAmount = returnsArmor.getAmount();
                if(incomingAmount == returnsAmount) {
                    armorReturns.remove(returnsArmor.getType());
                    // And drop the incoming armor
                    continue;
                } else if (incomingAmount > returnsAmount) {
                    incomingArmor.setAmount(incomingAmount - returnsAmount);
                    armorReturns.remove(returnsArmor.getType());
                } else {
                    returnsArmor.setAmount(returnsAmount - incomingAmount);
                    // and drop the incoming armor
                    continue;
                }
            }

            if (armorNeeded.containsKey(incomingArmor.getType())) {
                Armor neededArmor = armorNeeded.get(incomingArmor.getType());
                neededArmor.setAmount(neededArmor.getAmount() + incomingArmor.getAmount());
            } else {
                armorNeeded.put(incomingArmor.getType(), incomingArmor);
            }
        }

 

        // Now we have to do the same thing with ammo...

        Map<AmmoType,AmmoStorage> ammoNeeded = new HashMap<AmmoType,AmmoStorage>();
        Map<AmmoType,AmmoStorage> ammoReturns = new HashMap<AmmoType,AmmoStorage>();
    
        for (Part part : takeAllOfType(AmmoStorage.class, returnsList)) {
            AmmoStorage incomingAmmo = (AmmoStorage) part;
            if (ammoReturns.containsKey(incomingAmmo.getType())) {
                AmmoStorage existingAmmo = ammoReturns.get(incomingAmmo.getType());
                existingAmmo.setShots(existingAmmo.getShots() + incomingAmmo.getShots());
            } else {
                ammoReturns.put(incomingAmmo.getType(), incomingAmmo);
            }
        }

        for (Part part : takeAllOfType(AmmoStorage.class, neededList)) {
            AmmoStorage incomingAmmo = (AmmoStorage) part;
            int incomingShots = incomingAmmo.getShots();
            // Do we have returned ammo to use
            if (ammoReturns.containsKey(incomingAmmo.getType())) {
                AmmoStorage returnsAmmo = ammoReturns.get(incomingAmmo.getType());
                int returnsShots = returnsAmmo.getShots();
                if (incomingShots == returnsShots) {
                    ammoReturns.remove(returnsAmmo.getType());
                    // and drop the incoming ammo
                    continue;
                }
                else if (incomingShots > returnsShots) {
                    incomingAmmo.setShots(incomingShots - returnsShots);
                    ammoReturns.remove(returnsAmmo.getType());
                } else {
                    returnsAmmo.setShots(returnsShots - incomingShots);
                    // and drop the incoming ammo
                    continue;
                }
            }

            if (ammoNeeded.containsKey(incomingAmmo.getType())) {
                AmmoStorage neededAmmo = ammoNeeded.get(incomingAmmo.getType());
                neededAmmo.setShots(neededAmmo.getShots() + incomingAmmo.getShots());
            } else {
                ammoNeeded.put(incomingAmmo.getType(), incomingAmmo);
            }
        }


        // Now do this for everything else

        Map<Part,Part> partNeeded = new HashMap<Part,Part>();
        Map<Part,Part> partReturns = new HashMap<Part,Part>();

        for (Part incomingPart : returnsList) {
            if (partReturns.containsKey(incomingPart)) {
                Part existingPart = partReturns.get(incomingPart);
                existingPart.setQuantity(existingPart.getQuantity() + incomingPart.getQuantity());
            } else {
                partReturns.put(incomingPart, incomingPart);
            }
        }

        for (Part incomingPart : neededList) {
            int incomingQuantity = incomingPart.getQuantity();
            
            if (partReturns.containsKey(incomingPart)) {
                Part returnsPart = partReturns.get(incomingPart);
                int returnsQuantity = returnsPart.getQuantity();
                
                if (incomingQuantity == returnsQuantity) {
                    partReturns.remove(returnsPart);
                    // and drop incoming
                    continue;
                } else if (incomingQuantity > returnsQuantity) {
                    incomingPart.setQuantity(incomingQuantity - returnsQuantity);
                    partReturns.remove(returnsPart);
                } else {
                    returnsPart.setQuantity(returnsQuantity - incomingQuantity);
                    // and drop incoming
                    continue;
                }
            }

            if (partNeeded.containsKey(incomingPart)) {
                Part neededPart = partNeeded.get(incomingPart);
                neededPart.setQuantity(neededPart.getQuantity() + incomingPart.getQuantity());
            } else {
                partNeeded.put(incomingPart,incomingPart);
            }
        }

        // Now put the lists back together

        neededList = new ArrayList<Part>();
        returnsList = new ArrayList<Part>();

        neededList.addAll(armorNeeded.values());
        neededList.addAll(ammoNeeded.values());
        neededList.addAll(partNeeded.values());        
        returnsList.addAll(armorReturns.values());
        returnsList.addAll(ammoReturns.values());
        returnsList.addAll(partReturns.values());

    }



    /**
     * Remove all Parts of type type from list and return them in a new list
     */
    @SuppressWarnings("rawtypes")
    public static List<Part> takeAllOfType(Class type, List<Part> list) {
        List<Part> toReturn = new ArrayList<>();
        for(Iterator<Part> partIter = list.iterator(); partIter.hasNext(); ) {
            Part part = partIter.next();
            if (type.isInstance(part)) {
                partIter.remove();
                toReturn.add(part);
            }
        }
        return toReturn;
    }


    /**
     * Determines the refit class as the harest class of all the refit steps
     */
    public void figureRefitClass() {
        RefitClass rc = RefitClass.NO_CHANGE;
        for (RefitStep step : stepsList) {
            rc = rc.keepHardest(step.getRefitClass());
        }
        refitClass = rc;
    }

    /**
     * Determines the refit base time as the sum of all the refit steps' times, and then the full
     * time based on the overall refit class. So figure the refit class first. 
     */
    public void figureRefitTime() {
        unmodifiedTime = 0;
        for (RefitStep step : stepsList) {
            unmodifiedTime += step.getBaseTime();
        }
        time = (int) (unmodifiedTime * getRefitMultiplier());
    }

    /**
     * @return the multipler for our refit class considering if this is a custom job or not
     */
    public double getRefitMultiplier() {
        return refitClass.getTimeMultiplier(!customJob);
    }
    

    // -----------------------------------------  =======================================
    // region ---- -----OLD STUFF

    private void calculateRefurbishment() {
        // Refurbishment rules (class, time, and cost) are found in SO p189.
        // FIXME: WeaverThree - This should be its own code path rather than an appendix to the other
        // refitClass = CLASS_E;

        if (newEntity instanceof Warship || newEntity instanceof SpaceStation) {
            time = WORKMONTH * 3;
        } else if (newEntity instanceof Dropship || newEntity instanceof Jumpship) {
            time = WORKMONTH;
        } else if (newEntity instanceof Mek || newEntity instanceof Aero) { 
            // ConvFighter and SmallCraft are derived from Aero
            time = WORKWEEK * 2; 
        } else if (newEntity instanceof BattleArmor || newEntity instanceof Tank || newEntity instanceof ProtoMek) {
            time = WORKWEEK; 
        } else {
            time = WORKWEEK * 2; // Default to same as Mek
            logger.error("Unit " + newEntity.getModel() + " did not set its time correctly.");
        }

        // The cost is equal to 10 percent of the units base value (not modified for
        // quality). (SO p189)
        cost = oldUnit.getBuyCost().multipliedBy(0.1);
    }

    /**
     * Begins the refit after it's been calculated and configured.
     * @throws EntityLoadingException
     * @throws IOException
     */
    public void begin() throws EntityLoadingException, IOException {
        if (customJob && isSavingFile) {
            saveCustomization();
        }
        oldUnit.setRefit(this);
        // Bay space might change, and either way all cargo needs to be unloaded while
        // the refit is in progress
        oldUnit.unloadTransportShip();
        newEntity.setOwner(oldUnit.getEntity().getOwner());

        // We don't want to require waiting for a refit kit if all that is missing is
        // ammo or ammo bins.
        Map<AmmoType, Integer> shotsNeeded = new HashMap<>();
        for (Part part : newUnitParts) {
            if (part instanceof AmmoBin) {
                AmmoBin bin = (AmmoBin) part;
                bin.setShotsNeeded(bin.getFullShots());
                shotsNeeded.merge(bin.getType(), bin.getShotsNeeded(), Integer::sum);
            }
        }

        for (Iterator<Part> iter = neededList.iterator(); iter.hasNext();) {
            final Part part = iter.next();
            if (part instanceof AmmoBin) {
                part.setRefitUnit(oldUnit);
                part.setUnit(null);
                campaign.getQuartermaster().addPart(part, 0);
                newUnitParts.add(part);
                AmmoBin bin = (AmmoBin) part;
                bin.setShotsNeeded(bin.getFullShots());
                shotsNeeded.merge(bin.getType(), bin.getShotsNeeded(), Integer::sum);
                iter.remove();
            }
        }

        // If we need ammunition, put it into two buckets:
        // 1. Shots from the warehouse
        // 2. Shots to buy
        for (AmmoType atype : shotsNeeded.keySet()) {
            int shotsToBuy = shotsNeeded.get(atype);

            // Try pulling from our stock ...
            int shotsRemoved = campaign.getQuartermaster().removeAmmo(atype, shotsToBuy);
            if (shotsRemoved > 0) {
                shotsToBuy -= shotsRemoved;

                // ... and add that to our list of new unit parts.
                AmmoStorage ammo = new AmmoStorage(0, atype, shotsRemoved, campaign);
                ammo.setRefitUnit(oldUnit);
                campaign.getQuartermaster().addPart(ammo, 0);
                newUnitParts.add(ammo);
            }

            // Add to our shopping list however many shots we need to purchase
            if (shotsToBuy > 0) {
                int tons = (int) Math.ceil((double) shotsToBuy / atype.getShots());
                AmmoStorage ammo = new AmmoStorage(0, atype, tons * atype.getShots(), campaign);
                newUnitParts.add(ammo);
                neededList.add(ammo);
            }
        }

        reserveNewParts();
        if (customJob) {
            // add the stuff on the shopping list to the master shopping list
            ArrayList<Part> newShoppingList = new ArrayList<>();
            for (Part part : neededList) {
                part.setUnit(null);
                if (part instanceof Armor) {
                    /* Taharqa: WE shouldn't be here anymore, given that I am no longer adding
                    ** armor by location to the shopping list but instead changing it all via
                    ** the newArmorSupplies object, but commented out for completeness
                    */
                    // getCampaign().getQuartermaster().addPart(part, 0);
                    // part.setRefitUnit(oldUnit);
                    // newUnitParts.add(part.getId());
                } else if (part instanceof AmmoBin) {
                    // TODO: custom job ammo...

                    // ammo bins are free - bleh
                    AmmoBin ammoBin = (AmmoBin) part;
                    ammoBin.setShotsNeeded(ammoBin.getFullShots());
                    part.setRefitUnit(oldUnit);
                    // WeaverThree - the following function ignores ammo bins... only if they don't have a unit
                    getCampaign().getQuartermaster().addPart(part, 0);
                    newUnitParts.add(part);

                    // Check if we need more ammo
                    if (ammoBin.needsFixing()) {
                        getCampaign().getShoppingList().addShoppingItem(ammoBin.getNewPart(), 1, getCampaign());
                    }

                } else if (part instanceof IAcquisitionWork) {
                    getCampaign().getShoppingList().addShoppingItem(((IAcquisitionWork) part), 1, getCampaign());
                    newShoppingList.add(part);
                }
            }
            neededList = newShoppingList;
            if (null != newArmorSupplies) {
                // add enough armor to the shopping list
                int armorSupplied = 0;
                Armor existingArmorSupplies = getExistingArmorSupplies();
                if (null != existingArmorSupplies) {
                    armorSupplied = existingArmorSupplies.getAmount();
                }

                while (armorSupplied < armorNeeded) {
                    Armor armorPart = (Armor) (newArmorSupplies.getNewPart());
                    armorSupplied += armorPart.getAmount();
                    getCampaign().getShoppingList().addShoppingItem(armorPart, 1, getCampaign());
                }
            }
        } else {
            for (Part part : neededList) {
                part.setUnit(null);
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
            orderArmorSupplies();
            if (neededList.isEmpty() && (null == newArmorSupplies || newArmorSupplies.getAmountNeeded() == 0)) {
                kitFound = true;
            } else {
                getCampaign().getShoppingList().addShoppingItem(this, 1, getCampaign());
            }
        }

        if (isRefurbishing) {
            if (campaign.getQuartermaster().buyRefurbishment(this)) {
                campaign.addReport(ReportingUtilities.messageSurroundedBySpanWithColor(
                        MekHQ.getMHQOptions().getFontColorPositiveHexColor(), "<b>Refurbishment ready to begin</b>"));
            } else {
                campaign.addReport(ReportingUtilities.messageSurroundedBySpanWithColor(
                        MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
                        "You cannot afford to refurbish " + oldUnit.getEntity().getShortName()
                        +  ". Transaction cancelled"));
            }
        }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
    }

    /**
     * Goes through the required parts and marks them as reserved in the warehouse
     */
    public void reserveNewParts() {
        // we need to loop through the new parts and
        // if they are not on a unit already, then we need
        // to set the refit id. Also, if there is more than one part
        // then we need to clone a part and reserve that instead
        List<Part> newNewUnitParts = new ArrayList<>();
        for (Part newPart : newUnitParts) {
            if (newPart.isSpare()) {
                if (newPart.getQuantity() > 1) {
                    newPart.decrementQuantity();
                    newPart = newPart.clone();
                    newPart.setRefitUnit(oldUnit);
                    getCampaign().getQuartermaster().addPart(newPart, 0);
                    newNewUnitParts.add(newPart);
                } else {
                    newPart.setRefitUnit(oldUnit);
                    newNewUnitParts.add(newPart);
                }
            } else {
                newNewUnitParts.add(newPart);
            }
        }
        newUnitParts = newNewUnitParts;
    }

    /**
     * @return if any of our parts are still in transit.
     */
    public boolean partsInTransit() {
        for (Part part : newUnitParts) {
            if (!part.isPresent()) {
                return true;
            }
        }
        return null != newArmorSupplies && !newArmorSupplies.isPresent();
    }

    /**
     * Actually order the parts we need for this refit
     * @return true when the shopping list is empty and there's nothing left to buy
     */
    public boolean acquireParts() {
        if (!customJob) {
            orderArmorSupplies();
            return kitFound && !partsInTransit()
                    && (null == newArmorSupplies || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
        }

        ArrayList<Part> newShoppingList = new ArrayList<>();
        for (Part part : neededList) {
            if (part instanceof AmmoStorage) {
                continue;
            }

            if (part instanceof IAcquisitionWork) {
                // check to see if we found a replacement
                Part replacement = part;
                if (part instanceof MissingPart) {
                    replacement = ((MissingPart) part).findReplacement(true);
                }

                if (null != replacement) {
                    if (replacement.getQuantity() > 1) {
                        Part actualReplacement = replacement.clone();
                        actualReplacement.setRefitUnit(oldUnit);
                        getCampaign().getQuartermaster().addPart(actualReplacement, 0);
                        newUnitParts.add(actualReplacement);
                        replacement.decrementQuantity();
                    } else {
                        replacement.setRefitUnit(oldUnit);
                        newUnitParts.add(replacement);
                    }
                } else {
                    newShoppingList.add(part);
                }
            }
        }

        orderArmorSupplies();
        neededList = newShoppingList;

        // Also, check to make sure that they're not still in transit! - ralgith 2013/07/09
        if (partsInTransit()) {
            return false;
        }

        return neededList.isEmpty()
                && ((null == newArmorSupplies) || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
    }

    /**
     * Orders armor that is required for this refit
     */
    public void orderArmorSupplies() {
        if (null == newArmorSupplies) {
            return;
        }
        Armor existingArmorSupplies = getExistingArmorSupplies();
        int actualNeed = armorNeeded - newArmorSupplies.getAmount();
        if (null != existingArmorSupplies && actualNeed > 0) {
            if (existingArmorSupplies.getAmount() > actualNeed) {
                newArmorSupplies.setAmount(armorNeeded);
                newArmorSupplies.setAmountNeeded(0);
                existingArmorSupplies.setAmount(existingArmorSupplies.getAmount() - actualNeed);

            } else {
                newArmorSupplies.setAmount(newArmorSupplies.getAmount() + existingArmorSupplies.getAmount());
                newArmorSupplies
                        .setAmountNeeded(newArmorSupplies.getAmountNeeded() - existingArmorSupplies.getAmount());
                getCampaign().getWarehouse().removePart(existingArmorSupplies);
            }

            if (newArmorSupplies.getId() <= 0) {
                getCampaign().getQuartermaster().addPart(newArmorSupplies, 0);
            }
        }
    }

    /**
     * @return Armor that's in our warehouse already, if there is any
     */
    public @Nullable Armor getExistingArmorSupplies() {
        if (null == newArmorSupplies) {
            return null;
        }
        return (Armor) getCampaign().getWarehouse().findSparePart(part -> {
            if (part instanceof Armor && ((Armor) part).getType() == newArmorSupplies.getType()
                    && part.isClanTechBase() == newArmorSupplies.isClanTechBase()
                    && !part.isReservedForRefit()
                    && part.isPresent()) {
                return true;
            }
            return false;
        });
    }

    
    /**
     * Aborts this refit and releases the various marked parts to be used for other purposes. 
     * 
     * TODO: WeaverThree - Is it safe to call this on an un-started refit? Looks like no.
     */
    public void cancel() {
        oldUnit.setRefit(null);

        for (Part part : newUnitParts) {
            part.setRefitUnit(null);

            // If the part was not part of the old unit we need to consolidate it with others of its
            // type in the warehouse. Ammo Bins just get unloaded and removed; no reason to keep
            // them around.
            if (part.getUnit() == null) {
                if (part instanceof AmmoBin) {
                    ((AmmoBin) part).unload();
                    getCampaign().getWarehouse().removePart(part);
                } else {
                    getCampaign().getQuartermaster().addPart(part, 0);
                }
            }
        }

        if (null != newArmorSupplies) {
            newArmorSupplies.setRefitUnit(null);
            newArmorSupplies.setUnit(oldUnit);
            getCampaign().getWarehouse().removePart(newArmorSupplies);
            newArmorSupplies.changeAmountAvailable(newArmorSupplies.getAmount());
        }

        // Remove refit parts from the procurement list. Those which have already been purchased and
        // are in transit are left as is.
        List<IAcquisitionWork> toRemove = new ArrayList<>();
        toRemove.add(this);
        if (getRefitUnit() != null) {
            for (IAcquisitionWork part : campaign.getShoppingList().getPartList()) {
                if ((part instanceof Part) && Objects.equals(getRefitUnit(), ((Part) part).getRefitUnit())) {
                    toRemove.add(part);
                }
            }
        }
        for (IAcquisitionWork work : toRemove) {
            campaign.getShoppingList().removeItem(work);
        }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
    }

    /**
     * Actually transform the old unit into the new one, and do all the cleanup that that entails
     */
    private void complete() {
        boolean aclan = false;
        oldUnit.setRefit(null);
        Entity oldEntity = oldUnit.getEntity();
        List<Person> soldiers = new ArrayList<>();
        // unload any soldiers to reload later, because troop size may have changed
        if (oldEntity instanceof Infantry) {
            soldiers = oldUnit.getCrew();
            for (Person soldier : soldiers) {
                oldUnit.remove(soldier, true);
            }
        }
        // add old parts to the warehouse
        for (Part part : oldUnitParts) {
            part.setUnit(null);

            if (part instanceof TransportBayPart) {
                part.removeAllChildParts();
            }

            if (part instanceof MekLocation) {
                int loc = ((MekLocation) part).getLoc();
                // Don't add center locations or limbs with a bad hip or shoulder to warehouse
                if ((loc == Mek.LOC_CT) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER, loc) > 0)) {
                    part.setUnit(null);
                    getCampaign().getWarehouse().removePart(part);
                }

            } else if ((part instanceof StructuralIntegrity) || (part instanceof BattleArmorSuit)
                    || (part instanceof TransportBayPart)
                    || ((part instanceof EquipmentPart)
                            && (((EquipmentPart) part).getType() instanceof InfantryAttack))) {
                // SI Should never be "kept" for the Warehouse
                // We also don't want to generate new BA suits that have been replaced
                // or allow legacy InfantryAttack BA parts to show up in the warehouse.
                getCampaign().getWarehouse().removePart(part);

            } else if (part instanceof Armor) {
                Armor armor = (Armor) part;
                // lets just re-use this armor part
                if (!sameArmorType) {
                    // give the amount back to the warehouse since we are switching types
                    armor.changeAmountAvailable(armor.getAmount());
                    if (null != newArmorSupplies) {
                        armor.changeType(newArmorSupplies.getType(), newArmorSupplies.isClanTechBase());
                    }
                }
                // Removing vehicle turrets or changing BA squad size can reduce the number of
                // armor locations.
                if (part.getLocation() < newEntity.locations()) {
                    newUnitParts.add(part);
                } else {
                    getCampaign().getWarehouse().removePart(part);
                }

            } else if (part instanceof MissingPart) {
                // Don't add missing or destroyed parts to warehouse
                getCampaign().getWarehouse().removePart(part);

            } else {
                if (part instanceof AmmoBin) {
                    ((AmmoBin) part).unload();
                }

                Part spare = getCampaign().getWarehouse().checkForExistingSparePart(part);
                if (spare != null) {
                    spare.incrementQuantity();
                    getCampaign().getWarehouse().removePart(part);
                }
            }
        }

        // Unload any large craft ammo bins to ensure ammo isn't lost
        // when we're changing the amount but not the type of ammo
        for (Part part : largeCraftBinsToChange) {
            if (part instanceof AmmoBin) {
                ((AmmoBin) part).unload();
            }

        }
        // add leftover untracked heat sinks to the warehouse
        for (Part part : oldIntegratedHeatSinks) {
            campaign.getQuartermaster().addPart(part, 0);
        }

        // dont forget to switch entities!
        // ----------------- from here on oldUnit refers to the new entity ------------------------- 
        oldUnit.setEntity(newEntity);
        // Bay capacities might have changed - reset them
        oldUnit.initializeBaySpace();

        // set up new parts
        ArrayList<Part> newParts = new ArrayList<>();
        // We've already made the old suits go *poof*; now we materialize new ones.
        if (newEntity instanceof BattleArmor) {
            for (int t = BattleArmor.LOC_TROOPER_1; t < newEntity.locations(); t++) {
                Part suit = new BattleArmorSuit((BattleArmor) newEntity, t, getCampaign());
                newParts.add(suit);
                suit.setUnit(oldUnit);
            }
        }

        int expectedHeatSinkParts = 0;
        if (newEntity.getClass() == Aero.class) { // Aero but not subclasses
            // Only Aerospace Fighters are expected to have heat sink parts (Meks handled
            // separately) SmallCraft, DropShip, JumpShip, WarShip, and SpaceStation use
            // SpacecraftCoolingSystem instead
            expectedHeatSinkParts = 0;//FIXME://((Aero) newEntity).getHeatSinks() 
                   ///- ((Aero) newEntity).getPodHeatSinks() 
                    //- untrackedHeatSinkCount(newEntity);
        }
        for (Part part : newUnitParts) {
            if ((!replacingLocations) && (part instanceof MekLocation)) {
                // Preserve any hip or shoulder damage
                int loc = ((MekLocation) part).getLoc();
                if ((oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER, loc) > 0)) {
                    // Apply damage to hip or shoulder at slot 0
                    newEntity.getCritical(loc, 0).setDestroyed(true);
                }
            }

            if ((part instanceof HeatSink) && (newEntity instanceof Tank)) {
                // Unit should not have heat sink parts
                // Remove heat sink parts added for supply chain tracking purposes
                getCampaign().getWarehouse().removePart(part);
                continue;

            } else if ((part instanceof AeroHeatSink) && (newEntity instanceof Aero) && !part.isOmniPodded()) {
                if (expectedHeatSinkParts > 0) {
                    expectedHeatSinkParts--;
                } else {
                    // Unit has too many heat sink parts
                    // Remove heat sink parts added for supply chain tracking purposes
                    getCampaign().getWarehouse().removePart(part);
                    continue;
                }

            } else if (part instanceof AmmoStorage) {
                // FIXME: why are we merging this back in?!
                // merge back into the campaign before completing the refit
                AmmoStorage ammoStorage = (AmmoStorage) part;
                getCampaign().getQuartermaster().addAmmo(ammoStorage.getType(), ammoStorage.getShots());
                getCampaign().getWarehouse().removePart(part);
                continue;
            }
            part.setUnit(oldUnit);
            part.setRefitUnit(null);
            newParts.add(part);
            if (part instanceof Armor) {
                // get amounts correct for armor
                part.updateConditionFromEntity(false);
            }
        }
        oldUnit.setParts(newParts);

        // WeaverThree - Watford's tool
        final EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(oldUnit);
        final EquipmentUnscramblerResult result = unscrambler.unscramble();
        if (!result.succeeded()) {
            logger.warn(result.getMessage());
        }

        changeAmmoBinMunitions(oldUnit);

        assignArmActuators();
        assignBayParts();

        if (newEntity instanceof Mek) {
            // Now that Mek part locations have been set
            // Remove heat sink parts added for supply chain tracking purposes
            for (final Iterator<Part> partsIter = oldUnit.getParts().iterator(); partsIter.hasNext();) {
                final Part part = partsIter.next();
                if ((part instanceof HeatSink) && (part.getLocation() == Entity.LOC_NONE)) {
                    getCampaign().getWarehouse().removePart(part);
                    partsIter.remove();
                }
            }
        }

        for (Part part : newParts) {
            // CAW: after a refit some parts ended up NOT having a Campaign attached,
            // see https://github.com/MegaMek/mekhq/issues/2703
            part.setCampaign(getCampaign());

            if (part instanceof AmmoBin) {
                // All large craft ammo got unloaded into the warehouse earlier, though the part IDs
                // have now changed. Consider all LC ammobins empty and load them back up.
                if (part instanceof LargeCraftAmmoBin) {
                    ((AmmoBin) part).setShotsNeeded(((AmmoBin) part).getFullShots());
                }

                ((AmmoBin) part).loadBin();
            }
        }

        if (null != newArmorSupplies) {
            getCampaign().getWarehouse().removePart(newArmorSupplies);
        }
        // in some cases we may have had more armor on the original unit and so we may add more back
        // then we received

        // FIXME: This doesn't deal properly with patchwork armor.
        if (sameArmorType && armorNeeded < 0) {
            Armor armor;
            Entity en = oldUnit.getEntity();
            if (en.isSupportVehicle() && en.getArmorType(en.firstArmorIndex()) == EquipmentType.T_ARMOR_STANDARD) {
                armor = new SVArmor(en.getBARRating(en.firstArmorIndex()), en.getArmorTechRating(),
                        -armorNeeded, Entity.LOC_NONE, getCampaign());
            } else {
                armor = new Armor(0, en.getArmorType(en.firstArmorIndex()),
                        -1 * armorNeeded, -1, false, aclan, getCampaign());
            }
            armor.setUnit(oldUnit);
            armor.changeAmountAvailable(armor.getAmount());
        }

        for (Part part : oldUnit.getParts()) {
            part.updateConditionFromPart();
        }

        oldUnit.getEntity().setC3UUIDAsString(oldEntity.getC3UUIDAsString());
        oldUnit.getEntity().setExternalIdAsString(oldUnit.getId().toString());
        getCampaign().clearGameData(oldUnit.getEntity());
        getCampaign().reloadGameEntities();

        // reload any soldiers
        for (Person soldier : soldiers) {
            if (!oldUnit.canTakeMoreGunners()) {
                break;
            }
            oldUnit.addPilotOrSoldier(soldier);
        }
        oldUnit.resetPilotAndEntity();

        if (isRefurbishing) {
            for (Part part : oldUnit.getParts()) {
                part.improveQuality();
            }
        }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
    }


    /**
     * Deal with ammo bin changing munition type during a refit
     * @param unit - the unit to check
     */
    private void changeAmmoBinMunitions(final Unit unit) {
        for (final Part part : unit.getParts()) {
            if (part instanceof AmmoBin) {
                final AmmoBin ammoBin = (AmmoBin) part;
                final Mounted<?> mounted = unit.getEntity().getEquipment(ammoBin.getEquipmentNum());
                if ((mounted != null) && (mounted.getType() instanceof AmmoType)
                        && !ammoBin.getType().equals(mounted.getType())
                        && ammoBin.canChangeMunitions((AmmoType) mounted.getType())) {
                    // AmmoBin changed munition type during a refit
                    ammoBin.updateConditionFromPart();
                    // Unload bin before munition change
                    ammoBin.unload();
                    ammoBin.changeMunition((AmmoType) mounted.getType());
                }
            }
        }
    }

    /**
     * Writes the configuration for the new side of the refit to a 
     * .mtf or .blk file in the customs directory. 
     * @throws EntityLoadingException
     */
    public void saveCustomization() throws EntityLoadingException {
        UnitUtil.compactCriticals(newEntity);

        String unitName = newEntity.getShortNameRaw();
        String fileName = MHQXMLUtility.escape(unitName);
        String sCustomsDir = String.join(File.separator, "data", "mekfiles", "customs"); // TODO : Remove inline file path
        String sCustomsDirCampaign = sCustomsDir + File.separator + getCampaign().getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                logger.error("Failed to create directory " + sCustomsDir + ", and therefore cannot save the unit.");
                return;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDirCampaign.mkdir()) {
                logger.error(
                        "Failed to create directory " + sCustomsDirCampaign + ", and therefore cannot save the unit.");
                return;
            }
        }

        String fileNameCampaign;
        try {
            String fileExtension = newEntity instanceof Mek ? ".mtf" : ".blk";
            String fileOutName = sCustomsDir + File.separator + fileName + fileExtension;
            fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + fileExtension;
            
            // if this file already exists then don't overwrite it or we might break another unit
            if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                throw new IOException("A file already exists with the custom name " + fileNameCampaign
                        + ". Please choose a different name. (Unit name and/or model)");
            }
            
            if (newEntity instanceof Mek) {
                try (FileOutputStream out = new FileOutputStream(fileNameCampaign);
                        PrintStream p = new PrintStream(out)) {
                    p.println(((Mek) newEntity).getMtf());
                }

            } else {
                BLKFile.encode(fileNameCampaign, newEntity);
            }
        } catch (Exception ex) {
            logger.error("", ex);
            fileNameCampaign = null;
        }

        getCampaign().addCustom(unitName);
        MekSummaryCache.refreshUnitData(false);

        try {
            MekSummary summary = Utilities.retrieveUnit(newEntity.getShortNameRaw());

            newEntity = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            logger.info(String.format("Saved %s to %s", unitName, summary.getSourceFile()));
        } catch (EntityLoadingException ex) {
            logger.error(String.format("Could not read back refit entity %s", unitName), ex);

            if (fileNameCampaign != null) {
                logger.warn("Deleting invalid refit file " + fileNameCampaign);
                try {
                    new File(fileNameCampaign).delete();
                } catch (SecurityException ex2) {
                    logger.warn("Could not clean up bad refit file " + fileNameCampaign, ex2);
                }

                // Reload the mek cache if we had to delete the file
                MekSummaryCache.refreshUnitData(false);
            }

            throw ex;
        }
    }

    /**
     * @return The entity we're refitting FROM
     */
    public Entity getOriginalEntity() {
        return oldUnit.getEntity();
    }

    /**
     * @return The entity that we're refitting TO.
     */
    public Entity getNewEntity() {
        return newEntity;
    }

    /**
     * @return The unit we're refitting FROM
     */
    public Unit getOriginalUnit() {
        return oldUnit;
    }

    /**
     * @return Have we failed a refit check? If so, the quality of the parts will decrease. 
     * unless it's a refurbishment, at least.
     */
    public boolean hasFailedCheck() {
        return failedCheck;
    }

    /**
     * We always need fixing until the refit is done
     */
    @Override
    public boolean needsFixing() {
        return true;
    }

    /**
     * @return The difficulty modifier for our refit class
     */
    @Override
    public int getDifficulty() {
        return getRefitClass().getDifficultyModifier();
        // Refit kit bonus added below in getAllMods
    }

    
    /**
     * @param tech - a Person whose attribute may modify this roll
     * @return a TargetRoll describing all of our difficulty modifiers
     */
    @Override
    public TargetRoll getAllMods(Person tech) {
        TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
        mods.append(oldUnit.getSiteMod());
        if (oldUnit.getEntity().hasQuirk("easy_maintain")) {
            mods.addModifier(-1, "easy to maintain");
        } else if (oldUnit.getEntity().hasQuirk("difficult_maintain")) {
            mods.addModifier(1, "difficult to maintain");
        }

        if (!customJob) {
            mods.addModifier(-2, "refit kit used");
        }

        if ((null != tech) && tech.getOptions().booleanOption(PersonnelOptions.TECH_ENGINEER)) {
            mods.addModifier(-2, "Engineer");
        }
        return mods;
    }

    /**
     * @return string describing our already-decided success
     */
    @Override
    public String succeed() {
        complete();
        if (isRefurbishing) {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorPositiveHexColor(),
                    "Refurbishment of " + oldUnit.getEntity().getShortName() + " <b>is complete</b>.");
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorPositiveHexColor(),
                    "The customization of " + oldUnit.getEntity().getShortName() + " <b>is complete</b>.");
        }
    }

    /**
     * @param rating - ignored
     * @return string describing our already-decided failure
     */
    @Override
    public String fail(int rating) {
        timeSpent = 0;
        failedCheck = true;
        // Refurbishment doesn't get extra time like standard refits.
        if (isRefurbishing) {
            oldUnit.setRefit(null); // Failed roll results in lost time and money
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
                    "Refurbishment of " + oldUnit.getEntity().getShortName() + " <b>was unsuccessful</b>");
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
                    "The customization of " + oldUnit.getEntity().getShortName() 
                    + " will take <b>" + getTimeLeft() + " additional minutes</b> to complete.");
        }
    }

    /**
     * Reset time spent on this refit to zero minutes.
     */
    @Override
    public void resetTimeSpent() {
        timeSpent = 0;
    }

    /**
     * @return Name of the refit item
     */
    @Override
    public String getPartName() {
        if (customJob) {
            return newEntity.getShortName() + " Customization";
        } else if (isRefurbishing) {
            return newEntity.getShortName() + " Refurbishment";
        } else {
            return newEntity.getShortName() + " Refit Kit";
        }
    }

    /**
     * @return Name of the refit item
     */
    @Override
    public String getAcquisitionName() {
        return getPartName();
    }

    /**
     * @return Name of the refit item
     */
    @Override
    public String getName() {
        return getPartName();
    }

    /**
     * Anyone can do a refit, even if it's a bad idea. The skill required doesn't change.
     * @return EXP_GREEN
     */
    @Override
    public int getSkillMin() {
        return SkillType.EXP_GREEN;
    }

    /**
     * A refit has the same base and actual time
     * @return minutes to complete refit 
     */
    @Override
    public int getBaseTime() {
        return time;
    }

    /**
     * A refit has the same base and actual time
     * @return minutes to complete refit
     */
    @Override
    public int getActualTime() {
        return time;
    }

    /**
     * @return how many minutes have already been spent on this refit
     */
    @Override
    public int getTimeSpent() {
        return timeSpent;
    }

    /**
     * @return how many minutes are left to go on this refit
     */
    @Override
    public int getTimeLeft() {
        return time - timeSpent;
    }

    /**
     * @param time - minutes to add to time spent on this refit
     */
    @Override
    public void addTimeSpent(int time) {
        timeSpent += time;
    }

    /**
     * @return tech Person assigned to this refit
     */
    @Override
    public @Nullable Person getTech() {
        return assignedTech;
    }

    /**
     * @param tech - tech Person to assign to this refit
     */
    @Override
    public void setTech(@Nullable Person tech) {
        assignedTech = tech;
    }

    /**
     * We don't do overtime
     * @return false
     */
    @Override
    public boolean hasWorkedOvertime() {
        return false;
    }

    /**
     * We don't do overtime
     * @param b - ignored
     */
    @Override
    public void setWorkedOvertime(boolean b) {

    }

    /**
     * We don't do shorthandedness
     */
    @Override
    public int getShorthandedMod() {
        return 0;
    }

    /**
     * We don't do shorthandedness
     */
    @Override
    public void setShorthandedMod(int i) {

    }

    /**
     * Requiring the life support system to be changed just because the number of bay personnel
     * changes is a bit much. Instead, we'll limit it to changes in crew size, measured by quarters.
     *
     * @return true if the crew quarters capacity changed.
     */
    private boolean crewSizeChanged() {
        int oldCrew = oldUnit.getEntity().getTransportBays()
                .stream().filter(Bay::isQuarters)
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        int newCrew = newEntity.getTransportBays()
                .stream().filter(Bay::isQuarters)
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        return oldCrew != newCrew;
    }

    /**
     * We don't do this
     * @param checkForDestruction - ignored
     */
    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {

    }

    /**
     * We don't do this
     */
    @Override
    public void updateConditionFromPart() {

    }

    /**
     * We don't do this
     */
    @Override
    public void fix() {

    }

    /**
     * We don't do this
     * @param salvage - ignored
     */
    @Override
    public void remove(boolean salvage) {

    }

    /**
     * There is no missing part version of a refit
     * @return null
     */
    @Override
    public @Nullable MissingPart getMissingPart() {
        // not applicable
        return null;
    }

    /**
     * This should never come up for us
     * @return a description string
     */
    @Override
    public String getDesc() {
        return newEntity.getModel() + " " + getDetails();
    }

    /**
     * Get the details for this item. Always includes repair details that don't exist here.
     * @return Full details string
     */
    @Override
    public String getDetails() {
        return getDetails(true);
    }

    /**
     * Get the details for this item. Repair details optional but don't exist here.
     * @param includeRepairDetails - ignored
     * @return Full details string
     */
    @Override
    public String getDetails(boolean includeRepairDetails) {
        return "(" + getRefitClassName() + "/" + getTimeLeft() + " minutes/" + getCost().toAmountAndSymbolString()
                + ")";
    }

    /**
     * @return Always the unit we're refitting FROM
     */
    @Override
    public Unit getUnit() {
        return oldUnit;
    }

    /**
     * We don't do salvage
     * @return false
     */
    @Override
    public boolean isSalvaging() {
        return false;
    }

    /**
     * Is there anything blocking the refit?
     * @return String detailing blockers
     */
    @Override
    public @Nullable String checkFixable() {
        return errorStrings.length() == 0 ? null : errorStrings.toString();
    }

    /**
     * Dumps this object into XML for the save file
     * @param pw - output writer
     * @param indent - current indent level in the XML
     */
    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "refit");
        pw.println(MHQXMLUtility.writeEntityToXmlString(newEntity, indent, getCampaign().getEntities()));
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "time", time);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeSpent", timeSpent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "refitClass", refitClass.toNumeric());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cost", cost);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "failedCheck", failedCheck);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "customJob", customJob);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "kitFound", kitFound);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isRefurbishing", isRefurbishing);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "armorNeeded", armorNeeded);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sameArmorType", sameArmorType);
        if (assignedTech != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignedTechId", assignedTech.getId());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quantity", quantity);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToWait", daysToWait);
        if (!oldUnitParts.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "oldUnitParts");
            for (final Part part : oldUnitParts) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pid", part.getId());
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "oldUnitParts");
        }

        if (!newUnitParts.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "newUnitParts");
            for (final Part part : newUnitParts) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pid", part.getId());
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "newUnitParts");
        }

        if (!largeCraftBinsToChange.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "lcBinsToChange");
            for (Part part : largeCraftBinsToChange) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pid", part.getId());
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "lcBinsToChange");
        }

        if (!neededList.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "shoppingList");
            for (final Part part : neededList) {
                part.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "shoppingList");
        }

        if (newArmorSupplies != null) {
            if (newArmorSupplies.getId() <= 0) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "newArmorSupplies");
                newArmorSupplies.writeToXML(pw, indent);
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "newArmorSupplies");
            } else {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "newArmorSuppliesId", newArmorSupplies.getId());
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "refit");
    }

    /**
     * Recreates a refit from the save data
     * @param wn - our XML node
     * @param version - save file version ?
     * @param campaign - campaign we're loading into
     * @param unit - unit this refit is attached to
     * @return a brand-new Refit
     */
    public static @Nullable Refit generateInstanceFromXML(final Node wn, final Version version,
            final Campaign campaign, final Unit unit) {
        Refit retVal = new Refit();
        retVal.oldUnit = Objects.requireNonNull(unit);

        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("time")) {
                    retVal.time = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("refitClass")) {
                    retVal.refitClass = RefitClass.fromNumeric(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
                    retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                    retVal.cost = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("newArmorSuppliesId")) {
                    retVal.newArmorSupplies = new RefitArmorRef(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("assignedTechId")) {
                    retVal.assignedTech = new RefitPersonRef(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("failedCheck")) {
                    retVal.failedCheck = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("customJob")) {
                    retVal.customJob = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("kitFound")) {
                    retVal.kitFound = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("isRefurbishing")) {
                    retVal.isRefurbishing = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("armorNeeded")) {
                    retVal.armorNeeded = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("sameArmorType")) {
                    retVal.sameArmorType = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.newEntity = Objects
                            .requireNonNull(MHQXMLUtility.parseSingleEntityMul((Element) wn2, campaign));
                } else if (wn2.getNodeName().equalsIgnoreCase("oldUnitParts")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeName().equalsIgnoreCase("pid")) {
                            retVal.oldUnitParts.add(new RefitPartRef(Integer.parseInt(wn3.getTextContent())));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("newUnitParts")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeName().equalsIgnoreCase("pid")) {
                            retVal.newUnitParts.add(new RefitPartRef(Integer.parseInt(wn3.getTextContent())));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("lcBinsToChange")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeName().equalsIgnoreCase("pid")) {
                            retVal.largeCraftBinsToChange.add(new RefitPartRef(Integer.parseInt(wn3.getTextContent())));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("shoppingList")) {
                    processShoppingListFromXML(retVal, wn2, retVal.oldUnit, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("newArmorSupplies")) {
                    processArmorSuppliesFromXML(retVal, wn2, version);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }

        return retVal;
    }

    private static void processShoppingListFromXML(Refit retVal, Node wn, Unit u, Version version) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                logger.error("Unknown node type not loaded in Part nodes: " + wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                p.setUnit(u);
                retVal.neededList.add(p);
            } else {
                logger.error((u != null)
                        ? String.format("Unit %s has invalid parts in its refit shopping list", u.getId())
                        : "Invalid parts in shopping list");
            }
        }
    }

    private static void processArmorSuppliesFromXML(Refit retVal, Node wn, Version version) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                logger.error("Unknown node type not loaded in Part nodes: " + wn2.getNodeName());

                continue;
            }
            Part p = Part.generateInstanceFromXML(wn2, version);

            if (p instanceof Armor) {
                retVal.newArmorSupplies = (Armor) p;
                break;
            }
        }
    }

    /**
     * Used after loading from a save.
     */
    public void reCalc() {
        setCampaign(oldUnit.getCampaign());
        for (Part p : neededList) {
            p.setCampaign(oldUnit.getCampaign());
        }

        if (null != newArmorSupplies) {
            newArmorSupplies.setCampaign(oldUnit.getCampaign());
        }
    }

    /**
     * We are the new equipment
     */
    @Override
    public Part getNewEquipment() {
        return this;
    }

    /**
     * We don't use this
     */
    @Override
    public String getAcquisitionDesc() {
        return "This should never be seen (Refit.java)";
    }

    /**
     * We don't use this
     */
    @Override
    public String getAcquisitionDisplayName() {
        return null;
    }

    /**
     * We don't use this
     */
    @Override
    public String getAcquisitionExtraDesc() {
        return null;
    }

    /**
     * We don't use this
     */
    @Override
    public String getAcquisitionBonus() {
        return null;
    }

    /**
     * We don't use this
     */
    @Override
    public Part getAcquisitionPart() {
        return null;
    }

    /**
     * @return Cost of carrying otu the refit (mostly relevant for refurbishment)
     */
    @Override
    public Money getStickerPrice() {
        return cost;
    }

    /**
     * @return Cost of carrying otu the refit (mostly relevant for refurbishment)
     */
    @Override
    public Money getActualValue() {
        // This is a case that the price should already be adjusted for campaign options
        return getStickerPrice();
    }

    /**
     * @return Cost of carrying otu the refit (mostly relevant for refurbishment)
     */
    @Override
    public Money getBuyCost() {
        return getActualValue();
    }

    /**
     * Fixes up some special parts and clears the shopping list when we've found the refit kit
     * @param transitDays - How long will it take to acquire kit
     */
    public void addRefitKitParts(int transitDays) {
        for (Part part : neededList) {
            if (part instanceof AmmoBin) {
                part.setRefitUnit(oldUnit);
                getCampaign().getQuartermaster().addPart(part, 0);
                newUnitParts.add(part);
                AmmoBin bin = (AmmoBin) part;
                bin.setShotsNeeded(bin.getFullShots());
                bin.loadBin();
                if (bin.needsFixing()) {
                    getCampaign().getQuartermaster().addPart(bin.getNewPart(), transitDays);
                    bin.loadBin();
                }

            } else if (part instanceof MissingPart) {
                Part newPart = (Part) ((IAcquisitionWork) part).getNewEquipment();
                newPart.setRefitUnit(oldUnit);
                getCampaign().getQuartermaster().addPart(newPart, transitDays);
                newUnitParts.add(newPart);

            } else if (part instanceof AmmoStorage) {
                part.setUnit(null);
                part.setRefitUnit(oldUnit);
                campaign.getQuartermaster().addPart(part, transitDays);
            }
        }
        if (null != newArmorSupplies) {
            int amount = armorNeeded - newArmorSupplies.getAmount();
            if (amount > 0) {
                Armor armor = (Armor) newArmorSupplies.getNewPart();
                armor.setAmount(amount);
                getCampaign().getQuartermaster().addPart(armor, transitDays);
            }
            orderArmorSupplies();
        }
        neededList = new ArrayList<>();
        kitFound = true;
    }

    /**
     * You found the refit kit, now attempt to purchase it
     * @param transitDays - how long it's going to take to get here
     * @return string for report explaining how it went
     */
    @Override
    public String find(int transitDays) {
        if (campaign.getQuartermaster().buyPart(this, transitDays)) {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorPositiveHexColor(),
                    "<b> refit kit found.</b> Kit will arrive in " + transitDays + " days.");
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
                    "<b> You cannot afford this refit kit. Transaction cancelled</b>.");
        }
    }

    /**
     * You failed to find the kit
     * @return string describing this
     */
    @Override
    public String failToFind() {
        return ReportingUtilities.messageSurroundedBySpanWithColor(
            MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
            " refit kit not found.");
    }

    /**
     * Get acquistion difficulty based on hardest to find part in the kit
     * @return TargetRoll describing modifiers to roll
     */
    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll roll = new TargetRoll();
        int avail = EquipmentType.RATING_A;
        int techBaseMod = 0;
        for (Part part : neededList) {
            if (getTechBase() == T_CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > techBaseMod) {
                techBaseMod = campaign.getCampaignOptions().getClanAcquisitionPenalty();
            } else if (getTechBase() == T_IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > techBaseMod) {
                techBaseMod = campaign.getCampaignOptions().getIsAcquisitionPenalty();
            } else if (getTechBase() == T_BOTH) {
                int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(),
                        campaign.getCampaignOptions().getIsAcquisitionPenalty());
                if (penalty > techBaseMod) {
                    techBaseMod = penalty;
                }
            }
            avail = Math.max(avail, part.getAvailability());
        }
        if (techBaseMod > 0) {
            roll.addModifier(techBaseMod, "tech limit");
        }
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        roll.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
        return roll;
    }

    public @Nullable Armor getNewArmorSupplies() {
        return newArmorSupplies;
    }

    /**
     * We don't do this
     */
    @Override
    public void resetOvertime() {
    }

    /**
     * We don't do this
     */
    @Override
    public int getTechLevel() {
        return 0;
    }

    /**
     * Tech base is basically irrelevant for a refit kit
     */
    @Override
    public int getTechBase() {
        return Part.T_BOTH;
    }

    /**
     * We don't care what tech type it is, at least not here
     */
    @Override
    public boolean isRightTechType(String skillType) {
        return true;
    }


    /**
     * Suggest a new name for the unit being refit. Only works for infantry.
     */
    public void suggestNewName() {
        if (newEntity.isConventionalInfantry()) {
            Infantry infantry = (Infantry) newEntity;
            String chassis;
            switch (infantry.getMovementMode()) {
                case INF_UMU:
                    chassis = "Scuba ";
                    break;
                case INF_MOTORIZED:
                    chassis = "Motorized ";
                    break;
                case INF_JUMP:
                    chassis = "Jump ";
                    break;
                case HOVER:
                    chassis = "Mechanized Hover ";
                    break;
                case WHEELED:
                    chassis = "Mechanized Wheeled ";
                    break;
                case TRACKED:
                    chassis = "Mechanized Tracked ";
                    break;
                default:
                    chassis = "Foot ";
                    break;
            }

            if (infantry.isSquad()) {
                chassis += "Squad";
            } else {
                chassis += "Platoon";
            }
            newEntity.setChassis(chassis);
            String model = "?";
            if (infantry.getSecondaryWeaponsPerSquad() > 1 && null != infantry.getSecondaryWeapon()) {
                model = "(" + infantry.getSecondaryWeapon().getInternalName() + ")";
            } else if (null != infantry.getPrimaryWeapon()) {
                model = "(" + infantry.getPrimaryWeapon().getInternalName() + ")";
            }
            newEntity.setModel(model);
        }
    }

    /**
     * Moves the actuator Parts over from the old unit to the new unit, for BipedMeks
     */
    private void assignArmActuators() {
        if (!(oldUnit.getEntity() instanceof BipedMek)) {
            return;
        }
        BipedMek m = (BipedMek) oldUnit.getEntity();
        // we only need to worry about lower arm actuators and hands
        Part rightLowerArm = null;
        Part leftLowerArm = null;
        Part rightHand = null;
        Part leftHand = null;
        MekActuator missingHand1 = null;
        MekActuator missingHand2 = null;
        MekActuator missingArm1 = null;
        MekActuator missingArm2 = null;
        for (Part part : oldUnit.getParts()) {
            if (part instanceof MekActuator || part instanceof MissingMekActuator) {
                int type;
                if (part instanceof MekActuator) {
                    type = ((MekActuator) part).getType();
                } else {
                    type = ((MissingMekActuator) part).getType();
                }
                int loc = part.getLocation();

                if (type == Mek.ACTUATOR_LOWER_ARM) {
                    if (loc == Mek.LOC_RARM) {
                        rightLowerArm = part;
                    } else if (loc == Mek.LOC_LARM) {
                        leftLowerArm = part;
                    } else if (null == missingArm1 && part instanceof MekActuator) {
                        missingArm1 = (MekActuator) part;
                    } else if (part instanceof MekActuator) {
                        missingArm2 = (MekActuator) part;
                    }
                } else if (type == Mek.ACTUATOR_HAND) {
                    if (loc == Mek.LOC_RARM) {
                        rightHand = part;
                    } else if (loc == Mek.LOC_LARM) {
                        leftHand = part;
                    } else if (null == missingHand1 && part instanceof MekActuator) {
                        missingHand1 = (MekActuator) part;
                    } else if (part instanceof MekActuator) {
                        missingHand2 = (MekActuator) part;
                    }
                }
            }
        }

        // ok now check all the conditions, assign right hand stuff first
        if (null == rightHand && m.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)) {
            MekActuator part = missingHand1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingHand2;
            }
            if (null != part) {
                part.setLocation(Mek.LOC_RARM);
            }
        }

        if (null == leftHand && m.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
            MekActuator part = missingHand1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingHand2;
            }

            if (null != part) {
                part.setLocation(Mek.LOC_LARM);
            }
        }

        if (null == rightLowerArm && m.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)) {
            MekActuator part = missingArm1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }

            if (null != part) {
                part.setLocation(Mek.LOC_RARM);
            }
        }
        if (null == leftLowerArm && m.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
            MekActuator part = missingArm1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }

            if (null != part) {
                part.setLocation(Mek.LOC_LARM);
            }
        }
    }

    /**
     * Assigns bay doors and cubicles as child parts of the bay part. We also need to make sure the
     * bay number of the parts match up to the Entity. The easiest way to do that is to remove all
     * the bay parts and create new ones from scratch. Then we assign doors and cubicles.
     */
    private void assignBayParts() {
        final Entity entity = oldUnit.getEntity();

        List<Part> doors = new ArrayList<>();
        Map<BayType, List<Part>> cubicles = new HashMap<>();
        List<Part> oldBays = new ArrayList<>();
        for (Part part : oldUnit.getParts()) {
            if (part instanceof BayDoor) {
                doors.add(part);

            } else if (part instanceof Cubicle) {
                cubicles.putIfAbsent(((Cubicle) part).getBayType(), new ArrayList<>());
                cubicles.get(((Cubicle) part).getBayType()).add(part);

            } else if (part instanceof TransportBayPart) {
                oldBays.add(part);
            }
        }
        oldBays.forEach(part -> part.remove(false));
        for (Bay bay : entity.getTransportBays()) {
            if (bay.isQuarters()) {
                continue;
            }
            BayType btype = BayType.getTypeForBay(bay);
            Part bayPart = new TransportBayPart((int) oldUnit.getEntity().getWeight(),
                    bay.getBayNumber(), bay.getCapacity(), campaign);
            oldUnit.addPart(bayPart);
            getCampaign().getQuartermaster().addPart(bayPart, 0);
            for (int i = 0; i < bay.getDoors(); i++) {
                Part door;
                if (!doors.isEmpty()) {
                    door = doors.remove(0);
                } else {
                    // This shouldn't ever happen
                    door = new MissingBayDoor((int) entity.getWeight(), campaign);
                    oldUnit.addPart(door);
                    getCampaign().getQuartermaster().addPart(door, 0);
                }
                bayPart.addChildPart(door);
            }
            if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                for (int i = 0; i < bay.getCapacity(); i++) {
                    Part cubicle;
                    if (cubicles.containsKey(btype) && !cubicles.get(btype).isEmpty()) {
                        cubicle = cubicles.get(btype).remove(0);
                    } else {
                        cubicle = new MissingCubicle((int) entity.getWeight(), btype, campaign);
                        oldUnit.addPart(cubicle);
                        getCampaign().getQuartermaster().addPart(cubicle, 0);
                    }
                    bayPart.addChildPart(cubicle);
                }
            }
        }
    }



    /**
     * @param quantity - ignored
     * @return string for report about listing this refit
     */
    @Override
    public String getShoppingListReport(int quantity) {
        return getAcquisitionName() + " has been added to the procurement list.";
    }

    /**
     * Refits don't have a weight
     * @return 0
     */
    @Override
    public double getTonnage() {
        return 0;
    }

    /**
     * We don't have a tech rating
     * @return 0
     */
    @Override
    public int getTechRating() {
        return 0;
    }

    /**
     * We don't compare refits
     * @return false
     */
    @Override
    public boolean isSamePartType(Part part) {
        return false;
    }

    /**
     * We handle this in generateInstanceFromXML
     * @param wn - ignored
     */
    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
    }

    /**
     * You can't clone a refit
     * @return null
     */
    @Override
    public Part clone() {
        return null;
    }

    /**
     * Gets a value indicating whether or not this refit is a custom job. 
     * If false, this is a Refit Kit (CamOps 212).\
     * @return is custom job?
     */
    public boolean isCustomJob() {
        return customJob;
    }

    /**
     * @return Has a refit kit for this refit been found (if applicable). 
     */
    public boolean kitFound() {
        return kitFound;
    }

    /**
     * Refits have no location
     * @return null
     */
    @Override
    public String getLocationName() {
        return null;
    }

    /**
     * Refits have no location
     * @return LOC_NONE
     */
    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    /**
     * Tech Advancment doesn't matter for refit kits
     * @return TA_GENERIC
     */
    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }

    /**
     * @return are we doing a refurbishment?
     */
    public boolean isBeingRefurbished() {
        return isRefurbishing;
    }

    /**
     * Refits are a meta item that has no introduction
     * @return should probably always be true
     */
    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    /**
     * Refits are a meta item that never goes extinct
     * @return should probably always be false
     */
    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }

    /**
     * Make sure all of our units' parts are properly accounted for
     * @param campaign - campaign that owns the parts
     */
    @Override
    public void fixReferences(Campaign campaign) {
        super.fixReferences(campaign);

        setCampaign(campaign);

        if (newArmorSupplies instanceof RefitArmorRef) {
            Part realPart = campaign.getWarehouse().getPart(newArmorSupplies.getId());
            if (realPart instanceof Armor) {
                newArmorSupplies = (Armor) realPart;
            } else {
                logger.error(String.format("Refit on Unit %s references missing armor supplies %d",
                        getUnit().getId(), newArmorSupplies.getId()));
                newArmorSupplies = null;
            }
        }

        for (int oldPartIndex = oldUnitParts.size() - 1; oldPartIndex >= 0; --oldPartIndex) {
            Part part = oldUnitParts.get(oldPartIndex);
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                if (realPart != null) {
                    oldUnitParts.set(oldPartIndex, realPart);

                } else if (part.getId() > 0) {
                    logger.error(String.format("Refit on Unit %s references missing old unit part %d",
                            getUnit().getId(), part.getId()));
                    oldUnitParts.remove(oldPartIndex);

                } else {
                    logger.error(String.format("Refit on Unit %s references unknown old unit part with an id of 0",
                            getUnit().getId()));
                    oldUnitParts.remove(oldPartIndex);
                }
            }
        }

        for (int newPartIndex = newUnitParts.size() - 1; newPartIndex >= 0; --newPartIndex) {
            Part part = newUnitParts.get(newPartIndex);
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                if (realPart != null) {
                    newUnitParts.set(newPartIndex, realPart);

                } else if (part.getId() > 0) {
                    logger.error(String.format("Refit on Unit %s references missing new unit part %d",
                            getUnit().getId(), part.getId()));
                    newUnitParts.remove(newPartIndex);

                } else {
                    logger.error(String.format("Refit on Unit %s references unknown new unit part with an id of 0",
                            getUnit().getId()));
                    newUnitParts.remove(newPartIndex);
                }
            }
        }

        List<Part> realParts = new ArrayList<>();
        Iterator<Part> lcBinIt = largeCraftBinsToChange.iterator();
        while (lcBinIt.hasNext()) {
            Part part = lcBinIt.next();
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                lcBinIt.remove();
                if (realPart != null) {
                    realParts.add(realPart);
                } else {
                    logger.error(String.format("Refit on Unit %s references missing large craft ammo bin %d",
                            getUnit().getId(), part.getId()));
                }
            }
        }

        largeCraftBinsToChange.addAll(realParts);

        if (assignedTech instanceof RefitPersonRef) {
            UUID id = assignedTech.getId();
            assignedTech = campaign.getPerson(id);
            if (assignedTech == null) {
                logger.error(String.format("Refit on Unit %s references missing tech %s", getUnit().getId(), id));
            }
        }
    }

    /**
     * Proxy Armor that references a certain ID
     */
    public static class RefitArmorRef extends Armor {
        private RefitArmorRef(int id) {
            this.id = id;
        }
    }

    /**
     * Proxy Part that references a certain ID. All of its mandatory overrides are stubs.
     */
    public static class RefitPartRef extends Part {
        private RefitPartRef(int id) {
            this.id = id;
        }

        @Override
        public int getBaseTime() {
            return 0;
        }

        @Override
        public void updateConditionFromEntity(boolean checkForDestruction) {
        }

        @Override
        public void updateConditionFromPart() {
        }

        @Override
        public void remove(boolean salvage) {
        }

        @Override
        public MissingPart getMissingPart() {
            return null;
        }

        @Override
        public int getLocation() {
            return 0;
        }

        @Override
        public @Nullable String checkFixable() {
            return null;
        }

        @Override
        public boolean needsFixing() {
            return false;
        }

        @Override
        public int getDifficulty() {
            return 0;
        }

        @Override
        public Money getStickerPrice() {
            return null;
        }

        @Override
        public double getTonnage() {
            return 0;
        }

        @Override
        public boolean isSamePartType(Part part) {
            return false;
        }

        @Override
        public void writeToXML(final PrintWriter pw, int indent) {

        }

        @Override
        protected void loadFieldsFromXmlNode(Node wn) {

        }

        @Override
        public Part clone() {
            return null;
        }

        @Override
        public String getLocationName() {
            return null;
        }

        @Override
        public ITechnology getTechAdvancement() {
            return null;
        }
    }

    /**
     * Proxy Person that references a specific ID
     */
    public static class RefitPersonRef extends Person {
        private RefitPersonRef(UUID id) {
            super(id);
        }
    }
}
