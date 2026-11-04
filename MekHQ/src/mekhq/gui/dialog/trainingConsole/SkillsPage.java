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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.personnel.training.PersonProjection;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
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

    /** What the selection can do with one skill. */
    private static final class SkillStats {
        final String name;
        int holders;
        int ready;
        int shortXp;
        int atLimit;
        int learnReady;
        int learnXp;
        int xp;
        int minXp = Integer.MAX_VALUE;
        int maxXp;
        int minLevel = Integer.MAX_VALUE;
        int maxLevel = -1;
        int levelSum;
        int aptitudeReady;
        int aptitudeXp;
        Money fees = Money.zero();
        final int[] levels = new int[MAX_LEVEL + 1];
        /** Fees by the experience level the improvement reaches: how many people, and what they pay in all. */
        final Map<Integer, Object[]> feesByLevel = new TreeMap<>();
        final List<Person> readyPeople = new ArrayList<>();
        final List<Person> shortPeople = new ArrayList<>();
        /** Everyone who can't improve or learn the skill, by why not. */
        final Map<TrainingRefusal, List<Person>> cannot = new EnumMap<>(TrainingRefusal.class);
        final List<Person> lackingPeople = new ArrayList<>();
        final List<Person> holderPeople = new ArrayList<>();

        SkillStats(String name) {
            this.name = name;
        }
    }

    static List<JComponent> build(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String query = state.skillQuery.trim().toLowerCase(Locale.ROOT);

        Set<String> roleSkills = new HashSet<>();
        for (Person person : selection) {
            roleSkills.addAll(plan.context().roleSkillsOf(person));
        }

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
            SkillStats stats = statsFor(plan, skillName, selection);
            if ((state.skillGroup == SkillGroup.HELD) && (stats.holders == 0) && !roleSkills.contains(skillName)) {
                continue;
            }
            shown.add(stats);
        }
        if (state.skillGroup == SkillGroup.HELD) {
            // Role skills first, then the skills most of the selection hold
            shown.sort(Comparator.<SkillStats, Boolean>comparing(stats -> !roleSkills.contains(stats.name))
                             .thenComparing(stats -> -stats.holders));
        }

        List<JComponent> cards = new ArrayList<>();
        for (SkillStats stats : shown) {
            cards.add(card(console, stats, selection, roleSkills.contains(stats.name)));
        }
        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "skills.none")));
        }
        return cards;
    }

    private static boolean isInGroup(SkillType type, SkillGroup group) {
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
            PersonProjection projection = plan.projectionOf(person);
            int level = projection.skillLevel(skillName);
            Quote quote = plan.quote(new SkillImprovement(person, skillName));
            if (level < 0) {
                stats.lackingPeople.add(person);
                // Even those who could learn it can't improve it yet, so they all count as not having it
                stats.cannot.computeIfAbsent(TrainingRefusal.NOT_HELD, key -> new ArrayList<>()).add(person);
                if (quote.isAllowed()) {
                    stats.learnReady++;
                    stats.learnXp += quote.xp();
                }
                continue;
            }
            stats.holders++;
            stats.holderPeople.add(person);
            stats.levels[Math.min(level, MAX_LEVEL)]++;
            stats.levelSum += level;
            stats.minLevel = Math.min(stats.minLevel, level);
            stats.maxLevel = Math.max(stats.maxLevel, level);
            Quote aptitude = plan.quote(new NaturalAptitude(person, skillName));
            if (aptitude.isAllowed()) {
                stats.aptitudeReady++;
                stats.aptitudeXp += aptitude.xp();
            }
            if (quote.isAllowed()) {
                stats.ready++;
                stats.readyPeople.add(person);
                stats.xp += quote.xp();
                stats.minXp = Math.min(stats.minXp, quote.xp());
                stats.maxXp = Math.max(stats.maxXp, quote.xp());
                stats.fees = stats.fees.plus(quote.fee());
                if (!quote.fee().isZero()) {
                    Object[] line = stats.feesByLevel.computeIfAbsent(quote.reachedLevel(),
                          key -> new Object[] { 0, Money.zero() });
                    line[0] = (Integer) line[0] + 1;
                    line[1] = ((Money) line[1]).plus(quote.fee());
                }
            } else if (quote.refusal() == TrainingRefusal.NOT_ENOUGH_XP) {
                stats.shortXp++;
                stats.shortPeople.add(person);
                stats.cannot.computeIfAbsent(TrainingRefusal.NOT_ENOUGH_XP, key -> new ArrayList<>()).add(person);
            } else {
                stats.atLimit++;
                stats.cannot.computeIfAbsent(quote.refusal(), key -> new ArrayList<>()).add(person);
            }
        }
        return stats;
    }

    private static JComponent card(TrainingConsole console, SkillStats stats, List<Person> selection,
          boolean isRoleSkill) {
        TrainingConsoleState state = console.state();
        boolean isOpen = stats.name.equals(state.openSkill);

        // The skill's linked attributes, then whether it is a role skill and whether it costs C-bills
        List<JLabel> tags = new ArrayList<>();
        SkillType skillType = SkillType.getType(stats.name);
        if (skillType != null) {
            for (SkillAttribute attribute : List.of(skillType.getFirstAttribute(), skillType.getSecondAttribute())) {
                if ((attribute != null) && !attribute.isNoAttribute()) {
                    // The short name, such as DEX, keeps the tags compact; the full name is in the skill's description
                    tags.add(ConsoleWidgets.tag(attribute.getShortName(), TEXT_MUTED));
                }
            }
        }
        if (isRoleSkill) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.role"), ACCENT));
        }
        if (!stats.fees.isZero()) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.fee"), AMBER));
        }
        List<Segment> segments = List.of(
              new Segment(ConsoleWidgets.READY_COLOR, stats.ready, getTextAt(RESOURCE_BUNDLE, "skills.legend.ready")),
              new Segment(ConsoleWidgets.SHORT_COLOR, stats.shortXp, getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.short")),
              new Segment(ConsoleWidgets.HELD_COLOR, stats.atLimit, getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.limit")),
              new Segment(ConsoleWidgets.BLOCKED_COLOR, stats.lackingPeople.size(), getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.lacking")));

        JComponent action;
        if (stats.holders > 0) {
            HudButton raise = new HudButton(ConsoleWidgets.countedLabel("skills.raise.plain", "skills.raise",
                  stats.ready).toUpperCase(Locale.ROOT), true, true);
            boolean isAffordable = !stats.fees.isGreaterThan(console.plan().fundsLeft());
            raise.setArmed((stats.ready > 0) && isAffordable);
            raise.setToolTipText(isAffordable ? null : getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford"));
            raise.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                  "skills.step.raise", stats.name), improvements(stats.readyPeople, stats.name, 1)));
            String reason = null;
            if (!isAffordable) {
                reason = getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford.short");
            } else if (stats.ready == 0) {
                reason = (stats.shortXp >= stats.atLimit) ?
                               ConsoleWidgets.reason(stats.shortXp, TrainingRefusal.NOT_ENOUGH_XP) :
                               ConsoleWidgets.reason(stats.atLimit, TrainingRefusal.AT_LIMIT);
            }
            action = ConsoleWidgets.actionColumn(raise,
                  (stats.ready > 0) ? getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", stats.xp) : "",
                  ConsoleWidgets.money(stats.fees), reason);
        } else {
            HudButton learn = new HudButton(ConsoleWidgets.countedLabel("skills.learn.plain", "skills.learn",
                  stats.learnReady).toUpperCase(Locale.ROOT), true, true);
            learn.setArmed(stats.learnReady > 0);
            learn.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                  "skills.step.learn", stats.name), improvements(stats.lackingPeople, stats.name, 1)));
            int cannotLearn = stats.lackingPeople.size() - stats.learnReady;
            action = ConsoleWidgets.actionColumn(learn,
                  (stats.learnReady > 0) ? getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", stats.learnXp) : "",
                  "", (stats.learnReady == 0) ? ConsoleWidgets.reason(cannotLearn, TrainingRefusal.NOT_ENOUGH_XP) :
                            null);
        }

        CatalogCard card = new CatalogCard(stats.name, tags, "", new StackedBar(segments), action, isOpen, () -> {
            state.openSkill = isOpen ? null : stats.name;
            console.refreshPanel();
        });
        if (!isOpen) {
            return card;
        }

        // The skill's description and linked attributes, as its tooltip in the character panel describes it
        SkillType type = SkillType.getType(stats.name);
        if ((type != null) && isResourceKeyValid(type.getFlavorText(false, false))
                  && !type.getFlavorText(false, false).isBlank()) {
            card.addToBody(ConsoleWidgets.wrapped(type.getFlavorText(false, true), TEXT_MUTED));
        }
        if (stats.holders > 0) {
            card.addToBody(levelSpread(stats));
        }
        card.addToBody(ConsoleWidgets.legend(segments));
        if (stats.ready > 0) {
            card.addToBody(ConsoleWidgets.label((stats.minXp == stats.maxXp) ?
                                                      getFormattedTextAt(RESOURCE_BUNDLE, "skills.cost.single",
                                                            stats.minXp) :
                                                      getFormattedTextAt(RESOURCE_BUNDLE, "skills.cost.range",
                                                            stats.minXp, stats.maxXp), 0.84f, TEXT_MUTED));
        }
        if (stats.holders > 0) {
            card.addToBody(feeBreakdown(console, stats));
            card.addToBody(trainTo(console, stats));
        }
        if (stats.ready > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "skills.line.can",
                  stats.ready), getTextAt(RESOURCE_BUNDLE, "link.toggleThem"),
                  () -> console.toggle(stats.readyPeople)));
        }
        ConsoleWidgets.cannotLines(stats.cannot, console::toggle).forEach(card::addToBody);
        if (!stats.lackingPeople.isEmpty() && (stats.holders > 0)) {
            JPanel learnLine = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            learnLine.add(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.reason.lacking",
                  stats.lackingPeople.size(), stats.learnReady), 0.84f, TEXT_MUTED), BorderLayout.CENTER);
            HudButton learn = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "skills.learnXp", stats.learnXp)
                                                  .toUpperCase(Locale.ROOT), false, true);
            learn.setArmed(stats.learnReady > 0);
            learn.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                  "skills.step.learn", stats.name), improvements(stats.lackingPeople, stats.name, 1)));
            learnLine.add(learn, BorderLayout.EAST);
            card.addToBody(learnLine);
        }
        if (stats.aptitudeReady > 0) {
            JPanel aptitudeLine = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            aptitudeLine.add(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.aptitude",
                  stats.aptitudeReady), 0.84f, TEXT_MUTED), BorderLayout.CENTER);
            HudButton aptitude = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "skills.aptitudeXp",
                  stats.aptitudeXp).toUpperCase(Locale.ROOT), false, true);
            aptitude.addActionListener(event -> {
                List<TrainingPurchase> purchases = new ArrayList<>();
                stats.holderPeople.forEach(person -> purchases.add(new NaturalAptitude(person, stats.name)));
                console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "skills.step.aptitude", stats.name), purchases);
            });
            aptitudeLine.add(aptitude, BorderLayout.EAST);
            card.addToBody(aptitudeLine);
        }
        return card;
    }

    /** @return {@code steps} improvements per person, in order: everyone's first level, then everyone's second */
    private static List<TrainingPurchase> improvements(List<Person> people, String skillName, int steps) {
        List<TrainingPurchase> purchases = new ArrayList<>();
        for (int step = 0; step < steps; step++) {
            for (Person person : people) {
                purchases.add(new SkillImprovement(person, skillName));
            }
        }
        return purchases;
    }

    /** A small chart of how many of the selection hold each level, with the priced experience levels marked. */
    private static JComponent levelSpread(SkillStats stats) {
        SkillType type = SkillType.getType(stats.name);
        int most = 1;
        for (int count : stats.levels) {
            most = Math.max(most, count);
        }
        final int highest = most;
        JComponent chart = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                int columns = stats.levels.length;
                int gap = scaleForGUI(3);
                int width = (getWidth() - (gap * (columns - 1))) / columns;
                int labelHeight = scaleForGUI(12);
                int barArea = getHeight() - labelHeight;
                graphics.setFont(ConsoleWidgets.label("", 0.7f, TEXT_FAINT).getFont());
                for (int level = 0; level < columns; level++) {
                    int x = level * (width + gap);
                    int height = (int) Math.round((double) stats.levels[level] / highest * (barArea - 2));
                    graphics.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 140));
                    graphics.fillRect(x, barArea - Math.max(height, (stats.levels[level] > 0) ? 2 : 0), width,
                          Math.max(height, (stats.levels[level] > 0) ? 2 : 0));
                    // A level where a new experience level starts is marked, so the player can see where fees land
                    boolean isThreshold = (type != null) && (level > 0)
                                                && (type.getExperienceLevel(level)
                                                          > type.getExperienceLevel(level - 1));
                    graphics.setColor(isThreshold ? AMBER : TEXT_FAINT);
                    String text = String.valueOf(level);
                    int textWidth = graphics.getFontMetrics().stringWidth(text);
                    graphics.drawString(text, x + ((width - textWidth) / 2), getHeight() - 1);
                }
            }
        };
        chart.setPreferredSize(new Dimension(scaleForGUI(300), scaleForGUI(56)));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(56)));
        JPanel holder = ConsoleWidgets.column();
        holder.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "skills.spread"))));
        holder.add(Box.createVerticalStrut(scaleForGUI(4)));
        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.add(chart);
        holder.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "skills.spread.key"), 0.74f,
              TEXT_FAINT)));
        return holder;
    }

    private static JComponent feeBreakdown(TrainingConsole console, SkillStats stats) {
        if (!console.plan().context().isUseSkillTrainingCosts()) {
            return ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "fees.off"), 0.82f, TEXT_FAINT);
        }
        if (stats.fees.isZero()) {
            return ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "skills.fees.none"), 0.82f, TEXT_MUTED);
        }
        List<String[]> lines = new ArrayList<>();
        for (Map.Entry<Integer, Object[]> entry : stats.feesByLevel.entrySet()) {
            int count = (Integer) entry.getValue()[0];
            Money paid = (Money) entry.getValue()[1];
            lines.add(new String[] {
                  getFormattedTextAt(RESOURCE_BUNDLE, "skills.fees.reach", count,
                        SkillType.getExperienceLevelName(entry.getKey())),
                  getFormattedTextAt(RESOURCE_BUNDLE, "skills.fees.each",
                        paid.multipliedBy(1.0 / count).toAmountAndSymbolString()) });
        }
        Money left = console.plan().fundsLeft().minus(stats.fees);
        lines.add(new String[] { getTextAt(RESOURCE_BUNDLE, "fees.leftAfter"), left.toAmountAndSymbolString() });
        String warning = left.isPositiveOrZero() ? null :
                               getFormattedTextAt(RESOURCE_BUNDLE, "fees.shortBy",
                                     left.multipliedBy(-1).toAmountAndSymbolString());
        return ConsoleWidgets.feeBox(getTextAt(RESOURCE_BUNDLE, "skills.fees.title"),
              stats.fees.toAmountAndSymbolString(), lines, warning);
    }

    /** Trains everyone who holds the skill to a chosen level, with a preview of what that costs. */
    private static JComponent trainTo(TrainingConsole console, SkillStats stats) {
        TrainingConsoleState state = console.state();
        int suggested = Math.min(MAX_LEVEL, Math.max(1, (int) Math.round((double) stats.levelSum
                                                                                   / Math.max(1, stats.holders)) + 1));
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
        Consumer<Integer> choose = level -> {
            state.trainTo.put(stats.name, level);
            console.refreshPanel();
        };
        picker.addActionListener(event -> {
            if (picker.getSelectedItem() instanceof Integer level && (level != target)) {
                choose.accept(level);
            }
        });

        // Price the whole climb against what is already staged, on a copy, so the preview never stages anything
        TrainingPlan trial = console.plan().copy();
        List<TrainingPurchase> purchases = new ArrayList<>();
        int people = 0;
        int levels = 0;
        int xp = 0;
        Money fees = Money.zero();
        int stopShort = 0;
        for (Person person : stats.holderPeople) {
            boolean isTrained = false;
            while (trial.projectionOf(person).skillLevel(stats.name) < target) {
                SkillImprovement purchase = new SkillImprovement(person, stats.name);
                Quote quote = trial.quote(purchase);
                if (!quote.isAllowed() || quote.fee().isGreaterThan(trial.fundsLeft())) {
                    stopShort++;
                    break;
                }
                if (trial.stage("", List.of(purchase)).stagedCount() == 0) {
                    stopShort++;
                    break;
                }
                purchases.add(purchase);
                levels++;
                xp += quote.xp();
                fees = fees.plus(quote.fee());
                isTrained = true;
            }
            if (isTrained) {
                people++;
            }
        }

        JPanel box = ConsoleWidgets.column();
        box.setOpaque(true);
        box.setBackground(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 14));
        box.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(new Color(ACCENT.getRed(), ACCENT.getGreen(),
                    ACCENT.getBlue(), 90), scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(8),
                    scaleForGUI(8))));
        JPanel pickerRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        pickerRow.add(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "skills.trainTo"), 0.84f, TEXT_MUTED),
              BorderLayout.WEST);
        pickerRow.add(picker, BorderLayout.CENTER);
        pickerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(pickerRow);
        box.add(Box.createVerticalStrut(scaleForGUI(6)));
        box.add(leftAligned(ConsoleWidgets.bold(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.preview",
              people, levels, xp), 0.95f, ACCENT_BRIGHT)));
        if (!fees.isZero()) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.fees",
                  fees.toAmountAndSymbolString()), 0.84f, AMBER)));
        }
        if (stopShort > 0) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.short",
                  stopShort, target), 0.8f, TEXT_FAINT)));
        }
        HudButton stage = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "skills.trainTo.stage", target)
                                              .toUpperCase(Locale.ROOT), true, true);
        stage.setArmed(!purchases.isEmpty());
        final int finalTarget = target;
        stage.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "skills.step.trainTo",
              stats.name, finalTarget), purchases));
        JPanel buttonRow = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0,
              scaleForGUI(6)));
        buttonRow.add(stage);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(buttonRow);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }
}
