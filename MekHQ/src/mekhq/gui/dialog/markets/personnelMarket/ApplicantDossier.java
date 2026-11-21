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
package mekhq.gui.dialog.markets.personnelMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.RecruitmentCosts.SALARY_MONTHS;
import static mekhq.campaign.personnel.enums.GenderDescriptors.MALE_FEMALE_OTHER;
import static mekhq.gui.baseComponents.hud.Hud.addRow;
import static mekhq.gui.baseComponents.hud.Hud.addSpanning;
import static mekhq.gui.baseComponents.hud.Hud.column;
import static mekhq.gui.baseComponents.hud.Hud.escape;
import static mekhq.gui.baseComponents.hud.Hud.section;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.records.HiringCostBreakdown;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.RecruitmentCosts.TrainingCostBreakdown;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudWrappingLabel;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;
import mekhq.gui.utilities.WrapLayout;

/**
 * The dossier on the right of the personnel market: everything the player weighs before hiring the highlighted
 * applicant. A header with their portrait, name, profession and the quick facts (experience, age, gender, origin);
 * then what they cost to hire and how that cost is made up, their skills, their special abilities and flaws, their
 * profile, and the recruiter's interview notes. A link opens their full personnel record for anything else.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ApplicantDossier {
    private static final MMLogger LOGGER = MMLogger.create(ApplicantDossier.class);

    private static final int PORTRAIT_SIZE = 84;

    /** A special ability or flaw, as the dossier shows it. */
    record Ability(String name, String description, boolean isFlaw) {}

    private ApplicantDossier() {
    }

    /**
     * Builds the dossier for an applicant.
     *
     * @param market the market window, for the campaign and the applicant's cached facts
     * @param person the applicant
     *
     * @return the dossier, in a scroll pane that fills the dossier column
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JComponent build(PersonnelMarketDialog market, Person person) {
        JPanel detail = column();
        detail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(16), scaleForGUI(20), scaleForGUI(20),
              scaleForGUI(20)));

        detail.add(leftAligned(buildHeader(market, person)));
        detail.add(Box.createVerticalStrut(scaleForGUI(18)));
        detail.add(leftAligned(buildCostSection(market, person)));

        List<Skill> skills = new ArrayList<>(person.getSkills().getSkills());
        skills.sort(Comparator.comparing(skill -> skill.getType().getName(), String.CASE_INSENSITIVE_ORDER));
        List<Skill> trainedSkills = new ArrayList<>();
        List<Skill> roleplaySkills = new ArrayList<>();
        for (Skill skill : skills) {
            if (skill.getType().isRoleplaySkill()) {
                roleplaySkills.add(skill);
            } else {
                trainedSkills.add(skill);
            }
        }
        detail.add(Box.createVerticalStrut(scaleForGUI(18)));
        detail.add(leftAligned(buildSkillSection(person, text("dossier.personnelMarket.skills"), trainedSkills)));
        if (!roleplaySkills.isEmpty()) {
            detail.add(Box.createVerticalStrut(scaleForGUI(18)));
            detail.add(leftAligned(buildSkillSection(person, text("dossier.personnelMarket.roleplaySkills"),
                  roleplaySkills)));
        }

        detail.add(Box.createVerticalStrut(scaleForGUI(18)));
        detail.add(leftAligned(buildAbilitySection(person)));
        detail.add(Box.createVerticalStrut(scaleForGUI(18)));
        detail.add(leftAligned(buildProfileSection(market, person)));

        String notes = interviewNotes(market.campaign(), person);
        if (notes != null) {
            detail.add(Box.createVerticalStrut(scaleForGUI(18)));
            detail.add(leftAligned(Hud.sectionHeading(text("dossier.personnelMarket.interview"))));
            detail.add(Box.createVerticalStrut(scaleForGUI(6)));
            HudWrappingLabel notesLabel = new HudWrappingLabel("<html>" + notes + "</html>");
            notesLabel.setForeground(TEXT_MUTED);
            notesLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            detail.add(leftAligned(notesLabel));
        }

        detail.add(Box.createVerticalStrut(scaleForGUI(18)));
        detail.add(leftAligned(Hud.link(text("dossier.personnelMarket.fullRecord"),
              () -> market.showFullRecord(person))));
        return Hud.scroll(detail, GROUND);
    }

    /**
     * Builds the notice the dossier column shows when no applicant is highlighted.
     *
     * @param titleKey resource key of the notice's title
     * @param bodyKey  resource key of the notice's body
     *
     * @return the notice, pinned to the top of the column
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JComponent buildEmptyState(String titleKey, String bodyKey) {
        JPanel text = column();
        JLabel title = new JLabel(text(titleKey).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 0.95f, 0.12f));
        text.add(leftAligned(title));
        text.add(Box.createVerticalStrut(scaleForGUI(6)));
        HudWrappingLabel body = new HudWrappingLabel("<html>" + escape(text(bodyKey)) + "</html>");
        body.setForeground(TEXT_MUTED);
        body.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        text.add(leftAligned(body));

        JPanel notice = Hud.framedSurface(18);
        notice.add(text, BorderLayout.CENTER);

        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(20), scaleForGUI(20), scaleForGUI(20),
              scaleForGUI(20)));
        holder.add(notice, BorderLayout.NORTH);
        return holder;
    }

    // region Header

    /** Portrait beside the name, profession and quick facts. */
    private static JPanel buildHeader(PersonnelMarketDialog market, Person person) {
        Campaign campaign = market.campaign();
        LocalDate today = campaign.getLocalDate();
        ApplicantFacts facts = market.facts(person);

        JPanel header = Hud.transparentPanel(new BorderLayout(scaleForGUI(16), 0));
        header.add(Hud.topAligned(PersonnelMarketHud.portrait(person, scaleForGUI(PORTRAIT_SIZE))),
              BorderLayout.WEST);

        JPanel text = column();
        JLabel name = new JLabel(person.getFullTitle());
        name.setForeground(TEXT);
        name.setFont(hudFont(Font.BOLD, 1.25f, 0.0f));
        text.add(leftAligned(name));
        JLabel role = new JLabel(roles(person));
        role.setForeground(ACCENT);
        role.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        text.add(leftAligned(role));
        text.add(Box.createVerticalStrut(scaleForGUI(8)));

        JPanel factRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(4)));
        factRow.add(PersonnelMarketHud.fact(SkillType.getExperienceLevelName(facts.experienceLevel()),
              experienceColor(facts.experienceLevel())));
        factRow.add(PersonnelMarketHud.fact(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.personnelMarket.age",
              person.getAge(today)), TEXT_MUTED));
        factRow.add(PersonnelMarketHud.fact(MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender()),
              TEXT_MUTED));
        Faction origin = person.getOriginFaction();
        if (origin != null) {
            factRow.add(PersonnelMarketHud.fact(origin.getFullName(today.getYear()), TEXT_MUTED));
        }
        if (person.isCivilian()) {
            factRow.add(Hud.badge(text("tag.personnelMarket.civilian"), TEXT_FAINT,
                  text("tag.personnelMarket.civilian.tooltip")));
        }
        if (market.isRare(person)) {
            factRow.add(Hud.badge(text("tag.personnelMarket.rare"), AMBER,
                  text("tag.personnelMarket.rare.tooltip")));
        }
        // The facts wrap beside the portrait; the row is offset by the layout's own gap so it lines up with the name.
        factRow.setBorder(BorderFactory.createEmptyBorder(0, -scaleForGUI(6), 0, 0));
        text.add(leftAligned(factRow));

        header.add(text, BorderLayout.CENTER);
        return header;
    }

    /** @return the applicant's primary profession, and their secondary one if they have it */
    static String roles(Person person) {
        String primary = person.getPrimaryRole().toString();
        if (person.getSecondaryRole() == null || person.getSecondaryRole().isNone()) {
            return primary;
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "dossier.personnelMarket.roles", primary,
              person.getSecondaryRole().toString());
    }

    // endregion Header

    // region Sections

    /** What the applicant costs to hire, part by part, against the command's funds. */
    private static JPanel buildCostSection(PersonnelMarketDialog market, Person person) {
        JPanel section = section(text("dossier.personnelMarket.cost"));
        HiringCostBreakdown breakdown = market.market().getHiringCostBreakdown(person);
        if (breakdown != null) {
            if (breakdown.salaryCost().isPositive()) {
                addRow(section, getFormattedTextAt(RESOURCE_BUNDLE, "dossier.personnelMarket.cost.salary",
                      SALARY_MONTHS), breakdown.salaryCost().toAmountAndSymbolString(), null, null, false);
            }
            TrainingCostBreakdown training = breakdown.trainingCost();
            if (training != null) {
                for (SkillTrainingCategory category : SkillTrainingCategory.values()) {
                    Money skillCost = training.skillCosts().getOrDefault(category, Money.zero());
                    if (skillCost.isPositive()) {
                        addRow(section, text("dossier.personnelMarket.cost." + category.name()),
                              skillCost.toAmountAndSymbolString(), null, null, false);
                    }
                }
                if (training.spaCost().isPositive()) {
                    addRow(section, text("dossier.personnelMarket.cost.spa"),
                          training.spaCost().toAmountAndSymbolString(), null, null, false);
                }
            }
            if (breakdown.goldenHelloExtra().isPositive()) {
                addRow(section, text("dossier.personnelMarket.cost.goldenHello"),
                      breakdown.goldenHelloExtra().toAmountAndSymbolString(), AMBER,
                      text("dossier.personnelMarket.cost.goldenHello.tooltip"), false);
            }
        }

        Campaign campaign = market.campaign();
        Money monthlySalary = person.getSalary(campaign.getCampaignOptions(),
              campaign.getPlayerForce().isClanForce(),
              campaign.getLocalDate());
        Money total = market.facts(person).hiringCost();
        boolean isUnaffordable = market.balance().isLessThan(total);
        addRow(section, text("dossier.personnelMarket.cost.total"),
              total.isPositive() ? total.toAmountAndSymbolString() : text("board.personnelMarket.free"),
              isUnaffordable ? DANGER : ACCENT_BRIGHT, null, true);
        addRow(section, text("dossier.personnelMarket.cost.monthly"),
              getFormattedTextAt(RESOURCE_BUNDLE, "dossier.personnelMarket.cost.monthly.value",
                    monthlySalary.toAmountAndSymbolString()), TEXT_MUTED,
              text("dossier.personnelMarket.cost.monthly.tooltip"), false);
        if (isUnaffordable) {
            HudWrappingLabel warning = new HudWrappingLabel("<html>" + escape(getFormattedTextAt(RESOURCE_BUNDLE,
                  "dossier.personnelMarket.cost.unaffordable", market.balance().toAmountAndSymbolString()))
                                                                  + "</html>");
            warning.setForeground(DANGER);
            warning.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            addSpanning(section, warning, 6);
        }
        return section;
    }

    /** One row per skill: its target number, its level and how good that is, coloured by how good. */
    private static JPanel buildSkillSection(Person person, String heading, List<Skill> skills) {
        JPanel section = section(heading);
        if (skills.isEmpty()) {
            addSpanning(section, Hud.hint(text("dossier.personnelMarket.skills.none")), 2);
            return section;
        }
        SkillModifierData modifiers = person.getSkillModifierData();
        for (Skill skill : skills) {
            int target = skill.getFinalSkillValue(modifiers);
            String targetText = skill.isCountUp() ? "+" + target : target + "+";
            SkillLevel level = skill.getSkillLevel(modifiers);
            addRow(section, skill.getType().getName(), getFormattedTextAt(RESOURCE_BUNDLE,
                        "dossier.personnelMarket.skill.value", targetText, skill.getTotalSkillLevel(modifiers),
                        level.toString()), colorOf(SkillType.getExperienceLevelColor(level), TEXT), null, false);
        }
        return section;
    }

    /** Special abilities in green and flaws in red, each explained on hover. */
    private static JPanel buildAbilitySection(Person person) {
        JPanel section = section(text("dossier.personnelMarket.abilities"));
        List<Ability> abilities = abilities(person);
        if (abilities.isEmpty()) {
            addSpanning(section, Hud.hint(text("dossier.personnelMarket.abilities.none")), 2);
            return section;
        }
        JPanel badges = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(5), scaleForGUI(4)));
        // Abilities first, then flaws, so the two read as groups.
        for (boolean flaws : new boolean[] { false, true }) {
            for (Ability ability : abilities) {
                if (ability.isFlaw() == flaws) {
                    badges.add(Hud.badge(ability.name(), flaws ? DANGER : READY,
                          ability.description()));
                }
            }
        }
        // Offset by the layout's own gap, so the first badge lines up with the heading.
        badges.setBorder(BorderFactory.createEmptyBorder(0, -scaleForGUI(5), 0, 0));
        addSpanning(section, badges, 2);
        return section;
    }

    /** Where the applicant comes from and the numbers that matter beyond their skills. */
    private static JPanel buildProfileSection(PersonnelMarketDialog market, Person person) {
        Campaign campaign = market.campaign();
        CampaignOptions options = campaign.getCampaignOptions();
        LocalDate today = campaign.getLocalDate();
        boolean isClanCampaign = campaign.getPlayerForce().isClanForce();

        JPanel section = section(text("dossier.personnelMarket.profile"));
        Faction origin = person.getOriginFaction();
        if (origin != null) {
            addRow(section, text("dossier.personnelMarket.origin"), origin.getFullName(today.getYear()), null, null,
                  false);
        }
        if (market.isReputationShown()) {
            boolean isUsePersonality = options.get(CampaignOption.USE_RANDOM_PERSONALITIES)
                                             && options.get(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION);
            int reputation = person.getAdjustedReputation(options.get(CampaignOption.USE_AGE_EFFECTS),
                  isClanCampaign, today, isUsePersonality);
            addRow(section, text("dossier.personnelMarket.reputation"), signed(reputation),
                  reputation < 0 ? DANGER : (reputation > 0 ? READY : null),
                  text("dossier.personnelMarket.reputation.tooltip"), false);
        }
        addRow(section, text("dossier.personnelMarket.exam"),
              getFormattedTextAt(RESOURCE_BUNDLE, "dossier.personnelMarket.exam.value",
                    person.getPerformanceExamScore()), null, text("dossier.personnelMarket.exam.tooltip"), false);
        if (options.get(CampaignOption.USE_EDGE)) {
            addRow(section, text("dossier.personnelMarket.edge"), Integer.toString(person.getEdge()), null,
                  text("dossier.personnelMarket.edge.tooltip"), false);
        }
        return section;
    }

    // endregion Sections

    // region Helpers

    /**
     * @param person an applicant
     *
     * @return the applicant's special abilities and flaws, in the order the campaign lists them
     */
    static List<Ability> abilities(Person person) {
        List<Ability> abilities = new ArrayList<>();
        for (Enumeration<IOption> options = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES);
              options.hasMoreElements(); ) {
            IOption option = options.nextElement();
            if (!option.booleanValue()) {
                continue;
            }
            SpecialAbility ability = null;
            try {
                ability = SpecialAbility.getAbility(option.getName());
            } catch (Exception exception) {
                LOGGER.debug("Unable to find {} in the campaign's SPAs; this is expected if it is turned off",
                      option.getName());
            }
            String name = ability == null ? option.getDisplayableName() : ability.getDisplayName();
            String description = ability == null ? option.getDescription() : ability.getDescription();
            boolean isFlaw = ability != null && ability.getCost() < 0;
            abilities.add(new Ability(name, description == null ? "" : description, isFlaw));
        }
        return abilities;
    }

    /**
     * The recruiter's notes on the applicant's personality, as the personnel view shows them for recruits: the
     * personality labels alone when the campaign asks for labels only, or for a child.
     *
     * @return the notes as HTML body text, or {@code null} if the campaign hides personalities or there are none
     */
    private static @Nullable String interviewNotes(Campaign campaign, Person person) {
        CampaignOptions options = campaign.getCampaignOptions();
        if (!options.get(CampaignOption.USE_RANDOM_PERSONALITIES) || person.isHidePersonality()) {
            return null;
        }
        boolean isLabelsOnly = options.get(CampaignOption.USE_PERSONALITY_LABELS_ONLY)
                                     || person.isChild(campaign.getLocalDate());
        String notes = isLabelsOnly ? PersonalityController.getPersonalityLabels(person)
                             : person.getPersonalityInterviewNotes();
        if (notes == null || notes.isBlank()) {
            return null;
        }
        // The notes may already be a full HTML document; only their body is wanted inside the dossier's label.
        return notes.replaceAll("(?i)</?(html|body)>", "").strip();
    }

    /**
     * @param level an experience level, such as {@link SkillType#EXP_VETERAN}
     *
     * @return the colour MekHQ shows that experience level in
     */
    static Color experienceColor(int level) {
        return colorOf(SkillType.getExperienceLevelColor(level), TEXT);
    }

    /**
     * @param hexColor a colour as {@code #RRGGBB}, or a blank string
     * @param fallback the colour to use if {@code hexColor} is blank or cannot be read
     *
     * @return the colour
     */
    static Color colorOf(@Nullable String hexColor, Color fallback) {
        if (hexColor == null || hexColor.isBlank()) {
            return fallback;
        }
        try {
            return Color.decode(hexColor.strip());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    // endregion Helpers
}
