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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.TableColumn;

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.model.RankTableModel;
import mekhq.gui.panes.RankSystemsPane;

/**
 * The `BiographyTab` class is responsible for managing the biography-related
 * settings in the campaign options tab
 * within the MekHQ application. It provides an interface for configuring
 * various campaign settings, such as:
 * <ul>
 * <li>General campaign settings like gender distribution and familial
 * relationships.</li>
 * <li>Background options, including randomized personality traits and origin
 * determination.</li>
 * <li>Death-related settings such as probability of random deaths,
 * age-group-specific deaths, etc.</li>
 * <li>Education module settings for academic progression, dropout chances, and
 * related configurations.</li>
 * <li>Random name generation and portrait assignment based on roles and
 * factions.</li>
 * <li>Rank and hierarchy management for campaign personnel.</li>
 * </ul>
 * <p>
 * The class includes methods to initialize, load, and configure settings while
 * providing GUI tools to enable user
 * interaction. It also integrates with the current `Campaign` and
 * `CampaignOptions` objects to synchronize settings.
 * This class serves as the backbone for displaying and managing the "Biography"
 * tab in the campaign options dialog.
 */
public class BiographyTab {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int FORM_CONTROL_COLUMN_WIDTH = 240;
    private static final int CHECKBOX_GRID_COLUMNS = 2;
    private static final int RANK_SYSTEMS_PANEL_WIDTH = 860;
    private static final int RANK_TABLE_HEIGHT = 260;
    private static final int RANK_RATE_COLUMN_WIDTH = 60;
    private static final int RANK_NAME_COLUMN_WIDTH = 150;
    private static final int RANK_OFFICER_COLUMN_WIDTH = 90;
    private static final int RANK_PAY_MULTIPLIER_COLUMN_WIDTH = 120;

    private final Campaign campaign;
    private final GeneralTab generalTab;
    private final CampaignOptions campaignOptions;
    private final RandomOriginOptions randomOriginOptions;
    private BiographyOptionsModel model;
    private boolean generalPageCreated;
    private boolean backgroundsPageCreated;
    private boolean deathPageCreated;
    private boolean educationPageCreated;
    private boolean nameAndPortraitPageCreated;

    // start General Tab
    private CampaignOptionsHeaderPanel generalHeader;
    private JCheckBox chkUseDylansRandomXP;
    private JLabel lblGender;
    private JSpinner spnGender;
    private JLabel lblNonBinaryDiceSize;
    private JSpinner spnNonBinaryDiceSize;
    private JLabel lblFamilyDisplayLevel;
    private MMComboBox<FamilialRelationshipDisplayLevel> comboFamilyDisplayLevel;
    private JPanel pnlAnniversariesPanel;
    private JCheckBox chkAnnounceOfficersOnly;
    private JCheckBox chkAnnounceBirthdays;
    private JCheckBox chkAnnounceChildBirthdays;
    private JCheckBox chkAnnounceRecruitmentAnniversaries;
    private JPanel pnlLifeEvents;
    private JCheckBox chkShowLifeEventDialogBirths;
    private JCheckBox chkShowLifeEventDialogComingOfAge;
    private JCheckBox chkShowLifeEventDialogCelebrations;
    private JPanel pnlComingOfAge;
    private JCheckBox chkVeterancySPAs;
    private JCheckBox chkAwardRelevantVeterancySPAs;
    private JCheckBox chkComingOfAgeSPAs;
    private JCheckBox chkRewardComingOfAgeRPSkills;
    // end General Tab

    // start Backgrounds Tab
    private CampaignOptionsHeaderPanel backgroundHeader;
    private JPanel pnlRandomBackgrounds;
    private JCheckBox chkUseRandomPersonalities;
    private JCheckBox chkUseRandomPersonalityReputation;
    private JCheckBox chkUseReasoningXpMultiplier;
    private JCheckBox chkUseSimulatedRelationships;
    private JPanel pnlRandomOriginOptions;
    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JCheckBox chkRandomizeAroundSpecifiedPlanet;
    private JCheckBox chkSpecifiedSystemFactionSpecific;
    private JLabel lblSpecifiedSystem;
    private MMComboBox<PlanetarySystem> comboSpecifiedSystem;
    private JLabel lblSpecifiedPlanet;
    private MMComboBox<Planet> comboSpecifiedPlanet;
    private JLabel lblOriginSearchRadius;
    private JSpinner spnOriginSearchRadius;
    private JLabel lblOriginDistanceScale;
    private JSpinner spnOriginDistanceScale;
    private JCheckBox chkAllowClanOrigins;
    private JCheckBox chkExtraRandomOrigin;
    // end Backgrounds Tab

    // start Death Tab
    private CampaignOptionsHeaderPanel deathHeader;
    private JCheckBox chkUseRandomDeathSuicideCause;
    private JLabel lblRandomDeathMultiplier;
    private JSpinner spnRandomDeathMultiplier;

    private JPanel pnlDeathAgeGroup;
    private Map<AgeGroup, JCheckBox> chkEnabledRandomDeathAgeGroups;
    // end Death Tab

    // start Education Tab
    private CampaignOptionsHeaderPanel educationHeader;
    private JCheckBox chkUseEducationModule;
    private JLabel lblCurriculumXpRate;
    private JSpinner spnCurriculumXpRate;
    private JLabel lblMaximumJumpCount;
    private JSpinner spnMaximumJumpCount;
    private JCheckBox chkUseReeducationCamps;
    private JCheckBox chkEnableOverrideRequirements;
    private JCheckBox chkShowIneligibleAcademies;
    private JLabel lblEntranceExamBaseTargetNumber;
    private JSpinner spnEntranceExamBaseTargetNumber;

    private JPanel pnlEnableStandardSets;
    private JCheckBox chkEnableLocalAcademies;
    private JCheckBox chkEnablePrestigiousAcademies;
    private JCheckBox chkEnableUnitEducation;

    private JPanel pnlXpAndSkillBonuses;
    private JCheckBox chkEnableBonuses;
    private JLabel lblFacultyXpMultiplier;
    private JSpinner spnFacultyXpMultiplier;

    private JPanel pnlDropoutChance;
    private JLabel lblAdultDropoutChance;
    private JSpinner spnAdultDropoutChance;
    private JLabel lblChildrenDropoutChance;
    private JSpinner spnChildrenDropoutChance;

    private JPanel pnlAccidentsAndEvents;
    private JCheckBox chkAllAges;
    private JLabel lblMilitaryAcademyAccidents;
    private JSpinner spnMilitaryAcademyAccidents;
    // end Education Tab

    // start Name and Portrait Tab
    private JCheckBox chkUseOriginFactionForNames;
    private JLabel lblFactionNames;
    private MMComboBox<String> comboFactionNames;

    private CampaignOptionsHeaderPanel nameAndPortraitGenerationHeader;
    private JPanel pnlRandomPortrait;
    private List<PersonnelRole> personnelRoles;
    private JCheckBox[] chkUsePortrait;
    private JButton btnEnableAllPortraits;
    private JButton btnDisableAllPortraits;
    private JCheckBox chkAssignPortraitOnRoleChange;
    private JCheckBox chkAllowDuplicatePortraits;
    private JCheckBox chkUseGenderedPortraitsOnly;
    private JCheckBox chkNoRandomPortraitsForChildren;
    private JCheckBox chkChildPortraitsWhenComingOfAge;
    // end Name and Portrait Tab

    // start Rank Tab
    private RankSystemsPane rankSystemsPane;
    // end Rank Tab

    /**
     * Constructs the `BiographyTab` and initializes the campaign and its dependent
     * options.
     *
     * @param campaign   The current `Campaign` object to which the BiographyTab is
     *                   linked. The campaign options and
     *                   origin options are derived from this object.
     * @param generalTab The currently active General Tab.
     */
    public BiographyTab(Campaign campaign, GeneralTab generalTab) {
        this.campaign = campaign;
        this.generalTab = generalTab;
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomOriginOptions = campaignOptions.getRandomOriginOptions();

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the various sections and settings tabs within the BiographyTab.
     * This method organizes the following
     * tabs:
     * <p>
     * <li>General Tab: Handles general campaign settings such as gender
     * distribution and
     * relationship displays.</li>
     * <li>Background Tab: Configures randomized backgrounds for campaign
     * characters.</li>
     * <li>Death Tab: Sets up random death rules and options.</li>
     * <li>Education Tab: Defines education-related gameplay settings.</li>
     * <li>Name and Portrait Tab: Configures rules for name and portrait
     * generation.</li>
     * <li>Rank Tab: Manages the rank systems for campaign personnel.</li>
     * </p>
     */
    private void initialize() {
        initializeGeneralTab();
        initializeBackgroundsTab();
        initializeDeathTab();
        initializeEducationTab();
        initializeNameAndPortraitTab();

        rankSystemsPane = new RankSystemsPane(null, campaign);
    }

    /**
     * Initializes the Name and Portrait tab. The tab allows users to:
     * <ul>
     * <li>Enable or disable the use of origin factions for name generation.</li>
     * <li>Assign portraits to personnel upon role changes.</li>
     * <li>Customize which portraits should be used randomly based on roles.</li>
     * </ul>
     */
    private void initializeNameAndPortraitTab() {
        chkUseOriginFactionForNames = new JCheckBox();
        lblFactionNames = new JLabel();
        comboFactionNames = new MMComboBox<>("comboFactionNames", getFactionNamesModel());
        chkAssignPortraitOnRoleChange = new JCheckBox();
        chkAllowDuplicatePortraits = new JCheckBox();
        chkUseGenderedPortraitsOnly = new JCheckBox();
        chkNoRandomPortraitsForChildren = new JCheckBox();
        chkChildPortraitsWhenComingOfAge = new JCheckBox();

        pnlRandomPortrait = new JPanel();
        personnelRoles = PersonnelRole.getCombatRoles();
        personnelRoles.addAll(PersonnelRole.getSupportRoles());
        chkUsePortrait = new JCheckBox[personnelRoles.size() + 1]; // We're going to properly initialize this later
        btnEnableAllPortraits = new JButton();
        btnDisableAllPortraits = new JButton();
    }

    /**
     * Initializes the Education tab, providing settings such as:
     * <ul>
     * <li>Setting curriculum XP rates.</li>
     * <li>Enabling re-education camps or specific academy requirements.</li>
     * <li>Managing academy dropout chances for adults and children.</li>
     * <li>Supporting the configuration of education-related accidents and
     * events.</li>
     * </ul>
     */
    private void initializeEducationTab() {
        chkUseEducationModule = new JCheckBox();
        lblCurriculumXpRate = new JLabel();
        spnCurriculumXpRate = new JSpinner();
        lblMaximumJumpCount = new JLabel();
        spnMaximumJumpCount = new JSpinner();
        chkUseReeducationCamps = new JCheckBox();
        chkEnableOverrideRequirements = new JCheckBox();
        chkShowIneligibleAcademies = new JCheckBox();
        lblEntranceExamBaseTargetNumber = new JLabel();
        spnEntranceExamBaseTargetNumber = new JSpinner();

        pnlEnableStandardSets = new JPanel();
        chkEnableLocalAcademies = new JCheckBox();
        chkEnablePrestigiousAcademies = new JCheckBox();
        chkEnableUnitEducation = new JCheckBox();

        pnlXpAndSkillBonuses = new JPanel();
        chkEnableBonuses = new JCheckBox();
        lblFacultyXpMultiplier = new JLabel();
        spnFacultyXpMultiplier = new JSpinner();

        pnlDropoutChance = new JPanel();
        lblAdultDropoutChance = new JLabel();
        spnAdultDropoutChance = new JSpinner();
        lblChildrenDropoutChance = new JLabel();
        spnChildrenDropoutChance = new JSpinner();

        pnlAccidentsAndEvents = new JPanel();
        chkAllAges = new JCheckBox();
        lblMilitaryAcademyAccidents = new JLabel();
        spnMilitaryAcademyAccidents = new JSpinner();
    }

    /**
     * Initializes the Death tab, focusing on:
     * <ul>
     * <li>Allowing configuration of random death probabilities for personnel.</li>
     * <li>Customizing age-group-specific death settings.</li>
     * <li>Selecting methods for random deaths (e.g., natural causes or
     * accidents).</li>
     * </ul>
     */
    private void initializeDeathTab() {
        chkUseRandomDeathSuicideCause = new JCheckBox();
        lblRandomDeathMultiplier = new JLabel();
        spnRandomDeathMultiplier = new JSpinner();

        pnlDeathAgeGroup = new JPanel();
        chkEnabledRandomDeathAgeGroups = new HashMap<>();
    }

    /**
     * Initializes the Backgrounds tab, which handles:
     * <p>
     * <li>Randomized background settings for characters.</li>
     * <li>Options for specifying origins (e.g., faction-specific planetary
     * systems).</li>
     * <li>Custom search radius and distance scaling for randomized origins.</li>
     * </p>
     */
    private void initializeBackgroundsTab() {
        pnlRandomBackgrounds = new JPanel();
        chkUseRandomPersonalities = new JCheckBox();
        chkUseRandomPersonalityReputation = new JCheckBox();
        chkUseReasoningXpMultiplier = new JCheckBox();
        chkUseSimulatedRelationships = new JCheckBox();

        pnlRandomOriginOptions = new JPanel();
        chkRandomizeOrigin = new JCheckBox();
        chkRandomizeDependentsOrigin = new JCheckBox();
        chkRandomizeAroundSpecifiedPlanet = new JCheckBox();
        chkSpecifiedSystemFactionSpecific = new JCheckBox();
        lblSpecifiedSystem = new JLabel();
        comboSpecifiedSystem = new MMComboBox<>("comboSpecifiedSystem");
        lblSpecifiedPlanet = new JLabel();
        comboSpecifiedPlanet = new MMComboBox<>("comboSpecifiedPlanet");
        lblOriginSearchRadius = new JLabel();
        spnOriginSearchRadius = new JSpinner();
        lblOriginDistanceScale = new JLabel();
        spnOriginDistanceScale = new JSpinner();
        chkAllowClanOrigins = new JCheckBox();
        chkExtraRandomOrigin = new JCheckBox();
    }

    /**
     * Initializes the General tab, which provides options for:
     * <p>
     * <li>General gameplay settings such as gender distribution sliders.</li>
     * <li>Configuration of familial display levels and other general campaign
     * settings.</li>
     * <li>Annunciation of anniversaries, recruitment dates, and officer-related
     * milestones.</li>
     * </p>
     */
    private void initializeGeneralTab() {
        chkUseDylansRandomXP = new JCheckBox();
        lblGender = new JLabel();
        spnGender = new JSpinner();
        lblNonBinaryDiceSize = new JLabel();
        spnNonBinaryDiceSize = new JSpinner();
        lblFamilyDisplayLevel = new JLabel();
        comboFamilyDisplayLevel = new MMComboBox<>("comboFamilyDisplayLevel",
                FamilialRelationshipDisplayLevel.values());

        pnlAnniversariesPanel = new JPanel();
        chkAnnounceOfficersOnly = new JCheckBox();
        chkAnnounceBirthdays = new JCheckBox();
        chkAnnounceChildBirthdays = new JCheckBox();
        chkAnnounceRecruitmentAnniversaries = new JCheckBox();

        pnlLifeEvents = new JPanel();
        chkShowLifeEventDialogBirths = new JCheckBox();
        chkShowLifeEventDialogComingOfAge = new JCheckBox();
        chkShowLifeEventDialogCelebrations = new JCheckBox();

        pnlComingOfAge = new JPanel();
        chkVeterancySPAs = new JCheckBox();
        chkAwardRelevantVeterancySPAs = new JCheckBox();
        chkComingOfAgeSPAs = new JCheckBox();
        chkRewardComingOfAgeRPSkills = new JCheckBox();
    }

    /**
     * Builds and returns a `DefaultComboBoxModel` containing the names of all
     * available factions.
     *
     * @return A `DefaultComboBoxModel` populated with faction names for random name
     *         generation rules.
     */
    private static DefaultComboBoxModel<String> getFactionNamesModel() {
        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<>();
        for (final String faction : RandomNameGenerator.getInstance().getFactions()) {
            factionNamesModel.addElement(faction);
        }
        return factionNamesModel;
    }

    /**
     * Creates and lays out the General tab, including its components like:
     * <ul>
     * <li>Checkboxes for random XP distribution.</li>
     * <li>Sliders for gender representation customization.</li>
     * <li>Combo boxes for family display level settings within the GUI.</li>
     * </ul>
     *
     * @return A `JPanel` representing the General tab in the campaign options
     *         dialog.
     */
    public JPanel createGeneralTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_blood_spirit.png";
        generalHeader = new CampaignOptionsHeaderPanel("BiographyGeneralTab", imageAddress, 6);

        // Contents
        chkUseDylansRandomXP = new CampaignOptionsCheckBox("UseDylansRandomXP");
        chkUseDylansRandomXP.addMouseListener(createTipPanelUpdater(generalHeader, "UseDylansRandomXP"));

        lblGender = new CampaignOptionsLabel("Gender");
        lblGender.addMouseListener(createTipPanelUpdater(generalHeader, "Gender"));
        spnGender = new CampaignOptionsSpinner("Gender", 50, 0, 100, 1);
        spnGender.addMouseListener(createTipPanelUpdater(generalHeader, "Gender"));

        lblNonBinaryDiceSize = new CampaignOptionsLabel("NonBinaryDiceSize",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblNonBinaryDiceSize.addMouseListener(createTipPanelUpdater(generalHeader, "NonBinaryDiceSize"));
        spnNonBinaryDiceSize = new CampaignOptionsSpinner("NonBinaryDiceSize", 60, 0, 100000, 1);
        spnNonBinaryDiceSize.addMouseListener(createTipPanelUpdater(generalHeader, "NonBinaryDiceSize"));

        lblFamilyDisplayLevel = new CampaignOptionsLabel("FamilyDisplayLevel",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFamilyDisplayLevel.addMouseListener(createTipPanelUpdater(generalHeader, "FamilyDisplayLevel"));
        comboFamilyDisplayLevel.addMouseListener(createTipPanelUpdater(generalHeader, "FamilyDisplayLevel"));

        JPanel generalOptionsPanel = createBiographyGeneralOptionsPanel();
        pnlAnniversariesPanel = createAnniversariesPanel();
        pnlLifeEvents = createLifeEventsPanel();
        pnlComingOfAge = createComingOfAgePanel();
        JPanel panel = CampaignOptionsPagePanel.builder("BiographyGeneralTab", "BiographyGeneralTab", imageAddress)
            .header(generalHeader)
            .quote("biographyGeneralTab")
            .section("lblBiographyGeneralTab.text",
                "lblBiographyGeneralTab.summary",
                generalOptionsPanel,
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM))
            .section("lblAnniversariesPanel.text", "lblAnniversariesPanel.summary", pnlAnniversariesPanel)
            .section("lblLifeEventsPanel.text", "lblLifeEventsPanel.summary", pnlLifeEvents)
            .section("lblComingOfAgePanel.text", "lblComingOfAgePanel.summary", pnlComingOfAge)
            .build();

        generalPageCreated = true;
        updateGeneralControlsFromModel();

        return panel;
    }

    private JPanel createBiographyGeneralOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("BiographyGeneralOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseDylansRandomXP);
        panel.addRow(lblGender, spnGender);
        panel.addRow(lblNonBinaryDiceSize, spnNonBinaryDiceSize);
        panel.addRow(lblFamilyDisplayLevel, comboFamilyDisplayLevel);

        return panel;
    }

    /**
     * Creates the Anniversaries panel within the General tab for managing
     * announcement-related settings:
     * <p>
     * <li>Enabling birthday and recruitment anniversary announcements.</li>
     * <li>Specifying whether such announcements should be limited to officers.</li>
     * </p>
     *
     * @return A `JPanel` containing the UI components for defining
     *         anniversary-related settings.
     */
    private JPanel createAnniversariesPanel() {
        // Contents
        chkAnnounceBirthdays = new CampaignOptionsCheckBox("AnnounceBirthdays");
        chkAnnounceBirthdays.addMouseListener(createTipPanelUpdater(generalHeader, "AnnounceBirthdays"));
        chkAnnounceRecruitmentAnniversaries = new CampaignOptionsCheckBox("AnnounceRecruitmentAnniversaries");
        chkAnnounceRecruitmentAnniversaries.addMouseListener(createTipPanelUpdater(generalHeader,
                "AnnounceRecruitmentAnniversaries"));
        chkAnnounceOfficersOnly = new CampaignOptionsCheckBox("AnnounceOfficersOnly");
        chkAnnounceOfficersOnly.addMouseListener(createTipPanelUpdater(generalHeader, "AnnounceOfficersOnly"));
        chkAnnounceChildBirthdays = new CampaignOptionsCheckBox("AnnounceChildBirthdays",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkAnnounceChildBirthdays.addMouseListener(createTipPanelUpdater(generalHeader, "AnnounceChildBirthdays"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AnniversariesPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkAnnounceBirthdays,
                chkAnnounceRecruitmentAnniversaries,
                chkAnnounceOfficersOnly,
                chkAnnounceChildBirthdays);

        return panel;
    }

    private JPanel createLifeEventsPanel() {
        // Contents
        chkShowLifeEventDialogBirths = new CampaignOptionsCheckBox("ShowLifeEventDialogBirths");
        chkShowLifeEventDialogBirths.addMouseListener(createTipPanelUpdater(generalHeader,
                "ShowLifeEventDialogBirths"));
        chkShowLifeEventDialogComingOfAge = new CampaignOptionsCheckBox("ShowLifeEventDialogComingOfAge");
        chkShowLifeEventDialogComingOfAge.addMouseListener(createTipPanelUpdater(generalHeader,
                "ShowLifeEventDialogComingOfAge"));
        chkShowLifeEventDialogCelebrations = new CampaignOptionsCheckBox("ShowLifeEventDialogCelebrations");
        chkShowLifeEventDialogCelebrations.addMouseListener(createTipPanelUpdater(generalHeader,
                "ShowLifeEventDialogCelebrations"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("LifeEventsPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkShowLifeEventDialogBirths,
                chkShowLifeEventDialogComingOfAge,
                chkShowLifeEventDialogCelebrations);

        return panel;
    }

    private JPanel createComingOfAgePanel() {
        // Contents
        chkVeterancySPAs = new CampaignOptionsCheckBox("VeterancySPAs",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        chkVeterancySPAs.addMouseListener(createTipPanelUpdater(generalHeader, "VeterancySPAs"));

        chkAwardRelevantVeterancySPAs = new CampaignOptionsCheckBox("AwardRelevantVeterancySPAs",
                getMetadata(new Version(0, 51, 0), CampaignOptionFlag.IMPORTANT));
        chkAwardRelevantVeterancySPAs.addMouseListener(createTipPanelUpdater(generalHeader,
                "AwardRelevantVeterancySPAs"));

        chkComingOfAgeSPAs = new CampaignOptionsCheckBox("ComingOfAgeAbilities",
                getMetadata(null, CampaignOptionFlag.RECOMMENDED));
        chkComingOfAgeSPAs.addMouseListener(createTipPanelUpdater(generalHeader, "ComingOfAgeAbilities"));

        chkRewardComingOfAgeRPSkills = new CampaignOptionsCheckBox("ComingOfAgeRPSkills",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkRewardComingOfAgeRPSkills.addMouseListener(createTipPanelUpdater(generalHeader,
                "ComingOfAgeRPSkills"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ComingOfAgePanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkVeterancySPAs,
                chkAwardRelevantVeterancySPAs,
                chkComingOfAgeSPAs,
                chkRewardComingOfAgeRPSkills);

        return panel;
    }

    /**
     * Creates and lays out the Backgrounds tab, which includes:
     * <ul>
     * <li>Settings for enabling randomized personalities and relationships.</li>
     * <li>Random origin configurations such as faction specificity and distance
     * scaling.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Backgrounds tab in the campaign options
     *         dialog.
     */
    public JPanel createBackgroundsTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_nueva_castile.png";
        backgroundHeader = new CampaignOptionsHeaderPanel("BackgroundsTab", imageAddress, 3);

        // Contents
        pnlRandomOriginOptions = createRandomOriginOptionsPanel();
        pnlRandomBackgrounds = createRandomBackgroundsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("BackgroundsTab", "BackgroundsTab", imageAddress)
            .header(backgroundHeader)
            .quote("backgroundsTab")
            .section("lblRandomOriginOptionsPanel.text",
                "lblRandomOriginOptionsPanel.summary",
                pnlRandomOriginOptions,
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM))
            .section("lblRandomBackgroundsPanel.text",
                "lblRandomBackgroundsPanel.summary",
                pnlRandomBackgrounds)
            .build();

        backgroundsPageCreated = true;
        updateBackgroundControlsFromModel();

        return panel;
    }

    /**
     * Creates the panel for configuring random background options in the campaign.
     * <p>
     * This includes controls to enable or disable features such as:
     * <p>
     * <li>Random personalities for characters.</li>
     * <li>Random personality reputation.</li>
     * <li>Reasoning XP multipliers.</li>
     * <li>Simulated relationships.</li>
     * </p>
     *
     * @return A {@code JPanel} representing the random background configuration UI.
     */
    JPanel createRandomBackgroundsPanel() {
        // Contents
        chkUseRandomPersonalities = new CampaignOptionsCheckBox("UseRandomPersonalities",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED));
        chkUseRandomPersonalities.addMouseListener(createTipPanelUpdater(backgroundHeader, "UseRandomPersonalities"));
        chkUseRandomPersonalityReputation = new CampaignOptionsCheckBox("UseRandomPersonalityReputation");
        chkUseRandomPersonalityReputation.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "UseRandomPersonalityReputation"));
        chkUseReasoningXpMultiplier = new CampaignOptionsCheckBox("UseReasoningXpMultiplier");
        chkUseReasoningXpMultiplier.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "UseReasoningXpMultiplier"));
        chkUseSimulatedRelationships = new CampaignOptionsCheckBox("UseSimulatedRelationships",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseSimulatedRelationships.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "UseSimulatedRelationships"));

        // Layout the Panels
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomBackgroundsPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseRandomPersonalities,
                chkUseRandomPersonalityReputation,
                chkUseReasoningXpMultiplier,
                chkUseSimulatedRelationships);

        return panel;
    }

    /**
     * Creates and returns a panel for random origin options. This includes:
     * <p>
     * <li>Controls to enable or disable randomization of origins.</li>
     * <li>Options for selecting specific planetary systems or factions for origin
     * determination.</li>
     * <li>Search radius and distance scaling fields to tweak origin
     * calculations.</li>
     * </p>
     *
     * @return A `JPanel` for managing random origin settings.
     */
    private JPanel createRandomOriginOptionsPanel() {
        // Contents
        chkRandomizeOrigin = new CampaignOptionsCheckBox("RandomizeOrigin");
        chkRandomizeOrigin.addMouseListener(createTipPanelUpdater(backgroundHeader, "RandomizeOrigin"));
        chkRandomizeDependentsOrigin = new CampaignOptionsCheckBox("RandomizeDependentsOrigin");
        chkRandomizeDependentsOrigin.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "RandomizeDependentsOrigin"));

        chkRandomizeAroundSpecifiedPlanet = new CampaignOptionsCheckBox("RandomizeAroundSpecifiedPlanet");
        chkRandomizeAroundSpecifiedPlanet.addActionListener(evt -> refreshSystemsAndPlanets());
        chkRandomizeAroundSpecifiedPlanet.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "RandomizeAroundSpecifiedPlanet"));

        chkSpecifiedSystemFactionSpecific = new CampaignOptionsCheckBox("SpecifiedSystemFactionSpecific",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkSpecifiedSystemFactionSpecific.addActionListener(evt -> refreshSystemsAndPlanets());
        chkSpecifiedSystemFactionSpecific.addMouseListener(createTipPanelUpdater(backgroundHeader,
                "SpecifiedSystemFactionSpecific"));

        lblSpecifiedSystem = new CampaignOptionsLabel("SpecifiedSystem");
        lblSpecifiedSystem.addMouseListener(createTipPanelUpdater(backgroundHeader, "SpecifiedSystem"));
        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(
                getPlanetarySystems(chkSpecifiedSystemFactionSpecific.isSelected() ? generalTab.getFaction() : null)));
        comboSpecifiedSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    setText(((PlanetarySystem) value).getName(generalTab.getDate()));
                }
                return this;
            }
        });
        comboSpecifiedSystem.addActionListener(evt -> {
            final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
            final Planet planet = comboSpecifiedPlanet.getSelectedItem();
            if ((planetarySystem == null) || ((planet != null) && !planet.getParentSystem().equals(planetarySystem))) {
                restoreComboSpecifiedPlanet();
            }
        });
        comboSpecifiedSystem.addMouseListener(createTipPanelUpdater(backgroundHeader, "SpecifiedSystem"));

        lblSpecifiedPlanet = new CampaignOptionsLabel("SpecifiedPlanet");
        lblSpecifiedPlanet.addMouseListener(createTipPanelUpdater(backgroundHeader, "SpecifiedPlanet"));
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        if (planetarySystem != null) {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(planetarySystem.getPlanets()
                    .toArray(new Planet[] {})));
        }
        comboSpecifiedPlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(generalTab.getDate()));
                }
                return this;
            }
        });
        comboSpecifiedPlanet.addMouseListener(createTipPanelUpdater(backgroundHeader, "SpecifiedPlanet"));

        lblOriginSearchRadius = new CampaignOptionsLabel("OriginSearchRadius");
        lblOriginSearchRadius.addMouseListener(createTipPanelUpdater(backgroundHeader, "OriginSearchRadius"));
        spnOriginSearchRadius = new CampaignOptionsSpinner("OriginSearchRadius", 0, 0, 2000, 25);
        spnOriginSearchRadius.addMouseListener(createTipPanelUpdater(backgroundHeader, "OriginSearchRadius"));

        lblOriginDistanceScale = new CampaignOptionsLabel("OriginDistanceScale",
                getMetadata(null, CampaignOptionFlag.IMPORTANT));
        lblOriginDistanceScale.addMouseListener(createTipPanelUpdater(backgroundHeader, "OriginDistanceScale"));
        spnOriginDistanceScale = new CampaignOptionsSpinner("OriginDistanceScale", 0.6, 0.1, 2.0, 0.1);
        spnOriginDistanceScale.addMouseListener(createTipPanelUpdater(backgroundHeader, "OriginDistanceScale"));

        chkAllowClanOrigins = new CampaignOptionsCheckBox("AllowClanOrigins");
        chkAllowClanOrigins.addMouseListener(createTipPanelUpdater(backgroundHeader, "AllowClanOrigins"));
        chkExtraRandomOrigin = new CampaignOptionsCheckBox("ExtraRandomOrigin");
        chkExtraRandomOrigin.addMouseListener(createTipPanelUpdater(backgroundHeader, "ExtraRandomOrigin"));

        // The system/planet combos are backed by the whole universe, so an unprototyped
        // combo would size itself to
        // the widest entry and stretch this section wider than the other Biography
        // sub-tabs. Pin them to the control
        // column and surface the full selected value as a tooltip (it remains fully
        // visible in the dropdown).
        capComboWidthWithTooltip(comboSpecifiedSystem);
        capComboWidthWithTooltip(comboSpecifiedPlanet);

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomOriginOptionsPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
            chkRandomizeOrigin,
            chkRandomizeDependentsOrigin,
            chkRandomizeAroundSpecifiedPlanet,
            chkSpecifiedSystemFactionSpecific);
        panel.addRow(lblSpecifiedSystem, comboSpecifiedSystem);
        panel.addRow(lblSpecifiedPlanet, comboSpecifiedPlanet);
        panel.addRow(lblOriginSearchRadius, spnOriginSearchRadius);
        panel.addRow(lblOriginDistanceScale, spnOriginDistanceScale);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS, chkAllowClanOrigins, chkExtraRandomOrigin);

        return panel;
    }

    /**
     * Pins {@code combo}'s preferred width to the form's control column so a
     * long-content model (such as the full list
     * of planetary systems or planets) cannot stretch the section wider than its
     * siblings. The combo still fills the
     * control column through the form's horizontal-fill layout, and the full
     * selected value stays available as a
     * tooltip (and in the dropdown). Without this, an unprototyped
     * {@link javax.swing.JComboBox} measures every model
     * entry and adopts the widest, which made the Backgrounds page noticeably wider
     * than the other Biography sub-tabs.
     *
     * @param combo the combo box to constrain and decorate with a full-value
     *              tooltip
     */
    private void capComboWidthWithTooltip(MMComboBox<?> combo) {
        combo.setPreferredSize(new Dimension(FORM_CONTROL_COLUMN_WIDTH, combo.getPreferredSize().height));
        updateComboTooltip(combo);
        combo.addActionListener(evt -> updateComboTooltip(combo));
    }

    /**
     * Sets {@code combo}'s tooltip to the full, date-aware name of its selected
     * planetary system or planet, so a value
     * that is truncated with an ellipsis in the collapsed field can still be read
     * in full on hover.
     *
     * @param combo the combo box whose tooltip should reflect its current selection
     */
    private void updateComboTooltip(MMComboBox<?> combo) {
        final Object selected = combo.getSelectedItem();
        if (selected instanceof PlanetarySystem system) {
            combo.setToolTipText(system.getName(generalTab.getDate()));
        } else if (selected instanceof Planet planet) {
            combo.setToolTipText(planet.getName(generalTab.getDate()));
        } else {
            combo.setToolTipText(null);
        }
    }

    /**
     * Refreshes the planetary systems and planets displayed in the associated combo
     * boxes.
     *
     * <p>
     * This method first stores the currently selected planetary system and planet.
     * It then
     * restores the list of available planetary systems by repopulating the
     * `comboSpecifiedSystem`. Finally, it
     * re-selects the previously selected planetary system and planet in their
     * respective combo boxes.
     * </p>
     *
     * <p>
     * The method ensures that the user selection persists even after the combo
     * boxes are refreshed.
     * Any exceptions during the selection process are caught and ignored. As if we
     * can't restore the selection, that's
     * fine, we just use the fallback index of 0.
     * </p>
     */
    private void refreshSystemsAndPlanets() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        final Planet planet = comboSpecifiedPlanet.getSelectedItem();

        restoreComboSpecifiedSystem();

        try {
            comboSpecifiedSystem.setSelectedItem(planetarySystem);
            comboSpecifiedPlanet.setSelectedItem(planet);
        } catch (Exception ignored) {
        }
    }

    /**
     * Resets the planet combo box to show only the planets matching the currently
     * selected planetary system.
     */
    private void restoreComboSpecifiedPlanet() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();

        if (planetarySystem == null) {
            comboSpecifiedPlanet.removeAllItems();
        } else {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(planetarySystem.getPlanets()
                    .toArray(new Planet[] {})));
            comboSpecifiedPlanet.setSelectedItem(planetarySystem.getPrimaryPlanet());
        }
    }

    /**
     * Resets the system combo box to show only the planetary systems that match the
     * current faction, if applicable.
     */
    private void restoreComboSpecifiedSystem() {
        comboSpecifiedSystem.removeAllItems();

        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(
                getPlanetarySystems(chkSpecifiedSystemFactionSpecific.isSelected() ? generalTab.getFaction() : null)));

        restoreComboSpecifiedPlanet();
    }

    /**
     * Filters planetary systems based on a given faction (if specified) and returns
     * a sorted array of matches.
     *
     * @param faction The faction to filter planetary systems by (nullable). If
     *                `null`, all systems are included.
     *
     * @return An array of `PlanetarySystem` objects meeting the filter criteria.
     */
    private PlanetarySystem[] getPlanetarySystems(final @Nullable Faction faction) {
        ArrayList<PlanetarySystem> systems = campaign.getSystems();
        ArrayList<PlanetarySystem> filteredSystems = new ArrayList<>();

        // Filter systems
        for (PlanetarySystem planetarySystem : systems) {
            if ((faction == null) || planetarySystem.getFactionSet(generalTab.getDate()).contains(faction)) {
                filteredSystems.add(planetarySystem);
            }
        }

        // Sort systems
        filteredSystems.sort(Comparator.comparing(p -> p.getName(generalTab.getDate())));

        // Convert to array
        return filteredSystems.toArray(new PlanetarySystem[0]);
    }

    /**
     * Configures and creates the Death tab. This includes options like:
     * <ul>
     * <li>Methods for random death.</li>
     * <li>Percentage-based chances for random death events.</li>
     * <li>Check boxes to enable or disable age-specific death events.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Death tab.
     */
    public JPanel createDeathTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_fire_mandrills.png";
        deathHeader = new CampaignOptionsHeaderPanel("DeathTab", imageAddress, 5);

        // Contents
        lblRandomDeathMultiplier = new CampaignOptionsLabel("RandomDeathMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED, CampaignOptionFlag.IMPORTANT));
        lblRandomDeathMultiplier.addMouseListener(createTipPanelUpdater(deathHeader, "RandomDeathMultiplier"));
        spnRandomDeathMultiplier = new CampaignOptionsSpinner("RandomDeathMultiplier", 1.0, 0, 100.0, 0.01);
        spnRandomDeathMultiplier.addMouseListener(createTipPanelUpdater(deathHeader, "RandomDeathMultiplier"));

        chkUseRandomDeathSuicideCause = new CampaignOptionsCheckBox("UseRandomDeathSuicideCause");
        chkUseRandomDeathSuicideCause.addMouseListener(createTipPanelUpdater(deathHeader,
                "UseRandomDeathSuicideCause"));

        JPanel deathOptionsPanel = createDeathOptionsPanel();
        pnlDeathAgeGroup = createDeathAgeGroupsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("DeathTab", "DeathTab", imageAddress)
            .header(deathHeader)
            .quote("deathTab")
            .section("lblDeathTab.text", "lblDeathTab.summary", deathOptionsPanel)
            .section("lblDeathAgeGroupsPanel.text", "lblDeathAgeGroupsPanel.summary", pnlDeathAgeGroup)
            .build();

        deathPageCreated = true;
        updateDeathControlsFromModel();

        return panel;
    }

    private JPanel createDeathOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeathOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRandomDeathMultiplier, spnRandomDeathMultiplier);
        panel.addCheckBox(chkUseRandomDeathSuicideCause);

        return panel;
    }

    /**
     * Configures and creates a panel where users can enable or disable random death
     * probabilities for specific age
     * groups.
     *
     * @return A `JPanel` containing the random death age group options.
     */
    private JPanel createDeathAgeGroupsPanel() {
        final AgeGroup[] ageGroups = AgeGroup.values();

        // Contents
        JCheckBox[] ageGroupCheckBoxes = new JCheckBox[ageGroups.length];
        for (final AgeGroup ageGroup : ageGroups) {
            final JCheckBox checkBox = new JCheckBox(ageGroup.toString());
            checkBox.setToolTipText(ageGroup.getToolTipText());
            checkBox.setName("chk" + ageGroup);
            checkBox.addMouseListener(createTipPanelUpdater(deathHeader, null, ageGroup.getToolTipText()));

            ageGroupCheckBoxes[ageGroup.ordinal()] = checkBox;
            chkEnabledRandomDeathAgeGroups.put(ageGroup, checkBox);
        }

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeathAgeGroupsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS, ageGroupCheckBoxes);

        return panel;
    }

    /**
     * Creates the Education tab, which allows managing educational settings within
     * the campaign.
     * <p>
     * This includes:
     * <ul>
     * <li>Setting curriculum XP rates.</li>
     * <li>Configuring academy requirements and override options.</li>
     * <li>Managing dropout chances for adults and children.</li>
     * <li>Enabling or disabling the use of reeducation camps, accidents, and
     * events.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Education tab in the campaign UI.
     */
    public JPanel createEducationTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_taurian_concordat.png";
        educationHeader = new CampaignOptionsHeaderPanel("EducationTab", imageAddress, 3);

        // Contents
        chkUseEducationModule = new CampaignOptionsCheckBox("UseEducationModule");
        chkUseEducationModule.addMouseListener(createTipPanelUpdater(educationHeader, "UseEducationModule"));

        lblCurriculumXpRate = new CampaignOptionsLabel("CurriculumXpRate");
        lblCurriculumXpRate.addMouseListener(createTipPanelUpdater(educationHeader, "CurriculumXpRate"));
        spnCurriculumXpRate = new CampaignOptionsSpinner("CurriculumXpRate", 3, 1, 10, 1);
        spnCurriculumXpRate.addMouseListener(createTipPanelUpdater(educationHeader, "CurriculumXpRate"));

        lblMaximumJumpCount = new CampaignOptionsLabel("MaximumJumpCount");
        lblMaximumJumpCount.addMouseListener(createTipPanelUpdater(educationHeader, "MaximumJumpCount"));
        spnMaximumJumpCount = new CampaignOptionsSpinner("MaximumJumpCount", 5, 1, 200, 1);
        spnMaximumJumpCount.addMouseListener(createTipPanelUpdater(educationHeader, "MaximumJumpCount"));

        chkUseReeducationCamps = new CampaignOptionsCheckBox("UseReeducationCamps");
        chkUseReeducationCamps.addMouseListener(createTipPanelUpdater(educationHeader, "UseReeducationCamps"));

        chkEnableOverrideRequirements = new CampaignOptionsCheckBox("EnableOverrideRequirements",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableOverrideRequirements.addMouseListener(createTipPanelUpdater(educationHeader,
                "EnableOverrideRequirements"));

        chkShowIneligibleAcademies = new CampaignOptionsCheckBox("ShowIneligibleAcademies");
        chkShowIneligibleAcademies.addMouseListener(createTipPanelUpdater(educationHeader, "ShowIneligibleAcademies"));

        lblEntranceExamBaseTargetNumber = new CampaignOptionsLabel("EntranceExamBaseTargetNumber",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblEntranceExamBaseTargetNumber.addMouseListener(createTipPanelUpdater(educationHeader,
                "EntranceExamBaseTargetNumber"));
        spnEntranceExamBaseTargetNumber = new CampaignOptionsSpinner("EntranceExamBaseTargetNumber", 14, 0, 20, 1);
        spnEntranceExamBaseTargetNumber.addMouseListener(createTipPanelUpdater(educationHeader,
                "EntranceExamBaseTargetNumber"));

        JPanel educationOptionsPanel = createEducationOptionsPanel();
        pnlEnableStandardSets = createEnableStandardSetsPanel();
        pnlXpAndSkillBonuses = createXpAndSkillBonusesPanel();
        pnlDropoutChance = createDropoutChancePanel();
        pnlAccidentsAndEvents = createAccidentsAndEventsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("EducationTab", "EducationTab", imageAddress)
            .header(educationHeader)
            .quote("educationTab")
            .section("lblEducationTab.text", "lblEducationTab.summary", educationOptionsPanel)
            .section("lblEnableStandardSetsPanel.text", "lblEnableStandardSetsPanel.summary", pnlEnableStandardSets)
            .section("lblXpAndSkillBonusesPanel.text", "lblXpAndSkillBonusesPanel.summary", pnlXpAndSkillBonuses)
            .section("lblDropoutChancePanel.text", "lblDropoutChancePanel.summary", pnlDropoutChance)
            .section("lblAccidentsAndEventsPanel.text", "lblAccidentsAndEventsPanel.summary", pnlAccidentsAndEvents)
            .build();

        educationPageCreated = true;
        updateEducationControlsFromModel();

        return panel;
    }

    private JPanel createEducationOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("EducationOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseEducationModule);
        panel.addRow(lblCurriculumXpRate, spnCurriculumXpRate);
        panel.addRow(lblMaximumJumpCount, spnMaximumJumpCount);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
            chkUseReeducationCamps,
            chkEnableOverrideRequirements,
            chkShowIneligibleAcademies);
        panel.addRow(lblEntranceExamBaseTargetNumber, spnEntranceExamBaseTargetNumber);

        return panel;
    }

    /**
     * Creates a panel for enabling different education-related academy sets.
     * <p>
     * This includes options to toggle various academy types:
     * <p>
     * <li>Local academies.</li>
     * <li>Prestigious academies.</li>
     * <li>Unit-based education academies.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the Enable Standard Sets UI components.
     */
    private JPanel createEnableStandardSetsPanel() {
        chkEnableLocalAcademies = new CampaignOptionsCheckBox("EnableLocalAcademies");
        chkEnableLocalAcademies.addMouseListener(createTipPanelUpdater(educationHeader, "EnableLocalAcademies"));
        chkEnablePrestigiousAcademies = new CampaignOptionsCheckBox("EnablePrestigiousAcademies");
        chkEnablePrestigiousAcademies.addMouseListener(createTipPanelUpdater(educationHeader,
                "EnablePrestigiousAcademies"));
        chkEnableUnitEducation = new CampaignOptionsCheckBox("EnableUnitEducation");
        chkEnableUnitEducation.addMouseListener(createTipPanelUpdater(educationHeader, "EnableUnitEducation"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("EnableStandardSetsPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkEnableLocalAcademies,
                chkEnablePrestigiousAcademies,
                chkEnableUnitEducation);

        return panel;
    }

    /**
     * Creates a panel for configuring experience gain and skill bonuses.
     * <p>
     * This includes:
     * <p>
     * <li>Option to enable or disable bonuses.</li>
     * <li>Setting the faculty XP multiplier.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing XP rates and skill bonuses.
     */
    private JPanel createXpAndSkillBonusesPanel() {
        // Contents
        chkEnableBonuses = new CampaignOptionsCheckBox("EnableBonuses");
        chkEnableBonuses.addMouseListener(createTipPanelUpdater(educationHeader, "EnableBonuses"));

        lblFacultyXpMultiplier = new CampaignOptionsLabel("FacultyXpMultiplier");
        lblFacultyXpMultiplier.addMouseListener(createTipPanelUpdater(educationHeader, "FacultyXpMultiplier"));
        spnFacultyXpMultiplier = new CampaignOptionsSpinner("FacultyXpMultiplier", 1.00, 0.00, 10.00, 0.01);
        spnFacultyXpMultiplier.addMouseListener(createTipPanelUpdater(educationHeader, "FacultyXpMultiplier"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("XpAndSkillBonusesPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkEnableBonuses);
        panel.addRow(lblFacultyXpMultiplier, spnFacultyXpMultiplier);

        return panel;
    }

    /**
     * Creates a panel for configuring dropout chances for academies.
     * <p>
     * This includes:
     * <p>
     * <li>Setting the dropout chance for adults.</li>
     * <li>Setting the dropout chance for children.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing dropout change settings.
     */
    private JPanel createDropoutChancePanel() {
        // Contents
        lblAdultDropoutChance = new CampaignOptionsLabel("AdultDropoutChance");
        lblAdultDropoutChance.addMouseListener(createTipPanelUpdater(educationHeader, "AdultDropoutChance"));
        spnAdultDropoutChance = new CampaignOptionsSpinner("AdultDropoutChance", 1000, 0, 100000, 1);
        spnAdultDropoutChance.addMouseListener(createTipPanelUpdater(educationHeader, "AdultDropoutChance"));

        lblChildrenDropoutChance = new CampaignOptionsLabel("ChildrenDropoutChance");
        lblChildrenDropoutChance.addMouseListener(createTipPanelUpdater(educationHeader, "ChildrenDropoutChance"));
        spnChildrenDropoutChance = new CampaignOptionsSpinner("ChildrenDropoutChance", 10000, 0, 100000, 1);
        spnChildrenDropoutChance.addMouseListener(createTipPanelUpdater(educationHeader, "ChildrenDropoutChance"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DropoutChancePanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblAdultDropoutChance, spnAdultDropoutChance);
        panel.addRow(lblChildrenDropoutChance, spnChildrenDropoutChance);

        return panel;
    }

    /**
     * Creates a panel for configuring accidents and events related to military
     * academies.
     * <p>
     * This includes:
     * <p>
     * <li>Toggling settings for all-age accidents.</li>
     * <li>Configuring the frequency of military academy accidents.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the accidents and events configuration
     *         UI.
     */
    private JPanel createAccidentsAndEventsPanel() {
        // Contents
        chkAllAges = new CampaignOptionsCheckBox("AllAges");
        chkAllAges.addMouseListener(createTipPanelUpdater(educationHeader, "AllAges"));

        lblMilitaryAcademyAccidents = new CampaignOptionsLabel("MilitaryAcademyAccidents");
        lblMilitaryAcademyAccidents.addMouseListener(createTipPanelUpdater(educationHeader,
                "MilitaryAcademyAccidents"));
        spnMilitaryAcademyAccidents = new CampaignOptionsSpinner("MilitaryAcademyAccidents", 10000, 0, 100000, 1);
        spnMilitaryAcademyAccidents.addMouseListener(createTipPanelUpdater(educationHeader,
                "MilitaryAcademyAccidents"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AccidentsAndEventsPanel",
            FORM_LABEL_COLUMN_WIDTH,
            FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkAllAges);
        panel.addRow(lblMilitaryAcademyAccidents, spnMilitaryAcademyAccidents);

        return panel;
    }

    /**
     * Creates the Name and Portrait Generation tab for the campaign options.
     * <p>
     * This tab allows users to:
     * </p>
     * <ul>
     * <li>Enable or disable the use of origin factions for name generation.</li>
     * <li>Assign portraits to personnel upon role changes.</li>
     * <li>Customize which portraits are randomly used for specific roles.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Name and Portrait Generation tab.
     */
    public JPanel createNameAndPortraitGenerationTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_nova_cat.png";
        nameAndPortraitGenerationHeader = new CampaignOptionsHeaderPanel("NameAndPortraitGenerationTab",
            imageAddress,
            5);

        // Contents
        chkAssignPortraitOnRoleChange = new CampaignOptionsCheckBox("AssignPortraitOnRoleChange");
        chkAssignPortraitOnRoleChange.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "AssignPortraitOnRoleChange"));

        chkAllowDuplicatePortraits = new CampaignOptionsCheckBox("AllowDuplicatePortraits");
        chkAllowDuplicatePortraits.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "AllowDuplicatePortraits"));

        chkUseGenderedPortraitsOnly = new CampaignOptionsCheckBox("UseGenderedPortraitsOnly",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseGenderedPortraitsOnly.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "UseGenderedPortraitsOnly"));

        chkNoRandomPortraitsForChildren = new CampaignOptionsCheckBox("NoRandomPortraitsForChildren",
                getMetadata(new Version(0, 51, 0)));
        chkNoRandomPortraitsForChildren.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "NoRandomPortraitsForChildren"));

        chkChildPortraitsWhenComingOfAge = new CampaignOptionsCheckBox("ChildPortraitsWhenComingOfAge",
                getMetadata(new Version(0, 51, 0)));
        chkChildPortraitsWhenComingOfAge.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "ChildPortraitsWhenComingOfAge"));

        chkUseOriginFactionForNames = new CampaignOptionsCheckBox("UseOriginFactionForNames");
        chkUseOriginFactionForNames.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "UseOriginFactionForNames"));

        lblFactionNames = new CampaignOptionsLabel("FactionNames");
        lblFactionNames.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader, "FactionNames"));
        comboFactionNames.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader, "FactionNames"));

        JPanel nameGenerationPanel = createNameGenerationPanel();
        JPanel portraitRulesPanel = createPortraitRulesPanel();
        pnlRandomPortrait = createRandomPortraitPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("NameAndPortraitGenerationTab",
                "NameAndPortraitGenerationTab",
                imageAddress)
            .header(nameAndPortraitGenerationHeader)
            .quote("nameAndPortraitGenerationTab")
            .section("lblNameGenerationPanel.text", "lblNameGenerationPanel.summary", nameGenerationPanel)
            .section("lblPortraitRulesPanel.text", "lblPortraitRulesPanel.summary", portraitRulesPanel)
            .section("lblRandomPortraitPanel.text", "lblRandomPortraitPanel.summary", pnlRandomPortrait)
            .build();

        nameAndPortraitPageCreated = true;
        updateNameAndPortraitControlsFromModel();

        return panel;
    }

    private JPanel createNameGenerationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("NameGenerationPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseOriginFactionForNames);
        panel.addRow(lblFactionNames, comboFactionNames);

        return panel;
    }

    private JPanel createPortraitRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PortraitRulesPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
            chkAssignPortraitOnRoleChange,
            chkAllowDuplicatePortraits,
            chkUseGenderedPortraitsOnly,
            chkNoRandomPortraitsForChildren);
        panel.addCheckBox(chkChildPortraitsWhenComingOfAge);

        return panel;
    }

    /**
     * Creates a panel for customizing random portrait generation for personnel
     * roles.
     * <p>
     * This includes:
     * <p>
     * <li>Options to enable or disable the use of role-specific portraits.</li>
     * <li>Buttons to toggle all or no portrait options collectively.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the random portrait generation
     *         configuration UI.
     */
    private JPanel createRandomPortraitPanel() {
        // Contents
        chkUsePortrait = new JCheckBox[personnelRoles.size() + 1];

        btnEnableAllPortraits = createPortraitAssignmentButton("EnableAllPortraits");
        btnEnableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                if (checkBox != null) {
                    checkBox.setSelected(true);
                }
            }
        });
        btnEnableAllPortraits.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "EnableAllPortraits"));

        btnDisableAllPortraits = createPortraitAssignmentButton("DisableAllPortraits");
        btnDisableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                if (checkBox != null) {
                    checkBox.setSelected(false);
                }
            }
        });
        btnDisableAllPortraits.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                "DisableAllPortraits"));

        // Layout the Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(btnEnableAllPortraits);
        actionPanel.add(btnDisableAllPortraits);

        JCheckBox[] portraitCheckBoxes = new JCheckBox[personnelRoles.size() + 1];
        int portraitIndex = 0;

        // Add remaining checkboxes
        JCheckBox jCheckBox;
        for (final PersonnelRole role : personnelRoles) {
            jCheckBox = new JCheckBox(role.toString());
            jCheckBox.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                    null,
                    role.getDescription(false)));
            portraitCheckBoxes[portraitIndex++] = jCheckBox;
            chkUsePortrait[role.ordinal()] = jCheckBox;
        }

        jCheckBox = new JCheckBox(PersonnelRoleSubType.CIVILIAN.toString());
        jCheckBox.addMouseListener(createTipPanelUpdater(nameAndPortraitGenerationHeader,
                null,
                getTextAt(getCampaignOptionsResourceBundle(), "lblCivilian.tooltip")));
        portraitCheckBoxes[portraitIndex] = jCheckBox;
        chkUsePortrait[personnelRoles.size()] = jCheckBox;

        CampaignOptionsFormPanel rolePanel = new CampaignOptionsFormPanel("RandomPortraitRolePanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        rolePanel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS, portraitCheckBoxes);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setName("pnlRandomPortraitPanel");
        panel.setOpaque(false);
        panel.add(actionPanel, BorderLayout.NORTH);
        panel.add(rolePanel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createPortraitAssignmentButton(String name) {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text"));
        button.setName("btn" + name);
        button.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip"));
        button.putClientProperty("JComponent.sizeVariant", "small");
        return button;
    }

    /**
     * Creates the Rank tab for configuring rank systems within the campaign.
     * <p>
     * This tab provides options for:
     * <ul>
     * <li>Managing rank systems for personnel in the campaign.</li>
     * <li>Displaying rank-related UI components for user configuration.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Rank tab in the campaign
     *         configuration.
     */
    public JPanel createRankTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_umayyad_caliphate.png";
        CampaignOptionsHeaderPanel headerPanel = new CampaignOptionsHeaderPanel("RankTab", imageAddress);

        // Contents
        JPanel rankSystemsPanel = createRankSystemsPanel();
        return CampaignOptionsPagePanel.builder("RankTab", "RankTab", imageAddress)
            .header(headerPanel)
            .intro("lblRankTabBody.text")
            .quote("rankTab")
            .section("lblRankTab.text", "lblRankTab.summary", rankSystemsPanel)
            .build();
    }

    private JPanel createRankSystemsPanel() {
        rankSystemsPane.applyToCampaign();
        configureEmbeddedRankSystemsPane();

        JPanel rankSystemsPanel = new JPanel(new BorderLayout());
        rankSystemsPanel.setName("pnlRankSystemsPanel");
        rankSystemsPanel.setOpaque(false);
        rankSystemsPanel.add(rankSystemsPane, BorderLayout.CENTER);

        return rankSystemsPanel;
    }

    private void configureEmbeddedRankSystemsPane() {
        rankSystemsPane.setBorder(BorderFactory.createEmptyBorder());
        rankSystemsPane.setViewportBorder(null);
        rankSystemsPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rankSystemsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        removeEmbeddedRankSystemsBorders(rankSystemsPane.getViewport().getView());

        JTable ranksTable = rankSystemsPane.getRanksTable();
        ranksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ranksTable.setFillsViewportHeight(true);
        configureEmbeddedRankTableColumns(ranksTable);

        JScrollPane tableScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class,
                ranksTable);
        if (tableScrollPane != null) {
            Dimension tableSize = new Dimension(RANK_SYSTEMS_PANEL_WIDTH, RANK_TABLE_HEIGHT);
            tableScrollPane.setPreferredSize(tableSize);
            tableScrollPane.setMinimumSize(new Dimension(0, RANK_TABLE_HEIGHT));
            tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        Dimension rankSystemsSize = getEmbeddedRankSystemsSize();
        rankSystemsPane.setPreferredSize(rankSystemsSize);
        rankSystemsPane.setMinimumSize(new Dimension(0, rankSystemsSize.height));
    }

    private Dimension getEmbeddedRankSystemsSize() {
        Component view = rankSystemsPane.getViewport().getView();
        int viewHeight = view == null ? RANK_TABLE_HEIGHT : view.getPreferredSize().height;
        int horizontalScrollBarHeight = rankSystemsPane.getHorizontalScrollBar().getPreferredSize().height;

        return new Dimension(RANK_SYSTEMS_PANEL_WIDTH, viewHeight + horizontalScrollBarHeight);
    }

    private void configureEmbeddedRankTableColumns(JTable ranksTable) {
        for (int index = 0; index < ranksTable.getColumnModel().getColumnCount(); index++) {
            int modelIndex = ranksTable.convertColumnIndexToModel(index);
            TableColumn column = ranksTable.getColumnModel().getColumn(index);
            column.setPreferredWidth(getEmbeddedRankColumnWidth(modelIndex));
        }
    }

    private int getEmbeddedRankColumnWidth(int modelIndex) {
        return switch (modelIndex) {
            case RankTableModel.COL_NAME_RATE -> RANK_RATE_COLUMN_WIDTH;
            case RankTableModel.COL_OFFICER -> RANK_OFFICER_COLUMN_WIDTH;
            case RankTableModel.COL_PAY_MULTI -> RANK_PAY_MULTIPLIER_COLUMN_WIDTH;
            default -> RANK_NAME_COLUMN_WIDTH;
        };
    }

    private void removeEmbeddedRankSystemsBorders(Component component) {
        if (component instanceof JPanel panel && ("rankSystemPanel".equals(panel.getName()) ||
                "rankSystemFileButtonsPanel".equals(panel.getName()))) {
            panel.setBorder(BorderFactory.createEmptyBorder());
        }

        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                removeEmbeddedRankSystemsBorders(child);
            }
        }
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null, null);
    }

    /**
     * Loads values from campaign options, optionally integrating with presets for
     * default settings.
     *
     * @param presetCampaignOptions     Optional preset campaign options, or `null`
     *                                  to use the campaign's active
     *                                  settings.
     * @param presetRandomOriginOptions Optional random origin options, or `null` to
     *                                  use the default origin settings.
     * @param presetRankSystem          Optional rank system, or `null` to use the
     *                                  default system.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomOriginOptions presetRandomOriginOptions, @Nullable RankSystem presetRankSystem) {
        CampaignOptions options = presetCampaignOptions;
        if (options == null) {
            options = this.campaignOptions;
        }

        RandomOriginOptions originOptions = presetRandomOriginOptions;
        if (originOptions == null) {
            originOptions = this.randomOriginOptions;
        }

        RankSystem rankSystem = presetRankSystem;
        if (rankSystem == null) {
            rankSystem = campaign.getRankSystem();
        }

        model = new BiographyOptionsModel(options, originOptions);
        updateCreatedControlsFromModel();

        // Ranks
        rankSystemsPane.getComboRankSystems().setSelectedItem(rankSystem);
    }

    /**
     * Applies the current settings from the market UI components back into the
     * {@link CampaignOptions} of the
     * associated campaign.
     * <p>
     * If a preset options object is provided, the changes are applied there.
     * Otherwise, they are applied to the current
     * campaign's options.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object to update with
     *                              the current UI settings, or
     *                              {@code null} to apply changes to the campaign's
     *                              options directly.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        RandomOriginOptions originOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
            originOptions = this.randomOriginOptions;
        } else {
            originOptions = options.getRandomOriginOptions();
        }

        updateModelFromCreatedControls();
        model.applyTo(options, originOptions);

        // Ranks
        rankSystemsPane.applyToCampaign();
    }

    private void updateCreatedControlsFromModel() {
        updateGeneralControlsFromModel();
        updateBackgroundControlsFromModel();
        updateDeathControlsFromModel();
        updateEducationControlsFromModel();
        updateNameAndPortraitControlsFromModel();
    }

    private void updateGeneralControlsFromModel() {
        if (!generalPageCreated || model == null) {
            return;
        }

        chkUseDylansRandomXP.setSelected(model.useDylansRandomXP);
        spnGender.setValue(model.percentFemale);
        spnNonBinaryDiceSize.setValue(model.nonBinaryDiceSize);
        comboFamilyDisplayLevel.setSelectedItem(model.familyDisplayLevel);
        chkAnnounceOfficersOnly.setSelected(model.announceOfficersOnly);
        chkAnnounceBirthdays.setSelected(model.announceBirthdays);
        chkAnnounceChildBirthdays.setSelected(model.announceChildBirthdays);
        chkAnnounceRecruitmentAnniversaries.setSelected(model.announceRecruitmentAnniversaries);
        chkShowLifeEventDialogBirths.setSelected(model.showLifeEventDialogBirths);
        chkShowLifeEventDialogComingOfAge.setSelected(model.showLifeEventDialogComingOfAge);
        chkShowLifeEventDialogCelebrations.setSelected(model.showLifeEventDialogCelebrations);
        chkVeterancySPAs.setSelected(model.awardVeterancySPAs);
        chkAwardRelevantVeterancySPAs.setSelected(model.awardRelevantVeterancySPAs);
        chkComingOfAgeSPAs.setSelected(model.rewardComingOfAgeAbilities);
        chkRewardComingOfAgeRPSkills.setSelected(model.rewardComingOfAgeRPSkills);
    }

    private void updateBackgroundControlsFromModel() {
        if (!backgroundsPageCreated || model == null) {
            return;
        }

        chkUseRandomPersonalities.setSelected(model.useRandomPersonalities);
        chkUseRandomPersonalityReputation.setSelected(model.useRandomPersonalityReputation);
        chkUseReasoningXpMultiplier.setSelected(model.useReasoningXpMultiplier);
        chkUseSimulatedRelationships.setSelected(model.useSimulatedRelationships);
        chkRandomizeOrigin.setSelected(model.randomizeOrigin);
        chkRandomizeDependentsOrigin.setSelected(model.randomizeDependentOrigin);
        chkRandomizeAroundSpecifiedPlanet.setSelected(model.randomizeAroundSpecifiedPlanet);
        if (model.specifiedPlanet != null) {
            comboSpecifiedSystem.setSelectedItem(model.specifiedPlanet.getParentSystem());
            comboSpecifiedPlanet.setSelectedItem(model.specifiedPlanet);
        }
        spnOriginSearchRadius.setValue(model.originSearchRadius);
        spnOriginDistanceScale.setValue(model.originDistanceScale);
        chkAllowClanOrigins.setSelected(model.allowClanOrigins);
        chkExtraRandomOrigin.setSelected(model.extraRandomOrigin);
    }

    private void updateDeathControlsFromModel() {
        if (!deathPageCreated || model == null) {
            return;
        }

        chkUseRandomDeathSuicideCause.setSelected(model.useRandomDeathSuicideCause);
        spnRandomDeathMultiplier.setValue(model.randomDeathMultiplier);
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            JCheckBox ageGroupCheckBox = chkEnabledRandomDeathAgeGroups.get(ageGroup);
            if (ageGroupCheckBox != null) {
                ageGroupCheckBox.setSelected(Boolean.TRUE.equals(model.enabledRandomDeathAgeGroups.get(ageGroup)));
            }
        }
    }

    private void updateEducationControlsFromModel() {
        if (!educationPageCreated || model == null) {
            return;
        }

        chkUseEducationModule.setSelected(model.useEducationModule);
        spnCurriculumXpRate.setValue(model.curriculumXpRate);
        spnMaximumJumpCount.setValue(model.maximumJumpCount);
        chkUseReeducationCamps.setSelected(model.useReeducationCamps);
        chkEnableOverrideRequirements.setSelected(model.enableOverrideRequirements);
        chkShowIneligibleAcademies.setSelected(model.enableShowIneligibleAcademies);
        spnEntranceExamBaseTargetNumber.setValue(model.entranceExamBaseTargetNumber);
        chkEnableLocalAcademies.setSelected(model.enableLocalAcademies);
        chkEnablePrestigiousAcademies.setSelected(model.enablePrestigiousAcademies);
        chkEnableUnitEducation.setSelected(model.enableUnitEducation);
        chkEnableBonuses.setSelected(model.enableBonuses);
        spnFacultyXpMultiplier.setValue(model.facultyXpRate);
        spnAdultDropoutChance.setValue(model.adultDropoutChance);
        spnChildrenDropoutChance.setValue(model.childrenDropoutChance);
        chkAllAges.setSelected(model.allAges);
        spnMilitaryAcademyAccidents.setValue(model.militaryAcademyAccidents);
    }

    private void updateNameAndPortraitControlsFromModel() {
        if (!nameAndPortraitPageCreated || model == null) {
            return;
        }

        chkUseOriginFactionForNames.setSelected(model.useOriginFactionForNames);
        comboFactionNames.setSelectedItem(model.factionNames);
        chkAssignPortraitOnRoleChange.setSelected(model.assignPortraitOnRoleChange);
        chkAllowDuplicatePortraits.setSelected(model.allowDuplicatePortraits);
        chkUseGenderedPortraitsOnly.setSelected(model.useGenderedPortraitsOnly);
        chkNoRandomPortraitsForChildren.setSelected(model.noRandomPortraitsForChildren);
        chkChildPortraitsWhenComingOfAge.setSelected(model.childPortraitsWhenComingOfAge);

        for (int i = 0; i < Math.min(chkUsePortrait.length, model.usePortraitForRole.length); i++) {
            if (chkUsePortrait[i] != null) {
                chkUsePortrait[i].setSelected(model.usePortraitForRole[i]);
            }
        }
    }

    private void updateModelFromCreatedControls() {
        updateModelFromGeneralControls();
        updateModelFromBackgroundControls();
        updateModelFromDeathControls();
        updateModelFromEducationControls();
        updateModelFromNameAndPortraitControls();
    }

    private void updateModelFromGeneralControls() {
        if (!generalPageCreated || model == null) {
            return;
        }

        model.useDylansRandomXP = chkUseDylansRandomXP.isSelected();
        model.percentFemale = (int) spnGender.getValue();
        model.nonBinaryDiceSize = (int) spnNonBinaryDiceSize.getValue();
        model.familyDisplayLevel = comboFamilyDisplayLevel.getSelectedItem();
        model.announceOfficersOnly = chkAnnounceOfficersOnly.isSelected();
        model.announceBirthdays = chkAnnounceBirthdays.isSelected();
        model.announceChildBirthdays = chkAnnounceChildBirthdays.isSelected();
        model.announceRecruitmentAnniversaries = chkAnnounceRecruitmentAnniversaries.isSelected();
        model.showLifeEventDialogBirths = chkShowLifeEventDialogBirths.isSelected();
        model.showLifeEventDialogComingOfAge = chkShowLifeEventDialogComingOfAge.isSelected();
        model.showLifeEventDialogCelebrations = chkShowLifeEventDialogCelebrations.isSelected();
        model.awardVeterancySPAs = chkVeterancySPAs.isSelected();
        model.awardRelevantVeterancySPAs = chkAwardRelevantVeterancySPAs.isSelected();
        model.rewardComingOfAgeAbilities = chkComingOfAgeSPAs.isSelected();
        model.rewardComingOfAgeRPSkills = chkRewardComingOfAgeRPSkills.isSelected();
    }

    private void updateModelFromBackgroundControls() {
        if (!backgroundsPageCreated || model == null) {
            return;
        }

        model.useRandomPersonalities = chkUseRandomPersonalities.isSelected();
        model.useRandomPersonalityReputation = chkUseRandomPersonalityReputation.isSelected();
        model.useReasoningXpMultiplier = chkUseReasoningXpMultiplier.isSelected();
        model.useSimulatedRelationships = chkUseSimulatedRelationships.isSelected();
        model.randomizeOrigin = chkRandomizeOrigin.isSelected();
        model.randomizeDependentOrigin = chkRandomizeDependentsOrigin.isSelected();
        model.randomizeAroundSpecifiedPlanet = chkRandomizeAroundSpecifiedPlanet.isSelected();
        model.specifiedPlanet = comboSpecifiedPlanet.getSelectedItem();
        model.originSearchRadius = (int) spnOriginSearchRadius.getValue();
        model.originDistanceScale = (double) spnOriginDistanceScale.getValue();
        model.allowClanOrigins = chkAllowClanOrigins.isSelected();
        model.extraRandomOrigin = chkExtraRandomOrigin.isSelected();
    }

    private void updateModelFromDeathControls() {
        if (!deathPageCreated || model == null) {
            return;
        }

        model.useRandomDeathSuicideCause = chkUseRandomDeathSuicideCause.isSelected();
        model.randomDeathMultiplier = (double) spnRandomDeathMultiplier.getValue();
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            JCheckBox ageGroupCheckBox = chkEnabledRandomDeathAgeGroups.get(ageGroup);
            if (ageGroupCheckBox != null) {
                model.enabledRandomDeathAgeGroups.put(ageGroup, ageGroupCheckBox.isSelected());
            }
        }
    }

    private void updateModelFromEducationControls() {
        if (!educationPageCreated || model == null) {
            return;
        }

        model.useEducationModule = chkUseEducationModule.isSelected();
        model.curriculumXpRate = (int) spnCurriculumXpRate.getValue();
        model.maximumJumpCount = (int) spnMaximumJumpCount.getValue();
        model.useReeducationCamps = chkUseReeducationCamps.isSelected();
        model.enableOverrideRequirements = chkEnableOverrideRequirements.isSelected();
        model.enableShowIneligibleAcademies = chkShowIneligibleAcademies.isSelected();
        model.entranceExamBaseTargetNumber = (int) spnEntranceExamBaseTargetNumber.getValue();
        model.enableLocalAcademies = chkEnableLocalAcademies.isSelected();
        model.enablePrestigiousAcademies = chkEnablePrestigiousAcademies.isSelected();
        model.enableUnitEducation = chkEnableUnitEducation.isSelected();
        model.enableBonuses = chkEnableBonuses.isSelected();
        model.facultyXpRate = (double) spnFacultyXpMultiplier.getValue();
        model.adultDropoutChance = (int) spnAdultDropoutChance.getValue();
        model.childrenDropoutChance = (int) spnChildrenDropoutChance.getValue();
        model.allAges = chkAllAges.isSelected();
        model.militaryAcademyAccidents = (int) spnMilitaryAcademyAccidents.getValue();
    }

    private void updateModelFromNameAndPortraitControls() {
        if (!nameAndPortraitPageCreated || model == null) {
            return;
        }

        model.useOriginFactionForNames = chkUseOriginFactionForNames.isSelected();
        model.factionNames = comboFactionNames.getSelectedItem();
        model.assignPortraitOnRoleChange = chkAssignPortraitOnRoleChange.isSelected();
        model.allowDuplicatePortraits = chkAllowDuplicatePortraits.isSelected();
        model.useGenderedPortraitsOnly = chkUseGenderedPortraitsOnly.isSelected();
        model.noRandomPortraitsForChildren = chkNoRandomPortraitsForChildren.isSelected();
        model.childPortraitsWhenComingOfAge = chkChildPortraitsWhenComingOfAge.isSelected();

        for (int i = 0; i < Math.min(chkUsePortrait.length, model.usePortraitForRole.length); i++) {
            if (chkUsePortrait[i] == null) {
                continue;
            }
            if (i == chkUsePortrait.length - 1) {
                for (PersonnelRole role : PersonnelRole.getCivilianRoles()) {
                    if (role.ordinal() < model.usePortraitForRole.length) {
                        model.usePortraitForRole[role.ordinal()] = chkUsePortrait[i].isSelected();
                    }
                }
                continue;
            }
            model.usePortraitForRole[i] = chkUsePortrait[i].isSelected();
        }
    }

}
