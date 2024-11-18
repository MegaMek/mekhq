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
package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.*;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransportationRating {
    private static final MMLogger logger = MMLogger.create(TransportationRating.class);

    /**
     * Calculates the transportation rating for the given campaign.
     *
     * @param campaign The campaign for which to calculate the transportation
     *                 rating.
     * @return A list containing maps of transportation capacities and requirements,
     *         including the transportation rating.
     */
    public static List<Map<String, Integer>> calculateTransportationRating(Campaign campaign) {
        // Calculate transportation capacities and requirements for the campaign
        Map<String, Integer> transportationCapacities = calculateTransportationCapacities(campaign);
        Map<String, Integer> transportationRequirements = calculateTransportRequirements(campaign);
        Map<String, Integer> transportationValues = new HashMap<>();

        int transportationRating = 0;

        // For each type of entity (denoted here as "SmallCraft", "AeroSpaceFighter",
        // etc.)
        // calculate the rating adjustment based on the capacity and requirements

        // Small Craft
        int capacity = transportationCapacities.get("smallCraftBays"); // Get the capacity for the entity
        int requirements = transportationRequirements.get("smallCraftCount"); // Get the requirements for the entity

        // Add the rating adjustment to the total; We do this for each following entity
        int rating = calculateRating(capacity, requirements);
        transportationValues.put("smallCraft", rating);
        transportationRating += rating;

        // Calculate spare capacity if any.
        // Some entity types can use spare bays not used by other entities
        int spareCapacity = Math.max(0, capacity - requirements);

        // The above logic is repeated for the remaining entities

        // ASF
        capacity = transportationCapacities.get("asfBays");
        requirements = Math.max(0, transportationRequirements.get("asfCount") - spareCapacity);

        rating = calculateRating(capacity, requirements);
        transportationValues.put("asf", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // Meks
        capacity = transportationCapacities.get("mekBays");
        requirements = transportationRequirements.get("mekCount");

        rating = calculateRating(capacity, requirements);
        transportationValues.put("mek", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // Super Heavy Vehicles
        capacity = transportationCapacities.get("superHeavyVehicleBays");
        requirements = transportationRequirements.get("superHeavyVehicleCount");

        rating = calculateRating(capacity, requirements);
        transportationValues.put("superHeavyVehicle", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        spareCapacity = Math.max(0, capacity - requirements);

        // Heavy Vehicles
        capacity = transportationCapacities.get("heavyVehicleBays");
        requirements = Math.max(0, transportationRequirements.get("heavyVehicleCount") - spareCapacity);

        rating = calculateRating(capacity, requirements);
        transportationValues.put("heavyVehicle", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // as this spare capacity can also be used by light vehicles,
        // we need to track the remaining spare capacity
        spareCapacity -= Math.max(0, transportationRequirements.get("heavyVehicleCount"));
        spareCapacity += Math.max(0, capacity - requirements);

        // Light Vehicles
        capacity = transportationCapacities.get("lightVehicleBays");
        requirements = Math.max(0, transportationRequirements.get("lightVehicleCount") - spareCapacity);

        rating = calculateRating(capacity, requirements);
        transportationValues.put("lightVehicle", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // ProtoMeks
        capacity = transportationCapacities.get("protoMekBays");
        requirements = transportationRequirements.get("protoMekCount");

        rating = calculateRating(capacity, requirements);
        transportationValues.put("protoMek", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // Battle Armor
        capacity = transportationCapacities.get("battleArmorBays");
        requirements = transportationRequirements.get("battleArmorCount");

        rating = calculateRating(capacity, requirements);
        transportationValues.put("battleArmor", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // Infantry
        capacity = transportationCapacities.get("infantryBays");
        requirements = transportationRequirements.get("infantryCount");

        rating = calculateRating(capacity, requirements);
        transportationValues.put("infantry", rating);
        if (transportationRating == 0 && rating > 0) {
            transportationRating += rating;
        }

        // Support Personnel
        capacity = transportationCapacities.get("passengerCapacity");
        requirements = transportationRequirements.get("passengerCount");

        if ((capacity > 0) && (capacity >= requirements)) {
            transportationRating += 3;
            transportationValues.put("passenger", 3);
            logger.debug("Exceeding Support Personnel Transport Requirements: +3");
        } else if ((requirements > 0) && (transportationRating > 0)) {
            transportationRating -= 3;
            transportationValues.put("passenger", -3);
            logger.debug("Below Support Personnel Transport Requirements: -3");
        }

        // JumpShip & WarShip Presence
        if (transportationCapacities.get("hasJumpShipOrWarShip") > 0) {
            transportationRating += 10;
            logger.debug("Has JumpShip or WarShip: +10");
        }

        // Docking Collar Requirements
        int dockingCollarCount = transportationCapacities.get("dockingCollars");

        if ((dockingCollarCount > 0) && (dockingCollarCount >= transportationRequirements.get("dropShipCount"))) {
            transportationRating += 5;
            logger.debug("Exceeding docking collar requirements: +5");
        }

        if (transportationRequirements.get("dropShipCount") == 0) {
            transportationRating -= 5;
            logger.debug("No DropShip: -");
        }

        // Finally, the calculated transportation rating is added to the map of
        // transportation capacities
        transportationCapacities.put("total", transportationRating);
        logger.debug("Transportation Rating = {}", transportationRating);

        // Return list of capacities and requirements
        return List.of(transportationCapacities, transportationRequirements, transportationValues);
    }

    /**
     * Calculates the transportation rating adjustment based on capacity and
     * requirements.
     *
     * @param capacity     the transport bay capacity
     * @param requirements the transport bay usage
     * @return the rating calculated based on the capacity and requirements
     */
    protected static int calculateRating(int capacity, int requirements) {
        if (requirements > 0) {
            int usage = capacity - requirements;

            if (usage < 0) {
                return -5;
            } else if (usage > requirements * 2) {
                return 10;
            } else {
                return 5;
            }
        }

        return 0;
    }

    /**
     * Retrieves the transportation bays and passenger capacity for a campaign.
     *
     * @param campaign the campaign to retrieve the transportation bays and
     *                 passenger capacity for
     * @return a map containing the transportation bays and passenger capacity,
     *         where each key represents
     *         a bay type and its corresponding value represents the count or
     *         capacity
     */
    private static Map<String, Integer> calculateTransportationCapacities(Campaign campaign) {
        int uncrewedUnits = 0;
        int dockingCollars = 0;
        int hasJumpShipOrWarShip = 0;

        int smallCraftBays = 0, mekBays = 0, asfBays = 0, superHeavyVehicleBays = 0, heavyVehicleBays = 0,
                lightVehicleBays = 0, protoMekBays = 0, battleArmorBays = 0, infantryBays = 0, passengerCapacity = 0;

        // Iterating through each unit in the campaign
        for (Unit unit : campaign.getActiveUnits()) {
            Entity entity = unit.getEntity();

            // Skip the unit if it doesn't meet the specific criteria
            if (!(entity.isDropShip()) && !(entity.isJumpShip())
                    && !(entity.isWarShip()) && !(entity.isSmallCraft())) {
                continue;
            }

            // If not fully crewed, increment the uncrewed unit count and skip this unit
            if (!unit.isFullyCrewed()) {
                uncrewedUnits++;
                continue;
            }

            // this is a binary bonus, so we only need to flip the value once
            if ((hasJumpShipOrWarShip == 0) && (entity.isJumpShip() || entity.isWarShip())) {
                hasJumpShipOrWarShip = 1;
            }

            dockingCollars += entity.getDockingCollars().size();

            // Iterate through each bay in entity
            for (Bay bay : entity.getTransportBays()) {
                if (bay instanceof SmallCraftBay) {
                    smallCraftBays += (int) bay.getCapacity();
                } else if (bay instanceof MekBay) {
                    mekBays += (int) bay.getCapacity();
                } else if (bay instanceof ASFBay) {
                    asfBays += (int) bay.getCapacity();
                } else if (bay instanceof SuperHeavyVehicleBay) {
                    superHeavyVehicleBays += (int) bay.getCapacity();
                } else if (bay instanceof HeavyVehicleBay) {
                    heavyVehicleBays += (int) bay.getCapacity();
                } else if (bay instanceof LightVehicleBay) {
                    lightVehicleBays += (int) bay.getCapacity();
                } else if (bay instanceof ProtoMekBay) {
                    protoMekBays += (int) bay.getCapacity();
                } else if (bay instanceof BattleArmorBay) {
                    battleArmorBays += (int) bay.getCapacity();
                } else if (bay instanceof InfantryBay) {
                    infantryBays += (int) Math
                            .floor(bay.getCapacity() / ((InfantryBay) bay).getPlatoonType().getWeight());
                }

                passengerCapacity += bay.getPersonnel(entity.isClan());
            }
        }

        // Map the capacity of each bay type
        Map<String, Integer> transportationCapacities = new HashMap<>(Map.of(
                "smallCraftBays", smallCraftBays,
                "mekBays", mekBays,
                "asfBays", asfBays,
                "superHeavyVehicleBays", superHeavyVehicleBays,
                "heavyVehicleBays", heavyVehicleBays,
                "lightVehicleBays", lightVehicleBays,
                "protoMekBays", protoMekBays,
                "battleArmorBays", battleArmorBays,
                "infantryBays", infantryBays,
                "passengerCapacity", passengerCapacity));

        // add the supplemental information to the map
        transportationCapacities.put("hasJumpShipOrWarShip", hasJumpShipOrWarShip);
        transportationCapacities.put("dockingCollars", dockingCollars);
        transportationCapacities.put("uncrewedUnits", uncrewedUnits);

        // log the stored information to aid debugging
        logger.debug("Transportation Capacities = {}",
                transportationCapacities.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        // return the map
        return transportationCapacities;
    }

    /**
     * Calculates the transport requirements for the given campaign.
     *
     * @param campaign the campaign for which to calculate the transport
     *                 requirements
     * @return a map containing the count for each type of entity in the campaign
     */
    private static Map<String, Integer> calculateTransportRequirements(Campaign campaign) {
        // Initialize variables to store counts of different unit types
        int dropShipCount = 0, smallCraftCount = 0, mekCount = 0, asfCount = 0, superHeavyVehicleCount = 0,
                heavyVehicleCount = 0, lightVehicleCount = 0, protoMekCount = 0, battleArmorCount = 0,
                infantryCount = 0;

        // Iterate through each unit in the campaign
        for (Unit unit : campaign.getActiveUnits()) {
            Entity entity = unit.getEntity();

            // Vehicles are handled separately based on their weight
            if (entity.isVehicle()) {
                double weight = entity.getWeight();
                if (weight > 100) {
                    superHeavyVehicleCount++;
                } else if (weight > 50) {
                    heavyVehicleCount++;
                } else {
                    lightVehicleCount++;
                }
            }
            // Non-vehicle entities are categorized based on the entity types
            else {
                if (entity.isDropShip()) {
                    dropShipCount++;
                } else if (entity.isSmallCraft()) {
                    smallCraftCount++;
                } else if (entity.isMek()) {
                    mekCount++;
                } else if (entity.isAerospaceFighter() || entity.isConventionalFighter()) {
                    asfCount++;
                } else if (entity.isProtoMek()) {
                    protoMekCount++;
                } else if (entity.isBattleArmor()) {
                    battleArmorCount++;
                } else if (entity.isInfantry()) {
                    infantryCount++;
                }
            }
        }

        // Count the number of passengers by filtering the personnel list
        int passengerCount = (int) campaign.getPersonnel().stream()
                .filter(person -> !person.getStatus().isAbsent() && !person.getStatus().isDepartedUnit())
                .filter(person -> person.getUnit() == null)
                .count();

        // Map each unit count to its type
        Map<String, Integer> transportRequirements = new HashMap<>(Map.of(
                "dropShipCount", dropShipCount,
                "smallCraftCount", smallCraftCount,
                "mekCount", mekCount,
                "asfCount", asfCount,
                "superHeavyVehicleCount", superHeavyVehicleCount,
                "heavyVehicleCount", heavyVehicleCount,
                "lightVehicleCount", lightVehicleCount,
                "protoMekCount", protoMekCount,
                "battleArmorCount", battleArmorCount,
                "infantryCount", infantryCount));

        transportRequirements.put("totalVehicleCount",
                (superHeavyVehicleCount + heavyVehicleCount + lightVehicleCount));
        transportRequirements.put("passengerCount", passengerCount);

        // Log the calculated transport requirements
        logger.debug("Transportation Requirements = {}",
                transportRequirements.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        // Returns a map with calculated counts for each unit type
        return transportRequirements;
    }
}
