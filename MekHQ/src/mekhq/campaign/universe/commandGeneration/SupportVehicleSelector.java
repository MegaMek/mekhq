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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.annotations.Nullable;
import megamek.common.bays.CargoBay;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Picks the actual vehicle a support capability fields, by rolling the force generator rather than naming one.
 *
 * <p>Every command used to receive the same four models whatever it was and whenever it played: a 50-ton
 * BattleMek Recovery Vehicle, a Flatbed Truck, a MASH Truck (Small) and a Sherpa canteen. Two of those are
 * anachronisms in an early campaign - the Flatbed does not exist until 2580 and the MASH Truck until 2650 - because
 * a name lookup ignores intro dates entirely. Rolling asks the generator what the faction could actually field in
 * that year, which fixes the early eras and gives every other one some variety.</p>
 *
 * <p>Recovery is rolled on the {@link MissionRole#RECOVERY} role added for this work. The others are recognised by
 * the equipment they carry, which cannot drift away from the unit file the way a role can.</p>
 */
public final class SupportVehicleSelector {
    private static final MMLogger LOGGER = MMLogger.create(SupportVehicleSelector.class);

    /**
     * Smallest cargo bay, in tons, that counts as a supply truck. Cargo-tagged vehicles run from a 0.13-ton Skoda
     * Growler runabout to a 12.5-ton Burro II, and a convoy of runabouts is not a convoy.
     */
    static final double MINIMUM_CONVOY_CARGO_TONS = 2.0;

    /** How hard the role filter is applied when rolling; matches the strictness the cargo hull roller uses. */
    private static final int ROLE_STRICTNESS = 2;

    /** How long recognising an owned vehicle waits for the force generator to finish loading before giving up. */
    private static final long GENERATOR_WAIT_MILLIS = 30_000;

    /** Pause between checks while waiting for the force generator. */
    private static final long GENERATOR_POLL_MILLIS = 50;

    /**
     * The order an owned vehicle is tested in, so a vehicle that could do two jobs counts once. Equipment is the
     * surest sign and goes first: a MASH truck may also carry the support role and a cargo bay, and is still a MASH
     * truck. Security is infantry and is never recognised this way.
     */
    private static final List<SupportCapability> RECOGNITION_ORDER = List.of(SupportCapability.MEDICAL,
          SupportCapability.COMMISSARY, SupportCapability.SALVAGE, SupportCapability.LOGISTICS);

    /**
     * Reads a model's mission roles from the force generator. Replaceable so tests, which do not stage the generator
     * data, can say what roles a unit has.
     */
    static Function<String, Set<MissionRole>> roleLookup = SupportVehicleSelector::generatorRoles;

    private SupportVehicleSelector() {
    }

    /**
     * A vehicle the generator offered, with the capacity that decides whether it suits the capability.
     *
     * @param unitName      the unit's name as the cache knows it
     * @param summary       the cache entry, used to build the entity; {@code null} for a vehicle the campaign
     *                      already owns, which is only ever counted
     * @param cargoTons     cargo bay capacity in tons
     * @param mashTheatres  MASH theatres carried
     * @param fieldKitchens field kitchens carried
     * @param trailer       whether the vehicle is a trailer, which cannot move without a tractor
     */
    public record Candidate(String unitName, @Nullable MekSummary summary, double cargoTons, int mashTheatres,
                            int fieldKitchens, boolean trailer) {
    }

    /**
     * Rolls one vehicle for {@code capability}, or {@code null} when the faction could field none in this year.
     *
     * <p>A {@code null} is a real answer, not a failure to paper over. A command in 2398 predates the BattleMek
     * itself, so it has no recovery vehicle to be given, and naming one anyway is the anachronism this replaces.</p>
     *
     * @param capability  the capability being fielded
     * @param factionCode the faction the command is generated for
     * @param year        the campaign year
     * @param rating      the command's equipment rating, or {@code null} for any
     *
     * @return the chosen vehicle, or {@code null} when nothing suitable exists
     */
    public static @Nullable Candidate roll(SupportCapability capability, @Nullable String factionCode, int year,
          @Nullable String rating) {
        UnitTable table = tableFor(capability, factionCode, year, rating);
        if (table == null) {
            return null;
        }
        // The table's own weighted roll, narrowed by the capability. Rolling it rather than picking from a
        // collected list keeps the availability weighting intact, which is what makes the common recovery
        // vehicles common: they outweigh the rest of the table roughly three to one.
        MekSummary rolled = table.generateUnit(summary -> suits(capability, describe(summary)));
        if (rolled == null) {
            // Worth separating: a table with entries that none of them suit is a capability filter that is too
            // tight, while an empty table is a faction that could field nothing of the kind in that year.
            LOGGER.info("[CompanyGen][SupportUnits] {}: none of the {} vehicle(s) on the {} table for {} in {} "
                        + "suit it; none will be fielded",
                  capability, table.getNumEntries(), rolesFor(capability), factionCode, year);
            return null;
        }
        Candidate chosen = describe(rolled);
        LOGGER.info("[CompanyGen][SupportUnits] {}: rolled '{}' for {} in {} (cargo {}t, {} MASH theatre(s), "
                    + "{} field kitchen(s))",
              capability, chosen.unitName(), factionCode, year, chosen.cargoTons(), chosen.mashTheatres(),
              chosen.fieldKitchens());
        return chosen;
    }

    /**
     * Every vehicle the faction could field for {@code capability} in {@code year}, in table order.
     *
     * @param capability  the capability being fielded
     * @param factionCode the faction the command is generated for
     * @param year        the campaign year
     * @param rating      the command's equipment rating, or {@code null} for any
     *
     * @return the suitable candidates, empty when there are none
     */
    static List<Candidate> candidatesFor(SupportCapability capability, @Nullable String factionCode, int year,
          @Nullable String rating) {
        UnitTable table = tableFor(capability, factionCode, year, rating);
        List<Candidate> candidates = new ArrayList<>();
        if (table == null) {
            return candidates;
        }
        for (int index = 0; index < table.getNumEntries(); index++) {
            MekSummary summary = table.getMekSummary(index);
            if (summary == null) {
                // Salvage and isorla entries point at another faction's table rather than naming a unit.
                continue;
            }
            Candidate candidate = describe(summary);
            if (suits(capability, candidate)) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    /**
     * The capacity a typical vehicle of this kind carries, used to size a capability before anything is rolled.
     *
     * <p>Sizing runs before selection - the mechanic count needs a vehicle count first - so it cannot ask the
     * vehicle that will actually be fielded. The median of what the faction could field is used instead, which is
     * steadier than the mean when one outlier like a twelve ton trailer sits in a table of six ton trucks.</p>
     *
     * @param capability  the capability being sized
     * @param factionCode the faction the command is generated for
     * @param year        the campaign year
     * @param rating      the command's equipment rating, or {@code null} for any
     * @param measure     the capacity to read off each candidate
     *
     * @return the median capacity, or {@code 0} when the faction could field none
     */
    public static double typicalCapacity(SupportCapability capability, @Nullable String factionCode, int year,
          @Nullable String rating, ToDoubleFunction<Candidate> measure) {
        List<Candidate> candidates = candidatesFor(capability, factionCode, year, rating);
        if (candidates.isEmpty()) {
            return 0;
        }
        List<Double> capacities = new ArrayList<>();
        for (Candidate candidate : candidates) {
            capacities.add(measure.applyAsDouble(candidate));
        }
        Collections.sort(capacities);
        return capacities.get(capacities.size() / 2);
    }

    /** The generator table to roll on, filtered by mission role where the capability has one. */
    private static @Nullable UnitTable tableFor(SupportCapability capability, @Nullable String factionCode, int year,
          @Nullable String rating) {
        FactionRecord factionRecord = (factionCode == null)
              ? null
              : RATGenerator.getInstance().getFaction(factionCode);
        EnumSet<MissionRole> roles = rolesFor(capability);
        try {
            return UnitTable.findTable(factionRecord, UnitType.TANK, year, rating, null, ModelRecord.NETWORK_NONE,
                  EnumSet.noneOf(EntityMovementMode.class), roles, ROLE_STRICTNESS);
        } catch (Exception exception) {
            LOGGER.warn(exception, "[CompanyGen][SupportUnits] {}: could not build a table for {} in {}",
                  capability, factionCode, year);
            return null;
        }
    }

    /** The mission roles that narrow the table for this capability. */
    private static EnumSet<MissionRole> rolesFor(SupportCapability capability) {
        return switch (capability) {
            case SALVAGE -> EnumSet.of(MissionRole.RECOVERY);
            case LOGISTICS -> EnumSet.of(MissionRole.CARGO, MissionRole.SUPPORT);
            // Medical and commissary vehicles are recognised by the equipment they carry, but the table still
            // has to be asked for SUPPORT. Asking with no role at all builds a general combat table, and
            // adjustAvailabilityByRole drops anything carrying the SUPPORT role from one of those outright - so a
            // wider ask returns fewer vehicles, not more, and no MASH truck or canteen survives it.
            default -> EnumSet.of(MissionRole.SUPPORT);
        };
    }

    /** Whether the vehicle can actually do the capability's job. */
    static boolean suits(SupportCapability capability, Candidate candidate) {
        // A trailer has no engine. Fielding one without a tractor to pull it gives a command a canteen or a cargo
        // hold that cannot leave the depot, which is what a Clan command got when it was handed a Hector Road
        // Train trailer module. Pairing trailers with tractors is a separate piece of work.
        if (candidate.trailer()) {
            return false;
        }
        return switch (capability) {
            // The role has already filtered these, and a recovery vehicle need carry no equipment at all.
            case SALVAGE -> true;
            case LOGISTICS -> candidate.cargoTons() >= MINIMUM_CONVOY_CARGO_TONS;
            case MEDICAL -> candidate.mashTheatres() > 0;
            case COMMISSARY -> candidate.fieldKitchens() > 0;
            // Infantry, not a rolled vehicle.
            case SECURITY -> false;
        };
    }

    /**
     * Which support capability a vehicle the campaign already owns provides, judged by the same rules a rolled
     * vehicle must pass, so what is counted as owned matches what would otherwise be built.
     *
     * <p>Where the vehicle is filed plays no part. A recovery vehicle the player bought and left outside the TOE
     * recovers wrecks just the same, and counting by formation is what had a converted campaign build sixteen
     * recovery vehicles while four of its own sat in the hangar.</p>
     *
     * <p>Medical and commissary vehicles are recognised by the MASH theatre or field kitchen they carry. Recovery
     * vehicles and convoy trucks carry nothing that sets them apart from a combat vehicle, so they are recognised by
     * the mission role the force generator gives the model, the same role they are rolled on.</p>
     *
     * @param entity the owned vehicle; {@code null} provides nothing
     *
     * @return the capability it provides, or {@code null} when it provides none
     */
    public static @Nullable SupportCapability capabilityOf(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }
        Candidate candidate = describe(entity);
        // Looked up only when needed: equipment settles medical and commissary without asking the generator.
        Set<MissionRole> roles = null;
        for (SupportCapability capability : RECOGNITION_ORDER) {
            if (!suits(capability, candidate)) {
                continue;
            }
            if (isRecognisedByEquipment(capability)) {
                return capability;
            }
            if (roles == null) {
                roles = roleLookup.apply(candidate.unitName());
            }
            if (!Collections.disjoint(roles, rolesFor(capability))) {
                return capability;
            }
        }
        return null;
    }

    /** Whether the capability is recognised by the equipment it carries rather than by its mission role. */
    private static boolean isRecognisedByEquipment(SupportCapability capability) {
        return switch (capability) {
            case MEDICAL, COMMISSARY -> true;
            default -> false;
        };
    }

    /**
     * Reads a model's mission roles from the force generator, waiting a while for it to finish loading.
     *
     * <p>The generator loads on a thread of its own, and a campaign can be loaded before it finishes. Rather than
     * read an empty table as "no roles", the lookup waits, as {@code RATGeneratorConnector} does. If the generator
     * is still not ready the vehicle goes unrecognised, which is logged: it is then built around, as before this
     * check existed, rather than counted.</p>
     *
     * @param unitName the model's name as the generator keys it
     *
     * @return the model's roles, empty when the model is unknown or the generator is not ready
     */
    private static Set<MissionRole> generatorRoles(String unitName) {
        RATGenerator generator = RATGenerator.getInstance();
        long deadline = System.currentTimeMillis() + GENERATOR_WAIT_MILLIS;
        while (!generator.isInitialized() && (System.currentTimeMillis() < deadline)) {
            try {
                Thread.sleep(GENERATOR_POLL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!generator.isInitialized()) {
            LOGGER.warn("[CompanyGen][SupportUnits] the force generator is not loaded, so '{}' cannot be recognised"
                              + " by its mission role", unitName);
            return Set.of();
        }
        ModelRecord model = generator.getModelRecord(unitName);
        return (model == null) ? Set.of() : model.getRoles();
    }

    /**
     * Reads the capacities that decide suitability off a vehicle that already exists. The candidate carries no cache
     * entry, because an owned vehicle is counted, never built from.
     */
    private static Candidate describe(Entity entity) {
        return new Candidate(entity.getShortNameRaw(), null, cargoTons(entity),
              countEquipment(entity, MiscType.F_MASH), countEquipment(entity, MiscType.F_FIELD_KITCHEN),
              entity.isTrailer());
    }

    /** Cargo bay capacity in tons, read the way the unit cache reads it, but full rather than what is unused. */
    private static double cargoTons(Entity entity) {
        double tons = 0;
        for (var transporter : entity.getTransports()) {
            if (transporter instanceof CargoBay cargoBay) {
                tons += cargoBay.getCapacity();
            }
        }
        return tons;
    }

    /** Reads the capacities that decide suitability off the unit. */
    private static Candidate describe(MekSummary summary) {
        int mashTheatres = 0;
        int fieldKitchens = 0;
        boolean trailer = false;
        try {
            Entity entity = summary.loadEntity();
            if (entity != null) {
                mashTheatres = countEquipment(entity, MiscType.F_MASH);
                fieldKitchens = countEquipment(entity, MiscType.F_FIELD_KITCHEN);
                trailer = entity.isTrailer();
            }
        } catch (Exception exception) {
            LOGGER.warn(exception, "[CompanyGen][SupportUnits] could not read equipment from '{}'",
                  summary.getName());
        }
        return new Candidate(summary.getName(), summary, summary.getCargoBayUnits(), mashTheatres, fieldKitchens,
              trailer);
    }

    /** Counts the items on {@code entity} carrying {@code flag}. */
    private static int countEquipment(Entity entity, MiscTypeFlag flag) {
        int items = 0;
        for (var mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(flag)) {
                items++;
            }
        }
        return items;
    }
}
