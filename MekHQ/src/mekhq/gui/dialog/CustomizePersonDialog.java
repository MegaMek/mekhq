/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.personnel.Skill.getCountDownMaxValue;
import static mekhq.campaign.personnel.Skill.getCountUpMaxValue;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.personalityQuirksSortedAlphabetically;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.*;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.DialogOptionComponent;
import megamek.client.ui.swing.DialogOptionListener;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.enums.Gender;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.Intelligence;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.control.EditKillLogControl;
import mekhq.gui.control.EditPersonnelLogControl;
import mekhq.gui.control.EditScenarioLogControl;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MarkdownEditorPanel;

/**
 * This dialog is used to both hire new pilots and to edit existing ones
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class CustomizePersonDialog extends JDialog implements DialogOptionListener {
    private static final MMLogger logger = MMLogger.create(CustomizePersonDialog.class);

    // region Variable declarations
    private       Person                      person;
    private       List<DialogOptionComponent> optionComps = new ArrayList<>();
    private final Map<String, JSpinner>       skillLevels = new Hashtable<>();
    private final Map<String, JSpinner>       skillBonus  = new Hashtable<>();
    private final Map<String, JLabel>         skillValues = new Hashtable<>();
    private final Map<String, JCheckBox>      skillChecks = new Hashtable<>();
    private       PersonnelOptions            options;
    private       LocalDate                   birthdate;
    private       LocalDate                   recruitment;
    private       LocalDate                   lastRankChangeDate;
    private       LocalDate                   retirement;
    private final JFrame                      frame;

    private JButton                               btnDate;
    private JButton                               btnServiceDate;
    private JButton                               btnRankDate;
    private JButton                               btnRetirementDate;
    private JComboBox<Gender>                     choiceGender;
    private JLabel                                lblAge;
    private AbstractMHQScrollablePanel            skillsPanel;
    private AbstractMHQScrollablePanel            optionsPanel;
    private JTextField                            textToughness;
    private JTextField                            textFatigue;
    private JComboBox<EducationLevel>             textEducationLevel;
    private JTextField                            textLoyalty;
    private JTextField                            textPreNominal;
    private JTextField                            textGivenName;
    private JTextField                            textSurname;
    private JTextField                            textPostNominal;
    private JTextField                            textNickname;
    private JTextField                            textBloodname;
    private MarkdownEditorPanel                   txtBio;
    private JComboBox<Faction>                    choiceFaction;
    private JComboBox<PlanetarySystem>            choiceSystem;
    private DefaultComboBoxModel<PlanetarySystem> allSystems;
    private JCheckBox                             chkOnlyOurFaction;
    private JComboBox<Planet>                     choicePlanet;
    private JCheckBox                             chkClan;
    private JComboBox<Phenotype>                  choicePhenotype;
    private Phenotype                             selectedPhenotype;

    /* Against the Bot */
    private JComboBox<String> choiceUnitWeight;
    private JComboBox<String> choiceUnitTech;
    private JCheckBox         chkFounder;
    private JComboBox<Unit>   choiceOriginalUnit;

    // random personality
    private MMComboBox<Aggression>       comboAggression;
    private MMComboBox<Ambition>         comboAmbition;
    private MMComboBox<Greed>            comboGreed;
    private MMComboBox<Social>           comboSocial;
    private MMComboBox<PersonalityQuirk> comboPersonalityQuirk;
    private MMComboBox<Intelligence>     comboIntelligence;

    private final Campaign campaign;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.CustomizePersonDialog",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable declarations

    /** Creates new form CustomizePilotDialog */
    public CustomizePersonDialog(JFrame parent, boolean modal, Person person, Campaign campaign) {
        super(parent, modal);
        this.campaign = campaign;
        this.frame    = parent;
        this.person   = person;
        initializePilotAndOptions();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initializePilotAndOptions() {
        birthdate = person.getDateOfBirth();
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
        options           = person.getOptions();
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        setMinimumSize(new Dimension(1100, 500));

        JPanel      panDemog       = new JPanel(new GridBagLayout());
        JTabbedPane tabStats       = new JTabbedPane();
        JLabel      lblName        = new JLabel();
        JLabel      lblGender      = new JLabel();
        JLabel      lblBirthday    = new JLabel();
        JLabel      lblRecruitment = new JLabel();
        lblAge = new JLabel();
        JLabel lblNickname  = new JLabel();
        JLabel lblBloodname = new JLabel();
        JPanel panName      = new JPanel(new GridBagLayout());
        textNickname  = new JTextField();
        textBloodname = new JTextField();
        textToughness = new JTextField();
        JLabel lblFatigue = new JLabel();
        textFatigue = new JTextField();
        JLabel lblLoyalty = new JLabel();
        textLoyalty = new JTextField();
        JLabel lblToughness = new JLabel();
        textEducationLevel = new JComboBox<>();
        JLabel      lblEducationLevel = new JLabel();
        JScrollPane scrOptions        = new JScrollPaneWithSpeed();
        JScrollPane scrSkills         = new JScrollPaneWithSpeed();
        JPanel      panButtons        = new JPanel();
        JButton     btnOk             = new JButton();

        JButton btnClose           = new JButton();
        JButton btnRandomName      = new JButton();
        JButton btnRandomBloodname = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title"));

        setName("Form");
        getContentPane().setLayout(new GridBagLayout());

        int y = 1;

        lblName.setText(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(lblName, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;

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

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        panDemog.add(panName, gridBagConstraints);

        btnRandomName.setText(resourceMap.getString("btnRandomName.text"));
        btnRandomName.setName("btnRandomName");
        btnRandomName.addActionListener(evt -> randomName());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        panDemog.add(btnRandomName, gridBagConstraints);

        y++;

        if (person.isClanPersonnel()) {
            lblBloodname.setText(resourceMap.getString("lblBloodname.text"));
            lblBloodname.setName("lblBloodname");
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblBloodname, gridBagConstraints);

            textBloodname.setMinimumSize(new Dimension(150, 28));
            textBloodname.setName("textBloodname");
            textBloodname.setPreferredSize(new Dimension(150, 28));
            gridBagConstraints           = new GridBagConstraints();
            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.fill      = GridBagConstraints.BOTH;
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
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblNickname, gridBagConstraints);

            textNickname.setText(person.getCallsign());
            textNickname.setName("textNickname");
            gridBagConstraints           = new GridBagConstraints();
            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.fill      = GridBagConstraints.BOTH;
            panDemog.add(textNickname, gridBagConstraints);

            JButton btnRandomCallsign = new JButton(resourceMap.getString("btnRandomCallsign.text"));
            btnRandomCallsign.setName("btnRandomCallsign");
            btnRandomCallsign.addActionListener(e -> textNickname.setText(RandomCallsignGenerator.getInstance()
                                                                                .generate()));
            gridBagConstraints.gridx = 2;
            panDemog.add(btnRandomCallsign, gridBagConstraints);
        }

        y++;

        lblGender.setText(resourceMap.getString("lblGender.text"));
        lblGender.setName("lblGender");
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(lblGender, gridBagConstraints);

        choiceGender = new JComboBox<>(Gender.values());
        choiceGender.setName("choiceGender");
        choiceGender.setSelectedItem(person.getGender());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(choiceGender, gridBagConstraints);

        y++;

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin Faction:"), gridBagConstraints);

        DefaultComboBoxModel<Faction> factionsModel = getFactionsComboBoxModel();
        choiceFaction = new JComboBox<>(factionsModel);
        choiceFaction.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Faction faction) {
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
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(choiceFaction, gridBagConstraints);

        y++;

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin System:"), gridBagConstraints);

        DefaultComboBoxModel<Planet> planetsModel = new DefaultComboBoxModel<>();
        choicePlanet = new JComboBox<>(planetsModel);

        allSystems   = getPlanetarySystemsComboBoxModel();
        choiceSystem = new JComboBox<>(allSystems);
        choiceSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem system) {
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
            PlanetarySystem selectedSystem = (PlanetarySystem) choiceSystem.getSelectedItem();

            choicePlanet.setSelectedIndex(-1);
            updatePlanetsComboBoxModel(planetsModel, selectedSystem);
        });

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(choiceSystem, gridBagConstraints);

        chkOnlyOurFaction = new JCheckBox("Faction Specific");
        chkOnlyOurFaction.addActionListener(e -> filterPlanetarySystemsForOurFaction(chkOnlyOurFaction.isSelected()));

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(chkOnlyOurFaction, gridBagConstraints);

        y++;

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Origin Planet:"), gridBagConstraints);

        choicePlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet planet) {
                    setText(planet.getName(campaign.getLocalDate()));
                }

                return this;
            }
        });
        if (person.getOriginPlanet() != null) {
            choicePlanet.setSelectedIndex(planetsModel.getIndexOf(person.getOriginPlanet()));
        }

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(choicePlanet, gridBagConstraints);

        y++;

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
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
        choicePhenotype.setEnabled(person.isClanPersonnel());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(choicePhenotype, gridBagConstraints);

        chkClan = new JCheckBox("Clan Personnel");
        chkClan.setSelected(person.isClanPersonnel());
        chkClan.addItemListener(et -> backgroundChanged());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panDemog.add(chkClan, gridBagConstraints);

        y++;

        lblBirthday.setText(resourceMap.getString("lblBday.text"));
        lblBirthday.setName("lblBirthday");
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(lblBirthday, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(birthdate));
        btnDate.setName("btnDate");
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panDemog.add(btnDate, gridBagConstraints);

        lblAge.setText(person.getAge(campaign.getLocalDate()) + " " + resourceMap.getString("age"));
        lblAge.setName("lblAge");
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 2;
        gridBagConstraints.gridy  = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemog.add(lblAge, gridBagConstraints);

        y++;

        if (campaign.getCampaignOptions().isUseTimeInService() && (recruitment != null)) {
            lblRecruitment.setText(resourceMap.getString("lblRecruitment.text"));
            lblRecruitment.setName("lblRecruitment");
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblRecruitment, gridBagConstraints);

            btnServiceDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(recruitment));
            btnServiceDate.setName("btnServiceDate");
            btnServiceDate.addActionListener(this::btnServiceDateActionPerformed);
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(btnServiceDate, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().isUseTimeInRank() && (lastRankChangeDate != null)) {
            JLabel lblLastRankChangeDate = new JLabel(resourceMap.getString("lblLastRankChangeDate.text"));
            lblLastRankChangeDate.setName("lblLastRankChangeDate");
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblLastRankChangeDate, gridBagConstraints);

            btnRankDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(lastRankChangeDate));
            btnRankDate.setName("btnRankDate");
            btnRankDate.addActionListener(e -> btnRankDateActionPerformed());
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(btnRankDate, gridBagConstraints);

            y++;
        }

        if (retirement != null) {
            JLabel lblRetirement = new JLabel(resourceMap.getString("lblRetirement.text"));
            lblRetirement.setName("lblRetirement");
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblRetirement, gridBagConstraints);

            btnRetirementDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(retirement));
            btnRetirementDate.setName("btnRetirementDate");
            btnRetirementDate.addActionListener(e -> btnRetirementDateActionPerformed());
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(btnRetirementDate, gridBagConstraints);

            y++;
        }

        lblToughness.setText(resourceMap.getString("lblToughness.text"));
        lblToughness.setName("lblToughness");

        textToughness.setText(Integer.toString(person.getToughness()));
        textToughness.setName("textToughness");

        if (campaign.getCampaignOptions().isUseToughness()) {
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblToughness, gridBagConstraints);
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
            panDemog.add(textToughness, gridBagConstraints);

            y++;
        }

        lblFatigue.setText(resourceMap.getString("lblFatigue.text"));
        lblFatigue.setName("lblFatigue");

        textFatigue.setText(Integer.toString(person.getFatigue()));
        textFatigue.setName("textFatigue");

        if (campaign.getCampaignOptions().isUseFatigue()) {
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblFatigue, gridBagConstraints);
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
            panDemog.add(textFatigue, gridBagConstraints);

            y++;
        }

        lblEducationLevel.setText(resourceMap.getString("lblEducationLevel.text"));
        lblEducationLevel.setName("lblEducationLevel");

        for (EducationLevel level : EducationLevel.values()) {
            textEducationLevel.addItem(level);
        }
        textEducationLevel.setSelectedItem(person.getEduHighestEducation());
        textEducationLevel.setName("textEducationLevel");

        if (campaign.getCampaignOptions().isUseEducationModule()) {
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblEducationLevel, gridBagConstraints);
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
            panDemog.add(textEducationLevel, gridBagConstraints);

            y++;
        }

        lblLoyalty.setText(resourceMap.getString("lblLoyalty.text"));
        lblLoyalty.setName("lblLoyalty");

        textLoyalty.setText(Integer.toString(person.getLoyalty()));
        textLoyalty.setName("textLoyalty");

        if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) &&
            (!campaign.getCampaignOptions().isUseHideLoyalty())) {
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblLoyalty, gridBagConstraints);
            gridBagConstraints        = new GridBagConstraints();
            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
            panDemog.add(textLoyalty, gridBagConstraints);

            y++;
        }

        JLabel lblUnit = new JLabel();
        lblUnit.setText("Original unit:");
        lblUnit.setName("lblUnit");

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
        lblShares.setText(person.getNumShares(campaign, campaign.getCampaignOptions().isSharesForAll()) + " shares");

        chkFounder = new JCheckBox("Founding member");
        chkFounder.setSelected(person.isFounder());

        choiceOriginalUnit = new JComboBox<>();
        choiceOriginalUnit.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (null == value) {
                    setText("None");
                } else {
                    setText(((Unit) value).getName());
                }
                return this;
            }
        });
        populateUnitChoiceCombo();

        if (null == person.getOriginalUnitId() || null == campaign.getUnit(person.getOriginalUnitId())) {
            choiceOriginalUnit.setSelectedItem(null);
        } else {
            choiceOriginalUnit.setSelectedItem(campaign.getUnit(person.getOriginalUnitId()));
        }
        choiceOriginalUnit.addActionListener(ev -> {
            try {
                Unit unit = (Unit) choiceOriginalUnit.getSelectedItem();
                choiceUnitWeight.setSelectedIndex(unit.getEntity().getWeightClass());
                if (unit.getEntity().isClan()) {
                    choiceUnitTech.setSelectedIndex(2);
                } else if (unit.getEntity().getTechLevel() > TechConstants.T_INTRO_BOXSET) {
                    choiceUnitTech.setSelectedIndex(1);
                } else {
                    choiceUnitTech.setSelectedIndex(0);
                }
            } catch (Exception e) {
                choiceUnitWeight.setSelectedIndex(person.getOriginalUnitWeight());
                choiceUnitTech.setSelectedIndex(person.getOriginalUnitTech());
            }
        });

        y++;

        if (campaign.getCampaignOptions().isUseAtB()) {
            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(lblUnit, gridBagConstraints);

            gridBagConstraints.gridx  = 1;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(choiceUnitWeight, gridBagConstraints);

            gridBagConstraints.gridx  = 2;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemog.add(choiceUnitTech, gridBagConstraints);

            y++;

            gridBagConstraints.gridx     = 0;
            gridBagConstraints.gridy     = y;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(choiceOriginalUnit, gridBagConstraints);

            y++;

            gridBagConstraints.gridx     = 0;
            gridBagConstraints.gridy     = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(chkFounder, gridBagConstraints);

            if (campaign.getCampaignOptions().isUseShareSystem()) {
                gridBagConstraints.gridx     = 2;
                gridBagConstraints.gridy     = y;
                gridBagConstraints.gridwidth = 1;
                gridBagConstraints.anchor    = GridBagConstraints.WEST;
                panDemog.add(lblShares, gridBagConstraints);
            }
        }

        y++;

        // region random personality
        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            JLabel labelAggression = new JLabel();
            labelAggression.setText("Aggression:");
            labelAggression.setName("labelAggression");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelAggression, gridBagConstraints);

            comboAggression = new MMComboBox<>("comboAggression", Aggression.values());
            comboAggression.setSelectedItem(person.getAggression());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboAggression, gridBagConstraints);

            JLabel labelAmbition = new JLabel();
            labelAmbition.setText("Ambition:");
            labelAmbition.setName("labelAmbition");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelAmbition, gridBagConstraints);

            comboAmbition = new MMComboBox<>("comboAmbition", Ambition.values());
            comboAmbition.setSelectedItem(person.getAmbition());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboAmbition, gridBagConstraints);

            JLabel labelGreed = new JLabel();
            labelGreed.setText("Greed:");
            labelGreed.setName("labelGreed");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelGreed, gridBagConstraints);

            comboGreed = new MMComboBox<>("comboGreed", Greed.values());
            comboGreed.setSelectedItem(person.getGreed());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboGreed, gridBagConstraints);

            JLabel labelSocial = new JLabel();
            labelSocial.setText("Social:");
            labelSocial.setName("labelSocial");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelSocial, gridBagConstraints);

            comboSocial = new MMComboBox<>("comboSocial", Social.values());
            comboSocial.setSelectedItem(person.getSocial());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboSocial, gridBagConstraints);

            JLabel labelPersonalityQuirk = new JLabel();
            labelPersonalityQuirk.setText("Quirk:");
            labelPersonalityQuirk.setName("labelPersonalityQuirk");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelPersonalityQuirk, gridBagConstraints);

            comboPersonalityQuirk = new MMComboBox<>("comboPersonalityQuirk", personalityQuirksSortedAlphabetically());
            comboPersonalityQuirk.setSelectedItem(person.getPersonalityQuirk());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboPersonalityQuirk, gridBagConstraints);

            JLabel labelIntelligence = new JLabel();
            labelIntelligence.setText("Intelligence:");
            labelIntelligence.setName("labelIntelligence");

            gridBagConstraints.gridx  = 0;
            gridBagConstraints.gridy  = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemog.add(labelIntelligence, gridBagConstraints);

            comboIntelligence = new MMComboBox<>("comboIntelligence", Intelligence.values());
            comboIntelligence.setSelectedItem(person.getIntelligence());

            gridBagConstraints.gridx     = 1;
            gridBagConstraints.gridy     = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor    = GridBagConstraints.WEST;
            gridBagConstraints.insets    = new Insets(0, 5, 0, 0);
            panDemog.add(comboIntelligence, gridBagConstraints);
        }

        y++;

        txtBio = new MarkdownEditorPanel("Biography");
        txtBio.setText(person.getBiography());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx   = 0.0;
        gridBagConstraints.weighty   = 1.0;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panDemog.add(txtBio, gridBagConstraints);

        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 0;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(panDemog, gridBagConstraints);

        skillsPanel = new DefaultMHQScrollablePanel(frame, "skillsPanel");
        refreshSkills();
        scrSkills.setViewportView(skillsPanel);
        scrSkills.setMinimumSize(new Dimension(500, 500));
        scrSkills.setPreferredSize(new Dimension(500, 500));

        optionsPanel = new DefaultMHQScrollablePanel(frame, "optionsPanel");
        refreshOptions();
        scrOptions.setViewportView(optionsPanel);
        scrOptions.setMinimumSize(new Dimension(500, 500));
        scrOptions.setPreferredSize(new Dimension(500, 500));

        tabStats.addTab(resourceMap.getString("scrSkills.TabConstraints.tabTitle"), scrSkills);
        if (campaign.getCampaignOptions().isUseAbilities() ||
            campaign.getCampaignOptions().isUseEdge() ||
            campaign.getCampaignOptions().isUseImplants()) {
            tabStats.addTab(resourceMap.getString("scrOptions.TabConstraints.tabTitle"), scrOptions);
        }
        tabStats.add(resourceMap.getString("panLog.TabConstraints.tabTitle"),
              new EditPersonnelLogControl(frame, campaign, person));
        tabStats.add(resourceMap.getString("panScenarios.title"), new EditScenarioLogControl(frame, campaign, person));
        tabStats.add(resourceMap.getString("panKills.TabConstraints.tabTitle"),
              new EditKillLogControl(frame, campaign, person));

        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 1;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabStats, gridBagConstraints);

        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());

        btnOk.setText(resourceMap.getString("btnOk.text"));
        btnOk.setName("btnOk");
        btnOk.addActionListener(this::btnOkActionPerformed);
        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        panButtons.add(btnOk, gridBagConstraints);
        gridBagConstraints.gridx++;

        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }

    /**
     * Populates a combo box with a list of units that the specified person can interact with, based on their abilities
     * to drive, gun, or tech the corresponding entities.
     *
     * <p>
     * The method adds eligible units from the campaign's unit list to the combo box {@code choiceOriginalUnit}, and
     * starts by adding a {@code null} entry to represent no selection.
     * </p>
     */
    private void populateUnitChoiceCombo() {
        choiceOriginalUnit.addItem(null); // Add a null entry as the initial option

        // Iterate through all units in the campaign
        for (Unit unit : campaign.getUnits()) {
            Entity entity = unit.getEntity();

            // Skip units without an associated entity
            if (entity == null) {
                continue;
            }

            // Add units to the combo box based on the person's capabilities
            if (person.canDrive(entity)) {
                choiceOriginalUnit.addItem(unit);
                continue; // Skip further checks if already added
            }

            if (person.canGun(entity)) {
                choiceOriginalUnit.addItem(unit);
                continue; // Skip further checks if already added
            }

            if (person.canTech(entity)) {
                choiceOriginalUnit.addItem(unit);
            }
        }
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CustomizePersonDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private DefaultComboBoxModel<Faction> getFactionsComboBoxModel() {
        int year = campaign.getGameYear();
        List<Faction> orderedFactions = Factions.getInstance()
                                              .getFactions()
                                              .stream()
                                              .sorted((a, b) -> a.getFullName(year)
                                                                      .compareToIgnoreCase(b.getFullName(year)))
                                              .toList();

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
                int endYear = person.getRecruitment() != null ?
                                    Math.min(person.getRecruitment().getYear(), year) :
                                    year;
                if (faction.validBetween(person.getDateOfBirth().getYear(), endYear)) {
                    factionsModel.addElement(faction);
                }
            }
        }

        return factionsModel;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel() {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        List<PlanetarySystem> orderedSystems = campaign.getSystems()
                                                     .stream()
                                                     .sorted(Comparator.comparing(a -> a.getName(campaign.getLocalDate())))
                                                     .toList();
        for (PlanetarySystem system : orderedSystems) {
            model.addElement(system);
        }
        return model;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel(Faction faction) {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        List<PlanetarySystem> orderedSystems = campaign.getSystems()
                                                     .stream()
                                                     .filter(a -> a.getFactionSet(person.getDateOfBirth())
                                                                        .contains(faction))
                                                     .sorted(Comparator.comparing(a -> a.getName(person.getDateOfBirth())))
                                                     .toList();
        for (PlanetarySystem system : orderedSystems) {
            model.addElement(system);
        }

        return model;
    }

    private void filterPlanetarySystemsForOurFaction(boolean onlyOurFaction) {
        PlanetarySystem selectedSystem = (PlanetarySystem) choiceSystem.getSelectedItem();
        Planet          selectedPlanet = (Planet) choicePlanet.getSelectedItem();
        if (onlyOurFaction && choiceFaction.getSelectedItem() != null) {
            Faction faction = (Faction) choiceFaction.getSelectedItem();

            DefaultComboBoxModel<PlanetarySystem> model = getPlanetarySystemsComboBoxModel(faction);
            if (model.getIndexOf(selectedSystem) < 0) {
                selectedSystem = null;
                selectedPlanet = null;
            }

            updatePlanetsComboBoxModel((DefaultComboBoxModel<Planet>) choicePlanet.getModel(), null);
            choiceSystem.setModel(model);
        } else {
            choiceSystem.setModel(allSystems);
        }
        choiceSystem.setSelectedItem(selectedSystem);

        updatePlanetsComboBoxModel((DefaultComboBoxModel<Planet>) choicePlanet.getModel(), selectedSystem);
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
        person.setBloodname(textBloodname.getText().equals(resourceMap.getString("textBloodname.error")) ?
                                  "" :
                                  textBloodname.getText());
        person.setBiography(txtBio.getText());

        if (choiceGender.getSelectedItem() != null) {
            person.setGender((Gender) choiceGender.getSelectedItem());
        }

        person.setDateOfBirth(birthdate);
        person.setRecruitment(recruitment);
        person.setLastRankChangeDate(lastRankChangeDate);
        person.setRetirement(retirement);
        person.setOriginFaction((Faction) choiceFaction.getSelectedItem());

        if (choiceSystem.getSelectedItem() != null && choicePlanet.getSelectedItem() != null) {
            person.setOriginPlanet((Planet) choicePlanet.getSelectedItem());
        } else {
            person.setOriginPlanet(null);
        }
        person.setPhenotype((Phenotype) choicePhenotype.getSelectedItem());
        person.setClanPersonnel(chkClan.isSelected());

        if (campaign.getCampaignOptions().isUseToughness()) {
            try {
                person.setToughness(Integer.parseInt(textToughness.getText()));
            } catch (NumberFormatException ignored) {
            }
        }

        if (campaign.getCampaignOptions().isUseEducationModule()) {
            person.setEduHighestEducation((EducationLevel) textEducationLevel.getSelectedItem());
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            try {
                person.setLoyalty(Integer.parseInt(textLoyalty.getText()));
            } catch (NumberFormatException ignored) {
            }
        }

        if (campaign.getCampaignOptions().isUseFatigue()) {
            try {
                person.setFatigue(Integer.parseInt(textFatigue.getText()));
            } catch (NumberFormatException ignored) {
            }
        }

        if (null == choiceOriginalUnit.getSelectedItem()) {
            person.setOriginalUnit(null);
            person.setOriginalUnitWeight(choiceUnitWeight.getSelectedIndex());
            person.setOriginalUnitTech(choiceUnitTech.getSelectedIndex());
        } else {
            person.setOriginalUnit((Unit) choiceOriginalUnit.getSelectedItem());
        }

        person.setFounder(chkFounder.isSelected());

        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            person.setAggression(comboAggression.getSelectedItem());
            person.setAmbition(comboAmbition.getSelectedItem());
            person.setGreed(comboGreed.getSelectedItem());
            person.setSocial(comboSocial.getSelectedItem());
            person.setPersonalityQuirk(comboPersonalityQuirk.getSelectedItem());
            person.setIntelligence(comboIntelligence.getSelectedItem());
            PersonalityController.writePersonalityDescription(person);
        }

        setSkills();
        setOptions();
        setVisible(false);
    }

    private void randomName() {
        String factionCode = campaign.getCampaignOptions().isUseOriginFactionForNames() ?
                                   person.getOriginFaction().getShortName() :
                                   RandomNameGenerator.getInstance().getChosenFaction();

        String[] name = RandomNameGenerator.getInstance()
                              .generateGivenNameSurnameSplit((Gender) choiceGender.getSelectedItem(),
                                    person.isClanPersonnel(),
                                    factionCode);
        textGivenName.setText(name[0]);
        textSurname.setText(name[1]);
    }

    private void randomBloodname() {
        Faction faction = campaign.getFaction().isClan() ?
                                campaign.getFaction() :
                                (Faction) choiceFaction.getSelectedItem();
        faction = ((faction != null) && faction.isClan()) ? faction : person.getOriginFaction();
        Bloodname bloodname = Bloodname.randomBloodname(faction.getShortName(),
              selectedPhenotype,
              campaign.getGameYear());
        textBloodname.setText((bloodname != null) ? bloodname.getName() : resourceMap.getString("textBloodname.error"));
    }

    public void refreshSkills() {
        skillsPanel.removeAll();

        JCheckBox chkSkill;
        JLabel    lblName;
        JLabel    lblValue;
        JLabel    lblLevel;
        JLabel    lblBonus;
        JSpinner  spnLevel;
        JSpinner  spnBonus;

        GridBagLayout      gridBag = new GridBagLayout();
        GridBagConstraints c       = new GridBagConstraints();
        skillsPanel.setLayout(gridBag);

        c.gridwidth = 1;
        c.fill      = GridBagConstraints.NONE;
        c.insets    = new Insets(0, 10, 0, 0);
        c.gridx     = 0;

        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            c.gridy = i;
            c.gridx = 0;
            final String type = SkillType.getSkillList()[i];
            chkSkill = new JCheckBox();
            chkSkill.setSelected(person.hasSkill(type));
            skillChecks.put(type, chkSkill);
            chkSkill.addItemListener(e -> {
                changeSkillValue(type);
                changeValueEnabled(type);
            });
            lblName  = new JLabel(type);
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
            skillLevels.put(type, spnLevel);
            skillBonus.put(type, spnBonus);

            c.anchor  = GridBagConstraints.WEST;
            c.weightx = 0;
            skillsPanel.add(chkSkill, c);

            c.gridx  = 1;
            c.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblName, c);

            c.gridx  = 2;
            c.anchor = GridBagConstraints.CENTER;
            skillsPanel.add(lblValue, c);

            c.gridx  = 3;
            c.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblLevel, c);

            c.gridx  = 4;
            c.anchor = GridBagConstraints.WEST;
            skillsPanel.add(spnLevel, c);

            c.gridx  = 5;
            c.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblBonus, c);

            c.gridx   = 6;
            c.anchor  = GridBagConstraints.WEST;
            c.weightx = 1.0;
            skillsPanel.add(spnBonus, c);
        }
    }

    private void setSkills() {
        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            final String type = SkillType.getSkillList()[i];
            if (skillChecks.get(type).isSelected()) {
                int lvl = (Integer) skillLevels.get(type).getModel().getValue();
                int b   = (Integer) skillBonus.get(type).getModel().getValue();
                person.addSkill(type, lvl, b);
            } else {
                person.removeSkill(type);
            }
        }
        IOption option;
        for (final DialogOptionComponent newVar : optionComps) {
            option = newVar.getOption();
            if ((newVar.getValue().equals("None"))) {
                person.getOptions().getOption(option.getName()).setValue("None");
            } else {
                person.getOptions().getOption(option.getName()).setValue(newVar.getValue());
            }
        }
    }

    public void refreshOptions() {
        optionsPanel.removeAll();
        optionComps = new ArrayList<>();

        GridBagLayout      gridBag = new GridBagLayout();
        GridBagConstraints c       = new GridBagConstraints();
        optionsPanel.setLayout(gridBag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill      = GridBagConstraints.HORIZONTAL;
        c.insets    = new Insets(0, 0, 0, 0);
        c.ipadx     = 0;
        c.ipady     = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES) &&
                !campaign.getCampaignOptions().isUseAbilities()) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PersonnelOptions.EDGE_ADVANTAGES) &&
                !campaign.getCampaignOptions().isUseEdge()) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PersonnelOptions.MD_ADVANTAGES) &&
                !campaign.getCampaignOptions().isUseImplants()) {
                continue;
            }

            addGroup(group, gridBag, c);

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                addOption(j.nextElement(), gridBag, c);
            }
        }
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridBag, GridBagConstraints c) {
        JLabel groupLabel = new JLabel(resourceMap.getString("optionGroup." + group.getKey()));

        gridBag.setConstraints(groupLabel, c);
        optionsPanel.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridBag, GridBagConstraints c) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, true);

        if (OptionsConstants.GUNNERY_WEAPON_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            // holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements(); ) {
                EquipmentType equipmentType = i.nextElement();
                if (SpecialAbility.isWeaponEligibleForSPA(equipmentType, person.getPrimaryRole(), false)) {
                    optionComp.addValue(equipmentType.getName());
                }
            }
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_SANDBLASTER.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            // holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements(); ) {
                EquipmentType equipmentType = i.nextElement();
                if (SpecialAbility.isWeaponEligibleForSPA(equipmentType, person.getPrimaryRole(), true)) {
                    optionComp.addValue(equipmentType.getName());
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
        } else if (OptionsConstants.MISC_ENV_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.ENVSPC_NONE);
            optionComp.addValue(Crew.ENVSPC_FOG);
            optionComp.addValue(Crew.ENVSPC_LIGHT);
            optionComp.addValue(Crew.ENVSPC_RAIN);
            optionComp.addValue(Crew.ENVSPC_SNOW);
            optionComp.addValue(Crew.ENVSPC_WIND);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) {
            optionComp.addValue(Crew.HUMANTRO_NONE);
            optionComp.addValue(Crew.HUMANTRO_MEK);
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
        optionsPanel.add(optionComp);
        optionComps.add(optionComp);
    }

    private void setOptions() {
        IOption option;
        for (final DialogOptionComponent newVar : optionComps) {
            option = newVar.getOption();
            if ((newVar.getValue().equals("None"))) {
                person.getOptions().getOption(option.getName()).setValue("None");
            } else {
                person.getOptions().getOption(option.getName()).setValue(newVar.getValue());
            }
        }
    }

    private void changeSkillValue(String type) {
        if (!skillChecks.get(type).isSelected()) {
            skillValues.get(type).setText("-");
            return;
        }
        SkillType skillType = SkillType.getType(type);

        int level = (Integer) skillLevels.get(type).getModel().getValue();
        int bonus = (Integer) skillBonus.get(type).getModel().getValue();

        if (skillType.countUp()) {
            int target = min(getCountUpMaxValue(), skillType.getTarget() + level + bonus);
            skillValues.get(type).setText("+" + target);
        } else {
            int target = max(getCountDownMaxValue(), skillType.getTarget() - level - bonus);
            skillValues.get(type).setText(target + "+");
        }
    }

    private void changeValueEnabled(String type) {
        skillLevels.get(type).setEnabled(skillChecks.get(type).isSelected());
        skillBonus.get(type).setEnabled(skillChecks.get(type).isSelected());
    }

    private void btnDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, birthdate);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            birthdate = dc.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(birthdate));
            lblAge.setText(getAge() + " " + resourceMap.getString("age"));
        }
    }

    private void btnServiceDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, recruitment);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            recruitment = dc.getDate();
            btnServiceDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(recruitment));
        }
    }

    private void btnRankDateActionPerformed() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, lastRankChangeDate);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            lastRankChangeDate = dc.getDate();
            btnRankDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(lastRankChangeDate));
        }
    }

    private void btnRetirementDateActionPerformed() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, retirement);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            retirement = dc.getDate();
            btnRetirementDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(retirement));
        }
    }

    public int getAge() {
        // Get age based on year
        return Period.between(birthdate, campaign.getLocalDate()).getYears();
    }

    private void backgroundChanged() {
        final Phenotype newPhenotype = (Phenotype) choicePhenotype.getSelectedItem();
        if ((chkClan.isSelected()) || (Objects.requireNonNull(newPhenotype).isNone())) {
            if ((newPhenotype != null) && (newPhenotype != selectedPhenotype)) {
                switch (selectedPhenotype) {
                    case MEKWARRIOR:
                        decreasePhenotypeBonus(SkillType.S_GUN_MEK);
                        decreasePhenotypeBonus(SkillType.S_PILOT_MEK);
                        break;
                    case ELEMENTAL:
                        decreasePhenotypeBonus(SkillType.S_GUN_BA);
                        decreasePhenotypeBonus(SkillType.S_ANTI_MEK);
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
                    case PROTOMEK:
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
                    case MEKWARRIOR:
                        increasePhenotypeBonus(SkillType.S_GUN_MEK);
                        increasePhenotypeBonus(SkillType.S_PILOT_MEK);
                        break;
                    case ELEMENTAL:
                        increasePhenotypeBonus(SkillType.S_GUN_BA);
                        increasePhenotypeBonus(SkillType.S_ANTI_MEK);
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
                    case PROTOMEK:
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

    }

    @Override
    public void optionSwitched(DialogOptionComponent comp, IOption option, int i) {

    }
}
