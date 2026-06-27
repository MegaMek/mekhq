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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code FactionStandingPage} class builds and manages the Faction Standing leaf page of the Campaign Options
 * dialog. It owns the widgets for Faction Standing configuration - the tracking toggles, the regard multiplier, and the
 * per-effect modifier toggles - and synchronises them with a shared {@link SystemsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link SystemsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code SystemsPages}, while this class is responsible only for constructing the Faction Standing panel and
 * copying Faction Standing values to and from the model. The page is built lazily; until
 * {@link #createPanel(SystemsOptionsModel)} is called, {@link #readFromModel(SystemsOptionsModel)} and
 * {@link #writeToModel(SystemsOptionsModel)} are no-ops.</p>
 */
class FactionStandingPage {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private CampaignOptionsHeaderPanel factionStandingHeader;
    private JCheckBox chkTrackFactionStanding;
    private JCheckBox chkTrackClimateRegardChanges;
    private JSpinner spnRegardMultiplier;

    private JCheckBox chkUseFactionStandingNegotiation;
    private JCheckBox chkUseFactionStandingResupply;
    private JCheckBox chkUseFactionStandingCommandCircuit;
    private JCheckBox chkUseFactionStandingOutlawed;
    private JCheckBox chkUseFactionStandingBatchallRestrictions;
    private JCheckBox chkUseFactionStandingRecruitment;
    private JCheckBox chkUseFactionStandingBarracksCosts;
    private JCheckBox chkUseFactionStandingUnitMarket;
    private JCheckBox chkUseFactionStandingContractPay;
    private JCheckBox chkUseFactionStandingSupportPoints;

    private boolean created;

    /**
     * Creates the Faction Standing page panel, containing grouped UI elements for Faction Standing options and its
     * header.
     *
     * @param model the shared systems options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} component representing the entire Faction Standing page UI
     */
    @Nonnull JPanel createPanel(@Nullable SystemsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_morgrains_valkyrate.png";
        factionStandingHeader = new CampaignOptionsHeaderPanel("FactionStandingPage", imageAddress);

        // Contents
        JPanel pnlFactionStandingTrackingPanel = createFactionStandingTrackingPanel();
        JPanel pnlFactionStandingModifiersPanel = createFactionStandingModifiersPanel();

        // Layout the Panel
        final JPanel panel = CampaignOptionsPagePanel.builder("FactionStandingPage", "FactionStandingPage",
                        imageAddress)
                .header(factionStandingHeader)
                .quote("factionStandingPage")
                .section("lblFactionStandingTrackingPanel.text",
                        "lblFactionStandingTrackingPanel.summary",
                        pnlFactionStandingTrackingPanel)
                .section("lblFactionStandingEffectsPanel.text",
                        "lblFactionStandingEffectsPanel.summary",
                        pnlFactionStandingModifiersPanel)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createFactionStandingTrackingPanel() {
        // Contents
        chkTrackFactionStanding = new CampaignOptionsCheckBox("TrackFactionStanding",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.DOCUMENTED));
        chkTrackFactionStanding
                .addMouseListener(createTipPanelUpdater("TrackFactionStanding"));

        chkTrackClimateRegardChanges = new CampaignOptionsCheckBox("TrackClimateRegardChanges",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkTrackClimateRegardChanges.addMouseListener(createTipPanelUpdater("TrackClimateRegardChanges"));

        JLabel lblRegardMultiplier = new CampaignOptionsLabel("RegardMultiplier",
                getMetadata(MILESTONE_BEFORE_METADATA));
        lblRegardMultiplier.addMouseListener(createTipPanelUpdater("RegardMultiplier"));
        spnRegardMultiplier = new CampaignOptionsSpinner("RegardMultiplier", 1.0, 0.1, 3.0, 0.1);
        spnRegardMultiplier.addMouseListener(createTipPanelUpdater("RegardMultiplier"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FactionStandingTrackingPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkTrackFactionStanding,
                chkTrackClimateRegardChanges);
        panel.addRow(lblRegardMultiplier, spnRegardMultiplier);

        return panel;
    }

    /**
     * Creates and lays out the Faction Standing modifiers panel, which includes various checkboxes for limiting Faction
     * Standing modifiers.
     *
     * @return a {@link JPanel} containing the Faction Standing modifier controls
     */
    private @Nonnull JPanel createFactionStandingModifiersPanel() {
        // Contents
        chkUseFactionStandingNegotiation = new CampaignOptionsCheckBox("UseFactionStandingNegotiation",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingNegotiation.addMouseListener(createTipPanelUpdater("UseFactionStandingNegotiation"));

        chkUseFactionStandingResupply = new CampaignOptionsCheckBox("UseFactionStandingResupply",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingResupply.addMouseListener(createTipPanelUpdater("UseFactionStandingResupply"));

        chkUseFactionStandingCommandCircuit = new CampaignOptionsCheckBox("UseFactionStandingCommandCircuit",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingCommandCircuit.addMouseListener(createTipPanelUpdater("UseFactionStandingCommandCircuit"));

        chkUseFactionStandingOutlawed = new CampaignOptionsCheckBox("UseFactionStandingOutlawed",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingOutlawed.addMouseListener(createTipPanelUpdater("UseFactionStandingOutlawed"));

        chkUseFactionStandingBatchallRestrictions = new CampaignOptionsCheckBox(
                "UseFactionStandingBatchallRestrictions",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingBatchallRestrictions.addMouseListener(createTipPanelUpdater("UseFactionStandingBatchallRestrictions"));

        chkUseFactionStandingRecruitment = new CampaignOptionsCheckBox("UseFactionStandingRecruitment",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingRecruitment.addMouseListener(createTipPanelUpdater("UseFactionStandingRecruitment"));

        chkUseFactionStandingBarracksCosts = new CampaignOptionsCheckBox("UseFactionStandingBarracksCosts",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingBarracksCosts.addMouseListener(createTipPanelUpdater("UseFactionStandingBarracksCosts"));

        chkUseFactionStandingUnitMarket = new CampaignOptionsCheckBox("UseFactionStandingUnitMarket",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingUnitMarket.addMouseListener(createTipPanelUpdater("UseFactionStandingUnitMarket"));

        chkUseFactionStandingContractPay = new CampaignOptionsCheckBox("UseFactionStandingContractPay",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingContractPay.addMouseListener(createTipPanelUpdater("UseFactionStandingContractPay"));

        chkUseFactionStandingSupportPoints = new CampaignOptionsCheckBox("UseFactionStandingSupportPoints",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingSupportPoints.addMouseListener(createTipPanelUpdater("UseFactionStandingSupportPoints"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FactionStandingEffectsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseFactionStandingNegotiation,
                chkUseFactionStandingResupply,
                chkUseFactionStandingCommandCircuit,
                chkUseFactionStandingOutlawed,
                chkUseFactionStandingBatchallRestrictions,
                chkUseFactionStandingRecruitment,
                chkUseFactionStandingBarracksCosts,
                chkUseFactionStandingUnitMarket,
                chkUseFactionStandingContractPay,
                chkUseFactionStandingSupportPoints);

        return panel;
    }

    /**
     * Copies Faction Standing values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared systems options model to read values from
     */
    void readFromModel(@Nullable SystemsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkTrackFactionStanding.setSelected(model.trackFactionStanding);
        chkTrackClimateRegardChanges.setSelected(model.trackClimateRegardChanges);
        spnRegardMultiplier.setValue(model.regardMultiplier);
        chkUseFactionStandingNegotiation.setSelected(model.useFactionStandingNegotiation);
        chkUseFactionStandingResupply.setSelected(model.useFactionStandingResupply);
        chkUseFactionStandingCommandCircuit.setSelected(model.useFactionStandingCommandCircuit);
        chkUseFactionStandingOutlawed.setSelected(model.useFactionStandingOutlawed);
        chkUseFactionStandingBatchallRestrictions.setSelected(model.useFactionStandingBatchallRestrictions);
        chkUseFactionStandingRecruitment.setSelected(model.useFactionStandingRecruitment);
        chkUseFactionStandingBarracksCosts.setSelected(model.useFactionStandingBarracksCosts);
        chkUseFactionStandingUnitMarket.setSelected(model.useFactionStandingUnitMarket);
        chkUseFactionStandingContractPay.setSelected(model.useFactionStandingContractPay);
        chkUseFactionStandingSupportPoints.setSelected(model.useFactionStandingSupportPoints);
    }

    /**
     * Copies Faction Standing values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared systems options model to write values into
     */
    void writeToModel(@Nullable SystemsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.trackFactionStanding = chkTrackFactionStanding.isSelected();
        model.trackClimateRegardChanges = chkTrackClimateRegardChanges.isSelected();
        model.regardMultiplier = (double) spnRegardMultiplier.getValue();
        model.useFactionStandingNegotiation = chkUseFactionStandingNegotiation.isSelected();
        model.useFactionStandingResupply = chkUseFactionStandingResupply.isSelected();
        model.useFactionStandingCommandCircuit = chkUseFactionStandingCommandCircuit.isSelected();
        model.useFactionStandingOutlawed = chkUseFactionStandingOutlawed.isSelected();
        model.useFactionStandingBatchallRestrictions = chkUseFactionStandingBatchallRestrictions.isSelected();
        model.useFactionStandingRecruitment = chkUseFactionStandingRecruitment.isSelected();
        model.useFactionStandingBarracksCosts = chkUseFactionStandingBarracksCosts.isSelected();
        model.useFactionStandingUnitMarket = chkUseFactionStandingUnitMarket.isSelected();
        model.useFactionStandingContractPay = chkUseFactionStandingContractPay.isSelected();
        model.useFactionStandingSupportPoints = chkUseFactionStandingSupportPoints.isSelected();
    }
}
