/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.contents;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * The {@code AdvancementTab} class is responsible for rendering and managing two primary tabs
 * in the campaign options interface: the Experience Awards (XP Awards) tab and the Skill
 * Randomization tab. These tabs allow for customization of experience point distribution,
 * randomization preferences, and skill probabilities in the campaign.
 *
 * <p>This class provides methods to initialize the UI components, load current settings
 * from the campaign options, and update the options based on user input.</p>
 */
public class AdvancementTab {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;

    //start XP Awards Tab
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
    private JLabel lblIdleXP;
    private JSpinner spnIdleXP;
    private JLabel lblMonthsIdleXP;
    private JSpinner spnMonthsIdleXP;
    private JLabel lblTargetIdleXP;
    private JSpinner spnTargetIdleXP;
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
    //end XP Awards Tab

    //start Skill Randomization Tab
    private JCheckBox chkExtraRandomness;

    private JPanel pnlPhenotype;
    private JLabel[] phenotypeLabels;
    private JSpinner[] phenotypeSpinners;

    private JPanel pnlRandomAbilities;
    private JLabel lblAbilityGreen;
    private JSpinner spnAbilityGreen;
    private JLabel lblAbilityReg;
    private JSpinner spnAbilityReg;
    private JLabel lblAbilityVet;
    private JSpinner spnAbilityVet;
    private JLabel lblAbilityElite;
    private JSpinner spnAbilityElite;

    private JPanel pnlSkillGroups;

    private JPanel pnlTactics;
    private JLabel lblTacticsGreen;
    private JSpinner spnTacticsGreen;
    private JLabel lblTacticsReg;
    private JSpinner spnTacticsReg;
    private JLabel lblTacticsVet;
    private JSpinner spnTacticsVet;
    private JLabel lblTacticsElite;
    private JSpinner spnTacticsElite;

    private JPanel pnlSmallArms;
    private JLabel lblCombatSA;
    private JSpinner spnCombatSA;
    private JLabel lblSupportSA;
    private JSpinner spnSupportSA;

    private JPanel pnlArtillery;
    private JLabel lblArtyProb;
    private JSpinner spnArtyProb;
    private JLabel lblArtyBonus;
    private JSpinner spnArtyBonus;

    private JPanel pnlSecondarySkills;
    private JLabel lblAntiMekSkill;
    private JSpinner spnAntiMekSkill;
    private JLabel lblSecondProb;
    private JSpinner spnSecondProb;
    private JLabel lblSecondBonus;
    private JSpinner spnSecondBonus;
    //end Skill Randomization Tab

    /**
     * Constructs an {@code AdvancementTab} object for rendering and managing campaign advancement
     * configurations.
     *
     * @param campaign The {@code Campaign} instance from which the campaign options and random
     *                skill preferences are retrieved.
     */
    public AdvancementTab(Campaign campaign) {
        this.campaign = campaign;
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the UI components for the XP Awards and Skill Randomization tabs. This includes
     * setting up the labels, panels, and spinners for each of the categories within the respective tabs.
     */
    private void initialize() {
        initializeXPAwardsTab();
        initializeSkillRandomizationTab();
    }

    /**
     * Initializes the XP Awards tab by setting up the UI components, such as labels,
     * panels, and spinners, for various experience-related options within the campaign.
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
        lblIdleXP = new JLabel();
        spnIdleXP = new JSpinner();
        lblMonthsIdleXP = new JLabel();
        spnMonthsIdleXP = new JSpinner();
        lblTargetIdleXP = new JLabel();
        spnTargetIdleXP = new JSpinner();
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
     * Creates and returns the Experience Awards (XP Awards) tab panel. This tab allows users to
     * configure experience awards for tasks, scenarios, missions, and administrators, as well
     * as set the overall XP cost multiplier.
     *
     * @return A {@code JPanel} containing the configuration options for XP Awards in the campaign.
     */
    public JPanel xpAwardsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("XpAwardsTab",
            getImageDirectory() + "logo_clan_steel_viper.png");

        // Contents
        lblXpCostMultiplier = new CampaignOptionsLabel("XpCostMultiplier");
        spnXpCostMultiplier = new CampaignOptionsSpinner("XpCostMultiplier",
            1, 0, 5, 0.05);

        pnlTasks = createTasksPanel();
        pnlScenarios = createScenariosPanel();
        pnlAdministrators = createAdministratorsPanel();
        pnlMissions = createMissionsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("XpAwardsTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblXpCostMultiplier, layout);
        layout.gridx++;
        panel.add(spnXpCostMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 3;
        panel.add(pnlTasks, layout);
        layout.gridx += 3;
        layout.gridwidth = 1;
        panel.add(pnlMissions, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 3;
        panel.add(pnlScenarios, layout);
        layout.gridx += 3;
        layout.gridwidth = 1;
        panel.add(pnlAdministrators, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "XpAwardsTab");
    }

    /**
     * Creates and returns the Tasks panel, which allows users to configure settings
     * for task-related experience awards, such as successful task completions or mistakes.
     *
     * @return A {@code JPanel} containing configuration options for task-related experience awards.
     */
    private JPanel createTasksPanel() {
        // Contents
        lblTaskXP = new CampaignOptionsLabel("TaskXP");
        spnTaskXP = new CampaignOptionsSpinner("TaskXP",
            0, 0, 20, 1);

        lblNTasksXP = new CampaignOptionsLabel("NTasksXP");
        spnNTasksXP = new CampaignOptionsSpinner("NTasksXP",
            0, 0, 100, 1);

        lblSuccessXP = new CampaignOptionsLabel("SuccessXP");
        spnSuccessXP = new CampaignOptionsSpinner("SuccessXP",
            0, 0, 20, 1);

        lblMistakeXP = new CampaignOptionsLabel("MistakeXP");
        spnMistakeXP = new CampaignOptionsSpinner("MistakeXP",
            0, 0, 20, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TasksPanel", true,
            "TasksPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(spnTaskXP, layout);
        layout.gridx++;
        panel.add(lblTaskXP, layout);
        layout.gridx++;
        panel.add(spnNTasksXP, layout);
        layout.gridx++;
        panel.add(lblNTasksXP, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(spnSuccessXP, layout);
        layout.gridx++;
        layout.gridwidth = 2;
        panel.add(lblSuccessXP, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(spnMistakeXP, layout);
        layout.gridx++;
        layout.gridwidth = 2;
        panel.add(lblMistakeXP, layout);

        return panel;
    }

    /**
     * Creates and returns the Scenarios panel, which allows users to configure settings
     * for experience awards related to scenarios, kills, and kill thresholds.
     *
     * @return A {@code JPanel} containing configuration options for scenario-related experience awards.
     */
    private JPanel createScenariosPanel() {
        // Contents
        lblScenarioXP = new CampaignOptionsLabel("ScenarioXP");
        spnScenarioXP = new CampaignOptionsSpinner("ScenarioXP",
            0, 0, 20, 1);
        lblKillXP = new CampaignOptionsLabel("KillXP");
        spnKillXP = new CampaignOptionsSpinner("KillXP",
            0, 0, 20, 1);

        lblKills = new CampaignOptionsLabel("Kills");
        spnKills = new CampaignOptionsSpinner("Kills",
            0, 0, 100, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ScenariosPanel", true,
            "ScenariosPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(spnScenarioXP, layout);
        layout.gridx++;
        layout.gridwidth = 2;
        panel.add(lblScenarioXP, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(spnKillXP, layout);
        layout.gridx++;
        panel.add(lblKillXP, layout);
        layout.gridx++;
        panel.add(spnKills, layout);
        layout.gridx++;
        panel.add(lblKills, layout);

        return panel;
    }

    /**
     * Creates and returns the Missions panel, which allows users to configure settings
     * related to mission performance and idle time experience bonuses in the campaign.
     *
     * @return A {@code JPanel} containing configuration options for mission-related experience settings.
     */
    private JPanel createMissionsPanel() {
        // Contents
        lblIdleXP = new CampaignOptionsLabel("IdleXP");
        spnIdleXP = new CampaignOptionsSpinner("IdleXP",
            0, 0, 20, 1);
        lblMonthsIdleXP = new CampaignOptionsLabel("MonthsIdleXP");
        spnMonthsIdleXP = new CampaignOptionsSpinner("MonthsIdleXP",
            0, 0, 12, 1);
        lblTargetIdleXP = new CampaignOptionsLabel("TargetIdleXP");
        spnTargetIdleXP = new CampaignOptionsSpinner("TargetIdleXP",
            2, 0, 12, 1);

        lblMissionXpFail = new CampaignOptionsLabel("MissionXpFail");
        spnMissionXpFail = new CampaignOptionsSpinner("MissionXpFail",
            1, 0, 20, 1);

        lblMissionXpSuccess = new CampaignOptionsLabel("MissionXpSuccess");
        spnMissionXpSuccess = new CampaignOptionsSpinner("MissionXpSuccess",
            1, 0, 20, 1);

        lblMissionXpOutstandingSuccess = new CampaignOptionsLabel("MissionXpOutstandingSuccess");
        spnMissionXpOutstandingSuccess = new CampaignOptionsSpinner("MissionXpOutstandingSuccess",
            1, 0, 20, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("MissionsPanel", true,
            "MissionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(spnIdleXP, layout);
        layout.gridx++;
        panel.add(lblIdleXP, layout);
        layout.gridx++;
        panel.add(spnMonthsIdleXP, layout);
        layout.gridx++;
        panel.add(lblMonthsIdleXP, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(lblTargetIdleXP, layout);
        layout.gridx += 2;
        layout.gridwidth = 1;
        panel.add(spnTargetIdleXP, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(spnMissionXpFail, layout);
        layout.gridx++;
        panel.add(lblMissionXpFail, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(spnMissionXpSuccess, layout);
        layout.gridx++;
        panel.add(lblMissionXpSuccess, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(spnMissionXpOutstandingSuccess, layout);
        layout.gridx++;
        layout.gridwidth = 2;
        panel.add(lblMissionXpOutstandingSuccess, layout);

        return panel;
    }

    /**
     * Creates and returns the Administrators panel, which allows users to configure settings
     * for contract negotiation experience points and weekly experience points for administrators.
     *
     * @return A {@code JPanel} containing configuration options for administrator experience settings.
     */
    private JPanel createAdministratorsPanel() {
        // Contents
        lblAdminWeeklyXP = new CampaignOptionsLabel("AdminWeeklyXP");
        spnAdminWeeklyXP = new CampaignOptionsSpinner("AdminWeeklyXP",
            0, 0, 20, 1);
        lblAdminWeeklyXPPeriod = new CampaignOptionsLabel("AdminWeeklyXPPeriod");
        spnAdminWeeklyXPPeriod = new CampaignOptionsSpinner("AdminWeeklyXPPeriod",
            1, 1, 52, 1);

        lblContractNegotiationXP = new CampaignOptionsLabel("ContractNegotiationXP");
        spnContractNegotiationXP = new CampaignOptionsSpinner("ContractNegotiationXP",
            0, 0, 20, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministratorsXpPanel", true,
            "AdministratorsXpPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(spnAdminWeeklyXP, layout);
        layout.gridx++;
        panel.add(lblAdminWeeklyXP, layout);
        layout.gridx++;
        panel.add(spnAdminWeeklyXPPeriod, layout);
        layout.gridx++;
        panel.add(lblAdminWeeklyXPPeriod, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(spnContractNegotiationXP, layout);
        layout.gridx++;
        layout.gridwidth = 2;
        panel.add(lblContractNegotiationXP, layout);

        return panel;
    }

    /**
     * Initializes the Skill Randomization tab by setting up the UI components,
     * including phenotype configurations, random abilities, skill groups, and other
     * randomization settings.
     */
    private void initializeSkillRandomizationTab() {
        chkExtraRandomness = new JCheckBox();

        pnlPhenotype = new JPanel();
        phenotypeLabels = new JLabel[] {}; // This will be initialized properly later
        phenotypeSpinners = new JSpinner[] {}; // This will be initialized properly later

        pnlSkillGroups = new JPanel();

        pnlTactics = new JPanel();
        lblTacticsGreen = new JLabel();
        spnTacticsGreen = new JSpinner();
        lblTacticsReg = new JLabel();
        spnTacticsReg = new JSpinner();
        lblTacticsVet = new JLabel();
        spnTacticsVet = new JSpinner();
        lblTacticsElite = new JLabel();
        spnTacticsElite = new JSpinner();

        pnlSmallArms = new JPanel();
        lblCombatSA = new JLabel();
        spnCombatSA = new JSpinner();
        lblSupportSA = new JLabel();
        spnSupportSA = new JSpinner();

        pnlArtillery = new JPanel();
        lblArtyProb = new JLabel();
        spnArtyProb = new JSpinner();
        lblArtyBonus = new JLabel();
        spnArtyBonus = new JSpinner();

        pnlSecondarySkills = new JPanel();
        lblAntiMekSkill = new JLabel();
        spnAntiMekSkill = new JSpinner();
        lblSecondProb = new JLabel();
        spnSecondProb = new JSpinner();
        lblSecondBonus = new JLabel();
        spnSecondBonus = new JSpinner();

        pnlRandomAbilities = new JPanel();
        lblAbilityGreen = new JLabel();
        spnAbilityGreen = new JSpinner();
        lblAbilityReg = new JLabel();
        spnAbilityReg = new JSpinner();
        lblAbilityVet = new JLabel();
        spnAbilityVet = new JSpinner();
        lblAbilityElite = new JLabel();
        spnAbilityElite = new JSpinner();
    }

    /**
     * Creates and returns the Skill Randomization tab panel. This tab allows users to configure
     * settings related to skill randomization, including phenotype probabilities and skill bonuses
     * for different experience levels and skill groups.
     *
     * @return A {@code JPanel} containing the configuration options for skill randomization.
     */
    public JPanel skillRandomizationTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("SkillRandomizationTab",
            getImageDirectory() + "logo_republic_of_the_sphere.png");

        // Contents
        chkExtraRandomness = new CampaignOptionsCheckBox("ExtraRandomness");

        pnlPhenotype = createPhenotypePanel();
        pnlRandomAbilities = createAbilityPanel();
        pnlSkillGroups = createSkillGroupPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SkillRandomizationTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(chkExtraRandomness, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(pnlPhenotype, layout);
        layout.gridx++;
        panel.add(pnlRandomAbilities, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(pnlSkillGroups, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "XpAwardsTab");
    }

    /**
     * Creates and returns the Phenotype panel, which allows users to configure settings
     * for phenotype probabilities in the campaign. Each phenotype is assigned a spinner
     * to adjust its probability.
     *
     * @return A {@code JPanel} containing configuration options for phenotype probabilities.
     */
    private JPanel createPhenotypePanel() {
        // Contents
        List<Phenotype> phenotypes = Phenotype.getExternalPhenotypes();
        phenotypeLabels = new JLabel[phenotypes.size()];
        phenotypeSpinners = new JSpinner[phenotypes.size()];

        final JPanel panel = new CampaignOptionsStandardPanel("PhenotypesPanel", true,
            "PhenotypesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = -1;
        layout.gridy = 0;

        for (int i = 0; i < phenotypes.size(); i++) {
            phenotypeLabels[i] = new CampaignOptionsLabel(phenotypes.get(i).getName());
            phenotypeSpinners[i] = new CampaignOptionsSpinner(phenotypes.get(i).getName(),
                0, 0, 100, 1);

            layout.gridx++;
            panel.add(phenotypeLabels[i], layout);
            layout.gridx++;
            panel.add(phenotypeSpinners[i], layout);

            if (i == (phenotypes.size() / 3)) {
                layout.gridx = -1;
                layout.gridy++;
            }
        }

        return panel;
    }

    /**
     * Creates and returns the Ability panel, which allows users to configure settings
     * for bonuses to special abilities at different experience levels, such as green,
     * regular, veteran, and elite.
     *
     * @return A {@code JPanel} containing configuration options for special ability bonuses.
     */
    private JPanel createAbilityPanel() {
        // Contents
        lblAbilityGreen = new CampaignOptionsLabel("AbilityGreen");
        spnAbilityGreen = new CampaignOptionsSpinner("AbilityGreen",
            0, -10, 10, 1);
        lblAbilityReg = new CampaignOptionsLabel("AbilityRegular");
        spnAbilityReg = new CampaignOptionsSpinner("AbilityRegular",
            0, -10, 10, 1);

        lblAbilityVet = new CampaignOptionsLabel("AbilityVeteran");
        spnAbilityVet = new CampaignOptionsSpinner("AbilityVeteran",
            0, -10, 10, 1);

        lblAbilityElite = new CampaignOptionsLabel("AbilityElite");
        spnAbilityElite = new CampaignOptionsSpinner("AbilityElite",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AbilityPanel", true,
            "AbilityPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAbilityGreen, layout);
        layout.gridx++;
        panel.add(spnAbilityGreen, layout);
        layout.gridx++;
        panel.add(lblAbilityReg, layout);
        layout.gridx++;
        panel.add(spnAbilityReg, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAbilityVet, layout);
        layout.gridx++;
        panel.add(spnAbilityVet, layout);
        layout.gridx++;
        panel.add(lblAbilityElite, layout);
        layout.gridx++;
        panel.add(spnAbilityElite, layout);

        return panel;
    }

    /**
     * Creates and returns the Skill Groups panel, which groups together related panels
     * for configuring various skill-related options, including tactics, small arms,
     * secondary skills, and artillery.
     *
     * @return A {@code JPanel} containing grouped configuration options for skills.
     */
    private JPanel createSkillGroupPanel() {
        // Contents
        pnlArtillery = createArtilleryPanel();
        pnlSmallArms = createSmallArmsPanel();
        pnlSecondarySkills = createSecondarySkillPanel();

        pnlTactics = createTacticsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SkillGroupsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(pnlTactics, layout);
        layout.gridx++;
        panel.add(pnlSmallArms, layout);
        layout.gridx++;
        panel.add(pnlSecondarySkills, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(pnlSecondarySkills, layout);
        layout.gridx++;
        panel.add(pnlArtillery, layout);

        return panel;
    }

    /**
     * Creates and returns the Tactics panel, which allows users to configure settings
     * for Tactics modifiers for different skill levels, such as green, regular, veteran, and elite.
     *
     * @return A {@code JPanel} containing configuration options for Tactics modifiers.
     */
    private JPanel createTacticsPanel() {
        // Contents
        lblTacticsGreen = new CampaignOptionsLabel("TacticsGreen");
        spnTacticsGreen = new CampaignOptionsSpinner("TacticsGreen",
            0, -10, 10, 1);

        lblTacticsReg = new CampaignOptionsLabel("TacticsRegular");
        spnTacticsReg = new CampaignOptionsSpinner("TacticsRegular",
            0, -10, 10, 1);

        lblTacticsVet = new CampaignOptionsLabel("TacticsVeteran");
        spnTacticsVet = new CampaignOptionsSpinner("TacticsVeteran",
            0, -10, 10, 1);

        lblTacticsElite = new CampaignOptionsLabel("TacticsElite");
        spnTacticsElite = new CampaignOptionsSpinner("TacticsElite",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TacticsPanel", true,
            "TacticsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblTacticsGreen, layout);
        layout.gridx++;
        panel.add(spnTacticsGreen, layout);
        layout.gridx++;
        panel.add(lblTacticsReg, layout);
        layout.gridx++;
        panel.add(spnTacticsReg, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblTacticsVet, layout);
        layout.gridx++;
        panel.add(spnTacticsVet, layout);
        layout.gridx++;
        panel.add(lblTacticsElite, layout);
        layout.gridx++;
        panel.add(spnTacticsElite, layout);

        return panel;
    }

    /**
     * Creates and returns the Small Arms panel, which allows users to configure settings
     * for combat and non-combat small arms bonuses.
     *
     * @return A {@code JPanel} containing configuration options for small arms skill bonuses.
     */
    private JPanel createSmallArmsPanel() {
        // Contents
        lblCombatSA = new CampaignOptionsLabel("CombatSmallArms");
        spnCombatSA = new CampaignOptionsSpinner("CombatSmallArms",
            0, -10, 10, 1);

        lblSupportSA = new CampaignOptionsLabel("NonCombatSmallArms");
        spnSupportSA = new CampaignOptionsSpinner("NonCombatSmallArms",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SmallArmsPanel",
            true, "SmallArmsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblCombatSA, layout);
        layout.gridx++;
        panel.add(spnCombatSA, layout);
        layout.gridx++;
        panel.add(lblSupportSA, layout);
        layout.gridx++;
        panel.add(spnSupportSA, layout);

        return panel;
    }

    /**
     * Creates and returns the Artillery panel, which allows users to configure settings
     * for artillery-specific skills, including the chance for an artillery skill and
     * the bonus associated with it.
     *
     * @return A {@code JPanel} containing configuration options for artillery-related skills.
     */
    private JPanel createArtilleryPanel() {
        // Contents
        lblArtyProb = new CampaignOptionsLabel("ArtilleryChance");
        spnArtyProb = new CampaignOptionsSpinner("ArtilleryChance",
            0, -10, 10, 1);

        lblArtyBonus = new CampaignOptionsLabel("ArtilleryBonus");
        spnArtyBonus = new CampaignOptionsSpinner("ArtilleryBonus",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ArtilleryPanel",
            true, "ArtilleryPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblArtyProb, layout);
        layout.gridx++;
        panel.add(spnArtyProb, layout);
        layout.gridx++;
        panel.add(lblArtyBonus, layout);
        layout.gridx++;
        panel.add(spnArtyBonus, layout);

        return panel;
    }

    /**
     * Creates and returns the Secondary Skill panel, which allows users to configure settings
     * related to special secondary skills such as Anti-Mek, secondary skill probability,
     * and secondary skill bonuses.
     *
     * @return A {@code JPanel} containing configuration options for secondary skills.
     */
    private JPanel createSecondarySkillPanel() {
        // Contents
        lblAntiMekSkill = new CampaignOptionsLabel("AntiMekChance");
        spnAntiMekSkill = new CampaignOptionsSpinner("AntiMekChance",
            0, -10, 10, 1);

        lblSecondProb = new CampaignOptionsLabel("SecondarySkillChance");
        spnSecondProb = new CampaignOptionsSpinner("SecondarySkillChance",
            0, -10, 10, 1);

        lblSecondBonus = new CampaignOptionsLabel("SecondarySkillBonus");
        spnSecondBonus = new CampaignOptionsSpinner("SecondarySkillBonus",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SecondarySkillPanel",
            true, "SecondarySkillPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAntiMekSkill, layout);
        layout.gridx++;
        panel.add(spnAntiMekSkill, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSecondProb, layout);
        layout.gridx++;
        panel.add(spnSecondProb, layout);
        layout.gridx++;
        panel.add(lblSecondBonus, layout);
        layout.gridx++;
        panel.add(spnSecondBonus, layout);

        return panel;
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into the UI components
     * from the campaign options and random skill preferences.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into the UI components
     * from the given {@code CampaignOptions} and {@code RandomSkillPreferences} objects.
     *
     * @param presetCampaignOptions    Optional {@code CampaignOptions} object to load values from;
     *                                 if {@code null}, values are loaded from the current campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to load values
     *                                 from; if {@code null}, values are loaded from the current skill preferences.
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

        //start XP Awards Tab
        spnXpCostMultiplier.setValue(options.getXpCostMultiplier());
        spnTaskXP.setValue(options.getTaskXP());
        spnNTasksXP.setValue(options.getNTasksXP());
        spnSuccessXP.setValue(options.getSuccessXP());
        spnMistakeXP.setValue(options.getMistakeXP());
        spnScenarioXP.setValue(options.getScenarioXP());
        spnKillXP.setValue(options.getKillXPAward());
        spnKills.setValue(options.getKillsForXP());
        spnIdleXP.setValue(options.getIdleXP());
        spnMonthsIdleXP.setValue(options.getMonthsIdleXP());
        spnTargetIdleXP.setValue(options.getTargetIdleXP());
        spnMissionXpFail.setValue(options.getMissionXpFail());
        spnMissionXpSuccess.setValue(options.getMissionXpSuccess());
        spnMissionXpOutstandingSuccess.setValue(options.getMissionXpOutstandingSuccess());
        spnContractNegotiationXP.setValue(options.getContractNegotiationXP());
        spnAdminWeeklyXP.setValue(options.getAdminXP());
        spnAdminWeeklyXPPeriod.setValue(options.getAdminXPPeriod());

        //start Skill Randomization Tab
        chkExtraRandomness.setSelected(skillPreferences.randomizeSkill());
        final int[] phenotypeProbabilities = options.getPhenotypeProbabilities();
        for (int i = 0; i < phenotypeSpinners.length; i++) {
            phenotypeSpinners[i].setValue(phenotypeProbabilities[i]);
        }
        spnAbilityGreen.setValue(skillPreferences.getSpecialAbilityBonus(SkillType.EXP_GREEN));
        spnAbilityReg.setValue(skillPreferences.getSpecialAbilityBonus(SkillType.EXP_REGULAR));
        spnAbilityVet.setValue(skillPreferences.getSpecialAbilityBonus(SkillType.EXP_VETERAN));
        spnAbilityElite.setValue(skillPreferences.getSpecialAbilityBonus(SkillType.EXP_ELITE));
        spnTacticsGreen.setValue(skillPreferences.getTacticsMod(SkillType.EXP_GREEN));
        spnTacticsReg.setValue(skillPreferences.getTacticsMod(SkillType.EXP_REGULAR));
        spnTacticsVet.setValue(skillPreferences.getTacticsMod(SkillType.EXP_VETERAN));
        spnTacticsElite.setValue(skillPreferences.getTacticsMod(SkillType.EXP_ELITE));
        spnCombatSA.setValue(skillPreferences.getCombatSmallArmsBonus());
        spnSupportSA.setValue(skillPreferences.getSupportSmallArmsBonus());
        spnArtyProb.setValue(skillPreferences.getArtilleryProb());
        spnArtyBonus.setValue(skillPreferences.getArtilleryBonus());
        spnAntiMekSkill.setValue(skillPreferences.getAntiMekProb());
        spnSecondProb.setValue(skillPreferences.getSecondSkillProb());
        spnSecondBonus.setValue(skillPreferences.getSecondSkillBonus());
    }

    /**
     * Applies the current values from the XP Awards and Skill Randomization tabs
     * to the specified {@code CampaignOptions} and {@code RandomSkillPreferences}.
     *
     * @param presetCampaignOptions    Optional {@code CampaignOptions} object to set values to;
     *                                 if {@code null}, values are applied to the current campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to set values
     *                                 to; if {@code null}, values are applied to the current skill preferences.
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

        //start XP Awards Tab
        options.setXpCostMultiplier((Double) spnXpCostMultiplier.getValue());
        options.setTaskXP((Integer) spnTaskXP.getValue());
        options.setNTasksXP((Integer) spnNTasksXP.getValue());
        options.setSuccessXP((Integer) spnSuccessXP.getValue());
        options.setMistakeXP((Integer) spnMistakeXP.getValue());
        options.setScenarioXP((Integer) spnScenarioXP.getValue());
        options.setKillXPAward((Integer) spnKillXP.getValue());
        options.setKillsForXP((Integer) spnKills.getValue());
        options.setIdleXP((Integer) spnIdleXP.getValue());
        options.setMonthsIdleXP((Integer) spnMonthsIdleXP.getValue());
        options.setTargetIdleXP((Integer) spnTargetIdleXP.getValue());
        options.setMissionXpFail((Integer) spnMissionXpFail.getValue());
        options.setMissionXpSuccess((Integer) spnMissionXpSuccess.getValue());
        options.setMissionXpOutstandingSuccess((Integer) spnMissionXpOutstandingSuccess.getValue());
        options.setContractNegotiationXP((Integer) spnContractNegotiationXP.getValue());
        options.setAdminXP((Integer) spnAdminWeeklyXP.getValue());
        options.setAdminXPPeriod((Integer) spnAdminWeeklyXPPeriod.getValue());

        //start Skill Randomization Tab
        skillPreferences.setRandomizeSkill(chkExtraRandomness.isSelected());
        for (int i = 0; i < phenotypeSpinners.length; i++) {
            options.setPhenotypeProbability(i, (Integer) phenotypeSpinners[i].getValue());
        }

        skillPreferences.setAntiMekProb((Integer) spnAntiMekSkill.getValue());
        skillPreferences.setArtilleryProb((Integer) spnArtyProb.getValue());
        skillPreferences.setArtilleryBonus((Integer) spnArtyBonus.getValue());
        skillPreferences.setSecondSkillProb((Integer) spnSecondProb.getValue());
        skillPreferences.setSecondSkillBonus((Integer) spnSecondBonus.getValue());
        skillPreferences.setTacticsMod(SkillType.EXP_GREEN, (Integer) spnTacticsGreen.getValue());
        skillPreferences.setTacticsMod(SkillType.EXP_REGULAR, (Integer) spnTacticsReg.getValue());
        skillPreferences.setTacticsMod(SkillType.EXP_VETERAN, (Integer) spnTacticsVet.getValue());
        skillPreferences.setTacticsMod(SkillType.EXP_ELITE, (Integer) spnTacticsElite.getValue());
        skillPreferences.setCombatSmallArmsBonus((Integer) spnCombatSA.getValue());
        skillPreferences.setSupportSmallArmsBonus((Integer) spnSupportSA.getValue());
        skillPreferences.setSpecialAbilityBonus(SkillType.EXP_GREEN, (Integer) spnAbilityGreen.getValue());
        skillPreferences.setSpecialAbilityBonus(SkillType.EXP_REGULAR, (Integer) spnAbilityReg.getValue());
        skillPreferences.setSpecialAbilityBonus(SkillType.EXP_VETERAN, (Integer) spnAbilityVet.getValue());
        skillPreferences.setSpecialAbilityBonus(SkillType.EXP_ELITE, (Integer) spnAbilityElite.getValue());

        if (presetRandomSkillPreferences == null) {
            campaign.setRandomSkillPreferences(randomSkillPreferences);
        }
    }
}
