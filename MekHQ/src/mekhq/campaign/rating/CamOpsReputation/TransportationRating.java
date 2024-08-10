package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.InfantryBay;
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
     * @param campaign The campaign for which to calculate the transportation rating.
     * @return A list containing maps of transportation capacities and requirements, including the transportation rating.
     */
    public static List<Map<String, Integer>> calculateTransportationRating(Campaign campaign) {
        // Calculate transportation capacities and requirements for the campaign
        Map<String, Integer> transportationCapacities = calculateTransportationCapacities(campaign);
        Map<String, Integer> transportationRequirements = calculateTransportRequirements(campaign);

        int transportationRating = 0;

        // For each type of entity (denoted here as "SmallCraft", "AeroSpaceFighter", etc.)
        // calculate the rating adjustment based on the capacity and requirements

        // Small Craft
        int capacity = transportationCapacities.get("SmallCraftBay"); // Get the capacity for the entity
        int requirements = transportationRequirements.get("SmallCraft"); // Get the requirements for the entity

        // Add the rating adjustment to the total; We do this for each following entity
        transportationRating += calculateRating(capacity, requirements);

        // Calculate spare capacity if any.
        // Some entity types can use spare bays not used by other entities
        int spareCapacity = Math.max(0, capacity - requirements);

        // The above logic is repeated for the remaining entities

        // ASF
        capacity = transportationCapacities.get("ASFBay");
        requirements = Math.max(1, transportationRequirements.get("AeroSpaceFighter") - spareCapacity);

        transportationRating += calculateRating(capacity, requirements);

        // Mechs
        capacity = transportationCapacities.get("MechBay");
        requirements = transportationRequirements.get("Mech");

        transportationRating += calculateRating(capacity, requirements);

        // Super Heavy Vehicles
        capacity = transportationCapacities.get("SuperHeavyVehicleBay");
        requirements = transportationRequirements.get("SuperHeavyVehicle");

        transportationRating += calculateRating(capacity, requirements);

        spareCapacity = Math.max(0, capacity - requirements);

        // Heavy Vehicles
        capacity = transportationCapacities.get("HeavyVehicleBay");
        requirements = Math.max(0, transportationRequirements.get("HeavyVehicle") - spareCapacity);

        transportationRating += calculateRating(capacity, requirements);

        // as this spare capacity can also be used by light vehicles,
        // we need to track the remaining spare capacity
        spareCapacity -= Math.max(0, transportationRequirements.get("HeavyVehicle"));
        spareCapacity += Math.max(0, capacity - requirements);

        // Light Vehicles
        capacity = transportationCapacities.get("LightVehicleBay");
        requirements = Math.max(0, transportationRequirements.get("LightVehicle") - spareCapacity);

        transportationRating += calculateRating(capacity, requirements);

        // ProtoMechs
        capacity = transportationCapacities.get("ProtomechBay");
        requirements = transportationRequirements.get("Protomech");

        transportationRating += calculateRating(capacity, requirements);

        // Battle Armor
        capacity = transportationCapacities.get("BattleArmorBay");
        requirements = transportationRequirements.get("BattleArmor");

        transportationRating += calculateRating(capacity, requirements);

        // Infantry
        capacity = transportationCapacities.get("InfantryBay");
        requirements = transportationRequirements.get("Infantry");

        transportationRating += calculateRating(capacity, requirements);

        // Support Personnel
        capacity = transportationCapacities.get("passengerCapacity");
        requirements = transportationRequirements.get("passengerCount");

        if (capacity >= requirements) {
            transportationRating += 3;
        } else if (transportationRating > 0) {
            transportationRating -= 3;
        }

        // JumpShip & WarShip Presence
        if (transportationRequirements.get("hasJumpShipOrWarShip") > 0) {
            transportationRating += 10;
        }

        // Docking Collar Requirements
        if (transportationCapacities.get("dockingCollars") >= transportationRequirements.get("Dropship")) {
            transportationRating += 5;
        }

        // Finally, the calculated transportation rating is added to the map of transportation capacities
        transportationCapacities.put("total", transportationRating);
        logger.info("Transportation Rating = {}", transportationRating);

        // Return list of capacities and requirements
        return List.of(transportationCapacities, transportationRequirements);
    }

    /**
     * Calculates the transportation rating adjustment based on capacity and requirements.
     *
     * @param capacity     the transport bay capacity
     * @param requirements the transport bay usage
     * @return the rating calculated based on the capacity and requirements
     */
    private static int calculateRating(int capacity, int requirements) {
        if (requirements > 0) {
            double usagePercentage = (double) capacity / requirements;

            if (usagePercentage < 0.0) {
                return -5;
            } else if (usagePercentage > 2.0) {
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
     * @param campaign the campaign to retrieve the transportation bays and passenger capacity for
     * @return a map containing the transportation bays and passenger capacity, where each key represents
     *         a bay type and its corresponding value represents the count or capacity
     */
    private static Map<String, Integer> calculateTransportationCapacities(Campaign campaign) {
        // Declaring and initializing a map to store the bay counts
        Map<String, Integer> transportationCapacities = new HashMap<>();

        int uncrewedUnits = 0;
        int passengerCapacity = 0;
        int dockingCollars = 0;
        int hasJumpShipOrWarShip = 0;


        // Iterating through each unit in the campaign
        for (Unit unit : campaign.getUnits()) {
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
                String key = bay.getClass().getSimpleName();

                // Calculate the capacity of each bay and then store it in the map
                if (bay instanceof InfantryBay) {
                    int additionalCapacity = (int) Math.floor(bay.getCapacity() / ((InfantryBay) bay).getPlatoonType().getWeight());
                    transportationCapacities.put(key, transportationCapacities.getOrDefault(key, 0) + additionalCapacity);
                } else {
                    transportationCapacities.put(key, transportationCapacities.getOrDefault(key, 0) + (int)bay.getCapacity());
                }

                passengerCapacity += bay.getPersonnel(entity.isClan());
            }
        }

        // add the supplemental information to the map
        transportationCapacities.put("passengerCapacity", passengerCapacity);
        transportationCapacities.put("hasJumpShipOrWarShip", hasJumpShipOrWarShip);
        transportationCapacities.put("dockingCollars", dockingCollars);
        transportationCapacities.put("uncrewedUnits", uncrewedUnits);

        // log the stored information to aid debugging
        logger.info("Transportation Capacities = {}",
                transportationCapacities.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        // return the map
        return transportationCapacities;
    }

    /**
     * Calculates the transport requirements for the given campaign.
     *
     * @param campaign the campaign for which to calculate the transport requirements
     * @return a map containing the count for each type of entity in the campaign
     */
    private static Map<String, Integer> calculateTransportRequirements(Campaign campaign) {
        Map<String, Integer> transportRequirements = new HashMap<>();

        for (Unit unit : campaign.getUnits()) {
            // get the key for the type of unit
            String entityKey = getKey(unit.getEntity());

            // Increase the count for this kind of entity
            transportRequirements.put(entityKey, transportRequirements.getOrDefault(entityKey, 0) + 1);
        }

        int nonUnitCrewCount = (int) campaign.getPersonnel().stream()
                .filter(person -> !person.getStatus().isAbsent() && !person.getStatus().isDepartedUnit())
                .filter(person -> person.getUnit() == null)
                .count();

        transportRequirements.put("passengerCount", nonUnitCrewCount);

        // Logging the transport requirements
        logger.info("Transportation Requirements = {}",
                transportRequirements.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        return transportRequirements;
    }

    /**
     * Returns the key for the given unit.
     * If the unit represents a vehicle, the key is determined based on the weight of the entity.
     * If the unit does not represent a vehicle, the key is determined based on the classname of the entity.
     *
     * @param entity the unit
     * @return the key for the given unit
     */
    private static String getKey(Entity entity) {
        // Handle vehicle weights separately
        if (entity.isVehicle()) {
            double weight = entity.getWeight();
            if (weight > 100) {
                return "SuperHeavyVehicle";
            } else if (weight > 50) {
                return "HeavyVehicle";
            } else {
                return "LightVehicle";
            }
        // Otherwise, use the classname of the entity
        } else {
            return entity.getClass().getSimpleName();
        }
    }
}
