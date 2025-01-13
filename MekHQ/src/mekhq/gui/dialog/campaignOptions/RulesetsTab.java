package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Skills;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.*;

public class RulesetsTab {
    private final CampaignOptions campaignOptions;

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

    RulesetsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 3;
        panel.add(pnlUnitRatioPanel, layout);

        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUseDropShips, layout);

        layout.gridy++;
        panel.add(chkOpForUsesVTOLs, layout);

        layout.gridy++;
        panel.add(chkClanVehicles, layout);

        layout.gridy++;
        panel.add(chkRegionalMekVariations, layout);

        layout.gridy++;
        panel.add(chkAttachedPlayerCamouflage, layout);

        layout.gridy++;
        panel.add(chkPlayerControlsAttachedUnits, layout);

        layout.gridy++;
        panel.add(chkAutoConfigMunitions, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblSPAUpgradeIntensity, layout);
        layout.gridx++;
        panel.add(spnSPAUpgradeIntensity, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 3;
        panel.add(pnlScenarioModifiers, layout);

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblOpForLanceTypeMeks, layout);
        layout.gridx++;
        panel.add(spnOpForLanceTypeMeks, layout);
        layout.gridx++;
        panel.add(lblOpForLanceTypeMixed, layout);
        layout.gridx++;
        panel.add(spnOpForLanceTypeMixed, layout);
        layout.gridx++;
        panel.add(lblOpForLanceTypeVehicle, layout);
        layout.gridx++;
        panel.add(spnOpForLanceTypeVehicles, layout);

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblScenarioModMax, layout);
        layout.gridx++;
        panel.add(spnScenarioModMax, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblScenarioModChance, layout);
        layout.gridx++;
        panel.add(spnScenarioModChance, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblScenarioModBV, layout);
        layout.gridx++;
        panel.add(spnScenarioModBV, layout);

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkUseWeatherConditions, layout);

        layout.gridy++;
        panel.add(chkUseLightConditions, layout);

        layout.gridy++;
        panel.add(chkUsePlanetaryConditions, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblFixedMapChance, layout);
        layout.gridx++;
        panel.add(spnFixedMapChance, layout);

        return panel;
    }

    private JPanel createUniversalCampaignOptionsPanel() {
        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalCampaignOptionsPanel");
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 2;
        layout.gridy = 0;
        layout.gridx = 0;
        panel.add(pnlPartsPanel, layout);
        layout.gridy++;
        panel.add(pnlLancePanel, layout);
        layout.gridy++;
        panel.add(pnlMapGenerationPanel, layout);

        return panel;
    }

    private JPanel createUniversalPartsPanel() {
        // Content
        chkRestrictPartsByMission = new CampaignOptionsCheckBox("RestrictPartsByMission");

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalPartsPanel", true,
            "UniversalPartsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkRestrictPartsByMission, layout);

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkLimitLanceWeight, layout);

        layout.gridy++;
        panel.add(chkLimitLanceNumUnits, layout);

        layout.gridy++;
        panel.add(chkUseStrategy, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblBaseStrategyDeployment, layout);
        layout.gridx++;
        panel.add(spnBaseStrategyDeployment, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAdditionalStrategyDeployment, layout);
        layout.gridx++;
        panel.add(spnAdditionalStrategyDeployment, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkAdjustPaymentForStrategy, layout);

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
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(chkUseStratCon, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSkillLevel, layout);
        layout.gridx++;
        panel.add(comboSkillLevel, layout);
        layout.gridx++;
        panel.add(chkUseGenericBattleValue, layout);
        layout.gridx++;
        panel.add(chkUseVerboseBidding, layout);

        layout.gridwidth = 2;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(pnlScenarioGenerationPanel, layout);
        layout.gridx += 2;
        panel.add(pnlCampaignOptions, layout);

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
        spnAtBBattleChance = new JSpinner[CombatRole.values().length - 1];
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
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(chkUseAtB, layout);

        layout.gridy++;
        panel.add(pnlLegacyOpForGenerationPanel, layout);
        layout.gridx++;
        panel.add(pnlLegacyScenarioGenerationPanel, layout);

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
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkUseVehicles, layout);

        layout.gridy++;
        panel.add(chkDoubleVehicles, layout);

        layout.gridy++;
        panel.add(chkOpForUsesAero, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblOpForAeroChance, layout);
        layout.gridx++;
        panel.add(spnOpForAeroChance, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkOpForUsesLocalForces, layout);

        layout.gridy++;
        panel.add(chkAdjustPlayerVehicles, layout);

        return panel;
    }

    private JPanel createLegacyScenarioGenerationPanel() {
        // Content
        chkGenerateChases = new CampaignOptionsCheckBox("GenerateChases");
        lblIntensity = new CampaignOptionsLabel("AtBBattleIntensity");
        spnAtBBattleIntensity = new CampaignOptionsSpinner("AtBBattleIntensity",
            0.0, 0.0, 100.0, 0.1);

        lblFightChance = new JLabel(CombatRole.MANEUVER.toString());
        lblDefendChance = new JLabel(CombatRole.FRONTLINE.toString());
        lblScoutChance = new JLabel(CombatRole.PATROL.toString());
        lblTrainingChance = new JLabel(CombatRole.TRAINING.toString());
        spnAtBBattleChance = new JSpinner[CombatRole.values().length - 1];

        for (int i = 0; i < spnAtBBattleChance.length; i++) {
            spnAtBBattleChance[i] = new JSpinner(
                new SpinnerNumberModel(0, 0, 100, 1));
        }

        btnIntensityUpdate = new CampaignOptionsButton("IntensityUpdate");
        AtBBattleIntensityChangeListener atBBattleIntensityChangeListener = new AtBBattleIntensityChangeListener();
        btnIntensityUpdate.addChangeListener(evt -> {
            spnAtBBattleIntensity.removeChangeListener(atBBattleIntensityChangeListener);
            spnAtBBattleIntensity.setValue(determineAtBBattleIntensity());
            spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        });

        // Layout the Panel
        final JPanel panelBattleChance = new CampaignOptionsStandardPanel("LegacyScenarioGenerationPanel");
        final GridBagConstraints layoutBattleChance = new CampaignOptionsGridBagConstraints(panelBattleChance);

        layoutBattleChance.gridx = 0;
        layoutBattleChance.gridy = 0;
        layoutBattleChance.gridwidth = 1;
        panelBattleChance.add(lblFightChance, layoutBattleChance);
        layoutBattleChance.gridx++;
        panelBattleChance.add(spnAtBBattleChance[CombatRole.MANEUVER.ordinal()], layoutBattleChance);

        layoutBattleChance.gridx = 0;
        layoutBattleChance.gridy++;
        panelBattleChance.add(lblDefendChance, layoutBattleChance);
        layoutBattleChance.gridx++;
        panelBattleChance.add(spnAtBBattleChance[CombatRole.FRONTLINE.ordinal()], layoutBattleChance);

        layoutBattleChance.gridx = 0;
        layoutBattleChance.gridy++;
        panelBattleChance.add(lblScoutChance, layoutBattleChance);
        layoutBattleChance.gridx++;
        panelBattleChance.add(spnAtBBattleChance[CombatRole.PATROL.ordinal()], layoutBattleChance);

        layoutBattleChance.gridx = 0;
        layoutBattleChance.gridy++;
        panelBattleChance.add(lblTrainingChance, layoutBattleChance);
        layoutBattleChance.gridx++;
        panelBattleChance.add(spnAtBBattleChance[CombatRole.TRAINING.ordinal()], layoutBattleChance);

        final JPanel panel = new CampaignOptionsStandardPanel("LegacyScenarioGenerationPanel", true,
            "LegacyScenarioGenerationPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkGenerateChases, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblIntensity, layout);
        layout.gridx++;
        panel.add(spnAtBBattleIntensity, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(panelBattleChance, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(btnIntensityUpdate, layout);

        return panel;
    }

    private double determineAtBBattleIntensity() {
        double intensity = 0.0;

        int x = (int) spnAtBBattleChance[CombatRole.MANEUVER.ordinal()].getValue();
        intensity += ((-3.0 / 2.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (int) spnAtBBattleChance[CombatRole.FRONTLINE.ordinal()].getValue();
        intensity += ((-4.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (int) spnAtBBattleChance[CombatRole.PATROL.ordinal()].getValue();
        intensity += ((-2.0 / 3.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (int) spnAtBBattleChance[CombatRole.TRAINING.ordinal()].getValue();
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
            double intensity = (double) spnAtBBattleIntensity.getValue();

            if (intensity >= AtBContract.MINIMUM_INTENSITY) {
                int value = (int) Math.min(
                    Math.round(400.0 * intensity / (4.0 * intensity + 6.0) + 0.05), 100);
                spnAtBBattleChance[CombatRole.MANEUVER.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(200.0 * intensity / (2.0 * intensity + 8.0) + 0.05),
                    100);
                spnAtBBattleChance[CombatRole.FRONTLINE.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(600.0 * intensity / (6.0 * intensity + 4.0) + 0.05),
                    100);
                spnAtBBattleChance[CombatRole.PATROL.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(100.0 * intensity / (intensity + 9.0) + 0.05), 100);
                spnAtBBattleChance[CombatRole.TRAINING.ordinal()].setValue(value);
            } else {
                spnAtBBattleChance[CombatRole.MANEUVER.ordinal()].setValue(0);
                spnAtBBattleChance[CombatRole.FRONTLINE.ordinal()].setValue(0);
                spnAtBBattleChance[CombatRole.PATROL.ordinal()].setValue(0);
                spnAtBBattleChance[CombatRole.TRAINING.ordinal()].setValue(0);
            }
        }
    }

    void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Universal
        options.setSkillLevel(comboSkillLevel.getSelectedItem());
        options.setOpForLanceTypeMeks((int) spnOpForLanceTypeMeks.getValue());
        options.setOpForLanceTypeMixed((int) spnOpForLanceTypeMixed.getValue());
        options.setOpForLanceTypeVehicles((int) spnOpForLanceTypeVehicles.getValue());
        options.setUseDropShips(chkUseDropShips.isSelected());
        options.setOpForUsesVTOLs(chkOpForUsesVTOLs.isSelected());
        options.setClanVehicles(chkClanVehicles.isSelected());
        options.setRegionalMekVariations(chkRegionalMekVariations.isSelected());
        options.setAttachedPlayerCamouflage(chkAttachedPlayerCamouflage.isSelected());
        options.setPlayerControlsAttachedUnits(chkPlayerControlsAttachedUnits.isSelected());
        options.setSpaUpgradeIntensity((int) spnSPAUpgradeIntensity.getValue());
        options.setAutoConfigMunitions(chkAutoConfigMunitions.isSelected());
        options.setScenarioModMax((int) spnScenarioModMax.getValue());
        options.setScenarioModChance((int) spnScenarioModChance.getValue());
        options.setScenarioModBV((int) spnScenarioModBV.getValue());
        options.setUseWeatherConditions(chkUseWeatherConditions.isSelected());
        options.setUseLightConditions(chkUseLightConditions.isSelected());
        options.setUsePlanetaryConditions(chkUsePlanetaryConditions.isSelected());
        options.setFixedMapChance((int) spnFixedMapChance.getValue());
        options.setRestrictPartsByMission(chkRestrictPartsByMission.isSelected());
        options.setLimitLanceWeight(chkLimitLanceWeight.isSelected());
        options.setLimitLanceNumUnits(chkLimitLanceNumUnits.isSelected());
        options.setUseStrategy(chkUseStrategy.isSelected());
        options.setBaseStrategyDeployment((int) spnBaseStrategyDeployment.getValue());
        options.setAdditionalStrategyDeployment((int) spnAdditionalStrategyDeployment.getValue());
        options.setAdjustPaymentForStrategy(chkAdjustPaymentForStrategy.isSelected());

        // StratCon
        options.setUseStratCon(chkUseStratCon.isSelected());
        options.setUseGenericBattleValue(chkUseGenericBattleValue.isSelected());
        options.setUseVerboseBidding(chkUseVerboseBidding.isSelected());

        // Legacy
        options.setUseAtB(chkUseAtB.isSelected());
        options.setUseVehicles(chkUseVehicles.isSelected());
        options.setDoubleVehicles(chkDoubleVehicles.isSelected());
        options.setUseAero(chkOpForUsesAero.isSelected());
        options.setOpForAeroChance((int) spnOpForAeroChance.getValue());
        options.setAllowOpForLocalUnits(chkOpForUsesLocalForces.isSelected());
        options.setAdjustPlayerVehicles(chkAdjustPlayerVehicles.isSelected());
        options.setGenerateChases(chkGenerateChases.isSelected());

        for (int i = 0; i < spnAtBBattleChance.length; i++) {
            options.setAtBBattleChance(i, (int) spnAtBBattleChance[i].getValue());
        }
    }

    void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Universal
        comboSkillLevel.setSelectedItem(options.getSkillLevel());
        spnOpForLanceTypeMeks.setValue(options.getOpForLanceTypeMeks());
        spnOpForLanceTypeMixed.setValue(options.getOpForLanceTypeMixed());
        spnOpForLanceTypeVehicles.setValue(options.getOpForLanceTypeVehicles());
        chkUseDropShips.setSelected(options.isUseDropShips());
        chkOpForUsesVTOLs.setSelected(options.isOpForUsesVTOLs());
        chkClanVehicles.setSelected(options.isClanVehicles());
        chkRegionalMekVariations.setSelected(options.isRegionalMekVariations());
        chkAttachedPlayerCamouflage.setSelected(options.isAttachedPlayerCamouflage());
        chkPlayerControlsAttachedUnits.setSelected(options.isPlayerControlsAttachedUnits());
        spnSPAUpgradeIntensity.setValue(options.getSpaUpgradeIntensity());
        chkAutoConfigMunitions.setSelected(options.isAutoConfigMunitions());
        spnScenarioModMax.setValue(options.getScenarioModMax());
        spnScenarioModChance.setValue(options.getScenarioModChance());
        spnScenarioModBV.setValue(options.getScenarioModBV());
        chkUseWeatherConditions.setSelected(options.isUseWeatherConditions());
        chkUseLightConditions.setSelected(options.isUseLightConditions());
        chkUsePlanetaryConditions.setSelected(options.isUsePlanetaryConditions());
        spnFixedMapChance.setValue(options.getFixedMapChance());
        chkRestrictPartsByMission.setSelected(options.isRestrictPartsByMission());
        chkLimitLanceWeight.setSelected(options.isLimitLanceWeight());
        chkLimitLanceNumUnits.setSelected(options.isLimitLanceNumUnits());
        chkUseStrategy.setSelected(options.isUseStrategy());
        spnBaseStrategyDeployment.setValue(options.getBaseStrategyDeployment());
        spnAdditionalStrategyDeployment.setValue(options.getAdditionalStrategyDeployment());
        chkAdjustPaymentForStrategy.setSelected(options.isAdjustPaymentForStrategy());

        // StratCon
        chkUseStratCon.setSelected(options.isUseStratCon());
        chkUseGenericBattleValue.setSelected(options.isUseGenericBattleValue());
        chkUseVerboseBidding.setSelected(options.isUseVerboseBidding());

        // Legacy
        chkUseAtB.setSelected(options.isUseAtB());
        chkUseVehicles.setSelected(options.isUseVehicles());
        chkDoubleVehicles.setSelected(options.isDoubleVehicles());
        chkOpForUsesAero.setSelected(options.isUseAero());
        spnOpForAeroChance.setValue(options.getOpForAeroChance());
        chkOpForUsesLocalForces.setSelected(options.isAllowOpForLocalUnits());
        chkAdjustPlayerVehicles.setSelected(options.isAdjustPlayerVehicles());
        chkGenerateChases.setSelected(options.isGenerateChases());
        for (CombatRole role : CombatRole.values()) {
            if (role.ordinal() <= CombatRole.TRAINING.ordinal()) {
                spnAtBBattleChance[role.ordinal()].setValue(options.getAtBBattleChance(role));
            }
        }
    }
}
