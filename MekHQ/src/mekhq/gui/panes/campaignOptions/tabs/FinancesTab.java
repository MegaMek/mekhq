package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.finances.enums.FinancialYearDuration;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class FinancesTab {
    JFrame frame;
    String name;

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
    //end Price Multipliers

    FinancesTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeGeneralOptionsTab();
        createFinancesGeneralOptionsTab();
        createPriceMultipliersTab();
    }

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
            getFinancialYearDurationOptions());

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

    private static DefaultComboBoxModel<FinancialYearDuration> getFinancialYearDurationOptions() {
        return new DefaultComboBoxModel<>(FinancialYearDuration.values());
    }

    JPanel createFinancesGeneralOptionsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("FinancesGeneralTab",
            getImageDirectory() + "logo_clan_nova_cat.png",
            false, "", true);

        // Contents
        pnlGeneralOptions = createGeneralOptionsPanel();
        pnlPayments = createPaymentsPanel();
        pnlSales = createSalesPanel();

        pnlOtherSystems = createOtherSystemsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("FinancesGeneralTab", true,
            "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlGeneralOptions)
                    .addComponent(pnlOtherSystems))
                .addComponent(pnlSales)
                .addComponent(pnlPayments));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlGeneralOptions)
                    .addComponent(pnlOtherSystems)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(pnlSales)
                .addComponent(pnlPayments));

        // Create Parent Panel and return
        return createParentPanel(panel, "FinancesGeneralTab");
    }

    JPanel createPaymentsPanel() {
        // Contents
        payForPartsBox = createCheckBox("PayForPartsBox", null);
        payForRepairsBox = createCheckBox("PayForRepairsBox", null);
        payForUnitsBox = createCheckBox("PayForUnitsBox", null);
        payForSalariesBox = createCheckBox("PayForSalariesBox", null);
        payForOverheadBox = createCheckBox("PayForOverheadBox", null);
        payForMaintainBox = createCheckBox("PayForMaintainBox", null);
        payForTransportBox = createCheckBox("PayForTransportBox", null);
        payForRecruitmentBox = createCheckBox("PayForRecruitmentBox", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PaymentsPanel", true,
            "PaymentsPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(payForPartsBox)
                    .addComponent(payForRepairsBox)
                    .addComponent(payForUnitsBox)
                    .addComponent(payForSalariesBox))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(payForOverheadBox)
                    .addComponent(payForMaintainBox)
                    .addComponent(payForTransportBox)
                    .addComponent(payForRecruitmentBox)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(payForPartsBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForRepairsBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForUnitsBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForSalariesBox)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGap(UIUtil.scaleForGUI(5))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(payForOverheadBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForMaintainBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForTransportBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(payForRecruitmentBox)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }

    JPanel createOtherSystemsPanel() {
        // Contents
        pnlTaxes = createTaxesPanel();
        pnlShares = createSharesPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("OtherSystemsPanel", false, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(pnlTaxes)
                .addComponent(pnlShares));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(pnlTaxes)
                .addComponent(pnlShares));

        return panel;
    }

    JPanel createGeneralOptionsPanel() {
        // Contents
        useLoanLimitsBox = createCheckBox("UseLoanLimitsBox", null);
        usePercentageMaintenanceBox = createCheckBox("UsePercentageMaintenanceBox", null);
        useExtendedPartsModifierBox = createCheckBox("UseExtendedPartsModifierBox", null);
        usePeacetimeCostBox = createCheckBox("UsePeacetimeCostBox", null);
        showPeacetimeCostBox = createCheckBox("ShowPeacetimeCostBox", null);

        lblFinancialYearDuration = createLabel("FinancialYearDuration", null);

        newFinancialYearFinancesToCSVExportBox = createCheckBox("NewFinancialYearFinancesToCSVExportBox",
            null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("GeneralOptionsPanel", false, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(useLoanLimitsBox)
                .addComponent(usePercentageMaintenanceBox)
                .addComponent(useExtendedPartsModifierBox)
                .addComponent(usePeacetimeCostBox)
                .addComponent(showPeacetimeCostBox)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFinancialYearDuration)
                    .addComponent(comboFinancialYearDuration))
                .addComponent(newFinancialYearFinancesToCSVExportBox));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(useLoanLimitsBox)
                .addComponent(usePercentageMaintenanceBox)
                .addComponent(useExtendedPartsModifierBox)
                .addComponent(usePeacetimeCostBox)
                .addComponent(showPeacetimeCostBox)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblFinancialYearDuration)
                    .addComponent(comboFinancialYearDuration)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(newFinancialYearFinancesToCSVExportBox));

        return panel;
    }

    JPanel createSalesPanel() {
        // Contents
        sellUnitsBox = createCheckBox("SellUnitsBox", null);
        sellPartsBox = createCheckBox("SellPartsBox", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("SalesPanel", true,
            "SalesPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(sellUnitsBox)
                    .addComponent(sellPartsBox)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(sellUnitsBox)
                    .addGap(UIUtil.scaleForGUI(5))
                    .addComponent(sellPartsBox)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }

    JPanel createTaxesPanel() {
        // Contents
        chkUseTaxes = createCheckBox("UseTaxesBox", null);

        lblTaxesPercentage = createLabel("TaxesPercentage", null);
        spnTaxesPercentage = createSpinner("TaxesPercentage", null,
            30, 1, 100, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("TaxesPanel", true,
            "TaxesPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseTaxes)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTaxesPercentage)
                    .addComponent(spnTaxesPercentage)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUseTaxes)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblTaxesPercentage)
                    .addComponent(spnTaxesPercentage)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }

    JPanel createSharesPanel() {
        // Contents
        chkUseShareSystem = createCheckBox("UseShareSystem", null);
        chkSharesForAll = createCheckBox("SharesForAll", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("SharesPanel", true,
            "SharesPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseShareSystem)
                .addComponent(chkSharesForAll));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUseShareSystem)
                .addComponent(chkSharesForAll));

        return panel;
    }

    private void initializePriceMultipliersTab() {
    }

    JPanel createPriceMultipliersTab() {
        return null;
    }
}
