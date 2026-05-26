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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
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
import mekhq.gui.campaignOptions.CampaignOptionFlag;
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
      private PersonnelDraft draft;
      private boolean generalPageCreated;
      private boolean awardsPageCreated;
      private boolean medicalPageCreated;
      private boolean informationPageCreated;
      private boolean prisonersAndDependentsPageCreated;

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

    private JPanel pnlBlobCrew;
    private JCheckBox chkUseBlobInfantry;
    private JCheckBox chkUseBlobBattleArmor;
    private JCheckBox chkUseBlobVehicleCrewGround;
    private JCheckBox chkUseBlobVehicleCrewVTOL;
    private JCheckBox chkUseBlobVehicleCrewNaval;
    private JCheckBox chkUseBlobVesselPilot;
    private JCheckBox chkUseBlobVesselGunner;
    private JCheckBox chkUseBlobVesselCrew;
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
            loadValuesFromCampaignOptions(new Version());
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

        pnlBlobCrew = new JPanel();
        chkUseBlobInfantry = new JCheckBox();
        chkUseBlobBattleArmor = new JCheckBox();
        chkUseBlobVehicleCrewGround = new JCheckBox();
        chkUseBlobVehicleCrewVTOL = new JCheckBox();
        chkUseBlobVehicleCrewNaval = new JCheckBox();
        chkUseBlobVesselPilot = new JCheckBox();
        chkUseBlobVesselGunner = new JCheckBox();
        chkUseBlobVesselCrew = new JCheckBox();
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
        pnlBlobCrew = createBlobCrewPanel();

        // Layout the Panels
        final JPanel panelRight = new CampaignOptionsStandardPanel("RightPanel");
        GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridwidth = 1;
        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        panelRight.add(pnlPersonnelCleanup, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlAdministrators, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlBlobCrew, layoutRight);

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

      generalPageCreated = true;
      updateGeneralControlsFromDraft();

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
        chkUseTactics = new CampaignOptionsCheckBox("UseTactics",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseTactics.addMouseListener(createTipPanelUpdater(generalHeader, "UseTactics"));
        chkUseInitiativeBonus = new CampaignOptionsCheckBox("UseInitiativeBonus",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseInitiativeBonus.addMouseListener(createTipPanelUpdater(generalHeader, "UseInitiativeBonus"));
        chkUseToughness = new CampaignOptionsCheckBox("UseToughness",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseToughness.addMouseListener(createTipPanelUpdater(generalHeader, "UseToughness"));
        chkUseRandomToughness = new CampaignOptionsCheckBox("UseRandomToughness",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseRandomToughness.addMouseListener(createTipPanelUpdater(generalHeader, "UseRandomToughness"));
        chkUseArtillery = new CampaignOptionsCheckBox("UseArtillery");
        chkUseArtillery.addMouseListener(createTipPanelUpdater(generalHeader, "UseArtillery"));
        chkUseAbilities = new CampaignOptionsCheckBox("UseAbilities");
        chkUseAbilities.addMouseListener(createTipPanelUpdater(generalHeader, "UseAbilities"));
        chkOnlyCommandersMatterVehicles = new CampaignOptionsCheckBox("OnlyCommandersMatterVehicles",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterVehicles.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterVehicles"));
        chkOnlyCommandersMatterInfantry = new CampaignOptionsCheckBox("OnlyCommandersMatterInfantry",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterInfantry.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterInfantry"));
        chkOnlyCommandersMatterBattleArmor = new CampaignOptionsCheckBox("OnlyCommandersMatterBattleArmor",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterBattleArmor.addMouseListener(createTipPanelUpdater(generalHeader,
              "OnlyCommandersMatterBattleArmor"));
        chkUseEdge = new CampaignOptionsCheckBox("UseEdge");
        chkUseEdge.addMouseListener(createTipPanelUpdater(generalHeader, "UseEdge"));
        chkUseSupportEdge = new CampaignOptionsCheckBox("UseSupportEdge");
        chkUseSupportEdge.addMouseListener(createTipPanelUpdater(generalHeader, "UseSupportEdge"));
        chkUseImplants = new CampaignOptionsCheckBox("UseImplants");
        chkUseImplants.addMouseListener(createTipPanelUpdater(generalHeader, "UseImplants"));
        chkUseAlternativeQualityAveraging = new CampaignOptionsCheckBox("UseAlternativeQualityAveraging",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelCleanUpPanel", true, "PersonnelCleanUpPanel",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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
        final JPanel panel = new CampaignOptionsStandardPanel("AdministratorsPanel", true, "AdministratorsPanel",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkAdminsHaveNegotiation, layout);

        layout.gridy++;
        panel.add(chkAdminExperienceLevelIncludeNegotiation, layout);

        return panel;
    }

    /**
     * Creates the panel for blob crew settings in the General Tab.
     *
     * @return a {@link JPanel} containing settings related to blob crews (temporary personnel pools).
     */
    private JPanel createBlobCrewPanel() {
        // Contents
        chkUseBlobInfantry = new CampaignOptionsCheckBox("UseBlobInfantry", getMetadata(new Version(0, 50, 12)));
        chkUseBlobInfantry.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobInfantry"));
        chkUseBlobBattleArmor = new CampaignOptionsCheckBox("UseBlobBattleArmor", getMetadata(new Version(0, 50, 12)));
        chkUseBlobBattleArmor.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobBattleArmor"));
        chkUseBlobVehicleCrewGround = new CampaignOptionsCheckBox("UseBlobVehicleCrewGround", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewGround.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewGround"));
        chkUseBlobVehicleCrewVTOL = new CampaignOptionsCheckBox("UseBlobVehicleCrewVTOL", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewVTOL.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewVTOL"));
        chkUseBlobVehicleCrewNaval = new CampaignOptionsCheckBox("UseBlobVehicleCrewNaval", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewNaval.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewNaval"));
        chkUseBlobVesselPilot = new CampaignOptionsCheckBox("UseBlobVesselPilot", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselPilot.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselPilot"));
        chkUseBlobVesselGunner = new CampaignOptionsCheckBox("UseBlobVesselGunner", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselGunner.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselGunner"));
        chkUseBlobVesselCrew = new CampaignOptionsCheckBox("UseBlobVesselCrew", getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselCrew.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselCrew"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("BlobCrewPanel", true, "BlobCrewPanel",
              getMetadata(new Version(0, 50, 12)));
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUseBlobInfantry, layout);

        layout.gridy++;
        panel.add(chkUseBlobBattleArmor, layout);

        layout.gridy++;
        panel.add(chkUseBlobVehicleCrewGround, layout);

        layout.gridy++;
        panel.add(chkUseBlobVehicleCrewVTOL, layout);

        layout.gridy++;
        panel.add(chkUseBlobVehicleCrewNaval, layout);

        layout.gridy++;
        panel.add(chkUseBlobVesselPilot, layout);

        layout.gridy++;
        panel.add(chkUseBlobVesselGunner, layout);

        layout.gridy++;
        panel.add(chkUseBlobVesselCrew, layout);

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

        final JPanel panelBottom = new CampaignOptionsStandardPanel("AwardsTabBottom", true, "AwardsTabBottom",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
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

      awardsPageCreated = true;
      updateAwardsControlsFromDraft();

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
        chkEnableFactionHunterAwards = new CampaignOptionsCheckBox("EnableFactionHunterAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableFactionHunterAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableFactionHunterAwards"));
        chkEnableInjuryAwards = new CampaignOptionsCheckBox("EnableInjuryAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
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
        chkEnableTimeAwards = new CampaignOptionsCheckBox("EnableTimeAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableTimeAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTimeAwards"));
        chkEnableTrainingAwards = new CampaignOptionsCheckBox("EnableTrainingAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
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
        chkUseAdvancedMedical = new CampaignOptionsCheckBox("UseAdvancedMedical",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED));
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

        chkUseTougherHealing = new CampaignOptionsCheckBox("UseTougherHealing",
              getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseTougherHealing.addMouseListener(createTipPanelUpdater(medicalHeader, "UseTougherHealing"));

        chkUseAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseAlternativeAdvancedMedical",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT));
        chkUseAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseAlternativeAdvancedMedical"));

        chkUseKinderAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseKinderAlternativeAdvancedMedical",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT));
        chkUseKinderAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseKinderAlternativeAdvancedMedical"));

        chkUseRandomDiseases = new CampaignOptionsCheckBox("UseRandomDiseases",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT));
        chkUseRandomDiseases.addMouseListener(createTipPanelUpdater(medicalHeader,
              "UseRandomDiseases"));

        lblMaximumPatients = new CampaignOptionsLabel("MaximumPatients");
        lblMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));
        spnMaximumPatients = new CampaignOptionsSpinner("MaximumPatients", 25, 1, 100, 1);
        spnMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));

        chkDoctorsUseAdministration = new CampaignOptionsCheckBox("DoctorsUseAdministration",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkDoctorsUseAdministration.addMouseListener(createTipPanelUpdater(medicalHeader, "DoctorsUseAdministration"));

        chkUseUsefulMedics = new CampaignOptionsCheckBox("UseUsefulMedics",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseUsefulMedics.addMouseListener(createTipPanelUpdater(medicalHeader, "UseUsefulMedics"));

        chkUseMASHTheatres = new CampaignOptionsCheckBox("UseMASHTheatres",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseMASHTheatres.addMouseListener(createTipPanelUpdater(medicalHeader, "UseMASHTheatres"));

        lblMASHTheatreCapacity = new CampaignOptionsLabel("MASHTheatreCapacity",
              getMetadata(MILESTONE_BEFORE_METADATA));
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

      medicalPageCreated = true;
      updateMedicalControlsFromDraft();

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

        final JPanel panelParent = new CampaignOptionsStandardPanel("PersonnelInformation", true,
              "PersonnelInformation",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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

      informationPageCreated = true;
      updateInformationControlsFromDraft();

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
        chkUseTransfers = new CampaignOptionsCheckBox("UseTransfers",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
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
        chkDisplayPatientRecord = new CampaignOptionsCheckBox("DisplayPatientRecord",
              getMetadata(MILESTONE_BEFORE_METADATA));
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
        final JPanel panel = new CampaignOptionsStandardPanel("PrisonersAndDependentsTab", true, "PrisonersAndDependentsTab",
              getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED));
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

      prisonersAndDependentsPageCreated = true;
      updatePrisonersAndDependentsControlsFromDraft();

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

        chkUseFunctionalEscapeArtist = new CampaignOptionsCheckBox("UseFunctionalEscapeArtist",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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

        lblDependentProfessionDieSize = new CampaignOptionsLabel("DependentProfessionDieSize",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblDependentProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "DependentProfessionDieSize"));
        spnDependentProfessionDieSize = new CampaignOptionsSpinner("DependentProfessionDieSize",
              4, 0, 100, 1);
        spnDependentProfessionDieSize.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
              "DependentProfessionDieSize"));

        lblCivilianProfessionDieSize = new CampaignOptionsLabel("CivilianProfessionDieSize",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
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
     * Shortcut method to load default {@link CampaignOptions} values into the tab components.
     */
    public void loadValuesFromCampaignOptions(Version version) {
        loadValuesFromCampaignOptions(null, version);
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

      draft = new PersonnelDraft(options);
      updateCreatedControlsFromDraft();
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

            updateDraftFromCreatedControls();
            draft.applyTo(campaign, options);
      }

      private void updateCreatedControlsFromDraft() {
            updateGeneralControlsFromDraft();
            updateAwardsControlsFromDraft();
            updateMedicalControlsFromDraft();
            updateInformationControlsFromDraft();
            updatePrisonersAndDependentsControlsFromDraft();
      }

      private void updateGeneralControlsFromDraft() {
            if (!generalPageCreated || draft == null) {
                  return;
            }

            chkUseTactics.setSelected(draft.useTactics);
            chkUseInitiativeBonus.setSelected(draft.useInitiativeBonus);
            chkUseToughness.setSelected(draft.useToughness);
            chkUseRandomToughness.setSelected(draft.useRandomToughness);
            chkUseArtillery.setSelected(draft.useArtillery);
            chkUseAbilities.setSelected(draft.useAbilities);
            chkOnlyCommandersMatterVehicles.setSelected(draft.onlyCommandersMatterVehicles);
            chkOnlyCommandersMatterInfantry.setSelected(draft.onlyCommandersMatterInfantry);
            chkOnlyCommandersMatterBattleArmor.setSelected(draft.onlyCommandersMatterBattleArmor);
            chkUseEdge.setSelected(draft.useEdge);
            chkUseSupportEdge.setSelected(draft.useSupportEdge);
            chkUseImplants.setSelected(draft.useImplants);
            chkUseAlternativeQualityAveraging.setSelected(draft.alternativeQualityAveraging);
            chkUsePersonnelRemoval.setSelected(draft.usePersonnelRemoval);
            chkUseRemovalExemptCemetery.setSelected(draft.useRemovalExemptCemetery);
            chkUseRemovalExemptRetirees.setSelected(draft.useRemovalExemptRetirees);
            chkAdminsHaveNegotiation.setSelected(draft.adminsHaveNegotiation);
            chkAdminExperienceLevelIncludeNegotiation.setSelected(draft.adminExperienceLevelIncludeNegotiation);
            chkUseBlobInfantry.setSelected(draft.useBlobInfantry);
            chkUseBlobBattleArmor.setSelected(draft.useBlobBattleArmor);
            chkUseBlobVehicleCrewGround.setSelected(draft.useBlobVehicleCrewGround);
            chkUseBlobVehicleCrewVTOL.setSelected(draft.useBlobVehicleCrewVTOL);
            chkUseBlobVehicleCrewNaval.setSelected(draft.useBlobVehicleCrewNaval);
            chkUseBlobVesselPilot.setSelected(draft.useBlobVesselPilot);
            chkUseBlobVesselGunner.setSelected(draft.useBlobVesselGunner);
            chkUseBlobVesselCrew.setSelected(draft.useBlobVesselCrew);
      }

      private void updateAwardsControlsFromDraft() {
            if (!awardsPageCreated || draft == null) {
                  return;
            }

            comboAwardBonusStyle.setSelectedItem(draft.awardBonusStyle);
            spnAwardTierSize.setValue(draft.awardTierSize);
            chkEnableAutoAwards.setSelected(draft.enableAutoAwards);
            chkIssuePosthumousAwards.setSelected(draft.issuePosthumousAwards);
            chkIssueBestAwardOnly.setSelected(draft.issueBestAwardOnly);
            chkIgnoreStandardSet.setSelected(draft.ignoreStandardSet);
            chkEnableContractAwards.setSelected(draft.enableContractAwards);
            chkEnableFactionHunterAwards.setSelected(draft.enableFactionHunterAwards);
            chkEnableInjuryAwards.setSelected(draft.enableInjuryAwards);
            chkEnableIndividualKillAwards.setSelected(draft.enableIndividualKillAwards);
            chkEnableFormationKillAwards.setSelected(draft.enableFormationKillAwards);
            chkEnableRankAwards.setSelected(draft.enableRankAwards);
            chkEnableScenarioAwards.setSelected(draft.enableScenarioAwards);
            chkEnableSkillAwards.setSelected(draft.enableSkillAwards);
            chkEnableTheatreOfWarAwards.setSelected(draft.enableTheatreOfWarAwards);
            chkEnableTimeAwards.setSelected(draft.enableTimeAwards);
            chkEnableTrainingAwards.setSelected(draft.enableTrainingAwards);
            chkEnableMiscAwards.setSelected(draft.enableMiscAwards);
            txtAwardSetFilterList.setText(draft.awardSetFilterList);
      }

      private void updateMedicalControlsFromDraft() {
            if (!medicalPageCreated || draft == null) {
                  return;
            }

            chkUseAdvancedMedical.setSelected(draft.useAdvancedMedical);
            spnHealWaitingPeriod.setValue(draft.healingWaitingPeriod);
            spnNaturalHealWaitingPeriod.setValue(draft.naturalHealingWaitingPeriod);
            spnMinimumHitsForVehicles.setValue(draft.minimumHitsForVehicles);
            chkUseRandomHitsForVehicles.setSelected(draft.useRandomHitsForVehicles);
            chkUseTougherHealing.setSelected(draft.tougherHealing);
            chkUseAlternativeAdvancedMedical.setSelected(draft.useAlternativeAdvancedMedical);
            chkUseKinderAlternativeAdvancedMedical.setSelected(draft.useKinderAlternativeAdvancedMedical);
            chkUseRandomDiseases.setSelected(draft.useRandomDiseases);
            spnMaximumPatients.setValue(draft.maximumPatients);
            chkDoctorsUseAdministration.setSelected(draft.doctorsUseAdministration);
            chkUseUsefulMedics.setSelected(draft.useUsefulMedics);
            chkUseMASHTheatres.setSelected(draft.useMASHTheatres);
            spnMASHTheatreCapacity.setValue(draft.mashTheatreCapacity);
      }

      private void updateInformationControlsFromDraft() {
            if (!informationPageCreated || draft == null) {
                  return;
            }

            chkUseTransfers.setSelected(draft.useTransfers);
            chkUseExtendedTOEForceName.setSelected(draft.useExtendedTOEForceName);
            chkPersonnelLogSkillGain.setSelected(draft.personnelLogSkillGain);
            chkPersonnelLogAbilityGain.setSelected(draft.personnelLogAbilityGain);
            chkPersonnelLogEdgeGain.setSelected(draft.personnelLogEdgeGain);
            chkDisplayPersonnelLog.setSelected(draft.displayPersonnelLog);
            chkDisplayScenarioLog.setSelected(draft.displayScenarioLog);
            chkDisplayKillRecord.setSelected(draft.displayKillRecord);
            chkDisplayMedicalRecord.setSelected(draft.displayMedicalRecord);
            chkDisplayPatientRecord.setSelected(draft.displayPatientRecord);
            chkDisplayAssignmentRecord.setSelected(draft.displayAssignmentRecord);
            chkDisplayPerformanceRecord.setSelected(draft.displayPerformanceRecord);
            chkUseTimeInService.setSelected(draft.useTimeInService);
            comboTimeInServiceDisplayFormat.setSelectedItem(draft.timeInServiceDisplayFormat);
            chkUseTimeInRank.setSelected(draft.useTimeInRank);
            comboTimeInRankDisplayFormat.setSelectedItem(draft.timeInRankDisplayFormat);
            chkTrackTotalEarnings.setSelected(draft.trackTotalEarnings);
            chkTrackTotalXPEarnings.setSelected(draft.trackTotalXPEarnings);
            chkShowOriginFaction.setSelected(draft.showOriginFaction);
      }

      private void updatePrisonersAndDependentsControlsFromDraft() {
            if (!prisonersAndDependentsPageCreated || draft == null) {
                  return;
            }

            comboPrisonerCaptureStyle.setSelectedItem(draft.prisonerCaptureStyle);
            chkUseFunctionalEscapeArtist.setSelected(draft.useFunctionalEscapeArtist);
            chkResetTemporaryPrisonerCapacity.setSelected(draft.resetTemporaryPrisonerCapacity);
            chkUseRandomDependentAddition.setSelected(draft.useRandomDependentAddition);
            chkUseRandomDependentRemoval.setSelected(draft.useRandomDependentRemoval);
            spnDependentProfessionDieSize.setValue(draft.dependentProfessionDieSize);
            spnCivilianProfessionDieSize.setValue(draft.civilianProfessionDieSize);
      }

      private void updateDraftFromCreatedControls() {
            updateDraftFromGeneralControls();
            updateDraftFromAwardsControls();
            updateDraftFromMedicalControls();
            updateDraftFromInformationControls();
            updateDraftFromPrisonersAndDependentsControls();
      }

      private void updateDraftFromGeneralControls() {
            if (!generalPageCreated) {
                  return;
            }

            draft.useTactics = chkUseTactics.isSelected();
            draft.useInitiativeBonus = chkUseInitiativeBonus.isSelected();
            draft.useToughness = chkUseToughness.isSelected();
            draft.useRandomToughness = chkUseRandomToughness.isSelected();
            draft.useArtillery = chkUseArtillery.isSelected();
            draft.useAbilities = chkUseAbilities.isSelected();
            draft.onlyCommandersMatterVehicles = chkOnlyCommandersMatterVehicles.isSelected();
            draft.onlyCommandersMatterInfantry = chkOnlyCommandersMatterInfantry.isSelected();
            draft.onlyCommandersMatterBattleArmor = chkOnlyCommandersMatterBattleArmor.isSelected();
            draft.useEdge = chkUseEdge.isSelected();
            draft.useSupportEdge = chkUseSupportEdge.isSelected();
            draft.useImplants = chkUseImplants.isSelected();
            draft.alternativeQualityAveraging = chkUseAlternativeQualityAveraging.isSelected();
            draft.usePersonnelRemoval = chkUsePersonnelRemoval.isSelected();
            draft.useRemovalExemptCemetery = chkUseRemovalExemptCemetery.isSelected();
            draft.useRemovalExemptRetirees = chkUseRemovalExemptRetirees.isSelected();
            draft.adminsHaveNegotiation = chkAdminsHaveNegotiation.isSelected();
            draft.adminExperienceLevelIncludeNegotiation = chkAdminExperienceLevelIncludeNegotiation.isSelected();
            draft.useBlobInfantry = chkUseBlobInfantry.isSelected();
            draft.useBlobBattleArmor = chkUseBlobBattleArmor.isSelected();
            draft.useBlobVehicleCrewGround = chkUseBlobVehicleCrewGround.isSelected();
            draft.useBlobVehicleCrewVTOL = chkUseBlobVehicleCrewVTOL.isSelected();
            draft.useBlobVehicleCrewNaval = chkUseBlobVehicleCrewNaval.isSelected();
            draft.useBlobVesselPilot = chkUseBlobVesselPilot.isSelected();
            draft.useBlobVesselGunner = chkUseBlobVesselGunner.isSelected();
            draft.useBlobVesselCrew = chkUseBlobVesselCrew.isSelected();
      }

      private void updateDraftFromAwardsControls() {
            if (!awardsPageCreated) {
                  return;
            }

            draft.awardBonusStyle = comboAwardBonusStyle.getSelectedItem();
            draft.awardTierSize = (int) spnAwardTierSize.getValue();
            draft.enableAutoAwards = chkEnableAutoAwards.isSelected();
            draft.issuePosthumousAwards = chkIssuePosthumousAwards.isSelected();
            draft.issueBestAwardOnly = chkIssueBestAwardOnly.isSelected();
            draft.ignoreStandardSet = chkIgnoreStandardSet.isSelected();
            draft.enableContractAwards = chkEnableContractAwards.isSelected();
            draft.enableFactionHunterAwards = chkEnableFactionHunterAwards.isSelected();
            draft.enableInjuryAwards = chkEnableInjuryAwards.isSelected();
            draft.enableIndividualKillAwards = chkEnableIndividualKillAwards.isSelected();
            draft.enableFormationKillAwards = chkEnableFormationKillAwards.isSelected();
            draft.enableRankAwards = chkEnableRankAwards.isSelected();
            draft.enableScenarioAwards = chkEnableScenarioAwards.isSelected();
            draft.enableSkillAwards = chkEnableSkillAwards.isSelected();
            draft.enableTheatreOfWarAwards = chkEnableTheatreOfWarAwards.isSelected();
            draft.enableTimeAwards = chkEnableTimeAwards.isSelected();
            draft.enableTrainingAwards = chkEnableTrainingAwards.isSelected();
            draft.enableMiscAwards = chkEnableMiscAwards.isSelected();
            draft.awardSetFilterList = txtAwardSetFilterList.getText();
      }

      private void updateDraftFromMedicalControls() {
            if (!medicalPageCreated) {
                  return;
            }

            draft.useAdvancedMedical = chkUseAdvancedMedical.isSelected();
            draft.healingWaitingPeriod = (int) spnHealWaitingPeriod.getValue();
            draft.naturalHealingWaitingPeriod = (int) spnNaturalHealWaitingPeriod.getValue();
            draft.minimumHitsForVehicles = (int) spnMinimumHitsForVehicles.getValue();
            draft.useRandomHitsForVehicles = chkUseRandomHitsForVehicles.isSelected();
            draft.tougherHealing = chkUseTougherHealing.isSelected();
            draft.useAlternativeAdvancedMedical = chkUseAlternativeAdvancedMedical.isSelected();
            draft.useKinderAlternativeAdvancedMedical = chkUseKinderAlternativeAdvancedMedical.isSelected();
            draft.useRandomDiseases = chkUseRandomDiseases.isSelected();
            draft.maximumPatients = (int) spnMaximumPatients.getValue();
            draft.doctorsUseAdministration = chkDoctorsUseAdministration.isSelected();
            draft.useUsefulMedics = chkUseUsefulMedics.isSelected();
            draft.useMASHTheatres = chkUseMASHTheatres.isSelected();
            draft.mashTheatreCapacity = (int) spnMASHTheatreCapacity.getValue();
      }

      private void updateDraftFromInformationControls() {
            if (!informationPageCreated) {
                  return;
            }

            draft.useTransfers = chkUseTransfers.isSelected();
            draft.useExtendedTOEForceName = chkUseExtendedTOEForceName.isSelected();
            draft.personnelLogSkillGain = chkPersonnelLogSkillGain.isSelected();
            draft.personnelLogAbilityGain = chkPersonnelLogAbilityGain.isSelected();
            draft.personnelLogEdgeGain = chkPersonnelLogEdgeGain.isSelected();
            draft.displayPersonnelLog = chkDisplayPersonnelLog.isSelected();
            draft.displayScenarioLog = chkDisplayScenarioLog.isSelected();
            draft.displayKillRecord = chkDisplayKillRecord.isSelected();
            draft.displayMedicalRecord = chkDisplayMedicalRecord.isSelected();
            draft.displayPatientRecord = chkDisplayPatientRecord.isSelected();
            draft.displayAssignmentRecord = chkDisplayAssignmentRecord.isSelected();
            draft.displayPerformanceRecord = chkDisplayPerformanceRecord.isSelected();
            draft.useTimeInService = chkUseTimeInService.isSelected();
            draft.timeInServiceDisplayFormat = comboTimeInServiceDisplayFormat.getSelectedItem();
            draft.useTimeInRank = chkUseTimeInRank.isSelected();
            draft.timeInRankDisplayFormat = comboTimeInRankDisplayFormat.getSelectedItem();
            draft.trackTotalEarnings = chkTrackTotalEarnings.isSelected();
            draft.trackTotalXPEarnings = chkTrackTotalXPEarnings.isSelected();
            draft.showOriginFaction = chkShowOriginFaction.isSelected();
      }

      private void updateDraftFromPrisonersAndDependentsControls() {
            if (!prisonersAndDependentsPageCreated) {
                  return;
            }

            draft.prisonerCaptureStyle = comboPrisonerCaptureStyle.getSelectedItem();
            draft.useFunctionalEscapeArtist = chkUseFunctionalEscapeArtist.isSelected();
            draft.resetTemporaryPrisonerCapacity = chkResetTemporaryPrisonerCapacity.isSelected();
            draft.useRandomDependentAddition = chkUseRandomDependentAddition.isSelected();
            draft.useRandomDependentRemoval = chkUseRandomDependentRemoval.isSelected();
            draft.dependentProfessionDieSize = (int) spnDependentProfessionDieSize.getValue();
            draft.civilianProfessionDieSize = (int) spnCivilianProfessionDieSize.getValue();
      }

      private static class PersonnelDraft {
            private boolean useTactics;
            private boolean useInitiativeBonus;
            private boolean useToughness;
            private boolean useRandomToughness;
            private boolean useArtillery;
            private boolean useAbilities;
            private boolean onlyCommandersMatterVehicles;
            private boolean onlyCommandersMatterInfantry;
            private boolean onlyCommandersMatterBattleArmor;
            private boolean useEdge;
            private boolean useSupportEdge;
            private boolean useImplants;
            private boolean alternativeQualityAveraging;
            private boolean usePersonnelRemoval;
            private boolean useRemovalExemptCemetery;
            private boolean useRemovalExemptRetirees;
            private boolean adminsHaveNegotiation;
            private boolean adminExperienceLevelIncludeNegotiation;
            private boolean useBlobInfantry;
            private boolean useBlobBattleArmor;
            private boolean useBlobVehicleCrewGround;
            private boolean useBlobVehicleCrewVTOL;
            private boolean useBlobVehicleCrewNaval;
            private boolean useBlobVesselPilot;
            private boolean useBlobVesselGunner;
            private boolean useBlobVesselCrew;
            private boolean useTransfers;
            private boolean useExtendedTOEForceName;
            private boolean personnelLogSkillGain;
            private boolean personnelLogAbilityGain;
            private boolean personnelLogEdgeGain;
            private boolean displayPersonnelLog;
            private boolean displayScenarioLog;
            private boolean displayKillRecord;
            private boolean displayMedicalRecord;
            private boolean displayPatientRecord;
            private boolean displayAssignmentRecord;
            private boolean displayPerformanceRecord;
            private boolean useTimeInService;
            private TimeInDisplayFormat timeInServiceDisplayFormat;
            private boolean useTimeInRank;
            private TimeInDisplayFormat timeInRankDisplayFormat;
            private boolean trackTotalEarnings;
            private boolean trackTotalXPEarnings;
            private boolean showOriginFaction;
            private AwardBonus awardBonusStyle;
            private int awardTierSize;
            private boolean enableAutoAwards;
            private boolean issuePosthumousAwards;
            private boolean issueBestAwardOnly;
            private boolean ignoreStandardSet;
            private boolean enableContractAwards;
            private boolean enableFactionHunterAwards;
            private boolean enableInjuryAwards;
            private boolean enableIndividualKillAwards;
            private boolean enableFormationKillAwards;
            private boolean enableRankAwards;
            private boolean enableScenarioAwards;
            private boolean enableSkillAwards;
            private boolean enableTheatreOfWarAwards;
            private boolean enableTimeAwards;
            private boolean enableTrainingAwards;
            private boolean enableMiscAwards;
            private String awardSetFilterList;
            private boolean useAdvancedMedical;
            private int healingWaitingPeriod;
            private int naturalHealingWaitingPeriod;
            private int minimumHitsForVehicles;
            private boolean useRandomHitsForVehicles;
            private boolean tougherHealing;
            private boolean useAlternativeAdvancedMedical;
            private boolean useKinderAlternativeAdvancedMedical;
            private boolean useRandomDiseases;
            private int maximumPatients;
            private boolean doctorsUseAdministration;
            private boolean useUsefulMedics;
            private boolean useMASHTheatres;
            private int mashTheatreCapacity;
            private PrisonerCaptureStyle prisonerCaptureStyle;
            private boolean useFunctionalEscapeArtist;
            private boolean resetTemporaryPrisonerCapacity;
            private boolean useRandomDependentAddition;
            private boolean useRandomDependentRemoval;
            private int dependentProfessionDieSize;
            private int civilianProfessionDieSize;

            private PersonnelDraft(CampaignOptions options) {
                  useTactics = options.isUseTactics();
                  useInitiativeBonus = options.isUseInitiativeBonus();
                  useToughness = options.isUseToughness();
                  useRandomToughness = options.isUseRandomToughness();
                  useArtillery = options.isUseArtillery();
                  useAbilities = options.isUseAbilities();
                  onlyCommandersMatterVehicles = options.isOnlyCommandersMatterVehicles();
                  onlyCommandersMatterInfantry = options.isOnlyCommandersMatterInfantry();
                  onlyCommandersMatterBattleArmor = options.isOnlyCommandersMatterBattleArmor();
                  useEdge = options.isUseEdge();
                  useSupportEdge = options.isUseSupportEdge();
                  useImplants = options.isUseImplants();
                  alternativeQualityAveraging = options.isAlternativeQualityAveraging();
                  usePersonnelRemoval = options.isUsePersonnelRemoval();
                  useRemovalExemptCemetery = options.isUseRemovalExemptCemetery();
                  useRemovalExemptRetirees = options.isUseRemovalExemptRetirees();
                  adminsHaveNegotiation = options.isAdminsHaveNegotiation();
                  adminExperienceLevelIncludeNegotiation = options.isAdminExperienceLevelIncludeNegotiation();
                  useBlobInfantry = options.isUseBlobInfantry();
                  useBlobBattleArmor = options.isUseBlobBattleArmor();
                  useBlobVehicleCrewGround = options.isUseBlobVehicleCrewGround();
                  useBlobVehicleCrewVTOL = options.isUseBlobVehicleCrewVTOL();
                  useBlobVehicleCrewNaval = options.isUseBlobVehicleCrewNaval();
                  useBlobVesselPilot = options.isUseBlobVesselPilot();
                  useBlobVesselGunner = options.isUseBlobVesselGunner();
                  useBlobVesselCrew = options.isUseBlobVesselCrew();
                  useTransfers = options.isUseTransfers();
                  useExtendedTOEForceName = options.isUseExtendedTOEForceName();
                  personnelLogSkillGain = options.isPersonnelLogSkillGain();
                  personnelLogAbilityGain = options.isPersonnelLogAbilityGain();
                  personnelLogEdgeGain = options.isPersonnelLogEdgeGain();
                  displayPersonnelLog = options.isDisplayPersonnelLog();
                  displayScenarioLog = options.isDisplayScenarioLog();
                  displayKillRecord = options.isDisplayKillRecord();
                  displayMedicalRecord = options.isDisplayMedicalRecord();
                  displayPatientRecord = options.isDisplayPatientRecord();
                  displayAssignmentRecord = options.isDisplayAssignmentRecord();
                  displayPerformanceRecord = options.isDisplayPerformanceRecord();
                  useTimeInService = options.isUseTimeInService();
                  timeInServiceDisplayFormat = options.getTimeInServiceDisplayFormat();
                  useTimeInRank = options.isUseTimeInRank();
                  timeInRankDisplayFormat = options.getTimeInRankDisplayFormat();
                  trackTotalEarnings = options.isTrackTotalEarnings();
                  trackTotalXPEarnings = options.isTrackTotalXPEarnings();
                  showOriginFaction = options.isShowOriginFaction();
                  awardBonusStyle = options.getAwardBonusStyle();
                  awardTierSize = options.getAwardTierSize();
                  enableAutoAwards = options.isEnableAutoAwards();
                  issuePosthumousAwards = options.isIssuePosthumousAwards();
                  issueBestAwardOnly = options.isIssueBestAwardOnly();
                  ignoreStandardSet = options.isIgnoreStandardSet();
                  enableContractAwards = options.isEnableContractAwards();
                  enableFactionHunterAwards = options.isEnableFactionHunterAwards();
                  enableInjuryAwards = options.isEnableInjuryAwards();
                  enableIndividualKillAwards = options.isEnableIndividualKillAwards();
                  enableFormationKillAwards = options.isEnableFormationKillAwards();
                  enableRankAwards = options.isEnableRankAwards();
                  enableScenarioAwards = options.isEnableScenarioAwards();
                  enableSkillAwards = options.isEnableSkillAwards();
                  enableTheatreOfWarAwards = options.isEnableTheatreOfWarAwards();
                  enableTimeAwards = options.isEnableTimeAwards();
                  enableTrainingAwards = options.isEnableTrainingAwards();
                  enableMiscAwards = options.isEnableMiscAwards();
                  awardSetFilterList = options.getAwardSetFilterList();
                  useAdvancedMedical = options.isUseAdvancedMedicalDirect();
                  healingWaitingPeriod = options.getHealingWaitingPeriod();
                  naturalHealingWaitingPeriod = options.getNaturalHealingWaitingPeriod();
                  minimumHitsForVehicles = options.getMinimumHitsForVehicles();
                  useRandomHitsForVehicles = options.isUseRandomHitsForVehicles();
                  tougherHealing = options.isTougherHealing();
                  useAlternativeAdvancedMedical = options.isUseAlternativeAdvancedMedical();
                  useKinderAlternativeAdvancedMedical = options.isUseKinderAlternativeAdvancedMedical();
                  useRandomDiseases = options.isUseRandomDiseases();
                  maximumPatients = options.getMaximumPatients();
                  doctorsUseAdministration = options.isDoctorsUseAdministration();
                  useUsefulMedics = options.isUseUsefulMedics();
                  useMASHTheatres = options.isUseMASHTheatres();
                  mashTheatreCapacity = options.getMASHTheatreCapacity();
                  prisonerCaptureStyle = options.getPrisonerCaptureStyle();
                  useFunctionalEscapeArtist = options.isUseFunctionalEscapeArtist();
                  resetTemporaryPrisonerCapacity = false;
                  useRandomDependentAddition = options.isUseRandomDependentAddition();
                  useRandomDependentRemoval = options.isUseRandomDependentRemoval();
                  dependentProfessionDieSize = options.getDependentProfessionDieSize();
                  civilianProfessionDieSize = options.getCivilianProfessionDieSize();
            }

            private void applyTo(Campaign campaign, CampaignOptions options) {
                  options.setUseTactics(useTactics);
                  options.setUseInitiativeBonus(useInitiativeBonus);
                  options.setUseToughness(useToughness);
                  options.setUseRandomToughness(useRandomToughness);
                  options.setUseArtillery(useArtillery);
                  options.setUseAbilities(useAbilities);
                  options.setOnlyCommandersMatterVehicles(onlyCommandersMatterVehicles);
                  options.setOnlyCommandersMatterInfantry(onlyCommandersMatterInfantry);
                  options.setOnlyCommandersMatterBattleArmor(onlyCommandersMatterBattleArmor);
                  options.setUseEdge(useEdge);
                  options.setUseSupportEdge(useSupportEdge);
                  options.setUseImplants(useImplants);
                  options.setAlternativeQualityAveraging(alternativeQualityAveraging);
                  options.setUsePersonnelRemoval(usePersonnelRemoval);
                  options.setUseRemovalExemptCemetery(useRemovalExemptCemetery);
                  options.setUseRemovalExemptRetirees(useRemovalExemptRetirees);
                  options.setAdminsHaveNegotiation(adminsHaveNegotiation);
                  options.setAdminExperienceLevelIncludeNegotiation(adminExperienceLevelIncludeNegotiation);
                  options.setUseBlobInfantry(useBlobInfantry);
                  options.setUseBlobBattleArmor(useBlobBattleArmor);
                  options.setUseBlobVehicleCrewGround(useBlobVehicleCrewGround);
                  options.setUseBlobVehicleCrewVTOL(useBlobVehicleCrewVTOL);
                  options.setUseBlobVehicleCrewNaval(useBlobVehicleCrewNaval);
                  options.setUseBlobVesselPilot(useBlobVesselPilot);
                  options.setUseBlobVesselGunner(useBlobVesselGunner);
                  options.setUseBlobVesselCrew(useBlobVesselCrew);
                  options.setUseTransfers(useTransfers);
                  options.setUseExtendedTOEForceName(useExtendedTOEForceName);
                  options.setPersonnelLogSkillGain(personnelLogSkillGain);
                  options.setPersonnelLogAbilityGain(personnelLogAbilityGain);
                  options.setPersonnelLogEdgeGain(personnelLogEdgeGain);
                  options.setDisplayPersonnelLog(displayPersonnelLog);
                  options.setDisplayScenarioLog(displayScenarioLog);
                  options.setDisplayKillRecord(displayKillRecord);
                  options.setDisplayMedicalRecord(displayMedicalRecord);
                  options.setDisplayPatientRecord(displayPatientRecord);
                  options.setDisplayAssignmentRecord(displayAssignmentRecord);
                  options.setDisplayPerformanceRecord(displayPerformanceRecord);
                  options.setUseTimeInService(useTimeInService);
                  options.setTimeInServiceDisplayFormat(timeInServiceDisplayFormat);
                  options.setUseTimeInRank(useTimeInRank);
                  options.setTimeInRankDisplayFormat(timeInRankDisplayFormat);
                  options.setTrackTotalEarnings(trackTotalEarnings);
                  options.setTrackTotalXPEarnings(trackTotalXPEarnings);
                  options.setShowOriginFaction(showOriginFaction);
                  options.setAwardBonusStyle(awardBonusStyle);
                  options.setAwardTierSize(awardTierSize);
                  options.setEnableAutoAwards(enableAutoAwards);
                  options.setIssuePosthumousAwards(issuePosthumousAwards);
                  options.setIssueBestAwardOnly(issueBestAwardOnly);
                  options.setIgnoreStandardSet(ignoreStandardSet);
                  options.setEnableContractAwards(enableContractAwards);
                  options.setEnableFactionHunterAwards(enableFactionHunterAwards);
                  options.setEnableInjuryAwards(enableInjuryAwards);
                  options.setEnableIndividualKillAwards(enableIndividualKillAwards);
                  options.setEnableFormationKillAwards(enableFormationKillAwards);
                  options.setEnableRankAwards(enableRankAwards);
                  options.setEnableScenarioAwards(enableScenarioAwards);
                  options.setEnableSkillAwards(enableSkillAwards);
                  options.setEnableTheatreOfWarAwards(enableTheatreOfWarAwards);
                  options.setEnableTimeAwards(enableTimeAwards);
                  options.setEnableTrainingAwards(enableTrainingAwards);
                  options.setEnableMiscAwards(enableMiscAwards);
                  options.setAwardSetFilterList(awardSetFilterList);
                  options.setUseAdvancedMedical(useAdvancedMedical);
                  options.setHealingWaitingPeriod(healingWaitingPeriod);
                  options.setNaturalHealingWaitingPeriod(naturalHealingWaitingPeriod);
                  options.setMinimumHitsForVehicles(minimumHitsForVehicles);
                  options.setUseRandomHitsForVehicles(useRandomHitsForVehicles);
                  options.setTougherHealing(tougherHealing);
                  options.setUseAlternativeAdvancedMedical(useAlternativeAdvancedMedical);
                  options.setUseKinderAlternativeAdvancedMedical(useKinderAlternativeAdvancedMedical);
                  options.setUseRandomDiseases(useRandomDiseases);
                  options.setMaximumPatients(maximumPatients);
                  options.setDoctorsUseAdministration(doctorsUseAdministration);
                  options.setIsUseUsefulMedics(useUsefulMedics);
                  options.setIsUseMASHTheatres(useMASHTheatres);
                  options.setMASHTheatreCapacity(mashTheatreCapacity);
                  options.setPrisonerCaptureStyle(prisonerCaptureStyle);
                  options.setUseFunctionalEscapeArtist(useFunctionalEscapeArtist);
                  if (resetTemporaryPrisonerCapacity) {
                        campaign.setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
                  }
                  options.setUseRandomDependentAddition(useRandomDependentAddition);
                  options.setUseRandomDependentRemoval(useRandomDependentRemoval);
                  options.setDependentProfessionDieSize(dependentProfessionDieSize);
                  options.setCivilianProfessionDieSize(civilianProfessionDieSize);
            }
    }
}
