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
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.training.PersonProjection;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPlan.StagedPurchase;
import mekhq.campaign.personnel.training.TrainingPlan.Step;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.dialog.trainingConsole.TrainingRoster.PersonEntry;
import mekhq.utilities.spaUtilities.SpaUtilities;

/**
 * One person's record, shown above the purchases when the player clicks a name: their XP, each skill with the exact
 * price of its next level, their abilities, and what is staged for them. A skill's next level can be staged from
 * here, on the same plan as everything else.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PersonDetail {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;

    private PersonDetail() {
    }

    static JComponent build(TrainingConsole console, PersonEntry entry) {
        Person person = entry.person();
        TrainingPlan plan = console.plan();
        PersonProjection projection = plan.projectionOf(person);

        JPanel card = ConsoleWidgets.column();
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10), scaleForGUI(12))));

        JPanel head = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        head.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "person.eyebrow")), BorderLayout.WEST);
        head.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "person.close"), () -> console.focus(null)), BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(head);

        JLabel name = new JLabel(person.getFullTitle());
        name.setFont(hudFont(Font.BOLD, 1.1f, 0.04f));
        name.setForeground(ACCENT_BRIGHT);
        card.add(leftAligned(name));
        card.add(leftAligned(Hud.hint(entry.formationName().isEmpty() ? entry.roleName() :
                                            entry.roleName() + "  ·  " + entry.formationName())));
        card.add(Box.createVerticalStrut(scaleForGUI(6)));
        card.add(ConsoleWidgets.pair(getTextAt(RESOURCE_BUNDLE, "person.xp"),
              getFormattedTextAt(RESOURCE_BUNDLE, "person.xp.value", projection.availableXp(), person.getXP()),
              TEXT, true));
        card.add(ConsoleWidgets.pair(getTextAt(RESOURCE_BUNDLE, "person.level"), entry.skillLevel().toString(),
              ConsoleWidgets.levelColor(entry.skillLevel()), false));

        JPanel links = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(14), scaleForGUI(4)));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "person.selectOnly"), () -> console.select(List.of(person))));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, person.isQuickTrainIgnore() ? "person.includeAuto" :
                                                             "person.excludeAuto"), () -> {
            person.setQuickTrainIgnore(!person.isQuickTrainIgnore());
            console.refresh();
        }));
        links.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(links);

        card.add(Box.createVerticalStrut(scaleForGUI(6)));
        card.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "person.skills"))));
        List<String> skillNames = new ArrayList<>();
        for (Skill skill : projection.projectedSkills().getSkills()) {
            skillNames.add(skill.getType().getName());
        }
        skillNames.sort((first, second) -> {
            boolean firstRole = plan.context().roleSkillsOf(person).contains(first);
            boolean secondRole = plan.context().roleSkillsOf(person).contains(second);
            return (firstRole == secondRole) ? first.compareTo(second) : (firstRole ? -1 : 1);
        });
        for (String skillName : skillNames) {
            card.add(skillLine(console, person, projection, skillName));
        }

        card.add(Box.createVerticalStrut(scaleForGUI(6)));
        card.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "person.abilities"))));
        List<String> abilities = new ArrayList<>();
        for (SpecialAbility ability : SpecialAbility.getSpecialAbilities().values()) {
            if ((ability != null) && projection.hasAbility(ability.getName())) {
                String label = ability.getDisplayName();
                if (SpaUtilities.isFlaw(ability)) {
                    label = getFormattedTextAt(RESOURCE_BUNDLE, "person.flaw", label);
                }
                if (projection.isAbilityGained(ability.getName())) {
                    label = getFormattedTextAt(RESOURCE_BUNDLE, "person.staged", label);
                }
                abilities.add(label);
            }
        }
        abilities.sort(String::compareTo);
        card.add(leftAligned(ConsoleWidgets.wrapped(abilities.isEmpty() ?
                                                          getTextAt(RESOURCE_BUNDLE, "person.abilities.none") :
                                                          String.join(", ", abilities), TEXT_MUTED)));

        List<StagedPurchase> mine = new ArrayList<>();
        for (Step step : plan.steps()) {
            for (StagedPurchase staged : step.purchases()) {
                if (staged.purchase().person().equals(person)) {
                    mine.add(staged);
                }
            }
        }
        card.add(Box.createVerticalStrut(scaleForGUI(6)));
        card.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE, "person.stagedFor", mine.size()))));
        if (mine.isEmpty()) {
            card.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "person.nothingStaged"), 0.82f,
                  TEXT_FAINT)));
        }
        for (StagedPurchase staged : mine) {
            card.add(ConsoleWidgets.pair(ReviewPage.describe(staged),
                  getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", staged.quote().xp()), TEXT_MUTED, false));
        }
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private static JComponent skillLine(TrainingConsole console, Person person, PersonProjection projection,
          String skillName) {
        int level = projection.skillLevel(skillName);
        SkillType type = SkillType.getType(skillName);
        boolean isChanged = projection.isSkillChanged(skillName);
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.setBorder(BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER));
        String levelName = (type == null) ? "" : SkillType.getExperienceLevelName(type.getExperienceLevel(level));
        JLabel name = ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "person.skill", skillName, level,
              levelName), 0.84f, isChanged ? ACCENT : TEXT);
        line.add(name, BorderLayout.CENTER);

        SkillImprovement purchase = new SkillImprovement(person, skillName);
        Quote quote = console.plan().quote(purchase);
        String text = quote.fee().isZero() ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "person.raise", quote.xp()) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "person.raise.fee", quote.xp(),
                                  quote.fee().toAmountAndSymbolString());
        HudButton raise = new HudButton(text.toUpperCase(Locale.ROOT), false, true);
        raise.setArmed(quote.isAllowed() && !quote.fee().isGreaterThan(console.plan().fundsLeft()));
        raise.addActionListener(event -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "person.step",
              skillName, person.getFullTitle()), List.of(purchase)));
        if (!quote.isAllowed() && (quote.refusal() != null)) {
            raise.setToolTipText(quote.refusal().getLabel());
            name.setToolTipText(quote.refusal().getLabel());
            name.setForeground(isChanged ? ACCENT : TEXT_MUTED);
        }
        line.add(raise, BorderLayout.EAST);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }
}
