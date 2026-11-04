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
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPricer;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.Segment;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.StackedBar;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.AttributeGroup;

/**
 * The Attributes &amp; Traits page: raising attributes, moving traits up or down a step, and refilling spent Edge,
 * each for the whole selection at once. Each card works like a skill card: an eligibility bar, and when opened, the
 * description, the spread of the selection's current values, who can and can't, and, for attributes, raising
 * everyone to a chosen score.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class AttributesPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    private static final String EDGE_CARD = "edge";

    private AttributesPage() {
    }

    /** What the selection can do with one purchase. */
    private static final class Tally {
        int ready;
        int xp;
        int minXp = Integer.MAX_VALUE;
        int maxXp = Integer.MIN_VALUE;
        final List<TrainingPurchase> purchases = new ArrayList<>();
        final List<Person> readyPeople = new ArrayList<>();
        final Map<TrainingRefusal, List<Person>> cannot = new EnumMap<>(TrainingRefusal.class);

        int count(TrainingRefusal refusal) {
            return cannot.getOrDefault(refusal, List.of()).size();
        }

        /** @return why no one can, for under a disabled button, or {@code null} if someone can */
        @Nullable String reason() {
            if (ready > 0) {
                return null;
            }
            return cannot.entrySet().stream()
                         .max(Map.Entry.comparingByValue((first, second) -> Integer.compare(first.size(),
                               second.size())))
                         .map(entry -> ConsoleWidgets.reason(entry.getValue().size(), entry.getKey()))
                         .orElse(null);
        }

        /** The eligibility bar's parts: can now, short on XP, at the limit, and blocked for any other reason. */
        List<Segment> segments(String readyKey) {
            int other = cannot.entrySet().stream()
                              .filter(entry -> (entry.getKey() != TrainingRefusal.NOT_ENOUGH_XP)
                                                     && (entry.getKey() != TrainingRefusal.AT_LIMIT))
                              .mapToInt(entry -> entry.getValue().size()).sum();
            return List.of(
                  new Segment(ConsoleWidgets.READY_COLOR, ready, getTextAt(RESOURCE_BUNDLE, readyKey)),
                  new Segment(ConsoleWidgets.SHORT_COLOR, count(TrainingRefusal.NOT_ENOUGH_XP),
                        getTextAt(RESOURCE_BUNDLE, "skills.legend.short")),
                  new Segment(ConsoleWidgets.HELD_COLOR, count(TrainingRefusal.AT_LIMIT),
                        getTextAt(RESOURCE_BUNDLE, "skills.legend.limit")),
                  new Segment(ConsoleWidgets.BLOCKED_COLOR, other, getTextAt(RESOURCE_BUNDLE,
                        "abilities.legend.blocked")));
        }
    }

    private static Tally tally(TrainingPlan plan, List<Person> selection,
          Function<Person, TrainingPurchase> purchaseFor) {
        Tally tally = new Tally();
        for (Person person : selection) {
            TrainingPurchase purchase = purchaseFor.apply(person);
            Quote quote = plan.quote(purchase);
            if (quote.isAllowed()) {
                tally.ready++;
                tally.xp += quote.xp();
                tally.minXp = Math.min(tally.minXp, quote.xp());
                tally.maxXp = Math.max(tally.maxXp, quote.xp());
                tally.purchases.add(purchase);
                tally.readyPeople.add(person);
            } else {
                tally.cannot.computeIfAbsent(quote.refusal(), key -> new ArrayList<>()).add(person);
            }
        }
        return tally;
    }

    static List<JComponent> build(TrainingConsole console, List<Person> selection) {
        TrainingPlan plan = console.plan();
        TrainingConsoleState state = console.state();
        AttributeGroup group = state.attributeGroup;
        String query = state.attributeQuery.trim().toLowerCase(Locale.ROOT);
        List<JComponent> cards = new ArrayList<>();
        if ((group == AttributeGroup.EDGE) && !plan.context().isUseEdge()) {
            return List.of(Hud.notice(getTextAt(RESOURCE_BUNDLE, "edge.disabled")));
        }

        for (SkillAttribute attribute : SkillAttribute.values()) {
            // Edge is an attribute, but it is bought with the Edge refill, under its own group
            boolean isEdge = attribute == SkillAttribute.EDGE;
            if (attribute.isNoAttribute() || (isEdge != (group == AttributeGroup.EDGE))
                      || (group == AttributeGroup.TRAITS) || !matches(attribute.getLabel(), query)) {
                continue;
            }
            cards.add(attributeCard(console, selection, attribute));
        }

        for (ATOWTraits trait : ATOWTraits.values()) {
            if ((group == AttributeGroup.TRAITS) && TrainingPricer.isTrainableTrait(trait)
                      && matches(trait.getDisplayName(), query)) {
                cards.add(traitCard(console, selection, trait));
            }
        }

        if ((group == AttributeGroup.EDGE) && matches(getTextAt(RESOURCE_BUNDLE, "edge.title"), query)) {
            cards.add(edgeCard(console, selection));
        }

        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "attributes.none")));
        }
        JLabel note = ConsoleWidgets.wrapped(getTextAt(RESOURCE_BUNDLE, "attributes.note"), TEXT_FAINT);
        note.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(4), 0, 0));
        cards.add(note);
        return cards;
    }

    // region Attributes

    private static int attributeScore(TrainingPlan plan, Person person, SkillAttribute attribute) {
        int base = (attribute == SkillAttribute.EDGE) ? person.getEdge() : person.getAttributeScore(attribute);
        return base + plan.projectionOf(person).attributeChange(attribute);
    }

    private static int attributeCap(TrainingPlan plan, Person person, SkillAttribute attribute) {
        int cap = person.getAttributeCap(attribute);
        return (attribute == SkillAttribute.EDGE) ? Math.min(cap, plan.context().maximumEdge()) : cap;
    }

    private static JComponent attributeCard(TrainingConsole console, List<Person> selection,
          SkillAttribute attribute) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String key = attribute.name();
        boolean isOpen = key.equals(state.openAttribute);
        Tally raise = tally(plan, selection, person -> new AttributeIncrease(person, attribute));
        List<Segment> segments = raise.segments("attributes.legend.ready");

        HudButton button = button(ConsoleWidgets.countedLabel("attributes.raise.plain", "attributes.raise",
              raise.ready), raise.ready > 0, () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
              "attributes.step.raise", attribute.getLabel()), raise.purchases));
        JComponent action = ConsoleWidgets.actionColumn(button, xpLine(raise.xp, raise.ready), "", raise.reason());
        CatalogCard card = new CatalogCard(attribute.getLabel(), List.of(), "", new StackedBar(segments), action,
              isOpen, () -> toggleOpen(console, key));
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(attribute.getDescription(), TEXT_MUTED));
        card.addToBody(spread(selection, person -> attributeScore(plan, person, attribute),
              getTextAt(RESOURCE_BUNDLE, "attributes.spread")));
        card.addToBody(ConsoleWidgets.legend(segments));
        addCostLine(card, raise);
        card.addToBody(raiseTo(console, selection, attribute));
        addCanLines(console, card, raise, "attributes.line.can");
        return card;
    }

    /** Raises everyone to a chosen score, with a preview of what that costs. */
    private static JComponent raiseTo(TrainingConsole console, List<Person> selection, SkillAttribute attribute) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int cap = 0;
        for (Person person : selection) {
            int score = attributeScore(plan, person, attribute);
            lowest = Math.min(lowest, score);
            highest = Math.max(highest, score);
            cap = Math.max(cap, attributeCap(plan, person, attribute));
        }
        int first = Math.min(cap, lowest + 1);
        int target = Math.clamp(state.attributeTargets.getOrDefault(attribute.name(), Math.min(cap, highest)), first,
              Math.max(first, cap));

        JComboBox<Integer> picker = new JComboBox<>();
        for (int score = first; score <= Math.max(first, cap); score++) {
            picker.addItem(score);
        }
        picker.setSelectedItem(target);
        Hud.styleComboBox(picker);
        final int chosen = target;
        picker.addActionListener(event -> {
            if ((picker.getSelectedItem() instanceof Integer score) && (score != chosen)) {
                state.attributeTargets.put(attribute.name(), score);
                console.refreshPanel();
            }
        });

        // Priced on a copy of the plan, so the preview never stages anything
        TrainingPlan trial = plan.copy();
        List<TrainingPurchase> purchases = new ArrayList<>();
        int people = 0;
        int points = 0;
        int xp = 0;
        int stopShort = 0;
        for (Person person : selection) {
            boolean isRaised = false;
            while (attributeScore(trial, person, attribute) < target) {
                AttributeIncrease purchase = new AttributeIncrease(person, attribute);
                Quote quote = trial.quote(purchase);
                if (!quote.isAllowed() || (trial.stage("", List.of(purchase)).stagedCount() == 0)) {
                    stopShort++;
                    break;
                }
                purchases.add(purchase);
                points++;
                xp += quote.xp();
                isRaised = true;
            }
            if (isRaised) {
                people++;
            }
        }

        JPanel box = ConsoleWidgets.column();
        box.setOpaque(true);
        box.setBackground(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 14));
        box.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 90),
                    scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(8), scaleForGUI(8))));
        JPanel pickerRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        pickerRow.add(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "attributes.raiseTo"), 0.84f, TEXT_MUTED),
              BorderLayout.WEST);
        pickerRow.add(picker, BorderLayout.CENTER);
        pickerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(pickerRow);
        box.add(Box.createVerticalStrut(scaleForGUI(6)));
        box.add(leftAligned(ConsoleWidgets.bold(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raiseTo.preview",
              people, points, xp), 0.95f, ACCENT_BRIGHT)));
        if (stopShort > 0) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raiseTo.short",
                  stopShort, target), 0.8f, TEXT_FAINT)));
        }
        HudButton stage = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raiseTo.stage", target)
                                              .toUpperCase(Locale.ROOT), true, true);
        stage.setArmed(!purchases.isEmpty());
        stage.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.step.raiseTo",
              attribute.getLabel(), chosen), purchases));
        JPanel buttonRow = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, scaleForGUI(6)));
        buttonRow.add(stage);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(buttonRow);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    // endregion Attributes

    // region Traits and Edge

    private static JComponent traitCard(TrainingConsole console, List<Person> selection, ATOWTraits trait) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String key = trait.name();
        boolean isOpen = key.equals(state.openAttribute);
        Tally raise = tally(plan, selection, person -> new TraitChange(person, trait, 1));
        // Connections can only be bought, never sold back
        Tally lower = (trait == ATOWTraits.CONNECTIONS) ? null :
                            tally(plan, selection, person -> new TraitChange(person, trait, -1));

        JPanel buttons = ConsoleWidgets.column();
        buttons.add(traitButton(console, trait, 1, raise));
        if (lower != null) {
            buttons.add(Box.createVerticalStrut(scaleForGUI(4)));
            buttons.add(traitButton(console, trait, -1, lower));
        }
        CatalogCard card = new CatalogCard(trait.getDisplayName(), List.of(), "",
              new StackedBar(raise.segments("traits.legend.raise")), buttons, isOpen, () -> toggleOpen(console, key));
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(trait.getDescription(), TEXT_MUTED));
        card.addToBody(spread(selection, person -> TrainingPricer.traitLevel(person, trait)
                                                         + plan.projectionOf(person).traitChange(trait),
              getTextAt(RESOURCE_BUNDLE, "traits.spread")));
        card.addToBody(directionSection(console, raise, getTextAt(RESOURCE_BUNDLE, "traits.direction.raise"),
              "traits.legend.raise", "traits.line.canRaise"));
        if (lower != null) {
            card.addToBody(directionSection(console, lower, getTextAt(RESOURCE_BUNDLE, "traits.direction.lower"),
                  "traits.legend.lower", "traits.line.canLower"));
        }
        return card;
    }

    private static JComponent traitButton(TrainingConsole console, ATOWTraits trait, int delta, Tally tally) {
        String key = (delta > 0) ? "traits.raise" : "traits.lower";
        HudButton button = button(ConsoleWidgets.countedLabel(key + ".plain", key, tally.ready), tally.ready > 0,
              () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                    (delta > 0) ? "traits.step.raise" : "traits.step.lower", trait.getDisplayName()),
                    tally.purchases));
        button.setToolTipText(getTextAt(RESOURCE_BUNDLE, TrainingPricer.isCostlyDirection(trait, delta) ?
                                                               "traits.costs.toolTipText" :
                                                               "traits.gives.toolTipText"));
        return ConsoleWidgets.actionColumn(button, xpLine(tally.xp, tally.ready), "", tally.reason());
    }

    /** One direction of a trait: its own bar, cost, and who can and can't move that way. */
    private static JComponent directionSection(TrainingConsole console, Tally tally, String heading,
          String readyKey, String canKey) {
        JPanel section = ConsoleWidgets.column();
        section.add(leftAligned(Hud.eyebrow(heading)));
        section.add(Box.createVerticalStrut(scaleForGUI(4)));
        List<Segment> segments = tally.segments(readyKey);
        StackedBar bar = new StackedBar(segments);
        section.add(bar);
        section.add(Box.createVerticalStrut(scaleForGUI(4)));
        section.add(ConsoleWidgets.legend(segments));
        if (tally.ready > 0) {
            section.add(leftAligned(ConsoleWidgets.label(costText(tally), 0.84f, TEXT_MUTED)));
            section.add(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, canKey, tally.ready),
                  getTextAt(RESOURCE_BUNDLE, "link.toggleThem"), () -> console.toggle(tally.readyPeople)));
        }
        ConsoleWidgets.cannotLines(tally.cannot, console::toggle).forEach(section::add);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private static JComponent edgeCard(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        boolean isOpen = EDGE_CARD.equals(state.openAttribute);
        Tally refill = tally(plan, selection, EdgeRefill::new);
        List<Segment> segments = refill.segments("edge.legend.ready");
        HudButton button = button(ConsoleWidgets.countedLabel("edge.refill.plain", "edge.refill", refill.ready),
              refill.ready > 0, () -> console.stage(getTextAt(RESOURCE_BUNDLE, "edge.step"), refill.purchases));
        CatalogCard card = new CatalogCard(getTextAt(RESOURCE_BUNDLE, "edge.title"), List.of(), "",
              new StackedBar(segments), ConsoleWidgets.actionColumn(button, xpLine(refill.xp, refill.ready), "",
              refill.reason()), isOpen, () -> toggleOpen(console, EDGE_CARD));
        if (!isOpen) {
            return card;
        }
        card.addToBody(ConsoleWidgets.wrapped(getTextAt(RESOURCE_BUNDLE, "edge.description"), TEXT_MUTED));
        card.addToBody(spread(selection, Person::getUsedEdge, getTextAt(RESOURCE_BUNDLE, "edge.spread")));
        card.addToBody(ConsoleWidgets.legend(segments));
        addCostLine(card, refill);
        addCanLines(console, card, refill, "edge.line.can");
        return card;
    }

    // endregion Traits and Edge

    // region Shared parts

    private static void toggleOpen(TrainingConsole console, String key) {
        TrainingConsoleState state = console.state();
        state.openAttribute = key.equals(state.openAttribute) ? null : key;
        console.refreshPanel();
    }

    private static void addCostLine(CatalogCard card, Tally tally) {
        if (tally.ready > 0) {
            card.addToBody(ConsoleWidgets.label(costText(tally), 0.84f, TEXT_MUTED));
        }
    }

    private static String costText(Tally tally) {
        return (tally.minXp == tally.maxXp) ?
                     getFormattedTextAt(RESOURCE_BUNDLE, "attributes.cost.single", tally.minXp) :
                     getFormattedTextAt(RESOURCE_BUNDLE, "attributes.cost.range", tally.minXp, tally.maxXp);
    }

    private static void addCanLines(TrainingConsole console, CatalogCard card, Tally tally, String canKey) {
        if (tally.ready > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, canKey, tally.ready),
                  getTextAt(RESOURCE_BUNDLE, "link.toggleThem"), () -> console.toggle(tally.readyPeople)));
        }
        ConsoleWidgets.cannotLines(tally.cannot, console::toggle).forEach(card::addToBody);
    }

    /** A small chart of how many of the selection hold each value, from the lowest held to the highest. */
    private static JComponent spread(List<Person> selection, ToIntFunction<Person> value, String heading) {
        Map<Integer, Integer> counts = new TreeMap<>();
        for (Person person : selection) {
            counts.merge(value.applyAsInt(person), 1, Integer::sum);
        }
        int lowest = counts.isEmpty() ? 0 : ((TreeMap<Integer, Integer>) counts).firstKey();
        int highest = counts.isEmpty() ? 0 : ((TreeMap<Integer, Integer>) counts).lastKey();
        int most = counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        JComponent chart = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                int columns = (highest - lowest) + 1;
                int gap = scaleForGUI(3);
                int width = Math.max(1, (getWidth() - (gap * (columns - 1))) / columns);
                int labelHeight = scaleForGUI(12);
                int barArea = getHeight() - labelHeight;
                graphics.setFont(ConsoleWidgets.label("", 0.7f, TEXT_FAINT).getFont());
                for (int column = 0; column < columns; column++) {
                    int count = counts.getOrDefault(lowest + column, 0);
                    int x = column * (width + gap);
                    int height = (count == 0) ? 0 : Math.max(2, (int) Math.round((double) count / most
                                                                                       * (barArea - 2)));
                    graphics.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 140));
                    graphics.fillRect(x, barArea - height, width, height);
                    graphics.setColor(TEXT_FAINT);
                    String text = String.valueOf(lowest + column);
                    int textWidth = graphics.getFontMetrics().stringWidth(text);
                    graphics.drawString(text, x + ((width - textWidth) / 2), getHeight() - 1);
                }
            }
        };
        chart.setPreferredSize(new Dimension(scaleForGUI(300), scaleForGUI(56)));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(56)));
        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel holder = ConsoleWidgets.column();
        holder.add(leftAligned(Hud.eyebrow(heading)));
        holder.add(Box.createVerticalStrut(scaleForGUI(4)));
        holder.add(chart);
        return holder;
    }

    private static boolean matches(String label, String query) {
        return query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query);
    }

    private static HudButton button(String text, boolean isArmed, Runnable onClick) {
        HudButton button = new HudButton(text.toUpperCase(Locale.ROOT), true, true);
        button.setArmed(isArmed);
        button.addActionListener(event -> onClick.run());
        return button;
    }

    private static String xpLine(int xp, int ready) {
        if (ready == 0) {
            return "";
        }
        return (xp < 0) ? getFormattedTextAt(RESOURCE_BUNDLE, "xp.gained", -xp) :
                     getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", xp);
    }

    // endregion Shared parts
}
