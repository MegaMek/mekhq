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

import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.ratgen.SupportPersonnelCalculator.SupportDemand;

/**
 * Stage 7e of the ratgen company-generation pipeline: creates the support staff (techs, doctors,
 * administrators) the freshly-generated force needs to maintain itself, plus their astech and
 * medic assistants.
 *
 * <p>The flow:</p>
 *
 * <ol>
 *   <li>{@link SupportPersonnelCalculator#compute(Campaign)} returns the 100%-coverage demand for
 *       each role from the campaign's current force composition.</li>
 *   <li>For each support role this class multiplies the baseline by the user's per-role coverage
 *       percentage on {@link CompanyGenerationOptions#getSupportPersonnelCoveragePercents()}
 *       (default 100%), creates that many Persons via {@link Campaign#newPerson(PersonnelRole)},
 *       regenerates their skills at the user-selected experience tier via
 *       {@link AbstractSkillGenerator#generateSkills(Campaign, Person, int)}, sets the
 *       faction-appropriate support rank, and recruits them through
 *       {@link Campaign#recruitPerson(Person, PrisonerStatus, boolean, boolean)}.</li>
 *   <li>The four administrator roles share a single CamOps "1 admin per 20 personnel" demand —
 *       this class splits that demand equally across Command / Logistics / Transport / HR, then
 *       each role applies its own per-role coverage percentage independently.</li>
 *   <li>Astech and medic generation are independent toggles. When on, 6 astechs are generated per
 *       tech and 4 medics per doctor (the canonical {@code MHQConstants.AS_TECH_TEAM_SIZE} and
 *       {@code Campaign.getMedicsNeed} ratios). The pool-vs-individual-Personnel radio picks
 *       between adding to the campaign's anonymous pool counts or creating named Persons with the
 *       chosen skill level.</li>
 * </ol>
 *
 * <p>Every Person this class generates is added to the returned {@link Result#generatedPersons}
 * list so Stage 7d's founder/callsign flags can target them.</p>
 */
public final class SupportPersonnelGenerator {

    private static final MMLogger LOGGER = MMLogger.create(SupportPersonnelGenerator.class);

    /** Canonical 6 astechs per tech (one full astech team). */
    private static final int ASTECHS_PER_TECH = 6;
    /** Canonical 4 medics per doctor (one full medical team). */
    private static final int MEDICS_PER_DOCTOR = 4;
    /** Number of administrator roles the total admin demand is split across. */
    private static final int ADMIN_ROLE_COUNT = 4;

    private SupportPersonnelGenerator() {
        // utility class
    }

    /** Counts emitted by the generator, useful for logging and reporting. */
    public record Result(
          int mekTechsGenerated,
          int mechanicsGenerated,
          int aeroTeksGenerated,
          int baTechsGenerated,
          int doctorsGenerated,
          int administratorCommandGenerated,
          int administratorLogisticsGenerated,
          int administratorTransportGenerated,
          int administratorHRGenerated,
          int astechsAdded,
          int medicsAdded,
          List<Person> generatedPersons
    ) {
        /** Sum of the four tech-role generated counts. */
        public int totalTechsGenerated() {
            return mekTechsGenerated + mechanicsGenerated + aeroTeksGenerated + baTechsGenerated;
        }

        /** Sum of the four administrator-role generated counts. */
        public int totalAdministratorsGenerated() {
            return administratorCommandGenerated
                  + administratorLogisticsGenerated
                  + administratorTransportGenerated
                  + administratorHRGenerated;
        }
    }

    public static Result generate(Campaign campaign, CompanyGenerationOptions options) {
        if (campaign == null || options == null) {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>());
        }
        return generate(campaign, options,
              new DefaultSkillGenerator(campaign.getRandomSkillPreferences()));
    }

    /**
     * Package-private overload that lets tests inject a no-op or stubbed
     * {@link AbstractSkillGenerator}. Production callers should use the public single-arg form
     * which constructs a {@link DefaultSkillGenerator} from the campaign's skill preferences.
     */
    static Result generate(Campaign campaign, CompanyGenerationOptions options,
          AbstractSkillGenerator skillGen) {
        if (campaign == null || options == null) {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>());
        }

        long start = System.nanoTime();

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);
        Faction faction = resolveFaction(campaign, options);
        int supportRank = RulesetRankAssigner.supportRankForFaction(faction);
        // Resolve the rank system once so every Person we create renders rank names through the
        // target faction's table — a Clan-generated tech needs to read as a Clan "Warrior" tech,
        // not an IS "Corporal", even when generated into a Mercenary campaign.
        RankSystem targetRankSystem = faction.getRankSystem();
        RankValidator rankValidator = new RankValidator();

        List<Person> generated = new ArrayList<>();

        int mekTechs = generateRole(campaign, options, skillGen, PersonnelRole.MEK_TECH,
              demand.mekTechsNeeded(), supportRank, targetRankSystem, rankValidator, generated);
        int mechanics = generateRole(campaign, options, skillGen, PersonnelRole.MECHANIC,
              demand.mechanicsNeeded(), supportRank, targetRankSystem, rankValidator, generated);
        int aeroTeks = generateRole(campaign, options, skillGen, PersonnelRole.AERO_TEK,
              demand.aeroTeksNeeded(), supportRank, targetRankSystem, rankValidator, generated);
        int baTechs = generateRole(campaign, options, skillGen, PersonnelRole.BA_TECH,
              demand.baTechsNeeded(), supportRank, targetRankSystem, rankValidator, generated);
        int doctors = generateRole(campaign, options, skillGen, PersonnelRole.DOCTOR,
              demand.doctorsNeeded(), supportRank, targetRankSystem, rankValidator, generated);

        // Equal split of total admin demand across the four administrator roles. Each role then
        // applies its own per-role coverage percentage in generateRole().
        int adminBaselinePerRole = (int) Math.ceil(demand.administratorsNeeded() / (double) ADMIN_ROLE_COUNT);
        int adminCmd = generateRole(campaign, options, skillGen, PersonnelRole.ADMINISTRATOR_COMMAND,
              adminBaselinePerRole, supportRank, targetRankSystem, rankValidator, generated);
        int adminLog = generateRole(campaign, options, skillGen, PersonnelRole.ADMINISTRATOR_LOGISTICS,
              adminBaselinePerRole, supportRank, targetRankSystem, rankValidator, generated);
        int adminTpt = generateRole(campaign, options, skillGen, PersonnelRole.ADMINISTRATOR_TRANSPORT,
              adminBaselinePerRole, supportRank, targetRankSystem, rankValidator, generated);
        int adminHR = generateRole(campaign, options, skillGen, PersonnelRole.ADMINISTRATOR_HR,
              adminBaselinePerRole, supportRank, targetRankSystem, rankValidator, generated);

        int totalTechs = mekTechs + mechanics + aeroTeks + baTechs;
        int astechs = applyAstechs(campaign, options, skillGen, supportRank, targetRankSystem,
              rankValidator, totalTechs, generated);
        int medics = applyMedics(campaign, options, skillGen, supportRank, targetRankSystem,
              rankValidator, doctors, generated);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("[CompanyGen][Pipeline][Support] generated techs(mekTech={} mechanic={} aero={} ba={}) " +
                          "doctors={} admin(cmd={} log={} tpt={} hr={}) astechs={} medics={} elapsed={}ms",
              mekTechs, mechanics, aeroTeks, baTechs, doctors,
              adminCmd, adminLog, adminTpt, adminHR, astechs, medics, elapsedMs);

        return new Result(mekTechs, mechanics, aeroTeks, baTechs, doctors,
              adminCmd, adminLog, adminTpt, adminHR, astechs, medics, generated);
    }

    /**
     * Generates {@code ceil(baselineDemand × coverage / 100)} Persons of the given role, applying
     * the user-selected skill level and faction-appropriate rank to each. Returns the actual count
     * created (zero if either baseline or coverage is non-positive).
     */
    private static int generateRole(Campaign campaign, CompanyGenerationOptions options,
          AbstractSkillGenerator skillGen, PersonnelRole role, int baselineDemand, int supportRank,
          RankSystem targetRankSystem, RankValidator rankValidator, List<Person> out) {
        int percent = options.getSupportPersonnelCoveragePercents().getOrDefault(role, 100);
        int count = SupportPersonnelCalculator.applyPercent(baselineDemand, percent);
        if (count <= 0) {
            return 0;
        }
        SkillLevel skillLevel = options.getSupportPersonnelSkillLevels().getOrDefault(role, SkillLevel.REGULAR);
        int expLvl = toExperienceLevel(skillLevel);

        for (int i = 0; i < count; i++) {
            Person person = createAndRecruit(campaign, skillGen, role, expLvl, supportRank,
                  targetRankSystem, rankValidator);
            if (person != null) {
                out.add(person);
            }
        }
        return count;
    }

    /**
     * Pool-or-Person dispatch for astech generation. Returns the number of astechs added (pool
     * count or Person count, semantics depend on the mode).
     */
    private static int applyAstechs(Campaign campaign, CompanyGenerationOptions options,
          AbstractSkillGenerator skillGen, int supportRank, RankSystem targetRankSystem,
          RankValidator rankValidator, int totalTechs, List<Person> out) {
        if (!options.isGenerateAstechs() || totalTechs <= 0) {
            return 0;
        }
        int needed = ASTECHS_PER_TECH * totalTechs;
        return applyAssistant(campaign, skillGen, PersonnelRole.ASTECH, needed,
              options.isAstechsAsPersonnel(),
              options.getAstechSkillLevel(), supportRank, targetRankSystem, rankValidator,
              out, AssistantPool.ASTECH);
    }

    /**
     * Pool-or-Person dispatch for medic generation. Returns the number of medics added.
     */
    private static int applyMedics(Campaign campaign, CompanyGenerationOptions options,
          AbstractSkillGenerator skillGen, int supportRank, RankSystem targetRankSystem,
          RankValidator rankValidator, int totalDoctors, List<Person> out) {
        if (!options.isGenerateMedics() || totalDoctors <= 0) {
            return 0;
        }
        int needed = MEDICS_PER_DOCTOR * totalDoctors;
        return applyAssistant(campaign, skillGen, PersonnelRole.MEDIC, needed,
              options.isMedicsAsPersonnel(),
              options.getMedicSkillLevel(), supportRank, targetRankSystem, rankValidator,
              out, AssistantPool.MEDIC);
    }

    private enum AssistantPool { ASTECH, MEDIC }

    private static int applyAssistant(Campaign campaign, AbstractSkillGenerator skillGen,
          PersonnelRole role, int needed, boolean asPersonnel, SkillLevel skillLevel,
          int supportRank, RankSystem targetRankSystem, RankValidator rankValidator,
          List<Person> out, AssistantPool pool) {
        if (needed <= 0) {
            return 0;
        }
        if (asPersonnel) {
            int expLvl = toExperienceLevel(skillLevel == null ? SkillLevel.REGULAR : skillLevel);
            for (int i = 0; i < needed; i++) {
                Person person = createAndRecruit(campaign, skillGen, role, expLvl, supportRank,
                      targetRankSystem, rankValidator);
                if (person != null) {
                    out.add(person);
                }
            }
            return needed;
        }
        // Pool mode: anonymous slots in the campaign's astech / medic pool. No Persons created.
        switch (pool) {
            case ASTECH -> campaign.increaseAsTechPool(needed);
            case MEDIC -> campaign.increaseMedicPool(needed);
        }
        return needed;
    }

    /**
     * Creates a Person of {@code role}, regenerates their skills at {@code expLvl}, sets their
     * rank to {@code supportRank}, and recruits them into the campaign. Returns the created
     * Person, or {@code null} if recruitment failed.
     *
     * <p>Swaps the Person's rank system to {@code targetRankSystem} before setting the rank index
     * — without that swap, a Mercenary campaign generating Clan support staff would render the
     * Clan-targeted index through the campaign's IS rank table, producing wrong names like
     * "Corporal" instead of the Clan equivalent.</p>
     */
    private static Person createAndRecruit(Campaign campaign, AbstractSkillGenerator skillGen,
          PersonnelRole role, int expLvl, int supportRank, RankSystem targetRankSystem,
          RankValidator rankValidator) {
        Person person = campaign.newPerson(role);
        // newPerson already runs skill generation at the campaign's default level; regenerate at
        // the user-selected experience tier so the role's primary skills land at the right level.
        skillGen.generateSkills(campaign, person, expLvl);
        if (targetRankSystem != null) {
            RankSystem currentSystem = person.getRankSystem();
            if (currentSystem == null || !targetRankSystem.equals(currentSystem)) {
                person.setRankSystem(rankValidator, targetRankSystem);
            }
        }
        person.setRank(supportRank);
        boolean recruited = campaign.recruitPerson(person, PrisonerStatus.FREE, true, true);
        if (!recruited) {
            LOGGER.warn("[CompanyGen][Pipeline][Support] failed to recruit {} ({})",
                  role.name(), person.getFullName());
            return null;
        }
        return person;
    }

    /**
     * Picks the rank-authority faction the same way {@link RulesetRankAssigner#apply} does:
     * honors {@link CompanyGenerationOptions#isUseSpecifiedFactionToAssignRanks()} and falls back
     * to the campaign's faction.
     */
    private static Faction resolveFaction(Campaign campaign, CompanyGenerationOptions options) {
        Faction faction = options.isUseSpecifiedFactionToAssignRanks()
              ? options.getSpecifiedFaction()
              : campaign.getFaction();
        return (faction != null) ? faction : campaign.getFaction();
    }

    /**
     * Converts a {@link SkillLevel} (where {@code NONE = 0, ULTRA_GREEN = 1, …, ELITE = 5}) to a
     * {@link SkillType} {@code EXP_*} constant (where {@code EXP_ULTRA_GREEN = 0, …, EXP_ELITE =
     * 4}). The two enums use different baselines; this is the canonical mapping.
     */
    static int toExperienceLevel(SkillLevel skillLevel) {
        if (skillLevel == null) {
            return SkillType.EXP_REGULAR;
        }
        return switch (skillLevel) {
            case ULTRA_GREEN -> SkillType.EXP_ULTRA_GREEN;
            case GREEN -> SkillType.EXP_GREEN;
            case REGULAR -> SkillType.EXP_REGULAR;
            case VETERAN -> SkillType.EXP_VETERAN;
            case ELITE -> SkillType.EXP_ELITE;
            default -> SkillType.EXP_REGULAR;
        };
    }
}
