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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.utilities.PityContracts;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Determines how many contract offers of each {@link ContractSearchType} become available each month at the campaign's
 * current location, and drives the monthly refresh of the player force's {@link ContractMarket}.
 *
 * <p>Availability is driven primarily by the current system's {@link HiringHallLevel}: a better hiring hall both
 * raises the number of offer <em>slots</em> and the chance each slot yields an offer. A result of {@code 0} is always
 * possible, and {@link ContractSearchType#TOURNAMENT} is always {@code 0} (tournament bouts are not drawn from the
 * hiring-hall market).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ChaosContractMarketAvailability {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int DIE_SIZE = 6;
    /** Success targets are capped one below the die size so every slot can fail and a result of 0 stays possible. */
    private static final int MAX_SUCCESS_TARGET = DIE_SIZE - 1;
    private static final int MIN_SUCCESS_TARGET = 1;
    /** The most government offers a government campaign sees when it is off its own faction's worlds. */
    private static final int OFF_FACTION_GOVERNMENT_CAP = 1;

    private ChaosContractMarketAvailability() {}

    /**
     * The base number of offer slots the given hiring hall supports, before per-search-type adjustment. Each slot is an
     * independent chance at an offer, so this caps how many offers a search type can produce in a month.
     */
    static int baseOfferSlots(final HiringHallLevel level) {
        return switch (level) {
            case NONE, QUESTIONABLE -> 1;
            case MINOR -> 2;
            case STANDARD -> 3;
            case GREAT -> 4;
        };
    }

    /**
     * The base d6 target (a slot yields an offer on a roll {@code <=} this) for the given hiring hall, before planetary
     * and per-search-type modifiers.
     */
    static int baseSuccessTarget(final HiringHallLevel level) {
        return switch (level) {
            case NONE -> 1;
            case QUESTIONABLE -> 2;
            case MINOR -> 3;
            case STANDARD -> 4;
            case GREAT -> 5;
        };
    }

    /**
     * Per-search-type adjustment to a hiring hall's offer slots. A {@link HiringHallLevel#QUESTIONABLE} (grey-market)
     * hall leans toward piracy, giving pirate work an extra slot; all other types and levels are unchanged.
     */
    static int offerSlotModifier(final ContractSearchType type, final HiringHallLevel level) {
        return ((level == HiringHallLevel.QUESTIONABLE) && (type == ContractSearchType.PIRATE)) ? 1 : 0;
    }

    /**
     * Per-search-type adjustment to a slot's success target. A {@link HiringHallLevel#QUESTIONABLE} hall trades
     * legitimate mercenary work ({@code -1}) for pirate contracts ({@code +1}); all other types and levels are
     * unchanged.
     */
    static int successTargetModifier(final ContractSearchType type, final HiringHallLevel level) {
        if (level != HiringHallLevel.QUESTIONABLE) {
            return 0;
        }
        return switch (type) {
            case MERCENARY -> -1;
            case PIRATE -> 1;
            default -> 0;
        };
    }

    /**
     * The full breakdown of rolling one search type's monthly offers: the slot count and success target with the
     * modifiers that produced them, every die rolled, the raw number of successes, and the final {@link #count()} after
     * any post-roll cap (the government cap in {@link #rollMonthlyOffers(Campaign)}). Drives the skill-check report.
     *
     * @param type         the search type rolled
     * @param slots        how many offer slots were rolled
     * @param baseTarget   the hiring hall's base success target
     * @param typeModifier the per-search-type target modifier applied
     * @param finalTarget  the clamped success target each die was compared against
     * @param dice         each die rolled, in order
     * @param rawSuccesses the number of dice that met the target, before any cap
     * @param count        the final number of offers after any post-roll cap
     * @param capped       whether a post-roll cap reduced {@code count} below {@code rawSuccesses}
     */
    public record OfferRoll(ContractSearchType type, int slots, int baseTarget, int typeModifier, int finalTarget,
          List<Integer> dice, int rawSuccesses, int count, boolean capped) {
        OfferRoll cappedTo(final int cap) {
            return new OfferRoll(type, slots, baseTarget, typeModifier, finalTarget, dice, rawSuccesses,
                  cap, true);
        }
    }

    /**
     * Rolls one search type's monthly offers, capturing the full breakdown. The hiring hall's
     * {@link #baseOfferSlots(HiringHallLevel)} (adjusted by
     * {@link #offerSlotModifier(ContractSearchType, HiringHallLevel)}) are each a separate d6 that succeeds on
     * {@code <= finalTarget}; the target starts at {@link #baseSuccessTarget(HiringHallLevel)}, takes the per-type
     * {@link #successTargetModifier(ContractSearchType, HiringHallLevel)}, and is clamped to
     * {@value #MIN_SUCCESS_TARGET}..{@value #MAX_SUCCESS_TARGET} so 0 is always reachable.
     *
     * @param type  the search type being rolled
     * @param level the current system's hiring hall level
     *
     * @return the full roll breakdown, before any post-roll cap
     */
    public static OfferRoll rollOffer(final ContractSearchType type, final HiringHallLevel level) {
        final int slots = Math.max(0, baseOfferSlots(level) + offerSlotModifier(type, level));
        final int typeModifier = successTargetModifier(type, level);
        final int baseTarget = baseSuccessTarget(level);
        final int finalTarget = Math.clamp(baseTarget + typeModifier, MIN_SUCCESS_TARGET,
              MAX_SUCCESS_TARGET);

        final List<Integer> dice = new ArrayList<>();
        int successes = 0;
        for (int slot = 0; slot < slots; slot++) {
            final int roll = Compute.d6();
            dice.add(roll);
            if (roll <= finalTarget) {
                successes++;
            }
        }
        return new OfferRoll(type, slots, baseTarget, typeModifier, finalTarget, dice, successes,
              successes, false);
    }

    /**
     * Rolls the number of offers available this month for one search type - a thin wrapper over {@link #rollOffer}.
     *
     * @return the number of offers of this type available this month
     */
    public static int rollOfferCount(final ContractSearchType type, final HiringHallLevel level) {
        return rollOffer(type, level).count();
    }

    /**
     * Rolls this month's offer breakdown for every real search type at the campaign's current location.
     * {@link ContractSearchType#TOURNAMENT} is never offered and is omitted; the rest are rolled from the current
     * system's hiring hall level.
     *
     * <p>An uninhabited or abandoned system (zero population) has no legitimate market and draws only
     * {@link ContractSearchType#PIRATE} contracts; mercenary and government offers are omitted there.</p>
     *
     * <p>Government orders are only offered to government (non-mercenary, non-pirate) forces; for mercenary and pirate
     * forces {@link ContractSearchType#GOVERNMENT} is omitted entirely. For a government force, government offers also
     * concentrate on its own faction's worlds: off a world its faction controls they are capped at
     * {@value #OFF_FACTION_GOVERNMENT_CAP} (recorded on the returned {@link OfferRoll}).</p>
     *
     * @param campaign the active campaign
     *
     * @return a per-search-type roll breakdown (tournament always omitted; government omitted for non-government
     *       forces)
     */
    public static Map<ContractSearchType, OfferRoll> rollMonthlyOffers(final Campaign campaign) {
        final HiringHallLevel level = campaign.getSystemHiringHallLevel();
        final Faction playerFaction = campaign.getPlayerForce().getFaction();
        final boolean governmentForce = isGovernmentFaction(playerFaction);
        final boolean uninhabited = isUninhabitedSystem(campaign);

        final Map<ContractSearchType, OfferRoll> rolls = new EnumMap<>(ContractSearchType.class);
        for (final ContractSearchType type : ContractSearchType.values()) {
            if (type == ContractSearchType.TOURNAMENT) {
                continue; // tournament bouts are never drawn from the hiring-hall market
            }
            if (uninhabited && (type != ContractSearchType.PIRATE)) {
                continue; // uninhabited or abandoned systems draw only pirate contracts - there is no legitimate market
            }
            if ((type == ContractSearchType.GOVERNMENT) && !governmentForce) {
                continue; // government orders are only offered to government (non-mercenary, non-pirate) forces
            }
            rolls.put(type, rollOffer(type, level));
        }

        // Government contracts concentrate on the player's own faction's worlds.
        if (governmentForce && !isOnFactionControlledWorld(campaign, playerFaction)) {
            final OfferRoll government = rolls.get(ContractSearchType.GOVERNMENT);
            if ((government != null) && (government.count() > OFF_FACTION_GOVERNMENT_CAP)) {
                rolls.put(ContractSearchType.GOVERNMENT, government.cappedTo(OFF_FACTION_GOVERNMENT_CAP));
            }
        }

        return rolls;
    }

    /** A government campaign is any player force that is neither a mercenary command nor a pirate band. */
    static boolean isGovernmentFaction(final Faction faction) {
        return !faction.isMercenary() && !faction.isPirate();
    }

    /**
     * An uninhabited or abandoned system (zero population) has no legitimate contract market, so it draws only pirate
     * contracts.
     */
    static boolean isUninhabitedSystem(final Campaign campaign) {
        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        // With no known system there is nothing to call uninhabited; leave the market unrestricted rather than
        // narrowing it to pirate work on missing data.
        return (currentSystem != null) && (currentSystem.getPopulation(campaign.getLocalDate()) == 0);
    }

    /** Whether the campaign's current system is controlled (at least in part) by the given faction at the current date. */
    static boolean isOnFactionControlledWorld(final Campaign campaign, final Faction faction) {
        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        if (currentSystem == null) {
            return false;
        }

        final List<String> factions = currentSystem.getFactions(campaign.getLocalDate());
        return (factions != null) && factions.contains(faction.getShortName());
    }

    /** The search types listed in the reports, in a stable order (tournament is never offered, so it is excluded). */
    private static final List<ContractSearchType> REPORTED_TYPES =
          List.of(ContractSearchType.MERCENARY, ContractSearchType.PIRATE, ContractSearchType.GOVERNMENT);

    /**
     * The first-of-month contract-market refresh: rolls this month's offers, regenerates the player force's
     * {@link ContractMarket} to match, and posts two daily reports - a plain-language summary to
     * {@link DailyReportType#GENERAL}, and the exact rolls and their modifiers to
     * {@link DailyReportType#SKILL_CHECKS}.
     *
     * <p>Each search type's map is cleared and repopulated, so last month's unaccepted offers expire. Generation is
     * best-effort: a search type whose generator cannot place a contract yields fewer offers than rolled. The summary
     * counts what actually reached the market, since that is what the player can go and look at; the skill-check
     * breakdown reports the rolls themselves, shortfall included, as the audit of the determination.</p>
     *
     * @param campaign the active campaign
     */
    public static void processNewMonth(final Campaign campaign) {
        if (campaign.getCampaignOptions().get(CampaignOption.CONTRACT_MARKET_METHOD).isNone()) {
            return;
        }

        final Map<ContractSearchType, OfferRoll> rolls = rollMonthlyOffers(campaign);
        final ContractMarket market = campaign.getPlayerForce().getContractMarket();

        for (final ContractSearchType type : ContractSearchType.values()) {
            final Map<UUID, AbstractContract> offers = market.getContracts(type);
            offers.clear();
            final int count = offerCount(rolls, type);
            for (final AbstractContract contract : generateOffers(campaign, count, false, type)) {
                offers.put(contract.getId(), contract);
            }
        }

        // Top the market up with easy "Proving Ground" offers if the force has not yet earned enough successful
        // contracts. These land in one of the reported buckets and are counted alongside the rolled offers.
        PityContracts.generatePityContracts(campaign);

        // Count what actually landed in the market per type
        final Map<ContractSearchType, Integer> generatedCounts = new EnumMap<>(ContractSearchType.class);
        int marketOffers = 0;
        for (final ContractSearchType type : ContractSearchType.values()) {
            final int count = market.getContracts(type).size();
            generatedCounts.put(type, count);
            marketOffers += count;
        }

        campaign.addReport(DailyReportType.GENERAL, buildSummaryReport(generatedCounts, marketOffers));
        campaign.addReport(DailyReportType.SKILL_CHECKS, buildRollBreakdownReport(campaign, rolls));
    }

    /** The final offer count for a search type, or 0 when it was not rolled (e.g. tournament). */
    private static int offerCount(final Map<ContractSearchType, OfferRoll> rolls, final ContractSearchType type) {
        final OfferRoll roll = rolls.get(type);
        return (roll == null) ? 0 : roll.count();
    }

    /**
     * Builds the {@link DailyReportType#GENERAL} summary in the personnel-market report style: a hyperlink header that
     * opens the contract market, then one bold, colored line per type that has offers
     * ({@code <b>count</b> <b>Type</b> contract(s) available}). When nothing is available, a plain "no contracts" line
     * is returned instead.
     */
    private static String buildSummaryReport(final Map<ContractSearchType, Integer> generatedCounts,
          final int totalOffers) {
        if (totalOffers <= 0) {
            return getTextAt(RESOURCE_BUNDLE, "dailyReport.contractMarket.none");
        }

        final StringBuilder report = new StringBuilder(getTextAt(RESOURCE_BUNDLE, "hyperlink.contractMarket.report"));
        for (final ContractSearchType type : REPORTED_TYPES) {
            final int count = generatedCounts.getOrDefault(type, 0);
            if (count <= 0) {
                continue;
            }
            report.append("<br>")
                  .append(getFormattedTextAt(RESOURCE_BUNDLE,
                        "hyperlink.contractMarket.report.type",
                        count,
                        spanOpeningWithCustomColor(reportColor(type)),
                        getTextAt(RESOURCE_BUNDLE, "report.contractMarket.type." + type.name()),
                        CLOSING_SPAN_TAG,
                        (count > 1) ? 1 : 0));
        }
        return report.toString();
    }

    /** The report color for a search type: mercenary positive, pirate negative, government warning. */
    private static String reportColor(final ContractSearchType type) {
        return switch (type) {
            case MERCENARY -> getPositiveColor();
            case PIRATE -> getNegativeColor();
            default -> getWarningColor();
        };
    }

    /**
     * Builds the {@link DailyReportType#SKILL_CHECKS} breakdown: a header giving the location and hiring hall level,
     * then one line per search type showing its slots, target (with each modifier), the individual dice, the resulting
     * offers, and any cap applied. Lines are joined with {@code <br>} for the HTML report pane.
     */
    private static String buildRollBreakdownReport(final Campaign campaign,
          final Map<ContractSearchType, OfferRoll> rolls) {
        final HiringHallLevel level = campaign.getSystemHiringHallLevel();
        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        final String systemName = (currentSystem == null) ? "-" : currentSystem.getName(campaign.getLocalDate());
        final StringBuilder report = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE,
              "skillCheck.contractMarket.header",
              systemName,
              level.name()));

        for (final ContractSearchType type : REPORTED_TYPES) {
            final OfferRoll roll = rolls.get(type);
            if (roll == null) {
                continue;
            }
            final String capNote = roll.capped()
                                         ?
                                         getFormattedTextAt(RESOURCE_BUNDLE,
                                               "skillCheck.contractMarket.capNote",
                                               roll.rawSuccesses(),
                                               roll.count())
                                         :
                                         "";
            report.append("<br>")
                  .append(getFormattedTextAt(RESOURCE_BUNDLE,
                        "skillCheck.contractMarket.line",
                        getTextAt(RESOURCE_BUNDLE, "searchType.contractMarket." + type.name()),
                        roll.slots(),
                        roll.finalTarget(),
                        roll.baseTarget(),
                        signed(roll.typeModifier()),
                        roll.dice().toString(),
                        roll.count(),
                        capNote));
        }
        return report.toString();
    }

    /** Formats a modifier with an explicit sign, e.g. {@code +1}, {@code -1}, {@code +0}. */
    private static String signed(final int value) {
        return String.format("%+d", value);
    }

    /**
     * Rolls {@code count} contracts from {@link AbstractContractGeneration} for the given search type, keeping
     * whichever come back valid (a {@code null} roll - no contract could be placed - is skipped, so the returned list
     * may be shorter than {@code count} or empty).
     *
     * @param campaign   the active campaign
     * @param count      how many contracts to attempt to generate
     * @param isGM       whether to generate in GM mode, bypassing command-circuit and placement gating
     * @param searchType the search type to generate for
     *
     * @return the generated contracts (never {@code null}, possibly empty)
     */
    public static List<AbstractContract> generateOffers(final Campaign campaign, final int count, final boolean isGM,
          final ContractSearchType searchType) {
        final List<AbstractContract> generated = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final AbstractContract contract = generateOne(campaign, searchType, isGM, false);
            if (contract != null) {
                generated.add(contract);
            }
        }
        return generated;
    }

    /**
     * Generates a single "Proving Ground" (pity) offer for the given search type: an ordinary offer that generation
     * deliberately makes easy (a veteran ally against a green enemy) and flags as a
     * {@link AbstractContract#isProvingGround() Proving Ground}. Used by {@code PityContracts} to top up a struggling
     * force's market.
     *
     * @return the generated offer, or {@code null} if generation could not place one
     */
    public static @Nullable AbstractContract generateProvingGroundOffer(final Campaign campaign,
          final ContractSearchType searchType) {
        return generateOne(campaign, searchType, false, true);
    }

    /** Assembles the generation context from the campaign and produces one contract (or {@code null}). */
    private static @Nullable AbstractContract generateOne(final Campaign campaign,
          final ContractSearchType searchType, final boolean isGM, final boolean provingGround) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final LocalDate currentDate = campaign.getLocalDate();
        final PlayerForce playerForce = campaign.getPlayerForce();
        final Detachment detachment = playerForce.getForceDetachment();
        final FactionStandings factionStandings = playerForce.getFactionStandings();
        final boolean isOverridingCommandCircuitRequirements = playerForce.isOverridingCommandCircuitRequirements();

        return AbstractContractGeneration.createContract(campaign,
              campaignOptions,
              currentDate,
              detachment,
              0,
              searchType,
              factionStandings,
              isOverridingCommandCircuitRequirements,
              isGM,
              provingGround);
    }
}
