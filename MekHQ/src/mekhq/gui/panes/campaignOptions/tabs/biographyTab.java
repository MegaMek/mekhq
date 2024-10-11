package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.personnel.enums.RandomDeathMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class biographyTab {
    Campaign campaign;
    JFrame frame;
    String name;

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
    private JCheckBox chkKeepMarriedNameUponSpouseDeath;
    private JLabel lblRandomDeathMethod;
    private MMComboBox<RandomDeathMethod> comboRandomDeathMethod;
    private JCheckBox chkUseRandomClanPersonnelDeath;
    private JCheckBox chkUseRandomPrisonerDeath;
    private JCheckBox chkUseRandomDeathSuicideCause;
    private JLabel lblPercentageRandomDeathChance;
    private JSpinner spnPercentageRandomDeathChance;

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
    private JLabel lblEntranceExamBaseTargetNumberPost;

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

    /**
     * Initializes a new {@link biographyTab} with the specified campaign, frame, and name.
     *
     * @param campaign the campaign associated with the {@link biographyTab}
     * @param frame the {@link JFrame} used for displaying the {@link biographyTab}
     * @param name the name of the {@link biographyTab}
     */
    biographyTab(Campaign campaign, JFrame frame, String name) {
        this.campaign = campaign;
        this.frame = frame;
        this.name = name;

        initialize();
    }

    /**
     * Initializes the state and the UI components for the settings panels.
     * This includes General, Backgrounds, Marriage, Divorce, Procreation, and Death panels.
     * Each panel contains multiple aspects of settings such as Randomization, Dice Size, Display of
     * Anniversaries, Handling of Origin Options, Marriage & Divorce parameters, Probabilities related
     * to Procreation, and settings for Death scenarios among others.
     * {@link JCheckBox}, {@link JLabel}, {@link JSlider}, {@link JSpinner} and {@link MMComboBox}
     * components are thoroughly initialized in this method for handling various settings options.
     */
    protected void initialize() {
        // General Tab
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

        // Backgrounds Tab
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

        // Death Tab
        chkKeepMarriedNameUponSpouseDeath = new JCheckBox();
        lblRandomDeathMethod = new JLabel();
        comboRandomDeathMethod = new MMComboBox<>("comboRandomDeathMethod", RandomDeathMethod.values());
        chkUseRandomClanPersonnelDeath = new JCheckBox();
        chkUseRandomPrisonerDeath = new JCheckBox();
        chkUseRandomDeathSuicideCause = new JCheckBox();
        lblPercentageRandomDeathChance = new JLabel();
        spnPercentageRandomDeathChance = new JSpinner();

        pnlDeathAgeGroup = new JPanel();
        chkEnabledRandomDeathAgeGroups = new HashMap<>();

        // Education Tab
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
        lblEntranceExamBaseTargetNumberPost = new JLabel();

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
     * Creates a general tab for the Biography category containing various components
     *
     * @return a {@link JPanel} representing the general tab with multiple input components and panels
     */
    JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("BiographyGeneralTab",
            getImageDirectory() + "logo_federated_suns.png",
            false, "", true);

        // Contents
        chkUseDylansRandomXP = createCheckBox("UseDylansRandomXP", null);

        lblGender = createLabel("Gender", null);
        sldGender = new JSlider(SwingConstants.HORIZONTAL, 0, 100, RandomGenderGenerator.getPercentFemale());
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);

        lblNonBinaryDiceSize = createLabel("NonBinaryDiceSize", null);
        spnNonBinaryDiceSize = createSpinner("NonBinaryDiceSize", null,
            60, 0, 100000, 1);

        lblFamilyDisplayLevel = createLabel("FamilyDisplayLevel", null);

        pnlAnniversariesPanel = createAnniversariesPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("BiographyGeneralTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseDylansRandomXP)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblGender)
                    .addComponent(sldGender))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNonBinaryDiceSize)
                    .addComponent(spnNonBinaryDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFamilyDisplayLevel)
                    .addComponent(comboFamilyDisplayLevel))
                .addComponent(pnlAnniversariesPanel));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkUseDylansRandomXP)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblGender)
                        .addComponent(sldGender))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblNonBinaryDiceSize)
                        .addComponent(spnNonBinaryDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFamilyDisplayLevel)
                        .addComponent(comboFamilyDisplayLevel)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(pnlAnniversariesPanel)));

        // Create Parent Panel and return
        return createParentPanel(panel, "BiographyGeneralTab");
    }

    /**
     * @return a {@link JPanel} representing the Anniversaries panel with checkboxes for different
     * anniversary options
     */
    private JPanel createAnniversariesPanel() {
        // Contents
        chkAnnounceBirthdays = createCheckBox("AnnounceBirthdays", null);
        chkAnnounceRecruitmentAnniversaries = createCheckBox("AnnounceRecruitmentAnniversaries", null);
        chkAnnounceOfficersOnly = createCheckBox("AnnounceOfficersOnly", null);
        chkAnnounceChildBirthdays = createCheckBox("AnnounceChildBirthdays", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("AnniversariesPanel", true,
            "AnniversariesPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkAnnounceOfficersOnly)
                .addComponent(chkAnnounceBirthdays)
                .addComponent(chkAnnounceChildBirthdays)
                .addComponent(chkAnnounceRecruitmentAnniversaries));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkAnnounceOfficersOnly)
                    .addComponent(chkAnnounceBirthdays)
                    .addComponent(chkAnnounceChildBirthdays)
                    .addComponent(chkAnnounceRecruitmentAnniversaries)));

        return panel;
    }

    /**
     * @return a JPanel representing the Backgrounds tab with specific components like checkboxes and
     * options panel
     */
    JPanel createBackgroundsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("BackgroundsTab",
            getImageDirectory() + "logo_clan_goliath_scorpion.png",
            false, "", true);

        // Contents
        chkUseRandomPersonalities = createCheckBox("UseRandomPersonalities", null);
        chkUseRandomPersonalityReputation = createCheckBox("UseRandomPersonalityReputation", null);
        chkUseIntelligenceXpMultiplier = createCheckBox("UseIntelligenceXpMultiplier", null);
        chkUseSimulatedRelationships = createCheckBox("UseSimulatedRelationships", null);

        pnlRandomOriginOptions = createRandomOriginOptionsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("BackgroundsTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseRandomPersonalities)
                .addComponent(chkUseRandomPersonalityReputation)
                .addComponent(chkUseIntelligenceXpMultiplier)
                .addComponent(chkUseSimulatedRelationships)
                .addComponent(pnlRandomOriginOptions));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                        .addComponent(chkUseRandomPersonalities)
                        .addComponent(chkUseRandomPersonalityReputation)
                        .addComponent(chkUseIntelligenceXpMultiplier)
                        .addComponent(chkUseSimulatedRelationships)
                        .addComponent(pnlRandomOriginOptions)));

        // Create Parent Panel and return
        return createParentPanel(panel, "BackgroundsTab");
    }

    /**
     * Creates a panel containing options for randomizing personnel origins.
     *
     * @return a {@link JPanel} containing the random origin options, configured with the necessary components
     */
    private JPanel createRandomOriginOptionsPanel() {
        // Contents
        chkRandomizeOrigin = createCheckBox("RandomizeOrigin", null);
        chkRandomizeDependentsOrigin = createCheckBox("RandomizeDependentsOrigin", null);
        chkRandomizeAroundSpecifiedPlanet = createCheckBox("RandomizeAroundSpecifiedPlanet",
            null);

        chkSpecifiedSystemFactionSpecific = createCheckBox("SpecifiedSystemFactionSpecific",
            null);
        chkSpecifiedSystemFactionSpecific.addActionListener(evt -> {
            final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
            if ((planetarySystem == null)
                || !planetarySystem.getFactionSet(campaign.getLocalDate()).contains(campaign.getFaction())) {
                restoreComboSpecifiedSystem();
            }
        });


        lblSpecifiedSystem = createLabel("SpecifiedSystem", null);
        comboSpecifiedSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    setText(((PlanetarySystem) value).getName(campaign.getLocalDate()));
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

        lblSpecifiedPlanet = createLabel("SpecifiedPlanet", null);
        comboSpecifiedPlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(campaign.getLocalDate()));
                }
                return this;
            }
        });

        lblOriginSearchRadius = createLabel("OriginSearchRadius", null);
        spnOriginSearchRadius = createSpinner("OriginSearchRadius", null,
            0, 0, 2000, 25);

        lblOriginDistanceScale = createLabel("OriginDistanceScale", null);
        spnOriginDistanceScale = createSpinner("OriginDistanceScale", null,
            0.6, 0.1, 2.0, 0.1);

        chkAllowClanOrigins = createCheckBox("AllowClanOrigins", null);
        chkExtraRandomOrigin = createCheckBox("ExtraRandomOrigin", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomOriginOptionsPanel", true,
            "RandomOriginOptionsPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkRandomizeOrigin)
                .addComponent(chkRandomizeDependentsOrigin)
                .addComponent(chkRandomizeAroundSpecifiedPlanet)
                .addComponent(chkSpecifiedSystemFactionSpecific)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblSpecifiedSystem)
                    .addComponent(comboSpecifiedSystem)
                    .addComponent(lblSpecifiedPlanet)
                    .addComponent(comboSpecifiedPlanet))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblOriginSearchRadius)
                    .addComponent(spnOriginSearchRadius)
                    .addComponent(lblOriginDistanceScale)
                    .addComponent(spnOriginDistanceScale))
                .addComponent(chkAllowClanOrigins)
                .addComponent(chkExtraRandomOrigin));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkRandomizeOrigin)
                    .addComponent(chkRandomizeDependentsOrigin)
                    .addComponent(chkRandomizeAroundSpecifiedPlanet)
                    .addComponent(chkSpecifiedSystemFactionSpecific)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblSpecifiedSystem)
                        .addComponent(comboSpecifiedSystem)
                        .addComponent(lblSpecifiedPlanet)
                        .addComponent(comboSpecifiedPlanet)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblOriginSearchRadius)
                        .addComponent(spnOriginSearchRadius)
                        .addComponent(lblOriginDistanceScale)
                        .addComponent(spnOriginDistanceScale)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkAllowClanOrigins)
                    .addComponent(chkExtraRandomOrigin)));

        return panel;
    }

    /**
     * Restores the list of planets in the combo box based on the selected planetary system.
     * If no planetary system is selected, clear the combo box.
     * If a planetary system is selected, populates the combo box with the planets from that system,
     * and selects the primary planet of the system.
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
     * Removes all items from the combo box, then populates it with planetary systems
     * based on the selected faction filter.
     * Restores the combo box for specified planets after updating the specified system combo box.
     */
    private void restoreComboSpecifiedSystem() {
        comboSpecifiedSystem.removeAllItems();

        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
            chkSpecifiedSystemFactionSpecific.isSelected() ? campaign.getFaction() : null)));

        restoreComboSpecifiedPlanet();
    }

    /**
     * Retrieves an array of {@link PlanetarySystem} objects based on the provided {@link Faction}.
     *
     * @param faction The {@link Faction} to filter the {@link PlanetarySystem} by. Specify null for
     *               no filtering.
     * @return An array of {@link PlanetarySystem} objects that match the filtering criteria, sorted
     * by system name.
     */
    private PlanetarySystem[] getPlanetarySystems(final @Nullable Faction faction) {
        ArrayList<PlanetarySystem> systems = campaign.getSystems();
        ArrayList<PlanetarySystem> filteredSystems = new ArrayList<>();

        // Filter systems
        for (PlanetarySystem planetarySystem : systems) {
            if ((faction == null) || planetarySystem.getFactionSet(campaign.getLocalDate()).contains(faction)) {
                filteredSystems.add(planetarySystem);
            }
        }

        // Sort systems
        filteredSystems.sort(Comparator.comparing(p -> p.getName(campaign.getLocalDate())));

        // Convert to array
        return filteredSystems.toArray(new PlanetarySystem[0]);
    }

    /**
     * Creates and returns a {@link JPanel} for the Death Tab.
     * The panel includes header, various checkboxes, labels, spinners, and a custom combo box.
     *
     * @return {@link JPanel} representing the Death Tab with all its components
     */
    JPanel createDeathTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("DeathTab",
            getImageDirectory() + "logo_filtvelt_coalition.png",
            false, "", true);

        // Contents
        chkKeepMarriedNameUponSpouseDeath = createCheckBox("KeepMarriedNameUponSpouseDeath",
            null);

        lblRandomDeathMethod = createLabel("RandomDeathMethod", null);
        comboRandomDeathMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDeathMethod) {
                    list.setToolTipText(((RandomDeathMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomClanPersonnelDeath = createCheckBox("UseRandomClanPersonnelDeath",
            null);
        chkUseRandomPrisonerDeath = createCheckBox("UseRandomPrisonerDeath", null);
        chkUseRandomDeathSuicideCause = createCheckBox("UseRandomDeathSuicideCause", null);

        pnlDeathAgeGroup = createDeathAgeGroupsPanel();

        lblPercentageRandomDeathChance = createLabel("PercentageRandomDeathChance", null);
        spnPercentageRandomDeathChance = createSpinner("PercentageRandomDeathChance", null,
            0, 0, 100, 0.000001);

        // Layout the Panel
        final JPanel panel = createStandardPanel("DeathTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkKeepMarriedNameUponSpouseDeath)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomDeathMethod)
                    .addComponent(comboRandomDeathMethod))
                .addComponent(chkUseRandomClanPersonnelDeath)
                .addComponent(chkUseRandomPrisonerDeath)
                .addComponent(chkUseRandomDeathSuicideCause)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPercentageRandomDeathChance)
                    .addComponent(spnPercentageRandomDeathChance))
                .addComponent(pnlDeathAgeGroup));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkKeepMarriedNameUponSpouseDeath)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomDeathMethod)
                        .addComponent(comboRandomDeathMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRandomClanPersonnelDeath)
                    .addComponent(chkUseRandomPrisonerDeath)
                    .addComponent(chkUseRandomDeathSuicideCause)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(lblPercentageRandomDeathChance)
                            .addComponent(spnPercentageRandomDeathChance)
                            .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                        .addComponent(pnlDeathAgeGroup)));

        // Create Parent Panel and return
        return createParentPanel(panel, "DeathTab");
    }

    /**
     * Creates a panel for the Death Age Groups tab with checkboxes for different age groups
     *
     * @return a {@link JPanel} representing the Death Age Groups tab with checkboxes for each age group
     */
    JPanel createDeathAgeGroupsPanel() {
        final AgeGroup[] ageGroups = AgeGroup.values();

        // Create the Panel
        final JPanel panel = createStandardPanel("DeathAgeGroupsPanel", true,
            "DeathAgeGroupsPanel");
        panel.setLayout(new GridLayout(1, ageGroups.length));

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
     * Constructs and configures an "Education" {@link JPanel}.
     * The panel includes various controls related to education settings,
     * including checkboxes for enabling specific modules and settings,
     * spinners for configuring values such as XP rate and jump count,
     * and separate panels for managing standard sets, XP and skill bonuses,
     * dropout chances, accidents, and events.
     * <p>
     * The JPanel configuration is done using a {@link GroupLayout}, setting up
     * the components in a structured layout with optimized vertical and
     * horizontal alignments.
     *
     * @return {@link JPanel} The newly created and configured parent JPanel for
     * the Education tab, containing all the educational settings controls.
     */
    JPanel createEducationTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("EducationTab",
            getImageDirectory() + "logo_free_worlds_league.png",
            false, "", true);

        // Contents
        chkUseEducationModule = createCheckBox("UseEducationModule", null);

        lblCurriculumXpRate = createLabel("CurriculumXpRate", null);
        spnCurriculumXpRate = createSpinner("CurriculumXpRate", null,
            3, 1, 10, 1);

        lblMaximumJumpCount = createLabel("MaximumJumpCount", null);
        spnMaximumJumpCount = createSpinner("MaximumJumpCount", null,
            5, 1, 200, 1);

        chkUseReeducationCamps = createCheckBox("UseReeducationCamps", null);

        pnlEnableStandardSets = createEnableStandardSetsPanel();

        chkEnableOverrideRequirements = createCheckBox("EnableOverrideRequirements", null);

        chkShowIneligibleAcademies = createCheckBox("ShowIneligibleAcademies", null);

        lblEntranceExamBaseTargetNumber = createLabel("EntranceExamBaseTargetNumber", null);
        spnEntranceExamBaseTargetNumber = createSpinner("EntranceExamBaseTargetNumber", null,
            14, 0, 20, 1);
        lblEntranceExamBaseTargetNumberPost = createLabel("EntranceExamBaseTargetNumberPost", null);

        pnlXpAndSkillBonuses = createXpAndSkillBonusesPanel();

        pnlDropoutChance = createDropoutChancePanel();

        pnlAccidentsAndEvents = createAccidentsAndEventsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("EducationTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseEducationModule)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCurriculumXpRate)
                    .addComponent(spnCurriculumXpRate)
                    .addComponent(lblMaximumJumpCount)
                    .addComponent(spnMaximumJumpCount))
                .addComponent(chkUseReeducationCamps)
                .addComponent(pnlEnableStandardSets)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(chkShowIneligibleAcademies)
                    .addComponent(chkEnableOverrideRequirements))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblEntranceExamBaseTargetNumber)
                    .addComponent(spnEntranceExamBaseTargetNumber)
                    .addComponent(lblEntranceExamBaseTargetNumberPost))
                .addComponent(pnlXpAndSkillBonuses)
                .addComponent(pnlDropoutChance)
                .addComponent(pnlAccidentsAndEvents));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkUseEducationModule)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCurriculumXpRate)
                        .addComponent(spnCurriculumXpRate)
                        .addComponent(lblMaximumJumpCount)
                        .addComponent(spnMaximumJumpCount)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseReeducationCamps)
                    .addComponent(pnlEnableStandardSets)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(chkShowIneligibleAcademies)
                        .addComponent(chkEnableOverrideRequirements)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblEntranceExamBaseTargetNumber)
                        .addComponent(spnEntranceExamBaseTargetNumber)
                        .addComponent(lblEntranceExamBaseTargetNumberPost)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(pnlXpAndSkillBonuses)
                    .addComponent(pnlDropoutChance)
                    .addComponent(pnlAccidentsAndEvents)));

        // Create Parent Panel and return
        return createParentPanel(panel, "EducationTab");
    }

    /**
     * Constructs and configures an "Enable Standard Sets" {@link JPanel}.
     * The panel consists of three checkboxes: to enable local academies,
     * prestigious academies, and unit education.
     * <p>
     * A {@link GroupLayout} is used to arrange these components in an
     * optimal manner, ensuring good readability and usability in both
     * vertical and horizontal alignments.
     *
     * @return {@link JPanel} The newly constructed and configured JPanel for
     * Enable Standard Sets settings.
     */
    JPanel createEnableStandardSetsPanel() {
        // Contents
        chkEnableLocalAcademies = createCheckBox("EnableLocalAcademies", null);
        chkEnablePrestigiousAcademies = createCheckBox("EnablePrestigiousAcademies", null);
        chkEnableUnitEducation = createCheckBox("EnableUnitEducation", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("EnableStandardSetsPanel", true,
            "EnableStandardSetsPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(chkEnableLocalAcademies)
                    .addComponent(chkEnablePrestigiousAcademies)
                    .addComponent(chkEnableUnitEducation)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(chkEnableLocalAcademies)
                        .addComponent(chkEnablePrestigiousAcademies)
                        .addComponent(chkEnableUnitEducation)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Constructs and configures an "XP and Skill Bonuses" {@link JPanel}.
     * The panel includes a checkbox to enable or disable bonuses, a label
     * for faculty XP multiplier, and a spinner to adjust the faculty XP multiplier value.
     * <p>
     * The layout of the panel is set up with a {@link GroupLayout}, arranging
     * the checkbox, label, and spinner for optimal vertical and horizontal alignment.
     *
     * @return {@link JPanel} The newly created and configured JPanel
     * containing the XP and Skill Bonuses settings.
     */
    JPanel createXpAndSkillBonusesPanel() {
        // Contents
        chkEnableBonuses = createCheckBox("EnableBonuses", null);
        lblFacultyXpMultiplier = createLabel("FacultyXpMultiplier", null);
        spnFacultyXpMultiplier = createSpinner("FacultyXpMultiplier", null,
            1.00, 0.00, 10.00, 0.01);

        // Layout the Panel
        final JPanel panel = createStandardPanel("XpAndSkillBonusesPanel", true,
            "XpAndSkillBonusesPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkEnableBonuses)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFacultyXpMultiplier)
                    .addComponent(spnFacultyXpMultiplier)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkEnableBonuses)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFacultyXpMultiplier)
                        .addComponent(spnFacultyXpMultiplier)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Constructs and configures a "Dropout Chance" {@link JPanel}.
     * The created panel includes two labels and spinners: one set for adult dropout chances,
     * and the other for child dropout chances.
     * <p>
     * The JPanel layout is arranged using a {@link GroupLayout}, positioning the labels and spinners
     * in a way that optimizes horizontal and vertical alignments.
     *
     * @return {@link JPanel} The newly constructed and configured JPanel that contains the Dropout Chance settings.
     */
    JPanel createDropoutChancePanel() {
        // Contents
        lblAdultDropoutChance = createLabel("AdultDropoutChance", null);
        spnAdultDropoutChance = createSpinner("AdultDropoutChance", null,
            1000, 0, 100000, 1);
        lblChildrenDropoutChance = createLabel("ChildrenDropoutChance", null);
        spnChildrenDropoutChance = createSpinner("ChildrenDropoutChance", null,
            10000, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("DropoutChancePanel", true,
            "DropoutChancePanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAdultDropoutChance)
                    .addComponent(spnAdultDropoutChance))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblChildrenDropoutChance)
                    .addComponent(spnChildrenDropoutChance)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblAdultDropoutChance)
                        .addComponent(spnAdultDropoutChance)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblChildrenDropoutChance)
                        .addComponent(spnChildrenDropoutChance)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Constructs and configures an "Accidents and Events" {@link JPanel}.
     * The panel includes a checkbox indicating whether to include all ages,
     * a label for military academy accidents, and also a spinner for adjusting
     * the count of military academy accidents.
     * <p>
     * The layout of the panel is configured with a {@link GroupLayout},
     * organizing the checkbox and other components for optimized alignments
     * in vertical and horizontal layouts.
     *
     * @return {@link JPanel} The constructed, configured JPanel containing
     * "Accidents and Events" settings
     */
    JPanel createAccidentsAndEventsPanel() {
        // Contents
        chkAllAges = createCheckBox("AllAges", null);
        lblMilitaryAcademyAccidents = createLabel("MilitaryAcademyAccidents", null);
        spnMilitaryAcademyAccidents = createSpinner("MilitaryAcademyAccidents", null,
            10000, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("AccidentsAndEventsPanel", true,
            "AccidentsAndEventsPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkAllAges)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMilitaryAcademyAccidents)
                    .addComponent(spnMilitaryAcademyAccidents)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkAllAges)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMilitaryAcademyAccidents)
                        .addComponent(spnMilitaryAcademyAccidents)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }
}