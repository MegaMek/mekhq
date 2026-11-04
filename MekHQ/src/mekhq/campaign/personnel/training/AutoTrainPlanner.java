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
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.QuickTrain;
import mekhq.campaign.personnel.training.TrainingPlan.StagedPurchase;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;

/**
 * Works out what auto-train would buy, without buying it: the skill improvements, in order, that level people's
 * skills as Quick Train or Mass Training did, priced against everything already staged in a {@link TrainingPlan}.
 * The result can be previewed, then staged as one step.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AutoTrainPlanner {
    private AutoTrainPlanner() {
    }

    /** Why someone was left out of a plan. */
    public enum Skip {
        /** They are marked to be excluded from automatic training. */
        EXCLUDED,
        /** Their unit is in a training formation. */
        TRAINING_FORMATION,
        /** They aren't active. */
        INACTIVE
    }

    /**
     * What auto-train would buy.
     *
     * @param staged  the improvements and the prices they were planned at, in the order to stage them
     * @param xp      their XP cost
     * @param fees    their training fees
     * @param bySkill how many improvements each skill gets, most first
     * @param skipped how many people were left out, by reason
     */
    public record Plan(List<StagedPurchase> staged, int xp, Money fees, Map<String, Integer> bySkill,
          Map<Skip, Integer> skipped) {
        /** @return the improvements, in the order to stage them */
        public List<TrainingPurchase> purchases() {
            return staged.stream().map(StagedPurchase::purchase).toList();
        }

        /** @return how many different people the plan trains */
        public int peopleCount() {
            return TrainingPurchase.peopleCount(purchases());
        }
    }

    /**
     * @param campaign the campaign
     * @param plan     what is already staged; it isn't changed
     * @param people   the people to train
     * @param settings how to train them
     *
     * @return what auto-train would buy
     */
    public static Plan plan(Campaign campaign, TrainingPlan plan, Collection<Person> people,
          AutoTrainSettings settings) {
        TrainingPlan trial = plan.copy();
        List<StagedPurchase> staged = new ArrayList<>();
        Map<String, Integer> bySkill = new LinkedHashMap<>();
        Map<Skip, Integer> skipped = new EnumMap<>(Skip.class);
        int xp = 0;
        Money fees = Money.zero();

        for (Person person : people) {
            Skip skip = skipReason(campaign, person, settings);
            if (skip != null) {
                skipped.merge(skip, 1, Integer::sum);
                continue;
            }
            PersonProjection projection = trial.projectionOf(person);
            List<String> targets = targetSkills(campaign, person, projection, settings);
            if (targets.isEmpty()) {
                continue;
            }

            boolean improvedThisRound;
            do {
                improvedThisRound = false;
                for (String skillName : ordered(trial, person, projection, targets, settings)) {
                    if (projection.skillLevel(skillName) >= settings.targetLevel()) {
                        continue;
                    }
                    SkillImprovement purchase = new SkillImprovement(person, skillName);
                    Quote quote = trial.quote(purchase);
                    if (!quote.isAllowed()
                              || (quote.xp() > projection.availableXp() - settings.reserveXp())
                              || (settings.isSkipFeeImprovements() && !quote.fee().isZero())
                              || !trial.fits(quote)) {
                        continue;
                    }
                    trial.applyQuoted(purchase, quote);
                    staged.add(new StagedPurchase(purchase, quote));
                    bySkill.merge(skillName, 1, Integer::sum);
                    xp += quote.xp();
                    fees = fees.plus(quote.fee());
                    improvedThisRound = true;
                }
            } while (settings.isContinuous() && improvedThisRound);
        }

        Map<String, Integer> sortedBySkill = new LinkedHashMap<>();
        bySkill.entrySet().stream()
              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
              .forEach(entry -> sortedBySkill.put(entry.getKey(), entry.getValue()));
        return new Plan(List.copyOf(staged), xp, fees, sortedBySkill, skipped);
    }

    private static @Nullable Skip skipReason(Campaign campaign, Person person, AutoTrainSettings settings) {
        PersonnelStatus status = person.getStatus();
        if (!status.isActiveFlexible() || status.isDepartedUnit() || status.isStudent()) {
            return Skip.INACTIVE;
        }
        if (settings.isRespectExclusions() && person.isQuickTrainIgnore()) {
            return Skip.EXCLUDED;
        }
        if (settings.isSkipTrainingFormations() && QuickTrain.isInTrainingFormation(person, campaign)) {
            return Skip.TRAINING_FORMATION;
        }
        return null;
    }

    /** The skills to train: the one chosen skill, or the skills Quick Train would pick; only skills already held. */
    private static List<String> targetSkills(Campaign campaign, Person person, PersonProjection projection,
          AutoTrainSettings settings) {
        List<String> candidates = (settings.specificSkill() != null) ?
                                        List.of(settings.specificSkill()) :
                                        QuickTrain.getTargetSkills(campaign, person,
                                              settings.toQuickTrainOptions());
        List<String> held = new ArrayList<>();
        for (String skillName : candidates) {
            if ((projection.skill(skillName) != null) && !held.contains(skillName)) {
                held.add(skillName);
            }
        }
        return held;
    }

    /**
     * @return the skills in the order to train them this round: weakest first, by trained level, as Quick Train does;
     *       or cheapest first, each priced once
     */
    private static List<String> ordered(TrainingPlan trial, Person person, PersonProjection projection,
          List<String> targets, AutoTrainSettings settings) {
        List<String> ordered = new ArrayList<>(targets);
        if (settings.order() == AutoTrainSettings.Order.CHEAPEST_FIRST) {
            Map<String, Integer> prices = new HashMap<>();
            for (String skillName : ordered) {
                Quote quote = trial.quote(new SkillImprovement(person, skillName));
                prices.put(skillName, quote.isAllowed() ? quote.xp() : Integer.MAX_VALUE);
            }
            ordered.sort(Comparator.comparingInt(prices::get));
        } else {
            ordered.sort(Comparator.comparingInt(projection::skillLevel));
        }
        return ordered;
    }
}
