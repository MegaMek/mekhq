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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.setSmallSizeVariant;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code ReputationPage} class builds and manages the Reputation leaf page of the Campaign Options dialog. It owns
 * the widgets for reputation configuration - the manual unit rating modifier, the criminal-record reset button, and the
 * reputation "sanity" toggles - and synchronises them with a shared {@link SystemsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link SystemsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code SystemsPages}, while this class is responsible only for constructing the Reputation panel and copying
 * reputation values to and from the model. Unlike the other leaf pages it keeps a reference to the current model so the
 * reset-criminal-record button can flag a pending reset and refresh its own display; the actual criminal-record reset
 * is performed by {@code SystemsPages} during apply. The page is built lazily; until
 * {@link #createPanel(SystemsOptionsModel)} is called, {@link #readFromModel(SystemsOptionsModel)} and
 * {@link #writeToModel(SystemsOptionsModel)} are no-ops.</p>
 */
class ReputationPage {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private CampaignOptionsHeaderPanel reputationHeader;

    private JButton btnResetCriminalRecord;

    private JSpinner manualUnitRatingModifier;
    private JCheckBox chkRequireSupportForceTransportation;
    private JCheckBox chkClampReputationPayMultiplier;
    private JCheckBox chkReduceReputationPerformanceModifier;
    private JCheckBox chkReputationPerformanceModifierCutOff;

    private SystemsOptionsModel model;
    private boolean created;

    /**
     * Creates the Reputation page panel, containing grouped UI elements for reputation options and its header.
     *
     * @param model the shared systems options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} component representing the entire Reputation page UI
     */
    @Nonnull JPanel createPanel(@Nullable SystemsOptionsModel model) {
        this.model = model;

        // Header
        String imageAddress = getImageDirectory() + "logo_morgrains_valkyrate.png";
        reputationHeader = new CampaignOptionsHeaderPanel("ReputationPage", imageAddress);

        // Contents
        JPanel pnlReputationGeneralOptions = createReputationGeneralPanel();
        JPanel pnlReputationSanityOptions = createReputationSanityPanel();

        // Layout the Panel
        final JPanel panel = CampaignOptionsPagePanel.builder("ReputationPage", "ReputationPage", imageAddress)
                .header(reputationHeader)
                .quote("reputationPage")
                .section("lblReputationGeneralOptionsPanel.text",
                        "lblReputationGeneralOptionsPanel.summary",
                        pnlReputationGeneralOptions)
                .section("lblReputationSanityOptionsPanel.text",
                        "lblReputationSanityOptionsPanel.summary",
                        pnlReputationSanityOptions)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and lays out the general reputation options panel, including controls for selecting unit rating method,
     * manual modifiers, and criminal record reset.
     *
     * @return a {@link JPanel} containing the general reputation controls
     */
    private @Nonnull JPanel createReputationGeneralPanel() {
        // Contents
        JLabel lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        lblManualUnitRatingModifier.addMouseListener(createTipPanelUpdater("ManualUnitRatingModifier"));
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier", 0, -1000, 1000, 1);
        manualUnitRatingModifier
                .addMouseListener(createTipPanelUpdater("ManualUnitRatingModifier"));

        JLabel lblResetCriminalRecord = new CampaignOptionsLabel("ResetCriminalRecord",
                getResetCriminalRecordMetadata());
        lblResetCriminalRecord.addMouseListener(createTipPanelUpdater("ResetCriminalRecord"));
        btnResetCriminalRecord = createResetCriminalRecordButton();

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ReputationGeneralOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblManualUnitRatingModifier, manualUnitRatingModifier);
        panel.addRow(lblResetCriminalRecord, createLeftAlignedButtonPanel(btnResetCriminalRecord));

        return panel;
    }

    private JButton createResetCriminalRecordButton() {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "btnResetCriminalRecord.text"));
        button.setName("btnResetCriminalRecord");
        button.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblResetCriminalRecord.tooltip"));
        setSmallSizeVariant(button);
        button.addMouseListener(createTipPanelUpdater("ResetCriminalRecord"));
        button.addActionListener(event -> {
            if (model != null) {
                model.resetCriminalRecord = true;
            }
            updateResetCriminalRecordButtonFromModel();
        });
        return button;
    }

    private @Nonnull JPanel createLeftAlignedButtonPanel(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(button);
        return panel;
    }

    /**
     * Creates and lays out the reputation "sanity" options panel, which includes various checkboxes for limiting or
     * modifying reputation calculations.
     *
     * @return a {@link JPanel} containing the reputation sanity option controls
     */
    private @Nonnull JPanel createReputationSanityPanel() {
        // Contents
        chkRequireSupportForceTransportation = new CampaignOptionsCheckBox("RequireSupportForceTransportation",
                getMetadata(new Version(0, 51, 0)));
        chkRequireSupportForceTransportation.addMouseListener(createTipPanelUpdater("RequireSupportForceTransportation"));

        chkClampReputationPayMultiplier = new CampaignOptionsCheckBox("ClampReputationPayMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkClampReputationPayMultiplier.addMouseListener(createTipPanelUpdater("ClampReputationPayMultiplier"));

        chkReduceReputationPerformanceModifier = new CampaignOptionsCheckBox("ReduceReputationPerformanceModifier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkReduceReputationPerformanceModifier.addMouseListener(createTipPanelUpdater("ReduceReputationPerformanceModifier"));

        chkReputationPerformanceModifierCutOff = new CampaignOptionsCheckBox("ReputationPerformanceModifierCutOff",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkReputationPerformanceModifierCutOff.addMouseListener(createTipPanelUpdater("ReputationPerformanceModifierCutOff"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ReputationSanityOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkClampReputationPayMultiplier,
                chkRequireSupportForceTransportation,
                chkReduceReputationPerformanceModifier,
                chkReputationPerformanceModifierCutOff);

        return panel;
    }

    private CampaignOptionsMetadata getResetCriminalRecordMetadata() {
        return getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT);
    }

    /**
     * Refreshes the reset-criminal-record button's text and enabled state to reflect whether a criminal-record reset is
     * currently pending in the shared model. Safe to call before the page has been built (no-op until the button
     * exists).
     */
    void updateResetCriminalRecordButtonFromModel() {
        if (btnResetCriminalRecord == null) {
            return;
        }

        boolean isResetPending = model != null && model.resetCriminalRecord;
        String resourceKey = isResetPending ? "btnResetCriminalRecord.pending.text" :
                "btnResetCriminalRecord.text";
        btnResetCriminalRecord.setText(getTextAt(getCampaignOptionsResourceBundle(), resourceKey));
        btnResetCriminalRecord.setEnabled(!isResetPending);
    }

    /**
     * Copies reputation values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared systems options model to read values from
     */
    void readFromModel(@Nullable SystemsOptionsModel model) {
        this.model = model;
        if (!created || model == null) {
            return;
        }

        manualUnitRatingModifier.setValue(model.manualUnitRatingModifier);
        updateResetCriminalRecordButtonFromModel();
        chkRequireSupportForceTransportation.setSelected(model.requireSupportForceTransportation);
        chkClampReputationPayMultiplier.setSelected(model.clampReputationPayMultiplier);
        chkReduceReputationPerformanceModifier.setSelected(model.reduceReputationPerformanceModifier);
        chkReputationPerformanceModifierCutOff.setSelected(model.reputationPerformanceModifierCutOff);
    }

    /**
     * Copies reputation values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared systems options model to write values into
     */
    void writeToModel(@Nullable SystemsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.manualUnitRatingModifier = (int) manualUnitRatingModifier.getValue();
        model.requireSupportForceTransportation = chkRequireSupportForceTransportation.isSelected();
        model.clampReputationPayMultiplier = chkClampReputationPayMultiplier.isSelected();
        model.reduceReputationPerformanceModifier = chkReduceReputationPerformanceModifier.isSelected();
        model.reputationPerformanceModifierCutOff = chkReputationPerformanceModifierCutOff.isSelected();
    }
}
