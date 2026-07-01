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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code FinancesGeneralPage} class builds and manages the Finances General Options leaf page of the Campaign
 * Options dialog. It owns the widgets for general financial configuration - loan limits, peacetime costs, financial
 * year settings, payments, sales, taxes, shares, and rented facility costs - and synchronises them with a shared
 * {@link FinancesOptionsModel}.
 *
 * <p>This view is a sub-component of {@link FinancesPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code FinancesPages}, while this class is responsible only for constructing the Finances General
 * Options panel and copying general financial values to and from the model. The page is built lazily; until
 * {@link #createPanel(FinancesOptionsModel)} is called, {@link #readFromModel(FinancesOptionsModel)} and
 * {@link #writeToModel(FinancesOptionsModel)} are no-ops.</p>
 */
class FinancesGeneralPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

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

    private boolean created;

    /**
     * Creates and configures the Finances General Options page, assembling its
     * components, layout, and panels which
     * include general options, other systems, payments, and sales. This method
     * initializes required sub-panels and
     * arranges them within the overall structure to create a fully constructed page
     * for financial general options.
     *
     * @param model the shared finances options model to populate the freshly built controls from
     *
     * @return A fully configured JPanel representing the Finances General Options
     *         page.
     */
    @Nonnull JPanel createPanel(@Nullable FinancesOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_star_league.png";
        financesGeneralOptions = new CampaignOptionsHeaderPanel("FinancesGeneralPage",
                imageAddress);

        // Contents
        comboFinancialYearDuration = new MMComboBox<>("comboFinancialYearDuration", FinancialYearDuration.values());

        pnlGeneralOptions = createGeneralOptionsPanel();
        pnlPayments = createPaymentsPanel();
        pnlSales = createSalesPanel();
        pnlTaxes = createTaxesPanel();
        pnlShares = createSharesPanel();
        pnlRentedFacilities = createRentedFacilitiesPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("FinancesGeneralPage", "FinancesGeneralPage", imageAddress)
                .header(financesGeneralOptions)
                .quote("financesGeneralPage")
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

        created = true;
        readFromModel(model);

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
        payForPartsBox.addMouseListener(createTipPanelUpdater("PayForPartsBox"));
        payForRepairsBox = new CampaignOptionsCheckBox("PayForRepairsBox");
        payForRepairsBox.addMouseListener(createTipPanelUpdater("PayForRepairsBox"));
        payForUnitsBox = new CampaignOptionsCheckBox("PayForUnitsBox");
        payForUnitsBox.addMouseListener(createTipPanelUpdater("PayForUnitsBox"));
        payForSalariesBox = new CampaignOptionsCheckBox("PayForSalariesBox");
        payForSalariesBox.addMouseListener(createTipPanelUpdater("PayForSalariesBox"));
        payForOverheadBox = new CampaignOptionsCheckBox("PayForOverheadBox");
        payForOverheadBox.addMouseListener(createTipPanelUpdater("PayForOverheadBox"));
        payForMaintainBox = new CampaignOptionsCheckBox("PayForMaintainBox");
        payForMaintainBox.addMouseListener(createTipPanelUpdater("PayForMaintainBox"));
        payForTransportBox = new CampaignOptionsCheckBox("PayForTransportBox");
        payForTransportBox.addMouseListener(createTipPanelUpdater("PayForTransportBox"));
        payForRecruitmentBox = new CampaignOptionsCheckBox("PayForRecruitmentBox");
        payForRecruitmentBox
                .addMouseListener(createTipPanelUpdater("PayForRecruitmentBox"));
        payForFoodBox = new CampaignOptionsCheckBox("PayForFoodBox",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForFoodBox.addMouseListener(createTipPanelUpdater("PayForFoodBox"));
        payForHousingBox = new CampaignOptionsCheckBox("PayForHousingBox",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        payForHousingBox.addMouseListener(createTipPanelUpdater("PayForHousingBox"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PaymentsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
        useLoanLimitsBox.addMouseListener(createTipPanelUpdater("UseLoanLimitsBox"));
        usePercentageMaintenanceBox = new CampaignOptionsCheckBox("UsePercentageMaintenanceBox",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        usePercentageMaintenanceBox.addMouseListener(createTipPanelUpdater("UsePercentageMaintenanceBox"));
        useExtendedPartsModifierBox = new CampaignOptionsCheckBox("UseExtendedPartsModifierBox");
        useExtendedPartsModifierBox.addMouseListener(createTipPanelUpdater("UseExtendedPartsModifierBox"));
        usePeacetimeCostBox = new CampaignOptionsCheckBox("UsePeacetimeCostBox");
        usePeacetimeCostBox.addMouseListener(createTipPanelUpdater("UsePeacetimeCostBox"));
        showPeacetimeCostBox = new CampaignOptionsCheckBox("ShowPeacetimeCostBox");
        showPeacetimeCostBox
                .addMouseListener(createTipPanelUpdater("ShowPeacetimeCostBox"));

        lblFinancialYearDuration = new CampaignOptionsLabel("FinancialYearDuration",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFinancialYearDuration.addMouseListener(createTipPanelUpdater("FinancialYearDuration"));
        comboFinancialYearDuration.addMouseListener(createTipPanelUpdater("FinancialYearDuration"));

        newFinancialYearFinancesToCSVExportBox = new CampaignOptionsCheckBox(
                "NewFinancialYearFinancesToCSVExportBox");
        newFinancialYearFinancesToCSVExportBox.addMouseListener(createTipPanelUpdater("NewFinancialYearFinancesToCSVExportBox"));

        chkSimulateGrayMonday = new CampaignOptionsCheckBox("SimulateGrayMonday");
        chkSimulateGrayMonday.addMouseListener(createTipPanelUpdater("SimulateGrayMonday"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("GeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
     * Creates and configures the sales panel within the finance page. The panel
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
        sellUnitsBox.addMouseListener(createTipPanelUpdater("SellUnitsBox"));
        sellPartsBox = new CampaignOptionsCheckBox("SellPartsBox");
        sellPartsBox.addMouseListener(createTipPanelUpdater("SellPartsBox"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SalesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
        chkUseTaxes.addMouseListener(createTipPanelUpdater("UseTaxesBox"));

        lblTaxesPercentage = new CampaignOptionsLabel("TaxesPercentage");
        lblTaxesPercentage.addMouseListener(createTipPanelUpdater("TaxesPercentage"));
        spnTaxesPercentage = new CampaignOptionsSpinner("TaxesPercentage", 30, 1, 100, 1);
        spnTaxesPercentage.addMouseListener(createTipPanelUpdater("TaxesPercentage"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TaxesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseTaxes);
        panel.addRow(lblTaxesPercentage, spnTaxesPercentage);

        return panel;
    }

    /**
     * Creates and returns a JPanel representing the 'Shares Panel' within the
     * finance page.
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
        chkUseShareSystem.addMouseListener(createTipPanelUpdater("UseShareSystem"));
        chkSharesForAll = new CampaignOptionsCheckBox("SharesForAll");
        chkSharesForAll.addMouseListener(createTipPanelUpdater("SharesForAll"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SharesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2, chkUseShareSystem, chkSharesForAll);

        return panel;
    }

    private @Nonnull JPanel createRentedFacilitiesPanel() {
        // Contents
        lblRentedFacilitiesCostHospitalBeds = new CampaignOptionsLabel("RentedFacilitiesCostHospitalBeds",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostHospitalBeds.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostHospitalBeds"));
        spnRentedFacilitiesCostHospitalBeds = new CampaignOptionsSpinner("RentedFacilitiesCostHospitalBeds",
                4100, 0, 1000000, 1);
        spnRentedFacilitiesCostHospitalBeds.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostHospitalBeds"));

        lblRentedFacilitiesCostKitchens = new CampaignOptionsLabel("RentedFacilitiesCostKitchens",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostKitchens.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostKitchens"));
        spnRentedFacilitiesCostKitchens = new CampaignOptionsSpinner("RentedFacilitiesCostKitchens",
                3700, 0, 1000000, 1);
        spnRentedFacilitiesCostKitchens.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostKitchens"));

        lblRentedFacilitiesCostHoldingCells = new CampaignOptionsLabel("RentedFacilitiesCostHoldingCells",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostHoldingCells.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostHoldingCells"));
        spnRentedFacilitiesCostHoldingCells = new CampaignOptionsSpinner("RentedFacilitiesCostHoldingCells",
                6400, 0, 1000000, 1);
        spnRentedFacilitiesCostHoldingCells.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostHoldingCells"));

        lblRentedFacilitiesCostRepairBays = new CampaignOptionsLabel("RentedFacilitiesCostRepairBays",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRentedFacilitiesCostRepairBays.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostRepairBays"));
        spnRentedFacilitiesCostRepairBays = new CampaignOptionsSpinner("RentedFacilitiesCostRepairBays",
                25000, 0, 1000000, 1);
        spnRentedFacilitiesCostRepairBays.addMouseListener(createTipPanelUpdater("RentedFacilitiesCostRepairBays"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RentedFacilitiesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRentedFacilitiesCostHospitalBeds, spnRentedFacilitiesCostHospitalBeds);
        panel.addRow(lblRentedFacilitiesCostKitchens, spnRentedFacilitiesCostKitchens);
        panel.addRow(lblRentedFacilitiesCostHoldingCells, spnRentedFacilitiesCostHoldingCells);
        panel.addRow(lblRentedFacilitiesCostRepairBays, spnRentedFacilitiesCostRepairBays);

        return panel;
    }

    /**
     * Copies general financial values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared finances options model to read values from
     */
    void readFromModel(@Nullable FinancesOptionsModel model) {
        if (!created || model == null) {
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

    /**
     * Copies general financial values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared finances options model to write values into
     */
    void writeToModel(@Nullable FinancesOptionsModel model) {
        if (!created || model == null) {
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
}
