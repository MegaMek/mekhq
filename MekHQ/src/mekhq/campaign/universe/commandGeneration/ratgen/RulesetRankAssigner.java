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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import megamek.common.units.UnitType;

/**
 * Tree-aware rank assignment pass for the Force Generator pipeline.
 *
 * <p>Replaces the flat-list rank logic in the legacy {@code AbstractCompanyGenerator}. The legacy
 * pipeline picked "the highest-skilled Person → company commander, then one officer per lance" off
 * a sorted tracker list. The ratgen pipeline produces a real Formation tree with arbitrary nesting,
 * so this pass walks the tree post-order and claims one officer per Formation node — Lance commanders
 * pick their Lieutenant first, then the Company looks for its Captain among the remaining unranked
 * crew, and so on up to Brigade / Army / Galaxy.</p>
 *
 * <p>FormationLevel → officer rank index ({@code Rank.RWO_MAX + N}):</p>
 *
 * <ul>
 *   <li>LANCE / LEVEL_II_OR_CHOIR → +3 (Lieutenant / Adept); STAR_OR_NOVA → +2 (Star Commander), or +3
 *       (Nova Commander) for a Star that mixes Meks and Elementals</li>
 *   <li>COMPANY / BINARY_OR_TRINARY / LEVEL_III → +4 (Captain / Star Captain / Demi-Precentor)</li>
 *   <li>BATTALION → +5 (Major); LEVEL_IV and LEVEL_V → +7 (Precentor)</li>
 *   <li>REGIMENT / CLUSTER → +8 (Colonel / Star Colonel)</li>
 *   <li>BRIGADE / GALAXY → +9 (Brigadier General / Galaxy Commander)</li>
 *   <li>DIVISION → +10, CORPS → +11, ARMY → +12, ARMY_GROUP → +13</li>
 *   <li>LEVEL_VI → +12 (Precentor Martial); TOUMAN → +18 (Khan)</li>
 * </ul>
 *
 * <p>Which level a Formation counts as is taken from the echelon the generator was asked for, not
 * from the Formation's own {@code formationLevel}. See {@code requestedRootLevel} for why the
 * latter cannot be trusted at this point in the pipeline.</p>
 *
 * <p>Non-officer combat crew get an enlisted Sergeant rank (E12 IS, E4 Clan/CS); support staff get
 * a Corporal-equivalent (E8 IS, E4 Clan/CS) matching the legacy generator's convention. The campaign's
 * rank system (loaded from {@code data/universe/ranks.xml} per faction) maps the integer index to the
 * faction-appropriate display name.</p>
 *
 * <p>Gated on {@link CommandGenerationOptions#isAutomaticallyAssignRanks()}. Honors
 * {@link CommandGenerationOptions#isUseSpecifiedFactionToAssignRanks()} for the faction picker.</p>
 */
public final class RulesetRankAssigner {

    private static final MMLogger LOGGER = MMLogger.create(RulesetRankAssigner.class);

    private RulesetRankAssigner() {
        // utility class
    }

    /**
     * What the rank passes promoted.
     *
     * @param rootCommander the commander of the command as a whole, the one the dialog tags with the commander
     *                      flag: the person promoted at the campaign-root formation, or at the first formation
     *                      beneath it that carries a rank when the root itself is only a container; {@code null}
     *                      when ranks are off, the campaign has no formations, or nobody was eligible
     * @param officers      every person promoted to an officer rank, the root commander included, with the level
     *                      of the formation they were promoted at; in promotion order
     */
    public record Result(@Nullable Person rootCommander, Map<Person, FormationLevel> officers) {

        public static Result none() {
            return new Result(null, Map.of());
        }
    }

    /**
     * What the walker learned from the rolled tree, for the rank pass to lean on.
     *
     * <p>The formation tree alone does not say which echelon a formation is: a campaign's root force wraps the
     * command, and the tree's own levels are recomputed from depth. The descriptors do say, and they also name
     * the commander the engine chose for each formation, the one the preview showed.</p>
     *
     * @param levels           each formation's level from its descriptor's echelon; a formation not listed falls
     *                         back to counting rungs down from the requested echelon
     * @param engineCommanders the person the engine designated as each formation's commander, for the
     *                         formations whose designated commander was built
     */
    public record Guidance(Map<Formation, FormationLevel> levels, Map<Formation, Person> engineCommanders) {

        public static Guidance none() {
            return new Guidance(Map.of(), Map.of());
        }
    }

    /**
     * Runs the rank-assignment passes on the campaign's formation tree.
     *
     * @return the commander of the command as a whole (i.e. the top-echelon commander the dialog will tag with
     *         the commander flag in Stage 7d), or {@code null} when ranks are disabled, the campaign has no
     *         formations, or nobody was eligible
     */
    @Nullable
    public static Person apply(Campaign campaign, CommandGenerationOptions options) {
        return applyAndReport(campaign, options).rootCommander();
    }

    /**
     * Runs the rank-assignment passes on the campaign's formation tree and reports every promotion, with levels
     * counted down from the requested echelon and every commander found by walking the crews.
     *
     * @return the promotions made; {@link Result#none()} when ranks are disabled, the campaign has no formations,
     *       or nobody was eligible
     */
    public static Result applyAndReport(Campaign campaign, CommandGenerationOptions options) {
        return applyAndReport(campaign, options, Guidance.none());
    }

    /**
     * Runs the rank-assignment passes on the campaign's formation tree and reports every promotion.
     *
     * @param guidance what the walker learned from the rolled tree; {@link Guidance#none()} to count levels down
     *                 from the requested echelon and find every commander by walking the crews
     *
     * @return the promotions made; {@link Result#none()} when ranks are disabled, the campaign has no formations,
     *       or nobody was eligible
     */
    public static Result applyAndReport(Campaign campaign, CommandGenerationOptions options, Guidance guidance) {
        long startNanos = System.nanoTime();
        if (campaign == null || options == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign or options null, skipping");
            return Result.none();
        }
        if (!options.isAutomaticallyAssignRanks()) {
            LOGGER.info("[CompanyGen][RankAssign] apply: disabled by isAutomaticallyAssignRanks");
            return Result.none();
        }
        Formation root = campaign.getPlayerForce().getFormations();
        if (root == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign has no root Formation, skipping");
            return Result.none();
        }

        Faction specifiedFaction = options.getSpecifiedFaction();
        Faction campaignFaction = campaign.getPlayerForce().getFaction();
        boolean useSpecified = options.isUseSpecifiedFactionToAssignRanks();
        Faction faction = useSpecified ? specifiedFaction : campaignFaction;
        if (faction == null) {
            faction = campaignFaction;
        }
        LOGGER.info("[CompanyGen][RankAssign][Faction] resolve: useSpecified={} specifiedFaction={} campaignFaction={} -> resolved={} (isClan={} isComStarOrWoB={} isMercenary={})",
              useSpecified,
              specifiedFaction == null ? "null" : specifiedFaction.getShortName(),
              campaignFaction == null ? "null" : campaignFaction.getShortName(),
              faction == null ? "null" : faction.getShortName(),
              faction != null && faction.isClan(),
              faction != null && faction.isComStarOrWoB(),
              faction != null && faction.isMercenary());

        int enlistedRank = enlistedRankForFaction(faction);
        int supportRank = supportRankForFaction(faction);
        LOGGER.info("[CompanyGen][RankAssign][Faction] rank-index policy: enlistedRank={} supportRank={} (Clan/CS path={}, IS path={})",
              enlistedRank, supportRank,
              faction != null && (faction.isClan() || faction.isComStarOrWoB()),
              faction == null || (!faction.isClan() && !faction.isComStarOrWoB()));

        // The Person's rank-name lookup uses its OWN rank system, not the campaign's. So an IS
        // campaign generating a Clan force would render Clan-targeted rank indices through the
        // campaign's IS rank table ("Lieutenant" instead of "Nova Commander"). Resolve the target
        // faction's rank system here and apply it to every Person we rank below.
        RankSystem targetRankSystem = faction.getRankSystem();
        RankValidator rankValidator = new RankValidator();

        LOGGER.info("[CompanyGen][RankAssign] START faction={} rankSystem={} enlistedRank={} supportRank={} root='{}' guidedLevels={} engineCommanders={} thread={}",
              faction.getShortName(),
              targetRankSystem == null ? "null" : targetRankSystem.getCode(),
              enlistedRank, supportRank, root.getName(), guidance.levels().size(),
              guidance.engineCommanders().size(), Thread.currentThread().getName());

        // Pass 1: walk the tree top-down. Each formation promotes its commander before its sub-formations
        // choose theirs, and a formation whose crews already include an officer promoted above it is led by
        // that officer: the Cluster's Star Colonel leads the command Trinary and the command Star they sit in,
        // the way the engine's preview shows, rather than a separate officer being found for each.
        FormationLevel rootLevel = requestedRootLevel(campaign, options, root);
        LOGGER.info("[CompanyGen][RankAssign][Pass1] BEFORE walk rootLevel={}", rootLevel);
        long pass1Start = System.nanoTime();
        Set<Person> promoted = new LinkedHashSet<>();
        Map<Person, FormationLevel> officers = new LinkedHashMap<>();
        int[] officerCount = { 0 };
        Selection selection = Selection.from(campaign, options, faction.isClan());
        Pass pass = new Pass(campaign, selection, guidance, promoted, officers, officerCount, targetRankSystem,
              rankValidator);
        Person rootCommander = walk(pass, root, rootLevel, true);
        int officersAssigned = officerCount[0];
        long pass1Ms = (System.nanoTime() - pass1Start) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign][Pass1] AFTER walk officers={} rootCommander={} elapsed={}ms",
              officersAssigned, rootCommander == null ? "null" : rootCommander.getFullName(), pass1Ms);

        // Pass 2: every other combat Person gets the enlisted rank; every support Person gets the
        // Corporal-equivalent. Walk the tree's Units to limit the impact to crew we generated, not
        // pre-existing personnel.
        LOGGER.info("[CompanyGen][RankAssign][Pass2] BEFORE enlisted/support assignment");
        long pass2Start = System.nanoTime();
        int enlistedAssigned = 0;
        int supportAssigned = 0;
        Set<UUID> personIdsAlreadyDone = new HashSet<>();
        for (UUID unitId : root.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            for (Person person : unitCrew(unit)) {
                if (person == null) {
                    continue;
                }
                if (!personIdsAlreadyDone.add(person.getId())) {
                    continue;
                }
                if (promoted.contains(person)) {
                    continue;
                }
                if (person.isSupport()) {
                    setRankWithFallback(person, supportRank, targetRankSystem, rankValidator);
                    supportAssigned++;
                } else if (person.isCombat()) {
                    setRankWithFallback(person, enlistedRank, targetRankSystem, rankValidator);
                    enlistedAssigned++;
                }
            }
        }
        long pass2Ms = (System.nanoTime() - pass2Start) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign][Pass2] AFTER enlisted={} support={} elapsed={}ms",
              enlistedAssigned, supportAssigned, pass2Ms);

        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign] DONE; officers={} enlisted={} support={} totalMs={}",
              officersAssigned, enlistedAssigned, supportAssigned, totalMs);
        return new Result(rootCommander, officers);
    }

    /**
     * How commanders are chosen, from the Officer Selection options.
     *
     * @param commanderOrder how the command's own commander is chosen from everyone, or {@code null} to take the
     *                       engine's designated commander, failing that the first combat person found
     * @param officerOrder   how every other formation's commander is chosen from its remaining crew, or
     *                       {@code null} to take the engine's designated commander, failing that the first
     *                       combat person found
     * @param isClanCommand  {@code true} when the command is a Clan one, so a Bloodname comes first and a post
     *                       of Star Colonel or above requires one
     */
    record Selection(@Nullable Comparator<Person> commanderOrder, @Nullable Comparator<Person> officerOrder,
          boolean isClanCommand) {

        static Selection from(Campaign campaign, CommandGenerationOptions options, boolean isClanCommand) {
            Comparator<Person> commanderOrder = options.isAssignBestCompanyCommander()
                  ? OfficerSelector.bestFirst(campaign, options.isPrioritizeCompanyCommanderCombatSkills(),
                        isClanCommand)
                  : null;
            Comparator<Person> officerOrder = options.isAssignBestOfficers()
                  ? OfficerSelector.bestFirst(campaign, options.isPrioritizeOfficerCombatSkills(), isClanCommand)
                  : null;
            LOGGER.info("[CompanyGen][RankAssign] commander chosen {}, other officers chosen {}{}",
                  describeOrder(options.isAssignBestCompanyCommander(),
                        options.isPrioritizeCompanyCommanderCombatSkills()),
                  describeOrder(options.isAssignBestOfficers(), options.isPrioritizeOfficerCombatSkills()),
                  isClanCommand ? "; a Clan command, so a Bloodname comes first and Star Colonel and above require one"
                        : "");
            return new Selection(commanderOrder, officerOrder, isClanCommand);
        }

        private static String describeOrder(boolean bestFirst, boolean combatFirst) {
            if (!bestFirst) {
                return "as the engine designated";
            }
            return combatFirst ? "best first (combat before command skills)" : "best first (command skills before combat)";
        }

        /**
         * @param isRoot {@code true} for the command's own commander
         *
         * @return the order the formation chooses its commander by, best first, or {@code null} to take the
         *       engine's designated commander
         */
        @Nullable
        Comparator<Person> orderFor(boolean isRoot) {
            return isRoot ? commanderOrder : officerOrder;
        }
    }

    /** Everything one rank pass carries through the walk. */
    private record Pass(Campaign campaign, Selection selection, Guidance guidance, Set<Person> promoted,
          Map<Person, FormationLevel> officers, int[] officerCount, RankSystem targetRankSystem,
          RankValidator rankValidator) {
    }

    /**
     * Walks the formation tree top-down, promoting one Person per Formation to that Formation's officer rank.
     *
     * <p>A formation whose crews already include an officer promoted above it is led by that officer and gets
     * no separate one. Otherwise it chooses before its sub-formations do: the best person in its subtree by
     * the selection's order, or the engine's designated commander when there is no order.</p>
     *
     * @param fallbackLevel the formation's level when the guidance does not name one: counted down from the
     *                      requested echelon
     * @param isRoot        {@code true} for the campaign-root formation, whose commander is chosen by the
     *                      commander order rather than the officer order
     *
     * @return the person who leads this formation, whether promoted here or above it; when the formation itself
     *       carries no rank, the leader of the first sub-formation that does, so the top-level call's return is
     *       the commander of the command as a whole
     */
    @Nullable
    private static Person walk(Pass pass, Formation formation, @Nullable FormationLevel fallbackLevel,
          boolean isRoot) {
        return walk(pass, formation, fallbackLevel, isRoot, 0);
    }

    /**
     * @param positionInParent the formation's place among its parent's sub-formations, from zero: a Clan Point's
     *                         leader ranks by it, Point 2 to Point 5, the first Point being the Star Commander's own
     */
    @Nullable
    private static Person walk(Pass pass, Formation formation, @Nullable FormationLevel fallbackLevel,
          boolean isRoot, int positionInParent) {
        FormationLevel level = pass.guidance().levels().containsKey(formation)
              ? pass.guidance().levels().get(formation) : fallbackLevel;
        FormationLevel subLevel = oneLevelBelow(pass.campaign(), level);

        Person leader = ledFromAbove(pass, formation);
        if (leader != null) {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> led by '{}', promoted above it; no separate officer",
                  formation.getName(), level, leader.getFullName());
        } else {
            leader = promoteCommander(pass, formation, level, pass.selection().orderFor(isRoot), positionInParent);
        }

        Person firstBelow = null;
        int position = 0;
        for (Formation sub : formation.getSubFormations()) {
            Person subLeader = walk(pass, sub, subLevel, false, position);
            if ((firstBelow == null) && (subLeader != null)) {
                firstBelow = subLeader;
            }
            position++;
        }
        return (leader != null) ? leader : firstBelow;
    }

    /**
     * @return the officer promoted at a higher formation who sits in this one, or {@code null} when none does
     */
    @Nullable
    private static Person ledFromAbove(Pass pass, Formation formation) {
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = pass.campaign().getUnit(unitId);
            if (unit == null) {
                continue;
            }
            for (Person person : unitCrew(unit)) {
                if ((person != null) && pass.promoted().contains(person)) {
                    return person;
                }
            }
        }
        return null;
    }

    /** The rank index of a Star Colonel: from here up a Clan gives the post only to a Bloodnamed warrior. */
    static final int STAR_COLONEL_RANK_INDEX = Rank.RWO_MAX + 8;

    /**
     * @param rankIndex     the rank the post carries
     * @param isClanCommand whether the command is a Clan one
     *
     * @return {@code true} when the post is one a Clan gives only to a Bloodnamed warrior: Star Colonel and above
     *       in a Clan command
     */
    static boolean needsBloodname(int rankIndex, boolean isClanCommand) {
        return isClanCommand && (rankIndex >= STAR_COLONEL_RANK_INDEX);
    }

    /**
     * Promotes one person to the formation's officer rank. In a Clan command a post of Star Colonel or above goes
     * to a Bloodnamed warrior; when none is left, the warrior who takes it is awarded one.
     *
     * @param bestFirst the order to choose by, or {@code null} to take the engine's designated commander, failing
     *                  that the first combat person found
     *
     * @return the person promoted, or {@code null} when the level has no rank or nobody was left to promote
     */
    @Nullable
    private static Person promoteCommander(Pass pass, Formation formation, @Nullable FormationLevel level,
          @Nullable Comparator<Person> bestFirst, int positionInParent) {
        int rankIndex = rankIndexFor(pass.campaign(), formation, level, pass.selection().isClanCommand(),
              positionInParent);
        if (rankIndex < 0) {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no rank mapping, skip",
                  formation.getName(), level);
            return null;
        }
        boolean needsBloodname = needsBloodname(rankIndex, pass.selection().isClanCommand());
        Comparator<Person> order = bestFirst;
        if (needsBloodname && (order == null)) {
            order = OfficerSelector.bloodnamedFirst();
        }
        Person commander = null;
        if (order == null) {
            commander = engineCommanderOf(pass, formation);
        }
        if (commander == null) {
            commander = pickCommander(pass.campaign(), formation, pass.promoted(), order);
        }
        if ((commander != null) && needsBloodname && !OfficerSelector.hasBloodname(commander)) {
            // Nobody left in the formation holds a Bloodname, and the post is one a Clan gives only to the
            // Bloodnamed: the warrior taking it is held to have won one.
            pass.campaign().getPlayerForce().getHumanResources().checkBloodnameAdd(pass.campaign(), commander, true);
            LOGGER.info("[CompanyGen][RankAssign][Pick]   formation '{}': no Bloodnamed warrior was left for a post"
                        + " of Star Colonel or above; '{}' takes it and {}", formation.getName(),
                  commander.getFullName(), OfficerSelector.hasBloodname(commander)
                        ? "is awarded the Bloodname " + commander.getBloodname()
                        : "could not be awarded a Bloodname (not Clan personnel, or no phenotype)");
        }
        if (commander != null) {
            setRankWithFallback(commander, rankIndex, pass.targetRankSystem(), pass.rankValidator());
            pass.promoted().add(commander);
            pass.officers().put(commander, level);
            pass.officerCount()[0]++;
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> '{}' promoted to rank index {} (effective={})",
                  formation.getName(), level, commander.getFullName(), rankIndex,
                  commander.getRankNumeric());
        } else {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no unpromoted combat Person available",
                  formation.getName(), level);
        }
        return commander;
    }

    /**
     * @return the commander the engine designated for the formation, when it was built, is combat crew and is
     *       not yet promoted; {@code null} otherwise
     */
    @Nullable
    private static Person engineCommanderOf(Pass pass, Formation formation) {
        Person designated = pass.guidance().engineCommanders().get(formation);
        if (designated == null) {
            return null;
        }
        boolean isAvailable = designated.isCombat() && !pass.promoted().contains(designated);
        if (!isAvailable) {
            LOGGER.info("[CompanyGen][RankAssign][Pick]   formation '{}': the engine's commander '{}' is already promoted or not combat crew; choosing another",
                  formation.getName(), designated.getFullName());
            return null;
        }
        LOGGER.info("[CompanyGen][RankAssign][Pick]   formation '{}': the engine's commander {} takes the post",
              formation.getName(), OfficerSelector.describe(pass.campaign(), designated));
        return designated;
    }

    /**
     * The command level of the force as a whole when the guidance does not say, taken from the echelon the user
     * asked the generator for rather than from the Formation's own level.
     *
     * <p>The Formation's level cannot be trusted here. Generation fires an
     * {@link mekhq.campaign.events.OrganizationChangedEvent}, and that recomputes every level in the
     * tree from its depth - deepest formation counts as 1, each parent one higher. That model is
     * built for the Inner Sphere ladder of Lance, Company, Battalion. A Clan force has an extra rung
     * in it, Point below Star below Binary or Trinary below Cluster, so every level above the bottom
     * comes out one too high and the campaign's own root force lands above the Cluster entirely. A
     * requested Cluster was being commanded by a Khan.</p>
     *
     * @param campaign the campaign, for resolving a depth to its faction-family level
     * @param options  the generation options, carrying the echelon that was asked for
     * @param root     the campaign's root Formation, used only as a fallback
     *
     * @return the level to rank the root commander at, or {@code null} if neither source knows
     */
    private static @Nullable FormationLevel requestedRootLevel(Campaign campaign,
          CommandGenerationOptions options, Formation root) {
        Integer echelon = options.getForceDescriptorSnapshot().getEchelon();
        if (echelon == null) {
            LOGGER.info("[CompanyGen][RankAssign] no echelon requested; falling back to the Formation's own level {}",
                  root.getFormationLevel());
            return root.getFormationLevel();
        }

        FormationLevel level = ForceDescriptorWalker.mapEchelonToFormationLevel(echelon,
              campaign.getPlayerForce().getFaction().getShortName());
        if (level == null) {
            LOGGER.info("[CompanyGen][RankAssign] echelon {} has no level mapping; falling back to the Formation's own level {}",
                  echelon, root.getFormationLevel());
            return root.getFormationLevel();
        }
        return level;
    }

    /**
     * @param campaign the campaign, whose faction decides which ladder a depth resolves against
     * @param level    the parent's level, or {@code null} when it is unknown
     *
     * @return the level one rung below {@code level}, or {@code null} once the bottom is reached
     */
    private static @Nullable FormationLevel oneLevelBelow(Campaign campaign,
          @Nullable FormationLevel level) {
        if (level == null) {
            return null;
        }

        int depth = level.getDepth() - 1;
        return (depth < 0) ? null : FormationLevel.parseFromDepth(campaign, depth);
    }

    /**
     * Finds the combat Person in the formation's subtree to promote: the first not yet promoted, or the best of
     * them when an order is given.
     *
     * @param bestFirst the order to choose by, best first, or {@code null} to take the first found
     */
    @Nullable
    private static Person pickCommander(Campaign campaign, Formation formation, Set<Person> promoted,
          @Nullable Comparator<Person> bestFirst) {
        // A single-seat pilot is listed as both driver and gunner of their unit, so the crew walk names them
        // twice; a set keeps each candidate once.
        Set<Person> candidates = new LinkedHashSet<>();
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            for (Person person : unitCrew(unit)) {
                if (person == null) {
                    continue;
                }
                if (promoted.contains(person)) {
                    continue;
                }
                if (!person.isCombat()) {
                    continue;
                }
                if (bestFirst == null) {
                    return person;
                }
                candidates.add(person);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<Person> ranked = new ArrayList<>(candidates);
        ranked.sort(bestFirst);
        Person chosen = ranked.getFirst();
        String runnerUp = (ranked.size() > 1) ? OfficerSelector.describe(campaign, ranked.get(1)) : "nobody";
        LOGGER.info("[CompanyGen][RankAssign][Pick]   formation '{}': {} chosen over {} other(s); next best {}",
              formation.getName(), OfficerSelector.describe(campaign, chosen), ranked.size() - 1, runnerUp);
        return chosen;
    }

    /** The CLAN ladder's slot for the leader of a Star's first Point; Point 2 to Point 5 follow it. */
    static final int POINT_ONE_RANK_INDEX = 6;
    /** A Star has five Points, so the Point ranks stop here. */
    private static final int POINTS_IN_A_STAR = 5;

    /**
     * The rank index for a formation's commander, on the Clan table (Sarna, Clans: Ranks and Organization): a
     * Point's leader is a Point Commander, a Star's a Star Commander, a Nova's (a Mek Star and an Elemental Star)
     * a Nova Commander, a Binary's or Trinary's a Star Captain, a Supernova's (Novas paired) a Nova Captain, a
     * Cluster's a Star Colonel, a Galaxy's a Galaxy Commander. Other families take the level's slot as it is.
     *
     * @param campaign         the campaign, to read the formation's units
     * @param formation        the formation
     * @param level            its level, or {@code null} when unknown
     * @param isClanCommand    {@code true} when the command is a Clan one, so a Point's leader is ranked
     * @param positionInParent the formation's place among its parent's sub-formations, from zero
     *
     * @return the rank index, or {@code -1} when the level has no officer slot
     */
    static int rankIndexFor(Campaign campaign, Formation formation, @Nullable FormationLevel level,
          boolean isClanCommand, int positionInParent) {
        if ((level == FormationLevel.TEAM) && isClanCommand) {
            return pointRankIndex(positionInParent);
        }
        if ((level == FormationLevel.STAR_OR_NOVA) && isNova(unitTypesIn(campaign, formation))) {
            return Rank.RWO_MAX + 3;   // Nova Commander
        }
        if ((level == FormationLevel.BINARY_OR_TRINARY) && holdsANova(campaign, formation)) {
            return Rank.RWO_MAX + 5;   // Nova Captain, for a Supernova
        }
        return rankIndexForLevel(level);
    }

    /**
     * @param positionInParent the Point's place in its Star, from zero
     *
     * @return the CLAN ladder slot for that Point's leader, Point 1 to Point 5
     */
    static int pointRankIndex(int positionInParent) {
        int point = Math.min(Math.max(positionInParent, 0), POINTS_IN_A_STAR - 1);
        return POINT_ONE_RANK_INDEX + point;
    }

    /**
     * @return {@code true} when any of the formation's own sub-formations is a Nova, which makes a Binary or
     *       Trinary a Supernova; a Binary of a Mek Star beside an Elemental Star is not one
     */
    private static boolean holdsANova(Campaign campaign, Formation formation) {
        for (Formation sub : formation.getSubFormations()) {
            if (isNova(unitTypesIn(campaign, sub))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param unitTypes the {@link UnitType} values of a formation's units
     *
     * @return {@code true} when the formation mixes Meks (or fighters, or vehicles) with battle armour, which is
     *       what makes a Star a Nova
     */
    static boolean isNova(Set<Integer> unitTypes) {
        boolean hasElementals = unitTypes.contains(UnitType.BATTLE_ARMOR);
        boolean hasMachines = unitTypes.contains(UnitType.MEK) || unitTypes.contains(UnitType.AEROSPACE_FIGHTER)
              || unitTypes.contains(UnitType.TANK) || unitTypes.contains(UnitType.VTOL)
              || unitTypes.contains(UnitType.PROTOMEK);
        return hasElementals && hasMachines;
    }

    private static Set<Integer> unitTypesIn(Campaign campaign, Formation formation) {
        Set<Integer> unitTypes = new HashSet<>();
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && (unit.getEntity() != null)) {
                unitTypes.add(unit.getEntity().getUnitType());
            }
        }
        return unitTypes;
    }

    /**
     * Returns every Person attached to a unit — drivers + gunners + vessel crew + navigator. We
     * collect manually instead of using {@code getActiveCrew()} because the latter filters out
     * wounded Tank / Infantry pilots, and at generation time we want to count every assigned slot.
     */
    private static List<Person> unitCrew(Unit unit) {
        List<Person> out = new ArrayList<>();
        if (unit.getDrivers() != null) {
            out.addAll(unit.getDrivers());
        }
        if (unit.getGunners() != null) {
            out.addAll(unit.getGunners());
        }
        if (unit.getVesselCrew() != null) {
            out.addAll(unit.getVesselCrew());
        }
        Person navigator = unit.getNavigator();
        if (navigator != null) {
            out.add(navigator);
        }
        return out;
    }

    /**
     * Maps {@link FormationLevel} to the {@code Person.setRank(int)} index for that formation's
     * commander, covering every echelon the ratgen engine can produce. Cross-checked against
     * {@code data/universe/ranks.xml} slots and
     * {@code MekHQ/battletech_rank_command_mapping.xlsx} (the per-faction rank-to-command
     * authoritative reference).
     *
     * <p>Each FormationLevel enum value tells us the faction family (Inner Sphere uses LANCE /
     * COMPANY / ...; Clan uses STAR_OR_NOVA / BINARY_OR_TRINARY / ...; ComStar / WoB uses
     * LEVEL_II_OR_CHOIR / LEVEL_III / ...), so the switch can pick the family-correct slot without
     * a separate faction argument. See the rank-slot conventions block comment at the top of
     * {@code ranks.xml} for the canonical command-size-to-slot mapping per family.</p>
     *
     * <p>Package-private so {@code RulesetRankAssignerTest} can pin the mapping without going
     * through the full {@link #apply(Campaign, CommandGenerationOptions)} pipeline.</p>
     */
    static int rankIndexForLevel(FormationLevel level) {
        if (level == null) {
            return -1;
        }
        return switch (level) {
            // Inner Sphere / Periphery — uses STANDARD-style officer slots
            case LANCE -> Rank.RWO_MAX + 3;       // Lieutenant
            case COMPANY -> Rank.RWO_MAX + 4;     // Captain
            case BATTALION -> Rank.RWO_MAX + 5;   // Major
            case REGIMENT -> Rank.RWO_MAX + 8;    // Colonel (NOT O7 - O7 is empty in every IS rank XML)
            case BRIGADE -> Rank.RWO_MAX + 9;     // Brigadier General / Lieutenant General
            case DIVISION -> Rank.RWO_MAX + 10;   // Major General
            case CORPS -> Rank.RWO_MAX + 11;      // General
            case ARMY -> Rank.RWO_MAX + 12;       // Marshal / Major General (per faction)
            case ARMY_GROUP -> Rank.RWO_MAX + 13; // Field Marshal / General of the Armies

            // Clan — uses CLAN rank XML slots
            case STAR_OR_NOVA -> Rank.RWO_MAX + 2;          // Star Commander; rankIndexFor raises a
                                                            // Nova, a Star mixing Meks and Elementals,
                                                            // to Nova Commander
            case BINARY_OR_TRINARY -> Rank.RWO_MAX + 4;     // Star Captain
            case CLUSTER -> Rank.RWO_MAX + 8;               // Star Colonel
            case GALAXY -> Rank.RWO_MAX + 9;                // Galaxy Commander
            case TOUMAN -> Rank.RWO_MAX + 18;               // Khan

            // ComStar / Word of Blake — uses COMSTAR rank XML slots
            case LEVEL_II_OR_CHOIR -> Rank.RWO_MAX + 3;     // Adept
            case LEVEL_III -> Rank.RWO_MAX + 4;             // Demi-Precentor
            case LEVEL_IV -> Rank.RWO_MAX + 7;              // Precentor
            case LEVEL_V -> Rank.RWO_MAX + 7;               // Precentor (canon has no intermediate
                                                            // rank between Precentor and Precentor
                                                            // Martial; LEVEL_V and LEVEL_IV share)
            case LEVEL_VI -> Rank.RWO_MAX + 12;             // Precentor Martial

            default -> -1;
        };
    }

    /**
     * Returns the enlisted rank index for non-officer combat crew, matching the legacy generator's
     * convention: Sergeant-equivalent for Inner Sphere / Periphery (E12), lower index for Clan and
     * ComStar / WoB rank systems (E4).
     */
    private static int enlistedRankForFaction(Faction faction) {
        return (faction.isComStarOrWoB() || faction.isClan()) ? 4 : 12;
    }

    /**
     * Returns the rank index for support personnel (techs, doctors, admins). Mirrors the legacy
     * generator's {@code generateSupportPerson} switch: Corporal-equivalent for IS / Periphery,
     * lower index for Clan / ComStar / WoB.
     *
     * <p>Package-private so {@code SupportPersonnelGenerator} can share the same faction-to-rank
     * mapping when it creates fresh techs, doctors, admins, astechs, and medics.</p>
     */
    static int supportRankForFaction(Faction faction) {
        return (faction != null && (faction.isComStarOrWoB() || faction.isClan())) ? 4 : 8;
    }

    // Rank indices, using the bands fixed by Rank: enlisted 0-20, warrant 21-30, officer 31-50. The
    // warrant band is left alone because the shipped rank systems barely populate it - one of twenty
    // names anything there - so a warrant rank would almost always fall back anyway.
    private static final int RANK_CORPORAL = 8;         // E8
    private static final int RANK_SERGEANT = 12;        // E12
    private static final int RANK_SENIOR_SERGEANT = 16; // E16
    private static final int RANK_MASTER_SERGEANT = 20; // E20, top of the enlisted band
    private static final int RANK_LIEUTENANT = 33;      // O3
    private static final int RANK_CAPTAIN = 34;         // O4 - the ceiling for a post-less officer

    /**
     * The rank a generated support person should hold, from their role and how good they are.
     *
     * <p>Support staff were all given one index regardless of role or skill, which in the shipped rank
     * systems is Corporal - so a command's chief surgeon and its greenest astech ranked identically.
     * This spreads them across the ladder instead, so skill shows in the roster.</p>
     *
     * <p>Which band a role sits in follows real practice rather than canon, because canon does not
     * define separate medical or technical rank ladders - factions publish one ladder and MekHQ renders
     * it through each profession's column. Physicians and administrators are commissioned and sit in
     * the officer band; technicians and medics are enlisted and top out at the senior NCO ranks.</p>
     *
     * <p>Commissioned staff top out at Captain. Rank follows position rather than competence: mapping
     * skill straight onto seniority made every doctor in a command generated at Veteran a Major, and
     * every doctor in an Elite one a Lieutenant Colonel, because the generator creates the whole
     * cohort at a single experience level. Field grade is instead earned by holding a post -
     * {@link SeniorAppointmentAssigner} raises department heads above their staff and branch heads
     * above them - which produces a pyramid that scales with the size of the command rather than a
     * flat block of senior officers.</p>
     *
     * <p>Sparse rank systems are handled by the caller, which walks down from the requested index
     * until it finds a rank that profession actually names.</p>
     *
     * <p>Clan and ComStar/WoB keep a single flat index rather than this ladder. For the Clans that
     * index now resolves to real names - the rank system gives technicians, medics and administrators
     * their own columns, so one index reads as Technician, Doctor or Administrator according to the
     * holder's profession, and the rung above it carries the department heads. ComStar and WoB still
     * borrow the warrior column and remain outstanding work.</p>
     *
     * @param role    the support role being generated
     * @param skill   the skill level that person was generated at
     * @param faction the faction the command is being generated for
     *
     * @return the preferred rank index
     */
    static int supportRankFor(PersonnelRole role, SkillLevel skill, Faction faction) {
        if ((faction != null) && (faction.isComStarOrWoB() || faction.isClan())) {
            return supportRankForFaction(faction);
        }
        return isCommissionedSupportRole(role) ? commissionedRank(skill) : enlistedSupportRank(skill);
    }

    /**
     * Whether a support role is commissioned. Physicians and administrators hold commissions in the
     * services this ladder is modelled on; technicians, astechs and medics do not.
     */
    private static boolean isCommissionedSupportRole(PersonnelRole role) {
        return switch (role) {
            case DOCTOR, ADMINISTRATOR -> true;
            default -> false;
        };
    }

    private static int commissionedRank(SkillLevel skill) {
        if (skill == null) {
            return RANK_LIEUTENANT;
        }
        return switch (skill) {
            case NONE, ULTRA_GREEN, GREEN, REGULAR -> RANK_LIEUTENANT;
            // Veteran and above share Captain, the ceiling for a post-less officer. Anything higher is
            // reserved for the heads the appointment pass promotes.
            default -> RANK_CAPTAIN;
        };
    }

    private static int enlistedSupportRank(SkillLevel skill) {
        if (skill == null) {
            return RANK_SERGEANT;
        }
        return switch (skill) {
            case NONE, ULTRA_GREEN, GREEN -> RANK_CORPORAL;
            case REGULAR -> RANK_SERGEANT;
            case VETERAN -> RANK_SENIOR_SERGEANT;
            // Elite and above share the top of the enlisted band; the warrant band is unusable.
            default -> RANK_MASTER_SERGEANT;
        };
    }

    /**
     * Switches the Person's rank system to the target faction's, then assigns the preferred rank
     * index — walking DOWN the rank table if that index has no name for the Person's profession.
     *
     * <p>Two-step assignment is necessary because a Person's rank-name lookup goes through its
     * own {@code RankSystem} instance, not the campaign's. If a Mercenary campaign generates a
     * Clan force, the freshly-created Persons inherit the Mercenary rank system on construction;
     * setting their rank index to 33 then renders as "Lieutenant" (the IS slot 33 name) instead
     * of "Nova Commander" (the Clan slot 33 name). Swapping the rank system first ensures the
     * index resolves through the correct table.</p>
     *
     * <p>{@link Profession#getProfessionFromBase(RankSystem, Rank)} already walks alternate
     * profession columns; this method handles the case where every profession in that walk
     * is empty at the chosen rank index by stepping down through lower indices until it
     * finds one that resolves to a real name.</p>
     */
    static void setRankWithFallback(Person person, int preferredIndex,
          RankSystem targetRankSystem, RankValidator rankValidator) {
        if (targetRankSystem != null) {
            RankSystem currentSystem = person.getRankSystem();
            if (currentSystem == null || !targetRankSystem.equals(currentSystem)) {
                LOGGER.info("[CompanyGen][RankAssign][RankSystem] swap person='{}' role={} oldSystem={} newSystem={} preferredIndex={}",
                      person.getFullName(), person.getPrimaryRole().name(),
                      currentSystem == null ? "null" : currentSystem.getCode(),
                      targetRankSystem.getCode(), preferredIndex);
                person.setRankSystem(rankValidator, targetRankSystem);
            } else {
                LOGGER.info("[CompanyGen][RankAssign][RankSystem] no-swap person='{}' already on system={} preferredIndex={}",
                      person.getFullName(), targetRankSystem.getCode(), preferredIndex);
            }
        } else {
            LOGGER.warn("[CompanyGen][RankAssign][RankSystem] targetRankSystem is null for person='{}' - leaving on existing system={} (this is the wrong-rank-names path)",
                  person.getFullName(),
                  person.getRankSystem() == null ? "null" : person.getRankSystem().getCode());
        }
        person.setRank(preferredIndex);
        RankSystem rankSystem = person.getRankSystem();
        if (rankSystem == null) {
            return;
        }
        Profession base = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());

        int safeIndex = Math.min(preferredIndex, rankSystem.getRanks().size() - 1);
        for (int i = safeIndex; i >= 0; i--) {
            Rank candidate = rankSystem.getRank(i);
            if (candidate == null) {
                continue;
            }
            Profession effective = base.getProfession(rankSystem, candidate);
            if (!candidate.isEmpty(effective)) {
                if (i != preferredIndex) {
                    LOGGER.warn("[CompanyGen][RankAssign] preferred rank {} empty for {} (profession={}); falling back to {}",
                          preferredIndex, person.getFullName(), effective, i);
                    person.setRank(i);
                }
                return;
            }
        }
        LOGGER.warn("[CompanyGen][RankAssign] no valid rank found for {} (preferred={}); leaving as-is",
              person.getFullName(), preferredIndex);
    }
}
