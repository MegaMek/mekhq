package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;
import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.adapter.SocioIndustrialDataAdapter;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Climate;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.LifeForm;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.StarUtil;

public class NewPlanetaryEventDialog extends JDialog {
    private static final long serialVersionUID = 6025304629282204159L;
    
    private static final String FIELD_MESSAGE = "message"; //$NON-NLS-1$
    private static final String FIELD_NAME = "name"; //$NON-NLS-1$
    private static final String FIELD_SHORTNAME = "shortName"; //$NON-NLS-1$
    private static final String FIELD_FACTION = "faction"; //$NON-NLS-1$
    private static final String FIELD_LIFE_FORM = "lifeForm"; //$NON-NLS-1$
    private static final String FIELD_CLIMATE = "climate"; //$NON-NLS-1$
    private static final String FIELD_WATER = "water"; //$NON-NLS-1$
    private static final String FIELD_TEMPERATURE = "temperature"; //$NON-NLS-1$
    private static final String FIELD_SOCIO_INDUSTRIAL = "socioindustrial"; //$NON-NLS-1$
    private static final String FIELD_HPG = "hpg"; //$NON-NLS-1$
    private static final String FIELD_PRESSURE = "pressure"; //$NON-NLS-1$
    private static final String FIELD_PRESSURE_ATM = "pressureAtm"; //$NON-NLS-1$
    private static final String FIELD_ATM_MASS = "atmMass"; //$NON-NLS-1$
    private static final String FIELD_ATMOSPHERE = "atmosphere"; //$NON-NLS-1$
    private static final String FIELD_ALBEDO = "albedo"; //$NON-NLS-1$
    private static final String FIELD_GREENHOUSE = "greenhouse"; //$NON-NLS-1$
    private static final String FIELD_HABITABILITY = "habitability"; //$NON-NLS-1$
    private static final String FIELD_POPULATION = "pop"; //$NON-NLS-1$
    private static final String FIELD_GOVERNMENT = "government"; //$NON-NLS-1$
    private static final String FIELD_CONTROL = "control"; //$NON-NLS-1$
    
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd"); //$NON-NLS-1$
    private final static SocioIndustrialDataAdapter SOCIO_INDUSTRIAL_ADAPTER = new SocioIndustrialDataAdapter();
    
    ResourceBundle resourceMap;

    private final Planet planet;
    
    private DateTime date;
    
    private List<Planet.PlanetaryEvent> changedEvents = null;

    private JButton dateButton;
    private JTextField messageField;
    private JTextField nameField;
    private JTextField shortNameField;
    private JButton factionsButton;
    private JComboBox<LifeFormChoice> lifeFormField;
    private JComboBox<ClimateChoice> climateField;
    private JSpinner waterField;
    private JSpinner temperatureField;
    private JTextField socioindustrialField;
    private JComboBox<HPGChoice> hpgField;
    private JComboBox<PressureChoice> pressureField;
    private JSpinner pressureAtmField;
    private JSpinner atmMassField;
    private JComboBox<String> atmosphereField;
    private JSpinner albedoField;
    private JSpinner greenhouseField;
    private JSpinner habitabilityField;
    private JComboBox<PopulationChoice> popField;
    private JTextField governmentField;
    private JComboBox<ControlChoice> controlField;

    private JCheckBox nameKeep;
    private JCheckBox shortNameKeep;
    private JCheckBox factionKeep;
    private JCheckBox lifeFormKeep;
    private JCheckBox climateKeep;
    private JCheckBox waterKeep;
    private JCheckBox temperatureKeep;
    private JCheckBox socioindustrialKeep;
    private JCheckBox hpgKeep;
    private JCheckBox pressureKeep;
    private JCheckBox pressureAtmKeep;
    private JCheckBox atmMassKeep;
    private JCheckBox atmosphereKeep;
    private JCheckBox albedoKeep;
    private JCheckBox greenhouseKeep;
    private JCheckBox habitabilityKeep;
    private JCheckBox popKeep;
    private JCheckBox governmentKeep;
    private JCheckBox controlKeep;
    
    private JLabel nameCombined;
    private JLabel shortNameCombined;
    private JLabel factionCombined;
    private JLabel lifeFormCombined;
    private JLabel climateCombined;
    private JLabel waterCombined;
    private JLabel temperatureCombined;
    private JLabel socioindustrialCombined;
    private JLabel hpgCombined;
    private JLabel pressureCombined;
    private JLabel pressureAtmCombined;
    private JLabel atmMassCombined;
    private JLabel atmosphereCombined;
    private JLabel albedoCombined;
    private JLabel greenhouseCombined;
    private JLabel habitabilityCombined;
    private JLabel popCombined;
    private JLabel governmentCombined;
    private JLabel controlCombined;

    public NewPlanetaryEventDialog(Frame parent, Campaign campaign, Planet planet) {
        this(parent, campaign, planet, true);
    }
    
    public NewPlanetaryEventDialog(Frame parent, Campaign campaign, Planet planet, boolean modal) {
        super(parent, modal);
        this.planet = new Planet(Objects.requireNonNull(planet).getId());
        this.planet.copyDataFrom(planet);
        this.date = Utilities.getDateTimeDay(campaign.getCalendar());
        initComponents();
        setLocationRelativeTo(parent);
    }
    
    public List<Planet.PlanetaryEvent> getChangedEvents() {
        return changedEvents;
    }
    
    protected void initComponents() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.NewPlanetaryEventDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("form"); //$NON-NLS-1$
        setTitle(resourceMap.getString("Form.title")); //$NON-NLS-1$
        setMinimumSize(new Dimension(600, 600));
        
        final Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        content.add(new JLabel(String.format(resourceMap.getString("planetId.format"), planet.getId())), gbc); //$NON-NLS-1$
        
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("previousDay.label")){ //$NON-NLS-1$
            private static final long serialVersionUID = -4901868873472027052L;
            
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.CTRL_MASK));
                putValue(SHORT_DESCRIPTION, resourceMap.getString("previousDay.tooltip")); //$NON-NLS-1$
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if((e.getModifiers() & ActionEvent.ALT_MASK) > 0) {
                    date = date.minusYears(1);
                } else if((e.getModifiers() & ActionEvent.CTRL_MASK) > 0) {
                    date = date.minusMonths(1);
                } else {
                    date = date.minusDays(1);
                }
                updateDate();
            }
        }), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        dateButton = new JButton(new AbstractAction() {
            private static final long serialVersionUID = 5708871251030417524L;
            {
                putValue(SHORT_DESCRIPTION, resourceMap.getString("setDay.tooltip")); //$NON-NLS-1$
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                DateChooser dc = new DateChooser((content instanceof Frame) ? (Frame) content : null, date.toGregorianCalendar());
                if (dc.showDateChooser() == DateChooser.OK_OPTION) {
                    date = Utilities.getDateTimeDay(dc.getDate());
                    updateDate();
                }
            }
        });
        content.add(dateButton, gbc);
        
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("nextDay.label")){ //$NON-NLS-1$
            private static final long serialVersionUID = -4901868873472027053L;
            
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.CTRL_MASK));
                putValue(ACTION_COMMAND_KEY, "nextDay"); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, resourceMap.getString("nextDay.tooltip")); //$NON-NLS-1$
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if((e.getModifiers() & ActionEvent.ALT_MASK) > 0) {
                    date = date.plusYears(1);
                } else if((e.getModifiers() & ActionEvent.CTRL_MASK) > 0) {
                    date = date.plusMonths(1);
                } else {
                    date = date.plusDays(1);
                }
                updateDate();
            }
        }), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel data = new JPanel(new GridBagLayout());
        data.setName("data"); //$NON-NLS-1$
        data.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(resourceMap.getString("eventData.text")), //$NON-NLS-1$
            BorderFactory.createEmptyBorder(1, 5, 1, 5)));
        content.add(data, gbc);
        
        preparaDataPane(data);
        
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        content.add(new JButton(new AbstractAction(resourceMap.getString("save.text")){ //$NON-NLS-1$
            private static final long serialVersionUID = -8920630119126015952L;

            @Override
            public void actionPerformed(ActionEvent e) {
                changedEvents = new ArrayList<>();
                for(PlanetaryEvent event : planet.getEvents()) {
                    if(event.custom) {
                        changedEvents.add(event);
                    }
                }
                setVisible(false);
            }
        }), gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("cancel.text")){ //$NON-NLS-1$
            private static final long serialVersionUID = -8920630119126015953L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }), gbc);

        updateDate();
        pack();
    }
    
    private void preparaDataPane(JPanel pane) {
        GridBagConstraints gbc = new GridBagConstraints();
        
        Action changeValueAction = new AbstractAction() {
            private static final long serialVersionUID = 7405843636038153841L;

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEvent((Component) e.getSource(), planet.getOrCreateEvent(date));
                updateDate();
            }
        };
        
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateEvent((Component) e.getSource(), planet.getOrCreateEvent(date));
                updateDate();
            }
        };

        Action noChangeAction = new AbstractAction() {
            private static final long serialVersionUID = 7405843636038153841L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox check = ((JCheckBox) e.getSource());
                String field = check.getName();
                if(check.isSelected()) {
                    cleanEventField(getCurrentEvent(), field);
                    updateDate();
                }
            }
        };
        
        FocusAdapter textFocusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                ((JTextField) e.getSource()).selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if(e.getSource() instanceof JTextField)
                {
                    final JTextField source = (JTextField) e.getSource();
                    source.dispatchEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));  
                }
            }
        };
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        pane.add(new JLabel(resourceMap.getString("changeOf.text")), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        pane.add(new JLabel(resourceMap.getString("newValue.text")), gbc); //$NON-NLS-1$
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        pane.add(new JLabel(resourceMap.getString("combinedValue.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pane.add(new JLabel(resourceMap.getString("message.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        messageField = new JTextField();
        messageField.addActionListener(changeValueAction);
        messageField.addFocusListener(textFocusAdapter);
        messageField.setName(FIELD_MESSAGE);
        pane.add(messageField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        pane.add(new JLabel(resourceMap.getString("name.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        nameField = new JTextField();
        nameField.addActionListener(changeValueAction);
        nameField.addFocusListener(textFocusAdapter);
        nameField.setName(FIELD_NAME);
        pane.add(nameField, gbc);

        gbc.gridx = 2;
        gbc.ipadx = 10;
        nameKeep = new JCheckBox(noChangeAction);
        nameKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        nameKeep.setName(FIELD_NAME);
        pane.add(nameKeep, gbc);
        gbc.ipadx = 0;
        
        gbc.gridx = 3;
        nameCombined = new JLabel();
        pane.add(nameCombined, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        pane.add(new JLabel(resourceMap.getString("shortName.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        shortNameField = new JTextField();
        shortNameField.addActionListener(changeValueAction);
        shortNameField.addFocusListener(textFocusAdapter);
        shortNameField.setName(FIELD_SHORTNAME);
        pane.add(shortNameField, gbc);

        gbc.gridx = 2;
        shortNameKeep = new JCheckBox(noChangeAction);
        shortNameKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        shortNameKeep.setName(FIELD_SHORTNAME);
        pane.add(shortNameKeep, gbc);
        
        gbc.gridx = 3;
        shortNameCombined = new JLabel();
        pane.add(shortNameCombined, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        pane.add(new JLabel(resourceMap.getString("factionList.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        factionsButton = new JButton(new AbstractAction("") { //$NON-NLS-1$
            private static final long serialVersionUID = -168994356642401048L;

            @Override
            public void actionPerformed(ActionEvent e) {
                Planet.PlanetaryEvent event = planet.getOrCreateEvent(date);
                ChooseFactionsDialog chooser = new ChooseFactionsDialog(null, date, event.faction);
                chooser.setVisible(true);
                if(chooser.isChanged()) {
                    event.faction = chooser.getResult();
                    event.custom = true;
                    updateDate();
                }
            }
        });
        pane.add(factionsButton, gbc);

        gbc.gridx = 2;
        factionKeep = new JCheckBox(noChangeAction);
        factionKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        factionKeep.setName(FIELD_FACTION);
        pane.add(factionKeep, gbc);
        
        gbc.gridx = 3;
        factionCombined = new JLabel();
        pane.add(factionCombined, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        pane.add(new JLabel(resourceMap.getString("lifeform.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        lifeFormField = new JComboBox<LifeFormChoice>(new LifeFormChoice[]{
            new LifeFormChoice(null),
            new LifeFormChoice(LifeForm.NONE), new LifeFormChoice(LifeForm.MICROBE), new LifeFormChoice(LifeForm.PLANT),
            new LifeFormChoice(LifeForm.INSECT), new LifeFormChoice(LifeForm.FISH), new LifeFormChoice(LifeForm.AMPH),
            new LifeFormChoice(LifeForm.REPTILE), new LifeFormChoice(LifeForm.BIRD), new LifeFormChoice(LifeForm.MAMMAL)
        });
        lifeFormField.addActionListener(changeValueAction);
        lifeFormField.setName(FIELD_LIFE_FORM);
        pane.add(lifeFormField, gbc);

        gbc.gridx = 2;
        lifeFormKeep = new JCheckBox(noChangeAction);
        lifeFormKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        lifeFormKeep.setName(FIELD_LIFE_FORM);
        pane.add(lifeFormKeep, gbc);
        
        gbc.gridx = 3;
        lifeFormCombined = new JLabel();
        pane.add(lifeFormCombined, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        pane.add(new JLabel(resourceMap.getString("climate.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        climateField = new JComboBox<ClimateChoice>(new ClimateChoice[]{
                new ClimateChoice(null),
                new ClimateChoice(Climate.ARCTIC), new ClimateChoice(Climate.BOREAL), new ClimateChoice(Climate.TEMPERATE),
                new ClimateChoice(Climate.WARM), new ClimateChoice(Climate.TROPICAL), new ClimateChoice(Climate.SUPERTROPICAL),
                new ClimateChoice(Climate.HELL)
        });
        climateField.addActionListener(changeValueAction);
        climateField.setName(FIELD_CLIMATE);
        pane.add(climateField, gbc);

        gbc.gridx = 2;
        climateKeep = new JCheckBox(noChangeAction);
        climateKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        climateKeep.setName(FIELD_CLIMATE);
        pane.add(climateKeep, gbc);
        
        gbc.gridx = 3;
        climateCombined = new JLabel();
        pane.add(climateCombined, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 7;
        pane.add(new JLabel(resourceMap.getString("water.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        waterField = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        waterField.addChangeListener(changeListener);
        waterField.setName(FIELD_WATER);
        pane.add(waterField, gbc);

        gbc.gridx = 2;
        waterKeep = new JCheckBox(noChangeAction);
        waterKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        waterKeep.setName(FIELD_WATER);
        pane.add(waterKeep, gbc);
        
        gbc.gridx = 3;
        waterCombined = new JLabel();
        pane.add(waterCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        pane.add(new JLabel(resourceMap.getString("temperature.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        temperatureField = new JSpinner(new SpinnerNumberModel(0, -273, 10000, 1));
        temperatureField.addChangeListener(changeListener);
        temperatureField.setName(FIELD_TEMPERATURE);
        pane.add(temperatureField, gbc);

        gbc.gridx = 2;
        temperatureKeep = new JCheckBox(noChangeAction);
        temperatureKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        temperatureKeep.setName(FIELD_TEMPERATURE);
        pane.add(temperatureKeep, gbc);
        
        gbc.gridx = 3;
        temperatureCombined = new JLabel();
        pane.add(temperatureCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        pane.add(new JLabel(resourceMap.getString("socioindustrial.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        socioindustrialField = new JTextField();
        socioindustrialField.addActionListener(changeValueAction);
        socioindustrialField.addFocusListener(textFocusAdapter);
        socioindustrialField.setName(FIELD_SOCIO_INDUSTRIAL);
        socioindustrialField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                try {
                    SOCIO_INDUSTRIAL_ADAPTER.unmarshal(text);
                } catch(Exception ex) {
                    return false;
                }
                return true;
            }
        });
        pane.add(socioindustrialField, gbc);

        gbc.gridx = 2;
        socioindustrialKeep = new JCheckBox(noChangeAction);
        socioindustrialKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        socioindustrialKeep.setName(FIELD_SOCIO_INDUSTRIAL);
        pane.add(socioindustrialKeep, gbc);
        
        gbc.gridx = 3;
        socioindustrialCombined = new JLabel();
        pane.add(socioindustrialCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        pane.add(new JLabel(resourceMap.getString("hpg.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        hpgField = new JComboBox<HPGChoice>(new HPGChoice[]{
                new HPGChoice(null, resourceMap.getString("hpg.undefined.text")), //$NON-NLS-1$
                new HPGChoice(EquipmentType.RATING_A, resourceMap.getString("hpg.a.text")), new HPGChoice(EquipmentType.RATING_B, resourceMap.getString("hpg.b.text")), //$NON-NLS-1$ //$NON-NLS-2$
                new HPGChoice(EquipmentType.RATING_C, resourceMap.getString("hpg.c.text")), new HPGChoice(EquipmentType.RATING_D, resourceMap.getString("hpg.d.text")), //$NON-NLS-1$ //$NON-NLS-2$
                new HPGChoice(EquipmentType.RATING_X, resourceMap.getString("hpg.none.text")) //$NON-NLS-1$
        });
        hpgField.addActionListener(changeValueAction);
        hpgField.setName(FIELD_HPG);
        pane.add(hpgField, gbc);

        gbc.gridx = 2;
        hpgKeep = new JCheckBox(noChangeAction);
        hpgKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        hpgKeep.setName(FIELD_HPG);
        pane.add(hpgKeep, gbc);
        
        gbc.gridx = 3;
        hpgCombined = new JLabel();
        pane.add(hpgCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 11;
        pane.add(new JLabel(resourceMap.getString("pressureCategory.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        pressureField = new JComboBox<PressureChoice>(new PressureChoice[]{
                new PressureChoice(-1, resourceMap.getString("pressureCategory.undefined.text")), //$NON-NLS-1$
                new PressureChoice(PlanetaryConditions.ATMO_VACUUM), new PressureChoice(PlanetaryConditions.ATMO_TRACE),
                new PressureChoice(PlanetaryConditions.ATMO_THIN), new PressureChoice(PlanetaryConditions.ATMO_STANDARD),
                new PressureChoice(PlanetaryConditions.ATMO_HIGH), new PressureChoice(PlanetaryConditions.ATMO_VHIGH)
        });
        pressureField.addActionListener(changeValueAction);
        pressureField.setName(FIELD_PRESSURE);
        pane.add(pressureField, gbc);

        gbc.gridx = 2;
        pressureKeep = new JCheckBox(noChangeAction);
        pressureKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        pressureKeep.setName(FIELD_PRESSURE);
        pane.add(pressureKeep, gbc);
        
        gbc.gridx = 3;
        pressureCombined = new JLabel();
        pane.add(pressureCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 12;
        pane.add(new JLabel(resourceMap.getString("pressureValue.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        pressureAtmField = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.01));
        pressureAtmField.setEditor(new JSpinner.NumberEditor(pressureAtmField, resourceMap.getString("pressureValue.format"))); //$NON-NLS-1$
        pressureAtmField.addChangeListener(changeListener);
        pressureAtmField.setName(FIELD_PRESSURE_ATM);
        pane.add(pressureAtmField, gbc);

        gbc.gridx = 2;
        pressureAtmKeep = new JCheckBox(noChangeAction);
        pressureAtmKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        pressureAtmKeep.setName(FIELD_PRESSURE_ATM);
        pane.add(pressureAtmKeep, gbc);
        
        gbc.gridx = 3;
        pressureAtmCombined = new JLabel();
        pane.add(pressureAtmCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 13;
        pane.add(new JLabel(resourceMap.getString("atmosphereMass.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        atmMassField = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.01));
        atmMassField.setEditor(new JSpinner.NumberEditor(atmMassField, resourceMap.getString("atmosphereMass.format"))); //$NON-NLS-1$
        atmMassField.addChangeListener(changeListener);
        atmMassField.setName(FIELD_ATM_MASS);
        pane.add(atmMassField, gbc);

        gbc.gridx = 2;
        atmMassKeep = new JCheckBox(noChangeAction);
        atmMassKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        atmMassKeep.setName(FIELD_ATM_MASS);
        pane.add(atmMassKeep, gbc);
        
        gbc.gridx = 3;
        atmMassCombined = new JLabel();
        pane.add(atmMassCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 14;
        pane.add(new JLabel(resourceMap.getString("atmosphereType.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        atmosphereField = new JComboBox<String>(new String[]{
                null,
                resourceMap.getString("atmosphereType.breathable.text"), //$NON-NLS-1$
                resourceMap.getString("atmosphereType.tainted.text"), resourceMap.getString("atmosphereType.tainted.caustic.text"), resourceMap.getString("atmosphereType.tainted.flammable.text"), resourceMap.getString("atmosphereType.tainted.poisonous.text"), resourceMap.getString("atmosphereType.tainted.radiological.text"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                resourceMap.getString("atmosphereType.toxic.text"), resourceMap.getString("atmosphereType.toxic.caustic.text"), resourceMap.getString("atmosphereType.toxic.flammable.text"), resourceMap.getString("atmosphereType.toxic.poisonous.text"), resourceMap.getString("atmosphereType.toxic.radiological.text") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        });
        atmosphereField.addActionListener(changeValueAction);
        atmosphereField.setName(FIELD_ATMOSPHERE);
        pane.add(atmosphereField, gbc);

        gbc.gridx = 2;
        atmosphereKeep = new JCheckBox(noChangeAction);
        atmosphereKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        atmosphereKeep.setName(FIELD_ATMOSPHERE);
        pane.add(atmosphereKeep, gbc);
        
        gbc.gridx = 3;
        atmosphereCombined = new JLabel();
        pane.add(atmosphereCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 15;
        pane.add(new JLabel(resourceMap.getString("albedo.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        albedoField = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 1.0, 0.01));
        albedoField.setEditor(new JSpinner.NumberEditor(albedoField, resourceMap.getString("albedo.format"))); //$NON-NLS-1$
        albedoField.addChangeListener(changeListener);
        albedoField.setName(FIELD_ALBEDO);
        pane.add(albedoField, gbc);

        gbc.gridx = 2;
        albedoKeep = new JCheckBox(noChangeAction);
        albedoKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        albedoKeep.setName(FIELD_ALBEDO);
        pane.add(albedoKeep, gbc);
        
        gbc.gridx = 3;
        albedoCombined = new JLabel();
        pane.add(albedoCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 16;
        pane.add(new JLabel(resourceMap.getString("greenhouse.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        greenhouseField = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 100.0, 0.01));
        greenhouseField.setEditor(new JSpinner.NumberEditor(greenhouseField, resourceMap.getString("greenhouse.format"))); //$NON-NLS-1$
        greenhouseField.addChangeListener(changeListener);
        greenhouseField.setName(FIELD_GREENHOUSE);
        pane.add(greenhouseField, gbc);

        gbc.gridx = 2;
        greenhouseKeep = new JCheckBox(noChangeAction);
        greenhouseKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        greenhouseKeep.setName(FIELD_GREENHOUSE);
        pane.add(greenhouseKeep, gbc);
        
        gbc.gridx = 3;
        greenhouseCombined = new JLabel();
        pane.add(greenhouseCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 17;
        pane.add(new JLabel(resourceMap.getString("habitability.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        habitabilityField = new JSpinner(new SpinnerNumberModel(2, -5, 10, 1));
        habitabilityField.addChangeListener(changeListener);
        habitabilityField.setName(FIELD_HABITABILITY);
        pane.add(habitabilityField, gbc);

        gbc.gridx = 2;
        habitabilityKeep = new JCheckBox(noChangeAction);
        habitabilityKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        habitabilityKeep.setName(FIELD_HABITABILITY);
        pane.add(habitabilityKeep, gbc);
        
        gbc.gridx = 3;
        habitabilityCombined = new JLabel();
        pane.add(habitabilityCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 18;
        pane.add(new JLabel(resourceMap.getString("population.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        popField = new JComboBox<PopulationChoice>(new PopulationChoice[]{
                new PopulationChoice(null, resourceMap.getString("population.undefined.text")), //$NON-NLS-1$
                new PopulationChoice(-1), new PopulationChoice(0), new PopulationChoice(1), new PopulationChoice(2),
                new PopulationChoice(3), new PopulationChoice(4), new PopulationChoice(5), new PopulationChoice(6),
                new PopulationChoice(7), new PopulationChoice(8), new PopulationChoice(9), new PopulationChoice(10),
                new PopulationChoice(11), new PopulationChoice(12)
        });
        popField.addActionListener(changeValueAction);
        popField.setName(FIELD_POPULATION);
        pane.add(popField, gbc);

        gbc.gridx = 2;
        popKeep = new JCheckBox(noChangeAction);
        popKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        popKeep.setName(FIELD_POPULATION);
        pane.add(popKeep, gbc);
        
        gbc.gridx = 3;
        popCombined = new JLabel();
        pane.add(popCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 19;
        pane.add(new JLabel(resourceMap.getString("government.text")), gbc); //$NON-NLS-1$

        gbc.gridx = 1;
        governmentField = new JTextField();
        governmentField.addActionListener(changeValueAction);
        governmentField.addFocusListener(textFocusAdapter);
        governmentField.setName(FIELD_GOVERNMENT);
        pane.add(governmentField, gbc);

        gbc.gridx = 2;
        governmentKeep = new JCheckBox(noChangeAction);
        governmentKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        governmentKeep.setName(FIELD_GOVERNMENT);
        pane.add(governmentKeep, gbc);
        
        gbc.gridx = 3;
        governmentCombined = new JLabel();
        pane.add(governmentCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 20;
        pane.add(new JLabel(resourceMap.getString("control.text")), gbc); //$NON-NLS-1$
        
        gbc.gridx = 1;
        controlField = new JComboBox<ControlChoice>(new ControlChoice[]{
                new ControlChoice(null, resourceMap.getString("control.undefined.text")), //$NON-NLS-1$
                new ControlChoice(0), new ControlChoice(1), new ControlChoice(2),
                new ControlChoice(3), new ControlChoice(4), new ControlChoice(5),
                new ControlChoice(6), new ControlChoice(7)
        });
        controlField.addActionListener(changeValueAction);
        controlField.setName(FIELD_CONTROL);
        pane.add(controlField, gbc);

        gbc.gridx = 2;
        controlKeep = new JCheckBox(noChangeAction);
        controlKeep.setText(resourceMap.getString("noChange.text")); //$NON-NLS-1$
        controlKeep.setName(FIELD_CONTROL);
        pane.add(controlKeep, gbc);
        
        gbc.gridx = 3;
        controlCombined = new JLabel();
        pane.add(controlCombined, gbc);

    }
    
    private Planet.PlanetaryEvent getCurrentEvent() {
        return planet.getEvent(date);
    }
    
    private void updateDate() {
        dateButton.setText(date.toString(DATE_FORMATTER));
        Planet.PlanetaryEvent event = getCurrentEvent();

        messageField.setText((null != event) ? event.message : null);
        nameField.setText((null != event) ? event.name : null);
        shortNameField.setText((null != event) ? event.shortName : null);
        Set<Faction> factionSet = null;
        if((null != event) && (null != event.faction)) {
            factionSet = new HashSet<>();
            for(String f : event.faction) {
                factionSet.add(Faction.getFaction(f));
            }
        }
        factionsButton.setText(Faction.getFactionNames(factionSet, date.getYear()));
        lifeFormField.setSelectedItem(new LifeFormChoice((null != event) ? event.lifeForm : null));
        climateField.setSelectedItem(new ClimateChoice((null != event) ? event.climate : null));
        if((null == event) || (null == event.percentWater)) {
            waterField.setValue(Integer.valueOf(0));
        } else {
            waterField.setValue(Integer.valueOf(event.percentWater));
        }
        if((null == event) || (null == event.temperature)) {
            temperatureField.setValue(Integer.valueOf(0));
        } else {
            temperatureField.setValue(Integer.valueOf(event.temperature));
        }
        try {
            socioindustrialField.setText(((null != event) && (null != event.socioIndustrial))
                ? SOCIO_INDUSTRIAL_ADAPTER.marshal(event.socioIndustrial) : null);
        } catch (Exception ex) {
            socioindustrialField.setText(null);
        }
        hpgField.setSelectedItem(new HPGChoice((null != event) ? event.hpg : null, null));
        if((null == event) || (null == event.pressure)) {
            if(0 != pressureField.getSelectedIndex()) {
                pressureField.setSelectedIndex(0);
            }
        } else {
            final PressureChoice currentItem = (PressureChoice) pressureField.getSelectedItem();
            if(!Objects.equals(currentItem.pressure, event.pressure)) {
                pressureField.setSelectedItem(new PressureChoice(event.pressure.intValue(), null));
            }
        }
        if((null == event) || (null == event.pressureAtm)) {
            pressureAtmField.setValue(Double.valueOf(1.0));
        } else {
            pressureAtmField.setValue(Double.valueOf(event.pressureAtm));
        }
        if((null == event) || (null == event.atmMass)) {
            atmMassField.setValue(Double.valueOf(1.0));
        } else {
            atmMassField.setValue(Double.valueOf(event.atmMass));
        }
        atmosphereField.setSelectedItem((null != event) ? event.atmosphere : null);
        if((null == event) || (null == event.albedo)) {
            albedoField.setValue(Double.valueOf(0.1));
        } else {
            albedoField.setValue(Double.valueOf(event.albedo));
        }
        if((null == event) || (null == event.greenhouseEffect)) {
            greenhouseField.setValue(Double.valueOf(0.3));
        } else {
            greenhouseField.setValue(Double.valueOf(event.greenhouseEffect));
        }
        if((null == event) || (null == event.habitability)) {
            habitabilityField.setValue(Integer.valueOf(2));
        } else {
            habitabilityField.setValue(Integer.valueOf(event.habitability));
        }
        if((null == event) || (null == event.populationRating)) {
            if(0 != popField.getSelectedIndex()) {
                popField.setSelectedIndex(0);
            }
        } else {
            final PopulationChoice currentItem = (PopulationChoice) popField.getSelectedItem();
            if(!Objects.equals(currentItem.population, event.populationRating)) {
                popField.setSelectedItem(new PopulationChoice(event.populationRating.intValue(), null));
            }
        }
        governmentField.setText((null != event) ? event.government : null);
        if((null == event) || (null == event.controlRating)) {
            if(0 != controlField.getSelectedIndex()) {
                controlField.setSelectedIndex(0);
            }
        } else {
            final ControlChoice currentItem = (ControlChoice) controlField.getSelectedItem();
            if(!Objects.equals(currentItem.control, event.controlRating)) {
                controlField.setSelectedItem(new ControlChoice(event.controlRating.intValue(), null));
            }
        }

        nameKeep.setSelected((null == event) || (null == event.name));
        shortNameKeep.setSelected((null == event) || (null == event.shortName));
        factionKeep.setSelected((null == event) || (null == event.faction));
        lifeFormKeep.setSelected((null == event) || (null == event.lifeForm));
        climateKeep.setSelected((null == event) || (null == event.climate));
        waterKeep.setSelected((null == event) || (null == event.percentWater));
        temperatureKeep.setSelected((null == event) || (null == event.temperature));
        socioindustrialKeep.setSelected((null == event) || (null == event.socioIndustrial));
        hpgKeep.setSelected((null == event) || (null == event.hpg));
        pressureKeep.setSelected((null == event) || (null == event.pressure));
        pressureAtmKeep.setSelected((null == event) || (null == event.pressureAtm));
        atmMassKeep.setSelected((null == event) || (null == event.atmMass));
        atmosphereKeep.setSelected((null == event) || (null == event.atmosphere));
        albedoKeep.setSelected((null == event) || (null == event.albedo));
        greenhouseKeep.setSelected((null == event) || (null == event.greenhouseEffect));
        habitabilityKeep.setSelected((null == event) || (null == event.habitability));
        popKeep.setSelected((null == event) || (null == event.populationRating));
        governmentKeep.setSelected((null == event) || (null == event.government));
        controlKeep.setSelected((null == event) || (null == event.controlRating));
        
        nameCombined.setText(Utilities.nonNull(planet.getName(date), resourceMap.getString("undefined.text"))); //$NON-NLS-1$
        shortNameCombined.setText(Utilities.nonNull(planet.getShortName(date), resourceMap.getString("undefined.text"))); //$NON-NLS-1$
        factionCombined.setText(planet.getFactionDesc(date));
        lifeFormCombined.setText(planet.getLifeFormName(date));
        climateCombined.setText(planet.getClimateName(date));
        Integer intValue = planet.getPercentWater(date);
        waterCombined.setText(null != intValue ? String.format(resourceMap.getString("water.combined.format"), intValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        intValue = planet.getTemperature(date);
        temperatureCombined.setText(null != intValue ? String.format(resourceMap.getString("temperature.combined.format"), intValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        String socioIndustrialText = "";
        try {
            socioIndustrialText= SOCIO_INDUSTRIAL_ADAPTER.marshal(planet.getSocioIndustrial(date));
        } catch(Exception ex) {
            // Do nothing
        }
        socioindustrialCombined.setText(socioIndustrialText);
        hpgCombined.setText(planet.getHPGClass(date));
        pressureCombined.setText(planet.getPressureName(date));
        Double doubleValue = planet.getPressureAtm(date);
        pressureAtmCombined.setText(null != doubleValue ? String.format(Locale.ROOT, resourceMap.getString("pressureValue.combined.format"), doubleValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        doubleValue = planet.getAtmMass(date);
        atmMassCombined.setText(null != doubleValue ? String.format(Locale.ROOT, resourceMap.getString("atmosphereMass.combined.format"), doubleValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        atmosphereCombined.setText(Utilities.nonNull(planet.getAtmosphere(date), resourceMap.getString("undefined.text"))); //$NON-NLS-1$
        doubleValue = planet.getAlbedo(date);
        albedoCombined.setText(null != doubleValue ? String.format(Locale.ROOT, resourceMap.getString("albedo.combined.format"), doubleValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        doubleValue = planet.getGreenhouseEffect(date);
        greenhouseCombined.setText(null != doubleValue ? String.format(Locale.ROOT, resourceMap.getString("greenhouse.combined.format"), doubleValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        intValue = planet.getHabitability(date);
        habitabilityCombined.setText(null != intValue ? String.format(Locale.ROOT, resourceMap.getString("habitability.combined.format"), intValue) : resourceMap.getString("undefined.text")); //$NON-NLS-1$ //$NON-NLS-2$
        popCombined.setText(planet.getPopulationRatingString(date));
        governmentCombined.setText(Utilities.nonNull(planet.getGovernment(date), resourceMap.getString("undefined.text"))); //$NON-NLS-1$
        controlCombined.setText(planet.getControlRatingString(date));
    }
    
    private void cleanEventField(Planet.PlanetaryEvent event, String field) {
        if((null == event) || (null == field)) {
            return;
        }
        switch(field) {
            case FIELD_NAME: event.name = null; break;
            case FIELD_SHORTNAME: event.shortName = null; break;
            case FIELD_FACTION: event.faction = null; break;
            case FIELD_LIFE_FORM: event.lifeForm = null; break;
            case FIELD_CLIMATE: event.climate = null; break;
            case FIELD_WATER: event.percentWater = null; break;
            case FIELD_TEMPERATURE: event.temperature = null; break;
            case FIELD_SOCIO_INDUSTRIAL: event.socioIndustrial = null; break;
            case FIELD_HPG: event.hpg = null; break;
            case FIELD_PRESSURE: event.pressure = null; break;
            case FIELD_PRESSURE_ATM: event.pressureAtm = null; break;
            case FIELD_ATM_MASS: event.atmMass = null; break;
            case FIELD_ATMOSPHERE: event.atmosphere = null; break;
            case FIELD_ALBEDO: event.albedo = null; break;
            case FIELD_GREENHOUSE: event.greenhouseEffect = null; break;
            case FIELD_HABITABILITY: event.habitability = null; break;
            case FIELD_POPULATION: event.populationRating = null; break;
            case FIELD_GOVERNMENT: event.government = null; break;
            case FIELD_CONTROL: event.controlRating = null; break;
            default: break;
        }
        event.custom = true;
    }
    
    private String nullEmptyText(JTextField field) {
        final String text = field.getText();
        return ((null == text) || text.isEmpty()) ? null : text;
    }
    
    private void updateEvent(Component source, Planet.PlanetaryEvent event) {
        switch(source.getName()) {
            case FIELD_MESSAGE: event.message = nullEmptyText(messageField); break;
            case FIELD_NAME: event.name = nullEmptyText(nameField); break;
            case FIELD_SHORTNAME: event.shortName = nullEmptyText(shortNameField); break;
            case FIELD_LIFE_FORM: event.lifeForm = ((LifeFormChoice) lifeFormField.getSelectedItem()).lifeForm; break;
            case FIELD_CLIMATE: event.climate = ((ClimateChoice) climateField.getSelectedItem()).climate; break;
            case FIELD_WATER: event.percentWater = (Integer) waterField.getValue();
            case FIELD_TEMPERATURE: event.temperature = (Integer) temperatureField.getValue();
            case FIELD_SOCIO_INDUSTRIAL:
                String socioindustrialText = nullEmptyText(socioindustrialField);
                try {
                    event.socioIndustrial = SOCIO_INDUSTRIAL_ADAPTER.unmarshal(socioindustrialText);
                    String newText = SOCIO_INDUSTRIAL_ADAPTER.marshal(event.socioIndustrial);
                    if(!socioindustrialText.equals(newText)) {
                        socioindustrialField.setText(newText);
                    }
                } catch (Exception ex) {
                }
                break;
            case FIELD_HPG: event.hpg = ((HPGChoice) hpgField.getSelectedItem()).hpg; break;
            case FIELD_PRESSURE: event.pressure = ((PressureChoice) pressureField.getSelectedItem()).pressure; break;
            case FIELD_PRESSURE_ATM: event.pressureAtm = (Double) pressureAtmField.getValue(); break;
            case FIELD_ATM_MASS: event.atmMass = (Double) atmMassField.getValue(); break;
            case FIELD_ATMOSPHERE: event.atmosphere = (String) atmosphereField.getSelectedItem(); break;
            case FIELD_ALBEDO: event.albedo = (Double) albedoField.getValue(); break;
            case FIELD_GREENHOUSE: event.greenhouseEffect = (Double) greenhouseField.getValue(); break;
            case FIELD_HABITABILITY: event.habitability = (Integer) habitabilityField.getValue(); break;
            case FIELD_POPULATION: event.populationRating = ((PopulationChoice) popField.getSelectedItem()).population; break;
            case FIELD_GOVERNMENT: event.government = nullEmptyText(governmentField); break;
            case FIELD_CONTROL: event.controlRating = ((ControlChoice) controlField.getSelectedItem()).control; break;
            default: return;
        }
        event.custom = true;
    }
    
    private static class LifeFormChoice {
        public LifeForm lifeForm;
        
        public LifeFormChoice(LifeForm lifeForm) {
            this.lifeForm = lifeForm;
        }
        
        @Override
        public String toString() {
            return null != lifeForm ? lifeForm.name : ""; //$NON-NLS-1$
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(lifeForm);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final LifeFormChoice other = (LifeFormChoice) obj;
            return (other.lifeForm == lifeForm);
        }
    }
    
    private static class ClimateChoice {
        public Climate climate;
        
        public ClimateChoice(Climate climate) {
            this.climate = climate;
        }
        
        @Override
        public String toString() {
            return null != climate ? climate.climateName : ""; //$NON-NLS-1$
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(climate);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final ClimateChoice other = (ClimateChoice) obj;
            return (other.climate == climate);
        }
    }
    
    private static class HPGChoice {
        public Integer hpg;
        public String text;
        
        public HPGChoice(Integer hpg, String text) {
            this.hpg = hpg;
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(hpg);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final HPGChoice other = (HPGChoice) obj;
            return Objects.equals(hpg, other.hpg);
        }
    }
    
    private static class PressureChoice {
        public Integer pressure;
        public String text;
        
        public PressureChoice(int pressure) {
            this(pressure, PlanetaryConditions.getAtmosphereDisplayableName(pressure));
        }
        
        public PressureChoice(int pressure, String text) {
            this.pressure = Integer.valueOf(pressure);
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(pressure);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final PressureChoice other = (PressureChoice) obj;
            return Objects.equals(pressure, other.pressure);
        }
    }

    private static class PopulationChoice {
        public Integer population;
        public String text;
        
        public PopulationChoice(int population) {
            this(Integer.valueOf(population), StarUtil.getPopulationRatingString(population));
        }
        
        public PopulationChoice(Integer population, String text) {
            this.population = population;
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(population);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final PopulationChoice other = (PopulationChoice) obj;
            return Objects.equals(population, other.population);
        }
    }

    private static class ControlChoice {
        public Integer control;
        public String text;
        
        public ControlChoice(int control) {
            this(Integer.valueOf(control), StarUtil.getControlRatingString(control));
        }
        
        public ControlChoice(Integer control, String text) {
            this.control = control;
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(control);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final ControlChoice other = (ControlChoice) obj;
            return Objects.equals(control, other.control);
        }
    }
}
