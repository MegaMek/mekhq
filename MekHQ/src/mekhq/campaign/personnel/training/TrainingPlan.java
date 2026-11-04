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
package mekhq.campaign.personnel.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;

/**
 * Training staged in the Training Console but not yet applied: a list of steps, each a batch of purchases the player
 * made in one go, such as "Weapon Specialist for 142 people".
 *
 * <p>Purchases are priced in the order they were staged, each against the person as the purchases before it left
 * them, so XP is never counted twice and a skill trained to level 4 can unlock an ability staged after it. Undoing or
 * removing a step prices everything after it again; anything that no longer fits is dropped.</p>
 *
 * <p>Training fees are all or nothing: a step is only staged if the campaign can pay the fees of every step staged so
 * far, including it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TrainingPlan {
    /**
     * One staged purchase and the price it was staged at.
     *
     * @param purchase the purchase
     * @param quote    its price
     */
    public record StagedPurchase(TrainingPurchase purchase, Quote quote) {}

    /**
     * A batch of purchases staged together.
     *
     * @param label     what the player bought, such as "Gunnery/Mek to level 5"
     * @param purchases the purchases, in the order they are applied
     */
    public record Step(String label, List<StagedPurchase> purchases) {
        /** @return the XP the step costs, less any it gives back */
        public int xp() {
            return purchases.stream().mapToInt(staged -> staged.quote().xp()).sum();
        }

        /** @return the step's training fees */
        public Money fees() {
            Money total = Money.zero();
            for (StagedPurchase staged : purchases) {
                total = total.plus(staged.quote().fee());
            }
            return total;
        }

        /** @return how many different people the step trains */
        public int peopleCount() {
            Set<UUID> people = new HashSet<>();
            purchases.forEach(staged -> people.add(staged.purchase().person().getId()));
            return people.size();
        }
    }

    /**
     * What happened when a batch was staged.
     *
     * @param step      the staged step, or {@code null} if nothing was staged
     * @param refused   how many purchases were refused, by reason
     * @param shortfall when the whole batch was refused because the campaign can't pay its fees, how much more it
     *                  would need; otherwise zero
     */
    public record StageResult(@Nullable Step step, Map<TrainingRefusal, Integer> refused, Money shortfall) {
        /** @return how many purchases were staged */
        public int stagedCount() {
            return (step == null) ? 0 : step.purchases().size();
        }

        /** @return how many purchases were refused */
        public int refusedCount() {
            return refused.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private final TrainingContext context;
    private final Money funds;
    private final List<Step> steps = new ArrayList<>();
    private final Map<UUID, PersonProjection> projections = new HashMap<>();
    private int totalXp;
    private Money totalFees = Money.zero();

    /**
     * @param context the campaign settings that price the training
     * @param funds   what the campaign can spend on training fees
     */
    public TrainingPlan(TrainingContext context, Money funds) {
        this.context = context;
        this.funds = funds;
    }

    /** @return the settings that price this plan */
    public TrainingContext context() {
        return context;
    }

    /**
     * @param person a person
     *
     * @return the person as they will be once everything staged is applied
     */
    public PersonProjection projectionOf(Person person) {
        return projections.computeIfAbsent(person.getId(), id -> new PersonProjection(person));
    }

    /**
     * Prices a purchase against what is staged, without staging it.
     *
     * @param purchase the purchase
     *
     * @return its price, and why it can't be made if it can't
     */
    public Quote quote(TrainingPurchase purchase) {
        return TrainingPricer.quote(context, projectionOf(purchase.person()), purchase);
    }

    /**
     * Stages a batch of purchases as one step. Each is priced against the ones before it, in order; those that can't
     * be made are left out and counted. If the campaign can't pay the training fees of everything staged, this batch
     * included, nothing in the batch is staged.
     *
     * @param label     what the player bought, for the review list
     * @param purchases the purchases, in the order to apply them
     *
     * @return what was staged and what was refused
     */
    public StageResult stage(String label, List<? extends TrainingPurchase> purchases) {
        Map<TrainingRefusal, Integer> refused = new EnumMap<>(TrainingRefusal.class);
        List<StagedPurchase> staged = new ArrayList<>();
        Money batchFees = Money.zero();
        for (TrainingPurchase purchase : purchases) {
            Quote quote = quote(purchase);
            if (!quote.isAllowed()) {
                refused.merge(quote.refusal(), 1, Integer::sum);
                continue;
            }
            TrainingPricer.apply(projectionOf(purchase.person()), purchase, quote);
            staged.add(new StagedPurchase(purchase, quote));
            batchFees = batchFees.plus(quote.fee());
        }
        if (staged.isEmpty()) {
            return new StageResult(null, refused, Money.zero());
        }

        Money needed = totalFees.plus(batchFees);
        if (needed.isGreaterThan(funds)) {
            // Training is all or nothing: put the projections back as they were before this batch
            replay();
            Map<TrainingRefusal, Integer> allRefused = new EnumMap<>(TrainingRefusal.class);
            allRefused.put(TrainingRefusal.NOT_ENOUGH_FUNDS, purchases.size());
            return new StageResult(null, allRefused, needed.minus(funds));
        }

        Step step = new Step(label, List.copyOf(staged));
        steps.add(step);
        totalXp += step.xp();
        totalFees = needed;
        return new StageResult(step, refused, Money.zero());
    }

    /**
     * Removes the most recently staged step.
     *
     * @return {@code true} if there was one to remove
     */
    public boolean undo() {
        if (steps.isEmpty()) {
            return false;
        }
        steps.remove(steps.size() - 1);
        replay();
        return true;
    }

    /**
     * Removes a step and prices the steps after it again.
     *
     * @param index the step's position
     *
     * @return how many later purchases no longer fit and were dropped
     */
    public int removeStep(int index) {
        steps.remove(index);
        return replay();
    }

    /** Removes everything staged. */
    public void clear() {
        steps.clear();
        replay();
    }

    /**
     * Prices every step again from scratch, in order, dropping purchases that no longer fit and steps left empty.
     *
     * @return how many purchases were dropped
     */
    private int replay() {
        projections.clear();
        totalXp = 0;
        totalFees = Money.zero();
        int dropped = 0;
        List<Step> kept = new ArrayList<>();
        for (Step step : steps) {
            List<StagedPurchase> purchases = new ArrayList<>();
            for (StagedPurchase staged : step.purchases()) {
                Quote quote = quote(staged.purchase());
                if (quote.isAllowed() && !totalFees.plus(quote.fee()).isGreaterThan(funds)) {
                    TrainingPricer.apply(projectionOf(staged.purchase().person()), staged.purchase(), quote);
                    purchases.add(new StagedPurchase(staged.purchase(), quote));
                    totalXp += quote.xp();
                    totalFees = totalFees.plus(quote.fee());
                } else {
                    dropped++;
                }
            }
            if (!purchases.isEmpty()) {
                kept.add(new Step(step.label(), List.copyOf(purchases)));
            }
        }
        steps.clear();
        steps.addAll(kept);
        return dropped;
    }

    /** @return the staged steps, in order */
    public List<Step> steps() {
        return Collections.unmodifiableList(steps);
    }

    /** @return {@code true} if nothing is staged */
    public boolean isEmpty() {
        return steps.isEmpty();
    }

    /** @return how many purchases are staged across every step */
    public int purchaseCount() {
        return steps.stream().mapToInt(step -> step.purchases().size()).sum();
    }

    /** @return the XP everything staged costs, less any it gives back */
    public int totalXp() {
        return totalXp;
    }

    /** @return the training fees of everything staged */
    public Money totalFees() {
        return totalFees;
    }

    /** @return what the campaign can spend on training fees */
    public Money funds() {
        return funds;
    }

    /** @return the funds left once the staged fees are paid */
    public Money fundsLeft() {
        return funds.minus(totalFees);
    }

    /** @return the staged fees, by what they pay for */
    public Map<TrainingFeeCategory, Money> feesByCategory() {
        Map<TrainingFeeCategory, Money> fees = new EnumMap<>(TrainingFeeCategory.class);
        for (Step step : steps) {
            for (StagedPurchase staged : step.purchases()) {
                TrainingFeeCategory category = staged.quote().feeCategory();
                if (category != null) {
                    fees.merge(category, staged.quote().fee(), Money::plus);
                }
            }
        }
        return fees;
    }

    /** @return the people with anything staged, as they will be */
    public List<PersonProjection> changedPeople() {
        return projections.values().stream().filter(projection -> projection.purchaseCount() > 0).toList();
    }

    /**
     * @return a copy of this plan, with the same steps staged, that can be changed without changing this one; used to
     *       try out a plan before staging it
     */
    public TrainingPlan copy() {
        TrainingPlan copy = new TrainingPlan(context, funds);
        copy.steps.addAll(steps);
        copy.replay();
        return copy;
    }

    /**
     * Stages one purchase on its own, without making it a step; for planners working on a {@link #copy()}.
     *
     * @param purchase the purchase
     *
     * @return its quote; if allowed, it has been recorded against the person
     */
    Quote tryApply(TrainingPurchase purchase) {
        Quote quote = quote(purchase);
        if (quote.isAllowed() && !totalFees.plus(quote.fee()).isGreaterThan(funds)) {
            TrainingPricer.apply(projectionOf(purchase.person()), purchase, quote);
            totalXp += quote.xp();
            totalFees = totalFees.plus(quote.fee());
            return quote;
        }
        return quote.isAllowed() ? quote.refusedFor(TrainingRefusal.NOT_ENOUGH_FUNDS) : quote;
    }
}
