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
 */
package mekhq.gui.campaignOptions.contents;

import static megamek.client.generator.RandomGenderGenerator.getPercentFemale;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.*;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
import mekhq.gui.panes.RankSystemsPane;

/**
 * The `BiographyTab` class is responsible for managing the biography-related settings in the campaign options tab
 * within the MekHQ application. It provides an interface for configuring various campaign settings, such as:
 * <ul>
 *     <li>General campaign settings like gender distribution and familial relationships.</li>
 *     <li>Background options, including randomized personality traits and origin determination.</li>
 *     <li>Death-related settings such as probability of random deaths, age-group-specific deaths, etc.</li>
 *     <li>Education module settings for academic progression, dropout chances, and related configurations.</li>
 *     <li>Random name generation and portrait assignment based on roles and factions.</li>
 *     <li>Rank and hierarchy management for campaign personnel.</li>
 * </ul>
 *
 * The class includes methods to initialize, load, and configure settings while providing GUI tools to enable user
 * interaction. It also integrates with the current `Campaign` and `CampaignOptions` objects to synchronize settings.
 * This class serves as the backbone for displaying and managing the "Biography" tab in the campaign options dialog.
 */
public class BiographyTab {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private Campaign campaign;
    private GeneralTab generalTab;
    private CampaignOptions campaignOptions;
    private RandomOriginOptions randomOriginOptions;

    //start General Tab
    private JCheckBox chkUseDylansRandomXP;
    private JLabel lblGender;
    private JSlider sldGender;
    private JLabel lblNonBinaryDiceSize;
    private JSpinner spnNonBinaryDiceSize;
    private JLabel lblFamilyDisplayLevel;
    private MMComboBox<FamilialRelationshipDisplayLevel> comboFamilyDisplayLevel;
    private JPanel pnlAnniversariesPanel;
    private JCheckBox chkAnnounceOfficersOnly;
    private JCheckBox chkAnnounceBirthdays;
    private JCheckBox chkAnnounceChildBirthdays;
    private JCheckBox chkAnnounceRecruitmentAnniversaries;
    //end General Tab

    //start Backgrounds Tab
    private JPanel pnlRandomBackgrounds;
    private JCheckBox chkUseRandomPersonalities;
    private JCheckBox chkUseRandomPersonalityReputation;
    private JCheckBox chkUseIntelligenceXpMultiplier;
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
    //end Backgrounds Tab

    //start Death Tab
    private JCheckBox chkUseRandomDeathSuicideCause;
    private JLabel lblRandomDeathMultiplier;
    private JSpinner spnRandomDeathMultiplier;

    private JPanel pnlDeathAgeGroup;
    private Map<AgeGroup, JCheckBox> chkEnabledRandomDeathAgeGroups;
    //end Death Tab

    //start Education Tab
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
    //end Education Tab

    //start Name and Portrait Tab
    private JCheckBox chkUseOriginFactionForNames;
    private JLabel lblFactionNames;
    private MMComboBox<String> comboFactionNames;

    private JPanel pnlRandomPortrait;
    private JCheckBox[] chkUsePortrait;
    private JButton btnEnableAllPortraits;
    private JButton btnDisableAllPortraits;
    private JCheckBox chkAssignPortraitOnRoleChange;
    private JCheckBox chkAllowDuplicatePortraits;
    //end Name and Portrait Tab

    //start Rank Tab
    private RankSystemsPane rankSystemsPane;
    //end Rank Tab

    /**
     * Constructs the `BiographyTab` and initializes the campaign and its dependent options.
     *
     * @param campaign             The current `Campaign` object to which the BiographyTab is linked. The campaign
     *                             options and origin options are derived from this object.
     * @param generalTab           The currently active General Tab.
     */
    public BiographyTab(Campaign campaign, GeneralTab generalTab) {
        this.campaign = campaign;
        this.generalTab = generalTab;
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomOriginOptions = campaignOptions.getRandomOriginOptions();

        initialize();
    }

    /**
     * Initializes the various sections and settings tabs within the BiographyTab.
     * This method organizes the following tabs:
     * <p>
     *     <li>General Tab: Handles general campaign settings such as gender distribution and
     *     relationship displays.</li>
     *     <li>Background Tab: Configures randomized backgrounds for campaign characters.</li>
     *     <li>Death Tab: Sets up random death rules and options.</li>
     *     <li>Education Tab: Defines education-related gameplay settings.</li>
     *     <li>Name and Portrait Tab: Configures rules for name and portrait generation.</li>
     *     <li>Rank Tab: Manages the rank systems for campaign personnel.</li>
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
     *     <li>Enable or disable the use of origin factions for name generation.</li>
     *     <li>Assign portraits to personnel upon role changes.</li>
     *     <li>Customize which portraits should be used randomly based on roles.</li>
     * </ul>
     */
    private void initializeNameAndPortraitTab() {
        chkUseOriginFactionForNames = new JCheckBox();
        lblFactionNames = new JLabel();
        comboFactionNames = new MMComboBox<>("comboFactionNames", getFactionNamesModel());
        chkAssignPortraitOnRoleChange = new JCheckBox();
        chkAllowDuplicatePortraits = new JCheckBox();

        pnlRandomPortrait = new JPanel();
        chkUsePortrait = new JCheckBox[1]; // We're going to properly initialize this later
        btnEnableAllPortraits = new JButton();
        btnDisableAllPortraits = new JButton();
    }

    /**
     * Initializes the Education tab, providing settings such as:
     * <ul>
     *     <li>Setting curriculum XP rates.</li>
     *     <li>Enabling re-education camps or specific academy requirements.</li>
     *     <li>Managing academy dropout chances for adults and children.</li>
     *     <li>Supporting the configuration of education-related accidents and events.</li>
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
     *     <li>Allowing configuration of random death probabilities for personnel.</li>
     *     <li>Customizing age-group-specific death settings.</li>
     *     <li>Selecting methods for random deaths (e.g., natural causes or accidents).</li>
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
     *     <li>Randomized background settings for characters.</li>
     *     <li>Options for specifying origins (e.g., faction-specific planetary systems).</li>
     *     <li>Custom search radius and distance scaling for randomized origins.</li>
     * </p>
     */
    private void initializeBackgroundsTab() {
        pnlRandomBackgrounds = new JPanel();
        chkUseRandomPersonalities = new JCheckBox();
        chkUseRandomPersonalityReputation = new JCheckBox();
        chkUseIntelligenceXpMultiplier = new JCheckBox();
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
     *     <li>General gameplay settings such as gender distribution sliders.</li>
     *     <li>Configuration of familial display levels and other general campaign settings.</li>
     *     <li>Annunciation of anniversaries, recruitment dates, and officer-related milestones.</li>
     * </p>
     */
    private void initializeGeneralTab() {
        chkUseDylansRandomXP = new JCheckBox();
        lblGender = new JLabel();
        sldGender = new JSlider();
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
    }

    /**
     * Builds and returns a `DefaultComboBoxModel` containing the names of all available factions.
     *
     * @return A `DefaultComboBoxModel` populated with faction names for random name generation rules.
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
     *     <li>Checkboxes for random XP distribution.</li>
     *     <li>Sliders for gender representation customization.</li>
     *     <li>Combo boxes for family display level settings within the GUI.</li>
     * </ul>
     *
     * @return A `JPanel` representing the General tab in the campaign options dialog.
     */
    public JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("BiographyGeneralTab",
            getImageDirectory() + "logo_clan_blood_spirit.png");

        // Contents
        chkUseDylansRandomXP = new CampaignOptionsCheckBox("UseDylansRandomXP");

        lblGender = new CampaignOptionsLabel("Gender");
        sldGender = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);

        lblNonBinaryDiceSize = new CampaignOptionsLabel("NonBinaryDiceSize");
        spnNonBinaryDiceSize = new CampaignOptionsSpinner("NonBinaryDiceSize",
            60, 0, 100000, 1);

        lblFamilyDisplayLevel = new CampaignOptionsLabel("FamilyDisplayLevel");

        pnlAnniversariesPanel = createAnniversariesPanel();

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("BiographyGeneralTabLeft", true);
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridy = 0;
        layoutLeft.gridx = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(chkUseDylansRandomXP, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblGender, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(sldGender, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblNonBinaryDiceSize, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnNonBinaryDiceSize, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblFamilyDisplayLevel, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(comboFamilyDisplayLevel, layoutLeft);

        final JPanel panelParent = new CampaignOptionsStandardPanel("BiographyGeneralTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);
        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);
        layoutParent.gridx++;
        panelParent.add(pnlAnniversariesPanel, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "BiographyGeneralTab");
    }

    /**
     * Creates the Anniversaries panel within the General tab for managing announcement-related settings:
     * <p>
     *     <li>Enabling birthday and recruitment anniversary announcements.</li>
     *     <li>Specifying whether such announcements should be limited to officers.</li>
     * </p>
     *
     * @return A `JPanel` containing the UI components for defining anniversary-related settings.
     */
    private JPanel createAnniversariesPanel() {
        // Contents
        chkAnnounceBirthdays = new CampaignOptionsCheckBox("AnnounceBirthdays");
        chkAnnounceRecruitmentAnniversaries = new CampaignOptionsCheckBox("AnnounceRecruitmentAnniversaries");
        chkAnnounceOfficersOnly = new CampaignOptionsCheckBox("AnnounceOfficersOnly");
        chkAnnounceChildBirthdays = new CampaignOptionsCheckBox("AnnounceChildBirthdays");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AnniversariesPanel", true,
            "AnniversariesPanel");
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(chkAnnounceBirthdays, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(chkAnnounceRecruitmentAnniversaries, layoutParent);

        layoutParent.gridy++;
        panel.add(chkAnnounceOfficersOnly, layoutParent);

        layoutParent.gridy++;
        panel.add(chkAnnounceChildBirthdays, layoutParent);

        return panel;
    }

    /**
     * Creates and lays out the Backgrounds tab, which includes:
     * <ul>
     *     <li>Settings for enabling randomized personalities and relationships.</li>
     *     <li>Random origin configurations such as faction specificity and distance scaling.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Backgrounds tab in the campaign options dialog.
     */
    public JPanel createBackgroundsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("BackgroundsTab",
            getImageDirectory() + "logo_nueva_castile.png");

        // Contents
        pnlRandomOriginOptions = createRandomOriginOptionsPanel();
        pnlRandomBackgrounds = createRandomBackgroundsPanel();

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("BackgroundsTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlRandomOriginOptions, layout);

        layout.gridx++;
        panel.add(pnlRandomBackgrounds, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "BackgroundsTab");
    }

    /**
     * Creates the panel for configuring random background options in the campaign.
     * <p>
     * This includes controls to enable or disable features such as:
     * <p>
     *     <li>Random personalities for characters.</li>
     *     <li>Random personality reputation.</li>
     *     <li>Intelligence XP multipliers.</li>
     *     <li>Simulated relationships.</li>
     * </p>
     *
     * @return A {@code JPanel} representing the random background configuration UI.
     */
    JPanel createRandomBackgroundsPanel() {
        // Contents
        chkUseRandomPersonalities = new CampaignOptionsCheckBox("UseRandomPersonalities");
        chkUseRandomPersonalityReputation = new CampaignOptionsCheckBox("UseRandomPersonalityReputation");
        chkUseIntelligenceXpMultiplier = new CampaignOptionsCheckBox("UseIntelligenceXpMultiplier");
        chkUseSimulatedRelationships = new CampaignOptionsCheckBox("UseSimulatedRelationships");

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("RandomBackgroundsPanel", true,
            "RandomBackgroundsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkUseRandomPersonalities, layout);

        layout.gridy++;
        panel.add(chkUseRandomPersonalityReputation, layout);

        layout.gridy++;
        panel.add(chkUseIntelligenceXpMultiplier, layout);

        layout.gridy++;
        panel.add(chkUseSimulatedRelationships, layout);

        return panel;
    }

    /**
     * Creates and returns a panel for random origin options. This includes:
     * <p>
     *     <li>Controls to enable or disable randomization of origins.</li>
     *     <li>Options for selecting specific planetary systems or factions for origin determination.</li>
     *     <li>Search radius and distance scaling fields to tweak origin calculations.</li>
     * </p>
     *
     * @return A `JPanel` for managing random origin settings.
     */
    private JPanel createRandomOriginOptionsPanel() {
        // Contents
        chkRandomizeOrigin = new CampaignOptionsCheckBox("RandomizeOrigin");
        chkRandomizeDependentsOrigin = new CampaignOptionsCheckBox("RandomizeDependentsOrigin");
        chkRandomizeAroundSpecifiedPlanet = new CampaignOptionsCheckBox("RandomizeAroundSpecifiedPlanet");
        chkSpecifiedSystemFactionSpecific.addActionListener(evt -> refreshSystemsAndPlanets());

        chkSpecifiedSystemFactionSpecific = new CampaignOptionsCheckBox("SpecifiedSystemFactionSpecific");
        chkSpecifiedSystemFactionSpecific.addActionListener(evt -> refreshSystemsAndPlanets());


        lblSpecifiedSystem = new CampaignOptionsLabel("SpecifiedSystem");
        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
              chkSpecifiedSystemFactionSpecific.isSelected() ? generalTab.getFaction() : null)));
        comboSpecifiedSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
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
            if ((planetarySystem == null)
                || ((planet != null) && !planet.getParentSystem().equals(planetarySystem))) {
                restoreComboSpecifiedPlanet();
            }
        });

        lblSpecifiedPlanet = new CampaignOptionsLabel("SpecifiedPlanet");
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        if (planetarySystem != null) {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(planetarySystem.getPlanets().toArray(new Planet[] { })));
        }
        comboSpecifiedPlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(generalTab.getDate()));
                }
                return this;
            }
        });

        lblOriginSearchRadius = new CampaignOptionsLabel("OriginSearchRadius");
        spnOriginSearchRadius = new CampaignOptionsSpinner("OriginSearchRadius",
            0, 0, 2000, 25);

        lblOriginDistanceScale = new CampaignOptionsLabel("OriginDistanceScale");
        spnOriginDistanceScale = new CampaignOptionsSpinner("OriginDistanceScale",
            0.6, 0.1, 2.0, 0.1);

        chkAllowClanOrigins = new CampaignOptionsCheckBox("AllowClanOrigins");
        chkExtraRandomOrigin = new CampaignOptionsCheckBox("ExtraRandomOrigin");

        // Layout the Panel
        final JPanel panelSystemPlanetOrigins = new CampaignOptionsStandardPanel(
            "RandomOriginOptionsPanelSystemPlanetOrigins", false, "");
        final GridBagConstraints layoutSystemPlanetOrigins = new CampaignOptionsGridBagConstraints(panelSystemPlanetOrigins);

        layoutSystemPlanetOrigins.gridwidth = 1;
        layoutSystemPlanetOrigins.gridx = 0;
        layoutSystemPlanetOrigins.gridy = 0;
        panelSystemPlanetOrigins.add(lblSpecifiedSystem, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(comboSpecifiedSystem, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(lblSpecifiedPlanet, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(comboSpecifiedPlanet, layoutSystemPlanetOrigins);

        layoutSystemPlanetOrigins.gridx = 0;
        layoutSystemPlanetOrigins.gridy++;
        panelSystemPlanetOrigins.add(lblOriginSearchRadius, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(spnOriginSearchRadius, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(lblOriginDistanceScale, layoutSystemPlanetOrigins);
        layoutSystemPlanetOrigins.gridx++;
        panelSystemPlanetOrigins.add(spnOriginDistanceScale, layoutSystemPlanetOrigins);

        final JPanel panel = new CampaignOptionsStandardPanel("RandomOriginOptionsPanel", true,
            "RandomOriginOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkRandomizeOrigin, layout);

        layout.gridy++;
        panel.add(chkRandomizeDependentsOrigin, layout);

        layout.gridy++;
        panel.add(chkRandomizeAroundSpecifiedPlanet, layout);

        layout.gridy++;
        panel.add(chkSpecifiedSystemFactionSpecific, layout);

        layout.gridy++;
        panel.add(panelSystemPlanetOrigins, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkAllowClanOrigins, layout);

        layout.gridy++;
        panel.add(chkExtraRandomOrigin, layout);

        return panel;
    }

    /**
     * Refreshes the planetary systems and planets displayed in the associated combo boxes.
     *
     * <p>This method first stores the currently selected planetary system and planet. It then
     * restores the list of available planetary systems by repopulating the `comboSpecifiedSystem`.
     * Finally, it reselects the previously selected planetary system and planet in their respective
     * combo boxes.</p>
     *
     * <p>The method ensures that the user selection persists even after the combo boxes are refreshed.
     * Any exceptions during the selection process are caught and ignored. As if we can't restore
     * the selection, that's fine, we just use the fallback index of 0.</p>
     */
    private void refreshSystemsAndPlanets() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        final Planet planet = comboSpecifiedPlanet.getSelectedItem();

        restoreComboSpecifiedSystem();

        try {
            comboSpecifiedSystem.setSelectedItem(planetarySystem);
            comboSpecifiedPlanet.setSelectedItem(planet);
        } catch (Exception ignored) {}
    }

    /**
     * Resets the planet combo box to show only the planets matching the currently selected planetary system.
     */
    private void restoreComboSpecifiedPlanet() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();

        if (planetarySystem == null) {
            comboSpecifiedPlanet.removeAllItems();
        } else {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(
                planetarySystem.getPlanets().toArray(new Planet[] {})));
            comboSpecifiedPlanet.setSelectedItem(planetarySystem.getPrimaryPlanet());
        }
    }

    /**
     * Resets the system combo box to show only the planetary systems that match the current faction, if applicable.
     */
    private void restoreComboSpecifiedSystem() {
        comboSpecifiedSystem.removeAllItems();

        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
            chkSpecifiedSystemFactionSpecific.isSelected() ? generalTab.getFaction() : null)));

        restoreComboSpecifiedPlanet();
    }

    /**
     * Filters planetary systems based on a given faction (if specified) and returns a sorted array of matches.
     *
     * @param faction The faction to filter planetary systems by (nullable). If `null`, all systems are included.
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
     *     <li>Methods for random death.</li>
     *     <li>Percentage-based chances for random death events.</li>
     *     <li>Check boxes to enable or disable age-specific death events.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Death tab.
     */
    public JPanel createDeathTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("DeathTab",
            getImageDirectory() + "logo_clan_fire_mandrills.png");

        // Contents
        lblRandomDeathMultiplier = new CampaignOptionsLabel("RandomDeathMultiplier");
        spnRandomDeathMultiplier = new CampaignOptionsSpinner("RandomDeathMultiplier",
            1.0, 0, 100.0, 0.01);

        chkUseRandomDeathSuicideCause = new CampaignOptionsCheckBox("UseRandomDeathSuicideCause");

        pnlDeathAgeGroup = createDeathAgeGroupsPanel();

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("DeathTabLeft", true);
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridy = 0;
        layoutLeft.gridx = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(lblRandomDeathMultiplier, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnRandomDeathMultiplier, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseRandomDeathSuicideCause, layoutLeft);

        layoutLeft.gridy++;

        final JPanel panelParent = new CampaignOptionsStandardPanel("DeathTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(pnlDeathAgeGroup, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "DeathTab");
    }

    /**
     * Configures and creates a panel where users can enable or disable random death probabilities for specific age groups.
     *
     * @return A `JPanel` containing the random death age group options.
     */
    private JPanel createDeathAgeGroupsPanel() {
        final AgeGroup[] ageGroups = AgeGroup.values();

        // Create the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("DeathAgeGroupsPanel", true,
            "DeathAgeGroupsPanel");
        panel.setLayout(new GridLayout((ageGroups.length / 2) + 1, 1));

        // Contents
        for (final AgeGroup ageGroup : ageGroups) {
            final JCheckBox checkBox = new JCheckBox(ageGroup.toString());
            checkBox.setToolTipText(ageGroup.getToolTipText());
            checkBox.setName("chk" + ageGroup);

            panel.add(checkBox);
            chkEnabledRandomDeathAgeGroups.put(ageGroup, checkBox);
        }

        return panel;
    }

    /**
     * Creates the Education tab, which allows managing educational settings within the campaign.
     * <p>
     * This includes:
     * <ul>
     *     <li>Setting curriculum XP rates.</li>
     *     <li>Configuring academy requirements and override options.</li>
     *     <li>Managing dropout chances for adults and children.</li>
     *     <li>Enabling or disabling the use of reeducation camps, accidents, and events.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Education tab in the campaign UI.
     */
    public JPanel createEducationTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("EducationTab",
            getImageDirectory() + "logo_taurian_concordat.png");

        // Contents
        chkUseEducationModule = new CampaignOptionsCheckBox("UseEducationModule");

        lblCurriculumXpRate = new CampaignOptionsLabel("CurriculumXpRate");
        spnCurriculumXpRate = new CampaignOptionsSpinner("CurriculumXpRate",
            3, 1, 10, 1);

        lblMaximumJumpCount = new CampaignOptionsLabel("MaximumJumpCount");
        spnMaximumJumpCount = new CampaignOptionsSpinner("MaximumJumpCount",
            5, 1, 200, 1);

        chkUseReeducationCamps = new CampaignOptionsCheckBox("UseReeducationCamps");

        chkEnableOverrideRequirements = new CampaignOptionsCheckBox("EnableOverrideRequirements");

        chkShowIneligibleAcademies = new CampaignOptionsCheckBox("ShowIneligibleAcademies");

        lblEntranceExamBaseTargetNumber = new CampaignOptionsLabel("EntranceExamBaseTargetNumber");
        spnEntranceExamBaseTargetNumber = new CampaignOptionsSpinner("EntranceExamBaseTargetNumber",
            14, 0, 20, 1);

        pnlEnableStandardSets = createEnableStandardSetsPanel();

        pnlXpAndSkillBonuses = createXpAndSkillBonusesPanel();

        pnlDropoutChance = createDropoutChancePanel();

        pnlAccidentsAndEvents = createAccidentsAndEventsPanel();

        // Layout the Panels
        final JPanel panelLeft = new CampaignOptionsStandardPanel("EducationTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridwidth = 5;
        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        panelLeft.add(headerPanel, layoutLeft);

        layoutLeft.gridy++;
        layoutLeft.gridwidth = 1;
        panelLeft.add(chkUseEducationModule, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblCurriculumXpRate, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnCurriculumXpRate, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMaximumJumpCount, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaximumJumpCount, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseReeducationCamps, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkEnableOverrideRequirements, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkShowIneligibleAcademies, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblEntranceExamBaseTargetNumber, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnEntranceExamBaseTargetNumber, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(pnlEnableStandardSets, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("EducationTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridy++;
        layoutRight.gridwidth = 1;
        panelRight.add(panelLeft, layoutRight);
        layoutRight.gridy++;
        panelRight.add(pnlXpAndSkillBonuses, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlDropoutChance, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlAccidentsAndEvents, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("EducationTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "EducationTab");
    }

    /**
     * Creates a panel for enabling different education-related academy sets.
     * <p>
     * This includes options to toggle various academy types:
     * <p>
     *     <li>Local academies.</li>
     *     <li>Prestigious academies.</li>
     *     <li>Unit-based education academies.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the Enable Standard Sets UI components.
     */
    private JPanel createEnableStandardSetsPanel() {
        chkEnableLocalAcademies = new CampaignOptionsCheckBox("EnableLocalAcademies");
        chkEnablePrestigiousAcademies = new CampaignOptionsCheckBox("EnablePrestigiousAcademies");
        chkEnableUnitEducation = new CampaignOptionsCheckBox("EnableUnitEducation");

        final JPanel panel = new CampaignOptionsStandardPanel("EnableStandardSetsPanel", true,
            "EnableStandardSetsPanel");

        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkEnableLocalAcademies, layout);

        layout.gridy++;
        panel.add(chkEnablePrestigiousAcademies, layout);

        layout.gridy++;
        panel.add(chkEnableUnitEducation, layout);

        return panel;
    }

    /**
     * Creates a panel for configuring experience gain and skill bonuses.
     * <p>
     * This includes:
     * <p>
     *     <li>Option to enable or disable bonuses.</li>
     *     <li>Setting the faculty XP multiplier.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing XP rates and skill bonuses.
     */
    private JPanel createXpAndSkillBonusesPanel() {
        // Contents
        chkEnableBonuses = new CampaignOptionsCheckBox("EnableBonuses");
        lblFacultyXpMultiplier = new CampaignOptionsLabel("FacultyXpMultiplier");
        spnFacultyXpMultiplier = new CampaignOptionsSpinner("FacultyXpMultiplier",
            1.00, 0.00, 10.00, 0.01);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("XpAndSkillBonusesPanel", true,
            "XpAndSkillBonusesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        panel.add(chkEnableBonuses, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblFacultyXpMultiplier, layout);
        layout.gridx++;
        panel.add(spnFacultyXpMultiplier, layout);

        return panel;
    }

    /**
     * Creates a panel for configuring dropout chances for academies.
     * <p>
     * This includes:
     * <p>
     *     <li>Setting the dropout chance for adults.</li>
     *     <li>Setting the dropout chance for children.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing dropout chance settings.
     */
    private JPanel createDropoutChancePanel() {
        // Contents
        lblAdultDropoutChance = new CampaignOptionsLabel("AdultDropoutChance");
        spnAdultDropoutChance = new CampaignOptionsSpinner("AdultDropoutChance",
            1000, 0, 100000, 1);
        lblChildrenDropoutChance = new CampaignOptionsLabel("ChildrenDropoutChance");
        spnChildrenDropoutChance = new CampaignOptionsSpinner("ChildrenDropoutChance",
            10000, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("DropoutChancePanel", true,
            "DropoutChancePanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAdultDropoutChance, layout);
        layout.gridx++;
        panel.add(spnAdultDropoutChance, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblChildrenDropoutChance, layout);
        layout.gridx++;
        panel.add(spnChildrenDropoutChance, layout);

        return panel;
    }

    /**
     * Creates a panel for configuring accidents and events related to military academies.
     * <p>
     * This includes:
     * <p>
     *     <li>Toggling settings for all-age accidents.</li>
     *     <li>Configuring the frequency of military academy accidents.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the accidents and events configuration UI.
     */
    private JPanel createAccidentsAndEventsPanel() {
        // Contents
        chkAllAges = new CampaignOptionsCheckBox("AllAges");
        lblMilitaryAcademyAccidents = new CampaignOptionsLabel("MilitaryAcademyAccidents");
        spnMilitaryAcademyAccidents = new CampaignOptionsSpinner("MilitaryAcademyAccidents",
            10000, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AccidentsAndEventsPanel", true,
            "AccidentsAndEventsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkAllAges, layout);

        layout.gridy++;
        panel.add(lblMilitaryAcademyAccidents, layout);
        layout.gridx++;
        panel.add(spnMilitaryAcademyAccidents, layout);

        return panel;
    }

    /**
     * Creates the Name and Portrait Generation tab for the campaign options.
     * <p>
     * This tab allows users to:
     * </p>
     * <ul>
     *     <li>Enable or disable the use of origin factions for name generation.</li>
     *     <li>Assign portraits to personnel upon role changes.</li>
     *     <li>Customize which portraits are randomly used for specific roles.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Name and Portrait Generation tab.
     */
    public JPanel createNameAndPortraitGenerationTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("NameAndPortraitGenerationTab",
            getImageDirectory() + "logo_clan_nova_cat.png");

        // Contents
        chkAssignPortraitOnRoleChange = new CampaignOptionsCheckBox("AssignPortraitOnRoleChange");

        chkAllowDuplicatePortraits = new CampaignOptionsCheckBox("AllowDuplicatePortraits");

        chkUseOriginFactionForNames = new CampaignOptionsCheckBox("UseOriginFactionForNames");

        lblFactionNames = new CampaignOptionsLabel("FactionNames");

        pnlRandomPortrait = createRandomPortraitPanel();

        // Layout the Panels
        final JPanel panelTop = new CampaignOptionsStandardPanel("NameAndPortraitGenerationTab");
        final GridBagConstraints layoutTop = new CampaignOptionsGridBagConstraints(panelTop);

        layoutTop.gridwidth = 1;
        layoutTop.gridx = 0;
        layoutTop.gridy = 0;
        panelTop.add(chkAssignPortraitOnRoleChange, layoutTop);
        layoutTop.gridy++;
        panelTop.add(chkAllowDuplicatePortraits, layoutTop);

        layoutTop.gridy++;
        panelTop.add(chkUseOriginFactionForNames, layoutTop);
        layoutTop.gridx++;
        panelTop.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(25)));
        layoutTop.gridx++;
        panelTop.add(lblFactionNames, layoutTop);
        layoutTop.gridx++;
        panelTop.add(comboFactionNames, layoutTop);

        final JPanel panel = new CampaignOptionsStandardPanel("NameAndPortraitGenerationTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(panelTop, layoutParent);

        layoutParent.gridy++;
        panel.add(pnlRandomPortrait, layoutParent);


        // Create Parent Panel and return
        return createParentPanel(panel, "NameAndPortraitGenerationTab");
    }

    /**
     * Creates a panel for customizing random portrait generation for personnel roles.
     * <p>
     * This includes:
     * <p>
     *     <li>Options to enable or disable the use of role-specific portraits.</li>
     *     <li>Buttons to toggle all or no portrait options collectively.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the random portrait generation configuration UI.
     */
    private JPanel createRandomPortraitPanel() {
        // Contents
        final PersonnelRole[] personnelRoles = PersonnelRole.values();

        chkUsePortrait = new JCheckBox[personnelRoles.length];

        btnEnableAllPortraits = new CampaignOptionsButton("EnableAllPortraits");
        btnEnableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                checkBox.setSelected(true);
            }
        });

        btnDisableAllPortraits = new CampaignOptionsButton("DisableAllPortraits");
        btnDisableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                checkBox.setSelected(false);
            }
        });

        // Layout the Panel
        JPanel panel = new JPanel(
            new GridLayout((int) Math.ceil((personnelRoles.length + 2) / 5.0), 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            String.format(String.format("<html>%s</html>",
                resources.getString("lblRandomPortraitPanel.text")))));

        panel.add(btnEnableAllPortraits);
        panel.add(btnDisableAllPortraits);

        // Add remaining checkboxes
        JCheckBox jCheckBox;
        for (final PersonnelRole role : PersonnelRole.values()) {
            jCheckBox = new JCheckBox(role.toString());
            panel.add(jCheckBox);
            chkUsePortrait[role.ordinal()] = jCheckBox;
        }

        return panel;
    }

    /**
     * Creates the Rank tab for configuring rank systems within the campaign.
     * <p>
     * This tab provides options for:
     * <ul>
     *     <li>Managing rank systems for personnel in the campaign.</li>
     *     <li>Displaying rank-related UI components for user configuration.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Rank tab in the campaign configuration.
     */
    public JPanel createRankTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("RankTab",
            getImageDirectory() + "logo_umayyad_caliphate.png", true);

        // Contents
        Component rankSystemsViewport = rankSystemsPane.getViewport().getView();
        rankSystemsPane.applyToCampaign();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("RankTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(rankSystemsViewport, layoutParent);


        // Create Parent Panel and return
        return createParentPanel(panel, "RankTab");
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null, null);
    }

    /**
     * Loads values from campaign options, optionally integrating with presets for default settings.
     *
     * @param presetCampaignOptions Optional preset campaign options, or `null` to use the campaign's active settings.
     * @param presetRandomOriginOptions Optional random origin options, or `null` to use the default origin settings.
     * @param presetRankSystem Optional rank system, or `null` to use the default system.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
                                              @Nullable RandomOriginOptions presetRandomOriginOptions,
                                              @Nullable RankSystem presetRankSystem) {
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

        // General
        chkUseDylansRandomXP.setSelected(options.isUseDylansRandomXP());
        sldGender.setValue(getPercentFemale()); // 'getPercentFemale' is not stored in a Preset
        spnNonBinaryDiceSize.setValue(options.getNonBinaryDiceSize());
        comboFamilyDisplayLevel.setSelectedItem(options.getFamilyDisplayLevel());
        chkAnnounceOfficersOnly.setSelected(options.isAnnounceOfficersOnly());
        chkAnnounceBirthdays.setSelected(options.isAnnounceBirthdays());
        chkAnnounceChildBirthdays.setSelected(options.isAnnounceChildBirthdays());
        chkAnnounceRecruitmentAnniversaries.setSelected(options.isAnnounceRecruitmentAnniversaries());

        // Backgrounds
        chkUseRandomPersonalities.setSelected(options.isUseRandomPersonalities());
        chkUseRandomPersonalityReputation.setSelected(options.isUseRandomPersonalityReputation());
        chkUseIntelligenceXpMultiplier.setSelected(options.isUseIntelligenceXpMultiplier());
        chkUseSimulatedRelationships.setSelected(options.isUseSimulatedRelationships());
        chkRandomizeOrigin.setSelected(originOptions.isRandomizeOrigin());
        chkRandomizeDependentsOrigin.setSelected(originOptions.isRandomizeDependentOrigin());
        chkRandomizeAroundSpecifiedPlanet.setSelected(originOptions.isRandomizeAroundSpecifiedPlanet());
        comboSpecifiedSystem.setSelectedItem(originOptions.getSpecifiedPlanet().getParentSystem());
        comboSpecifiedPlanet.setSelectedItem(originOptions.getSpecifiedPlanet());
        spnOriginSearchRadius.setValue(originOptions.getOriginSearchRadius());
        spnOriginDistanceScale.setValue(originOptions.getOriginDistanceScale());
        chkAllowClanOrigins.setSelected(originOptions.isAllowClanOrigins());
        chkExtraRandomOrigin.setSelected(originOptions.isExtraRandomOrigin());

        // Death
        chkUseRandomDeathSuicideCause.setSelected(options.isUseRandomDeathSuicideCause());
        spnRandomDeathMultiplier.setValue(options.getRandomDeathMultiplier());

        Map<AgeGroup, Boolean> deathAgeGroups = options.getEnabledRandomDeathAgeGroups();
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            chkEnabledRandomDeathAgeGroups.get(ageGroup).setSelected(deathAgeGroups.get(ageGroup));
        }

        // Education
        chkUseEducationModule.setSelected(options.isUseEducationModule());
        spnCurriculumXpRate.setValue(options.getCurriculumXpRate());
        spnMaximumJumpCount.setValue(options.getMaximumJumpCount());
        chkUseReeducationCamps.setSelected(options.isUseReeducationCamps());
        chkEnableOverrideRequirements.setSelected(options.isEnableOverrideRequirements());
        chkShowIneligibleAcademies.setSelected(options.isEnableShowIneligibleAcademies());
        spnEntranceExamBaseTargetNumber.setValue(options.getEntranceExamBaseTargetNumber());
        chkEnableLocalAcademies.setSelected(options.isEnableLocalAcademies());
        chkEnablePrestigiousAcademies.setSelected(options.isEnablePrestigiousAcademies());
        chkEnableUnitEducation.setSelected(options.isEnableUnitEducation());
        chkEnableBonuses.setSelected(options.isEnableBonuses());
        spnFacultyXpMultiplier.setValue(options.getFacultyXpRate());
        spnAdultDropoutChance.setValue(options.getAdultDropoutChance());
        spnChildrenDropoutChance.setValue(options.getChildrenDropoutChance());
        chkAllAges.setSelected(options.isAllAges());
        spnMilitaryAcademyAccidents.setValue(options.getMilitaryAcademyAccidents());

        // Name and Portraits
        chkUseOriginFactionForNames.setSelected(options.isUseOriginFactionForNames());
        // 'RandomNameGenerator' is not stored in a Preset
        comboFactionNames.setSelectedItem(RandomNameGenerator.getInstance().getChosenFaction());
        chkAssignPortraitOnRoleChange.setSelected(options.isAssignPortraitOnRoleChange());
        chkAllowDuplicatePortraits.setSelected(options.isAllowDuplicatePortraits());

        final boolean[] usePortraitForRole = options.isUsePortraitForRoles();
        for (int i = 0; i < chkUsePortrait.length; i++) {
            chkUsePortrait[i].setSelected(usePortraitForRole[i]);
        }

        // Ranks
        rankSystemsPane.getComboRankSystems().setSelectedItem(rankSystem);
    }

    /**
     * Applies the current settings from the market UI components back into
     * the {@link CampaignOptions} of the associated campaign.
     * <p>
     * If a preset options object is provided, the changes are applied there.
     * Otherwise, they are applied to the current campaign's options.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object to update
     *                              with the current UI settings, or {@code null}
     *                              to apply changes to the campaign's options directly.
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

        // General
        options.setUseDylansRandomXP(chkUseDylansRandomXP.isSelected());
        RandomGenderGenerator.setPercentFemale(sldGender.getValue());
        options.setNonBinaryDiceSize((int) spnNonBinaryDiceSize.getValue());
        options.setFamilyDisplayLevel(comboFamilyDisplayLevel.getSelectedItem());
        options.setAnnounceOfficersOnly(chkAnnounceOfficersOnly.isSelected());
        options.setAnnounceBirthdays(chkAnnounceBirthdays.isSelected());
        options.setAnnounceChildBirthdays(chkAnnounceChildBirthdays.isSelected());
        options.setAnnounceRecruitmentAnniversaries(chkAnnounceRecruitmentAnniversaries.isSelected());

        // Backgrounds
        options.setUseRandomPersonalities(chkUseRandomPersonalities.isSelected());
        options.setUseRandomPersonalityReputation(chkUseRandomPersonalityReputation.isSelected());
        options.setUseIntelligenceXpMultiplier(chkUseIntelligenceXpMultiplier.isSelected());
        options.setUseSimulatedRelationships(chkUseSimulatedRelationships.isSelected());

        originOptions.setRandomizeOrigin(chkRandomizeOrigin.isSelected());
        originOptions.setRandomizeDependentOrigin(chkRandomizeDependentsOrigin.isSelected());
        originOptions.setRandomizeAroundSpecifiedPlanet(chkRandomizeAroundSpecifiedPlanet.isSelected());

        Planet selectedPlanet = comboSpecifiedPlanet.getSelectedItem();
        originOptions.setSpecifiedPlanet(selectedPlanet == null
                                               ? Systems.getInstance().getSystemById("Terra").getPrimaryPlanet()
                                               : selectedPlanet);

        originOptions.setOriginSearchRadius((int) spnOriginSearchRadius.getValue());
        originOptions.setOriginDistanceScale((double) spnOriginDistanceScale.getValue());
        originOptions.setAllowClanOrigins(chkAllowClanOrigins.isSelected());
        originOptions.setExtraRandomOrigin(chkExtraRandomOrigin.isSelected());
        options.setRandomOriginOptions(originOptions);

        // Death
        options.setUseRandomDeathSuicideCause(chkUseRandomDeathSuicideCause.isSelected());
        options.setRandomDeathMultiplier((double) spnRandomDeathMultiplier.getValue());
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            options.getEnabledRandomDeathAgeGroups().put(ageGroup,
                chkEnabledRandomDeathAgeGroups.get(ageGroup).isSelected());
        }

        // Education
        options.setUseEducationModule(chkUseEducationModule.isSelected());
        options.setCurriculumXpRate((int) spnCurriculumXpRate.getValue());
        options.setMaximumJumpCount((int) spnMaximumJumpCount.getValue());
        options.setUseReeducationCamps(chkUseReeducationCamps.isSelected());
        options.setEnableOverrideRequirements(chkEnableOverrideRequirements.isSelected());
        options.setEnableShowIneligibleAcademies(chkShowIneligibleAcademies.isSelected());
        options.setEntranceExamBaseTargetNumber((int) spnEntranceExamBaseTargetNumber.getValue());
        options.setEnableLocalAcademies(chkEnableLocalAcademies.isSelected());
        options.setEnablePrestigiousAcademies(chkEnablePrestigiousAcademies.isSelected());
        options.setEnableUnitEducation(chkEnableUnitEducation.isSelected());
        options.setEnableBonuses(chkEnableBonuses.isSelected());
        options.setFacultyXpRate((double) spnFacultyXpMultiplier.getValue());
        options.setAdultDropoutChance((int) spnAdultDropoutChance.getValue());
        options.setChildrenDropoutChance((int) spnChildrenDropoutChance.getValue());
        options.setAllAges(chkAllAges.isSelected());
        options.setMilitaryAcademyAccidents((int) spnMilitaryAcademyAccidents.getValue());

        // Name and Portraits
        options.setUseOriginFactionForNames(chkUseOriginFactionForNames.isSelected());
        options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());
        options.setAllowDuplicatePortraits(chkAllowDuplicatePortraits.isSelected());
        RandomNameGenerator.getInstance().setChosenFaction(comboFactionNames.getSelectedItem());
        for (int i = 0; i < chkUsePortrait.length; i++) {
            options.setUsePortraitForRole(i, chkUsePortrait[i].isSelected());
        }

        // Ranks
        rankSystemsPane.applyToCampaign();
    }
}
