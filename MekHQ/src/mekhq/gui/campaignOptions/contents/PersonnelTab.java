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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.GridBagConstraints;
import javax.swing.*;

import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code PersonnelTab} class represents the user interface components for configuring personnel-related options in
 * the MekHQ Campaign Options dialog. This class handles the initialization, layout, and logic for various personnel
 * settings spanning multiple tabs, such as general personnel options, personnel logs, information, awards, medical
 * options, salaries, and prisoners and dependents.
 * <p>
 * The class is organized into multiple tabs that encapsulate settings under specific categories:
 * </p>
 * <ul>
 *   <li><b>General Tab:</b> General settings for personnel management such as tactics,
 *   initiative bonus, toughness, and edge settings.</li>
 *   <li><b>Personnel Logs Tab:</b> Settings for logging activities like skill or ability
 *   gains, personnel transfers, and kill records.</li>
 *   <li><b>Personnel Information Tab:</b> Configuration of options for displaying
 *   personnel details like time in service, time in rank, and earnings tracking.</li>
 *   <li><b>Awards Tab:</b> Options for managing awards given during play, including
 *   auto-awards, tier size configurations, and specific award filters.</li>
 *   <li><b>Medical Tab:</b> Medical-related settings such as healing time,
 *   advanced medical usage, and tougher healing options.</li>
 *   <li><b>Prisoners and Dependents Tab:</b> Configuration of prisoner handling
 *   and dependent-related rules.</li>
 *   <li><b>Salaries Tab:</b> Configuration of salaries based on roles, experience
 *   multipliers, and base salary rates.</li>
 * </ul>
 *
 * <p>
 * This class serves as the main controller for the UI components of the Personnel Tab,
 * bridging the user interface with the {@link CampaignOptions} and ensuring the appropriate
 * application of configuration settings.
 * </p>
 */
public class PersonnelTab {
    private final CampaignOptions campaignOptions;

    //start General Tab
    private CampaignOptionsHeaderPanel generalHeader;
    private JPanel pnlPersonnelGeneralOptions;
    private JCheckBox chkUseTactics;
    private JCheckBox chkUseInitiativeBonus;
    private JCheckBox chkUseToughness;
    private JCheckBox chkUseRandomToughness;
    private JCheckBox chkUseArtillery;
    private JCheckBox chkUseAbilities;
    private JCheckBox chkOnlyCommandersMatterVehicles;
    private JCheckBox chkOnlyCommandersMatterInfantry;
    private JCheckBox chkOnlyCommandersMatterBattleArmor;
    private JCheckBox chkUseEdge;
    private JCheckBox chkUseSupportEdge;
    private JCheckBox chkUseImplants;
    private JCheckBox chkUseAlternativeQualityAveraging;

    private JPanel pnlPersonnelCleanup;
    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;

    private JPanel pnlAdministrators;
    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;
    //end General Tab

    //start Personnel Logs Tab
    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseExtendedTOEForceName;
    private JCheckBox chkPersonnelLogSkillGain;
    private JCheckBox chkPersonnelLogAbilityGain;
    private JCheckBox chkPersonnelLogEdgeGain;
    private JCheckBox chkDisplayPersonnelLog;
    private JCheckBox chkDisplayScenarioLog;
    private JCheckBox chkDisplayKillRecord;
    private JCheckBox chkDisplayMedicalRecord;
    private JCheckBox chkDisplayPatientRecord;
    private JCheckBox chkDisplayAssignmentRecord;
    private JCheckBox chkDisplayPerformanceRecord;
    //end Personnel Logs Tab

    //start Personnel Information Tab
    private CampaignOptionsHeaderPanel personnelInformationHeader;
    private JCheckBox chkUseTimeInService;
    private JLabel lblTimeInServiceDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private JLabel lblTimeInRankDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkTrackTotalXPEarnings;
    private JCheckBox chkShowOriginFaction;
    //end Personnel Information Tab

    //start Awards Tab
    private CampaignOptionsHeaderPanel awardsHeader;
    private JPanel pnlAwardsGeneralOptions;
    private JLabel lblAwardBonusStyle;
    private MMComboBox<AwardBonus> comboAwardBonusStyle;
    private JLabel lblAwardTierSize;
    private JSpinner spnAwardTierSize;
    private JCheckBox chkEnableAutoAwards;
    private JCheckBox chkIssuePosthumousAwards;
    private JCheckBox chkIssueBestAwardOnly;
    private JCheckBox chkIgnoreStandardSet;

    private JPanel pnlAutoAwardsFilter;
    private JCheckBox chkEnableContractAwards;
    private JCheckBox chkEnableFactionHunterAwards;
    private JCheckBox chkEnableInjuryAwards;
    private JCheckBox chkEnableIndividualKillAwards;
    private JCheckBox chkEnableFormationKillAwards;
    private JCheckBox chkEnableRankAwards;
    private JCheckBox chkEnableScenarioAwards;
    private JCheckBox chkEnableSkillAwards;
    private JCheckBox chkEnableTheatreOfWarAwards;
    private JCheckBox chkEnableTimeAwards;
    private JCheckBox chkEnableTrainingAwards;
    private JCheckBox chkEnableMiscAwards;
    private JTextArea txtAwardSetFilterList;
    //end Awards Tab

    private JCheckBox chkUseAdvancedMedical;
    private JLabel lblHealWaitingPeriod;
    private JSpinner spnHealWaitingPeriod;
    private JLabel lblNaturalHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JLabel lblMinimumHitsForVehicles;
    private JSpinner spnMinimumHitsForVehicles;
    private JCheckBox chkUseRandomHitsForVehicles;
    private JCheckBox chkUseTougherHealing;
    private JCheckBox chkUseAlternativeAdvancedMedical;
    private JCheckBox chkUseKinderAlternativeAdvancedMedical;
    private JCheckBox chkUseRandomDiseases;
    private JLabel lblMaximumPatients;
    private JSpinner spnMaximumPatients;
    private JCheckBox chkDoctorsUseAdministration;
    private JCheckBox chkUseUsefulMedics;
    private JCheckBox chkUseMASHTheatres;
    private JLabel lblMASHTheatreCapacity;
    private JSpinner spnMASHTheatreCapacity;
    //end Medical Tab

    //start Prisoners and Dependents Tab
    private CampaignOptionsHeaderPanel prisonersAndDependentsHeader;
    private JPanel prisonerPanel;
    private JLabel lblPrisonerCaptureStyle;
    private MMComboBox<PrisonerCaptureStyle> comboPrisonerCaptureStyle;
    private JCheckBox chkResetTemporaryPrisonerCapacity;
    private JCheckBox chkUseFunctionalEscapeArtist;

    private JPanel dependentsPanel;
    private JCheckBox chkUseRandomDependentAddition;
    private JCheckBox chkUseRandomDependentRemoval;
    private JLabel lblDependentProfessionDieSize;
    private JSpinner spnDependentProfessionDieSize;
    private JLabel lblCivilianProfessionDieSize;
    private JSpinner spnCivilianProfessionDieSize;
    //end Prisoners and Dependents Tab
    //end Salaries Tab

    /**
     * Constructs the {@code PersonnelTab} object with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for initializing and managing personnel
     *                        options.
     */
    public PersonnelTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes all tabs and their components within the PersonnelTab.
     */
    private void initialize() {
        initializeGeneralTab();
        initializePersonnelLogsTab();
        initializePersonnelInformationTab();
        initializeAwardsTab();
        initializeMedicalTab();
        initializePrisonersAndDependentsTab();
    }

    /**
     * Initializes the components of the Prisoners and Dependents Tab. This includes settings related to prisoners and
     * handling of dependents.
     */
    private void initializePrisonersAndDependentsTab() {
        prisonerPanel = new JPanel();
        lblPrisonerCaptureStyle = new JLabel();
        comboPrisonerCaptureStyle = new MMComboBox<>("comboPrisonerCaptureStyle", PrisonerCaptureStyle.values());
        chkResetTemporaryPrisonerCapacity = new JCheckBox();
        chkUseFunctionalEscapeArtist = new JCheckBox();

        dependentsPanel = new JPanel();
        chkUseRandomDependentAddition = new JCheckBox();
        chkUseRandomDependentRemoval = new JCheckBox();
        lblDependentProfessionDieSize = new JLabel();
        spnDependentProfessionDieSize = new JSpinner();
        lblCivilianProfessionDieSize = new JLabel();
        spnCivilianProfessionDieSize = new JSpinner();
    }

    /**
     * Initializes the components of the Medical Tab. This includes medical-related options such as recovery time,
     * random hits for vehicles, and limits on patients.
     */
    private void initializeMedicalTab() {
        chkUseAdvancedMedical = new JCheckBox();

        lblHealWaitingPeriod = new JLabel();
        spnHealWaitingPeriod = new JSpinner();

        lblNaturalHealWaitingPeriod = new JLabel();
        spnNaturalHealWaitingPeriod = new JSpinner();

        lblMinimumHitsForVehicles = new JLabel();
        spnMinimumHitsForVehicles = new JSpinner();

        chkUseRandomHitsForVehicles = new JCheckBox();
        chkUseTougherHealing = new JCheckBox();
        chkUseAlternativeAdvancedMedical = new JCheckBox();
        chkUseKinderAlternativeAdvancedMedical = new JCheckBox();
        chkUseRandomDiseases = new JCheckBox();

        lblMaximumPatients = new JLabel();
        spnMaximumPatients = new JSpinner();
        chkDoctorsUseAdministration = new JCheckBox();
        chkUseUsefulMedics = new JCheckBox();
        chkUseMASHTheatres = new JCheckBox();
        lblMASHTheatreCapacity = new JLabel();
        spnMASHTheatreCapacity = new JSpinner();
    }

    /**
     * Initializes the components of the Awards Tab. This includes settings for managing awards, such as automatic
     * awards issuance, tier configurations, and award filters.
     */
    private void initializeAwardsTab() {
        pnlAwardsGeneralOptions = new JPanel();
        lblAwardBonusStyle = new JLabel();
        comboAwardBonusStyle = new MMComboBox<>("comboAwardBonusStyle", AwardBonus.values());

        lblAwardTierSize = new JLabel();
        spnAwardTierSize = new JSpinner();
        chkEnableAutoAwards = new JCheckBox();
        chkIssuePosthumousAwards = new JCheckBox();
        chkIssueBestAwardOnly = new JCheckBox();
        chkIgnoreStandardSet = new JCheckBox();
        chkEnableContractAwards = new JCheckBox();
        chkEnableFactionHunterAwards = new JCheckBox();
        chkEnableInjuryAwards = new JCheckBox();
        chkEnableIndividualKillAwards = new JCheckBox();
        chkEnableFormationKillAwards = new JCheckBox();
        chkEnableRankAwards = new JCheckBox();
        chkEnableScenarioAwards = new JCheckBox();
        chkEnableSkillAwards = new JCheckBox();
        chkEnableTheatreOfWarAwards = new JCheckBox();
        chkEnableTimeAwards = new JCheckBox();
        chkEnableTrainingAwards = new JCheckBox();
        chkEnableMiscAwards = new JCheckBox();

        pnlAutoAwardsFilter = new JPanel();
        txtAwardSetFilterList = new JTextArea();
    }

    /**
     * Initializes the components of the Personnel Information Tab. This includes settings for tracking and displaying
     * information like service time, rank time, and earnings.
     */
    private void initializePersonnelInformationTab() {
        chkUseTimeInService = new JCheckBox();

        lblTimeInServiceDisplayFormat = new JLabel();
        comboTimeInServiceDisplayFormat = new MMComboBox<>("comboTimeInServiceDisplayFormat",
              TimeInDisplayFormat.values());

        chkUseTimeInRank = new JCheckBox();

        lblTimeInRankDisplayFormat = new JLabel();
        comboTimeInRankDisplayFormat = new MMComboBox<>("comboTimeInRankDisplayFormat", TimeInDisplayFormat.values());

        chkTrackTotalEarnings = new JCheckBox();
        chkTrackTotalXPEarnings = new JCheckBox();
        chkShowOriginFaction = new JCheckBox();
    }

    /**
     * Initializes the components of the Personnel Logs Tab. This includes options for personnel log-keeping, such as
     * tracking skill and ability gains, as well as transfers.
     */
    private void initializePersonnelLogsTab() {
        chkUseTransfers = new JCheckBox();
        chkUseExtendedTOEForceName = new JCheckBox();
        chkPersonnelLogSkillGain = new JCheckBox();
        chkPersonnelLogAbilityGain = new JCheckBox();
        chkPersonnelLogEdgeGain = new JCheckBox();
        chkDisplayPersonnelLog = new JCheckBox();
        chkDisplayScenarioLog = new JCheckBox();
        chkDisplayKillRecord = new JCheckBox();
        chkDisplayMedicalRecord = new JCheckBox();
        chkDisplayPatientRecord = new JCheckBox();
        chkDisplayAssignmentRecord = new JCheckBox();
        chkDisplayPerformanceRecord = new JCheckBox();
    }

    /**
     * Initializes the components of the General Tab. This includes general personnel-related options, such as tactics,
     * edge, initiative bonuses, and personnel cleanup settings.
     */
    private void initializeGeneralTab() {
        pnlPersonnelGeneralOptions = new JPanel();
        chkUseTactics = new JCheckBox();
        chkUseInitiativeBonus = new JCheckBox();
        chkUseToughness = new JCheckBox();
        chkUseRandomToughness = new JCheckBox();
        chkUseArtillery = new JCheckBox();
        chkUseAbilities = new JCheckBox();
        chkOnlyCommandersMatterVehicles = new JCheckBox();
        chkOnlyCommandersMatterInfantry = new JCheckBox();
        chkOnlyCommandersMatterBattleArmor = new JCheckBox();
        chkUseEdge = new JCheckBox();
        chkUseSupportEdge = new JCheckBox();
        chkUseImplants = new JCheckBox();
        chkUseAlternativeQualityAveraging = new JCheckBox();

        pnlPersonnelCleanup = new JPanel();
        chkUsePersonnelRemoval = new JCheckBox();
        chkUseRemovalExemptCemetery = new JCheckBox();
        chkUseRemovalExemptRetirees = new JCheckBox();

        pnlAdministrators = new JPanel();
        chkAdminsHaveNegotiation = new JCheckBox();
        chkAdminExperienceLevelIncludeNegotiation = new JCheckBox();
    }

    /**
     * Creates the components and layout for the General Tab, organizing personnel management settings into specific
     * groups.
     *
     * @return a {@link JPanel} representing the General Tab.
     */
    public JPanel createGeneralTab() {
        // Header
        generalHeader = new CampaignOptionsHeaderPanel("PersonnelGeneralTab",
              getImageDirectory() + "logo_clan_wolverine.png",
              5);

        // Contents
        pnlPersonnelGeneralOptions = createGeneralOptionsPanel();
        pnlPersonnelCleanup = createPersonnelCleanUpPanel();
        pnlAdministrators = createAdministratorsPanel();

        // Layout the Panels
        final JPanel panelRight = new CampaignOptionsStandardPanel("RightPanel");
        GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridwidth = 1;
        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        panelRight.add(pnlPersonnelCleanup, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlAdministrators, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("PersonnelGeneralTab", true);
        GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(generalHeader, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(pnlPersonnelGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "PersonnelGeneralTab");
    }

    /**
     * Creates the panel for general personnel options in the General Tab.
     *
     * @return a {@link JPanel} containing checkboxes for various personnel management settings.
     */
    private JPanel createGeneralOptionsPanel() {
        // Contents
        chkUseTactics = new CampaignOptionsCheckBox("UseTactics");
        chkUseTactics.addMouseListener(createTipPanelUpdater(generalHeader, "UseTactics"));
        chkUseInitiativeBonus = new CampaignOptionsCheckBox("UseInitiativeBonus");
        chkUseInitiativeBonus.addMouseListener(createTipPanelUpdater(generalHeader, "UseInitiativeBonus"));
        chkUseToughness = new CampaignOptionsCheckBox("UseToughness");
        chkUseToughness.addMouseListener(createTipPanelUpdater(generalHeader, "UseToughness"));
        chkUseRandomToughness = new CampaignOptionsCheckBox("UseRandomToughness");
        chkUseRandomToughness.addMouseListener(createTipPanelUpdater(generalHeader, "UseRandomToughness"));
        chkUseArtillery = new CampaignOptionsCheckBox("UseArtillery");
        chkUseArtillery.addMouseListener(createTipPanelUpdater(generalHeader, "UseArtillery"));
        chkUseAbilities = new CampaignOptionsCheckBox("UseAbilities");
        chkUseAbilities.addMouseListener(createTipPanelUpdater(generalHeader, "UseAbilities"));
        chkOnlyCommandersMatterVehicles = new CampaignOptionsCheckBox("OnlyCommandersMatterVehicles");
        chkOnlyCommandersMatterVehicles.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterVehicles"));
        chkOnlyCommandersMatterInfantry = new CampaignOptionsCheckBox("OnlyCommandersMatterInfantry");
        chkOnlyCommandersMatterInfantry.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterInfantry"));
        chkOnlyCommandersMatterBattleArmor = new CampaignOptionsCheckBox("OnlyCommandersMatterBattleArmor");
        chkOnlyCommandersMatterBattleArmor.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterBattleArmor"));
        chkUseEdge = new CampaignOptionsCheckBox("UseEdge");
        chkUseEdge.addMouseListener(createTipPanelUpdater(generalHeader, "UseEdge"));
        chkUseSupportEdge = new CampaignOptionsCheckBox("UseSupportEdge");
        chkUseSupportEdge.addMouseListener(createTipPanelUpdater(generalHeader, "UseSupportEdge"));
        chkUseImplants = new CampaignOptionsCheckBox("UseImplants");
        chkUseImplants.addMouseListener(createTipPanelUpdater(generalHeader, "UseImplants"));
        chkUseAlternativeQualityAveraging = new CampaignOptionsCheckBox("UseAlternativeQualityAveraging");
        chkUseAlternativeQualityAveraging.addMouseListener(createTipPanelUpdater(generalHeader,
              "UseAlternativeQualityAveraging"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelGeneralTab");
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUseTactics, layout);

        layout.gridy++;
        panel.add(chkUseInitiativeBonus, layout);

        layout.gridy++;
        panel.add(chkUseToughness, layout);

        layout.gridy++;
        panel.add(chkUseRandomToughness, layout);

        layout.gridy++;
        panel.add(chkUseArtillery, layout);

        layout.gridy++;
        panel.add(chkUseAbilities, layout);

        layout.gridy++;
        panel.add(chkOnlyCommandersMatterVehicles, layout);

        layout.gridy++;
        panel.add(chkOnlyCommandersMatterInfantry, layout);

        layout.gridy++;
        panel.add(chkOnlyCommandersMatterBattleArmor, layout);

        layout.gridy++;
        panel.add(chkUseEdge, layout);

        layout.gridy++;
        panel.add(chkUseSupportEdge, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseImplants, layout);

        layout.gridy++;
        panel.add(chkUseImplants, layout);

        layout.gridy++;
        panel.add(chkUseAlternativeQualityAveraging, layout);

        return panel;
    }

    /**
     * Creates the panel for personnel cleanup options in the General Tab.
     *
     * @return a {@link JPanel} containing options for personnel cleanup, such as removal exemptions.
     */
    private JPanel createPersonnelCleanUpPanel() {
        // Contents
        chkUsePersonnelRemoval = new CampaignOptionsCheckBox("UsePersonnelRemoval");
        chkUsePersonnelRemoval.addMouseListener(createTipPanelUpdater(generalHeader, "UsePersonnelRemoval"));
        chkUseRemovalExemptCemetery = new CampaignOptionsCheckBox("UseRemovalExemptCemetery");
        chkUseRemovalExemptCemetery.addMouseListener(createTipPanelUpdater(generalHeader, "UseRemovalExemptCemetery"));
        chkUseRemovalExemptRetirees = new CampaignOptionsCheckBox("UseRemovalExemptRetirees");
        chkUseRemovalExemptRetirees.addMouseListener(createTipPanelUpdater(generalHeader, "UseRemovalExemptRetirees"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelCleanUpPanel", true, "PersonnelCleanUpPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel, null, GridBagConstraints.NONE);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUsePersonnelRemoval, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseRemovalExemptCemetery, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseRemovalExemptRetirees, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        return panel;
    }

    /**
     * Creates the panel for administrative settings in the General Tab.
     *
     * @return a {@link JPanel} containing settings related to administrators, such as negotiation options.
     */
    private JPanel createAdministratorsPanel() {
        // Contents
        chkAdminsHaveNegotiation = new CampaignOptionsCheckBox("AdminsHaveNegotiation");
        chkAdminsHaveNegotiation.addMouseListener(createTipPanelUpdater(generalHeader, "AdminsHaveNegotiation"));
        chkAdminExperienceLevelIncludeNegotiation = new CampaignOptionsCheckBox("AdminExperienceLevelIncludeNegotiation");
        chkAdminExperienceLevelIncludeNegotiation.addMouseListener(createTipPanelUpdater(generalHeader,
              "AdminExperienceLevelIncludeNegotiation"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministratorsPanel", true, "AdministratorsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkAdminsHaveNegotiation, layout);

        layout.gridy++;
        panel.add(chkAdminExperienceLevelIncludeNegotiation, layout);

        return panel;
    }

    /**
     * Creates the panels and layout for the Awards Tab, including its general and filter components.
     *
     * @return a {@link JPanel} representing the Awards Tab.
     */
    public JPanel createAwardsTab() {
        // Header
        awardsHeader = new CampaignOptionsHeaderPanel("AwardsTab",
              getImageDirectory() + "logo_outworld_alliance.png",
              4);

        // Contents
        pnlAwardsGeneralOptions = createAwardsGeneralOptionsPanel();
        pnlAutoAwardsFilter = createAutoAwardsFilterPanel();

        txtAwardSetFilterList = new JTextArea(10, 60);
        txtAwardSetFilterList.setLineWrap(true);
        txtAwardSetFilterList.setWrapStyleWord(true);
        txtAwardSetFilterList.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardSetFilterList"));
        txtAwardSetFilterList.setToolTipText(wordWrap(getTextAt(getCampaignOptionsResourceBundle(),
              "lblAwardSetFilterList.tooltip")));
        txtAwardSetFilterList.setName("txtAwardSetFilterList");
        txtAwardSetFilterList.setText("");
        JScrollPane scrollAwardSetFilterList = new JScrollPane(txtAwardSetFilterList);
        scrollAwardSetFilterList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollAwardSetFilterList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Layout the Panel
        final JPanel panelRight = new CampaignOptionsStandardPanel("AwardsTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridy = 0;
        layoutRight.gridwidth = 1;
        layoutRight.gridy++;
        panelRight.add(pnlAutoAwardsFilter, layoutRight);

        final JPanel panelBottom = new CampaignOptionsStandardPanel("AwardsTabBottom", true, "AwardsTabBottom");
        final GridBagConstraints layoutBottom = new CampaignOptionsGridBagConstraints(panelBottom,
              null,
              GridBagConstraints.HORIZONTAL);

        layoutBottom.gridx = 0;
        layoutBottom.gridy++;
        panelBottom.add(txtAwardSetFilterList, layoutBottom);

        final JPanel panelParent = new CampaignOptionsStandardPanel("AwardsTabRight", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(awardsHeader, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(pnlAwardsGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 2;
        panelParent.add(panelBottom, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "AwardsTab");
    }

    /**
     * Creates the panel for general award configuration settings in the Awards Tab.
     *
     * @return a {@link JPanel} containing settings for awards, such as bonus style and auto awards.
     */
    JPanel createAwardsGeneralOptionsPanel() {
        // Contents
        lblAwardBonusStyle = new CampaignOptionsLabel("AwardBonusStyle");
        lblAwardBonusStyle.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardBonusStyle"));
        comboAwardBonusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AwardBonus) {
                    list.setToolTipText(((AwardBonus) value).getToolTipText());
                }
                return this;
            }
        });
        comboAwardBonusStyle.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardBonusStyle"));

        lblAwardTierSize = new CampaignOptionsLabel("AwardTierSize");
        lblAwardTierSize.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardTierSize"));
        spnAwardTierSize = new CampaignOptionsSpinner("AwardTierSize", 5, 1, 100, 1);
        spnAwardTierSize.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardTierSize"));

        chkEnableAutoAwards = new CampaignOptionsCheckBox("EnableAutoAwards");
        chkEnableAutoAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableAutoAwards"));

        chkIssuePosthumousAwards = new CampaignOptionsCheckBox("IssuePosthumousAwards");
        chkIssuePosthumousAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "IssuePosthumousAwards"));

        chkIssueBestAwardOnly = new CampaignOptionsCheckBox("IssueBestAwardOnly");
        chkIssueBestAwardOnly.addMouseListener(createTipPanelUpdater(awardsHeader, "IssueBestAwardOnly"));

        chkIgnoreStandardSet = new CampaignOptionsCheckBox("IgnoreStandardSet");
        chkIgnoreStandardSet.addMouseListener(createTipPanelUpdater(awardsHeader, "IgnoreStandardSet"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AwardsGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAwardBonusStyle, layout);
        layout.gridx++;
        panel.add(comboAwardBonusStyle, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAwardTierSize, layout);
        layout.gridx++;
        panel.add(spnAwardTierSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkEnableAutoAwards, layout);

        layout.gridy++;
        panel.add(chkIssuePosthumousAwards, layout);

        layout.gridy++;
        panel.add(chkIssueBestAwardOnly, layout);

        layout.gridy++;
        panel.add(chkIgnoreStandardSet, layout);

        return panel;
    }

    /**
     * Creates the panel for filtering auto-awards settings in the Awards Tab.
     *
     * @return a {@link JPanel} containing checkboxes for various award filters.
     */
    private JPanel createAutoAwardsFilterPanel() {
        // Contents
        chkEnableContractAwards = new CampaignOptionsCheckBox("EnableContractAwards");
        chkEnableContractAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableContractAwards"));
        chkEnableFactionHunterAwards = new CampaignOptionsCheckBox("EnableFactionHunterAwards");
        chkEnableFactionHunterAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableFactionHunterAwards"));
        chkEnableInjuryAwards = new CampaignOptionsCheckBox("EnableInjuryAwards");
        chkEnableInjuryAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableInjuryAwards"));
        chkEnableIndividualKillAwards = new CampaignOptionsCheckBox("EnableIndividualKillAwards");
        chkEnableIndividualKillAwards.addMouseListener(createTipPanelUpdater(awardsHeader,
              "EnableIndividualKillAwards"));
        chkEnableFormationKillAwards = new CampaignOptionsCheckBox("EnableFormationKillAwards");
        chkEnableFormationKillAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableFormationKillAwards"));
        chkEnableRankAwards = new CampaignOptionsCheckBox("EnableRankAwards");
        chkEnableRankAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableRankAwards"));
        chkEnableScenarioAwards = new CampaignOptionsCheckBox("EnableScenarioAwards");
        chkEnableScenarioAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableScenarioAwards"));
        chkEnableSkillAwards = new CampaignOptionsCheckBox("EnableSkillAwards");
        chkEnableSkillAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableSkillAwards"));
        chkEnableTheatreOfWarAwards = new CampaignOptionsCheckBox("EnableTheatreOfWarAwards");
        chkEnableTheatreOfWarAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTheatreOfWarAwards"));
        chkEnableTimeAwards = new CampaignOptionsCheckBox("EnableTimeAwards");
        chkEnableTimeAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTimeAwards"));
        chkEnableTrainingAwards = new CampaignOptionsCheckBox("EnableTrainingAwards");
        chkEnableTrainingAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTrainingAwards"));
        chkEnableMiscAwards = new CampaignOptionsCheckBox("EnableMiscAwards");
        chkEnableMiscAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableMiscAwards"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AutoAwardsFilterPanel", true, "AutoAwardsFilterPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkEnableContractAwards, layout);
        layout.gridx++;
        panel.add(chkEnableFactionHunterAwards, layout);
        layout.gridx++;
        panel.add(chkEnableInjuryAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableIndividualKillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableFormationKillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableRankAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableScenarioAwards, layout);
        layout.gridx++;
        panel.add(chkEnableSkillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableTheatreOfWarAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableTimeAwards, layout);
        layout.gridx++;
        panel.add(chkEnableTrainingAwards, layout);
        layout.gridx++;
        panel.add(chkEnableMiscAwards, layout);

        return panel;
    }

    /**
     * Creates the layout for the Medical Tab, combining components related to medical settings.
     *
     * @return a {@link JPanel} containing medical-related settings.
     */
    public JPanel createMedicalTab() {
        // Header
        //start Medical Tab
        CampaignOptionsHeaderPanel medicalHeader = new CampaignOptionsHeaderPanel("MedicalTab",
              getImageDirectory() + "logo_duchy_of_tamarind_abbey.png",
              3);

        // Contents
        chkUseAdvancedMedical = new CampaignOptionsCheckBox("UseAdvancedMedical");
        chkUseAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader, "UseAdvancedMedical"));

        lblHealWaitingPeriod = new CampaignOptionsLabel("HealWaitingPeriod");
        lblHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "HealWaitingPeriod"));
        spnHealWaitingPeriod = new CampaignOptionsSpinner("HealWaitingPeriod", 1, 1, 30, 1);
        spnHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "HealWaitingPeriod"));

        lblNaturalHealWaitingPeriod = new CampaignOptionsLabel("NaturalHealWaitingPeriod");
        lblNaturalHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "NaturalHealWaitingPeriod"));
        spnNaturalHealWaitingPeriod = new CampaignOptionsSpinner("NaturalHealWaitingPeriod", 1, 1, 365, 1);
        spnNaturalHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "NaturalHealWaitingPeriod"));

        lblMinimumHitsForVehicles = new CampaignOptionsLabel("MinimumHitsForVehicles");
        lblMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater(medicalHeader, "MinimumHitsForVehicles"));
        spnMinimumHitsForVehicles = new CampaignOptionsSpinner("MinimumHitsForVehicles", 1, 1, 5, 1);
        spnMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater(medicalHeader, "MinimumHitsForVehicles"));

        chkUseRandomHitsForVehicles = new CampaignOptionsCheckBox("UseRandomHitsForVehicles");
        chkUseRandomHitsForVehicles.addMouseListener(createTipPanelUpdater(medicalHeader, "UseRandomHitsForVehicles"));

        chkUseTougherHealing = new CampaignOptionsCheckBox("UseTougherHealing");
        chkUseTougherHealing.addMouseListener(createTipPanelUpdater(medicalHeader, "UseTougherHealing"));

        chkUseAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseAlternativeAdvancedMedical");
        chkUseAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseAlternativeAdvancedMedical"));

        chkUseKinderAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseKinderAlternativeAdvancedMedical");
        chkUseKinderAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseKinderAlternativeAdvancedMedical"));

        chkUseRandomDiseases = new CampaignOptionsCheckBox("UseRandomDiseases");
        chkUseRandomDiseases.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseRandomDiseases"));

        lblMaximumPatients = new CampaignOptionsLabel("MaximumPatients");
        lblMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));
        spnMaximumPatients = new CampaignOptionsSpinner("MaximumPatients", 25, 1, 100, 1);
        spnMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));

        chkDoctorsUseAdministration = new CampaignOptionsCheckBox("DoctorsUseAdministration");
        chkDoctorsUseAdministration.addMouseListener(createTipPanelUpdater(medicalHeader, "DoctorsUseAdministration"));

        chkUseUsefulMedics = new CampaignOptionsCheckBox("UseUsefulMedics");
        chkUseUsefulMedics.addMouseListener(createTipPanelUpdater(medicalHeader, "UseUsefulMedics"));

        chkUseMASHTheatres = new CampaignOptionsCheckBox("UseMASHTheatres");
        chkUseMASHTheatres.addMouseListener(createTipPanelUpdater(medicalHeader, "UseMASHTheatres"));

        lblMASHTheatreCapacity = new CampaignOptionsLabel("MASHTheatreCapacity");
        lblMASHTheatreCapacity.addMouseListener(createTipPanelUpdater(medicalHeader, "MASHTheatreCapacity"));
        spnMASHTheatreCapacity = new CampaignOptionsSpinner("MASHTheatreCapacity", 25, 1, 100, 1);
        spnMASHTheatreCapacity.addMouseListener(createTipPanelUpdater(medicalHeader, "MASHTheatreCapacity"));

        final JPanel panelLeft = new CampaignOptionsStandardPanel("MedicalTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridy = 0;
        layoutLeft.gridx = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(lblMaximumPatients, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaximumPatients, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkDoctorsUseAdministration, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseUsefulMedics, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseMASHTheatres, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMASHTheatreCapacity, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMASHTheatreCapacity, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblHealWaitingPeriod, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnHealWaitingPeriod, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblNaturalHealWaitingPeriod, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnNaturalHealWaitingPeriod, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseRandomHitsForVehicles, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMinimumHitsForVehicles, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMinimumHitsForVehicles, layoutLeft);

        // Layout the Panels
        final JPanel panelRight = new CampaignOptionsStandardPanel("MedicalTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridy++;
        layoutRight.gridwidth = 1;
        panelRight.add(chkUseAdvancedMedical, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(chkUseTougherHealing, layoutRight);
        layoutRight.gridy++;
        panelRight.add(chkUseAlternativeAdvancedMedical, layoutRight);
        layoutRight.gridy++;
        panelRight.add(chkUseKinderAlternativeAdvancedMedical, layoutRight);
        layoutRight.gridy++;
        panelRight.add(chkUseRandomDiseases, layoutRight);

        // Layout the Panels
        final JPanel panelParent = new CampaignOptionsStandardPanel("MedicalTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(medicalHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "MedicalTab");
    }

    /**
     * Creates the layout for the Personnel Information Tab, including its components for displaying personnel
     * information and logs.
     *
     * @return a {@link JPanel} representing the Personnel Information Tab.
     */
    public JPanel createPersonnelInformationTab() {
        // Header
        personnelInformationHeader = new CampaignOptionsHeaderPanel("PersonnelInformation",
              getImageDirectory() + "logo_rasalhague_dominion.png",
              3);

        // Contents
        chkUseTimeInService = new CampaignOptionsCheckBox("UseTimeInService");
        chkUseTimeInService.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "UseTimeInService"));
        lblTimeInServiceDisplayFormat = new CampaignOptionsLabel("TimeInServiceDisplayFormat");
        lblTimeInServiceDisplayFormat.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "TimeInServiceDisplayFormat"));
        chkUseTimeInRank = new CampaignOptionsCheckBox("UseTimeInRank");
        chkUseTimeInRank.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "UseTimeInRank"));
        lblTimeInRankDisplayFormat = new CampaignOptionsLabel("TimeInRankDisplayFormat");
        lblTimeInRankDisplayFormat.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "TimeInRankDisplayFormat"));
        chkTrackTotalEarnings = new CampaignOptionsCheckBox("TrackTotalEarnings");
        chkTrackTotalEarnings.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "TrackTotalEarnings"));
        chkTrackTotalXPEarnings = new CampaignOptionsCheckBox("TrackTotalXPEarnings");
        chkTrackTotalXPEarnings.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "TrackTotalXPEarnings"));
        chkShowOriginFaction = new CampaignOptionsCheckBox("ShowOriginFaction");
        chkShowOriginFaction.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "ShowOriginFaction"));

        JPanel pnlPersonnelLogs = createPersonnelLogsPanel();

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("PersonnelInformationLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(chkUseTimeInService, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblTimeInServiceDisplayFormat, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(comboTimeInServiceDisplayFormat, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseTimeInRank, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblTimeInRankDisplayFormat, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(comboTimeInRankDisplayFormat, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkTrackTotalEarnings, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkTrackTotalXPEarnings, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkShowOriginFaction, layoutLeft);

        final JPanel panelParent = new CampaignOptionsStandardPanel("PersonnelInformation", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(personnelInformationHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(pnlPersonnelLogs, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "PersonnelInformation");
    }

    /**
     * Creates a sub-panel for managing personnel log settings within the Personnel Information Tab.
     *
     * @return a {@link JPanel} containing log settings for personnel activities.
     */
    JPanel createPersonnelLogsPanel() {
        // Contents
        chkUseTransfers = new CampaignOptionsCheckBox("UseTransfers");
        chkUseTransfers.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "UseTransfers"));
        chkUseExtendedTOEForceName = new CampaignOptionsCheckBox("UseExtendedTOEForceName");
        chkUseExtendedTOEForceName.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "UseExtendedTOEForceName"));
        chkPersonnelLogSkillGain = new CampaignOptionsCheckBox("PersonnelLogSkillGain");
        chkPersonnelLogSkillGain.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "PersonnelLogSkillGain"));
        chkPersonnelLogAbilityGain = new CampaignOptionsCheckBox("PersonnelLogAbilityGain");
        chkPersonnelLogAbilityGain.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "PersonnelLogAbilityGain"));
        chkPersonnelLogEdgeGain = new CampaignOptionsCheckBox("PersonnelLogEdgeGain");
        chkPersonnelLogEdgeGain.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "PersonnelLogEdgeGain"));
        chkDisplayPersonnelLog = new CampaignOptionsCheckBox("DisplayPersonnelLog");
        chkDisplayPersonnelLog.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "DisplayPersonnelLog"));
        chkDisplayScenarioLog = new CampaignOptionsCheckBox("DisplayScenarioLog");
        chkDisplayScenarioLog.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "DisplayScenarioLog"));
        chkDisplayKillRecord = new CampaignOptionsCheckBox("DisplayKillRecord");
        chkDisplayKillRecord.addMouseListener(createTipPanelUpdater(personnelInformationHeader, "DisplayKillRecord"));
        chkDisplayMedicalRecord = new CampaignOptionsCheckBox("DisplayMedicalRecord");
        chkDisplayMedicalRecord.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "DisplayMedicalRecord"));
        chkDisplayPatientRecord = new CampaignOptionsCheckBox("DisplayPatientRecord");
        chkDisplayPatientRecord.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "DisplayPatientRecord"));
        chkDisplayAssignmentRecord = new CampaignOptionsCheckBox("DisplayAssignmentRecord");
        chkDisplayAssignmentRecord.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "DisplayAssignmentRecord"));
        chkDisplayPerformanceRecord = new CampaignOptionsCheckBox("DisplayPerformanceRecord");
        chkDisplayPerformanceRecord.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
              "DisplayPerformanceRecord"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelLogsPanel", true, "PersonnelLogsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkUseTransfers, layout);

        layout.gridy++;
        panel.add(chkUseExtendedTOEForceName, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogSkillGain, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogAbilityGain, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogEdgeGain, layout);

        layout.gridy++;
        panel.add(chkDisplayPersonnelLog, layout);

        layout.gridy++;
        panel.add(chkDisplayScenarioLog, layout);

        layout.gridy++;
        panel.add(chkDisplayKillRecord, layout);

        layout.gridy++;
        panel.add(chkDisplayMedicalRecord, layout);

        layout.gridy++;
        panel.add(chkDisplayPatientRecord, layout);

        layout.gridy++;
        panel.add(chkDisplayAssignmentRecord, layout);

        layout.gridy++;
        panel.add(chkDisplayPerformanceRecord, layout);

        return panel;
    }

    /**
     * Creates the layout for the Prisoners and Dependents Tab, organizing settings for prisoner handling and dependent
     * management.
     *
     * @return a {@link JPanel} containing the Prisoners and Dependents Tab components.
     */
    public JPanel createPrisonersAndDependentsTab() {
        // Header
        prisonersAndDependentsHeader = new CampaignOptionsHeaderPanel("PrisonersAndDependentsTab",
              getImageDirectory() + "logo_illyrian_palatinate.png",
              6);

        // Contents
        prisonerPanel = createPrisonersPanel();
        dependentsPanel = createDependentsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PrisonersAndDependentsTab", true, "");
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(prisonersAndDependentsHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(prisonerPanel, layoutParent);

        layoutParent.gridx++;
        panel.add(dependentsPanel, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "PrisonersAndDependentsTab");
    }

    /**
     * Creates the panel for configuring prisoner settings in the Prisoners and Dependents Tab.
     *
     * @return a {@link JPanel} containing prisoner-related options such as capture style and status.
     */
    private JPanel createPrisonersPanel() {
        // Contents
        lblPrisonerCaptureStyle = new CampaignOptionsLabel("PrisonerCaptureStyle");
        lblPrisonerCaptureStyle.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "PrisonerCaptureStyle"));
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(wordWrap(((PrisonerCaptureStyle) value).getTooltip()));
                }
                return this;
            }
        });
        comboPrisonerCaptureStyle.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "PrisonerCaptureStyle"));

        chkResetTemporaryPrisonerCapacity = new CampaignOptionsCheckBox("ResetTemporaryPrisonerCapacity");
        chkResetTemporaryPrisonerCapacity.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "ResetTemporaryPrisonerCapacity"));

        chkUseFunctionalEscapeArtist = new CampaignOptionsCheckBox("UseFunctionalEscapeArtist");
        chkUseFunctionalEscapeArtist.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "UseFunctionalEscapeArtist"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PrisonersPanel", true, "PrisonersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblPrisonerCaptureStyle, layout);
        layout.gridx++;
        panel.add(comboPrisonerCaptureStyle, layout);

        layout.gridy++;
        layout.gridx = 0;
        layout.gridwidth = 2;
        panel.add(chkUseFunctionalEscapeArtist, layout);

        layout.gridy++;
        layout.gridx = 0;
        layout.gridwidth = 2;
        panel.add(chkResetTemporaryPrisonerCapacity, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring dependent settings in the Prisoners and Dependents Tab.
     *
     * @return a {@link JPanel} containing dependent management options.
     */
    private JPanel createDependentsPanel() {
        // Contents
        chkUseRandomDependentAddition = new CampaignOptionsCheckBox("UseRandomDependentAddition");
        chkUseRandomDependentAddition.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "UseRandomDependentAddition"));

        chkUseRandomDependentRemoval = new CampaignOptionsCheckBox("UseRandomDependentRemoval");
        chkUseRandomDependentRemoval.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "UseRandomDependentRemoval"));

        lblDependentProfessionDieSize = new CampaignOptionsLabel("DependentProfessionDieSize");
        lblDependentProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "DependentProfessionDieSize"));
        spnDependentProfessionDieSize = new CampaignOptionsSpinner("DependentProfessionDieSize",
              4, 0, 100, 1);
        spnDependentProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "DependentProfessionDieSize"));

        lblCivilianProfessionDieSize = new CampaignOptionsLabel("CivilianProfessionDieSize");
        lblCivilianProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "CivilianProfessionDieSize"));
        spnCivilianProfessionDieSize = new CampaignOptionsSpinner("CivilianProfessionDieSize",
              2, 0, 100, 1);
        spnCivilianProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "CivilianProfessionDieSize"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("DependentsPanel", true, "DependentsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseRandomDependentAddition, layout);

        layout.gridy++;
        panel.add(chkUseRandomDependentRemoval, layout);

        layout.gridy++;
        panel.add(lblDependentProfessionDieSize, layout);
        layout.gridx++;
        panel.add(spnDependentProfessionDieSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblCivilianProfessionDieSize, layout);
        layout.gridx++;
        panel.add(spnCivilianProfessionDieSize, layout);

        return panel;
    }

    /**
     * @deprecated This content has been moved to its own tab
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public JPanel createSalariesTab() {
        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalariesTab", true);

        // Create Parent Panel and return
        return createParentPanel(panel, "SalariesTab");
    }

    /**
     * @deprecated use {@link #loadValuesFromCampaignOptions(Version)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, new Version());
    }

    /**
     * Shortcut method to load default {@link CampaignOptions} values into the tab components.
     */
    public void loadValuesFromCampaignOptions(Version version) {
        loadValuesFromCampaignOptions(null, version);
    }

    /**
     * @deprecated use {@link #loadValuesFromCampaignOptions(CampaignOptions, Version)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        loadValuesFromCampaignOptions(presetCampaignOptions, new Version());
    }

    /**
     * Loads and applies configuration values from the provided {@link CampaignOptions} object, or uses the default
     * campaign options if none are provided. The configuration includes general settings, personnel logs, personnel
     * information, awards, medical settings, prisoner and dependent settings, and salary-related options. It also
     * adjusts certain values based on the version of the application.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} object to load settings from. If null, default campaign
     *                              options will be used.
     * @param version               the version of the application, used to determine adjustments for compatibility.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions, Version version) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General
        chkUseTactics.setSelected(options.isUseTactics());
        chkUseInitiativeBonus.setSelected(options.isUseInitiativeBonus());
        chkUseToughness.setSelected(options.isUseToughness());
        chkUseRandomToughness.setSelected(options.isUseRandomToughness());
        chkUseArtillery.setSelected(options.isUseArtillery());
        chkUseAbilities.setSelected(options.isUseAbilities());
        chkOnlyCommandersMatterVehicles.setSelected(options.isOnlyCommandersMatterVehicles());
        chkOnlyCommandersMatterInfantry.setSelected(options.isOnlyCommandersMatterInfantry());
        chkOnlyCommandersMatterBattleArmor.setSelected(options.isOnlyCommandersMatterBattleArmor());
        chkUseEdge.setSelected(options.isUseEdge());
        chkUseSupportEdge.setSelected(options.isUseSupportEdge());
        chkUseImplants.setSelected(options.isUseImplants());
        chkUseAlternativeQualityAveraging.setSelected(options.isAlternativeQualityAveraging());
        chkUsePersonnelRemoval.setSelected(options.isUsePersonnelRemoval());
        chkUseRemovalExemptCemetery.setSelected(options.isUseRemovalExemptCemetery());
        chkUseRemovalExemptRetirees.setSelected(options.isUseRemovalExemptRetirees());
        chkAdminsHaveNegotiation.setSelected(options.isAdminsHaveNegotiation());
        chkAdminExperienceLevelIncludeNegotiation.setSelected(options.isAdminExperienceLevelIncludeNegotiation());

        // Personnel Log
        chkUseTransfers.setSelected(options.isUseTransfers());
        chkUseExtendedTOEForceName.setSelected(options.isUseExtendedTOEForceName());
        chkPersonnelLogSkillGain.setSelected(options.isPersonnelLogSkillGain());
        chkPersonnelLogAbilityGain.setSelected(options.isPersonnelLogAbilityGain());
        chkPersonnelLogEdgeGain.setSelected(options.isPersonnelLogEdgeGain());
        chkDisplayPersonnelLog.setSelected(options.isDisplayPersonnelLog());
        chkDisplayScenarioLog.setSelected(options.isDisplayScenarioLog());
        chkDisplayKillRecord.setSelected(options.isDisplayKillRecord());
        chkDisplayMedicalRecord.setSelected(options.isDisplayMedicalRecord());
        chkDisplayPatientRecord.setSelected(options.isDisplayPatientRecord());
        chkDisplayAssignmentRecord.setSelected(options.isDisplayAssignmentRecord());
        chkDisplayPerformanceRecord.setSelected(options.isDisplayPerformanceRecord());

        // Personnel Information
        chkUseTimeInService.setSelected(options.isUseTimeInService());
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        chkUseTimeInRank.setSelected(options.isUseTimeInRank());
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        chkTrackTotalEarnings.setSelected(options.isTrackTotalEarnings());
        chkTrackTotalXPEarnings.setSelected(options.isTrackTotalXPEarnings());
        chkShowOriginFaction.setSelected(options.isShowOriginFaction());

        // Awards
        comboAwardBonusStyle.setSelectedItem(options.getAwardBonusStyle());
        spnAwardTierSize.setValue(options.getAwardTierSize());
        chkEnableAutoAwards.setSelected(options.isEnableAutoAwards());
        chkIssuePosthumousAwards.setSelected(options.isIssuePosthumousAwards());
        chkIssueBestAwardOnly.setSelected(options.isIssueBestAwardOnly());
        chkIgnoreStandardSet.setSelected(options.isIgnoreStandardSet());
        chkEnableContractAwards.setSelected(options.isEnableContractAwards());
        chkEnableFactionHunterAwards.setSelected(options.isEnableFactionHunterAwards());
        chkEnableInjuryAwards.setSelected(options.isEnableInjuryAwards());
        chkEnableIndividualKillAwards.setSelected(options.isEnableIndividualKillAwards());
        chkEnableFormationKillAwards.setSelected(options.isEnableFormationKillAwards());
        chkEnableRankAwards.setSelected(options.isEnableRankAwards());
        chkEnableScenarioAwards.setSelected(options.isEnableScenarioAwards());
        chkEnableSkillAwards.setSelected(options.isEnableSkillAwards());
        chkEnableTheatreOfWarAwards.setSelected(options.isEnableTheatreOfWarAwards());
        chkEnableTimeAwards.setSelected(options.isEnableTimeAwards());
        chkEnableTrainingAwards.setSelected(options.isEnableTrainingAwards());
        chkEnableMiscAwards.setSelected(options.isEnableMiscAwards());
        txtAwardSetFilterList.setText(options.getAwardSetFilterList());

        // Medical
        chkUseAdvancedMedical.setSelected(options.isUseAdvancedMedicalDirect());
        spnHealWaitingPeriod.setValue(options.getHealingWaitingPeriod());
        spnNaturalHealWaitingPeriod.setValue(options.getNaturalHealingWaitingPeriod());
        spnMinimumHitsForVehicles.setValue(options.getMinimumHitsForVehicles());
        chkUseRandomHitsForVehicles.setSelected(options.isUseRandomHitsForVehicles());
        chkUseTougherHealing.setSelected(options.isTougherHealing());
        chkUseAlternativeAdvancedMedical.setSelected(options.isUseAlternativeAdvancedMedical());
        chkUseKinderAlternativeAdvancedMedical.setSelected(options.isUseKinderAlternativeAdvancedMedical());
        chkUseRandomDiseases.setSelected(options.isUseRandomDiseases());
        spnMaximumPatients.setValue(options.getMaximumPatients());
        chkDoctorsUseAdministration.setSelected(options.isDoctorsUseAdministration());
        chkUseUsefulMedics.setSelected(options.isUseUsefulMedics());
        chkUseMASHTheatres.setSelected(options.isUseMASHTheatres());
        spnMASHTheatreCapacity.setValue(options.getMASHTheatreCapacity());

        // Prisoners and Dependents
        comboPrisonerCaptureStyle.setSelectedItem(options.getPrisonerCaptureStyle());
        chkUseFunctionalEscapeArtist.setSelected(options.isUseFunctionalEscapeArtist());
        chkUseRandomDependentAddition.setSelected(options.isUseRandomDependentAddition());
        chkUseRandomDependentRemoval.setSelected(options.isUseRandomDependentRemoval());
        spnDependentProfessionDieSize.setValue(options.getDependentProfessionDieSize());
        spnCivilianProfessionDieSize.setValue(options.getCivilianProfessionDieSize());
    }

    /**
     * Applies the modified personnel tab settings to the repository's campaign options. If no preset
     * {@link CampaignOptions} is provided, the changes are applied to the current options.
     *
     * @param campaign              the {@link Campaign} object, representing the current campaign state.
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply changes to.
     */
    public void applyCampaignOptionsToCampaign(Campaign campaign, @Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General
        options.setUseTactics(chkUseTactics.isSelected());
        options.setUseInitiativeBonus(chkUseInitiativeBonus.isSelected());
        options.setUseToughness(chkUseToughness.isSelected());
        options.setUseRandomToughness(chkUseRandomToughness.isSelected());
        options.setUseArtillery(chkUseArtillery.isSelected());
        options.setUseAbilities(chkUseAbilities.isSelected());
        options.setOnlyCommandersMatterVehicles(chkOnlyCommandersMatterVehicles.isSelected());
        options.setOnlyCommandersMatterInfantry(chkOnlyCommandersMatterInfantry.isSelected());
        options.setOnlyCommandersMatterBattleArmor(chkOnlyCommandersMatterBattleArmor.isSelected());
        options.setUseEdge(chkUseEdge.isSelected());
        options.setUseSupportEdge(chkUseSupportEdge.isSelected());
        options.setUseImplants(chkUseImplants.isSelected());
        options.setAlternativeQualityAveraging(chkUseAlternativeQualityAveraging.isSelected());
        options.setUsePersonnelRemoval(chkUsePersonnelRemoval.isSelected());
        options.setUseRemovalExemptCemetery(chkUseRemovalExemptCemetery.isSelected());
        options.setUseRemovalExemptRetirees(chkUseRemovalExemptRetirees.isSelected());
        options.setAdminsHaveNegotiation(chkAdminsHaveNegotiation.isSelected());
        options.setAdminExperienceLevelIncludeNegotiation(chkAdminExperienceLevelIncludeNegotiation.isSelected());

        // Personnel Log
        options.setUseTransfers(chkUseTransfers.isSelected());
        options.setUseExtendedTOEForceName(chkUseExtendedTOEForceName.isSelected());
        options.setPersonnelLogSkillGain(chkPersonnelLogSkillGain.isSelected());
        options.setPersonnelLogAbilityGain(chkPersonnelLogAbilityGain.isSelected());
        options.setPersonnelLogEdgeGain(chkPersonnelLogEdgeGain.isSelected());
        options.setDisplayPersonnelLog(chkDisplayPersonnelLog.isSelected());
        options.setDisplayScenarioLog(chkDisplayScenarioLog.isSelected());
        options.setDisplayKillRecord(chkDisplayKillRecord.isSelected());
        options.setDisplayMedicalRecord(chkDisplayMedicalRecord.isSelected());
        options.setDisplayPatientRecord(chkDisplayPatientRecord.isSelected());
        options.setDisplayAssignmentRecord(chkDisplayAssignmentRecord.isSelected());
        options.setDisplayPerformanceRecord(chkDisplayPerformanceRecord.isSelected());

        // Personnel Information
        options.setUseTimeInService(chkUseTimeInService.isSelected());
        options.setTimeInServiceDisplayFormat(comboTimeInServiceDisplayFormat.getSelectedItem());
        options.setUseTimeInRank(chkUseTimeInRank.isSelected());
        options.setTimeInRankDisplayFormat(comboTimeInRankDisplayFormat.getSelectedItem());
        options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
        options.setTrackTotalXPEarnings(chkTrackTotalXPEarnings.isSelected());
        options.setShowOriginFaction(chkShowOriginFaction.isSelected());

        // Awards
        options.setAwardBonusStyle(comboAwardBonusStyle.getSelectedItem());
        options.setAwardTierSize((int) spnAwardTierSize.getValue());
        options.setEnableAutoAwards(chkEnableAutoAwards.isSelected());
        options.setIssuePosthumousAwards(chkIssuePosthumousAwards.isSelected());
        options.setIssueBestAwardOnly(chkIssueBestAwardOnly.isSelected());
        options.setIgnoreStandardSet(chkIgnoreStandardSet.isSelected());
        options.setEnableContractAwards(chkEnableContractAwards.isSelected());
        options.setEnableFactionHunterAwards(chkEnableFactionHunterAwards.isSelected());
        options.setEnableInjuryAwards(chkEnableInjuryAwards.isSelected());
        options.setEnableIndividualKillAwards(chkEnableIndividualKillAwards.isSelected());
        options.setEnableFormationKillAwards(chkEnableFormationKillAwards.isSelected());
        options.setEnableRankAwards(chkEnableRankAwards.isSelected());
        options.setEnableScenarioAwards(chkEnableScenarioAwards.isSelected());
        options.setEnableSkillAwards(chkEnableSkillAwards.isSelected());
        options.setEnableTheatreOfWarAwards(chkEnableTheatreOfWarAwards.isSelected());
        options.setEnableTimeAwards(chkEnableTimeAwards.isSelected());
        options.setEnableTrainingAwards(chkEnableTrainingAwards.isSelected());
        options.setEnableMiscAwards(chkEnableMiscAwards.isSelected());
        options.setAwardSetFilterList(txtAwardSetFilterList.getText());

        // Medical
        options.setUseAdvancedMedical(chkUseAdvancedMedical.isSelected());
        options.setHealingWaitingPeriod((int) spnHealWaitingPeriod.getValue());
        options.setNaturalHealingWaitingPeriod((int) spnNaturalHealWaitingPeriod.getValue());
        options.setMinimumHitsForVehicles((int) spnMinimumHitsForVehicles.getValue());
        options.setUseRandomHitsForVehicles(chkUseRandomHitsForVehicles.isSelected());
        options.setTougherHealing(chkUseTougherHealing.isSelected());
        options.setUseAlternativeAdvancedMedical(chkUseAlternativeAdvancedMedical.isSelected());
        options.setUseKinderAlternativeAdvancedMedical(chkUseKinderAlternativeAdvancedMedical.isSelected());
        options.setUseRandomDiseases(chkUseRandomDiseases.isSelected());
        options.setMaximumPatients((int) spnMaximumPatients.getValue());
        options.setDoctorsUseAdministration(chkDoctorsUseAdministration.isSelected());
        options.setIsUseUsefulMedics(chkUseUsefulMedics.isSelected());
        options.setIsUseMASHTheatres(chkUseMASHTheatres.isSelected());
        options.setMASHTheatreCapacity((int) spnMASHTheatreCapacity.getValue());

        // Prisoners and Dependents
        options.setPrisonerCaptureStyle(comboPrisonerCaptureStyle.getSelectedItem());
        options.setUseFunctionalEscapeArtist(chkUseFunctionalEscapeArtist.isSelected());
        if (chkResetTemporaryPrisonerCapacity.isSelected()) {
            campaign.setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }
        options.setUseRandomDependentAddition(chkUseRandomDependentAddition.isSelected());
        options.setUseRandomDependentRemoval(chkUseRandomDependentRemoval.isSelected());
        options.setDependentProfessionDieSize((int) spnDependentProfessionDieSize.getValue());
        options.setCivilianProfessionDieSize((int) spnCivilianProfessionDieSize.getValue());
    }
}
