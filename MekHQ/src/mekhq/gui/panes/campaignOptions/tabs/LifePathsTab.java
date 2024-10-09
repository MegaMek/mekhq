package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
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
    }

    JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("LifePathsGeneralTab",
            getImageDirectory() + "logo_escorpion_imperio.png",
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

    private JPanel createRandomOriginOptionsPanel() {
        // Contents
        chkRandomizeOrigin = createCheckBox("RandomizeOrigin", null);
        chkRandomizeDependentsOrigin = createCheckBox("RandomizeDependentsOrigin", null);
        chkRandomizeAroundSpecifiedPlanet = createCheckBox("RandomizeAroundSpecifiedPlanet", null);

        chkSpecifiedSystemFactionSpecific = createCheckBox("SpecifiedSystemFactionSpecific", null);
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

    private void restoreComboSpecifiedSystem() {
        comboSpecifiedSystem.removeAllItems();
        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
            chkSpecifiedSystemFactionSpecific.isSelected() ? campaign.getFaction() : null)));
        restoreComboSpecifiedPlanet();
    }

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
}
