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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.*;

import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.EdgeRefreshPeriod;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code PersonnelTab} class represents the user interface components for
 * configuring personnel-related options in
 * the MekHQ Campaign Options dialog. This class handles the initialization,
 * layout, and logic for various personnel
 * settings spanning multiple tabs, such as general personnel options, personnel
 * logs, information, awards, medical
 * options, salaries, and prisoners and dependents.
 * <p>
 * The class is organized into multiple tabs that encapsulate settings under
 * specific categories:
 * </p>
 * <ul>
 * <li><b>General Tab:</b> General settings for personnel management such as
 * tactics,
 * initiative bonus, toughness, and edge settings.</li>
 * <li><b>Personnel Logs Tab:</b> Settings for logging activities like skill or
 * ability
 * gains, personnel transfers, and kill records.</li>
 * <li><b>Personnel Information Tab:</b> Configuration of options for displaying
 * personnel details like time in service, time in rank, and earnings
 * tracking.</li>
 * <li><b>Awards Tab:</b> Options for managing awards given during play,
 * including
 * auto-awards, tier size configurations, and specific award filters.</li>
 * <li><b>Medical Tab:</b> Medical-related settings such as healing time,
 * advanced medical usage, and tougher healing options.</li>
 * <li><b>Prisoners and Dependents Tab:</b> Configuration of prisoner handling
 * and dependent-related rules.</li>
 * <li><b>Salaries Tab:</b> Configuration of salaries based on roles, experience
 * multipliers, and base salary rates.</li>
 * </ul>
 *
 * <p>
 * This class serves as the main controller for the UI components of the
 * Personnel Tab,
 * bridging the user interface with the {@link CampaignOptions} and ensuring the
 * appropriate
 * application of configuration settings.
 * </p>
 */
public class PersonnelTab {
        private static final int PERSONNEL_LABEL_COLUMN_WIDTH = 300;
        private static final int PERSONNEL_CONTROL_COLUMN_WIDTH = 220;

    private final CampaignOptions campaignOptions;
    private PersonnelOptionsModel model;
    private boolean generalPageCreated;
    private boolean awardsPageCreated;
    private boolean medicalPageCreated;
    private boolean informationPageCreated;
    private boolean prisonersAndDependentsPageCreated;

    // start General Tab
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
    private JLabel lblEdgeRefreshPeriod;
    private MMComboBox<EdgeRefreshPeriod> comboEdgeRefreshPeriod;
    private JLabel lblEdgeRefreshCost;
    private JSpinner spnEdgeRefreshCost;
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
    // end General Tab

    // start Personnel Logs Tab
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
    // end Personnel Logs Tab

    // start Personnel Information Tab
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
    // end Personnel Information Tab

    // start Awards Tab
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
    // end Awards Tab

        private CampaignOptionsHeaderPanel medicalHeader;
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
    // end Medical Tab

    // start Prisoners and Dependents Tab
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
    // end Prisoners and Dependents Tab
    // end Salaries Tab

    /**
     * Constructs the {@code PersonnelTab} object with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for
     *                        initializing and managing personnel
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
     * Initializes the components of the Prisoners and Dependents Tab. This includes
     * settings related to prisoners and
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
     * Initializes the components of the Medical Tab. This includes medical-related
     * options such as recovery time,
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
     * Initializes the components of the Awards Tab. This includes settings for
     * managing awards, such as automatic
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
     * Initializes the components of the Personnel Information Tab. This includes
     * settings for tracking and displaying
     * information like service time, rank time, and earnings.
     */
    private void initializePersonnelInformationTab() {
        chkUseTimeInService = new JCheckBox();

        lblTimeInServiceDisplayFormat = new JLabel();
        comboTimeInServiceDisplayFormat = new MMComboBox<>("comboTimeInServiceDisplayFormat",
                TimeInDisplayFormat.values());

        chkUseTimeInRank = new JCheckBox();

        lblTimeInRankDisplayFormat = new JLabel();
        comboTimeInRankDisplayFormat = new MMComboBox<>("comboTimeInRankDisplayFormat",
                TimeInDisplayFormat.values());

        chkTrackTotalEarnings = new JCheckBox();
        chkTrackTotalXPEarnings = new JCheckBox();
        chkShowOriginFaction = new JCheckBox();
    }

    /**
     * Initializes the components of the Personnel Logs Tab. This includes options
     * for personnel log-keeping, such as
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
     * Initializes the components of the General Tab. This includes general
     * personnel-related options, such as tactics,
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
        lblEdgeRefreshPeriod = new JLabel();
        comboEdgeRefreshPeriod = new MMComboBox<>("comboEdgeRefreshPeriod", EdgeRefreshPeriod.values());
        lblEdgeRefreshCost = new JLabel();
        spnEdgeRefreshCost = new JSpinner();
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
     * Creates the components and layout for the General Tab, organizing personnel
     * management settings into specific
     * groups.
     *
     * @return a {@link JPanel} representing the General Tab.
     */
    public JPanel createGeneralTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_wolverine.png";
        generalHeader = new CampaignOptionsHeaderPanel("PersonnelGeneralTab", imageAddress, 5);

        // Contents
        pnlPersonnelGeneralOptions = createGeneralOptionsPanel();
        pnlPersonnelCleanup = createPersonnelCleanUpPanel();
        pnlAdministrators = createAdministratorsPanel();
        pnlBlobCrew = createBlobCrewPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelGeneralTab", "PersonnelGeneralTab", imageAddress)
                .header(generalHeader)
                .quote("personnelGeneralTab")
                .section("lblPersonnelGeneralTab.text",
                        "lblPersonnelGeneralTab.summary",
                        pnlPersonnelGeneralOptions)
                .section("lblPersonnelCleanUpPanel.text",
                        "lblPersonnelCleanUpPanel.summary",
                        pnlPersonnelCleanup,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblAdministratorsPanel.text",
                        "lblAdministratorsPanel.summary",
                        pnlAdministrators,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblBlobCrewPanel.text",
                        "lblBlobCrewPanel.summary",
                        pnlBlobCrew,
                        getMetadata(new Version(0, 50, 12)))
                .build();

        generalPageCreated = true;
        updateGeneralControlsFromModel();

        return panel;
    }

    /**
     * Creates the panel for general personnel options in the General Tab.
     *
     * @return a {@link JPanel} containing checkboxes for various personnel
     *         management settings.
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

        lblEdgeRefreshPeriod = new CampaignOptionsLabel("EdgeRefreshPeriod", getMetadata(new Version(0, 51, 0)));
        lblEdgeRefreshPeriod.addMouseListener(createTipPanelUpdater(generalHeader, "EdgeRefreshPeriod"));
        comboEdgeRefreshPeriod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EdgeRefreshPeriod) {
                    list.setToolTipText(wordWrap(((EdgeRefreshPeriod) value).getTooltip()));
                }
                return this;
            }
        });
        comboEdgeRefreshPeriod.addMouseListener(createTipPanelUpdater(generalHeader, "EdgeRefreshPeriod"));

        lblEdgeRefreshCost = new CampaignOptionsLabel("EdgeRefreshCost", getMetadata(new Version(0, 51, 0)));
        lblEdgeRefreshCost.addMouseListener(createTipPanelUpdater(generalHeader, "EdgeRefreshCost"));
        spnEdgeRefreshCost = new CampaignOptionsSpinner("EdgeRefreshCost", 20, 0, 100, 1);
        spnEdgeRefreshCost.addMouseListener(createTipPanelUpdater(generalHeader, "EdgeRefreshCost"));
        chkUseImplants = new CampaignOptionsCheckBox("UseImplants");
        chkUseImplants.addMouseListener(createTipPanelUpdater(generalHeader, "UseImplants"));
        chkUseAlternativeQualityAveraging = new CampaignOptionsCheckBox("UseAlternativeQualityAveraging",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAlternativeQualityAveraging.addMouseListener(createTipPanelUpdater(generalHeader,
                "UseAlternativeQualityAveraging"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelGeneralTab",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseTactics,
                chkUseInitiativeBonus,
                chkUseToughness,
                chkUseRandomToughness,
                chkUseArtillery,
                chkUseAbilities,
                chkOnlyCommandersMatterVehicles,
                chkOnlyCommandersMatterInfantry,
                chkOnlyCommandersMatterBattleArmor,
                chkUseEdge,
                chkUseSupportEdge,
                chkUseImplants,
                chkUseAlternativeQualityAveraging);
        panel.addRow(lblEdgeRefreshCost, spnEdgeRefreshCost);
        panel.addRow(lblEdgeRefreshPeriod, comboEdgeRefreshPeriod);

        return panel;
    }

    /**
     * Creates the panel for personnel cleanup options in the General Tab.
     *
     * @return a {@link JPanel} containing options for personnel cleanup, such as
     *         removal exemptions.
     */
    private JPanel createPersonnelCleanUpPanel() {
        // Contents
        chkUsePersonnelRemoval = new CampaignOptionsCheckBox("UsePersonnelRemoval");
        chkUsePersonnelRemoval.addMouseListener(createTipPanelUpdater(generalHeader, "UsePersonnelRemoval"));
        chkUseRemovalExemptCemetery = new CampaignOptionsCheckBox("UseRemovalExemptCemetery");
        chkUseRemovalExemptCemetery
                .addMouseListener(createTipPanelUpdater(generalHeader, "UseRemovalExemptCemetery"));
        chkUseRemovalExemptRetirees = new CampaignOptionsCheckBox("UseRemovalExemptRetirees");
        chkUseRemovalExemptRetirees
                .addMouseListener(createTipPanelUpdater(generalHeader, "UseRemovalExemptRetirees"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelCleanUpPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUsePersonnelRemoval,
                chkUseRemovalExemptCemetery,
                chkUseRemovalExemptRetirees);

        return panel;
    }

    /**
     * Creates the panel for administrative settings in the General Tab.
     *
     * @return a {@link JPanel} containing settings related to administrators, such
     *         as negotiation options.
     */
    private JPanel createAdministratorsPanel() {
        // Contents
        chkAdminsHaveNegotiation = new CampaignOptionsCheckBox("AdminsHaveNegotiation");
        chkAdminsHaveNegotiation.addMouseListener(createTipPanelUpdater(generalHeader, "AdminsHaveNegotiation"));
        chkAdminExperienceLevelIncludeNegotiation = new CampaignOptionsCheckBox(
                "AdminExperienceLevelIncludeNegotiation");
        chkAdminExperienceLevelIncludeNegotiation.addMouseListener(createTipPanelUpdater(generalHeader,
                "AdminExperienceLevelIncludeNegotiation"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AdministratorsPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkAdminsHaveNegotiation,
                chkAdminExperienceLevelIncludeNegotiation);

        return panel;
    }

    /**
     * Creates the panel for blob crew settings in the General Tab.
     *
     * @return a {@link JPanel} containing settings related to blob crews (temporary
     *         personnel pools).
     */
    private JPanel createBlobCrewPanel() {
        // Contents
        chkUseBlobInfantry = new CampaignOptionsCheckBox("UseBlobInfantry", getMetadata(new Version(0, 50, 12)));
        chkUseBlobInfantry.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobInfantry"));
        chkUseBlobBattleArmor = new CampaignOptionsCheckBox("UseBlobBattleArmor",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobBattleArmor.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobBattleArmor"));
        chkUseBlobVehicleCrewGround = new CampaignOptionsCheckBox("UseBlobVehicleCrewGround",
        chkUseBlobVehicleCrewGround = new CampaignOptionsCheckBox("UseBlobVehicleCrewGround",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewGround
                .addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewGround"));
        chkUseBlobVehicleCrewVTOL = new CampaignOptionsCheckBox("UseBlobVehicleCrewVTOL",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewVTOL.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewVTOL"));
        chkUseBlobVehicleCrewNaval = new CampaignOptionsCheckBox("UseBlobVehicleCrewNaval",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewNaval
                .addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVehicleCrewNaval"));
        chkUseBlobVesselPilot = new CampaignOptionsCheckBox("UseBlobVesselPilot",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselPilot.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselPilot"));
        chkUseBlobVesselGunner = new CampaignOptionsCheckBox("UseBlobVesselGunner",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselGunner.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselGunner"));
        chkUseBlobVesselCrew = new CampaignOptionsCheckBox("UseBlobVesselCrew",
                getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselCrew.addMouseListener(createTipPanelUpdater(generalHeader, "UseBlobVesselCrew"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("BlobCrewPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseBlobInfantry,
                chkUseBlobBattleArmor,
                chkUseBlobVehicleCrewGround,
                chkUseBlobVehicleCrewVTOL,
                chkUseBlobVehicleCrewNaval,
                chkUseBlobVesselPilot,
                chkUseBlobVesselGunner,
                chkUseBlobVesselCrew);

        return panel;
    }

    /**
     * Creates the panels and layout for the Awards Tab, including its general and
     * filter components.
     *
     * @return a {@link JPanel} representing the Awards Tab.
     */
    public JPanel createAwardsTab() {
        // Header
                String imageAddress = getImageDirectory() + "logo_outworld_alliance.png";
                awardsHeader = new CampaignOptionsHeaderPanel("AwardsTab", imageAddress, 4);

        // Contents
        pnlAwardsGeneralOptions = createAwardsGeneralOptionsPanel();
        pnlAutoAwardsFilter = createAutoAwardsFilterPanel();

        txtAwardSetFilterList = new JTextArea(10, 1);
        txtAwardSetFilterList.setLineWrap(true);
        txtAwardSetFilterList.setWrapStyleWord(true);
        txtAwardSetFilterList.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardSetFilterList"));
        txtAwardSetFilterList.setToolTipText(wordWrap(getTextAt(getCampaignOptionsResourceBundle(),
                "lblAwardSetFilterList.tooltip")));
        txtAwardSetFilterList.setName("txtAwardSetFilterList");
        txtAwardSetFilterList.setText("");
        JScrollPane scrollAwardSetFilterList = new JScrollPane(txtAwardSetFilterList);
        scrollAwardSetFilterList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollAwardSetFilterList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JPanel pnlAwardSetFilter = createAwardSetFilterPanel(scrollAwardSetFilterList);
        JPanel panel = CampaignOptionsPagePanel.builder("AwardsTab", "AwardsTab", imageAddress)
                .header(awardsHeader)
                .quote("awardsTab")
                .section("lblAwardsTab.text", "lblAwardsTab.summary", pnlAwardsGeneralOptions)
                .section("lblAutoAwardsFilterPanel.text",
                        "lblAutoAwardsFilterPanel.summary",
                        pnlAutoAwardsFilter)
                .section("lblAwardsTabBottom.text",
                        "lblAwardsTabBottom.summary",
                        pnlAwardSetFilter,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT))
                .build();

        awardsPageCreated = true;
        updateAwardsControlsFromModel();

        return panel;
    }

    /**
     * Creates the panel for general award configuration settings in the Awards Tab.
     *
     * @return a {@link JPanel} containing settings for awards, such as bonus style
     *         and auto awards.
     */
    JPanel createAwardsGeneralOptionsPanel() {
        // Contents
        lblAwardBonusStyle = new CampaignOptionsLabel("AwardBonusStyle");
        lblAwardBonusStyle.addMouseListener(createTipPanelUpdater(awardsHeader, "AwardBonusStyle"));
        comboAwardBonusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index,
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AwardsGeneralOptionsPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblAwardBonusStyle, comboAwardBonusStyle);
        panel.addRow(lblAwardTierSize, spnAwardTierSize);
        panel.addCheckBoxGrid(2,
                chkEnableAutoAwards,
                chkIssuePosthumousAwards,
                chkIssueBestAwardOnly,
                chkIgnoreStandardSet);

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
        chkEnableFactionHunterAwards
                .addMouseListener(createTipPanelUpdater(awardsHeader, "EnableFactionHunterAwards"));
        chkEnableInjuryAwards = new CampaignOptionsCheckBox("EnableInjuryAwards",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableInjuryAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableInjuryAwards"));
        chkEnableIndividualKillAwards = new CampaignOptionsCheckBox("EnableIndividualKillAwards");
        chkEnableIndividualKillAwards.addMouseListener(createTipPanelUpdater(awardsHeader,
                "EnableIndividualKillAwards"));
        chkEnableFormationKillAwards = new CampaignOptionsCheckBox("EnableFormationKillAwards");
        chkEnableFormationKillAwards
                .addMouseListener(createTipPanelUpdater(awardsHeader, "EnableFormationKillAwards"));
        chkEnableRankAwards = new CampaignOptionsCheckBox("EnableRankAwards");
        chkEnableRankAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableRankAwards"));
        chkEnableScenarioAwards = new CampaignOptionsCheckBox("EnableScenarioAwards");
        chkEnableScenarioAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableScenarioAwards"));
        chkEnableSkillAwards = new CampaignOptionsCheckBox("EnableSkillAwards");
        chkEnableSkillAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableSkillAwards"));
        chkEnableTheatreOfWarAwards = new CampaignOptionsCheckBox("EnableTheatreOfWarAwards");
        chkEnableTheatreOfWarAwards
                .addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTheatreOfWarAwards"));
        chkEnableTimeAwards = new CampaignOptionsCheckBox("EnableTimeAwards",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableTimeAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTimeAwards"));
        chkEnableTrainingAwards = new CampaignOptionsCheckBox("EnableTrainingAwards",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableTrainingAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableTrainingAwards"));
        chkEnableMiscAwards = new CampaignOptionsCheckBox("EnableMiscAwards");
        chkEnableMiscAwards.addMouseListener(createTipPanelUpdater(awardsHeader, "EnableMiscAwards"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AutoAwardsFilterPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkEnableContractAwards,
                chkEnableFactionHunterAwards,
                chkEnableInjuryAwards,
                chkEnableIndividualKillAwards,
                chkEnableFormationKillAwards,
                chkEnableRankAwards,
                chkEnableScenarioAwards,
                chkEnableSkillAwards,
                chkEnableTheatreOfWarAwards,
                chkEnableTimeAwards,
                chkEnableTrainingAwards,
                chkEnableMiscAwards);

        return panel;
    }

    private JPanel createAwardSetFilterPanel(JScrollPane scrollAwardSetFilterList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlAwardSetFilterPanel");
        panel.setOpaque(false);
        panel.add(scrollAwardSetFilterList, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the layout for the Medical Tab, combining components related to
     * medical settings.
     *
     * @return a {@link JPanel} containing medical-related settings.
     */
    public JPanel createMedicalTab() {
        // Header
        // start Medical Tab
                String imageAddress = getImageDirectory() + "logo_duchy_of_tamarind_abbey.png";
                medicalHeader = new CampaignOptionsHeaderPanel("MedicalTab", imageAddress, 3);

        // Contents
        chkUseAdvancedMedical = new CampaignOptionsCheckBox("UseAdvancedMedical",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.DOCUMENTED));
        chkUseAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader, "UseAdvancedMedical"));

        lblHealWaitingPeriod = new CampaignOptionsLabel("HealWaitingPeriod");
        lblHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "HealWaitingPeriod"));
        spnHealWaitingPeriod = new CampaignOptionsSpinner("HealWaitingPeriod", 1, 1, 30, 1);
        spnHealWaitingPeriod.addMouseListener(createTipPanelUpdater(medicalHeader, "HealWaitingPeriod"));

        lblNaturalHealWaitingPeriod = new CampaignOptionsLabel("NaturalHealWaitingPeriod");
        lblNaturalHealWaitingPeriod
                .addMouseListener(createTipPanelUpdater(medicalHeader, "NaturalHealWaitingPeriod"));
        spnNaturalHealWaitingPeriod = new CampaignOptionsSpinner("NaturalHealWaitingPeriod", 1, 1, 365, 1);
        spnNaturalHealWaitingPeriod
                .addMouseListener(createTipPanelUpdater(medicalHeader, "NaturalHealWaitingPeriod"));

        lblMinimumHitsForVehicles = new CampaignOptionsLabel("MinimumHitsForVehicles");
        lblMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater(medicalHeader, "MinimumHitsForVehicles"));
        spnMinimumHitsForVehicles = new CampaignOptionsSpinner("MinimumHitsForVehicles", 1, 1, 5, 1);
        spnMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater(medicalHeader, "MinimumHitsForVehicles"));

        chkUseRandomHitsForVehicles = new CampaignOptionsCheckBox("UseRandomHitsForVehicles");
        chkUseRandomHitsForVehicles
                .addMouseListener(createTipPanelUpdater(medicalHeader, "UseRandomHitsForVehicles"));

        chkUseTougherHealing = new CampaignOptionsCheckBox("UseTougherHealing",
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseTougherHealing.addMouseListener(createTipPanelUpdater(medicalHeader, "UseTougherHealing"));

        chkUseAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseAlternativeAdvancedMedical",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        chkUseAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
                "UseAlternativeAdvancedMedical"));

        chkUseKinderAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseKinderAlternativeAdvancedMedical",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        chkUseKinderAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater(medicalHeader,
                "UseKinderAlternativeAdvancedMedical"));

        chkUseRandomDiseases = new CampaignOptionsCheckBox("UseRandomDiseases",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        chkUseRandomDiseases.addMouseListener(createTipPanelUpdater(medicalHeader,
                "UseRandomDiseases"));

        lblMaximumPatients = new CampaignOptionsLabel("MaximumPatients");
        lblMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));
        spnMaximumPatients = new CampaignOptionsSpinner("MaximumPatients", 25, 1, 100, 1);
        spnMaximumPatients.addMouseListener(createTipPanelUpdater(medicalHeader, "MaximumPatients"));

        chkDoctorsUseAdministration = new CampaignOptionsCheckBox("DoctorsUseAdministration",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkDoctorsUseAdministration
                .addMouseListener(createTipPanelUpdater(medicalHeader, "DoctorsUseAdministration"));

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

        // Layout the Panels
        final CampaignOptionsFormPanel medicalCapacityPanel = new CampaignOptionsFormPanel("MedicalCapacityPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        medicalCapacityPanel.addRow(lblMaximumPatients, spnMaximumPatients);
        medicalCapacityPanel.addCheckBoxGrid(2,
                chkDoctorsUseAdministration,
                chkUseUsefulMedics,
                chkUseMASHTheatres);
        medicalCapacityPanel.addRow(lblMASHTheatreCapacity, spnMASHTheatreCapacity);

        final CampaignOptionsFormPanel healingChecksPanel = new CampaignOptionsFormPanel("HealingChecksPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        healingChecksPanel.addRow(lblHealWaitingPeriod, spnHealWaitingPeriod);
        healingChecksPanel.addRow(lblNaturalHealWaitingPeriod, spnNaturalHealWaitingPeriod);
        healingChecksPanel.addCheckBox(chkUseRandomHitsForVehicles);
        healingChecksPanel.addRow(lblMinimumHitsForVehicles, spnMinimumHitsForVehicles);

        final CampaignOptionsFormPanel advancedMedicalRulesPanel = new CampaignOptionsFormPanel(
                "AdvancedMedicalRulesPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        advancedMedicalRulesPanel.addCheckBoxGrid(2,
                chkUseAdvancedMedical,
                chkUseTougherHealing,
                chkUseAlternativeAdvancedMedical,
                chkUseKinderAlternativeAdvancedMedical,
                chkUseRandomDiseases);
        JPanel panel = CampaignOptionsPagePanel.builder("MedicalTab", "MedicalTab", imageAddress)
                .header(medicalHeader)
                .quote("medicalTab")
                .section("lblMedicalCapacityPanel.text", "lblMedicalCapacityPanel.summary", medicalCapacityPanel)
                .section("lblHealingChecksPanel.text", "lblHealingChecksPanel.summary", healingChecksPanel)
                .section("lblAdvancedMedicalRulesPanel.text",
                        "lblAdvancedMedicalRulesPanel.summary",
                        advancedMedicalRulesPanel)
                .build();

        medicalPageCreated = true;
        updateMedicalControlsFromModel();

        return panel;
    }

    /**
     * Creates the layout for the Personnel Information Tab, including its
     * components for displaying personnel
     * information and logs.
     *
     * @return a {@link JPanel} representing the Personnel Information Tab.
     */
    public JPanel createPersonnelInformationTab() {
        // Header
                String imageAddress = getImageDirectory() + "logo_rasalhague_dominion.png";
                personnelInformationHeader = new CampaignOptionsHeaderPanel("PersonnelInformation", imageAddress, 3);

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
        chkTrackTotalEarnings
                .addMouseListener(createTipPanelUpdater(personnelInformationHeader, "TrackTotalEarnings"));
        chkTrackTotalXPEarnings = new CampaignOptionsCheckBox("TrackTotalXPEarnings");
        chkTrackTotalXPEarnings.addMouseListener(createTipPanelUpdater(personnelInformationHeader,
                "TrackTotalXPEarnings"));
        chkShowOriginFaction = new CampaignOptionsCheckBox("ShowOriginFaction");
        chkShowOriginFaction
                .addMouseListener(createTipPanelUpdater(personnelInformationHeader, "ShowOriginFaction"));

        JPanel pnlPersonnelLogs = createPersonnelLogsPanel();

        // Layout the Panel
        final CampaignOptionsFormPanel personnelInformationPanel = new CampaignOptionsFormPanel(
                "PersonnelInformation",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        personnelInformationPanel.addCheckBoxGrid(2,
                chkUseTimeInService,
                chkUseTimeInRank);
        personnelInformationPanel.addRow(lblTimeInServiceDisplayFormat, comboTimeInServiceDisplayFormat);
        personnelInformationPanel.addRow(lblTimeInRankDisplayFormat, comboTimeInRankDisplayFormat);
        personnelInformationPanel.addCheckBoxGrid(2,
                chkTrackTotalEarnings,
                chkTrackTotalXPEarnings,
                chkShowOriginFaction);
        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelInformation", "PersonnelInformation", imageAddress)
                .header(personnelInformationHeader)
                .quote("personnelInformationTab")
                .section("lblPersonnelInformation.text",
                        "lblPersonnelInformation.summary",
                        personnelInformationPanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblPersonnelLogsPanel.text", "lblPersonnelLogsPanel.summary", pnlPersonnelLogs)
                .build();

        informationPageCreated = true;
        updateInformationControlsFromModel();

        return panel;
    }

    /**
     * Creates a sub-panel for managing personnel log settings within the Personnel
     * Information Tab.
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
        chkDisplayScenarioLog
                .addMouseListener(createTipPanelUpdater(personnelInformationHeader, "DisplayScenarioLog"));
        chkDisplayKillRecord = new CampaignOptionsCheckBox("DisplayKillRecord");
        chkDisplayKillRecord
                .addMouseListener(createTipPanelUpdater(personnelInformationHeader, "DisplayKillRecord"));
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelLogsPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseTransfers,
                chkUseExtendedTOEForceName,
                chkPersonnelLogSkillGain,
                chkPersonnelLogAbilityGain,
                chkPersonnelLogEdgeGain,
                chkDisplayPersonnelLog,
                chkDisplayScenarioLog,
                chkDisplayKillRecord,
                chkDisplayMedicalRecord,
                chkDisplayPatientRecord,
                chkDisplayAssignmentRecord,
                chkDisplayPerformanceRecord);

        return panel;
    }

    /**
     * Creates the layout for the Prisoners and Dependents Tab, organizing settings
     * for prisoner handling and dependent
     * management.
     *
     * @return a {@link JPanel} containing the Prisoners and Dependents Tab
     *         components.
     */
    public JPanel createPrisonersAndDependentsTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_illyrian_palatinate.png";
        prisonersAndDependentsHeader = new CampaignOptionsHeaderPanel("PrisonersAndDependentsTab", imageAddress, 6);

        // Contents
        prisonerPanel = createPrisonersPanel();
        dependentsPanel = createDependentsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("PrisonersAndDependentsTab",
                        "PrisonersAndDependentsTab",
                        imageAddress)
                .header(prisonersAndDependentsHeader)
                .quote("prisonersAndDependentsTab")
                .section("lblPrisonersPanel.text",
                        "lblPrisonersPanel.summary",
                        prisonerPanel,
                        getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED))
                .section("lblDependentsPanel.text",
                        "lblDependentsPanel.summary",
                        dependentsPanel,
                        getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED))
                .build();

        prisonersAndDependentsPageCreated = true;
        updatePrisonersAndDependentsControlsFromModel();

        return panel;
    }

    /**
     * Creates the panel for configuring prisoner settings in the Prisoners and
     * Dependents Tab.
     *
     * @return a {@link JPanel} containing prisoner-related options such as capture
     *         style and status.
     */
    private JPanel createPrisonersPanel() {
        // Contents
        lblPrisonerCaptureStyle = new CampaignOptionsLabel("PrisonerCaptureStyle");
        lblPrisonerCaptureStyle.addMouseListener(createTipPanelUpdater(prisonersAndDependentsHeader,
                "PrisonerCaptureStyle"));
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index,
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PrisonersPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPrisonerCaptureStyle, comboPrisonerCaptureStyle);
        panel.addCheckBoxGrid(2,
                chkUseFunctionalEscapeArtist,
                chkResetTemporaryPrisonerCapacity);

        return panel;
    }

    /**
     * Creates the panel for configuring dependent settings in the Prisoners and
     * Dependents Tab.
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DependentsPanel",
                PERSONNEL_LABEL_COLUMN_WIDTH,
                PERSONNEL_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseRandomDependentAddition,
                chkUseRandomDependentRemoval);
        panel.addRow(lblDependentProfessionDieSize, spnDependentProfessionDieSize);
        panel.addRow(lblCivilianProfessionDieSize, spnCivilianProfessionDieSize);

        return panel;
    }

    /**
     * Shortcut method to load default {@link CampaignOptions} values into the tab
     * components.
     */
    public void loadValuesFromCampaignOptions(Version version) {
        loadValuesFromCampaignOptions(null, version);
    }

    /**
     * Loads and applies configuration values from the provided
     * {@link CampaignOptions} object, or uses the default
     * campaign options if none are provided. The configuration includes general
     * settings, personnel logs, personnel
     * information, awards, medical settings, prisoner and dependent settings, and
     * salary-related options. It also
     * adjusts certain values based on the version of the application.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} object to load
     *                              settings from. If null, default campaign
     *                              options will be used.
     * @param version               the version of the application, used to
     *                              determine adjustments for compatibility.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions, Version version) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new PersonnelOptionsModel(options);
        updateCreatedControlsFromModel();
    }
    }

    /**
     * Applies the modified personnel tab settings to the repository's campaign
     * options. If no preset
     * {@link CampaignOptions} is provided, the changes are applied to the current
     * options.
     *
     * @param campaign              the {@link Campaign} object, representing the
     *                              current campaign state.
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply
     *                              changes to.
     */
    public void applyCampaignOptionsToCampaign(Campaign campaign, @Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(campaign, options);
    }
    }

    private void updateCreatedControlsFromModel() {
        updateGeneralControlsFromModel();
        updateAwardsControlsFromModel();
        updateMedicalControlsFromModel();
        updateInformationControlsFromModel();
        updatePrisonersAndDependentsControlsFromModel();
    }

    private void updateGeneralControlsFromModel() {
        if (!generalPageCreated || model == null) {
            return;
        }

        chkUseTactics.setSelected(model.useTactics);
        chkUseInitiativeBonus.setSelected(model.useInitiativeBonus);
        chkUseToughness.setSelected(model.useToughness);
        chkUseRandomToughness.setSelected(model.useRandomToughness);
        chkUseArtillery.setSelected(model.useArtillery);
        chkUseAbilities.setSelected(model.useAbilities);
        chkOnlyCommandersMatterVehicles.setSelected(model.onlyCommandersMatterVehicles);
        chkOnlyCommandersMatterInfantry.setSelected(model.onlyCommandersMatterInfantry);
        chkOnlyCommandersMatterBattleArmor.setSelected(model.onlyCommandersMatterBattleArmor);
        chkUseEdge.setSelected(model.useEdge);
        chkUseSupportEdge.setSelected(model.useSupportEdge);
        comboEdgeRefreshPeriod.setSelectedItem(model.edgeRefreshPeriod);
        spnEdgeRefreshCost.setValue(model.edgeRefreshCost);
        chkUseImplants.setSelected(model.useImplants);
        chkUseAlternativeQualityAveraging.setSelected(model.alternativeQualityAveraging);
        chkUsePersonnelRemoval.setSelected(model.usePersonnelRemoval);
        chkUseRemovalExemptCemetery.setSelected(model.useRemovalExemptCemetery);
        chkUseRemovalExemptRetirees.setSelected(model.useRemovalExemptRetirees);
        chkAdminsHaveNegotiation.setSelected(model.adminsHaveNegotiation);
        chkAdminExperienceLevelIncludeNegotiation.setSelected(model.adminExperienceLevelIncludeNegotiation);
        chkUseBlobInfantry.setSelected(model.useBlobInfantry);
        chkUseBlobBattleArmor.setSelected(model.useBlobBattleArmor);
        chkUseBlobVehicleCrewGround.setSelected(model.useBlobVehicleCrewGround);
        chkUseBlobVehicleCrewVTOL.setSelected(model.useBlobVehicleCrewVTOL);
        chkUseBlobVehicleCrewNaval.setSelected(model.useBlobVehicleCrewNaval);
        chkUseBlobVesselPilot.setSelected(model.useBlobVesselPilot);
        chkUseBlobVesselGunner.setSelected(model.useBlobVesselGunner);
        chkUseBlobVesselCrew.setSelected(model.useBlobVesselCrew);
    }

    private void updateAwardsControlsFromModel() {
        if (!awardsPageCreated || model == null) {
            return;
        }

        comboAwardBonusStyle.setSelectedItem(model.awardBonusStyle);
        spnAwardTierSize.setValue(model.awardTierSize);
        chkEnableAutoAwards.setSelected(model.enableAutoAwards);
        chkIssuePosthumousAwards.setSelected(model.issuePosthumousAwards);
        chkIssueBestAwardOnly.setSelected(model.issueBestAwardOnly);
        chkIgnoreStandardSet.setSelected(model.ignoreStandardSet);
        chkEnableContractAwards.setSelected(model.enableContractAwards);
        chkEnableFactionHunterAwards.setSelected(model.enableFactionHunterAwards);
        chkEnableInjuryAwards.setSelected(model.enableInjuryAwards);
        chkEnableIndividualKillAwards.setSelected(model.enableIndividualKillAwards);
        chkEnableFormationKillAwards.setSelected(model.enableFormationKillAwards);
        chkEnableRankAwards.setSelected(model.enableRankAwards);
        chkEnableScenarioAwards.setSelected(model.enableScenarioAwards);
        chkEnableSkillAwards.setSelected(model.enableSkillAwards);
        chkEnableTheatreOfWarAwards.setSelected(model.enableTheatreOfWarAwards);
        chkEnableTimeAwards.setSelected(model.enableTimeAwards);
        chkEnableTrainingAwards.setSelected(model.enableTrainingAwards);
        chkEnableMiscAwards.setSelected(model.enableMiscAwards);
        txtAwardSetFilterList.setText(model.awardSetFilterList);
    }

    private void updateMedicalControlsFromModel() {
        if (!medicalPageCreated || model == null) {
            return;
        }

        chkUseAdvancedMedical.setSelected(model.useAdvancedMedical);
        spnHealWaitingPeriod.setValue(model.healingWaitingPeriod);
        spnNaturalHealWaitingPeriod.setValue(model.naturalHealingWaitingPeriod);
        spnMinimumHitsForVehicles.setValue(model.minimumHitsForVehicles);
        chkUseRandomHitsForVehicles.setSelected(model.useRandomHitsForVehicles);
        chkUseTougherHealing.setSelected(model.tougherHealing);
        chkUseAlternativeAdvancedMedical.setSelected(model.useAlternativeAdvancedMedical);
        chkUseKinderAlternativeAdvancedMedical.setSelected(model.useKinderAlternativeAdvancedMedical);
        chkUseRandomDiseases.setSelected(model.useRandomDiseases);
        spnMaximumPatients.setValue(model.maximumPatients);
        chkDoctorsUseAdministration.setSelected(model.doctorsUseAdministration);
        chkUseUsefulMedics.setSelected(model.useUsefulMedics);
        chkUseMASHTheatres.setSelected(model.useMASHTheatres);
        spnMASHTheatreCapacity.setValue(model.mashTheatreCapacity);
    }

    private void updateInformationControlsFromModel() {
        if (!informationPageCreated || model == null) {
            return;
        }

        chkUseTransfers.setSelected(model.useTransfers);
        chkUseExtendedTOEForceName.setSelected(model.useExtendedTOEForceName);
        chkPersonnelLogSkillGain.setSelected(model.personnelLogSkillGain);
        chkPersonnelLogAbilityGain.setSelected(model.personnelLogAbilityGain);
        chkPersonnelLogEdgeGain.setSelected(model.personnelLogEdgeGain);
        chkDisplayPersonnelLog.setSelected(model.displayPersonnelLog);
        chkDisplayScenarioLog.setSelected(model.displayScenarioLog);
        chkDisplayKillRecord.setSelected(model.displayKillRecord);
        chkDisplayMedicalRecord.setSelected(model.displayMedicalRecord);
        chkDisplayPatientRecord.setSelected(model.displayPatientRecord);
        chkDisplayAssignmentRecord.setSelected(model.displayAssignmentRecord);
        chkDisplayPerformanceRecord.setSelected(model.displayPerformanceRecord);
        chkUseTimeInService.setSelected(model.useTimeInService);
        comboTimeInServiceDisplayFormat.setSelectedItem(model.timeInServiceDisplayFormat);
        chkUseTimeInRank.setSelected(model.useTimeInRank);
        comboTimeInRankDisplayFormat.setSelectedItem(model.timeInRankDisplayFormat);
        chkTrackTotalEarnings.setSelected(model.trackTotalEarnings);
        chkTrackTotalXPEarnings.setSelected(model.trackTotalXPEarnings);
        chkShowOriginFaction.setSelected(model.showOriginFaction);
    }

    private void updatePrisonersAndDependentsControlsFromModel() {
        if (!prisonersAndDependentsPageCreated || model == null) {
            return;
        }

        comboPrisonerCaptureStyle.setSelectedItem(model.prisonerCaptureStyle);
        chkUseFunctionalEscapeArtist.setSelected(model.useFunctionalEscapeArtist);
        chkResetTemporaryPrisonerCapacity.setSelected(model.resetTemporaryPrisonerCapacity);
        chkUseRandomDependentAddition.setSelected(model.useRandomDependentAddition);
        chkUseRandomDependentRemoval.setSelected(model.useRandomDependentRemoval);
        spnDependentProfessionDieSize.setValue(model.dependentProfessionDieSize);
        spnCivilianProfessionDieSize.setValue(model.civilianProfessionDieSize);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromGeneralControls();
        updateModelFromAwardsControls();
        updateModelFromMedicalControls();
        updateModelFromInformationControls();
        updateModelFromPrisonersAndDependentsControls();
    }

    private void updateModelFromGeneralControls() {
        if (!generalPageCreated || model == null) {
            return;
        }

        model.useTactics = chkUseTactics.isSelected();
        model.useInitiativeBonus = chkUseInitiativeBonus.isSelected();
        model.useToughness = chkUseToughness.isSelected();
        model.useRandomToughness = chkUseRandomToughness.isSelected();
        model.useArtillery = chkUseArtillery.isSelected();
        model.useAbilities = chkUseAbilities.isSelected();
        model.onlyCommandersMatterVehicles = chkOnlyCommandersMatterVehicles.isSelected();
        model.onlyCommandersMatterInfantry = chkOnlyCommandersMatterInfantry.isSelected();
        model.onlyCommandersMatterBattleArmor = chkOnlyCommandersMatterBattleArmor.isSelected();
        model.useEdge = chkUseEdge.isSelected();
        model.useSupportEdge = chkUseSupportEdge.isSelected();
        model.edgeRefreshPeriod = comboEdgeRefreshPeriod.getSelectedItem();
        model.edgeRefreshCost = (int) spnEdgeRefreshCost.getValue();
        model.useImplants = chkUseImplants.isSelected();
        model.alternativeQualityAveraging = chkUseAlternativeQualityAveraging.isSelected();
        model.usePersonnelRemoval = chkUsePersonnelRemoval.isSelected();
        model.useRemovalExemptCemetery = chkUseRemovalExemptCemetery.isSelected();
        model.useRemovalExemptRetirees = chkUseRemovalExemptRetirees.isSelected();
        model.adminsHaveNegotiation = chkAdminsHaveNegotiation.isSelected();
        model.adminExperienceLevelIncludeNegotiation = chkAdminExperienceLevelIncludeNegotiation.isSelected();
        model.useBlobInfantry = chkUseBlobInfantry.isSelected();
        model.useBlobBattleArmor = chkUseBlobBattleArmor.isSelected();
        model.useBlobVehicleCrewGround = chkUseBlobVehicleCrewGround.isSelected();
        model.useBlobVehicleCrewVTOL = chkUseBlobVehicleCrewVTOL.isSelected();
        model.useBlobVehicleCrewNaval = chkUseBlobVehicleCrewNaval.isSelected();
        model.useBlobVesselPilot = chkUseBlobVesselPilot.isSelected();
        model.useBlobVesselGunner = chkUseBlobVesselGunner.isSelected();
        model.useBlobVesselCrew = chkUseBlobVesselCrew.isSelected();
    }

    private void updateModelFromAwardsControls() {
        if (!awardsPageCreated || model == null) {
            return;
        }

        model.awardBonusStyle = comboAwardBonusStyle.getSelectedItem();
        model.awardTierSize = (int) spnAwardTierSize.getValue();
        model.enableAutoAwards = chkEnableAutoAwards.isSelected();
        model.issuePosthumousAwards = chkIssuePosthumousAwards.isSelected();
        model.issueBestAwardOnly = chkIssueBestAwardOnly.isSelected();
        model.ignoreStandardSet = chkIgnoreStandardSet.isSelected();
        model.enableContractAwards = chkEnableContractAwards.isSelected();
        model.enableFactionHunterAwards = chkEnableFactionHunterAwards.isSelected();
        model.enableInjuryAwards = chkEnableInjuryAwards.isSelected();
        model.enableIndividualKillAwards = chkEnableIndividualKillAwards.isSelected();
        model.enableFormationKillAwards = chkEnableFormationKillAwards.isSelected();
        model.enableRankAwards = chkEnableRankAwards.isSelected();
        model.enableScenarioAwards = chkEnableScenarioAwards.isSelected();
        model.enableSkillAwards = chkEnableSkillAwards.isSelected();
        model.enableTheatreOfWarAwards = chkEnableTheatreOfWarAwards.isSelected();
        model.enableTimeAwards = chkEnableTimeAwards.isSelected();
        model.enableTrainingAwards = chkEnableTrainingAwards.isSelected();
        model.enableMiscAwards = chkEnableMiscAwards.isSelected();
        model.awardSetFilterList = txtAwardSetFilterList.getText();
    }

    private void updateModelFromMedicalControls() {
        if (!medicalPageCreated || model == null) {
            return;
        }

        model.useAdvancedMedical = chkUseAdvancedMedical.isSelected();
        model.healingWaitingPeriod = (int) spnHealWaitingPeriod.getValue();
        model.naturalHealingWaitingPeriod = (int) spnNaturalHealWaitingPeriod.getValue();
        model.minimumHitsForVehicles = (int) spnMinimumHitsForVehicles.getValue();
        model.useRandomHitsForVehicles = chkUseRandomHitsForVehicles.isSelected();
        model.tougherHealing = chkUseTougherHealing.isSelected();
        model.useAlternativeAdvancedMedical = chkUseAlternativeAdvancedMedical.isSelected();
        model.useKinderAlternativeAdvancedMedical = chkUseKinderAlternativeAdvancedMedical.isSelected();
        model.useRandomDiseases = chkUseRandomDiseases.isSelected();
        model.maximumPatients = (int) spnMaximumPatients.getValue();
        model.doctorsUseAdministration = chkDoctorsUseAdministration.isSelected();
        model.useUsefulMedics = chkUseUsefulMedics.isSelected();
        model.useMASHTheatres = chkUseMASHTheatres.isSelected();
        model.mashTheatreCapacity = (int) spnMASHTheatreCapacity.getValue();
    }

    private void updateModelFromInformationControls() {
        if (!informationPageCreated || model == null) {
            return;
        }

        model.useTransfers = chkUseTransfers.isSelected();
        model.useExtendedTOEForceName = chkUseExtendedTOEForceName.isSelected();
        model.personnelLogSkillGain = chkPersonnelLogSkillGain.isSelected();
        model.personnelLogAbilityGain = chkPersonnelLogAbilityGain.isSelected();
        model.personnelLogEdgeGain = chkPersonnelLogEdgeGain.isSelected();
        model.displayPersonnelLog = chkDisplayPersonnelLog.isSelected();
        model.displayScenarioLog = chkDisplayScenarioLog.isSelected();
        model.displayKillRecord = chkDisplayKillRecord.isSelected();
        model.displayMedicalRecord = chkDisplayMedicalRecord.isSelected();
        model.displayPatientRecord = chkDisplayPatientRecord.isSelected();
        model.displayAssignmentRecord = chkDisplayAssignmentRecord.isSelected();
        model.displayPerformanceRecord = chkDisplayPerformanceRecord.isSelected();
        model.useTimeInService = chkUseTimeInService.isSelected();
        model.timeInServiceDisplayFormat = comboTimeInServiceDisplayFormat.getSelectedItem();
        model.useTimeInRank = chkUseTimeInRank.isSelected();
        model.timeInRankDisplayFormat = comboTimeInRankDisplayFormat.getSelectedItem();
        model.trackTotalEarnings = chkTrackTotalEarnings.isSelected();
        model.trackTotalXPEarnings = chkTrackTotalXPEarnings.isSelected();
        model.showOriginFaction = chkShowOriginFaction.isSelected();
    }

    private void updateModelFromPrisonersAndDependentsControls() {
        if (!prisonersAndDependentsPageCreated || model == null) {
            return;
        }

        model.prisonerCaptureStyle = comboPrisonerCaptureStyle.getSelectedItem();
        model.useFunctionalEscapeArtist = chkUseFunctionalEscapeArtist.isSelected();
        model.resetTemporaryPrisonerCapacity = chkResetTemporaryPrisonerCapacity.isSelected();
        model.useRandomDependentAddition = chkUseRandomDependentAddition.isSelected();
        model.useRandomDependentRemoval = chkUseRandomDependentRemoval.isSelected();
        model.dependentProfessionDieSize = (int) spnDependentProfessionDieSize.getValue();
        model.civilianProfessionDieSize = (int) spnCivilianProfessionDieSize.getValue();
    }

}
