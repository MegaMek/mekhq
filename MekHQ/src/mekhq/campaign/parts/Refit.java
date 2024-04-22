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

import megamek.Version;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscMounted;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestTank;
import megamek.common.weapons.InfantryAttack;
import megameklab.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitRefitEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.cleanup.EquipmentUnscrambler;
import mekhq.campaign.unit.cleanup.EquipmentUnscramblerResult;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This object tracks the refit of a given unit into a new unit.
 * It has fields for the current entity and the new entity and it
 * uses these to calculate various characteristics of the refit.
 *
 * It can then also be used to track the actual refit process, by
 * attaching it to a Unit.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Refit extends Part implements IAcquisitionWork {
    public static final int NO_CHANGE = 0;
    public static final int CLASS_OMNI = 1;
    public static final int CLASS_A = 2;
    public static final int CLASS_B = 3;
    public static final int CLASS_C = 4;
    public static final int CLASS_D = 5;
    public static final int CLASS_E = 6;
    public static final int CLASS_F = 7;

    private Unit oldUnit;
    private Entity newEntity;

    private int refitClass;
    private int time;
    private int timeSpent;
    private Money cost;
    private boolean failedCheck;
    private boolean customJob;
    private boolean isRefurbishing;
    private boolean kitFound;
    private boolean replacingLocations;
    private String fixableString;

    private List<Part> oldUnitParts;
    private List<Part> newUnitParts;
    private List<Part> shoppingList;
    private List<Part> oldIntegratedHS;
    private List<Part> newIntegratedHS;
    private Set<Part> lcBinsToChange;

    private int armorNeeded;
    private Armor newArmorSupplies;
    private boolean sameArmorType;

    private int oldLargeCraftHeatSinks;
    private int oldLargeCraftSinkType;
    private int newLargeCraftHeatSinks;

    private Person assignedTech;

    public Refit() {
        oldUnitParts = new ArrayList<>();
        newUnitParts = new ArrayList<>();
        shoppingList = new ArrayList<>();
        oldIntegratedHS = new ArrayList<>();
        newIntegratedHS = new ArrayList<>();
        lcBinsToChange = new HashSet<>();
        fixableString = null;
        cost = Money.zero();
    }

    public Refit(Unit oUnit, Entity newEn, boolean custom, boolean refurbish) {
        this();
        isRefurbishing = refurbish;
        customJob = custom;
        oldUnit = oUnit;
        newEntity = newEn;
        newEntity.setOwner(oldUnit.getEntity().getOwner());
        newEntity.setGame(oldUnit.getEntity().getGame());
        if (newEntity.getClass() == SmallCraft.class) { // SmallCraft but not subclasses
            // Entity.setGame() will add a Single Hex ECM part to SmallCraft that otherwise has no ECM
            // This is required for MegaMek, but causes SmallCraft to be overweight when refitting in MekHQ
            // Work-around is to remove the ECM part early during a refit
            // Ref: https://github.com/MegaMek/mekhq/issues/1970
            newEntity.removeMisc(BattleArmor.SINGLE_HEX_ECM);
        }
        failedCheck = false;
        timeSpent = 0;
        fixableString = null;
        kitFound = false;
        replacingLocations = false;
        campaign = oldUnit.getCampaign();
        calculate();
        if (customJob) {
            suggestNewName();
        }
    }

    @Override
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Gets a value indicating whether or not the armor type
     * is the same for the refit.
     */
    public boolean isSameArmorType() {
        return sameArmorType;
    }

    public static String getRefitClassName(int refitClass) {
        switch (refitClass) {
            case NO_CHANGE:
                return "No Change";
            case CLASS_A:
                return "Class A (Field)";
            case CLASS_B:
                return "Class B (Field)";
            case CLASS_C:
                return "Class C (Maintenance)";
            case CLASS_D:
                return "Class D (Maintenance)";
            case CLASS_E:
                return "Class E (Factory)";
            case CLASS_F:
                return "Class F (Factory)";
            case CLASS_OMNI:
                return "Omni Repod";
            default:
                return "Unknown";
        }
    }

    public String getRefitClassName() {
        return getRefitClassName(refitClass);
    }

    public int getRefitClass() {
        return refitClass;
    }

    public Money getCost() {
        return cost;
    }

    /**
     * Returns a mutable list of parts for the old unit in the refit.
     * This is intended to be mutated only be {@link mekhq.campaign.Campaign Campaign} when merging
     * parts.
     * @return A mutable {@link List} of old parts in the refit.
     */
    public List<Part> getOldUnitParts() {
        return oldUnitParts;
    }

    /**
     * Returns a mutable list of parts for the new unit in the refit.
     * This is intended to be mutated only be {@link mekhq.campaign.Campaign Campaign} when merging
     * parts.
     * @return A mutable {@link List} of new part IDs in the refit.
     */
    public List<Part> getNewUnitParts() {
        return newUnitParts;
    }

    public List<Part> getShoppingList() {
        return shoppingList;
    }

    public String[] getShoppingListDescription() {
        Hashtable<String, Integer> tally = new Hashtable<>();
        Hashtable<String, String> desc = new Hashtable<>();
        for (Part p : shoppingList) {
            if (p instanceof Armor) {
                continue;
            }
            if (null != tally.get(p.getName())) {
                tally.put(p.getName(), tally.get(p.getName()) + 1);
                desc.put(p.getName(), p.getQuantityName(tally.get(p.getName())));
            } else {
                tally.put(p.getName(), 1);
                desc.put(p.getName(), p.getQuantityName(1));
            }
        }
        if (null != newArmorSupplies) {
            int actualAmountNeeded = armorNeeded;
            Armor existingSupplies = getExistingArmorSupplies();
            if (null != existingSupplies) {
                actualAmountNeeded -= existingSupplies.getAmount();
            }
            if (actualAmountNeeded > 0) {
                Armor a = (Armor) newArmorSupplies.getNewPart();
                a.setAmount(actualAmountNeeded);
                desc.put(a.getName(), a.getQuantityName(1));
            }
        }
        String[] descs = new String[desc.keySet().size()];
        int i = 0;
        for (String name : desc.keySet()) {
            descs[i] = desc.get(name);
            i++;
        }
        return descs;
    }

    public int getTime() {
        return time;
    }

    public void calculate() {
        Unit newUnit = new Unit(newEntity, getCampaign());
        newUnit.initializeParts(false);
        refitClass = NO_CHANGE;
        boolean isOmniRefit = oldUnit.getEntity().isOmni() && newEntity.isOmni();
        if (isOmniRefit && !Utilities.isOmniVariant(oldUnit.getEntity(), newEntity)) {
            fixableString = "A unit loses omni capabilities if any fixed equipment is modified.";
            return;
        }
        time = 0;
        sameArmorType = newEntity.getArmorType(newEntity.firstArmorIndex())
                == oldUnit.getEntity().getArmorType(oldUnit.getEntity().firstArmorIndex());
        // SVs with standard SV armor need to check for change in BAR/tech rating
        if (newEntity.isSupportVehicle()
                && (newEntity.getArmorType(newEntity.firstArmorIndex()) == EquipmentType.T_ARMOR_STANDARD)) {
            sameArmorType = newEntity.getBARRating(newEntity.firstArmorIndex())
                        == oldUnit.getEntity().getArmorType(oldUnit.getEntity().firstArmorIndex())
                    && (newEntity.getArmorTechRating() == oldUnit.getEntity().getArmorTechRating());
        }
        int recycledArmorPoints = 0;
        boolean[] locationHasNewStuff = new boolean[Math.max(newEntity.locations(), oldUnit.getEntity().locations())];
        boolean[] locationLostOldStuff = new boolean[Math.max(newEntity.locations(), oldUnit.getEntity().locations())];
        HashMap<AmmoType, Integer> ammoNeeded = new HashMap<>();
        HashMap<AmmoType, Integer> ammoRemoved = new HashMap<>();
        ArrayList<Part> newPartList = new ArrayList<>();

        // Step 1: put all of the parts from the current unit into a new arraylist so they can
        // be removed when we find a match.
        for (Part p : oldUnit.getParts()) {
            if (p instanceof SpacecraftCoolingSystem) {
                oldLargeCraftHeatSinks = ((SpacecraftCoolingSystem) p).getTotalSinks();
                oldLargeCraftSinkType = ((SpacecraftCoolingSystem) p).getSinkType();
            }
            if ((!isOmniRefit || p.isOmniPodded())
                    || (p instanceof TransportBayPart)) {
                oldUnitParts.add(p);
            }
        }

        // Step 2a: loop through the parts arraylist in the newUnit and attempt to find the
        // corresponding part of missing part in the parts arraylist we just created. Depending on
        // what we find, we may have:
        // a) An exact copy in the same location - we move the part from the oldunit parts to the
        // newunit parts. Nothing needs to be changed in terms of refit class, time, or anything.
        // b) An exact copy in a different location - move this part to the newunit part list, but
        // change its location id. Change refit class to C and add time for removing and reinstalling
        // part.
        // c) We dont find the part in the oldunit part list.  That means this is a new part.  Add
        // this to the newequipment arraylist from step 3.  Don't change anything in terms of refit
        // stats yet, that will happen later.
        List<Part> partsRemaining = new ArrayList<>();
        for (Part part : newUnit.getParts()) {
            if (isOmniRefit && !part.isOmniPodded()) {
                continue;
            }

            boolean partFound = false;
            int i = -1;
            for (Part oPart : oldUnitParts) {
                i++;

                if (isOmniRefit && !oPart.isOmniPodded()) {
                    continue;
                }

                // If we're changing the size but not type of an LC ammo bin, we want to ensure that the ammo
                // gets tracked appropriately - it should unload to the warehouse later in the process and then
                // reload in the correct quantity. For that we must make sure the bin doesn't get dropped off
                // the old parts list here.
                if (oPart instanceof LargeCraftAmmoBin
                        && part instanceof LargeCraftAmmoBin
                        && ((LargeCraftAmmoBin) oPart).getType().equals(((LargeCraftAmmoBin) part).getType())) {
                    lcBinsToChange.add(oPart);
                }

                if ((oPart instanceof MissingPart && ((MissingPart) oPart).isAcceptableReplacement(part, true))
                        || oPart.isSamePartType(part)
                        // We're not going to require replacing the life support system just because the
                        // number of bay personnel changes.
                        || ((oPart instanceof AeroLifeSupport)
                                && (part instanceof AeroLifeSupport)
                                && (!crewSizeChanged()))) {
                    // need a special check for location and armor amount for armor
                    if ((oPart instanceof Armor) && (part instanceof Armor) &&
                            (oPart.getLocation() != part.getLocation()
                            || ((Armor) oPart).isRearMounted() != ((Armor) part).isRearMounted()
                            || ((Armor) oPart).getTotalAmount() != ((Armor) part).getTotalAmount())) {
                        continue;
                    }
                    if ((oPart instanceof VeeStabiliser)
                            && (oPart.getLocation() != part.getLocation())) {
                        continue;
                    }
                    if (part instanceof EquipmentPart) {
                        // check the location to see if this moved. If so, then don't break, but
                        // save this in case we fail to find equipment in the same location.
                        int loc = part.getLocation();
                        boolean rear = ((EquipmentPart) part).isRearFacing();
                        if ((oPart instanceof EquipmentPart
                                && (oPart.getLocation() != loc || ((EquipmentPart) oPart).isRearFacing() != rear))
                                || (oPart instanceof MissingEquipmentPart
                                        && (oPart.getLocation() != loc || ((MissingEquipmentPart) oPart).isRearFacing() != rear))) {
                            continue;
                        }
                    }
                    newUnitParts.add(oPart);
                    partFound = true;
                    break;
                }
            }

            if (partFound) {
                oldUnitParts.remove(i);
            } else {
                // Address new and moved parts next
                partsRemaining.add(part);
            }
        }

        // Step 2b: Find parts that moved or add them as new parts
        for (Part part : partsRemaining) {
            Part movedPart = null;
            int moveIndex = 0;
            int i = -1;
            for (Part oPart : oldUnitParts) {
                i++;

                if (isOmniRefit && !oPart.isOmniPodded()) {
                    continue;
                }

                if ((oPart instanceof MissingPart && ((MissingPart) oPart).isAcceptableReplacement(part, true))
                        || oPart.isSamePartType(part)
                        // We're not going to require replacing the life support system just because the
                        // number of bay personnel changes.
                        || ((oPart instanceof AeroLifeSupport)
                                && (part instanceof AeroLifeSupport)
                                && (!crewSizeChanged()))) {
                    // need a special check for location and armor amount for armor
                    if ((oPart instanceof Armor) && (part instanceof Armor)
                            && ((oPart.getLocation() != part.getLocation())
                                    || ((Armor) oPart).isRearMounted() != ((Armor) part).isRearMounted()
                                    || ((Armor) oPart).getTotalAmount() != ((Armor) part).getTotalAmount())) {
                        continue;
                    }
                    if ((oPart instanceof VeeStabiliser)
                            && (oPart.getLocation() != part.getLocation())) {
                        continue;
                    }
                    if (part instanceof EquipmentPart) {
                        // check the location to see if this moved. If so, then don't break, but
                        // save this in case we fail to find equipment in the same location.
                        int loc = part.getLocation();
                        boolean rear = ((EquipmentPart) part).isRearFacing();
                        if ((oPart instanceof EquipmentPart
                                && (oPart.getLocation() != loc || ((EquipmentPart) oPart).isRearFacing() != rear))
                                || (oPart instanceof MissingEquipmentPart
                                        && (oPart.getLocation() != loc || ((MissingEquipmentPart) oPart).isRearFacing() != rear))) {
                            movedPart = oPart;
                            moveIndex = i;
                            break;
                        }
                    }
                }
            }

            // Actually move the part or add the new part
            if (null != movedPart) {
                newUnitParts.add(movedPart);
                oldUnitParts.remove(moveIndex);
                if (movedPart.getLocation() >= 0) {
                    locationLostOldStuff[movedPart.getLocation()] = true;
                }
                if (isOmniRefit && movedPart.isOmniPodded()) {
                    updateRefitClass(CLASS_OMNI);
                } else {
                    updateRefitClass(CLASS_C);
                }
                if (movedPart instanceof EquipmentPart) {
                    // Use equivalent MissingEquipmentPart install time
                    time += movedPart.getMissingPart().getBaseTime();
                }
            } else {
                // its a new part
                // dont actually add the part itself but rather its missing equivalent
                // except in the case of armor, ammobins and the spacecraft cooling system
                if (part instanceof Armor || part instanceof AmmoBin || part instanceof SpacecraftCoolingSystem
                        || part instanceof TransportBayPart) {
                    newPartList.add(part);
                } else {
                    Part mPart = part.getMissingPart();
                    if (null != mPart) {
                        newPartList.add(mPart);
                    } else {
                        LogManager.getLogger().error("null missing part for "
                                + part.getName() + " during refit calculations");
                    }
                }
            }
        }

        // Step 3: loop through the newequipment list and determine what class of refit it entails,
        // add time for both installing this part.
        // This may involve taking a look at remaining oldunit parts to determine whether this item
        // replaces another item of the same or fewer crits. Also add cost for new equipment.
        // at the same time, check spare parts for new equipment

        // first put oldUnitParts in a new arraylist so they can be removed as we find them
        List<Part> tempParts = new ArrayList<>(oldUnitParts);

        armorNeeded = 0;
        int atype = 0;
        boolean aclan = false;
        Map<Part,Integer> partQuantity = new HashMap<>();
        List<Part> plannedReplacementParts = new ArrayList<>();
        for (Part nPart : newPartList) {
            // We don't actually want to order new BA suits; we're just pretending that we're altering the
            // existing suits.
            if (nPart instanceof MissingBattleArmorSuit) {
                continue;
            }

            /*ADD TIMES AND COSTS*/
            if (nPart instanceof MissingPart) {
                time += nPart.getBaseTime();
                Part replacement = ((MissingPart) nPart).findReplacement(true);
                // check quantity
                // TODO : the one weakness here is that we will not pick up damaged parts
                if ((null != replacement) && (null == partQuantity.get(replacement))) {
                    partQuantity.put(replacement, replacement.getQuantity());
                }

                if ((null != replacement) && (partQuantity.get(replacement) > 0)) {
                    newUnitParts.add(replacement);
                    // adjust quantity
                    partQuantity.put(replacement, partQuantity.get(replacement) - 1);
                    // If the quantity is now 0 set usedForRefitPlanning flag so findReplacement ignores this item
                    if (partQuantity.get(replacement) == 0) {
                        replacement.setUsedForRefitPlanning(true);
                        plannedReplacementParts.add(replacement);
                    }
                } else {
                    replacement = ((MissingPart) nPart).getNewPart();
                    // set entity for variable cost items
                    replacement.setUnit(newUnit);
                    cost = cost.plus(replacement.getActualValue());
                    shoppingList.add(nPart);
                }
            } else if (nPart instanceof Armor) {
                int totalAmount = ((Armor) nPart).getTotalAmount();
                time += totalAmount * ((Armor) nPart).getBaseTimeFor(newEntity);
                armorNeeded += totalAmount;
                atype = ((Armor) nPart).getType();
                aclan = nPart.isClanTechBase();
                // armor always gets added to the shopping list - it will be checked for differently
                // NOT ANYMORE - I think this is overkill, lets just reuse existing armor parts
            } else if (nPart instanceof AmmoBin) {
                AmmoBin ammoBin = (AmmoBin) nPart;
                AmmoType type = ammoBin.getType();

                ammoNeeded.merge(type, ammoBin.getFullShots(), Integer::sum);
                shoppingList.add(nPart);

                if (nPart instanceof LargeCraftAmmoBin) {
                    // Adding ammo requires base 15 minutes per ton of ammo or 60 minutes per capital missile
                    if (type.hasFlag(AmmoType.F_CAP_MISSILE) || type.hasFlag(AmmoType.F_CRUISE_MISSILE) || type.hasFlag(AmmoType.F_SCREEN)) {
                        time += 60 * ammoBin.getFullShots();
                    } else {
                        time += (int) Math.ceil(15 * Math.max(1, nPart.getTonnage()));
                    }
                } else {
                    time += 120;
                }
            } else if (nPart instanceof SpacecraftCoolingSystem) {
                int sinkType = ((SpacecraftCoolingSystem) nPart).getSinkType();
                int sinksToReplace;
                Part replacement = new AeroHeatSink(0, sinkType, false, campaign);
                newLargeCraftHeatSinks = ((SpacecraftCoolingSystem) nPart).getTotalSinks();
                if (sinkType != oldLargeCraftSinkType) {
                    sinksToReplace = newLargeCraftHeatSinks;
                } else {
                    sinksToReplace = Math.max((newLargeCraftHeatSinks - oldLargeCraftHeatSinks), 0);
                }
                time += (60 * (sinksToReplace / 50));
                while (sinksToReplace > 0) {
                    shoppingList.add(replacement);
                    sinksToReplace--;
                }
            }

            /*CHECK REFIT CLASS*/
            // See Campaign Operations, page 211 as of third printing
            if (nPart instanceof MissingEnginePart) {
                if (oldUnit.getEntity().getEngine().getRating() != newUnit.getEntity().getEngine().getRating() || newUnit.getEntity().getEngine().getEngineType() != oldUnit.getEntity().getEngine().getEngineType()) {
                    updateRefitClass(customJob ? CLASS_E : CLASS_D);
                }
                if (((MissingEnginePart) nPart).getEngine().getSideTorsoCriticalSlots().length > 0) {
                    locationHasNewStuff[Mech.LOC_LT] = true;
                    locationHasNewStuff[Mech.LOC_RT] = true;
                }
            } else if (nPart instanceof MissingMekGyro) {
                updateRefitClass(CLASS_D);
            } else if (nPart instanceof MissingMekLocation) {
                replacingLocations = true;

                // If a location is being replaced, the internal structure or myomer must have been changed.
                updateRefitClass(CLASS_F);
            } else if (nPart instanceof Armor) {
                updateRefitClass(CLASS_A);
                locationHasNewStuff[nPart.getLocation()] = true;
            } else if (nPart instanceof MissingMekCockpit) {
                updateRefitClass(CLASS_E);
                locationHasNewStuff[Mech.LOC_HEAD] = true;
            } else if (nPart instanceof MissingInfantryMotiveType || nPart instanceof MissingInfantryArmorPart) {
                updateRefitClass(CLASS_A);
            } else {
                //determine whether this is A, B, or C
                if (nPart instanceof MissingEquipmentPart || nPart instanceof AmmoBin) {
                    nPart.setUnit(newUnit);
                    int loc;
                    EquipmentType type;
                    double size;
                    if (nPart instanceof MissingEquipmentPart) {
                        loc = nPart.getLocation();
                        if (loc > -1 && loc < newEntity.locations()) {
                            locationHasNewStuff[loc] = true;
                        }
                        type = ((MissingEquipmentPart) nPart).getType();
                        size = ((MissingEquipmentPart) nPart).getSize();
                    } else {
                        loc = nPart.getLocation();
                        if (loc > -1 && loc < newEntity.locations()) {
                            locationHasNewStuff[loc] = true;
                        }
                        type = ((AmmoBin) nPart).getType();
                        size = ((AmmoBin) nPart).getSize();
                    }
                    int crits = type.getCriticals(newUnit.getEntity(), size);
                    nPart.setUnit(oldUnit);
                    int i = -1;
                    boolean matchFound = false;
                    int matchIndex = -1;
                    int rClass = CLASS_D;
                    for (Part oPart : tempParts) {
                        i++;
                        int oLoc = -1;
                        int oCrits = -1;
                        EquipmentType oType = null;
                        if (oPart instanceof MissingEquipmentPart) {
                            oLoc = oPart.getLocation();
                            oType = ((MissingEquipmentPart) oPart).getType();
                            oCrits = oType.getCriticals(oldUnit.getEntity(),
                                    ((MissingEquipmentPart) oPart).getSize());
                        } else if (oPart instanceof EquipmentPart) {
                            oLoc = oPart.getLocation();
                            oType = ((EquipmentPart) oPart).getType();
                            oCrits = oType.getCriticals(oldUnit.getEntity(),
                                ((EquipmentPart) oPart).getSize());
                        }
                        if (loc != oLoc) {
                            continue;
                        }
                        if ((crits == oCrits) && (oType != null)
                                && (type.hasFlag(WeaponType.F_LASER) == oType.hasFlag(WeaponType.F_LASER))
                                && (type.hasFlag(WeaponType.F_MISSILE) == oType.hasFlag(WeaponType.F_MISSILE))
                                && (type.hasFlag(WeaponType.F_BALLISTIC) == oType.hasFlag(WeaponType.F_BALLISTIC))
                                && (type.hasFlag(WeaponType.F_ARTILLERY) == oType.hasFlag(WeaponType.F_ARTILLERY))) {
                            rClass = CLASS_A;
                            matchFound = true;
                            matchIndex = i;
                            break;
                        } else if (crits <= oCrits) {
                            rClass = CLASS_B;
                            matchFound = true;
                            matchIndex = i;
                            // don't break because we may find something better
                        } else {
                            rClass = CLASS_C;
                            matchFound = true;
                            matchIndex = i;
                            // don't break because we may find something better
                        }
                    }
                    if (isOmniRefit && nPart.isOmniPoddable()) {
                        rClass = CLASS_OMNI;
                    }
                    updateRefitClass(rClass);
                    if (matchFound) {
                        tempParts.remove(matchIndex);
                    }
                }
            }
        }

        // if oldUnitParts is not empty we are removing some stuff and so this should
        // be at least a Class A refit
        if (!oldUnitParts.isEmpty()) {
            if (isOmniRefit) {
                updateRefitClass(CLASS_OMNI);
            } else {
                updateRefitClass(CLASS_A);
            }
        }

        /*
         * Cargo and transport bays are essentially just open space and while it may take time and materials
         * to change the cubicles or the number of doors, the bay itself does not require any refit work
         * unless the size changes. First we create a list of all bays on each unit, then we attempt to
         * match them by size and number of doors. Any remaining are matched on size, and difference in
         * number of doors is noted as moving doors has to be accounted for in the time calculation.
         */
        List<Bay> oldUnitBays = oldUnit.getEntity().getTransportBays().stream()
                .filter(b -> !b.isQuarters()).collect(Collectors.toList());
        List<Bay> newUnitBays = newEntity.getTransportBays().stream()
                .filter(b -> !b.isQuarters()).collect(Collectors.toList());
        // If any bays keep the same size but have any doors added or removed, we need to note that separately
        // since removing a door from one bay and adding it to another requires time even if the number
        // of parts hasn't changed. We track them separately so that we don't charge time for changing the
        // overall number of doors twice.
        int doorsRemoved = 0;
        int doorsAdded = 0;
        if (oldUnitBays.size() + newUnitBays.size() > 0) {
            for (Iterator<Bay> oldbays = oldUnitBays.iterator(); oldbays.hasNext(); ) {
                final Bay oldbay = oldbays.next();
                for (Iterator<Bay> newbays = newUnitBays.iterator(); newbays.hasNext(); ) {
                    final Bay newbay = newbays.next();
                    if ((oldbay.getCapacity() == newbay.getCapacity())
                            && (oldbay.getDoors() == newbay.getDoors())) {
                        oldbays.remove();
                        newbays.remove();
                        break;
                    }
                }
            }
            for (Iterator<Bay> oldbays = oldUnitBays.iterator(); oldbays.hasNext(); ) {
                final Bay oldbay = oldbays.next();
                for (Iterator<Bay> newbays = newUnitBays.iterator(); newbays.hasNext(); ) {
                    final Bay newbay = newbays.next();
                    if (oldbay.getCapacity() == newbay.getCapacity()) {
                        if (oldbay.getDoors() > newbay.getDoors()) {
                            doorsRemoved += oldbay.getDoors() - newbay.getDoors();
                        } else {
                            doorsAdded += newbay.getDoors() - oldbay.getDoors();
                        }
                        oldbays.remove();
                        newbays.remove();
                        break;
                    }
                }
            }
            // Use bay replacement time of 1 month (30 days) for each bay to be resized,
            // plus another month for any bays to be added or removed.
            time += Math.max(oldUnitBays.size(), newUnitBays.size()) * 14400;
            int deltaDoors = oldUnitBays.stream().mapToInt(Bay::getDoors).sum()
                    - newUnitBays.stream().mapToInt(Bay::getDoors).sum();
            if (deltaDoors < 0) {
                doorsAdded = Math.max(0, doorsAdded - deltaDoors);
            } else {
                doorsRemoved = Math.max(0, doorsRemoved + deltaDoors);
            }
            time += (doorsAdded + doorsRemoved) * 600;
        }

        // Step 4: loop through remaining equipment on oldunit parts and add time for removing.
        for (Part oPart : oldUnitParts) {
            // We're pretending we're changing the old suit rather than removing it.
            // We also want to avoid accounting for legacy InfantryAttack parts.
            if ((oPart instanceof BattleArmorSuit)
                    || (oPart instanceof TransportBayPart)
                    || ((oPart instanceof EquipmentPart
                            && ((EquipmentPart) oPart).getType() instanceof InfantryAttack))) {
                continue;
            }
            if (oPart.getLocation() >= 0) {
                locationLostOldStuff[oPart.getLocation()] = true;
            }
            if (oPart instanceof MissingPart) {
                continue;
            }
            if (oPart instanceof AmmoBin) {
                int remainingShots = ((AmmoBin) oPart).getFullShots() - ((AmmoBin) oPart).getShotsNeeded();
                AmmoType type = ((AmmoBin) oPart).getType();
                if (remainingShots > 0) {
                    if (oPart instanceof LargeCraftAmmoBin) {
                        if (type.hasFlag(AmmoType.F_CAP_MISSILE) || type.hasFlag(AmmoType.F_CRUISE_MISSILE) || type.hasFlag(AmmoType.F_SCREEN)) {
                            time += 60 * ((LargeCraftAmmoBin) oPart).getFullShots();
                        } else {
                            time += 15 * Math.max(1, (int) oPart.getTonnage());
                        }
                    } else {
                        time += 120;
                    }
                    ammoRemoved.merge(type, remainingShots, Integer::sum);
                }
                continue;
            }
            if (oPart instanceof Armor && sameArmorType) {
                recycledArmorPoints += ((Armor) oPart).getAmount();
                // Refund the time we added above for the "new" armor that actually wasn't.
                time -= ((Armor) oPart).getAmount() * ((Armor) oPart).getBaseTimeFor(oldUnit.getEntity());
                continue;
            }
            boolean isSalvaging = oldUnit.isSalvage();
            oldUnit.setSalvage(true);
            time += oPart.getBaseTime();
            oldUnit.setSalvage(isSalvaging);
        }

        if (sameArmorType) {
            // if this is the same armor type then we can recycle armor
            armorNeeded -= recycledArmorPoints;
        }

        if (armorNeeded > 0) {
            if (newEntity.isSupportVehicle() && (atype == EquipmentType.T_ARMOR_STANDARD)) {
                newArmorSupplies = new SVArmor(newEntity.getBARRating(newEntity.firstArmorIndex()),
                        newEntity.getArmorTechRating(), 0, Entity.LOC_NONE, getCampaign());
            } else {
                newArmorSupplies = new Armor(0, atype, 0, 0, false, aclan, getCampaign());
            }
            newArmorSupplies.setAmountNeeded(armorNeeded);
            newArmorSupplies.setRefitUnit(oldUnit);
            // check existing supplies before determining cost
            Armor existingArmorSupplies = getExistingArmorSupplies();
            double tonnageNeeded = newArmorSupplies.getTonnageNeeded();
            if (null != existingArmorSupplies) {
                tonnageNeeded = Math.max(0, tonnageNeeded - existingArmorSupplies.getTonnage());
            }
            newArmorSupplies.setUnit(oldUnit);

            cost = cost.plus(newArmorSupplies.adjustCostsForCampaignOptions(
                    newArmorSupplies.getStickerPrice().multipliedBy(tonnageNeeded)).dividedBy(5.0));
            newArmorSupplies.setUnit(null);
        }

        // TODO : use ammo removed from the old unit in the case of changing between full ton and
        // TODO : half ton MG or OS/regular.
        for (AmmoType type : ammoNeeded.keySet()) {
            int shotsNeeded = Math.max(ammoNeeded.get(type) - campaign.getQuartermaster().getAmmoAvailable(type), 0);
            int shotsPerTon = type.getShots();
            if ((shotsNeeded > 0) && (shotsPerTon > 0)) {
                cost = cost.plus(Money.of(type.getCost(newEntity, false, -1) * ((double) shotsNeeded / shotsPerTon)));
            }
        }

        /*
         * Figure out how many untracked heat sinks are needed to complete the refit or will
         * be removed. These are engine integrated heat sinks for Mechs or ASFs that change
         * the heat sink type or heat sinks required for energy weapons for vehicles and
         * conventional fighters.
         */
        if ((newEntity instanceof Mech)
                || ((newEntity instanceof Aero) && !(newEntity instanceof ConvFighter))) {
            Part oldHS = heatSinkPart(oldUnit.getEntity());
            Part newHS = heatSinkPart(newEntity);
            int oldCount = untrackedHeatSinkCount(oldUnit.getEntity());
            int newCount = untrackedHeatSinkCount(newEntity);
            if (oldHS.isSamePartType(newHS)) {
                // If the number changes we need to add them to either the warehouse at the end of
                // refit or the shopping list at the beginning.
                for (int i = 0; i < oldCount - newCount; i++) {
                    oldIntegratedHS.add(oldHS.clone());
                }
                for (int i = 0; i < newCount - oldCount; i++) {
                    // Heat sink added for supply chain tracking purposes and removed from refit later
                    newIntegratedHS.add(newHS.getMissingPart());
                }
            } else {
                for (int i = 0; i < oldCount; i++) {
                    oldIntegratedHS.add(oldHS.clone());
                }
                for (int i = 0; i < newCount; i++) {
                    // Heat sink added for supply chain tracking purposes and removed from refit later
                    newIntegratedHS.add(newHS.getMissingPart());
                }
                updateRefitClass(CLASS_D);
            }
        } else if ((newEntity instanceof Tank)
                || (newEntity instanceof ConvFighter)) {
            int oldHS = untrackedHeatSinkCount(oldUnit.getEntity());
            int newHS = untrackedHeatSinkCount(newEntity);
            // We're only concerned with heat sinks that have to be installed in excess of what
            // may be provided by the engine.
            if (oldUnit.getEntity().hasEngine()) {
                oldHS = Math.max(0, oldHS - oldUnit.getEntity().getEngine().getWeightFreeEngineHeatSinks());
            }
            if (newEntity.hasEngine()) {
                newHS = Math.max(0, newHS - newEntity.getEngine().getWeightFreeEngineHeatSinks());
            }
            if (oldHS != newHS) {
                Part hsPart = heatSinkPart(newEntity); // only single HS allowed, so they have to be of the same type
                hsPart.setOmniPodded(isOmniRefit);
                for (int i = oldHS; i < newHS; i++) {
                    // Heat sink added for supply chain tracking purposes and removed from refit later
                    newIntegratedHS.add(hsPart.getMissingPart());
                }
                for (int i = newHS; i < oldHS; i++) {
                    oldIntegratedHS.add(hsPart.clone());
                }
            }
        }
        time += (oldIntegratedHS.size() + newIntegratedHS.size()) * 90;
        for (Part nHsPart : newIntegratedHS) {
            // Check warehouse for spare heat sinks before adding to shopping list
            Part replacement = ((MissingPart) nHsPart).findReplacement(true);
            //check quantity
            if ((null != replacement) && (null == partQuantity.get(replacement))) {
                partQuantity.put(replacement, replacement.getQuantity());
            }
            if ((null != replacement) && (partQuantity.get(replacement) > 0)) {
                newUnitParts.add(replacement);
                //adjust quantity
                partQuantity.put(replacement, partQuantity.get(replacement) - 1);
                //If the quantity is now 0 set usedForRefitPlanning flag so findReplacement ignores this item
                if (partQuantity.get(replacement) == 0) {
                    replacement.setUsedForRefitPlanning(true);
                    plannedReplacementParts.add(replacement);
                }
            } else {
                shoppingList.add(nHsPart);
            }
        }

        //clear any planned replacement flags
        for (Part rPart : plannedReplacementParts) {
            rPart.setUsedForRefitPlanning(false);
        }

        //check for CASE
        //TODO: we still dont have to order the part, we need to get the CASE issues sorted out
        if (!oldUnit.getEntity().isClan()) { // Clan units always have CASE or CASE II everywhere
            for (int loc = 0; loc < newEntity.locations(); loc++) {
                // If the old location has neither kind of CASE and the new location has either, update the refit class
                if (!(oldUnit.getEntity().locationHasCase(loc) || (oldUnit.getEntity() instanceof Mech && ((Mech) oldUnit.getEntity()).hasCASEII(loc))) && (newEntity.locationHasCase(loc) || (newEntity instanceof Mech && ((Mech) newEntity).hasCASEII(loc)))) {
                    if (isOmniRefit) {
                        updateRefitClass(CLASS_OMNI);
                    } else {
                        time += 60;
                        updateRefitClass(CLASS_D);
                    }
                }
            }
        }

        // multiply time by refit class
        time *= getTimeMultiplier();

        // Refit Kits cost an additional 10% beyond the cost
        // of their components. (SO p188)
        if (!customJob) {
            cost = cost.multipliedBy(1.1);
        }

        //TODO: track the number of locations changed so we can get stuff for omnis
        //TODO: some class D stuff is not omnipodable
        if (refitClass == CLASS_OMNI) {
            int nloc = 0;
            for (int loc = 0; loc < newEntity.locations(); loc++) {
                if (locationHasNewStuff[loc] || locationLostOldStuff[loc]) {
                    nloc++;
                }
            }
            time = 30 * nloc;
        }

        //infantry take zero time to re-organize
        //also check for squad size and number changes
        if (oldUnit.isConventionalInfantry()) {
            if (((Infantry) oldUnit.getEntity()).getSquadCount() != ((Infantry) newEntity).getSquadCount()
                    ||((Infantry) oldUnit.getEntity()).getSquadSize() != ((Infantry) newEntity).getSquadSize()) {
                updateRefitClass(CLASS_A);
            }
            time = 0;
        }

        //figure out if we are putting new stuff on a missing location
        if (!replacingLocations) {
            for (int loc = 0; loc < newEntity.locations(); loc++) {
                if (locationHasNewStuff[loc] && oldUnit.isLocationDestroyed(loc)) {
                    String problem = "Can't add new equipment to a missing " + newEntity.getLocationAbbr(loc);
                    if (null == fixableString) {
                        fixableString = problem;
                    } else {
                        fixableString += "\n" + problem;
                    }
                }
            }
        }

        // Now we set the refurbishment values
        if (isRefurbishing) {
            // Refurbishment rules (class, time, and cost) are found in SO p189.
            refitClass = CLASS_E;

            final int oneWeekInMinutes = 60 * 24 * 7;
            if (newEntity instanceof megamek.common.Warship || newEntity instanceof megamek.common.SpaceStation) {
                time = oneWeekInMinutes * 12; // 3 Months [12 weeks]
            } else if (newEntity instanceof megamek.common.Dropship || newEntity instanceof megamek.common.Jumpship) {
                time = oneWeekInMinutes * 4; // 1 Month [4 weeks]
            } else if (newEntity instanceof Mech || newEntity instanceof megamek.common.Aero) { // ConvFighter and SmallCraft are derived from Aero
                time = oneWeekInMinutes * 2; // 2 Weeks
            } else if (newEntity instanceof BattleArmor || newEntity instanceof megamek.common.Tank || newEntity instanceof megamek.common.Protomech) {
                time = oneWeekInMinutes; // 1 Week
            } else {
                time = 1111;
                LogManager.getLogger().error("Unit " + newEntity.getModel() + " did not set its time correctly.");
            }

            // The cost is equal to 10 percent of the units base value (not modified for quality). (SO p189)
            cost = oldUnit.getBuyCost().multipliedBy(0.1);
        }
        if (oldUnit.hasPrototypeTSM() || newUnit.hasPrototypeTSM()) {
            time *= 2;
        }
    }

    public void begin() throws EntityLoadingException, IOException {
        if (customJob) {
            saveCustomization();
        }
        oldUnit.setRefit(this);
        // Bay space might change, and either way all cargo needs to be unloaded while the refit is in progress
        oldUnit.unloadTransportShip();
        newEntity.setOwner(oldUnit.getEntity().getOwner());

        // We don't want to require waiting for a refit kit if all that is missing is ammo or ammo bins.
        Map<AmmoType,Integer> shotsNeeded = new HashMap<>();
        for (Part part : newUnitParts) {
            if (part instanceof AmmoBin) {
                AmmoBin bin = (AmmoBin) part;
                bin.setShotsNeeded(bin.getFullShots());
                shotsNeeded.merge(bin.getType(), bin.getShotsNeeded(), Integer::sum);
            }
        }

        for (Iterator<Part> iter = shoppingList.iterator(); iter.hasNext(); ) {
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
                shoppingList.add(ammo);
            }
        }

        reserveNewParts();
        if (customJob) {
            //add the stuff on the shopping list to the master shopping list
            ArrayList<Part> newShoppingList = new ArrayList<>();
            for (Part part : shoppingList) {
                part.setUnit(null);
                if (part instanceof Armor) {
                    //Taharqa: WE shouldn't be here anymore, given that I am no longer adding
                    //armor by location to the shopping list but instead changing it all via
                    //the newArmorSupplies object, but commented out for completeness
                    //getCampaign().getQuartermaster().addPart(part, 0);
                    //part.setRefitUnit(oldUnit);
                    //newUnitParts.add(part.getId());
                }
                else if (part instanceof AmmoBin) {
                    // TODO: custom job ammo...

                    //ammo bins are free - bleh
                    AmmoBin bin = (AmmoBin) part;
                    bin.setShotsNeeded(bin.getFullShots());
                    part.setRefitUnit(oldUnit);
                    getCampaign().getQuartermaster().addPart(part, 0);
                    newUnitParts.add(part);

                    // Check if we need more ammo
                    if (bin.needsFixing()) {
                        getCampaign().getShoppingList().addShoppingItem(bin.getNewPart(), 1, getCampaign());
                    }
                }
                else if (part instanceof IAcquisitionWork) {
                    getCampaign().getShoppingList().addShoppingItem(((IAcquisitionWork) part), 1, getCampaign());
                    newShoppingList.add(part);
                }
            }
            shoppingList = newShoppingList;
            if (null != newArmorSupplies) {
                //add enough armor to the shopping list
                int armorSupplied = 0;
                Armor existingArmorSupplies = getExistingArmorSupplies();
                if (null != existingArmorSupplies) {
                    armorSupplied = existingArmorSupplies.getAmount();
                }

                while (armorSupplied < armorNeeded) {
                    armorSupplied += ((Armor) newArmorSupplies.getNewPart()).getAmount();
                    getCampaign().getShoppingList().addShoppingItem((Armor) newArmorSupplies.getNewPart(), 1, getCampaign());
                }
            }
        } else {
            for (Part part : shoppingList) {
                part.setUnit(null);
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
            checkForArmorSupplies();
            if (shoppingList.isEmpty() && (null == newArmorSupplies || newArmorSupplies.getAmountNeeded() == 0)) {
                kitFound = true;
            } else {
                getCampaign().getShoppingList().addShoppingItem(this, 1, getCampaign());
            }
        }

        if (isRefurbishing) {
            if (campaign.getQuartermaster().buyRefurbishment(this)) {
                campaign.addReport("<font color='green'><b>Refurbishment ready to begin</b></font>");
            } else {
                campaign.addReport("You cannot afford to refurbish " + oldUnit.getEntity().getShortName() + ". Transaction cancelled");
            }
        }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
    }

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

    public boolean partsInTransit() {
        for (Part part : newUnitParts) {
            if (!part.isPresent()) {
                return true;
            }
        }
        return null != newArmorSupplies && !newArmorSupplies.isPresent();
    }

    public boolean acquireParts() {
        if (!customJob) {
            checkForArmorSupplies();
            return kitFound && !partsInTransit() && (null == newArmorSupplies || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
        }

        ArrayList<Part> newShoppingList = new ArrayList<>();
        for (Part part : shoppingList) {
            if (part instanceof AmmoStorage) {
                continue;
            }

            if (part instanceof IAcquisitionWork) {
                //check to see if we found a replacement
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

        checkForArmorSupplies();
        shoppingList = newShoppingList;

        // Also, check to make sure that they're not still in transit! - ralgith 2013/07/09
        if (partsInTransit()) {
            return false;
        }

        return shoppingList.isEmpty()
            && ((null == newArmorSupplies) || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
    }

    public void checkForArmorSupplies() {
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
                newArmorSupplies.setAmountNeeded(newArmorSupplies.getAmountNeeded() - existingArmorSupplies.getAmount());
                getCampaign().getWarehouse().removePart(existingArmorSupplies);
            }
            if (newArmorSupplies.getId() <= 0) {
                 getCampaign().getQuartermaster().addPart(newArmorSupplies, 0);
            }
        }
    }

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

    private void updateRefitClass(int rClass) {
        if (rClass > refitClass) {
            refitClass = rClass;
        }
    }

    public void cancel() {
        oldUnit.setRefit(null);

        for (Part part : newUnitParts) {
            part.setRefitUnit(null);

            // If the part was not part of the old unit we need to consolidate it with others of its type
            // in the warehouse. Ammo Bins just get unloaded and removed; no reason to keep them around.
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
                if ((loc == Mech.LOC_CT) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc) > 0)) {
                    part.setUnit(null);
                    getCampaign().getWarehouse().removePart(part);
                }
            } else if ((part instanceof StructuralIntegrity) || (part instanceof BattleArmorSuit)
                    || (part instanceof TransportBayPart)
                    || ((part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof InfantryAttack))) {
                // SI Should never be "kept" for the Warehouse
                // We also don't want to generate new BA suits that have been replaced
                // or allow legacy InfantryAttack BA parts to show up in the warehouse.
                getCampaign().getWarehouse().removePart(part);
            } else if (part instanceof Armor) {
                Armor a = (Armor) part;
                //lets just re-use this armor part
                if (!sameArmorType) {
                    //give the amount back to the warehouse since we are switching types
                    a.changeAmountAvailable(a.getAmount());
                    if (null != newArmorSupplies) {
                        a.changeType(newArmorSupplies.getType(), newArmorSupplies.isClanTechBase());
                    }
                }
                // Removing vehicle turrets or changing BA squad size can reduce the number of armor locations.
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
        for (Part part : lcBinsToChange) {
            if (part instanceof AmmoBin) {
                ((AmmoBin) part).unload();
            }

        }
        // add leftover untracked heat sinks to the warehouse
        for (Part part : oldIntegratedHS) {
            campaign.getQuartermaster().addPart(part, 0);
        }

        // dont forget to switch entities!
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
            // Only Aerospace Fighters are expected to have heat sink parts (Mechs handled separately)
            // SmallCraft, DropShip, JumpShip, WarShip, and SpaceStation use SpacecraftCoolingSystem instead
            expectedHeatSinkParts = ((Aero) newEntity).getHeatSinks() - ((Aero) newEntity).getPodHeatSinks() -
                    untrackedHeatSinkCount(newEntity);
        }
        for (Part part : newUnitParts) {
            if ((!replacingLocations) && (part instanceof MekLocation)) {
                // Preserve any hip or shoulder damage
                int loc = ((MekLocation) part).getLoc();
                if ((oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) ||
                        (oldEntity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc) > 0)) {
                    // Apply damage to hip or shoulder at slot 0
                    newEntity.getCritical(loc, 0).setDestroyed(true);
                }
            }
            if ((part instanceof HeatSink) && (newEntity instanceof Tank)) {
                // Unit should not have heat sink parts
                // Remove heat sink parts added for supply chain tracking purposes
                getCampaign().getWarehouse().removePart(part);
                continue;
            }
            else if ((part instanceof AeroHeatSink) && (newEntity instanceof Aero) && !part.isOmniPodded()) {
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

        final EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(oldUnit);
        final EquipmentUnscramblerResult result = unscrambler.unscramble();
        if (!result.succeeded()) {
            LogManager.getLogger().warn(result.getMessage());
        }

        changeAmmoBinMunitions(oldUnit);

        assignArmActuators();
        assignBayParts();

        if (newEntity instanceof Mech) {
            // Now that Mech part locations have been set
            // Remove heat sink parts added for supply chain tracking purposes
            for (final Iterator<Part> partsIter = oldUnit.getParts().iterator(); partsIter.hasNext();) {
                final Part part = partsIter.next();
                if ((part instanceof HeatSink) && (part.getLocation() == Entity.LOC_NONE)) {
                    getCampaign().getWarehouse().removePart(part);
                    partsIter.remove();
                }
            }
        }

        for (Part p : newParts) {
            // CAW: after a refit some parts ended up NOT having a Campaign attached,
            // see https://github.com/MegaMek/mekhq/issues/2703
            p.setCampaign(getCampaign());

            if (p instanceof AmmoBin) {
                // All large craft ammo got unloaded into the warehouse earlier, though the part IDs have now changed.
                // Consider all LC ammobins empty and load them back up.
                if (p instanceof LargeCraftAmmoBin) {
                    ((AmmoBin) p).setShotsNeeded(((AmmoBin) p).getFullShots());
                }

                ((AmmoBin) p).loadBin();
            }
        }

        if (null != newArmorSupplies) {
            getCampaign().getWarehouse().removePart(newArmorSupplies);
        }
        // in some cases we may have had more armor on the original unit and so we may add more
        // back then we received

        // FIXME: This doesn't deal properly with patchwork armor.
        if (sameArmorType && armorNeeded < 0) {
            Armor a;
            Entity en = oldUnit.getEntity();
            if (en.isSupportVehicle() && en.getArmorType(en.firstArmorIndex()) == EquipmentType.T_ARMOR_STANDARD) {
                a = new SVArmor(en.getBARRating(en.firstArmorIndex()), en.getArmorTechRating(),
                        -armorNeeded, Entity.LOC_NONE, getCampaign());
            } else{
                a = new Armor(0, en.getArmorType(en.firstArmorIndex()),
                        -1 * armorNeeded, -1, false, aclan, getCampaign());
            }
            a.setUnit(oldUnit);
            a.changeAmountAvailable(a.getAmount());
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
            for (Part p : oldUnit.getParts()) {
                if (p.getQuality() != QUALITY_F) {
                    p.improveQuality();
                }
            }
        }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
    }

    private void changeAmmoBinMunitions(final Unit unit) {
        for (final Part part : unit.getParts()) {
            if (part instanceof AmmoBin) {
                final AmmoBin ammoBin = (AmmoBin) part;
                final Mounted mounted = unit.getEntity().getEquipment(ammoBin.getEquipmentNum());
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

    public void saveCustomization() throws EntityLoadingException {
        UnitUtil.compactCriticals(newEntity);

        String fileName = MHQXMLUtility.escape(newEntity.getChassis() + ' ' + newEntity.getModel());
        String sCustomsDir = String.join(File.separator, "data", "mechfiles", "customs"); // TODO : Remove inline file path
        String sCustomsDirCampaign = sCustomsDir + File.separator + getCampaign().getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                LogManager.getLogger().error("Failed to create directory " + sCustomsDir + ", and therefore cannot save the unit.");
                return;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDirCampaign.mkdir()) {
                LogManager.getLogger().error("Failed to create directory " + sCustomsDirCampaign + ", and therefore cannot save the unit.");
                return;
            }
        }

        String fileNameCampaign;
        try {
            if (newEntity instanceof Mech) {
                // if this file already exists then don't overwrite it or we will end up with a bunch of copies
                String fileOutName = sCustomsDir + File.separator + fileName + ".mtf";
                fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".mtf";
                if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                    throw new IOException("A file already exists with the custom name "+fileNameCampaign+". Please choose a different name. (Unit name and/or model)");
                }
                try (FileOutputStream out = new FileOutputStream(fileNameCampaign);
                    PrintStream p = new PrintStream(out)) {
                    p.println(((Mech) newEntity).getMtf());
                }
            } else {
                // if this file already exists then don't overwrite it or we will end up with a bunch of copies
                String fileOutName = sCustomsDir + File.separator + fileName + ".blk";
                fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".blk";
                if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                    throw new IOException("A file already exists with the custom name "+fileNameCampaign+". Please choose a different name. (Unit name and/or model)");
                }
                BLKFile.encode(fileNameCampaign, newEntity);
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            fileNameCampaign = null;
        }

        getCampaign().addCustom(newEntity.getChassis() + " " + newEntity.getModel());

        try {
            MechSummary summary = Utilities.retrieveOriginalUnit(newEntity);

            newEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            LogManager.getLogger().info(String.format("Saved %s %s to %s",
                    newEntity.getChassis(), newEntity.getModel(), summary.getSourceFile()));
        } catch (EntityLoadingException ex) {
            LogManager.getLogger().error(String.format("Could not read back refit entity %s %s",
                    newEntity.getChassis(), newEntity.getModel()), ex);

            if (fileNameCampaign != null) {
                LogManager.getLogger().warn("Deleting invalid refit file " + fileNameCampaign);
                try {
                    new File(fileNameCampaign).delete();
                } catch (SecurityException ex2) {
                    LogManager.getLogger().warn("Could not clean up bad refit file " + fileNameCampaign, ex2);
                }
            }

            // Reload the mech cache if we had to delete the file
            MechSummaryCache.getInstance().loadMechData();

            throw ex;
        }
    }


    private int getTimeMultiplier() {
        int mult;
        switch (refitClass) {
            case NO_CHANGE:
                mult = 0;
                break;
            case CLASS_C:
                mult = 2;
                break;
            case CLASS_D:
                mult = 3;
                break;
            case CLASS_E:
                mult = 4;
                break;
            case CLASS_F:
                mult = 5;
                break;
            case CLASS_A:
            case CLASS_B:
            default:
                mult = 1;
                break;
        }
        if (customJob) {
            mult *= 2;
        }
        return mult;
    }

    public Entity getOriginalEntity() {
        return oldUnit.getEntity();
    }

    public Entity getNewEntity() {
        return newEntity;
    }

    public Unit getOriginalUnit() {
        return oldUnit;
    }

    public boolean hasFailedCheck() {
        return failedCheck;
    }

    @Override
    public boolean needsFixing() {
        return true;
    }

    @Override
    public int getDifficulty() {
        switch (refitClass) {
            case NO_CHANGE:
                return 0;
            case CLASS_C:
            case CLASS_D:
                return 2;
            case CLASS_E:
                return 3;
            case CLASS_F:
                return 4;
            case CLASS_OMNI:
                return -2;
            case CLASS_A:
            case CLASS_B:
            default:
                return 1;
        }
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
        mods.append(oldUnit.getSiteMod());
        if (oldUnit.getEntity().hasQuirk("easy_maintain")) {
            mods.addModifier(-1, "easy to maintain");
        } else if (oldUnit.getEntity().hasQuirk("difficult_maintain")) {
            mods.addModifier(1, "difficult to maintain");
        }

        if (customJob) {
            mods.addModifier(2, "custom job");
        }

        if ((null != tech) && tech.getOptions().booleanOption("tech_engineer")) {
            mods.addModifier(-2, "engineer");
        }
        return mods;
    }

    @Override
    public String succeed() {
        complete();
        if (isRefurbishing) {
            return "Refurbishment of " + oldUnit.getEntity().getShortName() + " is complete.";
        } else {
            return "The customization of "+ oldUnit.getEntity().getShortName() + " is complete.";
        }
    }

    @Override
    public String fail(int rating) {
        timeSpent = 0;
        failedCheck = true;
        // Refurbishment doesn't get extra time like standard refits.
        if (isRefurbishing) {
            oldUnit.setRefit(null); // Failed roll results in lost time and money
            return "Refurbishment of " + oldUnit.getEntity().getShortName() + " was unsuccessful";
        }
        else {
            return "The customization of " + oldUnit.getEntity().getShortName() + " will take " + getTimeLeft() + " additional minutes to complete.";
        }
    }

    @Override
    public void resetTimeSpent() {
        timeSpent = 0;
    }

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

    @Override
    public String getAcquisitionName() {
        return getPartName();
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public int getSkillMin() {
        return SkillType.EXP_GREEN;
    }

    @Override
    public int getBaseTime() {
        return time;
    }

    @Override
    public int getActualTime() {
        return time;
    }

    @Override
    public int getTimeSpent() {
        return timeSpent;
    }

    @Override
    public int getTimeLeft() {
        return time - timeSpent;
    }

    @Override
    public void addTimeSpent(int time) {
        timeSpent += time;
    }

    @Override
    public @Nullable Person getTech() {
        return assignedTech;
    }

    @Override
    public void setTech(@Nullable Person tech) {
        assignedTech = tech;
    }

    @Override
    public boolean hasWorkedOvertime() {
        return false;
    }

    @Override
    public void setWorkedOvertime(boolean b) {

    }

    @Override
    public int getShorthandedMod() {
        return 0;
    }

    @Override
    public void setShorthandedMod(int i) {

    }

    /**
     * Requiring the life support system to be changed just because the number of bay personnel changes
     * is a bit much. Instead, we'll limit it to changes in crew size, measured by quarters.
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

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {

    }

    @Override
    public void updateConditionFromPart() {

    }

    @Override
    public void fix() {

    }

    @Override
    public void remove(boolean salvage) {

    }

    @Override
    public @Nullable MissingPart getMissingPart() {
        // not applicable
        return null;
    }

    @Override
    public String getDesc() {
        return newEntity.getModel() + " " + getDetails();
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        return "(" + getRefitClassName() + "/" + getTimeLeft() + " minutes/" + getCost().toAmountAndSymbolString() + ")";
    }

    @Override
    public Unit getUnit() {
        return oldUnit;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public @Nullable String checkFixable() {
        return fixableString;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "refit");
        pw.println(MHQXMLUtility.writeEntityToXmlString(newEntity, indent, getCampaign().getEntities()));
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "time", time);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeSpent", timeSpent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "refitClass", refitClass);
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

        if (!lcBinsToChange.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "lcBinsToChange");
            for (Part part : lcBinsToChange) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pid", part.getId());
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "lcBinsToChange");
        }

        if (!shoppingList.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "shoppingList");
            for (final Part part : shoppingList) {
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
                    retVal.refitClass = Integer.parseInt(wn2.getTextContent());
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
                    retVal.newEntity = Objects.requireNonNull(MHQXMLUtility.parseSingleEntityMul((Element) wn2, campaign));
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
                            retVal.lcBinsToChange.add(new RefitPartRef(Integer.parseInt(wn3.getTextContent())));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("shoppingList")) {
                    processShoppingList(retVal, wn2, retVal.oldUnit, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("newArmorSupplies")) {
                    processArmorSupplies(retVal, wn2, version);
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }

        return retVal;
    }

    private static void processShoppingList(Refit retVal, Node wn, Unit u, Version version) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                LogManager.getLogger().error("Unknown node type not loaded in Part nodes: " + wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                p.setUnit(u);
                retVal.shoppingList.add(p);
            } else {
                LogManager.getLogger().error((u != null)
                        ? String.format("Unit %s has invalid parts in its refit shopping list", u.getId())
                        : "Invalid parts in shopping list");
            }
        }
    }

    private static void processArmorSupplies(Refit retVal, Node wn, Version version) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                LogManager.getLogger().error("Unknown node type not loaded in Part nodes: " + wn2.getNodeName());

                continue;
            }
            Part p = Part.generateInstanceFromXML(wn2, version);

            if (p instanceof Armor) {
                retVal.newArmorSupplies = (Armor) p;
                break;
            }
        }
    }

    public void reCalc() {
        setCampaign(oldUnit.getCampaign());
        for (Part p : shoppingList) {
            p.setCampaign(oldUnit.getCampaign());
        }

        if (null != newArmorSupplies) {
            newArmorSupplies.setCampaign(oldUnit.getCampaign());
        }
    }

    @Override
    public Part getNewEquipment() {
        return this;
    }

    @Override
    public String getAcquisitionDesc() {
        return "Fill this in";
    }

    @Override
    public String getAcquisitionDisplayName() {
        return null;
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return null;
    }

    @Override
    public String getAcquisitionBonus() {
        return null;
    }

    @Override
    public Part getAcquisitionPart() {
        return null;
    }

    @Override
    public Money getStickerPrice() {
        return cost;
    }

    @Override
    public Money getActualValue() {
        // This is a case that the price should already be adjusted for campaign options
        return getStickerPrice();
    }

    @Override
    public Money getBuyCost() {
        return getActualValue();
    }

    public void addRefitKitParts(int transitDays) {
        for (Part part : shoppingList) {
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
                Part newPart = (Part)((IAcquisitionWork) part).getNewEquipment();
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
                Armor a = (Armor) newArmorSupplies.getNewPart();
                a.setAmount(amount);
                getCampaign().getQuartermaster().addPart(a, transitDays);
            }
            checkForArmorSupplies();
        }
        shoppingList = new ArrayList<>();
        kitFound = true;
    }

    @Override
    public String find(int transitDays) {
        if (campaign.getQuartermaster().buyPart(this, transitDays)) {
            return "<font color='green'><b> refit kit found.</b> Kit will arrive in " + transitDays + " days.</font>";
        } else {
            return "<font color='red'><b> You cannot afford this refit kit. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public String failToFind() {
        return "<font color='red'> refit kit not found.</font>";
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll roll = new TargetRoll();
        int avail = EquipmentType.RATING_A;
        int techBaseMod = 0;
        for (Part part : shoppingList) {
            if (getTechBase() == T_CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > techBaseMod) {
                techBaseMod = campaign.getCampaignOptions().getClanAcquisitionPenalty();
            } else if (getTechBase() == T_IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > techBaseMod) {
                techBaseMod = campaign.getCampaignOptions().getIsAcquisitionPenalty();
            } else if (getTechBase() == T_BOTH) {
                int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(), campaign.getCampaignOptions().getIsAcquisitionPenalty());
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

    @Override
    public void resetOvertime() {
    }

    @Override
    public int getTechLevel() {
        return 0;
    }

    @Override
    public int getTechBase() {
        return Part.T_BOTH;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return true;
    }

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

    private void assignArmActuators() {
        if (!(oldUnit.getEntity() instanceof BipedMech)) {
            return;
        }
        BipedMech m = (BipedMech) oldUnit.getEntity();
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

                if (type == Mech.ACTUATOR_LOWER_ARM) {
                    if (loc == Mech.LOC_RARM) {
                        rightLowerArm = part;
                    } else if (loc == Mech.LOC_LARM) {
                        leftLowerArm = part;
                    } else if (null == missingArm1 && part instanceof MekActuator) {
                        missingArm1 = (MekActuator) part;
                    } else if (part instanceof MekActuator) {
                        missingArm2 = (MekActuator) part;
                    }
                } else if (type == Mech.ACTUATOR_HAND) {
                    if (loc == Mech.LOC_RARM) {
                        rightHand = part;
                    } else if (loc == Mech.LOC_LARM) {
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
        if (null == rightHand && m.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
            MekActuator part = missingHand1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingHand2;
            }
            if (null != part) {
                part.setLocation(Mech.LOC_RARM);
            }
        }

        if (null == leftHand && m.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            MekActuator part = missingHand1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingHand2;
            }

            if (null != part) {
                part.setLocation(Mech.LOC_LARM);
            }
        }

        if (null == rightLowerArm && m.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
            MekActuator part = missingArm1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }

            if (null != part) {
                part.setLocation(Mech.LOC_RARM);
            }
        }
        if (null == leftLowerArm && m.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            MekActuator part = missingArm1;
            if (null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }

            if (null != part) {
                part.setLocation(Mech.LOC_LARM);
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
        for (Part p : oldUnit.getParts()) {
            if (p instanceof BayDoor) {
                doors.add(p);
            } else if (p instanceof Cubicle) {
                cubicles.putIfAbsent(((Cubicle) p).getBayType(), new ArrayList<>());
                cubicles.get(((Cubicle) p).getBayType()).add(p);
            } else if (p instanceof TransportBayPart) {
                oldBays.add(p);
            }
        }
        oldBays.forEach(p -> p.remove(false));
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
     * Refits may require adding or removing heat sinks that are not tracked as parts. For Mechs and
     * ASFs this would be engine-integrated heat sinks if the heat sink type is changed. For vehicles and
     * conventional fighters this would be heat sinks required by energy weapons.
     *
     * @param entity Either the starting or the ending unit of the refit.
     * @return       The number of heat sinks the unit mounts that are not tracked as parts.
     */
    private int untrackedHeatSinkCount(Entity entity) {
        if (entity instanceof Mech) {
            return Math.min(((Mech) entity).heatSinks(), entity.getEngine().integralHeatSinkCapacity(((Mech) entity).hasCompactHeatSinks()));
        } else if (newEntity.getClass() == Aero.class) { // Aero but not subclasses
            return entity.getEngine().getWeightFreeEngineHeatSinks();
        } else {
            EntityVerifier verifier = EntityVerifier.getInstance(new File(
                    "data/mechfiles/UnitVerifierOptions.xml"));
            TestEntity te;
            if (entity instanceof Tank) {
                te = new TestTank((Tank) entity, verifier.tankOption, null);
                return te.getCountHeatSinks();
            } else if (entity instanceof ConvFighter) {
                te = new TestAero((Aero) entity, verifier.aeroOption, null);
                return te.getCountHeatSinks();
            } else {
                return 0;
            }
        }
    }

    /**
     * Creates an independent heat sink part appropriate to the unit that can be used to track
     * needed and leftover parts for heat sinks that are not actually tracked by the unit.
     *
     * @param entity Either the original or the new unit.
     * @return       The part corresponding to the type of heat sink for the unit.
     */
    private Part heatSinkPart(Entity entity) {
        if (entity instanceof Aero) {
            if (((Aero) entity).getHeatType() == Aero.HEAT_DOUBLE && entity.isClan()) {
                return new AeroHeatSink(0, AeroHeatSink.CLAN_HEAT_DOUBLE, false, campaign);
            }
            return new AeroHeatSink(0, ((Aero) entity).getHeatType(), false, campaign);
        } else if (entity instanceof Mech) {
            Optional<MiscMounted> mount = entity.getMisc().stream()
                    .filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
                            || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
                    .findAny();
            if (mount.isPresent()) {
                return new HeatSink(0, mount.get().getType(), -1, false, campaign);
            }
        }
        return new HeatSink(0, EquipmentType.get("Heat Sink"), -1, false, campaign);
    }


    @Override
    public String getShoppingListReport(int quantity) {
        return getAcquisitionName() + " has been added to the procurement list.";
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTechRating() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub

    }

    @Override
    public Part clone() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets a value indicating whether or not this refit
     * is a custom job. If false, this is a Refit Kit (SO p188).
     */
    public boolean isCustomJob() {
        return customJob;
    }

    /**
     * Gets a value indicating whether or not the refit
     * kit has been found, if applicable.
     */
    public boolean kitFound() {
        return kitFound;
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }

    public boolean isBeingRefurbished() {
        return isRefurbishing;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }

    @Override
    public void fixReferences(Campaign campaign) {
        super.fixReferences(campaign);

        setCampaign(campaign);

        if (newArmorSupplies instanceof RefitArmorRef) {
            Part realPart = campaign.getWarehouse().getPart(newArmorSupplies.getId());
            if (realPart instanceof Armor) {
                newArmorSupplies = (Armor) realPart;
            } else {
                LogManager.getLogger().error(
                    String.format("Refit on Unit %s references missing armor supplies %d",
                        getUnit().getId(), newArmorSupplies.getId()));
                newArmorSupplies = null;
            }
        }

        for (int ii = oldUnitParts.size() - 1; ii >= 0; --ii) {
            Part part = oldUnitParts.get(ii);
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                if (realPart != null) {
                    oldUnitParts.set(ii, realPart);
                } else if (part.getId() > 0) {
                    LogManager.getLogger().error(
                        String.format("Refit on Unit %s references missing old unit part %d",
                            getUnit().getId(), part.getId()));
                    oldUnitParts.remove(ii);
                } else {
                    LogManager.getLogger().error(
                            String.format("Refit on Unit %s references unknown old unit part with an id of 0",
                                    getUnit().getId()));
                    oldUnitParts.remove(ii);
                }
            }
        }

        for (int ii = newUnitParts.size() - 1; ii >= 0; --ii) {
            Part part = newUnitParts.get(ii);
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                if (realPart != null) {
                    newUnitParts.set(ii, realPart);
                } else if (part.getId() > 0) {
                    LogManager.getLogger().error(
                        String.format("Refit on Unit %s references missing new unit part %d",
                            getUnit().getId(), part.getId()));
                    newUnitParts.remove(ii);
                } else {
                    LogManager.getLogger().error(
                            String.format("Refit on Unit %s references unknown new unit part with an id of 0",
                                    getUnit().getId()));
                    newUnitParts.remove(ii);
                }
            }
        }

        List<Part> realParts = new ArrayList<>();
        Iterator<Part> it = lcBinsToChange.iterator();
        while (it.hasNext()) {
            Part part = it.next();
            if (part instanceof RefitPartRef) {
                Part realPart = campaign.getWarehouse().getPart(part.getId());
                it.remove();
                if (realPart != null) {
                    realParts.add(realPart);
                } else {
                    LogManager.getLogger().error(
                        String.format("Refit on Unit %s references missing large craft ammo bin %d",
                            getUnit().getId(), part.getId()));
                }
            }
        }

        lcBinsToChange.addAll(realParts);

        if (assignedTech instanceof RefitPersonRef) {
            UUID id = assignedTech.getId();
            assignedTech = campaign.getPerson(id);
            if (assignedTech == null) {
                LogManager.getLogger().error(
                    String.format("Refit on Unit %s references missing tech %s",
                        getUnit().getId(), id));
            }
        }
    }

    public static class RefitArmorRef extends Armor {
        private RefitArmorRef(int id) {
            this.id = id;
        }
    }

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

    public static class RefitPersonRef extends Person {
        private RefitPersonRef(UUID id) {
            super(id);
        }
    }
}
