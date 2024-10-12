package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.parts.Part;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class FinancesTab {
    Campaign campaign;
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

    FinancesTab(Campaign campaign, JFrame frame, String name) {
        this.campaign = campaign;
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeGeneralOptionsTab();
        initializePriceMultipliersTab();
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

    private JPanel createPaymentsPanel() {
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

    private JPanel createOtherSystemsPanel() {
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

    private JPanel createGeneralOptionsPanel() {
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

    private JPanel createSalesPanel() {
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

    private JPanel createTaxesPanel() {
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

    private JPanel createSharesPanel() {
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

    JPanel createPriceMultipliersTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PriceMultipliersTab",
            getImageDirectory() + "logo_illyrian_palatinate.png",
            false, "", true);

        // Contents
        pnlGeneralMultipliers = createGeneralMultipliersPanel();
        pnlUsedPartsMultipliers = createUsedPartsMultiplierPanel();
        pnlOtherMultipliers = createOtherMultipliersPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PriceMultipliersTab", true,
            "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlGeneralMultipliers)
                    .addComponent(pnlUsedPartsMultipliers)
                    .addComponent(pnlOtherMultipliers)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlGeneralMultipliers)
                    .addComponent(pnlUsedPartsMultipliers)
                    .addComponent(pnlOtherMultipliers)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        // Create Parent Panel and return
        return createParentPanel(panel, "PriceMultipliersTab");
    }

    private JPanel createGeneralMultipliersPanel() {
        // Contents
        lblCommonPartPriceMultiplier = createLabel("CommonPartPriceMultiplier", null);
        spnCommonPartPriceMultiplier = createSpinner("CommonPartPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        lblInnerSphereUnitPriceMultiplier = createLabel("InnerSphereUnitPriceMultiplier", null);
        spnInnerSphereUnitPriceMultiplier = createSpinner("InnerSphereUnitPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        lblInnerSpherePartPriceMultiplier = createLabel("InnerSpherePartPriceMultiplier", null);
        spnInnerSpherePartPriceMultiplier = createSpinner("InnerSpherePartPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        lblClanUnitPriceMultiplier = createLabel("ClanUnitPriceMultiplier", null);
        spnClanUnitPriceMultiplier = createSpinner("ClanUnitPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        lblClanPartPriceMultiplier = createLabel("ClanPartPriceMultiplier", null);
        spnClanPartPriceMultiplier = createSpinner("ClanPartPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        lblMixedTechUnitPriceMultiplier = createLabel("MixedTechUnitPriceMultiplier", null);
        spnMixedTechUnitPriceMultiplier = createSpinner("MixedTechUnitPriceMultiplier", null,
            1.0, 0.1, 100, 0.1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("GeneralMultipliersPanel", true,
            "GeneralMultipliersPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCommonPartPriceMultiplier)
                    .addComponent(spnCommonPartPriceMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblInnerSphereUnitPriceMultiplier)
                    .addComponent(spnInnerSphereUnitPriceMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblInnerSpherePartPriceMultiplier)
                    .addComponent(spnInnerSpherePartPriceMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblClanUnitPriceMultiplier)
                    .addComponent(spnClanUnitPriceMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblClanPartPriceMultiplier)
                    .addComponent(spnClanPartPriceMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMixedTechUnitPriceMultiplier)
                    .addComponent(spnMixedTechUnitPriceMultiplier)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblCommonPartPriceMultiplier)
                    .addComponent(spnCommonPartPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblInnerSphereUnitPriceMultiplier)
                    .addComponent(spnInnerSphereUnitPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblInnerSpherePartPriceMultiplier)
                    .addComponent(spnInnerSpherePartPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblClanUnitPriceMultiplier)
                    .addComponent(spnClanUnitPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblClanPartPriceMultiplier)
                    .addComponent(spnClanPartPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMixedTechUnitPriceMultiplier)
                    .addComponent(spnMixedTechUnitPriceMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }

    private JPanel createUsedPartsMultiplierPanel() {
        boolean reverseQualities = campaign.getCampaignOptions().isReverseQualityNames();

        // Contents
        lblUsedPartPriceMultipliers = new JLabel[Part.QUALITY_F + 1];
        spnUsedPartPriceMultipliers = new JSpinner[Part.QUALITY_F + 1];

        for (int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            final String qualityLevel = Part.getQualityName(i, reverseQualities);

            lblUsedPartPriceMultipliers[i] = new JLabel(qualityLevel);
            lblUsedPartPriceMultipliers[i].setName("lbl" + qualityLevel);

            spnUsedPartPriceMultipliers[i] = new JSpinner(
                new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[i].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[i]
                .setEditor(new NumberEditor(spnUsedPartPriceMultipliers[i], "0.00"));

            DefaultEditor editor = (DefaultEditor) spnUsedPartPriceMultipliers[i].getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        }

        // Layout the Panel
        final JPanel panel = createStandardPanel("UsedPartsMultiplierPanel", true,
            "UsedPartsMultiplierPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[0])
                    .addComponent(spnUsedPartPriceMultipliers[0]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[1])
                    .addComponent(spnUsedPartPriceMultipliers[1]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[2])
                    .addComponent(spnUsedPartPriceMultipliers[2]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[3])
                    .addComponent(spnUsedPartPriceMultipliers[3]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[4])
                    .addComponent(spnUsedPartPriceMultipliers[4]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUsedPartPriceMultipliers[5])
                    .addComponent(spnUsedPartPriceMultipliers[5])));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[0])
                    .addComponent(spnUsedPartPriceMultipliers[0])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[1])
                    .addComponent(spnUsedPartPriceMultipliers[1])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[2])
                    .addComponent(spnUsedPartPriceMultipliers[2])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[3])
                    .addComponent(spnUsedPartPriceMultipliers[3])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[4])
                    .addComponent(spnUsedPartPriceMultipliers[4])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUsedPartPriceMultipliers[5])
                    .addComponent(spnUsedPartPriceMultipliers[5])
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }

    private JPanel createOtherMultipliersPanel() {
        // Contents
        lblDamagedPartsValueMultiplier = createLabel("DamagedPartsValueMultiplier", null);
        spnDamagedPartsValueMultiplier = createSpinner("DamagedPartsValueMultiplier", null,
            0.33, 0.00, 1.00, 0.05);

        lblUnrepairablePartsValueMultiplier = createLabel("UnrepairablePartsValueMultiplier", null);
        spnUnrepairablePartsValueMultiplier = createSpinner("UnrepairablePartsValueMultiplier", null,
            0.10, 0.00, 1.00, 0.05);

        lblCancelledOrderRefundMultiplier = createLabel("CancelledOrderRefundMultiplier", null);
        spnCancelledOrderRefundMultiplier = createSpinner("CancelledOrderRefundMultiplier", null,
            0.50, 0.00, 1.00, 0.05);

        // Layout the Panel
        final JPanel panel = createStandardPanel("OtherMultipliersPanel", true,
            "OtherMultipliersPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDamagedPartsValueMultiplier)
                    .addComponent(spnDamagedPartsValueMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUnrepairablePartsValueMultiplier)
                    .addComponent(spnUnrepairablePartsValueMultiplier))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCancelledOrderRefundMultiplier)
                    .addComponent(spnCancelledOrderRefundMultiplier)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblDamagedPartsValueMultiplier)
                    .addComponent(spnDamagedPartsValueMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUnrepairablePartsValueMultiplier)
                    .addComponent(spnUnrepairablePartsValueMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblCancelledOrderRefundMultiplier)
                    .addComponent(spnCancelledOrderRefundMultiplier)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return panel;
    }
}
