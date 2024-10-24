package mekhq.gui.panes.campaignOptions;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class AdvancementTab {
    JFrame frame;
    String name;

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
    //end Skill Randomization Tab

    AdvancementTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeXPAwardsTab();
        initializeSkillRandomizationTab();
    }

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

    JPanel xpAwardsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("XpAwardsTab",
            getImageDirectory() + "logo_federated_suns.png",
            true);

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

    private JPanel createTasksPanel() {
        // Contents
        lblTaskXP = new CampaignOptionsLabel("TaskXP");
        spnTaskXP = new CampaignOptionsSpinner("TaskXP",
            0, 0, 10000, 1);

        lblNTasksXP = new CampaignOptionsLabel("NTasksXP");
        spnNTasksXP = new CampaignOptionsSpinner("NTasksXP",
            0, 0, 10000, 1);

        lblSuccessXP = new CampaignOptionsLabel("SuccessXP");
        spnSuccessXP = new CampaignOptionsSpinner("SuccessXP",
            0, 0, 10000, 1);

        lblMistakeXP = new CampaignOptionsLabel("MistakeXP");
        spnMistakeXP = new CampaignOptionsSpinner("MistakeXP",
            0, 0, 10000, 1);

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

    private JPanel createScenariosPanel() {
        // Contents
        lblScenarioXP = new CampaignOptionsLabel("ScenarioXP");
        spnScenarioXP = new CampaignOptionsSpinner("ScenarioXP",
            0, 0, 10000, 1);
        lblKillXP = new CampaignOptionsLabel("KillXP");
        spnKillXP = new CampaignOptionsSpinner("KillXP",
            0, 0, 10000, 1);

        lblKills = new CampaignOptionsLabel("Kills");
        spnKills = new CampaignOptionsSpinner("Kills",
            0, 0, 10000, 1);

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

    private JPanel createMissionsPanel() {
        // Contents
        lblIdleXP = new CampaignOptionsLabel("IdleXP");
        spnIdleXP = new CampaignOptionsSpinner("IdleXP",
            0, 0, 10000, 1);
        lblMonthsIdleXP = new CampaignOptionsLabel("MonthsIdleXP");
        spnMonthsIdleXP = new CampaignOptionsSpinner("MonthsIdleXP",
            0, 0, 36, 1);
        lblTargetIdleXP = new CampaignOptionsLabel("TargetIdleXP");
        spnTargetIdleXP = new CampaignOptionsSpinner("TargetIdleXP",
            2, 2, 13, 1);

        lblMissionXpFail = new CampaignOptionsLabel("MissionXpFail");
        spnMissionXpFail = new CampaignOptionsSpinner("MissionXpFail",
            1, 0, 10, 1);

        lblMissionXpSuccess = new CampaignOptionsLabel("MissionXpSuccess");
        spnMissionXpSuccess = new CampaignOptionsSpinner("MissionXpSuccess",
            1, 0, 10, 1);

        lblMissionXpOutstandingSuccess = new CampaignOptionsLabel("MissionXpOutstandingSuccess");
        spnMissionXpOutstandingSuccess = new CampaignOptionsSpinner("MissionXpOutstandingSuccess",
            1, 0, 10, 1);

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

    private JPanel createAdministratorsPanel() {
        // Contents
        lblAdminWeeklyXP = new CampaignOptionsLabel("AdminWeeklyXP");
        spnAdminWeeklyXP = new CampaignOptionsSpinner("AdminWeeklyXP",
            0, 0, 10000, 1);
        lblAdminWeeklyXPPeriod = new CampaignOptionsLabel("AdminWeeklyXPPeriod");
        spnAdminWeeklyXPPeriod = new CampaignOptionsSpinner("AdminWeeklyXPPeriod",
            1, 1, 100, 1);

        lblContractNegotiationXP = new CampaignOptionsLabel("ContractNegotiationXP");
        spnContractNegotiationXP = new CampaignOptionsSpinner("ContractNegotiationXP",
            0, 0, 10000, 1);
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

    private void initializeSkillRandomizationTab() {

    }
}
