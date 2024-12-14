/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.StrategicFormation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

import java.util.*;

import static java.lang.Math.floor;
import static megamek.common.MiscType.F_LIFTHOIST;
import static megamek.common.MiscType.F_MOBILE_FIELD_BASE;
import static megamek.common.MiscType.F_SALVAGE_ARM;
import static megamek.common.MiscType.S_SPOT_WELDER;
import static megamek.common.UnitType.DROPSHIP;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.force.FormationLevel.LANCE;
import static mekhq.campaign.force.StrategicFormation.getStandardForceSize;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_MAINTENANCE;
import static mekhq.campaign.unit.Unit.SITE_FIELD_WORKSHOP;
import static mekhq.campaign.unit.Unit.SITE_IMPROVISED;
import static mekhq.utilities.EntityUtilities.getEntityFromUnitId;

/**
 * The {@code RepairLocation} class is responsible for determining repair locations for units
 * within the context of a campaign. This includes assigning units to available repair bays,
 * designating fallback locations based on equipment or contract conditions, and handling
 * overflow when bay capacity is exceeded.
 * <p>
 * It uses data from the {@link Campaign}, {@link Force}, and {@link AtBContract} classes to:
 * - Assign repair sites to units.
 * - Compute available repair resources such as bays and equipment.
 * - Handle units that cannot be assigned to repair locations due to constraints.
 */
public class RepairLocation {
    private static Campaign campaign;

    private static int globalRepairLocation;

    private static double equipmentSupportCapacity;

    private static int smallCraftBays;
    private static int mekBays;
    private static int asfBays;
    private static int superHeavyVehicleBays;
    private static int heavyVehicleBays;
    private static int lightVehicleBays;
    private static int protoMekBays;
    private static int battleArmorBays;
    private static int infantryBays;

    private static final double EQUIPMENT_SUPPORT_VALUE = 0.5;

    /**
     * Determines and assigns repair locations for units based on bays available, contract type,
     * and equipment support. Units are assigned to the most appropriate location, with overflow
     * units handled based on remaining resources.
     *
     * @param campaign   The current campaign.
     * @param contract   The relevant contract.
     * @param force      The force whose units are being processed. Generally, this will be a
     *                  Combat Team, or if the force is not assigned to a Combat Team, the force itself.
     */
    public static void determineRepairLocation(Campaign campaign, AtBContract contract, Force force) {
        if (force.isStrategicFormation()) {
            StrategicFormation combatTeam = campaign.getStrategicFormationsTable().get(force.getId());

            if (combatTeam != null) {
                if (!combatTeam.getContract(campaign).equals(contract)) {
                    return;
                }
            }
        }

        RepairLocation.campaign = campaign;

        Vector<UUID> allUnitIds = force.getAllUnits(false);

        // Contract-based location
        getGlobalRepairLocation(contract);

        // Check we haven't already got a good location
        if (globalRepairLocation >= SITE_FACILITY_BASIC) {
            for (UUID uuid : allUnitIds) {
                Unit unit = campaign.getUnit(uuid);

                if (unit != null) {
                    Entity entity = unit.getEntity();

                    if (entity != null) {
                        if (entity.getUnitType() < DROPSHIP) {
                            unit.setSite(globalRepairLocation);
                        } else {
                            unit.setSite(SITE_FACILITY_BASIC);
                        }
                    }
                }
            }

            return;
        }

        // Bay-based modifiers
        countBays(allUnitIds);
        List<Unit> unitsWithoutBay = assignUnitsToBays(allUnitIds);

        // Equipment-based modifiers
        getSupportCapacity(allUnitIds);

        // Sort un-bayed units so that higher BV units are ordered before lower BV units
        unitsWithoutBay = unitsWithoutBay.stream()
            .filter(unit -> unit.getEntity() != null) // Remove units with null entities
            .sorted(Comparator.comparing(unit -> unit.getEntity().calculateBattleValue(),
                Comparator.reverseOrder()))
            .toList(); // Collect sorted units into list

        for (Unit unit : unitsWithoutBay) {
            if (equipmentSupportCapacity > 0) {
                equipmentSupportCapacity--;
                unit.setSite(SITE_FIELD_WORKSHOP);
            }
        }
    }

    /**
     * Determines and assigns simplified repair locations for a list of units based on the
     * contract type and global repair location. If a valid global repair location is found,
     * it assigns all eligible units to that location. Aerospace units are defaulted to a
     * basic facility if the unit type exceeds the threshold for DropShips.
     * <p>
     * This is primarily intended for salvaged units or units not present in a user's TO&E.
     *
     * @param campaign   The current campaign.
     * @param contract   The relevant contract.
     * @param units      A list of {@link Unit} objects to process for repair location assignment.
     */
    public static void determineSimplifiedRepairLocation(Campaign campaign, AtBContract contract,
                                                         List<Unit> units) {
        RepairLocation.campaign = campaign;

        // Contract-based location
        getGlobalRepairLocation(contract);

        // Check we haven't already got a good location
        if (globalRepairLocation >= SITE_FACILITY_BASIC) {
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (entity != null) {
                    if (entity.getUnitType() < DROPSHIP) {
                        unit.setSite(globalRepairLocation);
                    } else {
                        unit.setSite(SITE_FACILITY_BASIC);
                    }
                }
            }
        }
    }

    /**
     * Determines the global repair location based on the contract.
     *
     * @param contract   The relevant contract.
     */
    private static void getGlobalRepairLocation(AtBContract contract) {
        // Default location
        globalRepairLocation = SITE_FIELD_WORKSHOP;

        // Contract-based location
        AtBContractType contractType = contract.getContractType();

        if (contractType.isRaidType() || contractType.isGuerrillaWarfare()) {
            if (contract.getCommandRights().isIndependent()) {
                globalRepairLocation = SITE_IMPROVISED;
            }
        }

        if (contractType.isGarrisonType()) {
            if (contract.getCommandRights().isIndependent()) {
                globalRepairLocation = SITE_FACILITY_BASIC;
            } else {
                globalRepairLocation = SITE_FACILITY_MAINTENANCE;
            }
        }
    }

    /**
     * Counts the number of available repair bays by type in the campaign and updates the
     * respective static variables. Bays are classified by their type (e.g., Mek bays,
     * vehicle bays, infantry bays, etc.), and their capacities are multiplied based on
     * the faction's standard force size.
     *
     * @param allUnits   A vector of UUIDs representing all units to analyze.
     */
    private static void countBays(Vector<UUID> allUnits) {
        smallCraftBays = 0;
        mekBays = 0;
        asfBays = 0;
        superHeavyVehicleBays = 0;
        heavyVehicleBays = 0;
        lightVehicleBays = 0;
        protoMekBays = 0;
        battleArmorBays = 0;
        infantryBays = 0;

        // Iterating through each unit in the campaign
        for (UUID unitId : allUnits) {

            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            Faction campaignFaction = campaign.getFaction();
            final int CAPACITY_MULTIPLIER = getStandardForceSize(campaignFaction, COMPANY.getDepth());

            // Iterate through each bay in entity
            for (Bay bay : entity.getTransportBays()) {
                if (bay instanceof SmallCraftBay) {
                    smallCraftBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof MekBay) {
                    mekBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof ASFBay) {
                    asfBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof SuperHeavyVehicleBay) {
                    superHeavyVehicleBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof HeavyVehicleBay) {
                    heavyVehicleBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof LightVehicleBay) {
                    lightVehicleBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof ProtoMekBay) {
                    protoMekBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof BattleArmorBay) {
                    battleArmorBays += (int) bay.getCapacity() * CAPACITY_MULTIPLIER;
                } else if (bay instanceof InfantryBay) {
                    infantryBays += (int) floor(bay.getCapacity() / ((InfantryBay) bay).getPlatoonType().getWeight())
                        * CAPACITY_MULTIPLIER;
                }
            }
        }
    }

    /**
     * Assigns units to repair bays based on priority and availability. Units are sorted
     * by their battle value (highest to lowest) to prioritize more valuable units for repair bays.
     * <p>
     * Overflow units that cannot be assigned are returned for further processing.
     *
     * @param allUnitIds   A vector of UUIDs representing the IDs of all units to process.
     * @return A list of units that could not be assigned to a repair bay.
     */
    private static List<Unit> assignUnitsToBays(Vector<UUID> allUnitIds) {
        // Step 1: Fetch all valid units (non-null) and sort by battle value (highest to lowest)
        List<Unit> allUnits = allUnitIds.stream()
            .map(campaign::getUnit) // Fetch unit by UUID
            .filter(Objects::nonNull) // Remove null units
            .filter(unit -> unit.getEntity() != null) // Remove units with null entities
            .sorted(Comparator.comparing(unit -> unit.getEntity().calculateBattleValue(),
                Comparator.reverseOrder()))
            .toList(); // Collect sorted units into list

        // Step 2: Process bays and assign units to appropriate locations
        List<Unit> unitsWithoutBay = new ArrayList<>();
        List<Unit> overflowUnits = new ArrayList<>();

        for (Unit unit : allUnits) {
            if (!assignUnitToBay(unit, unitsWithoutBay)) {
                overflowUnits.add(unit);
            }
        }

        // Step 3: Handle overflow units with remaining bay capacity
        processOverflowUnits(overflowUnits, unitsWithoutBay);

        return unitsWithoutBay;
    }

    /**
     * Attempts to assign a single unit to the most appropriate repair bay based on its
     * characteristics. Bays are allocated in a prioritized order, starting with the most
     * suitable type for the unit in question.
     *
     * @param unit            The unit to assign to a repair bay.
     * @param unitsWithoutBay A list to store units that cannot be assigned to any bay.
     * @return {@code true} if the unit was successfully assigned to a bay, {@code false} otherwise.
     */
    private static boolean assignUnitToBay(Unit unit, List<Unit> unitsWithoutBay) {
        Entity entity = unit.getEntity();

        if (entity == null) {
            return false;
        }

        if (entity.isVehicle()) {
            double weight = entity.getWeight();
            if ((weight > 100) && (allocateSpecificBay(() -> superHeavyVehicleBays--, superHeavyVehicleBays))) {
                unit.setSite(SITE_FACILITY_BASIC);
                return true;
            } else if ((weight > 50) && (allocateSpecificBay(() -> heavyVehicleBays--, heavyVehicleBays))) {
                unit.setSite(SITE_FACILITY_BASIC);
                return true;
            } else if (allocateSpecificBay(() -> lightVehicleBays--, lightVehicleBays)) {
                unit.setSite(SITE_FACILITY_BASIC);
                return true;
            }
        } else if ((entity.isSmallCraft()) && (allocateSpecificBay(() -> smallCraftBays--, smallCraftBays))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        } else if ((entity.isMek()) && ((allocateSpecificBay(() -> mekBays--, mekBays)))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        } else if ((entity.isAerospaceFighter() || entity.isConventionalFighter())
            && (allocateSpecificBay(() -> asfBays--, asfBays))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        } else if ((entity.isProtoMek()) && (allocateSpecificBay(() -> protoMekBays--, protoMekBays))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        } else if ((entity.isBattleArmor()) && (allocateSpecificBay(() -> battleArmorBays--, battleArmorBays))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        } else if ((entity.isInfantry()) && (allocateSpecificBay(() -> infantryBays--, infantryBays))) {
            unit.setSite(SITE_FACILITY_BASIC);
            return true;
        }

        unitsWithoutBay.add(unit); // If no bay is found, add to units without a bay list
        return false;
    }

    /**
     * Processes overflow units that could not be assigned to standard repair bays.
     * Attempts to allocate remaining bays to these units based on their type and characteristics.
     * Units that cannot be allocated are added to the list of units without a bay.
     *
     * @param overflowUnits     A list of units that could not be assigned in the initial allocation phase.
     * @param unitsWithoutBay   A list to store units that still cannot be assigned to a repair location.
     */
    private static void processOverflowUnits(List<Unit> overflowUnits, List<Unit> unitsWithoutBay) {
        for (Unit unit : overflowUnits) {
            Entity entity = unit.getEntity();

            if (entity == null) {
                // If an entity is null, we ignore it and move one
                continue;
            }

            if (entity.isVehicle()) {
                double weight = entity.getWeight();
                if ((weight <= 100 && weight > 50)
                    && (allocateSpecificBay(() -> superHeavyVehicleBays--, superHeavyVehicleBays))) {
                    unit.setSite(SITE_FACILITY_BASIC);
                } else if (allocateSpecificBay(() -> superHeavyVehicleBays--, superHeavyVehicleBays)) {
                    unit.setSite(SITE_FACILITY_BASIC);
                } else {
                    unitsWithoutBay.add(unit); // If no bay available, add to units without bay
                }
            } else if ((entity.isAerospaceFighter() || entity.isConventionalFighter()) &&
                (allocateSpecificBay(() -> smallCraftBays--, smallCraftBays))) {
                unit.setSite(SITE_FACILITY_BASIC);
            } else {
                unitsWithoutBay.add(unit); // Add to units without bay if no suitable bay is found
            }
        }
    }

    /**
     * Allocates a specific type of repair bay to a unit if any are available. This method
     * decreases the count of the targeted bay if allocation is possible.
     *
     * @param decrementBay      A {@link Runnable} for decrementing the count of the targeted bay.
     * @param currentBayCount   The current count of available bays.
     * @return {@code true} if the bay was successfully allocated, {@code false} otherwise.
     */
    private static boolean allocateSpecificBay(Runnable decrementBay, int currentBayCount) {
        if (currentBayCount > 0) {
            decrementBay.run(); // Decrement the bay count
            return true;
        }
        return false; // No bay available
    }

    /**
     * Calculates the total equipment support capacity for the campaign based on mounted equipment,
     * such as lift hoists, salvage arms, and mobile field bases. Each type of equipment adds to the
     * available capacity or modifies the multiplier based on faction attributes.
     *
     * @param allUnits   A vector of UUIDs representing all units within the relevant force.
     */
    private static void getSupportCapacity(Vector<UUID> allUnits) {
        equipmentSupportCapacity = 0;

        Faction campaignFaction = campaign.getFaction();
        int supportEquipmentMultiplier = getStandardForceSize(campaignFaction, LANCE.getDepth());
        int mobileFieldBasesMultiplier = getStandardForceSize(campaignFaction, COMPANY.getDepth());

        for (UUID unitId : allUnits) {
            Unit unit = campaign.getUnit(unitId);

            if (unit == null || !unit.isFullyCrewed()) {
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                continue;
            }


            for (Mounted<?> mounted : entity.getMisc()) {
                if (mounted.getType().hasFlag(F_LIFTHOIST)) {
                    equipmentSupportCapacity += EQUIPMENT_SUPPORT_VALUE * supportEquipmentMultiplier;
                    continue;
                }

                if (mounted.getType().hasFlag(F_SALVAGE_ARM)) {
                    equipmentSupportCapacity += EQUIPMENT_SUPPORT_VALUE * supportEquipmentMultiplier;
                    continue;
                }

                if (mounted.getType().hasSubType(S_SPOT_WELDER)) {
                    equipmentSupportCapacity += EQUIPMENT_SUPPORT_VALUE * supportEquipmentMultiplier;
                    continue;
                }

                if (mounted.getType().hasFlag(F_MOBILE_FIELD_BASE)) {
                    equipmentSupportCapacity += mobileFieldBasesMultiplier;
                }
            }
        }
    }

    /**
     * Assigns all units in the campaign to appropriate repair locations based on their forces affiliation,
     * the contract, and available repair sites. Units are divided and handled in two groups:
     * - Units belonging to forces are processed using their respective forces.
     * - Units without a valid force are handled separately.
     *
     * @param campaign   The current campaign.
     * @param contract   The active contract.
     */
    public static void assignAllUnitsToRepairSite(Campaign campaign, AtBContract contract) {
        List<Unit> unitsWithoutAForce = new ArrayList<>();
        Set<Force> forces = new HashSet<>();

        for (Unit unit : campaign.getUnits()) {
            int forceId = unit.getForceId();

            if (forceId == FORCE_NONE) {
                unitsWithoutAForce.add(unit);
            } else {
                Force force = campaign.getForce(forceId);

                if (force == null) {
                    unitsWithoutAForce.add(unit);
                } else if (!force.isStrategicFormation()) {
                    forces.add(force);
                }
            }
        }

        for (StrategicFormation combatTeam : campaign.getAllStrategicFormations()) {
            if (!combatTeam.getContract(campaign).equals(contract)) {
                continue;
            }

            Force force = campaign.getForce(combatTeam.getForceId());

            if (force != null) {
                forces.add(force);
            }
        }

        for (Force force : forces) {
            determineRepairLocation(campaign, contract, force);
        }

        determineSimplifiedRepairLocation(campaign, contract, unitsWithoutAForce);
    }
}
