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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import java.awt.GridBagConstraints;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.Version;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.models.FileNameComboBoxModel;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.BoardScalingType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.stratCon.StratConPlayType;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * Represents a tab in the campaign options UI for managing ruleset configurations in campaigns.
 * <p>
 * This class organizes and manages options related to universal rules, legacy AtB rules (Against the Bot), and StratCon
 * (Strategic Context) settings. It provides a UI to customize configurations such as opponent force generation,
 * scenario rules, equipment behavior, and campaign-specific variations.
 * </p>
 *
 * <strong>Tab Sections:</strong>
 * <ul>
 *     <li><b>Universal Options:</b> Handles features applicable to all campaigns,
 *         such as skill levels, unit ratios, map conditions, and auto-resolve settings.</li>
 *     <li><b>Legacy AtB:</b> Legacy-specific rules for opponent force generation,
 *         scenario generation probabilities, and battle intensity configurations.</li>
 *     <li><b>StratCon:</b> Settings for Strategic Context campaigns, including BV usage
 *         (Battle Values) and verbose bidding options.</li>
 * </ul>
 */
public class RulesetsTab {
    private final CampaignOptions campaignOptions;
      private RulesetsDraft draft;
      private boolean stratConPageCreated;

    //start Universal Options
    private JLabel lblSkillLevel;
    private MMComboBox<SkillLevel> comboSkillLevel;
    private JLabel lblBoardScalingType;
    private MMComboBox<BoardScalingType> comboBoardScalingType;
    private JPanel pnlScenarioGenerationPanel;
    private JPanel pnlCampaignOptions;

    private JPanel pnlUnitRatioPanel;
    private JLabel lblOpForLanceTypeMeks;
    private JSpinner spnOpForLanceTypeMeks;
    private JLabel lblOpForLanceTypeMixed;
    private JSpinner spnOpForLanceTypeMixed;
    private JLabel lblOpForLanceTypeVehicle;
    private JSpinner spnOpForLanceTypeVehicles;

    private JPanel pnlCallSigns;
    private JCheckBox chkAutoGenerateOpForCallSigns;
    private JLabel lblMinimumCallsignSkillLevel;
    private MMComboBox<SkillLevel> comboMinimumCallsignSkillLevel;

    private JCheckBox chkUseDropShips;

    private JCheckBox chkRegionalMekVariations;

    private JCheckBox chkAttachedPlayerCamouflage;
    private JCheckBox chkPlayerControlsAttachedUnits;
    private JCheckBox chkUseAdvancedBuildingGunEmplacements;
    private JLabel lblSPAUpgradeIntensity;
    private JSpinner spnSPAUpgradeIntensity;
    private JCheckBox chkAutoConfigMunitions;

    private JPanel pnlScenarioModifiers;
    private JLabel lblEnemyFacilityModifierDieSize;
    private JSpinner spnEnemyFacilityModifierDieSize;
    private JLabel lblAlliedFacilityModifierDieSize;
    private JSpinner spnAlliedFacilityModifierDieSize;
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
    private JCheckBox chkUseNoTornadoes;
    private JLabel lblFixedMapChance;
    private JSpinner spnFixedMapChance;

    private JPanel pnlMorale;
    private JLabel lblMoraleVictory;
    private JSpinner spnMoraleVictory;
    private JLabel lblMoraleDecisiveVictory;
    private JSpinner spnMoraleDecisiveVictory;
    private JLabel lblMoraleDefeat;
    private JSpinner spnMoraleDefeat;
    private JLabel lblMoraleDecisiveDefeat;
    private JSpinner spnMoraleDecisiveDefeat;

    private JPanel pnlPartsPanel;
    private JCheckBox chkRestrictPartsByMission;

    private JPanel pnlAutoResolve;
    private JLabel lblAutoResolveMethod;
    private MMComboBox<AutoResolveMethod> comboAutoResolveMethod;
    private MMComboBox<String> minimapThemeSelector;
    private JCheckBox chkAutoResolveVictoryChanceEnabled;
    private JLabel lblMinimapTheme;
    private JCheckBox chkAutoResolveExperimentalPacarGuiEnabled;
    private JLabel lblAutoResolveNumberOfScenarios;
    private JSpinner spnAutoResolveNumberOfScenarios;
    //end Universal Options

    //start Legacy Options
    private CampaignOptionsHeaderPanel legacyHeader;
    // end Legacy Options

    private JLabel lblStratConPlayType;
    private MMComboBox<StratConPlayType> comboStratConPlayType;
    private JCheckBox chkUseAdvancedScouting;
    private JCheckBox chkNoSeedForces;
    private JCheckBox chkUseGenericBattleValue;
    private JCheckBox chkUseVerboseBidding;
    //end StratCon

    /**
     * Constructs a {@code RulesetsTab} instance for managing ruleset options.
     *
     * @param campaignOptions the {@link CampaignOptions} object to manage repair, maintenance, and other ruleset
     *                        options.
     */
    public RulesetsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
            loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the tab by setting up all three sections:
     * <p>
     * <li>Universal Options</li>
     * <li>StratCon Tab</li>
     * <li>Legacy Tab</li>
     * </p>
     */
    private void initialize() {
        initializeUniversalOptions();
        initializeStratConTab();
        initializeLegacyTab();
    }

    /**
     * Initializes the universal options section of the tab.
     * <p>
     * Universal options include settings like skill levels, scenario modifiers, map generation parameters, and
     * auto-resolve behavior.
     * </p>
     */
    private void initializeUniversalOptions() {
        // General
        lblSkillLevel = new JLabel();
        comboSkillLevel = new MMComboBox<>("comboSkillLevel", getSkillLevelOptions());
        lblBoardScalingType = new JLabel();
        comboBoardScalingType = new MMComboBox<>("comboBoardScalingType", BoardScalingType.values());
        pnlScenarioGenerationPanel = new JPanel();

        // CallSigns
        pnlCallSigns = new JPanel();
        chkAutoGenerateOpForCallSigns = new JCheckBox();
        lblMinimumCallsignSkillLevel = new JLabel();

        // OpFor Generation
        pnlUnitRatioPanel = new JPanel();
        lblOpForLanceTypeMeks = new JLabel();
        spnOpForLanceTypeMeks = new JSpinner();
        lblOpForLanceTypeMixed = new JLabel();
        spnOpForLanceTypeMixed = new JSpinner();
        lblOpForLanceTypeVehicle = new JLabel();
        spnOpForLanceTypeVehicles = new JSpinner();

        chkUseDropShips = new JCheckBox();
        chkRegionalMekVariations = new JCheckBox();

        chkAttachedPlayerCamouflage = new JCheckBox();
        chkPlayerControlsAttachedUnits = new JCheckBox();

        lblSPAUpgradeIntensity = new JLabel();
        spnSPAUpgradeIntensity = new JSpinner();
        chkAutoConfigMunitions = new JCheckBox();

        pnlScenarioModifiers = new JPanel();
        lblEnemyFacilityModifierDieSize = new JLabel();
        spnEnemyFacilityModifierDieSize = new JSpinner();
        lblAlliedFacilityModifierDieSize = new JLabel();
        spnAlliedFacilityModifierDieSize = new JSpinner();
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
        chkUseNoTornadoes = new JCheckBox();
        lblFixedMapChance = new JLabel();
        spnFixedMapChance = new JSpinner();

        // Morale
        pnlMorale = new JPanel();
        lblMoraleVictory = new JLabel();
        spnMoraleVictory = new JSpinner();
        lblMoraleDecisiveVictory = new JLabel();
        spnMoraleDecisiveVictory = new JSpinner();
        lblMoraleDefeat = new JLabel();
        spnMoraleDefeat = new JSpinner();
        lblMoraleDecisiveDefeat = new JLabel();
        spnMoraleDecisiveDefeat = new JSpinner();

        // Parts
        pnlPartsPanel = new JPanel();
        chkRestrictPartsByMission = new JCheckBox();

        // Auto Resolve
        pnlAutoResolve = new JPanel();
        lblAutoResolveMethod = new JLabel();
        final DefaultComboBoxModel<AutoResolveMethod> autoResolveTypeModel = new DefaultComboBoxModel<>(
              AutoResolveMethod.values());
        comboAutoResolveMethod = new MMComboBox<>("comboAutoResolveMethod", autoResolveTypeModel);
        minimapThemeSelector = new MMComboBox<>("minimapThemeSelector",
              new FileNameComboBoxModel(GUIPreferences.getInstance().getMinimapThemes()));
        chkAutoResolveVictoryChanceEnabled = new JCheckBox();
        lblAutoResolveNumberOfScenarios = new JLabel();
        spnAutoResolveNumberOfScenarios = new JSpinner();
        lblMinimapTheme = new JLabel();
        chkAutoResolveExperimentalPacarGuiEnabled = new JCheckBox();
        // Here we set up the options, so they can be used across both the AtB and StratCon tabs
        substantializeUniversalOptions();
    }

    /**
     * Configures and initializes universal options components for use across tabs.
     * <p>
     * This method sets up and organizes the various UI elements for universal options, such as skill levels, scenario
     * generation, map generation, and more. These initialized components are then used in other methods to build the
     * complete universal options UI.
     * </p>
     */
    private void substantializeUniversalOptions() {
        // General
        lblSkillLevel = new CampaignOptionsLabel("SkillLevel");
        lblBoardScalingType = new CampaignOptionsLabel("BoardScalingType",
              getMetadata(MILESTONE_BEFORE_METADATA));

        // CallSigns
        pnlCallSigns = createCallSignsPanel();

        // OpFor Generation
        pnlUnitRatioPanel = createUniversalUnitRatioPanel();

        chkUseDropShips = new CampaignOptionsCheckBox("UseDropShips");
        chkRegionalMekVariations = new CampaignOptionsCheckBox("RegionalMekVariations");

        chkAttachedPlayerCamouflage = new CampaignOptionsCheckBox("AttachedPlayerCamouflage");
        chkPlayerControlsAttachedUnits = new CampaignOptionsCheckBox("PlayerControlsAttachedUnits");
        chkUseAdvancedBuildingGunEmplacements = new CampaignOptionsCheckBox("UseAdvancedBuildingGunEmplacements",
              getMetadata(new Version(0, 50, 12)));
        lblSPAUpgradeIntensity = new CampaignOptionsLabel("SPAUpgradeIntensity");
        spnSPAUpgradeIntensity = new CampaignOptionsSpinner("SPAUpgradeIntensity",
              0, -1, 3, 1);
        chkAutoConfigMunitions = new CampaignOptionsCheckBox("AutoConfigMunitions",
              getMetadata(LEGACY_RULE_BEFORE_METADATA,
                    CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.DOCUMENTED));

        // Other
        pnlScenarioModifiers = createUniversalModifiersPanel();
        pnlMapGenerationPanel = createUniversalMapGenerationPanel();
        pnlPartsPanel = createUniversalPartsPanel();
        pnlMorale = createUniversalMoralePanel();

        pnlScenarioGenerationPanel = createUniversalScenarioGenerationPanel();
        pnlCampaignOptions = createUniversalCampaignOptionsPanel();
        pnlAutoResolve = createAutoResolvePanel();
    }

    /**
     * Retrieves the available skill levels as a {@link DefaultComboBoxModel}.
     * <p>
     * Returns the predefined {@link SkillLevel} values, excluding {@link SkillLevel#NONE}. Used for populating the
     * skill level selector in the universal options UI.
     * </p>
     *
     * @return a {@link DefaultComboBoxModel} containing available {@link SkillLevel} options
     */
    private static DefaultComboBoxModel<SkillLevel> getSkillLevelOptions() {
        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>(
              Skills.SKILL_LEVELS);

        skillLevelModel.removeElement(SkillLevel.NONE);

        return skillLevelModel;
    }

    /**
     * Creates the UI panel for configuring universal scenario generation options.
     * <p>
     * Allows users to define settings for opponent force configurations, such as enabling dropships, VTOLs, and clan
     * vehicles, as well as other universal scenario parameters.
     * </p>
     *
     * @return a {@link JPanel} containing controls to configure universal scenario generation
     */
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
        panel.add(chkRegionalMekVariations, layout);

        layout.gridy++;
        panel.add(chkAttachedPlayerCamouflage, layout);

        layout.gridy++;
        panel.add(chkPlayerControlsAttachedUnits, layout);

        layout.gridy++;
        panel.add(chkAutoConfigMunitions, layout);

        layout.gridy++;
        panel.add(chkUseAdvancedBuildingGunEmplacements, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblSPAUpgradeIntensity, layout);
        layout.gridx++;
        panel.add(spnSPAUpgradeIntensity, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 3;
        panel.add(pnlScenarioModifiers, layout);

        layout.gridy++;
        panel.add(pnlCallSigns, layout);

        return panel;
    }

    /**
     * Creates the UI panel for configuring the auto-resolve options in campaigns.
     * <p>
     * Includes controls to set the auto-resolve method, enable victory chance calculation, and specify the number of
     * scenarios to consider during auto-resolution.
     * </p>
     *
     * @return a {@link JPanel} containing controls to configure auto-resolve behavior
     */
    private JPanel createAutoResolvePanel() {
        // Content
        lblAutoResolveMethod = new CampaignOptionsLabel("AutoResolveMethod",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblAutoResolveNumberOfScenarios = new CampaignOptionsLabel("AutoResolveNumberOfScenarios",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        spnAutoResolveNumberOfScenarios = new CampaignOptionsSpinner("AutoResolveNumberOfScenarios",
              250, 10, 1000, 10);
        chkAutoResolveVictoryChanceEnabled = new CampaignOptionsCheckBox("AutoResolveVictoryChanceEnabled",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblMinimapTheme = new CampaignOptionsLabel("MinimapTheme",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkAutoResolveExperimentalPacarGuiEnabled = new CampaignOptionsCheckBox("AutoResolveExperimentalPacarGuiEnabled");

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("AutoResolvePanel", true,
              "AutoResolvePanel",
              getMetadata(LEGACY_RULE_BEFORE_METADATA,
                    CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.DOCUMENTED));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAutoResolveMethod, layout);
        layout.gridy++;
        panel.add(comboAutoResolveMethod, layout);
        layout.gridy++;
        panel.add(chkAutoResolveVictoryChanceEnabled, layout);
        layout.gridy++;
        panel.add(chkAutoResolveExperimentalPacarGuiEnabled, layout);
        layout.gridy++;
        panel.add(lblMinimapTheme, layout);
        layout.gridy++;
        panel.add(minimapThemeSelector, layout);
        layout.gridy++;
        panel.add(lblAutoResolveNumberOfScenarios, layout);
        layout.gridy++;
        panel.add(spnAutoResolveNumberOfScenarios, layout);

        return panel;
    }

    /**
     * Creates the UI panel for configuring unit ratios in universal options.
     * <p>
     * Includes spinners for setting the ratio of various unit types, such as meks, mixed units, and vehicles, for the
     * opponent forces.
     * </p>
     *
     * @return a {@link JPanel} containing controls for unit ratio configuration
     */
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

    private JPanel createCallSignsPanel() {
        // Content
        chkAutoGenerateOpForCallSigns = new CampaignOptionsCheckBox("AutoGenerateOpForCallSigns");
        lblMinimumCallsignSkillLevel = new CampaignOptionsLabel("MinimumCallsignSkillLevel");
        comboMinimumCallsignSkillLevel = new MMComboBox<>("comboMinimumCallsignSkillLevel", getSkillLevelOptions());

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("AutoGeneratedCallSignsPanel", true,
              "AutoGeneratedCallSignsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkAutoGenerateOpForCallSigns, layout);
        layout.gridx++;
        panel.add(lblMinimumCallsignSkillLevel, layout);
        layout.gridx++;
        panel.add(comboMinimumCallsignSkillLevel, layout);

        return panel;
    }

    /**
     * Creates the UI panel for configuring universal scenario modifiers.
     * <p>
     * This panel includes controls to adjust the maximum modifiers for scenario generation, modifier chance
     * percentages, and BV (Battle Value) impact. It is designed to provide flexible settings for campaign
     * customization.
     * </p>
     *
     * @return a {@link JPanel} containing controls to configure universal scenario modifiers
     */
    private JPanel createUniversalModifiersPanel() {
        //Content
        lblEnemyFacilityModifierDieSize = new CampaignOptionsLabel("EnemyFacilityModifierDieSize");
        spnEnemyFacilityModifierDieSize = new CampaignOptionsSpinner("EnemyFacilityModifierDieSize",
              2, 0, 10, 1);
        lblAlliedFacilityModifierDieSize = new CampaignOptionsLabel("AlliedFacilityModifierDieSize");
        spnAlliedFacilityModifierDieSize = new CampaignOptionsSpinner("AlliedFacilityModifierDieSize",
              2, 0, 10, 1);
        lblScenarioModMax = new CampaignOptionsLabel("ScenarioModMax", getMetadata(new Version(0, 51, 0)));
        spnScenarioModMax = new CampaignOptionsSpinner("ScenarioModMax",
              3, 0, 10, 1);
        lblScenarioModChance = new CampaignOptionsLabel("ScenarioModChance", getMetadata(new Version(0, 51, 0)));
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
        panel.add(lblEnemyFacilityModifierDieSize, layout);
        layout.gridx++;
        panel.add(spnEnemyFacilityModifierDieSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAlliedFacilityModifierDieSize, layout);
        layout.gridx++;
        panel.add(spnAlliedFacilityModifierDieSize, layout);

        layout.gridx = 0;
        layout.gridy++;
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

    /**
     * Creates the UI panel for configuring universal map generation settings.
     * <p>
     * Includes options for enabling weather, light, planetary conditions, and fixed map chances, with spinners and
     * checkboxes for user input.
     * </p>
     *
     * @return a {@link JPanel} containing controls to configure map generation options
     */
    private JPanel createUniversalMapGenerationPanel() {
        // Content
        chkUseWeatherConditions = new CampaignOptionsCheckBox("UseWeatherConditions");
        chkUseLightConditions = new CampaignOptionsCheckBox("UseLightConditions");
        chkUsePlanetaryConditions = new CampaignOptionsCheckBox("UsePlanetaryConditions");
        chkUseNoTornadoes = new CampaignOptionsCheckBox("UseNoTornadoes",
              getMetadata(MILESTONE_BEFORE_METADATA));
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
        panel.add(chkUseNoTornadoes, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblFixedMapChance, layout);
        layout.gridx++;
        panel.add(spnFixedMapChance, layout);

        return panel;
    }

    /**
     * Creates the UI panel that consolidates universal campaign options.
     * <p>
     * This panel combines sub-panels like the parts panel, lance panel, and map generation panel into a single cohesive
     * UI for configuring general campaign options.
     * </p>
     *
     * @return a {@link JPanel} containing all universal campaign options organized in sections
     */
    private JPanel createUniversalCampaignOptionsPanel() {
        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalCampaignOptionsPanel");
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 2;
        layout.gridy = 0;
        layout.gridx = 0;
        panel.add(pnlMorale, layout);
        layout.gridy++;
        panel.add(pnlPartsPanel, layout);
        layout.gridy++;
        panel.add(pnlMapGenerationPanel, layout);

        return panel;
    }

    private JPanel createUniversalMoralePanel() {
        // Content
        lblMoraleDecisiveVictory = new CampaignOptionsLabel("MoraleDecisiveVictory",
              getMetadata(MILESTONE_BEFORE_METADATA));
        spnMoraleDecisiveVictory = new CampaignOptionsSpinner("MoraleDecisiveVictory",
              4, 1, 10, 1,
              getMetadata(MILESTONE_BEFORE_METADATA));

        lblMoraleVictory = new CampaignOptionsLabel("MoraleVictory",
              getMetadata(MILESTONE_BEFORE_METADATA));
        spnMoraleVictory = new CampaignOptionsSpinner("MoraleVictory",
              2, 1, 10, 1,
              getMetadata(MILESTONE_BEFORE_METADATA));

        lblMoraleDefeat = new CampaignOptionsLabel("MoraleDefeat",
              getMetadata(MILESTONE_BEFORE_METADATA));
        spnMoraleDefeat = new CampaignOptionsSpinner("MoraleDefeat",
              -3, -10, -1, 1,
              getMetadata(MILESTONE_BEFORE_METADATA));

        lblMoraleDecisiveDefeat = new CampaignOptionsLabel("MoraleDecisiveDefeat",
              getMetadata(MILESTONE_BEFORE_METADATA));
        spnMoraleDecisiveDefeat = new CampaignOptionsSpinner("MoraleDecisiveDefeat",
              -5, -10, -1, 1,
              getMetadata(MILESTONE_BEFORE_METADATA));

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalMoralePanel", true,
              "UniversalMoralePanel",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblMoraleDecisiveVictory, layout);
        layout.gridx++;
        panel.add(spnMoraleDecisiveVictory, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMoraleVictory, layout);
        layout.gridx++;
        panel.add(spnMoraleVictory, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMoraleDefeat, layout);
        layout.gridx++;
        panel.add(spnMoraleDefeat, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMoraleDecisiveDefeat, layout);
        layout.gridx++;
        panel.add(spnMoraleDecisiveDefeat, layout);

        return panel;
    }

    /**
     * Creates the UI panel for configuring universal parts restrictions during campaigns.
     * <p>
     * Includes settings such as restricting parts availability based on mission requirements.
     * </p>
     *
     * @return a {@link JPanel} containing controls to configure parts-related options for campaigns
     */
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

    /**
     * Initializes the StratCon (Strategic Context) section of the tab.
     */
    private void initializeStratConTab() {
        lblStratConPlayType = new JLabel("StratConPlayType");
        comboStratConPlayType = new MMComboBox<>("StratConPlayType", StratConPlayType.values());
        comboStratConPlayType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                  JList<?> list,
                  Object value,
                  int index,
                  boolean isSelected,
                  boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                      list, value, index, isSelected, cellHasFocus);

                if (value instanceof StratConPlayType type) {
                    label.setToolTipText(type.getTooltip());
                }

                return label;
            }
        });

        chkUseAdvancedScouting = new JCheckBox();
        chkNoSeedForces = new JCheckBox();
        chkUseGenericBattleValue = new JCheckBox();
        chkUseVerboseBidding = new JCheckBox();
    }

    /**
     * Creates the UI panel for the StratCon configuration.
     * <p>
     * This section includes settings for using generic battle values, enabling verbose bidding, and other Strategic
     * Conquest-specific rules.
     * </p>
     *
     * @return a {@link JPanel} containing all StratCon settings.
     */
    public JPanel createStratConTab() {
        // Header
        //start StratCon
        CampaignOptionsHeaderPanel stratConHeader = new CampaignOptionsHeaderPanel("StratConTab",
              getImageDirectory() + "logo_clan_wolf.png",
              false,
              true,
              4);

        // Universal Content

        // Right now the universal content all lives in the StratCon tab, but that might not always be the case if
        // we ever introduce a new Digital GM. So, as this content is initialized before the stratConHeader, we need
        // to wait until now to add the mouse listeners. It's awkward, but it works.
        lblAutoResolveMethod.addMouseListener(createTipPanelUpdater(stratConHeader, "AutoResolveMethod"));
        comboAutoResolveMethod.addMouseListener(createTipPanelUpdater(stratConHeader, "AutoResolveMethod"));
        lblMinimapTheme.addMouseListener(createTipPanelUpdater(stratConHeader, "MinimapTheme"));
        minimapThemeSelector.addMouseListener(createTipPanelUpdater(stratConHeader, "MinimapTheme"));
        lblAutoResolveNumberOfScenarios.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AutoResolveNumberOfScenarios"));
        spnAutoResolveNumberOfScenarios.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AutoResolveNumberOfScenarios"));
        chkAutoResolveVictoryChanceEnabled.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AutoResolveVictoryChanceEnabled"));
        chkAutoResolveExperimentalPacarGuiEnabled.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AutoResolveExperimentalPacarGuiEnabled"));
        lblMoraleVictory.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleVictory"));
        spnMoraleVictory.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleVictory"));
        lblMoraleDecisiveVictory.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDecisiveVictory"));
        spnMoraleDecisiveVictory.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDecisiveVictory"));
        lblMoraleDefeat.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDefeat"));
        spnMoraleDefeat.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDefeat"));
        spnMoraleDecisiveDefeat.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDecisiveDefeat"));
        spnMoraleDecisiveDefeat.addMouseListener(createTipPanelUpdater(stratConHeader, "MoraleDecisiveDefeat"));
        chkRestrictPartsByMission.addMouseListener(createTipPanelUpdater(stratConHeader, "RestrictPartsByMission"));
        chkUseWeatherConditions.addMouseListener(createTipPanelUpdater(stratConHeader, "UseWeatherConditions"));
        chkUseLightConditions.addMouseListener(createTipPanelUpdater(stratConHeader, "UseLightConditions"));
        chkUsePlanetaryConditions.addMouseListener(createTipPanelUpdater(stratConHeader, "UsePlanetaryConditions"));
        chkUseNoTornadoes.addMouseListener(createTipPanelUpdater(stratConHeader, "UseNoTornadoes"));
        lblFixedMapChance.addMouseListener(createTipPanelUpdater(stratConHeader, "FixedMapChance"));
        spnFixedMapChance.addMouseListener(createTipPanelUpdater(stratConHeader, "FixedMapChance"));
        lblEnemyFacilityModifierDieSize.addMouseListener(createTipPanelUpdater(stratConHeader,
              "EnemyFacilityModifierDieSize"));
        spnEnemyFacilityModifierDieSize.addMouseListener(createTipPanelUpdater(stratConHeader,
              "EnemyFacilityModifierDieSize"));
        lblAlliedFacilityModifierDieSize.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AlliedFacilityModifierDieSize"));
        spnAlliedFacilityModifierDieSize.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AlliedFacilityModifierDieSize"));
        lblScenarioModMax.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModMax"));
        spnScenarioModMax.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModMax"));
        lblScenarioModChance.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModChance"));
        spnScenarioModChance.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModChance"));
        lblScenarioModBV.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModBV"));
        spnScenarioModBV.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModBV"));
        lblSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader, "SkillLevel"));
        comboSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader, "SkillLevel"));
        lblBoardScalingType.addMouseListener(createTipPanelUpdater(stratConHeader, "BoardScalingType"));
        comboBoardScalingType.addMouseListener(createTipPanelUpdater(stratConHeader, "BoardScalingType"));
        chkAutoGenerateOpForCallSigns.addMouseListener(createTipPanelUpdater(stratConHeader,
              "AutoGenerateOpForCallSigns"));
        lblMinimumCallsignSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader,
              "MinimumCallsignSkillLevel"));
        comboMinimumCallsignSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader,
              "MinimumCallsignSkillLevel"));
        chkUseDropShips.addMouseListener(createTipPanelUpdater(stratConHeader, "UseDropShips"));
        chkRegionalMekVariations.addMouseListener(createTipPanelUpdater(stratConHeader, "RegionalMekVariations"));
        chkAttachedPlayerCamouflage.addMouseListener(createTipPanelUpdater(stratConHeader, "AttachedPlayerCamouflage"));
        chkPlayerControlsAttachedUnits.addMouseListener(createTipPanelUpdater(stratConHeader,
              "PlayerControlsAttachedUnits"));
        chkUseAdvancedBuildingGunEmplacements.addMouseListener(createTipPanelUpdater(stratConHeader,
              "UseAdvancedBuildingGunEmplacements"));
        lblSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater(stratConHeader, "SPAUpgradeIntensity"));
        spnSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater(stratConHeader, "SPAUpgradeIntensity"));
        chkAutoConfigMunitions.addMouseListener(createTipPanelUpdater(stratConHeader, "AutoConfigMunitions"));

        // Content
        lblStratConPlayType = new CampaignOptionsLabel("StratConPlayType",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED));
        lblStratConPlayType.addMouseListener(createTipPanelUpdater(stratConHeader, "StratConPlayType"));
        comboStratConPlayType.addMouseListener(createTipPanelUpdater(stratConHeader, "StratConPlayType"));
        chkUseAdvancedScouting = new CampaignOptionsCheckBox("UseAdvancedScouting",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAdvancedScouting.addMouseListener(createTipPanelUpdater(stratConHeader, "UseAdvancedScouting"));
        chkNoSeedForces = new CampaignOptionsCheckBox("NoSeedForces",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkNoSeedForces.addMouseListener(createTipPanelUpdater(stratConHeader, "NoSeedForces"));
        chkUseGenericBattleValue = new CampaignOptionsCheckBox("UseGenericBattleValue");
        chkUseGenericBattleValue.addMouseListener(createTipPanelUpdater(stratConHeader, "UseGenericBattleValue"));
        chkUseVerboseBidding = new CampaignOptionsCheckBox("UseVerboseBidding");
        chkUseVerboseBidding.addMouseListener(createTipPanelUpdater(stratConHeader, "UseVerboseBidding"));
      stratConPageCreated = true;
      updateStratConControlsFromDraft();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("StratConTab", true);
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(stratConHeader, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblStratConPlayType, layout);
        layout.gridx++;
        panel.add(comboStratConPlayType, layout);
        layout.gridx++;
        panel.add(chkUseAdvancedScouting, layout);
        layout.gridx++;
        panel.add(chkNoSeedForces, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSkillLevel, layout);
        layout.gridx++;
        panel.add(comboSkillLevel, layout);
        layout.gridx++;
        panel.add(lblBoardScalingType, layout);
        layout.gridx++;
        panel.add(comboBoardScalingType, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseGenericBattleValue, layout);
        layout.gridx++;
        panel.add(chkUseVerboseBidding, layout);

        layout.gridwidth = 3;
        layout.gridx = 0;
        layout.gridy++;
        layout.anchor = GridBagConstraints.NORTHWEST;
        layout.fill = GridBagConstraints.BOTH;
        layout.gridheight = 2;
        panel.add(pnlScenarioGenerationPanel, layout);

        layout.gridwidth = 1;
        layout.gridheight = 1;
        layout.gridx = 3;
        panel.add(pnlCampaignOptions, layout);

        layout.gridy++;
        panel.add(pnlMorale, layout);

        layout.gridx = 4;
        layout.gridy -= 1;
        layout.gridheight = 2;
        panel.add(pnlAutoResolve, layout);

        // Create panel and return
        return createParentPanel(panel, "StratConTab");
    }

    /**
     * Initializes the Legacy Options section of the tab.
     */
    private void initializeLegacyTab() {}

    /**
     * Creates the UI panel for the Legacy AtB configuration.
     * <p>
     * This section configures opponent force generation, scenario generation probabilities, and customization of battle
     * intensities for "Against the Bot" campaigns.
     * </p>
     *
     * @return a {@link JPanel} containing all Legacy AtB settings.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public JPanel createLegacyTab() {
        // Header
        legacyHeader = new CampaignOptionsHeaderPanel("LegacyTab",
              getImageDirectory() + "logo_free_rasalhague_republic.png",
              true, true, 5);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(legacyHeader, layout);

        // Create panel and return
        return createParentPanel(panel, "LegacyTab");
    }

    /**
     * Applies the current values configured in the tab back to the provided {@link CampaignOptions}.
     * <p>
     * If no custom {@link CampaignOptions} is provided, it uses the default {@link CampaignOptions} associated with the
     * tab.
     * </p>
     *
     * @param presetCampaignOptions an optional custom {@link CampaignOptions} object to apply the values to; if
     *                              {@code null}, the default options are used.
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
     * A convenience method to load values from the default {@link CampaignOptions} instance.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the ruleset values from a {@link CampaignOptions} object into the UI components.
     * <p>
     * If no custom {@link CampaignOptions} is provided, it will fetch values from the default {@link CampaignOptions}
     * instance.
     * </p>
     *
     * @param presetCampaignOptions an optional custom {@link CampaignOptions} object to load values from; if
     *                              {@code null}, the default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

            draft = new RulesetsDraft(options);
            updateCreatedControlsFromDraft();
    }

      private void updateCreatedControlsFromDraft() {
            updateStratConControlsFromDraft();
      }

      private void updateStratConControlsFromDraft() {
            if (!stratConPageCreated || draft == null) {
                  return;
            }

            comboSkillLevel.setSelectedItem(draft.skillLevel);
            comboBoardScalingType.setSelectedItem(draft.boardScalingType);
            spnOpForLanceTypeMeks.setValue(draft.opForLanceTypeMeks);
            spnOpForLanceTypeMixed.setValue(draft.opForLanceTypeMixed);
            spnOpForLanceTypeVehicles.setValue(draft.opForLanceTypeVehicles);
            chkAutoGenerateOpForCallSigns.setSelected(draft.autoGenerateOpForCallSigns);
            comboMinimumCallsignSkillLevel.setSelectedItem(draft.minimumCallsignSkillLevel);
            chkUseDropShips.setSelected(draft.useDropShips);
            chkRegionalMekVariations.setSelected(draft.regionalMekVariations);
            chkAttachedPlayerCamouflage.setSelected(draft.attachedPlayerCamouflage);
            chkPlayerControlsAttachedUnits.setSelected(draft.playerControlsAttachedUnits);
            chkUseAdvancedBuildingGunEmplacements.setSelected(draft.useAdvancedBuildingGunEmplacements);
            spnSPAUpgradeIntensity.setValue(draft.spaUpgradeIntensity);
            chkAutoConfigMunitions.setSelected(draft.autoConfigMunitions);
            spnEnemyFacilityModifierDieSize.setValue(draft.enemyFacilityModifierDieSize);
            spnAlliedFacilityModifierDieSize.setValue(draft.alliedFacilityModifierDieSize);
            spnScenarioModMax.setValue(draft.scenarioModMax);
            spnScenarioModChance.setValue(draft.scenarioModChance);
            spnScenarioModBV.setValue(draft.scenarioModBV);
            chkUseWeatherConditions.setSelected(draft.useWeatherConditions);
            chkUseLightConditions.setSelected(draft.useLightConditions);
            chkUsePlanetaryConditions.setSelected(draft.usePlanetaryConditions);
            chkUseNoTornadoes.setSelected(draft.useNoTornadoes);
            spnFixedMapChance.setValue(draft.fixedMapChance);
            chkRestrictPartsByMission.setSelected(draft.restrictPartsByMission);
            spnMoraleVictory.setValue(draft.moraleVictoryEffect);
            spnMoraleDecisiveVictory.setValue(draft.moraleDecisiveVictoryEffect);
            spnMoraleDefeat.setValue(draft.moraleDefeatEffect);
            spnMoraleDecisiveDefeat.setValue(draft.moraleDecisiveDefeatEffect);
            comboAutoResolveMethod.setSelectedItem(draft.autoResolveMethod);
            minimapThemeSelector.setSelectedItem(draft.strategicViewTheme);
            chkAutoResolveVictoryChanceEnabled.setSelected(draft.autoResolveVictoryChanceEnabled);
            chkAutoResolveExperimentalPacarGuiEnabled.setSelected(draft.autoResolveExperimentalPacarGuiEnabled);
            spnAutoResolveNumberOfScenarios.setValue(draft.autoResolveNumberOfScenarios);
            comboStratConPlayType.setSelectedItem(draft.stratConPlayType);
            chkUseAdvancedScouting.setSelected(draft.useAdvancedScouting);
            chkNoSeedForces.setSelected(draft.noSeedForces);
            chkUseGenericBattleValue.setSelected(draft.useGenericBattleValue);
            chkUseVerboseBidding.setSelected(draft.useVerboseBidding);
      }

      private void updateDraftFromCreatedControls() {
            if (!stratConPageCreated || draft == null) {
                  return;
            }

            draft.skillLevel = comboSkillLevel.getSelectedItem();
            draft.boardScalingType = comboBoardScalingType.getSelectedItem();
            draft.opForLanceTypeMeks = (int) spnOpForLanceTypeMeks.getValue();
            draft.opForLanceTypeMixed = (int) spnOpForLanceTypeMixed.getValue();
            draft.opForLanceTypeVehicles = (int) spnOpForLanceTypeVehicles.getValue();
            draft.autoGenerateOpForCallSigns = chkAutoGenerateOpForCallSigns.isSelected();
            draft.minimumCallsignSkillLevel = comboMinimumCallsignSkillLevel.getSelectedItem();
            draft.useDropShips = chkUseDropShips.isSelected();
            draft.regionalMekVariations = chkRegionalMekVariations.isSelected();
            draft.attachedPlayerCamouflage = chkAttachedPlayerCamouflage.isSelected();
            draft.playerControlsAttachedUnits = chkPlayerControlsAttachedUnits.isSelected();
            draft.useAdvancedBuildingGunEmplacements = chkUseAdvancedBuildingGunEmplacements.isSelected();
            draft.spaUpgradeIntensity = (int) spnSPAUpgradeIntensity.getValue();
            draft.autoConfigMunitions = chkAutoConfigMunitions.isSelected();
            draft.enemyFacilityModifierDieSize = (int) spnEnemyFacilityModifierDieSize.getValue();
            draft.alliedFacilityModifierDieSize = (int) spnAlliedFacilityModifierDieSize.getValue();
            draft.scenarioModMax = (int) spnScenarioModMax.getValue();
            draft.scenarioModChance = (int) spnScenarioModChance.getValue();
            draft.scenarioModBV = (int) spnScenarioModBV.getValue();
            draft.useWeatherConditions = chkUseWeatherConditions.isSelected();
            draft.useLightConditions = chkUseLightConditions.isSelected();
            draft.usePlanetaryConditions = chkUsePlanetaryConditions.isSelected();
            draft.useNoTornadoes = chkUseNoTornadoes.isSelected();
            draft.fixedMapChance = (int) spnFixedMapChance.getValue();
            draft.restrictPartsByMission = chkRestrictPartsByMission.isSelected();
            draft.moraleVictoryEffect = (int) spnMoraleVictory.getValue();
            draft.moraleDecisiveVictoryEffect = (int) spnMoraleDecisiveVictory.getValue();
            draft.moraleDefeatEffect = (int) spnMoraleDefeat.getValue();
            draft.moraleDecisiveDefeatEffect = (int) spnMoraleDecisiveDefeat.getValue();
            draft.autoResolveMethod = comboAutoResolveMethod.getSelectedItem();
            draft.strategicViewTheme = minimapThemeSelector.getSelectedItem();
            draft.autoResolveVictoryChanceEnabled = chkAutoResolveVictoryChanceEnabled.isSelected();
            draft.autoResolveExperimentalPacarGuiEnabled = chkAutoResolveExperimentalPacarGuiEnabled.isSelected();
            draft.autoResolveNumberOfScenarios = (int) spnAutoResolveNumberOfScenarios.getValue();
            draft.stratConPlayType = comboStratConPlayType.getSelectedItem();
            draft.useAdvancedScouting = chkUseAdvancedScouting.isSelected();
            draft.noSeedForces = chkNoSeedForces.isSelected();
            draft.useGenericBattleValue = chkUseGenericBattleValue.isSelected();
            draft.useVerboseBidding = chkUseVerboseBidding.isSelected();
      }

      private static class RulesetsDraft {
            private SkillLevel skillLevel;
            private BoardScalingType boardScalingType;
            private int opForLanceTypeMeks;
            private int opForLanceTypeMixed;
            private int opForLanceTypeVehicles;
            private boolean autoGenerateOpForCallSigns;
            private SkillLevel minimumCallsignSkillLevel;
            private boolean useDropShips;
            private boolean regionalMekVariations;
            private boolean attachedPlayerCamouflage;
            private boolean playerControlsAttachedUnits;
            private boolean useAdvancedBuildingGunEmplacements;
            private int spaUpgradeIntensity;
            private boolean autoConfigMunitions;
            private int enemyFacilityModifierDieSize;
            private int alliedFacilityModifierDieSize;
            private int scenarioModMax;
            private int scenarioModChance;
            private int scenarioModBV;
            private boolean useWeatherConditions;
            private boolean useLightConditions;
            private boolean usePlanetaryConditions;
            private boolean useNoTornadoes;
            private int fixedMapChance;
            private boolean restrictPartsByMission;
            private int moraleVictoryEffect;
            private int moraleDecisiveVictoryEffect;
            private int moraleDefeatEffect;
            private int moraleDecisiveDefeatEffect;
            private AutoResolveMethod autoResolveMethod;
            private String strategicViewTheme;
            private boolean autoResolveVictoryChanceEnabled;
            private int autoResolveNumberOfScenarios;
            private boolean autoResolveExperimentalPacarGuiEnabled;
            private StratConPlayType stratConPlayType;
            private boolean useAdvancedScouting;
            private boolean noSeedForces;
            private boolean useGenericBattleValue;
            private boolean useVerboseBidding;

            private RulesetsDraft(CampaignOptions options) {
                  skillLevel = options.getSkillLevel();
                  boardScalingType = options.getBoardScalingType();
                  opForLanceTypeMeks = options.getOpForLanceTypeMeks();
                  opForLanceTypeMixed = options.getOpForLanceTypeMixed();
                  opForLanceTypeVehicles = options.getOpForLanceTypeVehicles();
                  autoGenerateOpForCallSigns = options.isAutoGenerateOpForCallSigns();
                  minimumCallsignSkillLevel = options.getMinimumCallsignSkillLevel();
                  useDropShips = options.isUseDropShips();
                  regionalMekVariations = options.isRegionalMekVariations();
                  attachedPlayerCamouflage = options.isAttachedPlayerCamouflage();
                  playerControlsAttachedUnits = options.isPlayerControlsAttachedUnits();
                  useAdvancedBuildingGunEmplacements = options.isUseAdvancedBuildingGunEmplacements();
                  spaUpgradeIntensity = options.getSpaUpgradeIntensity();
                  autoConfigMunitions = options.isAutoConfigMunitions();
                  enemyFacilityModifierDieSize = options.getEnemyFacilityModifierDieSize();
                  alliedFacilityModifierDieSize = options.getAlliedFacilityModifierDieSize();
                  scenarioModMax = options.getScenarioModMax();
                  scenarioModChance = options.getScenarioModChance();
                  scenarioModBV = options.getScenarioModBV();
                  useWeatherConditions = options.isUseWeatherConditions();
                  useLightConditions = options.isUseLightConditions();
                  usePlanetaryConditions = options.isUsePlanetaryConditions();
                  useNoTornadoes = options.isUseNoTornadoes();
                  fixedMapChance = options.getFixedMapChance();
                  restrictPartsByMission = options.isRestrictPartsByMission();
                  moraleVictoryEffect = options.getMoraleVictoryEffect();
                  moraleDecisiveVictoryEffect = options.getMoraleDecisiveVictoryEffect();
                  moraleDefeatEffect = options.getMoraleDefeatEffect();
                  moraleDecisiveDefeatEffect = options.getMoraleDecisiveDefeatEffect();
                  autoResolveMethod = options.getAutoResolveMethod();
                  strategicViewTheme = options.getStrategicViewTheme().getName();
                  autoResolveVictoryChanceEnabled = options.isAutoResolveVictoryChanceEnabled();
                  autoResolveNumberOfScenarios = options.getAutoResolveNumberOfScenarios();
                  autoResolveExperimentalPacarGuiEnabled = options.isAutoResolveExperimentalPacarGuiEnabled();
                  stratConPlayType = options.getStratConPlayType();
                  useAdvancedScouting = options.isUseAdvancedScouting();
                  noSeedForces = options.isNoSeedForces();
                  useGenericBattleValue = options.isUseGenericBattleValue();
                  useVerboseBidding = options.isUseVerboseBidding();
            }

            private void applyTo(CampaignOptions options) {
                  options.setSkillLevel(skillLevel);
                  options.setBoardScalingType(boardScalingType);
                  options.setOpForLanceTypeMeks(opForLanceTypeMeks);
                  options.setOpForLanceTypeMixed(opForLanceTypeMixed);
                  options.setOpForLanceTypeVehicles(opForLanceTypeVehicles);
                  options.setAutoGenerateOpForCallSigns(autoGenerateOpForCallSigns);
                  options.setMinimumCallsignSkillLevel(minimumCallsignSkillLevel);
                  options.setUseDropShips(useDropShips);
                  options.setRegionalMekVariations(regionalMekVariations);
                  options.setAttachedPlayerCamouflage(attachedPlayerCamouflage);
                  options.setPlayerControlsAttachedUnits(playerControlsAttachedUnits);
                  options.setUseAdvancedBuildingGunEmplacements(useAdvancedBuildingGunEmplacements);
                  options.setSpaUpgradeIntensity(spaUpgradeIntensity);
                  options.setAutoConfigMunitions(autoConfigMunitions);
                  options.setEnemyFacilityModifierDieSize(enemyFacilityModifierDieSize);
                  options.setAlliedFacilityModifierDieSize(alliedFacilityModifierDieSize);
                  options.setScenarioModMax(scenarioModMax);
                  options.setScenarioModChance(scenarioModChance);
                  options.setScenarioModBV(scenarioModBV);
                  options.setUseWeatherConditions(useWeatherConditions);
                  options.setUseLightConditions(useLightConditions);
                  options.setUsePlanetaryConditions(usePlanetaryConditions);
                  options.setUseNoTornadoes(useNoTornadoes);
                  options.setFixedMapChance(fixedMapChance);
                  options.setRestrictPartsByMission(restrictPartsByMission);
                  options.setMoraleVictoryEffect(moraleVictoryEffect);
                  options.setMoraleDecisiveVictoryEffect(moraleDecisiveVictoryEffect);
                  options.setMoraleDefeatEffect(moraleDefeatEffect);
                  options.setMoraleDecisiveDefeatEffect(moraleDecisiveDefeatEffect);
                  options.setAutoResolveMethod(autoResolveMethod);
                  options.setStrategicViewTheme(strategicViewTheme);
                  options.setAutoResolveVictoryChanceEnabled(autoResolveVictoryChanceEnabled);
                  options.setAutoResolveNumberOfScenarios(autoResolveNumberOfScenarios);
                  options.setAutoResolveExperimentalPacarGuiEnabled(autoResolveExperimentalPacarGuiEnabled);
                  options.setStratConPlayType(stratConPlayType);
                  options.setUseAdvancedScouting(useAdvancedScouting);
                  options.setNoSeedForces(noSeedForces);
                  options.setUseGenericBattleValue(useGenericBattleValue);
                  options.setUseVerboseBidding(useVerboseBidding);
            }
      }
}
