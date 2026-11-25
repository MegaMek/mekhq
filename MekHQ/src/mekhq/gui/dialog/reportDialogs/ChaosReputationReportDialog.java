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
package mekhq.gui.dialog.reportDialogs;

import static java.lang.Math.clamp;
import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.SuiteOptions;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.dialog.glossary.GlossaryConsole;

/**
 * Explains how the force's Chaos Campaign Reputation was reached, drawn as a heads-up display in the style of the
 * Oracle Console. This is the Chaos Campaign counterpart to {@link ReputationReportDialog}, which reports on the
 * Campaign Operations reputation system.
 *
 * <p>The command bar carries tiles for the reputation total, the force's average experience and the personnel
 * counted. The body sets out, on the left, the steps from the base reputation to the total and how the personnel's
 * reputations are spread; on the right, how the force's combat roles are spread across experience levels. When
 * reputation is tracked at the campaign level, the stored campaign value takes the place of the personnel average and
 * the personnel spread is left out, as personnel do not change the value.</p>
 *
 * <p>Each tracking mode has a guide that slides over the report, opened by the guide button or by itself the first
 * time the report is opened in that mode, as the Oracle Console's guides do.</p>
 *
 * <p>The dialog is modeless, so it can stay open beside the campaign.</p>
 */
public class ChaosReputationReportDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ChaosReputationReportDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosReputation";
    private static final String KEY = "report.";

    private final Campaign campaign;
    /** The guide for the campaign's tracking mode, one of the {@link ChaosReputationGuidePreferences} guide keys. */
    private String guideKey = ChaosReputationGuidePreferences.PERSONNEL_GUIDE;
    private HudGuideDrawer guideDrawer;
    private HudCheckBox autoOpenGuides;

    /**
     * Creates the Chaos Reputation report for a campaign. The caller shows it with {@code setVisible(true)}.
     *
     * @param frame    the parent frame the dialog is anchored to
     * @param campaign the campaign whose Chaos Reputation is reported on
     */
    public ChaosReputationReportDialog(final JFrame frame, final Campaign campaign) {
        super(frame, getTextAt("mekhq.resources.GUI", "ChaosReputationReportDialog.title"), false);
        this.campaign = campaign;

        // Recalculate and store the force reputation as the report opens, so the rating it explains reflects the
        // current roster rather than a snapshot from the last daily update, contract completion, or enable.
        ChaosReputation.processChaosCampaignReputationChanges(campaign.getCampaignOptions(),
              campaign.getPlayerForce(),
              campaign.getLocalDate());

        Breakdown breakdown = Breakdown.of(campaign);
        guideKey = breakdown.isCampaignLevel()
                         ? ChaosReputationGuidePreferences.CAMPAIGN_GUIDE
                         : ChaosReputationGuidePreferences.PERSONNEL_GUIDE;
        buildUI(breakdown);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide();
            }
        });
        setMinimumSize(scaleForGUI(680, 480));
        getContentPane().setPreferredSize(scaleForGUI(900, 620));
        pack();
        setLocationRelativeTo(frame);
        setPreferences();
    }

    /**
     * @return the campaign this dialog reports on
     */
    public Campaign getCampaign() {
        return campaign;
    }

    // region Layout

    private void buildUI(Breakdown breakdown) {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(breakdown), BorderLayout.NORTH);
        content.add(buildBody(breakdown), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        buildGuide();
        installKeyBindings();
    }

    private JPanel buildCommandBar(Breakdown breakdown) {
        JPanel commandBar = Hud.commandBar(new BorderLayout(0, scaleForGUI(12)));
        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(24), 0));

        String mode = text(breakdown.isCampaignLevel() ? "mode.campaign" : "mode.personnel");
        String subtitle = getFormattedTextAt(RESOURCE_BUNDLE, KEY + "subtitle",
              MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate()),
              campaign.getPlayerForce().getName(),
              mode);
        topRow.add(Hud.identity(text("eyebrow"), text("title"), subtitle), BorderLayout.CENTER);

        HudStatTile reputationTile = new HudStatTile(text("tile.reputation"),
              breakdown.isCapped()
                    ? getFormattedTextAt(RESOURCE_BUNDLE, KEY + "tile.reputation.sub.capped", breakdown.cap())
                    : text("tile.reputation.sub"));
        reputationTile.setValue(Integer.toString(breakdown.total()),
              breakdown.total() < 0 ? DANGER : ACCENT_BRIGHT);

        HudStatTile experienceTile = new HudStatTile(text("tile.experience"),
              getFormattedTextAt(RESOURCE_BUNDLE, KEY + "tile.experience.sub", formatCount(breakdown.roleCount())));
        experienceTile.setValue(breakdown.averageSkillLevel().toString(),
              experienceColor(SkillType.getExperienceLevelColor(breakdown.averageSkillLevel())));

        HudStatTile personnelTile = new HudStatTile(text("tile.personnel"),
              text(breakdown.isCampaignLevel() ? "tile.personnel.sub.campaign" : "tile.personnel.sub"));
        personnelTile.setValue(formatCount(breakdown.personnelCount()), TEXT);

        topRow.add(Hud.topAligned(Hud.tileRow(reputationTile, experienceTile, personnelTile)), BorderLayout.EAST);

        HudButton guideButton = new HudButton("?  " + guideText("button").toUpperCase(Locale.ROOT), false, true);
        guideButton.setToolTipText(guideText("button.toolTipText"));
        guideButton.addActionListener(event -> openGuide());
        JPanel guideRow = Hud.transparentPanel(new BorderLayout());
        guideRow.add(guideButton, BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(guideRow, BorderLayout.SOUTH);
        return commandBar;
    }

    private JComponent buildBody(Breakdown breakdown) {
        JPanel left = Hud.column();
        left.add(buildTotalSection(breakdown));
        left.add(Box.createVerticalStrut(scaleForGUI(22)));
        if (breakdown.isCampaignLevel()) {
            JPanel note = Hud.section(text("section.personnelReputation"));
            Hud.addSpanning(note, Hud.wrappingText(text("campaignLevel.note"), TEXT_MUTED,
                  hudFont(Font.PLAIN, 0.9f, 0.0f)), 2);
            left.add(leftAligned(note));
        } else {
            left.add(leftAligned(distributionSection(text("section.personnelReputation"),
                  text("reputationValue"), text("count.personnel"), breakdown.reputationRows(),
                  text("personnel.empty"))));
        }

        JPanel right = Hud.column();
        right.add(leftAligned(distributionSection(text("section.experience"), text("experienceLevel"),
              text("count.roles"), breakdown.experienceRows(), text("experience.empty"))));
        right.add(Box.createVerticalStrut(scaleForGUI(22)));
        JPanel summary = Hud.section(text("section.experienceSummary"));
        Hud.addRow(summary, text("totalExperience"), formatCount(breakdown.totalExperience()), null,
              toolTip("totalExperience.toolTip"), false);
        Hud.addRow(summary, text("rolesCounted"), formatCount(breakdown.roleCount()), null,
              toolTip("rolesCounted.toolTip"), false);
        Hud.addRow(summary, text("averageSkillLevel"), breakdown.averageSkillLevel().toString(),
              experienceColor(SkillType.getExperienceLevelColor(breakdown.averageSkillLevel())), null, true);
        right.add(leftAligned(summary));

        JPanel columns = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(16), 0));
        columns.add(framed(left));
        columns.add(framed(right));

        JPanel body = Hud.windowBody(new BorderLayout());
        body.add(Hud.scroll(columns, GROUND), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildTotalSection(Breakdown breakdown) {
        JPanel section = Hud.section(text("section.total"));
        Hud.addRow(section,
              text(breakdown.isCampaignLevel() ? "campaignReputation" : "averageReputation"),
              Integer.toString(breakdown.base()),
              null,
              toolTip(breakdown.isCampaignLevel() ? "campaignReputation.toolTip" : "averageReputation.toolTip"),
              false);
        Hud.addRow(section, text("debtPenalty"), signed(breakdown.debtModifier()),
              modifierColor(breakdown.debtModifier()), toolTip("debtPenalty.toolTip"), false);
        Hud.addRow(section, text("manualModifier"), signed(breakdown.manualModifier()),
              modifierColor(breakdown.manualModifier()), toolTip("manualModifier.toolTip"), false);
        Hud.addRow(section, text("otherModifiers"), signed(breakdown.otherModifiers()),
              modifierColor(breakdown.otherModifiers()), toolTip("otherModifiers.toolTip"), false);
        if (breakdown.cap() != 0) {
            Hud.addRow(section, text("cap"), Integer.toString(breakdown.cap()),
                  breakdown.isCapped() ? AMBER : TEXT_MUTED, toolTip("cap.toolTip"), false);
        }
        Hud.addRow(section, text("reputationTotal"), Integer.toString(breakdown.total()),
              breakdown.total() < 0 ? DANGER : ACCENT_BRIGHT, null, true);
        leftAligned(section);
        return section;
    }

    /**
     * A section listing how many personnel or roles share each value, with a bar showing each row's share of the
     * largest.
     *
     * @param heading     the section's heading
     * @param valueHeader the caption over the first column
     * @param countHeader the caption over the count column
     * @param rows        the rows, in order
     * @param emptyText   the notice shown when there are no rows
     *
     * @return the section
     */
    private static JPanel distributionSection(String heading, String valueHeader, String countHeader,
          List<DistributionRow> rows, String emptyText) {
        JPanel section = Hud.section(heading);
        if (rows.isEmpty()) {
            Hud.addSpanning(section, Hud.wrappingText(emptyText, TEXT_FAINT, hudFont(Font.PLAIN, 0.9f, 0.0f)), 2);
            return section;
        }

        JPanel grid = Hud.transparentPanel(new GridBagLayout());
        addDistributionCell(grid, Hud.eyebrow(valueHeader), 0, 0, false);
        JLabel countCaption = Hud.eyebrow(countHeader);
        countCaption.setHorizontalAlignment(SwingConstants.RIGHT);
        addDistributionCell(grid, countCaption, 2, 0, false);

        int largest = rows.stream().mapToInt(DistributionRow::count).max().orElse(1);
        int gridRow = 1;
        for (DistributionRow row : rows) {
            JLabel label = new JLabel(row.label());
            label.setForeground(row.color());
            label.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            addDistributionCell(grid, label, 0, gridRow, false);

            addDistributionCell(grid, new ShareBar(row.count() / (double) largest, row.color()), 1, gridRow, true);

            JLabel count = new JLabel(formatCount(row.count()), SwingConstants.RIGHT);
            count.setForeground(TEXT);
            count.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
            addDistributionCell(grid, count, 2, gridRow, false);
            gridRow++;
        }
        Hud.addSpanning(section, grid, 2);
        return section;
    }

    private static void addDistributionCell(JPanel grid, JComponent cell, int column, int row, boolean stretch) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = stretch ? 1.0 : 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        int gap = scaleForGUI(10);
        constraints.insets = new Insets(scaleForGUI(3), column == 0 ? 0 : gap, scaleForGUI(3), 0);
        grid.add(cell, constraints);
    }

    private static JPanel framed(JComponent content) {
        JPanel panel = Hud.framedSurface(16);
        panel.setBackground(SURFACE_DEEP);
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildFooter() {
        HudButton close = new HudButton(text("close").toUpperCase(Locale.ROOT), true);
        close.addActionListener(event -> dispose());
        return Hud.footer(new JLabel(text("footer")), close);
    }

    /**
     * Escape closes an open guide, or otherwise the report, as it does other dialogs. This replaces the binding the
     * guide drawer installs, so it must run after {@link #buildGuide()}.
     */
    private void installKeyBindings() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "close");
        root.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (guideDrawer.isVisible()) {
                    closeGuide();
                } else {
                    dispose();
                }
            }
        });
    }

    // endregion Layout

    // region Guides

    /**
     * Builds the guide drawer, which floats over the report above a scrim; Escape or a click on the scrim closes it.
     */
    private void buildGuide() {
        autoOpenGuides = new HudCheckBox(guideText("autoOpen"));
        autoOpenGuides.setSelected(ChaosReputationGuidePreferences.getInstance().isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> ChaosReputationGuidePreferences.getInstance()
                                                        .setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(guideText("tab.steps"), guideText("tab.example"),
              guideText("tab.terms")), autoOpenGuides, guideText("close"), this::closeGuide);
        guideDrawer.install(getRootPane());
    }

    /** Opens the guide by itself the first time the report is opened in this mode, unless the player turned that off. */
    private void maybeOpenGuide() {
        ChaosReputationGuidePreferences preferences = ChaosReputationGuidePreferences.getInstance();
        if (isShowing() && preferences.isGuideAutoOpen() && !preferences.isGuideSeen(guideKey)) {
            openGuide();
        }
    }

    /** Opens the guide for this mode, marking it seen so it does not open by itself again. */
    private void openGuide() {
        ChaosReputationGuidePreferences preferences = ChaosReputationGuidePreferences.getInstance();
        preferences.setGuideSeen(guideKey, true);
        autoOpenGuides.setSelected(preferences.isGuideAutoOpen());
        guideDrawer.open(createGuide(guideKey));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    /**
     * Builds a guide from the resource bundle. The steps, example lines and terms are numbered keys
     * ({@code guide.personnel.step.1.title}, {@code .step.2.title}, ...), read until the next number is missing, so
     * text can be added or reworded without code changes.
     *
     * @param key the guide's key, such as {@link ChaosReputationGuidePreferences#PERSONNEL_GUIDE}
     *
     * @return the guide
     */
    private Guide createGuide(String key) {
        String prefix = key + ".";
        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "step." + number + ".title")); number++) {
            steps.add(new Step(guideText(prefix + "step." + number + ".title"),
                  guideText(prefix + "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "example." + number + ".who")); number++) {
            example.add(new ExampleLine(guideText(prefix + "example." + number + ".who"),
                  guideText(prefix + "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "term." + number + ".name")); number++) {
            terms.add(new Term(guideText(prefix + "term." + number + ".name"),
                  guideText(prefix + "term." + number + ".text")));
        }
        return new Guide(guideText(prefix + "eyebrow"), guideText(prefix + "title"), steps, null,
              guideText("exampleIntro"), example, terms, glossaryLink());
    }

    /** A link to the glossary's entry on reputation, for the foot of the Terms tab. */
    private JComponent glossaryLink() {
        JPanel links = Hud.column();
        links.add(leftAligned(Hud.eyebrow(guideText("glossary"))));
        links.add(Box.createVerticalStrut(scaleForGUI(4)));
        GlossaryEntry entry = GlossaryEntry.FORCE_REPUTATION;
        links.add(leftAligned(Hud.link(entry.getTitle(), () -> GlossaryConsole.showEntry(this, entry))));
        return links;
    }

    /** @return the guide text under {@code guide.}, which every guide's keys share */
    private static String guideText(String key) {
        return getTextAt(RESOURCE_BUNDLE, "guide." + key);
    }

    // endregion Guides

    // region Formatting

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }

    /** @return the text at a key, wrapped as a tooltip at a comfortable reading width */
    private static String toolTip(String key) {
        return Hud.wrapToolTip(text(key));
    }

    private static String formatCount(long count) {
        return NumberFormat.getIntegerInstance(SuiteOptions.getNumberFormatLocale()).format(count);
    }

    /** @return the modifier with its sign, such as "+2", "-1" or "0" */
    private static String signed(int modifier) {
        return modifier > 0 ? "+" + modifier : Integer.toString(modifier);
    }

    private static Color modifierColor(int modifier) {
        if (modifier > 0) {
            return READY;
        }
        return modifier < 0 ? DANGER : TEXT_MUTED;
    }

    /**
     * @param hex an experience level's colour from MekHQ's options, as {@code #RRGGBB}, or empty for none
     *
     * @return the colour, or the standard text colour if there is none or it cannot be read
     */
    private static Color experienceColor(@Nullable String hex) {
        if (hex == null || hex.isBlank()) {
            return TEXT;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException exception) {
            return TEXT;
        }
    }

    // endregion Formatting

    /**
     * Tracks the report's size and position in MekHQ's preferences.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChaosReputationReportDialog.class);
            setName("ChaosReputationReportDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    // region Calculation

    /**
     * One row of a distribution: a value, how many share it, and the colour to draw it in.
     *
     * @param label the value as shown
     * @param color the label's and bar's colour
     * @param count how many share the value
     */
    record DistributionRow(String label, Color color, int count) {
    }

    /**
     * Everything the report shows, worked out once as it opens.
     *
     * @param isCampaignLevel   whether reputation is tracked at the campaign level
     * @param base              the personnel average, or the stored campaign value in campaign-level mode
     * @param debtModifier      the debt penalty, after the commander's SPAs
     * @param manualModifier    the manual unit rating modifier
     * @param otherModifiers    the commander's other SPA modifiers
     * @param cap               the reputation cap, or {@code 0} for none
     * @param total             the final, capped reputation
     * @param personnelCount    the weighted number of personnel counted
     * @param reputationRows    how many personnel share each adjusted reputation, highest first
     * @param experienceRows    how many combat roles sit at each experience level, highest first
     * @param totalExperience   the weighted sum of the counted roles' experience levels
     * @param roleCount         the weighted number of combat roles counted
     * @param averageSkillLevel the mean experience level of the counted roles
     */
    record Breakdown(boolean isCampaignLevel, int base, int debtModifier, int manualModifier, int otherModifiers,
          int cap, int total, int personnelCount, List<DistributionRow> reputationRows,
          List<DistributionRow> experienceRows, int totalExperience, int roleCount, SkillLevel averageSkillLevel) {

        /** @return whether the cap lowered the total */
        boolean isCapped() {
            return cap != 0 && base + debtModifier + manualModifier + otherModifiers > cap;
        }

        /**
         * Works out the breakdown for a campaign's player force.
         *
         * @param campaign the campaign
         *
         * @return the breakdown
         */
        static Breakdown of(Campaign campaign) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            PlayerForce playerForce = campaign.getPlayerForce();
            LocalDate currentDate = campaign.getLocalDate();
            boolean isClanForce = playerForce.isClanForce();
            boolean isUseAgeEffects = campaignOptions.get(CampaignOption.USE_AGE_EFFECTS);
            boolean applyPersonality = campaignOptions.get(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION)
                                             && campaignOptions.get(CampaignOption.USE_RANDOM_PERSONALITIES);

            // Tally how many personnel share each value, rather than listing every person, so the report stays
            // compact for large campaigns.
            Map<Integer, Integer> reputationCounts = new TreeMap<>(Collections.reverseOrder());
            Map<Integer, Integer> experienceCounts = new TreeMap<>(Collections.reverseOrder());
            int personnelCount = 0;
            int totalReputation = 0;
            int roleCount = 0;
            int totalExperience = 0;
            for (Person person : activePersonnel(playerForce.allPersonnel())) {
                int weight = ChaosReputation.getPersonnelReputationWeight(person);
                if (weight == 0) {
                    continue;
                }

                personnelCount += weight;
                int adjustedReputation = person.getAdjustedReputation(isUseAgeEffects, isClanForce, currentDate,
                      applyPersonality);
                totalReputation += adjustedReputation * weight;
                reputationCounts.merge(adjustedReputation, weight, Integer::sum);

                for (boolean primary : new boolean[] { true, false }) {
                    int level = ChaosReputation.getExperienceLevel(campaignOptions, isClanForce, currentDate, person,
                          primary);
                    if (level != EXP_NONE) {
                        roleCount += weight;
                        totalExperience += level * weight;
                        experienceCounts.merge(level, weight, Integer::sum);
                    }
                }
            }

            int meanExperienceLevel = roleCount == 0 ? EXP_NONE : (int) round(totalExperience / (double) roleCount);
            meanExperienceLevel = clamp(meanExperienceLevel, EXP_NONE, EXP_LEGENDARY);

            // In campaign-level mode the reputation is a single stored value, not derived from personnel; it takes
            // the place of the personnel average.
            boolean isCampaignLevel = campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
            int averageReputation = personnelCount == 0 ? 0 : (int) round(totalReputation / (double) personnelCount);
            int base = isCampaignLevel ? playerForce.getChaosCampaignReputation() : averageReputation;

            Person commander = playerForce.getHumanResources().getCommander(campaignOptions, isClanForce,
                  currentDate);
            int debtModifier = ChaosReputation.applyCommanderDebtModifier(
                  ChaosReputation.getDebtModifier(playerForce.getFinances().getLoans(), currentDate,
                        campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)),
                  commander);
            int otherModifiers = ChaosReputation.getOtherModifiers(commander);
            int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
            int cap = campaignOptions.get(CampaignOption.CHAOS_REPUTATION_CAP);
            int total = ChaosReputation.applyReputationCap(cap, base + debtModifier + otherModifiers + manualModifier);

            List<DistributionRow> reputationRows = new ArrayList<>();
            reputationCounts.forEach((value, count) -> reputationRows.add(new DistributionRow(signed(value),
                  value < 0 ? DANGER : ACCENT_BRIGHT, count)));
            List<DistributionRow> experienceRows = new ArrayList<>();
            experienceCounts.forEach((level, count) -> experienceRows.add(new DistributionRow(
                  SkillType.getExperienceLevelName(level),
                  experienceColor(SkillType.getExperienceLevelColor(level)),
                  count)));

            return new Breakdown(isCampaignLevel, base, debtModifier, manualModifier, otherModifiers, cap, total,
                  personnelCount, List.copyOf(reputationRows), List.copyOf(experienceRows), totalExperience,
                  roleCount, SkillType.skillLevelFromExperienceLevel(meanExperienceLevel));
        }

        private static List<Person> activePersonnel(Collection<Person> personnel) {
            List<Person> active = new ArrayList<>();
            for (Person person : personnel) {
                PersonnelStatus status = person.getStatus();
                if (!status.isDepartedUnit() && status.isActive() && person.isEmployed()) {
                    active.add(person);
                }
            }
            return active;
        }
    }

    // endregion Calculation

    /** A thin bar showing one row's share of the largest row in a distribution. */
    private static final class ShareBar extends JComponent {
        private final double share;
        private final Color color;

        private ShareBar(double share, Color color) {
            this.share = clamp(share, 0.0, 1.0);
            this.color = color;
            setPreferredSize(new Dimension(scaleForGUI(80), scaleForGUI(8)));
            setMinimumSize(new Dimension(scaleForGUI(20), scaleForGUI(8)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int height = scaleForGUI(6);
                int top = (getHeight() - height) / 2;
                canvas.setColor(DIVIDER);
                canvas.fillRect(0, top, getWidth(), height);
                canvas.setColor(translucent(color, 200));
                canvas.fillRect(0, top, (int) round(getWidth() * share), height);
            } finally {
                canvas.dispose();
            }
        }
    }
}
