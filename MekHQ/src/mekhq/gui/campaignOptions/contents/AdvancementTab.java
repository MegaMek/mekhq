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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsModifierTablePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code AdvancementTab} class is responsible for rendering and managing
 * two primary tabs in the campaign options
 * interface: the Experience Awards (XP Awards) tab and the Skill Randomization
 * tab. These tabs allow for customization
 * of experience point distribution, randomization preferences, and skill
 * probabilities in the campaign.
 *
 * <p>
 * This class provides methods to initialize the UI components, load current
 * settings
 * from the campaign options, and update the options based on user input.
 * </p>
 */
public class AdvancementTab {
    private static final int ADVANCEMENT_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int ADVANCEMENT_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int ADVANCEMENT_LABEL_CONTROL_GAP = 12;
    private static final int ADVANCEMENT_GRID_CONTROL_COLUMN_WIDTH = 100;
    // First pair column = label column + the form's label/control gap, so a
    // two-column grid's column 3 lines up with
    // the control column of the 2-column form sections on the same page (e.g. XP
    // Award Options sits above the XP-award
    // grids). The following pair is narrower so the whole grid still stays within
    // the shared page-width floor
    // (measured: 312 + 303 -> 640px section, exactly the floor; column 3 lands at
    // x=312, matching the 2-column control).
    private static final int ADVANCEMENT_GRID_FIRST_PAIR_COLUMN_WIDTH = ADVANCEMENT_LABEL_COLUMN_WIDTH
            + ADVANCEMENT_LABEL_CONTROL_GAP;
    private static final int ADVANCEMENT_GRID_FOLLOWING_PAIR_COLUMN_WIDTH = 303;
    private static final int ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH = 290;
    private static final int RECRUITMENT_LABEL_COLUMN_WIDTH = 190;
    private static final int RECRUITMENT_CONTROL_COLUMN_WIDTH = 90;
    private static final int RECRUITMENT_PAIRS_PER_ROW = 2;
    private static final int MODIFIER_ROW_LABEL_COLUMN_WIDTH = 120;
    private static final int MODIFIER_CONTROL_COLUMN_WIDTH = 104;

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private AdvancementOptionsModel model;
    private boolean xpAwardsPageCreated;
    private boolean randomizationPageCreated;
    private boolean recruitmentBonusesPageCreated;

    // start XP Awards Tab
    private CampaignOptionsHeaderPanel xpAwardsHeader;
    private JLabel lblXpCostMultiplier;
    private JSpinner spnXpCostMultiplier;

    private JPanel pnlTasks;
    private JLabel lblTaskXP;
    private JSpinner spnTaskXP;
    private JLabel lblNTasksXP;
    private JSpinner spnNTasksXP;
    private JLabel lblSuccessXP;
    private JSpinner spnSuccessXP;
    private JLabel lblMistakeXP;
    private JSpinner spnMistakeXP;

    private JPanel pnlScenarios;
    private JLabel lblScenarioXP;
    private JSpinner spnScenarioXP;
    private JLabel lblKillXP;
    private JSpinner spnKillXP;
    private JLabel lblKills;
    private JSpinner spnKills;

    private JPanel pnlMissions;
    private JLabel lblVocationalXP;
    private JSpinner spnVocationalXP;
    private JLabel lblVocationalXPFrequency;
    private JSpinner spnVocationalXPFrequency;
    private JLabel lblVocationalXPTargetNumber;
    private JSpinner spnVocationalXPTargetNumber;
    private JLabel lblMissionXpFail;
    private JSpinner spnMissionXpFail;
    private JLabel lblMissionXpSuccess;
    private JSpinner spnMissionXpSuccess;
    private JLabel lblMissionXpOutstandingSuccess;
    private JSpinner spnMissionXpOutstandingSuccess;

    private JPanel pnlAdministrators;
    private JLabel lblContractNegotiationXP;
    private JSpinner spnContractNegotiationXP;
    private JLabel lblAdminWeeklyXP;
    private JSpinner spnAdminWeeklyXP;
    private JLabel lblAdminWeeklyXPPeriod;
    private JSpinner spnAdminWeeklyXPPeriod;
    // end XP Awards Tab

    // start Skill Randomization Tab
    private CampaignOptionsHeaderPanel skillRandomizationHeader;
    private JCheckBox chkExtraRandomness;

    private JPanel pnlPhenotype;
    private JLabel[] phenotypeLabels;
    private JSpinner[] phenotypeSpinners;

    private JPanel pnlExperienceLevelModifiers;
    private JLabel lblAbilityUltraGreen;
    private JSpinner spnAbilityUltraGreen;
    private JLabel lblAbilityGreen;
    private JSpinner spnAbilityGreen;
    private JLabel lblAbilityReg;
    private JSpinner spnAbilityReg;
    private JLabel lblAbilityVet;
    private JSpinner spnAbilityVet;
    private JLabel lblAbilityElite;
    private JSpinner spnAbilityElite;
    private JLabel lblAbilityHeroic;
    private JSpinner spnAbilityHeroic;
    private JLabel lblAbilityLegendary;
    private JSpinner spnAbilityLegendary;

    private JPanel pnlSpecialSkillModifiers;

    private JSpinner spnCommandSkillsUltraGreen;
    private JSpinner spnCommandSkillsGreen;
    private JSpinner spnCommandSkillsReg;
    private JSpinner spnCommandSkillsVet;
    private JSpinner spnCommandSkillsElite;
    private JSpinner spnCommandSkillsHeroic;
    private JSpinner spnCommandSkillsLegendary;

    private JSpinner spnUtilitySkillsUltraGreen;
    private JSpinner spnUtilitySkillsGreen;
    private JSpinner spnUtilitySkillsReg;
    private JSpinner spnUtilitySkillsVet;
    private JSpinner spnUtilitySkillsElite;
    private JSpinner spnUtilitySkillsHeroic;
    private JSpinner spnUtilitySkillsLegendary;

    private JLabel lblCombatSA;
    private JSpinner spnCombatSA;
    private JLabel lblSupportSA;
    private JSpinner spnSupportSA;

    private JLabel lblArtyProb;
    private JSpinner spnArtyProb;
    private JLabel lblArtyBonus;
    private JSpinner spnArtyBonus;

    private JLabel lblAntiMekSkill;
    private JSpinner spnAntiMekSkill;
    private JLabel lblSecondProb;
    private JSpinner spnSecondProb;
    private JLabel lblSecondBonus;
    private JSpinner spnSecondBonus;
    private JLabel lblRoleplaySkillsModifier;
    private JSpinner spnRoleplaySkillsModifier;
    // end Skill Randomization Tab

    private JPanel pnlRecruitmentBonusesCombat;
    private JLabel[] lblRecruitmentBonusCombat;
    private JSpinner[] spnRecruitmentBonusCombat;

    private JPanel pnlRecruitmentBonusesSupport;
    private JLabel[] lblRecruitmentBonusSupport;
    private JSpinner[] spnRecruitmentBonusSupport;
    // end Recruitment Bonus Tab

    /**
     * Constructs an {@code AdvancementTab} object for rendering and managing
     * campaign advancement configurations.
     *
     * @param campaign The {@code Campaign} instance from which the campaign options
     *                 and random skill preferences are
     *                 retrieved.
     */
    public AdvancementTab(Campaign campaign) {
        this.campaign = campaign;
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the UI components for the XP Awards and Skill Randomization tabs.
     * This includes setting up the
     * labels, panels, and spinners for each of the categories within the respective
     * tabs.
     */
    private void initialize() {
        initializeXPAwardsTab();
        initializeSkillRandomizationTab();
    }

    /**
     * Initializes the XP Awards tab by setting up the UI components, such as
     * labels, panels, and spinners, for various
     * experience-related options within the campaign.
     */
    private void initializeXPAwardsTab() {
        lblXpCostMultiplier = new JLabel();
        spnXpCostMultiplier = new JSpinner();

        pnlTasks = new JPanel();
        lblTaskXP = new JLabel();
        spnTaskXP = new JSpinner();
        lblNTasksXP = new JLabel();
        spnNTasksXP = new JSpinner();
        lblSuccessXP = new JLabel();
        spnSuccessXP = new JSpinner();
        lblMistakeXP = new JLabel();
        spnMistakeXP = new JSpinner();

        pnlScenarios = new JPanel();
        lblScenarioXP = new JLabel();
        spnScenarioXP = new JSpinner();
        lblKillXP = new JLabel();
        spnKillXP = new JSpinner();
        lblKills = new JLabel();
        spnKills = new JSpinner();

        pnlMissions = new JPanel();
        lblVocationalXP = new JLabel();
        spnVocationalXP = new JSpinner();
        lblVocationalXPFrequency = new JLabel();
        spnVocationalXPFrequency = new JSpinner();
        lblVocationalXPTargetNumber = new JLabel();
        spnVocationalXPTargetNumber = new JSpinner();
        lblMissionXpFail = new JLabel();
        spnMissionXpFail = new JSpinner();
        lblMissionXpSuccess = new JLabel();
        spnMissionXpSuccess = new JSpinner();
        lblMissionXpOutstandingSuccess = new JLabel();
        spnMissionXpOutstandingSuccess = new JSpinner();

        pnlAdministrators = new JPanel();
        lblContractNegotiationXP = new JLabel();
        spnContractNegotiationXP = new JSpinner();
        lblAdminWeeklyXP = new JLabel();
        spnAdminWeeklyXP = new JSpinner();
        lblAdminWeeklyXPPeriod = new JLabel();
        spnAdminWeeklyXPPeriod = new JSpinner();
    }

    /**
     * Creates and returns the Experience Awards (XP Awards) tab panel. This tab
     * allows users to configure experience
     * awards for tasks, scenarios, missions, and administrators, as well as set the
     * overall XP cost multiplier.
     *
     * @return A {@code JPanel} containing the configuration options for XP Awards
     *         in the campaign.
     */
    public JPanel xpAwardsTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_steel_viper.png";
        xpAwardsHeader = new CampaignOptionsHeaderPanel("XpAwardsTab", imageAddress);

        // Contents
        JPanel xpAwardOptions = createXpAwardOptionsPanel();
        pnlTasks = createTasksPanel();
        pnlScenarios = createScenariosPanel();
        pnlMissions = createMissionsPanel();
        pnlAdministrators = createAdministratorsPanel();
        xpAwardsPageCreated = true;
        updateXpAwardsControlsFromModel();

        return CampaignOptionsPagePanel.builder("XpAwardsTab", "XpAwardsTab", imageAddress)
                .header(xpAwardsHeader)
                .section("lblXpAwardsTab.text",
                        "lblXpAwardsTab.summary",
                        xpAwardOptions)
                .section("lblTasksPanel.text",
                        "lblTasksPanel.summary",
                        pnlTasks)
                .section("lblScenariosPanel.text",
                        "lblScenariosPanel.summary",
                        pnlScenarios,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblMissionsPanel.text",
                        "lblMissionsPanel.summary",
                        pnlMissions)
                .section("lblAdministratorsXpPanel.text",
                        "lblAdministratorsXpPanel.summary",
                        pnlAdministrators,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                                CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED))
                .build();
    }

    private JPanel createXpAwardOptionsPanel() {
        lblXpCostMultiplier = new CampaignOptionsLabel("XpCostMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblXpCostMultiplier.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "XpCostMultiplier"));
        spnXpCostMultiplier = new CampaignOptionsSpinner("XpCostMultiplier", 1, 0, 5, 0.05);
        spnXpCostMultiplier.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "XpCostMultiplier"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("XpAwardOptionsPanel",
                ADVANCEMENT_LABEL_COLUMN_WIDTH,
                ADVANCEMENT_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblXpCostMultiplier, spnXpCostMultiplier);

        return panel;
    }

    private CampaignOptionsPairedFieldGridPanel createAdvancementPairedGrid(String name, JComponent[] labels,
            JComponent[] controls) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                ADVANCEMENT_GRID_FIRST_PAIR_COLUMN_WIDTH,
                ADVANCEMENT_GRID_FOLLOWING_PAIR_COLUMN_WIDTH,
                ADVANCEMENT_GRID_CONTROL_COLUMN_WIDTH,
                2);
        panel.addPairs(labels, controls);

        return panel;
    }

    private CampaignOptionsPairedFieldGridPanel createAdvancementPairedGrid(String name, JComponent[] labels,
            JComponent[] controls, int pairColumnWidth) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                pairColumnWidth,
                pairColumnWidth,
                ADVANCEMENT_GRID_CONTROL_COLUMN_WIDTH,
                2);
        panel.addPairs(labels, controls);

        return panel;
    }

    /**
     * Creates and returns the Tasks panel, which allows users to configure settings
     * for task-related experience awards,
     * such as successful task completions or mistakes.
     *
     * @return A {@code JPanel} containing configuration options for task-related
     *         experience awards.
     */
    private JPanel createTasksPanel() {
        // Contents
        lblTaskXP = new CampaignOptionsLabel("TaskXP");
        lblTaskXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "TaskXP"));
        spnTaskXP = new CampaignOptionsSpinner("TaskXP", 0, 0, 20, 1);
        spnTaskXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "TaskXP"));

        lblNTasksXP = new CampaignOptionsLabel("NTasksXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        lblNTasksXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "NTasksXP"));
        spnNTasksXP = new CampaignOptionsSpinner("NTasksXP", 0, 0, 100, 1);
        spnNTasksXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "NTasksXP"));

        lblSuccessXP = new CampaignOptionsLabel("SuccessXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblSuccessXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "SuccessXP"));
        spnSuccessXP = new CampaignOptionsSpinner("SuccessXP", 0, 0, 20, 1);
        spnSuccessXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "SuccessXP"));

        lblMistakeXP = new CampaignOptionsLabel("MistakeXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblMistakeXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MistakeXP"));
        spnMistakeXP = new CampaignOptionsSpinner("MistakeXP", 0, 0, 20, 1);
        spnMistakeXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MistakeXP"));

        JComponent[] labels = { lblTaskXP, lblNTasksXP, lblSuccessXP, lblMistakeXP };
        JComponent[] controls = { spnTaskXP, spnNTasksXP, spnSuccessXP, spnMistakeXP };

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TasksPanel",
                ADVANCEMENT_LABEL_COLUMN_WIDTH,
                ADVANCEMENT_CONTROL_COLUMN_WIDTH);
        for (int index = 0; index < labels.length; index++) {
            panel.addRow(labels[index], controls[index]);
        }

        return panel;
    }

    /**
     * Creates and returns the Scenarios panel, which allows users to configure
     * settings for experience awards related
     * to scenarios, kills, and kill thresholds.
     *
     * @return A {@code JPanel} containing configuration options for
     *         scenario-related experience awards.
     */
    private JPanel createScenariosPanel() {
        // Contents
        lblScenarioXP = new CampaignOptionsLabel("ScenarioXP");
        lblScenarioXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "ScenarioXP"));
        spnScenarioXP = new CampaignOptionsSpinner("ScenarioXP", 0, 0, 20, 1);
        spnScenarioXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "ScenarioXP"));
        lblKillXP = new CampaignOptionsLabel("KillXP");
        lblKillXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "KillXP"));
        spnKillXP = new CampaignOptionsSpinner("KillXP", 0, 0, 20, 1);
        spnKillXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "KillXP"));

        lblKills = new CampaignOptionsLabel("Kills");
        lblKills.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "Kills"));
        spnKills = new CampaignOptionsSpinner("Kills", 0, 0, 100, 1);
        spnKills.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "Kills"));

        JComponent[] labels = { lblScenarioXP, lblKillXP, lblKills };
        JComponent[] controls = { spnScenarioXP, spnKillXP, spnKills };

        return createAdvancementPairedGrid("ScenariosPanel", labels, controls);
    }

    /**
     * Creates and returns the Missions panel, which allows users to configure
     * settings related to mission performance
     * and idle time experience bonuses in the campaign.
     *
     * @return A {@code JPanel} containing configuration options for mission-related
     *         experience settings.
     */
    private JPanel createMissionsPanel() {
        // Contents
        lblVocationalXP = new CampaignOptionsLabel("VocationalXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblVocationalXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXP"));
        spnVocationalXP = new CampaignOptionsSpinner("VocationalXP", 0, 0, 20, 1);
        spnVocationalXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXP"));
        lblVocationalXPFrequency = new CampaignOptionsLabel("VocationalXPFrequency");
        lblVocationalXPFrequency.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXPFrequency"));
        spnVocationalXPFrequency = new CampaignOptionsSpinner("VocationalXPFrequency", 0, 0, 12, 1);
        spnVocationalXPFrequency.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXPFrequency"));
        lblVocationalXPTargetNumber = new CampaignOptionsLabel("VocationalXPTargetNumber");
        lblVocationalXPTargetNumber.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXPTargetNumber"));
        spnVocationalXPTargetNumber = new CampaignOptionsSpinner("VocationalXPTargetNumber", 2, 0, 12, 1);
        spnVocationalXPTargetNumber.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "VocationalXPTargetNumber"));

        lblMissionXpFail = new CampaignOptionsLabel("MissionXpFail");
        lblMissionXpFail.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MissionXpFail"));
        spnMissionXpFail = new CampaignOptionsSpinner("MissionXpFail", 1, 0, 20, 1);
        spnMissionXpFail.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MissionXpFail"));

        lblMissionXpSuccess = new CampaignOptionsLabel("MissionXpSuccess");
        lblMissionXpSuccess.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MissionXpSuccess"));
        spnMissionXpSuccess = new CampaignOptionsSpinner("MissionXpSuccess", 1, 0, 20, 1);
        spnMissionXpSuccess.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "MissionXpSuccess"));

        lblMissionXpOutstandingSuccess = new CampaignOptionsLabel("MissionXpOutstandingSuccess");
        lblMissionXpOutstandingSuccess.addMouseListener(createTipPanelUpdater(xpAwardsHeader,
                "MissionXpOutstandingSuccess"));
        spnMissionXpOutstandingSuccess = new CampaignOptionsSpinner("MissionXpOutstandingSuccess", 1, 0, 20, 1);
        spnMissionXpOutstandingSuccess.addMouseListener(createTipPanelUpdater(xpAwardsHeader,
                "MissionXpOutstandingSuccess"));

        JComponent[] labels = { lblVocationalXP, lblVocationalXPFrequency, lblVocationalXPTargetNumber,
                lblMissionXpFail, lblMissionXpSuccess, lblMissionXpOutstandingSuccess };
        JComponent[] controls = { spnVocationalXP, spnVocationalXPFrequency, spnVocationalXPTargetNumber,
                spnMissionXpFail, spnMissionXpSuccess, spnMissionXpOutstandingSuccess };

        return createAdvancementPairedGrid("MissionsPanel", labels, controls);
    }

    /**
     * Creates and returns the Administrators panel, which allows users to configure
     * settings for contract negotiation
     * experience points and weekly experience points for administrators.
     *
     * @return A {@code JPanel} containing configuration options for administrator
     *         experience settings.
     */
    private JPanel createAdministratorsPanel() {
        // Contents
        lblAdminWeeklyXP = new CampaignOptionsLabel("AdminWeeklyXP");
        lblAdminWeeklyXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "AdminWeeklyXP"));
        spnAdminWeeklyXP = new CampaignOptionsSpinner("AdminWeeklyXP", 0, 0, 20, 1);
        spnAdminWeeklyXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "AdminWeeklyXP"));
        lblAdminWeeklyXPPeriod = new CampaignOptionsLabel("AdminWeeklyXPPeriod",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblAdminWeeklyXPPeriod.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "AdminWeeklyXPPeriod"));
        spnAdminWeeklyXPPeriod = new CampaignOptionsSpinner("AdminWeeklyXPPeriod", 1, 1, 52, 1);
        spnAdminWeeklyXPPeriod.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "AdminWeeklyXPPeriod"));

        lblContractNegotiationXP = new CampaignOptionsLabel("ContractNegotiationXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblContractNegotiationXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "ContractNegotiationXP"));
        spnContractNegotiationXP = new CampaignOptionsSpinner("ContractNegotiationXP", 0, 0, 20, 1);
        spnContractNegotiationXP.addMouseListener(createTipPanelUpdater(xpAwardsHeader, "ContractNegotiationXP"));

        JComponent[] labels = { lblAdminWeeklyXP, lblAdminWeeklyXPPeriod, lblContractNegotiationXP };
        JComponent[] controls = { spnAdminWeeklyXP, spnAdminWeeklyXPPeriod, spnContractNegotiationXP };

        return createAdvancementPairedGrid("AdministratorsXpPanel", labels, controls);
    }

    /**
     * Initializes the Skill Randomization tab by setting up the UI components,
     * including phenotype configurations,
     * random abilities, skill groups, and other randomization settings.
     */
    private void initializeSkillRandomizationTab() {
        chkExtraRandomness = new JCheckBox();

        pnlPhenotype = new JPanel();
        phenotypeLabels = new JLabel[] {}; // This will be initialized properly later
        phenotypeSpinners = new JSpinner[] {}; // This will be initialized properly later

        pnlSpecialSkillModifiers = new JPanel();

        spnCommandSkillsUltraGreen = new JSpinner();
        spnCommandSkillsGreen = new JSpinner();
        spnCommandSkillsReg = new JSpinner();
        spnCommandSkillsVet = new JSpinner();
        spnCommandSkillsElite = new JSpinner();
        spnCommandSkillsHeroic = new JSpinner();
        spnCommandSkillsLegendary = new JSpinner();

        spnUtilitySkillsUltraGreen = new JSpinner();
        spnUtilitySkillsGreen = new JSpinner();
        spnUtilitySkillsReg = new JSpinner();
        spnUtilitySkillsVet = new JSpinner();
        spnUtilitySkillsElite = new JSpinner();
        spnUtilitySkillsHeroic = new JSpinner();
        spnUtilitySkillsLegendary = new JSpinner();

        lblCombatSA = new JLabel();
        spnCombatSA = new JSpinner();
        lblSupportSA = new JLabel();
        spnSupportSA = new JSpinner();

        lblArtyProb = new JLabel();
        spnArtyProb = new JSpinner();
        lblArtyBonus = new JLabel();
        spnArtyBonus = new JSpinner();

        lblAntiMekSkill = new JLabel();
        spnAntiMekSkill = new JSpinner();
        lblSecondProb = new JLabel();
        spnSecondProb = new JSpinner();
        lblSecondBonus = new JLabel();
        spnSecondBonus = new JSpinner();

        lblRoleplaySkillsModifier = new JLabel();
        spnRoleplaySkillsModifier = new JSpinner();

        pnlExperienceLevelModifiers = new JPanel();
        lblAbilityGreen = new JLabel();
        spnAbilityGreen = new JSpinner();
        lblAbilityUltraGreen = new JLabel();
        spnAbilityUltraGreen = new JSpinner();
        lblAbilityReg = new JLabel();
        spnAbilityReg = new JSpinner();
        lblAbilityVet = new JLabel();
        spnAbilityVet = new JSpinner();
        lblAbilityElite = new JLabel();
        spnAbilityElite = new JSpinner();
        lblAbilityHeroic = new JLabel();
        spnAbilityHeroic = new JSpinner();
        lblAbilityLegendary = new JLabel();
        spnAbilityLegendary = new JSpinner();

        pnlRecruitmentBonusesCombat = new JPanel();
        lblRecruitmentBonusCombat = new JLabel[] {}; // This will be initialized properly later
        spnRecruitmentBonusCombat = new JSpinner[] {}; // This will be initialized properly later

        pnlRecruitmentBonusesSupport = new JPanel();
        lblRecruitmentBonusSupport = new JLabel[] {}; // This will be initialized properly later
        spnRecruitmentBonusSupport = new JSpinner[] {}; // This will be initialized properly later
    }

    /**
     * Creates and returns the Skill Randomization tab panel. This tab allows users
     * to configure settings related to
     * skill randomization, including phenotype probabilities and skill bonuses for
     * different experience levels and
     * skill groups.
     *
     * @return A {@code JPanel} containing the configuration options for skill
     *         randomization.
     */
    public JPanel skillRandomizationTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_republic_of_the_sphere.png";
        skillRandomizationHeader = new CampaignOptionsHeaderPanel("SkillRandomizationTab", imageAddress);

        // Contents
        JPanel randomizationOptions = createSkillRandomizationOptionsPanel();
        pnlPhenotype = createPhenotypePanel();
        pnlExperienceLevelModifiers = createExperienceLevelModifiersPanel();
        pnlSpecialSkillModifiers = createSpecialSkillModifiersPanel();
        randomizationPageCreated = true;
        updateRandomizationControlsFromModel();

        return CampaignOptionsPagePanel.builder("SkillRandomizationTab", "SkillRandomizationTab", imageAddress)
                .header(skillRandomizationHeader)
                .section("lblSkillRandomizationTab.text",
                        "lblSkillRandomizationTab.summary",
                        randomizationOptions)
                .section("lblPhenotypesPanel.text",
                        "lblPhenotypesPanel.summary",
                        pnlPhenotype)
                .section("lblExperienceLevelModifiersPanel.text",
                        "lblExperienceLevelModifiersPanel.summary",
                        pnlExperienceLevelModifiers,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT))
                .section("lblSpecialSkillModifiersPanel.text",
                        "lblSpecialSkillModifiersPanel.summary",
                        pnlSpecialSkillModifiers)
                .build();
    }

    private JPanel createSkillRandomizationOptionsPanel() {
        chkExtraRandomness = new CampaignOptionsCheckBox("ExtraRandomness",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkExtraRandomness.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "ExtraRandomness"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SkillRandomizationOptionsPanel",
                ADVANCEMENT_LABEL_COLUMN_WIDTH,
                ADVANCEMENT_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkExtraRandomness);

        return panel;
    }

    /**
     * Creates and returns the Phenotype panel, which allows users to configure
     * settings for phenotype probabilities in
     * the campaign. Each phenotype is assigned a spinner to adjust its probability.
     *
     * @return A {@code JPanel} containing configuration options for phenotype
     *         probabilities.
     */
    private JPanel createPhenotypePanel() {
        // Contents
        List<Phenotype> phenotypes = Phenotype.getExternalPhenotypes();
        phenotypeLabels = new JLabel[phenotypes.size()];
        phenotypeSpinners = new JSpinner[phenotypes.size()];

        for (int i = 0; i < phenotypes.size(); i++) {
            phenotypeLabels[i] = new CampaignOptionsLabel(phenotypes.get(i).getLabel());
            phenotypeLabels[i].addMouseListener(createTipPanelUpdater(skillRandomizationHeader,
                    null,
                    phenotypes.get(i).getTooltip()));
            phenotypeSpinners[i] = new CampaignOptionsSpinner(phenotypes.get(i).getLabel(), 0, 0, 100, 1);
            phenotypeSpinners[i].addMouseListener(createTipPanelUpdater(skillRandomizationHeader,
                    null,
                    phenotypes.get(i).getTooltip()));
        }

        return createAdvancementPairedGrid("PhenotypesPanel", phenotypeLabels, phenotypeSpinners,
                ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH);
    }

    private JPanel createExperienceLevelModifiersPanel() {
        createAbilityModifierControls();
        createCommandSkillModifierControls();
        createUtilitySkillModifierControls();

        final CampaignOptionsModifierTablePanel panel = new CampaignOptionsModifierTablePanel(
                "ExperienceLevelModifiersPanel",
                MODIFIER_ROW_LABEL_COLUMN_WIDTH,
                MODIFIER_CONTROL_COLUMN_WIDTH,
                createModifierColumnHeader("AbilityPanel"),
                createModifierColumnHeader("CommandSkillsPanel"),
                createModifierColumnHeader("UtilitySkillsPanel"));

        panel.addRow(lblAbilityUltraGreen,
                spnAbilityUltraGreen,
                spnCommandSkillsUltraGreen,
                spnUtilitySkillsUltraGreen);
        panel.addRow(lblAbilityGreen,
                spnAbilityGreen,
                spnCommandSkillsGreen,
                spnUtilitySkillsGreen);
        panel.addRow(lblAbilityReg,
                spnAbilityReg,
                spnCommandSkillsReg,
                spnUtilitySkillsReg);
        panel.addRow(lblAbilityVet,
                spnAbilityVet,
                spnCommandSkillsVet,
                spnUtilitySkillsVet);
        panel.addRow(lblAbilityElite,
                spnAbilityElite,
                spnCommandSkillsElite,
                spnUtilitySkillsElite);
        panel.addRow(lblAbilityHeroic,
                spnAbilityHeroic,
                spnCommandSkillsHeroic,
                spnUtilitySkillsHeroic);
        panel.addRow(lblAbilityLegendary,
                spnAbilityLegendary,
                spnCommandSkillsLegendary,
                spnUtilitySkillsLegendary);

        final CampaignOptionsFormPanel wrapper = new CampaignOptionsFormPanel("ExperienceLevelModifiersWrapperPanel",
                MODIFIER_ROW_LABEL_COLUMN_WIDTH,
                MODIFIER_CONTROL_COLUMN_WIDTH);
        wrapper.addFullWidthComponent(panel);

        return wrapper;
    }

    private void createAbilityModifierControls() {
        lblAbilityUltraGreen = createExperienceLevelLabel(SkillType.EXP_ULTRA_GREEN);
        spnAbilityUltraGreen = createSkillModifierSpinner("AbilityUltraGreen");
        lblAbilityGreen = createExperienceLevelLabel(SkillType.EXP_GREEN);
        spnAbilityGreen = createSkillModifierSpinner("AbilityGreen");
        lblAbilityReg = createExperienceLevelLabel(SkillType.EXP_REGULAR);
        spnAbilityReg = createSkillModifierSpinner("AbilityRegular");
        lblAbilityVet = createExperienceLevelLabel(SkillType.EXP_VETERAN);
        spnAbilityVet = createSkillModifierSpinner("AbilityVeteran");
        lblAbilityElite = createExperienceLevelLabel(SkillType.EXP_ELITE);
        spnAbilityElite = createSkillModifierSpinner("AbilityElite");
        lblAbilityHeroic = createExperienceLevelLabel(SkillType.EXP_HEROIC);
        spnAbilityHeroic = createSkillModifierSpinner("AbilityHeroic");
        lblAbilityLegendary = createExperienceLevelLabel(SkillType.EXP_LEGENDARY);
        spnAbilityLegendary = createSkillModifierSpinner("AbilityLegendary");
    }

    private void createCommandSkillModifierControls() {
        spnCommandSkillsUltraGreen = createSkillModifierSpinner("CommandSkillsUltraGreen");
        spnCommandSkillsGreen = createSkillModifierSpinner("CommandSkillsGreen");
        spnCommandSkillsReg = createSkillModifierSpinner("CommandSkillsRegular");
        spnCommandSkillsVet = createSkillModifierSpinner("CommandSkillsVeteran");
        spnCommandSkillsElite = createSkillModifierSpinner("CommandSkillsElite");
        spnCommandSkillsHeroic = createSkillModifierSpinner("CommandSkillsHeroic");
        spnCommandSkillsLegendary = createSkillModifierSpinner("CommandSkillsLegendary");
    }

    private void createUtilitySkillModifierControls() {
        spnUtilitySkillsUltraGreen = createSkillModifierSpinner("UtilitySkillsUltraGreen");
        spnUtilitySkillsGreen = createSkillModifierSpinner("UtilitySkillsGreen");
        spnUtilitySkillsReg = createSkillModifierSpinner("UtilitySkillsRegular");
        spnUtilitySkillsVet = createSkillModifierSpinner("UtilitySkillsVeteran");
        spnUtilitySkillsElite = createSkillModifierSpinner("UtilitySkillsElite");
        spnUtilitySkillsHeroic = createSkillModifierSpinner("UtilitySkillsHeroic");
        spnUtilitySkillsLegendary = createSkillModifierSpinner("UtilitySkillsLegendary");
    }

    private JLabel createModifierColumnHeader(String name) {
        return new JLabel(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text"));
    }

    private JLabel createExperienceLevelLabel(int experienceLevel) {
        return new JLabel(SkillType.getExperienceLevelName(experienceLevel));
    }

    private JSpinner createSkillModifierSpinner(String name) {
        JSpinner spinner = new CampaignOptionsSpinner(name, 0, -12, 12, 1);
        spinner.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, name));
        return spinner;
    }

    private JPanel createSpecialSkillModifiersPanel() {
        createSecondarySkillControls();
        createArtilleryControls();
        createSmallArmsControls();

        JComponent[] labels = { lblRoleplaySkillsModifier, lblAntiMekSkill, lblSecondProb, lblSecondBonus,
                lblArtyProb, lblArtyBonus, lblCombatSA, lblSupportSA };
        JComponent[] controls = { spnRoleplaySkillsModifier, spnAntiMekSkill, spnSecondProb, spnSecondBonus,
                spnArtyProb, spnArtyBonus, spnCombatSA, spnSupportSA };

        return createAdvancementPairedGrid("SpecialSkillModifiersPanel", labels, controls,
                ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH);
    }

    private void createSecondarySkillControls() {
        lblRoleplaySkillsModifier = new CampaignOptionsLabel("RoleplaySkillsModifier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRoleplaySkillsModifier.addMouseListener(createTipPanelUpdater(skillRandomizationHeader,
                "RoleplaySkillsModifier"));
        spnRoleplaySkillsModifier = new CampaignOptionsSpinner("RoleplaySkillsModifier", 0, -12, 12, 1);
        spnRoleplaySkillsModifier.addMouseListener(createTipPanelUpdater(skillRandomizationHeader,
                "RoleplaySkillsModifier"));

        lblAntiMekSkill = new CampaignOptionsLabel("AntiMekChance");
        lblAntiMekSkill.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "AntiMekChance"));
        spnAntiMekSkill = new CampaignOptionsSpinner("AntiMekChance", 0, 0, 100, 1);
        spnAntiMekSkill.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "AntiMekChance"));

        lblSecondProb = new CampaignOptionsLabel("SecondarySkillChance");
        lblSecondProb.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "SecondarySkillChance"));
        spnSecondProb = new CampaignOptionsSpinner("SecondarySkillChance", 0, 0, 100, 1);
        spnSecondProb.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "SecondarySkillChance"));

        lblSecondBonus = new CampaignOptionsLabel("SecondarySkillBonus");
        lblSecondBonus.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "SecondarySkillBonus"));
        spnSecondBonus = new CampaignOptionsSpinner("SecondarySkillBonus", 0, -12, 12, 1);
        spnSecondBonus.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "SecondarySkillBonus"));
    }

    private void createArtilleryControls() {
        lblArtyProb = new CampaignOptionsLabel("ArtilleryChance");
        lblArtyProb.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "ArtilleryChance"));
        spnArtyProb = new CampaignOptionsSpinner("ArtilleryChance", 0, 0, 100, 1);
        spnArtyProb.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "ArtilleryChance"));

        lblArtyBonus = new CampaignOptionsLabel("ArtilleryBonus");
        lblArtyBonus.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "ArtilleryBonus"));
        spnArtyBonus = new CampaignOptionsSpinner("ArtilleryBonus", 0, -12, 12, 1);
        spnArtyBonus.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "ArtilleryBonus"));
    }

    private void createSmallArmsControls() {
        lblCombatSA = new CampaignOptionsLabel("CombatSmallArms");
        lblCombatSA.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "CombatSmallArms"));
        spnCombatSA = new CampaignOptionsSpinner("CombatSmallArms", 0, -12, 12, 1);
        spnCombatSA.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "CombatSmallArms"));

        lblSupportSA = new CampaignOptionsLabel("NonCombatSmallArms");
        lblSupportSA.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "NonCombatSmallArms"));
        spnSupportSA = new CampaignOptionsSpinner("NonCombatSmallArms", 0, -12, 12, 1);
        spnSupportSA.addMouseListener(createTipPanelUpdater(skillRandomizationHeader, "NonCombatSmallArms"));
    }

    /**
     * Constructs and returns the panel containing recruitment bonus controls
     * grouped by combat and support roles.
     *
     * <p>
     * Includes the header and separately laid-out subpanels for combat and support
     * personnel roles.
     * </p>
     *
     * @return the fully configured {@link JPanel} for recruitment bonus settings
     */
    public JPanel recruitmentBonusesTab() {
        // Header
        // start Recruitment Bonus Tab
        String imageAddress = getImageDirectory() + "logo_calderon_protectorate.png";
        CampaignOptionsHeaderPanel recruitmentBonusesHeader = new CampaignOptionsHeaderPanel("RecruitmentBonusesTab",
                imageAddress);

        // Contents
        pnlRecruitmentBonusesCombat = createRecruitmentBonusesCombatPanel();
        pnlRecruitmentBonusesSupport = createRecruitmentBonusesSupportPanel();
        recruitmentBonusesPageCreated = true;
        updateRecruitmentBonusControlsFromModel();

        return CampaignOptionsPagePanel.builder("RecruitmentBonusesTab", "RecruitmentBonusesTab", imageAddress)
                .header(recruitmentBonusesHeader)
                .section("lblRecruitmentBonusesCombatPanel.text",
                        "lblRecruitmentBonusesCombatPanel.summary",
                        pnlRecruitmentBonusesCombat)
                .section("lblRecruitmentBonusesSupportPanel.text",
                        "lblRecruitmentBonusesSupportPanel.summary",
                        pnlRecruitmentBonusesSupport)
                .build();
    }

    /**
     * Creates and initializes a panel for setting recruitment bonuses for combat
     * personnel roles.
     *
     * <p>
     * This method arranges labels and spinner controls for all combat-specific
     * personnel roles
     * in a grid layout. Each row contains up to four roles, where each role is
     * represented by a label and a
     * corresponding numeric spinner control for input.
     * </p>
     *
     * @return a configured {@link JPanel} specifically for defining recruitment
     *         bonuses for combat roles
     */
    private JPanel createRecruitmentBonusesCombatPanel() {
        // Contents
        List<PersonnelRole> roles = PersonnelRole.getCombatRoles();
        lblRecruitmentBonusCombat = new JLabel[roles.size()];
        spnRecruitmentBonusCombat = new JSpinner[roles.size()];

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RecruitmentBonusesCombatPanel",
                RECRUITMENT_LABEL_COLUMN_WIDTH,
                RECRUITMENT_CONTROL_COLUMN_WIDTH);

        JComponent[] labelsAndControls = new JComponent[roles.size() * 2];
        for (int i = 0; i < roles.size(); i++) {
            lblRecruitmentBonusCombat[i] = new JLabel(roles.get(i).getLabel(false));
            spnRecruitmentBonusCombat[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            CampaignOptionsSpinner.installSelectAllOnFocus(spnRecruitmentBonusCombat[i]);
            labelsAndControls[i * 2] = lblRecruitmentBonusCombat[i];
            labelsAndControls[i * 2 + 1] = spnRecruitmentBonusCombat[i];
        }
        panel.addRowGrid(RECRUITMENT_PAIRS_PER_ROW, labelsAndControls);

        return panel;
    }

    /**
     * Creates and initializes a panel for setting recruitment bonuses for support
     * (non-combat) personnel roles.
     *
     * <p>
     * This method arranges labels and spinner controls for all support-specific
     * personnel roles
     * in a grid layout. Each row contains up to four roles, where each role is
     * represented by a label and a
     * corresponding numeric spinner control for input.
     * </p>
     *
     * @return a configured {@link JPanel} specifically for defining recruitment
     *         bonuses for support roles
     */
    private JPanel createRecruitmentBonusesSupportPanel() {
        // Contents
        List<PersonnelRole> roles = PersonnelRole.getSupportRoles();
        lblRecruitmentBonusSupport = new JLabel[roles.size()];
        spnRecruitmentBonusSupport = new JSpinner[roles.size()];

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RecruitmentBonusesSupportPanel",
                RECRUITMENT_LABEL_COLUMN_WIDTH,
                RECRUITMENT_CONTROL_COLUMN_WIDTH);

        JComponent[] labelsAndControls = new JComponent[roles.size() * 2];
        for (int i = 0; i < roles.size(); i++) {
            lblRecruitmentBonusSupport[i] = new JLabel(roles.get(i).getLabel(false));
            spnRecruitmentBonusSupport[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            CampaignOptionsSpinner.installSelectAllOnFocus(spnRecruitmentBonusSupport[i]);
            labelsAndControls[i * 2] = lblRecruitmentBonusSupport[i];
            labelsAndControls[i * 2 + 1] = spnRecruitmentBonusSupport[i];
        }
        panel.addRowGrid(RECRUITMENT_PAIRS_PER_ROW, labelsAndControls);

        return panel;
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into
     * the UI components from the campaign
     * options and random skill preferences.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into
     * the UI components from the given
     * {@code CampaignOptions} and {@code RandomSkillPreferences} objects.
     *
     * @param presetCampaignOptions        Optional {@code CampaignOptions} object
     *                                     to load values from; if {@code null},
     *                                     values are loaded from the current
     *                                     campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to load values from; if
     *                                     {@code null}, values are loaded from the
     *                                     current skill preferences.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        model = new AdvancementOptionsModel(options, skillPreferences);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current values from the XP Awards and Skill Randomization tabs to
     * the specified
     * {@code CampaignOptions} and {@code RandomSkillPreferences}.
     *
     * @param presetCampaignOptions        Optional {@code CampaignOptions} object
     *                                     to set values to; if {@code null},
     *                                     values are applied to the current
     *                                     campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to set values to; if
     *                                     {@code null}, values are applied to the
     *                                     current skill preferences.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        updateModelFromCreatedControls();
        model.applyTo(options, skillPreferences);

        // Finishing Touches
        // This must be the last item, after all other tabs, no matter what.
        if (presetRandomSkillPreferences == null) {
            campaign.setRandomSkillPreferences(randomSkillPreferences);
        }
    }

    private void updateCreatedControlsFromModel() {
        updateXpAwardsControlsFromModel();
        updateRandomizationControlsFromModel();
        updateRecruitmentBonusControlsFromModel();
    }

    private void updateXpAwardsControlsFromModel() {
        if (!xpAwardsPageCreated || model == null) {
            return;
        }

        spnXpCostMultiplier.setValue(model.xpCostMultiplier);
        spnTaskXP.setValue(model.taskXP);
        spnNTasksXP.setValue(model.nTasksXP);
        spnSuccessXP.setValue(model.successXP);
        spnMistakeXP.setValue(model.mistakeXP);
        spnScenarioXP.setValue(model.scenarioXP);
        spnKillXP.setValue(model.killXP);
        spnKills.setValue(model.killsForXP);
        spnVocationalXP.setValue(model.vocationalXP);
        spnVocationalXPFrequency.setValue(model.vocationalXPFrequency);
        spnVocationalXPTargetNumber.setValue(model.vocationalXPTargetNumber);
        spnMissionXpFail.setValue(model.missionXpFail);
        spnMissionXpSuccess.setValue(model.missionXpSuccess);
        spnMissionXpOutstandingSuccess.setValue(model.missionXpOutstandingSuccess);
        spnContractNegotiationXP.setValue(model.contractNegotiationXP);
        spnAdminWeeklyXP.setValue(model.adminWeeklyXP);
        spnAdminWeeklyXPPeriod.setValue(model.adminWeeklyXPPeriod);
    }

    private void updateRandomizationControlsFromModel() {
        if (!randomizationPageCreated || model == null) {
            return;
        }

        chkExtraRandomness.setSelected(model.randomizeSkill);
        for (int i = 0; i < Math.min(phenotypeSpinners.length, model.phenotypeProbabilities.length); i++) {
            phenotypeSpinners[i].setValue(model.phenotypeProbabilities[i]);
        }

        spnAbilityUltraGreen.setValue(model.specialAbilityBonus[SkillType.EXP_ULTRA_GREEN]);
        spnAbilityGreen.setValue(model.specialAbilityBonus[SkillType.EXP_GREEN]);
        spnAbilityReg.setValue(model.specialAbilityBonus[SkillType.EXP_REGULAR]);
        spnAbilityVet.setValue(model.specialAbilityBonus[SkillType.EXP_VETERAN]);
        spnAbilityElite.setValue(model.specialAbilityBonus[SkillType.EXP_ELITE]);
        spnAbilityHeroic.setValue(model.specialAbilityBonus[SkillType.EXP_HEROIC]);
        spnAbilityLegendary.setValue(model.specialAbilityBonus[SkillType.EXP_LEGENDARY]);

        spnCommandSkillsUltraGreen.setValue(model.commandSkillsModifier[SkillType.EXP_ULTRA_GREEN]);
        spnCommandSkillsGreen.setValue(model.commandSkillsModifier[SkillType.EXP_GREEN]);
        spnCommandSkillsReg.setValue(model.commandSkillsModifier[SkillType.EXP_REGULAR]);
        spnCommandSkillsVet.setValue(model.commandSkillsModifier[SkillType.EXP_VETERAN]);
        spnCommandSkillsElite.setValue(model.commandSkillsModifier[SkillType.EXP_ELITE]);
        spnCommandSkillsHeroic.setValue(model.commandSkillsModifier[SkillType.EXP_HEROIC]);
        spnCommandSkillsLegendary.setValue(model.commandSkillsModifier[SkillType.EXP_LEGENDARY]);

        spnUtilitySkillsUltraGreen.setValue(model.utilitySkillsModifier[SkillType.EXP_ULTRA_GREEN]);
        spnUtilitySkillsGreen.setValue(model.utilitySkillsModifier[SkillType.EXP_GREEN]);
        spnUtilitySkillsReg.setValue(model.utilitySkillsModifier[SkillType.EXP_REGULAR]);
        spnUtilitySkillsVet.setValue(model.utilitySkillsModifier[SkillType.EXP_VETERAN]);
        spnUtilitySkillsElite.setValue(model.utilitySkillsModifier[SkillType.EXP_ELITE]);
        spnUtilitySkillsHeroic.setValue(model.utilitySkillsModifier[SkillType.EXP_HEROIC]);
        spnUtilitySkillsLegendary.setValue(model.utilitySkillsModifier[SkillType.EXP_LEGENDARY]);

        spnRoleplaySkillsModifier.setValue(model.roleplaySkillsModifier);
        spnCombatSA.setValue(model.combatSmallArmsBonus);
        spnSupportSA.setValue(model.supportSmallArmsBonus);
        spnArtyProb.setValue(model.artilleryProb);
        spnArtyBonus.setValue(model.artilleryBonus);
        spnAntiMekSkill.setValue(model.antiMekProb);
        spnSecondProb.setValue(model.secondSkillProb);
        spnSecondBonus.setValue(model.secondSkillBonus);
    }

    private void updateRecruitmentBonusControlsFromModel() {
        if (!recruitmentBonusesPageCreated || model == null) {
            return;
        }

        final List<PersonnelRole> combatRoles = PersonnelRole.getCombatRoles();
        for (int i = 0; i < spnRecruitmentBonusCombat.length; i++) {
            PersonnelRole role = combatRoles.get(i);
            spnRecruitmentBonusCombat[i].setValue(model.recruitmentBonuses.getOrDefault(role, 0));
        }

        final List<PersonnelRole> supportRoles = PersonnelRole.getSupportRoles();
        for (int i = 0; i < spnRecruitmentBonusSupport.length; i++) {
            PersonnelRole role = supportRoles.get(i);
            spnRecruitmentBonusSupport[i].setValue(model.recruitmentBonuses.getOrDefault(role, 0));
        }
    }

    private void updateModelFromCreatedControls() {
        updateModelFromXpAwardsControls();
        updateModelFromRandomizationControls();
        updateModelFromRecruitmentBonusControls();
    }

    private void updateModelFromXpAwardsControls() {
        if (!xpAwardsPageCreated || model == null) {
            return;
        }

        model.xpCostMultiplier = (double) spnXpCostMultiplier.getValue();
        model.taskXP = (int) spnTaskXP.getValue();
        model.nTasksXP = (int) spnNTasksXP.getValue();
        model.successXP = (int) spnSuccessXP.getValue();
        model.mistakeXP = (int) spnMistakeXP.getValue();
        model.scenarioXP = (int) spnScenarioXP.getValue();
        model.killXP = (int) spnKillXP.getValue();
        model.killsForXP = (int) spnKills.getValue();
        model.vocationalXP = (int) spnVocationalXP.getValue();
        model.vocationalXPFrequency = (int) spnVocationalXPFrequency.getValue();
        model.vocationalXPTargetNumber = (int) spnVocationalXPTargetNumber.getValue();
        model.missionXpFail = (int) spnMissionXpFail.getValue();
        model.missionXpSuccess = (int) spnMissionXpSuccess.getValue();
        model.missionXpOutstandingSuccess = (int) spnMissionXpOutstandingSuccess.getValue();
        model.contractNegotiationXP = (int) spnContractNegotiationXP.getValue();
        model.adminWeeklyXP = (int) spnAdminWeeklyXP.getValue();
        model.adminWeeklyXPPeriod = (int) spnAdminWeeklyXPPeriod.getValue();
    }

    private void updateModelFromRandomizationControls() {
        if (!randomizationPageCreated || model == null) {
            return;
        }

        model.randomizeSkill = chkExtraRandomness.isSelected();
        for (int i = 0; i < Math.min(phenotypeSpinners.length, model.phenotypeProbabilities.length); i++) {
            model.phenotypeProbabilities[i] = (int) phenotypeSpinners[i].getValue();
        }

        model.specialAbilityBonus[SkillType.EXP_ULTRA_GREEN] = (int) spnAbilityUltraGreen.getValue();
        model.specialAbilityBonus[SkillType.EXP_GREEN] = (int) spnAbilityGreen.getValue();
        model.specialAbilityBonus[SkillType.EXP_REGULAR] = (int) spnAbilityReg.getValue();
        model.specialAbilityBonus[SkillType.EXP_VETERAN] = (int) spnAbilityVet.getValue();
        model.specialAbilityBonus[SkillType.EXP_ELITE] = (int) spnAbilityElite.getValue();
        model.specialAbilityBonus[SkillType.EXP_HEROIC] = (int) spnAbilityHeroic.getValue();
        model.specialAbilityBonus[SkillType.EXP_LEGENDARY] = (int) spnAbilityLegendary.getValue();

        model.commandSkillsModifier[SkillType.EXP_ULTRA_GREEN] = (int) spnCommandSkillsUltraGreen.getValue();
        model.commandSkillsModifier[SkillType.EXP_GREEN] = (int) spnCommandSkillsGreen.getValue();
        model.commandSkillsModifier[SkillType.EXP_REGULAR] = (int) spnCommandSkillsReg.getValue();
        model.commandSkillsModifier[SkillType.EXP_VETERAN] = (int) spnCommandSkillsVet.getValue();
        model.commandSkillsModifier[SkillType.EXP_ELITE] = (int) spnCommandSkillsElite.getValue();
        model.commandSkillsModifier[SkillType.EXP_HEROIC] = (int) spnCommandSkillsHeroic.getValue();
        model.commandSkillsModifier[SkillType.EXP_LEGENDARY] = (int) spnCommandSkillsLegendary.getValue();

        model.utilitySkillsModifier[SkillType.EXP_ULTRA_GREEN] = (int) spnUtilitySkillsUltraGreen.getValue();
        model.utilitySkillsModifier[SkillType.EXP_GREEN] = (int) spnUtilitySkillsGreen.getValue();
        model.utilitySkillsModifier[SkillType.EXP_REGULAR] = (int) spnUtilitySkillsReg.getValue();
        model.utilitySkillsModifier[SkillType.EXP_VETERAN] = (int) spnUtilitySkillsVet.getValue();
        model.utilitySkillsModifier[SkillType.EXP_ELITE] = (int) spnUtilitySkillsElite.getValue();
        model.utilitySkillsModifier[SkillType.EXP_HEROIC] = (int) spnUtilitySkillsHeroic.getValue();
        model.utilitySkillsModifier[SkillType.EXP_LEGENDARY] = (int) spnUtilitySkillsLegendary.getValue();

        model.roleplaySkillsModifier = (int) spnRoleplaySkillsModifier.getValue();
        model.combatSmallArmsBonus = (int) spnCombatSA.getValue();
        model.supportSmallArmsBonus = (int) spnSupportSA.getValue();
        model.artilleryProb = (int) spnArtyProb.getValue();
        model.artilleryBonus = (int) spnArtyBonus.getValue();
        model.antiMekProb = (int) spnAntiMekSkill.getValue();
        model.secondSkillProb = (int) spnSecondProb.getValue();
        model.secondSkillBonus = (int) spnSecondBonus.getValue();
    }

    private void updateModelFromRecruitmentBonusControls() {
        if (!recruitmentBonusesPageCreated || model == null) {
            return;
        }

        final List<PersonnelRole> supportRoles = PersonnelRole.getSupportRoles();
        final List<PersonnelRole> combatRoles = PersonnelRole.getCombatRoles();

        for (int i = 0; i < spnRecruitmentBonusCombat.length; i++) {
            PersonnelRole role = combatRoles.get(i);
            model.recruitmentBonuses.put(role, (int) spnRecruitmentBonusCombat[i].getValue());
        }

        for (int i = 0; i < spnRecruitmentBonusSupport.length; i++) {
            PersonnelRole role = supportRoles.get(i);
            model.recruitmentBonuses.put(role, (int) spnRecruitmentBonusSupport[i].getValue());
        }
    }

}
