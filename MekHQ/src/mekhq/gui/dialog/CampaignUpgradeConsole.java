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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.common.ui.AppIcons;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptionsFreebieTracker;
import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges;
import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges.SystemChange;
import mekhq.campaign.io.CampaignSaveBackup;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;
import mekhq.gui.campaignOptions.PresetBrowser;
import mekhq.gui.campaignOptions.PresetGuidePreferences;
import mekhq.gui.campaignOptions.PresetGuidePreferences.GuideScreen;
import mekhq.gui.utilities.WindowUtilities;
import org.apache.commons.text.StringEscapeUtils;

/**
 * The campaign upgrade console: the window shown when a campaign saved in an older release is loaded, drawn as a
 * heads-up display in the style of the Oracle Console.
 *
 * <p>The player picks one of three paths: apply a preset, keep their current settings, or review everything in
 * Campaign Options. The preset path lists every preset with its full description, what it replaces, and which major
 * systems it switches on or off (flagging those that open a follow-up dialog). A banner offers a one-click backup of
 * the save, and quitting takes a second click so it can't happen by accident.</p>
 *
 * <p>The console only collects the player's {@link Choice}; {@link CampaignUpgradeDialog} acts on it. The console can
 * be shown again (for example after the player cancels Campaign Options) and keeps its state between showings.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CampaignUpgradeConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(CampaignUpgradeConsole.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignUpgradeDialog";
    private static final String KEY = "CampaignUpgradeConsole.";
    private static final int TEXT_WIDTH = 720;

    /** The paths the player can take. */
    enum UpgradePath {
        PRESET, KEEP, REVIEW
    }

    /** What the player decided. */
    public enum Decision {
        APPLY_PRESET, KEEP_SETTINGS, REVIEW_BY_HAND, QUIT
    }

    /**
     * The player's decision.
     *
     * @param decision what to do
     * @param preset   the preset to apply, for {@link Decision#APPLY_PRESET}; otherwise {@code null}
     */
    public record Choice(Decision decision, @Nullable CampaignPreset preset) {}

    private enum BackupState {
        NONE, WORKING, DONE, OWN, FAILED
    }

    private final Campaign campaign;
    private final List<CampaignPreset> presets;
    private final @Nullable File saveFile;
    private final Version savedVersion;
    private final CampaignOptionsFreebieTracker currentSystems;
    private final Map<CampaignPreset, List<SystemChange>> changesByPreset = new HashMap<>();

    private @Nullable UpgradePath path;
    private @Nullable CampaignPreset selectedPreset;
    private PresetBrowser presetBrowser;
    private BackupState backupState = BackupState.NONE;
    private @Nullable File backupFile;
    private @Nullable String backupError;
    private Choice choice = new Choice(Decision.QUIT, null);

    private final HudSegmentedControl<UpgradePath> pathSelector;
    private final HudStatTile backupTile = new HudStatTile(text("tile.backup"), "");
    private final HudVerdictBanner backupBanner = new HudVerdictBanner();
    private final JPanel backupActions = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageLayout);
    private final JLabel summary = new JLabel();
    private final HudButton primaryButton = new HudButton("", true);
    private final HudConfirmButton quitButton = new HudConfirmButton(text("button.quit"),
          text("button.quit.confirm"), false);
    private final HudGuideDrawer guideDrawer;

    /**
     * Builds the console. Call on the Event Dispatch Thread.
     *
     * @param campaign the campaign being upgraded
     * @param presets  the presets on offer
     * @param saveFile the save the campaign was loaded from, or {@code null} if it isn't known
     */
    public CampaignUpgradeConsole(Campaign campaign, List<CampaignPreset> presets, @Nullable File saveFile) {
        // Shown while MekHQ's main window is still hidden, so give the dialog its own taskbar entry
        super((JFrame) null, text("windowTitle"), true);
        AppIcons.applyToWindow(this, "mekhq");
        this.campaign = campaign;
        this.presets = presets;
        this.saveFile = saveFile;
        this.savedVersion = campaign.getVersion();
        this.currentSystems = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());

        pathSelector = new HudSegmentedControl<>(this::choosePath);
        pathSelector.setSegments(List.of(
              new Segment<>(UpgradePath.PRESET, text("path.preset"), text("path.preset.sub"), true),
              new Segment<>(UpgradePath.KEEP, text("path.keep"), text("path.keep.sub"), true),
              new Segment<>(UpgradePath.REVIEW, text("path.review"), text("path.review.sub"), true)));
        pathSelector.setSelected(null);

        guideDrawer = PresetBrowser.createGuideDrawer(GuideScreen.UPGRADE, text("guide.alwaysOpen"),
              this::closeGuide);

        buildUI();
        refreshAll();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // Closing the window quits too, so it asks for the same second click as the quit button
                quitButton.click();
            }

            @Override
            public void windowOpened(WindowEvent event) {
                // Only the first showing; coming back from Campaign Options doesn't reopen the guide
                if (PresetGuidePreferences.getInstance().shouldOpenGuide(GuideScreen.UPGRADE)) {
                    openGuide();
                }
            }
        });
        setMinimumSize(scaleForGUI(980, 680));
        getContentPane().setPreferredSize(scaleForGUI(1120, 760));
        pack();
        WindowUtilities.fitToScreen(this, null);
    }

    /**
     * Shows the console and waits until the player decides. Call on the Event Dispatch Thread.
     *
     * @return the player's choice
     */
    public Choice showAndWait() {
        choice = new Choice(Decision.QUIT, null);
        quitButton.reset();
        refreshFooter();
        setVisible(true);
        return choice;
    }

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        guideDrawer.install(getRootPane());
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = Hud.commandBar(new BorderLayout(0, scaleForGUI(12)));
        JPanel identity = Hud.identity(text("eyebrow"), campaign.getPlayerForce().getName(), text("subtitle"));

        HudStatTile savedTile = new HudStatTile(text("tile.saved"), getFormattedTextAt(RESOURCE_BUNDLE,
              KEY + "tile.saved.sub", MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate())));
        savedTile.setValue(savedVersion.toString(), TEXT);
        HudStatTile runningTile = new HudStatTile(text("tile.running"), text("tile.running.sub"));
        runningTile.setValue(MHQConstants.VERSION.toString(), ACCENT_BRIGHT);
        JPanel tiles = Hud.tileRow(savedTile, runningTile, backupTile);

        JPanel topRow = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.setOpaque(false);
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(tiles, BorderLayout.EAST);

        HudButton guideButton = new HudButton(text("guide.open"), false, true);
        guideButton.addActionListener(event -> openGuide());
        JPanel pathRow = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        pathRow.setOpaque(false);
        pathRow.add(pathSelector, BorderLayout.CENTER);
        pathRow.add(Hud.topAligned(guideButton), BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(pathRow, BorderLayout.SOUTH);
        return commandBar;
    }

    private JPanel buildBody() {
        JPanel body = Hud.windowBody(new BorderLayout(0, scaleForGUI(12)));

        backupActions.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), 0, 0, 0));
        backupBanner.add(backupActions, BorderLayout.SOUTH);
        body.add(backupBanner, BorderLayout.NORTH);

        pageHolder.setOpaque(false);
        pageHolder.add(buildStartPage(), "START");
        pageHolder.add(buildPresetPage(), UpgradePath.PRESET.name());
        pageHolder.add(buildKeepPage(), UpgradePath.KEEP.name());
        pageHolder.add(buildReviewPage(), UpgradePath.REVIEW.name());
        body.add(pageHolder, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildStartPage() {
        JPanel page = Hud.transparentPanel(new BorderLayout());
        JLabel notice = Hud.notice(text("start"));
        notice.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(80), 0, 0, 0));
        page.add(notice, BorderLayout.NORTH);
        return page;
    }

    private JComponent buildPresetPage() {
        presetBrowser = new PresetBrowser(presets, this::selectPreset, null, this::addUpgradeDetail);
        return presetBrowser;
    }

    private JComponent buildKeepPage() {
        JPanel column = textColumn();
        column.add(leftAligned(pageTitle(text("keep.title"))));
        column.add(Box.createVerticalStrut(scaleForGUI(8)));
        column.add(leftAligned(wrapped(getFormattedTextAt(RESOURCE_BUNDLE, KEY + "keep.text", savedVersion,
              MHQConstants.VERSION), TEXT_MUTED)));
        column.add(Box.createVerticalStrut(scaleForGUI(16)));
        column.add(leftAligned(Hud.eyebrow(text("keep.kept"))));
        column.add(Box.createVerticalStrut(scaleForGUI(6)));
        column.add(leftAligned(Hud.chipRow(optionGroups(), READY)));
        column.add(Box.createVerticalStrut(scaleForGUI(14)));
        column.add(leftAligned(Hud.eyebrow(text("keep.defaults"))));
        column.add(Box.createVerticalStrut(scaleForGUI(6)));
        column.add(leftAligned(Hud.chipRow(List.of(getFormattedTextAt(RESOURCE_BUNDLE, KEY + "keep.defaults.chip",
              savedVersion)), TEXT_MUTED)));
        column.add(Box.createVerticalStrut(scaleForGUI(18)));
        HudVerdictBanner balance = new HudVerdictBanner();
        balance.setVerdict(text("keep.balance.verdict"), wrapHtml(text("keep.balance.reason"), TEXT_WIDTH - 160),
              text("keep.balance.badge"), CAUTION);
        column.add(leftAligned(balance));
        return pageOf(column);
    }

    private JComponent buildReviewPage() {
        JPanel column = textColumn();
        column.add(leftAligned(pageTitle(text("review.title"))));
        column.add(Box.createVerticalStrut(scaleForGUI(8)));
        column.add(leftAligned(wrapped(text("review.text"), TEXT_MUTED)));
        column.add(Box.createVerticalStrut(scaleForGUI(16)));
        JPanel flow = Hud.tileRow(flowStep("review.step.open"), flowStep("review.step.ok"),
              flowStep("review.step.cancel"));
        flow.setMaximumSize(new Dimension(scaleForGUI(TEXT_WIDTH), Integer.MAX_VALUE));
        column.add(leftAligned(flow));
        return pageOf(column);
    }

    private JComponent flowStep(String key) {
        JPanel step = new JPanel();
        step.setLayout(new BoxLayout(step, BoxLayout.Y_AXIS));
        step.setOpaque(true);
        step.setBackground(SURFACE);
        step.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12)));
        JLabel title = new JLabel(text(key).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT);
        title.setFont(hudFont(Font.BOLD, 0.78f, 0.12f));
        step.add(leftAligned(title));
        step.add(Box.createVerticalStrut(scaleForGUI(3)));
        step.add(leftAligned(wrapped(text(key + ".text"), TEXT_MUTED)));
        return step;
    }

    private JPanel buildFooter() {
        quitButton.setToolTipText(text("button.quit.toolTipText"));
        quitButton.addActionListener(event -> decide(new Choice(Decision.QUIT, null)));
        primaryButton.addActionListener(event -> onPrimary());
        return Hud.footer(summary, quitButton, primaryButton);
    }

    // endregion Layout

    // region Behaviour

    private void choosePath(UpgradePath chosen) {
        path = chosen;
        refreshAll();
    }

    private void selectPreset(CampaignPreset preset) {
        selectedPreset = preset;
        refreshFooter();
    }

    private void onPrimary() {
        if ((path == null) || !primaryButton.isArmed()) {
            return;
        }
        switch (path) {
            case PRESET -> decide(new Choice(Decision.APPLY_PRESET, selectedPreset));
            case KEEP -> decide(new Choice(Decision.KEEP_SETTINGS, null));
            case REVIEW -> decide(new Choice(Decision.REVIEW_BY_HAND, null));
        }
    }

    private void decide(Choice decided) {
        choice = decided;
        closeGuide();
        setVisible(false);
    }

    private void backUp() {
        if (saveFile == null || backupState == BackupState.WORKING) {
            return;
        }
        backupState = BackupState.WORKING;
        refreshBackup();
        refreshFooter();
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws IOException {
                return CampaignSaveBackup.createBackup(saveFile, savedVersion);
            }

            @Override
            protected void done() {
                try {
                    backupFile = get();
                    backupState = BackupState.DONE;
                    LOGGER.info("Backed up {} to {} before upgrading", saveFile, backupFile);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    backupError = exception.getMessage();
                    backupState = BackupState.FAILED;
                } catch (ExecutionException exception) {
                    LOGGER.error("Failed to back up {}", saveFile, exception.getCause());
                    backupError = (exception.getCause() == null) ? exception.getMessage()
                                        : exception.getCause().getMessage();
                    backupState = BackupState.FAILED;
                }
                refreshBackup();
                refreshFooter();
            }
        }.execute();
    }

    private void showBackupFolder() {
        if (backupFile == null) {
            return;
        }
        try {
            Desktop.getDesktop().open(backupFile.getAbsoluteFile().getParentFile());
        } catch (Exception exception) {
            LOGGER.warn("Couldn't open the folder of {}", backupFile, exception);
        }
    }

    private void openGuide() {
        PresetGuidePreferences.getInstance().setGuideSeen(GuideScreen.UPGRADE, true);
        guideDrawer.open(PresetBrowser.buildGuide(RESOURCE_BUNDLE, KEY));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    // endregion Behaviour

    // region Refresh

    private void refreshAll() {
        pathSelector.setSelected(path);
        pageLayout.show(pageHolder, (path == null) ? "START" : path.name());
        refreshBackup();
        refreshFooter();
    }

    /**
     * Adds what applying a preset would change in this campaign to the preset's details.
     */
    private void addUpgradeDetail(JPanel column, CampaignPreset preset) {
        column.add(leftAligned(Hud.eyebrow(text("detail.replaced"))));
        column.add(Box.createVerticalStrut(scaleForGUI(6)));
        column.add(leftAligned(Hud.chipRow(replacedGroups(preset), CAUTION)));
        column.add(Box.createVerticalStrut(scaleForGUI(14)));
        column.add(leftAligned(Hud.eyebrow(text("detail.notReplaced"))));
        column.add(Box.createVerticalStrut(scaleForGUI(6)));
        column.add(leftAligned(Hud.chipRow(List.of(text("group.personnel"), text("group.units"),
              text("group.finances"), text("group.contracts"), text("group.journal")), READY)));
        column.add(Box.createVerticalStrut(scaleForGUI(16)));

        column.add(leftAligned(Hud.eyebrow(text("detail.systems"))));
        column.add(Box.createVerticalStrut(scaleForGUI(6)));
        List<SystemChange> changes = changesFor(preset);
        if (changes.isEmpty()) {
            column.add(leftAligned(Hud.hint(text("detail.systems.none"))));
        } else {
            JPanel rows = new JPanel();
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            rows.setOpaque(true);
            rows.setBackground(SURFACE);
            rows.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
            for (SystemChange change : changes) {
                rows.add(leftAligned(systemRow(change)));
            }
            column.add(leftAligned(rows));
            column.add(Box.createVerticalStrut(scaleForGUI(6)));
            column.add(leftAligned(wrapped(text("detail.systems.note"), TEXT_FAINT)));
        }
    }

    private JComponent systemRow(SystemChange change) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(10), scaleForGUI(6), scaleForGUI(10))));
        JLabel symbol = new JLabel(change.isNowOn() ? "+" : "-", SwingConstants.CENTER);
        symbol.setForeground(change.isNowOn() ? READY : TEXT_FAINT);
        symbol.setFont(hudFont(Font.BOLD, 1.1f, 0.0f));
        symbol.setPreferredSize(scaleForGUI(16, 16));
        String systemName = PresetBrowser.systemName(change.system());
        JLabel label = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY + (change.isNowOn() ? "detail.systems.on" : "detail.systems.off"), systemName));
        label.setForeground(TEXT);
        label.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        row.add(symbol, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        if (change.hasFollowUp()) {
            JLabel followUp = new JLabel(text("detail.systems.followUp").toUpperCase(Locale.ROOT));
            followUp.setForeground(AMBER);
            followUp.setFont(hudFont(Font.BOLD, 0.7f, 0.12f));
            followUp.setToolTipText(text("detail.systems.followUp.toolTipText"));
            row.add(followUp, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void refreshBackup() {
        backupActions.removeAll();
        switch (backupState) {
            case NONE -> {
                backupTile.setValue(text("tile.backup.none"), AMBER);
                backupTile.setSub(text("tile.backup.none.sub"));
                if (saveFile == null) {
                    showBackupVerdict(text("backup.none.verdict"), text("backup.noFile.reason"),
                          text("backup.none.badge"), CAUTION);
                } else {
                    showBackupVerdict(text("backup.none.verdict"), getFormattedTextAt(RESOURCE_BUNDLE,
                          KEY + "backup.none.reason", StringEscapeUtils.escapeHtml4(saveFile.getName())),
                          text("backup.none.badge"), CAUTION);
                    HudButton backUpButton = new HudButton(text("backup.button"), true, true);
                    backUpButton.addActionListener(event -> backUp());
                    backupActions.add(backUpButton);
                    backupActions.add(Box.createHorizontalStrut(scaleForGUI(16)));
                }
                backupActions.add(Hud.link(text("backup.own"), () -> {
                    backupState = BackupState.OWN;
                    refreshBackup();
                    refreshFooter();
                }));
            }
            case WORKING -> showBackupVerdict(text("backup.working.verdict"), text("backup.working.reason"),
                  text("backup.working.badge"), ACCENT);
            case DONE -> {
                backupTile.setValue(text("tile.backup.done"), READY);
                backupTile.setSub(text("tile.backup.done.sub"));
                File file = (backupFile == null) ? new File("") : backupFile;
                File folder = file.getAbsoluteFile().getParentFile();
                showBackupVerdict(text("backup.done.verdict"), getFormattedTextAt(RESOURCE_BUNDLE,
                      KEY + "backup.done.reason", StringEscapeUtils.escapeHtml4(file.getName()),
                      StringEscapeUtils.escapeHtml4((folder == null) ? "" : folder.getPath())),
                      text("backup.done.badge"), READY);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    backupActions.add(Hud.link(text("backup.showFolder"), this::showBackupFolder));
                }
            }
            case OWN -> {
                backupTile.setValue(text("tile.backup.own"), TEXT);
                backupTile.setSub(text("tile.backup.own.sub"));
                showBackupVerdict(text("backup.own.verdict"), text("backup.own.reason"),
                      text("backup.own.badge"), INELIGIBLE);
                if (saveFile != null) {
                    backupActions.add(Hud.link(text("backup.anyway"), this::backUp));
                }
            }
            case FAILED -> {
                backupTile.setValue(text("tile.backup.failed"), DANGER);
                backupTile.setSub(text("tile.backup.none.sub"));
                showBackupVerdict(text("backup.failed.verdict"), getFormattedTextAt(RESOURCE_BUNDLE,
                      KEY + "backup.failed.reason", StringEscapeUtils.escapeHtml4(String.valueOf(backupError))),
                      text("backup.failed.badge"), DANGER);
                HudButton retry = new HudButton(text("backup.retry"), true, true);
                retry.addActionListener(event -> backUp());
                backupActions.add(retry);
                backupActions.add(Box.createHorizontalStrut(scaleForGUI(16)));
                backupActions.add(Hud.link(text("backup.own"), () -> {
                    backupState = BackupState.OWN;
                    refreshBackup();
                    refreshFooter();
                }));
            }
        }
        backupActions.setVisible(backupActions.getComponentCount() > 0);
        backupActions.revalidate();
        backupBanner.revalidate();
        backupBanner.repaint();
    }

    private void showBackupVerdict(String verdict, String reason, String badge, Color accent) {
        backupBanner.setVerdict(verdict, "<html>" + reason + "</html>", badge, accent);
    }

    private void refreshFooter() {
        String message;
        boolean isReady = true;
        if (path == null) {
            message = text("summary.start");
            primaryButton.setText(text("button.choose"));
            isReady = false;
        } else {
            switch (path) {
                case PRESET -> {
                    if (selectedPreset == null) {
                        message = text("summary.preset.none");
                        primaryButton.setText(text("button.choose"));
                        isReady = false;
                    } else {
                        int followUps = 0;
                        for (SystemChange change : changesFor(selectedPreset)) {
                            if (change.hasFollowUp()) {
                                followUps++;
                            }
                        }
                        message = getFormattedTextAt(RESOURCE_BUNDLE, KEY + "summary.preset",
                              StringEscapeUtils.escapeHtml4(PresetBrowser.displayTitle(selectedPreset)));
                        if (followUps > 0) {
                            message += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, KEY + "summary.preset.followUps",
                                  followUps);
                        }
                        primaryButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, KEY + "button.apply",
                              PresetBrowser.displayTitle(selectedPreset)));
                    }
                }
                case KEEP -> {
                    message = getFormattedTextAt(RESOURCE_BUNDLE, KEY + "summary.keep", MHQConstants.VERSION);
                    primaryButton.setText(text("button.keep"));
                }
                case REVIEW -> {
                    message = text("summary.review");
                    primaryButton.setText(text("button.review"));
                }
                default -> throw new IllegalStateException("Unexpected path: " + path);
            }
        }
        switch (backupState) {
            case NONE, FAILED -> message += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, KEY + "summary.noBackup",
                  hex(AMBER));
            case WORKING -> {
                // Wait for the copy, so the save isn't changed while it is still being read
                message += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, KEY + "summary.backupWorking", hex(AMBER));
                isReady = false;
            }
            case DONE, OWN -> {
                // Nothing to add
            }
        }
        summary.setText("<html>" + message + "</html>");
        primaryButton.setArmed(isReady);
    }

    // endregion Refresh

    // region Presets

    private List<SystemChange> changesFor(CampaignPreset preset) {
        return changesByPreset.computeIfAbsent(preset, key -> (key.getCampaignOptions() == null) ? List.of()
              : CampaignOptionsSystemChanges.between(currentSystems,
                    new CampaignOptionsFreebieTracker(key.getCampaignOptions())));
    }

    private static List<String> optionGroups() {
        return List.of(text("group.campaignOptions"), text("group.gameOptions"), text("group.skills"),
              text("group.abilities"), text("group.randomSkills"));
    }

    private static List<String> replacedGroups(CampaignPreset preset) {
        List<String> groups = new ArrayList<>();
        if (preset.getCampaignOptions() != null) {
            groups.add(text("group.campaignOptions"));
        }
        if (preset.getGameOptions() != null) {
            groups.add(text("group.gameOptions"));
        }
        groups.add(text("group.skills"));
        groups.add(text("group.abilities"));
        if (preset.getRandomSkillPreferences() != null) {
            groups.add(text("group.randomSkills"));
        }
        return groups;
    }

    // endregion Presets

    // region Helpers

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }

    private static String wrapHtml(String html, int width) {
        return "<html><div style='width:" + scaleForGUI(width) + "px'>" + html + "</div></html>";
    }

    private static JLabel wrapped(String html, Color color) {
        return Hud.wrappingHtml(html, color, hudFont(Font.PLAIN, 0.92f, 0.0f));
    }

    private static JLabel pageTitle(String title) {
        JLabel label = new JLabel(title.toUpperCase(Locale.ROOT));
        label.setForeground(ACCENT_BRIGHT);
        label.setFont(hudFont(Font.BOLD, 1.2f, 0.14f));
        return label;
    }

    private static JPanel textColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(true);
        column.setBackground(SURFACE_DEEP);
        column.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(18), scaleForGUI(20), scaleForGUI(20),
              scaleForGUI(20)));
        return column;
    }

    private static JComponent pageOf(JPanel column) {
        // Takes the viewport's width, so the text wraps instead of running off to the side
        JPanel framed = new JScrollablePanel();
        framed.setLayout(new BorderLayout());
        framed.setOpaque(true);
        framed.setBackground(SURFACE_DEEP);
        framed.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        framed.add(column, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(framed);
        Hud.styleScroll(scroll, GROUND, false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    // endregion Helpers
}
