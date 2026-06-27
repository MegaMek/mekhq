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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code UnitMarketPage} class builds and manages the Unit Market leaf page of the Campaign Options dialog. It owns
 * the widgets for unit market configuration - the market method, regional variation toggle, artillery chance, rarity
 * modifier, and delivery options - and synchronises them with a shared {@link MarketsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link MarketsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code MarketsPages}, while this class is responsible only for constructing the Unit Market panel and copying
 * unit market values to and from the model. The page is built lazily; until {@link #createPanel(MarketsOptionsModel)} is
 * called, {@link #readFromModel(MarketsOptionsModel)} and {@link #writeToModel(MarketsOptionsModel)} are no-ops.</p>
 */
class UnitMarketPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private JLabel lblUnitMarketMethod;
    private MMComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMekVariations;
    private JLabel lblUnitMarketArtilleryUnitChance;
    private JSpinner spnUnitMarketArtilleryUnitChance;
    private JLabel lblUnitMarketRarityModifier;
    private JSpinner spnUnitMarketRarityModifier;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkMothballUnitMarketDeliveries;
    private JCheckBox chkUnitMarketReportRefresh;

    private boolean created;

    /**
     * Creates and returns the JPanel representing the Unit Market configuration page.
     * <p>
     * This page includes options such as unit market methods, rarity modifiers, special unit change settings, and more.
     *
     * @param model the shared markets options model to populate the freshly built controls from
     *
     * @return A {@link JPanel} for the Unit Market configuration page.
     */
    @Nonnull JPanel createPanel(@Nullable MarketsOptionsModel model) {
        // Header
        // start Unit Market
        String imageAddress = getImageDirectory() + "logo_clan_ice_hellion.png";
        CampaignOptionsHeaderPanel unitMarketHeader = new CampaignOptionsHeaderPanel("UnitMarketPage", imageAddress);

        // Contents
        lblUnitMarketMethod = new CampaignOptionsLabel("UnitMarketMethod");
        lblUnitMarketMethod.addMouseListener(createTipPanelUpdater("UnitMarketMethod"));
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        comboUnitMarketMethod.addMouseListener(createTipPanelUpdater("UnitMarketMethod"));

        chkUnitMarketRegionalMekVariations = new CampaignOptionsCheckBox("UnitMarketRegionalMekVariations");
        chkUnitMarketRegionalMekVariations.addMouseListener(createTipPanelUpdater("UnitMarketRegionalMekVariations"));

        lblUnitMarketArtilleryUnitChance = new CampaignOptionsLabel("UnitMarketArtilleryUnitChance");
        lblUnitMarketArtilleryUnitChance.addMouseListener(createTipPanelUpdater("UnitMarketArtilleryUnitChance"));
        spnUnitMarketArtilleryUnitChance = new CampaignOptionsSpinner("UnitMarketArtilleryUnitChance", 30, 0, 100,
                1);
        spnUnitMarketArtilleryUnitChance.addMouseListener(createTipPanelUpdater("UnitMarketArtilleryUnitChance"));

        lblUnitMarketRarityModifier = new CampaignOptionsLabel("UnitMarketRarityModifier");
        lblUnitMarketRarityModifier.addMouseListener(createTipPanelUpdater("UnitMarketRarityModifier"));
        spnUnitMarketRarityModifier = new CampaignOptionsSpinner("UnitMarketRarityModifier", 0, -10, 10, 1);
        spnUnitMarketRarityModifier.addMouseListener(createTipPanelUpdater("UnitMarketRarityModifier"));

        chkInstantUnitMarketDelivery = new CampaignOptionsCheckBox("InstantUnitMarketDelivery");
        chkInstantUnitMarketDelivery.addMouseListener(createTipPanelUpdater("InstantUnitMarketDelivery"));

        chkMothballUnitMarketDeliveries = new CampaignOptionsCheckBox("MothballUnitMarketDeliveries");
        chkMothballUnitMarketDeliveries.addMouseListener(createTipPanelUpdater("MothballUnitMarketDeliveries"));

        chkUnitMarketReportRefresh = new CampaignOptionsCheckBox("UnitMarketReportRefresh");
        chkUnitMarketReportRefresh
                .addMouseListener(createTipPanelUpdater("UnitMarketReportRefresh"));

        JPanel generationPanel = createUnitMarketGenerationPanel();
        JPanel deliveryPanel = createUnitMarketDeliveryPanel();

        final JPanel panel = CampaignOptionsPagePanel.builder("UnitMarketPage", "UnitMarketPage", imageAddress)
                .header(unitMarketHeader)
                .quote("unitMarketPage")
                .section("lblUnitMarketGenerationPanel.text",
                        "lblUnitMarketGenerationPanel.summary",
                        generationPanel)
                .section("lblUnitMarketDeliveryPanel.text",
                        "lblUnitMarketDeliveryPanel.summary",
                        deliveryPanel)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createUnitMarketGenerationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitMarketGenerationPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblUnitMarketMethod, comboUnitMarketMethod);
        panel.addCheckBox(chkUnitMarketRegionalMekVariations);
        panel.addRow(lblUnitMarketArtilleryUnitChance, spnUnitMarketArtilleryUnitChance);
        panel.addRow(lblUnitMarketRarityModifier, spnUnitMarketRarityModifier);

        return panel;
    }

    private @Nonnull JPanel createUnitMarketDeliveryPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitMarketDeliveryPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkInstantUnitMarketDelivery,
                chkMothballUnitMarketDeliveries,
                chkUnitMarketReportRefresh);

        return panel;
    }

    /**
     * Copies unit market values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared markets options model to read values from
     */
    void readFromModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboUnitMarketMethod.setSelectedItem(model.unitMarketMethod);
        chkUnitMarketRegionalMekVariations.setSelected(model.unitMarketRegionalMekVariations);
        spnUnitMarketArtilleryUnitChance.setValue(model.unitMarketArtilleryUnitChance);
        spnUnitMarketRarityModifier.setValue(model.unitMarketRarityModifier);
        chkInstantUnitMarketDelivery.setSelected(model.instantUnitMarketDelivery);
        chkMothballUnitMarketDeliveries.setSelected(model.mothballUnitMarketDeliveries);
        chkUnitMarketReportRefresh.setSelected(model.unitMarketReportRefresh);
    }

    /**
     * Copies unit market values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared markets options model to write values into
     */
    void writeToModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.unitMarketMethod = comboUnitMarketMethod.getSelectedItem();
        model.unitMarketRegionalMekVariations = chkUnitMarketRegionalMekVariations.isSelected();
        model.unitMarketArtilleryUnitChance = (int) spnUnitMarketArtilleryUnitChance.getValue();
        model.unitMarketRarityModifier = (int) spnUnitMarketRarityModifier.getValue();
        model.instantUnitMarketDelivery = chkInstantUnitMarketDelivery.isSelected();
        model.mothballUnitMarketDeliveries = chkMothballUnitMarketDeliveries.isSelected();
        model.unitMarketReportRefresh = chkUnitMarketReportRefresh.isSelected();
    }
}
