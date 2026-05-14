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
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Stage 7c-bis of the ratgen pipeline: generates the CamOps 5th-print support personnel
 * (techs, doctors, admins, astechs, medics) for the just-materialized combat force.
 *
 * <p>Headcount per role is derived from {@link Campaign#getActiveUnits()} using the canonical
 * CamOps tech-team / non-admin-members formulas. A positive spinner value in
 * {@link CompanyGenerationOptions#getSupportPersonnel()} for any role overrides the computed
 * value for that role; a zero (the {@code RULESET_BASED} default) leaves the role on
 * auto.</p>
 *
 * <p>Side effects:</p>
 * <ul>
 *   <li>Creates a {@link FormationType#SUPPORT}-typed sub-formation named
 *       "Headquarters Staff" under the campaign root. Empty in Phase 1; Phase 2 will populate
 *       it with team-as-Unit objects.</li>
 *   <li>GM-recruits each generated {@link Person} into the campaign roster.</li>
 *   <li>Assigns the faction-appropriate support rank via
 *       {@link RulesetRankAssigner#setRankWithFallback}.</li>
 *   <li>If {@link CompanyGenerationOptions#isPoolAssistants()}, fills the AsTech / Medic pools
 *       AFTER recruiting techs / doctors instead of creating ASTECH / MEDIC Persons.</li>
 * </ul>
 */
public final class SupportPersonnelGenerator {

    private static final MMLogger LOGGER = MMLogger.create(SupportPersonnelGenerator.class);

    /** CamOps p.180 Support Vehicle weight tier boundary between Medium and Large (tons). */
    private static final double LARGE_SUPPORT_VEHICLE_WEIGHT_THRESHOLD = 100.0;

    /** Per-vehicle admin-tally divisor: 1 admin-tally point per N tons of combat vehicle weight. */
    private static final int ADMIN_TALLY_TONS_PER_COMBAT_VEHICLE = 15;

    /** CamOps p.190: 1 admin per 10 non-administrative Force members, round up. */
    private static final int ADMIN_DIVISOR = 10;

    /** Government Forces halve their admin requirement (round up). */
    private static final int GOVERNMENT_ADMIN_DIVISOR = 2;

    /**
     * CamOps lists ~6 admin specialties (paper-pushers, doctors, intelligence, comms,
     * quartermasters, lawyers). Doctors are 1 of those 6; carving 1/6 of the admin pool
     * to the DOCTOR role preserves functional doctors in the campaign without changing
     * the CamOps total.
     */
    private static final int DOCTOR_CARVE_DIVISOR = 6;

    /** Legacy MekHQ ratio: 4 medics per doctor (matches {@link Campaign#getMedicsNeed}). */
    private static final int MEDICS_PER_DOCTOR = 4;

    private SupportPersonnelGenerator() {
        // utility class
    }

    /**
     * Aggregated counts of units / troopers tallied from {@link Campaign#getActiveUnits()},
     * with the CamOps-specific shape needed by the support-personnel formulas. Vehicle and
     * crewman counts are kept per-unit (not summed) because the admin tally rounds up
     * <em>per vehicle</em> on the weight/15 rule, and large craft / support vehicle crew
     * counts come from each unit's TRO crew size.
     */
    record UnitTally(int mek, int asfOrConvFighter, int smallCraft, int protoMek, int baTroopers,
          int infantryTroopers, List<Integer> combatVehicleWeights,
          List<Integer> nonLargeSupportVehicleCrew, List<Integer> largeCraftCrew) { }

    /** Per-role headcount to recruit at the end of the pipeline. */
    record Counts(int mekTech, int mechanic, int aeroTek, int baTech, int doctor,
          int adminCmd, int adminLog, int adminTxp, int adminHr, int astech, int medic) { }

    /**
     * Entry point: tallies the just-generated force, computes CamOps-aligned counts (or
     * applies per-role overrides), creates the Headquarters Staff sub-formation, then
     * GM-recruits the support persons.
     *
     * @return the flat list of {@link Person}s this stage added to the campaign; the caller
     *         appends them to its {@code generatedPersons} accumulator so Stage 7d's founder
     *         / callsign flags pick them up uniformly.
     */
    public static List<Person> generate(Campaign campaign, CompanyGenerationOptions options) {
        long startNanos = System.nanoTime();

        Faction faction = options.isUseSpecifiedFactionToAssignRanks() && options.getSpecifiedFaction() != null
                                ? options.getSpecifiedFaction()
                                : campaign.getFaction();
        int supportRank = RulesetRankAssigner.supportRankForFaction(faction);
        boolean isGovernment = !faction.isMercenary() && !faction.isPirate();

        Formation hq = createHeadquartersFormation(campaign);
        LOGGER.info("[CompanyGen][Support] HQ formation created id={} name='{}'",
              hq.getId(), hq.getName());

        UnitTally tally = tallyUnits(campaign);
        Counts counts = compute(tally, isGovernment, options);

        LOGGER.info("[CompanyGen][Support] CamOps counts: mekTech={} mechanic={} aeroTek={} baTech={} doctor={} adminCmd={} adminLog={} adminTxp={} adminHr={} astech={} medic={} (faction={} government={} poolAssistants={})",
              counts.mekTech(), counts.mechanic(), counts.aeroTek(), counts.baTech(),
              counts.doctor(), counts.adminCmd(), counts.adminLog(), counts.adminTxp(),
              counts.adminHr(), counts.astech(), counts.medic(),
              faction.getShortName(), isGovernment, options.isPoolAssistants());

        List<Person> generated = new ArrayList<>();
		 recruit(campaign, PersonnelRole.MEK_TECH, counts.mekTech(), supportRank, generated);
        recruit(campaign, PersonnelRole.MECHANIC, counts.mechanic(), supportRank, generated);
        recruit(campaign, PersonnelRole.AERO_TEK, counts.aeroTek(), supportRank, generated);
        recruit(campaign, PersonnelRole.BA_TECH, counts.baTech(), supportRank, generated);
        recruit(campaign, PersonnelRole.DOCTOR, counts.doctor(), supportRank, generated);
        recruit(campaign, PersonnelRole.ADMINISTRATOR_COMMAND, counts.adminCmd(), supportRank, generated);
        recruit(campaign, PersonnelRole.ADMINISTRATOR_LOGISTICS, counts.adminLog(), supportRank, generated);
        recruit(campaign, PersonnelRole.ADMINISTRATOR_TRANSPORT, counts.adminTxp(), supportRank, generated);
        recruit(campaign, PersonnelRole.ADMINISTRATOR_HR, counts.adminHr(), supportRank, generated);

        // Pool path: skip individual ASTECH/MEDIC creation; refresh pools from the now-current
        // tech/doctor counts. fillAsTechPool reads getAsTechNeed() = techs*6 - existing astechs;
        // resetMedicPool empties then fills doctors*4 - existing medics. Both pools were 0
        // before this stage because we don't generate any astechs/medics during combat assembly.
        if (options.isPoolAssistants()) {
            campaign.fillAsTechPool();
            campaign.resetMedicPool();
            LOGGER.info("[CompanyGen][Support] Pool path: filled astech pool ({}) and medic pool ({})",
                  campaign.getTemporaryAsTechPool(), campaign.getTemporaryMedicPool());
        } else {
            recruit(campaign, PersonnelRole.ASTECH, counts.astech(), supportRank, generated);
            recruit(campaign, PersonnelRole.MEDIC, counts.medic(), supportRank, generated);
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][Support] DONE generated={} elapsedMs={}", generated.size(), elapsedMs);
        return generated;
    }

    /**
     * Creates the "Headquarters Staff" sub-formation under the campaign root and marks it
     * SUPPORT. Empty in Phase 1; provides a stable hook for Phase 2 to attach team-Units.
     */
    static Formation createHeadquartersFormation(Campaign campaign) {
        Formation hq = new Formation("Headquarters Staff");
        hq.setFormationType(FormationType.SUPPORT, false);
        campaign.addFormation(hq, campaign.getFormations());
        return hq;
    }

    /** Walks {@link Campaign#getActiveUnits()} and bins entities into the CamOps categories. */
    static UnitTally tallyUnits(Campaign campaign) {
        int mek = 0, asfOrConv = 0, smallCraft = 0, protoMek = 0, baTroopers = 0, infTroopers = 0;
        List<Integer> combatVehicleWeights = new ArrayList<>();
        List<Integer> nonLargeSupportVehicleCrew = new ArrayList<>();
        List<Integer> largeCraftCrew = new ArrayList<>();

        for (Unit unit : campaign.getActiveUnits()) {
            Entity entity = unit.getEntity();
            if (entity == null) {
                continue;
            }
            // Order matters: check BA before generic Infantry (BattleArmor extends Infantry in
            // MegaMek), and check Large/SmallCraft before generic Vehicle / Aero.
            if (entity.isMek()) {
                mek++;
            } else if (entity.isProtoMek()) {
                protoMek++;
            } else if (entity.isBattleArmor()) {
                baTroopers += unit.getFullCrewSize();
            } else if (entity.isInfantry()) {
                infTroopers += unit.getFullCrewSize();
            } else if (entity.isLargeCraft()) {
                largeCraftCrew.add(unit.getFullCrewSize());
            } else if (entity.isSmallCraft()) {
                smallCraft++;
            } else if (entity.isAerospaceFighter() || entity.isConventionalFighter()) {
                asfOrConv++;
            } else if (entity.isSupportVehicle()) {
                // Large support vehicles (>100 tons) get folded into the large-craft bucket: no
                // tech team, crew counted for admin tally only. Smaller support vehicles need a
                // tech team and contribute their crew to the admin tally.
                int crew = unit.getFullCrewSize();
                if (entity.getWeight() >= LARGE_SUPPORT_VEHICLE_WEIGHT_THRESHOLD) {
                    largeCraftCrew.add(crew);
                } else {
                    nonLargeSupportVehicleCrew.add(crew);
                }
            } else if (entity.isVehicle()) {
                combatVehicleWeights.add((int) entity.getWeight());
            }
        }

        return new UnitTally(mek, asfOrConv, smallCraft, protoMek, baTroopers, infTroopers,
              combatVehicleWeights, nonLargeSupportVehicleCrew, largeCraftCrew);
    }

    /**
     * Applies the CamOps formulas to {@code tally} and merges per-role spinner overrides from
     * {@code options}. A positive override wins; zero / negative leaves the role on auto.
     */
    static Counts compute(UnitTally tally, boolean isGovernment, CompanyGenerationOptions options) {
        // Tech teams (1 tech + 6 astechs each).
        int mekTeams = tally.mek() + roundToInt(tally.protoMek() / 5.0);
        int mechanicTeams = tally.combatVehicleWeights().size()
                                  + tally.nonLargeSupportVehicleCrew().size()
                                  + roundToInt(tally.infantryTroopers() / 112.0);
        int aeroTeams = tally.asfOrConvFighter() + tally.smallCraft();
        int baTeams = roundToInt(tally.baTroopers() / 5.0);
        int totalTechTeams = mekTeams + mechanicTeams + aeroTeams + baTeams;

        // Non-admin members tally for the admin formula. CamOps p.190 enumeration.
        int nonAdminMembers = totalTechTeams * (1 + MHQConstants.AS_TECH_TEAM_SIZE)
                                    + tally.mek()
                                    + tally.asfOrConvFighter()
                                    + tally.protoMek()
                                    + tally.baTroopers()
                                    + tally.infantryTroopers();
        for (int weight : tally.combatVehicleWeights()) {
            nonAdminMembers += ceilDiv(weight, ADMIN_TALLY_TONS_PER_COMBAT_VEHICLE);
        }
        for (int crew : tally.nonLargeSupportVehicleCrew()) {
            nonAdminMembers += crew;
        }
        for (int crew : tally.largeCraftCrew()) {
            nonAdminMembers += crew;
        }

        int admins = ceilDiv(nonAdminMembers, ADMIN_DIVISOR);
        if (isGovernment) {
            admins = ceilDiv(admins, GOVERNMENT_ADMIN_DIVISOR);
        }

        // Carve doctors from the admin pool (1/6 of the admin slots, minimum 1 if admins > 0).
        int doctorAuto = admins > 0 ? Math.max(1, roundToInt(admins / (double) DOCTOR_CARVE_DIVISOR)) : 0;
        int remainingAdmins = Math.max(0, admins - doctorAuto);
        int adminLogAuto = (int) Math.ceil(remainingAdmins * 0.50);
        int adminCmdAuto = roundToInt(remainingAdmins * 0.20);
        int adminHrAuto = roundToInt(remainingAdmins * 0.20);
        int adminTxpAuto = Math.max(0, remainingAdmins - adminLogAuto - adminCmdAuto - adminHrAuto);

        // Astechs/medics scale from final (post-override) tech/doctor counts so that an admin
        // override of techs/doctors also rescales the pool-OFF support headcount.
        Map<PersonnelRole, Integer> overrides = options.getSupportPersonnel();
        int mekTech = override(overrides, PersonnelRole.MEK_TECH, mekTeams);
        int mechanic = override(overrides, PersonnelRole.MECHANIC, mechanicTeams);
        int aeroTek = override(overrides, PersonnelRole.AERO_TEK, aeroTeams);
        int baTech = override(overrides, PersonnelRole.BA_TECH, baTeams);
        int doctor = override(overrides, PersonnelRole.DOCTOR, doctorAuto);
        int adminCmd = override(overrides, PersonnelRole.ADMINISTRATOR_COMMAND, adminCmdAuto);
        int adminLog = override(overrides, PersonnelRole.ADMINISTRATOR_LOGISTICS, adminLogAuto);
        int adminTxp = override(overrides, PersonnelRole.ADMINISTRATOR_TRANSPORT, adminTxpAuto);
        int adminHr = override(overrides, PersonnelRole.ADMINISTRATOR_HR, adminHrAuto);

        int totalTechsAfterOverride = mekTech + mechanic + aeroTek + baTech;
        int astech = totalTechsAfterOverride * MHQConstants.AS_TECH_TEAM_SIZE;
        int medic = doctor * MEDICS_PER_DOCTOR;

        return new Counts(mekTech, mechanic, aeroTek, baTech, doctor,
              adminCmd, adminLog, adminTxp, adminHr, astech, medic);
    }

    /** Creates {@code count} Persons of {@code role}, GM-recruits each, and sets the rank. */
    private static void recruit(Campaign campaign, PersonnelRole role, int count, int rankIndex,
          List<Person> out) {
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Person person = campaign.newPerson(role);
            campaign.recruitPerson(person, /* gmAdd */ true, /* employ */ true);
            RulesetRankAssigner.setRankWithFallback(person, rankIndex);
            out.add(person);
        }
    }

    private static int override(Map<PersonnelRole, Integer> overrides, PersonnelRole role, int auto) {
        Integer requested = overrides == null ? null : overrides.get(role);
        return (requested != null && requested > 0) ? requested : auto;
    }

    /** Integer ceiling division for non-negative inputs; returns 0 if {@code divisor} is 0. */
    private static int ceilDiv(int dividend, int divisor) {
        if (divisor <= 0 || dividend <= 0) {
            return 0;
        }
        return (dividend + divisor - 1) / divisor;
    }

    /** Math.round to int (half-up), with a long-to-int narrow that's safe for our domain. */
    private static int roundToInt(double value) {
        return (int) Math.round(value);
    }
}