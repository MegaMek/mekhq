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

import java.awt.Component;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.models.FileNameComboBoxModel;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.BoardScalingType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.stratCon.StratConPlayType;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code StratConPage} class builds and manages the StratCon leaf page of the Campaign Options dialog. It owns the
 * widgets for the StratCon (Strategic Context) ruleset - the universal options shared by StratCon campaigns (skill
 * levels, opponent force generation, scenario modifiers, map conditions, morale, and auto-resolve settings) together
 * with the StratCon-specific options - and synchronises them with a shared {@link RulesetsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RulesetsPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code RulesetsPages}, while this class is responsible only for constructing the StratCon panel and
 * copying values to and from the model. The page is built lazily; until {@link #createPanel(RulesetsOptionsModel)} is
 * called, {@link #readFromModel(RulesetsOptionsModel)} and {@link #writeToModel(RulesetsOptionsModel)} are no-ops.</p>
 */
class StratConPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;
    private static final int FORM_LABEL_CONTROL_GAP = 12;
    private static final int GRID_CONTROL_COLUMN_WIDTH = 100;
    // First pair column = label column + the form's label/control gap, so a
    // two-column grid's column 3 lines up
    // with the control column of the 2-column form sections on the same page. The
    // following pair is narrower so the
    // whole grid still stays within the shared page-width floor (312 + 303 -> 640px
    // section; column 3 at x=312).
    private static final int GRID_FIRST_PAIR_COLUMN_WIDTH = LABEL_COLUMN_WIDTH + FORM_LABEL_CONTROL_GAP;
    private static final int GRID_FOLLOWING_PAIR_COLUMN_WIDTH = 303;

    // start Universal Options
    private JLabel lblSkillLevel;
    private MMComboBox<SkillLevel> comboSkillLevel;
    private JLabel lblBoardScalingType;
    private MMComboBox<BoardScalingType> comboBoardScalingType;
    private JLabel lblOpForLanceTypeMeks;
    private JSpinner spnOpForLanceTypeMeks;
    private JLabel lblOpForLanceTypeMixed;
    private JSpinner spnOpForLanceTypeMixed;
    private JLabel lblOpForLanceTypeVehicle;
    private JSpinner spnOpForLanceTypeVehicles;

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
    private JLabel lblReinforcementBaseTargetNumber;
    private JSpinner spnReinforcementBaseTargetNumber;
    private JCheckBox chkAutoConfigMunitions;

    private JCheckBox chkClansObeyBiddingRules;
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

    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    private JCheckBox chkUseNoTornadoes;
    private JLabel lblFixedMapChance;
    private JSpinner spnFixedMapChance;

    private JLabel lblMoraleVictory;
    private JSpinner spnMoraleVictory;
    private JLabel lblMoraleDecisiveVictory;
    private JSpinner spnMoraleDecisiveVictory;
    private JLabel lblMoraleDefeat;
    private JSpinner spnMoraleDefeat;
    private JLabel lblMoraleDecisiveDefeat;
    private JSpinner spnMoraleDecisiveDefeat;

    private JCheckBox chkRestrictPartsByMission;

    private JLabel lblAutoResolveMethod;
    private MMComboBox<AutoResolveMethod> comboAutoResolveMethod;
    private MMComboBox<String> minimapThemeSelector;
    private JCheckBox chkAutoResolveVictoryChanceEnabled;
    private JLabel lblMinimapTheme;
    private JCheckBox chkAutoResolveExperimentalPacarGuiEnabled;
    private JLabel lblAutoResolveNumberOfScenarios;
    private JSpinner spnAutoResolveNumberOfScenarios;
    // end Universal Options

    private JLabel lblStratConPlayType;
    private MMComboBox<StratConPlayType> comboStratConPlayType;
    private JCheckBox chkUseAdvancedScouting;
    private JCheckBox chkNoSeedForces;
    private JCheckBox chkUseGenericBattleValue;
    private JCheckBox chkUseVerboseBidding;
    // end StratCon

    private boolean created;

    /**
     * Builds the StratCon page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared rulesets options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the StratCon Page
     */
    @Nonnull JPanel createPanel(@Nullable RulesetsOptionsModel model) {
        // Combos (previously built during initialization)
        comboSkillLevel = new MMComboBox<>("comboSkillLevel", getSkillLevelOptions());
        comboBoardScalingType = new MMComboBox<>("comboBoardScalingType", BoardScalingType.values());
        final DefaultComboBoxModel<AutoResolveMethod> autoResolveTypeModel = new DefaultComboBoxModel<>(
                AutoResolveMethod.values());
        comboAutoResolveMethod = new MMComboBox<>("comboAutoResolveMethod", autoResolveTypeModel);
        minimapThemeSelector = new MMComboBox<>("minimapThemeSelector",
                new FileNameComboBoxModel(GUIPreferences.getInstance().getMinimapThemes()));
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

        // Header
        // start StratCon
        String imageAddress = getImageDirectory() + "logo_clan_wolf.png";
        CampaignOptionsHeaderPanel stratConHeader = new CampaignOptionsHeaderPanel("StratConPage", imageAddress);

        // Universal Content
        substantializeUniversalOptions();

        // Right now the universal content all lives in the StratCon page, but that might
        // not always be the case if
        // we ever introduce a new Digital GM. So, as this content is initialized before
        // the stratConHeader, we need
        // to wait until now to add the mouse listeners. It's awkward, but it works.
        lblAutoResolveMethod.addMouseListener(createTipPanelUpdater("AutoResolveMethod"));
        comboAutoResolveMethod.addMouseListener(createTipPanelUpdater("AutoResolveMethod"));
        lblMinimapTheme.addMouseListener(createTipPanelUpdater("MinimapTheme"));
        minimapThemeSelector.addMouseListener(createTipPanelUpdater("MinimapTheme"));
        lblAutoResolveNumberOfScenarios.addMouseListener(createTipPanelUpdater("AutoResolveNumberOfScenarios"));
        spnAutoResolveNumberOfScenarios.addMouseListener(createTipPanelUpdater("AutoResolveNumberOfScenarios"));
        chkAutoResolveVictoryChanceEnabled.addMouseListener(createTipPanelUpdater("AutoResolveVictoryChanceEnabled"));
        chkAutoResolveExperimentalPacarGuiEnabled.addMouseListener(createTipPanelUpdater("AutoResolveExperimentalPacarGuiEnabled"));
        lblMoraleVictory.addMouseListener(createTipPanelUpdater("MoraleVictory"));
        spnMoraleVictory.addMouseListener(createTipPanelUpdater("MoraleVictory"));
        lblMoraleDecisiveVictory.addMouseListener(createTipPanelUpdater("MoraleDecisiveVictory"));
        spnMoraleDecisiveVictory.addMouseListener(createTipPanelUpdater("MoraleDecisiveVictory"));
        lblMoraleDefeat.addMouseListener(createTipPanelUpdater("MoraleDefeat"));
        spnMoraleDefeat.addMouseListener(createTipPanelUpdater("MoraleDefeat"));
        lblMoraleDecisiveDefeat.addMouseListener(createTipPanelUpdater("MoraleDecisiveDefeat"));
        spnMoraleDecisiveDefeat.addMouseListener(createTipPanelUpdater("MoraleDecisiveDefeat"));
        chkRestrictPartsByMission.addMouseListener(createTipPanelUpdater("RestrictPartsByMission"));
        chkUseWeatherConditions.addMouseListener(createTipPanelUpdater("UseWeatherConditions"));
        chkUseLightConditions.addMouseListener(createTipPanelUpdater("UseLightConditions"));
        chkUsePlanetaryConditions.addMouseListener(createTipPanelUpdater("UsePlanetaryConditions"));
        chkUseNoTornadoes.addMouseListener(createTipPanelUpdater("UseNoTornadoes"));
        lblFixedMapChance.addMouseListener(createTipPanelUpdater("FixedMapChance"));
        spnFixedMapChance.addMouseListener(createTipPanelUpdater("FixedMapChance"));
        chkClansObeyBiddingRules.addMouseListener(createTipPanelUpdater("ClansObeyBiddingRules"));
        lblEnemyFacilityModifierDieSize.addMouseListener(createTipPanelUpdater("EnemyFacilityModifierDieSize"));
        spnEnemyFacilityModifierDieSize.addMouseListener(createTipPanelUpdater("EnemyFacilityModifierDieSize"));
        lblAlliedFacilityModifierDieSize.addMouseListener(createTipPanelUpdater("AlliedFacilityModifierDieSize"));
        spnAlliedFacilityModifierDieSize.addMouseListener(createTipPanelUpdater("AlliedFacilityModifierDieSize"));
        lblScenarioModMax.addMouseListener(createTipPanelUpdater("ScenarioModMax"));
        spnScenarioModMax.addMouseListener(createTipPanelUpdater("ScenarioModMax"));
        lblScenarioModChance.addMouseListener(createTipPanelUpdater("ScenarioModChance"));
        spnScenarioModChance.addMouseListener(createTipPanelUpdater("ScenarioModChance"));
        lblScenarioModBV.addMouseListener(createTipPanelUpdater("ScenarioModBV"));
        spnScenarioModBV.addMouseListener(createTipPanelUpdater("ScenarioModBV"));
        lblSkillLevel.addMouseListener(createTipPanelUpdater("SkillLevel"));
        comboSkillLevel.addMouseListener(createTipPanelUpdater("SkillLevel"));
        lblBoardScalingType.addMouseListener(createTipPanelUpdater("BoardScalingType"));
        comboBoardScalingType.addMouseListener(createTipPanelUpdater("BoardScalingType"));
        chkAutoGenerateOpForCallSigns.addMouseListener(createTipPanelUpdater("AutoGenerateOpForCallSigns"));
        lblMinimumCallsignSkillLevel.addMouseListener(createTipPanelUpdater("MinimumCallsignSkillLevel"));
        comboMinimumCallsignSkillLevel.addMouseListener(createTipPanelUpdater("MinimumCallsignSkillLevel"));
        chkUseDropShips.addMouseListener(createTipPanelUpdater("UseDropShips"));
        chkRegionalMekVariations.addMouseListener(createTipPanelUpdater("RegionalMekVariations"));
        chkAttachedPlayerCamouflage
                .addMouseListener(createTipPanelUpdater("AttachedPlayerCamouflage"));
        chkPlayerControlsAttachedUnits.addMouseListener(createTipPanelUpdater("PlayerControlsAttachedUnits"));
        chkUseAdvancedBuildingGunEmplacements.addMouseListener(createTipPanelUpdater("UseAdvancedBuildingGunEmplacements"));
        lblSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater("SPAUpgradeIntensity"));
        spnSPAUpgradeIntensity.addMouseListener(createTipPanelUpdater("SPAUpgradeIntensity"));
        lblReinforcementBaseTargetNumber.addMouseListener(createTipPanelUpdater("ReinforcementBaseTargetNumber"));
        spnReinforcementBaseTargetNumber.addMouseListener(createTipPanelUpdater("ReinforcementBaseTargetNumber"));
        chkAutoConfigMunitions.addMouseListener(createTipPanelUpdater("AutoConfigMunitions"));

        // Content
        lblStratConPlayType = new CampaignOptionsLabel("StratConPlayType",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED));
        lblStratConPlayType.addMouseListener(createTipPanelUpdater("StratConPlayType"));
        comboStratConPlayType.addMouseListener(createTipPanelUpdater("StratConPlayType"));
        chkUseAdvancedScouting = new CampaignOptionsCheckBox("UseAdvancedScouting",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAdvancedScouting.addMouseListener(createTipPanelUpdater("UseAdvancedScouting"));
        chkNoSeedForces = new CampaignOptionsCheckBox("NoSeedForces",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkNoSeedForces.addMouseListener(createTipPanelUpdater("NoSeedForces"));
        chkUseGenericBattleValue = new CampaignOptionsCheckBox("UseGenericBattleValue");
        chkUseGenericBattleValue.addMouseListener(createTipPanelUpdater("UseGenericBattleValue"));
        chkUseVerboseBidding = new CampaignOptionsCheckBox("UseVerboseBidding");
        chkUseVerboseBidding.addMouseListener(createTipPanelUpdater("UseVerboseBidding"));

        JPanel generalOptionsPanel = createStratConGeneralOptionsPanel();
        JPanel scenarioGenerationPanel = createStratConScenarioGenerationPanel();
        JPanel scenarioModifiersPanel = createStratConScenarioModifiersPanel();
        JPanel scenarioConditionsPanel = createStratConScenarioConditionsPanel();
        JPanel moralePanel = createStratConMoralePanel();
        JPanel autoResolvePanel = createStratConAutoResolvePanel();

        // Layout the Panel
        JPanel panel = CampaignOptionsPagePanel.builder("StratConPage", "StratConPage", imageAddress)
                .header(stratConHeader)
                .quote("stratConPage")
                .section("lblStratConPage.text",
                        "lblStratConPage.summary",
                        generalOptionsPanel)
                .section("lblUniversalScenarioGenerationPanel.text",
                        "lblUniversalScenarioGenerationPanel.summary",
                        scenarioGenerationPanel)
                .section("lblUniversalModifiersPanel.text",
                        "lblUniversalModifiersPanel.summary",
                        scenarioModifiersPanel)
                .section("lblStratConScenarioConditionsPanel.text",
                        "lblStratConScenarioConditionsPanel.summary",
                        scenarioConditionsPanel)
                .section("lblUniversalMoralePanel.text",
                        "lblUniversalMoralePanel.summary",
                        moralePanel,
                        getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblAutoResolvePanel.text",
                        "lblAutoResolvePanel.summary",
                        autoResolvePanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA,
                                CampaignOptionFlag.CUSTOM_SYSTEM,
                                CampaignOptionFlag.DOCUMENTED))
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Configures and initializes universal options components for use across pages.
     * <p>
     * This method sets up and organizes the various UI elements for universal
     * options, such as skill levels, scenario
     * generation, map generation, and more. These initialized components are then
     * used in other methods to build the
     * complete universal options UI.
     * </p>
     */
    private void substantializeUniversalOptions() {
        // General
        lblSkillLevel = new CampaignOptionsLabel("SkillLevel");
        lblBoardScalingType = new CampaignOptionsLabel("BoardScalingType",
                getMetadata(MILESTONE_BEFORE_METADATA));

        // CallSigns
        chkAutoGenerateOpForCallSigns = new CampaignOptionsCheckBox("AutoGenerateOpForCallSigns");
        lblMinimumCallsignSkillLevel = new CampaignOptionsLabel("MinimumCallsignSkillLevel");
        comboMinimumCallsignSkillLevel = new MMComboBox<>("comboMinimumCallsignSkillLevel", getSkillLevelOptions());

        // OpFor Generation
        lblOpForLanceTypeMeks = new CampaignOptionsLabel("OpForLanceTypeMeks");
        spnOpForLanceTypeMeks = new CampaignOptionsSpinner("OpForLanceTypeMeks", 0, 0, 10, 1);
        lblOpForLanceTypeMixed = new CampaignOptionsLabel("OpForLanceTypeMixed");
        spnOpForLanceTypeMixed = new CampaignOptionsSpinner("OpForLanceTypeMixed", 0, 0, 10, 1);
        lblOpForLanceTypeVehicle = new CampaignOptionsLabel("OpForLanceTypeVehicle");
        spnOpForLanceTypeVehicles = new CampaignOptionsSpinner("OpForLanceTypeVehicle", 0, 0, 10, 1);

        chkUseDropShips = new CampaignOptionsCheckBox("UseDropShips");
        chkRegionalMekVariations = new CampaignOptionsCheckBox("RegionalMekVariations");
        chkAttachedPlayerCamouflage = new CampaignOptionsCheckBox("AttachedPlayerCamouflage");
        chkPlayerControlsAttachedUnits = new CampaignOptionsCheckBox("PlayerControlsAttachedUnits");
        chkUseAdvancedBuildingGunEmplacements = new CampaignOptionsCheckBox("UseAdvancedBuildingGunEmplacements",
                getMetadata(new Version(0, 50, 12)));
        lblSPAUpgradeIntensity = new CampaignOptionsLabel("SPAUpgradeIntensity");
        spnSPAUpgradeIntensity = new CampaignOptionsSpinner("SPAUpgradeIntensity", 0, -1, 3, 1);
        lblReinforcementBaseTargetNumber = new CampaignOptionsLabel("ReinforcementBaseTargetNumber",
                getMetadata(new Version(0, 51, 0)));
        spnReinforcementBaseTargetNumber = new CampaignOptionsSpinner("ReinforcementBaseTargetNumber",
                7, -10, 10, 1);
        chkAutoConfigMunitions = new CampaignOptionsCheckBox("AutoConfigMunitions",
                getMetadata(LEGACY_RULE_BEFORE_METADATA,
                        CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.DOCUMENTED));

        // Scenario Modifiers
        chkClansObeyBiddingRules = new CampaignOptionsCheckBox("ClansObeyBiddingRules");
        lblEnemyFacilityModifierDieSize = new CampaignOptionsLabel("EnemyFacilityModifierDieSize");
        spnEnemyFacilityModifierDieSize = new CampaignOptionsSpinner("EnemyFacilityModifierDieSize", 2, 0, 10, 1);
        lblAlliedFacilityModifierDieSize = new CampaignOptionsLabel("AlliedFacilityModifierDieSize");
        spnAlliedFacilityModifierDieSize = new CampaignOptionsSpinner("AlliedFacilityModifierDieSize", 2, 0, 10, 1);
        lblScenarioModMax = new CampaignOptionsLabel("ScenarioModMax", getMetadata(new Version(0, 51, 0)));
        spnScenarioModMax = new CampaignOptionsSpinner("ScenarioModMax", 3, 0, 10, 1);
        lblScenarioModChance = new CampaignOptionsLabel("ScenarioModChance", getMetadata(new Version(0, 51, 0)));
        spnScenarioModChance = new CampaignOptionsSpinner("ScenarioModChance", 25, 5, 100, 5);
        lblScenarioModBV = new CampaignOptionsLabel("ScenarioModBV");
        spnScenarioModBV = new CampaignOptionsSpinner("ScenarioModBV", 50, 5, 100, 5);

        // Map Generation
        chkUseWeatherConditions = new CampaignOptionsCheckBox("UseWeatherConditions");
        chkUseLightConditions = new CampaignOptionsCheckBox("UseLightConditions");
        chkUsePlanetaryConditions = new CampaignOptionsCheckBox("UsePlanetaryConditions");
        chkUseNoTornadoes = new CampaignOptionsCheckBox("UseNoTornadoes",
                getMetadata(MILESTONE_BEFORE_METADATA));
        lblFixedMapChance = new CampaignOptionsLabel("FixedMapChance");
        spnFixedMapChance = new CampaignOptionsSpinner("FixedMapChance", 0, 0, 100, 1);

        // Parts
        chkRestrictPartsByMission = new CampaignOptionsCheckBox("RestrictPartsByMission");

        // Morale
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

        // Auto Resolve
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
        chkAutoResolveExperimentalPacarGuiEnabled = new CampaignOptionsCheckBox(
                "AutoResolveExperimentalPacarGuiEnabled");
    }

    /**
     * Retrieves the available skill levels as a {@link DefaultComboBoxModel}.
     * <p>
     * Returns the predefined {@link SkillLevel} values, excluding
     * {@link SkillLevel#NONE}. Used for populating the
     * skill level selector in the universal options UI.
     * </p>
     *
     * @return a {@link DefaultComboBoxModel} containing available
     *         {@link SkillLevel} options
     */
    private static DefaultComboBoxModel<SkillLevel> getSkillLevelOptions() {
        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>(
                Skills.SKILL_LEVELS);

        skillLevelModel.removeElement(SkillLevel.NONE);

        return skillLevelModel;
    }

    private @Nonnull JPanel createStratConGeneralOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("StratConGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblStratConPlayType, comboStratConPlayType);
        panel.addRow(lblSkillLevel, comboSkillLevel);
        panel.addRow(lblBoardScalingType, comboBoardScalingType);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseAdvancedScouting,
                chkNoSeedForces,
                chkUseGenericBattleValue,
                chkUseVerboseBidding);

        return panel;
    }

    private @Nonnull JPanel createStratConScenarioGenerationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("StratConScenarioGenerationPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblOpForLanceTypeMeks, spnOpForLanceTypeMeks);
        panel.addRow(lblOpForLanceTypeMixed, spnOpForLanceTypeMixed);
        panel.addRow(lblOpForLanceTypeVehicle, spnOpForLanceTypeVehicles);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseDropShips,
                chkRegionalMekVariations,
                chkAttachedPlayerCamouflage,
                chkPlayerControlsAttachedUnits,
                chkUseAdvancedBuildingGunEmplacements,
                chkAutoConfigMunitions,
                chkAutoGenerateOpForCallSigns);
        panel.addRow(lblSPAUpgradeIntensity, spnSPAUpgradeIntensity);
        panel.addRow(lblReinforcementBaseTargetNumber, spnReinforcementBaseTargetNumber);
        panel.addRow(lblMinimumCallsignSkillLevel, comboMinimumCallsignSkillLevel);

        return panel;
    }

    private @Nonnull JPanel createStratConScenarioModifiersPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("StratConScenarioModifiersPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblEnemyFacilityModifierDieSize, spnEnemyFacilityModifierDieSize);
        panel.addRow(lblAlliedFacilityModifierDieSize, spnAlliedFacilityModifierDieSize);
        panel.addRow(lblScenarioModMax, spnScenarioModMax);
        panel.addRow(lblScenarioModChance, spnScenarioModChance);
        panel.addRow(lblScenarioModBV, spnScenarioModBV);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS, chkClansObeyBiddingRules);

        return panel;
    }

    private @Nonnull JPanel createStratConScenarioConditionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("StratConScenarioConditionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkRestrictPartsByMission,
                chkUseWeatherConditions,
                chkUseLightConditions,
                chkUsePlanetaryConditions,
                chkUseNoTornadoes);
        panel.addRow(lblFixedMapChance, spnFixedMapChance);

        return panel;
    }

    private @Nonnull JPanel createStratConMoralePanel() {
        // A generic 4-column (label/control, label/control) aligned grid: the two
        // victory modifiers sit on the top
        // row and the two defeat modifiers on the bottom row. Reuses the shared
        // paired-grid widths so the grid's third
        // column lines up with the control column of the 2-column sections elsewhere on
        // this page.
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel("StratConMoralePanel",
                GRID_FIRST_PAIR_COLUMN_WIDTH,
                GRID_FOLLOWING_PAIR_COLUMN_WIDTH,
                GRID_CONTROL_COLUMN_WIDTH,
                2);
        JComponent[] labels = { lblMoraleDecisiveVictory, lblMoraleVictory,
                lblMoraleDefeat, lblMoraleDecisiveDefeat };
        JComponent[] controls = { spnMoraleDecisiveVictory, spnMoraleVictory,
                spnMoraleDefeat, spnMoraleDecisiveDefeat };
        panel.addPairs(labels, controls);

        return panel;
    }

    private @Nonnull JPanel createStratConAutoResolvePanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("StratConAutoResolvePanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblAutoResolveMethod, comboAutoResolveMethod);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkAutoResolveVictoryChanceEnabled,
                chkAutoResolveExperimentalPacarGuiEnabled);
        panel.addRow(lblMinimapTheme, minimapThemeSelector);
        panel.addRow(lblAutoResolveNumberOfScenarios, spnAutoResolveNumberOfScenarios);

        return panel;
    }

    /**
     * Copies StratCon values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared rulesets options model to read values from
     */
    void readFromModel(@Nullable RulesetsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboSkillLevel.setSelectedItem(model.skillLevel);
        comboBoardScalingType.setSelectedItem(model.boardScalingType);
        spnOpForLanceTypeMeks.setValue(model.opForLanceTypeMeks);
        spnOpForLanceTypeMixed.setValue(model.opForLanceTypeMixed);
        spnOpForLanceTypeVehicles.setValue(model.opForLanceTypeVehicles);
        chkAutoGenerateOpForCallSigns.setSelected(model.autoGenerateOpForCallSigns);
        comboMinimumCallsignSkillLevel.setSelectedItem(model.minimumCallsignSkillLevel);
        chkUseDropShips.setSelected(model.useDropShips);
        chkRegionalMekVariations.setSelected(model.regionalMekVariations);
        chkAttachedPlayerCamouflage.setSelected(model.attachedPlayerCamouflage);
        chkPlayerControlsAttachedUnits.setSelected(model.playerControlsAttachedUnits);
        chkUseAdvancedBuildingGunEmplacements.setSelected(model.useAdvancedBuildingGunEmplacements);
        spnSPAUpgradeIntensity.setValue(model.spaUpgradeIntensity);
        spnReinforcementBaseTargetNumber.setValue(model.reinforcementBaseTargetNumber);
        chkAutoConfigMunitions.setSelected(model.autoConfigMunitions);
        chkClansObeyBiddingRules.setSelected(model.clansObeyBiddingRules);
        spnEnemyFacilityModifierDieSize.setValue(model.enemyFacilityModifierDieSize);
        spnAlliedFacilityModifierDieSize.setValue(model.alliedFacilityModifierDieSize);
        spnScenarioModMax.setValue(model.scenarioModMax);
        spnScenarioModChance.setValue(model.scenarioModChance);
        spnScenarioModBV.setValue(model.scenarioModBV);
        chkUseWeatherConditions.setSelected(model.useWeatherConditions);
        chkUseLightConditions.setSelected(model.useLightConditions);
        chkUsePlanetaryConditions.setSelected(model.usePlanetaryConditions);
        chkUseNoTornadoes.setSelected(model.useNoTornadoes);
        spnFixedMapChance.setValue(model.fixedMapChance);
        chkRestrictPartsByMission.setSelected(model.restrictPartsByMission);
        spnMoraleVictory.setValue(model.moraleVictoryEffect);
        spnMoraleDecisiveVictory.setValue(model.moraleDecisiveVictoryEffect);
        spnMoraleDefeat.setValue(model.moraleDefeatEffect);
        spnMoraleDecisiveDefeat.setValue(model.moraleDecisiveDefeatEffect);
        comboAutoResolveMethod.setSelectedItem(model.autoResolveMethod);
        minimapThemeSelector.setSelectedItem(model.strategicViewTheme);
        chkAutoResolveVictoryChanceEnabled.setSelected(model.autoResolveVictoryChanceEnabled);
        chkAutoResolveExperimentalPacarGuiEnabled.setSelected(model.autoResolveExperimentalPacarGuiEnabled);
        spnAutoResolveNumberOfScenarios.setValue(model.autoResolveNumberOfScenarios);
        comboStratConPlayType.setSelectedItem(model.stratConPlayType);
        chkUseAdvancedScouting.setSelected(model.useAdvancedScouting);
        chkNoSeedForces.setSelected(model.noSeedForces);
        chkUseGenericBattleValue.setSelected(model.useGenericBattleValue);
        chkUseVerboseBidding.setSelected(model.useVerboseBidding);
    }

    /**
     * Copies StratCon values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared rulesets options model to write values into
     */
    void writeToModel(@Nullable RulesetsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.skillLevel = comboSkillLevel.getSelectedItem();
        model.boardScalingType = comboBoardScalingType.getSelectedItem();
        model.opForLanceTypeMeks = (int) spnOpForLanceTypeMeks.getValue();
        model.opForLanceTypeMixed = (int) spnOpForLanceTypeMixed.getValue();
        model.opForLanceTypeVehicles = (int) spnOpForLanceTypeVehicles.getValue();
        model.autoGenerateOpForCallSigns = chkAutoGenerateOpForCallSigns.isSelected();
        model.minimumCallsignSkillLevel = comboMinimumCallsignSkillLevel.getSelectedItem();
        model.useDropShips = chkUseDropShips.isSelected();
        model.regionalMekVariations = chkRegionalMekVariations.isSelected();
        model.attachedPlayerCamouflage = chkAttachedPlayerCamouflage.isSelected();
        model.playerControlsAttachedUnits = chkPlayerControlsAttachedUnits.isSelected();
        model.useAdvancedBuildingGunEmplacements = chkUseAdvancedBuildingGunEmplacements.isSelected();
        model.spaUpgradeIntensity = (int) spnSPAUpgradeIntensity.getValue();
        model.reinforcementBaseTargetNumber = (int) spnReinforcementBaseTargetNumber.getValue();
        model.autoConfigMunitions = chkAutoConfigMunitions.isSelected();
        model.clansObeyBiddingRules = chkClansObeyBiddingRules.isSelected();
        model.enemyFacilityModifierDieSize = (int) spnEnemyFacilityModifierDieSize.getValue();
        model.alliedFacilityModifierDieSize = (int) spnAlliedFacilityModifierDieSize.getValue();
        model.scenarioModMax = (int) spnScenarioModMax.getValue();
        model.scenarioModChance = (int) spnScenarioModChance.getValue();
        model.scenarioModBV = (int) spnScenarioModBV.getValue();
        model.useWeatherConditions = chkUseWeatherConditions.isSelected();
        model.useLightConditions = chkUseLightConditions.isSelected();
        model.usePlanetaryConditions = chkUsePlanetaryConditions.isSelected();
        model.useNoTornadoes = chkUseNoTornadoes.isSelected();
        model.fixedMapChance = (int) spnFixedMapChance.getValue();
        model.restrictPartsByMission = chkRestrictPartsByMission.isSelected();
        model.moraleVictoryEffect = (int) spnMoraleVictory.getValue();
        model.moraleDecisiveVictoryEffect = (int) spnMoraleDecisiveVictory.getValue();
        model.moraleDefeatEffect = (int) spnMoraleDefeat.getValue();
        model.moraleDecisiveDefeatEffect = (int) spnMoraleDecisiveDefeat.getValue();
        model.autoResolveMethod = comboAutoResolveMethod.getSelectedItem();
        model.strategicViewTheme = minimapThemeSelector.getSelectedItem();
        model.autoResolveVictoryChanceEnabled = chkAutoResolveVictoryChanceEnabled.isSelected();
        model.autoResolveExperimentalPacarGuiEnabled = chkAutoResolveExperimentalPacarGuiEnabled.isSelected();
        model.autoResolveNumberOfScenarios = (int) spnAutoResolveNumberOfScenarios.getValue();
        model.stratConPlayType = comboStratConPlayType.getSelectedItem();
        model.useAdvancedScouting = chkUseAdvancedScouting.isSelected();
        model.noSeedForces = chkNoSeedForces.isSelected();
        model.useGenericBattleValue = chkUseGenericBattleValue.isSelected();
        model.useVerboseBidding = chkUseVerboseBidding.isSelected();
    }
}
