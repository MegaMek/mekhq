/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.common.ui.AppIcons;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.campaign.campaignOptions.CampaignOptionsFreebieTracker;
import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges.TrackedSystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.campaignOptions.PresetGuidePreferences.GuideScreen;
import mekhq.gui.utilities.WindowUtilities;
import org.apache.commons.text.StringEscapeUtils;

/**
 * The preset picker: a heads-up display, in the style of the campaign upgrade console and the Oracle Console, for
 * choosing a campaign preset.
 *
 * <p>It is used in two places. When a new campaign is started it offers "Start" (which then asks only for the
 * essentials, such as the start date and faction) and "Customize" (which shows every option first), and its guide
 * opens by itself the first time. Inside Campaign Options it loads a preset into the options being edited.</p>
 *
 * <p>The list reads only each preset's title and description, so it opens quickly. The full preset is read when the
 * player selects it, which also lets the details show which major systems the preset switches on.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class CampaignOptionsPresetPicker extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsPresetPicker";
    private static final String KEY = "CampaignOptionsPresetPicker.";
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsPresetPicker.class);

    /**
     * Preset selection results.
     */
    private enum PresetSelection {
        CANCELLED, APPLY, CUSTOMIZE
    }

    private final boolean isStartup;
    private final Map<CampaignPreset, CampaignPreset> fullPresets = new HashMap<>();
    private final PresetBrowser browser;
    private final JLabel summary = new JLabel();
    private final HudButton primaryButton = new HudButton("", true);
    private final @Nullable HudButton customizeButton;
    private final HudGuideDrawer guideDrawer;

    private PresetSelection returnState = PresetSelection.CANCELLED;
    private @Nullable CampaignPreset selectedPreset;

    /**
     * Indicates if the user canceled the dialog.
     *
     * @return {@code true} if canceled, {@code false} otherwise
     */
    public boolean wasCanceled() {
        return returnState == PresetSelection.CANCELLED;
    }

    /**
     * Indicates if the user chose to use the preset as it is, rather than customize it.
     *
     * @return {@code true} if 'apply' was selected, {@code false} otherwise
     */
    public boolean wasApplied() {
        return returnState == PresetSelection.APPLY;
    }

    /**
     * Gets the selected campaign preset, fully loaded.
     *
     * @return the selected {@link CampaignPreset}, or {@code null} if none was chosen
     */
    public @Nullable CampaignPreset getSelectedPreset() {
        return selectedPreset;
    }

    /**
     * Constructs and displays the campaign options preset picker dialog, returning once the player has chosen.
     *
     * <p>If {@code parentFrame} is showing, the dialog is owned by it. Otherwise, such as while a new campaign is
     * being created and the startup screen is hidden, the dialog has no owner window and carries the MekHQ icon,
     * because Windows leaves windows owned by a hidden frame out of the taskbar and Alt+Tab.</p>
     *
     * @param parentFrame                  The parent {@link JFrame} for modal display, or {@code null}.
     * @param includeCustomizePresetOption {@code true} when starting a new campaign: offers "Customize" and the new
     *                                     campaign guide. {@code false} when loading a preset into Campaign Options.
     */
    public CampaignOptionsPresetPicker(@Nullable JFrame parentFrame, boolean includeCustomizePresetOption) {
        super(resolveOwner(parentFrame), ModalityType.APPLICATION_MODAL);
        if (getOwner() == null) {
            AppIcons.applyToWindow(this, "mekhq");
        }
        // With no owner window to clean it up, closing the window must release it like the Cancel button does
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.isStartup = includeCustomizePresetOption;
        setTitle(text("title"));

        List<CampaignPreset> presets = CampaignPreset.getCampaignPresetsMetadata();
        if (presets.isEmpty()) {
            LOGGER.errorDialog("Error", "No campaign presets found");
        }

        browser = new PresetBrowser(presets, preset -> refreshFooter(), this::activate, this::addSystemsDetail);
        customizeButton = isStartup ? new HudButton(text("button.customize"), false) : null;
        guideDrawer = PresetBrowser.createGuideDrawer(GuideScreen.NEW_CAMPAIGN, text("guide.alwaysOpen"),
              this::closeGuide);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);
        JPanel body = Hud.windowBody(new BorderLayout());
        body.add(browser, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        guideDrawer.install(getRootPane());

        if (!presets.isEmpty()) {
            browser.setSelected(presets.get(0));
        }
        refreshFooter();

        if (isStartup) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent event) {
                    if (PresetGuidePreferences.getInstance().shouldOpenGuide(GuideScreen.NEW_CAMPAIGN)) {
                        openGuide();
                    }
                }
            });
        }

        setMinimumSize(scaleForGUI(900, 600));
        getContentPane().setPreferredSize(scaleForGUI(1060, 720));
        pack();
        WindowUtilities.fitToScreen(this, parentFrame);
        setVisible(true);
    }

    // region Layout

    private JPanel buildCommandBar() {
        JPanel commandBar = Hud.commandBar(new BorderLayout(scaleForGUI(24), 0));
        commandBar.add(Hud.identity(text(isStartup ? "eyebrow.startup" : "eyebrow.load"), text("heading"),
              text(isStartup ? "subtitle.startup" : "subtitle.load")), BorderLayout.CENTER);

        // The guide teaches starting a campaign, so it isn't offered when loading a preset into Campaign Options
        if (isStartup) {
            HudButton guideButton = new HudButton(text("guide.open"), false, true);
            guideButton.addActionListener(event -> openGuide());
            commandBar.add(Hud.topAligned(guideButton), BorderLayout.EAST);
        }
        return commandBar;
    }

    private JPanel buildFooter() {
        HudButton cancelButton = new HudButton(text("button.cancel"), false);
        cancelButton.addActionListener(event -> {
            returnState = PresetSelection.CANCELLED;
            dispose();
        });
        primaryButton.addActionListener(event -> commitSelection(PresetSelection.APPLY));

        if (customizeButton == null) {
            return Hud.footer(summary, cancelButton, primaryButton);
        }
        customizeButton.setToolTipText(text("button.customize.toolTipText"));
        customizeButton.addActionListener(event -> commitSelection(PresetSelection.CUSTOMIZE));
        return Hud.footer(summary, cancelButton, customizeButton, primaryButton);
    }

    /**
     * Adds the preset's major systems, on and off, to its details.
     */
    private void addSystemsDetail(JPanel column, CampaignPreset preset) {
        CampaignPreset fullPreset = fullPresetFor(preset);
        if (fullPreset == null) {
            column.add(leftAligned(Hud.notice(text("detail.loadFailed"))));
            return;
        }
        if (fullPreset.getCampaignOptions() == null) {
            return;
        }
        CampaignOptionsFreebieTracker systems = new CampaignOptionsFreebieTracker(fullPreset.getCampaignOptions());
        List<String> on = new ArrayList<>();
        List<String> off = new ArrayList<>();
        for (TrackedSystem system : TrackedSystem.values()) {
            (system.isOn(systems) ? on : off).add(PresetBrowser.systemName(system));
        }
        if (!on.isEmpty()) {
            column.add(leftAligned(Hud.eyebrow(text("detail.systemsOn"))));
            column.add(Box.createVerticalStrut(scaleForGUI(6)));
            column.add(leftAligned(Hud.chipRow(on, READY)));
            column.add(Box.createVerticalStrut(scaleForGUI(14)));
        }
        if (!off.isEmpty()) {
            column.add(leftAligned(Hud.eyebrow(text("detail.systemsOff"))));
            column.add(Box.createVerticalStrut(scaleForGUI(6)));
            column.add(leftAligned(Hud.chipRow(off, TEXT_FAINT)));
            column.add(Box.createVerticalStrut(scaleForGUI(14)));
        }
        column.add(leftAligned(Hud.hint(text("detail.systemsNote"))));
    }

    // endregion Layout

    // region Behaviour

    private void activate(CampaignPreset preset) {
        commitSelection(PresetSelection.APPLY);
    }

    /**
     * Records the chosen outcome with the fully loaded preset and closes the dialog. If the preset can't be loaded, the
     * dialog stays open and says so, so the action never completes with no selection.
     *
     * @param selection the outcome to record
     */
    private void commitSelection(PresetSelection selection) {
        CampaignPreset chosen = browser.getSelected();
        if (chosen == null) {
            return;
        }
        CampaignPreset preset = fullPresetFor(chosen);
        if (preset == null) {
            JOptionPane.showMessageDialog(this, text("loadFailed.message"), text("loadFailed.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedPreset = preset;
        returnState = selection;
        closeGuide();
        dispose();
    }

    private void openGuide() {
        PresetGuidePreferences.getInstance().setGuideSeen(GuideScreen.NEW_CAMPAIGN, true);
        guideDrawer.open(PresetBrowser.buildGuide(RESOURCE_BUNDLE, KEY));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    private void refreshFooter() {
        CampaignPreset preset = browser.getSelected();
        boolean isLoadable = (preset != null) && (fullPresetFor(preset) != null);
        if (preset == null) {
            summary.setText(text("summary.none"));
            primaryButton.setText(text("button.choose"));
        } else {
            String name = PresetBrowser.displayTitle(preset);
            summary.setText("<html>" + getFormattedTextAt(RESOURCE_BUNDLE,
                  KEY + (isStartup ? "summary.startup" : "summary.load"), StringEscapeUtils.escapeHtml4(name))
                                 + "</html>");
            primaryButton.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  KEY + (isStartup ? "button.start" : "button.load"), name));
        }
        primaryButton.setArmed(isLoadable);
        if (customizeButton != null) {
            customizeButton.setArmed(isLoadable);
        }
    }

    // endregion Behaviour

    // region Presets

    /**
     * Fully loads the campaign preset behind a lightweight list entry, once, and keeps it for later.
     *
     * <p>For speed, the picker only parses preset titles and descriptions up front (see
     * {@link CampaignPreset#getCampaignPresetsMetadata()}). The complete preset - campaign options, game options,
     * skills, and so on - is read from disk the first time the player selects it.</p>
     *
     * @param metadataPreset the lightweight preset from the list
     *
     * @return the fully parsed {@link CampaignPreset}, or {@code null} if it could not be loaded
     */
    private @Nullable CampaignPreset fullPresetFor(CampaignPreset metadataPreset) {
        if (fullPresets.containsKey(metadataPreset)) {
            return fullPresets.get(metadataPreset);
        }
        CampaignPreset fullPreset = null;
        File presetFile = metadataPreset.getPresetFile();
        if (presetFile == null) {
            // Every preset offered by the picker is read from a file, so a missing file is unexpected.
            LOGGER.error("Selected campaign preset '{}' has no source file and cannot be loaded.", metadataPreset);
        } else {
            fullPreset = CampaignPreset.parseFromFile(presetFile);
            if (fullPreset == null) {
                LOGGER.error("Failed to fully load campaign preset from {}.", presetFile);
            }
        }
        fullPresets.put(metadataPreset, fullPreset);
        return fullPreset;
    }

    // endregion Presets

    // region Window

    /**
     * Picks the owner window for this dialog: the parent frame when it is showing, otherwise none. An explicit
     * {@code null} owner is needed because Swing attaches dialogs created with a {@code null} frame to a hidden shared
     * frame, which leaves them out of the Windows taskbar.
     *
     * @param parentFrame the parent frame, or {@code null}
     *
     * @return the parent frame if it is showing, otherwise {@code null}
     */
    private static @Nullable Window resolveOwner(@Nullable JFrame parentFrame) {
        return ((parentFrame != null) && parentFrame.isShowing()) ? parentFrame : null;
    }

    // endregion Window

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }
}
