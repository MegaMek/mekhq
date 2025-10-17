package mekhq.campaign.mission;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_HEROIC;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.CargoStatistics;

public class TransportCostCalculations {
    private static final MMLogger LOGGER = MMLogger.create(TransportCostCalculations.class);

    // Most costs are listed as per month. There are n days in the average month. Therefore, the cost/day is the base
    // cost divided by n.
    private static final double PER_DAY_DIVIDER = 30.436875;
    // Collar hiring is per week. There are 7 days in a week. Therefore, the cost/day is the base cost divided by 7
    private static final double PER_DAY_WEEK = 7.0;

    private static final double ELITE_CREW_MULTIPLIER = 2.0;
    private static final double VETERAN_CREW_MULTIPLIER = 1.5;
    private static final double OTHER_CREW_MULTIPLIER = 1.0;

    // This value is derived from the Union (2708). We do make some assumptions, however. Namely, we assume that the
    // player is always able to find a DropShip that has the exact bay types they need. Use of this magical DropShip
    // allows us to greatly simplify the amount of processing. It also helps make the logic easier for players to
    // understand.
    private static final int BAYS_PER_DROPSHIP = 14;
    // This value is derived from the Union (2708) (Cargo).
    private static final double CARGO_PER_DROPSHIP = 1874.5;

    // These values are taken from CamOps pg 43.
    private static final double JUMP_SHIP_COLLAR_COST = 100000 / PER_DAY_WEEK; // Collar prices are per week
    private static final double SMALL_CRAFT_OR_SUPER_HEAVY_COST = 100000 / PER_DAY_DIVIDER;
    private static final double MEK_COST = 50000 / PER_DAY_DIVIDER;
    private static final double FIGHTER_COST = 50000 / PER_DAY_DIVIDER;
    private static final double HEAVY_VEHICLE_COST = 50000 / PER_DAY_DIVIDER;
    private static final double LIGHT_VEHICLE_COST = 25000 / PER_DAY_DIVIDER;
    private static final double PLATOON_COST = 25000 / PER_DAY_DIVIDER;
    private static final double BA_SQUAD_COST = 25000 / PER_DAY_DIVIDER;
    private static final double PROTOMEK_COST = 20000 / PER_DAY_DIVIDER;
    private static final double OTHER_UNIT_COST = 50000 / PER_DAY_DIVIDER; // (Unofficial)
    private static final double CARGO_PER_TON_COST = 100000 / 1200.0 / PER_DAY_DIVIDER;

    private final Campaign campaign;
    private final int crewExperienceLevel;
    private double additionalCargoSpaceRequired;
    private double cargoBayCost;
    private double totalAdditionalBaysRequired;
    private double additionalDropShipsRequired;
    private double dockingCollarCost;

    private Money totalCost = Money.zero();

    // The only canon passenger DropShip is the Princess Luxury Liner. However, hiring one using CamOps rules proves
    // unreasonably expensive. Therefore, we're instead assuming that the player can find retrofit DropShips that has
    // passenger 'bays' that roughly equate to 15 passengers per bay. This number was determined by taking the
    // passenger capacity of the Princess Luxury Liner and dividing it by the number of bays in our magical DropShip.
    // We then assume a passenger bay cost is about the same as an Infantry Platoon bay. Presumably with nice
    // accommodations, which is why fewer people can fit.
    private static final double PASSENGERS_PER_BAY = 15;
    // Hiring a Princess for dependents proved to be insanely expensive. So we're instead assuming
    private static final double PASSENGERS_COST = PLATOON_COST;

    public TransportCostCalculations(Campaign campaign, final int crewExperienceLevel) {
        this.campaign = campaign;
        this.crewExperienceLevel = crewExperienceLevel;
    }

    public String getJumpCostString() {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("additionalCargoSpaceRequired: ").append(additionalCargoSpaceRequired).append("<br>");
        sb.append("cargoBayCost: ").append(cargoBayCost).append("<br>");
        sb.append("totalAdditionalBaysRequired: ").append(totalAdditionalBaysRequired).append("<br>");
        sb.append("additionalDropShipsRequired: ").append(additionalDropShipsRequired).append("<br>");
        sb.append("dockingCollarCost: ").append(dockingCollarCost).append("<br>");
        sb.append("totalCost: ").append(totalCost.toAmountString()).append("<br>");
        sb.append("</html>");
        return sb.toString();
    }

    public Money calculateJumpCostForEntireJourney(final int days) {
        Money totalCost = calculateJumpCostForEachDay();
        totalCost = totalCost.multipliedBy(days);
        return totalCost;
    }

    public Money calculateJumpCostForEachDay() {
        calculateCargoRequirements();

        dockingCollarCost = round(additionalDropShipsRequired * JUMP_SHIP_COLLAR_COST);
        totalCost = totalCost.plus(dockingCollarCost);

        double crewExperienceLevelMultiplier = switch (crewExperienceLevel) {
            case EXP_ELITE, EXP_HEROIC, EXP_LEGENDARY -> ELITE_CREW_MULTIPLIER;
            case EXP_VETERAN -> VETERAN_CREW_MULTIPLIER;
            default -> OTHER_CREW_MULTIPLIER;
        };
        totalCost = totalCost.multipliedBy(crewExperienceLevelMultiplier);

        return totalCost;
    }

    private void calculateCargoRequirements() {
        CargoStatistics cargoStatistics = campaign.getCargoStatistics();

        final double totalCargoCapacity = cargoStatistics.getTotalCargoCapacity();
        LOGGER.info("Total cargo capacity: {}", totalCargoCapacity);

        double totalCargoUsage = cargoStatistics.getCargoTonnage(false, false);
        totalCargoUsage += cargoStatistics.getCargoTonnage(false, true);
        LOGGER.info("Total cargo usage: {}", totalCargoUsage);

        additionalCargoSpaceRequired = -min(0, totalCargoCapacity - totalCargoUsage);
        LOGGER.info("Cargo requirements: {}", additionalCargoSpaceRequired);
        cargoBayCost = round(additionalCargoSpaceRequired * CARGO_PER_TON_COST);

        additionalDropShipsRequired += (int) ceil(additionalCargoSpaceRequired / CARGO_PER_DROPSHIP);
        LOGGER.info("Additional drop ships required: {}", additionalDropShipsRequired);

        totalCost = totalCost.plus(cargoBayCost);
    }
}
