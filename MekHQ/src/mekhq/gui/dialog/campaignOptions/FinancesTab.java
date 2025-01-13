package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.parts.enums.PartQuality;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;

import static mekhq.campaign.parts.enums.PartQuality.QUALITY_F;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.*;

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

    FinancesTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

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
     * Constructs and returns a {@link JPanel} for the 'FinancesGeneralTab'.
     * This tab includes 'GeneralOptions', 'Payments', 'Sales', and 'OtherSystems' panels that are
     * organized using {@link GroupLayout}. A header is also included in the layout.
     *
     * @return {@link JPanel} representing the 'Finances General' tab.
     */
    JPanel createFinancesGeneralOptionsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("FinancesGeneralTab",
            getImageDirectory() + "logo_clan_nova_cat.png",
            true);

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
     * Constructs and returns a {@link JPanel} for the 'PaymentsPanel'.
     * This panel contains checkboxes for different payment categories, organized into two rows
     * within a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'Payments' panel.
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
     * Constructs and returns a {@link JPanel} for the 'OtherSystemsPanel'.
     * This panel includes 'Taxes' and 'Shares' panels, arranged vertically within a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'Other Systems' panel.
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
     * Constructs and returns a {@link JPanel} for the 'GeneralOptionsPanel'. This panel contains
     * checkboxes for various options, a label displaying financial year duration,
     * and a combo box to select the financial year duration.
     * The components are arranged vertically within a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'General Options' panel.
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
     * Constructs and returns a {@link JPanel} for the 'SalesPanel'.
     * This panel contains two checkboxes - 'SellUnitsBox' and 'SellPartsBox',
     * organized in a horizontal arrangement within a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'Sales' panel.
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
     * Creates and returns a {@link JPanel} for the 'TaxesPanel'.
     * This panel includes a checkbox for activating/deactivating taxes and
     * a label and spinner for adjusting the tax percentage. These components are organized
     * vertically in a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'Taxes' panel.
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
     * Creates and returns a {@link JPanel} for the 'SharesPanel'.
     * This panel includes two check boxes - 'UseShareSystem', and 'SharesForAll' arranged vertically
     * in a {@link GroupLayout}.
     *
     * @return {@link JPanel} representing the 'Shares' panel.
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
     * Initializes the components of the 'PriceMultipliersTab'.
     * This method assigns new instances of {@link JPanel}, {@link JLabel}, and {@link JSpinner}
     * to the panel and its accompanying components for 'General Multipliers',
     * 'Used Parts Multipliers', and 'Other Multipliers'.
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
     * Constructs and returns a {@link JPanel} for the 'PriceMultipliersTab'.
     * This tab comprises a header panel and three individual sections - 'General Multipliers',
     * 'Used Parts Multipliers', and 'Other Multipliers'. These sections are vertically aligned
     * using a {@link GroupLayout}.
     *
     * @return {@link JPanel} that constitutes the 'Price Multipliers' tab.
     */
    JPanel createPriceMultipliersTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PriceMultipliersTab",
            getImageDirectory() + "logo_illyrian_palatinate.png",
            true);

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
     * Creates a {@link JPanel} for entering general multipliers with labeled spinners.
     * The panel groups together spinners for entering multipliers for multiple categories like 'CommonPart',
     * 'InnerSphereUnit', 'InnerSpherePart', 'ClanUnit', 'ClanPart', and 'MixedTechUnit'.
     * Each category has its own row with the respective label and spinner.
     *
     * @return {@link JPanel} representing the 'General Multipliers' panel.
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
     * Creates and returns a {@link JPanel} representing the 'Used Parts Multiplier' panel.
     * <p>
     * The {@link JPanel} includes numerous pairs of {@link JLabel} and {@link JSpinner} components,
     * each associated with a particular 'quality' level of the used parts.
     * Each {@link JLabel} is named according to the quality level.
     * Each {@link JSpinner} is initialized with a {@link SpinnerNumberModel} for inputting numbers with
     * a range of 0.00 to 1.00, and a step increment of 0.05.
     * <p>
     * The 'quality' levels range from 'QUALITY_A' to 'QUALITY_F', as defined in the Part class.
     * These different quality levels represent different conditions of used parts.
     * <p>
     * The {@link JPanel} uses a {@link GroupLayout} to arrange its components.
     * Vertically, each {@link JLabel} is paired alongside its corresponding {@link JSpinner}.
     * Horizontally, each {@link JLabel}-{@link JSpinner} pair is in a row of its own.
     *
     * @return {@link JPanel} representing the 'Used Parts Multiplier' panel.
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
     * Creates and returns a {@link JPanel} that includes various spinners associated with different multipliers.
     * These multipliers include "Damaged Parts Value Multiplier", "Unrepairable Parts Value Multiplier",
     * and "Cancelled Order Refund Multiplier". Each of these multipliers has their own label and
     * corresponding spinner for input.
     * <p>
     * The layout of the panel is organized so that each label is directly paired with its corresponding
     * spinner horizontally. Vertically, the pairs are stacked on top of each other in the order that they
     * were added to the panel.
     *
     * @return {@link JPanel} representing the 'Other Multipliers' panel.
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

    void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
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

    void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
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
