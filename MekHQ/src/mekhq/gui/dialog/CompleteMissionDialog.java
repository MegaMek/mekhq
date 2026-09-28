/*
 * Copyright (C) 2010-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.utilities.ContractDebriefStatistics;
import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;

/**
 * Contract debrief dialog shown when the player completes a mission.
 *
 * <p>Rather than a bare status dropdown, this presents an after-action console styled on the interstellar-map tab: a
 * briefing header for the contract, an auto-evaluated verdict banner recommending the outcome the contract's
 * performance implies, the combat record and objective/ledger statistics, and a segmented outcome selector the player
 * uses to confirm - or override - the recommended {@link MissionStatus}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CompleteMissionDialog extends AbstractButtonDialog {
    // region Palette (mirrors the interstellar-map tab)
    private static final Color GROUND = new Color(5, 13, 23);
    private static final Color SURFACE = new Color(15, 30, 43);
    private static final Color SURFACE_DEEP = new Color(7, 16, 27);
    private static final Color SURFACE_HIGHLIGHT = new Color(18, 45, 56);
    private static final Color BORDER = new Color(35, 66, 82);
    private static final Color BORDER_CYAN = new Color(65, 210, 224, 107);
    private static final Color DIVIDER = new Color(65, 210, 224, 40);
    private static final Color ACCENT = new Color(65, 210, 224);
    private static final Color ACCENT_BRIGHT = new Color(125, 230, 238);
    private static final Color AMBER = new Color(235, 166, 66);
    private static final Color TEXT = new Color(218, 231, 235);
    private static final Color TEXT_MUTED = new Color(158, 179, 187);
    private static final Color TEXT_FAINT = new Color(132, 153, 161);
    private static final Color SCROLLBAR_THUMB = new Color(54, 101, 113);
    // The faction logo ships black-tinted; recolour it to the map's bright cyan so the emblem reads on the dark plate.
    private static final Color EMBLEM_TINT = ACCENT_BRIGHT;

    private static final Color OUTCOME_SUCCESS = new Color(82, 199, 160);
    private static final Color OUTCOME_PARTIAL = ACCENT;
    private static final Color OUTCOME_FAILED = new Color(224, 134, 78);
    private static final Color OUTCOME_BREACH = new Color(215, 87, 79);
    // endregion Palette

    private static final int CONTENT_WIDTH = 600;
    private static final double MAX_VIEWPORT_SCREEN_FRACTION = 0.85;
    private static final int SCROLLBAR_WIDTH = 10;

    // The completion statuses the player may record - the completed set of MissionStatus, in debrief order.
    private static final MissionStatus[] OUTCOME_OPTIONS = { MissionStatus.SUCCESS, MissionStatus.PARTIAL,
                                                             MissionStatus.FAILED, MissionStatus.BREACH };

    private final transient Campaign campaign;
    private final transient AbstractContract mission;
    private final transient ContractDebriefStatistics statistics;

    private MissionStatus selectedStatus;

    // Verdict-banner controls updated as the selection changes.
    private VerdictBanner verdictBanner;
    private JLabel verdictWord;
    private JLabel verdictReason;
    private JLabel verdictBadge;
    private OutcomeSelector outcomeSelector;

    /**
     * Creates the contract debrief dialog.
     *
     * @param frame    the parent frame
     * @param campaign the active campaign, used for date formatting and the current location
     * @param mission  the contract being completed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public CompleteMissionDialog(final JFrame frame, final Campaign campaign, final AbstractContract mission) {
        super(frame, true, MekHQ.getDefaultResourceBundle(), "CompleteMissionDialog", "CompleteMissionDialog.title");
        this.campaign = campaign;
        this.mission = mission;
        this.statistics = ContractDebriefStatistics.from(mission,
              campaign.getCampaignOptions().isUseStratConMaplessMode(), campaign.getLocalDate());
        this.selectedStatus = statistics.getRecommendedStatus();
        initialize();
    }

    /**
     * @return the outcome the player chose in the selector, defaulting to {@link MissionStatus#ACTIVE} only if nothing
     *       is selected (which cannot happen through the UI)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public MissionStatus getStatus() {
        return (selectedStatus == null) ? MissionStatus.ACTIVE : selectedStatus;
    }

    // region Initialization
    @Override
    protected Container createCenterPane() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(true);
        content.setBackground(GROUND);
        int pad = UIUtil.scaleForGUI(18);
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        content.add(createBriefingHeader());
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(18)));
        content.add(createSectionHeading(getText("completeMission.section.combatRecord")));
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
        content.add(createCombatRecord());
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(18)));
        content.add(createSectionHeading(getText("completeMission.section.objectivesLedger")));
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
        content.add(createObjectivesLedger());
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(18)));
        content.add(createVerdictBanner());
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(18)));
        content.add(createSectionHeading(getText("completeMission.section.recordOutcome")));
        content.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
        content.add(createOutcomeSelector());
        content.add(createGuidance());

        applySelectedStatus();

        JScrollPane scrollPane = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(GROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIUtil.scaleForGUI(16));
        ImmersiveScrollBarStyle.apply(scrollPane.getVerticalScrollBar(), GROUND, DIVIDER, SCROLLBAR_THUMB,
              ACCENT_BRIGHT, UIUtil.scaleForGUI(SCROLLBAR_WIDTH));

        int contentWidth = UIUtil.scaleForGUI(CONTENT_WIDTH);
        int screenHeightCap = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * MAX_VIEWPORT_SCREEN_FRACTION);
        int viewportHeight = Math.min(content.getPreferredSize().height, screenHeightCap);
        scrollPane.setPreferredSize(new Dimension(contentWidth + UIUtil.scaleForGUI(SCROLLBAR_WIDTH + 2),
              viewportHeight));
        return scrollPane;
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(SURFACE_DEEP);
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(12), UIUtil.scaleForGUI(18),
                    UIUtil.scaleForGUI(12), UIUtil.scaleForGUI(18))));

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.weightx = 1.0;
        spacer.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), spacer);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 1;
        buttonConstraints.insets = new Insets(0, 0, 0, UIUtil.scaleForGUI(10));
        HudButton cancelButton = new HudButton(getText("Cancel.text"), false);
        cancelButton.setToolTipText(getText("Cancel.toolTipText"));
        cancelButton.addActionListener(this::cancelActionPerformed);
        panel.add(cancelButton, buttonConstraints);

        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(0, 0, 0, 0);
        HudButton confirmButton = new HudButton(getText("completeMission.confirm.text"), true);
        confirmButton.setToolTipText(getText("completeMission.confirm.toolTipText"));
        confirmButton.addActionListener(this::okButtonActionPerformed);
        panel.add(confirmButton, buttonConstraints);

        return panel;
    }

    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }

    /**
     * Enforces a content-sized floor after the base initialization restores the stored window preferences. The
     * previous version of this dialog stored a much smaller size under the same preference key, so a restored size can
     * be far too small to show the debrief; here the window is never allowed below its packed content size (capped to
     * 80% of the screen) and is grown back to it when a stale, smaller size was restored.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension preferred = getPreferredSize();
        int targetWidth = Math.min(preferred.width, (int) (screenSize.width * 0.8));
        int targetHeight = Math.min(preferred.height, (int) (screenSize.height * 0.8));
        Dimension target = new Dimension(targetWidth, targetHeight);

        setMinimumSize(target);
        if ((getWidth() < targetWidth) || (getHeight() < targetHeight)) {
            setSize(target);
            fitAndCenter();
        }
    }
    // endregion Initialization

    // region Briefing Header
    private JComponent createBriefingHeader() {
        JPanel header = new JPanel(new BorderLayout(UIUtil.scaleForGUI(16), 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(new EmblemBadge(getFactionLogo(campaign.getGameYear(), mission.getEmployerFactionCode())),
              BorderLayout.LINE_START);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);

        details.add(leftAligned(createEyebrow(objectiveSummaryText())));
        details.add(Box.createVerticalStrut(UIUtil.scaleForGUI(3)));

        JLabel contractName = new JLabel(mission.getName());
        contractName.setForeground(TEXT);
        contractName.setFont(hudFont(Font.BOLD, 1.7f, 0.01f));
        details.add(leftAligned(contractName));
        details.add(Box.createVerticalStrut(UIUtil.scaleForGUI(5)));

        details.add(leftAligned(createBelligerents()));
        details.add(Box.createVerticalStrut(UIUtil.scaleForGUI(11)));
        details.add(createMetaGrid());

        header.add(details, BorderLayout.CENTER);
        return capHeight(header);
    }

    private JComponent createBelligerents() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);

        JLabel employer = new JLabel(mission.getEmployerDisplayName());
        employer.setForeground(TEXT);
        employer.setFont(hudFont(Font.PLAIN, 1.0f, 0.03f));

        JLabel versus = new JLabel(getText("completeMission.belligerents.versus"));
        versus.setForeground(AMBER);
        versus.setFont(hudFont(Font.BOLD, 0.78f, 0.10f));
        versus.setBorder(BorderFactory.createEmptyBorder(0, UIUtil.scaleForGUI(8), 0, UIUtil.scaleForGUI(8)));

        JLabel enemy = new JLabel(mission.getEnemyDisplayNameIncludingFaction(campaign.getGameYear()));
        enemy.setForeground(TEXT_MUTED);
        enemy.setFont(hudFont(Font.PLAIN, 1.0f, 0.03f));

        row.add(employer);
        row.add(versus);
        row.add(enemy);
        return row;
    }

    private JComponent createMetaGrid() {
        LocalDate today = campaign.getLocalDate();
        String systemPlanet = buildSystemPlanet(today);
        String duration = buildDuration();

        JPanel grid = dividerGrid(2);
        grid.add(createMetaCell(getText("completeMission.meta.location"), systemPlanet));
        grid.add(createMetaCell(getText("completeMission.meta.commandRights"),
              mission.getCommandRights().toString()));
        grid.add(createMetaCell(getText("completeMission.meta.duration"), duration));
        grid.add(createMetaCell(getText("completeMission.meta.employerMorale"),
              mission.getMoraleLevel().toString()));
        return grid;
    }

    private String buildSystemPlanet(LocalDate today) {
        String systemName = mission.getTargetSystemName(today);
        String planetName = mission.getTargetPlanetName(today);
        if ((planetName == null) || planetName.isBlank() || planetName.equals(systemName)) {
            return systemName;
        }
        return getFormattedText("completeMission.meta.location.format", systemName, planetName);
    }

    private String buildDuration() {
        LocalDate start = mission.getStartDate();
        LocalDate end = mission.getEndingDate();
        String startText = (start == null) ? "?" : MekHQ.getMHQOptions().getDisplayFormattedDate(start);
        String endText = (end == null) ? "?" : MekHQ.getMHQOptions().getDisplayFormattedDate(end);
        return getFormattedText("completeMission.meta.duration.format", startText, endText,
              mission.getLengthInMonths());
    }

    private String objectiveSummaryText() {
        String objectiveType = mission.getObjectiveType().toString();
        String stance = mission.isPlayerAttacker()
              ? getText("completeMission.stance.attacker")
              : getText("completeMission.stance.defender");
        return getFormattedText("completeMission.objectiveSummary.format", objectiveType, stance);
    }
    // endregion Briefing Header

    // region Verdict Banner
    private JComponent createVerdictBanner() {
        verdictBanner = new VerdictBanner();
        verdictBanner.setLayout(new BorderLayout(UIUtil.scaleForGUI(16), 0));
        int inset = UIUtil.scaleForGUI(14);
        verdictBanner.setBorder(BorderFactory.createEmptyBorder(inset, inset + UIUtil.scaleForGUI(4), inset, inset));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        text.add(leftAligned(createEyebrow(getText("completeMission.verdict.eyebrow"))));
        text.add(Box.createVerticalStrut(UIUtil.scaleForGUI(4)));

        verdictWord = new JLabel();
        verdictWord.setFont(hudFont(Font.BOLD, 2.0f, 0.02f));
        text.add(leftAligned(verdictWord));
        text.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));

        verdictReason = new JLabel();
        verdictReason.setForeground(TEXT_MUTED);
        verdictReason.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        text.add(leftAligned(verdictReason));

        verdictBanner.add(text, BorderLayout.CENTER);

        verdictBadge = new JLabel();
        verdictBadge.setFont(hudFont(Font.BOLD, 0.72f, 0.14f));
        verdictBadge.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        GridBagConstraints badgeConstraints = new GridBagConstraints();
        badgeConstraints.anchor = GridBagConstraints.NORTH;
        badgeHolder.add(verdictBadge, badgeConstraints);
        verdictBanner.add(badgeHolder, BorderLayout.LINE_END);

        return capHeight(verdictBanner);
    }

    /**
     * Repaints the verdict banner and outcome selector to reflect {@link #selectedStatus}, flagging the badge as an
     * override when the player has moved away from the auto-evaluated recommendation.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void applySelectedStatus() {
        Color color = outcomeColor(selectedStatus);
        verdictBanner.setAccent(color);
        verdictWord.setForeground(color);
        verdictWord.setText(selectedStatus.toString());
        verdictReason.setText(buildReasonText());

        boolean isRecommendation = selectedStatus == statistics.getRecommendedStatus();
        verdictBadge.setText(isRecommendation
              ? getText("completeMission.verdict.badge.auto")
              : getText("completeMission.verdict.badge.override"));
        verdictBadge.setForeground(color);
        verdictBadge.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(color, UIUtil.scaleForGUI(1)),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(9),
                    UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(9))));

        if (outcomeSelector != null) {
            outcomeSelector.setSelected(selectedStatus);
        }
    }

    private String buildReasonText() {
        String body;
        if (selectedStatus == MissionStatus.BREACH) {
            body = getText("completeMission.reason.breach");
        } else if (statistics.hasVictoryPointTarget()) {
            body = getFormattedText("completeMission.reason.victoryPoints." + statusKey(selectedStatus),
                  statistics.getContractScore(), statistics.getRequiredVictoryPoints());
        } else if (statistics.hasStratConObjectives()) {
            body = getFormattedText("completeMission.reason.objectives." + statusKey(selectedStatus),
                  statistics.getObjectivesCompleted(), statistics.getObjectivesTotal(),
                  statistics.getObjectivesFailed());
        } else {
            body = getFormattedText("completeMission.reason.record." + statusKey(selectedStatus),
                  statistics.getVictories(), statistics.getDefeats(), statistics.getResolvedScenarios());
        }
        return "<html><body style='width:%dpx'>%s</body></html>".formatted(UIUtil.scaleForGUI(360), body);
    }

    private static String statusKey(MissionStatus status) {
        return switch (status) {
            case SUCCESS -> "success";
            case PARTIAL -> "partial";
            case FAILED -> "failure";
            case BREACH -> "breach";
            case ACTIVE -> "active";
        };
    }
    // endregion Verdict Banner

    // region Statistics
    private JComponent createCombatRecord() {
        JPanel grid = dividerGrid(4);

        grid.add(createStatTile(getText("completeMission.stat.engagements"),
              Integer.toString(statistics.getResolvedScenarios()), null,
              getText("completeMission.stat.engagements.sub"), TEXT));
        grid.add(createStatTile(getText("completeMission.stat.victories"),
              Integer.toString(statistics.getVictories()), null,
              getFormattedText("completeMission.stat.victories.sub",
                    statistics.getDecisiveVictories()), OUTCOME_SUCCESS));
        grid.add(createStatTile(getText("completeMission.stat.defeats"),
              Integer.toString(statistics.getDefeats()), null,
              getFormattedText("completeMission.stat.defeats.sub",
                    statistics.getDecisiveDefeats()), OUTCOME_FAILED));
        grid.add(createStatTile(getText("completeMission.stat.winRate"),
              Integer.toString(statistics.getWinRatePercent()), "%",
              getText("completeMission.stat.winRate.sub"), AMBER));

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(grid);
        column.add(new RecordBar(statistics.getVictories(), statistics.getDraws(), statistics.getDefeats()));
        return capHeight(column);
    }

    private JComponent createObjectivesLedger() {
        JPanel grid = dividerGrid(4);

        if (statistics.hasStratConObjectives()) {
            boolean allSecured = statistics.getObjectivesFailed() == 0;
            grid.add(createStatTile(getText("completeMission.stat.objectives"),
                  Integer.toString(statistics.getObjectivesCompleted()),
                  "/" + statistics.getObjectivesTotal(),
                  getText(allSecured
                        ? "completeMission.stat.objectives.sub.allSecured"
                        : "completeMission.stat.objectives.sub.someLost"),
                  allSecured ? OUTCOME_SUCCESS : AMBER));
        } else {
            grid.add(createStatTile(getText("completeMission.stat.objectives"),
                  getText("completeMission.stat.notTracked"), null,
                  getText("completeMission.stat.objectives.none"), TEXT_MUTED));
        }

        if (statistics.hasVictoryPointTarget()) {
            boolean targetMet = statistics.isVictoryPointTargetMet();
            grid.add(createStatTile(getText("completeMission.stat.victoryPoints"),
                  Integer.toString(statistics.getContractScore()),
                  "/" + statistics.getRequiredVictoryPoints(),
                  getText(targetMet
                        ? "completeMission.stat.victoryPoints.sub.met"
                        : "completeMission.stat.victoryPoints.sub.below"),
                  targetMet ? OUTCOME_SUCCESS : OUTCOME_FAILED));
        } else {
            grid.add(createStatTile(getText("completeMission.stat.victoryPoints"),
                  Integer.toString(statistics.getContractScore()), null,
                  getText("completeMission.stat.victoryPoints.noTarget"), TEXT));
        }

        grid.add(createMoneyTile(getText("completeMission.stat.contractPay"),
              statistics.getTotalContractPay(),
              getText("completeMission.stat.contractPay.sub")));
        grid.add(createMoneyTile(getText("completeMission.stat.salvage"),
              statistics.getSalvageWonValue(),
              getText("completeMission.stat.salvage.sub")));
        return capHeight(grid);
    }

    private JComponent createStatTile(String key, String value, String valueSuffix, String sub, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(SURFACE_DEEP);
        int inset = UIUtil.scaleForGUI(11);
        tile.setBorder(BorderFactory.createEmptyBorder(inset, inset, inset, inset));

        tile.add(leftAligned(createLabelText(key)));
        tile.add(Box.createVerticalStrut(UIUtil.scaleForGUI(4)));

        JLabel valueLabel = new JLabel(valueSuffix == null
              ? value
              : "<html>%s<span style='font-size:70%%; color:rgb(158,179,187);'>%s</span></html>".formatted(value,
                    valueSuffix));
        valueLabel.setForeground(valueColor);
        valueLabel.setFont(hudFont(Font.BOLD, 1.55f, 0.0f));
        tile.add(leftAligned(valueLabel));

        if (sub != null) {
            tile.add(Box.createVerticalStrut(UIUtil.scaleForGUI(3)));
            JLabel subLabel = new JLabel(sub);
            subLabel.setForeground(TEXT_FAINT);
            subLabel.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
            tile.add(leftAligned(subLabel));
        }
        return tile;
    }

    private JComponent createMoneyTile(String key, Money money, String sub) {
        return createStatTile(key, money.toAmountAndSymbolString(), null, sub, TEXT);
    }
    // endregion Statistics

    // region Outcome Selector & Guidance
    private JComponent createOutcomeSelector() {
        outcomeSelector = new OutcomeSelector();
        return capHeight(outcomeSelector);
    }

    private JComponent createGuidance() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(1),
                    BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(12), UIUtil.scaleForGUI(15), UIUtil.scaleForGUI(13),
                    UIUtil.scaleForGUI(15))));

        panel.add(leftAligned(createEyebrow(getText("completeMission.guidance.heading"))));
        panel.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));

        String guidanceBody = "<html><body style='width:%dpx'>%s</body></html>".formatted(UIUtil.scaleForGUI(540),
              getText("completeMission.guidance.body"));
        JLabel guidance = new JLabel(guidanceBody);
        guidance.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        guidance.setForeground(TEXT_MUTED);
        panel.add(leftAligned(guidance));
        return capHeight(panel);
    }
    // endregion Outcome Selector & Guidance

    // region Shared UI helpers
    private JComponent createSectionHeading(String text) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        label.setFont(hudFont(Font.BOLD, 0.82f, 0.16f));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        row.add(label, labelConstraints);

        JPanel rule = new JPanel();
        rule.setOpaque(true);
        rule.setBackground(DIVIDER);
        rule.setPreferredSize(new Dimension(UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(1)));
        GridBagConstraints ruleConstraints = new GridBagConstraints();
        ruleConstraints.gridx = 1;
        ruleConstraints.weightx = 1.0;
        ruleConstraints.fill = GridBagConstraints.HORIZONTAL;
        ruleConstraints.insets = new Insets(0, UIUtil.scaleForGUI(10), 0, 0);
        row.add(rule, ruleConstraints);
        return capHeight(row);
    }

    private JLabel createEyebrow(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        return label;
    }

    private JLabel createLabelText(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        return label;
    }

    private JComponent createMetaCell(String key, String value) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setOpaque(true);
        cell.setBackground(SURFACE_DEEP);
        int inset = UIUtil.scaleForGUI(8);
        cell.setBorder(BorderFactory.createEmptyBorder(inset, UIUtil.scaleForGUI(11), inset, UIUtil.scaleForGUI(11)));

        cell.add(leftAligned(createLabelText(key)));
        cell.add(Box.createVerticalStrut(UIUtil.scaleForGUI(2)));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        cell.add(leftAligned(valueLabel));
        return cell;
    }

    /** A grid whose 1px cell gaps show the divider colour behind, matching the map's inset-line panels. */
    private JPanel dividerGrid(int columns) {
        JPanel grid = new JPanel(new GridLayout(0, columns, UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(1)));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, UIUtil.scaleForGUI(1)));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        return grid;
    }

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    /** Stops a component from stretching vertically past its preferred height inside the BoxLayout column. */
    private static JComponent capHeight(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return component;
    }

    private static Font hudFont(int style, float sizeFactor, float tracking) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new JLabel().getFont();
        }
        Font sized = base.deriveFont(style, base.getSize2D() * sizeFactor);
        if (tracking == 0.0f) {
            return sized;
        }
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return sized.deriveFont(attributes);
    }

    private static Color outcomeColor(MissionStatus status) {
        return switch (status) {
            case SUCCESS -> OUTCOME_SUCCESS;
            case PARTIAL -> OUTCOME_PARTIAL;
            case FAILED -> OUTCOME_FAILED;
            case BREACH -> OUTCOME_BREACH;
            case ACTIVE -> ACCENT;
        };
    }

    private static Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    // endregion Shared UI helpers

    // region Custom components
    /**
     * The header emblem: the employer faction's logo, recoloured from its shipped black tint to a near-white
     * silhouette, set into a chamfered plate that echoes the map's system markers.
     */
    private static final class EmblemBadge extends JComponent {
        private final Image image;

        private EmblemBadge(ImageIcon icon) {
            this.image = (icon == null) ? null : tintToColor(icon.getImage(), EMBLEM_TINT);
            Dimension size = UIUtil.scaleForGUI(58, 58);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        /**
         * Recolours an image to a solid tint while preserving each pixel's alpha, so a black-tinted logo becomes a
         * clean silhouette in the given colour with its anti-aliased edges intact.
         */
        private static Image tintToColor(Image source, Color tint) {
            int width = source.getWidth(null);
            int height = source.getHeight(null);
            if ((width <= 0) || (height <= 0)) {
                return source;
            }

            BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffer.createGraphics();
            g2.drawImage(source, 0, 0, null);
            g2.dispose();

            int tintRgb = tint.getRGB() & 0x00FFFFFF;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = buffer.getRGB(x, y) >>> 24;
                    buffer.setRGB(x, y, (alpha << 24) | tintRgb);
                }
            }
            return buffer;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int width = getWidth();
                int height = getHeight();
                int chamfer = UIUtil.scaleForGUI(9);
                int[] xPoints = { chamfer, width, width, width - chamfer, 0, 0 };
                int[] yPoints = { 0, 0, height - chamfer, height, height, chamfer };
                g2.setPaint(new GradientPaint(0, 0, SURFACE_HIGHLIGHT, 0, height, SURFACE_DEEP));
                g2.fillPolygon(xPoints, yPoints, xPoints.length);

                if (image != null) {
                    int imageWidth = image.getWidth(this);
                    int imageHeight = image.getHeight(this);
                    if ((imageWidth > 0) && (imageHeight > 0)) {
                        // Fit the icon inside the plate, preserving its aspect ratio, and clip to the plate outline so
                        // a non-square icon never spills past the chamfered edge.
                        int padding = UIUtil.scaleForGUI(8);
                        int availableWidth = width - (padding * 2);
                        int availableHeight = height - (padding * 2);
                        double scale = Math.min(availableWidth / (double) imageWidth,
                              availableHeight / (double) imageHeight);
                        int drawWidth = (int) Math.round(imageWidth * scale);
                        int drawHeight = (int) Math.round(imageHeight * scale);
                        g2.setClip(new java.awt.Polygon(xPoints, yPoints, xPoints.length));
                        g2.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight,
                              this);
                        g2.setClip(null);
                    }
                }

                g2.setColor(BORDER_CYAN);
                g2.drawPolygon(xPoints, yPoints, xPoints.length);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Banner background: a settable left accent stripe and a fading tint over the surface fill. */
    private static final class VerdictBanner extends JPanel {
        private Color accent = ACCENT;

        private VerdictBanner() {
            setOpaque(false);
        }

        private void setAccent(Color accent) {
            this.accent = accent;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                g2.setColor(SURFACE);
                g2.fillRect(0, 0, width, height);
                g2.setPaint(new GradientPaint(0, 0, translucent(accent, 30), width * 0.65f, 0, SURFACE));
                g2.fillRect(0, 0, width, height);
                g2.setColor(BORDER);
                g2.drawRect(0, 0, width - 1, height - 1);
                g2.setColor(accent);
                g2.fillRect(0, 0, UIUtil.scaleForGUI(3), height);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    /** A proportional win / draw / loss strip beneath the combat-record tiles. */
    private static final class RecordBar extends JPanel {
        private final int wins;
        private final int draws;
        private final int losses;

        private RecordBar(int wins, int draws, int losses) {
            this.wins = wins;
            this.draws = draws;
            this.losses = losses;
            setOpaque(true);
            setBackground(SURFACE_DEEP);
            setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(11), UIUtil.scaleForGUI(11),
                  UIUtil.scaleForGUI(13), UIUtil.scaleForGUI(11)));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel key = new JLabel(getText("completeMission.stat.battleLedger"));
            key.setForeground(TEXT_FAINT);
            key.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
            header.add(key, BorderLayout.LINE_START);
            JLabel legend = new JLabel(getFormattedText("completeMission.stat.battleLedger.legend",
                  wins, draws, losses));
            legend.setForeground(TEXT_MUTED);
            legend.setFont(hudFont(Font.PLAIN, 0.75f, 0.0f));
            header.add(legend, BorderLayout.LINE_END);
            add(header);
            add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));

            Track track = new Track();
            track.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(track);
        }

        private final class Track extends JComponent {
            private Track() {
                Dimension size = new Dimension(UIUtil.scaleForGUI(200), UIUtil.scaleForGUI(12));
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(12)));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                try {
                    int width = getWidth();
                    int height = getHeight();
                    g2.setColor(GROUND);
                    g2.fillRect(0, 0, width, height);

                    int total = wins + draws + losses;
                    if (total > 0) {
                        int usable = width - UIUtil.scaleForGUI(2);
                        int x = UIUtil.scaleForGUI(1);
                        int winWidth = Math.round((usable * (float) wins) / total);
                        int drawWidth = Math.round((usable * (float) draws) / total);
                        int lossWidth = usable - winWidth - drawWidth;
                        int barTop = UIUtil.scaleForGUI(1);
                        int barHeight = height - UIUtil.scaleForGUI(2);
                        g2.setColor(OUTCOME_SUCCESS);
                        g2.fillRect(x, barTop, winWidth, barHeight);
                        g2.setColor(TEXT_FAINT);
                        g2.fillRect(x + winWidth, barTop, drawWidth, barHeight);
                        g2.setColor(OUTCOME_FAILED);
                        g2.fillRect(x + winWidth + drawWidth, barTop, lossWidth, barHeight);
                    }
                    g2.setColor(BORDER);
                    g2.drawRect(0, 0, width - 1, height - 1);
                } finally {
                    g2.dispose();
                }
            }
        }
    }

    /** The segmented outcome selector: four cells with the map's glowing selected-tab underline. */
    private final class OutcomeSelector extends JPanel {
        private final Map<MissionStatus, OutcomeCell> cells = new HashMap<>();

        private OutcomeSelector() {
            setLayout(new GridLayout(1, OUTCOME_OPTIONS.length, 0, 0));
            setOpaque(true);
            setBackground(SURFACE_DEEP);
            setBorder(BorderFactory.createLineBorder(BORDER, UIUtil.scaleForGUI(1)));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            for (int index = 0; index < OUTCOME_OPTIONS.length; index++) {
                MissionStatus status = OUTCOME_OPTIONS[index];
                OutcomeCell cell = new OutcomeCell(status, index < OUTCOME_OPTIONS.length - 1);
                cells.put(status, cell);
                add(cell);
            }
        }

        private void setSelected(MissionStatus status) {
            for (Map.Entry<MissionStatus, OutcomeCell> entry : cells.entrySet()) {
                entry.getValue().setSelected(entry.getKey() == status);
            }
        }
    }

    private final class OutcomeCell extends JPanel {
        private final MissionStatus status;
        private final Color accent;
        private final boolean rightBorder;
        private final JLabel title;
        private boolean selected;
        private boolean hovered;

        private OutcomeCell(MissionStatus status, boolean rightBorder) {
            this.status = status;
            this.accent = outcomeColor(status);
            this.rightBorder = rightBorder;
            setOpaque(false);
            setFocusable(true);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(12), UIUtil.scaleForGUI(6),
                  UIUtil.scaleForGUI(11), UIUtil.scaleForGUI(6)));
            setToolTipText(status.getToolTipText());

            title = new JLabel(outcomeTitle(status), SwingConstants.CENTER);
            title.setForeground(TEXT_MUTED);
            title.setFont(hudFont(Font.BOLD, 0.85f, 0.06f));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel(outcomeSubtitle(status), SwingConstants.CENTER);
            subtitle.setForeground(TEXT_FAINT);
            subtitle.setFont(hudFont(Font.PLAIN, 0.72f, 0.0f));
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(title);
            add(Box.createVerticalStrut(UIUtil.scaleForGUI(3)));
            add(subtitle);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                // Fire on release rather than click: mouseClicked is swallowed by the slightest pointer movement between
                // press and release, which makes the control feel unresponsive.
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        choose();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    refreshTitleColor();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    refreshTitleColor();
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);

            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    if ((event.getKeyCode() == KeyEvent.VK_SPACE) || (event.getKeyCode() == KeyEvent.VK_ENTER)) {
                        choose();
                    }
                }
            });
        }

        private void choose() {
            selectedStatus = status;
            requestFocusInWindow();
            applySelectedStatus();
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            refreshTitleColor();
            repaint();
        }

        private void refreshTitleColor() {
            title.setForeground((selected || hovered) ? TEXT : TEXT_MUTED);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                Color background = selected ? SURFACE : (hovered ? SURFACE_HIGHLIGHT : SURFACE_DEEP);
                g2.setColor(background);
                g2.fillRect(0, 0, width, height);

                if (rightBorder) {
                    g2.setColor(BORDER);
                    g2.fillRect(width - UIUtil.scaleForGUI(1), 0, UIUtil.scaleForGUI(1), height);
                }

                if (selected) {
                    int underline = UIUtil.scaleForGUI(2);
                    g2.setColor(accent);
                    g2.fillRect(0, height - underline, width, underline);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static String outcomeTitle(MissionStatus status) {
        return getText("completeMission.outcome." + statusKey(status) + ".title");
    }

    private static String outcomeSubtitle(MissionStatus status) {
        return getText("completeMission.outcome." + statusKey(status) + ".subtitle");
    }
    // endregion Custom components

    /** Flat HUD-styled button used in place of the base OK/Cancel controls. */
    private static final class HudButton extends JPanel {
        private final JLabel label;
        private final boolean primary;
        private boolean hovered;
        private java.util.List<java.awt.event.ActionListener> listeners = new java.util.ArrayList<>();

        private HudButton(String text, boolean primary) {
            this.primary = primary;
            setOpaque(false);
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(9), UIUtil.scaleForGUI(22),
                  UIUtil.scaleForGUI(9), UIUtil.scaleForGUI(22)));
            setFocusable(true);

            label = new JLabel(text);
            label.setFont(hudFont(Font.BOLD, 0.82f, 0.12f));
            label.setForeground(primary ? ACCENT_BRIGHT : TEXT_MUTED);
            add(label);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                // Fire on release rather than click: mouseClicked is swallowed by the slightest pointer movement between
                // press and release, which makes the button feel unresponsive.
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        fire();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    label.setForeground(primary ? Color.WHITE : TEXT);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    label.setForeground(primary ? ACCENT_BRIGHT : TEXT_MUTED);
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);
        }

        private void addActionListener(ActionListener listener) {
            listeners.add(listener);
        }

        private void fire() {
            ActionEvent event = new ActionEvent(this,
                  ActionEvent.ACTION_PERFORMED, "clicked");
            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                if (primary) {
                    g2.setPaint(new GradientPaint(0, 0, translucent(ACCENT, hovered ? 66 : 40),
                          0, height, translucent(ACCENT, hovered ? 26 : 13)));
                    g2.fillRect(0, 0, width, height);
                    g2.setColor(BORDER_CYAN);
                } else {
                    g2.setColor(hovered ? SURFACE_HIGHLIGHT : SURFACE);
                    g2.fillRect(0, 0, width, height);
                    g2.setColor(BORDER);
                }
                g2.drawRect(0, 0, width - 1, height - 1);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
