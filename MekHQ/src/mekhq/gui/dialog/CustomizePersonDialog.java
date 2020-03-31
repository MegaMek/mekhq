/*
 * NewPilotDialog.java
 *
 * Created on July 16, 2009, 5:30 PM
 */
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import mekhq.campaign.personnel.enums.GenderDescriptors;
import org.joda.time.DateTime;

import megamek.client.ui.swing.DialogOptionComponent;
import megamek.client.ui.swing.DialogOptionListener;
import megamek.common.Crew;
import megamek.common.EquipmentType;
import megamek.common.WeaponType;
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
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.control.EditKillLogControl;
import mekhq.gui.control.EditMissionLogControl;
import mekhq.gui.control.EditPersonnelLogControl;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.preferences.PreferencesNode;

/**
 * This dialog is used to both hire new pilots and to edit existing ones
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CustomizePersonDialog extends javax.swing.JDialog implements DialogOptionListener {
	private static final long serialVersionUID = -6265589976779860566L;

	private Person person;
    private ArrayList<DialogOptionComponent> optionComps = new ArrayList<>();
    private Hashtable<String, JSpinner> skillLvls = new Hashtable<>();
    private Hashtable<String, JSpinner> skillBonus = new Hashtable<>();
    private Hashtable<String, JLabel> skillValues = new Hashtable<>();
    private Hashtable<String, JCheckBox> skillChks = new Hashtable<>();
    private PilotOptions options;
    private GregorianCalendar birthdate;
    private GregorianCalendar recruitment;
    private SimpleDateFormat dateFormat;
    private Frame frame;

    private javax.swing.JButton btnDate;
    private javax.swing.JButton btnServiceDate;
    private javax.swing.JComboBox<String> choiceGender;
    private javax.swing.JLabel lblAge;
    private javax.swing.JPanel panSkills;
    private javax.swing.JPanel panOptions;
    private javax.swing.JTextField textToughness;
    private javax.swing.JTextField textGivenName;
    private javax.swing.JTextField textSurname;
    private javax.swing.JTextField textHonorific;
    private javax.swing.JTextField textNickname;
    private javax.swing.JTextField textBloodname;
    private MarkdownEditorPanel txtBio;
    private JComboBox<Faction> choiceFaction;
    private JComboBox<PlanetarySystem> choiceSystem;
    private DefaultComboBoxModel<PlanetarySystem> allSystems;
    private JCheckBox chkOnlyOurFaction;
    private JComboBox<Planet> choicePlanet;
    private JCheckBox chkClan;
    private JComboBox<String> choicePheno;

    /* Against the Bot */
    private JComboBox<String> choiceUnitWeight;
    private JComboBox<String> choiceUnitTech;
    private JCheckBox chkFounder;
    private JComboBox<Unit> choiceOriginalUnit;

    private Campaign campaign;

    /** Creates new form CustomizePilotDialog */
    public CustomizePersonDialog(java.awt.Frame parent, boolean modal, Person person, Campaign campaign) {
        super(parent, modal);
        this.campaign = campaign;
        this.frame = parent;
        this.dateFormat = new SimpleDateFormat("MMMM d yyyy");
        this.person = person;
        initializePilotAndOptions();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initializePilotAndOptions () {
        this.birthdate = (GregorianCalendar) person.getBirthday().clone();
    	if (campaign.getCampaignOptions().getUseTimeInService() && (person.getRecruitment() != null)) {
            this.recruitment = (GregorianCalendar) person.getRecruitment().clone();
        }
    	this.options = person.getOptions();
    	initComponents();
    }

    @SuppressWarnings("serial")
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
        textGivenName = new javax.swing.JTextField();
        textSurname = new javax.swing.JTextField();
        textHonorific = new javax.swing.JTextField();
        textNickname = new javax.swing.JTextField();
        textBloodname = new javax.swing.JTextField();
        textToughness = new javax.swing.JTextField();
        JLabel lblToughness = new JLabel();
        choiceGender = new javax.swing.JComboBox<>();
        JScrollPane scrOptions = new JScrollPane();
        panOptions = new javax.swing.JPanel();
        JScrollPane scrSkills = new JScrollPane();
        panSkills = new javax.swing.JPanel();
        JPanel panButtons = new JPanel();
        JButton btnOk = new JButton();

        JButton btnClose = new JButton();
        JButton btnRandomName = new JButton();
        JButton btnRandomBloodname = new JButton();
        btnDate = new javax.swing.JButton();
        btnServiceDate = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title")); // NOI18N

        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        int y = 1;

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblName, gridBagConstraints);

        textGivenName.setMinimumSize(new java.awt.Dimension(100, 28));
        textGivenName.setName("textGivenName"); // NOI18N
        textGivenName.setPreferredSize(new java.awt.Dimension(100, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        textGivenName.setText(person.getGivenName());
        panName.add(textGivenName, gridBagConstraints);

        textSurname.setMinimumSize(new java.awt.Dimension(100, 28));
        textSurname.setName("textSurname"); // NOI18N
        textSurname.setPreferredSize(new java.awt.Dimension(100, 28));
        gridBagConstraints.gridx = 2;
        textSurname.setText(person.getSurname());
        panName.add(textSurname, gridBagConstraints);

        textHonorific.setMinimumSize(new java.awt.Dimension(50, 28));
        textHonorific.setName("textHonorific"); // NOI18N
        textHonorific.setPreferredSize(new java.awt.Dimension(50, 28));
        gridBagConstraints.gridx = 3;
        textHonorific.setText(person.getHonorific());
        panName.add(textHonorific, gridBagConstraints);

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

            textBloodname.setMinimumSize(new java.awt.Dimension(150, 28));
            textBloodname.setName("textBloodname"); // NOI18N
            textBloodname.setPreferredSize(new java.awt.Dimension(150, 28));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            textBloodname.setText(person.getBloodname());
            panDemog.add(textBloodname, gridBagConstraints);

            btnRandomBloodname.setText(resourceMap.getString("btnRandomBloodname.text")); // NOI18N
            btnRandomBloodname.setName("btnRandomBloodname"); // NOI18N
            btnRandomBloodname.addActionListener(evt -> randomBloodname());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            panDemog.add(btnRandomBloodname, gridBagConstraints);
        } else {
        	lblNickname.setText(resourceMap.getString("lblNickname.text")); // NOI18N
        	lblNickname.setName("lblNickname"); // NOI18N
        	gridBagConstraints = new java.awt.GridBagConstraints();
        	gridBagConstraints.gridx = 0;
        	gridBagConstraints.gridy = y;
        	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        	gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        	panDemog.add(lblNickname, gridBagConstraints);

        	textNickname.setText(person.getCallsign());
        	textNickname.setName("textNickname"); // NOI18N
        	gridBagConstraints = new java.awt.GridBagConstraints();
        	gridBagConstraints.gridx = 1;
        	gridBagConstraints.gridy = y;
        	gridBagConstraints.gridwidth = 1;
        	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        	panDemog.add(textNickname, gridBagConstraints);
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

        DefaultComboBoxModel<String> genderModel = new DefaultComboBoxModel<>();
        genderModel.addElement(Person.getGenderString(Crew.G_MALE, GenderDescriptors.MALE_FEMALE));
        genderModel.addElement(Person.getGenderString(Crew.G_FEMALE, GenderDescriptors.MALE_FEMALE));
        choiceGender.setModel(genderModel);
        choiceGender.setName("choiceGender"); // NOI18N
        choiceGender.setSelectedIndex(person.getGender());
        choiceGender.addActionListener(evt -> randomName());
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
                super.getListCellRendererComponent(list, value, index, isSelected,
                                                   cellHasFocus);
                if (value instanceof Faction) {
                    Faction faction = (Faction)value;
                    setText(String.format("%s [%s]",
                        faction.getFullName(person.getCampaign().getGameYear()),
                        faction.getShortName()));
                }

                return this;
            }
        });
        choiceFaction.setSelectedIndex(factionsModel.getIndexOf(person.getOriginFaction()));
        choiceFaction.addActionListener(evt -> {
            // Update the clan check box based on the new selected faction
            Faction selectedFaction = (Faction)choiceFaction.getSelectedItem();
            chkClan.setSelected(selectedFaction.is(Tag.CLAN));

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
                    PlanetarySystem system = (PlanetarySystem)value;
                    setText(system.getName(new DateTime(campaign.getCalendar())));
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
                    Planet planet = (Planet)value;
                    setText(planet.getName(new DateTime(campaign.getCalendar())));
                }

                return this;
            }
        });
        if (person.getOriginPlanet() != null) {
            choicePlanet.setSelectedIndex(planetsModel.getIndexOf(person.getOriginPlanet()));
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choicePlanet, gridBagConstraints);

        y++;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Phenotype:"), gridBagConstraints);

        DefaultComboBoxModel<String> phenoModel = new DefaultComboBoxModel<>();
        for(int i = 0; i < Person.PHENOTYPE_NUM; i++) {
            phenoModel.addElement(Person.getPhenotypeName(i));
        }
        choicePheno = new JComboBox<>(phenoModel);
        choicePheno.setSelectedIndex(person.getPhenotype());
        choicePheno.addActionListener(evt -> backgroundChanged());
        choicePheno.setEnabled(person.isClanner());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choicePheno, gridBagConstraints);

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

        btnDate.setText(getDateAsString());
        btnDate.setName("btnDate"); // NOI18N
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panDemog.add(btnDate, gridBagConstraints);

        lblAge.setText(person.getAge(campaign.getCalendar()) + " " + resourceMap.getString("age")); // NOI18N
        lblAge.setName("lblAge"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblAge, gridBagConstraints);

        y++;

        if (campaign.getCampaignOptions().getUseTimeInService() && (recruitment != null)) {
            lblRecruitment.setText(resourceMap.getString("lblRecruitment.text")); // NOI18N
            lblRecruitment.setName("lblRecruitment"); // NOI18N
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblRecruitment, gridBagConstraints);

            btnServiceDate.setText(getDateAsString2());
            btnServiceDate.setName("btnServiceDate"); // NOI18N
            btnServiceDate.addActionListener(this::btnServiceDateActionPerformed);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panDemog.add(btnServiceDate, gridBagConstraints);

        }

        y++;

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
        			setText(((Unit)value).getName());
        		}
        		return this;
        	}
        });
    	choiceOriginalUnit.addItem(null);
        for (Unit unit : campaign.getUnits()) {
        	choiceOriginalUnit.addItem(unit);
        }
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
        if(campaign.getCampaignOptions().useAbilities() || campaign.getCampaignOptions().useEdge()
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
    }// </editor-fold>//GEN-END:initComponents

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CustomizePersonDialog.class);
        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

	private DefaultComboBoxModel<Faction> getFactionsComboBoxModel() {
        int year = person.getCampaign().getGameYear();
        List<Faction> orderedFactions = Faction.getFactions().stream()
            .sorted((a, b) -> a.getFullName(year).compareToIgnoreCase(b.getFullName(year)))
            .collect(Collectors.toList());

        DefaultComboBoxModel<Faction> factionsModel = new DefaultComboBoxModel<>();
        for (Faction faction : orderedFactions) {
            // Always include the person's faction
            if (faction == person.getOriginFaction()) {
                factionsModel.addElement(faction);
            } else {
                if (faction.is(Tag.HIDDEN) || faction.is(Tag.SPECIAL)) {
                    continue;
                }

                // Allow factions between the person's birthday
                // and when they were recruited, or now if we're
                // not tracking recruitment.
                int endYear = person.getRecruitment() != null
                    ? Math.min(person.getRecruitment().get(Calendar.YEAR), year)
                    : year;
                if (faction.validBetween(person.getBirthday().get(Calendar.YEAR), endYear)) {
                    factionsModel.addElement(faction);
                }
            }
        }

        return factionsModel;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel() {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        DateTime currentYear = new DateTime(campaign.getCalendar());
        List<PlanetarySystem> orderedSystems = campaign.getSystems().stream()
            .sorted(Comparator.comparing(a -> a.getName(currentYear)))
            .collect(Collectors.toList());
        for (PlanetarySystem system : orderedSystems) {
            model.addElement(system);
        }
        return model;
    }

    private DefaultComboBoxModel<PlanetarySystem> getPlanetarySystemsComboBoxModel(Faction faction) {
        DefaultComboBoxModel<PlanetarySystem> model = new DefaultComboBoxModel<>();

        DateTime birthYear = new DateTime(person.getBirthday());
        List<PlanetarySystem> orderedSystems = campaign.getSystems().stream()
            .filter(a -> a.getFactionSet(birthYear).contains(faction))
            .sorted(Comparator.comparing(a -> a.getName(birthYear)))
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
                if (planet != planetarySystem.getPrimaryPlanet()) {
                    planetsModel.addElement(planet);
                }
            }
        }
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCloseActionPerformed
        setVisible(false);
    }// GEN-LAST:event_btnCloseActionPerformed

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOkActionPerformed
        person.setGivenName(textGivenName.getText());
        person.setSurname(textSurname.getText());
        person.setHonorific(textHonorific.getText());
        person.setCallsign(textNickname.getText());
        person.setBloodname(textBloodname.getText());
        person.setBiography(txtBio.getText());
        person.setGender(choiceGender.getSelectedIndex());
        person.setBirthday(birthdate);
        person.setRecruitment(recruitment);
        person.setOriginFaction((Faction) choiceFaction.getSelectedItem());
        if (choiceSystem.getSelectedItem() != null && choicePlanet.getSelectedItem() != null) {
            person.setOriginPlanet((Planet)choicePlanet.getSelectedItem());
        } else {
            person.setOriginPlanet(null);
        }
        person.setPhenotype(choicePheno.getSelectedIndex());
        person.setClanner(chkClan.isSelected());
        try {
            person.setToughness(Integer.parseInt(textToughness.getText()));
        } catch (NumberFormatException ignored) { }
        if (null == choiceOriginalUnit.getSelectedItem()) {
        	person.setOriginalUnitWeight(choiceUnitWeight.getSelectedIndex());
        	person.setOriginalUnitTech(choiceUnitTech.getSelectedIndex());
        } else {
        	person.setOriginalUnitId(((Unit)choiceOriginalUnit.getSelectedItem()).getId());
        }
        person.setFounder(chkFounder.isSelected());
        setSkills();
        setOptions();
        setVisible(false);
    }//GEN-LAST:event_btnOkActionPerformed

    private void randomName() {
        String[] name = campaign.getRNG().generateGivenNameSurnameSplit(
                choiceGender.getSelectedIndex() == Crew.G_FEMALE,
                person.isClanner(), person.getOriginFaction().getShortName());
        textGivenName.setText(name[0]);
        textSurname.setText(name[1]);
    }

    private void randomBloodname() {
        int phenotype = Bloodname.P_GENERAL;
        switch (person.getPrimaryRole()) {
            case Person.T_MECHWARRIOR:
                phenotype = Bloodname.P_MECHWARRIOR;
                break;
            case Person.T_BA:
                phenotype = Bloodname.P_ELEMENTAL;
                break;
            case Person.T_AERO_PILOT:
            case Person.T_CONV_PILOT:
                phenotype = Bloodname.P_AEROSPACE;
                break;
            case Person.T_SPACE_CREW:
            case Person.T_NAVIGATOR:
            case Person.T_SPACE_GUNNER:
            case Person.T_SPACE_PILOT:
                phenotype = Bloodname.P_NAVAL;
                break;
            case Person.T_PROTO_PILOT:
                phenotype = Bloodname.P_PROTOMECH;
                break;
        }
        textBloodname.setText(Bloodname.randomBloodname(campaign.getFaction().isClan()
                ? campaign.getFactionCode() : person.getOriginFaction().getShortName(),
                phenotype, campaign.getCalendar().get(Calendar.YEAR)).getName());
    }

    public void refreshSkills() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog",
                new EncodeControl());
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

        for(int i = 0; i < SkillType.getSkillList().length; i++) {
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
    			int lvl = (Integer)skillLvls.get(type).getModel().getValue();
    			int b = (Integer)skillBonus.get(type).getModel().getValue();
    			person.addSkill(type, lvl, b);
    		} else {
    			person.removeSkill(type);
    		}
    	}
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue().equals("None"))) { // NON-NLS-$1
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
                IOption option = j.nextElement();

                addOption(option, gridBag, c, true);
            }
        }
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridBag, GridBagConstraints c) {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        JLabel groupLabel = new JLabel(resourceMap.getString("optionGroup." + group.getKey())); //$NON-NLS-1$

        gridBag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridBag, GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if (OptionsConstants.GUNNERY_WEAPON_SPECIALIST.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue("None"); //$NON-NLS-1$
            //holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements();) {
                EquipmentType etype = i.nextElement();
                if (etype instanceof WeaponType) {
                    optionComp.addValue(etype.getName());
                }
            }
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_SPECIALIST.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Crew.SPECIAL_NONE);
            optionComp.addValue(Crew.SPECIAL_ENERGY);
            optionComp.addValue(Crew.SPECIAL_BALLISTIC);
            optionComp.addValue(Crew.SPECIAL_MISSILE);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.GUNNERY_RANGE_MASTER.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Crew.RANGEMASTER_NONE);
            optionComp.addValue(Crew.RANGEMASTER_MEDIUM);
            optionComp.addValue(Crew.RANGEMASTER_LONG);
            optionComp.addValue(Crew.RANGEMASTER_EXTREME);
            optionComp.setSelected(option.stringValue());
        } else if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) { //$NON-NLS-1$
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
            if ((comp.getValue().equals("None"))) { // NON-NLS-$1
                person.getOptions().getOption(option.getName()).setValue("None"); // NON-NLS-$1
            } else {
                person.getOptions().getOption(option.getName())
                .setValue(comp.getValue());
            }
        }
    }

    private String getDateAsString() {
        return dateFormat.format(birthdate.getTime());
    }

    private String getDateAsString2() {
        return dateFormat.format(recruitment.getTime());
    }

    private void changeSkillValue(String type) {
    	if (!skillChks.get(type).isSelected()) {
    		skillValues.get(type).setText("-");
    		return;
    	}
        SkillType stype = SkillType.getType(type);
        int lvl = (Integer)skillLvls.get(type).getModel().getValue();
        int b = (Integer)skillBonus.get(type).getModel().getValue();
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

    private void btnDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDateActionPerformed
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        // show the date chooser
        DateChooser dc = new DateChooser(frame, birthdate);
        // user can eiter choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            birthdate = dc.getDate();
            btnDate.setText(getDateAsString());
            lblAge.setText(getAge() + " " + resourceMap.getString("age")); // NOI18N
        }
    }

    private void btnServiceDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnServiceDateActionPerformed
        // show the date chooser
        DateChooser dc = new DateChooser(frame, recruitment);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            recruitment = dc.getDate();
            btnServiceDate.setText(getDateAsString2());
        }
    }

    public int getAge() {
    	// Get age based on year
    	int age = campaign.getCalendar().get(Calendar.YEAR) - birthdate.get(Calendar.YEAR);

    	// Add the tentative age to the date of birth to get this year's birthday
    	GregorianCalendar tmpDate = (GregorianCalendar) birthdate.clone();
    	tmpDate.add(Calendar.YEAR, age);

    	// If this year's birthday has not happened yet, subtract one from age
    	if (campaign.getCalendar().before(tmpDate)) {
    	    age--;
    	}
    	return age;
    }

    private void backgroundChanged() {
        int pheno = choicePheno.getSelectedIndex();
        boolean clanner = chkClan.isSelected();
        clearAllPhenotypeBonuses();
        if (clanner) {
            switch(pheno) {
                case Person.PHENOTYPE_MW:
                    skillBonus.get(SkillType.S_GUN_MECH).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_MECH).setValue(1);
                    break;
                case Person.PHENOTYPE_AERO:
                    skillBonus.get(SkillType.S_GUN_AERO).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_AERO).setValue(1);
                    skillBonus.get(SkillType.S_GUN_JET).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_JET).setValue(1);
                    skillBonus.get(SkillType.S_GUN_PROTO).setValue(1);
                    break;
                case Person.PHENOTYPE_BA:
                    skillBonus.get(SkillType.S_GUN_BA).setValue(1);
                    break;
                case Person.PHENOTYPE_VEE:
                    skillBonus.get(SkillType.S_GUN_VEE).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_GVEE).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_NVEE).setValue(1);
                    skillBonus.get(SkillType.S_PILOT_VTOL).setValue(1);
                    break;
            }
            choicePheno.setEnabled(true);
        } else {
            choicePheno.setSelectedIndex(0);
            choicePheno.setEnabled(false);
        }
    }

    private void clearAllPhenotypeBonuses() {
        skillBonus.get(SkillType.S_GUN_MECH).setValue(0);
        skillBonus.get(SkillType.S_PILOT_MECH).setValue(0);
        skillBonus.get(SkillType.S_GUN_AERO).setValue(0);
        skillBonus.get(SkillType.S_PILOT_AERO).setValue(0);
        skillBonus.get(SkillType.S_GUN_JET).setValue(0);
        skillBonus.get(SkillType.S_PILOT_JET).setValue(0);
        skillBonus.get(SkillType.S_GUN_PROTO).setValue(0);
        skillBonus.get(SkillType.S_GUN_BA).setValue(0);
        skillBonus.get(SkillType.S_GUN_VEE).setValue(0);
        skillBonus.get(SkillType.S_PILOT_GVEE).setValue(0);
        skillBonus.get(SkillType.S_PILOT_NVEE).setValue(0);
        skillBonus.get(SkillType.S_PILOT_VTOL).setValue(0);
    }

    public void optionClicked(DialogOptionComponent arg0, IOption arg1, boolean arg2) {
        //IMplement me!!
    }
}
