package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.personnel.Skills;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.DefaultEditor;
import java.util.HashMap;
import java.util.Map;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class MarketsTab {
    JFrame frame;
    String name;

    //start Personnel Market
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketType;
    private MMComboBox<String> comboPersonnelMarketType;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;

    private JPanel pnlRemovalTargets;
    private JLabel lblPersonnelMarketDylansWeight;
    private JSpinner spnPersonnelMarketDylansWeight;
    private Map<SkillLevel, JLabel> lblPersonnelMarketRandomRemovalTargets;
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;
    //end Personnel Market

    //start Unit Market
    private JLabel lblUnitMarketMethod;
    private MMComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMekVariations;
    private JLabel lblUnitMarketSpecialUnitChance;
    private JSpinner spnUnitMarketSpecialUnitChance;
    private JLabel lblUnitMarketRarityModifier;
    private JSpinner spnUnitMarketRarityModifier;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkUnitMarketReportRefresh;
    //end Unit Market

    //start Contract Market
    private JPanel pnlContractMarketGeneralOptions;
    private JLabel lblContractMarketMethod;
    private MMComboBox<ContractMarketMethod> comboContractMarketMethod;
    private JLabel lblContractSearchRadius;
    private JSpinner spnContractSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkContractMarketReportRefresh;
    private JLabel lblCoontractMaxSalvagePercentage;
    private JSpinner spnContractMaxSalvagePercentage;
    private JLabel lblDropShipBonusPercentage;
    private JSpinner spnDropShipBonusPercentage;

    private JPanel pnlContractPay;
    private JRadioButton btnContractEquipment;
    private JLabel lblEquipPercent;
    private JSpinner spnEquipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JLabel lblDropShipPercent;
    private JSpinner spnDropShipPercent;
    private JLabel lblJumpShipPercent;
    private JSpinner spnJumpShipPercent;
    private JLabel lblWarShipPercent;
    private JSpinner spnWarShipPercent;
    private JRadioButton btnContractPersonnel;
    private JCheckBox useInfantryDoseNotCountBox;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;
    //end Contract Market

    MarketsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializePersonnelMarket();
        initializeUnitMarket();
        initializeContractMarket();
    }

    private void initializePersonnelMarket() {
        pnlPersonnelMarketGeneralOptions = new JPanel();
        lblPersonnelMarketType = new JLabel();
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());
        chkPersonnelMarketReportRefresh = new JCheckBox();
        chkUsePersonnelHireHiringHallOnly = new JCheckBox();

        pnlRemovalTargets = new JPanel();
        lblPersonnelMarketDylansWeight = new JLabel();
        spnPersonnelMarketDylansWeight = new JSpinner();
        lblPersonnelMarketRandomRemovalTargets = new HashMap<>();
        spnPersonnelMarketRandomRemovalTargets = new HashMap<>();
    }

    private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
            .getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        return personnelMarketTypeModel;
    }

    JPanel createPersonnelMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PersonnelMarketTab",
            getImageDirectory() + "logo_clan_sea_fox.png",
            true);

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
        pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketTab", true,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlPersonnelMarketGeneralOptions)
                    .addComponent(pnlRemovalTargets)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlPersonnelMarketGeneralOptions)
                    .addComponent(pnlRemovalTargets)));

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelMarketTab");
    }

    private JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketType = new CampaignOptionsLabel("PersonnelMarketType");
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());

        lblPersonnelMarketDylansWeight = new CampaignOptionsLabel("PersonnelMarketDylansWeight");
        spnPersonnelMarketDylansWeight = new CampaignOptionsSpinner("PersonnelMarketDylansWeight",
            0.3, 0, 1, 0.1);

        chkPersonnelMarketReportRefresh = new CampaignOptionsCheckBox("PersonnelMarketReportRefresh");

        chkUsePersonnelHireHiringHallOnly = new CampaignOptionsCheckBox("UsePersonnelHireHiringHallOnly");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketGeneralOptionsPanel", false,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketType)
                    .addComponent(comboPersonnelMarketType))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketDylansWeight)
                    .addComponent(spnPersonnelMarketDylansWeight))
                .addComponent(chkPersonnelMarketReportRefresh)
                .addComponent(chkUsePersonnelHireHiringHallOnly));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketType)
                    .addComponent(comboPersonnelMarketType))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketDylansWeight)
                    .addComponent(spnPersonnelMarketDylansWeight))
                .addComponent(chkPersonnelMarketReportRefresh)
                .addComponent(chkUsePersonnelHireHiringHallOnly));

        return panel;
    }

    private JPanel createPersonnelMarketRemovalOptionsPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel jLabel = new JLabel(skillLevel.toString());
            lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

            final JSpinner jSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, 12, 1));

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
        }

        // Layout the Panels
        final JPanel leftPanel = new CampaignOptionsStandardPanel("LeftPanel", false);
        final GroupLayout leftLayout = createGroupLayout(leftPanel);
        leftPanel.setLayout(leftLayout);

        leftLayout.setVerticalGroup(
            leftLayout.createSequentialGroup()
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))));

        leftLayout.setHorizontalGroup(
            leftLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE)))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN)))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN)))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))));

        final JPanel rightPanel = new CampaignOptionsStandardPanel("RightPanel", false);
        final GroupLayout rightLayout = createGroupLayout(rightPanel);
        rightPanel.setLayout(rightLayout);

        rightLayout.setVerticalGroup(
            rightLayout.createSequentialGroup()
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))));

        rightLayout.setHorizontalGroup(
            rightLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN)))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE)))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC)))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))));

        final JPanel parentPanel = new CampaignOptionsStandardPanel("PersonnelMarketRemovalOptionsPanel",
            true, "PersonnelMarketRemovalOptionsPanel");
        final GroupLayout parentLayout = createGroupLayout(parentPanel);
        parentPanel.setLayout(parentLayout);

        parentLayout.setVerticalGroup(
            parentLayout.createSequentialGroup()
                .addGroup(parentLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(leftPanel)
                    .addComponent(rightPanel)));

        parentLayout.setHorizontalGroup(
            parentLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(parentLayout.createSequentialGroup()
                    .addComponent(leftPanel)
                    .addComponent(rightPanel)));

        return parentPanel;
    }

    private void initializeUnitMarket() {
        lblUnitMarketMethod = new JLabel();
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        chkUnitMarketRegionalMekVariations = new JCheckBox();
        lblUnitMarketSpecialUnitChance = new JLabel();
        spnUnitMarketSpecialUnitChance = new JSpinner();
        lblUnitMarketRarityModifier = new JLabel();
        spnUnitMarketRarityModifier = new JSpinner();
        chkInstantUnitMarketDelivery = new JCheckBox();
        chkUnitMarketReportRefresh = new JCheckBox();
    }

    JPanel createUnitMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("UnitMarketTab",
            getImageDirectory() + "logo_loathian_league.png",
            true);

        // Contents
        lblUnitMarketMethod = new CampaignOptionsLabel("UnitMarketMethod");
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());

        chkUnitMarketRegionalMekVariations = new CampaignOptionsCheckBox("UnitMarketRegionalMekVariations");

        lblUnitMarketSpecialUnitChance = new CampaignOptionsLabel("UnitMarketSpecialUnitChance");
        spnUnitMarketSpecialUnitChance = new CampaignOptionsSpinner("UnitMarketSpecialUnitChance",
            30, 0, 100, 1);

        lblUnitMarketRarityModifier = new CampaignOptionsLabel("UnitMarketRarityModifier");
        spnUnitMarketRarityModifier = new CampaignOptionsSpinner("UnitMarketRarityModifier",
            0, -10, 10, 1);

        chkInstantUnitMarketDelivery = new CampaignOptionsCheckBox("InstantUnitMarketDelivery");

        chkUnitMarketReportRefresh = new CampaignOptionsCheckBox("UnitMarketReportRefresh");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitMarketTab", true,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUnitMarketMethod)
                    .addComponent(comboUnitMarketMethod))
                .addComponent(chkUnitMarketRegionalMekVariations)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUnitMarketSpecialUnitChance)
                    .addComponent(spnUnitMarketSpecialUnitChance))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblUnitMarketRarityModifier)
                    .addComponent(spnUnitMarketRarityModifier))
                .addComponent(chkInstantUnitMarketDelivery)
                .addComponent(chkUnitMarketReportRefresh));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUnitMarketMethod)
                    .addComponent(comboUnitMarketMethod))
                .addComponent(chkUnitMarketRegionalMekVariations)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUnitMarketSpecialUnitChance)
                    .addComponent(spnUnitMarketSpecialUnitChance))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblUnitMarketRarityModifier)
                    .addComponent(spnUnitMarketRarityModifier))
                .addComponent(chkInstantUnitMarketDelivery)
                .addComponent(chkUnitMarketReportRefresh));

        // Create Parent Panel and return
        return createParentPanel(panel, "UnitMarketTab");
    }

    private void initializeContractMarket() {
        pnlContractMarketGeneralOptions = new JPanel();
        lblContractMarketMethod = new JLabel();
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod",
            ContractMarketMethod.values());
        lblContractSearchRadius = new JLabel();
        spnContractSearchRadius = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkContractMarketReportRefresh = new JCheckBox();
        lblCoontractMaxSalvagePercentage = new JLabel();
        spnContractMaxSalvagePercentage = new JSpinner();
        lblDropShipBonusPercentage = new JLabel();
        spnDropShipBonusPercentage = new JSpinner();

        pnlContractPay = new JPanel();
        btnContractEquipment = new JRadioButton();
        lblEquipPercent = new JLabel();
        spnEquipPercent = new JSpinner();
        chkEquipContractSaleValue = new JCheckBox();
        lblDropShipPercent = new JLabel();
        spnDropShipPercent = new JSpinner();
        lblJumpShipPercent = new JLabel();
        spnJumpShipPercent = new JSpinner();
        lblWarShipPercent = new JLabel();
        spnWarShipPercent = new JSpinner();
        btnContractPersonnel = new JRadioButton();
        useInfantryDoseNotCountBox = new JCheckBox();
        chkMercSizeLimited = new JCheckBox();
        chkBLCSaleValue = new JCheckBox();
        chkOverageRepaymentInFinalPayment = new JCheckBox();
    }

    JPanel createContractMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("ContractMarketTab",
            getImageDirectory() + "logo_clan_smoke_jaguar.png",
            true);

        // Contents
        pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
        pnlContractPay = createContractPayPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractMarketTab", true,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlContractMarketGeneralOptions)
                    .addComponent(pnlContractPay)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlContractMarketGeneralOptions)
                    .addComponent(pnlContractPay)));

        // Create Parent Panel and return
        return createParentPanel(panel, "ContractMarketTab");
    }

    private JPanel createContractMarketGeneralOptionsPanel() {
        // Contents
        lblContractMarketMethod = new CampaignOptionsLabel("ContractMarketMethod");
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod",
            ContractMarketMethod.values());

        lblContractSearchRadius = new CampaignOptionsLabel("ContractSearchRadius");
        spnContractSearchRadius = new CampaignOptionsSpinner("ContractSearchRadius",
            300, 100, 2500, 100);

        chkVariableContractLength = new CampaignOptionsCheckBox("VariableContractLength");

        chkContractMarketReportRefresh = new CampaignOptionsCheckBox("ContractMarketReportRefresh");

        lblCoontractMaxSalvagePercentage = new CampaignOptionsLabel("CoontractMaxSalvagePercentage");
        spnContractMaxSalvagePercentage = new CampaignOptionsSpinner("CoontractMaxSalvagePercentage",
            100, 0, 100, 10);

        lblDropShipBonusPercentage = new CampaignOptionsLabel("DropShipBonusPercentage");
        spnDropShipBonusPercentage = new CampaignOptionsSpinner("DropShipBonusPercentage",
            0, 0, 20, 5);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractMarketGeneralOptionsPanel",
            false);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblContractMarketMethod)
                    .addComponent(comboContractMarketMethod))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblContractSearchRadius)
                    .addComponent(spnContractSearchRadius))
                .addComponent(chkVariableContractLength)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCoontractMaxSalvagePercentage)
                    .addComponent(spnContractMaxSalvagePercentage))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDropShipBonusPercentage)
                    .addComponent(spnDropShipBonusPercentage))
                .addComponent(chkContractMarketReportRefresh));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblContractMarketMethod)
                    .addComponent(comboContractMarketMethod))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblContractSearchRadius)
                    .addComponent(spnContractSearchRadius))
                .addComponent(chkVariableContractLength)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblCoontractMaxSalvagePercentage)
                    .addComponent(spnContractMaxSalvagePercentage))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblDropShipBonusPercentage)
                    .addComponent(spnDropShipBonusPercentage))
                .addComponent(chkContractMarketReportRefresh));

        return panel;
    }

    private JPanel createContractPayPanel() {
        // Contents
        btnContractEquipment = new JRadioButton(resources.getString("lblContractEquipment.text"));
        btnContractEquipment.setToolTipText(resources.getString("lblContractEquipment.tooltip"));

        chkEquipContractSaleValue = new CampaignOptionsCheckBox("EquipContractSaleValue");

        lblEquipPercent = new CampaignOptionsLabel("EquipPercent");
        spnEquipPercent = new CampaignOptionsSpinner("EquipPercent",
            0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblDropShipPercent = new CampaignOptionsLabel("DropShipPercent");
        spnDropShipPercent = new CampaignOptionsSpinner("DropShipPercent",
            0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblJumpShipPercent = new CampaignOptionsLabel("JumpShipPercent");
        spnJumpShipPercent = new CampaignOptionsSpinner("JumpShipPercent",
            0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblWarShipPercent = new CampaignOptionsLabel("WarShipPercent");
        spnWarShipPercent = new CampaignOptionsSpinner("WarShipPercent",
            0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        btnContractPersonnel = new JRadioButton(resources.getString("lblContractPersonnel.text"));
        btnContractPersonnel.setToolTipText(resources.getString("lblContractPersonnel.tooltip"));

        chkBLCSaleValue = new CampaignOptionsCheckBox("BLCSaleValue");

        useInfantryDoseNotCountBox = new CampaignOptionsCheckBox("UseInfantryDoseNotCountBox");
        chkMercSizeLimited = new CampaignOptionsCheckBox("MercSizeLimited");

        chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractPayPanel",
            true, "ContractPayPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(btnContractEquipment)
                .addComponent(chkEquipContractSaleValue)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblEquipPercent)
                    .addComponent(spnEquipPercent))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDropShipPercent)
                    .addComponent(spnDropShipPercent))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblJumpShipPercent)
                    .addComponent(spnJumpShipPercent))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblWarShipPercent)
                    .addComponent(spnWarShipPercent))
                .addComponent(btnContractPersonnel)
                .addGap(UIUtil.scaleForGUI(15))
                .addComponent(chkBLCSaleValue)
                .addComponent(useInfantryDoseNotCountBox)
                .addComponent(chkMercSizeLimited)
                .addComponent(chkOverageRepaymentInFinalPayment));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(btnContractEquipment)
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(chkEquipContractSaleValue))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblEquipPercent)
                    .addComponent(spnEquipPercent))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblDropShipPercent)
                    .addComponent(spnDropShipPercent))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblJumpShipPercent)
                    .addComponent(spnJumpShipPercent))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblWarShipPercent)
                    .addComponent(spnWarShipPercent))
                .addComponent(btnContractPersonnel)
                .addComponent(chkBLCSaleValue)
                .addComponent(useInfantryDoseNotCountBox)
                .addComponent(chkMercSizeLimited)
                .addComponent(chkOverageRepaymentInFinalPayment));

        return panel;
    }
}
