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

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.SupportPersonnelCalculator.SupportDemand;

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
 *       percentage on {@link CommandGenerationOptions#getSupportPersonnelCoveragePercents()}
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

    /** Guard on the HR-strain top-up loop so a misconfigured admin skill can't spin it forever. */
    private static final int MAX_HR_STRAIN_TOPUP = 100;

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

    public static Result generate(Campaign campaign, CommandGenerationOptions options) {
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
    static Result generate(Campaign campaign, CommandGenerationOptions options,
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

        // A freshly generated command should not open with an HR-strain turnover penalty, so top up
        // HR administrators until the strain modifier reaches zero (no-op when the rule is off).
        adminHR += topUpHumanResourcesToZeroStrain(campaign, options, skillGen, supportRank,
              targetRankSystem, rankValidator, generated);

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
    private static int generateRole(Campaign campaign, CommandGenerationOptions options,
          AbstractSkillGenerator skillGen, PersonnelRole role, int baselineDemand, int supportRank,
          RankSystem targetRankSystem, RankValidator rankValidator, List<Person> out) {
        int percent = options.getSupportPersonnelCoveragePercents().getOrDefault(role, 100);
        int target = SupportPersonnelCalculator.applyPercent(baselineDemand, percent);
        // Reconcile against staff already present so re-running support (for example after adding
        // combat forces) tops up only the shortfall instead of duplicating existing personnel.
        int existing = countActiveByRole(campaign, role);
        int count = Math.max(0, target - existing);
        if (count <= 0) {
            return 0;
        }
        // null = the "Random" picker option: roll each person's level individually (below).
        SkillLevel skillLevel = options.getSupportPersonnelSkillLevels().get(role);

        for (int i = 0; i < count; i++) {
            Person person = createAndRecruit(campaign, skillGen, role, experienceLevelFor(skillLevel),
                  supportRank, targetRankSystem, rankValidator);
            if (person != null) {
                out.add(person);
            }
        }
        return count;
    }

    /**
     * Counts the active personnel whose primary role is {@code role}, used to reconcile support-staff
     * generation against staff already present.
     *
     * @param campaign the campaign to inspect
     * @param role     the primary role to count
     *
     * @return the number of active personnel with that primary role
     */
    static int countActiveByRole(Campaign campaign, PersonnelRole role) {
        int matches = 0;
        for (Person person : campaign.getActivePersonnel(false, false)) {
            if (person.getPrimaryRole() == role) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * Generates HR administrators until the campaign's HR-strain modifier reaches zero, so a freshly
     * generated command does not start with an HR-strain turnover penalty. A no-op when the HR-strain
     * rule is disabled; guarded by {@link #MAX_HR_STRAIN_TOPUP} against runaway generation.
     *
     * @return the number of HR administrators added
     */
    private static int topUpHumanResourcesToZeroStrain(Campaign campaign, CommandGenerationOptions options,
          AbstractSkillGenerator skillGen, int supportRank, RankSystem targetRankSystem,
          RankValidator rankValidator, List<Person> out) {
        if (!campaign.getCampaignOptions().isUseHRStrain()) {
            return 0;
        }
        // null = "Random": each HR admin rolls its own level (below).
        SkillLevel skillLevel = options.getSupportPersonnelSkillLevels().get(PersonnelRole.ADMINISTRATOR_HR);

        int added = 0;
        while (RetirementDefectionTracker.getHRStrainModifier(campaign) > 0 && added < MAX_HR_STRAIN_TOPUP) {
            Person admin = createAndRecruit(campaign, skillGen, PersonnelRole.ADMINISTRATOR_HR,
                  experienceLevelFor(skillLevel), supportRank, targetRankSystem, rankValidator);
            if (admin == null) {
                break;
            }
            out.add(admin);
            added++;
        }
        LOGGER.info("[CompanyGen][Pipeline][Support] HR-strain top-up: added {} HR admins, strain modifier now {}",
              added, RetirementDefectionTracker.getHRStrainModifier(campaign));
        return added;
    }

    /**
     * Pool-or-Person dispatch for astech generation. Returns the number of astechs added (pool
     * count or Person count, semantics depend on the mode).
     */
    private static int applyAstechs(Campaign campaign, CommandGenerationOptions options,
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
    private static int applyMedics(Campaign campaign, CommandGenerationOptions options,
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
            // null = "Random": roll each assistant's level individually.
            for (int i = 0; i < needed; i++) {
                Person person = createAndRecruit(campaign, skillGen, role, experienceLevelFor(skillLevel),
                      supportRank, targetRankSystem, rankValidator);
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
                LOGGER.info("[CompanyGen][Support][RankSystem] swap person='{}' role={} oldSystem={} newSystem={} supportRank={}",
                      person.getFullName(), person.getPrimaryRole().name(),
                      currentSystem == null ? "null" : currentSystem.getCode(),
                      targetRankSystem.getCode(), supportRank);
                person.setRankSystem(rankValidator, targetRankSystem);
            } else {
                LOGGER.info("[CompanyGen][Support][RankSystem] no-swap person='{}' already on system={} supportRank={}",
                      person.getFullName(), targetRankSystem.getCode(), supportRank);
            }
        } else {
            LOGGER.warn("[CompanyGen][Support][RankSystem] targetRankSystem is null for person='{}' — leaving on existing system={} (this is the wrong-rank-names path)",
                  person.getFullName(),
                  person.getRankSystem() == null ? "null" : person.getRankSystem().getCode());
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
     * honors {@link CommandGenerationOptions#isUseSpecifiedFactionToAssignRanks()} and falls back
     * to the campaign's faction.
     */
    private static Faction resolveFaction(Campaign campaign, CommandGenerationOptions options) {
        Faction specifiedFaction = options.getSpecifiedFaction();
        Faction campaignFaction = campaign.getFaction();
        boolean useSpecified = options.isUseSpecifiedFactionToAssignRanks();
        Faction resolved = useSpecified ? specifiedFaction : campaignFaction;
        if (resolved == null) {
            resolved = campaignFaction;
        }
        LOGGER.info("[CompanyGen][Support][Faction] resolve: useSpecified={} specifiedFaction={} campaignFaction={} -> resolved={} (isClan={} isComStarOrWoB={} isMercenary={})",
              useSpecified,
              specifiedFaction == null ? "null" : specifiedFaction.getShortName(),
              campaignFaction == null ? "null" : campaignFaction.getShortName(),
              resolved == null ? "null" : resolved.getShortName(),
              resolved != null && resolved.isClan(),
              resolved != null && resolved.isComStarOrWoB(),
              resolved != null && resolved.isMercenary());
        return resolved;
    }

    /**
     * Converts a {@link SkillLevel} (where {@code NONE = 0, ULTRA_GREEN = 1, …, LEGENDARY = 7}) to a
     * {@link SkillType} {@code EXP_*} constant (where {@code EXP_ULTRA_GREEN = 0, …, EXP_LEGENDARY =
     * 6}). The two enums use different baselines; this is the canonical mapping.
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
            case HEROIC -> SkillType.EXP_HEROIC;
            case LEGENDARY -> SkillType.EXP_LEGENDARY;
            default -> SkillType.EXP_REGULAR;
        };
    }

    /**
     * The {@link SkillType} experience level for one generated person. When {@code configured} is
     * {@code null} - the "Random" skill-picker option - each person rolls their own level via
     * {@link #rollRandomSkillLevel()}; otherwise every person of the role shares the fixed level.
     *
     * @param configured the picker's skill level, or {@code null} for a per-person random roll
     *
     * @return the experience level to generate this person's skills at
     */
    static int experienceLevelFor(SkillLevel configured) {
        return toExperienceLevel(configured == null ? rollRandomSkillLevel() : configured);
    }

    /**
     * Rolls a random skill level for a support person: a 2d6 bell curve centered on Regular
     * (Ultra-Green on a 2, Elite on a 12), with a rare escalation - an Elite roll can climb to
     * Heroic, and a Heroic roll to Legendary - so exceptional staff appear but stay uncommon
     * (Heroic ~0.5%, Legendary ~0.08%).
     *
     * @return the rolled {@link SkillLevel}
     */
    static SkillLevel rollRandomSkillLevel() {
        SkillLevel level = switch (Compute.d6(2)) {
            case 2 -> SkillLevel.ULTRA_GREEN;
            case 3, 4, 5 -> SkillLevel.GREEN;
            case 6, 7, 8, 9 -> SkillLevel.REGULAR;
            case 10, 11 -> SkillLevel.VETERAN;
            default -> SkillLevel.ELITE; // 12
        };
        if (level == SkillLevel.ELITE && Compute.d6() == 6) {
            level = SkillLevel.HEROIC;
            if (Compute.d6() == 6) {
                level = SkillLevel.LEGENDARY;
            }
        }
        return level;
    }
}
