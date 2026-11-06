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

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

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
 * removing a step prices everything after it again; a purchase that no longer fits is dropped.</p>
 *
 * <p>Training fees are all or nothing: a step is only staged, and only kept when everything is priced again, if the
 * campaign can pay the fees of every step staged so far, including it.</p>
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
            return feesOf(purchases);
        }

        /** @return how many different people the step trains */
        public int peopleCount() {
            return TrainingPurchase.peopleCount(purchases.stream().map(StagedPurchase::purchase).toList());
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

    /**
     * What raising people one step at a time to a target would buy, priced against everything already staged.
     *
     * @param purchases the purchases, in order: each person's climb, one person after another
     * @param people    how many people would be raised at least one step
     * @param steps     how many steps would be bought
     * @param xp        their XP cost
     * @param fees      their training fees
     * @param stopShort how many people would stop short of the target
     */
    public record Climb(List<TrainingPurchase> purchases, int people, int steps, int xp, Money fees,
          int stopShort) {}

    private final TrainingContext context;
    private final Money funds;
    private final List<Step> steps = new ArrayList<>();
    private final Map<UUID, PersonProjection> projections = new HashMap<>();
    /** The people with anything staged, in the order they were first trained. */
    private final Set<UUID> changed = new LinkedHashSet<>();
    private int totalXp;
    private Money totalFees = Money.zero();
    private int revision;
    /** How many single purchases the last successful {@link #replayOnce()} dropped. */
    private int droppedByLastReplay;

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
     * @return a number that changes whenever anything staged changes, so callers can tell when what they worked out
     *       from the plan is out of date
     */
    public int revision() {
        return revision;
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
     * As {@link #projectionOf(Person)}, without making a projection for someone the plan hasn't priced yet; for
     * reading a whole roster without filling the plan with empty projections.
     *
     * @param person a person
     *
     * @return the person's projection, or {@code null} if nothing has priced anything for them
     */
    public @Nullable PersonProjection peek(Person person) {
        return projections.get(person.getId());
    }

    /** @return the person's XP once everything staged for them is paid for */
    public int availableXpOf(Person person) {
        PersonProjection projection = peek(person);
        return (projection == null) ? person.getXP() : projection.availableXp();
    }

    /** @return how many purchases are staged for the person */
    public int purchaseCountOf(Person person) {
        PersonProjection projection = peek(person);
        return (projection == null) ? 0 : projection.purchaseCount();
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
        // Each person's projection as it was before this batch, so a refused batch can be undone without pricing
        // the whole plan again; null for someone who had none
        Map<UUID, PersonProjection> before = new HashMap<>();
        for (TrainingPurchase purchase : purchases) {
            UUID id = purchase.person().getId();
            if (!before.containsKey(id)) {
                PersonProjection existing = projections.get(id);
                before.put(id, (existing == null) ? null : existing.copy());
            }
            Quote quote = quote(purchase);
            if (!quote.isAllowed()) {
                refused.merge(quote.refusal(), 1, Integer::sum);
                continue;
            }
            record(purchase, quote);
            staged.add(new StagedPurchase(purchase, quote));
        }
        if (staged.isEmpty()) {
            restore(before);
            return new StageResult(null, refused, Money.zero());
        }

        Money needed = totalFees.plus(feesOf(staged));
        if (needed.isPositive() && needed.isGreaterThan(funds)) {
            // Training is all or nothing: put the projections back as they were before this batch
            restore(before);
            Map<TrainingRefusal, Integer> allRefused = new EnumMap<>(TrainingRefusal.class);
            allRefused.put(TrainingRefusal.NOT_ENOUGH_FUNDS, purchases.size());
            return new StageResult(null, allRefused, needed.minus(funds));
        }

        Step step = new Step(label, List.copyOf(staged));
        steps.add(step);
        totalXp += step.xp();
        totalFees = needed;
        revision++;
        return new StageResult(step, refused, Money.zero());
    }

    /**
     * Stages, as one step, purchases a planner has already priced against this plan as it stands now, without
     * pricing them again. Refused if the campaign can't pay their fees.
     *
     * @param label  what the step buys
     * @param priced the purchases and their prices, in order
     *
     * @return {@code true} if they were staged
     */
    boolean stagePriced(String label, List<StagedPurchase> priced) {
        Money needed = totalFees.plus(feesOf(priced));
        if (priced.isEmpty() || (needed.isPositive() && needed.isGreaterThan(funds))) {
            return false;
        }
        for (StagedPurchase staged : priced) {
            record(staged.purchase(), staged.quote());
        }
        Step step = new Step(label, List.copyOf(priced));
        steps.add(step);
        totalXp += step.xp();
        totalFees = totalFees.plus(step.fees());
        revision++;
        return true;
    }

    private void restore(Map<UUID, PersonProjection> before) {
        for (Map.Entry<UUID, PersonProjection> entry : before.entrySet()) {
            PersonProjection projection = entry.getValue();
            if (projection == null) {
                projections.remove(entry.getKey());
                changed.remove(entry.getKey());
            } else {
                projections.put(entry.getKey(), projection);
                if (projection.purchaseCount() == 0) {
                    changed.remove(entry.getKey());
                }
            }
        }
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
     * Prices every step again from scratch, in order. A purchase that no longer fits is dropped, and so is a step left
     * empty. A step whose fees the campaign can no longer pay is dropped whole, as training fees are all or nothing.
     *
     * @return how many purchases were dropped
     */
    private int replay() {
        int dropped = 0;
        while (true) {
            int unaffordable = replayOnce();
            if (unaffordable < 0) {
                break;
            }
            // Drop the step and price everything again, as the steps after it may now fit
            dropped += steps.remove(unaffordable).purchases().size();
        }
        revision++;
        return dropped + droppedByLastReplay;
    }

    /**
     * @return the position of the first step whose fees can't be paid, in which case the steps are left as they were;
     *       or {@code -1} once every step has been priced again and the steps replaced by what still fits
     */
    private int replayOnce() {
        projections.clear();
        changed.clear();
        totalXp = 0;
        totalFees = Money.zero();
        droppedByLastReplay = 0;
        List<Step> kept = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            Step step = steps.get(index);
            List<StagedPurchase> purchases = new ArrayList<>();
            for (StagedPurchase staged : step.purchases()) {
                Quote quote = quote(staged.purchase());
                if (quote.isAllowed()) {
                    record(staged.purchase(), quote);
                    purchases.add(new StagedPurchase(staged.purchase(), quote));
                } else {
                    droppedByLastReplay++;
                }
            }
            Money stepFees = feesOf(purchases);
            Money needed = totalFees.plus(stepFees);
            if (needed.isPositive() && needed.isGreaterThan(funds)) {
                return index;
            }
            if (!purchases.isEmpty()) {
                Step repriced = new Step(step.label(), List.copyOf(purchases));
                kept.add(repriced);
                totalXp += repriced.xp();
                totalFees = totalFees.plus(stepFees);
            }
        }
        steps.clear();
        steps.addAll(kept);
        return -1;
    }

    /** Records an allowed purchase against the person, without touching the plan's totals. */
    private void record(TrainingPurchase purchase, Quote quote) {
        TrainingPricer.apply(projectionOf(purchase.person()), purchase, quote);
        changed.add(purchase.person().getId());
    }

    private static Money feesOf(List<StagedPurchase> purchases) {
        Money total = Money.zero();
        for (StagedPurchase staged : purchases) {
            total = total.plus(staged.quote().fee());
        }
        return total;
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

    /** @return how many people have anything staged */
    public int changedPeopleCount() {
        return changed.size();
    }

    /**
     * @return a copy of this plan, with the same steps staged, that can be changed without changing this one; used to
     *       try out purchases before staging them
     */
    public TrainingPlan copy() {
        TrainingPlan copy = new TrainingPlan(context, funds);
        copy.steps.addAll(steps);
        for (Map.Entry<UUID, PersonProjection> entry : projections.entrySet()) {
            copy.projections.put(entry.getKey(), entry.getValue().copy());
        }
        copy.changed.addAll(changed);
        copy.totalXp = totalXp;
        copy.totalFees = totalFees;
        return copy;
    }

    /**
     * @param quote an allowed quote
     *
     * @return {@code true} if the campaign can pay its fee on top of everything already staged
     */
    boolean fits(Quote quote) {
        Money needed = totalFees.plus(quote.fee());
        return !needed.isPositive() || !needed.isGreaterThan(funds);
    }

    /**
     * Records one purchase that has just been quoted against this plan and allowed, without making it a step; for
     * planners trying purchases out on a {@link #copy()}.
     *
     * @param purchase the purchase
     * @param quote    its allowed quote, which the caller has checked {@link #fits}
     */
    void applyQuoted(TrainingPurchase purchase, Quote quote) {
        record(purchase, quote);
        totalXp += quote.xp();
        totalFees = totalFees.plus(quote.fee());
        revision++;
    }

    /**
     * Works out what raising people one step at a time to a target would buy, against everything already staged,
     * without staging anything: each person is raised until they reach the target, run out of XP, hit a limit, or the
     * fees can no longer be paid.
     *
     * @param people      the people to raise
     * @param purchaseFor the purchase that raises a person one step
     * @param level       how far a person has been raised, read from their projection
     * @param target      the level to raise them to
     *
     * @return what would be bought
     */
    public Climb previewClimb(Collection<Person> people, Function<Person, ? extends TrainingPurchase> purchaseFor,
          ToIntFunction<PersonProjection> level, int target) {
        TrainingPlan trial = copy();
        List<TrainingPurchase> purchases = new ArrayList<>();
        int raised = 0;
        int bought = 0;
        int xp = 0;
        Money fees = Money.zero();
        int stopShort = 0;
        for (Person person : people) {
            PersonProjection projection = trial.projectionOf(person);
            boolean isRaised = false;
            int current = level.applyAsInt(projection);
            while (current < target) {
                TrainingPurchase purchase = purchaseFor.apply(person);
                Quote quote = trial.quote(purchase);
                if (!quote.isAllowed() || !trial.fits(quote)) {
                    stopShort++;
                    break;
                }
                trial.applyQuoted(purchase, quote);
                int next = level.applyAsInt(projection);
                if (next <= current) {
                    // A purchase that doesn't raise the level would never reach the target
                    stopShort++;
                    break;
                }
                current = next;
                purchases.add(purchase);
                bought++;
                xp += quote.xp();
                fees = fees.plus(quote.fee());
                isRaised = true;
            }
            if (isRaised) {
                raised++;
            }
        }
        return new Climb(List.copyOf(purchases), raised, bought, xp, fees, stopShort);
    }
}
