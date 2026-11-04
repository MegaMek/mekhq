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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.personnel.training.PersonProjection;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPlan.Climb;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.Segment;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.StackedBar;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.SkillGroup;

/**
 * The Skills page's catalog: one card per skill, showing how many of the selected people can improve it now, what that
 * costs in XP and training fees, and why the rest can't. An open card shows the spread of levels, trains everyone to
 * a chosen level, and teaches the skill to those who lack it.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class SkillsPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    private static final int MAX_LEVEL = 10;

    private SkillsPage() {
    }

    /** How many people reach one experience level with a fee, and what they pay in all. */
    private static final class FeeLine {
        int count;
        Money paid = Money.zero();
    }

    /** What the selection can do with one skill. */
    private static final class SkillStats {
        final String name;
        /** Improving the skill, for those who hold it. */
        final QuoteTally improve = new QuoteTally();
        /** Learning the skill, for those who lack it. */
        final QuoteTally learn = new QuoteTally();
        int levelSum;
        final int[] levels = new int[MAX_LEVEL + 1];
        /** Fees by the experience level the improvement reaches. */
        final Map<Integer, FeeLine> feesByLevel = new TreeMap<>();
        final List<Person> lackingPeople = new ArrayList<>();
        final List<Person> holderPeople = new ArrayList<>();

        SkillStats(String name) {
            this.name = name;
        }

        int holders() {
            return holderPeople.size();
        }

        /** @return everyone who can't improve the skill, by why not, those who lack it included */
        Map<TrainingRefusal, List<Person>> cannot() {
            Map<TrainingRefusal, List<Person>> cannot = new EnumMap<>(improve.cannot());
            if (!lackingPeople.isEmpty()) {
                cannot.put(TrainingRefusal.NOT_HELD, lackingPeople);
            }
            return cannot;
        }
    }

    static List<JComponent> build(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String query = state.skillQuery.trim().toLowerCase(Locale.ROOT);
        Set<String> roleSkills = console.cached("skills.roleSkills", () -> {
            Set<String> skills = new HashSet<>();
            for (Person person : selection) {
                skills.addAll(plan.context().roleSkillsOf(person));
            }
            return skills;
        });

        List<SkillStats> shown = new ArrayList<>();
        for (String skillName : SkillType.getSortedSkillNames()) {
            SkillType type = SkillType.getType(skillName);
            if ((type == null) || (!query.isEmpty() && !skillName.toLowerCase(Locale.ROOT).contains(query))
                      || !isInGroup(type, state.skillGroup)) {
                continue;
            }
            if (SkillType.S_ARTILLERY.equals(skillName) && !plan.context().isUseArtillery()) {
                continue;
            }
            // Checked before pricing anything, as most skills are held by no one selected
            if ((state.skillGroup == SkillGroup.HELD) && !roleSkills.contains(skillName)
                      && !isHeldByAnyone(plan, skillName, selection)) {
                continue;
            }
            shown.add(console.cached("skills.stats." + skillName, () -> statsFor(plan, skillName, selection)));
        }
        if (state.skillGroup == SkillGroup.HELD) {
            // Role skills first, then the skills most of the selection hold
            shown.sort(Comparator.<SkillStats, Boolean>comparing(stats -> !roleSkills.contains(stats.name))
                             .thenComparing(stats -> -stats.holders()));
        }

        List<JComponent> cards = new ArrayList<>();
        for (SkillStats stats : shown) {
            cards.add(card(console, stats, roleSkills.contains(stats.name)));
        }
        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "skills.none")));
        }
        return cards;
    }

    /** @return {@code true} if anyone selected knows the skill, staged learning included */
    static boolean isHeldByAnyone(TrainingPlan plan, String skillName, List<Person> selection) {
        for (Person person : selection) {
            PersonProjection projection = plan.peek(person);
            if (((projection == null) ? person.getSkill(skillName) : projection.skill(skillName)) != null) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} if the skill belongs to the chip group; the Aptitudes page shares these groups */
    static boolean isInGroup(SkillType type, SkillGroup group) {
        SkillSubType subType = type.getSubType();
        return switch (group) {
            case HELD, ALL -> true;
            case GUNNERY -> subType == SkillSubType.COMBAT_GUNNERY;
            case PILOTING -> subType == SkillSubType.COMBAT_PILOTING;
            case SUPPORT -> (subType == SkillSubType.SUPPORT) || (subType == SkillSubType.SUPPORT_TECHNICIAN);
            case UTILITY -> (subType == SkillSubType.UTILITY) || (subType == SkillSubType.UTILITY_COMMAND);
            case ROLEPLAY -> type.isRoleplaySkill();
        };
    }

    private static SkillStats statsFor(TrainingPlan plan, String skillName, List<Person> selection) {
        SkillStats stats = new SkillStats(skillName);
        for (Person person : selection) {
            int level = plan.projectionOf(person).skillLevel(skillName);
            SkillImprovement purchase = new SkillImprovement(person, skillName);
            Quote quote = plan.quote(purchase);
            if (level < 0) {
                // Even those who could learn it can't improve it yet, so they all count as not having it
                stats.lackingPeople.add(person);
                stats.learn.add(purchase, quote);
                continue;
            }
            stats.holderPeople.add(person);
            stats.levels[Math.min(level, MAX_LEVEL)]++;
            stats.levelSum += level;
            stats.improve.add(purchase, quote);
            if (quote.isAllowed() && !quote.fee().isZero()) {
                FeeLine line = stats.feesByLevel.computeIfAbsent(quote.reachedLevel(), key -> new FeeLine());
                line.count++;
                line.paid = line.paid.plus(quote.fee());
            }
        }
        return stats;
    }

    /**
     * @return the levels where improving into them charges a training fee for at least one of the people who hold the
     *       skill, priced the way the console charges it
     */
    private static IntPredicate paidLevels(TrainingConsole console, SkillStats stats, @Nullable SkillType type) {
        TrainingPlan plan = console.plan();
        if ((type == null) || !plan.context().isUseSkillTrainingCosts()) {
            return level -> false;
        }
        boolean hasRoleHolder = false;
        boolean hasOtherHolder = false;
        for (Person person : stats.holderPeople) {
            if (plan.context().roleSkillsOf(person).contains(stats.name)) {
                hasRoleHolder = true;
            } else {
                hasOtherHolder = true;
            }
        }
        boolean isRole = hasRoleHolder;
        boolean isOther = hasOtherHolder;
        CampaignOptions options = plan.context().options();
        return level -> (level > 0)
                              && ((isRole && !SkillTrainingCosts.getSkillImprovementCost(options, type, true,
                                    level - 1).isZero())
                                        || (isOther && !SkillTrainingCosts.getSkillImprovementCost(options, type,
                                    false, level - 1).isZero()));
    }

    private static JComponent card(TrainingConsole console, SkillStats stats, boolean isRoleSkill) {
        TrainingConsoleState state = console.state();
        boolean isOpen = stats.name.equals(state.openSkill);
        QuoteTally improve = stats.improve;

        // The skill's linked attributes, then whether it is a role skill and whether it costs C-bills
        List<JLabel> tags = new ArrayList<>();
        SkillType type = SkillType.getType(stats.name);
        if (type != null) {
            for (SkillAttribute attribute : List.of(type.getFirstAttribute(), type.getSecondAttribute())) {
                if ((attribute != null) && !attribute.isNoAttribute()) {
                    // The short name, such as DEX, keeps the tags compact; the full name is in the skill's description
                    tags.add(ConsoleWidgets.tag(attribute.getShortName(), TEXT_MUTED));
                }
            }
        }
        if (isRoleSkill) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.role"), ACCENT));
        }
        if (!improve.fees().isZero()) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.fee"), AMBER));
        }
        int shortXp = improve.count(TrainingRefusal.NOT_ENOUGH_XP);
        List<Segment> segments = List.of(
              new Segment(ConsoleWidgets.READY_COLOR, improve.ready(), getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.ready")),
              new Segment(ConsoleWidgets.SHORT_COLOR, shortXp, getTextAt(RESOURCE_BUNDLE, "skills.legend.short")),
              new Segment(ConsoleWidgets.HELD_COLOR, improve.cannotCount() - shortXp, getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.limit")),
              new Segment(ConsoleWidgets.BLOCKED_COLOR, stats.lackingPeople.size(), getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.lacking")));

        JComponent action;
        if (stats.holders() > 0) {
            boolean isAffordable = !improve.fees().isGreaterThan(console.plan().fundsLeft());
            HudButton raise = ConsoleWidgets.button(ConsoleWidgets.countedLabel("skills.raise.plain", "skills.raise",
                  improve.ready()), true, (improve.ready() > 0) && isAffordable,
                  () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "skills.step.raise", stats.name),
                        improve.purchases()));
            raise.setToolTipText(isAffordable ? null : getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford"));
            action = ConsoleWidgets.actionColumn(raise, improve.xpLine(), ConsoleWidgets.money(improve.fees()),
                  improve.unavailableReason(isAffordable));
        } else {
            HudButton learn = ConsoleWidgets.button(ConsoleWidgets.countedLabel("skills.learn.plain", "skills.learn",
                  stats.learn.ready()), true, stats.learn.ready() > 0, () -> stageLearning(console, stats));
            action = ConsoleWidgets.actionColumn(learn, stats.learn.xpLine(), "",
                  stats.learn.unavailableReason(true));
        }

        CatalogCard card = new CatalogCard(stats.name, tags, "", new StackedBar(segments), action, isOpen, () -> {
            state.openSkill = isOpen ? null : stats.name;
            console.refreshPanel();
        });
        if (!isOpen) {
            return card;
        }

        // The skill's description and linked attributes, as its tooltip in the character panel describes it
        if ((type != null) && isResourceKeyValid(type.getFlavorText(false, false))
                  && !type.getFlavorText(false, false).isBlank()) {
            card.addToBody(ConsoleWidgets.wrapped(type.getFlavorText(false, true), TEXT_MUTED));
        }
        if (stats.holders() > 0) {
            // A level is marked only where reaching it charges a training fee for someone selected: fees are on,
            // it starts a priced experience level, and the skill's category multiplier isn't zero. Whether it is a
            // role skill, which decides the category, can differ from person to person
            IntPredicate isPaid = paidLevels(console, stats, type);
            boolean hasPaidLevel = IntStream.rangeClosed(1, MAX_LEVEL).anyMatch(isPaid);
            card.addToBody(ConsoleWidgets.spreadChart(getTextAt(RESOURCE_BUNDLE, "skills.spread"), 0, MAX_LEVEL,
                  level -> stats.levels[level], hasPaidLevel ? isPaid : null,
                  hasPaidLevel ? getTextAt(RESOURCE_BUNDLE, "skills.spread.key") : null));
        }
        card.addToBody(ConsoleWidgets.legend(segments));
        if (improve.ready() > 0) {
            card.addToBody(ConsoleWidgets.label(improve.costText("skills.cost.single", "skills.cost.range"), 0.84f,
                  TEXT_MUTED));
        }
        if (stats.holders() > 0) {
            card.addToBody(feeBreakdown(console, stats));
            card.addToBody(trainTo(console, stats));
        }
        if (improve.ready() > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "skills.line.can",
                  improve.ready()), getTextAt(RESOURCE_BUNDLE, "link.toggleThem"),
                  () -> console.toggle(improve.readyPeople())));
        }
        ConsoleWidgets.cannotLines(stats.cannot(), console::toggle).forEach(card::addToBody);
        if (!stats.lackingPeople.isEmpty() && (stats.holders() > 0)) {
            JPanel learnLine = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            learnLine.add(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.reason.lacking",
                  stats.lackingPeople.size(), stats.learn.ready()), 0.84f, TEXT_MUTED), BorderLayout.CENTER);
            learnLine.add(ConsoleWidgets.button(getFormattedTextAt(RESOURCE_BUNDLE, "skills.learnXp",
                  stats.learn.xp()), false, stats.learn.ready() > 0, () -> stageLearning(console, stats)),
                  BorderLayout.EAST);
            card.addToBody(learnLine);
        }
        return card;
    }

    /** Teaches the skill to everyone selected who lacks it; those who can't are counted in the message. */
    private static void stageLearning(TrainingConsole console, SkillStats stats) {
        List<TrainingPurchase> purchases = new ArrayList<>();
        stats.lackingPeople.forEach(person -> purchases.add(new SkillImprovement(person, stats.name)));
        console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "skills.step.learn", stats.name), purchases);
    }

    private static JComponent feeBreakdown(TrainingConsole console, SkillStats stats) {
        if (!console.plan().context().isUseSkillTrainingCosts()) {
            return ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "fees.off"), 0.82f, TEXT_FAINT);
        }
        Money fees = stats.improve.fees();
        if (fees.isZero()) {
            return ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "skills.fees.none"), 0.82f, TEXT_MUTED);
        }
        List<String[]> lines = new ArrayList<>();
        for (Map.Entry<Integer, FeeLine> entry : stats.feesByLevel.entrySet()) {
            FeeLine line = entry.getValue();
            lines.add(new String[] {
                  getFormattedTextAt(RESOURCE_BUNDLE, "skills.fees.reach", line.count,
                        SkillType.getExperienceLevelName(entry.getKey())),
                  getFormattedTextAt(RESOURCE_BUNDLE, "skills.fees.each",
                        line.paid.multipliedBy(1.0 / line.count).toAmountAndSymbolString()) });
        }
        Money left = console.plan().fundsLeft().minus(fees);
        lines.add(new String[] { getTextAt(RESOURCE_BUNDLE, "fees.leftAfter"), left.toAmountAndSymbolString() });
        String warning = left.isPositiveOrZero() ? null :
                               getFormattedTextAt(RESOURCE_BUNDLE, "fees.shortBy",
                                     left.multipliedBy(-1).toAmountAndSymbolString());
        return ConsoleWidgets.feeBox(getTextAt(RESOURCE_BUNDLE, "skills.fees.title"), fees.toAmountAndSymbolString(),
              lines, warning);
    }

    /** Trains everyone who holds the skill to a chosen level, with a preview of what that costs. */
    private static JComponent trainTo(TrainingConsole console, SkillStats stats) {
        TrainingConsoleState state = console.state();
        // One level above the selection's average, to start the picker somewhere useful
        double average = (double) stats.levelSum / Math.max(1, stats.holders());
        int suggested = Math.min(MAX_LEVEL, Math.max(1, (int) Math.round(average) + 1));
        int target = state.trainTo.getOrDefault(stats.name, suggested);
        SkillType type = SkillType.getType(stats.name);

        JComboBox<Integer> picker = new JComboBox<>();
        for (int level = 1; level <= MAX_LEVEL; level++) {
            picker.addItem(level);
        }
        picker.setSelectedItem(target);
        Hud.styleComboBox(picker);
        picker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = Hud.comboRenderer().getListCellRendererComponent(list, value, index,
                      isSelected, cellHasFocus);
                if ((component instanceof JLabel label) && (value instanceof Integer level) && (type != null)) {
                    label.setText(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.level", level,
                          SkillType.getExperienceLevelName(type.getExperienceLevel(level))));
                }
                return component;
            }
        });
        picker.addActionListener(event -> {
            if ((picker.getSelectedItem() instanceof Integer level) && (level != target)) {
                state.trainTo.put(stats.name, level);
                console.refreshPanel();
            }
        });

        // Priced against what is already staged, without staging anything
        Climb climb = console.cached("skills.climb." + stats.name + '.' + target,
              () -> console.plan().previewClimb(stats.holderPeople,
                    person -> new SkillImprovement(person, stats.name),
                    projection -> projection.skillLevel(stats.name), target));

        JPanel box = ConsoleWidgets.previewBox();
        box.add(ConsoleWidgets.labeledRow(getTextAt(RESOURCE_BUNDLE, "skills.trainTo"), picker));
        box.add(Box.createVerticalStrut(scaleForGUI(6)));
        box.add(leftAligned(ConsoleWidgets.bold(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.preview",
              climb.people(), climb.steps(), climb.xp()), 0.95f, ACCENT_BRIGHT)));
        if (!climb.fees().isZero()) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.fees",
                  climb.fees().toAmountAndSymbolString()), 0.84f, AMBER)));
        }
        if (climb.stopShort() > 0) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.short",
                  climb.stopShort(), target), 0.8f, TEXT_FAINT)));
        }
        box.add(ConsoleWidgets.buttonRow(ConsoleWidgets.button(getFormattedTextAt(RESOURCE_BUNDLE,
              "skills.trainTo.stage", target), true, !climb.purchases().isEmpty(),
              () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "skills.step.trainTo", stats.name, target),
                    climb.purchases()))));
        return box;
    }
}
