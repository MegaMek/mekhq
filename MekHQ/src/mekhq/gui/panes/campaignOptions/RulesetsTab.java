package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.Skills;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class RulesetsTab {
    JFrame frame;
    String name;

    //start Universal Options
    private JLabel lblSkillLevel;
    private MMComboBox<SkillLevel> comboSkillLevel;
    private JPanel pnlScenarioGenerationPanel;
    private JPanel pnlCampaignOptions;

    private JPanel pnlUnitRatioPanel;
    private JLabel lblOpForLanceTypeMeks;
    private JSpinner spnOpForLanceTypeMeks;
    private JLabel lblOpForLanceTypeMixed;
    private JSpinner spnOpForLanceTypeMixed;
    private JLabel lblOpForLanceTypeVehicle;
    private JSpinner spnOpForLanceTypeVehicles;

    private JCheckBox chkUseDropShips;
    private JCheckBox chkOpForUsesVTOLs;

    private JCheckBox chkClanVehicles;
    private JCheckBox chkRegionalMekVariations;

    private JCheckBox chkAttachedPlayerCamouflage;
    private JCheckBox chkPlayerControlsAttachedUnits;
    private JLabel lblSPAUpgradeIntensity;
    private JSpinner spnSPAUpgradeIntensity;
    private JCheckBox chkAutoConfigMunitions;

    private JPanel pnlScenarioModifiers;
    private JLabel lblScenarioModMax;
    private JSpinner spnScenarioModMax;
    private JLabel lblScenarioModChance;
    private JSpinner spnScenarioModChance;
    private JLabel lblScenarioModBV;
    private JSpinner spnScenarioModBV;

    private JPanel pnlMapGenerationPanel;
    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    private JLabel lblFixedMapChance;
    private JSpinner spnFixedMapChance;

    private JPanel pnlPartsPanel;
    private JCheckBox chkRestrictPartsByMission;
    private JLabel lblBonusPartExchangeValue;
    private JSpinner spnBonusPartExchangeValue;
    private JLabel lblBonusPartMaxExchangeCount;
    private JSpinner spnBonusPartMaxExchangeCount;

    private JPanel pnlLancePanel;
    private JCheckBox chkLimitLanceWeight;
    private JCheckBox chkLimitLanceNumUnits;
    private JCheckBox chkUseStrategy;
    private JLabel lblBaseStrategyDeployment;
    private JSpinner spnBaseStrategyDeployment;
    private JLabel lblAdditionalStrategyDeployment;
    private JSpinner spnAdditionalStrategyDeployment;
    private JCheckBox chkAdjustPaymentForStrategy;
    //end Universal Options

    //start Legacy AtB
    private JCheckBox chkUseAtB;

    private JPanel pnlLegacyOpForGenerationPanel;
    private JCheckBox chkUseVehicles;
    private JCheckBox chkDoubleVehicles;
    private JCheckBox chkOpForUsesAero;
    private JLabel lblOpForAeroChance;
    private JSpinner spnOpForAeroChance;
    private JCheckBox chkOpForUsesLocalForces;
    private JCheckBox chkAdjustPlayerVehicles;

    private JPanel pnlLegacyScenarioGenerationPanel;
    private JLabel lblIntensity;
    private JSpinner spnAtBBattleIntensity;
    private JLabel lblFightChance;
    private JLabel lblDefendChance;
    private JLabel lblScoutChance;
    private JLabel lblTrainingChance;
    private JSpinner[] spnAtBBattleChance;
    private JButton btnIntensityUpdate;
    private JCheckBox chkGenerateChases;
    //end Legacy AtB

    //start StratCon
    private JCheckBox chkUseStratCon;
    private JCheckBox chkUseGenericBattleValue;
    private JCheckBox chkUseVerboseBidding;
    //end StratCon

    RulesetsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeUniversalOptions();
        initializeStratConTab();
        initializeLegacyTab();
    }

    private void initializeUniversalOptions() {
        // General
        lblSkillLevel = new JLabel();
        comboSkillLevel = new MMComboBox<>("comboSkillLevel", getSkillLevelOptions());
        pnlScenarioGenerationPanel = new JPanel();

        // OpFor Generation
        pnlUnitRatioPanel = new JPanel();
        lblOpForLanceTypeMeks = new JLabel();
        spnOpForLanceTypeMeks = new JSpinner();
        lblOpForLanceTypeMixed = new JLabel();
        spnOpForLanceTypeMixed = new JSpinner();
        lblOpForLanceTypeVehicle = new JLabel();
        spnOpForLanceTypeVehicles = new JSpinner();

        chkUseDropShips = new JCheckBox();
        chkOpForUsesVTOLs = new JCheckBox();
        chkClanVehicles = new JCheckBox();
        chkRegionalMekVariations = new JCheckBox();

        chkAttachedPlayerCamouflage = new JCheckBox();
        chkPlayerControlsAttachedUnits = new JCheckBox();

        lblSPAUpgradeIntensity = new JLabel();
        spnSPAUpgradeIntensity = new JSpinner();
        chkAutoConfigMunitions = new JCheckBox();

        pnlScenarioModifiers = new JPanel();
        lblScenarioModMax = new JLabel();
        spnScenarioModMax = new JSpinner();
        lblScenarioModChance = new JLabel();
        spnScenarioModChance = new JSpinner();
        lblScenarioModBV = new JLabel();
        spnScenarioModBV = new JSpinner();

        // Map Generation
        pnlMapGenerationPanel = new JPanel();
        chkUseWeatherConditions = new JCheckBox();
        chkUseLightConditions = new JCheckBox();
        chkUsePlanetaryConditions = new JCheckBox();
        lblFixedMapChance = new JLabel();
        spnFixedMapChance = new JSpinner();

        // Parts
        pnlPartsPanel = new JPanel();
        chkRestrictPartsByMission = new JCheckBox();
        lblBonusPartExchangeValue = new JLabel();
        spnBonusPartExchangeValue = new JSpinner();
        lblBonusPartMaxExchangeCount = new JLabel();
        spnBonusPartMaxExchangeCount = new JSpinner();

        // Lances
        pnlLancePanel = new JPanel();
        chkLimitLanceWeight = new JCheckBox();
        chkLimitLanceNumUnits = new JCheckBox();
        chkUseStrategy = new JCheckBox();
        lblBaseStrategyDeployment = new JLabel();
        spnBaseStrategyDeployment = new JSpinner();
        lblAdditionalStrategyDeployment = new JLabel();
        spnAdditionalStrategyDeployment = new JSpinner();
        chkAdjustPaymentForStrategy = new JCheckBox();

        // Here we set up the options, so they can be used across both the AtB and StratCon tabs
        substantializeUniversalOptions();
    }

    private void substantializeUniversalOptions() {
        // General
        lblSkillLevel = new CampaignOptionsLabel("SkillLevel");
        comboSkillLevel.setToolTipText(resources.getString("lblSkillLevel.tooltip"));

        // OpFor Generation
        pnlUnitRatioPanel = createUniversalUnitRatioPanel();

        chkUseDropShips = new CampaignOptionsCheckBox("UseDropShips");
        chkOpForUsesVTOLs = new CampaignOptionsCheckBox("OpForUsesVTOLs");
        chkClanVehicles = new CampaignOptionsCheckBox("ClanVehicles");
        chkRegionalMekVariations = new CampaignOptionsCheckBox("RegionalMekVariations");

        chkAttachedPlayerCamouflage = new CampaignOptionsCheckBox("AttachedPlayerCamouflage");
        chkPlayerControlsAttachedUnits = new CampaignOptionsCheckBox("PlayerControlsAttachedUnits");
        lblSPAUpgradeIntensity = new CampaignOptionsLabel("SPAUpgradeIntensity");
        spnSPAUpgradeIntensity = new CampaignOptionsSpinner("SPAUpgradeIntensity",
            0, -1, 3, 1);
        chkAutoConfigMunitions = new CampaignOptionsCheckBox("AutoConfigMunitions");

        // Other
        pnlScenarioModifiers = createUniversalModifiersPanel();
        pnlMapGenerationPanel = createUniversalMapGenerationPanel();
        pnlPartsPanel = createUniversalPartsPanel();
        pnlLancePanel = createUniversalLancePanel();


        pnlScenarioGenerationPanel = createUniversalScenarioGenerationPanel();
        pnlCampaignOptions = createUniversalCampaignOptionsPanel();
    }

    private static DefaultComboBoxModel<SkillLevel> getSkillLevelOptions() {
        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>(
            Skills.SKILL_LEVELS);

        skillLevelModel.removeElement(SkillLevel.NONE);

        return skillLevelModel;
    }

    private JPanel createUniversalScenarioGenerationPanel() {
        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalScenarioGenerationPanel", true,
            "UniversalScenarioGenerationPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(pnlUnitRatioPanel)
                .addComponent(chkUseDropShips)
                .addComponent(chkOpForUsesVTOLs)
                .addComponent(chkClanVehicles)
                .addComponent(chkRegionalMekVariations)
                .addComponent(chkAttachedPlayerCamouflage)
                .addComponent(chkPlayerControlsAttachedUnits)
                .addComponent(chkAutoConfigMunitions)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblSPAUpgradeIntensity)
                    .addComponent(spnSPAUpgradeIntensity))
                .addComponent(pnlScenarioModifiers));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(pnlUnitRatioPanel)
                .addComponent(chkUseDropShips)
                .addComponent(chkOpForUsesVTOLs)
                .addComponent(chkClanVehicles)
                .addComponent(chkRegionalMekVariations)
                .addComponent(chkAttachedPlayerCamouflage)
                .addComponent(chkPlayerControlsAttachedUnits)
                .addComponent(chkAutoConfigMunitions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblSPAUpgradeIntensity)
                    .addComponent(spnSPAUpgradeIntensity))
                .addComponent(pnlScenarioModifiers));

        return panel;
    }

    private JPanel createUniversalUnitRatioPanel() {
        // Content
        lblOpForLanceTypeMeks = new CampaignOptionsLabel("OpForLanceTypeMeks");
        spnOpForLanceTypeMeks = new CampaignOptionsSpinner("OpForLanceTypeMeks",
            0, 0, 10, 1);
        lblOpForLanceTypeMixed = new CampaignOptionsLabel("OpForLanceTypeMixed");
        spnOpForLanceTypeMixed = new CampaignOptionsSpinner("OpForLanceTypeMixed",
            0, 0, 10, 1);
        lblOpForLanceTypeVehicle = new CampaignOptionsLabel("OpForLanceTypeVehicle");
        spnOpForLanceTypeVehicles = new CampaignOptionsSpinner("OpForLanceTypeVehicle",
            0, 0, 10, 1);

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalUnitRatioPanel", true,
            "UniversalUnitRatioPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblOpForLanceTypeMeks)
                    .addComponent(spnOpForLanceTypeMeks)
                    .addComponent(lblOpForLanceTypeMixed)
                    .addComponent(spnOpForLanceTypeMixed)
                    .addComponent(lblOpForLanceTypeVehicle)
                    .addComponent(spnOpForLanceTypeVehicles)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblOpForLanceTypeMeks)
                    .addComponent(spnOpForLanceTypeMeks)
                    .addComponent(lblOpForLanceTypeMixed)
                    .addComponent(spnOpForLanceTypeMixed)
                    .addComponent(lblOpForLanceTypeVehicle)
                    .addComponent(spnOpForLanceTypeVehicles)));

        return panel;
    }

    private JPanel createUniversalModifiersPanel() {
        //Content
        lblScenarioModMax = new CampaignOptionsLabel("ScenarioModMax");
        spnScenarioModMax = new CampaignOptionsSpinner("ScenarioModMax",
            3, 0, 10, 1);
        lblScenarioModChance = new CampaignOptionsLabel("ScenarioModChance");
        spnScenarioModChance = new CampaignOptionsSpinner("ScenarioModChance",
            25, 5, 100, 5);
        lblScenarioModBV = new CampaignOptionsLabel("ScenarioModBV");
        spnScenarioModBV = new CampaignOptionsSpinner("ScenarioModBV",
            50, 5, 100, 5);

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalModifiersPanel", true,
            "UniversalModifiersPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblScenarioModMax)
                    .addComponent(spnScenarioModMax))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblScenarioModChance)
                    .addComponent(spnScenarioModChance))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblScenarioModBV)
                    .addComponent(spnScenarioModBV)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblScenarioModMax)
                    .addComponent(spnScenarioModMax))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblScenarioModChance)
                    .addComponent(spnScenarioModChance))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblScenarioModBV)
                    .addComponent(spnScenarioModBV)));

        return panel;
    }

    private JPanel createUniversalMapGenerationPanel() {
        // Content
        chkUseWeatherConditions = new CampaignOptionsCheckBox("UseWeatherConditions");
        chkUseLightConditions = new CampaignOptionsCheckBox("UseLightConditions");
        chkUsePlanetaryConditions = new CampaignOptionsCheckBox("UsePlanetaryConditions");
        lblFixedMapChance = new CampaignOptionsLabel("FixedMapChance");
        spnFixedMapChance = new CampaignOptionsSpinner("FixedMapChance",
            0, 0, 100, 1);

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalMapGenerationPanel", true,
            "UniversalMapGenerationPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseWeatherConditions)
                .addComponent(chkUseLightConditions)
                .addComponent(chkUsePlanetaryConditions)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFixedMapChance)
                    .addComponent(spnFixedMapChance)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUseWeatherConditions)
                .addComponent(chkUseLightConditions)
                .addComponent(chkUsePlanetaryConditions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblFixedMapChance)
                    .addComponent(spnFixedMapChance)));

        return panel;
    }

    private JPanel createUniversalCampaignOptionsPanel() {
        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalCampaignOptionsPanel", false,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(pnlPartsPanel)
                .addComponent(pnlLancePanel));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(pnlPartsPanel)
                .addComponent(pnlLancePanel));

        return panel;
    }

    private JPanel createUniversalPartsPanel() {
        // Content
        chkRestrictPartsByMission = new CampaignOptionsCheckBox("RestrictPartsByMission");
        lblBonusPartExchangeValue = new CampaignOptionsLabel("BonusPartExchangeValue");
        spnBonusPartExchangeValue = new CampaignOptionsSpinner("BonusPartExchangeValue",
            500000, 0, 1000000, 1);
        lblBonusPartMaxExchangeCount = new CampaignOptionsLabel("BonusPartMaxExchangeCount");
        spnBonusPartMaxExchangeCount = new CampaignOptionsSpinner("BonusPartMaxExchangeCount",
            10, 0, 100, 1);

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalPartsPanel", true,
            "UniversalPartsPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkRestrictPartsByMission)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblBonusPartExchangeValue)
                    .addComponent(spnBonusPartExchangeValue))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblBonusPartMaxExchangeCount)
                    .addComponent(spnBonusPartMaxExchangeCount)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkRestrictPartsByMission)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblBonusPartExchangeValue)
                    .addComponent(spnBonusPartExchangeValue))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblBonusPartMaxExchangeCount)
                    .addComponent(spnBonusPartMaxExchangeCount)));

        return panel;
    }

    private JPanel createUniversalLancePanel() {
        // Content
        chkLimitLanceWeight = new CampaignOptionsCheckBox("LimitLanceWeight");
        chkLimitLanceNumUnits = new CampaignOptionsCheckBox("LimitLanceNumUnits");
        chkUseStrategy = new CampaignOptionsCheckBox("UseStrategy");
        lblBaseStrategyDeployment = new CampaignOptionsLabel("BaseStrategyDeployment");
        spnBaseStrategyDeployment = new CampaignOptionsSpinner("BaseStrategyDeployment",
            0, 0, 10, 1);
        lblAdditionalStrategyDeployment = new CampaignOptionsLabel("AdditionalStrategyDeployment");
        spnAdditionalStrategyDeployment = new CampaignOptionsSpinner("AdditionalStrategyDeployment",
            0, 0, 10, 1);
        chkAdjustPaymentForStrategy = new CampaignOptionsCheckBox("AdjustPaymentForStrategy");

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalLancePanel", true,
            "UniversalLancePanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkLimitLanceWeight)
                .addComponent(chkLimitLanceNumUnits)
                .addComponent(chkUseStrategy)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblBaseStrategyDeployment)
                    .addComponent(spnBaseStrategyDeployment))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAdditionalStrategyDeployment)
                    .addComponent(spnAdditionalStrategyDeployment))
                .addComponent(chkAdjustPaymentForStrategy));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkLimitLanceWeight)
                .addComponent(chkLimitLanceNumUnits)
                .addComponent(chkUseStrategy)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblBaseStrategyDeployment)
                    .addComponent(spnBaseStrategyDeployment))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAdditionalStrategyDeployment)
                    .addComponent(spnAdditionalStrategyDeployment))
                .addComponent(chkAdjustPaymentForStrategy));

        return panel;
    }

    private void initializeStratConTab() {
        chkUseStratCon = new JCheckBox();
        chkUseGenericBattleValue = new JCheckBox();
        chkUseVerboseBidding = new JCheckBox();
    }

    JPanel createStratConTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("StratConTab",
            getImageDirectory() + "logo_lyran_alliance.png",
            true);

        // Content
        chkUseStratCon = new CampaignOptionsCheckBox("UseStratCon");
        chkUseGenericBattleValue = new CampaignOptionsCheckBox("UseGenericBattleValue");
        chkUseVerboseBidding = new CampaignOptionsCheckBox("UseVerboseBidding");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("StratConTab", true);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseStratCon)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblSkillLevel)
                    .addComponent(comboSkillLevel))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(chkUseGenericBattleValue)
                    .addComponent(chkUseVerboseBidding))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlScenarioGenerationPanel)
                    .addComponent(pnlCampaignOptions)));


        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addComponent(chkUseStratCon)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblSkillLevel)
                    .addComponent(comboSkillLevel))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(chkUseGenericBattleValue)
                    .addComponent(chkUseVerboseBidding))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlScenarioGenerationPanel)
                    .addComponent(pnlCampaignOptions)));

        // Create panel and return
        return createParentPanel(panel, "StratConTab");
    }

    private void initializeLegacyTab() {
        // General
        chkUseAtB = new JCheckBox();

        // OpFor Generation
        pnlLegacyOpForGenerationPanel = new JPanel();
        chkUseVehicles = new JCheckBox();
        chkDoubleVehicles = new JCheckBox();
        chkOpForUsesAero = new JCheckBox();
        lblOpForAeroChance = new JLabel();
        spnOpForAeroChance = new JSpinner();
        chkOpForUsesLocalForces = new JCheckBox();
        chkAdjustPlayerVehicles = new JCheckBox();

        // Scenarios
        pnlLegacyScenarioGenerationPanel = new JPanel();
        chkGenerateChases = new JCheckBox();
        lblIntensity = new JLabel();
        spnAtBBattleIntensity = new JSpinner();
        lblFightChance = new JLabel();
        lblDefendChance = new JLabel();
        lblScoutChance = new JLabel();
        lblTrainingChance = new JLabel();
        spnAtBBattleChance = new JSpinner[AtBLanceRole.values().length - 1];
        btnIntensityUpdate = new JButton();
    }

    JPanel createLegacyTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("LegacyTab",
            getImageDirectory() + "logo_clan_snow_raven.png",
            true);

        chkUseAtB = new CampaignOptionsCheckBox("UseAtB");
        pnlLegacyOpForGenerationPanel = createLegacyOpForGenerationPanel();
        pnlLegacyScenarioGenerationPanel = createLegacyScenarioGenerationPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyTab", true,
            "");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseAtB)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlLegacyOpForGenerationPanel)
                    .addComponent(pnlLegacyScenarioGenerationPanel)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addComponent(chkUseAtB)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlLegacyOpForGenerationPanel)
                    .addComponent(pnlLegacyScenarioGenerationPanel)));

        // Create panel and return
        return createParentPanel(panel, "LegacyTab");
    }

    private JPanel createLegacyOpForGenerationPanel() {
        // Content
        chkUseVehicles = new CampaignOptionsCheckBox("UseVehicles");
        chkDoubleVehicles = new CampaignOptionsCheckBox("DoubleVehicles");
        chkOpForUsesAero = new CampaignOptionsCheckBox("OpForUsesAero");
        lblOpForAeroChance = new CampaignOptionsLabel("OpForAeroChance");
        spnOpForAeroChance = new CampaignOptionsSpinner("OpForAeroChance",
            0, 0, 6, 1);
        chkOpForUsesLocalForces = new CampaignOptionsCheckBox("OpForUsesLocalForces");
        chkAdjustPlayerVehicles = new CampaignOptionsCheckBox("AdjustPlayerVehicles");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyOpForGenerationPanel", true,
            "LegacyOpForGenerationPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseVehicles)
                .addComponent(chkDoubleVehicles)
                .addComponent(chkOpForUsesAero)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblOpForAeroChance)
                    .addComponent(spnOpForAeroChance))
                .addComponent(chkOpForUsesLocalForces)
                .addComponent(chkAdjustPlayerVehicles));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUseVehicles)
                .addComponent(chkDoubleVehicles)
                .addComponent(chkOpForUsesAero)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblOpForAeroChance)
                    .addComponent(spnOpForAeroChance))
                .addComponent(chkOpForUsesLocalForces)
                .addComponent(chkAdjustPlayerVehicles));

        return panel;
    }

    private JPanel createLegacyScenarioGenerationPanel() {
        // Content
        chkGenerateChases = new CampaignOptionsCheckBox("GenerateChases");
        lblIntensity = new CampaignOptionsLabel("AtBBattleIntensity");
        spnAtBBattleIntensity = new CampaignOptionsSpinner("AtBBattleIntensity",
            0.0, 0.0, 100.0, 0.1);

        lblFightChance = new JLabel(AtBLanceRole.FIGHTING.toString());
        lblDefendChance = new JLabel(AtBLanceRole.DEFENCE.toString());
        lblScoutChance = new JLabel(AtBLanceRole.SCOUTING.toString());
        lblTrainingChance = new JLabel(AtBLanceRole.TRAINING.toString());
        spnAtBBattleChance = new JSpinner[AtBLanceRole.values().length - 1];

        for (int i = 0; i < spnAtBBattleChance.length; i++) {
            spnAtBBattleChance[i] = new JSpinner(
                new SpinnerNumberModel(0, 0, 100, 1));
        }

        btnIntensityUpdate = new CampaignOptionsButton("IntensityUpdate");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyScenarioGenerationPanel", true,
            "LegacyScenarioGenerationPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkGenerateChases)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblIntensity)
                    .addComponent(spnAtBBattleIntensity))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFightChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDefendChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblScoutChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTrainingChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()]))
                .addComponent(btnIntensityUpdate));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkGenerateChases)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblIntensity)
                    .addComponent(spnAtBBattleIntensity))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblFightChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()]))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblDefendChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()]))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblScoutChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()]))
                .addGroup(layout.createSequentialGroup()
                    .addGap(UIUtil.scaleForGUI(25))
                    .addComponent(lblTrainingChance)
                    .addComponent(spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()]))
                .addComponent(btnIntensityUpdate));

        return panel;
    }

    private double determineAtBBattleIntensity() {
        double intensity = 0.0;

        int x = (Integer) spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].getValue();
        intensity += ((-3.0 / 2.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].getValue();
        intensity += ((-4.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].getValue();
        intensity += ((-2.0 / 3.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].getValue();
        intensity += ((-9.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        intensity = intensity / 4.0;

        if (intensity > 100.0) {
            intensity = 100.0;
        }

        return Math.round(intensity * 10.0) / 10.0;
    }

    private class AtBBattleIntensityChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            double intensity = (Double) spnAtBBattleIntensity.getValue();

            if (intensity >= AtBContract.MINIMUM_INTENSITY) {
                int value = (int) Math.min(
                    Math.round(400.0 * intensity / (4.0 * intensity + 6.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(200.0 * intensity / (2.0 * intensity + 8.0) + 0.05),
                    100);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(600.0 * intensity / (6.0 * intensity + 4.0) + 0.05),
                    100);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(100.0 * intensity / (intensity + 9.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(value);
            } else {
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(0);
            }
        }
    }
}
