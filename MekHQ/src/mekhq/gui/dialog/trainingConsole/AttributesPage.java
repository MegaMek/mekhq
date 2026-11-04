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
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.PersonProjection;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPricer;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.AttributeGroup;

/**
 * The Attributes &amp; Traits page: raising attributes, moving traits up or down a step, and refilling spent Edge,
 * each for the whole selection at once.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class AttributesPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;

    private AttributesPage() {
    }

    /** How many of the selection can make a purchase, and what it costs them. */
    private record Tally(int ready, int xp, List<TrainingPurchase> purchases) {}

    private static Tally tally(TrainingPlan plan, List<Person> selection,
          Function<Person, TrainingPurchase> purchaseFor) {
        int ready = 0;
        int xp = 0;
        List<TrainingPurchase> purchases = new ArrayList<>();
        for (Person person : selection) {
            TrainingPurchase purchase = purchaseFor.apply(person);
            Quote quote = plan.quote(purchase);
            if (quote.isAllowed()) {
                ready++;
                xp += quote.xp();
                purchases.add(purchase);
            }
        }
        return new Tally(ready, xp, purchases);
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
            ToIntFunction<Person> current = person -> {
                PersonProjection projection = plan.projectionOf(person);
                int base = (attribute == SkillAttribute.EDGE) ? person.getEdge() :
                                 person.getAttributeScore(attribute);
                return base + projection.attributeChange(attribute);
            };
            Tally raise = tally(plan, selection, person -> new AttributeIncrease(person, attribute));
            HudButton button = button(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.raise", raise.ready()),
                  raise.ready() > 0, () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "attributes.step.raise",
                        attribute.getLabel()), raise.purchases()));
            cards.add(new CatalogCard(attribute.getLabel(), List.of(), range(selection, current), null,
                  ConsoleWidgets.actionColumn(button, xpLine(raise.xp(), raise.ready()), ""), false, null));
        }

        for (ATOWTraits trait : ATOWTraits.values()) {
            if ((group != AttributeGroup.TRAITS) || !TrainingPricer.isTrainableTrait(trait)
                      || !matches(trait.getDisplayName(), query)) {
                continue;
            }
            ToIntFunction<Person> current = person -> TrainingPricer.traitLevel(person, trait)
                                                            + plan.projectionOf(person).traitChange(trait);
            JPanel buttons = ConsoleWidgets.column();
            for (int delta : new int[] { 1, -1 }) {
                if ((trait == ATOWTraits.CONNECTIONS) && (delta < 0)) {
                    continue;
                }
                Tally step = tally(plan, selection, person -> new TraitChange(person, trait, delta));
                boolean isCostly = TrainingPricer.isCostlyDirection(trait, delta);
                HudButton button = button(getFormattedTextAt(RESOURCE_BUNDLE,
                            (delta > 0) ? "traits.raise" : "traits.lower", step.ready()), step.ready() > 0,
                      () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
                            (delta > 0) ? "traits.step.raise" : "traits.step.lower", trait.getDisplayName()),
                            step.purchases()));
                button.setToolTipText(getTextAt(RESOURCE_BUNDLE, isCostly ? "traits.costs.toolTipText" :
                                                                       "traits.gives.toolTipText"));
                buttons.add(ConsoleWidgets.actionColumn(button, xpLine(step.xp(), step.ready()), ""));
                buttons.add(Box.createVerticalStrut(scaleForGUI(4)));
            }
            cards.add(new CatalogCard(trait.getDisplayName(), List.of(), range(selection, current), null, buttons,
                  false, null));
        }

        if ((group == AttributeGroup.EDGE) && matches(getTextAt(RESOURCE_BUNDLE, "edge.title"), query)) {
            Tally refill = tally(plan, selection, EdgeRefill::new);
            long spent = selection.stream().filter(person -> person.getUsedEdge() > 0).count();
            HudButton button = button(getFormattedTextAt(RESOURCE_BUNDLE, "edge.refill", refill.ready()),
                  refill.ready() > 0, () -> console.stage(getTextAt(RESOURCE_BUNDLE, "edge.step"),
                        refill.purchases()));
            cards.add(new CatalogCard(getTextAt(RESOURCE_BUNDLE, "edge.title"), List.of(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "edge.meta", spent), null,
                  ConsoleWidgets.actionColumn(button, xpLine(refill.xp(), refill.ready()), ""), false, null));
        }

        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "attributes.none")));
        }
        JLabel note = ConsoleWidgets.wrapped(getTextAt(RESOURCE_BUNDLE, "attributes.note"), TEXT_FAINT);
        note.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(4), 0, 0));
        cards.add(note);
        return cards;
    }

    private static boolean matches(String label, String query) {
        return query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query);
    }

    private static HudButton button(String text, boolean isArmed, Runnable onClick) {
        HudButton button = new HudButton(text.toUpperCase(Locale.ROOT), false, true);
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

    /** @return the lowest, highest and average value across the selection, such as "4–7 · avg 5.2" */
    private static String range(List<Person> selection, ToIntFunction<Person> value) {
        if (selection.isEmpty()) {
            return "";
        }
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        long sum = 0;
        for (Person person : selection) {
            int current = value.applyAsInt(person);
            lowest = Math.min(lowest, current);
            highest = Math.max(highest, current);
            sum += current;
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "attributes.range", lowest, highest,
              String.format(Locale.ROOT, "%.1f", (double) sum / selection.size()));
    }
}
