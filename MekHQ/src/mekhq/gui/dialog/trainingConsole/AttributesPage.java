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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPlan.Climb;
import mekhq.campaign.personnel.training.TrainingPricer;
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

    /** Prices one purchase per selected person, worked out once until the plan or the selection changes. */
    private static QuoteTally tally(TrainingConsole console, String key, List<Person> selection,
          Function<Person, ? extends TrainingPurchase> purchaseFor) {
        return console.cached("attributes.tally." + key, () -> QuoteTally.of(console.plan(), selection, purchaseFor));
    }

    /** The eligibility bar's parts: can now, short on XP, at the limit, and blocked for any other reason. */
    private static List<Segment> segments(QuoteTally tally, String readyKey) {
        int shortXp = tally.count(TrainingRefusal.NOT_ENOUGH_XP);
        int atLimit = tally.count(TrainingRefusal.AT_LIMIT);
        return List.of(
              new Segment(ConsoleWidgets.READY_COLOR, tally.ready(), getTextAt(RESOURCE_BUNDLE, readyKey)),
              new Segment(ConsoleWidgets.SHORT_COLOR, shortXp, getTextAt(RESOURCE_BUNDLE, "skills.legend.short")),
              new Segment(ConsoleWidgets.HELD_COLOR, atLimit, getTextAt(RESOURCE_BUNDLE, "skills.legend.limit")),
              new Segment(ConsoleWidgets.BLOCKED_COLOR, tally.cannotCount() - shortXp - atLimit,
                    getTextAt(RESOURCE_BUNDLE, "abilities.legend.blocked")));
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

    private static JComponent attributeCard(TrainingConsole console, List<Person> selection,
          SkillAttribute attribute) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String key = attribute.name();
        boolean isOpen = key.equals(state.openAttribute);
        QuoteTally raise = tally(console, key, selection, person -> new AttributeIncrease(person, attribute));
        List<Segment> segments = segments(raise, "attributes.legend.ready");

        HudButton button = ConsoleWidgets.button(ConsoleWidgets.countedLabel("attributes.raise.plain",
              "attributes.raise", raise.ready()), true, raise.ready() > 0,
              () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.step.raise", attribute.getLabel()),
                    raise.purchases()));
        JComponent action = ConsoleWidgets.actionColumn(button, raise.xpLine(), "", raise.unavailableReason(true));
        CatalogCard card = new CatalogCard(attribute.getLabel(), List.of(), "", new StackedBar(segments), action,
              isOpen, () -> toggleOpen(console, key));
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(attribute.getDescription(), TEXT_MUTED));
        card.addToBody(spread(selection, person -> plan.projectionOf(person).attributeScore(attribute),
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
            int score = plan.projectionOf(person).attributeScore(attribute);
            lowest = Math.min(lowest, score);
            highest = Math.max(highest, score);
            cap = Math.max(cap, TrainingPricer.attributeCap(plan.context(), person, attribute));
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
        picker.addActionListener(event -> {
            if ((picker.getSelectedItem() instanceof Integer score) && (score != target)) {
                state.attributeTargets.put(attribute.name(), score);
                console.refreshPanel();
            }
        });

        // Priced against what is already staged, without staging anything
        Climb climb = console.cached("attributes.climb." + attribute.name() + '.' + target,
              () -> plan.previewClimb(selection, person -> new AttributeIncrease(person, attribute),
                    projection -> projection.attributeScore(attribute), target));

        JPanel box = ConsoleWidgets.previewBox();
        box.add(ConsoleWidgets.labeledRow(getTextAt(RESOURCE_BUNDLE, "attributes.raiseTo"), picker));
        box.add(Box.createVerticalStrut(scaleForGUI(6)));
        box.add(leftAligned(ConsoleWidgets.bold(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raiseTo.preview",
              climb.people(), climb.steps(), climb.xp()), 0.95f, ACCENT_BRIGHT)));
        if (climb.stopShort() > 0) {
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raiseTo.short",
                  climb.stopShort(), target), 0.8f, TEXT_FAINT)));
        }
        box.add(ConsoleWidgets.buttonRow(ConsoleWidgets.button(getFormattedTextAt(RESOURCE_BUNDLE,
              "attributes.raiseTo.stage", target), true, !climb.purchases().isEmpty(),
              () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.step.raiseTo",
                    attribute.getLabel(), target), climb.purchases()))));
        return box;
    }

    // endregion Attributes

    // region Traits and Edge

    private static JComponent traitCard(TrainingConsole console, List<Person> selection, ATOWTraits trait) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String key = trait.name();
        boolean isOpen = key.equals(state.openAttribute);
        QuoteTally raise = tally(console, key + ".up", selection, person -> new TraitChange(person, trait, 1));
        // Connections can only be bought, never sold back
        QuoteTally lower = (trait == ATOWTraits.CONNECTIONS) ? null :
                                 tally(console, key + ".down", selection, person -> new TraitChange(person, trait, -1));

        JPanel buttons = ConsoleWidgets.column();
        buttons.add(traitButton(console, trait, 1, raise));
        if (lower != null) {
            buttons.add(Box.createVerticalStrut(scaleForGUI(4)));
            buttons.add(traitButton(console, trait, -1, lower));
        }
        CatalogCard card = new CatalogCard(trait.getDisplayName(), List.of(), "",
              new StackedBar(segments(raise, "traits.legend.raise")), buttons, isOpen,
              () -> toggleOpen(console, key));
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(trait.getDescription(), TEXT_MUTED));
        card.addToBody(spread(selection, person -> plan.projectionOf(person).traitLevel(trait),
              getTextAt(RESOURCE_BUNDLE, "traits.spread")));
        card.addToBody(directionSection(console, raise, getTextAt(RESOURCE_BUNDLE, "traits.direction.raise"),
              "traits.legend.raise", "traits.line.canRaise"));
        if (lower != null) {
            card.addToBody(directionSection(console, lower, getTextAt(RESOURCE_BUNDLE, "traits.direction.lower"),
                  "traits.legend.lower", "traits.line.canLower"));
        }
        return card;
    }

    private static JComponent traitButton(TrainingConsole console, ATOWTraits trait, int delta, QuoteTally tally) {
        String key = (delta > 0) ? "traits.raise" : "traits.lower";
        HudButton button = ConsoleWidgets.button(ConsoleWidgets.countedLabel(key + ".plain", key, tally.ready()), true,
              tally.ready() > 0, () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                    (delta > 0) ? "traits.step.raise" : "traits.step.lower", trait.getDisplayName()),
                    tally.purchases()));
        button.setToolTipText(getTextAt(RESOURCE_BUNDLE, TrainingPricer.isCostlyDirection(trait, delta) ?
                                                               "traits.costs.toolTipText" :
                                                               "traits.gives.toolTipText"));
        return ConsoleWidgets.actionColumn(button, tally.xpLine(), "", tally.unavailableReason(true));
    }

    /** One direction of a trait: its own bar, cost, and who can and can't move that way. */
    private static JComponent directionSection(TrainingConsole console, QuoteTally tally, String heading,
          String readyKey, String canKey) {
        JPanel section = ConsoleWidgets.column();
        section.add(leftAligned(Hud.eyebrow(heading)));
        section.add(Box.createVerticalStrut(scaleForGUI(4)));
        List<Segment> segments = segments(tally, readyKey);
        section.add(new StackedBar(segments));
        section.add(Box.createVerticalStrut(scaleForGUI(4)));
        section.add(ConsoleWidgets.legend(segments));
        if (tally.hasCostText()) {
            section.add(leftAligned(ConsoleWidgets.label(costText(tally), 0.84f, TEXT_MUTED)));
        }
        if (tally.ready() > 0) {
            section.add(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, canKey, tally.ready()),
                  getTextAt(RESOURCE_BUNDLE, "link.toggleThem"), () -> console.toggle(tally.readyPeople())));
        }
        ConsoleWidgets.cannotLines(tally.cannot(), console::toggle).forEach(section::add);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private static JComponent edgeCard(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        boolean isOpen = EDGE_CARD.equals(state.openAttribute);
        QuoteTally refill = tally(console, EDGE_CARD, selection, EdgeRefill::new);
        List<Segment> segments = segments(refill, "edge.legend.ready");
        HudButton button = ConsoleWidgets.button(ConsoleWidgets.countedLabel("edge.refill.plain", "edge.refill",
              refill.ready()), true, refill.ready() > 0,
              () -> console.stage(getTextAt(RESOURCE_BUNDLE, "edge.step"), refill.purchases()));
        CatalogCard card = new CatalogCard(getTextAt(RESOURCE_BUNDLE, "edge.title"), List.of(), "",
              new StackedBar(segments), ConsoleWidgets.actionColumn(button, refill.xpLine(), "",
              refill.unavailableReason(true)), isOpen, () -> toggleOpen(console, EDGE_CARD));
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

    private static void addCostLine(CatalogCard card, QuoteTally tally) {
        if (tally.hasCostText()) {
            card.addToBody(ConsoleWidgets.label(costText(tally), 0.84f, TEXT_MUTED));
        }
    }

    private static String costText(QuoteTally tally) {
        return tally.costText("attributes.cost.single", "attributes.cost.range");
    }

    private static void addCanLines(TrainingConsole console, CatalogCard card, QuoteTally tally, String canKey) {
        if (tally.ready() > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, canKey, tally.ready()),
                  getTextAt(RESOURCE_BUNDLE, "link.toggleThem"), () -> console.toggle(tally.readyPeople())));
        }
        ConsoleWidgets.cannotLines(tally.cannot(), console::toggle).forEach(card::addToBody);
    }

    /** A small chart of how many of the selection hold each value, from the lowest held to the highest. */
    private static JComponent spread(List<Person> selection, ToIntFunction<Person> value, String heading) {
        Map<Integer, Integer> counts = new HashMap<>();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (Person person : selection) {
            int held = value.applyAsInt(person);
            counts.merge(held, 1, Integer::sum);
            lowest = Math.min(lowest, held);
            highest = Math.max(highest, held);
        }
        if (counts.isEmpty()) {
            lowest = 0;
            highest = 0;
        }
        return ConsoleWidgets.spreadChart(heading, lowest, highest, held -> counts.getOrDefault(held, 0), null, null);
    }

    private static boolean matches(String label, String query) {
        return query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query);
    }

    // endregion Shared parts
}
