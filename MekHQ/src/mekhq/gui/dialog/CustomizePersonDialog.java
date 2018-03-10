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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CustomizePersonDialog extends javax.swing.JDialog implements DialogOptionListener {

    /**
	 * This dialog is used to both hire new pilots and to edit existing ones
	 * 
	 */
	private static final long serialVersionUID = -6265589976779860566L;
	private Person person;
    private ArrayList<DialogOptionComponent> optionComps = new ArrayList<DialogOptionComponent>();
    private Hashtable<String, JSpinner> skillLvls = new Hashtable<String, JSpinner>();
    private Hashtable<String, JSpinner> skillBonus = new Hashtable<String, JSpinner>();
    private Hashtable<String, JLabel> skillValues = new Hashtable<String, JLabel>();
    private Hashtable<String, JCheckBox> skillChks = new Hashtable<String, JCheckBox>();
    private PilotOptions options;
    private GregorianCalendar birthdate;
    private GregorianCalendar recruitment;
    private SimpleDateFormat dateFormat;
    private Frame frame;
    
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOk;
    private javax.swing.JButton btnRandomName;
    private javax.swing.JButton btnRandomBloodname;
    private javax.swing.JButton btnDate;
    private javax.swing.JButton btnServiceDate;
    private javax.swing.JComboBox<String> choiceGender;
    private javax.swing.JScrollPane scrOptions;
    private javax.swing.JScrollPane scrBio;
    private javax.swing.JScrollPane scrSkills;
    private javax.swing.JLabel lblToughness;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblBday;
    private javax.swing.JLabel lblRecruitment;
    private javax.swing.JLabel lblAge;
    private javax.swing.JLabel lblNickname;
    private javax.swing.JLabel lblBloodname;
    private javax.swing.JPanel panButtons;
    private javax.swing.JPanel panDemog;
    private javax.swing.JTabbedPane tabStats;
    private javax.swing.JPanel panSkills;
    private javax.swing.JPanel panOptions;
    private javax.swing.JTextField textToughness;
    private javax.swing.JTextField textName;
    private javax.swing.JTextField textNickname;
    private javax.swing.JTextField textBloodname;
    private javax.swing.JTextPane txtBio;
    private JCheckBox chkClan;
    private JComboBox<String> choicePheno;
    
    /* Against the Bot */
    private JComboBox<String> choiceUnitWeight;
    private JComboBox<String> choiceUnitTech;
    private JLabel lblShares;
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
    }

    private void initializePilotAndOptions () {
        this.birthdate = (GregorianCalendar)person.getBirthday().clone();
    	if(campaign.getCampaignOptions().getUseTimeInService() && person.getRecruitment() != null) {
            this.recruitment = (GregorianCalendar)person.getRecruitment().clone();
        }
    	this.options = person.getOptions();	
    	initComponents();
    }

    @SuppressWarnings("serial")
	private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panDemog = new javax.swing.JPanel();
        tabStats = new javax.swing.JTabbedPane();
        lblName = new javax.swing.JLabel();
        lblGender = new javax.swing.JLabel();
        lblBday = new javax.swing.JLabel();
        lblRecruitment = new javax.swing.JLabel();
        lblAge = new javax.swing.JLabel();
        lblNickname = new javax.swing.JLabel();
        lblBloodname = new javax.swing.JLabel();
        textName = new javax.swing.JTextField();
        textNickname = new javax.swing.JTextField();
        textBloodname = new javax.swing.JTextField();
        textToughness = new javax.swing.JTextField();
        lblToughness = new javax.swing.JLabel();
        choiceGender = new javax.swing.JComboBox<String>();
        scrOptions = new javax.swing.JScrollPane();
        panOptions = new javax.swing.JPanel();
        scrSkills = new javax.swing.JScrollPane();
        panSkills = new javax.swing.JPanel();
        scrBio = new javax.swing.JScrollPane();
        txtBio = new javax.swing.JTextPane();
        panButtons = new javax.swing.JPanel();
        btnOk = new javax.swing.JButton();
        
        btnClose = new javax.swing.JButton();
        btnRandomName = new javax.swing.JButton();
        btnRandomBloodname = new javax.swing.JButton();
        btnDate = new javax.swing.JButton();
        btnServiceDate = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title")); // NOI18N
  
        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        panDemog.setLayout(new java.awt.GridBagLayout());
    
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblName, gridBagConstraints);

        textName.setMinimumSize(new java.awt.Dimension(150, 28));
        textName.setName("textName"); // NOI18N
        textName.setPreferredSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        textName.setText(person.getName());
        panDemog.add(textName, gridBagConstraints);

        btnRandomName.setText(resourceMap.getString("btnRandomName.text")); // NOI18N
        btnRandomName.setName("btnRandomName"); // NOI18N
        btnRandomName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	randomName();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panDemog.add(btnRandomName, gridBagConstraints);
        
        if (person.isClanner()) {
            lblBloodname.setText(resourceMap.getString("lblBloodname.text")); // NOI18N
            lblBloodname.setName("lblBloodname"); // NOI18N
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblBloodname, gridBagConstraints);

            textBloodname.setMinimumSize(new java.awt.Dimension(150, 28));
            textBloodname.setName("textBloodname"); // NOI18N
            textBloodname.setPreferredSize(new java.awt.Dimension(150, 28));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            textBloodname.setText(person.getBloodname());
            panDemog.add(textBloodname, gridBagConstraints);

            btnRandomBloodname.setText(resourceMap.getString("btnRandomBloodname.text")); // NOI18N
            btnRandomBloodname.setName("btnRandomBloodname"); // NOI18N
            btnRandomBloodname.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                	randomBloodname();
                 }
            });
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            panDemog.add(btnRandomBloodname, gridBagConstraints);        	
        } else {
        	lblNickname.setText(resourceMap.getString("lblNickname.text")); // NOI18N
        	lblNickname.setName("lblNickname"); // NOI18N
        	gridBagConstraints = new java.awt.GridBagConstraints();
        	gridBagConstraints.gridx = 0;
        	gridBagConstraints.gridy = 2;
        	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        	gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        	panDemog.add(lblNickname, gridBagConstraints);

        	textNickname.setText(person.getCallsign());
        	textNickname.setName("textNickname"); // NOI18N
        	gridBagConstraints = new java.awt.GridBagConstraints();
        	gridBagConstraints.gridx = 1;
        	gridBagConstraints.gridy = 2;
        	gridBagConstraints.gridwidth = 1;
        	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        	panDemog.add(textNickname, gridBagConstraints);
        }
        
        lblGender.setText(resourceMap.getString("lblGender.text")); // NOI18N
        lblGender.setName("lblGender"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblGender, gridBagConstraints);

        DefaultComboBoxModel<String> genderModel = new DefaultComboBoxModel<String>();
        genderModel.addElement(Person.getGenderName(Person.G_MALE));
        genderModel.addElement(Person.getGenderName(Person.G_FEMALE));
        choiceGender.setModel(genderModel);
        choiceGender.setName("choiceGender"); // NOI18N
        choiceGender.setSelectedIndex(person.getGender());
        choiceGender.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	randomName();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choiceGender, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(new JLabel("Phenotype:"), gridBagConstraints);

        DefaultComboBoxModel<String> phenoModel = new DefaultComboBoxModel<String>();
        for(int i = 0; i < Person.PHENOTYPE_NUM; i++) {
            phenoModel.addElement(Person.getPhenotypeName(i));
        }
        choicePheno = new JComboBox<String>(phenoModel);
        choicePheno.setSelectedIndex(person.getPhenotype());
        choicePheno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundChanged();
            }
        });
        choicePheno.setEnabled(person.isClanner());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(choicePheno, gridBagConstraints);

        chkClan = new JCheckBox("Clanner");
        chkClan.setSelected(person.isClanner());
        chkClan.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent et) {
                backgroundChanged();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panDemog.add(chkClan, gridBagConstraints);
        
        lblBday.setText(resourceMap.getString("lblBday.text")); // NOI18N
        lblBday.setName("lblBday"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblBday, gridBagConstraints);

        btnDate.setText(getDateAsString());
        btnDate.setName("btnDate"); // NOI18N
        btnDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	btnDateActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panDemog.add(btnDate, gridBagConstraints);
        
        lblAge.setText(person.getAge(campaign.getCalendar()) + " " + resourceMap.getString("age")); // NOI18N
        lblAge.setName("lblAge"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panDemog.add(lblAge, gridBagConstraints);

        if (campaign.getCampaignOptions().getUseTimeInService() && recruitment != null) {
            lblRecruitment.setText(resourceMap.getString("lblRecruitment.text")); // NOI18N
            lblRecruitment.setName("lblRecruitment"); // NOI18N
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblRecruitment, gridBagConstraints);

            if (recruitment != null) {
                btnServiceDate.setText(getDateAsString2());
                btnServiceDate.setName("btnServiceDate"); // NOI18N
                btnServiceDate.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        btnServiceDateActionPerformed(evt);
                    }
                });
            }
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panDemog.add(btnServiceDate, gridBagConstraints);

        }

        lblToughness.setText(resourceMap.getString("lblToughness.text")); // NOI18N
        lblToughness.setName("lblToughness"); // NOI18N
        
        textToughness.setText(Integer.toString(person.getToughness()));
        textToughness.setName("textToughness"); // NOI18N
             
        if(campaign.getCampaignOptions().useToughness()) {
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 7;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
            panDemog.add(lblToughness, gridBagConstraints);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 7;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            panDemog.add(textToughness, gridBagConstraints);
        }
        
        JLabel lblUnit = new JLabel();
        lblUnit.setText("Original unit:"); // NOI18N
        lblUnit.setName("lblUnit"); // NOI18N
        
        choiceUnitWeight = new JComboBox<String>();
        choiceUnitWeight.addItem("None");
        choiceUnitWeight.addItem("Light");
        choiceUnitWeight.addItem("Medium");
        choiceUnitWeight.addItem("Heavy");
        choiceUnitWeight.addItem("Assault");
        choiceUnitWeight.setSelectedIndex(person.getOriginalUnitWeight());

        choiceUnitTech = new JComboBox<String>();
        choiceUnitTech.addItem("IS1");
        choiceUnitTech.addItem("IS2");
        choiceUnitTech.addItem("Clan");
        choiceUnitTech.setSelectedIndex(person.getOriginalUnitTech());
        
        lblShares = new JLabel();
        lblShares.setText(person.getNumShares(campaign.getCampaignOptions().getSharesForAll()) + " shares");
        
        chkFounder = new JCheckBox("Founding member");
        chkFounder.setSelected(person.isFounder());
        
        choiceOriginalUnit = new JComboBox<Unit>();
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
        choiceOriginalUnit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
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
			}
        });
        
        if (campaign.getCampaignOptions().getUseAtB()) {
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 8;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
	        panDemog.add(lblUnit, gridBagConstraints);
	        
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 8;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        panDemog.add(choiceUnitWeight, gridBagConstraints);

	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 8;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        panDemog.add(choiceUnitTech, gridBagConstraints);

	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 9;
	        gridBagConstraints.gridwidth = 3;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
	        panDemog.add(choiceOriginalUnit, gridBagConstraints);
	        
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 10;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
	        panDemog.add(chkFounder, gridBagConstraints);
	        
	        if (campaign.getCampaignOptions().getUseShareSystem()) {
	        	gridBagConstraints.gridx = 2;
	        	gridBagConstraints.gridy = 10;
	        	gridBagConstraints.gridwidth = 1;
	        	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	        	panDemog.add(lblShares, gridBagConstraints);
	        }
        }

         
        
        scrBio.setName("scrBio"); // NOI18N

        txtBio.setName("txtBio"); // NOI18N
        txtBio.setText(person.getBiography());
		txtBio.setBorder(BorderFactory.createTitledBorder("Biography"));
		scrBio.setMinimumSize(new java.awt.Dimension(300, 300));
		scrBio.setPreferredSize(new java.awt.Dimension(300, 300));
        scrBio.setViewportView(txtBio);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panDemog.add(scrBio, gridBagConstraints);

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

        tabStats.addTab(resourceMap.getString("scrSkills.TabConstraints.tabTitle"),scrSkills); // NOI18N
        if(campaign.getCampaignOptions().useAbilities() 
        		|| campaign.getCampaignOptions().useEdge()
        		|| campaign.getCampaignOptions().useImplants()) {
        	tabStats.addTab(resourceMap.getString("scrOptions.TabConstraints.tabTitle"),scrOptions); // NOI18N
        }
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
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;

        panButtons.add(btnOk, gridBagConstraints);
        gridBagConstraints.gridx++;

        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCloseActionPerformed

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
        
        person.setName(textName.getText());
        person.setCallsign(textNickname.getText());
        person.setBloodname(textBloodname.getText());
        person.setBiography(txtBio.getText());
        person.setGender(choiceGender.getSelectedIndex());
        person.setBirthday(birthdate);
        person.setRecruitment(recruitment);
        person.setPhenotype(choicePheno.getSelectedIndex());
        person.setClanner(chkClan.isSelected());
        try {
        	person.setToughness(Integer.parseInt(textToughness.getText()));
        } catch(NumberFormatException e) {
        	//dont do anything
        }
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
		textName.setText(campaign.getRNG().generate(choiceGender.getSelectedIndex() == Person.G_FEMALE));
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
		textBloodname.setText(Bloodname.randomBloodname(campaign.getFactionCode(), phenotype,
					campaign.getCalendar().get(Calendar.YEAR)).getName());   	
    }

    public void refreshSkills() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        panSkills.removeAll();
        
        JCheckBox chkSkill;
        JLabel lblName;
	    JLabel lblValue;
	    JLabel lblLevel;
	    JLabel lblBonus;
	    JSpinner spnLevel;
	    JSpinner spnBonus;

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panSkills.setLayout(gridbag);

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
        	chkSkill.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					changeSkillValue(type);
					changeValueEnabled(type);
				}
    		});
        	lblName = new JLabel(type);
        	lblValue = new JLabel();
    		if(person.hasSkill(type)) {
    			lblValue.setText(person.getSkill(type).toString());
    		} else {
    			lblValue.setText("-");
    		}
    		skillValues.put(type, lblValue);
    		lblLevel = new JLabel(resourceMap.getString("lblLevel.text"));
    		lblBonus = new JLabel(resourceMap.getString("lblBonus.text"));
    		int level = 0;
    		int bonus = 0;
    		if(person.hasSkill(type)) {
    			level = person.getSkill(type).getLevel();
    			bonus = person.getSkill(type).getBonus();
    		}
    		spnLevel = new JSpinner(new SpinnerNumberModel(level, 0, 10, 1));
    		spnLevel.addChangeListener(new ChangeListener() {
    			@Override
    			public void stateChanged(ChangeEvent evt) {
    				changeSkillValue(type);
    			}
    		});
    		spnLevel.setEnabled(chkSkill.isSelected());
    		spnBonus = new JSpinner(new SpinnerNumberModel(bonus, -8, 8, 1));
    		spnBonus.addChangeListener(new ChangeListener() {
    			@Override
    			public void stateChanged(ChangeEvent evt) {
    				changeSkillValue(type);
    			}
    		});
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
    	for(int i = 0; i < SkillType.getSkillList().length; i++) {
        	final String type = SkillType.getSkillList()[i];
    		if(skillChks.get(type).isSelected()) {
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
                person.getOptions().getOption(option.getName())
                .setValue("None"); // NON-NLS-$1
            } else {
                person.getOptions().getOption(option.getName())
                .setValue(comp.getValue());
            }
        }
    }
    
    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new ArrayList<DialogOptionComponent>();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);

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
            
            addGroup(group, gridbag, c);

            for (Enumeration<IOption> j = group.getOptions(); j
            .hasMoreElements();) {
                IOption option = j.nextElement();

                addOption(option, gridbag, c, true);
            }
        }
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridbag,
            GridBagConstraints c) {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizePersonDialog", new EncodeControl()); //$NON-NLS-1$
        JLabel groupLabel = new JLabel(resourceMap.getString("optionGroup." + group.getKey())); //$NON-NLS-1$

        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridbag,
            GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this,
                option, editable);

        if (OptionsConstants.GUNNERY_WEAPON_SPECIALIST.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue("None"); //$NON-NLS-1$
            //holy crap, do we really need to add every weapon?
            for (Enumeration<EquipmentType> i = EquipmentType.getAllTypes(); i.hasMoreElements();) {
                EquipmentType etype = i.nextElement();
                if(etype instanceof WeaponType) {
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
            optionComp.addValue(Crew.RANGEMASTER_LOS);
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

        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);

        optionComps.add(optionComp);
    }

    private void setOptions() {
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue().equals("None"))) { // NON-NLS-$1
                person.getOptions().getOption(option.getName())
                .setValue("None"); // NON-NLS-$1
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
    	if(!skillChks.get(type).isSelected()) {
    		skillValues.get(type).setText("-");
    		return;
    	}
		SkillType stype = SkillType.getType(type);
		int lvl = (Integer)skillLvls.get(type).getModel().getValue();
		int b = (Integer)skillBonus.get(type).getModel().getValue();
		int target = stype.getTarget() - lvl - b;
		if(stype.countUp()) {
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
        if(clanner) {
            clearAllPhenotypeBonuses();
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
            clearAllPhenotypeBonuses();
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    

    public void optionClicked(DialogOptionComponent arg0, IOption arg1, boolean arg2) {
        //IMplement me!!
    }

}
