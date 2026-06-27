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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code XpAwardsPage} class builds and manages the Experience Awards leaf page of the Campaign Options dialog. It
 * owns the widgets for experience point distribution - task, scenario, mission, and administrator awards as well as the
 * overall XP cost multiplier - and synchronises them with a shared {@link AwardsAndRandomizationOptionsModel}.
 *
 * <p>This view is a sub-component of {@link AwardsAndRandomizationPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code AwardsAndRandomizationPages}, while this class is responsible only for constructing the
 * XP Awards panel and copying award values to and from the model. The page is built lazily; until
 * {@link #createPanel(AwardsAndRandomizationOptionsModel)} is called, {@link #readFromModel(AwardsAndRandomizationOptionsModel)}
 * and {@link #writeToModel(AwardsAndRandomizationOptionsModel)} are no-ops.</p>
 */
class XpAwardsPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
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
    private static final int ADVANCEMENT_GRID_FIRST_PAIR_COLUMN_WIDTH = LABEL_COLUMN_WIDTH
            + ADVANCEMENT_LABEL_CONTROL_GAP;
    private static final int ADVANCEMENT_GRID_FOLLOWING_PAIR_COLUMN_WIDTH = 303;

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

    private boolean created;

    /**
     * Creates and returns the Experience Awards (XP Awards) page panel. This page
     * allows users to configure experience
     * awards for tasks, scenarios, missions, and administrators, as well as set the
     * overall XP cost multiplier.
     *
     * @param model the shared awards and randomization options model to populate the freshly built controls from
     *
     * @return A {@code JPanel} containing the configuration options for XP Awards
     *         in the campaign.
     */
    @Nonnull JPanel createPanel(@Nullable AwardsAndRandomizationOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_steel_viper.png";
        xpAwardsHeader = new CampaignOptionsHeaderPanel("XpAwardsPage", imageAddress);

        // Contents
        JPanel xpAwardOptions = createXpAwardOptionsPanel();
        pnlTasks = createTasksPanel();
        pnlScenarios = createScenariosPanel();
        pnlMissions = createMissionsPanel();
        pnlAdministrators = createAdministratorsPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("XpAwardsPage", "XpAwardsPage", imageAddress)
                .header(xpAwardsHeader)
                .section("lblXpAwardsPage.text",
                        "lblXpAwardsPage.summary",
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

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createXpAwardOptionsPanel() {
        lblXpCostMultiplier = new CampaignOptionsLabel("XpCostMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblXpCostMultiplier.addMouseListener(createTipPanelUpdater("XpCostMultiplier"));
        spnXpCostMultiplier = new CampaignOptionsSpinner("XpCostMultiplier", 1, 0, 5, 0.05);
        spnXpCostMultiplier.addMouseListener(createTipPanelUpdater("XpCostMultiplier"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("XpAwardOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblXpCostMultiplier, spnXpCostMultiplier);

        return panel;
    }

    private @Nonnull CampaignOptionsPairedFieldGridPanel createAdvancementPairedGrid(String name, JComponent[] labels,
            JComponent[] controls) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                ADVANCEMENT_GRID_FIRST_PAIR_COLUMN_WIDTH,
                ADVANCEMENT_GRID_FOLLOWING_PAIR_COLUMN_WIDTH,
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
    private @Nonnull JPanel createTasksPanel() {
        // Contents
        lblTaskXP = new CampaignOptionsLabel("TaskXP");
        lblTaskXP.addMouseListener(createTipPanelUpdater("TaskXP"));
        spnTaskXP = new CampaignOptionsSpinner("TaskXP", 0, 0, 20, 1);
        spnTaskXP.addMouseListener(createTipPanelUpdater("TaskXP"));

        lblNTasksXP = new CampaignOptionsLabel("NTasksXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        lblNTasksXP.addMouseListener(createTipPanelUpdater("NTasksXP"));
        spnNTasksXP = new CampaignOptionsSpinner("NTasksXP", 0, 0, 100, 1);
        spnNTasksXP.addMouseListener(createTipPanelUpdater("NTasksXP"));

        lblSuccessXP = new CampaignOptionsLabel("SuccessXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblSuccessXP.addMouseListener(createTipPanelUpdater("SuccessXP"));
        spnSuccessXP = new CampaignOptionsSpinner("SuccessXP", 0, 0, 20, 1);
        spnSuccessXP.addMouseListener(createTipPanelUpdater("SuccessXP"));

        lblMistakeXP = new CampaignOptionsLabel("MistakeXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblMistakeXP.addMouseListener(createTipPanelUpdater("MistakeXP"));
        spnMistakeXP = new CampaignOptionsSpinner("MistakeXP", 0, 0, 20, 1);
        spnMistakeXP.addMouseListener(createTipPanelUpdater("MistakeXP"));

        JComponent[] labels = { lblTaskXP, lblNTasksXP, lblSuccessXP, lblMistakeXP };
        JComponent[] controls = { spnTaskXP, spnNTasksXP, spnSuccessXP, spnMistakeXP };

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TasksPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
    private @Nonnull JPanel createScenariosPanel() {
        // Contents
        lblScenarioXP = new CampaignOptionsLabel("ScenarioXP");
        lblScenarioXP.addMouseListener(createTipPanelUpdater("ScenarioXP"));
        spnScenarioXP = new CampaignOptionsSpinner("ScenarioXP", 0, 0, 20, 1);
        spnScenarioXP.addMouseListener(createTipPanelUpdater("ScenarioXP"));
        lblKillXP = new CampaignOptionsLabel("KillXP");
        lblKillXP.addMouseListener(createTipPanelUpdater("KillXP"));
        spnKillXP = new CampaignOptionsSpinner("KillXP", 0, 0, 20, 1);
        spnKillXP.addMouseListener(createTipPanelUpdater("KillXP"));

        lblKills = new CampaignOptionsLabel("Kills");
        lblKills.addMouseListener(createTipPanelUpdater("Kills"));
        spnKills = new CampaignOptionsSpinner("Kills", 0, 0, 100, 1);
        spnKills.addMouseListener(createTipPanelUpdater("Kills"));

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
    private @Nonnull JPanel createMissionsPanel() {
        // Contents
        lblVocationalXP = new CampaignOptionsLabel("VocationalXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblVocationalXP.addMouseListener(createTipPanelUpdater("VocationalXP"));
        spnVocationalXP = new CampaignOptionsSpinner("VocationalXP", 0, 0, 20, 1);
        spnVocationalXP.addMouseListener(createTipPanelUpdater("VocationalXP"));
        lblVocationalXPFrequency = new CampaignOptionsLabel("VocationalXPFrequency");
        lblVocationalXPFrequency.addMouseListener(createTipPanelUpdater("VocationalXPFrequency"));
        spnVocationalXPFrequency = new CampaignOptionsSpinner("VocationalXPFrequency", 0, 0, 12, 1);
        spnVocationalXPFrequency.addMouseListener(createTipPanelUpdater("VocationalXPFrequency"));
        lblVocationalXPTargetNumber = new CampaignOptionsLabel("VocationalXPTargetNumber");
        lblVocationalXPTargetNumber.addMouseListener(createTipPanelUpdater("VocationalXPTargetNumber"));
        spnVocationalXPTargetNumber = new CampaignOptionsSpinner("VocationalXPTargetNumber", 2, 0, 12, 1);
        spnVocationalXPTargetNumber.addMouseListener(createTipPanelUpdater("VocationalXPTargetNumber"));

        lblMissionXpFail = new CampaignOptionsLabel("MissionXpFail");
        lblMissionXpFail.addMouseListener(createTipPanelUpdater("MissionXpFail"));
        spnMissionXpFail = new CampaignOptionsSpinner("MissionXpFail", 1, 0, 20, 1);
        spnMissionXpFail.addMouseListener(createTipPanelUpdater("MissionXpFail"));

        lblMissionXpSuccess = new CampaignOptionsLabel("MissionXpSuccess");
        lblMissionXpSuccess.addMouseListener(createTipPanelUpdater("MissionXpSuccess"));
        spnMissionXpSuccess = new CampaignOptionsSpinner("MissionXpSuccess", 1, 0, 20, 1);
        spnMissionXpSuccess.addMouseListener(createTipPanelUpdater("MissionXpSuccess"));

        lblMissionXpOutstandingSuccess = new CampaignOptionsLabel("MissionXpOutstandingSuccess");
        lblMissionXpOutstandingSuccess.addMouseListener(createTipPanelUpdater("MissionXpOutstandingSuccess"));
        spnMissionXpOutstandingSuccess = new CampaignOptionsSpinner("MissionXpOutstandingSuccess", 1, 0, 20, 1);
        spnMissionXpOutstandingSuccess.addMouseListener(createTipPanelUpdater("MissionXpOutstandingSuccess"));

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
    private @Nonnull JPanel createAdministratorsPanel() {
        // Contents
        lblAdminWeeklyXP = new CampaignOptionsLabel("AdminWeeklyXP");
        lblAdminWeeklyXP.addMouseListener(createTipPanelUpdater("AdminWeeklyXP"));
        spnAdminWeeklyXP = new CampaignOptionsSpinner("AdminWeeklyXP", 0, 0, 20, 1);
        spnAdminWeeklyXP.addMouseListener(createTipPanelUpdater("AdminWeeklyXP"));
        lblAdminWeeklyXPPeriod = new CampaignOptionsLabel("AdminWeeklyXPPeriod",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblAdminWeeklyXPPeriod.addMouseListener(createTipPanelUpdater("AdminWeeklyXPPeriod"));
        spnAdminWeeklyXPPeriod = new CampaignOptionsSpinner("AdminWeeklyXPPeriod", 1, 1, 52, 1);
        spnAdminWeeklyXPPeriod.addMouseListener(createTipPanelUpdater("AdminWeeklyXPPeriod"));

        lblContractNegotiationXP = new CampaignOptionsLabel("ContractNegotiationXP",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblContractNegotiationXP.addMouseListener(createTipPanelUpdater("ContractNegotiationXP"));
        spnContractNegotiationXP = new CampaignOptionsSpinner("ContractNegotiationXP", 0, 0, 20, 1);
        spnContractNegotiationXP.addMouseListener(createTipPanelUpdater("ContractNegotiationXP"));

        JComponent[] labels = { lblAdminWeeklyXP, lblAdminWeeklyXPPeriod, lblContractNegotiationXP };
        JComponent[] controls = { spnAdminWeeklyXP, spnAdminWeeklyXPPeriod, spnContractNegotiationXP };

        return createAdvancementPairedGrid("AdministratorsXpPanel", labels, controls);
    }

    /**
     * Copies XP award values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared awards and randomization options model to read values from
     */
    void readFromModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
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

    /**
     * Copies XP award values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared awards and randomization options model to write values into
     */
    void writeToModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
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
}
