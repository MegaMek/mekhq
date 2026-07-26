/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.TransportCalculator;
import megamek.client.ratgenerator.TransportCalculator.CargoCapacity;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;

/**
 * Generates the cargo DropShips a newly built command needs to haul its own supplies, and crews them.
 *
 * <p>Runs at the end of generation rather than during it. The cargo requirement is the tonnage sitting
 * in the campaign warehouse - spares, ammunition and armour - which only exists once the force has been
 * materialized, so it cannot be known while the force is still being rolled. Units themselves are not
 * cargo: they ride in unit bays, which the Force Generator's DropShip Percentage already provisions.</p>
 *
 * <p>Solid and liquid holds are counted separately and never substituted for one another - a hull full
 * of liquid tankage is no help to a command that needs somewhere to stow spare parts. The requirement
 * MekHQ can actually measure is solid, so that is what ships are selected against; whatever liquid
 * capacity they happen to bring is reported rather than used to satisfy the target.</p>
 *
 * <p>The ships are ordinary units: they are added to the hangar, crewed, and priced like anything else
 * the build produces, so a player who asks to carry all their spares pays for the hulls that do it.</p>
 */
public final class CargoShipGenerator {

    private static final MMLogger LOGGER = MMLogger.create(CargoShipGenerator.class);

    /** Stop after this many hulls, so a pathological requirement cannot generate a fleet forever. */
    private static final int MAX_SHIPS = 50;

    /**
     * Size band a hull's hold must fall in to be considered for the tonnage still outstanding,
     * as a fraction of that tonnage. The lower bound keeps a big lift from being met by a swarm
     * of nearly-empty hulls; the upper bound keeps a small one from buying a leviathan.
     */
    private static final double BAND_LOWER_FRACTION = 0.25;
    private static final double BAND_UPPER_FRACTION = 1.5;

    /**
     * What a generation run produced.
     *
     * @param ships           the cargo DropShips added to the campaign, in creation order
     * @param requiredTons    the solid cargo tonnage the run was asked to cover
     * @param providedTons    the solid cargo capacity the new ships actually added
     * @param liquidTons      liquid capacity the new ships brought along, not counted toward the target
     * @param shortfallTons   solid tonnage still uncovered, when no suitable hull could be found
     */
    public record Result(List<Unit> ships, double requiredTons, double providedTons,
                         double liquidTons, double shortfallTons) {

        public static Result none() {
            return new Result(List.of(), 0, 0, 0, 0);
        }
    }

    private CargoShipGenerator() {
        // utility class
    }

    /**
     * Adds cargo DropShips until the command can haul {@code cargoPct} percent of its warehouse
     * tonnage, counting the cargo holds it already owns.
     *
     * @param campaign  the campaign whose warehouse sets the requirement and whose hangar receives the
     *                  ships
     * @param factionCode the faction to draw hulls from, or {@code null} to use the campaign's
     * @param year      the year to draw hulls from
     * @param rating    the equipment rating to draw hulls at, or {@code null} for any
     * @param cargoPct  percentage of the requirement to provision for; 100 covers it exactly, above
     *                  100 adds headroom, 0 or less generates nothing
     *
     * @return what was generated; never {@code null}
     */
    public static Result generate(Campaign campaign, @Nullable String factionCode, int year,
          @Nullable String rating, double cargoPct) {
        if (cargoPct <= 0) {
            LOGGER.debug("[CompanyGen][Cargo] cargo percentage is {}; generating no cargo ships", cargoPct);
            return Result.none();
        }

        CargoStatistics cargoStats = campaign.getCargoStatistics();
        // Spare parts plus the hulls of anything mothballed - everything that has to be stowed rather
        // than driven aboard. Active units are excluded on purpose: they occupy unit bays.
        double warehouseTons = cargoStats.getCargoTonnage(false) + cargoStats.getCargoTonnage(false, true);
        double requiredTons = warehouseTons * (cargoPct / 100.0);
        double existingCapacity = cargoStats.getTotalCombinedCargoCapacity();
        double shortfall = requiredTons - existingCapacity;

        LOGGER.info("[CompanyGen][Cargo] warehouse={} tons, target={}% -> required={} tons;"
                    + " existing solid capacity={} tons; shortfall={} tons",
              round(warehouseTons), cargoPct, round(requiredTons), round(existingCapacity), round(shortfall));

        if (shortfall <= 0) {
            LOGGER.info("[CompanyGen][Cargo] the command can already haul its cargo; no ships needed");
            return new Result(List.of(), requiredTons, 0, 0, 0);
        }

        UnitTable table = cargoDropshipTable(factionCode, year, rating);
        if (table == null || table.getNumEntries() == 0) {
            LOGGER.warn("[CompanyGen][Cargo] no DropShip table for faction={} year={} rating={};"
                        + " {} tons of cargo capacity will be missing",
                  factionCode, year, rating, round(shortfall));
            return new Result(List.of(), requiredTons, 0, 0, shortfall);
        }

        List<Candidate> candidates = cargoCapableHulls(table);
        if (candidates.isEmpty()) {
            LOGGER.warn("[CompanyGen][Cargo] the DropShip table holds no design with solid cargo holds;"
                        + " {} tons will be missing", round(shortfall));
            return new Result(List.of(), requiredTons, 0, 0, shortfall);
        }
        LOGGER.debug("[CompanyGen][Cargo] {} cargo-capable hull(s) available, {} to {} solid tons",
              candidates.size(), round(candidates.getFirst().solidTons()),
              round(candidates.getLast().solidTons()));

        List<Unit> ships = new ArrayList<>();
        double provided = 0;
        double liquidProvided = 0;
        double remaining = shortfall;
        while ((remaining > 0) && (ships.size() < MAX_SHIPS)) {
            Candidate chosen = pickHull(candidates, remaining);
            Unit ship = addCrewedShip(campaign, chosen.hull());
            if (ship == null) {
                // Loading failed and was logged. Drop the hull so the loop cannot spin on it, and try
                // again with what is left.
                candidates.remove(chosen);
                if (candidates.isEmpty()) {
                    break;
                }
                continue;
            }
            ships.add(ship);
            provided += chosen.solidTons();
            liquidProvided += chosen.liquidTons();
            remaining -= chosen.solidTons();
            LOGGER.debug("[CompanyGen][Cargo] added '{}' (+{} solid, +{} liquid tons); {} tons remaining",
                  chosen.hull().getName(), round(chosen.solidTons()), round(chosen.liquidTons()),
                  round(remaining));
        }

        if (ships.size() >= MAX_SHIPS) {
            LOGGER.warn("[CompanyGen][Cargo] stopped at the {}-ship cap with {} tons still uncovered",
                  MAX_SHIPS, round(Math.max(0, remaining)));
        }
        LOGGER.info("[CompanyGen][Cargo] generated {} cargo ship(s): +{} solid tons, +{} liquid tons,"
                    + " {} tons short",
              ships.size(), round(provided), round(liquidProvided), round(Math.max(0, remaining)));
        return new Result(ships, requiredTons, provided, liquidProvided, Math.max(0, remaining));
    }

    /**
     * A hull the run could buy, with the three numbers selection needs.
     *
     * @param hull               the design
     * @param solidTons          its dry cargo capacity
     * @param liquidTons         its liquid cargo capacity, carried along but never used to size the run
     * @param availabilityWeight the design's weight in the faction/era availability table, preserved so
     *                           the ruleset data still decides which freighter is picked
     */
    record Candidate(MekSummary hull, double solidTons, double liquidTons,
                     int availabilityWeight) {}

    /**
     * Every cargo-capable hull in the table, smallest hold first.
     *
     * <p>Eligibility is measured from the unit file rather than taken from the {@code CARGO} mission
     * role. The role is a hard filter that excludes anything untagged, and tagging covers only a
     * fraction of the DropShip designs, so filtering on it would hide most of the hulls that can
     * actually do the job.</p>
     */
    private static List<Candidate> cargoCapableHulls(UnitTable table) {
        List<Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < table.getNumEntries(); index++) {
            MekSummary hull = table.getMekSummary(index);
            if (hull == null) {
                // Salvage and other non-unit entries have no summary.
                continue;
            }
            CargoCapacity capacity = TransportCalculator.cargoCapacity(hull);
            if (capacity.solidTons() <= 0) {
                continue;
            }
            candidates.add(new Candidate(hull, capacity.solidTons(), capacity.liquidTons(),
                  Math.max(1, table.getEntryWeight(index))));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::solidTons));
        return candidates;
    }

    /**
     * Chooses the next hull to buy for a lift with {@code remaining} tons still to cover.
     *
     * <p>Candidates are narrowed to those whose hold is a sensible size for the job, and the choice
     * among them is then made by availability weight. That ordering matters: constraining the size
     * class first stops a large lift being met by a swarm of hulls that each carry a little, while
     * drawing by weight second keeps the ruleset's availability data in charge of <em>which</em>
     * freighter turns up.</p>
     *
     * <p>When nothing sits in the band the run falls back deliberately rather than giving up: if any
     * hull can cover what is left, the smallest such hull is taken so the last ship of a lift is not
     * wildly oversized; otherwise the largest available hull is taken to chip away at a requirement
     * bigger than any single design.</p>
     */
    static Candidate pickHull(List<Candidate> candidates, double remaining) {
        double lowerBound = remaining * BAND_LOWER_FRACTION;
        double upperBound = remaining * BAND_UPPER_FRACTION;

        List<Candidate> banded = candidates.stream()
              .filter(candidate -> (candidate.solidTons() >= lowerBound)
                    && (candidate.solidTons() <= upperBound))
              .toList();
        if (!banded.isEmpty()) {
            return drawByAvailability(banded);
        }

        // Nothing well-sized. Prefer the least overshoot among hulls that finish the job.
        Candidate smallestThatCovers = null;
        for (Candidate candidate : candidates) {
            if (candidate.solidTons() >= remaining) {
                smallestThatCovers = candidate;
                break; // sorted ascending, so the first match is the smallest
            }
        }
        if (smallestThatCovers != null) {
            return smallestThatCovers;
        }
        // Every hull is smaller than what is left: take the biggest and go round again.
        return candidates.getLast();
    }

    /** Weighted random draw honouring the availability table's weights. */
    private static Candidate drawByAvailability(List<Candidate> candidates) {
        int totalWeight = 0;
        for (Candidate candidate : candidates) {
            totalWeight += candidate.availabilityWeight();
        }
        int roll = Compute.randomInt(totalWeight);
        for (Candidate candidate : candidates) {
            roll -= candidate.availabilityWeight();
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.getLast();
    }

    /** Builds the DropShip availability table the hulls are drawn from, or {@code null} if unavailable. */
    private static @Nullable UnitTable cargoDropshipTable(@Nullable String factionCode, int year,
          @Nullable String rating) {
        FactionRecord factionRecord = (factionCode == null)
              ? null
              : RATGenerator.getInstance().getFaction(factionCode);
        if (factionRecord == null) {
            LOGGER.debug("[CompanyGen][Cargo] no faction record for '{}'; drawing from the general table",
                  factionCode);
        }
        try {
            return UnitTable.findTable(factionRecord, UnitType.DROPSHIP, year, rating, null,
                  ModelRecord.NETWORK_NONE, EnumSet.noneOf(EntityMovementMode.class),
                  EnumSet.noneOf(MissionRole.class), 0);
        } catch (Exception exception) {
            LOGGER.warn(exception, "[CompanyGen][Cargo] could not build a DropShip table for faction={}"
                        + " year={} rating={}", factionCode, year, rating);
            return null;
        }
    }

    /**
     * Adds one hull to the hangar with a crew. Mirrors how support units are created so cargo ships are
     * ordinary campaign units - owned, crewed and paid for.
     *
     * @return the new unit, or {@code null} when the design could not be loaded
     */
    private static @Nullable Unit addCrewedShip(Campaign campaign, MekSummary hull) {
        try {
            PartQuality quality = campaign.getCampaignOptions().isUseRandomUnitQualities()
                  ? UnitOrder.getRandomUnitQuality(0)
                  : PartQuality.QUALITY_D;
            // allowNewPilots = true: the ship arrives crewed, like every other generated unit.
            return campaign.addNewUnit(hull.loadEntity(), true, 0, quality);
        } catch (Exception exception) {
            LOGGER.error(exception, "[CompanyGen][Cargo] could not load cargo DropShip '{}' from {}",
                  hull.getName(), hull.getSourceFile());
            return null;
        }
    }

    private static double round(double tons) {
        return Math.round(tons * 10.0) / 10.0;
    }
}
