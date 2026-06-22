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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code ATimeOfWarPage} class builds and manages the A Time of War leaf page of the Campaign Options dialog. It
 * owns the widgets for ATOW configuration - the attribute, trait, age-effect, and related campaign-mechanic toggles -
 * and synchronises them with a shared {@link SystemsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link SystemsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code SystemsPages}, while this class is responsible only for constructing the A Time of War panel and
 * copying ATOW values to and from the model. The page is built lazily; until {@link #createPanel(SystemsOptionsModel)}
 * is called, {@link #readFromModel(SystemsOptionsModel)} and {@link #writeToModel(SystemsOptionsModel)} are no-ops.</p>
 */
class ATimeOfWarPage {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private CampaignOptionsHeaderPanel atowHeader;

    private JCheckBox chkUseAttributes;
    private JCheckBox chkRandomizeAttributes;
    private JCheckBox chkDisplayAllAttributes;
    private JCheckBox chkUseAgeEffects;
    private JCheckBox chkRandomizeTraits;
    private JCheckBox chkAllowMonthlyReinvestment;
    private JCheckBox chkAllowMonthlyConnections;
    private JCheckBox chkUseBetterExtraIncome;
    private JCheckBox chkUseSmallArmsOnly;

    private boolean created;

    /**
     * Creates the ATOW page panel, containing grouped UI elements for configuring ATOW-related options and its header.
     *
     * @param model the shared systems options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} component representing the entire ATOW page UI
     */
    @Nonnull JPanel createPanel(@Nullable SystemsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_elysian_fields.png";
        atowHeader = new CampaignOptionsHeaderPanel("ATimeOfWarPage", imageAddress);

        // Contents
        JPanel pnlATOWAttributes = createATOWAttributesPanel();

        // Layout the Panel
        final JPanel panel = CampaignOptionsPagePanel.builder("ATimeOfWarPage", "ATimeOfWarPage", imageAddress)
                .header(atowHeader)
                .quote("atowPage")
                .section("lblATOWAttributesPanel.text",
                        "lblATOWAttributesPanel.summary",
                        pnlATOWAttributes)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and returns the ATOW panel, which allows users to configure settings for attribute and traits
     * probabilities.
     *
     * @return A {@code JPanel} containing configuration options for phenotype probabilities.
     */
    private @Nonnull JPanel createATOWAttributesPanel() {
        // Contents
        chkUseAttributes = new CampaignOptionsCheckBox("UseAttributes",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseAttributes.addMouseListener(createTipPanelUpdater("UseAttributes"));
        chkRandomizeAttributes = new CampaignOptionsCheckBox("RandomizeAttributes");
        chkRandomizeAttributes.addMouseListener(createTipPanelUpdater("RandomizeAttributes"));
        chkDisplayAllAttributes = new CampaignOptionsCheckBox("DisplayAllAttributes",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkDisplayAllAttributes.addMouseListener(createTipPanelUpdater("DisplayAllAttributes"));
        chkUseAgeEffects = new CampaignOptionsCheckBox("UseAgeEffects",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAgeEffects.addMouseListener(createTipPanelUpdater("UseAgeEffects"));
        chkRandomizeTraits = new CampaignOptionsCheckBox("RandomizeTraits");
        chkRandomizeTraits.addMouseListener(createTipPanelUpdater("RandomizeTraits"));
        chkAllowMonthlyReinvestment = new CampaignOptionsCheckBox("AllowMonthlyReinvestment");
        chkAllowMonthlyReinvestment.addMouseListener(createTipPanelUpdater("AllowMonthlyReinvestment"));
        chkAllowMonthlyConnections = new CampaignOptionsCheckBox("AllowMonthlyConnections",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkAllowMonthlyConnections.addMouseListener(createTipPanelUpdater("AllowMonthlyConnections"));
        chkUseBetterExtraIncome = new CampaignOptionsCheckBox("UseBetterExtraIncome",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseBetterExtraIncome.addMouseListener(createTipPanelUpdater("UseBetterExtraIncome"));
        chkUseSmallArmsOnly = new CampaignOptionsCheckBox("UseSmallArmsOnly",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseSmallArmsOnly.addMouseListener(createTipPanelUpdater("UseSmallArmsOnly"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ATOWAttributesPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseAttributes,
                chkRandomizeAttributes,
                chkDisplayAllAttributes,
                chkUseAgeEffects,
                chkRandomizeTraits,
                chkAllowMonthlyReinvestment,
                chkAllowMonthlyConnections,
                chkUseBetterExtraIncome,
                chkUseSmallArmsOnly);

        return panel;
    }

    /**
     * Copies ATOW values from the shared model into this page's controls. This is a no-op until the page has been built.
     *
     * @param model the shared systems options model to read values from
     */
    void readFromModel(@Nullable SystemsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseAttributes.setSelected(model.useAttributes);
        chkRandomizeAttributes.setSelected(model.randomizeAttributes);
        chkDisplayAllAttributes.setSelected(model.displayAllAttributes);
        chkUseAgeEffects.setSelected(model.useAgeEffects);
        chkRandomizeTraits.setSelected(model.randomizeTraits);
        chkAllowMonthlyReinvestment.setSelected(model.allowMonthlyReinvestment);
        chkAllowMonthlyConnections.setSelected(model.allowMonthlyConnections);
        chkUseBetterExtraIncome.setSelected(model.useBetterExtraIncome);
        chkUseSmallArmsOnly.setSelected(model.useSmallArmsOnly);
    }

    /**
     * Copies ATOW values from this page's controls into the shared model. This is a no-op until the page has been built.
     *
     * @param model the shared systems options model to write values into
     */
    void writeToModel(@Nullable SystemsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useAttributes = chkUseAttributes.isSelected();
        model.randomizeAttributes = chkRandomizeAttributes.isSelected();
        model.displayAllAttributes = chkDisplayAllAttributes.isSelected();
        model.useAgeEffects = chkUseAgeEffects.isSelected();
        model.randomizeTraits = chkRandomizeTraits.isSelected();
        model.allowMonthlyReinvestment = chkAllowMonthlyReinvestment.isSelected();
        model.allowMonthlyConnections = chkAllowMonthlyConnections.isSelected();
        model.useBetterExtraIncome = chkUseBetterExtraIncome.isSelected();
        model.useSmallArmsOnly = chkUseSmallArmsOnly.isSelected();
    }
}
