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
package mekhq.gui.dialog.trainingConsole;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.training.TrainingFeeCategory;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPlan.StagedPurchase;
import mekhq.campaign.personnel.training.TrainingPlan.Step;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AbilityPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.FlawBuyOff;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import mekhq.campaign.personnel.training.TrainingService;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;

/**
 * The Review page: everything staged, step by step, with what each costs; the training fees by what they pay for,
 * against the campaign's funds; and anything that happens at commit, such as Veterancy rolls.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ReviewPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    /** Rows shown per open step; a step can train thousands of people. */
    private static final int MAX_LINES = 80;

    private ReviewPage() {
    }

    static List<JComponent> build(TrainingConsole console) {
        TrainingPlan plan = console.plan();
        List<JComponent> parts = new ArrayList<>();
        if (plan.isEmpty()) {
            parts.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "review.empty")));
            return parts;
        }

        JPanel top = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(12), 0));
        top.add(card(getTextAt(RESOURCE_BUNDLE, "review.notes"), notes(console)));
        top.add(card(getTextAt(RESOURCE_BUNDLE, "review.fees"), fees(console)));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        parts.add(top);

        parts.add(Hud.sectionHeading(getFormattedTextAt(RESOURCE_BUNDLE, "review.steps", plan.steps().size())));
        List<Step> steps = plan.steps();
        for (int index = 0; index < steps.size(); index++) {
            parts.add(stepCard(console, steps.get(index), index));
        }
        return parts;
    }

    private static JComponent card(String title, JComponent content) {
        JPanel card = ConsoleWidgets.column();
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10), scaleForGUI(12))));
        card.add(leftAligned(Hud.eyebrow(title)));
        card.add(Box.createVerticalStrut(scaleForGUI(6)));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(content);
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(card, BorderLayout.NORTH);
        return holder;
    }

    private static JComponent notes(TrainingConsole console) {
        TrainingPlan plan = console.plan();
        JPanel notes = ConsoleWidgets.column();
        List<String[]> lines = new ArrayList<>();
        Money funds = plan.funds();
        if (!plan.totalFees().isZero() && plan.totalFees().isGreaterThan(funds.multipliedBy(0.5))) {
            lines.add(new String[] { "amber", getTextAt(RESOURCE_BUNDLE, "review.note.fees") });
        }
        boolean hasSkills = plan.steps().stream()
                                  .anyMatch(step -> step.purchases().stream()
                                                          .anyMatch(staged -> staged.purchase()
                                                                                    instanceof SkillImprovement));
        if (hasSkills && console.campaign().getCampaignOptions().get(CampaignOption.AWARD_VETERANCY_SP_AS)) {
            lines.add(new String[] { "accent", getTextAt(RESOURCE_BUNDLE, "review.note.veterancy") });
        }
        lines.add(new String[] { "faint", getTextAt(RESOURCE_BUNDLE, "review.note.order") });
        lines.add(new String[] { "faint", getTextAt(RESOURCE_BUNDLE, "review.note.reprice") });
        for (String[] line : lines) {
            Color color = switch (line[0]) {
                case "amber" -> AMBER;
                case "accent" -> ACCENT;
                default -> TEXT_FAINT;
            };
            JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            JPanel stripe = new JPanel();
            stripe.setBackground(color);
            stripe.setPreferredSize(new Dimension(scaleForGUI(3), 0));
            row.add(stripe, BorderLayout.WEST);
            row.add(ConsoleWidgets.wrapped(line[1], TEXT_MUTED), BorderLayout.CENTER);
            row.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(6), 0));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            notes.add(row);
        }
        return notes;
    }

    private static JComponent fees(TrainingConsole console) {
        TrainingPlan plan = console.plan();
        JPanel fees = ConsoleWidgets.column();
        if (!plan.context().isUseSkillTrainingCosts() && !plan.context().isUseSpaTrainingCosts()) {
            fees.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "fees.off"), 0.84f,
                  TEXT_FAINT)));
            return fees;
        }
        Map<TrainingFeeCategory, Money> byCategory = plan.feesByCategory();
        for (TrainingFeeCategory category : TrainingFeeCategory.values()) {
            Money amount = byCategory.getOrDefault(category, Money.zero());
            fees.add(ConsoleWidgets.pair(category.getLabel(), amount.toAmountAndSymbolString(),
                  amount.isZero() ? TEXT_FAINT : TEXT_MUTED, false));
        }
        fees.add(Box.createVerticalStrut(scaleForGUI(4)));
        fees.add(ConsoleWidgets.pair(getTextAt(RESOURCE_BUNDLE, "review.fees.total"),
              plan.totalFees().toAmountAndSymbolString(), AMBER, true));
        fees.add(ConsoleWidgets.pair(getTextAt(RESOURCE_BUNDLE, "review.fees.funds"),
              plan.funds().toAmountAndSymbolString(), TEXT_MUTED, false));
        Money left = plan.fundsLeft();
        fees.add(ConsoleWidgets.pair(getTextAt(RESOURCE_BUNDLE, "fees.leftAfter"), left.toAmountAndSymbolString(),
              left.isPositiveOrZero() ? TEXT : DANGER, true));
        return fees;
    }

    private static JComponent stepCard(TrainingConsole console, Step step, int index) {
        TrainingConsoleState state = console.state();
        boolean isOpen = (state.openStep != null) && (state.openStep == index);
        String meta = getFormattedTextAt(RESOURCE_BUNDLE, "review.step.meta", step.peopleCount(),
              step.purchases().size());
        HudButton remove = ConsoleWidgets.button(getTextAt(RESOURCE_BUNDLE, "review.remove"), false, true,
              () -> console.removeStep(index));
        JComponent action = ConsoleWidgets.actionColumn(remove, ConsoleWidgets.xpText(step.xp()),
              ConsoleWidgets.money(step.fees()));
        CatalogCard card = new CatalogCard(getFormattedTextAt(RESOURCE_BUNDLE, "review.step.title", index + 1,
              step.label()), List.of(), meta, null, action, isOpen, () -> {
                  state.openStep = isOpen ? null : index;
                  console.refreshPanel();
              });
        if (isOpen) {
            List<StagedPurchase> purchases = step.purchases();
            for (int line = 0; line < Math.min(MAX_LINES, purchases.size()); line++) {
                card.addToBody(purchaseLine(purchases.get(line)));
            }
            if (purchases.size() > MAX_LINES) {
                card.addToBody(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "review.more",
                      purchases.size() - MAX_LINES), 0.8f, TEXT_FAINT));
            }
        }
        return card;
    }

    private static JComponent purchaseLine(StagedPurchase staged) {
        Person person = staged.purchase().person();
        JPanel line = Hud.transparentPanel(new GridLayout(1, 3, scaleForGUI(8), 0));
        line.add(ConsoleWidgets.label(person.getFullTitle(), 0.82f, TEXT));
        line.add(ConsoleWidgets.label(describe(staged), 0.82f, TEXT_MUTED));
        String cost = ConsoleWidgets.xpText(staged.quote().xp());
        if (!staged.quote().fee().isZero()) {
            cost = getFormattedTextAt(RESOURCE_BUNDLE, "review.line.withFee", cost,
                  staged.quote().fee().toAmountAndSymbolString());
        }
        line.add(ConsoleWidgets.label(cost, 0.82f, staged.quote().fee().isZero() ? TEXT_MUTED : AMBER));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    /**
     * @param staged a staged purchase
     *
     * @return what it buys, such as "Gunnery/Mek to level 5" or "Weapon Specialist Medium Laser"
     */
    static String describe(StagedPurchase staged) {
        TrainingPurchase purchase = staged.purchase();
        return switch (purchase) {
            case SkillImprovement improvement -> {
                int level = (staged.quote().projectedSkill() == null) ? 0 :
                                  staged.quote().projectedSkill().getLevel();
                yield getFormattedTextAt(RESOURCE_BUNDLE, "describe.skill", improvement.skillName(), level);
            }
            case NaturalAptitude aptitude -> getFormattedTextAt(RESOURCE_BUNDLE, "describe.aptitude",
                  aptitude.skillName());
            case AbilityPurchase ability -> TrainingService.abilityDisplayName(ability.abilityName(),
                  ability.choice());
            case FlawBuyOff buyOff -> getFormattedTextAt(RESOURCE_BUNDLE, "describe.buyOff",
                  SpecialAbility.getDisplayName(buyOff.abilityName()));
            case AttributeIncrease increase -> getFormattedTextAt(RESOURCE_BUNDLE, "describe.attribute",
                  increase.attribute().getLabel());
            case TraitChange change -> getFormattedTextAt(RESOURCE_BUNDLE, (change.delta() > 0) ?
                                                                               "describe.traitUp" :
                                                                               "describe.traitDown",
                  change.trait().getDisplayName());
            case EdgeRefill ignored -> getTextAt(RESOURCE_BUNDLE, "describe.edge");
        };
    }
}
