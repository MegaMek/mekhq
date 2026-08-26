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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic.Category;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import mekhq.campaign.mission.contract.contractData.EmployerData;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonUtility;

/**
 * Rolls and applies a contract's random {@link ContractCharacteristic}s. This is the single home for characteristic
 * mechanics: the roll, the generation-time effects that are baked straight into the contract's stored data, and the
 * derived values (bonus payments and reputation/standing multipliers) that the accept, completion, and negotiation
 * hooks read at their own point in the contract lifecycle.
 *
 * <p>Characteristics obey a strict grammar: each acts on exactly one lever, at most one characteristic per
 * {@link Category} is ever present, and the {@code magnitude} on each constant is interpreted per category.</p>
 */
public final class ContractCharacteristics {
    private ContractCharacteristics() {}

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractAutomation";

    /**
     * How many characteristics a contract rolls, as cumulative percentile thresholds: below {@code CHANCE_NONE} it gets
     * none, below {@code CHANCE_ONE} it gets one, otherwise two. Most contracts get none - they are a flourish, not the
     * norm.
     */
    private static final int CHANCE_NONE = 55;
    private static final int CHANCE_ONE = 85;

    /**
     * The reduced monthly total is divided by this to recover the 5% that {@link ContractCharacteristic#SIGNING_BONUS}
     * shifts up front. Monthly pay is reduced to 95% at generation, and 0.05 / 0.95 = 1/19 of the reduced total is the
     * removed 5%.
     */
    private static final int SIGNING_BONUS_DIVISOR = 19;

    // region Roll & generation-time application

    /**
     * If the campaign opts in, rolls this contract's characteristics, stores them, and applies the ones that are baked
     * into the contract at generation (pay, combat pay, length, objectives). The lifecycle characteristics - the
     * negotiator tier, the bonus payments, and the reputation/standing multipliers - are left for their own hooks to
     * read. Does nothing if the option is off.
     *
     * @param contract the freshly generated contract to characterize
     * @param campaign the campaign, consulted for {@code USE_RANDOM_CONTRACT_CHARACTERISTICS} and used to re-tier the
     *                 employer negotiator
     */
    public static void rollAndApply(final AbstractContract contract, final Campaign campaign) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_CONTRACT_CHARACTERISTICS)) {
            return;
        }

        final EnumSet<ContractCharacteristic> rolled = roll();
        contract.setCharacteristics(rolled);
        applyGenerationEffects(contract, campaign);
    }

    /**
     * Rolls a fresh set of characteristics: a weighted count (usually none), then that many distinct categories, each
     * yielding one characteristic chosen by weight. Picking one characteristic per category enforces the
     * one-per-category rule for free.
     *
     * @return the rolled set, possibly empty
     */
    static EnumSet<ContractCharacteristic> roll() {
        final EnumSet<ContractCharacteristic> result = EnumSet.noneOf(ContractCharacteristic.class);

        final int count = rollCount();
        if (count == 0) {
            return result;
        }

        final List<Category> categories = new ArrayList<>(List.of(Category.values()));
        Collections.shuffle(categories);

        for (final Category category : categories) {
            if (result.size() >= count) {
                break;
            }
            final ContractCharacteristic pick = weightedPick(category);
            if (pick != null) {
                result.add(pick);
            }
        }

        return result;
    }

    private static int rollCount() {
        final int roll = Compute.randomInt(100);
        if (roll < CHANCE_NONE) {
            return 0;
        }
        if (roll < CHANCE_ONE) {
            return 1;
        }
        return 2;
    }

    /**
     * Chooses one characteristic from the given category, weighted by {@link ContractCharacteristic#getWeight()}.
     *
     * @return the chosen characteristic, or {@code null} if the category has no characteristics
     */
    private static @Nullable ContractCharacteristic weightedPick(final Category category) {
        final List<ContractCharacteristic> candidates = new ArrayList<>();
        int totalWeight = 0;
        for (final ContractCharacteristic characteristic : ContractCharacteristic.values()) {
            if (characteristic.getCategory() == category) {
                candidates.add(characteristic);
                totalWeight += characteristic.getWeight();
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int roll = Compute.randomInt(totalWeight);
        for (final ContractCharacteristic candidate : candidates) {
            roll -= candidate.getWeight();
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Applies the characteristics whose effect is baked into the contract's stored data at generation. Everything else
     * is a lifecycle characteristic and is left untouched here.
     */
    private static void applyGenerationEffects(final AbstractContract contract, final Campaign campaign) {
        for (final ContractCharacteristic characteristic : contract.getCharacteristics()) {
            switch (characteristic.getCategory()) {
                case PAY -> applyPayMultiplier(contract, characteristic.getMagnitude());
                case COMBAT_PAY -> applyCombatPayMultiplier(contract, characteristic.getMagnitude());
                case LENGTH -> applyLengthMultiplier(contract, characteristic.getMagnitude());
                case OBJECTIVES -> applyObjectiveStep(contract, (int) Math.round(characteristic.getMagnitude()));
                case NEGOTIATOR -> applyNegotiatorTier(contract, campaign);
                default -> {
                    // COMPLETION_PAYMENT, UNIT_REPUTATION, EMPLOYER_STANDING and ENEMY_STANDING are applied at their
                    // own lifecycle points (accept / completion), not at generation.
                }
            }
        }
    }

    /**
     * Re-tiers the already-generated employer negotiator to Elite or Regular. The negotiator was created (as a Veteran)
     * back at the employer step, before characteristics were rolled, so the tier is overridden here.
     */
    private static void applyNegotiatorTier(final AbstractContract contract, final Campaign campaign) {
        final SkillLevel tier = getNegotiatorSkillOverride(contract);
        if (tier == null) {
            return;
        }
        final EmployerData employerData = contract.getEmployerData();
        if (employerData == null) {
            return;
        }
        final Person negotiator = employerData.negotiator();
        if (negotiator == null) {
            return;
        }
        // checkVeterancyEligibility is false so the exact rolled tier is applied rather than being clamped.
        PersonUtility.overrideSkills(campaign, negotiator, negotiator.getPrimaryRole(), tier, false);
    }

    private static void applyPayMultiplier(final AbstractContract contract, final double multiplier) {
        contract.updateMonthlyPay(contract.getContractFinanceData().monthlyPay().multipliedBy(multiplier));
    }

    private static void applyCombatPayMultiplier(final AbstractContract contract, final double multiplier) {
        contract.updateCombatPay(contract.getContractFinanceData().combatPay().multipliedBy(multiplier));
    }

    private static void applyLengthMultiplier(final AbstractContract contract, final double multiplier) {
        final ContractScheduleData schedule = contract.getScheduleData();
        if (schedule == null) {
            return;
        }
        final int newLength = Math.max(1, (int) Math.round(schedule.lengthInMonths() * multiplier));
        final LocalDate start = schedule.startDate();
        final LocalDate end = (start != null) ? start.plusMonths(newLength) : schedule.endDate();
        contract.setScheduleData(new ContractScheduleData(start, end, newLength));
    }

    private static void applyObjectiveStep(final AbstractContract contract, final int delta) {
        contract.setRequiredVictoryPoints(Math.max(1, contract.getRequiredVictoryPoints() + delta));
    }

    // endregion Roll & generation-time application

    // region Lifecycle-hook helpers

    /**
     * Credits the signing bonus, if the contract carries one. {@link ContractCharacteristic#SIGNING_BONUS} shifts 5% of
     * total pay up front; that 5% was removed from monthly pay at generation and is handed over now, on acceptance.
     * Does nothing if there is no signing bonus. Call this from the contract-acceptance flow.
     */
    public static void paySigningBonus(final Campaign campaign, final AbstractContract contract) {
        if (!contract.hasCharacteristic(ContractCharacteristic.SIGNING_BONUS)) {
            return;
        }
        final Money bonus = contract.getTotalMonthlyPay().dividedBy(SIGNING_BONUS_DIVISOR);
        creditBonus(campaign, contract, bonus, "acceptContract.signingBonus.report");
    }

    /**
     * Credits the completion bonus, if the contract carries one and was completed successfully.
     * {@link ContractCharacteristic#COMPLETION_BONUS} pays a bonus worth a fraction of total pay on top of the
     * contract. Does nothing otherwise. Call this from the contract-completion flow.
     */
    public static void payCompletionBonus(final Campaign campaign, final AbstractContract contract,
          final MissionStatus status) {
        if (!status.isSuccess()) {
            return;
        }
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.COMPLETION_PAYMENT);
        if (characteristic == null) {
            return;
        }
        final Money bonus = contract.getTotalPay().multipliedBy(characteristic.getMagnitude());
        creditBonus(campaign, contract, bonus, "completeContract.completionBonus.report");
    }

    private static void creditBonus(final Campaign campaign, final AbstractContract contract, final Money amount,
          final String reportKey) {
        if (!amount.isPositive()) {
            return;
        }
        final String report = getFormattedTextAt(RESOURCE_BUNDLE, reportKey, contract.getName());
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(), amount, report);
        campaign.addReport(DailyReportType.GENERAL, report);
    }

    /**
     * The multiplier to apply to a unit-reputation change from this contract, given how the contract ended.
     *
     * <ul>
     *     <li>A <b>breach</b> penalty is never scaled by a reputation characteristic.</li>
     *     <li>A <b>success</b> reputation gain is scaled by all four characteristics
     *         ({@link ContractCharacteristic#HIGH_PROFILE} / {@link ContractCharacteristic#CAREER_MAKER} double it,
     *         {@link ContractCharacteristic#MEDIA_BLACKOUT} / {@link ContractCharacteristic#THANKLESS} remove it).</li>
     *     <li>A <b>non-breach loss</b> (a failed contract, only meaningful when the Hinterlands failure-loss option is
     *         on) is scaled only by the "gains and losses" pair - {@link ContractCharacteristic#HIGH_PROFILE} and
     *         {@link ContractCharacteristic#MEDIA_BLACKOUT}. The "gains only" pair
     *         ({@link ContractCharacteristic#CAREER_MAKER} / {@link ContractCharacteristic#THANKLESS}) leaves it
     *         alone.</li>
     * </ul>
     *
     * @return the multiplier, or {@code 1.0} when no unit-reputation characteristic applies to this outcome
     */
    public static double getUnitReputationMultiplier(final AbstractContract contract, final MissionStatus status) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.UNIT_REPUTATION);
        if (characteristic == null) {
            return 1.0;
        }
        if (status.isBreach()) {
            return 1.0;
        }
        if (status.isOverallSuccess()) {
            return characteristic.getMagnitude();
        }
        final boolean scalesLosses = (characteristic == ContractCharacteristic.HIGH_PROFILE)
                                           || (characteristic == ContractCharacteristic.MEDIA_BLACKOUT);
        return scalesLosses ? characteristic.getMagnitude() : 1.0;
    }

    /**
     * The multiplier to apply to the employer faction-standing change from this contract.
     *
     * @return the multiplier, or {@code 1.0} when no employer-standing characteristic applies
     */
    public static double getEmployerRegardMultiplier(final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.EMPLOYER_STANDING);
        return (characteristic == null) ? 1.0 : characteristic.getMagnitude();
    }

    /**
     * The multiplier to apply to the enemy faction-standing loss taken for this contract.
     *
     * @return the multiplier, or {@code 1.0} when no enemy-standing characteristic applies
     */
    public static double getEnemyRegardMultiplier(final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.ENEMY_STANDING);
        return (characteristic == null) ? 1.0 : characteristic.getMagnitude();
    }

    /**
     * The enemy negotiator's skill-tier override.
     *
     * @return {@link SkillLevel#ELITE} for {@link ContractCharacteristic#ELITE_NEGOTIATOR}, {@link SkillLevel#REGULAR}
     *       for {@link ContractCharacteristic#NOVICE_NEGOTIATOR}, or {@code null} to keep the default tier
     */
    public static @Nullable SkillLevel getNegotiatorSkillOverride(final AbstractContract contract) {
        if (contract.hasCharacteristic(ContractCharacteristic.ELITE_NEGOTIATOR)) {
            return SkillLevel.ELITE;
        }
        if (contract.hasCharacteristic(ContractCharacteristic.NOVICE_NEGOTIATOR)) {
            return SkillLevel.REGULAR;
        }
        return null;
    }

    // endregion Lifecycle-hook helpers

    // region GM editor helpers

    /**
     * Applies the pay characteristic ({@link Category#PAY}) to a freshly-computed monthly pay. Used by the GM editor
     * when monthly pay is on "Automatic": the base is recomputed from the generator each commit, so multiplying the
     * characteristic on top stays idempotent.
     *
     * @return {@code base} scaled by the pay characteristic, or {@code base} unchanged if the contract has none
     */
    public static Money bakeMonthlyPay(final Money base, final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.PAY);
        return (characteristic == null) ? base : base.multipliedBy(characteristic.getMagnitude());
    }

    /**
     * Applies the combat-pay characteristic ({@link Category#COMBAT_PAY}) to a freshly-computed combat pay. As
     * {@link #bakeMonthlyPay}, for use when combat pay is on "Automatic".
     *
     * @return {@code base} scaled by the combat-pay characteristic, or {@code base} unchanged if the contract has none
     */
    public static Money bakeCombatPay(final Money base, final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.COMBAT_PAY);
        return (characteristic == null) ? base : base.multipliedBy(characteristic.getMagnitude());
    }

    /**
     * Applies the objectives characteristic ({@link Category#OBJECTIVES}) to a freshly-computed required victory-point
     * count. As {@link #bakeMonthlyPay}, for use when the victory points are on "Automatic".
     *
     * @return {@code base} stepped by the objectives characteristic (never below 1), or {@code base} if there is none
     */
    public static int bakeRequiredVictoryPoints(final int base, final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.OBJECTIVES);
        return (characteristic == null) ? base : Math.max(1, base + (int) Math.round(characteristic.getMagnitude()));
    }

    /**
     * Applies the length characteristic ({@link Category#LENGTH}) to a freshly-computed contract length. As
     * {@link #bakeMonthlyPay}, for use when the length is on "Automatic".
     *
     * @return {@code base} scaled by the length characteristic (never below 1 month), or {@code base} if there is none
     */
    public static int bakeLength(final int base, final AbstractContract contract) {
        final ContractCharacteristic characteristic = contract.getCharacteristic(Category.LENGTH);
        return (characteristic == null) ? base : Math.max(1, (int) Math.round(base * characteristic.getMagnitude()));
    }

    /**
     * Sets the employer negotiator's tier to match the contract's negotiator characteristic: Elite for
     * {@link ContractCharacteristic#ELITE_NEGOTIATOR}, Regular for {@link ContractCharacteristic#NOVICE_NEGOTIATOR}, or
     * Veteran (the baseline) when neither is present. Used by the GM editor so a change of selection is reflected on
     * the negotiator; safe to call repeatedly.
     */
    public static void syncNegotiatorTier(final AbstractContract contract, final Campaign campaign) {
        final EmployerData employerData = contract.getEmployerData();
        if (employerData == null) {
            return;
        }
        final Person negotiator = employerData.negotiator();
        if (negotiator == null) {
            return;
        }
        final SkillLevel override = getNegotiatorSkillOverride(contract);
        final SkillLevel tier = (override != null) ? override : SkillLevel.VETERAN;
        PersonUtility.overrideSkills(campaign, negotiator, negotiator.getPrimaryRole(), tier, false);
    }

    // endregion GM editor helpers
}
