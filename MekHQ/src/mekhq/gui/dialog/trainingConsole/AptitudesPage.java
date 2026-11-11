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

import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.CatalogCard;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.Segment;
import mekhq.gui.dialog.trainingConsole.ConsoleWidgets.StackedBar;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.SkillGroup;

/**
 * The Aptitudes page's catalog: one card per skill that offers a Natural Aptitude, showing how many of the selected
 * people can buy one now, what it costs, and why the rest can't. A Natural Aptitude is bought once per skill, and makes
 * the person's checks with that skill roll with advantage.
 *
 * <p>The page filters skills the same way as the Skills page, with its own search and chip choice.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class AptitudesPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;

    private AptitudesPage() {
    }

    /**
     * What the selection can do with one skill's aptitude.
     *
     * @param name    the skill
     * @param buy     buying the aptitude, for those who know the skill; those who don't are counted as unable
     * @param holders how many of the selection know the skill
     */
    private record AptitudeStats(String name, QuoteTally buy, int holders) {}

    static List<JComponent> build(TrainingConsole console, List<Person> selection) {
        TrainingConsoleState state = console.state();
        TrainingPlan plan = console.plan();
        String query = state.aptitudeQuery.trim().toLowerCase(Locale.ROOT);
        Set<String> roleSkills = console.cached("skills.roleSkills", () -> {
            Set<String> skills = new HashSet<>();
            for (Person person : selection) {
                skills.addAll(plan.context().roleSkillsOf(person));
            }
            return skills;
        });

        List<AptitudeStats> shown = new ArrayList<>();
        for (String skillName : SkillType.getSortedSkillNames()) {
            SkillType type = SkillType.getType(skillName);
            if ((type == null) || !type.isNaturalAptitudePurchasable()
                      || (!query.isEmpty() && !skillName.toLowerCase(Locale.ROOT).contains(query))
                      || !SkillsPage.isInGroup(type, state.aptitudeGroup)) {
                continue;
            }
            if (SkillType.S_ARTILLERY.equals(skillName) && !plan.context().isUseArtillery()) {
                continue;
            }
            // Checked before pricing anything, as most skills are known by no one selected
            if ((state.aptitudeGroup == SkillGroup.HELD) && !SkillsPage.isHeldByAnyone(plan, skillName, selection)) {
                continue;
            }
            shown.add(console.cached("aptitudes.stats." + skillName, () -> statsFor(plan, skillName, selection)));
        }
        if (state.aptitudeGroup == SkillGroup.HELD) {
            // Role skills first, then the skills most of the selection know
            shown.sort(Comparator.<AptitudeStats, Boolean>comparing(stats -> !roleSkills.contains(stats.name()))
                             .thenComparing(stats -> -stats.holders()));
        }

        List<JComponent> cards = new ArrayList<>();
        for (AptitudeStats stats : shown) {
            cards.add(card(console, stats, roleSkills.contains(stats.name())));
        }
        if (cards.isEmpty()) {
            cards.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "aptitudes.none")));
        }
        return cards;
    }

    private static AptitudeStats statsFor(TrainingPlan plan, String skillName, List<Person> selection) {
        QuoteTally buy = new QuoteTally();
        int holders = 0;
        for (Person person : selection) {
            if (plan.projectionOf(person).skill(skillName) == null) {
                // An aptitude is only bought in a skill the person knows
                buy.addCannot(TrainingRefusal.NOT_HELD, person);
                continue;
            }
            holders++;
            NaturalAptitude purchase = new NaturalAptitude(person, skillName);
            buy.add(purchase, plan.quote(purchase));
        }
        return new AptitudeStats(skillName, buy, holders);
    }

    private static JComponent card(TrainingConsole console, AptitudeStats stats, boolean isRoleSkill) {
        TrainingConsoleState state = console.state();
        boolean isOpen = stats.name().equals(state.openAptitude);
        QuoteTally buy = stats.buy();
        SkillType type = SkillType.getType(stats.name());

        // As on the Skills page: the skill's linked attributes, by short name, then whether it is a role skill
        List<JLabel> tags = new ArrayList<>();
        if (type != null) {
            for (SkillAttribute attribute : List.of(type.getFirstAttribute(), type.getSecondAttribute())) {
                if ((attribute != null) && !attribute.isNoAttribute()) {
                    tags.add(ConsoleWidgets.tag(attribute.getShortName(), TEXT_MUTED));
                }
            }
        }
        if (isRoleSkill) {
            tags.add(ConsoleWidgets.tag(getTextAt(RESOURCE_BUNDLE, "skills.tag.role"), ACCENT));
        }

        int shortXp = buy.count(TrainingRefusal.NOT_ENOUGH_XP);
        int held = buy.count(TrainingRefusal.ALREADY_HELD);
        List<Segment> segments = List.of(
              new Segment(ConsoleWidgets.READY_COLOR, buy.ready(), getTextAt(RESOURCE_BUNDLE,
                    "aptitudes.legend.ready")),
              new Segment(ConsoleWidgets.SHORT_COLOR, shortXp, getTextAt(RESOURCE_BUNDLE, "skills.legend.short")),
              new Segment(ConsoleWidgets.HELD_COLOR, held, getTextAt(RESOURCE_BUNDLE, "aptitudes.legend.held")),
              new Segment(ConsoleWidgets.BLOCKED_COLOR, buy.cannotCount() - shortXp - held,
                    getTextAt(RESOURCE_BUNDLE, "skills.legend.lacking")));

        HudButton button = ConsoleWidgets.button(ConsoleWidgets.countedLabel("aptitudes.buy.plain", "aptitudes.buy",
              buy.ready()), true, buy.ready() > 0, () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE,
              "aptitudes.step", stats.name()), buy.purchases()));
        JComponent action = ConsoleWidgets.actionColumn(button, buy.xpLine(), "", buy.unavailableReason(true));

        CatalogCard card = new CatalogCard(stats.name(), tags, "", new StackedBar(segments), action, isOpen, () -> {
            state.openAptitude = isOpen ? null : stats.name();
            console.refreshPanel();
        });
        if (!isOpen) {
            return card;
        }

        card.addToBody(ConsoleWidgets.wrapped(getTextAt(RESOURCE_BUNDLE, "aptitudes.description"), TEXT_MUTED));
        if ((type != null) && isResourceKeyValid(type.getFlavorText(false, false))
                  && !type.getFlavorText(false, false).isBlank()) {
            card.addToBody(ConsoleWidgets.wrapped(type.getFlavorText(false, true), TEXT_MUTED));
        }
        card.addToBody(ConsoleWidgets.legend(segments));
        if (buy.ready() > 0) {
            card.addToBody(ConsoleWidgets.label(buy.costText("aptitudes.cost.single", "aptitudes.cost.range"), 0.84f,
                  TEXT_MUTED));
            card.addToBody(ConsoleWidgets.reasonLine(getFormattedTextAt(RESOURCE_BUNDLE, "aptitudes.line.can",
                  buy.ready()), getTextAt(RESOURCE_BUNDLE, "link.toggleThem"),
                  () -> console.toggle(buy.readyPeople())));
        }
        ConsoleWidgets.cannotLines(buy.cannot(), console::toggle).forEach(card::addToBody);
        return card;
    }
}
