/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import java.awt.GridBagConstraints;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.models.FileNameComboBoxModel;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.Skills;
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

    private JPanel pnlCallSigns;
    private JCheckBox chkAutoGenerateOpForCallSigns;
    private JLabel lblMinimumCallsignSkillLevel;
    private MMComboBox<SkillLevel> comboMinimumCallsignSkillLevel;

    private JCheckBox chkUseDropShips;

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

    private JCheckBox chkUseStratCon;
    private JCheckBox chkUseStratConMaplessMode;
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

        // CallSigns
        pnlCallSigns = createCallSignsPanel();

        // OpFor Generation
        pnlUnitRatioPanel = createUniversalUnitRatioPanel();

        chkUseDropShips = new CampaignOptionsCheckBox("UseDropShips");
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
        lblAutoResolveMethod = new CampaignOptionsLabel("AutoResolveMethod");
        lblAutoResolveNumberOfScenarios = new CampaignOptionsLabel("AutoResolveNumberOfScenarios");
        spnAutoResolveNumberOfScenarios = new CampaignOptionsSpinner("AutoResolveNumberOfScenarios",
              250, 10, 1000, 10);
        chkAutoResolveVictoryChanceEnabled = new CampaignOptionsCheckBox("AutoResolveVictoryChanceEnabled");
        lblMinimapTheme = new CampaignOptionsLabel("MinimapTheme");
        chkAutoResolveExperimentalPacarGuiEnabled = new CampaignOptionsCheckBox("AutoResolveExperimentalPacarGuiEnabled");

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("AutoResolvePanel", true,
              "AutoResolvePanel");
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
        lblMoraleDecisiveVictory = new CampaignOptionsLabel("MoraleDecisiveVictory");
        spnMoraleDecisiveVictory = new CampaignOptionsSpinner("MoraleDecisiveVictory",
              4, 1, 10, 1);

        lblMoraleVictory = new CampaignOptionsLabel("MoraleVictory");
        spnMoraleVictory = new CampaignOptionsSpinner("MoraleVictory",
              2, 1, 10, 1);

        lblMoraleDefeat = new CampaignOptionsLabel("MoraleDefeat");
        spnMoraleDefeat = new CampaignOptionsSpinner("MoraleDefeat",
              -3, -10, -1, 1);

        lblMoraleDecisiveDefeat = new CampaignOptionsLabel("MoraleDecisiveDefeat");
        spnMoraleDecisiveDefeat = new CampaignOptionsSpinner("MoraleDecisiveDefeat",
              -5, -10, -1, 1);

        // Layout the panel
        final JPanel panel = new CampaignOptionsStandardPanel("UniversalMoralePanel", true,
              "UniversalMoralePanel");
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
        chkUseStratCon = new JCheckBox();
        chkUseStratConMaplessMode = new JCheckBox();
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
        lblFixedMapChance.addMouseListener(createTipPanelUpdater(stratConHeader, "FixedMapChance"));
        spnFixedMapChance.addMouseListener(createTipPanelUpdater(stratConHeader, "FixedMapChance"));
        lblScenarioModMax.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModMax"));
        spnScenarioModMax.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModMax"));
        lblScenarioModChance.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModChance"));
        spnScenarioModChance.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModChance"));
        lblScenarioModBV.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModBV"));
        spnScenarioModBV.addMouseListener(createTipPanelUpdater(stratConHeader, "ScenarioModBV"));
        lblSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader, "SkillLevel"));
        comboSkillLevel.addMouseListener(createTipPanelUpdater(stratConHeader, "SkillLevel"));
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
        lblSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater(stratConHeader, "SPAUpgradeIntensity"));
        spnSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater(stratConHeader, "SPAUpgradeIntensity"));
        chkAutoConfigMunitions.addMouseListener(createTipPanelUpdater(stratConHeader, "AutoConfigMunitions"));

        // Content
        chkUseStratCon = new CampaignOptionsCheckBox("UseStratCon");
        chkUseStratCon.addMouseListener(createTipPanelUpdater(stratConHeader, "UseStratCon"));
        chkUseStratConMaplessMode = new CampaignOptionsCheckBox("UseStratConMaplessMode");
        chkUseStratConMaplessMode.addMouseListener(createTipPanelUpdater(stratConHeader, "UseStratConMaplessMode"));
        chkUseAdvancedScouting = new CampaignOptionsCheckBox("UseAdvancedScouting");
        chkUseAdvancedScouting.addMouseListener(createTipPanelUpdater(stratConHeader, "UseAdvancedScouting"));
        chkNoSeedForces = new CampaignOptionsCheckBox("NoSeedForces");
        chkNoSeedForces.addMouseListener(createTipPanelUpdater(stratConHeader, "NoSeedForces"));
        chkUseGenericBattleValue = new CampaignOptionsCheckBox("UseGenericBattleValue");
        chkUseGenericBattleValue.addMouseListener(createTipPanelUpdater(stratConHeader, "UseGenericBattleValue"));
        chkUseVerboseBidding = new CampaignOptionsCheckBox("UseVerboseBidding");
        chkUseVerboseBidding.addMouseListener(createTipPanelUpdater(stratConHeader, "UseVerboseBidding"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("StratConTab", true);
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(stratConHeader, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseStratCon, layout);
        layout.gridx++;
        panel.add(chkUseAdvancedScouting, layout);
        layout.gridx++;
        panel.add(chkNoSeedForces, layout);
        layout.gridx++;
        panel.add(chkUseStratConMaplessMode, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSkillLevel, layout);
        layout.gridx++;
        panel.add(comboSkillLevel, layout);
        layout.gridx++;
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

        // Universal
        options.setSkillLevel(comboSkillLevel.getSelectedItem());
        options.setOpForLanceTypeMeks((int) spnOpForLanceTypeMeks.getValue());
        options.setOpForLanceTypeMixed((int) spnOpForLanceTypeMixed.getValue());
        options.setOpForLanceTypeVehicles((int) spnOpForLanceTypeVehicles.getValue());
        options.setAutoGenerateOpForCallSigns(chkAutoGenerateOpForCallSigns.isSelected());
        options.setMinimumCallsignSkillLevel(comboMinimumCallsignSkillLevel.getSelectedItem());
        options.setUseDropShips(chkUseDropShips.isSelected());
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
        options.setMoraleVictoryEffect((int) spnMoraleVictory.getValue());
        options.setMoraleDecisiveVictoryEffect((int) spnMoraleDecisiveVictory.getValue());
        options.setMoraleDefeatEffect((int) spnMoraleDefeat.getValue());
        options.setMoraleDecisiveDefeatEffect((int) spnMoraleDecisiveDefeat.getValue());
        options.setAutoResolveMethod(comboAutoResolveMethod.getSelectedItem());
        options.setStrategicViewTheme(minimapThemeSelector.getSelectedItem());
        options.setAutoResolveVictoryChanceEnabled(chkAutoResolveVictoryChanceEnabled.isSelected());
        options.setAutoResolveNumberOfScenarios((int) spnAutoResolveNumberOfScenarios.getValue());
        options.setAutoResolveExperimentalPacarGuiEnabled(chkAutoResolveExperimentalPacarGuiEnabled.isSelected());

        // StratCon
        options.setUseStratCon(chkUseStratCon.isSelected());
        options.setUseStratConMaplessMode(chkUseStratConMaplessMode.isSelected());
        options.setUseAdvancedScouting(chkUseAdvancedScouting.isSelected());
        options.setNoSeedForces(chkNoSeedForces.isSelected());
        options.setUseGenericBattleValue(chkUseGenericBattleValue.isSelected());
        options.setUseVerboseBidding(chkUseVerboseBidding.isSelected());
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

        // Universal
        comboSkillLevel.setSelectedItem(options.getSkillLevel());
        spnOpForLanceTypeMeks.setValue(options.getOpForLanceTypeMeks());
        spnOpForLanceTypeMixed.setValue(options.getOpForLanceTypeMixed());
        spnOpForLanceTypeVehicles.setValue(options.getOpForLanceTypeVehicles());
        chkAutoGenerateOpForCallSigns.setSelected(options.isAutoGenerateOpForCallSigns());
        comboMinimumCallsignSkillLevel.setSelectedItem(options.getMinimumCallsignSkillLevel());
        chkUseDropShips.setSelected(options.isUseDropShips());
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
        spnMoraleVictory.setValue(options.getMoraleVictoryEffect());
        spnMoraleDecisiveVictory.setValue(options.getMoraleDecisiveVictoryEffect());
        spnMoraleDefeat.setValue(options.getMoraleDefeatEffect());
        spnMoraleDecisiveDefeat.setValue(options.getMoraleDecisiveDefeatEffect());
        comboAutoResolveMethod.setSelectedItem(options.getAutoResolveMethod());
        minimapThemeSelector.setSelectedItem(options.getStrategicViewTheme().getName());
        chkAutoResolveVictoryChanceEnabled.setSelected(options.isAutoResolveVictoryChanceEnabled());
        chkAutoResolveExperimentalPacarGuiEnabled.setSelected(options.isAutoResolveExperimentalPacarGuiEnabled());
        spnAutoResolveNumberOfScenarios.setValue(options.getAutoResolveNumberOfScenarios());

        // StratCon
        chkUseStratCon.setSelected(options.isUseStratCon());
        chkUseStratConMaplessMode.setSelected(options.isUseStratConMaplessMode());
        chkUseAdvancedScouting.setSelected(options.isUseAdvancedScouting());
        chkNoSeedForces.setSelected(options.isNoSeedForces());
        chkUseGenericBattleValue.setSelected(options.isUseGenericBattleValue());
        chkUseVerboseBidding.setSelected(options.isUseVerboseBidding());
    }
}
