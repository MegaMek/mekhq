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
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.training.AbilityChoices;
import mekhq.campaign.personnel.training.PersonProjection;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.AbilityPurchase;
import mekhq.campaign.personnel.training.TrainingPurchase.FlawBuyOff;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.Segment;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.StackedBar;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.AbilityGroup;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * The Abilities page's catalog: one card per special ability in the chosen group. Each card says how many of the
 * selected people can gain the ability now, how many are short on XP, how many already have it, and why the rest
 * can't, with a link to select each group. An ability that needs a choice, such as Weapon Specialist, can take each
 * person's own weapon, so a whole company can gain it in one go.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class AbilitiesPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    private static final int META_LENGTH = 110;

    private AbilitiesPage() {
    }

    /** What the selection can do with one ability. */
    private static final class AbilityStats {
        int ready;
        int shortXp;
        int held;
        int xp;
        int minXp = Integer.MAX_VALUE;
        int maxXp = Integer.MIN_VALUE;
        Money fees = Money.zero();
        final List<Person> readyPeople = new ArrayList<>();
        final List<Person> shortPeople = new ArrayList<>();
        final List<Person> heldPeople = new ArrayList<>();
        final Map<TrainingRefusal, List<Person>> blocked = new EnumMap<>(TrainingRefusal.class);
        final Map<String, Integer> choices = new LinkedHashMap<>();

        int total() {
            return ready + shortXp + held + blocked.values().stream().mapToInt(List::size).sum();
        }
    }

    static List<JComponent> build(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        if (!console.plan().context().isUseAbilities()) {
            return List.of(Hud.notice(getTextAt(RESOURCE_BUNDLE, "abilities.disabled")));
        }
        String query = state.abilityQuery.trim().toLowerCase(Locale.ROOT);
        List<SpecialAbility> abilities = new ArrayList<>(SpecialAbility.getSpecialAbilities().values());
        abilities.sort(Comparator.comparing(SpecialAbility::getDisplayName));

        List<JComponent> cards = new ArrayList<>();
        for (SpecialAbility ability : abilities) {
            if ((ability == null) || !isInGroup(console, ability, state.abilityGroup)
                      || (!query.isEmpty() && !ability.getDisplayName().toLowerCase(Locale.ROOT).contains(query))) {
                continue;
            }
            AbilityStats stats = statsFor(console, ability, selection);
            if ((state.abilityGroup == AbilityGroup.BUY_OFF) && (stats.total() == 0)) {
                // Only flaws someone selected has can be bought off
                continue;
            }
            cards.add(card(console, ability, stats, selection));
        }
        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, (state.abilityGroup == AbilityGroup.BUY_OFF) ?
                                                                  "abilities.none.buyOff" : "abilities.none")));
        }
        return cards;
    }

    private static boolean isInGroup(TrainingConsole console, SpecialAbility ability, AbilityGroup group) {
        AbilityCategory category = console.plan().context().categoryOf(ability);
        return switch (group) {
            case COMBAT -> category == AbilityCategory.COMBAT_ABILITY;
            case MANEUVERING -> category == AbilityCategory.MANEUVERING_ABILITY;
            case UTILITY -> category == AbilityCategory.UTILITY_ABILITY;
            case CHARACTER -> category == AbilityCategory.CHARACTER_CREATION_ONLY;
            case FLAWS, BUY_OFF -> category == AbilityCategory.CHARACTER_FLAW;
        };
    }

    /**
     * @return the value the player chose for everyone, {@link TrainingConsoleState#CHOOSE_FOR_EACH}, or an empty
     *       string if they haven't chosen yet
     */
    private static String choiceSetting(TrainingConsoleState state, SpecialAbility ability) {
        String chosen = state.abilityChoices.get(ability.getName());
        if (chosen != null) {
            return chosen;
        }
        return AbilityChoices.canChooseForEachPerson(ability) ? TrainingConsoleState.CHOOSE_FOR_EACH : "";
    }

    private static TrainingPurchase purchaseFor(TrainingConsole console, SpecialAbility ability, Person person) {
        if (console.state().abilityGroup == AbilityGroup.BUY_OFF) {
            return new FlawBuyOff(person, ability.getName());
        }
        String choice = null;
        if (AbilityChoices.needsChoice(ability)) {
            String setting = choiceSetting(console.state(), ability);
            choice = TrainingConsoleState.CHOOSE_FOR_EACH.equals(setting) ?
                           AbilityChoices.choiceFor(ability, person) :
                           (setting.isEmpty() ? null : setting);
        }
        return new AbilityPurchase(person, ability.getName(), choice);
    }

    private static AbilityStats statsFor(TrainingConsole console, SpecialAbility ability, List<Person> selection) {
        AbilityStats stats = new AbilityStats();
        TrainingPlan plan = console.plan();
        boolean isBuyOff = console.state().abilityGroup == AbilityGroup.BUY_OFF;
        for (Person person : selection) {
            TrainingPurchase purchase = purchaseFor(console, ability, person);
            Quote quote = plan.quote(purchase);
            if (quote.isAllowed()) {
                stats.ready++;
                stats.readyPeople.add(person);
                stats.xp += quote.xp();
                stats.minXp = Math.min(stats.minXp, quote.xp());
                stats.maxXp = Math.max(stats.maxXp, quote.xp());
                stats.fees = stats.fees.plus(quote.fee());
                if ((purchase instanceof AbilityPurchase abilityPurchase) && (abilityPurchase.choice() != null)) {
                    stats.choices.merge(abilityPurchase.choice(), 1, Integer::sum);
                }
                continue;
            }
            TrainingRefusal refusal = quote.refusal();
            if (isBuyOff && (refusal == TrainingRefusal.NOT_HELD)) {
                continue;
            }
            if (refusal == TrainingRefusal.NOT_ENOUGH_XP) {
                stats.shortXp++;
                stats.shortPeople.add(person);
            } else if (refusal == TrainingRefusal.ALREADY_HELD) {
                stats.held++;
                stats.heldPeople.add(person);
            } else {
                stats.blocked.computeIfAbsent(refusal, key -> new ArrayList<>()).add(person);
            }
        }
        return stats;
    }

    private static JComponent card(TrainingConsole console, SpecialAbility ability, AbilityStats stats,
          List<Person> selection) {
        TrainingConsoleState state = console.state();
        boolean isOpen = ability.getName().equals(state.openAbility);
        AbilityGroup group = state.abilityGroup;
        boolean isBuyOff = group == AbilityGroup.BUY_OFF;
        boolean isTakingFlaw = group == AbilityGroup.FLAWS;

        List<JLabel> tags = new ArrayList<>();
        int baseCost = Math.abs(ability.getCost());
        tags.add(ConsoleWidgets.tag(getFormattedTextAt(RESOURCE_BUNDLE, isTakingFlaw ? "abilities.tag.gives" :
                                                                              "abilities.tag.cost", baseCost),
              TEXT_FAINT));
        boolean needsChoice = !isBuyOff && AbilityChoices.needsChoice(ability);
        if (needsChoice) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "abilities.tag.choice"), ACCENT));
        }
        if (group == AbilityGroup.CHARACTER) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "abilities.tag.gm"), AMBER));
        }
        if (!stats.fees.isZero()) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.fee"), AMBER));
        }

        int blockedCount = stats.blocked.values().stream().mapToInt(List::size).sum();
        List<Segment> segments = List.of(
              new Segment(ConsoleWidgets.READY_COLOR, stats.ready, getTextAt(RESOURCE_BUNDLE,
                    isBuyOff ? "abilities.legend.buyOff" : "abilities.legend.ready")),
              new Segment(ConsoleWidgets.SHORT_COLOR, stats.shortXp, getTextAt(RESOURCE_BUNDLE,
                    "skills.legend.short")),
              new Segment(ConsoleWidgets.HELD_COLOR, stats.held, getTextAt(RESOURCE_BUNDLE,
                    "abilities.legend.held")),
              new Segment(ConsoleWidgets.BLOCKED_COLOR, blockedCount, getTextAt(RESOURCE_BUNDLE,
                    "abilities.legend.blocked")));

        String verbKey = isBuyOff ? "abilities.buyOff" : (isTakingFlaw ? "abilities.take" : "abilities.grant");
        HudButton grant = new HudButton(ConsoleWidgets.countedLabel(verbKey + ".plain", verbKey, stats.ready)
                                              .toUpperCase(Locale.ROOT), true, true);
        boolean isAffordable = !stats.fees.isGreaterThan(console.plan().fundsLeft());
        boolean canChainPrerequisites = state.isIncludePrerequisites
                                              && stats.blocked.containsKey(TrainingRefusal.NOT_ELIGIBLE)
                                              && !ability.getPrereqAbilities().isEmpty();
        grant.setArmed(((stats.ready > 0) || canChainPrerequisites) && isAffordable);
        grant.setToolTipText(isAffordable ? null : getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford"));
        grant.addActionListener(event -> stageAbility(console, ability, stats, selection));
        String xpLine = "";
        if (stats.ready > 0) {
            xpLine = (stats.xp < 0) ?
                           getFormattedTextAt(RESOURCE_BUNDLE, "xp.gained", -stats.xp) :
                           getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", stats.xp);
        }
        JComponent action = ConsoleWidgets.actionColumn(grant, xpLine, ConsoleWidgets.money(stats.fees),
              grant.isArmed() ? null : unavailableReason(stats, isAffordable));

        CatalogCard card = new CatalogCard(ability.getDisplayName(), tags, shortDescription(ability),
              new StackedBar(segments), action, isOpen, () -> {
                  state.openAbility = isOpen ? null : ability.getName();
                  console.refreshPanel();
              });
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(ability.getDescription(), TEXT_MUTED));
        String prerequisites = ability.getAllPrereqDesc();
        if ((prerequisites != null) && !prerequisites.isBlank()) {
            JPanel requirement = ConsoleWidgets.column();
            requirement.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "abilities.prerequisites"))));
            requirement.add(leftAligned(ConsoleWidgets.wrapped(prerequisites, TEXT_MUTED)));
            card.addToBody(requirement);
        }
        card.addToBody(ConsoleWidgets.legend(segments));
        if (needsChoice) {
            card.addToBody(choicePicker(console, ability, stats, selection));
        }
        if (stats.ready > 0) {
            String cost = (stats.minXp == stats.maxXp) ?
                                getFormattedTextAt(RESOURCE_BUNDLE, "abilities.cost.single", stats.minXp) :
                                getFormattedTextAt(RESOURCE_BUNDLE, "abilities.cost.range", stats.minXp,
                                      stats.maxXp);
            card.addToBody(ConsoleWidgets.label(cost, 0.84f, TEXT_MUTED));
            if (!stats.fees.isZero()) {
                card.addToBody(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "abilities.fees",
                      stats.fees.toAmountAndSymbolString()), 0.84f, AMBER));
            }
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "abilities.reason.narrow",
                  stats.ready), getTextAt(RESOURCE_BUNDLE, "link.selectThem"),
                  () -> console.select(stats.readyPeople)));
        }
        if (stats.shortXp > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "skills.reason.short",
                  stats.shortXp), getTextAt(RESOURCE_BUNDLE, "link.selectThem"),
                  () -> console.select(stats.shortPeople)));
        }
        if (stats.held > 0) {
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "abilities.reason.held",
                  stats.held), getTextAt(RESOURCE_BUNDLE, "link.selectThem"),
                  () -> console.select(stats.heldPeople)));
        }
        for (Map.Entry<TrainingRefusal, List<Person>> entry : stats.blocked.entrySet()) {
            List<Person> people = entry.getValue();
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "abilities.reason.blocked",
                  people.size(), entry.getKey().getLabel()), getTextAt(RESOURCE_BUNDLE, "link.selectThem"),
                  () -> console.select(people)));
        }
        return card;
    }

    /** @return why no one selected can take the ability: the most common reason, with how many it stops */
    private static String unavailableReason(AbilityStats stats, boolean isAffordable) {
        if (!isAffordable) {
            return getTextAt(RESOURCE_BUNDLE, "fees.cannotAfford.short");
        }
        TrainingRefusal top = TrainingRefusal.NOT_ENOUGH_XP;
        int most = stats.shortXp;
        if (stats.held > most) {
            top = TrainingRefusal.ALREADY_HELD;
            most = stats.held;
        }
        for (Map.Entry<TrainingRefusal, List<Person>> entry : stats.blocked.entrySet()) {
            if (entry.getValue().size() > most) {
                top = entry.getKey();
                most = entry.getValue().size();
            }
        }
        return ConsoleWidgets.reason(most, top);
    }

    /** Stages the ability for everyone who can take it, and its missing prerequisites first if the player asked. */
    private static void stageAbility(TrainingConsole console, SpecialAbility ability, AbilityStats stats,
          List<Person> selection) {
        TrainingConsoleState state = console.state();
        List<TrainingPurchase> purchases = new ArrayList<>();
        Set<Person> people = new HashSet<>(stats.readyPeople);
        Set<Person> needingPrerequisites = state.isIncludePrerequisites ?
                                                 new HashSet<>(stats.blocked.getOrDefault(TrainingRefusal.NOT_ELIGIBLE,
                                                       List.of())) :
                                                 Set.of();
        for (Person person : selection) {
            if (people.contains(person)) {
                purchases.add(purchaseFor(console, ability, person));
            } else if (needingPrerequisites.contains(person)) {
                PersonProjection projection = console.plan().projectionOf(person);
                for (String required : ability.getPrereqAbilities()) {
                    if (!projection.hasAbility(required)) {
                        purchases.add(new AbilityPurchase(person, required, null));
                    }
                }
                purchases.add(purchaseFor(console, ability, person));
            }
        }
        String summary = choiceSummary(state, ability);
        String name = (summary == null) ? ability.getDisplayName() :
                            getFormattedTextAt(RESOURCE_BUNDLE, "abilities.step.withChoice", ability.getDisplayName(),
                                  summary);
        String key = switch (state.abilityGroup) {
            case BUY_OFF -> "abilities.step.buyOff";
            case FLAWS -> "abilities.step.take";
            default -> "abilities.step.grant";
        };
        console.stage(getFormattedTextAt(RESOURCE_BUNDLE, key, name), purchases);
    }

    private static @Nullable String choiceSummary(TrainingConsoleState state, SpecialAbility ability) {
        if (!AbilityChoices.needsChoice(ability) || (state.abilityGroup == AbilityGroup.BUY_OFF)) {
            return null;
        }
        String setting = choiceSetting(state, ability);
        return TrainingConsoleState.CHOOSE_FOR_EACH.equals(setting) ?
                     getTextAt(RESOURCE_BUNDLE, "abilities.choice.each.short") :
                     setting;
    }

    private static JComponent choicePicker(TrainingConsole console, SpecialAbility ability, AbilityStats stats,
          List<Person> selection) {
        TrainingConsoleState state = console.state();
        String current = choiceSetting(state, ability);
        JComboBox<String> picker = new JComboBox<>();
        picker.addItem(AbilityChoices.canChooseForEachPerson(ability) ? TrainingConsoleState.CHOOSE_FOR_EACH : "");
        for (String choice : AbilityChoices.sharedChoices(ability, selection)) {
            picker.addItem(choice);
        }
        picker.setSelectedItem(current);
        Hud.styleComboBox(picker);
        Map<String, String> labels = new HashMap<>();
        labels.put(TrainingConsoleState.CHOOSE_FOR_EACH, getTextAt(RESOURCE_BUNDLE, "abilities.choice.each"));
        labels.put("", getTextAt(RESOURCE_BUNDLE, "abilities.choice.pick"));
        picker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = Hud.comboRenderer().getListCellRendererComponent(list, value, index,
                      isSelected, cellHasFocus);
                if ((component instanceof JLabel label) && (value instanceof String text)) {
                    label.setText(labels.getOrDefault(text, getFormattedTextAt(RESOURCE_BUNDLE,
                          "abilities.choice.everyone", text)));
                }
                return component;
            }
        });
        picker.addActionListener(event -> {
            if ((picker.getSelectedItem() instanceof String chosen) && !chosen.equals(current)) {
                state.abilityChoices.put(ability.getName(), chosen);
                console.refreshPanel();
            }
        });

        JPanel holder = ConsoleWidgets.column();
        holder.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "abilities.choice"))));
        JPanel pickerRow = Hud.transparentPanel(new BorderLayout());
        pickerRow.add(picker, BorderLayout.CENTER);
        pickerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.add(pickerRow);
        if (stats.choices.size() > 1) {
            String summary = stats.choices.entrySet().stream()
                                   .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                   .limit(5)
                                   .map(entry -> getFormattedTextAt(RESOURCE_BUNDLE, "abilities.choice.count",
                                         entry.getKey(), entry.getValue()))
                                   .collect(Collectors.joining(", "));
            JLabel resolved = ConsoleWidgets.wrapped(getFormattedTextAt(RESOURCE_BUNDLE,
                  "abilities.choice.resolves", summary), TEXT_FAINT);
            resolved.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, 0, 0));
            holder.add(leftAligned(resolved));
        }
        return holder;
    }

    /** @return the ability's description as one short line, without its markup */
    private static String shortDescription(SpecialAbility ability) {
        String description = ability.getDescription();
        if (description == null) {
            return "";
        }
        String plain = description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return (plain.length() <= META_LENGTH) ? plain : plain.substring(0, META_LENGTH - 1).trim() + '…';
    }
}
