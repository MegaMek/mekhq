/*
 * CampaignOptionsDialog.java
 *
 * Created on August 19, 2009, 11:22 AM
 */

package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Faction;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.Ranks;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CampaignOptionsDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = 1935043247792962964L;
	private Campaign campaign;
    private CampaignOptions options;
    private RandomSkillPreferences rskillPrefs;
    private GregorianCalendar date;
    private SimpleDateFormat dateFormat;
    private Frame frame;
    private String camoCategory;
    private String camoFileName;
    private int colorIndex;
    private DirectoryItems camos;
    private Hashtable<String, JTextField> hashAbilityCosts;
    private Hashtable<String, JSpinner> hashSkillTargets;
    private Hashtable<String, JSpinner> hashGreenSkill;
    private Hashtable<String, JSpinner> hashRegSkill;
    private Hashtable<String, JSpinner> hashVetSkill;
    private Hashtable<String, JSpinner> hashEliteSkill;
    private boolean cancelled;
    
    /** Creates new form CampaignOptionsDialog */
    public CampaignOptionsDialog(java.awt.Frame parent, boolean modal, Campaign c, DirectoryItems camos) {
        super(parent, modal);
        this.campaign = c;
        this.options = c.getCampaignOptions();
        this.rskillPrefs = c.getRandomSkillPreferences();
        //this is a hack but I have no idea what is going on here
        this.frame = parent;
        this.date = campaign.calendar;
        dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
        this.camoCategory = campaign.getCamoCategory();
        this.camoFileName = campaign.getCamoFileName();
        this.colorIndex = campaign.getColorIndex();
        this.camos = camos;
        hashAbilityCosts = new Hashtable<String, JTextField>();
        hashSkillTargets = new Hashtable<String, JSpinner>();
        hashGreenSkill = new Hashtable<String, JSpinner>();
        hashRegSkill = new Hashtable<String, JSpinner>();
        hashVetSkill = new Hashtable<String, JSpinner>();
        hashEliteSkill = new Hashtable<String, JSpinner>();
        cancelled = false;

        initComponents();
        setCamoIcon();
        setLocationRelativeTo(parent);
        
        // Rules panel
        useFactionModifiersCheckBox.setSelected(options.useFactionModifiers());
        useDragoonRatingCheckBox.setSelected(options.useDragoonRating());
        clanPriceModifierJFormattedTextField.setValue(options.getClanPriceModifier());
        useFactionForNamesBox.setSelected(options.useFactionForNames());
        useTacticsBox.setSelected(options.useTactics());
        useInitBonusBox.setSelected(options.useInitBonus());
        useToughnessBox.setSelected(options.useToughness());
        useArtilleryBox.setSelected(options.useArtillery());
        useAbilitiesBox.setSelected(options.useAbilities());
        useEdgeBox.setSelected(options.useEdge());
        useImplantsBox.setSelected(options.useImplants());
        payForPartsBox.setSelected(options.payForParts());
        payForUnitsBox.setSelected(options.payForUnits());
        payForSalariesBox.setSelected(options.payForSalaries());
        payForOverheadBox.setSelected(options.payForOverhead());
        payForMaintainBox.setSelected(options.payForMaintain());
        payForTransportBox.setSelected(options.payForTransport());
        sellUnitsBox.setSelected(options.canSellUnits());
        sellPartsBox.setSelected(options.canSellParts());
        
        limitByYearBox.setSelected(options.limitByYear());
        allowClanPurchasesBox.setSelected(options.allowClanPurchases());
        allowISPurchasesBox.setSelected(options.allowISPurchases());
        allowCanonOnlyBox.setSelected(options.allowCanonOnly());

        useQuirksBox.setSelected(options.useQuirks());

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabOptions = new javax.swing.JTabbedPane();
        panGeneral = new javax.swing.JPanel();
        txtName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        lblFaction = new javax.swing.JLabel();
        lblFactionNames = new javax.swing.JLabel();
        lblRank = new javax.swing.JLabel();
        lblGender = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        btnDate = new javax.swing.JButton();
        comboFaction = new javax.swing.JComboBox();
        comboFactionNames = new javax.swing.JComboBox();
        comboRanks = new javax.swing.JComboBox();
        sldGender = new javax.swing.JSlider(SwingConstants.HORIZONTAL);
        btnCamo = new javax.swing.JButton();
        lblCamo = new javax.swing.JLabel();
        panRepair = new javax.swing.JPanel();
        panPersonnel = new javax.swing.JPanel();
        panFinances = new javax.swing.JPanel();
        panNameGen = new javax.swing.JPanel();
        panRank = new javax.swing.JPanel();
        panXP = new javax.swing.JPanel();
        panSkill = new javax.swing.JPanel();
        panAbilityXP = new javax.swing.JPanel();
        panTech = new javax.swing.JPanel();
        panRandomSkill = new javax.swing.JPanel();
        useFactionModifiersCheckBox = new javax.swing.JCheckBox();
        useDragoonRatingCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel clanPriceModifierLabel = new javax.swing.JLabel();
        DecimalFormat numberFormat = (DecimalFormat) DecimalFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setGroupingSeparator(' ');
        decimalFormatSymbols.setDecimalSeparator('.');
        numberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        clanPriceModifierJFormattedTextField = new JFormattedTextField(numberFormat);
        useFactionForNamesBox = new javax.swing.JCheckBox();
        useTacticsBox = new javax.swing.JCheckBox();
        useInitBonusBox = new javax.swing.JCheckBox();
        useToughnessBox = new javax.swing.JCheckBox();
        useArtilleryBox = new javax.swing.JCheckBox();
        useAbilitiesBox = new javax.swing.JCheckBox();
        useEdgeBox = new javax.swing.JCheckBox();
        useImplantsBox = new javax.swing.JCheckBox();
        payForPartsBox = new javax.swing.JCheckBox();
        payForUnitsBox = new javax.swing.JCheckBox();
        payForSalariesBox = new javax.swing.JCheckBox();
        payForOverheadBox = new javax.swing.JCheckBox();
        payForMaintainBox = new javax.swing.JCheckBox();
        payForTransportBox = new javax.swing.JCheckBox();
        sellUnitsBox = new javax.swing.JCheckBox();
        sellPartsBox = new javax.swing.JCheckBox();
        useQuirksBox = new javax.swing.JCheckBox();
        limitByYearBox = new javax.swing.JCheckBox();
        allowClanPurchasesBox = new javax.swing.JCheckBox();
        allowISPurchasesBox = new javax.swing.JCheckBox();
        allowCanonOnlyBox = new javax.swing.JCheckBox();
        choiceTechLevel = new javax.swing.JComboBox();
        btnOkay = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        textRanks = new javax.swing.JTextArea();
        scrRanks = new javax.swing.JScrollPane();

        textCustomRanks = new javax.swing.JTextArea();
        checkCustomRanks = new javax.swing.JCheckBox();
        testCustomRanks = new javax.swing.JButton();
        scrCustomRanks = new javax.swing.JScrollPane();
        choiceOfficerCut = new javax.swing.JComboBox();
        lblOfficerCut = new javax.swing.JLabel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.text"));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        tabOptions.setName("tabOptions"); // NOI18N

        panGeneral.setName("panGeneral"); // NOI18N
        panGeneral.setLayout(new java.awt.GridBagLayout());

        txtName.setText(campaign.getName());
        txtName.setMinimumSize(new java.awt.Dimension(500, 30));
        txtName.setName("txtName"); // NOI18N
        txtName.setPreferredSize(new java.awt.Dimension(500, 30));
        txtName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNameActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(txtName, gridBagConstraints);

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblName, gridBagConstraints);

        lblFaction.setText(resourceMap.getString("lblFaction.text")); // NOI18N
        lblFaction.setName("lblFaction"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblFaction, gridBagConstraints);
        
        lblDate.setText(resourceMap.getString("lblDate.text")); // NOI18N
        lblDate.setName("lblDate"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblDate, gridBagConstraints);

        btnDate.setText(getDateAsString());
        btnDate.setMinimumSize(new java.awt.Dimension(400, 30));
        btnDate.setName("btnDate"); // NOI18N
        btnDate.setPreferredSize(new java.awt.Dimension(400, 30));
        btnDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDateActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(btnDate, gridBagConstraints);

        DefaultComboBoxModel factionModel = new DefaultComboBoxModel();
        for(String sname : Faction.choosableFactionCodes) {
            factionModel.addElement(Faction.getFaction(sname).getFullName());
        }
        factionModel.setSelectedItem(campaign.getFaction().getFullName());
        comboFaction.setModel(factionModel);
        comboFaction.setMinimumSize(new java.awt.Dimension(400, 30));
        comboFaction.setName("comboFaction"); // NOI18N
        comboFaction.setPreferredSize(new java.awt.Dimension(400, 30));
        comboFaction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                factionSelected();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(comboFaction, gridBagConstraints);

        useDragoonRatingCheckBox.setText(resourceMap.getString("useDragoonRatingCheckBox.text")); // NOI18N
        useDragoonRatingCheckBox.setName("useDragoonRatingCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(useDragoonRatingCheckBox, gridBagConstraints);
        
        btnCamo.setMaximumSize(new java.awt.Dimension(84, 72));
        btnCamo.setMinimumSize(new java.awt.Dimension(84, 72));
        btnCamo.setName("btnCamo"); // NOI18N
        btnCamo.setPreferredSize(new java.awt.Dimension(84, 72));
        btnCamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCamoActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(btnCamo, gridBagConstraints);

        lblCamo.setText(resourceMap.getString("lblCamo.text")); // NOI18N
        lblCamo.setName("lblCamo"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblCamo, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panGeneral.TabConstraints.tabTitle"), panGeneral); // NOI18N

        panRepair.setName("panRules"); // NOI18N
        panRepair.setLayout(new java.awt.GridBagLayout());

        useFactionModifiersCheckBox.setText(resourceMap.getString("useFactionModifiersCheckBox.text")); // NOI18N
        useFactionModifiersCheckBox.setToolTipText(resourceMap.getString("useFactionModifiersCheckBox.toolTipText")); // NOI18N
        useFactionModifiersCheckBox.setName("useFactionModifiersCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRepair.add(useFactionModifiersCheckBox, gridBagConstraints);

        useQuirksBox.setText(resourceMap.getString("useQuirksBox.text")); // NOI18N
        useQuirksBox.setToolTipText(resourceMap.getString("useQuirksBox.toolTipText")); // NOI18N
        useQuirksBox.setName("useQuirksBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRepair.add(useQuirksBox, gridBagConstraints);
    
        tabOptions.addTab(resourceMap.getString("panRepair.TabConstraints.tabTitle"), panRepair); // NOI18N

        panTech.setName("panTech"); // NOI18N
        panTech.setLayout(new java.awt.GridBagLayout());

        limitByYearBox.setText(resourceMap.getString("limitByYearBox.text")); // NOI18N
        limitByYearBox.setToolTipText(resourceMap.getString("limitByYearBox.toolTipText")); // NOI18N
        limitByYearBox.setName("limitByYearBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(limitByYearBox, gridBagConstraints);
        
        allowClanPurchasesBox.setText(resourceMap.getString("allowClanPurchasesBox.text")); // NOI18N
        allowClanPurchasesBox.setToolTipText(resourceMap.getString("allowClanPurchasesBox.toolTipText")); // NOI18N
        allowClanPurchasesBox.setName("allowClanPurchasesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowClanPurchasesBox, gridBagConstraints);
        
        allowISPurchasesBox.setText(resourceMap.getString("allowISPurchasesBox.text")); // NOI18N
        allowISPurchasesBox.setToolTipText(resourceMap.getString("allowISPurchasesBox.toolTipText")); // NOI18N
        allowISPurchasesBox.setName("allowISPurchasesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowISPurchasesBox, gridBagConstraints);
        
        allowCanonOnlyBox.setText(resourceMap.getString("allowCanonOnlyBox.text")); // NOI18N
        allowCanonOnlyBox.setToolTipText(resourceMap.getString("allowCanonOnlyBox.toolTipText")); // NOI18N
        allowCanonOnlyBox.setName("allowCanonOnlyBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonOnlyBox, gridBagConstraints);
        
        DefaultComboBoxModel techLevelComboBoxModel = new DefaultComboBoxModel();
        for (int i=0;i<3; i++) {
            techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(i));
        }
        choiceTechLevel.setModel(techLevelComboBoxModel);
        //choiceTechLevel.setToolTipText(resourceMap.getString("choiceTechLevel.toolTipText")); // NOI18N
        choiceTechLevel.setName("choiceTechLevel"); // NOI18N
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(choiceTechLevel, gridBagConstraints);

        lblTechLevel = new JLabel(resourceMap.getString("lblTechLevel.text")); // NOI18N
        lblTechLevel.setName("lblTechLevel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panTech.TabConstraints.tabTitle"), panTech); // NOI18N
        
        panPersonnel.setName("panPersonnel"); // NOI18N
        panPersonnel.setLayout(new java.awt.GridBagLayout());
        
        useTacticsBox.setText(resourceMap.getString("useTacticsBox.text")); // NOI18N
        useTacticsBox.setToolTipText(resourceMap.getString("useTacticsBox.toolTipText")); // NOI18N
        useTacticsBox.setName("useTacticsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useTacticsBox, gridBagConstraints);
        
        useInitBonusBox.setText(resourceMap.getString("useInitBonusBox.text")); // NOI18N
        useInitBonusBox.setToolTipText(resourceMap.getString("useInitBonusBox.toolTipText")); // NOI18N
        useInitBonusBox.setName("useInitBonusBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useInitBonusBox, gridBagConstraints);
        
        useToughnessBox.setText(resourceMap.getString("useToughnessBox.text")); // NOI18N
        useToughnessBox.setToolTipText(resourceMap.getString("useToughnessBox.toolTipText")); // NOI18N
        useToughnessBox.setName("useToughnessBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useToughnessBox, gridBagConstraints);
        
        useArtilleryBox.setText(resourceMap.getString("useArtilleryBox.text")); // NOI18N
        useArtilleryBox.setToolTipText(resourceMap.getString("useArtilleryBox.toolTipText")); // NOI18N
        useArtilleryBox.setName("useArtilleryBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useArtilleryBox, gridBagConstraints);
        
        useAbilitiesBox.setText(resourceMap.getString("useAbilitiesBox.text")); // NOI18N
        useAbilitiesBox.setToolTipText(resourceMap.getString("useAbilitiesBox.toolTipText")); // NOI18N
        useAbilitiesBox.setName("useAbilitiesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useAbilitiesBox, gridBagConstraints);
        
        useEdgeBox.setText(resourceMap.getString("useEdgeBox.text")); // NOI18N
        useEdgeBox.setToolTipText(resourceMap.getString("useEdgeBox.toolTipText")); // NOI18N
        useEdgeBox.setName("useEdgeBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useEdgeBox, gridBagConstraints);
        
        useImplantsBox.setText(resourceMap.getString("useImplantsBox.text")); // NOI18N
        useImplantsBox.setToolTipText(resourceMap.getString("useImplantsBox.toolTipText")); // NOI18N
        useImplantsBox.setName("useImplantsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useImplantsBox, gridBagConstraints);
        
        tabOptions.addTab(resourceMap.getString("panPersonnel.TabConstraints.tabTitle"), panPersonnel); // NOI18N

        panFinances.setName("panFinances"); // NOI18N
        panFinances.setLayout(new java.awt.GridBagLayout());
        
        payForPartsBox.setText(resourceMap.getString("payForPartsBox.text")); // NOI18N
        payForPartsBox.setToolTipText(resourceMap.getString("payForPartsBox.toolTipText")); // NOI18N
        payForPartsBox.setName("payForPartsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForPartsBox, gridBagConstraints);
        
        payForUnitsBox.setText(resourceMap.getString("payForUnitsBox.text")); // NOI18N
        payForUnitsBox.setToolTipText(resourceMap.getString("payForUnitsBox.toolTipText")); // NOI18N
        payForUnitsBox.setName("payForUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForUnitsBox, gridBagConstraints);
        
        payForSalariesBox.setText(resourceMap.getString("payForSalariesBox.text")); // NOI18N
        payForSalariesBox.setToolTipText(resourceMap.getString("payForSalariesBox.toolTipText")); // NOI18N
        payForSalariesBox.setName("payForSalariesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForSalariesBox, gridBagConstraints);
        
        payForOverheadBox.setText(resourceMap.getString("payForOverheadBox.text")); // NOI18N
        payForOverheadBox.setToolTipText(resourceMap.getString("payForOverheadBox.toolTipText")); // NOI18N
        payForOverheadBox.setName("payForOverheadBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForOverheadBox, gridBagConstraints);
        
        payForMaintainBox.setText(resourceMap.getString("payForMaintainBox.text")); // NOI18N
        payForMaintainBox.setToolTipText(resourceMap.getString("payForMaintainBox.toolTipText")); // NOI18N
        payForMaintainBox.setName("payForMaintainBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForMaintainBox, gridBagConstraints);
        
        payForTransportBox.setText(resourceMap.getString("payForTransportBox.text")); // NOI18N
        payForTransportBox.setToolTipText(resourceMap.getString("payForTransportBox.toolTipText")); // NOI18N
        payForTransportBox.setName("payForTransportBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForTransportBox, gridBagConstraints);
        
        sellUnitsBox.setText(resourceMap.getString("sellUnitsBox.text")); // NOI18N
        sellUnitsBox.setToolTipText(resourceMap.getString("sellUnitsBox.toolTipText")); // NOI18N
        sellUnitsBox.setName("sellUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellUnitsBox, gridBagConstraints);
        
        sellPartsBox.setText(resourceMap.getString("sellPartsBox.text")); // NOI18N
        sellPartsBox.setToolTipText(resourceMap.getString("sellPartsBox.toolTipText")); // NOI18N
        sellPartsBox.setName("sellPartsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellPartsBox, gridBagConstraints);
        
        clanPriceModifierLabel.setText(resourceMap.getString("clanPriceModifierLabel.text")); // NOI18N
        clanPriceModifierLabel.setName("clanPriceModifierLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(clanPriceModifierLabel, gridBagConstraints);
        
        clanPriceModifierJFormattedTextField.setColumns(4);
        clanPriceModifierJFormattedTextField.setToolTipText(resourceMap.getString("clanPriceModifierJFormattedTextField.toolTipText")); // NOI18N
        clanPriceModifierJFormattedTextField.setName("clanPriceModifierJFormattedTextField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(clanPriceModifierJFormattedTextField, gridBagConstraints);
        
        tabOptions.addTab(resourceMap.getString("panFinances.TabConstraints.tabTitle"), panFinances); // NOI18N
        
        panXP.setName("panXP"); // NOI18N
        panXP.setLayout(new java.awt.GridBagLayout());
        
        lblScenarioXP = new JLabel(resourceMap.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(options.getScenarioXP(), 0, 10, 1));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblScenarioXP, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnScenarioXP, gridBagConstraints);
        
        lblKillXP = new JLabel(resourceMap.getString("lblKillXP.text"));
        spnKillXP = new JSpinner(new SpinnerNumberModel(options.getKillXPAward(), 0, 50, 1));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnKillXP, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblKillXP, gridBagConstraints);
        
        lblKills = new JLabel(resourceMap.getString("lblKills.text"));
        spnKills = new JSpinner(new SpinnerNumberModel(options.getKillsForXP(), 0, 50, 1));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnKills, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblKills, gridBagConstraints);
        
        txtInstructionsXP = new javax.swing.JTextArea();
        txtInstructionsXP.setText(resourceMap.getString("txtInstructionsXP.text"));
        txtInstructionsXP.setName("txtInstructions");
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setLineWrap(true);
        txtInstructionsXP.setWrapStyleWord(true);
        txtInstructionsXP.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtInstructionsXP.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructionsXP.setPreferredSize(new Dimension(550,120));
        txtInstructionsXP.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(txtInstructionsXP, gridBagConstraints);
        
        String[] colNames = {"+0","+1","+2","+3","+4","+5","+6","+7","+8","+9","+10"};
        tableXP = new JTable(SkillType.getSkillCostsArray(), colNames);
        tableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableXP.setRowSelectionAllowed(false);
        tableXP.setColumnSelectionAllowed(false);
        tableXP.setCellSelectionEnabled(true);
        scrXP = new JScrollPane(tableXP);
        scrXP.setMinimumSize(new Dimension(550,140));
        scrXP.setPreferredSize(new Dimension(550,140));
        JTable rowTable = new RowNamesTable(tableXP);
        scrXP.setRowHeaderView(rowTable);
        scrXP.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(scrXP, gridBagConstraints);      
        
        panAbilityXP.setName("panAbilityXP"); // NOI18N
        panAbilityXP.setLayout(new java.awt.GridBagLayout());
        //arggh.. cant access pilot options statically?
        PilotOptions pilotOptions = new PilotOptions();
        pilotOptions.initialize();
        JLabel lblOption;
        JTextField txtCost;
        int k = 0;
        for (Enumeration<IOptionGroup> i = pilotOptions.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES)) {
                continue;
            }
            if (group.getKey().equalsIgnoreCase(PilotOptions.EDGE_ADVANTAGES)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();
                lblOption = new JLabel(option.getDisplayableName());
                lblOption.setToolTipText(option.getDescription());
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = k;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                panAbilityXP.add(lblOption, gridBagConstraints); 
                txtCost = new JTextField();
                txtCost.setText(Integer.toString(SkillType.getAbilityCost(option.getName())));
                hashAbilityCosts.put(option.getName(), txtCost);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = k++;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.weighty = 0.0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                panAbilityXP.add(txtCost, gridBagConstraints); 
            }
        }
        scrAbilityXP = new JScrollPane(panAbilityXP);
        scrAbilityXP.setMinimumSize(new Dimension(300,140));
        scrAbilityXP.setPreferredSize(new Dimension(300,140));
        scrAbilityXP.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("scrAbilityXP.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrAbilityXP.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(scrAbilityXP, gridBagConstraints);      
        
        tabOptions.addTab(resourceMap.getString("panXP.TabConstraints.tabTitle"), panXP); // NOI18N
    
        panSkill.setName("panSkill"); // NOI18N
        panSkill.setLayout(new java.awt.GridBagLayout());
       
        JPanel skPanel;
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        
        GridBagConstraints c;
        JSpinner spnTarget;
        JSpinner spnGreen;
        JSpinner spnReg;
        JSpinner spnVet;
        JSpinner spnElite;
        SkillType type;
        JLabel lblSkill;
        for(String skillName : SkillType.getSkillList()) {
        	type = SkillType.getType(skillName);
        	skPanel = new JPanel();
        	c = new java.awt.GridBagConstraints();
        	c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = java.awt.GridBagConstraints.BOTH;
            c.anchor = java.awt.GridBagConstraints.WEST;
            c.insets = new java.awt.Insets(5, 5, 5, 5);
            lblSkill = new JLabel(resourceMap.getString("lblSkillTarget.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnTarget = new JSpinner(new SpinnerNumberModel(type.getTarget(), 0, 12, 1));
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            hashEliteSkill.put(skillName, spnElite);
            skPanel.add(spnElite, c);
            c.gridx++;

        	skPanel.setBorder(BorderFactory.createTitledBorder(skillName));
        	panSkill.add(skPanel, gridBagConstraints);
        	gridBagConstraints.gridy++;
        }
        
        JScrollPane scrSkill = new JScrollPane(panSkill);
        scrSkill.setPreferredSize(new java.awt.Dimension(500, 400));
        
        tabOptions.addTab(resourceMap.getString("panSkill.TabConstraints.tabTitle"), scrSkill); // NOI18N

        
        panRandomSkill.setName("panRandomSkill"); // NOI18N
        panRandomSkill.setLayout(new java.awt.GridBagLayout());
        
        lblOverallRecruitBonus = new JLabel(resourceMap.getString("lblOverallRecruitBonus.text"));
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getOverallRecruitBonus(), 0, 8, 1));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(lblOverallRecruitBonus, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(spnOverallRecruitBonus, gridBagConstraints);
        
        
        JScrollPane scrRandomSkill = new JScrollPane(panRandomSkill);
        scrRandomSkill.setPreferredSize(new java.awt.Dimension(500, 400));
        
        tabOptions.addTab(resourceMap.getString("panRandomSkill.TabConstraints.tabTitle"), scrRandomSkill); // NOI18N

        panRank.setName("panRank"); // NOI18N
        panRank.setLayout(new java.awt.GridBagLayout());
        
        lblRank.setText(resourceMap.getString("lblRank.text")); // NOI18N
        lblRank.setName("lblRank"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(lblRank, gridBagConstraints);      
        
        DefaultComboBoxModel rankModel = new DefaultComboBoxModel();
        for(int i = 0; i < Ranks.RS_NUM; i++) {
            rankModel.addElement(Ranks.getRankSystemName(i));
        }
        if(campaign.getRanks().getRankSystem() == Ranks.RS_CUSTOM) {
        	rankModel.setSelectedItem(Ranks.getRankSystemName(Ranks.RS_SL));
        	comboRanks.setEnabled(false);
        	checkCustomRanks.setSelected(true);
        	testCustomRanks.setEnabled(true);
			textCustomRanks.setEnabled(true);
			choiceOfficerCut.setEnabled(true);
        	textCustomRanks.setText(campaign.getRanks().getRankNameList());
        } else {
        	rankModel.setSelectedItem(Ranks.getRankSystemName(campaign.getRanks().getRankSystem()));
        	checkCustomRanks.setSelected(false);
           	testCustomRanks.setEnabled(false);
			textCustomRanks.setEnabled(false);
			choiceOfficerCut.setEnabled(false);
        }
        comboRanks.setModel(rankModel);
        comboRanks.setName("comboRanks"); // NOI18N
        comboRanks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillRankInfo();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(comboRanks, gridBagConstraints);

        checkCustomRanks.setText(resourceMap.getString("checkCustomRanks.text"));
        checkCustomRanks.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent evt) {
				fillRankInfo();
				if(checkCustomRanks.isSelected()) {
					comboRanks.setEnabled(false);
					textCustomRanks.setEnabled(true);
					testCustomRanks.setEnabled(true);
					choiceOfficerCut.setEnabled(true);
				} else {
					comboRanks.setEnabled(true);
					testCustomRanks.setEnabled(false);
					textCustomRanks.setEnabled(false);
					choiceOfficerCut.setEnabled(false);
				}
			}
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRank.add(checkCustomRanks, gridBagConstraints);
        
        testCustomRanks.setText(resourceMap.getString("testCustomRanks.text"));
        testCustomRanks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	fillRankInfo();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRank.add(testCustomRanks, gridBagConstraints);
        
        textCustomRanks.setToolTipText(resourceMap.getString("textCustomRanks.toolTipText"));
        textCustomRanks.setEditable(true);
        textCustomRanks.setLineWrap(true);
        textCustomRanks.setWrapStyleWord(true);
        scrCustomRanks.setViewportView(textCustomRanks);
        scrCustomRanks.setPreferredSize(new Dimension(150,100));
        scrCustomRanks.setMinimumSize(new Dimension(150,100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRank.add(scrCustomRanks, gridBagConstraints);
    
        lblOfficerCut.setText(resourceMap.getString("lblOfficerCut.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRank.add(lblOfficerCut, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panRank.add(choiceOfficerCut, gridBagConstraints);
        
        fillRankInfo();
        textRanks.setEditable(false);
        scrRanks.setViewportView(textRanks);
    	
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.gridheight = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		panRank.add(scrRanks, gridBagConstraints);
        
        tabOptions.addTab(resourceMap.getString("panRank.TabConstraints.tabTitle"), panRank); // NOI18N
        
        panNameGen.setName("panNameGen"); // NOI18N
        panNameGen.setLayout(new java.awt.GridBagLayout());
        
        useFactionForNamesBox.setText(resourceMap.getString("useFactionForNamesBox.text")); // NOI18N
        useFactionForNamesBox.setToolTipText(resourceMap.getString("useFactionForNamesBox.toolTipText")); // NOI18N
        useFactionForNamesBox.setName("useFactionForNamesBox"); // NOI18N
        useFactionForNamesBox.setSelected(campaign.getCampaignOptions().useFactionForNames());
        useFactionForNamesBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useFactionForNamesBoxEvent(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(useFactionForNamesBox, gridBagConstraints);

        
        lblFactionNames.setText(resourceMap.getString("lblFactionNames.text")); // NOI18N
        lblFactionNames.setName("lblFactionNames"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(lblFactionNames, gridBagConstraints);
        
        DefaultComboBoxModel factionNamesModel = new DefaultComboBoxModel();
        for (Iterator<String> i = campaign.getRNG().getFactions(); i.hasNext(); ) {
            String faction = (String) i.next();
            factionNamesModel.addElement(faction);
        }
        factionNamesModel.setSelectedItem(campaign.getRNG().getChosenFaction());
        comboFactionNames.setModel(factionNamesModel);
        comboFactionNames.setMinimumSize(new java.awt.Dimension(400, 30));
        comboFactionNames.setName("comboFactionNames"); // NOI18N
        comboFactionNames.setPreferredSize(new java.awt.Dimension(400, 30));
        comboFactionNames.setEnabled(!useFactionForNamesBox.isSelected());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(comboFactionNames, gridBagConstraints);
        
        lblGender.setText(resourceMap.getString("lblGender.text")); // NOI18N
        lblGender.setName("lblGender"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(lblGender, gridBagConstraints);
              
        sldGender.setMaximum(100);
        sldGender.setMinimum(0);
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);
        sldGender.setValue(campaign.getRNG().getPercentFemale());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(sldGender, gridBagConstraints);
        
        tabOptions.addTab(resourceMap.getString("panNameGen.TabConstraints.tabTitle"), panNameGen); // NOI18N
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabOptions, gridBagConstraints);

        btnOkay.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOkay.setName("btnOkay"); // NOI18N
        btnOkay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(btnOkay, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNameActionPerformed
    	// TODO add your handling code here:
    }//GEN-LAST:event_txtNameActionPerformed

	private void factionSelected() {
		if(useFactionForNamesBox.isSelected()) {
			switchFaction();
		}		
	}

	private void switchFaction() {
		String factionCode = Faction.getFaction(Faction.choosableFactionCodes[comboFaction.getSelectedIndex()]).getNameGenerator();
		boolean found = false;
		for (Iterator<String> i = campaign.getRNG().getFactions(); i.hasNext(); ) {
            String nextFaction = (String) i.next();
            if(nextFaction.equals(factionCode)) {
            	found = true;
            	break;
            }
        }
		if(found) {
			comboFactionNames.setSelectedItem(factionCode);
		}
	}
	
	private void fillRankInfo() {
		String ranks = "";
		if(checkCustomRanks.isSelected()) {
			DefaultComboBoxModel officerModel = new DefaultComboBoxModel();
			String[] customRanks = textCustomRanks.getText().split(",");
			for(String r : customRanks) {
				r = r.trim();
				ranks = ranks + r + "\n";
				officerModel.addElement(r);
			}
			choiceOfficerCut.setModel(officerModel);
			if(campaign.getRanks().getOfficerCut() < officerModel.getSize()) {
				choiceOfficerCut.setSelectedIndex(campaign.getRanks().getOfficerCut());
			} else if(officerModel.getSize() > 0){
				choiceOfficerCut.setSelectedIndex(0);
			}
		} else {
			for(String r : Ranks.getRankSystem(comboRanks.getSelectedIndex())) {
				ranks = ranks + r + "\n";
			}
		}
		textRanks.setText(ranks);
	}
	
	private void useFactionForNamesBoxEvent(java.awt.event.ActionEvent evt) {
		if(useFactionForNamesBox.isSelected()) {
			comboFactionNames.setEnabled(false);
			switchFaction();
		} else {
			comboFactionNames.setEnabled(true);
		}
	}

	private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkayActionPerformed
	    if(txtName.getText().length() > 0) {
	        campaign.setName(txtName.getText());
	        this.setVisible(false);
	    }
	    campaign.calendar = date;
	    campaign.setFactionCode(Faction.choosableFactionCodes[comboFaction.getSelectedIndex()]);
	    if(null != comboFactionNames.getSelectedItem()) {
	    	campaign.getRNG().setChosenFaction((String)comboFactionNames.getSelectedItem());
	    }
	    campaign.getRNG().setPerentFemale(sldGender.getValue());
	    if(checkCustomRanks.isSelected()) {
	    	ArrayList<String> customRanks = new ArrayList<String>();
	    	String[] customRankNames = textCustomRanks.getText().split(",");
			for(String name : customRankNames) {
				name = name.trim();
				customRanks.add(name);
			}
			if(customRanks.size() > 0) {
				campaign.getRanks().setCustomRanks(customRanks, choiceOfficerCut.getSelectedIndex());
			}
	    } else {
	    	if(campaign.getRanks().getRankSystem() != comboRanks.getSelectedIndex()) {
	    		campaign.setRankSystem(comboRanks.getSelectedIndex());
	    	}
	    }
	    campaign.setCamoCategory(camoCategory);
	    campaign.setCamoFileName(camoFileName);
	    campaign.setColorIndex(colorIndex);
	    
	    updateSkillTypes();
	    updateXPCosts();
	    
	    // Rules panel
	    options.setFactionModifiers(useFactionModifiersCheckBox.isSelected());
	    String clanPriceModifierString = clanPriceModifierJFormattedTextField.getText();
	    options.setClanPriceModifier(new Double(clanPriceModifierString));
	    options.setDragoonRating(useDragoonRatingCheckBox.isSelected());
	    options.setFactionForNames(useFactionForNamesBox.isSelected());
	    options.setTactics(useTacticsBox.isSelected());
	    if(useTacticsBox.isSelected()) {
	    	campaign.getGameOptions().getOption("command_init").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("command_init").setValue(false);
	    }
	    options.setInitBonus(useInitBonusBox.isSelected());
	    if(useInitBonusBox.isSelected()) {
	    	campaign.getGameOptions().getOption("individual_initiative").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("individual_initiative").setValue(false);
	    }
	    options.setToughness(useToughnessBox.isSelected());
	    if(useToughnessBox.isSelected()) {
	    	campaign.getGameOptions().getOption("toughness").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("toughness").setValue(false);
	    }
	    options.setArtillery(useArtilleryBox.isSelected());
	    if(useArtilleryBox.isSelected()) {
	    	campaign.getGameOptions().getOption("artillery_skill").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("artillery_skill").setValue(false);
	    }
	    options.setAbilities(useAbilitiesBox.isSelected());
	    if(useAbilitiesBox.isSelected()) {
	    	campaign.getGameOptions().getOption("pilot_advantages").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("pilot_advantages").setValue(false);
	    }
	    options.setEdge(useEdgeBox.isSelected());
	    if(useEdgeBox.isSelected()) {
	    	campaign.getGameOptions().getOption("edge").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("edge").setValue(false);
	    }
	    options.setImplants(useImplantsBox.isSelected());
	    if(useImplantsBox.isSelected()) {
	    	campaign.getGameOptions().getOption("manei_domini").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("manei_domini").setValue(false);
	    }
	    options.setPayForParts(payForPartsBox.isSelected());
	    options.setPayForUnits(payForUnitsBox.isSelected());
	    options.setPayForSalaries(payForSalariesBox.isSelected());
	    options.setPayForOverhead(payForOverheadBox.isSelected());
	    options.setPayForMaintain(payForMaintainBox.isSelected());
	    options.setPayForTransport(payForTransportBox.isSelected());
	    options.setSellUnits(sellUnitsBox.isSelected());
	    options.setSellParts(sellPartsBox.isSelected());
	
	    options.setQuirks(useQuirksBox.isSelected());
	    
	    options.setScenarioXP((Integer)spnScenarioXP.getModel().getValue());
	    options.setKillsForXP((Integer)spnKills.getModel().getValue());
	    options.setKillXPAward((Integer)spnKillXP.getModel().getValue());

	    options.setLimitByYear(limitByYearBox.isSelected());
	    if(limitByYearBox.isSelected()) {
	    	campaign.getGameOptions().getOption("is_eq_limits").setValue(true);
	    } else {
	    	campaign.getGameOptions().getOption("is_eq_limits").setValue(false);
	    }
	    options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
	    options.setAllowISPurchases(allowISPurchasesBox.isSelected());
	    options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
	    if(!allowCanonOnlyBox.isSelected()) {
	    	campaign.getGameOptions().getOption("canon_only").setValue(false);
	    }
	    options.setTechLevel(choiceTechLevel.getSelectedIndex());
	    if(choiceTechLevel.getSelectedIndex() > 0) {
	    	campaign.getGameOptions().getOption("allow_advanced_units").setValue(true);
	    	campaign.getGameOptions().getOption("allow_advanced_ammo").setValue(true);
	    } 
	    
	    rskillPrefs.setOverallRecruitBonus((Integer)spnOverallRecruitBonus.getModel().getValue());	    
	}
	
	private void updateXPCosts() {
		for(int i = 0; i < SkillType.skillList.length; i++) {
			for(int j = 0; j < 11; j++) {
				try {
					int cost = Integer.parseInt((String)tableXP.getValueAt(i, j));
					SkillType.setCost(SkillType.skillList[i],cost,j);
				} catch (NumberFormatException e) {
					MekHQ.logMessage("unreadable value in skill cost table for " + SkillType.skillList[i]);
				}
			}
		}
		for(String optionName : hashAbilityCosts.keySet()) {
			try {
				int cost = Integer.parseInt(hashAbilityCosts.get(optionName).getText());
				SkillType.setAbilityCost(optionName, cost);
			} catch (NumberFormatException e) {
				MekHQ.logMessage("unreadable value in ability cost table for " + optionName);
			}
		}
		//campaign.getSkillCosts().setScenarioXP((Integer)spnScenarioXP.getModel().getValue());
	}
	
	private void updateSkillTypes() {
		for(String skillName : SkillType.getSkillList()) {
			SkillType type = SkillType.getType(skillName);
			if(null != hashSkillTargets.get(skillName)) {
				type.setTarget((Integer)hashSkillTargets.get(skillName).getModel().getValue());
			}
			if(null != hashGreenSkill.get(skillName)) {
				type.setGreenLevel((Integer)hashGreenSkill.get(skillName).getModel().getValue());
			}
			if(null != hashRegSkill.get(skillName)) {
				type.setRegularLevel((Integer)hashRegSkill.get(skillName).getModel().getValue());
			}
			if(null != hashVetSkill.get(skillName)) {
				type.setVeteranLevel((Integer)hashVetSkill.get(skillName).getModel().getValue());
			}
			if(null != hashEliteSkill.get(skillName)) {
				type.setEliteLevel((Integer)hashEliteSkill.get(skillName).getModel().getValue());
			}
		}
	}



	private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
	    cancelled = true;
		this.setVisible(false);
	}//GEN-LAST:event_btnCancelActionPerformed

	public boolean wasCancelled() {
		return cancelled;
	}
	
	private void btnDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDateActionPerformed
	    // show the date chooser
	    DateChooser dc = new DateChooser(frame, date);
	    // user can eiter choose a date or cancel by closing
	    if (dc.showDateChooser() == DateChooser.OK_OPTION) {
	        date = dc.getDate();
	        btnDate.setText(getDateAsString());
	    }
	}//GEN-LAST:event_btnDateActionPerformed

	private void btnCamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCamoActionPerformed
	    CamoChoiceDialog ccd = new CamoChoiceDialog(frame, true, camoCategory, camoFileName, colorIndex, camos);
	    ccd.setVisible(true);
	    camoCategory = ccd.getCategory();
	    camoFileName = ccd.getFileName();
	    if(ccd.getColorIndex() != -1) {
	        colorIndex = ccd.getColorIndex();
	    }
	    setCamoIcon();
	}//GEN-LAST:event_btnCamoActionPerformed
	
	public String getDateAsString() {
	    return dateFormat.format(date.getTime());
	}

    public void setCamoIcon() {
        if (null == camoCategory) {
            return;
        }
        
        if(Player.NO_CAMO.equals(camoCategory)) {
            int colorInd = colorIndex;
            if (colorInd == -1) {
                colorInd = 0;
            }
            BufferedImage tempImage = new BufferedImage(84, 72,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = tempImage.createGraphics();
            graphics.setColor(PlayerColors.getColor(colorInd));
            graphics.fillRect(0, 0, 84, 72);
            btnCamo.setIcon(new ImageIcon(tempImage));
            return;
        }

        // Try to get the camo file.
        try {

            // Translate the root camo directory name.
            if (Player.ROOT_CAMO.equals(camoCategory))
                camoCategory = ""; //$NON-NLS-1$
            Image camo = (Image) camos.getItem(camoCategory, camoFileName);
            btnCamo.setIcon(new ImageIcon(camo));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /*
     * Taken from:
     *  http://tips4java.wordpress.com/2008/11/18/row-number-table/
     *	Use a JTable as a renderer for row numbers of a given main table.
     *  This table must be added to the row header of the scrollpane that
     *  contains the main table.
     */
    public class RowNamesTable extends JTable
    	implements ChangeListener, PropertyChangeListener
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 3151119498072423302L;
		private JTable main;

    	public RowNamesTable(JTable table)
    	{
    		main = table;
    		main.addPropertyChangeListener( this );

    		setFocusable( false );
    		setAutoCreateColumnsFromModel( false );
    		setModel( main.getModel() );
    		setSelectionModel( main.getSelectionModel() );

    		TableColumn column = new TableColumn();
    		column.setHeaderValue(" ");
    		addColumn( column );
    		column.setCellRenderer(new RowNumberRenderer());

    		getColumnModel().getColumn(0).setPreferredWidth(120);
    		setPreferredScrollableViewportSize(getPreferredSize());
    	}

    	@Override
    	public void addNotify()
    	{
    		super.addNotify();
    		Component c = getParent();
    		//  Keep scrolling of the row table in sync with the main table.
    		if (c instanceof JViewport)
    		{
    			JViewport viewport = (JViewport)c;
    			viewport.addChangeListener( this );
    		}
    	}

    	/*
    	 *  Delegate method to main table
    	 */
    	@Override
    	public int getRowCount()
    	{
    		return main.getRowCount();
    	}

    	@Override
    	public int getRowHeight(int row)
    	{
    		return main.getRowHeight(row);
    	}

    	/*
    	 *  This table does not use any data from the main TableModel,
    	 *  so just return a value based on the row parameter.
    	 */
    	@Override
    	public Object getValueAt(int row, int column)
    	{
    		return SkillType.skillList[row];
    	}

    	/*
    	 *  Don't edit data in the main TableModel by mistake
    	 */
    	@Override
    	public boolean isCellEditable(int row, int column)
    	{
    		return false;
    	}
    //
    //  Implement the ChangeListener
    //
    	public void stateChanged(ChangeEvent e)
    	{
    		//  Keep the scrolling of the row table in sync with main table
    		JViewport viewport = (JViewport) e.getSource();
    		JScrollPane scrollPane = (JScrollPane)viewport.getParent();
    		scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    	}
    //
    //  Implement the PropertyChangeListener
    //
    	public void propertyChange(PropertyChangeEvent e)
    	{
    		//  Keep the row table in sync with the main table

    		if ("selectionModel".equals(e.getPropertyName()))
    		{
    			setSelectionModel( main.getSelectionModel() );
    		}

    		if ("model".equals(e.getPropertyName()))
    		{
    			setModel( main.getModel() );
    		}
    	}

    	/*
    	 *  Borrow the renderer from JDK1.4.2 table header
    	 */
    	private class RowNumberRenderer extends DefaultTableCellRenderer
    	{
    		/**
			 * 
			 */
			private static final long serialVersionUID = -5430873664301394767L;

			public RowNumberRenderer()
    		{
    			setHorizontalAlignment(JLabel.LEFT);
    		}

    		public Component getTableCellRendererComponent(
    			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    		{
    			if (table != null)
    			{
    				JTableHeader header = table.getTableHeader();

    				if (header != null)
    				{
    					setForeground(header.getForeground());
    					setBackground(header.getBackground());
    					setFont(header.getFont());
    				}
    			}

    			if (isSelected)
    			{
    				setFont( getFont().deriveFont(Font.BOLD) );
    			}

    			setText((value == null) ? "" : value.toString());
    			setBorder(UIManager.getBorder("TableHeader.cellBorder"));

    			return this;
    		}
    	}
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCamo;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDate;
    private javax.swing.JButton btnOkay;
    private javax.swing.JFormattedTextField clanPriceModifierJFormattedTextField;
    private javax.swing.JComboBox comboFaction;
    private javax.swing.JComboBox comboFactionNames;
    private javax.swing.JComboBox comboRanks;
    private javax.swing.JSlider sldGender;
    private javax.swing.JLabel lblCamo;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblFaction;
    private javax.swing.JLabel lblFactionNames;
    private javax.swing.JLabel lblRank;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblName;
    private javax.swing.JPanel panGeneral;
    private javax.swing.JPanel panRepair;
    private javax.swing.JPanel panTech;
    private javax.swing.JPanel panPersonnel;
    private javax.swing.JPanel panFinances;
    private javax.swing.JPanel panNameGen;
    private javax.swing.JPanel panXP;
    private javax.swing.JPanel panRank;
    private javax.swing.JPanel panSkill;
    private javax.swing.JPanel panRandomSkill;
    private javax.swing.JTabbedPane tabOptions;
    private javax.swing.JTextField txtName;
    private javax.swing.JCheckBox useFactionModifiersCheckBox;
    private javax.swing.JCheckBox useDragoonRatingCheckBox;
    private javax.swing.JCheckBox useFactionForNamesBox;
    private javax.swing.JCheckBox useTacticsBox;
    private javax.swing.JCheckBox useInitBonusBox;
    private javax.swing.JCheckBox useToughnessBox;
    private javax.swing.JCheckBox useArtilleryBox;
    private javax.swing.JCheckBox useAbilitiesBox;
    private javax.swing.JCheckBox useQuirksBox;
    private javax.swing.JCheckBox useEdgeBox;
    private javax.swing.JCheckBox useImplantsBox;
    private javax.swing.JCheckBox payForPartsBox;
    private javax.swing.JCheckBox payForUnitsBox;
    private javax.swing.JCheckBox payForSalariesBox;
    private javax.swing.JCheckBox payForOverheadBox;
    private javax.swing.JCheckBox payForMaintainBox;
    private javax.swing.JCheckBox payForTransportBox;
    private javax.swing.JCheckBox sellUnitsBox;
    private javax.swing.JCheckBox sellPartsBox;
    
    private javax.swing.JTextArea textRanks;
    private javax.swing.JScrollPane scrRanks;
    
    private javax.swing.JTextArea textCustomRanks;
    private javax.swing.JCheckBox checkCustomRanks;
    private javax.swing.JButton testCustomRanks;
    private javax.swing.JScrollPane scrCustomRanks;
    private javax.swing.JComboBox choiceOfficerCut;
    private javax.swing.JLabel lblOfficerCut;
    
    private javax.swing.JTextArea txtInstructionsXP;
    private javax.swing.JScrollPane scrXP;
    private javax.swing.JTable tableXP;
    private javax.swing.JScrollPane scrAbilityXP;
    private javax.swing.JPanel panAbilityXP;
    private JLabel lblScenarioXP;
    private JSpinner spnScenarioXP;
    private JLabel lblKillXP;
    private JSpinner spnKillXP;
    private JLabel lblKills;
    private JSpinner spnKills;
    
    private javax.swing.JCheckBox limitByYearBox;
    private javax.swing.JCheckBox allowClanPurchasesBox;
    private javax.swing.JCheckBox allowISPurchasesBox;
    private javax.swing.JCheckBox allowCanonOnlyBox;
    private javax.swing.JLabel lblTechLevel;
    private javax.swing.JComboBox choiceTechLevel;
    
    private javax.swing.JTable tableRecruitBonus;
    private JLabel lblOverallRecruitBonus;
    private JSpinner spnOverallRecruitBonus;
    
    // End of variables declaration//GEN-END:variables

}
