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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.Collection;

import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

/**
 * Computes the "100% coverage" support-personnel demand for a campaign's current force composition,
 * following the Campaign Operations support-staff rules.
 *
 * <p>This is a pure function over {@link Campaign} state: no mutation, no side effects, no caching.
 * Returns a {@link SupportDemand} record holding per-role counts that the generation pipeline can
 * scale by user-supplied coverage percentages.</p>
 *
 * <p>Tech-role demand is "1 tech per unit" with the following per-CamOps groupings:</p>
 * <ul>
 *   <li><b>Mek Tech</b>: 1 per BattleMek (LandAirMeks count as Meks via {@code isMek()}) plus
 *       1 per Point of ProtoMeks (5 protomeks = 1 Point).</li>
 *   <li><b>Mechanic</b>: 1 per ground / VTOL / naval vehicle.</li>
 *   <li><b>Aero Tek</b>: 1 per aerospace fighter, conventional fighter, or small craft.
 *       Note: {@code SupportRating.calculateTechnicianRequirements} omits conventional fighters
 *       from this bucket — that's a bug in the rating code. A conventional fighter is maintained by
 *       an aerospace technician ({@code S_TECH_AERO}) at the whole-unit level (see
 *       {@code Person.getSkillForWorkingOn(Unit)}), so we count them here.</li>
 *   <li><b>BA Tech</b>: 1 per BA squad of 5 suits.</li>
 * </ul>
 *
 * <p>DropShips, JumpShips, WarShips, and SpaceStations are intentionally <em>not</em> in any
 * tech bucket — their {@code VESSEL_CREW} personnel carry {@code S_TECH_VESSEL} and act as the
 * ship's own technicians. Those crew slots are filled by the multi-crew assembler when the unit
 * is generated, so adding additional vessel techs here would double-count.</p>
 *
 * <p>Medical and admin demand scale with personnel headcount, projected forward to include the
 * tech staff this calculator's own output will produce. Admin uses a {@code 1 per 20} divisor for
 * standard factions and {@code 1 per 10} for pirate or mercenary factions
 * (per {@code SupportRating.calculateAdministratorRequirements}). Doctor demand uses the
 * campaign-options {@code maximumPatients} value as the doctor-to-patient ratio.</p>
 */
public final class SupportPersonnelCalculator {

    private static final MMLogger LOGGER = MMLogger.create(SupportPersonnelCalculator.class);

    /** Admin / 20 personnel for standard factions. */
    private static final int ADMIN_DIVISOR_STANDARD = 20;
    /** Admin / 10 personnel for pirate / mercenary factions. */
    private static final int ADMIN_DIVISOR_IRREGULAR = 10;
    /** Number of ProtoMeks in a Point (CamOps p.84). */
    private static final int PROTOMEK_POINT_SIZE = 5;
    /** Number of BA suits in a standard squad (CamOps p.220). */
    private static final int BA_SQUAD_SIZE = 5;

    private SupportPersonnelCalculator() {
        // utility class
    }

    /**
     * Per-role 100%-coverage demand snapshot. All counts are baseline (= 100% coverage); the
     * caller multiplies by the user's per-role coverage percentage via {@link #applyPercent(int,int)}
     * to get the actual generation count.
     */
    public record SupportDemand(
          int mekTechsNeeded,
          int mechanicsNeeded,
          int aeroTeksNeeded,
          int baTechsNeeded,
          int doctorsNeeded,
          int administratorsNeeded
    ) {
        /** Sum of the four tech-role baselines, useful for astech-pool sizing. */
        public int totalTechsNeeded() {
            return mekTechsNeeded + mechanicsNeeded + aeroTeksNeeded + baTechsNeeded;
        }
    }

    /**
     * Computes the per-role demand snapshot from {@code campaign}'s current force composition and
     * personnel count.
     *
     * @param campaign the campaign to read units / personnel / options / faction from; if
     *                 {@code null}, returns an all-zero demand
     * @return a non-{@code null} {@link SupportDemand}
     */
    public static SupportDemand compute(Campaign campaign) {
        return compute(campaign, 0);
    }

    /**
     * Computes the per-role demand snapshot from {@code campaign}'s current force composition, plus the vehicles
     * the support stage is still going to add. The stage sizes its personnel before it generates its own
     * vehicles - flatbed trucks, canteens, recovery and MASH vehicles - and those need mechanics like any other
     * vehicle, so the caller says how many are coming.
     *
     * @param campaign            the campaign to read units / personnel / options / faction from; if
     *                            {@code null}, returns an all-zero demand
     * @param vehiclesStillToCome vehicles not yet in the hangar that the caller is about to generate
     *
     * @return a non-{@code null} {@link SupportDemand}
     */
    public static SupportDemand compute(Campaign campaign, int vehiclesStillToCome) {
        if (campaign == null) {
            return new SupportDemand(0, 0, 0, 0, 0, 0);
        }
        UnitTally tally = new UnitTally();
        Collection<Unit> units = campaign.getActiveUnits();
        if (units != null) {
            for (Unit unit : units) {
                if ((unit == null) || unit.isMothballed() || (unit.getEntity() == null)) {
                    continue;
                }
                tally.count(unit.getEntity(), unit.getFullCrewSize());
            }
        }
        int basePersonnel = campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false).size();
        return demandFor(campaign, tally, vehiclesStillToCome, basePersonnel);
    }

    /**
     * Estimates the demand a command will have once units that do not exist yet have joined it: the campaign's own
     * units and people as {@link #compute(Campaign, int)} counts them, plus the planned units and the crews they will
     * bring. Used to preview a command before it is built, when its people and units are still only a rolled
     * design.
     *
     * @param campaign            the campaign the command is being built into; if {@code null}, returns an all-zero
     *                            demand
     * @param plannedEntities     the units the build will add, such as the included units of a rolled design
     * @param vehiclesStillToCome support vehicles the build will add on top of those
     *
     * @return a non-{@code null} {@link SupportDemand}
     */
    public static SupportDemand estimate(Campaign campaign, Collection<Entity> plannedEntities,
          int vehiclesStillToCome) {
        if (campaign == null) {
            return new SupportDemand(0, 0, 0, 0, 0, 0);
        }
        UnitTally tally = new UnitTally();
        Collection<Unit> units = campaign.getActiveUnits();
        if (units != null) {
            for (Unit unit : units) {
                if ((unit == null) || unit.isMothballed() || (unit.getEntity() == null)) {
                    continue;
                }
                tally.count(unit.getEntity(), unit.getFullCrewSize());
            }
        }
        int plannedCrew = 0;
        for (Entity entity : plannedEntities) {
            if (entity == null) {
                continue;
            }
            int crew = Compute.getFullCrewSize(entity);
            tally.count(entity, crew);
            plannedCrew += crew;
        }
        int basePersonnel = campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false).size()
              + plannedCrew;
        return demandFor(campaign, tally, vehiclesStillToCome, basePersonnel);
    }

    /** Counts the units that need support staff, by the kind of tech they need. */
    private static final class UnitTally {
        private int mekCount;
        private int vehicleCount;
        private int aeroCount;
        private int protoMekCrew;
        private int baSuits;

        private void count(Entity entity, int fullCrewSize) {
            if (entity.isMek()) {
                mekCount++;
            } else if (entity.isProtoMek()) {
                protoMekCrew += fullCrewSize;
            } else if (entity.isVehicle()) {
                vehicleCount++;
            } else if (entity.isAerospaceFighter()
                  || entity.isConventionalFighter()
                  || entity.isSmallCraft()) {
                aeroCount++;
            } else if (entity.isBattleArmor()) {
                baSuits += fullCrewSize;
            }
            // DropShip / JumpShip / WarShip / SpaceStation intentionally skipped — their
            // organic VESSEL_CREW carries S_TECH_VESSEL and acts as the ship's own tech.
        }
    }

    private static SupportDemand demandFor(Campaign campaign, UnitTally tally, int vehiclesStillToCome,
          int basePersonnel) {
        int vehicleCount = tally.vehicleCount + Math.max(0, vehiclesStillToCome);

        int mekTechs = tally.mekCount + ceilDiv(tally.protoMekCrew, PROTOMEK_POINT_SIZE);
        int baTechs = ceilDiv(tally.baSuits, BA_SQUAD_SIZE);

        // Doctor and admin demand reflect the post-generation roster, so add the techs we project
        // creating. Doctors and admins are not added back because including them would be circular
        // (admins counted in admin demand, doctors counted in doctor demand). This matches the
        // SupportRating convention.
        int totalTechs = mekTechs + vehicleCount + tally.aeroCount + baTechs;
        int personnelForCalc = basePersonnel + totalTechs;

        int maxPatients = Math.max(1, campaign.getCampaignOptions().get(CampaignOption.MAXIMUM_PATIENTS));
        int doctors = ceilDiv(personnelForCalc, maxPatients);

        Faction faction = campaign.getPlayerForce().getFaction();
        int adminDivisor = isIrregularFaction(faction) ? ADMIN_DIVISOR_IRREGULAR : ADMIN_DIVISOR_STANDARD;
        int admins = ceilDiv(personnelForCalc, adminDivisor);

        SupportDemand demand = new SupportDemand(mekTechs, vehicleCount, tally.aeroCount, baTechs, doctors, admins);
        LOGGER.debug(
              "SupportPersonnelCalculator: meks={} vehicles={} aero={} protoMekCrew={} baSuits={} basePersonnel={} -> {}",
              tally.mekCount, vehicleCount, tally.aeroCount, tally.protoMekCrew, tally.baSuits, basePersonnel, demand);
        return demand;
    }

    /**
     * Scales a baseline 100%-coverage count by a user-supplied coverage percentage, rounding up so
     * any non-zero baseline produces at least one Person at 1% coverage. Returns 0 for a zero
     * baseline regardless of percentage; clamps negative percentages to 0.
     *
     * @param baselineCount the 100%-coverage demand from {@link SupportDemand}
     * @param percent the user's coverage percentage (0 = none, 100 = baseline, 300 = triple)
     * @return the number of Persons to actually generate for this role
     */
    public static int applyPercent(int baselineCount, int percent) {
        if (baselineCount <= 0 || percent <= 0) {
            return 0;
        }
        return (int) Math.ceil(baselineCount * percent / 100.0);
    }

    private static int ceilDiv(int dividend, int divisor) {
        if (dividend <= 0) {
            return 0;
        }
        return (int) Math.ceil(dividend / (double) divisor);
    }

    private static boolean isIrregularFaction(Faction faction) {
        return faction != null && (faction.isPirate() || faction.isMercenary());
    }
}
