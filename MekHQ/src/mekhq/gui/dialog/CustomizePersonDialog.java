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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.personnel.Person.*;
import static mekhq.campaign.personnel.skills.Aging.getAgeModifier;
import static mekhq.campaign.personnel.skills.Aging.getMilestone;
import static mekhq.campaign.personnel.skills.Aging.updateAllSkillAgeModifiers;
import static mekhq.campaign.personnel.skills.Skill.getCountDownMaxValue;
import static mekhq.campaign.personnel.skills.Skill.getCountUpMaxValue;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.writeInterviewersNotes;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.writePersonalityDescription;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.personalityQuirksSortedAlphabetically;

import java.awt.Component;
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
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.TechConstants;
import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.AgingMilestone;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.control.EditKillLogControl;
import mekhq.gui.control.EditLogControl;
import mekhq.gui.control.EditLogControl.LogType;
import mekhq.gui.control.EditScenarioLogControl;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MarkdownEditorPanel;

/**
 * This dialog is used to both hire new pilots and to edit existing ones
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class CustomizePersonDialog extends JDialog implements DialogOptionListener {
    private static final MMLogger LOGGER = MMLogger.create(CustomizePersonDialog.class);

    // region Variable declarations
    private final Person person;
    private List<DialogOptionComponentYPanel> optionComps = new ArrayList<>();
    private final Map<String, JSpinner> skillLevels = new Hashtable<>();
    private final Map<String, JSpinner> skillBonus = new Hashtable<>();
    private final Map<String, JLabel> skillValues = new Hashtable<>();
    private final Map<String, JLabel> skillAgeModifiers = new Hashtable<>();
    private final Map<String, JCheckBox> skillChecks = new Hashtable<>();
    private PersonnelOptions options;
    private LocalDate birthdate;
    private LocalDate recruitment;
    private LocalDate lastRankChangeDate;
    private LocalDate retirement;
    private final JFrame frame;

    private JButton btnDate;
    private JButton btnServiceDate;
    private JButton btnRankDate;
    private JButton btnRetirementDate;
    private JComboBox<Gender> choiceGender;
    private JLabel lblAge;
    private AbstractMHQScrollablePanel skillsPanel;
    private AbstractMHQScrollablePanel optionsPanel;
    private JTextField textToughness;
    private JTextField textConnections;
    private JTextField textWealth;
    private JTextField textReputation;
    private JTextField textUnlucky;
    private JTextField textBloodmark;
    private JTextField textExtraIncome;
    private JTextField textFatigue;
    private JComboBox<EducationLevel> textEducationLevel;
    private JTextField textLoyalty;
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

    // random personality
    private MMComboBox<Aggression> comboAggression;
    private JSpinner spnAggression;
    private MMComboBox<Ambition> comboAmbition;
    private JSpinner spnAmbition;
    private MMComboBox<Greed> comboGreed;
    private JSpinner spnGreed;
    private MMComboBox<Social> comboSocial;
    private JSpinner spnSocial;
    private MMComboBox<PersonalityQuirk> comboPersonalityQuirk;
    private JSpinner spnPersonalityQuirk;
    private MMComboBox<Reasoning> comboReasoning;

    // Other
    private JCheckBox chkDarkSecretRevealed;

    private final Campaign campaign;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.CustomizePersonDialog",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable declarations

    /** Creates new form CustomizePilotDialog */
    public CustomizePersonDialog(JFrame parent, boolean modal, Person person, Campaign campaign) {
        super(parent, modal);
        this.campaign = campaign;
        this.frame = parent;
        this.person = person;
        if (campaign.getCampaignOptions().isUseAgeEffects()) {
            updateAllSkillAgeModifiers(campaign.getLocalDate(), person);
        }
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
        options = person.getOptions();
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        setMinimumSize(UIUtil.scaleForGUI(1100, 500));

        JPanel panDemographics = new JPanel(new GridBagLayout());
        JTabbedPane tabStats = new JTabbedPane();
        JLabel lblName = new JLabel();
        JLabel lblGender = new JLabel();
        JLabel lblBirthday = new JLabel();
        JLabel lblRecruitment = new JLabel();
        lblAge = new JLabel();
        JLabel lblNickname = new JLabel();
        JLabel lblBloodname = new JLabel();
        JPanel panName = new JPanel(new GridBagLayout());
        textNickname = new JTextField();
        textBloodname = new JTextField();
        textToughness = new JTextField();
        JLabel lblFatigue = new JLabel();
        textConnections = new JTextField();
        JLabel lblConnections = new JLabel();
        textWealth = new JTextField();
        JLabel lblWealth = new JLabel();
        textReputation = new JTextField();
        JLabel lblReputation = new JLabel();
        textUnlucky = new JTextField();
        JLabel lblUnlucky = new JLabel();
        textBloodmark = new JTextField();
        JLabel lblBloodmark = new JLabel();
        textExtraIncome = new JTextField();
        JLabel lblExtraIncome = new JLabel();
        textFatigue = new JTextField();
        JLabel lblLoyalty = new JLabel();
        textLoyalty = new JTextField();
        JLabel lblToughness = new JLabel();
        textEducationLevel = new JComboBox<>();
        JLabel lblEducationLevel = new JLabel();
        JScrollPane scrOptions = new JScrollPaneWithSpeed();
        JScrollPane scrSkills = new JScrollPaneWithSpeed();
        JPanel panButtons = new JPanel();
        JButton btnOk = new JButton();

        JButton btnClose = new JButton();
        JButton btnRandomName = new JButton();
        JButton btnRandomBloodname = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title"));

        setName("Form");
        getContentPane().setLayout(new GridBagLayout());

        int y = 1;

        lblName.setText(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblName, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        textPreNominal = new JTextField(person.getPreNominal());
        textPreNominal.setName("textPreNominal");
        textPreNominal.setMinimumSize(UIUtil.scaleForGUI(50, 28));
        textPreNominal.setPreferredSize(UIUtil.scaleForGUI(50, 28));
        panName.add(textPreNominal, gridBagConstraints);

        textGivenName = new JTextField(person.getGivenName());
        textGivenName.setName("textGivenName");
        textGivenName.setMinimumSize(UIUtil.scaleForGUI(100, 28));
        textGivenName.setPreferredSize(UIUtil.scaleForGUI(100, 28));
        gridBagConstraints.gridx = 2;
        panName.add(textGivenName, gridBagConstraints);

        textSurname = new JTextField(person.getSurname());
        textSurname.setName("textSurname");
        textSurname.setMinimumSize(UIUtil.scaleForGUI(100, 28));
        textSurname.setPreferredSize(UIUtil.scaleForGUI(100, 28));
        gridBagConstraints.gridx = 3;
        panName.add(textSurname, gridBagConstraints);

        textPostNominal = new JTextField(person.getPostNominal());
        textPostNominal.setName("textPostNominal");
        textPostNominal.setMinimumSize(UIUtil.scaleForGUI(50, 28));
        textPostNominal.setPreferredSize(UIUtil.scaleForGUI(50, 28));
        gridBagConstraints.gridx = 4;
        panName.add(textPostNominal, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panDemographics.add(panName, gridBagConstraints);

        btnRandomName.setText(resourceMap.getString("btnRandomName.text"));
        btnRandomName.setName("btnRandomName");
        btnRandomName.addActionListener(evt -> randomName());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panDemographics.add(btnRandomName, gridBagConstraints);

        y++;

        if (person.isClanPersonnel()) {
            lblBloodname.setText(resourceMap.getString("lblBloodname.text"));
            lblBloodname.setName("lblBloodname");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblBloodname, gridBagConstraints);

            textBloodname.setMinimumSize(UIUtil.scaleForGUI(150, 28));
            textBloodname.setName("textBloodname");
            textBloodname.setPreferredSize(UIUtil.scaleForGUI(150, 28));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            textBloodname.setText(person.getBloodname());
            panDemographics.add(textBloodname, gridBagConstraints);

            btnRandomBloodname.setText(resourceMap.getString("btnRandomBloodname.text"));
            btnRandomBloodname.setName("btnRandomBloodname");
            btnRandomBloodname.addActionListener(evt -> randomBloodname());
            gridBagConstraints.gridx = 2;
            panDemographics.add(btnRandomBloodname, gridBagConstraints);
        } else {
            lblNickname.setText(resourceMap.getString("lblNickname.text"));
            lblNickname.setName("lblNickname");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblNickname, gridBagConstraints);

            textNickname.setText(person.getCallsign());
            textNickname.setName("textNickname");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            panDemographics.add(textNickname, gridBagConstraints);

            JButton btnRandomCallsign = new JButton(resourceMap.getString("btnRandomCallsign.text"));
            btnRandomCallsign.setName("btnRandomCallsign");
            btnRandomCallsign.addActionListener(e -> textNickname.setText(RandomCallsignGenerator.getInstance()
                                                                                .generate()));
            gridBagConstraints.gridx = 2;
            panDemographics.add(btnRandomCallsign, gridBagConstraints);
        }

        y++;

        lblGender.setText(resourceMap.getString("lblGender.text"));
        lblGender.setName("lblGender");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblGender, gridBagConstraints);

        choiceGender = new JComboBox<>(Gender.values());
        choiceGender.setName("choiceGender");
        choiceGender.setSelectedItem(person.getGender());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(choiceGender, gridBagConstraints);

        y++;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(new JLabel("Origin Faction:"), gridBagConstraints);

        DefaultComboBoxModel<Faction> factionsModel = getFactionsComboBoxModel();
        choiceFaction = new JComboBox<>(factionsModel);
        choiceFaction.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(choiceFaction, gridBagConstraints);

        y++;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(new JLabel("Origin System:"), gridBagConstraints);

        DefaultComboBoxModel<Planet> planetsModel = new DefaultComboBoxModel<>();
        choicePlanet = new JComboBox<>(planetsModel);

        allSystems = getPlanetarySystemsComboBoxModel();
        choiceSystem = new JComboBox<>(allSystems);
        choiceSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
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

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(choiceSystem, gridBagConstraints);

        chkOnlyOurFaction = new JCheckBox("Faction Specific");
        chkOnlyOurFaction.addActionListener(e -> filterPlanetarySystemsForOurFaction(chkOnlyOurFaction.isSelected()));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(chkOnlyOurFaction, gridBagConstraints);

        y++;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(new JLabel("Origin Planet:"), gridBagConstraints);

        choicePlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
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

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(choicePlanet, gridBagConstraints);

        y++;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(new JLabel("Phenotype:"), gridBagConstraints);

        DefaultComboBoxModel<Phenotype> phenotypeModel = new DefaultComboBoxModel<>();
        phenotypeModel.addElement(Phenotype.NONE);
        for (Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            phenotypeModel.addElement(phenotype);
        }
        choicePhenotype = new JComboBox<>(phenotypeModel);
        choicePhenotype.setSelectedItem(selectedPhenotype);
        choicePhenotype.addActionListener(evt -> backgroundChanged());
        choicePhenotype.setEnabled(person.isClanPersonnel());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(choicePhenotype, gridBagConstraints);

        chkClan = new JCheckBox("Clan Personnel");
        chkClan.setSelected(person.isClanPersonnel());
        chkClan.addItemListener(et -> backgroundChanged());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panDemographics.add(chkClan, gridBagConstraints);

        y++;

        lblBirthday.setText(resourceMap.getString("lblBday.text"));
        lblBirthday.setName("lblBirthday");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblBirthday, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(birthdate));
        btnDate.setName("btnDate");
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panDemographics.add(btnDate, gridBagConstraints);

        lblAge.setText(person.getAge(campaign.getLocalDate()) + " " + resourceMap.getString("age"));
        lblAge.setName("lblAge");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblAge, gridBagConstraints);

        y++;

        if (campaign.getCampaignOptions().isUseTimeInService() && (recruitment != null)) {
            lblRecruitment.setText(resourceMap.getString("lblRecruitment.text"));
            lblRecruitment.setName("lblRecruitment");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblRecruitment, gridBagConstraints);

            btnServiceDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(recruitment));
            btnServiceDate.setName("btnServiceDate");
            btnServiceDate.addActionListener(this::btnServiceDateActionPerformed);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemographics.add(btnServiceDate, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().isUseTimeInRank() && (lastRankChangeDate != null)) {
            JLabel lblLastRankChangeDate = new JLabel(resourceMap.getString("lblLastRankChangeDate.text"));
            lblLastRankChangeDate.setName("lblLastRankChangeDate");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblLastRankChangeDate, gridBagConstraints);

            btnRankDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(lastRankChangeDate));
            btnRankDate.setName("btnRankDate");
            btnRankDate.addActionListener(e -> btnRankDateActionPerformed());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemographics.add(btnRankDate, gridBagConstraints);

            y++;
        }

        if (retirement != null) {
            JLabel lblRetirement = new JLabel(resourceMap.getString("lblRetirement.text"));
            lblRetirement.setName("lblRetirement");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblRetirement, gridBagConstraints);

            btnRetirementDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(retirement));
            btnRetirementDate.setName("btnRetirementDate");
            btnRetirementDate.addActionListener(e -> btnRetirementDateActionPerformed());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemographics.add(btnRetirementDate, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().isUseToughness()) {
            lblToughness.setText(resourceMap.getString("lblToughness.text"));
            lblToughness.setName("lblToughness");

            textToughness.setText(Integer.toString(person.getToughness()));
            textToughness.setName("textToughness");

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblToughness, gridBagConstraints);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            panDemographics.add(textToughness, gridBagConstraints);

            y++;
        }

        lblConnections.setText(resourceMap.getString("lblConnections.text"));
        lblConnections.setName("lblConnections");

        textConnections.setText(Integer.toString(person.getConnections()));
        textConnections.setName("textConnections");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblConnections, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textConnections, gridBagConstraints);

        y++;

        lblWealth.setText(resourceMap.getString("lblWealth.text"));
        lblWealth.setName("lblWealth");

        textWealth.setText(Integer.toString(person.getWealth()));
        textWealth.setName("textWealth");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblWealth, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textWealth, gridBagConstraints);

        y++;

        lblReputation.setText(resourceMap.getString("lblReputation.text"));
        lblReputation.setName("lblReputation");

        textReputation.setText(Integer.toString(person.getReputation()));
        textReputation.setName("textReputation");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblReputation, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textReputation, gridBagConstraints);

        y++;

        lblUnlucky.setText(resourceMap.getString("lblUnlucky.text"));
        lblUnlucky.setName("lblUnlucky");

        textUnlucky.setText(Integer.toString(person.getUnlucky()));
        textUnlucky.setName("textUnlucky");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblUnlucky, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textUnlucky, gridBagConstraints);

        y++;

        lblBloodmark.setText(resourceMap.getString("lblBloodmark.text"));
        lblBloodmark.setName("lblBloodmark");

        textBloodmark.setText(Integer.toString(person.getBloodmark()));
        textBloodmark.setName("textBloodmark");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblBloodmark, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textBloodmark, gridBagConstraints);

        y++;

        lblExtraIncome.setText(resourceMap.getString("lblExtraIncome.text"));
        lblExtraIncome.setName("lblExtraIncome");

        textExtraIncome.setText(Integer.toString(person.getExtraIncomeTraitLevel()));
        textExtraIncome.setName("textExtraIncome");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblExtraIncome, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panDemographics.add(textExtraIncome, gridBagConstraints);

        y++;

        if (campaign.getCampaignOptions().isUseFatigue()) {
            lblFatigue.setText(resourceMap.getString("lblFatigue.text"));
            lblFatigue.setName("lblFatigue");

            textFatigue.setText(Integer.toString(person.getFatigue()));
            textFatigue.setName("textFatigue");

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblFatigue, gridBagConstraints);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            panDemographics.add(textFatigue, gridBagConstraints);

            y++;
        }

        if (campaign.getCampaignOptions().isUseEducationModule()) {
            lblEducationLevel.setText(resourceMap.getString("lblEducationLevel.text"));
            lblEducationLevel.setName("lblEducationLevel");

            for (EducationLevel level : EducationLevel.values()) {
                textEducationLevel.addItem(level);
            }
            textEducationLevel.setSelectedItem(person.getEduHighestEducation());
            textEducationLevel.setName("textEducationLevel");

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblEducationLevel, gridBagConstraints);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            panDemographics.add(textEducationLevel, gridBagConstraints);

            y++;
        }

        if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) &&
                  (!campaign.getCampaignOptions().isUseHideLoyalty())) {
            lblLoyalty.setText(resourceMap.getString("lblLoyalty.text"));
            lblLoyalty.setName("lblLoyalty");

            textLoyalty.setText(Integer.toString(person.getBaseLoyalty()));
            textLoyalty.setName("textLoyalty");

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(lblLoyalty, gridBagConstraints);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            panDemographics.add(textLoyalty, gridBagConstraints);

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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
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
                Object object = choiceOriginalUnit.getSelectedItem();
                if (object instanceof Unit unit) {
                    choiceUnitWeight.setSelectedIndex(unit.getEntity().getWeightClass());
                    if (unit.getEntity().isClan()) {
                        choiceUnitTech.setSelectedIndex(2);
                    } else if (unit.getEntity().getTechLevel() > TechConstants.T_INTRO_BOX_SET) {
                        choiceUnitTech.setSelectedIndex(1);
                    } else {
                        choiceUnitTech.setSelectedIndex(0);
                    }
                } else {
                    choiceUnitWeight.setSelectedIndex(person.getOriginalUnitWeight());
                    choiceUnitTech.setSelectedIndex(person.getOriginalUnitTech());
                }
            } catch (Exception e) {
                choiceUnitWeight.setSelectedIndex(person.getOriginalUnitWeight());
                choiceUnitTech.setSelectedIndex(person.getOriginalUnitTech());
            }
        });

        y++;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(lblUnit, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panDemographics.add(choiceUnitWeight, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panDemographics.add(choiceUnitTech, gridBagConstraints);

        y++;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(choiceOriginalUnit, gridBagConstraints);

        y++;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        panDemographics.add(chkFounder, gridBagConstraints);

        if (campaign.getCampaignOptions().isUseShareSystem()) {
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panDemographics.add(lblShares, gridBagConstraints);
        }

        y++;

        // region random personality
        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            JLabel labelAggression = new JLabel();
            labelAggression.setText("Aggression:");
            labelAggression.setName("labelAggression");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelAggression, gridBagConstraints);

            comboAggression = new MMComboBox<>("comboAggression", Aggression.values());
            comboAggression.setSelectedItem(person.getAggression());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboAggression, gridBagConstraints);

            spnAggression = new JSpinner(new SpinnerNumberModel(person.getAggressionDescriptionIndex(),
                  0, Aggression.MAXIMUM_VARIATIONS, 1));

            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(spnAggression, gridBagConstraints);

            JLabel labelAmbition = new JLabel();
            labelAmbition.setText("Ambition:");
            labelAmbition.setName("labelAmbition");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelAmbition, gridBagConstraints);

            comboAmbition = new MMComboBox<>("comboAmbition", Ambition.values());
            comboAmbition.setSelectedItem(person.getAmbition());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboAmbition, gridBagConstraints);

            spnAmbition = new JSpinner(new SpinnerNumberModel(person.getAmbitionDescriptionIndex(),
                  0, Ambition.MAXIMUM_VARIATIONS, 1));

            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(spnAmbition, gridBagConstraints);

            JLabel labelGreed = new JLabel();
            labelGreed.setText("Greed:");
            labelGreed.setName("labelGreed");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelGreed, gridBagConstraints);

            comboGreed = new MMComboBox<>("comboGreed", Greed.values());
            comboGreed.setSelectedItem(person.getGreed());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboGreed, gridBagConstraints);

            spnGreed = new JSpinner(new SpinnerNumberModel(person.getGreedDescriptionIndex(),
                  0, Greed.MAXIMUM_VARIATIONS, 1));

            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(spnGreed, gridBagConstraints);

            JLabel labelSocial = new JLabel();
            labelSocial.setText("Social:");
            labelSocial.setName("labelSocial");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelSocial, gridBagConstraints);

            comboSocial = new MMComboBox<>("comboSocial", Social.values());
            comboSocial.setSelectedItem(person.getSocial());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboSocial, gridBagConstraints);

            spnSocial = new JSpinner(new SpinnerNumberModel(person.getSocialDescriptionIndex(),
                  0, Social.MAXIMUM_VARIATIONS, 1));

            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(spnSocial, gridBagConstraints);

            JLabel labelPersonalityQuirk = new JLabel();
            labelPersonalityQuirk.setText("Quirk:");
            labelPersonalityQuirk.setName("labelPersonalityQuirk");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelPersonalityQuirk, gridBagConstraints);

            comboPersonalityQuirk = new MMComboBox<>("comboPersonalityQuirk", personalityQuirksSortedAlphabetically());
            comboPersonalityQuirk.setSelectedItem(person.getPersonalityQuirk());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboPersonalityQuirk, gridBagConstraints);

            spnPersonalityQuirk = new JSpinner(new SpinnerNumberModel(person.getPersonalityQuirkDescriptionIndex(),
                  0, PersonalityQuirk.MAXIMUM_VARIATIONS, 1));

            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(spnPersonalityQuirk, gridBagConstraints);

            y++;

            JLabel labelReasoning = new JLabel();
            labelReasoning.setText("Reasoning:");
            labelReasoning.setName("labelReasoning");

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(labelReasoning, gridBagConstraints);

            comboReasoning = new MMComboBox<>("comboReasoning", Reasoning.values());
            comboReasoning.setSelectedItem(person.getReasoning());

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            panDemographics.add(comboReasoning, gridBagConstraints);

            y++;
        }

        if (person.hasDarkSecret()) {
            chkDarkSecretRevealed = new JCheckBox("Dark Secret Revealed");
            chkDarkSecretRevealed.setSelected(person.isDarkSecretRevealed());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            panDemographics.add(chkDarkSecretRevealed, gridBagConstraints);

            y++;
        }

        txtBio = new MarkdownEditorPanel("Biography");
        txtBio.setMinimumSize(UIUtil.scaleForGUI(400, 200));
        txtBio.setText(person.getBiography());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panDemographics.add(txtBio, gridBagConstraints);

        JScrollPane scrollPane = new JScrollPane(panDemographics);
        scrollPane.setMinimumSize(UIUtil.scaleForGUI(600, 500));
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(600, 500));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(scrollPane, gridBagConstraints);

        skillsPanel = new DefaultMHQScrollablePanel(frame, "skillsPanel");
        refreshSkills();
        scrSkills.setViewportView(skillsPanel);
        scrSkills.setMinimumSize(UIUtil.scaleForGUI(500, 500));
        scrSkills.setPreferredSize(UIUtil.scaleForGUI(500, 500));

        optionsPanel = new DefaultMHQScrollablePanel(frame, "optionsPanel");
        refreshOptions();
        scrOptions.setViewportView(optionsPanel);
        scrOptions.setMinimumSize(UIUtil.scaleForGUI(500, 500));
        scrOptions.setPreferredSize(UIUtil.scaleForGUI(500, 500));

        tabStats.addTab(resourceMap.getString("scrSkills.TabConstraints.tabTitle"), scrSkills);
        if (campaign.getCampaignOptions().isUseAbilities() ||
                  campaign.getCampaignOptions().isUseEdge() ||
                  campaign.getCampaignOptions().isUseImplants()) {
            tabStats.addTab(resourceMap.getString("scrOptions.TabConstraints.tabTitle"), scrOptions);
        }
        tabStats.add(resourceMap.getString("panLog.TabConstraints.tabTitle"),
              new EditLogControl(frame, person, campaign.getLocalDate(), LogType.PERSONAL_LOG));
        tabStats.add(resourceMap.getString("panScenarios.title"), new EditScenarioLogControl(frame, campaign, person));
        tabStats.add(resourceMap.getString("panKills.TabConstraints.tabTitle"),
              new EditKillLogControl(frame, campaign, person));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabStats, gridBagConstraints);

        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());

        btnOk.setText(resourceMap.getString("btnOk.text"));
        btnOk.setName("btnOk");
        btnOk.addActionListener(this::btnOkActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        panButtons.add(btnOk, gridBagConstraints);
        gridBagConstraints.gridx++;

        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
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
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CustomizePersonDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
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
                if (faction.is(FactionTag.HIDDEN) || faction.is(FactionTag.SPECIAL)) {
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
        Planet selectedPlanet = (Planet) choicePlanet.getSelectedItem();
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

    private void updatePlanetsComboBoxModel(DefaultComboBoxModel<Planet> planetsModel,
          PlanetarySystem planetarySystem) {
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
        if (campaign.getCampaignOptions().isUseAgeEffects()) {
            updateAllSkillAgeModifiers(campaign.getLocalDate(), person);
        }
        if (person.isEmployed()) {
            LocalDate joinedDate = person.getJoinedCampaign();

            if (recruitment != null) {
                if (joinedDate == null || recruitment.isBefore(joinedDate)) {
                    person.setJoinedCampaign(recruitment);
                }
            } else {
                person.setRecruitment(null);
            }
        }
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
            int currentValue = person.getToughness();
            person.setToughness(MathUtility.parseInt(textToughness.getText(), currentValue));
        }

        int currentValue = person.getConnections();
        int newValue = MathUtility.parseInt(textConnections.getText(), currentValue);
        person.setConnections(clamp(newValue, MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS));

        currentValue = person.getWealth();
        newValue = MathUtility.parseInt(textWealth.getText(), currentValue);
        person.setWealth(clamp(newValue, MINIMUM_WEALTH, MAXIMUM_WEALTH));

        currentValue = person.getReputation();
        newValue = MathUtility.parseInt(textReputation.getText(), currentValue);
        person.setReputation(clamp(newValue, MINIMUM_REPUTATION, MAXIMUM_REPUTATION));

        currentValue = person.getUnlucky();
        newValue = MathUtility.parseInt(textUnlucky.getText(), currentValue);
        person.setUnlucky(clamp(newValue, MINIMUM_UNLUCKY, MAXIMUM_UNLUCKY));

        currentValue = person.getBloodmark();
        newValue = MathUtility.parseInt(textBloodmark.getText(), currentValue);
        person.setBloodmark(clamp(newValue, MINIMUM_BLOODMARK, MAXIMUM_BLOODMARK));

        currentValue = person.getExtraIncomeTraitLevel();
        newValue = MathUtility.parseInt(textExtraIncome.getText(), currentValue);
        person.setExtraIncomeFromTraitLevel(clamp(newValue, MINIMUM_EXTRA_INCOME, MAXIMUM_EXTRA_INCOME));

        if (campaign.getCampaignOptions().isUseEducationModule()) {
            person.setEduHighestEducation((EducationLevel) textEducationLevel.getSelectedItem());
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            currentValue = person.getBaseLoyalty();
            person.setLoyalty(MathUtility.parseInt(textLoyalty.getText(), currentValue));
        }

        currentValue = person.getFatigue();
        person.setFatigue(MathUtility.parseInt(textFatigue.getText(), currentValue));

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
            person.setAggressionDescriptionIndex((int) spnAggression.getValue());

            person.setAmbition(comboAmbition.getSelectedItem());
            person.setAmbitionDescriptionIndex((int) spnAmbition.getValue());

            person.setGreed(comboGreed.getSelectedItem());
            person.setGreedDescriptionIndex((int) spnGreed.getValue());

            person.setSocial(comboSocial.getSelectedItem());
            person.setSocialDescriptionIndex((int) spnSocial.getValue());

            person.setPersonalityQuirk(comboPersonalityQuirk.getSelectedItem());
            person.setPersonalityQuirkDescriptionIndex((int) spnPersonalityQuirk.getValue());

            person.setReasoning(comboReasoning.getSelectedItem());

            writePersonalityDescription(person);
            writeInterviewersNotes(person);
        }

        if (person.hasDarkSecret()) {
            boolean darkSecretRevealed = chkDarkSecretRevealed.isSelected();
            if (darkSecretRevealed != person.isDarkSecretRevealed()) {
                if (!darkSecretRevealed) {
                    person.setDarkSecretRevealed(false);
                } else {
                    String report = person.isDarkSecretRevealed(true, true);
                    if (!report.isBlank()) {
                        campaign.addReport(report);
                    }
                }
            }
        }

        setSkills();
        setOptions();

        person.validateRoles(campaign);

        dispose();
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
        JLabel lblName;
        JLabel lblValue;
        JLabel lblLevel;
        JLabel lblBonus;
        JLabel lblAging;
        JSpinner spnLevel;
        JSpinner spnBonus;

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        skillsPanel.setLayout(gridBag);

        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 0, 0);
        constraints.gridx = 0;

        AgingMilestone milestone = getMilestone(person.getAge(campaign.getLocalDate()));

        List<String> sortedSkillNames = getSortedSkills();
        for (int index = 0; index < sortedSkillNames.size(); index++) {
            constraints.gridy = index;
            constraints.gridx = 0;
            final String type = sortedSkillNames.get(index);
            chkSkill = new JCheckBox();
            chkSkill.setSelected(person.hasSkill(type));
            skillChecks.put(type, chkSkill);
            chkSkill.addItemListener(e -> {
                changeSkillValue(type);
                changeValueEnabled(type);
            });
            lblName = new JLabel(type);
            lblValue = new JLabel();
            if (person.hasSkill(type)) {
                lblValue.setText(person.getSkill(type)
                                       .toString(person.getOptions(),
                                             person.getATOWAttributes(),
                                             person.getReputation()));
            } else {
                lblValue.setText("-");
            }
            skillValues.put(type, lblValue);

            lblLevel = new JLabel(resourceMap.getString("lblLevel.text"));
            lblBonus = new JLabel(resourceMap.getString("lblBonus.text"));
            int level = 0;
            int bonus = 0;
            if (person.hasSkill(type)) {
                Skill skill = person.getSkill(type);
                // We had errors where player modified their skills beyond these values which then caused the
                // JSpinners to break. This code here ensures that we self correct the values.
                level = clamp(skill.getLevel(), 0, 10);
                bonus = clamp(skill.getBonus(), -8, 8);
            }
            spnLevel = new JSpinner(new SpinnerNumberModel(level, 0, 10, 1));
            spnLevel.addChangeListener(evt -> changeSkillValue(type));
            spnLevel.setEnabled(chkSkill.isSelected());
            spnBonus = new JSpinner(new SpinnerNumberModel(clamp(bonus, -8, 8), -8, 8, 1));
            spnBonus.addChangeListener(evt -> changeSkillValue(type));
            spnBonus.setEnabled(chkSkill.isSelected());
            skillLevels.put(type, spnLevel);
            skillBonus.put(type, spnBonus);

            SkillType skillType = SkillType.getType(type);
            lblAging = new JLabel(resourceMap.getString("lblAging.text"));
            int ageModifier = getAgeModifier(milestone, skillType.getFirstAttribute(), skillType.getSecondAttribute());
            skillAgeModifiers.put(type, new JLabel(ageModifier + ""));

            constraints.anchor = GridBagConstraints.WEST;
            constraints.weightx = 0;
            skillsPanel.add(chkSkill, constraints);

            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblName, constraints);

            constraints.gridx = 2;
            constraints.anchor = GridBagConstraints.CENTER;
            skillsPanel.add(lblValue, constraints);

            constraints.gridx = 3;
            constraints.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblLevel, constraints);

            constraints.gridx = 4;
            constraints.anchor = GridBagConstraints.WEST;
            skillsPanel.add(spnLevel, constraints);

            constraints.gridx = 5;
            constraints.anchor = GridBagConstraints.WEST;
            skillsPanel.add(lblBonus, constraints);

            constraints.gridx = 6;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.weightx = 1.0;
            skillsPanel.add(spnBonus, constraints);

            if (campaign.getCampaignOptions().isUseAgeEffects()) {
                constraints.gridx = 7;
                constraints.anchor = GridBagConstraints.WEST;
                skillsPanel.add(lblAging, constraints);

                constraints.gridx = 8;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.weightx = 1.0;
                skillsPanel.add(skillAgeModifiers.get(type), constraints);
            }
        }
    }

    /**
     * Returns a list of skill names where the person’s owned skills are listed first in alphabetical order, followed by
     * any remaining skills, also in alphabetical order.
     *
     * <p>This method sorts the owned skill names and places them at the beginning of the returned list. It then
     * appends any skill names that are not owned by the person.</p>
     *
     * @return a {@code List<String>} of skill names, with owned skills first and all others following
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<String> getSortedSkills() {
        List<String> sortedSkillNames = SkillType.getSortedSkillNames();

        List<String> ownedSkills = person.getSkills().getSkills().stream()
                                         .map(Skill::getType)
                                         .filter(Objects::nonNull)
                                         .map(SkillType::getName).distinct().sorted().toList();

        List<String> remainingSkills = new ArrayList<>(sortedSkillNames);
        remainingSkills.removeAll(ownedSkills);

        List<String> allSkillsOrdered = new ArrayList<>();
        allSkillsOrdered.addAll(ownedSkills);
        allSkillsOrdered.addAll(remainingSkills);

        return allSkillsOrdered;
    }

    private void setSkills() {
        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            final String type = SkillType.getSkillList()[i];
            AgingMilestone milestone = getMilestone(person.getAge(campaign.getLocalDate()));
            if (skillChecks.get(type).isSelected()) {
                int level = (Integer) skillLevels.get(type).getModel().getValue();
                int bonus = (Integer) skillBonus.get(type).getModel().getValue();
                SkillType skillType = SkillType.getType(type);
                int ageModifier = 0;
                if (campaign.getCampaignOptions().isUseAgeEffects()) {
                    ageModifier = getAgeModifier(milestone,
                          skillType.getFirstAttribute(),
                          skillType.getSecondAttribute());
                }
                person.addSkill(type, level, bonus, ageModifier);
            } else {
                person.removeSkill(type);
            }
        }
        IOption option;
        for (final DialogOptionComponentYPanel newVar : optionComps) {
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

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        optionsPanel.setLayout(gridBag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

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
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, true);

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
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_NONE);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_FOG);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_LIGHT);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_RAIN);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_SNOW);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_WIND);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) {
            optionComp.addValue(Crew.HUMAN_TRO_NONE);
            optionComp.addValue(Crew.HUMAN_TRO_MEK);
            optionComp.addValue(Crew.HUMAN_TRO_AERO);
            optionComp.addValue(Crew.HUMAN_TRO_VEE);
            optionComp.addValue(Crew.HUMAN_TRO_BA);
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
        for (final DialogOptionComponentYPanel newVar : optionComps) {
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
        int ageModifier = 0;
        if (campaign.getCampaignOptions().isUseAgeEffects()) {
            ageModifier = getAgeModifier(getMilestone(person.getAge(campaign.getLocalDate())),
                  skillType.getFirstAttribute(),
                  skillType.getSecondAttribute());
        }

        if (skillType.isCountUp()) {
            int target = min(getCountUpMaxValue(), skillType.getTarget() + level + bonus + ageModifier);
            skillValues.get(type).setText("+" + target);
        } else {
            int target = max(getCountDownMaxValue(), skillType.getTarget() - level - bonus - ageModifier);
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
                        decreasePhenotypeBonus(SkillType.S_NAVIGATION);
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
                        increasePhenotypeBonus(SkillType.S_NAVIGATION);
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
    public void optionClicked(DialogOptionComponentYPanel arg0, IOption arg1, boolean arg2) {

    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel comp, IOption option, int i) {

    }
}
