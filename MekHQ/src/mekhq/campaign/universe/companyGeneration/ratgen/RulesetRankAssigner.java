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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

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
 *   <li>LANCE / STAR_OR_NOVA / LEVEL_II_OR_CHOIR → O3 (Lieutenant / Star Captain / Adept)</li>
 *   <li>COMPANY / BINARY_OR_TRINARY / LEVEL_III → O4 (Captain / Star Colonel / Demi-Precentor)</li>
 *   <li>BATTALION / CLUSTER / LEVEL_IV → O5 (Major / Galaxy Commander / Precentor)</li>
 *   <li>REGIMENT / GALAXY / LEVEL_V → O7 (Brigadier General / Khan / Precentor Martial)</li>
 *   <li>BRIGADE / TOUMAN / LEVEL_VI → O8 (Major General)</li>
 *   <li>DIVISION → O9, CORPS → O10, ARMY → O11, ARMY_GROUP → O12</li>
 * </ul>
 *
 * <p>Non-officer combat crew get an enlisted Sergeant rank (E12 IS, E4 Clan/CS); support staff get
 * a Corporal-equivalent (E8 IS, E4 Clan/CS) matching the legacy generator's convention. The campaign's
 * rank system (loaded from {@code data/universe/ranks.xml} per faction) maps the integer index to the
 * faction-appropriate display name.</p>
 *
 * <p>Gated on {@link CompanyGenerationOptions#isAutomaticallyAssignRanks()}. Honors
 * {@link CompanyGenerationOptions#isUseSpecifiedFactionToAssignRanks()} for the faction picker.</p>
 */
public final class RulesetRankAssigner {

    private static final MMLogger LOGGER = MMLogger.create(RulesetRankAssigner.class);

    private RulesetRankAssigner() {
        // utility class
    }

    /**
     * Runs the rank-assignment passes on the campaign's formation tree.
     *
     * @return the commander promoted at the campaign-root formation (i.e. the top-echelon commander
     *         the dialog will tag with the commander flag in Stage 7d), or {@code null} when ranks
     *         are disabled, the campaign has no formations, or the root has no eligible Person to
     *         promote
     */
    @Nullable
    public static Person apply(Campaign campaign, CompanyGenerationOptions options) {
        long startNanos = System.nanoTime();
        if (campaign == null || options == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign or options null, skipping");
            return null;
        }
        if (!options.isAutomaticallyAssignRanks()) {
            LOGGER.info("[CompanyGen][RankAssign] apply: disabled by isAutomaticallyAssignRanks");
            return null;
        }
        Formation root = campaign.getFormations();
        if (root == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign has no root Formation, skipping");
            return null;
        }

        Faction specifiedFaction = options.getSpecifiedFaction();
        Faction campaignFaction = campaign.getFaction();
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

        LOGGER.info("[CompanyGen][RankAssign] START faction={} rankSystem={} enlistedRank={} supportRank={} root='{}' thread={}",
              faction.getShortName(),
              targetRankSystem == null ? "null" : targetRankSystem.getCode(),
              enlistedRank, supportRank, root.getName(),
              Thread.currentThread().getName());

        // Pass 1: walk the tree post-order so lance / star commanders claim their officer rank
        // before their parent company / binary looks for ITS commander among remaining unranked
        // crew. Each Formation promotes the first not-yet-promoted combat Person in its subtree.
        LOGGER.info("[CompanyGen][RankAssign][Pass1] BEFORE walkPostOrder");
        long pass1Start = System.nanoTime();
        Set<Person> promoted = new LinkedHashSet<>();
        int[] officerCount = { 0 };
        Person rootCommander = walkPostOrder(campaign, root, promoted, officerCount, targetRankSystem, rankValidator);
        int officersAssigned = officerCount[0];
        long pass1Ms = (System.nanoTime() - pass1Start) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign][Pass1] AFTER walkPostOrder officers={} rootCommander={} elapsed={}ms",
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
        return rootCommander;
    }

    /**
     * Walks the formation tree post-order, promoting one Person per Formation node to that
     * Formation's officer rank.
     *
     * @param officerCount single-element counter incremented for every Person promoted (the array
     *                     wrapping lets the recursive call accumulate while we return the actual
     *                     promoted Person for the caller's formation)
     * @return the {@link Person} promoted at this formation, or {@code null} if the formation has
     *         no rank mapping or no eligible Person remained. The top-level call's return is the
     *         force commander.
     */
    @Nullable
    private static Person walkPostOrder(Campaign campaign, Formation formation, Set<Person> promoted,
          int[] officerCount, RankSystem targetRankSystem, RankValidator rankValidator) {
        for (Formation sub : formation.getSubFormations()) {
            walkPostOrder(campaign, sub, promoted, officerCount, targetRankSystem, rankValidator);
        }
        int rankIndex = rankIndexForLevel(formation.getFormationLevel());
        if (rankIndex < 0) {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no rank mapping, skip",
                  formation.getName(), formation.getFormationLevel());
            return null;
        }
        Person commander = pickCommander(campaign, formation, promoted);
        if (commander != null) {
            setRankWithFallback(commander, rankIndex, targetRankSystem, rankValidator);
            promoted.add(commander);
            officerCount[0]++;
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> '{}' promoted to rank index {} (effective={})",
                  formation.getName(), formation.getFormationLevel(),
                  commander.getFullName(), rankIndex, commander.getRankNumeric());
        } else {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no unpromoted combat Person available",
                  formation.getName(), formation.getFormationLevel());
        }
        return commander;
    }

    /**
     * Finds the first combat Person in the formation's subtree who hasn't already been promoted by
     * a deeper formation. For the MVP this is "first not-yet-promoted"; a future refinement can
     * sort by skill (combat-weighted when isPrioritizeOfficerCombatSkills is on).
     */
    private static Person pickCommander(Campaign campaign, Formation formation, Set<Person> promoted) {
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
                if (person.isCombat()) {
                    return person;
                }
            }
        }
        return null;
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
     * through the full {@link #apply(Campaign, CompanyGenerationOptions)} pipeline.</p>
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
            case REGIMENT -> Rank.RWO_MAX + 8;    // Colonel (NOT O7 — O7 is empty in every IS rank XML)
            case BRIGADE -> Rank.RWO_MAX + 9;     // Brigadier General / Lieutenant General
            case DIVISION -> Rank.RWO_MAX + 10;   // Major General
            case CORPS -> Rank.RWO_MAX + 11;      // General
            case ARMY -> Rank.RWO_MAX + 12;       // Marshal / Major General (per faction)
            case ARMY_GROUP -> Rank.RWO_MAX + 13; // Field Marshal / General of the Armies

            // Clan — uses CLAN rank XML slots
            case STAR_OR_NOVA -> Rank.RWO_MAX + 3;          // Nova Commander (covers both Stars
                                                            // and Novas; FormationLevel collapses
                                                            // the two)
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
    private static void setRankWithFallback(Person person, int preferredIndex,
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
            LOGGER.warn("[CompanyGen][RankAssign][RankSystem] targetRankSystem is null for person='{}' — leaving on existing system={} (this is the wrong-rank-names path)",
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
