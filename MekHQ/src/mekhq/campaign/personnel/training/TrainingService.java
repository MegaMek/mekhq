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

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPlan.StagedPurchase;
import mekhq.campaign.personnel.training.TrainingPlan.Step;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.AbilityPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.FlawBuyOff;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;

/**
 * Applies a {@link TrainingPlan} to the campaign: spends the XP, pays the training fees, and raises the skills,
 * abilities, attributes and traits, the same way the Spend XP menu does, logging each change in the person's log and
 * one line per step in the daily report.
 *
 * <p>Nothing is applied unless the campaign can pay every training fee in the plan. Each purchase is priced again as
 * it is applied, from the person as they are by then; a purchase that no longer fits, because something changed the
 * person since it was staged, is skipped and counted.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TrainingService {
    private static final MMLogger LOGGER = MMLogger.create(TrainingService.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";
    private static final String GUI_RESOURCE_BUNDLE = "mekhq.resources.GUI";

    private TrainingService() {
    }

    /**
     * What a commit did.
     *
     * @param isRefused   {@code true} if nothing was applied because the campaign can't pay the plan's fees
     * @param applied     how many purchases were applied
     * @param skipped     how many were skipped because they no longer fit
     * @param xpSpent     the XP spent, less any given back
     * @param feesPaid    the training fees paid
     */
    public record CommitReport(boolean isRefused, int applied, int skipped, int xpSpent, Money feesPaid) {}

    /**
     * Applies every staged purchase, in order.
     *
     * @param campaign the campaign
     * @param plan     the staged training
     *
     * @return what was applied
     */
    public static CommitReport commit(Campaign campaign, TrainingPlan plan) {
        Money balance = campaign.getPlayerForce().getFinances().getBalance();
        if (plan.totalFees().isGreaterThan(balance)) {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "report.refusedFunds",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                  plan.totalFees().toAmountAndSymbolString(), balance.toAmountAndSymbolString()));
            return new CommitReport(true, 0, 0, 0, Money.zero());
        }

        TrainingContext context = new TrainingContext(campaign);
        Set<Person> changed = new LinkedHashSet<>();
        int applied = 0;
        int skipped = 0;
        int xpSpent = 0;
        Money feesPaid = Money.zero();
        for (Step step : plan.steps()) {
            Set<Person> trainedThisStep = new HashSet<>();
            for (StagedPurchase staged : step.purchases()) {
                TrainingPurchase purchase = staged.purchase();
                Person person = purchase.person();
                // The person is as earlier purchases left them, so a fresh projection prices this one correctly
                Quote quote = TrainingPricer.quote(context, new PersonProjection(person), purchase);
                if (!quote.isAllowed() || !applyPurchase(campaign, context, purchase, quote)) {
                    LOGGER.info("Skipped training for {}: {} ({})", person.getFullName(), purchase,
                          quote.isAllowed() ? "payment failed" : quote.refusal());
                    skipped++;
                    continue;
                }
                applied++;
                xpSpent += quote.xp();
                feesPaid = feesPaid.plus(quote.fee());
                changed.add(person);
                trainedThisStep.add(person);
            }
            if (!trainedThisStep.isEmpty()) {
                campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "report.step", step.label(),
                      trainedThisStep.size()));
            }
        }

        for (Person person : changed) {
            campaign.getPlayerForce().getHumanResources().personUpdated(campaign, person);
        }
        if (skipped > 0) {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "report.skipped",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, skipped));
        }
        return new CommitReport(false, applied, skipped, xpSpent, feesPaid);
    }

    /**
     * Applies one purchase that has just been priced and allowed.
     *
     * @return {@code false} if the training fee couldn't be paid, in which case nothing changed
     */
    private static boolean applyPurchase(Campaign campaign, TrainingContext context, TrainingPurchase purchase,
          Quote quote) {
        Person person = purchase.person();
        LocalDate today = context.today();
        switch (purchase) {
            case SkillImprovement improvement -> {
                String skillName = improvement.skillName();
                // Priced from the current level, so it is paid before the skill is improved
                if (!SkillTrainingCosts.payForSkillImprovement(campaign, person, skillName, quote.fee())) {
                    return false;
                }
                Skill skill = person.getSkill(skillName);
                if (skill != null) {
                    // The progress already came off the price, so it's used up
                    skill.changeXpProgress(-person.getCostToImprove(skillName, context.isUseReasoning()));
                }
                person.improveSkill(skillName);
                person.spendXPOnSkills(campaign, quote.xp());
                Skill improved = person.getSkill(skillName);
                PerformanceLogger.improvedSkill(context.isLogSkillGain(), person, today, skillName,
                      (improved == null) ? 0 : improved.getLevel());
            }
            case NaturalAptitude aptitude -> {
                Skill skill = person.getSkill(aptitude.skillName());
                if (skill == null) {
                    return false;
                }
                skill.setHasNaturalAptitude(true);
                skill.changeNaturalAptitudeXpProgress(-skill.getNaturalAptitudeXpProgress());
                person.spendXP(quote.xp());
                PerformanceLogger.gainedNaturalAptitude(context.isLogSkillGain(), person, today,
                      aptitude.skillName());
            }
            case AbilityPurchase ability -> {
                String displayName = abilityDisplayName(ability.abilityName(), ability.choice());
                if (!quote.fee().isZero() && !campaign.getPlayerForce().getFinances()
                                                      .debit(TransactionType.EDUCATION, today, quote.fee(),
                                                            getFormattedTextAt(GUI_RESOURCE_BUNDLE,
                                                                  "spaTraining.transaction", person.getFullTitle(),
                                                                  displayName))) {
                    return false;
                }
                Object value = (ability.choice() == null) ? Boolean.TRUE : ability.choice();
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, ability.abilityName(), value);
                person.spendXP(quote.xp());
                PerformanceLogger.gainedSPA(campaign, person, today, displayName);
            }
            case FlawBuyOff buyOff -> {
                IOption option = person.getOptions().getOption(buyOff.abilityName());
                if (option == null) {
                    return false;
                }
                option.setValue(option.getDefault());
                person.spendXP(quote.xp());
                PerformanceLogger.paidOffFlaw(campaign, person, today,
                      SpecialAbility.getDisplayName(buyOff.abilityName()));
            }
            case AttributeIncrease increase -> {
                if (increase.attribute() == SkillAttribute.EDGE) {
                    if (person.gainEdge(1, context.maximumEdge()) == 0) {
                        return false;
                    }
                } else {
                    person.changeAttributeScore(increase.attribute(), 1);
                }
                person.spendXP(quote.xp());
            }
            case TraitChange change -> {
                int target = TrainingPricer.traitLevel(person, change.trait()) + change.delta();
                switch (change.trait()) {
                    case CONNECTIONS -> person.setConnections(target);
                    case FAME -> person.setFame(target);
                    case WEALTH -> person.setWealth(target);
                    case EXTRA_INCOME -> person.setExtraIncomeFromTraitLevel(target);
                    case UNLUCKY -> person.setUnlucky(target);
                    case BLOODMARK -> person.setBloodmark(target);
                    default -> {
                        return false;
                    }
                }
                person.spendXP(quote.xp());
            }
            case EdgeRefill ignored -> {
                person.spendXP(quote.xp());
                person.resetCurrentEdge();
            }
        }
        return true;
    }

    /**
     * @param abilityName an ability's lookup name
     * @param choice      its chosen value, or {@code null}
     *
     * @return the ability's name as the player sees it, with its chosen value, such as "Weapon Specialist Medium
     *       Laser"
     */
    public static String abilityDisplayName(String abilityName, @Nullable String choice) {
        String name = SpecialAbility.getDisplayName(abilityName);
        return ((choice == null) || choice.isBlank()) ? name : name + ' ' + choice;
    }
}
