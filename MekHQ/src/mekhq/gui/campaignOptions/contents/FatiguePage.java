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
 * The {@code FatiguePage} class builds and manages the Fatigue leaf page of the Campaign Options dialog. It owns the
 * widgets for fatigue configuration - fatigue rate and injury fatigue, field kitchen capacity, and the fatigue
 * automation thresholds - and synchronises them with a shared {@link TurnoverAndRetentionOptionsModel}.
 *
 * <p>This view is a sub-component of {@link TurnoverAndRetentionPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code TurnoverAndRetentionPages}, while this class is responsible only for constructing the
 * Fatigue panel and copying fatigue values to and from the model. The page is built lazily; until
 * {@link #createPanel(TurnoverAndRetentionOptionsModel)} is called,
 * {@link #readFromModel(TurnoverAndRetentionOptionsModel)} and {@link #writeToModel(TurnoverAndRetentionOptionsModel)}
 * are no-ops.</p>
 */
class FatiguePage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private JCheckBox chkUseFatigue;
    private JLabel lblFatigueRate;
    private JSpinner spnFatigueRate;
    private JCheckBox chkUseInjuryFatigue;
    private JLabel lblFieldKitchenCapacity;
    private JSpinner spnFieldKitchenCapacity;
    private JCheckBox chkFieldKitchenIgnoreNonCombatants;
    private JLabel lblFatigueUndeploymentThreshold;
    private JSpinner spnFatigueUndeploymentThreshold;
    private JLabel lblFatigueLeaveThreshold;
    private JSpinner spnFatigueLeaveThreshold;

    private boolean created;

    /**
     * Creates and configures the "Fatigue" page with its relevant components. These
     * include options related to enabling
     * fatigue, fatigue rates, injury fatigue, kitchen capacities, and leave
     * thresholds.
     *
     * @param model the shared turnover and retention options model to populate the freshly built controls from
     *
     * @return the {@link JPanel} representing the constructed Fatigue page.
     */
    @Nonnull JPanel createPanel(@Nullable TurnoverAndRetentionOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_mongoose.png";
        CampaignOptionsHeaderPanel fatigueHeader = new CampaignOptionsHeaderPanel("FatiguePage", imageAddress);

        // Contents
        chkUseFatigue = new CampaignOptionsCheckBox("UseFatigue");
        chkUseFatigue.addMouseListener(createTipPanelUpdater("UseFatigue"));

        lblFatigueRate = new CampaignOptionsLabel("FatigueRate",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFatigueRate.addMouseListener(createTipPanelUpdater("FatigueRate"));
        spnFatigueRate = new CampaignOptionsSpinner("FatigueRate",
                1, 1, 10, 1);
        spnFatigueRate.addMouseListener(createTipPanelUpdater("FatigueRate"));

        chkUseInjuryFatigue = new CampaignOptionsCheckBox("UseInjuryFatigue",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseInjuryFatigue.addMouseListener(createTipPanelUpdater("UseInjuryFatigue"));

        lblFieldKitchenCapacity = new CampaignOptionsLabel("FieldKitchenCapacity");
        lblFieldKitchenCapacity.addMouseListener(createTipPanelUpdater("FieldKitchenCapacity"));
        spnFieldKitchenCapacity = new CampaignOptionsSpinner("FieldKitchenCapacity",
                150, 0, 450, 1);
        spnFieldKitchenCapacity.addMouseListener(createTipPanelUpdater("FieldKitchenCapacity"));

        chkFieldKitchenIgnoreNonCombatants = new CampaignOptionsCheckBox("FieldKitchenIgnoreNonCombatants");
        chkFieldKitchenIgnoreNonCombatants.addMouseListener(createTipPanelUpdater("FieldKitchenIgnoreNonCombatants"));

        lblFatigueUndeploymentThreshold = new CampaignOptionsLabel("FatigueUndeploymentThreshold",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        lblFatigueUndeploymentThreshold.addMouseListener(createTipPanelUpdater("FatigueUndeploymentThreshold"));
        spnFatigueUndeploymentThreshold = new CampaignOptionsSpinner("FatigueUndeploymentThreshold",
                9, 0, 17, 1);
        spnFatigueUndeploymentThreshold.addMouseListener(createTipPanelUpdater("FatigueUndeploymentThreshold"));

        lblFatigueLeaveThreshold = new CampaignOptionsLabel("FatigueLeaveThreshold",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        lblFatigueLeaveThreshold.addMouseListener(createTipPanelUpdater("FatigueLeaveThreshold"));
        spnFatigueLeaveThreshold = new CampaignOptionsSpinner("FatigueLeaveThreshold",
                13, 0, 17, 1);
        spnFatigueLeaveThreshold.addMouseListener(createTipPanelUpdater("FatigueLeaveThreshold"));

        JPanel panel = CampaignOptionsPagePanel.builder("FatiguePage", "FatiguePage", imageAddress)
                .header(fatigueHeader)
                .quote("fatiguePage")
                .section("lblFatigueRulesPanel.text", "lblFatigueRulesPanel.summary", createFatigueRulesPanel())
                .section("lblFatigueFieldKitchenPanel.text",
                        "lblFatigueFieldKitchenPanel.summary",
                        createFatigueFieldKitchenPanel())
                .section("lblFatigueAutomationPanel.text",
                        "lblFatigueAutomationPanel.summary",
                        createFatigueAutomationPanel())
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createFatigueRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueRulesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseFatigue);
        panel.addRow(lblFatigueRate, spnFatigueRate);
        panel.addCheckBox(chkUseInjuryFatigue);

        return panel;
    }

    private @Nonnull JPanel createFatigueFieldKitchenPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueFieldKitchenPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblFieldKitchenCapacity, spnFieldKitchenCapacity);
        panel.addCheckBox(chkFieldKitchenIgnoreNonCombatants);

        return panel;
    }

    private @Nonnull JPanel createFatigueAutomationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueAutomationPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblFatigueUndeploymentThreshold, spnFatigueUndeploymentThreshold);
        panel.addRow(lblFatigueLeaveThreshold, spnFatigueLeaveThreshold);

        return panel;
    }

    /**
     * Copies fatigue values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared turnover and retention options model to read values from
     */
    void readFromModel(@Nullable TurnoverAndRetentionOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseFatigue.setSelected(model.useFatigue);
        spnFatigueRate.setValue(model.fatigueRate);
        chkUseInjuryFatigue.setSelected(model.useInjuryFatigue);
        spnFieldKitchenCapacity.setValue(model.fieldKitchenCapacity);
        chkFieldKitchenIgnoreNonCombatants.setSelected(model.fieldKitchenIgnoreNonCombatants);
        spnFatigueUndeploymentThreshold.setValue(model.fatigueUndeploymentThreshold);
        spnFatigueLeaveThreshold.setValue(model.fatigueLeaveThreshold);
    }

    /**
     * Copies fatigue values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared turnover and retention options model to write values into
     */
    void writeToModel(@Nullable TurnoverAndRetentionOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useFatigue = chkUseFatigue.isSelected();
        model.fatigueRate = (int) spnFatigueRate.getValue();
        model.useInjuryFatigue = chkUseInjuryFatigue.isSelected();
        model.fieldKitchenCapacity = (int) spnFieldKitchenCapacity.getValue();
        model.fieldKitchenIgnoreNonCombatants = chkFieldKitchenIgnoreNonCombatants.isSelected();
        model.fatigueUndeploymentThreshold = (int) spnFatigueUndeploymentThreshold.getValue();
        model.fatigueLeaveThreshold = (int) spnFatigueLeaveThreshold.getValue();
    }
}
