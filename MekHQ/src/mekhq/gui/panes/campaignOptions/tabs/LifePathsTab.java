package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class LifePathsTab {
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

    //start Marriage Tab
    private JPanel pnlMarriageGeneralOptions;
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClanPersonnelMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JLabel lblNoInterestInMarriageDiceSize;
    private JSpinner spnNoInterestInMarriageDiceSize;
    private JLabel lblCheckMutualAncestorsDepth;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChanges;

    private JPanel pnlRandomMarriage;
    private JLabel lblRandomMarriageMethod;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JLabel lblRandomMarriageAgeRange;
    private JSpinner spnRandomMarriageAgeRange;

    private JPanel pnlPercentageRandomMarriage;
    private JLabel lblRandomMarriageOppositeSexDiceSize;
    private JSpinner spnRandomMarriageDiceSize;
    private JLabel lblRandomSameSexMarriageDiceSize;
    private JSpinner spnRandomSameSexMarriageDiceSize;
    private JLabel lblRandomNewDependentMarriage;
    private JSpinner spnRandomNewDependentMarriage;
    //end Marriage Tab

    //start Divorce Tab
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClanPersonnelDivorce;
    private JCheckBox chkUsePrisonerDivorce;

    private JPanel pnlRandomDivorce;

    private JLabel lblRandomDivorceMethod;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClanPersonnelDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;
    //end Divorce Tab

    /**
     * Represents a tab for repair and maintenance in an application.
     */
    LifePathsTab(Campaign campaign, JFrame frame, String name) {
        this.campaign = campaign;
        this.frame = frame;
        this.name = name;

        initialize();
    }

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

        // Marriage Tab
        pnlMarriageGeneralOptions = new JPanel();
        chkUseManualMarriages = new JCheckBox();
        chkUseClanPersonnelMarriages = new JCheckBox();
        chkUsePrisonerMarriages = new JCheckBox();
        lblNoInterestInMarriageDiceSize = new JLabel();
        spnNoInterestInMarriageDiceSize = new JSpinner();
        lblCheckMutualAncestorsDepth = new JLabel();
        spnCheckMutualAncestorsDepth = new JSpinner();
        chkLogMarriageNameChanges = new JCheckBox();

        pnlRandomMarriage = new JPanel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());

        pnlRandomMarriage = new JPanel();
        lblRandomMarriageMethod = new JLabel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());
        chkUseRandomClanPersonnelMarriages = new JCheckBox();
        chkUseRandomPrisonerMarriages = new JCheckBox();
        lblRandomMarriageAgeRange = new JLabel();
        spnRandomMarriageAgeRange = new JSpinner();

        pnlPercentageRandomMarriage = new JPanel();
        lblRandomMarriageOppositeSexDiceSize = new JLabel();
        spnRandomMarriageDiceSize = new JSpinner();
        lblRandomSameSexMarriageDiceSize = new JLabel();
        spnRandomSameSexMarriageDiceSize = new JSpinner();
        lblRandomNewDependentMarriage = new JLabel();
        spnRandomNewDependentMarriage = new JSpinner();

        // Divorce Tab
        chkUseManualDivorce = new JCheckBox();
        chkUseClanPersonnelDivorce = new JCheckBox();
        chkUsePrisonerDivorce = new JCheckBox();

        pnlRandomDivorce = new JPanel();
        lblRandomDivorceMethod = new JLabel();
        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        chkUseRandomOppositeSexDivorce = new JCheckBox();
        chkUseRandomSameSexDivorce = new JCheckBox();
        chkUseRandomClanPersonnelDivorce = new JCheckBox();
        chkUseRandomPrisonerDivorce = new JCheckBox();
        lblRandomDivorceDiceSize = new JLabel();
        spnRandomDivorceDiceSize = new JSpinner();
    }

    /**
     * Creates a general tab for the Life Paths category containing various components
     *
     * @return a {@link JPanel} representing the general tab with multiple input components and panels
     */
    JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("LifePathsGeneralTab",
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
        final JPanel panel = createStandardPanel("LifePathsGeneralTab", true, "");
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
        return createParentPanel(panel, "LifePathsGeneralTab");
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
     * Creates a panel for the Marriage tab with various input components and panels related to marriage settings.
     *
     * @return a {@link} representing the Marriage tab with checkboxes for manual, clan personnel,
     * prisoner marriages, options for marriage characteristics, logging marriage name changes, surname
     * weight settings, and random marriage generation.
     */
    JPanel createMarriageTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("MarriageTab",
            getImageDirectory() + "logo_federated_commonwealth.png",
            false, "", true);

        // Contents
        pnlMarriageGeneralOptions = createMarriageGeneralOptionsPanel();
        pnlRandomMarriage = createRandomMarriagePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("MarriageTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlMarriageGeneralOptions)
                    .addComponent(pnlRandomMarriage)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlMarriageGeneralOptions)
                        .addComponent(pnlRandomMarriage))));

        // Create Parent Panel and return
        return createParentPanel(panel, "MarriageTab");
    }

    /**
     * Creates a panel for general marriage options with checkboxes and input components.
     *
     * @return a {@link JPanel} representing the general marriage options panel
     */
    JPanel createMarriageGeneralOptionsPanel() {
        // Contents
        chkUseManualMarriages = createCheckBox("UseManualMarriages", null);
        chkUseClanPersonnelMarriages = createCheckBox("UseClanPersonnelMarriages", null);
        chkUsePrisonerMarriages = createCheckBox("UsePrisonerMarriages", null);

        lblNoInterestInMarriageDiceSize = createLabel("NoInterestInMarriageDiceSize", null);
        spnNoInterestInMarriageDiceSize = createSpinner("NoInterestInMarriageDiceSize", null,
            10, 1, 100000, 1);

        lblCheckMutualAncestorsDepth = createLabel("CheckMutualAncestorsDepth", null);
        spnCheckMutualAncestorsDepth = createSpinner("CheckMutualAncestorsDepth", null,
            4, 0, 20, 1);

        chkLogMarriageNameChanges = createCheckBox("LogMarriageNameChanges", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("MarriageGeneralOptionsPanel",
            false, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseManualMarriages)
                .addComponent(chkUseClanPersonnelMarriages)
                .addComponent(chkUsePrisonerMarriages)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNoInterestInMarriageDiceSize)
                    .addComponent(spnNoInterestInMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCheckMutualAncestorsDepth)
                    .addComponent(spnCheckMutualAncestorsDepth))
                .addComponent(chkLogMarriageNameChanges));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseManualMarriages)
                    .addComponent(chkUseClanPersonnelMarriages)
                    .addComponent(chkUsePrisonerMarriages)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblNoInterestInMarriageDiceSize)
                        .addComponent(spnNoInterestInMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCheckMutualAncestorsDepth)
                        .addComponent(spnCheckMutualAncestorsDepth)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkLogMarriageNameChanges)));

        return panel;
    }

    /**
     * Creates a panel for random marriage settings, including options for different marriage methods,
     * using random clan personnel and prisoner marriages, setting age range for marriages, and
     * percentage settings.
     *
     * @return a {@link JPanel} representing the random marriage panel with various input components
     * and panels for configuring random marriage settings
     */
    JPanel createRandomMarriagePanel() {
        // Contents
        lblRandomMarriageMethod = createLabel("RandomMarriageMethod", null);
        comboRandomMarriageMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomMarriageMethod) {
                    list.setToolTipText(((RandomMarriageMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomClanPersonnelMarriages = createCheckBox("UseRandomClanPersonnelMarriages", null);
        chkUseRandomPrisonerMarriages = createCheckBox("UseRandomPrisonerMarriages", null);

        lblRandomMarriageAgeRange = createLabel("RandomMarriageAgeRange", null);
        spnRandomMarriageAgeRange = createSpinner("RandomMarriageAgeRange", null,
            10, 0, 100, 1.0);

        pnlPercentageRandomMarriage = createPercentageRandomMarriagePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomMarriages", true,
            "RandomMarriages");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageMethod)
                    .addComponent(comboRandomMarriageMethod))
                .addComponent(chkUseRandomClanPersonnelMarriages)
                .addComponent(chkUseRandomPrisonerMarriages)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageAgeRange)
                    .addComponent(spnRandomMarriageAgeRange))
                .addComponent(pnlPercentageRandomMarriage));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageMethod)
                        .addComponent(comboRandomMarriageMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRandomClanPersonnelMarriages)
                    .addComponent(chkUseRandomPrisonerMarriages)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageAgeRange)
                        .addComponent(spnRandomMarriageAgeRange)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(pnlPercentageRandomMarriage)));

        return panel;
    }

    /**
     * Creates a panel for setting percentage-based random marriage settings.
     *
     * @return a {@link JPanel} representing the Percentage Random Marriage panel with input components
     * for setting opposite-sex marriage dice size, same-sex marriage dice size, and new dependent
     * marriage dice size
     */
    JPanel createPercentageRandomMarriagePanel() {
        // Contents
        lblRandomMarriageOppositeSexDiceSize = createLabel("RandomMarriageOppositeSexDiceSize",
            null);
        spnRandomMarriageDiceSize = createSpinner("RandomMarriageOppositeSexDiceSize",
            null, 5000, 0, 100000, 1);

        lblRandomSameSexMarriageDiceSize = createLabel("RandomSameSexMarriageDiceSize", null);
        spnRandomSameSexMarriageDiceSize = createSpinner("RandomSameSexMarriageDiceSize",
            null, 14, 0, 100000, 1);

        lblRandomNewDependentMarriage = createLabel("RandomNewDependentMarriage", null);
        spnRandomNewDependentMarriage = createSpinner("RandomSameSexMarriageDiceSize",
            null, 20, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PercentageRandomMarriagePanel", true,
            "PercentageRandomMarriagePanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageOppositeSexDiceSize)
                    .addComponent(spnRandomMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomSameSexMarriageDiceSize)
                    .addComponent(spnRandomSameSexMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomNewDependentMarriage)
                    .addComponent(spnRandomNewDependentMarriage)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageOppositeSexDiceSize)
                        .addComponent(spnRandomMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomSameSexMarriageDiceSize)
                        .addComponent(spnRandomSameSexMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomNewDependentMarriage)
                        .addComponent(spnRandomNewDependentMarriage)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Creates a tab for divorce settings with various checkboxes and panels for manual, clan personnel,
     * and prisoner divorces.
     *
     * @return a {@link JPanel} representing the Divorce tab with checkboxes for manual divorce,
     * clan personnel divorce, prisoner divorce, and a panel for configuring random divorce settings.
     */
    JPanel createDivorceTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("DivorceTab",
            getImageDirectory() + "logo_clan_hells_horses.png",
            false, "", true);

        // Contents
        chkUseManualDivorce = createCheckBox("UseManualDivorce", null);
        chkUseClanPersonnelDivorce = createCheckBox("UseClanPersonnelDivorce", null);
        chkUsePrisonerDivorce = createCheckBox("UsePrisonerDivorce", null);

        pnlRandomDivorce = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("DivorceTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseManualDivorce)
                .addComponent(chkUseClanPersonnelDivorce)
                .addComponent(chkUsePrisonerDivorce)
                .addComponent(pnlRandomDivorce));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClanPersonnelDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(pnlRandomDivorce)));

        // Create Parent Panel and return
        return createParentPanel(panel, "DivorceTab");
    }

    /**
     * Creates a panel for the Divorce tab with checkboxes for manual divorce, clan personnel divorce,
     * prisoner divorce, and a panel for configuring random divorce settings.
     *
     * @return a {@link JPanel} representing the Divorce tab with various components for configuring divorce settings
     */
    JPanel createRandomDivorcePanel() {
        // Contents
        lblRandomDivorceMethod = createLabel("RandomDivorceMethod", null);
        comboRandomDivorceMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDivorceMethod) {
                    list.setToolTipText(((RandomDivorceMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomOppositeSexDivorce = createCheckBox("UseRandomOppositeSexDivorce", null);
        chkUseRandomSameSexDivorce = createCheckBox("UseRandomSameSexDivorce", null);
        chkUseRandomClanPersonnelDivorce = createCheckBox("UseRandomClanPersonnelDivorce", null);
        chkUseRandomPrisonerDivorce = createCheckBox("UseRandomPrisonerDivorce", null);

        lblRandomDivorceDiceSize = createLabel("RandomDivorceDiceSize", null);
        spnRandomDivorceDiceSize = createSpinner("RandomDivorceDiceSize", null,
            900, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomDivorcePanel", true, "RandomDivorcePanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomDivorceMethod)
                    .addComponent(comboRandomDivorceMethod))
                .addComponent(chkUseRandomOppositeSexDivorce)
                .addComponent(chkUseRandomSameSexDivorce)
                .addComponent(chkUseRandomClanPersonnelDivorce)
                .addComponent(chkUseRandomPrisonerDivorce)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomDivorceDiceSize)
                    .addComponent(spnRandomDivorceDiceSize)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomDivorceMethod)
                        .addComponent(comboRandomDivorceMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRandomOppositeSexDivorce)
                    .addComponent(chkUseRandomSameSexDivorce)
                    .addComponent(chkUseRandomClanPersonnelDivorce)
                    .addComponent(chkUseRandomPrisonerDivorce)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomDivorceDiceSize)
                        .addComponent(spnRandomDivorceDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }
}
