/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.contents;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;

import static mekhq.campaign.parts.enums.PartQuality.QUALITY_F;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * The FinancesTab class represents a UI tab within a larger financial management system
 * for a campaign. It provides panels, checkboxes, spinners, combo boxes, and other controls
 * to manage and configure various financial options, payments, sales, taxes, shares,
 * and price multipliers for the campaign.
 * <p>
 * It is primarily composed of multiple `JPanel` sections organized using
 * `GroupLayout` for modularity and clarity.
 */
public class FinancesTab {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    //start General Options
    private JPanel pnlGeneralOptions;
    private JCheckBox useLoanLimitsBox;
    private JCheckBox usePercentageMaintenanceBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox showPeacetimeCostBox;
    private JLabel lblFinancialYearDuration;
    private MMComboBox<FinancialYearDuration> comboFinancialYearDuration;
    private JCheckBox newFinancialYearFinancesToCSVExportBox;

    private JPanel pnlPayments;
    private JCheckBox payForPartsBox;
    private JCheckBox payForRepairsBox;
    private JCheckBox payForUnitsBox;
    private JCheckBox payForSalariesBox;
    private JCheckBox payForOverheadBox;
    private JCheckBox payForMaintainBox;
    private JCheckBox payForTransportBox;
    private JCheckBox payForRecruitmentBox;


    private JPanel pnlSales;
    private JCheckBox sellUnitsBox;
    private JCheckBox sellPartsBox;

    private JPanel pnlOtherSystems;

    private JPanel pnlTaxes;
    private JCheckBox chkUseTaxes;
    private JLabel lblTaxesPercentage;
    private JSpinner spnTaxesPercentage;

    private JPanel pnlShares;
    private JCheckBox chkUseShareSystem;
    private JCheckBox chkSharesForAll;
    //end General Options

    //start Price Multipliers
    private JPanel pnlGeneralMultipliers;
    private JLabel lblCommonPartPriceMultiplier;
    private JSpinner spnCommonPartPriceMultiplier;
    private JLabel lblInnerSphereUnitPriceMultiplier;
    private JSpinner spnInnerSphereUnitPriceMultiplier;
    private JLabel lblInnerSpherePartPriceMultiplier;
    private JSpinner spnInnerSpherePartPriceMultiplier;
    private JLabel lblClanUnitPriceMultiplier;
    private JSpinner spnClanUnitPriceMultiplier;
    private JLabel lblClanPartPriceMultiplier;
    private JSpinner spnClanPartPriceMultiplier;
    private JLabel lblMixedTechUnitPriceMultiplier;
    private JSpinner spnMixedTechUnitPriceMultiplier;

    private JPanel pnlUsedPartsMultipliers;
    private JLabel[] lblUsedPartPriceMultipliers;
    private JSpinner[] spnUsedPartPriceMultipliers;

    private JPanel pnlOtherMultipliers;
    private JLabel lblDamagedPartsValueMultiplier;
    private JSpinner spnDamagedPartsValueMultiplier;
    private JLabel lblUnrepairablePartsValueMultiplier;
    private JSpinner spnUnrepairablePartsValueMultiplier;
    private JLabel lblCancelledOrderRefundMultiplier;
    private JSpinner spnCancelledOrderRefundMultiplier;
    //end Price Multipliers

    /**
     * Constructs a `FinancesTab` instance which manages the financial settings
     * and configurations for a specific campaign.
     *
     * @param campaign The `Campaign` object that this `FinancesTab` will be associated with.
     *                 Provides access to campaign-related options and data.
     */
    public FinancesTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
    }

    /**
     * Initializes the primary components and subcomponents of the `FinancesTab`.
     * Specifically, sets up the 'General Options' and 'Price Multipliers' tabs
     * through their respective initialization methods.
     * This method ensures that the tabs are prepared prior to being displayed or used.
     */
    private void initialize() {
        initializeGeneralOptionsTab();
        initializePriceMultipliersTab();
    }

    /**
     * Initializes the General Options tab within the application's UI.
     * <p>
     * This method sets up various UI components and panels that
     * provide configurable options for general settings, payments,
     * sales, other systems, taxes, and shares. Components include
     * checkboxes, labels, spinners, and combo boxes that allow
     * the user to interact with and configure these settings.
     * <p>
     * All UI components are initialized, but additional configuration
     * such as layout placements, listeners, or actual visibility might
     * need to be completed separately.
     */
    private void initializeGeneralOptionsTab() {
        // General Options
        pnlGeneralOptions = new JPanel();
        useLoanLimitsBox = new JCheckBox();
        usePercentageMaintenanceBox = new JCheckBox();
        useExtendedPartsModifierBox = new JCheckBox();
        usePeacetimeCostBox = new JCheckBox();
        showPeacetimeCostBox = new JCheckBox();

        lblFinancialYearDuration = new JLabel();
        comboFinancialYearDuration = new MMComboBox<>("comboFinancialYearDuration",
            FinancialYearDuration.values());

        newFinancialYearFinancesToCSVExportBox = new JCheckBox();

        // Payments
        pnlPayments = new JPanel();
        payForPartsBox = new JCheckBox();
        payForRepairsBox = new JCheckBox();
        payForUnitsBox = new JCheckBox();
        payForSalariesBox = new JCheckBox();
        payForOverheadBox = new JCheckBox();
        payForMaintainBox = new JCheckBox();
        payForTransportBox = new JCheckBox();
        payForRecruitmentBox = new JCheckBox();

        // Sales
        pnlSales = new JPanel();
        sellUnitsBox = new JCheckBox();
        sellPartsBox = new JCheckBox();

        pnlOtherSystems = new JPanel();

        // Taxes
        pnlTaxes = new JPanel();
        chkUseTaxes = new JCheckBox();
        lblTaxesPercentage = new JLabel();
        spnTaxesPercentage = new JSpinner();

        // Shares
        pnlShares= new JPanel();
        chkUseShareSystem = new JCheckBox();
        chkSharesForAll = new JCheckBox();
    }

    /**
     * Creates and configures the Finances General Options tab, assembling its components,
     * layout, and panels which include general options, other systems, payments, and sales.
     * This method initializes required sub-panels and arranges them within the overall
     * structure to create a fully constructed tab for financial general options.
     *
     * @return A fully configured JPanel representing the Finances General Options tab.
     */
    public JPanel createFinancesGeneralOptionsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("FinancesGeneralTab",
            getImageDirectory() + "logo_star_league.png");

        // Contents
        pnlGeneralOptions = createGeneralOptionsPanel();
        pnlOtherSystems = createOtherSystemsPanel();

        pnlPayments = createPaymentsPanel();
        pnlSales = createSalesPanel();

        // Layout the Panel
        final JPanel panelTransactions = new CampaignOptionsStandardPanel("FinancesGeneralTabTransactions");
        GridBagConstraints layoutTransactions = new CampaignOptionsGridBagConstraints(panelTransactions);

        layoutTransactions.gridwidth = 2;
        layoutTransactions.gridy = 0;
        layoutTransactions.gridx = 0;
        panelTransactions.add(pnlPayments, layoutTransactions);
        layoutTransactions.gridx += 2;
        panelTransactions.add(pnlSales, layoutTransactions);

        final JPanel panel = new CampaignOptionsStandardPanel("FinancesGeneralTab", true);
        GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlGeneralOptions, layoutParent);
        layoutParent.gridx++;
        panel.add(pnlOtherSystems, layoutParent);

        layoutParent.gridwidth = 2;
        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(panelTransactions, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "FinancesGeneralTab");
    }

    /**
     * Creates and configures a payments panel with various checkbox options for payment categories such as
     * parts, repairs, units, salaries, overhead, maintenance, transport, and recruitment. The layout of
     * the panel organizes the checkboxes in a grid-based format.
     *
     * @return a JPanel instance containing the configured payment options checkboxes.
     */
    private JPanel createPaymentsPanel() {
        // Contents
        payForPartsBox = new CampaignOptionsCheckBox("PayForPartsBox");
        payForRepairsBox = new CampaignOptionsCheckBox("PayForRepairsBox");
        payForUnitsBox = new CampaignOptionsCheckBox("PayForUnitsBox");
        payForSalariesBox = new CampaignOptionsCheckBox("PayForSalariesBox");
        payForOverheadBox = new CampaignOptionsCheckBox("PayForOverheadBox");
        payForMaintainBox = new CampaignOptionsCheckBox("PayForMaintainBox");
        payForTransportBox = new CampaignOptionsCheckBox("PayForTransportBox");
        payForRecruitmentBox = new CampaignOptionsCheckBox("PayForRecruitmentBox");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PaymentsPanel", true,
            "PaymentsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(payForPartsBox, layout);
        layout.gridx++;
        panel.add(payForRepairsBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForUnitsBox, layout);
        layout.gridx++;
        panel.add(payForSalariesBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForOverheadBox, layout);
        layout.gridx++;
        panel.add(payForMaintainBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForTransportBox, layout);
        layout.gridx++;
        panel.add(payForRecruitmentBox, layout);

        return panel;
    }

    /**
     * Constructs and returns a {@link JPanel} for the 'Other Systems Panel'.
     * This panel combines two sub-panels: 'Taxes Panel' and 'Shares Panel'.
     * Each sub-panel is added sequentially to the main panel using a grid-bag layout.
     * These panels are organized vertically in the resulting panel.
     *
     * @return {@link JPanel} representing the 'Other Systems Panel', containing the
     *         'Taxes Panel' and 'Shares Panel'.
     */
    private JPanel createOtherSystemsPanel() {
        // Contents
        pnlTaxes = createTaxesPanel();
        pnlShares = createSharesPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OtherSystemsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(pnlTaxes, layout);

        layout.gridy++;
        panel.add(pnlShares, layout);

        return panel;
    }

    /**
     * Creates and initializes the General Options Panel with various configurable
     * options related to loan limits, maintenance, parts modifiers, peacetime costs,
     * and financial year settings. The panel includes checkboxes and labels for easy
     * user interaction and configuration of these parameters.
     *
     * @return A JPanel containing the general options components laid out in a
     *         structured format.
     */
    private JPanel createGeneralOptionsPanel() {
        // Contents
        useLoanLimitsBox = new CampaignOptionsCheckBox("UseLoanLimitsBox");
        usePercentageMaintenanceBox = new CampaignOptionsCheckBox("UsePercentageMaintenanceBox");
        useExtendedPartsModifierBox = new CampaignOptionsCheckBox("UseExtendedPartsModifierBox");
        usePeacetimeCostBox = new CampaignOptionsCheckBox("UsePeacetimeCostBox");
        showPeacetimeCostBox = new CampaignOptionsCheckBox("ShowPeacetimeCostBox");

        lblFinancialYearDuration = new CampaignOptionsLabel("FinancialYearDuration");

        newFinancialYearFinancesToCSVExportBox = new CampaignOptionsCheckBox("NewFinancialYearFinancesToCSVExportBox");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("GeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(useLoanLimitsBox, layout);

        layout.gridy++;
        panel.add(usePercentageMaintenanceBox, layout);

        layout.gridy++;
        panel.add(useExtendedPartsModifierBox, layout);

        layout.gridy++;
        panel.add(usePeacetimeCostBox, layout);

        layout.gridy++;
        panel.add(showPeacetimeCostBox, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblFinancialYearDuration, layout);
        layout.gridx++;
        panel.add(comboFinancialYearDuration, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(newFinancialYearFinancesToCSVExportBox, layout);

        return panel;
    }

    /**
     * Creates and configures the sales panel within the finance tab.
     * The panel contains checkboxes for options related to sales, including
     * "Sell Units" and "Sell Parts". These checkboxes are added to a layout
     * that organizes the components vertically.
     *
     * @return A JPanel instance containing the configured sales options.
     */
    private JPanel createSalesPanel() {
        // Contents
        sellUnitsBox = new CampaignOptionsCheckBox("SellUnitsBox");
        sellPartsBox = new CampaignOptionsCheckBox("SellPartsBox");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalesPanel", true,
            "SalesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(sellUnitsBox, layout);

        layout.gridy++;
        panel.add(sellPartsBox, layout);

        return panel;
    }

    /**
     * Creates and returns a JPanel representing the taxes panel in the campaign options.
     * This panel includes a checkbox to enable or disable taxes and a spinner
     * to set the percentage of taxes, along with corresponding labels.
     *
     * @return the configured JPanel containing the components for the taxes panel.
     */
    private JPanel createTaxesPanel() {
        // Contents
        chkUseTaxes = new CampaignOptionsCheckBox("UseTaxesBox");

        lblTaxesPercentage = new CampaignOptionsLabel("TaxesPercentage");
        spnTaxesPercentage = new CampaignOptionsSpinner("TaxesPercentage",
            30, 1, 100, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TaxesPanel", true,
            "TaxesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkUseTaxes, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblTaxesPercentage, layout);
        layout.gridx++;
        panel.add(spnTaxesPercentage, layout);

        return panel;
    }

    /**
     * Creates and returns a JPanel representing the 'Shares Panel' within the finance tab.
     * <p>
     * The panel is laid out using grid-based constraints to position the components
     * in a structured vertical arrangement.
     *
     * @return A JPanel containing the configured components for the 'Shares Panel'.
     */
    private JPanel createSharesPanel() {
        // Contents
        chkUseShareSystem = new CampaignOptionsCheckBox("UseShareSystem");
        chkSharesForAll = new CampaignOptionsCheckBox("SharesForAll");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SharesPanel", true,
            "SharesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUseShareSystem, layout);

        layout.gridy++;
        panel.add(chkSharesForAll, layout);

        return panel;
    }

    /**
     * Initializes the components and layout for the price multipliers tab.
     * This tab includes controls for setting various price multipliers such as
     * - General multipliers for unit and part prices.
     * - Multipliers for used parts.
     * - Miscellaneous multipliers for damaged, unrepairable parts, and order refunds.
     * <p>
     * The method creates and assigns UI components including panels, labels, and spinners
     * to their respective class fields. Each field corresponds to a specific category
     * of price multiplier.
     */
    private void initializePriceMultipliersTab() {
        pnlGeneralMultipliers = new JPanel();
        lblCommonPartPriceMultiplier = new JLabel();
        spnCommonPartPriceMultiplier = new JSpinner();
        lblInnerSphereUnitPriceMultiplier = new JLabel();
        spnInnerSphereUnitPriceMultiplier = new JSpinner();
        lblInnerSpherePartPriceMultiplier = new JLabel();
        spnInnerSpherePartPriceMultiplier = new JSpinner();
        lblClanUnitPriceMultiplier = new JLabel();
        spnClanUnitPriceMultiplier = new JSpinner();
        lblClanPartPriceMultiplier = new JLabel();
        spnClanPartPriceMultiplier = new JSpinner();
        lblMixedTechUnitPriceMultiplier = new JLabel();
        spnMixedTechUnitPriceMultiplier = new JSpinner();

        pnlUsedPartsMultipliers = new JPanel();
        lblUsedPartPriceMultipliers = new JLabel[1]; // we initialize this properly later
        spnUsedPartPriceMultipliers = new JSpinner[1]; // we initialize this properly later

        pnlOtherMultipliers = new JPanel();
        lblDamagedPartsValueMultiplier = new JLabel();
        spnDamagedPartsValueMultiplier = new JSpinner();
        lblUnrepairablePartsValueMultiplier = new JLabel();
        spnUnrepairablePartsValueMultiplier = new JSpinner();
        lblCancelledOrderRefundMultiplier = new JLabel();
        spnCancelledOrderRefundMultiplier = new JSpinner();
    }

    /**
     * Creates and returns a JPanel representing the "Price Multipliers" tab in the user interface.
     * The method includes a header section, general multipliers panel, used parts multipliers panel,
     * and other multipliers panel. These components are arranged using a specific layout and added
     * to a parent panel.
     *
     * @return a JPanel representing the "Price Multipliers" tab with all its components and layout configured
     */
    public JPanel createPriceMultipliersTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PriceMultipliersTab",
            getImageDirectory() + "logo_clan_stone_lion.png", true);

        // Contents
        pnlGeneralMultipliers = createGeneralMultipliersPanel();
        pnlUsedPartsMultipliers = createUsedPartsMultiplierPanel();
        pnlOtherMultipliers = createOtherMultipliersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PriceMultipliersTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlGeneralMultipliers, layout);
        layout.gridx++;
        panel.add(pnlUsedPartsMultipliers, layout);
        layout.gridx++;
        panel.add(pnlOtherMultipliers, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "PriceMultipliersTab");
    }

    /**
     * Creates and configures the general multipliers panel, which includes labels
     * and spinners for various pricing multipliers such as common parts, Inner Sphere
     * units, Inner Sphere parts, Clan units, Clan parts, and mixed tech units.
     * The panel is structured using a grid layout for organized placement of components.
     *
     * @return a JPanel containing the components for setting general multipliers.
     */
    private JPanel createGeneralMultipliersPanel() {
        // Contents
        lblCommonPartPriceMultiplier = new CampaignOptionsLabel("CommonPartPriceMultiplier");
        spnCommonPartPriceMultiplier = new CampaignOptionsSpinner("CommonPartPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        lblInnerSphereUnitPriceMultiplier = new CampaignOptionsLabel("InnerSphereUnitPriceMultiplier");
        spnInnerSphereUnitPriceMultiplier = new CampaignOptionsSpinner("InnerSphereUnitPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        lblInnerSpherePartPriceMultiplier = new CampaignOptionsLabel("InnerSpherePartPriceMultiplier");
        spnInnerSpherePartPriceMultiplier = new CampaignOptionsSpinner("InnerSpherePartPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        lblClanUnitPriceMultiplier = new CampaignOptionsLabel("ClanUnitPriceMultiplier");
        spnClanUnitPriceMultiplier = new CampaignOptionsSpinner("ClanUnitPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        lblClanPartPriceMultiplier = new CampaignOptionsLabel("ClanPartPriceMultiplier");
        spnClanPartPriceMultiplier = new CampaignOptionsSpinner("ClanPartPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        lblMixedTechUnitPriceMultiplier = new CampaignOptionsLabel("MixedTechUnitPriceMultiplier");
        spnMixedTechUnitPriceMultiplier = new CampaignOptionsSpinner("MixedTechUnitPriceMultiplier",
            1.0, 0.1, 100, 0.1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("GeneralMultipliersPanel", true,
            "GeneralMultipliersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblCommonPartPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnCommonPartPriceMultiplier, layout);
        layout.gridx++;
        panel.add(lblMixedTechUnitPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnMixedTechUnitPriceMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblInnerSphereUnitPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnInnerSphereUnitPriceMultiplier, layout);
        layout.gridx++;
        panel.add(lblInnerSpherePartPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnInnerSpherePartPriceMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblClanUnitPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnClanUnitPriceMultiplier, layout);
        layout.gridx++;
        panel.add(lblClanPartPriceMultiplier, layout);
        layout.gridx++;
        panel.add(spnClanPartPriceMultiplier, layout);

        return panel;
    }

    /**
     * Creates and returns a JPanel for configuring used parts price multipliers
     * based on part quality. Each part quality level is represented with a label
     * and a spinner for adjusting the multiplier value.
     * <p>
     * The spinners are initialized with a range of values from 0.00 to 1.00,
     * incrementing by 0.05, and include formatting for two decimal places.
     * Additionally, the alignment of the spinner text fields is set to left.
     * <p>
     * The panel is arranged using GridBagLayout to ensure proper alignment
     * between labels and spinners for each quality level.
     *
     * @return A JPanel containing labels and spinners for used parts price multipliers.
     */
    private JPanel createUsedPartsMultiplierPanel() {
        boolean reverseQualities = campaign.getCampaignOptions().isReverseQualityNames();

        // Contents
        lblUsedPartPriceMultipliers = new JLabel[QUALITY_F.ordinal() + 1];
        spnUsedPartPriceMultipliers = new JSpinner[QUALITY_F.ordinal() + 1];

        for (PartQuality partQuality : PartQuality.values()) {
            final String qualityLevel = partQuality.toName(reverseQualities);
            int ordinal = partQuality.ordinal();

            lblUsedPartPriceMultipliers[ordinal] = new JLabel(qualityLevel);
            lblUsedPartPriceMultipliers[ordinal].setName("lbl" + qualityLevel);

            spnUsedPartPriceMultipliers[ordinal] = new JSpinner(
                new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[ordinal].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[ordinal]
                .setEditor(new NumberEditor(spnUsedPartPriceMultipliers[ordinal], "0.00"));

            DefaultEditor editor = (DefaultEditor) spnUsedPartPriceMultipliers[ordinal].getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UsedPartsMultiplierPanel", true,
            "UsedPartsMultiplierPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;

        for (int i = 0; i < 6; i++) {
            layout.gridx = 0;
            layout.gridy = i;
            panel.add(lblUsedPartPriceMultipliers[i], layout);
            layout.gridx++;
            panel.add(spnUsedPartPriceMultipliers[i], layout);
        }

        return panel;
    }

    /**
     * Creates and returns a JPanel configured with components for adjusting
     * multipliers related to damaged parts value, unrepairable parts value,
     * and cancelled order refunds. Each multiplier is represented with a label
     * and an associated configurable spinner control.
     *
     * @return a JPanel instance containing the components for configuring the multipliers.
     */
    private JPanel createOtherMultipliersPanel() {
        // Contents
        lblDamagedPartsValueMultiplier = new CampaignOptionsLabel("DamagedPartsValueMultiplier");
        spnDamagedPartsValueMultiplier = new CampaignOptionsSpinner("DamagedPartsValueMultiplier",
            0.33, 0.00, 1.00, 0.05);

        lblUnrepairablePartsValueMultiplier = new CampaignOptionsLabel("UnrepairablePartsValueMultiplier");
        spnUnrepairablePartsValueMultiplier = new CampaignOptionsSpinner("UnrepairablePartsValueMultiplier",
            0.10, 0.00, 1.00, 0.05);

        lblCancelledOrderRefundMultiplier = new CampaignOptionsLabel("CancelledOrderRefundMultiplier");
        spnCancelledOrderRefundMultiplier = new CampaignOptionsSpinner("CancelledOrderRefundMultiplier",
            0.50, 0.00, 1.00, 0.05);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OtherMultipliersPanel", true,
            "OtherMultipliersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblDamagedPartsValueMultiplier, layout);
        layout.gridx++;
        panel.add(spnDamagedPartsValueMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblUnrepairablePartsValueMultiplier, layout);
        layout.gridx++;
        panel.add(spnUnrepairablePartsValueMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblCancelledOrderRefundMultiplier, layout);
        layout.gridx++;
        panel.add(spnCancelledOrderRefundMultiplier, layout);

        return panel;
    }

    /**
     * Applies the specified campaign options to the corresponding campaign settings.
     * If no campaign options are provided, default options are used instead.
     *
     * @param presetCampaignOptions
     *        The campaign options to be applied. If null, default campaign options
     *        are applied.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General Options
        options.setLoanLimits(useLoanLimitsBox.isSelected());
        options.setUsePercentageMaint(usePercentageMaintenanceBox.isSelected());
        options.setUseExtendedPartsModifier(useExtendedPartsModifierBox.isSelected());
        options.setShowPeacetimeCost(usePeacetimeCostBox.isSelected());
        options.setShowPeacetimeCost(showPeacetimeCostBox.isSelected());
        options.setFinancialYearDuration(comboFinancialYearDuration.getSelectedItem());
        options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExportBox.isSelected());
        options.setPayForParts(payForPartsBox.isSelected());
        options.setPayForRepairs(payForRepairsBox.isSelected());
        options.setPayForUnits(payForUnitsBox.isSelected());
        options.setPayForSalaries(payForSalariesBox.isSelected());
        options.setPayForOverhead(payForOverheadBox.isSelected());
        options.setPayForMaintain(payForMaintainBox.isSelected());
        options.setPayForTransport(payForTransportBox.isSelected());
        options.setPayForRecruitment(payForRecruitmentBox.isSelected());
        options.setSellUnits(sellUnitsBox.isSelected());
        options.setSellParts(sellPartsBox.isSelected());
        options.setUseTaxes(chkUseTaxes.isSelected());
        options.setTaxesPercentage((int) spnTaxesPercentage.getValue());
        options.setUseShareSystem(chkUseShareSystem.isSelected());
        options.setSharesForAll(chkSharesForAll.isSelected());

        // Price Multipliers
        options.setCommonPartPriceMultiplier((double) spnCommonPartPriceMultiplier.getValue());
        options.setInnerSphereUnitPriceMultiplier((double) spnInnerSphereUnitPriceMultiplier.getValue());
        options.setInnerSpherePartPriceMultiplier((double) spnInnerSpherePartPriceMultiplier.getValue());
        options.setClanUnitPriceMultiplier((double) spnClanUnitPriceMultiplier.getValue());
        options.setClanPartPriceMultiplier((double) spnClanPartPriceMultiplier.getValue());
        options.setMixedTechUnitPriceMultiplier((double) spnMixedTechUnitPriceMultiplier.getValue());
        for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
            options.getUsedPartPriceMultipliers()[i] = (Double) spnUsedPartPriceMultipliers[i]
                .getValue();
        }
        options.setDamagedPartsValueMultiplier((double) spnDamagedPartsValueMultiplier.getValue());
        options.setUnrepairablePartsValueMultiplier((double) spnUnrepairablePartsValueMultiplier.getValue());
        options.setCancelledOrderRefundMultiplier((double) spnCancelledOrderRefundMultiplier.getValue());
    }

    /**
     * Loads configuration values from the current campaign options to populate
     * the financial settings and related UI components in the `FinancesTab`.
     * <p>
     * This method is a convenience overload that invokes the overloaded
     * {@link #loadValuesFromCampaignOptions(CampaignOptions)} method with a
     * `null` parameter, ensuring that default campaign options will be loaded.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies the values from the provided campaign options or the default campaign options
     * if the provided options are null. Updates various UI components and internal variables based
     * on the configuration of the campaign options.
     *
     * @param presetCampaignOptions the campaign options to load values from; if null, the default
     *                              campaign options will be used
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General Options
        useLoanLimitsBox.setSelected(options.isUseLoanLimits());
        usePercentageMaintenanceBox.setSelected(options.isUsePercentageMaint());
        useExtendedPartsModifierBox.setSelected(options.isUseExtendedPartsModifier());
        usePeacetimeCostBox.setSelected(options.isShowPeacetimeCost());
        showPeacetimeCostBox.setSelected(options.isShowPeacetimeCost());
        comboFinancialYearDuration = new MMComboBox<>("comboFinancialYearDuration",
                FinancialYearDuration.values());
        newFinancialYearFinancesToCSVExportBox.setSelected(options.isNewFinancialYearFinancesToCSVExport());
        payForPartsBox.setSelected(options.isPayForParts());
        payForRepairsBox.setSelected(options.isPayForRepairs());
        payForUnitsBox.setSelected(options.isPayForUnits());
        payForSalariesBox.setSelected(options.isPayForSalaries());
        payForOverheadBox.setSelected(options.isPayForOverhead());
        payForMaintainBox.setSelected(options.isPayForMaintain());
        payForTransportBox.setSelected(options.isPayForTransport());
        payForRecruitmentBox.setSelected(options.isPayForRecruitment());
        sellUnitsBox.setSelected(options.isSellUnits());
        sellPartsBox.setSelected(options.isSellParts());
        chkUseTaxes.setSelected(options.isUseTaxes());
        spnTaxesPercentage.setValue(options.getTaxesPercentage());
        chkUseShareSystem.setSelected(options.isUseShareSystem());
        chkSharesForAll.setSelected(options.isSharesForAll());

        // Price Multipliers
        spnCommonPartPriceMultiplier.setValue(options.getCommonPartPriceMultiplier());
        spnInnerSphereUnitPriceMultiplier.setValue(options.getInnerSphereUnitPriceMultiplier());
        spnInnerSpherePartPriceMultiplier.setValue(options.getInnerSpherePartPriceMultiplier());
        spnClanUnitPriceMultiplier.setValue(options.getClanUnitPriceMultiplier());
        spnClanPartPriceMultiplier.setValue(options.getClanPartPriceMultiplier());
        spnMixedTechUnitPriceMultiplier.setValue(options.getMixedTechUnitPriceMultiplier());
        for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
            spnUsedPartPriceMultipliers[i].setValue(options.getUsedPartPriceMultipliers()[i]);
        }
        spnDamagedPartsValueMultiplier.setValue(options.getDamagedPartsValueMultiplier());
        spnUnrepairablePartsValueMultiplier.setValue(options.getUnrepairablePartsValueMultiplier());
        spnCancelledOrderRefundMultiplier.setValue(options.getCancelledOrderRefundMultiplier());
    }
}
