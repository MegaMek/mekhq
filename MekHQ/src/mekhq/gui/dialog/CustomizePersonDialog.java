/*
 * Copyright (C) 2013-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.enums.Gender;
import megamek.client.ui.swing.DialogOptionComponent;
import megamek.client.ui.swing.DialogOptionListener;
import megamek.common.Crew;
import megamek.common.EquipmentType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.control.EditKillLogControl;
import mekhq.gui.control.EditMissionLogControl;
import mekhq.gui.control.EditPersonnelLogControl;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.gui.utilities.MarkdownEditorPanel;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * This dialog is used to both hire new pilots and to edit existing ones
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CustomizePersonDialog extends JDialog implements DialogOptionListener {
    //region Variable declarations
    private static final long serialVersionUID = -6265589976779860566L;

    private Person person;
    private List<DialogOptionComponent> optionComps = new ArrayList<>();
    private Map<String, JSpinner> skillLvls = new Hashtable<>();
    private Map<String, JSpinner> skillBonus = new Hashtable<>();
    private Map<String, JLabel> skillValues = new Hashtable<>();
    private Map<String, JCheckBox> skillChks = new Hashtable<>();
    private PilotOptions options;
    private LocalDate birthdate;
    private LocalDate recruitment;
    private LocalDate lastRankChangeDate;
    private LocalDate retirement;
    private JFrame frame;

    private JButton btnDate;
    private JButton btnServiceDate;
    private JButton btnRankDate;
    private JButton btnRetirementDate;
    private JComboBox<Gender> choiceGender;
    private JLabel lblAge;
    private JPanel panSkills;
    private JPanel panOptions;
    private JTextField textToughness;
    private JTextField textPreNominal;
    private JTextField textGivenName;
    private JTextField textSurname;
    private JTextField textPostNominal;
    private JTextField textNickname;
    private JTextField textBloodname;
    private MarkdownEditorPanel txtBio;
    private JComboBox<Faction> choiceFaction;
    private JComboBox<PlanetarySystem> choiceSystem;
    private DefaultComboBoxModel<PlanetarySystem> allSystems;
    private JCheckBox chkOnlyOurFaction;
    private JComboBox<Planet> choicePlanet;
    private JCheckBox chkClan;
    private JComboBox<Phenotype> choicePhenotype;
    private Phenotype selectedPhenotype;

    /* Against the Bot */
    private JComboBox<String> choiceUnitWeight;
    private JComboBox<String> choiceUnitTech;
    private JCheckBox chkFounder;
    private JComboBox<Unit> choiceOriginalUnit;

    private Campaign campaign;

    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog",
            new EncodeControl());
    //endregion Variable declarations

    /** Creates new form CustomizePilotDialog */
    public CustomizePersonDialog(JFrame parent, boolean modal, Person person, Campaign campaign) {
        super(parent, modal);
        this.campaign = campaign;
        this.frame = parent;
        this.person = person;
        initializePilotAndOptions();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initializePilotAndOptions () {
        birthdate = person.getBirthday();
        if (person.getRecruitment() != null) {
            recruitment = person.getRecruitment();
        }

        if (person.getLastRankChangeDate() != null) {
            lastRankChangeDate = person.getLastRankChangeDate();
        }

        if (person.getRetirement() != null) {
            retirement = person.getRetirement();
        }

        selectedPhenotype = person.getPhenotype();
        options = person.getOptions();
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        JPanel panDemog = new JPanel(new GridBagLayout());
        JTabbedPane tabStats = new JTabbedPane();
        JLabel lblName = new JLabel();
        JLabel lblGender = new JLabel();
        JLabel lblBday = new JLabel();
        JLabel lblRecruitment = new JLabel();
        lblAge = new javax.swing.JLabel();
        JLabel lblNickname = new JLabel();
        JLabel lblBloodname = new JLabel();
        JPanel panName = new javax.swing.JPanel(new java.awt.GridBagLayout());
        textNickname = new javax.swing.JTextField();
        textBloodname = new javax.swing.JTextField();
        textToughness = new javax.swing.JTextField();
        JLabel lblToughness = new JLabel();
        JScrollPane scrOptions = new JScrollPane();
        panOptions = new javax.swing.JPanel();
        JScrollPane scrSkills = new JScrollPane();
        panSkills = new javax.swing.JPanel();
        JPanel panButtons = new JPanel();
        JButton btnOk = new JButton();

        JButton btnClose = new JButton();
        JButton btnRandomName = new JButton();
        JButton btnRandomBloodname = new JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title"));

        setName("Form");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        int y = 1;

        lblName.setText(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblName, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        textPreNominal = new JTextField(person.getPreNominal());
        textPreNominal.setName("textPreNominal");
        textPreNominal.setMinimumSize(new Dimension(50, 28));
        textPreNominal.setPreferredSize(new Dimension(50, 28));
        panName.add(textPreNominal, gridBagConstraints);

        textGivenName = new JTextField(person.getGivenName());
        textGivenName.setName("textGivenName");
        textGivenName.setMinimumSize(new Dimension(100, 28));
        textGivenName.setPreferredSize(new Dimension(100, 28));
        gridBagConstraints.gridx = 2;
        panName.add(textGivenName, gridBagConstraints);

        textSurname = new JTextField(person.getSurname());
        textSurname.setName("textSurname");
        textSurname.setMinimumSize(new Dimension(100, 28));
        textSurname.setPreferredSize(new Dimension(100, 28));
        gridBagConstraints.gridx = 3;
        panName.add(textSurname, gridBagConstraints);

        textPostNominal = new JTextField(person.getPostNominal());
        textPostNominal.setName("textPostNominal");
        textPostNominal.setMinimumSize(new Dimension(50, 28));
        textPostNominal.setPreferredSize(new Dimension(50, 28));
        gridBagConstraints.gridx = 4;
        panName.add(textPostNominal, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panDemog.add(panName, gridBagConstraints);

        btnRandomName.setText(resourceMap.getString("btnRandomName.text")); // NOI18N
        btnRandomName.setName("btnRandomName"); // NOI18N
        btnRandomName.addActionListener(evt -> randomName());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panDemog.add(btnRandomName, gridBagConstraints);

        y++;

        if (person.isClanner()) {
            lblBloodname.setText(resourceMap.getString("lblBloodname.text")); // NOI18N
            lblBloodname.setName("lblBloodname"); // NOI18N
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblBloodname, gridBagConstraints);

            textBloodname.setMinimumSize(new Dimension(150, 28));
            textBloodname.setName("textBloodname");
            textBloodname.setPreferredSize(new Dimension(150, 28));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            textBloodname.setText(person.getBloodname());
            panDemog.add(textBloodname, gridBagConstraints);

            btnRandomBloodname.setText(resourceMap.getString("btnRandomBloodname.text"));
            btnRandomBloodname.setName("btnRandomBloodname");
            btnRandomBloodname.addActionListener(evt -> randomBloodname());
            gridBagConstraints.gridx = 2;
            panDemog.add(btnRandomBloodname, gridBagConstraints);
        } else {
            lblNickname.setText(resourceMap.getString("lblNickname.text"));
            lblNickname.setName("lblNickname");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblNickname, gridBagConstraints);

            textNickname.setText(person.getCallsign());
            textNickname.setName("textNickname");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            panDemog.add(textNickname, gridBagConstraints);

            JButton btnRandomCallsign = new JButton(resourceMap.getString("btnRandomCallsign.text"));
            btnRandomCallsign.setName("btnRandomCallsign");
            btnRandomCallsign.addActionListener(e -> textNickname.setText(RandomCallsignGenerator.getInstance().generate()));
            gridBagConstraints.gridx = 2;
            panDemog.add(btnRandomCallsign, gridBagConstraints);
        }

        y++;

        lblGender.setText(resourceMap.getString("lblGender.text")); // NOI18N
        lblGender.setName("lblGender"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblGender, gridBagConstraints);

        DefaultComboBoxModel<Gender> genderModel = new DefaultComboBoxModel<>();
        for (Gender gender : Gender.getExternalOptions()) {
            genderModel.addElement(gender);
        }
        choiceGender = new JComboBox<>(genderModel);
        choiceGender.setName("choiceGender");
        choiceGender.setSelectedItem(person.getGender().isExternal() ? person.getGender()
                : person.getGender().getExternalVariant());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choiceGender, gridBagConstraints);

        y++;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin Faction:"), gridBagConstraints);

        DefaultComboBoxModel<Faction> factionsModel = getFactionsComboBoxModel();
        choiceFaction = new JComboBox<>(factionsModel);
        choiceFaction.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list,
                                                          final Object value,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Faction) {
                    Faction faction = (Faction)value;
                    setText(String.format("%s [%s]",
                            faction.getFullName(campaign.getGameYear()),
                            faction.getShortName()));
                }

                return this;
            }
        });
        choiceFaction.setSelectedIndex(factionsModel.getIndexOf(person.getOriginFaction()));
        choiceFaction.addActionListener(evt -> {
            // Update the clan check box based on the new selected faction
            Faction selectedFaction = (Faction) choiceFaction.getSelectedItem();
            if (selectedFaction != null) {
                chkClan.setSelected(selectedFaction.isClan());
            }

            // We don't have to call backgroundChanged because it is already
            // called when we update the chkClan checkbox.

            if (chkOnlyOurFaction.isSelected()) {
                filterPlanetarySystemsForOurFaction(true);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choiceFaction, gridBagConstraints);

        y++;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin System:"), gridBagConstraints);

        DefaultComboBoxModel<Planet> planetsModel = new DefaultComboBoxModel<>();
        choicePlanet = new JComboBox<>(planetsModel);

        allSystems = getPlanetarySystemsComboBoxModel();
        choiceSystem = new JComboBox<>(allSystems);
        choiceSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list,
                                                          final Object value,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    PlanetarySystem system = (PlanetarySystem) value;
                    setText(system.getName(campaign.getLocalDate()));
                }

                return this;
            }
        });
        if (person.getOriginPlanet() != null) {
            PlanetarySystem planetarySystem = person.getOriginPlanet().getParentSystem();
            choiceSystem.setSelectedIndex(allSystems.getIndexOf(planetarySystem));
            updatePlanetsComboBoxModel(planetsModel, planetarySystem);
        }
        choiceSystem.addActionListener(evt -> {
            // Update the clan check box based on the new selected faction
            PlanetarySystem selectedSystem = (PlanetarySystem)choiceSystem.getSelectedItem();

            choicePlanet.setSelectedIndex(-1);
            updatePlanetsComboBoxModel(planetsModel, selectedSystem);
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choiceSystem, gridBagConstraints);

        chkOnlyOurFaction = new JCheckBox("Faction Specific");
        chkOnlyOurFaction.addActionListener(e -> filterPlanetarySystemsForOurFaction(chkOnlyOurFaction.isSelected()));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(chkOnlyOurFaction, gridBagConstraints);

        y++;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin Planet:"), gridBagConstraints);

        choicePlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list,
                                                          final Object value,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof Planet) {
                    Planet planet = (Planet) value;
                    setText(planet.getName(campaign.getLocalDate()));
                }

                return this;
            }
        });
        if (person.getOriginPlanet() != null) {
            choicePlanet.setSelectedIndex(planetsModel.getIndexOf(person.getOriginPlanet()));
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemog.add(choicePlanet, gridBagConstraints);

        y++;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Phenotype:"), gridBagConstraints);

        DefaultComboBoxModel<Phenotype> phenotypeModel = new DefaultComboBoxModel<>();
        phenotypeModel.addElement(Phenotype.NONE);
        for (Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            phenotypeModel.addElement(phenotype);
        }
        choicePhenotype = new JComboBox<>(phenotypeModel);
        choicePhenotype.setSelectedItem(selectedPhenotype);
        choicePhenotype.addActionListener(evt -> backgroundChanged());
        choicePhenotype.setEnabled(person.isClanner());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemog.add(choicePhenotype, gridBagConstraints);

        chkClan = new JCheckBox("Clanner");
        chkClan.setSelected(person.isClanner());
        chkClan.addItemListener(et -> backgroundChanged());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(chkClan, gridBagConstraints);

        y++;

        lblBday.setText(resourceMap.getString("lblBday.text")); // NOI18N
        lblBday.setName("lblBday"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblBday, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(birthdate));
        btnDate.setName("btnDate");
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panDemog.add(btnDate, gridBagConstraints);

        lblAge.setText(person.getAge(campaign.getLocalDate()) + " " + resourceMap.getString("age")); // NOI18N
        lblAge.setName("lblAge"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblAge, gridBagConstraints);

        y++;

        if (campaign.getCampaignOptions().getUseTimeInService() && (recruitment != null)) {
            lblRecruitment.setText(resourceMap.getString("lblRecruitment.text"));
            lblRecruitment.setName("lblRecruitment");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblRecruitment, gridBagConstraints);

            btnServiceDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(recruitment));
            btnServiceDate.setName("btnServiceDate");
            btnServiceDate.addActionListener(this::btnServiceDateActionPerformed);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panDemog.add(btnServiceDate, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().getUseTimeInRank() && (lastRankChangeDate != null)) {
            JLabel lblLastRankChangeDate = new JLabel(resourceMap.getString("lblLastRankChangeDate.text"));
            lblLastRankChangeDate.setName("lblLastRankChangeDate");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblLastRankChangeDate, gridBagConstraints);

            btnRankDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(lastRankChangeDate));
            btnRankDate.setName("btnRankDate");
            btnRankDate.addActionListener(e -> btnRankDateActionPerformed());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(btnRankDate, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().useRetirementDateTracking() && (retirement != null)) {
            JLabel lblRetirement = new JLabel(resourceMap.getString("lblRetirement.text"));
            lblRetirement.setName("lblRetirement");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblRetirement, gridBagConstraints);

            btnRetirementDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(retirement));
            btnRetirementDate.setName("btnRetirementDate");
            btnRetirementDate.addActionListener(e -> btnRetirementDateActionPerformed());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(btnRetirementDate, gridBagConstraints);

            y++;
        }

        lblToughness.setText(resourceMap.getString("lblToughness.text")); // NOI18N
        lblToughness.setName("lblToughness"); // NOI18N

        textToughness.setText(Integer.toString(person.getToughness()));
        textToughness.setName("textToughness"); // NOI18N

        if (campaign.getCampaignOptions().useToughness()) {
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblToughness, gridBagConstraints);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            panDemog.add(textToughness, gridBagConstraints);
        }

        JLabel lblUnit = new JLabel();
        lblUnit.setText("Original unit:"); // NOI18N
        lblUnit.setName("lblUnit"); // NOI18N

        choiceUnitWeight = new JComboBox<>();
        choiceUnitWeight.addItem("None");
        choiceUnitWeight.addItem("Light");
        choiceUnitWeight.addItem("Medium");
        choiceUnitWeight.addItem("Heavy");
        choiceUnitWeight.addItem("Assault");
        choiceUnitWeight.setSelectedIndex(person.getOriginalUnitWeight());

        choiceUnitTech = new JComboBox<>();
        choiceUnitTech.addItem("IS1");
        choiceUnitTech.addItem("IS2");
        choiceUnitTech.addItem("Clan");
        choiceUnitTech.setSelectedIndex(person.getOriginalUnitTech());

        JLabel lblShares = new JLabel();
        lblShares.setText(person.getNumShares(campaign.getCampaignOptions().getSharesForAll()) + " shares");

        chkFounder = new JCheckBox("Founding member");
        chkFounder.setSelected(person.isFounder());

        choiceOriginalUnit = new JComboBox<>();
        choiceOriginalUnit.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                if (null == value) {
                    setText("None");
                } else {
                    setText(((Unit) value).getName());
                }
                return this;
            }
        });
        choiceOriginalUnit.addItem(null);
        campaign.getHangar().forEachUnit(choiceOriginalUnit::addItem);
        if (null == person.getOriginalUnitId() || null == campaign.getUnit(person.getOriginalUnitId())) {
            choiceOriginalUnit.setSelectedItem(null);
        } else {
            choiceOriginalUnit.setSelectedItem(campaign.getUnit(person.getOriginalUnitId()));
        }
        choiceOriginalUnit.addActionListener(ev -> {
            if (null == choiceOriginalUnit.getSelectedItem()) {
                choiceUnitWeight.setSelectedIndex(0);
                choiceUnitTech.setSelectedIndex(0);
            } else {
                Unit unit = (Unit)choiceOriginalUnit.getSelectedItem();
                choiceUnitWeight.setSelectedIndex(unit.getEntity().getWeightClass());
                if (unit.getEntity().isClan()) {
                    choiceUnitTech.setSelectedIndex(2);
                } else if (unit.getEntity().getTechLevel() > megamek.common.TechConstants.T_INTRO_BOXSET) {
                    choiceUnitTech.setSelectedIndex(1);
                } else {
                    choiceUnitTech.setSelectedIndex(0);
                }
            }
        });

        y++;

        if (campaign.getCampaignOptions().getUseAtB()) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblUnit, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panDemog.add(choiceUnitWeight, gridBagConstraints);

            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panDemog.add(choiceUnitTech, gridBagConstraints);

            y++;

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(choiceOriginalUnit, gridBagConstraints);

            y++;

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(chkFounder, gridBagConstraints);

            if (campaign.getCampaignOptions().getUseShareSystem()) {
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = y;
                gridBagConstraints.gridwidth = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                panDemog.add(lblShares, gridBagConstraints);
            }
        }

        y++;

        txtBio = new MarkdownEditorPanel("Biography");
        txtBio.setText(person.getBiography());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panDemog.add(txtBio, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(panDemog, gridBagConstraints);

        panSkills.setName("panSkills"); // NOI18N
        refreshSkills();
        scrSkills.setViewportView(panSkills);
        scrSkills.setMinimumSize(new java.awt.Dimension(500, 500));
        scrSkills.setPreferredSize(new java.awt.Dimension(500, 500));

        panOptions.setName("panOptions"); // NOI18N
        refreshOptions();
        scrOptions.setViewportView(panOptions);
        scrOptions.setMinimumSize(new java.awt.Dimension(500, 500));
        scrOptions.setPreferredSize(new java.awt.Dimension(500, 500));

        tabStats.addTab(resourceMap.getString("scrSkills.TabConstraints.tabTitle"), scrSkills); // NOI18N
        if (campaign.getCampaignOptions().useAbilities() || campaign.getCampaignOptions().useEdge()
                || campaign.getCampaignOptions().useImplants()) {
            tabStats.addTab(resourceMap.getString("scrOptions.TabConstraints.tabTitle"), scrOptions); // NOI18N
        }
        tabStats.add(resourceMap.getString("panLog.TabConstraints.tabTitle"), new EditPersonnelLogControl(frame, campaign, person));
        tabStats.add(resourceMap.getString("panMissions.TabConstraints.tabTitle"), new EditMissionLogControl(frame, campaign, person));
        tabStats.add(resourceMap.getString("panKills.TabConstraints.tabTitle"), new EditKillLogControl(frame, campaign, person));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabStats, gridBagConstraints);

        panButtons.setName("panButtons"); // NOI18N
        panButtons.setLayout(new java.awt.GridBagLayout());

        btnOk.setText(resourceMap.getString("btnOk.text")); // NOI18N
        btnOk.setName("btnOk"); // NOI18N
        btnOk.addActionListener(this::btnOkActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;

        panButtons.add(btnOk, gridBagConstraints);
        gridBagConstraints.gridx++;

        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CustomizePersonDialog.class);
        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private DefaultComboBoxModel<Faction> getFactionsComboBoxModel() {
        int year = campaign.getGameYear();
        List<Faction> orderedFactions = Factions.getInstance().getFactions().stream()
                .sorted((a, b) -> a.getFullName(year).compareToIgnoreCase(b.getFullName(year)))
                .collect(Collectors.toList());

        DefaultComboBoxModel<Faction> factionsModel = new DefaultComboBoxModel<>();
        for (Faction faction : orderedFactions) {
            // Always include the person's faction
            if (faction.equals(person.getOriginFaction())) {
                factionsModel.addElement(faction);
            } else {
                if (faction.is(Tag.HIDDEN) || faction.is(Tag.SPECIAL)) {
                    continue;
                }

                // Allow factions between the person's birthday
                // and when they were recruited, or now if we're
                // not tracking recruitment.
                int endYear = person.getRecruitment() != null
                        ? Math.min(person.getRecruitment().getYear(), year)
                        : year;
                if (faction.validBetween(person.getBirthday().getYear(), endYear)) {
                    factionsModel.addElement(faction);
                }
            }
        }

        return factionsModel;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel() {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        List<PlanetarySystem> orderedSystems = campaign.getSystems().stream()
                .sorted(Comparator.comparing(a -> a.getName(campaign.getLocalDate())))
                .collect(Collectors.toList());
        for (PlanetarySystem system : orderedSystems) {
            model.addElement(system);
        }
        return model;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel(Faction faction) {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        List<PlanetarySystem> orderedSystems = campaign.getSystems().stream()
                .filter(a -> a.getFactionSet(person.getBirthday()).contains(faction))
                .sorted(Comparator.comparing(a -> a.getName(person.getBirthday())))
                .collect(Collectors.toList());
        for (PlanetarySystem system : orderedSystems) {
            model.addElement(system);
        }

        return model;
    }

    private void filterPlanetarySystemsForOurFaction(boolean onlyOurFaction) {
        PlanetarySystem selectedSystem = (PlanetarySystem)choiceSystem.getSelectedItem();
        Planet selectedPlanet = (Planet)choicePlanet.getSelectedItem();
        if (onlyOurFaction && choiceFaction.getSelectedItem() != null) {
            Faction faction = (Faction)choiceFaction.getSelectedItem();

            DefaultComboBoxModel<PlanetarySystem> model = getPlanetarySystemsComboBoxModel(faction);
            if (model.getIndexOf(selectedSystem) < 0) {
                selectedSystem = null;
                selectedPlanet = null;
            }

            updatePlanetsComboBoxModel((DefaultComboBoxModel<Planet>)choicePlanet.getModel(), null);
            choiceSystem.setModel(model);
        } else {
            choiceSystem.setModel(allSystems);
        }
        choiceSystem.setSelectedItem(selectedSystem);

        updatePlanetsComboBoxModel((DefaultComboBoxModel<Planet>)choicePlanet.getModel(), selectedSystem);
        choicePlanet.setSelectedItem(selectedPlanet);
    }

    private void updatePlanetsComboBoxModel(DefaultComboBoxModel<Planet> planetsModel, PlanetarySystem planetarySystem) {
        planetsModel.removeAllElements();
        if (planetarySystem != null) {
            planetsModel.addElement(planetarySystem.getPrimaryPlanet());
            for (Planet planet : planetarySystem.getPlanets()) {
                if (!planet.equals(planetarySystem.getPrimaryPlanet())) {
                    planetsModel.addElement(planet);
                }
            }
        }
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

    private void btnOkActionPerformed(ActionEvent evt) {
        person.setPreNominal(textPreNominal.getText());
        person.setGivenName(textGivenName.getText());
        person.setSurname(textSurname.getText());
        person.setPostNominal(textPostNominal.getText());
        person.setCallsign(textNickname.getText());
        person.setBloodname(textBloodname.getText().equals(resourceMap.getString("textBloodname.error"))
                ? "" : textBloodname.getText());
        person.setBiography(txtBio.getText());
        if (choiceGender.getSelectedItem() != null) {
            person.setGender(person.getGender().isInternal()
                    ? ((Gender) choiceGender.getSelectedItem()).getInternalVariant()
                    : (Gender) choiceGender.getSelectedItem());
        }
        person.setBirthday(birthdate);
        person.setRecruitment(recruitment);
        person.setLastRankChangeDate(lastRankChangeDate);
        person.setRetirement(retirement);
        person.setOriginFaction((Faction) choiceFaction.getSelectedItem());
        if (choiceSystem.getSelectedItem() != null && choicePlanet.getSelectedItem() != null) {
            person.setOriginPlanet((Planet)choicePlanet.getSelectedItem());
        } else {
            person.setOriginPlanet(null);
        }
        person.setPhenotype((Phenotype) choicePhenotype.getSelectedItem());
        person.setClanner(chkClan.isSelected());
        try {
            person.setToughness(Integer.parseInt(textToughness.getText()));
        } catch (NumberFormatException ignored) { }
        if (null == choiceOriginalUnit.getSelectedItem()) {
            person.setOriginalUnitWeight(choiceUnitWeight.getSelectedIndex());
            person.setOriginalUnitTech(choiceUnitTech.getSelectedIndex());
        } else {
            person.setOriginalUnitId(((Unit) choiceOriginalUnit.getSelectedItem()).getId());
        }
        person.setFounder(chkFounder.isSelected());
        setSkills();
        setOptions();
        setVisible(false);
    }

    private void randomName() {
        String factionCode = campaign.getCampaignOptions().useOriginFactionForNames()
                ? person.getOriginFaction().getShortName()
                : RandomNameGenerator.getInstance().getChosenFaction();

        String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                (Gender) choiceGender.getSelectedItem(), person.isClanner(), factionCode);
        textGivenName.setText(name[0]);
        textSurname.setText(name[1]);
    }

    private void randomBloodname() {
        Faction faction = campaign.getFaction().isClan() ? campaign.getFaction()
                : (Faction) choiceFaction.getSelectedItem();
        faction = ((faction != null) && faction.isClan()) ? faction : person.getOriginFaction();
        Bloodname bloodname = Bloodname.randomBloodname(faction.getShortName(), selectedPhenotype, campaign.getGameYear());
        textBloodname.setText((bloodname != null) ? bloodname.getName() : resourceMap.getString("textBloodname.error"));
    }

    public void refreshSkills() {
        panSkills.removeAll();

        JCheckBox chkSkill;
        JLabel lblName;
        JLabel lblValue;
        JLabel lblLevel;
        JLabel lblBonus;
        JSpinner spnLevel;
        JSpinner spnBonus;

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panSkills.setLayout(gridBag);

        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.insets = new java.awt.Insets(0, 10, 0, 0);
        c.gridx = 0;

        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            c.gridy = i;
            c.gridx = 0;
            final String type = SkillType.getSkillList()[i];
            chkSkill = new JCheckBox();
            chkSkill.setSelected(person.hasSkill(type));
            skillChks.put(type, chkSkill);
            chkSkill.addItemListener(e -> {
                changeSkillValue(type);
                changeValueEnabled(type);
            });
            lblName = new JLabel(type);
            lblValue = new JLabel();
            if (person.hasSkill(type)) {
                lblValue.setText(person.getSkill(type).toString());
            } else {
                lblValue.setText("-");
            }
            skillValues.put(type, lblValue);
            lblLevel = new JLabel(resourceMap.getString("lblLevel.text"));
            lblBonus = new JLabel(resourceMap.getString("lblBonus.text"));
            int level = 0;
            int bonus = 0;
            if (person.hasSkill(type)) {
                level = person.getSkill(type).getLevel();
                bonus = person.getSkill(type).getBonus();
            }
            spnLevel = new JSpinner(new SpinnerNumberModel(level, 0, 10, 1));
            spnLevel.addChangeListener(evt -> changeSkillValue(type));
            spnLevel.setEnabled(chkSkill.isSelected());
            spnBonus = new JSpinner(new SpinnerNumberModel(bonus, -8, 8, 1));
            spnBonus.addChangeListener(evt -> changeSkillValue(type));
            spnBonus.setEnabled(chkSkill.isSelected());
            skillLvls.put(type, spnLevel);
            skillBonus.put(type, spnBonus);

            c.anchor = java.awt.GridBagConstraints.WEST;
            c.weightx = 0;
            panSkills.add(chkSkill, c);

            c.gridx = 1;
            c.anchor = java.awt.GridBagConstraints.WEST;
            panSkills.add(lblName, c);

            c.gridx = 2;
            c.anchor = java.awt.GridBagConstraints.CENTER;
            panSkills.add(lblValue, c);

            c.gridx = 3;
            c.anchor = java.awt.GridBagConstraints.WEST;
            panSkills.add(lblLevel, c);

            c.gridx = 4;
            c.anchor = java.awt.GridBagConstraints.WEST;
            panSkills.add(spnLevel, c);

            c.gridx = 5;
            c.anchor = java.awt.GridBagConstraints.WEST;
            panSkills.add(lblBonus, c);

            c.gridx = 6;
            c.anchor = java.awt.GridBagConstraints.WEST;
            c.weightx = 1.0;
            panSkills.add(spnBonus, c);
        }
    }

    private void setSkills() {
        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            final String type = SkillType.getSkillList()[i];
            if (skillChks.get(type).isSelected()) {
                int lvl = (Integer) skillLvls.get(type).getModel().getValue();
                int b = (Integer) skillBonus.get(type).getModel().getValue();
                person.addSkill(type, lvl, b);
            } else {
                person.removeSkill(type);
            }
        }
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue().equals("None"))) {
                person.getOptions().getOption(option.getName()).setValue("None");
            } else {
                person.getOptions().getOption(option.getName()).setValue(comp.getValue());
            }
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new ArrayList<>();

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridBag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)
                    && !campaign.getCampaignOptions().useAbilities()) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.EDGE_ADVANTAGES)
                    && !campaign.getCampaignOptions().useEdge()) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES)
                    && !campaign.getCampaignOptions().useImplants()) {
                continue;
            }

            addGroup(group, gridBag, c);

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                addOption(j.nextElement(), gridBag, c);
            }
        }
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridBag, GridBagConstraints c) {
        JLabel groupLabel = new JLabel(resourceMap.getString("optionGroup." + group.getKey()));

        gridBag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridBag, GridBagConstraints c) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, true);

        if (OptionsConstants.GUNNERY_WEAPON_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            //holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements();) {
                EquipmentType etype = i.nextElement();
                if (SpecialAbility.isWeaponEligibleForSPA(etype, person.getPrimaryRole(), false)) {
                    optionComp.addValue(etype.getName());
                }
            }
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_SANDBLASTER.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            //holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements();) {
                EquipmentType etype = i.nextElement();
                if (SpecialAbility.isWeaponEligibleForSPA(etype, person.getPrimaryRole(), true)) {
                    optionComp.addValue(etype.getName());
                }
            }
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            optionComp.addValue(Crew.SPECIAL_ENERGY);
            optionComp.addValue(Crew.SPECIAL_BALLISTIC);
            optionComp.addValue(Crew.SPECIAL_MISSILE);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_RANGE_MASTER.equals(option.getName())) {
            optionComp.addValue(Crew.RANGEMASTER_NONE);
            optionComp.addValue(Crew.RANGEMASTER_MEDIUM);
            optionComp.addValue(Crew.RANGEMASTER_LONG);
            optionComp.addValue(Crew.RANGEMASTER_EXTREME);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) {
            optionComp.addValue(Crew.HUMANTRO_NONE);
            optionComp.addValue(Crew.HUMANTRO_MECH);
            optionComp.addValue(Crew.HUMANTRO_AERO);
            optionComp.addValue(Crew.HUMANTRO_VEE);
            optionComp.addValue(Crew.HUMANTRO_BA);
            optionComp.setSelected(option.stringValue());
        } else if (option.getType() == Option.CHOICE) {
            SpecialAbility spa = SpecialAbility.getOption(option.getName());
            if (null != spa) {
                for (String val : spa.getChoiceValues()) {
                    optionComp.addValue(val);
                }
                optionComp.setSelected(option.stringValue());
            }
        }

        gridBag.setConstraints(optionComp, c);
        panOptions.add(optionComp);
        optionComps.add(optionComp);
    }

    private void setOptions() {
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue().equals("None"))) {
                person.getOptions().getOption(option.getName()).setValue("None");
            } else {
                person.getOptions().getOption(option.getName())
                        .setValue(comp.getValue());
            }
        }
    }

    private void changeSkillValue(String type) {
        if (!skillChks.get(type).isSelected()) {
            skillValues.get(type).setText("-");
            return;
        }
        SkillType stype = SkillType.getType(type);
        int lvl = (Integer) skillLvls.get(type).getModel().getValue();
        int b = (Integer) skillBonus.get(type).getModel().getValue();
        int target = stype.getTarget() - lvl - b;
        if (stype.countUp()) {
            target = stype.getTarget() + lvl + b;
            skillValues.get(type).setText("+" + target);
        } else {
            skillValues.get(type).setText(target + "+");
        }
    }

    private void changeValueEnabled(String type) {
        skillLvls.get(type).setEnabled(skillChks.get(type).isSelected());
        skillBonus.get(type).setEnabled(skillChks.get(type).isSelected());
    }

    private void btnDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, birthdate);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            birthdate = dc.getDate();
            btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(birthdate));
            lblAge.setText(getAge() + " " + resourceMap.getString("age"));
        }
    }

    private void btnServiceDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, recruitment);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            recruitment = dc.getDate();
            btnServiceDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(recruitment));
        }
    }

    private void btnRankDateActionPerformed() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, lastRankChangeDate);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            lastRankChangeDate = dc.getDate();
            btnRankDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(lastRankChangeDate));
        }
    }

    private void btnRetirementDateActionPerformed() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, retirement);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            retirement = dc.getDate();
            btnRetirementDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(retirement));
        }
    }

    public int getAge() {
        // Get age based on year
        return Period.between(birthdate, campaign.getLocalDate()).getYears();
    }

    private void backgroundChanged() {
        final Phenotype newPhenotype = (Phenotype) choicePhenotype.getSelectedItem();
        if (chkClan.isSelected() || (newPhenotype == Phenotype.NONE)) {
            if ((newPhenotype != null) && (newPhenotype != selectedPhenotype)) {
                switch (selectedPhenotype) {
                    case MECHWARRIOR:
                        decreasePhenotypeBonus(SkillType.S_GUN_MECH);
                        decreasePhenotypeBonus(SkillType.S_PILOT_MECH);
                        break;
                    case ELEMENTAL:
                        decreasePhenotypeBonus(SkillType.S_GUN_BA);
                        decreasePhenotypeBonus(SkillType.S_ANTI_MECH);
                        break;
                    case AEROSPACE:
                        decreasePhenotypeBonus(SkillType.S_GUN_AERO);
                        decreasePhenotypeBonus(SkillType.S_PILOT_AERO);
                        decreasePhenotypeBonus(SkillType.S_GUN_JET);
                        decreasePhenotypeBonus(SkillType.S_PILOT_JET);
                        break;
                    case VEHICLE:
                        decreasePhenotypeBonus(SkillType.S_GUN_VEE);
                        decreasePhenotypeBonus(SkillType.S_PILOT_GVEE);
                        decreasePhenotypeBonus(SkillType.S_PILOT_NVEE);
                        decreasePhenotypeBonus(SkillType.S_PILOT_VTOL);
                        break;
                    case PROTOMECH:
                        decreasePhenotypeBonus(SkillType.S_GUN_PROTO);
                        break;
                    case NAVAL:
                        decreasePhenotypeBonus(SkillType.S_TECH_VESSEL);
                        decreasePhenotypeBonus(SkillType.S_GUN_SPACE);
                        decreasePhenotypeBonus(SkillType.S_PILOT_SPACE);
                        decreasePhenotypeBonus(SkillType.S_NAV);
                        break;
                    default:
                        break;
                }

                switch (newPhenotype) {
                    case MECHWARRIOR:
                        increasePhenotypeBonus(SkillType.S_GUN_MECH);
                        increasePhenotypeBonus(SkillType.S_PILOT_MECH);
                        break;
                    case ELEMENTAL:
                        increasePhenotypeBonus(SkillType.S_GUN_BA);
                        increasePhenotypeBonus(SkillType.S_ANTI_MECH);
                        break;
                    case AEROSPACE:
                        increasePhenotypeBonus(SkillType.S_GUN_AERO);
                        increasePhenotypeBonus(SkillType.S_PILOT_AERO);
                        increasePhenotypeBonus(SkillType.S_GUN_JET);
                        increasePhenotypeBonus(SkillType.S_PILOT_JET);
                        break;
                    case VEHICLE:
                        increasePhenotypeBonus(SkillType.S_GUN_VEE);
                        increasePhenotypeBonus(SkillType.S_PILOT_GVEE);
                        increasePhenotypeBonus(SkillType.S_PILOT_NVEE);
                        increasePhenotypeBonus(SkillType.S_PILOT_VTOL);
                        break;
                    case PROTOMECH:
                        increasePhenotypeBonus(SkillType.S_GUN_PROTO);
                        break;
                    case NAVAL:
                        increasePhenotypeBonus(SkillType.S_TECH_VESSEL);
                        increasePhenotypeBonus(SkillType.S_GUN_SPACE);
                        increasePhenotypeBonus(SkillType.S_PILOT_SPACE);
                        increasePhenotypeBonus(SkillType.S_NAV);
                        break;
                    default:
                        break;
                }

                selectedPhenotype = newPhenotype;
            }
        } else {
            choicePhenotype.setSelectedItem(Phenotype.NONE);
        }

        choicePhenotype.setEnabled(chkClan.isSelected());
    }

    private void increasePhenotypeBonus(String skillType) {
        final int value = Math.min((Integer) skillBonus.get(skillType).getValue() + 1, 8);
        skillBonus.get(skillType).setValue(value);
    }

    private void decreasePhenotypeBonus(String skillType) {
        final int value = Math.max((Integer) skillBonus.get(skillType).getValue() - 1, -8);
        skillBonus.get(skillType).setValue(value);
    }

    @Override
    public void optionClicked(DialogOptionComponent arg0, IOption arg1, boolean arg2) {
        //Implement me!!
    }
}
