/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.parts.enums.PartQuality.QUALITY_F;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.GridBagConstraints;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The FinancesTab class represents a UI tab within a larger financial management system for a campaign. It provides
 * panels, checkboxes, spinners, combo boxes, and other controls to manage and configure various financial options,
 * payments, sales, taxes, shares, and price multipliers for the campaign.
 * <p>
 * It is primarily composed of multiple `JPanel` sections organized using `GroupLayout` for modularity and clarity.
 */
public class FinancesTab {
    private final CampaignOptions campaignOptions;
      private FinancesDraft draft;
      private boolean generalOptionsPageCreated;
      private boolean priceMultipliersPageCreated;

    //start General Options
    private CampaignOptionsHeaderPanel financesGeneralOptions;
    private JPanel pnlGeneralOptions;
    private JCheckBox useLoanLimitsBox;
    private JCheckBox usePercentageMaintenanceBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox showPeacetimeCostBox;
    private JLabel lblFinancialYearDuration;
    private MMComboBox<FinancialYearDuration> comboFinancialYearDuration;
    private JCheckBox newFinancialYearFinancesToCSVExportBox;
    private JCheckBox chkSimulateGrayMonday;

    private JPanel pnlPayments;
    private JCheckBox payForPartsBox;
    private JCheckBox payForRepairsBox;
    private JCheckBox payForUnitsBox;
    private JCheckBox payForSalariesBox;
    private JCheckBox payForOverheadBox;
    private JCheckBox payForMaintainBox;
    private JCheckBox payForTransportBox;
    private JCheckBox payForRecruitmentBox;
    private JCheckBox payForFoodBox;
    private JCheckBox payForHousingBox;


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

    private JPanel pnlRentedFacilities;
    private JLabel lblRentedFacilitiesCostHospitalBeds;
    private JSpinner spnRentedFacilitiesCostHospitalBeds;
    private JLabel lblRentedFacilitiesCostKitchens;
    private JSpinner spnRentedFacilitiesCostKitchens;
    private JLabel lblRentedFacilitiesCostHoldingCells;
    private JSpinner spnRentedFacilitiesCostHoldingCells;
    private JLabel lblRentedFacilitiesCostRepairBays;
    private JSpinner spnRentedFacilitiesCostRepairBays;
    //end General Options

    //start Price Multipliers
    private CampaignOptionsHeaderPanel priceMultipliersHeader;
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
     * Constructs a `FinancesTab` instance which manages the financial settings and configurations for a specific
     * campaign.
     *
     * @param campaign The `Campaign` object that this `FinancesTab` will be associated with. Provides access to
     *                 campaign-related options and data.
     */
    public FinancesTab(Campaign campaign) {
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
            loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the primary components and subcomponents of the `FinancesTab`. Specifically, sets up the 'General
     * Options' and 'Price Multipliers' tabs through their respective initialization methods. This method ensures that
     * the tabs are prepared prior to being displayed or used.
     */
    private void initialize() {
        initializeGeneralOptionsTab();
        initializePriceMultipliersTab();
    }

    /**
     * Initializes the General Options tab within the application's UI.
     * <p>
     * This method sets up various UI components and panels that provide configurable options for general settings,
     * payments, sales, other systems, taxes, and shares. Components include checkboxes, labels, spinners, and combo
     * boxes that allow the user to interact with and configure these settings.
     * <p>
     * All UI components are initialized, but additional configuration such as layout placements, listeners, or actual
     * visibility might need to be completed separately.
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
        comboFinancialYearDuration = new MMComboBox<>("comboFinancialYearDuration", FinancialYearDuration.values());

        newFinancialYearFinancesToCSVExportBox = new JCheckBox();

        chkSimulateGrayMonday = new JCheckBox();

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
        payForFoodBox = new JCheckBox();
        payForHousingBox = new JCheckBox();

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
        pnlShares = new JPanel();
        chkUseShareSystem = new JCheckBox();
        chkSharesForAll = new JCheckBox();

        // Rented Facilities
        pnlRentedFacilities = new JPanel();
        lblRentedFacilitiesCostHospitalBeds = new JLabel();
        spnRentedFacilitiesCostHospitalBeds = new JSpinner();
        lblRentedFacilitiesCostKitchens = new JLabel();
        spnRentedFacilitiesCostKitchens = new JSpinner();
        lblRentedFacilitiesCostHoldingCells = new JLabel();
        spnRentedFacilitiesCostHoldingCells = new JSpinner();
        lblRentedFacilitiesCostRepairBays = new JLabel();
        spnRentedFacilitiesCostRepairBays = new JSpinner();
    }

    /**
     * Creates and configures the Finances General Options tab, assembling its components, layout, and panels which
     * include general options, other systems, payments, and sales. This method initializes required sub-panels and
     * arranges them within the overall structure to create a fully constructed tab for financial general options.
     *
     * @return A fully configured JPanel representing the Finances General Options tab.
     */
    public JPanel createFinancesGeneralOptionsTab() {
        // Header
        financesGeneralOptions = new CampaignOptionsHeaderPanel("FinancesGeneralTab",
              getImageDirectory() + "logo_star_league.png", 8);

        // Contents
        pnlGeneralOptions = createGeneralOptionsPanel();
        pnlOtherSystems = createOtherSystemsPanel();

        pnlPayments = createPaymentsPanel();
        pnlSales = createSalesPanel();
      generalOptionsPageCreated = true;
      updateGeneralControlsFromDraft();

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
        panel.add(financesGeneralOptions, layoutParent);

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
     * Creates and configures a payments panel with various checkbox options for payment categories such as parts,
     * repairs, units, salaries, overhead, maintenance, transport, and recruitment. The layout of the panel organizes
     * the checkboxes in a grid-based format.
     *
     * @return a JPanel instance containing the configured payment options checkboxes.
     */
    private JPanel createPaymentsPanel() {
        // Contents
        payForPartsBox = new CampaignOptionsCheckBox("PayForPartsBox");
        payForPartsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForPartsBox"));
        payForRepairsBox = new CampaignOptionsCheckBox("PayForRepairsBox");
        payForRepairsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForRepairsBox"));
        payForUnitsBox = new CampaignOptionsCheckBox("PayForUnitsBox");
        payForUnitsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForUnitsBox"));
        payForSalariesBox = new CampaignOptionsCheckBox("PayForSalariesBox");
        payForSalariesBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForSalariesBox"));
        payForOverheadBox = new CampaignOptionsCheckBox("PayForOverheadBox");
        payForOverheadBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForOverheadBox"));
        payForMaintainBox = new CampaignOptionsCheckBox("PayForMaintainBox");
        payForMaintainBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForMaintainBox"));
        payForTransportBox = new CampaignOptionsCheckBox("PayForTransportBox");
        payForTransportBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForTransportBox"));
        payForRecruitmentBox = new CampaignOptionsCheckBox("PayForRecruitmentBox");
        payForRecruitmentBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForRecruitmentBox"));
        payForFoodBox = new CampaignOptionsCheckBox("PayForFoodBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForFoodBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForFoodBox"));
        payForHousingBox = new CampaignOptionsCheckBox("PayForHousingBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForHousingBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForHousingBox"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PaymentsPanel", true, "PaymentsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(payForPartsBox, layout);
        layout.gridx++;
        panel.add(payForRepairsBox, layout);
        layout.gridx++;
        panel.add(payForUnitsBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForSalariesBox, layout);
        layout.gridx++;
        panel.add(payForOverheadBox, layout);
        layout.gridx++;
        panel.add(payForMaintainBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForTransportBox, layout);
        layout.gridx++;
        panel.add(payForRecruitmentBox, layout);
        layout.gridx++;
        panel.add(payForFoodBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(payForHousingBox, layout);

        return panel;
    }

    /**
     * Constructs and returns a {@link JPanel} for the 'Other Systems Panel'. This panel combines two sub-panels: 'Taxes
     * Panel' and 'Shares Panel'. Each sub-panel is added sequentially to the main panel using a grid-bag layout. These
     * panels are organized vertically in the resulting panel.
     *
     * @return {@link JPanel} representing the 'Other Systems Panel', containing the 'Taxes Panel' and 'Shares Panel'.
     */
    private JPanel createOtherSystemsPanel() {
        // Contents
        pnlTaxes = createTaxesPanel();
        pnlShares = createSharesPanel();
        pnlRentedFacilities = createRentedFacilitiesPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OtherSystemsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(pnlTaxes, layout);

        layout.gridy++;
        panel.add(pnlShares, layout);

        layout.gridy++;
        panel.add(pnlRentedFacilities, layout);

        return panel;
    }

    /**
     * Creates and initializes the General Options Panel with various configurable options related to loan limits,
     * maintenance, parts modifiers, peacetime costs, and financial year settings. The panel includes checkboxes and
     * labels for easy user interaction and configuration of these parameters.
     *
     * @return A JPanel containing the general options components laid out in a structured format.
     */
    private JPanel createGeneralOptionsPanel() {
        // Contents
        useLoanLimitsBox = new CampaignOptionsCheckBox("UseLoanLimitsBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        useLoanLimitsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UseLoanLimitsBox"));
        usePercentageMaintenanceBox = new CampaignOptionsCheckBox("UsePercentageMaintenanceBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        usePercentageMaintenanceBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "UsePercentageMaintenanceBox"));
        useExtendedPartsModifierBox = new CampaignOptionsCheckBox("UseExtendedPartsModifierBox");
        useExtendedPartsModifierBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "UseExtendedPartsModifierBox"));
        usePeacetimeCostBox = new CampaignOptionsCheckBox("UsePeacetimeCostBox");
        usePeacetimeCostBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UsePeacetimeCostBox"));
        showPeacetimeCostBox = new CampaignOptionsCheckBox("ShowPeacetimeCostBox");
        showPeacetimeCostBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "ShowPeacetimeCostBox"));

        lblFinancialYearDuration = new CampaignOptionsLabel("FinancialYearDuration",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFinancialYearDuration.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "FinancialYearDuration"));
        comboFinancialYearDuration.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "FinancialYearDuration"));

        newFinancialYearFinancesToCSVExportBox = new CampaignOptionsCheckBox("NewFinancialYearFinancesToCSVExportBox");
        newFinancialYearFinancesToCSVExportBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "NewFinancialYearFinancesToCSVExportBox"));

        chkSimulateGrayMonday = new CampaignOptionsCheckBox("SimulateGrayMonday");
        chkSimulateGrayMonday.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SimulateGrayMonday"));

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

        layout.gridy++;
        panel.add(chkSimulateGrayMonday, layout);

        return panel;
    }

    /**
     * Creates and configures the sales panel within the finance tab. The panel contains checkboxes for options related
     * to sales, including "Sell Units" and "Sell Parts". These checkboxes are added to a layout that organizes the
     * components vertically.
     *
     * @return A JPanel instance containing the configured sales options.
     */
    private JPanel createSalesPanel() {
        // Contents
        sellUnitsBox = new CampaignOptionsCheckBox("SellUnitsBox");
        sellUnitsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SellUnitsBox"));
        sellPartsBox = new CampaignOptionsCheckBox("SellPartsBox");
        sellPartsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SellPartsBox"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalesPanel", true, "SalesPanel");
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
     * Creates and returns a JPanel representing the taxes panel in the campaign options. This panel includes a checkbox
     * to enable or disable taxes and a spinner to set the percentage of taxes, along with corresponding labels.
     *
     * @return the configured JPanel containing the components for the taxes panel.
     */
    private JPanel createTaxesPanel() {
        // Contents
        chkUseTaxes = new CampaignOptionsCheckBox("UseTaxesBox");
        chkUseTaxes.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UseTaxesBox"));

        lblTaxesPercentage = new CampaignOptionsLabel("TaxesPercentage");
        lblTaxesPercentage.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "TaxesPercentage"));
        spnTaxesPercentage = new CampaignOptionsSpinner("TaxesPercentage", 30, 1, 100, 1);
        spnTaxesPercentage.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "TaxesPercentage"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TaxesPanel", true, "TaxesPanel",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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
     * The panel is laid out using grid-based constraints to position the components in a structured vertical
     * arrangement.
     *
     * @return A JPanel containing the configured components for the 'Shares Panel'.
     */
    private JPanel createSharesPanel() {
        // Contents
        chkUseShareSystem = new CampaignOptionsCheckBox("UseShareSystem");
        chkUseShareSystem.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UseShareSystem"));
        chkSharesForAll = new CampaignOptionsCheckBox("SharesForAll");
        chkSharesForAll.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SharesForAll"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SharesPanel", true, "SharesPanel",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUseShareSystem, layout);

        layout.gridy++;
        panel.add(chkSharesForAll, layout);

        return panel;
    }

    private JPanel createRentedFacilitiesPanel() {
        // Contents
        lblRentedFacilitiesCostHospitalBeds = new CampaignOptionsLabel("RentedFacilitiesCostHospitalBeds",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostHospitalBeds.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostHospitalBeds"));
        spnRentedFacilitiesCostHospitalBeds = new CampaignOptionsSpinner("RentedFacilitiesCostHospitalBeds",
              4100, 0, 1000000, 1);
        spnRentedFacilitiesCostHospitalBeds.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostHospitalBeds"));

        lblRentedFacilitiesCostKitchens = new CampaignOptionsLabel("RentedFacilitiesCostKitchens",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostKitchens.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostKitchens"));
        spnRentedFacilitiesCostKitchens = new CampaignOptionsSpinner("RentedFacilitiesCostKitchens",
              3700, 0, 1000000, 1);
        spnRentedFacilitiesCostKitchens.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostKitchens"));

        lblRentedFacilitiesCostHoldingCells = new CampaignOptionsLabel("RentedFacilitiesCostHoldingCells",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostHoldingCells.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostHoldingCells"));
        spnRentedFacilitiesCostHoldingCells = new CampaignOptionsSpinner("RentedFacilitiesCostHoldingCells",
              6400, 0, 1000000, 1);
        spnRentedFacilitiesCostHoldingCells.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostHoldingCells"));

        lblRentedFacilitiesCostRepairBays = new CampaignOptionsLabel("RentedFacilitiesCostRepairBays",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostRepairBays.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostRepairBays"));
        spnRentedFacilitiesCostRepairBays = new CampaignOptionsSpinner("RentedFacilitiesCostRepairBays",
              25000, 0, 1000000, 1);
        spnRentedFacilitiesCostRepairBays.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
              "RentedFacilitiesCostRepairBays"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("RentedFacilitiesPanel", true, "RentedFacilitiesPanel",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblRentedFacilitiesCostHospitalBeds, layout);
        layout.gridx++;
        panel.add(spnRentedFacilitiesCostHospitalBeds, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRentedFacilitiesCostKitchens, layout);
        layout.gridx++;
        panel.add(spnRentedFacilitiesCostKitchens, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRentedFacilitiesCostHoldingCells, layout);
        layout.gridx++;
        panel.add(spnRentedFacilitiesCostHoldingCells, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRentedFacilitiesCostRepairBays, layout);
        layout.gridx++;
        panel.add(spnRentedFacilitiesCostRepairBays, layout);

        return panel;
    }

    /**
     * Initializes the components and layout for the price multipliers tab. This tab includes controls for setting
     * various price multipliers such as - General multipliers for unit and part prices. - Multipliers for used parts. -
     * Miscellaneous multipliers for damaged, unrepairable parts, and order refunds.
     * <p>
     * The method creates and assigns UI components including panels, labels, and spinners to their respective class
     * fields. Each field corresponds to a specific category of price multiplier.
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
     * Creates and returns a JPanel representing the "Price Multipliers" tab in the user interface. The method includes
     * a header section, general multipliers panel, used parts multipliers panel, and other multipliers panel. These
     * components are arranged using a specific layout and added to a parent panel.
     *
     * @return a JPanel representing the "Price Multipliers" tab with all its components and layout configured
     */
    public JPanel createPriceMultipliersTab() {
        // Header
        priceMultipliersHeader = new CampaignOptionsHeaderPanel("PriceMultipliersTab",
              getImageDirectory() + "logo_clan_stone_lion.png", true, true, 1);

        // Contents
        pnlGeneralMultipliers = createGeneralMultipliersPanel();
        pnlUsedPartsMultipliers = createUsedPartsMultiplierPanel();
        pnlOtherMultipliers = createOtherMultipliersPanel();
      priceMultipliersPageCreated = true;
      updatePriceMultiplierControlsFromDraft();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PriceMultipliersTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(priceMultipliersHeader, layout);

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
     * Creates and configures the general multipliers panel, which includes labels and spinners for various pricing
     * multipliers such as common parts, Inner Sphere units, Inner Sphere parts, Clan units, Clan parts, and mixed tech
     * units. The panel is structured using a grid layout for organized placement of components.
     *
     * @return a JPanel containing the components for setting general multipliers.
     */
    private JPanel createGeneralMultipliersPanel() {
        // Contents
        lblCommonPartPriceMultiplier = new CampaignOptionsLabel("CommonPartPriceMultiplier");
        lblCommonPartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "CommonPartPriceMultiplier"));
        spnCommonPartPriceMultiplier = new CampaignOptionsSpinner("CommonPartPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnCommonPartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "CommonPartPriceMultiplier"));

        lblInnerSphereUnitPriceMultiplier = new CampaignOptionsLabel("InnerSphereUnitPriceMultiplier");
        lblInnerSphereUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "InnerSphereUnitPriceMultiplier"));
        spnInnerSphereUnitPriceMultiplier = new CampaignOptionsSpinner("InnerSphereUnitPriceMultiplier",
              1.0,
              0.1,
              100,
              0.1);
        spnInnerSphereUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "InnerSphereUnitPriceMultiplier"));

        lblInnerSpherePartPriceMultiplier = new CampaignOptionsLabel("InnerSpherePartPriceMultiplier");
        lblInnerSpherePartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "InnerSpherePartPriceMultiplier"));
        spnInnerSpherePartPriceMultiplier = new CampaignOptionsSpinner("InnerSpherePartPriceMultiplier",
              1.0,
              0.1,
              100,
              0.1);
        spnInnerSpherePartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "InnerSpherePartPriceMultiplier"));

        lblClanUnitPriceMultiplier = new CampaignOptionsLabel("ClanUnitPriceMultiplier");
        lblClanUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "ClanUnitPriceMultiplier"));
        spnClanUnitPriceMultiplier = new CampaignOptionsSpinner("ClanUnitPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnClanUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "ClanUnitPriceMultiplier"));

        lblClanPartPriceMultiplier = new CampaignOptionsLabel("ClanPartPriceMultiplier");
        lblClanPartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "ClanPartPriceMultiplier"));
        spnClanPartPriceMultiplier = new CampaignOptionsSpinner("ClanPartPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnClanPartPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "ClanPartPriceMultiplier"));

        lblMixedTechUnitPriceMultiplier = new CampaignOptionsLabel("MixedTechUnitPriceMultiplier");
        lblMixedTechUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "MixedTechUnitPriceMultiplier"));
        spnMixedTechUnitPriceMultiplier = new CampaignOptionsSpinner("MixedTechUnitPriceMultiplier",
              1.0,
              0.1,
              100,
              0.1);
        spnMixedTechUnitPriceMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "MixedTechUnitPriceMultiplier"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("GeneralMultipliersPanel",
              true,
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
     * Creates and returns a JPanel for configuring used parts price multipliers based on part quality. Each part
     * quality level is represented with a label and a spinner for adjusting the multiplier value.
     * <p>
     * The spinners are initialized with a range of values from 0.00 to 1.00, incrementing by 0.05, and include
     * formatting for two decimal places. Additionally, the alignment of the spinner text fields is set to left.
     * <p>
     * The panel is arranged using GridBagLayout to ensure proper alignment between labels and spinners for each quality
     * level.
     *
     * @return A JPanel containing labels and spinners for used parts price multipliers.
     */
    private JPanel createUsedPartsMultiplierPanel() {
        // Contents
        lblUsedPartPriceMultipliers = new JLabel[QUALITY_F.ordinal() + 1];
        spnUsedPartPriceMultipliers = new JSpinner[QUALITY_F.ordinal() + 1];

        for (PartQuality partQuality : PartQuality.values()) {
            final String qualityLevel = partQuality.toName(false);
            int ordinal = partQuality.ordinal();

            lblUsedPartPriceMultipliers[ordinal] = new JLabel(qualityLevel);
            lblUsedPartPriceMultipliers[ordinal].setName("lbl" + qualityLevel);

            spnUsedPartPriceMultipliers[ordinal] = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[ordinal].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[ordinal].setEditor(new NumberEditor(spnUsedPartPriceMultipliers[ordinal],
                  "0.00"));

            DefaultEditor editor = (DefaultEditor) spnUsedPartPriceMultipliers[ordinal].getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UsedPartsMultiplierPanel",
              true,
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
     * Creates and returns a JPanel configured with components for adjusting multipliers related to damaged parts value,
     * unrepairable parts value, and cancelled order refunds. Each multiplier is represented with a label and an
     * associated configurable spinner control.
     *
     * @return a JPanel instance containing the components for configuring the multipliers.
     */
    private JPanel createOtherMultipliersPanel() {
        // Contents
        lblDamagedPartsValueMultiplier = new CampaignOptionsLabel("DamagedPartsValueMultiplier");
        lblDamagedPartsValueMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "DamagedPartsValueMultiplier"));
        spnDamagedPartsValueMultiplier = new CampaignOptionsSpinner("DamagedPartsValueMultiplier",
              0.33,
              0.00,
              1.00,
              0.05);
        spnDamagedPartsValueMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "DamagedPartsValueMultiplier"));

        lblUnrepairablePartsValueMultiplier = new CampaignOptionsLabel("UnrepairablePartsValueMultiplier");
        lblUnrepairablePartsValueMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "UnrepairablePartsValueMultiplier"));
        spnUnrepairablePartsValueMultiplier = new CampaignOptionsSpinner("UnrepairablePartsValueMultiplier",
              0.10,
              0.00,
              1.00,
              0.05);
        spnUnrepairablePartsValueMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "UnrepairablePartsValueMultiplier"));

        lblCancelledOrderRefundMultiplier = new CampaignOptionsLabel("CancelledOrderRefundMultiplier");
        lblCancelledOrderRefundMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "CancelledOrderRefundMultiplier"));
        spnCancelledOrderRefundMultiplier = new CampaignOptionsSpinner("CancelledOrderRefundMultiplier",
              0.50,
              0.00,
              1.00,
              0.05);
        spnCancelledOrderRefundMultiplier.addMouseListener(createTipPanelUpdater(priceMultipliersHeader,
              "CancelledOrderRefundMultiplier"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OtherMultipliersPanel", true, "OtherMultipliersPanel");
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
     * Applies the specified campaign options to the corresponding campaign settings. If no campaign options are
     * provided, default options are used instead.
     *
     * @param presetCampaignOptions The campaign options to be applied. If null, default campaign options are applied.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

            updateDraftFromCreatedControls();
            draft.applyTo(options);
    }

    /**
     * Loads configuration values from the current campaign options to populate the financial settings and related UI
     * components in the `FinancesTab`.
     * <p>
     * This method is a convenience overload that invokes the overloaded
     * {@link #loadValuesFromCampaignOptions(CampaignOptions)} method with a `null` parameter, ensuring that default
     * campaign options will be loaded.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies the values from the provided campaign options or the default campaign options if the provided
     * options are null. Updates various UI components and internal variables based on the configuration of the campaign
     * options.
     *
     * @param presetCampaignOptions the campaign options to load values from; if null, the default campaign options will
     *                              be used
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

            draft = new FinancesDraft(options);
            updateCreatedControlsFromDraft();
    }

      private void updateCreatedControlsFromDraft() {
            updateGeneralControlsFromDraft();
            updatePriceMultiplierControlsFromDraft();
      }

      private void updateGeneralControlsFromDraft() {
            if (!generalOptionsPageCreated || draft == null) {
                  return;
            }

            useLoanLimitsBox.setSelected(draft.useLoanLimits);
            usePercentageMaintenanceBox.setSelected(draft.usePercentageMaintenance);
            useExtendedPartsModifierBox.setSelected(draft.useExtendedPartsModifier);
            usePeacetimeCostBox.setSelected(draft.usePeacetimeCost);
            showPeacetimeCostBox.setSelected(draft.showPeacetimeCost);
            comboFinancialYearDuration.setSelectedItem(draft.financialYearDuration);
            newFinancialYearFinancesToCSVExportBox.setSelected(draft.newFinancialYearFinancesToCSVExport);
            chkSimulateGrayMonday.setSelected(draft.simulateGrayMonday);
            payForPartsBox.setSelected(draft.payForParts);
            payForRepairsBox.setSelected(draft.payForRepairs);
            payForUnitsBox.setSelected(draft.payForUnits);
            payForSalariesBox.setSelected(draft.payForSalaries);
            payForOverheadBox.setSelected(draft.payForOverhead);
            payForMaintainBox.setSelected(draft.payForMaintain);
            payForTransportBox.setSelected(draft.payForTransport);
            payForRecruitmentBox.setSelected(draft.payForRecruitment);
            payForFoodBox.setSelected(draft.payForFood);
            payForHousingBox.setSelected(draft.payForHousing);
            sellUnitsBox.setSelected(draft.sellUnits);
            sellPartsBox.setSelected(draft.sellParts);
            chkUseTaxes.setSelected(draft.useTaxes);
            spnTaxesPercentage.setValue(draft.taxesPercentage);
            chkUseShareSystem.setSelected(draft.useShareSystem);
            chkSharesForAll.setSelected(draft.sharesForAll);
            spnRentedFacilitiesCostHospitalBeds.setValue(draft.rentedFacilitiesCostHospitalBeds);
            spnRentedFacilitiesCostKitchens.setValue(draft.rentedFacilitiesCostKitchens);
            spnRentedFacilitiesCostHoldingCells.setValue(draft.rentedFacilitiesCostHoldingCells);
            spnRentedFacilitiesCostRepairBays.setValue(draft.rentedFacilitiesCostRepairBays);
      }

      private void updatePriceMultiplierControlsFromDraft() {
            if (!priceMultipliersPageCreated || draft == null) {
                  return;
            }

            spnCommonPartPriceMultiplier.setValue(draft.commonPartPriceMultiplier);
            spnInnerSphereUnitPriceMultiplier.setValue(draft.innerSphereUnitPriceMultiplier);
            spnInnerSpherePartPriceMultiplier.setValue(draft.innerSpherePartPriceMultiplier);
            spnClanUnitPriceMultiplier.setValue(draft.clanUnitPriceMultiplier);
            spnClanPartPriceMultiplier.setValue(draft.clanPartPriceMultiplier);
            spnMixedTechUnitPriceMultiplier.setValue(draft.mixedTechUnitPriceMultiplier);
            for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length, draft.usedPartPriceMultipliers.length); i++) {
                  spnUsedPartPriceMultipliers[i].setValue(draft.usedPartPriceMultipliers[i]);
            }
            spnDamagedPartsValueMultiplier.setValue(draft.damagedPartsValueMultiplier);
            spnUnrepairablePartsValueMultiplier.setValue(draft.unrepairablePartsValueMultiplier);
            spnCancelledOrderRefundMultiplier.setValue(draft.cancelledOrderRefundMultiplier);
      }

      private void updateDraftFromCreatedControls() {
            updateDraftFromGeneralControls();
            updateDraftFromPriceMultiplierControls();
      }

      private void updateDraftFromGeneralControls() {
            if (!generalOptionsPageCreated || draft == null) {
                  return;
            }

            draft.useLoanLimits = useLoanLimitsBox.isSelected();
            draft.usePercentageMaintenance = usePercentageMaintenanceBox.isSelected();
            draft.useExtendedPartsModifier = useExtendedPartsModifierBox.isSelected();
            draft.usePeacetimeCost = usePeacetimeCostBox.isSelected();
            draft.showPeacetimeCost = showPeacetimeCostBox.isSelected();
            draft.financialYearDuration = comboFinancialYearDuration.getSelectedItem();
            draft.newFinancialYearFinancesToCSVExport = newFinancialYearFinancesToCSVExportBox.isSelected();
            draft.simulateGrayMonday = chkSimulateGrayMonday.isSelected();
            draft.payForParts = payForPartsBox.isSelected();
            draft.payForRepairs = payForRepairsBox.isSelected();
            draft.payForUnits = payForUnitsBox.isSelected();
            draft.payForSalaries = payForSalariesBox.isSelected();
            draft.payForOverhead = payForOverheadBox.isSelected();
            draft.payForMaintain = payForMaintainBox.isSelected();
            draft.payForTransport = payForTransportBox.isSelected();
            draft.payForRecruitment = payForRecruitmentBox.isSelected();
            draft.payForFood = payForFoodBox.isSelected();
            draft.payForHousing = payForHousingBox.isSelected();
            draft.sellUnits = sellUnitsBox.isSelected();
            draft.sellParts = sellPartsBox.isSelected();
            draft.useTaxes = chkUseTaxes.isSelected();
            draft.taxesPercentage = (int) spnTaxesPercentage.getValue();
            draft.useShareSystem = chkUseShareSystem.isSelected();
            draft.sharesForAll = chkSharesForAll.isSelected();
            draft.rentedFacilitiesCostHospitalBeds = (int) spnRentedFacilitiesCostHospitalBeds.getValue();
            draft.rentedFacilitiesCostKitchens = (int) spnRentedFacilitiesCostKitchens.getValue();
            draft.rentedFacilitiesCostHoldingCells = (int) spnRentedFacilitiesCostHoldingCells.getValue();
            draft.rentedFacilitiesCostRepairBays = (int) spnRentedFacilitiesCostRepairBays.getValue();
      }

      private void updateDraftFromPriceMultiplierControls() {
            if (!priceMultipliersPageCreated || draft == null) {
                  return;
            }

            draft.commonPartPriceMultiplier = (double) spnCommonPartPriceMultiplier.getValue();
            draft.innerSphereUnitPriceMultiplier = (double) spnInnerSphereUnitPriceMultiplier.getValue();
            draft.innerSpherePartPriceMultiplier = (double) spnInnerSpherePartPriceMultiplier.getValue();
            draft.clanUnitPriceMultiplier = (double) spnClanUnitPriceMultiplier.getValue();
            draft.clanPartPriceMultiplier = (double) spnClanPartPriceMultiplier.getValue();
            draft.mixedTechUnitPriceMultiplier = (double) spnMixedTechUnitPriceMultiplier.getValue();
            for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length, draft.usedPartPriceMultipliers.length); i++) {
                  draft.usedPartPriceMultipliers[i] = (Double) spnUsedPartPriceMultipliers[i].getValue();
            }
            draft.damagedPartsValueMultiplier = (double) spnDamagedPartsValueMultiplier.getValue();
            draft.unrepairablePartsValueMultiplier = (double) spnUnrepairablePartsValueMultiplier.getValue();
            draft.cancelledOrderRefundMultiplier = (double) spnCancelledOrderRefundMultiplier.getValue();
      }

      private static class FinancesDraft {
            private boolean useLoanLimits;
            private boolean usePercentageMaintenance;
            private boolean useExtendedPartsModifier;
            private boolean usePeacetimeCost;
            private boolean showPeacetimeCost;
            private FinancialYearDuration financialYearDuration;
            private boolean newFinancialYearFinancesToCSVExport;
            private boolean simulateGrayMonday;
            private boolean payForParts;
            private boolean payForRepairs;
            private boolean payForUnits;
            private boolean payForSalaries;
            private boolean payForOverhead;
            private boolean payForMaintain;
            private boolean payForTransport;
            private boolean payForRecruitment;
            private boolean payForFood;
            private boolean payForHousing;
            private boolean sellUnits;
            private boolean sellParts;
            private boolean useTaxes;
            private int taxesPercentage;
            private boolean useShareSystem;
            private boolean sharesForAll;
            private int rentedFacilitiesCostHospitalBeds;
            private int rentedFacilitiesCostKitchens;
            private int rentedFacilitiesCostHoldingCells;
            private int rentedFacilitiesCostRepairBays;
            private double commonPartPriceMultiplier;
            private double innerSphereUnitPriceMultiplier;
            private double innerSpherePartPriceMultiplier;
            private double clanUnitPriceMultiplier;
            private double clanPartPriceMultiplier;
            private double mixedTechUnitPriceMultiplier;
            private double[] usedPartPriceMultipliers;
            private double damagedPartsValueMultiplier;
            private double unrepairablePartsValueMultiplier;
            private double cancelledOrderRefundMultiplier;

            private FinancesDraft(CampaignOptions options) {
                  useLoanLimits = options.isUseLoanLimits();
                  usePercentageMaintenance = options.isUsePercentageMaintenance();
                  useExtendedPartsModifier = options.isUseExtendedPartsModifier();
                  usePeacetimeCost = options.isUsePeacetimeCost();
                  showPeacetimeCost = options.isShowPeacetimeCost();
                  financialYearDuration = options.getFinancialYearDuration();
                  newFinancialYearFinancesToCSVExport = options.isNewFinancialYearFinancesToCSVExport();
                  simulateGrayMonday = options.isSimulateGrayMonday();
                  payForParts = options.isPayForParts();
                  payForRepairs = options.isPayForRepairs();
                  payForUnits = options.isPayForUnits();
                  payForSalaries = options.isPayForSalaries();
                  payForOverhead = options.isPayForOverhead();
                  payForMaintain = options.isPayForMaintain();
                  payForTransport = options.isPayForTransport();
                  payForRecruitment = options.isPayForRecruitment();
                  payForFood = options.isPayForFood();
                  payForHousing = options.isPayForHousing();
                  sellUnits = options.isSellUnits();
                  sellParts = options.isSellParts();
                  useTaxes = options.isUseTaxes();
                  taxesPercentage = options.getTaxesPercentage();
                  useShareSystem = options.isUseShareSystem();
                  sharesForAll = options.isSharesForAll();
                  rentedFacilitiesCostHospitalBeds = options.getRentedFacilitiesCostHospitalBeds();
                  rentedFacilitiesCostKitchens = options.getRentedFacilitiesCostKitchens();
                  rentedFacilitiesCostHoldingCells = options.getRentedFacilitiesCostHoldingCells();
                  rentedFacilitiesCostRepairBays = options.getRentedFacilitiesCostRepairBays();
                  commonPartPriceMultiplier = options.getCommonPartPriceMultiplier();
                  innerSphereUnitPriceMultiplier = options.getInnerSphereUnitPriceMultiplier();
                  innerSpherePartPriceMultiplier = options.getInnerSpherePartPriceMultiplier();
                  clanUnitPriceMultiplier = options.getClanUnitPriceMultiplier();
                  clanPartPriceMultiplier = options.getClanPartPriceMultiplier();
                  mixedTechUnitPriceMultiplier = options.getMixedTechUnitPriceMultiplier();
                  usedPartPriceMultipliers = Arrays.copyOf(options.getUsedPartPriceMultipliers(),
                          options.getUsedPartPriceMultipliers().length);
                  damagedPartsValueMultiplier = options.getDamagedPartsValueMultiplier();
                  unrepairablePartsValueMultiplier = options.getUnrepairablePartsValueMultiplier();
                  cancelledOrderRefundMultiplier = options.getCancelledOrderRefundMultiplier();
            }

            private void applyTo(CampaignOptions options) {
                  options.setLoanLimits(useLoanLimits);
                  options.setUsePercentageMaintenance(usePercentageMaintenance);
                  options.setUseExtendedPartsModifier(useExtendedPartsModifier);
                  options.setUsePeacetimeCost(usePeacetimeCost);
                  options.setShowPeacetimeCost(showPeacetimeCost);
                  options.setFinancialYearDuration(financialYearDuration);
                  options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExport);
                  options.setSimulateGrayMonday(simulateGrayMonday);
                  options.setPayForParts(payForParts);
                  options.setPayForRepairs(payForRepairs);
                  options.setPayForUnits(payForUnits);
                  options.setPayForSalaries(payForSalaries);
                  options.setPayForOverhead(payForOverhead);
                  options.setPayForMaintain(payForMaintain);
                  options.setPayForTransport(payForTransport);
                  options.setPayForRecruitment(payForRecruitment);
                  options.setPayForFood(payForFood);
                  options.setPayForHousing(payForHousing);
                  options.setSellUnits(sellUnits);
                  options.setSellParts(sellParts);
                  options.setUseTaxes(useTaxes);
                  options.setTaxesPercentage(taxesPercentage);
                  options.setUseShareSystem(useShareSystem);
                  options.setSharesForAll(sharesForAll);
                  options.setRentedFacilitiesCostHospitalBeds(rentedFacilitiesCostHospitalBeds);
                  options.setRentedFacilitiesCostKitchens(rentedFacilitiesCostKitchens);
                  options.setRentedFacilitiesCostHoldingCells(rentedFacilitiesCostHoldingCells);
                  options.setRentedFacilitiesCostRepairBays(rentedFacilitiesCostRepairBays);
                  options.setCommonPartPriceMultiplier(commonPartPriceMultiplier);
                  options.setInnerSphereUnitPriceMultiplier(innerSphereUnitPriceMultiplier);
                  options.setInnerSpherePartPriceMultiplier(innerSpherePartPriceMultiplier);
                  options.setClanUnitPriceMultiplier(clanUnitPriceMultiplier);
                  options.setClanPartPriceMultiplier(clanPartPriceMultiplier);
                  options.setMixedTechUnitPriceMultiplier(mixedTechUnitPriceMultiplier);
                  for (int i = 0; i < Math.min(options.getUsedPartPriceMultipliers().length,
                          usedPartPriceMultipliers.length); i++) {
                        options.getUsedPartPriceMultipliers()[i] = usedPartPriceMultipliers[i];
                  }
                  options.setDamagedPartsValueMultiplier(damagedPartsValueMultiplier);
                  options.setUnrepairablePartsValueMultiplier(unrepairablePartsValueMultiplier);
                  options.setCancelledOrderRefundMultiplier(cancelledOrderRefundMultiplier);
            }
      }
}
