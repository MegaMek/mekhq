/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code SystemsTab} class is responsible for managing and displaying the "Reputation" tab within the campaign
 * options UI. It provides Swing components for configuring reputation-related settings in a {@link Campaign}, including
 * unit rating methods, manual modifiers, and various campaign mechanics toggles.
 *
 * <p>The tab is constructed using helper panels and controls, grouped under "General" and "Sanity" sub-panels,
 * facilitating a user-friendly way to view and adjust reputation options.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SystemsTab {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    //start Reputation Tab
    private CampaignOptionsHeaderPanel reputationHeader;

    private JPanel pnlReputationGeneralOptions;
    private JLabel lblReputation;
    private MMComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JCheckBox chkResetCriminalRecord;

    private JPanel pnlReputationSanityOptions;
    private JLabel lblManualUnitRatingModifier;
    private JSpinner manualUnitRatingModifier;
    private JCheckBox chkClampReputationPayMultiplier;
    private JCheckBox chkReduceReputationPerformanceModifier;
    private JCheckBox chkReputationPerformanceModifierCutOff;

    /**
     * Constructs a new {@code SystemsTab} for the specified campaign.
     *
     * @param campaign the campaign associated with this tab
     *
     * @author Illiani
     * @since 0.50.07
     */
    public SystemsTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
    }

    /**
     * Creates the Reputation tab panel, containing grouped UI elements for reputation options and its header.
     *
     * @return a {@link JPanel} component representing the entire Reputation tab UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public JPanel createReputationTab() {
        // Header
        reputationHeader = new CampaignOptionsHeaderPanel("ReputationTab",
              getImageDirectory() + "logo_morgrains_valkyrate.png",
              10);

        // Contents
        pnlReputationGeneralOptions = createReputationGeneralPanel();
        pnlReputationSanityOptions = createReputationSanityPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(reputationHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlReputationGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlReputationSanityOptions, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "ReputationTab");
    }

    /**
     * Creates and lays out the general reputation options panel, including controls for selecting unit rating method,
     * manual modifiers, and criminal record reset.
     *
     * @return a {@link JPanel} containing the general reputation controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationGeneralPanel() {
        // Contents
        lblReputation = new CampaignOptionsLabel("Reputation");
        lblReputation.addMouseListener(createTipPanelUpdater(reputationHeader, "Reputation"));
        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());
        unitRatingMethodCombo.setToolTipText(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblReputation.tooltip")));
        unitRatingMethodCombo.addMouseListener(createTipPanelUpdater(reputationHeader, "Reputation"));

        lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        lblManualUnitRatingModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ManualUnitRatingModifier"));
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier", 0, -1000, 1000, 1);
        manualUnitRatingModifier.addMouseListener(createTipPanelUpdater(reputationHeader, "ManualUnitRatingModifier"));

        chkResetCriminalRecord = new CampaignOptionsCheckBox("ResetCriminalRecord");
        chkResetCriminalRecord.addMouseListener(createTipPanelUpdater(reputationHeader, "ResetCriminalRecord"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblReputation, layout);
        layout.gridx++;
        panel.add(unitRatingMethodCombo, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblManualUnitRatingModifier, layout);
        layout.gridx++;
        panel.add(manualUnitRatingModifier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkResetCriminalRecord, layout);

        return panel;
    }

    /**
     * Creates and lays out the reputation "sanity" options panel, which includes various checkboxes for limiting or
     * modifying reputation calculations.
     *
     * @return a {@link JPanel} containing the reputation sanity option controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationSanityPanel() {
        // Contents
        chkClampReputationPayMultiplier = new CampaignOptionsCheckBox("ClampReputationPayMultiplier");
        chkClampReputationPayMultiplier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ClampReputationPayMultiplier"));

        chkReduceReputationPerformanceModifier = new CampaignOptionsCheckBox("ReduceReputationPerformanceModifier");
        chkReduceReputationPerformanceModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ReduceReputationPerformanceModifier"));

        chkReputationPerformanceModifierCutOff = new CampaignOptionsCheckBox("ReputationPerformanceModifierCutOff");
        chkReputationPerformanceModifierCutOff.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ReputationPerformanceModifierCutOff"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationGeneralOptionsPanel", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkClampReputationPayMultiplier, layout);

        layout.gridy++;
        panel.add(chkReduceReputationPerformanceModifier, layout);

        layout.gridy++;
        panel.add(chkReputationPerformanceModifierCutOff, layout);

        return panel;
    }

    /**
     * Loads values from the current campaign or an optional preset campaign options into the UI components, updating
     * their states to match the data.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the specified {@code presetCampaignOptions}, or the current campaign's options if {@code null},
     * into the UI form fields and controls.
     *
     * @param presetCampaignOptions an alternative {@link CampaignOptions}, or {@code null} to use the current
     *                              campaign's options
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Reputation
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());

        chkClampReputationPayMultiplier.setSelected(options.isClampReputationPayMultiplier());
        chkReduceReputationPerformanceModifier.setSelected(options.isReduceReputationPerformanceModifier());
        chkReputationPerformanceModifierCutOff.setSelected(options.isReputationPerformanceModifierCutOff());
    }

    /**
     * Applies the currently selected values in the UI controls to modify the campaign's options. If a preset is
     * provided, that preset is updated instead of the campaign's default options.
     *
     * @param presetCampaignOptions an alternative {@link CampaignOptions} object to update, or {@code null} to update
     *                              the campaign's own options
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Reputation
        options.setUnitRatingMethod(unitRatingMethodCombo.getSelectedItem());
        options.setManualUnitRatingModifier((int) manualUnitRatingModifier.getValue());

        if (chkResetCriminalRecord.isSelected()) {
            campaign.setDateOfLastCrime(null);
            campaign.setCrimeRating(0);
            campaign.setCrimePirateModifier(0);
        }

        options.setClampReputationPayMultiplier(chkClampReputationPayMultiplier.isSelected());
        options.setReduceReputationPerformanceModifier(chkReduceReputationPerformanceModifier.isSelected());
        options.setReputationPerformanceModifierCutOff(chkReputationPerformanceModifierCutOff.isSelected());
    }
}
