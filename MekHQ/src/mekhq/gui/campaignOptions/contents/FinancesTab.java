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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The FinancesTab class represents a UI tab within a larger financial
 * management system for a campaign. It provides
 * panels, checkboxes, spinners, combo boxes, and other controls to manage and
 * configure various financial options,
 * payments, sales, taxes, shares, and price multipliers for the campaign.
 * <p>
 * It is primarily composed of multiple `JPanel` sections organized inside the campaign options page shell for
 * modularity and clarity.
 */
public class FinancesTab {
    private static final int FINANCES_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FINANCES_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int FINANCES_LABEL_CONTROL_GAP = 12;
    // Paired (4-column) grid widths for the Price Multipliers sections. The first
    // pair column is the label column plus
    // the form's label/control gap, so a two-column grid's column 3 sits where the
    // control column of 2-column form
    // sections does. The following pair is sized so the two-column grid's content
    // lands on the shared page-width floor
    // (measured: 312 + 303 -> 640px section). The control width keeps the spinners
    // uniform.
    private static final int FINANCES_GRID_FIRST_PAIR_COLUMN_WIDTH = FINANCES_LABEL_COLUMN_WIDTH
            + FINANCES_LABEL_CONTROL_GAP;
    private static final int FINANCES_GRID_FOLLOWING_PAIR_COLUMN_WIDTH = 303;
    private static final int FINANCES_GRID_CONTROL_COLUMN_WIDTH = 100;

    private final CampaignOptions campaignOptions;
    private FinancesOptionsModel model;
    private boolean generalOptionsPageCreated;
    private boolean priceMultipliersPageCreated;

    // start General Options
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
    // end General Options

    // start Price Multipliers
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
    // end Price Multipliers

    /**
     * Constructs a `FinancesTab` instance which manages the financial settings and
     * configurations for a specific
     * campaign.
     *
     * @param campaign The `Campaign` object that this `FinancesTab` will be
     *                 associated with. Provides access to
     *                 campaign-related options and data.
     */
    public FinancesTab(@Nonnull Campaign campaign) {
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the primary components and subcomponents of the `FinancesTab`.
     * Specifically, sets up the 'General
     * Options' and 'Price Multipliers' tabs through their respective initialization
     * methods. This method ensures that
     * the tabs are prepared prior to being displayed or used.
     */
    private void initialize() {
        initializeGeneralOptionsTab();
        initializePriceMultipliersTab();
    }

    /**
     * Initializes the General Options tab within the application's UI.
     * <p>
     * This method sets up various UI components and panels that provide
     * configurable options for general settings,
     * payments, sales, other systems, taxes, and shares. Components include
     * checkboxes, labels, spinners, and combo
     * boxes that allow the user to interact with and configure these settings.
     * <p>
     * All UI components are initialized, but additional configuration such as
     * layout placements, listeners, or actual
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
     * Creates and configures the Finances General Options tab, assembling its
     * components, layout, and panels which
     * include general options, other systems, payments, and sales. This method
     * initializes required sub-panels and
     * arranges them within the overall structure to create a fully constructed tab
     * for financial general options.
     *
     * @return A fully configured JPanel representing the Finances General Options
     *         tab.
     */
    public @Nonnull JPanel createFinancesGeneralOptionsTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_star_league.png";
        financesGeneralOptions = new CampaignOptionsHeaderPanel("FinancesGeneralTab",
                imageAddress, 8);

        // Contents
        pnlGeneralOptions = createGeneralOptionsPanel();
        pnlPayments = createPaymentsPanel();
        pnlSales = createSalesPanel();
        pnlTaxes = createTaxesPanel();
        pnlShares = createSharesPanel();
        pnlRentedFacilities = createRentedFacilitiesPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("FinancesGeneralTab", "FinancesGeneralTab", imageAddress)
                .header(financesGeneralOptions)
                .quote("financesGeneralTab")
                .section("lblFinancialRulesPanel.text",
                        "lblFinancialRulesPanel.summary",
                        pnlGeneralOptions)
                .section("lblPaymentsPanel.text",
                        "lblPaymentsPanel.summary",
                        pnlPayments)
                .section("lblSalesPanel.text",
                        "lblSalesPanel.summary",
                        pnlSales)
                .section("lblTaxesPanel.text",
                        "lblTaxesPanel.summary",
                        pnlTaxes,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblSharesPanel.text",
                        "lblSharesPanel.summary",
                        pnlShares,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblRentedFacilitiesPanel.text",
                        "lblRentedFacilitiesPanel.summary",
                        pnlRentedFacilities,
                        getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .build();

        generalOptionsPageCreated = true;
        updateGeneralControlsFromModel();

        return panel;
    }

    /**
     * Creates and configures a payments panel with various checkbox options for
     * payment categories such as parts,
     * repairs, units, salaries, overhead, maintenance, transport, and recruitment.
     * The layout of the panel organizes
     * the checkboxes in a grid-based format.
     *
     * @return a JPanel instance containing the configured payment options
     *         checkboxes.
     */
    private @Nonnull JPanel createPaymentsPanel() {
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
        payForRecruitmentBox
                .addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForRecruitmentBox"));
        payForFoodBox = new CampaignOptionsCheckBox("PayForFoodBox",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForFoodBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForFoodBox"));
        payForHousingBox = new CampaignOptionsCheckBox("PayForHousingBox",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForHousingBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "PayForHousingBox"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PaymentsPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                payForPartsBox,
                payForRepairsBox,
                payForUnitsBox,
                payForSalariesBox,
                payForOverheadBox,
                payForMaintainBox,
                payForTransportBox,
                payForRecruitmentBox,
                payForFoodBox,
                payForHousingBox);

        return panel;
    }

    /**
     * Creates and initializes the General Options Panel with various configurable
     * options related to loan limits,
     * maintenance, parts modifiers, peacetime costs, and financial year settings.
     * The panel includes checkboxes and
     * labels for easy user interaction and configuration of these parameters.
     *
     * @return A JPanel containing the general options components laid out in a
     *         structured format.
     */
    private @Nonnull JPanel createGeneralOptionsPanel() {
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
        showPeacetimeCostBox
                .addMouseListener(createTipPanelUpdater(financesGeneralOptions, "ShowPeacetimeCostBox"));

        lblFinancialYearDuration = new CampaignOptionsLabel("FinancialYearDuration",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFinancialYearDuration.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
                "FinancialYearDuration"));
        comboFinancialYearDuration.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
                "FinancialYearDuration"));

        newFinancialYearFinancesToCSVExportBox = new CampaignOptionsCheckBox(
                "NewFinancialYearFinancesToCSVExportBox");
        newFinancialYearFinancesToCSVExportBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions,
                "NewFinancialYearFinancesToCSVExportBox"));

        chkSimulateGrayMonday = new CampaignOptionsCheckBox("SimulateGrayMonday");
        chkSimulateGrayMonday.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SimulateGrayMonday"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("GeneralOptionsPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                useLoanLimitsBox,
                usePercentageMaintenanceBox,
                useExtendedPartsModifierBox,
                usePeacetimeCostBox,
                showPeacetimeCostBox,
                newFinancialYearFinancesToCSVExportBox,
                chkSimulateGrayMonday);
        panel.addRow(lblFinancialYearDuration, comboFinancialYearDuration);

        return panel;
    }

    /**
     * Creates and configures the sales panel within the finance tab. The panel
     * contains checkboxes for options related
     * to sales, including "Sell Units" and "Sell Parts". These checkboxes are added
     * to a layout that organizes the
     * components vertically.
     *
     * @return A JPanel instance containing the configured sales options.
     */
    private @Nonnull JPanel createSalesPanel() {
        // Contents
        sellUnitsBox = new CampaignOptionsCheckBox("SellUnitsBox");
        sellUnitsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SellUnitsBox"));
        sellPartsBox = new CampaignOptionsCheckBox("SellPartsBox");
        sellPartsBox.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SellPartsBox"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SalesPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2, sellUnitsBox, sellPartsBox);

        return panel;
    }

    /**
     * Creates and returns a JPanel representing the taxes panel in the campaign
     * options. This panel includes a checkbox
     * to enable or disable taxes and a spinner to set the percentage of taxes,
     * along with corresponding labels.
     *
     * @return the configured JPanel containing the components for the taxes panel.
     */
    private @Nonnull JPanel createTaxesPanel() {
        // Contents
        chkUseTaxes = new CampaignOptionsCheckBox("UseTaxesBox");
        chkUseTaxes.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UseTaxesBox"));

        lblTaxesPercentage = new CampaignOptionsLabel("TaxesPercentage");
        lblTaxesPercentage.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "TaxesPercentage"));
        spnTaxesPercentage = new CampaignOptionsSpinner("TaxesPercentage", 30, 1, 100, 1);
        spnTaxesPercentage.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "TaxesPercentage"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TaxesPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseTaxes);
        panel.addRow(lblTaxesPercentage, spnTaxesPercentage);

        return panel;
    }

    /**
     * Creates and returns a JPanel representing the 'Shares Panel' within the
     * finance tab.
     * <p>
     * The panel is laid out using grid-based constraints to position the components
     * in a structured vertical
     * arrangement.
     *
     * @return A JPanel containing the configured components for the 'Shares Panel'.
     */
    private @Nonnull JPanel createSharesPanel() {
        // Contents
        chkUseShareSystem = new CampaignOptionsCheckBox("UseShareSystem");
        chkUseShareSystem.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "UseShareSystem"));
        chkSharesForAll = new CampaignOptionsCheckBox("SharesForAll");
        chkSharesForAll.addMouseListener(createTipPanelUpdater(financesGeneralOptions, "SharesForAll"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SharesPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2, chkUseShareSystem, chkSharesForAll);

        return panel;
    }

    private @Nonnull JPanel createRentedFacilitiesPanel() {
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RentedFacilitiesPanel",
                FINANCES_LABEL_COLUMN_WIDTH,
                FINANCES_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRentedFacilitiesCostHospitalBeds, spnRentedFacilitiesCostHospitalBeds);
        panel.addRow(lblRentedFacilitiesCostKitchens, spnRentedFacilitiesCostKitchens);
        panel.addRow(lblRentedFacilitiesCostHoldingCells, spnRentedFacilitiesCostHoldingCells);
        panel.addRow(lblRentedFacilitiesCostRepairBays, spnRentedFacilitiesCostRepairBays);

        return panel;
    }

    /**
     * Initializes the components and layout for the price multipliers tab. This tab
     * includes controls for setting
     * various price multipliers such as - General multipliers for unit and part
     * prices. - Multipliers for used parts. -
     * Miscellaneous multipliers for damaged, unrepairable parts, and order refunds.
     * <p>
     * The method creates and assigns UI components including panels, labels, and
     * spinners to their respective class
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
     * Builds the Price Multipliers tab.
     *
     * @return a JPanel representing the Price Multipliers tab
     */
    public @Nonnull JPanel createPriceMultipliersTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_stone_lion.png";
        priceMultipliersHeader = new CampaignOptionsHeaderPanel("PriceMultipliersTab",
                imageAddress,
                1);

        // Contents
        pnlGeneralMultipliers = createGeneralMultipliersPanel();
        pnlUsedPartsMultipliers = createUsedPartsMultiplierPanel();
        pnlOtherMultipliers = createOtherMultipliersPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("PriceMultipliersTab", "PriceMultipliersTab", imageAddress)
                .header(priceMultipliersHeader)
                .intro("lblPriceMultipliersTabBody.text")
                .quote("priceMultipliersTab")
                .section("lblGeneralMultipliersPanel.text",
                        "lblGeneralMultipliersPanel.summary",
                        pnlGeneralMultipliers)
                .section("lblUsedPartsMultiplierPanel.text",
                        "lblUsedPartsMultiplierPanel.summary",
                        pnlUsedPartsMultipliers)
                .section("lblOtherMultipliersPanel.text",
                        "lblOtherMultipliersPanel.summary",
                        pnlOtherMultipliers)
                .build();

        priceMultipliersPageCreated = true;
        updatePriceMultiplierControlsFromModel();

        return panel;
    }

    /**
     * Creates and configures the general multipliers panel, which includes labels
     * and spinners for various pricing
     * multipliers such as common parts, Inner Sphere units, Inner Sphere parts,
     * Clan units, Clan parts, and mixed tech
     * units. The panel is structured using a grid layout for organized placement of
     * components.
     *
     * @return a JPanel containing the components for setting general multipliers.
     */
    private @Nonnull JPanel createGeneralMultipliersPanel() {
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
        JComponent[] labels = { lblCommonPartPriceMultiplier, lblMixedTechUnitPriceMultiplier,
                lblInnerSphereUnitPriceMultiplier, lblInnerSpherePartPriceMultiplier,
                lblClanUnitPriceMultiplier, lblClanPartPriceMultiplier };
        JComponent[] controls = { spnCommonPartPriceMultiplier, spnMixedTechUnitPriceMultiplier,
                spnInnerSphereUnitPriceMultiplier, spnInnerSpherePartPriceMultiplier,
                spnClanUnitPriceMultiplier, spnClanPartPriceMultiplier };

        return createPriceMultiplierGridPanel("GeneralMultipliersPanel", labels, controls);
    }

    /**
     * Creates and returns a JPanel for configuring used parts price multipliers
     * based on part quality. Each part
     * quality level is represented with a label and a spinner for adjusting the
     * multiplier value.
     * <p>
     * The spinners are initialized with a range of values from 0.00 to 1.00,
     * incrementing by 0.05, and include
     * formatting for two decimal places. Additionally, the alignment of the spinner
     * text fields is set to left.
     * <p>
     * The panel is arranged using GridBagLayout to ensure proper alignment between
     * labels and spinners for each quality
     * level.
     *
     * @return A JPanel containing labels and spinners for used parts price
     *         multipliers.
     */
    private @Nonnull JPanel createUsedPartsMultiplierPanel() {
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
            CampaignOptionsSpinner.installSelectAllOnFocus(spnUsedPartPriceMultipliers[ordinal]);
        }

        // Layout the Panel
        JComponent[] labels = new JComponent[spnUsedPartPriceMultipliers.length];
        JComponent[] controls = new JComponent[spnUsedPartPriceMultipliers.length];
        for (int index = 0; index < spnUsedPartPriceMultipliers.length; index++) {
            labels[index] = lblUsedPartPriceMultipliers[index];
            controls[index] = spnUsedPartPriceMultipliers[index];
        }

        return createPriceMultiplierGridPanel("UsedPartsMultiplierPanel", labels, controls);
    }

    /**
     * Creates and returns a JPanel configured with components for adjusting
     * multipliers related to damaged parts value,
     * unrepairable parts value, and cancelled order refunds. Each multiplier is
     * represented with a label and an
     * associated configurable spinner control.
     *
     * @return a JPanel instance containing the components for configuring the
     *         multipliers.
     */
    private @Nonnull JPanel createOtherMultipliersPanel() {
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
        JComponent[] labels = { lblDamagedPartsValueMultiplier, lblUnrepairablePartsValueMultiplier,
                lblCancelledOrderRefundMultiplier };
        JComponent[] controls = { spnDamagedPartsValueMultiplier, spnUnrepairablePartsValueMultiplier,
                spnCancelledOrderRefundMultiplier };

        return createPriceMultiplierGridPanel("OtherMultipliersPanel", labels, controls);
    }

    /**
     * Builds a Price Multipliers section as a two-column
     * ({@code label/control, label/control}) aligned grid. The pair
     * widths are shared by every Price Multipliers section so their columns line
     * up, and are sized so the section stays
     * within the dialog's common page width.
     *
     * @param name     the section's base name; the Swing component name becomes
     *                 {@code "pnl" + name}
     * @param labels   the label components, one per field, in row-major order
     * @param controls the control components, matching {@code labels} by index
     *
     * @return the assembled paired-field grid panel
     */
    private @Nonnull CampaignOptionsPairedFieldGridPanel createPriceMultiplierGridPanel(String name, JComponent[] labels,
            JComponent[] controls) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                FINANCES_GRID_FIRST_PAIR_COLUMN_WIDTH,
                FINANCES_GRID_FOLLOWING_PAIR_COLUMN_WIDTH,
                FINANCES_GRID_CONTROL_COLUMN_WIDTH,
                2);
        panel.addPairs(labels, controls);

        return panel;
    }

    /**
     * Loads configuration values from the current campaign options to populate the
     * financial settings and related UI
     * components in the `FinancesTab`.
     * <p>
     * This method is a convenience overload that invokes the overloaded
     * {@link #loadValuesFromCampaignOptions(CampaignOptions)} method with a `null`
     * parameter, ensuring that default
     * campaign options will be loaded.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies the values from the provided campaign options or the
     * default campaign options if the provided
     * options are null. Updates various UI components and internal variables based
     * on the configuration of the campaign
     * options.
     *
     * @param presetCampaignOptions the campaign options to load values from; if
     *                              null, the default campaign options will
     *                              be used
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new FinancesOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the specified campaign options to the corresponding campaign
     * settings. If no campaign options are
     * provided, default options are used instead.
     *
     * @param presetCampaignOptions The campaign options to be applied. If null,
     *                              default campaign options are applied.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(options);
    }

    private void updateCreatedControlsFromModel() {
        updateGeneralControlsFromModel();
        updatePriceMultiplierControlsFromModel();
    }

    private void updateGeneralControlsFromModel() {
        if (!generalOptionsPageCreated || model == null) {
            return;
        }

        useLoanLimitsBox.setSelected(model.useLoanLimits);
        usePercentageMaintenanceBox.setSelected(model.usePercentageMaintenance);
        useExtendedPartsModifierBox.setSelected(model.useExtendedPartsModifier);
        usePeacetimeCostBox.setSelected(model.usePeacetimeCost);
        showPeacetimeCostBox.setSelected(model.showPeacetimeCost);
        comboFinancialYearDuration.setSelectedItem(model.financialYearDuration);
        newFinancialYearFinancesToCSVExportBox.setSelected(model.newFinancialYearFinancesToCSVExport);
        chkSimulateGrayMonday.setSelected(model.simulateGrayMonday);
        payForPartsBox.setSelected(model.payForParts);
        payForRepairsBox.setSelected(model.payForRepairs);
        payForUnitsBox.setSelected(model.payForUnits);
        payForSalariesBox.setSelected(model.payForSalaries);
        payForOverheadBox.setSelected(model.payForOverhead);
        payForMaintainBox.setSelected(model.payForMaintain);
        payForTransportBox.setSelected(model.payForTransport);
        payForRecruitmentBox.setSelected(model.payForRecruitment);
        payForFoodBox.setSelected(model.payForFood);
        payForHousingBox.setSelected(model.payForHousing);
        sellUnitsBox.setSelected(model.sellUnits);
        sellPartsBox.setSelected(model.sellParts);
        chkUseTaxes.setSelected(model.useTaxes);
        spnTaxesPercentage.setValue(model.taxesPercentage);
        chkUseShareSystem.setSelected(model.useShareSystem);
        chkSharesForAll.setSelected(model.sharesForAll);
        spnRentedFacilitiesCostHospitalBeds.setValue(model.rentedFacilitiesCostHospitalBeds);
        spnRentedFacilitiesCostKitchens.setValue(model.rentedFacilitiesCostKitchens);
        spnRentedFacilitiesCostHoldingCells.setValue(model.rentedFacilitiesCostHoldingCells);
        spnRentedFacilitiesCostRepairBays.setValue(model.rentedFacilitiesCostRepairBays);
    }

    private void updatePriceMultiplierControlsFromModel() {
        if (!priceMultipliersPageCreated || model == null) {
            return;
        }

        spnCommonPartPriceMultiplier.setValue(model.commonPartPriceMultiplier);
        spnInnerSphereUnitPriceMultiplier.setValue(model.innerSphereUnitPriceMultiplier);
        spnInnerSpherePartPriceMultiplier.setValue(model.innerSpherePartPriceMultiplier);
        spnClanUnitPriceMultiplier.setValue(model.clanUnitPriceMultiplier);
        spnClanPartPriceMultiplier.setValue(model.clanPartPriceMultiplier);
        spnMixedTechUnitPriceMultiplier.setValue(model.mixedTechUnitPriceMultiplier);
        for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length,
                model.usedPartPriceMultipliers.length); i++) {
            spnUsedPartPriceMultipliers[i].setValue(model.usedPartPriceMultipliers[i]);
        }
        spnDamagedPartsValueMultiplier.setValue(model.damagedPartsValueMultiplier);
        spnUnrepairablePartsValueMultiplier.setValue(model.unrepairablePartsValueMultiplier);
        spnCancelledOrderRefundMultiplier.setValue(model.cancelledOrderRefundMultiplier);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromGeneralControls();
        updateModelFromPriceMultiplierControls();
    }

    private void updateModelFromGeneralControls() {
        if (!generalOptionsPageCreated || model == null) {
            return;
        }

        model.useLoanLimits = useLoanLimitsBox.isSelected();
        model.usePercentageMaintenance = usePercentageMaintenanceBox.isSelected();
        model.useExtendedPartsModifier = useExtendedPartsModifierBox.isSelected();
        model.usePeacetimeCost = usePeacetimeCostBox.isSelected();
        model.showPeacetimeCost = showPeacetimeCostBox.isSelected();
        model.financialYearDuration = comboFinancialYearDuration.getSelectedItem();
        model.newFinancialYearFinancesToCSVExport = newFinancialYearFinancesToCSVExportBox.isSelected();
        model.simulateGrayMonday = chkSimulateGrayMonday.isSelected();
        model.payForParts = payForPartsBox.isSelected();
        model.payForRepairs = payForRepairsBox.isSelected();
        model.payForUnits = payForUnitsBox.isSelected();
        model.payForSalaries = payForSalariesBox.isSelected();
        model.payForOverhead = payForOverheadBox.isSelected();
        model.payForMaintain = payForMaintainBox.isSelected();
        model.payForTransport = payForTransportBox.isSelected();
        model.payForRecruitment = payForRecruitmentBox.isSelected();
        model.payForFood = payForFoodBox.isSelected();
        model.payForHousing = payForHousingBox.isSelected();
        model.sellUnits = sellUnitsBox.isSelected();
        model.sellParts = sellPartsBox.isSelected();
        model.useTaxes = chkUseTaxes.isSelected();
        model.taxesPercentage = (int) spnTaxesPercentage.getValue();
        model.useShareSystem = chkUseShareSystem.isSelected();
        model.sharesForAll = chkSharesForAll.isSelected();
        model.rentedFacilitiesCostHospitalBeds = (int) spnRentedFacilitiesCostHospitalBeds.getValue();
        model.rentedFacilitiesCostKitchens = (int) spnRentedFacilitiesCostKitchens.getValue();
        model.rentedFacilitiesCostHoldingCells = (int) spnRentedFacilitiesCostHoldingCells.getValue();
        model.rentedFacilitiesCostRepairBays = (int) spnRentedFacilitiesCostRepairBays.getValue();
    }

    private void updateModelFromPriceMultiplierControls() {
        if (!priceMultipliersPageCreated || model == null) {
            return;
        }

        model.commonPartPriceMultiplier = (double) spnCommonPartPriceMultiplier.getValue();
        model.innerSphereUnitPriceMultiplier = (double) spnInnerSphereUnitPriceMultiplier.getValue();
        model.innerSpherePartPriceMultiplier = (double) spnInnerSpherePartPriceMultiplier.getValue();
        model.clanUnitPriceMultiplier = (double) spnClanUnitPriceMultiplier.getValue();
        model.clanPartPriceMultiplier = (double) spnClanPartPriceMultiplier.getValue();
        model.mixedTechUnitPriceMultiplier = (double) spnMixedTechUnitPriceMultiplier.getValue();
        for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length,
                model.usedPartPriceMultipliers.length); i++) {
            model.usedPartPriceMultipliers[i] = (Double) spnUsedPartPriceMultipliers[i].getValue();
        }
        model.damagedPartsValueMultiplier = (double) spnDamagedPartsValueMultiplier.getValue();
        model.unrepairablePartsValueMultiplier = (double) spnUnrepairablePartsValueMultiplier.getValue();
        model.cancelledOrderRefundMultiplier = (double) spnCancelledOrderRefundMultiplier.getValue();
    }

}
